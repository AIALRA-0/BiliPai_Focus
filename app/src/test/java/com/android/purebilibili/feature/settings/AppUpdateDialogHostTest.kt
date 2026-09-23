package com.android.purebilibili.feature.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class AppUpdateDialogHostTest {

    @Test
    fun `alternate channel links use the configured repository`() {
        val urls = appUpdateChannelUrls("https://github.com/example/BiliPai_Focus/releases/")

        assertEquals(
            "https://github.com/example/BiliPai_Focus/releases/latest",
            urls.stableReleaseUrl,
        )
        assertEquals(
            "https://github.com/example/BiliPai_Focus/releases",
            urls.prereleaseReleasesUrl,
        )
    }

    @Test
    fun `alternate channel links retain the Focus fallback repository`() {
        val endpoints = AppUpdateChecker.resolveEndpointCandidates(
            primary = AppUpdateEndpointSet(
                releasesApi = "",
                repositoryBuildGradleUrl = "",
                repositoryUrl = "",
                releasesPageUrl = "",
            ),
        )
        val urls = appUpdateChannelUrls(endpoints.first().releasesPageUrl)

        assertEquals(
            "https://github.com/AIALRA-0/BiliPai_Focus/releases/latest",
            urls.stableReleaseUrl,
        )
        assertEquals(
            "https://github.com/AIALRA-0/BiliPai_Focus/releases",
            urls.prereleaseReleasesUrl,
        )
    }
}
