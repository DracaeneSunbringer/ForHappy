package com.jinhuan.forhappy.ui.disguise

import android.graphics.RectF
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Reply
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jinhuan.forhappy.BuildConfig
import com.jinhuan.forhappy.core.chat.ChatSessionController
import com.jinhuan.forhappy.core.clock.ServerClock
import com.jinhuan.forhappy.core.compose.ComposeTrayItem
import com.jinhuan.forhappy.core.model.AppDisguiseMode
import com.jinhuan.forhappy.core.model.AppIconAlias
import com.jinhuan.forhappy.core.model.AppThemeMode
import com.jinhuan.forhappy.core.model.ChatGroup
import com.jinhuan.forhappy.core.model.DayEntry
import com.jinhuan.forhappy.core.model.DmThread
import com.jinhuan.forhappy.core.model.MeUser
import com.jinhuan.forhappy.core.model.MemeComment
import com.jinhuan.forhappy.core.model.MemeItem
import com.jinhuan.forhappy.core.model.MemeMediaItem
import com.jinhuan.forhappy.core.model.PrivacyStealthConfig
import com.jinhuan.forhappy.core.model.ShortcutGestureMode
import com.jinhuan.forhappy.core.model.TagStatItem
import com.jinhuan.forhappy.core.navigation.StealthExemptionGate
import com.jinhuan.forhappy.core.network.api.ForHappyApiClient
import com.jinhuan.forhappy.core.seen.LocalSeenTracker
import com.jinhuan.forhappy.core.seen.ObserveMemeSeenDwell
import com.jinhuan.forhappy.core.seen.computeVisibleAreaRatio
import com.jinhuan.forhappy.ui.chat.ChatScreen
import com.jinhuan.forhappy.ui.components.RichChatBodyView
import com.jinhuan.forhappy.ui.compose.ComposeMediaStager
import com.jinhuan.forhappy.ui.compose.ImageMaskEditorScreen
import com.jinhuan.forhappy.ui.theme.LocalBossKeyActive
import com.jinhuan.forhappy.ui.theme.LocalForHappyPalette
import com.jinhuan.forhappy.ui.theme.TABULAR_FONT_FEATURE
import com.jinhuan.forhappy.ui.wall.ConfigureFullscreenDialogWindow
import com.jinhuan.forhappy.ui.wall.FullscreenDialogProperties
import com.jinhuan.forhappy.ui.wall.ImmersiveMediaStage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Discourse topic metadata (`docs/redesign/03-privacy-state-machine.md` §5.4 & §5.7):
 * Strictly free of forbidden giveaway keywords.
 */
internal object LinuxDoTopicMeta {
    const val TITLE = "【持续更新】日常收集的图文与素材分享楼，欢迎佬友补充"
    val TAGS = listOf("资源分享", "持续更新")
    val CATEGORIES = listOf(
        "开发调优" to Color(0xFF3AB54A),
        "资源荟萃" to Color(0xFFF7941D),
        "文档共建" to Color(0xFF9EB83B),
        "搞七捻三" to Color(0xFF25AAE2),
        "跳蚤市场" to Color(0xFFED207B),
        "运营反馈" to Color(0xFF808281),
    )
}

private data class LinuxDoTopicSpec(
    val key: String,
    val title: String,
    val categoryName: String,
    val categoryColor: Color,
    val tags: List<String>,
    val posterName: String,
    val replyCount: Int,
    val unreadCount: Int,
    val lastActiveSec: Long,
    val pinned: Boolean = false,
    val dayFilter: String? = null,
    val tagFilter: String? = null,
)

private data class LinuxDoColors(
    val bg: Color,
    val surface: Color,
    val surfaceAlt: Color,
    val line: Color,
    val accent: Color,
    val text: Color,
    val subText: Color,
    val muted: Color,
    val danger: Color,
    val likeRed: Color,
    val heatOrange: Color,
    val isDark: Boolean,
)

@Composable
private fun rememberLinuxDoColors(): LinuxDoColors {
    val isDark = LocalForHappyPalette.current.isDark
    return remember(isDark) {
        if (isDark) {
            LinuxDoColors(
                bg = Color(0xFF191B1E),
                surface = Color(0xFF222428),
                surfaceAlt = Color(0xFF2C2F36),
                line = Color(0xFF333740),
                accent = Color(0xFF1A95E0),
                text = Color(0xFFE4E6EB),
                subText = Color(0xFFA6ACB8),
                muted = Color(0xFF858C99),
                danger = Color(0xFFE45735),
                likeRed = Color(0xFFFA6C8D),
                heatOrange = Color(0xFFE08A3C),
                isDark = true,
            )
        } else {
            LinuxDoColors(
                bg = Color(0xFFF6F7F9),
                surface = Color(0xFFFFFFFF),
                surfaceAlt = Color(0xFFEFEFF1),
                line = Color(0xFFE9ECEF),
                accent = Color(0xFF0088CC),
                text = Color(0xFF222222),
                subText = Color(0xFF646464),
                muted = Color(0xFF919191),
                danger = Color(0xFFE45735),
                likeRed = Color(0xFFFA6C8D),
                heatOrange = Color(0xFFCF7721),
                isDark = false,
            )
        }
    }
}

private fun linuxDoSafeName(raw: String?): String {
    val src = raw?.trim().orEmpty()
    if (src.isBlank()) return "佬友"
    val cleaned = DisguiseSanitizer.sanitizeSummaryForDisguise(src, AppDisguiseMode.LINUX_DO)
        .replace("欢友", "佬友")
        .trim()
    return cleaned.ifBlank { "佬友" }
}

private fun linuxDoOptionalFloorText(meme: MemeItem): String? {
    val raw = listOfNotNull(meme.content, meme.text, meme.caption, meme.body, meme.title)
        .map { it.trim() }
        .firstOrNull { it.isNotBlank() }
        ?: return if (meme.mediaItems.isEmpty()) "路过补充一层，供各位佬友参考。" else null
    if (meme.mediaItems.isNotEmpty() && (
            raw.startsWith("佬友素材分享") ||
                raw.startsWith("工作同步截图") ||
                raw == "分享一张图" ||
                raw == "无题"
            )
    ) {
        return null
    }
    val cleaned = DisguiseSanitizer.sanitizeSummaryForDisguise(raw, AppDisguiseMode.LINUX_DO)
        .replace("欢友", "佬友")
        .trim()
    return cleaned.takeIf { it.isNotBlank() }
        ?: if (meme.mediaItems.isEmpty()) "路过补充一层，供各位佬友参考。" else null
}

private fun linuxDoFloorBody(meme: MemeItem): String {
    return linuxDoOptionalFloorText(meme) ?: "分享附图"
}

private val DAILY_TOPIC_TEMPLATES = listOf(
    "【日常】%s 收集的一些实用截图与开发素材汇总",
    "随手整理今日看到的图文与工具碎片（%s）",
    "【纯水】%s 佬友日常交流与素材备忘楼",
    "分享几张刚存下来的配图与技术日常记录（%s）",
    "【茶水间】%s 碎片化阅读与日常配图跟帖",
    "整理了一波近期的参考图与实用记录（%s）",
)

private val DAILY_TOPIC_TAG_POOLS = listOf(
    listOf("纯水", "日常"),
    listOf("经验分享", "工具"),
    listOf("资源分享", "汇总"),
    listOf("日常", "记录"),
)

private fun buildDailyTopicSpec(dayEntry: DayEntry): LinuxDoTopicSpec {
    val parsed = runCatching { LocalDate.parse(dayEntry.day) }.getOrNull()
    val epochDay = parsed?.toEpochDay() ?: abs(dayEntry.day.hashCode()).toLong()
    val dateShort = if (parsed != null) "${parsed.monthValue}月${parsed.dayOfMonth}日" else dayEntry.day
    val catPool = listOf(
        LinuxDoTopicMeta.CATEGORIES[3], // 搞七捻三 #25AAE2
        LinuxDoTopicMeta.CATEGORIES[1], // 资源荟萃 #F7941D
        LinuxDoTopicMeta.CATEGORIES[0], // 开发调优 #3AB54A
        LinuxDoTopicMeta.CATEGORIES[2], // 文档共建 #9EB83B
    )
    val (catName, catColor) = catPool[Math.floorMod(epochDay, catPool.size)]
    val template = DAILY_TOPIC_TEMPLATES[Math.floorMod(epochDay * 5L + 1L, DAILY_TOPIC_TEMPLATES.size)]
    val snippet = dayEntry.latest?.text
        ?.let { DisguiseSanitizer.sanitizeSummaryForDisguise(it, AppDisguiseMode.LINUX_DO).replace("欢友", "佬友").trim() }
        ?.takeIf { it.length in 6..28 && !it.startsWith("[") }
    val title = if (snippet != null) {
        "$snippet（$dateShort · 共 ${dayEntry.totalCount} 楼）"
    } else {
        String.format(template, dateShort)
    }
    val tags = DAILY_TOPIC_TAG_POOLS[Math.floorMod(epochDay * 3L, DAILY_TOPIC_TAG_POOLS.size)]
    val poster = dayEntry.latest?.by?.let { linuxDoSafeName(it) } ?: "linux_dev"
    val lastTs = dayEntry.latest?.at?.takeIf { it > 0L }
        ?: parsed?.atTime(18, 30)?.toEpochSecond(ZoneOffset.ofHours(8))
        ?: 0L
    return LinuxDoTopicSpec(
        key = "day_${dayEntry.day}",
        title = title,
        categoryName = catName,
        categoryColor = catColor,
        tags = tags,
        posterName = poster,
        replyCount = maxOf(0, dayEntry.totalCount - 1),
        unreadCount = dayEntry.unseen,
        lastActiveSec = lastTs,
        pinned = false,
        dayFilter = dayEntry.day,
        tagFilter = null,
    )
}

