package com.android.purebilibili.core.ui.effect

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb

/**
 * Applies a liquid glass refraction effect to an element with a transparent
 * background, letting the shader sample whatever is rendered below it in the
 * graphics layer stack.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun Modifier.liquidGlassBackground(
    refractIntensity: Float = 0.2f,
    scrollOffsetProvider: () -> Float, // Use lambda to avoid recomposition
    backgroundColor: Color = Color.Transparent
): Modifier = composed {
    val shader = remember { RuntimeShader(LiquidGlassBackgroundShader.SHADER) }
    // Cache the RenderEffect once: it references the shader, so updating uniforms below
    // re-renders with new values without allocating a new RenderEffect every frame.
    val liquidGlassRenderEffect = remember(shader) {
        RenderEffect.createRuntimeShaderEffect(shader, "img").asComposeRenderEffect()
    }

    val uniforms = remember(shader) { LiquidGlassUniformCache(shader) }
    val backgroundArgb = remember(backgroundColor) { backgroundColor.toArgb() }
    this.graphicsLayer {
        if (size.width <= 0f || size.height <= 0f) {
            renderEffect = null
        } else {
            uniforms.updateStatic(size.width, size.height, refractIntensity, backgroundArgb)
            // The zero-refraction shader path does not use scrolling. Avoid subscribing to it.
            if (refractIntensity > 0.001f) {
                uniforms.updateScroll(scrollOffsetProvider())
            }
            renderEffect = liquidGlassRenderEffect
        }
    }
}

/** UI-thread cache only; uniform updates must not write Compose Snapshot state. */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private class LiquidGlassUniformCache(private val shader: RuntimeShader) {
    private var width = Float.NaN
    private var height = Float.NaN
    private var intensity = Float.NaN
    private var backgroundArgb: Int? = null
    private var scroll = Float.NaN

    fun updateStatic(nextWidth: Float, nextHeight: Float, nextIntensity: Float, nextBackground: Int) {
        if (width != nextWidth || height != nextHeight) {
            shader.setFloatUniform("resolution", nextWidth, nextHeight)
            width = nextWidth
            height = nextHeight
        }
        if (intensity != nextIntensity) {
            shader.setFloatUniform("refract_intensity", nextIntensity)
            intensity = nextIntensity
        }
        if (backgroundArgb != nextBackground) {
            val alpha = android.graphics.Color.alpha(nextBackground) / 255f
            shader.setFloatUniform(
                "background_color",
                android.graphics.Color.red(nextBackground) / 255f * alpha,
                android.graphics.Color.green(nextBackground) / 255f * alpha,
                android.graphics.Color.blue(nextBackground) / 255f * alpha,
                alpha,
            )
            backgroundArgb = nextBackground
        }
    }

    fun updateScroll(nextScroll: Float) {
        if (scroll != nextScroll) {
            shader.setFloatUniform("scroll_offset", nextScroll)
            scroll = nextScroll
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
object LiquidGlassBackgroundShader {
    const val SHADER = """
        uniform shader img;

        uniform float2 resolution;
        uniform float refract_intensity;
        uniform float scroll_offset;
        uniform float4 background_color;  // Premultiplied

        half4 main(in float2 fragCoord) {
            half2 uv = fragCoord;
            half2 minUV = half2(0.0);
            half2 maxUV = half2(resolution.x - 1.0, resolution.y - 1.0);

            if (refract_intensity <= 0.001) {
                half4 s = img.eval(uv);
                return s * (1.0 - background_color.a) + background_color;
            }

            // Create vertical wave distortion based on scroll
            float scrollProgress = scroll_offset * 0.008;
            float waveX = sin(fragCoord.x * 0.025 + scrollProgress) * 0.6;
            float waveY = cos(fragCoord.y * 0.015 + scrollProgress * 0.5) * 0.8;
            float wave = waveX * waveY + sin(scrollProgress * 0.3) * 0.4;

            float2 offset = float2(wave * 0.4, wave * 1.2) * (refract_intensity * 50.0);
            uv = clamp(uv + offset, minUV, maxUV);

            float aberration = refract_intensity * 0.35;
            float2 aberrOffset = float2(aberration * 4.0, aberration * 1.5);
            half r = img.eval(clamp(uv + aberrOffset, minUV, maxUV)).r;
            half4 centerSample = img.eval(uv);
            half b = img.eval(clamp(uv - aberrOffset, minUV, maxUV)).b;
            half4 sampled = half4(r, centerSample.g, b, centerSample.a);

            // Blend with semi-transparent background for legibility
            return sampled * (1.0 - background_color.a) + background_color;
        }
    """
}
