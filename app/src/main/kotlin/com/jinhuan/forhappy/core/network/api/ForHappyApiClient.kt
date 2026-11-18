package com.jinhuan.forhappy.core.network.api

import android.content.Context
import android.os.Build
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.jinhuan.forhappy.core.clock.ServerClock
import com.jinhuan.forhappy.core.compose.PuzzleDraftSpec
import com.jinhuan.forhappy.core.compose.SocialPreviewData
import com.jinhuan.forhappy.core.compose.SocialPreviewMedia
import com.jinhuan.forhappy.core.db.ForHappyDatabase
import com.jinhuan.forhappy.core.dynamic.TopbarModuleScanner
import com.jinhuan.forhappy.core.model.AnchorTag
import com.jinhuan.forhappy.core.model.AuthConfigResponse
import com.jinhuan.forhappy.core.model.AuthorOption
import com.jinhuan.forhappy.core.model.ChatGroup
import com.jinhuan.forhappy.core.model.ChatGroupsResponse
import com.jinhuan.forhappy.core.model.ChatMessage
import com.jinhuan.forhappy.core.model.ChatMessagesResponse
import com.jinhuan.forhappy.core.model.ChatRepliesResponse
import com.jinhuan.forhappy.core.model.ChatReplyItem
import com.jinhuan.forhappy.core.model.ChatSearchResponse
import com.jinhuan.forhappy.core.model.ChatUser
import com.jinhuan.forhappy.core.model.CreditFundingStatus
import com.jinhuan.forhappy.core.model.DaysResponse
import com.jinhuan.forhappy.core.model.DmMessage
import com.jinhuan.forhappy.core.model.DmMessagesResponse
import com.jinhuan.forhappy.core.model.DmThread
import com.jinhuan.forhappy.core.model.DynamicModule
import com.jinhuan.forhappy.core.model.FishFundingStatus
import com.jinhuan.forhappy.core.model.FolderItem
import com.jinhuan.forhappy.core.model.ForHappyJson
import com.jinhuan.forhappy.core.model.GroupMemberItem
import com.jinhuan.forhappy.core.model.GroupMembersResponse
import com.jinhuan.forhappy.core.model.GuessHistoryResponse
import com.jinhuan.forhappy.core.model.GuessRoomOverviewResponse
import com.jinhuan.forhappy.core.model.GuessRound
import com.jinhuan.forhappy.core.model.HbKeyStatus
import com.jinhuan.forhappy.core.model.MeItemsResponse
import com.jinhuan.forhappy.core.model.MeResponse
import com.jinhuan.forhappy.core.model.MeUser
import com.jinhuan.forhappy.core.model.MemeComment
import com.jinhuan.forhappy.core.model.MemeDetailResponse
import com.jinhuan.forhappy.core.model.MemeItem
import com.jinhuan.forhappy.core.model.MemesPageResponse
import com.jinhuan.forhappy.core.model.NotifItem
import com.jinhuan.forhappy.core.model.NotifsPayload
import com.jinhuan.forhappy.core.model.Owner52bCard
import com.jinhuan.forhappy.core.model.PetGuardBattle
import com.jinhuan.forhappy.core.model.PetNeighborSummary
import com.jinhuan.forhappy.core.model.RedpackArchiveItem
import com.jinhuan.forhappy.core.model.RoadCommentItem
import com.jinhuan.forhappy.core.model.RoadItem
import com.jinhuan.forhappy.core.model.SeenVisitEntry
import com.jinhuan.forhappy.core.model.SenderStatItem
import com.jinhuan.forhappy.core.model.SeriesItem
import com.jinhuan.forhappy.core.model.SiteNotice
import com.jinhuan.forhappy.core.model.StickerEntry
import com.jinhuan.forhappy.core.model.StickerPack
import com.jinhuan.forhappy.core.model.TagStatItem
import com.jinhuan.forhappy.core.model.TagsResponse
import com.jinhuan.forhappy.core.model.TitleRaffleItem
import com.jinhuan.forhappy.core.model.TodayStats
import com.jinhuan.forhappy.core.model.UploadedMedia
import com.jinhuan.forhappy.core.model.WalletSummary
import com.jinhuan.forhappy.core.model.WhoReactedResponse
import com.jinhuan.forhappy.core.network.cookie.CookieSyncBridge
import com.jinhuan.forhappy.core.network.cookie.PersistentCookieJar
import com.jinhuan.forhappy.core.network.media.MediaUrlRewriter
import com.jinhuan.forhappy.core.network.media.StickerFallbackInterceptor
import com.jinhuan.forhappy.core.network.media.WebAssetCacheManager
import com.jinhuan.forhappy.core.network.pet.PetCoexistenceCoordinator
import com.jinhuan.forhappy.core.network.rust.RustNetManager
import com.jinhuan.forhappy.core.network.scheduler.RequestSchedulerInterceptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import okhttp3.ConnectionPool
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okio.Path.Companion.toOkioPath
import java.io.File
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Central protocol client for `https://52dmeme.com`:
 * All traffic runs through [RequestSchedulerInterceptor] -> local Rust ECH+H2+EdgePin gateway
 * while preserving original HTTPS domains for cookies & redirects.
 */
