package com.android.purebilibili.navigation3.predictiveback

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import kotlin.math.PI
import kotlin.math.sin
import com.android.purebilibili.core.ui.transition.VideoCardSourceLayout
import com.android.purebilibili.core.ui.transition.VideoHeroMotionSpec
import com.android.purebilibili.core.ui.transition.VideoHeroMotionTokens
import com.android.purebilibili.core.ui.transition.VideoCardTransitionSettleState
import com.android.purebilibili.core.ui.transition.resolveVideoHeroMotionSpec
import com.android.purebilibili.core.ui.transition.resolveVideoHeroLandingScale
import top.yukonga.miuix.kmp.nav.transition.NavMotion
import top.yukonga.miuix.kmp.nav.transition.NavRole
import top.yukonga.miuix.kmp.nav.transition.NavSettleSpec
import top.yukonga.miuix.kmp.nav.transition.NavSwipeEdge
import top.yukonga.miuix.kmp.nav.transition.NavTransition
import top.yukonga.miuix.kmp.nav.transition.NavTransitionScope
import top.yukonga.miuix.kmp.nav.transition.NavSettlePhase

internal enum class MiuixVideoCardContentScale {
    /** Home dual-column / stacked cards: media fills card width, pinned to top. */
    FillWidthTop,
    /** Fullscreen, side-by-side, or square-ish morphs: cover scale about center. */
    CropCenter,
}

/**
 * Pick entry content compensation from the frozen source layout.
 * Side-by-side related/home single-column rows avoid FillWidthTop's vertical stretch
 * (which reads as "whole page crushed into a thin strip").
 */
internal fun resolveMiuixVideoCardContentScaleForSourceLayout(
    sourceLayout: VideoCardSourceLayout,
    fullscreen: Boolean = false,
): MiuixVideoCardContentScale {
    if (fullscreen) return MiuixVideoCardContentScale.CropCenter
    // FillWidthTop for both STACKED and SIDE_BY_SIDE so inverse-scale landing
    // (1/sourceScale) + outer non-uniform scale maps to the frozen card bounds.
    // CropCenter shifts top-left landing anchors and turns horizontal cards into black strips.
    return when (sourceLayout) {
        VideoCardSourceLayout.SIDE_BY_SIDE,
        VideoCardSourceLayout.STACKED,
        VideoCardSourceLayout.COVER_ONLY,
        -> MiuixVideoCardContentScale.FillWidthTop
    }
}

internal data class MiuixVideoCardContentCompensation(
    val scaleX: Float,
    val scaleY: Float,
    val transformOrigin: TransformOrigin,
)

internal data class MiuixVideoCardClipRadii(
    val radiusX: Float,
    val radiusY: Float,
)

internal data class MiuixVideoCardGestureTransform(
    val translationX: Float,
    val translationY: Float,
    val rotationZ: Float,
    val transformOrigin: TransformOrigin,
    val liftScale: Float = 1f,
    val cameraDistance: Float = 8f,
    val shadowElevationDp: Float = 0f,
    val rotationX: Float = 0f,
    val rotationY: Float = 0f,
)

internal const val MIUIX_VIDEO_CARD_GESTURE_HORIZONTAL_TRAVEL_FRACTION = 0.05f
internal const val MIUIX_VIDEO_CARD_GESTURE_VERTICAL_FOLLOW_FRACTION = 0.08f
internal const val MIUIX_VIDEO_CARD_GESTURE_PEEL_ROTATION_DEGREES = 2.4f
internal const val MIUIX_VIDEO_CARD_GESTURE_GRAB_ROTATION_DEGREES = 2.0f
internal const val MIUIX_VIDEO_CARD_GESTURE_LIFT_SCALE = 0.02f
internal const val MIUIX_VIDEO_CARD_GESTURE_CAMERA_DISTANCE_DP = 8f
internal const val MIUIX_VIDEO_CARD_GESTURE_CAMERA_PULL_DP = 1.0f
internal const val MIUIX_VIDEO_CARD_GESTURE_SHADOW_DP = 12f
internal const val MIUIX_VIDEO_CARD_FLOATING_CORNER_DP = 28f

