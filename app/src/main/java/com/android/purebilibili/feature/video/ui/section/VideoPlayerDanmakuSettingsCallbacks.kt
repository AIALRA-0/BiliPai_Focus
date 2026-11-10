package com.android.purebilibili.feature.video.ui.section

import android.content.Context
import com.android.purebilibili.core.store.DanmakuSettingsScope
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.data.repository.DanmakuCloudSyncSettings
import com.android.purebilibili.feature.video.danmaku.DanmakuManager
import com.android.purebilibili.feature.video.ui.overlay.VideoPlayerOverlayActions

// 只组装弹幕设置事件，保留其他控制回调。保存工作由播放器宿主的 UI 作用域启动，
// 即时渲染更新、持久化请求与云同步排队沿用原有顺序；当前显示范围由宿主传入。
internal fun VideoPlayerOverlayActions.withPlayerDanmakuSettingsCallbacks(
    context: Context,
    danmakuManager: DanmakuManager,
    danmakuEnabled: Boolean,
    activeDanmakuScope: DanmakuSettingsScope,
    cloudSyncSettings: DanmakuCloudSyncSettings,
    launchSettingsWrite: (suspend () -> Unit) -> Unit,
    onQueueCloudSync: (DanmakuCloudSyncSettings) -> Unit,
    onCloudSyncDisabled: () -> Unit,
    onSyncNow: () -> Unit,
): VideoPlayerOverlayActions = copy(
    onDanmakuToggle = {
        val newState = !danmakuEnabled
        danmakuManager.isEnabled = newState
        if (!newState) {
            danmakuManager.clear()
        }
        launchSettingsWrite {
            SettingsManager.setDanmakuEnabled(
                context,
                newState,
                activeDanmakuScope
            )
        }
        onQueueCloudSync(cloudSyncSettings.copy(enabled = newState))
        //  记录弹幕开关事件
        com.android.purebilibili.core.util.AnalyticsHelper.logDanmakuToggle(newState)
    },
    onDanmakuSliderPreview = { parameter, value ->
        com.android.purebilibili.feature.video.ui.components.applyDanmakuSliderPreview(danmakuManager, parameter, value)
    },
    onDanmakuOpacityChange = { value ->
        danmakuManager.opacity = value
        launchSettingsWrite {
            SettingsManager.setDanmakuOpacity(
                context,
                value,
                activeDanmakuScope
            )
        }
        onQueueCloudSync(cloudSyncSettings.copy(opacity = value))
    },
    onDanmakuFontScaleChange = { value ->
        danmakuManager.fontScale = value
        launchSettingsWrite {
            SettingsManager.setDanmakuFontScale(
                context,
                value,
                activeDanmakuScope
            )
        }
        onQueueCloudSync(cloudSyncSettings.copy(fontScale = value))
    },
    onDanmakuFontWeightChange = { value ->
        danmakuManager.fontWeight = value
        launchSettingsWrite {
            SettingsManager.setDanmakuFontWeight(
                context,
                value,
                activeDanmakuScope
            )
        }
    },
    onDanmakuSpeedChange = { value ->
        danmakuManager.speedFactor = value
        launchSettingsWrite {
            SettingsManager.setDanmakuSpeed(
                context,
                value,
                activeDanmakuScope
            )
        }
        onQueueCloudSync(cloudSyncSettings.copy(speed = value))
    },
    onDanmakuDisplayAreaChange = { value ->
        danmakuManager.displayArea = value
        launchSettingsWrite {
            SettingsManager.setDanmakuArea(
                context,
                value,
                activeDanmakuScope
            )
        }
        onQueueCloudSync(cloudSyncSettings.copy(displayAreaRatio = value))
    },
    onDanmakuStrokeWidthChange = { value ->
        danmakuManager.strokeWidth = value
        launchSettingsWrite {
            SettingsManager.setDanmakuStrokeWidth(
                context,
                value,
                activeDanmakuScope
            )
        }
    },
    onDanmakuLineHeightChange = { value ->
        danmakuManager.lineHeight = value
        launchSettingsWrite {
            SettingsManager.setDanmakuLineHeight(
                context,
                value,
                activeDanmakuScope
            )
        }
    },
    onDanmakuScrollDurationSecondsChange = { value ->
        danmakuManager.scrollDurationSeconds = value
        launchSettingsWrite {
            SettingsManager.setDanmakuScrollDurationSeconds(
                context,
                value,
                activeDanmakuScope
            )
        }
    },
    onDanmakuStaticDurationSecondsChange = { value ->
        danmakuManager.staticDurationSeconds = value
        launchSettingsWrite {
            SettingsManager.setDanmakuStaticDurationSeconds(
                context,
                value,
                activeDanmakuScope
            )
        }
    },
    onDanmakuScrollFixedVelocityChange = { value ->
        danmakuManager.scrollFixedVelocity = value
        launchSettingsWrite {
            SettingsManager.setDanmakuScrollFixedVelocity(
                context,
                value,
                activeDanmakuScope
            )
        }
    },
    onDanmakuStaticToScrollChange = { value ->
        danmakuManager.staticDanmakuToScroll = value
        launchSettingsWrite {
            SettingsManager.setDanmakuStaticToScroll(
                context,
                value,
                activeDanmakuScope
            )
        }
    },
    onDanmakuMassiveModeChange = { value ->
        danmakuManager.massiveMode = value
        launchSettingsWrite {
            SettingsManager.setDanmakuMassiveMode(
                context,
                value,
                activeDanmakuScope
            )
        }
    },
    onDanmakuMergeDuplicatesChange = { value ->
        launchSettingsWrite {
            SettingsManager.setDanmakuMergeDuplicates(
                context,
                value,
                activeDanmakuScope
            )
        }
    },
    onDanmakuDuplicateMergeWindowMsChange = { value ->
        launchSettingsWrite {
            SettingsManager.setDanmakuDuplicateMergeWindowMs(
                context,
                value,
                activeDanmakuScope
            )
        }
    },
    onDanmakuDuplicateMergeCountThresholdChange = { value ->
        launchSettingsWrite {
            SettingsManager.setDanmakuDuplicateMergeCountThreshold(
                context,
                value,
                activeDanmakuScope
            )
        }
    },
    onDanmakuWeightFilterLevelChange = { value ->
        launchSettingsWrite {
            SettingsManager
                .setDanmakuWeightFilterLevel(context, value)
        }
    },
    onDanmakuAllowScrollChange = { value ->
        launchSettingsWrite {
            SettingsManager.setDanmakuAllowScroll(
                context,
                value,
                activeDanmakuScope
            )
        }
        onQueueCloudSync(cloudSyncSettings.copy(allowScroll = value))
    },
    onDanmakuAllowTopChange = { value ->
        launchSettingsWrite {
            SettingsManager.setDanmakuAllowTop(
                context,
                value,
                activeDanmakuScope
            )
        }
        onQueueCloudSync(cloudSyncSettings.copy(allowTop = value))
    },
    onDanmakuAllowBottomChange = { value ->
        launchSettingsWrite {
            SettingsManager.setDanmakuAllowBottom(
                context,
                value,
                activeDanmakuScope
            )
        }
        onQueueCloudSync(cloudSyncSettings.copy(allowBottom = value))
    },
    onDanmakuAllowColorfulChange = { value ->
        launchSettingsWrite {
            SettingsManager.setDanmakuAllowColorful(
                context,
                value,
                activeDanmakuScope
            )
        }
        onQueueCloudSync(cloudSyncSettings.copy(allowColorful = value))
    },
    onDanmakuAllowSpecialChange = { value ->
        launchSettingsWrite {
            SettingsManager.setDanmakuAllowSpecial(
                context,
                value,
                activeDanmakuScope
            )
        }
        onQueueCloudSync(cloudSyncSettings.copy(allowSpecial = value))
    },
    onDanmakuHideInteractiveCommandsChange = { value ->
        launchSettingsWrite {
            SettingsManager
                .setDanmakuHideInteractiveCommands(context, value)
        }
    },
    onDanmakuSmartOcclusionChange = { value ->
        launchSettingsWrite {
            SettingsManager.setDanmakuSmartOcclusion(
                context,
                value,
                activeDanmakuScope
            )
        }
    },
    onDanmakuFullscreenPanelWidthModeChange = { value ->
        launchSettingsWrite {
            SettingsManager.setDanmakuFullscreenPanelWidthMode(context, value)
        }
    },
    onPortraitDanmakuDisplayAreaModeChange = { value ->
        launchSettingsWrite {
            SettingsManager
                .setPortraitDanmakuDisplayAreaMode(context, value)
        }
    },
    onDanmakuCloudSyncEnabledChange = { enabled ->
        launchSettingsWrite {
            SettingsManager
                .setDanmakuCloudSyncEnabled(context, enabled)
        }
        if (!enabled) {
            onCloudSyncDisabled()
        }
    },
    onDanmakuSyncNowClick = {
        onSyncNow()
    },
    onDanmakuBlockRulesRawChange = { value ->
        launchSettingsWrite {
            SettingsManager.setDanmakuBlockRulesRaw(
                context,
                value,
                activeDanmakuScope
            )
        }
    },
)
