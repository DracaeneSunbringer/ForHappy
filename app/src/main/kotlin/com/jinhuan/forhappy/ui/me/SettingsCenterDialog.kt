package com.jinhuan.forhappy.ui.me

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jinhuan.forhappy.BuildConfig
import com.jinhuan.forhappy.ForHappyApp
import com.jinhuan.forhappy.core.chat.ChatSessionController
import com.jinhuan.forhappy.core.model.ACCENT_PRESETS
import com.jinhuan.forhappy.core.model.AccentColorConfig
import com.jinhuan.forhappy.core.model.AccentColorMode
import com.jinhuan.forhappy.core.model.AppDisguiseMode
import com.jinhuan.forhappy.core.model.AppIconAlias
import com.jinhuan.forhappy.core.model.AppThemeMode
import com.jinhuan.forhappy.core.model.AppUiStyle
import com.jinhuan.forhappy.core.model.ChatScrollPolicy
import com.jinhuan.forhappy.core.model.CustomEdgeNode
import com.jinhuan.forhappy.core.model.MeUser
import com.jinhuan.forhappy.core.model.PrivacyStealthConfig
import com.jinhuan.forhappy.core.navigation.StealthExemptionGate
import com.jinhuan.forhappy.core.network.api.ForHappyApiClient
import com.jinhuan.forhappy.core.update.AppUpdateController
import com.jinhuan.forhappy.core.update.AppUpdateManager
import com.jinhuan.forhappy.core.update.UpdateStatus
import com.jinhuan.forhappy.ui.components.BadgePill
import com.jinhuan.forhappy.ui.components.ForHappyTextField
import com.jinhuan.forhappy.ui.disguise.applyLauncherIconAlias
import com.jinhuan.forhappy.ui.disguise.previewDrawableRes
import com.jinhuan.forhappy.ui.theme.ForHappyDimens
import com.jinhuan.forhappy.ui.theme.LocalForHappyPalette
import com.jinhuan.forhappy.ui.theme.colorToHsv
import com.jinhuan.forhappy.ui.theme.formatColorHex
import com.jinhuan.forhappy.ui.theme.hsvToColor
import com.jinhuan.forhappy.ui.theme.parseHexColorOrNull
import com.jinhuan.forhappy.ui.theme.relativeLuminance
import com.jinhuan.forhappy.ui.wall.ConfigureFullscreenDialogWindow
import com.jinhuan.forhappy.ui.wall.DialogBackOwnerScope
import com.jinhuan.forhappy.ui.wall.FullscreenDialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

