package com.jinhuan.forhappy.ui.wall

import android.graphics.RectF
import android.os.SystemClock
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddReaction
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil3.compose.AsyncImage
import com.jinhuan.forhappy.core.clock.ServerClock
import com.jinhuan.forhappy.core.compose.ComposeGenre
import com.jinhuan.forhappy.core.compose.ComposeLaunchRequest
import com.jinhuan.forhappy.core.compose.SocialLinkDetector
import com.jinhuan.forhappy.core.model.AnchorTag
import com.jinhuan.forhappy.core.model.DayEntry
import com.jinhuan.forhappy.core.model.ForHappyJson
import com.jinhuan.forhappy.core.model.MeUser
import com.jinhuan.forhappy.core.model.MemeItem
import com.jinhuan.forhappy.core.model.SiteNotice
import com.jinhuan.forhappy.core.model.TagStatItem
import com.jinhuan.forhappy.core.model.WallFilterState
import com.jinhuan.forhappy.core.network.api.ForHappyApiClient
import com.jinhuan.forhappy.core.network.media.MediaUrlRewriter
import com.jinhuan.forhappy.core.seen.LocalSeenTracker
import com.jinhuan.forhappy.core.seen.ObserveMemeSeenDwell
import com.jinhuan.forhappy.core.seen.computeVisibleAreaRatio
import com.jinhuan.forhappy.core.seen.shouldAutoReblurRevealed
import com.jinhuan.forhappy.ui.components.BadgePill
import com.jinhuan.forhappy.ui.components.BoostPillText
import com.jinhuan.forhappy.ui.components.ForHappyAvatar
import com.jinhuan.forhappy.ui.components.ReactionChip
import com.jinhuan.forhappy.ui.components.RichChatBodyView
import com.jinhuan.forhappy.ui.theme.ForHappyDimens
import com.jinhuan.forhappy.ui.theme.LocalForHappyPalette
import com.jinhuan.forhappy.ui.theme.LocalPrivacyStealthConfig
import com.jinhuan.forhappy.ui.theme.TABULAR_FONT_FEATURE
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encodeToString
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@Serializable
private data class WallPrefsSnapshot(
    val sort: String = "new",
    val unseenOnly: Boolean = false,
    val textFilter: String = "",
    val nsfwMode: String = "blur",
    val maskTap: String = "peek",
    val viewMode: String = "dual",
    val byDay: Boolean = true,
)

@Serializable
private data class WallPosSnapshot(
    val memeId: Long = 0L,
    val day: String? = null,
)

private sealed interface WallFeedEntry {
    data class DayHeader(
        val day: String,
        val label: String,
        val totalCount: Int,
        val unseenCount: Int,
    ) : WallFeedEntry

    data class MemeCard(val meme: MemeItem) : WallFeedEntry
}

private const val KV_WALL_PREFS = "ui.wall_prefs_v2"
private const val KV_WALL_POS = "ui.wall_pos_v2"
private const val KV_DISMISSED_NOTICES = "ui.dismissed_notice_ids"
private const val KV_PEEK_HINT_COUNT = "ui.peek_hint_count"

