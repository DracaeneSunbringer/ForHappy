package com.jinhuan.forhappy.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

val ForHappyJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
    encodeDefaults = true
}

const val BOOST_MAX_LEN = 12

val BOOST_PRESETS = listOf(
    "善", "甚好", "欢欢", "妈妈", "666", "太地狱了", "神！",
    "挂！", "我去~", "大大大", "电！！", "不讲不讲", "魔了",
)

val PICKED_REACTIONS = listOf(
    "❤️", "👍", "😆", "😮", "👏", "🎊", "🤗",
    "ld:distorted_face", "ld:tieba_087", "ld:bili_057",
)

private val REPLY_STICKER_COUNT_RE = Regex("""\[([A-Za-z0-9_-]{1,32}):([A-Za-z0-9_-]{1,48})]""")

/** Counts display units for `≤12`-char quick replies/boosts, treating `[pack:name]` stickers as 1 unit. */
fun countReplyUnits(raw: String): Int {
    val normalized = REPLY_STICKER_COUNT_RE.replace(raw.trim(), "□")
    return normalized.codePointCount(0, normalized.length)
}

@Serializable
data class RustNetConfig(
    val selectStateJson: String = "",
    val customNodes: List<CustomEdgeNode> = emptyList(),
    val ipv6Mode: String = "auto",
    val echEnabled: Boolean = true,
    val networkSig: String = "default",
)

@Serializable
data class CustomEdgeNode(
    val ip: String,
    val note: String = "",
    val enabled: Boolean = true,
)

@Serializable
data class RustNetStatus(
    val running: Boolean = false,
    val port: Int = 0,
    val phase: String = "idle",
    val winnerIp: String? = null,
    val latencyMs: Int? = null,
    val echAccepted: Boolean = false,
    val echKeyCount: Int = 0,
    val winnerSource: String? = null,
    val lastError: String? = null,
    val selectStateJson: String = "",
    val logs: List<String> = emptyList(),
)

@Serializable
data class MeUser(
    val id: Long = 0L,
    val username: String = "",
    val name: String? = null,
    val nick: String? = null,
    val avatar: String? = null,
    val bio: String? = null,
    val role: String? = null,
    val deco: JsonElement? = null,
    @SerialName("trust_level") val trustLevel: Int = 0,
    @SerialName("is_admin") val isAdmin: Boolean = false,
    val fancy: JsonElement? = null,
    @SerialName("linuxdo_id") val linuxdoId: Long? = null,
    @SerialName("notif_prefs") val notifPrefs: Map<String, Boolean> = emptyMap(),
) {
    val displayName: String
        get() = name?.takeIf { it.isNotBlank() }
            ?: nick?.takeIf { it.isNotBlank() }
            ?: username.ifBlank { "欢友" }

    fun isNotifEnabled(key: String): Boolean = notifPrefs[key] != false
}

@Serializable
data class ChatUser(
    val id: Long = 0L,
    val username: String = "",
    val name: String? = null,
    val nick: String? = null,
    val avatar: String? = null,
    @SerialName("is_admin") val isAdmin: Boolean = false,
    val fancy: JsonElement? = null,
    val deco: JsonElement? = null,
) {
    val displayName: String
        get() = name?.takeIf { it.isNotBlank() }
            ?: nick?.takeIf { it.isNotBlank() }
            ?: username.ifBlank { "欢友" }
}

@Serializable
data class MeResponse(
    val user: MeUser? = null,
    val now: Double? = null,
)

@Serializable
data class AuthConfigResponse(
    val github: Boolean = false,
    val google: Boolean = false,
    @SerialName("turnstile_site_key") val turnstileSiteKey: String? = null,
)

@Serializable
data class AnchorTag(
    val id: Long = 0L,
    val name: String = "",
    val slug: String = "",
    val kind: String = "",
)

@Serializable
data class DayLatest(
    val id: Long = 0L,
    val at: Long = 0L,
    val by: String? = null,
    val text: String? = null,
    val src: Boolean = false,
)

@Serializable
data class DayEntry(
    val day: String = "",
    val count: Int = 0,
    val n: Int = 0,
    val unseen: Int = 0,
    val latest: DayLatest? = null,
    val items: List<MemeItem> = emptyList(),
) {
    val totalCount: Int get() = if (n > 0) n else count
}

@Serializable
data class MemesPageResponse(
    val items: List<MemeItem> = emptyList(),
    val offset: Int = 0,
    val more: Boolean = false,
)

@Serializable
data class SeenVisitEntry(
    val id: Long,
    val at: Long,
)

@Serializable
data class DaysResponse(
    val days: List<DayEntry> = emptyList(),
    val hasMore: Boolean = false,
    val nextCursor: String? = null,
    val now: Double? = null,
)

@Serializable
data class MemeMediaItem(
    val u: String? = null,
    val t: String? = null,
    val o: String? = null,
    val v: String? = null,
    val k: String? = null,
    val w: Int? = null,
    val h: Int? = null,
    val b: Boolean = false,
)

@Serializable
data class MemeQuickReply(
    val username: String = "",
    val name: String? = null,
    val avatar: String? = null,
    val text: String = "",
    val n: Int = 1,
    val mine: Boolean = false,
) {
    val displayAuthor: String
        get() = name?.takeIf { it.isNotBlank() } ?: username.ifBlank { "佚名" }
}

@Serializable
data class PuzzleLayerData(
    val text: String? = null,
    val algo: String? = null,
    @SerialName("key_hint") val keyHint: String? = null,
    val img: String? = null,
    val pw: Boolean = false,
    val options: List<String> = emptyList(),
)

@Serializable
data class PuzzleLayer(
    val idx: Int = 0,
    val kind: String = "pass",
    val prompt: String? = null,
    val note: String? = null,
    val data: PuzzleLayerData? = null,
)

