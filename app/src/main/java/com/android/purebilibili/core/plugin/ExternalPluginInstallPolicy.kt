package com.android.purebilibili.core.plugin

data class ExternalPluginPackageDescriptor(
    val manifest: PluginCapabilityManifest,
    val packageSha256: String,
    val signerSha256: String?
)

sealed interface ExternalPluginInstallDecision {
    data class RequiresUserApproval(
        val manifest: PluginCapabilityManifest,
        val packageSha256: String,
        val signerTrusted: Boolean,
        val sensitiveCapabilities: Set<PluginCapability>
    ) : ExternalPluginInstallDecision

    data class Rejected(
        val reason: String
    ) : ExternalPluginInstallDecision
}

private val SENSITIVE_PLUGIN_CAPABILITIES = setOf(
    PluginCapability.PLAYER_CONTROL,
    PluginCapability.PLAYBACK_CDN,
    PluginCapability.DANMAKU_MUTATION,
    PluginCapability.LOCAL_HISTORY_READ,
    PluginCapability.LOCAL_FEEDBACK_READ,
    PluginCapability.NETWORK,
    PluginCapability.PLUGIN_STORAGE
)

fun evaluateExternalPluginInstall(
    packageDescriptor: ExternalPluginPackageDescriptor,
    trustedSignerSha256: Set<String>,
    supportedApiVersion: Int = 1
): ExternalPluginInstallDecision {
    val manifest = packageDescriptor.manifest
    if (manifest.apiVersion != supportedApiVersion) {
        return ExternalPluginInstallDecision.Rejected(
            "不支持的插件 API 版本: ${manifest.apiVersion}"
        )
    }
    if (packageDescriptor.packageSha256.isBlank()) {
        return ExternalPluginInstallDecision.Rejected("插件包缺少 SHA-256 校验值")
    }
    if (manifest.entryClassName.isBlank()) {
        return ExternalPluginInstallDecision.Rejected("插件入口类不能为空")
    }

    val signerTrusted = packageDescriptor.signerSha256 != null &&
        packageDescriptor.signerSha256 in trustedSignerSha256

    return ExternalPluginInstallDecision.RequiresUserApproval(
        manifest = manifest,
        packageSha256 = packageDescriptor.packageSha256,
        signerTrusted = signerTrusted,
        sensitiveCapabilities = manifest.capabilities.intersect(SENSITIVE_PLUGIN_CAPABILITIES)
    )
}
