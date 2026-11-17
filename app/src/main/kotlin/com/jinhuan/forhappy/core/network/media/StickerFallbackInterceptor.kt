package com.jinhuan.forhappy.core.network.media

import com.jinhuan.forhappy.core.db.ForHappyDatabase
import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.ConcurrentHashMap

/**
 * Handles `[pack:name]` sticker resolution fallback (`/stickers/{pack}/{name}.{png|gif|webp}`):
 * - Before sending, if a known working extension was persisted in SQLite, rewrites to that extension directly.
 * - On HTTP 404, tries remaining extensions in `["png", "gif", "webp"]` and persists the first HTTP 200 hit.
 */
class StickerFallbackInterceptor(
    private val db: ForHappyDatabase,
) : Interceptor {

    companion object {
        private val EXT_CANDIDATES = listOf("png", "gif", "webp")
        private val STICKER_PATH_REGEX = Regex("""^/stickers/([^/]+)/([^/]+)\.(png|gif|webp)$""", RegexOption.IGNORE_CASE)
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

        val knownExt = memoryExtCache[cacheKey] ?: runCatching {
            kotlinx.coroutines.runBlocking { db.getStickerExt(pack, name) }
        }.getOrNull()?.also { memoryExtCache[cacheKey] = it }

        val firstExt = knownExt ?: reqExt
        val firstReq = if (firstExt != reqExt) {
            request.newBuilder()
                .url(request.url.newBuilder().encodedPath("/stickers/$pack/$name.$firstExt").build())
                .build()
        } else {
            request
        }

        var response = chain.proceed(firstReq)
        if (response.code != 404) {
            if (response.isSuccessful && knownExt == null) {
                rememberExt(cacheKey, pack, name, firstExt)
            }
            return response
        }

        val remaining = EXT_CANDIDATES.filter { it != firstExt }
        for (altExt in remaining) {
            response.close()
            val altReq = request.newBuilder()
                .url(request.url.newBuilder().encodedPath("/stickers/$pack/$name.$altExt").build())
                .build()
            response = chain.proceed(altReq)
            if (response.code != 404) {
                if (response.isSuccessful) {
                    rememberExt(cacheKey, pack, name, altExt)
                }
                return response
            }
        }
        return response
    }

    private fun rememberExt(cacheKey: String, pack: String, name: String, ext: String) {
        memoryExtCache[cacheKey] = ext
        runCatching {
            kotlinx.coroutines.runBlocking { db.putStickerExt(pack, name, ext) }
        }
    }
}
