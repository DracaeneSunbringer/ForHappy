package com.jinhuan.forhappy.core.cosmetics

import com.jinhuan.forhappy.core.clock.ServerClock
import com.jinhuan.forhappy.core.model.ChatMessage
import com.jinhuan.forhappy.core.model.DecoConfig
import com.jinhuan.forhappy.core.model.FarmBadgeMeta
import com.jinhuan.forhappy.core.model.ForHappyJson
import com.jinhuan.forhappy.core.model.GrowthRewardMeta
import com.jinhuan.forhappy.core.model.MeResponse
import com.jinhuan.forhappy.core.model.OfficialTitleMeta
import com.jinhuan.forhappy.core.network.api.ForHappyApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet

sealed interface BadgeArtKind {
    data class BundledSvg(val assetPath: String) : BadgeArtKind
    data class AtlasV4(
        val tileIndex: Int,
        val style: String,
        val isHonor: Boolean = false,
        val isDay: Boolean = false,
    ) : BadgeArtKind
    data class InlineOfficialSvg(val innerSvg: String, val styleId: String = "serene") : BadgeArtKind
    data class RemoteImage(val url: String) : BadgeArtKind
    data class RemoteSvg(val url: String, val fallbackGlyph: String = "✦") : BadgeArtKind
    data class InlineXmlSvg(val xml: String) : BadgeArtKind
    data class SpriteSheetCell(
        val url: String,
        val cols: Int,
        val rows: Int,
        val col: Int,
        val row: Int,
    ) : BadgeArtKind
    data class CommunityCssBadge(
        val id: String,
        val imgSha: String? = null,
        val cssSha: String? = null,
        val isDarkTheme: Boolean = false,
    ) : BadgeArtKind
    data class EmojiGlyph(val glyph: String) : BadgeArtKind
}

data class BadgeCatalogEntry(
    val id: String,
    val name: String,
    val tier: String,
    val glyph: String,
    val desc: String,
    val unit: String = "次",
    val levels: List<Int> = BadgeAndCosmeticRegistry.DEFAULT_BADGE_LEVELS,
)

@Serializable
data class RaffleTitleCatalogItem(
    val id: String,
    val name: String,
    val mood: String,
    val colorHex: String,
    val accentHex: String,
    val rarity: String,
    val symbolKey: String = "",
)

@Serializable
data class PersonalTitleCatalogItem(
    val id: String,
    val name: String,
    val mood: String,
    val rarity: String,
    val art: String,
)

@Serializable
data class WebCosmeticCatalogSnapshot(
    val raffleTitles: List<RaffleTitleCatalogItem> = emptyList(),
    val personalTitles: List<PersonalTitleCatalogItem> = emptyList(),
    val growthV5Ids: List<String> = emptyList(),
    val v2ArtSpecs: Map<String, List<Float>> = emptyMap(),
    val v1Palettes: Map<String, List<String>> = emptyMap(),
    val titleSymbols: Map<String, String> = emptyMap(),
)

data class CommunitySkinCatalogItem(
    val id: String,
    val name: String,
    val author: String,
    val hasTitle: Boolean,
    val hasBadge: Boolean,
    val hasBubble: Boolean,
    val hasReply: Boolean = false,
    val bubbleImgSha: String? = null,
    val bubbleCssSha: String? = null,
    val bubbleSlice: IntArray? = null,
    val titleImgSha: String? = null,
    val titleCssSha: String? = null,
    val titleSlice: IntArray? = null,
    val badgeImgSha: String? = null,
    val badgeCssSha: String? = null,
    val replyImgSha: String? = null,
    val replyCssSha: String? = null,
    val replySlice: IntArray? = null,
    val hasFrame: Boolean = false,
    val frameImgSha: String? = null,
    val frameCssSha: String? = null,
)

data class OwnedBadgeItem(
    val id: String,
    val count: Int,
    val level: Int,
    val levelText: String,
    val nextAt: Int,
    val remainingToNext: Int,
    val lastDay: String?,
    val owned: Boolean,
    val meta: BadgeCatalogEntry,
    val art: BadgeArtKind,
)

data class ResolvedTitleChip(
    val key: String,
    val label: String,
    val levelText: String? = null,
    val rarity: String = "normal",
    val bgHex: String = "#1F6B4F",
    val textHex: String = "#FFFFFF",
    val borderHex: String = "#1F6B4F",
    val fillMode: String = "solid",
    val shape: String = "pill",
    val secondaryBgHex: String? = null,
    val effect: String = "none",
    val fontKey: String = "",
    val symbolSvgPath: String? = null,
    val bgImageUrl: String? = null,
    val specialKind: String = "standard",
    val customCss: ParsedSafeCss? = null,
    val leadingIconUrl: String? = null,
    val trailingIconUrl: String? = null,
    val dynamicCssSpec: String? = null,
    val personalV2Art: PersonalV2ArtSpec? = null,
    val communitySpec: CommunityTitleRenderSpec? = null,
    val officialPalette: OfficialPaletteSpec? = null,
    val raffleColorHex: String? = null,
    val raffleAccentHex: String? = null,
    val raffleStage: Int = 1,
    val hardShadowHex: String? = null,
    val hardShadowOffsetDp: Pair<Float, Float>? = null,
) {
    val displayLabel: String
        get() = if (levelText.isNullOrBlank()) label else "$label $levelText"
}

data class ResolvedBadgeChip(
    val id: String,
    val name: String,
    val level: Int,
    val levelText: String,
    val tier: String,
    val desc: String,
    val art: BadgeArtKind,
    val alwaysIconOnly: Boolean = false,
    val textHex: String = "#334155",
    val levelBgHex: String? = null,
    val iconWidthDp: Float = 24f,
    val iconHeightDp: Float = 24f,
    val dynamicCssSpec: String? = null,
)

data class ShowcaseItem(
    val key: String,
    val label: String,
    val chip: ResolvedBadgeChip,
)

data class SelectableCosmeticEntry(
    val key: String,
    val name: String,
    val category: String,
    val subtitle: String = "",
)

data class ResolvedUserIdentity(
    val title: ResolvedTitleChip? = null,
    val badge: ResolvedBadgeChip? = null,
    val nickGradientHexes: List<String> = emptyList(),
)

/**
 * Authoritative cosmetic & badge registry aligned 1:1 with `kourou25/meme-hall-frontend`
 * (`app.js`, `badge-art.js`, `growth-v5.js`, `title-collection.js`, `personal-catalog.js`,
 * `personal-v2-catalog.js`) and `kourou25/meme-hall-skins` (`catalog.json`).
 */
object BadgeAndCosmeticRegistry {

    val DEFAULT_BADGE_LEVELS = listOf(1, 3, 7, 15, 30)
    private val GUIDE_BADGE_LEVELS = listOf(1, 3, 10, 30)
    private val GROWTH_BADGE_LEVELS = listOf(1, 3, 10, 30, 100)
    private val BADGE_LV_NAMES = listOf("", "铜", "银", "金", "彩虹", "传说")
    const val TITLE_MAX_LEVEL = 5200
    val TITLE_STAGES = listOf(1, 10, 50, 100, 300, 800, 1600, 2600, 4000, 5200)
    val ADMIN_RAINBOW_HEXES = listOf("#e2557a", "#e3a33b", "#3bb58e", "#4b8be0")
    val DEFAULT_FANCY_RAINBOW_HEXES = listOf("#ff4d8d", "#ffb238", "#3bd18e", "#4b9bff", "#b06bff", "#ff4d8d")

    private val BUILT_IN_OFFICIAL_NAMES: Map<String, Pair<String, String>> = mapOf(
        "serene" to ("清欢" to "云淡风轻"),
        "gentleman" to ("绅士" to "翩翩有礼"),
        "coder" to ("码农" to "键盘生花"),
        "banana" to ("大蕉" to "能量拉满"),
        "peach" to ("蜜桃" to "甜度超标"),
        "oracle" to ("先知" to "洞察先机"),
        "whale" to ("蓝鲸" to "深海巡游"),
        "diver" to ("潜水" to "深潜摸鱼"),
        "reborn" to ("重生" to "浴火归来"),
    )

    data class ActiveRaffleInfo(
        val catalog: RaffleTitleCatalogItem,
        val earnedLevel: Int,
        val displayLevel: Int,
        val stage: Int,
    )

    fun isOwner52Suit(id: String?): Boolean =
        id == "owner-52di" || id == "owner-52b" || id == "owner52" || id == "owner52b"

    fun raffleStage(level: Int): Int {
        val capped = level.coerceIn(1, TITLE_MAX_LEVEL)
        return TITLE_STAGES.count { it <= capped }.coerceIn(1, 10)
    }

    val GROWTH_V5_IDS: Set<String> = setOf(
        "resource_respect", "card_draws", "card_wins", "card_dry",
        "scratch_draws", "scratch_wins", "scratch_jackpot", "scratch_dry",
        "lotto_wins", "coins_peak", "market_trades", "market_sold",
        "raid_wins", "defend_wins", "uprising_wins",
        "farm_plant", "farm_steal", "farm_harvest", "farm_discovery",
        "ranch_collected", "guardian_care",
        "fishing_landed", "fishing_treasure", "fishing_escaped", "fishing_discovery",
        "farm_water", "farm_pest",
    )

    private val ATLAS_V4_TILES: Map<String, Int> = mapOf(
        "guess" to 0,
        "luck_king" to 1,
        "luck_last" to 2,
        "guide" to 3,
        "newbie" to 4,
        "moyu_god" to 5,
        "sese" to 6,
        "water" to 7,
        "react" to 8,
        "night" to 9,
        "farm_harvest" to 10,
        "farm_steal" to 11,
        "farm_plant" to 12,
        "farm_discovery" to 14,
        "farm_wood" to 15,
        "elder" to 17,
        "post_god" to 18,
        "post_pro" to 19,
        "moyu_pro" to 20,
        "guess_pressure" to 38,
        "guess_tormentor" to 39,
        "guess_firstshot" to 40,
        "guess_easy" to 41,
        "chunse" to 43,
    )

    private val RANK_BOARDS = listOf(
        "guess", "guess_winrate", "sender_count", "sender_paid", "chat_count", "chat_early", "chat_late",
    )
    private val RANK_REGEX = Regex("^rank_(guess_winrate|guess|sender_count|sender_paid|chat_count|chat_early|chat_late)_(day|all)$")

    val OFFICIAL_ART_SVG: Map<String, String> = mapOf(
        "serene" to """<ellipse cx="16" cy="6" rx="9" ry="3"/><path d="M7 16q9 14 18 0M9 15l4 1m6 0 4-1M16 23v4m-4-1h8"/>""",
        "gentleman" to """<path d="M7 10 4 3l9 4m12 3 3-7-9 4M16 27S3 20 3 13c0-7 10-8 13-2 3-6 13-5 13 2 0 7-13 14-13 14Z"/><path d="m8 16 4 2m8 0 4-2m-12 6q4 2 8 0"/>""",
        "coder" to """<rect x="3" y="5" width="26" height="22" rx="3"/><path d="M3 11h26M7 8h1m3 0h1m-1 7-4 4 4 4m10-8 4 4-4 4m-3-9-4 10"/>""",
        "banana" to """<path d="M23 4C30 19 17 31 5 25c9-1 16-8 17-18l-1-3Zm-1 5C17 18 10 19 5 18m0 0 3 7M7 8 5 6m4-1V2"/><path d="m18 18 1 1m3-3 1 1m-5 5q3 2 5-2"/>""",
        "peach" to """<path d="M16 10C7 4 1 13 5 22s15 9 21 1c7-9-1-17-10-13Z"/><path d="M16 10c-3 4-3 8 0 11M16 8c1-6 8-6 8-6-1 6-8 6-8 6M8 17h2m12 0h2m-13 7q5 3 10-1"/>""",
        "oracle" to """<rect x="7" y="7" width="18" height="18" rx="5"/><path d="M11 2v5m10-5v5M11 25v5m10-5v5M2 11h5m-5 10h5m18-10h5m-5 10h5M16 10l2 4 4 2-4 2-2 4-2-4-4-2 4-2Z"/>""",
        "whale" to """<path d="M3 20c0-9 17-14 20-3l6-3-1 9-5-1C19 31 5 29 3 20ZM12 9V4m0 0L8 6m4-2 4 2"/><circle cx="8" cy="19" r="1"/><path d="M11 23q4 2 7-1"/>""",
        "diver" to """<path d="M5 13h22v9a7 7 0 0 1-7 7h-8a7 7 0 0 1-7-7ZM20 13V5h7v5h-3v3M4 8l3-2m-5 6 3-1"/><rect x="8" y="16" width="16" height="7" rx="3"/><path d="M12 19h1m6 0h1"/>""",
        "reborn" to """<path d="M5 11l5 4 6-10 6 10 5-4-2 15H7ZM8 22h16M12 18h1m6 0h1M3 5l2 2m22-2 2-2"/>""",
    )

    private val BUILT_IN_BADGES: LinkedHashMap<String, BadgeCatalogEntry> = linkedMapOf(
        "luck_king" to BadgeCatalogEntry("luck_king", "手气王", "fun", "♛", "拼手气红包确认结束后金额最多；1 / 3 / 10 / 30 / 100 次升级", "次", GROWTH_BADGE_LEVELS),
        "luck_last" to BadgeCatalogEntry("luck_last", "手气亡", "epic", "☾", "拼手气红包确认结束后金额最少；1 / 3 / 10 / 30 / 100 次升级", "次", GROWTH_BADGE_LEVELS),
        "guess" to BadgeCatalogEntry("guess", "猜词星章", "legend", "✧", "累计猜中 1 / 3 / 10 / 30 / 100 轮，解锁五级称号与徽章", "轮", GROWTH_BADGE_LEVELS),
        "guide" to BadgeCatalogEntry("guide", "引路人", "rare", "🧭", "邀请好友满 7 天并活跃互动；1 / 3 / 10 / 30 人升级", "人", GUIDE_BADGE_LEVELS),
        "newbie" to BadgeCatalogEntry("newbie", "初来乍到", "fun", "🌱", "通过好友邀请加入尽欢"),
        "elder" to BadgeCatalogEntry("elder", "太上长老", "legend", "☯", "2026-10-01 开站当天进站的元老", "次", listOf(1)),
        "moyu_god" to BadgeCatalogEntry("moyu_god", "摸鱼之神", "epic", "🐟", "某天看图最多的人（至少 500 张）"),
        "moyu_pro" to BadgeCatalogEntry("moyu_pro", "摸鱼高手", "rare", "🎣", "一天看了 200 张图以上"),
        "post_god" to BadgeCatalogEntry("post_god", "发图之神", "epic", "🖼", "某天发图最多的人（至少 10 张）"),
        "post_pro" to BadgeCatalogEntry("post_pro", "发图达人", "rare", "📸", "一天发了 5 张图以上"),
        "sese" to BadgeCatalogEntry("sese", "色色冠军", "fun", "🍑", "某天发 NSFW 最多的人（至少 3 张）"),
        "chunse" to BadgeCatalogEntry("chunse", "纯色迷", "fun", "👀", "某天看 NSFW 最多的人（至少 50 张）"),
        "water" to BadgeCatalogEntry("water", "水群王", "rare", "💦", "某天群聊发言最多的人（至少 50 条）"),
        "react" to BadgeCatalogEntry("react", "回应达人", "rare", "💬", "某天回应 / 表情最多的人（至少 30 次）"),
        "night" to BadgeCatalogEntry("night", "夜猫子", "fun", "🌙", "某天凌晨 1～4 点看图最多的人（至少 50 张）"),
        // Built-in fallbacks for v5 farm / activity / rank series (overridden live by /api/me)
        "farm_plant" to BadgeCatalogEntry("farm_plant", "春耕播种", "rare", "✿", "农场累计播种作物达成阶段目标", "株"),
        "farm_water" to BadgeCatalogEntry("farm_water", "甘霖浇灌", "rare", "✿", "农场累计浇水照料作物达成阶段目标", "次"),
        "farm_harvest" to BadgeCatalogEntry("farm_harvest", "五谷丰登", "rare", "✿", "农场累计收获成熟作物", "次"),
        "farm_steal" to BadgeCatalogEntry("farm_steal", "顺手牵羊", "fun", "✿", "拜访好友农场顺手采摘收获", "次"),
        "farm_pest" to BadgeCatalogEntry("farm_pest", "护苗卫士", "rare", "✿", "农场清理害虫杂草守护作物", "次"),
        "farm_discovery" to BadgeCatalogEntry("farm_discovery", "作物图鉴", "epic", "✿", "解锁农场作物与变异图鉴", "种"),
        "farm_wood" to BadgeCatalogEntry("farm_wood", "林间伐木", "rare", "✿", "鱼鱼谷林地采集木材", "次"),
        "ranch_collected" to BadgeCatalogEntry("ranch_collected", "牧场拾珍", "rare", "✿", "牧场收集动物产出", "次"),
        "guardian_care" to BadgeCatalogEntry("guardian_care", "守护陪伴", "epic", "✿", "精心照料守护伙伴成长", "次"),
        "fishing_landed" to BadgeCatalogEntry("fishing_landed", "满载渔获", "rare", "✿", "鱼鱼谷成功垂钓上岸", "尾"),
        "fishing_treasure" to BadgeCatalogEntry("fishing_treasure", "深海寻宝", "epic", "✿", "垂钓发现稀有宝箱与奇珍", "次"),
        "fishing_escaped" to BadgeCatalogEntry("fishing_escaped", "擦肩大鱼", "fun", "✿", "垂钓时大鱼脱钩溜走纪念", "次"),
        "fishing_discovery" to BadgeCatalogEntry("fishing_discovery", "百鱼图鉴", "epic", "✿", "点亮鱼鱼谷水域鱼类图鉴", "种"),
        "coins_peak" to BadgeCatalogEntry("coins_peak", "富甲一方", "legend", "✿", "持有金币峰值创下新高", "币"),
        "market_trades" to BadgeCatalogEntry("market_trades", "集市常客", "rare", "✿", "参与集市交易互通有无", "次"),
        "market_sold" to BadgeCatalogEntry("market_sold", "商海行家", "epic", "✿", "集市出售物产累计达成目标", "次"),
        "card_draws" to BadgeCatalogEntry("card_draws", "集卡猎手", "rare", "✿", "参与集卡抽取探索图鉴", "抽"),
        "card_wins" to BadgeCatalogEntry("card_wins", "欧气集卡", "epic", "✿", "集卡抽中稀有高阶藏品", "次"),
        "card_dry" to BadgeCatalogEntry("card_dry", "保底坚守", "fun", "✿", "集卡连续未出高阶但永不言弃", "抽"),
        "scratch_draws" to BadgeCatalogEntry("scratch_draws", "刮奖常客", "rare", "✿", "参与刮刮乐试试手气", "张"),
        "scratch_wins" to BadgeCatalogEntry("scratch_wins", "刮奖锦鲤", "epic", "✿", "刮刮乐成功刮出奖励", "次"),
        "scratch_jackpot" to BadgeCatalogEntry("scratch_jackpot", "头奖天选", "legend", "✿", "刮刮乐一举命中头奖", "次"),
        "scratch_dry" to BadgeCatalogEntry("scratch_dry", "再接再厉", "fun", "✿", "刮刮乐擦肩而过的趣味纪念", "次"),
        "lotto_wins" to BadgeCatalogEntry("lotto_wins", "乐透之星", "epic", "✿", "乐透开奖命中幸运号码", "次"),
        "raid_wins" to BadgeCatalogEntry("raid_wins", "突袭先锋", "epic", "✿", "鱼鱼谷攻防突袭获胜", "次"),
        "defend_wins" to BadgeCatalogEntry("defend_wins", "固若金汤", "epic", "✿", "成功抵御来袭守护家园", "次"),
        "uprising_wins" to BadgeCatalogEntry("uprising_wins", "逆境反击", "epic", "✿", "攻防逆袭取得胜利", "次"),
        "resource_respect" to BadgeCatalogEntry("resource_respect", "薪火相传", "legend", "✦", "社区资源分享与致敬荣誉徽章", "次"),
        "guess_pressure" to BadgeCatalogEntry("guess_pressure", "绝境破译", "epic", "✧", "猜词倒计时高压时刻一击命中", "次", GROWTH_BADGE_LEVELS),
        "guess_tormentor" to BadgeCatalogEntry("guess_tormentor", "出题鬼才", "epic", "✧", "发起高质量高难度猜词局", "次", GROWTH_BADGE_LEVELS),
        "guess_firstshot" to BadgeCatalogEntry("guess_firstshot", "首猜即中", "epic", "✧", "猜词局第一发直接命中答案", "次", GROWTH_BADGE_LEVELS),
        "guess_easy" to BadgeCatalogEntry("guess_easy", "秒懂默契", "rare", "✧", "闪电般迅速猜出答案", "次", GROWTH_BADGE_LEVELS),
    )

