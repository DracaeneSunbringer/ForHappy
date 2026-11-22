package com.jinhuan.forhappy.ui.hub

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jinhuan.forhappy.core.chat.ChatSessionController
import com.jinhuan.forhappy.core.clock.ServerClock
import com.jinhuan.forhappy.core.model.ChatGroup
import com.jinhuan.forhappy.core.model.ForHappyJson
import com.jinhuan.forhappy.core.model.MeUser
import com.jinhuan.forhappy.core.model.MoyuConfig
import com.jinhuan.forhappy.core.model.MoyuCustomHoliday
import com.jinhuan.forhappy.core.model.RoadCommentItem
import com.jinhuan.forhappy.core.model.RoadItem
import com.jinhuan.forhappy.core.model.TodayStats
import com.jinhuan.forhappy.core.moyu.MoyuCalculator
import com.jinhuan.forhappy.core.network.api.ForHappyApiClient
import com.jinhuan.forhappy.ui.theme.LocalForHappyPalette
import com.jinhuan.forhappy.ui.wall.ConfigureFullscreenDialogWindow
import com.jinhuan.forhappy.ui.wall.DialogBackOwnerScope
import com.jinhuan.forhappy.ui.wall.FullscreenDialogProperties
import com.jinhuan.forhappy.ui.wall.saveImageBytesToSystemGallery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

const val KV_MOYU_CONFIG = "moyu.config.v2"
const val KV_MOYU_FG_DAY = "moyu.foreground.day"
const val KV_MOYU_FG_SEC = "moyu.foreground.sec"
const val KV_HUB_PINNED = "hub.pinned.v2"

suspend fun loadMoyuConfig(api: ForHappyApiClient): MoyuConfig {
    val raw = api.getPref(KV_MOYU_CONFIG) ?: return MoyuConfig()
    return runCatching { ForHappyJson.decodeFromString(MoyuConfig.serializer(), raw) }
        .getOrDefault(MoyuConfig())
}

