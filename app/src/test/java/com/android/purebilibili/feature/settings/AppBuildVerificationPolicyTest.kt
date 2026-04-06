package com.android.purebilibili.feature.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppBuildVerificationPolicyTest {

    @Test
    fun `resolveAppBuildVerificationState returns verified when immutable release digest matches local apk`() {
        val state = resolveAppBuildVerificationState(
            currentVersion = "7.3.3",
            localBuildCommitSha = "abcdef1234567890",
            localWorkflowRunId = "123456789",
            localWorkflowRunUrl = "https://github.com/jay3-yy/BiliPai/actions/runs/123456789",
            localReleaseTag = "v7.3.3",
            localApkSha256 = "feedbeef",
            remoteRelease = AppUpdateCheckResult(
                isUpdateAvailable = false,
                currentVersion = "7.3.3",
                latestVersion = "7.3.3",
                releaseUrl = "https://example.com/release",
                releaseNotes = "notes",
                publishedAt = null,
                assets = listOf(
                    AppUpdateAsset(
                        name = "BiliPai-release-7.3.3.apk",
                        downloadUrl = "https://example.com/app.apk",
                        sizeBytes = 100,
                        contentType = "application/vnd.android.package-archive",
                        digest = "sha256:feedbeef"
                    )
                ),
                message = "已是最新版本",
                releaseIsImmutable = true,
                buildMetadata = AppReleaseBuildMetadata(
                    gitCommitSha = "abcdef1234567890",
                    workflowRunId = "123456789",
                    workflowRunUrl = "https://github.com/jay3-yy/BiliPai/actions/runs/123456789",
                    releaseTag = "v7.3.3"
                ),
                verificationMetadata = AppReleaseVerificationMetadata(
                    attestationUrl = "https://github.com/jay3-yy/BiliPai/attestations/123"
                )
            )
        )

        assertEquals(AppBuildVerificationStatus.VERIFIED, state.status)
        assertEquals("已验证", resolveAppBuildVerificationLabel(state.status))
        assertEquals(true, state.hasAttestation)
        assertTrue(state.summary.contains("SHA-256"))
        assertTrue(state.summary.contains("Release 已锁定"))
    }

    @Test
    fun `resolveAppBuildVerificationState returns likely verified when embedded provenance exists but remote evidence is absent`() {
        val state = resolveAppBuildVerificationState(
            currentVersion = "7.3.3",
            localBuildCommitSha = "abcdef1234567890",
            localWorkflowRunId = "123456789",
            localWorkflowRunUrl = "https://github.com/jay3-yy/BiliPai/actions/runs/123456789",
            localReleaseTag = "v7.3.3",
            localApkSha256 = null,
            remoteRelease = null
        )

        assertEquals(AppBuildVerificationStatus.LIKELY_VERIFIED, state.status)
    }

    @Test
    fun `resolveAppBuildVerificationState returns unverified when release is mutable`() {
        val state = resolveAppBuildVerificationState(
            currentVersion = "7.3.3",
            localBuildCommitSha = "abcdef1234567890",
            localWorkflowRunId = "123456789",
            localWorkflowRunUrl = "https://github.com/jay3-yy/BiliPai/actions/runs/123456789",
            localReleaseTag = "v7.3.3",
            localApkSha256 = "feedbeef",
            remoteRelease = AppUpdateCheckResult(
                isUpdateAvailable = false,
                currentVersion = "7.3.3",
                latestVersion = "7.3.3",
                releaseUrl = "https://example.com/release",
                releaseNotes = "notes",
                publishedAt = null,
                assets = listOf(
                    AppUpdateAsset(
                        name = "BiliPai-release-7.3.3.apk",
                        downloadUrl = "https://example.com/app.apk",
                        sizeBytes = 100,
                        contentType = "application/vnd.android.package-archive",
                        digest = "sha256:feedbeef"
                    )
                ),
                message = "已是最新版本",
                releaseIsImmutable = false,
                buildMetadata = AppReleaseBuildMetadata(
                    gitCommitSha = "abcdef1234567890",
                    workflowRunId = "123456789",
                    workflowRunUrl = "https://github.com/jay3-yy/BiliPai/actions/runs/123456789",
                    releaseTag = "v7.3.3"
                )
            )
        )

        assertEquals(AppBuildVerificationStatus.UNVERIFIED, state.status)
        assertTrue(state.summary.contains("还可被修改"))
    }

    @Test
    fun `resolveAppBuildVerificationState returns likely verified when immutable release digest matches without embedded provenance`() {
        val state = resolveAppBuildVerificationState(
            currentVersion = "7.4.0",
            localBuildCommitSha = "local",
            localWorkflowRunId = "",
            localWorkflowRunUrl = "",
            localReleaseTag = "",
            localApkSha256 = "feedbeef",
            remoteRelease = AppUpdateCheckResult(
                isUpdateAvailable = false,
                currentVersion = "7.4.0",
                latestVersion = "7.4.0",
                releaseUrl = "https://example.com/release",
                releaseNotes = "notes",
                publishedAt = null,
                assets = listOf(
                    AppUpdateAsset(
                        name = "BiliPai-release-7.4.0.apk",
                        downloadUrl = "https://example.com/app.apk",
                        sizeBytes = 100,
                        contentType = "application/vnd.android.package-archive",
                        digest = "sha256:feedbeef"
                    )
                ),
                message = "已是最新版本",
                releaseIsImmutable = true,
                buildMetadata = AppReleaseBuildMetadata(
                    gitCommitSha = "abcdef1234567890",
                    workflowRunId = "123456789",
                    workflowRunUrl = "https://github.com/jay3-yy/BiliPai/actions/runs/123456789",
                    releaseTag = "v7.4.0"
                ),
                verificationMetadata = AppReleaseVerificationMetadata(
                    attestationUrl = "https://github.com/jay3-yy/BiliPai/attestations/123"
                )
            )
        )

        assertEquals(AppBuildVerificationStatus.LIKELY_VERIFIED, state.status)
        assertEquals("abcdef1234567890", state.sourceCommitSha)
        assertEquals("123456789", state.workflowRunId)
        assertEquals("v7.4.0", state.releaseTag)
        assertTrue(state.summary.contains("SHA-256"))
    }

    @Test
    fun `resolveAppBuildVerificationState returns likely verified when mutable release digest matches without embedded provenance`() {
        val state = resolveAppBuildVerificationState(
            currentVersion = "7.4.0",
            localBuildCommitSha = "local",
            localWorkflowRunId = "",
            localWorkflowRunUrl = "",
            localReleaseTag = "",
            localApkSha256 = "feedbeef",
            remoteRelease = AppUpdateCheckResult(
                isUpdateAvailable = false,
                currentVersion = "7.4.0",
                latestVersion = "7.4.0",
                releaseUrl = "https://example.com/release",
                releaseNotes = "notes",
                publishedAt = null,
                assets = listOf(
                    AppUpdateAsset(
                        name = "BiliPai-release-7.4.0.apk",
                        downloadUrl = "https://example.com/app.apk",
                        sizeBytes = 100,
                        contentType = "application/vnd.android.package-archive",
                        digest = "sha256:feedbeef"
                    )
                ),
                message = "已是最新版本",
                releaseIsImmutable = false,
                buildMetadata = AppReleaseBuildMetadata(
                    releaseTag = "v7.4.0"
                )
            )
        )

        assertEquals(AppBuildVerificationStatus.LIKELY_VERIFIED, state.status)
        assertEquals("v7.4.0", state.releaseTag)
        assertTrue(state.summary.contains("SHA-256"))
        assertTrue(state.summary.contains("还可被修改"))
    }

    @Test
    fun `resolveBuildFingerprintValue shortens sha256 for about screen`() {
        val value = resolveBuildFingerprintValue("0123456789abcdef0123456789abcdef")

        assertEquals("0123456789ab", value)
    }

    @Test
    fun `resolveBuildSourceValue uses provided fallback when commit is missing`() {
        val value = resolveBuildSourceValue(
            commitSha = "",
            fallback = "GitHub Release"
        )

        assertEquals("GitHub Release", value)
    }

    @Test
    fun `resolveBuildFingerprintSubtitle describes matched immutable release with attestation`() {
        val subtitle = resolveBuildFingerprintSubtitle(
            localApkSha256 = "feedbeef",
            remoteApkSha256 = "feedbeef",
            releaseIsImmutable = true,
            hasAttestation = true
        )

        assertEquals("与 GitHub Release SHA-256 一致，Release 已锁定，含 provenance。", subtitle)
    }

    @Test
    fun `resolveVerificationDialogContent keeps full verification summary`() {
        val content = resolveVerificationDialogContent(
            label = "未验证",
            summary = "当前安装包缺少足够的发布侧证据，暂时无法核对源码与安装包是否一致。"
        )

        assertEquals("源码一致性", content.title)
        assertEquals("未验证", content.value)
        assertEquals("当前安装包缺少足够的发布侧证据，暂时无法核对源码与安装包是否一致。", content.body)
        assertEquals("查看证明", content.actionLabel)
    }

    @Test
    fun `resolveBuildSourceDialogContent keeps full build source subtitle`() {
        val content = resolveBuildSourceDialogContent(
            value = "GitHub Release",
            subtitle = "workflow #123456 · tag v7.4.0"
        )

        assertEquals("构建来源", content.title)
        assertEquals("GitHub Release", content.value)
        assertEquals("workflow #123456 · tag v7.4.0", content.body)
        assertEquals("查看来源", content.actionLabel)
    }

    @Test
    fun `resolveBuildFingerprintDialogContent includes full sha256 and subtitle`() {
        val content = resolveBuildFingerprintDialogContent(
            value = "49a40f9149e3",
            fullValue = "49a40f9149e31234567890abcdef1234567890abcdef1234567890abcdef1234",
            subtitle = "与当前 Release SHA-256 一致，但该 Release 还可被修改。"
        )

        assertEquals("SHA-256", content.title)
        assertEquals("49a40f9149e3", content.value)
        assertTrue(content.body.contains("49a40f9149e31234567890abcdef1234567890abcdef1234567890abcdef1234"))
        assertTrue(content.body.contains("与当前 Release SHA-256 一致，但该 Release 还可被修改。"))
        assertEquals("查看证明", content.actionLabel)
    }
}