@Serializable
data class PuzzleSpec(
    val id: Long = 0L,
    val status: String = "open",
    @SerialName("reward_kind") val rewardKind: String = "secret",
    @SerialName("reward_type") val rewardType: String? = null,
    val reward: String? = null,
    val hint: String? = null,
    val layer: Int = 0,
    @SerialName("layer_count") val layerCount: Int = 1,
    @SerialName("winners_count") val winnersCount: Int = 0,
    @SerialName("max_winners") val maxWinners: Int? = null,
    @SerialName("codes_left") val codesLeft: Int? = null,
    @SerialName("ends_at") val endsAt: Long? = null,
    val solved: Boolean = false,
    @SerialName("is_owner") val isOwner: Boolean = false,
    val current: PuzzleLayer? = null,
)

@Serializable
data class MemeItem(
    val id: Long = 0L,
    val day: String? = null,
    val kind: String? = null,
    val layout: String? = null,
    val title: String? = null,
    val text: String? = null,
    val content: String? = null,
    @SerialName("text_cut") val textCut: Boolean = false,
    val caption: String? = null,
    val body: String? = null,
    val image: String? = null,
    val thumb: String? = null,
    val images: List<MemeMediaItem> = emptyList(),
    @SerialName("image_count") val imageCount: Int = 0,
    val media: String? = null,
    @SerialName("media_type") val mediaType: String? = null,
    val mime: String? = null,
    val ext: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val nsfw: Int = 0,
    val sensitive: Boolean = false,
    val spoiler: Boolean = false,
    val views: Int = 0,
    val likes: Int = 0,
    @SerialName("like_count") val likeCount: Int = 0,
    @SerialName("ld_likes") val ldLikes: Int = 0,
    val liked: Boolean = false,
    val favs: Int = 0,
    val faved: Boolean = false,
    @SerialName("fav_folders") val favFolders: List<Long> = emptyList(),
    val comments: Int = 0,
    @SerialName("comment_count") val commentCount: Int = 0,
    @SerialName("reply_count") val replyCount: Int = 0,
    @SerialName("reply_list") val replyList: List<MemeQuickReply> = emptyList(),
    val replies: List<MemeQuickReply> = emptyList(),
    @SerialName("my_reply") val myReply: String? = null,
    @SerialName("can_manage") val canManage: Boolean = false,
    val reactions: List<ChatReaction> = emptyList(),
    val tags: List<String> = emptyList(),
    @SerialName("author_id") val authorId: Long? = null,
    @SerialName("author_name") val authorName: String? = null,
    @SerialName("author_nick") val authorNick: String? = null,
    @SerialName("author_username") val authorUsername: String? = null,
    @SerialName("author_avatar") val authorAvatar: String? = null,
    @SerialName("created_at") val createdAt: Long? = null,
    @SerialName("seen_at") val seenAt: Long? = null,
    @SerialName("source_url") val sourceUrl: String? = null,
    val puzzle: PuzzleSpec? = null,
) {
    val primaryImageUrl: String?
        get() = thumb?.takeIf { it.isNotBlank() }
            ?: images.firstOrNull()?.let { it.t ?: it.u }?.takeIf { it.isNotBlank() }
            ?: image?.takeIf { it.isNotBlank() }
            ?: media?.takeIf { it.isNotBlank() }

    val detailImageUrls: List<String>
        get() = if (images.isNotEmpty()) {
            images.mapNotNull { it.u ?: it.t }.filter { it.isNotBlank() }
        } else {
            listOfNotNull(image ?: thumb ?: media).filter { it.isNotBlank() }
        }

    val effectiveWidth: Int? get() = images.firstOrNull()?.w ?: width
    val effectiveHeight: Int? get() = images.firstOrNull()?.h ?: height

    val totalLikes: Int get() = if (likeCount + ldLikes > 0) likeCount + ldLikes else likes
    val totalComments: Int get() = if (commentCount > 0) commentCount else comments
    val isFaved: Boolean get() = faved || favFolders.isNotEmpty()
    val isRisky: Boolean get() = sensitive || spoiler || nsfw != 0

    val authorDisplay: String
        get() = authorNick?.takeIf { it.isNotBlank() }
            ?: authorName?.takeIf { it.isNotBlank() }
            ?: authorUsername?.takeIf { it.isNotBlank() }
            ?: "佚名"

    val safeLabel: String
        get() = title?.takeIf { it.isNotBlank() }
            ?: text?.takeIf { it.isNotBlank() }
            ?: content?.takeIf { it.isNotBlank() }
            ?: caption?.takeIf { it.isNotBlank() }
            ?: body?.takeIf { it.isNotBlank() }
            ?: "No.$id"

    fun effectiveQuickReplies(me: MeUser? = null): List<MemeQuickReply> {
        val base = replyList.ifEmpty { replies }
        if (!base.any { it.mine } && !myReply.isNullOrBlank()) {
            return base + MemeQuickReply(
                username = me?.username.orEmpty(),
                name = me?.displayName ?: "我",
                avatar = me?.avatar,
                text = myReply,
                n = 1,
                mine = true,
            )
        }
        return base
    }
}

@Serializable
data class MemeDetailResponse(
    val meme: MemeItem? = null,
    val item: MemeItem? = null,
    val comments: List<MemeComment> = emptyList(),
    val now: Double? = null,
) {
    val resolvedMeme: MemeItem? get() = meme ?: item
}

@Serializable
data class MemeComment(
    val id: Long = 0L,
    @SerialName("meme_id") val memeId: Long = 0L,
    @SerialName("user_id") val userId: Long = 0L,
    val username: String = "",
    val name: String? = null,
    val nick: String? = null,
    val avatar: String? = null,
    @SerialName("trust_level") val trustLevel: Int = 0,
    val kind: String? = null,
    @SerialName("can_delete") val canDelete: Boolean = false,
    @SerialName("reply_to") val replyTo: ChatReplyPreview? = null,
    val body: String = "",
    @SerialName("created_at") val createdAt: Long = 0L,
) {
    val displayAuthor: String
        get() = name?.takeIf { it.isNotBlank() }
            ?: nick?.takeIf { it.isNotBlank() }
            ?: username.ifBlank { "欢友" }
}

@Serializable
data class ChatGroupLatest(
    val id: Long = 0L,
    val at: Long = 0L,
    val by: String? = null,
    val text: String? = null,
)

