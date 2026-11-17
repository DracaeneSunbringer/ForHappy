package com.jinhuan.forhappy.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jinhuan.forhappy.ForHappyApp
import com.jinhuan.forhappy.core.model.AppDisguiseMode
import com.jinhuan.forhappy.core.model.AppIconAlias
import com.jinhuan.forhappy.core.model.AppThemeMode
import com.jinhuan.forhappy.core.model.DynamicModule
import com.jinhuan.forhappy.core.model.MeUser
import com.jinhuan.forhappy.core.model.NotifItem
import com.jinhuan.forhappy.core.model.PrivacyStealthConfig
import com.jinhuan.forhappy.core.update.UpdateStatus
import com.jinhuan.forhappy.ui.chat.ChatNotificationJump
import com.jinhuan.forhappy.ui.chat.ChatScreen
import com.jinhuan.forhappy.ui.components.BadgePill
import com.jinhuan.forhappy.ui.disguise.BossDocScreen
import com.jinhuan.forhappy.ui.disguise.DoubleBackBossHandler
import com.jinhuan.forhappy.ui.disguise.FeishuDisguiseScreen
import com.jinhuan.forhappy.ui.disguise.LinuxDoDisguiseScreen
import com.jinhuan.forhappy.ui.hub.HubScreen
import com.jinhuan.forhappy.ui.me.MeScreen
import com.jinhuan.forhappy.ui.me.PrivacyStealthSettingsContent
import com.jinhuan.forhappy.ui.notifications.NotificationsSheet
import com.jinhuan.forhappy.ui.pets.PetsScreen
import com.jinhuan.forhappy.ui.theme.LocalForHappyPalette
import com.jinhuan.forhappy.ui.wall.FullscreenMediaOverlay
import com.jinhuan.forhappy.ui.wall.MemeDetailModal
import com.jinhuan.forhappy.ui.wall.WallScreen
import com.jinhuan.forhappy.ui.web.LoginWebSheet
import com.jinhuan.forhappy.ui.web.ShelllessWebScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Main thumb-zone 4+1 bottom navigation shell for ForHappy:
 * 1. `图廊` (Wall)
 * 2. `群聊` (Chat + DM)
 * 3. `Pinned Dynamic Slot` (Defaults to `鱼鱼谷`, swappable in Hub)
 * 4. `百宝箱` (Dynamic Topbar Hub + Tools)
 * 5. `我的` (Me + Wallet + Disguise + Rust Diagnostics)
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ForHappyShell(
    app: ForHappyApp,
    themeMode: AppThemeMode,
    privacyConfig: PrivacyStealthConfig,
    bossKeyActive: Boolean,
    privacyShieldEnabled: Boolean,
    activeIconAlias: AppIconAlias,
    onChangeTheme: (AppThemeMode) -> Unit,
    onChangeDisguise: (AppDisguiseMode) -> Unit,
    onUpdatePrivacyConfig: (PrivacyStealthConfig) -> Unit,
    onSetBossKeyActive: (Boolean) -> Unit,
    onTogglePrivacyShield: (Boolean) -> Unit,
    onChangeIconAlias: (AppIconAlias) -> Unit,
) {
    val palette = LocalForHappyPalette.current
    val scope = rememberCoroutineScope()
    val api = app.apiClient
    val chatController = app.chatController

    var activeTab by remember { mutableStateOf("wall") } // "wall" | "chat" | "slot3" | "hub" | "me"
    var me by remember { mutableStateOf<MeUser?>(null) }
    var modules by remember { mutableStateOf(listOf<DynamicModule>()) }
    var pinnedModuleId by remember { mutableStateOf("pets") }
    var showLoginModal by remember { mutableStateOf(false) }
    var showPrivacySettingsSheet by remember { mutableStateOf(false) }

    // Cross-tab jump parameters (`No.1234` / `#tag` / notification routing)
    var wallTagArg by remember { mutableStateOf<String?>(null) }
    var wallMemeArg by remember { mutableStateOf<Long?>(null) }
    var webLinkModalUrl by remember { mutableStateOf<String?>(null) }
    var shellDetailMemeId by remember { mutableStateOf<Long?>(null) }
    var shellFullscreenImgUrl by remember { mutableStateOf<String?>(null) }
    var pendingChatJump by remember { mutableStateOf<ChatNotificationJump?>(null) }
    var chatJumpSeq by remember { mutableLongStateOf(0L) }

    // Notification center shared state
    var notifs by remember { mutableStateOf<List<NotifItem>>(emptyList()) }
    var notifUnreadCount by remember { mutableIntStateOf(0) }
    var notifsLoading by remember { mutableStateOf(false) }
    var inFlightNotifWrites by remember { mutableIntStateOf(0) }
    var notifReloadQueued by remember { mutableStateOf(false) }
    var notifRequestSeq by remember { mutableLongStateOf(0L) }
    var showNotifsSheet by remember { mutableStateOf(false) }

    val netStatus by api.rustNetManager.status.collectAsState()
    val chatState by chatController.uiState.collectAsState()
    val updateState by app.updateController.state.collectAsState()
    var dismissedUpdateTag by remember { mutableStateOf<String?>(null) }
    val totalChatUnread = remember(chatState.groups) { chatState.groups.sumOf { it.unread } }

    fun refreshNotifs(targetUserId: Long? = me?.id) {
        val uid = targetUserId ?: return
        if (inFlightNotifWrites > 0) {
            notifReloadQueued = true
            return
        }
        val seq = ++notifRequestSeq
        scope.launch {
            notifsLoading = true
            try {
                val payload = api.fetchNotifsPayload()
                if (seq == notifRequestSeq && me?.id == uid && inFlightNotifWrites == 0) {
                    notifs = payload.items
                    notifUnreadCount = payload.unread.coerceAtLeast(0)
                }
            } finally {
                if (seq == notifRequestSeq) {
                    notifsLoading = false
                }
            }
        }
    }

    fun markAllNotificationsRead() {
        notifRequestSeq++
        notifs = notifs.map { it.copy(unread = false) }
        notifUnreadCount = 0
        inFlightNotifWrites++
        scope.launch {
            try {
                val ok = api.markAllNotifsRead()
                if (!ok) notifReloadQueued = true
            } finally {
                inFlightNotifWrites = (inFlightNotifWrites - 1).coerceAtLeast(0)
                if (inFlightNotifWrites == 0 && notifReloadQueued) {
                    notifReloadQueued = false
                    refreshNotifs()
                }
            }
        }
    }

    fun markSingleNotificationRead(target: NotifItem) {
        if (!target.isUnread) return
        notifRequestSeq++
        var decremented = false
        notifs = notifs.map { item ->
            if (!decremented && item.matchesSameItem(target) && item.isUnread) {
                decremented = true
                item.copy(unread = false)
            } else {
                item
            }
        }
        if (decremented || notifUnreadCount > 0) {
            notifUnreadCount = (notifUnreadCount - 1).coerceAtLeast(0)
        }
        inFlightNotifWrites++
        scope.launch {
            try {
                val ok = api.markSingleNotifRead(target)
                if (!ok) notifReloadQueued = true
            } finally {
                inFlightNotifWrites = (inFlightNotifWrites - 1).coerceAtLeast(0)
                if (inFlightNotifWrites == 0 && notifReloadQueued) {
                    notifReloadQueued = false
                    refreshNotifs()
                }
            }
        }
    }

    fun bootstrapSession() {
        scope.launch {
            val savedPin = app.db.getKv("ui.pinned_slot3_id")
            if (!savedPin.isNullOrBlank()) pinnedModuleId = savedPin
            val currentUser = api.fetchMe()
            me = currentUser
            modules = api.scanDynamicTopbarModules()
            if (currentUser != null) {
                api.recordSiteVisit()
                chatController.refreshGroups()
                refreshNotifs(currentUser.id)
            } else {
                notifs = emptyList()
                notifUnreadCount = 0
            }
        }
    }

    LaunchedEffect(Unit) {
        bootstrapSession()
        app.updateController.ensureStartupCheck()
    }

    LaunchedEffect(netStatus.running, netStatus.winnerIp) {
        if (me == null && netStatus.running && api.cookieJar.hasSessionCookieFor()) {
            bootstrapSession()
        }
    }

    LaunchedEffect(me?.id) {
        val uid = me?.id
        app.seenTracker.bindUser(uid)
        if (uid == null) return@LaunchedEffect
        while (me?.id == uid) {
            delay(90_000L)
            refreshNotifs(uid)
        }
    }

    LaunchedEffect(showNotifsSheet) {
        if (showNotifsSheet && me != null) {
            refreshNotifs()
        }
    }

    DisposableEffect(chatController) {
        chatController.onGroupReadAcknowledged = { gid, upTo ->
            scope.launch {
                var seenCount = 0
                notifs = notifs.map { item ->
                    if (item.isUnread && item.isChatMessageNotif && item.gid == gid && (item.memeId ?: Long.MAX_VALUE) <= upTo) {
                        seenCount++
                        item.copy(unread = false)
                    } else {
                        item
                    }
                }
                if (seenCount > 0) {
                    notifRequestSeq++
                    notifUnreadCount = (notifUnreadCount - seenCount).coerceAtLeast(0)
                }
            }
        }
        onDispose {
            chatController.onGroupReadAcknowledged = null
        }
    }

    val pinnedModule = remember(modules, pinnedModuleId) {
        modules.firstOrNull { it.id == pinnedModuleId }
            ?: DynamicModule("pets", "鱼鱼谷", "/pets", iconKey = "pets", isBuiltInNative = true)
    }

    if (bossKeyActive) {
        BossDocScreen(onExit = { onSetBossKeyActive(false) })
        return
    }

    val effectiveDisguise = when {
        privacyConfig.disguiseMode != AppDisguiseMode.NONE -> privacyConfig.disguiseMode
        themeMode == AppThemeMode.FEISHU_WORK -> AppDisguiseMode.FEISHU
        themeMode == AppThemeMode.LINUX_DO -> AppDisguiseMode.LINUX_DO
        else -> AppDisguiseMode.NONE
    }

    when (effectiveDisguise) {
        AppDisguiseMode.FEISHU -> {
            FeishuDisguiseScreen(
                api = api,
                chatController = chatController,
                me = me,
                onRequireLogin = { showLoginModal = true },
                onToggleBlurAll = { enabled ->
                    onUpdatePrivacyConfig(privacyConfig.copy(blurAllEnabled = enabled))
                },
                onTriggerBossKey = { onSetBossKeyActive(true) },
                onOpenPrivacySettings = { showPrivacySettingsSheet = true },
                onExitDisguise = { onChangeDisguise(AppDisguiseMode.NONE) },
                onOpenUrl = { url -> webLinkModalUrl = url },
            )
        }

        AppDisguiseMode.LINUX_DO -> {
            LinuxDoDisguiseScreen(
                api = api,
                chatController = chatController,
                me = me,
                onRequireLogin = { showLoginModal = true },
                onToggleBlurAll = { enabled ->
                    onUpdatePrivacyConfig(privacyConfig.copy(blurAllEnabled = enabled))
                },
                onTriggerBossKey = { onSetBossKeyActive(true) },
                onOpenPrivacySettings = { showPrivacySettingsSheet = true },
                onExitDisguise = { onChangeDisguise(AppDisguiseMode.NONE) },
                onOpenUrl = { url -> webLinkModalUrl = url },
            )
        }

        AppDisguiseMode.NONE -> {
            DoubleBackBossHandler(
                enabled = privacyConfig.bossDoubleBack &&
                    !showNotifsSheet &&
                    shellDetailMemeId == null &&
                    shellFullscreenImgUrl == null &&
                    !showLoginModal &&
                    webLinkModalUrl == null &&
                    !showPrivacySettingsSheet,
                onTriggerBoss = { onSetBossKeyActive(true) },
            )

            LaunchedEffect(activeTab) {
                if (activeTab != "chat") {
                    chatController.isChatForegroundAtBottom = false
                }
            }

            val isImeVisible = WindowInsets.isImeVisible

            val showUpdateBanner = (
                updateState.status == UpdateStatus.AVAILABLE ||
                    updateState.status == UpdateStatus.DOWNLOADING ||
                    updateState.status == UpdateStatus.READY_TO_INSTALL
                ) && updateState.latestRelease?.tagName != dismissedUpdateTag

            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .background(palette.bg),
                topBar = {
                    Surface(
                        color = palette.surface,
                        modifier = Modifier.statusBarsPadding(),
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 5.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = palette.appDisplayName,
                                        color = palette.primary,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    BadgePill(
                                        text = when {
                                            !netStatus.running -> "CF优化初始化"
                                            netStatus.latencyMs != null -> "${netStatus.latencyMs}ms${if (netStatus.echAccepted) " ECH" else ""}"
                                            netStatus.phase == "racing" -> "优选测速…"
                                            else -> "CF :${netStatus.port}"
                                        },
                                        containerColor = palette.surface2,
                                        contentColor = palette.muted,
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (me == null) {
                                        BadgePill(
                                            text = "点此登录",
                                            containerColor = palette.primary,
                                            contentColor = palette.onPrimary,
                                            modifier = Modifier.clickable { showLoginModal = true },
                                        )
                                    } else {
                                        Text(
                                            text = me!!.displayName,
                                            color = palette.muted,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        IconButton(
                                            onClick = { showNotifsSheet = true },
                                            modifier = Modifier
                                                .width(42.dp)
                                                .height(34.dp),
                                        ) {
                                            BadgedBox(
                                                badge = {
                                                    if (notifUnreadCount > 0) {
                                                        Badge {
                                                            Text(
                                                                text = if (notifUnreadCount > 99) "99+" else "$notifUnreadCount",
                                                                fontSize = 9.sp,
                                                            )
                                                        }
                                                    }
                                                },
                                            ) {
                                                Box(modifier = Modifier.padding(top = 3.dp, end = 6.dp)) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.NotificationsNone,
                                                        contentDescription = if (notifUnreadCount > 0) {
                                                            "互动与系统提醒（$notifUnreadCount 条未读）"
                                                        } else {
                                                            "互动与系统提醒"
                                                        },
                                                        tint = if (notifUnreadCount > 0) palette.text else palette.muted,
                                                        modifier = Modifier.size(20.dp),
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            if (showUpdateBanner) {
                                val rel = updateState.latestRelease
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(palette.primary.copy(alpha = 0.08f))
                                        .clickable { activeTab = "me" }
                                        .padding(horizontal = 14.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Icon(
                                            Icons.Rounded.SystemUpdate,
                                            contentDescription = null,
                                            tint = palette.primary,
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = when (updateState.status) {
                                                UpdateStatus.DOWNLOADING -> "正在下载 v${rel?.versionName.orEmpty()} (${updateState.downloadProgressPercent}%)"
                                                UpdateStatus.READY_TO_INSTALL -> "新版本 v${rel?.versionName.orEmpty()} 已下载，点此安装"
                                                else -> "发现新版本 v${rel?.versionName.orEmpty()}（点击查看说明）"
                                            },
                                            color = palette.text,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    when (updateState.status) {
                                        UpdateStatus.AVAILABLE -> BadgePill(
                                            text = "立即更新",
                                            containerColor = palette.primary,
                                            contentColor = palette.onPrimary,
                                            modifier = Modifier.clickable { app.updateController.downloadAndInstall() },
                                        )
                                        UpdateStatus.READY_TO_INSTALL -> BadgePill(
                                            text = "安装",
                                            containerColor = palette.primary,
                                            contentColor = palette.onPrimary,
                                            modifier = Modifier.clickable { app.updateController.installReadyApk() },
                                        )
                                        else -> BadgePill(
                                            text = "${updateState.downloadProgressPercent}%",
                                            containerColor = palette.surface2,
                                            contentColor = palette.primary,
                                        )
                                    }
                                    IconButton(
                                        onClick = { dismissedUpdateTag = rel?.tagName },
                                        modifier = Modifier.size(24.dp),
                                    ) {
                                        Icon(
                                            Icons.Rounded.Close,
                                            contentDescription = "忽略更新提示",
                                            tint = palette.muted,
                                            modifier = Modifier.size(14.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                bottomBar = {
                    if (!isImeVisible) {
                        NavigationBar(
                            containerColor = palette.surface,
                            modifier = Modifier.navigationBarsPadding(),
                        ) {
                            NavigationBarItem(
                                selected = activeTab == "wall",
                                onClick = { activeTab = "wall" },
                                icon = { Icon(Icons.Rounded.Collections, contentDescription = "图廊") },
                                label = { Text("图廊", fontSize = 11.sp) },
                                colors = navItemColors(palette),
                            )
                            NavigationBarItem(
                                selected = activeTab == "chat",
                                onClick = { activeTab = "chat" },
                                icon = {
                                    BadgedBox(badge = {
                                        if (totalChatUnread > 0) {
                                            Badge { Text(if (totalChatUnread > 99) "99+" else "$totalChatUnread") }
                                        }
                                    }) {
                                        Icon(Icons.AutoMirrored.Rounded.Chat, contentDescription = "群聊")
                                    }
                                },
                                label = { Text("群聊", fontSize = 11.sp) },
                                colors = navItemColors(palette),
                            )
                            NavigationBarItem(
                                selected = activeTab == "slot3",
                                onClick = { activeTab = "slot3" },
                                icon = { Icon(Icons.Rounded.AutoAwesome, contentDescription = pinnedModule.label) },
                                label = { Text(pinnedModule.label.take(4), fontSize = 11.sp) },
                                colors = navItemColors(palette),
                            )
                            NavigationBarItem(
                                selected = activeTab == "hub",
                                onClick = { activeTab = "hub" },
                                icon = { Icon(Icons.Rounded.Apps, contentDescription = "百宝箱") },
                                label = { Text("百宝箱", fontSize = 11.sp) },
                                colors = navItemColors(palette),
                            )
                            NavigationBarItem(
                                selected = activeTab == "me",
                                onClick = { activeTab = "me" },
                                icon = { Icon(Icons.Rounded.Person, contentDescription = "我的") },
                                label = { Text("我的", fontSize = 11.sp) },
                                colors = navItemColors(palette),
                            )
                        }
                    }
                },
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .consumeWindowInsets(innerPadding),
                ) {
                    when (activeTab) {
                        "wall" -> WallScreen(
                            api = api,
                            me = me,
                            initialTag = wallTagArg,
                            initialDetailMemeId = wallMemeArg,
                            onConsumeInitialArgs = {
                                wallTagArg = null
                                wallMemeArg = null
                            },
                            onRequireLogin = { showLoginModal = true },
                            onToggleBlurAll = { enabled ->
                                onUpdatePrivacyConfig(privacyConfig.copy(blurAllEnabled = enabled))
                            },
                        )

                        "chat" -> ChatScreen(
                            api = api,
                            controller = chatController,
                            me = me,
                            initialJump = pendingChatJump,
                            onConsumeInitialJump = { pendingChatJump = null },
                            onNavigateToMeme = { id ->
                                wallMemeArg = id
                                activeTab = "wall"
                            },
                            onNavigateToTag = { tag ->
                                wallTagArg = tag
                                activeTab = "wall"
                            },
                            onOpenUrl = { url -> webLinkModalUrl = url },
                            onRequireLogin = { showLoginModal = true },
                        )

                        "slot3" -> {
                            if (pinnedModule.id == "pets") {
                                PetsScreen(
                                    api = api,
                                    me = me,
                                    onRequireLogin = { showLoginModal = true },
                                )
                            } else {
                                ShelllessWebScreen(
                                    route = pinnedModule.route,
                                    title = pinnedModule.label,
                                    cookieJar = api.cookieJar,
                                    assetCache = api.webAssetCache,
                                )
                            }
                        }

                        "hub" -> HubScreen(
                            api = api,
                            db = app.db,
                            me = me,
                            modules = modules,
                            pinnedModuleId = pinnedModule.id,
                            onPinModule = { mod ->
                                pinnedModuleId = mod.id
                                scope.launch { app.db.putKv("ui.pinned_slot3_id", mod.id) }
                            },
                            onRefreshModules = {
                                scope.launch { modules = api.scanDynamicTopbarModules() }
                            },
                            onNavigateRootTab = { tab -> activeTab = tab },
                        )

                        "me" -> MeScreen(
                            api = api,
                            updateController = app.updateController,
                            me = me,
                            themeMode = themeMode,
                            privacyConfig = privacyConfig,
                            privacyShieldEnabled = privacyShieldEnabled,
                            activeIconAlias = activeIconAlias,
                            notifUnreadCount = notifUnreadCount,
                            notifTotalCount = notifs.size,
                            onOpenNotifications = { showNotifsSheet = true },
                            onChangeTheme = onChangeTheme,
                            onChangeDisguise = onChangeDisguise,
                            onUpdatePrivacyConfig = onUpdatePrivacyConfig,
                            onTogglePrivacyShield = onTogglePrivacyShield,
                            onChangeIconAlias = onChangeIconAlias,
                            onRequireLogin = { showLoginModal = true },
                            onLoggedOut = {
                                me = null
                                notifs = emptyList()
                                notifUnreadCount = 0
                                showNotifsSheet = false
                            },
                        )
                    }
                }
            }
        }
    }

    if (showPrivacySettingsSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showPrivacySettingsSheet = false },
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
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("伪装与隐私设置", color = palette.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { showPrivacySettingsSheet = false }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = "关闭", tint = palette.muted)
                    }
                }
                HorizontalDivider(color = palette.line)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                ) {
                    PrivacyStealthSettingsContent(
                        themeMode = themeMode,
                        privacyConfig = privacyConfig,
                        privacyShieldEnabled = privacyShieldEnabled,
                        activeIconAlias = activeIconAlias,
                        onChangeTheme = onChangeTheme,
                        onChangeDisguise = { next ->
                            onChangeDisguise(next)
                            showPrivacySettingsSheet = false
                        },
                        onUpdatePrivacyConfig = onUpdatePrivacyConfig,
                        onTogglePrivacyShield = onTogglePrivacyShield,
                        onChangeIconAlias = onChangeIconAlias,
                    )
                }
            }
        }
    }

    if (showNotifsSheet && me != null) {
        NotificationsSheet(
            api = api,
            items = notifs,
            unreadCount = notifUnreadCount,
            loading = notifsLoading,
            notifPrefs = me?.notifPrefs.orEmpty(),
            onRefresh = { refreshNotifs() },
            onMarkAllRead = { markAllNotificationsRead() },
            onMarkItemRead = { item -> markSingleNotificationRead(item) },
            onUpdatePref = { key, enabled ->
                val prevPrefs = me?.notifPrefs.orEmpty()
                me = me?.copy(notifPrefs = prevPrefs + (key to enabled))
                scope.launch {
                    val updated = api.updateNotifPref(key, enabled)
                    if (updated != null) {
                        me = me?.copy(notifPrefs = updated)
                        refreshNotifs()
                    } else {
                        me = me?.copy(notifPrefs = prevPrefs)
                    }
                }
            },
            onOpenMeme = { memeId -> shellDetailMemeId = memeId },
            onOpenChat = { gid, msgId ->
                pendingChatJump = ChatNotificationJump(
                    groupId = gid,
                    messageId = msgId,
                    triggerSeq = ++chatJumpSeq,
                )
                activeTab = "chat"
            },
            onNavigateHub = { activeTab = "hub" },
            onOpenUrl = { url -> webLinkModalUrl = url },
            onDismiss = { showNotifsSheet = false },
        )
    }

    shellDetailMemeId?.let { memeId ->
        MemeDetailModal(
            memeId = memeId,
            api = api,
            me = me,
            onRequireLogin = { showLoginModal = true },
            onDismiss = { shellDetailMemeId = null },
            onOpenImageFullscreen = { url -> shellFullscreenImgUrl = url },
            onTagClick = { tag ->
                shellDetailMemeId = null
                wallTagArg = tag
                activeTab = "wall"
            },
            onStateUpdated = {},
        )
    }

    shellFullscreenImgUrl?.let { url ->
        FullscreenMediaOverlay(
            imageUrl = url,
            api = api,
            onDismiss = { shellFullscreenImgUrl = null },
        )
    }

    if (showLoginModal) {
        Dialog(
            onDismissRequest = { showLoginModal = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
        ) {
            LoginWebSheet(
                api = api,
                onAuthenticated = {
                    showLoginModal = false
                    bootstrapSession()
                },
                onDismiss = { showLoginModal = false },
            )
        }
    }

    webLinkModalUrl?.let { url ->
        Dialog(
            onDismissRequest = { webLinkModalUrl = null },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
        ) {
            ShelllessWebScreen(
                route = url,
                title = "网页浏览",
                cookieJar = api.cookieJar,
                assetCache = api.webAssetCache,
            )
        }
    }
}

@Composable
private fun navItemColors(palette: com.jinhuan.forhappy.ui.theme.ForHappyPalette) =
    NavigationBarItemDefaults.colors(
        selectedIconColor = palette.primary,
        selectedTextColor = palette.primary,
        indicatorColor = palette.primary.copy(alpha = 0.14f),
        unselectedIconColor = palette.muted,
        unselectedTextColor = palette.muted,
    )
