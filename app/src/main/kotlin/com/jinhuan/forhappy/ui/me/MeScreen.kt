package com.jinhuan.forhappy.ui.me

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jinhuan.forhappy.BuildConfig
import com.jinhuan.forhappy.core.model.AppIconAlias
import com.jinhuan.forhappy.core.model.AppThemeMode
import com.jinhuan.forhappy.core.model.CustomEdgeNode
import com.jinhuan.forhappy.core.model.FolderItem
import com.jinhuan.forhappy.core.model.MeUser
import com.jinhuan.forhappy.core.model.NotifItem
import com.jinhuan.forhappy.core.model.Owner52bCard
import com.jinhuan.forhappy.core.model.WalletSummary
import com.jinhuan.forhappy.core.network.api.ForHappyApiClient
import com.jinhuan.forhappy.core.update.AppUpdateController
import com.jinhuan.forhappy.core.update.AppUpdateManager
import com.jinhuan.forhappy.core.update.UpdateStatus
import com.jinhuan.forhappy.ui.components.BadgePill
import com.jinhuan.forhappy.ui.components.ForHappyAvatar
import com.jinhuan.forhappy.ui.components.SectionCard
import com.jinhuan.forhappy.ui.theme.LocalForHappyPalette
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import java.text.NumberFormat
import java.util.Locale

/**
 * Profile, Wallet, Notifications, Privacy/Disguise Studio, and Cloudflare Rust Gateway Diagnostics Console.
 */
