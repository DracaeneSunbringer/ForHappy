package com.jinhuan.forhappy.ui.navigation

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.jinhuan.forhappy.ForHappyApp
import com.jinhuan.forhappy.core.clock.ServerClock
import com.jinhuan.forhappy.core.compose.ComposeGenre
import com.jinhuan.forhappy.core.compose.ComposeLaunchRequest
import com.jinhuan.forhappy.core.dynamic.TopbarModuleScanner
import com.jinhuan.forhappy.core.model.AccentColorConfig
import com.jinhuan.forhappy.core.model.AppDisguiseMode
import com.jinhuan.forhappy.core.model.AppIconAlias
import com.jinhuan.forhappy.core.model.AppThemeMode
import com.jinhuan.forhappy.core.model.AppUiStyle
import com.jinhuan.forhappy.core.model.ChatOpenSessionRequest
import com.jinhuan.forhappy.core.model.DynamicModule
import com.jinhuan.forhappy.core.model.ForHappyJson
import com.jinhuan.forhappy.core.model.MeUser
import com.jinhuan.forhappy.core.model.MoyuConfig
import com.jinhuan.forhappy.core.model.NotifItem
import com.jinhuan.forhappy.core.model.PrivacyStealthConfig
import com.jinhuan.forhappy.core.model.SiteNotice
import com.jinhuan.forhappy.core.model.TodayStats
import com.jinhuan.forhappy.core.moyu.MoyuCalculator
import com.jinhuan.forhappy.core.navigation.DeepLinkRouter
import com.jinhuan.forhappy.core.navigation.DeepLinkTarget
import com.jinhuan.forhappy.core.navigation.SharedIncomingPayload
import com.jinhuan.forhappy.core.update.UpdateStatus
import com.jinhuan.forhappy.ui.chat.ChatNotificationJump
import com.jinhuan.forhappy.ui.chat.ChatScreen
import com.jinhuan.forhappy.ui.components.BadgePill
import com.jinhuan.forhappy.ui.components.LocalCosmeticHttpClient
import com.jinhuan.forhappy.ui.compose.ComposeWorkbench
import com.jinhuan.forhappy.ui.compose.SystemShareTargetSheet
import com.jinhuan.forhappy.ui.disguise.BossDocScreen
import com.jinhuan.forhappy.ui.disguise.DoubleBackBossHandler
import com.jinhuan.forhappy.ui.disguise.FeishuDisguiseScreen
import com.jinhuan.forhappy.ui.disguise.LinuxDoDisguiseScreen
import com.jinhuan.forhappy.ui.hub.HubScreen
import com.jinhuan.forhappy.ui.hub.KV_MOYU_FG_DAY
import com.jinhuan.forhappy.ui.hub.KV_MOYU_FG_SEC
import com.jinhuan.forhappy.ui.hub.loadMoyuConfig
import com.jinhuan.forhappy.ui.hub.loadPinnedHubIds
import com.jinhuan.forhappy.ui.hub.resolveHubCatalog
import com.jinhuan.forhappy.ui.me.MeScreen
import com.jinhuan.forhappy.ui.me.PrivacyStealthSettingsContent
import com.jinhuan.forhappy.ui.notifications.NotificationsSheet
import com.jinhuan.forhappy.ui.theme.ForHappyDimens
import com.jinhuan.forhappy.ui.theme.ForHappyPalette
import com.jinhuan.forhappy.ui.theme.LocalForHappyPalette
import com.jinhuan.forhappy.ui.theme.TABULAR_FONT_FEATURE
import com.jinhuan.forhappy.ui.wall.ConfigureFullscreenDialogWindow
import com.jinhuan.forhappy.ui.wall.DialogBackOwnerScope
import com.jinhuan.forhappy.ui.wall.FullscreenDialogProperties
import com.jinhuan.forhappy.ui.wall.FullscreenMediaOverlay
import com.jinhuan.forhappy.ui.wall.MemeDetailModal
import com.jinhuan.forhappy.ui.wall.WallScreen
import com.jinhuan.forhappy.ui.web.LoginWebSheet
import com.jinhuan.forhappy.ui.web.ShelllessWebScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import java.time.Instant
import java.time.ZoneId