@Serializable
data class ChatGroup(
    val id: Long = 0L,
    val name: String = "",
    val slug: String? = null,
    val desc: String? = null,
    val unread: Int = 0,
    @SerialName("at_me") val atMe: Int = 0,
    @SerialName("last_read_id") val lastReadId: Long = 0L,
    @SerialName("last_read") val lastRead: Long = 0L,
    @SerialName("latest_id") val latestId: Long = 0L,
    val latest: ChatGroupLatest? = null,
    @SerialName("member_count") val memberCount: Int = 0,
    val joined: Boolean = true,
    val muted: Boolean = false,
    @SerialName("has_pending_join") val hasPendingJoin: Boolean = false,
) {
    val effectiveLastRead: Long get() = maxOf(lastReadId, lastRead)
    val effectiveLatestId: Long get() = maxOf(latestId, latest?.id ?: 0L)
}

@Serializable
data class ChatGroupsResponse(
    val groups: List<ChatGroup> = emptyList(),
    val now: Double? = null,
)

@Serializable
data class ChatMedia(
    val id: Long = 0L,
    val url: String = "",
    val u: String? = null,
    val k: String? = null,
    val v: String? = null,
    val o: String? = null,
    val n: String? = null,
    val s: Long? = null,
    val mime: String? = null,
    val name: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val w: Int? = null,
    val h: Int? = null,
    val b: Boolean = false,
) {
    val resolvedUrl: String get() = u?.takeIf { it.isNotBlank() } ?: url
    val resolvedName: String? get() = name?.takeIf { it.isNotBlank() } ?: n
}

@Serializable
data class ChatReplyPreview(
    val id: Long = 0L,
    val body: String = "",
    val text: String? = null,
    @SerialName("user_id") val userId: Long? = null,
    @SerialName("sender_id") val senderId: Long? = null,
    val name: String? = null,
    val nick: String? = null,
    val username: String? = null,
    val avatar: String? = null,
    val deleted: Boolean = false,
    val image: JsonElement? = null,
    val thumb: String? = null,
    val file: String? = null,
) {
    val hasImage: Boolean
        get() {
            val prim = image as? JsonPrimitive ?: return false
            return prim.booleanOrNull == true ||
                prim.intOrNull == 1 ||
                (prim.isString && prim.contentOrNull?.isNotBlank() == true &&
                    prim.contentOrNull != "false" && prim.contentOrNull != "0")
        }

    val resolvedAuthor: String
        get() = name?.takeIf { it.isNotBlank() }
            ?: nick?.takeIf { it.isNotBlank() }
            ?: username?.takeIf { it.isNotBlank() }
            ?: "引用"

    val resolvedText: String
        get() = if (deleted) {
            "消息已撤回"
        } else {
            text?.takeIf { it.isNotBlank() }
                ?: body.ifBlank {
                    when {
                        file != null -> "[文件] $file"
                        hasImage || thumb != null -> "[图片]"
                        else -> "…"
                    }
                }
        }
}

@Serializable
data class ChatReaction(
    val emoji: String = "",
    val count: Int = 0,
    val mine: Boolean = false,
    val who: List<ChatUser> = emptyList(),
)

@Serializable
data class ChatBoost(
    val ai: Boolean = false,
    val status: String? = null,
    @SerialName("user_id") val userId: Long = 0L,
    val username: String = "",
    val name: String? = null,
    val avatar: String? = null,
    val text: String = "",
    val score: Int? = null,
    val at: Long = 0L,
    val mine: Boolean = false,
    val deco: JsonElement? = null,
) {
    val displayAuthor: String
        get() = if (ai) "欢欢" else name?.takeIf { it.isNotBlank() } ?: username.ifBlank { "群友" }
}

@Serializable
data class ChatReplyItem(
    val id: Long = 0L,
    val username: String = "",
    val name: String? = null,
    val avatar: String? = null,
    val text: String? = null,
    val image: JsonElement? = null,
    val at: Long = 0L,
) {
    val displayAuthor: String
        get() = name?.takeIf { it.isNotBlank() } ?: username.ifBlank { "群友" }

    val hasImage: Boolean
        get() = (image as? JsonPrimitive)?.let { prim ->
            prim.booleanOrNull == true || !prim.contentOrNull.isNullOrBlank()
        } ?: false
}

@Serializable
data class ChatRepliesResponse(
    val items: List<ChatReplyItem> = emptyList(),
)

@Serializable
data class ChatMessage(
    val id: Long = 0L,
    @SerialName("group_id") val groupId: Long = 0L,
    @SerialName("user_id") val userId: Long = 0L,
    val user: ChatUser? = null,
    val username: String = "",
    val nick: String? = null,
    val avatar: String? = null,
    val title: String? = null,
    val body: String = "",
    val image: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val mine: Boolean = false,
    val deleted: Boolean = false,
    val edited: Boolean = false,
    @SerialName("edited_at") val editedAt: Long? = null,
    @SerialName("can_edit") val canEdit: Boolean = false,
    @SerialName("can_delete") val canDelete: Boolean = false,
    val floor: Int? = null,
    @SerialName("meme_id") val memeId: Long? = null,
    @SerialName("system_kind") val systemKind: String? = null,
    @SerialName("system_ref") val systemRef: String? = null,
    @SerialName("reply_to") val replyTo: ChatReplyPreview? = null,
    @SerialName("reply_count") val replyCount: Int = 0,
    @SerialName("media_expired") val mediaExpired: Boolean = false,
    val media: List<ChatMedia> = emptyList(),
    val reactions: List<ChatReaction> = emptyList(),
    val boosts: List<ChatBoost> = emptyList(),
    val redpack: JsonObject? = null,
    @SerialName("road_id") val roadId: Long? = null,
    @SerialName("created_at") val createdAt: Long = 0L,
    @SerialName("updated_at") val updatedAt: Long? = null,
    val localId: String? = null,
    val deliveryState: String = "sent", // "sending" | "failed" | "sent"
) {
    val effectiveUserId: Long
        get() = user?.id?.takeIf { it > 0L } ?: userId

    val displayAuthor: String
        get() = user?.displayName
            ?: nick?.takeIf { it.isNotBlank() }
            ?: username.ifBlank { "欢友" }

    val resolvedUsername: String
        get() = user?.username?.takeIf { it.isNotBlank() } ?: username

    val resolvedAvatar: String?
        get() = user?.avatar?.takeIf { it.isNotBlank() } ?: avatar?.takeIf { it.isNotBlank() }

    val resolvedTitle: String?
        get() = title?.takeIf { it.isNotBlank() }
            ?: runCatching { user?.deco?.jsonObject?.get("t")?.jsonPrimitive?.contentOrNull }
                .getOrNull()
                ?.takeIf { it.isNotBlank() }

    val allMedia: List<ChatMedia>
        get() = if (media.isNotEmpty()) {
            media
        } else if (!image.isNullOrBlank()) {
            listOf(ChatMedia(u = image, w = width, h = height))
        } else {
            emptyList()
        }
}

