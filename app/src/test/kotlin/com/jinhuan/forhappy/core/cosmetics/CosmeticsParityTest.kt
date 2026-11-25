package com.jinhuan.forhappy.core.cosmetics

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.jinhuan.forhappy.ui.components.isAnimatedRasterBytes
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CosmeticsParityTest {

    @Test
    fun remDualFormBubbleAndTitleRenderParity() {
        val remSkin = BadgeAndCosmeticRegistry.resolveCommunitySkinMeta("rem-dual-form")!!
        val remBubbleCss = """
            @property --phase { syntax: "<number>"; inherits: true; initial-value: 0; }
            @property --burst { syntax: "<number>"; inherits: true; initial-value: 0; }
            @keyframes rem-dual-cycle {
              0%, 48.29%, 50%, 98.29%, 100% { --burst: 1; }
              48.3%, 98.3% { --burst: 0; }
              50%, 98.3% { --phase: 1; }
            }
            @media (prefers-reduced-motion: reduce) {
              .cskin-bubble-rem-dual-form { animation: none; }
            }
            .cskin-bubble-rem-dual-form {
              --avatar: clamp(3.65rem, 16vw, 4.85rem);
              --bg: color-mix(in srgb, #e8f2ff, #141f37 calc(round(var(--phase)) * 100%));
              --fg: color-mix(in srgb, #223952, #edf4ff calc(round(var(--phase)) * 100%));
              --bd: color-mix(in srgb, #9cb9db, #596fa7 calc(round(var(--phase)) * 100%));
              animation: rem-dual-cycle 12s linear infinite;
              min-height: 44px;
              padding: 9px 14px 9px 62px;
              border-radius: 15px 12px 12px 15px;
              border: 1.2px solid var(--bd);
              color: var(--fg);
              background: linear-gradient(130deg, rgba(255, 255, 255, 0.69), transparent), var(--bg);
            }
            .cskin-bubble-rem-dual-form::before,
            .cskin-bubble-rem-dual-form::after {
              content: "";
              position: absolute;
              left: 6px;
              top: 50%;
              width: var(--avatar);
              height: min(100%, var(--avatar));
              aspect-ratio: 1;
              transform: translateY(-50%);
            }
            .cskin-bubble-rem-dual-form::before {
              background: url("data:image/webp;base64,REMDAY") center / contain no-repeat;
              mask-image: linear-gradient(90deg, rgba(0, 0, 0, calc(1 - round(var(--phase)))), rgba(0, 0, 0, calc(1 - round(var(--phase)))));
            }
            .cskin-bubble-rem-dual-form::after {
              background: url("data:image/webp;base64,REMNIGHT") center / contain no-repeat;
              mask-image: linear-gradient(90deg, rgba(0, 0, 0, round(var(--phase))), rgba(0, 0, 0, round(var(--phase))));
            }
        """.trimIndent()
        val remTitleCss = """
            @property --phase { syntax: "<number>"; inherits: true; initial-value: 0; }
            @keyframes rem-title-cycle {
              50%, 98.3% { --phase: 1; }
            }
            .cskin-title-rem-dual-form {
              --bg: color-mix(in srgb, #e8f2ff, #202b49 calc(round(var(--phase)) * 100%));
              --fg: color-mix(in srgb, #254365, #f2f0ff calc(round(var(--phase)) * 100%));
              animation: rem-title-cycle 12s linear infinite;
              min-height: 24px;
              padding: 2px 9px 2px 26px;
              border-radius: 10px;
              border: 1px solid #94b1d3;
              color: var(--fg);
              background: linear-gradient(130deg, rgba(255, 255, 255, 0.69), transparent), var(--bg);
            }
            .cskin-title-rem-dual-form::before,
            .cskin-title-rem-dual-form::after {
              content: "";
              position: absolute;
              left: 3px;
              top: 50%;
              width: 20px;
              height: 20px;
              transform: translateY(-50%);
            }
            .cskin-title-rem-dual-form::before {
              background: url("data:image/webp;base64,TITLEDAY") center / contain no-repeat;
              opacity: calc(1 - round(var(--phase)));
            }
            .cskin-title-rem-dual-form::after {
              background: url("data:image/webp;base64,TITLENIGHT") center / contain no-repeat;
              opacity: round(var(--phase));
            }
        """.trimIndent()
        ChatBubbleCosmetics.registerParsedCommunityCss("bubble", remSkin.bubbleCssSha!!, remBubbleCss)
        ChatBubbleCosmetics.registerParsedCommunityCss("title", remSkin.titleCssSha!!, remTitleCss)

        val deco = buildJsonObject {
            put(
                "community",
                buildJsonObject {
                    put("title", JsonPrimitive("rem-dual-form"))
                    put("bubble", JsonPrimitive("rem-dual-form"))
                },
            )
        }

        val lightBubble = ChatBubbleCosmetics.resolveBubbleFromDeco(deco, isDarkTheme = false)
        assertNotNull(lightBubble)
        assertNull(lightBubble?.nineSliceSpec)
        assertEquals(Color(0xFFE8F2FF), lightBubble?.fillColor)
        assertTrue(lightBubble?.minHeightDp ?: 0f >= 44f)
        assertTrue(lightBubble?.contentPadding?.calculateLeftPadding(LayoutDirection.Ltr)!! >= 56.dp)
        assertEquals(1, lightBubble.pseudoLayers.size)
        assertTrue(lightBubble.pseudoLayers[0].heightCapToParent)
        assertEquals(1f, lightBubble.pseudoLayers[0].aspectRatio ?: 0f, 0.01f)
        assertTrue(lightBubble.pseudoLayers[0].bgImagesBottomToTop.firstOrNull()?.sourceUrl?.endsWith(":0") == true)

        val darkBubble = ChatBubbleCosmetics.resolveBubbleFromDeco(deco, isDarkTheme = true)
        assertNotNull(darkBubble)
        assertNull(darkBubble?.nineSliceSpec)
        assertEquals(Color(0xFF141F37), darkBubble?.fillColor)
        assertEquals(1, darkBubble?.pseudoLayers?.size)
        assertTrue(darkBubble?.pseudoLayers?.firstOrNull()?.bgImagesBottomToTop?.firstOrNull()?.sourceUrl?.endsWith(":1") == true)

        val lightId = BadgeAndCosmeticRegistry.resolveEquippedIdentity(decoJson = deco, where = "chat", isDarkTheme = false)
        val darkId = BadgeAndCosmeticRegistry.resolveEquippedIdentity(decoJson = deco, where = "chat", isDarkTheme = true)
        assertEquals("community:rem-dual-form", lightId.title?.key)
        assertNull(lightId.title?.leadingIconUrl)
        assertNull(lightId.title?.trailingIconUrl)
        assertEquals(1, lightId.title?.communitySpec?.pseudoLayers?.size)
        assertTrue(
            lightId.title?.communitySpec?.pseudoLayers?.firstOrNull()
                ?.bgImagesBottomToTop?.firstOrNull()?.sourceUrl?.endsWith(":0") == true,
        )
        assertEquals("#e8f2ff", lightId.title?.bgHex?.lowercase())
        assertEquals("#202b49", darkId.title?.bgHex?.lowercase())
        assertNull(darkId.title?.leadingIconUrl)
        assertEquals(1, darkId.title?.communitySpec?.pseudoLayers?.size)
        assertTrue(
            darkId.title?.communitySpec?.pseudoLayers?.firstOrNull()
                ?.bgImagesBottomToTop?.firstOrNull()?.sourceUrl?.endsWith(":1") == true,
        )
    }

    @Test
    fun p3rZeroHourFoolGenericCssParsesBorderImageInheritAndAspectRatio() {
        val p3rSkin = BadgeAndCosmeticRegistry.resolveCommunitySkinMeta("p3r-zero-hour-fool")!!
        val p3rBubbleCss = """
            .cskin-bubble-p3r-zero-hour-fool {
              border-image-source: url("data:image/webp;base64,P3R");
              border-image-width: 0;
              border-radius: .3em 1em 1em 1em;
              border: 2px solid #52d6ff;
              padding: 9px 14px 9px 44px;
              color: #f6fdff;
              background: linear-gradient(135deg, #0734b8, #052794);
            }
            .cskin-bubble-p3r-zero-hour-fool::before {
              content: "";
              position: absolute;
              left: 0;
              bottom: 0;
              height: 100%;
              aspect-ratio: 144 / 225;
              border-image-source: inherit;
              border-image-slice: 0 fill;
              border-image-width: 0;
            }
            .cskin-bubble-p3r-zero-hour-fool::after {
              content: "";
              position: absolute;
              inset: 0;
              background:
                linear-gradient(90deg, rgba(255,255,255,0.2), transparent) 0 0 / 12px 100% repeat-y,
                radial-gradient(circle at 90% 20%, rgba(82,214,255,0.25), transparent);
            }
        """.trimIndent()
        ChatBubbleCosmetics.registerParsedCommunityCss("bubble", p3rSkin.bubbleCssSha!!, p3rBubbleCss)
        val p3rDeco = buildJsonObject {
            put("community", buildJsonObject { put("bubble", JsonPrimitive("p3r-zero-hour-fool")) })
        }
        val style = ChatBubbleCosmetics.resolveBubbleFromDeco(p3rDeco, isDarkTheme = false)!!
        assertNull(style.nineSliceSpec)
        assertEquals(BubbleShapeKind.Rounded(4.5f, 15f, 15f, 15f), style.shapeKind)
        val portraitLayer = style.pseudoLayers.firstOrNull { it.nineSliceSpec != null }
        assertNotNull(portraitLayer)
        assertEquals(144f / 225f, portraitLayer?.aspectRatio ?: 0f, 0.01f)
        assertTrue(portraitLayer?.nineSliceSpec?.sourceUrl?.isNotBlank() == true)
    }

    @Test
    fun brandNewUnknownCommunitySubmissionRendersWithoutCodeChanges() {
        val unknownSlug = "brand-new-2099"
        val preDeco = buildJsonObject {
            put(
                "community",
                buildJsonObject {
                    put("title", JsonPrimitive(unknownSlug))
                    put("badge", JsonPrimitive(unknownSlug))
                    put("bubble", JsonPrimitive(unknownSlug))
                },
            )
        }
        val preIdentity = BadgeAndCosmeticRegistry.resolveEquippedIdentity(decoJson = preDeco, where = "chat")
        assertEquals("catalog:$unknownSlug", preIdentity.title?.dynamicCssSpec)
        assertEquals("catalog:$unknownSlug", preIdentity.badge?.dynamicCssSpec)
        assertTrue(preIdentity.badge?.art is BadgeArtKind.EmojiGlyph)
        val preBubble = ChatBubbleCosmetics.resolveBubbleFromDeco(preDeco)
        assertEquals("catalog:$unknownSlug", preBubble?.dynamicCssSpec)

        val catalogJson = """
            {
              "entries": [
                {
                  "id": "$unknownSlug",
                  "name": "未来投稿2099",
                  "author": "tester",
                  "components": {
                    "title": { "type": "text/css", "sha256": "t2099sha" },
                    "badge": { "type": "text/css", "sha256": "b2099sha" },
                    "bubble": { "type": "text/css", "sha256": "u2099sha" }
                  }
                }
              ]
            }
        """.trimIndent()
        BadgeAndCosmeticRegistry.updateCommunitySkinsCatalog(catalogJson)
        val syncedIdentity = BadgeAndCosmeticRegistry.resolveEquippedIdentity(decoJson = preDeco, where = "chat")
        assertTrue(syncedIdentity.badge?.art is BadgeArtKind.CommunityCssBadge)
        val newTitleCss = """
            .cskin-title-$unknownSlug {
              background: linear-gradient(90deg, #0ea5e9, #6366f1);
              color: #ffffff;
              padding: 2px 10px;
              border-radius: 999px;
            }
        """.trimIndent()
        val newBubbleCss = """
            .cskin-bubble-$unknownSlug {
              background: #0f172a;
              border: 2px solid #38bdf8;
              border-radius: 16px;
              color: #e0f2fe;
              padding: 8px 14px;
            }
        """.trimIndent()
        assertTrue(ChatBubbleCosmetics.registerParsedCommunityCss("title", "t2099sha", newTitleCss))
        assertTrue(ChatBubbleCosmetics.registerParsedCommunityCss("bubble", "u2099sha", newBubbleCss))

        val postIdentity = BadgeAndCosmeticRegistry.resolveEquippedIdentity(decoJson = preDeco, where = "chat")
        assertEquals("未来投稿2099", postIdentity.title?.label)
        assertEquals("#0ea5e9", postIdentity.title?.bgHex?.lowercase())
        val postBubble = ChatBubbleCosmetics.resolveBubbleFromDeco(preDeco)
        assertEquals(Color(0xFF0F172A), postBubble?.fillColor)
        assertEquals(Color(0xFF38BDF8), postBubble?.borderColor)
        assertNull(postBubble?.dynamicCssSpec)
    }

    @Test
    fun exact16050FloorDecoResolvesTitleLabelBadgeAndRemBubble() {
        val blackSkin = BadgeAndCosmeticRegistry.resolveCommunitySkinMeta("laowu-dispatch-black")!!
        val blackTitleCss = """
            .cskin-title-laowu-dispatch-black {
              background: linear-gradient(135deg, #ffe88f, #ffd43b);
              color: #392b1c;
              border: 1px solid #caa23e;
              padding-inline: 18px 8px;
            }
            .cskin-title-laowu-dispatch-black::after {
              content: "俺拾的嘞";
            }
        """.trimIndent()
        ChatBubbleCosmetics.registerParsedCommunityCss("title", blackSkin.titleCssSha!!, blackTitleCss)

        val deco16050 = buildJsonObject {
            put(
                "community",
                buildJsonObject {
                    put("title", JsonPrimitive("laowu-dispatch-black"))
                    put("bubble", JsonPrimitive("rem-dual-form"))
                },
            )
            put("bd", JsonPrimitive("scratch_draws"))
            put("bs", JsonPrimitive("all"))
            put("sc", JsonPrimitive("all"))
        }

        val identity = BadgeAndCosmeticRegistry.resolveEquippedIdentity(
            decoJson = deco16050,
            where = "chat",
            isDarkTheme = true,
        )
        assertEquals("community:laowu-dispatch-black", identity.title?.key)
        assertEquals("俺拾的嘞", identity.title?.label)
        assertEquals("scratch_draws", identity.badge?.id)

        val bubble = ChatBubbleCosmetics.resolveBubbleFromDeco(deco16050, isDarkTheme = true)
        assertEquals("community:rem-dual-form", bubble?.key)
        assertNull(bubble?.nineSliceSpec)
    }

    @Test
    fun shadowDaoAndShadowPathTitleCssParseNineSliceWithoutInlineLightUrl() {
        val shadowDaoTitleCss = """
            .cskin-title-shadow-dao {
              border-image-slice: 14 28 14 28 fill;
              border-image-width: 7px 14px 7px 14px;
              color: #141418;
            }
            [data-theme="dark"] .cskin-title-shadow-dao {
              border-image-source: url("data:image/png;base64,AAAA");
              color: #f3efe6;
            }
        """.trimIndent()
        val parsedDao = ChatBubbleCosmetics.parseCommunityTitleCss(shadowDaoTitleCss)
        assertNotNull(parsedDao)
        assertTrue(parsedDao?.hasNineSlice == true)
        assertEquals(14f, parsedDao?.sliceTop ?: 0f, 0.01f)
        assertEquals(28f, parsedDao?.sliceRight ?: 0f, 0.01f)
        assertTrue(parsedDao?.darkHasImageVariant == true)

        val shadowPathTitleCss = """
            .cskin-title-shadow-path {
              border-image-slice: 16 32 16 32 fill;
              border-image-width: 8px 16px 8px 16px;
              color: #0f1418;
            }
            [data-theme="dark"] .cskin-title-shadow-path {
              border-image-source: url("data:image/png;base64,BBBB");
              color: #f2f7f8;
            }
        """.trimIndent()
        val parsedPath = ChatBubbleCosmetics.parseCommunityTitleCss(shadowPathTitleCss)
        assertNotNull(parsedPath)
        assertTrue(parsedPath?.hasNineSlice == true)
        assertEquals(16f, parsedPath?.sliceTop ?: 0f, 0.01f)
    }

    @Test
    fun bsPostOnlyGatesCustomBadgeWhileFtIgnoresScPost() {
        // Case A1: `bs = "post"` hides custom `bd` in chat even when `sc = "all"`
        val bdGatedDeco = buildJsonObject {
            put("bd", JsonPrimitive("scratch_draws"))
            put("bs", JsonPrimitive("post"))
            put("sc", JsonPrimitive("all"))
        }
        assertNull(BadgeAndCosmeticRegistry.resolveEquippedIdentity(decoJson = bdGatedDeco, where = "chat").badge)
        assertEquals(
            "scratch_draws",
            BadgeAndCosmeticRegistry.resolveEquippedIdentity(decoJson = bdGatedDeco, where = "post").badge?.id,
        )

        // Case A2: `sc = "post"` does NOT hide custom `bd` in chat when `bs = "all"`
        val scPostBdAllDeco = buildJsonObject {
            put("bd", JsonPrimitive("scratch_draws"))
            put("bs", JsonPrimitive("all"))
            put("sc", JsonPrimitive("post"))
        }
        assertEquals(
            "scratch_draws",
            BadgeAndCosmeticRegistry.resolveEquippedIdentity(decoJson = scPostBdAllDeco, where = "chat").badge?.id,
        )

        // Case B: `ft = "gemini"` ignores `sc = "post"`, whereas `community` follows `sc = "post"`
        val raffleAndCommDeco = buildJsonObject {
            put("ft", JsonPrimitive("gemini"))
            put("fn", JsonPrimitive(100))
            put("fl", JsonPrimitive(50))
            put("bd", JsonPrimitive("scratch_draws"))
            put("bs", JsonPrimitive("post"))
            put("sc", JsonPrimitive("post"))
            put(
                "community",
                buildJsonObject {
                    put("title", JsonPrimitive("laowu-dispatch-black"))
                    put("bubble", JsonPrimitive("rem-dual-form"))
                },
            )
        }
        val chatId = BadgeAndCosmeticRegistry.resolveEquippedIdentity(decoJson = raffleAndCommDeco, where = "chat")
        assertEquals("raffle:gemini", chatId.title?.key)
        assertEquals("ft:gemini", chatId.badge?.id)
        assertEquals(
            "raffle:gemini",
            ChatBubbleCosmetics.resolveBubbleFromDeco(raffleAndCommDeco, where = "chat", isDarkTheme = true)?.key,
        )

        val postId = BadgeAndCosmeticRegistry.resolveEquippedIdentity(decoJson = raffleAndCommDeco, where = "post")
        assertEquals("community:laowu-dispatch-black", postId.title?.key)
        assertEquals("ft:gemini", postId.badge?.id)
        assertEquals(
            "community:rem-dual-form",
            ChatBubbleCosmetics.resolveBubbleFromDeco(raffleAndCommDeco, where = "post", isDarkTheme = true)?.key,
        )
    }

    @Test
    fun ensureReadableNickColorsLiftsDarkStopsOnlyInDarkTheme() {
        val blackgold = listOf(Color(0xFF111111), Color(0xFFF59E0B), Color(0xFFFBBF24))
        val lightOut = ChatBubbleCosmetics.ensureReadableNickColors(blackgold, isDarkTheme = false)
        assertEquals(Color(0xFF111111), lightOut[0])

        val darkOut = ChatBubbleCosmetics.ensureReadableNickColors(blackgold, isDarkTheme = true)
        assertTrue("Dark stop should be lifted above 0.18 luminance", darkOut[0].luminance() >= 0.18f)
        assertEquals(Color(0xFFF59E0B), darkOut[1])
        assertEquals(Color(0xFFFBBF24), darkOut[2])
    }

    @Test
    fun gushenYinpaPureCssBubbleParsesWithoutFallingBackToNineSlice() {
        val css = """
            .cskin-bubble-gushen-yinpa {
              min-height: 40px;
              border-radius: 18px;
              border: 2px solid #211525;
              padding: 8px 14px;
              color: #211525;
              background: linear-gradient(135deg, color-mix(in srgb, #ffe65b 92%, #ffffff), #ffd43b);
            }
            .cskin-bubble-gushen-yinpa::before {
              content: "";
              display: inline-flex;
              width: 24px;
              height: 24px;
            }
            [data-theme="dark"] .cskin-bubble-gushen-yinpa {
              background: linear-gradient(135deg, #3b2d0a, #5c430d);
              border-color: #ffe65b;
              color: #fff3b0;
            }
        """.trimIndent()
        val parsed = ChatBubbleCosmetics.parseCommunityBubbleCss(css)
        assertNotNull(parsed)
        assertFalse(parsed!!.isNineSlice)
        assertEquals(40f, parsed.minHeightDp, 0.01f)
        assertEquals(listOf(18f, 18f, 18f, 18f), parsed.nonNineSliceRadiiDp)
        assertTrue("In-flow display: inline-flex pseudo-element should not produce overlay", parsed.pseudoLayers.isEmpty())

        val skin = BadgeAndCosmeticRegistry.resolveCommunitySkinMeta("gushen-yinpa")
        assertNotNull(skin)
        val cssSha = skin!!.bubbleCssSha
        assertNotNull(cssSha)
        assertTrue(ChatBubbleCosmetics.registerParsedCommunityCss("bubble", cssSha!!, css))

        val deco = buildJsonObject {
            put("community", buildJsonObject { put("bubble", JsonPrimitive("gushen-yinpa")) })
        }
        val resolved = ChatBubbleCosmetics.resolveBubbleFromDeco(deco, isDarkTheme = true)
        assertNotNull(resolved)
        assertNull(resolved?.nineSliceSpec)
        assertTrue((resolved?.minHeightDp ?: 0f) >= 40f)
    }

    @Test
    fun remDualFormTimelineAnimatesBetweenDayAndNight() {
        val remSkin = BadgeAndCosmeticRegistry.resolveCommunitySkinMeta("rem-dual-form")!!
        val remBubbleCss = """
            @property --phase { syntax: "<number>"; inherits: true; initial-value: 0; }
            @property --burst { syntax: "<number>"; inherits: true; initial-value: 0; }
            @keyframes rem-dual-cycle {
              0%, 48.29%, 50%, 98.29%, 100% { --burst: 1; }
              48.3%, 98.3% { --burst: 0; }
              50%, 98.3% { --phase: 1; }
            }
            @media (prefers-reduced-motion: reduce) {
              .cskin-bubble-rem-dual-form { animation: none; }
            }
            .cskin-bubble-rem-dual-form {
              --avatar: clamp(3.65rem, 16vw, 4.85rem);
              --bg: color-mix(in srgb, #e8f2ff, #141f37 calc(round(var(--phase)) * 100%));
              --fg: color-mix(in srgb, #223952, #edf4ff calc(round(var(--phase)) * 100%));
              --bd: color-mix(in srgb, #9cb9db, #596fa7 calc(round(var(--phase)) * 100%));
              animation: rem-dual-cycle 12s linear infinite;
              min-height: 44px;
              padding: 9px 14px 9px 62px;
              border-radius: 15px 12px 12px 15px;
              border: 1.2px solid var(--bd);
              color: var(--fg);
              background: linear-gradient(130deg, rgba(255, 255, 255, 0.69), transparent), var(--bg);
            }
            .cskin-bubble-rem-dual-form::before,
            .cskin-bubble-rem-dual-form::after {
              content: "";
              position: absolute;
              left: 6px;
              top: 50%;
              width: var(--avatar);
              height: min(100%, var(--avatar));
              aspect-ratio: 1;
              transform: translateY(-50%);
            }
            .cskin-bubble-rem-dual-form::before {
              background: url("data:image/webp;base64,REMDAY") center / contain no-repeat;
              mask-image: linear-gradient(90deg, rgba(0, 0, 0, calc(1 - round(var(--phase)))), rgba(0, 0, 0, calc(1 - round(var(--phase)))));
            }
            .cskin-bubble-rem-dual-form::after {
              background: url("data:image/webp;base64,REMNIGHT") center / contain no-repeat;
              mask-image: linear-gradient(90deg, rgba(0, 0, 0, round(var(--phase))), rgba(0, 0, 0, round(var(--phase))));
            }
        """.trimIndent()
        val remTitleCss = """
            @property --phase { syntax: "<number>"; inherits: true; initial-value: 0; }
            @keyframes rem-title-cycle {
              50%, 98.3% { --phase: 1; }
            }
            .cskin-title-rem-dual-form {
              --bg: color-mix(in srgb, #e8f2ff, #202b49 calc(round(var(--phase)) * 100%));
              --fg: color-mix(in srgb, #254365, #f2f0ff calc(round(var(--phase)) * 100%));
              animation: rem-title-cycle 12s linear infinite;
              min-height: 24px;
              padding: 2px 9px 2px 26px;
              border-radius: 10px;
              border: 1px solid #94b1d3;
              color: var(--fg);
              background: linear-gradient(130deg, rgba(255, 255, 255, 0.69), transparent), var(--bg);
            }
            .cskin-title-rem-dual-form::before,
            .cskin-title-rem-dual-form::after {
              content: "";
              position: absolute;
              left: 3px;
              top: 50%;
              width: 20px;
              height: 20px;
              transform: translateY(-50%);
            }
            .cskin-title-rem-dual-form::before {
              background: url("data:image/webp;base64,TITLEDAY") center / contain no-repeat;
              opacity: calc(1 - round(var(--phase)));
            }
            .cskin-title-rem-dual-form::after {
              background: url("data:image/webp;base64,TITLENIGHT") center / contain no-repeat;
              opacity: round(var(--phase));
            }
        """.trimIndent()
        ChatBubbleCosmetics.registerParsedCommunityCss("bubble", remSkin.bubbleCssSha!!, remBubbleCss)
        ChatBubbleCosmetics.registerParsedCommunityCss("title", remSkin.titleCssSha!!, remTitleCss)

        val deco = buildJsonObject {
            put(
                "community",
                buildJsonObject {
                    put("title", JsonPrimitive("rem-dual-form"))
                    put("bubble", JsonPrimitive("rem-dual-form"))
                },
            )
        }
        val resolvedBubble = ChatBubbleCosmetics.resolveBubbleFromDeco(deco, isDarkTheme = false)!!
        assertEquals(Color(0xFFE8F2FF), resolvedBubble.fillColor)
        val bubbleTimeline = resolvedBubble.animationTimeline
        assertNotNull("Animated bubble must produce timeline", bubbleTimeline)
        assertEquals(12_000L, bubbleTimeline!!.periodMs)
        assertTrue(bubbleTimeline.frames.any { it.fillColor.luminance() > 0.8f })
        assertTrue(bubbleTimeline.frames.any { it.fillColor.luminance() < 0.15f })

        val resolvedId = BadgeAndCosmeticRegistry.resolveEquippedIdentity(decoJson = deco, where = "chat", isDarkTheme = false)
        val titleTimeline = resolvedId.title?.communitySpec?.animationTimeline
        assertNotNull("Animated title must produce timeline", titleTimeline)
        assertEquals(12_000L, titleTimeline!!.periodMs)
        assertTrue(titleTimeline.frames.any { (it.bgColors.firstOrNull()?.luminance() ?: 0f) > 0.8f })
        assertTrue(titleTimeline.frames.any { (it.bgColors.firstOrNull()?.luminance() ?: 1f) < 0.15f })
    }

    @Test
    fun keyframeUrlSwapAndCustomVarMotionPopulatePseudoTimelines() {
        val swapCss = """
            @keyframes tentacle-swap {
              0%, 100% { background-image: url("https://example.com/tentacle_a.webp"); }
              50% { background-image: url("https://example.com/tentacle_b.webp"); }
            }
            .cskin-bubble-test-swap {
              min-height: 44px;
              padding: 8px 14px;
              background: #fff8fc;
              color: #2c1e28;
            }
            .cskin-bubble-test-swap::after {
              content: "";
              position: absolute;
              right: 0;
              bottom: 0;
              width: 28px;
              height: 28px;
              background: url("https://example.com/tentacle_a.webp") center / contain no-repeat;
              animation: tentacle-swap 2s steps(1) infinite;
            }
        """.trimIndent()
        val urls = ChatBubbleCosmetics.extractAllCssImageUrls(swapCss)
        assertTrue(urls.contains("https://example.com/tentacle_a.webp"))
        assertTrue(urls.contains("https://example.com/tentacle_b.webp"))

        val parsedSwap = ChatBubbleCosmetics.parseCommunityBubbleCss(swapCss)!!
        assertEquals(1, parsedSwap.pseudoLayers.size)
        val swapLayer = parsedSwap.pseudoLayers[0]
        assertEquals(2_000L, swapLayer.timelinePeriodMs)
        val swappedIdxCount = swapLayer.timelineFrames
            .mapNotNull { it.bgImagesBottomToTop.firstOrNull()?.urlIdx }
            .distinct()
            .size
        assertTrue("Keyframe url() swap should produce >= 2 distinct URL indices", swappedIdxCount >= 2)

        val customVarCss = """
            @property --look-x { syntax: "<length>"; inherits: true; initial-value: 0px; }
            @keyframes buddy-look {
              0%, 100% { --look-x: 0px; }
              50% { --look-x: 8px; }
            }
            .cskin-bubble-test-var {
              min-height: 44px;
              padding: 8px 14px;
              background: #ffffff;
              color: #111111;
              animation: buddy-look 4s linear infinite;
            }
            .cskin-bubble-test-var::after {
              content: "";
              position: absolute;
              right: 0;
              bottom: 0;
              width: 24px;
              height: 24px;
              transform: translateX(var(--look-x));
              background: url("https://example.com/buddy.webp") center / contain no-repeat;
            }
        """.trimIndent()
        val parsedVar = ChatBubbleCosmetics.parseCommunityBubbleCss(customVarCss)!!
        assertEquals(1, parsedVar.pseudoLayers.size)
        assertEquals(4_000L, parsedVar.timelinePeriodMs)
        val distinctX = parsedVar.timelineFrames
            .mapNotNull { it.pseudoLayers.firstOrNull()?.offsetXDp }
            .distinct()
        assertTrue("Custom --var timeline should drive pseudo offsetXDp across frames: $distinctX", distinctX.size >= 2)
        assertTrue(distinctX.any { it >= 7f })
    }

    @Test
    fun keyframeTransformOpacityAndSpriteBadgeStepsParseGenerically() {
        val visualCss = """
            @keyframes float-pulse {
              0% { transform: translate(0px, 0px) scale(1) rotate(0deg); opacity: 1; }
              50% { transform: translate(4px, -6px) scale(1.2) rotate(12deg); opacity: 0.4; }
              100% { transform: translate(0px, 0px) scale(1) rotate(0deg); opacity: 1; }
            }
            .cskin-bubble-test-motion {
              min-height: 44px;
              padding: 8px 14px;
              background: #ffffff;
              color: #111111;
            }
            .cskin-bubble-test-motion::after {
              content: "";
              position: absolute;
              right: 2px;
              top: 2px;
              width: 20px;
              height: 20px;
              background: url("https://example.com/spark.webp") center / contain no-repeat;
              animation: float-pulse 2s linear infinite;
            }
        """.trimIndent()
        val parsedVisual = ChatBubbleCosmetics.parseCommunityBubbleCss(visualCss)!!
        val tracks = parsedVisual.pseudoLayers.first().visualAnim
        assertTrue("visualAnim tracks should be parsed from @keyframes", tracks.isNotEmpty())
        val atZero = ChatBubbleCosmetics.evaluateVisualAnim(tracks, 0L)
        val atHalf = ChatBubbleCosmetics.evaluateVisualAnim(tracks, 1_000L)
        assertEquals(0f, atZero.txDpDelta, 0.05f)
        assertEquals(1f, atZero.scaleX, 0.05f)
        assertEquals(1f, atZero.opacity ?: 1f, 0.05f)
        assertEquals(4f, atHalf.txDpDelta, 0.1f)
        assertEquals(-6f, atHalf.tyDpDelta, 0.1f)
        assertEquals(1.2f, atHalf.scaleX, 0.05f)
        assertEquals(12f, atHalf.rotateDeg, 0.2f)
        assertEquals(0.4f, atHalf.opacity ?: 0f, 0.05f)

        val spriteBadgeCss = """
            @keyframes badge-spin {
              0% { background-position: 0% 0%; }
              100% { background-position: -400% 0%; }
            }
            .cskin-badge-test-sprite {
              width: 24px;
              height: 24px;
              background: url("https://example.com/sprite.png") 0% 0% / 400% 100% no-repeat;
              animation: badge-spin 0.8s steps(4, end) infinite;
            }
        """.trimIndent()
        val parsedBadge = ChatBubbleCosmetics.parseCommunityBadgeCss(spriteBadgeCss)!!
        assertEquals(4, parsedBadge.spriteCols)
        assertEquals(800L, parsedBadge.timelinePeriodMs)
        val spriteCols = parsedBadge.timelineFrames.map { it.spriteCol }.distinct().sorted()
        assertEquals(listOf(0, 1, 2, 3), spriteCols)
    }

    @Test
    fun chickengunDecoFlagsAndAnimatedRasterHeaderDetection() {
        val decoWithDisabledMotion = buildJsonObject {
            put("ft", JsonPrimitive("chickengun"))
            put("fn", JsonPrimitive(3500))
            put("cg_motion", JsonPrimitive(false))
            put("cg_fire", JsonPrimitive(false))
        }
        val resolvedDisabled = ChatBubbleCosmetics.resolveBubbleFromDeco(
            deco = decoWithDisabledMotion,
            isDarkTheme = false,
            nowSec = 1_000_000L,
        )
        val ornamentDisabled = resolvedDisabled?.ornament as? BubbleOrnament.RaffleChickengun
        assertNotNull(ornamentDisabled)
        assertFalse(ornamentDisabled!!.cgMotion)
        assertFalse(ornamentDisabled.cgFire)

        val decoDefaultEnabled = buildJsonObject {
            put("ft", JsonPrimitive("chickengun"))
            put("fn", JsonPrimitive(3500))
        }
        val resolvedDefault = ChatBubbleCosmetics.resolveBubbleFromDeco(
            deco = decoDefaultEnabled,
            isDarkTheme = false,
            nowSec = 1_000_000L,
        )
        val ornamentDefault = resolvedDefault?.ornament as? BubbleOrnament.RaffleChickengun
        assertNotNull(ornamentDefault)
        assertTrue(ornamentDefault!!.cgMotion)
        assertTrue(ornamentDefault.cgFire)

        // Minimal animated WebP (`RIFF....WEBP` + `VP8X` with anim bit 0x02) vs static VP8L WebP
        val animWebp = byteArrayOf(
            'R'.code.toByte(), 'I'.code.toByte(), 'F'.code.toByte(), 'F'.code.toByte(),
            22, 0, 0, 0,
            'W'.code.toByte(), 'E'.code.toByte(), 'B'.code.toByte(), 'P'.code.toByte(),
            'V'.code.toByte(), 'P'.code.toByte(), '8'.code.toByte(), 'X'.code.toByte(),
            10, 0, 0, 0,
            0x02, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        )
        val staticWebp = byteArrayOf(
            'R'.code.toByte(), 'I'.code.toByte(), 'F'.code.toByte(), 'F'.code.toByte(),
            20, 0, 0, 0,
            'W'.code.toByte(), 'E'.code.toByte(), 'B'.code.toByte(), 'P'.code.toByte(),
            'V'.code.toByte(), 'P'.code.toByte(), '8'.code.toByte(), 'L'.code.toByte(),
            8, 0, 0, 0,
            0x2F, 0, 0, 0, 0, 0, 0, 0,
        )
        assertTrue(isAnimatedRasterBytes(animWebp))
        assertFalse(isAnimatedRasterBytes(staticWebp))

        // Multi-frame GIF89a (2 image descriptors) vs single-frame GIF89a
        val animGif = byteArrayOf(
            'G'.code.toByte(), 'I'.code.toByte(), 'F'.code.toByte(), '8'.code.toByte(), '9'.code.toByte(), 'a'.code.toByte(),
            1, 0, 1, 0, 0, 0, 0,
            0x2C, 0, 0, 0, 0, 1, 0, 1, 0, 0, 2, 1, 0, 0,
            0x2C, 0, 0, 0, 0, 1, 0, 1, 0, 0, 2, 1, 0, 0,
            0x3B,
        )
        val staticGif = byteArrayOf(
            'G'.code.toByte(), 'I'.code.toByte(), 'F'.code.toByte(), '8'.code.toByte(), '9'.code.toByte(), 'a'.code.toByte(),
            1, 0, 1, 0, 0, 0, 0,
            0x2C, 0, 0, 0, 0, 1, 0, 1, 0, 0, 2, 1, 0, 0,
            0x3B,
        )
        assertTrue(isAnimatedRasterBytes(animGif))
        assertFalse(isAnimatedRasterBytes(staticGif))

        // APNG (`acTL` chunk before `IDAT`) vs static PNG (`IDAT` first)
        val apng = byteArrayOf(
            0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte(), 0x0D, 0x0A, 0x1A, 0x0A,
            0, 0, 0, 13, 'I'.code.toByte(), 'H'.code.toByte(), 'D'.code.toByte(), 'R'.code.toByte(),
            0, 0, 0, 1, 0, 0, 0, 1, 8, 2, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 8, 'a'.code.toByte(), 'c'.code.toByte(), 'T'.code.toByte(), 'L'.code.toByte(),
            0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0,
        )
        val staticPng = byteArrayOf(
            0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte(), 0x0D, 0x0A, 0x1A, 0x0A,
            0, 0, 0, 13, 'I'.code.toByte(), 'H'.code.toByte(), 'D'.code.toByte(), 'R'.code.toByte(),
            0, 0, 0, 1, 0, 0, 0, 1, 8, 2, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 4, 'I'.code.toByte(), 'D'.code.toByte(), 'A'.code.toByte(), 'T'.code.toByte(),
            0, 0, 0, 0, 0, 0, 0, 0,
        )
        assertTrue(isAnimatedRasterBytes(apng))
        assertFalse(isAnimatedRasterBytes(staticPng))
    }
}
