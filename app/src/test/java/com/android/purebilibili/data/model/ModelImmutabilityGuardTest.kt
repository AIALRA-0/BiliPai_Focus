package com.android.purebilibili.data.model

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * `data/model` 包不可变性守卫——`compose_stability.conf` 的兑现机制。
 *
 * （注意：本文件的注释里不要写 `data/model` 后跟通配符的形式。Kotlin 的块注释
 * 是**可嵌套**的，那个写法会开出一层嵌套注释，把整个文件吃掉。）
 *
 * 根目录的 `compose_stability.conf` 向 Compose 编译器声明整个
 * `com.android.purebilibili.data.model` 包是 stable，让 `VideoCard` 重新获得
 * skippable 能力。这个声明是**一句承诺**：编译器会无条件相信它，据此跳过重组。
 *
 * 如果哪天有人往模型里加了一个 `var` 或 `MutableList`，编译器**不会报错**，
 * 而是继续跳过本该发生的重组——表现为「数据变了但界面不刷新」，
 * 且因为源头在一个 .conf 文件里，几乎不可能被联想到。
 * **错误的稳定性承诺比不做承诺更危险**，这条测试就是为了让它不可能出错。
 *
 * ### 为什么不能用正则
 *
 * 属性级 `var` 和函数内局部 `var` 的缩进可能完全一样。真实例子：
 * `VideoResponse.kt:249` 的 `    var score = 0` 在一个顶层函数体内，缩进 4 空格，
 * 与类属性无法用缩进区分。所以这里做了一个轻量的括号栈扫描
 * （见 [KotlinSourceScanner]），只把「不在任何函数体内」的声明算作属性。
 */
class ModelImmutabilityGuardTest {

    @Test
    fun modelsHaveNoMutableProperties() {
        val violations = modelSources().flatMap { file ->
            KotlinSourceScanner(file.readText())
                .propertyLevelMatches(VAR_KEYWORD)
                .map { "${file.name}:${it.line}  ${it.text.trim()}" }
        }

        assertTrue(
            violations.isEmpty(),
            buildString {
                appendLine("data/model 下发现属性级 var，共 ${violations.size} 处：")
                violations.forEach { appendLine("  $it") }
                appendLine()
                appendLine(
                    "根目录 compose_stability.conf 已把整个包声明为 stable，" +
                        "可变属性会让这个承诺失效——编译器仍会跳过重组，" +
                        "表现为数据变了界面不刷新。请改成 val；确实需要可变状态的，" +
                        "放到 ViewModel 或 UI state 里，不要放进网络模型。",
                )
            },
        )
    }

    @Test
    fun modelsHaveNoMutableCollectionTypes() {
        val violations = modelSources().flatMap { file ->
            KotlinSourceScanner(file.readText())
                .propertyLevelMatches(MUTABLE_COLLECTION_PATTERN)
                .map { "${file.name}:${it.line}  ${it.text.trim()}" }
        }

        assertTrue(
            violations.isEmpty(),
            buildString {
                appendLine("data/model 下发现可变集合类型，共 ${violations.size} 处：")
                violations.forEach { appendLine("  $it") }
                appendLine()
                appendLine(
                    "即使用 val 持有，MutableList/MutableMap 的内容仍可被就地修改，" +
                        "stable 声明因此不成立。请改用 List/Map/Set 只读接口。",
                )
            },
        )
    }

    /**
     * 稳定性配置的接线守卫。
     *
     * 实测过一次：把 `stabilityConfigurationFiles` 指向一个**不存在的文件**，
     * 编译照样成功、没有任何警告。也就是说路径写错一个字母，整个稳定性声明就
     * 静默失效，而表面上一切正常——`VideoCard` 悄悄退回不可跳过，没人会发现。
     *
     * 所以「文件存在」和「被 build 脚本引用」这两件事必须由测试来保证。
     */
    @Test
    fun stabilityConfigIsWiredAndNonEmpty() {
        val conf = repoFile("compose_stability.conf")
        assertTrue(
            conf.exists(),
            "根目录缺少 compose_stability.conf。app/build.gradle.kts 仍会引用它，" +
                "但 Compose 编译器对不存在的配置文件是**静默忽略**的——" +
                "结果是稳定性声明失效且无任何提示。",
        )

        val declarations = conf.readLines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("//") }
        assertTrue(
            declarations.any { it.startsWith("com.android.purebilibili.data.model") },
            "compose_stability.conf 里没有 data.model 的声明，当前有效声明：$declarations",
        )

