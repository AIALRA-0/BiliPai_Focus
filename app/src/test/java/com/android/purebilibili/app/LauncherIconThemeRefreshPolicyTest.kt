package com.android.purebilibili.app

import android.content.res.Configuration
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherIconThemeRefreshPolicyTest {

    @Test
    fun refreshesLauncherIconOnlyWhenNightModeChanges() {
        assertTrue(
            shouldRefreshLauncherIconForNightModeChange(
                previousUiMode = Configuration.UI_MODE_NIGHT_NO,
                currentUiMode = Configuration.UI_MODE_NIGHT_YES
            )
        )
        assertTrue(
            shouldRefreshLauncherIconForNightModeChange(
                previousUiMode = Configuration.UI_MODE_NIGHT_YES,
                currentUiMode = Configuration.UI_MODE_NIGHT_NO
            )
        )
        assertFalse(
            shouldRefreshLauncherIconForNightModeChange(
                previousUiMode = Configuration.UI_MODE_NIGHT_YES,
                currentUiMode = Configuration.UI_MODE_NIGHT_YES
            )
        )
    }

    @Test
    fun ignoresUnrelatedConfigurationBits() {
        assertFalse(
            shouldRefreshLauncherIconForNightModeChange(
                previousUiMode = Configuration.UI_MODE_NIGHT_NO or Configuration.UI_MODE_TYPE_NORMAL,
                currentUiMode = Configuration.UI_MODE_NIGHT_NO or Configuration.UI_MODE_TYPE_TELEVISION
            )
        )
    }
}
