package com.android.purebilibili.core.ui.migration

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 双主题原生组件迁移的存量棘轮。
 *
 * 这些上限冻结 2026-08-24 的生产源码现状。每完成一个迁移批次，都应同步调低对应上限；
 * 不允许为了让测试通过而提高上限。液态玻璃与共享 Miuix navigation 子系统不属于本轮组件迁移，
 * 因此使用精确文件集合而不是数量上限，防止例外继续扩散。
 */
class NativeThemeMigrationBoundaryTest {

    @Test
    fun featureVendorImportsOnlyDecrease() {
        val featureFiles = kotlinFiles("app/src/main/java/com/android/purebilibili/feature")

        assertAtMost(
            actual = featureFiles.count { it.hasMaterial3VisibleComponentImport() },
            maximum = MAX_FEATURE_MATERIAL3_FILES,
            label = "feature Material3 visible-component import files",
        )
        assertAtMost(
            actual = featureFiles.sumOf { it.material3VisibleComponentImportCount() },
            maximum = MAX_FEATURE_MATERIAL3_IMPORTS,
            label = "feature Material3 visible-component imports",
        )
        assertAtMost(
            actual = featureFiles.countImportLines("import androidx.compose.material3.*"),
            maximum = MAX_FEATURE_MATERIAL3_WILDCARD_IMPORTS,
            label = "feature Material3 wildcard imports",
        )
        val miuixImports = featureFiles.importSites(MIUIX_COMPONENT_IMPORTS)
        val unreviewedMiuixImports = miuixImports - MIUIX_COMPONENT_IMPORT_EXCEPTIONS
        assertTrue(
            unreviewedMiuixImports.isEmpty(),
            "Feature Miuix visible-component imports must stay within the reviewed per-path inventory; " +
                "migrate additions through App* facades:\n${unreviewedMiuixImports.sorted().joinToString("\n")}",
        )
        val miuixIconImports = featureFiles.importSites(listOf(MIUIX_ICON_IMPORT))
        val unreviewedMiuixIconImports = miuixIconImports - MIUIX_ICON_IMPORT_EXCEPTIONS
        assertTrue(
            unreviewedMiuixIconImports.isEmpty(),
            "Feature Miuix icon imports must stay within the exact reviewed path+import inventory; " +
                "migrate additions through AppIcon facades:\n" +
                unreviewedMiuixIconImports.sorted().joinToString("\n"),
        )
    }

    @Test
    fun featureThemeBranchingOnlyDecreases() {
        val featureFiles = kotlinFiles("app/src/main/java/com/android/purebilibili/feature")
        val composableNames = composableFunctionNames() + EXTERNAL_COMPOSABLE_NAMES
        val branchLines = featureFiles.flatMap { file ->
            file.themeBranchDecisionLines(composableNames).map { (lineNumber, line) ->
                "${repoRelativePath(file)}:$lineNumber: $line"
            }
        }
        val branchFiles = branchLines.map { it.substringBefore(':') }.toSet().size

        assertAtMost(branchFiles, MAX_FEATURE_THEME_BRANCH_FILES, "feature theme-branch files")
        assertAtMost(branchLines.size, MAX_FEATURE_THEME_BRANCH_LINES, "feature theme-branch lines")
    }

    @Test
    fun featureSliderColorsUseNeutralAppTokens() {
        val offenders = kotlinFiles("app/src/main/java/com/android/purebilibili/feature")
            .flatMap { file -> file.importedMemberReferences(MATERIAL3_PACKAGES, setOf("SliderDefaults"), "colors") }

        assertTrue(
            offenders.isEmpty(),
            "AppSlider colors must use AppSliderDefaults, not Material3 SliderDefaults:\n" +
                offenders.joinToString("\n"),
        )
    }

    @Test
    fun appDirectSliderCallsUseFacade() {
        val offenders = kotlinFiles("app/src/main/java")
            .nativeWidgetCalls(NATIVE_COMPONENT_PACKAGES, setOf("Slider"))
            .map(NativeWidgetCall::display)

        assertTrue(
            offenders.isEmpty(),
            "App production sources must use AppSlider, not a direct native Slider:\n" +
                offenders.joinToString("\n"),
        )
    }

    @Test
    fun appDirectButtonCallsUseFacade() {
        val nativeCalls = kotlinFiles("app/src/main/java")
            .nativeWidgetCalls(NATIVE_COMPONENT_PACKAGES, NATIVE_BUTTON_NAMES)
        val allowedMiuixRendererCalls = nativeCalls.filter { it.exceptionKey in MIUIX_TEXT_BUTTON_EXCEPTIONS }
        val offenders = nativeCalls
            .filterNot { it.exceptionKey in MIUIX_TEXT_BUTTON_EXCEPTIONS }
            .map(NativeWidgetCall::display)

        assertEquals(
            MIUIX_TEXT_BUTTON_EXCEPTIONS,
            allowedMiuixRendererCalls.map(NativeWidgetCall::exceptionKey).sorted(),
            "Only the four Miuix update dialog TextButton renderer sites are exempt.",
        )

        assertTrue(
            offenders.isEmpty(),
            "App production sources must use App*Button, not a direct native Button:\n" +
                offenders.joinToString("\n"),
        )
    }

    @Test
    fun appIconButtonVisualGeometryDoesNotReuseTouchTargetDimensions() {
        val remainingExceptions = ICON_BUTTON_FIXED_SIZE_EXCEPTIONS
            .mapValues { (_, dimensions) -> dimensions.toMutableList() }
            .toMutableMap()
        val offenders = buildList {
            kotlinFiles("app/src/main/java").forEach { file ->
                val path = repoRelativePath(file)
                file.appIconButtonFixedDimensions().forEach { (lineNumber, dimension) ->
                    val approvedDimensions = remainingExceptions[path]
                    if (approvedDimensions == null || !approvedDimensions.remove(dimension)) {
                        add("$path:$lineNumber: AppIconButton modifier size $dimension")
                    }
                }
            }
        }

        assertTrue(
            offenders.isEmpty(),
            "AppIconButton compact sizes must stay within the exact reviewed per-path inventory; " +
                "48dp minimum targets are unrestricted, but new compact sizes need migration or review:\n" +
                offenders.joinToString("\n"),
        )
    }

    @Test
    fun appDirectIconButtonCallsAndDefaultsUseFacade() {
        val files = kotlinFiles("app/src/main/java")
        val calls = files.nativeWidgetCalls(NATIVE_COMPONENT_PACKAGES, NATIVE_ICON_BUTTON_NAMES)
        val defaults = files.flatMap { file ->
            file.importedMemberReferences(
                NATIVE_COMPONENT_PACKAGES,
                setOf("IconButtonDefaults"),
                member = null,
            )
        }
        val offenders = calls.map(NativeWidgetCall::display) + defaults

        assertTrue(
            offenders.isEmpty(),
            "App production sources must use App*IconButton and AppIconButtonDefaults:\n" +
                offenders.joinToString("\n"),
        )
    }

    @Test
    fun featureDirectProgressCallsOnlyDecrease() {
        val directCalls = kotlinFiles("app/src/main/java/com/android/purebilibili/feature")
            .nativeWidgetCalls(NATIVE_COMPONENT_PACKAGES, NATIVE_PROGRESS_NAMES)
            .map(NativeWidgetCall::display)

        assertAtMost(
            actual = directCalls.size,
            maximum = MAX_FEATURE_DIRECT_PROGRESS_CALLS,
            label = "feature direct native progress calls",
        )
    }

