package com.jinhuan.forhappy.core.network.media

import com.jinhuan.forhappy.core.db.ForHappyDatabase
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.util.concurrent.ConcurrentHashMap

/**
 * Handles `[pack:name]` sticker resolution fallback (`/stickers/{pack}/{name}.{png|gif|webp}`):
 * - Rejects SPA fallback `HTTP 200 text/html` responses (the server returns `index.html` instead of 404
 *   when a sticker extension does not exist) so wrong extensions are never cached or fed to Coil's disk cache.
 * - Tries cached extension (if any), requested extension, and remaining candidates in `["png", "gif", "webp"]`.
 * - Persists the first valid `image/...` response and synthesizes HTTP 404 when no candidate is an image.
 */
class StickerFallbackInterceptor(
    private val getCachedExt: (pack: String, name: String) -> String?,
    private val putCachedExt: (pack: String, name: String, ext: String) -> Unit,
    private val deleteCachedExt: (pack: String, name: String) -> Unit = { _, _ -> },
) : Interceptor {

    companion object {
        private val EXT_CANDIDATES = listOf("png", "gif", "webp")
        private val STICKER_PATH_REGEX = Regex("""^/stickers/([^/]+)/([^/]+)\.(png|gif|webp)$""", RegexOption.IGNORE_CASE)

        fun forDatabase(db: ForHappyDatabase): StickerFallbackInterceptor = StickerFallbackInterceptor(
            getCachedExt = { pack, name ->
                runCatching { runBlocking { db.getStickerExt(pack, name) } }.getOrNull()
            },
            putCachedExt = { pack, name, ext ->
                runCatching { runBlocking { db.putStickerExt(pack, name, ext) } }
            },
            deleteCachedExt = { pack, name ->
                runCatching { runBlocking { db.deleteStickerExt(pack, name) } }
            },
        )
    }

    private val memoryExtCache = ConcurrentHashMap<String, String>()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath
        val match = STICKER_PATH_REGEX.matchEntire(path) ?: return chain.proceed(request)

        val pack = match.groupValues[1]
        val name = match.groupValues[2]
        val reqExt = match.groupValues[3].lowercase()
        val cacheKey = "$pack:$name"

        val knownExt = memoryExtCache[cacheKey] ?: getCachedExt(pack, name)?.lowercase()?.also {
            if (it in EXT_CANDIDATES) {
                memoryExtCache[cacheKey] = it
            }
        }

        val candidates = (listOfNotNull(knownExt?.takeIf { it in EXT_CANDIDATES }, reqExt) + EXT_CANDIDATES).distinct()

        for (ext in candidates) {
            val candidateReq = if (ext != reqExt) {
                request.newBuilder()
                    .url(request.url.newBuilder().encodedPath("/stickers/$pack/$name.$ext").build())
                    .build()
            } else {
                request
            }

            val response = chain.proceed(candidateReq)
            if (isValidStickerImage(response)) {
                if (ext != knownExt) {
                    rememberExt(cacheKey, pack, name, ext)
                }
                return response
            }

            // Do not treat transient rate-limit / gateway errors (429 / 5xx) as missing sticker assets.
            if (!isMissingOrHtmlFallback(response)) {
                return response
            }
            response.close()
        }

        memoryExtCache.remove(cacheKey)
        if (knownExt != null) {
            deleteCachedExt(pack, name)
        }
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(404)
            .message("Sticker Not Found")
            .body("".toResponseBody("text/plain".toMediaType()))
            .build()
    }

    private fun isValidStickerImage(response: Response): Boolean {
        if (!response.isSuccessful) return false
        val ct = response.header("Content-Type")?.lowercase().orEmpty()
        return ct.startsWith("image/")
    }

    private fun isMissingOrHtmlFallback(response: Response): Boolean {
        if (response.code == 404) return true
        if (!response.isSuccessful) return false
        val ct = response.header("Content-Type")?.lowercase().orEmpty()
        return !ct.startsWith("image/")
    }

    private fun rememberExt(cacheKey: String, pack: String, name: String, ext: String) {
        memoryExtCache[cacheKey] = ext
        putCachedExt(pack, name, ext)
    }
}
