package com.jinhuan.forhappy.core.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.jinhuan.forhappy.BuildConfig
import com.jinhuan.forhappy.core.db.ForHappyDatabase
import com.jinhuan.forhappy.core.model.ForHappyJson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.random.Random

enum class UpdateStatus {
    IDLE,
    CHECKING,
    UP_TO_DATE,
    AVAILABLE,
    DOWNLOADING,
    READY_TO_INSTALL,
    ERROR,
}

@Serializable
data class GitHubMirrorNode(
    val prefix: String,
    val region: String = "",
    val group: String = "cf_pool",
)

@Serializable
data class MirrorRegistrySnapshot(
    val active: List<GitHubMirrorNode> = emptyList(),
    val disabledPrefixes: Set<String> = emptySet(),
)

data class ReleaseUpdateInfo(
    val tagName: String,
    val versionName: String,
    val releaseTitle: String,
    val releaseNotes: String,
    val apkAssetName: String,
    val apkDownloadUrl: String,
    val apkSizeBytes: Long,
    val htmlUrl: String,
)

data class AppUpdateState(
    val currentVersion: String = BuildConfig.VERSION_NAME,
    val status: UpdateStatus = UpdateStatus.IDLE,
    val latestRelease: ReleaseUpdateInfo? = null,
    val downloadProgressPercent: Int = 0,
    val downloadedFilePath: String? = null,
    val customMirrorPrefix: String = "",
    val activeMirrorCount: Int = AppUpdateManager.DEFAULT_DOWNLOAD_MIRRORS.size,
    val statusMessage: String = "当前版本 v${BuildConfig.VERSION_NAME} · 支持从 GitHub Release 检查更新",
)

/**
 * GitHub Release auto-updater featuring XIU2 active mirror pool sync, stratified CDN
 * shuffling, 4-way concurrent first-byte header racing (with PK magic & Content-Length validation),
 * slow-speed watchdog failover with HTTP Range resume, mirror-accelerated release discovery,
 * and optional user-defined custom acceleration prefix.
 */
object AppUpdateManager {
    const val GITHUB_OWNER = "DracaeneSunbringer"
    const val GITHUB_REPO = "ForHappy"
    const val RELEASES_HTML_URL = "https://github.com/$GITHUB_OWNER/$GITHUB_REPO/releases"
    private const val LATEST_RELEASE_API = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"
    private const val LATEST_RELEASE_WEB = "https://github.com/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"

    private const val KV_CUSTOM_MIRROR_PREFIX = "update.custom_mirror_prefix"
    private const val KV_XIU2_MIRRORS_JSON = "update.xiu2_mirrors_json"
    private const val KV_XIU2_MIRRORS_AT = "update.xiu2_mirrors_at"
    private const val MIRROR_CACHE_TTL_MS = 24L * 60L * 60L * 1000L

    private const val PROBE_CONNECT_TIMEOUT_MS = 4_000
    private const val PROBE_READ_TIMEOUT_MS = 8_000
    private const val DEFAULT_MIN_SPEED_BYTES_PER_SEC = 40L * 1024L
    private const val DEFAULT_SPEED_WINDOW_MS = 5_000L
    private const val SPEED_GRACE_MS = 3_000L

    val DEFAULT_DOWNLOAD_MIRRORS: List<GitHubMirrorNode> = listOf(
        // Heterogeneous APAC / multi-CDN nodes
        GitHubMirrorNode("https://edgeone.gh-proxy.org/https://github.com", "EdgeOne", "apac_multi"),
        GitHubMirrorNode("https://cdn.gh-proxy.org/https://github.com", "Fastly", "apac_multi"),
        GitHubMirrorNode("https://hk.gh-proxy.org/https://github.com", "香港", "apac_multi"),
        GitHubMirrorNode("https://ghfast.top/https://github.com", "多线", "apac_multi"),
        GitHubMirrorNode("https://wget.la/https://github.com", "多线", "apac_multi"),
        // Independent US / EU edge proxies
        GitHubMirrorNode("https://ghproxy.net/https://github.com", "法国", "independent"),
        GitHubMirrorNode("https://ghproxy.it/https://github.com", "洛杉矶", "independent"),
        GitHubMirrorNode("https://gh.zwy.one/https://github.com", "洛杉矶", "independent"),
        GitHubMirrorNode("https://ghproxy.monkeyray.net/https://github.com", "洛杉矶", "independent"),
        // Cloudflare Workers / CDN pool
        GitHubMirrorNode("https://gh-proxy.org/https://github.com", "美国 CF", "cf_pool"),
        GitHubMirrorNode("https://gh.h233.eu.org/https://github.com", "美国 CF", "cf_pool"),
        GitHubMirrorNode("https://gh.ddlc.top/https://github.com", "美国 CF", "cf_pool"),
        GitHubMirrorNode("https://cors.isteed.cc/github.com", "美国 CF", "cf_pool"),
        GitHubMirrorNode("https://github.boki.moe/https://github.com", "美国 CF", "cf_pool"),
        GitHubMirrorNode("https://gh.jasonzeng.dev/https://github.com", "美国 CF", "cf_pool"),
        GitHubMirrorNode("https://gh.monlor.com/https://github.com", "美国 CF", "cf_pool"),
        GitHubMirrorNode("https://github.geekery.cn/https://github.com", "美国 CF", "cf_pool"),
        GitHubMirrorNode("https://github.ednovas.xyz/https://github.com", "美国 CF", "cf_pool"),
        GitHubMirrorNode("https://ghfile.geekertao.top/https://github.com", "美国 CF", "cf_pool"),
        GitHubMirrorNode("https://ghp.keleyaa.com/https://github.com", "美国 CF", "cf_pool"),
        GitHubMirrorNode("https://gh.chjina.com/https://github.com", "美国 CF", "cf_pool"),
        GitHubMirrorNode("https://ghpxy.hwinzniej.top/https://github.com", "美国 CF", "cf_pool"),
        GitHubMirrorNode("https://cdn.crashmc.com/https://github.com", "美国 CF", "cf_pool"),
        GitHubMirrorNode("https://git.yylx.win/https://github.com", "美国 CF", "cf_pool"),
        GitHubMirrorNode("https://gitproxy.mrhjx.cn/https://github.com", "美国 CF", "cf_pool"),
        GitHubMirrorNode("https://ghproxy.cxkpro.top/https://github.com", "美国 CF", "cf_pool"),
        GitHubMirrorNode("https://gh.idayer.com/https://github.com", "美国 CF", "cf_pool"),
        GitHubMirrorNode("https://down.npee.cn/?https://github.com", "美国 CF", "cf_pool"),
        GitHubMirrorNode("https://raw.ihtw.moe/github.com", "美国 CF", "cf_pool"),
        GitHubMirrorNode("https://xget.xi-xu.me/gh", "美国 CF", "cf_pool"),
    )

