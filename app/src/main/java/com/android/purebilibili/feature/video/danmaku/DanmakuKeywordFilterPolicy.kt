package com.android.purebilibili.feature.video.danmaku

private const val REGEX_RULE_PREFIX = "regex:"
private const val SHORT_REGEX_RULE_PREFIX = "re:"
private const val USER_HASH_RULE_PREFIX = "uid:"
private const val USER_RULE_PREFIX = "user:"
private const val HASH_RULE_PREFIX = "hash:"
private val DANMAKU_RULE_SPLITTER = Regex("[\\n,，]+")

enum class DanmakuBlockRuleGroup {
    KEYWORD,
    REGEX,
    USER_HASH
}

data class DanmakuBlockRuleSections(
    val keywordRules: List<String> = emptyList(),
    val regexRules: List<String> = emptyList(),
    val userHashRules: List<String> = emptyList()
)

internal sealed interface DanmakuBlockRuleMatcher {
    fun matches(content: String, userHash: String = ""): Boolean
}

internal data class DanmakuKeywordMatcher(
    val keyword: String
) : DanmakuBlockRuleMatcher {
    override fun matches(content: String, userHash: String): Boolean {
        return content.contains(keyword, ignoreCase = true)
    }
}

internal data class DanmakuRegexMatcher(
    val regex: Regex
) : DanmakuBlockRuleMatcher {
    override fun matches(content: String, userHash: String): Boolean {
        return regex.containsMatchIn(content)
    }
}

internal data class DanmakuUserHashMatcher(
    val userHashRule: String
) : DanmakuBlockRuleMatcher {
    override fun matches(content: String, userHash: String): Boolean {
        if (userHash.isBlank()) return false
        return userHash.equals(userHashRule, ignoreCase = true)
    }
}

fun parseDanmakuBlockRules(raw: String): List<String> {
    return raw.split(DANMAKU_RULE_SPLITTER)
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinct()
}

fun matchesDanmakuBlockRule(content: String, rule: String, userHash: String = ""): Boolean {
    val matcher = resolveDanmakuBlockRuleMatcher(rule) ?: return false
    return matcher.matches(content, userHash)
}

fun shouldBlockDanmakuByRules(
    content: String,
    rules: List<String>,
    userHash: String = ""
): Boolean {
    if (content.isBlank() && userHash.isBlank() || rules.isEmpty()) return false
    val matchers = compileDanmakuBlockRules(rules)
    if (matchers.isEmpty()) return false
    return matchers.any { it.matches(content, userHash) }
}

internal fun shouldBlockDanmakuByMatchers(
    content: String,
    matchers: List<DanmakuBlockRuleMatcher>,
    userHash: String = ""
): Boolean {
    if ((content.isBlank() && userHash.isBlank()) || matchers.isEmpty()) return false
    return matchers.any { it.matches(content, userHash) }
}

internal fun compileDanmakuBlockRules(rules: List<String>): List<DanmakuBlockRuleMatcher> {
    return rules.asSequence()
        .mapNotNull(::resolveDanmakuBlockRuleMatcher)
        .toList()
}

fun partitionDanmakuBlockRules(rules: List<String>): DanmakuBlockRuleSections {
    val normalizedRules = rules.map(String::trim).filter(String::isNotEmpty).distinct()
    return DanmakuBlockRuleSections(
        keywordRules = normalizedRules.filter { resolveDanmakuBlockRuleGroup(it) == DanmakuBlockRuleGroup.KEYWORD },
        regexRules = normalizedRules.filter { resolveDanmakuBlockRuleGroup(it) == DanmakuBlockRuleGroup.REGEX },
        userHashRules = normalizedRules.filter { resolveDanmakuBlockRuleGroup(it) == DanmakuBlockRuleGroup.USER_HASH }
    )
}

fun mergeDanmakuBlockRuleSections(
    keywordRules: List<String>,
    regexRules: List<String>,
    userHashRules: List<String>
): List<String> {
    val normalizedKeywords = keywordRules.map(String::trim).filter(String::isNotEmpty)
    val normalizedRegexRules = regexRules.map(String::trim).filter(String::isNotEmpty)
    val normalizedUserHashRules = userHashRules.mapNotNull(::normalizeDanmakuUserHashManagerInput)
    return (normalizedKeywords + normalizedRegexRules + normalizedUserHashRules).distinct()
}

