package com.jinhuan.forhappy.ui.me

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.PhotoCamera
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jinhuan.forhappy.core.model.DecoConfig
import com.jinhuan.forhappy.core.model.ForHappyJson
import com.jinhuan.forhappy.core.model.HbKeyStatus
import com.jinhuan.forhappy.core.model.MeUser
import com.jinhuan.forhappy.core.model.Owner52bCard
import com.jinhuan.forhappy.core.model.WalletLedgerRow
import com.jinhuan.forhappy.core.model.WalletSummary
import com.jinhuan.forhappy.core.navigation.StealthExemptionGate
import com.jinhuan.forhappy.core.network.api.ForHappyApiClient
import com.jinhuan.forhappy.ui.components.BadgePill
import com.jinhuan.forhappy.ui.components.ForHappyAvatar
import com.jinhuan.forhappy.ui.theme.ForHappyDimens
import com.jinhuan.forhappy.ui.theme.LocalForHappyPalette
import com.jinhuan.forhappy.ui.theme.TABULAR_FONT_FEATURE
import com.jinhuan.forhappy.ui.wall.ConfigureFullscreenDialogWindow
import kotlinx.coroutines.Dispatchers
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
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

const val KV_DECO_CLEAN = "deco.clean.v2"

fun parseDecoConfig(deco: JsonElement?, localCleanRaw: String? = null): DecoConfig {
    val obj = runCatching { deco?.jsonObject }.getOrNull()
    val fromServer = if (obj != null) {
        DecoConfig(
            customTitle = obj["customTitle"]?.jsonPrimitive?.contentOrNull
                ?: obj["title"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            titleStyle = obj["titleStyle"]?.jsonPrimitive?.contentOrNull ?: "pill",
            fillMode = obj["fillMode"]?.jsonPrimitive?.contentOrNull ?: "solid",
            primaryHex = obj["primaryHex"]?.jsonPrimitive?.contentOrNull ?: "#1B7F4B",
            secondaryHex = obj["secondaryHex"]?.jsonPrimitive?.contentOrNull ?: "#34A853",
            nickGradient = obj["nickGradient"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            bubbleSkin = obj["bubbleSkin"]?.jsonPrimitive?.contentOrNull ?: "default",
            cleanHideOthersNick = obj["cleanHideOthersNick"]?.jsonPrimitive?.booleanOrNull ?: false,
            cleanHideOthersBadges = obj["cleanHideOthersBadges"]?.jsonPrimitive?.booleanOrNull ?: false,
            cleanHideOthersStyles = obj["cleanHideOthersStyles"]?.jsonPrimitive?.booleanOrNull ?: false,
            cleanHideBubbles = obj["cleanHideBubbles"]?.jsonPrimitive?.booleanOrNull ?: false,
        )
    } else {
        DecoConfig()
    }
    if (localCleanRaw.isNullOrBlank()) return fromServer
    val localCfg = runCatching { ForHappyJson.decodeFromString<DecoConfig>(localCleanRaw) }.getOrNull() ?: return fromServer
    return fromServer.copy(
        cleanHideOthersNick = localCfg.cleanHideOthersNick || fromServer.cleanHideOthersNick,
        cleanHideOthersBadges = localCfg.cleanHideOthersBadges || fromServer.cleanHideOthersBadges,
        cleanHideOthersStyles = localCfg.cleanHideOthersStyles || fromServer.cleanHideOthersStyles,
        cleanHideBubbles = localCfg.cleanHideBubbles || fromServer.cleanHideBubbles,
    )
}

fun extractBadgeLabels(badges: JsonElement?): List<String> {
    val arr = runCatching { badges?.jsonArray }.getOrNull() ?: return emptyList()
    return arr.mapNotNull { el ->
        when (el) {
            is JsonPrimitive -> el.contentOrNull?.takeIf { it.isNotBlank() }
            is JsonObject -> {
                el["name"]?.jsonPrimitive?.contentOrNull
                    ?: el["label"]?.jsonPrimitive?.contentOrNull
                    ?: el["icon"]?.jsonPrimitive?.contentOrNull
                    ?: el["id"]?.jsonPrimitive?.contentOrNull
            }
            else -> null
        }
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
                OutlinedTextField(
                    value = nick,
                    onValueChange = { nick = it },
                    label = { Text("昵称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("个人签名 / Bio") },
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
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        ConfigureFullscreenDialogWindow(darkStatusIcons = !palette.isDark)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.bg)
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            Surface(color = palette.surface) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
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
            HorizontalDivider(color = palette.line)

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(14.dp),
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
                            .border(1.dp, palette.line, RoundedCornerShape(16.dp)),
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
                            .border(1.dp, palette.line, RoundedCornerShape(16.dp)),
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
    initialCards: List<Owner52bCard>,
    onOpenWebRoute: (route: String, title: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalForHappyPalette.current
    var cards by remember(initialCards) { mutableStateOf(initialCards) }
    var loading by remember { mutableStateOf(initialCards.isEmpty()) }

    LaunchedEffect(Unit) {
        val loaded = api.fetchOwner52bCards()
        if (loaded.isNotEmpty()) cards = loaded
        loading = false
    }

    val ownedCount = cards.count { it.owned }
    val ssrCards = cards.filter { it.tier.orEmpty().equals("SSR", ignoreCase = true) }
    val ssrOwned = ssrCards.count { it.owned }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        ConfigureFullscreenDialogWindow(darkStatusIcons = !palette.isDark)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.bg)
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            Surface(color = palette.surface) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
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
            HorizontalDivider(color = palette.line)

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "progress") {
                    Surface(
                        color = palette.surface,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, palette.line, RoundedCornerShape(16.dp)),
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("收集进度 $ownedCount / ${cards.size}", color = palette.text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                if (ssrCards.isNotEmpty()) {
                                    Text("SSR $ssrOwned/${ssrCards.size}", color = palette.gold, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            val frac = if (cards.isEmpty()) 0f else (ownedCount.toFloat() / cards.size.toFloat()).coerceIn(0f, 1f)
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
                } else {
                    items(cards.chunked(2)) { rowCards ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            rowCards.forEach { card ->
                                Surface(
                                    color = if (card.owned) palette.surface else palette.surface2.copy(alpha = 0.55f),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .border(
                                            1.dp,
                                            if (card.owned) palette.primary.copy(alpha = 0.35f) else palette.line,
                                            RoundedCornerShape(14.dp),
                                        ),
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = if (card.owned) card.icon?.ifBlank { "🃏" } ?: "🃏" else "🔒",
                                                fontSize = 20.sp,
                                            )
                                            card.tier?.takeIf { it.isNotBlank() }?.let { tier ->
                                                BadgePill(
                                                    text = tier.uppercase(),
                                                    containerColor = if (tier.equals("SSR", true)) palette.gold.copy(alpha = 0.2f) else palette.surface2,
                                                    contentColor = if (tier.equals("SSR", true)) palette.gold else palette.muted,
                                                )
                                            }
                                        }
                                        Text(
                                            text = if (card.owned) card.name else "未解锁 #${card.id}",
                                            color = if (card.owned) palette.text else palette.muted,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        card.desc?.takeIf { it.isNotBlank() && card.owned }?.let { desc ->
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

    var config by remember(me.id) { mutableStateOf(parseDecoConfig(me.deco, initialCleanRaw)) }
    var raffleTitles by remember { mutableStateOf<List<String>>(emptyList()) }
    var personalTitles by remember { mutableStateOf<List<String>>(emptyList()) }
    var activeTab by remember { mutableIntStateOf(0) } // 0=称号 1=渐变昵称 2=徽章柜 3=气泡皮肤 4=洁癖屏蔽
    var saving by remember { mutableStateOf(false) }
    var bannerText by remember { mutableStateOf<String?>(null) }

    val previewPagerState = rememberPagerState(initialPage = 0, pageCount = { 4 })
    val previewLabels = listOf("图廊卡片", "群聊名牌", "聊天气泡", "回应框")
    val badges = remember(me.badges) { extractBadgeLabels(me.badges) }

    LaunchedEffect(me.id) {
        val rObj = api.fetchMyRaffleTitles()
        val pObj = api.fetchMyPersonalTitles()
        raffleTitles = rObj?.get("titles")?.let { runCatching { it.jsonArray.mapNotNull { t -> t.jsonPrimitive.contentOrNull } }.getOrNull() }
            ?: rObj?.get("items")?.let { runCatching { it.jsonArray.mapNotNull { t -> t.jsonObject["title"]?.jsonPrimitive?.contentOrNull } }.getOrNull() }
            ?: emptyList()
        personalTitles = pObj?.get("titles")?.let { runCatching { it.jsonArray.mapNotNull { t -> t.jsonPrimitive.contentOrNull } }.getOrNull() }
            ?: pObj?.get("items")?.let { runCatching { it.jsonArray.mapNotNull { t -> t.jsonObject["title"]?.jsonPrimitive?.contentOrNull } }.getOrNull() }
            ?: emptyList()
    }

    val titleColor = remember(config.primaryHex, palette.primary) { parseHexColor(config.primaryHex, palette.primary) }
    val nickBrushColors = when (config.nickGradient) {
        "pine" -> listOf(Color(0xFF1B7F4B), Color(0xFF34A853))
        "sunset" -> listOf(Color(0xFFEA580C), Color(0xFFEC4899))
        "aurora" -> listOf(Color(0xFF06B6D4), Color(0xFF8B5CF6))
        "gold" -> listOf(Color(0xFFD97706), Color(0xFFF59E0B))
        else -> null
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        ConfigureFullscreenDialogWindow(darkStatusIcons = !palette.isDark)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.bg)
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            Surface(color = palette.surface) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ForHappyDimens.topBarHeight)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回", tint = palette.text)
                    }
                    Text("装扮工作室", color = palette.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Button(
                        enabled = !saving,
                        onClick = {
                            saving = true
                            scope.launch {
                                val decoJson = buildJsonObject {
                                    put("customTitle", JsonPrimitive(config.customTitle))
                                    put("titleStyle", JsonPrimitive(config.titleStyle))
                                    put("fillMode", JsonPrimitive(config.fillMode))
                                    put("primaryHex", JsonPrimitive(config.primaryHex))
                                    put("secondaryHex", JsonPrimitive(config.secondaryHex))
                                    put("nickGradient", JsonPrimitive(config.nickGradient))
                                    put("bubbleSkin", JsonPrimitive(config.bubbleSkin))
                                }
                                val res = api.updateMeDeco(decoJson)
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
            HorizontalDivider(color = palette.line)

            // Fixed Top 4-Scene Realtime Preview Pager
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.surface)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    previewLabels.forEachIndexed { idx, label ->
                        FilterChip(
                            selected = previewPagerState.currentPage == idx,
                            onClick = { scope.launch { previewPagerState.animateScrollToPage(idx) } },
                            label = { Text(label, fontSize = 11.sp) },
                        )
                    }
                }
                HorizontalPager(
                    state = previewPagerState,
                    modifier = Modifier.fillMaxWidth().height(92.dp),
                ) { page ->
                    val nickStyle = if (nickBrushColors != null && !config.cleanHideOthersStyles) {
                        TextStyle(brush = Brush.horizontalGradient(nickBrushColors), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    } else {
                        TextStyle(color = palette.text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Surface(
                        color = palette.surface2,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxSize().border(1.dp, palette.line, RoundedCornerShape(14.dp)),
                    ) {
                        Box(modifier = Modifier.fillMaxSize().padding(14.dp), contentAlignment = Alignment.CenterStart) {
                            when (page) {
                                0 -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    ForHappyAvatar(me.displayName, me.avatar, api.imageLoader, size = 38.dp)
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(me.displayName, style = nickStyle)
                                            if (config.customTitle.isNotBlank()) {
                                                BadgePill(config.customTitle, containerColor = titleColor.copy(alpha = 0.16f), contentColor = titleColor)
                                            }
                                        }
                                        Text("图廊卡片署名与角标实时预览", color = palette.muted, fontSize = 11.sp)
                                    }
                                }
                                1 -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("[名牌]", color = palette.muted, fontSize = 11.sp)
                                    Text(me.displayName, style = nickStyle)
                                    if (config.customTitle.isNotBlank()) {
                                        BadgePill(config.customTitle, containerColor = titleColor, contentColor = Color.White)
                                    }
                                    badges.take(3).forEach { b -> Text(b, color = palette.gold, fontSize = 11.sp) }
                                }
                                2 -> {
                                    val bubbleBg = when {
                                        config.cleanHideBubbles -> palette.surface
                                        config.bubbleSkin == "warm" -> Color(0xFFFFF3E0)
                                        config.bubbleSkin == "moss" -> Color(0xFFE7F5EC)
                                        config.bubbleSkin == "ocean" -> Color(0xFFE3F2FD)
                                        config.bubbleSkin == "paper" -> Color(0xFFF6F1E7)
                                        else -> palette.primaryContainer
                                    }
                                    Surface(color = bubbleBg, shape = RoundedCornerShape(14.dp)) {
                                        Text(
                                            "摸鱼快乐！当前皮肤：${config.bubbleSkin}",
                                            color = Color(0xFF1F2937),
                                            fontSize = 13.sp,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        )
                                    }
                                }
                                else -> Surface(
                                    color = palette.surface,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.border(1.dp, titleColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                                ) {
                                    Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(me.displayName, style = nickStyle)
                                        Text("回应了这组图片：「太好看了！」", color = palette.text, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            HorizontalDivider(color = palette.line)

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
                listOf("称号", "渐变昵称", "徽章柜", "气泡皮肤", "洁癖屏蔽").forEachIndexed { idx, label ->
                    FilterChip(
                        selected = activeTab == idx,
                        onClick = { activeTab = idx },
                        label = { Text(label, fontSize = 12.sp) },
                    )
                }
            }
            HorizontalDivider(color = palette.line)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when (activeTab) {
                    0 -> {
                        OutlinedTextField(
                            value = config.customTitle,
                            onValueChange = { config = config.copy(customTitle = it.take(16)) },
                            label = { Text("自定义称号 (留空隐藏)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        val available = (raffleTitles + personalTitles).distinct()
                        if (available.isNotEmpty()) {
                            Text("已拥有称号快捷选用", color = palette.muted, fontSize = 12.sp)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                available.forEach { t ->
                                    FilterChip(
                                        selected = config.customTitle == t,
                                        onClick = { config = config.copy(customTitle = t) },
                                        label = { Text(t, fontSize = 12.sp) },
                                    )
                                }
                            }
                        }
                        Text("主色调", color = palette.muted, fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("#1B7F4B", "#2563EB", "#C58C24", "#DB2777", "#7C3AED").forEach { hex ->
                                val c = parseHexColor(hex, palette.primary)
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(c)
                                        .border(if (config.primaryHex == hex) 2.5.dp else 1.dp, palette.text, CircleShape)
                                        .clickable { config = config.copy(primaryHex = hex) },
                                )
                            }
                        }
                    }
                    1 -> {
                        Text("选择昵称渐变色（仅在未开启洁癖屏蔽他人样式时生效）", color = palette.muted, fontSize = 12.sp)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("" to "默认纯色", "pine" to "松绿森林", "sunset" to "晚霞橘粉", "aurora" to "极光蓝紫", "gold" to "鎏金琥珀").forEach { (id, label) ->
                                FilterChip(
                                    selected = config.nickGradient == id,
                                    onClick = { config = config.copy(nickGradient = id) },
                                    label = { Text(label, fontSize = 12.sp) },
                                )
                            }
                        }
                    }
                    2 -> {
                        Text("已解锁徽章（展示前 3–6 枚）", color = palette.muted, fontSize = 12.sp)
                        if (badges.isEmpty()) {
                            Text("暂无可展示徽章，活跃发图/参与活动即可解锁", color = palette.muted, fontSize = 13.sp)
                        } else {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                badges.take(6).forEach { b ->
                                    BadgePill(b, containerColor = palette.surface, contentColor = palette.text)
                                }
                            }
                        }
                    }
                    3 -> {
                        Text("选择本人聊天消息气泡皮肤", color = palette.muted, fontSize = 12.sp)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("default" to "默认", "warm" to "暖杏", "moss" to "青苔", "ocean" to "海风", "paper" to "宣纸").forEach { (id, label) ->
                                FilterChip(
                                    selected = config.bubbleSkin == id,
                                    onClick = { config = config.copy(bubbleSkin = id) },
                                    label = { Text(label, fontSize = 12.sp) },
                                )
                            }
                        }
                    }
                    4 -> {
                        CleanDecoToggleRow("隐藏他人彩色渐变昵称", config.cleanHideOthersNick) {
                            config = config.copy(cleanHideOthersNick = it)
                        }
                        CleanDecoToggleRow("隐藏他人徽章列表", config.cleanHideOthersBadges) {
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
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        ConfigureFullscreenDialogWindow(darkStatusIcons = !palette.isDark)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.bg)
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            Surface(color = palette.surface) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
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
            HorizontalDivider(color = palette.line)

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(14.dp),
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

internal fun formatMeTime(epochSec: Long): String {
    if (epochSec <= 0L) return ""
    return SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(Date(epochSec * 1000L))
}
