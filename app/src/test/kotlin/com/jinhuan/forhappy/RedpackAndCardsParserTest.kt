package com.jinhuan.forhappy

import com.jinhuan.forhappy.core.chat.ChatRichTextParser
import com.jinhuan.forhappy.core.model.parseOwner52bPacksJson
import com.jinhuan.forhappy.core.model.parseRedpackArchivePageJson
import com.jinhuan.forhappy.core.model.parseSenderStatsJson
import com.jinhuan.forhappy.ui.chat.formatPacketCurrencyAmount
import com.jinhuan.forhappy.ui.components.estimateChatBodyVisualLines
import com.jinhuan.forhappy.ui.components.shouldCollapseChatBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RedpackAndCardsParserTest {

    @Test
    fun parseOwner52bPacksJson_handlesPacksAndEmptyInput() {
        val raw = """
            {
              "packs": [
                {
                  "id": "pack_classic",
                  "name": "52B 经典收藏",
                  "owned": 2,
                  "total": 4,
                  "cards": [
                    {
                      "id": "c1",
                      "name": "波奇酱",
                      "rarity": "SSR",
                      "description": "社恐但会弹吉他",
                      "asset": "/cards/c1.webp",
                      "back": "/cards/back.webp",
                      "owned": true,
                      "count": 3,
                      "first_at": 1731000000
                    },
                    {
                      "id": "c2",
                      "name": "大肥鱼",
                      "rarity": "R",
                      "owned": false,
                      "count": 0
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val packs = parseOwner52bPacksJson(raw)
        assertEquals(1, packs.size)
        val p = packs.first()
        assertEquals("pack_classic", p.id)
        assertEquals("52B 经典收藏", p.name)
        assertEquals(2, p.owned)
        assertEquals(4, p.total)
        assertEquals(2, p.cards.size)
        assertEquals("SSR", p.cards[0].tier)
        assertEquals("社恐但会弹吉他", p.cards[0].desc)
        assertTrue(p.cards[0].owned)
        assertEquals(3, p.cards[0].count)
        assertFalse(p.cards[1].owned)

        assertTrue(parseOwner52bPacksJson("not-json").isEmpty())
        assertTrue(parseOwner52bPacksJson("""{"packs":[]}""").isEmpty())
    }

    @Test
    fun parseRedpackArchivePageJson_parsesAllProvidersAndDeletedCorrectly() {
        val raw = """
            {
              "more": true,
              "next": "cursor_2",
              "items": [
                {
                  "id": 1001,
                  "group_id": 2,
                  "body": "",
                  "created_at": 1732000001,
                  "user": {"id": 10, "name": "博丽灵梦", "username": "reimu", "avatar": "/a/1.png"},
                  "redpack": {
                    "provider": "coin52",
                    "prompt": "赛钱箱分红",
                    "coin52": {"state": "active", "total": 5000, "count": 5, "remaining_count": 2, "mine": 500}
                  }
                },
                {
                  "id": 1002,
                  "group_id": 2,
                  "body": "",
                  "created_at": 1732000002,
                  "user": {"id": 11, "name": "东风谷早苗", "username": "sanae"},
                  "redpack": {
                    "provider": "native",
                    "game": "ldc",
                    "ldc_mode": "open",
                    "prompt": "守矢神社福利",
                    "ldc": {"state": "ended", "total": 1000, "count": 4, "claimed_count": 4}
                  }
                },
                {
                  "id": 1003,
                  "group_id": 2,
                  "body": "",
                  "created_at": 1732000003,
                  "user": {"id": 12, "name": "外链老板", "username": "boss"},
                  "redpack": {
                    "provider": "hb",
                    "prompt": "外部拼手气",
                    "snapshot": {"state": "active", "total": 5, "claimed_count": 2, "claimed": false}
                  }
                },
                {
                  "id": 1004,
                  "group_id": 2,
                  "body": "",
                  "deleted": true,
                  "created_at": 1732000004,
                  "user": {"id": 13, "name": "撤回君", "username": "revoker"},
                  "redpack": {
                    "provider": "native",
                    "mode": "phrase",
                    "prompt": "复读口令测试",
                    "opened": 3
                  }
                }
              ]
            }
        """.trimIndent()

        val page = parseRedpackArchivePageJson(raw, view = "history")
        assertTrue(page.more)
        assertEquals("cursor_2", page.next)
        assertEquals(4, page.items.size)

        val coin52Item = page.items[0]
        assertEquals(1001L, coin52Item.messageId)
        assertEquals("博丽灵梦", coin52Item.senderName)
        assertEquals("D币红包", coin52Item.channelLabel)
        assertEquals("已领取", coin52Item.stateLabel)
        assertEquals("50 D币", coin52Item.amountText)
        assertEquals("已领 3/5 份", coin52Item.progressText)

        val ldcItem = page.items[1]
        assertEquals("本站 LDC", ldcItem.channelLabel)
        assertEquals("已结束", ldcItem.stateLabel)
        assertEquals("10.00 LDC", ldcItem.amountText)

        // External packet snapshot.total is packet count, never LDC currency
        val extItem = page.items[2]
        assertEquals("HB 红包", extItem.channelLabel)
        assertEquals("可领取", extItem.stateLabel)
        assertEquals("", extItem.amountText)
        assertEquals("已领 2/5 份", extItem.progressText)

        val delItem = page.items[3]
        assertTrue(delItem.deleted)
        assertEquals("已撤回", delItem.stateLabel)
    }

    @Test
    fun parseSenderStatsJson_handlesCountsAndAmounts() {
        val rawCount = """
            {
              "today": "2026-11-22",
              "mine": {"value": 3},
              "mineRank": 2,
              "leaders": [
                {"user_id": 1, "name": "灵梦", "username": "reimu", "value": 8},
                {"user_id": 2, "name": "早苗", "username": "sanae", "value": 3}
              ]
            }
        """.trimIndent()
        val statsCount = parseSenderStatsJson(rawCount, metric = "count")
        assertEquals("2026-11-22", statsCount.today)
        assertEquals("3 次", statsCount.mineValueText)
        assertEquals(2, statsCount.mineRank)
        assertEquals("8 次", statsCount.leaders[0].valueText)

        val rawPaid = """
            {
              "mine": {"value": 0},
              "mineRank": 0,
              "leaders": [{"id": 1, "username": "reimu", "value": 1250}]
            }
        """.trimIndent()
        val statsPaid = parseSenderStatsJson(rawPaid, metric = "paid")
        assertEquals("0.00 LDC", statsPaid.mineValueText)
        assertNull(statsPaid.mineRank)
        assertEquals("12.50 LDC", statsPaid.leaders[0].valueText)
    }

    @Test
    fun formatPacketCurrencyAmount_normalizesDcoinIntegersAndPreservesLdc() {
        assertEquals("200000 D币", formatPacketCurrencyAmount("200000.00", "D币"))
        assertEquals("7518 D币", formatPacketCurrencyAmount("7518.00", "D币"))
        assertEquals("12.50 LDC", formatPacketCurrencyAmount("12.50", "LDC"))
    }

    @Test
    fun longChatBodyCollapsesWhileShortAndStickerMessagesStayExpanded() {
        val shortParsed = ChatRichTextParser.parse("早上好，今天天气真不错")
        assertFalse(shouldCollapseChatBody(shortParsed, maxLines = 8))

        val stickerParsed = ChatRichTextParser.parse("[dsh:happy][dsh:love]")
        assertFalse(shouldCollapseChatBody(stickerParsed, maxLines = 8))

        val tenLines = (1..10).joinToString("\n") { "第 $it 行长文本消息内容" }
        val longLinesParsed = ChatRichTextParser.parse(tenLines)
        assertTrue(estimateChatBodyVisualLines(longLinesParsed) >= 10)
        assertTrue(shouldCollapseChatBody(longLinesParsed, maxLines = 8))

        val longCode = "```\n" + (1..12).joinToString("\n") { "val item$it = computeSomething($it)" } + "\n```"
        val codeParsed = ChatRichTextParser.parse(longCode)
        assertTrue(shouldCollapseChatBody(codeParsed, maxLines = 8))
    }
}
