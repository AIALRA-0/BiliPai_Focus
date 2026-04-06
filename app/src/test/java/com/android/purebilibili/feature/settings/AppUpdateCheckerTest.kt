package com.android.purebilibili.feature.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateCheckerTest {

    @Test
    fun `normalizeVersion should trim v prefix and preserve beta suffix`() {
        assertEquals("5.3.1 Beta1", AppUpdateChecker.normalizeVersion("v5.3.1 Beta1"))
        assertEquals("5.3.1-beta.1", AppUpdateChecker.normalizeVersion(" V5.3.1-beta.1 "))
    }

    @Test
    fun `isRemoteNewer should compare semantic version parts`() {
        assertTrue(AppUpdateChecker.isRemoteNewer("5.3.1", "5.3.2"))
        assertTrue(AppUpdateChecker.isRemoteNewer("5.3.1", "5.4.0"))
        assertFalse(AppUpdateChecker.isRemoteNewer("5.3.1", "5.3.1"))
        assertFalse(AppUpdateChecker.isRemoteNewer("5.3.2", "5.3.1"))
    }

    @Test
    fun `isRemoteNewer should handle different part lengths`() {
        assertTrue(AppUpdateChecker.isRemoteNewer("5.3", "5.3.1"))
        assertFalse(AppUpdateChecker.isRemoteNewer("5.3.1", "5.3"))
    }

    @Test
    fun `isRemoteNewer should detect newer beta within same base version`() {
        assertTrue(AppUpdateChecker.isRemoteNewer("7.0.0 Beta1", "7.0.0 Beta2"))
        assertFalse(AppUpdateChecker.isRemoteNewer("7.0.0 Beta2", "7.0.0 Beta1"))
    }

    @Test
    fun `stable release should be newer than beta of same version`() {
        assertTrue(AppUpdateChecker.isRemoteNewer("7.0.0 Beta2", "7.0.0"))
        assertFalse(AppUpdateChecker.isRemoteNewer("7.0.0", "7.0.0 Beta3"))
    }

    @Test
    fun `rc release should sort between beta and stable of same version`() {
        assertTrue(AppUpdateChecker.isRemoteNewer("7.0.0 Beta5", "7.0.0 RC"))
        assertTrue(AppUpdateChecker.isRemoteNewer("7.0.0 RC", "7.0.0 RC2"))
        assertTrue(AppUpdateChecker.isRemoteNewer("7.0.0 RC", "7.0.0"))
        assertFalse(AppUpdateChecker.isRemoteNewer("7.0.0 RC", "7.0.0 Beta5"))
    }

    @Test
    fun `focus subversion should compare within same upstream base version`() {
        assertTrue(AppUpdateChecker.isRemoteNewer("7.1.0", "7.1.0-focus.1"))
        assertTrue(AppUpdateChecker.isRemoteNewer("7.1.0-focus.1", "7.1.0-focus.2"))
        assertFalse(AppUpdateChecker.isRemoteNewer("7.1.0-focus.2", "7.1.0-focus.1"))
    }

    @Test
    fun `selectLatestReleaseCandidate should allow prerelease when current version is beta`() {
        val release = AppUpdateChecker.selectLatestReleaseCandidate(
            rawReleaseJson = """
            [
              {
                "tag_name": "v7.0.0 Beta2",
                "html_url": "https://example.com/beta2",
                "body": "beta2 notes",
                "published_at": "2026-03-15T10:00:00Z",
                "draft": false,
                "prerelease": true,
                "assets": []
              },
              {
                "tag_name": "v6.9.9",
                "html_url": "https://example.com/stable",
                "body": "stable notes",
                "published_at": "2026-03-14T10:00:00Z",
                "draft": false,
                "prerelease": false,
                "assets": []
              }
            ]
            """.trimIndent(),
            currentVersion = "7.0.0 Beta1"
        )

        assertEquals("v7.0.0 Beta2", release?.tagName)
    }

    @Test
    fun `selectLatestReleaseCandidate should ignore prerelease for stable channel`() {
        val release = AppUpdateChecker.selectLatestReleaseCandidate(
            rawReleaseJson = """
            [
              {
                "tag_name": "v7.0.1 Beta1",
                "html_url": "https://example.com/beta",
                "body": "beta notes",
                "published_at": "2026-03-15T10:00:00Z",
                "draft": false,
                "prerelease": true,
                "assets": []
              },
              {
                "tag_name": "v7.0.0",
                "html_url": "https://example.com/stable",
                "body": "stable notes",
                "published_at": "2026-03-14T10:00:00Z",
                "draft": false,
                "prerelease": false,
                "assets": []
              }
            ]
            """.trimIndent(),
            currentVersion = "7.0.0"
        )

        assertEquals("v7.0.0", release?.tagName)
    }

    @Test
    fun `parseRepositoryVersionCandidate should read version from remote gradle file`() {
        val candidate = AppUpdateChecker.parseRepositoryVersionCandidate(
            rawBuildGradle = """
            android {
                defaultConfig {
                    versionCode = 119
                    versionName = "7.0.0 RC2"
                }
            }
            """.trimIndent()
        )

        assertEquals("7.0.0 RC2", candidate?.tagName)
        assertEquals("https://github.com/AIALRA-0/BiliPai_Focus", candidate?.releaseUrl)
        assertTrue(candidate?.releaseNotes?.contains("未创建 GitHub Release") == true)
        assertTrue(candidate?.isPrerelease == true)
    }

    @Test
    fun `parseReleaseAssets should keep apk metadata and sidecar assets`() {
        val assets = AppUpdateChecker.parseReleaseAssets(
            """
            {
              "assets": [
                {
                  "name": "BiliPai-v6.9.3.apk",
                  "browser_download_url": "https://example.com/BiliPai-v6.9.3.apk",
                  "size": 104857600,
                  "content_type": "application/vnd.android.package-archive"
                },
                {
                  "name": "BiliPai-v6.9.3-arm64-v8a.apk",
                  "browser_download_url": "https://example.com/BiliPai-v6.9.3-arm64-v8a.apk",
                  "size": 73400320,
                  "content_type": "application/vnd.android.package-archive"
                },
                {
                  "name": "checksums.txt",
                  "browser_download_url": "https://example.com/checksums.txt",
                  "size": 512,
                  "content_type": "text/plain"
                }
              ]
            }
            """.trimIndent()
        )

        assertEquals(3, assets.size)
        assertEquals("BiliPai-v6.9.3.apk", assets[0].name)
        assertEquals("https://example.com/BiliPai-v6.9.3.apk", assets[0].downloadUrl)
        assertEquals(104857600L, assets[0].sizeBytes)
        assertEquals("application/vnd.android.package-archive", assets[0].contentType)
        assertTrue(assets.take(2).all { it.isApk })
        assertTrue(assets.last().isChecksumsFile)
    }

    @Test
    fun `parseReleaseAssets should return empty list when assets are missing`() {
        assertTrue(AppUpdateChecker.parseReleaseAssets("""{"tag_name":"v6.9.3"}""").isEmpty())
    }

    @Test
    fun `selectLatestReleaseCandidate should parse immutable release and sidecar assets`() {
        val release = AppUpdateChecker.selectLatestReleaseCandidate(
            rawReleaseJson = """
            [
              {
                "tag_name": "v7.3.3",
                "html_url": "https://example.com/release",
                "body": "notes",
                "published_at": "2026-04-03T10:00:00Z",
                "draft": false,
                "prerelease": false,
                "immutable": true,
                "assets": [
                  {
                    "name": "BiliPai-release-7.3.3.apk",
                    "browser_download_url": "https://example.com/app.apk",
                    "size": 100,
                    "content_type": "application/vnd.android.package-archive",
                    "digest": "sha256:abc123"
                  },
                  {
                    "name": "build-metadata.json",
                    "browser_download_url": "https://example.com/build-metadata.json",
                    "size": 50,
                    "content_type": "application/json"
                  },
                  {
                    "name": "checksums.txt",
                    "browser_download_url": "https://example.com/checksums.txt",
                    "size": 12,
                    "content_type": "text/plain"
                  },
                  {
                    "name": "verification-metadata.json",
                    "browser_download_url": "https://example.com/verification-metadata.json",
                    "size": 64,
                    "content_type": "application/json"
                  }
                ]
              }
            ]
            """.trimIndent(),
            currentVersion = "7.3.3"
        )

        assertTrue(release?.isImmutable == true)
        assertEquals(4, release?.assets?.size)
        assertEquals("abc123", release?.assets?.firstOrNull()?.sha256Digest)
        assertTrue(release?.assets?.any { it.isBuildMetadata } == true)
        assertTrue(release?.assets?.any { it.isChecksumsFile } == true)
        assertTrue(release?.assets?.any { it.isVerificationMetadata } == true)
    }

    @Test
    fun `parseBuildMetadata should extract commit workflow and artifact digests`() {
        val metadata = AppUpdateChecker.parseBuildMetadata(
            """
            {
              "schemaVersion": 1,
              "appId": "com.android.purebilibili",
              "versionName": "7.3.3",
              "versionCode": 135,
              "gitCommitSha": "abcdef1234567890",
              "gitRef": "refs/tags/v7.3.3",
              "workflowRunId": "123456789",
              "workflowRunUrl": "https://github.com/jay3-yy/BiliPai/actions/runs/123456789",
              "releaseTag": "v7.3.3",
              "generatedAt": "2026-04-03T10:00:00Z",
              "artifacts": [
                {
                  "name": "BiliPai-release-7.3.3.apk",
                  "sha256": "feedbeef",
                  "sizeBytes": 100
                }
              ]
            }
            """.trimIndent()
        )

        assertEquals("abcdef1234567890", metadata?.gitCommitSha)
        assertEquals("123456789", metadata?.workflowRunId)
        assertEquals("v7.3.3", metadata?.releaseTag)
        assertEquals("feedbeef", metadata?.artifacts?.singleOrNull()?.sha256)
    }

    @Test
    fun `parseVerificationMetadata should extract attestation evidence`() {
        val metadata = AppUpdateChecker.parseVerificationMetadata(
            """
            {
              "attestationUrl": "https://github.com/jay3-yy/BiliPai/attestations/123",
              "bundleFileName": "build-provenance.intoto.jsonl",
              "predicateType": "https://slsa.dev/provenance/v1"
            }
            """.trimIndent()
        )

        assertEquals("https://github.com/jay3-yy/BiliPai/attestations/123", metadata?.attestationUrl)
        assertEquals("build-provenance.intoto.jsonl", metadata?.bundleFileName)
        assertEquals("https://slsa.dev/provenance/v1", metadata?.predicateType)
    }
}