@Composable
fun SettingsCenterDialog(
    api: ForHappyApiClient,
    updateController: AppUpdateController,
    me: MeUser?,
    chatController: ChatSessionController? = null,
    themeMode: AppThemeMode,
    uiStyle: AppUiStyle = AppUiStyle.MODERN,
    accentConfig: AccentColorConfig = AccentColorConfig(),
    privacyConfig: PrivacyStealthConfig,
    privacyShieldEnabled: Boolean,
    activeIconAlias: AppIconAlias,
    initialSubpage: String? = null,
    onChangeTheme: (AppThemeMode) -> Unit,
    onChangeUiStyle: (AppUiStyle) -> Unit = {},
    onChangeAccentConfig: (AccentColorConfig) -> Unit = {},
    onChangeDisguise: (AppDisguiseMode) -> Unit,
    onUpdatePrivacyConfig: (PrivacyStealthConfig) -> Unit,
    onTogglePrivacyShield: (Boolean) -> Unit,
    onChangeIconAlias: (AppIconAlias) -> Unit,
    onOpenNotifSettings: () -> Unit,
    onOpenInvites: () -> Unit,
    onOpenWebRoute: (route: String, title: String) -> Unit,
    onRequireLogin: () -> Unit,
    onLoggedOut: () -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalForHappyPalette.current
    var currentSubpage by remember(initialSubpage) { mutableStateOf(initialSubpage) }
    val chatStateOrNull = chatController?.uiState?.collectAsState()?.value

    val subpageTitle = when (currentSubpage) {
        "stealth" -> "防窥与摸鱼"
        "browse" -> "浏览默认偏好"
        "chat" -> "群聊与私信"
        "appearance" -> "外观与背景"
        "account" -> "账号与安全"
        "ai" -> "AI 小伙伴"
        "network" -> "网络加速与诊断"
        "storage" -> "存储与缓存"
        "about" -> "关于与更新"
        else -> "设置"
    }

    Dialog(
        onDismissRequest = {
            if (currentSubpage != null) currentSubpage = null else onDismiss()
        },
        properties = FullscreenDialogProperties,
    ) {
        ConfigureFullscreenDialogWindow(darkStatusIcons = !palette.isDark)
        DialogBackOwnerScope {
            BackHandler(enabled = currentSubpage != null) {
                currentSubpage = null
            }
        val navBottomPad = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.bg)
                .imePadding(),
        ) {
            Surface(color = if (palette.isModernUi) palette.glassSurface else palette.surface) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .height(if (palette.isModernUi) 52.dp else ForHappyDimens.topBarHeight)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = {
                            if (currentSubpage != null) {
                                currentSubpage = null
                            } else {
                                onDismiss()
                            }
                        },
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回", tint = palette.text)
                    }
                    Text(
                        subpageTitle,
                        color = palette.text,
                        fontSize = if (palette.isModernUi) 18.sp else 16.sp,
                        fontWeight = if (palette.isModernUi) FontWeight.Bold else FontWeight.SemiBold,
                    )
                }
            }
            HorizontalDivider(
                color = if (palette.isModernUi) palette.glassBorder else palette.line,
                thickness = if (palette.isModernUi) 0.5.dp else 1.dp,
            )

            if (currentSubpage == null) {
                val netStatus by api.rustNetManager.status.collectAsState()
                val updateState by updateController.state.collectAsState()
                val isAdminCapable = me?.isAdmin == true || me?.owner == true || me?.canManageAi == true
                val chatSubtitle = if (chatStateOrNull != null) {
                    buildString {
                        append(if (chatStateOrNull.dmEnabled) "私信已开启" else "私信已关闭")
                        if (chatStateOrNull.swipeSwitchGroups) append(" · 左右滑切群")
                        append(" · 滚动与干净铭牌")
                    }
                } else {
                    "消息滚动策略、私信开关与左右滑切群"
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 14.dp,
                        top = 14.dp,
                        end = 14.dp,
                        bottom = 14.dp + navBottomPad,
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        SettingsGroupHeader("常用")
                        SettingsGroupCard {
                            SettingsNavRow(
                                icon = Icons.Rounded.Security,
                                title = "防窥与摸鱼",
                                subtitle = "${privacyConfig.disguiseMode.label} · 全图模糊${if (privacyConfig.blurAllEnabled) "已开" else "已关"}",
                                badgeText = "10秒演练",
                                onClick = { currentSubpage = "stealth" },
                            )
                            HorizontalDivider(color = palette.line.copy(alpha = 0.6f))
                            SettingsNavRow(
                                icon = Icons.Rounded.Tune,
                                title = "浏览默认偏好",
                                subtitle = "默认排序、双列/单列视图与按日分组",
                                onClick = { currentSubpage = "browse" },
                            )
                            HorizontalDivider(color = palette.line.copy(alpha = 0.6f))
                            SettingsNavRow(
                                icon = Icons.Rounded.Tune,
                                title = "群聊与私信",
                                subtitle = chatSubtitle,
                                onClick = { currentSubpage = "chat" },
                            )
                            HorizontalDivider(color = palette.line.copy(alpha = 0.6f))
                            SettingsNavRow(
                                icon = Icons.Rounded.Notifications,
                                title = "提醒偏好",
                                subtitle = "互动、群聊、新帖与系统通知开关",
                                onClick = {
                                    onDismiss()
                                    onOpenNotifSettings()
                                },
                            )
                            HorizontalDivider(color = palette.line.copy(alpha = 0.6f))
                            SettingsNavRow(
                                icon = Icons.Rounded.Palette,
                                title = "外观与图标",
                                subtitle = "${uiStyle.label} · ${accentConfig.activeDisplayLabel()} · ${activeIconAlias.title}",
                                onClick = { currentSubpage = "appearance" },
                            )
                        }
                    }

                    item {
                        SettingsGroupHeader("账号")
                        SettingsGroupCard {
                            SettingsNavRow(
                                icon = Icons.Rounded.Person,
                                title = "账号与安全",
                                subtitle = me?.let { "@${it.username} · TL${it.trustLevel}" } ?: "未登录 · 点击登录或查看账号",
                                onClick = { currentSubpage = "account" },
                            )
                        }
                    }

                    item {
                        SettingsGroupHeader("应用")
                        SettingsGroupCard {
                            SettingsNavRow(
                                icon = Icons.Rounded.AutoAwesome,
                                title = "AI 小伙伴",
                                subtitle = "自有模型接入、书房续写与评论偏好",
                                onClick = { currentSubpage = "ai" },
                            )
                            HorizontalDivider(color = palette.line.copy(alpha = 0.6f))
                            SettingsNavRow(
                                icon = Icons.Rounded.Speed,
                                title = "网络加速与诊断",
                                subtitle = if (netStatus.running) "已就绪 · ${netStatus.latencyMs ?: "--"}ms" else "连接中 · 点此诊断与复制日志",
                                onClick = { currentSubpage = "network" },
                            )
                            HorizontalDivider(color = palette.line.copy(alpha = 0.6f))
                            SettingsNavRow(
                                icon = Icons.Rounded.Storage,
                                title = "存储与缓存",
                                subtitle = "静态网页、媒体缓存与本地草稿清理",
                                onClick = { currentSubpage = "storage" },
                            )
                            HorizontalDivider(color = palette.line.copy(alpha = 0.6f))
                            SettingsNavRow(
                                icon = Icons.Rounded.SystemUpdate,
                                title = "关于与更新",
                                subtitle = "v${BuildConfig.VERSION_NAME}",
                                badgeText = if (updateState.status == UpdateStatus.AVAILABLE || updateState.status == UpdateStatus.READY_TO_INSTALL) "新版本" else null,
                                onClick = { currentSubpage = "about" },
                            )
                        }
                    }

                    if (isAdminCapable) {
                        item {
                            SettingsGroupHeader("管理")
                            SettingsGroupCard {
                                SettingsNavRow(
                                    icon = Icons.Rounded.AdminPanelSettings,
                                    title = "管理后台",
                                    subtitle = "站务、审计与 AI 管理控制台",
                                    onClick = {
                                        onDismiss()
                                        onOpenWebRoute("/admin", "管理后台")
                                    },
                                )
                            }
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(
                            start = 14.dp,
                            top = 14.dp,
                            end = 14.dp,
                            bottom = 14.dp + navBottomPad,
                        ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    when (currentSubpage) {
                        "stealth" -> StealthAndPracticeSubpage(
                            themeMode = themeMode,
                            uiStyle = uiStyle,
                            privacyConfig = privacyConfig,
                            privacyShieldEnabled = privacyShieldEnabled,
                            activeIconAlias = activeIconAlias,
                            onChangeTheme = onChangeTheme,
                            onChangeDisguise = onChangeDisguise,
                            onUpdatePrivacyConfig = onUpdatePrivacyConfig,
                            onTogglePrivacyShield = onTogglePrivacyShield,
                            onChangeIconAlias = onChangeIconAlias,
                        )
                        "browse" -> BrowseDefaultsSubpage(api = api)
                        "chat" -> ChatPrefsSubpage(api = api, chatController = chatController)
                        "appearance" -> AppearanceSubpage(
                            api = api,
                            themeMode = themeMode,
                            uiStyle = uiStyle,
                            accentConfig = accentConfig,
                            activeIconAlias = activeIconAlias,
                            onChangeTheme = onChangeTheme,
                            onChangeUiStyle = onChangeUiStyle,
                            onChangeAccentConfig = onChangeAccentConfig,
                            onChangeIconAlias = onChangeIconAlias,
                        )
                        "account" -> AccountSecuritySubpage(
                            api = api,
                            me = me,
                            onOpenInvites = {
                                onDismiss()
                                onOpenInvites()
                            },
                            onRequireLogin = {
                                onDismiss()
                                onRequireLogin()
                            },
                            onLoggedOut = {
                                onLoggedOut()
                                onDismiss()
                            },
                        )
                        "ai" -> AiPrefsSubpage(api = api, me = me, onRequireLogin = onRequireLogin)
                        "network" -> NetworkDiagnosticsSubpage(api = api)
                        "storage" -> StorageCacheSubpage(api = api)
                        "about" -> AboutUpdateSubpage(updateController = updateController)
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun StealthAndPracticeSubpage(
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
    var practiceActive by remember { mutableStateOf(false) }
    var practiceSec by remember { mutableIntStateOf(0) }
    var practiceBlur by remember { mutableStateOf(true) }
    var practiceBossCover by remember { mutableStateOf(false) }

    LaunchedEffect(practiceActive) {
        if (practiceActive) {
            practiceSec = 10
            while (practiceSec > 0) {
                delay(1000L)
                practiceSec--
            }
            practiceActive = false
            practiceBossCover = false
        }
    }

    // 10-Second Interactive Stealth Practice Sandbox
    Surface(
        color = palette.surface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, palette.primary.copy(alpha = 0.45f), RoundedCornerShape(16.dp)),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("10 秒防窥演练", color = palette.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text("安全沙盒内试用糊图与老板键遮罩，退出不修改真实状态", color = palette.muted, fontSize = 11.sp)
                }
                if (practiceActive) {
                    BadgePill("${practiceSec}s", containerColor = palette.primary, contentColor = palette.onPrimary)
                }
            }

            if (!practiceActive) {
                Button(
                    onClick = {
                        practiceBlur = true
                        practiceBossCover = false
                        practiceActive = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = palette.primary, contentColor = palette.onPrimary),
                ) {
                    Text("开始 10 秒防窥演练")
                }
            } else {
                Surface(
                    color = if (practiceBossCover) Color(0xFFFFFFFF) else palette.surface2,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, palette.line, RoundedCornerShape(12.dp)),
                ) {
                    if (practiceBossCover) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { practiceBossCover = false }
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text("飞书云文档 · Q4 项目复盘（演练遮罩）", color = Color(0xFF1F2329), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("1. 核心指标达成情况综述\n2. 跨团队协同节奏与风险项跟踪…", color = Color(0xFF646A73), fontSize = 12.sp)
                            Text("点按此处解除演练遮罩", color = Color(0xFF3370FF), fontSize = 11.sp)
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(palette.primary.copy(alpha = 0.25f))
                                    .then(if (practiceBlur) Modifier.blur(12.dp) else Modifier)
                                    .clickable { practiceBlur = !practiceBlur },
                            ) {
                                Text("🖼️", fontSize = 26.sp)
                            }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = if (practiceBlur) "已全图模糊 · 点左侧图片看清" else "清晰态 · 再点重新模糊",
                                    color = palette.text,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = { practiceBlur = !practiceBlur }, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)) {
                                        Text(if (practiceBlur) "点清" else "糊回", fontSize = 11.sp)
                                    }
                                    OutlinedButton(onClick = { practiceBossCover = true }, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)) {
                                        Text("模拟老板键", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
                TextButton(
                    onClick = {
                        practiceActive = false
                        practiceBossCover = false
                    },
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text("结束演练", fontSize = 12.sp)
                }
            }
        }
    }

    SettingsGroupCard {
        Box(modifier = Modifier.padding(14.dp)) {
            PrivacyStealthSettingsContent(
                themeMode = themeMode,
                uiStyle = uiStyle,
                privacyConfig = privacyConfig,
                privacyShieldEnabled = privacyShieldEnabled,
                activeIconAlias = activeIconAlias,
                onChangeTheme = onChangeTheme,
                onChangeDisguise = onChangeDisguise,
                onUpdatePrivacyConfig = onUpdatePrivacyConfig,
                onTogglePrivacyShield = onTogglePrivacyShield,
                onChangeIconAlias = onChangeIconAlias,
            )
        }
    }
}

@Composable
private fun BrowseDefaultsSubpage(api: ForHappyApiClient) {
    val palette = LocalForHappyPalette.current
    val scope = rememberCoroutineScope()
    var defaultSort by remember { mutableStateOf("new") }
    var defaultView by remember { mutableStateOf("dual") }
    var byDayDefault by remember { mutableStateOf(true) }
    var savedBanner by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        defaultSort = api.getPref("ui.default_sort") ?: "new"
        defaultView = api.getPref("ui.default_view") ?: "dual"
        byDayDefault = api.getPref("ui.default_by_day") != "false"
    }

    SettingsGroupCard {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("默认排序", color = palette.muted, fontSize = 12.sp)
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("new" to "最新", "hot" to "最热", "top" to "最多喜欢", "old" to "最早").forEach { (id, label) ->
                    FilterChip(
                        selected = defaultSort == id,
                        onClick = {
                            defaultSort = id
                            scope.launch { api.putPref("ui.default_sort", id); savedBanner = "已保存默认排序" }
                        },
                        label = { Text(label, fontSize = 12.sp) },
                    )
                }
            }
            Text("默认布局", color = palette.muted, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("dual" to "双列瀑布流", "single" to "单列大图", "pager" to "上下沉浸刷").forEach { (id, label) ->
                    FilterChip(
                        selected = defaultView == id,
                        onClick = {
                            defaultView = id
                            scope.launch { api.putPref("ui.default_view", id); savedBanner = "已保存默认布局" }
                        },
                        label = { Text(label, fontSize = 12.sp) },
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("默认按自然日分段", color = palette.text, fontSize = 13.sp)
                Switch(
                    checked = byDayDefault,
                    onCheckedChange = {
                        byDayDefault = it
                        scope.launch { api.putPref("ui.default_by_day", it.toString()); savedBanner = "已更新分段设置" }
                    },
                )
            }
            savedBanner?.let { Text(it, color = palette.primary, fontSize = 12.sp) }
        }
    }
}

