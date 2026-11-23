package com.jinhuan.forhappy

import com.jinhuan.forhappy.core.network.api.formatUploadHttpError
import com.jinhuan.forhappy.core.network.media.MediaUrlRewriter
import com.jinhuan.forhappy.ui.web.shouldAutoCompleteWebLogin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaUrlRewriterTest {

    @Test
    fun rewritesStaticImgKeysAndSkipsDynamicAvatars() {
        assertEquals(
            "https://52dmeme.com/img/abc/xyz.webp",
            MediaUrlRewriter.rewriteMediaUrl("/img/abc/xyz.webp"),
        )
        assertEquals(
            "https://52dmeme.com/img/foo.png?w=400",
            MediaUrlRewriter.rewriteMediaUrl("https://cc.52dmeme.com/img/foo.png?w=400"),
        )
        assertEquals(
            "https://52dmeme.com/img/c/2026/11/e40365b5f5617626440c5cb0.jpg",
            MediaUrlRewriter.rewriteMediaUrl("https://img.52dmeme.com/img/c/2026/11/e40365b5f5617626440c5cb0.jpg"),
        )
        // Dynamic avatars under /img/d/* remain on 52dmeme.com origin
        assertEquals(
            "https://52dmeme.com/img/d/avatar/u1.svg",
            MediaUrlRewriter.rewriteMediaUrl("/img/d/avatar/u1.svg"),
        )
    }

    @Test
    fun buildsFallbackAndStickerUrls() {
        assertEquals(
            "https://52dmeme.com/stickers/dsh/smile.gif",
            MediaUrlRewriter.stickerUrl("dsh", "smile", "gif"),
        )
        assertEquals(
            "https://52dmeme.com/ext?u=https%3A%2F%2F52dmeme.com%2Fimg%2Fa.jpg",
            MediaUrlRewriter.externalFallbackUrl("/img/a.jpg"),
        )
        assertEquals(
            "https://52dmeme.com/ext?u=https%3A%2F%2Fcdn.ldstatic.com%2Fuser_avatar%2Flinux.do%2Fshiratori%2F288%2F1.png",
            MediaUrlRewriter.rewriteWithExtProxy("https://cdn.ldstatic.com/user_avatar/linux.do/shiratori/288/1.png"),
        )
        assertEquals(
            "https://52dmeme.com/img/abc.webp",
            MediaUrlRewriter.rewriteWithExtProxy("/img/abc.webp"),
        )
    }

    @Test
    fun parsesAvatarViewSpecFragmentsAccurately() {
        val rot90 = MediaUrlRewriter.parseAvatarViewSpec("/img/a/2026/11/aabbccddeeff001122334455.webp#av=512,512,90,1,0.5,0.5")
        assertNotNull(rot90)
        assertEquals("https://52dmeme.com/img/a/2026/11/aabbccddeeff001122334455.webp", rot90!!.cleanUrl)
        assertEquals(90, rot90.rotationDeg)
        assertEquals(1f, rot90.bw, 1e-4f)
        assertEquals(1f, rot90.bh, 1e-4f)
        assertEquals(0f, rot90.tx, 1e-4f)
        assertEquals(0f, rot90.ty, 1e-4f)

        val panZoomRot270 = MediaUrlRewriter.parseAvatarViewSpec("https://52dmeme.com/img/a/2026/11/aabbccddeeff001122334455.gif#av=608,640,270,1.5,0.2,0.8")
        assertNotNull(panZoomRot270)
        assertEquals("https://52dmeme.com/img/a/2026/11/aabbccddeeff001122334455.gif", panZoomRot270!!.cleanUrl)
        assertEquals(270, panZoomRot270.rotationDeg)
        assertEquals(1.5f / 608f, panZoomRot270.scale, 1e-6f)
        assertEquals((1f - 640f * (1.5f / 608f)) * (0.2f - 0.5f), panZoomRot270.tx, 1e-4f)
        assertEquals((1f - 608f * (1.5f / 608f)) * (0.8f - 0.5f), panZoomRot270.ty, 1e-4f)

        assertNull(MediaUrlRewriter.parseAvatarViewSpec("https://cdn.ldstatic.com/user_avatar/linux.do/foo/288/1.png"))
        assertNull(MediaUrlRewriter.parseAvatarViewSpec("/img/a/2026/11/aa.webp#av=100,100,45,1,0.5,0.5"))
        assertNull(MediaUrlRewriter.parseAvatarViewSpec("/img/a/2026/11/aa.webp#av=100,100,90"))
    }

    @Test
    fun webLoginCompletionGuardsAppAndAuthRoutes() {
        assertFalse(shouldAutoCompleteWebLogin("https://52dmeme.com/app", hasSessionCookie = true, cookiesChangedOrSawAuthFlow = true))
        assertFalse(shouldAutoCompleteWebLogin("https://52dmeme.com/login", hasSessionCookie = true, cookiesChangedOrSawAuthFlow = true))
        assertFalse(shouldAutoCompleteWebLogin("https://52dmeme.com/auth/callback?code=1", hasSessionCookie = true, cookiesChangedOrSawAuthFlow = true))
        assertFalse(shouldAutoCompleteWebLogin("https://52dmeme.com/register", hasSessionCookie = true, cookiesChangedOrSawAuthFlow = true))
        assertFalse(shouldAutoCompleteWebLogin("https://52dmeme.com/", hasSessionCookie = true, cookiesChangedOrSawAuthFlow = false))
        assertTrue(shouldAutoCompleteWebLogin("https://52dmeme.com/", hasSessionCookie = true, cookiesChangedOrSawAuthFlow = true))
        assertTrue(shouldAutoCompleteWebLogin("https://52dmeme.com/m/13077", hasSessionCookie = true, cookiesChangedOrSawAuthFlow = true))
    }

    @Test
    fun formatsUploadHttpErrorsWithCfAndVpsDiagnostics() {
        val err408 = formatUploadHttpError(408, "", cfRay = "a5f148edb9e0dbbe-LAX")
        assertTrue(err408.contains("上传超时"))
        assertTrue(err408.contains("a5f148edb9e0dbbe-LAX"))

        assertTrue(formatUploadHttpError(413, "Payload Too Large").contains("413"))
        assertTrue(formatUploadHttpError(522, "<html>522</html>").contains("522"))
        assertTrue(formatUploadHttpError(403, "<html>forbidden</html>", cfMitigated = "challenge").contains("403"))
        assertEquals("图片格式不支持", formatUploadHttpError(400, """{"error":"图片格式不支持"}"""))
    }
}
