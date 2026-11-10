package com.android.purebilibili.core.ui

import com.android.purebilibili.core.theme.AppUiStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppDialogComponentsPolicyTest {

    @Test
    fun `dialog actions stay content sized inside material alert dialogs`() {
        val policy = resolveDialogActionLayoutPolicy()

        assertFalse(policy.expandToContainer)
    }

    @Test
    fun miuixAlertDialogUsesWindowDialogWhenOutsideScaffoldHost() {
        assertEquals(
            AppAlertDialogRenderer.LOCAL_DIALOG,
            resolveAppAlertDialogRenderer(
                uiStyle = AppUiStyle.MIUIX
            )
        )
        val source = java.io.File(
            "src/main/java/com/android/purebilibili/core/ui/AdaptiveDialogComponents.kt"
        ).takeIf { it.exists() } ?: java.io.File(
            "design-system/src/main/java/com/android/purebilibili/core/ui/AdaptiveDialogComponents.kt"
        )
        val dialogSource = source.readText()
        assertTrue(dialogSource.contains("WindowDialog("))
        assertTrue(dialogSource.contains("backgroundColor = Color.Transparent"))
        assertTrue(dialogSource.contains("insideMargin = DpSize(0.dp, 0.dp)"))
        assertTrue(dialogSource.contains("cornerRadius = 0.dp"))
        assertFalse(dialogSource.contains("LocalAppPopupSurfaceRenderer.current"))
    }

    @Test
    fun material3KeepsMaterialAlertDialogRenderer() {
        assertEquals(
            AppAlertDialogRenderer.MATERIAL_ALERT,
            resolveAppAlertDialogRenderer(
                uiStyle = AppUiStyle.MATERIAL3
            )
        )
    }

    @Test
    fun contentDialogLayout_disablesPlatformDefaultWidthAndCapsMaxWidth() {
        val compact = resolveAppCompactContentDialogLayoutPolicy()
        val standard = resolveAppContentDialogLayoutPolicy()
        val expanded = resolveAppExpandedContentDialogLayoutPolicy()

        assertFalse(compact.usePlatformDefaultWidth)
        assertFalse(standard.usePlatformDefaultWidth)
        assertFalse(expanded.usePlatformDefaultWidth)
        assertEquals(360, compact.maxWidthDp)
        assertEquals(420, standard.maxWidthDp)
        assertEquals(560, expanded.maxWidthDp)
        assertTrue(compact.maxWidthDp <= standard.maxWidthDp)
        assertTrue(standard.maxWidthDp <= expanded.maxWidthDp)
    }

    @Test
    fun contentDialogProperties_forceTabletSafeWidthFlag() {
        val properties = resolveAppContentDialogProperties(
            usePlatformDefaultWidth = false,
        )
        assertFalse(properties.usePlatformDefaultWidth)
    }

    @Test
    fun `popup facades select native hosts and preserve the injected hinge surface`() {
        val popupSurface = java.io.File(
            "src/main/java/com/android/purebilibili/core/ui/AppPopupSurface.kt"
        ).readText()
        val sheet = java.io.File(
            "src/main/java/com/android/purebilibili/core/ui/AppSheetComponents.kt"
        ).readText()
        val selection = java.io.File(
            "src/main/java/com/android/purebilibili/core/ui/components/AppSelectionPreferenceComponents.kt"
        ).readText()
        val primitives = java.io.File(
            "src/main/java/com/android/purebilibili/core/ui/components/AppPrimitiveComponents.kt"
        ).readText()
        val actionMenu = java.io.File(
            "src/main/java/com/android/purebilibili/core/ui/components/AppWindowActionMenu.kt"
        ).readText()

        assertTrue(popupSurface.contains("LocalAppPopupSurfaceRenderer"))
        assertTrue(popupSurface.contains("renderer.Render("))
        assertTrue(sheet.contains("WindowBottomSheet("))
        assertTrue(sheet.contains("type = AppPopupSurfaceType.DIALOG"))
        assertTrue(selection.contains("type = AppPopupSurfaceType.DIALOG"))
        assertTrue(selection.contains("AppSingleChoicePresentation.CENTERED_DIALOG"))
        assertTrue(primitives.contains("DropdownMenu("))
        assertTrue(primitives.contains("LocalAppUiStyle.current == AppUiStyle.MIUIX &&"))
        assertTrue(actionMenu.contains("WindowCascadingListPopup("))
        assertTrue(actionMenu.contains("action.onClick?.invoke()"))
    }
}
