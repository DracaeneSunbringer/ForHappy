package com.jinhuan.forhappy

import com.jinhuan.forhappy.core.network.media.MediaUrlRewriter
import org.junit.Assert.assertEquals
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
}
