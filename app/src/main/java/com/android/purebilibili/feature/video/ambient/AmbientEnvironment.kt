package com.android.purebilibili.feature.video.ambient

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

internal data class AmbientEnvironment(val saving: Boolean = false, val severe: Boolean = false)

@Composable
internal fun rememberAmbientEnvironment(enabled: Boolean): AmbientEnvironment {
    val context = LocalContext.current
    var environment by remember { mutableStateOf(AmbientEnvironment()) }
    DisposableEffect(context, enabled) {
        if (!enabled) { environment = AmbientEnvironment(); onDispose { } }
        else {
            val power = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            var lowBattery = false
            fun update() {
                val thermal = if (Build.VERSION.SDK_INT >= 29) power.currentThermalStatus else 0
                environment = AmbientEnvironment(power.isPowerSaveMode || lowBattery || thermal >= 2, thermal >= 3)
            }
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                        lowBattery = level >= 0 && scale > 0 && level * 100 / scale <= 15
                    }
                    update()
                }
            }
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED).apply { addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED) }
            val sticky = ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
            receiver.onReceive(context, sticky)
            val thermalListener = if (Build.VERSION.SDK_INT >= 29) PowerManager.OnThermalStatusChangedListener { update() } else null
            if (Build.VERSION.SDK_INT >= 29 && thermalListener != null) power.addThermalStatusListener(context.mainExecutor, thermalListener)
            onDispose {
                context.unregisterReceiver(receiver)
                if (Build.VERSION.SDK_INT >= 29 && thermalListener != null) power.removeThermalStatusListener(thermalListener)
            }
        }
    }
    return environment
}
