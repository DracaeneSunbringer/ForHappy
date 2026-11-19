package com.jinhuan.forhappy.ui.disguise

import android.graphics.RectF
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Reply
import androidx.compose.material.icons.automirrored.rounded.Send
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
import androidx.compose.material.icons.rounded.Menu
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jinhuan.forhappy.BuildConfig
import com.jinhuan.forhappy.core.chat.ChatSessionController
import com.jinhuan.forhappy.core.compose.ComposeTrayItem
import com.jinhuan.forhappy.core.model.AppDisguiseMode
import com.jinhuan.forhappy.core.model.AppIconAlias
import com.jinhuan.forhappy.core.model.AppThemeMode
import com.jinhuan.forhappy.core.model.ChatGroup
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
import com.jinhuan.forhappy.ui.components.ReactionChip
import com.jinhuan.forhappy.ui.components.RichChatBodyView
import com.jinhuan.forhappy.ui.compose.ComposeMediaStager
import com.jinhuan.forhappy.ui.compose.ImageMaskEditorScreen
import com.jinhuan.forhappy.ui.theme.LocalBossKeyActive
import com.jinhuan.forhappy.ui.theme.LocalForHappyPalette
import com.jinhuan.forhappy.ui.theme.TABULAR_FONT_FEATURE
import com.jinhuan.forhappy.ui.wall.ConfigureFullscreenDialogWindow
import com.jinhuan.forhappy.ui.wall.ImmersiveMediaStage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Discourse topic metadata (`docs/redesign/03-privacy-state-machine.md` §5.4 & §5.7):
 * Strictly free of forbidden giveaway keywords.
 */
internal object LinuxDoTopicMeta {
    const val TITLE = "【持续更新】日常收集的图文与素材分享楼，欢迎佬友补充"
    val TAGS = listOf("资源分享", "持续更新")
}

private data class LinuxDoColors(
    val bg: Color,
    val surface: Color,
    val surfaceAlt: Color,
    val line: Color,
    val accent: Color,
    val text: Color,
    val muted: Color,
    val danger: Color,
)

