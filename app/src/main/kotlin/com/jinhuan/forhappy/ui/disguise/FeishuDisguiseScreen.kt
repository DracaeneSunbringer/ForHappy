package com.jinhuan.forhappy.ui.disguise

import android.graphics.RectF
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.PeopleOutline
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
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
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
import com.jinhuan.forhappy.core.seen.LocalSeenTracker
import com.jinhuan.forhappy.core.seen.ObserveMemeSeenDwell
import com.jinhuan.forhappy.core.seen.SeenTracker
import com.jinhuan.forhappy.core.seen.computeVisibleAreaRatio
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

    private val GROUP_SUFFIXES = listOf("同步群", "推进群", "沟通群", "专项群", "协同群", "对齐群")

    fun imConversationGroupTitle(day: String): String {
        val parsed = runCatching { LocalDate.parse(day) }.getOrNull() ?: return day
        val epochDay = parsed.toEpochDay()
        val grpIdx = Math.floorMod(epochDay, IM_GROUPS.size)
        val base = IM_GROUPS[grpIdx]
        val suffix = GROUP_SUFFIXES[Math.floorMod(epochDay * 7L + 3L, GROUP_SUFFIXES.size)]
        return if (base.endsWith("会") || base.endsWith("群")) "${base}沟通群" else "$base$suffix"
    }

    fun imGroupMemberCount(seedText: String): Int {
        return 18 + Math.floorMod(seedText.hashCode(), 46)
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
    val seenTracker = LocalSeenTracker.current
    val newlySeenByDayState = seenTracker?.newlySeenByDay?.collectAsState()
    val newlySeenByDay = newlySeenByDayState?.value ?: emptyMap()
    val adjustedDayGroups = remember(dayGroups, newlySeenByDay) {
        if (newlySeenByDay.isEmpty()) {
            dayGroups
        } else {
            dayGroups.map { d ->
                val dec = newlySeenByDay[d.day] ?: 0
                if (dec > 0) d.copy(unseen = (d.unseen - dec).coerceAtLeast(0)) else d
            }
        }
    }

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
        seenTracker?.resetDayDeltaCounter()
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
                                dayGroups = adjustedDayGroups,
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
                    title = FeishuDisguiseHelpers.imConversationGroupTitle(entry.day),
                    sortTs = entry.latest?.at ?: 0L,
                ),
            )
        }
        list.sortedByDescending { it.sortTs }
    }

    val totalUnreadBadge = remember(mergedConvs) {
        mergedConvs.sumOf { conv ->
            when (conv) {
                is FeishuConvTarget.RealGroup -> conv.group.unread
                is FeishuConvTarget.RealDm -> conv.dm.unread
                is FeishuConvTarget.DayWorkGroup -> conv.dayEntry.unseen
                is FeishuConvTarget.TagTopic -> 0
            }
        }
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
        // Authentic Feishu 7.x Android Top Header + Permanent Search Capsule + Filter Pills
        Surface(color = fc.card, modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clickable(onClick = onOpenMeTab),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(fc.blue),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = FeishuDisguiseHelpers.avatarChars(selfLabel).take(1),
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(9.5.dp)
                                .clip(CircleShape)
                                .background(fc.onlineGreen)
                                .border(1.5.dp, fc.card, CircleShape),
                        )
                    }
                    Text(
                        text = "消息",
                        color = fc.text,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = {
                            onSelectFilter(if (msgFilter == "unread") "all" else "unread")
                        },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FilterList,
                            contentDescription = "筛选未读",
                            tint = if (msgFilter == "unread") fc.blue else fc.text,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Box {
                        IconButton(onClick = { onTogglePlusMenu(true) }, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Rounded.AddCircleOutline,
                                contentDescription = "发起沟通",
                                tint = fc.text,
                                modifier = Modifier.size(21.dp),
                            )
                        }
                        DropdownMenu(
                            expanded = showPlusMenu,
                            onDismissRequest = { onTogglePlusMenu(false) },
                        ) {
                            DropdownMenuItem(
                                text = { Text("发起群聊 / 单聊") },
                                onClick = onJumpContacts,
                            )
                            DropdownMenuItem(
                                text = { Text("按话题标签浏览") },
                                onClick = onJumpTopicsFilter,
                            )
                        }
                    }
                }

                // Permanent Feishu Rounded Grey Search Capsule
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(fc.surfaceMuted)
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = fc.muted,
                        modifier = Modifier.size(17.dp),
                    )
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "搜索联系人、群组、消息记录",
                                color = fc.muted,
                                fontSize = 13.5.sp,
                                maxLines = 1,
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            singleLine = true,
                            textStyle = TextStyle(color = fc.text, fontSize = 13.5.sp),
                            cursorBrush = SolidColor(fc.blue),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "清除",
                            tint = fc.muted,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { onSearchQueryChange("") },
                        )
                    }
                }

                // Compact Feishu Filter Pills (`全部 / 未读 / @我 / 单聊 / 群组 / 话题`)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val unreadTabLabel = if (totalUnreadBadge > 0) {
                        "未读 ${if (totalUnreadBadge > 99) "99+" else "$totalUnreadBadge"}"
                    } else {
                        "未读"
                    }
                    val tabs = listOf(
                        "all" to "全部",
                        "unread" to unreadTabLabel,
                        "at" to "@我",
                        "dm" to "单聊",
                        "group" to "群组",
                        "topics" to "话题",
                    )
                    tabs.forEach { (id, label) ->
                        val active = msgFilter == id
                        Surface(
                            color = if (active) fc.reactionMineBg else Color.Transparent,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onSelectFilter(id) },
                        ) {
                            Text(
                                text = label,
                                color = if (active) fc.blue else fc.subText,
                                fontSize = 12.5.sp,
                                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
                HorizontalDivider(color = fc.line, thickness = 0.5.dp)
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
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(0.5.dp, fc.line),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            onSelectTarget(FeishuConvTarget.TagTopic(tagItem.tag, tagItem.totalCount))
                                        },
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Icon(Icons.Rounded.Tag, contentDescription = null, tint = fc.blue, modifier = Modifier.size(14.dp))
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
                    HorizontalDivider(
                        color = fc.line,
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(start = 72.dp),
                    )
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
                "${feishuSafeAuthor(g.id, raw, aliasEnabled)}："
            }.orEmpty()
            val rawBody = g.latest?.text?.takeIf { it.isNotBlank() } ?: g.desc.orEmpty().ifBlank { "群消息同步" }
            val safeBody = DisguiseSanitizer.sanitizeSummaryForDisguise(rawBody, AppDisguiseMode.FEISHU)
            ConvDisplay(
                title = conv.displayTitle,
                preview = "$latestBy$safeBody",
                timeText = FeishuDisguiseHelpers.formatConvTime(conv.sortTs),
                unread = g.unread,
                atMe = g.atMe,
                tagLabel = "部门",
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
            val rawText = d.latest?.text?.takeIf { it.isNotBlank() }
            val safeBody = if (rawText != null) {
                val cleaned = DisguiseSanitizer.sanitizeSummaryForDisguise(rawText, AppDisguiseMode.FEISHU).trim()
                cleaned.ifBlank { "[图片]" }
            } else {
                "[图片]"
            }
            val prefix = if (!latestAuthor.isNullOrBlank()) "$latestAuthor：" else ""
            val fallbackTs = conv.sortTs.takeIf { it > 0L } ?: runCatching {
                LocalDate.parse(d.day).atTime(18, 0).toEpochSecond(java.time.ZoneOffset.ofHours(8))
            }.getOrDefault(0L)
            ConvDisplay(
                title = conv.title,
                preview = "$prefix$safeBody",
                timeText = FeishuDisguiseHelpers.formatConvTime(fallbackTs),
                unread = d.unseen,
                atMe = 0,
                tagLabel = null,
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
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(modifier = Modifier.size(46.dp)) {
            when (conv) {
                is FeishuConvTarget.RealDm -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(avatarBg),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = FeishuDisguiseHelpers.avatarChars(title).take(1),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(fc.onlineGreen)
                            .border(1.5.dp, fc.card, CircleShape),
                    )
                }

                is FeishuConvTarget.RealGroup -> {
                    FeishuGroupMosaicAvatar(
                        seedKey = "rg_${conv.group.id}_$title",
                        fc = fc,
                        size = 46.dp,
                    )
                }

                is FeishuConvTarget.DayWorkGroup -> {
                    FeishuGroupMosaicAvatar(
                        seedKey = "dg_${conv.dayEntry.day}_$title",
                        fc = fc,
                        size = 46.dp,
                    )
                }

                is FeishuConvTarget.TagTopic -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(10.dp))
                            .background(fc.reactionMineBg),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Tag, contentDescription = null, tint = fc.blue, modifier = Modifier.size(20.dp))
                    }
                }
            }

            if (unreadCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 5.dp, y = (-4).dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(fc.unreadRed)
                        .border(1.5.dp, fc.card, RoundedCornerShape(999.dp))
                        .padding(horizontal = 5.dp, vertical = 1.dp),
                ) {
                    Text(
                        text = if (unreadCount > 99) "99+" else "$unreadCount",
                        color = Color.White,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(
                        text = title,
                        color = fc.text,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (!badgeLabel.isNullOrBlank()) {
                        Surface(
                            color = fc.reactionMineBg,
                            shape = RoundedCornerShape(3.dp),
                        ) {
                            Text(
                                text = badgeLabel,
                                color = fc.blue,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 0.5.dp),
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
                    Text("[有人@我]", color = fc.unreadRed, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
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
                        .height(48.dp)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("通讯录", color = fc.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(fc.surfaceMuted)
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Icon(Icons.Rounded.Search, contentDescription = null, tint = fc.muted, modifier = Modifier.size(17.dp))
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) {
                            Text("搜索组织内联系人发起单聊", color = fc.muted, fontSize = 13.5.sp)
                        }
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            singleLine = true,
                            textStyle = TextStyle(color = fc.text, fontSize = 13.5.sp),
                            cursorBrush = SolidColor(fc.blue),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    if (query.isNotEmpty()) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "清除",
                            tint = fc.muted,
                            modifier = Modifier.size(16.dp).clickable { query = "" },
                        )
                    }
                }
                HorizontalDivider(color = fc.line, thickness = 0.5.dp)
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
                            Text(FeishuDisguiseHelpers.avatarChars(name).take(1), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(name, color = fc.text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            Text("产品研发中心 · 点击发送消息", color = fc.muted, fontSize = 12.sp)
                        }
                    }
                    HorizontalDivider(color = fc.line, thickness = 0.5.dp, modifier = Modifier.padding(start = 68.dp))
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
                            Text(FeishuDisguiseHelpers.avatarChars(name).take(1), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(name, color = fc.text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            Text("产品研发中心 · 在线", color = fc.muted, fontSize = 12.sp)
                        }
                    }
                    HorizontalDivider(color = fc.line, thickness = 0.5.dp, modifier = Modifier.padding(start = 68.dp))
                }

                item {
                    Surface(color = fc.bg, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "我的群组 (${groups.size})",
                            color = fc.muted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                }
                items(groups, key = { "grp_${it.id}" }) { g ->
                    val alias = DisguiseSanitizer.resolveFeishuGroupAlias(g.id, g.name, customAliases)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenGroup(g) }
                            .padding(horizontal = 16.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        FeishuGroupMosaicAvatar(
                            seedKey = "rg_${g.id}_$alias",
                            fc = fc,
                            size = 40.dp,
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(alias, color = fc.text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            Text("${maxOf(1, g.memberCount)} 名群成员", color = fc.muted, fontSize = 12.sp)
                        }
                    }
                    HorizontalDivider(color = fc.line, thickness = 0.5.dp, modifier = Modifier.padding(start = 68.dp))
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
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Surface(
                color = fc.card,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = me == null) { onRequireLogin() },
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Box(modifier = Modifier.size(58.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(fc.blue),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = FeishuDisguiseHelpers.avatarChars(displayName).take(1),
                                    color = Color.White,
                                    fontSize = 21.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            if (me != null) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(fc.onlineGreen)
                                        .border(2.dp, fc.card, CircleShape),
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(displayName, color = fc.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                FeishuBrandMark(size = 15.dp)
                                Text(
                                    text = if (me != null) "产品研发协同中心 · 企业认证" else "企业协同办公空间",
                                    color = fc.subText,
                                    fontSize = 12.5.sp,
                                )
                            }
                        }
                    }
                    if (me != null) {
                        Surface(
                            color = fc.surfaceMuted,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(fc.onlineGreen),
                                )
                                Text("在线 · 正常接收工作通知", color = fc.subText, fontSize = 12.sp)
                            }
                        }
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
    val seenTracker = LocalSeenTracker.current
    var feedViewportRect by remember { mutableStateOf(RectF()) }

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
    var viewerState by remember { mutableStateOf<Triple<List<MemeMediaItem>, Int, MemeItem?>?>(null) }

    fun reloadMessages() {
        scope.launch {
            loading = true
            val list = api.fetchMemes(
                limit = 40,
                day = dayFilter,
                tag = tagFilter,
                sort = "new",
            )
            seenTracker?.ingestFetchedMemes(list)
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
        val activeRoot = threadRootMeme!!
        LaunchedEffect(activeRoot.id, bossKeyActive) {
            if (!bossKeyActive) {
                seenTracker?.markSeen(activeRoot.id, activeRoot.seenAt, activeRoot.resolvedDay)
            }
        }
        FeishuThreadScreen(
            rootMeme = activeRoot,
            api = api,
            blurAll = privacyConfig.blurAllEnabled,
            aliasEnabled = privacyConfig.feishuMemberAliasEnabled,
            unblurredIds = unblurredIds,
            onRevealMeme = { id -> unblurredIds = unblurredIds + id },
            onOpenViewer = { mediaList, idx -> viewerState = Triple(mediaList, idx, activeRoot) },
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
        return
    }

    val density = LocalDensity.current
    val memberCount = remember(title, dayFilter) {
        FeishuDisguiseHelpers.imGroupMemberCount("${dayFilter.orEmpty()}_$title")
    }
    var showGroupSearch by remember { mutableStateOf(false) }
    var groupSearchQuery by remember { mutableStateOf("") }
    var showGroupMenu by remember { mutableStateOf(false) }
    var unseenOnlyFilter by remember { mutableStateOf(false) }

    val displayedMessages = remember(messages, groupSearchQuery, unseenOnlyFilter, seenTracker) {
        val q = groupSearchQuery.trim()
        messages.filter { meme ->
            if (unseenOnlyFilter && seenTracker?.isSeen(meme.id, meme.seenAt) == true) return@filter false
            if (q.isBlank()) return@filter true
            val body = feishuMemeOptionalText(meme).orEmpty()
            val author = feishuSafeAuthor(meme.authorId ?: meme.id, meme.authorName ?: meme.authorUsername, privacyConfig.feishuMemberAliasEnabled)
            body.contains(q, ignoreCase = true) ||
                author.contains(q, ignoreCase = true) ||
                meme.tags.any { it.contains(q, ignoreCase = true) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(fc.bg)
            .statusBarsPadding(),
    ) {
        // Authentic Feishu 7.x Android Group Chat Top Bar
        Surface(color = fc.card, modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(38.dp)) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回", tint = fc.text)
                    }
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Text(
                            text = if (tagFilter == null) "$title ($memberCount)" else title,
                            color = fc.text,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Surface(
                            color = fc.reactionMineBg,
                            shape = RoundedCornerShape(3.dp),
                        ) {
                            Text(
                                text = if (tagFilter == null) "部门" else "话题",
                                color = fc.blue,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 0.5.dp),
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            showGroupSearch = !showGroupSearch
                            if (!showGroupSearch) groupSearchQuery = ""
                        },
                        modifier = Modifier.size(34.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "搜索聊天记录",
                            tint = if (showGroupSearch) fc.blue else fc.text,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    IconButton(
                        onClick = { onNotify("内部工作群 · $memberCount 名同事在线") },
                        modifier = Modifier.size(34.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PeopleOutline,
                            contentDescription = "群成员",
                            tint = fc.text,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Box {
                        IconButton(onClick = { showGroupMenu = true }, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.Rounded.MoreHoriz, contentDescription = "群选项", tint = fc.text, modifier = Modifier.size(20.dp))
                        }
                        DropdownMenu(
                            expanded = showGroupMenu,
                            onDismissRequest = { showGroupMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (unseenOnlyFilter) "显示全部消息" else "仅看未读消息") },
                                onClick = {
                                    showGroupMenu = false
                                    unseenOnlyFilter = !unseenOnlyFilter
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("刷新同步记录") },
                                onClick = {
                                    showGroupMenu = false
                                    reloadMessages()
                                },
                            )
                        }
                    }
                }
                if (showGroupSearch) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .height(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(fc.surfaceMuted)
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(Icons.Rounded.Search, contentDescription = null, tint = fc.muted, modifier = Modifier.size(16.dp))
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                            if (groupSearchQuery.isEmpty()) {
                                Text("搜索本群聊天记录", color = fc.muted, fontSize = 13.sp)
                            }
                            BasicTextField(
                                value = groupSearchQuery,
                                onValueChange = { groupSearchQuery = it },
                                singleLine = true,
                                textStyle = TextStyle(color = fc.text, fontSize = 13.sp),
                                cursorBrush = SolidColor(fc.blue),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        if (groupSearchQuery.isNotEmpty()) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = "清除",
                                tint = fc.muted,
                                modifier = Modifier.size(15.dp).clickable { groupSearchQuery = "" },
                            )
                        }
                    }
                }
                HorizontalDivider(color = fc.line, thickness = 0.5.dp)
            }
        }

        val rightEdgeGuardPx = with(density) { 32.dp.toPx() }
        val quoteTriggerPx = with(density) { 44.dp.toPx() }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .onGloballyPositioned { coords ->
                    val b = coords.boundsInWindow()
                    feedViewportRect = RectF(b.left, b.top, b.right, b.bottom)
                },
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!dayFilter.isNullOrBlank()) {
                item(key = "day_sep") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = FeishuDisguiseHelpers.daySeparatorLabel(dayFilter),
                            color = fc.muted,
                            fontSize = 11.5.sp,
                        )
                    }
                }
            }

            if (loading && displayedMessages.isEmpty()) {
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
            } else if (displayedMessages.isEmpty()) {
                item(key = "empty") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (unseenOnlyFilter) "本群未读消息已全部读完" else "暂无群消息，在下方发送第一条消息",
                            color = fc.muted,
                            fontSize = 13.sp,
                        )
                    }
                }
            }

            items(displayedMessages, key = { it.id }) { meme ->
                val authorName = feishuSafeAuthor(
                    meme.authorId ?: meme.id,
                    meme.authorName ?: meme.authorUsername,
                    privacyConfig.feishuMemberAliasEnabled,
                )
                val avatarColor = FeishuDisguiseHelpers.imColor(authorName)
                val commentTotal = maxOf(meme.comments, meme.commentCount)
                val boosts = meme.replyList.ifEmpty { meme.replies }
                val hasMedia = remember(meme.id, meme.images, meme.image, meme.thumb, meme.media) { meme.mediaItems.isNotEmpty() }
                val msgText = remember(meme) { feishuMemeOptionalText(meme) }
                var mediaReady by remember(meme.id, hasMedia) { mutableStateOf(!hasMedia) }
                var itemTopPx by remember(meme.id) { mutableFloatStateOf(0f) }
                var itemBottomPx by remember(meme.id) { mutableFloatStateOf(0f) }
                var dragX by remember(meme.id) { mutableFloatStateOf(0f) }
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

                val bubbleShape = RoundedCornerShape(
                    topStart = 2.dp,
                    topEnd = 10.dp,
                    bottomEnd = 10.dp,
                    bottomStart = 10.dp,
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset { IntOffset(dragX.roundToInt(), 0) }
                        .pointerInput(meme.id, rightEdgeGuardPx, quoteTriggerPx) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                if (down.position.x > size.width - rightEdgeGuardPx) {
                                    return@awaitEachGesture
                                }
                                val slop = awaitHorizontalTouchSlopOrCancellation(down.id) { change, over ->
                                    if (over < 0f) change.consume()
                                }
                                if (slop != null) {
                                    dragX = 0f
                                    horizontalDrag(slop.id) { change ->
                                        val dx = change.positionChange().x
                                        dragX = (dragX + dx).coerceIn(-quoteTriggerPx * 1.4f, 0f)
                                        change.consume()
                                    }
                                    if (dragX <= -quoteTriggerPx) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        quotedMeme = meme
                                    }
                                    dragX = 0f
                                }
                            }
                        }
                        .onGloballyPositioned { coords ->
                            val b = coords.boundsInWindow()
                            itemTopPx = b.top
                            itemBottomPx = b.bottom
                        },
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(avatarColor),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = FeishuDisguiseHelpers.avatarChars(authorName).take(1),
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    Column(
                        modifier = Modifier.widthIn(max = 268.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        // Colleague name + timestamp + subtle action menu trigger
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = authorName,
                                color = fc.subText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                text = FeishuDisguiseHelpers.formatClock(meme.createdAt),
                                color = fc.muted,
                                fontSize = 10.5.sp,
                            )
                            if (meme.faved) {
                                Icon(
                                    imageVector = Icons.Rounded.Bookmark,
                                    contentDescription = "已收藏",
                                    tint = fc.blue,
                                    modifier = Modifier.size(12.dp),
                                )
                            }
                            Icon(
                                imageVector = Icons.Rounded.MoreHoriz,
                                contentDescription = "消息操作",
                                tint = fc.muted.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .size(15.dp)
                                    .clickable { actionSheetMeme = meme },
                            )
                        }

                        // Authentic Feishu IM Message Content:
                        // 1) Pure media -> standalone 65% width rounded thumbnail / 9-grid (NO outer white box!)
                        // 2) Text or Rich-Post (Text + Media) -> compact borderless Feishu bubble
                        if (msgText == null && hasMedia) {
                            Box(
                                modifier = Modifier.combinedClickable(
                                    onClick = {},
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        actionSheetMeme = meme
                                    },
                                ),
                            ) {
                                DisguiseMediaStrip(
                                    meme = meme,
                                    api = api,
                                    blurAll = privacyConfig.blurAllEnabled,
                                    accentColor = fc.blue,
                                    isItemUnblurred = meme.id in unblurredIds,
                                    onRevealItem = { unblurredIds = unblurredIds + meme.id },
                                    onOpenViewer = { list, idx -> viewerState = Triple(list, idx, meme) },
                                    onMediaLoaded = { mediaReady = true },
                                    compactImMode = true,
                                )
                            }
                        } else {
                            Surface(
                                color = fc.peerBubble,
                                shape = bubbleShape,
                                modifier = Modifier.combinedClickable(
                                    onClick = {},
                                    onDoubleClick = {
                                        scope.launch {
                                            val updated = api.reactToMeme(meme.id, "👍")
                                            if (updated != null) {
                                                messages = messages.map { if (it.id == updated.id) updated else it }
                                            }
                                        }
                                    },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        actionSheetMeme = meme
                                    },
                                ),
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(7.dp),
                                ) {
                                    if (!msgText.isNullOrBlank()) {
                                        RichChatBodyView(
                                            rawBody = msgText,
                                            imageLoader = api.imageLoader,
                                            textColor = fc.text,
                                            showInlineImages = false,
                                            onLongPress = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                actionSheetMeme = meme
                                            },
                                        )
                                    }
                                    if (hasMedia) {
                                        DisguiseMediaStrip(
                                            meme = meme,
                                            api = api,
                                            blurAll = privacyConfig.blurAllEnabled,
                                            accentColor = fc.blue,
                                            isItemUnblurred = meme.id in unblurredIds,
                                            onRevealItem = { unblurredIds = unblurredIds + meme.id },
                                            onOpenViewer = { list, idx -> viewerState = Triple(list, idx, meme) },
                                            onMediaLoaded = { mediaReady = true },
                                            compactImMode = true,
                                        )
                                    }
                                    if (meme.tags.isNotEmpty()) {
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp),
                                        ) {
                                            meme.tags.take(3).forEach { tag ->
                                                Text(
                                                    text = "#${DisguiseSanitizer.sanitizeSummaryForDisguise(tag, AppDisguiseMode.FEISHU)}",
                                                    color = fc.blue,
                                                    fontSize = 12.sp,
                                                    modifier = Modifier.clickable { onOpenTagTopic(tag) },
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Compact Feishu Emoji Reaction & Quick-Stamp Pills directly below bubble/media
                        val activeReactions = remember(meme.reactions) { meme.reactions.filter { it.count > 0 } }
                        if (activeReactions.isNotEmpty() || boosts.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                activeReactions.forEach { reaction ->
                                    val safeEmoji = DisguiseSanitizer.mapStickerToSafeEmoji(reaction.emoji)
                                    Surface(
                                        color = if (reaction.mine) fc.reactionMineBg else fc.reactionBg,
                                        shape = RoundedCornerShape(999.dp),
                                        border = if (reaction.mine) BorderStroke(0.8.dp, fc.blue.copy(alpha = 0.65f)) else null,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .clickable {
                                                scope.launch {
                                                    val updated = api.reactToMeme(meme.id, reaction.emoji)
                                                    if (updated != null) {
                                                        messages = messages.map { if (it.id == updated.id) updated else it }
                                                    }
                                                }
                                            },
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                                        ) {
                                            Text(safeEmoji, fontSize = 12.sp)
                                            Text(
                                                text = "${reaction.count}",
                                                color = if (reaction.mine) fc.blue else fc.subText,
                                                fontSize = 11.sp,
                                                fontWeight = if (reaction.mine) FontWeight.SemiBold else FontWeight.Normal,
                                            )
                                        }
                                    }
                                }
                                boosts.forEach { reply ->
                                    val cleanReply = DisguiseSanitizer.sanitizeSummaryForDisguise(reply.text, AppDisguiseMode.FEISHU)
                                    Surface(
                                        color = fc.reactionBg,
                                        shape = RoundedCornerShape(999.dp),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .clickable {
                                                scope.launch {
                                                    api.putMemeQuickReply(meme.id, reply.text).onSuccess { updated ->
                                                        if (updated != null) {
                                                            messages = messages.map { if (it.id == updated.id) updated else it }
                                                        } else {
                                                            reloadMessages()
                                                        }
                                                    }
                                                }
                                            },
                                    ) {
                                        Text(
                                            text = cleanReply + if (reply.n > 1) " · ${reply.n}" else "",
                                            color = fc.subText,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.5.dp),
                                        )
                                    }
                                }
                            }
                        }

                        // Authentic Feishu Thread Bar (`| 💬 N 条回复 ›`)
                        if (commentTotal > 0) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { threadRootMeme = meme }
                                    .padding(vertical = 2.dp, horizontal = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .height(13.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(fc.blue.copy(alpha = 0.78f)),
                                )
                                Icon(
                                    imageVector = Icons.Rounded.ChatBubbleOutline,
                                    contentDescription = null,
                                    tint = fc.blue,
                                    modifier = Modifier.size(13.dp),
                                )
                                Text(
                                    text = "$commentTotal 条回复",
                                    color = fc.blue,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = fc.blue,
                                    modifier = Modifier.size(14.dp),
                                )
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
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(38.dp)) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回", tint = fc.text)
                    }
                    if (isGroup) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Text(
                                text = title,
                                color = fc.text,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            Surface(color = fc.reactionMineBg, shape = RoundedCornerShape(3.dp)) {
                                Text(
                                    text = "部门",
                                    color = fc.blue,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 0.5.dp),
                                )
                            }
                        }
                        Box {
                            IconButton(onClick = { showMenu = true }, modifier = Modifier.size(34.dp)) {
                                Icon(Icons.Rounded.MoreHoriz, contentDescription = "群设置", tint = fc.text, modifier = Modifier.size(20.dp))
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
                    } else {
                        val avatarColor = remember(title) { FeishuDisguiseHelpers.imColor(title) }
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(avatarColor),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = FeishuDisguiseHelpers.avatarChars(title).take(1),
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                            Column {
                                Text(
                                    text = title,
                                    color = fc.text,
                                    fontSize = 15.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(fc.onlineGreen),
                                    )
                                    Text(subtitle, color = fc.muted, fontSize = 10.5.sp)
                                }
                            }
                        }
                    }
                }
                HorizontalDivider(color = fc.line, thickness = 0.5.dp)
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
