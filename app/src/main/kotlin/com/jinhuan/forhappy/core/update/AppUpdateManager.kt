package com.jinhuan.forhappy.core.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.jinhuan.forhappy.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

enum class UpdateStatus {
    IDLE,
    CHECKING,
    UP_TO_DATE,
    AVAILABLE,
    DOWNLOADING,
    READY_TO_INSTALL,
    ERROR,
}

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
    val statusMessage: String = "当前版本 v${BuildConfig.VERSION_NAME} · 支持从 GitHub Release 检查更新",
)

/**
 * Queries GitHub Releases for newer APK builds, downloads over multi-mirror fallback channels,
 * verifies ZIP/APK integrity, and launches system package installation via FileProvider.
 */
object AppUpdateManager {
    const val GITHUB_OWNER = "DracaeneSunbringer"
    const val GITHUB_REPO = "ForHappy"
    const val RELEASES_HTML_URL = "https://github.com/$GITHUB_OWNER/$GITHUB_REPO/releases"
    private const val LATEST_RELEASE_API = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"
    private const val LATEST_RELEASE_WEB = "https://github.com/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"

    private val jsonParser = Json { ignoreUnknownKeys = true }

    suspend fun fetchLatestRelease(): Result<ReleaseUpdateInfo?> = withContext(Dispatchers.IO) {
        try {
            val apiResult = fetchViaGitHubApi()
            if (apiResult != null || apiNotFoundFlag) {
                return@withContext Result.success(apiResult)
            }
            Result.failure(IOException("未能获取 Release 信息"))
        } catch (e: CancellationException) {
            throw e
        } catch (primaryErr: Throwable) {
            try {
                val fallback = fetchViaReleaseRedirectFallback()
                Result.success(fallback)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                Result.failure(primaryErr)
            }
        }
    }

    @Volatile
    private var apiNotFoundFlag = false

