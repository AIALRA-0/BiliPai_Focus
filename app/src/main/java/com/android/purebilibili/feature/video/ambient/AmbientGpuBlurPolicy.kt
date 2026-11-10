package com.android.purebilibili.feature.video.ambient

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.ColorSpace
import android.graphics.HardwareBufferRenderer
import android.graphics.Paint
import android.graphics.RenderEffect
import android.graphics.RenderNode
import android.graphics.Shader
import android.hardware.HardwareBuffer
import android.os.Build
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlin.coroutines.suspendCoroutine
import java.util.concurrent.Executor

/**
 * 氛围光晕的 GPU 模糊路径（API 34+）。
 *
 * 用 RenderEffect + HardwareBufferRenderer 在采样帧上做高斯模糊，替代 CPU 逐像素
 * 内核；任何一步失败返回 null，调用方回退 [AmbientFrameAnalyzer.blur]。
 *
 * 语义与 CPU 内核对齐：
 * - 模糊源只有内容区域（letterbox 黑边不参与），模糊结果边缘色向外延伸填满全帧；
 * - 高斯 σ 与 CPU 内核一致（σ²=18 → σ=3，RenderEffect 的半径即标准差）。
 *
 * 每次调用独立创建并销毁 renderer：采样帧最长边 96px、最快 125ms 一帧，
 * 逐帧开销可忽略，换来的是单线程内完成创建-绘制-回收，不做跨线程复用。
 */
internal object AmbientGpuBlurPolicy {

    private const val GLOW_BLUR_SIGMA = 3f
    private const val RENDER_NODE_NAME = "ambient_glow_blur"
    private val callbackExecutor: Executor = Executor { it.run() }

    /** [source] 必须是软件位 ARGB_8888 全帧位图；返回同尺寸的模糊全帧，失败返回 null。 */
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @DoNotInline
    suspend fun glow(source: Bitmap, crop: AmbientCrop): Bitmap? {
        val width = source.width
        val height = source.height
        if (width <= 0 || height <= 0 || source.config != Bitmap.Config.ARGB_8888) return null
        val left = crop.left.coerceIn(0, width)
        val top = crop.top.coerceIn(0, height)
        val right = crop.right.coerceIn(left, width)
        val bottom = crop.bottom.coerceIn(top, height)
        val cropWidth = right - left
        val cropHeight = bottom - top
        if (cropWidth <= 0 || cropHeight <= 0) return null

        val region = Bitmap.createBitmap(source, left, top, cropWidth, cropHeight)
        val blurredRegion = try {
            blurRegion(region)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        } ?: return null
        return edgeExtendToFrame(
            blurredRegion = blurredRegion,
            left = left,
            top = top,
            cropWidth = cropWidth,
            cropHeight = cropHeight,
            width = width,
            height = height,
        )
    }

    /**
     * CLAMP 平铺把模糊结果的边缘像素向外无限延伸，等效 CPU 内核对采样坐标
     * 钳制到内容区域边界的取值：区域内是正常模糊，区域外是模糊后的边缘色。
     */
    private fun edgeExtendToFrame(
        blurredRegion: Bitmap,
        left: Int,
        top: Int,
        cropWidth: Int,
        cropHeight: Int,
        width: Int,
        height: Int,
    ): Bitmap {
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint().apply {
            shader = BitmapShader(blurredRegion, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }
        canvas.save()
        canvas.translate(left.toFloat(), top.toFloat())
        canvas.drawRect(
            -width.toFloat(),
            -height.toFloat(),
            (cropWidth + width).toFloat(),
            (cropHeight + height).toFloat(),
            paint,
        )
        canvas.restore()
        return output
    }

    /** GPU 高斯模糊单帧；异步绘制回调在渲染线程完成前不返回，取消也不中途释放缓冲。 */
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private suspend fun blurRegion(region: Bitmap): Bitmap? {
        val buffer = HardwareBuffer.create(
            region.width,
            region.height,
            HardwareBuffer.RGBA_8888,
            1,
            HardwareBuffer.USAGE_GPU_COLOR_OUTPUT or HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE,
        )
        // 构造渲染器失败时也要关闭已分配的缓冲，GPU 失败由 glow 回退 CPU。
        return buffer.use {
            HardwareBufferRenderer(buffer).use { renderer ->
                val node = RenderNode(RENDER_NODE_NAME)
                node.setPosition(0, 0, region.width, region.height)
                node.setRenderEffect(
                    RenderEffect.createBlurEffect(GLOW_BLUR_SIGMA, GLOW_BLUR_SIGMA, Shader.TileMode.CLAMP)
                )
                val recording = node.beginRecording()
                recording.drawBitmap(region, 0f, 0f, null)
                node.endRecording()
                renderer.setContentRoot(node)

                val result = withContext(NonCancellable) {
                    suspendCoroutine<HardwareBufferRenderer.RenderResult> { continuation ->
                        try {
                            renderer.obtainRenderRequest()
                                .setColorSpace(ColorSpace.get(ColorSpace.Named.SRGB))
                                .draw(callbackExecutor) { renderResult ->
                                    continuation.resumeWith(Result.success(renderResult))
                                }
                        } catch (error: Throwable) {
                            continuation.resumeWith(Result.failure(error))
                        }
                    }
                }

                if (result.status != HardwareBufferRenderer.RenderResult.SUCCESS) {
                    return null
                }
                Bitmap.wrapHardwareBuffer(buffer, null)
                    ?.copy(Bitmap.Config.ARGB_8888, false)
            }
        }
    }
}
