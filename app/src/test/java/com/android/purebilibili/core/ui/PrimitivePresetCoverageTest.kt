package com.android.purebilibili.core.ui

import com.android.purebilibili.core.theme.AndroidNativeVariant
import com.android.purebilibili.core.theme.UiPreset
import com.android.purebilibili.core.ui.IOSLargeTitleBarRenderer
import com.android.purebilibili.core.ui.resolveLargeTitleBarRenderer
import com.android.purebilibili.feature.home.components.IOSRefreshIndicatorRenderer
import com.android.purebilibili.feature.home.components.resolveRefreshIndicatorRenderer
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Asserts every shared iOS* primitive exposes a preset-aware renderer decision
 * so feature screens get the right look on iOS / MD3 / Miuix without primitive
 * call sites changing. Compose UI tests would assert actual rendered nodes;
 * here we assert the policy layer that drives the dispatch.
 */
class PrimitivePresetCoverageTest {

    @Test
    fun unifiedRenderer_matches_uiPresetMatrix() {
        assertEquals(
            PresetPrimitiveRenderer.IOS,
            resolvePresetPrimitiveRenderer(UiPreset.IOS, AndroidNativeVariant.MATERIAL3)
        )
        assertEquals(
            PresetPrimitiveRenderer.MATERIAL3,
            resolvePresetPrimitiveRenderer(UiPreset.MD3, AndroidNativeVariant.MATERIAL3)
        )
        assertEquals(
            PresetPrimitiveRenderer.MIUIX_BRIDGED,
            resolvePresetPrimitiveRenderer(UiPreset.MD3, AndroidNativeVariant.MIUIX)
        )
    }

    @Test
    fun largeTitleBarRenderer_collapsesMd3AndMiuixOntoAdaptiveTopAppBar() {
        assertEquals(
            IOSLargeTitleBarRenderer.IOS_LARGE_TITLE,
            resolveLargeTitleBarRenderer(UiPreset.IOS, AndroidNativeVariant.MATERIAL3)
        )
        assertEquals(
            IOSLargeTitleBarRenderer.ADAPTIVE_TOP_APP_BAR,
            resolveLargeTitleBarRenderer(UiPreset.MD3, AndroidNativeVariant.MATERIAL3)
        )
        assertEquals(
            IOSLargeTitleBarRenderer.ADAPTIVE_TOP_APP_BAR,
            resolveLargeTitleBarRenderer(UiPreset.MD3, AndroidNativeVariant.MIUIX)
        )
    }

    @Test
    fun refreshIndicatorRenderer_swapsToMaterialWhenNotIos() {
        assertEquals(
            IOSRefreshIndicatorRenderer.CUPERTINO_IOS,
            resolveRefreshIndicatorRenderer(UiPreset.IOS, AndroidNativeVariant.MATERIAL3)
        )
        assertEquals(
            IOSRefreshIndicatorRenderer.MATERIAL3_CIRCULAR,
            resolveRefreshIndicatorRenderer(UiPreset.MD3, AndroidNativeVariant.MATERIAL3)
        )
        assertEquals(
            IOSRefreshIndicatorRenderer.MIUIX_BRIDGED,
            resolveRefreshIndicatorRenderer(UiPreset.MD3, AndroidNativeVariant.MIUIX)
        )
    }

    @Test
    fun dialogActionLayoutPolicy_stillBranches() {
        // iOSDialogComponents already exposes resolveIosDialogActionLayoutPolicy.
        // Verify it continues to differentiate iOS vs MD3.
        val iosLayout = resolveIosDialogActionLayoutPolicy(UiPreset.IOS)
        val md3Layout = resolveIosDialogActionLayoutPolicy(UiPreset.MD3)
        assertEquals(true, iosLayout.expandToContainer)
        assertEquals(false, md3Layout.expandToContainer)
    }

    @Test
    fun adaptiveBottomSheetVisual_stillBranches() {
        // iOSSheetComponents exposes resolveAdaptiveBottomSheetVisualSpec.
        val ios = resolveAdaptiveBottomSheetVisualSpec(UiPreset.IOS)
        val md3 = resolveAdaptiveBottomSheetVisualSpec(UiPreset.MD3)
        assertEquals(14, ios.cornerRadiusDp)
        assertEquals(28, md3.cornerRadiusDp)
        assertEquals(false, ios.useMaterialDragHandle)
        assertEquals(true, md3.useMaterialDragHandle)
    }
}
