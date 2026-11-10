package com.android.purebilibili.feature.video.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.android.purebilibili.core.ui.LocalAppThemeConfig
import com.android.purebilibili.core.ui.rememberAppBottomSheetMotion
import top.yukonga.miuix.kmp.anim.folmeSpring

// Match the cascading menu's near-critically-damped expansion and quicker collapse.
internal fun <T> commentCascadeSpring(expanding: Boolean): SpringSpec<T> =
    if (expanding) folmeSpring(damping = 0.99f, response = 0.45f)
    else folmeSpring(damping = 0.95f, response = 0.2f)

/**
 * One transition owns a comment hierarchy change. Callers supply a stable level key and
 * a render snapshot so outgoing content keeps its own replies and scroll position.
 * This is shared comment motion in both material modes, not liquid-glass-only styling;
 * native sheet/menu layout and their entrance/exit animations remain with their hosts.
 */
@Composable
internal fun <T> CommentCascadingContent(
    targetState: T,
    contentKey: (T) -> Any,
    isDeeper: (T) -> Boolean,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit,
) {
    val motion = rememberAppBottomSheetMotion()
    val enabled = !motion.reducedMotion && LocalAppThemeConfig.current.uiEntranceAnimationEnabled
    AnimatedContent(
        targetState = targetState,
        contentKey = contentKey,
        modifier = modifier.clipToBounds(),
        contentAlignment = Alignment.TopStart,
        transitionSpec = {
            if (!enabled) {
                (EnterTransition.None togetherWith ExitTransition.None)
                    .using(SizeTransform { _, _ -> snap() })
            } else {
                val expanding = isDeeper(targetState)
                val direction = if (expanding) 1 else -1
                val enter = fadeIn(commentCascadeSpring(expanding)) +
                    slideInVertically(commentCascadeSpring(expanding)) { direction * it / 16 } +
                    scaleIn(commentCascadeSpring(expanding), initialScale = 0.98f)
                val exit = fadeOut(commentCascadeSpring(false)) +
                    slideOutVertically(commentCascadeSpring(false)) { -direction * it / 24 } +
                    scaleOut(commentCascadeSpring(false), targetScale = 0.98f)
                (enter togetherWith exit)
                    .using(SizeTransform { _, _ -> commentCascadeSpring(expanding) })
            }
        },
        label = "commentHierarchy",
    ) { page ->
        val active = contentKey(page) == contentKey(targetState)
        Box(
            modifier = if (active) Modifier else Modifier
                .clearAndSetSemantics { }
                .focusProperties { canFocus = false }
                .onPreviewKeyEvent { true }
                .pointerInput(Unit) {
                    // Outgoing replies are retained only for drawing, never for actions.
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                        }
                    }
                },
            propagateMinConstraints = true,
        ) {
            content(page)
        }
    }
}