    val RAFFLE_TITLES: Map<String, RaffleTitleCatalogItem> = mapOf(
        "oldfarmer" to RaffleTitleCatalogItem("oldfarmer", "老农民", "一畦土地，一段相聚；田园暂别，收获长存", "#9b7334", "#b8cf91", "SSR"),
        "newcomer" to RaffleTitleCatalogItem("newcomer", "初见欢欢", "带着好奇，开启新的故事", "#659d83", "#f1ce86", "normal"),
        "mona" to RaffleTitleCatalogItem("mona", "莫奈之王", "睡莲与湖光，画中漫游", "#5e9b92", "#d8a4ca", "normal"),
        "lace" to RaffleTitleCatalogItem("lace", "夜绒蕾丝", "紫夜天鹅与细绒花纹", "#655790", "#dba6c1", "SSR"),
        "giant" to RaffleTitleCatalogItem("giant", "几把大蕉", "大香蕉队长，气势和快乐都拉满", "#d9a92f", "#4c8ba2", "normal"),
        "worship" to RaffleTitleCatalogItem("worship", "寄吧几拜", "认真接梗，虔诚拜蕉", "#a57744", "#e0c46c", "normal"),
        "muscle" to RaffleTitleCatalogItem("muscle", "筋肉欢欢", "元气拳套与柔软猫爪", "#c17857", "#efd77a", "normal"),
        "deepseek" to RaffleTitleCatalogItem("deepseek", "鲸鱼娘", "深海里的蓝色心动", "#488caf", "#b6dcec", "normal"),
        "deepseekupgrade" to RaffleTitleCatalogItem("deepseekupgrade", "鲸澜女仆", "鲸蓝水纹里的温柔守候", "#486dad", "#bda9dc", "SSR"),
        "maid" to RaffleTitleCatalogItem("maid", "鲸眠女仆", "深蓝裙摆里的温柔陪伴", "#497ca7", "#accee2", "SSR"),
        "whalepunch" to RaffleTitleCatalogItem("whalepunch", "鲸拳出击", "蓝色电光里的元气一拳", "#4c8ebc", "#f1cc68", "SSR"),
        "nightdevil" to RaffleTitleCatalogItem("nightdevil", "夜宴小恶魔", "玫瑰与夜色里的俏皮心动", "#9a547e", "#e8b0b3", "SSR"),
        "whalebuddy" to RaffleTitleCatalogItem("whalebuddy", "鲸伴", "小鲸一路同行，终有星海巡游", "#368bad", "#b1daef", "normal"),
        "gpt" to RaffleTitleCatalogItem("gpt", "GPT龙姬", "紫星白龙的灵感相伴", "#8864b3", "#c7dcb7", "SSR"),
        "claude" to RaffleTitleCatalogItem("claude", "拾光司书", "每次相遇，都留有光芒", "#b9824e", "#efcfa1", "SSR"),
        "gemini" to RaffleTitleCatalogItem("gemini", "Gemini星喵", "双星闪烁的小宇宙", "#8571bd", "#99c9da", "SSR"),
        "whitedragon" to RaffleTitleCatalogItem("whitedragon", "白龙绮姬", "银发与绮花的优雅相逢", "#698aa0", "#d8cbe9", "SSR"),
        "rascal" to RaffleTitleCatalogItem("rascal", "纯坏蛋", "偷走饼干的小恶魔，坏得很可爱", "#8461b1", "#f0b278", "normal"),
        "weirdo" to RaffleTitleCatalogItem("weirdo", "大变态", "表情失控的怪趣布偶，专门制造笑场", "#c16d90", "#8cc4ad", "normal"),
        "blacksilk" to RaffleTitleCatalogItem("blacksilk", "黑丝夜羽", "黑天鹅的银线与夜色礼服", "#536586", "#e6b1c7", "SSR"),
        "loli" to RaffleTitleCatalogItem("loli", "萝莉糖星", "全然童话的月兔魔法师与糖纸星星", "#a083ba", "#c3dbe9", "normal"),
        "electric" to RaffleTitleCatalogItem("electric", "电光团子", "圆滚滚的小电灵，越熟悉越活泼", "#4a91aa", "#eed47b", "normal"),
        "railcannon" to RaffleTitleCatalogItem("railcannon", "乳汁炮", "银白缎带蓄能，满级点亮蓝白星光", "#407eac", "#d6e8eb", "SSR"),
        "chickengun" to RaffleTitleCatalogItem("chickengun", "鸡关枪", "怪趣双持，原版动图的连射节奏", "#ad686f", "#f3d4b7", "SSR"),
        "blade" to RaffleTitleCatalogItem("blade", "刀·赤狐", "赤狐守刀，落叶随行", "#b47054", "#d9bc7d", "normal"),
        "spear" to RaffleTitleCatalogItem("spear", "枪·青隼", "长风穿云，青隼振翅", "#4d8a8c", "#b6cdbb", "normal"),
        "sword" to RaffleTitleCatalogItem("sword", "剑·流萤", "银剑与流萤，共赴长夜", "#6d8eb4", "#c8d5ee", "SSR"),
        "halberd" to RaffleTitleCatalogItem("halberd", "戟·玄鹿", "山纹古戟，玄鹿守望", "#9c794c", "#bfd0b5", "SSR"),
        "snow" to RaffleTitleCatalogItem("snow", "雪·绒绒", "一团会打盹的雪，轻轻落下", "#709db4", "#e0e7f2", "normal"),
        "moon" to RaffleTitleCatalogItem("moon", "月·银兔", "抱着月亮打盹的小银兔", "#9381b4", "#e4d2a7", "SSR"),
        "wind" to RaffleTitleCatalogItem("wind", "风·纸鸢", "风带着纸鸢绕过你的肩头", "#5f9e8e", "#d6e2ac", "normal"),
        "flower" to RaffleTitleCatalogItem("flower", "花·春信", "花苞醒来，寄出一封春天", "#bd789b", "#afd0a1", "normal"),
        "sun" to RaffleTitleCatalogItem("sun", "日·暖橘", "太阳小狮子，暖意扑面", "#c8943d", "#f2ca97", "normal"),
        "star" to RaffleTitleCatalogItem("star", "星·许愿", "把星星揣进口袋，陪你闪光", "#7c7eb9", "#ecd39a", "SSR"),
        "cloud" to RaffleTitleCatalogItem("cloud", "云·软绵", "云朵羊的慢悠悠天空旅行", "#7dabb4", "#d9c9e8", "normal"),
    )

