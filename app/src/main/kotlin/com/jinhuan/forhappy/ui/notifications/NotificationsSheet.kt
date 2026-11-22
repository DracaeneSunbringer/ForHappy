package com.jinhuan.forhappy.ui.notifications

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.jinhuan.forhappy.core.model.NOTIF_CATEGORY_TABS
import com.jinhuan.forhappy.core.model.NOTIF_PREF_OPTIONS
import com.jinhuan.forhappy.core.model.NotifItem
import com.jinhuan.forhappy.core.model.SiteNotice
import com.jinhuan.forhappy.core.network.api.ForHappyApiClient
import com.jinhuan.forhappy.core.network.media.MediaUrlRewriter
import com.jinhuan.forhappy.ui.components.BadgePill
import com.jinhuan.forhappy.ui.components.ForHappyAvatar
import com.jinhuan.forhappy.ui.components.ReactionEmojiView
import com.jinhuan.forhappy.ui.theme.ForHappyDimens
import com.jinhuan.forhappy.ui.theme.LocalForHappyPalette
import com.jinhuan.forhappy.ui.wall.ConfigureFullscreenDialogWindow
import com.jinhuan.forhappy.ui.wall.DialogBackOwnerScope
import com.jinhuan.forhappy.ui.wall.FullscreenDialogProperties
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

val NOTIF_PREF_GROUPS: List<Pair<String, List<String>>> = listOf(
    "互动" to listOf("like", "emoji", "reply", "comment", "creply", "mention", "post_at"),
    "群聊" to listOf("chat_at", "chat_reply", "packet", "join_req"),
    "内容" to listOf("post", "ai_comment"),
    "系统" to listOf("release", "farm_conflict", "badge"),
)

/**
 * Full-screen 9-tab notification center matching `02e §2`:
 * - Unconditional 9-category filter tabs (`全部/@我/评论/回应/喜欢/群聊/新帖/农场/系统`) + unread-only toggle
 * - Pinned unread site announcements with dismiss support
 * - Instant single-item / bulk read sync + long-press toggle action & detail modal
 * - Grouped 16-item notification preference switches (`PUT /api/notifs/prefs`)
 */
