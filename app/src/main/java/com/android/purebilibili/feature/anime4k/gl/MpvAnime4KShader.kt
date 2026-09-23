package com.android.purebilibili.feature.anime4k.gl

import java.util.ArrayDeque

internal data class Anime4KTextureSize(
    val width: Int,
    val height: Int
)

internal data class MpvAnime4KShaderPass(
    val sourceName: String,
    val index: Int,
    val description: String,
    val hook: String,
    val bindings: List<String>,
    val save: String?,
    val widthExpression: String?,
    val heightExpression: String?,
    val whenExpression: String?,
    val components: Int,
    val shaderBody: String
)

/** 解析 Anime4K 为 mpv shader_hook 定义的多 Pass GLSL 文件。 */
internal object MpvAnime4KShaderParser {
    fun parse(sourceName: String, source: String): List<MpvAnime4KShaderPass> {
        val result = mutableListOf<MpvAnime4KShaderPass>()
        var builder: PassBuilder? = null

        source.lineSequence().forEach { line ->
            if (line.startsWith("//!DESC ")) {
                builder?.build(result.size)?.let(result::add)
                builder = PassBuilder(
                    sourceName = sourceName,
                    description = line.removePrefix("//!DESC ").trim()
                )
                return@forEach
            }

            val activeBuilder = builder ?: return@forEach
            if (line.startsWith("//!")) {
                activeBuilder.applyDirective(line.removePrefix("//!").trim())
            } else {
                activeBuilder.shaderBody.appendLine(line)
            }
        }

        builder?.build(result.size)?.let(result::add)
        check(result.isNotEmpty()) { "$sourceName 未包含可执行的 Anime4K shader pass" }
        return result
    }

    private class PassBuilder(
        private val sourceName: String,
        private val description: String
    ) {
        private var hook = "MAIN"
        private val bindings = mutableListOf<String>()
        private var save: String? = null
        private var widthExpression: String? = null
        private var heightExpression: String? = null
        private var whenExpression: String? = null
        private var components = 4
        val shaderBody = StringBuilder()

        fun applyDirective(directive: String) {
            val name = directive.substringBefore(' ')
            val value = directive.substringAfter(' ', "").trim()
            when (name) {
                "HOOK" -> hook = value
                "BIND" -> bindings += value
                "SAVE" -> save = value
                "WIDTH" -> widthExpression = value
                "HEIGHT" -> heightExpression = value
                "WHEN" -> whenExpression = value
                "COMPONENTS" -> components = value.toInt()
                else -> error("$sourceName 使用了尚未支持的 mpv shader 指令：$name")
            }
        }

        fun build(index: Int): MpvAnime4KShaderPass {
            check(bindings.isNotEmpty()) { "$sourceName 的 $description 未声明 BIND" }
            check(shaderBody.contains("vec4 hook()")) {
                "$sourceName 的 $description 未包含 vec4 hook()"
            }
            val referencedTextures = TEXTURE_MACRO_REGEX.findAll(shaderBody)
                .map { it.groupValues[1] }
                .toList()
            return MpvAnime4KShaderPass(
                sourceName = sourceName,
                index = index,
                description = description,
                hook = hook,
                // mpv 会隐式提供 Hook 目标宏，部分 Anime4K 文件使用 MAIN_tex 但只显式 BIND HOOKED。
                bindings = (bindings + referencedTextures).distinct(),
                save = save,
                widthExpression = widthExpression,
                heightExpression = heightExpression,
                whenExpression = whenExpression,
                components = components,
                shaderBody = shaderBody.toString().trim()
            )
        }
    }

    private val TEXTURE_MACRO_REGEX =
        Regex("""\b([A-Za-z_][A-Za-z0-9_]*)_(?:pos|size|pt|texOff|tex)\b""")
}

/** 执行 mpv shader 指令使用的逆波兰表达式。布尔结果用 0/1 表示。 */
internal fun evaluateMpvShaderExpression(
    expression: String,
    sizes: Map<String, Anime4KTextureSize>
): Double {
    val stack = ArrayDeque<Double>()
    expression.split(Regex("\\s+"))
        .filter(String::isNotBlank)
        .forEach { token ->
            when (token) {
                "+", "-", "*", "/", ">", "<", ">=", "<=", "=", "==" -> {
                    check(stack.size >= 2) { "Anime4K 表达式缺少操作数：$expression" }
                    val right = stack.removeLast()
                    val left = stack.removeLast()
                    stack.addLast(
                        when (token) {
                            "+" -> left + right
                            "-" -> left - right
                            "*" -> left * right
                            "/" -> left / right
                            ">" -> if (left > right) 1.0 else 0.0
                            "<" -> if (left < right) 1.0 else 0.0
                            ">=" -> if (left >= right) 1.0 else 0.0
                            "<=" -> if (left <= right) 1.0 else 0.0
                            else -> if (left == right) 1.0 else 0.0
                        }
                    )
                }

                "!" -> {
                    check(stack.isNotEmpty()) { "Anime4K 表达式缺少操作数：$expression" }
                    stack.addLast(if (stack.removeLast() == 0.0) 1.0 else 0.0)
                }

                else -> stack.addLast(resolveMpvShaderValue(token, sizes))
            }
        }

    check(stack.size == 1) { "Anime4K 表达式无法归约：$expression" }
    return stack.removeLast()
}

private fun resolveMpvShaderValue(
    token: String,
    sizes: Map<String, Anime4KTextureSize>
): Double {
    token.toDoubleOrNull()?.let { return it }
    val separator = token.lastIndexOf('.')
    check(separator > 0 && separator < token.lastIndex) { "无法识别 Anime4K 表达式值：$token" }
    val textureName = token.substring(0, separator)
    val dimension = token.substring(separator + 1)
    val size = sizes[textureName] ?: error("Anime4K 表达式引用了未生成的纹理：$textureName")
    return when (dimension) {
        "w" -> size.width.toDouble()
        "h" -> size.height.toDouble()
        else -> error("无法识别 Anime4K 纹理尺寸：$token")
    }
}

/** 将 mpv 注入的纹理宏适配为 GLES 3.0 fragment shader。 */
internal fun buildMpvAnime4KFragmentShader(pass: MpvAnime4KShaderPass): String {
    val bindings = buildString {
        pass.bindings.forEachIndexed { index, name ->
            appendLine("uniform sampler2D uTexture$index;")
            appendLine("uniform vec2 uTextureSize$index;")
            appendLine("#define ${name}_pos vTexCoord")
            appendLine("#define ${name}_size uTextureSize$index")
            appendLine("#define ${name}_pt (vec2(1.0) / uTextureSize$index)")
            appendLine("#define ${name}_tex(pos) texture(uTexture$index, (pos))")
            appendLine(
                "#define ${name}_texOff(offset) " +
                    "texture(uTexture$index, vTexCoord + ${name}_pt * (offset))"
            )
        }
    }
    return """
        #version 300 es
        precision highp float;
        precision highp int;
        in vec2 vTexCoord;
        out vec4 outColor;
        $bindings
        ${pass.shaderBody}
        void main() {
            outColor = hook();
        }
    """.trimIndent()
}
