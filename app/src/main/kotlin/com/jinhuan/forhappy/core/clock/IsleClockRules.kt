package com.jinhuan.forhappy.core.clock

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

/**
 * Exact Kotlin port of `pet-world-rules.js` dynamic island clock, FNV-1a avalanche hash,
 * weather/season calendar, and guild market event formulas.
 */
object IsleClockRules {
    const val ISLE_SPAN = 4L * 3600L // 14400s real time = 1 island day
    const val ISLE_LAUNCH = 1794052800L
    const val ISLE_SEASON_DAYS = 12
    const val NORMAL_VOLUME = 400

    val SEASONS = listOf("春", "夏", "秋", "冬")
    val WEATHERS = listOf("晴", "雨", "风", "雪")

    data class CropDef(
        val id: String,
        val name: String,
        val seasons: List<Int>,
        val seed: Int,
        val growMin: Int,
        val price: Int,
        val eat: Int,
        val regrowMin: Int = 0,
        val times: Int = 1,
    )

    data class AnimalDef(
        val id: String,
        val name: String,
        val price: Int,
        val everyMin: Int,
        val cap: Int,
        val product: String,
        val productName: String,
        val productPrice: Int,
    )

    data class FishDef(
        val id: String,
        val name: String,
        val price: Int,
        val weight: Int,
        val rod: Int,
        val seasons: List<Int>,
        val hours: IntRange,
    )

    data class OreDef(
        val id: String,
        val name: String,
        val price: Int,
        val weight: Double,
        val floor: Int,
    )

    data class MarketEventSpec(
        val id: String,
        val guildId: String,
        val text: String,
        val multiplier: Double,
        val itemType: String, // "list" | "season" | "crops"
        val items: List<String> = emptyList(),
        val weightFn: (season: Int, weather: Int) -> Double,
    )

    data class ActiveMarketEvent(
        val id: String,
        val guildId: String,
        val guildName: String,
        val text: String,
        val multiplier: Double,
        val items: List<String>,
    )

    data class IsleCalendarSnapshot(
        val clockConfigured: Boolean,
        val clockStart: Long,
        val dayKey: String,
        val dayNo: Long,
        val seasonIndex: Int,
        val seasonName: String,
        val seasonDaysLeft: Int,
        val weatherIndex: Int,
        val weatherName: String,
        val isleHour: Int,
        val isleMinute: Int,
        val isLongNight: Boolean,
        val nextBoundaryEpochSec: Long,
        val events: List<ActiveMarketEvent>,
    )

    val CROPS = listOf(
        CropDef("turnip", "小白萝卜", listOf(0, 3), 110, 30, 540, 8),
        CropDef("carrot", "胡萝卜", listOf(0, 2), 180, 60, 960, 10),
        CropDef("wheat", "小麦", listOf(1, 2), 220, 120, 1400, 6),
        CropDef("strawberry", "草莓", listOf(0), 720, 240, 3300, 14, 180, 4),
        CropDef("tomato", "番茄", listOf(1), 540, 180, 2550, 12, 150, 4),
        CropDef("corn", "玉米", listOf(1, 2), 900, 360, 4800, 14, 240, 3),
        CropDef("grape", "葡萄", listOf(2), 1080, 360, 5700, 12, 240, 4),
        CropDef("pumpkin", "南瓜", listOf(2, 3), 1440, 600, 9000, 20),
        CropDef("cabbage", "冬白菜", listOf(3), 450, 150, 2350, 12),
    )

    val ANIMALS = listOf(
        AnimalDef("chicken", "小鸡", 12000, 240, 3, "egg", "鸡蛋", 1350),
        AnimalDef("duck", "鸭子", 24000, 300, 3, "duckegg", "鸭蛋", 2500),
        AnimalDef("cow", "奶牛", 60000, 480, 2, "milk", "牛奶", 5850),
        AnimalDef("sheep", "绵羊", 90000, 1440, 1, "wool", "羊毛", 19000),
        AnimalDef("pig", "小猪", 150000, 720, 1, "truffle", "松露", 13500),
    )

    val FISH = listOf(
        FishDef("sardine", "沙丁鱼", 1200, 40, 1, listOf(0, 1, 2, 3), 0..24),
        FishDef("carp", "鲤鱼", 2000, 30, 1, listOf(0, 2, 3), 0..24),
        FishDef("bass", "鲈鱼", 3600, 22, 1, listOf(1, 2), 6..20),
        FishDef("salmon", "三文鱼", 7200, 14, 2, listOf(2, 3), 0..24),
        FishDef("squid", "鱿鱼", 8800, 12, 2, listOf(3, 0), 18..30),
        FishDef("tuna", "金枪鱼", 14500, 8, 3, listOf(1, 3), 0..24),
        FishDef("puffer", "河豚", 21000, 6, 3, listOf(1, 0), 12..20),
        FishDef("whale", "迷你鲸", 96000, 1, 4, listOf(0, 1, 2, 3), 0..24),
    )

