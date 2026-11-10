package com.android.purebilibili.feature.article

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/** 阅读偏好全局共用，正文位置按文章保存，最多保留最近 100 篇。 */
internal object ArticleReadingStore {
    private fun preferences(context: Context) =
        context.applicationContext.getSharedPreferences("article_reading", Context.MODE_PRIVATE)

    fun readPreferences(context: Context): ArticleReadingPreferences {
        val prefs = preferences(context)
        return normalizeArticleReadingPreferences(prefs.getInt("font_size", 16), prefs.getInt("line_spacing", 100))
    }

    fun observePreferences(context: Context) = callbackFlow {
        val prefs = preferences(context)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "font_size" || key == "line_spacing") trySend(readPreferences(context))
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(readPreferences(context))
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    fun savePreferences(context: Context, value: ArticleReadingPreferences) {
        val normalized = normalizeArticleReadingPreferences(value.fontSizeSp, value.lineSpacingPercent)
        preferences(context).edit().putInt("font_size", normalized.fontSizeSp)
            .putInt("line_spacing", normalized.lineSpacingPercent).apply()
    }

    fun readCollectionArticle(context: Context, collectionId: Long): Long? =
        preferences(context).getLong("collection:$collectionId", 0L).takeIf { it > 0L }

    fun observeCollectionArticle(context: Context, collectionId: Long) = callbackFlow {
        val prefs = preferences(context)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "collection:$collectionId") trySend(readCollectionArticle(context, collectionId))
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(readCollectionArticle(context, collectionId))
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    @Synchronized
    fun saveCollectionArticle(context: Context, collectionId: Long, articleId: Long) {
        if (collectionId <= 0L || articleId <= 0L) return
        val prefs = preferences(context)
        val recent = prefs.getString("recent_collections", "").orEmpty().split(',')
            .mapNotNull { it.toLongOrNull()?.takeIf { id -> id > 0L && id != collectionId } }
        val editor = prefs.edit()
        recent.drop(99).forEach { editor.remove("collection:$it") }
        editor.putString("recent_collections", (listOf(collectionId) + recent.take(99)).joinToString(","))
            .putLong("collection:$collectionId", articleId).apply()
    }

    fun readPosition(context: Context, articleId: Long): ArticleReadingPosition {
        val parts = preferences(context).getString("position:$articleId", null)?.split(':').orEmpty()
        return ArticleReadingPosition(
            parts.getOrNull(0)?.toIntOrNull()?.coerceAtLeast(-1) ?: -1,
            parts.getOrNull(1)?.toIntOrNull()?.coerceAtLeast(0) ?: 0,
        )
    }

    @Synchronized
    fun savePosition(context: Context, articleId: Long, value: ArticleReadingPosition) {
        if (articleId <= 0L) return
        val prefs = preferences(context)
        val ids = prefs.getString("recent_articles", "").orEmpty().split(',')
            .mapNotNull { it.toLongOrNull()?.takeIf { id -> id > 0L && id != articleId } }
        val editor = prefs.edit()
        ids.drop(99).forEach { editor.remove("position:$it") }
        editor.putString("recent_articles", (listOf(articleId) + ids.take(99)).joinToString(","))
            .putString("position:$articleId", "${value.blockIndex}:${value.offset}").apply()
    }
}