    @Test
    fun designSystemComponentsUseTheProgressFacade() {
        val offenders = kotlinFiles(
            "design-system/src/main/java/com/android/purebilibili/core/ui/components"
        )
            .filterNot { it.name == "AppProgressIndicator.kt" }
            .nativeWidgetCalls(NATIVE_COMPONENT_PACKAGES, NATIVE_PROGRESS_NAMES)
            .map(NativeWidgetCall::display)

        assertTrue(
            offenders.isEmpty(),
            "Design-system components must use App* progress facades:\n" +
                offenders.joinToString("\n"),
        )
    }

    @Test
    fun appNativeCardCallsStayWithinTheExactMd3PreviewException() {
        val directCalls = kotlinFiles("app/src/main/java")
            .flatMap { file ->
                val material3CardNames = file.material3CardImportNames()
                if (material3CardNames.isEmpty()) return@flatMap emptyList()

                var enclosingFunction = "<top-level>"
                buildList {
                    file.readLines().forEach { line ->
                        FUNCTION_DECLARATION.find(line)?.groupValues?.get(1)?.let {
                            enclosingFunction = it
                        }
                        material3CardNames.forEach { cardName ->
                            Regex("(?<![A-Za-z0-9_])${Regex.escape(cardName)}\\(")
                                .findAll(line)
                                .forEach {
                                    add("${repoRelativePath(file)}#$enclosingFunction#$cardName")
                                }
                        }
                    }
                }
            }
            .sorted()

        assertEquals(MD3_CARD_PREVIEW_EXCEPTIONS, directCalls)
    }

    @Test
    fun primitiveFacadeVendorImportsOnlyDecrease() {
        val source = repoFile(
            "design-system/src/main/java/com/android/purebilibili/core/ui/components/" +
                "AppPrimitiveComponents.kt"
        )
        val imports = source.readLines().count { it.startsWith(MATERIAL3_IMPORT) }

        assertAtMost(
            actual = imports,
            maximum = MAX_PRIMITIVE_FACADE_MATERIAL3_IMPORTS,
            label = "AppPrimitiveComponents Material3 imports",
        )
    }

    @Test
    fun rendererPackagesCannotCrossVendorBoundaries() {
        val materialRenderers = kotlinFiles(
            "design-system/src/main/java/com/android/purebilibili/core/ui/renderer/material3"
        )
        val miuixRenderers = kotlinFiles(
            "design-system/src/main/java/com/android/purebilibili/core/ui/renderer/miuix"
        )

        val materialOffenders = materialRenderers.importOffenders(MIUIX_VENDOR_IMPORT)
        val miuixOffenders = miuixRenderers.importOffenders(MATERIAL3_IMPORT)

        assertTrue(
            materialOffenders.isEmpty(),
            "Material3 renderer must not import Miuix:\n${materialOffenders.joinToString("\n")}",
        )
        assertTrue(
            miuixOffenders.isEmpty(),
            "Miuix renderer must not import Material3:\n${miuixOffenders.joinToString("\n")}",
        )
    }

    @Test
    fun liquidGlassEffectExceptionSetCannotExpandOrChangeIdentity() {
        val featureFiles = kotlinFiles("app/src/main/java/com/android/purebilibili/feature")
        val exceptionPaths = featureFiles
            .filter { file -> file.hasAnyImportPrefix(MIUIX_VISUAL_EFFECT_IMPORTS) }
            .map(::repoRelativePath)
            .toSet()

        assertEquals(LIQUID_GLASS_EFFECT_EXCEPTION_PATHS, exceptionPaths)
    }

    @Test
    fun sharedMiuixNavigationExceptionSetCannotExpandOrChangeIdentity() {
        val sourceFiles = kotlinFiles("app/src/main/java") + kotlinFiles("design-system/src/main/java")
        val navigationFiles = sourceFiles
            .filter { it.hasAnyImportPrefix(listOf(MIUIX_NAVIGATION_IMPORT)) }
            .map(::repoRelativePath)
            .toSet()

        assertEquals(MIUIX_NAVIGATION_EXCEPTION_PATHS, navigationFiles)
    }

    /** MaterialTheme, ColorScheme, and token imports are baseline APIs, not visible widgets. */
    private fun File.hasMaterial3VisibleComponentImport(): Boolean =
        material3VisibleComponentImportCount() > 0 || hasAnyImportPrefix(listOf(MATERIAL3_WILDCARD_IMPORT))

    private fun File.material3VisibleComponentImportCount(): Int = readLines().count { line ->
        val match = IMPORT_DIRECTIVE.matchEntire(line) ?: return@count false
        val qualifiedName = match.groupValues[1]
        qualifiedName.substringBeforeLast('.', "") == MATERIAL3_ROOT_PACKAGE &&
            qualifiedName.substringAfterLast('.') in MATERIAL3_VISIBLE_COMPONENT_NAMES
    }

    private fun List<File>.countImportLines(prefix: String): Int = countImportLines(listOf(prefix))

    private fun List<File>.countImportLines(prefixes: List<String>): Int = sumOf { file ->
        file.useLines { lines ->
            lines.count { line -> prefixes.any(line::startsWith) }
        }
    }

    private fun List<File>.importOffenders(prefix: String): List<String> = flatMap { file ->
        file.readLines().mapIndexedNotNull { index, line ->
            if (line.startsWith(prefix)) {
                "${repoRelativePath(file)}:${index + 1}: $line"
            } else {
                null
            }
        }
    }

    private fun List<File>.importSites(prefixes: List<String>): Set<String> = flatMap { file ->
        file.readLines().mapNotNull { line ->
            if (prefixes.any(line::startsWith)) "${repoRelativePath(file)}#$line" else null
        }
    }.toSet()

    private fun File.hasAnyImportPrefix(prefixes: List<String>): Boolean = useLines { lines ->
        lines.any { line -> prefixes.any(line::startsWith) }
    }

    /** Direct Card calls are only a bypass of the adaptive facade when resolved to Material3. */
    private fun File.material3CardImportNames(): Set<String> = readLines().mapNotNull { line ->
        val match = MATERIAL3_CARD_IMPORT.matchEntire(line) ?: return@mapNotNull null
        match.groupValues[2].ifEmpty { match.groupValues[1] }
    }.toSet().let { imports ->
        if (readLines().any { it == MATERIAL3_WILDCARD_IMPORT }) {
            imports + setOf("Card", "ElevatedCard", "OutlinedCard")
        } else {
            imports
        }
    }

