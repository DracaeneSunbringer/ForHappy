package com.jinhuan.forhappy.ui.wall

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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CollectionsBookmark
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.jinhuan.forhappy.core.model.AnchorTag
import com.jinhuan.forhappy.core.model.AuthorOption
import com.jinhuan.forhappy.core.model.BOOST_MAX_LEN
import com.jinhuan.forhappy.core.model.ChatGroup
import com.jinhuan.forhappy.core.model.DayEntry
import com.jinhuan.forhappy.core.model.FolderItem
import com.jinhuan.forhappy.core.model.ForHappyJson
import com.jinhuan.forhappy.core.model.MeUser
import com.jinhuan.forhappy.core.model.MemeItem
import com.jinhuan.forhappy.core.model.PICKED_REACTIONS
import com.jinhuan.forhappy.core.model.SeriesItem
import com.jinhuan.forhappy.core.model.TagStatItem
import com.jinhuan.forhappy.core.model.WallFilterState
import com.jinhuan.forhappy.core.model.countReplyUnits
import com.jinhuan.forhappy.core.network.api.ForHappyApiClient
import com.jinhuan.forhappy.core.network.media.MediaUrlRewriter
import com.jinhuan.forhappy.ui.components.ForHappyAvatar
import com.jinhuan.forhappy.ui.components.ReactionEmojiView
import com.jinhuan.forhappy.ui.theme.LocalForHappyPalette
import com.jinhuan.forhappy.ui.theme.TABULAR_FONT_FEATURE
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encodeToString
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