suspend fun saveMoyuConfig(api: ForHappyApiClient, cfg: MoyuConfig) {
    val raw = ForHappyJson.encodeToString(MoyuConfig.serializer(), cfg)
    api.putPref(KV_MOYU_CONFIG, raw)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MoyuCalendarDialog(
    api: ForHappyApiClient,
    me: MeUser?,
    initialConfig: MoyuConfig,
    todayStats: TodayStats?,
    foregroundSec: Long,
    chatController: ChatSessionController? = null,
    hideMoneyAmount: Boolean = false,
    onResetForeground: () -> Unit,
    onConfigSaved: (MoyuConfig) -> Unit,
    onRequireLogin: () -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalForHappyPalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var nowEpochSec by remember { mutableLongStateOf(ServerClock.serverNowMs() / 1000L) }
    LaunchedEffect(Unit) {
        while (true) {
            nowEpochSec = ServerClock.serverNowMs() / 1000L
            delay(1000L)
        }
    }

    var startHm by remember(initialConfig) { mutableStateOf(initialConfig.startHm) }
    var offHm by remember(initialConfig) { mutableStateOf(initialConfig.offHm) }
    var payDayText by remember(initialConfig) { mutableStateOf(initialConfig.payDay.toString()) }
    var salaryText by remember(initialConfig) {
        mutableStateOf(if (initialConfig.monthlySalary > 0.0) initialConfig.monthlySalary.toInt().toString() else "")
    }
    var workDaysText by remember(initialConfig) { mutableStateOf(initialConfig.workDaysPerMonth.toString()) }
    var showInWallHeader by remember(initialConfig) { mutableStateOf(initialConfig.showInWallHeader) }
    var showPetOnCard by remember(initialConfig) { mutableStateOf(initialConfig.showPetOnCard) }
    var holidays by remember(initialConfig) { mutableStateOf(initialConfig.holidays) }

    var newHolidayName by remember { mutableStateOf("") }
    var newHolidayDate by remember { mutableStateOf("") }
    var reportStyle by remember { mutableStateOf("A") }
    var bannerText by remember { mutableStateOf<String?>(null) }
    var busyAction by remember { mutableStateOf(false) }

    var showGroupPicker by remember { mutableStateOf(false) }
    var candidateGroups by remember { mutableStateOf<List<ChatGroup>>(emptyList()) }

    val draftConfig = remember(
        startHm,
        offHm,
        payDayText,
        salaryText,
        workDaysText,
        showInWallHeader,
        showPetOnCard,
        holidays,
    ) {
        MoyuConfig(
            startHm = startHm.ifBlank { "09:00" },
            offHm = offHm.ifBlank { "18:00" },
            payDay = (payDayText.toIntOrNull() ?: 10).coerceIn(1, 31),
            monthlySalary = salaryText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0,
            workDaysPerMonth = (workDaysText.toDoubleOrNull() ?: 21.75).coerceIn(1.0, 31.0),
            showInWallHeader = showInWallHeader,
            showPetOnCard = showPetOnCard,
            dailyHours = initialConfig.dailyHours,
            holidays = holidays.take(20),
        )
    }

    val snapshot = remember(nowEpochSec, draftConfig) {
        MoyuCalculator.compute(nowEpochSec, draftConfig)
    }
    val reportLines = remember(reportStyle, snapshot, todayStats, foregroundSec, me) {
        MoyuCalculator.formatReportText(
            style = reportStyle,
            snapshot = snapshot,
            today = todayStats,
            foregroundSec = foregroundSec,
            username = me?.username?.ifBlank { me.displayName } ?: "欢友",
        )
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
                .imePadding(),
        ) {
            Surface(color = palette.surface) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .height(48.dp)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回", tint = palette.text)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("摸鱼日历", color = palette.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Text(snapshot.dayLabel, color = palette.muted, fontSize = 11.sp)
                    }
                }
            }
            HorizontalDivider(
                color = if (palette.isModernUi) palette.glassBorder else palette.line,
                thickness = if (palette.isModernUi) 0.5.dp else 1.dp,
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                bannerText?.let { msg ->
                    Surface(
                        color = palette.primary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(msg, color = palette.primary, fontSize = 12.sp, modifier = Modifier.weight(1f))
                            Text(
                                "关闭",
                                color = palette.muted,
                                fontSize = 11.sp,
                                modifier = Modifier.clickable { bannerText = null },
                            )
                        }
                    }
                }

                // 1. Live Countdown Hero
                Surface(
                    color = palette.surface,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (palette.isModernUi && palette.cardBorder == Color.Transparent) Modifier else Modifier.border(1.dp, if (palette.isModernUi) palette.cardBorder else palette.line, RoundedCornerShape(16.dp))),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                snapshot.countdownText,
                                color = palette.text,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            if (draftConfig.monthlySalary > 0.0 && !hideMoneyAmount) {
                                Surface(
                                    color = palette.gold.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(999.dp),
                                ) {
                                    Text(
                                        "今日 ${snapshot.formattedEarnedExact}",
                                        color = palette.gold,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    )
                                }
                            }
                        }
                        LinearProgressIndicator(
                            progress = { snapshot.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(999.dp)),
                            color = palette.primary,
                            trackColor = palette.surface2,
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            MoyuCountdownPill("距周末", "${snapshot.daysToWeekend}天")
                            MoyuCountdownPill("距发薪", "${snapshot.daysToPayday}天")
                            MoyuCountdownPill("距元旦", "${snapshot.daysToNewYear}天")
                            snapshot.holidayCountdowns.forEach { (name, days) ->
                                MoyuCountdownPill(name, "${days}天")
                            }
                        }
                    }
                }

                // 2. Cumulative Foreground Moyu + Today Stats
                Surface(
                    color = palette.surface,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (palette.isModernUi && palette.cardBorder == Color.Transparent) Modifier else Modifier.border(1.dp, if (palette.isModernUi) palette.cardBorder else palette.line, RoundedCornerShape(16.dp))),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("累计前台摸鱼", color = palette.muted, fontSize = 12.sp)
                            Text(
                                MoyuCalculator.formatHms(foregroundSec),
                                color = palette.text,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                "今日看帖 ${todayStats?.seen ?: 0} · 发图 ${todayStats?.posts ?: 0} · 群聊 ${todayStats?.msgs ?: 0}",
                                color = palette.muted,
                                fontSize = 12.sp,
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                onResetForeground()
                                bannerText = "今日摸鱼计时已清零"
                            },
                        ) {
                            Text("清零", fontSize = 12.sp)
                        }
                    }
                }

                // 3. 3-Style Daily Report Generator
                Surface(
                    color = palette.surface,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (palette.isModernUi && palette.cardBorder == Color.Transparent) Modifier else Modifier.border(1.dp, if (palette.isModernUi) palette.cardBorder else palette.line, RoundedCornerShape(16.dp))),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("每日摸鱼日报", color = palette.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            listOf(
                                "A" to "版式A · 温馨卡片",
                                "B" to "版式B · 极简战报",
                                "C" to "版式C · 摸鱼证书",
                            ).forEach { (id, label) ->
                                FilterChip(
                                    selected = reportStyle == id,
                                    onClick = { reportStyle = id },
                                    label = { Text(label, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = palette.primary.copy(alpha = 0.14f),
                                        selectedLabelColor = palette.primary,
                                    ),
                                )
                            }
                        }

                        val (cardBg, titleColor, subColor, accentBorder) = when (reportStyle) {
                            "B" -> listOf(Color(0xFF132219), Color.White, Color(0xFFB8D1C2), Color(0xFF34A853))
                            "C" -> listOf(Color(0xFFFDF5E6), Color(0xFF3D290A), Color(0xFF6B532C), Color(0xFFC58C24))
                            else -> listOf(Color(0xFFF6F4EE), Color(0xFF1B241E), Color(0xFF5A655E), Color(0xFF1B7F4B))
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(cardBg)
                                .border(1.dp, accentBorder.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                                .padding(14.dp),
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                reportLines.forEachIndexed { idx, line ->
                                    Text(
                                        line,
                                        color = if (idx == 0) titleColor else subColor,
                                        fontSize = if (idx == 0) 15.sp else 12.sp,
                                        fontWeight = if (idx == 0) FontWeight.Bold else FontWeight.Normal,
                                    )
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                enabled = !busyAction,
                                onClick = {
                                    busyAction = true
                                    scope.launch {
                                        val bytes = withContext(Dispatchers.Default) {
                                            MoyuCalculator.renderReportPng(
                                                style = reportStyle,
                                                snapshot = snapshot,
                                                today = todayStats,
                                                foregroundSec = foregroundSec,
                                                username = me?.username?.ifBlank { me.displayName } ?: "欢友",
                                            )
                                        }
                                        val res = saveImageBytesToSystemGallery(context, bytes, ext = "png", mime = "image/png")
                                        busyAction = false
                                        bannerText = res.fold(onSuccess = { it }, onFailure = { it.message ?: "保存失败" })
                                    }
                                },
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("保存图片", fontSize = 13.sp)
                            }
                            Button(
                                enabled = !busyAction,
                                onClick = {
                                    if (me == null) {
                                        onRequireLogin()
                                        return@Button
                                    }
                                    scope.launch {
                                        val existing = chatController?.uiState?.value?.groups.orEmpty().filter { it.joined }
                                        candidateGroups = if (existing.isNotEmpty()) {
                                            existing
                                        } else {
                                            api.fetchChatGroups().filter { it.joined }
                                        }
                                        if (candidateGroups.isEmpty()) {
                                            bannerText = "暂无已加入群聊，无法发送"
                                        } else {
                                            showGroupPicker = true
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = palette.primary,
                                    contentColor = palette.onPrimary,
                                ),
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("发到群聊", fontSize = 13.sp)
                            }
                        }
                    }
                }

                // 4. Attendance & Salary Config (moyu.config.v2)
                Surface(
                    color = palette.surface,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (palette.isModernUi && palette.cardBorder == Color.Transparent) Modifier else Modifier.border(1.dp, if (palette.isModernUi) palette.cardBorder else palette.line, RoundedCornerShape(16.dp))),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("出勤与薪资设置", color = palette.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = startHm,
                                onValueChange = { startHm = it },
                                label = { Text("上班 HH:MM") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                            )
                            OutlinedTextField(
                                value = offHm,
                                onValueChange = { offHm = it },
                                label = { Text("下班 HH:MM") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = payDayText,
                                onValueChange = { payDayText = it.filter(Char::isDigit).take(2) },
                                label = { Text("发薪日(1-31)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                            )
                            OutlinedTextField(
                                value = salaryText,
                                onValueChange = { salaryText = it.filter { c -> c.isDigit() || c == '.' } },
                                label = { Text("月薪(元,0隐藏)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                            )
                        }
                        OutlinedTextField(
                            value = workDaysText,
                            onValueChange = { workDaysText = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("月工作天数(默认 21.75)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("在壁纸流顶栏显示今日收益", color = palette.text, fontSize = 13.sp)
                                Text("伪装模式与老板键下会自动隐藏", color = palette.muted, fontSize = 11.sp)
                            }
                            Switch(checked = showInWallHeader, onCheckedChange = { showInWallHeader = it })
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("摸鱼卡片角标显示宠物", color = palette.text, fontSize = 13.sp)
                                Text("点击角标可快速跳转鱼鱼谷", color = palette.muted, fontSize = 11.sp)
                            }
                            Switch(checked = showPetOnCard, onCheckedChange = { showPetOnCard = it })
                        }

                        HorizontalDivider(
                color = if (palette.isModernUi) palette.glassBorder else palette.line,
                thickness = if (palette.isModernUi) 0.5.dp else 1.dp,
            )
                        Text(
                            "自定义假日 (${holidays.size}/20)",
                            color = palette.text,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        holidays.forEachIndexed { index, item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("${item.name} · ${item.dateIso}", color = palette.text, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                IconButton(
                                    onClick = { holidays = holidays.toMutableList().apply { removeAt(index) } },
                                    modifier = Modifier.size(30.dp),
                                ) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "删除", tint = palette.muted, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                        if (holidays.size < 20) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = newHolidayName,
                                    onValueChange = { newHolidayName = it },
                                    label = { Text("假日名") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                )
                                OutlinedTextField(
                                    value = newHolidayDate,
                                    onValueChange = { newHolidayDate = it },
                                    label = { Text("YYYY-MM-DD") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1.2f),
                                )
                                IconButton(
                                    onClick = {
                                        val cleanName = newHolidayName.trim()
                                        val cleanDate = newHolidayDate.trim()
                                        val parsedOk = runCatching { java.time.LocalDate.parse(cleanDate) }.isSuccess
                                        if (cleanName.isNotEmpty() && parsedOk) {
                                            holidays = (holidays + MoyuCustomHoliday(cleanName, cleanDate)).take(20)
                                            newHolidayName = ""
                                            newHolidayDate = ""
                                        } else {
                                            bannerText = "请填写假日名及有效 YYYY-MM-DD 日期"
                                        }
                                    },
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = "添加假日", tint = palette.primary)
                                }
                            }
                        }

                        Button(
                            onClick = {
                                scope.launch {
                                    saveMoyuConfig(api, draftConfig)
                                    onConfigSaved(draftConfig)
                                    bannerText = "摸鱼配置已保存到本地"
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.primary,
                                contentColor = palette.onPrimary,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("保存设置")
                        }
                    }
                }
                Spacer(Modifier.navigationBarsPadding())
            }
        }
        }
    }

    if (showGroupPicker) {
        AlertDialog(
            onDismissRequest = { showGroupPicker = false },
            containerColor = palette.surface,
            titleContentColor = palette.text,
            textContentColor = palette.text,
            title = { Text("发到哪个群聊") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    candidateGroups.forEach { grp ->
                        Surface(
                            color = palette.surface2,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showGroupPicker = false
                                    busyAction = true
                                    scope.launch {
                                        val bytes = withContext(Dispatchers.Default) {
                                            MoyuCalculator.renderReportPng(
                                                style = reportStyle,
                                                snapshot = snapshot,
                                                today = todayStats,
                                                foregroundSec = foregroundSec,
                                                username = me?.username?.ifBlank { me.displayName } ?: "欢友",
                                            )
                                        }
                                        val up = api.uploadMediaBytes(
                                            bytes = bytes,
                                            fileName = "moyu_${snapshot.dayLabel}.png",
                                            mimeType = "image/png",
                                            forWhat = "chat",
                                        )
                                        bannerText = up.fold(
                                            onSuccess = { uploaded ->
                                                val mediaArr = JsonArray(
                                                    listOf(
                                                        buildJsonObject {
                                                            put("key", JsonPrimitive(uploaded.key))
                                                            put("w", JsonPrimitive(900))
                                                            put("h", JsonPrimitive(560))
                                                        },
                                                    ),
                                                )
                                                val sent = api.sendChatMessageWithMedia(
                                                    groupId = grp.id,
                                                    body = "今日摸鱼日报 (${reportStyle}版)",
                                                    media = mediaArr,
                                                )
                                                sent.fold(
                                                    onSuccess = {
                                                        chatController?.refreshGroups()
                                                        "已发送到「${grp.name}」"
                                                    },
                                                    onFailure = { it.message ?: "发送失败" },
                                                )
                                            },
                                            onFailure = { it.message ?: "日报图上传失败" },
                                        )
                                        busyAction = false
                                    }
                                },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(grp.name, color = palette.text, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                Text("${grp.memberCount}人", color = palette.muted, fontSize = 11.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showGroupPicker = false }) {
                    Text("取消")
                }
            },
        )
    }
}

@Composable
private fun MoyuCountdownPill(label: String, value: String) {
    val palette = LocalForHappyPalette.current
    Surface(
        color = palette.surface2,
        shape = RoundedCornerShape(999.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, color = palette.muted, fontSize = 11.sp)
            Text(value, color = palette.text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun NativeRoadDialog(
    api: ForHappyApiClient,
    me: MeUser?,
    onRequireLogin: () -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalForHappyPalette.current
    val scope = rememberCoroutineScope()

    var items by remember { mutableStateOf<List<RoadItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var kindFilter by remember { mutableStateOf("all") }
    var statusFilter by remember { mutableStateOf("all") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var activeCommentItem by remember { mutableStateOf<RoadItem?>(null) }
    var feedbackBanner by remember { mutableStateOf<String?>(null) }

    fun reload() {
        loading = true
        scope.launch {
            items = api.fetchRoadItems("all")
            loading = false
        }
    }

    LaunchedEffect(Unit) { reload() }

    val filteredItems = remember(items, kindFilter, statusFilter) {
        items.filter { item ->
            val kindOk = when (kindFilter) {
                "all" -> true
                "idea" -> item.kind.lowercase() in setOf("idea", "feat", "feature")
                "bug" -> item.kind.lowercase() == "bug"
                "wish" -> item.kind.lowercase() == "wish"
                "improve" -> item.kind.lowercase() in setOf("improve", "tweak", "opt")
                else -> item.kind.lowercase() == kindFilter
            }
            val rawStatus = item.status.orEmpty().lowercase().ifBlank { "open" }
            val statusOk = when (statusFilter) {
                "all" -> true
                "open" -> rawStatus in setOf("open", "new", "pending")
                "planned" -> rawStatus == "planned"
                "doing" -> rawStatus in setOf("doing", "in_progress", "wip")
                "done" -> rawStatus in setOf("done", "shipped", "resolved")
                "wontfix" -> rawStatus in setOf("wontfix", "closed", "rejected")
                else -> rawStatus == statusFilter
            }
            kindOk && statusOk
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = FullscreenDialogProperties,
    ) {
        ConfigureFullscreenDialogWindow(darkStatusIcons = !palette.isDark)
        DialogBackOwnerScope {
            BackHandler(onBack = onDismiss)
        val navBottomPad = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.bg),
        ) {
            Surface(color = palette.surface) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .height(48.dp)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回", tint = palette.text)
                }
                Text(
                    "鞭打路线图 · /road",
                    color = palette.text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { reload() }) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "刷新", tint = palette.muted)
                }
                Surface(
                    color = palette.primary,
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier.clickable {
                        if (me == null) onRequireLogin() else showCreateDialog = true
                    },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, tint = palette.onPrimary, modifier = Modifier.size(15.dp))
                        Text("鞭打", color = palette.onPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                }
            }
            HorizontalDivider(
                color = if (palette.isModernUi) palette.glassBorder else palette.line,
                thickness = if (palette.isModernUi) 0.5.dp else 1.dp,
            )

            // 4 kind filters + 5 status filters
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.surface)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    listOf(
                        "all" to "全部",
                        "idea" to "💡 点子",
                        "bug" to "🐛 Bug",
                        "wish" to "✨ 许愿",
                        "improve" to "🔧 改进",
                    ).forEach { (id, label) ->
                        FilterChip(
                            selected = kindFilter == id,
                            onClick = { kindFilter = id },
                            label = { Text(label, fontSize = 12.sp) },
                        )
                    }
                }
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    listOf(
                        "all" to "状态不限",
                        "open" to "待处理",
                        "planned" to "已规划",
                        "doing" to "进行中",
                        "done" to "已完成",
                        "wontfix" to "暂不考虑",
                    ).forEach { (id, label) ->
                        FilterChip(
                            selected = statusFilter == id,
                            onClick = { statusFilter = id },
                            label = { Text(label, fontSize = 11.sp) },
                        )
                    }
                }
            }
            HorizontalDivider(
                color = if (palette.isModernUi) palette.glassBorder else palette.line,
                thickness = if (palette.isModernUi) 0.5.dp else 1.dp,
            )

            feedbackBanner?.let { msg ->
                Text(
                    msg,
                    color = palette.primary,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.primary.copy(alpha = 0.08f))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }

            if (loading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = palette.primary)
                }
            } else if (filteredItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("当前筛选下暂无条目", color = palette.muted, fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 14.dp,
                        top = 14.dp,
                        end = 14.dp,
                        bottom = 14.dp + navBottomPad,
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(filteredItems, key = { it.id }) { item ->
                        Surface(
                            color = palette.surface,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (palette.isModernUi && palette.cardBorder == Color.Transparent) {
                                        Modifier
                                    } else {
                                        Modifier.border(
                                            1.dp,
                                            if (palette.isModernUi) palette.cardBorder else palette.line,
                                            RoundedCornerShape(14.dp),
                                        )
                                    },
                                ),
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Surface(color = palette.surface2, shape = RoundedCornerShape(6.dp)) {
                                        Text(
                                            roadKindLabel(item.kind),
                                            color = palette.text,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                        )
                                    }
                                    val stLabel = roadStatusLabel(item.status)
                                    Surface(color = palette.primary.copy(alpha = 0.12f), shape = RoundedCornerShape(6.dp)) {
                                        Text(
                                            stLabel,
                                            color = palette.primary,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                        )
                                    }
                                    Spacer(Modifier.weight(1f))
                                    Text(
                                        item.user?.displayName ?: item.authorName ?: "欢友",
                                        color = palette.muted,
                                        fontSize = 11.sp,
                                    )
                                }
                                Text(item.title, color = palette.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                if (item.body.isNotBlank()) {
                                    Text(item.body, color = palette.muted, fontSize = 13.sp, lineHeight = 18.sp)
                                }
                                item.reply?.takeIf { it.isNotBlank() }?.let { replyText ->
                                    Surface(
                                        color = palette.surface2,
                                        shape = RoundedCornerShape(8.dp),
                                    ) {
                                        Text(
                                            "站长回复：$replyText",
                                            color = palette.text,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(10.dp),
                                        )
                                    }
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    val upActive = item.effectiveMyVote == 1
                                    val downActive = item.effectiveMyVote == -1
                                    Surface(
                                        color = if (upActive) palette.primary.copy(alpha = 0.16f) else palette.surface2,
                                        shape = RoundedCornerShape(999.dp),
                                        modifier = Modifier.clickable {
                                            if (me == null) {
                                                onRequireLogin()
                                            } else {
                                                val target = if (upActive) 0 else 1
                                                scope.launch {
                                                    val res = api.voteRoadItem(item.id, target)
                                                    if (res != null) {
                                                        val newScore = res["score"]?.jsonPrimitive?.intOrNull ?: (item.displayScore + (target - item.effectiveMyVote))
                                                        items = items.map {
                                                            if (it.id == item.id) it.copy(score = newScore, my = target, myVote = target) else it
                                                        }
                                                    } else {
                                                        feedbackBanner = "投票失败，请稍后重试"
                                                    }
                                                }
                                            }
                                        },
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Icon(
                                                Icons.Rounded.KeyboardArrowUp,
                                                contentDescription = "赞同",
                                                tint = if (upActive) palette.primary else palette.text,
                                                modifier = Modifier.size(16.dp),
                                            )
                                            Text(
                                                "${item.displayScore}",
                                                color = if (upActive) palette.primary else palette.text,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                            )
                                        }
                                    }
                                    Surface(
                                        color = if (downActive) palette.danger.copy(alpha = 0.16f) else palette.surface2,
                                        shape = RoundedCornerShape(999.dp),
                                        modifier = Modifier.clickable {
                                            if (me == null) {
                                                onRequireLogin()
                                            } else {
                                                val target = if (downActive) 0 else -1
                                                scope.launch {
                                                    val res = api.voteRoadItem(item.id, target)
                                                    if (res != null) {
                                                        val newScore = res["score"]?.jsonPrimitive?.intOrNull ?: (item.displayScore + (target - item.effectiveMyVote))
                                                        items = items.map {
                                                            if (it.id == item.id) it.copy(score = newScore, my = target, myVote = target) else it
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                    ) {
                                        Icon(
                                            Icons.Rounded.KeyboardArrowDown,
                                            contentDescription = "踩",
                                            tint = if (downActive) palette.danger else palette.muted,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp).size(16.dp),
                                        )
                                    }
                                    Spacer(Modifier.weight(1f))
                                    Surface(
                                        color = palette.surface2,
                                        shape = RoundedCornerShape(999.dp),
                                        modifier = Modifier.clickable { activeCommentItem = item },
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        ) {
                                            Icon(Icons.Rounded.ChatBubbleOutline, contentDescription = null, tint = palette.muted, modifier = Modifier.size(14.dp))
                                            Text("${item.comments} 评论", color = palette.muted, fontSize = 12.sp)
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
    }

    if (showCreateDialog) {
        var kind by remember { mutableStateOf("idea") }
        var title by remember { mutableStateOf("") }
        var body by remember { mutableStateOf("") }
        var submitting by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!submitting) showCreateDialog = false },
            containerColor = palette.surface,
            titleContentColor = palette.text,
            textContentColor = palette.text,
            title = { Text("提交鞭打建议") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        listOf("idea" to "点子", "bug" to "Bug", "wish" to "许愿", "improve" to "改进").forEach { (id, label) ->
                            FilterChip(selected = kind == id, onClick = { kind = id }, label = { Text(label, fontSize = 12.sp) })
                        }
                    }
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("简短标题") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = body,
                        onValueChange = { body = it },
                        label = { Text("具体说明 / 复现步骤") },
                        minLines = 3,
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = title.trim().isNotEmpty() && !submitting,
                    onClick = {
                        submitting = true
                        scope.launch {
                            val res = api.createRoadItem(kind, title.trim(), body.trim())
                            submitting = false
                            res.fold(
                                onSuccess = {
                                    showCreateDialog = false
                                    feedbackBanner = "提交成功！"
                                    reload()
                                },
                                onFailure = {
                                    feedbackBanner = it.message ?: "提交失败"
                                },
                            )
                        }
                    },
                ) {
                    Text(if (submitting) "提交中…" else "提交")
                }
            },
            dismissButton = {
                TextButton(enabled = !submitting, onClick = { showCreateDialog = false }) { Text("取消") }
            },
        )
    }

    activeCommentItem?.let { targetItem ->
        RoadCommentsDialog(
            api = api,
            roadItem = targetItem,
            me = me,
            onRequireLogin = onRequireLogin,
            onCommentPosted = {
                items = items.map {
                    if (it.id == targetItem.id) it.copy(comments = it.comments + 1) else it
                }
            },
            onDismiss = { activeCommentItem = null },
        )
    }
}

@Composable
private fun RoadCommentsDialog(
    api: ForHappyApiClient,
    roadItem: RoadItem,
    me: MeUser?,
    onRequireLogin: () -> Unit,
    onCommentPosted: () -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalForHappyPalette.current
    val scope = rememberCoroutineScope()
    var comments by remember(roadItem.id) { mutableStateOf<List<RoadCommentItem>>(emptyList()) }
    var loading by remember(roadItem.id) { mutableStateOf(true) }
    var draft by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }

    fun loadComments() {
        loading = true
        scope.launch {
            comments = api.fetchRoadComments(roadItem.id)
            loading = false
        }
    }

    LaunchedEffect(roadItem.id) { loadComments() }

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
                .imePadding(),
        ) {
            Surface(color = palette.surface) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .height(48.dp)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回", tint = palette.text)
                    }
                    Text(
                        roadItem.title,
                        color = palette.text,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            HorizontalDivider(
                color = if (palette.isModernUi) palette.glassBorder else palette.line,
                thickness = if (palette.isModernUi) 0.5.dp else 1.dp,
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (loading) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = palette.primary)
                        }
                    }
                } else if (comments.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text("暂无评论，抢个沙发吧", color = palette.muted, fontSize = 13.sp)
                        }
                    }
                } else {
                    items(comments, key = { it.id }) { cmt ->
                        Surface(
                            color = palette.surface,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, palette.line, RoundedCornerShape(12.dp)),
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(cmt.displayAuthor, color = palette.text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    if (cmt.createdAt > 0L) {
                                        Text(formatRoadTime(cmt.createdAt), color = palette.muted, fontSize = 11.sp)
                                    }
                                }
                                Text(cmt.body, color = palette.text, fontSize = 13.sp, lineHeight = 18.sp)
                            }
                        }
                    }
                }
            }

            HorizontalDivider(
                color = if (palette.isModernUi) palette.glassBorder else palette.line,
                thickness = if (palette.isModernUi) 0.5.dp else 1.dp,
            )
            Surface(color = palette.surface) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        placeholder = { Text("发表评论…") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        enabled = draft.trim().isNotEmpty() && !sending,
                        onClick = {
                            if (me == null) {
                                onRequireLogin()
                                return@IconButton
                            }
                            sending = true
                            scope.launch {
                                val ok = api.postRoadComment(roadItem.id, draft.trim()).isSuccess
                                sending = false
                                if (ok) {
                                    draft = ""
                                    onCommentPosted()
                                    loadComments()
                                }
                            }
                        },
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "发送", tint = palette.primary)
                    }
                }
            }
        }
        }
    }
}

