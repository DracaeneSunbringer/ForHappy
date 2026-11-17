package com.jinhuan.forhappy

import com.jinhuan.forhappy.core.update.AppUpdateManager
import com.jinhuan.forhappy.core.update.GitHubMirrorNode
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class AppUpdateManagerTest {

    @Test
    fun parseReleaseJson_extractsApkAssetAndMetadata() {
        val sampleJson = """
            {
              "tag_name": "v0.2.0",
              "name": "ForHappy v0.2.0",
              "body": "1. 优化自动更新镜像回退\n2. 修复群聊渲染细节",
              "html_url": "https://github.com/DracaeneSunbringer/ForHappy/releases/tag/v0.2.0",
              "assets": [
                {
                  "name": "checksums.txt",
                  "content_type": "text/plain",
                  "size": 128,
                  "browser_download_url": "https://github.com/DracaeneSunbringer/ForHappy/releases/download/v0.2.0/checksums.txt"
                },
                {
                  "name": "ForHappy-v0.2.0.apk",
                  "content_type": "application/vnd.android.package-archive",
                  "size": 24576000,
                  "browser_download_url": "https://github.com/DracaeneSunbringer/ForHappy/releases/download/v0.2.0/ForHappy-v0.2.0.apk"
                }
              ]
            }
        """.trimIndent()

        val info = AppUpdateManager.parseReleaseJson(sampleJson)
        assertEquals("v0.2.0", info.tagName)
        assertEquals("0.2.0", info.versionName)
        assertEquals("ForHappy v0.2.0", info.releaseTitle)
        assertEquals("ForHappy-v0.2.0.apk", info.apkAssetName)
        assertEquals(24576000L, info.apkSizeBytes)
        assertEquals(
            "https://github.com/DracaeneSunbringer/ForHappy/releases/download/v0.2.0/ForHappy-v0.2.0.apk",
            info.apkDownloadUrl,
        )
        assertEquals(
            "https://github.com/DracaeneSunbringer/ForHappy/releases/tag/v0.2.0",
            info.htmlUrl,
        )
        assertTrue(info.releaseNotes.contains("优化自动更新镜像回退"))
    }

    @Test
    fun isNewerVersion_handlesSemverAndPrefixes() {
        assertTrue(AppUpdateManager.isNewerVersion("v0.2.0", "0.1.0"))
        assertTrue(AppUpdateManager.isNewerVersion("0.1.2", "0.1.1"))
        assertTrue(AppUpdateManager.isNewerVersion("1.0.0-rc1", "0.9.9"))
        assertFalse(AppUpdateManager.isNewerVersion("v0.1.2", "0.1.2"))
        assertFalse(AppUpdateManager.isNewerVersion("0.0.9", "0.1.0"))
        assertFalse(AppUpdateManager.isNewerVersion("", "0.1.0"))
    }

    @Test
    fun parseXiu2UserscriptMirrors_extractsActiveAndPrunesCommentedDeadMirrors() {
        val sampleScript = """
            const download_url_us = [
                ['https://gh.h233.eu.org/https://github.com', '美国', '[美国 Cloudflare CDN] - @XIU2'],
                //['https://hub.glowp.xyz/https://github.com', '香港', '[中国香港] - dead'],
                ['https://edgeone.gh-proxy.org/https://github.com', '其他', '[edgeone] - gh-proxy.com'],
                ['https://cors.isteed.cc/github.com', '美国', '[美国 Cloudflare CDN] - @Lufs'],
                ['https://xget.xi-xu.me/gh', '美国', '[美国 Cloudflare CDN] - Xget'],
                ['https://ghproxy.net/https://github.com', '其他', '[法国] - ghproxy.net'],
            ], clone_url = [
                ['https://gitclone.com', '国内', 'clone']
            ];
        """.trimIndent()

        val snapshot = AppUpdateManager.parseXiu2UserscriptMirrors(sampleScript)
        val activePrefixes = snapshot.active.map { it.prefix }
        assertTrue(activePrefixes.contains("https://gh.h233.eu.org/https://github.com"))
        assertTrue(activePrefixes.contains("https://edgeone.gh-proxy.org/https://github.com"))
        assertTrue(activePrefixes.contains("https://cors.isteed.cc/github.com"))
        assertTrue(activePrefixes.contains("https://xget.xi-xu.me/gh"))
        assertTrue(snapshot.disabledPrefixes.contains("https://hub.glowp.xyz/https://github.com"))
        assertFalse(activePrefixes.contains("https://hub.glowp.xyz/https://github.com"))

        assertEquals("apac_multi", snapshot.active.first { it.prefix.contains("edgeone") }.group)
        assertEquals("independent", snapshot.active.first { it.prefix.contains("ghproxy.net") }.group)
        assertEquals("cf_pool", snapshot.active.first { it.prefix.contains("gh.h233") }.group)

        val merged = AppUpdateManager.mergeMirrorPool(snapshot)
        assertFalse(merged.any { it.prefix == "https://hub.glowp.xyz/https://github.com" })
    }

    @Test
    fun applyMirrorPrefix_rewritesAllXiu2AndCustomUrlFormats() {
        val raw = "https://github.com/DracaeneSunbringer/ForHappy/releases/download/v0.1.2/ForHappy-v0.1.2.apk"
        val suffix = "/DracaeneSunbringer/ForHappy/releases/download/v0.1.2/ForHappy-v0.1.2.apk"

        assertEquals(
            "https://edgeone.gh-proxy.org/https://github.com$suffix",
            AppUpdateManager.applyMirrorPrefix("https://edgeone.gh-proxy.org/https://github.com/", raw),
        )
        assertEquals(
            "https://cors.isteed.cc/github.com$suffix",
            AppUpdateManager.applyMirrorPrefix("https://cors.isteed.cc/github.com", raw),
        )
        assertEquals(
            "https://down.npee.cn/?https://github.com$suffix",
            AppUpdateManager.applyMirrorPrefix("https://down.npee.cn/?https://github.com", raw),
        )
        assertEquals(
            "https://xget.xi-xu.me/gh$suffix",
            AppUpdateManager.applyMirrorPrefix("https://xget.xi-xu.me/gh", raw),
        )
        assertEquals(
            "https://my-worker.example.com/https://github.com$suffix",
            AppUpdateManager.applyMirrorPrefix("https://my-worker.example.com/", raw),
        )
        assertEquals(raw, AppUpdateManager.applyMirrorPrefix("https://github.com", raw))
    }

    @Test
    fun buildOrderedCandidateUrls_placesCustomFirstInterleavesGroupsAndDirectLast() {
        val raw = "https://github.com/DracaeneSunbringer/ForHappy/releases/download/v0.1.2/ForHappy-v0.1.2.apk"
        val mirrors = listOf(
            GitHubMirrorNode("https://apac-1.test/https://github.com", group = "apac_multi"),
            GitHubMirrorNode("https://apac-2.test/https://github.com", group = "apac_multi"),
            GitHubMirrorNode("https://ind-1.test/https://github.com", group = "independent"),
            GitHubMirrorNode("https://cf-1.test/https://github.com", group = "cf_pool"),
        )
        val candidates = AppUpdateManager.buildOrderedCandidateUrls(
            rawUrl = raw,
            mirrors = mirrors,
            customPrefix = "https://my-custom.test/https://github.com",
            random = Random(42),
        )

        assertTrue(candidates.first().startsWith("https://my-custom.test/"))
        assertEquals(raw, candidates.last())
        assertEquals(6, candidates.size)

        // Default list preserves raw last and ignores non-github URLs cleanly
        val defaultList = AppUpdateManager.buildCandidateDownloadUrls(raw)
        assertEquals(raw, defaultList.last())
        assertTrue(defaultList.size > 10)
        assertEquals(listOf("https://cdn.example.com/app.apk"), AppUpdateManager.buildCandidateDownloadUrls("https://cdn.example.com/app.apk"))
        assertTrue(AppUpdateManager.buildCandidateDownloadUrls("   ").isEmpty())
    }

    @Test
    fun downloadFromCandidates_racesConcurrentlyRejectsHtmlAndSelectsFastValidZip() = runBlocking {
        val validPayload = ByteArray(2048) { idx -> (idx % 251).toByte() }.apply {
            this[0] = 0x50
            this[1] = 0x4B
            this[2] = 0x03
            this[3] = 0x04
        }

        val badHtmlServer = MockWebServer()
        val slowServer = MockWebServer()
        val fastZipServer = MockWebServer()
        val tempDir = Files.createTempDirectory("forhappy_upd_race").toFile()
        val tempFile = File(tempDir, "test.apk.part")

        try {
            badHtmlServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody("<html><body>Mirror blocked</body></html>"),
            )
            slowServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setHeadersDelay(1500, TimeUnit.MILLISECONDS)
                    .setBody(Buffer().write(validPayload)),
            )
            fastZipServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(Buffer().write(validPayload)),
            )

            badHtmlServer.start()
            slowServer.start()
            fastZipServer.start()

            val hostsSeen = mutableListOf<String>()
            AppUpdateManager.downloadFromCandidates(
                candidateUrls = listOf(
                    badHtmlServer.url("/bad.apk").toString(),
                    slowServer.url("/slow.apk").toString(),
                    fastZipServer.url("/good.apk").toString(),
                ),
                expectedSizeBytes = validPayload.size.toLong(),
                tempFile = tempFile,
                onProgress = { _, host, _ -> if (host.isNotBlank()) hostsSeen += host },
            )

            assertTrue(AppUpdateManager.isValidDownloadedApk(tempFile, validPayload.size.toLong()))
            assertTrue(tempFile.readBytes().contentEquals(validPayload))
            assertTrue(hostsSeen.any { it == fastZipServer.hostName })
        } finally {
            runCatching { badHtmlServer.shutdown() }
            runCatching { slowServer.shutdown() }
            runCatching { fastZipServer.shutdown() }
            tempFile.delete()
            tempDir.delete()
        }
    }

    @Test
    fun downloadFromCandidates_failsOverOnSlowSpeedAndResumesWithRange206() = runBlocking {
        val totalSize = 4096
        val fullPayload = ByteArray(totalSize) { idx -> ((idx * 7) % 253).toByte() }.apply {
            this[0] = 0x50
            this[1] = 0x4B
            this[2] = 0x03
            this[3] = 0x04
        }
        val firstChunkSize = 512

        val throttledServer = MockWebServer()
        val fastResumeServer = MockWebServer()
        val tempDir = Files.createTempDirectory("forhappy_upd_resume").toFile()
        val tempFile = File(tempDir, "resume.apk.part")

        try {
            // Send first 512 bytes cleanly, then throttle remaining bytes below minSpeedBytesPerSec
            throttledServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Length", totalSize.toString())
                    .setBody(Buffer().write(fullPayload))
                    .throttleBody(firstChunkSize.toLong(), 350, TimeUnit.MILLISECONDS),
            )

            fastResumeServer.dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    val range = request.getHeader("Range") ?: ""
                    val offset = range.removePrefix("bytes=").substringBefore('-').toIntOrNull() ?: 0
                    return if (range.startsWith("bytes=") && offset in 1 until totalSize) {
                        val rem = fullPayload.copyOfRange(offset, totalSize)
                        MockResponse()
                            .setResponseCode(206)
                            .setHeader("Content-Range", "bytes $offset-${totalSize - 1}/$totalSize")
                            .setHeader("Content-Length", rem.size.toString())
                            .setBody(Buffer().write(rem))
                    } else {
                        MockResponse()
                            .setResponseCode(200)
                            .setHeader("Content-Length", totalSize.toString())
                            .setBody(Buffer().write(fullPayload))
                    }
                }
            }

            throttledServer.start()
            fastResumeServer.start()

            // Wave 1 (candidates 1..4): only throttledServer is in wave 1, triggers slow watchdog -> wave 2 uses fastResumeServer
            val candidates = listOf(
                throttledServer.url("/apk").toString(),
                "http://127.0.0.1:1/dead1",
                "http://127.0.0.1:1/dead2",
                "http://127.0.0.1:1/dead3",
                fastResumeServer.url("/apk").toString(),
            )

            AppUpdateManager.downloadFromCandidates(
                candidateUrls = candidates,
                expectedSizeBytes = totalSize.toLong(),
                tempFile = tempFile,
                minSpeedBytesPerSec = 50_000L, // > 512B / 350ms (~1.4 KB/s)
                speedWindowMs = 150L,
                speedGraceMs = 100L,
                onProgress = { _, _, _ -> },
            )

            assertTrue(AppUpdateManager.isValidDownloadedApk(tempFile, totalSize.toLong()))
            assertTrue(tempFile.readBytes().contentEquals(fullPayload))
            val resumeReq = fastResumeServer.takeRequest(1, TimeUnit.SECONDS)
            val rangeHdr = resumeReq?.getHeader("Range") ?: ""
            assertTrue("Expected Range header on wave 2 resume, got '$rangeHdr'", rangeHdr.startsWith("bytes="))
        } finally {
            runCatching { throttledServer.shutdown() }
            runCatching { fastResumeServer.shutdown() }
            tempFile.delete()
            tempDir.delete()
        }
    }
}