/**
 * 4-Tab main navigation shell for ForHappy v0.2.0 (`docs/redesign/01-information-architecture.md`):
 * 1. `图廊` ("wall")
 * 2. `消息` ("messages") — unified group chat + 1:1 DMs
 * 3. `发现` ("discover") — Pets, Road, Moyu, Novels & dynamic topbar extensions
 * 4. `我的` ("me") — Profile, Wallet, Disguise & Settings
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ForHappyShell(
    app: ForHappyApp,
    themeMode: AppThemeMode,
    uiStyle: AppUiStyle = AppUiStyle.MODERN,
    accentConfig: AccentColorConfig = AccentColorConfig(),
    privacyConfig: PrivacyStealthConfig,
    bossKeyActive: Boolean,
    privacyShieldEnabled: Boolean,
    activeIconAlias: AppIconAlias,
    incomingIntents: Flow<Intent> = emptyFlow(),
    onChangeTheme: (AppThemeMode) -> Unit,
    onChangeUiStyle: (AppUiStyle) -> Unit = {},
    onChangeAccentConfig: (AccentColorConfig) -> Unit = {},
    onChangeDisguise: (AppDisguiseMode) -> Unit,
    onUpdatePrivacyConfig: (PrivacyStealthConfig) -> Unit,
    onSetBossKeyActive: (Boolean) -> Unit,
    onTogglePrivacyShield: (Boolean) -> Unit,
    onChangeIconAlias: (AppIconAlias) -> Unit,
) {
    val palette = LocalForHappyPalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val api = app.apiClient
    val chatController = app.chatController

    var activeTab by remember { mutableStateOf("wall") } // "wall" | "messages" | "discover" | "me"
    var me by remember { mutableStateOf<MeUser?>(null) }
    var modules by remember { mutableStateOf(TopbarModuleScanner.DEFAULT_BUILT_IN_MODULES) }
    var showLoginModal by remember { mutableStateOf(false) }
    var showPrivacySettingsSheet by remember { mutableStateOf(false) }

    // Cross-tab & deep-link jump parameters
    var wallTagArg by remember { mutableStateOf<String?>(null) }
    var wallAuthorArg by remember { mutableStateOf<String?>(null) }
    var wallSeriesArg by remember { mutableStateOf<String?>(null) }
    var wallMemeArg by remember { mutableStateOf<Long?>(null) }
    var openBrowseSettingsSignal by remember { mutableIntStateOf(0) }
    var openSeriesIndexSignal by remember { mutableIntStateOf(0) }
    var wallScrollToTopSignal by remember { mutableIntStateOf(0) }
    var webLinkModalUrl by remember { mutableStateOf<String?>(null) }
    var shellDetailMemeId by remember { mutableStateOf<Long?>(null) }
    var shellFullscreenImgUrl by remember { mutableStateOf<String?>(null) }
    var pendingChatJump by remember { mutableStateOf<ChatNotificationJump?>(null) }
    var chatJumpSeq by remember { mutableLongStateOf(0L) }
    var chatOpenRequest by remember { mutableStateOf<ChatOpenSessionRequest?>(null) }
    var unreadCycleCursor by remember { mutableIntStateOf(0) }
    var discoverInitialRoute by remember { mutableStateOf<String?>(null) }
    var deferredDeepLink by remember { mutableStateOf<DeepLinkTarget?>(null) }
    var deferredSharePayload by remember { mutableStateOf<SharedIncomingPayload?>(null) }
    var activeSharePayload by remember { mutableStateOf<SharedIncomingPayload?>(null) }
    var activeComposeRequest by remember { mutableStateOf<ComposeLaunchRequest?>(null) }
    var wallRefreshSignal by remember { mutableIntStateOf(0) }

    // Notification center & Phase 5 Moyu / Hub / Settings shared state
    var notifs by remember { mutableStateOf<List<NotifItem>>(emptyList()) }
    var notifUnreadCount by remember { mutableIntStateOf(0) }
    var notifsLoading by remember { mutableStateOf(false) }
    var inFlightNotifWrites by remember { mutableIntStateOf(0) }
    var notifReloadQueued by remember { mutableStateOf(false) }
    var notifRequestSeq by remember { mutableLongStateOf(0L) }
    var showNotifsSheet by remember { mutableStateOf(false) }
    var openNotifPrefsDirectly by remember { mutableStateOf(false) }
    var siteNotices by remember { mutableStateOf<List<SiteNotice>>(emptyList()) }
    var dismissedNoticeIds by remember { mutableStateOf<Set<Long>>(emptySet()) }

    var moyuConfig by remember { mutableStateOf(MoyuConfig()) }
    var todayStats by remember { mutableStateOf<TodayStats?>(null) }
    var moyuForegroundSec by remember { mutableLongStateOf(0L) }
    var nowTickSec by remember { mutableLongStateOf(ServerClock.serverNowMs() / 1000L) }
    var pinnedHubIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var showDiscoverQuickRing by remember { mutableStateOf(false) }
    var forceWallUnseenSignal by remember { mutableIntStateOf(0) }
    var pendingSettingsSubpage by remember { mutableStateOf<String?>(null) }

    val netStatus by api.rustNetManager.status.collectAsState()
    val chatState by chatController.uiState.collectAsState()
    val updateState by app.updateController.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var shownUpdateTag by remember { mutableStateOf<String?>(null) }
    var startupGraceElapsed by remember { mutableStateOf(false) }

    val effectiveDisguise = when {
        privacyConfig.disguiseMode != AppDisguiseMode.NONE -> privacyConfig.disguiseMode
        themeMode == AppThemeMode.FEISHU_WORK -> AppDisguiseMode.FEISHU
        themeMode == AppThemeMode.LINUX_DO -> AppDisguiseMode.LINUX_DO
        else -> AppDisguiseMode.NONE
    }

    val currentBossActive by rememberUpdatedState(bossKeyActive)
    val currentDisguise by rememberUpdatedState(effectiveDisguise)

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
            launch { runCatching { api.fetchCommunitySkinsCatalog() } }
            val currentUser = api.fetchMe()
            me = currentUser
            val scanned = api.scanDynamicTopbarModules()
            modules = scanned
            pinnedHubIds = loadPinnedHubIds(api, resolveHubCatalog(scanned))
            siteNotices = api.fetchNotices()
            if (currentUser != null) {
                api.recordSiteVisit()
                chatController.refreshGroups()
                refreshNotifs(currentUser.id)
                todayStats = api.fetchTodayStats()
            } else {
                notifs = emptyList()
                notifUnreadCount = 0
                todayStats = null
            }
        }
    }

    fun applyDeepLinkTarget(target: DeepLinkTarget) {
        if (currentDisguise != AppDisguiseMode.NONE) {
            when (target) {
                is DeepLinkTarget.DiscoverSubApp,
                DeepLinkTarget.Road,
                DeepLinkTarget.SeriesIndex,
                is DeepLinkTarget.SeriesFeed,
                -> return
                is DeepLinkTarget.MemeDetail -> {
                    shellDetailMemeId = target.memeId
                    return
                }
                is DeepLinkTarget.GroupChat -> {
                    chatController.setActiveGroup(target.groupId)
                    return
                }
                else -> return
            }
        }

        when (target) {
            DeepLinkTarget.WallRoot -> {
                wallTagArg = null
                wallAuthorArg = null
                wallSeriesArg = null
                wallMemeArg = null
                activeTab = "wall"
            }
            is DeepLinkTarget.MemeDetail -> {
                wallMemeArg = target.memeId
                activeTab = "wall"
            }
            is DeepLinkTarget.TagFeed -> {
                wallAuthorArg = null
                wallSeriesArg = null
                wallTagArg = target.tag
                activeTab = "wall"
            }
            is DeepLinkTarget.AuthorFeed -> {
                wallTagArg = null
                wallSeriesArg = null
                wallAuthorArg = target.author
                activeTab = "wall"
            }
            DeepLinkTarget.SeriesIndex -> {
                openSeriesIndexSignal++
                activeTab = "wall"
            }
            is DeepLinkTarget.SeriesFeed -> {
                wallTagArg = null
                wallAuthorArg = null
                wallSeriesArg = target.series
                activeTab = "wall"
            }
            is DeepLinkTarget.MessagesRoot -> {
                val curChatState = chatController.uiState.value
                chatOpenRequest = if (curChatState.shouldBypassSessionList()) {
                    ChatOpenSessionRequest(
                        mode = "group",
                        groupId = curChatState.activeGroupId,
                        requestId = System.nanoTime(),
                    )
                } else {
                    ChatOpenSessionRequest(
                        mode = "list",
                        filter = target.filter,
                        requestId = System.nanoTime(),
                    )
                }
                activeTab = "messages"
            }
            is DeepLinkTarget.GroupChat -> {
                if (target.aroundMessageId != null) {
                    pendingChatJump = ChatNotificationJump(
                        groupId = target.groupId,
                        messageId = target.aroundMessageId,
                        triggerSeq = ++chatJumpSeq,
                    )
                } else {
                    chatOpenRequest = ChatOpenSessionRequest(
                        mode = "group",
                        groupId = target.groupId,
                        requestId = System.nanoTime(),
                    )
                }
                activeTab = "messages"
            }
            is DeepLinkTarget.DirectMessage -> {
                if (!chatController.uiState.value.dmEnabled) return
                chatOpenRequest = ChatOpenSessionRequest(
                    mode = "dm",
                    dmThreadId = if (!target.isNewPeer) target.threadOrUserId else null,
                    requestId = System.nanoTime(),
                )
                activeTab = "messages"
            }
            is DeepLinkTarget.DiscoverSubApp -> {
                discoverInitialRoute = target.route
                activeTab = "discover"
            }
            DeepLinkTarget.Road -> {
                discoverInitialRoute = "/road"
                activeTab = "discover"
            }
            DeepLinkTarget.Me -> {
                activeTab = "me"
            }
        }
    }

    LaunchedEffect(Unit) {
        moyuConfig = loadMoyuConfig(api)
        val rawDismissed = api.getPref("ui.dismissed_notice_ids")
        if (!rawDismissed.isNullOrBlank()) {
            dismissedNoticeIds = runCatching {
                ForHappyJson.decodeFromString(ListSerializer(Long.serializer()), rawDismissed).toSet()
            }.getOrDefault(emptySet())
        }
        val todayIso = Instant.ofEpochSecond(ServerClock.serverNowMs() / 1000L)
            .atZone(ZoneId.of("Asia/Shanghai")).toLocalDate().toString()
        val savedDay = api.getPref(KV_MOYU_FG_DAY)
        moyuForegroundSec = if (savedDay == todayIso) {
            api.getPref(KV_MOYU_FG_SEC)?.toLongOrNull()?.coerceAtLeast(0L) ?: 0L
        } else {
            api.putPref(KV_MOYU_FG_DAY, todayIso)
            api.putPref(KV_MOYU_FG_SEC, "0")
            0L
        }
        bootstrapSession()
        app.updateController.ensureStartupCheck()
        delay(3_000L)
        startupGraceElapsed = true
    }

    val shellLifecycleOwner = LocalLifecycleOwner.current
    var isShellResumed by remember { mutableStateOf(true) }
    DisposableEffect(shellLifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> isShellResumed = true
                Lifecycle.Event.ON_PAUSE -> isShellResumed = false
                else -> Unit
            }
        }
        shellLifecycleOwner.lifecycle.addObserver(observer)
        onDispose { shellLifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(isShellResumed, currentBossActive) {
        var trackedDay = Instant.ofEpochSecond(ServerClock.serverNowMs() / 1000L)
            .atZone(ZoneId.of("Asia/Shanghai")).toLocalDate().toString()
        while (isShellResumed && !currentBossActive) {
            delay(1000L)
            val nowSec = ServerClock.serverNowMs() / 1000L
            nowTickSec = nowSec
            val curDay = Instant.ofEpochSecond(nowSec)
                .atZone(ZoneId.of("Asia/Shanghai")).toLocalDate().toString()
            if (curDay != trackedDay) {
                trackedDay = curDay
                moyuForegroundSec = 0L
                api.putPref(KV_MOYU_FG_DAY, curDay)
                api.putPref(KV_MOYU_FG_SEC, "0")
            } else {
                moyuForegroundSec++
                if (moyuForegroundSec % 10L == 0L) {
                    api.putPref(KV_MOYU_FG_DAY, curDay)
                    api.putPref(KV_MOYU_FG_SEC, moyuForegroundSec.toString())
                }
            }
        }
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

    LaunchedEffect(bossKeyActive) {
        if (bossKeyActive) {
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
            showPrivacySettingsSheet = false
            showNotifsSheet = false
            showDiscoverQuickRing = false
        }
    }

    LaunchedEffect(incomingIntents) {
        incomingIntents.collect { intent ->
            val sharePayload = DeepLinkRouter.parseShareIntent(intent)
            if (sharePayload != null) {
                if (currentBossActive) {
                    deferredSharePayload = sharePayload
                } else {
                    activeSharePayload = sharePayload
                }
                return@collect
            }
            val target = DeepLinkRouter.parseDeepLinkUri(intent.dataString) ?: return@collect
            if (currentBossActive) {
                deferredDeepLink = target
            } else {
                applyDeepLinkTarget(target)
            }
        }
    }

    LaunchedEffect(bossKeyActive, deferredDeepLink, deferredSharePayload) {
        val pending = deferredDeepLink
        val pendingShare = deferredSharePayload
        if (!bossKeyActive) {
            if (pendingShare != null) {
                deferredSharePayload = null
                activeSharePayload = pendingShare
            } else if (pending != null) {
                deferredDeepLink = null
                applyDeepLinkTarget(pending)
            }
        }
    }

    LaunchedEffect(updateState.status, updateState.latestRelease?.tagName, effectiveDisguise, bossKeyActive) {
        if (effectiveDisguise != AppDisguiseMode.NONE || bossKeyActive) return@LaunchedEffect
        val rel = updateState.latestRelease ?: return@LaunchedEffect
        val shouldPrompt = updateState.status == UpdateStatus.AVAILABLE ||
            updateState.status == UpdateStatus.READY_TO_INSTALL
        if (!shouldPrompt || rel.tagName == shownUpdateTag) return@LaunchedEffect
        shownUpdateTag = rel.tagName
        val message = if (updateState.status == UpdateStatus.READY_TO_INSTALL) {
            "新版本 v${rel.versionName} 已就绪"
        } else {
            "发现新版本 v${rel.versionName}"
        }
        val result = withTimeoutOrNull(4_000L) {
            snackbarHostState.showSnackbar(
                message = message,
                actionLabel = "查看",
                duration = SnackbarDuration.Short,
            )
        }
        if (result == SnackbarResult.ActionPerformed) {
            pendingSettingsSubpage = "about"
            activeTab = "me"
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

    val clearLoggedOutState: () -> Unit = {
        me = null
        notifs = emptyList()
        notifUnreadCount = 0
        showNotifsSheet = false
        todayStats = null
    }

    CompositionLocalProvider(LocalCosmeticHttpClient provides api.okHttpClient) {
    Box(modifier = Modifier.fillMaxSize()) {
        BackHandler(
            enabled = !bossKeyActive &&
                effectiveDisguise == AppDisguiseMode.NONE &&
                activeTab != "wall" &&
                !showNotifsSheet &&
                shellDetailMemeId == null &&
                shellFullscreenImgUrl == null &&
                !showLoginModal &&
                webLinkModalUrl == null &&
                !showPrivacySettingsSheet &&
                !showDiscoverQuickRing &&
                activeComposeRequest == null &&
                activeSharePayload == null,
        ) {
            activeTab = "wall"
        }

        DoubleBackBossHandler(
            enabled = !bossKeyActive &&
                (effectiveDisguise != AppDisguiseMode.NONE || activeTab == "wall") &&
                !WindowInsets.isImeVisible &&
                !showNotifsSheet &&
                shellDetailMemeId == null &&
                shellFullscreenImgUrl == null &&
                !showLoginModal &&
                webLinkModalUrl == null &&
                !showPrivacySettingsSheet &&
                !showDiscoverQuickRing &&
                activeComposeRequest == null &&
                activeSharePayload == null,
            onTriggerBoss = {
                if (privacyConfig.bossDoubleBack) onSetBossKeyActive(true)
                else (context as? Activity)?.moveTaskToBack(true)
            },
            onSingleBackTimeout = {
                (context as? Activity)?.moveTaskToBack(true)
            },
        )

        when (effectiveDisguise) {
            AppDisguiseMode.FEISHU -> {
                FeishuDisguiseScreen(
                    api = api,
                    chatController = chatController,
                    me = me,
                    themeMode = themeMode,
                    privacyConfig = privacyConfig,
                    privacyShieldEnabled = privacyShieldEnabled,
                    activeIconAlias = activeIconAlias,
                    onRequireLogin = { showLoginModal = true },
                    onToggleBlurAll = { enabled ->
                        onUpdatePrivacyConfig(privacyConfig.copy(blurAllEnabled = enabled))
                    },
                    onTriggerBossKey = { onSetBossKeyActive(true) },
                    onOpenPrivacySettings = { showPrivacySettingsSheet = true },
                    onExitDisguise = { onChangeDisguise(AppDisguiseMode.NONE) },
                    onChangeDisguise = onChangeDisguise,
                    onChangeTheme = onChangeTheme,
                    onUpdatePrivacyConfig = onUpdatePrivacyConfig,
                    onTogglePrivacyShield = onTogglePrivacyShield,
                    onChangeIconAlias = onChangeIconAlias,
                    onLoggedOut = clearLoggedOutState,
                    onOpenUrl = { url -> webLinkModalUrl = url },
                )
            }

            AppDisguiseMode.LINUX_DO -> {
                LinuxDoDisguiseScreen(
                    api = api,
                    chatController = chatController,
                    me = me,
                    themeMode = themeMode,
                    privacyConfig = privacyConfig,
                    privacyShieldEnabled = privacyShieldEnabled,
                    activeIconAlias = activeIconAlias,
                    onRequireLogin = { showLoginModal = true },
                    onToggleBlurAll = { enabled ->
                        onUpdatePrivacyConfig(privacyConfig.copy(blurAllEnabled = enabled))
                    },
                    onTriggerBossKey = { onSetBossKeyActive(true) },
                    onOpenPrivacySettings = { showPrivacySettingsSheet = true },
                    onExitDisguise = { onChangeDisguise(AppDisguiseMode.NONE) },
                    onChangeDisguise = onChangeDisguise,
                    onChangeTheme = onChangeTheme,
                    onUpdatePrivacyConfig = onUpdatePrivacyConfig,
                    onTogglePrivacyShield = onTogglePrivacyShield,
                    onChangeIconAlias = onChangeIconAlias,
                    onLoggedOut = clearLoggedOutState,
                    onOpenUrl = { url -> webLinkModalUrl = url },
                )
            }

            AppDisguiseMode.NONE -> {
            var wallScrollDockHidden by remember { mutableStateOf(false) }
            var chatDockHidden by remember { mutableStateOf(false) }

            LaunchedEffect(activeTab) {
                if (activeTab != "messages") {
                    chatController.isChatForegroundAtBottom = false
                    chatDockHidden = false
                }
                if (activeTab != "wall") {
                    wallScrollDockHidden = false
                }
            }

            val isImeVisible = WindowInsets.isImeVisible
            val showNetworkStrip = (!netStatus.running && startupGraceElapsed) || !netStatus.lastError.isNullOrBlank()
            val hasNewDiscoverModule = remember(modules) {
                modules.any { it.isNewFromTopbar && !TopbarModuleScanner.isRetiredRoute(it.route) }
            }
            val hasUpdateDot = effectiveDisguise == AppDisguiseMode.NONE &&
                !bossKeyActive &&
                (updateState.status == UpdateStatus.AVAILABLE ||
                    updateState.status == UpdateStatus.READY_TO_INSTALL)

            val wallMoyuPill = remember(moyuConfig, effectiveDisguise, bossKeyActive, nowTickSec) {
                if (moyuConfig.showInWallHeader &&
                    effectiveDisguise == AppDisguiseMode.NONE &&
                    !bossKeyActive &&
                    moyuConfig.monthlySalary > 0.0
                ) {
                    MoyuCalculator.compute(nowTickSec, moyuConfig).formattedEarnedToday
                } else {
                    null
                }
            }

            val onMessagesTabClick: () -> Unit = {
                if (activeTab == "messages") {
                    val unreadTargets = buildList {
                        chatState.groups.filter { g ->
                            (if (g.muted) g.atMe else g.unread) > 0
                        }.forEach { g ->
                            add(ChatOpenSessionRequest(mode = "group", groupId = g.id))
                        }
                        if (chatState.dmEnabled) {
                            chatState.dmThreads.filter { t ->
                                !t.muted && t.id !in chatState.mutedDmIds && t.unread > 0
                            }.forEach { t ->
                                add(ChatOpenSessionRequest(mode = "dm", dmThreadId = t.id))
                            }
                        }
                    }
                    if (unreadTargets.isNotEmpty()) {
                        val idx = unreadCycleCursor % unreadTargets.size
                        unreadCycleCursor++
                        chatOpenRequest = unreadTargets[idx].copy(requestId = System.nanoTime())
                    } else if (chatState.shouldBypassSessionList()) {
                        val joined = chatState.groups.filter { it.joined }
                        val targetGroupId = if (joined.size >= 2) {
                            val curIdx = joined.indexOfFirst { it.id == chatState.activeGroupId }
                            val nextIdx = if (curIdx >= 0) (curIdx + 1) % joined.size else 0
                            joined[nextIdx].id
                        } else {
                            joined.firstOrNull()?.id ?: chatState.activeGroupId
                        }
                        chatOpenRequest = ChatOpenSessionRequest(
                            mode = "group",
                            groupId = targetGroupId,
                            requestId = System.nanoTime(),
                        )
                    } else {
                        chatOpenRequest = ChatOpenSessionRequest(mode = "list")
                    }
                } else {
                    activeTab = "messages"
                }
            }

            val renderActiveTab = @Composable {
                when (activeTab) {
                    "wall" -> WallScreen(
                        api = api,
                        me = me,
                        initialTag = wallTagArg,
                        initialAuthor = wallAuthorArg,
                        initialSeries = wallSeriesArg,
                        initialDetailMemeId = wallMemeArg,
                        notifUnreadCount = notifUnreadCount,
                        openBrowseSettingsSignal = openBrowseSettingsSignal,
                        openSeriesIndexSignal = openSeriesIndexSignal,
                        scrollToTopSignal = wallScrollToTopSignal,
                        externalRefreshSignal = wallRefreshSignal,
                        onConsumeInitialArgs = {
                            wallTagArg = null
                            wallAuthorArg = null
                            wallSeriesArg = null
                            wallMemeArg = null
                        },
                        onOpenNotifications = { showNotifsSheet = true },
                        onOpenUrl = { url -> webLinkModalUrl = url },
                        onOpenCompose = { req -> activeComposeRequest = req },
                        onRequireLogin = { showLoginModal = true },
                        onToggleBlurAll = { enabled ->
                            onUpdatePrivacyConfig(privacyConfig.copy(blurAllEnabled = enabled))
                        },
                        moyuHeaderPillText = wallMoyuPill,
                        onClickMoyuPill = {
                            discoverInitialRoute = "/moyu"
                            activeTab = "discover"
                        },
                        forceUnseenSignal = forceWallUnseenSignal,
                        onDockScrollHideChanged = { hidden -> wallScrollDockHidden = hidden },
                    )

                    "messages" -> ChatScreen(
                        api = api,
                        controller = chatController,
                        me = me,
                        hasFloatingDock = palette.isModernUi,
                        onDockHideChanged = { hidden -> chatDockHidden = hidden },
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
                        notifUnreadCount = notifUnreadCount,
                        onOpenNotifications = { showNotifsSheet = true },
                        openSessionRequest = chatOpenRequest,
                        onConsumeOpenSessionRequest = { chatOpenRequest = null },
                        onNavigateToAuthor = { uname ->
                            wallTagArg = null
                            wallSeriesArg = null
                            wallAuthorArg = uname
                            activeTab = "wall"
                        },
                        onOpenDiscoverRoute = { route ->
                            webLinkModalUrl = if (route.startsWith("http")) route else "https://52dmeme.com$route"
                        },
                        onConvertMessageToPost = { msg ->
                            if (me == null) {
                                showLoginModal = true
                            } else {
                                scope.launch {
                                    val files = withContext(Dispatchers.IO) {
                                        msg.allMedia.mapNotNull { media ->
                                            val u = media.resolvedUrl
                                            if (u.isNotBlank()) api.fetchCachedMediaFile(u)?.absolutePath else null
                                        }
                                    }
                                    val srcBody = msg.body.trim()
                                    val initContent = buildString {
                                        append("来源：群聊 @${msg.displayAuthor}")
                                        if (srcBody.isNotBlank()) append("\n$srcBody")
                                    }
                                    activeComposeRequest = ComposeLaunchRequest(
                                        triggerSeq = System.currentTimeMillis(),
                                        initialGenre = ComposeGenre.IMAGE,
                                        preStagedPaths = files,
                                        initialContent = initContent,
                                    )
                                }
                            }
                        },
                    )

                    "discover" -> HubScreen(
                        api = api,
                        db = app.db,
                        me = me,
                        modules = modules,
                        initialModuleRoute = discoverInitialRoute,
                        onConsumeInitialRoute = { discoverInitialRoute = null },
                        onRefreshModules = {
                            scope.launch { modules = api.scanDynamicTopbarModules() }
                        },
                        onRequireLogin = { showLoginModal = true },
                        notifUnreadCount = notifUnreadCount,
                        onOpenNotifications = { showNotifsSheet = true },
                        onJumpUnseenWall = {
                            forceWallUnseenSignal++
                            activeTab = "wall"
                        },
                        chatController = chatController,
                        effectiveDisguise = effectiveDisguise,
                        bossKeyActive = bossKeyActive,
                        todayStats = todayStats,
                        moyuConfig = moyuConfig,
                        onMoyuConfigUpdated = { moyuConfig = it },
                        moyuForegroundSec = moyuForegroundSec,
                        onResetMoyuForeground = {
                            moyuForegroundSec = 0L
                            scope.launch { api.putPref(KV_MOYU_FG_SEC, "0") }
                        },
                        onPinnedIdsChanged = { pinnedHubIds = it },
                        onJumpToGroupChat = { gid ->
                            chatOpenRequest = ChatOpenSessionRequest(mode = "group", groupId = gid)
                            activeTab = "messages"
                        },
                    )

                    "me" -> MeScreen(
                        api = api,
                        updateController = app.updateController,
                        me = me,
                        themeMode = themeMode,
                        uiStyle = uiStyle,
                        accentConfig = accentConfig,
                        privacyConfig = privacyConfig,
                        privacyShieldEnabled = privacyShieldEnabled,
                        activeIconAlias = activeIconAlias,
                        notifUnreadCount = notifUnreadCount,
                        notifTotalCount = notifs.size,
                        onOpenNotifications = { showNotifsSheet = true },
                        onChangeTheme = onChangeTheme,
                        onChangeUiStyle = onChangeUiStyle,
                        onChangeAccentConfig = onChangeAccentConfig,
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
                            todayStats = null
                        },
                        onOpenMeme = { id -> shellDetailMemeId = id },
                        onOpenComposeEdit = { meme ->
                            activeComposeRequest = ComposeLaunchRequest(
                                triggerSeq = System.currentTimeMillis(),
                                editingMeme = meme,
                            )
                        },
                        onOpenWebRoute = { route, _ ->
                            val clean = route.trim()
                            if (clean.startsWith("/pets") ||
                                clean.startsWith("/cards") ||
                                clean.startsWith("/novels") ||
                                clean.startsWith("/raffle") ||
                                clean.startsWith("/road") ||
                                clean.startsWith("/moyu") ||
                                clean.startsWith("/arcade")
                            ) {
                                discoverInitialRoute = clean
                                activeTab = "discover"
                            } else {
                                webLinkModalUrl = clean
                            }
                        },
                        onOpenNotifSettings = {
                            openNotifPrefsDirectly = true
                            showNotifsSheet = true
                        },
                        initialSettingsSubpage = pendingSettingsSubpage,
                        onConsumeSettingsSubpage = { pendingSettingsSubpage = null },
                        onRefreshMe = { bootstrapSession() },
                        chatController = chatController,
                        effectiveDisguise = effectiveDisguise,
                        bossKeyActive = bossKeyActive,
                    )
                }
            }

            if (palette.isModernUi) {
                val showDock = !isImeVisible && !wallScrollDockHidden && !(activeTab == "messages" && chatDockHidden)
                val netFail = !netStatus.running && !netStatus.lastError.isNullOrBlank()
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(palette.bg),
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        renderActiveTab()
                    }

                    if (showNetworkStrip) {
                        Surface(
                            color = palette.surfaceRaised,
                            shape = CircleShape,
                            shadowElevation = 8.dp,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                                .padding(
                                    start = 20.dp,
                                    end = 20.dp,
                                    bottom = if (showDock) 80.dp else 14.dp,
                                )
                                .border(1.dp, palette.glassBorder, CircleShape)
                                .clickable {
                                    pendingSettingsSubpage = "network"
                                    activeTab = "me"
                                },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(if (netFail) palette.danger else palette.gold),
                                )
                                Text(
                                    text = if (netFail) "网络连接失败 · 点按查看" else "网络连接中…",
                                    color = if (netFail) palette.danger else palette.text,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Surface(
                                    color = palette.primaryContainer,
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .clickable {
                                            if (netStatus.running) {
                                                api.rustNetManager.triggerReselect()
                                            } else {
                                                api.rustNetManager.start()
                                            }
                                        },
                                ) {
                                    Text(
                                        text = "重试",
                                        color = palette.primary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                                    )
                                }
                            }
                        }
                    }

                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(bottom = if (showDock) 80.dp else 14.dp),
                    )

                    AnimatedVisibility(
                        visible = showDock,
                        enter = slideInVertically { it * 2 } + fadeIn(),
                        exit = slideOutVertically { it * 2 } + fadeOut(),
                        modifier = Modifier.align(Alignment.BottomCenter),
                    ) {
                        Surface(
                            color = palette.glassSurface,
                            shape = CircleShape,
                            shadowElevation = if (palette.isDark) 16.dp else 10.dp,
                            modifier = Modifier
                                .navigationBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                                .widthIn(max = 400.dp)
                                .fillMaxWidth()
                                .height(58.dp)
                                .border(1.dp, palette.glassBorder, CircleShape),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 6.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                ModernCapsuleDockItem(
                                    modifier = Modifier.weight(1f),
                                    selected = activeTab == "wall",
                                    icon = Icons.Rounded.Collections,
                                    label = "图廊",
                                    palette = palette,
                                    onClick = {
                                        if (activeTab == "wall") {
                                            wallScrollToTopSignal++
                                        } else {
                                            activeTab = "wall"
                                        }
                                    },
                                    onLongClick = {
                                        activeTab = "wall"
                                        openBrowseSettingsSignal++
                                    },
                                )
                                ModernCapsuleDockItem(
                                    modifier = Modifier.weight(1f),
                                    selected = activeTab == "messages",
                                    icon = Icons.AutoMirrored.Rounded.Chat,
                                    label = "消息",
                                    badgeCount = chatState.totalUnreadCount,
                                    badgeIsMention = chatState.hasUnreadMention,
                                    palette = palette,
                                    onClick = onMessagesTabClick,
                                )
                                ModernCapsuleDockItem(
                                    modifier = Modifier.weight(1f),
                                    selected = activeTab == "discover",
                                    icon = Icons.Rounded.Explore,
                                    label = "发现",
                                    showDot = hasNewDiscoverModule,
                                    dotColor = palette.badge,
                                    palette = palette,
                                    onLongClick = { showDiscoverQuickRing = true },
                                    onClick = { activeTab = "discover" },
                                )
                                ModernCapsuleDockItem(
                                    modifier = Modifier.weight(1f),
                                    selected = activeTab == "me",
                                    icon = Icons.Rounded.Person,
                                    label = "我的",
                                    showDot = hasUpdateDot,
                                    dotColor = palette.primary,
                                    palette = palette,
                                    onClick = { activeTab = "me" },
                                )
                            }
                        }
                    }
                }
            } else {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(palette.bg),
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    topBar = {
                        Surface(
                            color = palette.surface,
                            modifier = Modifier.statusBarsPadding(),
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                if (showNetworkStrip) {
                                    val netFail = !netStatus.running && !netStatus.lastError.isNullOrBlank()
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(ForHappyDimens.inlineBannerHeight)
                                            .background(palette.surface2)
                                            .clickable {
                                                pendingSettingsSubpage = "network"
                                                activeTab = "me"
                                            }
                                            .padding(horizontal = 14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = if (netFail) "网络连接失败 · 点按查看" else "网络连接中…",
                                            color = if (netFail) palette.danger else palette.muted,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f),
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = "重试",
                                            color = palette.primary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .clickable {
                                                    if (netStatus.running) {
                                                        api.rustNetManager.triggerReselect()
                                                    } else {
                                                        api.rustNetManager.start()
                                                    }
                                                }
                                                .padding(horizontal = 8.dp, vertical = 4.dp),
                                        )
                                    }
                                    HorizontalDivider(color = palette.line)
                                }
                            }
                        }
                    },
                    bottomBar = {
                        if (!isImeVisible) {
                            Surface(
                                color = palette.surface,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(modifier = Modifier.navigationBarsPadding()) {
                                    HorizontalDivider(color = palette.line)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(ForHappyDimens.bottomBarHeight),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        CompactBottomBarItem(
                                            modifier = Modifier.weight(1f),
                                            selected = activeTab == "wall",
                                            icon = Icons.Rounded.Collections,
                                            label = "图廊",
                                            palette = palette,
                                            onClick = {
                                                if (activeTab == "wall") {
                                                    wallScrollToTopSignal++
                                                } else {
                                                    activeTab = "wall"
                                                }
                                            },
                                            onLongClick = {
                                                activeTab = "wall"
                                                openBrowseSettingsSignal++
                                            },
                                        )
                                        CompactBottomBarItem(
                                            modifier = Modifier.weight(1f),
                                            selected = activeTab == "messages",
                                            icon = Icons.AutoMirrored.Rounded.Chat,
                                            label = "消息",
                                            badgeCount = chatState.totalUnreadCount,
                                            badgeIsMention = chatState.hasUnreadMention,
                                            palette = palette,
                                            onClick = onMessagesTabClick,
                                        )
                                        CompactBottomBarItem(
                                            modifier = Modifier.weight(1f),
                                            selected = activeTab == "discover",
                                            icon = Icons.Rounded.Explore,
                                            label = "发现",
                                            showDot = hasNewDiscoverModule,
                                            dotColor = palette.badge,
                                            palette = palette,
                                            onLongClick = { showDiscoverQuickRing = true },
                                            onClick = { activeTab = "discover" },
                                        )
                                        CompactBottomBarItem(
                                            modifier = Modifier.weight(1f),
                                            selected = activeTab == "me",
                                            icon = Icons.Rounded.Person,
                                            label = "我的",
                                            showDot = hasUpdateDot,
                                            dotColor = palette.primary,
                                            palette = palette,
                                            onClick = { activeTab = "me" },
                                        )
                                    }
                                }
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
                        renderActiveTab()
                    }
                }
            }
        }
        }

        // Non-destructive Z5 Boss Cloud Doc cover (preserves underlying scroll/input state)
        if (bossKeyActive) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    ),
            ) {
                BossDocScreen(
                    longPressTitleEnabled = privacyConfig.bossExitLongPressTitle,
                    tapCollabFiveEnabled = privacyConfig.bossExitTapCollabFive,
                    doubleBackExitEnabled = privacyConfig.bossDoubleBack,
                    hapticEnabled = privacyConfig.stealthHapticFeedback,
                    api = api,
                    onExit = { onSetBossKeyActive(false) },
                )
            }
        }
    }

    if (!bossKeyActive && showPrivacySettingsSheet) {
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
                        uiStyle = uiStyle,
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

    if (!bossKeyActive && showNotifsSheet && me != null) {
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
                activeTab = "messages"
            },
            onNavigateHub = { activeTab = "discover" },
            onOpenUrl = { url -> webLinkModalUrl = url },
            onDismiss = {
                showNotifsSheet = false
                openNotifPrefsDirectly = false
            },
            siteNotices = siteNotices,
            dismissedNoticeIds = dismissedNoticeIds,
            onDismissNotice = { nid ->
                val next = dismissedNoticeIds + nid
                dismissedNoticeIds = next
                scope.launch {
                    api.putPref(
                        "ui.dismissed_notice_ids",
                        ForHappyJson.encodeToString(ListSerializer(Long.serializer()), next.toList().takeLast(50)),
                    )
                }
            },
            onToggleItemReadLocal = { target ->
                if (target.isUnread) {
                    markSingleNotificationRead(target)
                } else {
                    notifs = notifs.map { if (it.matchesSameItem(target)) it.copy(unread = true) else it }
                    notifUnreadCount = (notifUnreadCount + 1).coerceAtLeast(1)
                }
            },
            onOpenPetsRoute = {
                discoverInitialRoute = "/pets"
                activeTab = "discover"
            },
            initialShowSettings = openNotifPrefsDirectly,
        )
    }

    if (!bossKeyActive) {
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
                wallAuthorArg = null
                wallSeriesArg = null
                wallTagArg = tag
                activeTab = "wall"
            },
            onAuthorClick = { uname ->
                shellDetailMemeId = null
                wallTagArg = null
                wallSeriesArg = null
                wallAuthorArg = uname
                activeTab = "wall"
            },
            onOpenUrl = { url -> webLinkModalUrl = url },
            onOpenEditWorkbench = { targetMeme ->
                shellDetailMemeId = null
                activeComposeRequest = ComposeLaunchRequest(
                    triggerSeq = System.currentTimeMillis(),
                    editingMeme = targetMeme,
                )
            },
            onStateUpdated = {},
        )
    }

    activeSharePayload?.let { sharePayload ->
        SystemShareTargetSheet(
            context = context,
            api = api,
            me = me,
            payload = sharePayload,
            disguiseMode = effectiveDisguise,
            groups = chatState.groups,
            onRequireLogin = { showLoginModal = true },
            onOpenComposeForShare = { req ->
                activeComposeRequest = req
            },
            onSentToGroup = { gid ->
                chatOpenRequest = ChatOpenSessionRequest(
                    mode = "group",
                    groupId = gid,
                )
                activeTab = "messages"
            },
            onSentToDm = { tid ->
                chatOpenRequest = ChatOpenSessionRequest(
                    mode = "dm",
                    dmThreadId = tid,
                )
                activeTab = "messages"
            },
            onDismiss = { activeSharePayload = null },
        )
    }

    activeComposeRequest?.let { req ->
        ComposeWorkbench(
            db = app.db,
            api = api,
            me = me,
            launchRequest = req,
            onPublished = { publishedIds ->
                wallRefreshSignal++
                val firstId = publishedIds.firstOrNull()
                if (firstId != null && effectiveDisguise == AppDisguiseMode.NONE) {
                    scope.launch {
                        val msg = if (publishedIds.size > 1) {
                            "已发布 ${publishedIds.size} 帖（No.$firstId 等）"
                        } else {
                            "已发布 No.$firstId"
                        }
                        val res = snackbarHostState.showSnackbar(
                            message = msg,
                            actionLabel = "查看",
                            duration = SnackbarDuration.Short,
                        )
                        if (res == SnackbarResult.ActionPerformed) {
                            shellDetailMemeId = firstId
                        }
                    }
                }
            },
            onEdited = {
                wallRefreshSignal++
            },
            onDismiss = { activeComposeRequest = null },
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
            properties = FullscreenDialogProperties,
        ) {
            ConfigureFullscreenDialogWindow(darkStatusIcons = !palette.isDark)
            DialogBackOwnerScope {
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
    }

    if (showDiscoverQuickRing) {
        val catalog = remember(modules) { resolveHubCatalog(modules) }
        val quickItems = remember(catalog, pinnedHubIds) {
            val map = catalog.associateBy { it.id }
            val resolved = pinnedHubIds.mapNotNull { map[it] }
            (resolved.ifEmpty { catalog }).take(4)
        }
        AlertDialog(
            onDismissRequest = { showDiscoverQuickRing = false },
            containerColor = palette.surface,
            titleContentColor = palette.text,
            textContentColor = palette.text,
            title = { Text("快捷入口 · 前4个常用", fontSize = 15.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    quickItems.forEach { entry ->
                        Surface(
                            color = palette.surface2,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showDiscoverQuickRing = false
                                    discoverInitialRoute = entry.route
                                    activeTab = "discover"
                                },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Text(entry.emoji, fontSize = 18.sp)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(entry.label, color = palette.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                    Text(entry.subtitle, color = palette.muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Text("打开 ›", color = palette.primary, fontSize = 12.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDiscoverQuickRing = false }) { Text("关闭") }
            },
        )
    }

    webLinkModalUrl?.let { url ->
        Dialog(
            onDismissRequest = { webLinkModalUrl = null },
            properties = FullscreenDialogProperties,
        ) {
            ConfigureFullscreenDialogWindow(darkStatusIcons = !palette.isDark)
            DialogBackOwnerScope {
                Box(modifier = Modifier.fillMaxSize().background(palette.bg)) {
                    ShelllessWebScreen(
                        route = url,
                        title = url.removePrefix("https://").removePrefix("http://"),
                        cookieJar = api.cookieJar,
                        assetCache = api.webAssetCache,
                        showCompactBar = false,
                        floatingCapsule = true,
                        pausedOverlay = currentBossActive,
                        onExit = { webLinkModalUrl = null },
                        api = api,
                        chatController = chatController,
                        me = me,
                        onRequireLogin = { showLoginModal = true },
                        onJumpToGroupChat = { gid ->
                            webLinkModalUrl = null
                            chatOpenRequest = ChatOpenSessionRequest(mode = "group", groupId = gid)
                            activeTab = "messages"
                        },
                    )
                }
            }
        }
    }
    }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ModernCapsuleDockItem(
    selected: Boolean,
    icon: ImageVector,
    label: String,
    palette: ForHappyPalette,
    modifier: Modifier = Modifier,
    badgeCount: Int = 0,
    badgeIsMention: Boolean = false,
    showDot: Boolean = false,
    dotColor: Color = palette.badge,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    val fg = if (selected) palette.primary else palette.muted
    val pillBg = if (selected) {
        palette.primary.copy(alpha = if (palette.isDark) 0.20f else 0.13f)
    } else {
        Color.Transparent
    }
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(CircleShape)
            .background(pillBg)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        DockIconBadge(
            icon = icon,
            label = label,
            tint = fg,
            iconSize = 21.dp,
            badgeCount = badgeCount,
            badgeIsMention = badgeIsMention,
            showDot = showDot,
            dotColor = dotColor,
            palette = palette,
        )
        Spacer(Modifier.height(1.dp))
        Text(
            text = label,
            color = fg,
            fontSize = if (selected) 11.sp else 10.5.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
        )
    }
}

@Composable
private fun DockIconBadge(
    icon: ImageVector,
    label: String,
    tint: Color,
    iconSize: androidx.compose.ui.unit.Dp,
    badgeCount: Int,
    badgeIsMention: Boolean,
    showDot: Boolean,
    dotColor: Color,
    palette: ForHappyPalette,
) {
    Box(contentAlignment = Alignment.TopEnd) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(iconSize),
        )
        if (badgeCount > 0 || badgeIsMention) {
            val badgeBg = if (badgeIsMention) palette.danger else palette.badge
            val countLabel = if (badgeCount > 99) "99+" else "$badgeCount"
            val text = when {
                badgeIsMention && badgeCount > 0 -> "@$countLabel"
                badgeIsMention -> "@我"
                else -> countLabel
            }
            Box(
                modifier = Modifier
                    .offset(x = 8.dp, y = (-4).dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(badgeBg)
                    .padding(horizontal = 4.dp, vertical = 1.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = text,
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    style = TextStyle(fontFeatureSettings = TABULAR_FONT_FEATURE),
                )
            }
        } else if (showDot) {
            Box(
                modifier = Modifier
                    .offset(x = 3.dp, y = (-2).dp)
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(dotColor),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CompactBottomBarItem(
    selected: Boolean,
    icon: ImageVector,
    label: String,
    palette: ForHappyPalette,
    modifier: Modifier = Modifier,
    badgeCount: Int = 0,
    badgeIsMention: Boolean = false,
    showDot: Boolean = false,
    dotColor: Color = palette.badge,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    val fg = if (selected) palette.primary else palette.muted
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .fillMaxHeight()
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        DockIconBadge(
            icon = icon,
            label = label,
            tint = fg,
            iconSize = 22.dp,
            badgeCount = badgeCount,
            badgeIsMention = badgeIsMention,
            showDot = showDot,
            dotColor = dotColor,
            palette = palette,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            color = fg,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
        )
    }
}
