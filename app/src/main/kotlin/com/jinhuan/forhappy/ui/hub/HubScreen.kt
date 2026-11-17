package com.jinhuan.forhappy.ui.hub

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.jinhuan.forhappy.core.clock.ServerClock
import com.jinhuan.forhappy.core.db.ForHappyDatabase
import com.jinhuan.forhappy.core.model.DynamicModule
import com.jinhuan.forhappy.core.model.MeUser
import com.jinhuan.forhappy.core.model.MemeItem
import com.jinhuan.forhappy.core.model.RoadItem
import com.jinhuan.forhappy.core.model.TodayStats
import com.jinhuan.forhappy.core.network.api.ForHappyApiClient
import com.jinhuan.forhappy.core.network.media.MediaUrlRewriter
import com.jinhuan.forhappy.ui.components.BadgePill
import com.jinhuan.forhappy.ui.components.ForHappyAvatar
import com.jinhuan.forhappy.ui.components.SectionCard
import com.jinhuan.forhappy.ui.theme.LocalForHappyPalette
import com.jinhuan.forhappy.ui.wall.FullscreenMediaOverlay
import com.jinhuan.forhappy.ui.wall.MemeDetailModal
import com.jinhuan.forhappy.ui.web.ShelllessWebScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Dynamic Topbar Extension Hub (`百宝箱`):
 * - Displays all live modules parsed by [TopbarModuleScanner] from `52dmeme.com` topbar.
 * - Lets users pin any module into Bottom Nav Slot 3 (replacing or restoring 鱼鱼谷).
 * - Hosts native views for Road (`鞭打`), Puzzles (`解谜`), Moyu (`摸鱼工具`), and accelerated
 *   [ShelllessWebScreen] for Novels (`/novels`) and newly launched server modules.
 */
@Composable
fun HubScreen(
    api: ForHappyApiClient,
    db: ForHappyDatabase,
    me: MeUser?,
    modules: List<DynamicModule>,
    pinnedModuleId: String,
    onPinModule: (DynamicModule) -> Unit,
    onRefreshModules: () -> Unit,
    onNavigateRootTab: (String) -> Unit,
) {
    val palette = LocalForHappyPalette.current
    var activeSubModule by remember { mutableStateOf<DynamicModule?>(null) }

    val sub = activeSubModule
    if (sub != null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.bg),
        ) {
            Surface(color = palette.surface) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { activeSubModule = null }) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回百宝箱", tint = palette.text)
                        }
                        Text(sub.label, color = palette.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(onClick = { onPinModule(sub) }) {
                        Icon(Icons.Rounded.PushPin, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (pinnedModuleId == sub.id) "已钉到底栏" else "钉到底栏", fontSize = 11.sp)
                    }
                }
            }

            when (sub.id) {
                "road" -> NativeRoadPane(api = api, me = me)
                "puzzles" -> NativePuzzlesPane(api = api, me = me)
                "moyu" -> NativeMoyuPane(api = api, db = db, me = me)
                else -> ShelllessWebScreen(
                    route = sub.route,
                    title = sub.label,
                    cookieJar = api.cookieJar,
                    assetCache = api.webAssetCache,
                    showCompactBar = false,
                )
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.bg)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SectionCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("动态模块百宝箱", color = palette.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = "自动同步站长顶栏更新，长按或点图钉可替换底栏第 3 个常驻入口。",
                        color = palette.muted,
                        fontSize = 12.sp,
                    )
                }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = onRefreshModules) {
                    Text("同步顶栏", fontSize = 12.sp)
                }
            }
        }

        val hubModules = modules.filter { it.id !in setOf("wall", "chat") }
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(hubModules, key = { it.route }) { mod ->
                val isPinned = mod.id == pinnedModuleId
                Surface(
                    color = palette.surface,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (isPinned) 1.5.dp else 1.dp,
                            color = if (isPinned) palette.primary else palette.line,
                            shape = RoundedCornerShape(18.dp),
                        )
                        .clickable {
                            if (mod.id == "pets") {
                                onNavigateRootTab("slot3")
                            } else {
                                activeSubModule = mod
                            }
                        },
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Surface(
                                color = palette.primary.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Icon(
                                    imageVector = iconForModule(mod.iconKey),
                                    contentDescription = mod.label,
                                    tint = palette.primary,
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .size(22.dp),
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (mod.isNewFromTopbar) {
                                    BadgePill("NEW", containerColor = palette.gold.copy(alpha = 0.2f), contentColor = palette.gold)
                                }
                                IconButton(
                                    onClick = { onPinModule(mod) },
                                    modifier = Modifier.size(28.dp),
                                ) {
                                    Icon(
                                        Icons.Rounded.PushPin,
                                        contentDescription = "钉选到底栏",
                                        tint = if (isPinned) palette.primary else palette.muted.copy(alpha = 0.45f),
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        }

                        Text(mod.label, color = palette.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = if (mod.isBuiltInNative) "${mod.route} · 原生集成" else "${mod.route} · 站长新上线无壳容器",
                            color = palette.muted,
                            fontSize = 11.sp,
                        )
                    }
                }
            }
        }
    }
}

