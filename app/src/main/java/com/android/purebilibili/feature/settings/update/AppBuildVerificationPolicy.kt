package com.android.purebilibili.feature.settings

enum class AppBuildVerificationStatus {
    VERIFIED,
    LIKELY_VERIFIED,
    UNVERIFIED
}

data class AppBuildVerificationState(
    val status: AppBuildVerificationStatus,
    val summary: String,
    val sourceCommitSha: String? = null,
    val workflowRunId: String? = null,
    val workflowRunUrl: String? = null,
    val releaseTag: String? = null,
    val localApkSha256: String? = null,
    val remoteApkSha256: String? = null,
    val releaseIsImmutable: Boolean? = null,
    val hasAttestation: Boolean = false
)

enum class AppBuildInfoDialogAction {
    VIEW_VERIFICATION,
    VIEW_BUILD_SOURCE,
    VIEW_BUILD_FINGERPRINT
}

data class AppBuildInfoDialogContent(
    val title: String,
    val value: String,
    val body: String,
    val actionLabel: String,
    val action: AppBuildInfoDialogAction
)

internal fun resolveAppBuildVerificationState(
    currentVersion: String,
    localBuildCommitSha: String,
    localWorkflowRunId: String,
    localWorkflowRunUrl: String,
    localReleaseTag: String,
    localApkSha256: String?,
    remoteRelease: AppUpdateCheckResult?
): AppBuildVerificationState {
    val normalizedCurrentVersion = AppUpdateChecker.normalizeVersion(currentVersion)
    val normalizedLocalTag = AppUpdateChecker.normalizeVersion(localReleaseTag)
    val hasEmbeddedProvenance = localBuildCommitSha.isNotBlank() &&
        !localBuildCommitSha.equals("local", ignoreCase = true) &&
        localWorkflowRunId.isNotBlank()

    val preferredRemoteAsset = remoteRelease?.assets?.let(::selectPreferredAppUpdateAsset)
    val remoteDigest = preferredRemoteAsset?.sha256Digest
        ?: remoteRelease?.buildMetadata?.artifacts
            ?.firstOrNull { artifact -> preferredRemoteAsset?.name == artifact.name }
            ?.sha256
    val remoteCommit = remoteRelease?.buildMetadata?.gitCommitSha?.takeIf { it.isNotBlank() }
    val remoteRunId = remoteRelease?.buildMetadata?.workflowRunId?.takeIf { it.isNotBlank() }
    val remoteRunUrl = remoteRelease?.buildMetadata?.workflowRunUrl?.takeIf { it.isNotBlank() }
    val remoteTag = remoteRelease?.buildMetadata?.releaseTag?.takeIf { it.isNotBlank() }
        ?: remoteRelease?.latestVersion?.takeIf { it.isNotBlank() }?.let { "v$it" }
    val hasAttestation = remoteRelease?.verificationMetadata?.attestationUrl?.isNotBlank() == true
    val remoteMatchesCurrentVersion = remoteRelease?.latestVersion == normalizedCurrentVersion
    val digestMatches = localApkSha256 != null && remoteDigest != null &&
        localApkSha256.equals(remoteDigest, ignoreCase = true)
    val metadataMatchesLocalBuild = if (remoteRelease?.buildMetadata != null) {
        remoteCommit == localBuildCommitSha &&
            remoteRunId == localWorkflowRunId &&
            AppUpdateChecker.normalizeVersion(remoteTag.orEmpty()) == normalizedLocalTag
    } else {
        false
    }

    if (
        hasEmbeddedProvenance &&
        remoteRelease != null &&
        !remoteRelease.releaseIsImmutable &&
        remoteMatchesCurrentVersion &&
        metadataMatchesLocalBuild
    ) {
        return AppBuildVerificationState(
            status = AppBuildVerificationStatus.UNVERIFIED,
            summary = if (hasAttestation) {
                "已找到同版本发布与 provenance，但这个 Release 还可被修改，暂时不能当成最终证据。"
            } else {
                "已找到同版本发布，但这个 Release 还可被修改，暂时不能当成最终证据。"
            },
            sourceCommitSha = remoteCommit ?: localBuildCommitSha,
            workflowRunId = remoteRunId ?: localWorkflowRunId,
            workflowRunUrl = remoteRunUrl ?: localWorkflowRunUrl,
            releaseTag = remoteTag ?: localReleaseTag,
            localApkSha256 = localApkSha256,
            remoteApkSha256 = remoteDigest,
            releaseIsImmutable = false,
            hasAttestation = hasAttestation
        )
    }

    if (
        remoteRelease != null &&
        !remoteRelease.releaseIsImmutable &&
        remoteMatchesCurrentVersion &&
        digestMatches
    ) {
        return AppBuildVerificationState(
            status = AppBuildVerificationStatus.LIKELY_VERIFIED,
            summary = if (hasAttestation) {
                "当前安装包 SHA-256 已对上当前 GitHub Release，并已找到 provenance；但该 Release 还可被修改，暂不能当成最终校验结果。"
            } else {
                "当前安装包 SHA-256 已对上当前 GitHub Release；但该 Release 还可被修改，暂不能当成最终校验结果。"
            },
            sourceCommitSha = remoteCommit,
            workflowRunId = remoteRunId,
            workflowRunUrl = remoteRunUrl,
            releaseTag = remoteTag,
            localApkSha256 = localApkSha256,
            remoteApkSha256 = remoteDigest,
            releaseIsImmutable = false,
            hasAttestation = hasAttestation
        )
    }

    if (
        hasEmbeddedProvenance &&
        remoteRelease != null &&
        remoteRelease.releaseIsImmutable &&
        remoteMatchesCurrentVersion &&
        digestMatches &&
        metadataMatchesLocalBuild
    ) {
        return AppBuildVerificationState(
            status = AppBuildVerificationStatus.VERIFIED,
            summary = if (hasAttestation) {
                "版本、来源与 SHA-256 已对上 GitHub Release，且 Release 已锁定并附带 provenance。"
            } else {
                "版本、来源与 SHA-256 已对上 GitHub Release，且 Release 已锁定。"
            },
            sourceCommitSha = remoteCommit ?: localBuildCommitSha,
            workflowRunId = remoteRunId ?: localWorkflowRunId,
            workflowRunUrl = remoteRunUrl ?: localWorkflowRunUrl,
            releaseTag = remoteTag ?: localReleaseTag,
            localApkSha256 = localApkSha256,
            remoteApkSha256 = remoteDigest,
            releaseIsImmutable = true,
            hasAttestation = hasAttestation
        )
    }

    if (
        remoteRelease != null &&
        remoteRelease.releaseIsImmutable &&
        remoteMatchesCurrentVersion &&
        digestMatches
    ) {
        return AppBuildVerificationState(
            status = AppBuildVerificationStatus.LIKELY_VERIFIED,
            summary = if (hasAttestation) {
                "当前安装包 SHA-256 已对上 GitHub Release，且 Release 已锁定并附带 provenance；但安装包内未写入完整构建来源，只能做发布侧校验。"
            } else {
                "当前安装包 SHA-256 已对上 GitHub Release，且 Release 已锁定；但安装包内未写入完整构建来源，只能做发布侧校验。"
            },
            sourceCommitSha = remoteCommit,
            workflowRunId = remoteRunId,
            workflowRunUrl = remoteRunUrl,
            releaseTag = remoteTag,
            localApkSha256 = localApkSha256,
            remoteApkSha256 = remoteDigest,
            releaseIsImmutable = true,
            hasAttestation = hasAttestation
        )
    }

    if (hasEmbeddedProvenance) {
        val likelySummary = if (remoteRelease?.releaseIsImmutable == false) {
            if (hasAttestation) {
                "已拿到构建来源与 provenance，但对应 Release 还可被修改，先不要把它当成最终校验结果。"
            } else {
                "已拿到构建来源，但对应 Release 还可被修改，先不要把它当成最终校验结果。"
            }
        } else {
            if (hasAttestation) {
                "当前安装包已写入构建来源，并带有 provenance；再对上 Release SHA-256 就能形成完整证据链。"
            } else {
                "当前安装包已写入构建来源；再对上 Release SHA-256 就能形成更完整的证据链。"
            }
        }
        return AppBuildVerificationState(
            status = AppBuildVerificationStatus.LIKELY_VERIFIED,
            summary = likelySummary,
            sourceCommitSha = remoteCommit ?: localBuildCommitSha,
            workflowRunId = remoteRunId ?: localWorkflowRunId,
            workflowRunUrl = remoteRunUrl ?: localWorkflowRunUrl,
            releaseTag = remoteTag ?: localReleaseTag,
            localApkSha256 = localApkSha256,
            remoteApkSha256 = remoteDigest,
            releaseIsImmutable = remoteRelease?.releaseIsImmutable,
            hasAttestation = hasAttestation
        )
    }

    return AppBuildVerificationState(
        status = AppBuildVerificationStatus.UNVERIFIED,
        summary = if (hasAttestation) {
            "已发现 provenance，但还缺少足够的发布侧证据来核对当前安装包。"
        } else {
            "当前安装包缺少足够的发布侧证据，暂时无法核对源码与安装包是否一致。"
        },
        sourceCommitSha = remoteCommit,
        workflowRunId = remoteRunId,
        workflowRunUrl = remoteRunUrl,
        releaseTag = remoteTag,
        localApkSha256 = localApkSha256,
        remoteApkSha256 = remoteDigest,
        releaseIsImmutable = remoteRelease?.releaseIsImmutable,
        hasAttestation = hasAttestation
    )
}