    private val XIU2_SCRIPT_URLS = listOf(
        "https://ghfast.top/https://raw.githubusercontent.com/XIU2/UserScript/master/GithubEnhanced-High-Speed-Download.user.js",
        "https://hk.gh-proxy.org/https://raw.githubusercontent.com/XIU2/UserScript/master/GithubEnhanced-High-Speed-Download.user.js",
        "https://wget.la/https://raw.githubusercontent.com/XIU2/UserScript/master/GithubEnhanced-High-Speed-Download.user.js",
        "https://fastly.jsdelivr.net/gh/XIU2/UserScript@master/GithubEnhanced-High-Speed-Download.user.js",
        "https://cdn.gh-proxy.org/https://raw.githubusercontent.com/XIU2/UserScript/master/GithubEnhanced-High-Speed-Download.user.js",
        "https://g.blfrp.cn/https://raw.githubusercontent.com/XIU2/UserScript/master/GithubEnhanced-High-Speed-Download.user.js",
        "https://raw.githubusercontent.com/XIU2/UserScript/master/GithubEnhanced-High-Speed-Download.user.js",
    )

    private val XIU2_BLOCK_REGEX = Regex("""const\s+download_url_us\s*=\s*\[([\s\S]*?)\]\s*,\s*clone_url""")
    private val XIU2_ACTIVE_ROW_REGEX = Regex("""^\['(https://[^']+)',\s*'([^']*)',\s*'([^']*)'""")
    private val XIU2_COMMENTED_ROW_REGEX = Regex("""^//\s*\['(https://[^']+)'""")

    private val jsonParser = Json { ignoreUnknownKeys = true }

    private data class WinningProbe(
        val conn: HttpURLConnection,
        val stream: InputStream,
        val initialBytes: ByteArray,
        val initialLength: Int,
        val startOffset: Long,
        val totalBytes: Long,
        val displayHost: String,
    )

    suspend fun fetchLatestRelease(
        db: ForHappyDatabase? = null,
        customPrefix: String = "",
    ): Result<ReleaseUpdateInfo?> = withContext(Dispatchers.IO) {
        val resolvedCustom = customPrefix.trim().ifBlank {
            runCatching { db?.getKv(KV_CUSTOM_MIRROR_PREFIX)?.trim().orEmpty() }.getOrDefault("")
        }
        try {
            val (apiNotFound, apiResult) = fetchViaGitHubApi()
            if (apiNotFound) {
                return@withContext Result.success(null)
            }
            if (apiResult != null) {
                return@withContext Result.success(apiResult)
            }
            Result.failure(IOException("未能获取 Release 信息"))
        } catch (e: CancellationException) {
            throw e
        } catch (primaryErr: Throwable) {
            try {
                val mirrors = resolveMirrorPool(db = db, forceRefresh = false)
                val probeUrls = buildOrderedCandidateUrls(
                    rawUrl = LATEST_RELEASE_WEB,
                    mirrors = mirrors,
                    customPrefix = resolvedCustom,
                )
                val fallback = raceMirrorReleaseRedirect(probeUrls)
                if (fallback != null) {
                    Result.success(fallback)
                } else {
                    Result.failure(primaryErr)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                Result.failure(primaryErr)
            }
        }
    }

    private fun fetchViaGitHubApi(): Pair<Boolean, ReleaseUpdateInfo?> {
        val conn = (URL(LATEST_RELEASE_API).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 4_000
            readTimeout = 4_000
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "ForHappy/${BuildConfig.VERSION_NAME}")
        }
        try {
            val code = conn.responseCode
            if (code == HttpURLConnection.HTTP_NOT_FOUND) {
                return true to null
            }
            if (code !in 200..299) {
                throw IOException("GitHub Release 查询失败 (HTTP $code)")
            }
            val body = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            return false to parseReleaseJson(body)
        } finally {
            conn.disconnect()
        }
    }

    private suspend fun raceMirrorReleaseRedirect(candidateUrls: List<String>): ReleaseUpdateInfo? {
        if (candidateUrls.isEmpty()) return null
        val waves = candidateUrls.chunked(4).take(3)
        for (wave in waves) {
            val won = raceFirstSuccess(wave) { probeUrl, connRef ->
                probeReleaseTagViaRedirects(probeUrl, connRef)
            }
            if (won != null) return won
        }
        return null
    }

