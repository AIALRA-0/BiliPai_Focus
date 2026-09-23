// 文件路径: core/store/SettingsModels.kt
package com.android.purebilibili.core.store

import android.content.Context
import com.android.purebilibili.BuildConfig
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.android.purebilibili.core.ui.AppIconStyle
import com.android.purebilibili.core.ui.AppListItemStyle
import com.android.purebilibili.core.ui.components.AppSingleChoicePresentation
import com.android.purebilibili.core.ui.resolveAppIconStylePreference
import com.android.purebilibili.core.ui.resolveAppListItemStylePreference
import com.android.purebilibili.core.ui.blur.BlurIntensity
import com.android.purebilibili.core.ui.transition.VIDEO_SHARED_TRANSITION_CUSTOM_DEFAULT_MILLIS
import com.android.purebilibili.core.ui.transition.VideoSharedTransitionSpeed
import com.android.purebilibili.core.ui.transition.normalizeVideoSharedTransitionCustomDurationMillis
import com.android.purebilibili.core.store.home.HomeSettingsStore
import com.android.purebilibili.core.store.home.liquidGlassReadabilityModePreferencesKey
import com.android.purebilibili.core.store.navigation.NavigationSettingsStore
import com.android.purebilibili.core.store.navigation.bottomBarItemLabelsPreferencesKey
import com.android.purebilibili.core.store.navigation.miuixPredictiveBackMaxProgressPercentPreferencesKey
import com.android.purebilibili.core.store.navigation.parseBottomBarItemLabels
import com.android.purebilibili.core.store.player.PlayerSettingsStore
import com.android.purebilibili.core.store.player.defaultAudioQualityPreferenceKey
import com.android.purebilibili.core.theme.AppFontSizePreset
import com.android.purebilibili.core.theme.AppUiScalePreset
import com.android.purebilibili.core.theme.AndroidNativeVariant
import com.android.purebilibili.core.theme.AppUiStyle
import com.android.purebilibili.core.theme.UiPreset
import com.android.purebilibili.core.theme.normalizeThemeColorIndex
import com.android.purebilibili.core.theme.resolveColorSpecPreference
import com.android.purebilibili.core.theme.resolvePaletteStylePreference
import com.android.purebilibili.data.model.response.LiveFavoriteTagEntry
import com.android.purebilibili.feature.settings.share.SettingsShareApplyResult
import com.android.purebilibili.feature.settings.share.SettingsShareEntryDefinition
import com.android.purebilibili.feature.settings.share.SettingsShareSection
import com.android.purebilibili.feature.settings.AppLanguage
import com.android.purebilibili.feature.settings.AppThemeMode
import com.android.purebilibili.feature.settings.DarkThemeStyle
import com.android.purebilibili.feature.settings.Md3ColorSource
import com.android.purebilibili.feature.settings.normalizeMd3CustomColorHex
import com.android.purebilibili.feature.settings.resolveAppLanguagePreference
import com.android.purebilibili.feature.settings.resolveDarkThemeStylePreference
import com.android.purebilibili.feature.settings.resolveMd3ColorSourcePreference
import com.android.purebilibili.feature.settings.resolveThemeModePreference
import com.android.purebilibili.feature.screenshot.AppScreenshotCaptureMode
import com.android.purebilibili.feature.screenshot.AppScreenshotGestureMode
import com.android.purebilibili.feature.video.ui.components.CollectionSortMode
import com.android.purebilibili.feature.video.danmaku.DANMAKU_DEFAULT_OPACITY
import com.android.purebilibili.feature.video.danmaku.normalizeDanmakuOpacity
import com.android.purebilibili.feature.video.danmaku.parseDanmakuBlockRules
import com.android.purebilibili.feature.video.subtitle.SubtitleAutoPreference
import com.android.purebilibili.feature.video.subtitle.normalizeSubtitleVerticalOffsetFraction
import com.android.purebilibili.feature.video.ui.gesture.TwoFingerSpeedToggleState
import com.android.purebilibili.feature.video.ui.gesture.applyHorizontalTwoFingerSpeedToggle
import com.android.purebilibili.feature.video.ui.gesture.applyVerticalTwoFingerSpeedToggle
import com.android.purebilibili.core.util.ENHANCED_DIAGNOSTIC_LOG_PREF_KEY
import com.android.purebilibili.core.util.ENHANCED_DIAGNOSTIC_LOG_PREFS_NAME
import com.android.purebilibili.core.util.isLargeScreenOrFoldableConfiguration
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import kotlin.math.abs

/**
 *  首页设置合并类 - 减少 HomeScreen 重组次数
 * 将多个独立的设置流合并为单一流，避免每个设置变化都触发重组
 */
enum class LiquidGlassStyle(val value: Int) {
    CLASSIC(0),      // BiliPai's Wavy Ripple
    SUKISU(1),       // SukiSU floating bottom bar glass
    IOS26(2);        // iOS26-like layered liquid glass

    companion object {
        fun fromValue(value: Int): LiquidGlassStyle = entries.find { it.value == value } ?: CLASSIC
    }
}

enum class LiquidGlassMode(val value: Int, val label: String) {
    CLEAR(0, "通透玻璃"),
    BALANCED(1, "平衡"),
    FROSTED(2, "柔和磨砂");

    companion object {
        fun fromValue(value: Int): LiquidGlassMode = entries.find { it.value == value } ?: BALANCED
    }
}

enum class LiquidGlassAdvancedPreset(val value: Int, val label: String) {
    READABLE(0, "清晰"),
    BALANCED(1, "均衡"),
    PRISM(2, "棱镜"),
    CUSTOM(3, "自定");

    companion object {
        fun fromValue(value: Int): LiquidGlassAdvancedPreset =
            entries.find { it.value == value } ?: BALANCED
    }
}

data class LiquidGlassAdvancedSettings(
    val preset: LiquidGlassAdvancedPreset = LiquidGlassAdvancedPreset.BALANCED,
    val progressiveBlurRadius: Float = 0.40f,
    val progressiveBlurExtent: Float = 1.0f,
    val progressiveBlurCurve: Float = 0.55f,
    val contentReadability: Float = 0.62f,
    val chromaticAberration: Float = 0.56f,
    val contentDistortion: Float = 0.45f,
)

internal fun normalizeLiquidGlassAdvancedValue(value: Float, fallback: Float): Float =
    if (value.isFinite()) value.coerceIn(0f, 1f) else fallback

internal fun resolveLiquidGlassAdvancedPreset(
    preset: LiquidGlassAdvancedPreset,
): LiquidGlassAdvancedSettings = when (preset) {
    LiquidGlassAdvancedPreset.READABLE -> LiquidGlassAdvancedSettings(
        preset = preset,
        progressiveBlurRadius = 0.25f,
        progressiveBlurExtent = 0.6f,
        progressiveBlurCurve = 0.4f,
        contentReadability = 1f,
        chromaticAberration = 0.08f,
        contentDistortion = 0f,
    )
    LiquidGlassAdvancedPreset.BALANCED -> LiquidGlassAdvancedSettings(
        preset = preset,
        progressiveBlurRadius = 0.40f,
        progressiveBlurExtent = 1.0f,
        progressiveBlurCurve = 0.55f,
        contentReadability = 0.62f,
        chromaticAberration = 0.56f,
        contentDistortion = 0.45f,
    )
    LiquidGlassAdvancedPreset.PRISM -> LiquidGlassAdvancedSettings(
        preset = preset,
        progressiveBlurRadius = 0.8f,
        progressiveBlurExtent = 0.9f,
        progressiveBlurCurve = 0.65f,
        contentReadability = 0.72f,
        chromaticAberration = 0.96f,
        contentDistortion = 1f,
    )
    LiquidGlassAdvancedPreset.CUSTOM -> LiquidGlassAdvancedSettings(preset = preset)
}

