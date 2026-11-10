package com.android.purebilibili.danmaku.parser.bas

internal enum class BasTokenKind { ID, NUMBER, HEX, TIME, STRING, SYMBOL, END }
internal data class BasToken(val kind: BasTokenKind, val text: String, val line: Int, val column: Int)

/** Deliberately not a JavaScript lexer: only the BAS lexical contract is accepted. */
internal class BasLexer(private val source: String, private val retainStrings: Boolean = true) {
    private var offset = 0
    private var line = 1
    private var column = 1

    fun next(): BasToken {
        while (offset < source.length) {
            val c = source[offset]
            if (c in " \t\u000c\r\n") { advance(); continue }
            if (source.startsWith("//", offset)) {
                while (offset < source.length && source[offset] != '\n' && source[offset] != '\r') advance()
                continue
            }
            break
        }
        val row = line
        val col = column
        if (offset == source.length) return BasToken(BasTokenKind.END, "", row, col)
        val c = source[offset]
        if (c == '"') return string(row, col)
        if (c.isBasIdStart()) {
            val start = offset
            advance()
            while (offset < source.length && (source[offset].isBasIdStart() || source[offset] in '0'..'9')) advance()
            return BasToken(BasTokenKind.ID, source.substring(start, offset), row, col)
        }
        if (source.startsWith("0x", offset) || source.startsWith("0X", offset)) {
            val start = offset
            advance(); advance()
            val digits = offset
            while (offset < source.length && source[offset].basHexDigit() != null) advance()
            if (digits == offset) fail("Expected hexadecimal digits", row, col)
            return BasToken(BasTokenKind.HEX, source.substring(start, offset), row, col)
        }
        val numberEnd = if (c in '0'..'9' || c == '.') decimalEnd(offset) else offset
        if (numberEnd > offset) {
            val start = offset
            var end = numberEnd
            var lastRank = -1
            var time = false
            while (true) {
                val unit = when {
                    source.startsWith("ms", end) -> "ms"
                    end < source.length && source[end] in "hms" -> source[end].toString()
                    else -> null
                } ?: break
                val rank = when (unit) { "h" -> 0; "m" -> 1; "s" -> 2; else -> 3 }
                if (rank <= lastRank) fail("Time components must be ordered h, m, s, ms", row, col)
                lastRank = rank
                time = true
                end += unit.length
                val nextEnd = decimalEnd(end)
                if (nextEnd == end) break
                if (nextEnd >= source.length || source[nextEnd] !in "hms") break
                end = nextEnd
            }
            while (offset < end) advance()
            return BasToken(if (time) BasTokenKind.TIME else BasTokenKind.NUMBER, source.substring(start, end), row, col)
        }
        if (c in "=()[]{},;%+-.") {
            advance()
            return BasToken(BasTokenKind.SYMBOL, c.toString(), row, col)
        }
        fail("Unexpected character '$c'", row, col)
    }

    /** Android ICU matchers copy their entire input to native memory; scan only this token. */
    private fun decimalEnd(start: Int): Int {
        var end = start
        while (end < source.length && source[end] in '0'..'9') end++
        val hasInteger = end > start
        if (end + 1 < source.length && source[end] == '.' && source[end + 1] in '0'..'9') {
            end += 2
            while (end < source.length && source[end] in '0'..'9') end++
        } else if (!hasInteger) {
            return start
        }
        if (end < source.length && source[end] in "eE") {
            var exponent = end + 1
            if (exponent < source.length && source[exponent] in "+-") exponent++
            val digits = exponent
            while (exponent < source.length && source[exponent] in '0'..'9') exponent++
            if (exponent > digits) end = exponent
        }
        return end
    }

    private fun string(row: Int, col: Int): BasToken {
        advance()
        val start = offset
        var result: StringBuilder? = null
        while (offset < source.length) {
            val c = source[offset]
            advance()
            when (c) {
                '"' -> return BasToken(BasTokenKind.STRING,
                    if (!retainStrings) "" else result?.toString() ?: source.substring(start, offset - 1), row, col)
                '\r', '\n' -> fail("Unescaped newline in string", row, col)
                '\\' -> {
                    if (retainStrings && result == null) result = StringBuilder().append(source, start, offset - 1)
                    if (offset == source.length) fail("Unterminated escape", row, col)
                    val escape = source[offset]
                    advance()
                    val decoded = when (escape) {
                        'n' -> '\n'; 'r' -> '\r'; 't' -> '\t'
                        '\\' -> '\\'; '\'' -> '\''; '"' -> '"'
                        'x' -> hexEscape(2, row, col)
                        'u' -> hexEscape(4, row, col)
                        else -> fail("Invalid escape \\$escape", row, col)
                    }
                    result?.append(decoded)
                }
                else -> result?.append(c)
            }
        }
        fail("Unterminated string", row, col)
    }

    private fun hexEscape(count: Int, row: Int, col: Int): Char {
        var value = 0
        repeat(count) {
            if (offset == source.length) fail("Incomplete hexadecimal escape", row, col)
            val digit = source[offset].basHexDigit() ?: fail("Invalid hexadecimal escape", line, column)
            value = value * 16 + digit
            advance()
        }
        return value.toChar()
    }

    private fun advance() {
        val c = source[offset++]
        if (c == '\n' || c == '\r') {
            if (c != '\n' || offset < 2 || source[offset - 2] != '\r') line++
            column = 1
        } else column++
    }

    private fun Char.isBasIdStart() = this in 'a'..'z' || this in 'A'..'Z' || this in "_$@"
    private fun Char.basHexDigit(): Int? = when (this) {
        in '0'..'9' -> this - '0'
        in 'a'..'f' -> this - 'a' + 10
        in 'A'..'F' -> this - 'A' + 10
        else -> null
    }
    private fun fail(message: String, row: Int, col: Int): Nothing = throw BasParseException(message, row, col)
}