@Serializable
data class ChatMessagesResponse(
    val items: List<ChatMessage> = emptyList(),
    val changed: List<ChatMessage> = emptyList(),
    val more: Boolean = false,
    @SerialName("more_after") val moreAfter: Boolean = false,
    @SerialName("last_read") val lastRead: Long = 0L,
    val total: Long? = null,
    val now: Double? = null,
)

@Serializable
data class ChatSearchResponse(
    val items: List<ChatMessage> = emptyList(),
    val more: Boolean = false,
)

@Serializable
data class TagStatItem(
    val tag: String = "",
    val count: Int = 0,
    val n: Int = 0,
) {
    val totalCount: Int get() = if (n > 0) n else count
}

@Serializable
data class TagsResponse(
    val tags: List<TagStatItem> = emptyList(),
)

/** Gallery filter & sort parameters matching `app.js` `listParams`. */
data class WallFilterState(
    val sort: String = "new",        // "new" | "hot" | "old"
    val unseenOnly: Boolean = false,
    val textFilter: String = "",     // "" | "only" | "0"
    val nsfwMode: String = "blur",   // "blur" | "hide"
    val tags: Set<String> = emptySet(),
    val author: String = "",
    val searchQuery: String = "",
) {
    val isByDay: Boolean get() = sort != "hot"

    val activeFilterCount: Int
        get() = listOf(
            sort != "new",
            unseenOnly,
            textFilter.isNotBlank(),
            nsfwMode == "hide",
            tags.isNotEmpty(),
            author.isNotBlank(),
        ).count { it }
}

@Serializable
data class ChatPollCursor(
    val groupId: Long,
    val lastId: Long = 0L,
    val sinceServerSec: Long = 0L,
)

@Serializable
data class StickerPack(
    val key: String = "",
    val label: String = "",
    val tab: String? = null,
    val ext: String = "png",
    val hidden: Boolean = false,
    val items: List<String> = emptyList(),
) {
    /** Unique tab identifier (multiple `ld` packs share `key="ld"` with distinct `tab`). */
    val tabKey: String
        get() = tab?.takeIf { it.isNotBlank() } ?: "${key}_${label}"
}

@Serializable
data class StickerEntry(
    val pack: String = "",
    val name: String = "",
    val ext: String? = null,
    val url: String? = null,
    @SerialName("message_id") val messageId: Long = 0L,
    val index: Int = 0,
    val animated: Boolean = false,
    val unavailable: Boolean = false,
)

@Serializable
data class DmThread(
    val id: Long = 0L,
    @SerialName("peer_id") val peerId: Long = 0L,
    @SerialName("peer_name") val peerName: String? = null,
    @SerialName("peer_username") val peerUsername: String? = null,
    @SerialName("peer_avatar") val peerAvatar: String? = null,
    @SerialName("last_body") val lastBody: String? = null,
    @SerialName("last_sender") val lastSender: Long? = null,
    val unread: Int = 0,
    @SerialName("updated_at") val updatedAt: Long = 0L,
    @SerialName("last_at") val lastAt: Long = 0L,
)

@Serializable
data class DmMessage(
    val id: Long = 0L,
    @SerialName("thread_id") val threadId: Long = 0L,
    @SerialName("sender_id") val senderId: Long = 0L,
    val body: String = "",
    val deleted: Boolean = false,
    @SerialName("reply_to") val replyTo: ChatReplyPreview? = null,
    val media: List<ChatMedia> = emptyList(),
    @SerialName("created_at") val createdAt: Long = 0L,
)

data class NotifCategoryTab(
    val id: String,
    val label: String,
    val kinds: Set<String>? = null,
)

val NOTIF_CATEGORY_TABS = listOf(
    NotifCategoryTab("all", "全部", null),
    NotifCategoryTab("at", "@我", setOf("mention", "chat_at", "post_at")),
    NotifCategoryTab("cmt", "评论", setOf("comment", "creply")),
    NotifCategoryTab("rx", "回应", setOf("emoji", "reply")),
    NotifCategoryTab("like", "喜欢", setOf("like")),
    NotifCategoryTab("chat", "群聊", setOf("chat_at", "chat_reply", "join_req", "packet", "chat_idle")),
    NotifCategoryTab("post", "新帖", setOf("post")),
    NotifCategoryTab("farm", "农场", setOf("farm_conflict")),
    NotifCategoryTab("system", "系统", setOf("release", "skin_alert", "pet_refund", "ai_alert", "badge")),
)

val NOTIF_PREF_OPTIONS = listOf(
    "release" to "系统通知提醒",
    "farm_conflict" to "农场攻防预警与结算",
    "packet" to "红包参与与开奖结果",
    "like" to "有人喜欢我的图",
    "emoji" to "有人给我的图回应表情",
    "reply" to "有人给我的图一句回应",
    "comment" to "有人评论我的图",
    "creply" to "有人回复我的评论",
    "post" to "有人发了新帖",
    "mention" to "有人在评论里 @ 我",
    "chat_at" to "有人在群聊里 @ 我",
    "chat_reply" to "有人在群聊里回复我",
    "post_at" to "有人在帖子里 @ 我",
    "ai_comment" to "AI 评论、回复和提及提醒",
    "badge" to "我拿到了新徽章",
    "join_req" to "有人申请加入群聊（群管理）",
)

