package com.jinhuan.forhappy

import com.jinhuan.forhappy.core.chat.ChatRichTextParser
import com.jinhuan.forhappy.core.chat.ChatSessionController
import com.jinhuan.forhappy.core.chat.RedpackMosaicEngine
import com.jinhuan.forhappy.core.model.ChatGroup
import com.jinhuan.forhappy.core.model.ChatGroupLatest
import com.jinhuan.forhappy.core.model.ChatInputMode
import com.jinhuan.forhappy.core.model.ChatMessage
import com.jinhuan.forhappy.core.model.ChatMessagesResponse
import com.jinhuan.forhappy.core.model.ChatUser
import com.jinhuan.forhappy.core.model.DmThread
import com.jinhuan.forhappy.core.model.NovelExcerptSpec
import com.jinhuan.forhappy.core.model.UnifiedSessionItem
import com.jinhuan.forhappy.ui.chat.canGroupContinuation
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase4ChatGuessRedpackTest {

    @Test
    fun novelExcerptParsesSerializesAndPreviewsCleanly() {
        val spec = NovelExcerptSpec(
            bookId = "moriya-chronicle",
            chapter = 12,
            theme = "paper",
            title = "守矢神社见闻录",
            chapterTitle = "第 12 章 风起",
            excerpt = "山风掠过绘马架，青苔石阶尽头灯火微明。",
        )
        val wire = ChatRichTextParser.serializeNovelExcerpt(spec)
        val parsed = ChatRichTextParser.parseNovelExcerpt(wire)
        assertNotNull(parsed)
        assertEquals("moriya-chronicle", parsed?.bookId)
        assertEquals(12, parsed?.chapter)
        assertEquals("paper", parsed?.theme)
        assertEquals("守矢神社见闻录", parsed?.title)
        assertEquals("第 12 章 风起", parsed?.chapterTitle)
        assertEquals("山风掠过绘马架，青苔石阶尽头灯火微明。", parsed?.excerpt)
        assertEquals("[书房摘录] 《守矢神社见闻录》", ChatRichTextParser.previewSummary(wire))

        assertNull(ChatRichTextParser.parseNovelExcerpt("[novel-excerpt:v1:BadID:0:paper]\nTitle\nCh\nBody"))
    }

    @Test
    fun nineGridMosaicPreservesSelectedCellsAndIrreversiblyBlursHiddenCells() {
        val width = 90
        val height = 90
        val src = IntArray(width * height) { idx ->
            val x = idx % width
            val y = idx / width
            val r = (x * 17 + y * 31) and 0xFF
            val g = (x * 29 + y * 13) and 0xFF
            val b = (x * 7 + y * 43) and 0xFF
            (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }

        // Only center cell (index 4 -> row=1, col=1) revealed sharply.
        val out = RedpackMosaicEngine.synthesizeNineGridCluePixels(src, width, height, maskBits = 1 shl 4)
        assertEquals(src.size, out.size)

        // Check center cell pixel is bit-exact sharp
        val centerIdx = 45 * width + 45
        assertEquals(src[centerIdx], out[centerIdx])

        // Top-left cell (cell 0 -> rows 0..29, cols 0..29) is downsampled across coarse blocks,
        // meaning neighbor sharp pixels inside the same mosaic block become identical.
        assertEquals(out[0], out[1])
        assertEquals(out[0], out[width])
    }

    @Test(expected = IllegalArgumentException::class)
    fun nineGridMosaicRejectsZeroMaskBits() {
        RedpackMosaicEngine.synthesizeNineGridCluePixels(IntArray(9), 3, 3, maskBits = 0)
    }

    @Test
    fun externalPacketDetectionAndCipherPreviewsWorkAcrossAllSixGames() {
        val hb = RedpackMosaicEngine.detectExternalPacketLink("快来领 https://hb.unsnow.org/r/abc123")
        assertEquals(RedpackMosaicEngine.ExternalPacketPlatform.HB, hb?.platform)
        assertEquals("https://hb.unsnow.org/r/abc123", hb?.urlOrCode)

        val fish = RedpackMosaicEngine.detectExternalPacketLink("摸鱼红包 https://hb.lafi.sh/p/99")
        assertEquals(RedpackMosaicEngine.ExternalPacketPlatform.FISH, fish?.platform)

        val credit = RedpackMosaicEngine.detectExternalPacketLink("https://credit.linux.do/claim?k=1")
        assertEquals(RedpackMosaicEngine.ExternalPacketPlatform.CREDIT, credit?.platform)

        assertNull(RedpackMosaicEngine.detectExternalPacketLink("没有红包链接的普通聊天"))

        assertEquals("社神矢守", RedpackMosaicEngine.previewCipherText("守矢神社", "reverse"))
        assertEquals("矢守社神", RedpackMosaicEngine.previewCipherText("守矢神社", "pairs"))
        assertEquals("守神矢社", RedpackMosaicEngine.previewCipherText("守矢神社", "rails"))
        for (spec in RedpackMosaicEngine.CipherGameSpec.entries) {
            assertTrue(RedpackMosaicEngine.previewCipherText("尽欢快乐", spec.key).isNotBlank())
        }
    }

    @Test
    fun unifiedSessionUnreadCountsRespectMuteAndPinSorting() = runBlocking {
        val controller = ChatSessionController(
            fetchGroupsFn = {
                listOf(
                    ChatGroup(
                        id = 1L,
                        name = "默认大厅",
                        unread = 4,
                        atMe = 1,
                        muted = false,
                        latest = ChatGroupLatest(id = 100L, at = 1000L, text = "大厅最新"),
                    ),
                    ChatGroup(
                        id = 2L,
                        name = "静音水群",
                        unread = 9,
                        atMe = 2,
                        muted = true,
                        latest = ChatGroupLatest(id = 200L, at = 5000L, text = "好多人啊"),
                    ),
                    ChatGroup(
                        id = 3L,
                        name = "无未读静音群",
                        unread = 12,
                        atMe = 0,
                        muted = true,
                        latest = ChatGroupLatest(id = 300L, at = 2000L, text = "悄悄聊天"),
                    ),
                )
            },
            fetchMessagesFn = { _, _, _, _ -> ChatMessagesResponse() },
            sendMessageFn = { _, _, _, _ -> Result.success(null) },
            markReadFn = { _, _ -> true },
            toggleReactionFn = { _, _ -> true },
            editMessageFn = { _, _ -> true },
            deleteMessageFn = { _ -> true },
            fetchDmThreadsFn = {
                listOf(
                    DmThread(id = 10L, peerId = 55L, peerName = "小早苗", unread = 3, lastAt = 6000L, lastBody = "hi"),
                    DmThread(id = 11L, peerId = 66L, peerName = "静音好友", unread = 7, lastAt = 4000L, lastBody = "hey"),
                )
            },
        )

        controller.refreshGroups()
        controller.setDmMutedLocal(11L, true)

        val st = controller.uiState.value
        // Muted groups only count `@me`: 4 (g1 unmuted) + 2 (g2 muted atMe) + 0 (g3 muted, no atMe) = 6
        assertEquals(6, st.groupUnreadCount)
        // Unmuted DM (10L) has 3 unread, muted DM (11L) contributes 0
        assertEquals(3, st.dmUnreadCount)
        assertEquals(9, st.totalUnreadCount)
        assertTrue(st.hasUnreadMention)

        val allItems = controller.buildUnifiedConversationItems(filter = "all")
        // g:1 is pinned by default, so it comes first even though timestamp is older than d:10 and g:2
        assertEquals("g:1", allItems.first().sessionKey)
        assertEquals("d:10", allItems[1].sessionKey)

        val atItems = controller.buildUnifiedConversationItems(filter = "at")
        assertEquals(listOf("g:1", "g:2"), atItems.map { it.sessionKey })

        val dmItems = controller.buildUnifiedConversationItems(filter = "dm")
        assertTrue(dmItems.all { it is UnifiedSessionItem.Dm })
    }

    @Test
    fun perGroupThreeModeDraftsRemainIsolated() {
        val controller = ChatSessionController(
            fetchGroupsFn = { emptyList() },
            fetchMessagesFn = { _, _, _, _ -> ChatMessagesResponse() },
            sendMessageFn = { _, _, _, _ -> Result.success(null) },
            markReadFn = { _, _ -> true },
            toggleReactionFn = { _, _ -> true },
            editMessageFn = { _, _ -> true },
            deleteMessageFn = { _ -> true },
        )

        controller.updateGroupDraft(1L, ChatInputMode.CHAT, "普通聊天草稿")
        controller.updateGroupDraft(1L, ChatInputMode.GUESS, "猜词答案")
        controller.updateGroupDraft(1L, ChatInputMode.ASK, "是动物吗")
        controller.updateGroupDraft(2L, ChatInputMode.CHAT, "二群草稿")

        assertEquals("普通聊天草稿", controller.draftForGroup(1L, ChatInputMode.CHAT))
        assertEquals("猜词答案", controller.draftForGroup(1L, ChatInputMode.GUESS))
        assertEquals("是动物吗", controller.draftForGroup(1L, ChatInputMode.ASK))
        assertEquals("二群草稿", controller.draftForGroup(2L, ChatInputMode.CHAT))
        assertEquals("", controller.draftForGroup(2L, ChatInputMode.GUESS))
    }

    @Test
    fun canGroupContinuationEnforcesThreeMinuteThreshold() {
        val u1 = ChatUser(id = 10L, username = "alice", name = "Alice")
        val base = ChatMessage(id = 1L, user = u1, body = "one", createdAt = 1_794_844_000L)
        assertTrue(canGroupContinuation(base, base.copy(id = 2L, createdAt = base.createdAt + 179L)))
        assertFalse(canGroupContinuation(base, base.copy(id = 3L, createdAt = base.createdAt + 180L)))
    }
}