    private fun probeReleaseTagViaRedirects(
        startUrl: String,
        connRef: AtomicReference<HttpURLConnection?>,
    ): ReleaseUpdateInfo {
        var currentUrl = startUrl
        for (hop in 0 until 5) {
            extractTagFromUrl(currentUrl)?.let { tag ->
                return buildFallbackReleaseInfo(tag)
            }
            val conn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4_000
                readTimeout = 4_000
                instanceFollowRedirects = false
                setRequestProperty("User-Agent", "ForHappy/${BuildConfig.VERSION_NAME}")
            }
            connRef.set(conn)
            try {
                val code = conn.responseCode
                if (code == HttpURLConnection.HTTP_NOT_FOUND) {
                    throw IOException("Release 不存在 (HTTP 404)")
                }
                val location = conn.getHeaderField("Location").orEmpty()
                if (location.isNotBlank()) {
                    extractTagFromUrl(location)?.let { tag ->
                        return buildFallbackReleaseInfo(tag)
                    }
                    if (code in 300..399) {
                        val nextUrl = URL(URL(currentUrl), location).toString()
                        connRef.compareAndSet(conn, null)
                        conn.disconnect()
                        currentUrl = nextUrl
                        continue
                    }
                }
                if (code in 200..299) {
                    val body = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    val tagMatch = Regex("""/releases/tag/([A-Za-z0-9._-]+)""").find(body)
                    if (tagMatch != null) {
                        return buildFallbackReleaseInfo(tagMatch.groupValues[1])
                    }
                }
                throw IOException("未能从镜像重定向解析版本标签 (HTTP $code)")
            } finally {
                connRef.compareAndSet(conn, null)
                runCatching { conn.disconnect() }
            }
        }
        throw IOException("镜像检查更新重定向次数过多")
    }

    private fun extractTagFromUrl(rawUrl: String): String? {
        if (!rawUrl.contains("/releases/tag/")) return null
        val tag = rawUrl
            .substringAfter("/releases/tag/", missingDelimiterValue = "")
            .substringBefore('?')
            .substringBefore('#')
            .substringBefore('/')
            .trim()
        return tag.takeIf { it.isNotBlank() }
    }

    private fun buildFallbackReleaseInfo(tag: String): ReleaseUpdateInfo {
        val assetName = "ForHappy-$tag.apk"
        return ReleaseUpdateInfo(
            tagName = tag,
            versionName = normalizeVersionString(tag),
            releaseTitle = "ForHappy $tag",
            releaseNotes = "",
            apkAssetName = assetName,
            apkDownloadUrl = "https://github.com/$GITHUB_OWNER/$GITHUB_REPO/releases/download/$tag/$assetName",
            apkSizeBytes = 0L,
            htmlUrl = "https://github.com/$GITHUB_OWNER/$GITHUB_REPO/releases/tag/$tag",
        )
    }

    internal fun parseReleaseJson(jsonText: String): ReleaseUpdateInfo {
        val root = jsonParser.parseToJsonElement(jsonText).jsonObject
        val tagName = root["tag_name"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
        require(tagName.isNotBlank()) { "Release 缺少 tag_name" }

        val normalizedVersion = normalizeVersionString(tagName)
        val releaseTitle = root["name"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty().ifBlank { tagName }
        val releaseNotes = root["body"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
        val htmlUrl = root["html_url"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty().ifBlank { RELEASES_HTML_URL }

        var apkName = ""
        var apkUrl = ""
        var apkSize = 0L
        val assets = root["assets"]?.jsonArray
        if (assets != null) {
            for (el in assets) {
                val asset = runCatching { el.jsonObject }.getOrNull() ?: continue
                val name = asset["name"]?.jsonPrimitive?.contentOrNull.orEmpty()
                val contentType = asset["content_type"]?.jsonPrimitive?.contentOrNull.orEmpty()
                val downloadUrl = asset["browser_download_url"]?.jsonPrimitive?.contentOrNull.orEmpty()
                if (downloadUrl.isNotBlank() &&
                    (name.endsWith(".apk", ignoreCase = true) || contentType == "application/vnd.android.package-archive")
                ) {
                    apkName = name
                    apkUrl = downloadUrl
                    apkSize = asset["size"]?.jsonPrimitive?.longOrNull ?: 0L
                    break
                }
            }
        }

        return ReleaseUpdateInfo(
            tagName = tagName,
            versionName = normalizedVersion,
            releaseTitle = releaseTitle,
            releaseNotes = releaseNotes,
            apkAssetName = apkName,
            apkDownloadUrl = apkUrl,
            apkSizeBytes = apkSize,
            htmlUrl = htmlUrl,
        )
    }

    internal fun normalizeVersionString(raw: String): String {
        return raw.trim().removePrefix("v").removePrefix("V").trim()
    }

    internal fun isNewerVersion(remoteVersion: String, currentVersion: String): Boolean {
        val remoteClean = normalizeVersionString(remoteVersion).substringBefore('-').substringBefore('+')
        val currentClean = normalizeVersionString(currentVersion).substringBefore('-').substringBefore('+')
        if (remoteClean.isBlank() || currentClean.isBlank()) return false
        val remoteParts = remoteClean.split('.').map { it.toIntOrNull() ?: 0 }
        val currentParts = currentClean.split('.').map { it.toIntOrNull() ?: 0 }
        val maxLen = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r != c) return r > c
        }
        return false
    }

    internal fun applyMirrorPrefix(prefix: String, rawGitHubUrl: String): String {
        val trimmedRaw = rawGitHubUrl.trim()
        if (trimmedRaw.isBlank()) return ""
        val cleanPrefix = prefix.trim().trimEnd('/')
        if (cleanPrefix.isBlank()) return trimmedRaw
        if (!trimmedRaw.startsWith("https://github.com/", ignoreCase = true) &&
            !trimmedRaw.startsWith("http://github.com/", ignoreCase = true)
        ) {
            return trimmedRaw
        }
        val pathAndQuery = trimmedRaw.substringAfter("github.com", "")
        if (cleanPrefix.equals("https://github.com", ignoreCase = true) ||
            cleanPrefix.equals("http://github.com", ignoreCase = true)
        ) {
            return trimmedRaw
        }
        return if (cleanPrefix.endsWith("/https://github.com", ignoreCase = true) ||
            cleanPrefix.endsWith("/http://github.com", ignoreCase = true) ||
            cleanPrefix.endsWith("/github.com", ignoreCase = true) ||
            cleanPrefix.endsWith("/?https://github.com", ignoreCase = true) ||
            cleanPrefix.endsWith("/gh", ignoreCase = true)
        ) {
            "$cleanPrefix$pathAndQuery"
        } else {
            "$cleanPrefix/https://github.com$pathAndQuery"
        }
    }

    internal fun parseXiu2UserscriptMirrors(scriptText: String): MirrorRegistrySnapshot {
        val block = XIU2_BLOCK_REGEX.find(scriptText)?.groupValues?.get(1) ?: scriptText
        val active = mutableListOf<GitHubMirrorNode>()
        val disabled = linkedSetOf<String>()
        val seenActive = HashSet<String>()

        for (rawLine in block.lineSequence()) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue
            val commented = XIU2_COMMENTED_ROW_REGEX.find(line)
            if (commented != null) {
                val p = commented.groupValues[1].trim().trimEnd('/')
                if (p.isNotBlank()) disabled += p
                continue
            }
            if (line.startsWith("//")) continue
            val m = XIU2_ACTIVE_ROW_REGEX.find(line) ?: continue
            val prefix = m.groupValues[1].trim().trimEnd('/')
            val region = m.groupValues[2].trim()
            val desc = m.groupValues[3]
            if (prefix.isNotBlank() && seenActive.add(prefix)) {
                val group = classifyMirrorGroup(prefix, region, desc)
                active += GitHubMirrorNode(prefix = prefix, region = region, group = group)
            }
        }
        return MirrorRegistrySnapshot(active = active, disabledPrefixes = disabled)
    }

    private fun classifyMirrorGroup(prefix: String, region: String, desc: String): String {
        val lower = prefix.lowercase()
        return when {
            lower.contains("edgeone.gh-proxy") ||
                lower.contains("cdn.gh-proxy") ||
                lower.contains("hk.gh-proxy") ||
                lower.contains("ghfast.top") ||
                lower.contains("wget.la") ||
                region.contains("香港") ||
                region.contains("日本") ||
                region.contains("韩国") ||
                desc.contains("Fastly") ||
                desc.contains("edgeone", ignoreCase = true) -> "apac_multi"

            desc.contains("洛杉矶") ||
                desc.contains("法国") ||
                !desc.contains("Cloudflare", ignoreCase = true) -> "independent"

            else -> "cf_pool"
        }
    }

    internal fun mergeMirrorPool(snapshot: MirrorRegistrySnapshot): List<GitHubMirrorNode> {
        val disabled = snapshot.disabledPrefixes.map { it.trim().trimEnd('/') }.toHashSet()
        val merged = mutableListOf<GitHubMirrorNode>()
        val seen = HashSet<String>()
        for (node in snapshot.active + DEFAULT_DOWNLOAD_MIRRORS) {
            val p = node.prefix.trim().trimEnd('/')
            if (p.isNotBlank() && !disabled.contains(p) && seen.add(p)) {
                merged += node.copy(prefix = p)
            }
        }
        return if (merged.isNotEmpty()) merged else DEFAULT_DOWNLOAD_MIRRORS
    }

    suspend fun resolveMirrorPool(
        db: ForHappyDatabase? = null,
        forceRefresh: Boolean = false,
    ): List<GitHubMirrorNode> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val cachedSnapshot = if (db != null) {
            val rawJson = runCatching { db.getKv(KV_XIU2_MIRRORS_JSON) }.getOrNull()
            if (!rawJson.isNullOrBlank()) {
                runCatching { ForHappyJson.decodeFromString<MirrorRegistrySnapshot>(rawJson) }.getOrNull()
            } else {
                null
            }
        } else {
            null
        }
        val cachedAt = if (db != null) {
            runCatching { db.getKv(KV_XIU2_MIRRORS_AT)?.toLongOrNull() ?: 0L }.getOrDefault(0L)
        } else {
            0L
        }

        if (!forceRefresh && cachedSnapshot != null && cachedSnapshot.active.isNotEmpty() &&
            (now - cachedAt) in 0L until MIRROR_CACHE_TTL_MS
        ) {
            return@withContext mergeMirrorPool(cachedSnapshot)
        }

        val fetchedSnapshot = fetchRemoteXiu2Snapshot()
        if (fetchedSnapshot != null && fetchedSnapshot.active.isNotEmpty()) {
            if (db != null) {
                runCatching {
                    db.putKv(KV_XIU2_MIRRORS_JSON, ForHappyJson.encodeToString(fetchedSnapshot))
                    db.putKv(KV_XIU2_MIRRORS_AT, now.toString())
                }
            }
            return@withContext mergeMirrorPool(fetchedSnapshot)
        }

        if (cachedSnapshot != null && cachedSnapshot.active.isNotEmpty()) {
            return@withContext mergeMirrorPool(cachedSnapshot)
        }
        DEFAULT_DOWNLOAD_MIRRORS
    }

    private suspend fun fetchRemoteXiu2Snapshot(): MirrorRegistrySnapshot? {
        for (wave in XIU2_SCRIPT_URLS.chunked(4)) {
            val snapshot = raceFirstSuccess(wave) { scriptUrl, connRef ->
                val conn = (URL(scriptUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 4_000
                    readTimeout = 6_000
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "ForHappy/${BuildConfig.VERSION_NAME}")
                }
                connRef.set(conn)
                try {
                    val code = conn.responseCode
                    if (code !in 200..299) throw IOException("HTTP $code")
                    val text = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    val parsed = parseXiu2UserscriptMirrors(text)
                    if (parsed.active.isEmpty()) throw IOException("未解析到有效镜像列表")
                    parsed
                } finally {
                    connRef.compareAndSet(conn, null)
                    runCatching { conn.disconnect() }
                }
            }
            if (snapshot != null) return snapshot
        }
        return null
    }

    internal fun buildOrderedCandidateUrls(
        rawUrl: String,
        mirrors: List<GitHubMirrorNode> = DEFAULT_DOWNLOAD_MIRRORS,
        customPrefix: String = "",
        random: Random = Random.Default,
    ): List<String> {
        val clean = rawUrl.trim()
        if (clean.isBlank()) return emptyList()
        if (!clean.startsWith("https://github.com/", ignoreCase = true) &&
            !clean.startsWith("http://github.com/", ignoreCase = true)
        ) {
            return listOf(clean)
        }

        val apac = mirrors.filter { it.group == "apac_multi" }.shuffled(random)
        val ind = mirrors.filter { it.group == "independent" }.shuffled(random)
        val cf = mirrors.filter { it.group != "apac_multi" && it.group != "independent" }.shuffled(random)

        val orderedMirrors = ArrayList<GitHubMirrorNode>(mirrors.size)
        var iA = 0
        var iI = 0
        var iC = 0
        while (iA < apac.size || iI < ind.size || iC < cf.size) {
            repeat(2) { if (iA < apac.size) orderedMirrors += apac[iA++] }
            if (iI < ind.size) orderedMirrors += ind[iI++]
            if (iC < cf.size) orderedMirrors += cf[iC++]
        }

        val out = LinkedHashSet<String>(orderedMirrors.size + 2)
        val trimmedCustom = customPrefix.trim()
        if (trimmedCustom.isNotBlank()) {
            val rewrittenCustom = applyMirrorPrefix(trimmedCustom, clean)
            if (rewrittenCustom.isNotBlank()) out += rewrittenCustom
        }
        for (node in orderedMirrors) {
            val url = applyMirrorPrefix(node.prefix, clean)
            if (url.isNotBlank()) out += url
        }
        out += clean
        return out.toList()
    }

    internal fun buildCandidateDownloadUrls(rawUrl: String): List<String> {
        return buildOrderedCandidateUrls(rawUrl)
    }

    internal fun safeAssetFileName(release: ReleaseUpdateInfo): String {
        return release.apkAssetName
            .filter { it.isLetterOrDigit() || it == '.' || it == '-' || it == '_' }
            .ifBlank { "ForHappy-${release.versionName}.apk" }
    }

    internal fun isValidDownloadedApk(file: File, expectedSizeBytes: Long): Boolean {
        if (!file.isFile || file.length() < 1024L) return false
        if (expectedSizeBytes > 0L && file.length() != expectedSizeBytes) return false
        val header = ByteArray(4)
        val readCount = file.inputStream().use { it.read(header) }
        return readCount >= 4 &&
            header[0] == 0x50.toByte() &&
            header[1] == 0x4B.toByte() &&
            header[2] == 0x03.toByte() &&
            header[3] == 0x04.toByte()
    }

    suspend fun downloadReleaseApk(
        context: Context,
        release: ReleaseUpdateInfo,
        db: ForHappyDatabase? = null,
        customPrefix: String = "",
        onProgress: (percent: Int, host: String, bytesPerSec: Long) -> Unit,
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val mirrors = resolveMirrorPool(db = db, forceRefresh = false)
            val resolvedCustom = customPrefix.trim().ifBlank {
                runCatching { db?.getKv(KV_CUSTOM_MIRROR_PREFIX)?.trim().orEmpty() }.getOrDefault("")
            }
            val candidateUrls = buildOrderedCandidateUrls(
                rawUrl = release.apkDownloadUrl,
                mirrors = mirrors,
                customPrefix = resolvedCustom,
            )
            require(candidateUrls.isNotEmpty()) { "该 Release 未附带可下载的 APK 安装包" }
            val updatesDir = File(context.cacheDir, "updates")
            if (!updatesDir.isDirectory && !updatesDir.mkdirs()) {
                throw IOException("无法创建更新缓存目录")
            }
            val safeFileName = safeAssetFileName(release)
            val targetFile = File(updatesDir, safeFileName)
            val tempFile = File(updatesDir, "$safeFileName.part")

            downloadFromCandidates(
                candidateUrls = candidateUrls,
                expectedSizeBytes = release.apkSizeBytes,
                tempFile = tempFile,
                onProgress = onProgress,
            )

            if (targetFile.exists()) {
                targetFile.delete()
            }
            if (!tempFile.renameTo(targetFile)) {
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }
            onProgress(100, "", 0L)
            Result.success(targetFile)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    internal suspend fun downloadFromCandidates(
        candidateUrls: List<String>,
        expectedSizeBytes: Long,
        tempFile: File,
        minSpeedBytesPerSec: Long = DEFAULT_MIN_SPEED_BYTES_PER_SEC,
        speedWindowMs: Long = DEFAULT_SPEED_WINDOW_MS,
        speedGraceMs: Long = SPEED_GRACE_MS,
        onProgress: (percent: Int, host: String, bytesPerSec: Long) -> Unit,
    ) = withContext(Dispatchers.IO) {
        require(candidateUrls.isNotEmpty()) { "下载候选地址为空" }
        if (tempFile.exists()) {
            tempFile.delete()
        }

        var lastError: Throwable? = null
        for (wave in candidateUrls.chunked(4)) {
            val resumeOffset = if (tempFile.isFile && tempFile.length() > 0L) tempFile.length() else 0L
            val winning = raceFirstSuccess(wave) { candidateUrl, connRef ->
                probeDownloadCandidate(
                    initialUrl = candidateUrl,
                    expectedSizeBytes = expectedSizeBytes,
                    resumeOffset = resumeOffset,
                    connRef = connRef,
                )
            }
            if (winning == null) {
                lastError = IOException("本轮并发探测均超时或校验失败")
                continue
            }

            try {
                streamWinningProbeToFile(
                    probe = winning,
                    tempFile = tempFile,
                    expectedSizeBytes = expectedSizeBytes,
                    minSpeedBytesPerSec = minSpeedBytesPerSec,
                    speedWindowMs = speedWindowMs,
                    speedGraceMs = speedGraceMs,
                    onProgress = onProgress,
                )
                return@withContext
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                lastError = e
            }
        }
        throw (lastError ?: IOException("所有镜像通道均不可用"))
    }

    private suspend fun <T : Any> raceFirstSuccess(
        urls: List<String>,
        probeFn: (url: String, connRef: AtomicReference<HttpURLConnection?>) -> T,
    ): T? {
        if (urls.isEmpty()) return null
        val winnerLatch = AtomicBoolean(false)
        val deferred = CompletableDeferred<T?>()
        val remaining = AtomicInteger(urls.size)
        val openRefs = CopyOnWriteArrayList<AtomicReference<HttpURLConnection?>>()
        val parentCtx = currentCoroutineContext()
        val probeScope = CoroutineScope(parentCtx + SupervisorJob() + Dispatchers.IO)

        for (url in urls) {
            val connRef = AtomicReference<HttpURLConnection?>(null)
            openRefs += connRef
            probeScope.launch {
                try {
                    if (!isActive || winnerLatch.get()) return@launch
                    val res = probeFn(url, connRef)
                    if (winnerLatch.compareAndSet(false, true)) {
                        connRef.set(null)
                        deferred.complete(res)
                    } else if (res is WinningProbe) {
                        runCatching { res.stream.close() }
                        runCatching { res.conn.disconnect() }
                    }
                } catch (_: Throwable) {
                    // Losing or failed probe is ignored unless all fail.
                } finally {
                    val left = remaining.decrementAndGet()
                    if (!winnerLatch.get()) {
                        connRef.getAndSet(null)?.let { c -> runCatching { c.disconnect() } }
                        if (left == 0 && !deferred.isCompleted) {
                            deferred.complete(null)
                        }
                    }
                }
            }
        }

        val result = try {
            deferred.await()
        } finally {
            probeScope.cancel()
            CoroutineScope(Dispatchers.IO).launch {
                for (ref in openRefs) {
                    ref.getAndSet(null)?.let { c -> runCatching { c.disconnect() } }
                }
            }
        }
        return result
    }

    private fun probeDownloadCandidate(
        initialUrl: String,
        expectedSizeBytes: Long,
        resumeOffset: Long,
        connRef: AtomicReference<HttpURLConnection?>,
    ): WinningProbe {
        var currentUrl = initialUrl
        var redirects = 0
        var activeConn: HttpURLConnection? = null
        try {
            while (true) {
                val conn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = PROBE_CONNECT_TIMEOUT_MS
                    readTimeout = PROBE_READ_TIMEOUT_MS
                    instanceFollowRedirects = false
                    setRequestProperty("User-Agent", "ForHappy/${BuildConfig.VERSION_NAME}")
                    if (resumeOffset > 0L) {
                        setRequestProperty("Range", "bytes=$resumeOffset-")
                    }
                }
                connRef.set(conn)
                activeConn = conn

                val status = conn.responseCode
                if (status in listOf(301, 302, 303, 307, 308)) {
                    val location = conn.getHeaderField("Location")
                        ?: throw IOException("下载重定向缺少 Location 头")
                    connRef.compareAndSet(conn, null)
                    conn.disconnect()
                    activeConn = null
                    currentUrl = URL(URL(currentUrl), location).toString()
                    if (++redirects > 6) throw IOException("下载重定向次数过多")
                    continue
                }

                val isResumed = resumeOffset > 0L && status == HttpURLConnection.HTTP_PARTIAL
                if (!isResumed && status !in 200..299) {
                    throw IOException("HTTP $status")
                }

                val contentLen = conn.contentLengthLong
                val startOffset: Long
                val totalBytes: Long
                if (isResumed) {
                    val rangeHeader = conn.getHeaderField("Content-Range").orEmpty()
                    if (!rangeHeader.startsWith("bytes $resumeOffset-", ignoreCase = true)) {
                        throw IOException("Content-Range 不匹配: $rangeHeader")
                    }
                    startOffset = resumeOffset
                    val totalFromRange = rangeHeader.substringAfter('/', "").toLongOrNull() ?: 0L
                    totalBytes = when {
                        totalFromRange > 0L -> totalFromRange
                        contentLen > 0L -> resumeOffset + contentLen
                        else -> expectedSizeBytes
                    }
                    if (expectedSizeBytes > 0L && totalBytes > 0L && totalBytes != expectedSizeBytes) {
                        throw IOException("文件总大小不一致: $totalBytes != $expectedSizeBytes")
                    }
                } else {
                    if (expectedSizeBytes > 0L && contentLen > 0L && contentLen != expectedSizeBytes) {
                        throw IOException("Content-Length 不匹配: $contentLen != $expectedSizeBytes")
                    }
                    startOffset = 0L
                    totalBytes = if (contentLen > 0L) contentLen else expectedSizeBytes
                }

                val input = conn.inputStream
                val buf = ByteArray(4096)
                var initialRead = 0
                while (initialRead < 4) {
                    val n = input.read(buf, initialRead, buf.size - initialRead)
                    if (n < 0) break
                    initialRead += n
                }
                if (initialRead == 0) {
                    throw IOException("响应体为空")
                }
                if (startOffset == 0L) {
                    if (initialRead < 4 ||
                        buf[0] != 0x50.toByte() ||
                        buf[1] != 0x4B.toByte() ||
                        buf[2] != 0x03.toByte() ||
                        buf[3] != 0x04.toByte()
                    ) {
                        throw IOException("非有效 APK 文件头")
                    }
                }

                val host = runCatching { URL(initialUrl).host }.getOrDefault("mirror")
                // Transfer connection ownership out of connRef so losing cleanup does not close it.
                connRef.compareAndSet(conn, null)
                activeConn = null
                return WinningProbe(
                    conn = conn,
                    stream = input,
                    initialBytes = buf,
                    initialLength = initialRead,
                    startOffset = startOffset,
                    totalBytes = totalBytes,
                    displayHost = host,
                )
            }
        } catch (e: Throwable) {
            connRef.getAndSet(null)?.let { c -> runCatching { c.disconnect() } }
            runCatching { activeConn?.disconnect() }
            throw e
        }
    }

    private fun streamWinningProbeToFile(
        probe: WinningProbe,
        tempFile: File,
        expectedSizeBytes: Long,
        minSpeedBytesPerSec: Long,
        speedWindowMs: Long,
        speedGraceMs: Long,
        onProgress: (percent: Int, host: String, bytesPerSec: Long) -> Unit,
    ) {
        try {
            var writtenTotal = probe.startOffset
            var lastReportedPct = -1
            val startTimeMs = System.currentTimeMillis()
            var windowStartMs = startTimeMs
            var windowBytes = 0L
            var currentSpeedBps = 0L

            RandomAccessFile(tempFile, "rw").use { raf ->
                if (probe.startOffset == 0L) {
                    raf.setLength(0L)
                }
                raf.seek(probe.startOffset)
                raf.write(probe.initialBytes, 0, probe.initialLength)
                writtenTotal += probe.initialLength
                windowBytes += probe.initialLength

                fun emitProgress() {
                    if (probe.totalBytes > 0L) {
                        val pct = ((writtenTotal * 100L) / probe.totalBytes).toInt().coerceIn(0, 99)
                        if (pct != lastReportedPct) {
                            lastReportedPct = pct
                            onProgress(pct, probe.displayHost, currentSpeedBps)
                        }
                    }
                }
                emitProgress()

                val buffer = ByteArray(16 * 1024)
                while (true) {
                    val n = probe.stream.read(buffer)
                    if (n < 0) break
                    raf.write(buffer, 0, n)
                    writtenTotal += n
                    windowBytes += n

                    val nowMs = System.currentTimeMillis()
                    val windowElapsed = nowMs - windowStartMs
                    if (windowElapsed >= speedWindowMs) {
                        currentSpeedBps = (windowBytes * 1000L) / windowElapsed.coerceAtLeast(1L)
                        val totalElapsed = nowMs - startTimeMs
                        if (totalElapsed >= speedGraceMs && currentSpeedBps < minSpeedBytesPerSec) {
                            raf.fd.sync()
                            throw IOException("镜像下载过慢 (${currentSpeedBps / 1024} KB/s)，切换通道…")
                        }
                        windowStartMs = nowMs
                        windowBytes = 0L
                        if (probe.totalBytes > 0L) {
                            val pct = ((writtenTotal * 100L) / probe.totalBytes).toInt().coerceIn(0, 99)
                            lastReportedPct = pct
                            onProgress(pct, probe.displayHost, currentSpeedBps)
                        }
                    } else {
                        emitProgress()
                    }
                }
                raf.fd.sync()
            }

            if (!isValidDownloadedApk(tempFile, expectedSizeBytes)) {
                throw IOException("下载文件校验失败 (${tempFile.length()} bytes)")
            }
        } finally {
            runCatching { probe.stream.close() }
            runCatching { probe.conn.disconnect() }
        }
    }

    fun installDownloadedApk(context: Context, apkFile: File): Result<Boolean> {
        return runCatching {
            require(apkFile.isFile && apkFile.length() > 0L) { "未找到已下载的更新包文件" }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
                val permIntent = Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}"),
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(permIntent)
                return@runCatching false
            }

            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile,
            )
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(installIntent)
            true
        }
    }

    internal fun customMirrorKvKey(): String = KV_CUSTOM_MIRROR_PREFIX
}