internal fun resolveLiquidGlassAdvancedSettings(
    presetValue: Int?,
    progressiveBlurRadius: Float? = null,
    progressiveBlurExtent: Float? = null,
    progressiveBlurCurve: Float? = null,
    contentReadability: Float?,
    chromaticAberration: Float?,
    contentDistortion: Float?,
): LiquidGlassAdvancedSettings {
    val preset = presetValue
        ?.let(LiquidGlassAdvancedPreset::fromValue)
        ?: LiquidGlassAdvancedPreset.BALANCED
    val defaults = resolveLiquidGlassAdvancedPreset(preset)
    if (preset != LiquidGlassAdvancedPreset.CUSTOM) return defaults
    return defaults.copy(
        progressiveBlurRadius = normalizeLiquidGlassAdvancedValue(
            progressiveBlurRadius ?: defaults.progressiveBlurRadius,
            defaults.progressiveBlurRadius,
        ),
        progressiveBlurExtent = normalizeLiquidGlassAdvancedValue(
            progressiveBlurExtent ?: defaults.progressiveBlurExtent,
            defaults.progressiveBlurExtent,
        ),
        progressiveBlurCurve = normalizeLiquidGlassAdvancedValue(
            progressiveBlurCurve ?: defaults.progressiveBlurCurve,
            defaults.progressiveBlurCurve,
        ),
        contentReadability = normalizeLiquidGlassAdvancedValue(
            contentReadability ?: defaults.contentReadability,
            defaults.contentReadability,
        ),
        chromaticAberration = normalizeLiquidGlassAdvancedValue(
            chromaticAberration ?: defaults.chromaticAberration,
            defaults.chromaticAberration,
        ),
        contentDistortion = normalizeLiquidGlassAdvancedValue(
            contentDistortion ?: defaults.contentDistortion,
            defaults.contentDistortion,
        ),
    )
}

internal fun resolveLegacyLiquidGlassMode(style: LiquidGlassStyle): LiquidGlassMode = when (style) {
    LiquidGlassStyle.IOS26 -> LiquidGlassMode.CLEAR
    LiquidGlassStyle.CLASSIC -> LiquidGlassMode.BALANCED
    LiquidGlassStyle.SUKISU -> LiquidGlassMode.BALANCED
}

internal fun resolveDefaultLiquidGlassStrength(mode: LiquidGlassMode): Float = when (mode) {
    LiquidGlassMode.CLEAR -> 0.42f
    LiquidGlassMode.BALANCED -> 0.52f
    LiquidGlassMode.FROSTED -> 0.62f
}

internal fun normalizeLiquidGlassStrength(value: Float): Float =
    if (value.isFinite()) value.coerceIn(0f, 1f) else 0.52f

internal fun normalizeLiquidGlassProgress(value: Float): Float =
    if (value.isFinite()) value.coerceIn(0f, 1f) else 0.5f

/** 长按倍速提示整体缩放（0.8×–1.5×，默认 1.0×）。 */
internal const val LONG_PRESS_SPEED_HINT_SCALE_MIN = 0.8f
internal const val LONG_PRESS_SPEED_HINT_SCALE_MAX = 1.5f
internal const val LONG_PRESS_SPEED_HINT_DEFAULT_SCALE = 1.0f
internal const val LONG_PRESS_SPEED_HINT_ALPHA_MIN = 0.3f
internal const val LONG_PRESS_SPEED_HINT_ALPHA_MAX = 1.0f
internal const val LONG_PRESS_SPEED_HINT_DEFAULT_ALPHA = 0.5f
internal const val LONG_PRESS_SPEED_HINT_STEP = 0.05f

internal fun normalizeLongPressSpeedHintScale(value: Float): Float =
    if (!value.isFinite()) LONG_PRESS_SPEED_HINT_DEFAULT_SCALE
    else value.coerceIn(LONG_PRESS_SPEED_HINT_SCALE_MIN, LONG_PRESS_SPEED_HINT_SCALE_MAX)

/** 长按倍速提示背景/内容透明度（0.3–1.0，默认 0.5）。 */
internal fun normalizeLongPressSpeedHintAlpha(value: Float): Float =
    if (!value.isFinite()) LONG_PRESS_SPEED_HINT_DEFAULT_ALPHA
    else value.coerceIn(LONG_PRESS_SPEED_HINT_ALPHA_MIN, LONG_PRESS_SPEED_HINT_ALPHA_MAX)

internal fun resolveLegacyLiquidGlassProgress(
    mode: LiquidGlassMode,
    strength: Float
): Float {
    val normalizedStrength = normalizeLiquidGlassStrength(strength)
    val (start, end) = when (mode) {
        LiquidGlassMode.CLEAR -> 0f to 0.32f
        LiquidGlassMode.BALANCED -> 0.34f to 0.66f
        LiquidGlassMode.FROSTED -> 0.68f to 1f
    }
    return normalizeLiquidGlassProgress(start + (end - start) * normalizedStrength)
}

internal fun resolveLegacyLiquidGlassProgress(style: LiquidGlassStyle): Float {
    val mode = resolveLegacyLiquidGlassMode(style)
    return resolveLegacyLiquidGlassProgress(
        mode = mode,
        strength = resolveDefaultLiquidGlassStrength(mode)
    )
}

/** Prefer the v2 continuous value, falling back to the complete legacy material selection. */
internal fun resolveStoredLiquidGlassProgress(
    progress: Float?,
    legacyModeValue: Int?,
    legacyStrength: Float?,
    legacyStyleValue: Int?,
): Float {
    progress?.let { return normalizeLiquidGlassProgress(it) }
    val legacyStyle = LiquidGlassStyle.fromValue(
        legacyStyleValue ?: LiquidGlassStyle.SUKISU.value
    )
    val mode = legacyModeValue
        ?.let(LiquidGlassMode::fromValue)
        ?: resolveLegacyLiquidGlassMode(legacyStyle)
    val strength = normalizeLiquidGlassStrength(
        legacyStrength ?: resolveDefaultLiquidGlassStrength(mode)
    )
    return resolveLegacyLiquidGlassProgress(mode, strength)
}

internal fun resolveLiquidGlassModeFromProgress(progress: Float): LiquidGlassMode {
    val normalizedProgress = normalizeLiquidGlassProgress(progress)
    return when {
        normalizedProgress < 0.34f -> LiquidGlassMode.CLEAR
        normalizedProgress < 0.68f -> LiquidGlassMode.BALANCED
        else -> LiquidGlassMode.FROSTED
    }
}

internal fun resolveLiquidGlassStrengthFromProgress(progress: Float): Float {
    val normalizedProgress = normalizeLiquidGlassProgress(progress)
    val mode = resolveLiquidGlassModeFromProgress(normalizedProgress)
    val (start, end) = when (mode) {
        LiquidGlassMode.CLEAR -> 0f to 0.32f
        LiquidGlassMode.BALANCED -> 0.34f to 0.66f
        LiquidGlassMode.FROSTED -> 0.68f to 1f
    }
    return normalizeLiquidGlassStrength(
        if (end <= start) {
            0f
        } else {
            (normalizedProgress - start) / (end - start)
        }
    )
}