    val ORES = listOf(
        OreDef("coal", "煤", 480, 40.0, 1),
        OreDef("copper", "铜矿", 900, 35.0, 1),
        OreDef("iron", "铁矿", 1900, 25.0, 6),
        OreDef("amethyst", "紫水晶", 7200, 6.0, 8),
        OreDef("gold", "金矿", 4200, 14.0, 12),
        OreDef("emerald", "祖母绿", 12000, 4.0, 15),
        OreDef("ruby", "红宝石", 18000, 3.0, 20),
        OreDef("diamond", "钻石", 36000, 1.2, 25),
    )

    val GUILDS = mapOf(
        "longgong" to "东海龙宫",
        "kunlun" to "昆仑玉坊",
        "fusang" to "扶桑神农社",
        "yingzhou" to "瀛洲酒肆",
        "qingqiu" to "青丘狐市",
        "penglai" to "蓬莱仙市",
        "buzhou" to "不周山工坊",
        "guixu" to "归墟商号",
    )

    val BASE_PRICE: Map<String, Int> = buildMap {
        CROPS.forEach { put(it.id, it.price) }
        ANIMALS.forEach { put(it.product, it.productPrice) }
        FISH.forEach { put(it.id, it.price) }
        ORES.forEach { put(it.id, it.price) }
        put("salad", 6500)
        put("flour", 9200)
        put("jam", 17000)
        put("ketchup", 13500)
        put("popcorn", 18500)
        put("wine", 31000)
        put("pie", 26000)
        put("kimchi", 12500)
        put("mayo", 9800)
        put("cheese", 23500)
        put("sweater", 50000)
        put("ironbar", 10200)
        put("ingot", 23000)
        put("jewel", 46000)
        put("crown", 108000)
    }

    val ITEM_NAME: Map<String, String> = buildMap {
        CROPS.forEach {
            put(it.id, it.name)
            put("sd_${it.id}", "${it.name}种子")
        }
        ANIMALS.forEach { put(it.product, it.productName) }
        FISH.forEach { put(it.id, it.name) }
        ORES.forEach { put(it.id, it.name) }
        put("feed", "饲料")
        put("bait", "鱼饵")
        put("bomb", "炸药")
        put("salad", "田园沙拉")
        put("flour", "面粉")
        put("jam", "草莓果酱")
        put("ketchup", "番茄酱")
        put("popcorn", "爆米花")
        put("wine", "葡萄酒")
        put("pie", "南瓜派")
        put("kimchi", "泡菜")
        put("mayo", "蛋黄酱")
        put("cheese", "奶酪")
        put("sweater", "羊绒毛衣")
        put("ironbar", "铁锭")
        put("ingot", "金锭")
        put("jewel", "宝石项链")
        put("crown", "钻石王冠")
        put("tide_snack", "潮汐饭团")
    }

    private val FISH_IDS = FISH.map { it.id }
    private val GEMS = listOf("amethyst", "emerald", "ruby", "diamond")
    private val METAL = listOf("coal", "copper", "iron", "gold")
    private val ANIMAL_PROD = ANIMALS.map { it.product }

