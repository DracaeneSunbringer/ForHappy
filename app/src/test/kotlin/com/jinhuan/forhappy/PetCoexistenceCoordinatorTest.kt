package com.jinhuan.forhappy

import com.jinhuan.forhappy.core.network.pet.PetCoexistenceCoordinator
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class PetCoexistenceCoordinatorTest {

    @Test
    fun http409AdoptsServerStateAndNeverRetriesStaleRev() = runBlocking {
        var calls = 0
        val coord = PetCoexistenceCoordinator(
            transport = { _, _, _ ->
                calls++
                PetCoexistenceCoordinator.RawHttpResult(
                    code = 409,
                    body = """{"ok":false,"rev":42,"coins":9999,"clockStart":1794052800,"state":{"en":88,"arc":{"cur":{"g":"fishgame"}}}}""",
                )
            },
            sleeper = {},
        )

        val outcome = coord.submitWorldActions(
            listOf(buildJsonObject { put("a", JsonPrimitive("harvestAll")) }),
            expectedRev = 40L,
        )
        assertEquals(1, calls) // Never retried with old rev=40!
        assertTrue(outcome is PetCoexistenceCoordinator.WorldActOutcome.ConflictAdopted)
        val snap = (outcome as PetCoexistenceCoordinator.WorldActOutcome.ConflictAdopted).snapshot
        assertEquals(42L, snap.rev)
        assertTrue(coord.uiState.value.arcadeStandbyActive)
    }

    @Test
    fun idempotentActReusesExactOperationUuidOnTransientIoRetry() = runBlocking {
        val seenOps = mutableListOf<String>()
        var attempt = 0
        val coord = PetCoexistenceCoordinator(
            transport = { _, _, body ->
                seenOps += body?.get("operation")?.jsonPrimitive?.content.orEmpty()
                attempt++
                if (attempt == 1) throw IOException("timeout")
                PetCoexistenceCoordinator.RawHttpResult(200, """{"ok":true,"rev":11,"state":{"en":95}}""")
            },
            sleeper = {},
            uuidProvider = { "op-fixed-uuid-123" },
        )

        val out = coord.submitWorldActions(listOf(buildJsonObject { put("a", JsonPrimitive("water")) }), expectedRev = 10L)
        assertTrue(out is PetCoexistenceCoordinator.WorldActOutcome.Applied)
        assertEquals(listOf("op-fixed-uuid-123", "op-fixed-uuid-123"), seenOps)
    }

    @Test
    fun nonIdempotentVisitNeverRetriesOn5xxAndTriggersReconcile() = runBlocking {
        val paths = mutableListOf<String>()
        val coord = PetCoexistenceCoordinator(
            transport = { method, path, _ ->
                paths += "$method $path"
                if (path.contains("/visit/")) {
                    PetCoexistenceCoordinator.RawHttpResult(502, """{"error":"bad gateway"}""")
                } else {
                    PetCoexistenceCoordinator.RawHttpResult(200, """{"ok":true,"rev":55,"state":{"en":70}}""")
                }
            },
            sleeper = {},
        )

        val res = coord.nonIdempotentWorldPost("/api/pet/world/visit/7")
        assertTrue(res is PetCoexistenceCoordinator.SimpleWriteOutcome.UnconfirmedReconciled)
        assertEquals(listOf("POST /api/pet/world/visit/7", "GET /api/pet/world"), paths)
    }

    @Test
    fun rateBucketEnforces2200msWorldGapAnd429Backoff() = runBlocking {
        var fakeClockMs = 10_000L
        val sleeps = mutableListOf<Long>()
        var reqNum = 0
        val coord = PetCoexistenceCoordinator(
            transport = { _, _, _ ->
                reqNum++
                if (reqNum == 1) {
                    PetCoexistenceCoordinator.RawHttpResult(429, """{"retry":4}""", retryAfterSec = 4L)
                } else {
                    PetCoexistenceCoordinator.RawHttpResult(200, """{"ok":true,"rev":2,"state":{}}""")
                }
            },
            nowElapsedMs = { fakeClockMs },
            sleeper = { ms ->
                sleeps += ms
                fakeClockMs += ms
            },
        )

        val first = coord.submitWorldActions(emptyList(), expectedRev = 1L)
        assertTrue(first is PetCoexistenceCoordinator.WorldActOutcome.Failed)
        val second = coord.submitWorldActions(emptyList(), expectedRev = 1L)
        assertTrue(second is PetCoexistenceCoordinator.WorldActOutcome.Applied)
        assertNotNull(sleeps.firstOrNull { it >= 4000L })
    }
}
