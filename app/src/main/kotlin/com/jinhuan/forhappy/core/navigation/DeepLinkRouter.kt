package com.jinhuan.forhappy.core.navigation

import android.content.Intent
import android.net.Uri
import androidx.core.content.IntentCompat
import com.jinhuan.forhappy.core.dynamic.TopbarModuleScanner
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

data class SharedIncomingPayload(
    val uris: List<String>,
    val sharedText: String?,
    val mimeType: String?,
)

/**
 * Pure Kotlin App Links / in-app URI router mapped directly against `52dmeme.com` routes and
 * `docs/redesign/01-information-architecture.md` §5.
 */
sealed interface DeepLinkTarget {
    data object WallRoot : DeepLinkTarget
    data class MemeDetail(val memeId: Long) : DeepLinkTarget
    data class TagFeed(val tag: String) : DeepLinkTarget
    data class AuthorFeed(val author: String) : DeepLinkTarget
    data object SeriesIndex : DeepLinkTarget
    data class SeriesFeed(val series: String) : DeepLinkTarget
    data class MessagesRoot(val filter: String = "all") : DeepLinkTarget
    data class GroupChat(val groupId: Long, val aroundMessageId: Long? = null) : DeepLinkTarget
    data class DirectMessage(val threadOrUserId: Long, val isNewPeer: Boolean = false) : DeepLinkTarget
    data class DiscoverSubApp(val moduleId: String, val route: String, val title: String) : DeepLinkTarget
    data class WebModalRoute(val route: String, val title: String) : DeepLinkTarget
    data object Road : DeepLinkTarget
    data object Me : DeepLinkTarget
}

object DeepLinkRouter {
    private val ALLOWED_HOSTS = setOf("52dmeme.com", "www.52dmeme.com", "cc.52dmeme.com")