/**
 * Discourse compact relative age (`12m`, `3h`, `4d`, `11月15日`) matching mobile Discourse.
 */
private fun ldShortAgo(tsSec: Long?): String {
    if (tsSec == null || tsSec <= 0L) return ""
    val sec = if (tsSec > 10_000_000_000L) tsSec / 1000L else tsSec
    val diff = (ServerClock.serverNowSec() - sec).coerceAtLeast(0L)
    return when {
        diff < 60L -> "1m"
        diff < 3600L -> "${diff / 60L}m"
        diff < 86_400L -> "${diff / 3600L}h"
        diff < 86_400L * 25L -> "${diff / 86_400L}d"
        else -> FeishuDisguiseHelpers.formatConvTime(sec)
    }
}

/**
 * Authentic LINUX DO tri-band circular badge (`LD_MARK` viewBox `0 0 120 120`):
 * Outer `#F0F0F0` circle (`r=50`), clipped inner circle (`r=47`) with `#1C1C1E` top,
 * `#F0F0F0` middle, and `#FFB003` gold bottom.
 */
@Composable
private fun LinuxDoLogoMark(
    size: Dp = 26.dp,
    isDark: Boolean = false,
) {
    Canvas(modifier = Modifier.size(size)) {
        val d = this.size.minDimension
        val center = Offset(d * 0.5f, d * 0.5f)
        val outerR = d * (50f / 104f)
        val innerR = d * (46.5f / 104f)
        drawCircle(
            color = Color(0xFFF0F0F0),
            radius = outerR,
            center = center,
        )
        val innerTop = center.y - innerR
        val innerDiam = innerR * 2f
        val clip = Path().apply {
            addOval(Rect(center.x - innerR, innerTop, center.x + innerR, center.y + innerR))
        }
        clipPath(clip) {
            drawRect(
                color = Color(0xFF1C1C1E),
                topLeft = Offset(center.x - innerR, innerTop),
                size = Size(innerDiam, innerDiam * 0.31f),
            )
            drawRect(
                color = Color(0xFFF0F0F0),
                topLeft = Offset(center.x - innerR, innerTop + innerDiam * 0.31f),
                size = Size(innerDiam, innerDiam * 0.38f),
            )
            drawRect(
                color = Color(0xFFFFB003),
                topLeft = Offset(center.x - innerR, innerTop + innerDiam * 0.69f),
                size = Size(innerDiam, innerDiam * 0.32f),
            )
        }
        if (!isDark) {
            drawCircle(
                color = Color(0xFFD5D9DF),
                radius = outerR,
                center = center,
                style = Stroke(width = d * 0.035f),
            )
        }
    }
}

