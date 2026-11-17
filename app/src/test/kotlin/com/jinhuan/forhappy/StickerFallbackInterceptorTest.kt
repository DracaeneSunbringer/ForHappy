package com.jinhuan.forhappy

import com.jinhuan.forhappy.core.network.media.StickerFallbackInterceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StickerFallbackInterceptorTest {

    @Test
    fun spaHtml200FallbackTriggersExtensionRetryAndCachesRealImageExt() {
        val extStore = mutableMapOf<String, String>()
        val requestedPaths = mutableListOf<String>()
        val gifPayload = byteArrayOf(0x47, 0x49, 0x46, 0x38, 0x39, 0x61)

        val server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val p = request.requestUrl?.encodedPath.orEmpty()
                requestedPaths += p
                return when (p) {
                    "/stickers/qq/custom.png" -> MockResponse()
                        .setResponseCode(200)
                        .setHeader("Content-Type", "text/html; charset=utf-8")
                        .setBody("<!doctype html><html>spa</html>")
                    "/stickers/qq/custom.gif" -> MockResponse()
                        .setResponseCode(200)
                        .setHeader("Content-Type", "image/gif")
                        .setBody(okio.Buffer().write(gifPayload))
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }
        server.start()
        try {
            val interceptor = StickerFallbackInterceptor(
                getCachedExt = { pack, name -> extStore["$pack:$name"] },
                putCachedExt = { pack, name, ext -> extStore["$pack:$name"] = ext },
                deleteCachedExt = { pack, name -> extStore.remove("$pack:$name") },
            )
            val client = OkHttpClient.Builder().addInterceptor(interceptor).build()
            val req = Request.Builder().url(server.url("/stickers/qq/custom.png")).build()
            client.newCall(req).execute().use { resp ->
                assertEquals(200, resp.code)
                assertEquals("image/gif", resp.header("Content-Type"))
                assertArrayEquals(gifPayload, resp.body?.bytes())
            }
            assertEquals("gif", extStore["qq:custom"])
            assertEquals(listOf("/stickers/qq/custom.png", "/stickers/qq/custom.gif"), requestedPaths)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun poisonedCachedExtSelfHealsWhenServerReturnsHtml200() {
        val extStore = mutableMapOf("muscle:muscle_001" to "png")
        val webpPayload = byteArrayOf(0x52, 0x49, 0x46, 0x46)

        val server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when (request.requestUrl?.encodedPath) {
                    "/stickers/muscle/muscle_001.webp" -> MockResponse()
                        .setResponseCode(200)
                        .setHeader("Content-Type", "image/webp")
                        .setBody(okio.Buffer().write(webpPayload))
                    else -> MockResponse()
                        .setResponseCode(200)
                        .setHeader("Content-Type", "text/html; charset=utf-8")
                        .setBody("<!doctype html><html>spa</html>")
                }
            }
        }
        server.start()
        try {
            val interceptor = StickerFallbackInterceptor(
                getCachedExt = { pack, name -> extStore["$pack:$name"] },
                putCachedExt = { pack, name, ext -> extStore["$pack:$name"] = ext },
                deleteCachedExt = { pack, name -> extStore.remove("$pack:$name") },
            )
            val client = OkHttpClient.Builder().addInterceptor(interceptor).build()
            val req = Request.Builder().url(server.url("/stickers/muscle/muscle_001.webp")).build()
            client.newCall(req).execute().use { resp ->
                assertEquals(200, resp.code)
                assertEquals("image/webp", resp.header("Content-Type"))
                assertArrayEquals(webpPayload, resp.body?.bytes())
            }
            assertEquals("webp", extStore["muscle:muscle_001"])
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun missingStickerAcrossAllExtensionsSynthesizes404AndClearsCache() {
        val extStore = mutableMapOf("mu:ghost" to "png")
        val requestedPaths = mutableListOf<String>()

        val server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                requestedPaths += request.requestUrl?.encodedPath.orEmpty()
                return MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "text/html; charset=utf-8")
                    .setBody("<!doctype html><html>spa</html>")
            }
        }
        server.start()
        try {
            val interceptor = StickerFallbackInterceptor(
                getCachedExt = { pack, name -> extStore["$pack:$name"] },
                putCachedExt = { pack, name, ext -> extStore["$pack:$name"] = ext },
                deleteCachedExt = { pack, name -> extStore.remove("$pack:$name") },
            )
            val client = OkHttpClient.Builder().addInterceptor(interceptor).build()
            val req = Request.Builder().url(server.url("/stickers/mu/ghost.png")).build()
            client.newCall(req).execute().use { resp ->
                assertEquals(404, resp.code)
            }
            assertNull(extStore["mu:ghost"])
            assertEquals(
                listOf("/stickers/mu/ghost.png", "/stickers/mu/ghost.gif", "/stickers/mu/ghost.webp"),
                requestedPaths,
            )
        } finally {
            server.shutdown()
        }
    }
}
