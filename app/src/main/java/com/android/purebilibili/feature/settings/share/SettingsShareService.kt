package com.android.purebilibili.feature.settings.share

import android.content.Context
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.util.DisplayMetrics
import androidx.core.content.FileProvider
import com.android.purebilibili.BuildConfig
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.core.store.settingsDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.time.Instant

internal const val DEFAULT_SETTINGS_SHARE_PROFILE_NAME = "BiliPai 设置分享"
internal const val LIQUID_GLASS_SETTINGS_SHARE_PROFILE_NAME = "BiliPai 液态玻璃设置"
private const val DEFAULT_PROFILE_ASSET = "default_settings_profile.json"
private const val DEFAULT_PROFILE_PREFS = "settings_profile_defaults"
private const val DEFAULT_PROFILE_APPLIED_KEY = "bundled_default_applied_v1"
private val PRIVATE_DEFAULT_PROFILE_KEYS = setOf("home_wallpaper_uri")

interface SettingsShareServiceContract {
    suspend fun exportToUri(
        uri: Uri,
        profileName: String = DEFAULT_SETTINGS_SHARE_PROFILE_NAME,
        includeDeviceDebug: Boolean = true,
    ): Result<SettingsShareExportArtifact>

    suspend fun createShareUri(
        profileName: String = DEFAULT_SETTINGS_SHARE_PROFILE_NAME,
        includeDeviceDebug: Boolean = true,
    ): Result<Uri>

    suspend fun readImportSession(uri: Uri): Result<SettingsShareImportSession>

    suspend fun applyImport(session: SettingsShareImportSession): Result<SettingsShareApplyResult>

    suspend fun listSavedProfiles(): Result<List<SavedSettingsProfile>> = Result.success(emptyList())

    suspend fun saveCurrentProfile(name: String): Result<SavedSettingsProfile> =
        Result.failure(UnsupportedOperationException("保存配置不可用"))

    suspend fun restoreSavedProfile(profile: SavedSettingsProfile): Result<SettingsShareApplyResult> =
        Result.failure(UnsupportedOperationException("恢复配置不可用"))
}

class SettingsShareService(private val context: Context) : SettingsShareServiceContract {

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    private val savedProfilesDirectory: File
        get() = File(context.filesDir, "settings-profiles")

