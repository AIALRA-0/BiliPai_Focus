package com.android.purebilibili.feature.settings

import com.android.purebilibili.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.json.JSONObject
import org.json.JSONTokener
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdateAsset(
    val name: String,
    val downloadUrl: String,
    val sizeBytes: Long,
    val contentType: String,
    val digest: String = ""
) {
    val isApk: Boolean
        get() = name.endsWith(".apk", ignoreCase = true) ||
            contentType.equals("application/vnd.android.package-archive", ignoreCase = true)

    val isBuildMetadata: Boolean
        get() = name.equals("build-metadata.json", ignoreCase = true) ||
            name.endsWith("-build-metadata.json", ignoreCase = true)

    val isChecksumsFile: Boolean
        get() = name.equals("checksums.txt", ignoreCase = true) ||
            name.endsWith("-checksums.txt", ignoreCase = true)

    val isVerificationMetadata: Boolean
        get() = name.equals("verification-metadata.json", ignoreCase = true) ||
            name.endsWith("-verification-metadata.json", ignoreCase = true)

    val sha256Digest: String?
        get() = digest
            .takeIf { it.startsWith("sha256:", ignoreCase = true) }
            ?.substringAfter(':')
            ?.trim()
            ?.takeIf { it.isNotBlank() }
}

data class AppReleaseBuildArtifact(
    val name: String,
    val sha256: String,
    val sizeBytes: Long
)

data class AppReleaseBuildMetadata(
    val schemaVersion: Int = 1,
    val appId: String = "",
    val versionName: String = "",
    val versionCode: Int = 0,
    val gitCommitSha: String = "",
    val gitRef: String = "",
    val workflowRunId: String = "",
    val workflowRunUrl: String = "",
    val releaseTag: String = "",
    val generatedAt: String? = null,
    val artifacts: List<AppReleaseBuildArtifact> = emptyList()
)

data class AppReleaseVerificationMetadata(
    val attestationUrl: String = "",
    val bundleFileName: String = "",
    val predicateType: String = ""
)

data class AppUpdateCheckResult(
    val isUpdateAvailable: Boolean,
    val currentVersion: String,
    val latestVersion: String,
    val releaseUrl: String,
    val releaseNotes: String,
    val publishedAt: String?,
    val assets: List<AppUpdateAsset>,
    val message: String,
    val releaseIsImmutable: Boolean = false,
    val buildMetadata: AppReleaseBuildMetadata? = null,
    val verificationMetadata: AppReleaseVerificationMetadata? = null
)

internal data class AppUpdateReleaseCandidate(
    val tagName: String,
    val releaseUrl: String,
    val releaseNotes: String,
    val publishedAt: String?,
    val assets: List<AppUpdateAsset>,
    val isPrerelease: Boolean,
    val isImmutable: Boolean = false,
    val buildMetadata: AppReleaseBuildMetadata? = null
)

internal data class AppUpdateEndpointSet(
    val releasesApi: String,
    val repositoryBuildGradleUrl: String,
    val repositoryUrl: String,
    val releasesPageUrl: String
)

object AppUpdateChecker {
    private const val FALLBACK_FOCUS_REPOSITORY_PATH = "AIALRA-0/BiliPai_Focus"
    private const val CONNECT_TIMEOUT_MS = 6000
    private const val READ_TIMEOUT_MS = 8000
    private val releaseJson = Json { ignoreUnknownKeys = true }

