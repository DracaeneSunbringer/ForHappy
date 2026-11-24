package com.jinhuan.forhappy.ui.me

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jinhuan.forhappy.core.clock.ServerClock
import com.jinhuan.forhappy.core.cosmetics.BadgeAndCosmeticRegistry
import com.jinhuan.forhappy.core.cosmetics.ChatBubbleCosmetics
import com.jinhuan.forhappy.core.cosmetics.OwnedBadgeItem
import com.jinhuan.forhappy.core.model.DecoConfig
import com.jinhuan.forhappy.core.model.ForHappyJson
import com.jinhuan.forhappy.core.model.HbKeyStatus
import com.jinhuan.forhappy.core.model.MeUser
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.jinhuan.forhappy.core.model.Owner52bCard
import com.jinhuan.forhappy.core.model.Owner52bPack
import com.jinhuan.forhappy.core.model.WalletLedgerRow
import com.jinhuan.forhappy.core.model.WalletSummary
import com.jinhuan.forhappy.core.navigation.StealthExemptionGate
import com.jinhuan.forhappy.core.network.api.ForHappyApiClient
import com.jinhuan.forhappy.core.network.media.MediaUrlRewriter
import com.jinhuan.forhappy.ui.components.BadgeArtIcon
import com.jinhuan.forhappy.ui.components.BadgeChipView
import com.jinhuan.forhappy.ui.components.BadgePill
import com.jinhuan.forhappy.ui.components.BoostPillText
import com.jinhuan.forhappy.ui.components.ChatCosmeticBoostPill
import com.jinhuan.forhappy.ui.components.ChatCosmeticBubbleBox
import com.jinhuan.forhappy.ui.components.ForHappyAvatar
import com.jinhuan.forhappy.ui.components.ForHappyTextField
import com.jinhuan.forhappy.ui.components.TitleChipView
import com.jinhuan.forhappy.ui.components.parseSafeHex
import com.jinhuan.forhappy.ui.theme.ForHappyDimens
import com.jinhuan.forhappy.ui.theme.LocalForHappyPalette
import com.jinhuan.forhappy.ui.theme.TABULAR_FONT_FEATURE
import com.jinhuan.forhappy.ui.wall.ConfigureFullscreenDialogWindow
import com.jinhuan.forhappy.ui.wall.DialogBackOwnerScope
import com.jinhuan.forhappy.ui.wall.FullscreenDialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

const val KV_DECO_CLEAN = "deco.clean.v2"

fun parseDecoConfig(deco: JsonElement?, localCleanRaw: String? = null): DecoConfig {
    return BadgeAndCosmeticRegistry.parseServerDecoConfig(deco, localCleanRaw)
}

fun extractBadgeLabels(badges: JsonElement?): List<String> {
    return BadgeAndCosmeticRegistry.parseOwnedBadges(badges).map { item ->
        if (item.levelText.isNotBlank()) "${item.meta.name} ${item.levelText}" else item.meta.name
    }
}

fun parseHexColor(hex: String, fallback: Color): Color {
    val clean = hex.trim().removePrefix("#")
    val parsed = clean.toLongOrNull(16) ?: return fallback
    return when (clean.length) {
        6 -> Color((0xFF000000L or parsed))
        8 -> Color(parsed)
        else -> fallback
    }
}

@Composable
fun ProfileEditDialog(
    api: ForHappyApiClient,
    me: MeUser,
    onSaved: () -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalForHappyPalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var nick by remember(me.id) { mutableStateOf(me.nick?.ifBlank { me.name.orEmpty() } ?: me.displayName) }
    var bio by remember(me.id) { mutableStateOf(me.bio.orEmpty()) }
    var uploadedAvatarKey by remember { mutableStateOf<String?>(null) }
    var avatarUri by remember { mutableStateOf<Uri?>(null) }
    var saving by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf<String?>(null) }

    val pickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        avatarUri = uri
        saving = true
        statusText = "正在上传头像…"
        scope.launch {
            val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
            val bytes = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            }
            if (bytes == null || bytes.isEmpty()) {
                saving = false
                statusText = "读取所选图片失败"
                return@launch
            }
            val res = api.uploadMediaBytes(
                bytes = bytes,
                fileName = "avatar_${System.currentTimeMillis()}.jpg",
                mimeType = mime,
                forWhat = "avatar",
            )
            saving = false
            res.fold(
                onSuccess = { up ->
                    uploadedAvatarKey = up.key
                    statusText = "新头像已上传，点击保存生效"
                },
                onFailure = { err ->
                    statusText = err.message ?: "头像上传失败"
                },
            )
        }
    }

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        containerColor = palette.surface,
        titleContentColor = palette.text,
        textContentColor = palette.text,
        title = { Text("编辑个人资料") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ForHappyAvatar(
                        username = nick.ifBlank { me.displayName },
                        avatarUrl = avatarUri?.toString() ?: me.avatar,
                        imageLoader = api.imageLoader,
                        size = 54.dp,
                    )
                    OutlinedButton(
                        enabled = !saving,
                        onClick = {
                            StealthExemptionGate.markInternalJump()
                            pickerLauncher.launch("image/*")
                        },
                    ) {
                        Icon(Icons.Rounded.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("更换头像", fontSize = 12.sp)
                    }
                }
                ForHappyTextField(
                    value = nick,
                    onValueChange = { nick = it },
                    label = "昵称",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                ForHappyTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = "个人签名 / Bio",
                    singleLine = false,
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                )
                statusText?.let { msg ->
                    Text(msg, color = palette.primary, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !saving,
                onClick = {
                    saving = true
                    scope.launch {
                        val res = api.updateMeProfile(nick, bio, uploadedAvatarKey)
                        saving = false
                        res.fold(
                            onSuccess = {
                                onSaved()
                                onDismiss()
                            },
                            onFailure = { err ->
                                statusText = err.message ?: "保存失败"
                            },
                        )
                    }
                },
            ) {
                Text(if (saving) "保存中…" else "保存")
            }
        },
        dismissButton = {
            TextButton(enabled = !saving, onClick = onDismiss) { Text("取消") }
        },
    )
}

private val WALLET_FILTER_GROUPS = listOf(
    "all" to "全部",
    "income" to "收入",
    "expense" to "支出",
    "redpack" to "红包",
    "guess" to "猜词",
    "card" to "抽卡",
    "farm" to "农场",
    "other" to "其他",
)

private fun walletRowGroup(kind: String): String {
    val k = kind.lowercase()
    return when {
        k.contains("redpack") || k.contains("hb") || k.contains("packet") -> "redpack"
        k.contains("guess") -> "guess"
        k.contains("card") || k.contains("52b") || k.contains("draw") -> "card"
        k.contains("farm") || k.contains("pet") || k.contains("crop") -> "farm"
        else -> "other"
    }
}

