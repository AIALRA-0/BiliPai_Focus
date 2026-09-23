package com.android.purebilibili.core.plugin.js

import com.android.purebilibili.core.plugin.PluginCapability
import kotlinx.serialization.Serializable

@Serializable
data class BiliPaiJsPluginManifest(
    val id: String,
    val title: String,
    val version: String = "1.0.0",
    val author: String = "",
    val description: String = "",
    val modules: List<BiliPaiJsModule> = emptyList(),
    val permissions: Set<PluginCapability> = emptySet()
)

@Serializable
data class BiliPaiJsModule(
    val id: String = "",
    val title: String,
    val description: String = "",
    val functionName: String,
    val params: List<BiliPaiJsParam> = emptyList(),
    /** `feed` 时宿主自己请求并解析 RSS/Atom，不调用 [functionName]。 */
    val kind: String = ""
)

@Serializable
data class BiliPaiJsParam(
    val name: String,
    val title: String,
    val type: String = "text",
    val defaultValue: String = "",
    val options: List<BiliPaiJsEnumOption> = emptyList()
)

@Serializable
data class BiliPaiJsEnumOption(
    val title: String,
    val value: String
)

@Serializable
data class BiliPaiJsMediaItem(
    val id: String,
    val title: String,
    val description: String = "",
    val coverUrl: String? = null,
    val coverUrls: List<String> = emptyList(),
    val backdropUrl: String? = null,
    val backdropUrls: List<String> = emptyList(),
    val backdropPath: String? = null,
    val backdropPaths: List<String> = emptyList(),
    val posterPath: String? = null,
    val posterPaths: List<String> = emptyList(),
    val type: String = "video",
    val videoUrl: String? = null,
    val streams: List<BiliPaiJsMediaStream> = emptyList(),
    val childItems: List<BiliPaiJsMediaItem> = emptyList()
)

@Serializable
data class BiliPaiJsMediaStream(
    val id: String = "primary",
    val title: String = "默认线路",
    val url: String,
    val contentType: String? = null,
    val headers: Map<String, String> = emptyMap()
)

val BiliPaiJsMediaItem.isPlayable: Boolean
    get() = !videoUrl.isNullOrBlank() ||
        streams.any { it.url.isNotBlank() } ||
        childItems.any { it.isPlayable }

val BiliPaiJsMediaItem.hasNoImageCandidate: Boolean
    get() = resolveBiliPaiJsMediaImageCandidates(this).isEmpty()

fun resolveBiliPaiJsMediaImageCandidates(item: BiliPaiJsMediaItem): List<String> {
    return buildList {
        item.backdropUrl?.takeIf { it.isNotBlank() }?.let(::add)
        addAll(item.backdropUrls.filter { it.isNotBlank() })
        item.backdropPath?.takeIf { it.isNotBlank() }?.let(::add)
        addAll(item.backdropPaths.filter { it.isNotBlank() })
        item.coverUrl?.takeIf { it.isNotBlank() }?.let(::add)
        addAll(item.coverUrls.filter { it.isNotBlank() })
        item.posterPath?.takeIf { it.isNotBlank() }?.let(::add)
        addAll(item.posterPaths.filter { it.isNotBlank() })
    }.distinct()
}

fun resolveBiliPaiJsMediaStreams(item: BiliPaiJsMediaItem): List<BiliPaiJsMediaStream> {
    return buildList {
        item.videoUrl?.takeIf { it.isNotBlank() }?.let { url ->
            add(
                BiliPaiJsMediaStream(
                    id = "primary",
                    title = "默认线路",
                    url = url
                )
            )
        }
        addAll(item.streams.filter { it.url.isNotBlank() })
        item.childItems.forEach { child ->
            val childStream = resolveBiliPaiJsMediaStreams(child).firstOrNull() ?: return@forEach
            add(
                childStream.copy(
                    id = child.id,
                    title = child.title
                )
            )
        }
    }
}

fun validateBiliPaiJsPluginManifest(manifest: BiliPaiJsPluginManifest): String? {
    if (!pluginIdRegex.matches(manifest.id)) {
        return "JS 插件 ID 格式无效，仅支持字母数字/._-"
    }
    if (manifest.title.isBlank()) {
        return "JS 插件标题不能为空"
    }
    if (manifest.modules.isEmpty()) {
        return "JS 插件至少需要声明一个模块"
    }
    manifest.modules.forEach { module ->
        if (!functionNameRegex.matches(module.functionName)) {
            return "JS 插件模块函数名格式无效: ${module.functionName}"
        }
        if (module.title.isBlank()) {
            return "JS 插件模块标题不能为空"
        }
    }
    val knownPermissions = PluginCapability.entries.toSet()
    val unknownPermission = manifest.permissions.firstOrNull { it !in knownPermissions }
    if (unknownPermission != null) {
        return "JS 插件声明了未知权限: $unknownPermission"
    }
    return null
}

fun resolveBiliPaiJsPluginCapabilities(manifest: BiliPaiJsPluginManifest): Set<PluginCapability> {
    return manifest.permissions
}

private val pluginIdRegex = Regex("^[A-Za-z0-9_.-]{1,64}$")
private val functionNameRegex = Regex("^[A-Za-z_$][A-Za-z0-9_$]{0,63}$")