/**
 * Zero-giveaway 2-Level LINUX DO Mobile Discourse Disguise:
 * - Level 0 (`activeTopic == null`): Classic `linux.do` mobile Home Topic List with category squares,
 *   tag dropdowns, `最新 / 新 / 未读 / 热门` tabs, pinned megathread, and daily/tag Discourse topics.
 * - Level 1 (`activeTopic != null`): Authentic Discourse Topic Floor Stream with scroll-aware header,
 *   Lightbox image footer, boost chips, muted post controls, and bottom-right floating `X / Y` progress pill.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LinuxDoDisguiseScreen(
    api: ForHappyApiClient,
    chatController: ChatSessionController,
    me: MeUser?,
    themeMode: AppThemeMode = AppThemeMode.FOLLOW_SYSTEM,
    privacyConfig: PrivacyStealthConfig = PrivacyStealthConfig(),
    privacyShieldEnabled: Boolean = false,
    activeIconAlias: AppIconAlias = AppIconAlias.JINHUAN,
    onRequireLogin: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onToggleBlurAll: (Boolean) -> Unit,
    @Suppress("UNUSED_PARAMETER") onTriggerBossKey: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onOpenPrivacySettings: () -> Unit = {},
    onExitDisguise: () -> Unit,
    onChangeDisguise: (AppDisguiseMode) -> Unit = {},
    onChangeTheme: (AppThemeMode) -> Unit = {},
    onUpdatePrivacyConfig: (PrivacyStealthConfig) -> Unit = {},
    onTogglePrivacyShield: (Boolean) -> Unit = {},
    onChangeIconAlias: (AppIconAlias) -> Unit = {},
    onLoggedOut: () -> Unit = {},
    onOpenUrl: (String) -> Unit = {},
) {
    val lc = rememberLinuxDoColors()
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val homeListState = rememberLazyListState()
    val bossKeyActive = LocalBossKeyActive.current
    val seenTracker = LocalSeenTracker.current
    val chatState by chatController.uiState.collectAsState()

    val newlySeenByDayState = seenTracker?.newlySeenByDay?.collectAsState()
    val newlySeenByDay = newlySeenByDayState?.value ?: emptyMap()

    // Level-0 Home Topic List state
    var daysList by remember { mutableStateOf<List<DayEntry>>(emptyList()) }
    var tagsList by remember { mutableStateOf<List<TagStatItem>>(emptyList()) }
    var loadingHome by remember { mutableStateOf(true) }
    var homeNavTab by remember { mutableStateOf("latest") } // "latest" | "new" | "unread" | "top"
    var homeCategoryFilter by remember { mutableStateOf<String?>(null) }
    var homeTagFilter by remember { mutableStateOf<String?>(null) }
    var showCatMenu by remember { mutableStateOf(false) }
    var showTagMenu by remember { mutableStateOf(false) }

    // Level-1 Active Topic state (`null` = on Level-0 Discourse Home Topic List)
    var activeTopic by remember { mutableStateOf<LinuxDoTopicSpec?>(null) }

    var floors by remember { mutableStateOf<List<MemeItem>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var loadingMore by remember { mutableStateOf(false) }
    var hasMore by remember { mutableStateOf(true) }
    var currentPage by remember { mutableIntStateOf(1) }
    var unseenAnchor by remember { mutableStateOf<String?>(null) }
    var feedViewportRect by remember { mutableStateOf(RectF()) }

    var selectedTag by remember { mutableStateOf<String?>(null) }
    var unseenOnly by remember { mutableStateOf(false) }
    var sortDesc by remember { mutableStateOf(false) }
    var showSearchBar by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    var showLeftDrawer by remember { mutableStateOf(false) }
    var showChatDrawer by remember { mutableStateOf(false) }
    var showPrefsOverlay by remember { mutableStateOf(false) }
    var showUserMenu by remember { mutableStateOf(false) }
    var showTimeline by remember { mutableStateOf(false) }

    var unblurredIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    val expandedFloorIds = remember { mutableStateMapOf<Long, Boolean>() }
    val floorCommentsMap = remember { mutableStateMapOf<Long, List<MemeComment>>() }
    val floorCommentsLoading = remember { mutableStateMapOf<Long, Boolean>() }

    var showComposerSheet by remember { mutableStateOf(false) }
    var replyFloorTarget by remember { mutableStateOf<Pair<Int, MemeItem>?>(null) }
    var viewerState by remember { mutableStateOf<Triple<List<MemeMediaItem>, Int, MemeItem?>?>(null) }
    var toastMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(toastMsg) {
        if (toastMsg != null) {
            delay(2200L)
            toastMsg = null
        }
    }

    val adjustedDays = remember(daysList, newlySeenByDay) {
        if (newlySeenByDay.isEmpty()) {
            daysList
        } else {
            daysList.map { d ->
                val dec = newlySeenByDay[d.day] ?: 0
                if (dec > 0) d.copy(unseen = (d.unseen - dec).coerceAtLeast(0)) else d
            }
        }
    }

    fun reloadHomeTopics() {
        scope.launch {
            loadingHome = true
            seenTracker?.resetDayDeltaCounter()
            daysList = api.fetchDays(limitDays = 20).days
            tagsList = api.fetchTags()
            loadingHome = false
        }
    }

    fun reloadFloors(reset: Boolean = true) {
        val topic = activeTopic ?: return
        scope.launch {
            if (reset) loading = true else loadingMore = true
            if (unseenOnly) {
                seenTracker?.flushNow()
            }
            if (reset) {
                currentPage = 1
                unseenAnchor = null
            }
            val guestSnap = if (me == null && unseenOnly) {
                seenTracker?.guestSeenSnapshot().orEmpty()
            } else {
                emptyList()
            }
            val targetPage = if (reset) 1 else currentPage + 1
            val effectiveTag = selectedTag ?: topic.tagFilter
            if (!topic.dayFilter.isNullOrBlank() && effectiveTag == null && searchQuery.isBlank() && !unseenOnly) {
                val list = api.fetchMemes(
                    limit = 50,
                    day = topic.dayFilter,
                    sort = "old",
                )
                seenTracker?.ingestFetchedMemes(list)
                floors = list
                hasMore = false
                loading = false
                loadingMore = false
                return@launch
            }
            val page = api.fetchMemesPage(
                limit = 30,
                offset = if (reset) 0 else floors.size,
                page = targetPage,
                day = topic.dayFilter,
                tag = effectiveTag,
                query = searchQuery.trim().takeIf { it.isNotBlank() },
                sort = if (topic.dayFilter != null) "old" else "new",
                unseenOnly = unseenOnly,
                beforeAnchor = if (reset) null else unseenAnchor,
                guestSeenIds = guestSnap,
            )
            if (!reset) {
                currentPage = targetPage
            }
            if (unseenAnchor == null && page.anchor != null) {
                unseenAnchor = page.anchor
            }
            seenTracker?.ingestFetchedMemes(page.items)
            val incoming = if (unseenOnly) {
                page.items.filter { seenTracker?.isSeen(it.id, it.seenAt) != true }
            } else {
                page.items
            }
            floors = if (reset) incoming else (floors + incoming).distinctBy { it.id }
            hasMore = page.more && page.items.isNotEmpty()
            if (reset) loading = false else loadingMore = false
        }
    }

    LaunchedEffect(Unit) {
        chatController.refreshGroups()
        chatController.refreshDmThreads()
        reloadHomeTopics()
    }

    LaunchedEffect(activeTopic?.key, selectedTag, unseenOnly, searchQuery) {
        if (activeTopic != null) {
            listState.scrollToItem(0)
            reloadFloors(reset = true)
        }
    }

    // Re-blur media that scrolled out of viewport
    val visibleKeys by remember {
        derivedStateOf { listState.layoutInfo.visibleItemsInfo.map { it.key }.toSet() }
    }
    LaunchedEffect(visibleKeys, privacyConfig.autoReblurOnScrollOff) {
        if (privacyConfig.autoReblurOnScrollOff && unblurredIds.isNotEmpty()) {
            val kept = unblurredIds.filter { it in visibleKeys }.toSet()
            if (kept.size != unblurredIds.size) unblurredIds = kept
        }
    }

    val orderedFloors = remember(floors, sortDesc) {
        val indexed = floors.mapIndexed { idx, meme -> (idx + 1) to meme }
        if (sortDesc) indexed.reversed() else indexed
    }

    val currentFloor by remember(orderedFloors) {
        derivedStateOf {
            if (orderedFloors.isEmpty()) return@derivedStateOf 1
            val firstVisible = (listState.firstVisibleItemIndex - 1).coerceIn(0, orderedFloors.lastIndex)
            orderedFloors[firstVisible].first
        }
    }
    val scrolledPastOpTitle by remember {
        derivedStateOf {
            activeTopic != null && (listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 110)
        }
    }
    val totalFloors = maxOf(1, floors.size)
    val totalChatUnread = remember(chatState.groups, chatState.dmThreads) {
        chatState.groups.sumOf { it.unread } + chatState.dmThreads.sumOf { it.unread }
    }

    val anyOverlayOpen = showLeftDrawer || showChatDrawer || showPrefsOverlay || showTimeline
    BackHandler(enabled = anyOverlayOpen || activeTopic != null) {
        when {
            showPrefsOverlay -> showPrefsOverlay = false
            showChatDrawer -> showChatDrawer = false
            showLeftDrawer -> showLeftDrawer = false
            showTimeline -> showTimeline = false
            activeTopic != null -> {
                activeTopic = null
                selectedTag = null
                unblurredIds = emptySet()
            }
        }
    }

    val allHomeTopics = remember(adjustedDays, tagsList) {
        val totalPosts = adjustedDays.sumOf { it.totalCount }
        val totalUnseen = adjustedDays.sumOf { it.unseen }
        val latestGlobalSec = adjustedDays.firstOrNull()?.latest?.at ?: ServerClock.serverNowSec()
        val pinnedMaster = LinuxDoTopicSpec(
            key = "pinned_master",
            title = LinuxDoTopicMeta.TITLE,
            categoryName = "搞七捻三",
            categoryColor = Color(0xFF25AAE2),
            tags = LinuxDoTopicMeta.TAGS,
            posterName = "L先生",
            replyCount = maxOf(1, totalPosts),
            unreadCount = totalUnseen,
            lastActiveSec = latestGlobalSec,
            pinned = true,
            dayFilter = null,
            tagFilter = null,
        )
        val dayTopics = adjustedDays.map { buildDailyTopicSpec(it) }
        val tagTopics = tagsList.take(12).mapIndexed { idx, t ->
            val cleanTag = DisguiseSanitizer.sanitizeSummaryForDisguise(t.tag, AppDisguiseMode.LINUX_DO)
            val (catName, catColor) = LinuxDoTopicMeta.CATEGORIES[idx % 4]
            LinuxDoTopicSpec(
                key = "tag_${t.tag}",
                title = "【合集】#$cleanTag 相关技术与日常图文整理",
                categoryName = catName,
                categoryColor = catColor,
                tags = listOf(cleanTag, "合集"),
                posterName = "discourse_bot",
                replyCount = maxOf(1, t.totalCount),
                unreadCount = 0,
                lastActiveSec = latestGlobalSec - (idx + 1) * 7200L,
                pinned = false,
                dayFilter = null,
                tagFilter = t.tag,
            )
        }
        Triple(pinnedMaster, dayTopics, tagTopics)
    }

    val filteredHomeTopics = remember(allHomeTopics, homeNavTab, homeCategoryFilter, homeTagFilter, searchQuery) {
        val (pinned, dayTopics, tagTopics) = allHomeTopics
        val base = when (homeNavTab) {
            "unread" -> listOfNotNull(pinned.takeIf { it.unreadCount > 0 }) + dayTopics.filter { it.unreadCount > 0 }
            "new" -> dayTopics.take(8) + tagTopics.take(4)
            "top" -> (listOf(pinned) + dayTopics + tagTopics).sortedByDescending { it.replyCount }
            else -> listOf(pinned) + dayTopics + if (homeTagFilter != null) tagTopics else emptyList()
        }
        val q = searchQuery.trim()
        base.distinctBy { it.key }.filter { spec ->
            if (homeCategoryFilter != null && spec.categoryName != homeCategoryFilter) return@filter false
            if (homeTagFilter != null) {
                val cleanFilter = DisguiseSanitizer.sanitizeSummaryForDisguise(homeTagFilter!!, AppDisguiseMode.LINUX_DO)
                if (spec.tagFilter != homeTagFilter && spec.tags.none { it.equals(cleanFilter, ignoreCase = true) }) {
                    return@filter false
                }
            }
            if (q.isNotBlank() && !spec.title.contains(q, ignoreCase = true) && spec.tags.none { it.contains(q, ignoreCase = true) }) {
                return@filter false
            }
            true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(lc.bg),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            // Authentic Mobile Discourse Top Header (`d-header`)
            Surface(color = lc.surface, shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        if (activeTopic != null) {
                            IconButton(
                                onClick = {
                                    activeTopic = null
                                    selectedTag = null
                                    unblurredIds = emptySet()
                                },
                                modifier = Modifier.size(36.dp),
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回话题列表", tint = lc.subText, modifier = Modifier.size(21.dp))
                            }
                        }

                        if (scrolledPastOpTitle && activeTopic != null) {
                            val topTopic = activeTopic!!
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { scope.launch { listState.animateScrollToItem(0) } }
                                    .padding(end = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(
                                    text = topTopic.title,
                                    color = lc.text,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(topTopic.categoryColor),
                                    )
                                    Text(topTopic.categoryName, color = lc.subText, fontSize = 10.5.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable {
                                        if (activeTopic != null) {
                                            activeTopic = null
                                            selectedTag = null
                                        } else {
                                            scope.launch { homeListState.animateScrollToItem(0) }
                                        }
                                    }
                                    .padding(horizontal = 4.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                LinuxDoLogoMark(size = 27.dp, isDark = lc.isDark)
                                Text(
                                    text = "LINUX DO",
                                    color = lc.text,
                                    fontSize = 16.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.4.sp,
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                showSearchBar = !showSearchBar
                                if (!showSearchBar) searchQuery = ""
                            },
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = "搜索",
                                tint = if (showSearchBar) lc.accent else lc.subText,
                                modifier = Modifier.size(21.dp),
                            )
                        }
                        Box(contentAlignment = Alignment.TopEnd) {
                            IconButton(onClick = { showChatDrawer = true }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Rounded.ChatBubbleOutline, contentDescription = "聊天频道", tint = lc.subText, modifier = Modifier.size(20.dp))
                            }
                            if (totalChatUnread > 0) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 6.dp, end = 6.dp)
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(lc.accent),
                                )
                            }
                        }
                        IconButton(onClick = { showLeftDrawer = !showLeftDrawer }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Rounded.Menu, contentDescription = "侧栏菜单", tint = lc.subText, modifier = Modifier.size(22.dp))
                        }
                        Box {
                            val avatarChar = linuxDoSafeName(me?.displayName).take(1)
                            Box(
                                modifier = Modifier
                                    .padding(start = 2.dp, end = 4.dp)
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(FeishuDisguiseHelpers.imColor(avatarChar))
                                    .clickable { showUserMenu = true },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(avatarChar, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            DropdownMenu(
                                expanded = showUserMenu,
                                onDismissRequest = { showUserMenu = false },
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = if (me != null) linuxDoSafeName(me.displayName) else "登录 LINUX DO 账号",
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                    },
                                    onClick = {
                                        showUserMenu = false
                                        if (me == null) onRequireLogin() else showPrefsOverlay = true
                                    },
                                )
                                HorizontalDivider(color = lc.line)
                                DropdownMenuItem(
                                    text = { Text("偏好设置 › 界面与隐私") },
                                    onClick = {
                                        showUserMenu = false
                                        showPrefsOverlay = true
                                    },
                                )
                            }
                        }
                    }

                    if (showSearchBar) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .height(36.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(lc.surfaceAlt)
                                .border(1.dp, lc.line, RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(Icons.Rounded.Search, contentDescription = null, tint = lc.muted, modifier = Modifier.size(16.dp))
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = if (activeTopic == null) "搜索话题、标签或帖子…" else "在本话题楼层中搜索…",
                                        color = lc.muted,
                                        fontSize = 13.sp,
                                    )
                                }
                                BasicTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    singleLine = true,
                                    textStyle = TextStyle(color = lc.text, fontSize = 13.sp),
                                    cursorBrush = SolidColor(lc.accent),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                            if (searchQuery.isNotEmpty()) {
                                Icon(
                                    Icons.Rounded.Close,
                                    contentDescription = "清除",
                                    tint = lc.muted,
                                    modifier = Modifier.size(16.dp).clickable { searchQuery = "" },
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = lc.line)
                }
            }

            if (activeTopic == null) {
                // ==================== LEVEL 0: AUTHENTIC DISCOURSE HOME TOPIC LIST ====================
                val totalUnseenTopics = remember(adjustedDays) { adjustedDays.count { it.unseen > 0 } }
                LazyColumn(
                    state = homeListState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(lc.surface)
                        .navigationBarsPadding(),
                ) {
                    item(key = "discourse_nav_controls") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(lc.bg)
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            // Row 1: Category Breadcrumb + Tag Breadcrumb + `+ 新建话题`
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Box {
                                    Surface(
                                        color = lc.surface,
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, lc.line),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable { showCatMenu = true },
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                                        ) {
                                            homeCategoryFilter?.let { cat ->
                                                val sqColor = LinuxDoTopicMeta.CATEGORIES.firstOrNull { it.first == cat }?.second ?: lc.accent
                                                Box(modifier = Modifier.size(8.dp).background(sqColor))
                                            }
                                            Text(
                                                text = homeCategoryFilter ?: "所有类别",
                                                color = lc.text,
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Medium,
                                            )
                                            Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null, tint = lc.muted, modifier = Modifier.size(15.dp))
                                        }
                                    }
                                    DropdownMenu(
                                        expanded = showCatMenu,
                                        onDismissRequest = { showCatMenu = false },
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("所有类别") },
                                            onClick = {
                                                homeCategoryFilter = null
                                                showCatMenu = false
                                            },
                                        )
                                        LinuxDoTopicMeta.CATEGORIES.forEach { (catName, catColor) ->
                                            DropdownMenuItem(
                                                leadingIcon = {
                                                    Box(modifier = Modifier.size(10.dp).background(catColor))
                                                },
                                                text = { Text(catName) },
                                                onClick = {
                                                    homeCategoryFilter = catName
                                                    showCatMenu = false
                                                },
                                            )
                                        }
                                    }
                                }

                                Box {
                                    val activeTagClean = homeTagFilter?.let {
                                        DisguiseSanitizer.sanitizeSummaryForDisguise(it, AppDisguiseMode.LINUX_DO)
                                    }
                                    Surface(
                                        color = lc.surface,
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, lc.line),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable { showTagMenu = true },
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        ) {
                                            Text(
                                                text = activeTagClean ?: "所有标签",
                                                color = lc.text,
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                            )
                                            Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null, tint = lc.muted, modifier = Modifier.size(15.dp))
                                        }
                                    }
                                    DropdownMenu(
                                        expanded = showTagMenu,
                                        onDismissRequest = { showTagMenu = false },
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("所有标签") },
                                            onClick = {
                                                homeTagFilter = null
                                                showTagMenu = false
                                            },
                                        )
                                        tagsList.take(15).forEach { tagItem ->
                                            val cleanLabel = DisguiseSanitizer.sanitizeSummaryForDisguise(tagItem.tag, AppDisguiseMode.LINUX_DO)
                                            DropdownMenuItem(
                                                text = { Text("$cleanLabel (${tagItem.totalCount})") },
                                                onClick = {
                                                    homeTagFilter = tagItem.tag
                                                    showTagMenu = false
                                                },
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.weight(1f))

                                Surface(
                                    color = lc.surfaceAlt,
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable {
                                            replyFloorTarget = null
                                            showComposerSheet = true
                                        },
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    ) {
                                        Icon(Icons.Rounded.Add, contentDescription = null, tint = lc.text, modifier = Modifier.size(15.dp))
                                        Text("新建话题", color = lc.text, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }

                            // Row 2: Discourse Navigation Pills (`最新 / 新 / 未读 / 热门`)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                val navTabs = listOf(
                                    "latest" to "最新",
                                    "new" to "新",
                                    "unread" to if (totalUnseenTopics > 0) "未读 ($totalUnseenTopics)" else "未读",
                                    "top" to "热门",
                                )
                                navTabs.forEach { (id, label) ->
                                    val active = homeNavTab == id
                                    Surface(
                                        color = if (active) lc.accent.copy(alpha = 0.14f) else Color.Transparent,
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable { homeNavTab = id },
                                    ) {
                                        Text(
                                            text = label,
                                            color = if (active) lc.accent else lc.subText,
                                            fontSize = 13.5.sp,
                                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
                                        )
                                    }
                                }
                            }
                        }
                        HorizontalDivider(color = lc.line)
                    }

                    if (loadingHome && daysList.isEmpty()) {
                        item(key = "home_loading") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(40.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(color = lc.accent, strokeWidth = 2.dp, modifier = Modifier.size(26.dp))
                            }
                        }
                    } else if (filteredHomeTopics.isEmpty()) {
                        item(key = "home_empty") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(40.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("没有更多未读或匹配的话题", color = lc.muted, fontSize = 13.sp)
                            }
                        }
                    } else {
                        items(filteredHomeTopics, key = { it.key }) { topic ->
                            val posterColor = remember(topic.posterName) { FeishuDisguiseHelpers.imColor(topic.posterName) }
                            val isFullyRead = !topic.pinned && topic.unreadCount == 0 && topic.dayFilter != null
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (topic.pinned) lc.accent.copy(alpha = 0.03f) else lc.surface)
                                    .clickable {
                                        selectedTag = null
                                        unseenOnly = homeNavTab == "unread" && topic.unreadCount > 0
                                        activeTopic = topic
                                    }
                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(11.dp),
                            ) {
                                // Left circular poster avatar (`mobile Discourse topic-list-item`)
                                Box(
                                    modifier = Modifier
                                        .padding(top = 2.dp)
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(posterColor),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = topic.posterName.take(1).uppercase(),
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }

                                // Center Title + Category Square + Tag Pills
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    ) {
                                        if (topic.pinned) {
                                            Icon(
                                                imageVector = Icons.Rounded.PushPin,
                                                contentDescription = "置顶话题",
                                                tint = lc.subText,
                                                modifier = Modifier.size(14.dp),
                                            )
                                        }
                                        Text(
                                            text = topic.title,
                                            color = if (isFullyRead) lc.subText else lc.text,
                                            fontSize = 15.sp,
                                            lineHeight = 20.sp,
                                            fontWeight = if (topic.unreadCount > 0 || topic.pinned) FontWeight.SemiBold else FontWeight.Normal,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }

                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(9.dp)
                                                    .background(topic.categoryColor),
                                            )
                                            Text(
                                                text = topic.categoryName,
                                                color = lc.subText,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                            )
                                        }
                                        topic.tags.forEach { tag ->
                                            Surface(
                                                color = lc.surfaceAlt,
                                                shape = RoundedCornerShape(3.dp),
                                            ) {
                                                Text(
                                                    text = tag,
                                                    color = lc.subText,
                                                    fontSize = 10.5.sp,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                                )
                                            }
                                        }
                                    }
                                }

                                // Right stacked Reply Count / Unread Pill + Relative Age (`12m` / `3h`)
                                Column(
                                    horizontalAlignment = Alignment.End,
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    if (topic.unreadCount > 0) {
                                        Surface(
                                            color = lc.accent,
                                            shape = RoundedCornerShape(999.dp),
                                        ) {
                                            Text(
                                                text = if (topic.unreadCount > 99) "99+" else "${topic.unreadCount}",
                                                color = Color.White,
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                style = TextStyle(fontFeatureSettings = TABULAR_FONT_FEATURE),
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 1.5.dp),
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = "${topic.replyCount}",
                                            color = if (topic.replyCount >= 50) lc.heatOrange else lc.muted,
                                            fontSize = 13.sp,
                                            fontWeight = if (topic.replyCount >= 50) FontWeight.Bold else FontWeight.SemiBold,
                                            style = TextStyle(fontFeatureSettings = TABULAR_FONT_FEATURE),
                                        )
                                    }
                                    Text(
                                        text = ldShortAgo(topic.lastActiveSec),
                                        color = lc.muted,
                                        fontSize = 11.5.sp,
                                    )
                                }
                            }
                            HorizontalDivider(color = lc.line)
                        }
                    }
                }
            } else {
                // ==================== LEVEL 1: AUTHENTIC DISCOURSE TOPIC FLOOR STREAM ====================
                val currentTopic = activeTopic!!
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(bottom = 84.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(lc.surface)
                        .onGloballyPositioned { coords ->
                            val b = coords.boundsInWindow()
                            feedViewportRect = RectF(b.left, b.top, b.right, b.bottom)
                        },
                ) {
                    item(key = "op_header") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(lc.surface)
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(9.dp),
                        ) {
                            Text(
                                text = currentTopic.title,
                                color = lc.text,
                                fontSize = 19.sp,
                                lineHeight = 25.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(currentTopic.categoryColor),
                                    )
                                    Text(
                                        text = currentTopic.categoryName,
                                        color = lc.subText,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                                currentTopic.tags.forEach { tag ->
                                    Surface(color = lc.surfaceAlt, shape = RoundedCornerShape(3.dp)) {
                                        Text(
                                            text = tag,
                                            color = lc.subText,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        )
                                    }
                                }
                                if (!selectedTag.isNullOrBlank()) {
                                    Surface(
                                        color = lc.accent,
                                        shape = RoundedCornerShape(3.dp),
                                        modifier = Modifier.clickable { selectedTag = null },
                                    ) {
                                        Text(
                                            text = "${DisguiseSanitizer.sanitizeSummaryForDisguise(selectedTag!!, AppDisguiseMode.LINUX_DO)} ✕",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        )
                                    }
                                }
                            }
                            // Discourse Topic Map / Stats strip
                            Surface(
                                color = lc.bg,
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(0.5.dp, lc.line),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                        Column {
                                            Text("$totalFloors", color = lc.text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            Text("帖子", color = lc.muted, fontSize = 10.sp)
                                        }
                                        Column {
                                            Text("${totalFloors * 13 + 42}", color = lc.text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            Text("浏览", color = lc.muted, fontSize = 10.sp)
                                        }
                                        Column {
                                            Text("${maxOf(2, totalFloors / 3 + 2)}", color = lc.text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            Text("参与者", color = lc.muted, fontSize = 10.sp)
                                        }
                                    }
                                    Surface(
                                        color = if (unseenOnly) lc.accent else lc.surfaceAlt,
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .clickable { unseenOnly = !unseenOnly },
                                    ) {
                                        Text(
                                            text = if (unseenOnly) "未读楼层 ✓" else "只看未读",
                                            color = if (unseenOnly) Color.White else lc.subText,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        )
                                    }
                                }
                            }
                        }
                        HorizontalDivider(color = lc.line)
                    }

                    if (loading && orderedFloors.isEmpty()) {
                        item(key = "loading_floors") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(40.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(color = lc.accent, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                            }
                        }
                    } else if (orderedFloors.isEmpty()) {
                        item(key = "empty_floors") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(40.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = if (unseenOnly) "该话题未读楼层已全部读完" else "当前筛选暂无匹配楼层",
                                    color = lc.muted,
                                    fontSize = 13.sp,
                                )
                            }
                        }
                    }

                    itemsIndexed(orderedFloors, key = { _, (_, meme) -> meme.id }) { idx, (floorNum, meme) ->
                        val prevTs = orderedFloors.getOrNull(idx - 1)?.second?.createdAt ?: 0L
                        val curTs = meme.createdAt ?: 0L
                        val gapDays = if (idx > 0 && prevTs > 0L && curTs > 0L) {
                            abs(curTs - prevTs) / 86_400L
                        } else {
                            0L
                        }

                        val author = linuxDoSafeName(meme.authorName ?: meme.authorUsername)
                        val avatarBg = FeishuDisguiseHelpers.imColor(author)
                        val trustLevel = remember(author) { 1 + Math.floorMod(author.hashCode(), 3) }
                        val likeCount = maxOf(meme.likes, meme.likeCount, meme.ldLikes)
                        val commentCount = maxOf(meme.comments, meme.commentCount)
                        val boosts = meme.replyList.ifEmpty { meme.replies }
                        val optionalBody = remember(meme) { linuxDoOptionalFloorText(meme) }
                        val commentsExpanded = expandedFloorIds[meme.id] == true
                        val loadedComments = floorCommentsMap[meme.id].orEmpty()
                        val commentsBusy = floorCommentsLoading[meme.id] == true
                        val hasMedia = remember(meme.id, meme.images, meme.image, meme.thumb, meme.media) { meme.mediaItems.isNotEmpty() }
                        var mediaReady by remember(meme.id, hasMedia) { mutableStateOf(!hasMedia) }
                        var itemTopPx by remember(meme.id) { mutableFloatStateOf(0f) }
                        var itemBottomPx by remember(meme.id) { mutableFloatStateOf(0f) }
                        val visibleRatio = computeVisibleAreaRatio(itemTopPx, itemBottomPx, feedViewportRect.top, feedViewportRect.bottom)
                        val isItemBlurred = hasMedia && privacyConfig.blurAllEnabled && meme.id !in unblurredIds

                        if (!bossKeyActive) {
                            ObserveMemeSeenDwell(
                                memeId = meme.id,
                                seenAt = meme.seenAt,
                                memeDay = meme.resolvedDay,
                                isMediaReady = mediaReady,
                                isBlurred = isItemBlurred,
                                visibleRatio = visibleRatio,
                                scrollSpeedDpPerSec = 0f,
                            )
                        }

                        if (gapDays >= 5L) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(lc.bg)
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                HorizontalDivider(color = lc.line, modifier = Modifier.weight(1f))
                                Text(
                                    text = if (gapDays >= 30L) "${gapDays / 30L} 个月后" else "$gapDays 天后",
                                    color = lc.muted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                                HorizontalDivider(color = lc.line, modifier = Modifier.weight(1f))
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { coords ->
                                    val b = coords.boundsInWindow()
                                    itemTopPx = b.top
                                    itemBottomPx = b.bottom
                                }
                                .padding(horizontal = 15.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(9.dp),
                        ) {
                            // Discourse Post Header (`avatar + username + TL badge ... relative time + #floor`)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(avatarBg),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = author.take(1).uppercase(),
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Text(
                                        text = author,
                                        color = lc.subText,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = "TL$trustLevel",
                                        color = lc.muted,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Medium,
                                    )
                                }
                                Text(
                                    text = ldShortAgo(meme.createdAt),
                                    color = lc.muted,
                                    fontSize = 12.sp,
                                )
                                Text(
                                    text = "#$floorNum",
                                    color = lc.muted.copy(alpha = 0.75f),
                                    fontSize = 11.5.sp,
                                    style = TextStyle(fontFeatureSettings = TABULAR_FONT_FEATURE),
                                )
                            }

                            if (!optionalBody.isNullOrBlank()) {
                                RichChatBodyView(
                                    rawBody = optionalBody,
                                    imageLoader = api.imageLoader,
                                    textColor = lc.text,
                                    showInlineImages = false,
                                )
                            }

                            DisguiseMediaStrip(
                                meme = meme,
                                api = api,
                                blurAll = privacyConfig.blurAllEnabled,
                                accentColor = lc.accent,
                                isItemUnblurred = meme.id in unblurredIds,
                                onRevealItem = { unblurredIds = unblurredIds + meme.id },
                                onOpenViewer = { list, idx -> viewerState = Triple(list, idx, meme) },
                                onMediaLoaded = { mediaReady = true },
                                discourseLightbox = true,
                            )

                            if (meme.tags.isNotEmpty()) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    meme.tags.take(4).forEach { tag ->
                                        Surface(
                                            color = lc.surfaceAlt,
                                            shape = RoundedCornerShape(3.dp),
                                            modifier = Modifier.clickable { selectedTag = tag },
                                        ) {
                                            Text(
                                                text = DisguiseSanitizer.sanitizeSummaryForDisguise(tag, AppDisguiseMode.LINUX_DO),
                                                color = lc.subText,
                                                fontSize = 10.5.sp,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            )
                                        }
                                    }
                                }
                            }

                            // Discourse Boosts (`一句回应` / `助推`)
                            if (boosts.isNotEmpty()) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(5.dp),
                                ) {
                                    boosts.forEach { reply ->
                                        val cleanBoost = DisguiseSanitizer.sanitizeSummaryForDisguise(reply.text, AppDisguiseMode.LINUX_DO)
                                            .replace("欢友", "佬友")
                                        Surface(
                                            color = lc.surfaceAlt,
                                            shape = RoundedCornerShape(999.dp),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(999.dp))
                                                .clickable {
                                                    scope.launch {
                                                        api.putMemeQuickReply(meme.id, reply.text).onSuccess { updated ->
                                                            if (updated != null) {
                                                                floors = floors.map { if (it.id == updated.id) updated else it }
                                                            }
                                                        }
                                                    }
                                                },
                                        ) {
                                            Text(
                                                text = "「$cleanBoost」" + if (reply.n > 1) " ×${reply.n}" else "",
                                                color = lc.subText,
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            )
                                        }
                                    }
                                }
                            }

                            // Authentic Discourse Post Controls (`ldControls`)
                            val activeReactions = remember(meme.reactions) { meme.reactions.filter { it.count > 0 } }
                            val rxTotal = remember(activeReactions) { activeReactions.sumOf { it.count } }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (commentCount > 0 || commentsExpanded) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .clickable {
                                                val next = !commentsExpanded
                                                expandedFloorIds[meme.id] = next
                                                if (next && floorCommentsMap[meme.id] == null) {
                                                    floorCommentsLoading[meme.id] = true
                                                    scope.launch {
                                                        floorCommentsMap[meme.id] = api.fetchMemeComments(meme.id)
                                                        floorCommentsLoading[meme.id] = false
                                                    }
                                                }
                                            }
                                            .padding(vertical = 4.dp, horizontal = 4.dp),
                                    ) {
                                        Text(
                                            text = "${maxOf(commentCount, loadedComments.size)} 条回复",
                                            color = lc.subText,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Medium,
                                        )
                                        Icon(
                                            imageVector = if (commentsExpanded) Icons.Rounded.KeyboardArrowDown else Icons.Rounded.KeyboardArrowRight,
                                            contentDescription = null,
                                            tint = lc.muted,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                } else {
                                    Spacer(Modifier.width(4.dp))
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                ) {
                                    if (rxTotal > 0) {
                                        val topRx = activeReactions.first()
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(999.dp))
                                                .background(lc.surfaceAlt)
                                                .clickable {
                                                    scope.launch {
                                                        val updated = api.reactToMeme(meme.id, topRx.emoji)
                                                        if (updated != null) {
                                                            floors = floors.map { if (it.id == updated.id) updated else it }
                                                        }
                                                    }
                                                }
                                                .padding(horizontal = 7.dp, vertical = 2.dp),
                                        ) {
                                            activeReactions.take(3).forEach { r ->
                                                Text(DisguiseSanitizer.mapStickerToSafeEmoji(r.emoji), fontSize = 12.sp)
                                            }
                                            Text("$rxTotal", color = lc.subText, fontSize = 11.sp)
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                                        modifier = Modifier.clickable {
                                            scope.launch {
                                                val obj = api.toggleLike(meme.id)
                                                if (obj != null) {
                                                    val delta = if (meme.liked) -1 else 1
                                                    floors = floors.map {
                                                        if (it.id == meme.id) {
                                                            it.copy(
                                                                liked = !it.liked,
                                                                likes = maxOf(0, likeCount + delta),
                                                            )
                                                        } else it
                                                    }
                                                }
                                            }
                                        },
                                    ) {
                                        if (likeCount > 0) {
                                            Text(
                                                text = "$likeCount",
                                                color = if (meme.liked) lc.likeRed else lc.muted,
                                                fontSize = 12.5.sp,
                                            )
                                        }
                                        Icon(
                                            imageVector = if (meme.liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                            contentDescription = "点赞",
                                            tint = if (meme.liked) lc.likeRed else lc.muted,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }

                                    Icon(
                                        imageVector = Icons.Rounded.Link,
                                        contentDescription = "复制楼层链接",
                                        tint = lc.muted,
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clickable {
                                                clipboard.setText(AnnotatedString("#$floorNum ${linuxDoFloorBody(meme)}"))
                                                toastMsg = "已复制 #$floorNum 楼链接"
                                            },
                                    )

                                    Icon(
                                        imageVector = if (meme.faved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                                        contentDescription = "书签",
                                        tint = if (meme.faved) lc.accent else lc.muted,
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clickable {
                                                scope.launch {
                                                    if (api.toggleFav(meme.id, meme.faved)) {
                                                        floors = floors.map {
                                                            if (it.id == meme.id) it.copy(faved = !meme.faved) else it
                                                        }
                                                    }
                                                }
                                            },
                                    )

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .clickable {
                                                replyFloorTarget = floorNum to meme
                                                showComposerSheet = true
                                            }
                                            .padding(horizontal = 4.dp, vertical = 2.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.Reply,
                                            contentDescription = "回复此楼",
                                            tint = lc.subText,
                                            modifier = Modifier.size(17.dp),
                                        )
                                        Text("回复", color = lc.subText, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }

                            if (commentsExpanded) {
                                Surface(
                                    color = lc.bg,
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(0.5.dp, lc.line),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        if (commentsBusy) {
                                            Text("正在展开回复…", color = lc.muted, fontSize = 12.sp)
                                        } else if (loadedComments.isEmpty()) {
                                            Text("暂无内嵌回复", color = lc.muted, fontSize = 12.sp)
                                        } else {
                                            loadedComments.forEachIndexed { cmIdx, cm ->
                                                if (cmIdx > 0) HorizontalDivider(color = lc.line.copy(alpha = 0.7f))
                                                val cmName = linuxDoSafeName(cm.displayAuthor)
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .padding(top = 2.dp)
                                                            .size(24.dp)
                                                            .clip(CircleShape)
                                                            .background(FeishuDisguiseHelpers.imColor(cmName)),
                                                        contentAlignment = Alignment.Center,
                                                    ) {
                                                        Text(cmName.take(1).uppercase(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                    Column(
                                                        modifier = Modifier.weight(1f),
                                                        verticalArrangement = Arrangement.spacedBy(2.dp),
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                        ) {
                                                            Text(cmName, color = lc.subText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                            Text(ldShortAgo(cm.createdAt), color = lc.muted, fontSize = 10.5.sp)
                                                        }
                                                        RichChatBodyView(
                                                            rawBody = DisguiseSanitizer.sanitizeSummaryForDisguise(cm.body, AppDisguiseMode.LINUX_DO)
                                                                .replace("欢友", "佬友"),
                                                            imageLoader = api.imageLoader,
                                                            textColor = lc.text,
                                                            showInlineImages = false,
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        HorizontalDivider(color = lc.line)
                    }

                    if (hasMore && floors.isNotEmpty()) {
                        item(key = "load_more") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !loadingMore) { reloadFloors(reset = false) }
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = if (loadingMore) "加载后续楼层中…" else "加载更多楼层",
                                    color = lc.accent,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    }
                }
            }
        }

        // Authentic Mobile Discourse Bottom-Right Floating Topic Progress & Reply Bar (24dp safe edge margin)
        if (activeTopic != null && !anyOverlayOpen) {
            val progressRatio = remember(currentFloor, totalFloors) {
                if (totalFloors <= 1) 1f else (currentFloor.toFloat() / totalFloors.toFloat()).coerceIn(0.08f, 1f)
            }
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 24.dp, bottom = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Surface(
                    color = lc.surface,
                    shape = RoundedCornerShape(6.dp),
                    shadowElevation = 4.dp,
                    border = BorderStroke(1.dp, lc.accent.copy(alpha = 0.38f)),
                    modifier = Modifier
                        .height(36.dp)
                        .width(92.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { showTimeline = true },
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth(progressRatio)
                                .height(3.dp)
                                .background(lc.accent),
                        )
                        Text(
                            text = "$currentFloor / $totalFloors",
                            color = lc.accent,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            style = TextStyle(fontFeatureSettings = TABULAR_FONT_FEATURE),
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                }

                Surface(
                    color = lc.accent,
                    shape = RoundedCornerShape(6.dp),
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            replyFloorTarget = null
                            showComposerSheet = true
                        },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Reply, contentDescription = "回复话题", tint = Color.White, modifier = Modifier.size(16.dp))
                        Text("回复", color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Right-Edge-Safe 64dp Vertical Discourse Timeline (`padding(end = 24.dp)`)
        if (showTimeline) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { showTimeline = false },
            ) {
                var trackHeightPx by remember { mutableIntStateOf(1) }
                val progress = if (totalFloors <= 1) 0f else ((currentFloor - 1).toFloat() / (totalFloors - 1).toFloat()).coerceIn(0f, 1f)

                fun jumpToRatio(ratio: Float) {
                    if (orderedFloors.isEmpty()) return
                    val idx = (ratio.coerceIn(0f, 1f) * orderedFloors.lastIndex).roundToInt()
                    scope.launch { listState.scrollToItem(idx + 1) }
                }

                Surface(
                    color = lc.surface,
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 8.dp,
                    border = BorderStroke(1.dp, lc.line),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 24.dp)
                        .width(68.dp)
                        .height(310.dp)
                        .clickable(enabled = false) {},
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 10.dp, horizontal = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { jumpToRatio(0f) }
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                        ) {
                            Text("原帖", color = lc.accent, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            Text("#1", color = lc.muted, fontSize = 10.sp)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .onSizeChanged { trackHeightPx = maxOf(1, it.height) }
                                .pointerInput(totalFloors, orderedFloors.size) {
                                    detectVerticalDragGestures { change, _ ->
                                        change.consume()
                                        jumpToRatio(change.position.y / trackHeightPx.toFloat())
                                    }
                                },
                            contentAlignment = Alignment.TopCenter,
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(lc.accent.copy(alpha = 0.25f)),
                            )
                            val thumbY = (progress * (trackHeightPx - 36).coerceAtLeast(1)).roundToInt()
                            Box(
                                modifier = Modifier
                                    .offset { IntOffset(0, thumbY) }
                                    .width(34.dp)
                                    .height(22.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(lc.accent),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "$currentFloor",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    style = TextStyle(fontFeatureSettings = TABULAR_FONT_FEATURE),
                                )
                            }
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { jumpToRatio(1f) }
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                        ) {
                            Text("最新", color = lc.accent, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            Text("#$totalFloors", color = lc.muted, fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        // Left Discourse Sidebar Drawer (`话题 / 类别 / 标签 / 显示选项`)
        if (showLeftDrawer) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.38f))
                    .clickable { showLeftDrawer = false },
            ) {
                Surface(
                    color = lc.surface,
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(284.dp)
                        .clickable(enabled = false) {},
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .navigationBarsPadding()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                LinuxDoLogoMark(size = 22.dp, isDark = lc.isDark)
                                Text("LINUX DO", color = lc.text, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                            }
                            IconButton(onClick = { showLeftDrawer = false }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Rounded.Close, contentDescription = "关闭", tint = lc.muted)
                            }
                        }
                        HorizontalDivider(color = lc.line)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (activeTopic == null) lc.accent.copy(alpha = 0.10f) else Color.Transparent)
                                .clickable {
                                    activeTopic = null
                                    homeCategoryFilter = null
                                    showLeftDrawer = false
                                }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(Icons.Rounded.Forum, contentDescription = null, tint = if (activeTopic == null) lc.accent else lc.subText, modifier = Modifier.size(17.dp))
                            Text("全部话题", color = if (activeTopic == null) lc.accent else lc.text, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Text("类别", color = lc.muted, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        LinuxDoTopicMeta.CATEGORIES.forEach { (catName, catColor) ->
                            val selected = activeTopic == null && homeCategoryFilter == catName
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (selected) lc.accent.copy(alpha = 0.10f) else Color.Transparent)
                                    .clickable {
                                        activeTopic = null
                                        homeCategoryFilter = catName
                                        showLeftDrawer = false
                                    }
                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(9.dp),
                            ) {
                                Box(modifier = Modifier.size(10.dp).background(catColor))
                                Text(
                                    text = catName,
                                    color = if (selected) lc.accent else lc.text,
                                    fontSize = 13.5.sp,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                )
                            }
                        }

                        HorizontalDivider(color = lc.line)
                        Text("阅读与隐私选项", color = lc.muted, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        LinuxDoSwitchRow(
                            title = "仅看未读楼层",
                            checked = unseenOnly,
                            lc = lc,
                            onCheckedChange = { unseenOnly = it },
                        )
                        LinuxDoSwitchRow(
                            title = "最新楼层优先（倒序）",
                            checked = sortDesc,
                            lc = lc,
                            onCheckedChange = { sortDesc = it },
                        )
                        LinuxDoSwitchRow(
                            title = "图片默认保护遮罩",
                            checked = privacyConfig.blurAllEnabled,
                            lc = lc,
                            onCheckedChange = {
                                onUpdatePrivacyConfig(privacyConfig.copy(blurAllEnabled = it))
                            },
                        )

                        if (tagsList.isNotEmpty()) {
                            HorizontalDivider(color = lc.line)
                            Text("热门标签", color = lc.muted, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                tagsList.take(16).forEach { t ->
                                    val cleanLabel = DisguiseSanitizer.sanitizeSummaryForDisguise(t.tag, AppDisguiseMode.LINUX_DO)
                                    FilterChip(
                                        selected = (activeTopic != null && selectedTag == t.tag) || (activeTopic == null && homeTagFilter == t.tag),
                                        onClick = {
                                            if (activeTopic != null) {
                                                selectedTag = if (selectedTag == t.tag) null else t.tag
                                            } else {
                                                homeTagFilter = if (homeTagFilter == t.tag) null else t.tag
                                            }
                                            showLeftDrawer = false
                                        },
                                        label = { Text("$cleanLabel (${t.totalCount})", fontSize = 11.5.sp) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Discourse Chat Drawer (`# 公开频道` & `私信`)
        if (showChatDrawer && !bossKeyActive) {
            LinuxDoChatDrawerOverlay(
                api = api,
                chatController = chatController,
                me = me,
                groups = chatState.groups,
                dmThreads = chatState.dmThreads,
                lc = lc,
                onRequireLogin = onRequireLogin,
                onOpenUrl = onOpenUrl,
                onClose = { showChatDrawer = false },
            )
        }

        // Discourse User Preferences Overlay (`界面 / 隐私 / 账号`)
        if (showPrefsOverlay) {
            LinuxDoPreferencesOverlay(
                api = api,
                me = me,
                themeMode = themeMode,
                privacyConfig = privacyConfig,
                privacyShieldEnabled = privacyShieldEnabled,
                activeIconAlias = activeIconAlias,
                lc = lc,
                onChangeTheme = onChangeTheme,
                onExitDisguise = onExitDisguise,
                onChangeDisguise = onChangeDisguise,
                onUpdatePrivacyConfig = onUpdatePrivacyConfig,
                onTogglePrivacyShield = onTogglePrivacyShield,
                onChangeIconAlias = onChangeIconAlias,
                onRequireLogin = onRequireLogin,
                onLoggedOut = {
                    onLoggedOut()
                    showPrefsOverlay = false
                    toastMsg = "已退出社区账号"
                },
                onClose = { showPrefsOverlay = false },
            )
        }

        if (!toastMsg.isNullOrBlank()) {
            Surface(
                color = Color(0xFF1C1E21).copy(alpha = 0.88f),
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 68.dp),
            ) {
                Text(
                    text = toastMsg.orEmpty(),
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                )
            }
        }
    }

    if (showComposerSheet && !bossKeyActive) {
        LinuxDoComposerSheet(
            api = api,
            replyFloorTarget = replyFloorTarget,
            selectedTag = selectedTag ?: activeTopic?.tagFilter ?: homeTagFilter,
            lc = lc,
            onPublishedFloor = {
                showComposerSheet = false
                replyFloorTarget = null
                if (activeTopic != null) {
                    reloadFloors(reset = true)
                } else {
                    reloadHomeTopics()
                }
                toastMsg = if (activeTopic != null) "新楼层已发布" else "新话题内容已发布"
            },
            onRepliedToFloor = { targetId ->
                showComposerSheet = false
                replyFloorTarget = null
                expandedFloorIds[targetId] = true
                scope.launch {
                    floorCommentsMap[targetId] = api.fetchMemeComments(targetId)
                }
                floors = floors.map { m ->
                    if (m.id == targetId) m.copy(comments = m.comments + 1, commentCount = m.commentCount + 1) else m
                }
                toastMsg = "楼层回复已提交"
            },
            onNotify = { toastMsg = it },
            onDismiss = {
                showComposerSheet = false
                replyFloorTarget = null
            },
        )
    }

    viewerState?.let { (mediaItems, idx, ownerMeme) ->
        ImmersiveMediaStage(
            mediaItems = mediaItems,
            initialIndex = idx,
            api = api,
            meme = ownerMeme,
            forceBlurAll = false,
            onDismiss = { viewerState = null },
        )
    }
}

@Composable
private fun LinuxDoChatDrawerOverlay(
    api: ForHappyApiClient,
    chatController: ChatSessionController,
    me: MeUser?,
    groups: List<ChatGroup>,
    dmThreads: List<DmThread>,
    lc: LinuxDoColors,
    onRequireLogin: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onClose: () -> Unit,
) {
    var activeIsGroup by remember { mutableStateOf(true) }
    var activeGroupId by remember(groups) { mutableStateOf(groups.firstOrNull()?.id ?: 1L) }
    var activeDmId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(activeIsGroup, activeGroupId) {
        if (activeIsGroup) chatController.setActiveGroup(activeGroupId)
    }

    BackHandler(onBack = onClose)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(lc.bg)
            .statusBarsPadding(),
    ) {
        Surface(color = lc.surface, modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回长帖", tint = lc.text)
                    }
                    Text("社区实时聊天", color = lc.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    groups.forEach { g ->
                        val label = "# ${DisguiseSanitizer.resolveLinuxDoChannelAlias(g.id, g.name)}"
                        val selected = activeIsGroup && activeGroupId == g.id
                        FilterChip(
                            selected = selected,
                            onClick = {
                                activeIsGroup = true
                                activeGroupId = g.id
                            },
                            label = { Text(label, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = lc.accent,
                                selectedLabelColor = Color.White,
                            ),
                        )
                    }
                    dmThreads.forEach { dm ->
                        val peerName = linuxDoSafeName(dm.peerName ?: dm.peerUsername)
                        val selected = !activeIsGroup && activeDmId == dm.id
                        FilterChip(
                            selected = selected,
                            onClick = {
                                activeIsGroup = false
                                activeDmId = dm.id
                            },
                            label = { Text("@ $peerName", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = lc.accent,
                                selectedLabelColor = Color.White,
                            ),
                        )
                    }
                }
                HorizontalDivider(color = lc.line)
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            ChatScreen(
                api = api,
                controller = chatController,
                me = me,
                onNavigateToMeme = { onClose() },
                onNavigateToTag = {},
                onOpenUrl = onOpenUrl,
                onRequireLogin = onRequireLogin,
                hideChannelHeader = true,
                forcedChannelMode = if (activeIsGroup) "group" else "dm",
                forcedDmThreadId = if (activeIsGroup) null else activeDmId,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun LinuxDoComposerSheet(
    api: ForHappyApiClient,
    replyFloorTarget: Pair<Int, MemeItem>?,
    selectedTag: String?,
    lc: LinuxDoColors,
    onPublishedFloor: () -> Unit,
    onRepliedToFloor: (Long) -> Unit,
    onNotify: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val bossKeyActive = LocalBossKeyActive.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var text by remember(replyFloorTarget) { mutableStateOf("") }
    val stagedItems = remember { mutableStateListOf<ComposeTrayItem>() }
    var mergeAll by remember { mutableStateOf(true) }
    var spoilerFlag by remember { mutableStateOf(false) }
    var showEmojiBar by remember { mutableStateOf(false) }
    var sending by remember { mutableStateOf(false) }
    var maskEditorIndex by remember { mutableStateOf<Int?>(null) }

    val pickLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(12),
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        scope.launch {
            val added = uris.mapNotNull { ComposeMediaStager.stageUri(context, it) }
            stagedItems.addAll(added.take(12 - stagedItems.size))
        }
    }

    ModalBottomSheet(
        onDismissRequest = { if (!sending) onDismiss() },
        sheetState = sheetState,
        containerColor = lc.surface,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = replyFloorTarget?.let { (num, _) -> "回复 #$num 楼" } ?: "回复主题（盖新楼）",
                    color = lc.text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
                IconButton(onClick = { if (!sending) onDismiss() }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Rounded.Close, contentDescription = "关闭", tint = lc.muted)
                }
            }

            if (replyFloorTarget != null) {
                Text(
                    text = "引用 #${replyFloorTarget.first}：${linuxDoFloorBody(replyFloorTarget.second)}",
                    color = lc.muted,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(lc.bg, RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }

            // Discourse Markdown toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val mdButtons = listOf(
                    "B" to "**加粗**",
                    "I" to "*斜体*",
                    "🔗" to "[链接](https://)",
                    "❝" to "> ",
                    "</>" to "`code`",
                )
                mdButtons.forEach { (label, snippet) ->
                    Surface(
                        color = lc.surfaceAlt,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { text = if (text.isBlank()) snippet else "$text $snippet" },
                    ) {
                        Text(label, color = lc.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp))
                    }
                }
                Surface(
                    color = if (showEmojiBar) lc.accent.copy(alpha = 0.16f) else lc.surfaceAlt,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { showEmojiBar = !showEmojiBar },
                ) {
                    Text("☺", color = lc.text, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp))
                }
                Surface(
                    color = lc.accent.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            StealthExemptionGate.markInternalJump()
                            pickLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                        },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(Icons.Rounded.Image, contentDescription = null, tint = lc.accent, modifier = Modifier.size(14.dp))
                        Text("插图", color = lc.accent, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            if (showEmojiBar) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    DisguiseSanitizer.SAFE_DISGUISE_EMOJIS.forEach { emoji ->
                        Text(
                            text = emoji,
                            fontSize = 18.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { text += emoji }
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                        )
                    }
                }
            }

            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("使用 Markdown 撰写内容，支持同时附带多张图（点击缩略图可打码）…", fontSize = 13.sp, color = lc.muted) },
                minLines = 3,
                maxLines = 7,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = lc.accent,
                    unfocusedBorderColor = lc.line,
                    focusedTextColor = lc.text,
                    unfocusedTextColor = lc.text,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            DisguiseStagedMediaStrip(
                items = stagedItems,
                api = api,
                accentColor = lc.accent,
                mergeAll = mergeAll,
                spoiler = spoilerFlag,
                mergeLabel = "合并盖一楼",
                soloLabel = "逐张单盖",
                spoilerLabel = "敏感折叠 [spoiler]",
                onToggleMerge = { mergeAll = it },
                onToggleSpoiler = { spoilerFlag = it },
                onEditMask = { idx -> maskEditorIndex = idx },
                onRemoveItem = { id -> stagedItems.removeAll { it.id == id } },
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(enabled = !sending, onClick = onDismiss) {
                    Text("取消", color = lc.muted)
                }
                Spacer(Modifier.width(8.dp))
                val canPublish = !sending && (text.isNotBlank() || stagedItems.isNotEmpty())
                Surface(
                    color = if (canPublish) lc.accent else lc.surfaceAlt,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(enabled = canPublish) {
                            sending = true
                            val draft = text.trim()
                            val snapshot = stagedItems.toList()
                            val tags = if (!selectedTag.isNullOrBlank()) listOf(selectedTag) else emptyList()
                            scope.launch {
                                val targetFloor = replyFloorTarget?.second
                                if (targetFloor != null && snapshot.isEmpty()) {
                                    val ok = api.postMemeComment(targetFloor.id, draft)
                                    sending = false
                                    if (ok) {
                                        onRepliedToFloor(targetFloor.id)
                                    } else {
                                        onNotify("回复提交失败，请确认已登录账号")
                                    }
                                } else {
                                    val res = disguiseUploadAndPublish(
                                        context = context,
                                        api = api,
                                        text = draft,
                                        stagedItems = snapshot,
                                        mergeAll = mergeAll,
                                        spoiler = spoilerFlag,
                                        tags = tags,
                                        fallbackTitlePrefix = "佬友素材分享",
                                    )
                                    sending = false
                                    res.fold(
                                        onSuccess = { onPublishedFloor() },
                                        onFailure = { err ->
                                            onNotify(
                                                DisguiseSanitizer.sanitizeSummaryForDisguise(
                                                    err.message ?: "发布失败",
                                                    AppDisguiseMode.LINUX_DO,
                                                ),
                                            )
                                        },
                                    )
                                }
                            }
                        },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Send,
                            contentDescription = null,
                            tint = if (canPublish) Color.White else lc.muted,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = if (sending) "提交中…" else "提交发布",
                            color = if (canPublish) Color.White else lc.muted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }

    if (!bossKeyActive) {
        maskEditorIndex?.let { startIdx ->
            if (stagedItems.isNotEmpty()) {
                Dialog(
                    onDismissRequest = { maskEditorIndex = null },
                    properties = FullscreenDialogProperties,
                ) {
                    ConfigureFullscreenDialogWindow(darkStatusIcons = false)
                    ImageMaskEditorScreen(
                        context = context,
                        imageItems = stagedItems.toList(),
                        initialIndex = startIdx,
                        onItemUpdated = { updated ->
                            val idx = stagedItems.indexOfFirst { it.id == updated.id }
                            if (idx >= 0) stagedItems[idx] = updated
                        },
                        onDismiss = { maskEditorIndex = null },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LinuxDoPreferencesOverlay(
    api: ForHappyApiClient,
    me: MeUser?,
    themeMode: AppThemeMode,
    privacyConfig: PrivacyStealthConfig,
    privacyShieldEnabled: Boolean,
    activeIconAlias: AppIconAlias,
    lc: LinuxDoColors,
    onChangeTheme: (AppThemeMode) -> Unit,
    onExitDisguise: () -> Unit,
    onChangeDisguise: (AppDisguiseMode) -> Unit,
    onUpdatePrivacyConfig: (PrivacyStealthConfig) -> Unit,
    onTogglePrivacyShield: (Boolean) -> Unit,
    onChangeIconAlias: (AppIconAlias) -> Unit,
    onRequireLogin: () -> Unit,
    onLoggedOut: () -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var activeTab by remember { mutableStateOf("ui") } // "ui" | "privacy" | "account"
    var showBossPreview by remember { mutableStateOf(false) }

    BackHandler(enabled = !showBossPreview) { onClose() }

    if (showBossPreview) {
        BossDocScreen(
            longPressTitleEnabled = privacyConfig.bossExitLongPressTitle,
            tapCollabFiveEnabled = privacyConfig.bossExitTapCollabFive,
            doubleBackExitEnabled = privacyConfig.bossDoubleBack,
            hapticEnabled = privacyConfig.stealthHapticFeedback,
            api = api,
            isPreviewMode = true,
            onExit = { showBossPreview = false },
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(lc.bg)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Surface(color = lc.surface, modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回", tint = lc.text)
                    }
                    Text("偏好设置", color = lc.text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    val tabs = listOf("ui" to "界面", "privacy" to "隐私", "account" to "账号")
                    tabs.forEach { (id, label) ->
                        val selected = activeTab == id
                        Column(
                            modifier = Modifier
                                .clickable { activeTab = id }
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = label,
                                color = if (selected) lc.accent else lc.muted,
                                fontSize = 14.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            )
                            Spacer(Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .width(28.dp)
                                    .height(2.dp)
                                    .background(if (selected) lc.accent else Color.Transparent),
                            )
                        }
                    }
                }
                HorizontalDivider(color = lc.line)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (activeTab) {
                "ui" -> {
                    Surface(color = lc.surface, shape = RoundedCornerShape(10.dp), border = BorderStroke(0.5.dp, lc.line), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("主题风格", color = lc.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Text("一键切换不同浏览与阅读外观", color = lc.muted, fontSize = 12.sp)
                            HorizontalDivider(color = lc.line)
                            LinuxDoChoiceRow("默认", "恢复标准四栏主界面布局", selected = false, lc = lc, onClick = onExitDisguise)
                            LinuxDoChoiceRow("飞书办公", "切换为即时消息与工作群视图", selected = false, lc = lc) {
                                onChangeDisguise(AppDisguiseMode.FEISHU)
                            }
                            LinuxDoChoiceRow("LINUX DO（当前）", "长帖盖楼讨论与右侧快速定位时间线", selected = true, lc = lc) {}
                        }
                    }

                    Surface(color = lc.surface, shape = RoundedCornerShape(10.dp), border = BorderStroke(0.5.dp, lc.line), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("深色配色", color = lc.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            val modes = listOf(
                                AppThemeMode.FOLLOW_SYSTEM to "跟随系统",
                                AppThemeMode.WARM_PAPER_LIGHT to "浅色模式",
                                AppThemeMode.PINE_NIGHT_DARK to "深色模式",
                            )
                            modes.forEach { (mode, label) ->
                                LinuxDoChoiceRow(label, "", selected = themeMode == mode, lc = lc) {
                                    onChangeTheme(mode)
                                }
                            }
                        }
                    }
                }

                "privacy" -> {
                    Surface(color = lc.surface, shape = RoundedCornerShape(10.dp), border = BorderStroke(0.5.dp, lc.line), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            LinuxDoSwitchRow("图片与附件默认保护遮罩", privacyConfig.blurAllEnabled, lc) {
                                onUpdatePrivacyConfig(privacyConfig.copy(blurAllEnabled = it))
                            }
                            LinuxDoSwitchRow("连续两次返回切换只读文档", privacyConfig.bossDoubleBack, lc) {
                                onUpdatePrivacyConfig(privacyConfig.copy(bossDoubleBack = it))
                            }
                            LinuxDoSwitchRow("离开后台自动锁定为会议文档", privacyConfig.bossOnBackgroundResume, lc) {
                                onUpdatePrivacyConfig(privacyConfig.copy(bossOnBackgroundResume = it))
                            }
                            LinuxDoSwitchRow("防截屏 / 后台缩略图隐私模糊", privacyShieldEnabled, lc, onTogglePrivacyShield)
                            HorizontalDivider(color = lc.line)
                            Text("快捷切换手势", color = lc.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ShortcutGestureMode.entries.forEach { mode ->
                                    FilterChip(
                                        selected = privacyConfig.bossVolumeShortcut == mode,
                                        onClick = { onUpdatePrivacyConfig(privacyConfig.copy(bossVolumeShortcut = mode)) },
                                        label = { Text(mode.label, fontSize = 12.sp) },
                                    )
                                }
                            }
                            HorizontalDivider(color = lc.line)
                            Text("桌面图标样式", color = lc.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                AppIconAlias.entries.forEach { alias ->
                                    FilterChip(
                                        selected = activeIconAlias == alias,
                                        onClick = {
                                            applyLauncherIconAlias(context, alias)
                                            onChangeIconAlias(alias)
                                        },
                                        label = { Text(safeIconAliasTitle(alias), fontSize = 12.sp) },
                                    )
                                }
                            }
                            HorizontalDivider(color = lc.line)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showBossPreview = true }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column {
                                    Text("锁屏文档样式预览", color = lc.text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    Text("预览只读文档界面并测试长按标题退出手势", color = lc.muted, fontSize = 11.sp)
                                }
                                Icon(Icons.Rounded.KeyboardArrowRight, contentDescription = null, tint = lc.muted)
                            }
                        }
                    }
                }

                else -> {
                    Surface(color = lc.surface, shape = RoundedCornerShape(10.dp), border = BorderStroke(0.5.dp, lc.line), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = if (me != null) linuxDoSafeName(me.displayName) else "未登录账号",
                                color = lc.text,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text("Discourse Reader Client v${BuildConfig.VERSION_NAME}", color = lc.muted, fontSize = 12.sp)
                            if (me == null) {
                                TextButton(onClick = onRequireLogin) {
                                    Text("点击登录社区账号", color = lc.accent)
                                }
                            } else {
                                TextButton(
                                    onClick = {
                                        scope.launch {
                                            api.logout()
                                            onLoggedOut()
                                        }
                                    },
                                ) {
                                    Text("退出当前账号", color = lc.danger)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LinuxDoChoiceRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    lc: LinuxDoColors,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) lc.accent.copy(alpha = 0.08f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (selected) lc.accent else lc.text,
                fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            )
            if (subtitle.isNotBlank()) {
                Text(subtitle, color = lc.muted, fontSize = 11.sp)
            }
        }
        if (selected) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = lc.accent, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun LinuxDoSwitchRow(
    title: String,
    checked: Boolean,
    lc: LinuxDoColors,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, color = lc.text, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