internal fun resolveMiuixVideoCardGesturePoseWeight(morphProgress: Float): Float {
    val morph = morphProgress.coerceIn(0f, 1f)
    if (morph == 0f || morph == 1f) return 0f
    val pull = 1f - morph
    return sin(PI.toFloat() * pull)
}

internal fun resolveMiuixVideoCardGestureTransform(
    morphProgress: Float,
    touchY: Float,
    initialTouchY: Float,
    widthPx: Float,
    heightPx: Float,
    isLeftEdge: Boolean,
    maxVerticalTravelPx: Float,
): MiuixVideoCardGestureTransform {
    val poseWeight = resolveMiuixVideoCardGesturePoseWeight(morphProgress)
    val edgeSign = if (isLeftEdge) 1f else -1f
    val verticalDelta = touchY - initialTouchY
    val safeHeight = heightPx.coerceAtLeast(1f)
    val grabTilt = ((initialTouchY / safeHeight) - 0.5f) * 2f
    val moveTilt = (verticalDelta / (safeHeight * 0.5f)).coerceIn(-1f, 1f)
    val tilt = (grabTilt * 0.65f + moveTilt * 0.35f).coerceIn(-1f, 1f)
    val liveY = (touchY / safeHeight).coerceIn(0.12f, 0.88f)

    return MiuixVideoCardGestureTransform(
        translationX = edgeSign * widthPx.coerceAtLeast(0f) *
            MIUIX_VIDEO_CARD_GESTURE_HORIZONTAL_TRAVEL_FRACTION * poseWeight,
        translationY = (verticalDelta * MIUIX_VIDEO_CARD_GESTURE_VERTICAL_FOLLOW_FRACTION)
            .coerceIn(-maxVerticalTravelPx.coerceAtLeast(0f), maxVerticalTravelPx.coerceAtLeast(0f)) *
            poseWeight,
        rotationZ = edgeSign *
            (MIUIX_VIDEO_CARD_GESTURE_PEEL_ROTATION_DEGREES +
                tilt * MIUIX_VIDEO_CARD_GESTURE_GRAB_ROTATION_DEGREES) *
            poseWeight,
        transformOrigin = TransformOrigin(
            pivotFractionX = if (isLeftEdge) 0.16f else 0.84f,
            pivotFractionY = liveY,
        ),
        liftScale = 1f - MIUIX_VIDEO_CARD_GESTURE_LIFT_SCALE * poseWeight,
        cameraDistance = MIUIX_VIDEO_CARD_GESTURE_CAMERA_DISTANCE_DP -
            MIUIX_VIDEO_CARD_GESTURE_CAMERA_PULL_DP * poseWeight,
        shadowElevationDp = MIUIX_VIDEO_CARD_GESTURE_SHADOW_DP * poseWeight,
    )
}

/**
 * 点击卡片进场时的三维对称变换：与返回手势构成镜像闭环。
 * 倾角根据卡片距屏幕中线的横向相对偏角连续线性加权（向中线平滑衰减归零），避免平板多列及中线卡片突兀晃动。
 * 随 morph (0->1) 升起至中段达到峰值，并在落地全屏时平滑归零。
 */
