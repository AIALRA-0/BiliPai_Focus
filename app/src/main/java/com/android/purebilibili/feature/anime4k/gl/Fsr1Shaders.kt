package com.android.purebilibili.feature.anime4k.gl

/**
 * AMD FidelityFX Super Resolution 1.0 的 GLES 3.0 RGB 片元着色器。
 *
 * 算法严格按 AMD FSR 1.0（v1.20210629）的 EASU 12-tap 与 RCAS 5-tap 公式移植，
 * 并采用公开 mpv 实现验证过的 GLES 3.0 普通采样兼容路径。没有自定义卷积核。
 *
 * 固定参考：
 * - AMD GPUOpen FidelityFX-FSR commit a21ffb8f6c13233ba336352bdff293894c706575
 * - agyild/FSR.glsl（AMD MIT 头部，mpv RGB/LUMA 实战实现）
 *
 * Copyright (c) 2021 Advanced Micro Devices, Inc. All rights reserved.
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
internal object Fsr1Shaders {

    const val EASU = """
        #version 300 es
        #extension GL_OES_EGL_image_external_essl3 : require
        precision highp float;
        precision highp int;
        uniform samplerExternalOES uTexture;
        uniform vec2 uInputSize;
        in vec2 vTexCoord;
        out vec4 outColor;

        float fsrLuma(vec3 color) {
            return color.b * 0.5 + color.r * 0.5 + color.g;
        }

        float fsrApproxRcp(float value) {
            return uintBitsToFloat(0x7ef07ebbu - floatBitsToUint(value));
        }

        float fsrApproxRsqrt(float value) {
            return uintBitsToFloat(0x5f347d74u - (floatBitsToUint(value) >> 1u));
        }

        void fsrEasuSet(
            inout vec2 direction,
            inout float edgeLength,
            vec2 subPixel,
            bool topLeft,
            bool topRight,
            bool bottomLeft,
            bool bottomRight,
            float lumaA,
            float lumaB,
            float lumaC,
            float lumaD,
            float lumaE
        ) {
            float weight = 0.0;
            if (topLeft) weight = (1.0 - subPixel.x) * (1.0 - subPixel.y);
            if (topRight) weight = subPixel.x * (1.0 - subPixel.y);
            if (bottomLeft) weight = (1.0 - subPixel.x) * subPixel.y;
            if (bottomRight) weight = subPixel.x * subPixel.y;

            float dc = lumaD - lumaC;
            float cb = lumaC - lumaB;
            float lengthX = max(abs(dc), abs(cb));
            lengthX = fsrApproxRcp(lengthX);
            float directionX = lumaD - lumaB;
            lengthX = clamp(abs(directionX) * lengthX, 0.0, 1.0);
            lengthX *= lengthX;

            float ec = lumaE - lumaC;
            float ca = lumaC - lumaA;
            float lengthY = max(abs(ec), abs(ca));
            lengthY = fsrApproxRcp(lengthY);
            float directionY = lumaE - lumaA;
            lengthY = clamp(abs(directionY) * lengthY, 0.0, 1.0);
            lengthY *= lengthY;

            direction += vec2(directionX, directionY) * weight;
            edgeLength += (lengthX + lengthY) * weight;
        }

        void fsrEasuTap(
            inout vec3 accumulatedColor,
            inout float accumulatedWeight,
            vec2 offset,
            vec2 direction,
            vec2 edgeLength,
            float negativeLobe,
            float clippingPoint,
            vec3 color
        ) {
            vec2 rotated;
            rotated.x = offset.x * direction.x + offset.y * direction.y;
            rotated.y = offset.x * -direction.y + offset.y * direction.x;
            rotated *= edgeLength;
            float distanceSquared = min(dot(rotated, rotated), clippingPoint);
            float base = (2.0 / 5.0) * distanceSquared - 1.0;
            float window = negativeLobe * distanceSquared - 1.0;
            base *= base;
            window *= window;
            base = (25.0 / 16.0) * base - (25.0 / 16.0 - 1.0);
            float weight = base * window;
            accumulatedColor += color * weight;
            accumulatedWeight += weight;
        }

        vec3 fsrSample(vec2 pixelCenter) {
            return texture(uTexture, pixelCenter / uInputSize).rgb;
        }

        void main() {
            vec2 pixelPosition = vTexCoord * uInputSize - vec2(0.5);
            vec2 basePixel = floor(pixelPosition);
            vec2 subPixel = pixelPosition - basePixel;

            vec3 b = fsrSample(basePixel + vec2(0.5, -0.5));
            vec3 c = fsrSample(basePixel + vec2(1.5, -0.5));
            vec3 e = fsrSample(basePixel + vec2(-0.5, 0.5));
            vec3 f = fsrSample(basePixel + vec2(0.5, 0.5));
            vec3 g = fsrSample(basePixel + vec2(1.5, 0.5));
            vec3 h = fsrSample(basePixel + vec2(2.5, 0.5));
            vec3 i = fsrSample(basePixel + vec2(-0.5, 1.5));
            vec3 j = fsrSample(basePixel + vec2(0.5, 1.5));
            vec3 k = fsrSample(basePixel + vec2(1.5, 1.5));
            vec3 l = fsrSample(basePixel + vec2(2.5, 1.5));
            vec3 n = fsrSample(basePixel + vec2(0.5, 2.5));
            vec3 o = fsrSample(basePixel + vec2(1.5, 2.5));

            float bL = fsrLuma(b);
            float cL = fsrLuma(c);
            float eL = fsrLuma(e);
            float fL = fsrLuma(f);
            float gL = fsrLuma(g);
            float hL = fsrLuma(h);
            float iL = fsrLuma(i);
            float jL = fsrLuma(j);
            float kL = fsrLuma(k);
            float lL = fsrLuma(l);
            float nL = fsrLuma(n);
            float oL = fsrLuma(o);

            vec2 direction = vec2(0.0);
            float edgeLength = 0.0;
            fsrEasuSet(direction, edgeLength, subPixel, true, false, false, false, bL, eL, fL, gL, jL);
            fsrEasuSet(direction, edgeLength, subPixel, false, true, false, false, cL, fL, gL, hL, kL);
            fsrEasuSet(direction, edgeLength, subPixel, false, false, true, false, fL, iL, jL, kL, nL);
            fsrEasuSet(direction, edgeLength, subPixel, false, false, false, true, gL, jL, kL, lL, oL);

            float directionSquared = dot(direction, direction);
            bool isFlat = directionSquared < (1.0 / 32768.0);
            float inverseLength = fsrApproxRsqrt(directionSquared);
            inverseLength = isFlat ? 1.0 : inverseLength;
            direction.x = isFlat ? 1.0 : direction.x;
            direction *= inverseLength;
            edgeLength *= 0.5;
            edgeLength *= edgeLength;
            float stretch = dot(direction, direction) * fsrApproxRcp(max(abs(direction.x), abs(direction.y)));
            vec2 anisotropicLength = vec2(
                1.0 + (stretch - 1.0) * edgeLength,
                1.0 - 0.5 * edgeLength
            );
            float negativeLobe = 0.5 + ((1.0 / 4.0 - 0.04) - 0.5) * edgeLength;
            float clippingPoint = fsrApproxRcp(negativeLobe);

            vec3 accumulatedColor = vec3(0.0);
            float accumulatedWeight = 0.0;
            fsrEasuTap(accumulatedColor, accumulatedWeight, vec2(0.0, -1.0) - subPixel, direction, anisotropicLength, negativeLobe, clippingPoint, b);
            fsrEasuTap(accumulatedColor, accumulatedWeight, vec2(1.0, -1.0) - subPixel, direction, anisotropicLength, negativeLobe, clippingPoint, c);
            fsrEasuTap(accumulatedColor, accumulatedWeight, vec2(-1.0, 1.0) - subPixel, direction, anisotropicLength, negativeLobe, clippingPoint, i);
            fsrEasuTap(accumulatedColor, accumulatedWeight, vec2(0.0, 1.0) - subPixel, direction, anisotropicLength, negativeLobe, clippingPoint, j);
            fsrEasuTap(accumulatedColor, accumulatedWeight, vec2(0.0, 0.0) - subPixel, direction, anisotropicLength, negativeLobe, clippingPoint, f);
            fsrEasuTap(accumulatedColor, accumulatedWeight, vec2(-1.0, 0.0) - subPixel, direction, anisotropicLength, negativeLobe, clippingPoint, e);
            fsrEasuTap(accumulatedColor, accumulatedWeight, vec2(1.0, 1.0) - subPixel, direction, anisotropicLength, negativeLobe, clippingPoint, k);
            fsrEasuTap(accumulatedColor, accumulatedWeight, vec2(2.0, 1.0) - subPixel, direction, anisotropicLength, negativeLobe, clippingPoint, l);
            fsrEasuTap(accumulatedColor, accumulatedWeight, vec2(2.0, 0.0) - subPixel, direction, anisotropicLength, negativeLobe, clippingPoint, h);
            fsrEasuTap(accumulatedColor, accumulatedWeight, vec2(1.0, 0.0) - subPixel, direction, anisotropicLength, negativeLobe, clippingPoint, g);
            fsrEasuTap(accumulatedColor, accumulatedWeight, vec2(1.0, 2.0) - subPixel, direction, anisotropicLength, negativeLobe, clippingPoint, o);
            fsrEasuTap(accumulatedColor, accumulatedWeight, vec2(0.0, 2.0) - subPixel, direction, anisotropicLength, negativeLobe, clippingPoint, n);

            vec3 minimum = min(min(f, g), min(j, k));
            vec3 maximum = max(max(f, g), max(j, k));
            vec3 result = clamp(accumulatedColor / accumulatedWeight, minimum, maximum);
            outColor = vec4(clamp(result, 0.0, 1.0), 1.0);
        }
    """

    const val RCAS = """
        #version 300 es
        precision highp float;
        uniform sampler2D uTexture;
        uniform vec2 uTextureSize;
        uniform float uSharpnessStops;
        in vec2 vTexCoord;
        out vec4 outColor;

        float fsrLuma(vec3 color) {
            return color.b * 0.5 + color.r * 0.5 + color.g;
        }

        float fsrSafeRcp(float value) {
            float magnitude = max(abs(value), 1.0e-6);
            return (value < 0.0 ? -1.0 : 1.0) / magnitude;
        }

        void main() {
            vec2 pixelStep = 1.0 / uTextureSize;
            vec3 b = texture(uTexture, vTexCoord + vec2(0.0, -pixelStep.y)).rgb;
            vec3 d = texture(uTexture, vTexCoord + vec2(-pixelStep.x, 0.0)).rgb;
            vec3 e = texture(uTexture, vTexCoord).rgb;
            vec3 f = texture(uTexture, vTexCoord + vec2(pixelStep.x, 0.0)).rgb;
            vec3 h = texture(uTexture, vTexCoord + vec2(0.0, pixelStep.y)).rgb;

            float bL = fsrLuma(b);
            float dL = fsrLuma(d);
            float eL = fsrLuma(e);
            float fL = fsrLuma(f);
            float hL = fsrLuma(h);
            float noise = 0.25 * (bL + dL + fL + hL) - eL;
            float lumaMaximum = max(max(max(bL, dL), max(eL, fL)), hL);
            float lumaMinimum = min(min(min(bL, dL), min(eL, fL)), hL);
            noise = clamp(abs(noise) * fsrSafeRcp(lumaMaximum - lumaMinimum), 0.0, 1.0);
            noise = -0.5 * noise + 1.0;

            vec3 ringMinimum = min(min(b, d), min(f, h));
            vec3 ringMaximum = max(max(b, d), max(f, h));
            vec3 hitMinimum = min(ringMinimum, e) * vec3(
                fsrSafeRcp(4.0 * ringMaximum.r),
                fsrSafeRcp(4.0 * ringMaximum.g),
                fsrSafeRcp(4.0 * ringMaximum.b)
            );
            vec3 hitMaximum = (vec3(1.0) - max(ringMaximum, e)) * vec3(
                fsrSafeRcp(4.0 * ringMinimum.r - 4.0),
                fsrSafeRcp(4.0 * ringMinimum.g - 4.0),
                fsrSafeRcp(4.0 * ringMinimum.b - 4.0)
            );
            vec3 channelLobe = max(-hitMinimum, hitMaximum);
            float lobe = max(
                -(0.25 - 1.0 / 16.0),
                min(max(channelLobe.r, max(channelLobe.g, channelLobe.b)), 0.0)
            );
            lobe *= exp2(-clamp(uSharpnessStops, 0.0, 2.0));
            lobe *= noise;
            float reciprocal = fsrSafeRcp(4.0 * lobe + 1.0);
            vec3 result = (lobe * (b + d + f + h) + e) * reciprocal;
            outColor = vec4(clamp(result, 0.0, 1.0), 1.0);
        }
    """
}