    val PERSONAL_TITLES: Map<String, PersonalTitleCatalogItem> = mapOf(
        "owner-52di" to PersonalTitleCatalogItem("owner-52di", "52帝", "站长专属", "SSR", "owner"),
        "owner-52b" to PersonalTitleCatalogItem("owner-52b", "52B", "限定巡展", "SSR", "owner"),
        "owner52" to PersonalTitleCatalogItem("owner52", "52帝", "站长专属", "SSR", "owner"),
        "owner52b" to PersonalTitleCatalogItem("owner52b", "52B", "限定巡展", "SSR", "owner"),
        "p001" to PersonalTitleCatalogItem("p001", "云团猫猫", "云团与猫耳的安静午后", "normal", "v1"),
        "p002" to PersonalTitleCatalogItem("p002", "摸鱼观图员", "抱着画框的小水獭", "normal", "v1"),
        "p003" to PersonalTitleCatalogItem("p003", "星夜领航员", "星尘里的小小领航员", "normal", "v1"),
        "p004" to PersonalTitleCatalogItem("p004", "脑洞喵喵", "猫耳灵感与不断冒出的新点子", "normal", "v1"),
        "p005" to PersonalTitleCatalogItem("p005", "枫叶小队长", "枫叶纸袋里的元气队长", "normal", "v1"),
        "p006" to PersonalTitleCatalogItem("p006", "排障工程鱼", "带着工具箱的排障小鱼", "normal", "v1"),
        "p007" to PersonalTitleCatalogItem("p007", "蓝帆旅人", "小帆船与蓝色远行", "normal", "v1"),
        "p008" to PersonalTitleCatalogItem("p008", "发条兔兔", "发条口袋里的软萌兔子", "normal", "v1"),
        "p009" to PersonalTitleCatalogItem("p009", "湖心小鱼", "水波与湖心花开的游鱼", "normal", "v1"),
        "p010" to PersonalTitleCatalogItem("p010", "干饭下班官", "准点下班的便当小熊", "normal", "v1"),
        "p011" to PersonalTitleCatalogItem("p011", "云朵邮差", "白云信封与小鸟邮差", "normal", "v1"),
        "p012" to PersonalTitleCatalogItem("p012", "王冠收藏家", "收藏小王冠的猫猫", "normal", "v1"),
        "p013" to PersonalTitleCatalogItem("p013", "干饭祝福官", "给大家送来饱饱祝福的猫头鹰", "normal", "v1"),
        "p014" to PersonalTitleCatalogItem("p014", "柠檬小精灵", "柠檬枝上的清新小精灵", "normal", "v1"),
        "p015" to PersonalTitleCatalogItem("p015", "拟物调试王", "锅盖与屏幕间的设计调试员", "normal", "v1"),
        "p016" to PersonalTitleCatalogItem("p016", "电波收集员", "把微小电波收进口袋", "normal", "v1"),
        "p017" to PersonalTitleCatalogItem("p017", "群聊收藏家", "书签与红狐的聊天收藏册", "normal", "v1"),
        "p018" to PersonalTitleCatalogItem("p018", "晴空小太阳", "抱着花朵的小太阳", "normal", "v1"),
        "p019" to PersonalTitleCatalogItem("p019", "解谜提示员", "给谜题留一条线索的兔兔", "normal", "v1"),
        "p020" to PersonalTitleCatalogItem("p020", "原图侦察员", "像素猫猫追踪原图线索", "normal", "v1"),
        "p021" to PersonalTitleCatalogItem("p021", "梗图求解王", "拿着放大镜的求知兔兔", "normal", "v1"),
        "p022" to PersonalTitleCatalogItem("p022", "拆图试验员", "试一试再拆一拆的实验小鼠", "normal", "v1"),
        "p023" to PersonalTitleCatalogItem("p023", "星尘旅客", "随星光旅行的小幽灵", "normal", "v1"),
        "p024" to PersonalTitleCatalogItem("p024", "下班飞行员", "披着小斗篷的下班飞行员", "normal", "v1"),
        "p025" to PersonalTitleCatalogItem("p025", "望星小猫", "望远镜旁的猫耳星旅", "normal", "v1"),
        "p026" to PersonalTitleCatalogItem("p026", "咕嘎说书人", "捧着故事卷的咕嘎小鸟", "normal", "v1"),
        "p027" to PersonalTitleCatalogItem("p027", "梗图鉴赏员", "青叶狐的梗图鉴赏手册", "normal", "v1"),
        "p028" to PersonalTitleCatalogItem("p028", "版图小蚂蚁", "背着地图探索板块的小蚂蚁", "normal", "v1"),
        "p029" to PersonalTitleCatalogItem("p029", "白菜寻宝员", "白菜叶中藏着一张寻宝券", "normal", "v1"),
        "p030" to PersonalTitleCatalogItem("p030", "原型鉴赏家", "海豹伙伴与原型设计屏幕", "normal", "v1"),
        "p031" to PersonalTitleCatalogItem("p031", "稻田小精灵", "金色稻穗里的田园精灵", "normal", "v1"),
        "p032" to PersonalTitleCatalogItem("p032", "红包起飞官", "抱着纸飞机的红包小柯基", "normal", "v1"),
        "p033" to PersonalTitleCatalogItem("p033", "气泡规划师", "月兔用尺子校准每一条边框", "normal", "v1"),
        "p034" to PersonalTitleCatalogItem("p034", "开站见证员", "举着开站小旗的见证伙伴", "normal", "v1"),
        "p035" to PersonalTitleCatalogItem("p035", "尽欢造物师", "猫耳工程师的小小造物台", "normal", "v1"),
        "p036" to PersonalTitleCatalogItem("p036", "词语魔术师", "鲸鱼书页里翻出的文字魔法", "normal", "v1"),
        "p037" to PersonalTitleCatalogItem("p037", "掌上护航员", "护着手机的小小铁甲伙伴", "normal", "v1"),
        "p038" to PersonalTitleCatalogItem("p038", "星光审稿官", "北极星上的需求记录板", "normal", "v1"),
        "p039" to PersonalTitleCatalogItem("p039", "千叶小鹿", "带着千片叶子的小鹿", "normal", "v1"),
        "p040" to PersonalTitleCatalogItem("p040", "紫色调试员", "紫发猫耳与插件调试台", "normal", "v1"),
        "p041" to PersonalTitleCatalogItem("p041", "模型咖啡师", "一杯咖啡配一场模型讨论", "normal", "v1"),
        "p042" to PersonalTitleCatalogItem("p042", "口令小NPC", "守着谜题钥匙的像素伙伴", "normal", "v1"),
        "p043" to PersonalTitleCatalogItem("p043", "红包观察员", "小蛋侦探记录红包动向", "normal", "v1"),
        "p044" to PersonalTitleCatalogItem("p044", "吼吼小绿植", "跟着节奏摇摆的小绿植", "normal", "v1"),
        "p045" to PersonalTitleCatalogItem("p045", "巧克力信使", "巧克力色信封里的温柔问候", "normal", "v1"),
        "p046" to PersonalTitleCatalogItem("p046", "跨屏小助手", "花朵与手机间的跨屏伙伴", "normal", "v1"),
        "p047" to PersonalTitleCatalogItem("p047", "糖盐小厨神", "番茄蛋与糖盐小勺", "normal", "v1"),
        "p048" to PersonalTitleCatalogItem("p048", "松果小骑士", "守护松果的小小骑士", "normal", "v1"),
        "p049" to PersonalTitleCatalogItem("p049", "开奖观察员", "放大镜旁的开奖记录券", "normal", "v1"),
        "p050" to PersonalTitleCatalogItem("p050", "额度囤囤鼠", "把模型额度收进星星罐", "normal", "v1"),
        "p051" to PersonalTitleCatalogItem("p051", "插件荷包蛋", "荷包蛋的插件小工具箱", "normal", "v1"),
        "p052" to PersonalTitleCatalogItem("p052", "掌上评测员", "红色猫耳与掌上体验记录", "normal", "v1"),
        "p053" to PersonalTitleCatalogItem("p053", "表情小村民", "表情旗帜下的小村民", "normal", "v1"),
        "p054" to PersonalTitleCatalogItem("p054", "需求联络鸟", "白鸟捎来的新需求便笺", "normal", "v1"),
        "p055" to PersonalTitleCatalogItem("p055", "工位潜行者", "悄悄端着咖啡的工位狐", "normal", "v1"),
        "p056" to PersonalTitleCatalogItem("p056", "冰糖星球", "冰蓝星球上的糖果光芒", "normal", "v1"),
        "p057" to PersonalTitleCatalogItem("p057", "风铃歌者", "风铃与小鸟合奏的旋律", "normal", "v1"),
        "p058" to PersonalTitleCatalogItem("p058", "月光纸鸢", "牵着纸鸢的月光兔兔", "normal", "v1"),
        "p059" to PersonalTitleCatalogItem("p059", "起飞申请员", "夕阳里盖好章的纸飞机", "normal", "v1"),
        "pv2-p001-doodle" to PersonalTitleCatalogItem("pv2-p001-doodle", "狮堂云团", "奶油白小狮子、云边、靛蓝发带", "normal", "v2"),
        "pv2-p001-ssr" to PersonalTitleCatalogItem("pv2-p001-ssr", "狮堂云团", "奶油白小狮子、云边、靛蓝发带", "SSR", "v2"),
        "pv2-p002-doodle" to PersonalTitleCatalogItem("pv2-p002-doodle", "四格摸鱼", "四格图窗、小水獭、浅海蓝", "normal", "v2"),
        "pv2-p002-ssr" to PersonalTitleCatalogItem("pv2-p002-ssr", "四格摸鱼", "四格图窗、小水獭、浅海蓝", "SSR", "v2"),
        "pv2-p003-doodle" to PersonalTitleCatalogItem("pv2-p003-doodle", "明日星旅", "晨曦、六颗小星、旅行帽", "normal", "v2"),
        "pv2-p003-ssr" to PersonalTitleCatalogItem("pv2-p003-ssr", "明日星旅", "晨曦、六颗小星、旅行帽", "SSR", "v2"),
        "pv2-p004-doodle" to PersonalTitleCatalogItem("pv2-p004-doodle", "琦喵灵感", "猫耳、灵感纸条、短句对话符", "normal", "v2"),
        "pv2-p004-ssr" to PersonalTitleCatalogItem("pv2-p004-ssr", "琦喵灵感", "猫耳、灵感纸条、短句对话符", "SSR", "v2"),
        "pv2-p005-doodle" to PersonalTitleCatalogItem("pv2-p005-doodle", "枫叶来信", "枫叶、奶油信笺、暖棕", "normal", "v2"),
        "pv2-p005-ssr" to PersonalTitleCatalogItem("pv2-p005-ssr", "枫叶来信", "枫叶、奶油信笺、暖棕", "SSR", "v2"),
        "pv2-p006-doodle" to PersonalTitleCatalogItem("pv2-p006-doodle", "肥鱼探路", "圆胖小鱼、问号罗盘、红包小签", "normal", "v2"),
        "pv2-p006-ssr" to PersonalTitleCatalogItem("pv2-p006-ssr", "肥鱼探路", "圆胖小鱼、问号罗盘、红包小签", "SSR", "v2"),
        "pv2-p007-doodle" to PersonalTitleCatalogItem("pv2-p007-doodle", "蓝帆坐标", "小帆船、双点坐标、青蓝", "normal", "v2"),
        "pv2-p007-ssr" to PersonalTitleCatalogItem("pv2-p007-ssr", "蓝帆坐标", "小帆船、双点坐标、青蓝", "SSR", "v2"),
        "pv2-p008-doodle" to PersonalTitleCatalogItem("pv2-p008-doodle", "迷宫微光", "迷宫小路、发光钥匙、软兔", "normal", "v2"),
        "pv2-p008-ssr" to PersonalTitleCatalogItem("pv2-p008-ssr", "迷宫微光", "迷宫小路、发光钥匙、软兔", "SSR", "v2"),
        "pv2-p009-doodle" to PersonalTitleCatalogItem("pv2-p009-doodle", "镜水游鱼", "镜面水纹、银鱼、花瓣", "normal", "v2"),
        "pv2-p009-ssr" to PersonalTitleCatalogItem("pv2-p009-ssr", "镜水游鱼", "镜面水纹、银鱼、花瓣", "SSR", "v2"),
        "pv2-p010-doodle" to PersonalTitleCatalogItem("pv2-p010-doodle", "午间小歇", "便当小熊、午间小钟、米黄", "normal", "v2"),
        "pv2-p010-ssr" to PersonalTitleCatalogItem("pv2-p010-ssr", "午间小歇", "便当小熊、午间小钟、米黄", "SSR", "v2"),
        "pv2-p011-doodle" to PersonalTitleCatalogItem("pv2-p011-doodle", "云笺旅人", "云朵、信笺、浅银蓝", "normal", "v2"),
        "pv2-p011-ssr" to PersonalTitleCatalogItem("pv2-p011-ssr", "云笺旅人", "云朵、信笺、浅银蓝", "SSR", "v2"),
        "pv2-p012-doodle" to PersonalTitleCatalogItem("pv2-p012-doodle", "小冠来客", "小王冠、奶油猫、金色单星", "normal", "v2"),
        "pv2-p012-ssr" to PersonalTitleCatalogItem("pv2-p012-ssr", "小冠来客", "小王冠、奶油猫、金色单星", "SSR", "v2"),
        "pv2-p013-doodle" to PersonalTitleCatalogItem("pv2-p013-doodle", "饱饱福鱼", "笑脸胖鱼、祝福小签、暖金", "normal", "v2"),
        "pv2-p013-ssr" to PersonalTitleCatalogItem("pv2-p013-ssr", "饱饱福鱼", "笑脸胖鱼、祝福小签、暖金", "SSR", "v2"),
        "pv2-p014-doodle" to PersonalTitleCatalogItem("pv2-p014-doodle", "柠檬初熟", "柠檬叶、半透明黄果、浅绿", "normal", "v2"),
        "pv2-p014-ssr" to PersonalTitleCatalogItem("pv2-p014-ssr", "柠檬初熟", "柠檬叶、半透明黄果、浅绿", "SSR", "v2"),
        "pv2-p015-doodle" to PersonalTitleCatalogItem("pv2-p015-doodle", "同频肥鱼", "胖鱼、锅盖帽、双对话符、小放大镜", "normal", "v2"),
        "pv2-p015-ssr" to PersonalTitleCatalogItem("pv2-p015-ssr", "同频肥鱼", "胖鱼、锅盖帽、双对话符、小放大镜", "SSR", "v2"),
        "pv2-p016-doodle" to PersonalTitleCatalogItem("pv2-p016-doodle", "回声云雀", "小云雀、回声弧线、雾蓝", "normal", "v2"),
        "pv2-p016-ssr" to PersonalTitleCatalogItem("pv2-p016-ssr", "回声云雀", "小云雀、回声弧线、雾蓝", "SSR", "v2"),
        "pv2-p017-doodle" to PersonalTitleCatalogItem("pv2-p017-doodle", "拾趣藏家", "书签、趣图卡、小狐", "normal", "v2"),
        "pv2-p017-ssr" to PersonalTitleCatalogItem("pv2-p017-ssr", "拾趣藏家", "书签、趣图卡、小狐", "SSR", "v2"),
        "pv2-p018-doodle" to PersonalTitleCatalogItem("pv2-p018-doodle", "晴日暖光", "小太阳、花瓣、奶油黄", "normal", "v2"),
        "pv2-p018-ssr" to PersonalTitleCatalogItem("pv2-p018-ssr", "晴日暖光", "小太阳、花瓣、奶油黄", "SSR", "v2"),
        "pv2-p019-doodle" to PersonalTitleCatalogItem("pv2-p019-doodle", "虹光谜匣", "彩虹边、谜盒、小兔", "normal", "v2"),
        "pv2-p019-ssr" to PersonalTitleCatalogItem("pv2-p019-ssr", "虹光谜匣", "彩虹边、谜盒、小兔", "SSR", "v2"),
        "pv2-p020-doodle" to PersonalTitleCatalogItem("pv2-p020-doodle", "像素探窗", "像素小窗、放大镜、小猫", "normal", "v2"),
        "pv2-p020-ssr" to PersonalTitleCatalogItem("pv2-p020-ssr", "像素探窗", "像素小窗、放大镜、小猫", "SSR", "v2"),
        "pv2-p021-doodle" to PersonalTitleCatalogItem("pv2-p021-doodle", "拆梗小兔", "趣图卡、谜题便笺、兔耳", "normal", "v2"),
        "pv2-p021-ssr" to PersonalTitleCatalogItem("pv2-p021-ssr", "拆梗小兔", "趣图卡、谜题便笺、兔耳", "SSR", "v2"),
        "pv2-p022-doodle" to PersonalTitleCatalogItem("pv2-p022-doodle", "拼图试客", "拆开的拼图、实验小鼠、暖灰", "normal", "v2"),
        "pv2-p022-ssr" to PersonalTitleCatalogItem("pv2-p022-ssr", "拼图试客", "拆开的拼图、实验小鼠、暖灰", "SSR", "v2"),
        "pv2-p023-doodle" to PersonalTitleCatalogItem("pv2-p023-doodle", "星尘漫游", "星尘、小幽灵、蓝紫", "normal", "v2"),
        "pv2-p023-ssr" to PersonalTitleCatalogItem("pv2-p023-ssr", "星尘漫游", "星尘、小幽灵、蓝紫", "SSR", "v2"),
        "pv2-p024-doodle" to PersonalTitleCatalogItem("pv2-p024-doodle", "止水开场", "开场卷轴、羽毛笔、青色水纹", "normal", "v2"),
        "pv2-p024-ssr" to PersonalTitleCatalogItem("pv2-p024-ssr", "止水开场", "开场卷轴、羽毛笔、青色水纹", "SSR", "v2"),
        "pv2-p025-doodle" to PersonalTitleCatalogItem("pv2-p025-doodle", "望星小猫", "望远镜、猫耳、深蓝星点", "normal", "v2"),
        "pv2-p025-ssr" to PersonalTitleCatalogItem("pv2-p025-ssr", "望星小猫", "望远镜、猫耳、深蓝星点", "SSR", "v2"),
        "pv2-p026-doodle" to PersonalTitleCatalogItem("pv2-p026-doodle", "咕嘎回声", "小鸭、笑声对话符、明黄", "normal", "v2"),
        "pv2-p026-ssr" to PersonalTitleCatalogItem("pv2-p026-ssr", "咕嘎回声", "小鸭、笑声对话符、明黄", "SSR", "v2"),
        "pv2-p027-doodle" to PersonalTitleCatalogItem("pv2-p027-doodle", "随叶拾图", "青叶、图卡、小狐", "normal", "v2"),
        "pv2-p027-ssr" to PersonalTitleCatalogItem("pv2-p027-ssr", "随叶拾图", "青叶、图卡、小狐", "SSR", "v2"),
        "pv2-p028-doodle" to PersonalTitleCatalogItem("pv2-p028-doodle", "星路小蚁", "小蚂蚁、星轨、细地图线", "normal", "v2"),
        "pv2-p028-ssr" to PersonalTitleCatalogItem("pv2-p028-ssr", "星路小蚁", "小蚂蚁、星轨、细地图线", "SSR", "v2"),
        "pv2-p029-doodle" to PersonalTitleCatalogItem("pv2-p029-doodle", "白菜探宝", "白菜叶、红包小签、木色钥匙", "normal", "v2"),
        "pv2-p029-ssr" to PersonalTitleCatalogItem("pv2-p029-ssr", "白菜探宝", "白菜叶、红包小签、木色钥匙", "SSR", "v2"),
        "pv2-p030-doodle" to PersonalTitleCatalogItem("pv2-p030-doodle", "懒鱼巧构", "小鱼、自动齿轮、图窗卡", "normal", "v2"),
        "pv2-p030-ssr" to PersonalTitleCatalogItem("pv2-p030-ssr", "懒鱼巧构", "小鱼、自动齿轮、图窗卡", "SSR", "v2"),
        "pv2-p031-doodle" to PersonalTitleCatalogItem("pv2-p031-doodle", "稻光小穗", "稻穗、米粒精灵、暖金", "normal", "v2"),
        "pv2-p031-ssr" to PersonalTitleCatalogItem("pv2-p031-ssr", "稻光小穗", "稻穗、米粒精灵、暖金", "SSR", "v2"),
        "pv2-p032-doodle" to PersonalTitleCatalogItem("pv2-p032-doodle", "七号来客", "七芒小星、纸飞机、小犬", "normal", "v2"),
        "pv2-p032-ssr" to PersonalTitleCatalogItem("pv2-p032-ssr", "七号来客", "七芒小星、纸飞机、小犬", "SSR", "v2"),
        "pv2-p033-doodle" to PersonalTitleCatalogItem("pv2-p033-doodle", "月下巧思", "月牙、谜匣钥匙、图卡、细尺", "normal", "v2"),
        "pv2-p033-ssr" to PersonalTitleCatalogItem("pv2-p033-ssr", "月下巧思", "月牙、谜匣钥匙、图卡、细尺", "SSR", "v2"),
        "pv2-p034-doodle" to PersonalTitleCatalogItem("pv2-p034-doodle", "开场惊叹", "小熊、惊叹星、小旗", "normal", "v2"),
        "pv2-p034-ssr" to PersonalTitleCatalogItem("pv2-p034-ssr", "开场惊叹", "小熊、惊叹星、小旗", "SSR", "v2"),
        "pv2-p035-doodle" to PersonalTitleCatalogItem("pv2-p035-doodle", "尽欢筑梦", "彩色图窗、筑梦小台、猫耳、星灯", "normal", "v2"),
        "pv2-p035-ssr" to PersonalTitleCatalogItem("pv2-p035-ssr", "尽欢筑梦", "彩色图窗、筑梦小台、猫耳、星灯", "SSR", "v2"),
        "pv2-p036-doodle" to PersonalTitleCatalogItem("pv2-p036-doodle", "闪现词匣", "闪光、小鲸、词句卷轴", "normal", "v2"),
        "pv2-p036-ssr" to PersonalTitleCatalogItem("pv2-p036-ssr", "闪现词匣", "闪光、小鲸、词句卷轴", "SSR", "v2"),
        "pv2-p037-doodle" to PersonalTitleCatalogItem("pv2-p037-doodle", "铁甲摸鱼", "轻铁甲、小鱼、下班时钟、对话符", "normal", "v2"),
        "pv2-p037-ssr" to PersonalTitleCatalogItem("pv2-p037-ssr", "铁甲摸鱼", "轻铁甲、小鱼、下班时钟、对话符", "SSR", "v2"),
        "pv2-p038-doodle" to PersonalTitleCatalogItem("pv2-p038-doodle", "寻星拾趣", "北极星、趣味小签、观测镜", "normal", "v2"),
        "pv2-p038-ssr" to PersonalTitleCatalogItem("pv2-p038-ssr", "寻星拾趣", "北极星、趣味小签、观测镜", "SSR", "v2"),
        "pv2-p039-doodle" to PersonalTitleCatalogItem("pv2-p039-doodle", "千叶轻语", "小鹿、千叶纹、浅青", "normal", "v2"),
        "pv2-p039-ssr" to PersonalTitleCatalogItem("pv2-p039-ssr", "千叶轻语", "小鹿、千叶纹、浅青", "SSR", "v2"),
        "pv2-p040-doodle" to PersonalTitleCatalogItem("pv2-p040-doodle", "猫猫乐章", "猫猫头、小饭签、俏皮音符", "normal", "v2"),
        "pv2-p040-ssr" to PersonalTitleCatalogItem("pv2-p040-ssr", "猫猫乐章", "猫猫头、小饭签、俏皮音符", "SSR", "v2"),
        "pv2-p041-doodle" to PersonalTitleCatalogItem("pv2-p041-doodle", "模型茶话", "茶杯、小模型方块、蒸汽对话符", "normal", "v2"),
        "pv2-p041-ssr" to PersonalTitleCatalogItem("pv2-p041-ssr", "模型茶话", "茶杯、小模型方块、蒸汽对话符", "SSR", "v2"),
        "pv2-p042-doodle" to PersonalTitleCatalogItem("pv2-p042-doodle", "支线小客", "NPC对话符、小钥匙、像素星", "normal", "v2"),
        "pv2-p042-ssr" to PersonalTitleCatalogItem("pv2-p042-ssr", "支线小客", "NPC对话符、小钥匙、像素星", "SSR", "v2"),
        "pv2-p043-doodle" to PersonalTitleCatalogItem("pv2-p043-doodle", "问号回声", "双问号、小蛋、回声丝带", "normal", "v2"),
        "pv2-p043-ssr" to PersonalTitleCatalogItem("pv2-p043-ssr", "问号回声", "双问号、小蛋、回声丝带", "SSR", "v2"),
        "pv2-p044-doodle" to PersonalTitleCatalogItem("pv2-p044-doodle", "绿芽新客", "两片新芽、花盆、暖绿", "normal", "v2"),
        "pv2-p044-ssr" to PersonalTitleCatalogItem("pv2-p044-ssr", "绿芽新客", "两片新芽、花盆、暖绿", "SSR", "v2"),
        "pv2-p045-doodle" to PersonalTitleCatalogItem("pv2-p045-doodle", "可可来信", "巧克力色、封口纸笺、软兔", "normal", "v2"),
        "pv2-p045-ssr" to PersonalTitleCatalogItem("pv2-p045-ssr", "可可来信", "巧克力色、封口纸笺、软兔", "SSR", "v2"),
        "pv2-p046-doodle" to PersonalTitleCatalogItem("pv2-p046-doodle", "早茶羹羹", "暖色小碗、早晨太阳、红包签、小猫", "normal", "v2"),
        "pv2-p046-ssr" to PersonalTitleCatalogItem("pv2-p046-ssr", "早茶羹羹", "暖色小碗、早晨太阳、红包签、小猫", "SSR", "v2"),
        "pv2-p047-doodle" to PersonalTitleCatalogItem("pv2-p047-doodle", "糖盐初问", "小番茄、双小勺、淡黄", "normal", "v2"),
        "pv2-p047-ssr" to PersonalTitleCatalogItem("pv2-p047-ssr", "糖盐初问", "小番茄、双小勺、淡黄", "SSR", "v2"),
        "pv2-p048-doodle" to PersonalTitleCatalogItem("pv2-p048-doodle", "松果微光", "松果、小熊、奶茶棕", "normal", "v2"),
        "pv2-p048-ssr" to PersonalTitleCatalogItem("pv2-p048-ssr", "松果微光", "松果、小熊、奶茶棕", "SSR", "v2"),
        "pv2-p049-doodle" to PersonalTitleCatalogItem("pv2-p049-doodle", "风中看彩", "风卷、小彩签、狐狸", "normal", "v2"),
        "pv2-p049-ssr" to PersonalTitleCatalogItem("pv2-p049-ssr", "风中看彩", "风卷、小彩签、狐狸", "SSR", "v2"),
        "pv2-p050-doodle" to PersonalTitleCatalogItem("pv2-p050-doodle", "瓜瓜星仓", "西瓜、星星罐、谜题小镜", "normal", "v2"),
        "pv2-p050-ssr" to PersonalTitleCatalogItem("pv2-p050-ssr", "瓜瓜星仓", "西瓜、星星罐、谜题小镜", "SSR", "v2"),
        "pv2-p051-doodle" to PersonalTitleCatalogItem("pv2-p051-doodle", "荷包开席", "荷包蛋、红包小席、游戏按键", "normal", "v2"),
        "pv2-p051-ssr" to PersonalTitleCatalogItem("pv2-p051-ssr", "荷包开席", "荷包蛋、红包小席、游戏按键", "SSR", "v2"),
        "pv2-p052-doodle" to PersonalTitleCatalogItem("pv2-p052-doodle", "欢欢大王", "趣图册、麦穗小冠、猫耳、枣红", "normal", "v2"),
        "pv2-p052-ssr" to PersonalTitleCatalogItem("pv2-p052-ssr", "欢欢大王", "趣图册、麦穗小冠、猫耳、枣红", "SSR", "v2"),
        "pv2-p053-doodle" to PersonalTitleCatalogItem("pv2-p053-doodle", "表情小村", "小犬、表情旗、暖蓝", "normal", "v2"),
        "pv2-p053-ssr" to PersonalTitleCatalogItem("pv2-p053-ssr", "表情小村", "小犬、表情旗、暖蓝", "SSR", "v2"),
        "pv2-p054-doodle" to PersonalTitleCatalogItem("pv2-p054-doodle", "白羽来趣", "白鸟、趣图信笺、暖金红包结", "normal", "v2"),
        "pv2-p054-ssr" to PersonalTitleCatalogItem("pv2-p054-ssr", "白羽来趣", "白鸟、趣图信笺、暖金红包结", "SSR", "v2"),
        "pv2-p055-doodle" to PersonalTitleCatalogItem("pv2-p055-doodle", "闲间青狐", "青狐、咖啡小杯、短时钟", "normal", "v2"),
        "pv2-p055-ssr" to PersonalTitleCatalogItem("pv2-p055-ssr", "闲间青狐", "青狐、咖啡小杯、短时钟", "SSR", "v2"),
        "pv2-p056-doodle" to PersonalTitleCatalogItem("pv2-p056-doodle", "冰糖星点", "冰蓝星、糖晶、白色光点", "normal", "v2"),
        "pv2-p056-ssr" to PersonalTitleCatalogItem("pv2-p056-ssr", "冰糖星点", "冰蓝星、糖晶、白色光点", "SSR", "v2"),
        "pv2-p057-doodle" to PersonalTitleCatalogItem("pv2-p057-doodle", "风铃浅歌", "风铃、小羽鸟、天青", "normal", "v2"),
        "pv2-p057-ssr" to PersonalTitleCatalogItem("pv2-p057-ssr", "风铃浅歌", "风铃、小羽鸟、天青", "SSR", "v2"),
        "pv2-p058-doodle" to PersonalTitleCatalogItem("pv2-p058-doodle", "月光纸鸢", "纸鸢、月兔、灰紫", "normal", "v2"),
        "pv2-p058-ssr" to PersonalTitleCatalogItem("pv2-p058-ssr", "月光纸鸢", "纸鸢、月兔、灰紫", "SSR", "v2"),
        "pv2-p059-doodle" to PersonalTitleCatalogItem("pv2-p059-doodle", "落日小翼", "夕阳、纸飞机、小鸟", "normal", "v2"),
        "pv2-p059-ssr" to PersonalTitleCatalogItem("pv2-p059-ssr", "落日小翼", "夕阳、纸飞机、小鸟", "SSR", "v2"),
    )

