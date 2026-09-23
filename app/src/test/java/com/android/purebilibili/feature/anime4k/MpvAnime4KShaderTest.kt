package com.android.purebilibili.feature.anime4k

import com.android.purebilibili.feature.anime4k.gl.Anime4KTextureSize
import com.android.purebilibili.feature.anime4k.gl.MpvAnime4KShaderParser
import com.android.purebilibili.feature.anime4k.gl.buildMpvAnime4KFragmentShader
import com.android.purebilibili.feature.anime4k.gl.evaluateMpvShaderExpression
import com.android.purebilibili.feature.anime4k.gl.resolveAnime4KShaderFiles
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MpvAnime4KShaderTest {

    @Test
    fun vendoredKazumiShaders_parseIntoCompleteChains() {
        val assetDirectory = listOf(
            File("src/main/assets/anime4k"),
            File("app/src/main/assets/anime4k")
        ).firstOrNull(File::isDirectory) ?: error("找不到 Anime4K 测试资产")
        val expectedPassCounts = mapOf(
            "Anime4K_AutoDownscalePre_x2.glsl" to 1,
            "Anime4K_AutoDownscalePre_x4.glsl" to 1,
            "Anime4K_Clamp_Highlights.glsl" to 3,
            "Anime4K_Restore_CNN_M.glsl" to 8,
            "Anime4K_Restore_CNN_S.glsl" to 4,
            "Anime4K_Restore_CNN_VL.glsl" to 17,
            "Anime4K_Upscale_CNN_x2_M.glsl" to 9,
            "Anime4K_Upscale_CNN_x2_S.glsl" to 5,
            "Anime4K_Upscale_CNN_x2_VL.glsl" to 18
        )
        val parsedFiles = expectedPassCounts.mapValues { (name, expectedCount) ->
            val passes = MpvAnime4KShaderParser.parse(
                sourceName = name,
                source = File(assetDirectory, name).readText(Charsets.UTF_8)
            )
            assertEquals(expectedCount, passes.size, name)
            assertTrue(passes.all { it.bindings.size <= 16 }, name)
            assertTrue(passes.all { it.hook == "MAIN" || it.hook == "PREKERNEL" }, name)
            passes.forEach { pass ->
                val referencedTextures = TEXTURE_MACRO_REGEX.findAll(pass.shaderBody)
                    .map { it.groupValues[1] }
                    .toSet()
                assertTrue(
                    pass.bindings.containsAll(referencedTextures),
                    "$name / ${pass.description} 缺少 BIND：${referencedTextures - pass.bindings.toSet()}"
                )
                assertFalse("//!" in buildMpvAnime4KFragmentShader(pass))
            }
            passes
        }

        assertEquals(
            31,
            resolveAnime4KShaderFiles(Anime4KShaderChain.KAZUMI_EFFICIENCY)
                .sumOf { parsedFiles.getValue(it).size }
        )
        assertEquals(
            49,
            resolveAnime4KShaderFiles(Anime4KShaderChain.KAZUMI_QUALITY)
                .sumOf { parsedFiles.getValue(it).size }
        )
        assertEquals(
            listOf("MAIN", "MAIN", "PREKERNEL"),
            parsedFiles.getValue("Anime4K_Clamp_Highlights.glsl").map { it.hook }
        )
    }

    @Test
    fun parser_preservesMpvBindingsAndExpressions() {
        val passes = MpvAnime4KShaderParser.parse(
            sourceName = "fixture.glsl",
            source = """
                // MIT License
                //!DESC Fixture pass
                //!HOOK MAIN
                //!BIND MAIN
                //!BIND feature
                //!SAVE result
                //!WIDTH MAIN.w 2 *
                //!HEIGHT MAIN.h
                //!COMPONENTS 4
                //!WHEN OUTPUT.w MAIN.w / 1.2 >
                vec4 hook() {
                    return MAIN_tex(MAIN_pos) + feature_texOff(vec2(1.0, 0.0));
                }
            """.trimIndent()
        )

        assertEquals(1, passes.size)
        val pass = passes.single()
        assertEquals(listOf("MAIN", "feature"), pass.bindings)
        assertEquals("result", pass.save)
        assertEquals("MAIN.w 2 *", pass.widthExpression)
        assertEquals("OUTPUT.w MAIN.w / 1.2 >", pass.whenExpression)

        val fragment = buildMpvAnime4KFragmentShader(pass)
        assertTrue("#version 300 es" in fragment)
        assertTrue("#define MAIN_tex(pos)" in fragment)
        assertTrue("#define feature_texOff(offset)" in fragment)
        assertTrue("outColor = hook();" in fragment)
    }

    @Test
    fun expressionEvaluator_matchesAnime4kAutoDownscaleCondition() {
        val sizes = mapOf(
            "OUTPUT" to Anime4KTextureSize(1080, 608),
            "NATIVE" to Anime4KTextureSize(720, 405)
        )
        val expression =
            "OUTPUT.w NATIVE.w / 2.0 < OUTPUT.h NATIVE.h / 2.0 < * " +
                "OUTPUT.w NATIVE.w / 1.2 > OUTPUT.h NATIVE.h / 1.2 > * *"

        assertEquals(1.0, evaluateMpvShaderExpression(expression, sizes))

        val noDownscaleSizes = sizes + ("OUTPUT" to Anime4KTextureSize(720, 405))
        assertEquals(0.0, evaluateMpvShaderExpression(expression, noDownscaleSizes))
    }

    @Test
    fun expressionEvaluator_handlesUpscaleThreshold() {
        val expression = "OUTPUT.w MAIN.w / 1.200 > OUTPUT.h MAIN.h / 1.200 > *"
        val upscale = mapOf(
            "OUTPUT" to Anime4KTextureSize(1920, 1080),
            "MAIN" to Anime4KTextureSize(1280, 720)
        )
        val nativeSize = mapOf(
            "OUTPUT" to Anime4KTextureSize(1920, 1080),
            "MAIN" to Anime4KTextureSize(1920, 1080)
        )

        assertTrue(evaluateMpvShaderExpression(expression, upscale) != 0.0)
        assertFalse(evaluateMpvShaderExpression(expression, nativeSize) != 0.0)
    }

    private companion object {
        val TEXTURE_MACRO_REGEX =
            Regex("""\b([A-Za-z_][A-Za-z0-9_]*)_(?:pos|size|pt|tex|texOff)\b""")
    }
}