/**
 * Lifecycle-aware controller managing startup silent update checks, XIU2 mirror registry sync,
 * custom acceleration prefix persistence, multi-mirror racing download progress, and package installation.
 */
class AppUpdateController(
    private val appContext: Context,
    private val db: ForHappyDatabase? = null,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow(AppUpdateState())
    val state: StateFlow<AppUpdateState> = _state.asStateFlow()

    @Volatile
    private var startupChecked = false

    @Volatile
    private var pendingInstallOnResume = false

    init {
        if (db != null) {
            scope.launch {
                val savedPrefix = runCatching { db.getKv(AppUpdateManager.customMirrorKvKey()).orEmpty() }.getOrDefault("")
                val pool = AppUpdateManager.resolveMirrorPool(db = db, forceRefresh = false)
                _state.value = _state.value.copy(
                    customMirrorPrefix = savedPrefix.trim(),
                    activeMirrorCount = pool.size,
                )
            }
        }
    }

    fun saveCustomMirrorPrefix(rawPrefix: String) {
        val clean = rawPrefix.trim()
        _state.value = _state.value.copy(customMirrorPrefix = clean)
        if (db != null) {
            scope.launch {
                runCatching { db.putKv(AppUpdateManager.customMirrorKvKey(), clean) }
            }
        }
    }

    fun ensureStartupCheck() {
        if (startupChecked) return
        startupChecked = true
        checkForUpdates(silent = true)
    }

    fun checkForUpdates(silent: Boolean = false) {
        val currentStatus = _state.value.status
        if (currentStatus == UpdateStatus.CHECKING || currentStatus == UpdateStatus.DOWNLOADING) {
            return
        }
        scope.launch {
            if (!silent) {
                _state.value = _state.value.copy(
                    status = UpdateStatus.CHECKING,
                    statusMessage = "正在同步加速源并检查 Latest Release…",
                )
            }
            val pool = AppUpdateManager.resolveMirrorPool(db = db, forceRefresh = !silent)
            val custom = _state.value.customMirrorPrefix
            _state.value = _state.value.copy(activeMirrorCount = pool.size)

            val result = AppUpdateManager.fetchLatestRelease(db = db, customPrefix = custom)
            result.onSuccess { release ->
                if (release == null) {
                    _state.value = _state.value.copy(
                        currentVersion = BuildConfig.VERSION_NAME,
                        status = if (silent) UpdateStatus.IDLE else UpdateStatus.UP_TO_DATE,
                        statusMessage = if (silent) {
                            "当前版本 v${BuildConfig.VERSION_NAME}"
                        } else {
                            "当前仓库暂无已发布的 Release（当前 v${BuildConfig.VERSION_NAME}）"
                        },
                    )
                    return@onSuccess
                }

                val isNewer = AppUpdateManager.isNewerVersion(release.versionName, BuildConfig.VERSION_NAME)
                if (!isNewer) {
                    _state.value = _state.value.copy(
                        currentVersion = BuildConfig.VERSION_NAME,
                        status = if (silent) UpdateStatus.IDLE else UpdateStatus.UP_TO_DATE,
                        latestRelease = release,
                        statusMessage = "当前已是最新版本 v${BuildConfig.VERSION_NAME}",
                    )
                    return@onSuccess
                }

                val cachedFile = File(File(appContext.cacheDir, "updates"), AppUpdateManager.safeAssetFileName(release))
                if (AppUpdateManager.isValidDownloadedApk(cachedFile, release.apkSizeBytes)) {
                    _state.value = _state.value.copy(
                        currentVersion = BuildConfig.VERSION_NAME,
                        status = UpdateStatus.READY_TO_INSTALL,
                        latestRelease = release,
                        downloadProgressPercent = 100,
                        downloadedFilePath = cachedFile.absolutePath,
                        statusMessage = "新版本 v${release.versionName} 已就绪，点击即可安装",
                    )
                } else {
                    _state.value = _state.value.copy(
                        currentVersion = BuildConfig.VERSION_NAME,
                        status = UpdateStatus.AVAILABLE,
                        latestRelease = release,
                        statusMessage = "发现新版本 v${release.versionName}（${release.tagName}）",
                    )
                }
            }.onFailure { err ->
                if (silent) {
                    if (_state.value.status == UpdateStatus.CHECKING) {
                        _state.value = _state.value.copy(status = UpdateStatus.IDLE)
                    }
                } else {
                    _state.value = _state.value.copy(
                        status = UpdateStatus.ERROR,
                        statusMessage = "检查更新失败：${err.message ?: "网络异常"}",
                    )
                }
            }
        }
    }

    fun downloadAndInstall() {
        val current = _state.value
        if (current.status == UpdateStatus.DOWNLOADING) return
        val release = current.latestRelease ?: return

        val readyPath = current.downloadedFilePath?.let(::File)
        if (current.status == UpdateStatus.READY_TO_INSTALL &&
            readyPath != null &&
            AppUpdateManager.isValidDownloadedApk(readyPath, release.apkSizeBytes)
        ) {
            installReadyApk()
            return
        }

        scope.launch {
            _state.value = current.copy(
                status = UpdateStatus.DOWNLOADING,
                downloadProgressPercent = 0,
                statusMessage = "正在并发探测最快加速节点…",
            )
            val result = AppUpdateManager.downloadReleaseApk(
                context = appContext,
                release = release,
                db = db,
                customPrefix = current.customMirrorPrefix,
            ) { pct, host, bps ->
                val speedSuffix = if (bps > 0L) " · ${formatSpeed(bps)}" else ""
                val via = if (host.isNotBlank()) "[$host] " else ""
                _state.value = _state.value.copy(
                    status = UpdateStatus.DOWNLOADING,
                    downloadProgressPercent = pct,
                    statusMessage = "正在下载 v${release.versionName} $via($pct%$speedSuffix)",
                )
            }
            result.onSuccess { file ->
                _state.value = _state.value.copy(
                    status = UpdateStatus.READY_TO_INSTALL,
                    downloadProgressPercent = 100,
                    downloadedFilePath = file.absolutePath,
                    statusMessage = "安装包已下载完成，正在拉起系统安装器…",
                )
                installReadyApk()
            }.onFailure { err ->
                _state.value = _state.value.copy(
                    status = UpdateStatus.ERROR,
                    statusMessage = "下载失败：${err.message ?: "请检查网络或前往 GitHub 下载"}",
                )
            }
        }
    }

    private fun formatSpeed(bps: Long): String {
        return if (bps >= 1024L * 1024L) {
            String.format(java.util.Locale.US, "%.1f MB/s", bps / (1024.0 * 1024.0))
        } else {
            "${(bps / 1024L).coerceAtLeast(1L)} KB/s"
        }
    }

    fun installReadyApk() {
        val current = _state.value
        val file = current.downloadedFilePath?.let(::File) ?: return
        AppUpdateManager.installDownloadedApk(appContext, file)
            .onSuccess { launchedInstaller ->
                if (launchedInstaller) {
                    pendingInstallOnResume = false
                    _state.value = current.copy(
                        status = UpdateStatus.READY_TO_INSTALL,
                        statusMessage = "已调起系统安装器，请确认安装 v${current.latestRelease?.versionName ?: ""}",
                    )
                } else {
                    pendingInstallOnResume = true
                    _state.value = current.copy(
                        status = UpdateStatus.READY_TO_INSTALL,
                        statusMessage = "请授予「安装未知应用」权限后返回自动继续安装",
                    )
                }
            }
            .onFailure { err ->
                pendingInstallOnResume = false
                _state.value = current.copy(
                    status = UpdateStatus.ERROR,
                    statusMessage = "无法拉起安装器：${err.message ?: "未知错误"}",
                )
            }
    }

    fun onHostResumed() {
        if (!pendingInstallOnResume) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || appContext.packageManager.canRequestPackageInstalls()) {
            pendingInstallOnResume = false
            installReadyApk()
        }
    }
}