internal fun resolveLegacyLiquidGlassStyleFromProgress(progress: Float): LiquidGlassStyle {
    return when (resolveLiquidGlassModeFromProgress(progress)) {
        LiquidGlassMode.CLEAR -> LiquidGlassStyle.IOS26
        LiquidGlassMode.BALANCED -> LiquidGlassStyle.CLASSIC
        LiquidGlassMode.FROSTED -> LiquidGlassStyle.SUKISU
    }
}

enum class HomeHeaderBlurMode(val value: Int, val label: String) {
    FOLLOW_PRESET(0, "跟随预设"),
    ALWAYS_ON(1, "始终开启"),
    ALWAYS_OFF(2, "始终关闭");

    companion object {
        fun fromValue(value: Int): HomeHeaderBlurMode {
            return entries.find { it.value == value } ?: FOLLOW_PRESET
        }
    }
}

internal fun resolveHomeHeaderBlurEnabled(
    mode: HomeHeaderBlurMode,
): Boolean {
    return when (mode) {
        HomeHeaderBlurMode.FOLLOW_PRESET -> true
        HomeHeaderBlurMode.ALWAYS_ON -> true
        HomeHeaderBlurMode.ALWAYS_OFF -> false
    }
}

internal fun resolveHomeHeaderBlurModePreference(
    rawMode: Int?,
    legacyEnabled: Boolean?
): HomeHeaderBlurMode {
    return if (rawMode != null) {
        HomeHeaderBlurMode.fromValue(rawMode)
    } else if (legacyEnabled == false) {
        HomeHeaderBlurMode.ALWAYS_OFF
    } else {
        HomeHeaderBlurMode.FOLLOW_PRESET
    }
}

enum class PlaybackCompletionBehavior(val value: Int, val label: String) {
    CONTINUE_CURRENT_LOGIC(0, "自动连播"),
    STOP_AFTER_CURRENT(1, "播完暂停"),
    PLAY_IN_ORDER(2, "顺序播放"),
    REPEAT_ONE(3, "单个循环"),
    LOOP_PLAYLIST(4, "列表循环");

    companion object {
        fun fromValue(value: Int): PlaybackCompletionBehavior {
            return entries.find { it.value == value } ?: CONTINUE_CURRENT_LOGIC
        }
    }
}

enum class PortraitPlayerCollapseMode(val value: Int, val label: String, val description: String) {
    OFF(0, "关闭", "不自动缩小播放器"),
    INTRO_ONLY(1, "竖屏", "竖屏视频评论区或简介上滑时缩小播放器"),
    COMMENT_ONLY(2, "横屏", "仅横屏视频详情页滚动时缩小播放器"),
    BOTH(3, "全部", "横竖屏视频都使用播放器缩小策略"),
    PAUSED_ONLY(4, "暂停时", "横竖屏视频暂停后，下滑评论或简介可缩小播放器");

    val enablesPortraitVideo: Boolean
        get() = this == INTRO_ONLY || this == BOTH || this == PAUSED_ONLY

    val enablesLandscapeVideo: Boolean
        get() = this == COMMENT_ONLY || this == BOTH || this == PAUSED_ONLY

    fun enablesVideoOrientation(isVerticalVideo: Boolean): Boolean {
        return if (isVerticalVideo) enablesPortraitVideo else enablesLandscapeVideo
    }

    val enablesIntro: Boolean
        get() = this != OFF

    val enablesComment: Boolean
        get() = this != OFF

    companion object {
        fun fromValue(value: Int): PortraitPlayerCollapseMode {
            return entries.find { it.value == value } ?: OFF
        }

        fun fromLegacySwipeHide(enabled: Boolean): PortraitPlayerCollapseMode {
            return if (enabled) INTRO_ONLY else OFF
        }
    }
}

enum class FullscreenMode(val value: Int, val label: String, val description: String) {
    AUTO(0, "自动", "按视频方向自动切换全屏方向"),
    NONE(1, "不改方向", "保持当前方向，仅切换全屏 UI"),
    VERTICAL(2, "竖屏", "进入全屏时保持竖屏"),
    HORIZONTAL(3, "横屏", "进入全屏时切换到横屏");

    companion object {
        fun fromValue(value: Int): FullscreenMode {
            return when (value) {
                // 兼容历史配置：已下线的模式统一回收为 AUTO，避免老用户进入无感知分支。
                4, 5 -> AUTO
                else -> entries.find { it.value == value } ?: AUTO
            }
        }
    }
}

enum class FullscreenAspectRatio(val value: Int, val label: String, val description: String) {
    FIT(0, "适应", "完整显示画面，尽量不裁切"),
    FILL(1, "填充", "填满屏幕，可能裁切边缘"),
    RATIO_16_9(2, "16:9", "优先按 16:9 展示画面"),
    RATIO_4_3(3, "4:3", "优先按 4:3 展示画面"),
    STRETCH(4, "拉伸", "铺满屏幕，可能导致画面变形");

    companion object {
        fun fromValue(value: Int): FullscreenAspectRatio {
            return entries.find { it.value == value } ?: FIT
        }
    }
}

enum class BottomProgressBehavior(
    val value: Int,
    val label: String,
    val description: String
) {
    ALWAYS_SHOW(0, "始终展示", "控件隐藏时始终显示底部细进度条"),
    ALWAYS_HIDE(1, "始终隐藏", "不显示底部细进度条"),
    ONLY_SHOW_FULLSCREEN(2, "仅全屏时展示", "仅横屏全屏且控件隐藏时显示"),
    ONLY_HIDE_FULLSCREEN(3, "仅全屏时隐藏", "非全屏且控件隐藏时显示");

    companion object {
        fun fromValue(value: Int): BottomProgressBehavior {
            return entries.find { it.value == value } ?: ALWAYS_HIDE
        }
    }
}

enum class PlayerProgressPlacement(
    val value: Int,
    val label: String
) {
    ABOVE_CONTROLS(0, "控制栏上方"),
    BOTTOM_EDGE(1, "视频最底部");

    companion object {
        fun fromValue(value: Int): PlayerProgressPlacement {
            return entries.find { it.value == value } ?: ABOVE_CONTROLS
        }
    }
}

data class PlayerControlVisibilitySettings(
    val showCastButton: Boolean = true,
    val showFollowButton: Boolean = true
)

internal fun normalizeDanmakuDisplayArea(value: Float): Float {
    val normalized = value.coerceIn(0.25f, 1.0f)
    val supportedOptions = floatArrayOf(0.25f, 0.5f, 0.75f, 1.0f)
    return supportedOptions.minByOrNull { abs(it - normalized) } ?: 0.5f
}

internal fun normalizeDanmakuFontScale(value: Float): Float = value.coerceIn(0.3f, 2.0f)

internal const val DEFAULT_HOME_REFRESH_COUNT = 20
internal const val MIN_HOME_REFRESH_COUNT = 10
internal const val MAX_HOME_REFRESH_COUNT = 30

internal fun normalizeHomeRefreshCount(count: Int): Int {
    return count.coerceIn(MIN_HOME_REFRESH_COUNT, MAX_HOME_REFRESH_COUNT)
}

enum class HomeFeedCardWidthPreset(
    val value: Int,
    val label: String,
    val minCardWidthDp: Int?
) {
    AUTO(0, "自动", null),
    COMPACT(1, "紧凑", 160),
    BALANCED(2, "均衡", 200),
    WIDE(3, "宽卡片", 260),
    ULTRA_WIDE(4, "超宽", 320);

    companion object {
        fun fromValue(value: Int): HomeFeedCardWidthPreset =
            entries.find { it.value == value } ?: AUTO
    }
}

