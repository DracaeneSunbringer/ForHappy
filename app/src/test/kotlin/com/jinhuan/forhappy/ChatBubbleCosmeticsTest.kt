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
        assertTrue(commId.badge?.art is BadgeArtKind.CommunityCssBadge)
        val preLightBubble = ChatBubbleCosmetics.resolveBubbleFromDeco(allInDeco, isDarkTheme = false)
        assertTrue(preLightBubble?.nineSliceSpec?.sourceUrl?.startsWith("https://") == true)
        assertNotNull(preLightBubble?.dynamicCssSpec)

        val shadowDaoCss = """
            .cskin-bubble-shadow-dao {
              border-image-source: url("data:image/webp;base64,LIGHTDAO");
              border-image-slice: 42 60 48 64 fill;
              border-image-width: 22px 34px 24px 36px;
              padding: 16px 26px 18px 26px;
              color: #141418;
            }
            [data-theme="dark"] .cskin-bubble-shadow-dao {
              border-image-source: url("data:image/webp;base64,DARKDAO");
              color: #f3efe6;
            }
        """.trimIndent()
        val shadowPathCss = """
            .cskin-reply-shadow-path {
              border-image-source: url("data:image/webp;base64,LIGHTPATH");
              border-image-slice: 18 56 18 56 fill;
              border-image-width: 9px 22px 8px 22px;
              color: #0f1418;
            }
            [data-theme="dark"] .cskin-reply-shadow-path {
              border-image-source: url("data:image/webp;base64,DARKPATH");
              color: #f2f7f8;
            }
        """.trimIndent()
        val daoSha = BadgeAndCosmeticRegistry.resolveCommunitySkinMeta("shadow-dao")!!.bubbleCssSha!!
        val pathSha = BadgeAndCosmeticRegistry.resolveCommunitySkinMeta("shadow-path")!!.replyCssSha!!
        assertTrue(ChatBubbleCosmetics.registerParsedCommunityCss("bubble", daoSha, shadowDaoCss))
        assertTrue(ChatBubbleCosmetics.registerParsedCommunityCss("reply", pathSha, shadowPathCss))

        val lightCommBubble = ChatBubbleCosmetics.resolveBubbleFromDeco(allInDeco, isDarkTheme = false)
        val darkCommBubble = ChatBubbleCosmetics.resolveBubbleFromDeco(allInDeco, isDarkTheme = true)
        assertTrue(lightCommBubble?.nineSliceSpec?.sourceUrl?.startsWith("https://") == true)
        assertEquals(
            "community-css:shadow-dao:bubble:$daoSha:1",
            darkCommBubble?.nineSliceSpec?.sourceUrl,
        )
        val darkCommReply = ChatBubbleCosmetics.resolveBubbleFromDeco(allInDeco, frame = true, isDarkTheme = true)
        assertEquals(
            "community-css:shadow-path:reply:$pathSha:1",
            darkCommReply?.nineSliceSpec?.sourceUrl,
        )

        BadgeAndCosmeticRegistry.COMMUNITY_SKINS.forEach { (skinId, skin) ->
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
            if (skin.hasTitle) {
                assertNotNull("Community skin $skinId title must resolve", resolved.title)
            } else {
                org.junit.Assert.assertNull("Community skin $skinId title must be null when hasTitle=false", resolved.title)
            }
            if (skin.hasBadge) {
                assertNotNull("Community skin $skinId badge must resolve", resolved.badge)
            } else {
                org.junit.Assert.assertNull("Community skin $skinId badge must be null when hasBadge=false", resolved.badge)
            }
            val b = ChatBubbleCosmetics.resolveBubbleFromDeco(d, isDarkTheme = false)
            if (skin.hasBubble) {
                assertNotNull("Community skin $skinId bubble must resolve", b)
            } else {
                org.junit.Assert.assertNull("Community skin $skinId bubble must be null when hasBubble=false", b)
            }
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
        val boostReplySha = BadgeAndCosmeticRegistry.resolveCommunitySkinMeta("shadow-dao")!!.replyCssSha!!
        ChatBubbleCosmetics.registerParsedCommunityCss(
            "reply",
            boostReplySha,
            """
            .cskin-reply-shadow-dao {
              border-image-source: url("data:image/webp;base64,LIGHTBOOST");
              border-image-slice: 18 50 20 56 fill;
              border-image-width: 8px 20px 9px 22px;
              color: #141418;
            }
            [data-theme="dark"] .cskin-reply-shadow-dao {
              border-image-source: url("data:image/webp;base64,DARKBOOST");
              color: #f3efe6;
            }
            """.trimIndent(),
        )
        val boostStyle = ChatBubbleCosmetics.resolveBoostFrameStyle(boost, isDarkTheme = true)
        assertEquals("community-css:shadow-dao:reply:$boostReplySha:1", boostStyle?.nineSliceSpec?.sourceUrl)
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
            val skinMeta = BadgeAndCosmeticRegistry.resolveCommunitySkinMeta(skinId)!!
            val d = buildJsonObject {
                put("community", buildJsonObject { put("bubble", JsonPrimitive(skinId)) })
            }
            val preStyle = ChatBubbleCosmetics.resolveBubbleFromDeco(d, isDarkTheme = false)
            assertNotNull("Missing community bubble style for $skinId", preStyle)
            if (preStyle!!.nineSliceSpec == null) {
                assertNotNull("Expected dynamicCssSpec while CSS unloaded for $skinId", preStyle.dynamicCssSpec)
            }

            val cssSha = skinMeta.bubbleCssSha
            if (!cssSha.isNullOrBlank()) {
                val (bW, bR, pL, pR, minW) = when (skinId) {
                    "laowu-dispatch-black" -> listOf(31f, 37f, 21f, 21f, 112f)
                    "laowu-dispatch-siamese" -> listOf(34f, 48f, 123f, 25f, 205f)
                    "laowu-dispatch-white" -> listOf(42f, 57f, 36f, 43f, 119f)
                    else -> listOf(26f, 28f, 29f, 31f, 72f)
                }
                val css = """
                    .cskin-bubble-$skinId {
                      border-image-source: url("data:image/webp;base64,CAT");
                      border-image-slice: 42 43 40 36 fill;
                      border-image-width: 30px ${bR}px 30px ${bW}px;
                      padding: 16px ${pR}px 16px ${pL}px;
                      min-width: ${minW}px;
                      color: #302318;
                    }
                """.trimIndent()
                ChatBubbleCosmetics.registerParsedCommunityCss("bubble", cssSha, css)
                val style = ChatBubbleCosmetics.resolveBubbleFromDeco(d, isDarkTheme = false)!!
                assertEquals(BubbleShapeKind.Rounded(0f, 0f, 0f, 0f), style.shapeKind)
                assertNotNull("Missing nine-slice after CSS load for $skinId", style.nineSliceSpec)
                assertTrue("$skinId minWidthDp (${style.minWidthDp}) must be >= $minW", style.minWidthDp >= minW)
                val leftPad = style.contentPadding.calculateLeftPadding(LayoutDirection.Ltr).value
                val rightPad = style.contentPadding.calculateRightPadding(LayoutDirection.Ltr).value
                assertTrue("$skinId left pad ($leftPad) must match CSS ($pL)", leftPad >= pL)
                assertTrue("$skinId right pad ($rightPad) must match CSS ($pR)", rightPad >= pR)
            }
        }

        listOf("taike-lili", "step-by-step-bird").forEach { skinId ->
            val skinMeta = BadgeAndCosmeticRegistry.resolveCommunitySkinMeta(skinId)!!
            val d = buildJsonObject {
                put("community", buildJsonObject {
                    put("title", JsonPrimitive(skinId))
                    put("badge", JsonPrimitive(skinId))
                    put("bubble", JsonPrimitive(skinId))
                })
            }
            val titleCss = """
                .cskin-title-$skinId {
                  background: linear-gradient(135deg, #4c1d95, #7e22ce);
                  color: #ffffff;
                  padding-inline: 22px 10px;
                }
                .cskin-title-${skinId}::before {
                  content: "";
                  position: absolute;
                  left: 4px;
                  top: 50%;
                  width: 14px;
                  height: 14px;
                  transform: translateY(-50%);
                  background: url("data:image/webp;base64,ICO") center / contain no-repeat;
                }
            """.trimIndent()
            val bubbleCss = """
                .cskin-bubble-$skinId {
                  border-image-source: url("data:image/webp;base64,BUB");
                  border-image-slice: 45 49 43 47 fill;
                  border-image-width: 24px 33px 20px 32px;
                  padding: 22px 31px 17px 30px;
                  min-width: 96px;
                  color: #ffffff;
                }
            """.trimIndent()
            skinMeta.titleCssSha?.let { ChatBubbleCosmetics.registerParsedCommunityCss("title", it, titleCss) }
            skinMeta.bubbleCssSha?.let { ChatBubbleCosmetics.registerParsedCommunityCss("bubble", it, bubbleCss) }
            val identity = BadgeAndCosmeticRegistry.resolveEquippedIdentity(d, where = "chat")
            assertNotNull("$skinId title missing", identity.title)
            assertTrue(
                "$skinId title must have leadingIconUrl or pseudoLayers after CSS registration",
                identity.title?.leadingIconUrl != null || identity.title?.communitySpec?.pseudoLayers?.isNotEmpty() == true,
            )
            assertNotNull("$skinId badge missing", identity.badge)
            val bStyle = ChatBubbleCosmetics.resolveBubbleFromDeco(d)
            assertNotNull("$skinId bubble missing", bStyle)
            assertTrue("$skinId minWidthDp must be >= 90", bStyle!!.minWidthDp >= 90f)
            assertNotNull("$skinId nineSliceSpec missing", bStyle.nineSliceSpec)
            assertTrue(ChatBubbleCosmetics.contrastRatio(bStyle.textColor, bStyle.fillColor) >= 4.0f)
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
        val whaleTopPad = whaleStyle.contentPadding.calculateTopPadding().value
        assertTrue(
            "whalebuddy top contentPadding ($whaleTopPad) must clear cap (${whaleStyle.nineSliceSpec!!.borderTopDp})",
            whaleTopPad >= whaleStyle.nineSliceSpec!!.borderTopDp,
        )
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
    fun communityCssUrlExtractionAndDynamicSkinParserHandleSvgWithParentheses() {
        val sampleBubbleCss = """
            .cskin-bubble-future-demo {
              --cap: 30px;
              min-inline-size: min(110px, 100%);
              padding: 23px 32px 25px 31px !important;
              border: 0 solid transparent;
              border-image-source: url("data:image/svg+xml,%3Csvg%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%3E%3Cellipse%20transform%3D%22rotate(36%2029%2034.2)%22%20fill%3D%22url(%23eye)%22%2F%3E%3C%2Fsvg%3E");
              border-image-slice: 37 41 37 39 fill;
              border-image-width: 33px 36.5px 33px 34.5px;
              color: #fff9ff !important;
            }
        """.trimIndent()
        val urls = ChatBubbleCosmetics.extractCssImageUrls(sampleBubbleCss)
        assertEquals(1, urls.size)
        assertTrue(urls[0].endsWith("%3C%2Fsvg%3E"))

        val parsedBubble = ChatBubbleCosmetics.parseCommunityBubbleCss(sampleBubbleCss)
        assertNotNull(parsedBubble)
        assertTrue(parsedBubble!!.hasImage)
        assertEquals(37f, parsedBubble.sliceTop, 0.01f)
        assertEquals(41f, parsedBubble.sliceRight, 0.01f)
        assertEquals(36.5f, parsedBubble.borderRightDp, 0.01f)
        assertEquals(23f, parsedBubble.padTopDp, 0.01f)
        assertEquals(31f, parsedBubble.padLeftDp, 0.01f)
        assertEquals(110f, parsedBubble.minWidthDp, 0.01f)

        val sampleTitleCss = """
            .cskin-title-future-demo {
              border: 1px solid #46204f;
              border-radius: 7px 12px 9px 10px;
              background: url("data:image/svg+xml,%3Csvg/%3E") left 4px center/15px 13px no-repeat, linear-gradient(150deg,#a84bc1,#702786 62%,#522263);
            }
            .cskin-title-future-demo::after {
              content: "未来新装";
              color: #fff4fe;
            }
            [data-theme="dark"] .cskin-title-future-demo { border-color: #b570c6; }
        """.trimIndent()
        val parsedTitle = ChatBubbleCosmetics.parseCommunityTitleCss(sampleTitleCss)
        assertNotNull(parsedTitle)
        assertEquals("#a84bc1", parsedTitle!!.bgHex)
        assertEquals("#522263", parsedTitle.secondaryBgHex)
        assertEquals("#fff4fe", parsedTitle.textHex)
        assertEquals("#46204f", parsedTitle.borderHex)
        assertEquals("#b570c6", parsedTitle.darkBorderHex)
        assertEquals("grad", parsedTitle.fillMode)
        assertEquals(listOf(7f, 12f, 9f, 10f), parsedTitle.cornerRadiiDp)
        assertEquals(0, parsedTitle.leadingIconIdx)
        assertEquals("未来新装", parsedTitle.overrideLabel)
    }

    @Test
    fun liveMultiLayerCommunityBubbleCssParsesOutsetAndPseudoElements() {
        val taikeCss = """
            .cskin-bubble-future-taike {
              inline-size: fit-content; min-inline-size: min(108px, 100%);
              padding: 23px 32px 25px 31px !important;
              border: 0 solid transparent;
              border-image-source: url("data:image/webp;base64,AAAA");
              border-image-slice: 45 49 43 47 fill;
              border-image-width: 40.135px 43.622px 38.351px 41.577px;
              border-image-outset: 7.135px 7.122px 5.351px 7.077px;
              color: #fff9ff !important;
            }
            .cskin-bubble-future-taike::before {
              left: 0; width: 30.077px; top: 0; bottom: 0;
              border-image-slice: 45 0 43 0 fill;
              border-image-width: 40.135px 0 38.351px 0;
              border-image-outset: 7.135px 0 5.351px 7.077px;
              border-image-source: var(--left-rest);
              --left-rest: url("data:image/webp;base64,BBBB");
            }
            @keyframes tentacle-left {
              0% { border-image-source: var(--left-rest); }
            }
            .cskin-bubble-future-taike::after {
              right: 0; width: 30.268px; top: 0; bottom: 0;
              border-image-slice: 45 0 43 0 fill;
              border-image-width: 40.135px 0 38.351px 0;
              border-image-outset: 7.135px 7.122px 5.351px 0;
              border-image-source: var(--right-rest);
              --right-rest: url("data:image/webp;base64,CCCC");
            }
        """.trimIndent()
        val urls = ChatBubbleCosmetics.extractCssImageUrls(taikeCss)
        assertEquals(3, urls.size)

        val parsedTaike = ChatBubbleCosmetics.parseCommunityBubbleCss(taikeCss)!!
        assertEquals(45f, parsedTaike.sliceTop, 0.01f)
        assertEquals(47f, parsedTaike.sliceLeft, 0.01f)
        assertEquals(7.135f, parsedTaike.outsetTopDp, 0.01f)
        assertEquals(7.077f, parsedTaike.outsetLeftDp, 0.01f)
        assertEquals(2, parsedTaike.pseudoLayers.size)
        assertEquals(1, parsedTaike.pseudoLayers[0].nineSlice?.urlIdx)
        assertEquals(30.077f, parsedTaike.pseudoLayers[0].widthDp!!, 0.01f)
        assertEquals(2, parsedTaike.pseudoLayers[1].nineSlice?.urlIdx)
        assertEquals(30.268f, parsedTaike.pseudoLayers[1].widthDp!!, 0.01f)

        val whiteCss = """
            .cskin-bubble-future-white {
              min-inline-size: min(119px, 100%);
              max-inline-size: calc(100% - 19px);
              padding: 32px 43px 25px 36px !important;
              border: 0 solid transparent;
              border-image-source: url("data:image/webp;base64,BASE");
              border-image-slice: 50 90 49 66 fill;
              border-image-width: 32px 57px 31px 42px;
              color: #20201d !important;
            }
            @property --look-x { syntax: "<length>"; inherits: false; initial-value: 0px; }
            @property --look-y { syntax: "<length>"; inherits: false; initial-value: 0px; }
            .cskin-bubble-future-white::before {
              --foot-home: url("data:image/webp;base64,FOOT");
              inset: 0; border: 0 solid transparent;
              border-image-source: var(--foot-home);
              border-image-slice: 50 90 49 66 fill;
              border-image-width: 32px 57px 31px 42px;
              background-image: url("data:image/webp;base64,SHAD");
              background-position: center bottom 1px;
              background-size: calc(100% - 43px) 13.3px;
            }
            .cskin-bubble-future-white::after {
              --head-0: url("data:image/webp;base64,HEAD");
              --pupil: url("data:image/webp;base64,PUPIL");
              --reach: 0px;
              top: 0; right: -19px; width: 76px; height: 32px;
              background-image: var(--pupil), var(--head-0);
              background-size: 8.8667px 8.32px, 100% 100%;
              background-position: calc(17.7333px + var(--reach) + var(--look-x)) calc(11.52px + var(--look-y)), 0px 0px;
            }
        """.trimIndent()
        val parsedWhite = ChatBubbleCosmetics.parseCommunityBubbleCss(whiteCss)!!
        assertEquals(19f, parsedWhite.endOverflowReserveDp, 0.01f)
        assertEquals(2, parsedWhite.pseudoLayers.size)

        val beforeLayer = parsedWhite.pseudoLayers[0]
        assertEquals(1, beforeLayer.nineSlice?.urlIdx)
        assertEquals(1, beforeLayer.bgImagesBottomToTop.size)
        val shadowBg = beforeLayer.bgImagesBottomToTop[0]
        assertEquals(2, shadowBg.urlIdx)
        assertEquals(43f, shadowBg.widthSubFromFullDp!!, 0.01f)
        assertEquals(13.3f, shadowBg.heightDp!!, 0.01f)
        assertTrue(shadowBg.alignCenterX)
        assertEquals(1f, shadowBg.offsetBottomDp!!, 0.01f)

        val afterLayer = parsedWhite.pseudoLayers[1]
        assertEquals(-19f, afterLayer.rightDp!!, 0.01f)
        assertEquals(76f, afterLayer.widthDp!!, 0.01f)
        assertEquals(32f, afterLayer.heightDp!!, 0.01f)
        assertEquals(2, afterLayer.bgImagesBottomToTop.size)
        // Reversed bottom-to-top: head-0 (urlIdx 3) first, pupil (urlIdx 4) on top
        assertEquals(3, afterLayer.bgImagesBottomToTop[0].urlIdx)
        assertEquals(1f, afterLayer.bgImagesBottomToTop[0].widthFraction!!, 0.01f)
        assertEquals(4, afterLayer.bgImagesBottomToTop[1].urlIdx)
        assertEquals(17.7333f, afterLayer.bgImagesBottomToTop[1].offsetXDp, 0.01f)
        assertEquals(11.52f, afterLayer.bgImagesBottomToTop[1].offsetYDp, 0.01f)
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
