package com.android.purebilibili.feature.anime4k.gl

/** GLES 3.0 输入转换和最终显示 shader，CNN pass 从 assets 动态适配。 */
internal object Anime4KShaders {
    const val VERTEX = """
        #version 300 es
        in vec2 aPosition;
        in vec2 aTexCoord;
        uniform mat4 uTexMatrix;
        uniform vec2 uFlip;
        uniform vec2 uPositionScale;
        out vec2 vTexCoord;
        void main() {
            vec2 texCoord = aTexCoord;
            if (uFlip.x > 0.5) texCoord.x = 1.0 - texCoord.x;
            if (uFlip.y > 0.5) texCoord.y = 1.0 - texCoord.y;
            vTexCoord = (uTexMatrix * vec4(texCoord, 0.0, 1.0)).xy;
            gl_Position = vec4(aPosition * uPositionScale, 0.0, 1.0);
        }
    """

    const val EXTERNAL_COPY = """
        #version 300 es
        #extension GL_OES_EGL_image_external_essl3 : require
        precision highp float;
        uniform samplerExternalOES uTexture;
        in vec2 vTexCoord;
        out vec4 outColor;
        void main() {
            outColor = texture(uTexture, vTexCoord);
        }
    """

    const val DISPLAY_COPY = """
        #version 300 es
        precision highp float;
        uniform sampler2D uTexture;
        in vec2 vTexCoord;
        out vec4 outColor;
        void main() {
            outColor = texture(uTexture, vTexCoord);
        }
    """
}
