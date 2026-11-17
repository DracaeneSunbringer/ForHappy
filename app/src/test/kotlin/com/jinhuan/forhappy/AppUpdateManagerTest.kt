package com.jinhuan.forhappy

import com.jinhuan.forhappy.core.update.AppUpdateManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

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
        assertTrue(AppUpdateManager.isNewerVersion("0.1.1", "0.1.0"))
        assertTrue(AppUpdateManager.isNewerVersion("1.0.0-rc1", "0.9.9"))
        assertFalse(AppUpdateManager.isNewerVersion("v0.1.0", "0.1.0"))
        assertFalse(AppUpdateManager.isNewerVersion("0.0.9", "0.1.0"))
        assertFalse(AppUpdateManager.isNewerVersion("", "0.1.0"))
    }

    @Test
    fun buildCandidateDownloadUrls_ordersMirrorsThenDirectGitHub() {
        val raw = "https://github.com/DracaeneSunbringer/ForHappy/releases/download/v0.2.0/ForHappy-v0.2.0.apk"
        val candidates = AppUpdateManager.buildCandidateDownloadUrls(raw)
        assertEquals(
            listOf(
                "https://gh-proxy.com/$raw",
                "https://ghfast.top/$raw",
                "https://ghproxy.net/$raw",
                raw,
            ),
            candidates,
        )
        assertEquals(listOf("https://cdn.example.com/app.apk"), AppUpdateManager.buildCandidateDownloadUrls("https://cdn.example.com/app.apk"))
        assertTrue(AppUpdateManager.buildCandidateDownloadUrls("   ").isEmpty())
    }
}
