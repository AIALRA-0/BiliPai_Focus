/*
 * BiliPai 自有 AGSL shader，用于给玻璃底栏边缘提供轻量折射感。
 * 仅做边缘采样偏移；底色由调用方 onDrawSurface 单独绘制，避免双重染色。
 */
package com.android.purebilibili.feature.home.components

import org.intellij.lang.annotations.Language

internal const val LIQUID_GLASS_SHADER_KEY = "bilipai_liquid_glass"

@Language("AGSL")
internal const val LIQUID_GLASS_SHADER = """
uniform shader img;

uniform float2 resolution;
uniform float2 center;
uniform float2 size;
uniform float4 radius;
uniform float thickness;
uniform float refract_index;
uniform float refract_intensity;
uniform float4 foreground_color_premultiplied;

half sdfRect(half2 p, half4 r) {
  r.xy = (p.x > 0.0) ? r.xy : r.zw;
  r.x  = (p.y > 0.0) ? r.x  : r.y;
  half2 q = abs(p) - size + r.x;
  return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r.x;
}

half4 srcOver(half4 src, half4 dst) {
    half3 outRGB = (src.rgb + dst.rgb * (1.0 - src.a));
    float outA = src.a + (1.0 - src.a) * dst.a;
    return half4(outRGB, outA);
}

half4 main(in float2 fragCoord) {
  half2 p = fragCoord - center;
  half sd = sdfRect(p, radius);
  half2 uv = fragCoord;

  if (sd < 0.0 && thickness > 0.0 && refract_intensity > 0.0) {
    half edge = clamp((thickness + sd) / thickness, 0.0, 1.0);
    half strength = edge * edge * refract_intensity;
    half2 direction = normalize(p / max(size, half2(1.0, 1.0)));
    uv += direction * strength * max(refract_index - 1.0, 0.0);
  }

  return srcOver(half4(foreground_color_premultiplied), img.eval(uv));
}
"""
