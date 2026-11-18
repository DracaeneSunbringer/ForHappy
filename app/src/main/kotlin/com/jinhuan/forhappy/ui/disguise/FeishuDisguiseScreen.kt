package com.jinhuan.forhappy.ui.disguise

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jinhuan.forhappy.core.chat.ChatSessionController
import com.jinhuan.forhappy.core.compose.ComposeTrayItem
import com.jinhuan.forhappy.core.model.AppDisguiseMode
import com.jinhuan.forhappy.core.model.AppIconAlias
import com.jinhuan.forhappy.core.model.AppThemeMode
import com.jinhuan.forhappy.core.model.ChatGroup
import com.jinhuan.forhappy.core.model.ChatReaction
import com.jinhuan.forhappy.core.model.ChatUser
import com.jinhuan.forhappy.core.model.DayEntry
import com.jinhuan.forhappy.core.model.DmThread
import com.jinhuan.forhappy.core.model.ForHappyJson
import com.jinhuan.forhappy.core.model.MeUser
import com.jinhuan.forhappy.core.model.MemeItem
import com.jinhuan.forhappy.core.model.MemeMediaItem
import com.jinhuan.forhappy.core.model.PrivacyStealthConfig
import com.jinhuan.forhappy.core.model.TagStatItem
import com.jinhuan.forhappy.core.navigation.StealthExemptionGate
import com.jinhuan.forhappy.core.network.api.ForHappyApiClient
import com.jinhuan.forhappy.ui.chat.ChatScreen
import com.jinhuan.forhappy.ui.components.ReactionChip
import com.jinhuan.forhappy.ui.components.RichChatBodyView
import com.jinhuan.forhappy.ui.compose.ComposeMediaStager
import com.jinhuan.forhappy.ui.compose.ImageMaskEditorScreen
import com.jinhuan.forhappy.ui.theme.LocalBossKeyActive
import com.jinhuan.forhappy.ui.wall.ConfigureFullscreenDialogWindow
import com.jinhuan.forhappy.core.clock.ServerClock
import com.jinhuan.forhappy.core.seen.SeenTracker
import com.jinhuan.forhappy.ui.wall.ImmersiveMediaStage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private const val KV_FEISHU_GROUP_ALIASES = "disguise.feishu.group_aliases"

/**
 * Preserved internal helpers shared with [DisguiseSanitizer], [SeenTrackerAndDisguiseTest] & Phase 6 tests.
 */
internal object FeishuDisguiseHelpers {
    val IM_GROUPS = listOf(
        "产品需求评审",
        "研发周会",
        "设计走查",
        "增长数据复盘",
        "客户成功",
        "项目管理办公室",
        "技术委员会",
        "运营协同",
        "质量保障",
        "平台架构",
        "市场活动筹备",
        "效能改进",
    )

    val IM_COLORS = listOf(
        Color(0xFF3370FF),
        Color(0xFF14B8A6),
        Color(0xFFFF7D00),
        Color(0xFF7B61FF),
        Color(0xFF22C55E),
        Color(0xFF00B4D8),
        Color(0xFFF59E0B),
        Color(0xFFEF4444),
        Color(0xFF0FA0A8),
        Color(0xFF8B5CF6),
    )

    val IM_NAMES = listOf(
        "张伟", "王芳", "李静", "刘洋", "陈晨", "杨帆", "赵磊", "黄婷", "周宇", "吴昊",
        "徐蕾", "孙浩", "马超然", "朱琳", "胡斌", "郭佳", "何雨", "林峰", "罗敏", "高翔",
        "梁爽", "宋涛", "郑楠", "谢雯", "韩冰", "唐亮", "冯悦", "于洁", "董鹏", "萧然",
        "程诚", "曹露", "袁野", "邓可", "许诺", "傅航", "沈清", "曾凡", "彭博", "吕青",
    )

    private val WEEK_CHARS = arrayOf("日", "一", "二", "三", "四", "五", "六")
    private val CLEAN_TITLE_REGEX = Regex("""[\s·#【】\[\]()（）]""")

    fun imFake(seed: Long): String {
        val u32 = (seed * 2654435761L) and 0xFFFFFFFFL
        val idx = (u32 % IM_NAMES.size.toLong()).toInt()
        return IM_NAMES[idx]
    }

    fun imColor(seedText: String): Color {
        var acc = 0
        seedText.codePoints().forEach { cp ->
            acc = acc * 31 + cp
        }
        val idx = Math.floorMod(acc, IM_COLORS.size)
        return IM_COLORS[idx]
    }

    fun avatarChars(title: String): String {
        val cleaned = CLEAN_TITLE_REGEX.replace(title, "")
        val list = ArrayList<String>(2)
        var i = 0
        while (i < cleaned.length && list.size < 2) {
            val cp = cleaned.codePointAt(i)
            list.add(String(Character.toChars(cp)))
            i += Character.charCount(cp)
        }
        return list.joinToString("").ifBlank { "群" }
    }

    fun imGroupName(day: String): String {
        val parsed = runCatching { LocalDate.parse(day) }.getOrNull() ?: return day
        val epochDay = parsed.toEpochDay()
        val grpIdx = Math.floorMod(epochDay, IM_GROUPS.size)
        return "${IM_GROUPS[grpIdx]} · ${parsed.monthValue}月${parsed.dayOfMonth}日"
    }

    fun daySeparatorLabel(day: String): String {
        val parsed = runCatching { LocalDate.parse(day) }.getOrNull() ?: return day
        val today = runCatching {
            LocalDate.parse(SeenTracker.dayOfUtc8(ServerClock.serverNowSec()))
        }.getOrNull() ?: LocalDate.now()
        val diff = today.toEpochDay() - parsed.toEpochDay()
        val dayOfWeekIdx = parsed.dayOfWeek.value % 7 // Sunday -> 0, Monday -> 1
        val sub = when {
            diff == 0L -> "今天"
            diff == 1L -> "昨天"
            parsed.year != today.year -> "${parsed.year}年 · 周${WEEK_CHARS[dayOfWeekIdx]}"
            else -> "周${WEEK_CHARS[dayOfWeekIdx]}"
        }
        return "${parsed.monthValue}月${parsed.dayOfMonth}日 $sub"
    }