/**
 * 双列视频卡封面框三档（全局一份设置，均居中 Crop）：
 * - [CURRENT] 16:9：与 CDN 投稿源同比例，标准封面几乎不裁
 * - [OFFICIAL] 4:3：更高列表框，左右会裁
 * - [BILIPAI] 16:10：默认信息流封面比例
 */
enum class HomeFeedCardStyle(val value: Int, val label: String, val subtitle: String) {
    CURRENT(0, "16:9", "完整显示，接近投稿源图"),
    OFFICIAL(1, "4:3", "更高列表框，左右居中裁切"),
    BILIPAI(2, "16:10", "默认信息流封面比例");

    companion object {
        fun fromValue(value: Int): HomeFeedCardStyle =
            entries.find { it.value == value } ?: BILIPAI
    }
}

enum class HomeDurationStyle(val value: Int, val label: String) {
    OUTSIDE_COVER(0, "封面外"),
    OVERLAY_TEXT_ONLY(1, "封面内无底色"),
    HIDDEN(2, "隐藏");

    companion object {
        fun fromValue(value: Int): HomeDurationStyle =
            entries.find { it.value == value } ?: OUTSIDE_COVER
    }
}

/**
 * 首页/列表视频卡片标签（播放量、时长、信息区）表面效果。
 * 轻模糊在滚动时会降级为软玻璃，避免列表掉帧。
 */
enum class HomeCardBadgeEffectMode(
    val value: Int,
    val label: String,
    val subtitle: String
) {
    OFF(0, "关闭", "纯文字标签，性能最好"),
    SOFT_GLASS(1, "软玻璃", "半透明描边拟态，无实时采样"),
    LIGHT_BLUR(2, "实时模糊", "开发中，请勿使用：Haze 采样壁纸/背景");

    companion object {
        fun fromValue(value: Int): HomeCardBadgeEffectMode =
            entries.find { it.value == value } ?: SOFT_GLASS
    }
}

/**
 * Card info strip (title + UP under cover) glass — independent of cover badge pills.
 * Realtime blur (Haze) and realtime liquid glass (LayerBackdrop) are separate modes.
 */
enum class HomeCardInfoGlassMode(
    val value: Int,
    val label: String,
    val subtitle: String
) {
    OFF(0, "关闭", "实色/轻 tint，性能最好（推荐）"),
    REALTIME_BLUR(1, "实时模糊", "开发中，请勿使用：Haze 采样壁纸磨砂"),
    REALTIME_LIQUID_GLASS(2, "实时液态玻璃", "开发中，请勿使用：折射液态玻璃"),
    BLUR_AND_LIQUID(3, "模糊+液态", "开发中，请勿使用：Haze + 液态叠加");

    val usesRealtimeBlur: Boolean
        get() = this == REALTIME_BLUR || this == BLUR_AND_LIQUID

    val usesRealtimeLiquidGlass: Boolean
        get() = this == REALTIME_LIQUID_GLASS || this == BLUR_AND_LIQUID

    companion object {
        fun fromValue(value: Int): HomeCardInfoGlassMode =
            entries.find { it.value == value } ?: OFF
    }
}

enum class BottomBarLiquidGlassPreset(
    val value: Int,
    val label: String,
    val description: String
) {
    BILIPAI_TUNED(
        0,
        "BiliPai 调校",
        "保留当前多层折射、色散和指示器动效"
    ),
    IOS26_REFINED(
        1,
        "iOS 26 玻璃",
        "厚边折射 + 顶光高亮环，无色散，沿用 BiliPai 指示器滑动与配色"
    );

    companion object {
        fun fromValue(value: Int): BottomBarLiquidGlassPreset =
            entries.find { it.value == value } ?: BILIPAI_TUNED
    }
}

data class HomeSettings(
    val displayMode: Int = 0,              // 展示模式 (0=网格, 1=故事卡片)
    val isBottomBarFloating: Boolean = true,
    val navigationIconCrossScaleEnabled: Boolean = true,
    val bottomBarLabelMode: Int = 0,       // (0=图标+文字, 1=仅图标, 2=仅文字)
    val topTabLabelMode: Int = 2,          // (0=图标+文字, 1=仅图标, 2=仅文字)
    val hideTopTabs: Boolean = false,
    val homeTopRightAction: HomeTopRightAction = HomeTopRightAction.SETTINGS,
    val homeTopLayoutOrder: HomeTopLayoutOrder = HomeTopLayoutOrder.SEARCH_THEN_TABS,
    val isHeaderBlurEnabled: Boolean = true,
    val headerBlurMode: HomeHeaderBlurMode = HomeHeaderBlurMode.FOLLOW_PRESET,
    val isBottomBarBlurEnabled: Boolean = false,
    val isTopBarLiquidGlassEnabled: Boolean = false,
    val isHomeSearchLiquidGlassEnabled: Boolean = false,
    val isBottomBarLiquidGlassEnabled: Boolean = false,
    val bottomBarLiquidGlassPreset: BottomBarLiquidGlassPreset =
        BottomBarLiquidGlassPreset.BILIPAI_TUNED,
    val isBottomBarSearchEnabled: Boolean = false,
    val bottomBarSearchAutoExpandMode: BottomBarSearchAutoExpandMode =
        BottomBarSearchAutoExpandMode.EXPAND_AT_HOME_TOP,
    val bottomBarSearchLayoutMode: BottomBarSearchLayoutMode =
        BottomBarSearchLayoutMode.FULL_DOCK,
    val androidNativeLiquidGlassEnabled: Boolean = false,
    val liquidGlassStyle: LiquidGlassStyle = LiquidGlassStyle.CLASSIC, // [New]
    val liquidGlassMode: LiquidGlassMode = LiquidGlassMode.BALANCED,
    val liquidGlassStrength: Float = 0.52f,
    val liquidGlassProgress: Float = 0.5f,
    val liquidGlassReadabilityMode: LiquidGlassReadabilityMode =
        LiquidGlassReadabilityMode.STABLE,
    val liquidGlassAdvancedSettings: LiquidGlassAdvancedSettings = LiquidGlassAdvancedSettings(),
    val homeHeaderCollapseMode: HomeHeaderCollapseMode = HomeHeaderCollapseMode.BOTH,
    val homeBarHideType: HomeBarHideType = HomeBarHideType.SYNC,
    val commonListHeaderCollapseMode: CommonListHeaderCollapseMode =
        CommonListHeaderCollapseMode.SHOW_ON_REVERSE_SCROLL,
    val isHeaderCollapseEnabled: Boolean = true,
    val showPgcTimeline: Boolean = true,
    val gridColumnCount: Int = 0, // [New] 网格列数 (0=自动, 1-6=固定)
    val pinchToChangeGridColumnsEnabled: Boolean = true, // [新增] 双指缩放切换网格列数
    val homeFeedCardWidthPreset: HomeFeedCardWidthPreset = HomeFeedCardWidthPreset.AUTO,
    val homeFeedCardStyle: HomeFeedCardStyle = HomeFeedCardStyle.BILIPAI,
    val homeHeroCarouselEnabled: Boolean = true,
    val homeHeroCarouselAutoplayEnabled: Boolean = false,
    val cardAnimationEnabled: Boolean = false,    //  卡片进场动画（默认关闭）
    val cardTransitionEnabled: Boolean = true,    //  卡片过渡动画（默认开启）
    val videoSharedTransitionSpeed: VideoSharedTransitionSpeed = VideoSharedTransitionSpeed.STANDARD,
    val videoSharedTransitionCustomDurationMillis: Int =
        VIDEO_SHARED_TRANSITION_CUSTOM_DEFAULT_MILLIS,
    // [Retired] 旧的首页 feed「智能流畅优先」，固定关闭。
    // 运行时视觉守卫是另一套机制，见 [runtimeVisualGuardEnabled]。
    val smartVisualGuardEnabled: Boolean = false,
    // 运行时视觉守卫：连续掉帧时自动降级毛玻璃/液态玻璃/景深。
    // 影响面覆盖全 App 视觉，必须保留 kill switch——某机型 JankStats 读数异常时可关闭。
    val runtimeVisualGuardEnabled: Boolean = true,
    val compactVideoStatsOnCover: Boolean = false, // 播放/弹幕位于信息区，不叠加在封面上
    val lowQualityHomeCoverInDataSaver: Boolean = false, // 省流量时首页封面使用低清晰度
    // 卡片标签 / 信息区玻璃效果已下线，保留字段仅为兼容旧数据结构。
    val showHomeCoverGlassBadges: Boolean = false,
    val showHomeInfoGlassBadges: Boolean = false,
    val homeCardBadgeEffectMode: HomeCardBadgeEffectMode = HomeCardBadgeEffectMode.OFF,
    val homeCardInfoGlassMode: HomeCardInfoGlassMode = HomeCardInfoGlassMode.OFF,
    val homeWallpaperEffectMode: HomeWallpaperEffectMode = HomeWallpaperEffectMode.SOFT_BLUR,
    val homeWallpaperEffectScope: HomeWallpaperEffectScope = HomeWallpaperEffectScope.HOME_ONLY,
    val showHomeUpBadges: Boolean = false, // 首页和相关推荐 UP 主标识显示(默认关闭,设置后全局生效)
    val showHomeUpAvatars: Boolean = false, // 首页视频卡片 UP 主头像显示(默认关闭,设置后全局生效)
    val showHomePublishTime: Boolean = true, // 首页视频卡片发布时间（默认显示，可关闭）
    val showFullVideoCardContent: Boolean = false, // 视频卡片标题完整展示(默认关闭,设置后全局生效)
    val videoCardLongPressActionEnabled: Boolean = false, // 长按视频卡片快捷操作与预览（默认关闭）
    val homeCardDynamicTintEnabled: Boolean = true, // 卡片毛玻璃与动态取色
    val homeDurationStyle: HomeDurationStyle = HomeDurationStyle.OUTSIDE_COVER,
    val easterEggEnabled: Boolean = false, // 下拉刷新趣味提示开关
    //  [修复] 默认值改为 true，避免在 Flow 加载实际值之前错误触发弹窗
    // 当 Flow 加载完成后，如果实际值是 false，LaunchedEffect 会再次触发并显示弹窗
    val crashTrackingConsentShown: Boolean = true
) {
    val isLiquidGlassEnabled: Boolean
        get() = androidNativeLiquidGlassEnabled
}

