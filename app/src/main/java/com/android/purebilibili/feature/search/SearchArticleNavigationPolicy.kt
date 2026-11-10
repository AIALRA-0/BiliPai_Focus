package com.android.purebilibili.feature.search

import com.android.purebilibili.core.network.NetworkModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

private val opusRedirectPattern = Regex("""(?:^|/)opus/(\d+)(?:[/?#]|$)""")

internal sealed interface ArticleNavigationTarget {
    data class NativeArticle(val articleId: Long) : ArticleNavigationTarget
    data class NativeDynamic(val dynamicId: String) : ArticleNavigationTarget
}

internal fun buildArticleWebUrl(articleId: Long): String? {
    if (articleId <= 0L) return null
    return "https://www.bilibili.com/read/cv$articleId"
}

internal fun resolveArticleWebUrlFromRedirect(
    articleId: Long,
    redirectUrl: String?
): String? {
    val fallbackUrl = buildArticleWebUrl(articleId) ?: return null
    val opusId = redirectUrl
        ?.trim()
        ?.let { opusRedirectPattern.find(it)?.groupValues?.getOrNull(1) }
        ?.takeIf { it.isNotBlank() }
    return if (opusId != null) {
        "https://www.bilibili.com/opus/$opusId"
    } else {
        fallbackUrl
    }
}

internal fun resolveArticleNavigationTargetFromRedirect(
    articleId: Long,
    redirectUrl: String?
): ArticleNavigationTarget? {
    buildArticleWebUrl(articleId) ?: return null
    // /cv/ 链接无论 302 到 /cv/ 还是 /opus/，都是专栏；
    // 统一走专栏渲染器，避免 opus 重定向被误当成图文动态打开。
    return ArticleNavigationTarget.NativeArticle(articleId = articleId)
}

internal suspend fun resolveArticleWebUrl(articleId: Long): String? {
    val fallbackUrl = buildArticleWebUrl(articleId) ?: return null
    return withContext(Dispatchers.IO) {
        runCatching {
            val client = NetworkModule.okHttpClient.newBuilder()
                .followRedirects(false)
                .followSslRedirects(false)
                .build()
            val request = Request.Builder()
                .url(fallbackUrl)
                .method("HEAD", null)
                .build()
            client.newCall(request).execute().use { response ->
                resolveArticleWebUrlFromRedirect(
                    articleId = articleId,
                    redirectUrl = response.header("Location")
                )
            }
        }.onFailure { if (it is kotlinx.coroutines.CancellationException) throw it }.getOrDefault(fallbackUrl)
    }
}

/** 原生入口只依赖已知内容 ID，不等待网页重定向。 */
internal fun resolveArticleNavigationTarget(articleId: Long): ArticleNavigationTarget? {
    if (articleId <= 0L) return null
    return if (articleId >= ARTICLE_OPUS_ID_MIN_VALUE) {
        ArticleNavigationTarget.NativeDynamic(articleId.toString())
    } else {
        ArticleNavigationTarget.NativeArticle(articleId)
    }
}

internal const val ARTICLE_OPUS_ID_MIN_VALUE = 1_000_000_000_000_000L
