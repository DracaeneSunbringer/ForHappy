package com.jinhuan.forhappy.ui.disguise

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.ThumbUpOffAlt
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jinhuan.forhappy.core.model.ForHappyJson
import com.jinhuan.forhappy.core.network.api.ForHappyApiClient
import com.jinhuan.forhappy.ui.theme.BossDocDarkPalette
import com.jinhuan.forhappy.ui.theme.BossDocLightPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import java.util.Calendar
import java.util.Locale

private data class BossMetricRow(
    val name: String,
    val target: String,
    val actual: String,
    val rate: String,
    val ok: Boolean,
    val owner: String,
)

private data class BossTodoItem(
    val text: String,
    val done: Boolean,
)

private data class BossKbEntry(
    val title: String,
    val meta: String,
)

/**
 * Full-screen Feishu Cloud Document boss-key overlay matching `docs/redesign/03-privacy-state-machine.md §7`:
 * - Top-left `‹` opens a decoy knowledge-base doc list; `☰` opens right-side outline drawer
 * - Interactive local-persisted todo items (`boss.todos.v1`)
 * - Bottom Feishu comment / share / more action bar with subtle in-doc toast feedback
 * - Zero giveaway text or exit buttons in active Boss mode (`isPreviewMode = false`)
 * - Exits only via configured stealth triggers or covert 2s long-press on doc title / 5-tap collaborator avatars
 */
