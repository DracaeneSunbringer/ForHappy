package com.jinhuan.forhappy.ui.disguise

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.jinhuan.forhappy.core.model.ShortcutGestureMode
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Pure-Kotlin detector for rapid double-tap of volume-up / volume-down hardware keys.
 * Returns the matching [ShortcutGestureMode] ONLY on the second tap within [minDeltaMs]..[maxDeltaMs].
 * The first tap always returns null so OS volume adjusts immediately with zero artificial delay.
 */
class VolumeDoubleTapDetector(
    private val minDeltaMs: Long = 1L,
    private val maxDeltaMs: Long = 420L,
) {
    private var lastDownAtMs: Long = 0L
    private var lastUpAtMs: Long = 0L

    fun onVolumeKey(
        mode: ShortcutGestureMode,
        nowMs: Long,
    ): ShortcutGestureMode? = when (mode) {
        ShortcutGestureMode.VOL_DOUBLE_DOWN -> {
            lastUpAtMs = 0L
            val delta = nowMs - lastDownAtMs
            if (lastDownAtMs > 0L && delta in minDeltaMs..maxDeltaMs) {
                lastDownAtMs = 0L
                ShortcutGestureMode.VOL_DOUBLE_DOWN
            } else {
                lastDownAtMs = nowMs
                null
            }
        }
        ShortcutGestureMode.VOL_DOUBLE_UP -> {
            lastDownAtMs = 0L
            val delta = nowMs - lastUpAtMs
            if (lastUpAtMs > 0L && delta in minDeltaMs..maxDeltaMs) {
                lastUpAtMs = 0L
                ShortcutGestureMode.VOL_DOUBLE_UP
            } else {
                lastUpAtMs = nowMs
                null
            }
        }
        else -> null
    }

    fun reset() {
        lastDownAtMs = 0L
        lastUpAtMs = 0L
    }
}

/**
 * Intercepts rapid double system-back gestures (450ms window) on root views with no open dialogs,
 * mirroring the web client's double-Esc Boss Key trigger.
 */
@Composable
fun DoubleBackBossHandler(
    enabled: Boolean,
    onTriggerBoss: () -> Unit,
) {
    val currentOnTrigger by rememberUpdatedState(onTriggerBoss)
    var lastBackAt by remember { mutableLongStateOf(0L) }
    BackHandler(enabled = enabled) {
        val now = SystemClock.uptimeMillis()
        if (now - lastBackAt in 1L..450L) {
            lastBackAt = 0L
            currentOnTrigger()
        } else {
            lastBackAt = now
        }
    }
}

/**
 * Observes accelerometer events ONLY when flip-face-down Boss Key or wrist-shake Blur All trigger
 * is enabled by user settings. Unregisters immediately when neither option is active.
 */
@Composable
fun DeviceStealthSensorObserver(
    flipFaceDownEnabled: Boolean,
    shakeBlurEnabled: Boolean,
    suspendShakeInGame: Boolean = false,
    onFlipFaceDown: () -> Unit,
    onShake: () -> Unit,
) {
    val activeShake = shakeBlurEnabled && !suspendShakeInGame
    if (!flipFaceDownEnabled && !activeShake) return
    val context = LocalContext.current
    val curFlip by rememberUpdatedState(onFlipFaceDown)
    val curShake by rememberUpdatedState(onShake)

    DisposableEffect(context, flipFaceDownEnabled, activeShake) {
        val mgr = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val accel = mgr?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (mgr == null || accel == null) {
            return@DisposableEffect onDispose {}
        }

        val listener = object : SensorEventListener {
            private var sawUpright = false
            private var faceDownSinceMs = 0L
            private var faceDownTriggered = false

            private var firstShakePeakMs = 0L
            private var lastPeakMs = 0L
            private var lastShakeFireMs = 0L

            override fun onSensorChanged(event: SensorEvent) {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val now = SystemClock.uptimeMillis()

                if (flipFaceDownEnabled) {
                    if (z > 1.5f) {
                        sawUpright = true
                        faceDownSinceMs = 0L
                        faceDownTriggered = false
                    } else if (sawUpright && !faceDownTriggered && z < -8.0f && abs(x) < 4.0f && abs(y) < 4.0f) {
                        if (faceDownSinceMs == 0L) {
                            faceDownSinceMs = now
                        } else if (now - faceDownSinceMs >= 300L) {
                            faceDownTriggered = true
                            sawUpright = false
                            curFlip()
                        }
                    } else if (z > -6.5f) {
                        faceDownSinceMs = 0L
                    }
                }

                if (activeShake) {
                    val mag = sqrt((x * x + y * y + z * z).toDouble())
                    if (mag > 16.8 && now - lastPeakMs >= 130L) {
                        lastPeakMs = now
                        if (firstShakePeakMs != 0L && now - firstShakePeakMs <= 650L && now - lastShakeFireMs >= 1200L) {
                            firstShakePeakMs = 0L
                            lastShakeFireMs = now
                            curShake()
                        } else {
                            firstShakePeakMs = now
                        }
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        mgr.registerListener(listener, accel, SensorManager.SENSOR_DELAY_UI)
        onDispose {
            mgr.unregisterListener(listener)
        }
    }
}