    val MARKET_EVENTS = listOf(
        MarketEventSpec("feast", "longgong", "东海龙宫大摆水族宴，四处高价收鲜鱼", 1.35, "list", FISH_IDS) { _, w -> if (w == 1) 3.0 else 1.0 },
        MarketEventSpec("seal", "longgong", "龙王闭关，渔市冷清", 0.82, "list", FISH_IDS) { _, _ -> 0.7 },
        MarketEventSpec("storm", "longgong", "大风封港，鲜鱼紧俏", 1.5, "list", FISH_IDS) { _, w -> if (w == 2) 3.0 else 0.0 },
        MarketEventSpec("jade", "kunlun", "昆仑玉坊开窑，收宝石和首饰", 1.4, "list", GEMS + listOf("jewel", "crown")) { _, w -> if (w == 3) 3.0 else 1.0 },
        MarketEventSpec("jadeglut", "kunlun", "昆仑玉坊库房堆满，宝石压价", 0.8, "list", GEMS) { _, _ -> 0.7 },
        MarketEventSpec("forge", "buzhou", "不周山工坊开炉铸器，矿石和锭子抢手", 1.35, "list", METAL + listOf("ironbar", "ingot")) { s, _ -> if (s == 3) 2.0 else 1.0 },
        MarketEventSpec("quake", "buzhou", "不周山地动，矿道封闭，矿价飞涨", 1.55, "list", METAL + GEMS) { _, _ -> 0.35 },
        MarketEventSpec("harvest", "fusang", "扶桑神农社丰收祭，当季作物满街", 0.8, "season") { s, _ -> if (s == 2) 2.5 else 1.0 },
        MarketEventSpec("locust", "fusang", "扶桑闹虫灾，作物紧缺", 1.4, "crops") { s, _ -> if (s == 1) 2.0 else 0.6 },
        MarketEventSpec("frost", "fusang", "寒潮南下，冬菜涨价", 1.45, "list", listOf("cabbage", "turnip", "carrot", "kimchi", "salad")) { s, w -> if (s == 3) (if (w == 3) 3.0 else 2.0) else 0.0 },
        MarketEventSpec("rain", "fusang", "甘霖普降，菜农丰收", 0.85, "crops") { _, w -> if (w == 1) 2.0 else 0.0 },
        MarketEventSpec("brew", "yingzhou", "瀛洲酒肆酿新酒，收葡萄和小麦", 1.45, "list", listOf("grape", "wheat", "wine", "flour")) { s, _ -> if (s == 2) 2.5 else 1.0 },
        MarketEventSpec("lantern", "qingqiu", "青丘狐市办灯会，南瓜和点心热卖", 1.35, "list", listOf("pumpkin", "egg", "duckegg", "pie", "jam", "popcorn")) { s, _ -> if (s >= 2) 2.0 else 0.7 },
        MarketEventSpec("fox", "qingqiu", "青丘九尾狐出嫁，毛衣和首饰被抢购", 1.5, "list", listOf("wool", "sweater", "jewel")) { s, _ -> if (s == 3) 1.5 else 0.4 },
        MarketEventSpec("peach", "penglai", "蓬莱仙市开蟠桃会，鲜果供不应求", 1.35, "list", listOf("strawberry", "tomato", "corn", "jam", "ketchup", "salad")) { s, _ -> if (s <= 1) 2.0 else 0.6 },
        MarketEventSpec("immortal", "penglai", "蓬莱仙人下山采办，松露奶酪价翻倍", 1.6, "list", listOf("truffle", "cheese", "wine")) { _, _ -> 0.35 },
        MarketEventSpec("hoard", "guixu", "归墟商号大量抛售畜产", 0.78, "list", ANIMAL_PROD + listOf("cheese", "mayo")) { _, _ -> 1.0 },
        MarketEventSpec("caravan", "guixu", "归墟商号的商队到港，奇货高价收", 1.4, "list", listOf("truffle", "wool", "cheese", "crown", "wine", "sweater")) { _, _ -> 0.8 },
    )

    private val b12ClockEpoch = AtomicLong(0L)

    fun b12ConfigureClock(epoch: Long): Long {
        require(epoch in ISLE_LAUNCH..4_000_000_000L) { "长夜时钟配置无效: $epoch" }
        b12ClockEpoch.set(epoch)
        return epoch
    }

    fun b12ResetForTest() {
        b12ClockEpoch.set(0L)
    }

    fun b12ClockStart(): Long = b12ClockEpoch.get()

    internal fun b12ActiveSeconds(t: Long): Long {
        val local = t + 28800L
        val day = Math.floorDiv(local, 86400L)
        val second = local - day * 86400L
        val activeInDay = (second - 28800L).coerceIn(0L, 50400L)
        return day * 50400L + activeInDay
    }

    fun b12ClockSeconds(t: Long): Long {
        val epoch = b12ClockEpoch.get()
        if (epoch == 0L || t <= epoch) return t + 28800L
        return epoch + 28800L + b12ActiveSeconds(t) - b12ActiveSeconds(epoch)
    }

    fun b12IsLongNight(t: Long): Boolean {
        val hour = hourOf(t)
        return hour >= 22 || hour < 8
    }

    fun hourOf(t: Long): Int {
        val localDaySec = Math.floorMod(t + 28800L, 86400L)
        return (localDaySec / 3600L).toInt()
    }

