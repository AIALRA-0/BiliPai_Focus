package com.android.purebilibili.feature.list

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryClearAllPolicyTest {

    @Test
    fun `clear all action shows only for history list with items and toggle enabled`() {
        assertTrue(
            shouldShowHistoryClearAllAction(
                hasHistoryViewModel = true,
                hasItems = true,
                settingEnabled = true,
                isBatchMode = false
            )
        )
    }

    @Test
    fun `clear all action hides during batch mode or when setting is disabled`() {
        assertFalse(
            shouldShowHistoryClearAllAction(
                hasHistoryViewModel = true,
                hasItems = true,
                settingEnabled = false,
                isBatchMode = false
            )
        )
        assertFalse(
            shouldShowHistoryClearAllAction(
                hasHistoryViewModel = true,
                hasItems = true,
                settingEnabled = true,
                isBatchMode = true
            )
        )
        assertFalse(
            shouldShowHistoryClearAllAction(
                hasHistoryViewModel = false,
                hasItems = true,
                settingEnabled = true,
                isBatchMode = false
            )
        )
    }

    @Test
    fun `history management menu applies the Focus setting to upstream clear action`() {
        val source = listOf(
            File("app/src/main/java/com/android/purebilibili/feature/list/CommonListScreen.kt"),
            File("src/main/java/com/android/purebilibili/feature/list/CommonListScreen.kt"),
        ).first { it.exists() }.readText().replace(Regex("\\s+"), " ")

        assertTrue(source.contains("getFocusHistoryClearAllActionEnabled(context)"))
        assertTrue(source.contains("if ( shouldShowHistoryClearAllAction("))
        assertTrue(source.contains("settingEnabled = focusHistoryClearAllActionEnabled"))
        assertTrue(source.contains("label = \"清空历史\""))
        assertTrue(source.contains("onClick = { showHistoryClearConfirm = true }"))
    }
}