internal fun resolveMiuixVideoCardClickTransform(
    morphProgress: Float,
    widthPx: Float,
    heightPx: Float,
    sourceBounds: Rect,
): MiuixVideoCardGestureTransform {
    val morph = morphProgress.coerceIn(0f, 1f)
    val poseWeight = sin(PI.toFloat() * morph)
    val cardCenterX = (sourceBounds.left + sourceBounds.right) / 2f
    val screenCenterX = widthPx.coerceAtLeast(1f) / 2f
    // 归一化横向相对偏角：屏幕正中为 0，最左为 -1.0，最右为 +1.0
    val horizontalOffset = if (screenCenterX > 1f) {
        ((cardCenterX - screenCenterX) / screenCenterX).coerceIn(-1f, 1f)
    } else {
        0f
    }
    val screenCenterY = heightPx.coerceAtLeast(1f) / 2f
    val cardCenterY = (sourceBounds.top + sourceBounds.bottom) / 2f
    // 归一化纵向相对偏角：屏幕正中为 0，最上为 -1.0，最下为 +1.0
    val verticalOffset = if (screenCenterY > 1f) {
        ((cardCenterY - screenCenterY) / screenCenterY).coerceIn(-1f, 1f)
    } else {
        0f
    }

    return MiuixVideoCardGestureTransform(
        translationX = 0f,
        translationY = 0f,
        // 向屏幕内侧微倾：右侧卡片为负角（逆时针向内微倾），左侧卡片为正角（顺时针向内微倾）
        // 手机双列偏角约为 ±0.5，对应优雅自然的 ±1.6° 倾角；中列卡片偏角为 0，平正如初
        rotationZ = -horizontalOffset * 3.2f * poseWeight,
        // 三维拟物微透视：卡片飞起时根据受力点象限产生微小俯仰与侧倾（±1°~1.5°），落地全屏时平滑归零
        rotationX = -verticalOffset * 1.6f * poseWeight,
        rotationY = horizontalOffset * 1.4f * poseWeight,
        transformOrigin = TransformOrigin(
            pivotFractionX = (0.5f - 0.2f * horizontalOffset).coerceIn(0.2f, 0.8f),
            pivotFractionY = (0.5f - 0.2f * verticalOffset).coerceIn(0.2f, 0.8f),
        ),
        liftScale = 1f - 0.025f * poseWeight,
        cameraDistance = MIUIX_VIDEO_CARD_GESTURE_CAMERA_DISTANCE_DP - 2.5f * poseWeight,
        shadowElevationDp = MIUIX_VIDEO_CARD_GESTURE_SHADOW_DP * poseWeight,
    )
}

internal fun resolveMiuixVideoCardGestureCornerPx(
    sourceCornerPx: Float,
    morphProgress: Float,
    floatingCornerPx: Float,
    fullscreenCornerPx: Float = 0f,
): Float {
    val pull = (1f - morphProgress.coerceIn(0f, 1f)).coerceIn(0f, 1f)
    val pose = resolveMiuixVideoCardGesturePoseWeight(morphProgress)
    val source = sourceCornerPx.coerceAtLeast(0f)
    val floating = floatingCornerPx.coerceAtLeast(source)
    val landed = fullscreenCornerPx.coerceAtLeast(0f) +
        (source - fullscreenCornerPx.coerceAtLeast(0f)) * pull
    return landed + (floating - landed) * pose
}

/** Map a card-local gesture pivot into the fullscreen entry's transform origin. */
internal fun resolveMiuixVideoCardGestureVisualOrigin(
    sourceBounds: Rect,
    layoutWidth: Float,
    layoutHeight: Float,
    morph: Float,
    localOrigin: TransformOrigin,
): TransformOrigin {
    val m = morph.coerceIn(0f, 1f)
    val width = layoutWidth.coerceAtLeast(1f)
    val height = layoutHeight.coerceAtLeast(1f)
    val visualLeft = sourceBounds.left * (1f - m)
    val visualTop = sourceBounds.top * (1f - m)
    val visualWidth = sourceBounds.width + (width - sourceBounds.width) * m
    val visualHeight = sourceBounds.height + (height - sourceBounds.height) * m
    return TransformOrigin(
        pivotFractionX = ((visualLeft + visualWidth * localOrigin.pivotFractionX) / width)
            .coerceIn(0f, 1f),
        pivotFractionY = ((visualTop + visualHeight * localOrigin.pivotFractionY) / height)
            .coerceIn(0f, 1f),
    )
}

/** Top entry depth is 0 at rest and moves toward -1 while returning. */
internal fun resolveMiuixVideoCardDepthProgress(relativeDepth: Float): Float =
    topProgress(relativeDepth)

