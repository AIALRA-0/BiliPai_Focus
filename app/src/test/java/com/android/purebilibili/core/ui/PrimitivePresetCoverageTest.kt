package com.android.purebilibili.core.ui

import com.android.purebilibili.core.theme.AppUiStyle
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Asserts every shared adaptive primitive exposes a preset-aware renderer decision
 * so feature screens get the right look on MIUIX / MD3 without primitive
 * call sites changing. Compose UI tests would assert actual rendered nodes;
 * here we assert the policy layer that drives the dispatch.
 */
class PrimitivePresetCoverageTest {

    @Test
    fun unifiedRenderer_matches_uiStyleMatrix() {
        // 两值模型：MIUIX → MIUIX_BRIDGED、MATERIAL3 → MATERIAL3。
        assertEquals(
            PresetPrimitiveRenderer.MIUIX_BRIDGED,
            resolvePresetPrimitiveRenderer(AppUiStyle.MIUIX)
        )
        assertEquals(
            PresetPrimitiveRenderer.MATERIAL3,
            resolvePresetPrimitiveRenderer(AppUiStyle.MATERIAL3)
        )
    }

    @Test
    fun legacyLargeTitleBar_isRemovedAfterFeatureMigration() {
        val legacySource = listOf(
            File("app/src/main/java/com/android/purebilibili/core/ui/iOSLargeTitleBar.kt"),
            File("src/main/java/com/android/purebilibili/core/ui/iOSLargeTitleBar.kt"),
        )
        assertEquals(false, legacySource.any { it.exists() })
    }

    @Test
    fun dialogActionLayoutPolicy_isConstantAfterIosMigration() {
        // 2B 迁移：iOS 全宽铺满操作区行为已随单向迁移删除，布局政策收敛为常量。
        assertEquals(false, resolveDialogActionLayoutPolicy().expandToContainer)
    }

    @Test
    fun adaptiveBottomSheetVisual_branchesByUiStyle() {
        // AppSheetComponents 按两值风格分支圆角等级（胶囊级）。
        val miuix = resolveAdaptiveBottomSheetVisualSpec(AppUiStyle.MIUIX)
        val material3 = resolveAdaptiveBottomSheetVisualSpec(AppUiStyle.MATERIAL3)
        // 2B 迁移：两值风格统一使用胶囊圆角与 Material 拖拽把手。
        assertEquals(22, miuix.cornerRadiusDp)
        assertEquals(28, material3.cornerRadiusDp)
        assertTrue(miuix.useMaterialDragHandle)
        assertTrue(material3.useMaterialDragHandle)
    }
}
