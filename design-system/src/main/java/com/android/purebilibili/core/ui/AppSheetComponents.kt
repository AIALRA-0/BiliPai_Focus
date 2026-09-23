package com.android.purebilibili.core.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.android.purebilibili.core.theme.AppUiStyle
import com.android.purebilibili.core.theme.LocalAppUiStyle
import com.android.purebilibili.core.theme.resolveAndroidNativeChromeTokens
import com.android.purebilibili.core.ui.motion.AppMotionTokens

data class AdaptiveBottomSheetVisualSpec(
    val cornerRadiusDp: Int,
    val useMaterialDragHandle: Boolean
)

enum class AppModalPresentation {
    BottomSheet,
    CenteredDialog,
}

data class AppModalLayoutSpec(
    val presentation: AppModalPresentation,
    val maxWidthDp: Int,
    val maxHeightFraction: Float,
)

fun resolveAppModalLayoutSpec(
    windowWidthDp: Int,
    miuixNonGlass: Boolean = false,
): AppModalLayoutSpec = when {
    windowWidthDp < 600 -> AppModalLayoutSpec(
        presentation = AppModalPresentation.BottomSheet,
        maxWidthDp = windowWidthDp,
        maxHeightFraction = 1f,
    )
    miuixNonGlass -> AppModalLayoutSpec(
        presentation = AppModalPresentation.CenteredDialog,
        maxWidthDp = 420,
        maxHeightFraction = 0.86f,
    )
    windowWidthDp < 1200 -> AppModalLayoutSpec(
        presentation = AppModalPresentation.CenteredDialog,
        maxWidthDp = 640,
        maxHeightFraction = 0.86f,
    )
    else -> AppModalLayoutSpec(
        presentation = AppModalPresentation.CenteredDialog,
        maxWidthDp = 720,
        maxHeightFraction = 0.86f,
    )
}

internal data class AdaptiveBottomSheetMotionSpec(
    val scrimEnterDurationMillis: Int,
    val scrimExitDurationMillis: Int,
    val contentEnterFadeDurationMillis: Int,
    val contentExitFadeDurationMillis: Int
)

fun resolveAdaptiveBottomSheetVisualSpec(
    uiStyle: AppUiStyle,
    miuixNonGlass: Boolean = false,
): AdaptiveBottomSheetVisualSpec {
    val level = if (miuixNonGlass) ContainerLevel.Sheet else ContainerLevel.Pill
    val cornerRadiusDp = AppShapes.resolveContainerCornerDp(
        level = level,
        uiStyle = uiStyle,
        liquidGlassEnabled = !miuixNonGlass,
    ).value.toInt()
    return AdaptiveBottomSheetVisualSpec(
        cornerRadiusDp = cornerRadiusDp,
        useMaterialDragHandle = true,
    )
}

internal fun resolveAdaptiveBottomSheetMotionSpec(
    uiStyle: AppUiStyle,
): AdaptiveBottomSheetMotionSpec {
    val tokens = resolveAndroidNativeChromeTokens(uiStyle)
    return AdaptiveBottomSheetMotionSpec(
        scrimEnterDurationMillis = tokens.motionEmphasizedMillis,
        scrimExitDurationMillis = tokens.expressiveMotionDurationMillis,
        contentEnterFadeDurationMillis = tokens.motionEmphasizedMillis,
        contentExitFadeDurationMillis = tokens.expressiveMotionDurationMillis
    )
}

/**
 * 弹层宿主契约：App 风格对应的 BottomSheet 宿主实现。
 *
 * [MIUIX_OVERLAY] 对应 Miuix OverlayBottomSheet，[MATERIAL3] 对应 Material3
 * ModalBottomSheet。两者不仅外观不同，弹层宿主也不同：OverlayBottomSheet 依赖
 * Miuix overlay popup host（仅 AdaptiveScaffold 的 MIUIX 模式挂载，
 * 见 [resolveAdaptiveScaffoldRenderer]），直接替换会导致无 popup host 的页面
 * 点击无效或弹层不显示 —— 不允许机械替换。业务页不允许自行判断宿主，宿主感知
 * 场景（如筛选弹层）必须消费 [resolveBottomSheetHost]，而不是复制判断逻辑。
 */