internal fun resolveMiuixVideoCardOuterScale(sourceScale: Float, depth: Float, landingScale: Float): Float {
    val source = sourceScale.coerceIn(0.05f, 1f)
    return source + (1f - source) * depth.coerceIn(0f, 1f) - source * (1f - landingScale)
}

internal data class MiuixVideoCardInverseScale(
    val scaleX: Float,
    val scaleY: Float,
)

/**
 * Inverse of the current outer morph after FillWidthTop compensation.
 *
 * Frozen `1/sourceScaleX` on both axes is only correct at depth = 0. While the clip is still
 * non-uniform, that frozen inverse makes cover/info occupy the wrong fraction of the card
 * and then jump to the stationary list layout.
 */
internal fun resolveMiuixVideoCardInverseScale(
    sourceScaleX: Float,
    sourceScaleY: Float,
    outerScaleX: Float,
    outerScaleY: Float,
): MiuixVideoCardInverseScale {
    val compensation = resolveMiuixVideoCardContentCompensation(
        outerScaleX = outerScaleX,
        outerScaleY = outerScaleY,
        contentScale = MiuixVideoCardContentScale.FillWidthTop,
    )
    return MiuixVideoCardInverseScale(
        scaleX = (1f / sourceScaleX.coerceAtLeast(0.01f)) / compensation.scaleX.coerceAtLeast(0.01f),
        scaleY = (1f / sourceScaleY.coerceAtLeast(0.01f)) / compensation.scaleY.coerceAtLeast(0.01f),
    )
}

internal fun resolveMiuixVideoCardInverseScaleForDepth(
    sourceScaleX: Float,
    sourceScaleY: Float,
    depth: Float,
    autoReturning: Boolean = false,
): MiuixVideoCardInverseScale {
    val morph = depth.coerceIn(0f, 1f)
    val landingScale = resolveVideoHeroLandingScale(morph, autoReturning)
    return resolveMiuixVideoCardInverseScale(
        sourceScaleX = sourceScaleX,
        sourceScaleY = sourceScaleY,
        outerScaleX = resolveMiuixVideoCardOuterScale(sourceScaleX, morph, landingScale),
        outerScaleY = resolveMiuixVideoCardOuterScale(sourceScaleY, morph, landingScale),
    )
}

/**
 * Keeps the corner circular in screen space while the outer card layer scales non-uniformly.
 * A regular RoundedCornerShape is scaled together with the layer and becomes too small on the
 * compressed axis, which exposes the retained source card near the end of a card return.
 */
internal fun resolveMiuixVideoCardClipRadii(
    sourceCornerPx: Float,
    outerScaleX: Float,
    outerScaleY: Float,
    morphProgress: Float = 0f,
    floatingCornerPx: Float = sourceCornerPx,
    fullscreenCornerPx: Float = 0f,
): MiuixVideoCardClipRadii {
    val physicalRadius = resolveMiuixVideoCardGestureCornerPx(
        sourceCornerPx = sourceCornerPx,
        morphProgress = morphProgress,
        floatingCornerPx = floatingCornerPx,
        fullscreenCornerPx = fullscreenCornerPx,
    )
    return MiuixVideoCardClipRadii(
        radiusX = physicalRadius / outerScaleX.coerceAtLeast(0.01f),
        radiusY = physicalRadius / outerScaleY.coerceAtLeast(0.01f),
    )
}

