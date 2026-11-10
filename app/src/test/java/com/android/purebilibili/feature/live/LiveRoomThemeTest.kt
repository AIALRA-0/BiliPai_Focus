package com.android.purebilibili.feature.live

import com.android.purebilibili.core.ui.AppThemeConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LiveRoomThemeTest {
    @Test
    fun `room menus follow glass preference while retaining playback configuration`() {
        val config = AppThemeConfig(hapticFeedbackEnabled = false, globalTextTapCopyEnabled = true)
        val native = liveRoomThemeConfig(config.copy(liquidGlassEnabled = false))
        val glass = liveRoomThemeConfig(config.copy(liquidGlassEnabled = true, liquidControlsUseThemeColor = true))
        assertEquals(native, glass.copy(liquidGlassEnabled = false))
        assertFalse(native.liquidGlassEnabled)
        assertTrue(glass.liquidGlassEnabled)
        assertFalse(glass.nativeMiuixPopupsEnabled)
        assertFalse(glass.hapticFeedbackEnabled)
        assertEquals(true, glass.globalTextTapCopyEnabled)
    }
}
