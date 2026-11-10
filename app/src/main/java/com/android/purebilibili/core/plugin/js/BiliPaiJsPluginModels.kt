package com.android.purebilibili.core.plugin.js

import com.android.purebilibili.plugin.sdk.PluginCapability
import kotlinx.serialization.Serializable

@Serializable
data class BiliPaiJsPluginManifest(
    val id: String,
    val title: String,
    val version: String = "1.0.0",
    val author: String = "",
    val description: String = "",
    val modules: List<BiliPaiJsModule> = emptyList(),
    val permissions: Set<PluginCapability> = emptySet(),
    /** 非空时宿主在用户点开带 `link` 的条目时调用该函数，参数为 `{"link": ...}`。 */
    val detailFunctionName: String = "",
    /** 详情结果缓存秒数，默认 60；0 表示不缓存。 */
    val detailCacheDuration: Long = 60L,
    /** 非空时宿主在外部播放器按 `{"title": ...}` 调用该函数获取第三方弹幕。 */
    val danmakuFunctionName: String = "",
    /** 弹幕结果缓存秒数，默认 300；0 表示不缓存。 */
    val danmakuCacheDuration: Long = 300L,
    /** 插件要求的宿主 JS API 版本；高于 [BiliPaiJsApiLevel.CURRENT] 时拒绝安装。 */
    val requiredApi: Int = 1
) {
    val supportsDetail: Boolean
        get() = detailFunctionName.isNotBlank()

    val supportsDanmaku: Boolean
        get() = danmakuFunctionName.isNotBlank()
}

/** 宿主对外暴露的 JS 插件 API 版本。新增宿主能力时递增并在下方注明。 */
object BiliPaiJsApiLevel {
    /** 初版：BiliPai.http / storage / log。 */
    const val BASE = 1

    /** 新增 BiliPai.dom、page/offset/count 参数、cacheDuration、link + loadDetail 导航。 */
    const val DOM_AND_NAVIGATION = 2

    const val CURRENT = DOM_AND_NAVIGATION
}

@Serializable
data class BiliPaiJsModule(
    val id: String = "",
    val title: String,
    val description: String = "",
    val functionName: String,
    val params: List<BiliPaiJsParam> = emptyList(),
    /** `feed` 时宿主自己请求并解析 RSS/Atom，不调用 [functionName]。 */
    val kind: String = "",
    /** 结果缓存秒数；`0` 表示不缓存（默认）。宿主按 `pluginId + moduleId + params` 缓存模块返回值。 */
    val cacheDuration: Long = 0L,
    /** 内容布局：`list`（默认单列）或 `grid`（海报网格）。 */
    val layout: String = BiliPaiJsLayouts.LIST
)

@Serializable
data class BiliPaiJsParam(
    val name: String,
    val title: String,
    val type: String = BiliPaiJsParamTypes.TEXT,
    val defaultValue: String = "",
    val options: List<BiliPaiJsEnumOption> = emptyList()
)

object BiliPaiJsParamTypes {
    const val TEXT = "text"
    const val ENUM = "enum"
    /** 宿主在用户滚到底时自动递增的页码，模块函数收到数字。 */
    const val PAGE = "page"
    /** 每页数量，模块函数收到数字。 */
    const val COUNT = "count"
    /** 宿主写入的已加载条数偏移，模块函数收到数字，用户不可见。 */
    const val OFFSET = "offset"

    val ALL = setOf(TEXT, ENUM, PAGE, COUNT, OFFSET)
}

/** 模块内容布局：`grid` 为海报网格，其余按单列列表渲染。 */
object BiliPaiJsLayouts {
    const val LIST = "list"
    const val GRID = "grid"

    val ALL = setOf(LIST, GRID)
}

/** [BiliPaiJsParamTypes.PAGE] 与 [BiliPaiJsParamTypes.OFFSET] 由宿主驱动，不进参数表单。 */
val BiliPaiJsParam.isHostDriven: Boolean
    get() = type == BiliPaiJsParamTypes.PAGE || type == BiliPaiJsParamTypes.OFFSET

/** 有 options 的参数适合用选项 chip 展示而不是自由输入。 */
val BiliPaiJsParam.hasSelectableOptions: Boolean
    get() = options.isNotEmpty() && (type == BiliPaiJsParamTypes.ENUM || type == BiliPaiJsParamTypes.COUNT)

/** 声明了 page 或 offset 参数的模块由宿主在滚动到底时自动翻页。 */
val BiliPaiJsModule.supportsPagination: Boolean
    get() = params.any { it.type == BiliPaiJsParamTypes.PAGE || it.type == BiliPaiJsParamTypes.OFFSET }

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
    val childItems: List<BiliPaiJsMediaItem> = emptyList(),
    /** 非空且不可播放时，宿主把它作为详情入口，携带 `{"link": ...}` 重调插件。 */
    val link: String? = null
)

@Serializable
data class BiliPaiJsMediaStream(
    val id: String = "primary",
    val title: String = "默认线路",
    val url: String,
    val contentType: String? = null,
    val headers: Map<String, String> = emptyMap()
)

/** JS 插件返回的第三方弹幕条目；[mode] 取 `scroll` / `top` / `bottom`。 */
@Serializable
data class BiliPaiJsDanmuComment(
    val timeMs: Long = 0L,
    val text: String,
    val mode: String = "scroll",
    /** `#RRGGBB`、`#AARRGGBB` 或十进制整数；缺省用宿主默认色。 */
    val color: String? = null
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
        if (module.cacheDuration < 0L) {
            return "JS 插件模块 cacheDuration 不能为负数: ${module.title}"
        }
        module.params.forEach { param ->
            if (param.type !in BiliPaiJsParamTypes.ALL) {
                return "JS 插件参数类型无效: ${param.type}（${param.name}）"
            }
        }
        if (module.layout !in BiliPaiJsLayouts.ALL) {
            return "JS 插件模块布局无效: ${module.layout}（${module.title}），仅支持 list 或 grid"
        }
    }
    val knownPermissions = PluginCapability.entries.toSet()
    val unknownPermission = manifest.permissions.firstOrNull { it !in knownPermissions }
    if (unknownPermission != null) {
        return "JS 插件声明了未知权限: $unknownPermission"
    }
    if (manifest.detailFunctionName.isNotBlank() &&
        !functionNameRegex.matches(manifest.detailFunctionName)
    ) {
        return "JS 插件详情函数名格式无效: ${manifest.detailFunctionName}"
    }
    if (manifest.detailCacheDuration < 0L) {
        return "JS 插件 detailCacheDuration 不能为负数"
    }
    if (manifest.danmakuFunctionName.isNotBlank() &&
        !functionNameRegex.matches(manifest.danmakuFunctionName)
    ) {
        return "JS 插件弹幕函数名格式无效: ${manifest.danmakuFunctionName}"
    }
    if (manifest.danmakuCacheDuration < 0L) {
        return "JS 插件 danmakuCacheDuration 不能为负数"
    }
    if (manifest.requiredApi !in 1..BiliPaiJsApiLevel.CURRENT) {
        return "JS 插件要求宿主 API 版本 ${manifest.requiredApi}，当前宿主最高支持 ${BiliPaiJsApiLevel.CURRENT}，请升级 BiliPai"
    }
    return null
}

fun resolveBiliPaiJsPluginCapabilities(manifest: BiliPaiJsPluginManifest): Set<PluginCapability> {
    return manifest.permissions
}

private val pluginIdRegex = Regex("^[A-Za-z0-9_.-]{1,64}$")
private val functionNameRegex = Regex("^[A-Za-z_$][A-Za-z0-9_$]{0,63}$")