internal class MiuixVideoCardClipShape(
    var radiusX: Float = 0f,
    var radiusY: Float = 0f,
) : Shape {
    private var lastWidth = -1f
    private var lastHeight = -1f
    private var lastRadiusX = -1f
    private var lastRadiusY = -1f
    private var cachedOutline: Outline? = null

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val rx = radiusX.coerceIn(0f, size.width / 2f)
        val ry = radiusY.coerceIn(0f, size.height / 2f)
        val current = cachedOutline
        if (current != null && lastWidth == size.width && lastHeight == size.height &&
            lastRadiusX == rx && lastRadiusY == ry
        ) {
            return current
        }
        lastWidth = size.width
        lastHeight = size.height
        lastRadiusX = rx
        lastRadiusY = ry
        val outline = Outline.Rounded(
            RoundRect(
                rect = Rect(0f, 0f, size.width, size.height),
                cornerRadius = CornerRadius(
                    x = rx,
                    y = ry,
                ),
            ),
        )
        cachedOutline = outline
        return outline
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MiuixVideoCardClipShape) return false
        return radiusX == other.radiusX && radiusY == other.radiusY
    }

    override fun hashCode(): Int {
        var result = radiusX.hashCode()
        result = 31 * result + radiusY.hashCode()
        return result
    }
}

internal fun resolveMiuixVideoCardContentCompensation(
    outerScaleX: Float,
    outerScaleY: Float,
    contentScale: MiuixVideoCardContentScale,
): MiuixVideoCardContentCompensation {
    val safeOuterScaleX = outerScaleX.coerceAtLeast(0.01f)
    val safeOuterScaleY = outerScaleY.coerceAtLeast(0.01f)
    val uniformScale = when (contentScale) {
        MiuixVideoCardContentScale.FillWidthTop -> safeOuterScaleX
        MiuixVideoCardContentScale.CropCenter -> maxOf(safeOuterScaleX, safeOuterScaleY)
    }
    return MiuixVideoCardContentCompensation(
        scaleX = uniformScale / safeOuterScaleX,
        scaleY = uniformScale / safeOuterScaleY,
        transformOrigin = when (contentScale) {
            MiuixVideoCardContentScale.FillWidthTop -> TransformOrigin(0.5f, 0f)
            MiuixVideoCardContentScale.CropCenter -> TransformOrigin.Center
        },
    )
}

/** Deferred bridge to the top video entry's live Miuix driver. */
internal class MiuixVideoCardTransitionProgress {
    private var topScope: NavTransitionScope? by mutableStateOf(null)

    fun bind(scope: NavTransitionScope) {
        when (scope.role) {
            NavRole.Incoming,
            NavRole.Outgoing,
            -> topScope = scope
            NavRole.Top -> if (topScope == null || topScope?.role == NavRole.Covered) {
                topScope = scope
            }
            NavRole.Covered -> Unit
        }
    }

    /** Observe the ordinary slide too, so related source metadata survives its entire pop. */
    fun observe(transition: NavTransition): NavTransition = object : NavTransition by transition {
        override fun Modifier.transformEntry(scope: NavTransitionScope): Modifier {
            bind(scope)
            return with(transition) { this@transformEntry.transformEntry(scope) }
        }
    }

    fun clear() {
        topScope = null
    }

    fun depthOr(fallback: Float): Float = topScope
        ?.let { resolveMiuixVideoCardDepthProgress(it.relativeDepth) }
        ?: fallback.coerceIn(0f, 1f)

    // Miuix retains gesture metadata while settling. It is NOT still direct manipulation.
    fun isGestureInProgress(): Boolean = topScope?.let {
        it.gesture != null && it.settle == null
    } == true

    fun depthOrNull(): Float? = topScope?.let {
        resolveMiuixVideoCardDepthProgress(it.relativeDepth)
    }

    fun releaseVelocity(): Float = topScope?.settle?.releaseVelocity ?: 0f

    fun settleStateOrNull(): VideoCardTransitionSettleState? = topScope?.let { scope ->
        when {
            scope.settle?.phase == NavSettlePhase.Cancel -> VideoCardTransitionSettleState.CancelRestore
            isGestureInProgress() -> VideoCardTransitionSettleState.InteractiveSeek
            scope.role == NavRole.Outgoing && scope.relativeDepth <= -1f -> VideoCardTransitionSettleState.Idle
            scope.settle?.phase == NavSettlePhase.Commit || scope.role == NavRole.Outgoing ->
                VideoCardTransitionSettleState.AutoReturn
            scope.role == NavRole.Incoming -> VideoCardTransitionSettleState.AutoEnter
            else -> VideoCardTransitionSettleState.Held
        }
    }