data class AppThemeSettings(
    val uiStyle: AppUiStyle = AppUiStyle.MATERIAL3,
    val themeMode: AppThemeMode = AppThemeMode.FOLLOW_SYSTEM,
    val darkThemeStyle: DarkThemeStyle = DarkThemeStyle.DEFAULT,
    val appLanguage: AppLanguage = AppLanguage.FOLLOW_SYSTEM,
    val md3ColorSource: Md3ColorSource = Md3ColorSource.FOLLOW_WALLPAPER,
    val md3CustomColorHex: String = "#007AFF",
    val themeRoleOverrides: ThemeRoleOverrides = ThemeRoleOverrides(),
    val colorStyle: PaletteStyle = PaletteStyle.TonalSpot,
    val colorSpec: ColorSpec.SpecVersion = ColorSpec.SpecVersion.SPEC_2021,
    val themeColorIndex: Int = 0,
    val appFontSizePreset: AppFontSizePreset = AppFontSizePreset.DEFAULT,
    val appFontFileName: String = "",
    val appUiScalePreset: AppUiScalePreset = AppUiScalePreset.STANDARD,
    val appDpiOverridePercent: Int = 0,
    val appGestureScreenshotEnabled: Boolean = false,
    val appScreenshotGestureMode: AppScreenshotGestureMode =
        AppScreenshotGestureMode.TOP_RIGHT_TWO_FINGER_LONG_PRESS,
    val appScreenshotCaptureMode: AppScreenshotCaptureMode =
        AppScreenshotCaptureMode.FULL_WINDOW,
    val appIconStyle: AppIconStyle = AppIconStyle.AUTO,
    val appListItemStyle: AppListItemStyle = AppListItemStyle.AUTO,
    val singleChoicePresentation: AppSingleChoicePresentation =
        AppSingleChoicePresentation.WINDOW_POPUP,
)

data class ThemeModeRoleOverrides(
    val backgroundHex: String,
    val primaryTextHex: String,
    val secondaryTextHex: String,
    val controlAccentHex: String
)

data class ThemeRoleOverrides(
    val enabled: Boolean = false,
    val light: ThemeModeRoleOverrides = ThemeModeRoleOverrides(
        backgroundHex = "#FFFDF8",
        primaryTextHex = "#1C1B1F",
        secondaryTextHex = "#49454F",
        controlAccentHex = "#0061A4"
    ),
    val dark: ThemeModeRoleOverrides = ThemeModeRoleOverrides(
        backgroundHex = "#121212",
        primaryTextHex = "#E6E1E5",
        secondaryTextHex = "#CAC4D0",
        controlAccentHex = "#9ECAFF"
    )
)

enum class BottomBarSearchAutoExpandMode(val value: Int, val label: String) {
    EXPAND_WHEN_SCROLLING_DOWN(0, "下滑展开"),
    EXPAND_AT_HOME_TOP(1, "顶部展开"),
    DISABLED(2, "不自动展开");

    companion object {
        fun fromValue(value: Int): BottomBarSearchAutoExpandMode =
            entries.find { it.value == value } ?: EXPAND_AT_HOME_TOP
    }
}

enum class BottomBarSearchLayoutMode(val value: Int, val label: String) {
    FULL_DOCK(0, "完整底栏"),
    HOME_AND_SEARCH(1, "首页与搜索");

    companion object {
        fun fromValue(value: Int): BottomBarSearchLayoutMode =
            entries.find { it.value == value } ?: FULL_DOCK
    }
}

enum class HomeWallpaperEffectMode(val value: Int, val label: String) {
    OFF(0, "关闭"),
    SOFT_BLUR(1, "轻微模糊"),
    ORIGINAL(2, "原图"),
    STRONG_BLUR(3, "强模糊");

    companion object {
        fun fromValue(value: Int): HomeWallpaperEffectMode =
            entries.find { it.value == value } ?: SOFT_BLUR
    }
}

enum class HomeWallpaperEffectScope(val value: Int, val label: String) {
    HOME_ONLY(0, "仅首页"),
    GLOBAL(1, "首页与聊天");

    companion object {
        fun fromValue(value: Int): HomeWallpaperEffectScope =
            entries.find { it.value == value } ?: HOME_ONLY
    }
}

enum class HomeTopRightAction(val value: Int, val label: String) {
    SETTINGS(0, "设置"),
    INBOX(1, "消息");

