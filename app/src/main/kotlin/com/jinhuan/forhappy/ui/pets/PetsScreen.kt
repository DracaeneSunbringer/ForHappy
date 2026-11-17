package com.jinhuan.forhappy.ui.pets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jinhuan.forhappy.core.clock.IsleClockRules
import com.jinhuan.forhappy.core.clock.ServerClock
import com.jinhuan.forhappy.core.model.MeUser
import com.jinhuan.forhappy.core.model.PetGuardBattle
import com.jinhuan.forhappy.core.model.PetNeighborSummary
import com.jinhuan.forhappy.core.network.api.ForHappyApiClient
import com.jinhuan.forhappy.core.network.pet.PetCoexistenceCoordinator
import com.jinhuan.forhappy.ui.components.BadgePill
import com.jinhuan.forhappy.ui.components.SectionCard
import com.jinhuan.forhappy.ui.theme.LocalForHappyPalette
import com.jinhuan.forhappy.ui.web.ShelllessWebScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.text.NumberFormat
import java.util.Locale

/**
 * Pets & 鱼鱼谷 Coexistence Screen:
 * - Calibrates dynamic island clock via `b12ConfigureClock(world.clockStart)` and displays server-synced calendar/events.
 * - Detects & respects `arcade-standby` (`world.state.arc.cur != null`) without opening silent arcade rounds.
 * - Computes mature crop slots via [IsleClockRules.cropStage] and sends real `harvest`/`water` batch actions.
 * - Embeds full HTML5 Canvas island gameplay via [ShelllessWebScreen] tab.
 */
