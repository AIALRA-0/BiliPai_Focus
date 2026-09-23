package com.android.purebilibili.core.ui

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CommentOverlayNavigationBackHandlerStructureTest {

    @Test
    fun videoCommentOverlayUsesNavigationEventBackHandling() {
        val source = File(
            "src/main/java/com/android/purebilibili/feature/video/ui/components/VideoCommentSheetHost.kt"
        ).readText()

        assertTrue(source.contains("NavigationBackHandler("))
        assertTrue(source.contains("isBackEnabled = hostVisible"))
        assertTrue(source.contains("resolveVideoCommentPredictiveBackTarget("))
        assertTrue(source.contains("threadBackProgress"))
        assertTrue(source.contains("resolveCommentThreadPredictiveBackOffsetY("))
        assertTrue(source.contains("slideInVertically("))
        assertTrue(source.contains("resolvePredictiveBackBlurFrame("))
        assertFalse(source.contains("translationX = threadBackProgress"))
        assertFalse(source.contains("import androidx.activity.compose.BackHandler"))
    }

    @Test
    fun dynamicCommentSheetsUseNavigationEventBackHandling() {
        val mainSheetSource = File(
            "src/main/java/com/android/purebilibili/feature/dynamic/components/DynamicCommentSheet.kt"
        ).readText()
        val subReplySource = File(
            "src/main/java/com/android/purebilibili/feature/video/ui/components/SubReplySheet.kt"
        ).readText()

        assertTrue(mainSheetSource.contains("CommentWindowNavigation {"))
        assertTrue(mainSheetSource.contains("NavigationBackHandler("))
        assertTrue(mainSheetSource.contains("resolveVideoCommentPredictiveBackTarget("))
        assertTrue(mainSheetSource.contains("threadBackProgress"))
        assertTrue(mainSheetSource.contains("resolveCommentThreadPredictiveBackOffsetY("))
        assertTrue(mainSheetSource.contains("slideInVertically("))
        assertTrue(mainSheetSource.contains("SubReplyDetailContent("))
        assertTrue(subReplySource.contains("CommentWindowNavigation {"))
        assertTrue(subReplySource.contains("NavigationBackHandler("))
        assertTrue(subReplySource.contains("transition?.latestEvent?.progress"))
        assertTrue(subReplySource.contains("resolveCommentThreadPredictiveBackOffsetY(backProgress, size.height)"))
        assertTrue(subReplySource.contains("rememberCommentThreadDrag("))
        assertTrue(subReplySource.contains("dismissOnBackPress = false"))
        assertTrue(mainSheetSource.contains("dismissOnBackPress = false"))
    }
    @Test
    fun commentWindowUsesItsOwnDispatcherAndDisablesDefaultDismissal() {
        val window = File("src/main/java/com/android/purebilibili/core/ui/CommentWindowNavigation.kt").readText()
        val facade = File("../design-system/src/main/java/com/android/purebilibili/core/ui/AppSheetComponents.kt").readText()
        assertTrue(window.contains("LocalView.current.findViewTreeNavigationEventDispatcherOwner()"))
        assertTrue(window.contains("LocalNavigationEventDispatcherOwner provides owner"))
        assertTrue(facade.contains("ModalBottomSheetProperties(shouldDismissOnBackPress = dismissOnBackPress)"))
        assertTrue(facade.contains("dismissOnBackPress = dismissOnBackPress"))
        val detail = File("src/main/java/com/android/purebilibili/feature/dynamic/DynamicDetailScreen.kt").readText()
        val preview = File("src/main/java/com/android/purebilibili/feature/dynamic/components/DynamicSubReplyPreviewHost.kt").readText()
        assertTrue(detail.contains("DynamicSubReplyPreviewHost("))
        assertTrue(preview.contains("SubReplySheet("))
    }

}