enum class BottomSheetHost {
    /** Miuix OverlayBottomSheet：依赖 Miuix overlay popup host。 */
    MIUIX_OVERLAY,

    /** Material3 ModalBottomSheet：任意宿主下可用。 */
    MATERIAL3,
}

fun resolveBottomSheetHost(
    uiStyle: AppUiStyle
): BottomSheetHost = when (uiStyle) {
    AppUiStyle.MIUIX -> BottomSheetHost.MIUIX_OVERLAY
    AppUiStyle.MATERIAL3 -> BottomSheetHost.MATERIAL3
}

internal fun bottomSheetScrimEnterTransition(
    uiStyle: AppUiStyle,
): EnterTransition = fadeIn(
    AppMotionTokens.resolveBottomSheetFadeEnterSpec(uiStyle)
)

internal fun bottomSheetScrimExitTransition(
    uiStyle: AppUiStyle,
): ExitTransition = fadeOut(
    AppMotionTokens.resolveBottomSheetFadeExitSpec(uiStyle)
)

internal fun bottomSheetContentEnterTransition(
    uiStyle: AppUiStyle,
): EnterTransition {
    return slideInVertically(
        initialOffsetY = { it },
        animationSpec = AppMotionTokens.resolveBottomSheetSlideSpec(uiStyle)
    ) + fadeIn(
        AppMotionTokens.resolveBottomSheetFadeEnterSpec(uiStyle)
    )
}

internal fun bottomSheetContentExitTransition(
    uiStyle: AppUiStyle,
): ExitTransition {
    return slideOutVertically(
        targetOffsetY = { it },
        animationSpec = AppMotionTokens.resolveBottomSheetSlideExitSpec(uiStyle)
    ) + fadeOut(
        AppMotionTokens.resolveBottomSheetFadeExitSpec<Float>(uiStyle)
    )
}

