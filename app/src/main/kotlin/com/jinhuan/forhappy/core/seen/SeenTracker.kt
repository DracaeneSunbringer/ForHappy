package com.jinhuan.forhappy.core.seen

import com.jinhuan.forhappy.core.clock.ServerClock
import com.jinhuan.forhappy.core.db.ForHappyDatabase
import com.jinhuan.forhappy.core.model.ForHappyJson
import com.jinhuan.forhappy.core.model.MemeItem
import com.jinhuan.forhappy.core.model.SeenVisitEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Serializable
private data class DailySeenSnapshot(
    val day: String = "",
    val ids: List<Long> = emptyList(),
)

/**
 * Tracks viewed memes after 1s viewport dwell or detail open, matching `52dmeme.com/app.js` `SEEN`.
 * - Persists per-user pending visit queue (`seen-pending:{uid}`) and UTC+8 daily set (`seen-day:{uid}`).
 * - Debounces batch synchronization to `POST /api/views` (up to 100 visits per batch).
 * - Flushes immediately when the app moves to background or unseen filter is enabled.
 */
class SeenTracker(
    private val kvGet: (String) -> String?,
    private val kvPut: (String, String) -> Unit,
    private val sendVisitsFn: suspend (List<SeenVisitEntry>, Long) -> Boolean,
    private val nowSecFn: () -> Long = { ServerClock.serverNowSec() },
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {
    private val stateMutex = Mutex()
    private val flushMutex = Mutex()
    private var guestLoaded = false
    private var ownerUid: Long = 0L
    private var currentDay: String = dayOfUtc8(nowSecFn())

    private val pendingQueue = LinkedHashMap<String, SeenVisitEntry>()
    private val knownIds = LinkedHashSet<Long>()
    private val dailyIds = LinkedHashSet<Long>()
    private val localGuestIds = LinkedHashSet<Long>()

    private var flushJob: Job? = null
    private var retryJob: Job? = null

    private val _seenIds = MutableStateFlow<Set<Long>>(emptySet())
    val seenIds: StateFlow<Set<Long>> = _seenIds.asStateFlow()

    private val _dailySeenCount = MutableStateFlow(0)
    val dailySeenCount: StateFlow<Int> = _dailySeenCount.asStateFlow()

    private val _newlySeenByDay = MutableStateFlow<Map<String, Int>>(emptyMap())
    val newlySeenByDay: StateFlow<Map<String, Int>> = _newlySeenByDay.asStateFlow()

    fun bindUser(userId: Long?) {
        val targetUid = userId?.takeIf { it > 0L } ?: 0L
        scope.launch {
            bindUserAndSync(targetUid)
        }
    }

    internal suspend fun bindUserAndSync(targetUid: Long) {
        var shouldFlush = false
        stateMutex.withLock {
            ensureGuestLoadedLocked()
            restoreOrSwitchUserLocked(targetUid)
            shouldFlush = ownerUid > 0L && pendingQueue.isNotEmpty()
        }
        if (shouldFlush) {
            flushNow()
        }
    }

    fun ingestFetchedMemes(items: List<MemeItem>) {
        if (items.isEmpty()) return
        scope.launch {
            ingestFetchedMemesSuspend(items)
        }
    }

    internal suspend fun ingestFetchedMemesSuspend(items: List<MemeItem>) {
        stateMutex.withLock {
            ensureGuestLoadedLocked()
            val today = dayOfUtc8(nowSecFn())
            if (currentDay != today) {
                currentDay = today
                dailyIds.clear()
            }
            var changed = false
            for (m in items) {
                val seenAt = m.seenAt ?: continue
                if (seenAt > 0L && m.id > 0L) {
                    if (ownerUid > 0L) {
                        if (knownIds.add(m.id)) changed = true
                        if (dayOfUtc8(seenAt) == today && dailyIds.add(m.id)) {
                            changed = true
                        }
                    } else {
                        if (localGuestIds.add(m.id)) changed = true
                    }
                }
            }
            if (changed) {
                if (ownerUid > 0L) saveDailyLocked() else saveGuestLocked()
                publishSnapshotsLocked()
            }
        }
    }

    fun isSeen(memeId: Long, itemSeenAt: Long? = null): Boolean {
        if (itemSeenAt != null && itemSeenAt > 0L) return true
        return _seenIds.value.contains(memeId)
    }

    fun guestSeenSnapshot(): List<Long> {
        return if (ownerUid <= 0L) _seenIds.value.toList() else emptyList()
    }

    fun markSeen(
        memeId: Long,
        existingSeenAt: Long? = null,
        memeDay: String? = null,
    ) {
        if (memeId <= 0L) return
        scope.launch {
            if (markSeenSuspend(memeId, existingSeenAt, memeDay)) {
                val immediate = stateMutex.withLock { pendingQueue.size >= MAX_BATCH_SIZE }
                if (immediate) {
                    flushNow()
                } else {
                    scheduleDebouncedFlush()
                }
            }
        }
    }

    internal suspend fun markSeenSuspend(
        memeId: Long,
        existingSeenAt: Long? = null,
        memeDay: String? = null,
    ): Boolean {
        if (memeId <= 0L) return false
        return stateMutex.withLock {
            ensureGuestLoadedLocked()
            val nowSec = nowSecFn()
            val today = dayOfUtc8(nowSec)
            if (ownerUid <= 0L) {
                val already = (existingSeenAt != null && existingSeenAt > 0L) || localGuestIds.contains(memeId)
                if (already) return@withLock false
                val wasNew = localGuestIds.add(memeId)
                while (localGuestIds.size > LOCAL_SEEN_LIMIT) {
                    val first = localGuestIds.iterator().next()
                    localGuestIds.remove(first)
                }
                if (wasNew) {
                    saveGuestLocked()
                    if (!memeDay.isNullOrBlank()) {
                        val cur = _newlySeenByDay.value
                        _newlySeenByDay.value = cur + (memeDay to ((cur[memeDay] ?: 0) + 1))
                    }
                    publishSnapshotsLocked()
                }
                return@withLock false
            }

            restoreOrSwitchUserLocked(ownerUid)
            val alreadySeen = (existingSeenAt != null && existingSeenAt > 0L) || knownIds.contains(memeId)
            val alreadyDaily = (existingSeenAt != null && existingSeenAt > 0L && dayOfUtc8(existingSeenAt) == today) ||
                dailyIds.contains(memeId)
            if (alreadySeen && alreadyDaily) {
                return@withLock false
            }

            if (!alreadySeen && !memeDay.isNullOrBlank()) {
                val cur = _newlySeenByDay.value
                _newlySeenByDay.value = cur + (memeDay to ((cur[memeDay] ?: 0) + 1))
            }

            knownIds.add(memeId)
            dailyIds.add(memeId)
            pendingQueue["$today:$memeId"] = SeenVisitEntry(id = memeId, at = nowSec)
            saveDailyLocked()
            savePendingLocked()
            publishSnapshotsLocked()
            true
        }
    }

    fun resetDayDeltaCounter() {
        _newlySeenByDay.value = emptyMap()
    }

    private fun scheduleDebouncedFlush() {
        flushJob?.cancel()
        flushJob = scope.launch {
            delay(FLUSH_DEBOUNCE_MS)
            flushNow()
        }
    }

    private fun scheduleRetryFlush() {
        if (retryJob?.isActive == true) return
        retryJob = scope.launch {
            delay(RETRY_DELAY_MS)
            flushNow()
        }
    }

    suspend fun flushNow(): Boolean {
        flushJob?.cancel()
        return flushMutex.withLock {
            while (true) {
                val (uid, batch) = stateMutex.withLock {
                    if (ownerUid <= 0L || pendingQueue.isEmpty()) {
                        return@withLock (ownerUid to emptyList())
                    }
                    ownerUid to pendingQueue.entries.take(MAX_BATCH_SIZE).map { it.key to it.value }
                }
                if (uid <= 0L || batch.isEmpty()) return@withLock true
                val ok = runCatching {
                    sendVisitsFn(batch.map { it.second }, uid)
                }.getOrDefault(false)
                if (!ok) {
                    scheduleRetryFlush()
                    return@withLock false
                }
                val done = stateMutex.withLock {
                    if (ownerUid != uid) return@withLock true
                    for ((key, visit) in batch) {
                        if (pendingQueue[key] == visit) {
                            pendingQueue.remove(key)
                        }
                    }
                    savePendingLocked()
                    pendingQueue.isEmpty()
                }
                if (done) return@withLock true
            }
            @Suppress("UNREACHABLE_CODE")
            true
        }
    }

    private fun ensureGuestLoadedLocked() {
        if (guestLoaded) return
        guestLoaded = true
        val rawGuest = kvGet(KEY_GUEST_SEEN)
        if (!rawGuest.isNullOrBlank()) {
            runCatching {
                ForHappyJson.decodeFromString(ListSerializer(Long.serializer()), rawGuest)
            }.getOrNull()?.forEach { if (it > 0L) localGuestIds.add(it) }
        }
        publishSnapshotsLocked()
    }

    private fun restoreOrSwitchUserLocked(uid: Long) {
        val today = dayOfUtc8(nowSecFn())
        if (ownerUid == uid) {
            if (currentDay != today) {
                currentDay = today
                dailyIds.clear()
                saveDailyLocked()
                publishSnapshotsLocked()
            }
            return
        }
        savePendingLocked()
        pendingQueue.clear()
        knownIds.clear()
        dailyIds.clear()
        ownerUid = uid
        currentDay = today
        _newlySeenByDay.value = emptyMap()
        if (uid <= 0L) {
            publishSnapshotsLocked()
            return
        }

        val rawDaily = kvGet(seenDailyKey(uid))
        if (!rawDaily.isNullOrBlank()) {
            runCatching {
                ForHappyJson.decodeFromString(DailySeenSnapshot.serializer(), rawDaily)
            }.getOrNull()?.let { snap ->
                if (snap.day == today) {
                    snap.ids.forEach { id ->
                        if (id > 0L) {
                            dailyIds.add(id)
                            knownIds.add(id)
                        }
                    }
                }
            }
        }

        val rawPending = kvGet(seenPendingKey(uid))
        if (!rawPending.isNullOrBlank()) {
            runCatching {
                ForHappyJson.decodeFromString(ListSerializer(SeenVisitEntry.serializer()), rawPending)
            }.getOrNull()?.forEach { visit ->
                if (visit.id > 0L && visit.at > 0L) {
                    val vDay = dayOfUtc8(visit.at)
                    pendingQueue["$vDay:${visit.id}"] = visit
                    knownIds.add(visit.id)
                    if (vDay == today) {
                        dailyIds.add(visit.id)
                    }
                }
            }
        }
        publishSnapshotsLocked()
    }

    private fun savePendingLocked() {
        val uid = ownerUid
        if (uid <= 0L) return
        val list = pendingQueue.values.toList()
        val encoded = if (list.isEmpty()) "" else ForHappyJson.encodeToString(ListSerializer(SeenVisitEntry.serializer()), list)
        kvPut(seenPendingKey(uid), encoded)
    }

    private fun saveDailyLocked() {
        val uid = ownerUid
        if (uid <= 0L) return
        val trimmed = dailyIds.toList().takeLast(LOCAL_SEEN_LIMIT)
        val snap = DailySeenSnapshot(day = currentDay, ids = trimmed)
        kvPut(seenDailyKey(uid), ForHappyJson.encodeToString(DailySeenSnapshot.serializer(), snap))
    }

    private fun saveGuestLocked() {
        val trimmed = localGuestIds.toList().takeLast(LOCAL_SEEN_LIMIT)
        kvPut(KEY_GUEST_SEEN, ForHappyJson.encodeToString(ListSerializer(Long.serializer()), trimmed))
    }

    private fun publishSnapshotsLocked() {
        if (ownerUid > 0L) {
            _seenIds.value = knownIds.toSet()
            _dailySeenCount.value = dailyIds.size
        } else {
            _seenIds.value = localGuestIds.toSet()
            _dailySeenCount.value = localGuestIds.size
        }
    }

    companion object {
        const val DWELL_THRESHOLD_MS = 1000L
        private const val FLUSH_DEBOUNCE_MS = 1500L
        private const val RETRY_DELAY_MS = 5000L
        private const val MAX_BATCH_SIZE = 100
        private const val LOCAL_SEEN_LIMIT = 5000
        private const val KEY_GUEST_SEEN = "seen.guest_local"

        fun forDatabase(
            db: ForHappyDatabase,
            sendVisitsFn: suspend (List<SeenVisitEntry>, Long) -> Boolean,
        ): SeenTracker = SeenTracker(
            kvGet = db::getKvBlocking,
            kvPut = db::putKvBlocking,
            sendVisitsFn = sendVisitsFn,
        )

        private fun seenPendingKey(uid: Long): String = "seen-pending:$uid"
        private fun seenDailyKey(uid: Long): String = "seen-day:$uid"

        fun dayOfUtc8(epochSec: Long): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("GMT+08:00")
            return sdf.format(Date(epochSec * 1000L))
        }
    }
}
