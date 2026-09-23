package com.android.purebilibili.feature.settings

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AnimationSettingsScreenStructureTest {

    @Test
    fun animationSettingsScreen_controlsGlobalPredictivePreviewIndependently() {
        val source = animationSettingsSource()

        assertTrue(source.contains("title = \"预测性返回手势\""))
        assertTrue(source.contains("SettingsManager.setPredictiveBackEnabled(context, enabled)"))
        val predictiveItem = source
            .substringAfter("title = \"预测性返回手势\"")
            .substringBefore("AppPreferenceDivider()")
        assertFalse(predictiveItem.contains("enabled = state.cardTransitionEnabled"))
        assertFalse(source.contains("setPredictiveBackAnimationStyle"))
        assertFalse(source.contains("setPredictiveBackExitDirection"))
        assertFalse(source.contains("resolvePredictiveBackStyleOptions"))
        assertFalse(source.contains("resolvePredictiveBackExitDirectionOptions"))
    }

    @Test
    fun animationSettingsScreen_exposesRealtimeTransitionBlurToggle() {
        val source = animationSettingsSource()

        assertTrue(source.contains("title = \"转场时模糊背景\""))
        assertTrue(source.contains("checked = videoTransitionRealtimeBlurEnabled"))
        assertTrue(source.contains("toggleVideoTransitionRealtimeBlur"))
    }

    @Test
    fun animationSettingsScreen_exposesLiveSurfaceCardTransitionToggle() {
        val source = animationSettingsSource()

        assertTrue(source.contains("title = \"实时画面转场\""))
        assertTrue(source.contains("checked = liveSurfaceCardTransitionEnabled"))
        assertTrue(source.contains("toggleLiveSurfaceCardTransition"))
        assertTrue(source.contains("enabled = state.cardTransitionEnabled"))
        assertTrue(source.contains("getLiveSurfaceCardTransitionEnabled"))
    }

    @Test
    fun animationSettingsScreen_exposesVideoSharedReturnGestureFollowToggle() {
        val source = animationSettingsSource()

        assertTrue(source.contains("title = \"视频返回跟手姿态\""))
        assertTrue(source.contains("整卡跟手平移"))
        assertTrue(source.contains("checked = appNavigationSettings.videoSharedReturnGestureFollowEnabled"))
        assertTrue(source.contains("SettingsManager.setVideoSharedReturnGestureFollowEnabled("))
        assertTrue(source.contains("enabled = state.cardTransitionEnabled"))
    }

    @Test
    fun animationSettingsScreen_doesNotExposeLiveReturnPreviewToggle() {
        val source = animationSettingsSource()

        assertFalse(source.contains("预测返回预览实时画面"))
        assertFalse(source.contains("videoTransitionLiveReturnPreviewEnabled"))
        assertFalse(source.contains("setVideoTransitionLiveReturnPreviewEnabled"))
        assertFalse(source.contains("getVideoTransitionLiveReturnPreviewEnabled"))
    }

    @Test
    fun animationSettingsScreen_importsLiquidGlassSettingsAfterConfirmation() {
        val source = animationSettingsSource()

        assertTrue(source.contains("ActivityResultContracts.OpenDocument()"))
        assertTrue(source.contains("readLiquidGlassImportSession(uri)"))
        assertTrue(source.contains("title = \"导入液态玻璃设置？\""))
        assertTrue(source.contains("预览图片和其他应用设置不会改变"))
        assertTrue(source.contains("applyLiquidGlassImport(importSession)"))
    }

    private fun animationSettingsSource(): String {
        return listOf(
            File("app/src/main/java/com/android/purebilibili/feature/settings/screen/AnimationSettingsScreen.kt"),
            File("src/main/java/com/android/purebilibili/feature/settings/screen/AnimationSettingsScreen.kt"),
        ).first { it.exists() }.readText()
    }
}
