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
    fun `Focus patch successor remains detectable by old version-name-only clients`() {
        assertTrue(AppUpdateChecker.isRemoteNewer("9.1.1-focus.4", "9.1.1-focus.5"))
        assertTrue(
            AppUpdateChecker.shouldOfferUpdate(
                currentVersion = "9.1.1-focus.4",
                currentVersionCode = 226,
                latestVersion = "9.1.1-focus.5",
                buildMetadata = null
            )
        )
    }

    @Test
    fun `update endpoints stay on Focus and retain a Focus fallback`() {
        val endpoints = AppUpdateChecker.resolveEndpointCandidates(
            primary = AppUpdateEndpointSet(
                releasesApi = "https://api.github.com/repos/example/BiliPai_Focus/releases",
                repositoryBuildGradleUrl = "https://raw.githubusercontent.com/example/BiliPai_Focus/main/app/build.gradle.kts",
                repositoryUrl = "https://github.com/example/BiliPai_Focus",
                releasesPageUrl = "https://github.com/example/BiliPai_Focus/releases"
            )
        )

        assertEquals(2, endpoints.size)
        assertEquals("https://github.com/example/BiliPai_Focus", endpoints[0].repositoryUrl)
        assertEquals("https://api.github.com/repos/AIALRA-0/BiliPai_Focus/releases", endpoints[1].releasesApi)
    }

    @Test
    fun `repository version fallback reads Focus version without switching repositories`() {
        val candidate = AppUpdateChecker.parseRepositoryVersionCandidate(
            rawBuildGradle = "defaultConfig { versionName = \"9.1.1-focus.5\" }",
            repositoryUrl = "https://github.com/AIALRA-0/BiliPai_Focus"
        )

        assertEquals("9.1.1-focus.5", candidate?.tagName)
        assertEquals("https://github.com/AIALRA-0/BiliPai_Focus/releases", candidate?.releaseUrl)
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
    fun `selectLatestReleaseCandidate should ignore prereleases even on a beta install`() {
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
                "assets": [{
                  "name": "BiliPai-v6.9.9.apk",
                  "browser_download_url": "https://example.com/stable.apk",
                  "content_type": "application/vnd.android.package-archive"
                }]
              }
            ]
            """.trimIndent()
        )

        assertEquals("v6.9.9", release?.tagName)
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
                "assets": [{
                  "name": "BiliPai-v7.0.0.apk",
                  "browser_download_url": "https://example.com/stable.apk",
                  "content_type": "application/vnd.android.package-archive"
                }]
              }
            ]
            """.trimIndent()
        )

        assertEquals("v7.0.0", release?.tagName)
    }

    @Test
    fun `selectLatestReleaseCandidate should ignore stable releases without an apk`() {
        val release = AppUpdateChecker.selectLatestReleaseCandidate(
            rawReleaseJson = """
            [{
              "tag_name": "v8.0.0",
              "draft": false,
              "prerelease": false,
              "assets": [{
                "name": "build-metadata.json",
                "browser_download_url": "https://example.com/build-metadata.json",
                "content_type": "application/json"
              }]
            }]
            """.trimIndent()
        )

        assertEquals(null, release)
    }

    @Test
    fun `selectLatestReleaseCandidate should include prerelease with apk for beta channel`() {
        val release = AppUpdateChecker.selectLatestReleaseCandidate(
            rawReleaseJson = """
            [
              {
                "tag_name": "v7.1.0 Beta1",
                "html_url": "https://example.com/beta",
                "body": "beta notes",
                "published_at": "2026-03-15T10:00:00Z",
                "draft": false,
                "prerelease": true,
                "assets": [{
                  "name": "BiliPai-v7.1.0-Beta1.apk",
                  "browser_download_url": "https://example.com/beta.apk",
                  "content_type": "application/vnd.android.package-archive"
                }]
              },
              {
                "tag_name": "v7.0.0",
                "html_url": "https://example.com/stable",
                "body": "stable notes",
                "published_at": "2026-03-14T10:00:00Z",
                "draft": false,
                "prerelease": false,
                "assets": [{
                  "name": "BiliPai-v7.0.0.apk",
                  "browser_download_url": "https://example.com/stable.apk",
                  "content_type": "application/vnd.android.package-archive"
                }]
              }
            ]
            """.trimIndent(),
            includePrerelease = true
        )

        assertEquals("v7.1.0 Beta1", release?.tagName)
    }

    @Test
    fun `beta channel should fall back to stable when prerelease has no apk`() {
        val release = AppUpdateChecker.selectLatestReleaseCandidate(
            rawReleaseJson = """
            [
              {
                "tag_name": "v7.1.0 Beta1",
                "html_url": "https://example.com/beta",
                "body": "beta notes",
                "published_at": "2026-03-15T10:00:00Z",
                "draft": false,
                "prerelease": true,
                "assets": [{
                  "name": "build-metadata.json",
                  "browser_download_url": "https://example.com/build-metadata.json",
                  "content_type": "application/json"
                }]
              },
              {
                "tag_name": "v7.0.0",
                "html_url": "https://example.com/stable",
                "body": "stable notes",
                "published_at": "2026-03-14T10:00:00Z",
                "draft": false,
                "prerelease": false,
                "assets": [{
                  "name": "BiliPai-v7.0.0.apk",
                  "browser_download_url": "https://example.com/stable.apk",
                  "content_type": "application/vnd.android.package-archive"
                }]
              }
            ]
            """.trimIndent(),
            includePrerelease = true
        )

        assertEquals("v7.0.0", release?.tagName)
    }

    @Test
    fun `stable channel should keep ignoring prerelease when includePrerelease is false`() {
        val release = AppUpdateChecker.selectLatestReleaseCandidate(
            rawReleaseJson = """
            [
              {
                "tag_name": "v7.1.0 Beta1",
                "html_url": "https://example.com/beta",
                "body": "beta notes",
                "published_at": "2026-03-15T10:00:00Z",
                "draft": false,
                "prerelease": true,
                "assets": [{
                  "name": "BiliPai-v7.1.0-Beta1.apk",
                  "browser_download_url": "https://example.com/beta.apk",
                  "content_type": "application/vnd.android.package-archive"
                }]
              },
              {
                "tag_name": "v7.0.0",
                "html_url": "https://example.com/stable",
                "body": "stable notes",
                "published_at": "2026-03-14T10:00:00Z",
                "draft": false,
                "prerelease": false,
                "assets": [{
                  "name": "BiliPai-v7.0.0.apk",
                  "browser_download_url": "https://example.com/stable.apk",
                  "content_type": "application/vnd.android.package-archive"
                }]
              }
            ]
            """.trimIndent(),
            includePrerelease = false
        )

        assertEquals("v7.0.0", release?.tagName)
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
                    "name": "BiliPai-Focus-7.3.3.apk",
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
            """.trimIndent()
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
              "appId": "com.android.purebilibili.focus",
              "versionName": "9.1.1-focus.5",
              "versionCode": 385,
              "gitCommitSha": "abcdef1234567890",
              "gitRef": "refs/tags/v9.1.1-focus.5",
              "workflowRunId": "123456789",
              "workflowRunUrl": "https://github.com/AIALRA-0/BiliPai_Focus/actions/runs/123456789",
              "releaseTag": "v9.1.1-focus.5",
              "generatedAt": "2026-04-03T10:00:00Z",
              "artifacts": [
                {
                  "name": "BiliPai-Focus-9.1.1-focus.5.apk",
                  "sha256": "feedbeef",
                  "sizeBytes": 100
                }
              ]
            }
            """.trimIndent()
        )

        assertEquals("abcdef1234567890", metadata?.gitCommitSha)
        assertEquals("com.android.purebilibili.focus", metadata?.appId)
        assertEquals("123456789", metadata?.workflowRunId)
        assertEquals("v9.1.1-focus.5", metadata?.releaseTag)
        assertEquals("feedbeef", metadata?.artifacts?.singleOrNull()?.sha256)
    }

    @Test
    fun `parseVerificationMetadata should extract attestation evidence`() {
        val metadata = AppUpdateChecker.parseVerificationMetadata(
            """
            {
              "attestationUrl": "https://github.com/AIALRA-0/BiliPai_Focus/attestations/123",
              "bundleFileName": "build-provenance.intoto.jsonl",
              "predicateType": "https://slsa.dev/provenance/v1"
            }
            """.trimIndent()
        )

        assertEquals("https://github.com/AIALRA-0/BiliPai_Focus/attestations/123", metadata?.attestationUrl)
        assertEquals("build-provenance.intoto.jsonl", metadata?.bundleFileName)
        assertEquals("https://slsa.dev/provenance/v1", metadata?.predicateType)
    }

    @Test
    fun `latest published stable release wins across version epochs`() {
        val release = AppUpdateChecker.selectLatestReleaseCandidate(
            rawReleaseJson = """
            [
              {
                "tag_name": "v9.9.9.8.7",
                "published_at": "2026-08-02T10:00:00Z",
                "draft": false,
                "prerelease": false,
                "assets": [{
                  "name": "BiliPai-9.9.9.8.7.apk",
                  "browser_download_url": "https://example.com/legacy.apk",
                  "content_type": "application/vnd.android.package-archive"
                }]
              },
              {
                "tag_name": "v0.1.0",
                "published_at": "2026-08-04T10:00:00Z",
                "draft": false,
                "prerelease": false,
                "assets": [{
                  "name": "BiliPai-0.1.0.apk",
                  "browser_download_url": "https://example.com/current.apk",
                  "content_type": "application/vnd.android.package-archive"
                }]
              }
            ]
            """.trimIndent()
        )

        assertEquals("v0.1.0", release?.tagName)
    }

    @Test
    fun `version code is authoritative when release metadata is available`() {
        val metadata = AppReleaseBuildMetadata(versionName = "0.1.1", versionCode = 283)

        assertTrue(
            AppUpdateChecker.shouldOfferUpdate(
                currentVersion = "0.1.0",
                currentVersionCode = 282,
                latestVersion = "0.1.1",
                buildMetadata = metadata
            )
        )
        assertFalse(
            AppUpdateChecker.shouldOfferUpdate(
                currentVersion = "0.1.0",
                currentVersionCode = 283,
                latestVersion = "0.1.1",
                buildMetadata = metadata
            )
        )
    }

    @Test
    fun `metadata fallback compares version names only within the same epoch`() {
        assertTrue(
            AppUpdateChecker.shouldOfferUpdate(
                currentVersion = "0.1.0",
                currentVersionCode = 282,
                latestVersion = "0.1.1",
                buildMetadata = null
            )
        )
        assertFalse(
            AppUpdateChecker.shouldOfferUpdate(
                currentVersion = "9.9.9.8.7",
                currentVersionCode = 281,
                latestVersion = "0.1.0",
                buildMetadata = null
            )
        )
    }
}