    private fun List<File>.nativeWidgetCalls(
        packages: Set<String>,
        widgetNames: Set<String>,
    ): List<NativeWidgetCall> = flatMap { file ->
        val importedNames = file.importedNames(packages, widgetNames)
        val callPatterns = buildList {
            importedNames.forEach { importedName ->
                add(importedName to Regex("(?<![A-Za-z0-9_.])${Regex.escape(importedName)}\\s*\\("))
            }
            packages.forEach { packageName ->
                widgetNames.forEach { widgetName ->
                    add(
                        widgetName to Regex(
                            "(?<![A-Za-z0-9_.])${Regex.escape(packageName)}\\." +
                                "${Regex.escape(widgetName)}\\s*\\(",
                        ),
                    )
                }
            }
        }

        var enclosingFunction = "<top-level>"
        val occurrences = mutableMapOf<Pair<String, String>, Int>()
        val sourceLines = file.readLines()
        val codeLines = file.readText().withoutCommentsAndStringLiterals().lines()
        buildList {
            codeLines.forEachIndexed { index, line ->
                FUNCTION_DECLARATION.find(line)?.groupValues?.get(1)?.let {
                    enclosingFunction = it
                }
                callPatterns.forEach { (importedName, call) ->
                    call.findAll(line).forEach {
                        val key = enclosingFunction to importedName
                        val occurrence = (occurrences[key] ?: 0) + 1
                        occurrences[key] = occurrence
                        add(
                            NativeWidgetCall(
                                path = repoRelativePath(file),
                                lineNumber = index + 1,
                                enclosingFunction = enclosingFunction,
                                symbol = importedName,
                                occurrence = occurrence,
                                sourceLine = sourceLines.getOrElse(index) { line }.trim(),
                            ),
                        )
                    }
                }
            }
        }
    }.sortedWith(compareBy(NativeWidgetCall::path, NativeWidgetCall::lineNumber, NativeWidgetCall::symbol))

    private fun File.importedNames(packages: Set<String>, candidates: Set<String>): Set<String> {
        val visibleNames = mutableSetOf<String>()
        readLines().forEach { line ->
            val match = IMPORT_DIRECTIVE.matchEntire(line) ?: return@forEach
            val qualifiedName = match.groupValues[1]
            val alias = match.groupValues[2]
            if (qualifiedName.endsWith(".*")) {
                if (qualifiedName.removeSuffix(".*") in packages) visibleNames += candidates
                return@forEach
            }

            val packageName = qualifiedName.substringBeforeLast('.', "")
            val symbolName = qualifiedName.substringAfterLast('.')
            if (packageName in packages && symbolName in candidates) {
                visibleNames += alias.ifEmpty { symbolName }
            }
        }
        return visibleNames
    }

    private fun File.importedMemberReferences(
        packages: Set<String>,
        receiverNames: Set<String>,
        member: String?,
    ): List<String> {
        val importedReceivers = importedNames(packages, receiverNames)
        val receiverPatterns = buildList {
            importedReceivers.forEach { receiver ->
                val memberPattern = member?.let { "\\s*\\.\\s*${Regex.escape(it)}\\b" } ?: "\\s*\\."
                add(
                    receiver to Regex(
                        "(?<![A-Za-z0-9_.])${Regex.escape(receiver)}$memberPattern",
                    ),
                )
            }
            packages.forEach { packageName ->
                receiverNames.forEach { receiver ->
                    val memberPattern = member?.let { "\\s*\\.\\s*${Regex.escape(it)}\\b" } ?: "\\s*\\."
                    add(
                        receiver to Regex(
                            "(?<![A-Za-z0-9_.])${Regex.escape(packageName)}\\." +
                                "${Regex.escape(receiver)}$memberPattern",
                        ),
                    )
                }
            }
        }
        if (receiverPatterns.isEmpty()) return emptyList()

        return readLines().flatMapIndexed { index, line ->
            receiverPatterns.flatMap { (_, receiverPattern) ->
                receiverPattern
                    .findAll(line)
                    .map { "${repoRelativePath(this)}:${index + 1}: ${line.trim()}" }
                    .toList()
            }
        }
    }

    /** Counts only theme decisions that choose different composable renderers. */
    private fun File.themeBranchDecisionLines(composableNames: Set<String>): List<Pair<Int, String>> {
        val originalLines = readLines()
        val codeLines = readText().withoutCommentsAndStringLiterals().lines()
        val decisions = linkedMapOf<Int, String>()

        codeLines.forEachIndexed { index, codeLine ->
            THEME_BRANCH_START.findAll(codeLine).forEach { marker ->
                val header = captureDelimitedExpression(codeLines, index, marker.range.first, '(', ')')
                    ?: return@forEach
                if (!THEME_BRANCH_STYLE_REFERENCE.containsMatchIn(header.text)) return@forEach

                val source = originalLines.getOrElse(index) { "" }.trim()
                if (marker.value.startsWith("if")) {
                    val thenBranch = captureBranchExpression(codeLines, header.endLine, header.endOffset)
                    val elseMarker = findElse(codeLines, thenBranch.endLine, thenBranch.endOffset)
                        ?: return@forEach
                    val elseBranch = captureBranchExpression(codeLines, elseMarker.first, elseMarker.second)
                    val thenCall = firstComposableCall(thenBranch.text, composableNames)
                    val elseCall = firstComposableCall(elseBranch.text, composableNames)
                    if (thenCall != null && elseCall != null && thenCall != elseCall) {
                        decisions[index + 1] = source
                    }
                } else {
                    val body = captureBranchExpression(codeLines, header.endLine, header.endOffset)
                    val arms = THEME_WHEN_BRANCH_ARM.findAll(body.text).associate { arm ->
                        val expressionStart = arm.range.last + 1
                        val nextArmStart = THEME_WHEN_BRANCH_ARM.find(body.text, expressionStart)?.range?.first
                            ?: body.text.length
                        arm.groupValues[1] to firstComposableCall(
                            body.text.substring(expressionStart, nextArmStart),
                            composableNames,
                        )
                    }
                    val materialCall = arms["MATERIAL3"]
                    val miuixCall = arms["MIUIX"]
                    if (materialCall != null && miuixCall != null && materialCall != miuixCall) {
                        decisions[index + 1] = source
                    }
                }
            }
        }

        return decisions.entries.map { it.key to it.value }
    }

    private fun composableFunctionNames(): Set<String> =
        (kotlinFiles("app/src/main/java") + kotlinFiles("design-system/src/main/java"))
            .flatMap { file ->
                COMPOSABLE_FUNCTION_DECLARATION.findAll(file.readText().withoutCommentsAndStringLiterals())
                    .map { it.groupValues[1] }
                    .toList()
            }
            .toSet()

    private fun captureDelimitedExpression(
        lines: List<String>,
        startLine: Int,
        startOffset: Int,
        open: Char,
        close: Char,
    ): CapturedExpression? {
        val text = StringBuilder()
        var depth = 0
        for (lineIndex in startLine until lines.size) {
            val line = lines[lineIndex]
            val offset = if (lineIndex == startLine) startOffset else 0
            for (column in offset until line.length) {
                val character = line[column]
                text.append(character)
                if (character == open) depth++
                if (character == close && --depth < 0) {
                    // A closing delimiter before this expression opened means malformed input.
                    return null
                } else if (character == close && depth == 0) {
                    return CapturedExpression(text.toString(), lineIndex, column + 1)
                }
            }
            text.append('\n')
        }
        return null
    }

