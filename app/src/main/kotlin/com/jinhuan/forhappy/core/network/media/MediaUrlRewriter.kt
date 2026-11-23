package com.jinhuan.forhappy.core.network.media

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.net.URLEncoder
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

data class AvatarViewSpec(
    val cleanUrl: String,
    val width: Float,
    val height: Float,
    val rotationDeg: Int,
    val zoom: Float,
    val x: Float,
    val y: Float,
    val scale: Float,
    val bw: Float,
    val bh: Float,
    val tx: Float,
    val ty: Float,
)

/**
 * Exact Kotlin port of `chat-media-load.js` (`mediaUrl`) and `app.js` (`memeImg`):
 * - Rewrites `/img/{key}` on `52dmeme.com` / `cc.52dmeme.com` (excluding `/img/d/<id>` dynamic avatars)
 *   to static image CDN `https://img.52dmeme.com/img/{key}`.
 * - Supports external proxy fallback `https://52dmeme.com/ext?u=...`.
 */
object MediaUrlRewriter {
    private val AVATAR_FRAGMENT_RE = Regex("""#av=([\d.,-]+)$""")
    private val VALID_ROTATIONS = setOf(0, 90, 180, 270)
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

    /**
     * Parses `52dmeme.com/avatar-view.js` `#av=width,height,rotation,zoom,x,y` fragments so cropped
     * or rotated avatars render identically to the web frontend without re-encoding animated images.
     */
    fun parseAvatarViewSpec(raw: String?): AvatarViewSpec? {
        if (raw.isNullOrBlank()) return null
        val trimmed = raw.trim()
        val match = AVATAR_FRAGMENT_RE.find(trimmed) ?: return null
        val parts = match.groupValues[1].split(',').mapNotNull { it.toFloatOrNull()?.takeIf { v -> v.isFinite() } }
        if (parts.size != 6) return null
        val w = parts[0]
        val h = parts[1]
        val rotRaw = parts[2]
        if (w < 1f || h < 1f || w > 4096f || h > 4096f) return null
        val roundedRot = rotRaw.roundToInt()
        if (abs(rotRaw - roundedRot.toFloat()) > 1e-3f) return null
        val rotNorm = ((roundedRot % 360) + 360) % 360
        if (rotNorm !in VALID_ROTATIONS) return null
        val zoomRaw = parts[3]
        val xRaw = parts[4]
        val yRaw = parts[5]
        if (zoomRaw < 1f || zoomRaw > 3f || xRaw < 0f || xRaw > 1f || yRaw < 0f || yRaw > 1f) return null

        val zoom = zoomRaw.coerceIn(1f, 3f)
        val x = xRaw.coerceIn(0f, 1f)
        val y = yRaw.coerceIn(0f, 1f)
        val rw = if (rotNorm % 180 != 0) h else w
        val rh = if (rotNorm % 180 != 0) w else h
        val scale = zoom / min(rw, rh)
        val bw = rw * scale
        val bh = rh * scale
        val tx = (1f - bw) * (x - 0.5f)
        val ty = (1f - bh) * (y - 0.5f)
        val clean = rewriteWithExtProxy(trimmed.substringBefore('#'))
        return AvatarViewSpec(
            cleanUrl = clean,
            width = w,
            height = h,
            rotationDeg = rotNorm,
            zoom = zoom,
            x = x,
            y = y,
            scale = scale,
            bw = bw,
            bh = bh,
            tx = tx,
            ty = ty,
        )
    }
}