    val COMMUNITY_SKINS: Map<String, CommunitySkinCatalogItem> = mapOf(
        "aurora-preset" to CommunitySkinCatalogItem("aurora-preset", "极光流转", "qwddkxs", hasTitle = true, hasBadge = true, hasBubble = true, hasReply = true),
        "gushen-yinpa" to CommunitySkinCatalogItem(
            id = "gushen-yinpa", name = "古神银趴", author = "Earthcat",
            hasTitle = true, hasBadge = true, hasBubble = true, hasReply = true,
            bubbleCssSha = "44842f9a84ccfa72eef2eceb8d14c5671d5244961afedd66104e56a224855f56",
            titleCssSha = "6a7a2bb0ff0a2956b641fd98de0a65759c7e61de57d54d692a86f7857fcbf06a",
            badgeCssSha = "599048ca7b1d8131a35c1cee19e3efacd37c93c29ed3e53e729bc8fa9bd7e91b",
            replyCssSha = "89e0b9ca54c2ec94ef0ed8250a417624657e703b8cc73bd731eb7efaee8bc638",
        ),
        "lanting-ink" to CommunitySkinCatalogItem(
            id = "lanting-ink", name = "兰亭墨韵", author = "超时空蚂蚁",
            hasTitle = true, hasBadge = true, hasBubble = true, hasReply = true,
            bubbleCssSha = "d2061651cc0cbcd1e369dce2101c665bb3fb3bf1c47c47ddce6706c556c9a0f8",
            titleCssSha = "2e46a5e8bdc8c377a13fb6da7730a9569964865858ff411b7148befcf52b9de3",
            badgeCssSha = "91f13a156701580083213711d78d3c78897e9d44186e38b21795754405f384a1",
            replyCssSha = "977ee520dea47fd0a67265969bab90672f210a840feb3b15641b96b9bd81fca2",
        ),
        "laoda-purple" to CommunitySkinCatalogItem(
            id = "laoda-purple", name = "牢大牢大", author = "Earthcat",
            hasTitle = true, hasBadge = false, hasBubble = true,
            bubbleCssSha = "577da4e91d9738e86786c8406c07dfe88cbdcb44e63311042b7e77026da3a0d8",
            titleCssSha = "8af57fbe47154b322d365f9bfe185ac8bb4a6b89007c5dd2ad534b52f2c01cc8",
        ),
        "laowu-dispatch" to CommunitySkinCatalogItem(
            id = "laowu-dispatch", name = "老吴派遣·橘", author = "Earthcat",
            hasTitle = true, hasBadge = true, hasBubble = true,
            bubbleCssSha = "0214a7c86f6aceca3aed5e860ac49af4d1c87115f03488adf08bf775e2913d3a",
            titleCssSha = "7eb5f36f5a29e6a2aa3e2c88889202c3c9deb786ab3067caa67fd99a2fd03f8b",
            badgeCssSha = "8308f50c2f974c616763cf22dbee4a14de2f9927bdd7d402b74ae26faeaa662b",
        ),
        "laowu-dispatch-all-in" to CommunitySkinCatalogItem(
            id = "laowu-dispatch-all-in", name = "老吴派遣·all in", author = "Earthcat",
            hasTitle = true, hasBadge = true, hasBubble = true,
            bubbleCssSha = "2fddaed5011c32f624512f7cee4b7b53a15353dc275807c5847259a9b7cf6f28",
            titleCssSha = "12667bafacb0d837d7a43b50cfbcc9b296b326221b05b1da6078100e553d2067",
            badgeImgSha = "19b1612f344bacb77ff295e1015b806caf76b8c71d573f4d6ab572e20b3ede2d",
            badgeCssSha = "94246d3faec40ef12c0149002c8399d03334719b91f739e3c7b7b24288f1e83c",
        ),
        "laowu-dispatch-black" to CommunitySkinCatalogItem(
            id = "laowu-dispatch-black", name = "奶酪鼠王", author = "Earthcat",
            hasTitle = true, hasBadge = true, hasBubble = true, hasReply = true,
            bubbleCssSha = "0976ac4b25a15e410e37c3c85022bf50d068528a81a30e8d924ce7b2614e870d",
            titleCssSha = "c26412a5ab7388f20014ebeec4b73ded19703380b428645f97af8937af043117",
            badgeCssSha = "02495e1c24e7fa1733bf4a8099f05eab7d20016f9f6e8905c8b34140d21f8ac7",
            replyCssSha = "85bb88df8d931f5a8e44724978da15b8ac1e4fc6f0b7f2381a93e05a1282551b",
        ),
        "laowu-dispatch-calico" to CommunitySkinCatalogItem(
            id = "laowu-dispatch-calico", name = "老吴派遣·三花", author = "Earthcat",
            hasTitle = true, hasBadge = false, hasBubble = true,
            bubbleCssSha = "18c36ff1d9e2b4b6128e94c4015e663f0c48308c302bf704902735faab1aa0b6",
            titleCssSha = "936a072bc83ff4af6a10f51574152e872f00aa72d81a7429f1fd5aa42d1a925a",
        ),
        "laowu-dispatch-gray" to CommunitySkinCatalogItem(
            id = "laowu-dispatch-gray", name = "虫棍飞天虾", author = "Earthcat",
            hasTitle = true, hasBadge = true, hasBubble = true,
            bubbleCssSha = "85893b9737a342ae448bb9f26c9909204359b53d1817bf37d828da26c90a63c0",
            titleCssSha = "9d4c2d089f5f0b7fd4a4692bbcf29222e1abff129b14f8ce41b3b5a9cec6f38b",
            badgeCssSha = "f217d3c8463c884e369b24ce89688e5c87505fe2fb28a065cf912c2862edebd1",
        ),
        "laowu-dispatch-siamese" to CommunitySkinCatalogItem(
            id = "laowu-dispatch-siamese", name = "sup早苗", author = "Earthcat",
            hasTitle = true, hasBadge = true, hasBubble = true,
            bubbleCssSha = "a397e6bb8c6b5d1cdc1b39157136edff9bcb01556f30beed37e65d695b2cf647",
            titleCssSha = "f33eb7c4d83be2ca65d8d8b197073cb4eced04849cf7b0b8b9fbe52da9ab0723",
            badgeCssSha = "539b83f39c5c714e8a2b8307588dbf3d023362e7dddd3cc8e086894c6f96774c",
        ),
        "laowu-dispatch-white" to CommunitySkinCatalogItem(
            id = "laowu-dispatch-white", name = "整点薯条", author = "Earthcat",
            hasTitle = true, hasBadge = true, hasBubble = true, hasReply = true,
            bubbleCssSha = "25fc42b6c36583cc1667d11aea6be7026b00f63c511aab13e26adb6f6a390156",
            titleCssSha = "a2b9d6c54240e9e60c24cf21db215ff96428b7a45fde185ea7aea136ec1cc701",
            badgeCssSha = "b4fa89efd4d7ee5b295ed69149b418c4223d90a14bc2a554f98915bf52dfe5f8",
            replyCssSha = "71cc0bcec9f5ea878e4f4f4477a657b4bf23ca3cbba04c86bcfc2d7a1163f37b",
        ),
        "manga-tremor" to CommunitySkinCatalogItem(
            id = "manga-tremor", name = "娇怯拟声", author = "DracaeneSunbringer",
            hasTitle = true, hasBadge = true, hasBubble = true, hasReply = true,
            bubbleCssSha = "70628030042823f81331a1102144b183140c2d8dc60b4d12938d3d765a6d3d5a",
            titleCssSha = "dd182da1acbdf9bf8a2896cfee7bf8bd6220bb429b0e4e6e81c5f8d195331448",
            badgeImgSha = "ee7735aa671347752bf0d5b11f1f3b1ab263c64237f80c8de27d1e56f78cec97",
            badgeCssSha = "878e318ff9927971bf038459b22d947e1c33950e450b002e0c78fcef36036ac4",
            replyCssSha = "87d95d45610403d2d1255d708a0422c55dda13641edb8f46717255b9488761e2",
        ),
        "oiia-loaf" to CommunitySkinCatalogItem(
            id = "oiia-loaf", name = "oiia", author = "Earthcat",
            hasTitle = true, hasBadge = false, hasBubble = true,
            bubbleCssSha = "784ee18be88851697cd36bee3e1687c60f7ecc360c742aa74bffd5362ebf6892",
            titleCssSha = "1a0c4355046715dbc8adb807df228befb2da9c2b096e6011eb509659972f591e",
        ),
        "orange-flick" to CommunitySkinCatalogItem(
            id = "orange-flick", name = "谷恒条野", author = "Earthcat",
            hasTitle = true, hasBadge = true, hasBubble = true, hasReply = true,
            bubbleCssSha = "382c275581545f1a02f6a607f4383a77f908f28f5da89ea0c62aa751deec9ae6",
            titleCssSha = "9f191b7cb02af0ff36f2c76cf92a6b94bbf1f7a14db4adf5e62627412830a2a2",
            badgeCssSha = "8308f50c2f974c616763cf22dbee4a14de2f9927bdd7d402b74ae26faeaa662b",
            replyCssSha = "0a28c9e1259604c58ee9dd7b6c88ddafda6bbbed50014f7db908bc05d762f8f6",
        ),
        "p3r-zero-hour-fool" to CommunitySkinCatalogItem(
            id = "p3r-zero-hour-fool", name = "月行水上", author = "结城理",
            hasTitle = true, hasBadge = false, hasBubble = true,
            bubbleImgSha = "0a4fbf591d0059052bccc9baef213b050426ea90d02c154afe011e407e227245",
            bubbleCssSha = "003a8f449e3ff91f57c2c9fbcfffc6b735c7577a2b9b5cf45b7ee905123d674d",
            titleCssSha = "2e93383775162eabae36eb97ba3791c26073379d67b215ffe56b0fcae2c00ec8",
        ),
        "rem-dual-form" to CommunitySkinCatalogItem(
            id = "rem-dual-form", name = "蕾姆·昼夜誓约", author = "shx126",
            hasTitle = true, hasBadge = false, hasBubble = true,
            bubbleCssSha = "1ca1e3cfa3a29ec18471745e2fd38e3391dbd7cd60c7292f8abba4e80fd5b695",
            titleCssSha = "352b5fff6255041aefa65a5befb6325fcd3aa4a0abfe649d1196ff66a604c874",
        ),
        "round-maodie" to CommunitySkinCatalogItem(
            id = "round-maodie", name = "圆头耄耋", author = "Earthcat",
            hasTitle = true, hasBadge = false, hasBubble = true,
            bubbleCssSha = "9f8ae826293199307224daafce9e5d8d25c92a61777366eb6812ba460950dbd4",
            titleCssSha = "52994e13542a3a4767edf93934919096e95e2ca441d86cf9aeac8d8a8aaf2080",
        ),
        "shadow-dao" to CommunitySkinCatalogItem(
            id = "shadow-dao", name = "影道", author = "超时空蚂蚁",
            hasTitle = true, hasBadge = true, hasBubble = true, hasReply = true,
            bubbleImgSha = "c27d25f5af2a532894b64d7b3b067d761039e5f8a7d8b1f9bde1cc08af97e564",
            bubbleCssSha = "4109795dce89347e35af08858127b876d4ddfc2a73560d1175a4c222b9e6f6f1",
            bubbleSlice = intArrayOf(42, 60, 48, 64),
            titleImgSha = "43d02ac97cee40ff1a6d5ab4b65deb45e9821122d84434dde2d3b1e91c18de3e",
            titleCssSha = "5922d5865c0ad8231ad6300feecfda1d27c41bf99dc9bbd808e21d0cdfeba253",
            titleSlice = intArrayOf(16, 44, 18, 52),
            badgeImgSha = "035488c68252c293478618fae559040afa733ee1373333bd548a3da1a6143a88",
            badgeCssSha = "301c922e3cd6fdc25d792f7a52ed4bdec4bf3abc5ef966a336cdb632cfb7da97",
            replyImgSha = "3c8a4a9d85b7386530c543b9e16b73435257fcb42fc9168a373115fa53afbceb",
            replyCssSha = "18f7ad0c5bd6189267927b5b8ccfe63951f5311ce81d4077af57ea69f5757a9c",
            replySlice = intArrayOf(18, 50, 20, 56),
        ),
        "shadow-path" to CommunitySkinCatalogItem(
            id = "shadow-path", name = "影径", author = "超时空蚂蚁",
            hasTitle = true, hasBadge = true, hasBubble = true, hasReply = true,
            bubbleImgSha = "5d34f540288f7052042513c9b5b87f3fc7ff898f7388705c37c4c832eae515bf",
            bubbleCssSha = "e7dc6128c837836425a886cdb7cf10b42294dcea67f2487a4e321d6798b08b3d",
            bubbleSlice = intArrayOf(42, 64, 44, 62),
            titleImgSha = "f1253e3c6311ffd87353525341d358d0372af8c865700ccc8e78469850403bcb",
            titleCssSha = "9e6c7d162981c10ac3bbaa3a318fde9a2a85448247ab38a7077c4a6fbe1332fb",
            titleSlice = intArrayOf(16, 48, 16, 52),
            badgeImgSha = "0492ab924b26e80bb4db26ed17f9e2a4b4e54e6abb00cde8a5b5bcee3d505b60",
            badgeCssSha = "431b75b03262eba486b8489b958d3ff715e0aa715e8ca779c80bd401a4cbf62c",
            replyImgSha = "692bfbc3d734200791fd4e66a12c1c0e349e6a4446826d81c06fdbca9a36072b",
            replyCssSha = "3fc72637e828fa906ebdf38c59cdfc170d2ea63a3cc022342b98f4c6c6f5b24e",
            replySlice = intArrayOf(18, 56, 18, 56),
        ),
        "step-by-step-bird" to CommunitySkinCatalogItem(
            id = "step-by-step-bird", name = "一步一步走鸟", author = "Earthcat",
            hasTitle = true, hasBadge = true, hasBubble = true, hasReply = true,
            bubbleCssSha = "b2878c60154580c36c54645099a3281b1d7c6ea4a721e1d4da64a3e1e4c77645",
            titleCssSha = "e54236482a889d0e2397bdba4de33ea7e6516ace48b2346817e568cf2ebb3c8e",
            badgeCssSha = "64ad2adec4fb473dc109ba170059f4341487290354f45511ae2f2aecc30d8b22",
            replyCssSha = "f8cb9516e35ebb597d9014464abb9a20f664cab3c865c68bb955a09225ec7170",
        ),
        "succubus-lace" to CommunitySkinCatalogItem(
            id = "succubus-lace", name = "绯夜魅魔", author = "DracaeneSunbringer",
            hasTitle = true, hasBadge = true, hasBubble = true, hasReply = true,
            bubbleCssSha = "94aee08d840f68a591057ac5795db5544b4dcf329e4ff1fc3295dc22accece25",
            titleCssSha = "c4a4c731ece36e07008f62362314edae01b5a0b49f243b4b2ba5b0beb8793643",
            badgeImgSha = "ec7af8d611fa8d7ceabd9b693b650fc0a815979d426e836bdf45e4be76b1732f",
            badgeCssSha = "2629d87c959d41181f8c85e92fccb677ca2746185a35b5a8ee425eaf1f4b8bb2",
            replyCssSha = "bd75d8f2526b2c200e753f05a517ef0d0b6cea446cb8e2edb25a32561f7a24fd",
        ),
        "taike-lili" to CommunitySkinCatalogItem(
            id = "taike-lili", name = "肽克lili", author = "Earthcat",
            hasTitle = true, hasBadge = true, hasBubble = true, hasReply = true,
            bubbleCssSha = "33fd98aa4f867a9878aafc9878b31ad3440c55c8c30de71a761da0cd9e6b7770",
            titleCssSha = "f66ff4b08b49320c3027cd98b4be0beb24db90fd3b6623c69918498a90621774",
            badgeCssSha = "b9ec4f1f3aba6b619d1412c2c58b44137e9442eb3ada73510ff6e7d973fcbefb",
            replyCssSha = "be822d7b53b28a5606ed1696760b45dfeeda8f85e35f5c6ad05f86795df75018",
        ),
        "waibi-babu" to CommunitySkinCatalogItem(
            id = "waibi-babu", name = "外币巴布", author = "Earthcat",
            hasTitle = true, hasBadge = false, hasBubble = true,
            bubbleCssSha = "071f6e9dba7f15c2b6132491b34c6f2c9c889bfc004999444cef0face86572a4",
            titleCssSha = "08af8fd3adc2847b3e82e68b55d22d63e9e48b2b93667c665ed6a1ed94b35b45",
        ),
    )

    val FANCY_NICK_PRESETS: List<Pair<String, List<String>>> = listOf(
        "默认彩虹" to emptyList(),
        "樱粉" to listOf("#ff5fa2", "#ffb3d1", "#c86bff"),
        "海盐" to listOf("#00b4d8", "#48cae4", "#90e0ef"),
        "日落" to listOf("#ff6b35", "#f7c548", "#ff3864"),
        "森林" to listOf("#2d6a4f", "#52b788", "#b7e4c7"),
        "赛博" to listOf("#f72585", "#7209b7", "#4cc9f0"),
        "黑金" to listOf("#111111", "#d4a017", "#fff3b0"),
    )

    val BUBBLE_PRESETS: List<Pair<String, String>> = listOf(
        "" to "默认",
        "aurora" to "极光流转",
        "jelly" to "果冻",
        "puff" to "棉花云",
        "paw" to "猫爪",
        "galaxy" to "星河",
        "bit" to "像素机",
        "petal" to "樱吹雪",
        "boba" to "奶茶",
        "cyber" to "赛博",
        "prism" to "幻彩描边",
        "mint" to "薄荷",
        "bunny" to "兔兔",
        "lemon" to "柠檬汽水",
        "laowu" to "老吴",
        "cloud" to "云朵",
        "candy" to "糖果",
        "neon" to "霓虹",
        "paper" to "便签",
        "ink" to "水墨",
        "pixel" to "像素",
        "sakura" to "樱花",
        "gold" to "土豪金",
        "night" to "星空",
    )

    val AURORA_COMPASS_SVG: String = """
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64">
          <defs>
            <linearGradient id="rg" x1="0" y1="0" x2="1" y2="1">
              <stop offset="0%" stop-color="#00f0ff"/>
              <stop offset="35%" stop-color="#8b5cf6"/>
              <stop offset="70%" stop-color="#ff007f"/>
              <stop offset="100%" stop-color="#ffea00"/>
            </linearGradient>
          </defs>
          <polygon points="32,2 41,11 54,10 53,23 62,32 53,41 54,54 41,53 32,62 23,53 10,54 11,41 2,32 11,23 10,10 23,11" fill="#131a34" stroke="url(#rg)" stroke-width="3.5"/>
          <circle cx="32" cy="32" r="20" fill="none" stroke="url(#rg)" stroke-width="1.5"/>
          <path fill="#38bdf8" d="M32 10 L36 28 L54 10 L36 36 L54 54 L28 36 L10 54 L28 28 L10 10 L28 28 Z"/>
          <path fill="#ffffff" d="M32 4 L35.5 27.5 L60 32 L35.5 36.5 L32 60 L28.5 36.5 L4 32 L28.5 27.5 Z"/>
          <circle cx="32" cy="32" r="5" fill="#ff007f"/>
          <circle cx="32" cy="32" r="2.5" fill="#ffffff"/>
        </svg>
    """.trimIndent()

    private val _catalogRevision = MutableStateFlow(0)
    val catalogRevision: StateFlow<Int> = _catalogRevision.asStateFlow()

    const val KV_DECO_CLEAN_KEY = "deco.clean.v2"
    private val _localCleanRawFlow = MutableStateFlow<String?>(null)
    val localCleanRawFlow: StateFlow<String?> = _localCleanRawFlow.asStateFlow()

    fun seedLocalCleanRaw(raw: String?) {
        _localCleanRawFlow.value = raw
    }

    fun encodeLocalCleanConfig(config: DecoConfig): String {
        val cleanOnly = DecoConfig(
            cleanHideOthersNick = config.cleanHideOthersNick,
            cleanHideOthersBadges = config.cleanHideOthersBadges,
            cleanHideOthersStyles = config.cleanHideOthersStyles,
            cleanHideBubbles = config.cleanHideBubbles,
            cleanHideReplyFrames = config.cleanHideReplyFrames,
            cleanPauseOrnaments = config.cleanPauseOrnaments,
            blockedCosmeticKeys = config.blockedCosmeticKeys,
            blockedKeyHideFullSet = config.blockedKeyHideFullSet,
        )
        return ForHappyJson.encodeToString(cleanOnly)
    }

    suspend fun saveLocalCleanConfig(api: ForHappyApiClient, config: DecoConfig) {
        val encoded = encodeLocalCleanConfig(config)
        _localCleanRawFlow.value = encoded
        api.putPref(KV_DECO_CLEAN_KEY, encoded)
    }

    private val dynamicBadges = ConcurrentHashMap<String, BadgeCatalogEntry>()
    private val dynamicGrowthById = ConcurrentHashMap<String, GrowthRewardMeta>()
    private val dynamicGrowthBySeries = ConcurrentHashMap<String, List<GrowthRewardMeta>>()
    private val dynamicOfficialTitles = ConcurrentHashMap<String, OfficialTitleMeta>()
    private val dynamicCommunitySkins = ConcurrentHashMap<String, CommunitySkinCatalogItem>()
    private val dynamicRaffleTitles = ConcurrentHashMap<String, RaffleTitleCatalogItem>()
    private val dynamicPersonalTitles = ConcurrentHashMap<String, PersonalTitleCatalogItem>()
    private val dynamicGrowthV5Ids = CopyOnWriteArraySet<String>()

    fun notifyDynamicCosmeticParsed() {
        _catalogRevision.value += 1
    }

    fun resolveCommunitySkinMeta(id: String): CommunitySkinCatalogItem? =
        dynamicCommunitySkins[id] ?: COMMUNITY_SKINS[id]

    fun allCommunitySkins(): Map<String, CommunitySkinCatalogItem> {
        val merged = LinkedHashMap<String, CommunitySkinCatalogItem>(COMMUNITY_SKINS.size + dynamicCommunitySkins.size)
        COMMUNITY_SKINS.forEach { (id, item) -> merged[id] = dynamicCommunitySkins[id] ?: item }
        dynamicCommunitySkins.forEach { (id, item) -> if (!merged.containsKey(id)) merged[id] = item }
        return merged
    }

    fun resolveRaffleTitle(id: String?): RaffleTitleCatalogItem? {
        val clean = id?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return dynamicRaffleTitles[clean] ?: RAFFLE_TITLES[clean]
    }

    fun allRaffleTitles(): Map<String, RaffleTitleCatalogItem> {
        val merged = LinkedHashMap<String, RaffleTitleCatalogItem>(RAFFLE_TITLES.size + dynamicRaffleTitles.size)
        RAFFLE_TITLES.forEach { (id, item) -> merged[id] = dynamicRaffleTitles[id] ?: item }
        dynamicRaffleTitles.forEach { (id, item) -> if (!merged.containsKey(id)) merged[id] = item }
        return merged
    }

    fun resolvePersonalTitle(id: String?): PersonalTitleCatalogItem? {
        val clean = id?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return dynamicPersonalTitles[clean] ?: PERSONAL_TITLES[clean]
    }

    fun allPersonalTitles(): Map<String, PersonalTitleCatalogItem> {
        val merged = LinkedHashMap<String, PersonalTitleCatalogItem>(PERSONAL_TITLES.size + dynamicPersonalTitles.size)
        PERSONAL_TITLES.forEach { (id, item) -> merged[id] = dynamicPersonalTitles[id] ?: item }
        dynamicPersonalTitles.forEach { (id, item) -> if (!merged.containsKey(id)) merged[id] = item }
        return merged
    }

    fun parseUnlockKeys(unlocksJson: JsonElement?): Set<String> {
        if (unlocksJson == null) return emptySet()
        val out = LinkedHashSet<String>()
        when (unlocksJson) {
            is JsonArray -> unlocksJson.forEach { el ->
                val v = (el as? JsonPrimitive)?.contentOrNull?.trim().orEmpty()
                if (v.isNotEmpty()) out.add(v)
            }
            is JsonObject -> unlocksJson.forEach { (k, v) ->
                val truthy = (v as? JsonPrimitive)?.booleanOrNull == true ||
                    ((v as? JsonPrimitive)?.intOrNull ?: 0) > 0
                val clean = k.trim()
                if (truthy && clean.isNotEmpty()) out.add(clean)
            }
            else -> Unit
        }
        return out
    }

