package com.android.purebilibili.core.theme

import android.app.WallpaperManager
import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.android.purebilibili.core.store.settingsDataStore
import com.materialkolor.hct.Hct
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.min

internal data class WallpaperColorCompatibility(
    val wallpaperId: Int = -1,
    val seed: Color? = null,
    val wallpaperColorsAvailable: Boolean = false,
    val manual: Boolean = false,
)

internal val LocalWallpaperColorCompatibility = compositionLocalOf { WallpaperColorCompatibility() }

private val manualSeedKey = intPreferencesKey("wallpaper_compatibility_seed")
private val manualWallpaperIdKey = intPreferencesKey("wallpaper_compatibility_id")

internal fun isManualWallpaperSeedCurrent(currentId: Int, savedId: Int?): Boolean =
    currentId >= 0 && savedId == currentId

/** Notifications reuse the last completed read; never do wallpaper IPC on their main thread. */
@Volatile internal var latestWallpaperColorCompatibility = WallpaperColorCompatibility()
    private set

/** A large hue mismatch is a compatibility heuristic, not proof that an overlay is stale.
 * Respect explicit preset palettes. Only wallpaper-selected palettes may use this fallback. */
internal fun shouldUseWallpaperColorFallback(
    wallpaperArgb: Int,
    systemArgb: Int,
    colorSource: String?,
): Boolean {
    if (colorSource != "home_wallpaper") return false
    val wallpaper = Hct.fromInt(wallpaperArgb)
    val system = Hct.fromInt(systemArgb)
    val distance = abs(wallpaper.hue - system.hue)
    return wallpaper.chroma >= 12 && system.chroma >= 12 &&
        min(distance, 360 - distance) > 60
}

@androidx.annotation.RequiresApi(android.os.Build.VERSION_CODES.S)
private fun readWallpaperColors(context: Context): WallpaperColorCompatibility {
    val manager = runCatching { WallpaperManager.getInstance(context) }.getOrNull()
        ?: return WallpaperColorCompatibility()
    val id = runCatching { manager.getWallpaperId(WallpaperManager.FLAG_SYSTEM) }.getOrDefault(-1)
    val colors = runCatching { manager.getWallpaperColors(WallpaperManager.FLAG_SYSTEM) }.getOrNull()
        ?: return WallpaperColorCompatibility(wallpaperId = id)
    val primary = colors.primaryColor.toArgb()
    val candidates = listOfNotNull(colors.primaryColor, colors.secondaryColor, colors.tertiaryColor)
    val seed = if (Hct.fromInt(primary).chroma >= 12) primary else
        candidates.maxByOrNull { Hct.fromInt(it.toArgb()).chroma }?.toArgb() ?: primary
    val colorSource = runCatching {
        JSONObject(Settings.Secure.getString(context.contentResolver, "theme_customization_overlay_packages") ?: "{}")
            .optString("android.theme.customization.color_source")
    }.getOrNull()
    val useFallback = shouldUseWallpaperColorFallback(
        seed, context.getColor(android.R.color.system_accent1_500), colorSource,
    )
    return WallpaperColorCompatibility(id, if (useFallback) Color(seed) else null, true)
}

/** UI owns cancellation. Wallpaper IPC runs off main; source changes discard old results.
 * This shared color input applies to glass on/off alike, without changing native layout. */
@Composable
internal fun rememberWallpaperColorCompatibility(enabled: Boolean, refreshToken: Int): State<WallpaperColorCompatibility> {
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    return produceState(WallpaperColorCompatibility(), context, lifecycleOwner, enabled, refreshToken) {
        if (!enabled || android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) {
            latestWallpaperColorCompatibility = WallpaperColorCompatibility()
            value = WallpaperColorCompatibility()
            return@produceState
        }
        // Recheck IDs on EVERY resume, including returns inside the palette refresh throttle.
        // Unrelated settings writes must not trigger wallpaper service IPC.
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            context.settingsDataStore.data
                .map { it[manualSeedKey] to it[manualWallpaperIdKey] }
                .distinctUntilChanged()
                .collectLatest { (manualSeed, savedId) ->
                    val resolved = withContext(Dispatchers.IO) {
                        val automatic = readWallpaperColors(context)
                        if (manualSeed != null && isManualWallpaperSeedCurrent(automatic.wallpaperId, savedId)) {
                            automatic.copy(seed = Color(manualSeed), manual = true)
                        } else automatic
                    }
                    latestWallpaperColorCompatibility = resolved
                    value = resolved
                }
        }
    }
}

internal suspend fun saveManualWallpaperSeed(context: Context, argb: Int?, wallpaperId: Int = -1) {
    context.settingsDataStore.edit {
        if (argb == null) {
            it.remove(manualSeedKey)
            it.remove(manualWallpaperIdKey)
        } else {
            it[manualSeedKey] = argb
            it[manualWallpaperIdKey] = wallpaperId
        }
    }
}
