package com.jinhuan.forhappy.core.chat

import com.jinhuan.forhappy.core.clock.ServerClock
import com.jinhuan.forhappy.core.db.ForHappyDatabase
import com.jinhuan.forhappy.core.model.ChatGroup
import com.jinhuan.forhappy.core.model.ChatMessage
import com.jinhuan.forhappy.core.model.ChatMessagesResponse
import com.jinhuan.forhappy.core.model.ChatReplyPreview
import com.jinhuan.forhappy.core.network.scheduler.BackgroundGate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * HTTP cursor-based chat engine (no WebSocket):
 * - Polls `/api/chat/groups/{gid}/messages?after={lastId}&since={sinceServerSec}&include_total=0`
 * - Enforces contiguous message windows per group: only updates `changed` items already present
 *   in the loaded window (never splicing out-of-window historical edits/reactions/boosts into the
 *   list), replaces stale non-overlapping offline cache on initial `after=0`, and runs up to 5-page
 *   sequential catch-up when `more == true`.
 * - Tracks local optimistic `inflight` messages: attaches `retry: true` on resend, avoids jumping
 *   the `after` poll cursor ahead of unpolled peer messages on `POST`, and settles matching items.
 */
class ChatSessionController(
    private val db: ForHappyDatabase? = null,
    private val fetchGroupsFn: suspend () -> List<ChatGroup>,
    private val fetchMessagesFn: suspend (groupId: Long, afterId: Long, sinceSec: Long, beforeId: Long?) -> ChatMessagesResponse?,
    private val sendMessageFn: suspend (groupId: Long, body: String, replyToId: Long?, retry: Boolean) -> Result<ChatMessage?>,
    private val markReadFn: suspend (groupId: Long, lastId: Long) -> Boolean,
    private val toggleReactionFn: suspend (msgId: Long, emoji: String) -> Boolean,
    private val editMessageFn: suspend (msgId: Long, body: String) -> Boolean,
    private val deleteMessageFn: suspend (msgId: Long) -> Boolean,
    private val postBoostFn: suspend (msgId: Long, text: String) -> Result<ChatMessage?> = { _, _ -> Result.success(null) },
    private val postReactionMsgFn: suspend (msgId: Long, emoji: String) -> ChatMessage? = { _, _ -> null },
) {
    data class ChatUiState(
        val groups: List<ChatGroup> = emptyList(),
        val activeGroupId: Long = 1L,
        val messages: List<ChatMessage> = emptyList(),
        val hasMoreOlder: Boolean = false,
        val isLoadingHistory: Boolean = false,
        val isPollingActive: Boolean = false,
        val lastError: String? = null,
        val replyTarget: ChatMessage? = null,
        val editTarget: ChatMessage? = null,
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val stateMutex = Mutex()
    private val groupPollLocks = ConcurrentHashMap<Long, Mutex>()

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val messagesByGroup = HashMap<Long, List<ChatMessage>>()
    private val hasMoreOlderByGroup = HashMap<Long, Boolean>()

    private val lastIdByGroup = ConcurrentHashMap<Long, Long>()
    private val sinceSecByGroup = ConcurrentHashMap<Long, Long>()
    private val sentReadByGroup = ConcurrentHashMap<Long, Long>()
    private val pollCycleByGroup = ConcurrentHashMap<Long, Int>()
    private var pollJob: Job? = null

    @Volatile
    var isChatForegroundAtBottom: Boolean = false

    private fun pollLockFor(groupId: Long): Mutex = groupPollLocks.getOrPut(groupId) { Mutex() }

    internal suspend fun seedCachedWindow(
        groupId: Long,
        cached: List<ChatMessage>,
        hasMore: Boolean = true,
    ) {
        stateMutex.withLock {
            messagesByGroup[groupId] = cached
            hasMoreOlderByGroup[groupId] = hasMore
            if (_uiState.value.activeGroupId == groupId) {
                _uiState.value = _uiState.value.copy(messages = cached, hasMoreOlder = hasMore)
            }
        }
    }

    fun setActiveGroup(groupId: Long) {
        scope.launch {
            val loadedFromDb = stateMutex.withLock {
                if (messagesByGroup.containsKey(groupId)) {
                    val existing = messagesByGroup[groupId].orEmpty()
                    val changedGroup = _uiState.value.activeGroupId != groupId
                    _uiState.value = _uiState.value.copy(
                        activeGroupId = groupId,
                        messages = existing,
                        hasMoreOlder = hasMoreOlderByGroup[groupId] ?: false,
                        replyTarget = if (changedGroup) null else _uiState.value.replyTarget,
                        editTarget = if (changedGroup) null else _uiState.value.editTarget,
                        lastError = null,
                    )
                    false
                } else {
                    true
                }
            }

            if (loadedFromDb) {
                val cached = db?.loadChatMessages(groupId).orEmpty()
                stateMutex.withLock {
                    val seed = messagesByGroup[groupId] ?: cached
                    if (!messagesByGroup.containsKey(groupId) && cached.isNotEmpty()) {
                        messagesByGroup[groupId] = cached
                        hasMoreOlderByGroup.putIfAbsent(groupId, true)
                    }
                    val changedGroup = _uiState.value.activeGroupId != groupId
                    _uiState.value = _uiState.value.copy(
                        activeGroupId = groupId,
                        messages = seed,
                        hasMoreOlder = hasMoreOlderByGroup[groupId] ?: (seed.isNotEmpty()),
                        replyTarget = if (changedGroup) null else _uiState.value.replyTarget,
                        editTarget = if (changedGroup) null else _uiState.value.editTarget,
                        lastError = null,
                    )
                }
            }

            pollOnce(groupId)
        }
    }

    fun setReplyTarget(message: ChatMessage?) {
        _uiState.value = _uiState.value.copy(replyTarget = message, editTarget = null)
    }

    fun setEditTarget(message: ChatMessage?) {
        _uiState.value = _uiState.value.copy(editTarget = message, replyTarget = null)
    }

    fun startPolling() {
        if (pollJob?.isActive == true) return
        pollJob = scope.launch {
            refreshGroups()
            while (isActive) {
                val gid = _uiState.value.activeGroupId
                val needsFastCatchup = pollOnce(gid)
                delay(if (needsFastCatchup) 250L else BackgroundGate.pollingIntervalMs(3_000L))
            }
        }
    }

    fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
        _uiState.value = _uiState.value.copy(isPollingActive = false)
    }

    suspend fun refreshGroups() {
        val fetched = runCatching { fetchGroupsFn() }.getOrNull() ?: return
        stateMutex.withLock {
            val currentId = _uiState.value.activeGroupId
            val resolvedId = if (fetched.any { it.id == currentId }) currentId else (fetched.firstOrNull()?.id ?: currentId)
            val reconciled = fetched.map { g ->
                val sent = sentReadByGroup[g.id] ?: 0L
                val headId = maxOf(g.effectiveLatestId, g.effectiveLastRead)
                if (sent > 0L && sent >= headId) {
                    g.copy(unread = 0, atMe = 0, lastReadId = maxOf(g.lastReadId, sent), lastRead = maxOf(g.lastRead, sent))
                } else {
                    g
                }
            }
            _uiState.value = _uiState.value.copy(groups = reconciled, activeGroupId = resolvedId)
        }
    }

    suspend fun acknowledgeGroupRead(
        groupId: Long = _uiState.value.activeGroupId,
        explicitUpToId: Long = 0L,
    ) {
        val snap = _uiState.value
        val storedMsgs = stateMutex.withLock { messagesByGroup[groupId] }
        val maxMsgId = storedMsgs?.lastOrNull { it.id > 0L }?.id
            ?: if (snap.activeGroupId == groupId) snap.messages.lastOrNull { it.id > 0L }?.id ?: 0L else 0L
        val group = snap.groups.firstOrNull { it.id == groupId }
        val upTo = maxOf(explicitUpToId, maxMsgId, group?.effectiveLatestId ?: 0L)
        if (upTo <= 0L) return

        val prevSent = sentReadByGroup[groupId] ?: 0L
        if (upTo <= prevSent && (group?.unread ?: 0) == 0) return

        val ok = runCatching { markReadFn(groupId, upTo) }.getOrDefault(false)
        if (!ok) return

        sentReadByGroup[groupId] = maxOf(prevSent, upTo)
        stateMutex.withLock {
            val cur = _uiState.value
            val groupMsgs = messagesByGroup[groupId] ?: if (cur.activeGroupId == groupId) cur.messages else emptyList()
            val leftUnread = groupMsgs.count { it.id > upTo && !it.mine && !it.deleted }
            val updatedGroups = cur.groups.map { g ->
                if (g.id == groupId) {
                    g.copy(
                        unread = leftUnread,
                        atMe = if (leftUnread == 0) 0 else g.atMe,
                        lastReadId = maxOf(g.lastReadId, upTo),
                        lastRead = maxOf(g.lastRead, upTo),
                    )
                } else {
                    g
                }
            }
            _uiState.value = cur.copy(groups = updatedGroups)
        }
    }

    suspend fun pollOnce(groupId: Long): Boolean {
        val moreRemaining = pollLockFor(groupId).withLock {
            var after = lastIdByGroup[groupId] ?: 0L
            if (after == 0L) {
                val resp = fetchMessagesFn(groupId, 0L, 0L, null) ?: return@withLock false
                resp.now?.let { nowSec ->
                    ServerClock.calibrateFromServerSec(nowSec)
                    sinceSecByGroup[groupId] = nowSec.toLong()
                }

                val (mergedList, overlaps) = stateMutex.withLock {
                    val current = messagesByGroup[groupId]
                        ?: if (_uiState.value.activeGroupId == groupId) _uiState.value.messages else emptyList()
                    val cachedSrv = current.filter { it.id > 0L }.sortedBy { it.id }
                    val freshSrv = resp.items.filter { it.id > 0L }.sortedBy { it.id }
                    val hasOverlap = cachedSrv.isNotEmpty() && freshSrv.isNotEmpty() &&
                        freshSrv.first().id <= cachedSrv.last().id
                    val base = if (hasOverlap) current else current.filter { it.id <= 0L }
                    val merged = mergeServerDelta(
                        current = base,
                        newItems = resp.items,
                        changedItems = resp.changed,
                        isLiveIncremental = false,
                    )
                    val hasMore = if (hasOverlap && cachedSrv.first().id < freshSrv.first().id) {
                        hasMoreOlderByGroup[groupId] ?: true
                    } else {
                        resp.more
                    }
                    messagesByGroup[groupId] = merged
                    hasMoreOlderByGroup[groupId] = hasMore
                    merged.filter { it.id > 0L }.maxOfOrNull { it.id }?.let { lastIdByGroup[groupId] = it }

                    if (_uiState.value.activeGroupId == groupId) {
                        _uiState.value = _uiState.value.copy(
                            messages = merged,
                            hasMoreOlder = hasMore,
                            isPollingActive = true,
                            lastError = null,
                        )
                    }
                    merged to hasOverlap
                }

                if (overlaps) {
                    db?.saveChatMessages(groupId, mergedList)
                } else {
                    db?.replaceGroupServerMessages(groupId, mergedList)
                }
                false
            } else {
                var batch = 0
                var moreCatchup = false
                var latestMerged: List<ChatMessage>? = null
                while (batch < 5) {
                    val since = sinceSecByGroup[groupId] ?: 0L
                    val resp = fetchMessagesFn(groupId, after, since, null) ?: break
                    resp.now?.let { nowSec ->
                        ServerClock.calibrateFromServerSec(nowSec)
                        sinceSecByGroup[groupId] = nowSec.toLong()
                    }
                    val nextAfter = maxOfId(resp.items, after)
                    val advanced = nextAfter != null && nextAfter > after

                    stateMutex.withLock {
                        val current = messagesByGroup[groupId]
                            ?: if (_uiState.value.activeGroupId == groupId) _uiState.value.messages else emptyList()
                        var merged = mergeServerDelta(
                            current = current,
                            newItems = resp.items,
                            changedItems = resp.changed,
                            isLiveIncremental = true,
                        )
                        var hasMore = hasMoreOlderByGroup[groupId]
                            ?: if (_uiState.value.activeGroupId == groupId) _uiState.value.hasMoreOlder else false

                        if (isChatForegroundAtBottom && _uiState.value.activeGroupId == groupId && merged.size > 400) {
                            merged = merged.drop(merged.size - 320)
                            hasMore = true
                        }

                        messagesByGroup[groupId] = merged
                        hasMoreOlderByGroup[groupId] = hasMore
                        latestMerged = merged
                        if (nextAfter != null) {
                            lastIdByGroup[groupId] = nextAfter
                        }

                        val newlyArrivedUnread = resp.items.count { it.id > after && !it.mine && !it.deleted }
                        val shouldIncrementUnread = newlyArrivedUnread > 0 &&
                            !(isChatForegroundAtBottom && _uiState.value.activeGroupId == groupId)
                        val updatedGroups = if (shouldIncrementUnread) {
                            _uiState.value.groups.map { g ->
                                if (g.id == groupId) g.copy(unread = (g.unread + newlyArrivedUnread).coerceAtMost(99)) else g
                            }
                        } else {
                            _uiState.value.groups
                        }

                        if (_uiState.value.activeGroupId == groupId) {
                            _uiState.value = _uiState.value.copy(
                                groups = updatedGroups,
                                messages = merged,
                                hasMoreOlder = hasMore,
                                isPollingActive = true,
                                lastError = null,
                            )
                        } else if (shouldIncrementUnread) {
                            _uiState.value = _uiState.value.copy(groups = updatedGroups)
                        }
                    }

                    if (nextAfter != null) {
                        after = nextAfter
                    }
                    moreCatchup = resp.more && advanced
                    batch++
                    if (!moreCatchup) break
                }

                latestMerged?.let { db?.saveChatMessages(groupId, it) }
                moreCatchup
            }
        }

        val newestId = stateMutex.withLock {
            messagesByGroup[groupId]?.lastOrNull { it.id > 0L }?.id ?: 0L
        }
        if (isChatForegroundAtBottom && _uiState.value.activeGroupId == groupId && newestId > 0L) {
            acknowledgeGroupRead(groupId, newestId)
        }
        val cycle = (pollCycleByGroup[groupId] ?: 0) + 1
        pollCycleByGroup[groupId] = cycle
        if (cycle % 5 == 0) {
            refreshGroups()
        }
        return moreRemaining
    }

    suspend fun loadOlderHistory(): Int {
        val gid = _uiState.value.activeGroupId
        return pollLockFor(gid).withLock {
            val oldestId = stateMutex.withLock {
                (messagesByGroup[gid] ?: _uiState.value.messages).firstOrNull { it.id > 0L }?.id
            } ?: return@withLock 0

            _uiState.value = _uiState.value.copy(isLoadingHistory = true)
            val resp = fetchMessagesFn(gid, 0L, 0L, oldestId)
            if (resp == null) {
                _uiState.value = _uiState.value.copy(isLoadingHistory = false)
                return@withLock 0
            }

            val (prependedCount, merged) = stateMutex.withLock {
                val base = messagesByGroup[gid]
                    ?: if (_uiState.value.activeGroupId == gid) _uiState.value.messages else emptyList()
                val existingIds = base.asSequence().filter { it.id > 0L }.map { it.id }.toHashSet()
                val mergedList = mergeServerDelta(
                    current = base,
                    newItems = resp.items,
                    changedItems = resp.changed,
                    isLiveIncremental = false,
                )
                val count = mergedList.count { it.id > 0L && it.id < oldestId && !existingIds.contains(it.id) }
                messagesByGroup[gid] = mergedList
                hasMoreOlderByGroup[gid] = resp.more
                if (_uiState.value.activeGroupId == gid) {
                    _uiState.value = _uiState.value.copy(
                        messages = mergedList,
                        hasMoreOlder = resp.more,
                        isLoadingHistory = false,
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isLoadingHistory = false)
                }
                count to mergedList
            }

            db?.saveChatMessages(gid, merged)
            prependedCount
        }
    }

    suspend fun sendOrEditMessage(rawBody: String) {
        val body = rawBody.trim()
        if (body.isEmpty()) return
        val editing = _uiState.value.editTarget
        if (editing != null) {
            val ok = editMessageFn(editing.id, body)
            if (ok) {
                setEditTarget(null)
                pollOnce(_uiState.value.activeGroupId)
            } else {
                _uiState.value = _uiState.value.copy(lastError = "编辑失败")
            }
            return
        }

        val gid = _uiState.value.activeGroupId
        val reply = _uiState.value.replyTarget
        val localId = UUID.randomUUID().toString()
        val optimistic = ChatMessage(
            id = 0L,
            groupId = gid,
            body = body,
            mine = true,
            replyTo = reply?.let {
                ChatReplyPreview(
                    id = it.id,
                    body = it.body,
                    text = it.body,
                    userId = it.userId,
                    name = it.displayAuthor,
                    username = it.resolvedUsername,
                    avatar = it.resolvedAvatar,
                )
            },
            createdAt = ServerClock.serverNowSec(),
            localId = localId,
            deliveryState = "sending",
        )

        stateMutex.withLock {
            val current = messagesByGroup[gid] ?: if (_uiState.value.activeGroupId == gid) _uiState.value.messages else emptyList()
            val updated = current + optimistic
            messagesByGroup[gid] = updated
            if (_uiState.value.activeGroupId == gid) {
                _uiState.value = _uiState.value.copy(
                    messages = updated,
                    replyTarget = null,
                )
            }
        }
        sendInternal(gid, optimistic, retry = false)
    }

    suspend fun retryFailedMessage(localId: String) {
        val gid = _uiState.value.activeGroupId
        val target = stateMutex.withLock {
            val list = messagesByGroup[gid] ?: _uiState.value.messages
            val found = list.firstOrNull { it.localId == localId } ?: return@withLock null
            val updated = list.map { if (it.localId == localId) it.copy(deliveryState = "sending") else it }
            messagesByGroup[gid] = updated
            if (_uiState.value.activeGroupId == gid) {
                _uiState.value = _uiState.value.copy(messages = updated)
            }
            found
        } ?: return
        sendInternal(gid, target, retry = true)
    }

    private suspend fun sendInternal(gid: Long, inflight: ChatMessage, retry: Boolean) {
        val result = sendMessageFn(gid, inflight.body, inflight.replyTo?.id, retry)
        result.fold(
            onSuccess = { confirmed ->
                if (confirmed != null) {
                    stateMutex.withLock {
                        val base = messagesByGroup[gid]
                            ?: if (_uiState.value.activeGroupId == gid) _uiState.value.messages else emptyList()
                        val updated = base.mapNotNull { m ->
                            when {
                                m.localId == inflight.localId -> confirmed.copy(deliveryState = "sent")
                                m.id == confirmed.id && m.id > 0L -> null
                                else -> m
                            }
                        }.sortedWith(messageComparator)
                        messagesByGroup[gid] = updated
                        if (_uiState.value.activeGroupId == gid) {
                            _uiState.value = _uiState.value.copy(messages = updated)
                        }
                    }
                    db?.let {
                        inflight.localId?.let { lid -> it.deleteLocalChatMessage(gid, lid) }
                        it.saveChatMessages(gid, listOf(confirmed))
                    }
                }
                // Poll without jumping lastIdByGroup past confirmed.id so intervening peer messages are never skipped.
                pollOnce(gid)
            },
            onFailure = { err ->
                stateMutex.withLock {
                    val base = messagesByGroup[gid]
                        ?: if (_uiState.value.activeGroupId == gid) _uiState.value.messages else emptyList()
                    val updated = base.map {
                        if (it.localId == inflight.localId) it.copy(deliveryState = "failed") else it
                    }
                    messagesByGroup[gid] = updated
                    if (_uiState.value.activeGroupId == gid) {
                        _uiState.value = _uiState.value.copy(
                            messages = updated,
                            lastError = err.message ?: "发送失败",
                        )
                    }
                }
            },
        )
    }

    suspend fun applyUpdatedServerMessage(updated: ChatMessage) {
        val settled = updated.copy(deliveryState = "sent")
        if (settled.id <= 0L) return
        val targetGid = if (settled.groupId > 0L) settled.groupId else _uiState.value.activeGroupId
        val updatedInWindow = stateMutex.withLock {
            val base = messagesByGroup[targetGid]
                ?: if (_uiState.value.activeGroupId == targetGid) _uiState.value.messages else emptyList()
            val existing = base.firstOrNull { it.id == settled.id } ?: return@withLock false
            val preserved = settled.copy(floor = settled.floor ?: existing.floor)
            val next = refreshReferences(
                messages = base.map { if (it.id == preserved.id) preserved else it },
                source = preserved,
            )
            messagesByGroup[targetGid] = next
            if (_uiState.value.activeGroupId == targetGid) {
                _uiState.value = _uiState.value.copy(messages = next)
            }
            true
        }
        if (updatedInWindow) {
            db?.saveChatMessages(targetGid, listOf(settled))
        }
    }

    suspend fun toggleReaction(msgId: Long, emoji: String) {
        val directMsg = runCatching { postReactionMsgFn(msgId, emoji) }.getOrNull()
        if (directMsg != null) {
            applyUpdatedServerMessage(directMsg)
            return
        }
        if (toggleReactionFn(msgId, emoji)) {
            pollOnce(_uiState.value.activeGroupId)
        }
    }

    suspend fun sendBoost(msgId: Long, text: String): Result<Unit> {
        val res = postBoostFn(msgId, text)
        return res.fold(
            onSuccess = { msg ->
                if (msg != null) {
                    applyUpdatedServerMessage(msg)
                } else {
                    pollOnce(_uiState.value.activeGroupId)
                }
                Result.success(Unit)
            },
            onFailure = { err ->
                _uiState.value = _uiState.value.copy(lastError = err.message ?: "回应失败")
                Result.failure(err)
            },
        )
    }

    suspend fun recallMessage(msgId: Long) {
        if (deleteMessageFn(msgId)) {
            pollOnce(_uiState.value.activeGroupId)
        }
    }

    /**
     * Pure merge function suitable for direct unit testing:
     * - Applies `changed` ONLY for messages already loaded in [current] (never splicing older out-of-window
     *   messages into the timeline and creating gaps), and refreshes matching `replyTo` summaries.
     * - Converges/settles local inflight optimistic items when an incoming `m.mine == true` server
     *   item matches normalized `body` and `replyTo?.id`.
     */
    fun mergeServerDelta(
        current: List<ChatMessage>,
        newItems: List<ChatMessage>,
        changedItems: List<ChatMessage>,
        isLiveIncremental: Boolean = false,
    ): List<ChatMessage> {
        val byServerId = LinkedHashMap<Long, ChatMessage>()
        val remainingInflight = mutableListOf<ChatMessage>()

        for (m in current) {
            if (m.id > 0L) {
                byServerId[m.id] = m
            } else {
                remainingInflight += m
            }
        }
        val existingIds = byServerId.keys.toHashSet()

        for (inc in newItems) {
            if (inc.id > 0L) {
                val prev = byServerId[inc.id]
                byServerId[inc.id] = inc.copy(
                    floor = inc.floor ?: prev?.floor,
                    deliveryState = "sent",
                )
                if (inc.mine) {
                    val incNorm = ChatRichTextParser.normalizeForMatch(inc.body)
                    val incReplyId = inc.replyTo?.id
                    val matchIdx = remainingInflight.indexOfFirst { inf ->
                        ChatRichTextParser.normalizeForMatch(inf.body) == incNorm &&
                            inf.replyTo?.id == incReplyId
                    }
                    if (matchIdx >= 0) {
                        remainingInflight.removeAt(matchIdx)
                    }
                }
            }
        }

        if (isLiveIncremental) {
            for (inc in newItems) {
                if (inc.id > 0L && !existingIds.contains(inc.id) && !inc.deleted) {
                    val parentId = inc.replyTo?.id ?: continue
                    val parent = byServerId[parentId] ?: continue
                    byServerId[parentId] = parent.copy(replyCount = parent.replyCount + 1)
                }
            }
        }

        for (chg in changedItems) {
            if (chg.id > 0L) {
                val existing = byServerId[chg.id]
                if (existing != null) {
                    val updated = chg.copy(
                        floor = chg.floor ?: existing.floor,
                        deliveryState = "sent",
                    )
                    byServerId[chg.id] = updated
                    for ((keyId, item) in byServerId) {
                        val ref = item.replyTo
                        if (ref != null && ref.id == updated.id) {
                            byServerId[keyId] = item.copy(replyTo = updatedReplyPreview(ref, updated))
                        }
                    }
                }
            }
        }

        val combined = ArrayList<ChatMessage>(byServerId.size + remainingInflight.size)
        combined.addAll(byServerId.values)
        combined.addAll(remainingInflight)
        combined.sortWith(messageComparator)
        return combined
    }

    private fun refreshReferences(messages: List<ChatMessage>, source: ChatMessage): List<ChatMessage> {
        return messages.map { m ->
            val ref = m.replyTo
            if (ref != null && ref.id == source.id) {
                m.copy(replyTo = updatedReplyPreview(ref, source))
            } else {
                m
            }
        }
    }

    private fun updatedReplyPreview(ref: ChatReplyPreview, source: ChatMessage): ChatReplyPreview {
        return if (source.deleted) {
            ref.copy(deleted = true, body = "", text = null)
        } else {
            val summary = ChatRichTextParser.previewSummary(source.body).take(200)
            ref.copy(
                deleted = false,
                body = source.body,
                text = summary.ifBlank { ref.text },
            )
        }
    }

    private val messageComparator = Comparator<ChatMessage> { a, b ->
        when {
            a.id > 0L && b.id > 0L -> a.id.compareTo(b.id)
            a.id > 0L -> -1
            b.id > 0L -> 1
            else -> a.createdAt.compareTo(b.createdAt)
        }
    }

    private fun maxOfId(items: List<ChatMessage>, fallback: Long): Long? {
        val mx = items.maxOfOrNull { it.id } ?: return null
        return maxOf(mx, fallback)
    }
}