private fun iconForModule(key: String): ImageVector = when (key) {
    "pets" -> Icons.Rounded.AutoAwesome
    "book" -> Icons.AutoMirrored.Rounded.MenuBook
    "road" -> Icons.Rounded.Flag
    "puzzle" -> Icons.Rounded.Extension
    "tools" -> Icons.Rounded.Build
    else -> Icons.Rounded.Language
}

@Composable
private fun NativeRoadPane(api: ForHappyApiClient, me: MeUser?) {
    val palette = LocalForHappyPalette.current
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf<List<RoadItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var selectedKind by remember { mutableStateOf("feature") }
    var titleDraft by remember { mutableStateOf("") }
    var bodyDraft by remember { mutableStateOf("") }
    var note by remember { mutableStateOf<String?>(null) }

    val kindLabels = mapOf("feature" to "需求", "bug" to "Bug", "idea" to "改进意见")
    val statusLabels = mapOf(
        "open" to "待评估",
        "planned" to "计划中",
        "doing" to "进行中",
        "done" to "已上线",
        "declined" to "暂不做",
    )

    LaunchedEffect(Unit) {
        loading = true
        items = api.fetchRoadItems()
        loading = false
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (me != null) {
            item {
                SectionCard {
                    Text("向站长提需求 / 反馈 Bug", color = palette.text, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("feature" to "需求", "bug" to "Bug", "idea" to "改进意见").forEach { (k, label) ->
                            FilterChip(
                                selected = selectedKind == k,
                                onClick = { selectedKind = k },
                                label = { Text(label, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = palette.primary,
                                    selectedLabelColor = palette.onPrimary,
                                ),
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = titleDraft,
                        onValueChange = { titleDraft = it },
                        label = { Text("简短标题") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = bodyDraft,
                        onValueChange = { bodyDraft = it },
                        label = { Text("详细说明") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        enabled = titleDraft.isNotBlank() && bodyDraft.isNotBlank(),
                        onClick = {
                            scope.launch {
                                val res = api.createRoadItem(selectedKind, titleDraft.trim(), bodyDraft.trim())
                                note = if (res.isSuccess) {
                                    titleDraft = ""
                                    bodyDraft = ""
                                    items = api.fetchRoadItems()
                                    "已提交鞭打"
                                } else {
                                    res.exceptionOrNull()?.message ?: "提交失败"
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = palette.primary, contentColor = palette.onPrimary),
                    ) {
                        Text("发送鞭打")
                    }
                    note?.let { Text(it, color = palette.primary, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp)) }
                }
            }
        }

        if (loading && items.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = palette.primary)
                }
            }
        }

        items(items, key = { it.id }) { road ->
            SectionCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            BadgePill(kindLabels[road.kind] ?: road.kind)
                            road.status?.let { st ->
                                BadgePill(
                                    statusLabels[st] ?: st,
                                    containerColor = palette.primary.copy(alpha = 0.14f),
                                    contentColor = palette.primary,
                                )
                            }
                        }
                        Text(road.title, color = palette.text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        if (road.body.isNotBlank()) {
                            Text(road.body, color = palette.muted, fontSize = 12.sp)
                        }
                        road.reply?.takeIf { it.isNotBlank() }?.let { rep ->
                            Surface(
                                color = palette.surface2,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("站长回复：$rep", color = palette.primary, fontSize = 12.sp, modifier = Modifier.padding(8.dp))
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            ForHappyAvatar(
                                username = road.user?.displayName ?: road.authorName ?: "欢友",
                                avatarUrl = road.user?.avatar,
                                imageLoader = api.imageLoader,
                                size = 18.dp,
                            )
                            Text(
                                text = "${road.user?.displayName ?: road.authorName ?: "欢友"} · ${road.comments} 讨论",
                                color = palette.muted,
                                fontSize = 11.sp,
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = {
                        scope.launch {
                            api.voteRoadItem(road.id, if (road.effectiveMyVote == 1) 0 else 1)
                            items = api.fetchRoadItems()
                        }
                    }) {
                        Icon(
                            Icons.Rounded.ThumbUp,
                            contentDescription = null,
                            tint = if (road.effectiveMyVote == 1) palette.primary else palette.muted,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("${road.displayScore}", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun NativePuzzlesPane(api: ForHappyApiClient, me: MeUser?) {
    val palette = LocalForHappyPalette.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var puzzles by remember { mutableStateOf<List<MemeItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var answers by remember { mutableStateOf<Map<Long, String>>(emptyMap()) }
    var feedback by remember { mutableStateOf<Pair<Long, String>?>(null) }
    var detailMemeId by remember { mutableStateOf<Long?>(null) }
    var fullscreenUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        loading = true
        puzzles = api.fetchPuzzles()
        loading = false
    }

    if (loading && puzzles.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = palette.primary)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (puzzles.isEmpty()) {
            item {
                SectionCard {
                    Text("暂时没有解谜题目", color = palette.muted, fontSize = 13.sp)
                }
            }
        }

        items(puzzles, key = { it.id }) { meme ->
            val pz = meme.puzzle
            if (pz == null) return@items
            val isOpen = pz.status == "open"
            val rewardLabel = when (pz.rewardKind) {
                "cdk" -> "CDK / 兑换码"
                "link" -> "隐藏链接"
                "dcoin" -> "D币奖励"
                else -> "神秘彩蛋"
            }

            SectionCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { detailMemeId = meme.id },
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val thumbUrl = meme.primaryImageUrl
                    var thumbFailed by remember(meme.id, thumbUrl) { mutableStateOf(false) }
                    if (!thumbUrl.isNullOrBlank() && !thumbFailed) {
                        AsyncImage(
                            model = MediaUrlRewriter.rewriteMediaUrl(thumbUrl),
                            imageLoader = api.imageLoader,
                            contentDescription = meme.safeLabel,
                            contentScale = ContentScale.Crop,
                            onError = { thumbFailed = true },
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(palette.surface2),
                        )
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            BadgePill(
                                text = if (pz.solved) "已解开" else if (isOpen) "进行中" else "已结束",
                                containerColor = if (isOpen) palette.primary.copy(alpha = 0.14f) else palette.surface2,
                                contentColor = if (isOpen) palette.primary else palette.muted,
                            )
                            BadgePill(
                                text = rewardLabel,
                                containerColor = palette.gold.copy(alpha = 0.14f),
                                contentColor = palette.gold,
                            )
                        }
                        Text(
                            text = meme.title?.takeIf { it.isNotBlank() }
                                ?: meme.text?.takeIf { it.isNotBlank() }
                                ?: "解谜帖子 No.${meme.id}",
                            color = palette.text,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = buildString {
                                if (pz.layerCount > 1) append("${pz.layerCount} 层连环 · ")
                                append("已解 ${pz.winnersCount}")
                                pz.maxWinners?.takeIf { it > 0 }?.let { append(" / $it") }
                                pz.codesLeft?.takeIf { it > 0 }?.let { append(" · 剩 $it 码") }
                            },
                            color = palette.muted,
                            fontSize = 11.sp,
                        )
                    }
                }

                pz.hint?.takeIf { it.isNotBlank() }?.let { hint ->
                    Spacer(Modifier.height(6.dp))
                    Text("提示：$hint", color = palette.muted, fontSize = 12.sp)
                }

                pz.current?.let { layer ->
                    Spacer(Modifier.height(8.dp))
                    layer.prompt?.takeIf { it.isNotBlank() && it != pz.hint }?.let { pr ->
                        Text(pr, color = palette.text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                    val ld = layer.data
                    if (layer.kind == "cipher" && !ld?.text.isNullOrBlank()) {
                        Surface(
                            color = palette.surface2,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = ld!!.text!!,
                                    fontFamily = FontFamily.Monospace,
                                    color = palette.text,
                                    fontSize = 12.sp,
                                    modifier = Modifier.weight(1f),
                                )
                                IconButton(
                                    onClick = { clipboard.setText(AnnotatedString(ld.text!!)) },
                                    modifier = Modifier.size(28.dp),
                                ) {
                                    Icon(Icons.Rounded.ContentCopy, contentDescription = "复制密文", tint = palette.primary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                    if (!ld?.img.isNullOrBlank()) {
                        val layerImgUrl = MediaUrlRewriter.rewriteMediaUrl(ld!!.img)
                        AsyncImage(
                            model = layerImgUrl,
                            imageLoader = api.imageLoader,
                            contentDescription = "题面图片",
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { fullscreenUrl = layerImgUrl },
                        )
                    }
                    if (layer.kind == "quiz" && ld?.options?.isNotEmpty() == true && isOpen && !pz.solved) {
                        Column(modifier = Modifier.padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            ld.options.forEachIndexed { idx, opt ->
                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            val res = api.solvePuzzle(pz.id, opt)
                                            val ok = res["correct"]?.jsonPrimitive?.booleanOrNull == true
                                            val msg = if (ok) "答对了！" else res["error"]?.jsonPrimitive?.content ?: "选错了，再试试"
                                            feedback = pz.id to msg
                                            puzzles = api.fetchPuzzles()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text("${'A' + idx}. $opt", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                pz.reward?.takeIf { it.isNotBlank() }?.let { rw ->
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        color = palette.primary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("奖励：$rw", color = palette.primary, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                            TextButton(onClick = { clipboard.setText(AnnotatedString(rw)) }) {
                                Text("复制", fontSize = 12.sp)
                            }
                        }
                    }
                }

                if (isOpen && !pz.solved && pz.current?.kind != "quiz") {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = answers[pz.id].orEmpty(),
                            onValueChange = { v -> answers = answers + (pz.id to v) },
                            placeholder = { Text("输入谜底", fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        Button(
                            onClick = {
                                val ans = answers[pz.id].orEmpty().trim()
                                if (ans.isBlank()) return@Button
                                scope.launch {
                                    val res = api.solvePuzzle(pz.id, ans)
                                    val ok = res["correct"]?.jsonPrimitive?.booleanOrNull == true
                                    val next = res["next"]?.jsonPrimitive?.booleanOrNull == true
                                    val msg = when {
                                        ok && next -> "这一层过了，继续下一层！"
                                        ok -> "答对了！已解锁奖励"
                                        else -> res["error"]?.jsonPrimitive?.content ?: "不对，再想想"
                                    }
                                    feedback = pz.id to msg
                                    puzzles = api.fetchPuzzles()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = palette.primary, contentColor = palette.onPrimary),
                        ) {
                            Text("解密")
                        }
                    }
                }

                if (feedback?.first == pz.id) {
                    Text(
                        text = feedback!!.second,
                        color = palette.primary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }
    }

    detailMemeId?.let { id ->
        MemeDetailModal(
            memeId = id,
            api = api,
            me = me,
            onDismiss = { detailMemeId = null },
            onOpenImageFullscreen = { fullscreenUrl = it },
            onTagClick = {},
            onStateUpdated = {},
        )
    }

    fullscreenUrl?.let { url ->
        FullscreenMediaOverlay(imageUrl = url, api = api, onDismiss = { fullscreenUrl = null })
    }
}

@Composable
private fun NativeMoyuPane(
    api: ForHappyApiClient,
    db: ForHappyDatabase,
    me: MeUser?,
) {
    val palette = LocalForHappyPalette.current
    val scope = rememberCoroutineScope()

    var today by remember { mutableStateOf<TodayStats?>(null) }
    var serverNowSec by remember { mutableLongStateOf(ServerClock.serverNowSec()) }
    var showSettings by remember { mutableStateOf(false) }

    var startHm by remember { mutableStateOf("09:00") }
    var offHm by remember { mutableStateOf("18:00") }
    var payDay by remember { mutableStateOf("10") }
    var monthlySalary by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        db.getKv("moyu.start")?.takeIf { it.contains(":") }?.let { startHm = it }
        db.getKv("moyu.off")?.takeIf { it.contains(":") }?.let { offHm = it }
        db.getKv("moyu.payday")?.toIntOrNull()?.let { payDay = it.toString() }
        db.getKv("moyu.salary")?.let { monthlySalary = it }

        while (isActive) {
            serverNowSec = ServerClock.serverNowSec()
            delay(1_000L)
        }
    }

    LaunchedEffect(me?.id) {
        if (me != null) {
            today = api.fetchTodayStats()
        }
    }

    val bjZone = remember { ZoneId.of("Asia/Shanghai") }
    val nowDt = remember(serverNowSec) {
        Instant.ofEpochSecond(serverNowSec).atZone(bjZone)
    }
    val startTime = remember(startHm) { parseHm(startHm, 9, 0) }
    val offTime = remember(offHm) { parseHm(offHm, 18, 0) }
    val isWeekend = nowDt.dayOfWeek == DayOfWeek.SATURDAY || nowDt.dayOfWeek == DayOfWeek.SUNDAY
    val startDt = nowDt.toLocalDate().atTime(startTime).atZone(bjZone)
    val offDt = nowDt.toLocalDate().atTime(offTime).atZone(bjZone)

    val (offLine, pct) = when {
        isWeekend -> "今天周末，不用上班" to 0f
        nowDt.isBefore(startDt) -> {
            val sec = ChronoUnit.SECONDS.between(nowDt, startDt)
            "还没上班，${formatHms(sec)} 后开工" to 0f
        }
        !nowDt.isBefore(offDt) -> "已经下班啦~" to 1f
        else -> {
            val left = ChronoUnit.SECONDS.between(nowDt, offDt)
            val total = ChronoUnit.SECONDS.between(startDt, offDt).coerceAtLeast(1L)
            val ratio = (1f - left.toFloat() / total.toFloat()).coerceIn(0f, 1f)
            "还有 ${formatHms(left)}" to ratio
        }
    }

    val daysToPay = remember(nowDt.toLocalDate(), payDay) {
        val targetDay = (payDay.toIntOrNull() ?: 10).coerceIn(1, 31)
        val curDate = nowDt.toLocalDate()
        val thisMonthDate = LocalDate.of(curDate.year, curDate.month, targetDay.coerceAtMost(curDate.lengthOfMonth()))
        val nextPayDate = if (!thisMonthDate.isBefore(curDate)) {
            thisMonthDate
        } else {
            val nm = curDate.plusMonths(1)
            LocalDate.of(nm.year, nm.month, targetDay.coerceAtMost(nm.lengthOfMonth()))
        }
        ChronoUnit.DAYS.between(curDate, nextPayDate)
    }

    val daysToWeekend = remember(nowDt) {
        when (nowDt.dayOfWeek) {
            DayOfWeek.SATURDAY, DayOfWeek.SUNDAY -> 0L
            else -> ChronoUnit.DAYS.between(
                nowDt.toLocalDate(),
                nowDt.toLocalDate().plusDays((DayOfWeek.FRIDAY.value - nowDt.dayOfWeek.value).toLong()),
            )
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Countdown Hero Card
        item {
            SectionCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("下班倒计时", color = palette.muted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    IconButton(onClick = { showSettings = !showSettings }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Rounded.Settings, contentDescription = "设置班表", tint = palette.muted, modifier = Modifier.size(18.dp))
                    }
                }
                Text(
                    text = offLine,
                    color = palette.primary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                if (pct > 0f) {
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { pct },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(999.dp)),
                        color = palette.primary,
                        trackColor = palette.surface2,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = String.format(Locale.CHINA, "今天已熬过 %.1f%% · 班表 %s - %s", pct * 100f, startHm, offHm),
                        color = palette.muted,
                        fontSize = 11.sp,
                    )
                }

                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = palette.line)
                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = if (isWeekend || daysToWeekend == 0L) "周末进行中" else "距周五下班 $daysToWeekend 天",
                        color = palette.text,
                        fontSize = 13.sp,
                    )
                    Text(
                        text = if (daysToPay == 0L) "今天发工资！" else "距发薪还有 $daysToPay 天（每月 ${payDay} 号）",
                        color = if (daysToPay == 0L) palette.gold else palette.text,
                        fontSize = 13.sp,
                        fontWeight = if (daysToPay == 0L) FontWeight.Bold else FontWeight.Normal,
                    )
                }

                val salaryNum = monthlySalary.toDoubleOrNull() ?: 0.0
                if (salaryNum > 0.0) {
                    val dailyPay = salaryNum / 21.75
                    val earnedToday = if (isWeekend) 0.0 else dailyPay * pct
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = String.format(Locale.CHINA, "今日已带薪收入约 ¥%.2f（仅本机计算，绝不上传）", earnedToday),
                        color = palette.gold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                if (showSettings) {
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = palette.line)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = startHm,
                            onValueChange = { startHm = it },
                            label = { Text("上班 (HH:mm)", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = offHm,
                            onValueChange = { offHm = it },
                            label = { Text("下班 (HH:mm)", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = payDay,
                            onValueChange = { payDay = it },
                            label = { Text("发薪日 (1-31)", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = monthlySalary,
                            onValueChange = { monthlySalary = it },
                            label = { Text("月薪(本地)", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                db.putKv("moyu.start", startHm.trim())
                                db.putKv("moyu.off", offHm.trim())
                                db.putKv("moyu.payday", payDay.trim())
                                db.putKv("moyu.salary", monthlySalary.trim())
                                showSettings = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = palette.primary, contentColor = palette.onPrimary),
                    ) {
                        Text("保存设置")
                    }
                }
            }
        }

        // Today Report Card
        item {
            SectionCard {
                Text("今日摸鱼小报", color = palette.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = if (today?.day?.isNotBlank() == true) "${today!!.day} · 尽欢营业记录" else "把快乐攒成一页",
                    color = palette.muted,
                    fontSize = 11.sp,
                )
                Spacer(Modifier.height(10.dp))

                if (me == null) {
                    Text("登录后可查看站内今日浏览、发文与群聊活跃数据。", color = palette.muted, fontSize = 13.sp)
                } else if (today == null) {
                    Text("正在统计今天的战绩…", color = palette.muted, fontSize = 13.sp)
                } else {
                    val t = today!!
                    val metrics = listOf(
                        "浏览帖子" to t.seen,
                        "发布帖子" to t.posts,
                        "群聊发言" to t.msgs,
                        "浏览消息" to t.browsed,
                        "帖子回应" to t.postReacts,
                        "群聊互动" to t.reacts,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        metrics.chunked(3).forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                rowItems.forEach { (label, count) ->
                                    Surface(
                                        color = palette.surface2,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                        ) {
                                            Text(
                                                text = count.toString(),
                                                color = palette.primary,
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                            )
                                            Text(label, color = palette.muted, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    val unseenProgress = if (t.newSeen > 0) t.newSeen else t.seen
                    val hint = when {
                        unseenProgress < 200 -> "今日首次看过 $unseenProgress 帖，再看 ${200 - unseenProgress} 个未看帖子明天可拿「摸鱼高手」徽章"
                        t.posts < 5 -> "已达「摸鱼高手」标准！再发 ${5 - t.posts} 张图还能拿「发图达人」"
                        else -> "今天的门槛徽章已稳稳拿下，冠军徽章明日揭晓"
                    }
                    Text(hint, color = palette.muted, fontSize = 12.sp)
                }
            }
        }
    }
}

private fun parseHm(raw: String, defHour: Int, defMin: Int): LocalTime {
    val parts = raw.trim().split(':')
    val h = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: defHour
    val m = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: defMin
    return LocalTime.of(h, m)
}

private fun formatHms(totalSeconds: Long): String {
    val s = totalSeconds.coerceAtLeast(0L)
    val h = s / 3600L
    val m = (s % 3600L) / 60L
    val sec = s % 60L
    return String.format(Locale.CHINA, "%02d:%02d:%02d", h, m, sec)
}
