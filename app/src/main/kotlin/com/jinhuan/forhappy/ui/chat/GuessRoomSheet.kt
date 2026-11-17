package com.jinhuan.forhappy.ui.chat

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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Leaderboard
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Queue
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jinhuan.forhappy.core.clock.ServerClock
import com.jinhuan.forhappy.core.model.GuessEntryItem
import com.jinhuan.forhappy.core.model.GuessHistoryItem
import com.jinhuan.forhappy.core.model.GuessRoomOverviewResponse
import com.jinhuan.forhappy.core.model.GuessRound
import com.jinhuan.forhappy.core.model.MeUser
import com.jinhuan.forhappy.core.network.api.ForHappyApiClient
import com.jinhuan.forhappy.ui.components.BadgePill
import com.jinhuan.forhappy.ui.components.ForHappyAvatar
import com.jinhuan.forhappy.ui.theme.LocalForHappyPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Native Compose implementation of `欢欢猜词屋` (`/guess-room.js`):
 * - Overview: active/recent round metadata, deadline countdown, hints + owner hint composer,
 *   similarity ranking board with character match highlighting, Yes/No fact boards, and
 *   `!答案` / `?是非问题` / `@欢欢 线索` composer.
 * - History: past rounds expandable to view answer, explanation & winners.
 * - Stats: today / all-time leaderboard (wins & win-rate).
 * - Queue: pending upcoming rounds.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GuessRoomSheet(
    groupId: Long,
    initialMessageId: Long? = null,
    refreshSignal: Int = 0,
    api: ForHappyApiClient,
    me: MeUser?,
    onRequireLogin: () -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalForHappyPalette.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var subTab by remember { mutableStateOf("live") } // "live" | "history" | "stats" | "queue"
    var boardTab by remember { mutableStateOf("guesses") } // "guesses" | "facts" | "negative" | "extra"
    var selectedMid by remember(initialMessageId) { mutableStateOf(initialMessageId) }
    var receiptId by remember { mutableStateOf<Long?>(null) }

    var overview by remember { mutableStateOf<GuessRoomOverviewResponse?>(null) }
    var loading by remember { mutableStateOf(true) }
    var statusText by remember { mutableStateOf<String?>(null) }

    var mode by remember { mutableStateOf("guess") } // "guess" | "question"
    var inputDraft by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }

    var hintsExpanded by remember { mutableStateOf(true) }
    var hintDraft by remember { mutableStateOf("") }
    var hintBusy by remember { mutableStateOf(false) }

    var historyItems by remember { mutableStateOf<List<GuessHistoryItem>>(emptyList()) }
    var historyLoading by remember { mutableStateOf(false) }
    var expandedHistoryMid by remember { mutableStateOf<Long?>(null) }
    var expandedHistoryRound by remember { mutableStateOf<GuessRound?>(null) }

    var statsPeriod by remember { mutableStateOf("today") } // "today" | "all"
    var statsMetric by remember { mutableStateOf("wins") }  // "wins" | "rate"
    var statsJson by remember { mutableStateOf<JsonObject?>(null) }
    var statsLoading by remember { mutableStateOf(false) }

    var queueJson by remember { mutableStateOf<JsonObject?>(null) }
    var queueLoading by remember { mutableStateOf(false) }

    var nowTickSec by remember { mutableLongStateOf(ServerClock.serverNowSec()) }

    LaunchedEffect(Unit) {
        while (isActive) {
            nowTickSec = ServerClock.serverNowSec()
            delay(1_000L)
        }
    }

    fun loadOverview(showSpinner: Boolean = true) {
        scope.launch {
            if (showSpinner) loading = true
            val res = api.fetchGuessOverview(
                groupId = groupId,
                messageId = selectedMid,
                extra = boardTab == "extra",
                receiptId = receiptId,
            )
            overview = res
            loading = false
            res?.receipt?.reply?.text?.takeIf { it.isNotBlank() }?.let {
                statusText = "欢欢：$it"
            }
        }
    }

    fun loadHistory() {
        scope.launch {
            historyLoading = true
            val res = api.fetchGuessHistory(groupId)
            historyItems = res.items
            historyLoading = false
        }
    }

    fun loadStats() {
        scope.launch {
            statsLoading = true
            statsJson = api.fetchGuessStats(groupId, statsPeriod)
            statsLoading = false
        }
    }

    fun loadQueue() {
        scope.launch {
            queueLoading = true
            queueJson = api.fetchGuessQueue(groupId)
            queueLoading = false
        }
    }

    LaunchedEffect(groupId, selectedMid, boardTab, refreshSignal) {
        when (subTab) {
            "live" -> loadOverview(showSpinner = overview == null)
            "history" -> loadHistory()
            "stats" -> loadStats()
            "queue" -> loadQueue()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.bg,
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .navigationBarsPadding(),
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.surface)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = palette.gold, modifier = Modifier.size(20.dp))
                    Text("欢欢猜词屋", color = palette.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    if (selectedMid != null && subTab == "live") {
                        BadgePill(
                            text = "切回最新轮次",
                            containerColor = palette.surface2,
                            contentColor = palette.primary,
                            modifier = Modifier.clickable {
                                selectedMid = null
                                receiptId = null
                                loadOverview()
                            },
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            when (subTab) {
                                "live" -> loadOverview()
                                "history" -> loadHistory()
                                "stats" -> loadStats()
                                "queue" -> loadQueue()
                            }
                        },
                        modifier = Modifier.size(34.dp),
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "刷新", tint = palette.primary)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = "关闭", tint = palette.text)
                    }
                }
            }

            // Navigation tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.surface)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                listOf(
                    Triple("live", "本轮猜词", Icons.Rounded.AutoAwesome),
                    Triple("history", "历史", Icons.Rounded.History),
                    Triple("stats", "战绩榜", Icons.Rounded.Leaderboard),
                    Triple("queue", "队列", Icons.Rounded.Queue),
                ).forEach { (id, label, icon) ->
                    val sel = subTab == id
                    Surface(
                        color = if (sel) palette.primary else palette.surface2,
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                subTab = id
                                when (id) {
                                    "live" -> loadOverview(showSpinner = overview == null)
                                    "history" -> loadHistory()
                                    "stats" -> loadStats()
                                    "queue" -> loadQueue()
                                }
                            },
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                icon,
                                contentDescription = null,
                                tint = if (sel) palette.onPrimary else palette.muted,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = label,
                                color = if (sel) palette.onPrimary else palette.text,
                                fontSize = 12.sp,
                                fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = palette.line)

            when (subTab) {
                "live" -> {
                    val round = overview?.round
                    if (loading && overview == null) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = palette.primary)
                        }
                    } else if (round == null) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(24.dp),
                            ) {
                                Text("当前群暂无进行中的猜词局", color = palette.text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                Text("可在群内发包发起猜词，或切换到「历史 / 战绩榜」查看往期谜底", color = palette.muted, fontSize = 12.sp)
                                OutlinedButton(onClick = {
                                    subTab = "history"
                                    loadHistory()
                                }) {
                                    Text("查看历史轮次")
                                }
                            }
                        }
                    } else {
                        val isOpen = round.state == "open"
                        val remainingSec = (round.endsAt - nowTickSec).coerceAtLeast(0L)
                        val stateLabel = when {
                            isOpen -> "进行中"
                            round.closeKind != null -> "已流局"
                            round.recipients.isNotEmpty() -> "已猜中"
                            else -> "已结束"
                        }
                        val rewardLabel = if (round.rewardType == "none") {
                            "无奖励 · 自由猜词"
                        } else {
                            "${round.totalText} ${round.currency ?: "LDC"}"
                        }

                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentPadding = PaddingValues(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            // Round summary card
                            item {
                                Surface(
                                    color = palette.surface,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, palette.line, RoundedCornerShape(14.dp)),
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                                BadgePill(
                                                    text = stateLabel,
                                                    containerColor = if (isOpen) palette.primary else palette.surface2,
                                                    contentColor = if (isOpen) palette.onPrimary else palette.muted,
                                                )
                                                Text(rewardLabel, color = palette.gold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                                round.answerLength?.let { len ->
                                                    BadgePill(text = "$len 字")
                                                }
                                            }
                                            if (isOpen && round.endsAt > 0L) {
                                                val mm = remainingSec / 60
                                                val ss = remainingSec % 60
                                                Text(
                                                    text = "剩 ${mm}m${ss.toString().padStart(2, '0')}s",
                                                    color = palette.primary,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                )
                                            }
                                        }

                                        Text(
                                            text = "${round.ownerName} 发起 · 猜测 ${round.guessCount} · 提问 ${round.questionCount} · AI ${round.aiCount}/${round.aiLimit}" +
                                                if (round.count > 1) " · 名额 ${round.claimedCount}/${round.count}" else "",
                                            color = palette.muted,
                                            fontSize = 11.sp,
                                        )

                                        if (!round.answer.isNullOrBlank()) {
                                            Surface(
                                                color = palette.primary.copy(alpha = 0.12f),
                                                shape = RoundedCornerShape(10.dp),
                                            ) {
                                                Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                                                    Text("正确答案：${round.answer}", color = palette.primary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                                    round.explanation?.takeIf { it.isNotBlank() }?.let { exp ->
                                                        Spacer(Modifier.height(2.dp))
                                                        Text(exp, color = palette.text, fontSize = 12.sp)
                                                    }
                                                }
                                            }
                                        }

                                        if (round.owner && round.ownerAnswers.isNotEmpty()) {
                                            Text(
                                                text = "我的谜底：${round.ownerAnswers.joinToString(" / ")}",
                                                color = palette.muted,
                                                fontSize = 11.sp,
                                            )
                                        }
                                    }
                                }
                            }

                            // Hints card
                            item {
                                Surface(
                                    color = palette.surface,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, palette.line, RoundedCornerShape(14.dp)),
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { hintsExpanded = !hintsExpanded },
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Icon(Icons.Rounded.Lightbulb, contentDescription = null, tint = palette.gold, modifier = Modifier.size(16.dp))
                                                Text(
                                                    text = "公开线索 (${round.hints.size})",
                                                    color = palette.text,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                )
                                            }
                                            Icon(
                                                imageVector = if (hintsExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                                contentDescription = null,
                                                tint = palette.muted,
                                            )
                                        }

                                        if (hintsExpanded) {
                                            if (round.hints.isEmpty()) {
                                                Text("暂未公开线索，点击底部「要线索」可向欢欢索要", color = palette.muted, fontSize = 12.sp)
                                            } else {
                                                round.hints.forEachIndexed { idx, hText ->
                                                    Text("${idx + 1}. $hText", color = palette.text, fontSize = 13.sp)
                                                }
                                            }

                                            if (round.owner && isOpen) {
                                                Spacer(Modifier.height(4.dp))
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    OutlinedTextField(
                                                        value = hintDraft,
                                                        onValueChange = { hintDraft = it },
                                                        placeholder = { Text("出题者追加一条公开线索…", fontSize = 12.sp) },
                                                        singleLine = true,
                                                        modifier = Modifier.weight(1f),
                                                    )
                                                    Button(
                                                        enabled = hintDraft.isNotBlank() && !hintBusy,
                                                        onClick = {
                                                            val txt = hintDraft.trim()
                                                            hintBusy = true
                                                            scope.launch {
                                                                val res = api.submitGuessHint(round.messageId, txt)
                                                                hintBusy = false
                                                                res.onSuccess {
                                                                    hintDraft = ""
                                                                    statusText = "已公开新线索"
                                                                    loadOverview(showSpinner = false)
                                                                }.onFailure {
                                                                    statusText = it.message ?: "追加线索失败"
                                                                }
                                                            }
                                                        },
                                                    ) {
                                                        Text("发布", fontSize = 12.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Latest AI answer banner
                            overview?.latest?.let { lt ->
                                if (lt.question.isNotBlank() || lt.text.isNotBlank()) {
                                    item {
                                        Surface(
                                            color = palette.primary.copy(alpha = 0.08f),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            Text(
                                                text = "最新判定：${lt.question} → ${lt.text}",
                                                color = palette.primary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            )
                                        }
                                    }
                                }
                            }

                            // Board selector
                            item {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                                    val gCount = overview?.guesses?.items?.size ?: 0
                                    val fCount = overview?.facts?.items?.size ?: 0
                                    val nCount = overview?.negative?.items?.size ?: 0
                                    listOf(
                                        "guesses" to "高相关 ($gCount)",
                                        "facts" to "确认是 ($fCount)",
                                        "negative" to "确认否 ($nCount)",
                                        "extra" to "其他判断",
                                    ).forEach { (id, lbl) ->
                                        val sel = boardTab == id
                                        Surface(
                                            color = if (sel) palette.primary else palette.surface2,
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { boardTab = id },
                                        ) {
                                            Box(
                                                modifier = Modifier.padding(vertical = 7.dp),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Text(
                                                    text = lbl,
                                                    color = if (sel) palette.onPrimary else palette.text,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal,
                                                    maxLines = 1,
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            val activeItems: List<GuessEntryItem> = when (boardTab) {
                                "guesses" -> overview?.guesses?.items.orEmpty()
                                "facts" -> overview?.facts?.items.orEmpty()
                                "negative" -> overview?.negative?.items.orEmpty()
                                else -> overview?.unknown?.items.orEmpty() + overview?.notQuestion?.items.orEmpty()
                            }

                            if (activeItems.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text("这一栏暂无记录", color = palette.muted, fontSize = 13.sp)
                                    }
                                }
                            } else {
                                items(activeItems, key = { "${boardTab}_${it.id}_${it.question}" }) { entry ->
                                    GuessEntryRow(entry = entry, api = api, showScore = boardTab == "guesses")
                                }
                            }
                        }

                        // Compose bar for Guess / Yes-No Question / Clue
                        Surface(
                            color = palette.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .imePadding(),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, palette.line)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                statusText?.takeIf { it.isNotBlank() }?.let { msg ->
                                    Text(msg, color = palette.primary, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    listOf("guess" to "! 猜答案", "question" to "? 问是非").forEach { (mKey, mLbl) ->
                                        val sel = mode == mKey
                                        Surface(
                                            color = if (sel) palette.primary else palette.surface2,
                                            shape = RoundedCornerShape(999.dp),
                                            modifier = Modifier.clickable(enabled = isOpen) { mode = mKey },
                                        ) {
                                            Text(
                                                text = mLbl,
                                                color = if (sel) palette.onPrimary else palette.text,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            )
                                        }
                                    }
                                    Spacer(Modifier.weight(1f))
                                    Surface(
                                        color = palette.gold.copy(alpha = 0.16f),
                                        shape = RoundedCornerShape(999.dp),
                                        modifier = Modifier.clickable(enabled = isOpen && !sending) {
                                            if (me == null) {
                                                onRequireLogin()
                                                return@clickable
                                            }
                                            sending = true
                                            statusText = "正在向欢欢要线索…"
                                            scope.launch {
                                                val res = api.submitGuessInput(round.messageId, "@欢欢 线索")
                                                sending = false
                                                res.onSuccess { eid ->
                                                    receiptId = eid
                                                    statusText = "已请求线索，等待欢欢回应…"
                                                    loadOverview(showSpinner = false)
                                                }.onFailure {
                                                    statusText = it.message ?: "要线索失败"
                                                }
                                            }
                                        },
                                    ) {
                                        Text(
                                            text = "要线索",
                                            color = palette.gold,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Surface(
                                        color = palette.surface2,
                                        shape = RoundedCornerShape(20.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .border(1.dp, palette.line, RoundedCornerShape(20.dp)),
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 9.dp),
                                        ) {
                                            if (inputDraft.isEmpty()) {
                                                Text(
                                                    text = when {
                                                        me == null -> "登录后参与猜词"
                                                        !isOpen -> "本轮已结束"
                                                        !round.canPlay -> "你已知道谜底，仅可发布线索"
                                                        mode == "guess" -> "直接输入答案（自动加 !）"
                                                        else -> "输入是非问题，如：第一个字是水吗"
                                                    },
                                                    color = palette.muted,
                                                    fontSize = 13.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                            }
                                            BasicTextField(
                                                value = inputDraft,
                                                onValueChange = { inputDraft = it },
                                                enabled = isOpen && round.canPlay,
                                                maxLines = 3,
                                                textStyle = TextStyle(color = palette.text, fontSize = 13.sp),
                                                cursorBrush = SolidColor(palette.primary),
                                                modifier = Modifier.fillMaxWidth(),
                                            )
                                        }
                                    }

                                    Surface(
                                        color = if (isOpen && round.canPlay && inputDraft.isNotBlank() && !sending) palette.primary else palette.surface2,
                                        shape = RoundedCornerShape(18.dp),
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clickable(enabled = isOpen && round.canPlay && inputDraft.isNotBlank() && !sending) {
                                                if (me == null) {
                                                    onRequireLogin()
                                                    return@clickable
                                                }
                                                val raw = inputDraft.trim()
                                                val normalized = if (raw.startsWith("!") || raw.startsWith("！") ||
                                                    raw.startsWith("?") || raw.startsWith("？") ||
                                                    raw.startsWith("@欢欢") || raw.startsWith("＠欢欢")
                                                ) {
                                                    raw
                                                } else {
                                                    (if (mode == "guess") "!" else "?") + raw
                                                }
                                                sending = true
                                                statusText = "正在提交…"
                                                scope.launch {
                                                    val res = api.submitGuessInput(round.messageId, normalized)
                                                    sending = false
                                                    res.onSuccess { eid ->
                                                        inputDraft = ""
                                                        receiptId = eid
                                                        statusText = "已提交，等待欢欢判定…"
                                                        loadOverview(showSpinner = false)
                                                    }.onFailure {
                                                        statusText = it.message ?: "提交失败"
                                                    }
                                                }
                                            },
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            if (sending) {
                                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = palette.primary)
                                            } else {
                                                Icon(
                                                    Icons.AutoMirrored.Rounded.Send,
                                                    contentDescription = "发送",
                                                    tint = if (isOpen && round.canPlay && inputDraft.isNotBlank()) palette.onPrimary else palette.muted,
                                                    modifier = Modifier.size(18.dp),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                "history" -> {
                    if (historyLoading && historyItems.isEmpty()) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = palette.primary)
                        }
                    } else if (historyItems.isEmpty()) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("暂无已结束的历史轮次", color = palette.muted, fontSize = 14.sp)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            contentPadding = PaddingValues(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(historyItems, key = { it.messageId }) { item ->
                                val stamp = remember(item.createdAt) {
                                    runCatching {
                                        SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(Date(item.createdAt * 1000L))
                                    }.getOrDefault("")
                                }
                                val isExpanded = expandedHistoryMid == item.messageId
                                Surface(
                                    color = palette.surface,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, palette.line, RoundedCornerShape(12.dp))
                                        .clickable {
                                            if (isExpanded) {
                                                expandedHistoryMid = null
                                                expandedHistoryRound = null
                                            } else {
                                                expandedHistoryMid = item.messageId
                                                expandedHistoryRound = null
                                                scope.launch {
                                                    expandedHistoryRound = api.fetchGuessRoundResult(groupId, item.messageId)
                                                }
                                            }
                                        },
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text("${item.ownerName} 的猜词", color = palette.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                            Text(
                                                text = stamp + " · " + if (item.rewardType == "none") "无奖励" else "${item.totalText} ${item.currency ?: "LDC"}",
                                                color = palette.muted,
                                                fontSize = 11.sp,
                                            )
                                        }
                                        if (isExpanded) {
                                            HorizontalDivider(color = palette.line, modifier = Modifier.padding(vertical = 4.dp))
                                            val r = expandedHistoryRound
                                            if (r == null) {
                                                Text("正在读取答案与获奖记录…", color = palette.muted, fontSize = 12.sp)
                                            } else {
                                                Text("正确答案：${r.answer ?: "尚未公布"}", color = palette.primary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                                r.explanation?.takeIf { it.isNotBlank() }?.let {
                                                    Text(it, color = palette.text, fontSize = 12.sp)
                                                }
                                                Text("猜测 ${r.guessCount} 次 · 提问 ${r.questionCount} 次", color = palette.muted, fontSize = 11.sp)
                                                r.recipients.forEach { rec ->
                                                    val desc = if (r.rewardType == "none") {
                                                        "${rec.name ?: "群成员"} 猜中"
                                                    } else {
                                                        "${rec.name ?: "群成员"} · ${rec.amountText} ${r.currency ?: "LDC"}"
                                                    }
                                                    Text("🏆 $desc", color = palette.gold, fontSize = 12.sp)
                                                }
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.End,
                                                ) {
                                                    TextButton(onClick = {
                                                        selectedMid = item.messageId
                                                        subTab = "live"
                                                        loadOverview()
                                                    }) {
                                                        Text("查看本轮完整榜单", fontSize = 12.sp)
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

                "stats" -> {
                    Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            listOf("today" to "今日战绩", "all" to "开站总榜").forEach { (p, lbl) ->
                                val sel = statsPeriod == p
                                Surface(
                                    color = if (sel) palette.primary else palette.surface2,
                                    shape = RoundedCornerShape(999.dp),
                                    modifier = Modifier.clickable {
                                        statsPeriod = p
                                        loadStats()
                                    },
                                ) {
                                    Text(
                                        text = lbl,
                                        color = if (sel) palette.onPrimary else palette.text,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                    )
                                }
                            }
                            Spacer(Modifier.weight(1f))
                            listOf("wins" to "猜中榜", "rate" to "胜率榜").forEach { (m, lbl) ->
                                val sel = statsMetric == m
                                Surface(
                                    color = if (sel) palette.gold.copy(alpha = 0.2f) else palette.surface2,
                                    shape = RoundedCornerShape(999.dp),
                                    modifier = Modifier.clickable { statsMetric = m },
                                ) {
                                    Text(
                                        text = lbl,
                                        color = if (sel) palette.gold else palette.muted,
                                        fontSize = 12.sp,
                                        fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                    )
                                }
                            }
                        }

                        if (statsLoading && statsJson == null) {
                            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = palette.primary)
                            }
                        } else {
                            val obj = statsJson
                            val totalsObj = runCatching { obj?.get("totals")?.jsonObject?.get(statsPeriod)?.jsonObject }.getOrNull()
                            val rounds = totalsObj?.get("rounds")?.jsonPrimitive?.intOrNull ?: 0
                            val wins = totalsObj?.get("wins")?.jsonPrimitive?.intOrNull ?: 0
                            val mineWins = obj?.get("mine")?.jsonPrimitive?.intOrNull ?: 0
                            val mineRank = obj?.get("mineRank")?.jsonPrimitive?.intOrNull
                            val leadersArr = runCatching {
                                if (statsMetric == "rate") {
                                    obj?.get("winrate")?.jsonObject?.get("leaders")?.jsonArray
                                } else {
                                    obj?.get("leaders")?.jsonArray
                                }
                            }.getOrNull()

                            LazyColumn(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                contentPadding = PaddingValues(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                item {
                                    Surface(
                                        color = palette.surface,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, palette.line, RoundedCornerShape(12.dp)),
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(
                                                text = "共开局 $rounds 轮 · 猜中 $wins 次",
                                                color = palette.text,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                            )
                                            Text(
                                                text = "我猜中 $mineWins 次" + if (mineRank != null && mineRank > 0) " · 排名第 $mineRank" else "",
                                                color = palette.primary,
                                                fontSize = 12.sp,
                                            )
                                        }
                                    }
                                }

                                if (leadersArr.isNullOrEmpty()) {
                                    item {
                                        Text("当前时段暂无上榜记录", color = palette.muted, fontSize = 13.sp)
                                    }
                                } else {
                                    items(leadersArr.size) { idx ->
                                        val row = leadersArr[idx].jsonObject
                                        val name = row["name"]?.jsonPrimitive?.contentOrNull
                                            ?: row["username"]?.jsonPrimitive?.contentOrNull
                                            ?: "群成员"
                                        val avatar = row["avatar"]?.jsonPrimitive?.contentOrNull
                                        val valLabel = if (statsMetric == "rate") {
                                            val rate = (row["rate"]?.jsonPrimitive?.doubleOrNull ?: 0.0) * 100.0
                                            val rW = row["wins"]?.jsonPrimitive?.intOrNull ?: 0
                                            val rR = row["rounds"]?.jsonPrimitive?.intOrNull ?: 0
                                            String.format(Locale.US, "%.1f%% (%d/%d)", rate, rW, rR)
                                        } else {
                                            "${row["wins"]?.jsonPrimitive?.intOrNull ?: 0} 次"
                                        }
                                        Surface(
                                            color = palette.surface,
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Text(
                                                    text = "#${idx + 1}",
                                                    color = if (idx < 3) palette.gold else palette.muted,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    modifier = Modifier.width(30.dp),
                                                )
                                                ForHappyAvatar(
                                                    username = name,
                                                    avatarUrl = avatar,
                                                    imageLoader = api.imageLoader,
                                                    size = 28.dp,
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Text(name, color = palette.text, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                                Text(valLabel, color = palette.primary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                else -> {
                    val queueArr = runCatching { queueJson?.get("queue")?.jsonArray }.getOrNull()
                    if (queueLoading && queueJson == null) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = palette.primary)
                        }
                    } else if (queueArr.isNullOrEmpty()) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("当前暂无等候开局的猜词队列", color = palette.muted, fontSize = 14.sp)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            contentPadding = PaddingValues(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(queueArr.size) { idx ->
                                val item = queueArr[idx].jsonObject
                                val owner = item["owner_name"]?.jsonPrimitive?.contentOrNull ?: "群友"
                                val rewardType = item["reward_type"]?.jsonPrimitive?.contentOrNull ?: "none"
                                val cents = item["amount_cents"]?.jsonPrimitive?.longOrNull ?: 0L
                                val pay = item["payment_provider"]?.jsonPrimitive?.contentOrNull
                                val stateStr = if (item["state"]?.jsonPrimitive?.contentOrNull == "starting") "正在开局" else "等候中"
                                val rewardStr = when {
                                    rewardType == "none" -> "无奖励"
                                    pay == "coin52" -> "${cents / 100} D币"
                                    else -> String.format(Locale.US, "%.2f LDC", cents / 100.0)
                                }
                                Surface(
                                    color = palette.surface,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth().border(1.dp, palette.line, RoundedCornerShape(10.dp)),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text("${idx + 1}. $owner · $rewardStr", color = palette.text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                        BadgePill(text = stateStr)
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GuessEntryRow(
    entry: GuessEntryItem,
    api: ForHappyApiClient,
    showScore: Boolean,
) {
    val palette = LocalForHappyPalette.current
    val displayUser = entry.users.firstOrNull() ?: entry.user
    val who = displayUser?.displayName ?: "欢友"

    Surface(
        color = palette.surface,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, palette.line.copy(alpha = 0.7f), RoundedCornerShape(12.dp)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    ForHappyAvatar(
                        username = who,
                        avatarUrl = displayUser?.avatar,
                        imageLoader = api.imageLoader,
                        size = 24.dp,
                    )
                    if (entry.characterMatches.isNotEmpty() && !entry.hiddenAnswer) {
                        val chars = entry.question.toList()
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            chars.forEachIndexed { idx, ch ->
                                val matchState = entry.characterMatches.getOrNull(idx)
                                val bg = when (matchState) {
                                    "correct" -> palette.primary
                                    "present" -> palette.gold.copy(alpha = 0.25f)
                                    else -> palette.surface2
                                }
                                val fg = when (matchState) {
                                    "correct" -> palette.onPrimary
                                    "present" -> palette.gold
                                    else -> palette.text
                                }
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(bg),
                                ) {
                                    Text(
                                        text = ch.toString(),
                                        color = fg,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = if (entry.hiddenAnswer) "（已隐去答案）" else entry.question,
                            color = palette.text,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }

                if (showScore && entry.score != null) {
                    val pct = (entry.score * 100.0).coerceIn(0.0, 100.0).toInt()
                    Text(
                        text = "$pct%",
                        color = if (pct >= 80) palette.primary else palette.gold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            if (showScore && entry.score != null) {
                LinearProgressIndicator(
                    progress = { entry.score.toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(999.dp)),
                    color = palette.primary,
                    trackColor = palette.surface2,
                )
            }

            Text(
                text = who + if (entry.participantCount > 1) " 等 ${entry.participantCount} 人" else "",
                color = palette.muted,
                fontSize = 10.sp,
            )
        }
    }
}