    /** Applies the bundled profile once, only for a genuinely new settings store. */
    suspend fun applyBundledDefaultIfNeeded(): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val marker = context.getSharedPreferences(DEFAULT_PROFILE_PREFS, Context.MODE_PRIVATE)
            if (marker.getBoolean(DEFAULT_PROFILE_APPLIED_KEY, false)) return@runCatching false
            if (context.settingsDataStore.data.first().asMap().isNotEmpty()) {
                marker.edit().putBoolean(DEFAULT_PROFILE_APPLIED_KEY, true).apply()
                return@runCatching false
            }
            val profile = context.assets.open(DEFAULT_PROFILE_ASSET).use { input ->
                decodeProfile(input.bufferedReader(Charsets.UTF_8).readText())
            }
            SettingsManager.applyShareableSettingsSnapshot(
                context = context,
                settings = flattenSettingsShareSections(profile.sections)
                    .filterKeys { it !in PRIVATE_DEFAULT_PROFILE_KEYS },
            )
            SettingsManager.markHomeVisualDefaultsCurrent(context)
            marker.edit().putBoolean(DEFAULT_PROFILE_APPLIED_KEY, true).apply()
            true
        }
    }

    suspend fun createExportArtifact(
        profileName: String = DEFAULT_SETTINGS_SHARE_PROFILE_NAME,
        includeDeviceDebug: Boolean = true,
    ): SettingsShareExportArtifact = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val rawSettings = SettingsManager.exportShareableSettingsSnapshot(context)
        val definitions = SettingsManager.getShareableSettingsEntryDefinitions()
        val deviceDebug = if (includeDeviceDebug) {
            captureDeviceDebugInfo(context, rawSettings)
        } else {
            null
        }
        val profile = buildSettingsShareProfile(
            profileName = profileName,
            appVersion = BuildConfig.VERSION_NAME,
            exportedAtIso = Instant.ofEpochMilli(now).toString(),
            rawSettings = rawSettings,
            definitions = definitions,
            deviceDebug = deviceDebug,
        )
        SettingsShareExportArtifact(
            fileName = buildSettingsShareFileName(
                appVersion = BuildConfig.VERSION_NAME,
                epochMs = now
            ),
            json = json.encodeToString(SettingsShareProfile.serializer(), profile),
            profile = profile
        )
    }

    internal suspend fun createLiquidGlassExportArtifact(): SettingsShareExportArtifact =
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val profile = buildSettingsShareProfile(
                profileName = LIQUID_GLASS_SETTINGS_SHARE_PROFILE_NAME,
                appVersion = BuildConfig.VERSION_NAME,
                exportedAtIso = Instant.ofEpochMilli(now).toString(),
                rawSettings = SettingsManager.exportLiquidGlassShareableSettingsSnapshot(context),
                definitions = SettingsManager
                    .getLiquidGlassShareableSettingsEntryDefinitions(),
                deviceDebug = null,
            )
            SettingsShareExportArtifact(
                fileName = buildLiquidGlassSettingsShareFileName(
                    appVersion = BuildConfig.VERSION_NAME,
                    epochMs = now,
                ),
                json = json.encodeToString(SettingsShareProfile.serializer(), profile),
                profile = profile,
            )
        }

    override suspend fun exportToUri(
        uri: Uri,
        profileName: String,
        includeDeviceDebug: Boolean,
    ): Result<SettingsShareExportArtifact> = withContext(Dispatchers.IO) {
        runCatching {
            val artifact = createExportArtifact(
                profileName = profileName,
                includeDeviceDebug = includeDeviceDebug,
            )
            context.contentResolver.openOutputStream(uri)?.use { output ->
                output.write(artifact.json.toByteArray(Charsets.UTF_8))
            } ?: error("无法写入导出文件")
            artifact
        }
    }

    override suspend fun createShareUri(
        profileName: String,
        includeDeviceDebug: Boolean,
    ): Result<Uri> = withContext(Dispatchers.IO) {
        runCatching {
            val artifact = createExportArtifact(
                profileName = profileName,
                includeDeviceDebug = includeDeviceDebug,
            )
            writeShareArtifactToCache(artifact)
        }
    }

    suspend fun createLiquidGlassShareUri(): Result<Uri> = withContext(Dispatchers.IO) {
        runCatching {
            val artifact = createLiquidGlassExportArtifact()
            writeShareArtifactToCache(artifact)
        }
    }

    private fun writeShareArtifactToCache(artifact: SettingsShareExportArtifact): Uri {
        val shareDir = File(context.cacheDir, "logs/settings-share").apply { mkdirs() }
        val shareFile = File(shareDir, artifact.fileName)
        shareFile.writeText(artifact.json, Charsets.UTF_8)
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            shareFile,
        )
    }

    private fun captureDeviceDebugInfo(
        context: Context,
        rawSettings: Map<String, kotlinx.serialization.json.JsonElement>,
    ): SettingsShareDeviceDebugInfo {
        val metrics: DisplayMetrics = context.resources.displayMetrics
        val config: Configuration = context.resources.configuration
        val (uiPresetValue, nativeVariantValue) = resolveDebugThemeValues(rawSettings)
        val nightMask = config.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return buildSettingsShareDeviceDebugInfo(
            androidSdkInt = Build.VERSION.SDK_INT,
            androidRelease = Build.VERSION.RELEASE.orEmpty(),
            securityPatch = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Build.VERSION.SECURITY_PATCH.orEmpty()
            } else {
                ""
            },
            manufacturer = Build.MANUFACTURER.orEmpty(),
            brand = Build.BRAND.orEmpty(),
            model = Build.MODEL.orEmpty(),
            device = Build.DEVICE.orEmpty(),
            product = Build.PRODUCT.orEmpty(),
            hardware = Build.HARDWARE.orEmpty(),
            displayId = Build.DISPLAY.orEmpty(),
            widthPixels = metrics.widthPixels,
            heightPixels = metrics.heightPixels,
            density = metrics.density,
            densityDpi = metrics.densityDpi,
            scaledDensity = metrics.scaledDensity,
            xdpi = metrics.xdpi,
            ydpi = metrics.ydpi,
            widthDp = config.screenWidthDp.toFloat(),
            heightDp = config.screenHeightDp.toFloat(),
            smallestWidthDp = config.smallestScreenWidthDp,
            fontScale = config.fontScale,
            uiModeNight = nightMask == Configuration.UI_MODE_NIGHT_YES,
            uiPresetValue = uiPresetValue,
            uiPresetName = resolveUiPresetNameFromValue(uiPresetValue),
            androidNativeVariantValue = nativeVariantValue,
            androidNativeVariantName = resolveAndroidNativeVariantNameFromValue(nativeVariantValue),
            appVersionName = BuildConfig.VERSION_NAME,
            appVersionCode = BuildConfig.VERSION_CODE.toLong(),
        )
    }

    override suspend fun readImportSession(uri: Uri): Result<SettingsShareImportSession> =
        withContext(Dispatchers.IO) {
            runCatching {
                val rawJson = context.contentResolver.openInputStream(uri)?.use { input ->
                    input.bufferedReader(Charsets.UTF_8).readText()
                } ?: error("无法读取导入文件")
                val profile = decodeProfile(rawJson)
                // 旧格式文件在导入边界归一化为新键 theme_selection_v1，
                // 预览与回写都基于归一化后的 profile（旧键仅用于旧文件导入兼容）。
                val normalizedProfile = normalizeThemeSelectionForImport(profile.sections)
                    .let { sections ->
                        if (sections == profile.sections) profile else profile.copy(sections = sections)
                    }
                val preview = resolveSettingsShareImportPreview(
                    profile = normalizedProfile,
                    definitions = SettingsManager.getShareableSettingsEntryDefinitions()
                )
                SettingsShareImportSession(
                    profile = normalizedProfile,
                    preview = preview,
                    rawJson = rawJson
                )
            }
        }

    suspend fun readLiquidGlassImportSession(uri: Uri): Result<SettingsShareImportSession> =
        withContext(Dispatchers.IO) {
            runCatching {
                val rawJson = context.contentResolver.openInputStream(uri)?.use { input ->
                    input.bufferedReader(Charsets.UTF_8).readText()
                } ?: error("无法读取导入文件")
                val profile = decodeProfile(rawJson)
                val definitions = SettingsManager.getLiquidGlassShareableSettingsEntryDefinitions()
                val allowedKeys = definitions.mapTo(linkedSetOf()) { it.storageKey }
                val filteredSections = filterSettingsShareSections(profile.sections, allowedKeys)
                if (flattenSettingsShareSections(filteredSections).isEmpty()) {
                    error("所选文件中没有可导入的液态玻璃设置")
                }
                val filteredProfile = profile.copy(sections = filteredSections)
                SettingsShareImportSession(
                    profile = filteredProfile,
                    preview = resolveSettingsShareImportPreview(
                        profile = filteredProfile,
                        definitions = definitions,
                    ),
                    rawJson = rawJson,
                )
            }
        }

    suspend fun applyLiquidGlassImport(
        session: SettingsShareImportSession,
    ): Result<SettingsShareApplyResult> = withContext(Dispatchers.IO) {
        runCatching {
            val allowedKeys = SettingsManager.getLiquidGlassShareableSettingsEntryDefinitions()
                .mapTo(linkedSetOf()) { it.storageKey }
            val settings = flattenSettingsShareSections(session.profile.sections)
                .filterKeys(allowedKeys::contains)
            if (settings.isEmpty()) {
                error("导入文件中没有可应用的液态玻璃设置")
            }
            SettingsManager.applyShareableSettingsSnapshot(
                context = context,
                settings = settings,
            )
        }
    }

    override suspend fun applyImport(session: SettingsShareImportSession): Result<SettingsShareApplyResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                val settings = flattenSettingsShareSections(session.profile.sections)
                SettingsManager.applyShareableSettingsSnapshot(
                    context = context,
                    settings = settings
                )
            }
        }

    override suspend fun listSavedProfiles(): Result<List<SavedSettingsProfile>> =
        withContext(Dispatchers.IO) {
            runCatching {
                savedProfilesDirectory.listFiles()
                    .orEmpty()
                    .filter { it.isFile && it.extension == "json" }
                    .mapNotNull { file ->
                        runCatching {
                            val profile = decodeProfile(file.readText(Charsets.UTF_8))
                            SavedSettingsProfile(
                                name = profile.profileName,
                                fileName = file.name,
                                exportedAtIso = profile.exportedAtIso,
                            )
                        }.getOrNull()
                    }
                    .sortedByDescending { it.exportedAtIso }
            }
        }

    override suspend fun saveCurrentProfile(name: String): Result<SavedSettingsProfile> =
        withContext(Dispatchers.IO) {
            runCatching {
                val normalizedName = name.trim().take(80).ifBlank { error("配置名不能为空") }
                val artifact = createExportArtifact(
                    profileName = normalizedName,
                    includeDeviceDebug = false,
                )
                val fileName = "${sanitizeProfileFileName(normalizedName)}-${System.currentTimeMillis()}.json"
                savedProfilesDirectory.mkdirs()
                File(savedProfilesDirectory, fileName).writeText(artifact.json, Charsets.UTF_8)
                SavedSettingsProfile(
                    name = normalizedName,
                    fileName = fileName,
                    exportedAtIso = artifact.profile.exportedAtIso,
                )
            }
        }

    override suspend fun restoreSavedProfile(
        profile: SavedSettingsProfile,
    ): Result<SettingsShareApplyResult> = withContext(Dispatchers.IO) {
        runCatching {
            val file = File(savedProfilesDirectory, profile.fileName)
            require(file.parentFile?.canonicalFile == savedProfilesDirectory.canonicalFile) {
                "无效的配置文件"
            }
            val saved = decodeProfile(file.readText(Charsets.UTF_8))
            SettingsManager.applyShareableSettingsSnapshot(
                context = context,
                settings = flattenSettingsShareSections(saved.sections),
            )
        }
    }

    private fun decodeProfile(rawJson: String): SettingsShareProfile {
        return json.decodeFromString(SettingsShareProfile.serializer(), rawJson)
    }

    private fun sanitizeProfileFileName(name: String): String = name
        .replace(Regex("[^\\p{L}\\p{N}_-]+"), "_")
        .trim('_')
        .ifBlank { "profile" }
}
