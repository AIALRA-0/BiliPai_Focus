package com.android.purebilibili.core.plugin.skin

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayInputStream
import java.io.File
import java.util.zip.ZipInputStream

class UiSkinInstallStore(
    private val rootDir: File,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    fun previewPackage(packageBytes: ByteArray): Result<UiSkinPackagePreview> {
        return UiSkinPackageReader.preview(packageBytes)
    }

    fun installPreview(
        preview: UiSkinPackagePreview,
        packageBytes: ByteArray
    ): Result<InstalledUiSkinPackage> {
        return runCatching {
            val verifiedPreview = previewPackage(packageBytes).getOrThrow()
            if (verifiedPreview.packageSha256 != preview.packageSha256) {
                throw IllegalArgumentException("皮肤包内容与预览 SHA-256 不一致")
            }
            val packageFile = packageFile(preview.manifest.skinId, preview.packageSha256)
            packageFile.parentFile?.mkdirs()
            packageFile.writeBytes(packageBytes)
            val assetFiles = extractDeclaredAssets(
                preview = preview,
                packageBytes = packageBytes
            )
            val installed = InstalledUiSkinPackage(
                manifest = preview.manifest,
                packageSha256 = preview.packageSha256,
                packagePath = packageFile.absolutePath,
                installedAtMillis = clock(),
                enabled = false,
                assetFiles = assetFiles
            )
            writeJson(installedFile(installed.installId), installed)
            deleteInstalledRecordsForSameSkin(
                skinId = installed.skinId,
                keepInstallId = installed.installId
            )
            installed
        }
    }

    fun listInstalledPackages(): List<InstalledUiSkinPackage> {
        val external = installedDir()
            .listFiles { file -> file.extension == "json" }
            ?.sortedBy { it.name }
            ?.mapNotNull { file ->
                runCatching {
                    json.decodeFromString(InstalledUiSkinPackage.serializer(), file.readText())
                }.getOrNull()
            }
            ?: emptyList()
        val deduplicatedExternal = external
            .groupBy { it.skinId }
            .values
            .map { records ->
                records.maxWith(
                    compareBy<InstalledUiSkinPackage> { it.installedAtMillis }
                        .thenBy { it.installId }
                )
            }
            .sortedBy { it.displayName }
        return deduplicatedExternal
    }

    fun deleteInstalledPackage(installId: String): Result<Boolean> {
        return runCatching {
            val recordFile = installedFile(installId)
            if (!recordFile.exists()) {
                return@runCatching false
            }
            val installed = json.decodeFromString(InstalledUiSkinPackage.serializer(), recordFile.readText())
            if (!recordFile.delete()) {
                throw IllegalArgumentException("皮肤安装记录删除失败")
            }
            File(installed.packagePath).delete()
            assetDir(installed.skinId, installed.packageSha256).deleteRecursively()
            packageDir(installed.skinId).deleteIfEmpty()
            File(assetsDir(), installed.skinId.safeUiSkinFileSegment()).deleteIfEmpty()
            true
        }
    }

    fun extractPreviewAssetFiles(
        preview: UiSkinPackagePreview,
        packageBytes: ByteArray
    ): Result<Map<String, String>> {
        return runCatching {
            val verifiedPreview = previewPackage(packageBytes).getOrThrow()
            if (verifiedPreview.packageSha256 != preview.packageSha256) {
                throw IllegalArgumentException("皮肤包内容与预览 SHA-256 不一致")
            }
            val assetFiles = extractDeclaredAssets(
                preview = preview,
                packageBytes = packageBytes,
                assetRoot = previewAssetDir(preview.packageSha256)
            )
            assetFiles
        }
    }

    companion object {
        fun createDefault(context: Context): UiSkinInstallStore {
            return UiSkinInstallStore(
                rootDir = File(context.filesDir, "ui_skins")
            )
        }
    }

    private fun packageFile(skinId: String, packageSha256: String): File {
        return File(packageDir(skinId), "$packageSha256.bpskin")
    }

    private fun packageDir(skinId: String): File {
        return File(packagesDir(), skinId.safeUiSkinFileSegment())
    }

    private fun packagesDir(): File = File(rootDir, "packages")

    private fun installedDir(): File = File(rootDir, "installed").also { it.mkdirs() }

    private fun installedFile(installId: String): File {
        return File(installedDir(), "${installId.safeUiSkinFileSegment()}.json")
    }

    private fun deleteInstalledRecordsForSameSkin(
        skinId: String,
        keepInstallId: String
    ) {
        installedDir()
            .listFiles { file -> file.extension == "json" }
            ?.forEach { file ->
                val installed = runCatching {
                    json.decodeFromString(InstalledUiSkinPackage.serializer(), file.readText())
                }.getOrNull()
                if (installed?.skinId == skinId && installed.installId != keepInstallId) {
                    file.delete()
                }
            }
    }

    private fun extractDeclaredAssets(
        preview: UiSkinPackagePreview,
        packageBytes: ByteArray,
        assetRoot: File = assetDir(preview.manifest.skinId, preview.packageSha256)
    ): Map<String, String> {
        val declaredPaths = preview.assetEntries.mapTo(linkedSetOf()) { it.path }
        if (declaredPaths.isEmpty()) return emptyMap()
        assetRoot.deleteRecursively()
        assetRoot.mkdirs()
        val assetFiles = linkedMapOf<String, String>()

        ZipInputStream(ByteArrayInputStream(packageBytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val normalizedName = entry.name
                    .replace('\\', '/')
                    .split('/')
                    .filter { it.isNotEmpty() && it != "." }
                    .joinToString("/")
                if (!entry.isDirectory && normalizedName in declaredPaths) {
                    val relativeAssetPath = normalizedName.removePrefix("assets/")
                    val target = File(assetRoot, relativeAssetPath)
                    ensureInsideDirectory(assetRoot, target)
                    target.parentFile?.mkdirs()
                    target.outputStream().use { output -> zip.copyTo(output) }
                    assetFiles[normalizedName] = target.absolutePath
                }
                zip.closeEntry()
            }
        }
        if (assetFiles.keys != declaredPaths) {
            throw IllegalArgumentException("皮肤包资源解压不完整")
        }
        return assetFiles
    }

    private fun assetDir(skinId: String, packageSha256: String): File {
        return File(File(assetsDir(), skinId.safeUiSkinFileSegment()), packageSha256)
    }

    private fun assetsDir(): File = File(rootDir, "assets")

    private fun previewAssetDir(packageSha256: String): File {
        return File(File(rootDir, "preview_assets"), packageSha256)
    }

    private fun ensureInsideDirectory(root: File, target: File) {
        val rootPath = root.canonicalFile.toPath()
        val targetPath = target.canonicalFile.toPath()
        if (!targetPath.startsWith(rootPath)) {
            throw IllegalArgumentException("皮肤资源路径越界")
        }
    }

    private fun File.deleteIfEmpty() {
        if (isDirectory && listFiles()?.isEmpty() == true) {
            delete()
        }
    }

    private inline fun <reified T> writeJson(file: File, value: T) {
        file.parentFile?.mkdirs()
        file.writeText(json.encodeToString(value))
    }
}