@Composable
private fun ChatPrefsSubpage(
    api: ForHappyApiClient,
    chatController: ChatSessionController?,
) {
    val palette = LocalForHappyPalette.current
    val scope = rememberCoroutineScope()
    val controllerState = chatController?.uiState?.collectAsState()?.value

    var localDmEnabled by remember { mutableStateOf(true) }
    var localSwipeSwitch by remember { mutableStateOf(false) }
    var localScrollPolicy by remember { mutableStateOf(ChatScrollPolicy.SMART) }
    var localCleanMode by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (chatController == null) {
            localDmEnabled = api.getPref(ChatSessionController.KV_DM_ENABLED)?.let { it != "0" && it != "false" } ?: true
            localSwipeSwitch = api.getPref(ChatSessionController.KV_SWIPE_SWITCH_GROUPS)?.let { it == "1" || it == "true" } ?: false
            localScrollPolicy = ChatScrollPolicy.fromKey(api.getPref("ui.chat.scroll_policy"))
            localCleanMode = api.getPref("ui.chat.clean_mode") == "true"
        }
    }

    val dmEnabled = controllerState?.dmEnabled ?: localDmEnabled
    val swipeSwitchGroups = controllerState?.swipeSwitchGroups ?: localSwipeSwitch
    val scrollPolicy = controllerState?.scrollPolicy ?: localScrollPolicy
    val cleanMode = controllerState?.cleanDecoMode ?: localCleanMode

    SettingsGroupCard {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("开启私信功能", color = palette.text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text(
                        "关闭后隐藏私信入口与未读数；仅 1 个群聊（或开启左右滑切群）时点「消息」直接进入群聊，无需进入群聊选择界面",
                        color = palette.muted,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                    )
                }
                Switch(
                    checked = dmEnabled,
                    onCheckedChange = { enabled ->
                        localDmEnabled = enabled
                        if (chatController != null) {
                            chatController.setDmEnabled(enabled)
                        } else {
                            scope.launch { api.putPref(ChatSessionController.KV_DM_ENABLED, if (enabled) "1" else "0") }
                        }
                    },
                )
            }

            HorizontalDivider(color = palette.line.copy(alpha = 0.6f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("左右滑动快速切换群聊", color = palette.text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text(
                        "拥有多个群聊时，在聊天界面左右滑动即可快速切群（顶部同步显示群切换胶囊）；配合关闭私信可直接免选列表",
                        color = palette.muted,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                    )
                }
                Switch(
                    checked = swipeSwitchGroups,
                    onCheckedChange = { enabled ->
                        localSwipeSwitch = enabled
                        if (chatController != null) {
                            chatController.setSwipeSwitchGroups(enabled)
                        } else {
                            scope.launch { api.putPref(ChatSessionController.KV_SWIPE_SWITCH_GROUPS, if (enabled) "1" else "0") }
                        }
                    },
                )
            }

            HorizontalDivider(color = palette.line.copy(alpha = 0.6f))

            Text("新消息到来时的自动滚动行为", color = palette.muted, fontSize = 12.sp)
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    ChatScrollPolicy.SMART to "智能定位",
                    ChatScrollPolicy.AUTO_FOLLOW to "始终回底",
                    ChatScrollPolicy.MANUAL to "保持当前位置",
                ).forEach { (policy, label) ->
                    FilterChip(
                        selected = scrollPolicy == policy,
                        onClick = {
                            localScrollPolicy = policy
                            chatController?.setScrollPolicy(policy)
                            scope.launch { api.putPref("ui.chat.scroll_policy", policy.key) }
                        },
                        label = { Text(label, fontSize = 12.sp) },
                    )
                }
            }

            HorizontalDivider(color = palette.line.copy(alpha = 0.6f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("群聊极简干净模式", color = palette.text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text("隐藏花哨称号与装扮气泡背景", color = palette.muted, fontSize = 11.sp)
                }
                Switch(
                    checked = cleanMode,
                    onCheckedChange = { enabled ->
                        localCleanMode = enabled
                        chatController?.setCleanDecoMode(enabled)
                        scope.launch { api.putPref("ui.chat.clean_mode", enabled.toString()) }
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppearanceSubpage(
    api: ForHappyApiClient,
    themeMode: AppThemeMode,
    uiStyle: AppUiStyle,
    accentConfig: AccentColorConfig,
    activeIconAlias: AppIconAlias,
    onChangeTheme: (AppThemeMode) -> Unit,
    onChangeUiStyle: (AppUiStyle) -> Unit,
    onChangeAccentConfig: (AccentColorConfig) -> Unit,
    onChangeIconAlias: (AppIconAlias) -> Unit,
) {
    val palette = LocalForHappyPalette.current
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val scope = rememberCoroutineScope()
    var wallpaperMode by remember { mutableStateOf("solid") }
    var vignetteStrength by remember { mutableFloatStateOf(0.25f) }

    val initialSeedColor = remember(accentConfig.mode, accentConfig.presetId, accentConfig.customHex) {
        parseHexColorOrNull(accentConfig.resolvedPresetOrCustomHex()) ?: Color(0xFF4F46E5)
    }
    val initialHsv = remember(initialSeedColor) { colorToHsv(initialSeedColor) }
    var pickerHue by remember { mutableFloatStateOf(initialHsv[0]) }
    var pickerSat by remember { mutableFloatStateOf(initialHsv[1].coerceAtLeast(0.15f)) }
    var pickerVal by remember { mutableFloatStateOf(initialHsv[2].coerceAtLeast(0.25f)) }
    var hexInput by remember { mutableStateOf(formatColorHex(initialSeedColor)) }

    LaunchedEffect(accentConfig.mode, accentConfig.presetId, accentConfig.customHex) {
        val resolved = parseHexColorOrNull(accentConfig.resolvedPresetOrCustomHex())
        if (resolved != null) {
            val hsv = colorToHsv(resolved)
            if (hsv[1] > 0.02f) pickerHue = hsv[0]
            pickerSat = hsv[1]
            pickerVal = hsv[2]
            val formatted = formatColorHex(resolved)
            if (!hexInput.equals(formatted, ignoreCase = true)) {
                hexInput = formatted
            }
        }
    }

    LaunchedEffect(Unit) {
        wallpaperMode = api.getPref("ui.appearance.bg_mode") ?: "solid"
        vignetteStrength = api.getPref("ui.appearance.vignette")?.toFloatOrNull() ?: 0.25f
    }

    val dynamicPreviewColor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val useDarkForDynamic = when (themeMode) {
            AppThemeMode.WARM_PAPER_LIGHT -> false
            AppThemeMode.PINE_NIGHT_DARK -> true
            else -> systemDark
        }
        if (useDarkForDynamic) dynamicDarkColorScheme(context).primary else dynamicLightColorScheme(context).primary
    } else {
        null
    }

    fun commitCustomHsv(nextH: Float, nextS: Float, nextV: Float) {
        pickerHue = nextH.coerceIn(0f, 360f)
        pickerSat = nextS.coerceIn(0f, 1f)
        pickerVal = nextV.coerceIn(0.18f, 1f)
        val color = hsvToColor(pickerHue, pickerSat, pickerVal)
        val formatted = formatColorHex(color)
        hexInput = formatted
        onChangeAccentConfig(
            accentConfig.copy(
                mode = AccentColorMode.CUSTOM,
                customHex = formatted,
            ),
        )
    }

    // 1. Custom Accent Color Card
    SettingsGroupCard {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("主题强调色", color = palette.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "作用于按钮、导航高亮与我的气泡；自动适配日夜对比度，不影响伪装模式",
                        color = palette.muted,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(palette.primary)
                        .border(1.5.dp, palette.line, CircleShape),
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                FilterChip(
                    selected = accentConfig.mode == AccentColorMode.DEFAULT,
                    onClick = { onChangeAccentConfig(accentConfig.copy(mode = AccentColorMode.DEFAULT)) },
                    label = { Text("主题默认", fontSize = 12.sp) },
                )
                if (dynamicPreviewColor != null) {
                    FilterChip(
                        selected = accentConfig.mode == AccentColorMode.DYNAMIC,
                        onClick = { onChangeAccentConfig(accentConfig.copy(mode = AccentColorMode.DYNAMIC)) },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(dynamicPreviewColor)
                                    .border(0.5.dp, palette.line, CircleShape),
                            )
                        },
                        label = { Text("壁纸取色", fontSize = 12.sp) },
                    )
                }
                FilterChip(
                    selected = accentConfig.mode == AccentColorMode.CUSTOM,
                    onClick = {
                        val baseColor = parseHexColorOrNull(accentConfig.resolvedPresetOrCustomHex()) ?: palette.primary
                        val formatted = formatColorHex(baseColor)
                        hexInput = formatted
                        onChangeAccentConfig(accentConfig.copy(mode = AccentColorMode.CUSTOM, customHex = formatted))
                    },
                    label = { Text("自由取色", fontSize = 12.sp) },
                )
            }

            Text("精选色板", color = palette.muted, fontSize = 12.sp)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ACCENT_PRESETS.chunked(5).forEach { rowPresets ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        rowPresets.forEach { preset ->
                            val swatchColor = remember(preset.hex) {
                                parseHexColorOrNull(preset.hex) ?: Color(0xFF4A6B8A)
                            }
                            val isSelected = accentConfig.mode == AccentColorMode.PRESET && accentConfig.presetId == preset.id
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        onChangeAccentConfig(
                                            AccentColorConfig(
                                                mode = AccentColorMode.PRESET,
                                                presetId = preset.id,
                                                customHex = preset.hex,
                                            ),
                                        )
                                    }
                                    .padding(vertical = 4.dp),
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(34.dp)
                                        .then(
                                            if (isSelected) {
                                                Modifier
                                                    .border(2.dp, palette.text, CircleShape)
                                                    .padding(3.dp)
                                            } else {
                                                Modifier
                                            },
                                        )
                                        .clip(CircleShape)
                                        .background(swatchColor),
                                ) {
                                    if (isSelected) {
                                        val checkTint = if (relativeLuminance(swatchColor) > 0.45f) Color(0xFF0F172A) else Color.White
                                        Icon(
                                            Icons.Rounded.Check,
                                            contentDescription = preset.label,
                                            tint = checkTint,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                }
                                Text(
                                    text = preset.label,
                                    color = if (isSelected) palette.primary else palette.muted,
                                    fontSize = 10.5.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }

            // Live Accent Preview Strip
            Surface(
                color = palette.surface2.copy(alpha = 0.72f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        color = palette.primary,
                        shape = RoundedCornerShape(999.dp),
                    ) {
                        Text(
                            "主按钮",
                            color = palette.onPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                    Surface(
                        color = palette.primaryContainer,
                        shape = RoundedCornerShape(999.dp),
                    ) {
                        Text(
                            "#选中态",
                            color = palette.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Surface(
                        color = palette.bubbleSelf,
                        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 4.dp, bottomEnd = 12.dp, bottomStart = 12.dp),
                    ) {
                        Text(
                            "我的气泡",
                            color = palette.text,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        )
                    }
                }
            }

            if (accentConfig.mode == AccentColorMode.CUSTOM) {
                val activeCustomColor = remember(pickerHue, pickerSat, pickerVal) {
                    hsvToColor(pickerHue, pickerSat, pickerVal)
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HsvGradientSliderRow(
                        label = "色相 H",
                        valueText = "${pickerHue.toInt()}°",
                        value = pickerHue,
                        valueRange = 0f..360f,
                        trackColors = listOf(
                            Color(0xFFFF0000),
                            Color(0xFFFFFF00),
                            Color(0xFF00FF00),
                            Color(0xFF00FFFF),
                            Color(0xFF0000FF),
                            Color(0xFFFF00FF),
                            Color(0xFFFF0000),
                        ),
                        thumbColor = hsvToColor(pickerHue, 1f, 1f),
                        onValueChange = { commitCustomHsv(it, pickerSat, pickerVal) },
                    )
                    HsvGradientSliderRow(
                        label = "饱和度 S",
                        valueText = "${(pickerSat * 100).toInt()}%",
                        value = pickerSat,
                        valueRange = 0f..1f,
                        trackColors = listOf(
                            hsvToColor(pickerHue, 0f, pickerVal),
                            hsvToColor(pickerHue, 1f, pickerVal),
                        ),
                        thumbColor = activeCustomColor,
                        onValueChange = { commitCustomHsv(pickerHue, it, pickerVal) },
                    )
                    HsvGradientSliderRow(
                        label = "明度 V",
                        valueText = "${(pickerVal * 100).toInt()}%",
                        value = pickerVal,
                        valueRange = 0.18f..1f,
                        trackColors = listOf(
                            hsvToColor(pickerHue, pickerSat, 0.18f),
                            hsvToColor(pickerHue, pickerSat, 1f),
                        ),
                        thumbColor = activeCustomColor,
                        onValueChange = { commitCustomHsv(pickerHue, pickerSat, it) },
                    )

                    val hexValid = parseHexColorOrNull(hexInput) != null
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(activeCustomColor)
                                .border(1.5.dp, palette.line, CircleShape),
                        )
                        ForHappyTextField(
                            value = hexInput,
                            onValueChange = { raw ->
                                val filtered = buildString {
                                    for (ch in raw.take(7)) {
                                        if (ch == '#' && isEmpty()) {
                                            append(ch)
                                        } else if (ch in '0'..'9' || ch in 'a'..'f' || ch in 'A'..'F') {
                                            append(ch.uppercaseChar())
                                        }
                                    }
                                }
                                hexInput = filtered
                                val parsed = parseHexColorOrNull(filtered)
                                if (parsed != null) {
                                    val hsv = colorToHsv(parsed)
                                    if (hsv[1] > 0.02f) pickerHue = hsv[0]
                                    pickerSat = hsv[1]
                                    pickerVal = hsv[2].coerceAtLeast(0.18f)
                                    val normalized = formatColorHex(parsed)
                                    onChangeAccentConfig(
                                        accentConfig.copy(
                                            mode = AccentColorMode.CUSTOM,
                                            customHex = normalized,
                                        ),
                                    )
                                }
                            },
                            placeholder = "#4A6B8A",
                            singleLine = true,
                            isError = hexInput.isNotBlank() && !hexValid,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(
                            onClick = {
                                onChangeAccentConfig(accentConfig.copy(mode = AccentColorMode.DEFAULT))
                            },
                        ) {
                            Text("恢复默认", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    // 2. Minimalist Geometric Launcher Icon Switcher Card
    SettingsGroupCard {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("桌面应用图标", color = palette.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "默认采用尽欢原站标志（灰蓝换色），也可切换极简几何图标（伪装图标在「防窥与摸鱼」中）",
                color = palette.muted,
                fontSize = 11.sp,
                lineHeight = 15.sp,
            )
            if (activeIconAlias.isDisguise) {
                Surface(
                    color = palette.primaryContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        "当前桌面为伪装图标「${activeIconAlias.title}」，点击下方任一图标即可切回尽欢",
                        color = palette.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppIconAlias.jinhuanEntries.chunked(2).forEach { rowIcons ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        rowIcons.forEach { alias ->
                            val selected = activeIconAlias == alias
                            val cardShape = RoundedCornerShape(14.dp)
                            Surface(
                                color = if (selected) palette.primaryContainer else palette.surface2.copy(alpha = 0.72f),
                                shape = cardShape,
                                modifier = Modifier
                                    .weight(1f)
                                    .border(
                                        width = if (selected) 1.5.dp else 1.dp,
                                        color = if (selected) palette.primary else palette.line.copy(alpha = 0.55f),
                                        shape = cardShape,
                                    )
                                    .clip(cardShape)
                                    .clickable {
                                        applyLauncherIconAlias(context, alias)
                                        onChangeIconAlias(alias)
                                    },
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                                ) {
                                    Image(
                                        painter = painterResource(alias.previewDrawableRes()),
                                        contentDescription = alias.title,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(11.dp)),
                                    )
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(1.dp),
                                    ) {
                                        Text(
                                            text = alias.title,
                                            color = if (selected) palette.primary else palette.text,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = alias.subtitle,
                                            color = palette.muted,
                                            fontSize = 10.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                        }
                        if (rowIcons.size == 1) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }

    // 3. Visual Style Card
    SettingsGroupCard {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("界面视觉风格", color = palette.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text("切换应用默认原生视觉或怀旧原站风格（不影响飞书 / LINUX DO 伪装模式）", color = palette.muted, fontSize = 11.sp)
            AppUiStyle.entries.forEach { style ->
                val selected = uiStyle == style
                val swatches = when (style) {
                    AppUiStyle.MODERN -> listOf(
                        Color(0xFFF3F6F9),
                        Color(0xFF0B0E13),
                        if (uiStyle == AppUiStyle.MODERN) palette.primary else Color(0xFF4A6B8A),
                        if (uiStyle == AppUiStyle.MODERN) palette.accent else Color(0xFF688CAE),
                    )
                    AppUiStyle.CLASSIC_WEB -> listOf(
                        Color(0xFFF7F6F0),
                        Color(0xFF14261F),
                        if (uiStyle == AppUiStyle.CLASSIC_WEB) palette.primary else Color(0xFF1F6B4F),
                        Color(0xFF5A9A7C),
                    )
                }
                val optionShape = RoundedCornerShape(14.dp)
                Surface(
                    color = if (selected) palette.primaryContainer else palette.surface2.copy(alpha = 0.7f),
                    shape = optionShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (selected) {
                                Modifier.border(1.5.dp, palette.primary, optionShape)
                            } else {
                                Modifier
                            },
                        )
                        .clip(optionShape)
                        .clickable { onChangeUiStyle(style) },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            swatches.forEach { sw ->
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(sw)
                                        .border(0.5.dp, palette.glassBorder, CircleShape),
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(style.label, color = palette.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(style.subtitle, color = palette.muted, fontSize = 11.sp, lineHeight = 15.sp)
                        }
                        if (selected) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = "当前启用", tint = palette.primary, modifier = Modifier.size(19.dp))
                        }
                    }
                }
            }
        }
    }

    // 4. Day/Night & Background Card
    SettingsGroupCard {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("日夜主题", color = palette.muted, fontSize = 12.sp)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                AppThemeMode.dayNightEntries.forEach { mode ->
                    FilterChip(
                        selected = themeMode == mode,
                        onClick = { onChangeTheme(mode) },
                        label = { Text(mode.displayLabel(uiStyle), fontSize = 12.sp) },
                    )
                }
            }
            HorizontalDivider(color = palette.line)
            Text("背景风格", color = palette.muted, fontSize = 12.sp)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                listOf("solid" to "纯色宣纸/松夜", "wallpaper" to "柔光质感壁纸").forEach { (id, label) ->
                    FilterChip(
                        selected = wallpaperMode == id,
                        onClick = {
                            wallpaperMode = id
                            scope.launch { api.putPref("ui.appearance.bg_mode", id) }
                        },
                        label = { Text(label, fontSize = 12.sp) },
                    )
                }
            }
            Text("暗角 / 遮罩强度 (${(vignetteStrength * 100).toInt()}%)", color = palette.muted, fontSize = 12.sp)
            Slider(
                value = vignetteStrength,
                onValueChange = { vignetteStrength = it },
                onValueChangeFinished = {
                    scope.launch { api.putPref("ui.appearance.vignette", vignetteStrength.toString()) }
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HsvGradientSliderRow(
    label: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    trackColors: List<Color>,
    thumbColor: Color,
    onValueChange: (Float) -> Unit,
) {
    val palette = LocalForHappyPalette.current
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, color = palette.muted, fontSize = 11.5.sp)
            Text(valueText, color = palette.text, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
                    .height(10.dp)
                    .clip(CircleShape)
                    .background(Brush.horizontalGradient(trackColors))
                    .border(0.5.dp, palette.line.copy(alpha = 0.75f), CircleShape),
            )
            Slider(
                value = value.coerceIn(valueRange.start, valueRange.endInclusive),
                onValueChange = onValueChange,
                valueRange = valueRange,
                colors = SliderDefaults.colors(
                    thumbColor = thumbColor,
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent,
                ),
                thumb = {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(thumbColor)
                            .border(2.5.dp, Color.White, CircleShape)
                            .border(0.75.dp, Color.Black.copy(alpha = 0.30f), CircleShape),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun AccountSecuritySubpage(
    api: ForHappyApiClient,
    me: MeUser?,
    onOpenInvites: () -> Unit,
    onRequireLogin: () -> Unit,
    onLoggedOut: () -> Unit,
) {
    val palette = LocalForHappyPalette.current
    val scope = rememberCoroutineScope()
    var confirmLogout by remember { mutableStateOf(false) }

    SettingsGroupCard {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (me == null) {
                Text("当前为访客浏览模式", color = palette.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Button(onClick = onRequireLogin, modifier = Modifier.fillMaxWidth()) { Text("登录 52dmeme.com") }
            } else {
                Text("用户名：@${me.username}", color = palette.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("信任等级：TL${me.trustLevel} · LINUX DO ${if ((me.linuxdoId ?: 0L) > 0L) "已绑定" else "未绑定"}", color = palette.muted, fontSize = 12.sp)
                OutlinedButton(onClick = onOpenInvites, modifier = Modifier.fillMaxWidth()) { Text("管理我的邀请码") }
                OutlinedButton(
                    onClick = { confirmLogout = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, tint = palette.danger, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("退出登录", color = palette.danger)
                }
            }
        }
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            containerColor = palette.surface,
            titleContentColor = palette.text,
            textContentColor = palette.text,
            title = { Text("确认退出登录？") },
            text = { Text("退出后仅清除本机 Cookie，不影响云端收藏与账本数据。") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = palette.danger, contentColor = Color.White),
                    onClick = {
                        confirmLogout = false
                        scope.launch {
                            api.logout()
                            onLoggedOut()
                        }
                    },
                ) { Text("退出") }
            },
            dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text("取消") } },
        )
    }
}

@Composable
private fun AiPrefsSubpage(
    api: ForHappyApiClient,
    me: MeUser?,
    onRequireLogin: () -> Unit,
) {
    val palette = LocalForHappyPalette.current
    val scope = rememberCoroutineScope()
    var baseUrl by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }
    var modelName by remember { mutableStateOf("") }
    var enableComment by remember { mutableStateOf(false) }
    var enableNovel by remember { mutableStateOf(false) }
    var stylePrompt by remember { mutableStateOf("") }
    var showApiKey by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var statusBanner by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(me?.id) {
        if (me == null) return@LaunchedEffect
        val obj = api.fetchMeAiPrefs() ?: return@LaunchedEffect
        baseUrl = obj["api_base"]?.jsonPrimitive?.contentOrNull ?: obj["base_url"]?.jsonPrimitive?.contentOrNull.orEmpty()
        apiKey = obj["api_key"]?.jsonPrimitive?.contentOrNull.orEmpty()
        modelName = obj["model"]?.jsonPrimitive?.contentOrNull.orEmpty()
        enableComment = obj["comment_enabled"]?.jsonPrimitive?.booleanOrNull == true
        enableNovel = obj["novel_enabled"]?.jsonPrimitive?.booleanOrNull == true
        stylePrompt = obj["style"]?.jsonPrimitive?.contentOrNull.orEmpty()
    }

    SettingsGroupCard {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (me == null) {
                Text("请登录后配置个人自有模型 AI 小伙伴参数", color = palette.muted, fontSize = 13.sp)
                Button(onClick = onRequireLogin) { Text("去登录") }
            } else {
                Text("仅使用您本人填写的 API 额度（按量自费，站方不代扣）。", color = palette.muted, fontSize = 12.sp)
                ForHappyTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = "API 地址 (OpenAI Compatible)",
                    placeholder = "https://api.openai.com/v1",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                ForHappyTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = "API Key (保存后遮罩)",
                    placeholder = "sk-...",
                    singleLine = true,
                    visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        Icon(
                            imageVector = if (showApiKey) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                            contentDescription = if (showApiKey) "隐藏 Key" else "显示 Key",
                            tint = palette.muted,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { showApiKey = !showApiKey },
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                ForHappyTextField(
                    value = modelName,
                    onValueChange = { modelName = it },
                    label = "模型名 (如 gpt-4o-mini)",
                    placeholder = "gpt-4o-mini",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                ForHappyTextField(
                    value = stylePrompt,
                    onValueChange = { stylePrompt = it },
                    label = "互动性格 / 续写风格",
                    placeholder = "例如：幽默简短、二次元吐槽风…",
                    singleLine = false,
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("允许参与评论互动", color = palette.text, fontSize = 13.sp)
                    Switch(checked = enableComment, onCheckedChange = { enableComment = it })
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("允许辅助书房接龙", color = palette.text, fontSize = 13.sp)
                    Switch(checked = enableNovel, onCheckedChange = { enableNovel = it })
                }
                Button(
                    enabled = !saving,
                    onClick = {
                        saving = true
                        scope.launch {
                            val payload = buildJsonObject {
                                put("api_base", JsonPrimitive(baseUrl.trim()))
                                if (apiKey.isNotBlank()) put("api_key", JsonPrimitive(apiKey.trim()))
                                put("model", JsonPrimitive(modelName.trim()))
                                put("style", JsonPrimitive(stylePrompt.trim()))
                                put("comment_enabled", JsonPrimitive(enableComment))
                                put("novel_enabled", JsonPrimitive(enableNovel))
                            }
                            val res = api.patchMeAiPrefs(payload)
                            saving = false
                            statusBanner = res.fold(onSuccess = { "AI 小伙伴配置已保存" }, onFailure = { it.message ?: "保存失败" })
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (saving) "保存中…" else "保存配置")
                }
                statusBanner?.let { Text(it, color = palette.primary, fontSize = 12.sp) }
            }
        }
    }
}

@Composable
private fun NetworkDiagnosticsSubpage(api: ForHappyApiClient) {
    val palette = LocalForHappyPalette.current
    val clipboard = LocalClipboardManager.current
    val netStatus by api.rustNetManager.status.collectAsState()
    val netConfig by api.rustNetManager.config.collectAsState()

    var customIpInput by remember { mutableStateOf("") }
    var diagMsg by remember { mutableStateOf<String?>(null) }

    SettingsGroupCard {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Cloudflare Rust 加速内核", color = palette.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                BadgePill(
                    text = if (netStatus.running) "已就绪 :${netStatus.port}" else netStatus.phase,
                    containerColor = if (netStatus.running) palette.primary.copy(alpha = 0.16f) else palette.danger.copy(alpha = 0.16f),
                    contentColor = if (netStatus.running) palette.primary else palette.danger,
                )
            }
            Text("优选 Edge IP：${netStatus.winnerIp ?: "测速中…"} (${netStatus.latencyMs?.let { "${it} ms" } ?: "--"})", color = palette.text, fontSize = 13.sp)
            Text("ECH：${if (netStatus.echAccepted) "服务端已接受 (${netStatus.echKeyCount} 钥匙)" else "待协商 (${netStatus.echKeyCount} keys)"} · 来源：${netStatus.winnerSource ?: "--"}", color = palette.muted, fontSize = 12.sp)
            netStatus.lastError?.let { err -> Text("最近诊断：$err", color = palette.danger, fontSize = 11.sp) }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("启用 ECH (Encrypted ClientHello)", color = palette.text, fontSize = 13.sp)
                Switch(checked = netConfig.echEnabled, onCheckedChange = { api.rustNetManager.updateSettings(echEnabled = it) })
            }

            Text("IPv6 策略", color = palette.muted, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("auto" to "双栈自动", "prefer_v6" to "优先 IPv6", "prefer_v4" to "仅/优先 IPv4").forEach { (id, label) ->
                    FilterChip(selected = netConfig.ipv6Mode == id, onClick = { api.rustNetManager.updateSettings(ipv6Mode = id) }, label = { Text(label, fontSize = 11.sp) })
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ForHappyTextField(
                    value = customIpInput,
                    onValueChange = { customIpInput = it },
                    placeholder = "添加自定义优选 CF IP",
                    singleLine = true,
                    minHeight = 40.dp,
                    modifier = Modifier.weight(1f),
                )
                Button(
                    enabled = customIpInput.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(40.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    onClick = {
                        val next = netConfig.customNodes + CustomEdgeNode(ip = customIpInput.trim(), note = "手动添加")
                        api.rustNetManager.updateSettings(customNodes = next)
                        customIpInput = ""
                    },
                ) { Text("添加", fontSize = 12.5.sp) }
            }

            if (netConfig.customNodes.isNotEmpty()) {
                netConfig.customNodes.forEachIndexed { idx, node ->
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("${node.ip} (${node.note})", color = palette.muted, fontSize = 12.sp, modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = {
                                val next = netConfig.customNodes.toMutableList().apply { removeAt(idx) }
                                api.rustNetManager.updateSettings(customNodes = next)
                            },
                            modifier = Modifier.size(28.dp),
                        ) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "移除", tint = palette.muted, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            Button(
                onClick = {
                    api.rustNetManager.triggerReselect()
                    diagMsg = "已触发 Rust 并发赛马重选…"
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = palette.primary, contentColor = palette.onPrimary),
            ) {
                Icon(Icons.Rounded.Refresh, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("重新优选节点", fontSize = 13.sp)
            }

            diagMsg?.let { Text(it, color = palette.primary, fontSize = 12.sp) }

            if (netStatus.logs.isNotEmpty()) {
                Surface(color = palette.surface2, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Rust 内核日志 (${netStatus.logs.size} 条)", color = palette.muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            TextButton(
                                onClick = {
                                    clipboard.setText(AnnotatedString(netStatus.logs.joinToString("\n")))
                                    diagMsg = "内核诊断日志已全部复制到剪贴板"
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            ) {
                                Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("复制全部", fontSize = 11.sp)
                            }
                        }
                        netStatus.logs.takeLast(12).reversed().forEach { line ->
                            Text(line, color = palette.text, fontSize = 10.sp, lineHeight = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StorageCacheSubpage(api: ForHappyApiClient) {
    val palette = LocalForHappyPalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var webBytes by remember { mutableLongStateOf(api.webAssetCache.cacheSizeBytes()) }
    var imageCacheBytes by remember { mutableLongStateOf(0L) }
    var dbBytes by remember { mutableLongStateOf(0L) }
    var statusText by remember { mutableStateOf<String?>(null) }

    suspend fun refreshSizes() {
        webBytes = api.webAssetCache.cacheSizeBytes()
        withContext(Dispatchers.IO) {
            imageCacheBytes = dirSizeBytes(context.cacheDir)
            dbBytes = context.getDatabasePath("forhappy.db").takeIf { it.exists() }?.length() ?: 0L
        }
    }

    LaunchedEffect(Unit) { refreshSizes() }

    SettingsGroupCard {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            StorageRow("网页静态资源缓存", "${webBytes / 1024} KB") {
                scope.launch {
                    api.webAssetCache.clearAll()
                    refreshSizes()
                    statusText = "已清理网页静态缓存"
                }
            }
            HorizontalDivider(color = palette.line.copy(alpha = 0.6f))
            StorageRow("图片 / 临时媒体缓存", "${imageCacheBytes / 1024} KB") {
                scope.launch {
                    withContext(Dispatchers.IO) {
                        context.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
                    }
                    refreshSizes()
                    statusText = "已清理图片与媒体临时缓存"
                }
            }
            HorizontalDivider(color = palette.line.copy(alpha = 0.6f))
            StorageRow("本地消息数据库 (forhappy.db)", "${dbBytes / 1024} KB", actionLabel = "清理群聊缓存") {
                scope.launch {
                    withContext(Dispatchers.IO) {
                        val db = (context.applicationContext as? ForHappyApp)?.db
                        db?.writableDatabase?.delete("chat_messages", null, null)
                    }
                    refreshSizes()
                    statusText = "已清理本地聊天消息窗口缓存（不影响云端）"
                }
            }
            HorizontalDivider(color = palette.line.copy(alpha = 0.6f))
            StorageRow("发帖草稿缓存", "本地 KV", actionLabel = "清空草稿") {
                scope.launch {
                    api.putPref("compose.draft.v1", "")
                    statusText = "已清空发帖草稿"
                }
            }
            statusText?.let { Text(it, color = palette.primary, fontSize = 12.sp) }
        }
    }
}

@Composable
private fun StorageRow(title: String, sizeLabel: String, actionLabel: String = "清理", onClear: () -> Unit) {
    val palette = LocalForHappyPalette.current
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = palette.text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(sizeLabel, color = palette.muted, fontSize = 11.sp)
        }
        OutlinedButton(onClick = onClear, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
            Text(actionLabel, fontSize = 12.sp)
        }
    }
}

@Composable
private fun AboutUpdateSubpage(updateController: AppUpdateController) {
    val palette = LocalForHappyPalette.current
    val context = LocalContext.current
    val updateState by updateController.state.collectAsState()
    var customMirrorInput by remember(updateState.customMirrorPrefix) { mutableStateOf(updateState.customMirrorPrefix) }

    SettingsGroupCard {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("当前版本", color = palette.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                BadgePill("v${BuildConfig.VERSION_NAME}", containerColor = palette.surface2, contentColor = palette.primary)
            }
            Text(
                text = updateState.statusMessage,
                color = if (updateState.status == UpdateStatus.ERROR) palette.danger else palette.muted,
                fontSize = 12.sp,
            )
            Text(
                text = "加速通道：${if (updateState.customMirrorPrefix.isNotBlank()) "自定义源优先 + " else ""}${updateState.activeMirrorCount} 个公益镜像并发竞速",
                color = palette.muted,
                fontSize = 11.sp,
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "自定义加速前缀（留空自动）",
                    color = palette.muted,
                    fontSize = 11.5.sp,
                    modifier = Modifier.padding(start = 2.dp),
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ForHappyTextField(
                        value = customMirrorInput,
                        onValueChange = { customMirrorInput = it },
                        placeholder = "例如 https://ghfast.top",
                        singleLine = true,
                        minHeight = 40.dp,
                        modifier = Modifier.weight(1f),
                    )
                    Button(
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(40.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp),
                        onClick = { updateController.saveCustomMirrorPrefix(customMirrorInput) },
                    ) {
                        Text("保存", fontSize = 12.5.sp)
                    }
                }
            }

            if (updateState.status == UpdateStatus.DOWNLOADING) {
                LinearProgressIndicator(
                    progress = { (updateState.downloadProgressPercent / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                    color = palette.primary,
                    trackColor = palette.surface2,
                )
            }

            val rel = updateState.latestRelease
            if (rel != null && rel.releaseNotes.isNotBlank() && updateState.status != UpdateStatus.UP_TO_DATE) {
                Surface(color = palette.surface2, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(rel.releaseTitle, color = palette.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(2.dp))
                        Text(rel.releaseNotes, color = palette.muted, fontSize = 11.sp, lineHeight = 15.sp, maxLines = 8, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    enabled = updateState.status != UpdateStatus.CHECKING && updateState.status != UpdateStatus.DOWNLOADING,
                    onClick = { updateController.checkForUpdates(silent = false) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (updateState.status == UpdateStatus.CHECKING) "检查中…" else "检查更新", fontSize = 12.sp)
                }

                if (updateState.status == UpdateStatus.AVAILABLE || (updateState.status == UpdateStatus.ERROR && rel != null)) {
                    Button(
                        onClick = { updateController.downloadAndInstall() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = palette.primary, contentColor = palette.onPrimary),
                    ) { Text("下载并更新", fontSize = 12.sp) }
                } else if (updateState.status == UpdateStatus.READY_TO_INSTALL) {
                    Button(
                        onClick = {
                            StealthExemptionGate.markInternalJump()
                            updateController.installReadyApk()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = palette.primary, contentColor = palette.onPrimary),
                    ) { Text("立即安装", fontSize = 12.sp) }
                } else {
                    OutlinedButton(
                        onClick = {
                            val targetUrl = rel?.htmlUrl?.takeIf { it.isNotBlank() } ?: AppUpdateManager.RELEASES_HTML_URL
                            runCatching {
                                StealthExemptionGate.markInternalJump()
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl))
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text("GitHub Release", fontSize = 12.sp) }
                }
            }
        }
    }
}

@Composable
private fun SettingsGroupHeader(title: String) {
    val palette = LocalForHappyPalette.current
    Text(title, color = palette.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp, start = 4.dp))
}

@Composable
private fun SettingsGroupCard(content: @Composable () -> Unit) {
    val palette = LocalForHappyPalette.current
    val shape = RoundedCornerShape(if (palette.isModernUi) 20.dp else 16.dp)
    Surface(
        color = palette.surface,
        shape = shape,
        shadowElevation = if (palette.isModernUi && !palette.isDark) 1.dp else 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (palette.isModernUi) {
                    if (palette.cardBorder != Color.Transparent) Modifier.border(1.dp, palette.cardBorder, shape) else Modifier
                } else {
                    Modifier.border(1.dp, palette.line, shape)
                },
            ),
    ) {
        Column { content() }
    }
}

@Composable
private fun SettingsNavRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badgeText: String? = null,
    onClick: () -> Unit,
) {
    val palette = LocalForHappyPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (palette.isModernUi) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(palette.primary.copy(alpha = 0.12f)),
            ) {
                Icon(icon, contentDescription = null, tint = palette.primary, modifier = Modifier.size(18.dp))
            }
        } else {
            Icon(icon, contentDescription = null, tint = palette.primary, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, color = palette.text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                badgeText?.let { BadgePill(it, containerColor = palette.primary.copy(alpha = 0.14f), contentColor = palette.primary) }
            }
            Text(subtitle, color = palette.muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = palette.muted, modifier = Modifier.size(18.dp))
    }
}

private fun dirSizeBytes(dir: File?): Long {
    if (dir == null || !dir.exists()) return 0L
    if (dir.isFile) return dir.length()
    return dir.listFiles()?.sumOf { dirSizeBytes(it) } ?: 0L
}
