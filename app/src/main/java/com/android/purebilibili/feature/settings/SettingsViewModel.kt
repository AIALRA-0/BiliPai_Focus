// 文件路径: feature/settings/SettingsViewModel.kt
package com.android.purebilibili.feature.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.core.store.BottomBarSearchAutoExpandMode
import com.android.purebilibili.core.store.HomeFeedCardWidthPreset
import com.android.purebilibili.core.store.LiquidGlassMode
import com.android.purebilibili.core.store.allManagedAppIconLauncherAliases
import com.android.purebilibili.core.store.resolveDefaultLiquidGlassStrength
import com.android.purebilibili.core.store.resolveLegacyLiquidGlassProgress
import com.android.purebilibili.core.store.resolveLegacyLiquidGlassMode
import com.android.purebilibili.core.store.normalizeAppIconKey
import com.android.purebilibili.core.store.resolveAppIconLauncherAlias
import com.android.purebilibili.core.theme.AppFontSizePreset
import com.android.purebilibili.core.theme.AppUiScalePreset
import com.android.purebilibili.core.theme.AndroidNativeVariant
import com.android.purebilibili.core.theme.UiPreset
import com.android.purebilibili.core.ui.blur.BlurIntensity
import com.android.purebilibili.core.util.CacheClearTarget
import com.android.purebilibili.core.util.CacheUtils
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val uiPreset: UiPreset = UiPreset.IOS,
    val androidNativeVariant: AndroidNativeVariant = AndroidNativeVariant.MATERIAL3,
    val hwDecode: Boolean = true,
    val themeMode: AppThemeMode = AppThemeMode.FOLLOW_SYSTEM,
    val darkThemeStyle: DarkThemeStyle = DarkThemeStyle.DEFAULT,
    val appLanguage: AppLanguage = AppLanguage.FOLLOW_SYSTEM,
    val dynamicColor: Boolean = true,
    val md3ColorSource: Md3ColorSource = Md3ColorSource.FOLLOW_WALLPAPER,
    val md3CustomColorHex: String = "#007AFF",
    val colorStyle: PaletteStyle = PaletteStyle.TonalSpot,
    val colorSpec: ColorSpec.SpecVersion = ColorSpec.SpecVersion.SPEC_2021,
    val appFontSizePreset: AppFontSizePreset = AppFontSizePreset.DEFAULT,
    val appFontFileName: String = "",
    val appFontDisplayName: String = "",
    val appUiScalePreset: AppUiScalePreset = AppUiScalePreset.STANDARD,
    val appDpiOverridePercent: Int = 0,
    val bgPlay: Boolean = false,
    val gestureSensitivity: Float = 1.0f,
    val themeColorIndex: Int = 0,
    val appIcon: String = "icon_3d",
    val isBottomBarFloating: Boolean = true,
    val bottomBarLabelMode: Int = 1,  // 0=图标+文字, 1=仅图标, 2=仅文字
    val headerBlurEnabled: Boolean = true,
    val bottomBarBlurEnabled: Boolean = true,
    val blurIntensity: BlurIntensity = BlurIntensity.THIN,  //  模糊强度
    val displayMode: Int = 0,
    val cardAnimationEnabled: Boolean = false,     //  卡片进场动画（默认关闭）
    val cardTransitionEnabled: Boolean = false,    //  卡片过渡动画（默认关闭）
    val videoTransitionRealtimeBlurEnabled: Boolean = true,
    val smartVisualGuardEnabled: Boolean = false, // [Retired] 智能流畅优先已下线
    val cacheSize: String = "计算中...",
    val cacheBreakdown: CacheUtils.CacheBreakdown? = null,  //  详细缓存统计
    //  实验性功能
    val auto1080p: Boolean = true,
    val autoSkipOpEd: Boolean = false,
    val prefetchVideo: Boolean = false,
    val doubleTapLike: Boolean = true,

    //  空降助手
    val sponsorBlockEnabled: Boolean = false,
    val sponsorBlockAutoSkip: Boolean = true,
    // [新增] 触感反馈
    val hapticFeedbackEnabled: Boolean = true,
    val topBarLiquidGlassEnabled: Boolean = false,
    val homeSearchLiquidGlassEnabled: Boolean = false,
    val bottomBarLiquidGlassEnabled: Boolean = true,
    val bottomBarSearchEnabled: Boolean = false,
    val bottomBarSearchAutoExpandMode: BottomBarSearchAutoExpandMode =
        BottomBarSearchAutoExpandMode.EXPAND_AT_HOME_TOP,
    val androidNativeLiquidGlassEnabled: Boolean = false,
    val liquidGlassStyle: com.android.purebilibili.core.store.LiquidGlassStyle = com.android.purebilibili.core.store.LiquidGlassStyle.CLASSIC, // [New]
    val liquidGlassMode: LiquidGlassMode = LiquidGlassMode.BALANCED,
    val liquidGlassStrength: Float = 0.52f,
    val liquidGlassProgress: Float = 0.5f,
    // [New] 平板导航模式
    val tabletUseSidebar: Boolean = false,
    val isHeaderCollapseEnabled: Boolean = true,
    val gridColumnCount: Int = 0, // [New]
    val homeFeedCardWidthPreset: HomeFeedCardWidthPreset = HomeFeedCardWidthPreset.AUTO
) {
    val isLiquidGlassEnabled: Boolean
        get() = bottomBarLiquidGlassEnabled
}

// 内部数据类，用于分批合并流
private data class CoreSettings(
    val uiPreset: UiPreset,
    val androidNativeVariant: AndroidNativeVariant,
    val hwDecode: Boolean,
    val themeMode: AppThemeMode,
    val darkThemeStyle: DarkThemeStyle,
    val appLanguage: AppLanguage,
    val dynamicColor: Boolean,
    val md3ColorSource: Md3ColorSource,
    val md3CustomColorHex: String,
    val colorStyle: PaletteStyle,
    val colorSpec: ColorSpec.SpecVersion,
    val bgPlay: Boolean
)