@Composable
fun BossDocScreen(
    longPressTitleEnabled: Boolean = true,
    tapCollabFiveEnabled: Boolean = false,
    doubleBackExitEnabled: Boolean = false,
    hapticEnabled: Boolean = true,
    api: ForHappyApiClient? = null,
    isPreviewMode: Boolean = false,
    onExit: () -> Unit,
) {
    val bossPalette = if (isSystemInDarkTheme()) BossDocDarkPalette else BossDocLightPalette
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var activeOutline by remember { mutableIntStateOf(0) }
    var showFakeKnowledgeBase by remember { mutableStateOf(false) }
    var showOutlineDrawer by remember { mutableStateOf(false) }
    var docToast by remember { mutableStateOf<String?>(null) }

    var collabTapCount by remember { mutableIntStateOf(0) }
    var lastCollabTapMs by remember { mutableLongStateOf(0L) }

    val triggerSecretExit by rememberUpdatedState {
        if (hapticEnabled) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        onExit()
    }

    LaunchedEffect(docToast) {
        if (docToast != null) {
            delay(2000L)
            docToast = null
        }
    }

    when {
        isPreviewMode -> BackHandler(enabled = true) { onExit() }
        showOutlineDrawer -> BackHandler(enabled = true) { showOutlineDrawer = false }
        showFakeKnowledgeBase -> BackHandler(enabled = true) { showFakeKnowledgeBase = false }
        doubleBackExitEnabled -> DoubleBackBossHandler(enabled = true, onTriggerBoss = { triggerSecretExit() })
        else -> BackHandler(enabled = true) { /* Swallow single system back to keep cloud-doc cover intact */ }
    }

    val docBlue = bossPalette.link
    val bg = bossPalette.bg
    val barBg = bossPalette.barBg
    val ink = bossPalette.text
    val subInk = bossPalette.subText
    val muted = bossPalette.textMuted
    val line = bossPalette.tableBorder
    val calloutBg = bossPalette.calloutBg
    val okBg = bossPalette.okBg
    val okFg = bossPalette.okFg
    val warnBg = bossPalette.warnBg
    val warnFg = bossPalette.warnFg

    val modifiedStamp = remember {
        val cal = Calendar.getInstance()
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val hour = maxOf(9, cal.get(Calendar.HOUR_OF_DAY) - 1)
        val min = (cal.get(Calendar.MINUTE) * 7) % 60
        String.format(Locale.US, "%d月%d日 %02d:%02d", month, day, hour, min)
    }

    val kbDocs = remember(modifiedStamp) {
        listOf(
            BossKbEntry("2026 Q4 项目复盘与规划", "最近修改：$modifiedStamp · PMO"),
            BossKbEntry("跨部门研发排期与验收标准 v2.4", "3 小时前 · 平台架构组"),
            BossKbEntry("2026 Q4 OKR 对齐与指标拆分", "昨天 19:20 · PMO"),
            BossKbEntry("新员工入职指引与权限申请流程", "本周一 · HRBP"),
            BossKbEntry("线上 SLA 故障应急响应手册", "上周 · SRE 团队"),
            BossKbEntry("供应商季度结算核对清单", "10 月 28 日 · 财务 BP"),
        )
    }

    val outlineItems = remember {
        listOf(
            "一、整体结论" to 2,
            "二、关键数据" to 3,
            "三、问题复盘" to 4,
            "四、下阶段重点" to 5,
            "五、风险与依赖" to 6,
            "六、待确认事项" to 7,
        )
    }

    val metrics = remember {
        listOf(
            BossMetricRow("周活跃用户", "120 万", "126.4 万", "105%", true, "增长组"),
            BossMetricRow("主链路转化率", "21.0%", "22.6%", "108%", true, "产品组"),
            BossMetricRow("平均响应时长", "≤ 320ms", "286ms", "达成", true, "平台架构"),
            BossMetricRow("需求交付准时率", "90%", "84%", "93%", false, "PMO"),
            BossMetricRow("自动化覆盖率", "70%", "63%", "90%", false, "质量保障"),
        )
    }

    val todos = remember {
        mutableStateListOf(
            BossTodoItem("Q4 预算拆分方案（@财务BP）", false),
            BossTodoItem("试点团队名单（@运营协同）", false),
            BossTodoItem("复盘会议室与时间（已订 周五 14:00）", true),
            BossTodoItem("权限体系重构一期验收标准确认（@李娜）", false),
        )
    }

    LaunchedEffect(api) {
        val raw = api?.getPref("boss.todos.v1") ?: return@LaunchedEffect
        val flags = runCatching { ForHappyJson.decodeFromString(ListSerializer(Boolean.serializer()), raw) }.getOrNull()
        if (flags != null) {
            for (i in todos.indices) {
                if (i < flags.size) todos[i] = todos[i].copy(done = flags[i])
            }
        }
    }

    fun persistTodos() {
        val flags = todos.map { it.done }
        scope.launch {
            api?.putPref("boss.todos.v1", ForHappyJson.encodeToString(ListSerializer(Boolean.serializer()), flags))
        }
    }

    if (showFakeKnowledgeBase) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(bg)
                .navigationBarsPadding(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(barBg)
                    .statusBarsPadding(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FolderOpen,
                        contentDescription = null,
                        tint = docBlue,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = "知识库 · 项目管理办公室",
                        color = ink,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    if (isPreviewMode) {
                        Surface(
                            color = docBlue.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(999.dp),
                            modifier = Modifier.clickable { onExit() },
                        ) {
                            Text("关闭预览", color = docBlue, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                        }
                    }
                }
                HorizontalDivider(color = line)
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bg),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                itemsIndexed(kbDocs) { idx, entry ->
                    Surface(
                        color = if (idx == 0) calloutBg else barBg,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, line, RoundedCornerShape(10.dp))
                            .clickable { showFakeKnowledgeBase = false },
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(docBlue.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Description,
                                    contentDescription = null,
                                    tint = docBlue,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(
                                    text = entry.title,
                                    color = ink,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(entry.meta, color = muted, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .navigationBarsPadding(),
    ) {
        val scrolledPastDocH1 by remember {
            derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 88 }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // Authentic Feishu Mobile Cloud Doc Top Bar (minimal chrome; title reveals on scroll)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bg)
                    .statusBarsPadding(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = { showFakeKnowledgeBase = true },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                            contentDescription = "知识库目录",
                            tint = ink,
                            modifier = Modifier.size(25.dp),
                        )
                    }

                    if (scrolledPastDocH1) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { scope.launch { listState.animateScrollToItem(0) } },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(docBlue),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Description,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp),
                                )
                            }
                            Text(
                                text = "2026 Q4 项目复盘与规划",
                                color = ink,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    } else {
                        Text(
                            text = "已保存到云端",
                            color = muted,
                            fontSize = 11.5.sp,
                            maxLines = 1,
                            modifier = Modifier.weight(1f),
                        )
                    }

                    Spacer(Modifier.width(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy((-4).dp),
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            enabled = tapCollabFiveEnabled,
                        ) {
                            val now = SystemClock.uptimeMillis()
                            collabTapCount = if (now - lastCollabTapMs <= 1200L) collabTapCount + 1 else 1
                            lastCollabTapMs = now
                            if (collabTapCount >= 5) {
                                collabTapCount = 0
                                triggerSecretExit()
                            }
                        },
                    ) {
                        BossCollabDot("产", docBlue, bg)
                        BossCollabDot("研", Color(0xFF14B8A6), bg)
                        BossCollabDot("设", Color(0xFFFF7D00), bg)
                    }
                    IconButton(
                        onClick = { docToast = "链接已复制（企业内成员可阅读）" },
                        modifier = Modifier.size(34.dp),
                    ) {
                        Icon(Icons.Rounded.Share, contentDescription = "分享文档", tint = ink, modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = { showOutlineDrawer = !showOutlineDrawer },
                        modifier = Modifier.size(34.dp),
                    ) {
                        Icon(Icons.Rounded.MoreHoriz, contentDescription = "文档更多与目录", tint = ink, modifier = Modifier.size(20.dp))
                    }
                    if (isPreviewMode) {
                        Surface(
                            color = docBlue,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.clickable { onExit() },
                        ) {
                            Text(
                                text = "关闭预览",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
                if (scrolledPastDocH1) {
                    HorizontalDivider(color = line, thickness = 0.5.dp)
                }
            }

            // Document body
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(bg),
                contentPadding = PaddingValues(start = 20.dp, end = 24.dp, top = 12.dp, bottom = 22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Item 0: Breadcrumb + Title + Metadata (covert 2000ms long-press on title exits Boss Doc)
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier.clickable { showFakeKnowledgeBase = true },
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FolderOpen,
                                contentDescription = null,
                                tint = docBlue,
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                text = "项目管理办公室 / 季度复盘归档",
                                color = muted,
                                fontSize = 12.sp,
                            )
                        }
                        Text(
                            text = "2026 Q4 项目复盘与规划",
                            color = ink,
                            fontSize = 25.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 33.sp,
                            modifier = Modifier.pointerInput(longPressTitleEnabled, isPreviewMode) {
                                if (!longPressTitleEnabled || isPreviewMode) return@pointerInput
                                awaitEachGesture {
                                    awaitFirstDown(requireUnconsumed = false)
                                    val earlyOutcome = withTimeoutOrNull(2000L) {
                                        waitForUpOrCancellation()
                                        true
                                    }
                                    if (earlyOutcome == null) {
                                        triggerSecretExit()
                                        waitForUpOrCancellation()
                                    }
                                }
                            },
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(docBlue),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("王", color = Color.White, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                text = "王芳 创建 · $modifiedStamp 修改 · 阅读 48",
                                color = muted,
                                fontSize = 12.sp,
                            )
                        }
                    }
                }

                // Item 1: Authentic Feishu Callout Block
                item {
                    Surface(
                        color = calloutBg,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Text("📌", fontSize = 14.sp)
                            Text(
                                text = "本文档用于周五复盘会前对齐，请各模块负责人在周四 18:00 前补充本组内容。",
                                color = ink,
                                fontSize = 13.5.sp,
                                lineHeight = 20.sp,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                // Item 2: Section 1
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("一、整体结论", color = ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "本季度核心目标基本达成：主链路转化率较上季度提升 3.2 个百分点，新版工作台完成全量发布，平均响应时长下降 18%。遗留问题集中在数据看板口径不一致和跨团队排期冲突两方面，下文分别说明。",
                            color = ink,
                            fontSize = 14.sp,
                            lineHeight = 22.sp,
                        )
                        BossBullet("里程碑按期完成 7 / 9，延期 2 项已确认新时间点", ink)
                        BossBullet("线上 P1 故障 0 起，P2 故障 2 起，均在 SLA 内恢复", ink)
                        BossBullet("用户满意度调研 NPS 42，较上季度 +5", ink)
                    }
                }

                // Item 3: Section 2 Table
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("二、关键数据", color = ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, line, RoundedCornerShape(8.dp))
                                .clip(RoundedCornerShape(8.dp)),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(barBg)
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                            ) {
                                Text("指标", color = subInk, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1.3f))
                                Text("目标", color = subInk, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(0.9f))
                                Text("实际", color = subInk, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(0.95f))
                                Text("达成率", color = subInk, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(0.85f))
                            }
                            metrics.forEach { row ->
                                HorizontalDivider(color = line)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1.3f)) {
                                        Text(row.name, color = ink, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                        Text(row.owner, color = muted, fontSize = 10.sp)
                                    }
                                    Text(row.target, color = subInk, fontSize = 12.sp, modifier = Modifier.weight(0.9f))
                                    Text(row.actual, color = ink, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(0.95f))
                                    Box(modifier = Modifier.weight(0.85f)) {
                                        Surface(
                                            color = if (row.ok) okBg else warnBg,
                                            shape = RoundedCornerShape(4.dp),
                                        ) {
                                            Text(
                                                text = row.rate,
                                                color = if (row.ok) okFg else warnFg,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Item 4: Section 3
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("三、问题复盘", color = ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("3.1 数据口径不一致", color = ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "运营看板与财务报表对「活跃」的定义不同，导致月中两次对数返工。已由数据中台牵头统一口径，新定义自下月 1 日起生效，历史数据回刷预计 3 个工作日。",
                            color = ink,
                            fontSize = 14.sp,
                            lineHeight = 22.sp,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text("3.2 跨团队排期冲突", color = ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "设计走查与研发提测时间重叠，部分需求在提测后才发现交互问题。下阶段调整为「评审 → 走查 → 开发」三段式，走查结论作为提测前置条件。",
                            color = ink,
                            fontSize = 14.sp,
                            lineHeight = 22.sp,
                        )
                    }
                }

                // Item 5: Section 4
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text("四、下阶段重点", color = ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        BossNumbered(1, "完成权限体系重构一期，支持按项目粒度授权", ink, docBlue)
                        BossNumbered(2, "上线智能排期助手灰度版本，覆盖 3 个试点团队", ink, docBlue)
                        BossNumbered(3, "核心接口平均时延降到预期水位以内", ink, docBlue)
                        BossNumbered(4, "补齐关键链路的自动化回归用例，覆盖率提升到 75%", ink, docBlue)
                    }
                }

                // Item 6: Section 5
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text("五、风险与依赖", color = ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        BossBullet("权限重构依赖账号中心接口改造，需对方在第 2 周前提供联调环境", ink)
                        BossBullet("灰度期间需客户成功团队配合收集反馈，每周同步一次", ink)
                    }
                }

                // Item 7: Section 6 Interactive Todo checklist
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("六、待确认事项", color = ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        todos.forEachIndexed { idx, item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        todos[idx] = item.copy(done = !item.done)
                                        persistTodos()
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = if (item.done) Icons.Rounded.CheckBox else Icons.Rounded.CheckBoxOutlineBlank,
                                    contentDescription = null,
                                    tint = if (item.done) docBlue else muted,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = item.text,
                                    color = if (item.done) muted else ink,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }

            // Authentic Feishu Mobile Docs Bottom Comment Capsule & Reaction Bar
            HorizontalDivider(color = line, thickness = 0.5.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(bg)
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(bossPalette.tableHeader)
                        .clickable { docToast = "企业全员只读权限 · 暂无新评论" }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text("添加全文评论…", color = muted, fontSize = 12.5.sp)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { docToast = "已为本文档点赞" }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                ) {
                    Icon(Icons.Rounded.ThumbUpOffAlt, contentDescription = "点赞", tint = subInk, modifier = Modifier.size(17.dp))
                    Text("6", color = subInk, fontSize = 12.sp)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { docToast = "共 3 条协作者批注（均已解决）" }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                ) {
                    Icon(Icons.Rounded.ChatBubbleOutline, contentDescription = "评论", tint = subInk, modifier = Modifier.size(16.dp))
                    Text("3", color = subInk, fontSize = 12.sp)
                }
                Icon(
                    imageVector = Icons.Rounded.Menu,
                    contentDescription = "目录",
                    tint = subInk,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { showOutlineDrawer = true },
                )
            }
        }

        // Authentic Feishu Mobile Docs Right-Edge Floating Mini-Outline Ticks
        if (!showOutlineDrawer) {
            val approxSection = (listState.firstVisibleItemIndex - 2).coerceIn(0, outlineItems.lastIndex)
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 5.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable {
                        activeOutline = approxSection
                        showOutlineDrawer = true
                    }
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                outlineItems.indices.forEach { idx ->
                    val on = approxSection == idx
                    Box(
                        modifier = Modifier
                            .width(if (on) 14.dp else 9.dp)
                            .height(2.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (on) docBlue else muted.copy(alpha = 0.36f)),
                    )
                }
            }
        }

        // Right-side Outline Drawer Overlay
        if (showOutlineDrawer) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.28f))
                    .clickable { showOutlineDrawer = false },
            ) {
                Surface(
                    color = barBg,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .width(236.dp)
                        .fillMaxHeight()
                        .clickable(enabled = true) {},
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("文档目录", color = ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        HorizontalDivider(color = line)
                        outlineItems.forEachIndexed { idx, (label, itemIdx) ->
                            val selected = activeOutline == idx
                            Text(
                                text = label,
                                color = if (selected) docBlue else ink,
                                fontSize = 13.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (selected) calloutBg else Color.Transparent)
                                    .clickable {
                                        activeOutline = idx
                                        showOutlineDrawer = false
                                        scope.launch { listState.animateScrollToItem(itemIdx) }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = docToast != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 62.dp),
        ) {
            Surface(
                color = Color(0xFF1F2329).copy(alpha = 0.90f),
                shape = RoundedCornerShape(999.dp),
            ) {
                Text(
                    text = docToast.orEmpty(),
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                )
            }
        }
    }
}

@Composable
private fun BossCollabDot(letter: String, color: Color, borderColor: Color) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(color)
            .border(1.5.dp, borderColor, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(letter, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun BossBullet(text: String, ink: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("•", color = ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(text, color = ink, fontSize = 14.sp, lineHeight = 21.sp, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun BossNumbered(num: Int, text: String, ink: Color, accent: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("$num.", color = accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Text(text, color = ink, fontSize = 14.sp, lineHeight = 21.sp, modifier = Modifier.weight(1f))
    }
}