    private fun captureBranchExpression(lines: List<String>, startLine: Int, startOffset: Int): CapturedExpression {
        var lineIndex = startLine
        var column = startOffset
        while (lineIndex < lines.size && (column >= lines[lineIndex].length || lines[lineIndex][column].isWhitespace())) {
            lineIndex++
            column = 0
        }
        if (lineIndex >= lines.size) return CapturedExpression("", lines.lastIndex, lines.last().length)

        val text = StringBuilder()
        if (lines[lineIndex][column] == '{') {
            var braceDepth = 0
            for (currentLine in lineIndex until lines.size) {
                val line = lines[currentLine]
                val offset = if (currentLine == lineIndex) column else 0
                for (currentColumn in offset until line.length) {
                    val character = line[currentColumn]
                    text.append(character)
                    if (character == '{') braceDepth++
                    if (character == '}') {
                        braceDepth--
                        if (braceDepth == 0) {
                            return CapturedExpression(text.toString(), currentLine, currentColumn + 1)
                        }
                    }
                }
                text.append('\n')
            }
            return CapturedExpression(text.toString(), lines.lastIndex, lines.last().length)
        }

        var parenDepth = 0
        for (currentLine in lineIndex until lines.size) {
            val line = lines[currentLine]
            val offset = if (currentLine == lineIndex) column else 0
            for (currentColumn in offset until line.length) {
                if (parenDepth == 0 && line.startsWith("else", currentColumn)) {
                    return CapturedExpression(text.toString(), currentLine, currentColumn)
                }
                val character = line[currentColumn]
                if (character == '}' && parenDepth == 0) {
                    return CapturedExpression(text.toString(), currentLine, currentColumn)
                }
                text.append(character)
                if (character == '(') parenDepth++
                if (character == ')') parenDepth--
            }
            if (parenDepth == 0) return CapturedExpression(text.toString(), currentLine, line.length)
            text.append('\n')
        }
        return CapturedExpression(text.toString(), lines.lastIndex, lines.last().length)
    }

    private fun findElse(lines: List<String>, startLine: Int, startOffset: Int): Pair<Int, Int>? {
        var lineIndex = startLine
        var column = startOffset
        while (lineIndex < lines.size) {
            val line = lines[lineIndex]
            while (column < line.length && line[column].isWhitespace()) column++
            if (column < line.length) {
                return if (line.startsWith("else", column) &&
                    (column + 4 == line.length || !line[column + 4].isLetterOrDigit())
                ) {
                    lineIndex to column
                } else {
                    null
                }
            }
            lineIndex++
            column = 0
        }
        return null
    }

    private fun firstComposableCall(source: String, composableNames: Set<String>): String? =
        COMPOSABLE_CALL.findAll(source).firstOrNull { it.groupValues[1] in composableNames }
            ?.groupValues?.get(1)

    /** Replaces comments and literals with spaces while keeping newlines and code positions. */
    private fun String.withoutCommentsAndStringLiterals(): String {
        val result = StringBuilder(length)
        var index = 0
        var blockCommentDepth = 0
        var inLineComment = false
        var inString = false
        var inRawString = false
        var inCharacter = false
        var escaped = false

        while (index < length) {
            val current = this[index]
            val next = getOrNull(index + 1)
            val afterNext = getOrNull(index + 2)
            when {
                inLineComment -> {
                    if (current == '\n') {
                        inLineComment = false
                        result.append(current)
                    } else {
                        result.append(' ')
                    }
                    index += 1
                }
                blockCommentDepth > 0 -> {
                    when {
                        current == '/' && next == '*' -> {
                            blockCommentDepth += 1
                            result.append("  ")
                            index += 2
                        }
                        current == '*' && next == '/' -> {
                            blockCommentDepth -= 1
                            result.append("  ")
                            index += 2
                        }
                        else -> {
                            result.append(if (current == '\n') '\n' else ' ')
                            index += 1
                        }
                    }
                }
                inRawString -> {
                    if (current == '"' && next == '"' && afterNext == '"') {
                        inRawString = false
                        result.append("   ")
                        index += 3
                    } else {
                        result.append(if (current == '\n') '\n' else ' ')
                        index += 1
                    }
                }
                inString || inCharacter -> {
                    when {
                        escaped -> {
                            escaped = false
                            result.append(' ')
                            index += 1
                        }
                        current == '\\' -> {
                            escaped = true
                            result.append(' ')
                            index += 1
                        }
                        inString && current == '"' -> {
                            inString = false
                            result.append(' ')
                            index += 1
                        }
                        inCharacter && current == '\'' -> {
                            inCharacter = false
                            result.append(' ')
                            index += 1
                        }
                        else -> {
                            result.append(if (current == '\n') '\n' else ' ')
                            index += 1
                        }
                    }
                }
                current == '/' && next == '/' -> {
                    inLineComment = true
                    result.append("  ")
                    index += 2
                }
                current == '/' && next == '*' -> {
                    blockCommentDepth = 1
                    result.append("  ")
                    index += 2
                }
                current == '"' && next == '"' && afterNext == '"' -> {
                    inRawString = true
                    result.append("   ")
                    index += 3
                }
                current == '"' -> {
                    inString = true
                    result.append(' ')
                    index += 1
                }
                current == '\'' -> {
                    inCharacter = true
                    result.append(' ')
                    index += 1
                }
                else -> {
                    result.append(current)
                    index += 1
                }
            }
        }
        return result.toString()
    }

    private fun File.callHeaders(callName: String): List<Pair<Int, String>> {
        val marker = Regex("(?<![A-Za-z0-9_])${Regex.escape(callName)}\\s*\\(")
        var startLine = -1
        var parenthesisDepth = 0
        var header = StringBuilder()
        val codeLines = readText().withoutCommentsAndStringLiterals().lines()

        return buildList {
            codeLines.forEachIndexed { index, line ->
                if (startLine < 0) {
                    val match = marker.find(line) ?: return@forEachIndexed
                    val markerIndex = match.range.first
                    startLine = index + 1
                    val firstLine = line.substring(markerIndex)
                    header.appendLine(firstLine)
                    parenthesisDepth += firstLine.count { it == '(' } - firstLine.count { it == ')' }
                } else {
                    header.appendLine(line)
                    parenthesisDepth += line.count { it == '(' } - line.count { it == ')' }
                }

                if (parenthesisDepth <= 0) {
                    add(startLine to header.toString())
                    startLine = -1
                    parenthesisDepth = 0
                    header = StringBuilder()
                }
            }
            check(startLine < 0) { "Unterminated $callName call header in ${path}" }
        }
    }

    private fun File.appIconButtonFixedDimensions(): List<Pair<Int, String>> =
        callHeaders("AppIconButton").flatMap { (lineNumber, header) ->
            FIXED_ICON_BUTTON_SIZE_CALL.findAll(header).mapNotNull { sizeCall ->
                val dimensions = sizeCall.groupValues[1].replace(WHITESPACE, "")
                val dpValues = FIXED_DP_LITERAL.findAll(dimensions).map { it.groupValues[1] }.toList()
                if (dpValues.isEmpty() || dpValues.all { it == "48" || it == "48.0" }) {
                    null
                } else {
                    lineNumber to dimensions
                }
            }.toList()
        }

    private fun assertAtMost(actual: Int, maximum: Int, label: String) {
        assertTrue(
            actual <= maximum,
            "$label increased to $actual (frozen maximum: $maximum). " +
                "Route feature UI through App* and lower the migration budget after each batch.",
        )
    }

