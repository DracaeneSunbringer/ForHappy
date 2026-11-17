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

/**
 * HTTP cursor-based chat engine (no WebSocket):
 * - Polls `/api/chat/groups/{gid}/messages?after={lastId}&since={sinceServerSec}&include_total=0`
 * - Applies `items` and `changed` (message edits / recalls), respects `more` pagination,
 *   calibrates [ServerClock] from `now`.
 * - Tracks local optimistic `inflight` messages: attaches `retry: true` on resend and settles/converges
 *   local inflight items as soon as polling sees `m.mine == true` with matching `body` and `replyTo.id`.
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

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val lastIdByGroup = HashMap<Long, Long>()
    private val sinceSecByGroup = HashMap<Long, Long>()
    private val sentReadByGroup = HashMap<Long, Long>()
    private val pollCycleByGroup = HashMap<Long, Int>()
    private var pollJob: Job? = null

    @Volatile
    var isChatForegroundAtBottom: Boolean = false

    fun setActiveGroup(groupId: Long) {
        scope.launch {
            val cached = db?.loadChatMessages(groupId).orEmpty()
            stateMutex.withLock {
                _uiState.value = _uiState.value.copy(
                    activeGroupId = groupId,
                    messages = cached,
                    replyTarget = null,
                    editTarget = null,
                    lastError = null,
                )
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
                pollOnce(gid)
                delay(BackgroundGate.pollingIntervalMs(3_000L))
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
                val headId = maxOf(g.latestId, g.effectiveLastRead)
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
        val maxMsgId = if (snap.activeGroupId == groupId) {
            snap.messages.lastOrNull { it.id > 0L }?.id ?: 0L
        } else {
            0L
        }
        val group = snap.groups.firstOrNull { it.id == groupId }
        val upTo = maxOf(explicitUpToId, maxMsgId, group?.latestId ?: 0L)
        if (upTo <= 0L) return

        val prevSent = sentReadByGroup[groupId] ?: 0L
        if (upTo <= prevSent && (group?.unread ?: 0) == 0) return

        val ok = runCatching { markReadFn(groupId, upTo) }.getOrDefault(false)
        if (!ok) return

        sentReadByGroup[groupId] = maxOf(prevSent, upTo)
        stateMutex.withLock {
            val cur = _uiState.value
            val leftUnread = if (cur.activeGroupId == groupId) {
                cur.messages.count { it.id > upTo && !it.mine && !it.deleted }
            } else {
                0
            }
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

    suspend fun pollOnce(groupId: Long) {
        val after = lastIdByGroup[groupId] ?: 0L
        val since = sinceSecByGroup[groupId] ?: 0L
        val resp = fetchMessagesFn(groupId, after, since, null) ?: return

        resp.now?.let { nowSec ->
            ServerClock.calibrateFromServerSec(nowSec)
            sinceSecByGroup[groupId] = nowSec.toLong()
        }

        val updatedList = stateMutex.withLock {
            if (_uiState.value.activeGroupId != groupId) {
                // Still advance cursors cleanly
                maxOfId(resp.items, after)?.let { lastIdByGroup[groupId] = it }
                return
            }
            val merged = mergeServerDelta(
                current = _uiState.value.messages,
                newItems = resp.items,
                changedItems = resp.changed,
            )
            maxOfId(resp.items, after)?.let { lastIdByGroup[groupId] = it }
            val currentGroupUnread = _uiState.value.groups.firstOrNull { it.id == groupId }?.unread ?: 0
            val newlyArrivedUnread = resp.items.count { it.id > after && !it.mine && !it.deleted }
            val updatedGroups = if (after > 0L && newlyArrivedUnread > 0 && !isChatForegroundAtBottom) {
                _uiState.value.groups.map { g ->
                    if (g.id == groupId) g.copy(unread = (g.unread + newlyArrivedUnread).coerceAtMost(99)) else g
                }
            } else {
                _uiState.value.groups
            }
            _uiState.value = _uiState.value.copy(
                groups = updatedGroups,
                messages = merged,
                hasMoreOlder = if (after == 0L) resp.more else _uiState.value.hasMoreOlder,
                isPollingActive = true,
                lastError = null,
            )
            merged
        }

        db?.saveChatMessages(groupId, updatedList)
        val newestId = updatedList.lastOrNull { it.id > 0L }?.id ?: 0L
        if (isChatForegroundAtBottom && _uiState.value.activeGroupId == groupId && newestId > 0L) {
            acknowledgeGroupRead(groupId, newestId)
        }
        val cycle = (pollCycleByGroup[groupId] ?: 0) + 1
        pollCycleByGroup[groupId] = cycle
        if (cycle % 5 == 0) {
            refreshGroups()
        }
    }

    suspend fun loadOlderHistory(): Int {
        val gid = _uiState.value.activeGroupId
        val oldestId = _uiState.value.messages.firstOrNull { it.id > 0L }?.id ?: return 0
        _uiState.value = _uiState.value.copy(isLoadingHistory = true)
        val resp = fetchMessagesFn(gid, 0L, 0L, oldestId)
        if (resp == null) {
            _uiState.value = _uiState.value.copy(isLoadingHistory = false)
            return 0
        }
        return stateMutex.withLock {
            val existingIds = _uiState.value.messages.asSequence().filter { it.id > 0L }.map { it.id }.toHashSet()
            val merged = mergeServerDelta(_uiState.value.messages, resp.items, resp.changed)
            val prependedCount = merged.count { it.id > 0L && it.id < oldestId && !existingIds.contains(it.id) }
            _uiState.value = _uiState.value.copy(
                messages = merged,
                hasMoreOlder = resp.more,
                isLoadingHistory = false,
            )
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
            _uiState.value = _uiState.value.copy(
                messages = _uiState.value.messages + optimistic,
                replyTarget = null,
            )
        }
        sendInternal(gid, optimistic, retry = false)
    }

    suspend fun retryFailedMessage(localId: String) {
        val target = _uiState.value.messages.firstOrNull { it.localId == localId } ?: return
        stateMutex.withLock {
            _uiState.value = _uiState.value.copy(
                messages = _uiState.value.messages.map {
                    if (it.localId == localId) it.copy(deliveryState = "sending") else it
                },
            )
        }
        sendInternal(_uiState.value.activeGroupId, target, retry = true)
    }

    private suspend fun sendInternal(gid: Long, inflight: ChatMessage, retry: Boolean) {
        val result = sendMessageFn(gid, inflight.body, inflight.replyTo?.id, retry)
        result.fold(
            onSuccess = { confirmed ->
                if (confirmed != null) {
                    stateMutex.withLock {
                        val updated = _uiState.value.messages.mapNotNull { m ->
                            when {
                                m.localId == inflight.localId -> confirmed.copy(deliveryState = "sent")
                                m.id == confirmed.id && m.id > 0L -> null
                                else -> m
                            }
                        }.sortedWith(messageComparator)
                        lastIdByGroup[gid] = maxOf(lastIdByGroup[gid] ?: 0L, confirmed.id)
                        _uiState.value = _uiState.value.copy(messages = updated)
                    }
                    db?.let {
                        inflight.localId?.let { lid -> it.deleteLocalChatMessage(gid, lid) }
                        it.saveChatMessages(gid, listOf(confirmed))
                    }
                } else {
                    pollOnce(gid)
                }
            },
            onFailure = { err ->
                stateMutex.withLock {
                    _uiState.value = _uiState.value.copy(
                        messages = _uiState.value.messages.map {
                            if (it.localId == inflight.localId) it.copy(deliveryState = "failed") else it
                        },
                        lastError = err.message ?: "发送失败",
                    )
                }
            },
        )
    }

    suspend fun applyUpdatedServerMessage(updated: ChatMessage) {
        val settled = updated.copy(deliveryState = "sent")
        val targetGid = if (settled.groupId > 0L) settled.groupId else _uiState.value.activeGroupId
        stateMutex.withLock {
            if (_uiState.value.activeGroupId == targetGid) {
                val exists = _uiState.value.messages.any { it.id == settled.id }
                val next = if (exists) {
                    _uiState.value.messages.map { if (it.id == settled.id) settled else it }
                } else {
                    (_uiState.value.messages + settled).sortedWith(messageComparator)
                }
                _uiState.value = _uiState.value.copy(messages = next)
            }
        }
        if (settled.id > 0L) {
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
     * - Replaces changed messages (`edited` / `deleted`) by server `id`.
     * - Converges/settles local inflight optimistic items when an incoming `m.mine == true` server
     *   item matches normalized `body` and `replyTo?.id`.
     */
    fun mergeServerDelta(
        current: List<ChatMessage>,
        newItems: List<ChatMessage>,
        changedItems: List<ChatMessage>,
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

        for (chg in changedItems) {
            if (chg.id > 0L) {
                byServerId[chg.id] = chg.copy(deliveryState = "sent")
            }
        }

        for (inc in newItems) {
            if (inc.id > 0L) {
                byServerId[inc.id] = inc.copy(deliveryState = "sent")
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

        val combined = ArrayList<ChatMessage>(byServerId.size + remainingInflight.size)
        combined.addAll(byServerId.values)
        combined.addAll(remainingInflight)
        combined.sortWith(messageComparator)
        return combined
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