fun appendDanmakuBlockRule(
    rawRules: String,
    rule: String
): String {
    val normalizedRule = normalizeDanmakuBlockRuleForAppend(rule) ?: return parseDanmakuBlockRules(rawRules)
        .joinToString(separator = "\n")
    return (parseDanmakuBlockRules(rawRules) + normalizedRule)
        .distinct()
        .joinToString(separator = "\n")
}

fun appendDanmakuKeywordBlockRule(
    rawRules: String,
    keyword: String
): String = appendDanmakuBlockRule(rawRules = rawRules, rule = keyword)

fun appendDanmakuUserHashBlockRule(
    rawRules: String,
    userHash: String
): String {
    val normalizedUserHash = normalizeDanmakuUserHashManagerInput(userHash) ?: return parseDanmakuBlockRules(rawRules)
        .joinToString(separator = "\n")
    return appendDanmakuBlockRule(rawRules = rawRules, rule = normalizedUserHash)
}

private fun resolveDanmakuBlockRuleMatcher(rule: String): DanmakuBlockRuleMatcher? {
    val normalized = rule.trim()
    if (normalized.isEmpty()) return null

    val normalizedUserHashRule = normalizeDanmakuUserHashRule(normalized)
    if (normalizedUserHashRule != null) {
        return DanmakuUserHashMatcher(
            userHashRule = normalizedUserHashRule.substringAfter(USER_HASH_RULE_PREFIX)
        )
    }

    val regexBody = when {
        normalized.startsWith(REGEX_RULE_PREFIX, ignoreCase = true) -> {
            normalized.substring(REGEX_RULE_PREFIX.length).trim()
        }
        normalized.startsWith(SHORT_REGEX_RULE_PREFIX, ignoreCase = true) -> {
            normalized.substring(SHORT_REGEX_RULE_PREFIX.length).trim()
        }
        normalized.length >= 2 && normalized.startsWith("/") && normalized.endsWith("/") -> {
            normalized.substring(1, normalized.length - 1).trim()
        }
        else -> null
    }

    if (regexBody != null) {
        if (regexBody.isBlank()) return null
        val compiled = runCatching { Regex(regexBody, setOf(RegexOption.IGNORE_CASE)) }.getOrNull()
            ?: return null
        return DanmakuRegexMatcher(compiled)
    }

    return DanmakuKeywordMatcher(normalized)
}

private fun resolveDanmakuBlockRuleGroup(rule: String): DanmakuBlockRuleGroup {
    val normalized = rule.trim()
    return when {
        normalizeDanmakuUserHashRule(normalized) != null -> DanmakuBlockRuleGroup.USER_HASH
        isDanmakuRegexRule(normalized) -> DanmakuBlockRuleGroup.REGEX
        else -> DanmakuBlockRuleGroup.KEYWORD
    }
}

private fun normalizeDanmakuBlockRuleForAppend(rule: String): String? {
    val normalized = rule.trim()
    if (normalized.isEmpty()) return null
    return normalizeDanmakuUserHashRule(normalized) ?: normalized
}

private fun isDanmakuRegexRule(rule: String): Boolean {
    return rule.startsWith(REGEX_RULE_PREFIX, ignoreCase = true) ||
        rule.startsWith(SHORT_REGEX_RULE_PREFIX, ignoreCase = true) ||
        (rule.length >= 2 && rule.startsWith("/") && rule.endsWith("/"))
}

private fun normalizeDanmakuUserHashRule(rule: String): String? {
    val normalized = rule.trim()
    val body = when {
        normalized.startsWith(USER_HASH_RULE_PREFIX, ignoreCase = true) ->
            normalized.substring(USER_HASH_RULE_PREFIX.length)
        normalized.startsWith(USER_RULE_PREFIX, ignoreCase = true) ->
            normalized.substring(USER_RULE_PREFIX.length)
        normalized.startsWith(HASH_RULE_PREFIX, ignoreCase = true) ->
            normalized.substring(HASH_RULE_PREFIX.length)
        else -> normalized.takeIf { it.startsWith("@") }?.substring(1)
    }?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    return "$USER_HASH_RULE_PREFIX$body"
}

private fun normalizeDanmakuUserHashManagerInput(rule: String): String? {
    val normalized = rule.trim()
    if (normalized.isEmpty()) return null
    return normalizeDanmakuUserHashRule(normalized) ?: "$USER_HASH_RULE_PREFIX$normalized"
}