    private fun kotlinFiles(path: String): List<File> = repoFile(path)
        .walkTopDown()
        .filter { it.isFile && it.extension == "kt" }
        .toList()

    private fun repoFile(path: String): File = File(repositoryRoot(), path)

    private fun repoRelativePath(file: File): String = file
        .relativeTo(repositoryRoot())
        .invariantSeparatorsPath

    private fun repositoryRoot(): File {
        val cwd = File(".").absoluteFile.canonicalFile
        return when {
            File(cwd, "app/src/main/java").isDirectory -> cwd
            else -> {
                val parent = cwd.parentFile
                if (File(cwd, "src/main/java").isDirectory && parent != null &&
                    File(parent, "design-system").isDirectory
                ) {
                    parent.canonicalFile
                } else {
                    error("Cannot locate repository root from ${cwd.path}")
                }
            }
        }
    }

    private data class CapturedExpression(
        val text: String,
        val endLine: Int,
        val endOffset: Int,
    )

    private data class NativeWidgetCall(
        val path: String,
        val lineNumber: Int,
        val enclosingFunction: String,
        val symbol: String,
        val occurrence: Int,
        val sourceLine: String,
    ) {
        val exceptionKey: String
            get() = "$path#$enclosingFunction#$symbol@$occurrence"

        val display: String
            get() = "$path:$lineNumber: $sourceLine [$symbol]"
    }

