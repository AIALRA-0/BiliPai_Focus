package com.android.purebilibili.feature.settings

import com.android.purebilibili.R
import com.android.purebilibili.core.ui.AppIcons
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class SettingsSearchUiPolicyTest {

    @Test
    fun telegramSearchResult_reusesSettingsSectionIconResource() {
        val visual = resolveSettingsEntryVisual(SettingsSearchTarget.TELEGRAM)

        assertEquals(R.drawable.ic_telegram_mono, visual.iconResId)
    }

    @Test
    fun twitterSearchResult_usesTwitterIcon() {
        val visual = resolveSettingsEntryVisual(SettingsSearchTarget.TWITTER)

        assertSame(AppIcons.Twitter, visual.icon)
    }

    @Test
    fun playbackSearchResult_usesPlaybackIcon() {
        val visual = resolveSettingsEntryVisual(SettingsSearchTarget.PLAYBACK)

        assertSame(CupertinoIcons.Outlined.PlayCircle, visual.icon)
    }

    @Test
    fun webDavSearchResult_reusesDataStorageSectionIcon() {
        val visual = resolveSettingsEntryVisual(SettingsSearchTarget.WEBDAV_BACKUP)

        assertSame(CupertinoIcons.Outlined.IcloudAndArrowUp, visual.icon)
    }

    @Test
    fun homeFeedSearchResult_usesHomeSemanticIcon() {
        val visual = resolveSettingsEntryVisual(SettingsSearchTarget.HOME_FEED)

        assertSame(CupertinoIcons.Outlined.House, visual.icon)
    }
}
