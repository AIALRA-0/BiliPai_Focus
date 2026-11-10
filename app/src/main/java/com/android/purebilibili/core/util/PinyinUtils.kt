package com.android.purebilibili.core.util

import com.github.promeg.pinyinhelper.Pinyin

object PinyinUtils {

    /**
     * 智能匹配：检查文本是否包含查询字符串 (支持中文原声、全拼、拼音首字母)
     */
    fun matches(text: String, query: String): Boolean {
        if (query.isBlank()) return true

        // 1. 原始文本匹配 (最快)
        if (text.contains(query, ignoreCase = true)) return true

        // 2. 转换拼音 (全拼 + 首字母)
        val (fullPinyin, firstLetters) = toPinyin(text)

        // 3. 检查匹配
        return fullPinyin.contains(query, ignoreCase = true) ||
               firstLetters.contains(query, ignoreCase = true)
    }

    /**
     * 转换字符串为拼音元组 (全拼, 首字母)
     */
    private fun toPinyin(text: String): Pair<String, String> {
        val fullSb = StringBuilder()
        val firstSb = StringBuilder()

        for (c in text) {
            if (Pinyin.isChinese(c)) {
                // 内置常用多音字的上下文消歧；无声调，"重" -> "zhong"
                val pinyin = Pinyin.toPinyin(c).lowercase()
                fullSb.append(pinyin)
                firstSb.append(pinyin[0])
            } else {
                // 非中文直接保留 (如英文、数字)
                fullSb.append(c)
                firstSb.append(c)
            }
        }
        return fullSb.toString() to firstSb.toString()
    }
}