data class ExtraSettings(
    val gestureSensitivity: Float,
    val themeColorIndex: Int,
    val appIcon: String,
    val appFontSizePreset: AppFontSizePreset,
    val appFontFileName: String,
    val appFontDisplayName: String,
    val appUiScalePreset: AppUiScalePreset,
    val appDpiOverridePercent: Int,
    val isBottomBarFloating: Boolean,
    val bottomBarLabelMode: Int,
    val headerBlurEnabled: Boolean,
    val bottomBarBlurEnabled: Boolean,
    val blurIntensity: BlurIntensity,  //  添加模糊强度
    val displayMode: Int,
    val cardAnimationEnabled: Boolean,
    val cardTransitionEnabled: Boolean,
    val videoTransitionRealtimeBlurEnabled: Boolean,
    val smartVisualGuardEnabled: Boolean,
    val hapticFeedbackEnabled: Boolean, // [Restored]
    val topBarLiquidGlassEnabled: Boolean = false,
    val homeSearchLiquidGlassEnabled: Boolean = false,
    val bottomBarLiquidGlassEnabled: Boolean = true,
    val bottomBarSearchEnabled: Boolean = false,
    val bottomBarSearchAutoExpandMode: BottomBarSearchAutoExpandMode =
        BottomBarSearchAutoExpandMode.EXPAND_AT_HOME_TOP,
    val androidNativeLiquidGlassEnabled: Boolean = false,
    val liquidGlassStyle: com.android.purebilibili.core.store.LiquidGlassStyle, // [New]
    val liquidGlassMode: LiquidGlassMode, // [New]
    val liquidGlassStrength: Float, // [New]
    val liquidGlassProgress: Float, // [New]
    val tabletUseSidebar: Boolean, // [New]
    val isHeaderCollapseEnabled: Boolean,
    val gridColumnCount: Int, // [New]
    val homeFeedCardWidthPreset: HomeFeedCardWidthPreset
)


//  实验性功能设置
data class ExperimentalSettings(
    val auto1080p: Boolean,
    val autoSkipOpEd: Boolean,
    val prefetchVideo: Boolean,
    val doubleTapLike: Boolean,
    //  空降助手
    val sponsorBlockEnabled: Boolean,
    val sponsorBlockAutoSkip: Boolean
)

private data class BaseSettings(
    val uiPreset: UiPreset,
    val androidNativeVariant: AndroidNativeVariant,
    val hwDecode: Boolean,
    val themeMode: AppThemeMode,
    val darkThemeStyle: DarkThemeStyle,
    val appLanguage: AppLanguage,
    val dynamicColor: Boolean,
    val md3ColorSource: Md3ColorSource,
    val md3CustomColorHex: String,
    val colorStyle: PaletteStyle,
    val colorSpec: ColorSpec.SpecVersion,
    val appFontSizePreset: AppFontSizePreset,
    val appFontFileName: String,
    val appFontDisplayName: String,
    val appUiScalePreset: AppUiScalePreset,
    val appDpiOverridePercent: Int,
    val bgPlay: Boolean,
    val gestureSensitivity: Float,
    val themeColorIndex: Int,
    val appIcon: String,
    val isBottomBarFloating: Boolean,
    val bottomBarLabelMode: Int,
    val headerBlurEnabled: Boolean,
    val bottomBarBlurEnabled: Boolean,
    val blurIntensity: BlurIntensity,  //  模糊强度
    val displayMode: Int, //  新增
    val cardAnimationEnabled: Boolean, //  卡片进场动画
    val cardTransitionEnabled: Boolean, //  卡片过渡动画
    val videoTransitionRealtimeBlurEnabled: Boolean,
    val smartVisualGuardEnabled: Boolean, // [New]
    val hapticFeedbackEnabled: Boolean, // [新增]
    val topBarLiquidGlassEnabled: Boolean,
    val homeSearchLiquidGlassEnabled: Boolean,
    val bottomBarLiquidGlassEnabled: Boolean,
    val bottomBarSearchEnabled: Boolean,
    val bottomBarSearchAutoExpandMode: BottomBarSearchAutoExpandMode,
    val androidNativeLiquidGlassEnabled: Boolean,
    val liquidGlassStyle: com.android.purebilibili.core.store.LiquidGlassStyle, // [New]
    val liquidGlassMode: LiquidGlassMode, // [New]
    val liquidGlassStrength: Float, // [New]
    val liquidGlassProgress: Float, // [New]
    val tabletUseSidebar: Boolean, // [New]
    val isHeaderCollapseEnabled: Boolean,
    val gridColumnCount: Int, // [New]
    val homeFeedCardWidthPreset: HomeFeedCardWidthPreset
)

private fun <T> Flow<T>.asAnyFlow(): Flow<Any?> = map { it }


