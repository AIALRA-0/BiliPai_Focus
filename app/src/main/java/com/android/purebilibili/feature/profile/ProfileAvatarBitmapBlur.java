package com.android.purebilibili.feature.profile;

import android.graphics.Bitmap;

/** Separable triangular kernel for the fusion view's small software buffer. */
final class ProfileAvatarBitmapBlur {
    static void blur(Bitmap bitmap, int radius) {
        if (radius <= 0) return;
        int width = bitmap.getWidth(), height = bitmap.getHeight();
        int[] pixels = new int[width * height];
        int[] horizontal = new int[pixels.length];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        pass(pixels, horizontal, width, height, radius, true);
        pass(horizontal, pixels, width, height, radius, false);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
    }

    private static void pass(int[] source, int[] target, int width, int height, int radius, boolean horizontal) {
        int divisor = (radius + 1) * (radius + 1);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int a = 0, r = 0, g = 0, b = 0;
                for (int offset = -radius; offset <= radius; offset++) {
                    int sx = horizontal ? Math.max(0, Math.min(width - 1, x + offset)) : x;
                    int sy = horizontal ? y : Math.max(0, Math.min(height - 1, y + offset));
                    int color = source[sy * width + sx];
                    int weight = radius + 1 - Math.abs(offset);
                    a += (color >>> 24) * weight;
                    r += ((color >>> 16) & 255) * weight;
                    g += ((color >>> 8) & 255) * weight;
                    b += (color & 255) * weight;
                }
                target[y * width + x] = ((a / divisor) << 24) | ((r / divisor) << 16) |
                        ((g / divisor) << 8) | (b / divisor);
            }
        }
    }
}