private val NOTIF_VERB_MAP = mapOf(
    "packet" to "红包提醒",
    "like" to "喜欢了",
    "emoji" to "回应了表情",
    "reply" to "回应了",
    "comment" to "评论了",
    "creply" to "回复了你在这帖的评论",
    "post" to "发了新帖",
    "mention" to "在评论里 @ 了你",
    "chat_at" to "在群聊里 @ 了你",
    "chat_reply" to "在群聊里回复了你",
    "post_at" to "在帖子里 @ 了你",
    "join_req" to "申请加入群聊",
    "badge" to "你获得了徽章",
)

private val CHAT_MSG_NOTIF_KINDS = setOf("chat_at", "chat_reply", "packet")
private val CHAT_GROUP_NOTIF_KINDS = setOf("chat_at", "chat_reply", "packet", "chat_idle", "join_req")
private val POST_NOTIF_KINDS = setOf("like", "emoji", "reply", "comment", "creply", "post", "mention", "post_at")
private val SHOW_VAL_KINDS = setOf("reply", "comment", "creply", "mention", "post_at")

@Serializable
data class NotifItem(
    val id: Long = 0L,
    val kind: String = "",
    val title: String? = null,
    val body: String? = null,
    @SerialName("val") val valueText: String? = null,
    @SerialName("group_name") val groupName: String? = null,
    @SerialName("user_id") val userId: Long? = null,
    val username: String? = null,
    val name: String? = null,
    val avatar: String? = null,
    val thumb: String? = null,
    @SerialName("meme_id") val memeId: Long? = null,
    val gid: Long? = null,
    @SerialName("badge_n") val badgeCount: Int? = null,
    val url: String? = null,
    val href: String? = null,
    val read: Boolean = false,
    val unread: Boolean = false,
    val at: Long = 0L,
    @SerialName("created_at") val createdAt: Long = 0L,
) {
    /**
     * Authoritative unread state reported by `/api/notifs?system=1` via `unread: Boolean`.
     * Server items omit any `read` field, so checking `!read && at > 0L` previously pinned
     * all notifications as permanently unread.
     */
    val isUnread: Boolean get() = unread

    val isChatMessageNotif: Boolean get() = kind in CHAT_MSG_NOTIF_KINDS
    val isChatGroupNotif: Boolean get() = kind in CHAT_GROUP_NOTIF_KINDS
    val isPostNotif: Boolean get() = kind in POST_NOTIF_KINDS

    val actorDisplay: String
        get() = name?.takeIf { it.isNotBlank() }
            ?: username?.takeIf { it.isNotBlank() }
            ?: "有人"

    val effectiveAtSec: Long
        get() {
            val raw = if (at > 0L) at else createdAt
            return if (raw > 10_000_000_000L) raw / 1000L else raw
        }

    val identityKey: String
        get() = "${kind}_${memeId ?: 0L}_${userId ?: 0L}_${gid ?: 0L}_${at}_${id}"

    fun matchesSameItem(other: NotifItem): Boolean =
        kind == other.kind &&
            at == other.at &&
            memeId == other.memeId &&
            userId == other.userId &&
            gid == other.gid &&
            id == other.id

    fun isSystemAlertKind(): Boolean =
        kind in setOf("release", "farm_conflict", "badge", "ai_alert", "skin_alert", "pet_refund", "chat_idle") ||
            !NOTIF_VERB_MAP.containsKey(kind)

    fun headlineSummary(): String = when (kind) {
        "release" -> "系统更新与公告"
        "farm_conflict" -> "农场攻防"
        "skin_alert" -> "装扮投稿状态"
        "pet_refund" -> "鱼鱼谷 · 今日可一键回收"
        "ai_alert" -> when (valueText) {
            "cap" -> "今日 AI 请求已达上限"
            "breaker" -> "AI 服务连续失败，已暂停"
            "configuration" -> "AI 服务配置需要检查"
            else -> "AI 运行提醒"
        }
        "badge" -> if (valueText == "guide") "邀请徽章已更新" else "你获得了新徽章"
        "chat_idle" -> "你已被移出「${groupName?.takeIf { it.isNotBlank() } ?: "群聊"}」"
        "join_req" -> {
            val verb = when (valueText) {
                "left", "kick" -> "申请重新加入"
                else -> "申请加入"
            }
            "$actorDisplay $verb「${groupName?.takeIf { it.isNotBlank() } ?: "群聊"}」"
        }
        else -> {
            val verb = NOTIF_VERB_MAP[kind]
            if (verb == null) {
                title?.takeIf { it.isNotBlank() } ?: "系统提醒"
            } else if (isChatMessageNotif) {
                "$actorDisplay $verb「${groupName?.takeIf { it.isNotBlank() } ?: "群聊"}」"
            } else {
                val targetTitle = title?.takeIf { it.isNotBlank() }
                    ?: when (kind) {
                        "post" -> "一帖"
                        "creply", "mention" -> "这帖"
                        else -> "你的帖"
                    }
                "$actorDisplay $verb「$targetTitle」"
            }
        }
    }

    fun detailBodyText(): String = when (kind) {
        "ai_alert" -> valueText?.takeIf { it.isNotBlank() }?.let { "状态：$it · 请前往网站 AI 设置查看" } ?: "查看 AI 设置与运行状态"
        "chat_idle" -> valueText?.takeIf { it.isNotBlank() }
            ?: "超过 10 天没有登录网站，系统自动清理。点此进入对应群聊重新加入。"
        "join_req" -> when (valueText) {
            "left" -> "曾主动退群 · 请前往群管理审核"
            "kick" -> "曾被踢出 · 请前往群管理审核"
            else -> "首次申请入群 · 请前往群管理审核"
        }
        "badge" -> valueText?.takeIf { it.isNotBlank() }?.let { badgeKey ->
            if (badgeKey == "guide") {
                val n = badgeCount ?: 0
                if (n > 0) "当前合格邀请 $n 人" else "邀请已作废，徽章暂未点亮"
            } else {
                "徽章：$badgeKey"
            }
        }.orEmpty()
        "emoji" -> body?.takeIf { it.isNotBlank() }.orEmpty()
        else -> {
            val text = valueText?.takeIf { it.isNotBlank() } ?: body?.takeIf { it.isNotBlank() }
            if (text != null) {
                text
            } else if (kind !in NOTIF_VERB_MAP || kind in SHOW_VAL_KINDS || isChatMessageNotif) {
                if (!NOTIF_VERB_MAP.containsKey(kind)) "请点击查看完整提醒内容" else ""
            } else {
                ""
            }
        }
    }

    val displayLine: String
        get() = listOfNotNull(
            headlineSummary().takeIf { it.isNotBlank() },
            detailBodyText().takeIf { it.isNotBlank() },
        ).joinToString(" · ").ifBlank { kind }
}