    /**
     * Host layout width used by outer morph (`bounds.width / layoutSize.width`).
     * Landing inverse scale must use the same width or land size drifts from the list card.
     */
    fun layoutWidthOr(fallback: Float): Float {
        val w = topScope?.layoutSize?.width?.toFloat() ?: return fallback.coerceAtLeast(1f)
        return w.coerceAtLeast(1f)
    }

    /**
     * Host layout height used by outer morph (`bounds.height / layoutSize.height`).
     * Inverse Y must use this or stacked cover/info drift off the frozen card.
     */
    fun layoutHeightOr(fallback: Float): Float {
        val h = topScope?.layoutSize?.height?.toFloat() ?: return fallback.coerceAtLeast(1f)
        return h.coerceAtLeast(1f)
    }

    /**
     * 预测返回手势进度（0=开始 → 1=完全提交），无手势时为 null。
     * 供预测返回背景模糊（predictiveBackBackgroundEffect）随手势与落地动画平滑消退，避免松手瞬间断档闪烁。
     */
    fun gestureBackProgress(): Float? = topScope?.let { scope ->
        if (scope.gesture != null || scope.settle != null) {
            val morph = resolveMiuixVideoCardDepthProgress(scope.relativeDepth)
            (1f - morph).coerceIn(0f, 1f)
        } else {
            null
        }
    }
}

internal fun resolveVideoHeroNavMotion(spec: VideoHeroMotionSpec, returning: Boolean): NavMotion = NavMotion(
    // Unlike Tween, Spring consumes the existing driver's release velocity and partial position.
    // No generic fallback spring is now needed for interrupted programmatic transitions.
    commit = NavSettleSpec.Spring(
        dampingRatio = VideoHeroMotionTokens.SPRING_DAMPING,
        stiffness = spec.commitStiffness,
    ),
    cancel = NavSettleSpec.Spring(
        dampingRatio = VideoHeroMotionTokens.SPRING_DAMPING,
        stiffness = spec.cancelStiffness,
    ),
    programmatic = NavSettleSpec.Tween(
        durationMillis = if (returning) spec.returnDurationMillis else spec.enterDurationMillis,
        easing = if (returning) spec.returnSpatialSpec else spec.enterSpatialSpec,
    ),
)

/**
 * Video-card morph authored directly against Miuix's shared navigation driver.
 *
 * The video entry is transformed from the click-time card rectangle to the navigation host. The
 * same [NavTransitionScope.relativeDepth] drives push, programmatic pop, predictive back, commit,
 * and cancellation, so there is no AndroidX Navigation3 or AnimatedVisibility compatibility path.
 */
