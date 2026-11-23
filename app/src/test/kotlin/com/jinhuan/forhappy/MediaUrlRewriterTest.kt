package com.jinhuan.forhappy

import com.jinhuan.forhappy.core.network.media.MediaUrlRewriter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class MediaUrlRewriterTest {

    @Test
    fun rewritesStaticImgKeysAndSkipsDynamicAvatars() {
        assertEquals(
            "https://img.52dmeme.com/img/abc/xyz.webp",
            MediaUrlRewriter.rewriteMediaUrl("/img/abc/xyz.webp"),
        )
        assertEquals(
            "https://img.52dmeme.com/img/foo.png?w=400",
            MediaUrlRewriter.rewriteMediaUrl("https://cc.52dmeme.com/img/foo.png?w=400"),
        )
        // Dynamic avatars under /img/d/* MUST remain on 52dmeme.com origin
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
            "https://52dmeme.com/ext?u=https%3A%2F%2Fimg.52dmeme.com%2Fimg%2Fa.jpg",
            MediaUrlRewriter.externalFallbackUrl("/img/a.jpg"),
        )
        assertEquals(
            "https://52dmeme.com/ext?u=https%3A%2F%2Fcdn.ldstatic.com%2Fuser_avatar%2Flinux.do%2Fshiratori%2F288%2F1.png",
            MediaUrlRewriter.rewriteWithExtProxy("https://cdn.ldstatic.com/user_avatar/linux.do/shiratori/288/1.png"),
        )
        assertEquals(
            "https://img.52dmeme.com/img/abc.webp",
            MediaUrlRewriter.rewriteWithExtProxy("/img/abc.webp"),
        )
    }

    @Test
    fun parsesAvatarViewSpecFragmentsAccurately() {
        val rot90 = MediaUrlRewriter.parseAvatarViewSpec("/img/a/2026/11/aabbccddeeff001122334455.webp#av=512,512,90,1,0.5,0.5")
        assertNotNull(rot90)
        assertEquals("https://img.52dmeme.com/img/a/2026/11/aabbccddeeff001122334455.webp", rot90!!.cleanUrl)
        assertEquals(90, rot90.rotationDeg)
        assertEquals(1f, rot90.bw, 1e-4f)
        assertEquals(1f, rot90.bh, 1e-4f)
        assertEquals(0f, rot90.tx, 1e-4f)
        assertEquals(0f, rot90.ty, 1e-4f)

        val panZoomRot270 = MediaUrlRewriter.parseAvatarViewSpec("https://52dmeme.com/img/a/2026/11/aabbccddeeff001122334455.gif#av=608,640,270,1.5,0.2,0.8")
        assertNotNull(panZoomRot270)
        assertEquals("https://img.52dmeme.com/img/a/2026/11/aabbccddeeff001122334455.gif", panZoomRot270!!.cleanUrl)
        assertEquals(270, panZoomRot270.rotationDeg)
        assertEquals(1.5f / 608f, panZoomRot270.scale, 1e-6f)
        assertEquals((1f - 640f * (1.5f / 608f)) * (0.2f - 0.5f), panZoomRot270.tx, 1e-4f)
        assertEquals((1f - 608f * (1.5f / 608f)) * (0.8f - 0.5f), panZoomRot270.ty, 1e-4f)

        assertNull(MediaUrlRewriter.parseAvatarViewSpec("https://cdn.ldstatic.com/user_avatar/linux.do/foo/288/1.png"))
        assertNull(MediaUrlRewriter.parseAvatarViewSpec("/img/a/2026/11/aa.webp#av=100,100,45,1,0.5,0.5"))
        assertNull(MediaUrlRewriter.parseAvatarViewSpec("/img/a/2026/11/aa.webp#av=100,100,90"))
    }
}