@Serializable
data class NotifsPayload(
    val unread: Int = 0,
    val items: List<NotifItem> = emptyList(),
)

@Serializable
data class FolderItem(
    val id: Long = 0L,
    val name: String = "",
    val n: Int = 0,
    val count: Int = 0,
) {
    val itemCount: Int get() = if (n > 0) n else count
}

@Serializable
data class PuzzleItem(
    val id: Long = 0L,
    val title: String = "",
    val body: String? = null,
    val hint: String? = null,
    val solved: Boolean = false,
    val reward: Int? = null,
)

@Serializable
data class RoadItem(
    val id: Long = 0L,
    val kind: String = "idea",
    val title: String = "",
    val body: String = "",
    val status: String? = null,
    val score: Int = 0,
    val votes: Int = 0,
    val my: Int = 0,
    @SerialName("my_vote") val myVote: Int = 0,
    val comments: Int = 0,
    val reply: String? = null,
    val mine: Boolean = false,
    val user: ChatUser? = null,
    @SerialName("author_name") val authorName: String? = null,
    @SerialName("created_at") val createdAt: Long = 0L,
) {
    val displayScore: Int get() = if (score != 0 || votes == 0) score else votes
    val effectiveMyVote: Int get() = if (my != 0) my else myVote
}

@Serializable
data class TodayStats(
    val day: String = "",
    val seen: Int = 0,
    @SerialName("new_seen") val newSeen: Int = 0,
    val posts: Int = 0,
    val msgs: Int = 0,
    val browsed: Int = 0,
    @SerialName("post_reacts") val postReacts: Int = 0,
    val reacts: Int = 0,
    @SerialName("seen_nsfw") val seenNsfw: Int = 0,
)

@Serializable
data class PetCloudState(
    val level: Int = 1,
    val xp: Int = 0,
    val hunger: Int = 100,
    val energy: Int = 100,
    val cleanliness: Int = 100,
    val affection: Int = 50,
    val nickname: String? = null,
    val activePet: String? = null,
)

@Serializable
data class PetSummary(
    val name: String = "大肥鱼",
    val level: Int = 1,
    val xp: Int = 0,
    val hunger: Int = 100,
    val mood: Int = 100,
    val clean: Int = 100,
    val energy: Int = 100,
    val sleeping: Boolean = false,
    val species: String? = null,
    val stage: String? = null,
)

@Serializable
data class PetStateResponse(
    val revision: Long = 0L,
    val state: PetCloudState? = null,
    val pet: PetSummary? = null,
    val coins: Long = 0L,
    val now: Double? = null,
    val error: String? = null,
)

@Serializable
data class PetWorldPlot(
    val k: String? = null,
    val p: Long? = null,
    val w: Long? = null,
    val stages: Int? = null,
    val st: Int? = null,
)

@Serializable
data class PetWorldSnapshot(
    val ok: Boolean = true,
    val rev: Long = 0L,
    val expectedRev: Long? = null,
    val coins: Long = 0L,
    val clockStart: Long = 0L,
    val now: Double? = null,
    val state: JsonObject? = null,
    val error: String? = null,
    val msg: String? = null,
)

@Serializable
data class PetNeighborSummary(
    val id: Long = 0L,
    @SerialName("user_id") val userId: Long = 0L,
    val name: String = "",
    val username: String = "",
    val nick: String? = null,
    val avatar: String? = null,
    val level: Int = 1,
    val lv: Int = 1,
    val plots: Int = 0,
    val pens: Int = 0,
    val newbie: Boolean = false,
    val active: Long = 0L,
    val marked: Boolean = false,
    val readyCrops: Int = 0,
    val dryPlots: Int = 0,
) {
    val resolvedId: Long get() = if (id > 0L) id else userId
    val displayName: String get() = name.ifBlank { nick ?: username.ifBlank { "岛主 #$resolvedId" } }
    val resolvedLevel: Int get() = if (level > 1) level else lv
}

@Serializable
data class PetGuardBattle(
    val id: Long = 0L,
    @SerialName("attacker_id") val attackerId: Long = 0L,
    val attacker: String = "",
    val defender: String = "",
    val winner: Int = 0,
    @SerialName("created_at") val createdAt: Long = 0L,
)

@Serializable
data class WalletLedgerRow(
    val kind: String = "",
    val delta: Double = 0.0,
    @SerialName("balance_after") val balanceAfter: Double = 0.0,
    @SerialName("created_at") val createdAt: Long = 0L,
)

@Serializable
data class WalletSummary(
    val coins: Long = 0L,
    val balance: Double = 0.0,
    @SerialName("hb_bound") val hbBound: Boolean = false,
    @SerialName("hb_masked") val hbMasked: String? = null,
    val rows: List<WalletLedgerRow> = emptyList(),
    val next: String? = null,
) {
    val resolvedCoins: Long get() = if (balance > 0.0) balance.toLong() else coins
}

@Serializable
data class Owner52bCard(
    val id: Long = 0L,
    val name: String = "",
    val desc: String? = null,
    val tier: String? = null,
    val owned: Boolean = false,
    val icon: String? = null,
)

@Serializable
data class DynamicModule(
    val id: String,
    val label: String,
    val route: String,
    val iconKey: String = "web",
    val isBuiltInNative: Boolean = false,
    val isNewFromTopbar: Boolean = false,
    val visibleInNav: Boolean = true,
)

@Serializable
data class UploadedMedia(
    val key: String = "",
    val url: String = "",
)