    suspend fun check(currentVersion: String): Result<AppUpdateCheckResult> = withContext(Dispatchers.IO) {
        runCatching {
            val endpointErrors = mutableListOf<String>()
            val resolved = resolveEndpointCandidates().firstNotNullOfOrNull { endpoints ->
                runCatching {
                    val releaseCandidate = fetchRemoteText(endpoints.releasesApi, required = false)
                        ?.let { body ->
                            selectLatestReleaseCandidate(
                                rawReleaseJson = body,
                                currentVersion = currentVersion,
                                releasesPageUrl = endpoints.releasesPageUrl
                            )
                        }
                    val repositoryCandidate = fetchRemoteText(
                        url = endpoints.repositoryBuildGradleUrl,
                        required = false
                    )?.let { body ->
                        parseRepositoryVersionCandidate(
                            rawBuildGradle = body,
                            repositoryUrl = endpoints.repositoryUrl
                        )
                    }?.takeIf { candidate ->
                        !candidate.isPrerelease || isPrereleaseVersion(currentVersion)
                    }
                    val preferred = selectPreferredUpdateCandidate(
                        releaseCandidate = releaseCandidate,
                        repositoryCandidate = repositoryCandidate
                    ) ?: return@runCatching null
                    endpoints to preferred
                }.onFailure { error ->
                    endpointErrors += "${endpoints.repositoryUrl}: ${error.message ?: "未知错误"}"
                }.getOrNull()
            } ?: throw IllegalStateException(
                buildString {
                    append("未获取到有效版本信息")
                    if (endpointErrors.isNotEmpty()) {
                        append("（")
                        append(endpointErrors.joinToString("；"))
                        append('）')
                    }
                }
            )

            val release = resolved.second

            val latestTag = release.tagName
            val latestVersion = normalizeVersion(latestTag)
            if (latestVersion.isEmpty()) {
                throw IllegalStateException("未获取到有效版本号")
            }

            val releaseUrl = release.releaseUrl
            val releaseNotes = release.releaseNotes
            val publishedAt = release.publishedAt
            val assets = release.assets
            val buildMetadata = assets
                .firstOrNull { it.isBuildMetadata }
                ?.downloadUrl
                ?.let { metadataUrl ->
                    fetchRemoteText(metadataUrl, required = false)
                }
                ?.let(::parseBuildMetadata)
            val verificationMetadata = assets
                .firstOrNull { it.isVerificationMetadata }
                ?.downloadUrl
                ?.let { metadataUrl ->
                    fetchRemoteText(metadataUrl, required = false)
                }
                ?.let(::parseVerificationMetadata)
            val updateAvailable = isRemoteNewer(currentVersion, latestVersion)
            val message = if (updateAvailable) {
                "发现新版本 v$latestVersion"
            } else {
                "已是最新版本"
            }

            AppUpdateCheckResult(
                isUpdateAvailable = updateAvailable,
                currentVersion = normalizeVersion(currentVersion),
                latestVersion = latestVersion,
                releaseUrl = releaseUrl,
                releaseNotes = releaseNotes,
                publishedAt = publishedAt,
                assets = assets,
                message = message,
                releaseIsImmutable = release.isImmutable,
                buildMetadata = buildMetadata,
                verificationMetadata = verificationMetadata
            )
        }
    }

    internal fun resolveEndpointCandidates(
        primary: AppUpdateEndpointSet = AppUpdateEndpointSet(
            releasesApi = BuildConfig.FOCUS_RELEASES_API_URL,
            repositoryBuildGradleUrl = BuildConfig.FOCUS_REPOSITORY_BUILD_GRADLE_URL,
            repositoryUrl = BuildConfig.FOCUS_REPOSITORY_URL,
            releasesPageUrl = BuildConfig.FOCUS_RELEASES_URL
        )
    ): List<AppUpdateEndpointSet> {
        val fallback = buildEndpointSetFromRepositoryPath(FALLBACK_FOCUS_REPOSITORY_PATH)
        return listOf(primary, fallback)
            .filter { endpoint ->
                endpoint.releasesApi.isNotBlank() &&
                    endpoint.repositoryBuildGradleUrl.isNotBlank() &&
                    endpoint.repositoryUrl.isNotBlank() &&
                    endpoint.releasesPageUrl.isNotBlank()
            }
            .distinctBy { it.repositoryUrl.trim().lowercase() }
    }

