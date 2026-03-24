package com.android.purebilibili.feature.download

import java.io.File
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
    @Test
    fun resolveManagedDownloadDirectory_fallsBackToInternalFilesDirWhenExternalRootMissing() {
        val filesDir = File("/data/user/0/com.android.purebilibili.debug/files")

        val resolved = resolveManagedDownloadDirectory(
            filesDir = filesDir,
            externalFilesRoot = null,
            customPath = null
        )

        assertEquals(
            File(filesDir, "downloads").absolutePath,
            resolved.absolutePath
        )
    }

    @Test
    fun resolveManagedDownloadDirectory_rejectsLegacyCustomPathOutsideManagedRoot() {
        val filesDir = File("/data/user/0/com.android.purebilibili.debug/files")
        val externalRoot = File("/storage/emulated/0/Android/data/com.android.purebilibili.debug/files")

        val resolved = resolveManagedDownloadDirectory(
            filesDir = filesDir,
            externalFilesRoot = externalRoot,
            customPath = "/storage/emulated/0/Download"
        )

        assertEquals(
            File(externalRoot, "downloads").absolutePath,
            resolved.absolutePath
        )
    }

    @Test
    fun resolveDisplayedDownloadLocation_prefersUserSelectedExportDirectory() {
        val displayed = resolveDisplayedDownloadLocation(
            defaultManagedPath = "/storage/emulated/0/Android/data/com.android.purebilibili.debug/files/downloads",
            customManagedPath = null,
            exportTreeUri = "content://com.android.externalstorage.documents/tree/primary%3ADownload%2FBiliPai"
        )

        assertEquals("/storage/emulated/0/Download/BiliPai", displayed)
    }

    @Test
    fun resolveDisplayedDownloadLocation_fallsBackToManagedDirectoryWhenExportDirectoryIsMissing() {
        val displayed = resolveDisplayedDownloadLocation(
            defaultManagedPath = "/storage/emulated/0/Android/data/com.android.purebilibili.debug/files/downloads",
            customManagedPath = "/storage/emulated/0/Android/data/com.android.purebilibili.debug/files/custom-downloads",
            exportTreeUri = null
        )

        assertEquals(
            "/storage/emulated/0/Android/data/com.android.purebilibili.debug/files/custom-downloads",
            displayed
        )
    }
}