@Serializable
enum class AppThemeMode(val label: String) {
    WARM_PAPER_LIGHT("暖纸松绿（日间）"),
    PINE_NIGHT_DARK("深松暗夜（夜间）"),
    FEISHU_WORK("飞书工作伪装"),
    LINUX_DO("LINUX DO 伪装"),
    FOLLOW_SYSTEM("跟随系统（暖纸/深松）"),
    ;

    companion object {
        val dayNightEntries: List<AppThemeMode>
            get() = listOf(FOLLOW_SYSTEM, WARM_PAPER_LIGHT, PINE_NIGHT_DARK)
    }
}

@Serializable
enum class AppDisguiseMode(
    val skinKey: String,
    val label: String,
    val subtitle: String,
) {
    NONE("", "不伪装（默认尽欢）", "原版五栏主界面（图廊、群聊、鱼鱼谷、百宝箱、我的）"),
    FEISHU("feishu", "飞书聊天界面", "整站变成飞书 IM 会话与工作群，看图、回应、评论、真群聊与私信照常用"),
    LINUX_DO("linuxdo", "LINUX DO 论坛帖子", "整站变成一篇论坛盖楼长帖，每张图一楼，右下角楼层进度条，顶栏拉出群聊抽屉"),
}

@Serializable
enum class ShortcutGestureMode(
    val key: String,
    val label: String,
) {
    NONE("none", "关闭"),
    VOL_DOUBLE_DOWN("vol_down", "双击音量下键"),
    VOL_DOUBLE_UP("vol_up", "双击音量上键"),
    SHAKE("shake", "轻晃手机"),
}

@Serializable
data class PrivacyStealthConfig(
    val disguiseMode: AppDisguiseMode = AppDisguiseMode.NONE,
    val blurAllEnabled: Boolean = false,
    val blurBeforeDisguise: Boolean = false,
    val autoBlurOnEnterDisguise: Boolean = true,
    val autoReblurOnScrollOff: Boolean = true,
    val autoBlurOnBackground: Boolean = false,
    val blurShortcut: ShortcutGestureMode = ShortcutGestureMode.NONE,
    val bossDoubleBack: Boolean = true,
    val bossVolumeShortcut: ShortcutGestureMode = ShortcutGestureMode.NONE,
    val bossFlipFaceDown: Boolean = false,
    val bossOnBackgroundResume: Boolean = false,
)

@Serializable
enum class AppIconAlias(
    val aliasClass: String,
    val title: String,
    val subtitle: String,
) {
    JINHUAN("com.jinhuan.forhappy.AliasJinhuan", "尽欢", "松绿小欢默认图标"),
    FEISHU("com.jinhuan.forhappy.AliasFeishu", "飞书文档", "蓝色办公伪装图标"),
    DOCS("com.jinhuan.forhappy.AliasDocs", "协作文档", "青蓝文档伪装图标"),
}

/**
 * Structured representation of `ChatMessage.redpack` following `52dmeme.com/app.js` `cm-redpack`.
 */
data class RedpackMeta(
    val prompt: String,
    val mode: String,
    val provider: String,
    val paymentProvider: String?,
    val game: String?,
    val ldcMode: String?,
    val ldcTotalCents: Long?,
    val ldcCount: Int?,
    val ldcAnswerLength: Int?,
    val ldcResponseMode: String?,
    val opened: Int,
    val solved: Boolean,
    val shared: Boolean,
    val cover: String?,
    val drawKind: String?,
    val snapshotClaimed: Boolean,
    val snapshotState: String?,
) {
    val isGuessGame: Boolean
        get() = game == "free_guess" || ldcMode == "guess" || ldcMode == "clues"

    val isDcoin: Boolean
        get() = provider == "coin52" || paymentProvider == "coin52"

    val formattedAmount: String?
        get() = ldcTotalCents?.let { cents ->
            if (isDcoin) "${cents / 100} D币" else String.format(java.util.Locale.US, "%.2f LDC", cents / 100.0)
        }

    val headline: String
        get() = prompt.ifBlank { if (game == "free_guess") "一起猜词吧" else "给你一个红包" }

    val subline: String
        get() = when {
            provider == "coin52" -> buildString {
                append("红包 · 站内积分")
                formattedAmount?.let { append(" · ").append(it) }
            }
            game == "free_guess" -> buildString {
                append("欢欢猜词 · 无 LDC 奖励")
                if (ldcAnswerLength != null) append(" · $ldcAnswerLength 字")
            }
            game == "ldc" -> buildString {
                val modeName = when {
                    drawKind == "lottery" -> "限时抽奖"
                    ldcMode == "fixed" -> "普通红包"
                    ldcMode == "random" -> "拼手气"
                    ldcMode == "guess" -> "欢欢猜词"
                    ldcMode == "clues" -> "逐步线索"
                    else -> "本站红包"
                }
                append(modeName)
                formattedAmount?.let { append(" · ").append(it) }
                if (isGuessGame && ldcAnswerLength != null) append(" · 答案 $ldcAnswerLength 字")
            }
            provider.isNotBlank() && provider != "native" -> {
                val pname = when (provider) {
                    "hb" -> "HB"
                    "credit" -> "Credit"
                    "fish" -> "Fish"
                    else -> provider
                }
                "$pname 红包"
            }
            else -> buildString {
                val mName = when (mode) {
                    "open" -> "直接开"
                    "phrase" -> "复读红包"
                    "cipher" -> "解个谜"
                    "image" -> "猜张图"
                    else -> "本站红包"
                }
                append(mName)
                formattedAmount?.let { append(" · ").append(it) }
                if (solved) append(" · 已拆封")
            }
        }

    val actionLabel: String
        get() = when {
            game == "free_guess" || ldcMode == "guess" || ldcMode == "clues" -> "进入猜词专区"
            snapshotClaimed -> "已领取"
            snapshotState != null -> when (snapshotState) {
                "exhausted" -> "被抢光啦"
                "expired" -> "已过期"
                "cancelled" -> "已取消"
                "registered" -> "等待开奖"
                "drawing" -> "正在开奖"
                "not_won" -> "未中奖"
                "payment_pending" -> "等待发放"
                "simulation" -> "模拟奖励"
                "draft" -> "尚未开放"
                "unconfirmed" -> "结果待核对"
                else -> "查看红包"
            }
            drawKind == "lottery" -> "参与 / 查看抽奖"
            (provider.isNotBlank() && provider != "native") || solved -> "查看红包"
            else -> "戳我拆开"
        }

    val typeBadge: String
        get() = when {
            isDcoin -> "D币"
            game == "free_guess" -> "猜词游戏"
            game == "ldc" || (provider.isNotBlank() && provider != "native") -> "LDC 红包"
            ldcCount != null -> "$ldcCount 份 · $opened 人"
            else -> "$opened 人拆过"
        }
}

