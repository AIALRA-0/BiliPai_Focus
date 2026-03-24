package com.android.purebilibili.feature.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AppUpdateAssetSelectionPolicyTest {

    @Test
    fun selectPreferredAppUpdateAsset_prefers_generic_release_apk_over_arch_specific_apk() {
        val selected = selectPreferredAppUpdateAsset(
            listOf(
                AppUpdateAsset(
                    name = "BiliPai-v6.9.3-arm64-v8a.apk",
                    downloadUrl = "https://example.com/arm64.apk",
                    sizeBytes = 73400320L,
                    contentType = "application/vnd.android.package-archive"
                ),
                AppUpdateAsset(
                    name = "BiliPai-v6.9.3.apk",
                    downloadUrl = "https://example.com/release.apk",
                    sizeBytes = 104857600L,
                    contentType = "application/vnd.android.package-archive"
                )
            )
        )

        assertEquals("BiliPai-v6.9.3.apk", selected?.name)
    }

    @Test
    fun selectPreferredAppUpdateAsset_prefers_release_apk_over_debug_apk() {
        val selected = selectPreferredAppUpdateAsset(
            listOf(
                AppUpdateAsset(
                    name = "BliPai-Focus-debug-7.1.2-focus.3-debug.apk",
                    downloadUrl = "https://example.com/debug.apk",
                    sizeBytes = 45034213L,
                    contentType = "application/vnd.android.package-archive"
                ),
                AppUpdateAsset(
                    name = "BliPai-Focus-release-7.1.2-focus.3.apk",
                    downloadUrl = "https://example.com/release.apk",
                    sizeBytes = 20756490L,
                    contentType = "application/vnd.android.package-archive"
                )
            )
        )

        assertEquals("BliPai-Focus-release-7.1.2-focus.3.apk", selected?.name)
    }

    @Test
    fun selectPreferredAppUpdateAsset_returns_null_when_no_apk_assets_exist() {
        val selected = selectPreferredAppUpdateAsset(
            listOf(
                AppUpdateAsset(
                    name = "checksums.txt",
                    downloadUrl = "https://example.com/checksums.txt",
                    sizeBytes = 512L,
                    contentType = "text/plain"
                )
            )
        )

        assertNull(selected)
    }
}
