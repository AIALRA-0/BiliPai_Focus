package com.android.purebilibili.feature.dynamic

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DynamicNeutralUiStructureTest {

    private val sourceRoot = File("src/main/java/com/android/purebilibili/feature/dynamic")

    @Test
    fun `dynamic feature does not read legacy style state`() {
        val source = sourceRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .joinToString("\n") { it.readText() }

        assertFalse(source.contains("UiPreset"))
        assertFalse(source.contains("AndroidNativeVariant"))
        assertFalse(source.contains("LocalUiPreset"))
        assertFalse(source.contains("LocalAndroidNativeVariant"))
        assertFalse(source.contains("LocalUiStyle"))
    }

    @Test
    fun `dynamic cards consume neutral surface values without vendor branching`() {
        val source = File(sourceRoot, "components/DynamicComponents.kt").readText()

        assertTrue(source.contains("rememberContentCardSurfaceSpec()"))
        assertTrue(source.contains("AppSurfaceTokens.surfaceContainer()"))
        assertTrue(source.contains("AppSurfaceTokens.surface()"))
        assertTrue(source.contains("AppSurfaceTokens.divider()"))
        assertFalse(source.contains("useMiuixTokens"))
    }

    @Test
    fun `dynamic overlays use neutral dialog and sheet entry points`() {
        val cardSource = File(sourceRoot, "components/DynamicCard.kt").readText()
        val commentSource = File(sourceRoot, "components/DynamicCommentSheet.kt").readText()

        assertTrue(cardSource.contains("AppAlertDialog("))
        assertFalse(Regex("(?m)^\\s*AlertDialog\\(").containsMatchIn(cardSource))
        assertTrue(commentSource.contains("AppModalBottomSheet("))
        assertFalse(commentSource.contains("IOSModalBottomSheet("))
        assertTrue(commentSource.contains("val useMiuixNonGlassInput = isMiuixNonGlassEnabled()"))
        assertTrue(commentSource.contains("AppOutlinedTextField("))
        assertTrue(commentSource.contains("OutlinedTextField("))
        assertTrue(commentSource.contains("val commentFieldContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh"))
        assertTrue(commentSource.contains("focusedContainerColor = fieldColor"))
        assertTrue(commentSource.contains("focusedBorderColor = Color.Transparent"))
        assertTrue(commentSource.contains("val dockShape = resolveSharedBottomBarCapsuleShape()"))
        assertTrue(commentSource.contains("shape = dockShape"))
        assertFalse(commentSource.contains("shape = AppShapes.container(ContainerLevel.Sheet)"))
    }

    @Test
    fun `dynamic empty state does not use the telegram mascot animation`() {
        val source = File(sourceRoot, "DynamicScreen.kt").readText()

        assertTrue(source.contains("DynamicEmptyState("))
        assertFalse(source.contains("com.android.purebilibili.core.ui.EmptyState"))
        assertFalse(Regex("(?m)^\\s*EmptyState\\(").containsMatchIn(source))
    }

    @Test
    fun `dynamic primary actions select their product semantic icons inside the component`() {
        val actionSource = File(sourceRoot, "components/ActionButton.kt").readText()
        val cardSource = File(sourceRoot, "components/DynamicCard.kt").readText()

        assertTrue(actionSource.contains("isForward -> Icons.Outlined.IosShare"))
        assertTrue(actionSource.contains("isComment -> Icons.Outlined.Sms"))
        assertTrue(actionSource.contains("rememberAppLikeIcon()"))
        assertTrue(actionSource.contains("rememberAppLikeFilledIcon()"))
        assertFalse(cardSource.contains("icon = io.github.alexzhirkevich.cupertino"))
    }

    @Test
    fun `dynamic native action buttons keep labels in full width touch targets`() {
        val actionSource = File(sourceRoot, "components/ActionButton.kt").readText()

        assertTrue(actionSource.contains("MiuixButton("))
        assertTrue(actionSource.contains("if (LocalAppUiStyle.current == AppUiStyle.MIUIX)"))
        assertTrue(actionSource.contains("insideMargin = PaddingValues("))
        assertTrue(actionSource.contains("horizontal = AppSpacingTokens.Small"))
        assertTrue(actionSource.contains("if (LocalAppUiStyle.current == AppUiStyle.MATERIAL3)"))
        assertTrue(actionSource.contains(".heightIn(min = AppChromeSizeTokens.MinimumTouchTarget)"))
        assertTrue(actionSource.contains("DynamicNativeActionText("))
        assertTrue(actionSource.contains("softWrap = false"))
        assertTrue(actionSource.contains("overflow = TextOverflow.Ellipsis"))
        assertFalse(actionSource.contains("FilledTonalButton("))
        assertFalse(actionSource.contains("shape = AppShapes.container(ContainerLevel.Card)"))
    }

    @Test
    fun `dynamic card overflow uses the miuix window action menu`() {
        val cardSource = File(sourceRoot, "components/DynamicCard.kt").readText()

        assertTrue(cardSource.contains("AppWindowActionMenu("))
        assertTrue(cardSource.contains("label = \"复制链接\""))
        assertTrue(cardSource.contains("label = \"分享动态\""))
        assertTrue(cardSource.contains("label = \"屏蔽该 UP 主\""))
        assertFalse(cardSource.contains("AppDropdownMenu("))
        assertFalse(cardSource.contains("AppDropdownMenuItem("))
    }

    @Test
    fun `dynamic segmented controls do not depend on another feature renderer`() {
        val commentSource = File(sourceRoot, "components/DynamicCommentSheet.kt").readText()
        val topBarSource = File(sourceRoot, "components/DynamicTopBar.kt").readText()
        val segmentedSource = File(sourceRoot, "components/DynamicAdaptiveSegmentedControl.kt").readText()

        assertTrue(commentSource.contains("DynamicAdaptiveSegmentedControl("))
        assertTrue(commentSource.contains("rememberLayerBackdrop()"))
        assertTrue(commentSource.contains("miuixBackdrop = commentChromeBackdrop"))
        assertTrue(segmentedSource.contains("BottomBarLiquidSegmentedControl("))
        assertTrue(segmentedSource.contains("AppNativeSegmentedControl("))
        assertTrue(segmentedSource.contains("miuixBackdrop = backdrop"))
        assertFalse(commentSource.contains("CommentSegmentedControl("))
        assertFalse(commentSource.contains("feature.video.ui.components.CommentSegmentedControl"))
        assertFalse(topBarSource.contains("AndroidNativeUnderlinedSegmentedControl("))
        assertFalse(topBarSource.contains("feature.home.components.AndroidNativeUnderlinedSegmentedControl"))
    }
}
