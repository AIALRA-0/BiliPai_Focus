// 文件路径: feature/video/danmaku/DanmakuCloudRuleSyncPolicy.kt
package com.android.purebilibili.feature.video.danmaku

/**
 * 云端弹幕屏蔽规则同步策略（对齐 PiliPlus /x/dm/filter/user 行为）。
 *
 * 云端规则 type：0=关键词，1=正则，2=UID(crc32 hex)。
 * 本地规则为带前缀的单行字符串（关键词裸文本、`regex:`/`re:`、`uid:`/`user:`/`hash:`）。
 */
object DanmakuCloudRuleSyncPolicy {

    private const val REGEX_RULE_PREFIX = "regex:"
    private const val SHORT_REGEX_RULE_PREFIX = "re:"
    private const val USER_HASH_RULE_PREFIX = "uid:"
    private const val USER_RULE_PREFIX = "user:"
    private const val HASH_RULE_PREFIX = "hash:"

    const val CLOUD_TYPE_KEYWORD = 0
    const val CLOUD_TYPE_REGEX = 1
    const val CLOUD_TYPE_UID = 2

    /** UID 输入 crc32 hex（与 PiliPlus getCrc32(...).toRadixString(16) 一致，无前导零填充） */
    fun crc32Hex(input: String): String {
        val crc = java.util.zip.CRC32()
        crc.update(input.toByteArray(Charsets.US_ASCII))
        return java.lang.Long.toHexString(crc.value)
    }

    /** 云端规则 → 本地单行规则；无法映射时返回 null */
    fun cloudRuleToLocalRule(type: Int, filter: String): String? {
        val body = filter.trim()
        if (body.isEmpty()) return null
        return when (type) {
            CLOUD_TYPE_KEYWORD -> body
            CLOUD_TYPE_REGEX -> if (isDanmakuRegexRule(body) || isDanmakuRegexRuleCandidate(body)) body else "regex:$body"
            CLOUD_TYPE_UID -> if (body.all(Char::isDigit)) "hash:$body" else "uid:$body"
            else -> null
        }
    }

    /** 本地正则规则 → 云端正则文本（剥掉 `regex:`/`re:` 前缀和 `/.../` 定界） */
    fun localRegexRuleToCloudFilter(rule: String): String? {
        val body = stripRulePrefix(rule, setOf(REGEX_RULE_PREFIX, SHORT_REGEX_RULE_PREFIX))
            ?.removeSurrounding("/")?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: return null
        return body
    }

    /**
     * 本地 UID 规则 → 云端 UID 文本。
     * 纯数字输入视为真实 UID，转 crc32 hex；否则假定已是 midHash hex，原样上传。
     */
    fun localUidRuleToCloudFilter(rule: String): String? {
        val body = stripRulePrefix(rule, setOf(USER_HASH_RULE_PREFIX, USER_RULE_PREFIX, HASH_RULE_PREFIX))
            ?: return null
        return if (!rule.trim().startsWith(HASH_RULE_PREFIX, ignoreCase = true) && body.matches(Regex("\\d+"))) crc32Hex(body) else body
    }

    /**
     * 计算保存时需要上传云端的新增规则。
     * [baseline] 为同步完成后的本地规则基线，[current] 为保存时的本地规则。
     */
    fun resolveCloudRuleAdds(
        baseline: DanmakuBlockRuleSections,
        current: DanmakuBlockRuleSections
    ): List<CloudRuleAdd> {
        val adds = mutableListOf<CloudRuleAdd>()
        (current.keywordRules - baseline.keywordRules.toSet()).forEach { rule ->
            val filter = rule.trim().takeIf { it.isNotEmpty() } ?: return@forEach
            adds += CloudRuleAdd(CLOUD_TYPE_KEYWORD, filter)
        }
        (current.regexRules - baseline.regexRules.toSet()).forEach { rule ->
            val filter = localRegexRuleToCloudFilter(rule) ?: return@forEach
            adds += CloudRuleAdd(CLOUD_TYPE_REGEX, filter)
        }
        (current.userHashRules - baseline.userHashRules.toSet()).forEach { rule ->
            val filter = localUidRuleToCloudFilter(rule) ?: return@forEach
            adds += CloudRuleAdd(CLOUD_TYPE_UID, filter)
        }
        return adds
    }

    /** 计算保存时需要从云端删除的规则 id：云端规则对应的本地表示已不在 [current] 中 */
    fun resolveCloudRuleDeletes(
        cloudRules: List<com.android.purebilibili.data.repository.DanmakuCloudFilterRule>,
        current: DanmakuBlockRuleSections
    ): List<Long> {
        val keywordSet = current.keywordRules.toSet()
        val regexSet = current.regexRules.toSet()
        val uidSet = current.userHashRules.toSet()
        return cloudRules.mapNotNull { cloud ->
            val local = cloudRuleToLocalRule(cloud.type, cloud.filter)
            val stillPresent = when (cloud.type) {
                CLOUD_TYPE_KEYWORD -> local != null && local in keywordSet
                CLOUD_TYPE_REGEX -> local != null && local in regexSet
                CLOUD_TYPE_UID -> local != null && local in uidSet
                else -> true
            }
            if (stillPresent) null else cloud.id
        }
    }

    private fun stripRulePrefix(rule: String, prefixes: Set<String>): String? {
        val trimmed = rule.trim()
        val body = prefixes.firstOrNull { trimmed.startsWith(it, ignoreCase = true) }
            ?.let { trimmed.substring(it.length) }
            ?: trimmed
        return body.trim().takeIf { it.isNotEmpty() }
    }

    data class CloudRuleAdd(val type: Int, val filter: String)
}
