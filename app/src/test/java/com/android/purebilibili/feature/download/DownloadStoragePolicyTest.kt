package com.android.purebilibili.feature.download

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DownloadStoragePolicyTest {

    @Test
    fun `legacy custom path outside app scoped root should be rejected`() {
        val sanitized = sanitizeLegacyCustomPath(
            customPath = "/storage/emulated/0/Download/BiliPai",
            appScopedRoot = "/storage/emulated/0/Android/data/com.android.purebilibili/files"
        )

        assertNull(sanitized)
    }

    @Test
    fun `legacy custom path inside app scoped root should be kept`() {
        val sanitized = sanitizeLegacyCustomPath(
            customPath = "/storage/emulated/0/Android/data/com.android.purebilibili/files/downloads",
            appScopedRoot = "/storage/emulated/0/Android/data/com.android.purebilibili/files"
        )

        assertEquals(
            "/storage/emulated/0/Android/data/com.android.purebilibili/files/downloads",
            sanitized
        )
    }

    @Test
    fun `export display name should sanitize invalid characters`() {
        val displayName = buildSafeExportDisplayName(
            title = "A/B:C*D?E\"F<G>H|I",
            qualityDesc = "1080P",
            extension = "mp4"
        )

        assertEquals("A_B_C_D_E_F_G_H_I_1080P.mp4", displayName)
    }
}