    private fun buildEndpointSetFromRepositoryPath(path: String): AppUpdateEndpointSet {
        val normalizedPath = path.trim().removePrefix("https://github.com/").removeSuffix(".git")
        val repositoryUrl = "https://github.com/$normalizedPath"
        return AppUpdateEndpointSet(
            releasesApi = "https://api.github.com/repos/$normalizedPath/releases",
            repositoryBuildGradleUrl = "https://raw.githubusercontent.com/$normalizedPath/main/app/build.gradle.kts",
            repositoryUrl = repositoryUrl,
            releasesPageUrl = "$repositoryUrl/releases"
        )
    }

    private fun fetchRemoteText(
        url: String,
        required: Boolean
    ): String? {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            setRequestProperty("User-Agent", "BiliPai-UpdateChecker")
        }
        return try {
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                if (required) {
                    throw IllegalStateException("更新接口异常: HTTP $responseCode")
                }
                return null
            }
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    internal fun normalizeVersion(version: String): String {
        return version
            .trim()
            .removePrefix("v")
            .removePrefix("V")
            .trim()
    }

    internal fun isRemoteNewer(localVersion: String, remoteVersion: String): Boolean {
        return compareVersions(
            localVersion = normalizeVersion(localVersion),
            remoteVersion = normalizeVersion(remoteVersion)
        ) < 0
    }

internal fun parseVersionParts(version: String): List<Int> {
    if (version.isBlank()) return emptyList()
    return version
        .split('.')
        .mapNotNull { part -> part.toIntOrNull() }
}

private data class ParsedVersion(
    val numericParts: List<Int>,
    val qualifiers: List<ParsedVersionQualifier>
)

private data class ParsedVersionQualifier(
    val name: String,
    val rank: Int,
    val number: Int
)

private fun parseComparableVersion(version: String): ParsedVersion {
    val normalized = normalizeVersion(version)
    val numericPrefix = normalized
        .takeWhile { it.isDigit() || it == '.' }
        .trimEnd('.')
    val suffix = normalized.removePrefix(numericPrefix)
    val qualifiers = Regex(
        pattern = """(?i)(alpha|beta|rc|focus)[\s._-]*(\d+)?"""
    ).findAll(suffix)
        .map { match ->
            val name = match.groupValues[1].lowercase()
            ParsedVersionQualifier(
                name = name,
                rank = when (name) {
                    "alpha" -> 0
                    "beta" -> 1
                    "rc" -> 2
                    "focus" -> 4
                    else -> 3
                },
                number = match.groupValues[2].toIntOrNull() ?: 0
            )
        }
        .toList()
    return ParsedVersion(
        numericParts = parseVersionParts(numericPrefix),
        qualifiers = qualifiers
    )
}

private fun compareVersions(localVersion: String, remoteVersion: String): Int {
    val local = parseComparableVersion(localVersion)
        val remote = parseComparableVersion(remoteVersion)
        val maxSize = maxOf(local.numericParts.size, remote.numericParts.size)
        for (index in 0 until maxSize) {
            val localPart = local.numericParts.getOrElse(index) { 0 }
            val remotePart = remote.numericParts.getOrElse(index) { 0 }
            if (localPart != remotePart) {
                return localPart.compareTo(remotePart)
            }
        }
    val maxQualifierCount = maxOf(local.qualifiers.size, remote.qualifiers.size)
    for (index in 0 until maxQualifierCount) {
        val localQualifier = local.qualifiers.getOrNull(index)
        val remoteQualifier = remote.qualifiers.getOrNull(index)
        if (localQualifier == null && remoteQualifier == null) break
        if (localQualifier == null) {
            return if (remoteQualifier?.name == "focus") -1 else 1
        }
        if (remoteQualifier == null) {
            return if (localQualifier.name == "focus") 1 else -1
        }
        if (localQualifier.rank != remoteQualifier.rank) {
            return localQualifier.rank.compareTo(remoteQualifier.rank)
        }
        if (localQualifier.number != remoteQualifier.number) {
            return localQualifier.number.compareTo(remoteQualifier.number)
        }
    }
    return 0
}

