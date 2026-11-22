package com.jinhuan.forhappy.ui.hub

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jinhuan.forhappy.core.chat.ChatSessionController
import com.jinhuan.forhappy.core.clock.ServerClock
import com.jinhuan.forhappy.core.db.ForHappyDatabase
import com.jinhuan.forhappy.core.model.AppDisguiseMode
import com.jinhuan.forhappy.core.model.DynamicModule
import com.jinhuan.forhappy.core.model.ForHappyJson
import com.jinhuan.forhappy.core.model.MeUser
import com.jinhuan.forhappy.core.model.MoyuConfig
import com.jinhuan.forhappy.core.model.TodayStats
import com.jinhuan.forhappy.core.moyu.MoyuCalculator
import com.jinhuan.forhappy.core.network.api.ForHappyApiClient
import com.jinhuan.forhappy.ui.components.BadgePill
import com.jinhuan.forhappy.ui.theme.ForHappyDimens
import com.jinhuan.forhappy.ui.theme.LocalForHappyPalette
import com.jinhuan.forhappy.ui.theme.TABULAR_FONT_FEATURE
import com.jinhuan.forhappy.ui.wall.ConfigureFullscreenDialogWindow
import com.jinhuan.forhappy.ui.wall.DialogBackOwnerScope
import com.jinhuan.forhappy.ui.wall.FullscreenDialogProperties
import com.jinhuan.forhappy.ui.web.ShelllessWebScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer

data class HubModuleEntry(
    val id: String,
    val label: String,
    val route: String,
    val emoji: String,
    val subtitle: String,
    val isNew: Boolean = false,
)

private val BUILT_IN_HUB_CATALOG = listOf(
    HubModuleEntry("moyu", "摸鱼日历", "/moyu", "📅", "倒计时 · 每日日报"),
    HubModuleEntry("road", "鞭打路线", "/road", "🛣️", "点子 · Bug · 投票"),
    HubModuleEntry("novels", "尽欢书房", "/novels", "📖", "接龙协作 · 摘录分享"),
    HubModuleEntry("pets", "鱼鱼谷", "/pets", "🐾", "农场养宠 · 邻居串门"),
    HubModuleEntry("arcade", "街机游乐厅", "/arcade", "🎮", "史莱姆 · 欢鼠 · 裂隙"),
    HubModuleEntry("cards52b", "52B 抽卡", "/cards", "🃏", "卡包抽取 · 收集图鉴"),
    HubModuleEntry("raffle", "抽奖广场", "/raffle", "🎁", "欧皇抽奖 · 限定称号"),
)

private val EXCLUDED_DYNAMIC_PREFIXES = listOf("/wall", "/chat", "/me", "/settings")

fun resolveHubCatalog(dynamicModules: List<DynamicModule>): List<HubModuleEntry> {
    val builtInRoutes = BUILT_IN_HUB_CATALOG.map { it.route.trimEnd('/') }.toSet()
    val extras = dynamicModules.mapNotNull { mod ->
        val normRoute = mod.route.trim().let { if (it.startsWith("/")) it else "/$it" }.trimEnd('/')
        if (normRoute in builtInRoutes) return@mapNotNull null
        if (EXCLUDED_DYNAMIC_PREFIXES.any { normRoute == it || normRoute.startsWith("$it/") }) return@mapNotNull null
        val emoji = when (mod.iconKey.lowercase()) {
            "book", "novel" -> "📚"
            "game", "arcade" -> "🕹️"
            "gift", "raffle" -> "🎁"
            "chart", "rank" -> "📊"
            "pet", "farm" -> "🌱"
            else -> "✨"
        }
        HubModuleEntry(
            id = mod.id.ifBlank { "dyn_${normRoute.trimStart('/')}" },
            label = mod.label,
            route = normRoute,
            emoji = emoji,
            subtitle = normRoute,
            isNew = mod.isNewFromTopbar,
        )
    }
    return BUILT_IN_HUB_CATALOG + extras
}