        val appBuild = repoFile("app/build.gradle.kts").readText()
        assertTrue(
            appBuild.contains("stabilityConfigurationFiles"),
            "app/build.gradle.kts 没有引用 stabilityConfigurationFiles，配置文件不会生效",
        )
        assertTrue(
            appBuild.contains("compose_stability.conf"),
            "app/build.gradle.kts 引用的稳定性配置文件名不是 compose_stability.conf——" +
                "路径不匹配会被静默忽略，请核对",
        )
    }

    /**
     * 扫描器自身的自检。
     *
     * 这条守卫的价值完全取决于扫描器能不能正确区分属性和局部变量。如果扫描器坏了
     * （比如有人「简化」成正则），上面两条会静默变绿，而不是变红——
     * 这正是最坏的失效方式：守卫看起来还在，实际已经不设防。
     */
    @Test
    fun scannerDistinguishesPropertiesFromLocals() {
        val sample = """
            data class Sample(
                val a: Int = 0,
                var b: Int = 0
            ) {
                var c: Int = 0

                fun compute(): Int {
                    var local = 0
                    listOf(1).forEach { var deeper = it; local += deeper }
                    return local
                }
            }

            // 多行签名：真实存在于 VideoResponse.kt，是扫描器最容易踩空的形状
            private fun scoreOf(
                first: Int,
                second: Int
            ): Int {
                var wrapped = first
                return wrapped + second
            }
        """.trimIndent()

        val hits = KotlinSourceScanner(sample).propertyLevelMatches(VAR_KEYWORD)
        val texts = hits.map { it.text.trim() }

        assertTrue(
            texts.any { it.startsWith("var b") } && texts.any { it.startsWith("var c") },
            "扫描器漏掉了属性级 var，实际命中：$texts",
        )
        assertTrue(
            texts.none { it.contains("local") } && texts.none { it.contains("deeper") },
            "扫描器把函数内的局部 var 误判为属性，实际命中：$texts",
        )
        assertTrue(
            texts.none { it.contains("wrapped") },
            "扫描器在多行函数签名处踩空，把函数体当成了类体，实际命中：$texts",
        )
    }

    /** 单测工作目录是 `app/`，仓库根在上一级。 */
    private fun repoFile(relativePath: String): File =
        File(relativePath).takeIf { it.exists() } ?: File("..", relativePath)

    private fun modelSources(): List<File> {
        val roots = listOf(
            "src/main/java/com/android/purebilibili/data/model",
            "app/src/main/java/com/android/purebilibili/data/model",
        )
        val root = roots.map { File(it) }.firstOrNull { it.isDirectory }
            ?: error("找不到 data/model 目录，cwd=" + File(".").absoluteFile.canonicalPath)

        return root.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
            .also { assertTrue(it.isNotEmpty(), "data/model 下没扫到任何 .kt，扫描逻辑可能已失效") }
    }

    private companion object {
        val VAR_KEYWORD = Regex("""\bvar\s+\w""")

        val MUTABLE_COLLECTION_PATTERN = Regex(
            listOf(
                "MutableList", "MutableMap", "MutableSet",
                "ArrayList", "HashMap", "HashSet", "LinkedHashMap", "LinkedHashSet",
            ).joinToString(separator = "|", prefix = """\b(""", postfix = """)<"""),
        )
    }
}

