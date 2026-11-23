package com.jinhuan.forhappy

import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.jinhuan.forhappy.core.chat.ChatRichTextParser
import com.jinhuan.forhappy.core.cosmetics.BadgeAndCosmeticRegistry
import com.jinhuan.forhappy.core.cosmetics.BadgeArtKind
import com.jinhuan.forhappy.core.cosmetics.BubbleOrnament
import com.jinhuan.forhappy.core.cosmetics.BubbleShapeKind
import com.jinhuan.forhappy.core.cosmetics.ChatBubbleCosmetics
import com.jinhuan.forhappy.core.model.ChatBoost
import com.jinhuan.forhappy.core.model.ChatUser
import com.jinhuan.forhappy.core.model.ForHappyJson
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatBubbleCosmeticsTest {

    @Test
    fun nicknameGradientRespectsAdminDefaultRainbowAndFancyGating() {
        val adminNullDeco = BadgeAndCosmeticRegistry.resolveEquippedIdentity(
            decoJson = null,
            where = "chat",
            isAdmin = true,
            hasFancyPermission = false,
        )
        assertEquals(BadgeAndCosmeticRegistry.ADMIN_RAINBOW_HEXES, adminNullDeco.nickGradientHexes)

        val grantedDefault = BadgeAndCosmeticRegistry.resolveEquippedIdentity(
            decoJson = buildJsonObject { put("t", JsonPrimitive("测试")) },
            where = "chat",
            isAdmin = false,
            hasFancyPermission = true,
        )
        assertEquals(BadgeAndCosmeticRegistry.DEFAULT_FANCY_RAINBOW_HEXES, grantedDefault.nickGradientHexes)

        val ungrantedWithCustomFc = BadgeAndCosmeticRegistry.resolveEquippedIdentity(
            decoJson = buildJsonObject {
                put("fc", JsonArray(listOf("#ff5fa2", "#c86bff").map { JsonPrimitive(it) }))
            },
            where = "chat",
            isAdmin = false,
            hasFancyPermission = false,
        )
        assertTrue(
            "Users without fancy permission must not render unauthorized custom fc gradients",
            ungrantedWithCustomFc.nickGradientHexes.isEmpty(),
        )

        val scopedOutCardStillKeepsFancyForGranted = BadgeAndCosmeticRegistry.resolveEquippedIdentity(
            decoJson = buildJsonObject {
                put("sc", JsonPrimitive("card"))
                put("t", JsonPrimitive("仅名片称号"))
            },
            where = "chat",
            isAdmin = false,
            hasFancyPermission = true,
        )
        assertNull(scopedOutCardStillKeepsFancyForGranted.title)
        assertEquals(BadgeAndCosmeticRegistry.DEFAULT_FANCY_RAINBOW_HEXES, scopedOutCardStillKeepsFancyForGranted.nickGradientHexes)
    }

    @Test
    fun raffleTrialExpiryAndCosmeticAccessAllMatchWebRules() {
        val now = 1_800_000_000L
        val activeTrial = buildJsonObject {
            put("ft", JsonPrimitive("claude"))
            put("fn", JsonPrimitive(800))
            put("fp", JsonPrimitive(2))
            put("fxp", JsonPrimitive(now + 3600))
        }
        val activeRes = BadgeAndCosmeticRegistry.resolveActiveRaffle(activeTrial, cosmeticAccess = null, nowSec = now)
        assertNotNull(activeRes)
        assertEquals(800, activeRes!!.earnedLevel)
        assertEquals(800, activeRes.displayLevel)

        val expiredWithPermanent = buildJsonObject {
            put("ft", JsonPrimitive("claude"))
            put("fn", JsonPrimitive(800))
            put("fp", JsonPrimitive(12))
            put("fxp", JsonPrimitive(now - 10))
        }
        val expiredFallback = BadgeAndCosmeticRegistry.resolveActiveRaffle(expiredWithPermanent, cosmeticAccess = null, nowSec = now)
        assertNotNull(expiredFallback)
        assertEquals(12, expiredFallback!!.displayLevel)

        val expiredNoPerm = buildJsonObject {
            put("ft", JsonPrimitive("claude"))
            put("fn", JsonPrimitive(800))
            put("fp", JsonPrimitive(0))
            put("fxp", JsonPrimitive(now - 10))
        }
        assertNull(BadgeAndCosmeticRegistry.resolveActiveRaffle(expiredNoPerm, cosmeticAccess = null, nowSec = now))

        val ownerAll = BadgeAndCosmeticRegistry.resolveActiveRaffle(expiredNoPerm, cosmeticAccess = "all", nowSec = now)
        assertNotNull(ownerAll)
        assertEquals(BadgeAndCosmeticRegistry.TITLE_MAX_LEVEL, ownerAll!!.earnedLevel)

        val shiguangIdentity = BadgeAndCosmeticRegistry.resolveEquippedIdentity(
            decoJson = activeTrial,
            where = "chat",
            nowSec = now,
        )
        assertEquals("拾光司书", shiguangIdentity.title?.label)
        assertEquals("shiguang", shiguangIdentity.title?.specialKind)
        val shiguangBubble = ChatBubbleCosmetics.resolveBubbleFromDeco(activeTrial, nowSec = now)
        assertTrue(shiguangBubble?.ornament is BubbleOrnament.ShiguangBook)
    }

    @Test
    fun owner52PersonalCommunityOfficialAndGrowthSuitsResolveCompletely() {
        val ownerDeco = buildJsonObject { put("pt", JsonPrimitive("owner-52di")) }
        val ownerId = BadgeAndCosmeticRegistry.resolveEquippedIdentity(ownerDeco, where = "chat")
        assertEquals("owner52", ownerId.title?.specialKind)
        assertTrue(ownerId.badge?.art is BadgeArtKind.SpriteSheetCell)
        val ownerBubble = ChatBubbleCosmetics.resolveBubbleFromDeco(ownerDeco, userKey = "u1", messageId = "m1")
        assertTrue(ownerBubble?.ornament is BubbleOrnament.Owner52)
        assertTrue(ChatBubbleCosmetics.contrastRatio(ownerBubble!!.textColor, ownerBubble.fillColor) >= 4.0f)

        val v1Deco = buildJsonObject { put("pt", JsonPrimitive("p001")) }
        val v1Id = BadgeAndCosmeticRegistry.resolveEquippedIdentity(v1Deco, where = "chat")
        assertTrue(v1Id.badge?.art is BadgeArtKind.RemoteSvg)
        val v1Bubble = ChatBubbleCosmetics.resolveBubbleFromDeco(v1Deco)
        assertTrue(v1Bubble?.ornament is BubbleOrnament.PersonalV1Mascot)

        val v2Deco = buildJsonObject { put("pt", JsonPrimitive("pv2-p001-ssr")) }
        val v2Id = BadgeAndCosmeticRegistry.resolveEquippedIdentity(v2Deco, where = "chat")
        assertEquals("personal_v2", v2Id.title?.specialKind)
        assertTrue(v2Id.badge?.art is BadgeArtKind.RemoteImage)
        val v2Bubble = ChatBubbleCosmetics.resolveBubbleFromDeco(v2Deco)
        assertTrue(v2Bubble?.ornament is BubbleOrnament.PersonalV2Mascot)
        assertNotNull(v2Bubble?.nineSliceSpec)

        val allInDeco = buildJsonObject {
            put(
                "community",
                buildJsonObject {
                    put("title", JsonPrimitive("laowu-dispatch-all-in"))
                    put("badge", JsonPrimitive("laowu-dispatch-all-in"))
                    put("bubble", JsonPrimitive("shadow-dao"))
                    put("reply", JsonPrimitive("shadow-path"))
                },
            )
        }
        val commId = BadgeAndCosmeticRegistry.resolveEquippedIdentity(allInDeco, where = "chat")
        assertTrue(commId.badge?.art is BadgeArtKind.SpriteSheetCell)
        val lightCommBubble = ChatBubbleCosmetics.resolveBubbleFromDeco(allInDeco, isDarkTheme = false)
        val darkCommBubble = ChatBubbleCosmetics.resolveBubbleFromDeco(allInDeco, isDarkTheme = true)
        assertTrue(lightCommBubble?.nineSliceSpec?.sourceUrl?.startsWith("https://") == true)
        assertTrue(darkCommBubble?.nineSliceSpec?.sourceUrl?.startsWith("community-css-dark:shadow-dao:") == true)
        val darkCommReply = ChatBubbleCosmetics.resolveBubbleFromDeco(allInDeco, frame = true, isDarkTheme = true)
        assertTrue(darkCommReply?.nineSliceSpec?.sourceUrl?.startsWith("community-css-dark:shadow-path:") == true)

        BadgeAndCosmeticRegistry.COMMUNITY_SKINS.keys.forEach { skinId ->
            val d = buildJsonObject {
                put(
                    "community",
                    buildJsonObject {
                        put("title", JsonPrimitive(skinId))
                        put("badge", JsonPrimitive(skinId))
                        put("bubble", JsonPrimitive(skinId))
                    },
                )
            }
            val resolved = BadgeAndCosmeticRegistry.resolveEquippedIdentity(d, where = "chat")
            assertNotNull("Community skin $skinId title must resolve", resolved.title)
            assertNotNull("Community skin $skinId badge must resolve", resolved.badge)
            val b = ChatBubbleCosmetics.resolveBubbleFromDeco(d, isDarkTheme = false)
            assertNotNull("Community skin $skinId bubble must resolve", b)
        }

        ChatBubbleCosmetics.OFFICIAL_PALETTES.keys.forEach { otId ->
            val d = buildJsonObject { put("ot", JsonPrimitive(otId)) }
            listOf(false, true).forEach { dark ->
                val b = ChatBubbleCosmetics.resolveBubbleFromDeco(d, isDarkTheme = dark)
                assertNotNull("Official $otId bubble missing", b)
                assertTrue(
                    "Official $otId contrast failed in dark=$dark",
                    ChatBubbleCosmetics.contrastRatio(b!!.textColor, b.fillColor) >= 4.0f,
                )
            }
        }

        listOf("guess_5", "luck_king_3", "luck_last_2", "rank_chat_count_all_4").forEach { grId ->
            val d = buildJsonObject { put("gr", JsonPrimitive(grId)) }
            val resolved = BadgeAndCosmeticRegistry.resolveEquippedIdentity(d, where = "chat")
            assertNotNull("Growth $grId title missing", resolved.title)
            val b = ChatBubbleCosmetics.resolveBubbleFromDeco(d)
            assertNotNull("Growth $grId bubble missing", b)
            assertTrue(ChatBubbleCosmetics.contrastRatio(b!!.textColor, b.fillColor) >= 4.0f)
        }
    }

    @Test
    fun allBubblePresetsPassWcagContrastAndCleanCssSanitizesDangerousRules() {
        val presets = BadgeAndCosmeticRegistry.BUBBLE_PRESETS.map { it.first }.filter { it.isNotBlank() } + "yinlu"
        presets.forEach { preset ->
            listOf(false, true).forEach { dark ->
                val style = ChatBubbleCosmetics.resolvePresetBubble(preset, isDarkTheme = dark)
                assertNotNull("Preset $preset must resolve", style)
                val ratio = ChatBubbleCosmetics.contrastRatio(style!!.textColor, style.fillColor)
                assertTrue(
                    "Preset $preset contrast ($ratio) too low in dark=$dark",
                    ratio >= 4.0f,
                )
            }
        }

        val cleaned = ChatBubbleCosmetics.cleanCss(
            "background-image: url(https://evil.example/x.png); position: fixed; z-index: 9999; color: #fffaaa; background: linear-gradient(135deg, #112233, #334455); border-radius: 18px;",
        )
        assertFalse(cleaned.contains("url("))
        assertFalse(cleaned.contains("fixed"))
        assertFalse(cleaned.contains("z-index"))
        assertTrue(cleaned.contains("linear-gradient"))
        assertTrue(cleaned.contains("border-radius"))

        val parsed = ChatBubbleCosmetics.parseSafeCss(cleaned)
        assertNotNull(parsed)
        assertNotNull(parsed!!.textColor)
        assertEquals(2, parsed.bgGradientColors?.size)
        assertEquals(18f, parsed.cornerRadiusDp)

        val customDeco = buildJsonObject {
            put("bb", JsonPrimitive("mint"))
            put("cssb", JsonPrimitive("color: #f8fafc; background: #0f172a; border-color: #38bdf8; border-width: 2px;"))
        }
        val chatUser = ChatUser(id = 42, username = "u42", deco = customDeco)
        val styled = ChatBubbleCosmetics.resolveChatBubbleStyle(chatUser, messageId = "100", isDarkTheme = false)
        assertNotNull(styled)
        assertTrue(ChatBubbleCosmetics.contrastRatio(styled!!.textColor, styled.fillColor) >= 4.0f)

        val boost = ChatBoost(
            userId = 42,
            text = "好",
            deco = ForHappyJson.parseToJsonElement("""{"community":{"reply":"shadow-dao"}}"""),
        )
        val boostStyle = ChatBubbleCosmetics.resolveBoostFrameStyle(boost, isDarkTheme = true)
        assertTrue(boostStyle?.nineSliceSpec?.sourceUrl?.startsWith("community-css-dark:shadow-dao:reply:") == true)
    }

    @Test
    fun whalebuddyAndRaffleNameplatesKeepHighContrastAcrossLightAndDarkThemes() {
        val whaleDeco = buildJsonObject {
            put("ft", JsonPrimitive("whalebuddy"))
            put("fn", JsonPrimitive(5200))
        }
        listOf(false, true).forEach { dark ->
            val identity = BadgeAndCosmeticRegistry.resolveEquippedIdentity(whaleDeco, where = "chat", isDarkTheme = dark)
            val title = identity.title
            assertNotNull(title)
            val titleBg = ChatBubbleCosmetics.parseHexColorPure(title!!.bgHex)
            val titleFg = ChatBubbleCosmetics.parseHexColorPure(title.textHex)
            assertTrue(
                "Whalebuddy nameplate contrast (${ChatBubbleCosmetics.contrastRatio(titleFg, titleBg)}) must be >= 4.5 in dark=$dark",
                ChatBubbleCosmetics.contrastRatio(titleFg, titleBg) >= 4.5f,
            )

            val bubble = ChatBubbleCosmetics.resolveBubbleFromDeco(whaleDeco, isDarkTheme = dark)
            assertNotNull(bubble)
            assertTrue(
                "Whalebuddy dark/light 9-slice bubble contrast (${ChatBubbleCosmetics.contrastRatio(bubble!!.textColor, bubble.fillColor)}) must be >= 4.5 in dark=$dark",
                ChatBubbleCosmetics.contrastRatio(bubble.textColor, bubble.fillColor) >= 4.5f,
            )
        }
    }

    @Test
    fun laowuCommunityBubblesWhalebuddyAndOwner52bMatchWebLayout() {
        val catSkins = listOf(
            "laowu-dispatch",
            "laowu-dispatch-all-in",
            "laowu-dispatch-black",
            "laowu-dispatch-calico",
            "laowu-dispatch-gray",
            "laowu-dispatch-siamese",
            "laowu-dispatch-white",
            "waibi-babu",
        )
        catSkins.forEach { skinId ->
            val d = buildJsonObject {
                put("community", buildJsonObject { put("bubble", JsonPrimitive(skinId)) })
            }
            val style = ChatBubbleCosmetics.resolveBubbleFromDeco(d, isDarkTheme = false)
            assertNotNull("Missing community bubble style for $skinId", style)
            assertEquals(BubbleShapeKind.Rounded(0f, 0f, 0f, 0f), style!!.shapeKind)
            assertTrue("$skinId minWidthDp must be >= 72", style.minWidthDp >= 72f)
            val slice = style.nineSliceSpec!!
            val leftPad = style.contentPadding.calculateLeftPadding(LayoutDirection.Ltr).value
            val rightPad = style.contentPadding.calculateRightPadding(LayoutDirection.Ltr).value
            assertTrue(
                "$skinId left pad ($leftPad) must clear cat/frame cap (${slice.borderLeftDp})",
                leftPad >= slice.borderLeftDp,
            )
            assertTrue(
                "$skinId right pad ($rightPad) must clear cat/frame cap (${slice.borderRightDp})",
                rightPad >= slice.borderRightDp,
            )
        }

        val shadowStyle = ChatBubbleCosmetics.resolveBubbleFromDeco(
            buildJsonObject {
                put("community", buildJsonObject { put("bubble", JsonPrimitive("shadow-dao")) })
            },
        )
        assertNotNull(shadowStyle)
        assertEquals(BubbleShapeKind.Rounded(0f, 0f, 0f, 0f), shadowStyle!!.shapeKind)
        assertTrue(shadowStyle.minWidthDp >= 72f)
        assertTrue(shadowStyle.contentPadding.calculateLeftPadding(LayoutDirection.Ltr).value >= 26f)

        val whaleStyle = ChatBubbleCosmetics.resolveBubbleFromDeco(
            buildJsonObject {
                put("ft", JsonPrimitive("whalebuddy"))
                put("fn", JsonPrimitive(5200))
            },
        )
        assertNotNull(whaleStyle)
        assertEquals("https://52dmeme.com/assets/raffle/whale-motion/body-still.webp", whaleStyle!!.nineSliceSpec?.sourceUrl)
        assertEquals(BubbleShapeKind.Rounded(0f, 0f, 0f, 0f), whaleStyle.shapeKind)
        assertEquals(105f, whaleStyle.minWidthDp, 0.01f)
        assertTrue(whaleStyle.ornament is BubbleOrnament.RaffleWhalebuddy)

        listOf("owner-52b", "owner-52di").forEach { ptId ->
            val id = BadgeAndCosmeticRegistry.resolveEquippedIdentity(
                decoJson = buildJsonObject { put("pt", JsonPrimitive(ptId)) },
                where = "card",
            )
            assertEquals("owner52", id.title?.specialKind)
            assertEquals("", id.badge?.levelText)
        }
    }

    @Test
    fun isStickerOnlyDetectsPureStickerMessagesAndRejectsMixedText() {
        assertTrue(ChatRichTextParser.isStickerOnly("[biaoqing:bocchi_01]"))
        assertTrue(ChatRichTextParser.isStickerOnly("  [pack:a]  [pack:b] "))
        assertFalse(ChatRichTextParser.isStickerOnly(""))
        assertFalse(ChatRichTextParser.isStickerOnly("哈哈 [biaoqing:bocchi_01]"))
        assertFalse(ChatRichTextParser.isStickerOnly("纯文本消息"))
    }
}
