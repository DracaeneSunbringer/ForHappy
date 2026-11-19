package com.jinhuan.forhappy.ui.chat

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Create
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.GroupAdd
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.jinhuan.forhappy.core.chat.ChatRichTextParser
import com.jinhuan.forhappy.core.chat.ChatSessionController
import com.jinhuan.forhappy.core.clock.ServerClock
import com.jinhuan.forhappy.core.cosmetics.BadgeAndCosmeticRegistry
import com.jinhuan.forhappy.core.model.AppDisguiseMode
import com.jinhuan.forhappy.core.model.ChatGoneGroup
import com.jinhuan.forhappy.core.model.ChatGroup
import com.jinhuan.forhappy.core.model.ChatMessage
import com.jinhuan.forhappy.core.model.ChatScrollPolicy
import com.jinhuan.forhappy.core.model.ChatUser
import com.jinhuan.forhappy.core.model.DmThread
import com.jinhuan.forhappy.core.model.GroupMemberItem
import com.jinhuan.forhappy.core.model.MeUser
import com.jinhuan.forhappy.core.model.UnifiedSessionItem
import com.jinhuan.forhappy.core.network.api.ForHappyApiClient
import com.jinhuan.forhappy.core.network.media.MediaUrlRewriter
import com.jinhuan.forhappy.ui.components.BadgeChipView
import com.jinhuan.forhappy.ui.components.BadgePill
import com.jinhuan.forhappy.ui.components.ForHappyAvatar
import com.jinhuan.forhappy.ui.components.TitleChipView
import com.jinhuan.forhappy.ui.components.parseSafeHex
import com.jinhuan.forhappy.ui.theme.ForHappyDimens
import com.jinhuan.forhappy.ui.theme.LocalForHappyPalette
import com.jinhuan.forhappy.ui.theme.LocalPrivacyStealthConfig
import com.jinhuan.forhappy.ui.wall.ConfigureFullscreenDialogWindow
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Unified `消息` Tab Level-0 screen (`02c §1`):
 * - 48dp header (`消息` + Search + New DM + Notifications Bell with badge)
 * - 5 filter capsules (`全部 / 群聊 / 私信 / @我 N / 未读`)
 * - Mixed pinned + sorted Groups & DMs list with long-press action sheet (no horizontal swipe!)
 * - Collapsible `── 可加入的群 ──` section backed by `/api/chat/groups` `gone`
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MessagesListView(
    state: ChatSessionController.ChatUiState,
    controller: ChatSessionController,
    api: ForHappyApiClient,
    me: MeUser?,
    notifUnreadCount: Int,
    selectedFilter: String,
    onSelectFilter: (String) -> Unit,
    onOpenGroup: (Long) -> Unit,
    onOpenDmThread: (DmThread) -> Unit,
    onStartNewDmUser: (ChatUser) -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenGroupSearchJump: (groupId: Long, msgId: Long) -> Unit,
    onRequireLogin: () -> Unit,
) {
    val palette = LocalForHappyPalette.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    var showGlobalSearch by remember { mutableStateOf(false) }
    var showNewDmSheet by remember { mutableStateOf(false) }
    var goneExpanded by remember { mutableStateOf(false) }
    var longPressItem by remember { mutableStateOf<UnifiedSessionItem?>(null) }
    var statusBanner by remember { mutableStateOf<String?>(null) }
    var joiningGroupId by remember { mutableStateOf<Long?>(null) }

    val items = remember(
        state.groups,
        state.dmThreads,
        state.pinnedSessionKeys,
        state.mutedDmIds,
        state.activeGuessRoundByGroup,
        selectedFilter,
    ) {
        controller.buildUnifiedConversationItems(filter = selectedFilter)
    }

    LaunchedEffect(statusBanner) {
        if (statusBanner != null) {
            delay(2800L)
            statusBanner = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.bg),
    ) {
        // 48dp Top Bar
        Surface(color = palette.surface) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ForHappyDimens.topBarHeight)
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "消息",
                        color = palette.text,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = { showGlobalSearch = true },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "搜索会话与消息",
                            tint = palette.text,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    IconButton(
                        onClick = {
                            if (me == null) onRequireLogin() else showNewDmSheet = true
                        },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Create,
                            contentDescription = "发起私信",
                            tint = palette.text,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable { onOpenNotifications() },
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Notifications,
                            contentDescription = "通知中心",
                            tint = palette.text,
                            modifier = Modifier.size(20.dp),
                        )
                        if (notifUnreadCount > 0) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 3.dp, end = 2.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(palette.badge)
                                    .padding(horizontal = 4.dp, vertical = 0.5.dp),
                            ) {
                                Text(
                                    text = if (notifUnreadCount > 99) "99+" else notifUnreadCount.toString(),
                                    color = Color.White,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }

                // Filter Capsules Row
                val atMeTotal = state.totalAtMeCount
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    listOf(
                        "all" to "全部",
                        "groups" to "群聊",
                        "dm" to "私信",
                        "at" to if (atMeTotal > 0) "@我 $atMeTotal" else "@我",
                        "unread" to if (state.totalUnreadCount > 0) "未读 ${state.totalUnreadCount}" else "未读",
                    ).forEach { (key, label) ->
                        val selected = selectedFilter == key
                        Surface(
                            color = if (selected) palette.primary else palette.surface2,
                            shape = RoundedCornerShape(999.dp),
                            modifier = Modifier
                                .border(
                                    1.dp,
                                    if (selected) palette.primary else palette.line,
                                    RoundedCornerShape(999.dp),
                                )
                                .clickable { onSelectFilter(key) },
                        ) {
                            Text(
                                text = label,
                                color = when {
                                    selected -> palette.onPrimary
                                    key == "at" && atMeTotal > 0 -> palette.danger
                                    else -> palette.text
                                },
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
                            )
                        }
                    }
                }
                HorizontalDivider(color = palette.line.copy(alpha = 0.7f))
            }
        }

        statusBanner?.let { banner ->
            Surface(color = palette.primary.copy(alpha = 0.12f)) {
                Text(
                    text = banner,
                    color = palette.primary,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                )
            }
        }

        if (me == null && items.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(24.dp),
                ) {
                    Text("登录后同步群聊与私信会话", color = palette.muted, fontSize = 14.sp)
                    Button(onClick = onRequireLogin) { Text("立即登录") }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 6.dp),
            ) {
                if (items.isEmpty()) {
                    item(key = "empty_list") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 36.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = when (selectedFilter) {
                                    "at" -> "暂无未读 @我的群聊"
                                    "unread" -> "所有消息都已读啦"
                                    "dm" -> "暂无私信会话，点右上角 ✎ 发起私信"
                                    else -> "暂无会话记录"
                                },
                                color = palette.muted,
                                fontSize = 13.sp,
                            )
                        }
                    }
                } else {
                    items(items, key = { it.sessionKey }) { session ->
                        SessionRowCard(
                            session = session,
                            meId = me?.id ?: 0L,
                            api = api,
                            onClick = {
                                when (session) {
                                    is UnifiedSessionItem.Group -> onOpenGroup(session.group.id)
                                    is UnifiedSessionItem.Dm -> onOpenDmThread(session.thread)
                                }
                            },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                longPressItem = session
                            },
                        )
                    }
                }

                // Joinable / outside groups section
                if (state.goneGroups.isNotEmpty() && selectedFilter in setOf("all", "groups")) {
                    item(key = "gone_header") {
                        Surface(
                            color = palette.surface.copy(alpha = 0.6f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                                .clickable { goneExpanded = !goneExpanded },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = "── 可加入的群 (${state.goneGroups.size}) ──",
                                    color = palette.muted,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                                Icon(
                                    imageVector = if (goneExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                    contentDescription = null,
                                    tint = palette.muted,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }

                    if (goneExpanded) {
                        items(state.goneGroups, key = { "gone_${it.id}" }) { gone ->
                            GoneGroupRow(
                                gone = gone,
                                busy = joiningGroupId == gone.id,
                                onJoin = {
                                    if (me == null) {
                                        onRequireLogin()
                                        return@GoneGroupRow
                                    }
                                    joiningGroupId = gone.id
                                    scope.launch {
                                        val res = api.applyJoinGroup(gone.id, adultContentAck = true)
                                        joiningGroupId = null
                                        res.onSuccess { msg ->
                                            statusBanner = "${gone.name}：$msg"
                                            controller.refreshGroups()
                                        }.onFailure { err ->
                                            statusBanner = err.message ?: "操作失败"
                                        }
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    // Long-press session options bottom sheet
    longPressItem?.let { item ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val title = when (item) {
            is UnifiedSessionItem.Group -> item.group.name
            is UnifiedSessionItem.Dm -> item.thread.peerName ?: item.thread.peerUsername ?: "私信"
        }
        val canMarkRead = item.unreadCount > 0 || (item is UnifiedSessionItem.Group && item.group.unread > 0)
        ModalBottomSheet(
            onDismissRequest = { longPressItem = null },
            sheetState = sheetState,
            containerColor = palette.bg,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            dragHandle = null,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(title, color = palette.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                HorizontalDivider(color = palette.line)
                SessionSheetActionRow(
                    icon = Icons.Rounded.PushPin,
                    label = if (item.isPinned) "取消置顶" else "置顶会话",
                    onClick = {
                        controller.toggleSessionPinned(item.sessionKey)
                        longPressItem = null
                    },
                )
                if (canMarkRead) {
                    SessionSheetActionRow(
                        icon = Icons.Rounded.DoneAll,
                        label = "标为已读",
                        onClick = {
                            scope.launch {
                                when (item) {
                                    is UnifiedSessionItem.Group -> controller.acknowledgeGroupRead(item.group.id)
                                    is UnifiedSessionItem.Dm -> {
                                        val msgs = api.fetchDmMessages(item.thread.id)
                                        val latest = msgs.lastOrNull { it.id > 0L }?.id ?: 0L
                                        if (latest > 0L && api.markDmRead(item.thread.id, latest)) {
                                            controller.updateDmThreadsLocal(
                                                state.dmThreads.map { if (it.id == item.thread.id) it.copy(unread = 0) else it },
                                            )
                                        }
                                    }
                                }
                            }
                            longPressItem = null
                        },
                    )
                }
                SessionSheetActionRow(
                    icon = if (item.isMuted) Icons.Rounded.Notifications else Icons.Rounded.NotificationsOff,
                    label = if (item.isMuted) "关闭消息免打扰" else "开启消息免打扰",
                    onClick = {
                        when (item) {
                            is UnifiedSessionItem.Group -> {
                                val nextMute = !item.group.muted
                                controller.setGroupMutedLocal(item.group.id, nextMute)
                                scope.launch {
                                    if (!api.setGroupMute(item.group.id, nextMute)) {
                                        controller.setGroupMutedLocal(item.group.id, !nextMute)
                                    }
                                }
                            }
                            is UnifiedSessionItem.Dm -> {
                                controller.setDmMutedLocal(item.thread.id, !item.isMuted)
                            }
                        }
                        longPressItem = null
                    },
                )
            }
        }
    }

    if (showNewDmSheet) {
        NewDmUserSearchSheet(
            api = api,
            meId = me?.id ?: 0L,
            onDismiss = { showNewDmSheet = false },
            onSelectUser = { user ->
                showNewDmSheet = false
                val existing = state.dmThreads.firstOrNull { it.peerId == user.id }
                if (existing != null) {
                    onOpenDmThread(existing)
                } else {
                    onStartNewDmUser(user)
                }
            },
        )
    }

    if (showGlobalSearch) {
        GlobalMessagesSearchDialog(
            state = state,
            api = api,
            onDismiss = { showGlobalSearch = false },
            onOpenGroup = { gid ->
                showGlobalSearch = false
                onOpenGroup(gid)
            },
            onOpenDm = { thread ->
                showGlobalSearch = false
                onOpenDmThread(thread)
            },
            onJumpGroupMessage = { gid, mid ->
                showGlobalSearch = false
                onOpenGroupSearchJump(gid, mid)
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SessionRowCard(
    session: UnifiedSessionItem,
    meId: Long,
    api: ForHappyApiClient,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val palette = LocalForHappyPalette.current
    val timeText = remember(session.sortTimestamp) {
        formatSessionTime(session.sortTimestamp)
    }

    val title: String
    val avatarName: String
    val avatarUrl: String?
    val preview: String
    val isDm = session is UnifiedSessionItem.Dm
    var hasActiveGuess = false
    var atMeCount = 0
    var mutedRawUnread = 0

    when (session) {
        is UnifiedSessionItem.Group -> {
            title = session.group.name
            avatarName = session.group.name
            avatarUrl = null
            hasActiveGuess = session.hasActiveGuess
            atMeCount = session.group.atMe
            mutedRawUnread = session.group.unread
            val latestText = session.group.latest?.text.orEmpty().let { ChatRichTextParser.previewSummary(it) }
            val by = session.group.latest?.by?.takeIf { it.isNotBlank() }
            preview = when {
                hasActiveGuess && session.group.activeGuessSummary.isNotBlank() ->
                    "🎯 猜词进行中：${session.group.activeGuessSummary}"
                by != null && latestText.isNotBlank() -> "$by：$latestText"
                latestText.isNotBlank() -> latestText
                else -> session.group.desc?.takeIf { it.isNotBlank() } ?: "${session.group.memberCount} 位成员"
            }
        }
        is UnifiedSessionItem.Dm -> {
            val t = session.thread
            title = t.peerName?.takeIf { it.isNotBlank() } ?: t.peerUsername ?: "私信"
            avatarName = title
            avatarUrl = t.peerAvatar
            val cleanBody = ChatRichTextParser.previewSummary(t.lastBody.orEmpty())
            val senderPrefix = when {
                cleanBody.isBlank() -> ""
                meId > 0L && t.lastSender == meId -> "我："
                t.lastSender != null && t.lastSender > 0L -> "对方："
                else -> ""
            }
            preview = (senderPrefix + cleanBody).ifBlank { "点击发送私信" }
        }
    }

    val rowBg = if (session.isPinned) palette.surface2.copy(alpha = 0.55f) else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBg)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box {
            if (isDm) {
                ForHappyAvatar(
                    username = avatarName,
                    avatarUrl = avatarUrl,
                    imageLoader = api.imageLoader,
                    size = ForHappyDimens.avatarConversation,
                )
            } else {
                Surface(
                    color = palette.primary.copy(alpha = 0.14f),
                    shape = CircleShape,
                    modifier = Modifier
                        .size(ForHappyDimens.avatarConversation)
                        .border(1.dp, palette.primary.copy(alpha = 0.28f), CircleShape),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Groups,
                            contentDescription = title,
                            tint = palette.primary,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
            if (session.isPinned) {
                Surface(
                    color = palette.surface,
                    shape = CircleShape,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(16.dp)
                        .border(1.dp, palette.line, CircleShape),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.PushPin,
                            contentDescription = "置顶",
                            tint = palette.primary,
                            modifier = Modifier.size(10.dp),
                        )
                    }
                }
            }
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                if (isDm) {
                    Surface(
                        color = palette.primary.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(4.dp),
                    ) {
                        Text(
                            text = "私",
                            color = palette.primary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                        )
                    }
                }
                Text(
                    text = title,
                    color = palette.text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (session.isMuted) {
                    Icon(
                        imageVector = Icons.Rounded.NotificationsOff,
                        contentDescription = "免打扰",
                        tint = palette.muted,
                        modifier = Modifier.size(14.dp),
                    )
                }
                if (timeText.isNotEmpty()) {
                    Text(
                        text = timeText,
                        color = palette.muted,
                        fontSize = 11.sp,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (atMeCount > 0) {
                    Text(
                        text = "[@我]",
                        color = palette.danger,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    text = preview,
                    color = if (hasActiveGuess) palette.primary else palette.muted,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                val badgeCount = session.unreadCount
                when {
                    badgeCount > 0 -> {
                        Surface(
                            color = if (atMeCount > 0 || !session.isMuted) palette.badge else palette.muted,
                            shape = RoundedCornerShape(999.dp),
                        ) {
                            Text(
                                text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                            )
                        }
                    }
                    session.isMuted && mutedRawUnread > 0 -> {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(palette.muted.copy(alpha = 0.65f)),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GoneGroupRow(
    gone: ChatGoneGroup,
    busy: Boolean,
    onJoin: () -> Unit,
) {
    val palette = LocalForHappyPalette.current
    val actionLabel = when (gone.kind) {
        "left", "kick" -> "重新申请"
        else -> if (gone.reqCount > 0) "审核中" else "加入"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Surface(
            color = palette.surface2,
            shape = CircleShape,
            modifier = Modifier.size(40.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.GroupAdd, contentDescription = null, tint = palette.muted, modifier = Modifier.size(19.dp))
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(gone.name, color = palette.text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(
                text = gone.desc?.takeIf { it.isNotBlank() } ?: if (gone.idle) "长期未登录移出，可一键重回" else "公开群组",
                color = palette.muted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        OutlinedButton(
            enabled = !busy,
            onClick = onJoin,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        ) {
            Text(if (busy) "提交中…" else actionLabel, fontSize = 12.sp)
        }
    }
}

@Composable
private fun SessionSheetActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    val palette = LocalForHappyPalette.current
    Surface(
        color = palette.surface,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, palette.line.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
            .clickable { onClick() },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(icon, contentDescription = null, tint = palette.primary, modifier = Modifier.size(19.dp))
            Text(label, color = palette.text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewDmUserSearchSheet(
    api: ForHappyApiClient,
    meId: Long,
    onDismiss: () -> Unit,
    onSelectUser: (ChatUser) -> Unit,
) {
    val palette = LocalForHappyPalette.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var users by remember { mutableStateOf<List<ChatUser>>(emptyList()) }

    LaunchedEffect(query) {
        delay(280L)
        loading = true
        val res = if (query.isBlank()) api.searchDmUsers("") else api.searchDmUsers(query)
        users = res.filter { it.id != meId }
        loading = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.bg,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 320.dp, max = 560.dp)
                .navigationBarsPadding()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("发起私信", color = palette.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Rounded.Close, contentDescription = "关闭", tint = palette.text)
                }
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("搜索用户名或昵称…", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            if (loading && users.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = palette.primary)
                }
            } else if (users.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) {
                    Text("没有找到可私信的用户", color = palette.muted, fontSize = 13.sp)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(users, key = { it.id }) { u ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(palette.surface)
                                .clickable { onSelectUser(u) }
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            ForHappyAvatar(
                                username = u.displayName,
                                avatarUrl = u.avatar,
                                imageLoader = api.imageLoader,
                                size = 36.dp,
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(u.displayName, color = palette.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                if (u.username.isNotBlank()) {
                                    Text("@${u.username}", color = palette.muted, fontSize = 11.sp)
                                }
                            }
                            Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null, tint = palette.primary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GlobalMessagesSearchDialog(
    state: ChatSessionController.ChatUiState,
    api: ForHappyApiClient,
    onDismiss: () -> Unit,
    onOpenGroup: (Long) -> Unit,
    onOpenDm: (DmThread) -> Unit,
    onJumpGroupMessage: (groupId: Long, msgId: Long) -> Unit,
) {
    val palette = LocalForHappyPalette.current
    var query by remember { mutableStateOf("") }
    var searchingRemote by remember { mutableStateOf(false) }
    var msgHits by remember { mutableStateOf<List<Pair<ChatGroup, ChatMessage>>>(emptyList()) }

    val matchingSessions = remember(query, state.groups, state.dmThreads) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) emptyList()
        else {
            val gHits = state.groups.filter { it.name.lowercase().contains(q) }.map { it to null }
            val dHits = state.dmThreads.filter {
                (it.peerName.orEmpty() + it.peerUsername.orEmpty()).lowercase().contains(q)
            }.map { null to it }
            gHits + dHits
        }
    }

    LaunchedEffect(query, state.groups) {
        val kw = query.trim()
        if (kw.length < 2) {
            msgHits = emptyList()
            return@LaunchedEffect
        }
        delay(320L)
        searchingRemote = true
        val targetGroups = state.groups.take(4)
        val combined = coroutineScope {
            targetGroups.map { grp ->
                async {
                    api.searchChatMessages(grp.id, kw).items.take(8).map { grp to it }
                }
            }.awaitAll().flatten()
        }.sortedByDescending { it.second.createdAt }
        msgHits = combined
        searchingRemote = false
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        ConfigureFullscreenDialogWindow(darkStatusIcons = !palette.isDark)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.bg)
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.surface)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回", tint = palette.text)
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("搜群聊、好友或聊天记录…", fontSize = 13.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
            HorizontalDivider(color = palette.line)

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (matchingSessions.isNotEmpty()) {
                    item {
                        Text("会话联系人", color = palette.muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    items(matchingSessions.size) { idx ->
                        val (grp, dm) = matchingSessions[idx]
                        Surface(
                            color = palette.surface,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, palette.line.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                                .clickable {
                                    if (grp != null) onOpenGroup(grp.id) else if (dm != null) onOpenDm(dm)
                                },
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Text(
                                    text = if (grp != null) "[群] ${grp.name}" else "[私信] ${dm?.peerName ?: dm?.peerUsername ?: "私信"}",
                                    color = palette.text,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    }
                }

                if (searchingRemote) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = palette.primary)
                            Spacer(Modifier.width(8.dp))
                            Text("正在搜索群聊记录…", color = palette.muted, fontSize = 12.sp)
                        }
                    }
                }

                if (msgHits.isNotEmpty()) {
                    item {
                        Text("聊天记录", color = palette.muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    items(msgHits, key = { (g, m) -> "g_${g.id}_m_${m.id}" }) { (grp, msg) ->
                        Surface(
                            color = palette.surface,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, palette.line.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                                .clickable { onJumpGroupMessage(grp.id, msg.id) },
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text("${grp.name} · ${msg.displayAuthor}", color = palette.primary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = ChatRichTextParser.previewSummary(msg.body).ifBlank { "[图片/媒体]" },
                                    color = palette.text,
                                    fontSize = 13.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Full-screen group information & management dialog (`02c §1`).
 */
@Composable
fun GroupInfoDialog(
    group: ChatGroup,
    state: ChatSessionController.ChatUiState,
    controller: ChatSessionController,
    api: ForHappyApiClient,
    me: MeUser?,
    onDismiss: () -> Unit,
    onOpenRedpackArchive: () -> Unit,
    onSelectMember: (GroupMemberItem) -> Unit,
    onLeftGroup: () -> Unit,
) {
    val palette = LocalForHappyPalette.current
    val scope = rememberCoroutineScope()
    val canManage = group.canManage || me?.isAdmin == true
    val isPinned = "g:${group.id}" in state.pinnedSessionKeys

    var members by remember(group.id) { mutableStateOf<List<GroupMemberItem>>(emptyList()) }
    var joinRequests by remember(group.id) { mutableStateOf<List<GroupMemberItem>>(emptyList()) }
    var memberQuery by remember { mutableStateOf("") }
    var editingNotice by remember { mutableStateOf(false) }
    var noticeDraft by remember(group.notice, group.desc) { mutableStateOf(group.notice ?: group.desc ?: "") }
    var noticeFeedback by remember { mutableStateOf<String?>(null) }
    var showLeaveConfirm by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    LaunchedEffect(group.id, canManage) {
        members = api.fetchGroupMembers(group.id)
        if (canManage) {
            joinRequests = api.fetchGroupOutsideUsers(group.id, kind = "req")
        }
    }

    val visibleMembers = remember(members, memberQuery) {
        val q = memberQuery.trim().lowercase()
        if (q.isEmpty()) members
        else members.filter { it.displayName.lowercase().contains(q) || it.username.lowercase().contains(q) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        ConfigureFullscreenDialogWindow(darkStatusIcons = !palette.isDark)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.bg)
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.surface)
                    .height(ForHappyDimens.topBarHeight)
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回", tint = palette.text)
                }
                Text("群聊资料与设置", color = palette.text, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            }
            HorizontalDivider(color = palette.line)

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Surface(
                        color = palette.surface,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().border(1.dp, palette.line, RoundedCornerShape(14.dp)),
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(group.name, color = palette.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("共 ${maxOf(group.memberCount, members.size)} 位群成员", color = palette.muted, fontSize = 12.sp)
                            group.desc?.takeIf { it.isNotBlank() }?.let {
                                Text(it, color = palette.muted, fontSize = 13.sp)
                            }
                        }
                    }
                }

                // Group notice
                item {
                    Surface(
                        color = palette.surface,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().border(1.dp, palette.line, RoundedCornerShape(14.dp)),
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("群公告", color = palette.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                if (canManage && !editingNotice) {
                                    TextButton(onClick = { editingNotice = true }) {
                                        Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("编辑", fontSize = 12.sp)
                                    }
                                }
                            }
                            if (editingNotice) {
                                OutlinedTextField(
                                    value = noticeDraft,
                                    onValueChange = { noticeDraft = it },
                                    minLines = 2,
                                    maxLines = 5,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = { editingNotice = false }) { Text("取消") }
                                    Button(
                                        enabled = !busy,
                                        onClick = {
                                            busy = true
                                            scope.launch {
                                                val res = api.updateGroupNotice(group.id, noticeDraft)
                                                busy = false
                                                res.onSuccess {
                                                    editingNotice = false
                                                    noticeFeedback = "群公告已更新"
                                                    controller.refreshGroups()
                                                }.onFailure {
                                                    noticeFeedback = it.message ?: "更新失败"
                                                }
                                            }
                                        },
                                    ) { Text("保存公告") }
                                }
                            } else {
                                Text(
                                    text = noticeDraft.ifBlank { "暂无群公告" },
                                    color = if (noticeDraft.isBlank()) palette.muted else palette.text,
                                    fontSize = 13.sp,
                                )
                            }
                            noticeFeedback?.let {
                                Text(it, color = palette.primary, fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Controls: Pin / Mute / Clean Mode / Scroll Policy / Redpack Archive
                item {
                    Surface(
                        color = palette.surface,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().border(1.dp, palette.line, RoundedCornerShape(14.dp)),
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("置顶群聊", color = palette.text, fontSize = 14.sp)
                                Switch(checked = isPinned, onCheckedChange = { controller.toggleSessionPinned("g:${group.id}") })
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("消息免打扰", color = palette.text, fontSize = 14.sp)
                                Switch(
                                    checked = group.muted,
                                    onCheckedChange = { nextMute ->
                                        controller.setGroupMutedLocal(group.id, nextMute)
                                        scope.launch {
                                            if (!api.setGroupMute(group.id, nextMute)) {
                                                controller.setGroupMutedLocal(group.id, !nextMute)
                                            }
                                        }
                                    },
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("清爽模式", color = palette.text, fontSize = 14.sp)
                                    Text("弱化气泡装扮与挂件效果，阅读更纯粹", color = palette.muted, fontSize = 11.sp)
                                }
                                Switch(
                                    checked = state.cleanDecoMode,
                                    onCheckedChange = { controller.setCleanDecoMode(it) },
                                )
                            }
                            HorizontalDivider(color = palette.line.copy(alpha = 0.6f))
                            Text("新消息滚屏策略", color = palette.text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                                ChatScrollPolicy.entries.forEach { pol ->
                                    val sel = state.scrollPolicy == pol
                                    Surface(
                                        color = if (sel) palette.primary else palette.surface2,
                                        shape = RoundedCornerShape(999.dp),
                                        modifier = Modifier.clickable { controller.setScrollPolicy(pol) },
                                    ) {
                                        Text(
                                            text = pol.label,
                                            color = if (sel) palette.onPrimary else palette.text,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        )
                                    }
                                }
                            }
                            HorizontalDivider(color = palette.line.copy(alpha = 0.6f))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onOpenRedpackArchive() }
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Rounded.CardGiftcard, contentDescription = null, tint = palette.redpacket, modifier = Modifier.size(18.dp))
                                    Text("本群红包记录与土豪榜", color = palette.text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                }
                                Text("查看 ›", color = palette.primary, fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Join requests (for admins/owners)
                if (canManage && joinRequests.isNotEmpty()) {
                    item {
                        Surface(
                            color = palette.surface,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().border(1.dp, palette.gold.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("入群审核 (${joinRequests.size})", color = palette.gold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                joinRequests.forEach { req ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        ForHappyAvatar(username = req.displayName, avatarUrl = req.avatar, imageLoader = api.imageLoader, size = 30.dp)
                                        Text(req.displayName, color = palette.text, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                        TextButton(onClick = {
                                            scope.launch {
                                                api.handleGroupJoinRequest(group.id, req.id, false)
                                                joinRequests = joinRequests.filterNot { it.id == req.id }
                                            }
                                        }) { Text("拒绝", color = palette.danger, fontSize = 12.sp) }
                                        Button(
                                            onClick = {
                                                scope.launch {
                                                    api.handleGroupJoinRequest(group.id, req.id, true)
                                                    joinRequests = joinRequests.filterNot { it.id == req.id }
                                                    members = api.fetchGroupMembers(group.id)
                                                }
                                            },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                        ) { Text("通过", fontSize = 12.sp) }
                                    }
                                }
                            }
                        }
                    }
                }

                // Members search & list
                item {
                    OutlinedTextField(
                        value = memberQuery,
                        onValueChange = { memberQuery = it },
                        placeholder = { Text("搜索群成员 (${members.size})…", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                items(visibleMembers.take(80), key = { "mem_${it.id}" }) { m ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(palette.surface)
                            .clickable { onSelectMember(m) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        ForHappyAvatar(username = m.displayName, avatarUrl = m.avatar, imageLoader = api.imageLoader, size = 34.dp)
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text(m.displayName, color = palette.text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                if (m.owner) BadgePill("群主", containerColor = palette.gold.copy(alpha = 0.2f), contentColor = palette.gold)
                                else if (m.admin) BadgePill("管理", containerColor = palette.primaryContainer, contentColor = palette.primary)
                                m.title?.takeIf { it.isNotBlank() }?.let { BadgePill(it) }
                            }
                            if (m.username.isNotBlank()) {
                                Text("@${m.username}", color = palette.muted, fontSize = 11.sp)
                            }
                        }
                    }
                }

                if (group.id != 1L && me != null) {
                    item {
                        Spacer(Modifier.height(6.dp))
                        OutlinedButton(
                            onClick = { showLeaveConfirm = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.danger),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("退出该群聊")
                        }
                    }
                }
            }
        }
    }

    if (showLeaveConfirm && me != null) {
        AlertDialog(
            onDismissRequest = { showLeaveConfirm = false },
            title = { Text("退出「${group.name}」") },
            text = { Text("确定要退出当前群聊吗？退出后可在「可加入的群」重新申请加入。") },
            confirmButton = {
                TextButton(onClick = {
                    showLeaveConfirm = false
                    scope.launch {
                        if (api.kickOrLeaveGroupMember(group.id, me.id)) {
                            controller.refreshGroups()
                            onLeftGroup()
                        }
                    }
                }) { Text("确认退出", color = palette.danger) }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveConfirm = false }) { Text("取消") }
            },
        )
    }
}

/**
 * Bottom sheet shown when tapping a group member's avatar (`02c §1`).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MemberProfileSheet(
    member: GroupMemberItem,
    groupId: Long,
    canKick: Boolean,
    api: ForHappyApiClient,
    me: MeUser?,
    onDismiss: () -> Unit,
    onMentionUser: (String) -> Unit,
    onSendDm: (ChatUser) -> Unit,
    onViewWall: (String) -> Unit,
    onKicked: () -> Unit = {},
) {
    val palette = LocalForHappyPalette.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.bg,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ForHappyAvatar(
                    username = member.displayName,
                    avatarUrl = member.avatar,
                    imageLoader = api.imageLoader,
                    size = 54.dp,
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    val disguiseMode = LocalPrivacyStealthConfig.current.disguiseMode
                    val memberIdentity = remember(member.deco, disguiseMode) {
                        if (disguiseMode == AppDisguiseMode.NONE) {
                            BadgeAndCosmeticRegistry.resolveEquippedIdentity(member.deco, where = "card")
                        } else {
                            null
                        }
                    }
                    val gradColors = remember(memberIdentity?.nickGradientHexes, palette.text) {
                        memberIdentity?.nickGradientHexes?.map { parseSafeHex(it, palette.text) }?.takeIf { it.size >= 2 }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = member.displayName,
                            style = if (gradColors != null) {
                                TextStyle(brush = Brush.horizontalGradient(gradColors), fontSize = 17.sp, fontWeight = FontWeight.Bold)
                            } else {
                                TextStyle(color = palette.text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                            },
                        )
                        if (disguiseMode == AppDisguiseMode.NONE) {
                            if (memberIdentity?.title != null) {
                                TitleChipView(title = memberIdentity.title)
                            } else {
                                member.title?.takeIf { it.isNotBlank() }?.let {
                                    BadgePill(it, containerColor = palette.gold.copy(alpha = 0.2f), contentColor = palette.gold)
                                }
                            }
                            memberIdentity?.badge?.let {
                                BadgeChipView(badge = it, imageLoader = api.imageLoader, mini = false)
                            }
                        }
                    }
                    if (member.username.isNotBlank()) {
                        Text("@${member.username} · TL${member.trustLevel}", color = palette.muted, fontSize = 12.sp)
                    }
                }
            }

            HorizontalDivider(color = palette.line)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                if (member.username.isNotBlank()) {
                    OutlinedButton(
                        onClick = { onMentionUser("@${member.username} ") },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Rounded.AlternateEmail, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("@TA", fontSize = 12.sp)
                    }
                }
                if (member.id > 0L && member.id != me?.id) {
                    Button(
                        onClick = {
                            onSendDm(
                                ChatUser(
                                    id = member.id,
                                    username = member.username,
                                    name = member.name ?: member.displayName,
                                    nick = member.nick,
                                    avatar = member.avatar,
                                    deco = member.deco,
                                ),
                            )
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("发私信", fontSize = 12.sp)
                    }
                }
            }

            if (member.username.isNotBlank()) {
                OutlinedButton(
                    onClick = { onViewWall(member.username) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Rounded.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("在图廊查看 TA 的作品", fontSize = 13.sp)
                }
            }

            if (canKick && member.id > 0L && member.id != me?.id) {
                TextButton(
                    onClick = {
                        scope.launch {
                            if (api.kickOrLeaveGroupMember(groupId, member.id)) {
                                onKicked()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("将其移出本群", color = palette.danger, fontSize = 13.sp)
                }
            }
        }
    }
}

/**
 * Full-screen in-group history search with keyword & multi-speaker filter capsules (`02c §1`).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatHistorySearchDialog(
    groupId: Long,
    groupName: String,
    api: ForHappyApiClient,
    onDismiss: () -> Unit,
    onSelectMessage: (Long) -> Unit,
) {
    val palette = LocalForHappyPalette.current
    val scope = rememberCoroutineScope()

    var queryText by remember { mutableStateOf("") }
    var onlyImages by remember { mutableStateOf(false) }
    var selectedSpeakers by remember { mutableStateOf<Set<String>>(emptySet()) }
    var members by remember { mutableStateOf<List<GroupMemberItem>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var hasMore by remember { mutableStateOf(false) }

    LaunchedEffect(groupId) {
        members = api.fetchGroupMembers(groupId).take(16)
    }

    fun runSearch(beforeId: Long? = null) {
        val raw = queryText.trim()
        val atMatches = Regex("""(?:^|\s)@(\S+)""").findAll(raw).map { it.groupValues[1] }.toList()
        val kw = raw.replace(Regex("""(?:^|\s)@\S+"""), "").trim()
        val speakerSet = (selectedSpeakers + atMatches).filter { it.isNotBlank() }.toSet()
        if (kw.isBlank() && speakerSet.isEmpty() && !onlyImages) {
            results = emptyList()
            hasMore = false
            return
        }
        scope.launch {
            loading = true
            if (speakerSet.size <= 1) {
                val res = api.searchChatMessages(
                    groupId = groupId,
                    q = kw,
                    user = speakerSet.firstOrNull().orEmpty(),
                    onlyImages = onlyImages,
                    beforeId = beforeId,
                )
                results = if (beforeId == null) res.items else results + res.items
                hasMore = res.more
            } else {
                val merged = coroutineScope {
                    speakerSet.take(5).map { sp ->
                        async {
                            api.searchChatMessages(
                                groupId = groupId,
                                q = kw,
                                user = sp,
                                onlyImages = onlyImages,
                                beforeId = beforeId,
                            ).items
                        }
                    }.awaitAll().flatten()
                }.distinctBy { it.id }.sortedByDescending { it.id }
                results = if (beforeId == null) merged else (results + merged).distinctBy { it.id }
                hasMore = false
            }
            loading = false
        }
    }

    LaunchedEffect(queryText, onlyImages, selectedSpeakers) {
        delay(300L)
        runSearch(null)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        ConfigureFullscreenDialogWindow(darkStatusIcons = !palette.isDark)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.bg)
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.surface)
                    .height(ForHappyDimens.topBarHeight)
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回", tint = palette.text)
                }
                Text("搜索「$groupName」聊天记录", color = palette.text, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            }
            HorizontalDivider(color = palette.line)

            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = queryText,
                    onValueChange = { queryText = it },
                    placeholder = { Text("输入关键词，或点选下方发言人（支持多选）", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("只看包含图片的消息", color = palette.muted, fontSize = 12.sp)
                    Switch(checked = onlyImages, onCheckedChange = { onlyImages = it })
                }

                if (members.isNotEmpty()) {
                    Text("发言人筛选（可多选）", color = palette.muted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(members, key = { "spk_${it.id}_${it.username}" }) { m ->
                            val key = m.username.ifBlank { m.displayName }
                            val selected = key in selectedSpeakers
                            Surface(
                                color = if (selected) palette.primary else palette.surface2,
                                shape = RoundedCornerShape(999.dp),
                                modifier = Modifier
                                    .border(1.dp, if (selected) palette.primary else palette.line, RoundedCornerShape(999.dp))
                                    .clickable {
                                        selectedSpeakers = if (selected) selectedSpeakers - key else selectedSpeakers + key
                                    },
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    if (selected) {
                                        Icon(Icons.Rounded.Check, contentDescription = null, tint = palette.onPrimary, modifier = Modifier.size(12.dp))
                                    }
                                    Text(
                                        text = m.displayName,
                                        color = if (selected) palette.onPrimary else palette.text,
                                        fontSize = 11.sp,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = palette.line)

            if (loading && results.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = palette.primary)
                }
            } else if (results.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (queryText.isBlank() && selectedSpeakers.isEmpty() && !onlyImages) {
                            "输入关键词、选择发言人或勾选只看图"
                        } else {
                            "没有找到相关消息"
                        },
                        color = palette.muted,
                        fontSize = 13.sp,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(results, key = { "hist_srch_${it.id}" }) { m ->
                        val ts = remember(m.createdAt) {
                            runCatching {
                                SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(Date(m.createdAt * 1000L))
                            }.getOrDefault("")
                        }
                        Surface(
                            color = palette.surface,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, palette.line.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                                .clickable { onSelectMessage(m.id) },
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                ForHappyAvatar(
                                    username = m.displayAuthor,
                                    avatarUrl = m.resolvedAvatar,
                                    imageLoader = api.imageLoader,
                                    size = 32.dp,
                                )
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text(m.displayAuthor, color = palette.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        Text(ts, color = palette.muted, fontSize = 10.sp)
                                    }
                                    Text(
                                        text = ChatRichTextParser.previewSummary(m.body).ifBlank { if (m.allMedia.isNotEmpty()) "[图片]" else "[消息]" },
                                        color = palette.muted,
                                        fontSize = 12.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                m.allMedia.firstOrNull()?.resolvedUrl?.let { thumb ->
                                    AsyncImage(
                                        model = MediaUrlRewriter.rewriteWithExtProxy(thumb),
                                        imageLoader = api.imageLoader,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.size(38.dp).clip(RoundedCornerShape(8.dp)),
                                    )
                                }
                            }
                        }
                    }

                    if (hasMore) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                OutlinedButton(onClick = { runSearch(results.lastOrNull()?.id) }) {
                                    Text("加载更多结果")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatSessionTime(epochSec: Long): String {
    if (epochSec <= 0L) return ""
    val nowSec = ServerClock.serverNowSec()
    val todayDay = (nowSec + 28_800L).floorDiv(86_400L)
    val itemDay = (epochSec + 28_800L).floorDiv(86_400L)
    val pattern = when {
        itemDay == todayDay -> "HH:mm"
        todayDay - itemDay == 1L -> "'昨天' HH:mm"
        else -> "MM-dd"
    }
    return runCatching {
        SimpleDateFormat(pattern, Locale.CHINA).apply {
            timeZone = java.util.TimeZone.getTimeZone("Asia/Shanghai")
        }.format(Date(epochSec * 1000L))
    }.getOrDefault("")
}