@Composable
fun NotificationsSheet(
    api: ForHappyApiClient,
    items: List<NotifItem>,
    unreadCount: Int,
    loading: Boolean,
    notifPrefs: Map<String, Boolean>,
    onRefresh: () -> Unit,
    onMarkAllRead: () -> Unit,
    onMarkItemRead: (NotifItem) -> Unit,
    onUpdatePref: (String, Boolean) -> Unit,
    onOpenMeme: (Long) -> Unit,
    onOpenChat: (groupId: Long, messageId: Long?) -> Unit,
    onNavigateHub: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onDismiss: () -> Unit,
    siteNotices: List<SiteNotice> = emptyList(),
    dismissedNoticeIds: Set<Long> = emptySet(),
    onDismissNotice: (Long) -> Unit = {},
    onToggleItemReadLocal: ((NotifItem) -> Unit)? = null,
    onOpenPetsRoute: (() -> Unit)? = null,
    initialShowSettings: Boolean = false,
) {
    val palette = LocalForHappyPalette.current

    var selectedTabId by remember { mutableStateOf("all") }
    var onlyUnread by remember { mutableStateOf(false) }
    var showSettings by remember(initialShowSettings) { mutableStateOf(initialShowSettings) }
    var detailNotif by remember { mutableStateOf<NotifItem?>(null) }
    var longPressMenuNotif by remember { mutableStateOf<NotifItem?>(null) }

    val localUnread = remember(items, unreadCount) {
        maxOf(unreadCount, items.count { it.isUnread })
    }
    val hasAnyUnread = remember(items, unreadCount) {
        unreadCount > 0 || items.any { it.isUnread }
    }

    val activeTab = remember(selectedTabId) {
        NOTIF_CATEGORY_TABS.firstOrNull { it.id == selectedTabId } ?: NOTIF_CATEGORY_TABS.first()
    }
    val pinnedNotices = remember(siteNotices, dismissedNoticeIds) {
        siteNotices.filter { it.id !in dismissedNoticeIds && it.body.isNotBlank() }
    }
    val filteredItems = remember(items, activeTab, onlyUnread) {
        items.filter { item ->
            val kindOk = activeTab.kinds == null || item.kind in activeTab.kinds
            val unreadOk = !onlyUnread || item.isUnread
            kindOk && unreadOk
        }.sortedWith(
            compareByDescending<NotifItem> { it.isUnread }
                .thenByDescending { it.effectiveAtSec },
        )
    }

    Dialog(
        onDismissRequest = {
            if (showSettings && !initialShowSettings) {
                showSettings = false
            } else {
                onDismiss()
            }
        },
        properties = FullscreenDialogProperties,
    ) {
        ConfigureFullscreenDialogWindow(darkStatusIcons = !palette.isDark)
        DialogBackOwnerScope {
            BackHandler(enabled = showSettings && !initialShowSettings) {
                showSettings = false
            }
        val navBottomPad = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.bg),
        ) {
            // 48dp Fullscreen Header
            Surface(color = palette.surface) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .height(ForHappyDimens.topBarHeight)
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f),
                    ) {
                        IconButton(
                            onClick = {
                                if (showSettings && !initialShowSettings) {
                                    showSettings = false
                                } else {
                                    onDismiss()
                                }
                            },
                            modifier = Modifier.size(38.dp),
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回", tint = palette.text)
                        }
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (showSettings) "提醒偏好设置" else "互动与系统提醒",
                            color = palette.text,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        if (!showSettings && localUnread > 0) {
                            Spacer(Modifier.width(8.dp))
                            BadgePill(
                                text = if (localUnread > 99) "99+ 未读" else "$localUnread 未读",
                                containerColor = palette.primary,
                                contentColor = palette.onPrimary,
                            )
                        }
                    }

                    if (!showSettings) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (hasAnyUnread) {
                                TextButton(
                                    onClick = onMarkAllRead,
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                ) {
                                    Icon(
                                        Icons.Rounded.DoneAll,
                                        contentDescription = null,
                                        tint = palette.primary,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("全部已读", color = palette.primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            IconButton(
                                onClick = onRefresh,
                                enabled = !loading,
                                modifier = Modifier.size(34.dp),
                            ) {
                                if (loading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = palette.primary,
                                    )
                                } else {
                                    Icon(
                                        Icons.Rounded.Refresh,
                                        contentDescription = "刷新提醒",
                                        tint = palette.muted,
                                        modifier = Modifier.size(19.dp),
                                    )
                                }
                            }
                            IconButton(
                                onClick = { showSettings = true },
                                modifier = Modifier.size(34.dp),
                            ) {
                                Icon(
                                    Icons.Rounded.Settings,
                                    contentDescription = "提醒设置",
                                    tint = palette.muted,
                                    modifier = Modifier.size(19.dp),
                                )
                            }
                        }
                    }
                }
            }
            HorizontalDivider(color = palette.line)

            if (showSettings) {
                val labelByKey = remember { NOTIF_PREF_OPTIONS.toMap() }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                ) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "选择需要接收的消息与系统提醒类型（即时同步到账号设置）",
                        color = palette.muted,
                        fontSize = 12.sp,
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = 20.dp + navBottomPad),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        NOTIF_PREF_GROUPS.forEach { (groupTitle, keys) ->
                            item(key = "hdr_$groupTitle") {
                                Text(
                                    text = groupTitle,
                                    color = palette.primary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                            }
                            item(key = "grp_$groupTitle") {
                                Surface(
                                    color = palette.surface,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, palette.line, RoundedCornerShape(14.dp)),
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                                        keys.forEachIndexed { idx, key ->
                                            val label = labelByKey[key] ?: key
                                            val checked = notifPrefs[key] != false
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 6.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Text(
                                                    text = label,
                                                    color = palette.text,
                                                    fontSize = 13.sp,
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .padding(end = 12.dp),
                                                )
                                                Switch(
                                                    checked = checked,
                                                    onCheckedChange = { next -> onUpdatePref(key, next) },
                                                )
                                            }
                                            if (idx < keys.lastIndex) {
                                                HorizontalDivider(color = palette.line.copy(alpha = 0.55f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Filter bar: unread chip + all 9 category tabs
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.surface)
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    item(key = "filter_unread") {
                        FilterChip(
                            selected = onlyUnread,
                            onClick = { onlyUnread = !onlyUnread },
                            label = { Text("仅看未读", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = palette.primary,
                                selectedLabelColor = palette.onPrimary,
                            ),
                        )
                    }
                    items(NOTIF_CATEGORY_TABS, key = { it.id }) { tab ->
                        val tabUnread = remember(items, tab) {
                            items.count { it.isUnread && (tab.kinds == null || it.kind in tab.kinds) }
                        }
                        val label = if (tabUnread > 0 && tab.kinds != null) {
                            "${tab.label} $tabUnread"
                        } else {
                            tab.label
                        }
                        FilterChip(
                            selected = activeTab.id == tab.id,
                            onClick = { selectedTabId = tab.id },
                            label = { Text(label, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = palette.primary.copy(alpha = 0.16f),
                                selectedLabelColor = palette.primary,
                            ),
                        )
                    }
                }
                HorizontalDivider(color = palette.line)

                when {
                    loading && items.isEmpty() && pinnedNotices.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(color = palette.primary)
                        }
                    }

                    filteredItems.isEmpty() && pinnedNotices.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = when {
                                    onlyUnread -> "当前分类下没有未读提醒"
                                    activeTab.kinds != null -> "这一类暂时没有提醒"
                                    else -> "暂无互动或系统提醒"
                                },
                                color = palette.muted,
                                fontSize = 13.sp,
                            )
                        }
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentPadding = PaddingValues(
                                start = 14.dp,
                                top = 14.dp,
                                end = 14.dp,
                                bottom = 14.dp + navBottomPad,
                            ),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (pinnedNotices.isNotEmpty() && (activeTab.id == "all" || activeTab.id == "system")) {
                                items(pinnedNotices, key = { "notice_${it.id}" }) { notice ->
                                    Surface(
                                        color = palette.highlight.copy(alpha = if (palette.isDark) 0.14f else 0.55f),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, palette.gold.copy(alpha = 0.45f), RoundedCornerShape(12.dp)),
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                            BadgePill("全站公告", containerColor = palette.gold.copy(alpha = 0.22f), contentColor = palette.text)
                                            Text(
                                                text = notice.body.replace(Regex("<[^>]+>"), " ").replace(Regex("\\s+"), " ").trim(),
                                                color = palette.text,
                                                fontSize = 12.sp,
                                                lineHeight = 17.sp,
                                                modifier = Modifier.weight(1f),
                                            )
                                            IconButton(
                                                onClick = { onDismissNotice(notice.id) },
                                                modifier = Modifier.size(26.dp),
                                            ) {
                                                Icon(Icons.Rounded.Close, contentDescription = "关闭公告", tint = palette.muted, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }

                            itemsIndexed(
                                items = filteredItems,
                                key = { idx, item -> "${idx}_${item.identityKey}" },
                            ) { _, item ->
                                NotificationRowCard(
                                    api = api,
                                    item = item,
                                    onClick = {
                                        onMarkItemRead(item)
                                        when {
                                            item.isChatGroupNotif && (item.gid ?: 0L) > 0L -> {
                                                onDismiss()
                                                onOpenChat(
                                                    item.gid!!,
                                                    item.memeId?.takeIf { item.isChatMessageNotif && it > 0L },
                                                )
                                            }

                                            item.isPostNotif && (item.memeId ?: 0L) > 0L -> {
                                                onDismiss()
                                                onOpenMeme(item.memeId!!)
                                            }

                                            item.kind == "farm_conflict" -> {
                                                onDismiss()
                                                onOpenPetsRoute?.invoke() ?: onNavigateHub()
                                            }

                                            else -> {
                                                detailNotif = item
                                            }
                                        }
                                    },
                                    onLongClick = {
                                        longPressMenuNotif = item
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
        }
    }

    longPressMenuNotif?.let { target ->
        AlertDialog(
            onDismissRequest = { longPressMenuNotif = null },
            containerColor = palette.surface,
            titleContentColor = palette.text,
            textContentColor = palette.text,
            title = { Text(target.headlineSummary(), fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        color = palette.surface2,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                longPressMenuNotif = null
                                onToggleItemReadLocal?.invoke(target) ?: onMarkItemRead(target)
                            },
                    ) {
                        Text(
                            text = if (target.isUnread) "标为已读" else "标为未读",
                            color = palette.text,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        )
                    }
                    Surface(
                        color = palette.surface2,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                longPressMenuNotif = null
                                detailNotif = target
                            },
                    ) {
                        Text(
                            text = "查看详情",
                            color = palette.text,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { longPressMenuNotif = null }) { Text("取消") }
            },
        )
    }

    detailNotif?.let { item ->
        NotificationDetailDialog(
            item = item,
            onDismiss = { detailNotif = null },
            onOpenChat = { gid, msgId ->
                detailNotif = null
                onDismiss()
                onOpenChat(gid, msgId)
            },
            onOpenMeme = { memeId ->
                detailNotif = null
                onDismiss()
                onOpenMeme(memeId)
            },
            onOpenPets = {
                detailNotif = null
                onDismiss()
                onOpenPetsRoute?.invoke() ?: onNavigateHub()
            },
            onOpenUrl = { url ->
                detailNotif = null
                onDismiss()
                onOpenUrl(url)
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NotificationRowCard(
    api: ForHappyApiClient,
    item: NotifItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val palette = LocalForHappyPalette.current
    val bodyPreview = remember(item) { item.detailBodyText() }
    val showSystemAvatar = remember(item) { item.isSystemAlertKind() }

    Surface(
        color = if (item.isUnread) palette.primary.copy(alpha = 0.08f) else palette.surface2.copy(alpha = 0.45f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (item.isUnread) palette.primary.copy(alpha = 0.35f) else palette.line,
                shape = RoundedCornerShape(12.dp),
            )
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                contentAlignment = Alignment.TopStart,
                modifier = Modifier.padding(top = 2.dp),
            ) {
                if (showSystemAvatar) {
                    SystemNotifIconBadge(kind = item.kind)
                } else {
                    ForHappyAvatar(
                        username = item.actorDisplay,
                        avatarUrl = item.avatar,
                        imageLoader = api.imageLoader,
                        size = 36.dp,
                    )
                }
                if (item.isUnread) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(palette.primary)
                            .border(1.5.dp, palette.surface, CircleShape),
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = item.headlineSummary(),
                        color = palette.text,
                        fontSize = 13.sp,
                        fontWeight = if (item.isUnread) FontWeight.SemiBold else FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (item.kind == "emoji" && !item.valueText.isNullOrBlank()) {
                        ReactionEmojiView(
                            emojiKey = item.valueText,
                            imageLoader = api.imageLoader,
                            size = 18.dp,
                        )
                    }
                }

                if (bodyPreview.isNotBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = bodyPreview,
                        color = if (item.isUnread) palette.text.copy(alpha = 0.88f) else palette.muted,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        maxLines = if (showSystemAvatar) 3 else 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = formatRelativeNotifTime(item.effectiveAtSec),
                        color = palette.muted,
                        fontSize = 11.sp,
                    )
                    val actionHint = when {
                        item.isChatGroupNotif && (item.gid ?: 0L) > 0L -> "前往群聊 ›"
                        item.isPostNotif && (item.memeId ?: 0L) > 0L -> "查看原帖 ›"
                        else -> "查看详情 ›"
                    }
                    Text(
                        text = actionHint,
                        color = palette.primary.copy(alpha = 0.85f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            if (!item.thumb.isNullOrBlank()) {
                AsyncImage(
                    model = remember(item.thumb) { MediaUrlRewriter.rewriteWithExtProxy(item.thumb) },
                    imageLoader = api.imageLoader,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(palette.surface2),
                )
            }
        }
    }
}

@Composable
private fun SystemNotifIconBadge(kind: String) {
    val palette = LocalForHappyPalette.current
    val icon = when (kind) {
        "farm_conflict" -> Icons.Rounded.Security
        "badge" -> Icons.Rounded.EmojiEvents
        "packet" -> Icons.Rounded.CardGiftcard
        "ai_alert" -> Icons.Rounded.AutoAwesome
        "release", "skin_alert", "pet_refund" -> Icons.Rounded.Campaign
        else -> Icons.Rounded.Notifications
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(palette.primary.copy(alpha = 0.14f))
            .border(1.dp, palette.primary.copy(alpha = 0.25f), CircleShape),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = palette.primary,
            modifier = Modifier.size(19.dp),
        )
    }
}

@Composable
private fun NotificationDetailDialog(
    item: NotifItem,
    onDismiss: () -> Unit,
    onOpenChat: (groupId: Long, messageId: Long?) -> Unit,
    onOpenMeme: (Long) -> Unit,
    onOpenPets: () -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    val palette = LocalForHappyPalette.current
    val fullText = remember(item) {
        listOfNotNull(
            item.title?.takeIf { it.isNotBlank() && !item.headlineSummary().contains(it) },
            item.valueText?.takeIf { it.isNotBlank() },
            item.body?.takeIf { it.isNotBlank() && it != item.valueText },
        ).joinToString("\n\n").ifBlank { item.detailBodyText().ifBlank { item.headlineSummary() } }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        DialogBackOwnerScope {
            BackHandler(onBack = onDismiss)
            Surface(
            color = palette.surface,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .border(1.dp, palette.line, RoundedCornerShape(16.dp)),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.headlineSummary(),
                            color = palette.text,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        if (item.effectiveAtSec > 0L) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = formatFullNotifTime(item.effectiveAtSec),
                                color = palette.muted,
                                fontSize = 11.sp,
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = "关闭", tint = palette.muted)
                    }
                }

                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = palette.line)
                Spacer(Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 64.dp, max = 360.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    SelectionContainer {
                        Text(
                            text = fullText,
                            color = palette.text,
                            fontSize = 14.sp,
                            lineHeight = 21.sp,
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("关闭", color = palette.muted)
                    }
                    when {
                        item.isChatGroupNotif && (item.gid ?: 0L) > 0L -> {
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    onOpenChat(
                                        item.gid!!,
                                        item.memeId?.takeIf { item.isChatMessageNotif && it > 0L },
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = palette.primary,
                                    contentColor = palette.onPrimary,
                                ),
                            ) {
                                Text("前往群聊")
                            }
                        }

                        item.isPostNotif && (item.memeId ?: 0L) > 0L -> {
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = { onOpenMeme(item.memeId!!) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = palette.primary,
                                    contentColor = palette.onPrimary,
                                ),
                            ) {
                                Text("查看原帖")
                            }
                        }

                        item.kind == "farm_conflict" -> {
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = onOpenPets,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = palette.primary,
                                    contentColor = palette.onPrimary,
                                ),
                            ) {
                                Text("前往鱼鱼谷")
                            }
                        }

                        !item.url.isNullOrBlank() || !item.href.isNullOrBlank() -> {
                            val target = item.url?.takeIf { it.isNotBlank() } ?: item.href.orEmpty()
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = { onOpenUrl(target) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = palette.primary,
                                    contentColor = palette.onPrimary,
                                ),
                            ) {
                                Text("打开页面")
                            }
                        }
                    }
                }
            }
        }
        }
    }
}