@Composable
fun MeScreen(
    api: ForHappyApiClient,
    updateController: AppUpdateController,
    me: MeUser?,
    themeMode: AppThemeMode,
    privacyShieldEnabled: Boolean,
    activeIconAlias: AppIconAlias,
    onChangeTheme: (AppThemeMode) -> Unit,
    onTogglePrivacyShield: (Boolean) -> Unit,
    onChangeIconAlias: (AppIconAlias) -> Unit,
    onRequireLogin: () -> Unit,
    onLoggedOut: () -> Unit,
    onTriggerBossKey: () -> Unit,
) {
    val palette = LocalForHappyPalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val netStatus by api.rustNetManager.status.collectAsState()
    val netConfig by api.rustNetManager.config.collectAsState()
    val updateState by updateController.state.collectAsState()

    var stats by remember { mutableStateOf<JsonObject?>(null) }
    var wallet by remember { mutableStateOf<WalletSummary?>(null) }
    var cards52b by remember { mutableStateOf<List<Owner52bCard>>(emptyList()) }
    var folders by remember { mutableStateOf<List<FolderItem>>(emptyList()) }
    var notifs by remember { mutableStateOf<List<NotifItem>>(emptyList()) }
    var customIpInput by remember { mutableStateOf("") }
    var showCustomMirrorEditor by remember { mutableStateOf(false) }
    var customMirrorInput by remember(updateState.customMirrorPrefix) { mutableStateOf(updateState.customMirrorPrefix) }
    var cacheBytes by remember { mutableStateOf(api.webAssetCache.cacheSizeBytes()) }
    var consoleMsg by remember { mutableStateOf<String?>(null) }
    var showLedger by remember { mutableStateOf(false) }

    val statLabels = remember {
        mapOf(
            "fav" to "收藏",
            "like" to "喜欢",
            "reply" to "回应",
            "comment" to "评论",
            "post" to "发布",
            "seen" to "看过",
        )
    }

    LaunchedEffect(me?.id) {
        if (me != null) {
            stats = api.fetchMeStats()
            wallet = api.fetchWallet()
            cards52b = api.fetchOwner52bCards()
            folders = api.fetchFolders()
            notifs = api.fetchNotifs()
        } else {
            stats = null
            wallet = null
            cards52b = emptyList()
            folders = emptyList()
            notifs = emptyList()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.bg),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Account card
        item {
            SectionCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        ForHappyAvatar(
                            username = me?.displayName ?: "guest",
                            avatarUrl = me?.avatar,
                            imageLoader = api.imageLoader,
                            size = 52.dp,
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = me?.displayName ?: "尚未登录",
                                color = palette.text,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = me?.bio?.takeIf { it.isNotBlank() }
                                    ?: if (me != null) "@${me.username}" else "点击登录 52dmeme.com 解锁全部同步功能",
                                color = palette.muted,
                                fontSize = 12.sp,
                            )
                        }
                    }

                    if (me == null) {
                        Button(
                            onClick = onRequireLogin,
                            colors = ButtonDefaults.buttonColors(containerColor = palette.primary, contentColor = palette.onPrimary),
                        ) {
                            Text("去登录")
                        }
                    } else {
                        OutlinedButton(onClick = {
                            scope.launch {
                                api.logout()
                                onLoggedOut()
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = "登出")
                        }
                    }
                }

                if (me != null) {
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = palette.line)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showLedger = !showLedger },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "D币余额：${NumberFormat.getNumberInstance(Locale.CHINA).format(wallet?.resolvedCoins ?: 0L)}",
                            color = palette.gold,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "收藏夹 ${folders.size} 个 · 52B卡 ${cards52b.count { it.owned }}/${cards52b.size}",
                            color = palette.muted,
                            fontSize = 12.sp,
                        )
                    }
                    stats?.let { st ->
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = st.entries.take(6).joinToString(" · ") {
                                "${statLabels[it.key] ?: it.key} ${it.value}"
                            },
                            color = palette.muted,
                            fontSize = 11.sp,
                        )
                    }
                    if (showLedger && !wallet?.rows.isNullOrEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        wallet!!.rows.take(5).forEach { row ->
                            val sign = if (row.delta > 0) "+" else ""
                            Text(
                                text = "• ${row.kind}: ${sign}${row.delta.toLong()} (余额 ${row.balanceAfter.toLong()})",
                                color = palette.muted,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(vertical = 1.dp),
                            )
                        }
                    }
                }
            }
        }

        // App version & GitHub Release auto-update card
        item {
            SectionCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.SystemUpdate, contentDescription = null, tint = palette.primary)
                        Spacer(Modifier.width(6.dp))
                        Text("版本与自动更新", color = palette.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                    BadgePill(
                        text = "v${BuildConfig.VERSION_NAME}",
                        containerColor = palette.surface2,
                        contentColor = palette.primary,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = updateState.statusMessage,
                    color = if (updateState.status == UpdateStatus.ERROR) palette.danger else palette.muted,
                    fontSize = 12.sp,
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "加速通道：${if (updateState.customMirrorPrefix.isNotBlank()) "自定义源优先 + " else ""}${updateState.activeMirrorCount} 个公益镜像并发竞速",
                        color = palette.muted,
                        fontSize = 11.sp,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = if (showCustomMirrorEditor) "收起" else "自定义加速源",
                        color = palette.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clickable { showCustomMirrorEditor = !showCustomMirrorEditor }
                            .padding(start = 8.dp, top = 2.dp, bottom = 2.dp),
                    )
                }

                if (showCustomMirrorEditor) {
                    Spacer(Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        OutlinedTextField(
                            value = customMirrorInput,
                            onValueChange = { customMirrorInput = it },
                            placeholder = { Text("https://example.com/https://github.com（留空自动）", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        Button(
                            onClick = {
                                updateController.saveCustomMirrorPrefix(customMirrorInput)
                                showCustomMirrorEditor = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = palette.primary, contentColor = palette.onPrimary),
                        ) {
                            Text("保存", fontSize = 12.sp)
                        }
                        if (updateState.customMirrorPrefix.isNotBlank() || customMirrorInput.isNotBlank()) {
                            OutlinedButton(
                                onClick = {
                                    customMirrorInput = ""
                                    updateController.saveCustomMirrorPrefix("")
                                    showCustomMirrorEditor = false
                                },
                            ) {
                                Text("清空", fontSize = 12.sp)
                            }
                        }
                    }
                }

                if (updateState.status == UpdateStatus.DOWNLOADING) {
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { (updateState.downloadProgressPercent / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                        color = palette.primary,
                        trackColor = palette.surface2,
                    )
                }

                val rel = updateState.latestRelease
                if (rel != null && rel.releaseNotes.isNotBlank() && updateState.status != UpdateStatus.UP_TO_DATE) {
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        color = palette.surface2,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(rel.releaseTitle, color = palette.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = rel.releaseNotes,
                                color = palette.muted,
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                maxLines = 6,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        enabled = updateState.status != UpdateStatus.CHECKING && updateState.status != UpdateStatus.DOWNLOADING,
                        onClick = { updateController.checkForUpdates(silent = false) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = if (updateState.status == UpdateStatus.CHECKING) "检查中…" else "检查更新",
                            fontSize = 12.sp,
                        )
                    }

                    if (updateState.status == UpdateStatus.AVAILABLE || (updateState.status == UpdateStatus.ERROR && rel != null)) {
                        Button(
                            onClick = { updateController.downloadAndInstall() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = palette.primary, contentColor = palette.onPrimary),
                        ) {
                            Text("下载并更新", fontSize = 12.sp)
                        }
                    } else if (updateState.status == UpdateStatus.READY_TO_INSTALL) {
                        Button(
                            onClick = { updateController.installReadyApk() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = palette.primary, contentColor = palette.onPrimary),
                        ) {
                            Text("立即安装", fontSize = 12.sp)
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                val targetUrl = rel?.htmlUrl?.takeIf { it.isNotBlank() } ?: AppUpdateManager.RELEASES_HTML_URL
                                runCatching {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl))
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("GitHub Release", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Notifications summary
        if (me != null && notifs.isNotEmpty()) {
            item {
                SectionCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Notifications, contentDescription = null, tint = palette.primary)
                            Spacer(Modifier.width(6.dp))
                            Text("通知中心 (${notifs.count { it.isUnread }} 未读)", color = palette.text, fontWeight = FontWeight.SemiBold)
                        }
                        OutlinedButton(onClick = {
                            scope.launch {
                                api.markAllNotifsRead()
                                notifs = api.fetchNotifs()
                            }
                        }) {
                            Text("全部已读", fontSize = 11.sp)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    notifs.take(4).forEach { n ->
                        Text(
                            text = "• ${n.displayLine}",
                            color = if (!n.isUnread) palette.muted else palette.text,
                            fontSize = 12.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(vertical = 2.dp),
                        )
                    }
                }
            }
        }

        // Privacy / Disguise Studio
        item {
            SectionCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Security, contentDescription = null, tint = palette.primary)
                        Spacer(Modifier.width(6.dp))
                        Text("界面伪装与隐私防护", color = palette.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(onClick = onTriggerBossKey) {
                        Text("一键老板键", fontSize = 11.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))

                Text("视觉主题", color = palette.muted, fontSize = 12.sp)
                Spacer(Modifier.height(4.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    AppThemeMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onChangeTheme(mode) }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(mode.label, color = palette.text, fontSize = 13.sp)
                            if (themeMode == mode) {
                                BadgePill("当前", containerColor = palette.primary, contentColor = palette.onPrimary)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = palette.line)
                Spacer(Modifier.height(8.dp))

                Text("桌面启动器图标伪装（切换后桌面数秒内刷新）", color = palette.muted, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AppIconAlias.entries.forEach { alias ->
                        FilterChip(
                            selected = activeIconAlias == alias,
                            onClick = {
                                switchLauncherAlias(context, alias)
                                onChangeIconAlias(alias)
                            },
                            label = { Text(alias.title, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = palette.primary,
                                selectedLabelColor = palette.onPrimary,
                            ),
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("防截屏 / 后台任务预览隐私模糊", color = palette.text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text("开启 FLAG_SECURE，切换后台时不显示真实内容缩略图", color = palette.muted, fontSize = 11.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(checked = privacyShieldEnabled, onCheckedChange = onTogglePrivacyShield)
                }
            }
        }

        // Cloudflare Rust JNI Acceleration & Diagnostics Console
        item {
            SectionCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.Speed, contentDescription = null, tint = palette.primary)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Cloudflare Rust 加速内核",
                            color = palette.text,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    BadgePill(
                        text = if (netStatus.running) "已就绪 :${netStatus.port}" else netStatus.phase,
                        containerColor = if (netStatus.running) palette.primary.copy(alpha = 0.16f) else palette.danger.copy(alpha = 0.16f),
                        contentColor = if (netStatus.running) palette.primary else palette.danger,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "优选 Edge IP：${netStatus.winnerIp ?: "测速中…"}  (${netStatus.latencyMs?.let { "${it} ms" } ?: "--"})",
                    color = palette.text,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = "ECH：${if (netStatus.echAccepted) "服务端已接受 (${netStatus.echKeyCount} 钥匙)" else "待协商 (${netStatus.echKeyCount} keys)"} · 来源：${netStatus.winnerSource ?: "--"}",
                    color = palette.muted,
                    fontSize = 12.sp,
                )
                netStatus.lastError?.let { err ->
                    Text("最近诊断：$err", color = palette.danger, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                }

                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("启用 ECH (Encrypted ClientHello)", color = palette.text, fontSize = 13.sp)
                    Switch(
                        checked = netConfig.echEnabled,
                        onCheckedChange = { checked ->
                            api.rustNetManager.updateSettings(echEnabled = checked)
                        },
                    )
                }

                Spacer(Modifier.height(6.dp))
                Text("IPv6 策略", color = palette.muted, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                    listOf("auto" to "双栈自动", "prefer_v6" to "优先 IPv6", "prefer_v4" to "仅/优先 IPv4").forEach { (id, label) ->
                        FilterChip(
                            selected = netConfig.ipv6Mode == id,
                            onClick = { api.rustNetManager.updateSettings(ipv6Mode = id) },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = palette.primary,
                                selectedLabelColor = palette.onPrimary,
                            ),
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = customIpInput,
                        onValueChange = { customIpInput = it },
                        placeholder = { Text("添加自定义优选 CF IP", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    Button(
                        enabled = customIpInput.isNotBlank(),
                        onClick = {
                            val ip = customIpInput.trim()
                            val next = netConfig.customNodes + CustomEdgeNode(ip = ip, note = "手动添加")
                            api.rustNetManager.updateSettings(customNodes = next)
                            customIpInput = ""
                        },
                    ) {
                        Text("添加", fontSize = 12.sp)
                    }
                }

                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            api.rustNetManager.triggerReselect()
                            consoleMsg = "已触发 Rust 并发赛马重选…"
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = palette.primary, contentColor = palette.onPrimary),
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("重新优选节点", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                api.webAssetCache.clearAll()
                                cacheBytes = api.webAssetCache.cacheSizeBytes()
                                consoleMsg = "已清理静态网页缓存"
                            }
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("清缓存 (${cacheBytes / 1024}KB)", fontSize = 12.sp)
                    }
                }
                consoleMsg?.let { Text(it, color = palette.primary, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp)) }

                if (netStatus.logs.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Surface(
                        color = palette.surface2,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("Rust 内核日志 (最近 ${netStatus.logs.size} 条)", color = palette.muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            netStatus.logs.takeLast(8).reversed().forEach { line ->
                                Text(
                                    text = line,
                                    color = palette.text,
                                    fontSize = 10.sp,
                                    lineHeight = 14.sp,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun switchLauncherAlias(context: Context, selected: AppIconAlias) {
    val pm = context.packageManager
    val pkg = context.packageName
    for (alias in AppIconAlias.entries) {
        val state = if (alias == selected) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }
        runCatching {
            pm.setComponentEnabledSetting(
                ComponentName(pkg, alias.aliasClass),
                state,
                PackageManager.DONT_KILL_APP,
            )
        }
    }
}