class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext

    private data class UiSettingsGroup1(
        val gestureSensitivity: Float,
        val themeColorIndex: Int,
        val appIcon: String,
        val appFontSizePreset: AppFontSizePreset,
        val appFontFileName: String,
        val appFontDisplayName: String,
        val appUiScalePreset: AppUiScalePreset,
        val appDpiOverridePercent: Int
    )

    // 本地状态流：缓存大小
    private val _cacheSize = MutableStateFlow("计算中...")
    private val _cacheBreakdown = MutableStateFlow<CacheUtils.CacheBreakdown?>(null)

    //  [核心修复] 分步合并，解决 combine 参数限制报错
    // 第 1 步：合并前 4 个设置
    private val coreSettingsFlow = combine(
        SettingsManager.getUiPreset(context).asAnyFlow(),
        SettingsManager.getAndroidNativeVariant(context).asAnyFlow(),
        SettingsManager.getHwDecode(context).asAnyFlow(),
        SettingsManager.getThemeMode(context).asAnyFlow(),
        SettingsManager.getDarkThemeStyle(context).asAnyFlow(),
        SettingsManager.getAppLanguage(context).asAnyFlow(),
        SettingsManager.getDynamicColor(context).asAnyFlow(),
        SettingsManager.getMd3ColorSource(context).asAnyFlow(),
        SettingsManager.getMd3CustomColorHex(context).asAnyFlow(),
        SettingsManager.getThemeColorStyle(context).asAnyFlow(),
        SettingsManager.getThemeColorSpec(context).asAnyFlow(),
        SettingsManager.getMiniPlayerMode(context)
            .map { it != SettingsManager.MiniPlayerMode.OFF }
            .asAnyFlow()
    ) { values ->
        CoreSettings(
            uiPreset = values[0] as UiPreset,
            androidNativeVariant = values[1] as AndroidNativeVariant,
            hwDecode = values[2] as Boolean,
            themeMode = values[3] as AppThemeMode,
            darkThemeStyle = values[4] as DarkThemeStyle,
            appLanguage = values[5] as AppLanguage,
            dynamicColor = values[6] as Boolean,
            md3ColorSource = values[7] as Md3ColorSource,
            md3CustomColorHex = values[8] as String,
            colorStyle = values[9] as PaletteStyle,
            colorSpec = values[10] as ColorSpec.SpecVersion,
            bgPlay = values[11] as Boolean
        )
    }
    
    // 第 2 步：合并界面设置 (分两组，每组最多5个)
    private val uiSettingsFlow1 = combine(
        SettingsManager.getGestureSensitivity(context).asAnyFlow(),
        SettingsManager.getThemeColorIndex(context).asAnyFlow(),
        SettingsManager.getAppIcon(context).asAnyFlow(),
        SettingsManager.getAppFontSizePreset(context).asAnyFlow(),
        SettingsManager.getAppFontFileName(context).asAnyFlow(),
        SettingsManager.getAppFontDisplayName(context).asAnyFlow(),
        SettingsManager.getAppUiScalePreset(context).asAnyFlow(),
        SettingsManager.getAppDpiOverridePercent(context).asAnyFlow()
    ) { values ->
        UiSettingsGroup1(
            gestureSensitivity = values[0] as Float,
            themeColorIndex = values[1] as Int,
            appIcon = values[2] as String,
            appFontSizePreset = values[3] as AppFontSizePreset,
            appFontFileName = values[4] as String,
            appFontDisplayName = values[5] as String,
            appUiScalePreset = values[6] as AppUiScalePreset,
            appDpiOverridePercent = values[7] as Int
        )
    }
    
    private val uiSettingsFlow2 = combine(
        SettingsManager.getBottomBarFloating(context).asAnyFlow(),
        SettingsManager.getBottomBarLabelMode(context).asAnyFlow(),
        SettingsManager.getDisplayMode(context).asAnyFlow(),
        SettingsManager.getCardAnimationEnabled(context).asAnyFlow(), // [Restored]
        SettingsManager.getCardTransitionEnabled(context).asAnyFlow(),
        SettingsManager.getVideoTransitionRealtimeBlurEnabled(context).asAnyFlow(),
        SettingsManager.getSmartVisualGuardEnabled(context).asAnyFlow(), // [New]
        SettingsManager.getHapticFeedbackEnabled(context).asAnyFlow(), // [新增]
        SettingsManager.getTopBarLiquidGlassEnabled(context).asAnyFlow(),
        SettingsManager.getHomeSearchLiquidGlassEnabled(context).asAnyFlow(),
        SettingsManager.getBottomBarLiquidGlassEnabled(context).asAnyFlow(),
        SettingsManager.getBottomBarSearchEnabled(context).asAnyFlow(),
        SettingsManager.getBottomBarSearchAutoExpandMode(context).asAnyFlow(),
        SettingsManager.getAndroidNativeLiquidGlassEnabled(context).asAnyFlow(),
        SettingsManager.getLiquidGlassStyle(context).asAnyFlow(), // [New]
        SettingsManager.getLiquidGlassMode(context).asAnyFlow(), // [New]
        SettingsManager.getLiquidGlassStrength(context).asAnyFlow(), // [New]
        SettingsManager.getLiquidGlassProgress(context).asAnyFlow(), // [New]
        SettingsManager.getTabletUseSidebar(context).asAnyFlow(), // [New]
        SettingsManager.getHeaderCollapseEnabled(context).asAnyFlow(),
        SettingsManager.getGridColumnCount(context).asAnyFlow(), // [New]
        SettingsManager.getHomeFeedCardWidthPreset(context).asAnyFlow()
    ) { values ->
        val isBottomBarFloating = values[0] as Boolean
        val labelMode = values[1] as Int
        val displayMode = values[2] as Int
        val cardAnimation = values[3] as Boolean
        val cardTransition = values[4] as Boolean
        val videoTransitionRealtimeBlur = values[5] as Boolean
        val smartVisualGuard = values[6] as Boolean
        val hapticFeedback = values[7] as Boolean
        val topBarLiquidGlass = values[8] as Boolean
        val homeSearchLiquidGlass = values[9] as Boolean
        val bottomBarLiquidGlass = values[10] as Boolean
        val bottomBarSearch = values[11] as Boolean
        val bottomBarSearchAutoExpandMode = values[12] as BottomBarSearchAutoExpandMode
        val androidNativeLiquidGlass = values[13] as Boolean
        val liquidGlassStyle = values[14] as com.android.purebilibili.core.store.LiquidGlassStyle
        val liquidGlassMode = values[15] as LiquidGlassMode
        val liquidGlassStrength = values[16] as Float
        val liquidGlassProgress = values[17] as Float
        val tabletUseSidebar = values[18] as Boolean
        val headerCollapse = values[19] as Boolean
        val gridColumnCount = values[20] as Int
        val homeFeedCardWidthPreset = values[21] as HomeFeedCardWidthPreset
        
        data class Ui2(
            val f: Boolean,
            val l: Int,
            val d: Int,
            val ca: Boolean,
            val ct: Boolean,
            val vtrb: Boolean,
            val svg: Boolean,
            val h: Boolean,
            val tlg: Boolean,
            val hslg: Boolean,
            val blg: Boolean,
            val bbs: Boolean,
            val bbsam: BottomBarSearchAutoExpandMode,
            val anlg: Boolean,
            val lgs: com.android.purebilibili.core.store.LiquidGlassStyle,
            val lgm: LiquidGlassMode,
            val lgt: Float,
            val lgp: Float,
            val tus: Boolean,
            val hc: Boolean,
            val gcc: Int,
            val hfcwp: HomeFeedCardWidthPreset
        )
        Ui2(
            isBottomBarFloating,
            labelMode,
            displayMode,
            cardAnimation,
            cardTransition,
            videoTransitionRealtimeBlur,
            smartVisualGuard,
            hapticFeedback,
            topBarLiquidGlass,
            homeSearchLiquidGlass,
            bottomBarLiquidGlass,
            bottomBarSearch,
            bottomBarSearchAutoExpandMode,
            androidNativeLiquidGlass,
            liquidGlassStyle,
            liquidGlassMode,
            liquidGlassStrength,
            liquidGlassProgress,
            tabletUseSidebar,
            headerCollapse,
            gridColumnCount,
            homeFeedCardWidthPreset
        )
    }

    // 合并所有 UI 设置
    private val uiSettingsFlow = combine(uiSettingsFlow1, uiSettingsFlow2) { ui1, ui2 ->
        // ui2: Ui2 class
        ExtraSettings(
            gestureSensitivity = ui1.gestureSensitivity,
            themeColorIndex = ui1.themeColorIndex,
            appIcon = ui1.appIcon,
            appFontSizePreset = ui1.appFontSizePreset,
            appFontFileName = ui1.appFontFileName,
            appFontDisplayName = ui1.appFontDisplayName,
            appUiScalePreset = ui1.appUiScalePreset,
            appDpiOverridePercent = ui1.appDpiOverridePercent,
            isBottomBarFloating = ui2.f,
            bottomBarLabelMode = ui2.l,
            displayMode = ui2.d,
            cardAnimationEnabled = ui2.ca,
            cardTransitionEnabled = ui2.ct,
            videoTransitionRealtimeBlurEnabled = ui2.vtrb,
            smartVisualGuardEnabled = ui2.svg,
            hapticFeedbackEnabled = ui2.h, // [新增]
            topBarLiquidGlassEnabled = ui2.tlg,
            homeSearchLiquidGlassEnabled = ui2.hslg,
            bottomBarLiquidGlassEnabled = ui2.blg,
            bottomBarSearchEnabled = ui2.bbs,
            bottomBarSearchAutoExpandMode = ui2.bbsam,
            androidNativeLiquidGlassEnabled = ui2.anlg,
            liquidGlassStyle = ui2.lgs, // [New]
            liquidGlassMode = ui2.lgm, // [New]
            liquidGlassStrength = ui2.lgt, // [New]
            liquidGlassProgress = ui2.lgp, // [New]
            tabletUseSidebar = ui2.tus, // [New]
            isHeaderCollapseEnabled = ui2.hc,
            gridColumnCount = ui2.gcc, // [New]
            homeFeedCardWidthPreset = ui2.hfcwp,
            headerBlurEnabled = false, // 暂存，将在下一步合并
            bottomBarBlurEnabled = false, // 暂存
            blurIntensity = BlurIntensity.THIN // 暂存
        )
    }
    
    // 第 3 步：合并模糊设置 (3个)
    private val blurSettingsFlow = combine(
        SettingsManager.getHeaderBlurEnabled(context),
        SettingsManager.getBottomBarBlurEnabled(context),
        SettingsManager.getBlurIntensity(context)  //  添加模糊强度
    ) { headerBlur, bottomBarBlur, blurIntensity ->
        Triple(headerBlur, bottomBarBlur, blurIntensity)
    }
    
    // 第 4 步：合并 UI 和 模糊设置
    private val extraSettingsFlow = combine(uiSettingsFlow, blurSettingsFlow) { uiSettings, blur ->
        uiSettings.copy(
            headerBlurEnabled = blur.first,
            bottomBarBlurEnabled = blur.second,
            blurIntensity = blur.third
        )
    }
    
    //  第 4.5 步：合并实验性功能设置
    private val experimentalSettingsFlow = combine(
        SettingsManager.getAuto1080p(context),
        SettingsManager.getAutoSkipOpEd(context),
        SettingsManager.getPrefetchVideo(context),
        SettingsManager.getDoubleTapLike(context),
        SettingsManager.getSponsorBlockEnabled(context),
        SettingsManager.getSponsorBlockAutoSkip(context)
    ) { values ->
        ExperimentalSettings(
            auto1080p = values[0],
            autoSkipOpEd = values[1],
            prefetchVideo = values[2],
            doubleTapLike = values[3],
            sponsorBlockEnabled = values[4],
            sponsorBlockAutoSkip = values[5]
        )
    }
    
    // 第 5 步：合并两组设置
    private val baseSettingsFlow = combine(coreSettingsFlow, extraSettingsFlow) { core, extra ->
        BaseSettings(
            uiPreset = core.uiPreset,
            androidNativeVariant = core.androidNativeVariant,
            hwDecode = core.hwDecode,
            themeMode = core.themeMode,
            darkThemeStyle = core.darkThemeStyle,
            appLanguage = core.appLanguage,
            dynamicColor = core.dynamicColor,
            md3ColorSource = core.md3ColorSource,
            md3CustomColorHex = core.md3CustomColorHex,
            colorStyle = core.colorStyle,
            colorSpec = core.colorSpec,
            appFontSizePreset = extra.appFontSizePreset,
            appFontFileName = extra.appFontFileName,
            appFontDisplayName = extra.appFontDisplayName,
            appUiScalePreset = extra.appUiScalePreset,
            appDpiOverridePercent = extra.appDpiOverridePercent,
            bgPlay = core.bgPlay,
            gestureSensitivity = extra.gestureSensitivity,
            themeColorIndex = extra.themeColorIndex,
            appIcon = extra.appIcon,
            isBottomBarFloating = extra.isBottomBarFloating,
            bottomBarLabelMode = extra.bottomBarLabelMode,
            headerBlurEnabled = extra.headerBlurEnabled,
            bottomBarBlurEnabled = extra.bottomBarBlurEnabled,
            blurIntensity = extra.blurIntensity,  //  模糊强度
            displayMode = extra.displayMode,
            cardAnimationEnabled = extra.cardAnimationEnabled,
            cardTransitionEnabled = extra.cardTransitionEnabled,
            videoTransitionRealtimeBlurEnabled = extra.videoTransitionRealtimeBlurEnabled,
            smartVisualGuardEnabled = extra.smartVisualGuardEnabled,
            hapticFeedbackEnabled = extra.hapticFeedbackEnabled, // [新增]
            topBarLiquidGlassEnabled = extra.topBarLiquidGlassEnabled,
            homeSearchLiquidGlassEnabled = extra.homeSearchLiquidGlassEnabled,
            bottomBarLiquidGlassEnabled = extra.bottomBarLiquidGlassEnabled,
            bottomBarSearchEnabled = extra.bottomBarSearchEnabled,
            bottomBarSearchAutoExpandMode = extra.bottomBarSearchAutoExpandMode,
            androidNativeLiquidGlassEnabled = extra.androidNativeLiquidGlassEnabled,
            liquidGlassStyle = extra.liquidGlassStyle, // [New]
            liquidGlassMode = extra.liquidGlassMode, // [New]
            liquidGlassStrength = extra.liquidGlassStrength, // [New]
            liquidGlassProgress = extra.liquidGlassProgress, // [New]
            tabletUseSidebar = extra.tabletUseSidebar, // [New]
            isHeaderCollapseEnabled = extra.isHeaderCollapseEnabled,
            gridColumnCount = extra.gridColumnCount, // [New]
            homeFeedCardWidthPreset = extra.homeFeedCardWidthPreset
        )

    }

    // 第 6 步：与缓存大小和实验性功能合并
    private val cacheFlow = combine(_cacheSize, _cacheBreakdown) { size, breakdown ->
        Pair(size, breakdown)
    }
    
    val state: StateFlow<SettingsUiState> = combine(
        baseSettingsFlow,
        cacheFlow,
        experimentalSettingsFlow
    ) { settings, cache, experimental ->
        SettingsUiState(
            uiPreset = settings.uiPreset,
            androidNativeVariant = settings.androidNativeVariant,
            hwDecode = settings.hwDecode,
            themeMode = settings.themeMode,
            darkThemeStyle = settings.darkThemeStyle,
            appLanguage = settings.appLanguage,
            dynamicColor = settings.dynamicColor,
            md3ColorSource = settings.md3ColorSource,
            md3CustomColorHex = settings.md3CustomColorHex,
            colorStyle = settings.colorStyle,
            colorSpec = settings.colorSpec,
            appFontSizePreset = settings.appFontSizePreset,
            appFontFileName = settings.appFontFileName,
            appFontDisplayName = settings.appFontDisplayName,
            appUiScalePreset = settings.appUiScalePreset,
            appDpiOverridePercent = settings.appDpiOverridePercent,
            bgPlay = settings.bgPlay,
            gestureSensitivity = settings.gestureSensitivity,
            themeColorIndex = settings.themeColorIndex,
            appIcon = settings.appIcon,
            isBottomBarFloating = settings.isBottomBarFloating,
            bottomBarLabelMode = settings.bottomBarLabelMode,
            headerBlurEnabled = settings.headerBlurEnabled,
            bottomBarBlurEnabled = settings.bottomBarBlurEnabled,
            blurIntensity = settings.blurIntensity,  //  模糊强度
            displayMode = settings.displayMode,
            cardAnimationEnabled = settings.cardAnimationEnabled,
            cardTransitionEnabled = settings.cardTransitionEnabled,
            videoTransitionRealtimeBlurEnabled = settings.videoTransitionRealtimeBlurEnabled,
            smartVisualGuardEnabled = settings.smartVisualGuardEnabled,
            hapticFeedbackEnabled = settings.hapticFeedbackEnabled, // [新增]
            topBarLiquidGlassEnabled = settings.topBarLiquidGlassEnabled,
            homeSearchLiquidGlassEnabled = settings.homeSearchLiquidGlassEnabled,
            bottomBarLiquidGlassEnabled = settings.bottomBarLiquidGlassEnabled,
            bottomBarSearchEnabled = settings.bottomBarSearchEnabled,
            bottomBarSearchAutoExpandMode = settings.bottomBarSearchAutoExpandMode,
            androidNativeLiquidGlassEnabled = settings.androidNativeLiquidGlassEnabled,
            liquidGlassStyle = settings.liquidGlassStyle, // [New]
            liquidGlassMode = settings.liquidGlassMode, // [New]
            liquidGlassStrength = settings.liquidGlassStrength, // [New]
            liquidGlassProgress = settings.liquidGlassProgress, // [New]
            tabletUseSidebar = settings.tabletUseSidebar, // [New]
            isHeaderCollapseEnabled = settings.isHeaderCollapseEnabled,
            gridColumnCount = settings.gridColumnCount, // [New]
            homeFeedCardWidthPreset = settings.homeFeedCardWidthPreset,

            cacheSize = cache.first,
            cacheBreakdown = cache.second,  //  详细缓存统计
            //  实验性功能
            auto1080p = experimental.auto1080p,
            autoSkipOpEd = experimental.autoSkipOpEd,
            prefetchVideo = experimental.prefetchVideo,
            doubleTapLike = experimental.doubleTapLike,
            //  空降助手
            sponsorBlockEnabled = experimental.sponsorBlockEnabled,
            sponsorBlockAutoSkip = experimental.sponsorBlockAutoSkip
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    init {
        refreshCacheSize()
    }

    // --- 功能方法 ---

    //  优化：同时获取缓存大小和详细统计
    fun refreshCacheSize() {
        viewModelScope.launch { 
            val breakdown = CacheUtils.getCacheBreakdown(context)
            _cacheSize.value = breakdown.format()
            _cacheBreakdown.value = breakdown
        }
    }

    suspend fun clearCache(
        targets: Set<CacheClearTarget> = CacheClearTarget.entries.toSet()
    ): Result<CacheUtils.CacheBreakdown> {
        return CacheUtils.clearCache(context, targets).mapCatching {
            // 清理后立即刷新
            val breakdown = CacheUtils.getCacheBreakdown(context)
            _cacheSize.value = breakdown.format()
            _cacheBreakdown.value = breakdown
            breakdown
        }
    }

    fun toggleHwDecode(value: Boolean) { viewModelScope.launch { SettingsManager.setHwDecode(context, value) } }
    fun setUiPreset(preset: UiPreset) {
        viewModelScope.launch {
            SettingsManager.setUiPreset(context, preset)
        }
    }
    fun setAndroidNativeVariant(variant: AndroidNativeVariant) {
        viewModelScope.launch {
            SettingsManager.setAndroidNativeVariant(context, variant)
        }
    }
    fun setThemeMode(mode: AppThemeMode) { 
        viewModelScope.launch { 
            SettingsManager.setThemeMode(context, mode)
        } 
    }
    fun setDarkThemeStyle(style: DarkThemeStyle) {
        viewModelScope.launch {
            SettingsManager.setDarkThemeStyle(context, style)
        }
    }
    fun setAppLanguage(appLanguage: AppLanguage) {
        viewModelScope.launch {
            SettingsManager.setAppLanguage(context, appLanguage)
        }
    }
    fun toggleDynamicColor(value: Boolean) { viewModelScope.launch { SettingsManager.setDynamicColor(context, value) } }
    fun setMd3ColorSource(source: Md3ColorSource) {
        viewModelScope.launch { SettingsManager.setMd3ColorSource(context, source) }
    }
    fun setMd3CustomColorHex(hex: String) {
        viewModelScope.launch { SettingsManager.setMd3CustomColorHex(context, hex) }
    }
    fun setThemeColorStyle(style: PaletteStyle) {
        viewModelScope.launch { SettingsManager.setThemeColorStyle(context, style) }
    }
    fun setThemeColorSpec(spec: ColorSpec.SpecVersion) {
        viewModelScope.launch { SettingsManager.setThemeColorSpec(context, spec) }
    }
    fun setAppFontSizePreset(preset: AppFontSizePreset) {
        viewModelScope.launch { SettingsManager.setAppFontSizePreset(context, preset) }
    }
    fun setAppFontFile(fileName: String, displayName: String) {
        viewModelScope.launch { SettingsManager.setAppFontFile(context, fileName, displayName) }
    }
    fun clearAppFontFile() {
        viewModelScope.launch { SettingsManager.clearAppFontFile(context) }
    }
    fun setAppUiScalePreset(preset: AppUiScalePreset) {
        viewModelScope.launch { SettingsManager.setAppUiScalePreset(context, preset) }
    }
    fun setAppDpiOverridePercent(percent: Int) {
        viewModelScope.launch { SettingsManager.setAppDpiOverridePercent(context, percent) }
    }
    fun toggleBgPlay(value: Boolean) {
        viewModelScope.launch {
            SettingsManager.setMiniPlayerMode(
                context,
                if (value) SettingsManager.MiniPlayerMode.SYSTEM_PIP else SettingsManager.MiniPlayerMode.OFF
            )
        }
    }
    //  [新增] 手势灵敏度和主题色
    fun setGestureSensitivity(value: Float) { viewModelScope.launch { SettingsManager.setGestureSensitivity(context, value) } }
    fun setThemeColorIndex(index: Int) { 
        viewModelScope.launch { 
            SettingsManager.setThemeColorIndex(context, index)
        }
    }

    //  [新增] 切换应用图标
    fun setAppIcon(iconKey: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val normalizedIconKey = normalizeAppIconKey(iconKey)
            // 1. 保存偏好
            SettingsManager.setAppIcon(context, normalizedIconKey)
            applyLauncherAliasForCurrentSplashIconSetting(normalizedIconKey)
        }
    }

    private suspend fun applyLauncherAliasForCurrentSplashIconSetting(iconKey: String) {
        val normalizedIconKey = normalizeAppIconKey(iconKey)
        // 2. 应用 Alias
        val pm = context.packageManager
        val packageName = context.packageName
        val splashIconVisible = SettingsManager.isSplashIconAnimationEnabledSync(context)

        val targetAlias = resolveAppIconLauncherAlias(
            packageName = packageName,
            rawKey = normalizedIconKey,
            splashIconVisible = splashIconVisible
        )
        val allUniqueAliases = allManagedAppIconLauncherAliases(packageName)

        android.util.Log.d("SettingsViewModel", "Switching icon to: $normalizedIconKey, splashIconVisible=$splashIconVisible -> $targetAlias")

        try {
            // 第一步：先启用目标 alias（确保始终有一个活动入口点）
            // ⚠️ [修复] 在尝试杀死进程的操作前，再次延迟，确保 DataStore/SharedPrefs 完全写入磁盘
            kotlinx.coroutines.delay(100)

            pm.setComponentEnabledSetting(
                android.content.ComponentName(packageName, targetAlias),
                android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                android.content.pm.PackageManager.DONT_KILL_APP
            )
            android.util.Log.d("SettingsViewModel", "Enabled alias: $targetAlias")

            // 第二步：立即禁用其他 alias，避免部分桌面出现“双图标”残留
            allUniqueAliases
                .filter { it != targetAlias }
                .forEach { aliasFullName ->
                    try {
                        pm.setComponentEnabledSetting(
                            android.content.ComponentName(packageName, aliasFullName),
                            android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                            android.content.pm.PackageManager.DONT_KILL_APP
                        )
                    } catch (e: Exception) {
                        android.util.Log.w("SettingsViewModel", "Failed to disable alias: $aliasFullName", e)
                    }
                }
            android.util.Log.d("SettingsViewModel", "Icon switch completed: $normalizedIconKey")
        } catch (e: Exception) {
            android.util.Log.e("SettingsViewModel", "Failed to switch app icon to $normalizedIconKey", e)
        }
    }

    //  [新增] 切换底栏样式
    fun toggleBottomBarFloating(value: Boolean) { viewModelScope.launch { SettingsManager.setBottomBarFloating(context, value) } }
    
    //  [新增] 底栏显示模式 (0=图标+文字, 1=仅图标, 2=仅文字)
    fun setBottomBarLabelMode(mode: Int) { viewModelScope.launch { SettingsManager.setBottomBarLabelMode(context, mode) } }
    


    fun toggleHeaderBlur(value: Boolean) {
        viewModelScope.launch {
            val resolved = resolveTopBarBlurToggleState(
                enableHeaderBlur = value
            )
            SettingsManager.setHeaderBlurEnabled(context, resolved.headerBlurEnabled)
        }
    }
    fun toggleHeaderCollapse(value: Boolean) { viewModelScope.launch { SettingsManager.setHeaderCollapseEnabled(context, value) } }
    fun toggleBottomBarBlur(value: Boolean) {
        viewModelScope.launch {
            val resolved = resolveBottomBarBlurToggleState(
                enableBottomBarBlur = value,
                currentLiquidGlassEnabled = state.value.bottomBarLiquidGlassEnabled
            )
            SettingsManager.setBottomBarBlurEnabled(context, resolved.bottomBarBlurEnabled)
            SettingsManager.setBottomBarLiquidGlassEnabled(context, resolved.liquidGlassEnabled)
        }
    }
    fun setBlurIntensity(intensity: BlurIntensity) { viewModelScope.launch { SettingsManager.setBlurIntensity(context, intensity) } }  //  模糊强度设置
    
    //  [新增] 卡片进场动画开关
    fun toggleCardAnimation(value: Boolean) { viewModelScope.launch { SettingsManager.setCardAnimationEnabled(context, value) } }
    
    //  [新增] 卡片过渡动画开关
    fun toggleCardTransition(value: Boolean) { viewModelScope.launch { SettingsManager.setCardTransitionEnabled(context, value) } }

    fun toggleVideoTransitionRealtimeBlur(value: Boolean) {
        viewModelScope.launch {
            SettingsManager.setVideoTransitionRealtimeBlurEnabled(context, value)
        }
    }

    fun toggleSmartVisualGuard(value: Boolean) {
        viewModelScope.launch {
            SettingsManager.setSmartVisualGuardEnabled(context, value)
        }
    }
    
    //  [新增] 首页展示模式
    fun setDisplayMode(mode: Int) { 
        viewModelScope.launch { 
            // 兼容旧的 shared preferences
            context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                .edit().putInt("display_mode", mode).apply()
            // 触发 flow 更新 (如果需要，或者仅仅依赖 prefs 监听? 这里简化处理，假设 ViewModel 只负责写，读在 flow 中)
            // 实际上这里的 flow 是基于 SettingsManager (DataStore) 的。
            // 如果 display_mode 还是 SharedPreferences，我们需要一个 flow 来通过 DataStore 或者手动构建。
            //为了简单统一，建议迁移到 SettingsManager。但为了不破坏 HomeScreen 读取，我们先保持 Prefs，
            // 并在 SettingsManager 中增加对 display_mode 的支持 (或者直接在这里用 MutableStateFlow 桥接?)
            // 鉴于 HomeScreen 可能直接读 Prefs，我们这里只需写 Prefs。
            // 但为了 UI 响应，我们需要通知 UIState。
            // 由于 SettingsManager 目前不管理 display_mode，我们需要添加它。
            // 既然要 refactor，就彻底点。
            SettingsManager.setDisplayMode(context, mode)
        } 
    }
    
    //  [新增] 实验性功能
    fun toggleAuto1080p(value: Boolean) { viewModelScope.launch { SettingsManager.setAuto1080p(context, value) } }
    fun toggleAutoSkipOpEd(value: Boolean) { viewModelScope.launch { SettingsManager.setAutoSkipOpEd(context, value) } }
    fun togglePrefetchVideo(value: Boolean) { viewModelScope.launch { SettingsManager.setPrefetchVideo(context, value) } }
    fun toggleDoubleTapLike(value: Boolean) { viewModelScope.launch { SettingsManager.setDoubleTapLike(context, value) } }
    
    //  [新增] 空降助手
    fun toggleSponsorBlock(value: Boolean) { viewModelScope.launch { SettingsManager.setSponsorBlockEnabled(context, value) } }
    fun toggleSponsorBlockAutoSkip(value: Boolean) { viewModelScope.launch { SettingsManager.setSponsorBlockAutoSkip(context, value) } }
    
    // [New] Splash Screen
    fun toggleSplashEnabled(value: Boolean) { viewModelScope.launch { SettingsManager.setSplashEnabled(context, value) } }
    fun toggleSplashRandomEnabled(value: Boolean) { viewModelScope.launch { SettingsManager.setSplashRandomEnabled(context, value) } }
    fun toggleSplashIconAnimationEnabled(value: Boolean) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            SettingsManager.setSplashIconAnimationEnabled(context, value)
            val currentIcon = SettingsManager.getAppIconSync(context)
            applyLauncherAliasForCurrentSplashIconSetting(currentIcon)
        }
    }

    // [New] 触感反馈
    fun toggleHapticFeedback(value: Boolean) { viewModelScope.launch { SettingsManager.setHapticFeedbackEnabled(context, value) } }
    
    // [New] Liquid Glass
    fun toggleTopBarLiquidGlass(enabled: Boolean) {
        viewModelScope.launch {
            SettingsManager.setTopBarLiquidGlassEnabled(context, enabled)
        }
    }

    fun toggleHomeSearchLiquidGlass(enabled: Boolean) {
        viewModelScope.launch {
            SettingsManager.setHomeSearchLiquidGlassEnabled(context, enabled)
        }
    }

    fun toggleBottomBarLiquidGlass(enabled: Boolean) {
        viewModelScope.launch {
            val resolved = resolveLiquidGlassToggleState(
                enableLiquidGlass = enabled,
                currentBottomBarBlurEnabled = state.value.bottomBarBlurEnabled
            )
            SettingsManager.setBottomBarLiquidGlassEnabled(context, resolved.liquidGlassEnabled)
            SettingsManager.setBottomBarBlurEnabled(context, resolved.bottomBarBlurEnabled)
        }
    }

    fun toggleBottomBarSearch(enabled: Boolean) {
        viewModelScope.launch {
            SettingsManager.setBottomBarSearchEnabled(context, enabled)
        }
    }

    fun setBottomBarSearchAutoExpandMode(mode: BottomBarSearchAutoExpandMode) {
        viewModelScope.launch {
            SettingsManager.setBottomBarSearchAutoExpandMode(context, mode)
        }
    }

    fun toggleAndroidNativeLiquidGlass(enabled: Boolean) {
        viewModelScope.launch {
            SettingsManager.setAndroidNativeLiquidGlassEnabled(context, enabled)
            if (enabled) {
                val bottomBarResolved = resolveLiquidGlassToggleState(
                    enableLiquidGlass = true,
                    currentBottomBarBlurEnabled = state.value.bottomBarBlurEnabled
                )
                SettingsManager.setBottomBarLiquidGlassEnabled(context, bottomBarResolved.liquidGlassEnabled)
                SettingsManager.setBottomBarBlurEnabled(context, bottomBarResolved.bottomBarBlurEnabled)
            }
        }
    }

    fun toggleLiquidGlass(enabled: Boolean) {
        viewModelScope.launch {
            SettingsManager.setLiquidGlassEnabled(context, enabled)
        }
    }
    
    fun setLiquidGlassStyle(style: com.android.purebilibili.core.store.LiquidGlassStyle) {
        viewModelScope.launch {
            SettingsManager.setLiquidGlassStyle(context, style)
            val mode = resolveLegacyLiquidGlassMode(style)
            val strength = resolveDefaultLiquidGlassStrength(mode)
            SettingsManager.setLiquidGlassMode(context, mode)
            SettingsManager.setLiquidGlassStrength(context, strength)
            SettingsManager.setLiquidGlassProgress(
                context,
                resolveLegacyLiquidGlassProgress(mode = mode, strength = strength)
            )
        }
    }

    fun setLiquidGlassMode(mode: LiquidGlassMode) {
        viewModelScope.launch {
            SettingsManager.setLiquidGlassMode(context, mode)
        }
    }

    fun setLiquidGlassStrength(strength: Float) {
        viewModelScope.launch {
            SettingsManager.setLiquidGlassStrength(context, strength)
        }
    }

    fun setLiquidGlassProgress(progress: Float) {
        viewModelScope.launch {
            SettingsManager.setLiquidGlassProgress(context, progress)
        }
    }

    // [New] 平板导航模式
    fun toggleTabletUseSidebar(value: Boolean) {
        viewModelScope.launch {
            SettingsManager.setTabletUseSidebar(context, value)
        }
    }

    // [New] 网格列数
    fun setGridColumnCount(count: Int) {
        viewModelScope.launch {
            SettingsManager.setGridColumnCount(context, count)
        }
    }

    fun setHomeFeedCardWidthPreset(preset: HomeFeedCardWidthPreset) {
        viewModelScope.launch {
            SettingsManager.setHomeFeedCardWidthPreset(context, preset)
        }
    }
    

}

internal class SettingsViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            return SettingsViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

// Move DisplayMode enum here to be accessible
enum class DisplayMode(val title: String, val description: String, val value: Int) {
    DoubleGrid(title = "双列网格", description = "经典双列瀑布流布局", value = 0),
    SingleColumn(title = "单列视频", description = "类似信息流的单列布局", value = 1)
}
