package com.jinhuan.forhappy

import android.os.Bundle
import android.os.SystemClock
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
import com.jinhuan.forhappy.core.network.scheduler.BackgroundGate
import com.jinhuan.forhappy.ui.disguise.DeviceStealthSensorObserver
import com.jinhuan.forhappy.ui.navigation.ForHappyShell
import com.jinhuan.forhappy.ui.theme.ForHappyTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var app: ForHappyApp
    private var lastVolDownMs: Long = 0L
    private var lastVolUpMs: Long = 0L
    private var volumeActionHandler: ((ShortcutGestureMode) -> Boolean)? = null
    private var backgroundLeaveHandler: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        app = application as ForHappyApp

        setContent {
            val scope = rememberCoroutineScope()
            var themeMode by remember { mutableStateOf(AppThemeMode.FOLLOW_SYSTEM) }
            var privacyConfig by remember { mutableStateOf(PrivacyStealthConfig()) }
            var privacyShield by remember { mutableStateOf(false) }
            var iconAlias by remember { mutableStateOf(AppIconAlias.JINHUAN) }
            var bossKeyActive by remember { mutableStateOf(false) }

            fun persistPrivacyConfig(next: PrivacyStealthConfig) {
                privacyConfig = next
                scope.launch {
                    app.db.putKv(
                        KV_PRIVACY_STEALTH_V2,
                        ForHappyJson.encodeToString(PrivacyStealthConfig.serializer(), next),
                    )
                }
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

            LaunchedEffect(Unit) {
                val savedTheme = app.db.getKv("ui.theme_mode")?.let { raw ->
                    runCatching { AppThemeMode.valueOf(raw) }.getOrNull()
                }
                val rawPrivacy = app.db.getKv(KV_PRIVACY_STEALTH_V2)
                if (!rawPrivacy.isNullOrBlank()) {
                    runCatching {
                        ForHappyJson.decodeFromString(PrivacyStealthConfig.serializer(), rawPrivacy)
                    }.getOrNull()?.let { privacyConfig = it }
                    themeMode = when (savedTheme) {
                        AppThemeMode.WARM_PAPER_LIGHT,
                        AppThemeMode.PINE_NIGHT_DARK,
                        AppThemeMode.FOLLOW_SYSTEM,
                        -> savedTheme
                        else -> AppThemeMode.FOLLOW_SYSTEM
                    }
                } else {
                    when (savedTheme) {
                        AppThemeMode.FEISHU_WORK -> {
                            themeMode = AppThemeMode.FOLLOW_SYSTEM
                            privacyConfig = PrivacyStealthConfig(
                                disguiseMode = AppDisguiseMode.FEISHU,
                                blurAllEnabled = true,
                            )
                        }
                        AppThemeMode.LINUX_DO -> {
                            themeMode = AppThemeMode.FOLLOW_SYSTEM
                            privacyConfig = PrivacyStealthConfig(
                                disguiseMode = AppDisguiseMode.LINUX_DO,
                                blurAllEnabled = true,
                            )
                        }
                        AppThemeMode.WARM_PAPER_LIGHT,
                        AppThemeMode.PINE_NIGHT_DARK,
                        AppThemeMode.FOLLOW_SYSTEM,
                        -> themeMode = savedTheme
                        null -> Unit
                    }
                }
                privacyShield = app.db.getKv("ui.privacy_shield")?.toBooleanStrictOrNull() ?: false
                app.db.getKv("ui.icon_alias")?.let { saved ->
                    runCatching { AppIconAlias.valueOf(saved) }.getOrNull()?.let { iconAlias = it }
                }
            }

            volumeActionHandler = { gesture ->
                var handled = false
                if (privacyConfig.bossVolumeShortcut == gesture) {
                    bossKeyActive = !bossKeyActive
                    handled = true
                }
                if (privacyConfig.blurShortcut == gesture && !bossKeyActive) {
                    persistPrivacyConfig(privacyConfig.copy(blurAllEnabled = !privacyConfig.blurAllEnabled))
                    handled = true
                }
                handled
            }

            backgroundLeaveHandler = {
                if (privacyConfig.autoBlurOnBackground && !privacyConfig.blurAllEnabled) {
                    persistPrivacyConfig(privacyConfig.copy(blurAllEnabled = true))
                }
                if (privacyConfig.bossOnBackgroundResume) {
                    bossKeyActive = true
                }
            }

            DeviceStealthSensorObserver(
                flipFaceDownEnabled = privacyConfig.bossFlipFaceDown && !bossKeyActive,
                shakeBlurEnabled = privacyConfig.blurShortcut == ShortcutGestureMode.SHAKE && !bossKeyActive,
                onFlipFaceDown = { bossKeyActive = true },
                onShake = {
                    persistPrivacyConfig(privacyConfig.copy(blurAllEnabled = !privacyConfig.blurAllEnabled))
                },
            )

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
            ) {
                ForHappyShell(
                    app = app,
                    themeMode = themeMode,
                    privacyConfig = privacyConfig,
                    bossKeyActive = bossKeyActive,
                    privacyShieldEnabled = privacyShield,
                    activeIconAlias = iconAlias,
                    onChangeTheme = { next ->
                        themeMode = next
                        scope.launch { app.db.putKv("ui.theme_mode", next.name) }
                    },
                    onChangeDisguise = ::switchDisguiseMode,
                    onUpdatePrivacyConfig = ::persistPrivacyConfig,
                    onSetBossKeyActive = { bossKeyActive = it },
                    onTogglePrivacyShield = { enabled ->
                        privacyShield = enabled
                        scope.launch { app.db.putKv("ui.privacy_shield", enabled.toString()) }
                    },
                    onChangeIconAlias = { next ->
                        iconAlias = next
                        scope.launch { app.db.putKv("ui.icon_alias", next.name) }
                    },
                )
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (event != null && event.repeatCount == 0) {
            val now = SystemClock.uptimeMillis()
            when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    val isDouble = now - lastVolDownMs in 1L..420L
                    lastVolDownMs = if (isDouble) 0L else now
                    if (isDouble && volumeActionHandler?.invoke(ShortcutGestureMode.VOL_DOUBLE_DOWN) == true) {
                        return true
                    }
                }
                KeyEvent.KEYCODE_VOLUME_UP -> {
                    val isDouble = now - lastVolUpMs in 1L..420L
                    lastVolUpMs = if (isDouble) 0L else now
                    if (isDouble && volumeActionHandler?.invoke(ShortcutGestureMode.VOL_DOUBLE_UP) == true) {
                        return true
                    }
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
        app.updateController.onHostResumed()
    }

    override fun onStop() {
        if (!isChangingConfigurations) {
            backgroundLeaveHandler?.invoke()
            lifecycleScope.launch(Dispatchers.IO) {
                app.seenTracker.flushNow()
            }
        }
        BackgroundGate.setForeground(false)
        app.rustNetManager.onForegroundChanged(false)
        super.onStop()
    }

    companion object {
        private const val KV_PRIVACY_STEALTH_V2 = "ui.privacy_stealth_v2"
    }
}
