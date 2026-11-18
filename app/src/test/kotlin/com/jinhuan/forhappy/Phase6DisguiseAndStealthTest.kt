package com.jinhuan.forhappy

import com.jinhuan.forhappy.core.model.AppDisguiseMode
import com.jinhuan.forhappy.core.model.AppIconAlias
import com.jinhuan.forhappy.core.model.PrivacyStealthConfig
import com.jinhuan.forhappy.ui.disguise.DisguiseSanitizer
import com.jinhuan.forhappy.ui.disguise.FeishuDisguiseHelpers
import com.jinhuan.forhappy.ui.disguise.LinuxDoTopicMeta
import com.jinhuan.forhappy.ui.disguise.safeIconAliasTitle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase6DisguiseAndStealthTest {

    @Test
    fun mapStickerToSafeEmojiConvertsPackKeysAndPreservesUnicode() {
        assertEquals("👍", DisguiseSanitizer.mapStickerToSafeEmoji(""))
        assertEquals("👍", DisguiseSanitizer.mapStickerToSafeEmoji("+1"))
        assertEquals("🎉", DisguiseSanitizer.mapStickerToSafeEmoji("🎉"))

        val mappedPack = DisguiseSanitizer.mapStickerToSafeEmoji("mygo:cry")
        val mappedToken = DisguiseSanitizer.mapStickerToSafeEmoji("[bocchi:guitar]")
        assertTrue(mappedPack in DisguiseSanitizer.SAFE_DISGUISE_EMOJIS)
        assertTrue(mappedToken in DisguiseSanitizer.SAFE_DISGUISE_EMOJIS)
        assertEquals(mappedPack, DisguiseSanitizer.mapStickerToSafeEmoji("mygo:cry"))
    }

    @Test
    fun mapColleagueNameIsDeterministicWhenEnabledAndRespectsFallback() {
        val alice = DisguiseSanitizer.mapColleagueName(seedId = 42L, fallbackAuthor = "欢友A", enabled = true)
        val aliceAgain = DisguiseSanitizer.mapColleagueName(seedId = 42L, fallbackAuthor = "其他名字", enabled = true)
        assertEquals(alice, aliceAgain)
        assertTrue(alice in FeishuDisguiseHelpers.IM_NAMES)

        assertEquals("真实昵称", DisguiseSanitizer.mapColleagueName(seedId = 42L, fallbackAuthor = "真实昵称", enabled = false))
        assertEquals("同事", DisguiseSanitizer.mapColleagueName(seedId = 42L, fallbackAuthor = "   ", enabled = false))
    }

    @Test
    fun groupAndChannelAliasesRespectCustomOverridesAndContainZeroForbiddenTerms() {
        assertEquals(
            "Q4 交付专班",
            DisguiseSanitizer.resolveFeishuGroupAlias(
                groupId = 1L,
                rawName = "尽欢主群",
                customAliases = mapOf(1L to "Q4 交付专班"),
            ),
        )
        for (gid in 1L..20L) {
            val feishuAlias = DisguiseSanitizer.resolveFeishuGroupAlias(gid, "鱼鱼谷-$gid")
            assertEquals(emptyList<String>(), DisguiseSanitizer.findForbiddenTerms(feishuAlias))
            val ldAlias = DisguiseSanitizer.resolveLinuxDoChannelAlias(gid, "摸鱼群-$gid")
            assertEquals(emptyList<String>(), DisguiseSanitizer.findForbiddenTerms(ldAlias))
        }
    }

    @Test
    fun sanitizeSummaryScrubsAllGiveawayTermsAndTokensInDisguiseModes() {
        val leaky = "来尽欢 ForHappy (52dmeme.com) 的图廊和鱼鱼谷摸鱼领 10 D币 / 5.0 LDC，" +
            "开启 ECH 与 Edge IP [mygo:happy] [书房摘录]第1章"

        // Normal mode leaves strings unchanged
        assertEquals(leaky, DisguiseSanitizer.sanitizeSummaryForDisguise(leaky, AppDisguiseMode.NONE))

        // Feishu & LINUX DO remove every forbidden term and sticker/novel token
        listOf(AppDisguiseMode.FEISHU, AppDisguiseMode.LINUX_DO).forEach { mode ->
            val cleaned = DisguiseSanitizer.sanitizeSummaryForDisguise(leaky, mode)
            val remaining = DisguiseSanitizer.findForbiddenTerms(cleaned)
            assertEquals("Mode $mode still leaked $remaining in: $cleaned", emptyList<String>(), remaining)
            assertFalse(cleaned.contains("[mygo:happy]"))
            assertFalse(cleaned.contains("[书房摘录]"))
            assertTrue(cleaned.contains("[表情]"))
            assertTrue(cleaned.contains("[文档摘录]"))
        }
    }

    @Test
    fun linuxDoTopicAndFeishuGroupPoolsAndIconAliasesHaveZeroGiveawayTerms() {
        assertEquals(emptyList<String>(), DisguiseSanitizer.findForbiddenTerms(LinuxDoTopicMeta.TITLE))
        LinuxDoTopicMeta.TAGS.forEach { tag ->
            assertEquals(emptyList<String>(), DisguiseSanitizer.findForbiddenTerms(tag))
        }
        FeishuDisguiseHelpers.IM_GROUPS.forEach { group ->
            assertEquals("Group $group contains forbidden term", emptyList<String>(), DisguiseSanitizer.findForbiddenTerms(group))
        }
        FeishuDisguiseHelpers.IM_NAMES.forEach { name ->
            assertEquals(emptyList<String>(), DisguiseSanitizer.findForbiddenTerms(name))
        }
        AppIconAlias.entries.forEach { alias ->
            val safeTitle = safeIconAliasTitle(alias)
            assertEquals(emptyList<String>(), DisguiseSanitizer.findForbiddenTerms(safeTitle))
        }
        assertNotEquals("尽欢", safeIconAliasTitle(AppIconAlias.JINHUAN))
    }

    @Test
    fun stealthStateTransitionsPreserveBlurAndReturnDisguiseInvariants() {
        val start = PrivacyStealthConfig(
            disguiseMode = AppDisguiseMode.NONE,
            blurAllEnabled = false,
            autoBlurOnEnterDisguise = true,
            keepBlurAfterBossExit = true,
        )

        // Enter Feishu disguise -> saves blurBeforeDisguise=false & sets blurAllEnabled=true
        val inFeishu = DisguiseSanitizer.applyDisguiseSwitch(start, AppDisguiseMode.FEISHU)
        assertEquals(AppDisguiseMode.FEISHU, inFeishu.disguiseMode)
        assertFalse(inFeishu.blurBeforeDisguise)
        assertTrue(inFeishu.blurAllEnabled)

        // Switch disguise Feishu -> LINUX DO keeps blur state & blurBeforeDisguise
        val inLinuxDo = DisguiseSanitizer.applyDisguiseSwitch(inFeishu, AppDisguiseMode.LINUX_DO)
        assertEquals(AppDisguiseMode.LINUX_DO, inLinuxDo.disguiseMode)
        assertFalse(inLinuxDo.blurBeforeDisguise)
        assertTrue(inLinuxDo.blurAllEnabled)

        // Trigger Boss Key -> records bossReturnDisguise = LINUX_DO
        val inBoss = DisguiseSanitizer.applyEnterBoss(inLinuxDo, alreadyBoss = false)
        assertTrue(inBoss.isBossPersisted)
        assertEquals(AppDisguiseMode.LINUX_DO, inBoss.bossReturnDisguise)

        // Repeated Boss trigger (e.g. background while already in Boss) preserves original return target
        val stillBoss = DisguiseSanitizer.applyEnterBoss(inBoss, alreadyBoss = true)
        assertEquals(AppDisguiseMode.LINUX_DO, stillBoss.bossReturnDisguise)

        // Exit Boss Key -> returns to LINUX_DO & keeps blurAllEnabled=true
        val afterBoss = DisguiseSanitizer.applyExitBoss(stillBoss)
        assertFalse(afterBoss.isBossPersisted)
        assertEquals(AppDisguiseMode.LINUX_DO, afterBoss.disguiseMode)
        assertTrue(afterBoss.blurAllEnabled)

        // Exit Disguise back to NONE -> restores original blurBeforeDisguise=false
        val backNormal = DisguiseSanitizer.applyDisguiseSwitch(afterBoss, AppDisguiseMode.NONE)
        assertEquals(AppDisguiseMode.NONE, backNormal.disguiseMode)
        assertFalse(backNormal.blurAllEnabled)
    }
}