class ForHappyApiClient(
    private val context: Context,
    private val db: ForHappyDatabase,
    val rustNetManager: RustNetManager,
    val cookieJar: PersistentCookieJar,
) {
    companion object {
        const val ORIGIN = "https://52dmeme.com"
        private const val KV_MODULES_CACHE = "dynamic.topbar_modules"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .followRedirects(false)
        .followSslRedirects(false)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .connectionPool(ConnectionPool(8, 90, TimeUnit.SECONDS))
        .addInterceptor(StickerFallbackInterceptor.forDatabase(db))
        .addInterceptor(RequestSchedulerInterceptor(rustNetManager, cookieJar))
        .build()

    val imageLoader: ImageLoader by lazy {
        ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { okHttpClient }))
                if (Build.VERSION.SDK_INT >= 28) {
                    add(AnimatedImageDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.22)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(File(context.cacheDir, "forhappy_coil_disk_v2").toOkioPath())
                    .maxSizeBytes(192L * 1024L * 1024L)
                    .build()
            }
            .build()
    }

    val webAssetCache: WebAssetCacheManager by lazy {
        WebAssetCacheManager(context, db, okHttpClient)
    }

    suspend fun getPref(key: String): String? = db.getKv(key)

    suspend fun putPref(key: String, value: String) = db.putKv(key, value)

    val petCoordinator: PetCoexistenceCoordinator by lazy {
        PetCoexistenceCoordinator(transport = { method, apiPath, bodyObj ->
            executeRawJson(method, apiPath, bodyObj?.let { ForHappyJson.encodeToString(it) })
        })
    }

    suspend fun executeRawJson(
        method: String,
        pathOrUrl: String,
        jsonBody: String? = null,
    ): PetCoexistenceCoordinator.RawHttpResult = withContext(Dispatchers.IO) {
        val targetUrl = if (pathOrUrl.startsWith("http")) pathOrUrl else "$ORIGIN$pathOrUrl"
        val reqBody = when (method.uppercase()) {
            "GET", "HEAD" -> null
            else -> (jsonBody ?: "{}").toRequestBody(JSON_MEDIA_TYPE)
        }
        val req = Request.Builder()
            .url(targetUrl)
            .method(method.uppercase(), reqBody)
            .header("Accept", "application/json")
            .header("Cache-Control", "no-store")
            .build()
        try {
            okHttpClient.newCall(req).execute().use { resp ->
                val retryAfter = resp.header("Retry-After")?.trim()?.toLongOrNull()
                val bodyStr = resp.body?.string().orEmpty()
                runCatching {
                    val obj = ForHappyJson.parseToJsonElement(bodyStr).jsonObject
                    obj["now"]?.jsonPrimitive?.doubleOrNull?.let { ServerClock.calibrateFromServerSec(it) }
                }
                PetCoexistenceCoordinator.RawHttpResult(resp.code, bodyStr, retryAfter)
            }
        } catch (_: Exception) {
            PetCoexistenceCoordinator.RawHttpResult(0, "", null)
        }
    }

    private suspend fun executeText(path: String): String? = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url(if (path.startsWith("http")) path else "$ORIGIN$path")
            .get()
            .header("Cache-Control", "no-store")
            .build()
        okHttpClient.newCall(req).execute().use { resp ->
            if (resp.isSuccessful) resp.body?.string() else null
        }
    }

    suspend fun scanDynamicTopbarModules(): List<DynamicModule> {
        val html = runCatching { executeText("/") }.getOrNull()
        if (!html.isNullOrBlank()) {
            val parsed = TopbarModuleScanner.parseTopbarHtml(html)
            if (parsed.isNotEmpty()) {
                val merged = TopbarModuleScanner.mergeWithDefaults(parsed)
                db.putKv(KV_MODULES_CACHE, ForHappyJson.encodeToString(merged))
                return merged
            }
        }
        val cached = db.getKv(KV_MODULES_CACHE)
        if (!cached.isNullOrBlank()) {
            runCatching { ForHappyJson.decodeFromString<List<DynamicModule>>(cached) }
                .getOrNull()
                ?.let { return TopbarModuleScanner.mergeWithDefaults(it) }
        }
        return TopbarModuleScanner.DEFAULT_BUILT_IN_MODULES
    }

    suspend fun fetchAuthConfig(): AuthConfigResponse? {
        val res = executeRawJson("GET", "/api/auth/config")
        if (res.code !in 200..299) return null
        return runCatching { ForHappyJson.decodeFromString<AuthConfigResponse>(res.body) }.getOrNull()
    }

    suspend fun fetchMe(): MeUser? {
        val res = executeRawJson("GET", "/api/me")
        if (res.code !in 200..299) return null
        val parsed = runCatching { ForHappyJson.decodeFromString<MeResponse>(res.body) }.getOrNull()
        parsed?.now?.let { ServerClock.calibrateFromServerSec(it) }
        return parsed?.user
    }

    suspend fun loginWithPassword(login: String, password: String): Result<Unit> {
        val body = buildJsonObject {
            put("login", JsonPrimitive(login.trim()))
            put("password", JsonPrimitive(password))
            put("next", JsonPrimitive("/"))
        }.toString()
        val res = executeRawJson("POST", "/api/auth/password-login", body)
        if (res.code in 200..299) {
            CookieSyncBridge.pushJarToWebView(cookieJar)
            return Result.success(Unit)
        }
        val errMsg = runCatching {
            ForHappyJson.parseToJsonElement(res.body).jsonObject["error"]?.jsonPrimitive?.content
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: "登录失败 (HTTP ${res.code})"
        return Result.failure(IllegalStateException(errMsg))
    }

    suspend fun logout() {
        runCatching { executeRawJson("POST", "/api/auth/logout", "{}") }
        cookieJar.clearAll()
        CookieSyncBridge.clearWebViewCookies()
    }

    suspend fun recordSiteVisit(): JsonObject? {
        val res = executeRawJson("POST", "/api/site-visit", "{}")
        return if (res.code in 200..299) runCatching { ForHappyJson.parseToJsonElement(res.body).jsonObject }.getOrNull() else null
    }

    suspend fun fetchDays(
        limitDays: Int? = 5,
        beforeDay: String? = null,
        fromDay: String? = null,
        tag: String? = null,
        search: String? = null,
        sort: String = "new",
        unseenOnly: Boolean = false,
        textFilter: String = "",
        nsfwHide: Boolean = false,
        nsfwModeParam: String? = null,
        author: String? = null,
    ): DaysResponse {
        val effectiveNsfw = nsfwModeParam ?: if (nsfwHide) "0" else null
        val q = buildList {
            if (limitDays != null && limitDays > 0) add("days=$limitDays")
            add("sort=${enc(sort)}")
            if (effectiveNsfw != null) add("nsfw=${enc(effectiveNsfw)}")
            if (!fromDay.isNullOrBlank()) add("from=${enc(fromDay)}")
            if (!beforeDay.isNullOrBlank()) add("before=${enc(beforeDay)}")
            if (!tag.isNullOrBlank()) add("tag=${enc(tag)}")
            if (!search.isNullOrBlank()) add("q=${enc(search)}")
            if (unseenOnly) add("unseen=1")
            if (textFilter.isNotBlank()) add("text=${enc(textFilter)}")
            if (!author.isNullOrBlank()) add("author=${enc(author)}")
        }.joinToString("&")
        val res = executeRawJson("GET", "/api/days?$q")
        if (res.code !in 200..299) return DaysResponse()
        return runCatching { ForHappyJson.decodeFromString<DaysResponse>(res.body) }.getOrDefault(DaysResponse())
    }

    suspend fun fetchMemesPage(
        limit: Int = 30,
        offset: Int = 0,
        day: String? = null,
        focus: Long? = null,
        tag: String? = null,
        query: String? = null,
        series: String? = null,
        sort: String = "new",
        unseenOnly: Boolean = false,
        textFilter: String = "",
        nsfwHide: Boolean = false,
        nsfwModeParam: String? = null,
        author: String? = null,
    ): MemesPageResponse {
        val effectiveNsfw = nsfwModeParam ?: if (nsfwHide) "0" else null
        val q = buildList {
            add("limit=$limit")
            if (focus != null && focus > 0L) {
                add("focus=$focus")
            } else {
                add("offset=$offset")
            }
            add("sort=${enc(sort)}")
            if (sort == "hot") add("page=${(offset / maxOf(1, limit)) + 1}")
            if (effectiveNsfw != null) add("nsfw=${enc(effectiveNsfw)}")
            if (!day.isNullOrBlank()) add("day=${enc(day)}")
            if (!tag.isNullOrBlank()) add("tag=${enc(tag)}")
            if (!query.isNullOrBlank()) add("q=${enc(query)}")
            if (!series.isNullOrBlank()) add("series=${enc(series)}")
            if (unseenOnly) add("unseen=1")
            if (textFilter.isNotBlank()) add("text=${enc(textFilter)}")
            if (!author.isNullOrBlank()) add("author=${enc(author)}")
        }.joinToString("&")
        val res = executeRawJson("GET", "/api/memes?$q")
        if (res.code !in 200..299) return MemesPageResponse()
        val obj = runCatching { ForHappyJson.parseToJsonElement(res.body).jsonObject }.getOrNull() ?: return MemesPageResponse()
        val arr = obj["items"] ?: obj["memes"] ?: return MemesPageResponse()
        val items = runCatching {
            ForHappyJson.decodeFromJsonElement(ListSerializer(MemeItem.serializer()), arr)
        }.getOrDefault(emptyList())
        val respOffset = obj["offset"]?.jsonPrimitive?.intOrNull ?: offset
        val more = obj["more"]?.jsonPrimitive?.booleanOrNull ?: (items.size >= limit)
        return MemesPageResponse(items = items, offset = respOffset, more = more)
    }

    suspend fun fetchMemes(
        limit: Int = 30,
        offset: Int = 0,
        day: String? = null,
        focus: Long? = null,
        tag: String? = null,
        query: String? = null,
        series: String? = null,
        sort: String = "new",
        unseenOnly: Boolean = false,
        textFilter: String = "",
        nsfwHide: Boolean = false,
        nsfwModeParam: String? = null,
        author: String? = null,
    ): List<MemeItem> = fetchMemesPage(
        limit = limit,
        offset = offset,
        day = day,
        focus = focus,
        tag = tag,
        query = query,
        series = series,
        sort = sort,
        unseenOnly = unseenOnly,
        textFilter = textFilter,
        nsfwHide = nsfwHide,
        nsfwModeParam = nsfwModeParam,
        author = author,
    ).items

    suspend fun fetchTags(hideNsfw: Boolean = false, nsfwModeParam: String? = null): List<TagStatItem> {
        val effectiveNsfw = nsfwModeParam ?: if (hideNsfw) "0" else null
        val qs = if (effectiveNsfw != null) "?nsfw=${enc(effectiveNsfw)}" else ""
        val res = executeRawJson("GET", "/api/tags$qs")
        if (res.code !in 200..299) return emptyList()
        return runCatching { ForHappyJson.decodeFromString<TagsResponse>(res.body).tags }.getOrDefault(emptyList())
    }

    suspend fun recordMemeVisits(
        visits: List<SeenVisitEntry>,
        expectedUserId: Long? = null,
    ): Boolean {
        val clean = visits.filter { it.id > 0L && it.at > 0L }.take(100)
        if (clean.isEmpty()) return true
        val visitsArr = JsonArray(
            clean.map { entry ->
                buildJsonObject {
                    put("id", JsonPrimitive(entry.id))
                    put("at", JsonPrimitive(entry.at))
                }
            },
        )
        val payload = buildJsonObject { put("visits", visitsArr) }
        val res = executeRawJson("POST", "/api/views", ForHappyJson.encodeToString(payload))
        if (res.code !in 200..299) return false
        val ack = runCatching { ForHappyJson.parseToJsonElement(res.body).jsonObject }.getOrNull() ?: return false
        val ok = ack["ok"]?.jsonPrimitive?.booleanOrNull == true
        val recorded = ack["recorded"]?.jsonPrimitive?.booleanOrNull == true
        val uidOk = expectedUserId == null || ack["user_id"]?.jsonPrimitive?.longOrNull == expectedUserId
        return ok && recorded && uidOk
    }

    suspend fun recordMemeViews(memeIds: List<Long>): Boolean {
        val nowSec = ServerClock.serverNowSec()
        val visits = memeIds.filter { it > 0L }.distinct().take(100).map { id ->
            SeenVisitEntry(id = id, at = nowSec)
        }
        return recordMemeVisits(visits)
    }

    suspend fun fetchMemeDetail(id: Long): MemeDetailResponse? {
        val res = executeRawJson("GET", "/api/memes/$id")
        if (res.code !in 200..299) return null
        return runCatching { ForHappyJson.decodeFromString<MemeDetailResponse>(res.body) }.getOrNull()
    }

    suspend fun fetchMemeComments(memeId: Long): List<MemeComment> {
        val res = executeRawJson("GET", "/api/memes/$memeId/comments")
        if (res.code !in 200..299) return emptyList()
        val obj = runCatching { ForHappyJson.parseToJsonElement(res.body).jsonObject }.getOrNull() ?: return emptyList()
        val arr = obj["comments"] ?: return emptyList()
        return runCatching { ForHappyJson.decodeFromJsonElement(ListSerializer(MemeComment.serializer()), arr) }
            .getOrDefault(emptyList())
    }

    suspend fun toggleLike(memeId: Long): JsonObject? {
        val res = executeRawJson("POST", "/api/memes/$memeId/like", "{}")
        return if (res.code in 200..299) runCatching { ForHappyJson.parseToJsonElement(res.body).jsonObject }.getOrNull() else null
    }

    suspend fun toggleMemeReaction(memeId: Long, emoji: String): Boolean {
        return reactToMeme(memeId, emoji) != null
    }

    suspend fun reactToMeme(memeId: Long, emoji: String): MemeItem? {
        val payload = buildJsonObject { put("emoji", JsonPrimitive(emoji)) }
        val res = executeRawJson("POST", "/api/memes/$memeId/reactions", ForHappyJson.encodeToString(payload))
        if (res.code !in 200..299) return null
        val obj = runCatching { ForHappyJson.parseToJsonElement(res.body).jsonObject }.getOrNull() ?: return null
        val itemEl = obj["item"] ?: obj["meme"] ?: return null
        return runCatching { ForHappyJson.decodeFromJsonElement(MemeItem.serializer(), itemEl) }.getOrNull()
    }

    suspend fun putMemeQuickReply(memeId: Long, text: String): Result<MemeItem?> {
        val payload = buildJsonObject { put("text", JsonPrimitive(text)) }
        val res = executeRawJson("PUT", "/api/memes/$memeId/reply", ForHappyJson.encodeToString(payload))
        val obj = runCatching { ForHappyJson.parseToJsonElement(res.body).jsonObject }.getOrNull()
        return if (res.code in 200..299) {
            val itemEl = obj?.get("item") ?: obj?.get("meme")
            val parsed = itemEl?.let { runCatching { ForHappyJson.decodeFromJsonElement(MemeItem.serializer(), it) }.getOrNull() }
            Result.success(parsed)
        } else {
            val err = runCatching { obj?.get("error")?.jsonPrimitive?.content }.getOrNull() ?: "回应失败 (${res.code})"
            Result.failure(IllegalStateException(err))
        }
    }

    suspend fun deleteMemeQuickReply(memeId: Long): Result<MemeItem?> {
        val res = executeRawJson("DELETE", "/api/memes/$memeId/reply", "{}")
        val obj = runCatching { ForHappyJson.parseToJsonElement(res.body).jsonObject }.getOrNull()
        return if (res.code in 200..299) {
            val itemEl = obj?.get("item") ?: obj?.get("meme")
            val parsed = itemEl?.let { runCatching { ForHappyJson.decodeFromJsonElement(MemeItem.serializer(), it) }.getOrNull() }
            Result.success(parsed)
        } else {
            val err = runCatching { obj?.get("error")?.jsonPrimitive?.content }.getOrNull() ?: "撤回失败 (${res.code})"
            Result.failure(IllegalStateException(err))
        }
    }

    suspend fun toggleFav(memeId: Long, currentlyFaved: Boolean, folderId: Long? = null): Boolean {
        if (currentlyFaved && folderId == null) {
            val res = executeRawJson("DELETE", "/api/memes/$memeId/fav", "{}")
            return res.code in 200..299
        }
        val targetFolderId = folderId ?: fetchFolders().firstOrNull()?.id
        val body = if (targetFolderId != null) """{"folder_id":$targetFolderId}""" else "{}"
        val res = executeRawJson("POST", "/api/memes/$memeId/fav", body)
        return res.code in 200..299
    }

    suspend fun postMemeComment(memeId: Long, text: String, replyToId: Long? = null): Boolean =
        postMemeCommentFull(memeId = memeId, body = text, replyToId = replyToId).isSuccess

    suspend fun postMemeCommentFull(
        memeId: Long,
        body: String,
        replyToId: Long? = null,
    ): Result<MemeComment?> {
        val payload = buildJsonObject {
            put("body", JsonPrimitive(body))
            if (replyToId != null && replyToId > 0L) put("reply_to", JsonPrimitive(replyToId))
        }
        val res = executeRawJson("POST", "/api/memes/$memeId/comments", ForHappyJson.encodeToString(payload))
        val obj = runCatching { ForHappyJson.parseToJsonElement(res.body).jsonObject }.getOrNull()
        return if (res.code in 200..299) {
            val cm = obj?.get("comment")?.let {
                runCatching { ForHappyJson.decodeFromJsonElement(MemeComment.serializer(), it) }.getOrNull()
            }
            Result.success(cm)
        } else {
            val err = runCatching { obj?.get("error")?.jsonPrimitive?.content }.getOrNull() ?: "评论失败 (${res.code})"
            Result.failure(IllegalStateException(err))
        }
    }

    suspend fun deleteComment(commentId: Long): Boolean {
        val res = executeRawJson("DELETE", "/api/comments/$commentId", "{}")
        return res.code in 200..299
    }

    suspend fun toggleMemeFolderFav(memeId: Long, folderId: Long): MemeItem? {
        val res = executeRawJson("POST", "/api/memes/$memeId/fav", """{"folder_id":$folderId}""")
        if (res.code !in 200..299) return null
        val obj = runCatching { ForHappyJson.parseToJsonElement(res.body).jsonObject }.getOrNull() ?: return null
        val itemEl = obj["item"] ?: obj["meme"] ?: return null
        return runCatching { ForHappyJson.decodeFromJsonElement(MemeItem.serializer(), itemEl) }.getOrNull()
    }

    suspend fun clearMemeFav(memeId: Long): MemeItem? {
        val res = executeRawJson("DELETE", "/api/memes/$memeId/fav", "{}")
        if (res.code !in 200..299) return null
        val obj = runCatching { ForHappyJson.parseToJsonElement(res.body).jsonObject }.getOrNull() ?: return null
        val itemEl = obj["item"] ?: obj["meme"] ?: return null
        return runCatching { ForHappyJson.decodeFromJsonElement(MemeItem.serializer(), itemEl) }.getOrNull()
    }

    suspend fun createFolder(name: String): Pair<Long?, List<FolderItem>> {
        val clean = name.trim()
        if (clean.isBlank()) return null to emptyList()
        val payload = buildJsonObject { put("name", JsonPrimitive(clean)) }
        val res = executeRawJson("POST", "/api/folders", ForHappyJson.encodeToString(payload))
        if (res.code !in 200..299) return null to emptyList()
        val obj = runCatching { ForHappyJson.parseToJsonElement(res.body).jsonObject }.getOrNull() ?: return null to emptyList()
        val newId = obj["id"]?.jsonPrimitive?.longOrNull
        val arr = obj["folders"]
        val folders = if (arr != null) {
            runCatching { ForHappyJson.decodeFromJsonElement(ListSerializer(FolderItem.serializer()), arr) }
                .getOrDefault(emptyList())
        } else {
            emptyList()
        }
        return newId to folders
    }

    suspend fun fetchMemeWhoReacted(memeId: Long): WhoReactedResponse {
        val res = executeRawJson("GET", "/api/memes/$memeId/who")
        if (res.code !in 200..299) return WhoReactedResponse()
        return runCatching { ForHappyJson.decodeFromString<WhoReactedResponse>(res.body) }
            .getOrDefault(WhoReactedResponse())
    }

    suspend fun setMemeNsfw(memeId: Long, on: Boolean): MemeItem? {
        val payload = buildJsonObject { put("on", JsonPrimitive(on)) }
        val res = executeRawJson("POST", "/api/memes/$memeId/nsfw", ForHappyJson.encodeToString(payload))
        if (res.code !in 200..299) return null
        val obj = runCatching { ForHappyJson.parseToJsonElement(res.body).jsonObject }.getOrNull() ?: return null
        val itemEl = obj["item"] ?: obj["meme"] ?: return null
        return runCatching { ForHappyJson.decodeFromJsonElement(MemeItem.serializer(), itemEl) }.getOrNull()
    }

    suspend fun patchMemeBasic(
        memeId: Long,
        title: String,
        content: String,
        tags: List<String>,
        spoiler: Boolean,
    ): Result<MemeItem?> {
        val payload = buildJsonObject {
            put("title", JsonPrimitive(title))
            put("content", JsonPrimitive(content))
            put("tags", JsonArray(tags.map { JsonPrimitive(it) }))
            put("spoiler", JsonPrimitive(spoiler))
        }
        val res = executeRawJson("PATCH", "/api/memes/$memeId", ForHappyJson.encodeToString(payload))
        val obj = runCatching { ForHappyJson.parseToJsonElement(res.body).jsonObject }.getOrNull()
        return if (res.code in 200..299) {
            val itemEl = obj?.get("item") ?: obj?.get("meme")
            val updated = itemEl?.let { runCatching { ForHappyJson.decodeFromJsonElement(MemeItem.serializer(), it) }.getOrNull() }
            Result.success(updated)
        } else {
            val err = runCatching { obj?.get("error")?.jsonPrimitive?.content }.getOrNull() ?: "编辑失败 (${res.code})"
            Result.failure(IllegalStateException(err))
        }
    }

    suspend fun deleteMeme(memeId: Long): Boolean {
        val res = executeRawJson("DELETE", "/api/memes/$memeId", "{}")
        return res.code in 200..299
    }

    suspend fun patchMemeFull(
        memeId: Long,
        title: String,
        content: String,
        tags: List<String>,
        spoiler: Boolean,
        aiOff: Boolean? = null,
        items: JsonArray? = null,
    ): Result<MemeItem?> {
        val payload = buildJsonObject {
            put("title", JsonPrimitive(title))
            put("content", JsonPrimitive(content))
            put("tags", JsonArray(tags.map { JsonPrimitive(it) }))
            put("spoiler", JsonPrimitive(spoiler))
            if (aiOff != null) put("ai_off", JsonPrimitive(aiOff))
            if (items != null) {
                put("items", items)
                put("layout", JsonPrimitive("carousel"))
            }
        }
        val res = executeRawJson("PATCH", "/api/memes/$memeId", ForHappyJson.encodeToString(payload))
        val obj = runCatching { ForHappyJson.parseToJsonElement(res.body).jsonObject }.getOrNull()
        return if (res.code in 200..299) {
            val itemEl = obj?.get("item") ?: obj?.get("meme")
            val updated = itemEl?.let { runCatching { ForHappyJson.decodeFromJsonElement(MemeItem.serializer(), it) }.getOrNull() }
            Result.success(updated)
        } else {
            val err = runCatching { obj?.get("error")?.jsonPrimitive?.content }.getOrNull() ?: "保存失败 (${res.code})"
            Result.failure(IllegalStateException(err))
        }
    }

    suspend fun createMeme(
        title: String,
        content: String,
        tags: List<String>,
        spoiler: Boolean = false,
        uploadedKey: String? = null,
    ): Result<Long> {
        val items = if (!uploadedKey.isNullOrBlank()) {
            JsonArray(listOf(buildJsonObject { put("key", JsonPrimitive(uploadedKey)) }))
        } else {
            JsonArray(emptyList())
        }
        return createMemeFull(
            kind = if (uploadedKey.isNullOrBlank()) "text" else null,
            title = title,
            content = content,
            tags = tags,
            spoiler = spoiler,
            layout = if (uploadedKey.isNullOrBlank()) "grid" else "carousel",
            items = items,
        )
    }

    suspend fun createMemeFull(
        kind: String? = null,
        title: String,
        content: String,
        tags: List<String>,
        spoiler: Boolean = false,
        aiOff: Boolean = false,
        layout: String = "carousel",
        items: JsonArray = JsonArray(emptyList()),
        shareUrl: String? = null,
    ): Result<Long> {
        val payload = buildJsonObject {
            if (!kind.isNullOrBlank()) put("kind", JsonPrimitive(kind))
            if (!shareUrl.isNullOrBlank()) put("share_url", JsonPrimitive(shareUrl))
            put("title", JsonPrimitive(title))
            put("content", JsonPrimitive(content))
            put("tags", JsonArray(tags.map { JsonPrimitive(it) }))
            put("spoiler", JsonPrimitive(spoiler))
            if (shareUrl.isNullOrBlank()) {
                put("layout", JsonPrimitive(layout))
                if (kind == null) put("ai_off", JsonPrimitive(aiOff))
                put("items", items)
            }
        }
        val res = executeRawJson("POST", "/api/memes", ForHappyJson.encodeToString(payload))
        val obj = runCatching { ForHappyJson.parseToJsonElement(res.body).jsonObject }.getOrNull()
        return if (res.code in 200..299) {
            val id = obj?.get("id")?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
            Result.success(id)
        } else {
            Result.failure(IllegalStateException(obj?.get("error")?.jsonPrimitive?.content ?: "发布失败 (${res.code})"))
        }
    }

    suspend fun fetchSocialPreview(shareText: String): Result<SocialPreviewData> {
        val res = executeRawJson("GET", "/api/social/preview?share=${enc(shareText)}")
        val obj = runCatching { ForHappyJson.parseToJsonElement(res.body).jsonObject }.getOrNull()
        if (res.code !in 200..299 || obj == null) {
            val err = runCatching { obj?.get("error")?.jsonPrimitive?.contentOrNull }.getOrNull()
                ?: "链接读取失败 (${res.code})"
            return Result.failure(IllegalStateException(err))
        }
        val available = obj["available"]?.jsonPrimitive?.booleanOrNull ?: false
        val description = obj["description"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val sensitive = obj["sensitive"]?.jsonPrimitive?.booleanOrNull ?: false
        val embedUrl = runCatching { obj["embed"]?.jsonObject?.get("url")?.jsonPrimitive?.contentOrNull }.getOrNull()
        val mediaList = runCatching { obj["media"]?.jsonArray }.getOrNull()?.mapNotNull { elem ->
            val m = runCatching { elem.jsonObject }.getOrNull() ?: return@mapNotNull null
            val url = m["url"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val kind = m["kind"]?.jsonPrimitive?.contentOrNull ?: "image"
            val poster = m["poster"]?.jsonPrimitive?.contentOrNull
            SocialPreviewMedia(kind = kind, url = url, poster = poster)
        }.orEmpty()
        return Result.success(
            SocialPreviewData(
                available = available,
                description = description,
                sensitive = sensitive,
                embedUrl = embedUrl,
                media = mediaList,
            ),
        )
    }

    suspend fun fetchAiComposeStatus(): Pair<Boolean, Boolean> {
        val cfgRes = executeRawJson("GET", "/api/ai/config")
        val prefsRes = executeRawJson("GET", "/api/me/ai-prefs")
        val cfgEnabled = if (cfgRes.code in 200..299) {
            runCatching {
                ForHappyJson.parseToJsonElement(cfgRes.body).jsonObject["img"]?.jsonPrimitive?.booleanOrNull
            }.getOrNull() == true
        } else false
        val prefsEnabled = if (prefsRes.code in 200..299) {
            runCatching {
                ForHappyJson.parseToJsonElement(prefsRes.body).jsonObject["img_reply"]?.jsonPrimitive?.booleanOrNull
            }.getOrNull() == true
        } else false
        return cfgEnabled to prefsEnabled
    }

    suspend fun attachMemePuzzle(memeId: Long, draft: PuzzleDraftSpec): Result<Unit> {
        val layerJson = JsonArray(
            draft.layers.map { layer ->
                val options = layer.optionsRaw.split('\n')
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                val effectiveAnswer = if (layer.kind == "quiz") {
                    layer.answer.ifBlank { options.firstOrNull().orEmpty() }
                } else {
                    layer.answer.trim()
                }
                val dataObj = buildJsonObject {
                    if (layer.kind == "quiz") {
                        put("options", JsonArray(options.map { JsonPrimitive(it) }))
                    }
                    if (!layer.uploadedImgUrl.isNullOrBlank()) {
                        put("img", JsonPrimitive(layer.uploadedImgUrl))
                    }
                }
                buildJsonObject {
                    put("kind", JsonPrimitive(layer.kind))
                    put("prompt", JsonPrimitive(layer.prompt.trim()))
                    put("answer", JsonPrimitive(effectiveAnswer))
                    put("note", JsonPrimitive(layer.note.trim()))
                    put("data", dataObj)
                }
            },
        )
        val payload = buildJsonObject {
            put("layers", layerJson)
            put("hint", JsonPrimitive(draft.hint.trim()))
            put("reward_kind", JsonPrimitive(draft.rewardKind))
            put("reward_type", JsonPrimitive(draft.rewardType))
            put("reward_text", JsonPrimitive(draft.rewardText.trim()))
            put("codes", JsonPrimitive(draft.codes.trim()))
            put("max_winners", JsonPrimitive(draft.maxWinners.coerceAtLeast(0)))
            put("ends_at", JsonNull)
        }
        val res = executeRawJson("POST", "/api/memes/$memeId/puzzle", ForHappyJson.encodeToString(payload))
        val obj = runCatching { ForHappyJson.parseToJsonElement(res.body).jsonObject }.getOrNull()
        return if (res.code in 200..299) {
            Result.success(Unit)
        } else {
            val err = runCatching { obj?.get("error")?.jsonPrimitive?.contentOrNull }.getOrNull() ?: "挂载谜题失败 (${res.code})"
            Result.failure(IllegalStateException(err))
        }
    }

    suspend fun uploadImageBytes(bytes: ByteArray, fileName: String, mimeType: String): Result<UploadedMedia> =
        uploadMediaBytes(bytes = bytes, fileName = fileName, mimeType = mimeType, forWhat = "meme")

    suspend fun uploadMediaBytes(
        bytes: ByteArray,
        fileName: String,
        mimeType: String,
        forWhat: String = "meme",
        nonce: String = UUID.randomUUID().toString(),
    ): Result<UploadedMedia> = withContext(Dispatchers.IO) {
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("for", forWhat)
            .addFormDataPart("nonce", nonce)
            .addFormDataPart("file", fileName, bytes.toRequestBody(mimeType.toMediaType()))
            .build()
        val req = Request.Builder()
            .url("$ORIGIN/api/uploads")
            .post(body)
            .build()
        okHttpClient.newCall(req).execute().use { resp ->
            val raw = resp.body?.string().orEmpty()
            val obj = runCatching { ForHappyJson.parseToJsonElement(raw).jsonObject }.getOrNull()
            if (resp.isSuccessful) {
                val key = obj?.get("key")?.jsonPrimitive?.content.orEmpty()
                val url = obj?.get("url")?.jsonPrimitive?.content
                    ?: obj?.get("path")?.jsonPrimitive?.content
                    ?: key.takeIf { it.isNotBlank() }?.let { "/img/$it" }
                    ?: ""
                if (key.isNotBlank() || url.isNotBlank()) {
                    Result.success(UploadedMedia(key = key.ifBlank { url.removePrefix("/img/") }, url = url))
                } else {
                    Result.failure(IllegalStateException("上传响应缺少图片地址"))
                }
            } else {
                Result.failure(IllegalStateException(obj?.get("error")?.jsonPrimitive?.content ?: "上传失败(${resp.code})"))
            }
        }
    }

    suspend fun fetchAnchors(): List<AnchorTag> {
        val res = executeRawJson("GET", "/api/anchors")
        if (res.code !in 200..299) return emptyList()
        val obj = runCatching { ForHappyJson.parseToJsonElement(res.body).jsonObject }.getOrNull() ?: return emptyList()
        val arr = obj["anchors"] ?: obj["items"] ?: return emptyList()
        return runCatching { ForHappyJson.decodeFromJsonElement(ListSerializer(AnchorTag.serializer()), arr) }
            .getOrDefault(emptyList())
    }

    suspend fun putAnchor(memeId: Long, note: String = ""): List<AnchorTag> {
        val payload = buildJsonObject { put("note", JsonPrimitive(note.take(20))) }
        val res = executeRawJson("PUT", "/api/anchors/$memeId", ForHappyJson.encodeToString(payload))
        if (res.code !in 200..299) return fetchAnchors()
        val obj = runCatching { ForHappyJson.parseToJsonElement(res.body).jsonObject }.getOrNull() ?: return fetchAnchors()
        val arr = obj["anchors"] ?: obj["items"] ?: return fetchAnchors()
        return runCatching { ForHappyJson.decodeFromJsonElement(ListSerializer(AnchorTag.serializer()), arr) }
            .getOrDefault(emptyList())
    }

    suspend fun deleteAnchor(memeId: Long): List<AnchorTag> {
        val res = executeRawJson("DELETE", "/api/anchors/$memeId", "{}")
        if (res.code !in 200..299) return fetchAnchors()
        val obj = runCatching { ForHappyJson.parseToJsonElement(res.body).jsonObject }.getOrNull() ?: return fetchAnchors()
        val arr = obj["anchors"] ?: obj["items"] ?: return fetchAnchors()
        return runCatching { ForHappyJson.decodeFromJsonElement(ListSerializer(AnchorTag.serializer()), arr) }
            .getOrDefault(emptyList())
    }

    suspend fun fetchNotices(): List<SiteNotice> {
        val res = executeRawJson("GET", "/api/notices")
        if (res.code !in 200..299) return emptyList()
        val obj = runCatching { ForHappyJson.parseToJsonElement(res.body).jsonObject }.getOrNull() ?: return emptyList()
        val arr = obj["notices"] ?: obj["items"] ?: return emptyList()
        return runCatching { ForHappyJson.decodeFromJsonElement(ListSerializer(SiteNotice.serializer()), arr) }
            .getOrDefault(emptyList())
    }

    suspend fun fetchAuthors(query: String = "", nsfwModeParam: String? = null): List<AuthorOption> {
        val qs = buildList {
            if (nsfwModeParam != null) add("nsfw=${enc(nsfwModeParam)}")
            if (query.isNotBlank()) add("q=${enc(query.trim())}")
        }.joinToString("&")
        val path = if (qs.isNotEmpty()) "/api/authors?$qs" else "/api/authors"
        val res = executeRawJson("GET", path)
        if (res.code !in 200..299) return emptyList()
        val obj = runCatching { ForHappyJson.parseToJsonElement(res.body).jsonObject }.getOrNull() ?: return emptyList()
        val arr = obj["authors"] ?: obj["items"] ?: return emptyList()
        return runCatching { ForHappyJson.decodeFromJsonElement(ListSerializer(AuthorOption.serializer()), arr) }
            .getOrDefault(emptyList())
    }

    suspend fun fetchSeriesList(
        nsfwModeParam: String? = null,
        unseenOnly: Boolean = false,
        tag: String? = null,
        author: String? = null,
        query: String? = null,
    ): List<SeriesItem> {
        val qs = buildList {
            if (nsfwModeParam != null) add("nsfw=${enc(nsfwModeParam)}")
            if (unseenOnly) add("unseen=1")
            if (!tag.isNullOrBlank()) add("tag=${enc(tag)}")
            if (!author.isNullOrBlank()) add("author=${enc(author)}")
            if (!query.isNullOrBlank()) add("q=${enc(query)}")
        }.joinToString("&")
        val path = if (qs.isNotEmpty()) "/api/series?$qs" else "/api/series"
        val res = executeRawJson("GET", path)
        if (res.code !in 200..299) return emptyList()
        val obj = runCatching { ForHappyJson.parseToJsonElement(res.body).jsonObject }.getOrNull() ?: return emptyList()
        val arr = obj["series"] ?: obj["items"] ?: return emptyList()
        return runCatching { ForHappyJson.decodeFromJsonElement(ListSerializer(SeriesItem.serializer()), arr) }
            .getOrDefault(emptyList())
    }

    suspend fun fetchChatGroupsEnvelope(): ChatGroupsResponse? {
        val res = executeRawJson("GET", "/api/chat/groups")
        if (res.code !in 200..299) return null
        return runCatching { ForHappyJson.decodeFromString<ChatGroupsResponse>(res.body) }.getOrNull()
    }

    suspend fun fetchChatGroups(): List<ChatGroup> {
        return fetchChatGroupsEnvelope()?.groups ?: emptyList()
    }

    suspend fun applyJoinGroup(groupId: Long, adultContentAck: Boolean = true): Result<String> {
        val payload = buildJsonObject {
            if (adultContentAck) put("adult_content_ack", JsonPrimitive(true))
        }
        val res = executeRawJson("POST", "/api/chat/groups/$groupId/join-req", ForHappyJson.encodeToString(payload))
        val obj = runCatching { ForHappyJson.parseToJsonElement(res.body).jsonObject }.getOrNull()
        return if (res.code in 200..299) {
            val status = obj?.get("status")?.jsonPrimitive?.contentOrNull
            val msg = if (status == "joined") "已加入群聊" else "已提交入群申请，请等待审核"
            Result.success(msg)
        } else {
            Result.failure(IllegalStateException(obj?.get("error")?.jsonPrimitive?.contentOrNull ?: "申请失败 (${res.code})"))
        }
    }

    suspend fun setGroupMute(groupId: Long, muted: Boolean): Boolean {
        val payload = buildJsonObject { put("muted", JsonPrimitive(muted)) }
        val res = executeRawJson("PUT", "/api/chat/groups/$groupId/mute", ForHappyJson.encodeToString(payload))
        return res.code in 200..299
    }

    suspend fun updateGroupNotice(groupId: Long, notice: String): Result<Unit> {
        val payload = buildJsonObject { put("notice", JsonPrimitive(notice.trim())) }
        val res = executeRawJson("PUT", "/api/chat/groups/$groupId/notice", ForHappyJson.encodeToString(payload))
        val obj = runCatching { ForHappyJson.parseToJsonElement(res.body).jsonObject }.getOrNull()
        return if (res.code in 200..299) {
            Result.success(Unit)
        } else {
            Result.failure(IllegalStateException(obj?.get("error")?.jsonPrimitive?.contentOrNull ?: "更新群公告失败 (${res.code})"))
        }
    }

    suspend fun fetchGroupMembers(groupId: Long): List<GroupMemberItem> {
        val res = executeRawJson("GET", "/api/chat/groups/$groupId/members")
        if (res.code !in 200..299) return emptyList()
        return runCatching { ForHappyJson.decodeFromString<GroupMembersResponse>(res.body).resolvedMembers }
            .getOrDefault(emptyList())
    }

    suspend fun fetchGroupOutsideUsers(groupId: Long, kind: String = "req"): List<GroupMemberItem> {
        val res = executeRawJson("GET", "/api/chat/groups/$groupId/outside?view=membership&kind=${enc(kind)}")
        if (res.code !in 200..299) return emptyList()
        return runCatching { ForHappyJson.decodeFromString<GroupMembersResponse>(res.body).resolvedMembers }
            .getOrDefault(emptyList())
    }

    suspend fun handleGroupJoinRequest(groupId: Long, userId: Long, accept: Boolean): Result<Unit> {
        val payload = buildJsonObject { put("ok", JsonPrimitive(accept)) }
        val res = executeRawJson("POST", "/api/chat/groups/$groupId/join-req/$userId", ForHappyJson.encodeToString(payload))
        val obj = runCatching { ForHappyJson.parseToJsonElement(res.body).jsonObject }.getOrNull()
        return if (res.code in 200..299) {
            Result.success(Unit)
        } else {
            Result.failure(IllegalStateException(obj?.get("error")?.jsonPrimitive?.contentOrNull ?: "审批失败 (${res.code})"))
        }
    }

    suspend fun kickOrLeaveGroupMember(groupId: Long, userId: Long): Boolean {
        val res = executeRawJson("DELETE", "/api/chat/groups/$groupId/members/$userId", "{}")
        return res.code in 200..299
    }

    suspend fun searchChatUsers(query: String): List<ChatUser> {
        val res = executeRawJson("GET", "/api/chat/users?q=${enc(query.trim())}")
        if (res.code !in 200..299) return emptyList()
        val obj = runCatching { ForHappyJson.parseToJsonElement(res.body).jsonObject }.getOrNull() ?: return emptyList()
        val arr = obj["users"] ?: obj["items"] ?: return emptyList()
        return runCatching { ForHappyJson.decodeFromJsonElement(ListSerializer(ChatUser.serializer()), arr) }
            .getOrDefault(emptyList())
    }

    suspend fun fetchChatMessages(groupId: Long, afterId: Long = 0L, sinceSec: Long = 0L, beforeId: Long? = null): ChatMessagesResponse? {
        val qs = buildList {
            if (beforeId != null && beforeId > 0L) {
                add("before=$beforeId")
            } else {
                add("after=$afterId")
                add("since=$sinceSec")
                add("include_total=0")
            }
        }.joinToString("&")
        val res = executeRawJson("GET", "/api/chat/groups/$groupId/messages?$qs")
        if (res.code !in 200..299) return null
        return decodeChatMessagesResponse(res.body)
    }

    suspend fun fetchChatMessagesAround(groupId: Long, aroundId: Long): ChatMessagesResponse? {
        val res = executeRawJson("GET", "/api/chat/groups/$groupId/messages?around=$aroundId")
        if (res.code !in 200..299) return null
        return decodeChatMessagesResponse(res.body)
    }

    private fun decodeChatMessagesResponse(rawBody: String): ChatMessagesResponse? {
        val obj = runCatching { ForHappyJson.parseToJsonElement(rawBody).jsonObject }.getOrNull() ?: return null
        val items = runCatching { obj["items"]?.jsonArray }.getOrNull()?.mapNotNull { el ->
            runCatching { ForHappyJson.decodeFromJsonElement(ChatMessage.serializer(), el) }.getOrNull()
        }.orEmpty()
        val changed = runCatching { obj["changed"]?.jsonArray }.getOrNull()?.mapNotNull { el ->
            runCatching { ForHappyJson.decodeFromJsonElement(ChatMessage.serializer(), el) }.getOrNull()
        }.orEmpty()
        val more = obj["more"]?.jsonPrimitive?.booleanOrNull ?: false
        val moreAfter = obj["more_after"]?.jsonPrimitive?.booleanOrNull ?: false
        val lastRead = obj["last_read"]?.jsonPrimitive?.longOrNull ?: 0L
        val total = obj["total"]?.jsonPrimitive?.longOrNull
        val now = obj["now"]?.jsonPrimitive?.doubleOrNull
        return ChatMessagesResponse(
            items = items,
            changed = changed,
            more = more,
            moreAfter = moreAfter,
            lastRead = lastRead,
            total = total,
            now = now,
        )
    }

    suspend fun searchChatMessages(
        groupId: Long,
        q: String,
        user: String = "",
        onlyImages: Boolean = false,
        beforeId: Long? = null,
    ): ChatSearchResponse {
        val qs = buildList {
            if (q.isNotBlank()) add("q=${enc(q.trim())}")
            if (user.isNotBlank()) add("user=${enc(user.trim())}")
            if (onlyImages) add("img=1")
            if (beforeId != null && beforeId > 0L) add("before=$beforeId")
        }.joinToString("&")
        val res = executeRawJson("GET", "/api/chat/groups/$groupId/search?$qs")
        if (res.code !in 200..299) return ChatSearchResponse()
        return runCatching { ForHappyJson.decodeFromString<ChatSearchResponse>(res.body) }.getOrDefault(ChatSearchResponse())
    }

    suspend fun sendChatMessage(groupId: Long, body: String, replyToId: Long?, retry: Boolean): Result<ChatMessage?> {
        val payload = buildJsonObject {
            put("body", JsonPrimitive(body))
            put("reply_to", JsonPrimitive(replyToId ?: 0L))
            if (retry) put("retry", JsonPrimitive(true))
        }
        val res = executeRawJson("POST", "/api/chat/groups/$groupId/messages", ForHappyJson.encodeToString(payload))
        val obj = runCatching { ForHappyJson.parseToJsonElement(res.body).jsonObject }.getOrNull()
        return if (res.code in 200..299) {
            val msgElem = obj?.get("message") ?: obj?.get("item")
            val parsed = msgElem?.let { runCatching { ForHappyJson.decodeFromJsonElement(ChatMessage.serializer(), it) }.getOrNull() }
            Result.success(parsed)
        } else {
            Result.failure(IllegalStateException(obj?.get("error")?.jsonPrimitive?.content ?: "发送失败 (${res.code})"))
        }
    }

    suspend fun sendChatMessageWithMedia(
        groupId: Long,
        body: String,
        media: JsonArray,
        replyToId: Long? = null,
    ): Result<ChatMessage?> {
        val payload = buildJsonObject {
            put("body", JsonPrimitive(body))
            put("reply_to", JsonPrimitive(replyToId ?: 0L))
            if (media.isNotEmpty()) put("media", media)
        }
        val res = executeRawJson("POST", "/api/chat/groups/$groupId/messages", ForHappyJson.encodeToString(payload))
        val obj = runCatching { ForHappyJson.parseToJsonElement(res.body).jsonObject }.getOrNull()
        return if (res.code in 200..299) {
            val msgElem = obj?.get("message") ?: obj?.get("item")
            val parsed = msgElem?.let { runCatching { ForHappyJson.decodeFromJsonElement(ChatMessage.serializer(), it) }.getOrNull() }
            Result.success(parsed)
        } else {
            Result.failure(IllegalStateException(obj?.get("error")?.jsonPrimitive?.content ?: "发送失败 (${res.code})"))
        }
    }

    suspend fun chatMessageToPost(
        msgId: Long,
        tags: List<String>,
        postAs: String = "author",
    ): Result<Long> {
        val payload = buildJsonObject {
            put("tags", JsonArray(tags.map { JsonPrimitive(it) }))
            put("as", JsonPrimitive(postAs))
        }
        val res = executeRawJson("POST", "/api/chat/messages/$msgId/to-post", ForHappyJson.encodeToString(payload))
        val obj = runCatching { ForHappyJson.parseToJsonElement(res.body).jsonObject }.getOrNull()
        return if (res.code in 200..299) {
            val id = obj?.get("id")?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
            Result.success(id)
        } else {
            Result.failure(IllegalStateException(obj?.get("error")?.jsonPrimitive?.content ?: "转帖失败 (${res.code})"))
        }
    }

    suspend fun markChatRead(groupId: Long, lastId: Long): Boolean {
        val res = executeRawJson("POST", "/api/chat/groups/$groupId/read", """{"id":$lastId}""")
        return res.code in 200..299
    }

    suspend fun toggleChatReaction(msgId: Long, emoji: String): Boolean {
        return postChatReaction(msgId, emoji) != null
    }

    suspend fun postChatReaction(msgId: Long, emoji: String): ChatMessage? {
        val payload = buildJsonObject { put("emoji", JsonPrimitive(emoji)) }
        val res = executeRawJson("POST", "/api/chat/messages/$msgId/reactions", ForHappyJson.encodeToString(payload))
        if (res.code !in 200..299) return null
        val obj = runCatching { ForHappyJson.parseToJsonElement(res.body).jsonObject }.getOrNull() ?: return null
        val msgEl = obj["message"] ?: obj["item"] ?: return null
        return runCatching { ForHappyJson.decodeFromJsonElement(ChatMessage.serializer(), msgEl) }.getOrNull()
    }

    suspend fun postChatBoost(msgId: Long, text: String): Result<ChatMessage?> {
        val payload = buildJsonObject { put("text", JsonPrimitive(text)) }
        val res = executeRawJson("POST", "/api/chat/messages/$msgId/boosts", ForHappyJson.encodeToString(payload))
        val obj = runCatching { ForHappyJson.parseToJsonElement(res.body).jsonObject }.getOrNull()
        return if (res.code in 200..299) {
            val msgEl = obj?.get("message") ?: obj?.get("item")
            val parsed = msgEl?.let { runCatching { ForHappyJson.decodeFromJsonElement(ChatMessage.serializer(), it) }.getOrNull() }
            Result.success(parsed)
        } else {
            val err = runCatching { obj?.get("error")?.jsonPrimitive?.content }.getOrNull() ?: "回应失败 (${res.code})"
            Result.failure(IllegalStateException(err))
        }
    }

    suspend fun fetchChatMessageReplies(msgId: Long): List<ChatReplyItem> {
        val res = executeRawJson("GET", "/api/chat/messages/$msgId/replies")
        if (res.code !in 200..299) return emptyList()
        return runCatching { ForHappyJson.decodeFromString<ChatRepliesResponse>(res.body).items }
            .getOrDefault(emptyList())
    }

    suspend fun editChatMessage(msgId: Long, body: String): Boolean {
        val payload = buildJsonObject { put("body", JsonPrimitive(body)) }
        val res = executeRawJson("PATCH", "/api/chat/messages/$msgId", ForHappyJson.encodeToString(payload))
        return res.code in 200..299
    }

    suspend fun deleteChatMessage(msgId: Long): Boolean {
        val res = executeRawJson("DELETE", "/api/chat/messages/$msgId", "{}")
        return res.code in 200..299
    }

    suspend fun fetchStickers(): List<StickerEntry> {
        val res = executeRawJson("GET", "/api/chat/stickers")
        if (res.code !in 200..299) return emptyList()
        val obj = runCatching { ForHappyJson.parseToJsonElement(res.body).jsonObject }.getOrNull() ?: return emptyList()
        val arr = obj["stickers"] ?: obj["items"] ?: return emptyList()
        return runCatching { ForHappyJson.decodeFromJsonElement(ListSerializer(StickerEntry.serializer()), arr) }
            .getOrDefault(emptyList())
    }

    suspend fun fetchStickerPacks(): List<StickerPack> {
        val raw = runCatching { executeText("/stickers/packs.json") }.getOrNull() ?: return emptyList()
        return runCatching { ForHappyJson.decodeFromString(ListSerializer(StickerPack.serializer()), raw) }
            .getOrDefault(emptyList())
    }

    suspend fun fetchRedpackDetail(msgId: Long): JsonObject? {
        val res = executeRawJson("GET", "/api/chat/messages/$msgId/redpack")
        if (res.code !in 200..299) return null
        return runCatching { ForHappyJson.parseToJsonElement(res.body).jsonObject }.getOrNull()
    }

    suspend fun openRedpack(msgId: Long, answer: String? = null): JsonObject {
        val payload = buildJsonObject {
            if (!answer.isNullOrBlank()) put("answer", JsonPrimitive(answer))
        }
        val res = executeRawJson("POST", "/api/chat/messages/$msgId/redpack/open", ForHappyJson.encodeToString(payload))
        return runCatching { ForHappyJson.parseToJsonElement(res.body).jsonObject }.getOrDefault(JsonObject(emptyMap()))
    }

    suspend fun claimCoin52Redpack(msgId: Long, answer: String? = null): JsonObject {
        val payload = buildJsonObject {
            if (!answer.isNullOrBlank()) put("answer", JsonPrimitive(answer))
        }
        val res = executeRawJson("POST", "/api/chat/messages/$msgId/redpack/coin52-claim", ForHappyJson.encodeToString(payload))
        return runCatching { ForHappyJson.parseToJsonElement(res.body).jsonObject }.getOrDefault(JsonObject(emptyMap()))
    }

    suspend fun postLdcRedpackAction(
        msgId: Long,
        action: String, // "claim" | "enter" | "reconcile" | "close"
        answer: String? = null,
    ): JsonObject {
        val payload = buildJsonObject {
            if (!answer.isNullOrBlank()) put("answer", JsonPrimitive(answer))
        }
        val res = executeRawJson("POST", "/api/chat/messages/$msgId/ldc/$action", ForHappyJson.encodeToString(payload))
        return runCatching { ForHappyJson.parseToJsonElement(res.body).jsonObject }.getOrDefault(JsonObject(emptyMap()))
    }

    suspend fun claimLdcRedpack(msgId: Long): JsonObject {
        return postLdcRedpackAction(msgId, "claim")
    }

    suspend fun createGroupRedpack(groupId: Long, payload: JsonObject): Result<ChatMessage?> {
        val res = executeRawJson("POST", "/api/chat/groups/$groupId/redpacks", ForHappyJson.encodeToString(payload))
        val obj = runCatching { ForHappyJson.parseToJsonElement(res.body).jsonObject }.getOrNull()
        return if (res.code in 200..299) {
            val msgEl = obj?.get("message") ?: obj?.get("item")
            val parsed = msgEl?.let { runCatching { ForHappyJson.decodeFromJsonElement(ChatMessage.serializer(), it) }.getOrNull() }
            Result.success(parsed)
        } else {
            val err = obj?.get("error")?.jsonPrimitive?.contentOrNull
                ?: obj?.get("message")?.jsonPrimitive?.contentOrNull
                ?: "发送红包失败 (${res.code})"
            Result.failure(IllegalStateException(err))
        }
    }

    suspend fun fetchGroupRedpacksArchive(groupId: Long, view: String = "all"): List<RedpackArchiveItem> {
        val res = executeRawJson("GET", "/api/chat/groups/$groupId/redpacks?view=${enc(view)}")
        if (res.code !in 200..299) return emptyList()
        val obj = runCatching { ForHappyJson.parseToJsonElement(res.body).jsonObject }.getOrNull() ?: return emptyList()
        val arr = obj["items"] ?: obj["redpacks"] ?: return emptyList()
        return runCatching { ForHappyJson.decodeFromJsonElement(ListSerializer(RedpackArchiveItem.serializer()), arr) }
            .getOrDefault(emptyList())
    }

    suspend fun fetchGroupSenderStats(
        groupId: Long,
        period: String = "month",
        metric: String = "amount",
    ): List<SenderStatItem> {
        val res = executeRawJson(
            "GET",
            "/api/chat/groups/$groupId/sender-stats?period=${enc(period)}&metric=${enc(metric)}",
        )
        if (res.code !in 200..299) return emptyList()
        val obj = runCatching { ForHappyJson.parseToJsonElement(res.body).jsonObject }.getOrNull() ?: return emptyList()
        val arr = obj["items"] ?: obj["leaders"] ?: obj["stats"] ?: return emptyList()
        return runCatching { ForHappyJson.decodeFromJsonElement(ListSerializer(SenderStatItem.serializer()), arr) }
            .getOrDefault(emptyList())
    }

    suspend fun fetchFishFunding(): FishFundingStatus? {
        val res = executeRawJson("GET", "/api/me/fish")
        if (res.code !in 200..299) return null
        return runCatching { ForHappyJson.decodeFromString<FishFundingStatus>(res.body) }.getOrNull()
    }

    suspend fun saveFishApiKey(apiKey: String): Result<FishFundingStatus?> {
        val payload = buildJsonObject { put("api_key", JsonPrimitive(apiKey.trim())) }
        val res = executeRawJson("POST", "/api/me/fish", ForHappyJson.encodeToString(payload))
        val obj = runCatching { ForHappyJson.parseToJsonElement(res.body).jsonObject }.getOrNull()
        return if (res.code in 200..299) {
            Result.success(runCatching { ForHappyJson.decodeFromString<FishFundingStatus>(res.body) }.getOrNull())
        } else {
            Result.failure(IllegalStateException(obj?.get("error")?.jsonPrimitive?.contentOrNull ?: "绑定 Fish 失败 (${res.code})"))
        }
    }

    suspend fun disableFishFunding(): Boolean {
        val res = executeRawJson("POST", "/api/me/fish/disable", "{}")
        return res.code in 200..299
    }

    suspend fun fetchCreditFunding(): CreditFundingStatus? {
        val res = executeRawJson("GET", "/api/me/ldc")
        if (res.code !in 200..299) return null
        return runCatching { ForHappyJson.decodeFromString<CreditFundingStatus>(res.body) }.getOrNull()
    }

    suspend fun saveCreditApp(clientId: String, clientSecret: String): Result<CreditFundingStatus?> {
        val payload = buildJsonObject {
            put("client_id", JsonPrimitive(clientId.trim()))
            put("client_secret", JsonPrimitive(clientSecret.trim()))
        }
        val res = executeRawJson("POST", "/api/me/ldc", ForHappyJson.encodeToString(payload))
        val obj = runCatching { ForHappyJson.parseToJsonElement(res.body).jsonObject }.getOrNull()
        return if (res.code in 200..299) {
            Result.success(runCatching { ForHappyJson.decodeFromString<CreditFundingStatus>(res.body) }.getOrNull())
        } else {
            Result.failure(IllegalStateException(obj?.get("error")?.jsonPrimitive?.contentOrNull ?: "绑定 Credit 失败 (${res.code})"))
        }
    }

    suspend fun setCreditChannelEnabled(enabled: Boolean): Boolean {
        val payload = buildJsonObject { put("enabled", JsonPrimitive(enabled)) }
        val res = executeRawJson("POST", "/api/me/ldc/channel", ForHappyJson.encodeToString(payload))
        return res.code in 200..299
    }

    suspend fun fetchGuessOverview(
        groupId: Long,
        messageId: Long? = null,
        extra: Boolean = false,
        receiptId: Long? = null,
    ): GuessRoomOverviewResponse? {
        val qs = buildList {
            add("view=overview")
            if (extra) add("extra=1")
            if (messageId != null && messageId > 0L) add("message_id=$messageId")
            if (receiptId != null && receiptId > 0L) add("receipt_id=$receiptId")
        }.joinToString("&")
        val res = executeRawJson("GET", "/api/chat/groups/$groupId/guess-room?$qs")
        if (res.code !in 200..299) return null
        return runCatching { ForHappyJson.decodeFromString<GuessRoomOverviewResponse>(res.body) }.getOrNull()
    }

    suspend fun submitGuessInput(
        messageId: Long,
        body: String,
        nonce: String = UUID.randomUUID().toString(),
    ): Result<Long?> {
        val payload = buildJsonObject {
            put("body", JsonPrimitive(body))
            put("nonce", JsonPrimitive(nonce))
        }
        val res = executeRawJson("POST", "/api/chat/messages/$messageId/guess-room", ForHappyJson.encodeToString(payload))
        val obj = runCatching { ForHappyJson.parseToJsonElement(res.body).jsonObject }.getOrNull()
        return if (res.code in 200..299) {
            val entryId = runCatching { obj?.get("entry")?.jsonObject?.get("id")?.jsonPrimitive?.content?.toLongOrNull() }.getOrNull()
            Result.success(entryId)
        } else {
            val err = obj?.get("error")?.jsonPrimitive?.content ?: "发送失败 (${res.code})"
            Result.failure(IllegalStateException(err))
        }
    }

    suspend fun submitGuessHint(messageId: Long, text: String): Result<Unit> {
        val payload = buildJsonObject {
            put("mid", JsonPrimitive(messageId))
            put("text", JsonPrimitive(text))
            put("nonce", JsonPrimitive(UUID.randomUUID().toString()))
        }
        val res = executeRawJson("POST", "/api/chat/messages/$messageId/guess-hints", ForHappyJson.encodeToString(payload))
        val obj = runCatching { ForHappyJson.parseToJsonElement(res.body).jsonObject }.getOrNull()
        return if (res.code in 200..299) {
            Result.success(Unit)
        } else {
            Result.failure(IllegalStateException(obj?.get("error")?.jsonPrimitive?.content ?: "发布失败 (${res.code})"))
        }
    }

    suspend fun fetchGuessHistory(groupId: Long, cursor: String? = null): GuessHistoryResponse {
        val qs = buildString {
            append("view=history")
            if (!cursor.isNullOrBlank()) append("&cursor=").append(enc(cursor))
        }
        val res = executeRawJson("GET", "/api/chat/groups/$groupId/guess-room?$qs")
        if (res.code !in 200..299) return GuessHistoryResponse()
        return runCatching { ForHappyJson.decodeFromString<GuessHistoryResponse>(res.body) }.getOrDefault(GuessHistoryResponse())
    }

    suspend fun fetchGuessRoundResult(groupId: Long, messageId: Long): GuessRound? {
        val res = executeRawJson("GET", "/api/chat/groups/$groupId/guess-room?view=result&message_id=$messageId")
        if (res.code !in 200..299) return null
        val obj = runCatching { ForHappyJson.parseToJsonElement(res.body).jsonObject }.getOrNull() ?: return null
        val roundEl = obj["round"] ?: return null
        return runCatching { ForHappyJson.decodeFromJsonElement(GuessRound.serializer(), roundEl) }.getOrNull()
    }

    suspend fun fetchGuessStats(groupId: Long, period: String = "today"): JsonObject? {
        val res = executeRawJson("GET", "/api/chat/groups/$groupId/guess-stats?period=${enc(period)}")
        if (res.code !in 200..299) return null
        return runCatching { ForHappyJson.parseToJsonElement(res.body).jsonObject }.getOrNull()
    }

    suspend fun fetchGuessQueue(groupId: Long): JsonObject? {
        val res = executeRawJson("GET", "/api/chat/groups/$groupId/guess-queue")
        if (res.code !in 200..299) return null
        return runCatching { ForHappyJson.parseToJsonElement(res.body).jsonObject }.getOrNull()
    }

    suspend fun postGuessQueueAction(
        groupId: Long,
        action: String, // "draft" | "queue"
        config: JsonObject,
        nonce: String = UUID.randomUUID().toString(),
    ): Result<JsonObject> {
        val payload = buildJsonObject {
            put("action", JsonPrimitive(action))
            put("nonce", JsonPrimitive(nonce))
            put("config", config)
        }
        val res = executeRawJson("POST", "/api/chat/groups/$groupId/guess-queue", ForHappyJson.encodeToString(payload))
        val obj = runCatching { ForHappyJson.parseToJsonElement(res.body).jsonObject }.getOrNull()
        return if (res.code in 200..299) {
            Result.success(obj ?: JsonObject(emptyMap()))
        } else {
            Result.failure(IllegalStateException(obj?.get("error")?.jsonPrimitive?.contentOrNull ?: "队列操作失败 (${res.code})"))
        }
    }

    suspend fun deleteGuessQueueItem(groupId: Long, itemId: Long): Boolean {
        val res = executeRawJson("DELETE", "/api/chat/groups/$groupId/guess-queue/$itemId", "{}")
        return res.code in 200..299
    }

    suspend fun generateGuessAutoWord(groupId: Long, payload: JsonObject): Result<JsonObject> {
        val res = executeRawJson("POST", "/api/chat/groups/$groupId/ldc/auto-word", ForHappyJson.encodeToString(payload))
        val obj = runCatching { ForHappyJson.parseToJsonElement(res.body).jsonObject }.getOrNull()
        return if (res.code in 200..299 && obj != null) {
            Result.success(obj)
        } else {
            Result.failure(IllegalStateException(obj?.get("error")?.jsonPrimitive?.contentOrNull ?: "AI 出题失败 (${res.code})"))
        }
    }

    suspend fun fetchGuessCloseStatus(messageId: Long): JsonObject? {
        val res = executeRawJson("GET", "/api/chat/messages/$messageId/guess-close")
        if (res.code !in 200..299) return null
        return runCatching { ForHappyJson.parseToJsonElement(res.body).jsonObject }.getOrNull()
    }

    suspend fun postGuessCloseAction(messageId: Long, action: String): Result<JsonObject> {
        val payload = buildJsonObject { put("action", JsonPrimitive(action)) }
        val res = executeRawJson("POST", "/api/chat/messages/$messageId/guess-close", ForHappyJson.encodeToString(payload))
        val obj = runCatching { ForHappyJson.parseToJsonElement(res.body).jsonObject }.getOrNull()
        return if (res.code in 200..299) {
            Result.success(obj ?: JsonObject(emptyMap()))
        } else {
            Result.failure(IllegalStateException(obj?.get("error")?.jsonPrimitive?.contentOrNull ?: "流局操作失败 (${res.code})"))
        }
    }

    suspend fun fetchGuessTipInfo(messageId: Long): JsonObject? {
        val res = executeRawJson("GET", "/api/chat/messages/$messageId/guess-tip")
        if (res.code !in 200..299) return null
        return runCatching { ForHappyJson.parseToJsonElement(res.body).jsonObject }.getOrNull()
    }

    suspend fun submitGuessTip(
        messageId: Long,
        amount: String,
        nonce: String = UUID.randomUUID().toString(),
    ): Result<JsonObject> {
        val payload = buildJsonObject {
            put("amount", JsonPrimitive(amount.trim()))
            put("nonce", JsonPrimitive(nonce))
            put("confirm", JsonPrimitive(true))
        }
        val res = executeRawJson("POST", "/api/chat/messages/$messageId/guess-tip", ForHappyJson.encodeToString(payload))
        val obj = runCatching { ForHappyJson.parseToJsonElement(res.body).jsonObject }.getOrNull()
        return if (res.code in 200..299) {
            Result.success(obj ?: JsonObject(emptyMap()))
        } else {
            Result.failure(IllegalStateException(obj?.get("error")?.jsonPrimitive?.contentOrNull ?: "打赏失败 (${res.code})"))
        }
    }

    suspend fun fetchActiveTitleRaffles(groupId: Long = 1L): List<TitleRaffleItem> {
        val res = executeRawJson("GET", "/api/chat/$groupId/title-raffles/active")
        if (res.code !in 200..299) return emptyList()
        val obj = runCatching { ForHappyJson.parseToJsonElement(res.body).jsonObject }.getOrNull() ?: return emptyList()
        val arr = obj["items"] ?: obj["raffles"] ?: return emptyList()
        return runCatching { ForHappyJson.decodeFromJsonElement(ListSerializer(TitleRaffleItem.serializer()), arr) }
            .getOrDefault(emptyList())
    }

    suspend fun joinActiveTitleRaffle(raffleId: Long): String {
        val res = executeRawJson("POST", "/api/chat/1/title-raffles/$raffleId/join", "{}")
        val obj = runCatching { ForHappyJson.parseToJsonElement(res.body).jsonObject }.getOrNull()
        return obj?.get("error")?.jsonPrimitive?.content ?: if (res.code in 200..299) "已报名头衔抽奖" else "报名失败 (${res.code})"
    }

    suspend fun fetchDmThreads(): List<DmThread> {
        val res = executeRawJson("GET", "/api/dm/threads")
        if (res.code !in 200..299) return emptyList()
        val obj = runCatching { ForHappyJson.parseToJsonElement(res.body).jsonObject }.getOrNull() ?: return emptyList()
        val arr = obj["threads"] ?: obj["items"] ?: return emptyList()
        return runCatching { ForHappyJson.decodeFromJsonElement(ListSerializer(DmThread.serializer()), arr) }
            .getOrDefault(emptyList())
    }

    suspend fun searchDmUsers(query: String): List<ChatUser> {
        val res = executeRawJson("GET", "/api/dm/users?q=${enc(query.trim())}")
        if (res.code !in 200..299) return emptyList()
        val obj = runCatching { ForHappyJson.parseToJsonElement(res.body).jsonObject }.getOrNull() ?: return emptyList()
        val arr = obj["users"] ?: obj["items"] ?: return emptyList()
        return runCatching { ForHappyJson.decodeFromJsonElement(ListSerializer(ChatUser.serializer()), arr) }
            .getOrDefault(emptyList())
    }

    suspend fun fetchDmUser(userId: Long): ChatUser? {
        val res = executeRawJson("GET", "/api/dm/users/$userId")
        if (res.code !in 200..299) return null
        val obj = runCatching { ForHappyJson.parseToJsonElement(res.body).jsonObject }.getOrNull() ?: return null
        val uEl = obj["user"] ?: return null
        return runCatching { ForHappyJson.decodeFromJsonElement(ChatUser.serializer(), uEl) }.getOrNull()
    }

    suspend fun fetchDmMessagesFull(
        threadId: Long,
        afterId: Long = 0L,
        sinceSec: Long = 0L,
        beforeId: Long? = null,
    ): DmMessagesResponse? {
        val qs = buildList {
            when {
                beforeId != null && beforeId > 0L -> add("before=$beforeId")
                afterId > 0L -> {
                    add("after=$afterId")
                    if (sinceSec > 0L) add("since=$sinceSec")
                }
            }
        }.joinToString("&")
        val suffix = if (qs.isNotEmpty()) "?$qs" else ""
        val res = executeRawJson("GET", "/api/dm/threads/$threadId/messages$suffix")
        if (res.code !in 200..299) return null
        return runCatching { ForHappyJson.decodeFromString<DmMessagesResponse>(res.body) }.getOrNull()
    }

    suspend fun fetchDmMessages(threadId: Long): List<DmMessage> {
        return fetchDmMessagesFull(threadId)?.resolvedMessages ?: emptyList()
    }

    suspend fun sendDmToThreadOrUser(
        threadId: Long? = null,
        peerUserId: Long? = null,
        body: String,
        media: JsonArray = JsonArray(emptyList()),
        replyToId: Long? = null,
        nonce: String = UUID.randomUUID().toString(),
    ): Result<Pair<Long, DmMessage?>> {
        val endpoint = when {
            threadId != null && threadId > 0L -> "/api/dm/threads/$threadId/messages"
            peerUserId != null && peerUserId > 0L -> "/api/dm/users/$peerUserId/messages"
            else -> return Result.failure(IllegalArgumentException("缺少私信对象"))
        }
        val payload = buildJsonObject {
            put("body", JsonPrimitive(body))
            if (media.isNotEmpty()) put("media", media)
            if (replyToId != null && replyToId > 0L) put("reply_to", JsonPrimitive(replyToId))
            put("nonce", JsonPrimitive(nonce))
        }
        val res = executeRawJson("POST", endpoint, ForHappyJson.encodeToString(payload))
        val obj = runCatching { ForHappyJson.parseToJsonElement(res.body).jsonObject }.getOrNull()
        return if (res.code in 200..299) {
            val resolvedTid = obj?.get("thread_id")?.jsonPrimitive?.longOrNull ?: threadId ?: 0L
            val msgEl = obj?.get("message") ?: obj?.get("item")
            val msg = msgEl?.let { runCatching { ForHappyJson.decodeFromJsonElement(DmMessage.serializer(), it) }.getOrNull() }
            Result.success(resolvedTid to msg)
        } else {
            Result.failure(IllegalStateException(obj?.get("error")?.jsonPrimitive?.contentOrNull ?: "私信发送失败 (${res.code})"))
        }
    }

    suspend fun sendDmMessage(threadId: Long, body: String): Boolean {
        return sendDmToThreadOrUser(threadId = threadId, body = body).isSuccess
    }

    suspend fun sendDmMessageWithMedia(
        threadId: Long,
        body: String,
        media: JsonArray,
        fallbackImageUrls: List<String> = emptyList(),
    ): Boolean {
        val effectiveBody = if (body.isNotBlank()) body else fallbackImageUrls.joinToString("\n")
        return sendDmToThreadOrUser(threadId = threadId, body = effectiveBody, media = media).isSuccess
    }

    suspend fun editDmMessage(msgId: Long, body: String): Boolean {
        val payload = buildJsonObject { put("body", JsonPrimitive(body.trim())) }
        val res = executeRawJson("PATCH", "/api/dm/messages/$msgId", ForHappyJson.encodeToString(payload))
        return res.code in 200..299
    }

    suspend fun deleteDmMessage(msgId: Long): Boolean {
        val res = executeRawJson("DELETE", "/api/dm/messages/$msgId", "{}")
        return res.code in 200..299
    }

    suspend fun markDmRead(threadId: Long, throughId: Long): Boolean {
        if (throughId <= 0L) return false
        val res = executeRawJson("PUT", "/api/dm/threads/$threadId/read", """{"through":$throughId}""")
        return res.code in 200..299
    }

    suspend fun fetchNotifsPayload(): NotifsPayload {
        val res = executeRawJson("GET", "/api/notifs?system=1")
        if (res.code !in 200..299) return NotifsPayload()
        val obj = runCatching { ForHappyJson.parseToJsonElement(res.body).jsonObject }.getOrNull()
            ?: return NotifsPayload()
        val serverUnread = obj["unread"]?.jsonPrimitive?.intOrNull
        val arr = (obj["items"] ?: obj["notifs"]) as? JsonArray
            ?: return NotifsPayload(unread = serverUnread ?: 0)
        val decoded = arr.mapNotNull { elem ->
            runCatching { ForHappyJson.decodeFromJsonElement(NotifItem.serializer(), elem) }.getOrNull()
        }.sortedWith(
            compareByDescending<NotifItem> { it.isUnread }
                .thenByDescending { it.effectiveAtSec },
        )
        return NotifsPayload(
            unread = serverUnread ?: decoded.count { it.isUnread },
            items = decoded,
        )
    }

    suspend fun fetchNotifs(): List<NotifItem> = fetchNotifsPayload().items

    suspend fun markAllNotifsRead(): Boolean {
        val res = executeRawJson("POST", "/api/notifs/read", "{}")
        return res.code in 200..299
    }

    suspend fun markSingleNotifRead(item: NotifItem): Boolean {
        val itemObj = buildJsonObject {
            put("kind", JsonPrimitive(item.kind))
            item.memeId?.let { put("meme_id", JsonPrimitive(it)) }
            item.userId?.let { put("user_id", JsonPrimitive(it)) }
            if (item.at > 0L) put("at", JsonPrimitive(item.at))
            item.gid?.let { put("gid", JsonPrimitive(it)) }
        }
        val payload = buildJsonObject {
            put("item", itemObj)
        }
        val res = executeRawJson("POST", "/api/notifs/read", ForHappyJson.encodeToString(payload))
        return res.code in 200..299
    }

    suspend fun updateNotifPref(key: String, enabled: Boolean): Map<String, Boolean>? {
        val payload = buildJsonObject {
            put(key, JsonPrimitive(enabled))
        }
        val res = executeRawJson("PUT", "/api/notifs/prefs", ForHappyJson.encodeToString(payload))
        if (res.code !in 200..299) return null
        val prefsObj = runCatching {
            ForHappyJson.parseToJsonElement(res.body).jsonObject["prefs"]?.jsonObject
        }.getOrNull() ?: return null
        return buildMap {
            prefsObj.forEach { (k, v) ->
                v.jsonPrimitive.booleanOrNull?.let { put(k, it) }
            }
        }
    }

    suspend fun fetchMeStats(): JsonObject? {
        val res = executeRawJson("GET", "/api/me/stats")
        return if (res.code in 200..299) runCatching { ForHappyJson.parseToJsonElement(res.body).jsonObject }.getOrNull() else null
    }

    suspend fun fetchMeToday(): JsonObject? {
        val res = executeRawJson("GET", "/api/me/today")
        return if (res.code in 200..299) runCatching { ForHappyJson.parseToJsonElement(res.body).jsonObject }.getOrNull() else null
    }

    suspend fun fetchTodayStats(): TodayStats? {
        val res = executeRawJson("GET", "/api/me/today")
        if (res.code !in 200..299) return null
        return runCatching { ForHappyJson.decodeFromString<TodayStats>(res.body) }.getOrNull()
    }

    suspend fun fetchWallet(beforeCursor: String? = null): WalletSummary? {
        val suffix = if (!beforeCursor.isNullOrBlank()) "&before=${enc(beforeCursor)}" else ""
        val res = executeRawJson("GET", "/api/me/wallet?ledger=1$suffix")
        if (res.code !in 200..299) return null
        return runCatching { ForHappyJson.decodeFromString<WalletSummary>(res.body) }.getOrNull()
    }

    suspend fun fetchHbKeyStatus(): HbKeyStatus {
        val res = executeRawJson("GET", "/api/me/hb-key")
        if (res.code !in 200..299) return HbKeyStatus()
        val obj = runCatching { ForHappyJson.parseToJsonElement(res.body).jsonObject }.getOrNull() ?: return HbKeyStatus()
        val keyObj = runCatching { obj["key"]?.jsonObject }.getOrNull()
        return HbKeyStatus(
            bound = obj["bound"]?.jsonPrimitive?.booleanOrNull ?: false,
            keyId = keyObj?.get("id")?.jsonPrimitive?.contentOrNull,
            revealedKey = obj["key"]?.let { runCatching { it.jsonPrimitive.contentOrNull }.getOrNull() },
        )
    }

    suspend fun postHbKeyAction(action: String, id: String? = null): Result<HbKeyStatus> {
        val payload = buildJsonObject {
            put("action", JsonPrimitive(action))
            if (!id.isNullOrBlank()) put("id", JsonPrimitive(id))
        }
        val res = executeRawJson("POST", "/api/me/hb-key", ForHappyJson.encodeToString(payload))
        val obj = runCatching { ForHappyJson.parseToJsonElement(res.body).jsonObject }.getOrNull()
        return if (res.code in 200..299 && obj != null) {
            Result.success(
                HbKeyStatus(
                    bound = true,
                    keyId = obj["id"]?.jsonPrimitive?.contentOrNull ?: id,
                    revealedKey = obj["key"]?.jsonPrimitive?.contentOrNull,
                ),
            )
        } else {
            Result.failure(IllegalStateException(obj?.get("error")?.jsonPrimitive?.contentOrNull ?: "密钥操作失败 (${res.code})"))
        }
    }

    suspend fun fetchMeItems(
        tab: String, // "fav" | "like" | "reply" | "comment" | "post" | "seen"
        page: Int = 1,
        query: String = "",
        sort: String = "new", // "new" | "old"
        folderId: Long? = null,
    ): MeItemsResponse {
        val qs = buildList {
            add("tab=${enc(tab)}")
            add("page=$page")
            if (query.isNotBlank()) add("q=${enc(query.trim())}")
            if (sort == "old") add("sort=old")
            if (folderId != null && folderId > 0L) add("folder=$folderId")
        }.joinToString("&")
        val res = executeRawJson("GET", "/api/me/items?$qs")
        if (res.code !in 200..299) return MeItemsResponse()
        return runCatching { ForHappyJson.decodeFromString<MeItemsResponse>(res.body) }.getOrDefault(MeItemsResponse())
    }

    suspend fun updateMeProfile(nick: String?, bio: String?, avatarKey: String? = null): Result<Unit> {
        val payload = buildJsonObject {
            if (nick != null) put("nick", JsonPrimitive(nick.trim()))
            if (bio != null) put("bio", JsonPrimitive(bio.trim()))
            if (!avatarKey.isNullOrBlank()) put("avatar", JsonPrimitive(avatarKey))
        }
        val res = executeRawJson("PATCH", "/api/me/profile", ForHappyJson.encodeToString(payload))
        val obj = runCatching { ForHappyJson.parseToJsonElement(res.body).jsonObject }.getOrNull()
        return if (res.code in 200..299) Result.success(Unit)
        else Result.failure(IllegalStateException(obj?.get("error")?.jsonPrimitive?.contentOrNull ?: "保存资料失败 (${res.code})"))
    }

    suspend fun updateMeDeco(decoJson: JsonElement?): Result<JsonElement?> {
        val payload = buildJsonObject {
            put("deco", decoJson ?: JsonNull)
        }
        val res = executeRawJson("PATCH", "/api/me/profile", ForHappyJson.encodeToString(payload))
        val obj = runCatching { ForHappyJson.parseToJsonElement(res.body).jsonObject }.getOrNull()
        return if (res.code in 200..299) Result.success(obj?.get("deco"))
        else Result.failure(IllegalStateException(obj?.get("error")?.jsonPrimitive?.contentOrNull ?: "更新装扮失败 (${res.code})"))
    }

    suspend fun fetchMyRaffleTitles(): JsonObject? {
        val res = executeRawJson("GET", "/api/me/raffle-titles")
        return if (res.code in 200..299) runCatching { ForHappyJson.parseToJsonElement(res.body).jsonObject }.getOrNull() else null
    }

    suspend fun fetchMyPersonalTitles(): JsonObject? {
        val res = executeRawJson("GET", "/api/me/personal-titles")
        return if (res.code in 200..299) runCatching { ForHappyJson.parseToJsonElement(res.body).jsonObject }.getOrNull() else null
    }

    suspend fun fetchMeAiPrefs(): JsonObject? {
        val res = executeRawJson("GET", "/api/me/ai-prefs")
        return if (res.code in 200..299) runCatching { ForHappyJson.parseToJsonElement(res.body).jsonObject }.getOrNull() else null
    }

    suspend fun patchMeAiPrefs(payload: JsonObject): Result<Unit> {
        val res = executeRawJson("PATCH", "/api/me/ai-prefs", ForHappyJson.encodeToString(payload))
        return if (res.code in 200..299) Result.success(Unit)
        else Result.failure(IllegalStateException("保存 AI 偏好失败 (${res.code})"))
    }

    suspend fun fetchOwner52bCards(): List<Owner52bCard> {
        val res = executeRawJson("GET", "/api/me/owner52b-cards")
        if (res.code !in 200..299) return emptyList()
        val obj = runCatching { ForHappyJson.parseToJsonElement(res.body).jsonObject }.getOrNull() ?: return emptyList()
        val arr = obj["cards"] ?: obj["items"] ?: return emptyList()
        return runCatching { ForHappyJson.decodeFromJsonElement(ListSerializer(Owner52bCard.serializer()), arr) }
            .getOrDefault(emptyList())
    }

    suspend fun fetchFolders(): List<FolderItem> {
        val res = executeRawJson("GET", "/api/folders")
        if (res.code !in 200..299) return emptyList()
        val obj = runCatching { ForHappyJson.parseToJsonElement(res.body).jsonObject }.getOrNull() ?: return emptyList()
        val arr = obj["folders"] ?: return emptyList()
        return runCatching { ForHappyJson.decodeFromJsonElement(ListSerializer(FolderItem.serializer()), arr) }
            .getOrDefault(emptyList())
    }

    suspend fun renameFolder(folderId: Long, newName: String): List<FolderItem>? {
        val payload = buildJsonObject { put("name", JsonPrimitive(newName.trim())) }
        val res = executeRawJson("PATCH", "/api/folders/$folderId", ForHappyJson.encodeToString(payload))
        if (res.code !in 200..299) return null
        val obj = runCatching { ForHappyJson.parseToJsonElement(res.body).jsonObject }.getOrNull() ?: return null
        val arr = obj["folders"] ?: return null
        return runCatching { ForHappyJson.decodeFromJsonElement(ListSerializer(FolderItem.serializer()), arr) }.getOrNull()
    }

    suspend fun deleteFolder(folderId: Long): Boolean {
        val res = executeRawJson("DELETE", "/api/folders/$folderId", "{}")
        return res.code in 200..299
    }

    suspend fun fetchMyInvites(): JsonObject? {
        val res = executeRawJson("GET", "/api/me/invites")
        return if (res.code in 200..299) {
            runCatching { ForHappyJson.parseToJsonElement(res.body).jsonObject }.getOrNull()
        } else {
            null
        }
    }

    suspend fun createMyInvite(): Result<JsonObject> {
        val res = executeRawJson("POST", "/api/me/invites", "{}")
        val obj = runCatching { ForHappyJson.parseToJsonElement(res.body).jsonObject }.getOrNull()
        return if (res.code in 200..299) {
            Result.success(obj ?: JsonObject(emptyMap()))
        } else {
            val err = obj?.get("error")?.jsonPrimitive?.contentOrNull
            Result.failure(IllegalStateException(err ?: "生成邀请码失败 (${res.code})"))
        }
    }

    suspend fun revokeMyInvite(codeOrId: String): Boolean {
        val res = executeRawJson("DELETE", "/api/me/invites/${enc(codeOrId)}", "{}")
        return res.code in 200..299
    }

    suspend fun fetchPuzzles(): List<MemeItem> {
        val res = executeRawJson("GET", "/api/puzzles")
        if (res.code !in 200..299) return emptyList()
        val obj = runCatching { ForHappyJson.parseToJsonElement(res.body).jsonObject }.getOrNull() ?: return emptyList()
        val arr = obj["puzzles"] ?: return emptyList()
        return runCatching { ForHappyJson.decodeFromJsonElement(ListSerializer(MemeItem.serializer()), arr) }
            .getOrDefault(emptyList())
    }

    suspend fun solvePuzzle(id: Long, answer: String): JsonObject {
        val payload = buildJsonObject { put("answer", JsonPrimitive(answer)) }
        val res = executeRawJson("POST", "/api/puzzles/$id/solve", ForHappyJson.encodeToString(payload))
        return runCatching { ForHappyJson.parseToJsonElement(res.body).jsonObject }.getOrDefault(JsonObject(emptyMap()))
    }

    suspend fun fetchRoadItems(tab: String = "all"): List<RoadItem> {
        val qs = if (tab != "all") "?kind=${enc(tab)}" else ""
        val res = executeRawJson("GET", "/api/road$qs")
        if (res.code !in 200..299) return emptyList()
        val obj = runCatching { ForHappyJson.parseToJsonElement(res.body).jsonObject }.getOrNull() ?: return emptyList()
        val arr = obj["items"] ?: return emptyList()
        return runCatching { ForHappyJson.decodeFromJsonElement(ListSerializer(RoadItem.serializer()), arr) }
            .getOrDefault(emptyList())
    }

    suspend fun createRoadItem(kind: String, title: String, body: String): Result<Unit> {
        val payload = buildJsonObject {
            put("kind", JsonPrimitive(kind))
            put("title", JsonPrimitive(title))
            put("body", JsonPrimitive(body))
        }
        val res = executeRawJson("POST", "/api/road", ForHappyJson.encodeToString(payload))
        return if (res.code in 200..299) Result.success(Unit)
        else Result.failure(IllegalStateException("提交失败 (${res.code})"))
    }

    suspend fun voteRoadItem(id: Long, vote: Int): JsonObject? {
        val res = executeRawJson("POST", "/api/road/$id/vote", """{"v":$vote}""")
        return if (res.code in 200..299) runCatching { ForHappyJson.parseToJsonElement(res.body).jsonObject }.getOrNull() else null
    }

    suspend fun fetchRoadComments(roadId: Long): List<RoadCommentItem> {
        val res = executeRawJson("GET", "/api/road/$roadId/comments")
        if (res.code !in 200..299) return emptyList()
        val obj = runCatching { ForHappyJson.parseToJsonElement(res.body).jsonObject }.getOrNull() ?: return emptyList()
        val arr = obj["comments"] ?: obj["items"] ?: return emptyList()
        return runCatching { ForHappyJson.decodeFromJsonElement(ListSerializer(RoadCommentItem.serializer()), arr) }
            .getOrDefault(emptyList())
    }

    suspend fun postRoadComment(roadId: Long, body: String): Result<Unit> {
        val payload = buildJsonObject { put("body", JsonPrimitive(body.trim())) }
        val res = executeRawJson("POST", "/api/road/$roadId/comments", ForHappyJson.encodeToString(payload))
        return if (res.code in 200..299) Result.success(Unit)
        else Result.failure(IllegalStateException("评论失败 (${res.code})"))
    }

    suspend fun fetchPetNeighbors(): List<PetNeighborSummary> {
        val res = executeRawJson("GET", "/api/pet/world/neighbors?limit=20")
        if (res.code !in 200..299) return emptyList()
        val obj = runCatching { ForHappyJson.parseToJsonElement(res.body).jsonObject }.getOrNull() ?: return emptyList()
        val arr = obj["neighbors"] ?: obj["items"] ?: return emptyList()
        return runCatching { ForHappyJson.decodeFromJsonElement(ListSerializer(PetNeighborSummary.serializer()), arr) }
            .getOrDefault(emptyList())
    }

    suspend fun fetchPetGuardBattles(): List<PetGuardBattle> {
        val res = executeRawJson("GET", "/api/pet/world/guard-log")
        if (res.code !in 200..299) return emptyList()
        val obj = runCatching { ForHappyJson.parseToJsonElement(res.body).jsonObject }.getOrNull() ?: return emptyList()
        val arr = obj["battles"] ?: obj["logs"] ?: return emptyList()
        return runCatching { ForHappyJson.decodeFromJsonElement(ListSerializer(PetGuardBattle.serializer()), arr) }
            .getOrDefault(emptyList())
    }

    suspend fun fetchCachedMediaFile(rawUrl: String): File? = withContext(Dispatchers.IO) {
        val abs = MediaUrlRewriter.rewriteMediaUrl(rawUrl)
        if (abs.isBlank()) return@withContext null
        val dir = File(context.cacheDir, "forhappy_media_cache").apply { mkdirs() }
        val cleanPath = abs.substringBefore('?').substringBefore('#').lowercase()
        val ext = when {
            cleanPath.endsWith(".mp4") -> ".mp4"
            cleanPath.endsWith(".webm") -> ".webm"
            cleanPath.endsWith(".mov") -> ".mov"
            cleanPath.endsWith(".gif") -> ".gif"
            cleanPath.endsWith(".webp") -> ".webp"
            cleanPath.endsWith(".png") -> ".png"
            cleanPath.endsWith(".jpg") || cleanPath.endsWith(".jpeg") -> ".jpg"
            else -> ".bin"
        }
        val key = sha256Hex(abs)
        val target = File(dir, "$key$ext")
        if (target.exists() && target.length() > 0L) {
            target.setLastModified(System.currentTimeMillis())
            return@withContext target
        }

        val candidates = buildList {
            add(abs)
            if (!abs.contains("/ext?u=")) {
                add(MediaUrlRewriter.externalFallbackUrl(abs))
            }
        }
        for (candidate in candidates) {
            val tmp = File(dir, "$key$ext.tmp")
            val downloaded = runCatching {
                val req = Request.Builder().url(candidate).get().build()
                okHttpClient.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@use false
                    val body = resp.body ?: return@use false
                    tmp.outputStream().use { out ->
                        body.byteStream().use { input -> input.copyTo(out) }
                    }
                    tmp.length() > 0L
                }
            }.getOrDefault(false)
            if (downloaded) {
                if (target.exists()) target.delete()
                if (tmp.renameTo(target) && target.length() > 0L) {
                    pruneMediaCache(dir, maxBytes = 160L * 1024L * 1024L)
                    return@withContext target
                }
            }
            if (tmp.exists()) tmp.delete()
        }
        null
    }

    private fun pruneMediaCache(dir: File, maxBytes: Long) {
        val files = dir.listFiles()?.filter { it.isFile && !it.name.endsWith(".tmp") } ?: return
        var total = files.sumOf { it.length() }
        if (total <= maxBytes) return
        for (f in files.sortedBy { it.lastModified() }) {
            val len = f.length()
            if (f.delete()) {
                total -= len
                if (total <= maxBytes * 3 / 4) break
            }
        }
    }

    private fun sha256Hex(text: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(text.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    private fun enc(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name())
}
