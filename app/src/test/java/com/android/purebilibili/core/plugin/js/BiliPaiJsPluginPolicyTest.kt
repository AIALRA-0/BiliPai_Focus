package com.android.purebilibili.core.plugin.js

import com.android.purebilibili.plugin.sdk.PluginCapability
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BiliPaiJsPluginPolicyTest {

    @Test
    fun validManifestPassesValidationAndMapsCapabilities() {
        val manifest = BiliPaiJsPluginManifest(
            id = "live.tv",
            title = "电视台",
            version = "1.0.0",
            modules = listOf(
                BiliPaiJsModule(
                    title = "直播频道",
                    functionName = "loadChannels"
                )
            ),
            permissions = setOf(
                PluginCapability.NETWORK,
                PluginCapability.EXTERNAL_MEDIA_PLAYBACK
            )
        )

        assertEquals(null, validateBiliPaiJsPluginManifest(manifest))
        assertEquals(
            setOf(PluginCapability.NETWORK, PluginCapability.EXTERNAL_MEDIA_PLAYBACK),
            resolveBiliPaiJsPluginCapabilities(manifest)
        )
    }

    @Test
    fun invalidManifestReturnsFirstBlockingReason() {
        val manifest = BiliPaiJsPluginManifest(
            id = "bad id",
            title = "坏插件",
            modules = listOf(BiliPaiJsModule(title = "模块", functionName = "load"))
        )

        assertEquals("JS 插件 ID 格式无效，仅支持字母数字/._-", validateBiliPaiJsPluginManifest(manifest))
    }

    @Test
    fun manifestRequiresAtLeastOneValidModuleFunction() {
        assertEquals(
            "JS 插件至少需要声明一个模块",
            validateBiliPaiJsPluginManifest(
                BiliPaiJsPluginManifest(id = "empty.modules", title = "空模块")
            )
        )
        assertEquals(
            "JS 插件模块函数名格式无效: load-items",
            validateBiliPaiJsPluginManifest(
                BiliPaiJsPluginManifest(
                    id = "bad.function",
                    title = "坏函数",
                    modules = listOf(BiliPaiJsModule(title = "模块", functionName = "load-items"))
                )
            )
        )
    }

    @Test
    fun manifestRejectsNegativeCacheDurationAndUnknownParamType() {
        assertEquals(
            "JS 插件模块 cacheDuration 不能为负数: 负缓存",
            validateBiliPaiJsPluginManifest(
                BiliPaiJsPluginManifest(
                    id = "bad.cache",
                    title = "坏缓存",
                    modules = listOf(
                        BiliPaiJsModule(title = "负缓存", functionName = "load", cacheDuration = -1L)
                    )
                )
            )
        )
        assertEquals(
            "JS 插件参数类型无效: number（page）",
            validateBiliPaiJsPluginManifest(
                BiliPaiJsPluginManifest(
                    id = "bad.param",
                    title = "坏参数",
                    modules = listOf(
                        BiliPaiJsModule(
                            title = "模块",
                            functionName = "load",
                            params = listOf(BiliPaiJsParam(name = "page", title = "页码", type = "number"))
                        )
                    )
                )
            )
        )
        assertEquals(
            null,
            validateBiliPaiJsPluginManifest(
                BiliPaiJsPluginManifest(
                    id = "good.params",
                    title = "好参数",
                    modules = listOf(
                        BiliPaiJsModule(
                            title = "模块",
                            functionName = "load",
                            cacheDuration = 600L,
                            params = listOf(
                                BiliPaiJsParam(name = "page", title = "页码", type = BiliPaiJsParamTypes.PAGE)
                            )
                        )
                    )
                )
            )
        )
    }

    @Test
    fun manifestRejectsInvalidDetailFunctionAndNegativeDetailCache() {
        assertEquals(
            "JS 插件详情函数名格式无效: load-detail",
            validateBiliPaiJsPluginManifest(
                BiliPaiJsPluginManifest(
                    id = "bad.detail",
                    title = "坏详情",
                    modules = listOf(BiliPaiJsModule(title = "模块", functionName = "load")),
                    detailFunctionName = "load-detail"
                )
            )
        )
        assertEquals(
            "JS 插件 detailCacheDuration 不能为负数",
            validateBiliPaiJsPluginManifest(
                BiliPaiJsPluginManifest(
                    id = "bad.detail.cache",
                    title = "坏详情缓存",
                    modules = listOf(BiliPaiJsModule(title = "模块", functionName = "load")),
                    detailFunctionName = "loadDetail",
                    detailCacheDuration = -5L
                )
            )
        )
    }

    @Test
    fun manifestRejectsRequiredApiBeyondHostSupport() {
        assertEquals(
            "JS 插件要求宿主 API 版本 99，当前宿主最高支持 ${BiliPaiJsApiLevel.CURRENT}，请升级 BiliPai",
            validateBiliPaiJsPluginManifest(
                BiliPaiJsPluginManifest(
                    id = "future.plugin",
                    title = "未来插件",
                    modules = listOf(BiliPaiJsModule(title = "模块", functionName = "load")),
                    requiredApi = 99
                )
            )
        )
        assertEquals(
            null,
            validateBiliPaiJsPluginManifest(
                BiliPaiJsPluginManifest(
                    id = "current.plugin",
                    title = "当前插件",
                    modules = listOf(BiliPaiJsModule(title = "模块", functionName = "load")),
                    requiredApi = BiliPaiJsApiLevel.CURRENT
                )
            )
        )
    }

    @Test
    fun manifestRejectsInvalidDanmakuFunctionAndNegativeDanmakuCache() {
        assertEquals(
            "JS 插件弹幕函数名格式无效: load-danmu",
            validateBiliPaiJsPluginManifest(
                BiliPaiJsPluginManifest(
                    id = "bad.danmu",
                    title = "坏弹幕",
                    modules = listOf(BiliPaiJsModule(title = "模块", functionName = "load")),
                    danmakuFunctionName = "load-danmu"
                )
            )
        )
        assertEquals(
            "JS 插件 danmakuCacheDuration 不能为负数",
            validateBiliPaiJsPluginManifest(
                BiliPaiJsPluginManifest(
                    id = "bad.danmu.cache",
                    title = "坏弹幕缓存",
                    modules = listOf(BiliPaiJsModule(title = "模块", functionName = "load")),
                    danmakuFunctionName = "loadDanmu",
                    danmakuCacheDuration = -1L
                )
            )
        )
    }

    @Test
    fun manifestRejectsUnknownModuleLayout() {
        assertEquals(
            "JS 插件模块布局无效: waterfall（瀑布流模块），仅支持 list 或 grid",
            validateBiliPaiJsPluginManifest(
                BiliPaiJsPluginManifest(
                    id = "bad.layout",
                    title = "坏布局",
                    modules = listOf(
                        BiliPaiJsModule(title = "瀑布流模块", functionName = "load", layout = "waterfall")
                    )
                )
            )
        )
        assertEquals(
            null,
            validateBiliPaiJsPluginManifest(
                BiliPaiJsPluginManifest(
                    id = "good.layout",
                    title = "好布局",
                    modules = listOf(
                        BiliPaiJsModule(title = "网格模块", functionName = "load", layout = BiliPaiJsLayouts.GRID)
                    )
                )
            )
        )
    }

    @Test
    fun supportsDanmakuFollowsDanmakuFunctionName() {
        assertTrue(
            BiliPaiJsPluginManifest(
                id = "with.danmu",
                title = "有弹幕",
                modules = listOf(BiliPaiJsModule(title = "模块", functionName = "load")),
                danmakuFunctionName = "loadDanmu"
            ).supportsDanmaku
        )
        assertFalse(
            BiliPaiJsPluginManifest(
                id = "without.danmu",
                title = "无弹幕",
                modules = listOf(BiliPaiJsModule(title = "模块", functionName = "load"))
            ).supportsDanmaku
        )
    }

    @Test
    fun supportsDetailFollowsDetailFunctionName() {
        assertTrue(
            BiliPaiJsPluginManifest(
                id = "with.detail",
                title = "有详情",
                modules = listOf(BiliPaiJsModule(title = "模块", functionName = "load")),
                detailFunctionName = "loadDetail"
            ).supportsDetail
        )
        assertFalse(
            BiliPaiJsPluginManifest(
                id = "without.detail",
                title = "无详情",
                modules = listOf(BiliPaiJsModule(title = "模块", functionName = "load"))
            ).supportsDetail
        )
    }

    @Test
    fun hostDrivenParamsAndSelectableOptionsAreClassified() {
        val pageParam = BiliPaiJsParam(name = "page", title = "页码", type = BiliPaiJsParamTypes.PAGE)
        val offsetParam = BiliPaiJsParam(name = "offset", title = "偏移", type = BiliPaiJsParamTypes.OFFSET)
        val enumParam = BiliPaiJsParam(
            name = "category",
            title = "分类",
            type = BiliPaiJsParamTypes.ENUM,
            options = listOf(BiliPaiJsEnumOption(title = "全部", value = "all"))
        )
        val textParam = BiliPaiJsParam(name = "source", title = "数据源")

        assertTrue(pageParam.isHostDriven)
        assertTrue(offsetParam.isHostDriven)
        assertTrue(!textParam.isHostDriven)
        assertTrue(enumParam.hasSelectableOptions)
        assertTrue(!textParam.hasSelectableOptions)
    }

    @Test
    fun moduleWithPageOrOffsetParamSupportsPagination() {
        assertTrue(
            BiliPaiJsModule(
                title = "分页模块",
                functionName = "load",
                params = listOf(BiliPaiJsParam(name = "page", title = "页码", type = BiliPaiJsParamTypes.PAGE))
            ).supportsPagination
        )
        assertTrue(
            BiliPaiJsModule(
                title = "偏移模块",
                functionName = "load",
                params = listOf(BiliPaiJsParam(name = "offset", title = "偏移", type = BiliPaiJsParamTypes.OFFSET))
            ).supportsPagination
        )
        assertTrue(
            !BiliPaiJsModule(title = "普通模块", functionName = "load").supportsPagination
        )
    }

    @Test
    fun mediaItemResolvesPrimaryAndChildStreams() {
        val item = BiliPaiJsMediaItem(
            id = "cctv1",
            title = "CCTV1",
            videoUrl = "https://example.com/main.m3u8",
            streams = listOf(
                BiliPaiJsMediaStream(id = "backup", title = "备用", url = "https://example.com/backup.m3u8")
            ),
            childItems = listOf(
                BiliPaiJsMediaItem(id = "line2", title = "线路 2", videoUrl = "https://example.com/line2.m3u8")
            )
        )

        val streams = resolveBiliPaiJsMediaStreams(item)

        assertEquals(
            listOf(
                BiliPaiJsMediaStream(id = "primary", title = "默认线路", url = "https://example.com/main.m3u8"),
                BiliPaiJsMediaStream(id = "backup", title = "备用", url = "https://example.com/backup.m3u8"),
                BiliPaiJsMediaStream(id = "line2", title = "线路 2", url = "https://example.com/line2.m3u8")
            ),
            streams
        )
        assertTrue(item.isPlayable)
    }

    @Test
    fun mediaItemWithoutPlayableUrlIsNotPlayable() {
        val item = BiliPaiJsMediaItem(id = "folder", title = "分类")

        assertFalse(item.isPlayable)
        assertEquals(emptyList(), resolveBiliPaiJsMediaStreams(item))
    }

    @Test
    fun mediaItemResolvesImageCandidatesAndDistinguishesMissingIcon() {
        val item = BiliPaiJsMediaItem(
            id = "logo",
            title = "台标",
            coverUrl = "https://example.com/a.png",
            coverUrls = listOf("https://example.com/a.png", "https://example.com/c.png"),
            backdropPath = "https://example.com/b.png",
            posterPath = "https://example.com/p.png"
        )

        assertEquals(
            listOf(
                "https://example.com/b.png",
                "https://example.com/a.png",
                "https://example.com/c.png",
                "https://example.com/p.png"
            ),
            resolveBiliPaiJsMediaImageCandidates(item)
        )
        assertTrue(BiliPaiJsMediaItem(id = "no-logo", title = "无台标").hasNoImageCandidate)
    }

    @Test
    fun mediaItemImageCandidatesFollowForwardBackdropCoverPosterFallbackOrder() {
        val item = BiliPaiJsMediaItem(
            id = "forward",
            title = "Forward 模块",
            coverUrl = "https://example.com/cover.png",
            backdropPath = "https://example.com/backdrop.png",
            backdropPaths = listOf("https://example.com/backdrop.png", "https://example.com/backdrop-backup.png"),
            posterPath = "https://example.com/poster.png",
            posterPaths = listOf("https://example.com/poster-backup.png")
        )

        assertEquals(
            listOf(
                "https://example.com/backdrop.png",
                "https://example.com/backdrop-backup.png",
                "https://example.com/cover.png",
                "https://example.com/poster.png",
                "https://example.com/poster-backup.png"
            ),
            resolveBiliPaiJsMediaImageCandidates(item)
        )
    }
}
