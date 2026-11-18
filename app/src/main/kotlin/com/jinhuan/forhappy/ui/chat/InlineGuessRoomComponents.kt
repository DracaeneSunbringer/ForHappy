package com.jinhuan.forhappy.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.HowToVote
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jinhuan.forhappy.core.model.AppDisguiseMode
import com.jinhuan.forhappy.core.model.ChatInputMode
import com.jinhuan.forhappy.core.model.GuessEntryItem
import com.jinhuan.forhappy.core.model.GuessRoomOverviewResponse
import com.jinhuan.forhappy.core.model.GuessRound
import com.jinhuan.forhappy.ui.theme.LocalForHappyPalette
import com.jinhuan.forhappy.ui.theme.LocalPrivacyStealthConfig

/**
 * 36dp compact strip displayed directly beneath chat header whenever an active `欢欢猜词屋` round runs (`02c §4`).
 */
@Composable
fun GuessStatusStrip(
    round: GuessRound,
    nowTickSec: Long,
    onExpand: () -> Unit,
    onOpenFullPanel: () -> Unit,
) {
    val palette = LocalForHappyPalette.current
    val disguiseMode = LocalPrivacyStealthConfig.current.disguiseMode
    val remainingSec = (round.endsAt - nowTickSec).coerceAtLeast(0L)
    val urgent = round.endsAt > 0L && remainingSec < 30L
    val lenPart = round.answerLength?.let { " · ${it}字" }.orEmpty()
    val rewardPart = if (disguiseMode != AppDisguiseMode.NONE) {
        ""
    } else if (round.rewardType == "none") {
        " · 自由局"
    } else {
        " · ${round.totalText}${round.currency ?: "LDC"}"
    }
    val timerPart = if (round.endsAt > 0L) {
        val mm = remainingSec / 60
        val ss = remainingSec % 60
        " · ⏱ ${mm}:${ss.toString().padStart(2, '0')}"
    } else {
        ""
    }
    val stripTitle = when (disguiseMode) {
        AppDisguiseMode.FEISHU -> "📋 待办：共 ${round.count.coerceAtLeast(1)} 项讨论中"
        AppDisguiseMode.LINUX_DO -> "📊 主题投票进行中$lenPart"
        AppDisguiseMode.NONE -> "🎯 猜词屋$lenPart$rewardPart"
    }

    var dragAccum by remember { mutableStateOf(0f) }

    Surface(
        color = palette.surface,
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .pointerInput(round.messageId) {
                detectVerticalDragGestures(
                    onDragStart = { dragAccum = 0f },
                    onVerticalDrag = { _, dragAmount ->
                        dragAccum += dragAmount
                        if (dragAccum > 28f) {
                            onExpand()
                            dragAccum = 0f
                        }
                    },
                )
            }
            .clickable { onExpand() },
    ) {
        Column {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = palette.gold,
                    modifier = Modifier.size(15.dp),
                )
                Text(
                    text = stripTitle,
                    color = palette.text,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (timerPart.isNotEmpty()) {
                    Text(
                        text = timerPart.removePrefix(" · "),
                        color = if (urgent) palette.danger else palette.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(palette.primaryContainer)
                        .clickable { onExpand() }
                        .padding(horizontal = 7.dp, vertical = 2.dp),
                ) {
                    Text(
                        if (disguiseMode == AppDisguiseMode.FEISHU) "详情" else "展开",
                        color = palette.primary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Icon(Icons.Rounded.ExpandMore, contentDescription = null, tint = palette.primary, modifier = Modifier.size(13.dp))
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = "完整面板",
                    tint = palette.muted,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { onOpenFullPanel() },
                )
            }
            HorizontalDivider(color = palette.line.copy(alpha = 0.7f))
        }
    }
}

/**
 * Co-screen squeezed Guess Room intel board (`160dp..maxHeightDp`) pushing chat messages down
 * cleanly instead of overlaying them (`02c §4`).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GuessIntelBoard(
    round: GuessRound,
    overview: GuessRoomOverviewResponse?,
    nowTickSec: Long,
    boardHeightDp: Dp,
    minHeightDp: Dp = 160.dp,
    maxHeightDp: Dp,
    onHeightChangeDp: (Dp) -> Unit,
    hintBusy: Boolean,
    closeVoteBusy: Boolean,
    onCollapse: () -> Unit,
    onOpenFullPanel: () -> Unit,
    onPrefillGuessWord: (String) -> Unit,
    onRequestAiHint: () -> Unit,
    onVoteCloseRound: (action: String) -> Unit,
) {
    val palette = LocalForHappyPalette.current
    val disguiseMode = LocalPrivacyStealthConfig.current.disguiseMode
    val density = LocalDensity.current
    var activeTab by remember { mutableStateOf("guesses") } // "guesses" | "yes" | "no" | "other"
    var showCloseConfirm by remember { mutableStateOf(false) }

    val remainingSec = (round.endsAt - nowTickSec).coerceAtLeast(0L)
    val urgent = round.endsAt > 0L && remainingSec < 30L
    val highItems = overview?.guesses?.items.orEmpty()
    val yesItems = overview?.facts?.items.orEmpty()
    val noItems = overview?.negative?.items.orEmpty()
    val otherItems = overview?.unknown?.items.orEmpty() + overview?.notQuestion?.items.orEmpty()

    val shownItems: List<GuessEntryItem> = when (activeTab) {
        "guesses" -> highItems
        "yes" -> yesItems
        "no" -> noItems
        else -> otherItems
    }

    Surface(
        color = palette.surface,
        modifier = Modifier
            .fillMaxWidth()
            .height(boardHeightDp.coerceIn(minHeightDp, maxOf(minHeightDp, maxHeightDp))),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Compact Top Summary + Collapse + Full Panel link
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.surface2.copy(alpha = 0.65f))
                    .padding(horizontal = 12.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = palette.gold, modifier = Modifier.size(14.dp))
                Text(
                    text = when (disguiseMode) {
                        AppDisguiseMode.FEISHU -> "📋 待办讨论表"
                        AppDisguiseMode.LINUX_DO -> "📊 主题投票详情"
                        AppDisguiseMode.NONE -> buildString {
                            append("欢欢猜词")
                            round.answerLength?.let { append(" · ${it}字") }
                            if (round.rewardType == "none") append(" · 自由局")
                            else append(" · ${round.totalText}${round.currency ?: "LDC"}")
                        }
                    },
                    color = palette.text,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (round.endsAt > 0L) {
                    Text(
                        text = "${remainingSec / 60}:${(remainingSec % 60).toString().padStart(2, '0')}",
                        color = if (urgent) palette.danger else palette.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    text = "完整面板 ›",
                    color = palette.primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onOpenFullPanel() }
                        .padding(horizontal = 5.dp, vertical = 2.dp),
                )
                Icon(
                    imageVector = Icons.Rounded.ExpandLess,
                    contentDescription = "收起面板",
                    tint = palette.muted,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { onCollapse() },
                )
            }

            // Public hints horizontal strip
            if (round.hints.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    round.hints.forEachIndexed { idx, h ->
                        Surface(
                            color = palette.gold.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text(
                                text = "线索${idx + 1}：$h",
                                color = palette.text,
                                fontSize = 11.sp,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                    }
                }
            }

            // 4 Intel Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                val tabDefs = when (disguiseMode) {
                    AppDisguiseMode.FEISHU -> listOf(
                        "guesses" to "候选 ${highItems.size}",
                        "yes" to "已确认 ${yesItems.size}",
                        "no" to "已排除 ${noItems.size}",
                        "other" to "待定 ${otherItems.size}",
                    )
                    AppDisguiseMode.LINUX_DO -> listOf(
                        "guesses" to "高赞 ${highItems.size}",
                        "yes" to "赞成 ${yesItems.size}",
                        "no" to "反对 ${noItems.size}",
                        "other" to "中立 ${otherItems.size}",
                    )
                    AppDisguiseMode.NONE -> listOf(
                        "guesses" to "高相关 ${highItems.size}",
                        "yes" to "是 ${yesItems.size}",
                        "no" to "否 ${noItems.size}",
                        "other" to "其他 ${otherItems.size}",
                    )
                }
                tabDefs.forEach { (key, label) ->
                    val sel = activeTab == key
                    Surface(
                        color = if (sel) palette.primary else palette.surface2,
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { activeTab = key },
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 4.dp)) {
                            Text(
                                text = label,
                                color = if (sel) palette.onPrimary else palette.text,
                                fontSize = 10.sp,
                                fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }

            // Squeezed intel chips/list
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (overview == null) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = palette.primary)
                        }
                    }
                } else if (shownItems.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                            Text("这一栏暂无情报", color = palette.muted, fontSize = 11.sp)
                        }
                    }
                } else {
                    item {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            shownItems.take(24).forEach { item ->
                                val pct = item.score?.let { "${(it * 100.0).toInt()}%" }.orEmpty()
                                val label = if (item.hiddenAnswer) "（已隐去）" else item.question
                                Surface(
                                    color = palette.surface2,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .border(1.dp, palette.line.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                                        .clickable(enabled = !item.hiddenAnswer && item.question.isNotBlank()) {
                                            onPrefillGuessWord(item.question)
                                        },
                                ) {
                                    Text(
                                        text = if (pct.isNotEmpty() && activeTab == "guesses") "$label $pct" else label,
                                        color = palette.text,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Action Footer + Resize Handle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.surface2.copy(alpha = 0.45f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(
                    color = palette.gold.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier.clickable(enabled = !hintBusy && round.canPlay) {
                        onRequestAiHint()
                    },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Icon(Icons.Rounded.Lightbulb, contentDescription = null, tint = palette.gold, modifier = Modifier.size(13.dp))
                        Text(
                            if (hintBusy) "请求中…" else if (disguiseMode != AppDisguiseMode.NONE) "请求提示" else "💡 要线索",
                            color = palette.gold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                val closeVote = round.closeVote
                val voteText = when {
                    round.owner -> "结束本轮"
                    closeVote?.active == true -> "终止票 ${closeVote.votes}/${closeVote.threshold}"
                    else -> if (disguiseMode != AppDisguiseMode.NONE) "终止讨论" else "流局/表决"
                }
                Surface(
                    color = palette.surface2,
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier
                        .border(1.dp, palette.line, RoundedCornerShape(999.dp))
                        .clickable(enabled = !closeVoteBusy) { showCloseConfirm = true },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Icon(Icons.Rounded.HowToVote, contentDescription = null, tint = palette.muted, modifier = Modifier.size(13.dp))
                        Text("🗳 $voteText", color = palette.text, fontSize = 11.sp)
                    }
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = "拖动底边调高度",
                    color = palette.muted.copy(alpha = 0.7f),
                    fontSize = 9.sp,
                )
            }

            // Interactive bottom resize handle
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .pointerInput(boardHeightDp, maxHeightDp) {
                        detectVerticalDragGestures { _, dragAmount ->
                            val deltaDp = with(density) { dragAmount.toDp() }
                            val next = (boardHeightDp + deltaDp).coerceIn(minHeightDp, maxOf(minHeightDp, maxHeightDp))
                            onHeightChangeDp(next)
                        }
                    },
            ) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(palette.muted.copy(alpha = 0.45f)),
                )
            }
            HorizontalDivider(color = palette.line)
        }
    }

    if (showCloseConfirm) {
        val cv = round.closeVote
        val actionKey = when {
            round.owner -> "force"
            cv?.active == true && !cv.votedByMe -> "vote"
            else -> "request"
        }
        val promptTxt = when (actionKey) {
            "force" -> "你是出题者，确定提前结束 / 流局本轮猜词吗？"
            "vote" -> "当前已有 ${cv?.votes ?: 0}/${cv?.threshold ?: 0} 人支持流局，确定投赞成票吗？"
            else -> "确定发起本轮猜词流局表决吗？"
        }
        AlertDialog(
            onDismissRequest = { showCloseConfirm = false },
            title = { Text("流局 / 结束猜词") },
            text = { Text(promptTxt) },
            confirmButton = {
                TextButton(onClick = {
                    showCloseConfirm = false
                    onVoteCloseRound(actionKey)
                }) { Text("确认", color = palette.danger) }
            },
            dismissButton = {
                TextButton(onClick = { showCloseConfirm = false }) { Text("取消") }
            },
        )
    }
}

/**
 * `[聊天 | 猜 | 问]` input mode bar + 2s AI receipt banner shown above the group chat composer
 * whenever an active Guess Room round exists (`02c §4`).
 */
@Composable
fun GuessInputModeBar(
    activeMode: ChatInputMode,
    canPlay: Boolean,
    receiptBanner: String?,
    onSelectMode: (ChatInputMode) -> Unit,
) {
    val palette = LocalForHappyPalette.current
    val disguiseMode = LocalPrivacyStealthConfig.current.disguiseMode
    Column(modifier = Modifier.fillMaxWidth().background(palette.surface)) {
        AnimatedVisibility(visible = !receiptBanner.isNullOrBlank()) {
            Surface(color = palette.primary.copy(alpha = 0.12f)) {
                Text(
                    text = receiptBanner.orEmpty(),
                    color = palette.primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            ChatInputMode.entries.forEach { mode ->
                val enabled = mode == ChatInputMode.CHAT || canPlay
                val selected = activeMode == mode
                Surface(
                    color = when {
                        !enabled -> palette.surface2.copy(alpha = 0.4f)
                        selected -> palette.primary
                        else -> palette.surface2
                    },
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier
                        .border(
                            1.dp,
                            if (selected) palette.primary else palette.line,
                            RoundedCornerShape(999.dp),
                        )
                        .clickable(enabled = enabled) { onSelectMode(mode) },
                ) {
                    Text(
                        text = if (disguiseMode != AppDisguiseMode.NONE) {
                            when (mode) {
                                ChatInputMode.CHAT -> "消息"
                                ChatInputMode.GUESS -> "提交"
                                ChatInputMode.ASK -> "提问"
                            }
                        } else {
                            when (mode) {
                                ChatInputMode.CHAT -> "💬 聊天"
                                ChatInputMode.GUESS -> "🎯 猜"
                                ChatInputMode.ASK -> "❓ 问"
                            }
                        },
                        color = when {
                            !enabled -> palette.textDisabled
                            selected -> palette.onPrimary
                            else -> palette.text
                        },
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = if (disguiseMode != AppDisguiseMode.NONE) {
                    when (activeMode) {
                        ChatInputMode.CHAT -> "滑动输入框切换模式"
                        ChatInputMode.GUESS -> "按待办提交"
                        ChatInputMode.ASK -> "提问不进入群聊"
                    }
                } else {
                    when (activeMode) {
                        ChatInputMode.CHAT -> "空输入框左右滑可切 猜/问"
                        ChatInputMode.GUESS -> "直接提交给 AI 判词 · 不刷群聊"
                        ChatInputMode.ASK -> "向欢欢问是非 · 不刷群聊"
                    }
                },
                color = palette.muted,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        HorizontalDivider(color = palette.line.copy(alpha = 0.55f))
    }
}