    companion object {
        fun fromValue(value: Int): HomeTopRightAction =
            entries.find { it.value == value } ?: SETTINGS
    }
}

enum class HomeTopLayoutOrder(val value: Int, val label: String) {
    SEARCH_THEN_TABS(0, "搜索在上"),
    TABS_THEN_SEARCH(1, "标签在上");

    companion object {
        fun fromValue(value: Int): HomeTopLayoutOrder =
            entries.find { it.value == value } ?: SEARCH_THEN_TABS
    }
}

enum class HomeHeaderCollapseMode(
    val value: Int,
    val label: String,
    val description: String,
    val collapseSearch: Boolean,
    val collapseTabs: Boolean
) {
    SEARCH_ONLY(0, "仅搜索", "列表下滑时只收起搜索行，标签页保持显示", true, false),
    TABS_ONLY(1, "仅标签", "列表下滑时只收起标签页，搜索行保持显示", false, true),
    BOTH(2, "都折叠", "搜索行和标签页都会随列表下滑收起", true, true),
    OFF(3, "都不折叠", "搜索行和标签页始终展开", false, false);

    val hasAnyCollapse: Boolean
        get() = collapseSearch || collapseTabs

    val hideTopBar: Boolean
        get() = collapseSearch

    companion object {
        fun fromValue(value: Int): HomeHeaderCollapseMode =
            entries.find { it.value == value } ?: BOTH

        fun fromLegacyBoolean(value: Boolean): HomeHeaderCollapseMode =
            if (value) BOTH else OFF
    }
}

enum class HomeBarHideType(
    val value: Int,
    val label: String,
    val description: String
) {
    SYNC(0, "同步", "顶栏高度跟随列表滑动，上滑展开、下滑收起"),
    INSTANT(1, "即时", "识别滑动方向后，顶栏整段收起或展开");

    companion object {
        fun fromValue(value: Int): HomeBarHideType =
            entries.find { it.value == value } ?: SYNC
    }
}

enum class CommonListHeaderCollapseMode(
    val value: Int,
    val label: String,
    val description: String
) {
    ALWAYS_VISIBLE(0, "始终显示", "历史记录和收藏夹等通用列表的顶部栏保持展开"),
    SHOW_ON_REVERSE_SCROLL(1, "上滑时显示", "向下浏览时折叠，反向上滑时恢复"),
    SHOW_AT_TOP_ONLY(2, "仅回顶显示", "向下浏览时折叠，仅回到列表顶部时恢复");

    companion object {
        fun fromValue(value: Int): CommonListHeaderCollapseMode =
            entries.find { it.value == value } ?: SHOW_ON_REVERSE_SCROLL
    }
}

internal fun resolveHomeHeaderCollapseModeForTopBarHide(
    hideTopBar: Boolean
): HomeHeaderCollapseMode {
    return if (hideTopBar) HomeHeaderCollapseMode.BOTH else HomeHeaderCollapseMode.OFF
}

@Suppress("UNUSED_PARAMETER")
internal fun resolveHomeHeaderCollapseModeForTopTabs(
    currentMode: HomeHeaderCollapseMode,
    collapseTabs: Boolean
): HomeHeaderCollapseMode {
    return resolveHomeHeaderCollapseModeForTopBarHide(currentMode.hideTopBar)
}

@Suppress("UNUSED_PARAMETER")
internal fun resolveHomeHeaderCollapseModeForSearch(
    currentMode: HomeHeaderCollapseMode,
    collapseSearch: Boolean
): HomeHeaderCollapseMode {
    return resolveHomeHeaderCollapseModeForTopBarHide(collapseSearch)
}

internal fun resolveUiPresetPreferenceValue(rawValue: Int?): UiPreset {
    return UiPreset.fromValue(rawValue ?: UiPreset.MD3.value)
}

internal fun resolveAndroidNativeVariantPreferenceValue(rawValue: Int?): AndroidNativeVariant {
    return AndroidNativeVariant.fromValue(rawValue ?: AndroidNativeVariant.MATERIAL3.value)
}

enum class DanmakuPanelWidthMode(val value: Int, val label: String, val widthFraction: Float) {
    FULL(0, "全宽", 1f),
    HALF(1, "半屏", 0.5f),
    THIRD(2, "1/3 屏", 1f / 3f);

    companion object {
        fun fromValue(value: Int): DanmakuPanelWidthMode =
            entries.find { it.value == value } ?: THIRD
    }
}

enum class PortraitDanmakuDisplayAreaMode(val value: Int, val label: String) {
    VIDEO_VIEWPORT(0, "视频画面"),
    SCREEN_TOP(1, "屏幕顶部");

    companion object {
        fun fromValue(value: Int): PortraitDanmakuDisplayAreaMode =
            entries.find { it.value == value } ?: VIDEO_VIEWPORT
    }
}

enum class TabletCommentPanelWidthPreset(
    val value: Int,
    val label: String
) {
    COMPACT(0, "窄"),
    STANDARD(1, "标准"),
    WIDE(2, "宽"),
    ULTRA_WIDE(3, "超宽");

    companion object {
        fun fromValue(value: Int): TabletCommentPanelWidthPreset =
            entries.find { it.value == value } ?: STANDARD
    }
}

enum class TabletSecondaryDefaultTab(val value: Int, val label: String) {
    COMMENTS(0, "评论"),
    RELATED(1, "推荐");

    companion object {
        fun fromValue(value: Int): TabletSecondaryDefaultTab =
            entries.find { it.value == value } ?: RELATED
    }
}

internal fun normalizeDanmakuFullscreenPanelWidthMode(
    mode: DanmakuPanelWidthMode
): DanmakuPanelWidthMode = DanmakuPanelWidthMode.THIRD

enum class DanmakuSettingsScope(
    val keyPrefix: String,
    val badgeLabel: String,
    val subtitle: String
) {
    PORTRAIT(
        keyPrefix = "portrait",
        badgeLabel = "竖屏专用",
        subtitle = "开关、字号和区域与横屏同步，其余样式独立"
    ),
    LANDSCAPE(
        keyPrefix = "landscape",
        badgeLabel = "横屏专用",
        subtitle = "开关、字号和区域与竖屏同步，其余样式独立"
    )
}

internal fun resolveDanmakuSettingsScope(isLandscape: Boolean): DanmakuSettingsScope {
    return if (isLandscape) DanmakuSettingsScope.LANDSCAPE else DanmakuSettingsScope.PORTRAIT
}

data class DanmakuSettings(
    val enabled: Boolean = true,
    val opacity: Float = DANMAKU_DEFAULT_OPACITY,
    val fontScale: Float = 1.0f,
    val speed: Float = 1.0f,
    val displayArea: Float = 0.5f,
    val fontWeight: Int = 5,
    val strokeWidth: Float = 1.5f,
    val lineHeight: Float = 1.6f,
    val scrollDurationSeconds: Float = 7.0f,
    val staticDurationSeconds: Float = 4.0f,
    val scrollFixedVelocity: Boolean = false,
    val staticDanmakuToScroll: Boolean = false,
    val massiveMode: Boolean = false,
    val mergeDuplicates: Boolean = true,
    val duplicateMergeWindowMs: Int = 500,
    val duplicateMergeCountThreshold: Int = 2,
    val allowScroll: Boolean = true,
    val allowTop: Boolean = true,
    val allowBottom: Boolean = true,
    val allowColorful: Boolean = true,
    val allowSpecial: Boolean = true,
    val hideInteractiveCommands: Boolean = false,
    val blockAttentionCommands: Boolean = false,
    val smartOcclusion: Boolean = false,
    val portraitDisplayAreaMode: PortraitDanmakuDisplayAreaMode =
        PortraitDanmakuDisplayAreaMode.VIDEO_VIEWPORT,
    val fullscreenPanelWidthMode: DanmakuPanelWidthMode = DanmakuPanelWidthMode.THIRD,
    val blockRulesRaw: String = "",
    val blockRules: List<String> = emptyList()
)

