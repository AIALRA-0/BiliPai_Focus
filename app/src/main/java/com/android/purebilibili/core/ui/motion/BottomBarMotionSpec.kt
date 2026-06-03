package com.android.purebilibili.core.ui.motion

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring

internal data class MotionSpringConfig(
    val dampingRatio: Float,
    val stiffness: Float,
    val visibilityThreshold: Float? = null
) {
    fun toSpringSpec(): SpringSpec<Float> {
        return spring(
            dampingRatio = dampingRatio,
            stiffness = stiffness,
            visibilityThreshold = visibilityThreshold
        )
    }
}

internal data class BottomBarDragMotionSpec(
    val baseResistance: Float,
    val overscrollResistance: Float,
    val overscrollLimitItems: Float,
    val flingProjectionTimeSeconds: Float,
    val maxReleaseStepCount: Int,
    val pressSpring: MotionSpringConfig,
    val selectionSpring: MotionSpringConfig,
    val offsetSnapSpring: MotionSpringConfig
)

internal data class BottomBarRefractionMotionSpec(
    val movingVelocityThresholdPxPerSecond: Float,
    val speedProgressDivisorPxPerSecond: Float,
    val dragProgressFloor: Float,
    val motionDeadzone: Float,
    val panelOffsetMaxDp: Float
)

internal data class BottomBarIndicatorMotionSpec(
    val deformationScaleXDelta: Float,
    val deformationScaleYCompressionRatio: Float,
    val scaleSpring: MotionSpringConfig,
    val dragScaleSpring: MotionSpringConfig,
    val lensVelocityRangePxPerSecond: Float,
    val railFractionStretchMultiplier: Float,
    val capsuleVelocityNormalizationDivisor: Float,
    val capsuleVelocityScaleXMultiplier: Float,
    val capsuleVelocityScaleYMultiplier: Float,
    val capsuleVelocityClamp: Float
)

internal data class BottomBarMotionSpec(
    val drag: BottomBarDragMotionSpec,
    val refraction: BottomBarRefractionMotionSpec,
    val indicator: BottomBarIndicatorMotionSpec
)

internal enum class BottomBarMotionProfile {
    DEFAULT,
    IOS_FLOATING,
    ANDROID_NATIVE_FLOATING,
    MIUI_FLOATING
}

