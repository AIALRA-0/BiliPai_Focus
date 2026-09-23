package com.android.purebilibili.feature.settings

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AnimationSettingsScreenStructureTest {

    @Test
    fun animationSettingsScreen_controlsGlobalNavigationAnimationIndependently() {
        val source = animationSettingsSource()

        assertTrue(source.contains("title = \"全局导航动画\""))
        assertTrue(source.contains("options = predictiveBackStyleOptions"))
        assertTrue(source.contains("selectedValue = predictiveBackStyle"))
        assertTrue(source.contains("SettingsManager.setPredictiveBackEnabled(context, true)"))
        assertTrue(source.contains("SettingsManager.setPredictiveBackAnimationStyle("))
        assertTrue(source.contains("style.storageValue"))
        val predictiveItem = source
            .substringAfter("title = \"全局导航动画\"")
            .substringBefore("if (predictiveBackStyle ==")
        assertFalse(predictiveItem.contains("enabled = state.cardTransitionEnabled"))
        assertTrue(source.contains("if (predictiveBackStyle == BiliPaiPredictiveBackAnimationStyle.SCALE)"))
        assertTrue(source.contains("options = predictiveBackExitDirectionOptions"))
        assertTrue(source.contains("SettingsManager.setPredictiveBackExitDirection("))
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
        assertTrue(source.contains("text = \"导入液态玻璃设置？\""))
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
