package com.jinhuan.forhappy.ui.disguise

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.Reply
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.jinhuan.forhappy.core.chat.ChatSessionController
import com.jinhuan.forhappy.core.clock.ServerClock
import com.jinhuan.forhappy.core.model.MeUser
import com.jinhuan.forhappy.core.model.MemeItem
import com.jinhuan.forhappy.core.network.api.ForHappyApiClient
import com.jinhuan.forhappy.core.network.media.MediaUrlRewriter
import com.jinhuan.forhappy.core.seen.LocalSeenTracker
import com.jinhuan.forhappy.core.seen.ObserveMemeSeenDwell
import com.jinhuan.forhappy.ui.chat.ChatNotificationJump
import com.jinhuan.forhappy.ui.chat.ChatScreen
import com.jinhuan.forhappy.ui.components.BoostPillText
import com.jinhuan.forhappy.ui.components.ForHappyAvatar
import com.jinhuan.forhappy.ui.components.ReactionChip
import com.jinhuan.forhappy.ui.theme.LocalForHappyPalette
import com.jinhuan.forhappy.ui.theme.LocalPrivacyStealthConfig
import com.jinhuan.forhappy.ui.wall.ComposeMemeModal
import com.jinhuan.forhappy.ui.wall.FullscreenMediaOverlay
import com.jinhuan.forhappy.ui.wall.MemeDetailModal
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private object LinuxDoTopicMeta {
    const val TITLE = "【持续更新】每天摸鱼时收集的图，欢迎佬友补充"
    const val CATEGORY = "搞七捻三"
    val CATEGORY_COLOR = Color(0xFF25AAE2)
    val TAGS = listOf("纯水", "日常")
    val SIDEBAR_CATS = listOf(
        "开发调优" to Color(0xFF3AB54A),
        "资源荟萃" to Color(0xFFF7941D),
        "文档共建" to Color(0xFF9EB83B),
        "搞七捻三" to Color(0xFF25AAE2),
        "跳蚤市场" to Color(0xFFED207B),
        "运营反馈" to Color(0xFF808281),
    )

    fun formatAgo(ts: Long?): String {
        if (ts == null || ts <= 0L) return ""
        val s = ServerClock.serverNowSec() - ts
        if (s < 60L) return "刚刚"
        if (s < 3600L) return "${s / 60L} 分钟"
        if (s < 86_400L) return "${s / 3600L} 小时"
        if (s < 86_400L * 30L) return "${s / 86_400L} 天"
        val sdf = SimpleDateFormat("M月d日", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("GMT+08:00")
        return sdf.format(Date(ts * 1000L))
    }
}

/**
 * Pure Discourse single-topic disguise shell matching `52dmeme.com/app.js` `LD`.
 * Hides the 5-tab bottom navigation bar completely; turns the meme stream into numbered forum
 * floors (`#1`, `#2`...), places a Discourse floor progress pill bottom-right, and provides real
 * group chat via top-bar chat drawer.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinuxDoDisguiseScreen(
    api: ForHappyApiClient,
    chatController: ChatSessionController,
    me: MeUser?,
    onRequireLogin: () -> Unit,
    onToggleBlurAll: (Boolean) -> Unit,
    onTriggerBossKey: () -> Unit,
    onOpenPrivacySettings: () -> Unit,
    onExitDisguise: () -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    val palette = LocalForHappyPalette.current
    val privacyConfig = LocalPrivacyStealthConfig.current
    val seenTracker = LocalSeenTracker.current
    val chatState by chatController.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var hideNsfw by remember { mutableStateOf(true) }
    var searchOpen by remember { mutableStateOf(false) }
    var searchDraft by remember { mutableStateOf("") }
    var activeQuery by remember { mutableStateOf("") }

    var posts by remember { mutableStateOf<List<MemeItem>>(emptyList()) }
    var baseOffset by remember { mutableIntStateOf(0) }
    var totalFloors by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var loadingMore by remember { mutableStateOf(false) }
    var hasMoreDown by remember { mutableStateOf(false) }

    var showMenu by remember { mutableStateOf(false) }
    var showSidebarSheet by remember { mutableStateOf(false) }
    var showChatSheet by remember { mutableStateOf(false) }
    var chatJumpGroupId by remember { mutableStateOf<Long?>(null) }
    var showJumpDialog by remember { mutableStateOf(false) }
    var jumpInput by remember { mutableStateOf("") }

    var showCompose by remember { mutableStateOf(false) }
    var selectedDetailId by remember { mutableStateOf<Long?>(null) }
    var fullscreenImgUrl by remember { mutableStateOf<String?>(null) }

    val totalChatUnread = remember(chatState.groups) { chatState.groups.sumOf { it.unread } }
    val showTopicInHeader by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
    val currentFloor by remember {
        derivedStateOf {
            if (posts.isEmpty()) 0
            else {
                val visible = listState.layoutInfo.visibleItemsInfo
                    .firstOrNull { it.index >= 1 && (it.index - 1) in posts.indices }
                val idx = ((visible?.index ?: 1) - 1).coerceIn(0, posts.lastIndex)
                baseOffset + idx + 1
            }
        }
    }

    fun jumpToOffset(targetOffset: Int) {
        scope.launch {
            loading = true
            val safeOffset = targetOffset.coerceAtLeast(0)
            val daysDef = async { api.fetchDays(limitDays = null, sort = "old", search = activeQuery.ifBlank { null }, nsfwHide = hideNsfw) }
            val pageDef = async {
                api.fetchMemesPage(
                    limit = 20,
                    offset = safeOffset,
                    sort = "old",
                    query = activeQuery.ifBlank { null },
                    nsfwHide = hideNsfw,
                )
            }
            val daysResp = daysDef.await()
            val page = pageDef.await()
            seenTracker?.ingestFetchedMemes(page.items)
            totalFloors = daysResp.days.sumOf { it.totalCount }.coerceAtLeast(safeOffset + page.items.size)
            baseOffset = page.offset
            posts = page.items
            hasMoreDown = page.more
            loading = false
            listState.scrollToItem(0)
        }
    }

    fun loadMoreDown() {
        if (loading || loadingMore || !hasMoreDown) return
        scope.launch {
            loadingMore = true
            val nextOff = baseOffset + posts.size
            val page = api.fetchMemesPage(
                limit = 20,
                offset = nextOff,
                sort = "old",
                query = activeQuery.ifBlank { null },
                nsfwHide = hideNsfw,
            )
            seenTracker?.ingestFetchedMemes(page.items)
            val have = posts.asSequence().map { it.id }.toHashSet()
            val fresh = page.items.filter { have.add(it.id) }
            if (fresh.isNotEmpty()) {
                posts = posts + fresh
                hasMoreDown = page.more
            } else {
                hasMoreDown = false
            }
            loadingMore = false
        }
    }

    LaunchedEffect(activeQuery, hideNsfw, me?.id) {
        if (me != null) {
            chatController.refreshGroups()
        }
        if (activeQuery.isBlank()) {
            loading = true
            val daysResp = api.fetchDays(limitDays = null, sort = "old", nsfwHide = hideNsfw)
            val sumFloors = daysResp.days.sumOf { it.totalCount }
            val startOff = maxOf(0, sumFloors - 20)
            val page = api.fetchMemesPage(
                limit = 20,
                offset = startOff,
                sort = "old",
                nsfwHide = hideNsfw,
            )
            seenTracker?.ingestFetchedMemes(page.items)
            totalFloors = sumFloors.coerceAtLeast(startOff + page.items.size)
            baseOffset = page.offset
            posts = page.items
            hasMoreDown = page.more
            loading = false
            listState.scrollToItem(0)
        } else {
            jumpToOffset(0)
        }
    }

    DoubleBackBossHandler(
        enabled = privacyConfig.bossDoubleBack &&
            !showSidebarSheet &&
            !showChatSheet &&
            !showJumpDialog &&
            !showCompose &&
            selectedDetailId == null &&
            fullscreenImgUrl == null,
        onTriggerBoss = onTriggerBossKey,
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.bg)
            .navigationBarsPadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Discourse top header
            Surface(
                color = palette.surface,
                modifier = Modifier.statusBarsPadding(),
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .padding(horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = { showSidebarSheet = true }) {
                            Icon(Icons.Rounded.Menu, contentDescription = "侧边栏", tint = palette.text)
                        }
                        if (showTopicInHeader) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { scope.launch { listState.animateScrollToItem(0) } },
                            ) {
                                Text(
                                    text = LinuxDoTopicMeta.TITLE,
                                    color = palette.text,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(LinuxDoTopicMeta.CATEGORY_COLOR, RoundedCornerShape(2.dp)),
                                    )
                                    Spacer(Modifier.width(5.dp))
                                    Text(LinuxDoTopicMeta.CATEGORY, color = palette.muted, fontSize = 10.sp)
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { scope.launch { listState.animateScrollToItem(0) } },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                LinuxDoLogoMark(size = 24.dp)
                                Spacer(Modifier.width(7.dp))
                                Text(
                                    text = "LINUX DO",
                                    color = palette.text,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp,
                                )
                            }
                        }

                        IconButton(onClick = { searchOpen = !searchOpen }) {
                            Icon(Icons.Rounded.Search, contentDescription = "本帖搜索", tint = palette.text)
                        }
                        IconButton(
                            onClick = {
                                if (me == null) {
                                    onRequireLogin()
                                } else {
                                    chatJumpGroupId = chatState.groups.firstOrNull()?.id
                                    showChatSheet = true
                                }
                            },
                        ) {
                            BadgedBox(
                                badge = {
                                    if (totalChatUnread > 0) {
                                        Badge {
                                            Text(
                                                text = if (totalChatUnread > 99) "99+" else "$totalChatUnread",
                                                fontSize = 9.sp,
                                            )
                                        }
                                    }
                                },
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.Chat, contentDescription = "聊天", tint = palette.text)
                            }
                        }
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Rounded.MoreHoriz, contentDescription = "论坛菜单", tint = palette.text)
                            }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text(if (privacyConfig.blurAllEnabled) "全图模糊：已开启（点击关闭）" else "全图模糊：已关闭（点击开启）") },
                                    onClick = {
                                        showMenu = false
                                        onToggleBlurAll(!privacyConfig.blurAllEnabled)
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(if (hideNsfw) "内容过滤：隐藏敏感（点击显示）" else "内容过滤：模糊显示（点击隐藏）") },
                                    onClick = {
                                        showMenu = false
                                        hideNsfw = !hideNsfw
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("跳到某一楼") },
                                    onClick = {
                                        showMenu = false
                                        jumpInput = ""
                                        showJumpDialog = true
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("发布新楼（发图）") },
                                    onClick = {
                                        showMenu = false
                                        if (me == null) onRequireLogin() else showCompose = true
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("切到云文档（老板键）") },
                                    onClick = {
                                        showMenu = false
                                        onTriggerBossKey()
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("伪装与隐私设置") },
                                    onClick = {
                                        showMenu = false
                                        onOpenPrivacySettings()
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("退出伪装") },
                                    onClick = {
                                        showMenu = false
                                        onExitDisguise()
                                    },
                                )
                            }
                        }
                    }

                    if (searchOpen) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(palette.surface2)
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Surface(
                                color = palette.surface,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .border(1.dp, palette.line, RoundedCornerShape(8.dp)),
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 10.dp),
                                    contentAlignment = Alignment.CenterStart,
                                ) {
                                    if (searchDraft.isEmpty()) {
                                        Text("在本帖中搜索配文、描述和标签…", color = palette.muted, fontSize = 12.sp)
                                    }
                                    BasicTextField(
                                        value = searchDraft,
                                        onValueChange = { searchDraft = it },
                                        singleLine = true,
                                        textStyle = TextStyle(color = palette.text, fontSize = 13.sp),
                                        cursorBrush = SolidColor(palette.primary),
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                        keyboardActions = KeyboardActions(
                                            onSearch = {
                                                activeQuery = searchDraft.trim()
                                                searchOpen = false
                                            },
                                        ),
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            }
                            TextButton(
                                onClick = {
                                    activeQuery = searchDraft.trim()
                                    searchOpen = false
                                },
                            ) {
                                Text("搜索", fontSize = 12.sp)
                            }
                        }
                    }
                    HorizontalDivider(color = palette.line)
                }
            }

            if (loading && posts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = palette.primary)
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 72.dp),
                ) {
                    // Topic Title Block
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(palette.surface)
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = LinuxDoTopicMeta.TITLE,
                                color = palette.text,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 25.sp,
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(LinuxDoTopicMeta.CATEGORY_COLOR, RoundedCornerShape(2.dp)),
                                    )
                                    Spacer(Modifier.width(5.dp))
                                    Text(LinuxDoTopicMeta.CATEGORY, color = palette.muted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                }
                                LinuxDoTopicMeta.TAGS.forEach { t ->
                                    Surface(color = palette.surface2, shape = RoundedCornerShape(4.dp)) {
                                        Text(
                                            text = t,
                                            color = palette.muted,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        )
                                    }
                                }
                            }
                            if (activeQuery.isNotBlank()) {
                                Surface(
                                    color = palette.primary.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text("本帖搜索「$activeQuery」", color = palette.primary, fontSize = 12.sp)
                                        Text(
                                            text = "清除",
                                            color = palette.primary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.clickable {
                                                searchDraft = ""
                                                activeQuery = ""
                                            },
                                        )
                                    }
                                }
                            }
                            if (baseOffset > 0) {
                                OutlinedButton(
                                    onClick = { jumpToOffset((baseOffset - 20).coerceAtLeast(0)) },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text("↑ 查看前 20 楼（当前从第 ${baseOffset + 1} 楼起）", fontSize = 12.sp)
                                }
                            }
                        }
                        HorizontalDivider(color = palette.line)
                    }

                    itemsIndexed(posts, key = { _, m -> m.id }) { idx, m ->
                        val floorNum = baseOffset + idx + 1
                        LinuxDoFloorItem(
                            floor = floorNum,
                            meme = m,
                            api = api,
                            me = me,
                            onRequireLogin = onRequireLogin,
                            onOpenDetail = { selectedDetailId = m.id },
                            onOpenImage = { fullscreenImgUrl = it },
                            onMemeUpdated = { updated ->
                                posts = posts.map { if (it.id == updated.id) updated else it }
                            },
                        )
                        HorizontalDivider(color = palette.line)
                    }

                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            when {
                                loadingMore -> CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.dp,
                                    color = palette.primary,
                                )
                                hasMoreDown -> OutlinedButton(onClick = { loadMoreDown() }) {
                                    Text("加载后续楼层", fontSize = 12.sp)
                                }
                                else -> Text("已到底部 · 共 ${totalFloors} 楼", color = palette.muted, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Bottom-right Discourse floor progress pill
        if (totalFloors > 0) {
            val frac = (currentFloor.toFloat() / totalFloors.toFloat()).coerceIn(0.02f, 1f)
            Surface(
                color = palette.surface.copy(alpha = 0.96f),
                shadowElevation = 4.dp,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 14.dp, bottom = 16.dp)
                    .border(1.dp, palette.line, RoundedCornerShape(10.dp))
                    .clickable {
                        jumpInput = ""
                        showJumpDialog = true
                    },
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "$currentFloor / $totalFloors",
                        color = palette.text,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { frac },
                        modifier = Modifier
                            .width(72.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(999.dp)),
                        color = palette.primary,
                        trackColor = palette.surface2,
                    )
                }
            }
        }
    }

    if (showSidebarSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSidebarSheet = false },
            containerColor = palette.bg,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("导航与类别", color = palette.muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                LinuxDoTopicMeta.SIDEBAR_CATS.forEach { (catName, color) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                showSidebarSheet = false
                                scope.launch { listState.animateScrollToItem(0) }
                            }
                            .padding(vertical = 6.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(11.dp)
                                .background(color, RoundedCornerShape(3.dp)),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(catName, color = palette.text, fontSize = 14.sp)
                    }
                }
                HorizontalDivider(color = palette.line)
                Text("聊天频道", color = palette.muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                if (me == null) {
                    OutlinedButton(
                        onClick = {
                            showSidebarSheet = false
                            onRequireLogin()
                        },
                    ) {
                        Text("登录后加入频道聊天")
                    }
                } else {
                    chatState.groups.forEach { grp ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    showSidebarSheet = false
                                    chatJumpGroupId = grp.id
                                    showChatSheet = true
                                }
                                .padding(vertical = 8.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("# ${grp.name}", color = palette.text, fontSize = 14.sp)
                            if (grp.unread > 0) {
                                Surface(color = palette.primary, shape = CircleShape) {
                                    Text(
                                        text = if (grp.unread > 99) "99+" else "${grp.unread}",
                                        color = palette.onPrimary,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
        }
    }

    if (showChatSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showChatSheet = false },
            sheetState = sheetState,
            containerColor = palette.bg,
            dragHandle = null,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.86f)
                    .navigationBarsPadding(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.surface)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("频道聊天", color = palette.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    IconButton(onClick = { showChatSheet = false }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = "关闭", tint = palette.muted)
                    }
                }
                HorizontalDivider(color = palette.line)
                Box(modifier = Modifier.weight(1f)) {
                    ChatScreen(
                        api = api,
                        controller = chatController,
                        me = me,
                        initialJump = chatJumpGroupId?.let { ChatNotificationJump(groupId = it) },
                        onNavigateToMeme = { id ->
                            showChatSheet = false
                            selectedDetailId = id
                        },
                        onNavigateToTag = { showChatSheet = false },
                        onOpenUrl = onOpenUrl,
                        onRequireLogin = {
                            showChatSheet = false
                            onRequireLogin()
                        },
                    )
                }
            }
        }
    }

    if (showJumpDialog) {
        AlertDialog(
            onDismissRequest = { showJumpDialog = false },
            title = { Text("跳到第几楼？（1 - ${totalFloors}）", fontSize = 15.sp, fontWeight = FontWeight.SemiBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = jumpInput,
                        onValueChange = { jumpInput = it.filter { ch -> ch.isDigit() } },
                        singleLine = true,
                        label = { Text("输入楼层号", fontSize = 12.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                showJumpDialog = false
                                jumpToOffset(0)
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("第 1 楼", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                showJumpDialog = false
                                jumpToOffset(maxOf(0, totalFloors - 20))
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("最新楼层", fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val floor = jumpInput.toIntOrNull()?.coerceIn(1, maxOf(1, totalFloors)) ?: 1
                        showJumpDialog = false
                        jumpToOffset(floor - 1)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.primary, contentColor = palette.onPrimary),
                ) {
                    Text("跳转")
                }
            },
            dismissButton = {
                TextButton(onClick = { showJumpDialog = false }) { Text("取消") }
            },
        )
    }

    if (showCompose) {
        ComposeMemeModal(
            api = api,
            onDismiss = { showCompose = false },
            onPublished = { jumpToOffset(maxOf(0, totalFloors - 19)) },
        )
    }

    selectedDetailId?.let { memeId ->
        MemeDetailModal(
            memeId = memeId,
            api = api,
            me = me,
            onRequireLogin = onRequireLogin,
            onDismiss = { selectedDetailId = null },
            onOpenImageFullscreen = { fullscreenImgUrl = it },
            onTagClick = { selectedDetailId = null },
            onStateUpdated = { updated ->
                posts = posts.map { if (it.id == updated.id) updated else it }
            },
        )
    }

    fullscreenImgUrl?.let { url ->
        FullscreenMediaOverlay(
            imageUrl = url,
            api = api,
            onDismiss = { fullscreenImgUrl = null },
        )
    }
}

@Composable
private fun LinuxDoLogoMark(size: androidx.compose.ui.unit.Dp) {
    Column(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .border(1.5.dp, Color(0xFFE2E8F0), CircleShape),
    ) {
        Box(modifier = Modifier.weight(0.3f).fillMaxWidth().background(Color(0xFF1C1C1E)))
        Box(modifier = Modifier.weight(0.4f).fillMaxWidth().background(Color(0xFFF0F0F0)))
        Box(modifier = Modifier.weight(0.3f).fillMaxWidth().background(Color(0xFFFFB003)))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LinuxDoFloorItem(
    floor: Int,
    meme: MemeItem,
    api: ForHappyApiClient,
    me: MeUser?,
    onRequireLogin: () -> Unit,
    onOpenDetail: () -> Unit,
    onOpenImage: (String) -> Unit,
    onMemeUpdated: (MemeItem) -> Unit,
) {
    val palette = LocalForHappyPalette.current
    val privacyConfig = LocalPrivacyStealthConfig.current
    val scope = rememberCoroutineScope()

    val rawUrl = remember(meme.id, meme.primaryImageUrl, meme.detailImageUrls) {
        val candidate = meme.detailImageUrls.firstOrNull() ?: meme.primaryImageUrl
        MediaUrlRewriter.rewriteMediaUrl(candidate)
    }
    var revealed by remember(meme.id, privacyConfig.blurAllEnabled) { mutableStateOf(false) }
    var imageLoaded by remember(rawUrl) { mutableStateOf(rawUrl.isBlank()) }

    if (privacyConfig.autoReblurOnScrollOff) {
        DisposableEffect(meme.id) {
            onDispose { revealed = false }
        }
    }

    val isBlurred = rawUrl.isNotBlank() && (privacyConfig.blurAllEnabled || meme.isRisky) && !revealed
    ObserveMemeSeenDwell(
        memeId = meme.id,
        seenAt = meme.seenAt,
        memeDay = meme.day,
        isMediaReady = imageLoaded,
        isBlurred = isBlurred,
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.surface)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Post author header & floor number
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ForHappyAvatar(
                username = meme.authorDisplay,
                avatarUrl = meme.authorAvatar,
                imageLoader = api.imageLoader,
                size = 38.dp,
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = meme.authorDisplay,
                color = palette.text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${LinuxDoTopicMeta.formatAgo(meme.createdAt)} · #$floor",
                color = palette.muted,
                fontSize = 12.sp,
            )
        }

        // Floor text content
        val postText = remember(meme.id) {
            listOfNotNull(meme.title, meme.text, meme.content, meme.caption, meme.body)
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()
                .joinToString("\n")
        }
        if (postText.isNotBlank()) {
            Text(
                text = postText,
                color = palette.text,
                fontSize = 14.sp,
                lineHeight = 22.sp,
                modifier = Modifier.clickable { onOpenDetail() },
            )
        }

        // Floor media
        if (rawUrl.isNotBlank()) {
            val w = meme.effectiveWidth ?: 0
            val h = meme.effectiveHeight ?: 0
            val ratio = if (w > 0 && h > 0) (w.toFloat() / h.toFloat()).coerceIn(0.45f, 2.2f) else 4f / 3f
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(ratio)
                    .heightIn(max = 460.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(palette.surface2)
                    .clickable {
                        when {
                            isBlurred -> revealed = true
                            privacyConfig.blurAllEnabled && revealed -> revealed = false
                            else -> onOpenImage(rawUrl)
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = rawUrl,
                    imageLoader = api.imageLoader,
                    contentDescription = meme.safeLabel,
                    contentScale = ContentScale.Crop,
                    onSuccess = { imageLoaded = true },
                    modifier = Modifier
                        .fillMaxSize()
                        .let { if (isBlurred) it.blur(22.dp) else it },
                )
                if (isBlurred) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.62f),
                        shape = RoundedCornerShape(999.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(Icons.Rounded.VisibilityOff, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                            Text("已模糊 · 轻触显示", color = Color.White, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Boost quick replies inline block
        val quickReplies = remember(meme.replyList, meme.myReply, me?.username) { meme.effectiveQuickReplies(me) }
        if (quickReplies.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                quickReplies.take(5).forEach { qr ->
                    Surface(
                        color = if (qr.mine) palette.primary.copy(alpha = 0.12f) else palette.surface2,
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier.clickable {
                            if (me == null) {
                                onRequireLogin()
                            } else {
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
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            ForHappyAvatar(
                                username = qr.displayAuthor,
                                avatarUrl = qr.avatar,
                                imageLoader = api.imageLoader,
                                size = 14.dp,
                            )
                            BoostPillText(text = qr.text, imageLoader = api.imageLoader, fontSize = 11.sp)
                            if (qr.n > 1) {
                                Text("×${qr.n}", color = palette.muted, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }

        // Reaction chips summary
        val activeReactions = remember(meme.reactions) { meme.reactions.filter { it.count > 0 } }
        if (activeReactions.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                activeReactions.take(6).forEach { rx ->
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

        // Discourse post action bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            if (meme.totalComments > 0) {
                Text(
                    text = "${meme.totalComments} 条回复 ⌄",
                    color = palette.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onOpenDetail() }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                )
            } else {
                Spacer(Modifier.width(4.dp))
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        if (me == null) {
                            onRequireLogin()
                        } else {
                            scope.launch {
                                api.toggleLike(meme.id)
                                onMemeUpdated(
                                    meme.copy(
                                        liked = !meme.liked,
                                        likes = if (meme.liked) maxOf(0, meme.likes - 1) else meme.likes + 1,
                                        likeCount = if (meme.liked) maxOf(0, meme.likeCount - 1) else meme.likeCount + 1,
                                    ),
                                )
                            }
                        }
                    },
                ) {
                    if (meme.totalLikes > 0) {
                        Text(
                            text = "${meme.totalLikes}",
                            color = if (meme.liked) palette.danger else palette.muted,
                            fontSize = 12.sp,
                        )
                        Spacer(Modifier.width(3.dp))
                    }
                    Icon(
                        imageVector = if (meme.liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = "点赞",
                        tint = if (meme.liked) palette.danger else palette.muted,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.RocketLaunch,
                    contentDescription = "助推 / 详情",
                    tint = palette.muted,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { onOpenDetail() },
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(palette.surface2)
                        .clickable { onOpenDetail() }
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Reply, contentDescription = null, tint = palette.text, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("回复", color = palette.text, fontSize = 12.sp)
                }
            }
        }
    }
}