    fun b12NextIsleBoundary(t: Long): Long {
        var left = ISLE_SPAN - Math.floorMod(b12ClockSeconds(t), ISLE_SPAN)
        val epoch = b12ClockEpoch.get()
        if (epoch == 0L || t < epoch) return t + left
        var cursor = t
        repeat(3) {
            val second = Math.floorMod(cursor + 28800L, 86400L)
            if (second < 28800L) {
                cursor += 28800L - second
                return@repeat
            }
            if (second >= 79200L) {
                cursor += 86400L - second + 28800L
                return@repeat
            }
            val room = 79200L - second
            if (left <= room) return cursor + left
            left -= room
            cursor += room
        }
        return cursor + left
    }

    fun isleIndex(t: Long): Long = Math.floorDiv(b12ClockSeconds(t), ISLE_SPAN)

    fun isleDay(t: Long): String {
        val epochDay = isleIndex(t)
        return Instant.ofEpochMilli(epochDay * 86400_000L)
            .atOffset(ZoneOffset.UTC)
            .toLocalDate()
            .format(DateTimeFormatter.ISO_LOCAL_DATE)
    }

    fun isleHour(t: Long): Int {
        val mod = Math.floorMod(b12ClockSeconds(t), ISLE_SPAN)
        return (mod / (ISLE_SPAN / 24L)).toInt()
    }

    fun isleMinute(t: Long): Int {
        val mod = Math.floorMod(b12ClockSeconds(t), ISLE_SPAN)
        val rem = Math.floorMod(mod, ISLE_SPAN / 24L)
        return (rem / (ISLE_SPAN / 1440L)).toInt()
    }

    fun isleDayNo(t: Long): Long = isleIndex(t) - isleIndex(ISLE_LAUNCH) + 1L

    fun isleSeasonLeft(t: Long): Int {
        val delta = isleIndex(t) - isleIndex(ISLE_LAUNCH)
        val mod = Math.floorMod(delta, ISLE_SEASON_DAYS.toLong()).toInt()
        return ISLE_SEASON_DAYS - 1 - mod
    }

    private fun dayNum(day: String): Long = LocalDate.parse(day, DateTimeFormatter.ISO_LOCAL_DATE).toEpochDay()

    fun isIsleDay(day: String): Boolean = day > "2100-01-01"

    fun seasonOf(day: String): Int {
        return if (isIsleDay(day)) {
            val idxDiff = dayNum(day) - isleIndex(ISLE_LAUNCH)
            Math.floorMod(Math.floorDiv(idxDiff, ISLE_SEASON_DAYS.toLong()), 4L).toInt()
        } else {
            val diff = dayNum(day) - dayNum("2026-11-02")
            Math.floorMod(Math.floorDiv(diff, 7L), 4L).toInt()
        }
    }

    /**
     * Exact JS `Math.imul` 32-bit FNV-1a + avalanche hash producing double in [0, 1).
     */
    fun worldHash(s: String): Double {
        var h = 2166136261L.toInt()
        for (ch in s) {
            h = h xor ch.code
            h *= 16777619
        }
        h = h xor (h ushr 16)
        h *= 0x85ebca6b.toInt()
        h = h xor (h ushr 13)
        h *= 0xc2b2ae35.toInt()
        h = h xor (h ushr 16)
        val unsigned = h.toLong() and 0xFFFF_FFFFL
        return unsigned.toDouble() / 4294967296.0
    }

    fun weatherOf(day: String): Int {
        val r = worldHash("weather:$day")
        val s = seasonOf(day)
        if (s == 3) {
            return when {
                r < 0.35 -> 3
                r < 0.5 -> 1
                r < 0.65 -> 2
                else -> 0
            }
        }
        return when {
            r < 0.3 -> 1
            r < 0.45 -> 2
            else -> 0
        }
    }

    fun marketEvents(day: String): List<ActiveMarketEvent> {
        if (!isIsleDay(day)) return emptyList()
        val s = seasonOf(day)
        val w = weatherOf(day)
        var pool = MARKET_EVENTS
            .map { spec -> spec to spec.weightFn(s, w) }
            .filter { it.second > 0.0 }
        val out = mutableListOf<ActiveMarketEvent>()
        var k = 0
        while (k < 2 && pool.isNotEmpty()) {
            val total = pool.sumOf { it.second }
            var x = worldHash("mkt:$day:$k") * total
            var idx = 0
            while (idx < pool.size - 1) {
                x -= pool[idx].second
                if (x < 0.0) break
                idx++
            }
            val picked = pool[idx].first
            pool = pool.filter { it.first.guildId != picked.guildId }
            val items = when (picked.itemType) {
                "season" -> CROPS.filter { s in it.seasons }.map { it.id }
                "crops" -> CROPS.map { it.id }
                else -> picked.items
            }
            out += ActiveMarketEvent(
                id = picked.id,
                guildId = picked.guildId,
                guildName = GUILDS[picked.guildId].orEmpty(),
                text = picked.text,
                multiplier = picked.multiplier,
                items = items,
            )
            k++
        }
        return out
    }