internal fun formatRelativeNotifTime(atSec: Long, nowMs: Long = System.currentTimeMillis()): String {
    if (atSec <= 0L) return ""
    val diffSec = (nowMs / 1000L) - atSec
    if (diffSec in 0L until 60L) return "刚刚"
    if (diffSec in 60L until 3600L) return "${diffSec / 60L} 分钟前"

    val targetMs = atSec * 1000L
    val targetCal = Calendar.getInstance().apply { timeInMillis = targetMs }
    val todayCal = Calendar.getInstance().apply {
        timeInMillis = nowMs
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val yesterdayStartMs = todayCal.timeInMillis - 86_400_000L
    val hm = SimpleDateFormat("HH:mm", Locale.CHINA).format(Date(targetMs))
    if (targetMs >= todayCal.timeInMillis) return "今天 $hm"
    if (targetMs >= yesterdayStartMs) return "昨天 $hm"
    val sameYear = targetCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR)
    val pattern = if (sameYear) "M月d日 HH:mm" else "yyyy年M月d日 HH:mm"
    return SimpleDateFormat(pattern, Locale.CHINA).format(Date(targetMs))
}

private fun formatFullNotifTime(atSec: Long): String {
    if (atSec <= 0L) return ""
    return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(Date(atSec * 1000L))
}