    private fun fetchViaGitHubApi(): ReleaseUpdateInfo? {
        apiNotFoundFlag = false
        val conn = (URL(LATEST_RELEASE_API).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "ForHappy/${BuildConfig.VERSION_NAME}")
        }
        try {
            val code = conn.responseCode
            if (code == HttpURLConnection.HTTP_NOT_FOUND) {
                apiNotFoundFlag = true
                return null
            }
            if (code !in 200..299) {
                throw IOException("GitHub Release 查询失败 (HTTP $code)")
            }
            val body = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            return parseReleaseJson(body)
        } finally {
            conn.disconnect()
        }
    }

    private fun fetchViaReleaseRedirectFallback(): ReleaseUpdateInfo? {
        val conn = (URL(LATEST_RELEASE_WEB).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
            instanceFollowRedirects = false
            setRequestProperty("User-Agent", "ForHappy/${BuildConfig.VERSION_NAME}")
        }
        try {
            val code = conn.responseCode
            if (code == HttpURLConnection.HTTP_NOT_FOUND) {
                return null
            }
            if (code !in listOf(301, 302, 303, 307, 308)) {
                throw IOException("GitHub Releases 页面返回异常状态 (HTTP $code)")
            }
            val location = conn.getHeaderField("Location").orEmpty()
            val tag = location
                .substringAfter("/releases/tag/", missingDelimiterValue = "")
                .substringBefore('?')
                .substringBefore('#')
                .trim()
            if (tag.isBlank()) {
                return null
            }
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
        } finally {
            conn.disconnect()
        }
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

    internal fun buildCandidateDownloadUrls(rawUrl: String): List<String> {
        val clean = rawUrl.trim()
        if (clean.isBlank()) return emptyList()
        if (!clean.startsWith("https://github.com/", ignoreCase = true)) {
            return listOf(clean)
        }
        return listOf(
            "https://gh-proxy.com/$clean",
            "https://ghfast.top/$clean",
            "https://ghproxy.net/$clean",
            clean,
        )
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
        return readCount >= 2 && header[0] == 0x50.toByte() && header[1] == 0x4B.toByte()
    }

    suspend fun downloadReleaseApk(
        context: Context,
        release: ReleaseUpdateInfo,
        onProgress: (Int) -> Unit,
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val candidateUrls = buildCandidateDownloadUrls(release.apkDownloadUrl)
            require(candidateUrls.isNotEmpty()) { "该 Release 未附带可下载的 APK 安装包" }
            val updatesDir = File(context.cacheDir, "updates")
            if (!updatesDir.isDirectory && !updatesDir.mkdirs()) {
                throw IOException("无法创建更新缓存目录")
            }
            val safeFileName = safeAssetFileName(release)
            val targetFile = File(updatesDir, safeFileName)
            val tempFile = File(updatesDir, "$safeFileName.part")

            var lastError: Throwable? = null
            for (candidateUrl in candidateUrls) {
                try {
                    if (tempFile.exists()) {
                        tempFile.delete()
                    }
                    downloadFromSingleUrl(
                        initialUrl = candidateUrl,
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
                    onProgress(100)
                    return@withContext Result.success(targetFile)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    lastError = e
                }
            }
            throw (lastError ?: IOException("所有下载通道均不可用"))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    private fun downloadFromSingleUrl(
        initialUrl: String,
        expectedSizeBytes: Long,
        tempFile: File,
        onProgress: (Int) -> Unit,
    ) {
        var currentUrl = initialUrl
        var redirects = 0
        var conn: HttpURLConnection? = null
        try {
            while (true) {
                conn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 8_000
                    readTimeout = 20_000
                    instanceFollowRedirects = false
                    setRequestProperty("User-Agent", "ForHappy/${BuildConfig.VERSION_NAME}")
                }
                val status = conn.responseCode
                if (status in listOf(301, 302, 303, 307, 308)) {
                    val location = conn.getHeaderField("Location")
                        ?: throw IOException("下载重定向缺少 Location 头")
                    conn.disconnect()
                    currentUrl = URL(URL(currentUrl), location).toString()
                    if (++redirects > 6) throw IOException("下载重定向次数过多")
                    continue
                }
                if (status !in 200..299) {
                    throw IOException("APK 下载失败 (HTTP $status)")
                }
                break
            }

            val activeConn = conn ?: throw IOException("下载连接未建立")
            val totalBytes = activeConn.contentLengthLong.takeIf { it > 0L } ?: expectedSizeBytes
            var downloadedBytes = 0L
            var lastReportedPercent = -1

            activeConn.inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(16 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        downloadedBytes += read
                        if (totalBytes > 0L) {
                            val pct = ((downloadedBytes * 100L) / totalBytes).toInt().coerceIn(0, 100)
                            if (pct != lastReportedPercent) {
                                lastReportedPercent = pct
                                onProgress(pct)
                            }
                        }
                    }
                    output.fd.sync()
                }
            }

            if (tempFile.length() < 1024L) {
                throw IOException("下载的 APK 文件不完整")
            }
            if (expectedSizeBytes > 0L && tempFile.length() != expectedSizeBytes) {
                throw IOException("下载文件大小校验失败 (${tempFile.length()} / $expectedSizeBytes)")
            }
            val header = ByteArray(4)
            tempFile.inputStream().use { it.read(header) }
            if (header[0] != 0x50.toByte() || header[1] != 0x4B.toByte()) {
                throw IOException("下载的文件不是有效的 APK 安装包")
            }
        } finally {
            conn?.disconnect()
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
}

/**
 * Lifecycle-aware controller managing startup silent update checks, download progress,
 * and automatic resume after granting unknown-source package install permission.
 */
class AppUpdateController(private val appContext: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow(AppUpdateState())
    val state: StateFlow<AppUpdateState> = _state.asStateFlow()

    @Volatile
    private var startupChecked = false

    @Volatile
    private var pendingInstallOnResume = false

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
                    statusMessage = "正在查询 GitHub Latest Release…",
                )
            }
            val result = AppUpdateManager.fetchLatestRelease()
            result.onSuccess { release ->
                if (release == null) {
                    _state.value = AppUpdateState(
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
                    _state.value = AppUpdateState(
                        currentVersion = BuildConfig.VERSION_NAME,
                        status = if (silent) UpdateStatus.IDLE else UpdateStatus.UP_TO_DATE,
                        latestRelease = release,
                        statusMessage = "当前已是最新版本 v${BuildConfig.VERSION_NAME}",
                    )
                    return@onSuccess
                }

                val cachedFile = File(File(appContext.cacheDir, "updates"), AppUpdateManager.safeAssetFileName(release))
                if (AppUpdateManager.isValidDownloadedApk(cachedFile, release.apkSizeBytes)) {
                    _state.value = AppUpdateState(
                        currentVersion = BuildConfig.VERSION_NAME,
                        status = UpdateStatus.READY_TO_INSTALL,
                        latestRelease = release,
                        downloadProgressPercent = 100,
                        downloadedFilePath = cachedFile.absolutePath,
                        statusMessage = "新版本 v${release.versionName} 已就绪，点击即可安装",
                    )
                } else {
                    _state.value = AppUpdateState(
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
                statusMessage = "正在通过镜像通道下载 v${release.versionName}…",
            )
            val result = AppUpdateManager.downloadReleaseApk(appContext, release) { pct ->
                _state.value = _state.value.copy(
                    status = UpdateStatus.DOWNLOADING,
                    downloadProgressPercent = pct,
                    statusMessage = "正在下载 v${release.versionName} ($pct%)",
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