internal fun resolveAppBuildVerificationLabel(
    status: AppBuildVerificationStatus
): String {
    return when (status) {
        AppBuildVerificationStatus.VERIFIED -> "已验证"
        AppBuildVerificationStatus.LIKELY_VERIFIED -> "基本可验证"
        AppBuildVerificationStatus.UNVERIFIED -> "未验证"
    }
}

internal fun resolveBuildSourceValue(
    commitSha: String?,
    fallback: String = "本地构建"
): String {
    val normalized = commitSha?.trim().orEmpty()
    if (normalized.isBlank() || normalized.equals("local", ignoreCase = true)) return fallback
    return normalized.take(7)
}

internal fun resolveBuildSourceSubtitle(
    workflowRunId: String?,
    releaseTag: String?
): String {
    val workflow = workflowRunId?.takeIf { it.isNotBlank() }?.let { "workflow #$it" }
    val tag = releaseTag?.takeIf { it.isNotBlank() }?.let { "tag $it" }
    return listOfNotNull(workflow, tag).joinToString(" · ").ifBlank { "未绑定 GitHub Release" }
}

internal fun resolveBuildFingerprintValue(
    sha256: String?,
    fallback: String = "未读取"
): String {
    val normalized = sha256?.trim().orEmpty()
    if (normalized.isBlank()) return fallback
    return normalized.take(12)
}