    fun allUnlockedGrowthRewards(
        meResponse: MeResponse? = null,
        rawGrowthRewards: JsonElement? = null,
        unlocksJson: JsonElement? = null,
        allCosmetics: Boolean = false,
    ): List<GrowthRewardMeta> {
        val effectiveAll = allCosmetics || meResponse?.user?.cosmeticAccess == "all"
        val unlocksSet = parseUnlockKeys(unlocksJson ?: meResponse?.user?.unlocks)
        fun isUnlocked(meta: GrowthRewardMeta): Boolean {
            if (effectiveAll) return true
            val series = meta.series?.takeIf { it.isNotBlank() } ?: "guess"
            return "$series:${meta.level}" in unlocksSet || meta.id in unlocksSet
        }

        val byId = LinkedHashMap<String, GrowthRewardMeta>()
        fun addMeta(meta: GrowthRewardMeta?) {
            if (meta != null && meta.id.isNotBlank()) byId[meta.id] = meta
        }
        meResponse?.let { r ->
            (r.guessRewards + r.luckRewards + r.rankRewards + r.activityRewards).forEach { meta ->
                if (isUnlocked(meta)) addMeta(meta)
            }
        }
        if (rawGrowthRewards != null) {
            when (rawGrowthRewards) {
                is JsonArray -> rawGrowthRewards.forEach { el ->
                    when (el) {
                        is JsonPrimitive -> resolveGrowthRewardMeta(el.contentOrNull.orEmpty())?.let(::addMeta)
                        is JsonObject -> el["id"]?.jsonPrimitive?.contentOrNull?.let { id ->
                            resolveGrowthRewardMeta(id)?.let(::addMeta)
                        }
                        else -> Unit
                    }
                }
                is JsonObject -> rawGrowthRewards.forEach { (k, v) ->
                    val truthy = (v as? JsonPrimitive)?.booleanOrNull == true ||
                        ((v as? JsonPrimitive)?.intOrNull ?: 0) > 0
                    if (truthy) resolveGrowthRewardMeta(k)?.let(::addMeta)
                }
                else -> Unit
            }
        }
        if (byId.isEmpty() && meResponse == null && rawGrowthRewards == null) {
            dynamicGrowthById.values.forEach { meta ->
                if (isUnlocked(meta)) addMeta(meta)
            }
            if (dynamicGrowthById.isEmpty() && unlocksSet.isNotEmpty()) {
                unlocksSet.forEach { key ->
                    resolveGrowthRewardMeta(key)?.let(::addMeta)
                }
            }
        }
        return byId.values.toList()
    }

    private fun unescapeJsUnicode(raw: String): String {
        if (!raw.contains("\\u")) return raw
        return Regex("""\\u([0-9a-fA-F]{4})""").replace(raw) { m ->
            val cp = m.groupValues[1].toIntOrNull(16) ?: return@replace m.value
            cp.toChar().toString()
        }
    }

