package com.jinhuan.forhappy.core.network.pet

import com.jinhuan.forhappy.core.clock.IsleClockRules
import com.jinhuan.forhappy.core.clock.ServerClock
import com.jinhuan.forhappy.core.model.ForHappyJson
import com.jinhuan.forhappy.core.model.PetStateResponse
import com.jinhuan.forhappy.core.model.PetWorldSnapshot
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.io.IOException
import java.util.UUID
import kotlin.math.max

/**
 * Enforces strict coexistence rules with the server-side `live-daemon`:
 * 1. Optimistic locking via `rev` (`expectedRev`) on `/api/pet/world/act`: HTTP 409 immediately
 *    adopts the server's returned `state` & `rev` and NEVER retries using the stale `rev`.
 * 2. Strict idempotency split:
 *    - Idempotent writes (`/world/act`, `/battle`, `/battle/buy|enhance|respec|train`, `/state`)
 *      attach one `operation: UUID` and reuse THAT SAME `operation` on transient retry.
 *    - Non-idempotent writes (`/world/visit/:id`, `/world/help/:id`, `/battle/equip`, `/battle/chest`,
 *      `/battle/points`) NEVER auto-retry on timeout/5xx; they reconcile by re-fetching state.
 * 3. Per-bucket rate limiting (`>= 2200ms` world/visit/help bucket, `>= 1200ms` battle/state bucket)
 *    plus HTTP 429 `Retry-After` / `retry` backoff.
 * 4. Detects `world.state.arc.cur` active arcade round (`arcade-standby`) and warns the user.
 * 5. Calibrates [IsleClockRules.b12ConfigureClock] from `world.clockStart` and [ServerClock] from `now`.
 */
