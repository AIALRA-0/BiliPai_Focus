package com.android.purebilibili.feature.settings

import kotlin.test.Test
import kotlin.test.assertEquals

class AppUpdateAssetChecksumPolicyTest {
    @Test
    fun assetDigest_takesPrecedenceOverBuildMetadata() {
        val asset = AppUpdateAsset(
            name = "BiliPai.apk",
            downloadUrl = "https://example.com/BiliPai.apk",
            sizeBytes = 1,
            contentType = "application/vnd.android.package-archive",
            digest = "sha256:asset-digest",
        )
        val metadata = AppReleaseBuildMetadata(
            artifacts = listOf(AppReleaseBuildArtifact("BiliPai.apk", "metadata-digest", 1)),
        )

        assertEquals("asset-digest", resolveAppUpdateExpectedSha256(asset, metadata))
    }
}
