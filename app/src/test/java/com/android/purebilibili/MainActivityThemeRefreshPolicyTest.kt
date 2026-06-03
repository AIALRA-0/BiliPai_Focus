package com.android.purebilibili

import android.content.res.Configuration
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MainActivityThemeRefreshPolicyTest {

    @Test
    fun systemNightModeParser_readsUiModeNightMask() {
        assertTrue(
            resolveMainActivitySystemInDarkTheme(
                Configuration.UI_MODE_NIGHT_YES
            )
        )
        assertFalse(
            resolveMainActivitySystemInDarkTheme(
                Configuration.UI_MODE_NIGHT_NO
            )
        )
        assertFalse(
            resolveMainActivitySystemInDarkTheme(
                Configuration.UI_MODE_NIGHT_UNDEFINED
            )
        )
    }

    @Test
    fun systemThemeSnapshotRefreshesWhenBackgroundUiModeChanged() {
        assertTrue(
            shouldRefreshMainActivitySystemThemeSnapshot(
                previousSystemInDark = true,
                currentSystemInDark = false
            )
        )
        assertFalse(
            shouldRefreshMainActivitySystemThemeSnapshot(
                previousSystemInDark = false,
                currentSystemInDark = false
            )
        )
    }
}