internal fun resolveBottomBarMotionSpec(
    profile: BottomBarMotionProfile = BottomBarMotionProfile.DEFAULT
): BottomBarMotionSpec {
    val base = createDefaultBottomBarMotionSpec()
    return when (profile) {
        BottomBarMotionProfile.DEFAULT -> base
        BottomBarMotionProfile.IOS_FLOATING -> base.copy(
            drag = base.drag.copy(
                baseResistance = 1.04f,
                overscrollResistance = 0.34f,
                flingProjectionTimeSeconds = 0.24f,
                selectionSpring = MotionSpringConfig(
                    dampingRatio = 0.78f,
                    stiffness = 430f
                )
            ),
            refraction = base.refraction.copy(
                movingVelocityThresholdPxPerSecond = 32f,
                speedProgressDivisorPxPerSecond = 1150f,
                dragProgressFloor = 0.24f,
                panelOffsetMaxDp = 6f
            ),
            indicator = base.indicator.copy(
                deformationScaleXDelta = 0.42f,
                deformationScaleYCompressionRatio = 0.58f,
                scaleSpring = MotionSpringConfig(
                    dampingRatio = 0.44f,
                    stiffness = 520f
                ),
                dragScaleSpring = MotionSpringConfig(
                    dampingRatio = 0.56f,
                    stiffness = 360f
                ),
                lensVelocityRangePxPerSecond = 2200f
            )
        )
        BottomBarMotionProfile.ANDROID_NATIVE_FLOATING -> base.copy(
            drag = base.drag.copy(
                baseResistance = 1.02f,
                overscrollResistance = 0.34f,
                flingProjectionTimeSeconds = 0.20f,
                selectionSpring = MotionSpringConfig(
                    dampingRatio = 0.68f,
                    stiffness = 520f
                ),
                offsetSnapSpring = MotionSpringConfig(
                    dampingRatio = 0.62f,
                    stiffness = 420f
                )
            ),
            refraction = base.refraction.copy(
                movingVelocityThresholdPxPerSecond = 38f,
                speedProgressDivisorPxPerSecond = 1250f,
                dragProgressFloor = 0.22f,
                panelOffsetMaxDp = 5f
            ),
            indicator = base.indicator.copy(
                deformationScaleXDelta = 0.40f,
                deformationScaleYCompressionRatio = 0.54f,
                scaleSpring = MotionSpringConfig(
                    dampingRatio = 0.46f,
                    stiffness = 620f
                ),
                dragScaleSpring = MotionSpringConfig(
                    dampingRatio = 0.54f,
                    stiffness = 400f
                ),
                lensVelocityRangePxPerSecond = 2500f,
                railFractionStretchMultiplier = 0.095f,
                capsuleVelocityNormalizationDivisor = 10.5f,
                capsuleVelocityScaleXMultiplier = 0.72f,
                capsuleVelocityScaleYMultiplier = 0.24f,
                capsuleVelocityClamp = 0.20f
            )
        )
        BottomBarMotionProfile.MIUI_FLOATING -> base.copy(
            drag = base.drag.copy(
                baseResistance = 1.01f,
                overscrollResistance = 0.30f,
                flingProjectionTimeSeconds = 0.18f,
                selectionSpring = MotionSpringConfig(
                    dampingRatio = 0.84f,
                    stiffness = 500f
                )
            ),
            refraction = base.refraction.copy(
                movingVelocityThresholdPxPerSecond = 48f,
                speedProgressDivisorPxPerSecond = 1500f,
                dragProgressFloor = 0.16f,
                panelOffsetMaxDp = 4f
            ),
            indicator = base.indicator.copy(
                deformationScaleXDelta = 0.30f,
                deformationScaleYCompressionRatio = 0.48f,
                scaleSpring = MotionSpringConfig(
                    dampingRatio = 0.58f,
                    stiffness = 620f
                ),
                dragScaleSpring = MotionSpringConfig(
                    dampingRatio = 0.64f,
                    stiffness = 430f
                ),
                lensVelocityRangePxPerSecond = 2800f,
                railFractionStretchMultiplier = 0.065f,
                capsuleVelocityNormalizationDivisor = 11f,
                capsuleVelocityScaleXMultiplier = 0.64f,
                capsuleVelocityScaleYMultiplier = 0.22f,
                capsuleVelocityClamp = 0.18f
            )
        )
    }
}

private fun createDefaultBottomBarMotionSpec(): BottomBarMotionSpec {
    return BottomBarMotionSpec(
        drag = BottomBarDragMotionSpec(
            baseResistance = 1f,
            overscrollResistance = 0.3f,
            overscrollLimitItems = 0.5f,
            flingProjectionTimeSeconds = 0.2f,
            maxReleaseStepCount = 1,
            pressSpring = MotionSpringConfig(
                dampingRatio = 1f,
                stiffness = 1000f,
                visibilityThreshold = 0.001f
            ),
            selectionSpring = MotionSpringConfig(
                dampingRatio = 0.82f,
                stiffness = 500f
            ),
            offsetSnapSpring = MotionSpringConfig(
                dampingRatio = 0.78f,
                stiffness = 420f
            )
        ),
        refraction = BottomBarRefractionMotionSpec(
            movingVelocityThresholdPxPerSecond = 45f,
            speedProgressDivisorPxPerSecond = 1400f,
            dragProgressFloor = 0.18f,
            motionDeadzone = 0.03f,
            panelOffsetMaxDp = 4f
        ),
        indicator = BottomBarIndicatorMotionSpec(
            deformationScaleXDelta = 0.34f,
            deformationScaleYCompressionRatio = 0.52f,
            scaleSpring = MotionSpringConfig(
                dampingRatio = 0.5f,
                stiffness = 600f
            ),
            dragScaleSpring = MotionSpringConfig(
                dampingRatio = 0.6f,
                stiffness = 400f
            ),
            lensVelocityRangePxPerSecond = 2600f,
            railFractionStretchMultiplier = 0.08f,
            capsuleVelocityNormalizationDivisor = 10f,
            capsuleVelocityScaleXMultiplier = 0.75f,
            capsuleVelocityScaleYMultiplier = 0.25f,
            capsuleVelocityClamp = 0.2f
        )
    )
}