    fun eventFactor(item: String, day: String): Double =
        marketEvents(day).fold(1.0) { acc, ev -> if (item in ev.items) acc * ev.multiplier else acc }

    fun marketFactor(item: String, day: String): Double {
        val d = dayNum(day)
        val weights = doubleArrayOf(1.0, 0.6, 0.35, 0.15)
        var walk = 0.0
        for (k in weights.indices) {
            walk += (worldHash("$item:${d - k}") - 0.5) * weights[k]
        }
        val isle = isIsleDay(day)
        val eventProb = if (isle) 0.015 else 0.04
        val eventMult = if (worldHash("event:$item:$d") < eventProb) {
            if (worldHash("ev2:$item$d") < 0.5) {
                if (isle) 0.8 else 0.7
            } else {
                if (isle) 1.3 else 1.45
            }
        } else {
            1.0
        }
        val crop = CROPS.find { it.id == item }
        val season = seasonOf(day)
        val seasonal = if (crop != null) {
            if (season in crop.seasons) 0.92 else 1.3
        } else {
            1.0
        }
        return if (isle) {
            max(0.65, min(1.7, exp(walk * 0.5 - 0.02) * seasonal * eventMult))
        } else {
            max(0.5, min(2.0, exp(walk * 0.7 - 0.03) * seasonal * eventMult))
        }
    }

    fun supplyFactor(soldYesterday: Int, normal: Int): Double =
        max(0.8, min(1.06, 1.06 - 0.3 * (soldYesterday.toDouble() / max(1, normal).toDouble())))

    fun marketDepth(item: String): Int {
        val base = BASE_PRICE[item] ?: 1000
        val ratio = (1_600_000.0 / max(1, base)).roundToLong().toInt()
        return ratio.coerceIn(800, 6000)
    }

    fun priceOf(item: String, day: String, soldYesterday: Int = 0): Int {
        val base = BASE_PRICE[item] ?: return 0
        val depth = if (isIsleDay(day)) marketDepth(item) else NORMAL_VOLUME
        val raw = base * marketFactor(item, day) * supplyFactor(soldYesterday, depth) * eventFactor(item, day)
        return max(1, raw.roundToLong().toInt())
    }

    /**
     * Port of `cropStage` from `pet-world-rules.js`:
     * returns 0..2 (growing), 3 (mature ready to harvest), or -1 (off-season withered).
     */
    fun cropStage(
        cropId: String,
        plantedAt: Long,
        bonusSec: Long,
        harvestsDone: Int,
        autoWatered: Boolean,
        nowSec: Long,
    ): Int {
        val crop = CROPS.find { it.id == cropId } ?: return 0
        val validSeason = if (plantedAt >= ISLE_LAUNCH) seasonOf(isleDay(plantedAt)) in crop.seasons else true
        if (!validSeason) return -1
        val fast = plantedAt >= ISLE_LAUNCH
        val baseMin = if (harvestsDone > 0 && crop.regrowMin > 0) {
            crop.regrowMin * (if (fast) 0.8 else 1.0)
        } else {
            crop.growMin.toDouble()
        }
        val baseSec = baseMin * 60.0 * (if (fast) 0.8 else 1.0)
        val total = baseSec * (if (autoWatered) 0.6 else 1.0) - bonusSec
        val elapsed = (nowSec - plantedAt).toDouble()
        if (elapsed >= total) return 3
        return min(2, max(0, (elapsed / max(1.0, total) * 3.0).toInt()))
    }

    fun snapshot(t: Long = ServerClock.serverNowSec()): IsleCalendarSnapshot {
        val day = isleDay(t)
        val season = seasonOf(day)
        val weather = weatherOf(day)
        return IsleCalendarSnapshot(
            clockConfigured = b12ClockEpoch.get() > 0L,
            clockStart = b12ClockEpoch.get(),
            dayKey = day,
            dayNo = isleDayNo(t),
            seasonIndex = season,
            seasonName = SEASONS[season],
            seasonDaysLeft = isleSeasonLeft(t),
            weatherIndex = weather,
            weatherName = WEATHERS[weather],
            isleHour = isleHour(t),
            isleMinute = isleMinute(t),
            isLongNight = b12IsLongNight(t),
            nextBoundaryEpochSec = b12NextIsleBoundary(t),
            events = marketEvents(day),
        )
    }
}
