package com.android.purebilibili.feature.audio.lyrics.halcyon

/** Ported Halcyon `String.needsPhoneticAnnotation`. */
internal fun String.needsPhoneticAnnotation(): Boolean {
    var index = 0
    while (index < length) {
        val codePoint = codePointAt(index)
        index += Character.charCount(codePoint)
        if (!Character.isLetter(codePoint)) continue
        if (Character.UnicodeScript.of(codePoint) != Character.UnicodeScript.LATIN) return true
    }
    return false
}

/** Ported Halcyon `String.isRtlText`. */
internal fun String.isRtlText(): Boolean {
    var rtlCount = 0
    var ltrCount = 0
    var i = 0
    while (i < length) {
        val cp = codePointAt(i)
        when (Character.getDirectionality(cp).toInt()) {
            Character.DIRECTIONALITY_RIGHT_TO_LEFT.toInt(),
            Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC.toInt() -> rtlCount++
            Character.DIRECTIONALITY_LEFT_TO_RIGHT.toInt() -> ltrCount++
        }
        i += Character.charCount(cp)
    }
    return rtlCount > ltrCount
}

/** Ported Halcyon `Char.isCjkChar`. */
internal fun Char.isCjkChar(): Boolean =
    Character.UnicodeBlock.of(this) in setOf(
        Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS,
        Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A,
        Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B,
        Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS,
        Character.UnicodeBlock.HIRAGANA,
        Character.UnicodeBlock.KATAKANA,
        Character.UnicodeBlock.HANGUL_SYLLABLES,
    )

internal fun Char.isCjkIdeograph(): Boolean =
    Character.UnicodeBlock.of(this) in setOf(
        Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS,
        Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A,
        Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B,
        Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS,
    )

internal fun String.hasCjk(): Boolean = any { it.isCjkChar() }

/** Ported Halcyon `Char.isKanjiOrHangul`. */
internal fun Char.isKanjiOrHangul(): Boolean =
    Character.UnicodeBlock.of(this) in setOf(
        Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS,
        Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A,
        Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B,
        Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS,
        Character.UnicodeBlock.HANGUL_SYLLABLES,
        Character.UnicodeBlock.HANGUL_JAMO,
        Character.UnicodeBlock.HANGUL_COMPATIBILITY_JAMO,
    )
