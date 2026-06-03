package com.android.purebilibili.core.plugin.kotlinpkg

import android.content.Context
import com.android.purebilibili.core.plugin.PluginCapability
import com.android.purebilibili.core.plugin.PluginCapabilityManifest
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class ExternalPluginAuthorization(
    val pluginId: String,
    val packageSha256: String,
    val grantedCapabilities: Set<PluginCapability>,
    val signerSha256: String?,
    val approvedAtMillis: Long
)

@Serializable
data class InstalledExternalPluginPackage(
    val manifest: PluginCapabilityManifest,
    val packageSha256: String,
    val signerSha256: String?,
    val grantedCapabilities: Set<PluginCapability>,
    val packagePath: String,
    val installedAtMillis: Long,
    val enabled: Boolean = false
)

class ExternalKotlinPluginInstallStore(
    private val rootDir: File,
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val trustedPublicKeys: Map<String, ByteArray> = emptyMap()
) {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    fun previewPackage(packageBytes: ByteArray): Result<ExternalKotlinPluginPackagePreview> {
        return ExternalKotlinPluginPackageReader.preview(
            packageBytes = packageBytes,
            trustedPublicKeys = trustedPublicKeys
        )
    }

    fun installPreview(
        preview: ExternalKotlinPluginPackagePreview,
        packageBytes: ByteArray,
        grantedCapabilities: Set<PluginCapability>
    ): Result<InstalledExternalPluginPackage> {
        return runCatching {
            val verifiedPreview = previewPackage(packageBytes).getOrThrow()
            if (verifiedPreview.descriptor.packageSha256 != preview.descriptor.packageSha256) {
                throw IllegalArgumentException("插件包内容与预览 SHA-256 不一致")
            }

            val manifest = preview.descriptor.manifest
            val packageFile = packageFile(manifest.pluginId, preview.descriptor.packageSha256)
            packageFile.parentFile?.mkdirs()
            packageFile.writeBytes(packageBytes)

            val approvedCapabilities = grantedCapabilities.intersect(manifest.capabilities)
            val now = clock()
            val authorization = ExternalPluginAuthorization(
                pluginId = manifest.pluginId,
                packageSha256 = preview.descriptor.packageSha256,
                grantedCapabilities = approvedCapabilities,
                signerSha256 = preview.descriptor.signerSha256,
                approvedAtMillis = now
            )
            val installed = InstalledExternalPluginPackage(
                manifest = manifest,
                packageSha256 = preview.descriptor.packageSha256,
                signerSha256 = preview.descriptor.signerSha256,
                grantedCapabilities = approvedCapabilities,
                packagePath = packageFile.absolutePath,
                installedAtMillis = now,
                enabled = false
            )

            writeJson(authorizationFile(authorization.pluginId, authorization.packageSha256), authorization)
            writeJson(installedFile(manifest.pluginId), installed)
            installed
        }
    }

    fun listInstalledPackages(): List<InstalledExternalPluginPackage> {
        return installedDir()
            .listFiles { file -> file.extension == "json" }
            ?.sortedBy { it.name }
            ?.mapNotNull { file ->
                runCatching {
                    json.decodeFromString(InstalledExternalPluginPackage.serializer(), file.readText())
                }.getOrNull()
            }
            ?: emptyList()
    }

    fun getAuthorization(
        pluginId: String,
        packageSha256: String
    ): ExternalPluginAuthorization? {
        val file = authorizationFile(pluginId, packageSha256)
        if (!file.exists()) return null
        return runCatching {
            json.decodeFromString(ExternalPluginAuthorization.serializer(), file.readText())
        }.getOrNull()
    }

    fun removeInstalledPackage(pluginId: String): Boolean {
        val installed = listInstalledPackages().find { it.manifest.pluginId == pluginId }
        val metadataDeleted = installedFile(pluginId).deleteIfExists()
        val packageDeleted = installed?.packagePath?.let { File(it).deleteIfExists() } ?: false
        packageDir(pluginId).deleteIfEmpty()
        return metadataDeleted || packageDeleted
    }

    fun revokeAuthorization(
        pluginId: String,
        packageSha256: String
    ): Boolean {
        return authorizationFile(pluginId, packageSha256).deleteIfExists()
    }

    companion object {
        fun createDefault(
            context: Context,
            trustedPublicKeys: Map<String, ByteArray> = emptyMap()
        ): ExternalKotlinPluginInstallStore {
            return ExternalKotlinPluginInstallStore(
                rootDir = File(context.filesDir, "external_kotlin_plugins"),
                trustedPublicKeys = trustedPublicKeys
            )
        }
    }

    private fun packageFile(pluginId: String, packageSha256: String): File {
        return File(packageDir(pluginId), "$packageSha256.bpplugin")
    }

    private fun packageDir(pluginId: String): File {
        return File(packagesDir(), pluginId.safeFileSegment())
    }

    private fun packagesDir(): File = File(rootDir, "packages")

    private fun installedDir(): File = File(rootDir, "installed").also { it.mkdirs() }

    private fun authorizationDir(pluginId: String): File {
        return File(rootDir, "authorizations/${pluginId.safeFileSegment()}").also { it.mkdirs() }
    }

    private fun installedFile(pluginId: String): File {
        return File(installedDir(), "${pluginId.safeFileSegment()}.json")
    }

    private fun authorizationFile(pluginId: String, packageSha256: String): File {
        return File(authorizationDir(pluginId), "${packageSha256.safeFileSegment()}.json")
    }

    private inline fun <reified T> writeJson(file: File, value: T) {
        file.parentFile?.mkdirs()
        file.writeText(json.encodeToString(value))
    }
}

private fun File.deleteIfExists(): Boolean {
    return exists() && delete()
}

private fun File.deleteIfEmpty(): Boolean {
    val children = listFiles()
    return children != null && children.isEmpty() && delete()
}

private fun String.safeFileSegment(): String {
    return replace(Regex("[^A-Za-z0-9_.-]"), "_")
}
