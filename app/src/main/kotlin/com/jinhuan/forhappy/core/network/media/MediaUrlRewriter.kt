package com.jinhuan.forhappy.core.network.media

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.net.URLEncoder

/**
 * Exact Kotlin port of `chat-media-load.js` (`mediaUrl`) and `app.js` (`memeImg`):
 * - Rewrites `/img/{key}` on `52dmeme.com` / `cc.52dmeme.com` (excluding `/img/d/<id>` dynamic avatars)
 *   to static image CDN `https://img.52dmeme.com/img/{key}`.
 * - Supports external proxy fallback `https://52dmeme.com/ext?u=...`.
 */
object MediaUrlRewriter {
    private val ORIGIN_HOST = "52dmeme.com"
    private val CC_HOST = "cc.52dmeme.com"
    private val IMG_HOST = "img.52dmeme.com"
    private val BASE_ORIGIN = "https://52dmeme.com"
    private val IMG_ORIGIN = "https://img.52dmeme.com"

    val DEFAULT_STICKER_EXT = mapOf(
        "bb" to "png",
        "bm" to "png",
        "tieba" to "png",
        "weibo" to "png",
        "qq" to "gif",
        "alu" to "png",
        "ld" to "png",
        "muscle" to "webp",
        "heartgif" to "webp",
        "nga" to "png",
    )

    fun rewriteMediaUrl(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        val trimmed = raw.trim()
        if (trimmed.startsWith("data:", ignoreCase = true) || trimmed.startsWith("blob:", ignoreCase = true)) {
            return trimmed
        }

        val resolved = when {
            trimmed.startsWith("https://", ignoreCase = true) || trimmed.startsWith("http://", ignoreCase = true) -> trimmed
            trimmed.startsWith("//") -> "https:$trimmed"
            trimmed.startsWith("/") -> "$BASE_ORIGIN$trimmed"
            else -> "$BASE_ORIGIN/$trimmed"
        }

        val url = resolved.toHttpUrlOrNull() ?: return resolved
        val host = url.host.lowercase()
        val encodedPath = url.encodedPath

        if ((host == ORIGIN_HOST || host == CC_HOST) && encodedPath.startsWith("/img/")) {
            val key = encodedPath.removePrefix("/img/")
            val validKey = key.isNotEmpty() &&
                !key.startsWith("d/") &&
                !key.contains('\\') &&
                key.split('/').none { seg -> seg == "." || seg == ".." }
            if (validKey) {
                return buildString {
                    append(IMG_ORIGIN)
                    append(encodedPath)
                    if (!url.encodedQuery.isNullOrEmpty()) {
                        append('?').append(url.encodedQuery)
                    }
                }
            }
        }
        return resolved
    }

    fun isLinuxDoExternalHost(host: String?): Boolean {
        val h = host?.lowercase() ?: return false
        return h == "linux.do" || h.endsWith(".linux.do") || h == "ldstatic.com" || h.endsWith(".ldstatic.com")
    }

    /**
     * Resolves media/avatar URLs and routes external Linux.DO (`cdn.ldstatic.com` / `linux.do`)
     * assets through `52dmeme.com/ext?u=` so they travel over the Rust ECH tunnel instead of
     * stalling on direct mobile connections.
     */
    fun rewriteWithExtProxy(raw: String?): String {
        val abs = rewriteMediaUrl(raw)
        if (abs.isBlank()) return ""
        val parsed = abs.toHttpUrlOrNull() ?: return abs
        return if (isLinuxDoExternalHost(parsed.host)) {
            "$BASE_ORIGIN/ext?u=${URLEncoder.encode(abs, Charsets.UTF_8.name())}"
        } else {
            abs
        }
    }

    fun externalFallbackUrl(originalMediaUrl: String): String {
        val abs = rewriteMediaUrl(originalMediaUrl)
        val encoded = URLEncoder.encode(abs, Charsets.UTF_8.name())
        return "$BASE_ORIGIN/ext?u=$encoded"
    }

    fun stickerUrl(pack: String, name: String, ext: String? = null): String {
        val cleanPack = pack.trim().trim('/')
        val cleanName = name.trim().trim('/')
        val chosenExt = ext?.takeIf { it.isNotBlank() } ?: DEFAULT_STICKER_EXT[cleanPack.lowercase()] ?: "png"
        return "$BASE_ORIGIN/stickers/$cleanPack/$cleanName.$chosenExt"
    }

    fun avatarUrl(username: String, rawAvatar: String? = null): String {
        if (!rawAvatar.isNullOrBlank()) {
            return rewriteMediaUrl(rawAvatar)
        }
        val clean = username.trim().ifEmpty { "guest" }
        return "$BASE_ORIGIN/img/d/avatar/${URLEncoder.encode(clean, Charsets.UTF_8.name())}.svg"
    }

    fun isStaticCdnHost(host: String?): Boolean = host?.equals(IMG_HOST, ignoreCase = true) == true
}
