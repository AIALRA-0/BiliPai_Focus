package com.android.purebilibili.feature.anime4k.gl

import android.content.Context
import android.opengl.GLES11Ext
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
import android.view.Surface
import com.android.purebilibili.feature.anime4k.Anime4KConfig
import com.android.purebilibili.feature.anime4k.Anime4KPreset
import com.android.purebilibili.feature.anime4k.VideoEnhancementAlgorithm
import com.android.purebilibili.feature.anime4k.resolveAnime4KInputSize
import com.android.purebilibili.feature.anime4k.resolveAnime4KRenderProfile
import com.android.purebilibili.feature.anime4k.resolveFsr1TargetSize
import com.android.purebilibili.feature.anime4k.resolveFsrRcasSharpnessStops
import com.android.purebilibili.feature.anime4k.shouldApplyFsr1Enhancement
import kotlin.math.roundToInt

internal class Anime4KPipelineRenderer(
    context: Context,
    initialConfig: Anime4KConfig,
    private val onFrameAvailable: () -> Unit,
    private val onInputSurfaceChanged: (Surface?) -> Unit,
    private val onFirstFrameRendered: () -> Unit,
    private val onPipelineError: (Throwable) -> Unit
) : GLSurfaceView.Renderer {

    private data class CompiledPass(
        val program: Int,
        val samplerLocations: IntArray,
        val sizeLocations: IntArray
    )

    private val shaderRepository = Anime4KShaderRepository(context)
    private val fboManager = FboManager()
    private val textureMatrix = FloatArray(16)
    private val identityMatrix = FloatArray(16).also { Matrix.setIdentityM(it, 0) }
    private val compiledPasses = mutableMapOf<String, CompiledPass>()

    private var externalTextureId = 0
    private var inputSurface: Anime4KInputSurface? = null
    private var outputWidth = 0
    private var outputHeight = 0
    private var inputWidth = 0
    private var inputHeight = 0
    private var flipHorizontal = false
    private var flipVertical = false
    private var displayScaleMode = Anime4KDisplayScaleMode.FIT
    private var config = initialConfig
    private var maxTextureSize = 1
    private var maxTextureUnits = 1
    @Volatile
    private var frameAvailable = false
    private var hasLatchedFrame = false
    private var notifiedFirstFrame = false
    private var failed = false
    private var renderSampleCount = 0
    private var accumulatedRenderNs = 0L
    private var externalProgram = 0
    private var displayProgram = 0
    private var fsrEasuProgram = 0
    private var fsrRcasProgram = 0

    override fun onSurfaceCreated(
        unused: javax.microedition.khronos.opengles.GL10?,
        eglConfig: javax.microedition.khronos.egl.EGLConfig?
    ) {
        failed = false
        hasLatchedFrame = false
        notifiedFirstFrame = false
        resetPerformanceStats()
        try {
            releaseGlResources(releaseInput = true)
            maxTextureSize = queryGlInteger(GLES30.GL_MAX_TEXTURE_SIZE)
            maxTextureUnits = queryGlInteger(GLES30.GL_MAX_TEXTURE_IMAGE_UNITS)
            externalProgram = createProgram(Anime4KShaders.EXTERNAL_COPY, "OES 输入转换")
            displayProgram = createProgram(Anime4KShaders.DISPLAY_COPY, "最终画面输出")

            prepareRenderPipeline(config)
            ensureInputSurface()
            GLES30.glDisable(GLES30.GL_BLEND)
            GLES30.glClearColor(0f, 0f, 0f, 0f)
        } catch (error: Throwable) {
            failPipeline(error)
        }
    }

    override fun onSurfaceChanged(
        unused: javax.microedition.khronos.opengles.GL10?,
        width: Int,
        height: Int
    ) {
        val safeWidth = width.coerceAtLeast(1)
        val safeHeight = height.coerceAtLeast(1)
        if (outputWidth != safeWidth || outputHeight != safeHeight) {
            outputWidth = safeWidth
            outputHeight = safeHeight
            fboManager.release()
            resetPerformanceStats()
        }
    }

    override fun onDrawFrame(unused: javax.microedition.khronos.opengles.GL10?) {
        if (failed) return
        try {
            val renderStartedNs = System.nanoTime()
            if (frameAvailable) {
                frameAvailable = false
                inputSurface?.surfaceTexture?.updateTexImage()
                inputSurface?.surfaceTexture?.getTransformMatrix(textureMatrix)
                hasLatchedFrame = true
            }
            if (!hasLatchedFrame || outputWidth <= 0 || outputHeight <= 0) {
                GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
                return
            }

            renderFrame()
            observeRenderTime(System.nanoTime() - renderStartedNs)
            if (!notifiedFirstFrame) {
                notifiedFirstFrame = true
                onFirstFrameRendered()
            }
        } catch (error: Throwable) {
            failPipeline(error)
        }
    }

    fun setConfig(value: Anime4KConfig) {
        val pipelineChanged = config.algorithm != value.algorithm || config.preset != value.preset
        if (!pipelineChanged && config.fsrSharpness == value.fsrSharpness) return
        try {
            config = value
            // FSR 锐度是逐帧上传的 uniform，拖动滑条时无需重建 shader/FBO 管线。
            if (!pipelineChanged) return
            notifiedFirstFrame = false
            fboManager.release()
            resetPerformanceStats()
            prepareRenderPipeline(value)
            // 切模型只更换 GL pass，不能重建解码 Surface，否则播放器可能重新选择视频流。
            ensureInputSurface()
        } catch (error: Throwable) {
            failPipeline(error)
        }
    }

    fun setInputSize(width: Int, height: Int) {
        val safeWidth = width.coerceAtLeast(0)
        val safeHeight = height.coerceAtLeast(0)
        if (inputWidth != safeWidth || inputHeight != safeHeight) {
            inputWidth = safeWidth
            inputHeight = safeHeight
            fboManager.release()
            resetPerformanceStats()
        }
        inputSurface?.setDefaultBufferSize(inputWidth, inputHeight)
    }

    fun setFlip(horizontal: Boolean, vertical: Boolean) {
        flipHorizontal = horizontal
        flipVertical = vertical
    }

    fun setDisplayScaleMode(scaleMode: Anime4KDisplayScaleMode) {
        displayScaleMode = scaleMode
    }

    fun ensureInputSurface() {
        if (inputSurface != null || failed) return
        externalTextureId = IntArray(1).also { GLES30.glGenTextures(1, it, 0) }[0]
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, externalTextureId)
        GLES30.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES30.GL_TEXTURE_MIN_FILTER,
            GLES30.GL_LINEAR
        )
        GLES30.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES30.GL_TEXTURE_MAG_FILTER,
            GLES30.GL_LINEAR
        )
        GLES30.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES30.GL_TEXTURE_WRAP_S,
            GLES30.GL_CLAMP_TO_EDGE
        )
        GLES30.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES30.GL_TEXTURE_WRAP_T,
            GLES30.GL_CLAMP_TO_EDGE
        )
        inputSurface = Anime4KInputSurface(externalTextureId) {
            frameAvailable = true
            onFrameAvailable()
        }.also { it.setDefaultBufferSize(inputWidth, inputHeight) }
        onInputSurfaceChanged(inputSurface?.surface)
    }

    fun releaseInputSurface() {
        inputSurface?.let {
            onInputSurfaceChanged(null)
            it.release()
        }
        inputSurface = null
        if (externalTextureId != 0) {
            GLES30.glDeleteTextures(1, intArrayOf(externalTextureId), 0)
            externalTextureId = 0
        }
        hasLatchedFrame = false
        frameAvailable = false
    }

    fun release() {
        releaseGlResources(releaseInput = true)
    }

    private fun renderFrame() {
        if (config.algorithm == VideoEnhancementAlgorithm.FSR_1_0) {
            renderFsrFrame()
            return
        }
        val profile = resolveAnime4KRenderProfile(config.preset)
        val sourceWidth = inputWidth.takeIf { it > 0 } ?: outputWidth
        val sourceHeight = inputHeight.takeIf { it > 0 } ?: outputHeight
        val (cnnInputWidth, cnnInputHeight) = resolveAnime4KInputSize(
            inputWidth = sourceWidth,
            inputHeight = sourceHeight,
            glMaxTextureSize = maxTextureSize
        )
        val nativeTarget = fboManager.obtain(
            width = cnnInputWidth,
            height = cnnInputHeight,
            precision = FboPrecision.RGBA8
        )

        drawExternal(nativeTarget)
        val mainTarget = executeShaderChain(
            shaderFiles = shaderRepository.loadChain(profile.shaderChain),
            nativeTarget = nativeTarget
        )
        drawDisplay(mainTarget)
    }

    /**
     * 公开 Android 视频实现已验证的 FSR 路径：OES 直接执行 EASU 到 RGBA8，
     * RCAS 再直接写入展示 Surface，避免额外的 OES 拷贝和半浮点附件。
     */
    private fun renderFsrFrame() {
        val sourceWidth = inputWidth.takeIf { it > 0 } ?: outputWidth
        val sourceHeight = inputHeight.takeIf { it > 0 } ?: outputHeight
        val displayTransform = resolveAnime4KDisplayTransform(
            outputWidth = outputWidth,
            outputHeight = outputHeight,
            sourceWidth = sourceWidth,
            sourceHeight = sourceHeight,
            scaleMode = displayScaleMode
        )
        val requestedWidth = (outputWidth * displayTransform.scaleX).roundToInt()
        val requestedHeight = (outputHeight * displayTransform.scaleY).roundToInt()
        if (
            !shouldApplyFsr1Enhancement(
                sourceWidth = sourceWidth,
                sourceHeight = sourceHeight,
                outputWidth = requestedWidth,
                outputHeight = requestedHeight
            )
        ) {
            // 明显高于屏幕分辨率的视频已经有足够采样信息，避免额外 GPU 负载。
            val nativeSize = resolveAnime4KInputSize(sourceWidth, sourceHeight, maxTextureSize)
            val nativeTarget = fboManager.obtain(
                width = nativeSize.first,
                height = nativeSize.second,
                precision = FboPrecision.RGBA8
            )
            drawExternal(nativeTarget)
            drawDisplay(nativeTarget)
            return
        }
        val targetSize = resolveFsrTargetSize(
            sourceWidth = sourceWidth,
            sourceHeight = sourceHeight,
            requestedWidth = requestedWidth,
            requestedHeight = requestedHeight
        )
        val easuTarget = fboManager.obtain(
            width = targetSize.first,
            height = targetSize.second,
            precision = FboPrecision.RGBA8
        )
        drawFsrEasu(easuTarget, sourceWidth, sourceHeight)
        drawFsrRcas(easuTarget)
    }

    private fun resolveFsrTargetSize(
        sourceWidth: Int,
        sourceHeight: Int,
        requestedWidth: Int,
        requestedHeight: Int
    ): Pair<Int, Int> {
        return resolveFsr1TargetSize(
            sourceWidth = sourceWidth,
            sourceHeight = sourceHeight,
            requestedWidth = requestedWidth,
            requestedHeight = requestedHeight,
            glMaxTextureSize = maxTextureSize
        )
    }

    private fun drawFsrEasu(target: FboTarget, sourceWidth: Int, sourceHeight: Int) {
        bindTarget(target)
        GLES30.glUseProgram(fsrEasuProgram)
        bindTexture(
            program = fsrEasuProgram,
            uniform = "uTexture",
            target = GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            texture = externalTextureId,
            unit = 0
        )
        GLES30.glUniform2f(
            GLES30.glGetUniformLocation(fsrEasuProgram, "uInputSize"),
            sourceWidth.toFloat(),
            sourceHeight.toFloat()
        )
        setVertexUniforms(fsrEasuProgram, textureMatrix, flipHorizontal, flipVertical)
        QuadRenderUtils.draw(fsrEasuProgram)
    }

    private fun drawFsrRcas(target: FboTarget) {
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        GLES30.glViewport(0, 0, outputWidth, outputHeight)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
        GLES30.glUseProgram(fsrRcasProgram)
        bindTexture(
            program = fsrRcasProgram,
            uniform = "uTexture",
            target = GLES30.GL_TEXTURE_2D,
            texture = target.texture,
            unit = 0
        )
        GLES30.glUniform2f(
            GLES30.glGetUniformLocation(fsrRcasProgram, "uTextureSize"),
            target.width.toFloat(),
            target.height.toFloat()
        )
        GLES30.glUniform1f(
            GLES30.glGetUniformLocation(fsrRcasProgram, "uSharpnessStops"),
            resolveFsrRcasSharpnessStops(config.fsrSharpness)
        )
        val displayTransform = resolveAnime4KDisplayTransform(
            outputWidth = outputWidth,
            outputHeight = outputHeight,
            sourceWidth = inputWidth.takeIf { it > 0 } ?: target.width,
            sourceHeight = inputHeight.takeIf { it > 0 } ?: target.height,
            scaleMode = displayScaleMode
        )
        setVertexUniforms(
            program = fsrRcasProgram,
            matrix = identityMatrix,
            horizontalFlip = false,
            verticalFlip = false,
            positionScaleX = displayTransform.scaleX,
            positionScaleY = displayTransform.scaleY
        )
        QuadRenderUtils.draw(fsrRcasProgram)
    }

    private fun executeShaderChain(
        shaderFiles: List<Anime4KShaderFile>,
        nativeTarget: FboTarget
    ): FboTarget {
        val logicalTargets = mutableMapOf("MAIN" to nativeTarget)
        val deferredBindings = shaderFiles
            .flatMap(Anime4KShaderFile::passes)
            .filter { it.hook != "MAIN" }
            .flatMap(MpvAnime4KShaderPass::bindings)
            .filterNot { it == "MAIN" || it == "HOOKED" || it == "NATIVE" }
            .toSet()
        SUPPORTED_HOOK_ORDER.forEach { hook ->
            shaderFiles.forEach { shaderFile ->
                shaderFile.passes
                    .filter { it.hook == hook }
                    .forEach { pass ->
                        executeShaderPass(
                            shaderFile = shaderFile,
                            pass = pass,
                            logicalTargets = logicalTargets,
                            nativeTarget = nativeTarget
                        )
                    }
                val fileLocalOutputs = shaderFile.passes
                    .filter { it.hook == hook }
                    .mapNotNull(MpvAnime4KShaderPass::save)
                    .filterNot { it == "MAIN" || it in deferredBindings }
                logicalTargets.keys.removeAll(fileLocalOutputs.toSet())
            }
        }
        val unsupportedHooks = shaderFiles
            .flatMap(Anime4KShaderFile::passes)
            .map(MpvAnime4KShaderPass::hook)
            .filterNot(SUPPORTED_HOOK_ORDER::contains)
            .distinct()
        check(unsupportedHooks.isEmpty()) {
            "Anime4K shader 使用了尚未支持的 Hook：${unsupportedHooks.joinToString()}"
        }
        return logicalTargets["MAIN"] ?: nativeTarget
    }

    private fun executeShaderPass(
        shaderFile: Anime4KShaderFile,
        pass: MpvAnime4KShaderPass,
        logicalTargets: MutableMap<String, FboTarget>,
        nativeTarget: FboTarget
    ) {
        val hookTarget = logicalTargets["MAIN"]
            ?: error("${shaderFile.name} 缺少 MAIN 输入")
        val sizeEnvironment = buildSizeEnvironment(
            logicalTargets = logicalTargets,
            hookTarget = hookTarget,
            nativeTarget = nativeTarget
        )
        if (pass.whenExpression != null &&
            evaluateMpvShaderExpression(pass.whenExpression, sizeEnvironment) == 0.0
        ) {
            return
        }

        val boundTargets = pass.bindings.map { binding ->
            when (binding) {
                "HOOKED" -> hookTarget
                "NATIVE" -> nativeTarget
                else -> logicalTargets[binding]
                    ?: error("${shaderFile.name} 的 ${pass.description} 缺少纹理 $binding")
            }
        }
        check(boundTargets.size <= maxTextureUnits) {
            "${pass.description} 需要 ${boundTargets.size} 个纹理单元，设备仅支持 $maxTextureUnits 个"
        }

        val targetWidth = resolveTargetDimension(
            expression = pass.widthExpression,
            fallback = hookTarget.width,
            sizes = sizeEnvironment
        )
        val targetHeight = resolveTargetDimension(
            expression = pass.heightExpression,
            fallback = hookTarget.height,
            sizes = sizeEnvironment
        )
        val protectedTextureIds = buildSet {
            add(nativeTarget.texture)
            logicalTargets.values.forEach { add(it.texture) }
            boundTargets.forEach { add(it.texture) }
        }
        val outputTarget = fboManager.obtain(
            width = targetWidth,
            height = targetHeight,
            precision = if (pass.components == 1) FboPrecision.R16F else FboPrecision.RGBA16F,
            protectedTextureIds = protectedTextureIds
        )
        drawMpvPass(pass, boundTargets, outputTarget)

        val outputName = pass.save ?: "MAIN"
        logicalTargets[outputName] = outputTarget
    }

    private fun buildSizeEnvironment(
        logicalTargets: Map<String, FboTarget>,
        hookTarget: FboTarget,
        nativeTarget: FboTarget
    ): Map<String, Anime4KTextureSize> {
        val sizes = logicalTargets.mapValues { (_, target) ->
            Anime4KTextureSize(target.width, target.height)
        }.toMutableMap()
        sizes["HOOKED"] = Anime4KTextureSize(hookTarget.width, hookTarget.height)
        sizes["NATIVE"] = Anime4KTextureSize(nativeTarget.width, nativeTarget.height)
        sizes["OUTPUT"] = Anime4KTextureSize(outputWidth, outputHeight)
        return sizes
    }

    private fun resolveTargetDimension(
        expression: String?,
        fallback: Int,
        sizes: Map<String, Anime4KTextureSize>
    ): Int {
        val value = expression
            ?.let { evaluateMpvShaderExpression(it, sizes).roundToInt() }
            ?: fallback
        return value.coerceIn(1, maxTextureSize)
    }

    private fun drawExternal(target: FboTarget) {
        bindTarget(target)
        GLES30.glUseProgram(externalProgram)
        bindTexture(
            program = externalProgram,
            uniform = "uTexture",
            target = GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            texture = externalTextureId,
            unit = 0
        )
        setVertexUniforms(externalProgram, textureMatrix, flipHorizontal, flipVertical)
        QuadRenderUtils.draw(externalProgram)
    }

    private fun drawMpvPass(
        pass: MpvAnime4KShaderPass,
        boundTargets: List<FboTarget>,
        outputTarget: FboTarget
    ) {
        val compiled = obtainCompiledPass(pass)
        bindTarget(outputTarget)
        GLES30.glUseProgram(compiled.program)
        boundTargets.forEachIndexed { index, target ->
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0 + index)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, target.texture)
            GLES30.glUniform1i(compiled.samplerLocations[index], index)
            GLES30.glUniform2f(
                compiled.sizeLocations[index],
                target.width.toFloat(),
                target.height.toFloat()
            )
        }
        setVertexUniforms(compiled.program, identityMatrix, false, false)
        QuadRenderUtils.draw(compiled.program)
    }

    private fun drawDisplay(target: FboTarget) {
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        GLES30.glViewport(0, 0, outputWidth, outputHeight)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
        GLES30.glUseProgram(displayProgram)
        bindTexture(
            program = displayProgram,
            uniform = "uTexture",
            target = GLES30.GL_TEXTURE_2D,
            texture = target.texture,
            unit = 0
        )
        val displayTransform = resolveAnime4KDisplayTransform(
            outputWidth = outputWidth,
            outputHeight = outputHeight,
            sourceWidth = inputWidth.takeIf { it > 0 } ?: target.width,
            sourceHeight = inputHeight.takeIf { it > 0 } ?: target.height,
            scaleMode = displayScaleMode
        )
        setVertexUniforms(
            program = displayProgram,
            matrix = identityMatrix,
            horizontalFlip = false,
            verticalFlip = false,
            positionScaleX = displayTransform.scaleX,
            positionScaleY = displayTransform.scaleY
        )
        QuadRenderUtils.draw(displayProgram)
    }

    private fun obtainCompiledPass(pass: MpvAnime4KShaderPass): CompiledPass {
        val key = "${pass.sourceName}:${pass.index}"
        return compiledPasses.getOrPut(key) {
            val program = createProgram(
                fragmentSource = buildMpvAnime4KFragmentShader(pass),
                description = "${pass.sourceName} / ${pass.description}"
            )
            CompiledPass(
                program = program,
                samplerLocations = IntArray(pass.bindings.size) { index ->
                    GLES30.glGetUniformLocation(program, "uTexture$index")
                },
                sizeLocations = IntArray(pass.bindings.size) { index ->
                    GLES30.glGetUniformLocation(program, "uTextureSize$index")
                }
            )
        }
    }

    private fun prepareShaderChain(preset: Anime4KPreset) {
        val profile = resolveAnime4KRenderProfile(preset)
        shaderRepository.loadChain(profile.shaderChain)
            .flatMap(Anime4KShaderFile::passes)
            .forEach { pass ->
                check(pass.bindings.size <= maxTextureUnits) {
                    "${pass.description} 需要 ${pass.bindings.size} 个纹理单元，设备仅支持 $maxTextureUnits 个"
                }
                obtainCompiledPass(pass)
            }
    }

    private fun prepareRenderPipeline(value: Anime4KConfig) {
        when (value.algorithm) {
            VideoEnhancementAlgorithm.ANIME4K -> {
                // 实际创建附件比仅检查扩展字符串更可靠。
                val rgba16fProbe = fboManager.obtain(
                    width = 1,
                    height = 1,
                    precision = FboPrecision.RGBA16F
                )
                fboManager.obtain(
                    width = 1,
                    height = 1,
                    precision = FboPrecision.R16F,
                    protectedTextureIds = setOf(rgba16fProbe.texture)
                )
                fboManager.release()
                prepareShaderChain(value.preset)
            }

            VideoEnhancementAlgorithm.FSR_1_0 -> {
                if (fsrEasuProgram == 0) {
                    fsrEasuProgram = createProgram(Fsr1Shaders.EASU, "AMD FSR 1.0 EASU")
                }
                if (fsrRcasProgram == 0) {
                    fsrRcasProgram = createProgram(Fsr1Shaders.RCAS, "AMD FSR 1.0 RCAS")
                }
            }
        }
    }

    private fun bindTarget(target: FboTarget) {
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, target.framebuffer)
        GLES30.glViewport(0, 0, target.width, target.height)
    }

    private fun bindTexture(
        program: Int,
        uniform: String,
        target: Int,
        texture: Int,
        unit: Int
    ) {
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0 + unit)
        GLES30.glBindTexture(target, texture)
        GLES30.glUniform1i(GLES30.glGetUniformLocation(program, uniform), unit)
    }

    private fun setVertexUniforms(
        program: Int,
        matrix: FloatArray,
        horizontalFlip: Boolean,
        verticalFlip: Boolean,
        positionScaleX: Float = 1f,
        positionScaleY: Float = 1f
    ) {
        GLES30.glUniformMatrix4fv(
            GLES30.glGetUniformLocation(program, "uTexMatrix"),
            1,
            false,
            matrix,
            0
        )
        GLES30.glUniform2f(
            GLES30.glGetUniformLocation(program, "uFlip"),
            if (horizontalFlip) 1f else 0f,
            if (verticalFlip) 1f else 0f
        )
        GLES30.glUniform2f(
            GLES30.glGetUniformLocation(program, "uPositionScale"),
            positionScaleX,
            positionScaleY
        )
    }

    private fun createProgram(fragmentSource: String, description: String): Int {
        val vertex = compileShader(
            type = GLES30.GL_VERTEX_SHADER,
            source = Anime4KShaders.VERTEX,
            description = "$description vertex"
        )
        val fragment = compileShader(
            type = GLES30.GL_FRAGMENT_SHADER,
            source = fragmentSource,
            description = "$description fragment"
        )
        val program = GLES30.glCreateProgram()
        GLES30.glAttachShader(program, vertex)
        GLES30.glAttachShader(program, fragment)
        GLES30.glLinkProgram(program)
        val linked = IntArray(1)
        GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, linked, 0)
        GLES30.glDeleteShader(vertex)
        GLES30.glDeleteShader(fragment)
        if (linked[0] != GLES30.GL_TRUE) {
            val log = GLES30.glGetProgramInfoLog(program)
            GLES30.glDeleteProgram(program)
            error("Anime4K program 链接失败（$description）：$log")
        }
        return program
    }

    private fun compileShader(type: Int, source: String, description: String): Int {
        val shader = GLES30.glCreateShader(type)
        GLES30.glShaderSource(shader, source)
        GLES30.glCompileShader(shader)
        val compiled = IntArray(1)
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] != GLES30.GL_TRUE) {
            val log = GLES30.glGetShaderInfoLog(shader)
            GLES30.glDeleteShader(shader)
            error("Anime4K shader 编译失败（$description）：$log")
        }
        return shader
    }

    private fun queryGlInteger(name: Int): Int {
        return IntArray(1)
            .also { GLES30.glGetIntegerv(name, it, 0) }[0]
            .coerceAtLeast(1)
    }

    private fun releaseGlResources(releaseInput: Boolean) {
        fboManager.release()
        buildSet {
            add(externalProgram)
            add(displayProgram)
            add(fsrEasuProgram)
            add(fsrRcasProgram)
            compiledPasses.values.forEach { add(it.program) }
        }.filter { it != 0 }
            .forEach(GLES30::glDeleteProgram)
        externalProgram = 0
        displayProgram = 0
        fsrEasuProgram = 0
        fsrRcasProgram = 0
        compiledPasses.clear()
        if (releaseInput) releaseInputSurface()
    }

    private fun failPipeline(error: Throwable) {
        if (failed) return
        failed = true
        Log.e(TAG, "画质增强渲染失败", error)
        runCatching { releaseInputSurface() }
        onPipelineError(error)
    }

    private fun observeRenderTime(renderNs: Long) {
        renderSampleCount += 1
        accumulatedRenderNs += renderNs
        if (renderSampleCount >= PERFORMANCE_LOG_SAMPLE_COUNT) {
            val averageMs = accumulatedRenderNs / renderSampleCount / 1_000_000.0
            Log.d(
                TAG,
                "algorithm=${config.algorithm}, preset=${config.preset}, " +
                    "averageSubmitMs=${"%.2f".format(averageMs)}"
            )
            renderSampleCount = 0
            accumulatedRenderNs = 0L
        }
    }

    private fun resetPerformanceStats() {
        renderSampleCount = 0
        accumulatedRenderNs = 0L
    }

    private companion object {
        const val TAG = "Anime4KRenderer"
        const val PERFORMANCE_LOG_SAMPLE_COUNT = 120
        val SUPPORTED_HOOK_ORDER = listOf("MAIN", "PREKERNEL")
    }
}