/**
 * App 通用模态弹层 facade。紧凑窗口使用底部弹层，Medium 及以上使用限宽居中弹层。
 *
 * 使用 Material3 ModalBottomSheet 作为中性宿主：即使宿主契约
 * （[resolveBottomSheetHost]）在 MIUIX 下解析为 [BottomSheetHost.MIUIX_OVERLAY]，
 * 本 facade 也不做机械替换 —— OverlayBottomSheet 依赖 Miuix overlay popup host
 * （仅 AdaptiveScaffold 的 MIUIX 模式挂载），而本 facade 的调用点无法保证处于该
 * 宿主之下。需要 Miuix overlay 宿主的场景由宿主感知 facade 消费
 * [resolveBottomSheetHost]。两值风格在此仅做视觉区分（容器色、圆角、拖拽条、动效）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    shape: Shape? = null,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    tonalElevation: Dp = 0.dp,
    scrimColor: Color = BottomSheetDefaults.ScrimColor,
    presentationProgress: Float = 1f,
    dismissOnBackPress: Boolean = true,
    dragHandle: @Composable (() -> Unit)? = null,
    windowInsets: androidx.compose.foundation.layout.WindowInsets = androidx.compose.material3.BottomSheetDefaults.modalWindowInsets,
    content: @Composable ColumnScope.() -> Unit
) {
    val uiStyle = LocalAppUiStyle.current
    val resolvedDragHandle = dragHandle ?: when (uiStyle) {
        AppUiStyle.MIUIX -> { { AppBottomSheetDragHandle() } }
        AppUiStyle.MATERIAL3 -> { { BottomSheetDefaults.DragHandle() } }
    }
    val miuixNonGlass = isMiuixNonGlassEnabled()
    val configuration = LocalConfiguration.current
    val layoutSpec = remember(configuration.screenWidthDp, miuixNonGlass) {
        resolveAppModalLayoutSpec(
            windowWidthDp = configuration.screenWidthDp,
            miuixNonGlass = miuixNonGlass,
        )
    }
    val visualSpec = remember(uiStyle, miuixNonGlass) {
        resolveAdaptiveBottomSheetVisualSpec(
            uiStyle = uiStyle,
            miuixNonGlass = miuixNonGlass,
        )
    }
    val adaptiveSheetShape = remember(visualSpec) {
        RoundedCornerShape(
            topStart = visualSpec.cornerRadiusDp.dp,
            topEnd = visualSpec.cornerRadiusDp.dp,
        )
    }
    val sheetShape = shape ?: adaptiveSheetShape
    val centeredSheetShape = shape ?: RoundedCornerShape(visualSpec.cornerRadiusDp.dp)
    val progressVisual = resolveInteractiveOverlayProgressVisual(
        presentationProgress = presentationProgress,
        surfaceType = InteractiveOverlaySurfaceType.BOTTOM_SHEET,
        blurActive = !miuixNonGlass,
        maxScrimAlpha = scrimColor.alpha
    )
    val resolvedContainerColor = when (uiStyle) {
        AppUiStyle.MIUIX -> MaterialTheme.colorScheme.surfaceContainer
        AppUiStyle.MATERIAL3 -> MaterialTheme.colorScheme.surfaceContainerLow
    }.let { color ->
        color.copy(alpha = color.alpha * progressVisual.surfaceAlphaMultiplier)
    }
    if (layoutSpec.presentation == AppModalPresentation.CenteredDialog) {
        Dialog(
            onDismissRequest = onDismissRequest,
            properties = DialogProperties(
                dismissOnBackPress = dismissOnBackPress,
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                AppPopupSurface(
                    type = AppPopupSurfaceType.SHEET,
                    modifier = Modifier
                        .widthIn(max = layoutSpec.maxWidthDp.dp)
                        .heightIn(
                            max = (configuration.screenHeightDp *
                                layoutSpec.maxHeightFraction).dp
                        )
                        .then(modifier)
                        .fillMaxWidth(),
                    shape = centeredSheetShape,
                    containerColor = resolvedContainerColor,
                    contentColor = contentColor,
                    tonalElevation = tonalElevation,
                ) {
                    Column(content = content)
                }
            }
        }
        return
    }
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        properties = ModalBottomSheetProperties(shouldDismissOnBackPress = dismissOnBackPress),
        shape = sheetShape,
        containerColor = Color.Transparent,
        contentColor = contentColor,
        tonalElevation = tonalElevation,
        scrimColor = scrimColor.copy(alpha = progressVisual.scrimAlpha),
        dragHandle = null,
        contentWindowInsets = { windowInsets },
        content = {
            AppPopupSurface(
                type = AppPopupSurfaceType.SHEET,
                modifier = Modifier.fillMaxWidth(),
                shape = sheetShape,
                containerColor = resolvedContainerColor,
                contentColor = contentColor,
                tonalElevation = tonalElevation,
            ) {
                Column {
                    resolvedDragHandle()
                    content()
                }
            }
        }
    )
}

data class AppBottomSheetMotion(
    val scrimEnter: EnterTransition,
    val scrimExit: ExitTransition,
    val contentEnter: EnterTransition,
    val contentExit: ExitTransition,
    val scrimEnterDurationMillis: Int,
    val scrimExitDurationMillis: Int,
    val contentEnterFadeDurationMillis: Int,
    val contentExitFadeDurationMillis: Int,
)

@Composable
fun rememberAppBottomSheetMotion(): AppBottomSheetMotion {
    val uiStyle = LocalAppUiStyle.current
    return remember(uiStyle) {
        val motionSpec = resolveAdaptiveBottomSheetMotionSpec(uiStyle)
        AppBottomSheetMotion(
            scrimEnter = bottomSheetScrimEnterTransition(uiStyle),
            scrimExit = bottomSheetScrimExitTransition(uiStyle),
            contentEnter = bottomSheetContentEnterTransition(uiStyle),
            contentExit = bottomSheetContentExitTransition(uiStyle),
            scrimEnterDurationMillis = motionSpec.scrimEnterDurationMillis,
            scrimExitDurationMillis = motionSpec.scrimExitDurationMillis,
            contentEnterFadeDurationMillis = motionSpec.contentEnterFadeDurationMillis,
            contentExitFadeDurationMillis = motionSpec.contentExitFadeDurationMillis,
        )
    }
}

@Composable
fun AppBottomSheetDragHandle() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(5.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
    }
}