private data class ArcadeGameOption(
    val route: String,
    val emoji: String,
    val title: String,
    val desc: String,
)

private val ARCADE_GAMES = listOf(
    ArcadeGameOption("/slime", "🧪", "史莱姆工坊", "融合粘液图鉴 · 工坊收益收集"),
    ArcadeGameOption("/arcade/mole", "🔨", "狂揍欢鼠", "限时敲打欢鼠 · 连击冲刺排行榜"),
    ArcadeGameOption("/arcade/rift", "⚡", "裂隙突围", "高压弹幕闪避 · 无尽生存挑战"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArcadeLauncherSheet(
    api: ForHappyApiClient,
    onLaunchRoute: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalForHappyPalette.current
    val scope = rememberCoroutineScope()
    val petUi by api.petCoordinator.uiState.collectAsState()
    var checkingRoute by remember { mutableStateOf<String?>(null) }
    var conflictRoute by remember { mutableStateOf<String?>(null) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.surface,
        contentColor = palette.text,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.SportsEsports, contentDescription = null, tint = palette.primary)
                Text("游乐厅 · 休闲小游戏", color = palette.text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
            if (petUi.arcadeStandbyActive) {
                Surface(
                    color = palette.gold.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        "宠物挂机提示：${petUi.arcadeStandbySummary?.takeIf { it.isNotBlank() } ?: "当前有挂机任务运行中"}",
                        color = palette.text,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(10.dp),
                    )
                }
            }
            ARCADE_GAMES.forEach { game ->
                Surface(
                    color = palette.surface2,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = checkingRoute == null) {
                            checkingRoute = game.route
                            scope.launch {
                                runCatching { api.petCoordinator.fetchWorld() }
                                checkingRoute = null
                                val state = api.petCoordinator.uiState.value
                                if (state.arcadeStandbyActive) {
                                    conflictRoute = game.route
                                } else {
                                    onLaunchRoute(game.route)
                                }
                            }
                        },
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(palette.surface),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(game.emoji, fontSize = 22.sp)
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(game.title, color = palette.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Text(game.desc, color = palette.muted, fontSize = 12.sp)
                        }
                        if (checkingRoute == game.route) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = palette.primary)
                        } else {
                            Text("进入 →", color = palette.primary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }
    }

    conflictRoute?.let { pendingRoute ->
        AlertDialog(
            onDismissRequest = { conflictRoute = null },
            containerColor = palette.surface,
            titleContentColor = palette.text,
            textContentColor = palette.text,
            title = { Text("宠物挂机冲突提醒") },
            text = {
                Text(
                    "检测到当前鱼鱼谷存在挂机进程（${petUi.arcadeStandbySummary?.takeIf { it.isNotBlank() } ?: "挂机收益中"}）。进入小游戏可能会暂停或覆盖挂机，确定继续进入吗？",
                    fontSize = 13.sp,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        conflictRoute = null
                        onLaunchRoute(pendingRoute)
                    },
                ) {
                    Text("继续进入")
                }
            },
            dismissButton = {
                TextButton(onClick = { conflictRoute = null }) { Text("暂不进入") }
            },
        )
    }
}

private fun roadKindLabel(kind: String): String = when (kind.lowercase()) {
    "idea", "feat", "feature" -> "💡 点子"
    "bug" -> "🐛 Bug"
    "wish" -> "✨ 许愿"
    "improve", "tweak", "opt" -> "🔧 改进"
    else -> kind.ifBlank { "💡 建议" }
}

private fun roadStatusLabel(status: String?): String = when (status.orEmpty().lowercase()) {
    "planned" -> "已规划"
    "doing", "in_progress", "wip" -> "进行中"
    "done", "shipped", "resolved" -> "已完成"
    "wontfix", "closed", "rejected" -> "暂不考虑"
    else -> "待处理"
}

private fun formatRoadTime(epochSec: Long): String {
    val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.CHINA)
    return sdf.format(Date(epochSec * 1000L))
}
