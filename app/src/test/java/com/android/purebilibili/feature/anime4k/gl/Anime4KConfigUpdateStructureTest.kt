package com.android.purebilibili.feature.anime4k.gl

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class Anime4KConfigUpdateStructureTest {

    @Test
    fun `模型切换完成后才请求绘制新画面`() {
        val source = source("Anime4KGLSurfaceView.kt")
        val updateConfig = source.substring(
            startIndex = source.indexOf("fun updateConfig(config: Anime4KConfig)"),
            endIndex = source.indexOf("fun updateInputSize")
        )

        assertTrue(updateConfig.contains("queueEvent {"))
        assertTrue(
            updateConfig.indexOf("pipelineRenderer.setConfig(config)") <
                updateConfig.indexOf("requestRender()")
        )
    }

    @Test
    fun `模型切换重置首帧状态但不重建解码Surface`() {
        val source = source("Anime4KPipelineRenderer.kt")
        val setConfig = source.substring(
            startIndex = source.indexOf("fun setConfig(value: Anime4KConfig)"),
            endIndex = source.indexOf("fun setInputSize")
        )

        assertTrue(setConfig.contains("notifiedFirstFrame = false"))
        assertTrue(setConfig.contains("ensureInputSurface()"))
        assertFalse(setConfig.contains("releaseInputSurface()"))
    }

    @Test
    fun `FSR锐度变化只更新逐帧uniform不重建管线`() {
        val source = source("Anime4KPipelineRenderer.kt")
        val setConfig = source.substring(
            startIndex = source.indexOf("fun setConfig(value: Anime4KConfig)"),
            endIndex = source.indexOf("fun setInputSize")
        )

        assertTrue(setConfig.contains("val pipelineChanged ="))
        assertTrue(
            setConfig.indexOf("if (!pipelineChanged) return") <
                setConfig.indexOf("notifiedFirstFrame = false")
        )
    }

    private fun source(fileName: String): String = listOf(
        File("app/src/main/java/com/android/purebilibili/feature/anime4k/gl/$fileName"),
        File("src/main/java/com/android/purebilibili/feature/anime4k/gl/$fileName")
    ).first { it.exists() }.readText()
}
