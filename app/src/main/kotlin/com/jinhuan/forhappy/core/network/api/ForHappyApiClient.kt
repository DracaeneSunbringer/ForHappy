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
import com.jinhuan.forhappy.core.db.ForHappyDatabase
import com.jinhuan.forhappy.core.dynamic.TopbarModuleScanner
import com.jinhuan.forhappy.core.model.AnchorTag
import com.jinhuan.forhappy.core.model.AuthConfigResponse
import com.jinhuan.forhappy.core.model.ChatGroup
import com.jinhuan.forhappy.core.model.ChatGroupsResponse
import com.jinhuan.forhappy.core.model.ChatMessage
import com.jinhuan.forhappy.core.model.ChatMessagesResponse
import com.jinhuan.forhappy.core.model.ChatRepliesResponse
import com.jinhuan.forhappy.core.model.ChatReplyItem
import com.jinhuan.forhappy.core.model.ChatSearchResponse
import com.jinhuan.forhappy.core.model.DaysResponse
import com.jinhuan.forhappy.core.model.DmMessage
import com.jinhuan.forhappy.core.model.DmThread
import com.jinhuan.forhappy.core.model.DynamicModule
import com.jinhuan.forhappy.core.model.FolderItem
import com.jinhuan.forhappy.core.model.ForHappyJson
import com.jinhuan.forhappy.core.model.GuessHistoryResponse
import com.jinhuan.forhappy.core.model.GuessRoomOverviewResponse
import com.jinhuan.forhappy.core.model.GuessRound
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
import com.jinhuan.forhappy.core.model.RoadItem
import com.jinhuan.forhappy.core.model.SeenVisitEntry
import com.jinhuan.forhappy.core.model.StickerEntry
import com.jinhuan.forhappy.core.model.StickerPack
import com.jinhuan.forhappy.core.model.TagStatItem
import com.jinhuan.forhappy.core.model.TagsResponse
import com.jinhuan.forhappy.core.model.TodayStats
import com.jinhuan.forhappy.core.model.UploadedMedia
import com.jinhuan.forhappy.core.model.WalletSummary
import com.jinhuan.forhappy.core.network.cookie.CookieSyncBridge
import com.jinhuan.forhappy.core.network.cookie.PersistentCookieJar
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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
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
        tag: String? = null,
        search: String? = null,
        sort: String = "new",
        unseenOnly: Boolean = false,
        textFilter: String = "",
        nsfwHide: Boolean = false,
        author: String? = null,
    ): DaysResponse {
        val q = buildList {
            if (limitDays != null && limitDays > 0) add("days=$limitDays")
            add("sort=${enc(sort)}")
            if (nsfwHide) add("nsfw=0")
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
        author: String? = null,
    ): MemesPageResponse {
        val q = buildList {
            add("limit=$limit")
            if (focus != null && focus > 0L) {
                add("focus=$focus")
            } else {
                add("offset=$offset")
            }
            add("sort=${enc(sort)}")
            if (sort == "hot") add("page=${(offset / maxOf(1, limit)) + 1}")
            if (nsfwHide) add("nsfw=0")
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
        author = author,
    ).items

    suspend fun fetchTags(hideNsfw: Boolean = false): List<TagStatItem> {
        val qs = if (hideNsfw) "?nsfw=0" else ""
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

    suspend fun postMemeComment(memeId: Long, text: String, replyToId: Long? = null): Boolean {
        val payload = buildJsonObject {
            put("body", JsonPrimitive(text))
            if (replyToId != null && replyToId > 0L) put("reply_to", JsonPrimitive(replyToId))
        }
        val res = executeRawJson("POST", "/api/memes/$memeId/comments", ForHappyJson.encodeToString(payload))
        return res.code in 200..299
    }

    suspend fun createMeme(
        title: String,
        content: String,
        tags: List<String>,
        spoiler: Boolean = false,
        uploadedKey: String? = null,
    ): Result<Long> {
        val payload = if (!uploadedKey.isNullOrBlank()) {
            buildJsonObject {
                put("title", JsonPrimitive(title))
                put("content", JsonPrimitive(content))
                put("tags", JsonArray(tags.map { JsonPrimitive(it) }))
                put("spoiler", JsonPrimitive(spoiler))
                put("layout", JsonPrimitive("carousel"))
                put("items", JsonArray(listOf(buildJsonObject { put("key", JsonPrimitive(uploadedKey)) })))
            }
        } else {
            buildJsonObject {
                put("kind", JsonPrimitive("text"))
                put("title", JsonPrimitive(title))
                put("content", JsonPrimitive(content))
                put("tags", JsonArray(tags.map { JsonPrimitive(it) }))
                put("spoiler", JsonPrimitive(spoiler))
                put("layout", JsonPrimitive("grid"))
                put("items", JsonArray(emptyList()))
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

    suspend fun uploadImageBytes(bytes: ByteArray, fileName: String, mimeType: String): Result<UploadedMedia> = withContext(Dispatchers.IO) {
        val nonce = UUID.randomUUID().toString()
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
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
        return runCatching { ForHappyJson.decodeFromJsonElement(kotlinx.serialization.builtins.ListSerializer(AnchorTag.serializer()), arr) }
            .getOrDefault(emptyList())
    }

    suspend fun fetchChatGroups(): List<ChatGroup> {
        val res = executeRawJson("GET", "/api/chat/groups")
        if (res.code !in 200..299) return emptyList()
        return runCatching { ForHappyJson.decodeFromString<ChatGroupsResponse>(res.body).groups }.getOrDefault(emptyList())
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

    suspend fun claimLdcRedpack(msgId: Long): JsonObject {
        val res = executeRawJson("POST", "/api/chat/messages/$msgId/ldc/claim", "{}")
        return runCatching { ForHappyJson.parseToJsonElement(res.body).jsonObject }.getOrDefault(JsonObject(emptyMap()))
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
        return runCatching { ForHappyJson.decodeFromJsonElement(kotlinx.serialization.builtins.ListSerializer(DmThread.serializer()), arr) }
            .getOrDefault(emptyList())
    }

    suspend fun fetchDmMessages(threadId: Long): List<DmMessage> {
        val res = executeRawJson("GET", "/api/dm/threads/$threadId/messages")
        if (res.code !in 200..299) return emptyList()
        val obj = runCatching { ForHappyJson.parseToJsonElement(res.body).jsonObject }.getOrNull() ?: return emptyList()
        val arr = obj["items"] ?: obj["messages"] ?: return emptyList()
        return runCatching { ForHappyJson.decodeFromJsonElement(kotlinx.serialization.builtins.ListSerializer(DmMessage.serializer()), arr) }
            .getOrDefault(emptyList())
    }

    suspend fun sendDmMessage(threadId: Long, body: String): Boolean {
        val payload = buildJsonObject { put("body", JsonPrimitive(body)) }
        val res = executeRawJson("POST", "/api/dm/threads/$threadId/messages", ForHappyJson.encodeToString(payload))
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

    suspend fun fetchWallet(): WalletSummary? {
        val res = executeRawJson("GET", "/api/me/wallet?ledger=1")
        if (res.code !in 200..299) return null
        return runCatching { ForHappyJson.decodeFromString<WalletSummary>(res.body) }.getOrNull()
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

    private fun enc(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name())
}
