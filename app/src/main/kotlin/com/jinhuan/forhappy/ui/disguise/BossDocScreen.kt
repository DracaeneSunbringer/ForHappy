package com.jinhuan.forhappy.ui.disguise

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
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

/**
 * Full-screen Feishu Cloud Document boss-key overlay matching `52dmeme.com/app.js` `bossDoc()`.
 * Contains zero giveaway text such as "恢复正常视图"; exits only via top-left `‹` or system back.
 */
@Composable
fun BossDocScreen(
    onExit: () -> Unit,
) {
    BackHandler { onExit() }

    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var activeOutline by remember { mutableIntStateOf(0) }

    val docBlue = Color(0xFF3370FF)
    val bg = Color(0xFFFFFFFF)
    val barBg = Color(0xFFFAFAFB)
    val ink = Color(0xFF1F2329)
    val subInk = Color(0xFF646A73)
    val muted = Color(0xFF8F959E)
    val line = Color(0xFFDEE0E3)
    val calloutBg = Color(0xFFEFF4FF)
    val okBg = Color(0xFFE8F7EE)
    val okFg = Color(0xFF14866D)
    val warnBg = Color(0xFFFFF3E0)
    val warnFg = Color(0xFFD97706)

    val modifiedStamp = remember {
        val cal = Calendar.getInstance()
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val hour = maxOf(9, cal.get(Calendar.HOUR_OF_DAY) - 1)
        val min = (cal.get(Calendar.MINUTE) * 7) % 60
        String.format(Locale.US, "%d月%d日 %02d:%02d", month, day, hour, min)
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
            BossTodoItem("Q4 预算拆分方案（财务 BP）", false),
            BossTodoItem("试点团队名单（运营协同）", false),
            BossTodoItem("复盘会议室与时间（已订 周五 14:00）", true),
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .navigationBarsPadding(),
    ) {
        // Feishu Cloud Doc top breadcrumb bar
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
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onExit,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                        contentDescription = "返回",
                        tint = ink,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(docBlue),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Description,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(15.dp),
                    )
                }
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "知识库 / 项目管理办公室 / 2026 Q4 项目复盘与规划",
                        color = ink,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "已自动保存到云端",
                        color = muted,
                        fontSize = 10.sp,
                        maxLines = 1,
                    )
                }
                Spacer(Modifier.width(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy((-4).dp)) {
                    BossCollabDot("产", docBlue)
                    BossCollabDot("研", Color(0xFF14B8A6))
                    BossCollabDot("设", Color(0xFFFF7D00))
                }
                Spacer(Modifier.width(8.dp))
                Surface(
                    color = docBlue,
                    shape = RoundedCornerShape(6.dp),
                ) {
                    Text(
                        text = "分享",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
                Spacer(Modifier.width(4.dp))
            }

            // Document outline bar
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(barBg)
                    .padding(bottom = 8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                itemsIndexed(outlineItems) { idx, (label, itemIdx) ->
                    val on = activeOutline == idx
                    Surface(
                        color = if (on) calloutBg else Color.White,
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier
                            .border(
                                1.dp,
                                if (on) docBlue.copy(alpha = 0.45f) else line,
                                RoundedCornerShape(999.dp),
                            )
                            .clickable {
                                activeOutline = idx
                                scope.launch { listState.animateScrollToItem(itemIdx) }
                            },
                    ) {
                        Text(
                            text = label,
                            color = if (on) docBlue else subInk,
                            fontSize = 11.sp,
                            fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
            }
            HorizontalDivider(color = line)
        }

        // Document body
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(bg),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Item 0: Title + Metadata
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "2026 Q4 项目复盘与规划",
                        color = ink,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 31.sp,
                    )
                    Text(
                        text = "创建者：项目管理办公室 · 最近修改：$modifiedStamp · 协作者 6 人",
                        color = muted,
                        fontSize = 12.sp,
                    )
                }
            }

            // Item 1: Callout box
            item {
                Surface(
                    color = calloutBg,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, docBlue.copy(alpha = 0.18f), RoundedCornerShape(10.dp)),
                ) {
                    Text(
                        text = "📌 本文档用于周五复盘会前对齐，请各模块负责人在周四 18:00 前补充本组内容。",
                        color = ink,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(12.dp),
                    )
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
                    BossNumbered(3, "核心接口 P99 时延降到 500ms 以内", ink, docBlue)
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
                                .clickable { todos[idx] = item.copy(done = !item.done) }
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
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun BossCollabDot(letter: String, color: Color) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(color)
            .border(1.5.dp, Color.White, CircleShape),
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
