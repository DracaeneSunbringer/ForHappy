package com.jinhuan.forhappy.core.cosmetics

import com.jinhuan.forhappy.core.model.DecoConfig
import com.jinhuan.forhappy.core.model.GrowthRewardMeta
import com.jinhuan.forhappy.core.model.MeResponse
import com.jinhuan.forhappy.core.model.OfficialTitleMeta
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CosmeticsDynamicSyncAndRenderTest {

    @Test
    fun parseAndApplyWebModulesDecodesUnicodeAndUpdatesAllCatalogs() {
        val fakeTitleCollectionJs = """
            var rows = [
              ["claude", "\u62FE\u5149\u53F8\u4E66", "\u6BCF\u6B21\u76F8\u9047\uFF0C\u90FD\u7559\u6709\u5149\u8292", "flower", "#b9824e", "#efcfa1", "SSR"],
              ["starlight_sync_test", "\u661F\u8F89\u540C\u6B65", "\u52A8\u6001\u4E0B\u53D1\u6D4B\u8BD5\u79F0\u53F7", "star", "#3b82f6", "#bfdbfe", "SSR"]
            ];
        """.trimIndent()
        val fakePersonalV2Js = """
            export const PERSONAL_V2_TITLES={"pv2-sync-test":{"name":"动态星旅","mood":"实时热更专属","rarity":"SSR","art":"v2"}}
        """.trimIndent()
        val fakePv2ArtCss = """
            .pt-pv2-sync-test { --pv-title-x: 42%; --pv-title-y: 54%; --pv-title-pad: 18px; }
        """.trimIndent()
        val fakePalCss = """
            .pt-p099 { --pt-ink: #123456; --pt-paper: #eef8ff; --pt-accent: #f59e0b; }
        """.trimIndent()
        val fakeGrowthV5Js = """
            export const GROWTH_V5=["resource_respect","fishing_boss_sync"]
        """.trimIndent()
        val fakeSymbolsJs = """
            export const TITLE_SYMBOLS = { star: 'M12 2l3 6', nova_sync: 'M2 2L20 20' }
        """.trimIndent()

        val snapJson = BadgeAndCosmeticRegistry.parseAndApplyWebModules(
            titleCollectionJs = fakeTitleCollectionJs,
            personalV2CatalogJs = fakePersonalV2Js,
            growthV5Js = fakeGrowthV5Js,
            personalV2ArtCss = fakePv2ArtCss,
            personalPalettesCss = fakePalCss,
            titleSymbolsJs = fakeSymbolsJs,
        )

        val claudeMeta = BadgeAndCosmeticRegistry.resolveRaffleTitle("claude")
        assertNotNull(claudeMeta)
        assertEquals("拾光司书", claudeMeta?.name)
        assertEquals("每次相遇，都留有光芒", claudeMeta?.mood)

        val newRaffle = BadgeAndCosmeticRegistry.resolveRaffleTitle("starlight_sync_test")
        assertNotNull(newRaffle)
        assertEquals("星辉同步", newRaffle?.name)
        assertEquals("star", newRaffle?.symbolKey)

        val newPv2 = BadgeAndCosmeticRegistry.resolvePersonalTitle("pv2-sync-test")
        assertEquals("动态星旅", newPv2?.name)
        val pv2Art = ChatBubbleCosmetics.resolvePersonalV2Art("pv2-sync-test")
        assertEquals(0.42f, pv2Art.titleX, 0.001f)
        assertEquals(0.54f, pv2Art.titleY, 0.001f)
        assertEquals(18f, pv2Art.titlePadDp, 0.01f)

        val pal99 = ChatBubbleCosmetics.resolvePersonalV1Palette("p099")
        assertEquals("#123456", pal99.inkHex)

        val v5DynArt = BadgeAndCosmeticRegistry.resolveBadgeArt("fishing_boss_sync", 5)
        assertTrue(v5DynArt is BadgeArtKind.RemoteSvg)

        assertTrue(BadgeAndCosmeticRegistry.applyWebCatalogSnapshotJson(snapJson))
    }

    @Test
    fun succubusLaceDynamicFourPieceSyncAndBorderBoxShiftWorkAccurately() {
        val catalogJson = """
            {
              "entries": [
                {
                  "id": "succubus-lace",
                  "name": "绯夜魅魔",
                  "author": "DracaeneSunbringer",
                  "components": {
                    "bubble": { "type": "image/webp", "sha256": "bimg", "style": { "sha256": "bcss" } },
                    "title": { "type": "image/webp", "sha256": "timg", "style": { "sha256": "tcss" } },
                    "badge": { "type": "text/css", "sha256": "bdcss" },
                    "reply": { "type": "image/webp", "sha256": "rimg", "style": { "sha256": "rcss" } }
                  }
                }
              ]
            }
        """.trimIndent()
        BadgeAndCosmeticRegistry.updateCommunitySkinsCatalog(catalogJson)
        val synced = BadgeAndCosmeticRegistry.allCommunitySkins()["succubus-lace"]
        assertNotNull(synced)
        assertEquals("绯夜魅魔", synced?.name)
        assertTrue(synced?.hasTitle == true && synced.hasBadge && synced.hasBubble && synced.hasReply)

        val bubbleCss = """
            .cskin-bubble-succubus-lace {
              padding: 3px 6px !important;
              border-width: 25px 34px 23px 38px !important;
              border-image-source: url("data:image/webp;base64,AA") !important;
              border-image-slice: 67 85 42 92 fill !important;
              border-image-width: 44px 56px 28px 61px !important;
              color: #4f1b38 !important;
            }
            .cskin-bubble-succubus-lace::before {
              top: -25px; left: -38px; width: 38px; height: 71px;
              background-image: url("data:image/svg+xml,%3Csvg/%3E"), url("data:image/webp;base64,BB");
              background-size: 9px 9px, 22px 71px;
              background-position: 18px 13px, left top;
            }
            .cskin-bubble-succubus-lace::after {
              top: -25px; right: -34px; width: 34px; height: 71px;
              background-image: url("data:image/svg+xml,%3Csvg/%3E"), url("data:image/webp;base64,CC");
            }
            [data-theme="dark"] .cskin-bubble-succubus-lace {
              border-image-source: url("data:image/webp;base64,DD") !important;
              color: #fdf2f8 !important;
            }
        """.trimIndent()
        assertTrue(ChatBubbleCosmetics.registerParsedCommunityCss("bubble", "bcss", bubbleCss))

        val titleCss = """
            .cskin-title-succubus-lace {
              height: 48px;
              padding: 2px 4px 0 4px !important;
              border-width: 14px 42px 12px 58px !important;
              border-image-source: url("data:image/webp;base64,TT") !important;
              border-image-slice: 42 80 42 105 fill !important;
              border-image-width: 24px 46px 24px 60px !important;
              color: #561d3d !important;
            }
            .cskin-title-succubus-lace::before {
              left: -32px; top: -2px; width: 24px; height: 24px;
              background: center / contain no-repeat url("data:image/svg+xml,%3Csvg/%3E");
            }
            .cskin-title-succubus-lace::after {
              right: -38px; top: 6px; width: 20px; height: 20px;
              background: center / contain no-repeat url("data:image/svg+xml,%3Csvg/%3E");
            }
        """.trimIndent()
        assertTrue(ChatBubbleCosmetics.registerParsedCommunityCss("title", "tcss", titleCss))

        val replyCss = """
            .cskin-reply-succubus-lace {
              padding: 0 4px !important;
              border-width: 15px 54px 13px 62px !important;
              border-image-source: url("data:image/webp;base64,RR") !important;
              border-image-slice: 34 99 34 103 fill !important;
              border-image-width: 24px 70px 24px 73px !important;
              color: #4f1b38 !important;
            }
            .cskin-reply-succubus-lace::before {
              left: -38px; top: -2px; width: 24px; height: 24px;
              background: center / contain no-repeat url("data:image/svg+xml,%3Csvg/%3E");
            }
        """.trimIndent()
        assertTrue(ChatBubbleCosmetics.registerParsedCommunityCss("reply", "rcss", replyCss))

        val badgeCss = """
            .cskin-badge-succubus-lace {
              width: 28px; height: 28px;
              background: url("data:image/webp;base64,BD") center / contain no-repeat !important;
            }
            .cskin-badge-succubus-lace::before {
              inset: -4px;
              background: center / contain no-repeat url("data:image/svg+xml,%3Csvg/%3E");
            }
        """.trimIndent()
        assertTrue(ChatBubbleCosmetics.registerParsedCommunityCss("badge", "bdcss", badgeCss))

        val deco = buildJsonObject {
            put(
                "community",
                buildJsonObject {
                    put("bubble", JsonPrimitive("succubus-lace"))
                    put("title", JsonPrimitive("succubus-lace"))
                    put("badge", JsonPrimitive("succubus-lace"))
                    put("reply", JsonPrimitive("succubus-lace"))
                },
            )
        }

        // Light & Dark bubble: dark mode pseudo layers still reference light CSS indices (`community-css:...`)
        val darkBubble = ChatBubbleCosmetics.resolveBubbleFromDeco(deco, frame = false, isDarkTheme = true)
        assertNotNull(darkBubble?.nineSliceSpec)
        assertEquals(2, darkBubble?.pseudoLayers?.size)
        assertEquals(0f, darkBubble?.pseudoLayers?.get(0)?.topDp ?: -1f, 0.01f)
        assertEquals(0f, darkBubble?.pseudoLayers?.get(0)?.leftDp ?: -1f, 0.01f)
        assertTrue(darkBubble?.pseudoLayers?.get(0)?.bgImagesBottomToTop?.firstOrNull()?.sourceUrl?.startsWith("community-css:") == true)

        // Reply frame (`frame = true`) now receives parsed 9-slice & pseudo layers
        val replyStyle = ChatBubbleCosmetics.resolveBubbleFromDeco(deco, frame = true, isDarkTheme = false)
        assertNotNull(replyStyle?.nineSliceSpec)
        assertEquals(1, replyStyle?.pseudoLayers?.size)
        assertEquals(13f, replyStyle?.pseudoLayers?.get(0)?.topDp ?: -1f, 0.01f)

        // Title chip uses parsed 9-slice + padding-box -> border-box shifted star pseudo layers
        val identity = BadgeAndCosmeticRegistry.resolveEquippedIdentity(decoJson = deco, where = "card")
        val cSpec = identity.title?.communitySpec
        assertNotNull(cSpec?.nineSliceSpec)
        assertEquals(48f, cSpec?.fixedHeightDp ?: 0f, 0.01f)
        assertEquals(2, cSpec?.pseudoLayers?.size)
        assertEquals(26f, cSpec?.pseudoLayers?.get(0)?.leftDp ?: -1f, 0.01f)
        assertEquals(12f, cSpec?.pseudoLayers?.get(0)?.topDp ?: -1f, 0.01f)

        // Community badge is icon-only and resolves parsed CommunityBadgeRenderSpec
        assertEquals(true, identity.badge?.alwaysIconOnly)
        assertTrue(identity.badge?.art is BadgeArtKind.CommunityCssBadge)
        val badgeSpec = ChatBubbleCosmetics.resolveCommunityBadgeSpec("succubus-lace", null, "bdcss", false)
        assertNotNull(badgeSpec)
        assertEquals(28f, badgeSpec?.widthDp ?: 0f, 0.01f)
        assertEquals(4f, badgeSpec?.topReserveDp ?: 0f, 0.01f)
    }

    @Test
    fun badgeAlwaysIconOnlySpriteGridWallNormalizationAndShowcaseWork() {
        val spriteCss = """
            .cskin-badge-grid-test {
              width: 24px; height: 24px;
              background-image: url("data:image/webp;base64,AA");
              background-size: 900% 500%;
              background-position: 25% 50%;
            }
        """.trimIndent()
        val parsedSprite = ChatBubbleCosmetics.parseCommunityBadgeCss(spriteCss)
        assertNotNull(parsedSprite)
        assertEquals(9, parsedSprite?.spriteCols)
        assertEquals(5, parsedSprite?.spriteRows)
        assertEquals(2, parsedSprite?.spriteCol)
        assertEquals(2, parsedSprite?.spriteRow)

        val dotTitleCss = """
            .cskin-title-waibi-test {
              padding-inline: 10px 14px;
              border: 1.5px solid #bc8331;
              background: linear-gradient(135deg, #fff0a9, #ffcf62);
              color: #543510;
            }
            .cskin-title-waibi-test::after {
              content: "";
              right: 6px;
              top: 50%;
              width: 5px;
              height: 5px;
              border-radius: 50%;
              transform: translateY(-50%);
              box-shadow: -8px 0 0 #bc8331;
            }
        """.trimIndent()
        val parsedDot = ChatBubbleCosmetics.parseCommunityTitleCss(dotTitleCss)
        assertNotNull(parsedDot)
        assertEquals(1, parsedDot?.pseudoLayers?.size)
        val dotLayer = parsedDot?.pseudoLayers?.firstOrNull()
        assertEquals(true, dotLayer?.centerY)
        assertEquals(true, dotLayer?.isCircle)
        assertNotNull(dotLayer?.shadowDot)
        assertEquals(7.5f, dotLayer?.rightDp ?: 0f, 0.01f)

        // Badge alwaysIconOnly & levelText rules (`earnedLevel` on title/badge, `displayLevel` on bubble)
        val raffleGeminiDeco = buildJsonObject {
            put("ft", JsonPrimitive("gemini"))
            put("fn", JsonPrimitive(100))
            put("fl", JsonPrimitive(50))
            put("sc", JsonPrimitive("wall"))
        }
        val geminiPost = BadgeAndCosmeticRegistry.resolveEquippedIdentity(decoJson = raffleGeminiDeco, where = "post")
        assertNotNull(geminiPost.title)
        assertEquals("Lv100", geminiPost.title?.levelText)
        assertTrue(geminiPost.badge?.alwaysIconOnly == true)
        val geminiCard = BadgeAndCosmeticRegistry.resolveEquippedIdentity(decoJson = raffleGeminiDeco, where = "card")
        assertEquals("Lv100", geminiCard.title?.levelText)
        assertFalse(geminiCard.badge?.alwaysIconOnly ?: true)

        val raffleClaudeDeco = buildJsonObject {
            put("ft", JsonPrimitive("claude"))
            put("fn", JsonPrimitive(100))
        }
        val claudeId = BadgeAndCosmeticRegistry.resolveEquippedIdentity(decoJson = raffleClaudeDeco, where = "card")
        assertEquals("拾光司书", claudeId.title?.label)
        assertEquals("shiguang", claudeId.title?.specialKind)
        assertTrue(claudeId.badge?.alwaysIconOnly == true)

        // Wall normalization & Showcase keys
        val parsedDeco = BadgeAndCosmeticRegistry.parseServerDecoConfig(
            buildJsonObject {
                put("sc", JsonPrimitive("wall"))
                put("bs", JsonPrimitive("wall"))
                put("f", JsonPrimitive("song"))
                put("fx", JsonPrimitive("shine"))
                put("community", buildJsonObject { put("reply", JsonPrimitive("succubus-lace")) })
                put("showcase", JsonArray(listOf(JsonPrimitive("badge:guide"), JsonPrimitive("official:coder"))))
            },
        )
        assertEquals("post", parsedDeco.decoScope)
        assertEquals("post", parsedDeco.badgeScope)
        assertEquals("succubus-lace", parsedDeco.communityReplyId)
        assertEquals(listOf("badge:guide", "official:coder"), parsedDeco.showcaseKeys)

        val serialized = BadgeAndCosmeticRegistry.buildServerDecoPayload(
            originalDeco = null,
            config = DecoConfig(decoScope = "wall", badgeScope = "wall", communityReplyId = "succubus-lace", showcaseKeys = emptyList()),
        )
        assertEquals("post", serialized["sc"]?.jsonPrimitive?.content)
        assertEquals("post", serialized["bs"]?.jsonPrimitive?.content)
        assertEquals("succubus-lace", serialized["community"]?.jsonObject?.get("reply")?.jsonPrimitive?.content)
        assertEquals(0, serialized["showcase"]?.jsonArray?.size)

        val badgesArr = buildJsonArray {
            add(buildJsonObject { put("id", JsonPrimitive("guide")); put("n", JsonPrimitive(5)) })
            add(buildJsonObject { put("id", JsonPrimitive("guess")); put("n", JsonPrimitive(10)) })
        }
        val ots = listOf(OfficialTitleMeta(id = "coder", name = "代码诗人", mood = "写诗"))
        val (selectedShowcase, availableShowcase) = BadgeAndCosmeticRegistry.resolveShowcaseItems(
            badgesJson = badgesArr,
            officialTitles = ots,
            showcaseKeys = listOf("official:coder", "badge:guide"),
        )
        assertEquals(3, availableShowcase.size)
        assertEquals(listOf("official:coder", "badge:guide"), selectedShowcase.map { it.key })
    }

    @Test
    fun growthUnlockFilteringAndUnlockKeysMatchWebRules() {
        val g1 = GrowthRewardMeta(id = "g1", series = "guess", level = 1, name = "猜词新手")
        val g2 = GrowthRewardMeta(id = "g2", series = "guess", level = 2, name = "猜词达人")
        val fh1 = GrowthRewardMeta(id = "fh1", series = "farm_harvest", level = 1, name = "丰收初露")
        BadgeAndCosmeticRegistry.updateFromMeResponse(
            MeResponse(
                guessRewards = listOf(g1, g2),
                activityRewards = listOf(fh1),
            ),
        )

        val emptyLocked = BadgeAndCosmeticRegistry.allUnlockedGrowthRewards(
            unlocksJson = null,
            allCosmetics = false,
        )
        assertTrue(emptyLocked.isEmpty())

        val partialUnlocked = BadgeAndCosmeticRegistry.allUnlockedGrowthRewards(
            unlocksJson = JsonArray(listOf(JsonPrimitive("guess:1"), JsonPrimitive("farm_harvest:1"))),
            allCosmetics = false,
        ).map { it.id }.toSet()
        assertEquals(setOf("g1", "fh1"), partialUnlocked)
        assertFalse("g2" in partialUnlocked)

        val ownerAll = BadgeAndCosmeticRegistry.allUnlockedGrowthRewards(
            unlocksJson = null,
            allCosmetics = true,
        ).map { it.id }.toSet()
        assertTrue(setOf("g1", "g2", "fh1").all { it in ownerAll })

        val keys = BadgeAndCosmeticRegistry.parseUnlockKeys(
            JsonArray(listOf(JsonPrimitive("title:guangjie"))),
        )
        assertTrue("title:guangjie" in keys)
    }

    @Test
    fun selectiveCosmeticBlockingNormalizesKeysAndStripsFromServerPayload() {
        assertEquals("raffle:chickengun", BadgeAndCosmeticRegistry.canonicalCosmeticKey("ft:chickengun"))
        assertEquals("personal:owner-52di", BadgeAndCosmeticRegistry.canonicalCosmeticKey("pt:owner52"))
        assertEquals("official:coder", BadgeAndCosmeticRegistry.canonicalCosmeticKey("ot:coder"))
        assertEquals("growth:guess", BadgeAndCosmeticRegistry.canonicalCosmeticKey("guess:3"))
        assertEquals("growth:guess", BadgeAndCosmeticRegistry.canonicalCosmeticKey("growth:g1"))
        assertEquals("growth:rank_points_day", BadgeAndCosmeticRegistry.canonicalCosmeticKey("rank_points_day:2"))
        assertEquals("reward:guangjie", BadgeAndCosmeticRegistry.canonicalCosmeticKey("preset:yinlu"))
        assertEquals("growth:farm_harvest", BadgeAndCosmeticRegistry.canonicalCosmeticKey("farm_harvest", isBadgeId = true))
        assertEquals("badge:guide", BadgeAndCosmeticRegistry.canonicalCosmeticKey("guide", isBadgeId = true))

        val blocked = setOf(
            "community:succubus-lace",
            "raffle:chickengun",
            "growth:rank",
            "growth:activity",
        )
        assertTrue(BadgeAndCosmeticRegistry.isCosmeticBlocked("community:succubus-lace", blocked))
        assertTrue(BadgeAndCosmeticRegistry.isCosmeticBlocked("ft:chickengun", blocked))
        assertTrue(BadgeAndCosmeticRegistry.isCosmeticBlocked("ft:chickengun", blocked, isBadgeId = true))
        assertTrue(BadgeAndCosmeticRegistry.isCosmeticBlocked("rank_guess_day:1", blocked))
        assertTrue(BadgeAndCosmeticRegistry.isCosmeticBlocked("farm_harvest", blocked, isBadgeId = true))
        assertFalse(BadgeAndCosmeticRegistry.isCosmeticBlocked("community:aurora-preset", blocked))

        val cfg = DecoConfig(
            customTitle = "测试",
            cleanHideOthersNick = true,
            cleanHideReplyFrames = true,
            cleanPauseOrnaments = true,
            blockedCosmeticKeys = blocked,
            blockedKeyHideFullSet = false,
        )
        val cleanJson = BadgeAndCosmeticRegistry.encodeLocalCleanConfig(cfg)
        val serverPayload = BadgeAndCosmeticRegistry.buildServerDecoPayload(null, cfg)
        assertFalse(serverPayload.containsKey("cleanHideReplyFrames"))
        assertFalse(serverPayload.containsKey("cleanPauseOrnaments"))
        assertFalse(serverPayload.containsKey("blockedCosmeticKeys"))
        assertFalse(serverPayload.containsKey("blockedKeyHideFullSet"))

        val parsed = BadgeAndCosmeticRegistry.parseServerDecoConfig(serverPayload, cleanJson)
        assertTrue(parsed.cleanHideOthersNick)
        assertTrue(parsed.cleanHideReplyFrames)
        assertTrue(parsed.cleanPauseOrnaments)
        assertFalse(parsed.blockedKeyHideFullSet)
        assertEquals(blocked, parsed.blockedCosmeticKeys)
    }
}
