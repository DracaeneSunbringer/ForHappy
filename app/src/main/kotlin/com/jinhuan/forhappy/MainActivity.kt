package com.jinhuan.forhappy

import android.app.ActivityManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.jinhuan.forhappy.core.model.AppDisguiseMode
import com.jinhuan.forhappy.core.model.AppIconAlias
import com.jinhuan.forhappy.core.model.AppThemeMode
import com.jinhuan.forhappy.core.model.ForHappyJson
import com.jinhuan.forhappy.core.model.PrivacyStealthConfig
import com.jinhuan.forhappy.core.model.ShortcutGestureMode
import com.jinhuan.forhappy.core.navigation.StealthExemptionGate
import com.jinhuan.forhappy.core.network.scheduler.BackgroundGate
import com.jinhuan.forhappy.ui.disguise.DeviceStealthSensorObserver
import com.jinhuan.forhappy.ui.disguise.VolumeDoubleTapDetector
import com.jinhuan.forhappy.ui.navigation.ForHappyShell
import com.jinhuan.forhappy.ui.theme.ForHappyTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private data class InitialUiSettings(
        val themeMode: AppThemeMode,
        val privacyConfig: PrivacyStealthConfig,
        val privacyShield: Boolean,
        val iconAlias: AppIconAlias,
    )

    private lateinit var app: ForHappyApp
    private val volumeDetector = VolumeDoubleTapDetector()
    private val incomingIntentChannel = Channel<Intent>(capacity = Channel.BUFFERED)
    val incomingIntents = incomingIntentChannel.receiveAsFlow()

    private var volumeActionHandler: ((ShortcutGestureMode) -> Boolean)? = null
    private var backgroundLeaveHandler: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        app = application as ForHappyApp

        val initial = loadInitialUiSettings()
        if (initial.privacyShield) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        syncRecentsTaskDescription(initial.iconAlias)
        if (savedInstanceState == null) {
            intent?.let { incomingIntentChannel.trySend(it) }
        }

        setContent {
            val scope = rememberCoroutineScope()
            var themeMode by remember { mutableStateOf(initial.themeMode) }
            var privacyConfig by remember { mutableStateOf(initial.privacyConfig) }
            var privacyShield by remember { mutableStateOf(initial.privacyShield) }
            var iconAlias by remember { mutableStateOf(initial.iconAlias) }
            var bossKeyActive by remember { mutableStateOf(initial.privacyConfig.isBossPersisted) }

            fun persistPrivacyConfig(next: PrivacyStealthConfig, blocking: Boolean = false) {
                privacyConfig = next
                val encoded = ForHappyJson.encodeToString(PrivacyStealthConfig.serializer(), next)
                if (blocking) {
                    app.db.putKvBlocking(KV_PRIVACY_STEALTH_V2, encoded)
                } else {
                    scope.launch {
                        app.db.putKv(KV_PRIVACY_STEALTH_V2, encoded)
                    }
                }
            }

            fun enterBossKey(blocking: Boolean = false) {
                val alreadyBoss = bossKeyActive
                val cur = privacyConfig
                bossKeyActive = true
                val next = cur.copy(
                    isBossPersisted = true,
                    bossReturnDisguise = if (alreadyBoss) cur.bossReturnDisguise else cur.disguiseMode,
                )
                persistPrivacyConfig(next, blocking = blocking)
            }

            fun exitBossKey() {
                val cur = privacyConfig
                bossKeyActive = false
                val next = cur.copy(
                    isBossPersisted = false,
                    disguiseMode = cur.bossReturnDisguise,
                    blurAllEnabled = if (cur.keepBlurAfterBossExit) true else cur.blurAllEnabled,
                )
                persistPrivacyConfig(next, blocking = false)
            }

            fun switchDisguiseMode(nextMode: AppDisguiseMode) {
                val cur = privacyConfig
                if (cur.disguiseMode == nextMode) return
                val enteringDisguise = cur.disguiseMode == AppDisguiseMode.NONE && nextMode != AppDisguiseMode.NONE
                val leavingDisguise = cur.disguiseMode != AppDisguiseMode.NONE && nextMode == AppDisguiseMode.NONE
                val updated = when {
                    enteringDisguise -> cur.copy(
                        disguiseMode = nextMode,
                        blurBeforeDisguise = cur.blurAllEnabled,
                        blurAllEnabled = if (cur.autoBlurOnEnterDisguise) true else cur.blurAllEnabled,
                    )
                    leavingDisguise -> cur.copy(
                        disguiseMode = AppDisguiseMode.NONE,
                        blurAllEnabled = if (cur.autoBlurOnEnterDisguise) cur.blurBeforeDisguise else cur.blurAllEnabled,
                    )
                    else -> cur.copy(disguiseMode = nextMode)
                }
                persistPrivacyConfig(updated)
            }

            volumeActionHandler = { gesture ->
                var handled = false
                if (privacyConfig.bossVolumeShortcut == gesture) {
                    if (bossKeyActive) exitBossKey() else enterBossKey()
                    handled = true
                }
                if (!bossKeyActive && privacyConfig.blurShortcut == gesture) {
                    persistPrivacyConfig(privacyConfig.copy(blurAllEnabled = !privacyConfig.blurAllEnabled))
                    handled = true
                }
                if (handled && privacyConfig.stealthHapticFeedback) {
                    window.decorView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                }
                handled
            }

            backgroundLeaveHandler = {
                val cur = privacyConfig
                val shouldBlur = cur.autoBlurOnBackground && !cur.blurAllEnabled
                val shouldBoss = cur.bossOnBackgroundResume && !bossKeyActive
                if (shouldBlur || shouldBoss) {
                    val next = cur.copy(
                        blurAllEnabled = if (shouldBlur) true else cur.blurAllEnabled,
                        isBossPersisted = if (shouldBoss) true else cur.isBossPersisted,
                        bossReturnDisguise = if (shouldBoss) cur.disguiseMode else cur.bossReturnDisguise,
                    )
                    if (shouldBoss) {
                        bossKeyActive = true
                    }
                    persistPrivacyConfig(next, blocking = true)
                }
            }

            DeviceStealthSensorObserver(
                flipFaceDownEnabled = privacyConfig.bossFlipFaceDown && !bossKeyActive,
                shakeBlurEnabled = privacyConfig.blurShortcut == ShortcutGestureMode.SHAKE && !bossKeyActive,
                onFlipFaceDown = { enterBossKey() },
                onShake = {
                    persistPrivacyConfig(privacyConfig.copy(blurAllEnabled = !privacyConfig.blurAllEnabled))
                },
            )

            // Keep FLAG_SECURE invariant whenever enabled (never strip during disguise or Boss Key)
            LaunchedEffect(privacyShield) {
                if (privacyShield) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }

            ForHappyTheme(
                mode = themeMode,
                privacyConfig = privacyConfig,
                seenTracker = app.seenTracker,
                bossKeyActive = bossKeyActive,
            ) {
                ForHappyShell(
                    app = app,
                    themeMode = themeMode,
                    privacyConfig = privacyConfig,
                    bossKeyActive = bossKeyActive,
                    privacyShieldEnabled = privacyShield,
                    activeIconAlias = iconAlias,
                    incomingIntents = incomingIntents,
                    onChangeTheme = { next ->
                        themeMode = next
                        scope.launch { app.db.putKv("ui.theme_mode", next.name) }
                    },
                    onChangeDisguise = ::switchDisguiseMode,
                    onUpdatePrivacyConfig = { next -> persistPrivacyConfig(next) },
                    onSetBossKeyActive = { active ->
                        if (active) enterBossKey() else exitBossKey()
                    },
                    onTogglePrivacyShield = { enabled ->
                        privacyShield = enabled
                        scope.launch { app.db.putKv("ui.privacy_shield", enabled.toString()) }
                    },
                    onChangeIconAlias = { next ->
                        iconAlias = next
                        syncRecentsTaskDescription(next)
                        scope.launch { app.db.putKv("ui.icon_alias", next.name) }
                    },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        incomingIntentChannel.trySend(intent)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (event != null && event.repeatCount == 0) {
            val mode = when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_DOWN -> ShortcutGestureMode.VOL_DOUBLE_DOWN
                KeyEvent.KEYCODE_VOLUME_UP -> ShortcutGestureMode.VOL_DOUBLE_UP
                else -> null
            }
            if (mode != null) {
                val triggered = volumeDetector.onVolumeKey(mode, SystemClock.uptimeMillis())
                if (triggered != null && volumeActionHandler?.invoke(triggered) == true) {
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onStart() {
        super.onStart()
        BackgroundGate.setForeground(true)
        app.rustNetManager.onForegroundChanged(true)
    }

    override fun onResume() {
        super.onResume()
        StealthExemptionGate.clear()
        app.updateController.onHostResumed()
    }

    override fun onStop() {
        if (!isChangingConfigurations) {
            if (!StealthExemptionGate.consumeShouldSuppress()) {
                backgroundLeaveHandler?.invoke()
            }
            lifecycleScope.launch(Dispatchers.IO) {
                app.seenTracker.flushNow()
            }
        }
        BackgroundGate.setForeground(false)
        app.rustNetManager.onForegroundChanged(false)
        super.onStop()
    }

    private fun loadInitialUiSettings(): InitialUiSettings {
        val savedTheme = app.db.getKvBlocking("ui.theme_mode")?.let { raw ->
            runCatching { AppThemeMode.valueOf(raw) }.getOrNull()
        }
        val rawPrivacy = app.db.getKvBlocking(KV_PRIVACY_STEALTH_V2)
        var theme = AppThemeMode.FOLLOW_SYSTEM
        var privacy = PrivacyStealthConfig()
        if (!rawPrivacy.isNullOrBlank()) {
            runCatching {
                ForHappyJson.decodeFromString(PrivacyStealthConfig.serializer(), rawPrivacy)
            }.getOrNull()?.let { privacy = it }
            theme = when (savedTheme) {
                AppThemeMode.WARM_PAPER_LIGHT,
                AppThemeMode.PINE_NIGHT_DARK,
                AppThemeMode.FOLLOW_SYSTEM,
                -> savedTheme
                else -> AppThemeMode.FOLLOW_SYSTEM
            }
        } else {
            when (savedTheme) {
                AppThemeMode.FEISHU_WORK -> {
                    theme = AppThemeMode.FOLLOW_SYSTEM
                    privacy = PrivacyStealthConfig(
                        disguiseMode = AppDisguiseMode.FEISHU,
                        blurAllEnabled = true,
                    )
                }
                AppThemeMode.LINUX_DO -> {
                    theme = AppThemeMode.FOLLOW_SYSTEM
                    privacy = PrivacyStealthConfig(
                        disguiseMode = AppDisguiseMode.LINUX_DO,
                        blurAllEnabled = true,
                    )
                }
                AppThemeMode.WARM_PAPER_LIGHT,
                AppThemeMode.PINE_NIGHT_DARK,
                AppThemeMode.FOLLOW_SYSTEM,
                -> theme = savedTheme
                null -> Unit
            }
        }
        val shield = app.db.getKvBlocking("ui.privacy_shield")?.toBooleanStrictOrNull() ?: false
        val alias = app.db.getKvBlocking("ui.icon_alias")?.let { saved ->
            runCatching { AppIconAlias.valueOf(saved) }.getOrNull()
        } ?: AppIconAlias.JINHUAN
        return InitialUiSettings(
            themeMode = theme,
            privacyConfig = privacy,
            privacyShield = shield,
            iconAlias = alias,
        )
    }

    private fun syncRecentsTaskDescription(alias: AppIconAlias) {
        val iconRes = when (alias) {
            AppIconAlias.JINHUAN -> R.drawable.ic_launcher_jinhuan
            AppIconAlias.FEISHU -> R.drawable.ic_launcher_feishu
            AppIconAlias.DOCS -> R.drawable.ic_launcher_docs
        }
        val desc = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
                ActivityManager.TaskDescription.Builder()
                    .setLabel(alias.title)
                    .setIcon(iconRes)
                    .build()
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P ->
                @Suppress("DEPRECATION")
                ActivityManager.TaskDescription(alias.title, iconRes)
            else ->
                @Suppress("DEPRECATION")
                ActivityManager.TaskDescription(alias.title)
        }
        setTaskDescription(desc)
    }

    companion object {
        private const val KV_PRIVACY_STEALTH_V2 = "ui.privacy_stealth_v2"
    }
}
