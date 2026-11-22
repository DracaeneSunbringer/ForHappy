package com.jinhuan.forhappy.ui.chat

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.jinhuan.forhappy.core.chat.RedpackMosaicEngine
import com.jinhuan.forhappy.core.model.AppDisguiseMode
import com.jinhuan.forhappy.core.model.CreditFundingStatus
import com.jinhuan.forhappy.core.model.FishFundingStatus
import com.jinhuan.forhappy.core.model.GroupMemberItem
import com.jinhuan.forhappy.core.model.MeUser
import com.jinhuan.forhappy.core.model.RedpackArchiveItem
import com.jinhuan.forhappy.core.model.RedpackMeta
import com.jinhuan.forhappy.core.model.SenderStatItem
import com.jinhuan.forhappy.core.navigation.StealthExemptionGate
import com.jinhuan.forhappy.core.network.api.ForHappyApiClient
import com.jinhuan.forhappy.core.network.media.MediaUrlRewriter
import com.jinhuan.forhappy.ui.components.BadgePill
import com.jinhuan.forhappy.ui.components.ForHappyAvatar
import com.jinhuan.forhappy.ui.disguise.DisguiseSanitizer
import com.jinhuan.forhappy.ui.theme.ForHappyDimens
import com.jinhuan.forhappy.ui.theme.LocalForHappyPalette
import com.jinhuan.forhappy.ui.theme.LocalPrivacyStealthConfig
import com.jinhuan.forhappy.ui.wall.ConfigureFullscreenDialogWindow
import com.jinhuan.forhappy.ui.wall.FullscreenDialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.roundToInt

private val REDPACK_DARK_RED = Color(0xFF9E271E)
private val REDPACK_CRIMSON = Color(0xFFC84B31)
private val REDPACK_GOLD = Color(0xFFF6C86E)

