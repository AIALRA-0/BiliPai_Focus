package com.android.purebilibili.core.util

/**
 * 轻量 HTML 实体反转义工具（纯 Kotlin，无 Android 依赖）。
 *
 * B 站评论接口返回的文本会把特殊字符编码为 HTML 实体
 * （`'` -> `&#39;`、`"` -> `&quot;`、`&` -> `&amp;` 等），
 * 直接展示会出现 `&#39` 这类字面量。统一在数据解析层调用 [unescape] 清洗。
 *
 * 对不含实体的文本原样返回，无法识别的 `&...;` 序列保持原样，
 * 可安全对任意文本调用。
 */
object HtmlEntityUtils {

    private val namedEntities = mapOf(
        "amp" to "&",
        "lt" to "<",
        "gt" to ">",
        "quot" to "\"",
        "apos" to "'",
        "nbsp" to " "
    )

    // 命名实体 `&amp;`、十进制 `&#39;`、十六进制 `&#x2605;`；
    // 数字实体允许省略末尾分号（兼容 `&#39` 这种写法），命名实体要求分号。
    private val entityRegex = Regex("&(#[0-9]+|#[xX][0-9a-fA-F]+|[a-zA-Z][a-zA-Z0-9]*);?")

    /**
     * 反转义常见的命名实体与数字实体（十进制 `&#39;` / 十六进制 `&#x2605;`）。
     * 无法识别的 `&...` 序列保持原样，避免破坏普通文本中的 `&`。
     */
    fun unescape(input: String): String {
        if (input.isEmpty() || '&' !in input) return input
        return entityRegex.replace(input) { match ->
            val token = match.groupValues[1]
            decodeToken(token) ?: match.value
        }
    }

    private fun decodeToken(token: String): String? {
        namedEntities[token]?.let { return it }
        return when {
            token.startsWith("#x") || token.startsWith("#X") -> {
                token.removePrefix("#x").removePrefix("#X")
                    .toIntOrNull(16)
                    ?.takeIf { it in 1..0x10FFFF }
                    ?.let { codePointToString(it) }
            }
            token.startsWith("#") -> {
                token.removePrefix("#").toIntOrNull()
                    ?.takeIf { it in 1..0x10FFFF }
                    ?.let { codePointToString(it) }
            }
            else -> null
        }
    }

    private fun codePointToString(codePoint: Int): String {
        return if (codePoint <= Char.MAX_VALUE.code) {
            Char(codePoint).toString()
        } else {
            // 补充平面字符（emoji 等），用代理对表示
            String(Character.toChars(codePoint))
        }
    }
}