internal fun miuixVideoCardNavTransition(
    sourceBounds: Rect?,
    sourceCornerDp: Int?,
    durationMillis: Int,
    fallback: NavTransition,
    progress: MiuixVideoCardTransitionProgress,
    contentScale: MiuixVideoCardContentScale = MiuixVideoCardContentScale.FillWidthTop,
    gestureFollowEnabled: Boolean = true,
    heroMotionSpec: VideoHeroMotionSpec = resolveVideoHeroMotionSpec(durationMillis),
    returningProvider: () -> Boolean = { false },
    deviceCornerDp: Dp = 32.dp,
): NavTransition {
    val bounds = sourceBounds?.takeIf { it.width > 1f && it.height > 1f }
        ?: return fallback
    val enterMotion = resolveVideoHeroNavMotion(heroMotionSpec, returning = false)
    val returnMotion = resolveVideoHeroNavMotion(heroMotionSpec, returning = true)
    val corner = sourceCornerDp?.coerceAtLeast(0) ?: 16
    val clipShapeA = MiuixVideoCardClipShape()
    val clipShapeB = MiuixVideoCardClipShape()
    var useShapeA = true

    return object : NavTransition {
        override val opaqueDepth: Float = fallback.opaqueDepth
        override val motion: NavMotion get() = if (returningProvider()) returnMotion else enterMotion

        // Source-page scrim and blur are rendered by the existing depth layer from this same
        // transition's deferred progress. Do not add Miuix's generic dim on top of it.
        override fun scrimFraction(scope: NavTransitionScope): Float = 0f

        override fun Modifier.transformEntry(scope: NavTransitionScope): Modifier {
            progress.bind(scope)
            val floatingCornerPx = with(scope.density) {
                val devicePx = deviceCornerDp.toPx()
                val floatingCornerPx = MIUIX_VIDEO_CARD_FLOATING_CORNER_DP.dp.toPx()
                    .coerceAtLeast(devicePx)
                floatingCornerPx
            }
            val gestureModifier = if (gestureFollowEnabled) {
                this.graphicsLayer {
                    val depth = scope.relativeDepth
                    val gesture = scope.gesture
                    if (depth <= 0f) {
                        val width = scope.layoutSize.width.toFloat().coerceAtLeast(1f)
                        val height = scope.layoutSize.height.toFloat().coerceAtLeast(1f)
                        val morph = resolveMiuixVideoCardDepthProgress(depth)
                        val transform = if (gesture != null) {
                            resolveMiuixVideoCardGestureTransform(
                                morphProgress = morph,
                                touchY = gesture.touchY,
                                initialTouchY = gesture.initialTouchY,
                                widthPx = width,
                                heightPx = height,
                                isLeftEdge = gesture.swipeEdge == NavSwipeEdge.Left,
                                maxVerticalTravelPx = 56.dp.toPx(),
                            )
                        } else {
                            resolveMiuixVideoCardClickTransform(
                                morphProgress = morph,
                                widthPx = width,
                                heightPx = height,
                                sourceBounds = bounds,
                            )
                        }
                        transformOrigin = resolveMiuixVideoCardGestureVisualOrigin(
                            sourceBounds = bounds,
                            layoutWidth = width,
                            layoutHeight = height,
                            morph = morph,
                            localOrigin = transform.transformOrigin,
                        )
                        translationX = transform.translationX
                        translationY = transform.translationY
                        rotationZ = transform.rotationZ
                        rotationX = transform.rotationX
                        rotationY = transform.rotationY
                        scaleX = transform.liftScale
                        scaleY = transform.liftScale
                        cameraDistance = transform.cameraDistance
                    }
                }
            } else {
                this
            }
            return gestureModifier
                .graphicsLayer {
                    val width = scope.layoutSize.width.toFloat().coerceAtLeast(1f)
                    val height = scope.layoutSize.height.toFloat().coerceAtLeast(1f)
                    val depth = scope.relativeDepth
                    if (depth <= 0f) {
                        val morph = resolveMiuixVideoCardDepthProgress(depth)
                        val sourceScaleX = (bounds.width / width).coerceIn(0.05f, 1f)
                        val sourceScaleY = (bounds.height / height).coerceIn(0.05f, 1f)
                        val landingScale = resolveVideoHeroLandingScale(
                            depth = morph,
                            autoReturning = !heroMotionSpec.reducedMotion &&
                                scope.settle != null && scope.settle?.phase != NavSettlePhase.Cancel &&
                                scope.role == NavRole.Outgoing,
                        )
                        val outerScaleX = resolveMiuixVideoCardOuterScale(sourceScaleX, morph, landingScale)
                        val outerScaleY = resolveMiuixVideoCardOuterScale(sourceScaleY, morph, landingScale)
                        scaleX = outerScaleX
                        scaleY = outerScaleY
                        transformOrigin = TransformOrigin(0f, 0f)
                        val remaining = 1f - morph
                        // 微弧线轨迹（Arc Motion）：卡片左右飞向全屏时，横向轨迹微向屏幕中心弯曲，
                        // 赋予纸片或卡片飞入时的自然物理力矩感，且在 morph=0 与 morph=1 时严格归零。
                        val arcWeight = sin(PI.toFloat() * morph) * remaining
                        val cardCenterX = (bounds.left + bounds.right) / 2f
                        val screenCenterX = width / 2f
                        val arcSign = if (cardCenterX < screenCenterX) 1f else -1f
                        val arcOffsetPx = arcSign * with(scope.density) { 10.dp.toPx() } * arcWeight
                        translationX = bounds.left.coerceIn(-width, width) * remaining + arcOffsetPx
                        translationY = bounds.top.coerceIn(-height, height) * remaining
                        // Keep the complete flying entry opaque. The source card and the detail entry
                        // already share the same geometry driver; an entry-level alpha handoff would
                        // expose the player's black Surface frame at landing.
                        alpha = 1f
                        val poseWeight = resolveMiuixVideoCardGesturePoseWeight(morph)
                        clip = morph < 0.999f || poseWeight > 0.001f || (gestureFollowEnabled && scope.gesture != null)
                        val physicalRadius = resolveMiuixVideoCardGestureCornerPx(
                            sourceCornerPx = corner.dp.toPx(),
                            morphProgress = morph,
                            floatingCornerPx = floatingCornerPx,
                            // Fullscreen content must reach square host bounds, including on tablets.
                            fullscreenCornerPx = 0f,
                        )
                        val radX = physicalRadius / outerScaleX.coerceAtLeast(0.01f)
                        val radY = physicalRadius / outerScaleY.coerceAtLeast(0.01f)
                        val activeShape = if (useShapeA) clipShapeA else clipShapeB
                        if (kotlin.math.abs(activeShape.radiusX - radX) > 0.05f || kotlin.math.abs(activeShape.radiusY - radY) > 0.05f) {
                            useShapeA = !useShapeA
                            val nextShape = if (useShapeA) clipShapeA else clipShapeB
                            nextShape.radiusX = radX
                            nextShape.radiusY = radY
                            shape = nextShape
                        } else {
                            shape = activeShape
                        }
                        if (gestureFollowEnabled) {
                            // Click and back poses share this envelope. Reuse it instead of
                            // allocating and resolving the full gesture transform a second time.
                            shadowElevation = MIUIX_VIDEO_CARD_GESTURE_SHADOW_DP.dp.toPx() * poseWeight
                        }
                    }
                }.graphicsLayer {
                    val depth = scope.relativeDepth
                    if (depth <= 0f) {
                        val width = scope.layoutSize.width.toFloat().coerceAtLeast(1f)
                        val height = scope.layoutSize.height.toFloat().coerceAtLeast(1f)
                        val morph = resolveMiuixVideoCardDepthProgress(depth)
                        val landingScale = resolveVideoHeroLandingScale(
                            depth = morph,
                            autoReturning = !heroMotionSpec.reducedMotion &&
                                scope.settle != null && scope.settle?.phase != NavSettlePhase.Cancel &&
                                scope.role == NavRole.Outgoing,
                        )
                        val outerScaleX = resolveMiuixVideoCardOuterScale(bounds.width / width, morph, landingScale)
                        val outerScaleY = resolveMiuixVideoCardOuterScale(bounds.height / height, morph, landingScale)
                        val safeOuterScaleX = outerScaleX.coerceAtLeast(0.01f)
                        val safeOuterScaleY = outerScaleY.coerceAtLeast(0.01f)
                        val uniformScale = when (contentScale) {
                            MiuixVideoCardContentScale.FillWidthTop -> safeOuterScaleX
                            MiuixVideoCardContentScale.CropCenter -> maxOf(safeOuterScaleX, safeOuterScaleY)
                        }
                        scaleX = uniformScale / safeOuterScaleX
                        scaleY = uniformScale / safeOuterScaleY
                        transformOrigin = when (contentScale) {
                            MiuixVideoCardContentScale.FillWidthTop -> TransformOrigin(0.5f, 0f)
                            MiuixVideoCardContentScale.CropCenter -> TransformOrigin.Center
                        }
                    }
                }.zIndex(1f)
        }
    }
}