@Composable
fun PetsScreen(
    api: ForHappyApiClient,
    me: MeUser?,
    onRequireLogin: () -> Unit,
) {
    val palette = LocalForHappyPalette.current
    val scope = rememberCoroutineScope()
    val coord = api.petCoordinator
    val ui by coord.uiState.collectAsState()

    var subTab by remember { mutableStateOf("island") } // "island" | "care" | "canvas"
    var serverNowTickSec by remember { mutableLongStateOf(ServerClock.serverNowSec()) }
    var actionFeedback by remember { mutableStateOf<String?>(null) }
    var neighbors by remember { mutableStateOf<List<PetNeighborSummary>>(emptyList()) }
    var guardBattles by remember { mutableStateOf<List<PetGuardBattle>>(emptyList()) }

    LaunchedEffect(Unit) {
        while (isActive) {
            serverNowTickSec = ServerClock.serverNowSec()
            delay(1_000L)
        }
    }

    fun reloadAll() {
        scope.launch {
            coord.fetchWorld()
            coord.fetchPetState()
            neighbors = api.fetchPetNeighbors()
            guardBattles = api.fetchPetGuardBattles()
        }
    }

    LaunchedEffect(me?.id) {
        if (me != null) {
            reloadAll()
        }
    }

    if (me == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.bg),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("登录解锁鱼鱼谷与站宠系统", color = palette.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Button(
                    onClick = onRequireLogin,
                    colors = ButtonDefaults.buttonColors(containerColor = palette.primary, contentColor = palette.onPrimary),
                ) {
                    Text("去登录")
                }
            }
        }
        return
    }

    val calendar = remember(serverNowTickSec, ui.world?.clockStart) {
        IsleClockRules.snapshot(serverNowTickSec)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.bg),
    ) {
        Surface(color = palette.surface) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "island" to "岛屿协防",
                        "care" to "站宠状态",
                        "canvas" to "完整画布岛",
                    ).forEach { (id, label) ->
                        FilterChip(
                            selected = subTab == id,
                            onClick = { subTab = id },
                            label = { Text(label, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = palette.primary,
                                selectedLabelColor = palette.onPrimary,
                            ),
                        )
                    }
                }
                IconButton(onClick = { reloadAll() }) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "同步状态", tint = palette.primary)
                }
            }
        }

        when (subTab) {
            "canvas" -> {
                ShelllessWebScreen(
                    route = "/pets",
                    title = "鱼鱼谷 · HTML5 画布与街机",
                    cookieJar = api.cookieJar,
                    assetCache = api.webAssetCache,
                    modifier = Modifier.weight(1f),
                    showCompactBar = false,
                )
            }

            "care" -> {
                val cloud = ui.petState?.state
                val summary = ui.petState?.pet
                val rev = ui.petState?.revision ?: 0L
                val displayName = cloud?.nickname?.takeIf { it.isNotBlank() } ?: summary?.name ?: "大肥鱼"
                val lv = cloud?.level ?: summary?.level ?: 1
                val xp = cloud?.xp ?: summary?.xp ?: 0
                val hunger = cloud?.hunger ?: summary?.hunger ?: 100
                val affection = cloud?.affection ?: summary?.mood ?: 80
                val cleanliness = cloud?.cleanliness ?: summary?.clean ?: 100
                val energy = cloud?.energy ?: summary?.energy ?: 100

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item {
                        SectionCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column {
                                    Text(displayName, color = palette.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    Text("Lv.$lv · 经验 $xp / ${lv * 100} (云档 rev=$rev)", color = palette.muted, fontSize = 12.sp)
                                }
                                BadgePill(
                                    text = "${NumberFormat.getNumberInstance(Locale.CHINA).format(ui.world?.coins ?: ui.petState?.coins ?: 0L)} D币",
                                    containerColor = palette.gold.copy(alpha = 0.16f),
                                    contentColor = palette.gold,
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            StatProgressRow("饱食度", hunger)
                            StatProgressRow("亲密度", affection)
                            StatProgressRow("清洁度", cleanliness)
                            StatProgressRow("精力值", energy)

                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("feed" to "投喂", "play" to "陪玩", "clean" to "清洁").forEach { (act, label) ->
                                    OutlinedButton(
                                        onClick = {
                                            val curHunger = hunger
                                            val curEnergy = energy
                                            val curClean = cleanliness
                                            val curAff = affection
                                            val (nH, nE, nC, nA) = when (act) {
                                                "feed" -> listOf((curHunger + 18).coerceAtMost(100), curEnergy, curClean, (curAff + 2).coerceAtMost(100))
                                                "play" -> listOf((curHunger - 4).coerceAtLeast(0), (curEnergy - 10).coerceAtLeast(0), curClean, (curAff + 7).coerceAtMost(100))
                                                else -> listOf(curHunger, (curEnergy - 4).coerceAtLeast(0), (curClean + 22).coerceAtMost(100), (curAff + 3).coerceAtMost(100))
                                            }
                                            scope.launch {
                                                val res = coord.idempotentBattleOrStatePost(
                                                    "/api/pet/state",
                                                    buildJsonObject {
                                                        put("user_id", JsonPrimitive(me.id))
                                                        put("revision", JsonPrimitive(rev))
                                                        put(
                                                            "state",
                                                            buildJsonObject {
                                                                put("level", JsonPrimitive(lv))
                                                                put("xp", JsonPrimitive(xp))
                                                                put("hunger", JsonPrimitive(nH))
                                                                put("energy", JsonPrimitive(nE))
                                                                put("cleanliness", JsonPrimitive(nC))
                                                                put("affection", JsonPrimitive(nA))
                                                                cloud?.nickname?.let { put("nickname", JsonPrimitive(it)) }
                                                                cloud?.activePet?.let { put("activePet", JsonPrimitive(it)) }
                                                            },
                                                        )
                                                    },
                                                )
                                                actionFeedback = when (res) {
                                                    is PetCoexistenceCoordinator.SimpleWriteOutcome.Success -> "$label 云档已保存"
                                                    is PetCoexistenceCoordinator.SimpleWriteOutcome.Failed -> res.message
                                                    is PetCoexistenceCoordinator.SimpleWriteOutcome.UnconfirmedReconciled -> res.message
                                                }
                                                coord.fetchPetState()
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Text(label, fontSize = 12.sp)
                                    }
                                }
                            }
                            actionFeedback?.let { msg ->
                                Spacer(Modifier.height(8.dp))
                                Text(msg, color = palette.primary, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            else -> {
                val world = ui.world
                val worldState = world?.state
                val energy = worldState?.get("en")?.jsonPrimitive?.intOrNull ?: 0
                val sprLv = runCatching { worldState?.get("tools")?.jsonObject?.get("spr")?.jsonPrimitive?.intOrNull }.getOrNull() ?: 0
                val sprCover = listOf(0, 8, 16, 24).getOrElse(sprLv) { 0 }

                val (plotsCount, readyIndices, dryIndices) = remember(worldState, serverNowTickSec, sprCover, calendar.weatherIndex) {
                    val arr = worldState?.get("plots")?.jsonArray
                    if (arr == null) {
                        Triple(0, emptyList<Int>(), emptyList<Int>())
                    } else {
                        val ready = mutableListOf<Int>()
                        val dry = mutableListOf<Int>()
                        arr.forEachIndexed { idx, elem ->
                            if (elem is JsonNull) return@forEachIndexed
                            val obj = runCatching { elem.jsonObject }.getOrNull() ?: return@forEachIndexed
                            val cropId = obj["c"]?.jsonPrimitive?.content ?: return@forEachIndexed
                            val plantedAt = obj["t"]?.jsonPrimitive?.longOrNull ?: 0L
                            val watered = obj["w"]?.jsonPrimitive?.intOrNull ?: 0
                            val bonus = obj["g"]?.jsonPrimitive?.longOrNull ?: 0L
                            val harvests = obj["h"]?.jsonPrimitive?.intOrNull ?: 0
                            val autoWater = idx < sprCover || calendar.weatherIndex == 1
                            val stage = IsleClockRules.cropStage(cropId, plantedAt, bonus, harvests, autoWater, serverNowTickSec)
                            if (stage >= 3) {
                                ready += idx
                            } else if (stage in 0..2 && !autoWater && watered <= stage) {
                                dry += idx
                            }
                        }
                        Triple(arr.size, ready, dry)
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (ui.arcadeStandbyActive && ui.arcadeStandbySummary != null) {
                        item {
                            Surface(
                                color = palette.gold.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, palette.gold, RoundedCornerShape(16.dp)),
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(Icons.Rounded.SportsEsports, contentDescription = null, tint = palette.gold)
                                    Spacer(Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Arcade-Standby 礼让保护生效中", color = palette.gold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(ui.arcadeStandbySummary!!, color = palette.text, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    ui.lastConflictNote?.let { conflict ->
                        item {
                            Surface(
                                color = palette.danger.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = palette.danger)
                                    Spacer(Modifier.width(8.dp))
                                    Text(conflict, color = palette.danger, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    // Dynamic Island Calendar Card
                    item {
                        SectionCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "第 ${calendar.dayNo} 岛日 · ${calendar.seasonName}季 · ${calendar.weatherName}",
                                        color = palette.text,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        text = String.format(
                                            Locale.CHINA,
                                            "岛时 %02d:%02d · 本季余 %d 天 · %s",
                                            calendar.isleHour,
                                            calendar.isleMinute,
                                            calendar.seasonDaysLeft,
                                            if (calendar.isLongNight) "长夜静默期" else "白天运行期",
                                        ),
                                        color = palette.muted,
                                        fontSize = 12.sp,
                                    )
                                }
                                BadgePill(
                                    text = if (calendar.clockConfigured) "clockStart 已校准" else "默认时钟",
                                    containerColor = palette.primary.copy(alpha = 0.14f),
                                    contentColor = palette.primary,
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "距下一岛日：${ServerClock.formatCountdown(calendar.nextBoundaryEpochSec, serverNowTickSec)}",
                                color = palette.gold,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                            )

                            if (calendar.events.isNotEmpty()) {
                                Spacer(Modifier.height(10.dp))
                                calendar.events.forEach { ev ->
                                    Surface(
                                        color = palette.surface2,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp),
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text(
                                                text = "[${ev.guildName}] ${ev.text}",
                                                color = palette.text,
                                                fontSize = 12.sp,
                                                modifier = Modifier.weight(1f),
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            BadgePill("×${ev.multiplier}")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Farm / Island Quick Actions & Optimistic Rev Control
                    item {
                        SectionCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column {
                                    Text("我的小岛状态", color = palette.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        text = "D币 ${NumberFormat.getNumberInstance(Locale.CHINA).format(world?.coins ?: 0L)} · 体力 $energy · $plotsCount 地块 (${readyIndices.size} 可收 / ${dryIndices.size} 待水)",
                                        color = palette.muted,
                                        fontSize = 12.sp,
                                    )
                                }
                                BadgePill("rev=${world?.rev ?: 0}")
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    enabled = readyIndices.isNotEmpty(),
                                    onClick = {
                                        scope.launch {
                                            val actions = readyIndices.map { i ->
                                                buildJsonObject {
                                                    put("a", JsonPrimitive("harvest"))
                                                    put("i", JsonPrimitive(i))
                                                }
                                            }
                                            val res = coord.submitWorldActions(actions)
                                            actionFeedback = when (res) {
                                                is PetCoexistenceCoordinator.WorldActOutcome.Applied ->
                                                    "收获 ${actions.size} 块成熟作物完成 (rev=${res.snapshot.rev})"
                                                is PetCoexistenceCoordinator.WorldActOutcome.ConflictAdopted ->
                                                    res.reason
                                                is PetCoexistenceCoordinator.WorldActOutcome.Failed ->
                                                    res.message
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = palette.primary, contentColor = palette.onPrimary),
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("收熟地 (${readyIndices.size})", fontSize = 12.sp)
                                }
                                OutlinedButton(
                                    enabled = dryIndices.isNotEmpty(),
                                    onClick = {
                                        scope.launch {
                                            val actions = dryIndices.map { i ->
                                                buildJsonObject {
                                                    put("a", JsonPrimitive("water"))
                                                    put("i", JsonPrimitive(i))
                                                }
                                            }
                                            val res = coord.submitWorldActions(actions)
                                            actionFeedback = when (res) {
                                                is PetCoexistenceCoordinator.WorldActOutcome.Applied ->
                                                    "已完成 ${actions.size} 块作物浇水 (rev=${res.snapshot.rev})"
                                                is PetCoexistenceCoordinator.WorldActOutcome.ConflictAdopted ->
                                                    res.reason
                                                is PetCoexistenceCoordinator.WorldActOutcome.Failed ->
                                                    res.message
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text("一键浇水 (${dryIndices.size})", fontSize = 12.sp)
                                }
                            }
                            actionFeedback?.let { note ->
                                Spacer(Modifier.height(8.dp))
                                Text(note, color = palette.primary, fontSize = 12.sp)
                            }
                        }
                    }

                    // Neighbors Co-op & Non-Idempotent Visit/Help
                    if (neighbors.isNotEmpty()) {
                        item {
                            Text(
                                text = "邻居串门与帮忙（非幂等写接口 · 严格禁自动重试）",
                                color = palette.muted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 4.dp),
                            )
                        }
                        items(neighbors.take(8), key = { it.resolvedId }) { nb ->
                            SectionCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(nb.displayName, color = palette.text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                        Text(
                                            text = "大肥鱼 Lv.${nb.resolvedLevel} · 可偷 ${nb.plots} 地 / ${nb.pens} 窝",
                                            color = palette.muted,
                                            fontSize = 12.sp,
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        OutlinedButton(onClick = {
                                            scope.launch {
                                                val res = coord.nonIdempotentWorldPost("/api/pet/world/help/${nb.resolvedId}")
                                                actionFeedback = when (res) {
                                                    is PetCoexistenceCoordinator.SimpleWriteOutcome.Success -> "已帮 ${nb.displayName} 照料"
                                                    is PetCoexistenceCoordinator.SimpleWriteOutcome.UnconfirmedReconciled -> res.message
                                                    is PetCoexistenceCoordinator.SimpleWriteOutcome.Failed -> res.message
                                                }
                                            }
                                        }) {
                                            Text("帮忙", fontSize = 12.sp)
                                        }
                                        OutlinedButton(onClick = {
                                            scope.launch {
                                                val res = coord.nonIdempotentWorldPost("/api/pet/world/visit/${nb.resolvedId}")
                                                actionFeedback = when (res) {
                                                    is PetCoexistenceCoordinator.SimpleWriteOutcome.Success -> "完成串门 ${nb.displayName}"
                                                    is PetCoexistenceCoordinator.SimpleWriteOutcome.UnconfirmedReconciled -> res.message
                                                    is PetCoexistenceCoordinator.SimpleWriteOutcome.Failed -> res.message
                                                }
                                            }
                                        }) {
                                            Text("串门", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (guardBattles.isNotEmpty()) {
                        item {
                            SectionCard {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Shield, contentDescription = null, tint = palette.primary)
                                    Spacer(Modifier.width(6.dp))
                                    Text("大肥鱼守家战报", color = palette.text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                }
                                Spacer(Modifier.height(6.dp))
                                guardBattles.take(5).forEach { b ->
                                    val isAtk = b.attackerId == me.id
                                    val outcome = when {
                                        b.winner == -1 -> "打平"
                                        (b.winner == 0) == isAtk -> "获胜"
                                        else -> "落败"
                                    }
                                    val desc = if (isAtk) {
                                        "拜访 ${b.defender} · $outcome"
                                    } else {
                                        "${b.attacker} 来袭 · 守护$outcome"
                                    }
                                    Text("• $desc", color = palette.muted, fontSize = 12.sp, modifier = Modifier.padding(vertical = 2.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatProgressRow(label: String, value: Int) {
    val palette = LocalForHappyPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = palette.muted, fontSize = 12.sp, modifier = Modifier.width(56.dp))
        LinearProgressIndicator(
            progress = { (value / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .weight(1f)
                .height(8.dp),
            color = palette.primary,
            trackColor = palette.surface2,
        )
        Spacer(Modifier.width(8.dp))
        Text("$value%", color = palette.text, fontSize = 12.sp, modifier = Modifier.width(40.dp))
    }
}