suspend fun loadPinnedHubIds(api: ForHappyApiClient, catalog: List<HubModuleEntry>): List<String> {
    val validIds = catalog.map { it.id }.toSet()
    val raw = api.getPref(KV_HUB_PINNED)
    val parsed = if (!raw.isNullOrBlank()) {
        runCatching { ForHappyJson.decodeFromString(ListSerializer(String.serializer()), raw) }
            .getOrDefault(emptyList())
            .filter { it in validIds }
            .distinct()
            .take(8)
    } else {
        emptyList()
    }
    return parsed.ifEmpty { catalog.take(6).map { it.id } }
}

suspend fun savePinnedHubIds(api: ForHappyApiClient, ids: List<String>) {
    val capped = ids.distinct().take(8)
    api.putPref(KV_HUB_PINNED, ForHappyJson.encodeToString(ListSerializer(String.serializer()), capped))
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun HubScreen(
    api: ForHappyApiClient,
    @Suppress("UNUSED_PARAMETER") db: ForHappyDatabase,
    me: MeUser?,
    modules: List<DynamicModule>,
    initialModuleRoute: String?,
    onConsumeInitialRoute: () -> Unit,
    onRefreshModules: () -> Unit,
    onRequireLogin: () -> Unit,
    notifUnreadCount: Int = 0,
    onOpenNotifications: () -> Unit = {},
    onJumpUnseenWall: () -> Unit = {},
    chatController: ChatSessionController? = null,
    effectiveDisguise: AppDisguiseMode = AppDisguiseMode.NONE,
    bossKeyActive: Boolean = false,
    todayStats: TodayStats? = null,
    moyuConfig: MoyuConfig = MoyuConfig(),
    onMoyuConfigUpdated: (MoyuConfig) -> Unit = {},
    moyuForegroundSec: Long = 0L,
    onResetMoyuForeground: () -> Unit = {},
    onPinnedIdsChanged: (List<String>) -> Unit = {},
    onJumpToGroupChat: ((Long) -> Unit)? = null,
) {
    val palette = LocalForHappyPalette.current
    val scope = rememberCoroutineScope()

    val catalog = remember(modules) { resolveHubCatalog(modules) }
    var pinnedIds by remember { mutableStateOf(catalog.take(6).map { it.id }) }
    var editingPinned by remember { mutableStateOf(false) }

    BackHandler(enabled = editingPinned) {
        editingPinned = false
    }

    var showMoyuDialog by remember { mutableStateOf(false) }
    var showRoadDialog by remember { mutableStateOf(false) }
    var showArcadeSheet by remember { mutableStateOf(false) }
    var activeWebEntry by remember { mutableStateOf<HubModuleEntry?>(null) }

    var nowEpochSec by remember { mutableLongStateOf(ServerClock.serverNowMs() / 1000L) }
    LaunchedEffect(Unit) {
        while (true) {
            nowEpochSec = ServerClock.serverNowMs() / 1000L
            delay(1000L)
        }
    }

    LaunchedEffect(catalog) {
        val loaded = loadPinnedHubIds(api, catalog)
        pinnedIds = loaded
        onPinnedIdsChanged(loaded)
    }

    fun launchModuleEntry(entry: HubModuleEntry) {
        val route = entry.route.trimEnd('/')
        when {
            route == "/moyu" -> showMoyuDialog = true
            route == "/road" -> showRoadDialog = true
            route == "/arcade" -> showArcadeSheet = true
            else -> activeWebEntry = entry
        }
    }

    fun launchRoute(rawRoute: String, fallbackTitle: String = "探索页面") {
        val clean = rawRoute.trim()
        val norm = if (clean.startsWith("http")) clean else {
            val p = if (clean.startsWith("/")) clean else "/$clean"
            p.trimEnd('/').ifBlank { "/" }
        }
        when {
            norm == "/moyu" -> showMoyuDialog = true
            norm == "/road" -> showRoadDialog = true
            norm == "/arcade" -> showArcadeSheet = true
            else -> {
                val match = catalog.firstOrNull { it.route.trimEnd('/') == norm }
                activeWebEntry = match ?: HubModuleEntry(
                    id = "route_${norm.hashCode()}",
                    label = fallbackTitle,
                    route = clean,
                    emoji = "🌐",
                    subtitle = clean,
                )
            }
        }
    }

    LaunchedEffect(initialModuleRoute) {
        val req = initialModuleRoute?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        onConsumeInitialRoute()
        launchRoute(req)
    }

    fun togglePin(id: String) {
        val next = if (id in pinnedIds) {
            if (pinnedIds.size <= 1) pinnedIds else pinnedIds - id
        } else {
            (pinnedIds + id).take(8)
        }
        pinnedIds = next
        onPinnedIdsChanged(next)
        scope.launch { savePinnedHubIds(api, next) }
    }

    val pinnedEntries = remember(catalog, pinnedIds) {
        val map = catalog.associateBy { it.id }
        pinnedIds.mapNotNull { map[it] }
    }
    val moreEntries = remember(catalog, pinnedIds) {
        catalog.filter { it.id !in pinnedIds }
    }

    val moyuSnapshot = remember(nowEpochSec, moyuConfig) {
        MoyuCalculator.compute(nowEpochSec, moyuConfig)
    }
    val navBottomPad = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.bg),
    ) {
        // Discover Header
        Surface(color = if (palette.isModernUi) palette.glassSurface else palette.surface) {
            Column(
                modifier = if (palette.isModernUi) Modifier.fillMaxWidth().statusBarsPadding() else Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (palette.isModernUi) 52.dp else ForHappyDimens.topBarHeight)
                        .padding(horizontal = if (palette.isModernUi) 16.dp else 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "发现",
                        color = palette.text,
                        fontSize = if (palette.isModernUi) 22.sp else 18.sp,
                        fontWeight = if (palette.isModernUi) FontWeight.ExtraBold else FontWeight.Bold,
                        letterSpacing = if (palette.isModernUi) (-0.5).sp else TextStyle.Default.letterSpacing,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(if (palette.isModernUi) 6.dp else 0.dp),
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(if (palette.isModernUi) 36.dp else 40.dp)
                                .clip(CircleShape)
                                .let { if (palette.isModernUi) it.background(palette.surface2.copy(alpha = 0.78f)) else it }
                                .clickable { onRefreshModules() },
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = "刷新模块", tint = palette.muted, modifier = Modifier.size(19.dp))
                        }
                        if (me == null) {
                            BadgePill(
                                text = "登录",
                                containerColor = palette.primary,
                                contentColor = palette.onPrimary,
                                modifier = Modifier.clickable { onRequireLogin() },
                            )
                        } else {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(if (palette.isModernUi) 36.dp else 40.dp)
                                    .clip(CircleShape)
                                    .let { if (palette.isModernUi) it.background(palette.surface2.copy(alpha = 0.78f)) else it }
                                    .clickable { onOpenNotifications() },
                            ) {
                                Box(contentAlignment = Alignment.TopEnd) {
                                    Icon(
                                        imageVector = Icons.Rounded.NotificationsNone,
                                        contentDescription = "通知",
                                        tint = if (notifUnreadCount > 0) palette.text else palette.muted,
                                        modifier = Modifier.size(if (palette.isModernUi) 19.dp else 21.dp),
                                    )
                                    if (notifUnreadCount > 0) {
                                        Box(
                                            modifier = Modifier
                                                .offset(x = 5.dp, y = (-3).dp)
                                                .clip(RoundedCornerShape(999.dp))
                                                .background(palette.badge)
                                                .padding(horizontal = 4.dp, vertical = 1.dp),
                                        ) {
                                            Text(
                                                text = if (notifUnreadCount > 99) "99+" else "$notifUnreadCount",
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                style = TextStyle(fontFeatureSettings = TABULAR_FONT_FEATURE),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                HorizontalDivider(
                    color = if (palette.isModernUi) palette.glassBorder else palette.line,
                    thickness = if (palette.isModernUi) 0.5.dp else 1.dp,
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = if (palette.isModernUi) 16.dp else 14.dp,
                end = if (palette.isModernUi) 16.dp else 14.dp,
                top = 14.dp,
                bottom = if (palette.isModernUi) navBottomPad + 92.dp else 14.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(if (palette.isModernUi) 16.dp else 14.dp),
        ) {
            // 1. Moyu Hero Card
            item(key = "moyu_hero") {
                val showSalary = moyuConfig.monthlySalary > 0.0 &&
                    effectiveDisguise == AppDisguiseMode.NONE &&
                    !bossKeyActive
                val heroShape = RoundedCornerShape(if (palette.isModernUi) 24.dp else 16.dp)
                val heroGlow = Brush.linearGradient(
                    listOf(
                        palette.primary.copy(alpha = if (palette.isDark) 0.16f else 0.08f),
                        Color.Transparent,
                    ),
                )
                Surface(
                    color = palette.surface,
                    shape = heroShape,
                    shadowElevation = if (palette.isModernUi && !palette.isDark) 3.dp else 0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (palette.isModernUi) {
                                if (palette.cardBorder != Color.Transparent) Modifier.border(1.dp, palette.cardBorder, heroShape) else Modifier
                            } else {
                                Modifier.border(1.dp, palette.line, heroShape)
                            },
                        )
                        .clip(heroShape)
                        .clickable { showMoyuDialog = true },
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .let { if (palette.isModernUi) it.background(heroGlow) else it },
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(if (palette.isModernUi) 18.dp else 16.dp),
                            verticalArrangement = Arrangement.spacedBy(if (palette.isModernUi) 12.dp else 10.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "摸鱼看板",
                                        color = if (palette.isModernUi) palette.primary else palette.muted,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        text = moyuSnapshot.countdownText,
                                        color = palette.text,
                                        fontSize = if (palette.isModernUi) 21.sp else 18.sp,
                                        style = TextStyle(fontFeatureSettings = TABULAR_FONT_FEATURE),
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                                if (showSalary) {
                                    Surface(
                                        color = palette.gold.copy(alpha = 0.14f),
                                        shape = RoundedCornerShape(999.dp),
                                        modifier = if (moyuConfig.showPetOnCard) Modifier.padding(end = 54.dp) else Modifier,
                                    ) {
                                        Text(
                                            text = "今日 ${moyuSnapshot.formattedEarnedToday}",
                                            color = palette.gold,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            style = TextStyle(fontFeatureSettings = TABULAR_FONT_FEATURE),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        )
                                    }
                                }
                            }

                            LinearProgressIndicator(
                                progress = { moyuSnapshot.progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(if (palette.isModernUi) 8.dp else 7.dp)
                                    .clip(RoundedCornerShape(999.dp)),
                                color = palette.primary,
                                trackColor = palette.surface2,
                            )

                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                HeroMiniPill("周末 ${moyuSnapshot.daysToWeekend}天")
                                HeroMiniPill("发薪 ${moyuSnapshot.daysToPayday}天")
                                moyuSnapshot.holidayCountdowns.take(2).forEach { (hName, hDays) ->
                                    HeroMiniPill("$hName ${hDays}天")
                                }
                            }

                            val newSeen = todayStats?.let { if (it.newSeen > 0) it.newSeen else it.seen } ?: 0
                            val remaining = (200 - newSeen).coerceAtLeast(0)
                            Surface(
                                color = if (palette.isModernUi) palette.primaryContainer else palette.primary.copy(alpha = 0.10f),
                                shape = RoundedCornerShape(if (palette.isModernUi) 14.dp else 10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(if (palette.isModernUi) 14.dp else 10.dp))
                                    .clickable {
                                        if (remaining > 0) onJumpUnseenWall() else showMoyuDialog = true
                                    },
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = if (palette.isModernUi) 10.dp else 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = if (remaining > 0) {
                                            "🎯 今日未看还差 $remaining 帖 · 冲刺「摸鱼高手」"
                                        } else {
                                            "🏅 今日已看 $newSeen 帖 · 点击生成摸鱼日报"
                                        },
                                        color = palette.primary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Text(
                                        text = if (remaining > 0) "去刷图 →" else "日报 →",
                                        color = palette.primary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }

                        if (moyuConfig.showPetOnCard) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(10.dp)
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(palette.surface2)
                                    .then(if (palette.isModernUi) Modifier else Modifier.border(1.dp, palette.line, CircleShape))
                                    .clickable { launchRoute("/pets", "鱼鱼谷") },
                            ) {
                                Text("🐾", fontSize = 22.sp)
                            }
                        }
                    }
                }
            }

            // 2. Editable `常用` Bento / 8-grid
            item(key = "pinned_section") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "常用 (${pinnedEntries.size}/8)",
                            color = palette.text,
                            fontSize = if (palette.isModernUi) 16.sp else 15.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Surface(
                            color = if (editingPinned) palette.primary else palette.surface2,
                            shape = RoundedCornerShape(999.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .clickable { editingPinned = !editingPinned },
                        ) {
                            Text(
                                text = if (editingPinned) "完成" else "编辑",
                                color = if (editingPinned) palette.onPrimary else palette.muted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            )
                        }
                    }

                    if (palette.isModernUi && !editingPinned && pinnedEntries.size >= 2) {
                        val featured = pinnedEntries.take(2)
                        val rest = pinnedEntries.drop(2)
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                featured.forEach { entry ->
                                    FeaturedBentoTile(
                                        entry = entry,
                                        onClick = { launchModuleEntry(entry) },
                                        onLongClick = { editingPinned = true },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                            if (rest.isNotEmpty()) {
                                val cols = if (rest.size == 4) 4 else 3
                                rest.chunked(cols).forEach { rowItems ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        rowItems.forEach { entry ->
                                            PinnedGridTile(
                                                entry = entry,
                                                editing = false,
                                                onClick = { launchModuleEntry(entry) },
                                                onLongClick = { editingPinned = true },
                                                modifier = Modifier.weight(1f),
                                            )
                                        }
                                        repeat(cols - rowItems.size) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        val rows = pinnedEntries.chunked(4)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            rows.forEach { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    rowItems.forEach { entry ->
                                        PinnedGridTile(
                                            entry = entry,
                                            editing = editingPinned,
                                            onClick = {
                                                if (editingPinned) togglePin(entry.id) else launchModuleEntry(entry)
                                            },
                                            onLongClick = { editingPinned = true },
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                    repeat(4 - rowItems.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. `更多模块` section (unpinned or NEW topbar extensions)
            if (moreEntries.isNotEmpty() || editingPinned) {
                item(key = "more_header") {
                    Text(
                        text = if (editingPinned) "点击 + 固定到常用（长按底部「发现」显示前4个）" else "更多模块",
                        color = palette.muted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                items(moreEntries, key = { it.id }) { entry ->
                    val rowShape = RoundedCornerShape(if (palette.isModernUi) 18.dp else 14.dp)
                    Surface(
                        color = palette.surface,
                        shape = rowShape,
                        shadowElevation = if (palette.isModernUi && !palette.isDark) 1.dp else 0.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (palette.isModernUi) {
                                    if (palette.cardBorder != Color.Transparent) Modifier.border(1.dp, palette.cardBorder, rowShape) else Modifier
                                } else {
                                    Modifier.border(1.dp, palette.line, rowShape)
                                },
                            )
                            .clip(rowShape)
                            .clickable {
                                if (editingPinned) togglePin(entry.id) else launchModuleEntry(entry)
                            },
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(palette.surface2),
                            ) {
                                Text(entry.emoji, fontSize = 20.sp)
                            }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(entry.label, color = palette.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                    if (entry.isNew) {
                                        BadgePill("NEW", containerColor = palette.badge, contentColor = Color.White)
                                    }
                                }
                                Text(entry.subtitle, color = palette.muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            if (editingPinned) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(palette.primary.copy(alpha = 0.14f)),
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = "添加到常用", tint = palette.primary, modifier = Modifier.size(16.dp))
                                }
                            } else {
                                Text("打开 ›", color = palette.primary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showMoyuDialog) {
        MoyuCalendarDialog(
            api = api,
            me = me,
            initialConfig = moyuConfig,
            todayStats = todayStats,
            foregroundSec = moyuForegroundSec,
            chatController = chatController,
            hideMoneyAmount = effectiveDisguise != AppDisguiseMode.NONE || bossKeyActive,
            onResetForeground = onResetMoyuForeground,
            onConfigSaved = onMoyuConfigUpdated,
            onRequireLogin = onRequireLogin,
            onDismiss = { showMoyuDialog = false },
        )
    }

    if (showRoadDialog) {
        NativeRoadDialog(
            api = api,
            me = me,
            onRequireLogin = onRequireLogin,
            onDismiss = { showRoadDialog = false },
        )
    }

    if (showArcadeSheet) {
        ArcadeLauncherSheet(
            api = api,
            onLaunchRoute = { gameRoute ->
                showArcadeSheet = false
                launchRoute(gameRoute, "街机小游戏")
            },
            onDismiss = { showArcadeSheet = false },
        )
    }

    activeWebEntry?.let { entry ->
        Dialog(
            onDismissRequest = { activeWebEntry = null },
            properties = FullscreenDialogProperties,
        ) {
            ConfigureFullscreenDialogWindow(darkStatusIcons = true)
            DialogBackOwnerScope {
                Box(modifier = Modifier.fillMaxSize().background(palette.bg)) {
                    ShelllessWebScreen(
                        route = entry.route,
                        title = entry.label,
                        cookieJar = api.cookieJar,
                        assetCache = api.webAssetCache,
                        showCompactBar = false,
                        floatingCapsule = true,
                        pausedOverlay = bossKeyActive,
                        isPinnedToHub = entry.id in pinnedIds,
                        onTogglePinToHub = { togglePin(entry.id) },
                        onExit = { activeWebEntry = null },
                        api = api,
                        chatController = chatController,
                        me = me,
                        onRequireLogin = onRequireLogin,
                        onJumpToGroupChat = if (onJumpToGroupChat != null) {
                            { gid ->
                                activeWebEntry = null
                                onJumpToGroupChat.invoke(gid)
                            }
                        } else {
                            null
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroMiniPill(text: String) {
    val palette = LocalForHappyPalette.current
    Surface(
        color = palette.surface2,
        shape = RoundedCornerShape(999.dp),
    ) {
        Text(
            text = text,
            color = palette.text,
            fontSize = 11.sp,
            style = TextStyle(fontFeatureSettings = TABULAR_FONT_FEATURE),
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FeaturedBentoTile(
    entry: HubModuleEntry,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalForHappyPalette.current
    val shape = RoundedCornerShape(20.dp)
    Box(modifier = modifier) {
        Surface(
            color = palette.surface,
            shape = shape,
            shadowElevation = if (!palette.isDark) 2.dp else 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .then(if (palette.cardBorder != Color.Transparent) Modifier.border(1.dp, palette.cardBorder, shape) else Modifier)
                .clip(shape)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                ),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.primaryContainer),
                ) {
                    Text(entry.emoji, fontSize = 22.sp)
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = entry.label,
                        color = palette.text,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = entry.subtitle,
                        color = palette.muted,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        if (entry.isNew) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(palette.badge)
                    .padding(horizontal = 5.dp, vertical = 1.dp),
            ) {
                Text("NEW", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PinnedGridTile(
    entry: HubModuleEntry,
    editing: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalForHappyPalette.current
    val tileShape = RoundedCornerShape(if (palette.isModernUi) 18.dp else 14.dp)
    Box(modifier = modifier) {
        Surface(
            color = palette.surface,
            shape = tileShape,
            shadowElevation = if (palette.isModernUi && !palette.isDark) 1.dp else 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (palette.isModernUi) {
                        if (palette.cardBorder != Color.Transparent) Modifier.border(1.dp, palette.cardBorder, tileShape) else Modifier
                    } else {
                        Modifier.border(1.dp, palette.line, tileShape)
                    },
                )
                .clip(tileShape)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                ),
        ) {
            Column(
                modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(if (palette.isModernUi) 13.dp else 11.dp))
                        .background(palette.surface2),
                ) {
                    Text(entry.emoji, fontSize = 20.sp)
                }
                Text(
                    text = entry.label,
                    color = palette.text,
                    fontSize = 12.sp,
                    fontWeight = if (palette.isModernUi) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (editing) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(palette.danger),
            ) {
                Icon(Icons.Rounded.Remove, contentDescription = "移除", tint = Color.White, modifier = Modifier.size(12.dp))
            }
        } else if (entry.isNew) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(palette.badge)
                    .padding(horizontal = 5.dp, vertical = 1.dp),
            ) {
                Text("NEW", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