data class AppNavigationSettings(
    val bottomBarVisibilityMode: SettingsManager.BottomBarVisibilityMode = SettingsManager.BottomBarVisibilityMode.ALWAYS_VISIBLE,
    val orderedVisibleTabIds: List<String> = listOf("HOME", "DYNAMIC", "HISTORY", "LISTEN_VIDEO", "PROFILE"),
    val bottomBarItemColors: Map<String, Int> = emptyMap(),
    val bottomBarItemLabels: Map<String, String> = emptyMap(),
    val tabletUseSidebar: Boolean = false,
    val sidebarExpanded: Boolean = true,
    val sidebarAccountSwitcherEnabled: Boolean = true,
    val predictiveBackEnabled: Boolean = true,
    val predictiveBackAnimationStyle: String = "miuix",
    val predictiveBackExitDirection: String = "always_right",
    val miuixTransitionBlurEnabled: Boolean = true,
    val miuixPredictiveBackMaxProgressPercent: Int = 100,
    val videoSharedReturnGestureFollowEnabled: Boolean = true,
)

internal data class BottomTabMigrationResult(
    val order: List<String>,
    val visible: Set<String>,
    val markComplete: Boolean
)

data class FocusSettings(
    val showHomeRecommendTab: Boolean = false,
    val showHomeFollowTab: Boolean = true,
    val showHomePopularTab: Boolean = false,
    val showHomeLiveTab: Boolean = false,
    val showHomeAnimeTab: Boolean = true,
    val showHomeGameTab: Boolean = false,
    val showHomeKnowledgeTab: Boolean = true,
    val showHomeTechTab: Boolean = true,
    val showHomePartitionButton: Boolean = false,
    val enableFollowGroupFiltering: Boolean = true,
    val showVideoRelatedVideosSection: Boolean = false,
    val showHistoryClearAllAction: Boolean = true,
    val showSearchHotSection: Boolean = false,
    val showSearchDiscoverSection: Boolean = false,
    val showSearchHistorySection: Boolean = false
)

internal fun resolveListenVideoBottomTabMigration(
    order: List<String>,
    visible: Set<String>,
    migrationComplete: Boolean
): BottomTabMigrationResult {
    if (migrationComplete) {
        return BottomTabMigrationResult(order, visible, markComplete = false)
    }
    if ("LISTEN_VIDEO" in order || "LISTEN_VIDEO" in visible || visible.size >= 5) {
        return BottomTabMigrationResult(order, visible, markComplete = true)
    }
    val insertionIndex = order.indexOf("PROFILE").takeIf { it >= 0 } ?: order.size
    val migratedOrder = order.toMutableList().apply {
        add(insertionIndex, "LISTEN_VIDEO")
    }
    return BottomTabMigrationResult(
        order = migratedOrder,
        visible = visible + "LISTEN_VIDEO",
        markComplete = true
    )
}

data class HomeTopTabSettings(
    val orderIds: List<String> = listOf("RECOMMEND", "FOLLOW", "POPULAR", "LIVE", "GAME"),
    val visibleIds: Set<String> = setOf("RECOMMEND", "FOLLOW", "POPULAR", "LIVE", "GAME"),
    val hideTopTabs: Boolean = false
)

/**
 * 自动退出全屏策略。
 * - [OFF]：不自动退
 * - [CURRENT_PART]：当前分P/单视频结束就退（旧「开」语义中的激进行为）
 * - [ALL_PARTS]：还有下一段可连播时保持全屏，全部播完再退（默认）
 */
enum class AutoExitFullscreenMode(val value: Int, val label: String, val subtitle: String) {
    OFF(0, "关闭", "播放结束不自动退出全屏"),
    CURRENT_PART(1, "当前分P结束", "每个分P/视频播完就退出全屏"),
    ALL_PARTS(2, "全部连播结束", "合集/分P/列表全部播完再退出全屏");

    companion object {
        fun fromValue(value: Int): AutoExitFullscreenMode =
            entries.find { it.value == value } ?: ALL_PARTS

        /** 兼容旧布尔：true→全部结束再退，false→关闭 */
        fun fromLegacyEnabled(enabled: Boolean): AutoExitFullscreenMode =
            if (enabled) ALL_PARTS else OFF
    }
}

internal fun resolveAutoExitFullscreenMode(
    modeValue: Int?,
    legacyEnabled: Boolean?,
): AutoExitFullscreenMode {
    if (modeValue != null) return AutoExitFullscreenMode.fromValue(modeValue)
    return AutoExitFullscreenMode.fromLegacyEnabled(legacyEnabled ?: true)
}

data class PlayerInteractionSettings(
    val gestureSensitivity: Float = 1.0f,
    val doubleTapLikeEnabled: Boolean = true,
    val doubleTapSeekEnabled: Boolean = false,
    val portraitSwipeToFullscreenEnabled: Boolean = true,
    val centerSwipeToFullscreenEnabled: Boolean = true,
    val slideVolumeBrightnessEnabled: Boolean = true,
    val setSystemBrightnessEnabled: Boolean = false,
    val pipNoDanmakuEnabled: Boolean = false,
    val seekForwardSeconds: Int = 10,
    val seekBackwardSeconds: Int = 10,
    val inlineSwipeSeekSeconds: Int = 30,
    val fullscreenSwipeSeekSeconds: Int = 15,
    val fullscreenSwipeSeekEnabled: Boolean = true,
    val fullscreenGestureReverse: Boolean = false,
    val hideVideoPageStatusBar: Boolean = false,
    /** 竖屏详情横屏视频上下黑边动态模糊，默认开启。 */
    val portraitLetterboxAmbientHaze: Boolean = true,
    val tabletCommentPanelWidthPreset: TabletCommentPanelWidthPreset =
        TabletCommentPanelWidthPreset.STANDARD,
    val autoEnterFullscreenEnabled: Boolean = false,
    val autoExitFullscreenEnabled: Boolean = true,
    /**
     * 自动退出全屏粒度。旧布尔 [autoExitFullscreenEnabled]=true 映射为 [ALL_PARTS]，
     * 避免连播下一P 时被 STATE_ENDED 踢回竖屏。
     */
    val autoExitFullscreenMode: AutoExitFullscreenMode = AutoExitFullscreenMode.ALL_PARTS,
    val fixedFullscreenAspectRatio: FullscreenAspectRatio = FullscreenAspectRatio.FIT,
    val subtitleAutoPreference: SubtitleAutoPreference = SubtitleAutoPreference.OFF,
    val longPressSpeed: Float = 2.0f,
    val longPressSpeedLockEnabled: Boolean = false,
    val longPressSpeedLockHintShown: Boolean = false,
    /**
     * 长按倍速浮层是否显示关闭（×）按钮。默认 false（始终隐藏），
     * 不在设置页暴露，避免第二指点 × 打断长按加速。
     */
    val longPressSpeedHintCloseEnabled: Boolean = false,
    val longPressSpeedHintHidden: Boolean = false,
    val longPressSpeedHintScale: Float = 1.0f,
    val longPressSpeedHintAlpha: Float = 0.5f,
    val subtitleVerticalOffsetFraction: Float = 0.0f,
    /** Vertical offset for portrait immersive / story subtitles (independent of landscape). */
    val subtitlePortraitVerticalOffsetFraction: Float = 0.0f,
    /** Prevent player gestures from accidentally moving subtitle overlays. */
    val subtitlePositionLocked: Boolean = true,
    val twoFingerVerticalSpeedEnabled: Boolean = false,
    val twoFingerHorizontalSpeedEnabled: Boolean = false,
    val hiResLongPressCompatHintShown: Boolean = false,
    val directPortraitStoryEntry: Boolean = false,
    val launchToPortraitFeedOnStartup: Boolean = false
)