fun ChatMessage.parseRedpackMeta(): RedpackMeta? {
    val obj = redpack ?: return null
    fun JsonObject.str(key: String): String? =
        (get(key) as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }

    val snapshotObj = runCatching { obj["snapshot"]?.jsonObject }.getOrNull()
    val drawObj = runCatching { obj["draw"]?.jsonObject }.getOrNull()
    return RedpackMeta(
        prompt = obj.str("prompt") ?: obj.str("title").orEmpty(),
        mode = obj.str("mode") ?: "open",
        provider = obj.str("provider") ?: "native",
        paymentProvider = obj.str("payment_provider"),
        game = obj.str("game"),
        ldcMode = obj.str("ldc_mode"),
        ldcTotalCents = obj["ldc_total"]?.jsonPrimitive?.longOrNull,
        ldcCount = obj["ldc_count"]?.jsonPrimitive?.intOrNull,
        ldcAnswerLength = obj["ldc_answer_length"]?.jsonPrimitive?.intOrNull,
        ldcResponseMode = obj.str("ldc_response_mode"),
        opened = obj["opened"]?.jsonPrimitive?.intOrNull ?: 0,
        solved = obj["solved"]?.jsonPrimitive?.booleanOrNull ?: false,
        shared = obj["shared"]?.jsonPrimitive?.booleanOrNull ?: false,
        cover = obj.str("cover"),
        drawKind = drawObj?.str("kind"),
        snapshotClaimed = snapshotObj?.get("claimed")?.jsonPrimitive?.booleanOrNull ?: false,
        snapshotState = snapshotObj?.str("state"),
    )
}

@Serializable
data class GuessRecipient(
    val name: String? = null,
    val amount: JsonElement? = null,
    val state: String? = null,
) {
    val amountText: String
        get() = (amount as? JsonPrimitive)?.contentOrNull.orEmpty()
}

@Serializable
data class GuessRound(
    @SerialName("message_id") val messageId: Long = 0L,
    val state: String = "open",
    @SerialName("close_kind") val closeKind: String? = null,
    val owner: Boolean = false,
    @SerialName("owner_name") val ownerName: String = "",
    @SerialName("can_play") val canPlay: Boolean = true,
    @SerialName("reward_type") val rewardType: String = "none",
    val total: JsonElement? = null,
    val currency: String? = null,
    @SerialName("answer_length") val answerLength: Int? = null,
    val count: Int = 1,
    @SerialName("claimed_count") val claimedCount: Int = 0,
    @SerialName("guess_count") val guessCount: Int = 0,
    @SerialName("question_count") val questionCount: Int = 0,
    @SerialName("ai_count") val aiCount: Int = 0,
    @SerialName("ai_limit") val aiLimit: Int = 0,
    @SerialName("ends_at") val endsAt: Long = 0L,
    val hints: List<String> = emptyList(),
    @SerialName("hint_source") val hintSource: String? = null,
    @SerialName("ai_generated") val aiGenerated: Boolean = false,
    @SerialName("auto_hint_every") val autoHintEvery: Int? = null,
    @SerialName("next_hint_guess") val nextHintGuess: Int? = null,
    @SerialName("hint_progress") val hintProgress: Int? = null,
    val answer: String? = null,
    val explanation: String? = null,
    @SerialName("owner_answers") val ownerAnswers: List<String> = emptyList(),
    val recipients: List<GuessRecipient> = emptyList(),
) {
    val totalText: String get() = (total as? JsonPrimitive)?.contentOrNull.orEmpty()
}

@Serializable
data class GuessEntryItem(
    val id: Long = 0L,
    val question: String = "",
    val score: Double? = null,
    @SerialName("character_matches") val characterMatches: List<String> = emptyList(),
    @SerialName("review_state") val reviewState: String? = null,
    @SerialName("hidden_answer") val hiddenAnswer: Boolean = false,
    val user: ChatUser? = null,
    val users: List<ChatUser> = emptyList(),
    @SerialName("participant_count") val participantCount: Int = 1,
)

@Serializable
data class GuessPage(
    val items: List<GuessEntryItem> = emptyList(),
    val next: String? = null,
)

@Serializable
data class GuessLatest(
    val user: ChatUser? = null,
    val name: String? = null,
    val username: String? = null,
    val avatar: String? = null,
    val question: String = "",
    val text: String = "",
)

@Serializable
data class GuessReply(
    val status: String = "",
    val text: String = "",
)

@Serializable
data class GuessReceipt(
    val id: Long = 0L,
    val reply: GuessReply? = null,
)

@Serializable
data class GuessRoomOverviewResponse(
    val now: Double? = null,
    val round: GuessRound? = null,
    val guesses: GuessPage = GuessPage(),
    val facts: GuessPage = GuessPage(),
    val negative: GuessPage = GuessPage(),
    val unknown: GuessPage = GuessPage(),
    @SerialName("not_question") val notQuestion: GuessPage = GuessPage(),
    val latest: GuessLatest? = null,
    val receipt: GuessReceipt? = null,
)

@Serializable
data class GuessHistoryItem(
    @SerialName("message_id") val messageId: Long = 0L,
    @SerialName("owner_name") val ownerName: String = "",
    @SerialName("created_at") val createdAt: Long = 0L,
    @SerialName("reward_type") val rewardType: String = "none",
    val total: JsonElement? = null,
    val currency: String? = null,
) {
    val totalText: String get() = (total as? JsonPrimitive)?.contentOrNull.orEmpty()
}

@Serializable
data class GuessHistoryResponse(
    val items: List<GuessHistoryItem> = emptyList(),
    val next: String? = null,
)