@Composable
private fun rememberLinuxDoColors(): LinuxDoColors {
    val isDark = LocalForHappyPalette.current.isDark
    return remember(isDark) {
        if (isDark) {
            LinuxDoColors(
                bg = Color(0xFF1A1D23),
                surface = Color(0xFF22262E),
                surfaceAlt = Color(0xFF2B303A),
                line = Color(0xFF343A46),
                accent = Color(0xFF409EFF),
                text = Color(0xFFE6EAF0),
                muted = Color(0xFF9098A6),
                danger = Color(0xFFE05252),
            )
        } else {
            LinuxDoColors(
                bg = Color(0xFFF6F7F9),
                surface = Color(0xFFFFFFFF),
                surfaceAlt = Color(0xFFF0F2F5),
                line = Color(0xFFE5E7EB),
                accent = Color(0xFF0088CC),
                text = Color(0xFF1C1E21),
                muted = Color(0xFF687078),
                danger = Color(0xFFD93838),
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

private fun linuxDoFloorBody(meme: MemeItem): String {
    val raw = listOfNotNull(meme.title, meme.content, meme.text, meme.caption, meme.body)
        .firstOrNull { it.isNotBlank() }
        .orEmpty()
        .trim()
    if (raw.isBlank()) return "分享一张日常收藏素材，供大家参考。"
    return DisguiseSanitizer.sanitizeSummaryForDisguise(raw, AppDisguiseMode.LINUX_DO)
        .replace("欢友", "佬友")
        .ifBlank { "分享一张日常收藏素材，供大家参考。" }
}

@Composable
private fun LinuxDoLogoMark(color: Color) {
    Canvas(modifier = Modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        drawRoundRect(
            color = color,
            topLeft = Offset.Zero,
            size = Size(w, h),
            cornerRadius = CornerRadius(w * 0.25f, w * 0.25f),
        )
        drawCircle(
            color = Color.White,
            radius = w * 0.24f,
            center = Offset(w * 0.5f, h * 0.5f),
        )
    }
}

/**
 * Zero-giveaway LINUX DO Discourse long-topic disguise matching
 * `docs/redesign/03-privacy-state-machine.md` §5.4–5.7.
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
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val bossKeyActive = LocalBossKeyActive.current
    val seenTracker = LocalSeenTracker.current
    val chatState by chatController.uiState.collectAsState()

    var floors by remember { mutableStateOf<List<MemeItem>>(emptyList()) }
    var tagsList by remember { mutableStateOf<List<TagStatItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
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

    fun reloadFloors(reset: Boolean = true) {
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
            val page = api.fetchMemesPage(
                limit = 30,
                offset = if (reset) 0 else floors.size,
                page = targetPage,
                tag = selectedTag,
                query = searchQuery.trim().takeIf { it.isNotBlank() },
                sort = "new",
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
        tagsList = api.fetchTags()
    }

    LaunchedEffect(selectedTag, unseenOnly, searchQuery) {
        reloadFloors(reset = true)
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
    val totalFloors = maxOf(1, floors.size)
    val totalChatUnread = remember(chatState.groups, chatState.dmThreads) {
        chatState.groups.sumOf { it.unread } + chatState.dmThreads.sumOf { it.unread }
    }

    BackHandler(enabled = showLeftDrawer || showChatDrawer || showPrefsOverlay || showTimeline) {
        when {
            showPrefsOverlay -> showPrefsOverlay = false
            showChatDrawer -> showChatDrawer = false
            showLeftDrawer -> showLeftDrawer = false
            showTimeline -> showTimeline = false
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
            // Discourse Top Header
            Surface(color = lc.surface, shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .padding(horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        IconButton(onClick = { showLeftDrawer = !showLeftDrawer }) {
                            Icon(Icons.Rounded.Menu, contentDescription = "版块与显示设置", tint = lc.text)
                        }
                        LinuxDoLogoMark(color = lc.accent)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "LINUX DO",
                            color = lc.text,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(
                            onClick = {
                                showSearchBar = !showSearchBar
                                if (!showSearchBar) searchQuery = ""
                            },
                        ) {
                            Icon(Icons.Rounded.Search, contentDescription = "搜索楼层", tint = lc.text)
                        }
                        Box(contentAlignment = Alignment.TopEnd) {
                            IconButton(onClick = { showChatDrawer = true }) {
                                Icon(Icons.Rounded.ChatBubbleOutline, contentDescription = "社区频道与私信", tint = lc.text)
                            }
                            if (totalChatUnread > 0) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 8.dp, end = 8.dp)
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(lc.danger),
                                )
                            }
                        }
                        Box {
                            val avatarChar = linuxDoSafeName(me?.displayName).take(1)
                            Box(
                                modifier = Modifier
                                    .padding(end = 6.dp)
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(lc.accent)
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
                                            text = if (me != null) linuxDoSafeName(me.displayName) else "登录社区账号",
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
                                    text = { Text("偏好设置 › 界面 / 隐私") },
                                    onClick = {
                                        showUserMenu = false
                                        showPrefsOverlay = true
                                    },
                                )
                            }
                        }
                    }

                    if (showSearchBar) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("在本帖楼层中搜索关键词…", fontSize = 13.sp, color = lc.muted) },
                            singleLine = true,
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Rounded.Close, contentDescription = "清空", tint = lc.muted)
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                    HorizontalDivider(color = lc.line)
                }
            }

            // Discourse Topic Floors Feed
            LazyColumn(
                state = listState,
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
                            .background(lc.bg)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = LinuxDoTopicMeta.TITLE,
                            color = lc.text,
                            fontSize = 18.sp,
                            lineHeight = 25.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            LinuxDoTopicMeta.TAGS.forEach { tag ->
                                Surface(color = lc.accent.copy(alpha = 0.12f), shape = RoundedCornerShape(4.dp)) {
                                    Text(
                                        text = tag,
                                        color = lc.accent,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    )
                                }
                            }
                            if (!selectedTag.isNullOrBlank()) {
                                Surface(
                                    color = lc.accent,
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.clickable { selectedTag = null },
                                ) {
                                    Text(
                                        text = "#$selectedTag ✕",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    )
                                }
                            }
                        }
                        Text(
                            text = "L先生 · 话题发起人  |  已收录 $totalFloors 层讨论  |  点击右下角刻度快速跳楼",
                            color = lc.muted,
                            fontSize = 11.sp,
                        )
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
                            Text("当前筛选暂无匹配楼层", color = lc.muted, fontSize = 13.sp)
                        }
                    }
                }

                itemsIndexed(orderedFloors, key = { _, (_, meme) -> meme.id }) { _, (floorNum, meme) ->
                    val author = linuxDoSafeName(meme.authorName ?: meme.authorUsername)
                    val avatarBg = FeishuDisguiseHelpers.imColor(author)
                    val likeCount = maxOf(meme.likes, meme.likeCount, meme.ldLikes)
                    val commentCount = maxOf(meme.comments, meme.commentCount)
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

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { coords ->
                                val b = coords.boundsInWindow()
                                itemTopPx = b.top
                                itemBottomPx = b.bottom
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(avatarBg),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = author.take(2),
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(author, color = lc.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text(FeishuDisguiseHelpers.formatConvTime(meme.createdAt ?: 0L), color = lc.muted, fontSize = 11.sp)
                            }
                            Surface(color = lc.surfaceAlt, shape = RoundedCornerShape(4.dp)) {
                                Text(
                                    text = "#$floorNum",
                                    color = lc.muted,
                                    fontSize = 11.sp,
                                    style = TextStyle(fontFeatureSettings = TABULAR_FONT_FEATURE),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                        }

                        RichChatBodyView(
                            rawBody = linuxDoFloorBody(meme),
                            imageLoader = api.imageLoader,
                            textColor = lc.text,
                            showInlineImages = false,
                        )

                        DisguiseMediaStrip(
                            meme = meme,
                            api = api,
                            blurAll = privacyConfig.blurAllEnabled,
                            accentColor = lc.accent,
                            isItemUnblurred = meme.id in unblurredIds,
                            onRevealItem = { unblurredIds = unblurredIds + meme.id },
                            onOpenViewer = { list, idx -> viewerState = Triple(list, idx, meme) },
                            onMediaLoaded = { mediaReady = true },
                        )

                        if (meme.tags.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                meme.tags.take(4).forEach { tag ->
                                    Surface(
                                        color = lc.surfaceAlt,
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.clickable { selectedTag = tag },
                                    ) {
                                        Text(
                                            text = DisguiseSanitizer.sanitizeSummaryForDisguise(tag, AppDisguiseMode.LINUX_DO),
                                            color = lc.muted,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        )
                                    }
                                }
                            }
                        }

                        if (meme.reactions.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                meme.reactions.forEach { reaction ->
                                    ReactionChip(
                                        emojiKey = reaction.emoji,
                                        count = reaction.count,
                                        mine = reaction.mine,
                                        imageLoader = api.imageLoader,
                                        onClick = {
                                            scope.launch {
                                                val updated = api.reactToMeme(meme.id, reaction.emoji)
                                                if (updated != null) {
                                                    floors = floors.map { if (it.id == updated.id) updated else it }
                                                }
                                            }
                                        },
                                    )
                                }
                            }
                        }

                        // Discourse floor footer actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (commentCount > 0 || commentsExpanded) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
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
                                        .padding(vertical = 4.dp, horizontal = 2.dp),
                                ) {
                                    Icon(
                                        imageVector = if (commentsExpanded) Icons.Rounded.KeyboardArrowDown else Icons.Rounded.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = lc.accent,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Text(
                                        text = "${maxOf(commentCount, loadedComments.size)} 条回复",
                                        color = lc.accent,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                    )
                                }
                            } else {
                                Spacer(Modifier.width(4.dp))
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
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
                                    Icon(
                                        imageVector = if (meme.liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                        contentDescription = "赞",
                                        tint = if (meme.liked) lc.danger else lc.muted,
                                        modifier = Modifier.size(17.dp),
                                    )
                                    if (likeCount > 0) {
                                        Text("$likeCount", color = if (meme.liked) lc.danger else lc.muted, fontSize = 12.sp)
                                    }
                                }

                                Icon(
                                    imageVector = if (meme.faved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                                    contentDescription = "书签",
                                    tint = if (meme.faved) lc.accent else lc.muted,
                                    modifier = Modifier
                                        .size(17.dp)
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

                                Icon(
                                    imageVector = Icons.Rounded.ContentCopy,
                                    contentDescription = "复制楼层链接",
                                    tint = lc.muted,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable {
                                            clipboard.setText(AnnotatedString("#$floorNum ${linuxDoFloorBody(meme)}"))
                                            toastMsg = "已复制楼层链接"
                                        },
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    modifier = Modifier.clickable {
                                        replyFloorTarget = floorNum to meme
                                        showComposerSheet = true
                                    },
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.Reply,
                                        contentDescription = "回复此楼",
                                        tint = lc.accent,
                                        modifier = Modifier.size(17.dp),
                                    )
                                    Text("回复", color = lc.accent, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }

                        if (commentsExpanded) {
                            Surface(
                                color = lc.bg,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(0.5.dp, lc.line),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    if (commentsBusy) {
                                        Text("加载回复中…", color = lc.muted, fontSize = 12.sp)
                                    } else if (loadedComments.isEmpty()) {
                                        Text("暂无内嵌回复", color = lc.muted, fontSize = 12.sp)
                                    } else {
                                        loadedComments.forEach { cm ->
                                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                ) {
                                                    Text(
                                                        text = linuxDoSafeName(cm.displayAuthor),
                                                        color = lc.text,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                    )
                                                    Text(
                                                        text = FeishuDisguiseHelpers.formatClock(cm.createdAt),
                                                        color = lc.muted,
                                                        fontSize = 10.sp,
                                                    )
                                                }
                                                RichChatBodyView(
                                                    rawBody = DisguiseSanitizer.sanitizeSummaryForDisguise(cm.body, AppDisguiseMode.LINUX_DO),
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
                                text = if (loadingMore) "加载后续楼层中…" else "点击加载更多楼层",
                                color = lc.accent,
                                fontSize = 13.sp,
                            )
                        }
                    }
                }
            }

            // Bottom Discourse Reply Bar
            HorizontalDivider(color = lc.line)
            Surface(
                color = lc.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Surface(
                        color = lc.accent,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                replyFloorTarget = null
                                showComposerSheet = true
                            },
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Reply, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "回复主题（盖新楼）",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }

        // Bottom-right Floor Counter Pill (24dp safe margin from right edge)
        if (!showLeftDrawer && !showChatDrawer && !showPrefsOverlay && !showTimeline) {
            Surface(
                color = lc.surface,
                shape = RoundedCornerShape(999.dp),
                shadowElevation = 4.dp,
                border = BorderStroke(1.dp, lc.line),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 24.dp, bottom = 72.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .clickable { showTimeline = true },
            ) {
                Text(
                    text = "$currentFloor / $totalFloors",
                    color = lc.text,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    style = TextStyle(fontFeatureSettings = TABULAR_FONT_FEATURE),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                )
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
                    shape = RoundedCornerShape(14.dp),
                    shadowElevation = 8.dp,
                    border = BorderStroke(1.dp, lc.line),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 24.dp)
                        .width(64.dp)
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
                                    .width(4.dp)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(lc.line),
                            )
                            val thumbY = (progress * (trackHeightPx - 36).coerceAtLeast(1)).roundToInt()
                            Box(
                                modifier = Modifier
                                    .offset { IntOffset(0, thumbY) }
                                    .width(28.dp)
                                    .height(22.dp)
                                    .clip(RoundedCornerShape(6.dp))
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

        // Left Category & Display Drawer
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
                        .width(290.dp)
                        .clickable(enabled = false) {},
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .navigationBarsPadding()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("帖子显示与话题版块", color = lc.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { showLeftDrawer = false }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Rounded.Close, contentDescription = "关闭", tint = lc.muted)
                            }
                        }
                        HorizontalDivider(color = lc.line)

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
                            title = "图床附件默认保护遮罩",
                            checked = privacyConfig.blurAllEnabled,
                            lc = lc,
                            onCheckedChange = {
                                onUpdatePrivacyConfig(privacyConfig.copy(blurAllEnabled = it))
                            },
                        )

                        HorizontalDivider(color = lc.line)
                        Text("按话题标签筛选楼层", color = lc.muted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            FilterChip(
                                selected = selectedTag == null,
                                onClick = {
                                    selectedTag = null
                                    showLeftDrawer = false
                                },
                                label = { Text("全部楼层", fontSize = 12.sp) },
                            )
                            tagsList.forEach { t ->
                                val cleanLabel = DisguiseSanitizer.sanitizeSummaryForDisguise(t.tag, AppDisguiseMode.LINUX_DO)
                                FilterChip(
                                    selected = selectedTag == t.tag,
                                    onClick = {
                                        selectedTag = t.tag
                                        showLeftDrawer = false
                                    },
                                    label = { Text("#$cleanLabel (${t.totalCount})", fontSize = 12.sp) },
                                )
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
                    .padding(bottom = 76.dp),
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
            selectedTag = selectedTag,
            lc = lc,
            onPublishedFloor = {
                showComposerSheet = false
                replyFloorTarget = null
                reloadFloors(reset = true)
                toastMsg = "新楼层已发布"
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
                    properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
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
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