internal fun resolveBuildFingerprintSubtitle(
    localApkSha256: String?,
    remoteApkSha256: String?,
    releaseIsImmutable: Boolean?,
    hasAttestation: Boolean
): String {
    if (localApkSha256.isNullOrBlank()) {
        return "暂未读取到当前安装包 SHA-256。"
    }
    if (remoteApkSha256.isNullOrBlank()) {
        return if (hasAttestation) {
            "这是当前安装包的 SHA-256，已找到 provenance，等待发布侧摘要一起核对。"
        } else {
            "这是当前安装包的 SHA-256，可与 GitHub Release 里的摘要手动对照。"
        }
    }

    val digestMatches = localApkSha256.equals(remoteApkSha256, ignoreCase = true)
    if (!digestMatches) {
        return "当前安装包 SHA-256 与发布页摘要不一致，请确认来源。"
    }

    return when (releaseIsImmutable) {
        true -> if (hasAttestation) {
            "与 GitHub Release SHA-256 一致，Release 已锁定，含 provenance。"
        } else {
            "与 GitHub Release SHA-256 一致，Release 已锁定。"
        }
        false -> if (hasAttestation) {
            "与当前 Release SHA-256 一致，但该 Release 还可被修改。"
        } else {
            "与当前 Release SHA-256 一致，但该 Release 还可被修改。"
        }
        else -> "已读取到发布页 SHA-256，可继续结合构建来源核对。"
    }
}

internal fun resolveVerificationDialogContent(
    label: String,
    summary: String
): AppBuildInfoDialogContent {
    return AppBuildInfoDialogContent(
        title = "源码一致性",
        value = label,
        body = summary,
        actionLabel = "查看证明",
        action = AppBuildInfoDialogAction.VIEW_VERIFICATION
    )
}

internal fun resolveBuildSourceDialogContent(
    value: String,
    subtitle: String
): AppBuildInfoDialogContent {
    return AppBuildInfoDialogContent(
        title = "构建来源",
        value = value,
        body = subtitle,
        actionLabel = "查看来源",
        action = AppBuildInfoDialogAction.VIEW_BUILD_SOURCE
    )
}

internal fun resolveBuildFingerprintDialogContent(
    value: String,
    fullValue: String,
    subtitle: String
): AppBuildInfoDialogContent {
    val resolvedFullValue = fullValue.ifBlank { value }
    val body = buildString {
        append("完整 SHA-256：")
        append('\n')
        append(resolvedFullValue)
        if (subtitle.isNotBlank()) {
            append("\n\n")
            append(subtitle)
        }
    }
    return AppBuildInfoDialogContent(
        title = "SHA-256",
        value = value,
        body = body,
        actionLabel = "查看证明",
        action = AppBuildInfoDialogAction.VIEW_BUILD_FINGERPRINT
    )
}