/**
 * Gallery Feed (`图廊`) implementing `docs/redesign/02a-scenario-wall.md` & `04-gestures.md`:
 * - Single collapsible 48dp topbar (`图廊 [摘要胶囊 ▾] 🔍 🔔`) + optional 32dp dismissible notice strip
 * - Natural-day section headers (`byDay`) + calendar jumper (`WallDayJumpSheet`)
 * - Three view modes: Dual Waterfall (`dual`), Single Large Card (`single`), Immersive Vertical Pager (`pager`)
 * - Clean cards (zero seen/unseen/post-id/time clutter), dual-gated 1s seen observer & auto-reblur <50%
 * - Long-press quick menu, full-screen search, browse settings sheet, and stealth progress resume (`wallPos`)
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun WallScreen(
    api: ForHappyApiClient,
    me: MeUser?,
    initialTag: String? = null,
    initialAuthor: String? = null,
    initialSeries: String? = null,
    initialDetailMemeId: Long? = null,
    notifUnreadCount: Int = 0,
    openBrowseSettingsSignal: Int = 0,
    openSeriesIndexSignal: Int = 0,
    scrollToTopSignal: Int = 0,
    externalRefreshSignal: Int = 0,
    onConsumeInitialArgs: () -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    onOpenUrl: (String) -> Unit = {},
    onOpenCompose: ((ComposeLaunchRequest) -> Unit)? = null,
    onRequireLogin: () -> Unit = {},
    onToggleBlurAll: (Boolean) -> Unit = {},
    moyuHeaderPillText: String? = null,
    onClickMoyuPill: (() -> Unit)? = null,
    forceUnseenSignal: Int = 0,
) {
    val palette = LocalForHappyPalette.current
    val privacyConfig = LocalPrivacyStealthConfig.current
    val seenTracker = LocalSeenTracker.current
    val clipboard = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val gridState = rememberLazyStaggeredGridState()

    var prefsLoaded by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf(WallFilterState()) }
    var anchors by remember { mutableStateOf<List<AnchorTag>>(emptyList()) }
    var popularTags by remember { mutableStateOf<List<TagStatItem>>(emptyList()) }
    var daysMeta by remember { mutableStateOf<List<DayEntry>>(emptyList()) }
    var notices by remember { mutableStateOf<List<SiteNotice>>(emptyList()) }
    var dismissedNoticeIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var peekHintCount by remember { mutableIntStateOf(0) }

    var memes by remember { mutableStateOf<List<MemeItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    var loadingMore by remember { mutableStateOf(false) }
    var hasMore by remember { mutableStateOf(false) }
    var fetchOffset by remember { mutableIntStateOf(0) }

    // Sub-sheets & full-screen overlays
    var selectedMemeId by remember { mutableStateOf(initialDetailMemeId) }
    var quickActionMeme by remember { mutableStateOf<MemeItem?>(null) }
    var immersiveViewerTarget by remember { mutableStateOf<MemeItem?>(null) }
    var detailInlineFullscreenUrl by remember { mutableStateOf<String?>(null) }
    var showCompose by remember { mutableStateOf(false) }
    var showFabMenu by remember { mutableStateOf(false) }
    var showBrowseSettings by remember { mutableStateOf(false) }
    var showDayJumpSheet by remember { mutableStateOf(false) }
    var showSearchScreen by remember { mutableStateOf(false) }
    var showSeriesIndexDialog by remember { mutableStateOf(false) }
    var activeNoticeDetail by remember { mutableStateOf<SiteNotice?>(null) }
    var inlineToast by remember { mutableStateOf<String?>(null) }
    var showJumpTopPill by remember { mutableStateOf(false) }
    var didInitialResume by remember { mutableStateOf(false) }

    // Collapsible topBar & FAB scroll tracking
    val topBarHeightPx = with(density) { ForHappyDimens.topBarHeight.toPx() }
    var topBarOffsetPx by remember { mutableFloatStateOf(0f) }
    var fabVisible by remember { mutableStateOf(true) }
    var feedViewportRect by remember { mutableStateOf(RectF()) }
    var scrollSpeedDpPerSec by remember { mutableFloatStateOf(0f) }
    var lastVelocitySampleMs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(inlineToast) {
        if (inlineToast != null) {
            delay(2_200L)
            inlineToast = null
        }
    }

    // Smoothly decay scroll velocity to 0 and restore FAB after 600ms idle (`01 §1.3`)
    LaunchedEffect(gridState.isScrollInProgress) {
        if (!gridState.isScrollInProgress) {
            delay(120L)
            scrollSpeedDpPerSec = 0f
            delay(480L)
            fabVisible = true
        }
    }

    val nestedScrollConnection = remember(topBarHeightPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val dy = available.y
                if (dy < -2f) fabVisible = false else if (dy > 4f) fabVisible = true

                val now = SystemClock.uptimeMillis()
                val dt = (now - lastVelocitySampleMs).coerceAtLeast(16L)
                lastVelocitySampleMs = now
                if (source == NestedScrollSource.UserInput) {
                    val speedPxPerSec = abs(dy) * (1000f / dt.toFloat())
                    scrollSpeedDpPerSec = with(density) { speedPxPerSec / this.density }
                }

                val next = (topBarOffsetPx + dy).coerceIn(-topBarHeightPx, 0f)
                topBarOffsetPx = next
                return Offset.Zero
            }
        }
    }

    // Load persisted preferences, notice dismissals, peek hint counter
    LaunchedEffect(Unit) {
        val rawPrefs = api.getPref(KV_WALL_PREFS)
        val snap = rawPrefs?.let {
            runCatching { ForHappyJson.decodeFromString(WallPrefsSnapshot.serializer(), it) }.getOrNull()
        }
        val rawDismissed = api.getPref(KV_DISMISSED_NOTICES)
        if (!rawDismissed.isNullOrBlank()) {
            dismissedNoticeIds = runCatching {
                ForHappyJson.decodeFromString(ListSerializer(Long.serializer()), rawDismissed).toSet()
            }.getOrDefault(emptySet())
        }
        peekHintCount = api.getPref(KV_PEEK_HINT_COUNT)?.toIntOrNull() ?: 0

        val base = if (snap != null) {
            WallFilterState(
                sort = snap.sort,
                unseenOnly = snap.unseenOnly,
                textFilter = snap.textFilter,
                nsfwMode = snap.nsfwMode,
                maskTap = snap.maskTap,
                viewMode = snap.viewMode,
                byDay = snap.byDay,
            )
        } else {
            WallFilterState()
        }
        filter = base.copy(
            tags = if (!initialTag.isNullOrBlank()) setOf(initialTag) else base.tags,
            author = initialAuthor?.takeIf { it.isNotBlank() } ?: base.author,
            series = initialSeries?.takeIf { it.isNotBlank() } ?: base.series,
        )
        prefsLoaded = true
        if (initialTag != null || initialAuthor != null || initialSeries != null) {
            onConsumeInitialArgs()
        }
    }

    // Apply incoming cross-tab / deep-link arguments after startup
    LaunchedEffect(initialDetailMemeId) {
        if (initialDetailMemeId != null) {
            selectedMemeId = initialDetailMemeId
            onConsumeInitialArgs()
        }
    }
    LaunchedEffect(initialTag, initialAuthor, initialSeries) {
        if (!prefsLoaded) return@LaunchedEffect
        if (initialTag != null || initialAuthor != null || initialSeries != null) {
            filter = filter.copy(
                tags = initialTag?.let { setOf(it) } ?: emptySet(),
                author = initialAuthor.orEmpty(),
                series = initialSeries.orEmpty(),
            )
            onConsumeInitialArgs()
        }
    }
    LaunchedEffect(openBrowseSettingsSignal) {
        if (openBrowseSettingsSignal > 0) showBrowseSettings = true
    }
    LaunchedEffect(openSeriesIndexSignal) {
        if (openSeriesIndexSignal > 0) showSeriesIndexDialog = true
    }

    fun persistPrefs(next: WallFilterState) {
        scope.launch {
            val snap = WallPrefsSnapshot(
                sort = next.sort,
                unseenOnly = next.unseenOnly,
                textFilter = next.textFilter,
                nsfwMode = next.nsfwMode,
                maskTap = next.maskTap,
                viewMode = next.viewMode,
                byDay = next.byDay,
            )
            api.putPref(KV_WALL_PREFS, ForHappyJson.encodeToString(WallPrefsSnapshot.serializer(), snap))
        }
    }

    fun applyFilter(next: WallFilterState) {
        filter = next
        persistPrefs(next)
        topBarOffsetPx = 0f
        fabVisible = true
    }

    var lastHandledUnseenSignal by remember { mutableIntStateOf(0) }
    LaunchedEffect(forceUnseenSignal, prefsLoaded) {
        if (prefsLoaded && forceUnseenSignal > lastHandledUnseenSignal) {
            lastHandledUnseenSignal = forceUnseenSignal
            if (!filter.unseenOnly || filter.fromDay != null || filter.focusMemeId != null) {
                applyFilter(filter.copy(unseenOnly = true, fromDay = null, focusMemeId = null))
            }
        }
    }

    // Fetch anchors, tags, notices
    LaunchedEffect(filter.nsfwMode, me?.id) {
        val nsfwParam = filter.nsfwQueryValue()
        val anchorsDef = async { if (me != null) api.fetchAnchors() else emptyList() }
        val tagsDef = async { api.fetchTags(nsfwModeParam = nsfwParam) }
        val noticesDef = async { api.fetchNotices() }
        anchors = anchorsDef.await()
        popularTags = tagsDef.await()
        notices = noticesDef.await()
    }

    fun reload(curFilter: WallFilterState = filter, asRefresh: Boolean = false) {
        scope.launch {
            if (asRefresh) refreshing = true else loading = true
            if (curFilter.unseenOnly) {
                seenTracker?.flushNow()
            }
            val tagsCsv = curFilter.tags.takeIf { it.isNotEmpty() }?.joinToString(",")
            val q = curFilter.searchQuery.trim().ifBlank { null }
            val author = curFilter.author.trim().ifBlank { null }
            val series = curFilter.series.trim().ifBlank { null }
            val nsfwParam = curFilter.nsfwQueryValue()

            val pageDef = async {
                api.fetchMemesPage(
                    limit = 40,
                    offset = 0,
                    day = curFilter.fromDay,
                    focus = curFilter.focusMemeId,
                    tag = tagsCsv,
                    query = q,
                    series = series,
                    sort = curFilter.sort,
                    unseenOnly = curFilter.unseenOnly,
                    textFilter = curFilter.textFilter,
                    nsfwModeParam = nsfwParam,
                    author = author,
                )
            }
            val daysDef = async {
                if (curFilter.isByDay) {
                    api.fetchDays(
                        limitDays = 45,
                        fromDay = curFilter.fromDay,
                        tag = tagsCsv,
                        search = q,
                        sort = curFilter.sort,
                        unseenOnly = curFilter.unseenOnly,
                        textFilter = curFilter.textFilter,
                        nsfwModeParam = nsfwParam,
                        author = author,
                    )
                } else {
                    null
                }
            }

            val page = pageDef.await()
            val daysResp = daysDef.await()
            if (daysResp != null) {
                daysMeta = daysResp.days
            }
            seenTracker?.ingestFetchedMemes(page.items)
            val filtered = if (curFilter.unseenOnly) {
                page.items.filter { seenTracker?.isSeen(it.id, it.seenAt) != true }
            } else {
                page.items
            }
            memes = filtered
            fetchOffset = page.items.size
            hasMore = page.more
            loading = false
            refreshing = false

            // Silent `wallPos` cold-start restore when on default feed
            if (!didInitialResume &&
                curFilter.sort == "new" &&
                !curFilter.unseenOnly &&
                curFilter.tags.isEmpty() &&
                curFilter.author.isBlank() &&
                curFilter.series.isBlank() &&
                curFilter.fromDay == null &&
                curFilter.focusMemeId == null &&
                curFilter.searchQuery.isBlank()
            ) {
                didInitialResume = true
                val rawPos = api.getPref(KV_WALL_POS)
                val pos = rawPos?.let { runCatching { ForHappyJson.decodeFromString(WallPosSnapshot.serializer(), it) }.getOrNull() }
                if (pos != null && pos.memeId > 0L) {
                    val idx = filtered.indexOfFirst { it.id == pos.memeId }
                    if (idx > 6) {
                        gridState.scrollToItem(idx)
                        if (idx > 50) showJumpTopPill = true
                    }
                }
            } else {
                didInitialResume = true
            }
        }
    }

    fun loadMore() {
        if (loading || loadingMore || !hasMore) return
        scope.launch {
            loadingMore = true
            val tagsCsv = filter.tags.takeIf { it.isNotEmpty() }?.joinToString(",")
            val q = filter.searchQuery.trim().ifBlank { null }
            val author = filter.author.trim().ifBlank { null }
            val series = filter.series.trim().ifBlank { null }
            val nsfwParam = filter.nsfwQueryValue()
            val existingIds = memes.asSequence().map { it.id }.toHashSet()

            val page = api.fetchMemesPage(
                limit = 30,
                offset = fetchOffset,
                day = filter.fromDay,
                tag = tagsCsv,
                query = q,
                series = series,
                sort = filter.sort,
                unseenOnly = filter.unseenOnly,
                textFilter = filter.textFilter,
                nsfwModeParam = nsfwParam,
                author = author,
            )
            seenTracker?.ingestFetchedMemes(page.items)
            val appended = page.items.filter { existingIds.add(it.id) }.let { list ->
                if (filter.unseenOnly) list.filter { seenTracker?.isSeen(it.id, it.seenAt) != true } else list
            }
            if (appended.isNotEmpty()) {
                memes = memes + appended
            }
            fetchOffset += page.items.size
            hasMore = page.more && page.items.isNotEmpty()
            loadingMore = false
        }
    }

    LaunchedEffect(externalRefreshSignal) {
        if (externalRefreshSignal > 0 && prefsLoaded) {
            reload(filter)
        }
    }

    LaunchedEffect(
        prefsLoaded,
        filter.sort,
        filter.unseenOnly,
        filter.textFilter,
        filter.nsfwMode,
        filter.byDay,
        filter.tags,
        filter.author,
        filter.series,
        filter.fromDay,
        filter.focusMemeId,
        filter.searchQuery,
    ) {
        if (prefsLoaded) reload(filter)
    }

    // Bottom tab re-tap: scroll to top if down, else refresh
    LaunchedEffect(scrollToTopSignal) {
        if (scrollToTopSignal == 0) return@LaunchedEffect
        topBarOffsetPx = 0f
        if (gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > 0) {
            gridState.animateScrollToItem(0)
            showJumpTopPill = false
        } else {
            reload(filter, asRefresh = true)
        }
    }

    val entries = remember(memes, daysMeta, filter.isByDay) {
        buildFeedEntries(memes, daysMeta, filter.isByDay)
    }

    // Auto-pagination trigger when scrolling near bottom
    val nearEnd by remember(entries.size) {
        derivedStateOf {
            val total = entries.size
            val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            total > 0 && lastVisible >= total - 6
        }
    }
    LaunchedEffect(nearEnd, hasMore, loading, loadingMore) {
        if (nearEnd && hasMore && !loading && !loadingMore) {
            loadMore()
        }
    }

    // Persist last viewed post (`wallPos`) silently on scroll stop
    LaunchedEffect(gridState, entries) {
        @Suppress("OPT_IN_USAGE")
        snapshotFlow { gridState.firstVisibleItemIndex }
            .debounce(400L)
            .collect { firstIdx ->
                if (firstIdx > 50) showJumpTopPill = true else if (firstIdx <= 3) showJumpTopPill = false
                val card = entries.drop(firstIdx).filterIsInstance<WallFeedEntry.MemeCard>().firstOrNull()
                    ?: return@collect
                api.putPref(
                    KV_WALL_POS,
                    ForHappyJson.encodeToString(WallPosSnapshot.serializer(), WallPosSnapshot(card.meme.id, card.meme.day)),
                )
            }
    }

    // Keep detail-revealed-only states clean when leaving foreground
    val activeNotice = remember(notices, dismissedNoticeIds) {
        notices.firstOrNull { !dismissedNoticeIds.contains(it.id) }
    }
    val topBarDpOffset = with(density) { topBarOffsetPx.roundToInt() }
    val showTopBarVisible = filter.viewMode != "pager"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.bg),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Single 48dp collapsible gallery topbar (`02a §1`)
            if (showTopBarVisible) {
                val collapsedFrac = (-topBarOffsetPx / topBarHeightPx).coerceIn(0f, 1f)
                val barHeight = ForHappyDimens.topBarHeight * (1f - collapsedFrac)
                if (barHeight > 1.dp) {
                    Surface(
                        color = palette.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(barHeight),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .offset { IntOffset(0, topBarDpOffset) }
                                .padding(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                when {
                                    filter.series.isNotBlank() -> ActiveHeaderFilterChip(
                                        label = filter.series,
                                        onClear = { applyFilter(filter.copy(series = "", fromDay = null, focusMemeId = null)) },
                                    )
                                    filter.author.isNotBlank() -> ActiveHeaderFilterChip(
                                        label = "@${filter.author.substringBefore(',')}",
                                        onClear = { applyFilter(filter.copy(author = "", fromDay = null, focusMemeId = null)) },
                                    )
                                    filter.tags.isNotEmpty() -> ActiveHeaderFilterChip(
                                        label = "#${filter.tags.first()}" + if (filter.tags.size > 1) " +${filter.tags.size - 1}" else "",
                                        onClear = { applyFilter(filter.copy(tags = emptySet(), fromDay = null, focusMemeId = null)) },
                                    )
                                    else -> Text(
                                        text = "图廊",
                                        color = palette.text,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }

                                Surface(
                                    color = palette.surface2,
                                    shape = RoundedCornerShape(999.dp),
                                    modifier = Modifier
                                        .border(1.dp, palette.line, RoundedCornerShape(999.dp))
                                        .clickable { showBrowseSettings = true },
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    ) {
                                        Text(
                                            text = filter.summaryCapsuleLabel(),
                                            color = palette.text,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Icon(
                                            imageVector = Icons.Rounded.ArrowDropDown,
                                            contentDescription = "浏览设置",
                                            tint = palette.muted,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                moyuHeaderPillText?.takeIf { it.isNotBlank() }?.let { pill ->
                                    Surface(
                                        color = palette.gold.copy(alpha = 0.14f),
                                        shape = RoundedCornerShape(999.dp),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .clickable(enabled = onClickMoyuPill != null) { onClickMoyuPill?.invoke() },
                                    ) {
                                        Text(
                                            text = pill,
                                            color = palette.gold,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            style = TextStyle(fontFeatureSettings = TABULAR_FONT_FEATURE),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        )
                                    }
                                    Spacer(Modifier.width(4.dp))
                                }
                                IconButton(
                                    onClick = { showSearchScreen = true },
                                    modifier = Modifier.size(40.dp),
                                ) {
                                    Icon(Icons.Rounded.Search, contentDescription = "搜索", tint = palette.text, modifier = Modifier.size(21.dp))
                                }

                                if (me == null) {
                                    BadgePill(
                                        text = "登录",
                                        containerColor = palette.primary,
                                        contentColor = palette.onPrimary,
                                        modifier = Modifier.clickable { onRequireLogin() },
                                    )
                                } else {
                                    IconButton(
                                        onClick = onOpenNotifications,
                                        modifier = Modifier.size(40.dp),
                                    ) {
                                        Box(contentAlignment = Alignment.TopEnd) {
                                            Icon(
                                                imageVector = Icons.Rounded.NotificationsNone,
                                                contentDescription = "通知",
                                                tint = if (notifUnreadCount > 0) palette.text else palette.muted,
                                                modifier = Modifier.size(21.dp),
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
                    }
                    HorizontalDivider(color = palette.line)
                }
            }

            // Optional 32dp dismissible site notice banner (`02a §1`)
            if (activeNotice != null && showTopBarVisible) {
                val plainLine = remember(activeNotice.id, activeNotice.body) {
                    activeNotice.body.replace("**", "").replace(Regex("\\s+"), " ").trim()
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .background(palette.primaryContainer.copy(alpha = 0.55f))
                        .clickable { activeNoticeDetail = activeNotice }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.Rounded.Campaign, contentDescription = null, tint = palette.primary, modifier = Modifier.size(16.dp))
                    Text(
                        text = plainLine,
                        color = palette.text,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "关闭公告",
                        tint = palette.muted,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable {
                                val next = dismissedNoticeIds + activeNotice.id
                                dismissedNoticeIds = next
                                scope.launch {
                                    api.putPref(
                                        KV_DISMISSED_NOTICES,
                                        ForHappyJson.encodeToString(ListSerializer(Long.serializer()), next.toList().takeLast(50)),
                                    )
                                }
                            },
                    )
                }
                HorizontalDivider(color = palette.line)
            }

            // Feed Area (Dual / Single / Immersive Vertical Pager)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .onGloballyPositioned { coords ->
                        val b = coords.boundsInWindow()
                        feedViewportRect = RectF(b.left, b.top, b.right, b.bottom)
                    },
            ) {
                if (loading && memes.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = palette.primary)
                    }
                } else if (memes.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = when {
                                    filter.unseenOnly -> "全都看过了"
                                    filter.searchQuery.isNotBlank() -> "没搜到“${filter.searchQuery}”"
                                    else -> "当前筛选条件下暂无梗图"
                                },
                                color = palette.muted,
                                fontSize = 14.sp,
                            )
                            if (filter.activeFilterCount > 0 || filter.searchQuery.isNotBlank()) {
                                OutlinedButton(onClick = { applyFilter(WallFilterState()) }) {
                                    Text("恢复默认筛选")
                                }
                            }
                        }
                    }
                } else if (filter.viewMode == "pager") {
                    WallImmersivePagerFeed(
                        memes = memes,
                        api = api,
                        me = me,
                        filter = filter,
                        blurAllEnabled = privacyConfig.blurAllEnabled,
                        hasMore = hasMore,
                        onLoadMore = { loadMore() },
                        onOpenBrowseSettings = { showBrowseSettings = true },
                        onOpenSearch = { showSearchScreen = true },
                        onRequireLogin = onRequireLogin,
                        onOpenDetail = { id -> selectedMemeId = id },
                        onOpenQuickMenu = { m -> quickActionMeme = m },
                        onMemeUpdated = { updated ->
                            memes = memes.map { if (it.id == updated.id) updated else it }
                        },
                        onToggleLike = { meme ->
                            if (me == null) {
                                onRequireLogin()
                            } else {
                                scope.launch {
                                    api.toggleLike(meme.id)
                                    memes = memes.map { cur ->
                                        if (cur.id == meme.id) {
                                            cur.copy(
                                                liked = !cur.liked,
                                                likes = if (cur.liked) maxOf(0, cur.likes - 1) else cur.likes + 1,
                                                likeCount = if (cur.liked) maxOf(0, cur.likeCount - 1) else cur.likeCount + 1,
                                            )
                                        } else cur
                                    }
                                }
                            }
                        },
                    )
                } else {
                    val columns = if (filter.viewMode == "single") 1 else 2
                    PullToRefreshBox(
                        isRefreshing = refreshing,
                        onRefresh = { reload(filter, asRefresh = true) },
                        modifier = Modifier
                            .fillMaxSize()
                            .nestedScroll(nestedScrollConnection),
                    ) {
                        LazyVerticalStaggeredGrid(
                            state = gridState,
                            columns = StaggeredGridCells.Fixed(columns),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                horizontal = if (columns == 1) 12.dp else 8.dp,
                                vertical = 8.dp,
                            ),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalItemSpacing = 8.dp,
                        ) {
                            items(
                                items = entries,
                                key = { entry ->
                                    when (entry) {
                                        is WallFeedEntry.DayHeader -> "day_${entry.day}"
                                        is WallFeedEntry.MemeCard -> "meme_${entry.meme.id}"
                                    }
                                },
                                span = { entry ->
                                    when (entry) {
                                        is WallFeedEntry.DayHeader -> StaggeredGridItemSpan.FullLine
                                        is WallFeedEntry.MemeCard -> StaggeredGridItemSpan.SingleLane
                                    }
                                },
                            ) { entry ->
                                when (entry) {
                                    is WallFeedEntry.DayHeader -> {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(36.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { showDayJumpSheet = true }
                                                .padding(horizontal = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            val suffix = buildString {
                                                append(" · ${entry.totalCount} 帖")
                                                if (entry.unseenCount > 0) append(" · 未看 ${entry.unseenCount}")
                                            }
                                            Text(
                                                text = entry.label + suffix,
                                                color = palette.text,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                style = TextStyle(fontFeatureSettings = TABULAR_FONT_FEATURE),
                                            )
                                            Icon(
                                                imageVector = Icons.Rounded.CalendarMonth,
                                                contentDescription = "跳到哪天",
                                                tint = palette.muted,
                                                modifier = Modifier.size(18.dp),
                                            )
                                        }
                                    }

                                    is WallFeedEntry.MemeCard -> {
                                        CleanMemeCard(
                                            meme = entry.meme,
                                            api = api,
                                            me = me,
                                            filter = filter,
                                            isSingleColumn = columns == 1,
                                            blurAllEnabled = privacyConfig.blurAllEnabled,
                                            viewportRect = feedViewportRect,
                                            scrollSpeedDpPerSec = scrollSpeedDpPerSec,
                                            showPeekHint = peekHintCount < 3,
                                            onIncrementPeekHint = {
                                                if (peekHintCount < 3) {
                                                    val next = peekHintCount + 1
                                                    peekHintCount = next
                                                    scope.launch { api.putPref(KV_PEEK_HINT_COUNT, next.toString()) }
                                                }
                                            },
                                            onRequireLogin = onRequireLogin,
                                            onOpenDetail = { selectedMemeId = entry.meme.id },
                                            onOpenFullscreen = { immersiveViewerTarget = entry.meme },
                                            onLongPress = { quickActionMeme = entry.meme },
                                            onMemeUpdated = { updated ->
                                                memes = memes.map { if (it.id == updated.id) updated else it }
                                            },
                                            onToggleLike = {
                                                if (me == null) {
                                                    onRequireLogin()
                                                } else {
                                                    scope.launch {
                                                        api.toggleLike(entry.meme.id)
                                                        memes = memes.map { cur ->
                                                            if (cur.id == entry.meme.id) {
                                                                cur.copy(
                                                                    liked = !cur.liked,
                                                                    likes = if (cur.liked) maxOf(0, cur.likes - 1) else cur.likes + 1,
                                                                    likeCount = if (cur.liked) maxOf(0, cur.likeCount - 1) else cur.likeCount + 1,
                                                                )
                                                            } else cur
                                                        }
                                                    }
                                                }
                                            },
                                        )
                                    }
                                }
                            }

                            item(span = StaggeredGridItemSpan.FullLine) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 14.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    when {
                                        loadingMore -> CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = palette.primary)
                                        hasMore -> OutlinedButton(onClick = { loadMore() }) {
                                            Text("加载更多", fontSize = 12.sp)
                                        }
                                        else -> Text("没有更多了", color = palette.muted, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // Floating return-to-today / top pill (`02a §4–5`)
                val showBackToday = filter.fromDay != null || filter.focusMemeId != null || showJumpTopPill
                if (showBackToday && filter.viewMode != "pager") {
                    Surface(
                        color = palette.primary,
                        shape = RoundedCornerShape(999.dp),
                        shadowElevation = 3.dp,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 10.dp)
                            .clickable {
                                if (filter.fromDay != null || filter.focusMemeId != null) {
                                    applyFilter(filter.copy(fromDay = null, focusMemeId = null))
                                } else {
                                    scope.launch {
                                        gridState.animateScrollToItem(0)
                                        showJumpTopPill = false
                                    }
                                }
                            },
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(Icons.Rounded.ArrowUpward, contentDescription = null, tint = palette.onPrimary, modifier = Modifier.size(14.dp))
                            Text(
                                text = if (filter.fromDay != null || filter.focusMemeId != null) "回到今天" else "最新",
                                color = palette.onPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }

                inlineToast?.let { msg ->
                    Surface(
                        color = Color.Black.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 18.dp),
                    ) {
                        Text(msg, color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp))
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = fabVisible && filter.viewMode != "pager",
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        ) {
            Box {
                Surface(
                    color = palette.primary,
                    contentColor = palette.onPrimary,
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .combinedClickable(
                            onClick = {
                                if (me == null) {
                                    onRequireLogin()
                                } else if (onOpenCompose != null) {
                                    onOpenCompose(
                                        ComposeLaunchRequest(
                                            triggerSeq = System.currentTimeMillis(),
                                            initialGenre = ComposeGenre.IMAGE,
                                            autoOpenPicker = true,
                                        ),
                                    )
                                } else {
                                    showCompose = true
                                }
                            },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (me == null) {
                                    onRequireLogin()
                                } else {
                                    showFabMenu = true
                                }
                            },
                        ),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Add, contentDescription = "投稿")
                    }
                }

                DropdownMenu(
                    expanded = showFabMenu,
                    onDismissRequest = { showFabMenu = false },
                ) {
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        text = { Text("图文") },
                        onClick = {
                            showFabMenu = false
                            val req = ComposeLaunchRequest(
                                triggerSeq = System.currentTimeMillis(),
                                initialGenre = ComposeGenre.IMAGE,
                                autoOpenPicker = true,
                            )
                            if (onOpenCompose != null) onOpenCompose(req) else showCompose = true
                        },
                    )
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        text = { Text("文字长帖") },
                        onClick = {
                            showFabMenu = false
                            val req = ComposeLaunchRequest(
                                triggerSeq = System.currentTimeMillis(),
                                initialGenre = ComposeGenre.TEXT,
                            )
                            if (onOpenCompose != null) onOpenCompose(req) else showCompose = true
                        },
                    )
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Rounded.Link, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        text = { Text("粘贴链接分享") },
                        onClick = {
                            showFabMenu = false
                            val detected = SocialLinkDetector.detectFirstSocialLink(clipboard.getText()?.text)
                            val req = ComposeLaunchRequest(
                                triggerSeq = System.currentTimeMillis(),
                                initialGenre = ComposeGenre.LINK,
                                initialShareUrl = detected?.url ?: clipboard.getText()?.text?.trim().orEmpty(),
                            )
                            if (onOpenCompose != null) onOpenCompose(req) else showCompose = true
                        },
                    )
                }
            }
        }
    }

    if (showBrowseSettings) {
        WallBrowseSettingsSheet(
            current = filter,
            blurAllEnabled = privacyConfig.blurAllEnabled,
            anchors = anchors,
            api = api,
            onToggleBlurAll = onToggleBlurAll,
            onJumpAnchor = { anchor ->
                applyFilter(
                    filter.copy(
                        fromDay = anchor.day.takeIf { it.isNotBlank() },
                        focusMemeId = anchor.id,
                    ),
                )
            },
            onAnchorsChanged = { anchors = it },
            onOpenAuthorTagFilter = { showSearchScreen = true },
            onApply = { next ->
                applyFilter(next)
                showBrowseSettings = false
            },
            onDismiss = { showBrowseSettings = false },
        )
    }

    if (showDayJumpSheet) {
        WallDayJumpSheet(
            days = daysMeta,
            currentFromDay = filter.fromDay,
            onSelectDay = { isoDay ->
                applyFilter(filter.copy(fromDay = isoDay, focusMemeId = null))
            },
            onDismiss = { showDayJumpSheet = false },
        )
    }

    if (showSearchScreen) {
        WallSearchScreen(
            api = api,
            currentFilter = filter,
            blurAllEnabled = privacyConfig.blurAllEnabled,
            popularTags = popularTags,
            onOpenMeme = { id -> selectedMemeId = id },
            onApplyFilter = { next -> applyFilter(next) },
            onDismiss = { showSearchScreen = false },
        )
    }

    if (showSeriesIndexDialog) {
        SeriesIndexDialog(
            api = api,
            currentFilter = filter,
            blurAllEnabled = privacyConfig.blurAllEnabled,
            onSelectSeries = { sName ->
                showSeriesIndexDialog = false
                applyFilter(filter.copy(series = sName, tags = emptySet(), author = ""))
            },
            onDismiss = { showSeriesIndexDialog = false },
        )
    }

    quickActionMeme?.let { qMeme ->
        MemeQuickActionsSheet(
            meme = qMeme,
            api = api,
            me = me,
            blurThumbnail = filter.shouldBlurMeme(qMeme, privacyConfig.blurAllEnabled),
            showQuickInteractions = true,
            onRequireLogin = onRequireLogin,
            onMemeUpdated = { updated ->
                memes = memes.map { if (it.id == updated.id) updated else it }
            },
            onDeleted = { deletedId ->
                memes = memes.filterNot { it.id == deletedId }
                inlineToast = "已删除帖子"
            },
            onAnchorsUpdated = { anchors = it },
            onOpenUrl = onOpenUrl,
            onStatusToast = { inlineToast = it },
            onOpenEditWorkbench = onOpenCompose?.let { openCompose ->
                { targetMeme ->
                    openCompose(
                        ComposeLaunchRequest(
                            triggerSeq = System.currentTimeMillis(),
                            editingMeme = targetMeme,
                        ),
                    )
                }
            },
            onDismiss = { quickActionMeme = null },
        )
    }

    selectedMemeId?.let { id ->
        MemeDetailModal(
            memeId = id,
            api = api,
            me = me,
            onRequireLogin = onRequireLogin,
            onDismiss = { selectedMemeId = null },
            onOpenImageFullscreen = { url -> detailInlineFullscreenUrl = url },
            onTagClick = { tag -> applyFilter(filter.copy(tags = setOf(tag))) },
            onAuthorClick = { uname -> applyFilter(filter.copy(author = uname)) },
            onStateUpdated = { updated ->
                memes = memes.map { if (it.id == updated.id) updated else it }
            },
            onDeleted = { deletedId ->
                memes = memes.filterNot { it.id == deletedId }
                inlineToast = "已删除帖子"
            },
            onAnchorsUpdated = { anchors = it },
            onOpenUrl = onOpenUrl,
            onOpenEditWorkbench = onOpenCompose?.let { openCompose ->
                { targetMeme ->
                    selectedMemeId = null
                    openCompose(
                        ComposeLaunchRequest(
                            triggerSeq = System.currentTimeMillis(),
                            editingMeme = targetMeme,
                        ),
                    )
                }
            },
        )
    }

    detailInlineFullscreenUrl?.let { url ->
        FullscreenMediaOverlay(
            imageUrl = url,
            api = api,
            onDismiss = { detailInlineFullscreenUrl = null },
        )
    }

    immersiveViewerTarget?.let { target ->
        ImmersiveMediaStage(
            mediaItems = target.mediaItems,
            initialIndex = 0,
            api = api,
            meme = target,
            forceBlurAll = privacyConfig.blurAllEnabled,
            onLikeToggle = {
                if (me == null) {
                    onRequireLogin()
                } else {
                    scope.launch {
                        api.toggleLike(target.id)
                        memes = memes.map { cur ->
                            if (cur.id == target.id) {
                                cur.copy(
                                    liked = !cur.liked,
                                    likes = if (cur.liked) maxOf(0, cur.likes - 1) else cur.likes + 1,
                                    likeCount = if (cur.liked) maxOf(0, cur.likeCount - 1) else cur.likeCount + 1,
                                )
                            } else cur
                        }
                    }
                }
            },
            onOpenReactions = {
                immersiveViewerTarget = null
                selectedMemeId = target.id
            },
            onOpenQuickReplies = {
                immersiveViewerTarget = null
                selectedMemeId = target.id
            },
            onOpenComments = {
                immersiveViewerTarget = null
                selectedMemeId = target.id
            },
            onDismiss = { immersiveViewerTarget = null },
        )
    }

    if (showCompose) {
        ComposeMemeModal(
            api = api,
            onDismiss = { showCompose = false },
            onPublished = { reload(filter) },
        )
    }

    activeNoticeDetail?.let { n ->
        AlertDialog(
            onDismissRequest = { activeNoticeDetail = null },
            containerColor = palette.surface,
            title = { Text(if (n.kind == "daily") "今日公告" else "网站公告", color = palette.text, fontWeight = FontWeight.Bold) },
            text = {
                RichChatBodyView(
                    rawBody = n.body,
                    imageLoader = api.imageLoader,
                    textColor = palette.text,
                    onPostRefClick = { refId ->
                        activeNoticeDetail = null
                        selectedMemeId = refId
                    },
                    onHashtagClick = { tag ->
                        activeNoticeDetail = null
                        applyFilter(filter.copy(tags = setOf(tag)))
                    },
                    onLinkClick = { url ->
                        activeNoticeDetail = null
                        onOpenUrl(url)
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = { activeNoticeDetail = null }) { Text("知道了") }
            },
        )
    }
}

@Composable
private fun ActiveHeaderFilterChip(
    label: String,
    onClear: () -> Unit,
) {
    val palette = LocalForHappyPalette.current
    Surface(
        color = palette.primaryContainer,
        shape = RoundedCornerShape(999.dp),
        modifier = Modifier.clickable { onClear() },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(label, color = palette.primary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Icon(Icons.Rounded.Close, contentDescription = "清除", tint = palette.primary, modifier = Modifier.size(14.dp))
        }
    }
}

/**
 * Clean, distraction-free gallery card (`02a §1.1`, `§6`, `§7`):
 * - Strictly ZERO seen badges, ZERO unseen dots, ZERO post numbers, ZERO timestamps, ZERO admin buttons
 * - Max media height 1.8× width in waterfall (`长图 ▤` pill), multi-image `1/4`, `视频`/`LIVE`/`GIF` badges
 * - Supports peek vs view mask tap, <50% visible & ON_STOP auto-reblur, >=60% + slow-scroll 1s seen dwell
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun CleanMemeCard(
    meme: MemeItem,
    api: ForHappyApiClient,
    me: MeUser?,
    filter: WallFilterState,
    isSingleColumn: Boolean,
    blurAllEnabled: Boolean,
    viewportRect: RectF,
    scrollSpeedDpPerSec: Float,
    showPeekHint: Boolean,
    onIncrementPeekHint: () -> Unit,
    onRequireLogin: () -> Unit,
    onOpenDetail: () -> Unit,
    onOpenFullscreen: () -> Unit,
    onLongPress: () -> Unit,
    onMemeUpdated: (MemeItem) -> Unit,
    onToggleLike: () -> Unit,
) {
    val palette = LocalForHappyPalette.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    var revealed by remember(meme.id, blurAllEnabled, filter.nsfwMode) { mutableStateOf(false) }
    var useExtFallback by remember(meme.id) { mutableStateOf(false) }
    var visibleRatio by remember(meme.id) { mutableFloatStateOf(0f) }

    val primaryMedia = remember(meme) { meme.mediaItems.firstOrNull() }
    val rawUrl = remember(meme.id, isSingleColumn, useExtFallback) {
        val candidate = if (isSingleColumn) {
            primaryMedia?.resolvedFullUrl ?: meme.primaryImageUrl
        } else {
            meme.primaryImageUrl
        }
        val rewritten = MediaUrlRewriter.rewriteMediaUrl(candidate)
        if (useExtFallback && rewritten.isNotBlank()) MediaUrlRewriter.externalFallbackUrl(rewritten) else rewritten
    }

    var mediaReady by remember(rawUrl) { mutableStateOf(rawUrl.isBlank()) }
    val hasVisual = rawUrl.isNotBlank()
    val shouldMask = hasVisual && filter.shouldBlurMeme(meme, blurAllEnabled)
    val isBlurred = shouldMask && !revealed

    // Auto-reblur when revealed card scrolls < 50% visible OR leaves composition / foreground (`02a §7`)
    LaunchedEffect(revealed, visibleRatio) {
        if (shouldAutoReblurRevealed(revealed, visibleRatio)) {
            revealed = false
        }
    }
    DisposableEffect(lifecycleOwner, meme.id) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                revealed = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            revealed = false
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    ObserveMemeSeenDwell(
        memeId = meme.id,
        seenAt = meme.seenAt,
        memeDay = meme.day,
        isMediaReady = mediaReady,
        isBlurred = isBlurred,
        visibleRatio = visibleRatio,
        scrollSpeedDpPerSec = scrollSpeedDpPerSec,
    )

    val naturalRatio = remember(meme.effectiveWidth, meme.effectiveHeight) {
        val w = meme.effectiveWidth ?: 0
        val h = meme.effectiveHeight ?: 0
        if (w > 0 && h > 0) w.toFloat() / h.toFloat() else null
    }
    val clampedAspect = remember(naturalRatio, isSingleColumn) {
        when {
            naturalRatio != null && !isSingleColumn -> naturalRatio.coerceIn(1f / 1.8f, 2.2f)
            naturalRatio != null && isSingleColumn -> naturalRatio.coerceIn(0.42f, 2.4f)
            isSingleColumn -> null
            else -> 1f
        }
    }
    val isCroppedLong = naturalRatio != null && (1f / naturalRatio) >= 1.8f

    fun handleCardTap() {
        if (isBlurred) {
            if (filter.maskTap == "view") {
                onOpenFullscreen()
            } else {
                revealed = true
                onIncrementPeekHint()
            }
        } else if (shouldMask && revealed) {
            revealed = false
        } else {
            onOpenDetail()
        }
    }

    Surface(
        color = palette.surface,
        shape = RoundedCornerShape(ForHappyDimens.radiusM),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, palette.line.copy(alpha = 0.7f), RoundedCornerShape(ForHappyDimens.radiusM))
            .onGloballyPositioned { coords ->
                val b = coords.boundsInWindow()
                visibleRatio = computeVisibleAreaRatio(b.top, b.bottom, viewportRect.top, viewportRect.bottom)
            }
            .combinedClickable(
                onClick = { handleCardTap() },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongPress()
                },
            ),
    ) {
        Column {
            if (hasVisual) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .let { mod ->
                            if (clampedAspect != null) mod.aspectRatio(clampedAspect)
                            else mod.heightIn(min = 160.dp, max = 520.dp)
                        }
                        .background(palette.surface2),
                    contentAlignment = Alignment.Center,
                ) {
                    AsyncImage(
                        model = rawUrl,
                        imageLoader = api.imageLoader,
                        contentDescription = meme.safeLabel,
                        contentScale = if (isSingleColumn && !isCroppedLong) ContentScale.FillWidth else ContentScale.Crop,
                        onSuccess = { mediaReady = true },
                        onError = {
                            if (!useExtFallback && !rawUrl.contains("/ext?u=")) {
                                useExtFallback = true
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .let { if (isBlurred) it.blur(32.dp) else it },
                    )

                    if (isBlurred) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(palette.blurOverlay),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("◌ 点按查看", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                        if (meme.isRisky) {
                            Surface(
                                color = Color.Black.copy(alpha = 0.62f),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(6.dp),
                            ) {
                                Text("敏感", color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    } else {
                        if (shouldMask && revealed && showPeekHint) {
                            Text(
                                text = "◐",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp),
                            )
                        }

                        // Media corner badges (`1/4`, `GIF` / `视频` / `LIVE`, `长图 ▤`)
                        val totalImages = maxOf(meme.imageCount, meme.mediaItems.size)
                        val kindLabel = when {
                            primaryMedia?.isVideo == true -> "▶ 视频"
                            primaryMedia?.isLive == true -> "LIVE"
                            primaryMedia?.isGif == true -> "GIF"
                            totalImages > 1 -> "1/$totalImages"
                            else -> null
                        }
                        if (kindLabel != null) {
                            Surface(
                                color = Color.Black.copy(alpha = 0.60f),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp),
                            ) {
                                Text(
                                    text = kindLabel,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    style = TextStyle(fontFeatureSettings = TABULAR_FONT_FEATURE),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                        }
                        if (isCroppedLong) {
                            Surface(
                                color = Color.Black.copy(alpha = 0.60f),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(6.dp),
                            ) {
                                Text("长图 ▤", color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }
                }
            } else {
                // Pure-text card (`📝 文字帖` + first 4 lines)
                val bodyPreview = meme.text?.takeIf { it.isNotBlank() && it != meme.title }
                    ?: meme.content?.takeIf { it.isNotBlank() && it != meme.title }
                    ?: meme.body.orEmpty()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.surface2.copy(alpha = 0.55f))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text("📝 文字帖", color = palette.primary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    if (bodyPreview.isNotBlank()) {
                        Text(
                            text = bodyPreview,
                            color = palette.text,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            // Below media: suppress extra details when blurred & risky (title + minimal footer only per `02a §7`)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val showTitleText = meme.title?.takeIf { it.isNotBlank() }
                    ?: if (hasVisual) meme.text?.takeIf { it.isNotBlank() } else null
                if (!showTitleText.isNullOrBlank()) {
                    Text(
                        text = showTitleText,
                        color = palette.text,
                        fontSize = 14.sp,
                        lineHeight = 19.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (!isBlurred) {
                    // `02a §1.1`: <= 2 hottest quick replies
                    val quickReplies = remember(meme.replyList, meme.replies, meme.myReply, me?.username) {
                        meme.effectiveQuickReplies(me).sortedByDescending { it.n }
                    }
                    val shownBoosts = if (isSingleColumn) quickReplies.take(4) else quickReplies.take(2)
                    if (shownBoosts.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            shownBoosts.forEach { qr ->
                                Surface(
                                    color = if (qr.mine) palette.primaryContainer else palette.primary.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(999.dp),
                                    modifier = Modifier
                                        .border(
                                            1.dp,
                                            if (qr.mine) palette.primary else palette.primary.copy(alpha = 0.25f),
                                            RoundedCornerShape(999.dp),
                                        )
                                        .clickable {
                                            if (me == null) {
                                                onRequireLogin()
                                            } else {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                scope.launch {
                                                    val res = if (qr.mine) api.deleteMemeQuickReply(meme.id) else api.putMemeQuickReply(meme.id, qr.text)
                                                    res.getOrNull()?.let(onMemeUpdated)
                                                }
                                            }
                                        },
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    ) {
                                        Text("「", color = palette.primary, fontSize = 10.sp)
                                        BoostPillText(text = qr.text, imageLoader = api.imageLoader, fontSize = 11.sp)
                                        Text("」", color = palette.primary, fontSize = 10.sp)
                                        if (qr.n > 1) {
                                            Text(
                                                text = "×${qr.n}",
                                                color = palette.primary,
                                                fontSize = 10.sp,
                                                style = TextStyle(fontFeatureSettings = TABULAR_FONT_FEATURE),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // `02a §1.1`: <= 3 sticker reactions in dual waterfall (expanded in single column)
                    val usedReactions = remember(meme.reactions) {
                        meme.reactions.filter { it.count > 0 }
                            .sortedWith(compareByDescending<com.jinhuan.forhappy.core.model.ChatReaction> { it.count }.thenByDescending { it.mine })
                    }
                    val shownRx = if (isSingleColumn) usedReactions.take(6) else usedReactions.take(3)
                    if (shownRx.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            shownRx.forEach { rx ->
                                ReactionChip(
                                    emojiKey = rx.emoji,
                                    count = rx.count,
                                    mine = rx.mine,
                                    imageLoader = api.imageLoader,
                                    compact = true,
                                    onClick = {
                                        if (me == null) {
                                            onRequireLogin()
                                        } else {
                                            scope.launch { api.reactToMeme(meme.id, rx.emoji)?.let(onMemeUpdated) }
                                        }
                                    },
                                )
                            }
                        }
                    }
                }

                // Author + Direct Heart Like (`@author` + `♡ N`) + single-col `评论 N` (`02a §1.2`)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "@${meme.authorDisplay}",
                        color = palette.muted,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (isSingleColumn && meme.totalComments > 0) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                modifier = Modifier.clickable { onOpenDetail() },
                            ) {
                                Icon(Icons.Rounded.ChatBubbleOutline, contentDescription = "评论", tint = palette.muted, modifier = Modifier.size(14.dp))
                                Text(
                                    text = "评论 ${meme.totalComments}",
                                    color = palette.muted,
                                    fontSize = 12.sp,
                                    style = TextStyle(fontFeatureSettings = TABULAR_FONT_FEATURE),
                                )
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onToggleLike()
                                }
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                        ) {
                            Icon(
                                imageVector = if (meme.liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = "赞",
                                tint = if (meme.liked) palette.like else palette.muted,
                                modifier = Modifier.size(15.dp),
                            )
                            Text(
                                text = meme.totalLikes.toString(),
                                color = if (meme.liked) palette.like else palette.muted,
                                fontSize = 12.sp,
                                style = TextStyle(fontFeatureSettings = TABULAR_FONT_FEATURE),
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Full-screen Immersive Vertical Pager mode (`02a §1.2`):
 * One post per screen with vertical snap, double-tap to like, single-tap to toggle chrome,
 * and right-edge thumb action rail (`♡ / ☺ / 「」 / 💬 / ⋯`).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WallImmersivePagerFeed(
    memes: List<MemeItem>,
    api: ForHappyApiClient,
    me: MeUser?,
    filter: WallFilterState,
    blurAllEnabled: Boolean,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
    onOpenBrowseSettings: () -> Unit,
    onOpenSearch: () -> Unit,
    onRequireLogin: () -> Unit,
    onOpenDetail: (Long) -> Unit,
    onOpenQuickMenu: (MemeItem) -> Unit,
    onMemeUpdated: (MemeItem) -> Unit,
    onToggleLike: (MemeItem) -> Unit,
) {
    val palette = LocalForHappyPalette.current
    val haptic = LocalHapticFeedback.current
    val pagerState = rememberPagerState(pageCount = { memes.size })
    var showChrome by remember { mutableStateOf(true) }

    LaunchedEffect(pagerState.currentPage, memes.size, hasMore) {
        if (hasMore && memes.isNotEmpty() && pagerState.currentPage >= memes.size - 4) {
            onLoadMore()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            val meme = memes[page]
            val mediaList = remember(meme) { meme.mediaItems }
            var revealed by remember(meme.id, blurAllEnabled) { mutableStateOf(false) }
            var showHeartBurst by remember { mutableStateOf(false) }
            var mediaReady by remember(meme.id) { mutableStateOf(mediaList.isEmpty()) }
            val shouldMask = mediaList.isNotEmpty() && filter.shouldBlurMeme(meme, blurAllEnabled)
            val isBlurred = shouldMask && !revealed

            ObserveMemeSeenDwell(
                memeId = meme.id,
                seenAt = meme.seenAt,
                memeDay = meme.day,
                isMediaReady = mediaReady,
                isBlurred = isBlurred,
                visibleRatio = if (page == pagerState.currentPage) 1f else 0f,
                scrollSpeedDpPerSec = 0f,
            )

            LaunchedEffect(showHeartBurst) {
                if (showHeartBurst) {
                    delay(650L)
                    showHeartBurst = false
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(meme.id, isBlurred) {
                        detectTapGestures(
                            onTap = {
                                if (isBlurred) revealed = true else showChrome = !showChrome
                            },
                            onDoubleTap = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showHeartBurst = true
                                if (!meme.liked) onToggleLike(meme)
                            },
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (mediaList.isNotEmpty()) {
                    val innerPager = rememberPagerState(pageCount = { mediaList.size })
                    HorizontalPager(
                        state = innerPager,
                        modifier = Modifier.fillMaxSize(),
                    ) { imgIdx ->
                        val item = mediaList[imgIdx]
                        val url = remember(item) { MediaUrlRewriter.rewriteMediaUrl(item.resolvedFullUrl) }
                        AsyncImage(
                            model = url,
                            imageLoader = api.imageLoader,
                            contentDescription = meme.safeLabel,
                            contentScale = ContentScale.Fit,
                            onSuccess = { mediaReady = true },
                            modifier = Modifier
                                .fillMaxSize()
                                .let { if (isBlurred) it.blur(32.dp) else it },
                        )
                    }
                } else {
                    Text(
                        text = meme.content ?: meme.text ?: meme.safeLabel,
                        color = Color.White,
                        fontSize = 18.sp,
                        lineHeight = 27.sp,
                        modifier = Modifier.padding(horizontal = 24.dp),
                    )
                }

                if (isBlurred) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(palette.blurOverlay),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("◌ 点按查看", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }

                AnimatedVisibility(
                    visible = showHeartBurst,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut(),
                ) {
                    Icon(Icons.Rounded.Favorite, contentDescription = null, tint = palette.like, modifier = Modifier.size(72.dp))
                }

                if (showChrome) {
                    // Bottom info overlay
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f))))
                            .padding(start = 16.dp, end = 76.dp, top = 28.dp, bottom = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text("@${meme.authorDisplay}", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text(meme.safeLabel, color = Color.White, fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }

                    // Right-side vertical thumb action column (`♡ / ☺ / 「」 / 💬 / ⋯`)
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 12.dp, bottom = 22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        PagerActionButton(
                            icon = if (meme.liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            tint = if (meme.liked) palette.like else Color.White,
                            label = "${meme.totalLikes}",
                        ) { onToggleLike(meme) }

                        PagerActionButton(
                            icon = Icons.Rounded.AddReaction,
                            tint = Color.White,
                            label = "${meme.reactions.sumOf { it.count }}",
                        ) {
                            if (me == null) onRequireLogin() else onOpenDetail(meme.id)
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable {
                                if (me == null) onRequireLogin() else onOpenDetail(meme.id)
                            },
                        ) {
                            Text("「」", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("${meme.replyCount}", color = Color.White, fontSize = 11.sp)
                        }

                        PagerActionButton(
                            icon = Icons.Rounded.ChatBubbleOutline,
                            tint = Color.White,
                            label = "${meme.totalComments}",
                        ) { onOpenDetail(meme.id) }

                        IconButton(onClick = { onOpenQuickMenu(meme) }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Rounded.MoreHoriz, contentDescription = "更多", tint = Color.White)
                        }
                    }
                }
            }
        }

        if (showChrome) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.56f),
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier.clickable { onOpenBrowseSettings() },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(filter.summaryCapsuleLabel(), color = Color.White, fontSize = 12.sp)
                        Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
                IconButton(onClick = onOpenSearch) {
                    Icon(Icons.Rounded.Search, contentDescription = "搜索", tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun PagerActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    label: String,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() },
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(26.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 11.sp,
            style = TextStyle(fontFeatureSettings = TABULAR_FONT_FEATURE),
        )
    }
}

private fun buildFeedEntries(
    memes: List<MemeItem>,
    daysMeta: List<DayEntry>,
    byDay: Boolean,
): List<WallFeedEntry> {
    if (!byDay) return memes.map { WallFeedEntry.MemeCard(it) }
    val dayLookup = daysMeta.associateBy { it.day }
    val out = ArrayList<WallFeedEntry>(memes.size + 8)
    var currentDay: String? = null
    for (m in memes) {
        val day = m.day?.takeIf { it.isNotBlank() }
        if (day != null && day != currentDay) {
            currentDay = day
            val meta = dayLookup[day]
            out.add(
                WallFeedEntry.DayHeader(
                    day = day,
                    label = formatDaySectionLabel(day),
                    totalCount = meta?.totalCount ?: 0,
                    unseenCount = meta?.unseen ?: 0,
                ),
            )
        }
        out.add(WallFeedEntry.MemeCard(m))
    }
    return out
}

private fun formatDaySectionLabel(dayIso: String): String {
    val parsed = runCatching { LocalDate.parse(dayIso) }.getOrNull() ?: return dayIso
    val today = Instant.ofEpochSecond(ServerClock.serverNowSec())
        .atZone(ZoneId.of("Asia/Shanghai"))
        .toLocalDate()
    return when (parsed) {
        today -> "今天"
        today.minusDays(1) -> "昨天"
        else -> {
            val dow = parsed.dayOfWeek.getDisplayName(JavaTextStyle.SHORT, Locale.CHINA)
            "${parsed.monthValue}月${parsed.dayOfMonth}日 $dow"
        }
    }
}