    private companion object {
        const val MATERIAL3_IMPORT = "import androidx.compose.material3."
        const val MATERIAL3_ROOT_PACKAGE = "androidx.compose.material3"
        const val MIUIX_VENDOR_IMPORT = "import top.yukonga.miuix."
        const val MIUIX_ICON_IMPORT = "import top.yukonga.miuix.kmp.icon."
        const val MIUIX_NAVIGATION_IMPORT = "import top.yukonga.miuix.kmp.nav."
        const val MATERIAL3_WILDCARD_IMPORT = "import androidx.compose.material3.*"
        val MATERIAL3_PACKAGES = setOf("androidx.compose.material3")
        val MATERIAL3_VISIBLE_COMPONENT_NAMES = setOf(
            "AlertDialog", "BasicAlertDialog", "Badge", "BadgedBox", "BottomAppBar", "BottomSheetScaffold",
            "Button", "ButtonDefaults", "Card", "Checkbox", "CircularProgressIndicator",
            "CircularWavyProgressIndicator", "DatePicker", "DatePickerDialog", "Divider", "DropdownMenu",
            "DropdownMenuItem", "ElevatedButton", "ElevatedCard", "ExtendedFloatingActionButton",
            "FilterChip", "FilterChipDefaults", "FloatingActionButton", "FloatingToolbarDefaults",
            "FilledIconButton", "FilledTonalButton", "FilledTonalIconButton", "HorizontalDivider",
            "HorizontalFloatingToolbar", "Icon", "IconButton", "IconButtonDefaults", "ListItem",
            "LinearProgressIndicator", "ModalBottomSheet", "NavigationBar", "NavigationBarItem",
            "NavigationBarItemDefaults", "NavigationDrawerItem", "NavigationRail", "OutlinedButton",
            "OutlinedCard", "OutlinedIconButton", "OutlinedTextField", "OutlinedTextFieldDefaults",
            "PlainTooltip", "RadioButton", "RangeSlider", "Scaffold", "SearchBar", "SegmentedButton",
            "Slider", "Snackbar", "SnackbarHost", "Surface", "SwipeToDismissBox", "Switch", "Tab",
            "TabRow", "Text", "TextButton", "TimePicker", "TopAppBar", "TopAppBarDefaults",
            "VerticalDivider",
        )
        val NATIVE_COMPONENT_PACKAGES = setOf(
            "androidx.compose.material3",
            "top.yukonga.miuix.kmp.basic",
        )
        val NATIVE_BUTTON_NAMES = setOf(
            "Button",
            "ElevatedButton",
            "FilledButton",
            "FilledTonalButton",
            "OutlinedButton",
            "TextButton",
        )
        val NATIVE_ICON_BUTTON_NAMES = setOf(
            "IconButton",
            "FilledIconButton",
            "FilledTonalIconButton",
            "OutlinedIconButton",
        )
        val NATIVE_PROGRESS_NAMES = setOf("CircularProgressIndicator", "LinearProgressIndicator")
        val IMPORT_DIRECTIVE = Regex("^import\\s+([A-Za-z0-9_.*]+)(?:\\s+as\\s+([A-Za-z0-9_]+))?$")
        val MATERIAL3_CARD_IMPORT = Regex(
            "^import androidx\\.compose\\.material3\\.(Card|ElevatedCard|OutlinedCard)(?:\\s+as\\s+(\\w+))?$",
        )

        val MIUIX_COMPONENT_IMPORTS = listOf(
            "import top.yukonga.miuix.kmp.basic.",
            "import top.yukonga.miuix.kmp.preference.",
            "import top.yukonga.miuix.kmp.overlay.",
        )
        // Both merge parents contain these same nine paths and 38 icon imports. Two wildcard
        // imports remain only in style-aware icon policy files; the inventory allows contraction.
        val MIUIX_ICON_IMPORT_EXCEPTIONS = mapOf(
            "app/src/main/java/com/android/purebilibili/feature/video/ui/section/AiSummarySection.kt" to
                listOf("MiuixIcons", "basic.ArrowRight"),
            "app/src/main/java/com/android/purebilibili/feature/settings/SettingsSemanticIconPolicy.kt" to
                listOf("MiuixIcons", "extended.*"),
            "app/src/main/java/com/android/purebilibili/feature/settings/screen/SettingsNavigationIconPreviewPolicy.kt" to
                listOf("MiuixIcons", "extended.*"),
            "app/src/main/java/com/android/purebilibili/feature/settings/webdav/WebDavBackupScreen.kt" to
                listOf("MiuixIcons", "extended.Reset", "extended.UploadCloud"),
            "app/src/main/java/com/android/purebilibili/feature/settings/share/SettingsShareScreen.kt" to
                listOf("MiuixIcons", "extended.Report", "extended.Tasks"),
            "app/src/main/java/com/android/purebilibili/feature/settings/update/MiuixAppUpdateDialog.kt" to
                listOf("MiuixIcons", "basic.Close"),
            "app/src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt" to
                listOf("MiuixIcons", "extended.Search"),
            "app/src/main/java/com/android/purebilibili/feature/home/components/HomeNavigationIconPolicy.kt" to
                listOf(
                    "MiuixIcons", "extended.Community", "extended.Contacts", "extended.ContactsCircle",
                    "extended.Favorites", "extended.Folder", "extended.GridView", "extended.Home",
                    "extended.Music", "extended.Notes", "extended.Play", "extended.Recent",
                    "extended.Recording", "extended.Settings", "extended.Stopwatch", "extended.Store",
                    "extended.Theme", "extended.TopDownloads",
                ),
            "app/src/main/java/com/android/purebilibili/feature/home/components/HomeHeader.kt" to
                listOf("MiuixIcons", "extended.Messages", "extended.Search", "extended.Settings"),
        ).flatMapTo(mutableSetOf()) { (path, icons) ->
            icons.map { icon -> "$path#import top.yukonga.miuix.kmp.icon.$icon" }
        }
        // These remaining sites are specialized popups, dropdowns, update-renderer controls, or
        // chrome APIs without an equivalent shared facade. The path+import identity set only
        // allows contraction; a new native import requires review and an App* facade migration.
        val MIUIX_COMPONENT_IMPORT_EXCEPTIONS = setOf(
            "app/src/main/java/com/android/purebilibili/feature/audio/screen/MusicPlayerContent.kt#" +
                "import top.yukonga.miuix.kmp.basic.ListPopupColumn",
            "app/src/main/java/com/android/purebilibili/feature/audio/screen/MusicPlayerContent.kt#" +
                "import top.yukonga.miuix.kmp.basic.ListPopupDefaults",
            "app/src/main/java/com/android/purebilibili/feature/audio/screen/MusicPlayerContent.kt#" +
                "import top.yukonga.miuix.kmp.basic.PopupPositionProvider",
            "app/src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt#" +
                "import top.yukonga.miuix.kmp.basic.NavigationBarDefaults as MiuixNavigationBarDefaults",
            "app/src/main/java/com/android/purebilibili/feature/search/SearchVideoFilterSheet.kt#" +
                "import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet",
            "app/src/main/java/com/android/purebilibili/feature/settings/update/MiuixAppUpdateDialog.kt#" +
                "import top.yukonga.miuix.kmp.basic.ButtonDefaults",
            "app/src/main/java/com/android/purebilibili/feature/settings/update/MiuixAppUpdateDialog.kt#" +
                "import top.yukonga.miuix.kmp.basic.Card",
            "app/src/main/java/com/android/purebilibili/feature/settings/update/MiuixAppUpdateDialog.kt#" +
                "import top.yukonga.miuix.kmp.basic.CardDefaults",
            "app/src/main/java/com/android/purebilibili/feature/settings/update/MiuixAppUpdateDialog.kt#" +
                "import top.yukonga.miuix.kmp.basic.HorizontalDivider",
            "app/src/main/java/com/android/purebilibili/feature/settings/update/MiuixAppUpdateDialog.kt#" +
                "import top.yukonga.miuix.kmp.basic.Icon",
            "app/src/main/java/com/android/purebilibili/feature/settings/update/MiuixAppUpdateDialog.kt#" +
                "import top.yukonga.miuix.kmp.basic.Text",
            "app/src/main/java/com/android/purebilibili/feature/settings/update/MiuixAppUpdateDialog.kt#" +
                "import top.yukonga.miuix.kmp.basic.TextButton",
            "app/src/main/java/com/android/purebilibili/feature/video/ui/components/AudioQualitySelectionMenu.kt#" +
                "import top.yukonga.miuix.kmp.basic.DropdownImpl",
            "app/src/main/java/com/android/purebilibili/feature/video/ui/components/AudioQualitySelectionMenu.kt#" +
                "import top.yukonga.miuix.kmp.basic.DropdownItem",
            "app/src/main/java/com/android/purebilibili/feature/video/ui/components/ChapterListPanel.kt#" +
                "import top.yukonga.miuix.kmp.basic.DropdownImpl",
            "app/src/main/java/com/android/purebilibili/feature/video/ui/components/ChapterListPanel.kt#" +
                "import top.yukonga.miuix.kmp.basic.DropdownItem",
            "app/src/main/java/com/android/purebilibili/feature/video/ui/components/PagesSelector.kt#" +
                "import top.yukonga.miuix.kmp.basic.DropdownImpl",
            "app/src/main/java/com/android/purebilibili/feature/video/ui/components/PagesSelector.kt#" +
                "import top.yukonga.miuix.kmp.basic.DropdownItem",
            "app/src/main/java/com/android/purebilibili/feature/video/ui/components/PlayerMiuixListPopup.kt#" +
                "import top.yukonga.miuix.kmp.basic.ListPopupColumn",
            "app/src/main/java/com/android/purebilibili/feature/video/ui/components/PlayerMiuixListPopup.kt#" +
                "import top.yukonga.miuix.kmp.basic.ListPopupDefaults",
            "app/src/main/java/com/android/purebilibili/feature/video/ui/components/PlayerMiuixListPopup.kt#" +
                "import top.yukonga.miuix.kmp.basic.PopupPositionProvider",
            "app/src/main/java/com/android/purebilibili/feature/video/ui/components/PlayerMiuixListPopup.kt#" +
                "import top.yukonga.miuix.kmp.basic.SmallTitle",
            "app/src/main/java/com/android/purebilibili/feature/video/ui/components/QualityMenu.kt#" +
                "import top.yukonga.miuix.kmp.basic.DropdownImpl",
            "app/src/main/java/com/android/purebilibili/feature/video/ui/components/QualityMenu.kt#" +
                "import top.yukonga.miuix.kmp.basic.DropdownItem",
            "app/src/main/java/com/android/purebilibili/feature/video/ui/components/VideoAspectRatio.kt#" +
                "import top.yukonga.miuix.kmp.basic.DropdownImpl",
            "app/src/main/java/com/android/purebilibili/feature/video/ui/components/VideoAspectRatio.kt#" +
                "import top.yukonga.miuix.kmp.basic.DropdownItem",
            "app/src/main/java/com/android/purebilibili/feature/video/ui/overlay/PlaybackOrderSelectionSheet.kt#" +
                "import top.yukonga.miuix.kmp.basic.DropdownImpl",
            "app/src/main/java/com/android/purebilibili/feature/video/ui/overlay/PlaybackOrderSelectionSheet.kt#" +
                "import top.yukonga.miuix.kmp.basic.DropdownItem",
            "app/src/main/java/com/android/purebilibili/feature/video/ui/section/AiSummarySection.kt#" +
                "import top.yukonga.miuix.kmp.basic.BasicComponent",
            "app/src/main/java/com/android/purebilibili/feature/video/ui/section/AiSummarySection.kt#" +
                "import top.yukonga.miuix.kmp.basic.Text as MiuixText",
        )

        // Miuix AppIconButton applies min48 sizeIn before the caller Modifier, so these compact
        // call-site sizes preserve icon-control layout while the facade keeps a 48dp hit target.
        // The multiset is path-scoped: existing sites may disappear, but additions require review.
        val ICON_BUTTON_FIXED_SIZE_EXCEPTIONS = mapOf(
            "app/src/main/java/com/android/purebilibili/core/ui/common/TextSelectionBottomSheet.kt" to
                listOf("36.dp"),
            "app/src/main/java/com/android/purebilibili/feature/audio/screen/ListenVideoScreen.kt" to
                listOf("40.dp"),
            "app/src/main/java/com/android/purebilibili/feature/download/OfflineVideoPlayerScreen.kt" to
                listOf("36.dp"),
            "app/src/main/java/com/android/purebilibili/feature/live/LiveListScreen.kt" to
                listOf("40.dp", "40.dp", "40.dp"),
            "app/src/main/java/com/android/purebilibili/feature/login/LoginComponents.kt" to
                listOf("38.dp"),
            "app/src/main/java/com/android/purebilibili/feature/message/InboxScreen.kt" to
                listOf("24.dp"),
            "app/src/main/java/com/android/purebilibili/feature/plugin/AdFilterPlugin.kt" to
                listOf("32.dp"),
            "app/src/main/java/com/android/purebilibili/feature/profile/ProfileScreen.kt" to
                listOf("40.dp"),
            "app/src/main/java/com/android/purebilibili/feature/search/SearchLandingUi.kt" to
                listOf("28.dp", "40.dp"),
            "app/src/main/java/com/android/purebilibili/feature/search/SearchScreen.kt" to
                listOf("24.dp"),
            "app/src/main/java/com/android/purebilibili/feature/settings/screen/CommentFraudHistoryScreen.kt" to
                listOf("32.dp"),
            "app/src/main/java/com/android/purebilibili/feature/settings/screen/JsonPluginEditorScreen.kt" to
                listOf("24.dp"),
            "app/src/main/java/com/android/purebilibili/feature/settings/screen/SettingsScreen.kt" to
                listOf("32.dp"),
            "app/src/main/java/com/android/purebilibili/feature/video/screen/VideoDetailScreenStateHolder.kt" to
                listOf("width=42.dp,height=34.dp", "width=42.dp,height=34.dp", "width=42.dp,height=34.dp"),
            "app/src/main/java/com/android/purebilibili/feature/video/ui/components/CollectionRow.kt" to
                listOf("28.dp"),
            "app/src/main/java/com/android/purebilibili/feature/video/ui/components/CommentInputBar.kt" to
                listOf("36.dp"),
            "app/src/main/java/com/android/purebilibili/feature/video/ui/components/CommentSearchSheet.kt" to
                listOf("36.dp"),
            "app/src/main/java/com/android/purebilibili/feature/video/ui/components/DanmakuPoolSheet.kt" to
                listOf("28.dp", "36.dp"),
            "app/src/main/java/com/android/purebilibili/feature/video/ui/components/DanmakuSendDialog.kt" to
                listOf("32.dp"),
            "app/src/main/java/com/android/purebilibili/feature/video/ui/components/DanmakuSettingsPanel.kt" to
                listOf("32.dp", "32.dp", "32.dp"),
            "app/src/main/java/com/android/purebilibili/feature/video/ui/components/LandscapeDanmakuComposer.kt" to
                listOf("40.dp"),
            "app/src/main/java/com/android/purebilibili/feature/video/ui/components/SubReplyDetailComponents.kt" to
                listOf("40.dp"),
            "app/src/main/java/com/android/purebilibili/feature/video/ui/overlay/LandscapeTopControlBar.kt" to
                listOf("36.dp", "36.dp", "40.dp"),
            "app/src/main/java/com/android/purebilibili/feature/video/ui/pager/PortraitVideoPager.kt" to
                listOf("36.dp*longPressSpeedHintScale"),
            "app/src/main/java/com/android/purebilibili/feature/video/ui/section/VideoPlayerSection.kt" to
                listOf("36.dp*longPressSpeedHintScale"),
        )
        val MIUIX_VISUAL_EFFECT_IMPORTS = listOf(
            "import top.yukonga.miuix.kmp.blur.",
            "import top.yukonga.miuix.kmp.shader.",
            "import top.yukonga.miuix.kmp.squircle.",
        )
        val THEME_BRANCH_START = Regex("\\b(?:if|when)\\s*\\(")
        val THEME_BRANCH_STYLE_REFERENCE = Regex("\\b(?:LocalAppUiStyle|AppUiStyle)\\b")
        val THEME_WHEN_BRANCH_ARM = Regex("\\bAppUiStyle\\.(MIUIX|MATERIAL3)\\s*->")
        val COMPOSABLE_FUNCTION_DECLARATION = Regex(
            "@Composable\\s+(?:(?:private|internal|public|inline|suspend)\\s+)*fun\\s+([A-Z][A-Za-z0-9_]*)\\s*\\(",
        )
        val COMPOSABLE_CALL = Regex("\\b([A-Z][A-Za-z0-9_]*)\\s*\\(")
        val FIXED_ICON_BUTTON_SIZE_CALL = Regex("\\.(?:size|width|height)\\s*\\(([^()]*)\\)")
        val FIXED_DP_LITERAL = Regex("\\b(\\d+(?:\\.\\d+)?)\\.dp\\b")
        val WHITESPACE = Regex("\\s+")
        val EXTERNAL_COMPOSABLE_NAMES = setOf(
            "AlertDialog", "AppAssistChip", "AppButton", "AppOutlinedButton", "AppSurface", "AppTextButton",
            "AppThemeAdaptiveTabRow", "BasicAlertDialog", "BasicRichTextEditor", "BasicTextField", "Box",
            "Button", "Column", "Divider", "FilledTonalButton", "FlowRow", "Icon", "IconButton", "LazyColumn",
            "LazyRow", "Material3AppUpdateDialog", "MiuixAppUpdateDialog", "OutlinedButton", "RichTextEditor",
            "Row", "Scaffold", "Surface", "Text", "TextButton",
        )
        val FUNCTION_DECLARATION = Regex("\\bfun\\s+([A-Za-z0-9_]+)\\s*\\(")

        // This exact Miuix renderer surface uses text-as-parameter actions with platform labels.
        // Each site remains visible by owner function and occurrence, so a fifth call cannot hide.
        val MIUIX_TEXT_BUTTON_EXCEPTIONS = listOf(
            "app/src/main/java/com/android/purebilibili/feature/settings/update/" +
                "MiuixAppUpdateDialog.kt#MiuixDownloadChannels#TextButton@1",
            "app/src/main/java/com/android/purebilibili/feature/settings/update/" +
                "MiuixAppUpdateDialog.kt#MiuixDownloadChannels#TextButton@2",
            "app/src/main/java/com/android/purebilibili/feature/settings/update/" +
                "MiuixAppUpdateDialog.kt#MiuixUpdateActions#TextButton@1",
            "app/src/main/java/com/android/purebilibili/feature/settings/update/" +
                "MiuixAppUpdateDialog.kt#MiuixUpdateActions#TextButton@2",
        )

        val MD3_CARD_PREVIEW_EXCEPTIONS = listOf(
            "app/src/main/java/com/android/purebilibili/feature/settings/screen/" +
                "AppearanceSettingsScreen.kt#Md3ThemeColorPreview#ElevatedCard",
        )

        // Exact post-merge widget-import ratchet; baseline theme/token imports are excluded above.
        const val MAX_FEATURE_MATERIAL3_FILES = 126
        const val MAX_FEATURE_MATERIAL3_IMPORTS = 64
        const val MAX_FEATURE_MATERIAL3_WILDCARD_IMPORTS = 87
        const val MAX_FEATURE_THEME_BRANCH_FILES = 8
        const val MAX_FEATURE_THEME_BRANCH_LINES = 34
        const val MAX_FEATURE_DIRECT_PROGRESS_CALLS = 0
        const val MAX_PRIMITIVE_FACADE_MATERIAL3_IMPORTS = 47

        // Reviewed upstream effect sites. Every path imports Miuix blur, shader, or squircle APIs
        // for liquid chrome, playback art, filters, or the live glass preview; additions require
        // review of the actual renderer use at that path, rather than a larger numerical budget.
        val LIQUID_GLASS_EFFECT_EXCEPTION_PATHS = setOf(
            "app/src/main/java/com/android/purebilibili/feature/audio/screen/AudioNowPlayingBar.kt",
            "app/src/main/java/com/android/purebilibili/feature/audio/screen/ListenVideoScreen.kt",
            "app/src/main/java/com/android/purebilibili/feature/audio/screen/Music3DCoverFlow.kt",
            "app/src/main/java/com/android/purebilibili/feature/audio/screen/MusicPlayerContent.kt",
            "app/src/main/java/com/android/purebilibili/feature/bangumi/BangumiHubContent.kt",
            "app/src/main/java/com/android/purebilibili/feature/bangumi/BangumiReviewScreen.kt",
            "app/src/main/java/com/android/purebilibili/feature/bangumi/ui/player/BangumiPlayerContent.kt",
            "app/src/main/java/com/android/purebilibili/feature/category/CategoryScreen.kt",
            "app/src/main/java/com/android/purebilibili/feature/dynamic/components/DynamicAdaptiveSegmentedControl.kt",
            "app/src/main/java/com/android/purebilibili/feature/dynamic/components/DynamicCommentSheet.kt",
            "app/src/main/java/com/android/purebilibili/feature/dynamic/components/DynamicCreateVoteDialog.kt",
            "app/src/main/java/com/android/purebilibili/feature/dynamic/components/DynamicPublishComposer.kt",
            "app/src/main/java/com/android/purebilibili/feature/dynamic/components/DynamicTopBar.kt",
            "app/src/main/java/com/android/purebilibili/feature/dynamic/DynamicDetailScreen.kt",
            "app/src/main/java/com/android/purebilibili/feature/dynamic/DynamicScreen.kt",
            "app/src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt",
            "app/src/main/java/com/android/purebilibili/feature/home/components/BottomBarFloatingSegmentedControl.kt",
            "app/src/main/java/com/android/purebilibili/feature/home/components/BottomBarLiquidSegmentedControl.kt",
            "app/src/main/java/com/android/purebilibili/feature/home/components/BottomBarMatchedLiquidChrome.kt",
            "app/src/main/java/com/android/purebilibili/feature/home/components/cards/VideoCard.kt",
            "app/src/main/java/com/android/purebilibili/feature/home/components/FloatingBottomBar.kt",
            "app/src/main/java/com/android/purebilibili/feature/home/components/FloatingDockChrome.kt",
            "app/src/main/java/com/android/purebilibili/feature/home/components/HomeHeader.kt",
            "app/src/main/java/com/android/purebilibili/feature/home/components/HomeTopTabChrome.kt",
            "app/src/main/java/com/android/purebilibili/feature/home/components/HomeTopTabFloatingDock.kt",
            "app/src/main/java/com/android/purebilibili/feature/home/components/LinkedBottomDock.kt",
            "app/src/main/java/com/android/purebilibili/feature/home/components/liquid/CombinedBackdrop.kt",
            "app/src/main/java/com/android/purebilibili/feature/home/components/liquid/Lens.kt",
            "app/src/main/java/com/android/purebilibili/feature/home/components/liquid/Vibrancy.kt",
            "app/src/main/java/com/android/purebilibili/feature/home/components/LiquidIndicator.kt",
            "app/src/main/java/com/android/purebilibili/feature/home/components/MineSideDrawer.kt",
            "app/src/main/java/com/android/purebilibili/feature/home/components/ProgressiveTopChrome.kt",
            "app/src/main/java/com/android/purebilibili/feature/home/components/TopBar.kt",
            "app/src/main/java/com/android/purebilibili/feature/home/HomeCategoryPage.kt",
            "app/src/main/java/com/android/purebilibili/feature/home/HomeScreen.kt",
            "app/src/main/java/com/android/purebilibili/feature/live/LiveAreaDetailScreen.kt",
            "app/src/main/java/com/android/purebilibili/feature/live/LivePlayerScreen.kt",
            "app/src/main/java/com/android/purebilibili/feature/live/LiveSearchScreen.kt",
            "app/src/main/java/com/android/purebilibili/feature/message/ChatScreen.kt",
            "app/src/main/java/com/android/purebilibili/feature/partition/PartitionScreen.kt",
            "app/src/main/java/com/android/purebilibili/feature/plugin/TodayWatchPlugin.kt",
            "app/src/main/java/com/android/purebilibili/feature/profile/ProfileScreen.kt",
            "app/src/main/java/com/android/purebilibili/feature/search/SearchScreen.kt",
            "app/src/main/java/com/android/purebilibili/feature/search/SearchVideoFilterSheet.kt",
            "app/src/main/java/com/android/purebilibili/feature/search/TopicDetailScreen.kt",
            "app/src/main/java/com/android/purebilibili/feature/settings/LiquidGlassLivePreview.kt",
            "app/src/main/java/com/android/purebilibili/feature/settings/screen/SettingsTabletShell.kt",
            "app/src/main/java/com/android/purebilibili/feature/settings/ui/SettingsPageScaffold.kt",
            "app/src/main/java/com/android/purebilibili/feature/video/screen/TabletCinemaLayout.kt",
            "app/src/main/java/com/android/purebilibili/feature/video/screen/TabletVideoLayout.kt",
            "app/src/main/java/com/android/purebilibili/feature/video/screen/VideoContentSection.kt",
            "app/src/main/java/com/android/purebilibili/feature/video/screen/VideoDetailPhoneContent.kt",
            "app/src/main/java/com/android/purebilibili/feature/video/ui/components/BottomInputBar.kt",
            "app/src/main/java/com/android/purebilibili/feature/video/ui/components/CommentSearchSheet.kt",
            "app/src/main/java/com/android/purebilibili/feature/video/ui/components/CommentSortFilterBar.kt",
            "app/src/main/java/com/android/purebilibili/feature/video/ui/components/VideoCommentSheetHost.kt",
        )

        // Shared Navigation3 and predictive-back implementation only. Feature comment/reply
        // screens no longer import Miuix navigation directly; the progress transition is part of
        // the shared transition inventory and is included explicitly here.
        val MIUIX_NAVIGATION_EXCEPTION_PATHS = setOf(
            "app/src/main/java/com/android/purebilibili/navigation/AppNavigation.kt",
            "app/src/main/java/com/android/purebilibili/navigation3/BiliPaiNavDisplayHost.kt",
            "app/src/main/java/com/android/purebilibili/navigation3/BiliPaiNavEntryProvider.kt",
            "app/src/main/java/com/android/purebilibili/navigation3/BiliPaiNavKey.kt",
            "app/src/main/java/com/android/purebilibili/navigation3/predictiveback/AospNavTransition.kt",
            "app/src/main/java/com/android/purebilibili/navigation3/predictiveback/BiliPaiMiuixNavTransition.kt",
            "app/src/main/java/com/android/purebilibili/navigation3/predictiveback/ClassicNavTransition.kt",
            "app/src/main/java/com/android/purebilibili/navigation3/predictiveback/MiuixPredictiveBackProgressTransition.kt",
            "app/src/main/java/com/android/purebilibili/navigation3/predictiveback/MiuixVideoCardNavTransition.kt",
            "app/src/main/java/com/android/purebilibili/navigation3/predictiveback/NoPredictiveBackTransition.kt",
            "app/src/main/java/com/android/purebilibili/navigation3/predictiveback/ScaleNavTransition.kt",
        )
    }
}
