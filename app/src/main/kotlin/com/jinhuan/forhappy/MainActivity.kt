package com.jinhuan.forhappy

import android.os.Bundle
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
import com.jinhuan.forhappy.core.model.AppIconAlias
import com.jinhuan.forhappy.core.model.AppThemeMode
import com.jinhuan.forhappy.core.network.scheduler.BackgroundGate
import com.jinhuan.forhappy.ui.navigation.ForHappyShell
import com.jinhuan.forhappy.ui.theme.ForHappyTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var app: ForHappyApp

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        app = application as ForHappyApp

        setContent {
            val scope = rememberCoroutineScope()
            var themeMode by remember { mutableStateOf(AppThemeMode.FOLLOW_SYSTEM) }
            var privacyShield by remember { mutableStateOf(false) }
            var iconAlias by remember { mutableStateOf(AppIconAlias.JINHUAN) }

            LaunchedEffect(Unit) {
                app.db.getKv("ui.theme_mode")?.let { saved ->
                    runCatching { AppThemeMode.valueOf(saved) }.getOrNull()?.let { themeMode = it }
                }
                privacyShield = app.db.getKv("ui.privacy_shield")?.toBooleanStrictOrNull() ?: false
                app.db.getKv("ui.icon_alias")?.let { saved ->
                    runCatching { AppIconAlias.valueOf(saved) }.getOrNull()?.let { iconAlias = it }
                }
            }

            LaunchedEffect(privacyShield) {
                if (privacyShield) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }

            ForHappyTheme(mode = themeMode) {
                ForHappyShell(
                    app = app,
                    themeMode = themeMode,
                    privacyShieldEnabled = privacyShield,
                    activeIconAlias = iconAlias,
                    onChangeTheme = { next ->
                        themeMode = next
                        scope.launch { app.db.putKv("ui.theme_mode", next.name) }
                    },
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
        BackgroundGate.setForeground(false)
        app.rustNetManager.onForegroundChanged(false)
        super.onStop()
    }
}
