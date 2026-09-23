package com.android.purebilibili.feature.anime4k

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoEnhancementSessionPolicyTest {

    @Test
    fun `新视频默认不启用画质增强`() {
        val config = Anime4KConfig(
            rememberAcrossVideos = false,
            rememberedEnabled = true
        )

        assertFalse(
            resolveInitialVideoEnhancementEnabled(
                pluginEnabled = true,
                config = config
            )
        )
    }

    @Test
    fun `只有插件和跨视频记忆同时启用时才恢复状态`() {
        val config = Anime4KConfig(
            rememberAcrossVideos = true,
            rememberedEnabled = true
        )

        assertTrue(resolveInitialVideoEnhancementEnabled(pluginEnabled = true, config = config))
        assertFalse(resolveInitialVideoEnhancementEnabled(pluginEnabled = false, config = config))
    }

    @Test
    fun `未开启记忆时当前视频开关不写入长期状态`() {
        val config = Anime4KConfig(rememberedEnabled = false)

        assertEquals(
            config,
            resolveConfigAfterVideoEnhancementToggle(config, enabled = true)
        )
    }

    @Test
    fun `开启记忆后当前视频开关写入长期状态`() {
        val config = Anime4KConfig(rememberAcrossVideos = true)

        assertTrue(
            resolveConfigAfterVideoEnhancementToggle(config, enabled = true).rememberedEnabled
        )
    }

    @Test
    fun `开启记忆时继承当前视频状态`() {
        val updated = resolveConfigAfterRememberAcrossVideosChange(
            config = Anime4KConfig(),
            rememberAcrossVideos = true,
            currentVideoEnabled = true
        )

        assertTrue(updated.rememberAcrossVideos)
        assertTrue(updated.rememberedEnabled)
    }

    @Test
    fun `关闭记忆时清除长期启用状态`() {
        val updated = resolveConfigAfterRememberAcrossVideosChange(
            config = Anime4KConfig(
                rememberAcrossVideos = true,
                rememberedEnabled = true
            ),
            rememberAcrossVideos = false,
            currentVideoEnabled = true
        )

        assertFalse(updated.rememberAcrossVideos)
        assertFalse(updated.rememberedEnabled)
    }

    @Test
    fun `首次开启记忆必须确认遗忘风险提醒`() {
        assertTrue(
            shouldConfirmRememberAcrossVideosChange(
                currentValue = false,
                requestedValue = true
            )
        )
        assertFalse(
            shouldConfirmRememberAcrossVideosChange(
                currentValue = true,
                requestedValue = false
            )
        )
    }

    @Test
    fun `旧配置迁移后保留 Anime4K 并关闭跨视频记忆`() {
        val config = decodeVideoEnhancementConfig("""{"preset":"QUALITY"}""")

        assertEquals(VideoEnhancementAlgorithm.ANIME4K, config.algorithm)
        assertEquals(Anime4KPreset.QUALITY, config.preset)
        assertFalse(config.rememberAcrossVideos)
        assertFalse(config.rememberedEnabled)
    }

    @Test
    fun `旧中间档迁移到效率档`() {
        val config = decodeVideoEnhancementConfig("""{"preset":"BALANCED"}""")

        assertEquals(Anime4KPreset.FAST, config.preset)
    }

    @Test
    fun `FSR 默认锐度对应公开实现的 0点2 stop`() {
        assertEquals(0.2f, resolveFsrRcasSharpnessStops(DEFAULT_FSR_SHARPNESS), 0.0001f)
    }

    @Test
    fun `历史连续锐化值归一化到最近的零点一档位`() {
        assertEquals(0.7f, normalizeFsrSharpness(0.69f), 0.0001f)
        assertEquals(0f, normalizeFsrSharpness(-0.2f), 0.0001f)
        assertEquals(1f, normalizeFsrSharpness(1.2f), 0.0001f)
    }

    @Test
    fun `读取旧配置时同步归一化锐化档位`() {
        val config = decodeVideoEnhancementConfig("""{"fsrSharpness":0.69}""")

        assertEquals(0.7f, config.fsrSharpness, 0.0001f)
    }

    @Test
    fun `FSR 每条边最多放大两倍且保持宽高比`() {
        assertEquals(
            2560 to 1440,
            resolveFsr1TargetSize(
                sourceWidth = 1280,
                sourceHeight = 720,
                requestedWidth = 3840,
                requestedHeight = 2160,
                glMaxTextureSize = 4096
            )
        )
    }

    @Test
    fun `FSR 目标受纹理上限约束时仍保持宽高比`() {
        assertEquals(
            2048 to 1152,
            resolveFsr1TargetSize(
                sourceWidth = 1920,
                sourceHeight = 1080,
                requestedWidth = 3840,
                requestedHeight = 2160,
                glMaxTextureSize = 2048
            )
        )
    }
}
