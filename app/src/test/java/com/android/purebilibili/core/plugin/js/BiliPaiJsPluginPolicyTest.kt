package com.android.purebilibili.core.plugin.js

import com.android.purebilibili.core.plugin.PluginCapability
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
