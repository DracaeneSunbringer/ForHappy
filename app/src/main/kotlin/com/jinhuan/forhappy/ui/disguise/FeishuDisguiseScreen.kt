package com.jinhuan.forhappy.ui.disguise

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.Reply
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEmotions
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.jinhuan.forhappy.core.chat.ChatRichTextParser
import com.jinhuan.forhappy.core.chat.ChatSessionController
import com.jinhuan.forhappy.core.clock.ServerClock
import com.jinhuan.forhappy.core.model.ChatGroup
import com.jinhuan.forhappy.core.model.DayEntry
import com.jinhuan.forhappy.core.model.DmThread
import com.jinhuan.forhappy.core.model.MeUser
import com.jinhuan.forhappy.core.model.MemeItem
import com.jinhuan.forhappy.core.model.PICKED_REACTIONS
import com.jinhuan.forhappy.core.model.TagStatItem
import com.jinhuan.forhappy.core.network.api.ForHappyApiClient
import com.jinhuan.forhappy.core.network.media.MediaUrlRewriter
import com.jinhuan.forhappy.core.seen.LocalSeenTracker
import com.jinhuan.forhappy.core.seen.ObserveMemeSeenDwell
import com.jinhuan.forhappy.core.seen.SeenTracker
import com.jinhuan.forhappy.ui.chat.ChatNotificationJump
import com.jinhuan.forhappy.ui.chat.ChatScreen
import com.jinhuan.forhappy.ui.components.BoostPillText
import com.jinhuan.forhappy.ui.components.ForHappyAvatar
import com.jinhuan.forhappy.ui.components.ReactionChip
import com.jinhuan.forhappy.ui.components.ReactionEmojiView
import com.jinhuan.forhappy.ui.theme.LocalForHappyPalette
import com.jinhuan.forhappy.ui.theme.LocalPrivacyStealthConfig
import com.jinhuan.forhappy.ui.wall.ComposeMemeModal
import com.jinhuan.forhappy.ui.wall.FullscreenMediaOverlay
import com.jinhuan.forhappy.ui.wall.MemeDetailModal
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

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

    fun imWho(meme: MemeItem): String {
        return if (!meme.sourceUrl.isNullOrBlank()) {
            imFake(meme.id)
        } else {
            meme.authorDisplay
        }
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
        val sdf = SimpleDateFormat("HH:mm", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("GMT+08:00")
        return sdf.format(Date(ts * 1000L))
    }

    fun formatConvTime(ts: Long): String {
        if (ts <= 0L) return ""
        val msgCal = Calendar.getInstance(TimeZone.getTimeZone("GMT+08:00")).apply {
            timeInMillis = ts * 1000L
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
            msgMs >= todayStart -> formatClock(ts)
            msgMs >= todayStart - 86_400_000L -> "昨天"
            msgMs >= todayStart - 6 * 86_400_000L -> "周${WEEK_CHARS[(msgCal.get(Calendar.DAY_OF_WEEK) - 1).coerceIn(0, 6)]}"
            else -> "${msgCal.get(Calendar.MONTH) + 1}月${msgCal.get(Calendar.DAY_OF_MONTH)}日"
        }
    }
}

private sealed interface FeishuConvTarget {
    val key: String
    val title: String
    val unreadCount: Int

    data class RealGroup(
        val group: ChatGroup,
    ) : FeishuConvTarget {
        override val key: String = "grp:${group.id}"
        override val title: String = group.name
        override val unreadCount: Int = group.unread
    }

    data class DirectMsg(
        val thread: DmThread,
    ) : FeishuConvTarget {
        override val key: String = "dm:${thread.id}"
        override val title: String = thread.peerName?.takeIf { it.isNotBlank() }
            ?: thread.peerUsername?.takeIf { it.isNotBlank() }
            ?: "私信"
        override val unreadCount: Int = thread.unread
    }

    data class DayWorkGroup(
        val entry: DayEntry,
        val adjustedUnseen: Int,
    ) : FeishuConvTarget {
        override val key: String = "day:${entry.day}"
        override val title: String = FeishuDisguiseHelpers.imGroupName(entry.day)
        override val unreadCount: Int = adjustedUnseen
    }

    data class TagTopic(
        val stat: TagStatItem,
    ) : FeishuConvTarget {
        override val key: String = "tag:${stat.tag}"
        override val title: String = "#${stat.tag}"
        override val unreadCount: Int = 0
    }

