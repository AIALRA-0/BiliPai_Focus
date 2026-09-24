package com.android.purebilibili.core.plugin.skin

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

private const val UI_SKIN_PREFS = "ui_skin_settings"
private const val KEY_ENABLED = "enabled"
private const val KEY_SELECTED_SKIN_ID = "selected_skin_id"
private const val KEY_SELECTED_INSTALL_ID = "selected_install_id"

object UiSkinSettingsStore {

    fun observe(context: Context): Flow<UiSkinState> {
        val appContext = context.applicationContext
        return callbackFlow {
            val prefs = appContext.getSharedPreferences(UI_SKIN_PREFS, Context.MODE_PRIVATE)
            val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key == KEY_ENABLED || key == KEY_SELECTED_SKIN_ID || key == KEY_SELECTED_INSTALL_ID) {
                    // Preference callbacks can arrive on the main thread. Only invalidate here;
                    // the snapshot and installed-package scan run in the IO flow context below.
                    trySend(Unit)
                }
            }
            prefs.registerOnSharedPreferenceChangeListener(listener)
            trySend(Unit)
            awaitClose {
                prefs.unregisterOnSharedPreferenceChangeListener(listener)
            }
        }
            .buffer(Channel.CONFLATED)
            .map { readState(appContext) }
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)
    }

    private fun readState(context: Context): UiSkinState {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(UI_SKIN_PREFS, Context.MODE_PRIVATE)
        val selection = UiSkinSelection(
            enabled = prefs.getBoolean(KEY_ENABLED, false),
            selectedSkinId = prefs.getString(KEY_SELECTED_SKIN_ID, null),
            selectedInstallId = prefs.getString(KEY_SELECTED_INSTALL_ID, null)
        )
        if (!selection.enabled) return UiSkinState()
        return resolveUiSkinState(
            selection = selection,
            installedSkins = UiSkinInstallStore.createDefault(appContext).listInstalledPackages()
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
    val appContext = context.applicationContext
    val stateFlow = remember(appContext) { UiSkinSettingsStore.observe(appContext) }
    return stateFlow.collectAsStateWithLifecycle(initialValue = UiSkinState())
}
