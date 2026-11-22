package com.jinhuan.forhappy

import com.jinhuan.forhappy.core.chat.ChatRichTextParser
import com.jinhuan.forhappy.core.chat.ChatSessionController
import com.jinhuan.forhappy.core.model.ChatBoost
import com.jinhuan.forhappy.core.model.ChatGroup
import com.jinhuan.forhappy.core.model.ChatMessage
import com.jinhuan.forhappy.core.model.ChatMessagesResponse
import com.jinhuan.forhappy.core.model.ChatReaction
import com.jinhuan.forhappy.core.model.ChatReplyPreview
import com.jinhuan.forhappy.core.model.ChatUser
import com.jinhuan.forhappy.ui.chat.canGroupContinuation
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
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

    @Test
    fun changedItemsOutOfWindowNeverSplicedIntoLiveMessages() = runBlocking {
        val currentWindow = (1451L..1460L).map { id ->
            ChatMessage(id = id, groupId = 1L, body = "msg-$id", floor = id.toInt(), createdAt = id)
        }
        val replyReferencingOld = ChatMessage(
            id = 1461L,
            groupId = 1L,
            body = "replying",
            replyTo = ChatReplyPreview(id = 1100L, body = "old-before"),
            createdAt = 1461L,
        )
        val outOfWindowChanged = ChatMessage(
            id = 1100L,
            groupId = 1L,
            body = "old-reacted",
            reactions = listOf(ChatReaction(emoji = "❤️", count = 2)),
            boosts = listOf(ChatBoost(username = "a", text = "666")),
            createdAt = 1100L,
        )
        val inWindowChanged = ChatMessage(
            id = 1455L,
            groupId = 1L,
            body = "msg-1455-edited",
            edited = true,
            floor = null,
            createdAt = 1455L,
        )

        val controller = ChatSessionController(
            fetchGroupsFn = { emptyList() },
            fetchMessagesFn = { _, _, _, _ -> ChatMessagesResponse() },
            sendMessageFn = { _, _, _, _ -> Result.success(null) },
            markReadFn = { _, _ -> true },
            toggleReactionFn = { _, _ -> true },
            editMessageFn = { _, _ -> true },
            deleteMessageFn = { _ -> true },
        )

        val merged = controller.mergeServerDelta(
            current = currentWindow + replyReferencingOld,
            newItems = emptyList(),
            changedItems = listOf(outOfWindowChanged, inWindowChanged),
        )

        assertEquals((1451L..1461L).toList(), merged.map { it.id })
        assertFalse(merged.any { it.id == 1100L })
        val updated1455 = merged.first { it.id == 1455L }
        assertEquals("msg-1455-edited", updated1455.body)
        assertEquals(1455, updated1455.floor)

        // Applying an out-of-window reaction response also must not insert into timeline
        controller.seedCachedWindow(1L, merged)
        controller.applyUpdatedServerMessage(outOfWindowChanged)
        assertEquals((1451L..1461L).toList(), controller.uiState.value.messages.map { it.id })
    }

    @Test
    fun initialPollDisjointFromCacheReplacesStaleWindowWithoutGap() = runBlocking {
        val staleCached = (100L..120L).map { id ->
            ChatMessage(id = id, groupId = 1L, body = "stale-$id", createdAt = id)
        }
        val freshWindow = (200L..250L).map { id ->
            ChatMessage(id = id, groupId = 1L, body = "fresh-$id", createdAt = id)
        }
        val controller = ChatSessionController(
            fetchGroupsFn = { listOf(ChatGroup(id = 1L, name = "g1")) },
            fetchMessagesFn = { _, afterId, _, _ ->
                if (afterId == 0L) ChatMessagesResponse(items = freshWindow, more = true, now = 3000.0)
                else ChatMessagesResponse()
            },
            sendMessageFn = { _, _, _, _ -> Result.success(null) },
            markReadFn = { _, _ -> true },
            toggleReactionFn = { _, _ -> true },
            editMessageFn = { _, _ -> true },
            deleteMessageFn = { _ -> true },
        )

        controller.seedCachedWindow(1L, staleCached, hasMore = true)
        controller.pollOnce(1L)

        assertEquals((200L..250L).toList(), controller.uiState.value.messages.map { it.id })
        assertTrue(controller.uiState.value.hasMoreOlder)
    }

    @Test
    fun sendMessageDoesNotAdvanceCursorPastUnpolledPeerMessages() = runBlocking {
        val requestedAfters = mutableListOf<Long>()
        val controller = ChatSessionController(
            fetchGroupsFn = { listOf(ChatGroup(id = 1L, name = "g1")) },
            fetchMessagesFn = { _, afterId, _, _ ->
                requestedAfters += afterId
                when (afterId) {
                    0L -> ChatMessagesResponse(
                        items = (100L..101L).map { ChatMessage(id = it, groupId = 1L, body = "m$it", createdAt = it) },
                        more = false,
                        now = 1000.0,
                    )
                    101L -> ChatMessagesResponse(
                        items = listOf(
                            ChatMessage(id = 102L, groupId = 1L, body = "peer-102", createdAt = 102L),
                            ChatMessage(id = 103L, groupId = 1L, body = "peer-103", createdAt = 103L),
                            ChatMessage(id = 104L, groupId = 1L, body = "my-msg", mine = true, createdAt = 104L),
                        ),
                        more = false,
                        now = 1005.0,
                    )
                    else -> ChatMessagesResponse()
                }
            },
            sendMessageFn = { _, body, _, _ ->
                Result.success(ChatMessage(id = 104L, groupId = 1L, body = body, mine = true, createdAt = 104L))
            },
            markReadFn = { _, _ -> true },
            toggleReactionFn = { _, _ -> true },
            editMessageFn = { _, _ -> true },
            deleteMessageFn = { _ -> true },
        )

        controller.pollOnce(1L)
        assertEquals(listOf(100L, 101L), controller.uiState.value.messages.map { it.id })

        controller.sendOrEditMessage("my-msg")

        assertTrue(requestedAfters.contains(101L))
        assertEquals(listOf(100L, 101L, 102L, 103L, 104L), controller.uiState.value.messages.map { it.id })
    }

    @Test
    fun pollOnceCatchesUpMultiplePagesContiguously() = runBlocking {
        val controller = ChatSessionController(
            fetchGroupsFn = { listOf(ChatGroup(id = 1L, name = "g1")) },
            fetchMessagesFn = { _, afterId, _, _ ->
                when (afterId) {
                    0L -> ChatMessagesResponse(
                        items = listOf(ChatMessage(id = 10L, groupId = 1L, body = "m10", createdAt = 10L)),
                        more = false,
                        now = 1000.0,
                    )
                    10L -> ChatMessagesResponse(
                        items = (11L..13L).map { ChatMessage(id = it, groupId = 1L, body = "m$it", createdAt = it) },
                        more = true,
                        now = 1002.0,
                    )
                    13L -> ChatMessagesResponse(
                        items = (14L..16L).map { ChatMessage(id = it, groupId = 1L, body = "m$it", createdAt = it) },
                        more = false,
                        now = 1004.0,
                    )
                    else -> ChatMessagesResponse()
                }
            },
            sendMessageFn = { _, _, _, _ -> Result.success(null) },
            markReadFn = { _, _ -> true },
            toggleReactionFn = { _, _ -> true },
            editMessageFn = { _, _ -> true },
            deleteMessageFn = { _ -> true },
        )

        controller.pollOnce(1L)
        val moreAfterCatchup = controller.pollOnce(1L)

        assertFalse(moreAfterCatchup)
        assertEquals((10L..16L).toList(), controller.uiState.value.messages.map { it.id })
    }

    @Test
    fun setActiveGroupPreservesLoadedMessagesAcrossGroupSwitches() = runBlocking {
        val controller = ChatSessionController(
            fetchGroupsFn = {
                listOf(
                    ChatGroup(id = 1L, name = "g1"),
                    ChatGroup(id = 2L, name = "g2"),
                )
            },
            fetchMessagesFn = { gid, afterId, _, _ ->
                if (gid == 1L && afterId == 0L) {
                    ChatMessagesResponse(
                        items = (50L..55L).map { ChatMessage(id = it, groupId = 1L, body = "g1-$it", createdAt = it) },
                        more = true,
                    )
                } else if (gid == 2L && afterId == 0L) {
                    ChatMessagesResponse(
                        items = (200L..202L).map { ChatMessage(id = it, groupId = 2L, body = "g2-$it", createdAt = it) },
                        more = false,
                    )
                } else {
                    ChatMessagesResponse()
                }
            },
            sendMessageFn = { _, _, _, _ -> Result.success(null) },
            markReadFn = { _, _ -> true },
            toggleReactionFn = { _, _ -> true },
            editMessageFn = { _, _ -> true },
            deleteMessageFn = { _ -> true },
        )

        controller.pollOnce(1L)
        assertEquals((50L..55L).toList(), controller.uiState.value.messages.map { it.id })

        controller.setActiveGroup(2L)
        delay(120L)
        assertEquals(2L, controller.uiState.value.activeGroupId)
        assertEquals((200L..202L).toList(), controller.uiState.value.messages.map { it.id })

        controller.setActiveGroup(1L)
        delay(120L)
        assertEquals(1L, controller.uiState.value.activeGroupId)
        assertEquals((50L..55L).toList(), controller.uiState.value.messages.map { it.id })
    }

    @Test
    fun muscleStickerParsesAndPreviewSummaryNormalizesStickers() {
        val parsed = ChatRichTextParser.parse("[muscle:muscle_001] 可惜")
        assertTrue(parsed.tokens.first() == ChatRichTextParser.Token.Sticker("muscle", "muscle_001"))
        assertEquals(ChatRichTextParser.Token.PlainText(" 可惜"), parsed.tokens[1])

        val upperBracket = ChatRichTextParser.parse("[HTTP:200] OK")
        assertTrue(upperBracket.tokens.none { it is ChatRichTextParser.Token.Sticker })

        val summary = ChatRichTextParser.previewSummary("刚刚叫AI拉了一下happycoding的状态[alu:blush]")
        assertEquals("刚刚叫AI拉了一下happycoding的状态[表情]", summary)
    }

    @Test
    fun requestLoadOlderHistorySurvivesCallerCancellationAndResetsState() = runBlocking {
        val gate = CompletableDeferred<Unit>()
        val controller = ChatSessionController(
            fetchGroupsFn = { listOf(ChatGroup(id = 1L, name = "g1")) },
            fetchMessagesFn = { _, afterId, _, beforeId ->
                if (beforeId != null) {
                    gate.await()
                    ChatMessagesResponse(
                        items = (80L..89L).map { ChatMessage(id = it, groupId = 1L, body = "old-$it", createdAt = it) },
                        more = true,
                    )
                } else if (afterId == 0L) {
                    ChatMessagesResponse(
                        items = (90L..100L).map { ChatMessage(id = it, groupId = 1L, body = "new-$it", createdAt = it) },
                        more = true,
                    )
                } else {
                    ChatMessagesResponse()
                }
            },
            sendMessageFn = { _, _, _, _ -> Result.success(null) },
            markReadFn = { _, _ -> true },
            toggleReactionFn = { _, _ -> true },
            editMessageFn = { _, _ -> true },
            deleteMessageFn = { _ -> true },
        )

        controller.pollOnce(1L)
        assertTrue(controller.uiState.value.hasMoreOlder)
        assertEquals(90L, controller.uiState.value.messages.first().id)

        // Mimic Compose LaunchedEffect cancellation immediately after triggering load
        val ephemeralJob = launch {
            controller.requestLoadOlderHistory(1L)
        }
        ephemeralJob.join()
        ephemeralJob.cancel()

        withTimeout(1500L) {
            while (!controller.uiState.value.isLoadingHistory) delay(10L)
        }
        gate.complete(Unit)
        withTimeout(1500L) {
            while (controller.uiState.value.isLoadingHistory) delay(10L)
        }

        assertFalse(controller.uiState.value.isLoadingHistory)
        assertEquals(80L, controller.uiState.value.messages.first().id)
        assertEquals(21, controller.uiState.value.messages.size)
        assertTrue(controller.uiState.value.hasMoreOlder)
    }

    @Test
    fun loadOlderHistoryRecoversInFinallyAfterDirectCancellationAndClearsMoreOnEmptyResult() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val neverFinishes = CompletableDeferred<Unit>()
        var callCount = 0
        val controller = ChatSessionController(
            fetchGroupsFn = { listOf(ChatGroup(id = 1L, name = "g1")) },
            fetchMessagesFn = { _, _, _, beforeId ->
                if (beforeId != null) {
                    callCount++
                    if (callCount == 1) {
                        started.complete(Unit)
                        neverFinishes.await()
                    }
                    // Empty older result even if server says more=true must end pagination cleanly
                    ChatMessagesResponse(items = emptyList(), more = true)
                } else {
                    ChatMessagesResponse(
                        items = listOf(ChatMessage(id = 50L, groupId = 1L, body = "cur", createdAt = 50L)),
                        more = true,
                    )
                }
            },
            sendMessageFn = { _, _, _, _ -> Result.success(null) },
            markReadFn = { _, _ -> true },
            toggleReactionFn = { _, _ -> true },
            editMessageFn = { _, _ -> true },
            deleteMessageFn = { _ -> true },
        )

        controller.pollOnce(1L)
        val canceledJob = launch {
            controller.loadOlderHistory(1L)
        }
        started.await()
        assertTrue(controller.uiState.value.isLoadingHistory)
        canceledJob.cancel()
        canceledJob.join()

        // Must never stay stuck at isLoadingHistory = true
        assertFalse(controller.uiState.value.isLoadingHistory)

        val prepended = controller.loadOlderHistory(1L)
        assertEquals(0, prepended)
        assertFalse(controller.uiState.value.isLoadingHistory)
        assertFalse(controller.uiState.value.hasMoreOlder)
    }
}
