package com.jinhuan.forhappy

import com.jinhuan.forhappy.core.chat.ChatRichTextParser
import com.jinhuan.forhappy.core.chat.ChatSessionController
import com.jinhuan.forhappy.core.model.ChatBoost
import com.jinhuan.forhappy.core.model.ChatGroup
import com.jinhuan.forhappy.core.model.ChatMessage
import com.jinhuan.forhappy.core.model.ChatMessagesResponse
import com.jinhuan.forhappy.core.model.ChatReplyPreview
import com.jinhuan.forhappy.core.model.ChatUser
import com.jinhuan.forhappy.ui.chat.canGroupContinuation
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatRichTextAndInflightTest {

    @Test
    fun richTextParsesStickersPostRefsHashtagsAndQuotes() {
        val raw = """[quote="小欢, msg:99"]
看看 [dsh:happy] 还有 No.1234 #每日一图 https://52dmeme.com/m/1234"""
        val parsed = ChatRichTextParser.parse(raw)
        assertEquals("小欢", parsed.quoteAuthor)
        assertEquals(99L, parsed.quoteMessageId)
        assertTrue(parsed.tokens.any { it == ChatRichTextParser.Token.Sticker("dsh", "happy") })
        assertTrue(parsed.tokens.any { it is ChatRichTextParser.Token.PostRef && it.memeId == 1234L })
        assertTrue(parsed.tokens.any { it == ChatRichTextParser.Token.Hashtag("每日一图") })
        assertTrue(parsed.tokens.any { it == ChatRichTextParser.Token.WebLink("https://52dmeme.com/m/1234") })
    }

    @Test
    fun pollingSettlesMatchingInflightAndAppliesChangedEdits() {
        val controller = ChatSessionController(
            fetchGroupsFn = { emptyList() },
            fetchMessagesFn = { _, _, _, _ -> ChatMessagesResponse() },
            sendMessageFn = { _, _, _, _ -> Result.success(null) },
            markReadFn = { _, _ -> true },
            toggleReactionFn = { _, _ -> true },
            editMessageFn = { _, _ -> true },
            deleteMessageFn = { _ -> true },
        )

        val local = ChatMessage(
            id = 0L,
            body = "hello [dsh:wave]",
            mine = true,
            replyTo = ChatReplyPreview(id = 50L, body = "hi"),
            createdAt = 1000L,
            localId = "loc-1",
            deliveryState = "sending",
        )
        val existingServer = ChatMessage(id = 50L, body = "hi", createdAt = 999L)

        val incomingConfirmed = ChatMessage(
            id = 101L,
            body = "hello [dsh:wave]",
            mine = true,
            replyTo = ChatReplyPreview(id = 50L, body = "hi"),
            createdAt = 1001L,
        )
        val editedOld = ChatMessage(id = 50L, body = "hi (edited)", edited = true, createdAt = 999L)

        val merged = controller.mergeServerDelta(
            current = listOf(existingServer, local),
            newItems = listOf(incomingConfirmed),
            changedItems = listOf(editedOld),
        )

        assertEquals(2, merged.size)
        assertEquals(50L, merged[0].id)
        assertEquals("hi (edited)", merged[0].body)
        assertEquals(101L, merged[1].id)
        assertEquals("sent", merged[1].deliveryState)
    }

    @Test
    fun acknowledgeGroupReadSendsMarkerAndClearsUnreadCounter() = runBlocking {
        var sentGid = -1L
        var sentUpTo = -1L
        val controller = ChatSessionController(
            fetchGroupsFn = {
                listOf(ChatGroup(id = 1L, name = "守矢神社", unread = 5, latestId = 300L))
            },
            fetchMessagesFn = { _, _, _, beforeId ->
                if (beforeId != null) {
                    ChatMessagesResponse(
                        items = listOf(
                            ChatMessage(id = 280L, body = "old1", createdAt = 900L),
                            ChatMessage(id = 290L, body = "old2", createdAt = 910L),
                        ),
                        more = false,
                    )
                } else {
                    ChatMessagesResponse(
                        items = listOf(ChatMessage(id = 300L, body = "latest", createdAt = 1000L)),
                        more = true,
                    )
                }
            },
            sendMessageFn = { _, _, _, _ -> Result.success(null) },
            markReadFn = { gid, lastId ->
                sentGid = gid
                sentUpTo = lastId
                true
            },
            toggleReactionFn = { _, _ -> true },
            editMessageFn = { _, _ -> true },
            deleteMessageFn = { _ -> true },
        )

        controller.refreshGroups()
        assertEquals(5, controller.uiState.value.groups.first().unread)

        controller.isChatForegroundAtBottom = true
        controller.pollOnce(1L)

        assertEquals(1L, sentGid)
        assertEquals(300L, sentUpTo)
        assertEquals(0, controller.uiState.value.groups.first().unread)

        val prepended = controller.loadOlderHistory()
        assertEquals(2, prepended)
        assertEquals(3, controller.uiState.value.messages.size)
        assertEquals(280L, controller.uiState.value.messages.first().id)
    }

    @Test
    fun canGroupContinuationNeverMergesDifferentNestedUsers() {
        val u1 = ChatUser(id = 10L, username = "alice", name = "Alice")
        val u2 = ChatUser(id = 20L, username = "bob", name = "Bob")

        // Server messages do NOT populate top-level `userId` (stays 0L); author lives in `user.id`.
        val m1 = ChatMessage(id = 1L, userId = 0L, user = u1, body = "hello", createdAt = 1_794_844_700L)
        val m2DifferentUser = ChatMessage(id = 2L, userId = 0L, user = u2, body = "world", createdAt = 1_794_844_730L)
        val m3SameUser = ChatMessage(id = 3L, userId = 0L, user = u1, body = "again", createdAt = 1_794_844_780L)
        val m4MineMismatch = ChatMessage(id = 4L, userId = 0L, user = u1, mine = true, body = "mine", createdAt = 1_794_844_785L)
        val m5WithReply = ChatMessage(
            id = 5L,
            userId = 0L,
            user = u1,
            body = "replying",
            replyTo = ChatReplyPreview(id = 1L, body = "hello"),
            createdAt = 1_794_844_790L,
        )

        assertFalse(canGroupContinuation(m1, m2DifferentUser))
        assertTrue(canGroupContinuation(m1, m3SameUser))
        assertFalse(canGroupContinuation(m1, m4MineMismatch))
        assertFalse(canGroupContinuation(m1, m5WithReply))
        assertFalse(canGroupContinuation(m1, m3SameUser.copy(createdAt = m1.createdAt + 305L)))

        // Across Beijing midnight (UTC+8): 2026-11-16 23:59:00 vs 2026-11-17 00:01:00
        // UTC timestamp for 2026-11-17 00:00:00+08:00 = multiple of 86400 minus 28800
        val midnightUtc = 20_774L * 86_400L - 28_800L
        val beforeMidnight = ChatMessage(id = 10L, user = u1, body = "night", createdAt = midnightUtc - 60L)
        val afterMidnight = ChatMessage(id = 11L, user = u1, body = "morning", createdAt = midnightUtc + 60L)
        assertFalse(canGroupContinuation(beforeMidnight, afterMidnight))
    }

    @Test
    fun richTextParsesFullQuoteBlocksTwemojiMarkdownSpoilersAndDetails() {
        val raw = """
            [quote="浅草, msg:5120, g:1"]
            引用里的原话 [ld:tieba_087]
            [/quote]
            [details="点击展开"]细节说明[/details]
            回复正文 **粗体** :joy: [spoiler]剧透[/spoiler] `code()`
        """.trimIndent()

        val parsed = ChatRichTextParser.parse(raw)
        val quoteBlock = parsed.blocks.filterIsInstance<ChatRichTextParser.Block.QuoteBlock>().firstOrNull()
        assertTrue(quoteBlock != null)
        assertEquals("浅草", quoteBlock?.author)
        assertEquals(5120L, quoteBlock?.messageId)
        assertEquals(1L, quoteBlock?.groupId)
        assertTrue(quoteBlock!!.bodyTokens.any { it == ChatRichTextParser.Token.Sticker("ld", "tieba_087") })

        val details = parsed.blocks.filterIsInstance<ChatRichTextParser.Block.DetailsBlock>().firstOrNull()
        assertEquals("点击展开", details?.summary)

        assertTrue(parsed.tokens.any { it == ChatRichTextParser.Token.Bold("粗体") })
        assertTrue(parsed.tokens.any { it == ChatRichTextParser.Token.Twemoji("joy") })
        assertTrue(parsed.tokens.any { it == ChatRichTextParser.Token.Spoiler("剧透") })
        assertTrue(parsed.tokens.any { it == ChatRichTextParser.Token.InlineCode("code()") })
    }

    @Test
    fun controllerSendBoostUpdatesTargetMessageInPlace() = runBlocking {
        val initialMsg = ChatMessage(id = 5001L, groupId = 1L, body = "好活", createdAt = 1000L)
        val updatedWithBoost = initialMsg.copy(
            boosts = listOf(ChatBoost(username = "hitori", name = "后藤独", text = "甚好", at = 1005L, mine = true)),
        )
        val controller = ChatSessionController(
            fetchGroupsFn = { listOf(ChatGroup(id = 1L, name = "守矢神社")) },
            fetchMessagesFn = { _, _, _, _ -> ChatMessagesResponse(items = listOf(initialMsg)) },
            sendMessageFn = { _, _, _, _ -> Result.success(null) },
            markReadFn = { _, _ -> true },
            toggleReactionFn = { _, _ -> true },
            editMessageFn = { _, _ -> true },
            deleteMessageFn = { _ -> true },
            postBoostFn = { msgId, txt ->
                if (msgId == 5001L && txt == "甚好") Result.success(updatedWithBoost) else Result.failure(IllegalStateException("bad"))
            },
        )

        controller.pollOnce(1L)
        assertTrue(controller.uiState.value.messages.first().boosts.isEmpty())

        val res = controller.sendBoost(5001L, "甚好")
        assertTrue(res.isSuccess)
        val boosts = controller.uiState.value.messages.first().boosts
        assertEquals(1, boosts.size)
        assertEquals("甚好", boosts.first().text)
        assertTrue(boosts.first().mine)
    }
}
