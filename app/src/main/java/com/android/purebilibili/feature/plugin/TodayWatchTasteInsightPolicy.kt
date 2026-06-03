package com.android.purebilibili.feature.plugin

import com.android.purebilibili.core.store.CreatorSignalSnapshot
import com.android.purebilibili.core.store.TodayWatchDislikedVideoSnapshot
import com.android.purebilibili.core.store.TodayWatchFeedbackSnapshot

data class TodayWatchTasteSignalUiModel(
    val label: String,
    val value: String
)

data class TodayWatchRecentDislikedVideoUiModel(
    val title: String,
    val subtitle: String
)

data class TodayWatchTasteInsightState(
    val modeTitle: String,
    val modeSummary: String,
    val preferredCreators: List<TodayWatchTasteSignalUiModel>,
    val negativeSignals: List<TodayWatchTasteSignalUiModel>,
    val recentDislikedVideos: List<TodayWatchRecentDislikedVideoUiModel>
)

fun buildTodayWatchTasteInsightState(
    mode: TodayWatchPluginMode,
    feedbackSnapshot: TodayWatchFeedbackSnapshot,
    creatorSignals: List<CreatorSignalSnapshot>
): TodayWatchTasteInsightState {
    val preferredCreators = creatorSignals
        .sortedByDescending { it.score }
        .take(5)
        .map { signal ->
            TodayWatchTasteSignalUiModel(
                label = signal.name.ifBlank { "UP主${signal.mid}" },
                value = "${signal.watchCount} 次"
            )
        }
    val negativeKeywords = feedbackSnapshot.dislikedKeywords
        .take(8)
        .map { keyword ->
            TodayWatchTasteSignalUiModel(label = keyword, value = "已降权")
        }
    val negativeCreators = feedbackSnapshot.dislikedCreatorMids
        .take(4)
        .map { mid ->
            TodayWatchTasteSignalUiModel(label = "UP主$mid", value = "已降权")
        }

    return TodayWatchTasteInsightState(
        modeTitle = when (mode) {
            TodayWatchPluginMode.RELAX -> "今晚轻松看"
            TodayWatchPluginMode.LEARN -> "深度学习看"
        },
        modeSummary = when (mode) {
            TodayWatchPluginMode.RELAX -> "优先短时长、低刺激、近期更新和轻松主题；降低硬核学习与高刺激内容。"
            TodayWatchPluginMode.LEARN -> "优先教程、科普、技术、复盘和中长时长内容；降低短平快娱乐内容。"
        },
        preferredCreators = preferredCreators,
        negativeSignals = negativeKeywords + negativeCreators,
        recentDislikedVideos = feedbackSnapshot.recentDislikedVideos
            .sortedByDescending { it.dislikedAtMillis }
            .take(6)
            .map { it.toUiModel() }
    )
}

private fun TodayWatchDislikedVideoSnapshot.toUiModel(): TodayWatchRecentDislikedVideoUiModel {
    return TodayWatchRecentDislikedVideoUiModel(
        title = title.ifBlank { bvid },
        subtitle = creatorName.ifBlank {
            if (creatorMid > 0L) "UP主$creatorMid" else "未知 UP"
        }
    )
}