/**
 * 极简 Kotlin 源码扫描器：判断一个匹配是否落在任何函数体之外。
 *
 * 做法是维护一个花括号栈，每个栈帧记录「这个 `{` 的头部文本里有没有 `fun`」。
 * 匹配点只要有任意一层祖先帧是函数，就算局部声明。
 *
 * 头部文本**跨换行累积**，只在 `{` `}` `;` 处清空。这一点很关键：
 * data/model 下存在多行函数签名（`VideoResponse.kt` 的 `videoPlaybackSelectionScore`
 * 就是），若按换行清空，`): Int {` 这一行的头部里根本看不到 `fun`，
 * 整个函数体会被误判成类体——那会产生**假红**，而假红最终一定以「把测试删掉」收场。
 *
 * 已知边界（对 data/model 这种纯声明式代码足够，但要诚实标出来）：
 * - 若某层 `{` 的头部里残留着上一条表达式体函数的 `fun`（如 `fun a() = 1` 紧跟
 *   `val b = list.map { ... }`），该 lambda 会被当成函数体。这是**漏报**方向，
 *   不会造成假红。
 * - 反方向的误判（把函数体当类体）只可能出现在没有 `fun` 关键字的构造里，
 *   data/model 下不存在这种写法。
 */
internal class KotlinSourceScanner(source: String) {

    private val sanitized = stripCommentsAndLiterals(source)

    data class Match(val line: Int, val text: String)

    /** 返回所有落在函数体之外的匹配。 */
    fun propertyLevelMatches(pattern: Regex): List<Match> {
        val insideFunction = functionBodyMask()
        return pattern.findAll(sanitized)
            .filterNot { insideFunction[it.range.first] }
            .map { Match(line = lineNumberAt(it.range.first), text = lineTextAt(it.range.first)) }
            .toList()
    }

    /** `mask[i] == true` 表示下标 i 处于某个函数体内。 */
    private fun functionBodyMask(): BooleanArray {
        val mask = BooleanArray(sanitized.length)
        // 每个栈帧 = 这层花括号是不是由 `fun` 开启的
        val frames = ArrayDeque<Boolean>()
        // 自上一个 { } ; 或换行以来累积的「头部」文本，用来判断这个 { 属于谁
        val header = StringBuilder()

        sanitized.forEachIndexed { index, ch ->
            mask[index] = frames.any { it }
            when (ch) {
                '{' -> {
                    frames.addLast(FUN_HEADER.containsMatchIn(header))
                    header.clear()
                }
                '}' -> {
                    frames.removeLastOrNull()
                    header.clear()
                    // 闭合的这一刻已经出了函数体
                    mask[index] = frames.any { it }
                }
                ';' -> header.clear()
                else -> header.append(ch)
            }
        }
        return mask
    }

    private fun lineNumberAt(index: Int): Int =
        sanitized.substring(0, index).count { it == '\n' } + 1

    private fun lineTextAt(index: Int): String {
        val start = sanitized.lastIndexOf('\n', index).let { if (it < 0) 0 else it + 1 }
        val end = sanitized.indexOf('\n', index).let { if (it < 0) sanitized.length else it }
        return sanitized.substring(start, end)
    }

    /**
     * 把注释和字符串字面量替换成等长空格。
     *
     * 保持长度不变是关键——这样行号和括号栈的下标才与原文一一对应，
     * 报错信息里的行号才可信。
     */
    private fun stripCommentsAndLiterals(source: String): String {
        val out = StringBuilder(source.length)
        var i = 0
        while (i < source.length) {
            val rest = source.length - i
            when {
                rest >= 2 && source.startsWith("//", i) -> {
                    while (i < source.length && source[i] != '\n') { out.append(' '); i++ }
                }
                rest >= 2 && source.startsWith("/*", i) -> {
                    val end = source.indexOf("*/", i + 2).let { if (it < 0) source.length else it + 2 }
                    while (i < end) { out.append(if (source[i] == '\n') '\n' else ' '); i++ }
                }
                rest >= 3 && source.startsWith("\"\"\"", i) -> {
                    val end = source.indexOf("\"\"\"", i + 3).let { if (it < 0) source.length else it + 3 }
                    while (i < end) { out.append(if (source[i] == '\n') '\n' else ' '); i++ }
                }
                source[i] == '"' -> {
                    out.append(' '); i++
                    while (i < source.length && source[i] != '"') {
                        if (source[i] == '\\' && i + 1 < source.length) { out.append(' '); i++ }
                        out.append(if (source[i] == '\n') '\n' else ' '); i++
                    }
                    if (i < source.length) { out.append(' '); i++ }
                }
                else -> { out.append(source[i]); i++ }
            }
        }
        return out.toString()
    }

    private companion object {
        val FUN_HEADER = Regex("""\bfun\b""")
    }
}