class PetCoexistenceCoordinator(
    private val transport: suspend (method: String, apiPath: String, body: JsonObject?) -> RawHttpResult,
    private val nowElapsedMs: () -> Long = { System.nanoTime() / 1_000_000L },
    private val sleeper: suspend (Long) -> Unit = { ms -> if (ms > 0L) delay(ms) },
    private val uuidProvider: () -> String = { UUID.randomUUID().toString() },
) {
    data class RawHttpResult(
        val code: Int,
        val body: String,
        val retryAfterSec: Long? = null,
    )

    sealed class WorldActOutcome {
        data class Applied(val snapshot: PetWorldSnapshot, val raw: JsonObject) : WorldActOutcome()
        data class ConflictAdopted(val snapshot: PetWorldSnapshot, val reason: String) : WorldActOutcome()
        data class Failed(val code: Int, val message: String, val reconcilable: Boolean) : WorldActOutcome()
    }

    sealed class SimpleWriteOutcome {
        data class Success(val body: JsonObject) : SimpleWriteOutcome()
        data class UnconfirmedReconciled(val message: String, val refreshed: JsonObject?) : SimpleWriteOutcome()
        data class Failed(val code: Int, val message: String) : SimpleWriteOutcome()
    }

    data class UiCoexistenceState(
        val world: PetWorldSnapshot? = null,
        val petState: PetStateResponse? = null,
        val arcadeStandbyActive: Boolean = false,
        val arcadeStandbySummary: String? = null,
        val lastConflictNote: String? = null,
        val clockConfigured: Boolean = false,
    )

    companion object {
        const val WORLD_BUCKET_GAP_MS = 2200L
        const val BATTLE_BUCKET_GAP_MS = 1200L
    }

    private val worldBucketMutex = Mutex()
    private val battleBucketMutex = Mutex()
    private var lastWorldReqAtMs: Long = Long.MIN_VALUE / 4
    private var lastBattleReqAtMs: Long = Long.MIN_VALUE / 4
    private var worldCooldownUntilMs: Long = 0L
    private var battleCooldownUntilMs: Long = 0L

    private val _uiState = MutableStateFlow(UiCoexistenceState())
    val uiState: StateFlow<UiCoexistenceState> = _uiState.asStateFlow()

    suspend fun fetchWorld(): PetWorldSnapshot? {
        val res = transport("GET", "/api/pet/world", null)
        if (res.code !in 200..299) return null
        val json = parseObj(res.body) ?: return null
        val snap = extractWorldSnapshot(json)
        adoptWorldSnapshot(snap, conflictNote = null)
        return snap
    }

    suspend fun fetchPetState(): PetStateResponse? {
        val res = transport("GET", "/api/pet/state", null)
        if (res.code !in 200..299) return null
        val parsed = runCatching { ForHappyJson.decodeFromString<PetStateResponse>(res.body) }.getOrNull() ?: return null
        parsed.now?.let { ServerClock.calibrateFromServerSec(it) }
        _uiState.value = _uiState.value.copy(petState = parsed)
        return parsed
    }

    suspend fun submitWorldActions(
        actions: List<JsonObject>,
        expectedRev: Long? = _uiState.value.world?.rev,
    ): WorldActOutcome {
        val opId = uuidProvider()
        val payload = buildJsonObject {
            expectedRev?.let { put("rev", JsonPrimitive(it)) }
            expectedRev?.let { put("expectedRev", JsonPrimitive(it)) }
            put("operation", JsonPrimitive(opId))
            put("actions", JsonArray(actions))
        }

        // Idempotent write: reuse exact same `operation` across max 1 transient network/5xx retry
        var attempt = 0
        while (true) {
            val res = try {
                acquireWorldSlot { transport("POST", "/api/pet/world/act", payload) }
            } catch (ioe: IOException) {
                if (attempt == 0) {
                    attempt++
                    sleeper(450L)
                    continue
                }
                return WorldActOutcome.Failed(-1, "网络超时，已保留幂等凭证待刷新对账", reconcilable = true)
            }

            val obj = parseObj(res.body) ?: JsonObject(emptyMap())
            applyServerClockAndIslandClock(obj)

            when (res.code) {
                in 200..299 -> {
                    val snap = extractWorldSnapshot(obj)
                    adoptWorldSnapshot(snap, conflictNote = null)
                    return WorldActOutcome.Applied(snap, obj)
                }

                409 -> {
                    // Optimistic lock conflict with live-daemon or another device:
                    // MUST adopt returned state + rev and NEVER retry with the old rev!
                    val snap = extractWorldSnapshot(obj)
                    val note = "检测到远端守护/另一端同步冲突 (409)，已直接采纳服务端最新 rev=${snap.rev}"
                    adoptWorldSnapshot(snap, conflictNote = note)
                    return WorldActOutcome.ConflictAdopted(snap, note)
                }

                429 -> {
                    val waitSec = parseRetryAfterSec(res, obj).coerceAtLeast(3L)
                    applyWorldCooldown(waitSec * 1000L)
                    return WorldActOutcome.Failed(429, "请求过快，已自动退避 ${waitSec}s", reconcilable = false)
                }

                in 500..599 -> {
                    if (attempt == 0) {
                        attempt++
                        sleeper(500L)
                        continue
                    }
                    return WorldActOutcome.Failed(res.code, obj["error"]?.jsonPrimitive?.content ?: "服务端临时错误(${res.code})", reconcilable = true)
                }

                else -> {
                    val msg = obj["error"]?.jsonPrimitive?.content ?: obj["msg"]?.jsonPrimitive?.content ?: "操作失败 (${res.code})"
                    return WorldActOutcome.Failed(res.code, msg, reconcilable = false)
                }
            }
        }
    }

    /**
     * Non-idempotent world interactions (`/world/visit/:id` or `/world/help/:id`):
     * MUST respect >=2200ms world bucket and MUST NEVER auto-retry on timeout/5xx.
     */
    suspend fun nonIdempotentWorldPost(path: String, body: JsonObject = JsonObject(emptyMap())): SimpleWriteOutcome {
        val res = try {
            acquireWorldSlot { transport("POST", path, body) }
        } catch (ioe: IOException) {
            fetchWorld()
            return SimpleWriteOutcome.UnconfirmedReconciled(
                "网络超时（非幂等接口禁止自动重试，已重拉最新小岛状态对账）",
                _uiState.value.world?.state,
            )
        }
        val obj = parseObj(res.body) ?: JsonObject(emptyMap())
        applyServerClockAndIslandClock(obj)
        if (res.code == 429) {
            val waitSec = parseRetryAfterSec(res, obj).coerceAtLeast(3L)
            applyWorldCooldown(waitSec * 1000L)
            return SimpleWriteOutcome.Failed(429, "请求过于频繁，请 ${waitSec}s 后再试")
        }
        if (res.code in 500..599) {
            fetchWorld()
            return SimpleWriteOutcome.UnconfirmedReconciled(
                "服务端 ${res.code}（已停止重试并重拉状态对账，避免重复扣费）",
                _uiState.value.world?.state,
            )
        }
        if (res.code in 200..299) {
            return SimpleWriteOutcome.Success(obj)
        }
        return SimpleWriteOutcome.Failed(res.code, obj["error"]?.jsonPrimitive?.content ?: "操作失败 (${res.code})")
    }

    /**
     * Idempotent battle/pet writes (`/api/pet/battle`, `/battle/buy`, `/battle/enhance`,
     * `/battle/respec`, `/battle/train`, `/api/pet/state`): attach `operation: UUID`,
     * respect >=1200ms bucket, reuse SAME `operation` on transient failure retry.
     */
    suspend fun idempotentBattleOrStatePost(
        path: String,
        body: JsonObject,
        method: String = "POST",
    ): SimpleWriteOutcome {
        val opId = uuidProvider()
        val withOp = buildJsonObject {
            for ((k, v) in body) {
                put(k, v)
            }
            if (!body.containsKey("operation")) {
                put("operation", JsonPrimitive(opId))
            }
        }
        var attempt = 0
        while (true) {
            val res = try {
                acquireBattleSlot { transport(method, path, withOp) }
            } catch (ioe: IOException) {
                if (attempt == 0) {
                    attempt++
                    sleeper(400L)
                    continue
                }
                return SimpleWriteOutcome.Failed(-1, "网络超时，请刷新状态核对")
            }
            val obj = parseObj(res.body) ?: JsonObject(emptyMap())
            applyServerClockAndIslandClock(obj)
            when (res.code) {
                in 200..299 -> return SimpleWriteOutcome.Success(obj)
                429 -> {
                    val waitSec = parseRetryAfterSec(res, obj).coerceAtLeast(2L)
                    applyBattleCooldown(waitSec * 1000L)
                    return SimpleWriteOutcome.Failed(429, "请求过快，退避 ${waitSec}s")
                }
                in 500..599 -> {
                    if (attempt == 0) {
                        attempt++
                        sleeper(400L)
                        continue
                    }
                    return SimpleWriteOutcome.Failed(res.code, obj["error"]?.jsonPrimitive?.content ?: "服务端错误(${res.code})")
                }
                else -> return SimpleWriteOutcome.Failed(res.code, obj["error"]?.jsonPrimitive?.content ?: "操作失败(${res.code})")
            }
        }
    }

    /**
     * Non-idempotent battle writes (`/battle/equip`, `/battle/chest`, `/battle/points`):
     * NEVER auto-retry on timeout or 5xx!
     */
    suspend fun nonIdempotentBattlePost(path: String, body: JsonObject): SimpleWriteOutcome {
        val res = try {
            acquireBattleSlot { transport("POST", path, body) }
        } catch (ioe: IOException) {
            val refreshed = transport("GET", "/api/pet/battle/home", null)
            return SimpleWriteOutcome.UnconfirmedReconciled(
                "网络超时（非幂等接口已禁止自动重试，请确认装备/宝箱结果）",
                parseObj(refreshed.body),
            )
        }
        val obj = parseObj(res.body) ?: JsonObject(emptyMap())
        applyServerClockAndIslandClock(obj)
        if (res.code == 429) {
            val waitSec = parseRetryAfterSec(res, obj).coerceAtLeast(2L)
            applyBattleCooldown(waitSec * 1000L)
            return SimpleWriteOutcome.Failed(429, "请求过快，退避 ${waitSec}s")
        }
        if (res.code in 500..599) {
            val refreshed = transport("GET", "/api/pet/battle/home", null)
            return SimpleWriteOutcome.UnconfirmedReconciled(
                "服务端 ${res.code}（已中止自动重试并刷新状态对账）",
                parseObj(refreshed.body),
            )
        }
        return if (res.code in 200..299) {
            SimpleWriteOutcome.Success(obj)
        } else {
            SimpleWriteOutcome.Failed(res.code, obj["error"]?.jsonPrimitive?.content ?: "操作失败(${res.code})")
        }
    }

    private suspend fun <T> acquireWorldSlot(block: suspend () -> T): T = worldBucketMutex.withLock {
        val now = nowElapsedMs()
        val readyAt = max(lastWorldReqAtMs + WORLD_BUCKET_GAP_MS, worldCooldownUntilMs)
        if (readyAt > now) {
            sleeper(readyAt - now)
        }
        lastWorldReqAtMs = nowElapsedMs()
        block()
    }

    private suspend fun <T> acquireBattleSlot(block: suspend () -> T): T = battleBucketMutex.withLock {
        val now = nowElapsedMs()
        val readyAt = max(lastBattleReqAtMs + BATTLE_BUCKET_GAP_MS, battleCooldownUntilMs)
        if (readyAt > now) {
            sleeper(readyAt - now)
        }
        lastBattleReqAtMs = nowElapsedMs()
        block()
    }

    private fun applyWorldCooldown(ms: Long) {
        worldCooldownUntilMs = max(worldCooldownUntilMs, nowElapsedMs() + ms)
    }

    private fun applyBattleCooldown(ms: Long) {
        battleCooldownUntilMs = max(battleCooldownUntilMs, nowElapsedMs() + ms)
    }

    private fun parseObj(raw: String): JsonObject? =
        runCatching { ForHappyJson.parseToJsonElement(raw).jsonObject }.getOrNull()

    private fun parseRetryAfterSec(res: RawHttpResult, obj: JsonObject): Long {
        val fromHeader = res.retryAfterSec
        if (fromHeader != null && fromHeader > 0L) return fromHeader
        val retryElem = obj["retry"] ?: obj["retryAfter"] ?: obj["retry_after"]
        val parsed = runCatching { retryElem?.jsonPrimitive?.longOrNull }.getOrNull()
        return parsed?.coerceIn(1L, 60L) ?: 3L
    }

    private fun applyServerClockAndIslandClock(obj: JsonObject) {
        runCatching { obj["now"]?.jsonPrimitive?.doubleOrNull }.getOrNull()?.let {
            ServerClock.calibrateFromServerSec(it)
        }
        val clockStart = runCatching { obj["clockStart"]?.jsonPrimitive?.longOrNull }.getOrNull() ?: 0L
        if (clockStart > 0L) {
            runCatching { IsleClockRules.b12ConfigureClock(clockStart) }
        }
    }

    private fun extractWorldSnapshot(obj: JsonObject): PetWorldSnapshot {
        applyServerClockAndIslandClock(obj)
        val stateObj = runCatching { obj["state"]?.jsonObject }.getOrNull()
        val rev = runCatching { obj["rev"]?.jsonPrimitive?.longOrNull }.getOrNull()
            ?: runCatching { obj["expectedRev"]?.jsonPrimitive?.longOrNull }.getOrNull()
            ?: _uiState.value.world?.rev
            ?: 0L
        val coins = runCatching { obj["coins"]?.jsonPrimitive?.longOrNull }.getOrNull()
            ?: _uiState.value.world?.coins
            ?: 0L
        val clockStart = runCatching { obj["clockStart"]?.jsonPrimitive?.longOrNull }.getOrNull()
            ?: IsleClockRules.b12ClockStart()
        val now = runCatching { obj["now"]?.jsonPrimitive?.doubleOrNull }.getOrNull()
        return PetWorldSnapshot(
            ok = runCatching { obj["ok"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() }.getOrNull() ?: true,
            rev = rev,
            coins = coins,
            clockStart = clockStart,
            now = now,
            state = stateObj,
            error = runCatching { obj["error"]?.jsonPrimitive?.content }.getOrNull(),
            msg = runCatching { obj["msg"]?.jsonPrimitive?.content }.getOrNull(),
        )
    }

    private fun adoptWorldSnapshot(snapshot: PetWorldSnapshot, conflictNote: String?) {
        val prev = _uiState.value
        val state = snapshot.state ?: prev.world?.state
        val merged = snapshot.copy(state = state)
        val arcadeCur = detectArcadeStandby(state)
        _uiState.value = prev.copy(
            world = merged,
            arcadeStandbyActive = arcadeCur != null,
            arcadeStandbySummary = arcadeCur,
            lastConflictNote = conflictNote,
            clockConfigured = IsleClockRules.b12ClockStart() > 0L,
        )
    }

    fun detectArcadeStandby(state: JsonObject?): String? {
        if (state == null) return null
        val arc = runCatching { state["arc"]?.jsonObject }.getOrNull() ?: return null
        val cur: JsonElement = arc["cur"] ?: return null
        if (cur is JsonNull) return null
        val type = runCatching {
            cur.jsonObject["g"]?.jsonPrimitive?.content
                ?: cur.jsonObject["type"]?.jsonPrimitive?.content
                ?: "街机对局"
        }.getOrDefault("街机对局")
        return "检测到未结算街机对局（$type）：远端 live-daemon 已触发 arcade-standby 礼让暂停农场自动写入，请先完成/结算对局。"
    }
}
