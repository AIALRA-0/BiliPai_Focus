package com.android.purebilibili.feature.home.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.android.purebilibili.core.store.BottomBarLiquidGlassPreset
import com.android.purebilibili.core.store.LiquidGlassMode
import com.android.purebilibili.core.ui.motion.BottomBarMotionProfile
import com.android.purebilibili.core.ui.motion.resolveBottomBarMotionSpec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BottomBarIndicatorPolicyTest {

    @Test
    fun `five or more items stays close to top floating indicator family`() {
        val policy = resolveBottomBarIndicatorPolicy(itemCount = 5)
        val topTuning = resolveTopTabVisualTuning()

        assertEquals(topTuning.floatingIndicatorWidthMultiplier + 0.02f, policy.widthMultiplier)
        assertEquals(topTuning.floatingIndicatorMinWidthDp + 2f, policy.minWidthDp)
        assertEquals(topTuning.floatingIndicatorMaxWidthDp + 2f, policy.maxWidthDp)
        assertEquals(true, policy.clampToBounds)
        assertEquals(topTuning.floatingIndicatorMaxWidthToItemRatio + 0.02f, policy.maxWidthToItemRatio)
    }

    @Test
    fun `four items is only slightly wider than five item geometry`() {
        val policy = resolveBottomBarIndicatorPolicy(itemCount = 4)
        val topTuning = resolveTopTabVisualTuning()

        assertEquals(topTuning.floatingIndicatorWidthMultiplier + 0.04f, policy.widthMultiplier)
        assertEquals(topTuning.floatingIndicatorMinWidthDp + 4f, policy.minWidthDp)
        assertEquals(topTuning.floatingIndicatorMaxWidthDp + 4f, policy.maxWidthDp)
        assertEquals(topTuning.floatingIndicatorMaxWidthToItemRatio + 0.04f, policy.maxWidthToItemRatio)
        assertEquals(true, policy.clampToBounds)
    }

    @Test
    fun `icon and text mode with five items uses flatter indicator height on phone`() {
        assertEquals(
            50f,
            resolveBottomIndicatorHeightDp(
                labelMode = 0,
                isTablet = false,
                itemCount = 5
            )
        )
    }

    @Test
    fun `static indicator keeps theme color and disables refraction`() {
        val policy = resolveBottomBarIndicatorVisualPolicy(
            position = 2f,
            isDragging = false,
            velocity = 0f,
            useNeutralIndicatorTint = true
        )

        assertFalse(policy.isInMotion)
        assertFalse(policy.shouldRefract)
        assertFalse(policy.useNeutralTint)
    }

    @Test
    fun `moving indicator can use neutral tint and refraction`() {
        val policy = resolveBottomBarIndicatorVisualPolicy(
            position = 2.15f,
            isDragging = false,
            velocity = 52f,
            useNeutralIndicatorTint = true
        )

        assertTrue(policy.isInMotion)
        assertTrue(policy.shouldRefract)
        assertTrue(policy.useNeutralTint)
    }

    @Test
    fun `refraction layer enables tinted export only for floating moving glass`() {
        val active = resolveBottomBarRefractionLayerPolicy(
            isFloating = true,
            isLiquidGlassEnabled = true,
            indicatorVisualPolicy = BottomBarIndicatorVisualPolicy(
                isInMotion = true,
                shouldRefract = true,
                useNeutralTint = true
            )
        )
        val idle = resolveBottomBarRefractionLayerPolicy(
            isFloating = true,
            isLiquidGlassEnabled = true,
            indicatorVisualPolicy = BottomBarIndicatorVisualPolicy(
                isInMotion = false,
                shouldRefract = false,
                useNeutralTint = false
            )
        )
        val docked = resolveBottomBarRefractionLayerPolicy(
            isFloating = false,
            isLiquidGlassEnabled = true,
            indicatorVisualPolicy = BottomBarIndicatorVisualPolicy(
                isInMotion = true,
                shouldRefract = true,
                useNeutralTint = true
            )
        )

        assertTrue(active.captureTintedContentLayer)
        assertTrue(active.useCombinedBackdrop)
        assertFalse(idle.captureTintedContentLayer)
        assertFalse(idle.useCombinedBackdrop)
        assertFalse(docked.captureTintedContentLayer)
        assertFalse(docked.useCombinedBackdrop)
    }

    @Test
    fun `transitioning bottom pager keeps hidden refraction capture during tap pulse`() {
        assertTrue(
            shouldRenderBottomBarRefractionCapture(
                glassEnabled = true,
                hasBackdrop = true,
                captureProgress = 1f,
                isTransitionRunning = true,
                isFeedScrollInProgress = false,
                isBottomBarInteractionActive = true
            )
        )
        assertFalse(
            shouldRenderBottomBarRefractionCapture(
                glassEnabled = true,
                hasBackdrop = true,
                captureProgress = 1f,
                isTransitionRunning = true,
                isFeedScrollInProgress = false,
                isBottomBarInteractionActive = false
            )
        )
        assertTrue(
            shouldRenderBottomBarRefractionCapture(
                glassEnabled = true,
                hasBackdrop = true,
                captureProgress = 1f,
                isTransitionRunning = false,
                isFeedScrollInProgress = false,
                isBottomBarInteractionActive = true
            )
        )
    }

    @Test
    fun `transitioning bottom pager uses opaque indicator fallback instead of transparent lens`() {
        assertFalse(
            shouldRenderBottomBarIndicatorBackdrop(
                glassEnabled = true,
                hasContentBackdrop = true,
                indicatorProgress = 1f,
                isTransitionRunning = true,
                isBottomBarInteractionActive = true
            )
        )
        assertTrue(
            shouldRenderBottomBarIndicatorBackdrop(
                glassEnabled = true,
                hasContentBackdrop = true,
                indicatorProgress = 1f,
                isTransitionRunning = false,
                isBottomBarInteractionActive = true
            )
        )
    }

    @Test
    fun `transitioning bottom pager allows indicator backdrop only for click pulse`() {
        assertTrue(
            shouldRenderBottomBarIndicatorBackdrop(
                glassEnabled = true,
                hasContentBackdrop = true,
                indicatorProgress = 1f,
                isTransitionRunning = true,
                isBottomBarInteractionActive = true,
                allowTransitionIndicatorPulse = true
            )
        )
        assertTrue(
            shouldRenderBottomBarRefractionCapture(
                glassEnabled = true,
                hasBackdrop = true,
                captureProgress = 1f,
                isTransitionRunning = true,
                isBottomBarInteractionActive = true
            )
        )
    }

    @Test
    fun `indicator click settle pulse reuses indicator layer transform scale`() {
        val pressed = resolveBottomBarIndicatorLayerTransform(
            motionProgress = 1f,
            velocityItemsPerSecond = 0f,
            isDragging = false,
            dragScaleProgress = 1f
        )
        val settled = resolveBottomBarIndicatorLayerTransform(
            motionProgress = 0f,
            velocityItemsPerSecond = 0f,
            isDragging = false,
            dragScaleProgress = 0f
        )

        assertTrue(pressed.scaleX >= 1.35f)
        assertEquals(pressed.scaleX, pressed.scaleY)
        assertEquals(1f, settled.scaleX)
        assertEquals(1f, settled.scaleY)
    }

    @Test
    fun `idle bottom bar does not render hidden capture or indicator backdrop`() {
        assertFalse(
            shouldRenderBottomBarRefractionCapture(
                glassEnabled = true,
                hasBackdrop = true,
                captureProgress = 1f,
                isTransitionRunning = false,
                isFeedScrollInProgress = false,
                isBottomBarInteractionActive = false
            )
        )
        assertFalse(
            shouldRenderBottomBarIndicatorBackdrop(
                glassEnabled = true,
                hasContentBackdrop = true,
                indicatorProgress = 1f,
                isTransitionRunning = false,
                isBottomBarInteractionActive = false
            )
        )
    }

    @Test
    fun `transparent glass preset keeps idle background refraction without content capture`() {
        assertFalse(
            shouldRenderBottomBarRefractionCapture(
                glassEnabled = true,
                hasBackdrop = true,
                captureProgress = 1f,
                isTransitionRunning = false,
                isFeedScrollInProgress = false,
                isBottomBarInteractionActive = false
            )
        )
        assertTrue(
            shouldRenderBottomBarIndicatorBackdrop(
                glassEnabled = true,
                hasContentBackdrop = true,
                indicatorProgress = 1f,
                isTransitionRunning = false,
                isBottomBarInteractionActive = false,
                allowIdleGlassEffect = true
            )
        )
        assertFalse(
            shouldRenderBottomBarIndicatorBackdrop(
                glassEnabled = true,
                hasContentBackdrop = true,
                indicatorProgress = 1f,
                isTransitionRunning = true,
                isBottomBarInteractionActive = false,
                allowIdleGlassEffect = true
            )
        )
    }

    @Test
    fun `heavy bottom bar effects require settled interaction progress`() {
        assertFalse(
            shouldRenderBottomBarHeavyInteractiveEffects(
                isTransitionRunning = true,
                isBottomBarInteractionActive = true,
                progress = 1f
            )
        )
        assertFalse(
            shouldRenderBottomBarHeavyInteractiveEffects(
                isTransitionRunning = false,
                isBottomBarInteractionActive = false,
                progress = 1f
            )
        )
        assertFalse(
            shouldRenderBottomBarHeavyInteractiveEffects(
                isTransitionRunning = false,
                isBottomBarInteractionActive = true,
                progress = 0f
            )
        )
        assertTrue(
            shouldRenderBottomBarHeavyInteractiveEffects(
                isTransitionRunning = false,
                isBottomBarInteractionActive = true,
                progress = 1f
            )
        )
    }

    @Test
    fun `idle hold keeps refraction layer alive without marking indicator moving`() {
        val idle = BottomBarIndicatorVisualPolicy(
            isInMotion = false,
            shouldRefract = false,
            useNeutralTint = false
        )

        val held = resolveBottomBarIndicatorVisualPolicyWithHold(
            basePolicy = idle,
            keepRefractionLayerAlive = true
        )
        val released = resolveBottomBarIndicatorVisualPolicyWithHold(
            basePolicy = idle,
            keepRefractionLayerAlive = false
        )

        assertFalse(held.isInMotion)
        assertTrue(held.shouldRefract)
        assertFalse(released.shouldRefract)
    }

    @Test
    fun `ios moving indicator uses bottom dock surface on light floating bar`() {
        val color = resolveIosFloatingBottomIndicatorColor(
            isDarkTheme = false,
            visualPolicy = BottomBarIndicatorVisualPolicy(
                isInMotion = true,
                shouldRefract = true,
                useNeutralTint = true
            ),
            liquidGlassTuning = resolveLiquidGlassTuning(progress = 0.2f)
        )
        val surface = resolveBottomBarMovingIndicatorSurfaceColor(isDarkTheme = false)

        assertEquals(surface.red, color.red)
        assertEquals(surface.green, color.green)
        assertEquals(surface.blue, color.blue)
        assertTrue(color.alpha > 0f)
    }

    @Test
    fun `ios indicator surface uses neutral dock color instead of theme color`() {
        val themeColor = Color(0xFF6750A4)
        val color = resolveIosFloatingBottomIndicatorColor(
            isDarkTheme = false,
            visualPolicy = BottomBarIndicatorVisualPolicy(
                isInMotion = true,
                shouldRefract = true,
                useNeutralTint = false
            ),
            liquidGlassTuning = resolveLiquidGlassTuning(progress = 0.7f)
        )
        val surface = resolveBottomBarMovingIndicatorSurfaceColor(isDarkTheme = false)

        assertEquals(surface.red, color.red)
        assertEquals(surface.green, color.green)
        assertEquals(surface.blue, color.blue)
        assertTrue(color.blue > themeColor.blue)
        assertTrue(color.alpha > 0f)
    }

    @Test
    fun `ios moving indicator alpha has floor on bright floating bar`() {
        val alpha = resolveIosFloatingBottomIndicatorTintAlpha(
            visualPolicy = BottomBarIndicatorVisualPolicy(
                isInMotion = true,
                shouldRefract = true,
                useNeutralTint = true
            ),
            isDarkTheme = false,
            liquidGlassProgress = 0.2f,
            configuredAlpha = 0.12f
        )

        assertTrue(alpha >= 0.40f)
    }

    @Test
    fun `ios neutral moving indicator keeps bottom dock surface hue`() {
        val color = resolveIosFloatingBottomIndicatorColor(
            isDarkTheme = false,
            visualPolicy = BottomBarIndicatorVisualPolicy(
                isInMotion = true,
                shouldRefract = true,
                useNeutralTint = true
            ),
            liquidGlassTuning = resolveLiquidGlassTuning(progress = 0.2f)
        )
        val surface = resolveBottomBarMovingIndicatorSurfaceColor(isDarkTheme = false)

        assertEquals(surface.red, color.red)
        assertEquals(surface.green, color.green)
        assertEquals(surface.blue, color.blue)
    }

    @Test
    fun `moving refraction profile adds panel offset and keeps low theme emphasis`() {
        val profile = resolveBottomBarRefractionMotionProfile(
            position = 1.32f,
            velocity = 860f,
            isDragging = true
        )

        assertTrue(profile.progress > 0f)
        assertTrue(profile.exportPanelOffsetFraction > 0f)
        assertTrue(profile.indicatorPanelOffsetFraction > profile.exportPanelOffsetFraction)
        assertTrue(profile.visiblePanelOffsetFraction > 0f)
        assertTrue(profile.visibleSelectionEmphasis > 0.22f)
        assertTrue(profile.visibleSelectionEmphasis < 0.52f)
        assertTrue(profile.exportSelectionEmphasis > 0.45f)
        assertTrue(profile.exportSelectionEmphasis < 0.72f)
        assertTrue(profile.exportCaptureWidthScale > 1f)
    }

    @Test
    fun `bilipai tuned preset keeps original horizontal refraction motion`() {
        val profile = resolveBottomBarRefractionMotionProfile(
            position = 1.32f,
            velocity = 860f,
            isDragging = true
        )
        val effectiveProfile = resolveBottomBarEffectiveRefractionMotionProfile(
            preset = BottomBarLiquidGlassPreset.BILIPAI_TUNED,
            profile = profile
        )

        assertEquals(profile.progress, effectiveProfile.progress, 0.001f)
        assertEquals(profile.exportPanelOffsetFraction, effectiveProfile.exportPanelOffsetFraction, 0.001f)
        assertEquals(profile.indicatorPanelOffsetFraction, effectiveProfile.indicatorPanelOffsetFraction, 0.001f)
        assertEquals(profile.visiblePanelOffsetFraction, effectiveProfile.visiblePanelOffsetFraction, 0.001f)
        assertEquals(profile.visibleSelectionEmphasis, effectiveProfile.visibleSelectionEmphasis, 0.001f)
        assertEquals(profile.exportSelectionEmphasis, effectiveProfile.exportSelectionEmphasis, 0.001f)
        assertEquals(profile.exportCaptureWidthScale, effectiveProfile.exportCaptureWidthScale, 0.001f)
    }

    @Test
    fun `liquid glass lens progress follows backdrop preset progress`() {
        val idle = resolveBottomBarLiquidGlassLensProgress(motionProgress = 0f)
        val moving = resolveBottomBarLiquidGlassLensProgress(motionProgress = 1f)

        assertEquals(0f, idle, 0.001f)
        assertTrue(idle < moving)
        assertEquals(1f, moving, 0.001f)
    }

    @Test
    fun `backdrop preset lens dimensions match AndroidLiquidGlass bottom tabs`() {
        val capture = resolveBottomBarBackdropPresetCaptureLens(progress = 1f)
        val indicator = resolveBottomBarBackdropPresetIndicatorLens(progress = 1f)

        assertEquals(24f, capture.refractionHeightDp, 0.001f)
        assertEquals(24f, capture.refractionAmountDp, 0.001f)
        assertEquals(10f, indicator.refractionHeightDp, 0.001f)
        assertEquals(14f, indicator.refractionAmountDp, 0.001f)
    }

    @Test
    fun `stationary tap press activates indicator refraction preset`() {
        val progress = resolveBottomBarBackdropPresetProgress(
            motionProgress = 0f,
            verticalProgress = 0f,
            pressProgress = 1f
        )

        assertEquals(1f, progress.shellProgress, 0.001f)
        assertTrue(progress.captureProgress > 0f)
        assertEquals(1f, progress.indicatorProgress, 0.001f)
    }

    @Test
    fun `indicator glow follows press progress only when glass is enabled`() {
        assertEquals(
            0.72f,
            resolveBottomBarIndicatorGlowAlpha(
                glassEnabled = true,
                pressProgress = 0.72f,
                motionProgress = 0f
            ),
            0.001f
        )
        assertEquals(
            1f,
            resolveBottomBarIndicatorGlowAlpha(
                glassEnabled = true,
                pressProgress = 1.4f,
                motionProgress = 0f
            ),
            0.001f
        )
        assertEquals(
            0f,
            resolveBottomBarIndicatorGlowAlpha(
                glassEnabled = false,
                pressProgress = 1f,
                motionProgress = 1f
            ),
            0.001f
        )
    }

    @Test
    fun `indicator glow follows drag motion even without press progress`() {
        assertEquals(
            0.64f,
            resolveBottomBarIndicatorGlowAlpha(
                glassEnabled = true,
                pressProgress = 0f,
                motionProgress = 0.64f
            ),
            0.001f
        )
    }

    @Test
    fun `shell highlight follows indicator motion while dragging`() {
        assertEquals(
            0.86f,
            resolveBottomBarShellHighlightAlpha(
                glassEnabled = true,
                pressProgress = 0.12f,
                motionProgress = 0.86f
            ),
            0.001f
        )
        assertEquals(
            0.72f,
            resolveBottomBarShellHighlightAlpha(
                glassEnabled = true,
                pressProgress = 0.72f,
                motionProgress = 0.18f
            ),
            0.001f
        )
    }

    @Test
    fun `shell highlight keeps a floor while dragging so it stays pinned`() {
        // 慢拖时 press/motion 都低,但拖拽中高光应保持可见(跟手)
        assertEquals(
            0.6f,
            resolveBottomBarShellHighlightAlpha(
                glassEnabled = true,
                pressProgress = 0.1f,
                motionProgress = 0.2f,
                isDragging = true
            ),
            0.001f
        )
        // 非拖拽时无地板,沿用 max(press, motion)
        assertEquals(
            0.2f,
            resolveBottomBarShellHighlightAlpha(
                glassEnabled = true,
                pressProgress = 0.1f,
                motionProgress = 0.2f,
                isDragging = false
            ),
            0.001f
        )
        // 高 motion 不被地板压低
        assertEquals(
            0.9f,
            resolveBottomBarShellHighlightAlpha(
                glassEnabled = true,
                pressProgress = 0f,
                motionProgress = 0.9f,
                isDragging = true
            ),
            0.001f
        )
    }

    @Test
    fun `interactive highlight center follows indicator and panel offset`() {
        assertEquals(
            124f,
            resolveBottomBarInteractiveHighlightCenterX(
                indicatorTranslationXPx = 80f,
                itemWidthPx = 72f,
                panelOffsetPx = 8f
            ),
            0.001f
        )
    }

    @Test
    fun `interactive highlight modifier draws over existing surface`() {
        val source = listOf(
            java.io.File("app/src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt"),
            java.io.File("src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt")
        ).first { it.exists() }.readText()
        val highlightModifierSource = source
            .substringAfter("private fun Modifier.bottomBarInteractiveHighlight(")
            .substringBefore("internal fun resolveBottomBarBackdropPresetCaptureLens(")

        assertTrue(highlightModifierSource.indexOf("drawContent()") >= 0)
        assertTrue(
            highlightModifierSource.indexOf("drawContent()") <
                highlightModifierSource.indexOf("Brush.radialGradient(")
        )
        assertFalse(
            highlightModifierSource.contains("RuntimeShader"),
            "低版本系统会在 materialize modifier 时解析 RuntimeShader 类，交互高光不能直接引用它"
        )
        assertFalse(source.contains("import android.graphics.RuntimeShader"))
    }

    @Test
    fun `tap press can reuse indicator drag scale without horizontal motion`() {
        val transform = resolveBottomBarIndicatorLayerTransform(
            motionProgress = 1f,
            velocityItemsPerSecond = 0f,
            isDragging = false,
            dragScaleProgress = 1f
        )

        assertTrue(transform.scaleX > 1f)
        assertTrue(transform.scaleY > 1f)
        assertEquals(transform.scaleX, transform.scaleY, 0.001f)
    }

    @Test
    fun `home vertical scroll does not scale bottom bar shell or capture layers`() {
        val progress = resolveBottomBarBackdropPresetProgress(
            motionProgress = 0f,
            verticalProgress = 1f,
            pressProgress = 0f
        )
        val indicator = resolveBottomBarBackdropPresetIndicatorLens(
            progress = progress.indicatorProgress
        )

        assertEquals(0f, progress.shellProgress, 0.001f)
        assertEquals(0f, progress.captureProgress, 0.001f)
        assertEquals(0f, progress.indicatorProgress, 0.001f)
        assertEquals(0f, indicator.refractionHeightDp, 0.001f)
        assertEquals(0f, indicator.refractionAmountDp, 0.001f)
    }

    @Test
    fun `indicator layer transform uses android native motion spec`() {
        val transform = resolveBottomBarIndicatorLayerTransform(
            motionProgress = 1f,
            velocityItemsPerSecond = 0f,
            isDragging = true,
            motionSpec = resolveBottomBarMotionSpec(BottomBarMotionProfile.ANDROID_NATIVE_FLOATING)
        )

        assertEquals(88f / 56f, transform.scaleX, 0.001f)
        assertEquals(88f / 56f, transform.scaleY, 0.001f)
    }

    @Test
    fun `bottom bar keeps BiliPai drag scale target while using KernelSU velocity constants`() {
        val source = listOf(
            java.io.File("app/src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt"),
            java.io.File("src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt")
        ).first { it.exists() }.readText()

        assertTrue(source.contains("private const val BOTTOM_BAR_INDICATOR_DRAG_SCALE_TARGET = 88f / 56f"))
        assertTrue(source.contains("private const val KSU_INDICATOR_VELOCITY_NORMALIZATION_DIVISOR = 10f"))
        assertTrue(source.contains("private const val KSU_INDICATOR_VELOCITY_SCALE_X_MULTIPLIER = 0.75f"))
        assertTrue(source.contains("private const val KSU_INDICATOR_VELOCITY_SCALE_Y_MULTIPLIER = 0.25f"))
        assertTrue(source.contains("private const val KSU_INDICATOR_VELOCITY_CLAMP = 0.2f"))
    }

    @Test
    fun `indicator lens is driven by press progress while motion remains for capture`() {
        val source = listOf(
            java.io.File("app/src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt"),
            java.io.File("src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt")
        ).first { it.exists() }.readText()
        val rendererSource = source
            .substringAfter("private fun KernelSuAlignedBottomBar(")
            .substringBefore("@Composable\nprivate fun KernelSuBottomBarShell(")
        val lensSource = rendererSource
            .substringAfter("val indicatorLensSpec = resolveBottomBarBackdropPresetIndicatorLens(")
            .substringBefore(")")

        assertTrue(lensSource.contains("progress = effectivePressProgress"))
        assertTrue(rendererSource.contains("val effectiveIndicatorEffectProgress = maxOf("))
        assertTrue(rendererSource.contains("indicatorProgress = effectiveIndicatorEffectProgress"))
    }

    @Test
    fun `indicator keeps enlarged base during active drag while preserving velocity deformation`() {
        val partial = resolveBottomBarIndicatorLayerTransform(
            motionProgress = 0.12f,
            velocityItemsPerSecond = 0f,
            isDragging = true,
            motionSpec = resolveBottomBarMotionSpec(BottomBarMotionProfile.ANDROID_NATIVE_FLOATING)
        )
        val full = resolveBottomBarIndicatorLayerTransform(
            motionProgress = 1f,
            velocityItemsPerSecond = 0f,
            isDragging = true,
            motionSpec = resolveBottomBarMotionSpec(BottomBarMotionProfile.ANDROID_NATIVE_FLOATING)
        )
        val deformed = resolveBottomBarIndicatorLayerTransform(
            motionProgress = 0.12f,
            velocityItemsPerSecond = 2f,
            isDragging = true,
            motionSpec = resolveBottomBarMotionSpec(BottomBarMotionProfile.ANDROID_NATIVE_FLOATING)
        )

        assertEquals(full.scaleX, partial.scaleX, 0.001f)
        assertEquals(full.scaleY, partial.scaleY, 0.001f)
        assertEquals(88f / 56f, partial.scaleX, 0.001f)
        assertEquals(88f / 56f, partial.scaleY, 0.001f)
        assertTrue(deformed.scaleX > partial.scaleX)
        assertTrue(deformed.scaleY < partial.scaleY)
    }

    @Test
    fun `indicator does not enlarge during tap driven switch motion`() {
        val transform = resolveBottomBarIndicatorLayerTransform(
            motionProgress = 1f,
            velocityItemsPerSecond = 2f,
            isDragging = false,
            dragScaleProgress = 0f,
            motionSpec = resolveBottomBarMotionSpec(BottomBarMotionProfile.ANDROID_NATIVE_FLOATING)
        )

        assertEquals(1f, transform.scaleX, 0.001f)
        assertEquals(1f, transform.scaleY, 0.001f)
    }

    @Test
    fun `indicator drag enlargement can ease back with velocity deformation`() {
        val settling = resolveBottomBarIndicatorLayerTransform(
            motionProgress = 1f,
            velocityItemsPerSecond = 2f,
            isDragging = false,
            dragScaleProgress = 0.5f,
            motionSpec = resolveBottomBarMotionSpec(BottomBarMotionProfile.ANDROID_NATIVE_FLOATING)
        )
        val full = resolveBottomBarIndicatorLayerTransform(
            motionProgress = 1f,
            velocityItemsPerSecond = 0f,
            isDragging = true,
            dragScaleProgress = 1f,
            motionSpec = resolveBottomBarMotionSpec(BottomBarMotionProfile.ANDROID_NATIVE_FLOATING)
        )
        val halfWithoutVelocity = resolveBottomBarIndicatorLayerTransform(
            motionProgress = 1f,
            velocityItemsPerSecond = 0f,
            isDragging = false,
            dragScaleProgress = 0.5f,
            motionSpec = resolveBottomBarMotionSpec(BottomBarMotionProfile.ANDROID_NATIVE_FLOATING)
        )

        assertTrue(settling.scaleX > 1f)
        assertTrue(halfWithoutVelocity.scaleX < full.scaleX)
        assertTrue(settling.scaleX > settling.scaleY)
        assertTrue(settling.scaleY > 1f)
    }

    @Test
    fun `indicator velocity deformation follows KernelSU constants without changing drag scale target`() {
        val baseScale = 88f / 56f
        val transform = resolveBottomBarIndicatorLayerTransform(
            motionProgress = 1f,
            velocityItemsPerSecond = 2f,
            isDragging = true,
            dragScaleProgress = 1f,
            motionSpec = resolveBottomBarMotionSpec(BottomBarMotionProfile.ANDROID_NATIVE_FLOATING)
        )

        assertEquals(baseScale / (1f - ((2f / 10f) * 0.75f)), transform.scaleX, 0.001f)
        assertEquals(baseScale * (1f - ((2f / 10f) * 0.25f)), transform.scaleY, 0.001f)
    }

    @Test
    fun `shared indicator drag scale uses KernelSU separate axis springs`() {
        val source = listOf(
            java.io.File("app/src/main/java/com/android/purebilibili/core/ui/animation/DampedDragAnimation.kt"),
            java.io.File("src/main/java/com/android/purebilibili/core/ui/animation/DampedDragAnimation.kt")
        ).first { it.exists() }.readText()

        assertTrue(source.contains("private const val KERNEL_SU_PRESSED_SCALE = 78f / 56f"))
        assertTrue(source.contains("private val scaleXAnimationSpec = spring(0.6f, 250f, 0.001f)"))
        assertTrue(source.contains("private val scaleYAnimationSpec = spring(0.7f, 250f, 0.001f)"))
        assertTrue(source.contains("scaleXAnimation.animateTo(KERNEL_SU_PRESSED_SCALE, scaleXAnimationSpec)"))
        assertTrue(source.contains("scaleYAnimation.animateTo(KERNEL_SU_PRESSED_SCALE, scaleYAnimationSpec)"))
    }

    @Test
    fun `settle rebound transform expands on both axes and returns to neutral`() {
        val compressed = resolveBottomBarSettleReboundTransform(progress = 0.1f)
        val rebound = resolveBottomBarSettleReboundTransform(progress = 0.46f)
        val idle = resolveBottomBarSettleReboundTransform(progress = 1f)

        assertTrue(compressed.scaleX < 1f)
        assertTrue(compressed.scaleX >= 0.96f)
        assertTrue(compressed.scaleY > 1f)
        assertTrue(compressed.scaleY <= 1.03f)
        assertTrue(rebound.scaleX > 1f)
        assertTrue(rebound.scaleX <= 1.10f)
        assertTrue(rebound.scaleY > 1f)
        assertTrue(rebound.scaleY <= 1.09f)
        assertEquals(1f, idle.scaleX, 0.001f)
        assertEquals(1f, idle.scaleY, 0.001f)
    }

    @Test
    fun `shared segmented control ignores tap press for refraction when disabled`() {
        assertEquals(
            0f,
            resolveSegmentedControlMotionProgress(
                pressProgress = 1f,
                refractionProgress = 0f,
                tapPressRefractionEnabled = false
            ),
            0.001f
        )
        assertEquals(
            1f,
            resolveSegmentedControlMotionProgress(
                pressProgress = 1f,
                refractionProgress = 0f,
                tapPressRefractionEnabled = true
            ),
            0.001f
        )
        assertEquals(
            0.42f,
            resolveSegmentedControlMotionProgress(
                pressProgress = 1f,
                refractionProgress = 0.42f,
                tapPressRefractionEnabled = false
            ),
            0.001f
        )
    }

    @Test
    fun `shared segmented control motion is calmer than bottom dock`() {
        val bottomDock = resolveBottomBarMotionSpec(BottomBarMotionProfile.ANDROID_NATIVE_FLOATING)
        val segmented = resolveSegmentedControlMotionSpec()

        assertTrue(segmented.drag.selectionSpring.stiffness < bottomDock.drag.selectionSpring.stiffness)
        assertTrue(segmented.drag.selectionSpring.dampingRatio > bottomDock.drag.selectionSpring.dampingRatio)
        assertTrue(segmented.refraction.speedProgressDivisorPxPerSecond > bottomDock.refraction.speedProgressDivisorPxPerSecond)
        assertTrue(segmented.refraction.dragProgressFloor < bottomDock.refraction.dragProgressFloor)
        assertTrue(segmented.refraction.panelOffsetMaxDp < bottomDock.refraction.panelOffsetMaxDp)
    }

    @Test
    fun `idle refraction profile disables offset and keeps full visible emphasis`() {
        val profile = resolveBottomBarRefractionMotionProfile(
            position = 2f,
            velocity = 0f,
            isDragging = false
        )

        assertEquals(0f, profile.progress)
        assertEquals(0f, profile.exportPanelOffsetFraction)
        assertEquals(0f, profile.indicatorPanelOffsetFraction)
        assertEquals(0f, profile.visiblePanelOffsetFraction)
        assertEquals(1f, profile.visibleSelectionEmphasis)
        assertEquals(1f, profile.exportSelectionEmphasis)
        assertEquals(1f, profile.exportCaptureWidthScale)
    }

    @Test
    fun `sliding item coverage follows indicator instead of fixed selected tab`() {
        val home = resolveBottomBarItemCoverage(
            itemIndex = 0,
            indicatorPosition = 0.8f,
            currentSelectedIndex = 0,
            motionProgress = 1f
        )
        val dynamic = resolveBottomBarItemCoverage(
            itemIndex = 1,
            indicatorPosition = 0.8f,
            currentSelectedIndex = 0,
            motionProgress = 1f
        )

        assertTrue(dynamic > home)
        assertTrue(
            resolveBottomBarItemMotionScale(dynamic, motionProgress = 1f) >
                resolveBottomBarItemMotionScale(home, motionProgress = 1f)
        )
    }

    @Test
    fun `sliding item coverage fills icon for item covered by indicator`() {
        val home = resolveBottomBarItemCoverage(
            itemIndex = 0,
            indicatorPosition = 0.8f,
            currentSelectedIndex = 0,
            motionProgress = 1f
        )
        val dynamic = resolveBottomBarItemCoverage(
            itemIndex = 1,
            indicatorPosition = 0.8f,
            currentSelectedIndex = 0,
            motionProgress = 1f
        )

        assertTrue(home in 0f..1f)
        assertTrue(dynamic in 0f..1f)
        assertFalse(home >= 0.5f)
        assertTrue(dynamic >= 0.5f)
    }

    @Test
    fun `sliding item coverage drives selected icon alpha continuously`() {
        val home = resolveBottomBarItemCoverage(
            itemIndex = 0,
            indicatorPosition = 0.8f,
            currentSelectedIndex = 0,
            motionProgress = 1f
        )
        val dynamic = resolveBottomBarItemCoverage(
            itemIndex = 1,
            indicatorPosition = 0.8f,
            currentSelectedIndex = 0,
            motionProgress = 1f
        )

        assertEquals(0.2f, home, 0.001f)
        assertEquals(0.8f, dynamic, 0.001f)
    }

    @Test
    fun `sliding item color weight is the indicator coverage`() {
        val dynamic = resolveBottomBarItemCoverage(
            itemIndex = 1,
            indicatorPosition = 0.8f,
            currentSelectedIndex = 0,
            motionProgress = 1f
        )

        assertEquals(0.8f, dynamic, 0.001f)
    }

    @Test
    fun `edge overscroll clamps visual indicator position and keeps selected coverage stable`() {
        val startVisualPosition = resolveBottomBarVisualIndicatorPosition(
            rawPosition = -0.36f,
            itemCount = 5
        )
        val endVisualPosition = resolveBottomBarVisualIndicatorPosition(
            rawPosition = 4.36f,
            itemCount = 5
        )

        assertEquals(0f, startVisualPosition, 0.001f)
        assertEquals(4f, endVisualPosition, 0.001f)
        assertEquals(
            1f,
            resolveBottomBarItemCoverage(
                itemIndex = 0,
                indicatorPosition = startVisualPosition,
                currentSelectedIndex = 0,
                motionProgress = 1f
            ),
            0.001f
        )
        assertEquals(
            0f,
            resolveBottomBarItemCoverage(
                itemIndex = 1,
                indicatorPosition = startVisualPosition,
                currentSelectedIndex = 0,
                motionProgress = 1f
            ),
            0.001f
        )
        assertEquals(
            1f,
            resolveBottomBarItemCoverage(
                itemIndex = 4,
                indicatorPosition = endVisualPosition,
                currentSelectedIndex = 4,
                motionProgress = 1f
            ),
            0.001f
        )
        assertEquals(
            0f,
            resolveBottomBarItemCoverage(
                itemIndex = 3,
                indicatorPosition = endVisualPosition,
                currentSelectedIndex = 4,
                motionProgress = 1f
            ),
            0.001f
        )
    }

    @Test
    fun `edge strain reports only overscroll beyond the visual bounds`() {
        assertEquals(
            -0.36f,
            resolveBottomBarEdgeStrain(rawPosition = -0.36f, itemCount = 5),
            0.001f
        )
        assertEquals(
            0.36f,
            resolveBottomBarEdgeStrain(rawPosition = 4.36f, itemCount = 5),
            0.001f
        )
        assertEquals(
            0f,
            resolveBottomBarEdgeStrain(rawPosition = 2.25f, itemCount = 5),
            0.001f
        )
    }

    @Test
    fun `sliding item scale is derived from shared coverage`() {
        val home = resolveBottomBarItemCoverage(
            itemIndex = 0,
            indicatorPosition = 0.8f,
            currentSelectedIndex = 0,
            motionProgress = 1f
        )
        val dynamic = resolveBottomBarItemCoverage(
            itemIndex = 1,
            indicatorPosition = 0.8f,
            currentSelectedIndex = 0,
            motionProgress = 1f
        )

        assertTrue(resolveBottomBarItemMotionScale(dynamic, motionProgress = 1f) > 1f)
        assertTrue(resolveBottomBarItemMotionScale(home, motionProgress = 1f) > 1f)
    }

    @Test
    fun `sampled item scale follows press progress even before indicator covers the tab`() {
        val notCoveredScale = resolveBottomBarSampledItemMotionScale(
            coverage = 0f,
            motionProgress = 1f,
            pressProgress = 1f
        )
        val partiallyCoveredScale = resolveBottomBarSampledItemMotionScale(
            coverage = 0.25f,
            motionProgress = 1f,
            pressProgress = 1f
        )

        assertEquals(1.2f, notCoveredScale, 0.001f)
        assertEquals(1.2f, partiallyCoveredScale, 0.001f)
    }

    @Test
    fun `click pulse transform rebounds horizontally without vertical lift`() {
        val pressed = resolveBottomBarClickPulseTransform(progress = 0.18f)
        val overshoot = resolveBottomBarClickPulseTransform(progress = 0.46f)
        val settleBack = resolveBottomBarClickPulseTransform(progress = 0.72f)
        val idle = resolveBottomBarClickPulseTransform(progress = 1f)

        assertEquals(0.945f, pressed.scaleX, 0.001f)
        assertTrue(pressed.scaleX < 1f)
        assertTrue(overshoot.scaleX > 1f)
        assertTrue(overshoot.scaleX >= 1.03f)
        assertTrue(settleBack.scaleX > 1f)
        assertTrue(settleBack.scaleX < overshoot.scaleX)
        assertEquals(1f, pressed.scaleY, 0.001f)
        assertEquals(1f, overshoot.scaleY, 0.001f)
        assertEquals(1f, idle.scaleX, 0.001f)
        assertEquals(1f, idle.scaleY, 0.001f)
    }

    @Test
    fun `click pulse release decays without a second compression twitch`() {
        val rebound = resolveBottomBarClickPulseTransform(progress = 0.46f)
        val settle = resolveBottomBarClickPulseTransform(progress = 0.68f)
        val nearlyIdle = resolveBottomBarClickPulseTransform(progress = 0.88f)

        assertTrue(rebound.scaleX > settle.scaleX)
        assertTrue(settle.scaleX > nearlyIdle.scaleX)
        assertTrue(nearlyIdle.scaleX >= 1f)
    }

    @Test
    fun `sliding color transfers continuously from current tab to next tab`() {
        val home = resolveBottomBarItemCoverage(
            itemIndex = 0,
            indicatorPosition = 0.2f,
            currentSelectedIndex = 0,
            motionProgress = 1f
        )
        val dynamic = resolveBottomBarItemCoverage(
            itemIndex = 1,
            indicatorPosition = 0.2f,
            currentSelectedIndex = 0,
            motionProgress = 1f
        )

        assertEquals(0.8f, home, 0.001f)
        assertEquals(0.2f, dynamic, 0.001f)
    }

    @Test
    fun `sliding item scale only affects indicator neighbors`() {
        val home = resolveBottomBarItemCoverage(
            itemIndex = 0,
            indicatorPosition = 0.5f,
            currentSelectedIndex = 0,
            motionProgress = 1f
        )
        val dynamic = resolveBottomBarItemCoverage(
            itemIndex = 1,
            indicatorPosition = 0.5f,
            currentSelectedIndex = 0,
            motionProgress = 1f
        )
        val history = resolveBottomBarItemCoverage(
            itemIndex = 2,
            indicatorPosition = 0.5f,
            currentSelectedIndex = 0,
            motionProgress = 1f
        )
        val profile = resolveBottomBarRefractionMotionProfile(
            position = 0.5f,
            velocity = 620f,
            isDragging = true
        )

        assertTrue(resolveBottomBarItemMotionScale(home, motionProgress = 1f) > 1f)
        assertTrue(resolveBottomBarItemMotionScale(dynamic, motionProgress = 1f) > 1f)
        assertEquals(1f, resolveBottomBarItemMotionScale(history, motionProgress = 1f))
        assertEquals(0f, history)
        assertTrue(profile.progress > 0f)
    }

    @Test
    fun `idle item coverage follows visual indicator instead of selected page`() {
        val home = resolveBottomBarItemCoverage(
            itemIndex = 0,
            indicatorPosition = 0.5f,
            currentSelectedIndex = 0,
            motionProgress = 0f
        )
        val dynamic = resolveBottomBarItemCoverage(
            itemIndex = 1,
            indicatorPosition = 0.5f,
            currentSelectedIndex = 0,
            motionProgress = 0f
        )

        assertEquals(0.5f, home, 0.001f)
        assertEquals(0.5f, dynamic, 0.001f)
        assertEquals(1f, resolveBottomBarItemMotionScale(home, motionProgress = 0f), 0.001f)
    }

    @Test
    fun `frosted idle indicator keeps stronger alpha floor for visibility`() {
        val alpha = resolveBottomBarIndicatorTintAlpha(
            shouldRefract = false,
            liquidGlassProgress = 1f,
            configuredAlpha = 0.14f
        )

        assertTrue(alpha >= 0.56f)
    }

    @Test
    fun `clear idle indicator stays lighter than frosted idle indicator`() {
        val clear = resolveBottomBarIndicatorTintAlpha(
            shouldRefract = false,
            liquidGlassProgress = 0f,
            configuredAlpha = 0.14f
        )
        val frosted = resolveBottomBarIndicatorTintAlpha(
            shouldRefract = false,
            liquidGlassProgress = 1f,
            configuredAlpha = 0.14f
        )

        assertTrue(clear < frosted)
    }
}
