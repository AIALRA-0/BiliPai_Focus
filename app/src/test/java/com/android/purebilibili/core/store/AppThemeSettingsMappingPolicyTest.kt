package com.android.purebilibili.core.store

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.android.purebilibili.core.theme.AndroidNativeVariant
import com.android.purebilibili.core.theme.AppFontSizePreset
import com.android.purebilibili.core.theme.AppUiScalePreset
import com.android.purebilibili.core.theme.UiPreset
import com.android.purebilibili.feature.screenshot.AppScreenshotCaptureMode
import com.android.purebilibili.feature.screenshot.AppScreenshotGestureMode
import com.android.purebilibili.feature.settings.AppLanguage
import com.android.purebilibili.feature.settings.AppThemeMode
import com.android.purebilibili.feature.settings.DarkThemeStyle
import com.android.purebilibili.feature.settings.Md3ColorSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class AppThemeSettingsMappingPolicyTest {

    @Test
    fun emptyPreferences_useStartupThemeDefaults() {
        val result = mapAppThemeSettingsFromPreferences(mutablePreferencesOf())

        assertEquals(UiPreset.MD3, result.uiPreset)
        assertEquals(AndroidNativeVariant.MATERIAL3, result.androidNativeVariant)
        assertEquals(AppThemeMode.FOLLOW_SYSTEM, result.themeMode)
        assertEquals(DarkThemeStyle.DEFAULT, result.darkThemeStyle)
        assertEquals(AppLanguage.FOLLOW_SYSTEM, result.appLanguage)
        assertEquals(Md3ColorSource.FOLLOW_WALLPAPER, result.md3ColorSource)
        assertEquals("#007AFF", result.md3CustomColorHex)
        assertEquals(0, result.themeColorIndex)
        assertEquals(AppFontSizePreset.DEFAULT, result.appFontSizePreset)
        assertEquals("", result.appFontFileName)
        assertEquals(AppUiScalePreset.STANDARD, result.appUiScalePreset)
        assertEquals(0, result.appDpiOverridePercent)
        assertFalse(result.appGestureScreenshotEnabled)
        assertEquals(
            AppScreenshotGestureMode.TOP_RIGHT_TWO_FINGER_LONG_PRESS,
            result.appScreenshotGestureMode
        )
        assertEquals(AppScreenshotCaptureMode.FULL_WINDOW, result.appScreenshotCaptureMode)
    }

    @Test
    fun preferences_mapAllStartupThemeFieldsFromOneSnapshot() {
        val result = mapAppThemeSettingsFromPreferences(
            mutablePreferencesOf(
                intPreferencesKey("ui_preset") to UiPreset.IOS.value,
                intPreferencesKey("android_native_variant_v1") to AndroidNativeVariant.MIUIX.value,
                intPreferencesKey("theme_mode_v2") to AppThemeMode.DARK.value,
                intPreferencesKey("dark_theme_style_v1") to DarkThemeStyle.AMOLED.value,
                intPreferencesKey("app_language_v1") to AppLanguage.ENGLISH.value,
                stringPreferencesKey("md3_color_source") to Md3ColorSource.CUSTOM.name,
                stringPreferencesKey("md3_custom_color_hex") to "#ff00ff",
                intPreferencesKey("theme_color_index") to 4,
                intPreferencesKey("app_font_size_preset") to AppFontSizePreset.LARGE.value,
                stringPreferencesKey("app_font_file_name") to "demo.ttf",
                intPreferencesKey("app_ui_scale_preset") to AppUiScalePreset.LARGE.value,
                intPreferencesKey("app_dpi_override_percent") to 200,
                booleanPreferencesKey("app_gesture_screenshot_enabled") to true,
                intPreferencesKey("app_screenshot_gesture_mode") to
                    AppScreenshotGestureMode.THREE_FINGER_SWIPE_DOWN.value,
                intPreferencesKey("app_screenshot_capture_mode") to
                    AppScreenshotCaptureMode.SELECT_REGION.value
            )
        )

        assertEquals(UiPreset.IOS, result.uiPreset)
        assertEquals(AndroidNativeVariant.MIUIX, result.androidNativeVariant)
        assertEquals(AppThemeMode.DARK, result.themeMode)
        assertEquals(DarkThemeStyle.AMOLED, result.darkThemeStyle)
        assertEquals(AppLanguage.ENGLISH, result.appLanguage)
        assertEquals(Md3ColorSource.CUSTOM, result.md3ColorSource)
        assertEquals("#FF00FF", result.md3CustomColorHex)
        assertEquals(4, result.themeColorIndex)
        assertEquals(AppFontSizePreset.LARGE, result.appFontSizePreset)
        assertEquals("demo.ttf", result.appFontFileName)
        assertEquals(AppUiScalePreset.LARGE, result.appUiScalePreset)
        assertEquals(115, result.appDpiOverridePercent)
        assertEquals(true, result.appGestureScreenshotEnabled)
        assertEquals(AppScreenshotGestureMode.THREE_FINGER_SWIPE_DOWN, result.appScreenshotGestureMode)
        assertEquals(AppScreenshotCaptureMode.SELECT_REGION, result.appScreenshotCaptureMode)
    }
}
