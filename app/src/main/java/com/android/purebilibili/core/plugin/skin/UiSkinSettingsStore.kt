package com.android.purebilibili.core.plugin.skin

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

private const val UI_SKIN_PREFS = "ui_skin_settings"
private const val KEY_ENABLED = "enabled"
private const val KEY_SELECTED_SKIN_ID = "selected_skin_id"
private const val KEY_SELECTED_INSTALL_ID = "selected_install_id"

object UiSkinSettingsStore {

    fun observe(context: Context): Flow<UiSkinState> {
        val appContext = context.applicationContext
        return callbackFlow {
            val prefs = appContext.getSharedPreferences(UI_SKIN_PREFS, Context.MODE_PRIVATE)
            fun sendLatest() {
                trySend(readState(appContext))
            }
            val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key == KEY_ENABLED || key == KEY_SELECTED_SKIN_ID || key == KEY_SELECTED_INSTALL_ID) {
                    sendLatest()
                }
            }
            sendLatest()
            prefs.registerOnSharedPreferenceChangeListener(listener)
            awaitClose {
                prefs.unregisterOnSharedPreferenceChangeListener(listener)
            }
        }.distinctUntilChanged()
    }

    fun readState(context: Context): UiSkinState {
        val prefs = context.applicationContext.getSharedPreferences(UI_SKIN_PREFS, Context.MODE_PRIVATE)
        val selection = UiSkinSelection(
            enabled = prefs.getBoolean(KEY_ENABLED, false),
            selectedSkinId = prefs.getString(KEY_SELECTED_SKIN_ID, null),
            selectedInstallId = prefs.getString(KEY_SELECTED_INSTALL_ID, null)
        )
        return resolveUiSkinState(
            selection = selection,
            installedSkins = UiSkinInstallStore.createDefault(context).listInstalledPackages()
        )
    }

    fun setSelection(
        context: Context,
        selection: UiSkinSelection
    ) {
        context.applicationContext
            .getSharedPreferences(UI_SKIN_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENABLED, selection.enabled)
            .putString(KEY_SELECTED_SKIN_ID, selection.selectedSkinId)
            .putString(KEY_SELECTED_INSTALL_ID, selection.selectedInstallId)
            .apply()
    }
}

@Composable
fun rememberUiSkinState(context: Context): State<UiSkinState> {
    return UiSkinSettingsStore.observe(context).collectAsStateWithLifecycle(initialValue = UiSkinState())
}
