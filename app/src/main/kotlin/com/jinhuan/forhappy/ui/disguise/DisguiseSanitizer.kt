package com.jinhuan.forhappy.ui.disguise

import com.jinhuan.forhappy.core.model.AppDisguiseMode
import com.jinhuan.forhappy.core.model.PrivacyStealthConfig
import kotlin.math.abs

/**
 * Pure-Kotlin sanitization & identity-mapping helpers for zero-giveaway Feishu / LINUX DO disguises
 * (`docs/redesign/03-privacy-state-machine.md` §5.3 & §5.7).
 */
object DisguiseSanitizer {
    val FORBIDDEN_DISGUISE_TERMS: List<String> = listOf(
        "尽欢",
        "ForHappy",
        "52dmeme",
        "图廊",
        "鱼鱼谷",
        "摸鱼",
        "D币",
        "ECH",
        "Edge IP",
    )

    val SAFE_DISGUISE_EMOJIS: List<String> = listOf(
        "👍", "😂", "✅", "🎉", "👏", "🤔", "❤️", "🔥",
    )

    val FEISHU_WORK_PLACEHOLDERS: List<String> = listOf(
        "同步一下截图",
        "见附图，供参考",
        "这块大家看下",
        "最新进度同步",
        "归档留存",
    )

    val FEISHU_WORK_GROUP_ALIASES: List<String> = listOf(
        "研发中心-全员群",
        "产品设计协同群",
        "平台架构讨论组",
        "质量保障专项群",
        "运营推进同步群",
        "交付保障工作组",
    )

    val LINUX_DO_CHANNEL_ALIASES: List<String> = listOf(
        "研发交流",
        "技术讨论",
        "资源荟萃",
        "日常吹水",
    )

    private val STICKER_TOKEN_REGEX = Regex("""\[[a-z][a-z0-9_-]{0,31}:[a-z0-9_-]{1,48}\]""")
    private val NOVEL_EXCERPT_REGEX = Regex("""\[书房摘录][^\n]*""")

    fun mapStickerToSafeEmoji(rawKey: String): String {
        val clean = rawKey.trim()
        if (clean.isEmpty()) return "👍"
        if (clean == "+1") return "👍"
        if (!clean.contains(':') && !clean.startsWith('[')) return clean
        val idx = abs(clean.hashCode()) % SAFE_DISGUISE_EMOJIS.size
        return SAFE_DISGUISE_EMOJIS[idx]
    }

    fun mapColleagueName(seedId: Long, fallbackAuthor: String, enabled: Boolean): String {
        if (!enabled) return fallbackAuthor.ifBlank { "同事" }
        val pool = FeishuDisguiseHelpers.IM_NAMES
        val seed = if (seedId != 0L) seedId else fallbackAuthor.hashCode().toLong()
        val idx = ((seed and 0x7FFFFFFFL) % pool.size.toLong()).toInt()
        return pool[idx]
    }

    fun resolveFeishuGroupAlias(
        groupId: Long,
        @Suppress("UNUSED_PARAMETER") rawName: String,
        customAliases: Map<Long, String> = emptyMap(),
    ): String {
        customAliases[groupId]?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
        val idx = abs((groupId xor 0x2F).toInt()) % FEISHU_WORK_GROUP_ALIASES.size
        return FEISHU_WORK_GROUP_ALIASES[idx]
    }

    fun resolveLinuxDoChannelAlias(groupId: Long, @Suppress("UNUSED_PARAMETER") rawName: String): String {
        val idx = abs((groupId xor 0x1D).toInt()) % LINUX_DO_CHANNEL_ALIASES.size
        return LINUX_DO_CHANNEL_ALIASES[idx]
    }

    fun workPlaceholder(seedId: Long): String {
        val idx = abs((seedId % FEISHU_WORK_PLACEHOLDERS.size).toInt())
        return FEISHU_WORK_PLACEHOLDERS[idx]
    }

    fun sanitizeSummaryForDisguise(raw: String, mode: AppDisguiseMode): String {
        if (mode == AppDisguiseMode.NONE) return raw
        var s = STICKER_TOKEN_REGEX.replace(raw, "[表情]")
        s = NOVEL_EXCERPT_REGEX.replace(s, "[文档摘录]")
        s = s
            .replace("ForHappy", if (mode == AppDisguiseMode.FEISHU) "协作平台" else "社区", ignoreCase = true)
            .replace("52dmeme.com", if (mode == AppDisguiseMode.FEISHU) "docs.example.cn" else "linux.do", ignoreCase = true)
            .replace("52dmeme", if (mode == AppDisguiseMode.FEISHU) "协同文档" else "LINUX DO", ignoreCase = true)
            .replace("尽欢", if (mode == AppDisguiseMode.FEISHU) "团队" else "社区")
            .replace("鱼鱼谷", "项目空间")
            .replace("摸鱼", "日常")
            .replace("图廊", "共享资料")
            .replace("D币", "积分")
            .replace("LDC", "积分")
            .replace("Edge IP", "节点")
            .replace(Regex("""\bECH\b"""), "安全通道")
        return s
    }

    fun findForbiddenTerms(text: String): List<String> =
        FORBIDDEN_DISGUISE_TERMS.filter { term -> text.contains(term, ignoreCase = true) }

    fun applyDisguiseSwitch(cur: PrivacyStealthConfig, nextMode: AppDisguiseMode): PrivacyStealthConfig {
        if (cur.disguiseMode == nextMode) return cur
        val entering = cur.disguiseMode == AppDisguiseMode.NONE && nextMode != AppDisguiseMode.NONE
        val leaving = cur.disguiseMode != AppDisguiseMode.NONE && nextMode == AppDisguiseMode.NONE
        return when {
            entering -> cur.copy(
                disguiseMode = nextMode,
                blurBeforeDisguise = cur.blurAllEnabled,
                blurAllEnabled = if (cur.autoBlurOnEnterDisguise) true else cur.blurAllEnabled,
            )
            leaving -> cur.copy(
                disguiseMode = AppDisguiseMode.NONE,
                blurAllEnabled = if (cur.autoBlurOnEnterDisguise) cur.blurBeforeDisguise else cur.blurAllEnabled,
            )
            else -> cur.copy(disguiseMode = nextMode)
        }
    }

    fun applyEnterBoss(cur: PrivacyStealthConfig, alreadyBoss: Boolean): PrivacyStealthConfig = cur.copy(
        isBossPersisted = true,
        bossReturnDisguise = if (alreadyBoss) cur.bossReturnDisguise else cur.disguiseMode,
    )

    fun applyExitBoss(cur: PrivacyStealthConfig): PrivacyStealthConfig = cur.copy(
        isBossPersisted = false,
        disguiseMode = cur.bossReturnDisguise,
        blurAllEnabled = if (cur.keepBlurAfterBossExit) true else cur.blurAllEnabled,
    )
}