    data class SearchResult(
        val query: String,
    ) : FeishuConvTarget {
        override val key: String = "search:$query"
        override val title: String = "搜索「$query」"
        override val unreadCount: Int = 0
    }
}

/**
 * Complete Feishu IM disguise shell matching `52dmeme.com/app.js` `IM`.
 * Hides the 5-tab bottom navigation bar cleanly and merges real group chats, DMs, and daily meme
 * feeds disguised as rotating work groups in one authentic Feishu conversation list.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FeishuDisguiseScreen(
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
    val newlySeenByDay by (seenTracker?.newlySeenByDay?.collectAsState() ?: remember { mutableStateOf(emptyMap()) })
    val scope = rememberCoroutineScope()

    var activeTab by remember { mutableStateOf("all") } // "all" | "unread" | "tag"
    var searchQuery by remember { mutableStateOf("") }
    var hideNsfw by remember { mutableStateOf(true) }
    var activeConv by remember { mutableStateOf<FeishuConvTarget?>(null) }

    var dayEntries by remember { mutableStateOf<List<DayEntry>>(emptyList()) }
    var tagStats by remember { mutableStateOf<List<TagStatItem>>(emptyList()) }
    var dmThreads by remember { mutableStateOf<List<DmThread>>(emptyList()) }
    var loadingConvs by remember { mutableStateOf(true) }

    var showCompose by remember { mutableStateOf(false) }
    var selectedDetailId by remember { mutableStateOf<Long?>(null) }
    var fullscreenImgUrl by remember { mutableStateOf<String?>(null) }
    var showConvMenu by remember { mutableStateOf(false) }

    fun reloadConversations() {
        scope.launch {
            loadingConvs = true
            val daysDef = async { api.fetchDays(limitDays = null, sort = "new", nsfwHide = hideNsfw) }
            val tagsDef = async { api.fetchTags(hideNsfw = hideNsfw) }
            val dmDef = async { if (me != null) api.fetchDmThreads() else emptyList() }
            if (me != null) {
                chatController.refreshGroups()
            }
            dayEntries = daysDef.await().days
            tagStats = tagsDef.await()
            dmThreads = dmDef.await()
            loadingConvs = false
        }
    }

    LaunchedEffect(me?.id, hideNsfw) {
        reloadConversations()
    }

    // Active conversation feed state (for Day / Tag / Search work groups)
    var feedMemes by remember { mutableStateOf<List<MemeItem>>(emptyList()) }
    var feedLoading by remember { mutableStateOf(false) }
    var feedLoadingMore by remember { mutableStateOf(false) }
    var feedHasMore by remember { mutableStateOf(false) }
    var feedOffset by remember { mutableStateOf(0) }
    var quotedMeme by remember { mutableStateOf<MemeItem?>(null) }
    var replyDraft by remember { mutableStateOf("") }
    var sendingComment by remember { mutableStateOf(false) }
    var emojiDropdownMeme by remember { mutableStateOf<MemeItem?>(null) }

    fun loadConversationFeed(conv: FeishuConvTarget, reset: Boolean) {
        val (dayParam, tagParam, queryParam, sortParam) = when (conv) {
            is FeishuConvTarget.DayWorkGroup -> listOf(conv.entry.day, null, null, "old")
            is FeishuConvTarget.TagTopic -> listOf(null, conv.stat.tag, null, "new")
            is FeishuConvTarget.SearchResult -> listOf(null, null, conv.query, "new")
            else -> return
        }
        scope.launch {
            if (reset) {
                feedLoading = true
                feedOffset = 0
                feedMemes = emptyList()
            } else {
                feedLoadingMore = true
            }
            val nextOffset = if (reset) 0 else feedOffset
            val page = api.fetchMemesPage(
                limit = 20,
                offset = nextOffset,
                day = dayParam,
                tag = tagParam,
                query = queryParam,
                sort = sortParam ?: "old",
                nsfwHide = hideNsfw,
            )
            seenTracker?.ingestFetchedMemes(page.items)
            val seenIds = if (reset) HashSet<Long>() else feedMemes.asSequence().map { it.id }.toHashSet()
            val fresh = page.items.filter { seenIds.add(it.id) }
            feedMemes = if (reset) page.items else feedMemes + fresh
            feedOffset = page.offset + page.items.size
            feedHasMore = page.more
            feedLoading = false
            feedLoadingMore = false
        }
    }

    LaunchedEffect(activeConv?.key, hideNsfw) {
        val conv = activeConv ?: return@LaunchedEffect
        quotedMeme = null
        replyDraft = ""
        when (conv) {
            is FeishuConvTarget.RealGroup -> chatController.setActiveGroup(conv.group.id)
            is FeishuConvTarget.DirectMsg -> Unit
            else -> loadConversationFeed(conv, reset = true)
        }
    }

    val currentConv = activeConv
    if (currentConv != null) {
        BackHandler(
            enabled = !showCompose && selectedDetailId == null && fullscreenImgUrl == null,
        ) {
            activeConv = null
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.bg)
                .navigationBarsPadding(),
        ) {
            // Feishu conversation detail header
            Surface(
                color = palette.surface,
                modifier = Modifier.statusBarsPadding(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { activeConv = null }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                            contentDescription = "返回",
                            tint = palette.text,
                        )
                    }
                    when (currentConv) {
                        is FeishuConvTarget.DirectMsg -> ForHappyAvatar(
                            username = currentConv.title,
                            avatarUrl = currentConv.thread.peerAvatar,
                            imageLoader = api.imageLoader,
                            size = 32.dp,
                        )
                        else -> FeishuGroupAvatar(title = currentConv.title, size = 32.dp)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentConv.title,
                            color = palette.text,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        val sub = when (currentConv) {
                            is FeishuConvTarget.DayWorkGroup -> "${currentConv.entry.totalCount} 条消息"
                            is FeishuConvTarget.TagTopic -> "${currentConv.stat.totalCount} 条消息"
                            is FeishuConvTarget.RealGroup -> if (currentConv.group.memberCount > 0) "${currentConv.group.memberCount} 位成员" else "内部交流群"
                            is FeishuConvTarget.DirectMsg -> "同事单聊"
                            is FeishuConvTarget.SearchResult -> "${feedMemes.size} 条匹配消息"
                        }
                        Text(sub, color = palette.muted, fontSize = 11.sp, maxLines = 1)
                    }
                    Box {
                        IconButton(onClick = { showConvMenu = true }) {
                            Icon(Icons.Rounded.MoreHoriz, contentDescription = "更多设置", tint = palette.text)
                        }
                        FeishuSettingsMenu(
                            expanded = showConvMenu,
                            onDismiss = { showConvMenu = false },
                            blurAllEnabled = privacyConfig.blurAllEnabled,
                            hideNsfw = hideNsfw,
                            onToggleBlurAll = onToggleBlurAll,
                            onToggleNsfw = { hideNsfw = !hideNsfw },
                            onTriggerBossKey = onTriggerBossKey,
                            onOpenPrivacySettings = onOpenPrivacySettings,
                            onExitDisguise = onExitDisguise,
                        )
                    }
                }
            }
            HorizontalDivider(color = palette.line)

            when (currentConv) {
                is FeishuConvTarget.RealGroup -> {
                    Box(modifier = Modifier.weight(1f)) {
                        ChatScreen(
                            api = api,
                            controller = chatController,
                            me = me,
                            initialJump = ChatNotificationJump(groupId = currentConv.group.id),
                            onNavigateToMeme = { selectedDetailId = it },
                            onNavigateToTag = { tag ->
                                activeConv = FeishuConvTarget.TagTopic(TagStatItem(tag = tag))
                            },
                            onOpenUrl = onOpenUrl,
                            onRequireLogin = onRequireLogin,
                            hideChannelHeader = true,
                            forcedChannelMode = "group",
                        )
                    }
                }

                is FeishuConvTarget.DirectMsg -> {
                    Box(modifier = Modifier.weight(1f)) {
                        ChatScreen(
                            api = api,
                            controller = chatController,
                            me = me,
                            onNavigateToMeme = { selectedDetailId = it },
                            onNavigateToTag = { tag ->
                                activeConv = FeishuConvTarget.TagTopic(TagStatItem(tag = tag))
                            },
                            onOpenUrl = onOpenUrl,
                            onRequireLogin = onRequireLogin,
                            hideChannelHeader = true,
                            forcedChannelMode = "dm",
                            forcedDmThreadId = currentConv.thread.id,
                        )
                    }
                }

                else -> {
                    Box(modifier = Modifier.weight(1f)) {
                        if (feedLoading && feedMemes.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = palette.primary)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                items(feedMemes.size, key = { idx -> feedMemes[idx].id }) { idx ->
                                    val m = feedMemes[idx]
                                    val prev = if (idx > 0) feedMemes[idx - 1] else null
                                    val curDay = m.day ?: SeenTracker.dayOfUtc8(m.createdAt ?: 0L)
                                    val prevDay = prev?.let { it.day ?: SeenTracker.dayOfUtc8(it.createdAt ?: 0L) }
                                    if (curDay.isNotBlank() && curDay != prevDay) {
                                        FeishuDayDivider(FeishuDisguiseHelpers.daySeparatorLabel(curDay))
                                    }
                                    val who = FeishuDisguiseHelpers.imWho(m)
                                    val prevWho = prev?.let { FeishuDisguiseHelpers.imWho(it) }
                                    val sameWindow = prev != null &&
                                        curDay == prevDay &&
                                        who == prevWho &&
                                        kotlin.math.abs((prev.createdAt ?: 0L) - (m.createdAt ?: 0L)) < 300L
                                    val isFakeColleague = !m.sourceUrl.isNullOrBlank()

                                    FeishuMemeMessageRow(
                                        meme = m,
                                        who = who,
                                        isFakeColleague = isFakeColleague,
                                        isContinuation = sameWindow,
                                        api = api,
                                        me = me,
                                        onRequireLogin = onRequireLogin,
                                        onOpenDetail = { selectedDetailId = m.id },
                                        onOpenImage = { fullscreenImgUrl = it },
                                        onQuote = { quotedMeme = m },
                                        onMemeUpdated = { updated ->
                                            feedMemes = feedMemes.map { if (it.id == updated.id) updated else it }
                                        },
                                        onToggleReactionSheet = { emojiDropdownMeme = m },
                                    )
                                }

                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        when {
                                            feedLoadingMore -> CircularProgressIndicator(
                                                modifier = Modifier.size(22.dp),
                                                strokeWidth = 2.dp,
                                                color = palette.primary,
                                            )
                                            feedHasMore -> OutlinedButton(
                                                onClick = { loadConversationFeed(currentConv, reset = false) },
                                            ) {
                                                Text("加载更多消息", fontSize = 12.sp)
                                            }
                                            else -> Text("没有更多消息了", color = palette.muted, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Bottom Feishu IM composer bar
                    Surface(
                        color = palette.surface,
                        modifier = Modifier.imePadding(),
                    ) {
                        Column {
                            HorizontalDivider(color = palette.line)
                            quotedMeme?.let { qm ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(palette.surface2)
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = "回复 ${FeishuDisguiseHelpers.imWho(qm)}：${qm.safeLabel}",
                                        color = palette.muted,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "取消回复",
                                        tint = palette.muted,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable { quotedMeme = null },
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                IconButton(
                                    onClick = {
                                        if (me == null) onRequireLogin() else showCompose = true
                                    },
                                    modifier = Modifier.size(34.dp),
                                ) {
                                    Icon(Icons.Rounded.Image, contentDescription = "发送图片", tint = palette.muted)
                                }
                                Surface(
                                    color = palette.surface2,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 38.dp),
                                ) {
                                    Box(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                        contentAlignment = Alignment.CenterStart,
                                    ) {
                                        if (replyDraft.isEmpty()) {
                                            Text(
                                                text = if (quotedMeme != null) {
                                                    "回复 ${FeishuDisguiseHelpers.imWho(quotedMeme!!)}…"
                                                } else {
                                                    "先点消息右上角「回复」再输入评论，或点左侧图片发帖"
                                                },
                                                color = palette.muted,
                                                fontSize = 13.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                        BasicTextField(
                                            value = replyDraft,
                                            onValueChange = { replyDraft = it },
                                            textStyle = TextStyle(color = palette.text, fontSize = 13.sp),
                                            cursorBrush = SolidColor(palette.primary),
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                    }
                                }
                                IconButton(
                                    enabled = replyDraft.isNotBlank() && quotedMeme != null && !sendingComment,
                                    onClick = {
                                        val target = quotedMeme ?: return@IconButton
                                        val text = replyDraft.trim()
                                        if (text.isBlank()) return@IconButton
                                        if (me == null) {
                                            onRequireLogin()
                                            return@IconButton
                                        }
                                        sendingComment = true
                                        scope.launch {
                                            val ok = api.postMemeComment(target.id, text)
                                            if (ok) {
                                                replyDraft = ""
                                                quotedMeme = null
                                                feedMemes = feedMemes.map { cur ->
                                                    if (cur.id == target.id) {
                                                        cur.copy(
                                                            comments = cur.comments + 1,
                                                            commentCount = cur.commentCount + 1,
                                                        )
                                                    } else {
                                                        cur
                                                    }
                                                }
                                            }
                                            sendingComment = false
                                        }
                                    },
                                    modifier = Modifier.size(34.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.Send,
                                        contentDescription = "发送",
                                        tint = if (replyDraft.isNotBlank() && quotedMeme != null) palette.primary else palette.muted,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
        // Conversation List Root
        DoubleBackBossHandler(
            enabled = privacyConfig.bossDoubleBack &&
                !showCompose &&
                selectedDetailId == null &&
                fullscreenImgUrl == null,
            onTriggerBoss = onTriggerBossKey,
        )

        val allConvs = remember(chatState.groups, dmThreads, dayEntries, newlySeenByDay) {
            buildList {
                chatState.groups.forEach { add(FeishuConvTarget.RealGroup(it)) }
                dmThreads.forEach { add(FeishuConvTarget.DirectMsg(it)) }
                dayEntries.forEach { d ->
                    val delta = newlySeenByDay[d.day] ?: 0
                    add(FeishuConvTarget.DayWorkGroup(d, (d.unseen - delta).coerceAtLeast(0)))
                }
            }
        }

        val displayedConvs = remember(activeTab, searchQuery, allConvs, tagStats) {
            val kw = searchQuery.trim()
            if (activeTab == "tag") {
                val base = tagStats.map { FeishuConvTarget.TagTopic(it) }
                if (kw.isBlank()) base else base.filter { it.title.contains(kw, ignoreCase = true) }
            } else {
                var list = allConvs
                if (activeTab == "unread") {
                    list = list.filter { it.unreadCount > 0 }
                }
                if (kw.isNotBlank()) {
                    list = list.filter { item ->
                        item.title.contains(kw, ignoreCase = true) ||
                            (item is FeishuConvTarget.DayWorkGroup && item.entry.latest?.text?.contains(kw, ignoreCase = true) == true) ||
                            (item is FeishuConvTarget.RealGroup && item.group.latest?.text?.contains(kw, ignoreCase = true) == true)
                    }
                }
                list
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.bg)
                .navigationBarsPadding(),
        ) {
            // Feishu list top search + create + settings menu
            Surface(
                color = palette.surface,
                modifier = Modifier.statusBarsPadding(),
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(palette.primary),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("飞", color = palette.onPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Surface(
                            color = palette.surface2,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Rounded.Search, contentDescription = null, tint = palette.muted, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                                    if (searchQuery.isEmpty()) {
                                        Text("搜索会话或回车全站搜消息", color = palette.muted, fontSize = 13.sp, maxLines = 1)
                                    }
                                    BasicTextField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        singleLine = true,
                                        textStyle = TextStyle(color = palette.text, fontSize = 13.sp),
                                        cursorBrush = SolidColor(palette.primary),
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                        keyboardActions = KeyboardActions(
                                            onSearch = {
                                                val q = searchQuery.trim()
                                                if (q.isNotBlank()) activeConv = FeishuConvTarget.SearchResult(q)
                                            },
                                        ),
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                                if (searchQuery.isNotEmpty()) {
                                    Icon(
                                        Icons.Rounded.Close,
                                        contentDescription = "清除",
                                        tint = palette.muted,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable { searchQuery = "" },
                                    )
                                }
                            }
                        }
                        IconButton(
                            onClick = { if (me == null) onRequireLogin() else showCompose = true },
                            modifier = Modifier.size(34.dp),
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = "发起", tint = palette.text)
                        }
                        Box {
                            IconButton(
                                onClick = { showConvMenu = true },
                                modifier = Modifier.size(34.dp),
                            ) {
                                Icon(Icons.Rounded.MoreHoriz, contentDescription = "伪装菜单", tint = palette.text)
                            }
                            FeishuSettingsMenu(
                                expanded = showConvMenu,
                                onDismiss = { showConvMenu = false },
                                blurAllEnabled = privacyConfig.blurAllEnabled,
                                hideNsfw = hideNsfw,
                                onToggleBlurAll = onToggleBlurAll,
                                onToggleNsfw = { hideNsfw = !hideNsfw },
                                onTriggerBossKey = onTriggerBossKey,
                                onOpenPrivacySettings = onOpenPrivacySettings,
                                onExitDisguise = onExitDisguise,
                            )
                        }
                    }

                    // 消息 / 未读 / 话题 tabs
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                    ) {
                        FeishuTabLabel("消息", selected = activeTab == "all") { activeTab = "all" }
                        if (me != null) {
                            FeishuTabLabel("未读", selected = activeTab == "unread") { activeTab = "unread" }
                        }
                        FeishuTabLabel("话题", selected = activeTab == "tag") { activeTab = "tag" }
                    }
                    HorizontalDivider(color = palette.line)
                }
            }

            if (loadingConvs && displayedConvs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = palette.primary)
                }
            } else if (displayedConvs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = when {
                                searchQuery.isNotBlank() -> "点击下方搜索全站相关消息"
                                activeTab == "unread" -> "没有未读会话"
                                else -> "暂无会话"
                            },
                            color = palette.muted,
                            fontSize = 13.sp,
                        )
                        if (searchQuery.isNotBlank()) {
                            OutlinedButton(onClick = { activeConv = FeishuConvTarget.SearchResult(searchQuery.trim()) }) {
                                Text("搜索「${searchQuery.trim()}」", fontSize = 12.sp)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 4.dp),
                ) {
                    items(displayedConvs, key = { it.key }) { conv ->
                        FeishuConvRow(
                            conv = conv,
                            me = me,
                            api = api,
                            onClick = { activeConv = conv },
                        )
                        HorizontalDivider(color = palette.line.copy(alpha = 0.5f), modifier = Modifier.padding(start = 68.dp))
                    }
                }
            }
        }
    }

    if (showCompose) {
        ComposeMemeModal(
            api = api,
            onDismiss = { showCompose = false },
            onPublished = {
                reloadConversations()
                activeConv?.let { loadConversationFeed(it, reset = true) }
            },
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
            onTagClick = { tag ->
                selectedDetailId = null
                activeConv = FeishuConvTarget.TagTopic(TagStatItem(tag = tag))
            },
            onStateUpdated = { updated ->
                feedMemes = feedMemes.map { if (it.id == updated.id) updated else it }
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

    emojiDropdownMeme?.let { target ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { emojiDropdownMeme = null },
            title = { Text("快捷回应表情", fontSize = 15.sp, fontWeight = FontWeight.SemiBold) },
            text = {
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PICKED_REACTIONS.forEach { key ->
                        Surface(
                            color = palette.surface2,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.clickable {
                                emojiDropdownMeme = null
                                scope.launch {
                                    api.reactToMeme(target.id, key)?.let { updated ->
                                        feedMemes = feedMemes.map { if (it.id == updated.id) updated else it }
                                    }
                                }
                            },
                        ) {
                            Box(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                                ReactionEmojiView(
                                    emojiKey = key,
                                    imageLoader = api.imageLoader,
                                    size = 24.dp,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { emojiDropdownMeme = null }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun FeishuTabLabel(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val palette = LocalForHappyPalette.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(vertical = 4.dp),
    ) {
        Text(
            text = text,
            color = if (selected) palette.primary else palette.muted,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .width(20.dp)
                .height(2.5.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(if (selected) palette.primary else Color.Transparent),
        )
    }
}

@Composable
private fun FeishuSettingsMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    blurAllEnabled: Boolean,
    hideNsfw: Boolean,
    onToggleBlurAll: (Boolean) -> Unit,
    onToggleNsfw: () -> Unit,
    onTriggerBossKey: () -> Unit,
    onOpenPrivacySettings: () -> Unit,
    onExitDisguise: () -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text(if (blurAllEnabled) "全图模糊：已开启（点击关闭）" else "全图模糊：已关闭（点击开启）") },
            onClick = {
                onDismiss()
                onToggleBlurAll(!blurAllEnabled)
            },
        )
        DropdownMenuItem(
            text = { Text(if (hideNsfw) "内容过滤：隐藏敏感（点击显示）" else "内容过滤：模糊显示（点击隐藏）") },
            onClick = {
                onDismiss()
                onToggleNsfw()
            },
        )
        DropdownMenuItem(
            text = { Text("切到云文档（老板键）") },
            onClick = {
                onDismiss()
                onTriggerBossKey()
            },
        )
        DropdownMenuItem(
            text = { Text("伪装与隐私设置") },
            onClick = {
                onDismiss()
                onOpenPrivacySettings()
            },
        )
        DropdownMenuItem(
            text = { Text("退出伪装") },
            onClick = {
                onDismiss()
                onExitDisguise()
            },
        )
    }
}

@Composable
private fun FeishuGroupAvatar(title: String, size: Dp) {
    val bg = remember(title) { FeishuDisguiseHelpers.imColor(title) }
    val chars = remember(title) { FeishuDisguiseHelpers.avatarChars(title) }
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp))
            .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = chars,
            color = Color.White,
            fontSize = (size.value * 0.34f).sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun FeishuConvRow(
    conv: FeishuConvTarget,
    me: MeUser?,
    api: ForHappyApiClient,
    onClick: () -> Unit,
) {
    val palette = LocalForHappyPalette.current
    val (subtitle, timeStr) = remember(conv, me?.id) {
        when (conv) {
            is FeishuConvTarget.RealGroup -> {
                val lat = conv.group.latest
                val sub = if (lat != null) {
                    val sender = lat.by?.takeIf { it.isNotBlank() }?.let { "$it：" }.orEmpty()
                    val body = ChatRichTextParser.previewSummary(lat.text.orEmpty()).ifBlank { "[图片]" }
                    "$sender$body"
                } else {
                    conv.group.desc?.ifBlank { "群聊" } ?: "群聊"
                }
                sub to FeishuDisguiseHelpers.formatConvTime(lat?.at ?: 0L)
            }
            is FeishuConvTarget.DirectMsg -> {
                val t = conv.thread
                val prefix = if (t.lastSender != null && t.lastSender == me?.id) "我：" else ""
                val body = ChatRichTextParser.previewSummary(t.lastBody.orEmpty()).ifBlank { "开始聊天" }
                "$prefix$body" to FeishuDisguiseHelpers.formatConvTime(if (t.lastAt > 0L) t.lastAt else t.updatedAt)
            }
            is FeishuConvTarget.DayWorkGroup -> {
                val lat = conv.entry.latest
                val sub = if (lat != null) {
                    val sender = if (lat.src) FeishuDisguiseHelpers.imFake(lat.id) else lat.by.orEmpty()
                    val head = if (sender.isNotBlank()) "$sender：" else ""
                    val body = ChatRichTextParser.previewSummary(lat.text.orEmpty()).ifBlank { "[图片]" }
                    "$head$body"
                } else {
                    "${conv.entry.totalCount} 条消息"
                }
                sub to FeishuDisguiseHelpers.formatConvTime(lat?.at ?: 0L)
            }
            is FeishuConvTarget.TagTopic -> "${conv.stat.totalCount} 条相关记录" to ""
            is FeishuConvTarget.SearchResult -> conv.title to ""
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (conv) {
            is FeishuConvTarget.DirectMsg -> ForHappyAvatar(
                username = conv.title,
                avatarUrl = conv.thread.peerAvatar,
                imageLoader = api.imageLoader,
                size = 42.dp,
            )
            else -> FeishuGroupAvatar(title = conv.title, size = 42.dp)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = conv.title,
                    color = palette.text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (timeStr.isNotBlank()) {
                    Spacer(Modifier.width(8.dp))
                    Text(timeStr, color = palette.muted, fontSize = 11.sp)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = subtitle,
                    color = palette.muted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (conv.unreadCount > 0) {
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = palette.danger,
                        shape = CircleShape,
                    ) {
                        Text(
                            text = if (conv.unreadCount > 99) "99+" else "${conv.unreadCount}",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeishuDayDivider(label: String) {
    val palette = LocalForHappyPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        HorizontalDivider(color = palette.line, modifier = Modifier.weight(1f))
        Text(label, color = palette.muted, fontSize = 11.sp)
        HorizontalDivider(color = palette.line, modifier = Modifier.weight(1f))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FeishuMemeMessageRow(
    meme: MemeItem,
    who: String,
    isFakeColleague: Boolean,
    isContinuation: Boolean,
    api: ForHappyApiClient,
    me: MeUser?,
    onRequireLogin: () -> Unit,
    onOpenDetail: () -> Unit,
    onOpenImage: (String) -> Unit,
    onQuote: () -> Unit,
    onMemeUpdated: (MemeItem) -> Unit,
    onToggleReactionSheet: () -> Unit,
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

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        if (isContinuation) {
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .padding(top = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = FeishuDisguiseHelpers.formatClock(meme.createdAt),
                    color = palette.muted.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                )
            }
        } else {
            ForHappyAvatar(
                username = who,
                avatarUrl = if (isFakeColleague) null else meme.authorAvatar,
                imageLoader = api.imageLoader,
                size = 36.dp,
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            if (!isContinuation) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(who, color = palette.text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = FeishuDisguiseHelpers.formatClock(meme.createdAt),
                            color = palette.muted,
                            fontSize = 11.sp,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.EmojiEmotions,
                            contentDescription = "表情回应",
                            tint = palette.muted,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable {
                                    if (me == null) onRequireLogin() else onToggleReactionSheet()
                                },
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Reply,
                            contentDescription = "回复",
                            tint = palette.muted,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { onQuote() },
                        )
                        Icon(
                            imageVector = if (meme.liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = "点赞",
                            tint = if (meme.liked) palette.danger else palette.muted,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable {
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
                        )
                    }
                }
            }

            val messageText = remember(meme.id) {
                listOfNotNull(meme.title, meme.text, meme.content, meme.caption, meme.body)
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .joinToString("\n")
            }
            if (messageText.isNotBlank()) {
                Surface(
                    color = palette.surface,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .widthIn(max = 310.dp)
                        .border(1.dp, palette.line.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                        .clickable { onOpenDetail() },
                ) {
                    Text(
                        text = messageText,
                        color = palette.text,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
                    )
                }
            }

            if (rawUrl.isNotBlank()) {
                val w = meme.effectiveWidth ?: 0
                val h = meme.effectiveHeight ?: 0
                val ratio = if (w > 0 && h > 0) (w.toFloat() / h.toFloat()).coerceIn(0.60f, 1.85f) else 4f / 3f
                Box(
                    modifier = Modifier
                        .width(220.dp)
                        .aspectRatio(ratio)
                        .clip(RoundedCornerShape(10.dp))
                        .background(palette.surface2)
                        .border(1.dp, palette.line, RoundedCornerShape(10.dp))
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
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                            ) {
                                Icon(Icons.Rounded.VisibilityOff, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                Text("已模糊 · 轻触查看", color = Color.White, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }

            // Reaction chips row
            val activeReactions = remember(meme.reactions) { meme.reactions.filter { it.count > 0 } }
            if (meme.totalLikes > 0 || activeReactions.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (meme.totalLikes > 0) {
                        Surface(
                            color = if (meme.liked) palette.primary.copy(alpha = 0.14f) else palette.surface2,
                            shape = RoundedCornerShape(999.dp),
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
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                            ) {
                                Icon(
                                    imageVector = if (meme.liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                    contentDescription = null,
                                    tint = if (meme.liked) palette.primary else palette.muted,
                                    modifier = Modifier.size(12.dp),
                                )
                                Text("${meme.totalLikes}", color = palette.text, fontSize = 11.sp)
                            }
                        }
                    }
                    activeReactions.take(8).forEach { rx ->
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

            // Quick replies + N条回复 thread link
            val replies = remember(meme.replyList, meme.myReply, me?.username) { meme.effectiveQuickReplies(me) }
            val threadCount = meme.replyCount + meme.totalComments
            if (replies.isNotEmpty() || threadCount > 0) {
                Row(
                    modifier = Modifier.clickable { onOpenDetail() },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (threadCount > 0) {
                        Text(
                            text = "${threadCount} 条回复 ›",
                            color = palette.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    replies.take(2).forEach { qr ->
                        Surface(
                            color = palette.surface2,
                            shape = RoundedCornerShape(999.dp),
                        ) {
                            Box(modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)) {
                                BoostPillText(text = qr.text, imageLoader = api.imageLoader, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
