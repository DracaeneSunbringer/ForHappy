package com.jinhuan.forhappy.core.network.media

import android.content.Context
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import com.jinhuan.forhappy.core.db.ForHappyDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayInputStream
import java.io.File
import java.security.MessageDigest

/**
 * Local file cache + OkHttp-backed bridge for [ShelllessWebScreen] and [LoginWebSheet]:
 * routes same-origin HTML documents, ES modules (`/app.js`, `/pets.js`, `/pet-isle.js`),
 * CSS, stickers, and API endpoints through [httpClient] (Rust ECH + H2 + EdgePin) so embedded
 * WebViews bypass plain-SNI network blocks and attach proper CORS/status headers for ES modules.
 */
class WebAssetCacheManager(
    context: Context,
    private val db: ForHappyDatabase,
    private val httpClient: OkHttpClient,
) {
    @Serializable
    data class BridgedHttpResult(
        val status: Int,
        val contentType: String,
        val body: String,
    )

    private val cacheDir = File(context.cacheDir, "forhappy_web_assets").apply { mkdirs() }

    private fun isCoveredHost(host: String): Boolean =
        host == "52dmeme.com" || host == "cc.52dmeme.com" || host == "img.52dmeme.com"

    private fun isCacheableAsset(path: String): Boolean {
        return path.endsWith(".js", ignoreCase = true) ||
            path.endsWith(".mjs", ignoreCase = true) ||
            path.endsWith(".css", ignoreCase = true) ||
            path.startsWith("/stickers/", ignoreCase = true) ||
            path.endsWith(".woff2", ignoreCase = true)
    }

    private fun mimeForPath(path: String): String = when {
        path.endsWith(".js", ignoreCase = true) || path.endsWith(".mjs", ignoreCase = true) -> "application/javascript"
        path.endsWith(".css", ignoreCase = true) -> "text/css"
        path.endsWith(".json", ignoreCase = true) -> "application/json"
        path.endsWith(".png", ignoreCase = true) -> "image/png"
        path.endsWith(".gif", ignoreCase = true) -> "image/gif"
        path.endsWith(".webp", ignoreCase = true) -> "image/webp"
        path.endsWith(".svg", ignoreCase = true) -> "image/svg+xml"
        path.endsWith(".woff2", ignoreCase = true) -> "font/woff2"
        path.endsWith(".html", ignoreCase = true) -> "text/html"
        else -> "application/octet-stream"
    }

    private fun corsHeaders(contentType: String, cacheControl: String? = null): Map<String, String> =
        buildMap {
            put("Access-Control-Allow-Origin", "https://52dmeme.com")
            put("Access-Control-Allow-Credentials", "true")
            put("Content-Type", contentType)
            if (!cacheControl.isNullOrBlank()) put("Cache-Control", cacheControl)
        }

    /**
     * Intercepts static subresources (JS/CSS/fonts/stickers) with full 200 status + CORS headers
     * so `<script type="module">` and dynamic `import()` succeed cleanly in Android WebView.
     */
    fun maybeInterceptWebRequest(request: WebResourceRequest): WebResourceResponse? {
        if (!request.method.equals("GET", ignoreCase = true)) return null
        val url = request.url
        val host = url.host?.lowercase().orEmpty()
        if (!isCoveredHost(host)) return null
        val path = url.encodedPath.orEmpty()
        if (!isCacheableAsset(path)) return null

        val key = sha256(url.toString())
        val cachedFile = File(cacheDir, key)
        if (cachedFile.exists() && cachedFile.length() > 0L) {
            val mime = mimeForPath(path)
            return WebResourceResponse(
                mime,
                "utf-8",
                200,
                "OK",
                corsHeaders(mime, "public, max-age=86400"),
                cachedFile.inputStream(),
            )
        }

        return runCatching {
            val okReq = Request.Builder().url(url.toString()).get().build()
            httpClient.newCall(okReq).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val bytes = resp.body?.bytes() ?: return null
                val mime = resp.header("Content-Type")?.substringBefore(';')?.trim()?.ifBlank { null }
                    ?: mimeForPath(path)
                cachedFile.writeBytes(bytes)
                runBlocking {
                    db.recordAssetCacheMeta(key, url.toString(), mime, bytes.size.toLong())
                }
                WebResourceResponse(
                    mime,
                    "utf-8",
                    200,
                    "OK",
                    corsHeaders(mime, "public, max-age=86400"),
                    ByteArrayInputStream(bytes),
                )
            }
        }.getOrNull()
    }

    /**
     * Full GET interception for [ShelllessWebScreen]: routes main HTML document, API endpoints, ES modules,
     * and images through the Rust ECH+H2 gateway so canvas routes (`/pets`) never stall on plain SNI.
     */
    fun interceptShelllessGet(
        request: WebResourceRequest,
        htmlHeadInjection: String? = null,
    ): WebResourceResponse? {
        if (!request.method.equals("GET", ignoreCase = true)) return null
        val url = request.url
        val host = url.host?.lowercase().orEmpty()
        if (!isCoveredHost(host)) return null
        val path = url.encodedPath.orEmpty()

        if (isCacheableAsset(path)) {
            maybeInterceptWebRequest(request)?.let { return it }
        }

        return runCatching {
            val reqBuilder = Request.Builder().url(url.toString()).get()
            request.requestHeaders?.forEach { (k, v) ->
                if (k.equals("Accept", ignoreCase = true) || k.equals("Accept-Language", ignoreCase = true)) {
                    reqBuilder.header(k, v)
                }
            }
            httpClient.newCall(reqBuilder.build()).execute().use { resp ->
                val rawBytes = resp.body?.bytes() ?: ByteArray(0)
                val rawCt = resp.header("Content-Type").orEmpty()
                val mime = rawCt.substringBefore(';').trim().ifBlank {
                    if (request.isForMainFrame) "text/html" else mimeForPath(path)
                }
                val finalBytes = if (
                    request.isForMainFrame &&
                    mime.equals("text/html", ignoreCase = true) &&
                    !htmlHeadInjection.isNullOrBlank()
                ) {
                    injectIntoHtmlHead(String(rawBytes, Charsets.UTF_8), htmlHeadInjection).toByteArray(Charsets.UTF_8)
                } else {
                    rawBytes
                }

                if (resp.isSuccessful && isCacheableAsset(path) && finalBytes.isNotEmpty()) {
                    val key = sha256(url.toString())
                    File(cacheDir, key).writeBytes(finalBytes)
                }

                val status = if (resp.code in 200..599) resp.code else 200
                val reason = resp.message.ifBlank { "OK" }
                val encoding = if (mime.startsWith("image/") || mime.startsWith("font/") || mime.startsWith("audio/") || mime.startsWith("video/")) {
                    null
                } else {
                    "utf-8"
                }
                WebResourceResponse(
                    mime,
                    encoding,
                    status,
                    reason,
                    corsHeaders(rawCt.ifBlank { mime }, resp.header("Cache-Control")),
                    ByteArrayInputStream(finalBytes),
                )
            }
        }.getOrNull()
    }

    /**
     * Bridges same-origin mutating `fetch` requests (`POST`/`PUT`/`PATCH`/`DELETE`) from WebView
     * through Rust-routed [httpClient] with [PersistentCookieJar] & automatic `Origin` header.
     */
    suspend fun executeBridgedMutation(
        method: String,
        urlString: String,
        contentType: String?,
        bodyText: String?,
    ): BridgedHttpResult = withContext(Dispatchers.IO) {
        val parsed = runCatching { android.net.Uri.parse(urlString) }.getOrNull()
        val host = parsed?.host?.lowercase().orEmpty()
        if (!isCoveredHost(host)) {
            return@withContext BridgedHttpResult(403, "application/json", """{"error":"forbidden host"}""")
        }
        val safeMethod = method.uppercase()
        val mediaType = (contentType?.takeIf { it.isNotBlank() } ?: "application/json; charset=utf-8").toMediaTypeOrNull()
        val reqBody = when (safeMethod) {
            "GET", "HEAD" -> null
            else -> (bodyText ?: "{}").toRequestBody(mediaType)
        }
        val req = Request.Builder()
            .url(urlString)
            .method(safeMethod, reqBody)
            .header("Accept", "application/json, text/plain, */*")
            .build()
        runCatching {
            httpClient.newCall(req).execute().use { resp ->
                val respBody = resp.body?.string().orEmpty()
                val ct = resp.header("Content-Type") ?: "application/json; charset=utf-8"
                BridgedHttpResult(status = resp.code, contentType = ct, body = respBody)
            }
        }.getOrElse { err ->
            BridgedHttpResult(status = 502, contentType = "application/json", body = """{"error":${org.json.JSONObject.quote(err.message ?: "network error")}}""")
        }
    }

    private fun injectIntoHtmlHead(html: String, snippet: String): String {
        val headRegex = Regex("<head[^>]*>", RegexOption.IGNORE_CASE)
        val match = headRegex.find(html)
        return if (match != null) {
            val pos = match.range.last + 1
            html.substring(0, pos) + snippet + html.substring(pos)
        } else {
            snippet + html
        }
    }

    suspend fun clearAll(): Unit = withContext(Dispatchers.IO) {
        cacheDir.listFiles()?.forEach { it.delete() }
        db.clearAssetCacheMeta()
    }

    fun cacheSizeBytes(): Long =
        cacheDir.listFiles()?.sumOf { it.length() } ?: 0L

    private fun sha256(s: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(s.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
