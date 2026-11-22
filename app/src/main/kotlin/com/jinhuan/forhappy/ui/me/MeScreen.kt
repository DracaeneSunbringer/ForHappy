package com.jinhuan.forhappy.ui.me

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.jinhuan.forhappy.core.chat.ChatSessionController
import com.jinhuan.forhappy.core.model.AppDisguiseMode
import com.jinhuan.forhappy.core.model.AppIconAlias
import com.jinhuan.forhappy.core.model.AppThemeMode
import com.jinhuan.forhappy.core.model.AppUiStyle
import com.jinhuan.forhappy.core.model.FolderItem
import com.jinhuan.forhappy.core.model.MeUser
import com.jinhuan.forhappy.core.model.MemeItem
import com.jinhuan.forhappy.core.model.Owner52bCard
import com.jinhuan.forhappy.core.model.PrivacyStealthConfig
import com.jinhuan.forhappy.core.model.ShortcutGestureMode
import com.jinhuan.forhappy.core.model.WalletSummary
import com.jinhuan.forhappy.core.network.api.ForHappyApiClient
import com.jinhuan.forhappy.core.network.media.MediaUrlRewriter
import com.jinhuan.forhappy.core.cosmetics.BadgeAndCosmeticRegistry
import com.jinhuan.forhappy.core.update.AppUpdateController
import com.jinhuan.forhappy.core.update.UpdateStatus
import com.jinhuan.forhappy.ui.components.BadgeChipView
import com.jinhuan.forhappy.ui.components.BadgePill
import com.jinhuan.forhappy.ui.components.ForHappyAvatar
import com.jinhuan.forhappy.ui.components.TitleChipView
import com.jinhuan.forhappy.ui.components.parseSafeHex
import com.jinhuan.forhappy.ui.theme.ForHappyDimens
import com.jinhuan.forhappy.ui.theme.LocalForHappyPalette
import com.jinhuan.forhappy.ui.theme.LocalPrivacyStealthConfig
import com.jinhuan.forhappy.ui.theme.TABULAR_FONT_FEATURE
import com.jinhuan.forhappy.ui.wall.FullscreenMediaOverlay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val ME_STREAM_TABS = listOf(
    "fav" to "收藏",
    "like" to "喜欢",
    "reply" to "回应",
    "comment" to "评论",
    "post" to "发布",
    "seen" to "最近看过",
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun MeScreen(
    api: ForHappyApiClient,
    updateController: AppUpdateController,
    me: MeUser?,
    themeMode: AppThemeMode,
    uiStyle: AppUiStyle = AppUiStyle.MODERN,
    privacyConfig: PrivacyStealthConfig,
    privacyShieldEnabled: Boolean,
    activeIconAlias: AppIconAlias,
    notifUnreadCount: Int = 0,
    @Suppress("UNUSED_PARAMETER") notifTotalCount: Int = 0,
    onOpenNotifications: () -> Unit = {},
    onChangeTheme: (AppThemeMode) -> Unit,
    onChangeUiStyle: (AppUiStyle) -> Unit = {},
    onChangeDisguise: (AppDisguiseMode) -> Unit,
    onUpdatePrivacyConfig: (PrivacyStealthConfig) -> Unit,
    onTogglePrivacyShield: (Boolean) -> Unit,
    onChangeIconAlias: (AppIconAlias) -> Unit,
    onRequireLogin: () -> Unit,
    onLoggedOut: () -> Unit,
    onOpenMeme: (Long) -> Unit = {},
    @Suppress("UNUSED_PARAMETER") onOpenComposeEdit: ((MemeItem) -> Unit)? = null,
    onOpenWebRoute: (route: String, title: String) -> Unit = { _, _ -> },
    onOpenNotifSettings: () -> Unit = {},
    initialSettingsSubpage: String? = null,
    onConsumeSettingsSubpage: () -> Unit = {},
    onRefreshMe: () -> Unit = {},
    chatController: ChatSessionController? = null,
    effectiveDisguise: AppDisguiseMode = AppDisguiseMode.NONE,
    bossKeyActive: Boolean = false,
) {
    val palette = LocalForHappyPalette.current
    val stealth = LocalPrivacyStealthConfig.current
    val scope = rememberCoroutineScope()
    val updateState by updateController.state.collectAsState()
    val navBottomPad = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    var stats by remember { mutableStateOf<JsonObject?>(null) }
    var wallet by remember { mutableStateOf<WalletSummary?>(null) }
    var cards52b by remember { mutableStateOf<List<Owner52bCard>>(emptyList()) }
    var folders by remember { mutableStateOf<List<FolderItem>>(emptyList()) }
    var cleanRawPref by remember { mutableStateOf<String?>(null) }

    var activeStreamTab by remember { mutableStateOf("fav") }
    var selectedFolderId by remember { mutableStateOf<Long?>(null) }
    var sortMode by remember { mutableStateOf("new") }
    var searchExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var streamPage by remember { mutableIntStateOf(1) }
    var streamItems by remember { mutableStateOf<List<MemeItem>>(emptyList()) }
    var streamMore by remember { mutableStateOf(false) }
    var streamLoading by remember { mutableStateOf(false) }
    var streamLoadingMore by remember { mutableStateOf(false) }

    var showAvatarFullscreen by remember { mutableStateOf(false) }
    var showProfileEdit by remember { mutableStateOf(false) }
    var showWalletDialog by remember { mutableStateOf(false) }
    var showCardsDialog by remember { mutableStateOf(false) }
    var showDecoDialog by remember { mutableStateOf(false) }
    var showInvitesDialog by remember { mutableStateOf(false) }

    var showSettingsDialog by remember { mutableStateOf(false) }
    var settingsSubpageArg by remember { mutableStateOf<String?>(null) }

    var showNewFolderDialog by remember { mutableStateOf(false) }
    var managingFolder by remember { mutableStateOf<FolderItem?>(null) }

    LaunchedEffect(initialSettingsSubpage) {
        if (!initialSettingsSubpage.isNullOrBlank()) {
            settingsSubpageArg = initialSettingsSubpage
            showSettingsDialog = true
            onConsumeSettingsSubpage()
        }
    }

    LaunchedEffect(me?.id) {
        cleanRawPref = api.getPref(KV_DECO_CLEAN)
        if (me != null) {
            stats = api.fetchMeStats()
            wallet = api.fetchWallet()
            cards52b = api.fetchOwner52bCards()
            folders = api.fetchFolders()
        } else {
            stats = null
            wallet = null
            cards52b = emptyList()
            folders = emptyList()
            streamItems = emptyList()
        }
    }

    LaunchedEffect(me?.id, activeStreamTab, selectedFolderId, sortMode, searchQuery) {
        if (me == null) return@LaunchedEffect
        streamLoading = true
        streamPage = 1
        val resp = api.fetchMeItems(
            tab = activeStreamTab,
            page = 1,
            query = searchQuery,
            sort = sortMode,
            folderId = if (activeStreamTab == "fav") selectedFolderId else null,
        )
        streamItems = resp.items
        streamMore = resp.more
        streamLoading = false
    }

    val showUpdateDot = effectiveDisguise == AppDisguiseMode.NONE &&
        !bossKeyActive &&
        (updateState.status == UpdateStatus.AVAILABLE || updateState.status == UpdateStatus.READY_TO_INSTALL)

    val cosmeticCatalogRev by BadgeAndCosmeticRegistry.catalogRevision.collectAsState()
    val decoConfig = remember(me?.deco, cleanRawPref) { parseDecoConfig(me?.deco, cleanRawPref) }
    val equippedIdentity = remember(
        me?.deco,
        me?.badges,
        me?.officialTitles,
        me?.isAdmin,
        me?.isFancyGranted,
        me?.cosmeticAccess,
        palette.isDark,
        cosmeticCatalogRev,
    ) {
        BadgeAndCosmeticRegistry.resolveEquippedIdentity(
            decoJson = me?.deco,
            badgesJson = me?.badges,
            officialTitles = me?.officialTitles.orEmpty(),
            where = "card",
            isAdmin = me?.isAdmin == true,
            hasFancyPermission = me?.isFancyGranted,
            cosmeticAccess = me?.cosmeticAccess,
            isDarkTheme = palette.isDark,
        )
    }
    val ownedBadges = remember(me?.badges, me?.farmBadgeProgress) {
        BadgeAndCosmeticRegistry.parseOwnedBadges(me?.badges, me?.farmBadgeProgress.orEmpty())
    }
    val renderAssetTiles = @Composable { rowModifier: Modifier ->
        Row(modifier = rowModifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val coinsVal = wallet?.let { NumberFormat.getNumberInstance(Locale.CHINA).format(it.resolvedCoins) } ?: "--"
            val cardsVal = if (cards52b.isNotEmpty()) "${cards52b.count { it.owned }}/${cards52b.size}" else "--"

            AssetGridCell(
                emoji = "🪙",
                value = coinsVal,
                label = "D币",
                onClick = { if (me == null) onRequireLogin() else showWalletDialog = true },
                modifier = Modifier.weight(1f),
            )
            AssetGridCell(
                emoji = "🃏",
                value = cardsVal,
                label = "52B集卡",
                onClick = { if (me == null) onRequireLogin() else showCardsDialog = true },
                modifier = Modifier.weight(1f),
            )
            AssetGridCell(
                emoji = "🎨",
                value = if (me != null && ownedBadges.isNotEmpty()) "${ownedBadges.size}枚" else "装扮",
                label = "装扮与徽章",
                onClick = { if (me == null) onRequireLogin() else showDecoDialog = true },
                modifier = Modifier.weight(1f),
            )
            AssetGridCell(
                emoji = "🎟",
                value = "邀请",
                label = "邀请码",
                onClick = { if (me == null) onRequireLogin() else showInvitesDialog = true },
                modifier = Modifier.weight(1f),
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.bg),
    ) {
        // Top Header: `我的`, `🔔`, `⚙•`
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
                        text = "我的",
                        color = palette.text,
                        fontSize = if (palette.isModernUi) 22.sp else 18.sp,
                        fontWeight = if (palette.isModernUi) FontWeight.ExtraBold else FontWeight.Bold,
                        letterSpacing = if (palette.isModernUi) (-0.5).sp else TextStyle.Default.letterSpacing,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(if (palette.isModernUi) 6.dp else 0.dp),
                    ) {
                        if (me != null) {
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
                                        Icons.Rounded.NotificationsNone,
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
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            BadgePill(
                                text = "登录",
                                containerColor = palette.primary,
                                contentColor = palette.onPrimary,
                                modifier = Modifier.clickable { onRequireLogin() },
                            )
                            if (!palette.isModernUi) Spacer(Modifier.width(4.dp))
                        }

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(if (palette.isModernUi) 36.dp else 40.dp)
                                .clip(CircleShape)
                                .let { if (palette.isModernUi) it.background(palette.surface2.copy(alpha = 0.78f)) else it }
                                .clickable {
                                    settingsSubpageArg = null
                                    showSettingsDialog = true
                                },
                        ) {
                            Box(contentAlignment = Alignment.TopEnd) {
                                Icon(Icons.Rounded.Settings, contentDescription = "设置", tint = palette.text, modifier = Modifier.size(if (palette.isModernUi) 19.dp else 21.dp))
                                if (showUpdateDot) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(palette.primary),
                                    )
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
            contentPadding = PaddingValues(bottom = if (palette.isModernUi) navBottomPad + 92.dp else 18.dp),
        ) {
            // Compact Profile Hero + 4 Asset Bento Row
            item(key = "compact_profile_header") {
                val isModern = palette.isModernUi
                val heroShape = RoundedCornerShape(if (isModern) 24.dp else 0.dp)
                val heroGlow = Brush.linearGradient(
                    listOf(
                        Color.Transparent,
                        palette.primary.copy(alpha = if (palette.isDark) 0.14f else 0.07f),
                    ),
                )
                Column(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        color = palette.surface,
                        shape = heroShape,
                        shadowElevation = if (isModern && !palette.isDark) 3.dp else 0.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .let { mod ->
                                if (isModern) {
                                    mod.padding(horizontal = 14.dp, vertical = 10.dp)
                                        .then(if (palette.cardBorder != Color.Transparent) Modifier.border(1.dp, palette.cardBorder, heroShape) else Modifier)
                                } else {
                                    mod
                                }
                            },
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .let { if (isModern) it.background(heroGlow) else it }
                                .padding(horizontal = if (isModern) 16.dp else 14.dp, vertical = if (isModern) 16.dp else 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .clickable(enabled = !me?.avatar.isNullOrBlank()) { showAvatarFullscreen = true },
                                ) {
                                    ForHappyAvatar(
                                        username = me?.displayName ?: "guest",
                                        avatarUrl = me?.avatar,
                                        imageLoader = api.imageLoader,
                                        size = if (isModern) 68.dp else ForHappyDimens.avatarProfile,
                                    )
                                }

                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    if (me == null) {
                                        Text("尚未登录", color = palette.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                        Text("登录 52dmeme.com 解锁 D币、集卡、收藏与同步", color = palette.muted, fontSize = 12.sp)
                                    } else {
                                        val grad = remember(equippedIdentity.nickGradientHexes, decoConfig.nickGradient, palette.text) {
                                            val fromIdentity = equippedIdentity.nickGradientHexes.map { parseSafeHex(it, palette.text) }
                                            when {
                                                fromIdentity.size >= 2 -> fromIdentity
                                                decoConfig.nickGradient == "pine" -> listOf(Color(0xFF1B7F4B), Color(0xFF34A853))
                                                decoConfig.nickGradient == "sunset" -> listOf(Color(0xFFEA580C), Color(0xFFEC4899))
                                                decoConfig.nickGradient == "aurora" -> listOf(Color(0xFF06B6D4), Color(0xFF8B5CF6))
                                                decoConfig.nickGradient == "gold" -> listOf(Color(0xFFD97706), Color(0xFFF59E0B))
                                                else -> null
                                            }
                                        }
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp),
                                        ) {
                                            Text(
                                                text = me.displayName,
                                                style = if (grad != null) {
                                                    TextStyle(brush = Brush.horizontalGradient(grad), fontSize = if (isModern) 19.sp else 18.sp, fontWeight = FontWeight.Bold)
                                                } else {
                                                    TextStyle(color = palette.text, fontSize = if (isModern) 19.sp else 18.sp, fontWeight = FontWeight.Bold)
                                                },
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            equippedIdentity.title?.let { titleChip ->
                                                TitleChipView(title = titleChip)
                                            }
                                            equippedIdentity.badge?.let { badgeChip ->
                                                BadgeChipView(
                                                    badge = badgeChip,
                                                    imageLoader = api.imageLoader,
                                                    mini = false,
                                                    onClick = { showDecoDialog = true },
                                                )
                                            }
                                        }
                                        val joinDate = me.createdAt?.takeIf { it > 0L }?.let {
                                            " · ${SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(Date(it * 1000L))} 加入"
                                        }.orEmpty()
                                        Text("@${me.username}$joinDate", color = palette.muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        me.bio?.takeIf { it.isNotBlank() }?.let { bio ->
                                            Text(bio, color = palette.muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                }

                                if (me != null) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .let { if (isModern) it.background(palette.surface2.copy(alpha = 0.8f)) else it }
                                            .clickable { showProfileEdit = true },
                                    ) {
                                        Icon(Icons.Rounded.Edit, contentDescription = "编辑资料", tint = palette.muted, modifier = Modifier.size(18.dp))
                                    }
                                } else {
                                    Button(
                                        onClick = onRequireLogin,
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    ) {
                                        Text("去登录", fontSize = 12.sp)
                                    }
                                }
                            }

                            // Conditional account restriction alert
                            if (me != null && me.limits.isNotEmpty()) {
                                Surface(
                                    color = palette.danger.copy(alpha = 0.10f),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth().border(1.dp, palette.danger.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = palette.danger, modifier = Modifier.size(16.dp))
                                        val lim = me.limits.first()
                                        Text(
                                            text = "账号受限 (${lim.kind})：${lim.reason ?: "详情请留意系统通知"}",
                                            color = palette.danger,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }

                            if (!isModern) {
                                renderAssetTiles(Modifier.fillMaxWidth())
                            }
                        }
                    }
                    if (isModern) {
                        renderAssetTiles(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp)
                                .padding(bottom = 8.dp),
                        )
                    } else {
                        HorizontalDivider(color = palette.line)
                    }
                }
            }

            // Sticky 6-Tab Stream & Toolbar Header
            stickyHeader(key = "stream_sticky_header") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (palette.isModernUi) palette.bg.copy(alpha = 0.96f) else palette.surface)
                        .padding(top = 8.dp, bottom = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(if (palette.isModernUi) 8.dp else 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ME_STREAM_TABS.forEach { (tabId, label) ->
                            val cnt = stats?.get(tabId)?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() && it != "0" }
                            val selected = activeStreamTab == tabId
                            val tabLabel = if (cnt != null) "$label $cnt" else label
                            if (palette.isModernUi) {
                                Surface(
                                    color = if (selected) palette.primary else palette.surface2,
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .clickable {
                                            activeStreamTab = tabId
                                            if (tabId != "fav") selectedFolderId = null
                                        },
                                ) {
                                    Text(
                                        text = tabLabel,
                                        color = if (selected) palette.onPrimary else palette.text,
                                        fontSize = 12.sp,
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 6.dp),
                                    )
                                }
                            } else {
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        activeStreamTab = tabId
                                        if (tabId != "fav") selectedFolderId = null
                                    },
                                    label = { Text(tabLabel, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = palette.primary.copy(alpha = 0.14f),
                                        selectedLabelColor = palette.primary,
                                    ),
                                )
                            }
                        }
                    }

                    // Folder chips for `fav` tab
                    if (me != null && activeStreamTab == "fav") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (palette.isModernUi) {
                                val allSelected = selectedFolderId == null
                                Surface(
                                    color = if (allSelected) palette.primaryContainer else palette.surface2,
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .then(
                                            if (allSelected) Modifier.border(1.dp, palette.primary, CircleShape) else Modifier,
                                        )
                                        .clickable { selectedFolderId = null },
                                ) {
                                    Text(
                                        text = "全部收藏",
                                        color = if (allSelected) palette.primary else palette.text,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    )
                                }
                            } else {
                                FilterChip(
                                    selected = selectedFolderId == null,
                                    onClick = { selectedFolderId = null },
                                    label = { Text("全部收藏", fontSize = 11.sp) },
                                )
                            }
                            folders.forEach { f ->
                                val selected = selectedFolderId == f.id
                                Surface(
                                    color = if (selected) palette.primaryContainer else palette.surface2,
                                    shape = RoundedCornerShape(999.dp),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .then(
                                            when {
                                                palette.isModernUi && !selected -> Modifier
                                                else -> Modifier.border(
                                                    1.dp,
                                                    if (selected) palette.primary else palette.line,
                                                    RoundedCornerShape(999.dp),
                                                )
                                            },
                                        )
                                        .combinedClickable(
                                            onClick = { selectedFolderId = f.id },
                                            onLongClick = { managingFolder = f },
                                        ),
                                ) {
                                    Text(
                                        text = "${f.name} (${f.count})",
                                        color = if (selected) palette.primary else palette.text,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    )
                                }
                            }
                            Surface(
                                color = palette.surface2,
                                shape = RoundedCornerShape(999.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .clickable { showNewFolderDialog = true },
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = null, tint = palette.primary, modifier = Modifier.size(13.dp))
                                    Text("新建", color = palette.primary, fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    // Sort + Search row
                    if (me != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Surface(
                                color = palette.surface2,
                                shape = RoundedCornerShape(if (palette.isModernUi) 999.dp else 8.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(if (palette.isModernUi) 999.dp else 8.dp))
                                    .clickable { sortMode = if (sortMode == "new") "old" else "new" },
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Icon(Icons.Rounded.SwapVert, contentDescription = null, tint = palette.muted, modifier = Modifier.size(14.dp))
                                    Text(if (sortMode == "new") "最新优先" else "最早优先", color = palette.text, fontSize = 11.sp)
                                }
                            }
                            if (searchExpanded) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = { Text("搜标题/标签/正文…", fontSize = 12.sp) },
                                    singleLine = true,
                                    trailingIcon = {
                                        IconButton(
                                            onClick = {
                                                searchQuery = ""
                                                searchExpanded = false
                                            },
                                            modifier = Modifier.size(28.dp),
                                        ) {
                                            Icon(Icons.Rounded.Close, contentDescription = "清除", modifier = Modifier.size(16.dp))
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                            } else {
                                Spacer(Modifier.weight(1f))
                                Surface(
                                    color = palette.surface2,
                                    shape = RoundedCornerShape(if (palette.isModernUi) 999.dp else 8.dp),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(if (palette.isModernUi) 999.dp else 8.dp))
                                        .clickable { searchExpanded = true },
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Icon(Icons.Rounded.Search, contentDescription = null, tint = palette.muted, modifier = Modifier.size(14.dp))
                                        Text("搜此分类", color = palette.muted, fontSize = 11.sp)
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

            // Stream Body
            if (me == null) {
                item(key = "guest_stream") {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("登录后可在此查看收藏、喜欢、评论与浏览足迹", color = palette.muted, fontSize = 13.sp)
                    }
                }
            } else if (streamLoading) {
                item(key = "stream_loading") {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = palette.primary)
                    }
                }
            } else if (streamItems.isEmpty()) {
                item(key = "stream_empty") {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("该分类下暂时没有内容", color = palette.muted, fontSize = 13.sp)
                    }
                }
            } else {
                items(streamItems.chunked(2)) { pair ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        pair.forEach { meme ->
                            MeStreamCard(
                                api = api,
                                meme = meme,
                                blurEnabled = stealth.blurAllEnabled || meme.nsfw > 0 || meme.spoiler || meme.sensitive,
                                onClick = { onOpenMeme(meme.id) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }

                if (streamMore) {
                    item(key = "stream_more") {
                        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
                            OutlinedButton(
                                enabled = !streamLoadingMore,
                                onClick = {
                                    streamLoadingMore = true
                                    val nextPage = streamPage + 1
                                    scope.launch {
                                        val resp = api.fetchMeItems(
                                            tab = activeStreamTab,
                                            page = nextPage,
                                            query = searchQuery,
                                            sort = sortMode,
                                            folderId = if (activeStreamTab == "fav") selectedFolderId else null,
                                        )
                                        streamItems = (streamItems + resp.items).distinctBy { it.id }
                                        streamMore = resp.more
                                        streamPage = nextPage
                                        streamLoadingMore = false
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(if (streamLoadingMore) "加载中…" else "加载更多")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAvatarFullscreen && !me?.avatar.isNullOrBlank()) {
        FullscreenMediaOverlay(imageUrl = me!!.avatar!!, api = api, onDismiss = { showAvatarFullscreen = false })
    }

    if (showProfileEdit && me != null) {
        ProfileEditDialog(
            api = api,
            me = me,
            onSaved = { onRefreshMe() },
            onDismiss = { showProfileEdit = false },
        )
    }

    if (showWalletDialog && me != null) {
        WalletDetailDialog(
            api = api,
            initialWallet = wallet,
            onOpenWebRoute = { route, title ->
                showWalletDialog = false
                onOpenWebRoute(route, title)
            },
            onDismiss = {
                showWalletDialog = false
                scope.launch { wallet = api.fetchWallet() }
            },
        )
    }

    if (showCardsDialog && me != null) {
        Cards52bDialog(
            api = api,
            initialCards = cards52b,
            onOpenWebRoute = onOpenWebRoute,
            onDismiss = { showCardsDialog = false },
        )
    }

    if (showDecoDialog && me != null) {
        DecoStudioDialog(
            api = api,
            me = me,
            initialCleanRaw = cleanRawPref,
            onSaved = {
                scope.launch { cleanRawPref = api.getPref(KV_DECO_CLEAN) }
                onRefreshMe()
            },
            onDismiss = { showDecoDialog = false },
        )
    }

    if (showInvitesDialog && me != null) {
        InviteCodesDialog(
            api = api,
            onOpenWebRoute = onOpenWebRoute,
            onDismiss = { showInvitesDialog = false },
        )
    }

    if (showSettingsDialog) {
        SettingsCenterDialog(
            api = api,
            updateController = updateController,
            me = me,
            chatController = chatController,
            themeMode = themeMode,
            uiStyle = uiStyle,
            privacyConfig = privacyConfig,
            privacyShieldEnabled = privacyShieldEnabled,
            activeIconAlias = activeIconAlias,
            initialSubpage = settingsSubpageArg,
            onChangeTheme = onChangeTheme,
            onChangeUiStyle = onChangeUiStyle,
            onChangeDisguise = onChangeDisguise,
            onUpdatePrivacyConfig = onUpdatePrivacyConfig,
            onTogglePrivacyShield = onTogglePrivacyShield,
            onChangeIconAlias = onChangeIconAlias,
            onOpenNotifSettings = onOpenNotifSettings,
            onOpenInvites = { if (me == null) onRequireLogin() else showInvitesDialog = true },
            onOpenWebRoute = onOpenWebRoute,
            onRequireLogin = onRequireLogin,
            onLoggedOut = onLoggedOut,
            onDismiss = { showSettingsDialog = false },
        )
    }

    if (showNewFolderDialog) {
        var newName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            containerColor = palette.surface,
            titleContentColor = palette.text,
            textContentColor = palette.text,
            title = { Text("新建收藏夹") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("收藏夹名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                Button(
                    enabled = newName.trim().isNotEmpty(),
                    onClick = {
                        val name = newName.trim()
                        showNewFolderDialog = false
                        scope.launch {
                            val (_, list) = api.createFolder(name)
                            if (list.isNotEmpty()) folders = list else folders = api.fetchFolders()
                        }
                    },
                ) { Text("创建") }
            },
            dismissButton = { TextButton(onClick = { showNewFolderDialog = false }) { Text("取消") } },
        )
    }

    managingFolder?.let { folder ->
        var editName by remember(folder.id) { mutableStateOf(folder.name) }
        AlertDialog(
            onDismissRequest = { managingFolder = null },
            containerColor = palette.surface,
            titleContentColor = palette.text,
            textContentColor = palette.text,
            title = { Text("管理收藏夹 · ${folder.name}") },
            text = {
                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    label = { Text("重命名") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                Button(
                    enabled = editName.trim().isNotEmpty(),
                    onClick = {
                        val nextName = editName.trim()
                        managingFolder = null
                        scope.launch {
                            val updated = api.renameFolder(folder.id, nextName)
                            if (updated != null) folders = updated else folders = api.fetchFolders()
                        }
                    },
                ) { Text("重命名") }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = {
                            managingFolder = null
                            scope.launch {
                                if (api.deleteFolder(folder.id)) {
                                    if (selectedFolderId == folder.id) selectedFolderId = null
                                    folders = api.fetchFolders()
                                }
                            }
                        },
                    ) { Text("删除", color = palette.danger) }
                    TextButton(onClick = { managingFolder = null }) { Text("取消") }
                }
            },
        )
    }
}

@Composable
private fun AssetGridCell(
    emoji: String,
    value: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalForHappyPalette.current
    val tileShape = RoundedCornerShape(if (palette.isModernUi) 18.dp else 12.dp)
    Surface(
        color = if (palette.isModernUi) palette.surface else palette.surface2,
        shape = tileShape,
        shadowElevation = if (palette.isModernUi && !palette.isDark) 2.dp else 0.dp,
        modifier = modifier
            .then(
                if (palette.isModernUi) {
                    if (palette.cardBorder != Color.Transparent) Modifier.border(1.dp, palette.cardBorder, tileShape) else Modifier
                } else {
                    Modifier.border(1.dp, palette.line, tileShape)
                },
            )
            .clip(tileShape)
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(vertical = if (palette.isModernUi) 10.dp else 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (palette.isModernUi) 4.dp else 2.dp),
        ) {
            if (palette.isModernUi) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(palette.primaryContainer),
                ) {
                    Text(emoji, fontSize = 16.sp)
                }
            } else {
                Text(emoji, fontSize = 16.sp)
            }
            Text(
                value,
                color = palette.text,
                fontSize = if (palette.isModernUi) 13.sp else 12.sp,
                fontWeight = FontWeight.Bold,
                style = TextStyle(fontFeatureSettings = TABULAR_FONT_FEATURE),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(label, color = palette.muted, fontSize = 10.sp, maxLines = 1)
        }
    }
}

@Composable
private fun MeStreamCard(
    api: ForHappyApiClient,
    meme: MemeItem,
    blurEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalForHappyPalette.current
    val imgUrl = remember(meme.id, meme.primaryImageUrl) {
        meme.primaryImageUrl?.let { MediaUrlRewriter.rewriteWithExtProxy(it) }
    }
    val cardShape = RoundedCornerShape(if (palette.isModernUi) 16.dp else 12.dp)
    Surface(
        color = palette.surface,
        shape = cardShape,
        shadowElevation = if (palette.isModernUi && !palette.isDark) 2.dp else 0.dp,
        modifier = modifier
            .then(
                if (palette.isModernUi) {
                    if (palette.cardBorder != Color.Transparent) Modifier.border(1.dp, palette.cardBorder, cardShape) else Modifier
                } else {
                    Modifier.border(1.dp, palette.line, cardShape)
                },
            )
            .clip(cardShape)
            .clickable(onClick = onClick),
    ) {
        Column {
            if (!imgUrl.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.1f)
                        .background(palette.surface2),
                ) {
                    AsyncImage(
                        model = imgUrl,
                        imageLoader = api.imageLoader,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .then(if (blurEnabled) Modifier.blur(18.dp) else Modifier),
                    )
                    if (meme.imageCount > 1 || meme.images.size > 1) {
                        BadgePill(
                            text = "${maxOf(meme.imageCount, meme.images.size)}图",
                            containerColor = Color.Black.copy(alpha = 0.55f),
                            contentColor = Color.White,
                            modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
                        )
                    }
                }
            }
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val snippet = (meme.title?.takeIf { it.isNotBlank() }
                    ?: meme.text?.takeIf { it.isNotBlank() }
                    ?: meme.body?.takeIf { it.isNotBlank() }
                    ?: meme.caption?.takeIf { it.isNotBlank() }
                    ?: "No.${meme.id}").trim()
                Text(snippet, color = palette.text, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 16.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("♥ ${meme.likeCount.coerceAtLeast(meme.likes)} · 💬 ${meme.comments}", color = palette.muted, fontSize = 10.sp)
                    meme.day?.let { Text(it, color = palette.muted, fontSize = 10.sp) }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PrivacyStealthSettingsContent(
    themeMode: AppThemeMode,
    uiStyle: AppUiStyle = AppUiStyle.MODERN,
    privacyConfig: PrivacyStealthConfig,
    privacyShieldEnabled: Boolean,
    activeIconAlias: AppIconAlias,
    onChangeTheme: (AppThemeMode) -> Unit,
    onChangeDisguise: (AppDisguiseMode) -> Unit,
    onUpdatePrivacyConfig: (PrivacyStealthConfig) -> Unit,
    onTogglePrivacyShield: (Boolean) -> Unit,
    onChangeIconAlias: (AppIconAlias) -> Unit,
) {
    val palette = LocalForHappyPalette.current
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("界面伪装模式（隐藏原站五栏底栏）", color = palette.muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            AppDisguiseMode.entries.forEach { mode ->
                val selected = privacyConfig.disguiseMode == mode
                Surface(
                    color = if (selected) palette.primary.copy(alpha = 0.08f) else palette.surface2.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onChangeDisguise(mode) },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(mode.label, color = palette.text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text(mode.subtitle, color = palette.muted, fontSize = 11.sp, lineHeight = 15.sp)
                        }
                        if (selected) {
                            Spacer(Modifier.width(8.dp))
                            BadgePill("当前", containerColor = palette.primary, contentColor = palette.onPrimary)
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = palette.line)

        Text("日夜视觉主题", color = palette.muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            AppThemeMode.dayNightEntries.forEach { mode ->
                FilterChip(
                    selected = themeMode == mode,
                    onClick = { onChangeTheme(mode) },
                    label = { Text(mode.displayLabel(uiStyle), fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = palette.primary,
                        selectedLabelColor = palette.onPrimary,
                    ),
                )
            }
        }

        HorizontalDivider(color = palette.line)

        Text("全图模糊（防突现大图）", color = palette.muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        StealthToggleRow(
            title = "开启全图模糊",
            subtitle = "所有图片视频默认高斯模糊，点哪张哪张变清晰",
            checked = privacyConfig.blurAllEnabled,
            onCheckedChange = { onUpdatePrivacyConfig(privacyConfig.copy(blurAllEnabled = it)) },
        )
        StealthToggleRow(
            title = "进入伪装模式时自动开启全图模糊",
            subtitle = "切入飞书 / LINUX DO 自动糊图，退出伪装恢复",
            checked = privacyConfig.autoBlurOnEnterDisguise,
            onCheckedChange = { onUpdatePrivacyConfig(privacyConfig.copy(autoBlurOnEnterDisguise = it)) },
        )
        StealthToggleRow(
            title = "点开看清的图片滑出屏幕后自动恢复模糊",
            subtitle = "滑走后自动糊回，无需手动点第二下",
            checked = privacyConfig.autoReblurOnScrollOff,
            onCheckedChange = { onUpdatePrivacyConfig(privacyConfig.copy(autoReblurOnScrollOff = it)) },
        )
        StealthToggleRow(
            title = "切后台 / 锁屏恢复时自动开启全图模糊",
            subtitle = "离开或息屏回来时自动进入模糊防窥状态",
            checked = privacyConfig.autoBlurOnBackground,
            onCheckedChange = { onUpdatePrivacyConfig(privacyConfig.copy(autoBlurOnBackground = it)) },
        )
        Text("全图模糊快捷手势", color = palette.muted, fontSize = 11.sp)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            ShortcutGestureMode.entries.forEach { gesture ->
                FilterChip(
                    selected = privacyConfig.blurShortcut == gesture,
                    onClick = { onUpdatePrivacyConfig(privacyConfig.copy(blurShortcut = gesture)) },
                    label = { Text(gesture.label, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = palette.primary,
                        selectedLabelColor = palette.onPrimary,
                    ),
                )
            }
        }

        HorizontalDivider(color = palette.line)

        Text("老板键（飞书云文档《Q4 项目复盘》）盲操触发", color = palette.muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        StealthToggleRow(
            title = "无弹窗时连滑两次系统返回",
            subtitle = "450ms 内连滑两次返回即切入云文档（对应网页双击 Esc）",
            checked = privacyConfig.bossDoubleBack,
            onCheckedChange = { onUpdatePrivacyConfig(privacyConfig.copy(bossDoubleBack = it)) },
        )
        StealthToggleRow(
            title = "手机屏幕朝下扣放桌面自动切入云文档",
            subtitle = "传感器检测到翻转扣屏立即掩护，再拿起即为云文档",
            checked = privacyConfig.bossFlipFaceDown,
            onCheckedChange = { onUpdatePrivacyConfig(privacyConfig.copy(bossFlipFaceDown = it)) },
        )
        StealthToggleRow(
            title = "切后台 / 锁屏恢复时自动挂起云文档",
            subtitle = "切换应用或息屏时自动定格为云文档页面",
            checked = privacyConfig.bossOnBackgroundResume,
            onCheckedChange = { onUpdatePrivacyConfig(privacyConfig.copy(bossOnBackgroundResume = it)) },
        )
        Text("双击侧面音量键切入云文档", color = palette.muted, fontSize = 11.sp)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            listOf(
                ShortcutGestureMode.NONE,
                ShortcutGestureMode.VOL_DOUBLE_DOWN,
                ShortcutGestureMode.VOL_DOUBLE_UP,
            ).forEach { gesture ->
                FilterChip(
                    selected = privacyConfig.bossVolumeShortcut == gesture,
                    onClick = { onUpdatePrivacyConfig(privacyConfig.copy(bossVolumeShortcut = gesture)) },
                    label = { Text(gesture.label, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = palette.primary,
                        selectedLabelColor = palette.onPrimary,
                    ),
                )
            }
        }

        HorizontalDivider(color = palette.line)

        Text("桌面启动器图标伪装（切换后桌面数秒内刷新）", color = palette.muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AppIconAlias.entries.forEach { alias ->
                FilterChip(
                    selected = activeIconAlias == alias,
                    onClick = {
                        switchLauncherAlias(context, alias)
                        onChangeIconAlias(alias)
                    },
                    label = { Text(alias.title, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = palette.primary,
                        selectedLabelColor = palette.onPrimary,
                    ),
                )
            }
        }
        StealthToggleRow(
            title = "防截屏 / 后台任务预览隐私模糊",
            subtitle = "开启 FLAG_SECURE，切换后台时不显示真实内容缩略图",
            checked = privacyShieldEnabled,
            onCheckedChange = onTogglePrivacyShield,
        )
    }
}

@Composable
private fun StealthToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val palette = LocalForHappyPalette.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = palette.text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = palette.muted, fontSize = 11.sp, lineHeight = 15.sp)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun switchLauncherAlias(context: Context, selected: AppIconAlias) {
    val pm = context.packageManager
    val pkg = context.packageName
    for (alias in AppIconAlias.entries) {
        val state = if (alias == selected) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }
        runCatching {
            pm.setComponentEnabledSetting(
                ComponentName(pkg, alias.aliasClass),
                state,
                PackageManager.DONT_KILL_APP,
            )
        }
    }
}
