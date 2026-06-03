package com.android.purebilibili.feature.video.note

fun resolveVideoNoteEmptyMessage(isLoggedIn: Boolean, forbidNoteEntrance: Boolean): String {
    return when {
        forbidNoteEntrance -> "该视频暂不支持笔记。"
        !isLoggedIn -> "登录后可以把这条视频的重点留成笔记。"
        else -> "还没有笔记，可以边看边记下时间点。"
    }
}

fun resolveVideoNoteSaveFeedback(fromAiSummary: Boolean): String {
    return if (fromAiSummary) {
        "AI 草稿已保存为视频笔记。"
    } else {
        "笔记已保存。"
    }
}

fun resolveVideoNoteConflictMessage(hasExistingPrivateNote: Boolean): String {
    return if (hasExistingPrivateNote) {
        "这条视频已有私有笔记，AI 草稿会追加到当前笔记末尾，保存前仍可修改。"
    } else {
        "已根据 AI 总结生成草稿，确认后再保存到笔记。"
    }
}