/**
 * Full-screen Scheme 3A Red-Packet Creation Workbench (`02c §5`):
 * - Asset selector (`D币` / `LDC原生` / `HB口令` / `摸鱼派`)
 * - 4 native claim gates (`open`, `phrase`, `cipher` 6 rules, `image` 9-grid irreversible mosaic)
 * - Fish / Credit inline binding & WebView auth (`ShelllessWebScreen` via `onOpenUrl`)
 * - External `HB` / `Fish` / `Credit` WebView guide & share-link paste-back
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateRedpackDialog(
    groupId: Long,
    groupName: String,
    api: ForHappyApiClient,
    me: MeUser?,
    onDismiss: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onSendExternalTextToChat: suspend (String) -> Boolean,
    onCreated: () -> Unit,
) {
    val palette = LocalForHappyPalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var assetTab by remember { mutableStateOf("coin52") } // "coin52" | "ldc" | "hb" | "fish"
    var ldcFundingChannel by remember { mutableStateOf("credit") } // "credit" | "fish" | "coin52"
    var ldcMode by remember { mutableStateOf("random") } // "random" | "fixed" | "exclusive"
    var exclusiveMember by remember { mutableStateOf<GroupMemberItem?>(null) }
    var members by remember { mutableStateOf<List<GroupMemberItem>>(emptyList()) }

    var amountText by remember { mutableStateOf("10") }
    var countText by remember { mutableStateOf("5") }
    var minutesText by remember { mutableStateOf("1440") }
    var promptText by remember { mutableStateOf("大吉大利，恭喜发财") }

    // Claim gate for native / D币 / LDC
    var gateMode by remember { mutableStateOf("open") } // "open" | "phrase" | "cipher" | "image"
    var phraseInput by remember { mutableStateOf("") }
    var secretInput by remember { mutableStateOf("") }
    var cipherGame by remember { mutableStateOf("rails") }
    var maskBits by remember { mutableIntStateOf(0b000010000) } // center cell default
    var pickedUri by remember { mutableStateOf<Uri?>(null) }
    var sourceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Funding status & inline binding
    var fishStatus by remember { mutableStateOf<FishFundingStatus?>(null) }
    var creditStatus by remember { mutableStateOf<CreditFundingStatus?>(null) }
    var showFundingSetup by remember { mutableStateOf(false) }
    var fishKeyInput by remember { mutableStateOf("") }
    var creditIdInput by remember { mutableStateOf("") }
    var creditSecretInput by remember { mutableStateOf("") }

    // External platform paste-back
    var externalLinkDraft by remember { mutableStateOf("") }

    var busy by remember { mutableStateOf(false) }
    var errText by remember { mutableStateOf<String?>(null) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            pickedUri = uri
            scope.launch {
                val bmp = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            BitmapFactory.decodeStream(stream)
                        }
                    }.getOrNull()
                }
                sourceBitmap = bmp
            }
        }
    }

    LaunchedEffect(sourceBitmap, maskBits) {
        val bmp = sourceBitmap
        if (bmp != null) {
            previewBitmap = withContext(Dispatchers.Default) {
                RedpackMosaicEngine.renderNineGridPreviewBitmap(bmp, maskBits)
            }
        } else {
            previewBitmap = null
        }
    }

    LaunchedEffect(assetTab) {
        if (assetTab == "ldc") {
            fishStatus = api.fetchFishFunding()
            creditStatus = api.fetchCreditFunding()
        }
        if (members.isEmpty()) {
            members = api.fetchGroupMembers(groupId).take(30)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = FullscreenDialogProperties,
    ) {
        ConfigureFullscreenDialogWindow(darkStatusIcons = !palette.isDark)
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
                        .height(ForHappyDimens.topBarHeight)
                        .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回", tint = palette.text)
                    }
                    Text("发红包 · $groupName", color = palette.text, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
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
                // Asset selector
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf(
                        "coin52" to "🪙 D币",
                        "ldc" to "💎 LDC原生",
                        "hb" to "🧧 HB口令",
                        "fish" to "🐟 摸鱼派",
                    ).forEach { (key, lbl) ->
                        val sel = assetTab == key
                        Surface(
                            color = if (sel) REDPACK_CRIMSON else palette.surface2,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    assetTab = key
                                    errText = null
                                },
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
                                Text(
                                    text = lbl,
                                    color = if (sel) Color.White else palette.text,
                                    fontSize = 11.sp,
                                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }

                if (assetTab == "hb" || assetTab == "fish") {
                    val platform = if (assetTab == "hb") {
                        RedpackMosaicEngine.ExternalPacketPlatform.HB
                    } else {
                        RedpackMosaicEngine.ExternalPacketPlatform.FISH
                    }
                    Surface(
                        color = palette.surface,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().border(1.dp, palette.line, RoundedCornerShape(14.dp)),
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("${platform.label}（站外网页发包 · Scheme 3A）", color = palette.text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(
                                "站外第三方红包不接入危险脚本桥接，请在内置无壳网页中生成红包口令或链接，再粘贴下方发送至群聊：",
                                color = palette.muted,
                                fontSize = 12.sp,
                            )
                            OutlinedButton(
                                onClick = { onOpenUrl(platform.createUrl) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(Icons.Rounded.Public, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("在内置网页打开 ${platform.label}")
                            }
                            OutlinedTextField(
                                value = externalLinkDraft,
                                onValueChange = { externalLinkDraft = it },
                                label = { Text("粘贴红包链接或口令（支持 hb.unsnow.org / hb.lafi.sh）") },
                                minLines = 2,
                                maxLines = 4,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Button(
                                enabled = externalLinkDraft.isNotBlank() && !busy,
                                onClick = {
                                    busy = true
                                    errText = null
                                    scope.launch {
                                        val ok = onSendExternalTextToChat(externalLinkDraft.trim())
                                        busy = false
                                        if (ok) {
                                            onCreated()
                                        } else {
                                            errText = "发送到群聊失败"
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = REDPACK_CRIMSON, contentColor = Color.White),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(if (busy) "发送中…" else "发送至本群")
                            }
                        }
                    }
                } else {
                    // LDC funding channel configuration card
                    if (assetTab == "ldc") {
                        Surface(
                            color = palette.surface,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().border(1.dp, palette.line, RoundedCornerShape(14.dp)),
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text("支付渠道", color = palette.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    TextButton(onClick = { showFundingSetup = !showFundingSetup }) {
                                        Icon(Icons.Rounded.Settings, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(if (showFundingSetup) "收起凭证配置" else "配置/授权渠道", fontSize = 11.sp)
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                                    listOf(
                                        "credit" to ("Credit" + if (creditStatus?.bound == true) " ✓" else ""),
                                        "fish" to ("Fish" + if (fishStatus?.bound == true) " ✓" else ""),
                                        "coin52" to "D币代付",
                                    ).forEach { (ch, lbl) ->
                                        val sel = ldcFundingChannel == ch
                                        Surface(
                                            color = if (sel) palette.primary else palette.surface2,
                                            shape = RoundedCornerShape(999.dp),
                                            modifier = Modifier.weight(1f).clickable { ldcFundingChannel = ch },
                                        ) {
                                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 6.dp)) {
                                                Text(lbl, color = if (sel) palette.onPrimary else palette.text, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                            }
                                        }
                                    }
                                }
                                if (showFundingSetup) {
                                    HorizontalDivider(color = palette.line.copy(alpha = 0.6f))
                                    Text("LinuxDo Credit 授权 / 凭证", color = palette.muted, fontSize = 11.sp)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        OutlinedTextField(
                                            value = creditIdInput,
                                            onValueChange = { creditIdInput = it },
                                            placeholder = { Text("Client ID", fontSize = 11.sp) },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f),
                                        )
                                        OutlinedTextField(
                                            value = creditSecretInput,
                                            onValueChange = { creditSecretInput = it },
                                            placeholder = { Text("Client Secret", fontSize = 11.sp) },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        OutlinedButton(onClick = { onOpenUrl(creditStatus?.authUrl ?: "https://credit.linux.do/") }) {
                                            Text("打开 Credit 控制台", fontSize = 11.sp)
                                        }
                                        Button(
                                            enabled = creditIdInput.isNotBlank() && creditSecretInput.isNotBlank(),
                                            onClick = {
                                                scope.launch {
                                                    api.saveCreditApp(creditIdInput, creditSecretInput)
                                                    creditStatus = api.fetchCreditFunding()
                                                }
                                            },
                                        ) { Text("保存 Credit", fontSize = 11.sp) }
                                    }
                                    HorizontalDivider(color = palette.line.copy(alpha = 0.6f))
                                    Text("摸鱼派 Fish API Key", color = palette.muted, fontSize = 11.sp)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        OutlinedTextField(
                                            value = fishKeyInput,
                                            onValueChange = { fishKeyInput = it },
                                            placeholder = { Text("输入 Fish API Key", fontSize = 11.sp) },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f),
                                        )
                                        Button(
                                            enabled = fishKeyInput.isNotBlank(),
                                            onClick = {
                                                scope.launch {
                                                    api.saveFishApiKey(fishKeyInput)
                                                    fishStatus = api.fetchFishFunding()
                                                }
                                            },
                                        ) { Text("绑定", fontSize = 11.sp) }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        TextButton(onClick = { onOpenUrl(fishStatus?.authUrl ?: "https://hb.lafi.sh/") }) {
                                            Text("去 Fish 网页查看 Key", fontSize = 11.sp)
                                        }
                                        if (fishStatus?.bound == true) {
                                            TextButton(onClick = {
                                                scope.launch {
                                                    api.disableFishFunding()
                                                    fishStatus = api.fetchFishFunding()
                                                }
                                            }) { Text("停用 Fish", color = palette.danger, fontSize = 11.sp) }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Distribution mode + Amount + Count
                    Surface(
                        color = palette.surface,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().border(1.dp, palette.line, RoundedCornerShape(14.dp)),
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                                listOf("random" to "拼手气", "fixed" to "普通等额", "exclusive" to "专属红包").forEach { (m, lbl) ->
                                    if (assetTab == "coin52" && m == "exclusive") return@forEach
                                    val sel = ldcMode == m
                                    Surface(
                                        color = if (sel) palette.primaryContainer else palette.surface2,
                                        shape = RoundedCornerShape(999.dp),
                                        modifier = Modifier.weight(1f).clickable { ldcMode = m },
                                    ) {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 6.dp)) {
                                            Text(lbl, color = if (sel) palette.primary else palette.text, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                        }
                                    }
                                }
                            }

                            if (ldcMode == "exclusive" && assetTab == "ldc") {
                                Text(
                                    text = exclusiveMember?.let { "已选定专属接收人：${it.displayName}" } ?: "点选一位群成员作为专属接收人：",
                                    color = palette.primary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    members.filter { it.id != me?.id }.take(20).forEach { m ->
                                        val sel = exclusiveMember?.id == m.id
                                        Surface(
                                            color = if (sel) palette.primary else palette.surface2,
                                            shape = RoundedCornerShape(999.dp),
                                            modifier = Modifier.clickable { exclusiveMember = m },
                                        ) {
                                            Text(m.displayName, color = if (sel) palette.onPrimary else palette.text, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp))
                                        }
                                    }
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = amountText,
                                    onValueChange = { amountText = it },
                                    label = { Text(if (assetTab == "coin52") "总 D 币" else "总 LDC 金额") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                )
                                if (ldcMode != "exclusive") {
                                    OutlinedTextField(
                                        value = countText,
                                        onValueChange = { countText = it },
                                        label = { Text("红包个数") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = promptText,
                                    onValueChange = { promptText = it },
                                    label = { Text("红包封面/祝福语") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1.7f),
                                )
                                OutlinedTextField(
                                    value = minutesText,
                                    onValueChange = { minutesText = it },
                                    label = { Text("有效(分)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }

                    // 4 Claim Gates (Native Open / Phrase / Cipher / 9-Grid Mosaic Image)
                    Surface(
                        color = palette.surface,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().border(1.dp, palette.line, RoundedCornerShape(14.dp)),
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("领取玩法门槛", color = palette.text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                                listOf(
                                    "open" to "直接开",
                                    "phrase" to "复读口令",
                                    "cipher" to "解谜挑战",
                                    "image" to "九宫猜图",
                                ).forEach { (g, lbl) ->
                                    val sel = gateMode == g
                                    Surface(
                                        color = if (sel) palette.primary else palette.surface2,
                                        shape = RoundedCornerShape(999.dp),
                                        modifier = Modifier.weight(1f).clickable { gateMode = g },
                                    ) {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 6.dp)) {
                                            Text(lbl, color = if (sel) palette.onPrimary else palette.text, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                        }
                                    }
                                }
                            }

                            when (gateMode) {
                                "phrase" -> {
                                    OutlinedTextField(
                                        value = phraseInput,
                                        onValueChange = { phraseInput = it },
                                        label = { Text("需复读的完整口令") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                                "cipher" -> {
                                    OutlinedTextField(
                                        value = secretInput,
                                        onValueChange = { secretInput = it },
                                        label = { Text("谜底答案（例如：守矢神社）") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                    Text("选择 6 种密文玩法规则：", color = palette.muted, fontSize = 12.sp)
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        RedpackMosaicEngine.CipherGameSpec.entries.forEach { spec ->
                                            val sel = cipherGame == spec.key
                                            Surface(
                                                color = if (sel) palette.primaryContainer else palette.surface2,
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier
                                                    .border(1.dp, if (sel) palette.primary else palette.line, RoundedCornerShape(10.dp))
                                                    .clickable { cipherGame = spec.key },
                                            ) {
                                                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                                    Text(spec.label, color = if (sel) palette.primary else palette.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                                    Text(spec.shortHint, color = palette.muted, fontSize = 10.sp)
                                                }
                                            }
                                        }
                                    }
                                    if (secretInput.isNotBlank()) {
                                        val prev = remember(secretInput, cipherGame) {
                                            RedpackMosaicEngine.previewCipherText(secretInput, cipherGame)
                                        }
                                        Surface(color = palette.surface2, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                                            Text("密文预览：$prev", color = palette.primary, fontSize = 12.sp, modifier = Modifier.padding(10.dp))
                                        }
                                    }
                                }
                                "image" -> {
                                    OutlinedTextField(
                                        value = secretInput,
                                        onValueChange = { secretInput = it },
                                        label = { Text("猜图答案") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                    OutlinedButton(
                                        onClick = {
                                            StealthExemptionGate.markInternalJump()
                                            imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(if (sourceBitmap == null) "选择原图并设置九宫格高清区域" else "更换图片")
                                    }
                                    val bmpPrev = previewBitmap
                                    if (bmpPrev != null) {
                                        Text(
                                            "轻触勾选至少 1 个清晰格，未勾选格子将在端侧降采样彻底打码（不可逆）后上传：",
                                            color = palette.muted,
                                            fontSize = 11.sp,
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(1f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .border(1.dp, palette.line, RoundedCornerShape(12.dp)),
                                        ) {
                                            Image(
                                                bitmap = bmpPrev.asImageBitmap(),
                                                contentDescription = "九宫格马赛克预览",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize(),
                                            )
                                            Column(modifier = Modifier.fillMaxSize()) {
                                                for (r in 0..2) {
                                                    Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                                        for (c in 0..2) {
                                                            val cellIdx = r * 3 + c
                                                            val isClear = (maskBits and (1 shl cellIdx)) != 0
                                                            Box(
                                                                contentAlignment = Alignment.Center,
                                                                modifier = Modifier
                                                                    .weight(1f)
                                                                    .fillMaxHeight()
                                                                    .border(1.dp, Color.White.copy(alpha = 0.7f))
                                                                    .background(if (isClear) Color.Transparent else Color.Black.copy(alpha = 0.25f))
                                                                    .clickable {
                                                                        val next = maskBits xor (1 shl cellIdx)
                                                                        if ((next and RedpackMosaicEngine.ALL_NINE_BITS) != 0) {
                                                                            maskBits = next
                                                                        }
                                                                    },
                                                            ) {
                                                                if (isClear) {
                                                                    Surface(color = palette.primary.copy(alpha = 0.82f), shape = RoundedCornerShape(999.dp)) {
                                                                        Text("清晰", color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
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
                        }
                    }

                    errText?.let { Text(it, color = palette.danger, fontSize = 12.sp) }

                    Button(
                        enabled = !busy,
                        onClick = {
                            if (gateMode == "phrase" && phraseInput.isBlank()) {
                                errText = "请填写复读口令"
                                return@Button
                            }
                            if ((gateMode == "cipher" || gateMode == "image") && secretInput.isBlank()) {
                                errText = "请填写挑战谜底"
                                return@Button
                            }
                            if (gateMode == "image" && sourceBitmap == null) {
                                errText = "请先选择猜图图片"
                                return@Button
                            }
                            if (ldcMode == "exclusive" && assetTab == "ldc" && exclusiveMember == null) {
                                errText = "请选择专属接收人"
                                return@Button
                            }
                            busy = true
                            errText = null
                            scope.launch {
                                var uploadedClueKey: String? = null
                                if (gateMode == "image" && sourceBitmap != null) {
                                    val webp = withContext(Dispatchers.Default) {
                                        RedpackMosaicEngine.encodeNineGridClueWebp(sourceBitmap!!, maskBits)
                                    }
                                    val upRes = api.uploadMediaBytes(webp, "clue.webp", "image/webp", forWhat = "chat")
                                    if (upRes.isFailure) {
                                        busy = false
                                        errText = upRes.exceptionOrNull()?.message ?: "马赛克线索图上传失败"
                                        return@launch
                                    }
                                    uploadedClueKey = upRes.getOrNull()?.key
                                }

                                val payload = buildJsonObject {
                                    val isNativeGate = gateMode != "open"
                                    put("provider", JsonPrimitive(if (assetTab == "coin52" && !isNativeGate) "coin52" else if (isNativeGate) "native" else "ldc"))
                                    if (assetTab == "coin52") {
                                        put("payment_provider", JsonPrimitive("coin52"))
                                        put("kind", JsonPrimitive(if (ldcMode == "fixed") "fixed" else "random"))
                                    } else {
                                        put("payment_provider", JsonPrimitive(ldcFundingChannel))
                                        put("ldc_kind", JsonPrimitive(if (ldcMode == "exclusive") "fixed" else ldcMode))
                                    }
                                    put("mode", JsonPrimitive(if (isNativeGate) gateMode else ldcMode))
                                    put("amount", JsonPrimitive(amountText.trim()))
                                    put("count", JsonPrimitive(if (ldcMode == "exclusive") 1 else (countText.toIntOrNull() ?: 5)))
                                    put("minutes", JsonPrimitive(minutesText.toIntOrNull() ?: 1440))
                                    put("prompt", JsonPrimitive(promptText.trim().ifBlank { "恭喜发财" }))

                                    if (assetTab == "ldc" && ldcMode == "exclusive" && exclusiveMember != null) {
                                        put("exclusive_user_id", JsonPrimitive(exclusiveMember!!.id))
                                    }
                                    if (isNativeGate) {
                                        put("ldc_reward", JsonPrimitive(true))
                                        put("ldc_amount", JsonPrimitive(amountText.trim()))
                                        put("ldc_count", JsonPrimitive(if (ldcMode == "exclusive") 1 else (countText.toIntOrNull() ?: 5)))
                                        put("ldc_minutes", JsonPrimitive(minutesText.toIntOrNull() ?: 1440))
                                        put("ldc_mode", JsonPrimitive(if (ldcMode == "fixed") "fixed" else "random"))
                                        when (gateMode) {
                                            "phrase" -> put("phrase", JsonPrimitive(phraseInput.trim()))
                                            "cipher" -> {
                                                put("game", JsonPrimitive(cipherGame))
                                                put("secret", JsonPrimitive(secretInput.trim()))
                                            }
                                            "image" -> {
                                                put("secret", JsonPrimitive(secretInput.trim()))
                                                uploadedClueKey?.let { put("clue_key", JsonPrimitive(it)) }
                                                put("mask_bits", JsonPrimitive(maskBits))
                                            }
                                        }
                                    }
                                }

                                val res = api.createGroupRedpack(groupId, payload)
                                busy = false
                                res.onSuccess { onCreated() }
                                    .onFailure { errText = it.message ?: "发送红包失败" }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = REDPACK_CRIMSON, contentColor = Color.White),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                    ) {
                        Text(if (busy) "正在塞进红包…" else "塞钱进红包", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Centered red-packet opening & result dialog (`02c §5`):
 * - `600ms` 3D Y-axis coin flip on open
 * - `一键复读并开`, 6 cipher games + interactive `tiles`, wrong-answer horizontal shake
 * - 9-grid zoomable mosaic image, Lottery enter, External HB/Fish/Credit WebView jump
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OpenRedpackCenteredDialog(
    messageId: Long,
    senderName: String?,
    fallbackMeta: RedpackMeta?,
    api: ForHappyApiClient,
    onDismiss: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onOpenGuessRoom: (Long) -> Unit,
    onOpenFullscreenImage: (String) -> Unit,
    onSendPhraseToChat: suspend (String) -> Unit,
    onOpenArchive: () -> Unit,
    onUpdated: () -> Unit,
) {
    val palette = LocalForHappyPalette.current
    val disguiseMode = LocalPrivacyStealthConfig.current.disguiseMode
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var loading by remember(messageId) { mutableStateOf(true) }
    var packetObj by remember(messageId) { mutableStateOf<JsonObject?>(null) }
    var answerInput by remember(messageId) { mutableStateOf("") }
    var statusMsg by remember(messageId) { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var coinSpinning by remember { mutableStateOf(false) }
    val shakeOffset = remember { Animatable(0f) }

    val coinRotation by animateFloatAsState(
        targetValue = if (coinSpinning) 1080f else 0f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "redpackCoinFlip",
    )

    suspend fun reloadPacket() {
        loading = true
        val res = api.fetchRedpackDetail(messageId)
        packetObj = res?.get("packet")?.let { runCatching { it.jsonObject }.getOrNull() } ?: res
        loading = false
    }

    suspend fun triggerWrongAnswerShake() {
        for (x in listOf(-18f, 18f, -12f, 12f, -6f, 6f, 0f)) {
            shakeOffset.animateTo(x, tween(durationMillis = 42))
        }
    }

    LaunchedEffect(messageId) {
        reloadPacket()
    }

    val p = packetObj
    val ldc = runCatching { p?.get("ldc")?.jsonObject }.getOrNull()
    val ldcMode = ldc?.get("mode")?.jsonPrimitive?.contentOrNull
    val coin52 = runCatching { p?.get("coin52")?.jsonObject }.getOrNull()
    val external = runCatching { p?.get("external")?.jsonObject }.getOrNull()
    val drawObj = runCatching { p?.get("draw")?.jsonObject }.getOrNull()
    val drawKind = drawObj?.get("kind")?.jsonPrimitive?.contentOrNull ?: fallbackMeta?.drawKind
    val mode = p?.get("mode")?.jsonPrimitive?.contentOrNull ?: fallbackMeta?.mode ?: "open"
    val game = p?.get("game")?.jsonPrimitive?.contentOrNull ?: fallbackMeta?.game
    val solved = p?.get("solved")?.jsonPrimitive?.booleanOrNull == true
    val openedCount = p?.get("opened")?.jsonPrimitive?.intOrNull ?: fallbackMeta?.opened ?: 0
    val prompt = p?.get("prompt")?.jsonPrimitive?.contentOrNull ?: fallbackMeta?.headline ?: "恭喜发财，大吉大利"
    val phrase = p?.get("phrase")?.jsonPrimitive?.contentOrNull
    val secret = p?.get("secret")?.jsonPrimitive?.contentOrNull
    val clueObj = runCatching { p?.get("clue")?.jsonObject }.getOrNull()
    val clueText = clueObj?.get("text")?.jsonPrimitive?.contentOrNull
    val clueTiles = runCatching { clueObj?.get("tiles")?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull } }.getOrNull().orEmpty()
    val clueImage = runCatching { p?.get("image")?.jsonObject?.get("u")?.jsonPrimitive?.contentOrNull }.getOrNull()
    val mineReward = ldc?.get("mine")?.jsonObject ?: coin52?.get("mine")?.jsonObject
    val recipients = runCatching { ldc?.get("recipients")?.jsonArray ?: coin52?.get("recipients")?.jsonArray }.getOrNull()

    fun executeClaim(overrideAnswer: String? = null, isLotteryEnter: Boolean = false) {
        if (busy) return
        busy = true
        coinSpinning = true
        statusMsg = null
        scope.launch {
            delay(480L)
            val res = when {
                isLotteryEnter -> api.postLdcRedpackAction(messageId, "enter")
                coin52 != null -> api.claimCoin52Redpack(messageId, overrideAnswer)
                ldc != null && mode == "open" -> api.postLdcRedpackAction(messageId, "claim", overrideAnswer)
                else -> api.openRedpack(messageId, overrideAnswer)
            }
            coinSpinning = false
            busy = false
            val err = res["error"]?.jsonPrimitive?.contentOrNull
            if (!err.isNullOrBlank()) {
                statusMsg = err
                if (mode in setOf("cipher", "image")) {
                    triggerWrongAnswerShake()
                }
            } else {
                statusMsg = res["msg"]?.jsonPrimitive?.contentOrNull ?: if (isLotteryEnter) "已参与定时抽奖！" else "拆包成功！"
                reloadPacket()
                onUpdated()
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = palette.bg,
            shape = RoundedCornerShape(22.dp),
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(shakeOffset.value.roundToInt(), 0) },
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Red & Gold Top Envelope Cover
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(REDPACK_DARK_RED, REDPACK_CRIMSON),
                            ),
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.TopEnd).size(28.dp),
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "关闭", tint = Color.White)
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 6.dp),
                    ) {
                        if (!senderName.isNullOrBlank()) {
                            Text("来自 $senderName 的红包", color = REDPACK_GOLD, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                        Text(
                            text = DisguiseSanitizer.sanitizeSummaryForDisguise(prompt, disguiseMode),
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        )
                        if (fallbackMeta?.formattedAmount != null) {
                            Text(
                                text = DisguiseSanitizer.sanitizeSummaryForDisguise(fallbackMeta.formattedAmount!!, disguiseMode),
                                color = REDPACK_GOLD,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }

                if (loading) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = REDPACK_CRIMSON)
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        when {
                            ldcMode in setOf("guess", "clues") -> {
                                Button(
                                    onClick = { onOpenGuessRoom(messageId) },
                                    colors = ButtonDefaults.buttonColors(containerColor = palette.primary, contentColor = palette.onPrimary),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text(if (disguiseMode != AppDisguiseMode.NONE) "进入群待办详情" else "进入欢欢猜词屋")
                                }
                            }
                            external != null -> {
                                val extUrl = external["url"]?.jsonPrimitive?.contentOrNull
                                val refCode = external["ref"]?.jsonPrimitive?.contentOrNull
                                val platName = if (disguiseMode != AppDisguiseMode.NONE) {
                                    "外部口令"
                                } else {
                                    RedpackMosaicEngine.ExternalPacketPlatform.fromKey(fallbackMeta?.provider).label
                                }
                                Surface(
                                    color = palette.surface,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().border(1.dp, palette.line, RoundedCornerShape(12.dp)),
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        BadgePill(platName, containerColor = REDPACK_CRIMSON.copy(alpha = 0.14f), contentColor = REDPACK_CRIMSON)
                                        if (!refCode.isNullOrBlank()) {
                                            Text("口令：$refCode", color = palette.text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                        }
                                        if (!extUrl.isNullOrBlank()) {
                                            Text(extUrl, color = palette.primary, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    if (!refCode.isNullOrBlank()) {
                                        OutlinedButton(
                                            onClick = {
                                                clipboard.setText(AnnotatedString(refCode))
                                                statusMsg = "已复制红包口令"
                                            },
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("复制口令", fontSize = 12.sp)
                                        }
                                    }
                                    Button(
                                        onClick = {
                                            val target = extUrl?.takeIf { it.isNotBlank() }
                                                ?: RedpackMosaicEngine.ExternalPacketPlatform.fromKey(fallbackMeta?.provider).homeUrl
                                            onDismiss()
                                            onOpenUrl(target)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = REDPACK_CRIMSON, contentColor = Color.White),
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Icon(Icons.Rounded.OpenInBrowser, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("前往领取", fontSize = 12.sp)
                                    }
                                }
                            }
                            else -> {
                                if (!clueImage.isNullOrBlank()) {
                                    val fullImgUrl = MediaUrlRewriter.rewriteMediaUrl(clueImage)
                                    AsyncImage(
                                        model = fullImgUrl,
                                        imageLoader = api.imageLoader,
                                        contentDescription = "猜图线索",
                                        contentScale = ContentScale.FillWidth,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { onOpenFullscreenImage(fullImgUrl) },
                                    )
                                }
                                if (!clueText.isNullOrBlank()) {
                                    Surface(color = palette.surface2, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            text = buildString {
                                                game?.let { g ->
                                                    append("[${RedpackMosaicEngine.CipherGameSpec.fromKey(g).label}] ")
                                                }
                                                append(clueText)
                                            },
                                            color = palette.text,
                                            fontSize = 13.sp,
                                            modifier = Modifier.padding(10.dp),
                                        )
                                    }
                                }
                                if (clueTiles.isNotEmpty() && !solved) {
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                                        verticalArrangement = Arrangement.spacedBy(5.dp),
                                    ) {
                                        clueTiles.forEach { tile ->
                                            Surface(
                                                color = palette.surface2,
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier
                                                    .border(1.dp, palette.line, RoundedCornerShape(8.dp))
                                                    .clickable { answerInput += tile },
                                            ) {
                                                Text(tile, color = palette.text, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp))
                                            }
                                        }
                                    }
                                }

                                mineReward?.let { mine ->
                                    Surface(
                                        color = REDPACK_GOLD.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        ) {
                                            Text("恭喜领到", color = palette.muted, fontSize = 11.sp)
                                            Text(
                                                text = mine["amount"]?.jsonPrimitive?.contentOrNull.orEmpty().ifBlank { "已拆封" },
                                                color = REDPACK_CRIMSON,
                                                fontSize = 22.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                            )
                                        }
                                    }
                                }

                                if (solved) {
                                    Surface(color = palette.primaryContainer, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text("已拆封 · $openedCount 人已开", color = palette.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            if (!secret.isNullOrBlank()) {
                                                Text("谜底 / 内容：$secret", color = palette.text, fontSize = 13.sp)
                                            }
                                        }
                                    }
                                }

                                // Claim gates when open & not yet claimed
                                if (!solved && mineReward == null) {
                                    when {
                                        drawKind == "lottery" -> {
                                            Button(
                                                enabled = !busy,
                                                onClick = { executeClaim(isLotteryEnter = true) },
                                                colors = ButtonDefaults.buttonColors(containerColor = REDPACK_CRIMSON, contentColor = Color.White),
                                                modifier = Modifier.fillMaxWidth(),
                                            ) {
                                                Text(if (busy) "报名中…" else "参与定时抽奖")
                                            }
                                        }
                                        mode == "phrase" && !phrase.isNullOrBlank() -> {
                                            Surface(color = palette.surface2, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                                                Text("复读口令：$phrase", color = palette.text, fontSize = 13.sp, modifier = Modifier.padding(10.dp))
                                            }
                                            Button(
                                                enabled = !busy,
                                                onClick = {
                                                    scope.launch { onSendPhraseToChat(phrase) }
                                                    executeClaim(overrideAnswer = phrase)
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = REDPACK_CRIMSON, contentColor = Color.White),
                                                modifier = Modifier.fillMaxWidth(),
                                            ) {
                                                Text(if (busy) "开包中…" else "一键复读并开")
                                            }
                                        }
                                        mode == "cipher" || mode == "image" -> {
                                            OutlinedTextField(
                                                value = answerInput,
                                                onValueChange = { answerInput = it },
                                                label = { Text("输入谜底答案") },
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth(),
                                            )
                                            Button(
                                                enabled = answerInput.isNotBlank() && !busy,
                                                onClick = { executeClaim(overrideAnswer = answerInput.trim()) },
                                                colors = ButtonDefaults.buttonColors(containerColor = REDPACK_CRIMSON, contentColor = Color.White),
                                                modifier = Modifier.fillMaxWidth(),
                                            ) {
                                                Text(if (busy) "验证中…" else "提交答案拆红包")
                                            }
                                        }
                                        else -> {
                                            // Classic 80dp Gold Coin 600ms 3D Spin Button
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                            ) {
                                                Surface(
                                                    color = REDPACK_GOLD,
                                                    shape = CircleShape,
                                                    shadowElevation = 6.dp,
                                                    modifier = Modifier
                                                        .size(80.dp)
                                                        .graphicsLayer {
                                                            rotationY = coinRotation
                                                            cameraDistance = 12f * density
                                                        }
                                                        .clickable(enabled = !busy) { executeClaim() },
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Text(
                                                            text = "開",
                                                            color = REDPACK_DARK_RED,
                                                            fontSize = 30.sp,
                                                            fontWeight = FontWeight.ExtraBold,
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Recipients & Lucky King
                                if (!recipients.isNullOrEmpty()) {
                                    HorizontalDivider(color = palette.line.copy(alpha = 0.6f))
                                    val maxAmount = recipients.maxOfOrNull {
                                        it.jsonObject["amount_cents"]?.jsonPrimitive?.doubleOrNull
                                            ?: it.jsonObject["amount"]?.jsonPrimitive?.contentOrNull?.filter { c -> c.isDigit() || c == '.' }?.toDoubleOrNull()
                                            ?: 0.0
                                    } ?: 0.0
                                    Text("领取记录 (${recipients.size})", color = palette.muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    recipients.forEach { el ->
                                        val r = el.jsonObject
                                        val who = r["name"]?.jsonPrimitive?.contentOrNull ?: r["username"]?.jsonPrimitive?.contentOrNull ?: "群友"
                                        val amtStr = r["amount"]?.jsonPrimitive?.contentOrNull.orEmpty()
                                        val amtVal = r["amount_cents"]?.jsonPrimitive?.doubleOrNull
                                            ?: amtStr.filter { c -> c.isDigit() || c == '.' }.toDoubleOrNull()
                                            ?: 0.0
                                        val isKing = recipients.size > 1 && maxAmount > 0.0 && amtVal >= maxAmount
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                                Text(who, color = palette.text, fontSize = 12.sp)
                                                if (isKing) {
                                                    Text("👑 手气王", color = REDPACK_GOLD, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                            Text(amtStr, color = palette.gold, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }
                        }

                        statusMsg?.let {
                            Text(it, color = REDPACK_CRIMSON, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = {
                                onDismiss()
                                onOpenArchive()
                            }) {
                                Text("查看本群红包归档 / 土豪榜 ›", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Group red-packet archive & sender leaderboard sheet (`02c §5`).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupRedpackArchiveSheet(
    groupId: Long,
    api: ForHappyApiClient,
    onDismiss: () -> Unit,
    onSelectPacketMsgId: (Long) -> Unit,
) {
    val palette = LocalForHappyPalette.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var tab by remember { mutableStateOf("archive") } // "archive" | "stats"
    var loading by remember { mutableStateOf(true) }
    var archives by remember { mutableStateOf<List<RedpackArchiveItem>>(emptyList()) }
    var stats by remember { mutableStateOf<List<SenderStatItem>>(emptyList()) }

    LaunchedEffect(groupId, tab) {
        loading = true
        if (tab == "archive") {
            archives = api.fetchGroupRedpacksArchive(groupId)
        } else {
            stats = api.fetchGroupSenderStats(groupId, period = "month", metric = "amount")
        }
        loading = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.bg,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.84f)
                .navigationBarsPadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.surface)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Rounded.CardGiftcard, contentDescription = null, tint = REDPACK_CRIMSON)
                Text("本群红包与土豪榜", color = palette.text, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Rounded.Close, contentDescription = "关闭", tint = palette.text)
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.surface)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf("archive" to "红包归档", "stats" to "月度土豪榜").forEach { (k, lbl) ->
                    val sel = tab == k
                    Surface(
                        color = if (sel) REDPACK_CRIMSON else palette.surface2,
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier.weight(1f).clickable { tab = k },
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 7.dp)) {
                            Text(lbl, color = if (sel) Color.White else palette.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            HorizontalDivider(color = palette.line)

            if (loading) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = palette.primary)
                }
            } else if (tab == "archive") {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (archives.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("本群暂无红包归档", color = palette.muted, fontSize = 13.sp)
                            }
                        }
                    } else {
                        items(archives, key = { "arch_${it.id}_${it.messageId}" }) { item ->
                            Surface(
                                color = palette.surface,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, palette.line.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                                    .clickable(enabled = item.messageId > 0L) { onSelectPacketMsgId(item.messageId) },
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(item.displayTitle, color = palette.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            text = "${item.senderName ?: "群友"} · 已拆 ${item.opened}/${item.count} · ${item.state}",
                                            color = palette.muted,
                                            fontSize = 11.sp,
                                        )
                                    }
                                    if (item.totalText.isNotBlank()) {
                                        Text("${item.totalText} ${item.currency.orEmpty()}".trim(), color = palette.gold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (stats.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("本月暂无发包榜单数据", color = palette.muted, fontSize = 13.sp)
                            }
                        }
                    } else {
                        items(stats.size) { idx ->
                            val s = stats[idx]
                            Surface(
                                color = palette.surface,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().border(1.dp, palette.line.copy(alpha = 0.7f), RoundedCornerShape(12.dp)),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text("#${idx + 1}", color = if (idx < 3) palette.gold else palette.muted, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(28.dp))
                                    ForHappyAvatar(username = s.displayName, avatarUrl = s.avatar, imageLoader = api.imageLoader, size = 30.dp)
                                    Text(s.displayName, color = palette.text, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                                    Column(horizontalAlignment = Alignment.End) {
                                        if (s.totalText.isNotBlank()) {
                                            Text("${s.totalText} ${s.currency.orEmpty()}".trim(), color = palette.gold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Text("${s.count} 个红包", color = palette.muted, fontSize = 10.sp)
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