@Composable
internal fun WallSheetPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val palette = LocalForHappyPalette.current
    Surface(
        color = if (selected) palette.primaryContainer else palette.surface2,
        shape = RoundedCornerShape(999.dp),
        modifier = Modifier
            .border(
                width = 1.dp,
                color = if (selected) palette.primary else palette.line,
                shape = RoundedCornerShape(999.dp),
            )
            .clickable { onClick() },
    ) {
        Text(
            text = label,
            color = if (selected) palette.primary else palette.text,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

/**
 * Gallery Browse Settings Bottom Sheet (`02a §2`):
 * Consolidates sort, unseen, content type, 4-tier NSFW strategy, mask tap mode, view mode,
 * by-day toggle, personal anchors strip, and multi-select author/tag jump with live count preview.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WallBrowseSettingsSheet(
    current: WallFilterState,
    blurAllEnabled: Boolean,
    anchors: List<AnchorTag>,
    api: ForHappyApiClient,
    onToggleBlurAll: (Boolean) -> Unit,
    onJumpAnchor: (AnchorTag) -> Unit,
    onAnchorsChanged: (List<AnchorTag>) -> Unit,
    onOpenAuthorTagFilter: () -> Unit,
    onApply: (WallFilterState) -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalForHappyPalette.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var draft by remember(current) { mutableStateOf(current) }
    var previewCount by remember { mutableStateOf<Int?>(null) }
    var showManageAnchors by remember { mutableStateOf(false) }

    LaunchedEffect(
        draft.sort,
        draft.unseenOnly,
        draft.textFilter,
        draft.nsfwMode,
        draft.tags,
        draft.author,
        draft.searchQuery,
    ) {
        delay(220L)
        val daysResp = api.fetchDays(
            limitDays = 45,
            sort = draft.sort,
            unseenOnly = draft.unseenOnly,
            textFilter = draft.textFilter,
            nsfwModeParam = draft.nsfwQueryValue(),
            tag = draft.tags.takeIf { it.isNotEmpty() }?.joinToString(","),
            author = draft.author.takeIf { it.isNotBlank() },
            search = draft.searchQuery.takeIf { it.isNotBlank() },
        )
        previewCount = daysResp.days.sumOf { it.totalCount }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.bg,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
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
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("浏览设置", color = palette.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                TextButton(
                    onClick = {
                        draft = WallFilterState(searchQuery = current.searchQuery)
                    },
                ) {
                    Text("重置", color = palette.primary, fontSize = 13.sp)
                }
            }

            HorizontalDivider(
                color = if (palette.isModernUi) palette.glassBorder else palette.line,
                thickness = if (palette.isModernUi) 0.5.dp else 1.dp,
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("排序", color = palette.muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("new" to "最新", "hot" to "热门", "old" to "最早").forEach { (k, label) ->
                                WallSheetPill(label = label, selected = draft.sort == k) {
                                    draft = draft.copy(sort = k)
                                }
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("只看未看", color = palette.text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Switch(
                            checked = draft.unseenOnly,
                            onCheckedChange = { draft = draft.copy(unseenOnly = it) },
                        )
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("体裁", color = palette.muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("" to "全部", "only" to "只看文字", "0" to "不看文字").forEach { (k, label) ->
                                WallSheetPill(label = label, selected = draft.textFilter == k) {
                                    draft = draft.copy(textFilter = k)
                                }
                            }
                        }
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("敏感内容", color = palette.muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            listOf(
                                "hide" to "隐藏",
                                "blur" to "模糊",
                                "show" to "显示",
                                "only" to "仅看",
                            ).forEach { (k, label) ->
                                WallSheetPill(label = label, selected = draft.nsfwMode == k) {
                                    draft = draft.copy(nsfwMode = k)
                                }
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                        Text("点模糊图时", color = palette.muted, fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("peek" to "原地揭开", "view" to "打开大图").forEach { (k, label) ->
                                WallSheetPill(label = label, selected = draft.maskTap == k) {
                                    draft = draft.copy(maskTap = k)
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("全图防窥模糊", color = palette.text, fontSize = 13.sp)
                            Switch(checked = blurAllEnabled, onCheckedChange = onToggleBlurAll)
                        }
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("视图", color = palette.muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                "dual" to "双列瀑布",
                                "single" to "单列大卡",
                                "pager" to "沉浸竖翻",
                            ).forEach { (k, label) ->
                                WallSheetPill(label = label, selected = draft.viewMode == k) {
                                    draft = draft.copy(viewMode = k)
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = if (draft.sort == "hot") "按日分组（热门排序下自动关闭）" else "按日分组",
                                color = palette.text,
                                fontSize = 14.sp,
                            )
                            Switch(
                                checked = draft.byDay && draft.sort != "hot",
                                enabled = draft.sort != "hot",
                                onCheckedChange = { draft = draft.copy(byDay = it) },
                            )
                        }
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("我的锚点", color = palette.muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            if (anchors.isNotEmpty()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { showManageAnchors = true },
                                ) {
                                    Text("管理", color = palette.primary, fontSize = 12.sp)
                                    Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = palette.primary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                        if (anchors.isEmpty()) {
                            Text(
                                text = "长按图廊卡片或进详情「插锚点」后，可在这里一键跳回",
                                color = palette.muted,
                                fontSize = 12.sp,
                            )
                        } else {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(end = 8.dp),
                            ) {
                                items(anchors, key = { it.id }) { anchor ->
                                    Surface(
                                        color = palette.surface2,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .width(124.dp)
                                            .then(
                                                if (palette.isModernUi && palette.cardBorder == Color.Transparent) {
                                                    Modifier
                                                } else {
                                                    Modifier.border(
                                                        1.dp,
                                                        if (palette.isModernUi) palette.cardBorder else palette.line,
                                                        RoundedCornerShape(10.dp),
                                                    )
                                                },
                                            )
                                            .clickable {
                                                onJumpAnchor(anchor)
                                                onDismiss()
                                            },
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(3.dp),
                                        ) {
                                            Text(
                                                text = "No.${anchor.id}",
                                                color = palette.primary,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                style = TextStyle(fontFeatureSettings = TABULAR_FONT_FEATURE),
                                            )
                                            Text(
                                                text = anchor.displayLabel,
                                                color = palette.text,
                                                fontSize = 12.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            if (anchor.day.isNotBlank()) {
                                                Text(
                                                    text = anchor.day,
                                                    color = palette.muted,
                                                    fontSize = 10.sp,
                                                    style = TextStyle(fontFeatureSettings = TABULAR_FONT_FEATURE),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    val chosenAuthors = remember(draft.author) {
                        draft.author.split(',', ' ', '，').filter { it.isNotBlank() }.size
                    }
                    val chosenTotal = draft.tags.size + chosenAuthors + if (draft.series.isNotBlank()) 1 else 0
                    Surface(
                        color = palette.surface2,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (palette.isModernUi && palette.cardBorder == Color.Transparent) {
                                    Modifier
                                } else {
                                    Modifier.border(
                                        1.dp,
                                        if (palette.isModernUi) palette.cardBorder else palette.line,
                                        RoundedCornerShape(12.dp),
                                    )
                                },
                            )
                            .clickable {
                                onDismiss()
                                onOpenAuthorTagFilter()
                            },
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("发图人 / 标签 / 图集筛选", color = palette.text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (chosenTotal > 0) "已选 $chosenTotal 项" else "去选择",
                                    color = palette.primary,
                                    fontSize = 12.sp,
                                )
                                Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = palette.primary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            Surface(color = palette.surface) {
                Button(
                    onClick = { onApply(draft) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.primary,
                        contentColor = palette.onPrimary,
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .height(46.dp),
                ) {
                    val countText = previewCount?.let { "（显示 $it 帖）" }.orEmpty()
                    Text("应用$countText", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    if (showManageAnchors) {
        AlertDialog(
            onDismissRequest = { showManageAnchors = false },
            containerColor = palette.surface,
            title = { Text("管理锚点 (${anchors.size}/30)", color = palette.text, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(
                    modifier = Modifier.height(260.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(anchors, key = { "mgr_${it.id}" }) { anchor ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(palette.surface2)
                                .clickable {
                                    showManageAnchors = false
                                    onJumpAnchor(anchor)
                                    onDismiss()
                                }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("No.${anchor.id} · ${anchor.displayLabel}", color = palette.text, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (anchor.day.isNotBlank()) {
                                    Text(anchor.day, color = palette.muted, fontSize = 11.sp)
                                }
                            }
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        val updated = api.deleteAnchor(anchor.id)
                                        onAnchorsChanged(updated)
                                        if (updated.isEmpty()) showManageAnchors = false
                                    }
                                },
                            ) {
                                Icon(Icons.Rounded.Close, contentDescription = "删除锚点", tint = palette.muted, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showManageAnchors = false }) { Text("完成") }
            },
        )
    }
}

/**
 * Calendar day jumper bottom sheet (`02a §4`).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallDayJumpSheet(
    days: List<DayEntry>,
    currentFromDay: String?,
    onSelectDay: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalForHappyPalette.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val dayMap = remember(days) { days.associateBy { it.day } }

    val availableMonths = remember(days) {
        val parsed = days.mapNotNull { entry ->
            runCatching { LocalDate.parse(entry.day) }.getOrNull()?.let { YearMonth.from(it) }
        }.distinct().sorted()
        parsed.ifEmpty { listOf(YearMonth.now()) }
    }

    var monthIdx by remember(availableMonths, currentFromDay) {
        val targetMonth = currentFromDay?.let {
            runCatching { YearMonth.from(LocalDate.parse(it)) }.getOrNull()
        }
        mutableIntStateOf(
            targetMonth?.let { availableMonths.indexOf(it).takeIf { idx -> idx >= 0 } }
                ?: availableMonths.lastIndex,
        )
    }
    var selectedDay by remember(currentFromDay, days) {
        mutableStateOf(currentFromDay ?: days.firstOrNull()?.day)
    }

    val activeMonth = availableMonths[monthIdx.coerceIn(0, availableMonths.lastIndex)]
    val firstDayOfMonth = activeMonth.atDay(1)
    val leadingBlanks = (firstDayOfMonth.dayOfWeek.value - DayOfWeek.MONDAY.value).coerceAtLeast(0)
    val totalDaysInMonth = activeMonth.lengthOfMonth()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.bg,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("跳到哪天", color = palette.text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "关闭", tint = palette.muted)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(enabled = monthIdx > 0, onClick = { monthIdx-- }) {
                    Icon(
                        Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                        contentDescription = "上个月",
                        tint = if (monthIdx > 0) palette.text else palette.textDisabled,
                    )
                }
                Text(
                    text = "${activeMonth.year} 年 ${activeMonth.monthValue} 月",
                    color = palette.text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    style = TextStyle(fontFeatureSettings = TABULAR_FONT_FEATURE),
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                IconButton(enabled = monthIdx < availableMonths.lastIndex, onClick = { monthIdx++ }) {
                    Icon(
                        Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = "下个月",
                        tint = if (monthIdx < availableMonths.lastIndex) palette.text else palette.textDisabled,
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("一", "二", "三", "四", "五", "六", "日").forEach { wd ->
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(wd, color = palette.muted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            val cells = remember(activeMonth) {
                List(leadingBlanks) { null } + (1..totalDaysInMonth).map { dayNum ->
                    activeMonth.atDay(dayNum)
                }
            }
            val rows = cells.chunked(7)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                rows.forEach { week ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        for (i in 0 until 7) {
                            val date = week.getOrNull(i)
                            if (date == null) {
                                Spacer(Modifier.weight(1f))
                            } else {
                                val iso = date.toString()
                                val entry = dayMap[iso]
                                val isSel = selectedDay == iso
                                val enabled = entry != null
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            when {
                                                isSel -> palette.primaryContainer
                                                enabled -> palette.surface2
                                                else -> palette.bg
                                            },
                                        )
                                        .border(
                                            width = if (isSel) 1.dp else 0.dp,
                                            color = if (isSel) palette.primary else palette.bg,
                                            shape = RoundedCornerShape(10.dp),
                                        )
                                        .clickable(enabled = enabled) { selectedDay = iso }
                                        .padding(vertical = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Text(
                                        text = "${date.dayOfMonth}",
                                        color = when {
                                            isSel -> palette.primary
                                            enabled -> palette.text
                                            else -> palette.textDisabled
                                        },
                                        fontSize = 13.sp,
                                        fontWeight = if (isSel || enabled) FontWeight.SemiBold else FontWeight.Normal,
                                        style = TextStyle(fontFeatureSettings = TABULAR_FONT_FEATURE),
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if ((entry?.unseen ?: 0) > 0) palette.primary else palette.bg.copy(alpha = 0f),
                                            ),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            val selInfo = selectedDay?.let { dayMap[it] }
            Text(
                text = if (selInfo != null) {
                    "选中：${selInfo.day} · ${selInfo.totalCount} 帖" + if (selInfo.unseen > 0) " · 未看 ${selInfo.unseen}" else ""
                } else {
                    "请选择有更新的日期"
                },
                color = palette.muted,
                fontSize = 12.sp,
                style = TextStyle(fontFeatureSettings = TABULAR_FONT_FEATURE),
            )

            Button(
                enabled = selectedDay != null,
                onClick = {
                    selectedDay?.let {
                        onSelectDay(it)
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = palette.primary, contentColor = palette.onPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
            ) {
                Text("跳到这一天", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/**
 * Full-screen Search & Multi-Select Tag / Author / Series Picker (`02a §3`).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WallSearchScreen(
    api: ForHappyApiClient,
    currentFilter: WallFilterState,
    blurAllEnabled: Boolean,
    popularTags: List<TagStatItem>,
    initialTab: String = "posts", // "posts" | "tags" | "authors"
    onOpenMeme: (Long) -> Unit,
    onApplyFilter: (WallFilterState) -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalForHappyPalette.current
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()

    var activeSegment by remember(initialTab) { mutableStateOf(initialTab) }
    var query by remember { mutableStateOf(currentFilter.searchQuery) }
    var recentSearches by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedTags by remember { mutableStateOf(currentFilter.tags) }
    var selectedAuthors by remember {
        mutableStateOf(
            currentFilter.author.split(',', ' ', '，').filter { it.isNotBlank() }.toSet(),
        )
    }

    var postResults by remember { mutableStateOf<List<MemeItem>>(emptyList()) }
    var loadingPosts by remember { mutableStateOf(false) }
    var authorResults by remember { mutableStateOf<List<AuthorOption>>(emptyList()) }
    var loadingAuthors by remember { mutableStateOf(false) }
    var showSeriesDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val rawRecent = api.getPref("ui.wall_recent_searches")
        if (!rawRecent.isNullOrBlank()) {
            recentSearches = runCatching {
                ForHappyJson.decodeFromString(ListSerializer(String.serializer()), rawRecent)
            }.getOrDefault(emptyList())
        }
        delay(80L)
        runCatching { focusRequester.requestFocus() }
    }

    val isScrolling by remember { derivedStateOf { listState.isScrollInProgress } }
    LaunchedEffect(isScrolling) {
        if (isScrolling) keyboardController?.hide()
    }

    fun saveRecentSearch(term: String) {
        val clean = term.trim()
        if (clean.isBlank()) return
        val next = (listOf(clean) + recentSearches.filter { it != clean }).take(12)
        recentSearches = next
        scope.launch {
            api.putPref("ui.wall_recent_searches", ForHappyJson.encodeToString(ListSerializer(String.serializer()), next))
        }
    }

    LaunchedEffect(query, activeSegment) {
        val clean = query.trim()
        when (activeSegment) {
            "posts" -> {
                if (clean.isBlank()) {
                    postResults = emptyList()
                } else {
                    delay(260L)
                    loadingPosts = true
                    postResults = api.fetchMemes(
                        limit = 30,
                        query = clean,
                        nsfwModeParam = currentFilter.nsfwQueryValue(),
                    )
                    loadingPosts = false
                }
            }
            "authors" -> {
                delay(200L)
                loadingAuthors = true
                authorResults = api.fetchAuthors(
                    query = clean,
                    nsfwModeParam = currentFilter.nsfwQueryValue(),
                )
                loadingAuthors = false
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = FullscreenDialogProperties,
    ) {
        ConfigureFullscreenDialogWindow(darkStatusIcons = !palette.isDark)
        DialogBackOwnerScope {
            BackHandler(onBack = onDismiss)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.bg)
                .navigationBarsPadding(),
        ) {
            Surface(color = palette.surface) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回", tint = palette.text)
                    }
                    Surface(
                        color = palette.surface2,
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .then(
                                if (palette.isModernUi && palette.cardBorder == Color.Transparent) {
                                    Modifier
                                } else {
                                    Modifier.border(
                                        1.dp,
                                        if (palette.isModernUi) palette.cardBorder else palette.line,
                                        RoundedCornerShape(999.dp),
                                    )
                                },
                            ),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.Search, contentDescription = null, tint = palette.muted, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                            if (query.isEmpty()) {
                                Text("搜索帖子、标签、发图人", color = palette.muted, fontSize = 13.sp, maxLines = 1)
                            }
                            BasicTextField(
                                value = query,
                                onValueChange = { query = it },
                                singleLine = true,
                                textStyle = TextStyle(color = palette.text, fontSize = 14.sp),
                                cursorBrush = SolidColor(palette.primary),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(
                                    onSearch = {
                                        keyboardController?.hide()
                                        saveRecentSearch(query)
                                        if (activeSegment == "posts" && query.isNotBlank()) {
                                            onApplyFilter(currentFilter.copy(searchQuery = query.trim()))
                                            onDismiss()
                                        }
                                    },
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester),
                            )
                        }
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Rounded.Close, contentDescription = "清空", tint = palette.muted, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.surface)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf("posts" to "帖子", "tags" to "标签", "authors" to "发图人").forEach { (key, label) ->
                    WallSheetPill(label = label, selected = activeSegment == key) {
                        activeSegment = key
                    }
                }
            }
            HorizontalDivider(
                color = if (palette.isModernUi) palette.glassBorder else palette.line,
                thickness = if (palette.isModernUi) 0.5.dp else 1.dp,
            )

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (activeSegment) {
                    "posts" -> {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            if (query.isBlank()) {
                                if (recentSearches.isNotEmpty()) {
                                    item {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text("最近搜索", color = palette.muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                            Text(
                                                text = "清空",
                                                color = palette.primary,
                                                fontSize = 12.sp,
                                                modifier = Modifier.clickable {
                                                    recentSearches = emptyList()
                                                    scope.launch { api.putPref("ui.wall_recent_searches", "[]") }
                                                },
                                            )
                                        }
                                        Spacer(Modifier.height(8.dp))
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                            recentSearches.forEach { term ->
                                                WallSheetPill(label = term, selected = false) { query = term }
                                            }
                                        }
                                    }
                                }
                                item {
                                    Text("热门标签", color = palette.muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Spacer(Modifier.height(8.dp))
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        popularTags.take(20).forEach { tag ->
                                            WallSheetPill(label = "#${tag.tag}", selected = selectedTags.contains(tag.tag)) {
                                                onApplyFilter(currentFilter.copy(tags = setOf(tag.tag)))
                                                onDismiss()
                                            }
                                        }
                                    }
                                }
                            } else if (loadingPosts) {
                                item {
                                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(color = palette.primary)
                                    }
                                }
                            } else if (postResults.isEmpty()) {
                                item {
                                    Text("没有找到与“$query”相关的帖子", color = palette.muted, fontSize = 13.sp)
                                }
                            } else {
                                item {
                                    OutlinedButton(
                                        onClick = {
                                            saveRecentSearch(query)
                                            onApplyFilter(currentFilter.copy(searchQuery = query.trim()))
                                            onDismiss()
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Text("在图廊查看全部“${query.trim()}”结果")
                                    }
                                }
                                items(postResults, key = { it.id }) { m ->
                                    val thumbUrl = remember(m.id) { MediaUrlRewriter.rewriteMediaUrl(m.primaryImageUrl) }
                                    val masked = currentFilter.shouldBlurMeme(m, blurAllEnabled)
                                    Surface(
                                        color = palette.surface,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, palette.line, RoundedCornerShape(12.dp))
                                            .clickable {
                                                saveRecentSearch(query)
                                                onOpenMeme(m.id)
                                            },
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            if (thumbUrl.isNotBlank()) {
                                                AsyncImage(
                                                    model = thumbUrl,
                                                    imageLoader = api.imageLoader,
                                                    contentDescription = m.safeLabel,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .size(56.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(palette.surface2)
                                                        .let { mod -> if (masked) mod.blur(18.dp) else mod },
                                                )
                                            }
                                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text(m.safeLabel, color = palette.text, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                                Text("@${m.authorDisplay}", color = palette.muted, fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    "tags" -> {
                        val needle = query.trim().removePrefix("#").lowercase()
                        val shownTags = remember(popularTags, needle, selectedTags) {
                            val base = (selectedTags.map { TagStatItem(tag = it) } + popularTags)
                                .distinctBy { it.tag }
                            if (needle.isBlank()) base else base.filter { it.tag.lowercase().contains(needle) }
                        }
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            item {
                                Surface(
                                    color = palette.primaryContainer,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showSeriesDialog = true },
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Icon(Icons.Rounded.CollectionsBookmark, contentDescription = null, tint = palette.primary, modifier = Modifier.size(18.dp))
                                            Text("专题图集", color = palette.primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = palette.primary)
                                    }
                                }
                            }
                            items(shownTags, key = { it.tag }) { tag ->
                                val checked = selectedTags.contains(tag.tag)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(palette.surface)
                                        .clickable {
                                            selectedTags = if (checked) selectedTags - tag.tag else selectedTags + tag.tag
                                        }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(
                                            imageVector = if (checked) Icons.Rounded.CheckBox else Icons.Rounded.CheckBoxOutlineBlank,
                                            contentDescription = null,
                                            tint = if (checked) palette.primary else palette.muted,
                                        )
                                        Text("#${tag.tag}", color = palette.text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    }
                                    if (tag.totalCount > 0) {
                                        Text("${tag.totalCount}", color = palette.muted, fontSize = 12.sp, style = TextStyle(fontFeatureSettings = TABULAR_FONT_FEATURE))
                                    }
                                }
                            }
                        }
                    }

                    "authors" -> {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (loadingAuthors) {
                                item {
                                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(color = palette.primary)
                                    }
                                }
                            } else {
                                items(authorResults, key = { it.username }) { author ->
                                    val checked = selectedAuthors.any { it.equals(author.username, ignoreCase = true) }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(palette.surface)
                                            .clickable {
                                                selectedAuthors = if (checked) {
                                                    selectedAuthors.filterNot { it.equals(author.username, ignoreCase = true) }.toSet()
                                                } else {
                                                    selectedAuthors + author.username
                                                }
                                            }
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Icon(
                                                imageVector = if (checked) Icons.Rounded.CheckBox else Icons.Rounded.CheckBoxOutlineBlank,
                                                contentDescription = null,
                                                tint = if (checked) palette.primary else palette.muted,
                                            )
                                            ForHappyAvatar(
                                                username = author.displayName,
                                                avatarUrl = author.avatar,
                                                imageLoader = api.imageLoader,
                                                size = 32.dp,
                                            )
                                            Column {
                                                Text(author.displayName, color = palette.text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                                Text("@${author.username}", color = palette.muted, fontSize = 11.sp)
                                            }
                                        }
                                        Text("${author.posts} 帖", color = palette.muted, fontSize = 12.sp, style = TextStyle(fontFeatureSettings = TABULAR_FONT_FEATURE))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (activeSegment == "tags" || activeSegment == "authors") {
                val totalSelected = selectedTags.size + selectedAuthors.size
                Surface(color = palette.surface) {
                    Button(
                        onClick = {
                            onApplyFilter(
                                currentFilter.copy(
                                    tags = selectedTags,
                                    author = selectedAuthors.joinToString(","),
                                ),
                            )
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = palette.primary, contentColor = palette.onPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                            .height(46.dp),
                    ) {
                        Text(
                            text = if (totalSelected > 0) "用已选 $totalSelected 项筛选图廊" else "应用筛选（显示全部）",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
        }
    }

    if (showSeriesDialog) {
        SeriesIndexDialog(
            api = api,
            currentFilter = currentFilter,
            blurAllEnabled = blurAllEnabled,
            onSelectSeries = { seriesName ->
                showSeriesDialog = false
                onApplyFilter(currentFilter.copy(series = seriesName))
                onDismiss()
            },
            onDismiss = { showSeriesDialog = false },
        )
    }
}

/**
 * Series Index Fullscreen Dialog (`02a §3`, `/series`).
 */
@Composable
fun SeriesIndexDialog(
    api: ForHappyApiClient,
    currentFilter: WallFilterState,
    blurAllEnabled: Boolean,
    onSelectSeries: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalForHappyPalette.current
    var loading by remember { mutableStateOf(true) }
    var seriesList by remember { mutableStateOf<List<SeriesItem>>(emptyList()) }
    var sortKey by remember { mutableStateOf("last") } // "last" | "first" | "n" | "likes"

    LaunchedEffect(currentFilter.nsfwMode, currentFilter.unseenOnly) {
        loading = true
        seriesList = api.fetchSeriesList(
            nsfwModeParam = currentFilter.nsfwQueryValue(),
            unseenOnly = currentFilter.unseenOnly,
        )
        loading = false
    }

    val sortedSeries = remember(seriesList, sortKey) {
        when (sortKey) {
            "first" -> seriesList.sortedBy { it.firstAt }
            "n" -> seriesList.sortedByDescending { it.n }
            "likes" -> seriesList.sortedByDescending { it.likes }
            else -> seriesList.sortedByDescending { it.lastAt }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = FullscreenDialogProperties,
    ) {
        ConfigureFullscreenDialogWindow(darkStatusIcons = !palette.isDark)
        DialogBackOwnerScope {
            BackHandler(onBack = onDismiss)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.bg)
                .navigationBarsPadding(),
        ) {
            Surface(color = palette.surface) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回", tint = palette.text)
                    }
                    Text("专题图集", color = palette.text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
            }

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.surface)
                    .padding(bottom = 8.dp),
                contentPadding = PaddingValues(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(
                    listOf("last" to "最近更新", "first" to "最早一期", "n" to "图最多", "likes" to "最受欢迎"),
                ) { (k, label) ->
                    WallSheetPill(label = label, selected = sortKey == k) { sortKey = k }
                }
            }
            HorizontalDivider(
                color = if (palette.isModernUi) palette.glassBorder else palette.line,
                thickness = if (palette.isModernUi) 0.5.dp else 1.dp,
            )

            if (loading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = palette.primary)
                }
            } else if (sortedSeries.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("暂无可用专题图集（可能需先登录）", color = palette.muted, fontSize = 14.sp)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(sortedSeries, key = { it.name }) { s ->
                        Surface(
                            color = palette.surface,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, palette.line, RoundedCornerShape(12.dp))
                                .clickable { onSelectSeries(s.name) },
                        ) {
                            Column {
                                val cover = s.covers.firstOrNull()
                                val coverUrl = remember(cover) { MediaUrlRewriter.rewriteMediaUrl(cover?.src) }
                                val blurCover = blurAllEnabled || (cover?.spoiler == true && currentFilter.nsfwMode != "show")
                                if (coverUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = coverUrl,
                                        imageLoader = api.imageLoader,
                                        contentDescription = s.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(1.25f)
                                            .background(palette.surface2)
                                            .let { if (blurCover) it.blur(20.dp) else it },
                                    )
                                }
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(s.name, color = palette.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    s.desc?.takeIf { it.isNotBlank() }?.let { d ->
                                        Text(d, color = palette.muted, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    }
                                    Text(
                                        text = "${s.n} 张" + if (s.likes > 0) " · ♡ ${s.likes}" else "",
                                        color = palette.muted,
                                        fontSize = 11.sp,
                                        style = TextStyle(fontFeatureSettings = TABULAR_FONT_FEATURE),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        }
    }
}

/**
 * Shared Quick Actions & Management Sheet (`02a §5.1` card long-press & `02b §4` detail `⋯`):
 * Quick boosts/stickers + inline multi-folder favorite + inline forward to chat + pin anchor +
 * copy/save/source + author/admin edit/NSFW/delete.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MemeQuickActionsSheet(
    meme: MemeItem,
    api: ForHappyApiClient,
    me: MeUser?,
    blurThumbnail: Boolean = false,
    showQuickInteractions: Boolean = true,
    onRequireLogin: () -> Unit,
    onMemeUpdated: (MemeItem) -> Unit,
    onDeleted: (Long) -> Unit,
    onAnchorsUpdated: (List<AnchorTag>) -> Unit = {},
    onOpenUrl: (String) -> Unit = {},
    onStatusToast: (String) -> Unit = {},
    onOpenEditWorkbench: ((MemeItem) -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    val palette = LocalForHappyPalette.current
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var subMode by remember { mutableStateOf("main") } // "main" | "fav" | "forward" | "edit" | "custom_boost"
    var folders by remember { mutableStateOf<List<FolderItem>>(emptyList()) }
    var newFolderName by remember { mutableStateOf("") }
    var chatGroups by remember { mutableStateOf<List<ChatGroup>>(emptyList()) }
    var forwardNote by remember { mutableStateOf("") }
    var customBoostDraft by remember { mutableStateOf("") }
    var editTitle by remember(meme.id) { mutableStateOf(meme.title.orEmpty()) }
    var editContent by remember(meme.id) { mutableStateOf(meme.content ?: meme.text.orEmpty()) }
    var editTagsRaw by remember(meme.id) { mutableStateOf(meme.tags.joinToString(" ")) }
    var editSpoiler by remember(meme.id) { mutableStateOf(meme.isRisky) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val canManage = meme.canManage || me?.isAdmin == true || (me != null && meme.authorId == me.id)

    LaunchedEffect(subMode) {
        if (subMode == "fav" && folders.isEmpty() && me != null) {
            folders = api.fetchFolders()
        } else if (subMode == "forward" && chatGroups.isEmpty() && me != null) {
            chatGroups = api.fetchChatGroups().filter { it.joined }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.bg,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (subMode) {
                "fav" -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = { subMode = "main" }) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回", tint = palette.text)
                        }
                        Text("收藏到…", color = palette.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedTextField(
                            value = newFolderName,
                            onValueChange = { if (it.length <= 12) newFolderName = it },
                            placeholder = { Text("＋ 新建收藏夹（≤12字）", fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        Button(
                            enabled = newFolderName.isNotBlank(),
                            onClick = {
                                scope.launch {
                                    val (createdId, updatedFolders) = api.createFolder(newFolderName)
                                    if (updatedFolders.isNotEmpty()) folders = updatedFolders
                                    if (createdId != null) {
                                        api.toggleMemeFolderFav(meme.id, createdId)?.let(onMemeUpdated)
                                        newFolderName = ""
                                    }
                                }
                            },
                        ) {
                            Text("新建")
                        }
                    }
                    LazyColumn(modifier = Modifier.height(220.dp)) {
                        items(folders, key = { it.id }) { folder ->
                            val checked = meme.favFolders.contains(folder.id)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        scope.launch {
                                            api.toggleMemeFolderFav(meme.id, folder.id)?.let(onMemeUpdated)
                                        }
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = checked, onCheckedChange = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text(folder.name, color = palette.text, fontSize = 14.sp)
                                }
                                Text("${folder.itemCount}", color = palette.muted, fontSize = 12.sp)
                            }
                        }
                    }
                    if (meme.favFolders.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                scope.launch {
                                    api.clearMemeFav(meme.id)?.let(onMemeUpdated)
                                    onStatusToast("已取消收藏")
                                    onDismiss()
                                }
                            },
                        ) {
                            Text("取消所有收藏", color = palette.danger)
                        }
                    }
                }

                "forward" -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = { subMode = "main" }) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回", tint = palette.text)
                        }
                        Text("转发到群聊", color = palette.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedTextField(
                        value = forwardNote,
                        onValueChange = { forwardNote = it },
                        placeholder = { Text("附带一句话（可选）", fontSize = 13.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    LazyColumn(
                        modifier = Modifier.height(240.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(chatGroups, key = { it.id }) { grp ->
                            Surface(
                                color = palette.surface2,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        scope.launch {
                                            val body = buildString {
                                                if (forwardNote.isNotBlank()) append(forwardNote.trim()).append('\n')
                                                append("No.${meme.id}")
                                            }
                                            api.sendChatMessage(grp.id, body, replyToId = null, retry = false).fold(
                                                onSuccess = {
                                                    onStatusToast("已转发到「${grp.name}」")
                                                    onDismiss()
                                                },
                                                onFailure = { onStatusToast(it.message ?: "转发失败") },
                                            )
                                        }
                                    },
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(grp.name, color = palette.text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "发送", tint = palette.primary, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }

                "edit" -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = { subMode = "main" }) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回", tint = palette.text)
                        }
                        Text("编辑帖子 No.${meme.id}", color = palette.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it.take(80) },
                        label = { Text("标题") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = editContent,
                        onValueChange = { editContent = it },
                        label = { Text("正文 / 说明") },
                        minLines = 3,
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = editTagsRaw,
                        onValueChange = { editTagsRaw = it },
                        label = { Text("标签（空格或逗号分隔）") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("标为敏感内容", color = palette.text, fontSize = 14.sp)
                        Switch(checked = editSpoiler, onCheckedChange = { editSpoiler = it })
                    }
                    Button(
                        onClick = {
                            scope.launch {
                                val tags = editTagsRaw.split(',', ' ', '，', '#').map { it.trim() }.filter { it.isNotEmpty() }
                                api.patchMemeBasic(meme.id, editTitle.trim(), editContent.trim(), tags, editSpoiler).fold(
                                    onSuccess = { updated ->
                                        if (updated != null) onMemeUpdated(updated)
                                        onStatusToast("已保存修改")
                                        onDismiss()
                                    },
                                    onFailure = { onStatusToast(it.message ?: "保存失败") },
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("保存修改")
                    }
                }

                "custom_boost" -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = { subMode = "main" }) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回", tint = palette.text)
                        }
                        Text("写一句助推", color = palette.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    val units = countReplyUnits(customBoostDraft)
                    OutlinedTextField(
                        value = customBoostDraft,
                        onValueChange = { customBoostDraft = it },
                        placeholder = { Text("12 个显示单位以内") },
                        singleLine = true,
                        supportingText = {
                            Text("$units / $BOOST_MAX_LEN", color = if (units > BOOST_MAX_LEN) palette.danger else palette.muted)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        enabled = units in 1..BOOST_MAX_LEN,
                        onClick = {
                            scope.launch {
                                api.putMemeQuickReply(meme.id, customBoostDraft.trim()).fold(
                                    onSuccess = { updated ->
                                        updated?.let(onMemeUpdated)
                                        onStatusToast("已发送「${customBoostDraft.trim()}」")
                                        onDismiss()
                                    },
                                    onFailure = { onStatusToast(it.message ?: "发送失败") },
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("发送")
                    }
                }

                else -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val thumb = remember(meme.id) { MediaUrlRewriter.rewriteMediaUrl(meme.primaryImageUrl) }
                        if (thumb.isNotBlank()) {
                            AsyncImage(
                                model = thumb,
                                imageLoader = api.imageLoader,
                                contentDescription = meme.safeLabel,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(palette.surface2)
                                    .let { if (blurThumbnail) it.blur(16.dp) else it },
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(meme.safeLabel, color = palette.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("@${meme.authorDisplay} · No.${meme.id}", color = palette.muted, fontSize = 12.sp)
                        }
                    }

                    if (showQuickInteractions) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text("一句", color = palette.muted, fontSize = 12.sp, modifier = Modifier.width(34.dp))
                                LazyRow(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    items(listOf("善", "甚好", "666", "神！", "挂！")) { word ->
                                        WallSheetPill(label = word, selected = meme.myReply == word) {
                                            if (me == null) {
                                                onRequireLogin()
                                            } else {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                scope.launch {
                                                    val res = if (meme.myReply == word) api.deleteMemeQuickReply(meme.id) else api.putMemeQuickReply(meme.id, word)
                                                    res.getOrNull()?.let(onMemeUpdated)
                                                    onDismiss()
                                                }
                                            }
                                        }
                                    }
                                }
                                IconButton(
                                    onClick = { if (me == null) onRequireLogin() else subMode = "custom_boost" },
                                    modifier = Modifier.size(32.dp),
                                ) {
                                    Icon(Icons.Rounded.Edit, contentDescription = "自定义一句", tint = palette.primary, modifier = Modifier.size(18.dp))
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text("贴纸", color = palette.muted, fontSize = 12.sp, modifier = Modifier.width(34.dp))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    items(PICKED_REACTIONS.take(8)) { emo ->
                                        Surface(
                                            color = palette.surface2,
                                            shape = RoundedCornerShape(999.dp),
                                            modifier = Modifier
                                                .clickable {
                                                    if (me == null) {
                                                        onRequireLogin()
                                                    } else {
                                                        scope.launch {
                                                            api.reactToMeme(meme.id, emo)?.let(onMemeUpdated)
                                                            onDismiss()
                                                        }
                                                    }
                                                }
                                                .padding(horizontal = 8.dp, vertical = 5.dp),
                                        ) {
                                            ReactionEmojiView(
                                                emojiKey = emo,
                                                imageLoader = api.imageLoader,
                                                size = 20.dp,
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

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        QuickActionButton(
                            icon = Icons.Rounded.BookmarkBorder,
                            label = "收藏到…",
                            modifier = Modifier.weight(1f),
                        ) {
                            if (me == null) onRequireLogin() else subMode = "fav"
                        }
                        QuickActionButton(
                            icon = Icons.AutoMirrored.Rounded.Send,
                            label = "转发到群聊",
                            modifier = Modifier.weight(1f),
                        ) {
                            if (me == null) onRequireLogin() else subMode = "forward"
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        QuickActionButton(
                            icon = Icons.Rounded.PushPin,
                            label = "插锚点",
                            modifier = Modifier.weight(1f),
                        ) {
                            if (me == null) {
                                onRequireLogin()
                            } else {
                                scope.launch {
                                    val updated = api.putAnchor(meme.id, meme.title.orEmpty().take(20))
                                    onAnchorsUpdated(updated)
                                    onStatusToast("已设为锚点")
                                    onDismiss()
                                }
                            }
                        }
                        QuickActionButton(
                            icon = Icons.Rounded.ContentCopy,
                            label = "复制链接",
                            modifier = Modifier.weight(1f),
                        ) {
                            clipboard.setText(AnnotatedString("https://52dmeme.com/m/${meme.id}"))
                            onStatusToast("已复制 No.${meme.id} 链接")
                            onDismiss()
                        }
                    }

                    val saveUrl = remember(meme.id) {
                        MediaUrlRewriter.rewriteMediaUrl(
                            meme.mediaItems.firstOrNull()?.let { it.v ?: it.resolvedFullUrl } ?: meme.primaryImageUrl,
                        )
                    }
                    if (saveUrl.isNotBlank() || !meme.sourceUrl.isNullOrBlank()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (saveUrl.isNotBlank()) {
                                QuickActionButton(
                                    icon = Icons.Rounded.Download,
                                    label = "保存到相册",
                                    modifier = Modifier.weight(1f),
                                ) {
                                    scope.launch {
                                        saveMediaToSystemGallery(context, api, saveUrl).fold(
                                            onSuccess = { onStatusToast(it) },
                                            onFailure = { onStatusToast(it.message ?: "保存失败") },
                                        )
                                        onDismiss()
                                    }
                                }
                            }
                            if (!meme.sourceUrl.isNullOrBlank()) {
                                QuickActionButton(
                                    icon = Icons.Rounded.Link,
                                    label = "查看来源",
                                    modifier = Modifier.weight(1f),
                                ) {
                                    onOpenUrl(meme.sourceUrl)
                                    onDismiss()
                                }
                            }
                        }
                    }

                    if (canManage) {
                        HorizontalDivider(
                color = if (palette.isModernUi) palette.glassBorder else palette.line,
                thickness = if (palette.isModernUi) 0.5.dp else 1.dp,
            )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            QuickActionButton(
                                icon = Icons.Rounded.Edit,
                                label = "编辑帖子",
                                modifier = Modifier.weight(1f),
                            ) {
                                if (onOpenEditWorkbench != null) {
                                    onOpenEditWorkbench(meme)
                                    onDismiss()
                                } else {
                                    subMode = "edit"
                                }
                            }
                            QuickActionButton(
                                icon = Icons.Rounded.WarningAmber,
                                label = if (meme.isRisky) "取消敏感" else "标为敏感",
                                modifier = Modifier.weight(1f),
                            ) {
                                scope.launch {
                                    api.setMemeNsfw(meme.id, !meme.isRisky)?.let(onMemeUpdated)
                                    onStatusToast(if (!meme.isRisky) "已标为敏感" else "已取消敏感标记")
                                    onDismiss()
                                }
                            }
                            QuickActionButton(
                                icon = Icons.Rounded.DeleteOutline,
                                label = "删除",
                                isDanger = true,
                                modifier = Modifier.weight(1f),
                            ) {
                                showDeleteConfirm = true
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = palette.surface,
            title = { Text("删除帖子 No.${meme.id}？", color = palette.text) },
            text = { Text("删除后将从图廊移除且无法恢复。", color = palette.muted) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        scope.launch {
                            if (api.deleteMeme(meme.id)) {
                                onDeleted(meme.id)
                                onDismiss()
                            } else {
                                onStatusToast("删除失败")
                            }
                        }
                    },
                ) {
                    Text("确认删除", color = palette.danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    isDanger: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val palette = LocalForHappyPalette.current
    Surface(
        color = palette.surface2,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.clickable { onClick() },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isDanger) palette.danger else palette.text,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = label,
                color = if (isDanger) palette.danger else palette.text,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
