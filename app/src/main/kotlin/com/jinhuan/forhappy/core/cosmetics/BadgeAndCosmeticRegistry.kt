package com.jinhuan.forhappy.core.cosmetics

import com.jinhuan.forhappy.core.clock.ServerClock
import com.jinhuan.forhappy.core.model.DecoConfig
import com.jinhuan.forhappy.core.model.FarmBadgeMeta
import com.jinhuan.forhappy.core.model.ForHappyJson
import com.jinhuan.forhappy.core.model.GrowthRewardMeta
import com.jinhuan.forhappy.core.model.MeResponse
import com.jinhuan.forhappy.core.model.OfficialTitleMeta
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

sealed interface BadgeArtKind {
    data class BundledSvg(val assetPath: String) : BadgeArtKind
    data class AtlasV4(
        val tileIndex: Int,
        val style: String,
        val isHonor: Boolean = false,
        val isDay: Boolean = false,
    ) : BadgeArtKind
    data class InlineOfficialSvg(val innerSvg: String) : BadgeArtKind
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

data class RaffleTitleCatalogItem(
    val id: String,
    val name: String,
    val mood: String,
    val colorHex: String,
    val accentHex: String,
    val rarity: String,
)

data class PersonalTitleCatalogItem(
    val id: String,
    val name: String,
    val mood: String,
    val rarity: String,
    val art: String,
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
        "claude" to RaffleTitleCatalogItem("claude", "Claude花姬", "橘花盛开的温暖时刻", "#b9824e", "#efcfa1", "SSR"),
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
            bubbleCssSha = "c3274be65de19dc7dc4fbbffd378225d03792333ae747559675d1f0abe903a65",
            titleCssSha = "12667bafacb0d837d7a43b50cfbcc9b296b326221b05b1da6078100e553d2067",
            badgeImgSha = "19b1612f344bacb77ff295e1015b806caf76b8c71d573f4d6ab572e20b3ede2d",
            badgeCssSha = "94246d3faec40ef12c0149002c8399d03334719b91f739e3c7b7b24288f1e83c",
        ),
        "laowu-dispatch-black" to CommunitySkinCatalogItem(
            id = "laowu-dispatch-black", name = "老吴派遣·黑", author = "Earthcat",
            hasTitle = true, hasBadge = false, hasBubble = true,
            bubbleCssSha = "124cbd46081146004ff36715429e5b03dab38fad884fe3f04ee525755d96316c",
            titleCssSha = "8cd9a254d97135f971c3ea3ec057ff5daf413d3a3f6f3f02b6dd358f131604b3",
        ),
        "laowu-dispatch-calico" to CommunitySkinCatalogItem(
            id = "laowu-dispatch-calico", name = "老吴派遣·三花", author = "Earthcat",
            hasTitle = true, hasBadge = false, hasBubble = true,
            bubbleCssSha = "18c36ff1d9e2b4b6128e94c4015e663f0c48308c302bf704902735faab1aa0b6",
            titleCssSha = "936a072bc83ff4af6a10f51574152e872f00aa72d81a7429f1fd5aa42d1a925a",
        ),
        "laowu-dispatch-gray" to CommunitySkinCatalogItem(
            id = "laowu-dispatch-gray", name = "老吴派遣·灰", author = "Earthcat",
            hasTitle = true, hasBadge = false, hasBubble = true,
            bubbleCssSha = "32ca8af9fd0dbe6576e4e3df7fbf38ffb1621abf9a6f28facf9763497a62d02c",
            titleCssSha = "8598f77b56ad49ce8023f3c7b91d739691faebc6ffc8a917c9a39e9e27c3f488",
        ),
        "laowu-dispatch-siamese" to CommunitySkinCatalogItem(
            id = "laowu-dispatch-siamese", name = "老吴派遣·暹罗", author = "Earthcat",
            hasTitle = true, hasBadge = false, hasBubble = true,
            bubbleCssSha = "832ed7919d46f2c984116cd0125284db50246b525a01560addb9b9541c168429",
            titleCssSha = "2f5b6dbdbf7d14d7e7db3b12ca8957b8990a56e66fd7643d23b5ff4890bf6a62",
        ),
        "laowu-dispatch-white" to CommunitySkinCatalogItem(
            id = "laowu-dispatch-white", name = "老吴派遣·白", author = "Earthcat",
            hasTitle = true, hasBadge = false, hasBubble = true,
            bubbleCssSha = "38500ed04d5a2541e9de75951627ddd3204488245d1d338112fad40a816b5623",
            titleCssSha = "aed3e69262739d914ec1ca2a1dce4631687df1878dd98cc41ebff18eccda4925",
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

    val LANTING_INK_BADGE_SVG: String = """
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64">
          <defs>
            <linearGradient id="ik" x1="0" y1="0" x2="1" y2="1">
              <stop offset="0%" stop-color="#111827"/>
              <stop offset="55%" stop-color="#374151"/>
              <stop offset="100%" stop-color="#b48327"/>
            </linearGradient>
          </defs>
          <circle cx="32" cy="32" r="27" fill="#f7f3e8" stroke="url(#ik)" stroke-width="4.5" stroke-linecap="round"/>
          <circle cx="32" cy="32" r="22" fill="none" stroke="#b48327" stroke-width="1.2"/>
          <circle cx="24" cy="22" r="8" fill="#b91c1c" opacity="0.92"/>
          <path d="M10 41 L22 26 L34 38 L44 23 L54 40 Q32 54 10 41 Z" fill="#374151" opacity="0.65"/>
          <path d="M14 45 L31 29 L45 41 L53 33 Q33 56 14 45 Z" fill="#111827" opacity="0.92"/>
          <path d="M16 43 Q32 39 48 43" fill="none" stroke="#fef3c7" stroke-width="2" stroke-linecap="round" opacity="0.85"/>
          <circle cx="44" cy="19" r="3" fill="#dc2626"/>
          <circle cx="49" cy="23" r="2.2" fill="#fde68a"/>
        </svg>
    """.trimIndent()

    val ORANGE_FLICK_BADGE_SVG: String = """
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64"><defs><linearGradient id="ginger" x1="0" y1="0" x2=".25" y2="1"><stop stop-color="#ffc575"/><stop offset="1" stop-color="#f3a04b"/></linearGradient><linearGradient id="wafer" x1="0" y1="0" x2="0" y2="1"><stop stop-color="#f4d0a0"/><stop offset="1" stop-color="#d9a16a"/></linearGradient></defs><path d="M18 34.2H52L47.2 58.1Q46.8 60.8 43.9 61H26Q23.3 61 22.8 58.5Z" fill="url(#wafer)" stroke="#965737" stroke-width="1.7" stroke-linejoin="round"/><path d="M22 43.5 39.1 59M34 43.5 46.8 54.8M45.9 43.5 49 46.3M22.2 50.5 30.1 43.5M24.8 57.3 40.6 43.5M35.2 59.1 48.4 47.5" fill="none" stroke="#9d6239" stroke-width="1.25" stroke-linecap="round" opacity=".92"/><path d="M12.8 30.5Q12.2 28.5 15 28.5H51.8Q55.2 28.5 54.3 32.2L52.7 39.6Q52 42 49.1 42H18.5Q15.1 42 14.2 38.7Z" fill="url(#wafer)" stroke="#965737" stroke-width="1.7" stroke-linejoin="round"/><path d="M18.2 32.1 27.7 40.1M28.8 32.4 37.8 40.1M39.6 32.4 48.5 39.9M19.5 40 28.6 32.4M30 40 39.2 32.4M40.5 40 49.7 32.4" fill="none" stroke="#9d6239" stroke-width="1.25" stroke-linecap="round" opacity=".92"/><path d="M4.3 28.5Q3 22.4 6.9 18.8C10.1 15.5 14.1 13 19 10.9L19.6 5.6Q20.1 1.9 22.6 4.3Q24 5.5 24.6 9C31.8 6.5 39 6.9 45.6 9.1Q46.9 3.8 49.7 5.2Q52 6.4 51.5 11.7C56.9 15.4 59.1 21.5 59.2 29.9L59.4 41.9Q59.4 47.1 56 47.2Q53.3 47.2 53.3 43.1V33.2Q53.2 31.2 50.8 31.2H24.6Q22.8 31.2 21.2 33L18.1 36.3Q17 37.4 15.8 36.2L11.1 31.8Q9.7 30.4 8.3 32.2L6.9 33.8Q5.5 35.2 4.8 33.2Z" fill="url(#ginger)" stroke="#965737" stroke-width="1.65" stroke-linejoin="round"/><path d="M5.5 28.7Q7.2 25.4 12.5 25.1Q18 24.9 22.5 29.8Q21.1 31.3 17.1 35.9L11.3 31.8Q9.7 30.2 8.2 32.2L6.6 33.8Q5.9 34.1 5.5 28.7Z" fill="#fff0d3"/><path d="M20.9 8.9 21.5 5.9 23.1 8.1M47.5 8.4 49.2 6.9 49.9 10.3" fill="#f4a3a0" stroke-linejoin="round"/><path d="M26.5 9.2Q28.5 11.6 28.5 14.3M33 8.1Q34.5 10.7 34.8 14.1M39.3 8.8Q40.8 11.1 41.3 13.8M54.6 25.6Q56.8 26.2 58.4 25.6M54.4 37Q56.5 37.8 58.8 37.1" fill="none" stroke="#b86731" stroke-width="1.8" stroke-linecap="round"/><path d="M25 20.6Q25.6 15.1 27.9 15.6Q30.3 15.8 30.5 21.5M43.9 22.2Q47.3 18.5 47.3 14.9Q47.5 12.3 49.4 12.8Q51.4 13.2 51.9 18.2" fill="none" stroke="#965737" stroke-width="1.3" stroke-linecap="round" opacity=".86"/><circle cx="7.9" cy="25.5" r="1.4" fill="#512d24"/><circle cx="18.6" cy="27.2" r="1.5" fill="#512d24"/><path d="M11.5 24.8 14.2 25.4 12.6 27Z" fill="#512d24"/><path d="M21 28.8Q22.3 28.9 23.2 29.5" fill="none" stroke="#e58b86" stroke-width="1.5" stroke-linecap="round"/></svg>
    """.trimIndent()

    private val _catalogRevision = MutableStateFlow(0)
    val catalogRevision: StateFlow<Int> = _catalogRevision.asStateFlow()

    private val dynamicBadges = ConcurrentHashMap<String, BadgeCatalogEntry>()
    private val dynamicGrowthById = ConcurrentHashMap<String, GrowthRewardMeta>()
    private val dynamicGrowthBySeries = ConcurrentHashMap<String, List<GrowthRewardMeta>>()
    private val dynamicOfficialTitles = ConcurrentHashMap<String, OfficialTitleMeta>()
    private val dynamicCommunitySkins = ConcurrentHashMap<String, CommunitySkinCatalogItem>()

    fun resolveCommunitySkinMeta(id: String): CommunitySkinCatalogItem? =
        dynamicCommunitySkins[id] ?: COMMUNITY_SKINS[id]

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
        val cat = RAFFLE_TITLES[ftId] ?: return null
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

        val sc = obj["sc"]?.jsonPrimitive?.contentOrNull ?: obj["decoScope"]?.jsonPrimitive?.contentOrNull ?: "all"
        if (where != "card" && sc != "all" && sc != where) {
            return ResolvedUserIdentity(nickGradientHexes = nickHexes)
        }

        val communityObj = runCatching { obj["community"]?.jsonObject }.getOrNull()
        val communityTitleId = communityObj?.get("title")?.jsonPrimitive?.contentOrNull
            ?: obj["communityTitleId"]?.jsonPrimitive?.contentOrNull
        val communityBadgeId = communityObj?.get("badge")?.jsonPrimitive?.contentOrNull
            ?: obj["communityBadgeId"]?.jsonPrimitive?.contentOrNull

        val ptId = obj["pt"]?.jsonPrimitive?.contentOrNull ?: obj["equippedPersonalId"]?.jsonPrimitive?.contentOrNull
        val otId = obj["ot"]?.jsonPrimitive?.contentOrNull ?: obj["equippedOfficialId"]?.jsonPrimitive?.contentOrNull
        val grId = obj["gr"]?.jsonPrimitive?.contentOrNull ?: obj["equippedGrowthId"]?.jsonPrimitive?.contentOrNull
        val rtId = obj["rt"]?.jsonPrimitive?.contentOrNull
            ?: if (obj["equippedInviteTitle"]?.jsonPrimitive?.booleanOrNull == true) "guangjie" else null
        val customText = (obj["t"]?.jsonPrimitive?.contentOrNull
            ?: obj["customTitle"]?.jsonPrimitive?.contentOrNull
            ?: obj["title"]?.jsonPrimitive?.contentOrNull)
            ?.trim()
            .orEmpty()

        val commTitleMeta = communityTitleId?.takeIf { it.isNotBlank() }?.let { resolveCommunitySkinMeta(it) }
        val commBadgeMeta = communityBadgeId?.takeIf { it.isNotBlank() }?.let { resolveCommunitySkinMeta(it) }
        val growthMeta = grId?.takeIf { it.isNotBlank() }?.let { resolveGrowthRewardMeta(it) }
        val activeRaffle = resolveActiveRaffle(obj, cosmeticAccess, nowSec)
        val officialMeta = otId?.takeIf { it.isNotBlank() }?.let { resolveOfficialTitleName(it, officialTitles) }

        // 2. Title resolution (`titleEl` priority order)
        val resolvedTitle: ResolvedTitleChip? = when {
            isOwner52Suit(ptId) -> {
                val suitId = ptId!!
                val p = PERSONAL_TITLES[suitId] ?: PersonalTitleCatalogItem(suitId, "52帝", "站长专属", "SSR", "owner")
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
            commTitleMeta != null -> {
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
            !ptId.isNullOrBlank() && PERSONAL_TITLES[ptId] != null -> {
                val p = PERSONAL_TITLES[ptId]!!
                if (p.art == "v2") {
                    val isSsr = p.rarity.equals("SSR", ignoreCase = true)
                    ResolvedTitleChip(
                        key = "personal:$ptId",
                        label = p.name,
                        levelText = null,
                        rarity = p.rarity,
                        bgHex = if (isSsr) "#7C2D12" else "#334155",
                        textHex = if (isSsr) "#FEF3C7" else "#FFFFFF",
                        borderHex = if (isSsr) "#F59E0B" else "#94A3B8",
                        fillMode = "grad",
                        shape = "pill",
                        secondaryBgHex = if (isSsr) "#431407" else "#1E293B",
                        bgImageUrl = "https://52dmeme.com/assets/personal-v2/$ptId-title.webp",
                        specialKind = "personal_v2",
                    )
                } else {
                    val pal = ChatBubbleCosmetics.PERSONAL_V1_PALETTES[ptId]
                        ?: PersonalV1PaletteSpec("#367a78", "#e0f2e9", "#f2bc72")
                    ResolvedTitleChip(
                        key = "personal:$ptId",
                        label = p.name,
                        levelText = null,
                        rarity = p.rarity,
                        bgHex = if (isDarkTheme) "#1E293B" else pal.paperHex,
                        textHex = if (isDarkTheme) "#F1F5F9" else pal.inkHex,
                        borderHex = pal.accentHex,
                        fillMode = "solid",
                        shape = "pill",
                        specialKind = "personal_v1",
                    )
                }
            }
            activeRaffle != null -> {
                val r = activeRaffle.catalog
                val dispLv = activeRaffle.displayLevel
                if (r.id == "claude") {
                    ResolvedTitleChip(
                        key = "raffle:claude",
                        label = "拾光司书",
                        levelText = if (dispLv > 1) "Lv$dispLv" else null,
                        rarity = "SSR",
                        bgHex = "#4A2E1B",
                        textHex = "#FBF5EA",
                        borderHex = "#D4AF37",
                        fillMode = "grad",
                        shape = "ribbon",
                        secondaryBgHex = "#2A180D",
                        effect = if (activeRaffle.stage >= 6) "shine" else "none",
                        specialKind = "shiguang",
                    )
                } else {
                    val npBg = if (isDarkTheme) blendHex(r.colorHex, "#15202B", 0.34f) else blendHex(r.accentHex, "#FFFFFF", 0.34f)
                    val npSec = if (isDarkTheme) blendHex(r.accentHex, "#15202B", 0.18f) else blendHex(r.colorHex, "#FFFFFF", 0.22f)
                    val npFg = if (isDarkTheme) "#F1F5F9" else blendHex(r.colorHex, "#1E293B", 0.68f)
                    val npBd = if (isDarkTheme) r.accentHex else blendHex(r.colorHex, "#FFFFFF", 0.68f)
                    ResolvedTitleChip(
                        key = "raffle:${r.id}",
                        label = r.name,
                        levelText = if (dispLv > 1) "Lv$dispLv" else null,
                        rarity = r.rarity,
                        bgHex = npBg,
                        textHex = npFg,
                        borderHex = npBd,
                        fillMode = "grad",
                        shape = "tag",
                        secondaryBgHex = npSec,
                        effect = if (activeRaffle.stage >= 8) "shine" else "none",
                        symbolSvgPath = ChatBubbleCosmetics.TITLE_SYMBOL_PATHS[r.id],
                        specialKind = "raffle_nameplate",
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
                    bgHex = "#9D174D",
                    textHex = "#FCE7F3",
                    borderHex = "#F472B6",
                    fillMode = "grad",
                    shape = "pill",
                    secondaryBgHex = "#BE185D",
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
        val bs = obj["bs"]?.jsonPrimitive?.contentOrNull
            ?: obj["badgeScope"]?.jsonPrimitive?.contentOrNull
            ?: "all"
        if (badgeMode == "none" || (where != "card" && bs != "all" && bs != where)) {
            return ResolvedUserIdentity(title = resolvedTitle, badge = null, nickGradientHexes = nickHexes)
        }

        val bdId = (obj["bd"]?.jsonPrimitive?.contentOrNull
            ?: obj["equippedBadgeId"]?.jsonPrimitive?.contentOrNull)
            ?.trim()
            .orEmpty()
        val ownedBadges = parseOwnedBadges(badgesJson)

        fun buildCustomBadge(targetId: String): ResolvedBadgeChip? {
            if (targetId.isEmpty()) return null
            val meta = resolveBadgeMeta(targetId) ?: return null
            val ownedN = ownedBadges.firstOrNull { it.id == targetId }?.count
            val bn = obj["bn"]?.jsonPrimitive?.intOrNull
                ?: ownedN
                ?: if (cosmeticAccess == "all") 999999 else 1
            val bl = obj["bl"]?.jsonPrimitive?.intOrNull ?: computeBadgeLevel(targetId, bn).coerceAtLeast(1)
            return ResolvedBadgeChip(
                id = targetId,
                name = meta.name,
                level = bl,
                levelText = if (targetId == "elder") "★" else "Lv$bl",
                tier = meta.tier,
                desc = meta.desc,
                art = resolveBadgeArt(targetId, bl),
            )
        }

        val resolvedBadge: ResolvedBadgeChip? = when {
            commBadgeMeta != null -> {
                val cid = communityBadgeId!!
                val badgeArt: BadgeArtKind = when {
                    cid == "aurora-preset" -> BadgeArtKind.InlineXmlSvg(AURORA_COMPASS_SVG)
                    cid == "lanting-ink" -> BadgeArtKind.InlineXmlSvg(LANTING_INK_BADGE_SVG)
                    cid == "orange-flick" || cid == "laowu-dispatch" -> BadgeArtKind.InlineXmlSvg(ORANGE_FLICK_BADGE_SVG)
                    cid == "laowu-dispatch-all-in" -> {
                        val sheetUrl = commBadgeMeta.badgeImgSha?.let {
                            "https://52dmeme.com/api/community-skins/image/$it"
                        } ?: "https://52dmeme.com/api/community-skins/image/19b1612f344bacb77ff295e1015b806caf76b8c71d573f4d6ab572e20b3ede2d"
                        BadgeArtKind.SpriteSheetCell(sheetUrl, cols = 9, rows = 5, col = 2, row = 2)
                    }
                    (cid == "shadow-dao" || cid == "shadow-path") && isDarkTheme && commBadgeMeta.badgeCssSha != null -> {
                        BadgeArtKind.RemoteImage("community-css-dark:$cid:badge:${commBadgeMeta.badgeCssSha}")
                    }
                    commBadgeMeta.badgeImgSha != null -> {
                        BadgeArtKind.RemoteImage("https://52dmeme.com/api/community-skins/image/${commBadgeMeta.badgeImgSha}")
                    }
                    commBadgeMeta.badgeCssSha != null -> {
                        BadgeArtKind.RemoteImage("community-css:$cid:badge:${commBadgeMeta.badgeCssSha}")
                    }
                    else -> BadgeArtKind.EmojiGlyph("✦")
                }
                ResolvedBadgeChip(
                    id = "community:$cid",
                    name = commBadgeMeta.name,
                    level = 1,
                    levelText = "",
                    tier = "epic",
                    desc = "社区投稿徽章 · ${commBadgeMeta.author}",
                    art = badgeArt,
                )
            }
            badgeMode == "custom" -> buildCustomBadge(bdId)
            isOwner52Suit(ptId) -> {
                val suitId = ptId!!
                val p = PERSONAL_TITLES[suitId] ?: PersonalTitleCatalogItem(suitId, "52帝", "站长专属", "SSR", "owner")
                ResolvedBadgeChip(
                    id = "pt:$suitId",
                    name = p.name,
                    level = 5,
                    levelText = "SSR",
                    tier = "legend",
                    desc = p.mood,
                    art = BadgeArtKind.SpriteSheetCell(
                        url = "https://52dmeme.com/assets/owner52-v031/regalia.webp",
                        cols = 4,
                        rows = 2,
                        col = 1,
                        row = 1,
                    ),
                )
            }
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
                )
            }
            !ptId.isNullOrBlank() && PERSONAL_TITLES[ptId] != null -> {
                val p = PERSONAL_TITLES[ptId]!!
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
                )
            }
            activeRaffle != null -> {
                val r = activeRaffle.catalog
                val dispLv = activeRaffle.displayLevel
                val artKind = when (r.id) {
                    "claude" -> BadgeArtKind.RemoteImage("https://52dmeme.com/shiguang/assets/shiguang-sticker.webp")
                    "oldfarmer" -> BadgeArtKind.RemoteSvg(raffleBadgeUrl("oldfarmer"), "🥕")
                    else -> BadgeArtKind.RemoteImage(raffleBadgeUrl(r.id))
                }
                ResolvedBadgeChip(
                    id = "ft:${r.id}",
                    name = if (r.id == "claude") "拾光司书" else r.name,
                    level = dispLv,
                    levelText = "Lv$dispLv",
                    tier = if (r.rarity == "SSR") "legend" else "rare",
                    desc = r.mood,
                    art = artKind,
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
                    art = BadgeArtKind.InlineOfficialSvg(OFFICIAL_ART_SVG.getValue(otId!!)),
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
     * Parses server `me.deco` into `DecoConfig` (reading real web keys `t, sh, fill, c, c2, bc, fc, bb, bd, badge_mode, pt, ft, ot, gr, rt, community`
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
        val fromServer = if (obj != null) {
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
                nickGradient = matchedPreset,
                customGradientStops = fcList,
                bubbleSkin = obj["bb"]?.jsonPrimitive?.contentOrNull
                    ?: obj["bubbleSkin"]?.jsonPrimitive?.contentOrNull ?: "",
                badgeMode = obj["badge_mode"]?.jsonPrimitive?.contentOrNull
                    ?: obj["badgeMode"]?.jsonPrimitive?.contentOrNull ?: "set",
                equippedBadgeId = obj["bd"]?.jsonPrimitive?.contentOrNull
                    ?: obj["equippedBadgeId"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                badgeScope = obj["bs"]?.jsonPrimitive?.contentOrNull
                    ?: obj["badgeScope"]?.jsonPrimitive?.contentOrNull ?: "all",
                decoScope = obj["sc"]?.jsonPrimitive?.contentOrNull
                    ?: obj["decoScope"]?.jsonPrimitive?.contentOrNull ?: "all",
                equippedPersonalId = obj["pt"]?.jsonPrimitive?.contentOrNull
                    ?: obj["equippedPersonalId"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                equippedRaffleId = obj["ft"]?.jsonPrimitive?.contentOrNull
                    ?: obj["equippedRaffleId"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                equippedRaffleLevel = obj["fn"]?.jsonPrimitive?.intOrNull
                    ?: obj["equippedRaffleLevel"]?.jsonPrimitive?.intOrNull ?: 1,
                equippedOfficialId = obj["ot"]?.jsonPrimitive?.contentOrNull
                    ?: obj["equippedOfficialId"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                equippedGrowthId = obj["gr"]?.jsonPrimitive?.contentOrNull
                    ?: obj["equippedGrowthId"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                equippedInviteTitle = obj["rt"]?.jsonPrimitive?.contentOrNull == "guangjie" ||
                    obj["equippedInviteTitle"]?.jsonPrimitive?.booleanOrNull == true,
                communityTitleId = commObj?.get("title")?.jsonPrimitive?.contentOrNull.orEmpty(),
                communityBadgeId = commObj?.get("badge")?.jsonPrimitive?.contentOrNull.orEmpty(),
                communityBubbleId = commObj?.get("bubble")?.jsonPrimitive?.contentOrNull.orEmpty(),
                cleanHideOthersNick = obj["cleanHideOthersNick"]?.jsonPrimitive?.booleanOrNull ?: false,
                cleanHideOthersBadges = obj["cleanHideOthersBadges"]?.jsonPrimitive?.booleanOrNull ?: false,
                cleanHideOthersStyles = obj["cleanHideOthersStyles"]?.jsonPrimitive?.booleanOrNull ?: false,
                cleanHideBubbles = obj["cleanHideBubbles"]?.jsonPrimitive?.booleanOrNull ?: false,
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
                    "nickGradient", "bubbleSkin", "badgeMode", "equippedBadgeId", "badgeScope", "decoScope",
                    "equippedPersonalId", "equippedRaffleId", "equippedRaffleLevel", "equippedOfficialId",
                    "equippedGrowthId", "equippedInviteTitle", "communityTitleId", "communityBadgeId",
                    "communityBubbleId", "cleanHideOthersNick", "cleanHideOthersBadges",
                    "cleanHideOthersStyles", "cleanHideBubbles", "customGradientStops",
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
        map["sc"] = JsonPrimitive(config.decoScope.ifBlank { "all" })
        map["bs"] = JsonPrimitive(config.badgeScope.ifBlank { "all" })
        map["badge_mode"] = JsonPrimitive(config.badgeMode.ifBlank { "set" })

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
                map["ft"] = JsonPrimitive(config.equippedRaffleId)
                map["fn"] = JsonPrimitive(config.equippedRaffleLevel.coerceAtLeast(1))
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
        if (commMap.isNotEmpty()) {
            map["community"] = JsonObject(commMap)
        } else {
            map.remove("community")
        }

        return JsonObject(map)
    }
}
