package com.android.purebilibili.core.plugin.json

import androidx.collection.LruCache

/** Patterns are content keys: editing/deleting a rule cannot reuse another rule's matcher.
 * Invalid patterns are cached too; malformed rules must not throw once per video/danmaku.
 */
internal class CompiledRegexCache(
    private val capacity: Int = 128,
    private val compile: (String) -> Regex = { Regex(it) },
) {
    private data class Entry(val regex: Regex?)
    private val entries = LruCache<String, Entry>(capacity.coerceAtLeast(1))

    @Synchronized
    fun get(pattern: String): Regex? {
        entries.get(pattern)?.let { return it.regex }
        val compiled = try { compile(pattern) } catch (_: IllegalArgumentException) { null }
        entries.put(pattern, Entry(compiled))
        return compiled
    }
}