    private fun isPrereleaseVersion(version: String): Boolean {
        val normalized = normalizeVersion(version).lowercase()
        return normalized.contains("alpha") || normalized.contains("beta") || normalized.contains("rc")
    }

    internal fun selectLatestReleaseCandidate(
        rawReleaseJson: String,
        currentVersion: String,
        releasesPageUrl: String = BuildConfig.FOCUS_RELEASES_URL
    ): AppUpdateReleaseCandidate? {
        val releasesJson = runCatching {
            releaseJson.parseToJsonElement(rawReleaseJson).jsonArray
        }.getOrNull() ?: return null

        val allowPrerelease = isPrereleaseVersion(currentVersion)
        return releasesJson
            .mapNotNull { releaseElement ->
                parseReleaseCandidateElement(
                    releaseElement = releaseElement,
                    releasesPageUrl = releasesPageUrl
                )
            }
            .filter { !it.isPrerelease || allowPrerelease }
            .maxWithOrNull { left, right ->
                compareVersions(
                    localVersion = normalizeVersion(left.tagName),
                    remoteVersion = normalizeVersion(right.tagName)
                )
            }
    }

    internal fun parseRepositoryVersionCandidate(
        rawBuildGradle: String,
        repositoryUrl: String = BuildConfig.FOCUS_REPOSITORY_URL
    ): AppUpdateReleaseCandidate? {
        val versionName = Regex("""versionName\s*=\s*"([^"]+)"""")
            .find(rawBuildGradle)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
            .orEmpty()
        if (versionName.isBlank()) return null
        return AppUpdateReleaseCandidate(
            tagName = versionName,
            releaseUrl = repositoryUrl,
            releaseNotes = "当前版本来自仓库默认分支，尚未创建 GitHub Release",
            publishedAt = null,
            assets = emptyList(),
            isPrerelease = isPrereleaseVersion(versionName)
        )
    }

    internal fun selectPreferredUpdateCandidate(
        releaseCandidate: AppUpdateReleaseCandidate?,
        repositoryCandidate: AppUpdateReleaseCandidate?
    ): AppUpdateReleaseCandidate? {
        if (releaseCandidate == null) return repositoryCandidate
        if (repositoryCandidate == null) return releaseCandidate
        return if (
            compareVersions(
                localVersion = normalizeVersion(releaseCandidate.tagName),
                remoteVersion = normalizeVersion(repositoryCandidate.tagName)
            ) >= 0
        ) {
            releaseCandidate
        } else {
            repositoryCandidate
        }
    }

    private fun parseReleaseCandidateElement(
        releaseElement: JsonElement,
        releasesPageUrl: String
    ): AppUpdateReleaseCandidate? {
        val releaseObject = releaseElement.jsonObject
        val isDraft = releaseObject["draft"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false
        if (isDraft) return null
        val tagName = releaseObject["tag_name"]?.jsonPrimitive?.content.orEmpty().trim()
        if (tagName.isBlank()) return null
        val releaseUrl = releaseObject["html_url"]?.jsonPrimitive?.content
            ?.takeIf { it.isNotBlank() }
            ?: releasesPageUrl
        val releaseNotes = releaseObject["body"]?.jsonPrimitive?.content.orEmpty().trim()
        val publishedAt = releaseObject["published_at"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
        val isPrerelease = releaseObject["prerelease"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false
        val isImmutable = releaseObject["immutable"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false
        val assets = parseReleaseAssets(releaseObject.toString())
        return AppUpdateReleaseCandidate(
            tagName = tagName,
            releaseUrl = releaseUrl,
            releaseNotes = releaseNotes,
            publishedAt = publishedAt,
            assets = assets,
            isPrerelease = isPrerelease,
            isImmutable = isImmutable
        )
    }

    internal fun parseReleaseAssets(rawReleaseJson: String): List<AppUpdateAsset> {
        val assetsJson = runCatching {
            releaseJson
                .parseToJsonElement(rawReleaseJson)
                .jsonObject["assets"]
                ?.jsonArray
        }.getOrNull() ?: return emptyList()

        return buildList {
            for (assetElement in assetsJson) {
                val assetJson = assetElement.jsonObject
                val asset = AppUpdateAsset(
                    name = assetJson["name"]?.jsonPrimitive?.content.orEmpty().trim(),
                    downloadUrl = assetJson["browser_download_url"]?.jsonPrimitive?.content.orEmpty().trim(),
                    sizeBytes = assetJson["size"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L,
                    contentType = assetJson["content_type"]?.jsonPrimitive?.content.orEmpty().trim(),
                    digest = assetJson["digest"]?.jsonPrimitive?.content.orEmpty().trim()
                )
                if (asset.name.isBlank() || asset.downloadUrl.isBlank()) continue
                add(asset)
            }
        }
    }

    internal fun parseReleaseAssets(releaseJson: JSONObject): List<AppUpdateAsset> {
        return parseReleaseAssets(releaseJson.toString())
    }

    internal fun parseBuildMetadata(rawMetadataJson: String): AppReleaseBuildMetadata? {
        val metadataObject = runCatching {
            releaseJson.parseToJsonElement(rawMetadataJson).jsonObject
        }.getOrNull() ?: return null

        val artifacts = metadataObject["artifacts"]
            ?.jsonArray
            ?.mapNotNull { element ->
                val artifact = element.jsonObject
                val name = artifact["name"]?.jsonPrimitive?.content.orEmpty().trim()
                val sha256 = artifact["sha256"]?.jsonPrimitive?.content.orEmpty().trim()
                if (name.isBlank() || sha256.isBlank()) return@mapNotNull null
                AppReleaseBuildArtifact(
                    name = name,
                    sha256 = sha256,
                    sizeBytes = artifact["sizeBytes"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
                )
            }
            .orEmpty()

        return AppReleaseBuildMetadata(
            schemaVersion = metadataObject["schemaVersion"]?.jsonPrimitive?.content?.toIntOrNull() ?: 1,
            appId = metadataObject["appId"]?.jsonPrimitive?.content.orEmpty(),
            versionName = metadataObject["versionName"]?.jsonPrimitive?.content.orEmpty(),
            versionCode = metadataObject["versionCode"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
            gitCommitSha = metadataObject["gitCommitSha"]?.jsonPrimitive?.content.orEmpty(),
            gitRef = metadataObject["gitRef"]?.jsonPrimitive?.content.orEmpty(),
            workflowRunId = metadataObject["workflowRunId"]?.jsonPrimitive?.content.orEmpty(),
            workflowRunUrl = metadataObject["workflowRunUrl"]?.jsonPrimitive?.content.orEmpty(),
            releaseTag = metadataObject["releaseTag"]?.jsonPrimitive?.content.orEmpty(),
            generatedAt = metadataObject["generatedAt"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() },
            artifacts = artifacts
        )
    }

    internal fun parseVerificationMetadata(rawMetadataJson: String): AppReleaseVerificationMetadata? {
        val metadataObject = runCatching {
            releaseJson.parseToJsonElement(rawMetadataJson).jsonObject
        }.getOrNull() ?: return null

        val attestationUrl = metadataObject["attestationUrl"]
            ?.jsonPrimitive
            ?.content
            .orEmpty()
            .trim()
        if (attestationUrl.isBlank()) return null

        return AppReleaseVerificationMetadata(
            attestationUrl = attestationUrl,
            bundleFileName = metadataObject["bundleFileName"]
                ?.jsonPrimitive
                ?.content
                .orEmpty()
                .trim(),
            predicateType = metadataObject["predicateType"]
                ?.jsonPrimitive
                ?.content
                .orEmpty()
                .trim()
        )
    }
}