    /**
     * Parses main-site ES/CSS catalog modules fetched dynamically from `52dmeme.com`, applies them
     * live to [BadgeAndCosmeticRegistry] & [ChatBubbleCosmetics], and returns a serialized snapshot JSON.
     */
    fun parseAndApplyWebModules(
        titleCollectionJs: String? = null,
        personalCatalogJs: String? = null,
        personalV2CatalogJs: String? = null,
        growthV5Js: String? = null,
        personalV2ArtCss: String? = null,
        personalPalettesCss: String? = null,
        titleSymbolsJs: String? = null,
    ): String {
        val parsedRaffle = ArrayList<RaffleTitleCatalogItem>()
        if (!titleCollectionJs.isNullOrBlank()) {
            val rowRe = Regex(
                """\[\s*"([^"]+)"\s*,\s*"([^"]*)"\s*,\s*"([^"]*)"\s*,\s*"([^"]*)"\s*,\s*"(#[0-9a-fA-F]{3,8})"\s*,\s*"(#[0-9a-fA-F]{3,8})"\s*,\s*"([^"]*)"\s*]""",
            )
            for (m in rowRe.findAll(titleCollectionJs)) {
                val id = unescapeJsUnicode(m.groupValues[1]).trim()
                val name = unescapeJsUnicode(m.groupValues[2]).trim()
                val mood = unescapeJsUnicode(m.groupValues[3]).trim()
                val sym = unescapeJsUnicode(m.groupValues[4]).trim()
                val color = m.groupValues[5].trim()
                val accent = m.groupValues[6].trim()
                val rarity = unescapeJsUnicode(m.groupValues[7]).trim().ifBlank { "normal" }
                if (id.isNotEmpty() && name.isNotEmpty()) {
                    val item = RaffleTitleCatalogItem(id, name, mood, color, accent, rarity, sym)
                    parsedRaffle.add(item)
                    dynamicRaffleTitles[id] = item
                }
            }
        }

        val parsedPersonal = ArrayList<PersonalTitleCatalogItem>()
        fun extractJsonObjectAfterEquals(js: String, varName: String): JsonObject? {
            val markerIdx = js.indexOf(varName)
            if (markerIdx < 0) return null
            val braceStart = js.indexOf('{', markerIdx)
            val braceEnd = js.lastIndexOf('}')
            if (braceStart < 0 || braceEnd <= braceStart) return null
            val rawObj = js.substring(braceStart, braceEnd + 1)
            return runCatching { ForHappyJson.parseToJsonElement(rawObj).jsonObject }.getOrNull()
        }
        if (!personalCatalogJs.isNullOrBlank()) {
            extractJsonObjectAfterEquals(personalCatalogJs, "PERSONAL_TITLES")?.forEach { (id, el) ->
                val obj = runCatching { el.jsonObject }.getOrNull() ?: return@forEach
                val name = obj["name"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
                val mood = obj["mood"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
                if (id.isNotBlank() && name.isNotBlank()) {
                    val item = PersonalTitleCatalogItem(id, name, mood, "normal", "v1")
                    parsedPersonal.add(item)
                    dynamicPersonalTitles[id] = item
                }
            }
        }
        if (!personalV2CatalogJs.isNullOrBlank()) {
            extractJsonObjectAfterEquals(personalV2CatalogJs, "PERSONAL_V2_TITLES")?.forEach { (id, el) ->
                val obj = runCatching { el.jsonObject }.getOrNull() ?: return@forEach
                val name = obj["name"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
                val mood = obj["mood"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
                val rarity = obj["rarity"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty().ifBlank { "normal" }
                val art = obj["art"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty().ifBlank { "v2" }
                if (id.isNotBlank() && name.isNotBlank()) {
                    val item = PersonalTitleCatalogItem(id, name, mood, rarity, art)
                    parsedPersonal.add(item)
                    dynamicPersonalTitles[id] = item
                }
            }
        }

        val parsedGrowthV5 = ArrayList<String>()
        if (!growthV5Js.isNullOrBlank()) {
            val arrStart = growthV5Js.indexOf('[')
            val arrEnd = growthV5Js.lastIndexOf(']')
            if (arrStart >= 0 && arrEnd > arrStart) {
                runCatching {
                    ForHappyJson.parseToJsonElement(growthV5Js.substring(arrStart, arrEnd + 1)).jsonArray
                }.getOrNull()?.forEach { el ->
                    val id = el.jsonPrimitive.contentOrNull?.trim().orEmpty()
                    if (id.isNotEmpty()) {
                        parsedGrowthV5.add(id)
                        dynamicGrowthV5Ids.add(id)
                    }
                }
            }
        }

        val v2ArtMap = personalV2ArtCss?.let { ChatBubbleCosmetics.parsePersonalV2ArtCss(it) }.orEmpty()
        val v1PalMap = personalPalettesCss?.let { ChatBubbleCosmetics.parsePersonalPalettesCss(it) }.orEmpty()
        val symbolMap = titleSymbolsJs?.let { ChatBubbleCosmetics.parseTitleSymbolsJs(it) }.orEmpty()

        val snapshot = WebCosmeticCatalogSnapshot(
            raffleTitles = parsedRaffle,
            personalTitles = parsedPersonal,
            growthV5Ids = parsedGrowthV5,
            v2ArtSpecs = v2ArtMap.mapValues { (_, v) -> listOf(v.titleX, v.titleY, v.titlePadDp) },
            v1Palettes = v1PalMap.mapValues { (_, v) -> listOf(v.inkHex, v.paperHex, v.accentHex) },
            titleSymbols = symbolMap,
        )
        _catalogRevision.value += 1
        return ForHappyJson.encodeToString(snapshot)
    }

    fun applyWebCatalogSnapshotJson(rawJson: String): Boolean {
        val snap = runCatching { ForHappyJson.decodeFromString<WebCosmeticCatalogSnapshot>(rawJson) }.getOrNull()
            ?: return false
        snap.raffleTitles.forEach { if (it.id.isNotBlank()) dynamicRaffleTitles[it.id] = it }
        snap.personalTitles.forEach { if (it.id.isNotBlank()) dynamicPersonalTitles[it.id] = it }
        snap.growthV5Ids.forEach { if (it.isNotBlank()) dynamicGrowthV5Ids.add(it) }
        val v2Art = snap.v2ArtSpecs.mapNotNull { (k, vals) ->
            if (vals.size >= 3) k to PersonalV2ArtSpec(vals[0], vals[1], vals[2]) else null
        }.toMap()
        val v1Pal = snap.v1Palettes.mapNotNull { (k, vals) ->
            if (vals.size >= 3) k to PersonalV1PaletteSpec(vals[0], vals[1], vals[2]) else null
        }.toMap()
        ChatBubbleCosmetics.applyWebDynamicAssets(
            v2ArtSpecs = v2Art,
            v1Palettes = v1Pal,
            symbolPaths = snap.titleSymbols,
        )
        _catalogRevision.value += 1
        return true
    }

    fun resolveOfficialTitleName(
        id: String,
        officialTitles: List<OfficialTitleMeta> = emptyList(),
    ): OfficialTitleMeta? {
        officialTitles.firstOrNull { it.id == id && it.name.isNotBlank() }?.let { return it }
        dynamicOfficialTitles[id]?.let { return it }
        return BUILT_IN_OFFICIAL_NAMES[id]?.let { (name, mood) ->
            OfficialTitleMeta(id = id, name = name, mood = mood)
        }
    }

    fun resolveGrowthRewardMeta(grId: String): GrowthRewardMeta? {
        dynamicGrowthById[grId]?.let { return it }
        val match = Regex("""^(guess|luck_king|luck_last|rank_[a-z_]+)[_:](\d)$""").matchEntire(grId.trim())
            ?: return null
        val rawSeries = match.groupValues[1]
        val lv = match.groupValues[2].toIntOrNull()?.coerceIn(1, 5) ?: 1
        val (series, board, period) = if (rawSeries.startsWith("rank_")) {
            val suffix = if (rawSeries.endsWith("_all")) "all" else "day"
            val brd = rawSeries.removePrefix("rank_").removeSuffix("_all").removeSuffix("_day")
            Triple(rawSeries, brd, suffix)
        } else {
            Triple(rawSeries, null, null)
        }
        val fallbackLabel = resolveBadgeMeta(series)?.name ?: when (series) {
            "luck_king" -> "手气王"
            "luck_last" -> "手气亡"
            else -> "猜词成长"
        }
        return GrowthRewardMeta(
            id = grId,
            series = series,
            board = board,
            period = period,
            level = lv,
            name = fallbackLabel,
            label = fallbackLabel,
        )
    }

    fun resolveActiveRaffle(
        decoObj: JsonObject,
        cosmeticAccess: String? = null,
        nowSec: Long = ServerClock.serverNowSec(),
    ): ActiveRaffleInfo? {
        val ftId = decoObj["ft"]?.jsonPrimitive?.contentOrNull
            ?: decoObj["equippedRaffleId"]?.jsonPrimitive?.contentOrNull
            ?: return null
        val cat = resolveRaffleTitle(ftId) ?: return null
        val owner = cosmeticAccess == "all"
        val fxp = decoObj["fxp"]?.jsonPrimitive?.longOrNull ?: 0L
        val fp = decoObj["fp"]?.jsonPrimitive?.intOrNull ?: 0
        val expired = !owner && fxp in 1L..nowSec
        val fn = decoObj["fn"]?.jsonPrimitive?.intOrNull
            ?: decoObj["equippedRaffleLevel"]?.jsonPrimitive?.intOrNull
            ?: 1
        val earnedRaw = when {
            owner -> TITLE_MAX_LEVEL
            expired -> fp
            else -> fn
        }
        if (expired && earnedRaw <= 0) return null
        val n = earnedRaw.coerceIn(1, TITLE_MAX_LEVEL)
        val fl = decoObj["fl"]?.jsonPrimitive?.intOrNull ?: n
        val displayLevel = fl.coerceIn(1, n)
        return ActiveRaffleInfo(
            catalog = cat,
            earnedLevel = n,
            displayLevel = displayLevel,
            stage = raffleStage(displayLevel),
        )
    }

    fun updateCommunitySkinsCatalog(rawJson: String) {
        val root = runCatching { ForHappyJson.parseToJsonElement(rawJson).jsonObject }.getOrNull() ?: return
        val list = ArrayList<JsonObject>()
        root["entries"]?.let { el ->
            runCatching { el.jsonArray }.getOrNull()?.mapNotNullTo(list) { runCatching { it.jsonObject }.getOrNull() }
        }
        root["archived"]?.let { el ->
            runCatching { el.jsonArray }.getOrNull()?.mapNotNullTo(list) { runCatching { it.jsonObject }.getOrNull() }
        }
        for (item in list) {
            val id = item["id"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
            val name = item["name"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
            if (id.isEmpty() || name.isEmpty()) continue
            val author = item["author"]?.jsonPrimitive?.contentOrNull ?: ""
            val comps = runCatching { item["components"]?.jsonObject }.getOrNull()
            fun compHash(kind: String): Triple<Boolean, String?, String?> {
                val c = runCatching { comps?.get(kind)?.jsonObject }.getOrNull() ?: return Triple(false, null, null)
                val type = c["type"]?.jsonPrimitive?.contentOrNull
                val sha = c["sha256"]?.jsonPrimitive?.contentOrNull
                val styleSha = runCatching { c["style"]?.jsonObject?.get("sha256")?.jsonPrimitive?.contentOrNull }.getOrNull()
                return if (type == "text/css") Triple(true, null, sha) else Triple(true, sha, styleSha)
            }
            fun compSlice(kind: String): IntArray? {
                val arr = runCatching { comps?.get(kind)?.jsonObject?.get("slice")?.jsonArray }.getOrNull() ?: return null
                if (arr.size != 4) return null
                return IntArray(4) { i -> arr[i].jsonPrimitive.intOrNull ?: 0 }
            }
            val (hasT, tImg, tCss) = compHash("title")
            val (hasBd, bdImg, bdCss) = compHash("badge")
            val (hasBb, bbImg, bbCss) = compHash("bubble")
            val (hasRp, rpImg, rpCss) = compHash("reply")
            val (hasFr, frImg, frCss) = compHash("frame")
            val base = COMMUNITY_SKINS[id]
            dynamicCommunitySkins[id] = CommunitySkinCatalogItem(
                id = id,
                name = name,
                author = author.ifBlank { base?.author.orEmpty() },
                hasTitle = hasT || (base?.hasTitle == true),
                hasBadge = hasBd || (base?.hasBadge == true),
                hasBubble = hasBb || (base?.hasBubble == true),
                hasReply = hasRp || (base?.hasReply == true),
                bubbleImgSha = bbImg ?: base?.bubbleImgSha,
                bubbleCssSha = bbCss ?: base?.bubbleCssSha,
                bubbleSlice = compSlice("bubble") ?: base?.bubbleSlice,
                titleImgSha = tImg ?: base?.titleImgSha,
                titleCssSha = tCss ?: base?.titleCssSha,
                titleSlice = compSlice("title") ?: base?.titleSlice,
                badgeImgSha = bdImg ?: base?.badgeImgSha,
                badgeCssSha = bdCss ?: base?.badgeCssSha,
                replyImgSha = rpImg ?: base?.replyImgSha,
                replyCssSha = rpCss ?: base?.replyCssSha,
                replySlice = compSlice("reply") ?: base?.replySlice,
                hasFrame = hasFr || (base?.hasFrame == true),
                frameImgSha = frImg ?: base?.frameImgSha,
                frameCssSha = frCss ?: base?.frameCssSha,
            )
        }
        _catalogRevision.value += 1
    }

    fun updateFromMeResponse(resp: MeResponse) {
        resp.officialTitles.forEach { t ->
            if (t.id.isNotBlank() && t.name.isNotBlank()) {
                dynamicOfficialTitles[t.id] = t
            }
        }
        resp.user?.officialTitles?.forEach { t ->
            if (t.id.isNotBlank() && t.name.isNotBlank()) {
                dynamicOfficialTitles[t.id] = t
            }
        }
        val allGrowth = resp.guessRewards + resp.luckRewards + resp.rankRewards + resp.activityRewards
        allGrowth.forEach { r ->
            if (r.id.isNotBlank()) dynamicGrowthById[r.id] = r
        }
        allGrowth.groupBy { it.series ?: "guess" }.forEach { (series, list) ->
            dynamicGrowthBySeries[series] = list.sortedBy { it.level }
        }
        resp.activityRewards.filter { it.level == 1 }.forEach { r ->
            val series = r.series?.takeIf { it.isNotBlank() } ?: return@forEach
            val label = r.label?.takeIf { it.isNotBlank() } ?: r.name
            if (label.isNotBlank()) {
                val levels = dynamicGrowthBySeries[series]?.map { it.wins }?.takeIf { it.isNotEmpty() } ?: GROWTH_BADGE_LEVELS
                dynamicBadges[series] = BadgeCatalogEntry(
                    id = series,
                    name = label,
                    tier = "epic",
                    glyph = r.symbol?.takeIf { it.isNotBlank() } ?: "✦",
                    desc = r.desc,
                    unit = r.unit?.takeIf { it.isNotBlank() } ?: "次",
                    levels = levels,
                )
            }
        }
        resp.farmBadges.forEach { b ->
            if (b.id.isNotBlank() && b.name.isNotBlank()) {
                dynamicBadges[b.id] = BadgeCatalogEntry(
                    id = b.id,
                    name = b.name,
                    tier = b.tier.ifBlank { "rare" },
                    glyph = "✿",
                    desc = b.desc,
                    unit = b.unit.ifBlank { "次" },
                    levels = b.levels.ifEmpty { DEFAULT_BADGE_LEVELS },
                )
            }
        }
        resp.rankRewards.filter { it.level == 1 }.forEach { r ->
            val series = r.series?.takeIf { it.isNotBlank() } ?: return@forEach
            val label = r.label?.takeIf { it.isNotBlank() } ?: r.name
            if (label.isNotBlank()) {
                val levels = dynamicGrowthBySeries[series]?.map { it.wins }?.takeIf { it.isNotEmpty() } ?: GROWTH_BADGE_LEVELS
                dynamicBadges[series] = BadgeCatalogEntry(
                    id = series,
                    name = label,
                    tier = "epic",
                    glyph = r.symbol?.takeIf { it.isNotBlank() } ?: "♛",
                    desc = "日榜、总榜各自每日前三 +3 / +2 / +1 经验，1 / 3 / 10 / 30 / 100 经验升级",
                    unit = "经验",
                    levels = levels,
                )
            }
        }
        _catalogRevision.value += 1
    }

    fun resolveBadgeMeta(id: String): BadgeCatalogEntry? {
        val clean = id.trim()
        if (clean.isEmpty()) return null
        dynamicBadges[clean]?.let { return it }
        BUILT_IN_BADGES[clean]?.let { return it }
        val rankMatch = RANK_REGEX.matchEntire(clean)
        if (rankMatch != null) {
            val board = rankMatch.groupValues[1]
            val period = rankMatch.groupValues[2]
            val boardLabel = when (board) {
                "guess" -> "猜词榜"
                "guess_winrate" -> "猜词胜率榜"
                "sender_count" -> "发包场次榜"
                "sender_paid" -> "发包慷慨榜"
                "chat_count" -> "群聊活跃榜"
                "chat_early" -> "早起打卡榜"
                "chat_late" -> "守夜打卡榜"
                else -> "荣誉榜"
            }
            val suffix = if (period == "all") "总榜" else "日榜"
            return BadgeCatalogEntry(
                id = clean,
                name = "$boardLabel·$suffix",
                tier = if (period == "all") "legend" else "epic",
                glyph = "♛",
                desc = "排行榜前三荣誉徽章",
                unit = "经验",
                levels = GROWTH_BADGE_LEVELS,
            )
        }
        return null
    }

    fun resolveBadgeArt(id: String, level: Int = 1): BadgeArtKind {
        val clean = id.trim()
        if (clean == "resource_respect") {
            return BadgeArtKind.BundledSvg("badges/resource-respect.svg")
        }
        if (clean in GROWTH_V5_IDS) {
            return BadgeArtKind.BundledSvg("badges/v5/$clean.svg")
        }
        if (clean in dynamicGrowthV5Ids) {
            val meta = resolveBadgeMeta(clean)
            return BadgeArtKind.RemoteSvg(
                url = "https://52dmeme.com/assets/badges/v5/$clean.svg",
                fallbackGlyph = meta?.glyph ?: "✿",
            )
        }
        val rankMatch = RANK_REGEX.matchEntire(clean)
        val tile = if (rankMatch != null) {
            val boardIdx = RANK_BOARDS.indexOf(rankMatch.groupValues[1]).coerceAtLeast(0)
            (if (rankMatch.groupValues[2] == "all") 24 else 31) + boardIdx
        } else {
            ATLAS_V4_TILES[clean]
        }
        if (tile != null) {
            val style = when {
                tile < 12 -> "anime"
                tile < 24 -> "ink"
                tile < 36 -> "enamel"
                else -> "pixel"
            }
            return BadgeArtKind.AtlasV4(
                tileIndex = tile,
                style = style,
                isHonor = rankMatch?.groupValues?.get(2) == "all",
                isDay = rankMatch?.groupValues?.get(2) == "day",
            )
        }
        val meta = resolveBadgeMeta(clean)
        return BadgeArtKind.EmojiGlyph(meta?.glyph ?: "✦")
    }

    fun computeBadgeLevel(id: String, count: Int): Int {
        if (id == "elder") return 5
        if (count <= 0) return 0
        val thresholds = resolveBadgeMeta(id)?.levels ?: DEFAULT_BADGE_LEVELS
        return thresholds.count { count >= it }.coerceIn(1, 5)
    }

    fun formatBadgeLevelText(id: String, level: Int): String = when {
        id == "elder" -> "★ 唯一"
        level <= 0 -> "未获得"
        else -> "Lv$level ${BADGE_LV_NAMES.getOrElse(level) { "" }}"
    }

    fun raffleBadgeUrl(id: String): String = when (id) {
        "oldfarmer" -> "https://52dmeme.com/assets/farm-memorial/carrot-v3.svg"
        "chickengun" -> "https://52dmeme.com/assets/raffle/bubble-growth-v8/chickengun-badge.webp"
        "muscle" -> "https://52dmeme.com/assets/raffle/muscle-tail-still.webp"
        "whalebuddy" -> "https://52dmeme.com/assets/raffle/collection/whalebuddy-badge.webp"
        else -> "https://52dmeme.com/assets/raffle/anime-v6/$id-badge.webp"
    }

    fun parseOwnedBadges(
        badgesJson: JsonElement?,
        farmProgress: Map<String, Int> = emptyMap(),
    ): List<OwnedBadgeItem> {
        val arr = runCatching { badgesJson?.jsonArray }.getOrNull() ?: return emptyList()
        val result = ArrayList<OwnedBadgeItem>(arr.size)
        for (el in arr) {
            val (rawId, rawN, lastDay) = when (el) {
                is JsonPrimitive -> {
                    val s = el.contentOrNull?.trim().orEmpty()
                    if (s.isEmpty()) continue
                    Triple(s, 1, null)
                }
                is JsonObject -> {
                    val id = el["id"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
                    if (id.isEmpty()) continue
                    val n = el["n"]?.jsonPrimitive?.intOrNull ?: 1
                    val day = el["last_day"]?.jsonPrimitive?.contentOrNull
                    Triple(id, n, day)
                }
                else -> continue
            }
            val meta = resolveBadgeMeta(rawId) ?: continue
            val effectiveCount = maxOf(rawN, farmProgress[rawId] ?: 0, 1)
            val lv = computeBadgeLevel(rawId, effectiveCount)
            val nextAt = if (rawId == "elder") 0 else meta.levels.getOrNull(lv) ?: 0
            val rem = if (nextAt > 0) (nextAt - effectiveCount).coerceAtLeast(0) else 0
            result.add(
                OwnedBadgeItem(
                    id = rawId,
                    count = effectiveCount,
                    level = lv,
                    levelText = formatBadgeLevelText(rawId, lv),
                    nextAt = nextAt,
                    remainingToNext = rem,
                    lastDay = lastDay,
                    owned = true,
                    meta = meta,
                    art = resolveBadgeArt(rawId, lv),
                ),
            )
        }
        return result
    }

    fun buildBadgeWall(
        badgesJson: JsonElement?,
        farmProgress: Map<String, Int> = emptyMap(),
        allCosmetics: Boolean = false,
    ): List<OwnedBadgeItem> {
        val ownedList = parseOwnedBadges(badgesJson, farmProgress)
        val ownedMap = ownedList.associateBy { it.id }
        val orderedIds = LinkedHashSet<String>().apply {
            ownedList.forEach { add(it.id) }
            BUILT_IN_BADGES.keys.forEach { add(it) }
            dynamicBadges.keys.forEach { add(it) }
        }
        return orderedIds.mapNotNull { id ->
            ownedMap[id] ?: run {
                val meta = resolveBadgeMeta(id) ?: return@mapNotNull null
                val prog = farmProgress[id] ?: 0
                val lv = if (allCosmetics) (if (id == "guide") 4 else 5) else computeBadgeLevel(id, prog)
                val nextAt = if (id == "elder") 0 else meta.levels.getOrNull(lv) ?: 0
                val rem = if (nextAt > 0) (nextAt - prog).coerceAtLeast(0) else 0
                OwnedBadgeItem(
                    id = id,
                    count = prog,
                    level = lv.coerceAtLeast(1),
                    levelText = if (allCosmetics) "最高阶外观" else formatBadgeLevelText(id, lv),
                    nextAt = nextAt,
                    remainingToNext = rem,
                    lastDay = null,
                    owned = allCosmetics,
                    meta = meta,
                    art = resolveBadgeArt(id, lv.coerceAtLeast(1)),
                )
            }
        }
    }

    /**
     * Resolves nickname gradient, 1 active title, and at most 1 active equipped badge
     * strictly following `app.js` `uName` + `titleEl` + `badgeEl`.
     * Unequipped badges in `user.badges` are NEVER dumped into the nickname row.
     */
    private fun blendHex(aHex: String, bHex: String, weightA: Float): String {
        fun parseRgb(raw: String): Triple<Int, Int, Int> {
            val clean = raw.trim().removePrefix("#")
            val full = if (clean.length == 3) "${clean[0]}${clean[0]}${clean[1]}${clean[1]}${clean[2]}${clean[2]}" else clean.take(6)
            val v = full.toIntOrNull(16) ?: return Triple(31, 107, 79)
            return Triple((v shr 16) and 0xFF, (v shr 8) and 0xFF, v and 0xFF)
        }
        val (ar, ag, ab) = parseRgb(aHex)
        val (br, bg, bb) = parseRgb(bHex)
        val w = weightA.coerceIn(0f, 1f)
        val r = (ar * w + br * (1f - w)).toInt().coerceIn(0, 255)
        val g = (ag * w + bg * (1f - w)).toInt().coerceIn(0, 255)
        val b = (ab * w + bb * (1f - w)).toInt().coerceIn(0, 255)
        return "#%02x%02x%02x".format(r, g, b)
    }

    private val COMMUNITY_SLUG_REGEX = Regex("""^[a-z0-9][a-z0-9-]{2,47}$""")

    fun isValidCommunitySlug(id: String?): Boolean =
        !id.isNullOrBlank() && COMMUNITY_SLUG_REGEX.matches(id)

    fun resolveCommunityAvatarFrameUrl(
        decoJson: JsonElement?,
        where: String = "chat",
    ): String? {
        val obj = runCatching { decoJson?.jsonObject }.getOrNull() ?: return null
        val rawSc = obj["sc"]?.jsonPrimitive?.contentOrNull ?: obj["decoScope"]?.jsonPrimitive?.contentOrNull ?: "all"
        val sc = if (rawSc == "wall") "post" else rawSc
        val effWhere = if (where == "wall") "post" else where
        if (effWhere != "card" && sc != "all" && sc != effWhere) return null
        val commObj = runCatching { obj["community"]?.jsonObject }.getOrNull() ?: return null
        val frameId = commObj["frame"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
        if (!isValidCommunitySlug(frameId)) return null
        val skin = resolveCommunitySkinMeta(frameId) ?: return null
        val imgSha = skin.frameImgSha?.takeIf { it.isNotBlank() } ?: return null
        return "https://52dmeme.com/api/community-skins/image/$imgSha"
    }

    fun resolveEquippedIdentity(
        decoJson: JsonElement?,
        badgesJson: JsonElement? = null,
        officialTitles: List<OfficialTitleMeta> = emptyList(),
        where: String = "card",
        isAdmin: Boolean = false,
        hasFancyPermission: Boolean? = null,
        cosmeticAccess: String? = null,
        nowSec: Long = ServerClock.serverNowSec(),
        isDarkTheme: Boolean = false,
    ): ResolvedUserIdentity {
        val obj = runCatching { decoJson?.jsonObject }.getOrNull()

        // 1. Nickname gradient (`app.js:1707` nameCls is independent of deco `sc` scope)
        val fcArr = runCatching { obj?.get("fc")?.jsonArray }.getOrNull()
            ?.mapNotNull { it.jsonPrimitive.contentOrNull?.takeIf { s -> s.startsWith("#") } }
            .orEmpty()
        val legacyGrad = when (obj?.get("nickGradient")?.jsonPrimitive?.contentOrNull) {
            "pine" -> listOf("#1B7F4B", "#34A853")
            "sunset" -> listOf("#EA580C", "#EC4899")
            "aurora" -> listOf("#06B6D4", "#8B5CF6")
            "gold" -> listOf("#D97706", "#F59E0B")
            "sakura" -> listOf("#ff5fa2", "#ffb3d1", "#c86bff")
            "salt" -> listOf("#00b4d8", "#48cae4", "#90e0ef")
            "cyber" -> listOf("#f72585", "#7209b7", "#4cc9f0")
            "blackgold" -> listOf("#111111", "#d4a017", "#fff3b0")
            else -> emptyList()
        }
        val rawCustomFc = fcArr.ifEmpty { legacyGrad }
        val nickHexes = when {
            isAdmin -> ADMIN_RAINBOW_HEXES
            hasFancyPermission == true -> rawCustomFc.ifEmpty { DEFAULT_FANCY_RAINBOW_HEXES }
            hasFancyPermission == false -> emptyList()
            else -> rawCustomFc
        }

        if (obj == null) {
            return ResolvedUserIdentity(nickGradientHexes = nickHexes)
        }

        val rawSc = obj["sc"]?.jsonPrimitive?.contentOrNull ?: obj["decoScope"]?.jsonPrimitive?.contentOrNull ?: "all"
        val sc = if (rawSc == "wall") "post" else rawSc
        val effWhere = if (where == "wall") "post" else where
        val scMatches = effWhere == "card" || sc == "all" || sc == effWhere

        val communityObj = runCatching { obj["community"]?.jsonObject }.getOrNull()
        val communityTitleId = if (scMatches) {
            communityObj?.get("title")?.jsonPrimitive?.contentOrNull
                ?: obj["communityTitleId"]?.jsonPrimitive?.contentOrNull
        } else {
            null
        }
        val communityBadgeId = if (scMatches) {
            communityObj?.get("badge")?.jsonPrimitive?.contentOrNull
                ?: obj["communityBadgeId"]?.jsonPrimitive?.contentOrNull
        } else {
            null
        }

        val ptId = if (scMatches) obj["pt"]?.jsonPrimitive?.contentOrNull ?: obj["equippedPersonalId"]?.jsonPrimitive?.contentOrNull else null
        val otId = if (scMatches) obj["ot"]?.jsonPrimitive?.contentOrNull ?: obj["equippedOfficialId"]?.jsonPrimitive?.contentOrNull else null
        val grId = if (scMatches) obj["gr"]?.jsonPrimitive?.contentOrNull ?: obj["equippedGrowthId"]?.jsonPrimitive?.contentOrNull else null
        val rtId = if (scMatches) {
            obj["rt"]?.jsonPrimitive?.contentOrNull
                ?: if (obj["equippedInviteTitle"]?.jsonPrimitive?.booleanOrNull == true) "guangjie" else null
        } else {
            null
        }
        val customText = if (scMatches) {
            (obj["t"]?.jsonPrimitive?.contentOrNull
                ?: obj["customTitle"]?.jsonPrimitive?.contentOrNull
                ?: obj["title"]?.jsonPrimitive?.contentOrNull)
                ?.trim()
                .orEmpty()
        } else {
            ""
        }

        val commTitleMeta = communityTitleId?.takeIf { it.isNotBlank() }?.let { resolveCommunitySkinMeta(it) }
        val commTitleAllowed = isValidCommunitySlug(communityTitleId) && (commTitleMeta == null || commTitleMeta.hasTitle)
        val commBadgeMeta = communityBadgeId?.takeIf { it.isNotBlank() }?.let { resolveCommunitySkinMeta(it) }
        val commBadgeAllowed = isValidCommunitySlug(communityBadgeId) && (commBadgeMeta == null || commBadgeMeta.hasBadge)
        val growthMeta = grId?.takeIf { it.isNotBlank() }?.let { resolveGrowthRewardMeta(it) }
        val activeRaffle = resolveActiveRaffle(obj, cosmeticAccess, nowSec)
        val officialMeta = otId?.takeIf { it.isNotBlank() }?.let { resolveOfficialTitleName(it, officialTitles) }

        // 2. Title resolution (`titleEl` priority order)
        val resolvedTitle: ResolvedTitleChip? = when {
            isOwner52Suit(ptId) -> {
                val suitId = ptId!!
                val p = resolvePersonalTitle(suitId) ?: PersonalTitleCatalogItem(suitId, "52帝", "站长专属", "SSR", "owner")
                val isB = suitId == "owner-52b" || suitId == "owner52b"
                val artUrl = if (isB) {
                    "https://52dmeme.com/assets/owner52-v032/title-tiger-integrated-v1.webp"
                } else {
                    "https://52dmeme.com/assets/owner52-v031/title-dragon-integrated-v1.webp"
                }
                ResolvedTitleChip(
                    key = "personal:$suitId",
                    label = p.name,
                    levelText = null,
                    rarity = "SSR",
                    bgHex = "#1E293B",
                    textHex = "#FDE68A",
                    borderHex = "#F59E0B",
                    fillMode = "grad",
                    shape = "pill",
                    secondaryBgHex = "#0F172A",
                    effect = "shine",
                    bgImageUrl = artUrl,
                    specialKind = "owner52",
                )
            }
            commTitleAllowed -> {
                ChatBubbleCosmetics.resolveCommunityTitleStyle(communityTitleId!!, commTitleMeta, isDarkTheme)
            }
            growthMeta != null -> {
                val (bgHex, secHex, fgHex, borderHex) = when {
                    growthMeta.series == "luck_king" -> if (isDarkTheme) {
                        listOf("#3B2506", "#4A2E08", "#FDE68A", "#F59E0B")
                    } else {
                        listOf("#FEF3C7", "#FDE68A", "#92400E", "#F59E0B")
                    }
                    growthMeta.series == "luck_last" -> if (isDarkTheme) {
                        listOf("#241B3B", "#312352", "#DDD6FE", "#8B5CF6")
                    } else {
                        listOf("#EDE9FE", "#DDD6FE", "#5B21B6", "#8B5CF6")
                    }
                    growthMeta.series?.startsWith("rank_") == true -> if (isDarkTheme) {
                        listOf("#172554", "#1E3A8A", "#DBEAFE", "#60A5FA")
                    } else {
                        listOf("#DBEAFE", "#BFDBFE", "#1E40AF", "#60A5FA")
                    }
                    else -> if (isDarkTheme) {
                        listOf("#2E1065", "#3B0764", "#F3E8FF", "#A855F7")
                    } else {
                        listOf("#F3E8FF", "#E9D5FF", "#6B21A8", "#A855F7")
                    }
                }
                ResolvedTitleChip(
                    key = "growth:$grId",
                    label = growthMeta.name,
                    levelText = "Lv${growthMeta.level}",
                    rarity = if (growthMeta.level >= 4) "SSR" else "rare",
                    bgHex = bgHex,
                    textHex = fgHex,
                    borderHex = borderHex,
                    fillMode = "grad",
                    shape = "pill",
                    secondaryBgHex = secHex,
                    effect = if (growthMeta.level >= 5) "shine" else "none",
                    specialKind = "growth",
                )
            }
            !ptId.isNullOrBlank() && resolvePersonalTitle(ptId) != null -> {
                val p = resolvePersonalTitle(ptId)!!
                if (p.art == "v2") {
                    val isSsr = p.rarity.equals("SSR", ignoreCase = true)
                    val artSpec = ChatBubbleCosmetics.resolvePersonalV2Art(ptId)
                    ResolvedTitleChip(
                        key = "personal:$ptId",
                        label = p.name,
                        levelText = null,
                        rarity = p.rarity,
                        bgHex = if (isSsr) "#7C2D12" else "#334155",
                        textHex = "#304052",
                        borderHex = if (isSsr) "#F59E0B" else "#94A3B8",
                        fillMode = "grad",
                        shape = "pill",
                        secondaryBgHex = if (isSsr) "#431407" else "#1E293B",
                        bgImageUrl = "https://52dmeme.com/assets/personal-v2/$ptId-title.webp",
                        specialKind = "personal_v2",
                        personalV2Art = artSpec,
                    )
                } else {
                    val pal = ChatBubbleCosmetics.resolvePersonalV1Palette(ptId)
                    ResolvedTitleChip(
                        key = "personal:$ptId",
                        label = p.name,
                        levelText = null,
                        rarity = p.rarity,
                        bgHex = pal.paperHex,
                        textHex = pal.inkHex,
                        borderHex = pal.inkHex,
                        fillMode = "solid",
                        shape = "pill",
                        specialKind = "personal_v1",
                        hardShadowHex = pal.accentHex,
                        hardShadowOffsetDp = 1.5f to 1.5f,
                    )
                }
            }
            activeRaffle != null -> {
                val r = activeRaffle.catalog
                val earnedLv = activeRaffle.earnedLevel
                val earnedStage = raffleStage(earnedLv)
                if (r.id == "claude") {
                    ResolvedTitleChip(
                        key = "raffle:claude",
                        label = "拾光司书",
                        levelText = if (earnedLv > 1) "Lv$earnedLv" else null,
                        rarity = "SSR",
                        bgHex = "#FAF0DC",
                        textHex = "#542710",
                        borderHex = "#B8894A",
                        fillMode = "grad",
                        shape = "ribbon",
                        secondaryBgHex = "#EAD3AE",
                        effect = if (earnedStage >= 6) "shine" else "none",
                        specialKind = "shiguang",
                        raffleColorHex = r.colorHex,
                        raffleAccentHex = r.accentHex,
                        raffleStage = earnedStage,
                    )
                } else {
                    val isOldFarmer = r.id == "oldfarmer"
                    val npBg = if (isDarkTheme) blendHex(r.colorHex, "#15202B", 0.34f) else blendHex(r.accentHex, "#FFFFFF", 0.28f)
                    val npSec = if (isDarkTheme) blendHex(r.accentHex, "#15202B", 0.20f) else blendHex(r.colorHex, "#FFFFFF", 0.16f)
                    val npFg = if (isDarkTheme) "#F1F5F9" else blendHex(r.colorHex, "#212E40", 0.70f)
                    val symPath = ChatBubbleCosmetics.TITLE_SYMBOL_PATHS[r.id]
                        ?: r.symbolKey.takeIf { it.isNotBlank() }?.let { ChatBubbleCosmetics.resolveTitleSymbolPath(it) }
                        ?: ChatBubbleCosmetics.resolveTitleSymbolPath(r.id)
                    val bannerSpec = ChatBubbleCosmetics.RAFFLE_TITLE_BANNERS[r.id]
                    ResolvedTitleChip(
                        key = "raffle:${r.id}",
                        label = r.name,
                        levelText = if (earnedLv > 1) "Lv$earnedLv" else null,
                        rarity = r.rarity,
                        bgHex = npBg,
                        textHex = npFg,
                        borderHex = r.colorHex,
                        fillMode = "grad",
                        shape = "tag",
                        secondaryBgHex = npSec,
                        effect = if (earnedStage >= 8) "shine" else "none",
                        symbolSvgPath = symPath,
                        bgImageUrl = bannerSpec?.let { "https://52dmeme.com${it.bannerPath}" },
                        specialKind = if (isOldFarmer) "raffle_oldfarmer" else "raffle_nameplate",
                        raffleColorHex = r.colorHex,
                        raffleAccentHex = r.accentHex,
                        raffleStage = earnedStage,
                    )
                }
            }
            officialMeta != null -> {
                val pal = ChatBubbleCosmetics.OFFICIAL_PALETTES[otId]
                if (pal != null) {
                    ResolvedTitleChip(
                        key = "official:$otId",
                        label = officialMeta.name,
                        levelText = null,
                        rarity = "SSR",
                        bgHex = if (isDarkTheme && otId != "diver") "#1E293B" else pal.softHex,
                        textHex = if (isDarkTheme && otId != "diver") pal.edgeHex else pal.inkHex,
                        borderHex = pal.edgeHex,
                        fillMode = "solid",
                        shape = "tag",
                        specialKind = "official",
                        customCss = ParsedSafeCss(cornerRadiiDp = pal.cornersDp),
                        officialPalette = pal,
                        hardShadowHex = pal.accentHex,
                        hardShadowOffsetDp = 0f to 2f,
                    )
                } else {
                    ResolvedTitleChip("official:$otId", officialMeta.name, null, "SSR", "#1D4ED8", "#FFFFFF", "#60A5FA", specialKind = "official")
                }
            }
            rtId == "guangjie" -> {
                ResolvedTitleChip(
                    key = "reward:guangjie",
                    label = "✿ 广结善缘",
                    levelText = null,
                    rarity = "rare",
                    bgHex = if (isDarkTheme) "#24351A" else "#EFFBDC",
                    textHex = if (isDarkTheme) "#D7F3AF" else "#3B5B1D",
                    borderHex = if (isDarkTheme) "#638D39" else "#9BC665",
                    fillMode = "solid",
                    shape = "pill",
                    secondaryBgHex = if (isDarkTheme) "#1B2913" else "#E2F5C7",
                    specialKind = "guangjie",
                    hardShadowHex = if (isDarkTheme) "#182411" else "#BAD78E",
                    hardShadowOffsetDp = 0f to 2f,
                )
            }
            customText.isNotEmpty() -> {
                val fg = obj["c"]?.jsonPrimitive?.contentOrNull ?: obj["textHex"]?.jsonPrimitive?.contentOrNull ?: "#FFFFFF"
                val bg = obj["c2"]?.jsonPrimitive?.contentOrNull ?: obj["primaryHex"]?.jsonPrimitive?.contentOrNull ?: "#1F6B4F"
                val bc = obj["bc"]?.jsonPrimitive?.contentOrNull ?: obj["secondaryHex"]?.jsonPrimitive?.contentOrNull ?: bg
                val sh = obj["sh"]?.jsonPrimitive?.contentOrNull ?: obj["titleStyle"]?.jsonPrimitive?.contentOrNull ?: "pill"
                val fill = obj["fill"]?.jsonPrimitive?.contentOrNull ?: obj["fillMode"]?.jsonPrimitive?.contentOrNull ?: "solid"
                val fx = obj["fx"]?.jsonPrimitive?.contentOrNull ?: "none"
                val fontKey = obj["f"]?.jsonPrimitive?.contentOrNull.orEmpty()
                val csst = ChatBubbleCosmetics.parseSafeCss(obj["csst"]?.jsonPrimitive?.contentOrNull)
                ResolvedTitleChip(
                    key = "custom",
                    label = customText,
                    levelText = null,
                    rarity = "normal",
                    bgHex = bg,
                    textHex = fg,
                    borderHex = bc,
                    fillMode = fill,
                    shape = sh,
                    secondaryBgHex = bc,
                    effect = fx,
                    fontKey = fontKey,
                    customCss = csst,
                )
            }
            else -> null
        }

        // 3. Single Equipped Badge resolution (`badgeEl` priority order)
        val badgeMode = obj["badge_mode"]?.jsonPrimitive?.contentOrNull
            ?: obj["badgeMode"]?.jsonPrimitive?.contentOrNull
            ?: "set"
        val rawBs = obj["bs"]?.jsonPrimitive?.contentOrNull
            ?: obj["badgeScope"]?.jsonPrimitive?.contentOrNull
            ?: "all"
        val bs = if (rawBs == "wall") "post" else rawBs
        if (badgeMode == "none") {
            return ResolvedUserIdentity(title = resolvedTitle, badge = null, nickGradientHexes = nickHexes)
        }
        val bsMatches = effWhere == "card" || bs == "all" || bs == effWhere

        val bdId = (obj["bd"]?.jsonPrimitive?.contentOrNull
            ?: obj["equippedBadgeId"]?.jsonPrimitive?.contentOrNull)
            ?.trim()
            .orEmpty()
        val ownedBadges = parseOwnedBadges(badgesJson)

        fun buildCustomBadge(targetId: String): ResolvedBadgeChip? {
            if (!bsMatches || targetId.isEmpty()) return null
            val meta = resolveBadgeMeta(targetId) ?: return null
            val ownedN = ownedBadges.firstOrNull { it.id == targetId }?.count
            val bn = obj["bn"]?.jsonPrimitive?.intOrNull
                ?: ownedN
                ?: if (cosmeticAccess == "all") 999999 else 1
            val bl = obj["bl"]?.jsonPrimitive?.intOrNull ?: computeBadgeLevel(targetId, bn).coerceAtLeast(1)
            val isGrowthBadge = targetId == "guess" || targetId == "luck_king" ||
                targetId == "luck_last" || targetId.startsWith("rank_")
            return ResolvedBadgeChip(
                id = targetId,
                name = meta.name,
                level = bl,
                levelText = if (targetId == "elder") "★" else "Lv$bl",
                tier = meta.tier,
                desc = meta.desc,
                art = resolveBadgeArt(targetId, bl),
                alwaysIconOnly = isGrowthBadge && effWhere != "card",
            )
        }

        val resolvedBadge: ResolvedBadgeChip? = when {
            isOwner52Suit(ptId) && badgeMode != "custom" -> {
                val suitId = ptId!!
                val p = resolvePersonalTitle(suitId) ?: PersonalTitleCatalogItem(suitId, "52帝", "站长专属", "SSR", "owner")
                val isB = suitId == "owner-52b" || suitId == "owner52b"
                ResolvedBadgeChip(
                    id = "pt:$suitId",
                    name = p.name,
                    level = 5,
                    levelText = "",
                    tier = "legend",
                    desc = p.mood,
                    art = BadgeArtKind.SpriteSheetCell(
                        url = "https://52dmeme.com/assets/owner52-v031/regalia.webp",
                        cols = 4,
                        rows = 2,
                        col = if (isB) 2 else 0,
                        row = 1,
                    ),
                    alwaysIconOnly = true,
                )
            }
            commBadgeAllowed -> {
                val cid = communityBadgeId!!
                val badgeSpec = ChatBubbleCosmetics.resolveCommunityBadgeSpec(
                    id = cid,
                    imgSha = commBadgeMeta?.badgeImgSha,
                    cssSha = commBadgeMeta?.badgeCssSha,
                    isDarkTheme = isDarkTheme,
                )
                val badgeArt: BadgeArtKind = when {
                    cid == "aurora-preset" -> BadgeArtKind.InlineXmlSvg(AURORA_COMPASS_SVG)
                    !commBadgeMeta?.badgeCssSha.isNullOrBlank() -> BadgeArtKind.CommunityCssBadge(
                        id = cid,
                        imgSha = commBadgeMeta?.badgeImgSha,
                        cssSha = commBadgeMeta?.badgeCssSha,
                        isDarkTheme = isDarkTheme,
                    )
                    commBadgeMeta?.badgeImgSha != null -> {
                        BadgeArtKind.RemoteImage("https://52dmeme.com/api/community-skins/image/${commBadgeMeta.badgeImgSha}")
                    }
                    else -> BadgeArtKind.EmojiGlyph("✦")
                }
                ResolvedBadgeChip(
                    id = "community:$cid",
                    name = commBadgeMeta?.name ?: cid,
                    level = 1,
                    levelText = "",
                    tier = "epic",
                    desc = "社区投稿徽章${commBadgeMeta?.author?.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()}",
                    art = badgeArt,
                    alwaysIconOnly = true,
                    iconWidthDp = badgeSpec?.widthDp ?: 24f,
                    iconHeightDp = badgeSpec?.heightDp ?: 24f,
                    dynamicCssSpec = when {
                        cid == "aurora-preset" -> null
                        commBadgeMeta == null -> "catalog:$cid"
                        badgeSpec == null && !commBadgeMeta.badgeCssSha.isNullOrBlank() -> "$cid:badge:${commBadgeMeta.badgeCssSha}"
                        else -> null
                    },
                )
            }
            badgeMode == "custom" -> buildCustomBadge(bdId)
            growthMeta != null -> {
                val series = growthMeta.series ?: "guess"
                ResolvedBadgeChip(
                    id = series,
                    name = growthMeta.label?.takeIf { it.isNotBlank() } ?: growthMeta.name,
                    level = growthMeta.level,
                    levelText = "Lv${growthMeta.level}",
                    tier = "epic",
                    desc = growthMeta.desc,
                    art = resolveBadgeArt(series, growthMeta.level),
                    alwaysIconOnly = effWhere != "card",
                )
            }
            !ptId.isNullOrBlank() && resolvePersonalTitle(ptId) != null -> {
                val p = resolvePersonalTitle(ptId)!!
                val artKind = if (p.art == "v2") {
                    BadgeArtKind.RemoteImage("https://52dmeme.com/assets/personal-v2/$ptId-badge.webp")
                } else {
                    BadgeArtKind.RemoteSvg("https://52dmeme.com/assets/personal/$ptId.svg", "✦")
                }
                ResolvedBadgeChip(
                    id = "pt:$ptId",
                    name = p.name,
                    level = 1,
                    levelText = if (p.rarity == "SSR") "SSR" else "",
                    tier = if (p.rarity == "SSR") "legend" else "rare",
                    desc = p.mood,
                    art = artKind,
                    alwaysIconOnly = true,
                )
            }
            activeRaffle != null -> {
                val r = activeRaffle.catalog
                val earnedLv = activeRaffle.earnedLevel
                val artKind = when (r.id) {
                    "claude" -> BadgeArtKind.RemoteImage("https://52dmeme.com/shiguang/assets/shiguang-sticker.webp")
                    "oldfarmer" -> BadgeArtKind.RemoteSvg(raffleBadgeUrl("oldfarmer"), "🥕")
                    else -> BadgeArtKind.RemoteImage(raffleBadgeUrl(r.id))
                }
                ResolvedBadgeChip(
                    id = "ft:${r.id}",
                    name = if (r.id == "claude") "拾光司书" else r.name,
                    level = earnedLv,
                    levelText = "Lv$earnedLv",
                    tier = if (r.rarity == "SSR") "legend" else "rare",
                    desc = r.mood,
                    art = artKind,
                    alwaysIconOnly = r.id == "claude" || effWhere != "card",
                    textHex = r.colorHex,
                    levelBgHex = r.colorHex,
                )
            }
            officialMeta != null && OFFICIAL_ART_SVG.containsKey(otId) -> {
                ResolvedBadgeChip(
                    id = "ot:$otId",
                    name = officialMeta.name,
                    level = 5,
                    levelText = "",
                    tier = "legend",
                    desc = "站长授予 · ${officialMeta.mood}",
                    art = BadgeArtKind.InlineOfficialSvg(OFFICIAL_ART_SVG.getValue(otId!!), otId),
                    alwaysIconOnly = true,
                )
            }
            else -> buildCustomBadge(bdId)
        }

        return ResolvedUserIdentity(
            title = resolvedTitle,
            badge = resolvedBadge,
            nickGradientHexes = nickHexes,
        )
    }

    /**
     * Resolves profile showcase items (`badge:<id>` & `official:<id>`, max 6 per `app.js:2266-2274`).
     * Returns `(selectedUpTo6, allAvailable)`.
     */
    fun resolveShowcaseItems(
        badgesJson: JsonElement?,
        officialTitles: List<OfficialTitleMeta> = emptyList(),
        showcaseKeys: List<String>? = null,
    ): Pair<List<ShowcaseItem>, List<ShowcaseItem>> {
        val available = ArrayList<ShowcaseItem>()
        for (b in parseOwnedBadges(badgesJson)) {
            val fullLabel = if (b.levelText.isNotBlank()) "${b.meta.name} ${b.levelText}" else b.meta.name
            available.add(
                ShowcaseItem(
                    key = "badge:${b.id}",
                    label = fullLabel,
                    chip = ResolvedBadgeChip(
                        id = b.id,
                        name = b.meta.name,
                        level = b.level,
                        levelText = if (b.id == "elder") "★" else "Lv${b.level}",
                        tier = b.meta.tier,
                        desc = b.meta.desc,
                        art = b.art,
                        alwaysIconOnly = false,
                    ),
                ),
            )
        }
        val seenOt = HashSet<String>()
        for (t in officialTitles) {
            if (t.id.isBlank() || !seenOt.add(t.id) || !OFFICIAL_ART_SVG.containsKey(t.id)) continue
            val resolvedName = t.name.takeIf { it.isNotBlank() } ?: resolveOfficialTitleName(t.id)?.name ?: t.id
            available.add(
                ShowcaseItem(
                    key = "official:${t.id}",
                    label = resolvedName,
                    chip = ResolvedBadgeChip(
                        id = "ot:${t.id}",
                        name = resolvedName,
                        level = 5,
                        levelText = "",
                        tier = "legend",
                        desc = "站长授予 · ${t.mood}",
                        art = BadgeArtKind.InlineOfficialSvg(OFFICIAL_ART_SVG.getValue(t.id), t.id),
                        alwaysIconOnly = true,
                    ),
                ),
            )
        }
        val selected = if (showcaseKeys != null) {
            val byKey = available.associateBy { it.key }
            showcaseKeys.mapNotNull { byKey[it] }.distinctBy { it.key }.take(6)
        } else {
            available.take(6)
        }
        return selected to available
    }

    /**
     * Parses server `me.deco` into `DecoConfig` (reading real web keys `t, sh, fill, c, c2, bc, fc, bb, bd, badge_mode, pt, ft, ot, gr, rt, community, showcase`
     * with full backward compatibility for local clean preferences).
     */
    fun parseServerDecoConfig(deco: JsonElement?, localCleanRaw: String? = null): DecoConfig {
        val obj = runCatching { deco?.jsonObject }.getOrNull()
        val fcList = runCatching { obj?.get("fc")?.jsonArray }.getOrNull()
            ?.mapNotNull { it.jsonPrimitive.contentOrNull }
            .orEmpty()
        val matchedPreset = when (fcList) {
            listOf("#ff5fa2", "#ffb3d1", "#c86bff") -> "sakura"
            listOf("#00b4d8", "#48cae4", "#90e0ef") -> "salt"
            listOf("#ff6b35", "#f7c548", "#ff3864") -> "sunset"
            listOf("#2d6a4f", "#52b788", "#b7e4c7") -> "pine"
            listOf("#f72585", "#7209b7", "#4cc9f0") -> "cyber"
            listOf("#111111", "#d4a017", "#fff3b0") -> "blackgold"
            else -> obj?.get("nickGradient")?.jsonPrimitive?.contentOrNull.orEmpty()
        }
        val commObj = runCatching { obj?.get("community")?.jsonObject }.getOrNull()
        val showcaseParsed: List<String>? = if (obj != null && obj.containsKey("showcase") && obj["showcase"] !is JsonNull) {
            runCatching { obj["showcase"]?.jsonArray }.getOrNull()
                ?.mapNotNull { it.jsonPrimitive.contentOrNull?.trim()?.takeIf { k -> k.isNotEmpty() } }
                ?.take(6)
                ?: emptyList()
        } else {
            obj?.get("showcaseKeys")?.let { el ->
                if (el is JsonNull) null
                else runCatching { el.jsonArray }.getOrNull()
                    ?.mapNotNull { it.jsonPrimitive.contentOrNull?.trim()?.takeIf { k -> k.isNotEmpty() } }
                    ?.take(6)
            }
        }
        val fromServer = if (obj != null) {
            val rawSc = obj["sc"]?.jsonPrimitive?.contentOrNull
                ?: obj["decoScope"]?.jsonPrimitive?.contentOrNull
                ?: "all"
            val rawBs = obj["bs"]?.jsonPrimitive?.contentOrNull
                ?: obj["badgeScope"]?.jsonPrimitive?.contentOrNull
                ?: "all"
            DecoConfig(
                customTitle = obj["t"]?.jsonPrimitive?.contentOrNull
                    ?: obj["customTitle"]?.jsonPrimitive?.contentOrNull
                    ?: obj["title"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                titleStyle = obj["sh"]?.jsonPrimitive?.contentOrNull
                    ?: obj["titleStyle"]?.jsonPrimitive?.contentOrNull ?: "pill",
                fillMode = obj["fill"]?.jsonPrimitive?.contentOrNull
                    ?: obj["fillMode"]?.jsonPrimitive?.contentOrNull ?: "solid",
                textHex = obj["c"]?.jsonPrimitive?.contentOrNull
                    ?: obj["textHex"]?.jsonPrimitive?.contentOrNull ?: "#FFFFFF",
                primaryHex = obj["c2"]?.jsonPrimitive?.contentOrNull
                    ?: obj["primaryHex"]?.jsonPrimitive?.contentOrNull ?: "#1B7F4B",
                secondaryHex = obj["bc"]?.jsonPrimitive?.contentOrNull
                    ?: obj["secondaryHex"]?.jsonPrimitive?.contentOrNull ?: "#34A853",
                titleFont = obj["f"]?.jsonPrimitive?.contentOrNull
                    ?: obj["titleFont"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                titleEffect = obj["fx"]?.jsonPrimitive?.contentOrNull
                    ?: obj["titleEffect"]?.jsonPrimitive?.contentOrNull ?: "none",
                nickGradient = matchedPreset,
                customGradientStops = fcList,
                bubbleSkin = obj["bb"]?.jsonPrimitive?.contentOrNull
                    ?: obj["bubbleSkin"]?.jsonPrimitive?.contentOrNull ?: "",
                badgeMode = obj["badge_mode"]?.jsonPrimitive?.contentOrNull
                    ?: obj["badgeMode"]?.jsonPrimitive?.contentOrNull ?: "set",
                equippedBadgeId = obj["bd"]?.jsonPrimitive?.contentOrNull
                    ?: obj["equippedBadgeId"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                badgeScope = if (rawBs == "wall") "post" else rawBs,
                decoScope = if (rawSc == "wall") "post" else rawSc,
                equippedPersonalId = obj["pt"]?.jsonPrimitive?.contentOrNull
                    ?: obj["equippedPersonalId"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                equippedRaffleId = obj["ft"]?.jsonPrimitive?.contentOrNull
                    ?: obj["equippedRaffleId"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                equippedRaffleLevel = obj["fn"]?.jsonPrimitive?.intOrNull
                    ?: obj["equippedRaffleLevel"]?.jsonPrimitive?.intOrNull ?: 1,
                equippedRaffleDisplayLevel = obj["fl"]?.jsonPrimitive?.intOrNull
                    ?: obj["equippedRaffleDisplayLevel"]?.jsonPrimitive?.intOrNull ?: 0,
                equippedRafflePermLevel = obj["fp"]?.jsonPrimitive?.intOrNull
                    ?: obj["equippedRafflePermLevel"]?.jsonPrimitive?.intOrNull ?: 0,
                equippedRaffleExpiresAt = obj["fxp"]?.jsonPrimitive?.longOrNull
                    ?: obj["equippedRaffleExpiresAt"]?.jsonPrimitive?.longOrNull ?: 0L,
                cgSize = obj["cg_size"]?.jsonPrimitive?.intOrNull
                    ?: obj["cgSize"]?.jsonPrimitive?.intOrNull ?: 0,
                cgRandom = obj["cg_random"]?.jsonPrimitive?.booleanOrNull
                    ?: obj["cgRandom"]?.jsonPrimitive?.booleanOrNull ?: false,
                cgMotion = obj["cg_motion"]?.jsonPrimitive?.booleanOrNull
                    ?: obj["cgMotion"]?.jsonPrimitive?.booleanOrNull ?: true,
                cgFire = obj["cg_fire"]?.jsonPrimitive?.booleanOrNull
                    ?: obj["cgFire"]?.jsonPrimitive?.booleanOrNull ?: true,
                equippedOfficialId = obj["ot"]?.jsonPrimitive?.contentOrNull
                    ?: obj["equippedOfficialId"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                equippedGrowthId = obj["gr"]?.jsonPrimitive?.contentOrNull
                    ?: obj["equippedGrowthId"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                equippedInviteTitle = obj["rt"]?.jsonPrimitive?.contentOrNull == "guangjie" ||
                    obj["equippedInviteTitle"]?.jsonPrimitive?.booleanOrNull == true,
                communityTitleId = commObj?.get("title")?.jsonPrimitive?.contentOrNull.orEmpty(),
                communityBadgeId = commObj?.get("badge")?.jsonPrimitive?.contentOrNull.orEmpty(),
                communityBubbleId = commObj?.get("bubble")?.jsonPrimitive?.contentOrNull.orEmpty(),
                communityReplyId = commObj?.get("reply")?.jsonPrimitive?.contentOrNull
                    ?: obj["communityReplyId"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                showcaseKeys = showcaseParsed,
                cleanHideOthersNick = obj["cleanHideOthersNick"]?.jsonPrimitive?.booleanOrNull ?: false,
                cleanHideOthersBadges = obj["cleanHideOthersBadges"]?.jsonPrimitive?.booleanOrNull ?: false,
                cleanHideOthersStyles = obj["cleanHideOthersStyles"]?.jsonPrimitive?.booleanOrNull ?: false,
                cleanHideBubbles = obj["cleanHideBubbles"]?.jsonPrimitive?.booleanOrNull ?: false,
                cleanHideReplyFrames = obj["cleanHideReplyFrames"]?.jsonPrimitive?.booleanOrNull ?: false,
                cleanPauseOrnaments = obj["cleanPauseOrnaments"]?.jsonPrimitive?.booleanOrNull ?: false,
                blockedCosmeticKeys = emptySet(),
                blockedKeyHideFullSet = obj["blockedKeyHideFullSet"]?.jsonPrimitive?.booleanOrNull ?: true,
            )
        } else {
            DecoConfig()
        }
        if (localCleanRaw.isNullOrBlank()) return fromServer
        val localCfg = runCatching { ForHappyJson.decodeFromString<DecoConfig>(localCleanRaw) }.getOrNull() ?: return fromServer
        return fromServer.copy(
            cleanHideOthersNick = localCfg.cleanHideOthersNick,
            cleanHideOthersBadges = localCfg.cleanHideOthersBadges,
            cleanHideOthersStyles = localCfg.cleanHideOthersStyles,
            cleanHideBubbles = localCfg.cleanHideBubbles,
            cleanHideReplyFrames = localCfg.cleanHideReplyFrames,
            cleanPauseOrnaments = localCfg.cleanPauseOrnaments,
            blockedCosmeticKeys = localCfg.blockedCosmeticKeys,
            blockedKeyHideFullSet = localCfg.blockedKeyHideFullSet,
        )
    }

    /**
     * Builds the exact server `deco` JSON payload expected by `PATCH /api/me/profile`,
     * preserving unknown server fields (`fp`, `fxp`, `cg_size`, `showcase`, `csst`, `cssb`).
     */
    fun buildServerDecoPayload(
        originalDeco: JsonElement?,
        config: DecoConfig,
        ownedBadges: List<OwnedBadgeItem> = emptyList(),
    ): JsonObject {
        val prev = runCatching { originalDeco?.jsonObject }.getOrNull()
        val map = LinkedHashMap<String, JsonElement>()
        prev?.forEach { (k, v) ->
            // Strip legacy Android-only keys so server stays clean
            if (k !in setOf(
                    "customTitle", "titleStyle", "fillMode", "primaryHex", "secondaryHex", "textHex",
                    "titleFont", "titleEffect", "nickGradient", "bubbleSkin", "badgeMode",
                    "equippedBadgeId", "badgeScope", "decoScope", "equippedPersonalId",
                    "equippedRaffleId", "equippedRaffleLevel", "equippedRaffleDisplayLevel",
                    "equippedRafflePermLevel", "equippedRaffleExpiresAt",
                    "cgSize", "cgRandom", "cgMotion", "cgFire",
                    "equippedOfficialId", "equippedGrowthId", "equippedInviteTitle",
                    "communityTitleId", "communityBadgeId", "communityBubbleId", "communityReplyId",
                    "showcaseKeys", "cleanHideOthersNick", "cleanHideOthersBadges",
                    "cleanHideOthersStyles", "cleanHideBubbles",
                    "cleanHideReplyFrames", "cleanPauseOrnaments",
                    "blockedCosmeticKeys", "blockedKeyHideFullSet",
                    "customGradientStops",
                ) && v !is JsonNull
            ) {
                map[k] = v
            }
        }

        map["t"] = JsonPrimitive(config.customTitle.trim().take(8))
        map["sh"] = JsonPrimitive(config.titleStyle.ifBlank { "pill" })
        map["fill"] = JsonPrimitive(config.fillMode.ifBlank { "solid" })
        map["c"] = JsonPrimitive(config.textHex.ifBlank { "#ffffff" })
        map["c2"] = JsonPrimitive(config.primaryHex.ifBlank { "#16a34a" })
        map["bc"] = JsonPrimitive(config.secondaryHex.ifBlank { config.primaryHex.ifBlank { "#16a34a" } })
        val normSc = config.decoScope.ifBlank { "all" }.let { if (it == "wall") "post" else it }
        val normBs = config.badgeScope.ifBlank { "all" }.let { if (it == "wall") "post" else it }
        map["sc"] = JsonPrimitive(normSc)
        map["bs"] = JsonPrimitive(normBs)
        map["badge_mode"] = JsonPrimitive(config.badgeMode.ifBlank { "set" })
        if (config.titleFont.isNotBlank()) map["f"] = JsonPrimitive(config.titleFont) else map.remove("f")
        if (config.titleEffect.isNotBlank() && config.titleEffect != "none") {
            map["fx"] = JsonPrimitive(config.titleEffect)
        } else {
            map.remove("fx")
        }

        val resolvedStops = when (config.nickGradient) {
            "sakura" -> listOf("#ff5fa2", "#ffb3d1", "#c86bff")
            "salt" -> listOf("#00b4d8", "#48cae4", "#90e0ef")
            "sunset" -> listOf("#ff6b35", "#f7c548", "#ff3864")
            "pine" -> listOf("#2d6a4f", "#52b788", "#b7e4c7")
            "cyber" -> listOf("#f72585", "#7209b7", "#4cc9f0")
            "blackgold", "gold" -> listOf("#111111", "#d4a017", "#fff3b0")
            "aurora" -> listOf("#06B6D4", "#8B5CF6", "#EC4899")
            "" -> emptyList()
            else -> config.customGradientStops
        }
        if (resolvedStops.isNotEmpty()) {
            map["fc"] = JsonArray(resolvedStops.map { JsonPrimitive(it) })
        } else {
            map.remove("fc")
        }

        val bb = config.bubbleSkin.takeIf { it.isNotBlank() && it != "default" }
        if (bb != null) map["bb"] = JsonPrimitive(bb) else map.remove("bb")

        if (config.equippedBadgeId.isNotBlank()) {
            map["bd"] = JsonPrimitive(config.equippedBadgeId)
            val n = ownedBadges.firstOrNull { it.id == config.equippedBadgeId }?.count ?: 1
            map["bn"] = JsonPrimitive(n)
        } else {
            map.remove("bd")
            map.remove("bn")
            map.remove("bl")
        }

        // Exclusive outfit slot (`pt` / `ft` / `ot` / `gr` / `rt`)
        when {
            config.equippedPersonalId.isNotBlank() -> {
                map["pt"] = JsonPrimitive(config.equippedPersonalId)
                listOf("ft", "fn", "fl", "fp", "fxp", "ot", "gr", "rt").forEach { map.remove(it) }
            }
            config.equippedRaffleId.isNotBlank() -> {
                val baseLv = config.equippedRaffleLevel.coerceAtLeast(1)
                map["ft"] = JsonPrimitive(config.equippedRaffleId)
                map["fn"] = JsonPrimitive(baseLv)
                if (config.equippedRaffleDisplayLevel > 0) {
                    map["fl"] = JsonPrimitive(config.equippedRaffleDisplayLevel.coerceIn(1, baseLv))
                } else {
                    map.remove("fl")
                }
                if (config.equippedRaffleId == "chickengun") {
                    if (config.cgSize > 0) map["cg_size"] = JsonPrimitive(config.cgSize) else map.remove("cg_size")
                    map["cg_random"] = JsonPrimitive(config.cgRandom)
                    map["cg_motion"] = JsonPrimitive(config.cgMotion)
                    map["cg_fire"] = JsonPrimitive(config.cgFire)
                }
                listOf("pt", "ot", "gr", "rt").forEach { map.remove(it) }
            }
            config.equippedOfficialId.isNotBlank() -> {
                map["ot"] = JsonPrimitive(config.equippedOfficialId)
                listOf("pt", "ft", "fn", "fl", "fp", "fxp", "gr", "rt").forEach { map.remove(it) }
            }
            config.equippedGrowthId.isNotBlank() -> {
                map["gr"] = JsonPrimitive(config.equippedGrowthId)
                listOf("pt", "ft", "fn", "fl", "fp", "fxp", "ot", "rt").forEach { map.remove(it) }
            }
            config.equippedInviteTitle -> {
                map["rt"] = JsonPrimitive("guangjie")
                listOf("pt", "ft", "fn", "fl", "fp", "fxp", "ot", "gr").forEach { map.remove(it) }
            }
            else -> {
                listOf("pt", "ft", "fn", "fl", "fp", "fxp", "ot", "gr", "rt").forEach { map.remove(it) }
            }
        }

        val prevComm = runCatching { prev?.get("community")?.jsonObject }.getOrNull()
        val commMap = LinkedHashMap<String, JsonElement>()
        prevComm?.forEach { (k, v) -> commMap[k] = v }
        if (config.communityTitleId.isNotBlank()) commMap["title"] = JsonPrimitive(config.communityTitleId) else commMap.remove("title")
        if (config.communityBadgeId.isNotBlank()) commMap["badge"] = JsonPrimitive(config.communityBadgeId) else commMap.remove("badge")
        if (config.communityBubbleId.isNotBlank()) commMap["bubble"] = JsonPrimitive(config.communityBubbleId) else commMap.remove("bubble")
        if (config.communityReplyId.isNotBlank()) commMap["reply"] = JsonPrimitive(config.communityReplyId) else commMap.remove("reply")
        if (commMap.isNotEmpty()) {
            map["community"] = JsonObject(commMap)
        } else {
            map.remove("community")
        }

        if (config.showcaseKeys != null) {
            map["showcase"] = JsonArray(config.showcaseKeys.distinct().take(6).map { JsonPrimitive(it) })
        } else {
            map.remove("showcase")
        }

        return JsonObject(map)
    }

    fun canonicalCosmeticKey(rawKey: String?, isBadgeId: Boolean = false): String? {
        val trimmed = rawKey?.trim().orEmpty()
        if (trimmed.isEmpty() || trimmed == "default") return null
        if (trimmed == "custom") return "custom"
        if (trimmed == "preset:yinlu" || trimmed == "reward:guangjie") return "reward:guangjie"
        if (trimmed.startsWith("pt:")) {
            val id = trimmed.removePrefix("pt:")
            return when (id) {
                "owner52" -> "personal:owner-52di"
                "owner52b" -> "personal:owner-52b"
                else -> "personal:$id"
            }
        }
        if (trimmed == "personal:owner52") return "personal:owner-52di"
        if (trimmed == "personal:owner52b") return "personal:owner-52b"
        if (trimmed.startsWith("ft:")) return "raffle:${trimmed.removePrefix("ft:")}"
        if (trimmed.startsWith("ot:")) return "official:${trimmed.removePrefix("ot:")}"
        if (trimmed.startsWith("community:") ||
            trimmed.startsWith("personal:") ||
            trimmed.startsWith("raffle:") ||
            trimmed.startsWith("official:") ||
            trimmed.startsWith("preset:") ||
            trimmed.startsWith("reward:") ||
            trimmed.startsWith("badge:")
        ) {
            return trimmed
        }
        if (trimmed.startsWith("growth:")) {
            val id = trimmed.removePrefix("growth:")
            if (id == "rank" || id == "activity" || id in BUILT_IN_BADGES || dynamicGrowthBySeries.containsKey(id)) {
                return "growth:$id"
            }
            val resolvedSeries = resolveGrowthRewardMeta(id)?.series?.takeIf { it.isNotBlank() }
            val fallbackSeries = when {
                id.startsWith("luck_king") -> "luck_king"
                id.startsWith("luck_last") -> "luck_last"
                id.startsWith("rank_") -> id.substringBeforeLast('_')
                Regex("""^.+[_:]\d+$""").matches(id) -> id.replace(Regex("""[_:]\d+$"""), "")
                else -> id
            }
            return "growth:${resolvedSeries ?: fallbackSeries.ifBlank { "guess" }}"
        }
        Regex("""^([a-z0-9_]+):(\d+)$""").matchEntire(trimmed)?.let { m ->
            return "growth:${m.groupValues[1]}"
        }
        if (isBadgeId) {
            return if (
                trimmed == "guess" ||
                trimmed.startsWith("luck_") ||
                trimmed.startsWith("rank_") ||
                trimmed in GROWTH_V5_IDS ||
                dynamicGrowthBySeries.containsKey(trimmed)
            ) {
                "growth:$trimmed"
            } else {
                "badge:$trimmed"
            }
        }
        return trimmed
    }

    fun isCosmeticBlocked(
        rawKey: String?,
        blockedKeys: Set<String>,
        isBadgeId: Boolean = false,
    ): Boolean {
        if (rawKey.isNullOrBlank() || blockedKeys.isEmpty()) return false
        if (rawKey in blockedKeys) return true
        val canon = canonicalCosmeticKey(rawKey, isBadgeId) ?: return false
        if (canon in blockedKeys) return true
        if (canon.startsWith("growth:")) {
            val series = canon.removePrefix("growth:")
            if (series.startsWith("rank_") && "growth:rank" in blockedKeys) return true
            if (series !in setOf("guess", "luck_king", "luck_last") &&
                !series.startsWith("rank_") &&
                "growth:activity" in blockedKeys
            ) {
                return true
            }
        }
        return false
    }

    fun describeCosmeticKey(canonKey: String): SelectableCosmeticEntry {
        val key = canonicalCosmeticKey(canonKey) ?: canonKey
        return when {
            key.startsWith("community:") -> {
                val id = key.removePrefix("community:")
                val skin = resolveCommunitySkinMeta(id)
                SelectableCosmeticEntry(
                    key = key,
                    name = skin?.name?.ifBlank { id } ?: id,
                    category = "开源社区装扮",
                    subtitle = skin?.author?.takeIf { it.isNotBlank() }?.let { "作者 · $it" } ?: "社区投稿",
                )
            }
            key.startsWith("raffle:") -> {
                val id = key.removePrefix("raffle:")
                val r = resolveRaffleTitle(id)
                SelectableCosmeticEntry(
                    key = key,
                    name = r?.name ?: id,
                    category = "抽奖角色套装",
                    subtitle = r?.let { "${it.rarity} · ${it.mood}" } ?: "抽奖套装",
                )
            }
            key.startsWith("personal:") -> {
                val id = key.removePrefix("personal:")
                val p = resolvePersonalTitle(id)
                SelectableCosmeticEntry(
                    key = key,
                    name = p?.name ?: id,
                    category = "群友专属定制",
                    subtitle = p?.let { "${it.rarity} · ${it.mood}" } ?: "专属定制",
                )
            }
            key.startsWith("official:") -> {
                val id = key.removePrefix("official:")
                val o = resolveOfficialTitleName(id)
                SelectableCosmeticEntry(
                    key = key,
                    name = o?.name ?: id,
                    category = "官方与精选气泡",
                    subtitle = o?.mood?.takeIf { it.isNotBlank() }?.let { "官方称号 · $it" } ?: "官方授予",
                )
            }
            key == "reward:guangjie" -> SelectableCosmeticEntry(
                key = key,
                name = "广结善缘",
                category = "官方与精选气泡",
                subtitle = "邀请奖励专属装扮",
            )
            key.startsWith("preset:") -> {
                val id = key.removePrefix("preset:")
                val label = BUBBLE_PRESETS.firstOrNull { it.first == id }?.second ?: id
                SelectableCosmeticEntry(
                    key = key,
                    name = label,
                    category = "官方与精选气泡",
                    subtitle = "预设气泡皮肤",
                )
            }
            key == "custom" -> SelectableCosmeticEntry(
                key = key,
                name = "自定义 CSS / 文本装扮",
                category = "官方与精选气泡",
                subtitle = "个人自定义样式或气泡 CSS",
            )
            key.startsWith("growth:") -> {
                val series = key.removePrefix("growth:")
                val (label, sub) = when (series) {
                    "guess" -> "猜词成长奖励" to "猜词累计解锁五阶称号与气泡"
                    "luck_king" -> "手气王成长奖励" to "拼手气红包最佳成长装扮"
                    "luck_last" -> "手气亡成长奖励" to "拼手气红包垫底成长装扮"
                    "rank" -> "日榜 / 总榜荣誉装扮" to "每日/总榜前三结算荣誉"
                    "activity" -> "农牧 / 钓鱼 / 集卡奖励" to "各项活跃度成长系列装扮"
                    else -> {
                        val badgeName = resolveBadgeMeta(series)?.name ?: series
                        badgeName to "成长系列装扮"
                    }
                }
                SelectableCosmeticEntry(
                    key = key,
                    name = label,
                    category = "成长与排行榜奖励",
                    subtitle = sub,
                )
            }
            key.startsWith("badge:") -> {
                val id = key.removePrefix("badge:")
                val b = resolveBadgeMeta(id)
                SelectableCosmeticEntry(
                    key = key,
                    name = b?.name ?: id,
                    category = "常规成就徽章",
                    subtitle = b?.desc ?: "成就徽章",
                )
            }
            else -> SelectableCosmeticEntry(key = key, name = key, category = "其他装扮")
        }
    }

    fun buildSelectableCosmeticCatalog(): List<SelectableCosmeticEntry> {
        val out = LinkedHashMap<String, SelectableCosmeticEntry>()
        fun add(entry: SelectableCosmeticEntry) {
            if (!out.containsKey(entry.key)) out[entry.key] = entry
        }

        allCommunitySkins().values.forEach { skin ->
            val parts = buildList {
                if (skin.hasBubble) add("气泡")
                if (skin.hasTitle) add("称号")
                if (skin.hasBadge) add("徽章")
                if (skin.hasReply) add("回应框")
            }.joinToString("/")
            add(
                SelectableCosmeticEntry(
                    key = "community:${skin.id}",
                    name = skin.name.ifBlank { skin.id },
                    category = "开源社区装扮",
                    subtitle = "${skin.author}${if (parts.isNotBlank()) " · $parts" else ""}",
                ),
            )
        }

        allRaffleTitles().values.forEach { r ->
            add(
                SelectableCosmeticEntry(
                    key = "raffle:${r.id}",
                    name = r.name,
                    category = "抽奖角色套装",
                    subtitle = "${r.rarity} · ${r.mood}",
                ),
            )
        }

        allPersonalTitles().values.forEach { p ->
            if (p.id == "owner52" || p.id == "owner52b") return@forEach
            add(
                SelectableCosmeticEntry(
                    key = "personal:${p.id}",
                    name = p.name,
                    category = "群友专属定制",
                    subtitle = "${p.rarity} · ${p.mood}",
                ),
            )
        }

        val officialIds = LinkedHashSet<String>().apply {
            addAll(BUILT_IN_OFFICIAL_NAMES.keys)
            addAll(dynamicOfficialTitles.keys)
        }
        officialIds.forEach { id ->
            val meta = resolveOfficialTitleName(id)
            add(
                SelectableCosmeticEntry(
                    key = "official:$id",
                    name = meta?.name ?: id,
                    category = "官方与精选气泡",
                    subtitle = meta?.mood?.takeIf { it.isNotBlank() } ?: "官方称号",
                ),
            )
        }
        add(describeCosmeticKey("reward:guangjie"))
        BUBBLE_PRESETS.forEach { (id, label) ->
            if (id.isNotBlank() && id != "default") {
                add(
                    SelectableCosmeticEntry(
                        key = "preset:$id",
                        name = label,
                        category = "官方与精选气泡",
                        subtitle = "官方/精选预设气泡",
                    ),
                )
            }
        }
        add(describeCosmeticKey("custom"))

        listOf("growth:guess", "growth:luck_king", "growth:luck_last", "growth:rank", "growth:activity").forEach {
            add(describeCosmeticKey(it))
        }

        BUILT_IN_BADGES.values.forEach { b ->
            if (b.id != "guess" && !b.id.startsWith("luck_") && !b.id.startsWith("rank_") && b.id !in GROWTH_V5_IDS) {
                add(
                    SelectableCosmeticEntry(
                        key = "badge:${b.id}",
                        name = b.name,
                        category = "常规成就徽章",
                        subtitle = b.desc,
                    ),
                )
            }
        }

        return out.values.toList()
    }

    fun extractMessageCosmeticEntries(
        msg: ChatMessage,
        isDarkTheme: Boolean = false,
    ): List<SelectableCosmeticEntry> {
        val byKey = LinkedHashMap<String, SelectableCosmeticEntry>()
        fun addRaw(rawKey: String?, isBadgeId: Boolean = false) {
            val canon = canonicalCosmeticKey(rawKey, isBadgeId) ?: return
            if (!byKey.containsKey(canon)) {
                byKey[canon] = describeCosmeticKey(canon)
            }
        }
        val user = msg.user
        if (user != null) {
            val id = resolveEquippedIdentity(
                decoJson = user.deco,
                badgesJson = user.badges,
                officialTitles = user.officialTitles,
                where = "chat",
                isAdmin = user.isAdmin,
                hasFancyPermission = user.isFancyGranted,
                cosmeticAccess = user.cosmeticAccess,
                isDarkTheme = isDarkTheme,
            )
            addRaw(id.title?.key)
            addRaw(id.badge?.id, isBadgeId = true)
            val bubble = ChatBubbleCosmetics.resolveChatBubbleStyle(
                user = user,
                messageId = msg.id.toString(),
                isDarkTheme = isDarkTheme,
            )
            addRaw(bubble?.key)
        }
        msg.boosts.forEach { boost ->
            val frame = ChatBubbleCosmetics.resolveBoostFrameStyle(
                boost = boost,
                isDarkTheme = isDarkTheme,
            )
            addRaw(frame?.key)
        }
        return byKey.values.toList()
    }

    fun extractActiveChatCosmetics(
        messages: List<ChatMessage>,
        isDarkTheme: Boolean = false,
    ): List<SelectableCosmeticEntry> {
        val byKey = LinkedHashMap<String, SelectableCosmeticEntry>()
        for (i in messages.indices.reversed()) {
            val msg = messages[i]
            val entries = extractMessageCosmeticEntries(msg, isDarkTheme)
            for (entry in entries) {
                if (!byKey.containsKey(entry.key)) {
                    byKey[entry.key] = entry.copy(subtitle = "本群出现 · @${msg.displayAuthor}")
                }
            }
        }
        return byKey.values.toList()
    }
}