internal sealed interface ShareablePreferenceDefinition {
    val entryDefinition: SettingsShareEntryDefinition

    fun read(preferences: Preferences): JsonElement?

    fun write(preferences: MutablePreferences, value: JsonElement): Boolean
}

internal class BooleanShareablePreferenceDefinition(
    private val key: Preferences.Key<Boolean>,
    section: SettingsShareSection
) : ShareablePreferenceDefinition {
    override val entryDefinition = SettingsShareEntryDefinition(
        storageKey = key.name,
        section = section
    )

    override fun read(preferences: Preferences): JsonElement? {
        return preferences[key]?.let(::JsonPrimitive)
    }

    override fun write(preferences: MutablePreferences, value: JsonElement): Boolean {
        val parsed = value.jsonPrimitive.booleanOrNull ?: return false
        preferences[key] = parsed
        return true
    }
}

internal class IntShareablePreferenceDefinition(
    private val key: Preferences.Key<Int>,
    section: SettingsShareSection
) : ShareablePreferenceDefinition {
    override val entryDefinition = SettingsShareEntryDefinition(
        storageKey = key.name,
        section = section
    )

    override fun read(preferences: Preferences): JsonElement? {
        return preferences[key]?.let(::JsonPrimitive)
    }

    override fun write(preferences: MutablePreferences, value: JsonElement): Boolean {
        val parsed = value.jsonPrimitive.intOrNull ?: return false
        preferences[key] = parsed
        return true
    }
}

internal class FloatShareablePreferenceDefinition(
    private val key: Preferences.Key<Float>,
    section: SettingsShareSection
) : ShareablePreferenceDefinition {
    override val entryDefinition = SettingsShareEntryDefinition(
        storageKey = key.name,
        section = section
    )

    override fun read(preferences: Preferences): JsonElement? {
        return preferences[key]?.let(::JsonPrimitive)
    }

    override fun write(preferences: MutablePreferences, value: JsonElement): Boolean {
        val parsed = value.jsonPrimitive.content.toFloatOrNull() ?: return false
        preferences[key] = parsed
        return true
    }
}

internal class StringShareablePreferenceDefinition(
    private val key: Preferences.Key<String>,
    section: SettingsShareSection
) : ShareablePreferenceDefinition {
    override val entryDefinition = SettingsShareEntryDefinition(
        storageKey = key.name,
        section = section
    )

    override fun read(preferences: Preferences): JsonElement? {
        return preferences[key]?.let(::JsonPrimitive)
    }

    override fun write(preferences: MutablePreferences, value: JsonElement): Boolean {
        preferences[key] = value.jsonPrimitive.content
        return true
    }
}

internal fun mapHomeSettingsFromPreferences(preferences: Preferences): HomeSettings {
    return SettingsManager.mapHomeSettingsFromPreferences(preferences)
}

internal fun mapAppThemeSettingsFromPreferences(preferences: Preferences): AppThemeSettings {
    return SettingsManager.mapAppThemeSettingsFromPreferences(preferences)
}

internal fun mapDanmakuSettingsFromPreferences(
    preferences: Preferences,
    scope: DanmakuSettingsScope = DanmakuSettingsScope.PORTRAIT
): DanmakuSettings {
    return SettingsManager.mapDanmakuSettingsFromPreferences(preferences, scope)
}

internal fun mapAppNavigationSettingsFromPreferences(
    preferences: Preferences,
    defaultTabletUseSidebar: Boolean = false
): AppNavigationSettings {
    return SettingsManager.mapAppNavigationSettingsFromPreferences(
        preferences = preferences,
        defaultTabletUseSidebar = defaultTabletUseSidebar
    )
}

/**
 * 平板导航首启默认值：按设备类型预设（平板默认侧栏，手机默认底栏）。
 * 与 [SettingsManager.getHorizontalAdaptationEnabled] 的 isTablet 预设机制一致。
 */
internal fun defaultTabletUseSidebar(isTabletDevice: Boolean): Boolean = isTabletDevice

internal fun mapHomeTopTabSettingsFromPreferences(preferences: Preferences): HomeTopTabSettings {
    return SettingsManager.mapHomeTopTabSettingsFromPreferences(preferences)
}

internal fun mapPlayerInteractionSettingsFromPreferences(
    preferences: Preferences
): PlayerInteractionSettings {
    return SettingsManager.mapPlayerInteractionSettingsFromPreferences(preferences)
}

internal fun decodeCollectionSubscriptionIds(rawValue: String?): Set<String> {
    return rawValue
        ?.split(",")
        ?.map { it.trim() }
        ?.filter { it.isNotBlank() }
        ?.toSet()
        ?: emptySet()
}

internal fun encodeCollectionSubscriptionIds(collectionIds: Set<String>): String {
    return collectionIds
        .filter { it.isNotBlank() }
        .sorted()
        .joinToString(",")
}

internal fun toggleCollectionSubscription(
    subscribedCollectionIds: Set<String>,
    collectionId: Long
): Set<String> {
    val normalizedId = collectionId.takeIf { it > 0L }?.toString() ?: return subscribedCollectionIds
    return if (normalizedId in subscribedCollectionIds) {
        subscribedCollectionIds - normalizedId
    } else {
        subscribedCollectionIds + normalizedId
    }
}

internal fun setCollectionSubscription(
    subscribedCollectionIds: Set<String>,
    collectionId: Long,
    subscribed: Boolean
): Set<String> {
    val normalizedId = collectionId.takeIf { it > 0L }?.toString() ?: return subscribedCollectionIds
    return if (subscribed) {
        subscribedCollectionIds + normalizedId
    } else {
        subscribedCollectionIds - normalizedId
    }
}

internal fun decodeCollectionSortPreferences(rawValue: String?): Map<Long, CollectionSortMode> {
    if (rawValue.isNullOrBlank()) return emptyMap()
    return runCatching {
        Json.parseToJsonElement(rawValue).jsonObject.entries.mapNotNull { (key, value) ->
            val collectionId = key.toLongOrNull() ?: return@mapNotNull null
            val sortMode = runCatching {
                CollectionSortMode.valueOf(value.jsonPrimitive.content)
            }.getOrNull() ?: return@mapNotNull null
            collectionId to sortMode
        }.toMap()
    }.getOrDefault(emptyMap())
}

internal fun encodeCollectionSortPreferences(
    preferences: Map<Long, CollectionSortMode>
): String {
    if (preferences.isEmpty()) return "{}"
    return preferences.entries
        .sortedBy { it.key }
        .joinToString(
            separator = ",",
            prefix = "{",
            postfix = "}"
        ) { entry ->
            "\"${entry.key}\":\"${entry.value.name}\""
        }
}
