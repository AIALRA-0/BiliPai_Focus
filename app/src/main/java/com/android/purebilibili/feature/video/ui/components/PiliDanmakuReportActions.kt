package com.android.purebilibili.feature.video.ui.components

import android.content.Context
import android.widget.Toast
import com.android.purebilibili.core.store.DanmakuSettingsScope
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.data.repository.DanmakuRepository
import com.android.purebilibili.feature.video.danmaku.appendDanmakuUserHashBlockRule
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first

/** All request identity and sender values belong to the menu that submitted them. */
internal suspend fun submitPiliDanmakuReport(
    context: Context,
    cid: Long,
    dmid: Long,
    reason: Int,
    explanation: String,
    userHash: String,
    blockUser: Boolean,
    settingsScope: DanmakuSettingsScope,
) {
    if (cid <= 0L || dmid <= 0L) {
        Toast.makeText(context, "举报失败：视频或弹幕标识无效", Toast.LENGTH_SHORT).show()
        return
    }
    val report = DanmakuRepository.reportDanmaku(
        cid = cid,
        dmid = dmid,
        reason = reason,
        content = explanation.trim(),
    )
    currentCoroutineContext().ensureActive()
    val reportMessage = report.fold(
        onSuccess = { "举报成功" },
        onFailure = { "举报失败：${it.message ?: "未知错误"}" },
    )
    val blockMessage = if (blockUser) {
        persistPiliDanmakuSenderBlock(context, userHash, settingsScope)
    } else {
        null
    }
    Toast.makeText(
        context,
        listOfNotNull(reportMessage, blockMessage).joinToString("\n"),
        Toast.LENGTH_LONG,
    ).show()
}

internal suspend fun blockPiliDanmakuSender(
    context: Context,
    userHash: String,
    settingsScope: DanmakuSettingsScope,
) {
    Toast.makeText(
        context,
        persistPiliDanmakuSenderBlock(context, userHash, settingsScope),
        Toast.LENGTH_LONG,
    ).show()
}

private suspend fun persistPiliDanmakuSenderBlock(
    context: Context,
    userHash: String,
    settingsScope: DanmakuSettingsScope,
): String {
    val sender = userHash.trim()
    if (sender.isBlank()) return "屏蔽失败：该弹幕缺少发送者标识"
    val localMessage = try {
        // Read the store at submission time; a menu snapshot must not overwrite newer rules.
        val currentRules = SettingsManager.getDanmakuBlockRulesRaw(context, settingsScope).first()
        val updatedRules = appendDanmakuUserHashBlockRule(currentRules, sender)
        if (currentRules != updatedRules) {
            SettingsManager.setDanmakuBlockRulesRaw(context, updatedRules, settingsScope)
        }
        resolveDanmakuBlockActionFeedbackMessage(
            target = DanmakuBlockActionTarget.USER,
            changed = currentRules != updatedRules,
        )
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        "本地屏蔽失败：${error.message ?: "未知错误"}"
    }
    val cloud = DanmakuRepository.addDanmakuCloudFilterRule(type = 2, filter = sender)
    currentCoroutineContext().ensureActive()
    return cloud.fold(
        onSuccess = { "$localMessage；云端屏蔽成功" },
        onFailure = { "$localMessage；云端屏蔽失败：${it.message ?: "未知错误"}" },
    )
}