    fun parseDeepLinkUri(rawUri: String?): DeepLinkTarget? {
        val text = rawUri?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val uri = runCatching { URI(text) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase()
        val host = uri.host?.lowercase()
        if (scheme != null && scheme != "http" && scheme != "https") {
            return null
        }
        if (host != null && host !in ALLOWED_HOSTS) {
            return null
        }

        val rawPath = uri.rawPath?.takeIf { it.isNotBlank() } ?: "/"
        val normalizedPath = normalizePath(rawPath)
        val query = parseQueryParams(uri.rawQuery)

        if (normalizedPath == "/" || TopbarModuleScanner.isRetiredRoute(normalizedPath)) {
            if (normalizedPath == "/arcade" || normalizedPath.startsWith("/arcade/")) {
                return DeepLinkTarget.DiscoverSubApp("arcade", "/arcade", "谷内游乐与竞技")
            }
            return DeepLinkTarget.WallRoot
        }

        Regex("""^/m/(\d+)$""").matchEntire(normalizedPath)?.let { m ->
            val memeId = m.groupValues[1].toLongOrNull() ?: return DeepLinkTarget.WallRoot
            return DeepLinkTarget.MemeDetail(memeId)
        }

        if (normalizedPath.startsWith("/t/")) {
            val tag = urlDecode(normalizedPath.removePrefix("/t/")).trim()
            return if (tag.isNotBlank()) DeepLinkTarget.TagFeed(tag) else DeepLinkTarget.WallRoot
        }

        if (normalizedPath.startsWith("/u/")) {
            val author = urlDecode(normalizedPath.removePrefix("/u/")).trim()
            return if (author.isNotBlank()) DeepLinkTarget.AuthorFeed(author) else DeepLinkTarget.WallRoot
        }

        if (normalizedPath == "/series") {
            return DeepLinkTarget.SeriesIndex
        }

        if (normalizedPath.startsWith("/s/")) {
            val series = urlDecode(normalizedPath.removePrefix("/s/")).trim()
            return if (series.isNotBlank()) DeepLinkTarget.SeriesFeed(series) else DeepLinkTarget.SeriesIndex
        }

        if (normalizedPath == "/chat") {
            return DeepLinkTarget.MessagesRoot("group")
        }

        Regex("""^/chat/(\d+)$""").matchEntire(normalizedPath)?.let { m ->
            val gid = m.groupValues[1].toLongOrNull() ?: return DeepLinkTarget.MessagesRoot("group")
            val around = (query["around"] ?: query["msg"] ?: query["message_id"])?.toLongOrNull()
            return DeepLinkTarget.GroupChat(groupId = gid, aroundMessageId = around)
        }

        if (normalizedPath == "/dm") {
            return DeepLinkTarget.MessagesRoot("dm")
        }

        Regex("""^/dm/new/(-?\d+)$""").matchEntire(normalizedPath)?.let { m ->
            val uid = m.groupValues[1].toLongOrNull() ?: return DeepLinkTarget.MessagesRoot("dm")
            return DeepLinkTarget.DirectMessage(threadOrUserId = uid, isNewPeer = true)
        }

        Regex("""^/dm/(\d+)$""").matchEntire(normalizedPath)?.let { m ->
            val threadId = m.groupValues[1].toLongOrNull() ?: return DeepLinkTarget.MessagesRoot("dm")
            return DeepLinkTarget.DirectMessage(threadOrUserId = threadId, isNewPeer = false)
        }

        if (normalizedPath == "/pets" || normalizedPath == "/pet-isle" || normalizedPath == "/valley") {
            val frag = uri.fragment?.trim().orEmpty()
            val targetRoute = if (frag == "battle") "/pets#battle" else "/pets"
            return DeepLinkTarget.DiscoverSubApp("pets", targetRoute, if (frag == "battle") "大肥鱼擂台" else "鱼鱼谷")
        }

        if (normalizedPath == "/community-updates" || normalizedPath == "/community-updates.html") {
            return DeepLinkTarget.DiscoverSubApp("community_updates", "/community-updates", "社区更新")
        }

        if (normalizedPath == "/leaderboard" || normalizedPath == "/wealth-rank") {
            return DeepLinkTarget.DiscoverSubApp("wealth", "/wealth-rank", "站内财富榜")
        }

        if (normalizedPath == "/cards") {
            return DeepLinkTarget.DiscoverSubApp("gacha", "/gacha", "抽卡与集卡")
        }

        if (normalizedPath == "/raffle") {
            return DeepLinkTarget.DiscoverSubApp("gacha", "/me#fh-raffle", "历史称号抽奖")
        }

        if (normalizedPath == "/slime" || normalizedPath == "/arcade" || normalizedPath.startsWith("/arcade/")) {
            return DeepLinkTarget.DiscoverSubApp("arcade", "/arcade", "谷内游乐与竞技")
        }

        if (normalizedPath == "/novels" || normalizedPath.startsWith("/novels/")) {
            val isStudio = normalizedPath.startsWith("/novels/studio")
            return DeepLinkTarget.DiscoverSubApp(
                moduleId = if (isStudio) "novel_studio" else "novels",
                route = fullPathWithQuery(normalizedPath, uri.rawQuery),
                title = if (isStudio) "书房创作工坊" else "尽欢书房",
            )
        }

        if (normalizedPath == "/road") {
            val itemId = query["item"]?.toLongOrNull()
            return if (itemId != null && itemId > 0L) {
                DeepLinkTarget.DiscoverSubApp("road", "/road?item=$itemId", "鞭打详情")
            } else {
                DeepLinkTarget.Road
            }
        }

        if (normalizedPath == "/me/invites") {
            return DeepLinkTarget.WebModalRoute("/me/invites", "邀请码与记录")
        }

        if (normalizedPath == "/me/account" || normalizedPath.startsWith("/me/account/")) {
            return DeepLinkTarget.WebModalRoute(fullPathWithQuery(normalizedPath, uri.rawQuery), "账号安全")
        }

        if (normalizedPath == "/me" && query["section"] == "admin") {
            return DeepLinkTarget.WebModalRoute(fullPathWithQuery(normalizedPath, uri.rawQuery), "站务管理面板")
        }

        if (normalizedPath == "/me" || normalizedPath.startsWith("/me/")) {
            return DeepLinkTarget.Me
        }

        if (normalizedPath == "/admin" || normalizedPath.startsWith("/admin/")) {
            return if (normalizedPath == "/admin") {
                DeepLinkTarget.WebModalRoute("/me?section=admin", "站务管理面板")
            } else {
                val adminTitle = when (normalizedPath) {
                    "/admin/ai" -> "AI 接入与模型管理"
                    "/admin/invites" -> "全站邀请码管理"
                    "/admin/flags" -> "站点功能开关"
                    else -> "管理后台"
                }
                DeepLinkTarget.WebModalRoute(fullPathWithQuery(normalizedPath, uri.rawQuery), adminTitle)
            }
        }

        return DeepLinkTarget.WallRoot
    }

    private fun fullPathWithQuery(normalizedPath: String, rawQuery: String?): String =
        if (rawQuery.isNullOrBlank()) normalizedPath else "$normalizedPath?$rawQuery"

    private fun normalizePath(path: String): String {
        val prefixed = if (path.startsWith('/')) path else "/$path"
        return if (prefixed.length > 1) prefixed.trimEnd('/') else prefixed
    }

    private fun urlDecode(raw: String): String =
        runCatching { URLDecoder.decode(raw, StandardCharsets.UTF_8.name()) }.getOrDefault(raw)

    private fun parseQueryParams(rawQuery: String?): Map<String, String> {
        if (rawQuery.isNullOrBlank()) return emptyMap()
        val out = LinkedHashMap<String, String>()
        for (pair in rawQuery.split('&')) {
            if (pair.isBlank()) continue
            val idx = pair.indexOf('=')
            val k = if (idx >= 0) urlDecode(pair.substring(0, idx)) else urlDecode(pair)
            val v = if (idx >= 0) urlDecode(pair.substring(idx + 1)) else ""
            if (k.isNotBlank()) out[k] = v
        }
        return out
    }

    fun extractSharePayload(
        action: String?,
        mimeType: String?,
        extraText: String?,
        singleStreamUri: String?,
        multiStreamUris: List<String>,
        clipUris: List<String>,
    ): SharedIncomingPayload? {
        if (action != "android.intent.action.SEND" && action != "android.intent.action.SEND_MULTIPLE") {
            return null
        }
        val uris = linkedSetOf<String>()
        singleStreamUri?.trim()?.takeIf { it.isNotBlank() }?.let { uris += it }
        multiStreamUris.forEach { raw -> raw.trim().takeIf { it.isNotBlank() }?.let { uris += it } }
        clipUris.forEach { raw -> raw.trim().takeIf { it.isNotBlank() }?.let { uris += it } }
        val text = extraText?.trim()?.takeIf { it.isNotBlank() }
        if (uris.isEmpty() && text == null) return null
        return SharedIncomingPayload(
            uris = uris.toList(),
            sharedText = text,
            mimeType = mimeType?.trim()?.takeIf { it.isNotBlank() },
        )
    }

    fun parseShareIntent(intent: Intent?): SharedIncomingPayload? {
        if (intent == null) return null
        val singleUri = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)?.toString()
        val multiUris = IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
            ?.map { it.toString() }
            .orEmpty()
        val clipUris = buildList {
            val clip = intent.clipData
            if (clip != null) {
                for (i in 0 until clip.itemCount) {
                    clip.getItemAt(i)?.uri?.toString()?.let { add(it) }
                }
            }
        }
        val extraText = intent.getStringExtra(Intent.EXTRA_TEXT)
            ?: intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
        return extractSharePayload(
            action = intent.action,
            mimeType = intent.type,
            extraText = extraText,
            singleStreamUri = singleUri,
            multiStreamUris = multiUris,
            clipUris = clipUris,
        )
    }
}
