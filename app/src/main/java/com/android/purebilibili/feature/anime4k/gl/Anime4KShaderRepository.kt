package com.android.purebilibili.feature.anime4k.gl

import android.content.Context
import com.android.purebilibili.feature.anime4k.Anime4KShaderChain

internal data class Anime4KShaderFile(
    val name: String,
    val passes: List<MpvAnime4KShaderPass>
)

internal fun resolveAnime4KShaderFiles(chain: Anime4KShaderChain): List<String> {
    return when (chain) {
        Anime4KShaderChain.KAZUMI_EFFICIENCY -> listOf(
            "Anime4K_Clamp_Highlights.glsl",
            "Anime4K_Restore_CNN_M.glsl",
            "Anime4K_Restore_CNN_S.glsl",
            "Anime4K_Upscale_CNN_x2_M.glsl",
            "Anime4K_AutoDownscalePre_x2.glsl",
            "Anime4K_AutoDownscalePre_x4.glsl",
            "Anime4K_Upscale_CNN_x2_S.glsl"
        )

        Anime4KShaderChain.KAZUMI_QUALITY -> listOf(
            "Anime4K_Clamp_Highlights.glsl",
            "Anime4K_Restore_CNN_VL.glsl",
            "Anime4K_Upscale_CNN_x2_VL.glsl",
            "Anime4K_AutoDownscalePre_x2.glsl",
            "Anime4K_AutoDownscalePre_x4.glsl",
            "Anime4K_Upscale_CNN_x2_M.glsl"
        )
    }
}

/** 从 APK assets 读取并缓存 Kazumi 使用的 Anime4K shader_hook 文件。 */
internal class Anime4KShaderRepository(context: Context) {
    private val assets = context.applicationContext.assets
    private val fileCache = mutableMapOf<String, Anime4KShaderFile>()

    fun loadChain(chain: Anime4KShaderChain): List<Anime4KShaderFile> {
        return resolveAnime4KShaderFiles(chain).map(::loadFile)
    }

    private fun loadFile(name: String): Anime4KShaderFile {
        return fileCache.getOrPut(name) {
            val source = assets.open("anime4k/$name")
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }
            Anime4KShaderFile(
                name = name,
                passes = MpvAnime4KShaderParser.parse(name, source)
            )
        }
    }
}