    fun formatClock(ts: Long?): String {
        if (ts == null || ts <= 0L) return ""
        val ms = if (ts > 10_000_000_000L) ts else ts * 1000L
        val sdf = SimpleDateFormat("HH:mm", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("GMT+08:00")
        return sdf.format(Date(ms))
    }

    fun formatConvTime(ts: Long): String {
        if (ts <= 0L) return ""
        val sec = if (ts > 10_000_000_000L) ts / 1000L else ts
        val msgCal = Calendar.getInstance(TimeZone.getTimeZone("GMT+08:00")).apply {
            timeInMillis = sec * 1000L
        }
        val todayCal = Calendar.getInstance(TimeZone.getTimeZone("GMT+08:00")).apply {
            timeInMillis = ServerClock.serverNowSec() * 1000L
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val msgMs = msgCal.timeInMillis
        val todayStart = todayCal.timeInMillis
        return when {
            msgMs >= todayStart -> formatClock(sec)
            msgMs >= todayStart - 86_400_000L -> "昨天"
            msgMs >= todayStart - 6 * 86_400_000L -> "周${WEEK_CHARS[(msgCal.get(Calendar.DAY_OF_WEEK) - 1).coerceIn(0, 6)]}"
            else -> "${msgCal.get(Calendar.MONTH) + 1}月${msgCal.get(Calendar.DAY_OF_MONTH)}日"
        }
    }
}

private sealed interface FeishuConvTarget {
    val sortTs: Long

    data class RealGroup(
        val group: ChatGroup,
        val displayTitle: String,
        override val sortTs: Long,
    ) : FeishuConvTarget

    data class RealDm(
        val dm: DmThread,
        val displayTitle: String,
        override val sortTs: Long,
    ) : FeishuConvTarget

    data class DayWorkGroup(
        val dayEntry: DayEntry,
        val title: String,
        override val sortTs: Long,
    ) : FeishuConvTarget

    data class TagTopic(
        val tag: String,
        val count: Int,
        override val sortTs: Long = 0L,
    ) : FeishuConvTarget
}

/**
 * Full 3-Tab native Feishu IM Disguise Layer (`消息 / 通讯录 / 我的`) matching
 * `docs/redesign/03-privacy-state-machine.md` §5.1–5.3 & §5.6–5.7.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FeishuDisguiseScreen(
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
    val fc = rememberFeishuColors()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val bossKeyActive = LocalBossKeyActive.current
    val chatState by chatController.uiState.collectAsState()

    var activeTab by remember { mutableStateOf("messages") } // "messages" | "contacts" | "me"
    var msgFilter by remember { mutableStateOf("all") } // "all" | "unread" | "dm" | "group" | "at" | "topics"
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showPlusMenu by remember { mutableStateOf(false) }

    var dayGroups by remember { mutableStateOf<List<DayEntry>>(emptyList()) }
    var tagsList by remember { mutableStateOf<List<TagStatItem>>(emptyList()) }
    var loadingDays by remember { mutableStateOf(true) }
    var customGroupAliases by remember { mutableStateOf<Map<Long, String>>(emptyMap()) }

    var activeTarget by remember { mutableStateOf<FeishuConvTarget?>(null) }
    var settingsPage by remember { mutableStateOf<FeishuSettingsPage?>(null) }
    var newDmTargetUser by remember { mutableStateOf<ChatUser?>(null) }
    var newDmFirstText by remember { mutableStateOf("") }
    var newDmSending by remember { mutableStateOf(false) }

    var globalBanner by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(globalBanner) {
        if (globalBanner != null) {
            delay(2200L)
            globalBanner = null
        }
    }

    LaunchedEffect(Unit) {
        chatController.refreshGroups()
        chatController.refreshDmThreads()
        val rawAliasJson = api.getPref(KV_FEISHU_GROUP_ALIASES)
        if (!rawAliasJson.isNullOrBlank()) {
            runCatching {
                ForHappyJson.decodeFromString(
                    MapSerializer(String.serializer(), String.serializer()),
                    rawAliasJson,
                )
            }.getOrNull()?.let { parsed ->
                customGroupAliases = parsed.mapNotNull { (k, v) ->
                    k.toLongOrNull()?.let { it to v }
                }.toMap()
            }
        }
        loadingDays = true
        dayGroups = api.fetchDays(limitDays = 12).days.take(12)
        tagsList = api.fetchTags()
        loadingDays = false
    }

    val saveGroupAlias: (Long, String) -> Unit = { gid, newAlias ->
        val clean = newAlias.trim()
        val next = customGroupAliases.toMutableMap().apply {
            if (clean.isEmpty()) remove(gid) else put(gid, clean)
        }
        customGroupAliases = next
        scope.launch {
            val serialized = ForHappyJson.encodeToString(
                MapSerializer(String.serializer(), String.serializer()),
                next.mapKeys { it.key.toString() },
            )
            api.putPref(KV_FEISHU_GROUP_ALIASES, serialized)
        }
    }

    val totalUnreadCount = remember(chatState.groups, chatState.dmThreads) {
        chatState.groups.sumOf { it.unread } + chatState.dmThreads.sumOf { it.unread }
    }

    // Back handler for open detail / settings overlays
    BackHandler(enabled = activeTarget != null && settingsPage == null) {
        activeTarget = null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(fc.bg),
    ) {
        when {
            settingsPage != null -> {
                FeishuSettingsOverlay(
                    api = api,
                    initialPage = settingsPage ?: FeishuSettingsPage.ROOT,
                    themeMode = themeMode,
                    privacyConfig = privacyConfig,
                    privacyShieldEnabled = privacyShieldEnabled,
                    activeIconAlias = activeIconAlias,
                    onChangeTheme = onChangeTheme,
                    onExitDisguise = onExitDisguise,
                    onChangeDisguise = onChangeDisguise,
                    onUpdatePrivacyConfig = onUpdatePrivacyConfig,
                    onTogglePrivacyShield = onTogglePrivacyShield,
                    onChangeIconAlias = onChangeIconAlias,
                    onClose = { settingsPage = null },
                )
            }

            activeTarget != null -> {
                when (val target = activeTarget!!) {
                    is FeishuConvTarget.DayWorkGroup -> {
                        FeishuWorkGroupDetailScreen(
                            title = target.title,
                            dayFilter = target.dayEntry.day,
                            tagFilter = null,
                            api = api,
                            privacyConfig = privacyConfig,
                            onNotify = { globalBanner = it },
                            onOpenTagTopic = { tag ->
                                activeTarget = FeishuConvTarget.TagTopic(tag = tag, count = 0)
                            },
                            onBack = { activeTarget = null },
                        )
                    }

                    is FeishuConvTarget.TagTopic -> {
                        FeishuWorkGroupDetailScreen(
                            title = "#${DisguiseSanitizer.sanitizeSummaryForDisguise(target.tag, AppDisguiseMode.FEISHU)}",
                            dayFilter = null,
                            tagFilter = target.tag,
                            api = api,
                            privacyConfig = privacyConfig,
                            onNotify = { globalBanner = it },
                            onOpenTagTopic = { tag ->
                                activeTarget = FeishuConvTarget.TagTopic(tag = tag, count = 0)
                            },
                            onBack = { activeTarget = null },
                        )
                    }

                    is FeishuConvTarget.RealGroup -> {
                        val currentTitle = DisguiseSanitizer.resolveFeishuGroupAlias(
                            groupId = target.group.id,
                            rawName = target.group.name,
                            customAliases = customGroupAliases,
                        )
                        FeishuRealChatShell(
                            title = currentTitle,
                            subtitle = "${maxOf(1, target.group.memberCount)} 名成员 · 企业内部群",
                            isGroup = true,
                            groupId = target.group.id,
                            dmThreadId = null,
                            currentAlias = customGroupAliases[target.group.id].orEmpty(),
                            api = api,
                            chatController = chatController,
                            me = me,
                            onSaveAlias = { alias -> saveGroupAlias(target.group.id, alias) },
                            onOpenMemeThread = { memeId ->
                                scope.launch {
                                    api.fetchMemeDetail(memeId)?.resolvedMeme?.let { fetched ->
                                        activeTarget = FeishuConvTarget.DayWorkGroup(
                                            dayEntry = DayEntry(day = fetched.day.orEmpty(), items = listOf(fetched)),
                                            title = currentTitle,
                                            sortTs = fetched.createdAt ?: 0L,
                                        )
                                    }
                                }
                            },
                            onOpenUrl = onOpenUrl,
                            onRequireLogin = onRequireLogin,
                            onBack = { activeTarget = null },
                        )
                    }

                    is FeishuConvTarget.RealDm -> {
                        FeishuRealChatShell(
                            title = target.displayTitle,
                            subtitle = "同事单聊 · 在线",
                            isGroup = false,
                            groupId = null,
                            dmThreadId = target.dm.id,
                            currentAlias = "",
                            api = api,
                            chatController = chatController,
                            me = me,
                            onSaveAlias = {},
                            onOpenMemeThread = {},
                            onOpenUrl = onOpenUrl,
                            onRequireLogin = onRequireLogin,
                            onBack = { activeTarget = null },
                        )
                    }
                }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding(),
                ) {
                    when (activeTab) {
                        "messages" -> {
                            FeishuMessagesHomeTab(
                                fc = fc,
                                me = me,
                                groups = chatState.groups,
                                dmThreads = chatState.dmThreads,
                                dayGroups = dayGroups,
                                tagsList = tagsList,
                                loadingDays = loadingDays,
                                customAliases = customGroupAliases,
                                aliasEnabled = privacyConfig.feishuMemberAliasEnabled,
                                msgFilter = msgFilter,
                                showSearch = showSearch,
                                searchQuery = searchQuery,
                                showPlusMenu = showPlusMenu,
                                onSelectFilter = { msgFilter = it },
                                onToggleSearch = {
                                    showSearch = !showSearch
                                    if (!showSearch) searchQuery = ""
                                },
                                onSearchQueryChange = { searchQuery = it },
                                onTogglePlusMenu = { showPlusMenu = it },
                                onJumpContacts = {
                                    showPlusMenu = false
                                    activeTab = "contacts"
                                },
                                onJumpTopicsFilter = {
                                    showPlusMenu = false
                                    msgFilter = "topics"
                                },
                                onOpenMeTab = { activeTab = "me" },
                                onSelectTarget = { activeTarget = it },
                                modifier = Modifier.weight(1f),
                            )
                        }

                        "contacts" -> {
                            FeishuContactsTab(
                                fc = fc,
                                api = api,
                                me = me,
                                groups = chatState.groups,
                                dmThreads = chatState.dmThreads,
                                customAliases = customGroupAliases,
                                aliasEnabled = privacyConfig.feishuMemberAliasEnabled,
                                onRequireLogin = onRequireLogin,
                                onOpenGroup = { grp ->
                                    val alias = DisguiseSanitizer.resolveFeishuGroupAlias(grp.id, grp.name, customGroupAliases)
                                    activeTarget = FeishuConvTarget.RealGroup(grp, alias, grp.latest?.at ?: 0L)
                                },
                                onOpenDmThread = { dm ->
                                    val peerName = feishuSafeAuthor(dm.peerId, dm.peerName ?: dm.peerUsername, privacyConfig.feishuMemberAliasEnabled)
                                    activeTarget = FeishuConvTarget.RealDm(dm, peerName, maxOf(dm.lastAt, dm.updatedAt))
                                },
                                onStartNewDmUser = { user ->
                                    newDmTargetUser = user
                                    newDmFirstText = ""
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }

                        else -> {
                            FeishuMeTab(
                                fc = fc,
                                me = me,
                                scope = scope,
                                api = api,
                                aliasEnabled = privacyConfig.feishuMemberAliasEnabled,
                                onRequireLogin = onRequireLogin,
                                onOpenSettings = { settingsPage = it },
                                onFilterFavorites = {
                                    activeTab = "messages"
                                    globalBanner = "可在任意工作群消息内管理收藏条目"
                                },
                                onLoggedOut = {
                                    onLoggedOut()
                                    globalBanner = "已退出工作区账号"
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }

                    // Strict 3-Tab Feishu Bottom Bar (消息 / 通讯录 / 我的) — ZERO fake 云文档 tab
                    HorizontalDivider(color = fc.line)
                    Surface(
                        color = fc.card,
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            FeishuBottomTabItem(
                                selected = activeTab == "messages",
                                icon = if (activeTab == "messages") Icons.Rounded.ChatBubble else Icons.Rounded.ChatBubbleOutline,
                                label = "消息",
                                badgeCount = totalUnreadCount,
                                fc = fc,
                                modifier = Modifier.weight(1f),
                                onClick = { activeTab = "messages" },
                            )
                            FeishuBottomTabItem(
                                selected = activeTab == "contacts",
                                icon = Icons.Rounded.Contacts,
                                label = "通讯录",
                                badgeCount = 0,
                                fc = fc,
                                modifier = Modifier.weight(1f),
                                onClick = { activeTab = "contacts" },
                            )
                            FeishuBottomTabItem(
                                selected = activeTab == "me",
                                icon = Icons.Rounded.Person,
                                label = "我的",
                                badgeCount = 0,
                                fc = fc,
                                modifier = Modifier.weight(1f),
                                onClick = { activeTab = "me" },
                            )
                        }
                    }
                }
            }
        }

        if (!globalBanner.isNullOrBlank()) {
            Surface(
                color = Color(0xFF1F2329).copy(alpha = 0.88f),
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 72.dp),
            ) {
                Text(
                    text = globalBanner.orEmpty(),
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                )
            }
        }
    }

    if (!bossKeyActive) {
        newDmTargetUser?.let { peer ->
            val colleagueName = feishuSafeAuthor(peer.id, peer.displayName, privacyConfig.feishuMemberAliasEnabled)
            AlertDialog(
                onDismissRequest = { if (!newDmSending) newDmTargetUser = null },
                containerColor = fc.card,
                titleContentColor = fc.text,
                textContentColor = fc.text,
                title = { Text("发送工作消息给 $colleagueName", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) },
                text = {
                    OutlinedTextField(
                        value = newDmFirstText,
                        onValueChange = { newDmFirstText = it },
                        placeholder = { Text("输入单聊首条消息…", fontSize = 13.sp, color = fc.muted) },
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
                confirmButton = {
                    TextButton(
                        enabled = !newDmSending && newDmFirstText.isNotBlank(),
                        onClick = {
                            newDmSending = true
                            val body = newDmFirstText.trim()
                            scope.launch {
                                val res = api.sendDmToThreadOrUser(peerUserId = peer.id, body = body)
                                newDmSending = false
                                res.fold(
                                    onSuccess = { (tid, _) ->
                                        chatController.refreshDmThreads()
                                        newDmTargetUser = null
                                        val syntheticDm = DmThread(
                                            id = tid,
                                            peerId = peer.id,
                                            peerName = peer.displayName,
                                            lastBody = body,
                                            lastAt = System.currentTimeMillis() / 1000L,
                                        )
                                        activeTarget = FeishuConvTarget.RealDm(
                                            dm = syntheticDm,
                                            displayTitle = colleagueName,
                                            sortTs = syntheticDm.lastAt,
                                        )
                                    },
                                    onFailure = { err ->
                                        globalBanner = DisguiseSanitizer.sanitizeSummaryForDisguise(
                                            err.message ?: "发送失败",
                                            AppDisguiseMode.FEISHU,
                                        )
                                    },
                                )
                            }
                        },
                    ) {
                        Text(if (newDmSending) "发送中…" else "发送", color = fc.blue)
                    }
                },
                dismissButton = {
                    TextButton(enabled = !newDmSending, onClick = { newDmTargetUser = null }) {
                        Text("取消", color = fc.muted)
                    }
                },
            )
        }
    }
}

@Composable
private fun FeishuBottomTabItem(
    selected: Boolean,
    icon: ImageVector,
    label: String,
    badgeCount: Int,
    fc: FeishuThemePalette,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val fg = if (selected) fc.blue else fc.muted
    Column(
        modifier = modifier
            .height(56.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Icon(icon, contentDescription = label, tint = fg, modifier = Modifier.size(22.dp))
            if (badgeCount > 0) {
                Box(
                    modifier = Modifier
                        .offset(x = 8.dp, y = (-3).dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(fc.unreadRed)
                        .padding(horizontal = 4.dp, vertical = 0.5.dp),
                ) {
                    Text(
                        text = if (badgeCount > 99) "99+" else "$badgeCount",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            color = fg,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FeishuMessagesHomeTab(
    fc: FeishuThemePalette,
    me: MeUser?,
    groups: List<ChatGroup>,
    dmThreads: List<DmThread>,
    dayGroups: List<DayEntry>,
    tagsList: List<TagStatItem>,
    loadingDays: Boolean,
    customAliases: Map<Long, String>,
    aliasEnabled: Boolean,
    msgFilter: String,
    showSearch: Boolean,
    searchQuery: String,
    showPlusMenu: Boolean,
    onSelectFilter: (String) -> Unit,
    onToggleSearch: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onTogglePlusMenu: (Boolean) -> Unit,
    onJumpContacts: () -> Unit,
    onJumpTopicsFilter: () -> Unit,
    onOpenMeTab: () -> Unit,
    onSelectTarget: (FeishuConvTarget) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selfLabel = feishuSafeAuthor(me?.id ?: 0L, me?.displayName ?: "我", aliasEnabled)

    val mergedConvs = remember(groups, dmThreads, dayGroups, customAliases, aliasEnabled) {
        val list = mutableListOf<FeishuConvTarget>()
        groups.forEach { g ->
            list.add(
                FeishuConvTarget.RealGroup(
                    group = g,
                    displayTitle = DisguiseSanitizer.resolveFeishuGroupAlias(g.id, g.name, customAliases),
                    sortTs = g.latest?.at ?: 0L,
                ),
            )
        }
        dmThreads.forEach { dm ->
            val title = feishuSafeAuthor(dm.peerId, dm.peerName ?: dm.peerUsername, aliasEnabled)
            list.add(
                FeishuConvTarget.RealDm(
                    dm = dm,
                    displayTitle = title,
                    sortTs = maxOf(dm.lastAt, dm.updatedAt),
                ),
            )
        }
        dayGroups.forEach { entry ->
            list.add(
                FeishuConvTarget.DayWorkGroup(
                    dayEntry = entry,
                    title = FeishuDisguiseHelpers.imGroupName(entry.day),
                    sortTs = entry.latest?.at ?: 0L,
                ),
            )
        }
        list.sortedByDescending { it.sortTs }
    }

    val filteredConvs = remember(mergedConvs, msgFilter, searchQuery) {
        val q = searchQuery.trim()
        mergedConvs.filter { conv ->
            val matchFilter = when (msgFilter) {
                "unread" -> when (conv) {
                    is FeishuConvTarget.RealGroup -> conv.group.unread > 0
                    is FeishuConvTarget.RealDm -> conv.dm.unread > 0
                    is FeishuConvTarget.DayWorkGroup -> conv.dayEntry.unseen > 0
                    is FeishuConvTarget.TagTopic -> false
                }
                "dm" -> conv is FeishuConvTarget.RealDm
                "group" -> conv is FeishuConvTarget.RealGroup || conv is FeishuConvTarget.DayWorkGroup
                "at" -> conv is FeishuConvTarget.RealGroup && conv.group.atMe > 0
                else -> true
            }
            if (!matchFilter) return@filter false
            if (q.isBlank()) return@filter true
            val title = when (conv) {
                is FeishuConvTarget.RealGroup -> conv.displayTitle
                is FeishuConvTarget.RealDm -> conv.displayTitle
                is FeishuConvTarget.DayWorkGroup -> conv.title
                is FeishuConvTarget.TagTopic -> conv.tag
            }
            title.contains(q, ignoreCase = true)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Feishu Top Bar
        Surface(color = fc.card, modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(fc.blue)
                            .clickable(onClick = onOpenMeTab),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = FeishuDisguiseHelpers.avatarChars(selfLabel),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Text(
                        text = "消息",
                        color = fc.text,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onToggleSearch, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Rounded.Search, contentDescription = "搜索", tint = fc.text)
                    }
                    Box {
                        IconButton(onClick = { onTogglePlusMenu(true) }, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.Rounded.Add, contentDescription = "发起沟通", tint = fc.text)
                        }
                        DropdownMenu(
                            expanded = showPlusMenu,
                            onDismissRequest = { onTogglePlusMenu(false) },
                        ) {
                            DropdownMenuItem(
                                text = { Text("发起单聊（选择同事）") },
                                onClick = onJumpContacts,
                            )
                            DropdownMenuItem(
                                text = { Text("按话题标签浏览") },
                                onClick = onJumpTopicsFilter,
                            )
                        }
                    }
                }

                if (showSearch) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = { Text("搜索联系人、群聊或话题", fontSize = 13.sp, color = fc.muted) },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = fc.muted) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchQueryChange("") }) {
                                    Icon(Icons.Rounded.Close, contentDescription = "清除", tint = fc.muted)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }

                // Underline Sub-tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    val tabs = listOf(
                        "all" to "全部",
                        "unread" to "未读",
                        "dm" to "单聊",
                        "group" to "群组",
                        "at" to "@我",
                        "topics" to "话题",
                    )
                    tabs.forEach { (id, label) ->
                        val active = msgFilter == id
                        Column(
                            modifier = Modifier
                                .clickable { onSelectFilter(id) }
                                .padding(top = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = label,
                                color = if (active) fc.blue else fc.muted,
                                fontSize = 14.sp,
                                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                            )
                            Spacer(Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .width(24.dp)
                                    .height(2.5.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(if (active) fc.blue else Color.Transparent),
                            )
                        }
                    }
                }
                HorizontalDivider(color = fc.line)
            }
        }

        if (msgFilter == "topics") {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Text("部门工作话题列表", color = fc.muted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
                if (tagsList.isEmpty()) {
                    item {
                        Text("暂无归档话题", color = fc.muted, fontSize = 13.sp, modifier = Modifier.padding(vertical = 12.dp))
                    }
                } else {
                    item {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            tagsList.forEach { tagItem ->
                                val safeLabel = DisguiseSanitizer.sanitizeSummaryForDisguise(tagItem.tag, AppDisguiseMode.FEISHU)
                                Surface(
                                    color = fc.card,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(0.5.dp, fc.line),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            onSelectTarget(FeishuConvTarget.TagTopic(tagItem.tag, tagItem.totalCount))
                                        },
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Icon(Icons.Rounded.Tag, contentDescription = null, tint = fc.blue, modifier = Modifier.size(15.dp))
                                        Text(safeLabel, color = fc.text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                        Text("${tagItem.totalCount}", color = fc.muted, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(fc.card),
            ) {
                if (loadingDays && filteredConvs.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(color = fc.blue, strokeWidth = 2.dp, modifier = Modifier.size(26.dp))
                        }
                    }
                } else if (filteredConvs.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("暂无匹配会话", color = fc.muted, fontSize = 13.sp)
                        }
                    }
                }
                items(
                    filteredConvs,
                    key = { conv ->
                        when (conv) {
                            is FeishuConvTarget.RealGroup -> "rg_${conv.group.id}"
                            is FeishuConvTarget.RealDm -> "dm_${conv.dm.id}"
                            is FeishuConvTarget.DayWorkGroup -> "dg_${conv.dayEntry.day}"
                            is FeishuConvTarget.TagTopic -> "tag_${conv.tag}"
                        }
                    },
                ) { conv ->
                    FeishuConversationRow(
                        conv = conv,
                        fc = fc,
                        aliasEnabled = aliasEnabled,
                        onClick = { onSelectTarget(conv) },
                    )
                    HorizontalDivider(color = fc.line, modifier = Modifier.padding(start = 74.dp))
                }
            }
        }
    }
}

@Composable
private fun FeishuConversationRow(
    conv: FeishuConvTarget,
    fc: FeishuThemePalette,
    aliasEnabled: Boolean,
    onClick: () -> Unit,
) {
    val (title, preview, timeText, unreadCount, atMeCount, badgeLabel) = when (conv) {
        is FeishuConvTarget.RealGroup -> {
            val g = conv.group
            val latestBy = g.latest?.by?.takeIf { it.isNotBlank() }?.let { raw ->
                "${feishuSafeAuthor(g.id, raw, aliasEnabled)}: "
            }.orEmpty()
            val rawBody = g.latest?.text?.takeIf { it.isNotBlank() } ?: g.desc.orEmpty().ifBlank { "群消息同步" }
            val safeBody = DisguiseSanitizer.sanitizeSummaryForDisguise(rawBody, AppDisguiseMode.FEISHU)
            ConvDisplay(
                title = conv.displayTitle,
                preview = "$latestBy$safeBody",
                timeText = FeishuDisguiseHelpers.formatConvTime(conv.sortTs),
                unread = g.unread,
                atMe = g.atMe,
                tagLabel = "群",
            )
        }
        is FeishuConvTarget.RealDm -> {
            val dm = conv.dm
            val rawBody = dm.lastBody?.takeIf { it.isNotBlank() } ?: "[消息]"
            val safeBody = DisguiseSanitizer.sanitizeSummaryForDisguise(rawBody, AppDisguiseMode.FEISHU)
            ConvDisplay(
                title = conv.displayTitle,
                preview = safeBody,
                timeText = FeishuDisguiseHelpers.formatConvTime(conv.sortTs),
                unread = dm.unread,
                atMe = 0,
                tagLabel = null,
            )
        }
        is FeishuConvTarget.DayWorkGroup -> {
            val d = conv.dayEntry
            val latestAuthor = d.latest?.by?.let { feishuSafeAuthor(d.latest.id, it, aliasEnabled) }
            val rawText = d.latest?.text?.takeIf { it.isNotBlank() } ?: "共 ${d.totalCount} 条项目记录"
            val safeBody = DisguiseSanitizer.sanitizeSummaryForDisguise(rawText, AppDisguiseMode.FEISHU)
            val prefix = if (!latestAuthor.isNullOrBlank()) "$latestAuthor: " else ""
            ConvDisplay(
                title = conv.title,
                preview = "$prefix$safeBody",
                timeText = FeishuDisguiseHelpers.formatConvTime(conv.sortTs),
                unread = d.unseen,
                atMe = 0,
                tagLabel = "部门群",
            )
        }
        is FeishuConvTarget.TagTopic -> {
            ConvDisplay(
                title = "#${conv.tag}",
                preview = "${conv.count} 条话题记录",
                timeText = "",
                unread = 0,
                atMe = 0,
                tagLabel = "话题",
            )
        }
    }

    val avatarBg = remember(title) { FeishuDisguiseHelpers.imColor(title) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(avatarBg),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = FeishuDisguiseHelpers.avatarChars(title),
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (unreadCount > 0) {
                Box(
                    modifier = Modifier
                        .offset(x = 5.dp, y = (-4).dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(fc.unreadRed)
                        .padding(horizontal = 5.dp, vertical = 1.dp),
                ) {
                    Text(
                        text = if (unreadCount > 99) "99+" else "$unreadCount",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = title,
                        color = fc.text,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (!badgeLabel.isNullOrBlank()) {
                        Surface(color = fc.surfaceMuted, shape = RoundedCornerShape(4.dp)) {
                            Text(
                                text = badgeLabel,
                                color = fc.muted,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                            )
                        }
                    }
                }
                if (timeText.isNotBlank()) {
                    Spacer(Modifier.width(8.dp))
                    Text(timeText, color = fc.muted, fontSize = 11.sp)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (atMeCount > 0) {
                    Text("[有人@我]", color = fc.unreadRed, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
                Text(
                    text = preview,
                    color = fc.muted,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

private data class ConvDisplay(
    val title: String,
    val preview: String,
    val timeText: String,
    val unread: Int,
    val atMe: Int,
    val tagLabel: String?,
)

@Composable
private fun FeishuContactsTab(
    fc: FeishuThemePalette,
    api: ForHappyApiClient,
    me: MeUser?,
    groups: List<ChatGroup>,
    dmThreads: List<DmThread>,
    customAliases: Map<Long, String>,
    aliasEnabled: Boolean,
    onRequireLogin: () -> Unit,
    onOpenGroup: (ChatGroup) -> Unit,
    onOpenDmThread: (DmThread) -> Unit,
    onStartNewDmUser: (ChatUser) -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<ChatUser>>(emptyList()) }

    LaunchedEffect(query, me?.id) {
        if (me == null) {
            searchResults = emptyList()
            return@LaunchedEffect
        }
        val clean = query.trim()
        if (clean.isEmpty()) {
            searchResults = emptyList()
            return@LaunchedEffect
        }
        searching = true
        delay(220L)
        searchResults = api.searchDmUsers(clean)
        searching = false
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(fc.bg),
    ) {
        Surface(color = fc.card, modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("通讯录", color = fc.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("搜索组织内联系人发起单聊", fontSize = 13.sp, color = fc.muted) },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = fc.muted) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                )
                HorizontalDivider(color = fc.line)
            }
        }

        if (me == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("登录后可访问企业通讯录与同事单聊", color = fc.muted, fontSize = 14.sp)
                    Surface(
                        color = fc.blue,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onRequireLogin),
                    ) {
                        Text("立即登录", color = Color.White, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp))
                    }
                }
            }
            return
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(fc.card),
        ) {
            if (query.isNotBlank()) {
                item {
                    Text(
                        text = if (searching) "正在搜索联系人…" else "搜索结果 (${searchResults.size})",
                        color = fc.muted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                items(searchResults, key = { "u_${it.id}" }) { user ->
                    val name = feishuSafeAuthor(user.id, user.displayName, aliasEnabled)
                    val color = FeishuDisguiseHelpers.imColor(name)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val existingDm = dmThreads.firstOrNull { it.peerId == user.id }
                                if (existingDm != null) onOpenDmThread(existingDm) else onStartNewDmUser(user)
                            }
                            .padding(horizontal = 16.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(color),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(FeishuDisguiseHelpers.avatarChars(name), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(name, color = fc.text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            Text("产品研发中心 · 点击发送消息", color = fc.muted, fontSize = 12.sp)
                        }
                    }
                    HorizontalDivider(color = fc.line, modifier = Modifier.padding(start = 68.dp))
                }
            } else {
                item {
                    Surface(color = fc.bg, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "常用同事 (${dmThreads.size})",
                            color = fc.muted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                }
                items(dmThreads, key = { "peer_${it.id}" }) { dm ->
                    val name = feishuSafeAuthor(dm.peerId, dm.peerName ?: dm.peerUsername, aliasEnabled)
                    val color = FeishuDisguiseHelpers.imColor(name)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenDmThread(dm) }
                            .padding(horizontal = 16.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(color),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(FeishuDisguiseHelpers.avatarChars(name), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(name, color = fc.text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            Text("内部同事 · 点击打开单聊", color = fc.muted, fontSize = 12.sp)
                        }
                    }
                    HorizontalDivider(color = fc.line, modifier = Modifier.padding(start = 68.dp))
                }

                item {
                    Surface(color = fc.bg, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "部门工作群 (${groups.size})",
                            color = fc.muted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                }
                items(groups, key = { "grp_${it.id}" }) { g ->
                    val alias = DisguiseSanitizer.resolveFeishuGroupAlias(g.id, g.name, customAliases)
                    val color = FeishuDisguiseHelpers.imColor(alias)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenGroup(g) }
                            .padding(horizontal = 16.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(color),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Rounded.Group, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(alias, color = fc.text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            Text("${maxOf(1, g.memberCount)} 名群成员", color = fc.muted, fontSize = 12.sp)
                        }
                    }
                    HorizontalDivider(color = fc.line, modifier = Modifier.padding(start = 68.dp))
                }
            }
        }
    }
}

@Composable
private fun FeishuMeTab(
    fc: FeishuThemePalette,
    me: MeUser?,
    scope: kotlinx.coroutines.CoroutineScope,
    api: ForHappyApiClient,
    aliasEnabled: Boolean,
    onRequireLogin: () -> Unit,
    onOpenSettings: (FeishuSettingsPage) -> Unit,
    onFilterFavorites: () -> Unit,
    onLoggedOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val displayName = if (me == null) {
        "点击登录企业账号"
    } else {
        feishuSafeAuthor(me.id, me.displayName, aliasEnabled)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .background(fc.bg),
        contentPadding = PaddingValues(vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Surface(
                color = fc.card,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = me == null) { onRequireLogin() },
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(fc.blue),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = FeishuDisguiseHelpers.avatarChars(displayName),
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(displayName, color = fc.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = if (me != null) "产品研发中心 · 在线工作区" else "企业协同办公空间",
                            color = fc.muted,
                            fontSize = 13.sp,
                        )
                    }
                }
            }
        }

        item {
            Surface(color = fc.card, modifier = Modifier.fillMaxWidth()) {
                Column {
                    FeishuMeMenuRow(Icons.Rounded.Bookmark, "我的收藏", "工作群与话题中标记的重点信息", fc) {
                        onFilterFavorites()
                    }
                    HorizontalDivider(color = fc.line, modifier = Modifier.padding(start = 52.dp))
                    FeishuMeMenuRow(Icons.Rounded.Lock, "隐私与安全", "图片遮罩、隐藏切换规则与锁屏文档预览", fc) {
                        onOpenSettings(FeishuSettingsPage.PRIVACY)
                    }
                    HorizontalDivider(color = fc.line, modifier = Modifier.padding(start = 52.dp))
                    FeishuMeMenuRow(Icons.Rounded.Settings, "设置", "通用界面风格、通知提醒与关于", fc) {
                        onOpenSettings(FeishuSettingsPage.ROOT)
                    }
                }
            }
        }

        if (me != null) {
            item {
                Surface(
                    color = fc.card,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            scope.launch {
                                api.logout()
                                onLoggedOut()
                            }
                        },
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 15.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("退出当前账号", color = fc.unreadRed, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
private fun FeishuMeMenuRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    fc: FeishuThemePalette,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, contentDescription = null, tint = fc.blue, modifier = Modifier.size(22.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = fc.text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = fc.muted, fontSize = 12.sp)
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = fc.muted)
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun FeishuWorkGroupDetailScreen(
    title: String,
    dayFilter: String?,
    tagFilter: String?,
    api: ForHappyApiClient,
    privacyConfig: PrivacyStealthConfig,
    onNotify: (String) -> Unit,
    onOpenTagTopic: (String) -> Unit,
    onBack: () -> Unit,
) {
    val fc = rememberFeishuColors()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val bossKeyActive = LocalBossKeyActive.current

    var messages by remember(dayFilter, tagFilter) { mutableStateOf<List<MemeItem>>(emptyList()) }
    var loading by remember(dayFilter, tagFilter) { mutableStateOf(true) }
    var unblurredIds by remember(dayFilter, tagFilter) { mutableStateOf<Set<Long>>(emptySet()) }

    var inputText by remember(dayFilter, tagFilter) { mutableStateOf("") }
    var showEmojiDrawer by remember { mutableStateOf(false) }
    var quotedMeme by remember { mutableStateOf<MemeItem?>(null) }
    val stagedItems = remember { mutableStateListOf<ComposeTrayItem>() }
    var mergeAll by remember { mutableStateOf(true) }
    var spoilerFlag by remember { mutableStateOf(false) }
    var sending by remember { mutableStateOf(false) }

    var actionSheetMeme by remember { mutableStateOf<MemeItem?>(null) }
    var threadRootMeme by remember { mutableStateOf<MemeItem?>(null) }
    var maskEditorIndex by remember { mutableStateOf<Int?>(null) }
    var viewerState by remember { mutableStateOf<Pair<List<MemeMediaItem>, Int>?>(null) }

    fun reloadMessages() {
        scope.launch {
            loading = true
            val list = api.fetchMemes(
                limit = 40,
                day = dayFilter,
                tag = tagFilter,
                sort = "new",
            )
            messages = list
            loading = false
        }
    }

    LaunchedEffect(dayFilter, tagFilter) {
        reloadMessages()
    }

    // Re-blur images whose rows scrolled off screen when configured
    val visibleKeys by remember {
        derivedStateOf { listState.layoutInfo.visibleItemsInfo.map { it.key }.toSet() }
    }
    LaunchedEffect(visibleKeys, privacyConfig.autoReblurOnScrollOff) {
        if (privacyConfig.autoReblurOnScrollOff && unblurredIds.isNotEmpty()) {
            val retained = unblurredIds.filter { id -> id in visibleKeys }.toSet()
            if (retained.size != unblurredIds.size) {
                unblurredIds = retained
            }
        }
    }

    val pickVisualMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(12),
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        scope.launch {
            val added = uris.mapNotNull { uri -> ComposeMediaStager.stageUri(context, uri) }
            stagedItems.addAll(added.take(12 - stagedItems.size))
        }
    }

    BackHandler(enabled = threadRootMeme == null && maskEditorIndex == null) {
        onBack()
    }

    if (threadRootMeme != null) {
        FeishuThreadScreen(
            rootMeme = threadRootMeme!!,
            api = api,
            blurAll = privacyConfig.blurAllEnabled,
            aliasEnabled = privacyConfig.feishuMemberAliasEnabled,
            unblurredIds = unblurredIds,
            onRevealMeme = { id -> unblurredIds = unblurredIds + id },
            onOpenViewer = { mediaList, idx -> viewerState = mediaList to idx },
            onReactMeme = { targetMeme, emoji ->
                scope.launch {
                    val updated = api.reactToMeme(targetMeme.id, emoji)
                    if (updated != null) {
                        messages = messages.map { if (it.id == updated.id) updated else it }
                        if (threadRootMeme?.id == updated.id) threadRootMeme = updated
                    }
                }
            },
            onMemeCommentAdded = { memeId, newCount ->
                messages = messages.map { m ->
                    if (m.id == memeId) m.copy(comments = newCount, commentCount = newCount) else m
                }
            },
            onClose = { threadRootMeme = null },
        )
        viewerState?.let { (mediaItems, idx) ->
            ImmersiveMediaStage(
                mediaItems = mediaItems,
                initialIndex = idx,
                api = api,
                forceBlurAll = false,
                onDismiss = { viewerState = null },
            )
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(fc.bg)
            .statusBarsPadding(),
    ) {
        Surface(color = fc.card, modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回", tint = fc.text)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            color = fc.text,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "${messages.size} 条部门同步消息",
                            color = fc.muted,
                            fontSize = 11.sp,
                        )
                    }
                }
                HorizontalDivider(color = fc.line)
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!dayFilter.isNullOrBlank()) {
                item(key = "day_sep") {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Surface(color = fc.surfaceMuted, shape = RoundedCornerShape(999.dp)) {
                            Text(
                                text = FeishuDisguiseHelpers.daySeparatorLabel(dayFilter),
                                color = fc.muted,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                            )
                        }
                    }
                }
            }

            if (loading && messages.isEmpty()) {
                item(key = "loading") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = fc.blue, strokeWidth = 2.dp, modifier = Modifier.size(26.dp))
                    }
                }
            } else if (messages.isEmpty()) {
                item(key = "empty") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("暂无群消息，在下方发送第一条消息", color = fc.muted, fontSize = 13.sp)
                    }
                }
            }

            items(messages, key = { it.id }) { meme ->
                val authorName = feishuSafeAuthor(meme.authorId ?: meme.id, meme.authorName ?: meme.authorUsername, privacyConfig.feishuMemberAliasEnabled)
                val avatarColor = FeishuDisguiseHelpers.imColor(authorName)
                val commentTotal = maxOf(meme.comments, meme.commentCount)
                val boosts = meme.replyList.ifEmpty { meme.replies }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(avatarColor),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = FeishuDisguiseHelpers.avatarChars(authorName),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(authorName, color = fc.muted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Text(FeishuDisguiseHelpers.formatClock(meme.createdAt), color = fc.muted, fontSize = 11.sp)
                        }

                        Surface(
                            color = fc.card,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(0.5.dp, fc.line),
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {},
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        actionSheetMeme = meme
                                    },
                                ),
                        ) {
                            Column(
                                modifier = Modifier.padding(11.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                RichChatBodyView(
                                    rawBody = feishuMemePrimaryText(meme),
                                    imageLoader = api.imageLoader,
                                    textColor = fc.text,
                                    showInlineImages = false,
                                )

                                DisguiseMediaStrip(
                                    meme = meme,
                                    api = api,
                                    blurAll = privacyConfig.blurAllEnabled,
                                    accentColor = fc.blue,
                                    isItemUnblurred = meme.id in unblurredIds,
                                    onRevealItem = { unblurredIds = unblurredIds + meme.id },
                                    onOpenViewer = { list, idx -> viewerState = list to idx },
                                )

                                if (meme.tags.isNotEmpty()) {
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        meme.tags.take(4).forEach { tag ->
                                            Surface(
                                                color = fc.surfaceMuted,
                                                shape = RoundedCornerShape(4.dp),
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .clickable { onOpenTagTopic(tag) },
                                            ) {
                                                Text(
                                                    text = "#${DisguiseSanitizer.sanitizeSummaryForDisguise(tag, AppDisguiseMode.FEISHU)}",
                                                    color = fc.blue,
                                                    fontSize = 11.sp,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                )
                                            }
                                        }
                                    }
                                }

                                if (meme.reactions.isNotEmpty() || boosts.isNotEmpty()) {
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
                                                            messages = messages.map { if (it.id == updated.id) updated else it }
                                                        }
                                                    }
                                                },
                                            )
                                        }
                                        boosts.forEach { reply ->
                                            Surface(color = fc.surfaceMuted, shape = RoundedCornerShape(999.dp)) {
                                                Text(
                                                    text = DisguiseSanitizer.sanitizeSummaryForDisguise(reply.text, AppDisguiseMode.FEISHU) +
                                                        if (reply.n > 1) " ×${reply.n}" else "",
                                                    color = fc.text,
                                                    fontSize = 11.sp,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                )
                                            }
                                        }
                                    }
                                }

                                HorizontalDivider(color = fc.line)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (commentTotal > 0) "话题 ($commentTotal)" else "话题回复",
                                            color = fc.blue,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.clickable { threadRootMeme = meme },
                                        )
                                        Text(
                                            text = "引用回复",
                                            color = fc.muted,
                                            fontSize = 12.sp,
                                            modifier = Modifier.clickable { quotedMeme = meme },
                                        )
                                        Text(
                                            text = if (meme.faved) "已收藏" else "收藏",
                                            color = if (meme.faved) fc.blue else fc.muted,
                                            fontSize = 12.sp,
                                            modifier = Modifier.clickable {
                                                scope.launch {
                                                    val ok = api.toggleFav(meme.id, meme.faved)
                                                    if (ok) {
                                                        messages = messages.map {
                                                            if (it.id == meme.id) it.copy(faved = !meme.faved) else it
                                                        }
                                                    }
                                                }
                                            },
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Rounded.MoreHoriz,
                                        contentDescription = "更多操作",
                                        tint = fc.muted,
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clickable { actionSheetMeme = meme },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        FeishuWorkGroupInputBar(
            groupTitle = title,
            api = api,
            text = inputText,
            onTextChange = { inputText = it },
            stagedItems = stagedItems,
            mergeAll = mergeAll,
            spoiler = spoilerFlag,
            quotedMeme = quotedMeme,
            sending = sending,
            showEmojiDrawer = showEmojiDrawer,
            aliasEnabled = privacyConfig.feishuMemberAliasEnabled,
            onToggleEmojiDrawer = { showEmojiDrawer = !showEmojiDrawer },
            onPickPhotos = {
                StealthExemptionGate.markInternalJump()
                pickVisualMediaLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo),
                )
            },
            onEditMask = { idx -> maskEditorIndex = idx },
            onRemoveStaged = { id -> stagedItems.removeAll { it.id == id } },
            onToggleMerge = { mergeAll = it },
            onToggleSpoiler = { spoilerFlag = it },
            onClearQuote = { quotedMeme = null },
            onInsertPhrase = { phrase ->
                inputText = if (inputText.isBlank()) phrase else "$inputText $phrase"
            },
            onSend = {
                sending = true
                val quote = quotedMeme
                val draft = inputText.trim()
                val stagedSnapshot = stagedItems.toList()
                val tags = if (!tagFilter.isNullOrBlank()) listOf(tagFilter) else emptyList()
                scope.launch {
                    if (quote != null && stagedSnapshot.isEmpty()) {
                        val ok = api.postMemeComment(quote.id, draft)
                        sending = false
                        if (ok) {
                            inputText = ""
                            quotedMeme = null
                            showEmojiDrawer = false
                            messages = messages.map { m ->
                                if (m.id == quote.id) m.copy(comments = m.comments + 1, commentCount = m.commentCount + 1) else m
                            }
                            onNotify("已发送话题回复")
                        } else {
                            onNotify("发送失败，请确认账号已登录")
                        }
                    } else {
                        val res = disguiseUploadAndPublish(
                            context = context,
                            api = api,
                            text = draft,
                            stagedItems = stagedSnapshot,
                            mergeAll = mergeAll,
                            spoiler = spoilerFlag,
                            tags = tags,
                            fallbackTitlePrefix = "工作同步截图",
                        )
                        sending = false
                        res.fold(
                            onSuccess = {
                                inputText = ""
                                stagedItems.clear()
                                quotedMeme = null
                                spoilerFlag = false
                                showEmojiDrawer = false
                                reloadMessages()
                                onNotify("已同步到工作群")
                            },
                            onFailure = { err ->
                                onNotify(
                                    DisguiseSanitizer.sanitizeSummaryForDisguise(
                                        err.message ?: "发送失败",
                                        AppDisguiseMode.FEISHU,
                                    ),
                                )
                            },
                        )
                    }
                }
            },
        )
    }

    actionSheetMeme?.let { sheetMeme ->
        FeishuMemeMessageActionSheet(
            meme = sheetMeme,
            api = api,
            onReactEmoji = { emoji ->
                scope.launch {
                    val updated = api.reactToMeme(sheetMeme.id, emoji)
                    if (updated != null) {
                        messages = messages.map { if (it.id == updated.id) updated else it }
                    }
                }
            },
            onQuickBoost = { phrase ->
                scope.launch {
                    api.putMemeQuickReply(sheetMeme.id, phrase).onSuccess { updated ->
                        if (updated != null) {
                            messages = messages.map { if (it.id == updated.id) updated else it }
                        } else {
                            reloadMessages()
                        }
                    }
                }
            },
            onReplyQuote = { quotedMeme = sheetMeme },
            onOpenThread = { threadRootMeme = sheetMeme },
            onToggleFav = {
                scope.launch {
                    if (api.toggleFav(sheetMeme.id, sheetMeme.faved)) {
                        messages = messages.map {
                            if (it.id == sheetMeme.id) it.copy(faved = !sheetMeme.faved) else it
                        }
                    }
                }
            },
            onNotify = onNotify,
            onDismiss = { actionSheetMeme = null },
        )
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

    viewerState?.let { (mediaItems, idx) ->
        ImmersiveMediaStage(
            mediaItems = mediaItems,
            initialIndex = idx,
            api = api,
            forceBlurAll = false,
            onDismiss = { viewerState = null },
        )
    }
}

@Composable
private fun FeishuRealChatShell(
    title: String,
    subtitle: String,
    isGroup: Boolean,
    groupId: Long?,
    dmThreadId: Long?,
    currentAlias: String,
    api: ForHappyApiClient,
    chatController: ChatSessionController,
    me: MeUser?,
    onSaveAlias: (String) -> Unit,
    onOpenMemeThread: (Long) -> Unit,
    onOpenUrl: (String) -> Unit,
    onRequireLogin: () -> Unit,
    onBack: () -> Unit,
) {
    val fc = rememberFeishuColors()
    val bossKeyActive = LocalBossKeyActive.current
    var showMenu by remember { mutableStateOf(false) }
    var showAliasDialog by remember { mutableStateOf(false) }
    var aliasDraft by remember(currentAlias) { mutableStateOf(currentAlias) }

    LaunchedEffect(groupId) {
        if (isGroup && groupId != null) {
            chatController.setActiveGroup(groupId)
        }
    }

    BackHandler(onBack = onBack)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(fc.bg)
            .statusBarsPadding(),
    ) {
        Surface(color = fc.card, modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回", tint = fc.text)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            color = fc.text,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(subtitle, color = fc.muted, fontSize = 11.sp)
                    }
                    if (isGroup) {
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Rounded.MoreHoriz, contentDescription = "群设置", tint = fc.text)
                            }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text("修改群名备注") },
                                    onClick = {
                                        showMenu = false
                                        aliasDraft = currentAlias
                                        showAliasDialog = true
                                    },
                                )
                            }
                        }
                    }
                }
                HorizontalDivider(color = fc.line)
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
                onNavigateToMeme = onOpenMemeThread,
                onNavigateToTag = {},
                onOpenUrl = onOpenUrl,
                onRequireLogin = onRequireLogin,
                hideChannelHeader = true,
                forcedChannelMode = if (isGroup) "group" else "dm",
                forcedDmThreadId = dmThreadId,
            )
        }
    }

    if (showAliasDialog && !bossKeyActive) {
        AlertDialog(
            onDismissRequest = { showAliasDialog = false },
            containerColor = fc.card,
            titleContentColor = fc.text,
            textContentColor = fc.text,
            title = { Text("设置工作群备注名", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) },
            text = {
                OutlinedTextField(
                    value = aliasDraft,
                    onValueChange = { aliasDraft = it },
                    placeholder = { Text("留空恢复系统默认群名", fontSize = 13.sp, color = fc.muted) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onSaveAlias(aliasDraft)
                        showAliasDialog = false
                    },
                ) {
                    Text("保存", color = fc.blue)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAliasDialog = false }) {
                    Text("取消", color = fc.muted)
                }
            },
        )
    }
}
