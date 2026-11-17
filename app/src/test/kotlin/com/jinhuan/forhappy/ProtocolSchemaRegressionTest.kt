package com.jinhuan.forhappy

import com.jinhuan.forhappy.core.clock.IsleClockRules
import com.jinhuan.forhappy.core.model.ChatMessage
import com.jinhuan.forhappy.core.model.ForHappyJson
import com.jinhuan.forhappy.core.model.GuessRoomOverviewResponse
import com.jinhuan.forhappy.core.model.MeUser
import com.jinhuan.forhappy.core.model.MemeDetailResponse
import com.jinhuan.forhappy.core.model.MemeItem
import com.jinhuan.forhappy.core.model.RustNetStatus
import com.jinhuan.forhappy.core.model.StickerPack
import com.jinhuan.forhappy.core.model.countReplyUnits
import com.jinhuan.forhappy.core.model.parseRedpackMeta
import kotlinx.serialization.builtins.ListSerializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtocolSchemaRegressionTest {

    @Test
    fun rustNetStatusDecodesStringLogsFromSelectorSnapshot() {
        val raw = """
            {
              "running": true,
              "port": 41867,
              "phase": "ready",
              "winnerIp": "104.16.1.1",
              "latencyMs": 52,
              "echAccepted": true,
              "echKeyCount": 1,
              "winnerSource": "vendor_doh",
              "lastError": null,
              "selectStateJson": "{\"v\":1}",
              "logs": [
                "select: probing 28 candidates (has_v6=false, ech_key=true)",
                "select: winner 104.16.1.1 (52 ms, ech=true, src=vendor_doh)"
              ]
            }
        """.trimIndent()

        val status = ForHappyJson.decodeFromString<RustNetStatus>(raw)
        assertTrue(status.running)
        assertEquals(41867, status.port)
        assertEquals("104.16.1.1", status.winnerIp)
        assertEquals(52, status.latencyMs)
        assertTrue(status.echAccepted)
        assertEquals(2, status.logs.size)
    }

    @Test
    fun memeDetailAndPuzzleDecodeExactSiteJsonFields() {
        val detailRaw = """
            {
              "meme": {
                "id": 12665,
                "title": "一张好图",
                "text": "补充文案",
                "image": "/img/fallback.webp",
                "thumb": "/img/thumb.webp",
                "images": [{"u": "/img/x.webp", "t": "/img/x_t.webp", "w": 800, "h": 600}],
                "like_count": 7,
                "ld_likes": 2,
                "fav_folders": [10],
                "comment_count": 3,
                "author_name": "shiratori",
                "author_nick": "白鸟",
                "puzzle": {
                  "id": 88,
                  "status": "open",
                  "reward_kind": "cdk",
                  "hint": "Base64试试",
                  "layer": 0,
                  "layer_count": 2,
                  "winners_count": 1,
                  "current": {
                    "idx": 0,
                    "kind": "cipher",
                    "prompt": "解开下一串字符",
                    "data": {"text": "SGVsbG8=", "algo": "base64"}
                  }
                }
              }
            }
        """.trimIndent()

        val detail = ForHappyJson.decodeFromString<MemeDetailResponse>(detailRaw)
        val meme: MemeItem? = detail.resolvedMeme
        assertNotNull(meme)
        assertEquals("/img/thumb.webp", meme!!.primaryImageUrl)
        assertEquals(listOf("/img/x.webp"), meme.detailImageUrls)
        assertEquals(9, meme.totalLikes)
        assertTrue(meme.isFaved)
        assertEquals("白鸟", meme.authorDisplay)
        assertEquals("Base64试试", meme.puzzle?.hint)
        assertEquals("SGVsbG8=", meme.puzzle?.current?.data?.text)
    }

    @Test
    fun chatMessageDecodesNestedUserReplyAndMediaFields() {
        val msgRaw = """
            {
              "id": 5001,
              "body": "[qq:1] 看看 No.12665",
              "created_at": 1794883500,
              "user": {
                "id": 9,
                "username": "nijika",
                "name": "伊地知虹夏",
                "avatar": "/img/av.webp"
              },
              "reply_to": {
                "id": 4999,
                "name": "结城理",
                "username": "makoto",
                "text": "刚才那条"
              },
              "media": [
                {"u": "/img/c.png", "w": 400, "h": 300}
              ]
            }
        """.trimIndent()

        val msg = ForHappyJson.decodeFromString<ChatMessage>(msgRaw)
        assertEquals("伊地知虹夏", msg.displayAuthor)
        assertEquals("nijika", msg.resolvedUsername)
        assertEquals("/img/av.webp", msg.resolvedAvatar)
        assertEquals("结城理", msg.replyTo?.resolvedAuthor)
        assertEquals("刚才那条", msg.replyTo?.resolvedText)
        assertEquals("/img/c.png", msg.allMedia.firstOrNull()?.resolvedUrl)
    }

    @Test
    fun isleCropStageDetectsMaturity() {
        val now = IsleClockRules.ISLE_LAUNCH + 10_000L
        val maturePlantedAt = now - 3600L // turnip growMin = 30 -> definitely >= 3
        val stage = IsleClockRules.cropStage(
            cropId = "turnip",
            plantedAt = maturePlantedAt,
            bonusSec = 0L,
            harvestsDone = 0,
            autoWatered = false,
            nowSec = now,
        )
        assertEquals(3, stage)
    }

    @Test
    fun stickerPacksSharingKeyProvideDistinctTabKeys() {
        val raw = """
            [
              {"key": "ld", "tab": "ld-bili", "label": "L站·B站", "ext": "png", "items": ["bili_001"]},
              {"key": "ld", "tab": "ld-tieba", "label": "L站·贴吧", "ext": "png", "items": ["tieba_001"]}
            ]
        """.trimIndent()
        val packs = ForHappyJson.decodeFromString(ListSerializer(StickerPack.serializer()), raw)
        assertEquals(2, packs.size)
        assertEquals("ld-bili", packs[0].tabKey)
        assertEquals("ld-tieba", packs[1].tabKey)
    }

    @Test
    fun redpackMetaParsesGuessDcoinAndRegularRedpacks() {
        val guessMsgRaw = """
            {
              "id": 8801,
              "body": "",
              "created_at": 1794885000,
              "redpack": {
                "prompt": "今天中午吃什么",
                "game": "ldc",
                "ldc_mode": "guess",
                "ldc_total": 5000,
                "payment_provider": "coin52",
                "ldc_answer_length": 4,
                "ldc_response_mode": "similarity"
              }
            }
        """.trimIndent()
        val guessMsg = ForHappyJson.decodeFromString<ChatMessage>(guessMsgRaw)
        val meta = guessMsg.parseRedpackMeta()
        assertNotNull(meta)
        assertTrue(meta!!.isGuessGame)
        assertTrue(meta.isDcoin)
        assertEquals("今天中午吃什么", meta.headline)
        assertTrue(meta.subline.contains("50 D币"))
        assertEquals("进入猜词专区", meta.actionLabel)

        val ldcMsgRaw = """
            {
              "id": 8802,
              "body": "",
              "created_at": 1794885100,
              "redpack": {
                "prompt": "周年庆拼手气",
                "game": "ldc",
                "ldc_mode": "random",
                "ldc_total": 888,
                "ldc_count": 10,
                "opened": 4
              }
            }
        """.trimIndent()
        val ldcMeta = ForHappyJson.decodeFromString<ChatMessage>(ldcMsgRaw).parseRedpackMeta()!!
        assertFalse(ldcMeta.isGuessGame)
        assertFalse(ldcMeta.isDcoin)
        assertTrue(ldcMeta.subline.contains("8.88 LDC"))
        assertEquals("LDC 红包", ldcMeta.typeBadge)
    }

    @Test
    fun guessRoomOverviewDecodesMixedNumericAndStringTotals() {
        val raw = """
            {
              "now": 1794886000.5,
              "round": {
                "message_id": 8801,
                "state": "open",
                "owner": false,
                "owner_name": "虹夏",
                "reward_type": "ldc",
                "total": 10.5,
                "currency": "LDC",
                "answer_length": 3,
                "ai_count": 2,
                "ai_limit": 20,
                "hints": ["是一种食物"]
              },
              "guesses": {
                "items": [
                  {
                    "id": 91,
                    "question": "拉面店",
                    "score": 0.67,
                    "character_matches": ["correct", "present", "absent"],
                    "user": { "id": 7, "username": "ryo", "name": "凉" }
                  }
                ]
              },
              "facts": { "items": [] },
              "negative": { "items": [] }
            }
        """.trimIndent()
        val parsed = ForHappyJson.decodeFromString<GuessRoomOverviewResponse>(raw)
        assertNotNull(parsed.round)
        assertEquals(8801L, parsed.round?.messageId)
        assertEquals("10.5", parsed.round?.totalText)
        assertEquals(listOf("correct", "present", "absent"), parsed.guesses.items.first().characterMatches)
        assertEquals(1, parsed.round?.hints?.size)
    }

    @Test
    fun chatBoostsRepliesAndMemeQuickRepliesDecodeAccurately() {
        val chatJson = """
            {
              "id": 9102,
              "body": "看看这个",
              "created_at": 1794887000,
              "reply_count": 3,
              "media_expired": true,
              "user": { "id": 42, "username": "ryo", "name": "山田凉", "deco": { "t": "贝斯手" } },
              "boosts": [
                { "username": "kita", "name": "喜多", "text": "甚好", "at": 1794887010, "mine": false },
                { "ai": true, "status": "done", "text": "很可能是拉面", "score": 78 }
              ],
              "reactions": [
                { "emoji": "ld:distorted_face", "count": 2, "mine": true, "who": [{ "id": 42, "username": "ryo", "name": "山田凉" }] }
              ]
            }
        """.trimIndent()
        val msg = ForHappyJson.decodeFromString<ChatMessage>(chatJson)
        assertEquals(42L, msg.effectiveUserId)
        assertEquals("贝斯手", msg.resolvedTitle)
        assertEquals(3, msg.replyCount)
        assertTrue(msg.mediaExpired)
        assertEquals(2, msg.boosts.size)
        assertEquals("甚好", msg.boosts[0].text)
        assertTrue(msg.boosts[1].ai)
        assertEquals(78, msg.boosts[1].score)
        assertEquals("ld:distorted_face", msg.reactions.first().emoji)
        assertEquals(1, msg.reactions.first().who.size)

        val memeJson = """
            {
              "id": 12668,
              "title": "草图",
              "reply_count": 2,
              "my_reply": "太草了[ld:tieba_087]",
              "reactions": [{ "emoji": "ld:tieba_087", "count": 4, "mine": false }],
              "reply_list": [
                { "username": "nijika", "name": "虹夏", "text": "666", "n": 2, "mine": false }
              ]
            }
        """.trimIndent()
        val meme = ForHappyJson.decodeFromString<MemeItem>(memeJson)
        val me = MeUser(id = 1L, username = "hitori", name = "后藤独")
        val mergedReplies = meme.effectiveQuickReplies(me)
        assertEquals(2, mergedReplies.size)
        assertTrue(mergedReplies.last().mine)
        assertEquals("太草了[ld:tieba_087]", mergedReplies.last().text)
        assertEquals(3, countReplyUnits("好图[ld:tieba_087]"))
    }
}
