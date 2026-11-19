package com.jinhuan.forhappy

import com.jinhuan.forhappy.core.cosmetics.BadgeAndCosmeticRegistry
import com.jinhuan.forhappy.core.cosmetics.BadgeArtKind
import com.jinhuan.forhappy.core.model.FarmBadgeMeta
import com.jinhuan.forhappy.core.model.ForHappyJson
import com.jinhuan.forhappy.core.model.GrowthRewardMeta
import com.jinhuan.forhappy.core.model.MeResponse
import com.jinhuan.forhappy.core.model.OfficialTitleMeta
import com.jinhuan.forhappy.ui.me.extractBadgeLabels
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class BadgeAndCosmeticRegistryTest {

    @Test
    fun growthV5AndFarmBadgesNeverLeakRawSnakeCaseIds() {
        val sampleBadges = buildJsonArray {
            add(buildJsonObject { put("id", JsonPrimitive("farm_plant")); put("n", JsonPrimitive(15)) })
            add(buildJsonObject { put("id", JsonPrimitive("farm_water")); put("n", JsonPrimitive(7)) })
            add(buildJsonObject { put("id", JsonPrimitive("coins_peak")); put("n", JsonPrimitive(30)) })
        }

        val labels = extractBadgeLabels(sampleBadges)
        assertEquals(3, labels.size)
        labels.forEach { label ->
            assertFalse("Badge label must not contain snake_case raw ID: $label", label.contains("_"))
        }

        BadgeAndCosmeticRegistry.GROWTH_V5_IDS.forEach { id ->
            val meta = BadgeAndCosmeticRegistry.resolveBadgeMeta(id)
            assertNotNull("Missing metadata for v5 badge: $id", meta)
            assertFalse("Name for $id should be Chinese, got ${meta!!.name}", meta.name.contains("_"))

            val art = BadgeAndCosmeticRegistry.resolveBadgeArt(id, 3)
            assertTrue("Expected BundledSvg for $id, got $art", art is BadgeArtKind.BundledSvg)
        }
    }

    @Test
    fun bundledBadgeAssetsExistOnDiskForAllGrowthV5AndAtlasV4() {
        val candidates = listOf(
            File("src/main/assets"),
            File("app/src/main/assets"),
        )
        val assetsRoot = candidates.firstOrNull { it.isDirectory }
        assertNotNull("Could not locate src/main/assets directory", assetsRoot)

        val atlas = File(assetsRoot!!, "badges/v4/atlas.webp")
        assertTrue("Missing badges/v4/atlas.webp", atlas.isFile && atlas.length() > 10_000L)

        BadgeAndCosmeticRegistry.GROWTH_V5_IDS.forEach { id ->
            val art = BadgeAndCosmeticRegistry.resolveBadgeArt(id, 1) as BadgeArtKind.BundledSvg
            val svgFile = File(assetsRoot, art.assetPath)
            assertTrue("Missing bundled SVG for $id at ${art.assetPath}", svgFile.isFile && svgFile.length() > 100L)
        }
    }

    @Test
    fun classicAndRankBadgesMapToExactAtlasV4Tiles() {
        val moyuArt = BadgeAndCosmeticRegistry.resolveBadgeArt("moyu_god", 2)
        assertEquals(BadgeArtKind.AtlasV4(tileIndex = 5, style = "anime", isHonor = false, isDay = false), moyuArt)

        val elderArt = BadgeAndCosmeticRegistry.resolveBadgeArt("elder", 5)
        assertEquals(BadgeArtKind.AtlasV4(tileIndex = 17, style = "ink", isHonor = false, isDay = false), elderArt)

        val rankAll = BadgeAndCosmeticRegistry.resolveBadgeArt("rank_guess_all", 4)
        assertEquals(BadgeArtKind.AtlasV4(tileIndex = 24, style = "enamel", isHonor = true, isDay = false), rankAll)

        val rankDay = BadgeAndCosmeticRegistry.resolveBadgeArt("rank_chat_count_day", 1)
        assertEquals(BadgeArtKind.AtlasV4(tileIndex = 35, style = "enamel", isHonor = false, isDay = true), rankDay)
    }

    @Test
    fun unequippedBadgesDoNotAppearNextToNicknameWhileEquippedBadgeDoes() {
        val ownedJson = ForHappyJson.parseToJsonElement(
            """[{"id":"farm_plant","n":15},{"id":"farm_water","n":8},{"id":"coins_peak","n":500}]""",
        )

        // 1. Null / empty deco -> zero badges next to nickname!
        val unequipped = BadgeAndCosmeticRegistry.resolveEquippedIdentity(decoJson = null, badgesJson = ownedJson)
        assertNull(unequipped.badge)
        assertNull(unequipped.title)

        val defaultEmptyDeco = BadgeAndCosmeticRegistry.resolveEquippedIdentity(
            decoJson = ForHappyJson.parseToJsonElement("""{"t":"","badge_mode":"set"}"""),
            badgesJson = ownedJson,
        )
        assertNull(defaultEmptyDeco.badge)

        // 2. Actively equipped custom badge -> exactly 1 badge resolved with Chinese name & Lv
        val customEquipped = BadgeAndCosmeticRegistry.resolveEquippedIdentity(
            decoJson = ForHappyJson.parseToJsonElement("""{"badge_mode":"custom","bd":"farm_plant","bn":15}"""),
            badgesJson = ownedJson,
        )
        assertNotNull(customEquipped.badge)
        assertEquals("farm_plant", customEquipped.badge!!.id)
        assertFalse(customEquipped.badge!!.name.contains("_"))
        assertEquals(4, customEquipped.badge!!.level)
        assertEquals("Lv4", customEquipped.badge!!.levelText)

        // 3. badge_mode = none -> hidden even if bd is present
        val hiddenMode = BadgeAndCosmeticRegistry.resolveEquippedIdentity(
            decoJson = ForHappyJson.parseToJsonElement("""{"badge_mode":"none","bd":"farm_plant"}"""),
            badgesJson = ownedJson,
        )
        assertNull(hiddenMode.badge)
    }

    @Test
    fun rafflePersonalOfficialAndCommunitySetsResolveAuthenticChineseIdentity() {
        val raffleDeco = ForHappyJson.parseToJsonElement("""{"ft":"gemini","fn":3,"badge_mode":"set"}""")
        val raffleIdentity = BadgeAndCosmeticRegistry.resolveEquippedIdentity(raffleDeco)
        assertEquals("Gemini星喵", raffleIdentity.title?.label)
        assertEquals("Lv3", raffleIdentity.title?.levelText)
        assertEquals("Gemini星喵", raffleIdentity.badge?.name)
        assertEquals(
            BadgeArtKind.RemoteImage("https://52dmeme.com/assets/raffle/anime-v6/gemini-badge.webp"),
            raffleIdentity.badge?.art,
        )

        val communityDeco = ForHappyJson.parseToJsonElement(
            """{"community":{"title":"laowu-dispatch","badge":"laowu-dispatch","bubble":"laowu-dispatch"}}""",
        )
        val commIdentity = BadgeAndCosmeticRegistry.resolveEquippedIdentity(communityDeco)
        assertEquals("老吴派遣·橘", commIdentity.title?.label)
        assertEquals("老吴派遣·橘", commIdentity.badge?.name)
    }

    @Test
    fun updateFromMeResponseOverridesDynamicFarmAndActivityMetadata() {
        val resp = MeResponse(
            officialTitles = listOf(OfficialTitleMeta(id = "coder", name = "代码诗人", mood = "键盘生花")),
            farmBadges = listOf(
                FarmBadgeMeta(
                    id = "farm_plant",
                    name = "播种小能手",
                    tier = "epic",
                    desc = "累计播种升级",
                    unit = "株",
                    levels = listOf(1, 10, 50, 200, 500),
                ),
            ),
            activityRewards = listOf(
                GrowthRewardMeta(
                    id = "coins_peak_1",
                    series = "coins_peak",
                    level = 1,
                    wins = 1000,
                    name = "千金散尽",
                    label = "金库满盈",
                    desc = "持有金币峰值突破目标",
                    unit = "币",
                ),
            ),
        )
        BadgeAndCosmeticRegistry.updateFromMeResponse(resp)

        assertEquals("播种小能手", BadgeAndCosmeticRegistry.resolveBadgeMeta("farm_plant")?.name)
        assertEquals("金库满盈", BadgeAndCosmeticRegistry.resolveBadgeMeta("coins_peak")?.name)

        val otIdentity = BadgeAndCosmeticRegistry.resolveEquippedIdentity(
            decoJson = ForHappyJson.parseToJsonElement("""{"ot":"coder"}"""),
        )
        assertEquals("代码诗人", otIdentity.title?.label)
        assertTrue(otIdentity.badge?.art is BadgeArtKind.InlineOfficialSvg)
    }

    @Test
    fun serverDecoRoundTripUsesRealWebSchemaAndPreservesExtraServerFields() {
        val originalServerDeco = buildJsonObject {
            put("t", JsonPrimitive("摸鱼学家"))
            put("sh", JsonPrimitive("ribbon"))
            put("fill", JsonPrimitive("outline"))
            put("c", JsonPrimitive("#ffffff"))
            put("c2", JsonPrimitive("#2563EB"))
            put("fc", JsonArray(listOf("#ff5fa2", "#ffb3d1", "#c86bff").map { JsonPrimitive(it) }))
            put("badge_mode", JsonPrimitive("custom"))
            put("bd", JsonPrimitive("moyu_god"))
            put("bn", JsonPrimitive(7))
            put("showcase", JsonArray(listOf(JsonPrimitive("elder"))))
            put("customTitle", JsonPrimitive("legacy_junk"))
        }

        val parsed = BadgeAndCosmeticRegistry.parseServerDecoConfig(originalServerDeco)
        assertEquals("摸鱼学家", parsed.customTitle)
        assertEquals("ribbon", parsed.titleStyle)
        assertEquals("outline", parsed.fillMode)
        assertEquals("#2563EB", parsed.primaryHex)
        assertEquals("sakura", parsed.nickGradient)
        assertEquals("custom", parsed.badgeMode)
        assertEquals("moyu_god", parsed.equippedBadgeId)

        val rebuilt = BadgeAndCosmeticRegistry.buildServerDecoPayload(
            originalDeco = originalServerDeco,
            config = parsed.copy(equippedRaffleId = "claude", equippedRaffleLevel = 2),
        )
        assertEquals("摸鱼学家", rebuilt["t"]?.jsonPrimitive?.contentOrNull)
        assertEquals("claude", rebuilt["ft"]?.jsonPrimitive?.contentOrNull)
        assertEquals(2, rebuilt["fn"]?.jsonPrimitive?.intOrNull)
        assertEquals(3, rebuilt["fc"]?.jsonArray?.size)
        assertNotNull("Must preserve server showcase array", rebuilt["showcase"])
        assertNull("Must strip legacy Android-only keys", rebuilt["customTitle"])
    }
}