@Composable
fun WalletDetailDialog(
    api: ForHappyApiClient,
    initialWallet: WalletSummary?,
    onOpenWebRoute: (route: String, title: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalForHappyPalette.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var wallet by remember(initialWallet) { mutableStateOf(initialWallet) }
    var rows by remember(initialWallet) { mutableStateOf(initialWallet?.rows.orEmpty()) }
    var nextCursor by remember(initialWallet) { mutableStateOf(initialWallet?.next) }
    var loading by remember { mutableStateOf(initialWallet == null) }
    var loadingMore by remember { mutableStateOf(false) }
    var activeFilter by remember { mutableStateOf("all") }

    var hbStatus by remember { mutableStateOf(HbKeyStatus()) }
    var hbRevealedPlain by remember { mutableStateOf<String?>(null) }
    var hbBusy by remember { mutableStateOf(false) }
    var confirmRevokeKey by remember { mutableStateOf(false) }
    var bannerText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val fresh = api.fetchWallet()
        if (fresh != null) {
            wallet = fresh
            rows = fresh.rows
            nextCursor = fresh.next
        }
        loading = false
        hbStatus = api.fetchHbKeyStatus()
    }

    val monthStartSec = remember {
        Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis / 1000L
    }
    val monthIncome = remember(rows, monthStartSec) {
        rows.filter { it.createdAt >= monthStartSec && it.delta > 0 }.sumOf { it.delta }.toLong()
    }
    val monthExpense = remember(rows, monthStartSec) {
        rows.filter { it.createdAt >= monthStartSec && it.delta < 0 }.sumOf { -it.delta }.toLong()
    }

    val filteredRows = remember(rows, activeFilter) {
        rows.filter { row ->
            when (activeFilter) {
                "all" -> true
                "income" -> row.delta > 0.0
                "expense" -> row.delta < 0.0
                else -> walletRowGroup(row.kind) == activeFilter
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = FullscreenDialogProperties,
    ) {
        ConfigureFullscreenDialogWindow(darkStatusIcons = !palette.isDark)
        DialogBackOwnerScope {
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
                        .height(ForHappyDimens.topBarHeight)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回", tint = palette.text)
                    }
                    Text(
                        "D 币钱包与密钥",
                        color = palette.text,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedButton(
                        onClick = { onOpenWebRoute("/leaderboard", "财富榜") },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Icon(Icons.Rounded.EmojiEvents, contentDescription = null, modifier = Modifier.size(15.dp), tint = palette.gold)
                        Spacer(Modifier.width(4.dp))
                        Text("财富榜", fontSize = 12.sp)
                    }
                }
            }
            HorizontalDivider(
                color = if (palette.isModernUi) palette.glassBorder else palette.line,
                thickness = if (palette.isModernUi) 0.5.dp else 1.dp,
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 14.dp,
                    top = 14.dp,
                    end = 14.dp,
                    bottom = 14.dp + navBottomPad,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                bannerText?.let { msg ->
                    item(key = "banner") {
                        Surface(
                            color = palette.primary.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Text(msg, color = palette.primary, fontSize = 12.sp, modifier = Modifier.fillMaxWidth().padding(10.dp))
                        }
                    }
                }

                // Balance Card
                item(key = "balance") {
                    Surface(
                        color = palette.surface,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (palette.isModernUi && palette.cardBorder == Color.Transparent) {
                                    Modifier
                                } else {
                                    Modifier.border(
                                        1.dp,
                                        if (palette.isModernUi) palette.cardBorder else palette.line,
                                        RoundedCornerShape(16.dp),
                                    )
                                },
                            ),
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("当前余额", color = palette.muted, fontSize = 12.sp)
                            Text(
                                "${NumberFormat.getNumberInstance(Locale.CHINA).format(wallet?.resolvedCoins ?: 0L)} D币",
                                color = palette.gold,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                style = TextStyle(fontFeatureSettings = TABULAR_FONT_FEATURE),
                            )
                            Text(
                                "本月已加载流水：+$monthIncome / -$monthExpense D币",
                                color = palette.muted,
                                fontSize = 12.sp,
                            )
                        }
                    }
                }

                // 8-group ledger filter
                item(key = "filters") {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        WALLET_FILTER_GROUPS.forEach { (id, label) ->
                            FilterChip(
                                selected = activeFilter == id,
                                onClick = { activeFilter = id },
                                label = { Text(label, fontSize = 12.sp) },
                            )
                        }
                    }
                }

                // Redpack key management
                item(key = "hb_key") {
                    Surface(
                        color = palette.surface,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (palette.isModernUi && palette.cardBorder == Color.Transparent) {
                                    Modifier
                                } else {
                                    Modifier.border(
                                        1.dp,
                                        if (palette.isModernUi) palette.cardBorder else palette.line,
                                        RoundedCornerShape(16.dp),
                                    )
                                },
                            ),
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Rounded.Key, contentDescription = null, tint = palette.primary, modifier = Modifier.size(18.dp))
                                Text("红包站密钥", color = palette.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                BadgePill(
                                    text = if (hbStatus.bound) "已绑定" else "未生成",
                                    containerColor = if (hbStatus.bound) palette.primary.copy(alpha = 0.14f) else palette.surface2,
                                    contentColor = if (hbStatus.bound) palette.primary else palette.muted,
                                )
                            }
                            if (hbStatus.bound) {
                                Text(
                                    text = "密钥 ID：${hbStatus.keyId ?: "--"}",
                                    color = palette.muted,
                                    fontSize = 12.sp,
                                )
                                hbRevealedPlain?.let { plain ->
                                    Surface(color = palette.surface2, shape = RoundedCornerShape(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text(plain, color = palette.text, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                            IconButton(
                                                onClick = {
                                                    clipboard.setText(AnnotatedString(plain))
                                                    bannerText = "密钥已复制到剪贴板"
                                                },
                                                modifier = Modifier.size(28.dp),
                                            ) {
                                                Icon(Icons.Rounded.ContentCopy, contentDescription = "复制", tint = palette.primary, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        enabled = !hbBusy,
                                        onClick = {
                                            hbBusy = true
                                            scope.launch {
                                                val res = api.postHbKeyAction("reveal", hbStatus.keyId)
                                                hbBusy = false
                                                res.fold(
                                                    onSuccess = { st ->
                                                        hbStatus = st
                                                        hbRevealedPlain = st.revealedKey
                                                        if (st.revealedKey.isNullOrBlank()) bannerText = "服务端未返回明文密钥"
                                                    },
                                                    onFailure = { bannerText = it.message },
                                                )
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Icon(Icons.Rounded.Visibility, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("查看明文", fontSize = 12.sp)
                                    }
                                    OutlinedButton(
                                        enabled = !hbBusy,
                                        onClick = { confirmRevokeKey = true },
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Text("收回密钥", color = palette.danger, fontSize = 12.sp)
                                    }
                                }
                            } else {
                                Button(
                                    enabled = !hbBusy,
                                    onClick = {
                                        hbBusy = true
                                        scope.launch {
                                            val res = api.postHbKeyAction("generate")
                                            hbBusy = false
                                            res.fold(
                                                onSuccess = { st ->
                                                    hbStatus = st
                                                    hbRevealedPlain = st.revealedKey
                                                    bannerText = "新密钥已生成，请妥善保存"
                                                },
                                                onFailure = { bannerText = it.message },
                                            )
                                        }
                                    },
                                ) {
                                    Text(if (hbBusy) "生成中…" else "生成并绑定红包站密钥", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                if (loading) {
                    item(key = "loading") {
                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = palette.primary)
                        }
                    }
                } else if (filteredRows.isEmpty()) {
                    item(key = "empty") {
                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text("该分类下暂无流水记录", color = palette.muted, fontSize = 13.sp)
                        }
                    }
                } else {
                    items(filteredRows) { row: WalletLedgerRow ->
                        val positive = row.delta > 0
                        Surface(
                            color = palette.surface,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, palette.line, RoundedCornerShape(12.dp)),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(row.kind.ifBlank { "账目变更" }, color = palette.text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Text(formatMeTime(row.createdAt), color = palette.muted, fontSize = 11.sp)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${if (positive) "+" else ""}${row.delta.toLong()}",
                                        color = if (positive) palette.primary else palette.danger,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        style = TextStyle(fontFeatureSettings = TABULAR_FONT_FEATURE),
                                    )
                                    Text(
                                        text = "余额 ${row.balanceAfter.toLong()}",
                                        color = palette.muted,
                                        fontSize = 11.sp,
                                    )
                                }
                            }
                        }
                    }

                    if (!nextCursor.isNullOrBlank()) {
                        item(key = "more") {
                            OutlinedButton(
                                enabled = !loadingMore,
                                onClick = {
                                    loadingMore = true
                                    scope.launch {
                                        val page = api.fetchWallet(beforeCursor = nextCursor)
                                        loadingMore = false
                                        if (page != null) {
                                            rows = rows + page.rows
                                            nextCursor = page.next
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(if (loadingMore) "加载中…" else "加载更多流水")
                            }
                        }
                    }
                }
            }
        }
        }
    }

    if (confirmRevokeKey) {
        AlertDialog(
            onDismissRequest = { confirmRevokeKey = false },
            containerColor = palette.surface,
            titleContentColor = palette.text,
            textContentColor = palette.text,
            title = { Text("确认收回密钥？") },
            text = { Text("收回后当前绑定的红包站密钥将立即失效，需重新生成方可使用。") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = palette.danger, contentColor = Color.White),
                    onClick = {
                        confirmRevokeKey = false
                        hbBusy = true
                        scope.launch {
                            val res = api.postHbKeyAction("revoke", hbStatus.keyId)
                            hbBusy = false
                            res.fold(
                                onSuccess = {
                                    hbStatus = HbKeyStatus(bound = false)
                                    hbRevealedPlain = null
                                    bannerText = "密钥已收回"
                                },
                                onFailure = { bannerText = it.message },
                            )
                        }
                    },
                ) { Text("确认收回") }
            },
            dismissButton = {
                TextButton(onClick = { confirmRevokeKey = false }) { Text("取消") }
            },
        )
    }
}

@Composable
fun Cards52bDialog(
    api: ForHappyApiClient,
    initialPacks: List<Owner52bPack>,
    onOpenWebRoute: (route: String, title: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalForHappyPalette.current
    var packs by remember(initialPacks) { mutableStateOf(initialPacks) }
    var loading by remember { mutableStateOf(initialPacks.isEmpty()) }
    var selectedPackIdx by remember { mutableIntStateOf(0) }
    var detailCard by remember { mutableStateOf<Owner52bCard?>(null) }

    LaunchedEffect(Unit) {
        val loaded = api.fetchOwner52bPacks()
        if (loaded.isNotEmpty() || packs.isEmpty()) packs = loaded
        loading = false
    }

    val safeIdx = selectedPackIdx.coerceIn(0, (packs.size - 1).coerceAtLeast(0))
    val activePack = packs.getOrNull(safeIdx)
    val cards = activePack?.cards.orEmpty()
    val totalCount = activePack?.let { maxOf(it.total, cards.size) } ?: 0
    val ownedCount = activePack?.let { if (cards.isNotEmpty()) cards.count { c -> c.owned } else it.owned } ?: 0
    val ssrCards = cards.filter { it.tier.orEmpty().equals("SSR", ignoreCase = true) }
    val ssrOwned = ssrCards.count { it.owned }

    Dialog(
        onDismissRequest = {
            if (detailCard != null) detailCard = null else onDismiss()
        },
        properties = FullscreenDialogProperties,
    ) {
        ConfigureFullscreenDialogWindow(darkStatusIcons = !palette.isDark)
        DialogBackOwnerScope {
            BackHandler(enabled = detailCard != null) {
                detailCard = null
            }
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
                            .height(ForHappyDimens.topBarHeight)
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回", tint = palette.text)
                        }
                        Text("52B 集卡图鉴", color = palette.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Button(
                            onClick = {
                                onDismiss()
                                onOpenWebRoute("/cards", "52B 抽卡")
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        ) {
                            Text("去抽卡 →", fontSize = 12.sp)
                        }
                    }
                }
                HorizontalDivider(
                    color = if (palette.isModernUi) palette.glassBorder else palette.line,
                    thickness = if (palette.isModernUi) 0.5.dp else 1.dp,
                )

                if (packs.size > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        packs.forEachIndexed { idx, p ->
                            FilterChip(
                                selected = idx == safeIdx,
                                onClick = { selectedPackIdx = idx },
                                label = { Text("${p.name} (${p.owned}/${maxOf(p.total, p.cards.size)})", fontSize = 12.sp) },
                            )
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 14.dp,
                        top = 14.dp,
                        end = 14.dp,
                        bottom = 14.dp + navBottomPad,
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item(key = "progress") {
                        Surface(
                            color = palette.surface,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (palette.isModernUi && palette.cardBorder == Color.Transparent) {
                                        Modifier
                                    } else {
                                        Modifier.border(
                                            1.dp,
                                            if (palette.isModernUi) palette.cardBorder else palette.line,
                                            RoundedCornerShape(16.dp),
                                        )
                                    },
                                ),
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(
                                        text = "${activePack?.name ?: "收集进度"} · $ownedCount / $totalCount",
                                        color = palette.text,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    if (ssrCards.isNotEmpty()) {
                                        Text("SSR $ssrOwned/${ssrCards.size}", color = palette.gold, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                                val frac = if (totalCount <= 0) 0f else (ownedCount.toFloat() / totalCount.toFloat()).coerceIn(0f, 1f)
                                LinearProgressIndicator(
                                    progress = { frac },
                                    modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(999.dp)),
                                    color = palette.primary,
                                    trackColor = palette.surface2,
                                )
                            }
                        }
                    }

                    if (loading) {
                        item(key = "loading") {
                            Box(modifier = Modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = palette.primary)
                            }
                        }
                    } else if (cards.isEmpty()) {
                        item(key = "empty") {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("暂无卡包数据，点击右上角去抽卡试试", color = palette.muted, fontSize = 13.sp)
                            }
                        }
                    } else {
                        items(cards.chunked(2)) { rowCards ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                rowCards.forEach { card ->
                                    val artUrl = card.asset?.takeIf { it.isNotBlank() }?.let { MediaUrlRewriter.rewriteMediaUrl(it) }
                                    Surface(
                                        color = if (card.owned) palette.surface else palette.surface2.copy(alpha = 0.55f),
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .border(
                                                1.dp,
                                                if (card.owned) palette.primary.copy(alpha = 0.35f) else palette.line,
                                                RoundedCornerShape(14.dp),
                                            )
                                            .clickable { detailCard = card },
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .aspectRatio(0.75f)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(palette.surface2),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                if (!artUrl.isNullOrBlank()) {
                                                    AsyncImage(
                                                        model = artUrl,
                                                        imageLoader = api.imageLoader,
                                                        contentDescription = card.name,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .alpha(if (card.owned) 1f else 0.42f),
                                                    )
                                                } else {
                                                    Text(
                                                        text = if (card.owned) card.icon?.ifBlank { "🃏" } ?: "🃏" else "🔒",
                                                        fontSize = 28.sp,
                                                    )
                                                }
                                                if (!card.owned) {
                                                    Surface(
                                                        color = Color.Black.copy(alpha = 0.55f),
                                                        shape = RoundedCornerShape(999.dp),
                                                        modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp),
                                                    ) {
                                                        Text("待收集", color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp))
                                                    }
                                                } else if (card.count > 1) {
                                                    Surface(
                                                        color = palette.primary.copy(alpha = 0.88f),
                                                        shape = RoundedCornerShape(999.dp),
                                                        modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp),
                                                    ) {
                                                        Text("×${card.count}", color = palette.onPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                                    }
                                                }
                                                card.tier?.takeIf { it.isNotBlank() }?.let { tier ->
                                                    Box(modifier = Modifier.align(Alignment.TopStart).padding(6.dp)) {
                                                        BadgePill(
                                                            text = tier.uppercase(),
                                                            containerColor = if (tier.equals("SSR", true)) palette.gold.copy(alpha = 0.92f) else palette.surface.copy(alpha = 0.9f),
                                                            contentColor = if (tier.equals("SSR", true)) Color(0xFF3A2400) else palette.text,
                                                        )
                                                    }
                                                }
                                            }
                                            Text(
                                                text = card.name.ifBlank { "卡片 #${card.id}" },
                                                color = if (card.owned) palette.text else palette.muted,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            card.desc?.takeIf { it.isNotBlank() }?.let { desc ->
                                                Text(desc, color = palette.muted, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                            }
                                        }
                                    }
                                }
                                if (rowCards.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }

    detailCard?.let { card ->
        val artUrl = card.asset?.takeIf { it.isNotBlank() }?.let { MediaUrlRewriter.rewriteMediaUrl(it) }
        AlertDialog(
            onDismissRequest = { detailCard = null },
            containerColor = palette.surface,
            titleContentColor = palette.text,
            textContentColor = palette.text,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(card.name.ifBlank { "卡片详情" }, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    card.tier?.takeIf { it.isNotBlank() }?.let { tier ->
                        BadgePill(
                            text = tier.uppercase(),
                            containerColor = if (tier.equals("SSR", true)) palette.gold.copy(alpha = 0.2f) else palette.surface2,
                            contentColor = if (tier.equals("SSR", true)) palette.gold else palette.muted,
                        )
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!artUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = artUrl,
                            imageLoader = api.imageLoader,
                            contentDescription = card.name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(palette.surface2),
                        )
                    }
                    card.desc?.takeIf { it.isNotBlank() }?.let {
                        Text(it, color = palette.text, fontSize = 13.sp)
                    }
                    Text(
                        text = buildString {
                            append("卡包：${card.packName.ifBlank { "52B 巡展" }}")
                            append(" · ")
                            append(if (card.owned) "已拥有 ${card.count} 张" else "尚未获得")
                        },
                        color = palette.muted,
                        fontSize = 12.sp,
                    )
                    card.firstAt?.takeIf { it > 0L }?.let { ts ->
                        val dateText = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(Date(ts * 1000L))
                        Text("首次获得：$dateText", color = palette.muted, fontSize = 11.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { detailCard = null }) { Text("关闭") }
            },
        )
    }
}

private data class OwnedRaffleEntry(
    val id: String,
    val name: String,
    val level: Int,
    val rarity: String,
    val permLevel: Int = 0,
    val expiresAt: Long = 0L,
    val isTrial: Boolean = false,
)

private data class OwnedPersonalEntry(
    val id: String,
    val name: String,
    val rarity: String,
    val art: String = "v1",
)

private val NICK_GRADIENT_OPTIONS = listOf(
    Triple("", "默认纯色", emptyList<String>()),
    Triple("sakura", "樱粉", listOf("#ff5fa2", "#ffb3d1", "#c86bff")),
    Triple("salt", "海盐", listOf("#00b4d8", "#48cae4", "#90e0ef")),
    Triple("sunset", "日落", listOf("#ff6b35", "#f7c548", "#ff3864")),
    Triple("pine", "森林", listOf("#2d6a4f", "#52b788", "#b7e4c7")),
    Triple("cyber", "赛博", listOf("#f72585", "#7209b7", "#4cc9f0")),
    Triple("blackgold", "黑金", listOf("#111111", "#d4a017", "#fff3b0")),
)

private fun parseOwnedRaffleTitles(obj: JsonObject?, allCosmetics: Boolean): List<OwnedRaffleEntry> {
    if (allCosmetics) {
        return BadgeAndCosmeticRegistry.allRaffleTitles().values.map {
            OwnedRaffleEntry(id = it.id, name = it.name, level = BadgeAndCosmeticRegistry.TITLE_MAX_LEVEL, rarity = it.rarity, permLevel = BadgeAndCosmeticRegistry.TITLE_MAX_LEVEL)
        }
    }
    val arr = runCatching { (obj?.get("titles") ?: obj?.get("items"))?.jsonArray }.getOrNull() ?: return emptyList()
    return arr.mapNotNull { el ->
        when (el) {
            is JsonPrimitive -> {
                val id = el.contentOrNull?.trim().orEmpty()
                if (id.isEmpty()) null
                else {
                    val cat = BadgeAndCosmeticRegistry.resolveRaffleTitle(id)
                    OwnedRaffleEntry(id = id, name = cat?.name ?: id, level = 1, rarity = cat?.rarity ?: "normal", permLevel = 1)
                }
            }
            is JsonObject -> {
                val enabled = el["enabled"]?.jsonPrimitive?.booleanOrNull ?: true
                if (!enabled) return@mapNotNull null
                val id = (el["id"]?.jsonPrimitive?.contentOrNull ?: el["title"]?.jsonPrimitive?.contentOrNull)?.trim().orEmpty()
                if (id.isEmpty()) return@mapNotNull null
                val cat = BadgeAndCosmeticRegistry.resolveRaffleTitle(id)
                val permLv = el["permanent_level"]?.jsonPrimitive?.intOrNull ?: 0
                val fxp = el["expires_at"]?.jsonPrimitive?.longOrNull ?: 0L
                val lv = el["level"]?.jsonPrimitive?.intOrNull
                    ?: permLv.takeIf { it > 0 }
                    ?: 1
                val name = el["name"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() } ?: cat?.name ?: id
                val rarity = el["rarity"]?.jsonPrimitive?.contentOrNull ?: cat?.rarity ?: "normal"
                val isTrial = el["trial"]?.jsonPrimitive?.booleanOrNull == true || (fxp > 0L && permLv <= 0)
                OwnedRaffleEntry(
                    id = id,
                    name = name,
                    level = lv.coerceAtLeast(1),
                    rarity = rarity,
                    permLevel = permLv,
                    expiresAt = fxp,
                    isTrial = isTrial,
                )
            }
            else -> null
        }
    }
}

private fun parseOwnedPersonalTitles(obj: JsonObject?, allCosmetics: Boolean): List<OwnedPersonalEntry> {
    if (allCosmetics) {
        return BadgeAndCosmeticRegistry.allPersonalTitles().values.map {
            OwnedPersonalEntry(id = it.id, name = it.name, rarity = it.rarity, art = it.art)
        }
    }
    val arr = runCatching { (obj?.get("titles") ?: obj?.get("items"))?.jsonArray }.getOrNull() ?: return emptyList()
    return arr.mapNotNull { el ->
        when (el) {
            is JsonPrimitive -> {
                val id = el.contentOrNull?.trim().orEmpty()
                if (id.isEmpty()) null
                else {
                    val cat = BadgeAndCosmeticRegistry.resolvePersonalTitle(id)
                    OwnedPersonalEntry(id = id, name = cat?.name ?: id, rarity = cat?.rarity ?: "normal", art = cat?.art ?: "v1")
                }
            }
            is JsonObject -> {
                val id = (el["id"]?.jsonPrimitive?.contentOrNull ?: el["title"]?.jsonPrimitive?.contentOrNull)?.trim().orEmpty()
                if (id.isEmpty()) return@mapNotNull null
                val cat = BadgeAndCosmeticRegistry.resolvePersonalTitle(id)
                val name = el["name"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() } ?: cat?.name ?: id
                val rarity = el["rarity"]?.jsonPrimitive?.contentOrNull ?: cat?.rarity ?: "normal"
                OwnedPersonalEntry(id = id, name = name, rarity = rarity, art = cat?.art ?: "v1")
            }
            else -> null
        }
    }
}

private fun formatTrialRemaining(expiresAtSec: Long): String {
    val diff = (expiresAtSec - ServerClock.serverNowSec()).coerceAtLeast(0L)
    if (diff <= 0L) return "体验卡已过期"
    val days = diff / 86400L
    val hours = (diff % 86400L) / 3600L
    val mins = (diff % 3600L) / 60L
    return when {
        days > 0 -> "体验卡剩余 ${days}天${hours}小时"
        hours > 0 -> "体验卡剩余 ${hours}小时${mins}分"
        else -> "体验卡剩余 ${mins.coerceAtLeast(1)}分钟"
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DecoStudioDialog(
    api: ForHappyApiClient,
    me: MeUser,
    initialCleanRaw: String?,
    onSaved: () -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalForHappyPalette.current
    val scope = rememberCoroutineScope()
    var liveMe by remember(me.id) { mutableStateOf(me) }
    val allCosmetics = liveMe.cosmeticAccess == "all"

    var config by remember(me.id) { mutableStateOf(parseDecoConfig(me.deco, initialCleanRaw)) }
    var ownedRaffles by remember { mutableStateOf<List<OwnedRaffleEntry>>(emptyList()) }
    var ownedPersonals by remember { mutableStateOf<List<OwnedPersonalEntry>>(emptyList()) }
    var ownedCommunityIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var activeTab by remember { mutableIntStateOf(0) } // 0=称号套装 1=炫彩昵称 2=徽章墙与展柜 3=气泡与社区 4=洁癖屏蔽
    var previewExpanded by remember { mutableStateOf(true) }
    var syncingCatalog by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var bannerText by remember { mutableStateOf<String?>(null) }

    val cosmeticCatalogRev by BadgeAndCosmeticRegistry.catalogRevision.collectAsState()

    val ownedBadges = remember(liveMe.badges, liveMe.farmBadgeProgress) {
        BadgeAndCosmeticRegistry.parseOwnedBadges(liveMe.badges, liveMe.farmBadgeProgress)
    }
    val badgeWall = remember(liveMe.badges, liveMe.farmBadgeProgress, allCosmetics) {
        BadgeAndCosmeticRegistry.buildBadgeWall(
            badgesJson = liveMe.badges,
            farmProgress = liveMe.farmBadgeProgress,
            allCosmetics = allCosmetics,
        )
    }
    val unlockedBadgeCount = remember(badgeWall) { badgeWall.count { it.owned } }
    val communitySkins = remember(cosmeticCatalogRev) {
        BadgeAndCosmeticRegistry.allCommunitySkins().values.toList()
    }
    val unlockKeys = remember(liveMe.unlocks) {
        BadgeAndCosmeticRegistry.parseUnlockKeys(liveMe.unlocks)
    }
    val unlockedGrowths = remember(cosmeticCatalogRev, liveMe.unlocks, allCosmetics) {
        BadgeAndCosmeticRegistry.allUnlockedGrowthRewards(
            unlocksJson = liveMe.unlocks,
            allCosmetics = allCosmetics,
        ).sortedWith(compareBy({ it.series }, { it.level }))
    }
    val (showcaseSelected, showcaseAvailable) = remember(
        liveMe.badges,
        liveMe.officialTitles,
        config.showcaseKeys,
        cosmeticCatalogRev,
    ) {
        BadgeAndCosmeticRegistry.resolveShowcaseItems(
            badgesJson = liveMe.badges,
            officialTitles = liveMe.officialTitles,
            showcaseKeys = config.showcaseKeys,
        )
    }

    suspend fun syncAllCatalogs(forceWeb: Boolean) {
        syncingCatalog = true
        coroutineScope {
            val skinsJob = async { runCatching { api.fetchCommunitySkinsCatalog() } }
            val ownedCommJob = async { runCatching { api.fetchOwnedCommunityBubbles() }.getOrDefault(emptySet()) }
            val webJob = async { runCatching { api.syncWebCosmeticCatalog(force = forceWeb) } }
            val raffleJob = async { runCatching { api.fetchMyRaffleTitles() }.getOrNull() }
            val personalJob = async { runCatching { api.fetchMyPersonalTitles() }.getOrNull() }
            val meJob = async { runCatching { api.fetchMe() }.getOrNull() }
            skinsJob.await()
            webJob.await()
            ownedCommunityIds = ownedCommJob.await()
            val refreshedMe = meJob.await()
            if (refreshedMe != null) liveMe = refreshedMe
            val curAll = (refreshedMe ?: liveMe).cosmeticAccess == "all"
            ownedRaffles = parseOwnedRaffleTitles(raffleJob.await(), curAll)
            ownedPersonals = parseOwnedPersonalTitles(personalJob.await(), curAll)
        }
        syncingCatalog = false
    }

    LaunchedEffect(me.id) {
        syncAllCatalogs(forceWeb = false)
    }

    val previewDecoJson = remember(liveMe.deco, config, ownedBadges) {
        BadgeAndCosmeticRegistry.buildServerDecoPayload(liveMe.deco, config, ownedBadges)
    }
    val previewCardIdentity = remember(
        previewDecoJson,
        liveMe.badges,
        liveMe.officialTitles,
        liveMe.isAdmin,
        liveMe.isFancyGranted,
        liveMe.cosmeticAccess,
        palette.isDark,
        cosmeticCatalogRev,
    ) {
        BadgeAndCosmeticRegistry.resolveEquippedIdentity(
            decoJson = previewDecoJson,
            badgesJson = liveMe.badges,
            officialTitles = liveMe.officialTitles,
            where = "card",
            isAdmin = liveMe.isAdmin,
            hasFancyPermission = liveMe.isFancyGranted,
            cosmeticAccess = liveMe.cosmeticAccess,
            isDarkTheme = palette.isDark,
        )
    }
    val previewChatIdentity = remember(
        previewDecoJson,
        liveMe.badges,
        liveMe.officialTitles,
        liveMe.isAdmin,
        liveMe.isFancyGranted,
        liveMe.cosmeticAccess,
        palette.isDark,
        cosmeticCatalogRev,
    ) {
        BadgeAndCosmeticRegistry.resolveEquippedIdentity(
            decoJson = previewDecoJson,
            badgesJson = liveMe.badges,
            officialTitles = liveMe.officialTitles,
            where = "chat",
            isAdmin = liveMe.isAdmin,
            hasFancyPermission = liveMe.isFancyGranted,
            cosmeticAccess = liveMe.cosmeticAccess,
            isDarkTheme = palette.isDark,
        )
    }

    val titleColor = remember(config.primaryHex, palette.primary) { parseHexColor(config.primaryHex, palette.primary) }
    val nickBrushColors = remember(previewCardIdentity.nickGradientHexes, palette.text) {
        previewCardIdentity.nickGradientHexes.map { parseSafeHex(it, palette.text) }.takeIf { it.size >= 2 }
    }

    fun clearExclusiveTitleSlots(base: DecoConfig): DecoConfig = base.copy(
        equippedPersonalId = "",
        equippedRaffleId = "",
        equippedRaffleLevel = 1,
        equippedRaffleDisplayLevel = 0,
        equippedRafflePermLevel = 0,
        equippedRaffleExpiresAt = 0L,
        equippedOfficialId = "",
        equippedGrowthId = "",
        equippedInviteTitle = false,
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = FullscreenDialogProperties,
    ) {
        ConfigureFullscreenDialogWindow(darkStatusIcons = !palette.isDark)
        DialogBackOwnerScope {
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
                            .height(ForHappyDimens.topBarHeight)
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回", tint = palette.text)
                        }
                        Text("装扮工作室", color = palette.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        IconButton(
                            enabled = !syncingCatalog,
                            onClick = {
                                scope.launch {
                                    syncAllCatalogs(forceWeb = true)
                                    bannerText = "已从站端拉取最新装扮目录"
                                }
                            },
                        ) {
                            if (syncingCatalog) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = palette.primary)
                            } else {
                                Icon(Icons.Rounded.Refresh, contentDescription = "刷新目录", tint = palette.muted, modifier = Modifier.size(18.dp))
                            }
                        }
                        TextButton(
                            enabled = !saving,
                            onClick = {
                                saving = true
                                scope.launch {
                                    val res = api.updateMeDeco(null)
                                    val cleaned = DecoConfig(
                                        cleanHideOthersNick = config.cleanHideOthersNick,
                                        cleanHideOthersBadges = config.cleanHideOthersBadges,
                                        cleanHideOthersStyles = config.cleanHideOthersStyles,
                                        cleanHideBubbles = config.cleanHideBubbles,
                                    )
                                    api.putPref(KV_DECO_CLEAN, ForHappyJson.encodeToString(DecoConfig.serializer(), cleaned))
                                    saving = false
                                    res.fold(
                                        onSuccess = {
                                            config = cleaned
                                            bannerText = "已清空全部装扮"
                                            onSaved()
                                        },
                                        onFailure = { err -> bannerText = err.message ?: "清空失败" },
                                    )
                                }
                            },
                        ) {
                            Text("清空", color = palette.muted, fontSize = 12.sp)
                        }
                        Button(
                            enabled = !saving,
                            onClick = {
                                saving = true
                                scope.launch {
                                    val payload = BadgeAndCosmeticRegistry.buildServerDecoPayload(
                                        originalDeco = liveMe.deco,
                                        config = config,
                                        ownedBadges = ownedBadges,
                                    )
                                    val res = api.updateMeDeco(payload)
                                    api.putPref(KV_DECO_CLEAN, ForHappyJson.encodeToString(DecoConfig.serializer(), config))
                                    saving = false
                                    res.fold(
                                        onSuccess = {
                                            bannerText = "装扮已保存同步！"
                                            onSaved()
                                        },
                                        onFailure = { err -> bannerText = err.message ?: "保存失败" },
                                    )
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                        ) {
                            Text(if (saving) "保存中…" else "保存装扮", fontSize = 12.sp)
                        }
                    }
                }
                HorizontalDivider(
                    color = if (palette.isModernUi) palette.glassBorder else palette.line,
                    thickness = if (palette.isModernUi) 0.5.dp else 1.dp,
                )

                // Collapsible 4-in-1 Live Preview Panel
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.surface)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { previewExpanded = !previewExpanded },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "四合一实时预览",
                            color = palette.text,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "署名卡片 · 群聊头 · 气泡 · +1框",
                            color = palette.muted,
                            fontSize = 11.sp,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            imageVector = if (previewExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = if (previewExpanded) "收起预览" else "展开预览",
                            tint = palette.muted,
                            modifier = Modifier.size(18.dp),
                        )
                    }

                    AnimatedVisibility(visible = previewExpanded) {
                        val singleNickColor = previewCardIdentity.nickGradientHexes.firstOrNull()?.let {
                            parseSafeHex(it, palette.text)
                        }
                        val previewNickPlatform = PlatformTextStyle(includeFontPadding = false)
                        val nickStyle = when {
                            nickBrushColors != null && !config.cleanHideOthersNick -> TextStyle(
                                brush = Brush.horizontalGradient(nickBrushColors),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                lineHeight = 16.sp,
                                platformStyle = previewNickPlatform,
                            )
                            singleNickColor != null && !config.cleanHideOthersNick -> TextStyle(
                                color = singleNickColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                lineHeight = 16.sp,
                                platformStyle = previewNickPlatform,
                            )
                            else -> TextStyle(
                                color = palette.text,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                lineHeight = 16.sp,
                                platformStyle = previewNickPlatform,
                            )
                        }
                        val previewBubbleStyle = remember(
                            previewDecoJson,
                            liveMe.cosmeticAccess,
                            liveMe.username,
                            config.cleanHideBubbles,
                            palette.isDark,
                            cosmeticCatalogRev,
                        ) {
                            ChatBubbleCosmetics.resolveBubbleFromDeco(
                                deco = previewDecoJson,
                                cosmeticAccess = liveMe.cosmeticAccess,
                                userKey = liveMe.username,
                                where = "chat",
                                frame = false,
                                isDarkTheme = palette.isDark,
                                hideBubble = config.cleanHideBubbles,
                            )
                        }
                        val previewFrameStyle = remember(
                            previewDecoJson,
                            liveMe.cosmeticAccess,
                            liveMe.username,
                            config.cleanHideBubbles,
                            palette.isDark,
                            cosmeticCatalogRev,
                        ) {
                            ChatBubbleCosmetics.resolveBubbleFromDeco(
                                deco = previewDecoJson,
                                cosmeticAccess = liveMe.cosmeticAccess,
                                userKey = liveMe.username,
                                where = "chat",
                                frame = true,
                                isDarkTheme = palette.isDark,
                                hideBubble = config.cleanHideBubbles,
                            )
                        }
                        val bubbleName = BadgeAndCosmeticRegistry.resolveCommunitySkinMeta(config.communityBubbleId)?.let { "社区·${it.name}" }
                            ?: BadgeAndCosmeticRegistry.BUBBLE_PRESETS.firstOrNull { it.first == config.bubbleSkin }?.second
                            ?: if (config.bubbleSkin.isBlank() || config.bubbleSkin == "default") "默认" else config.bubbleSkin
                        val replyName = BadgeAndCosmeticRegistry.resolveCommunitySkinMeta(config.communityReplyId)?.name

                        Surface(
                            color = palette.surface2,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, palette.line),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                // 1. Profile Card & Showcase
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    ForHappyAvatar(liveMe.displayName, liveMe.avatar, api.imageLoader, size = 32.dp)
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
                                        ) {
                                            Text(liveMe.displayName, style = nickStyle, modifier = Modifier.align(Alignment.CenterVertically))
                                            if (!config.cleanHideOthersStyles) {
                                                previewCardIdentity.title?.let {
                                                    TitleChipView(title = it, modifier = Modifier.align(Alignment.CenterVertically))
                                                }
                                            }
                                            if (!config.cleanHideOthersBadges) {
                                                previewCardIdentity.badge?.let {
                                                    BadgeChipView(
                                                        badge = it,
                                                        imageLoader = api.imageLoader,
                                                        mini = false,
                                                        modifier = Modifier.align(Alignment.CenterVertically),
                                                    )
                                                }
                                            }
                                        }
                                        if (showcaseSelected.isNotEmpty()) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                            ) {
                                                Text("展柜", color = palette.muted, fontSize = 10.sp)
                                                showcaseSelected.forEach { sc ->
                                                    BadgeArtIcon(
                                                        art = sc.chip.art,
                                                        level = sc.chip.level,
                                                        imageLoader = api.imageLoader,
                                                        size = 16.dp,
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                HorizontalDivider(color = palette.line.copy(alpha = 0.6f), thickness = 0.6.dp)

                                // 2. Chat Header Line
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
                                ) {
                                    Text(
                                        text = "[群聊]",
                                        color = palette.muted,
                                        fontSize = 10.5.sp,
                                        lineHeight = 13.sp,
                                        style = TextStyle(platformStyle = previewNickPlatform),
                                        modifier = Modifier.align(Alignment.CenterVertically),
                                    )
                                    Text(liveMe.displayName, style = nickStyle, modifier = Modifier.align(Alignment.CenterVertically))
                                    if (!config.cleanHideOthersStyles) {
                                        previewChatIdentity.title?.let {
                                            TitleChipView(title = it, modifier = Modifier.align(Alignment.CenterVertically))
                                        }
                                    }
                                    if (!config.cleanHideOthersBadges) {
                                        previewChatIdentity.badge?.let {
                                            BadgeChipView(
                                                badge = it,
                                                imageLoader = api.imageLoader,
                                                mini = true,
                                                modifier = Modifier.align(Alignment.CenterVertically),
                                            )
                                        }
                                    }
                                }

                                // 3. Bubble & 4. Reply/Boost Frame
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    ChatCosmeticBubbleBox(
                                        style = previewBubbleStyle,
                                        defaultContainerColor = palette.primary.copy(alpha = 0.14f),
                                        defaultBorderColor = palette.primary.copy(alpha = 0.35f),
                                        defaultShape = RoundedCornerShape(14.dp),
                                        defaultPadding = PaddingValues(horizontal = 11.dp, vertical = 7.dp),
                                        imageLoader = api.imageLoader,
                                        modifier = Modifier.weight(1f, fill = false),
                                    ) {
                                        Text(
                                            text = "摸鱼快乐！$bubbleName",
                                            color = previewBubbleStyle?.textColor ?: palette.text,
                                            fontSize = 12.5.sp,
                                            fontFamily = previewBubbleStyle?.fontFamily,
                                            fontWeight = previewBubbleStyle?.fontWeight,
                                            fontStyle = previewBubbleStyle?.fontStyle,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    ChatCosmeticBoostPill(
                                        style = previewFrameStyle,
                                        defaultContainerColor = palette.surface,
                                        defaultBorderColor = titleColor.copy(alpha = 0.4f),
                                        onClick = {},
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        ) {
                                            ForHappyAvatar(liveMe.displayName, liveMe.avatar, api.imageLoader, size = 14.dp)
                                            BoostPillText(
                                                text = if (!replyName.isNullOrBlank()) "+1 $replyName" else "+1 好图",
                                                imageLoader = api.imageLoader,
                                                fontSize = 10.5.sp,
                                                textColor = previewFrameStyle?.textColor,
                                                frameFillColor = previewFrameStyle?.fillColor,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                HorizontalDivider(
                    color = if (palette.isModernUi) palette.glassBorder else palette.line,
                    thickness = if (palette.isModernUi) 0.5.dp else 1.dp,
                )

                bannerText?.let { msg ->
                    Text(
                        msg,
                        color = palette.primary,
                        fontSize = 12.sp,
                        modifier = Modifier.fillMaxWidth().background(palette.primary.copy(alpha = 0.08f)).padding(horizontal = 14.dp, vertical = 6.dp),
                    )
                }

                // Bottom 5 Editor Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.surface)
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    listOf("称号套装", "炫彩昵称", "徽章墙 & 展柜 ($unlockedBadgeCount)", "气泡与社区", "洁癖屏蔽").forEachIndexed { idx, label ->
                        FilterChip(
                            selected = activeTab == idx,
                            onClick = { activeTab = idx },
                            label = { Text(label, fontSize = 12.sp) },
                        )
                    }
                }
                HorizontalDivider(
                    color = if (palette.isModernUi) palette.glassBorder else palette.line,
                    thickness = if (palette.isModernUi) 0.5.dp else 1.dp,
                )

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (activeTab == 2) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 14.dp,
                                top = 14.dp,
                                end = 14.dp,
                                bottom = 14.dp + navBottomPad,
                            ),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            item(key = "badge_mode_header") {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("昵称旁徽章搭配方式（最多展示 1 枚佩戴徽章）", color = palette.muted, fontSize = 12.sp)
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf(
                                            "set" to "跟随套装",
                                            "custom" to "自选单枚徽章",
                                            "none" to "不展示徽章",
                                        ).forEach { (modeKey, modeLabel) ->
                                            FilterChip(
                                                selected = config.badgeMode == modeKey,
                                                onClick = { config = config.copy(badgeMode = modeKey) },
                                                label = { Text(modeLabel, fontSize = 12.sp) },
                                            )
                                        }
                                    }

                                    // Profile Showcase (`showcase` max 6)
                                    if (showcaseAvailable.isNotEmpty()) {
                                        HorizontalDivider(color = palette.line.copy(alpha = 0.6f), thickness = 0.6.dp)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text(
                                                text = "主页徽章展柜 (${showcaseSelected.size}/6)",
                                                color = palette.text,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.weight(1f),
                                            )
                                            TextButton(
                                                onClick = { config = config.copy(showcaseKeys = null) },
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                            ) {
                                                Text("恢复默认", fontSize = 11.sp)
                                            }
                                            TextButton(
                                                onClick = { config = config.copy(showcaseKeys = emptyList()) },
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                            ) {
                                                Text("全部隐藏", color = palette.muted, fontSize = 11.sp)
                                            }
                                        }
                                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            val activeSet = showcaseSelected.map { it.key }.toSet()
                                            showcaseAvailable.forEach { sc ->
                                                val picked = sc.key in activeSet
                                                FilterChip(
                                                    selected = picked,
                                                    onClick = {
                                                        val curList = config.showcaseKeys ?: showcaseSelected.map { it.key }
                                                        val nextList = if (picked) {
                                                            curList.filter { it != sc.key }
                                                        } else {
                                                            (curList + sc.key).distinct().take(6)
                                                        }
                                                        config = config.copy(showcaseKeys = nextList)
                                                    },
                                                    leadingIcon = {
                                                        BadgeArtIcon(
                                                            art = sc.chip.art,
                                                            level = sc.chip.level,
                                                            imageLoader = api.imageLoader,
                                                            size = 16.dp,
                                                        )
                                                    },
                                                    label = { Text(sc.label, fontSize = 11.sp) },
                                                )
                                            }
                                        }
                                        HorizontalDivider(color = palette.line.copy(alpha = 0.6f), thickness = 0.6.dp)
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = "徽章墙 · 已点亮 $unlockedBadgeCount / ${badgeWall.size} 枚",
                                            color = palette.text,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                        if (config.equippedBadgeId.isNotBlank()) {
                                            TextButton(
                                                onClick = { config = config.copy(equippedBadgeId = "") },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                            ) {
                                                Text("卸下当前徽章", fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }

                            items(badgeWall, key = { "wall_badge_${it.id}" }) { item ->
                                val isEquipped = config.communityBadgeId.isBlank() &&
                                    config.badgeMode != "none" &&
                                    config.equippedBadgeId == item.id
                                BadgeWallCardRow(
                                    item = item,
                                    isEquipped = isEquipped,
                                    api = api,
                                    onToggleEquip = {
                                        if (item.owned) {
                                            config = if (isEquipped) {
                                                config.copy(equippedBadgeId = "")
                                            } else {
                                                config.copy(
                                                    equippedBadgeId = item.id,
                                                    badgeMode = "custom",
                                                    communityBadgeId = "",
                                                )
                                            }
                                        }
                                    },
                                )
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .imePadding()
                                .padding(start = 14.dp, top = 14.dp, end = 14.dp, bottom = 14.dp + navBottomPad),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            when (activeTab) {
                                0 -> {
                                    if (ownedRaffles.isNotEmpty()) {
                                        Text("已拥有抽奖称号套装（点选穿戴 / 再点卸下）", color = palette.muted, fontSize = 12.sp)
                                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            ownedRaffles.forEach { item ->
                                                val selected = config.equippedRaffleId == item.id
                                                val suffix = buildString {
                                                    if (item.level > 1) append(" Lv${item.level}")
                                                    if (item.isTrial) append(" · 体验")
                                                }
                                                FilterChip(
                                                    selected = selected,
                                                    onClick = {
                                                        config = if (selected) {
                                                            clearExclusiveTitleSlots(config)
                                                        } else {
                                                            clearExclusiveTitleSlots(config).copy(
                                                                equippedRaffleId = item.id,
                                                                equippedRaffleLevel = item.level,
                                                                equippedRaffleDisplayLevel = item.level,
                                                                equippedRafflePermLevel = item.permLevel,
                                                                equippedRaffleExpiresAt = item.expiresAt,
                                                                communityTitleId = "",
                                                            )
                                                        }
                                                    },
                                                    label = { Text("${item.name}$suffix", fontSize = 12.sp) },
                                                )
                                            }
                                        }

                                        if (config.equippedRaffleId.isNotBlank()) {
                                            val curEntry = ownedRaffles.firstOrNull { it.id == config.equippedRaffleId }
                                            val maxLv = maxOf(config.equippedRaffleLevel, curEntry?.level ?: 1)
                                            val expSec = config.equippedRaffleExpiresAt.takeIf { it > 0L } ?: (curEntry?.expiresAt ?: 0L)
                                            if (expSec > 0L) {
                                                Text(
                                                    text = formatTrialRemaining(expSec),
                                                    color = palette.primary,
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Medium,
                                                )
                                            }
                                            if (maxLv > 1) {
                                                val stageSteps = (BadgeAndCosmeticRegistry.TITLE_STAGES.filter { it <= maxLv } + maxLv).distinct().sorted()
                                                val curFl = config.equippedRaffleDisplayLevel.takeIf { it > 0 } ?: maxLv
                                                Text("展示等级阶段（实际等级 Lv$maxLv，可自由下调外观阶段）", color = palette.muted, fontSize = 11.5.sp)
                                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    stageSteps.forEach { lvOpt ->
                                                        FilterChip(
                                                            selected = curFl == lvOpt,
                                                            onClick = { config = config.copy(equippedRaffleDisplayLevel = lvOpt) },
                                                            label = { Text("Lv$lvOpt", fontSize = 11.sp) },
                                                        )
                                                    }
                                                }
                                            }
                                            if (config.equippedRaffleId == "chickengun") {
                                                HorizontalDivider(color = palette.line.copy(alpha = 0.6f), thickness = 0.6.dp)
                                                Text("鸡关枪专属装饰参数", color = palette.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    listOf(0, 24, 32, 40, 48, 56, 60).forEach { sz ->
                                                        FilterChip(
                                                            selected = config.cgSize == sz,
                                                            onClick = { config = config.copy(cgSize = sz) },
                                                            label = { Text(if (sz <= 0) "自动尺寸" else "${sz}px", fontSize = 11.sp) },
                                                        )
                                                    }
                                                }
                                                CleanDecoToggleRow("动态摇摆动作 (cg_motion)", config.cgMotion) {
                                                    config = config.copy(cgMotion = it)
                                                }
                                                CleanDecoToggleRow("随机尺寸变化 (cg_random)", config.cgRandom) {
                                                    config = config.copy(cgRandom = it)
                                                }
                                                CleanDecoToggleRow("鸡关枪连射火焰特效 (cg_fire)", config.cgFire) {
                                                    config = config.copy(cgFire = it)
                                                }
                                            }
                                        }
                                    }

                                    if (ownedPersonals.isNotEmpty()) {
                                        Text("已拥有专属称号套装", color = palette.muted, fontSize = 12.sp)
                                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            ownedPersonals.forEach { item ->
                                                val selected = config.equippedPersonalId == item.id
                                                val tag = when {
                                                    item.art == "v2" && item.rarity.equals("SSR", ignoreCase = true) -> " · v2 SSR"
                                                    item.art == "v2" -> " · v2"
                                                    item.rarity.equals("SSR", ignoreCase = true) -> " · SSR"
                                                    else -> ""
                                                }
                                                FilterChip(
                                                    selected = selected,
                                                    onClick = {
                                                        config = if (selected) {
                                                            clearExclusiveTitleSlots(config)
                                                        } else {
                                                            clearExclusiveTitleSlots(config).copy(
                                                                equippedPersonalId = item.id,
                                                                communityTitleId = "",
                                                            )
                                                        }
                                                    },
                                                    label = { Text("${item.name}$tag", fontSize = 12.sp) },
                                                )
                                            }
                                        }
                                    }

                                    if (liveMe.officialTitles.isNotEmpty()) {
                                        Text("站长授予官方称号", color = palette.muted, fontSize = 12.sp)
                                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            liveMe.officialTitles.forEach { ot ->
                                                val selected = config.equippedOfficialId == ot.id
                                                FilterChip(
                                                    selected = selected,
                                                    onClick = {
                                                        config = if (selected) {
                                                            clearExclusiveTitleSlots(config)
                                                        } else {
                                                            clearExclusiveTitleSlots(config).copy(
                                                                equippedOfficialId = ot.id,
                                                                communityTitleId = "",
                                                            )
                                                        }
                                                    },
                                                    label = { Text(ot.name, fontSize = 12.sp) },
                                                )
                                            }
                                        }
                                    }

                                    if (unlockedGrowths.isNotEmpty()) {
                                        Text("成长与排行榜奖励称号 (${unlockedGrowths.size})", color = palette.muted, fontSize = 12.sp)
                                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            unlockedGrowths.forEach { gr ->
                                                val selected = config.equippedGrowthId == gr.id
                                                val labelStr = gr.label?.takeIf { it.isNotBlank() } ?: gr.name
                                                FilterChip(
                                                    selected = selected,
                                                    onClick = {
                                                        config = if (selected) {
                                                            clearExclusiveTitleSlots(config)
                                                        } else {
                                                            clearExclusiveTitleSlots(config).copy(
                                                                equippedGrowthId = gr.id,
                                                                communityTitleId = "",
                                                            )
                                                        }
                                                    },
                                                    label = { Text("$labelStr Lv${gr.level}", fontSize = 11.5.sp) },
                                                )
                                            }
                                        }
                                    }

                                    if (ownedBadges.any { it.id == "guide" && it.count >= 3 } || "title:guangjie" in unlockKeys || allCosmetics) {
                                        FilterChip(
                                            selected = config.equippedInviteTitle,
                                            onClick = {
                                                config = if (config.equippedInviteTitle) {
                                                    clearExclusiveTitleSlots(config)
                                                } else {
                                                    clearExclusiveTitleSlots(config).copy(equippedInviteTitle = true, communityTitleId = "")
                                                }
                                            },
                                            label = { Text("✿ 广结善缘（引路人专属）", fontSize = 12.sp) },
                                        )
                                    }

                                    Text("开源社区投稿称号（动态同步 ${communitySkins.count { it.hasTitle }} 款）", color = palette.muted, fontSize = 12.sp)
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        communitySkins.filter { it.hasTitle }.forEach { skin ->
                                            val selected = config.communityTitleId == skin.id
                                            val ownedMark = if (ownedCommunityIds.contains(skin.id)) " ✓" else ""
                                            FilterChip(
                                                selected = selected,
                                                onClick = {
                                                    config = config.copy(communityTitleId = if (selected) "" else skin.id)
                                                },
                                                label = { Text("${skin.name}$ownedMark", fontSize = 12.sp) },
                                            )
                                        }
                                    }

                                    HorizontalDivider(
                                        color = if (palette.isModernUi) palette.glassBorder else palette.line,
                                        thickness = if (palette.isModernUi) 0.5.dp else 1.dp,
                                    )

                                    ForHappyTextField(
                                        value = config.customTitle,
                                        onValueChange = { config = config.copy(customTitle = it.take(8)) },
                                        label = "自定义文字称号（最多 8 字，未穿戴套装时生效）",
                                        placeholder = "输入自定义称号",
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                    )

                                    Text("称号外框、填充、字体与特效", color = palette.muted, fontSize = 12.sp)
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf("pill" to "胶囊", "tag" to "方签", "ribbon" to "丝带", "seal" to "印章").forEach { (shKey, shLabel) ->
                                            FilterChip(
                                                selected = config.titleStyle == shKey,
                                                onClick = { config = config.copy(titleStyle = shKey) },
                                                label = { Text(shLabel, fontSize = 12.sp) },
                                            )
                                        }
                                        listOf("solid" to "实心底", "outline" to "描边底", "grad" to "渐变").forEach { (fillKey, fillLabel) ->
                                            FilterChip(
                                                selected = config.fillMode == fillKey,
                                                onClick = { config = config.copy(fillMode = fillKey) },
                                                label = { Text(fillLabel, fontSize = 12.sp) },
                                            )
                                        }
                                    }
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf("" to "默认字体", "hei" to "黑体", "song" to "宋体", "kai" to "楷体", "mono" to "等宽").forEach { (fk, fl) ->
                                            FilterChip(
                                                selected = config.titleFont == fk,
                                                onClick = { config = config.copy(titleFont = fk) },
                                                label = { Text(fl, fontSize = 11.5.sp) },
                                            )
                                        }
                                        listOf("none" to "无特效", "shine" to "扫光", "flow" to "流光").forEach { (fxk, fxl) ->
                                            FilterChip(
                                                selected = config.titleEffect == fxk,
                                                onClick = { config = config.copy(titleEffect = fxk) },
                                                label = { Text(fxl, fontSize = 11.5.sp) },
                                            )
                                        }
                                    }

                                    Text("自定义称号主色", color = palette.muted, fontSize = 12.sp)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf("#16a34a", "#1B7F4B", "#2563EB", "#D97706", "#DB2777", "#7C3AED", "#1E293B").forEach { hex ->
                                            val c = parseHexColor(hex, palette.primary)
                                            Box(
                                                modifier = Modifier
                                                    .size(30.dp)
                                                    .clip(CircleShape)
                                                    .background(c)
                                                    .border(if (config.primaryHex.equals(hex, ignoreCase = true)) 2.5.dp else 1.dp, palette.text, CircleShape)
                                                    .clickable { config = config.copy(primaryHex = hex, secondaryHex = hex) },
                                            )
                                        }
                                    }

                                    Text("装扮展示范围", color = palette.muted, fontSize = 12.sp)
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf("all" to "全站显示", "chat" to "仅群聊显示", "post" to "仅图廊帖子").forEach { (scKey, scLabel) ->
                                            FilterChip(
                                                selected = config.decoScope == scKey,
                                                onClick = { config = config.copy(decoScope = scKey) },
                                                label = { Text(scLabel, fontSize = 12.sp) },
                                            )
                                        }
                                    }
                                }
                                1 -> {
                                    Text("选择炫彩渐变昵称（与网页端预设实时同步）", color = palette.muted, fontSize = 12.sp)
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        NICK_GRADIENT_OPTIONS.forEach { (id, label, stops) ->
                                            val isSelected = config.nickGradient == id && (id.isNotEmpty() || config.customGradientStops.isEmpty())
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = {
                                                    config = config.copy(
                                                        nickGradient = id,
                                                        customGradientStops = stops,
                                                    )
                                                },
                                                label = {
                                                    if (stops.size >= 2) {
                                                        Text(
                                                            text = label,
                                                            style = TextStyle(
                                                                brush = Brush.horizontalGradient(stops.map { parseSafeHex(it, palette.text) }),
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.Bold,
                                                            ),
                                                        )
                                                    } else {
                                                        Text(label, fontSize = 12.sp)
                                                    }
                                                },
                                            )
                                        }
                                    }
                                }
                                3 -> {
                                    Text("官方聊天气泡皮肤", color = palette.muted, fontSize = 12.sp)
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        BadgeAndCosmeticRegistry.BUBBLE_PRESETS.forEach { (id, label) ->
                                            val selected = config.communityBubbleId.isBlank() &&
                                                (config.bubbleSkin == id || (id.isEmpty() && config.bubbleSkin == "default"))
                                            FilterChip(
                                                selected = selected,
                                                onClick = { config = config.copy(bubbleSkin = id, communityBubbleId = "") },
                                                label = { Text(label, fontSize = 12.sp) },
                                            )
                                        }
                                    }

                                    HorizontalDivider(
                                        color = if (palette.isModernUi) palette.glassBorder else palette.line,
                                        thickness = if (palette.isModernUi) 0.5.dp else 1.dp,
                                    )

                                    Text("开源社区投稿气泡皮肤（限定池已拥有 ${ownedCommunityIds.size} / ${communitySkins.count { it.hasBubble }} 款）", color = palette.muted, fontSize = 12.sp)
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        communitySkins.filter { it.hasBubble }.forEach { skin ->
                                            val selected = config.communityBubbleId == skin.id
                                            val isOwned = allCosmetics || ownedCommunityIds.contains(skin.id)
                                            val canEquip = isOwned || selected
                                            val suffix = if (isOwned) " ✓" else " · 未获得"
                                            FilterChip(
                                                selected = selected,
                                                enabled = canEquip,
                                                onClick = {
                                                    if (canEquip) {
                                                        config = config.copy(
                                                            communityBubbleId = if (selected) "" else skin.id,
                                                            bubbleSkin = if (selected) config.bubbleSkin else "default",
                                                        )
                                                    }
                                                },
                                                label = { Text("${skin.name} · ${skin.author}$suffix", fontSize = 12.sp) },
                                            )
                                        }
                                    }

                                    Text("开源社区引用/+1回应框（共 ${communitySkins.count { it.hasReply }} 款）", color = palette.muted, fontSize = 12.sp)
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        communitySkins.filter { it.hasReply }.forEach { skin ->
                                            val selected = config.communityReplyId == skin.id
                                            val ownedMark = if (ownedCommunityIds.contains(skin.id)) " ✓" else ""
                                            FilterChip(
                                                selected = selected,
                                                onClick = {
                                                    config = config.copy(communityReplyId = if (selected) "" else skin.id)
                                                },
                                                label = { Text("${skin.name} · ${skin.author}$ownedMark", fontSize = 12.sp) },
                                            )
                                        }
                                    }

                                    Text("开源社区投稿徽章（共 ${communitySkins.count { it.hasBadge }} 款）", color = palette.muted, fontSize = 12.sp)
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        communitySkins.filter { it.hasBadge }.forEach { skin ->
                                            val selected = config.communityBadgeId == skin.id
                                            val ownedMark = if (ownedCommunityIds.contains(skin.id)) " ✓" else ""
                                            FilterChip(
                                                selected = selected,
                                                onClick = {
                                                    config = config.copy(
                                                        communityBadgeId = if (selected) "" else skin.id,
                                                        badgeMode = if (selected) config.badgeMode else "set",
                                                    )
                                                },
                                                label = { Text("${skin.name} · ${skin.author}$ownedMark", fontSize = 12.sp) },
                                            )
                                        }
                                    }
                                }
                                4 -> {
                                    CleanDecoToggleRow("隐藏他人彩色渐变昵称", config.cleanHideOthersNick) {
                                        config = config.copy(cleanHideOthersNick = it)
                                    }
                                    CleanDecoToggleRow("隐藏他人徽章图标", config.cleanHideOthersBadges) {
                                        config = config.copy(cleanHideOthersBadges = it)
                                    }
                                    CleanDecoToggleRow("隐藏他人装饰边框与特殊称号", config.cleanHideOthersStyles) {
                                        config = config.copy(cleanHideOthersStyles = it)
                                    }
                                    CleanDecoToggleRow("隐藏聊天气泡特效皮肤", config.cleanHideBubbles) {
                                        config = config.copy(cleanHideBubbles = it)
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

@Composable
private fun BadgeWallCardRow(
    item: OwnedBadgeItem,
    isEquipped: Boolean,
    api: ForHappyApiClient,
    onToggleEquip: () -> Unit,
) {
    val palette = LocalForHappyPalette.current
    val borderColor = when {
        isEquipped -> palette.primary
        item.owned -> palette.line
        else -> palette.line.copy(alpha = 0.45f)
    }
    val containerColor = when {
        isEquipped -> palette.primary.copy(alpha = 0.10f)
        item.owned -> palette.surface
        else -> palette.surface.copy(alpha = 0.55f)
    }

    Surface(
        color = containerColor,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(if (isEquipped) 1.5.dp else 1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (item.owned) 1f else 0.58f)
            .clickable(enabled = item.owned) { onToggleEquip() },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BadgeArtIcon(
                art = item.art,
                level = item.level,
                imageLoader = api.imageLoader,
                size = 42.dp,
            )

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = item.meta.name,
                        color = palette.text,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    BadgePill(
                        text = item.levelText,
                        containerColor = if (item.owned) palette.gold.copy(alpha = 0.16f) else palette.surface2,
                        contentColor = if (item.owned) palette.gold else palette.muted,
                    )
                }

                Text(
                    text = item.meta.desc,
                    color = palette.muted,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                val progressLine = buildString {
                    if (item.id == "elder") {
                        append(if (item.owned) "开站纪念元老专属" else "2026-10-01 开站限定")
                    } else {
                        if (item.count > 0) {
                            append("当前累计 ${item.count} ${item.meta.unit}")
                        } else {
                            append("尚未点亮")
                        }
                        if (item.nextAt > 0 && item.remainingToNext > 0) {
                            append(" · 再达成 ${item.remainingToNext} ${item.meta.unit} 升级")
                        } else if (item.owned && item.nextAt == 0) {
                            append(" · 已满级")
                        }
                    }
                    item.lastDay?.takeIf { it.isNotBlank() }?.let { append(" · 最近 $it") }
                }
                Text(progressLine, color = palette.muted.copy(alpha = 0.85f), fontSize = 11.sp)
            }

            if (item.owned) {
                Surface(
                    color = if (isEquipped) palette.primary else palette.surface2,
                    shape = RoundedCornerShape(999.dp),
                ) {
                    Text(
                        text = if (isEquipped) "佩戴中" else "佩戴",
                        color = if (isEquipped) palette.onPrimary else palette.text,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun CleanDecoToggleRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val palette = LocalForHappyPalette.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, color = palette.text, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private data class InviteRow(
    val code: String,
    val status: String,
    val note: String,
)

@Composable
fun InviteCodesDialog(
    api: ForHappyApiClient,
    onOpenWebRoute: (route: String, title: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalForHappyPalette.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var invites by remember { mutableStateOf<List<InviteRow>>(emptyList()) }
    var quotaInfo by remember { mutableStateOf("可用邀请码") }
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var bannerText by remember { mutableStateOf<String?>(null) }

    fun reload() {
        loading = true
        scope.launch {
            val obj = api.fetchMyInvites()
            loading = false
            if (obj != null) {
                val arr = (obj["invites"] ?: obj["codes"] ?: obj["items"])?.let { runCatching { it.jsonArray }.getOrNull() }
                invites = arr?.mapNotNull { el ->
                    val row = runCatching { el.jsonObject }.getOrNull() ?: return@mapNotNull null
                    val code = row["code"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                    val used = row["used_by"]?.jsonPrimitive?.contentOrNull
                    val revoked = row["revoked"]?.jsonPrimitive?.booleanOrNull == true
                    val status = when {
                        revoked -> "已撤销"
                        !used.isNullOrBlank() -> "已使用 ($used)"
                        else -> "有效可用"
                    }
                    InviteRow(code = code, status = status, note = row["qualified"]?.jsonPrimitive?.contentOrNull.orEmpty())
                } ?: emptyList()
                val rem = obj["remaining"]?.jsonPrimitive?.contentOrNull ?: obj["quota"]?.jsonPrimitive?.contentOrNull
                if (!rem.isNullOrBlank()) quotaInfo = "剩余可用名额：$rem"
            }
        }
    }

    LaunchedEffect(Unit) { reload() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = FullscreenDialogProperties,
    ) {
        ConfigureFullscreenDialogWindow(darkStatusIcons = !palette.isDark)
        DialogBackOwnerScope {
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
                        .height(ForHappyDimens.topBarHeight)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回", tint = palette.text)
                    }
                    Text("邀请码管理", color = palette.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Button(
                        enabled = !busy,
                        onClick = {
                            busy = true
                            scope.launch {
                                val res = api.createMyInvite()
                                busy = false
                                res.fold(
                                    onSuccess = {
                                        bannerText = "已生成新邀请码"
                                        reload()
                                    },
                                    onFailure = { err -> bannerText = err.message ?: "生成失败" },
                                )
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("生成", fontSize = 12.sp)
                    }
                }
            }
            HorizontalDivider(
                color = if (palette.isModernUi) palette.glassBorder else palette.line,
                thickness = if (palette.isModernUi) 0.5.dp else 1.dp,
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 14.dp,
                    top = 14.dp,
                    end = 14.dp,
                    bottom = 14.dp + navBottomPad,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                bannerText?.let { msg ->
                    item(key = "banner") {
                        Surface(color = palette.primary.copy(alpha = 0.12f), shape = RoundedCornerShape(10.dp)) {
                            Text(msg, color = palette.primary, fontSize = 12.sp, modifier = Modifier.fillMaxWidth().padding(10.dp))
                        }
                    }
                }
                item(key = "intro") {
                    Surface(
                        color = palette.surface,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().border(1.dp, palette.line, RoundedCornerShape(14.dp)),
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(quotaInfo, color = palette.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("好友通过你的邀请码注册并在 7 天内保持活跃即可计为合格邀请奖励。", color = palette.muted, fontSize = 12.sp)
                            TextButton(
                                onClick = {
                                    onDismiss()
                                    onOpenWebRoute("/me", "邀请记录")
                                },
                                contentPadding = PaddingValues(0.dp),
                            ) {
                                Text("在网页端查看完整邀请记录 →", fontSize = 12.sp)
                            }
                        }
                    }
                }
                if (loading) {
                    item(key = "loading") {
                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = palette.primary)
                        }
                    }
                } else if (invites.isEmpty()) {
                    item(key = "empty") {
                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text("暂无邀请码记录，可点击右上角生成", color = palette.muted, fontSize = 13.sp)
                        }
                    }
                } else {
                    items(invites, key = { it.code }) { row ->
                        Surface(
                            color = palette.surface,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().border(1.dp, palette.line, RoundedCornerShape(12.dp)),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(row.code, color = palette.text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = if (row.note.isNotBlank()) "${row.status} · ${row.note}" else row.status,
                                        color = palette.muted,
                                        fontSize = 11.sp,
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        clipboard.setText(AnnotatedString(row.code))
                                        bannerText = "已复制 ${row.code}"
                                    },
                                ) {
                                    Icon(Icons.Rounded.ContentCopy, contentDescription = "复制", tint = palette.primary, modifier = Modifier.size(18.dp))
                                }
                                if (row.status == "有效可用") {
                                    IconButton(
                                        onClick = {
                                            busy = true
                                            scope.launch {
                                                val ok = api.revokeMyInvite(row.code)
                                                busy = false
                                                if (ok) reload() else bannerText = "撤销失败"
                                            }
                                        },
                                    ) {
                                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "撤销", tint = palette.danger, modifier = Modifier.size(18.dp))
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

internal fun formatMeTime(epochSec: Long): String {
    if (epochSec <= 0L) return ""
    return SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(Date(epochSec * 1000L))
}
