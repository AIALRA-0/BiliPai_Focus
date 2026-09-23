package com.android.purebilibili.feature.video.danmaku

enum class DanmakuCloudSyncStatus {
    IDLE,
    PENDING,
    SYNCING,
    SUCCESS,
    FAILURE
}

data class DanmakuCloudSyncUiState(
    val status: DanmakuCloudSyncStatus = DanmakuCloudSyncStatus.IDLE,
    val message: String? = null,
    val lastSuccessAtMillis: Long? = null
)

internal fun resolveDanmakuCloudSyncStateAfterQueued(
    previous: DanmakuCloudSyncUiState
): DanmakuCloudSyncUiState {
    return previous.copy(
        status = DanmakuCloudSyncStatus.PENDING,
        message = null
    )
}

internal fun resolveDanmakuCloudSyncStateAfterStarted(
    previous: DanmakuCloudSyncUiState
): DanmakuCloudSyncUiState {
    return previous.copy(
        status = DanmakuCloudSyncStatus.SYNCING,
        message = null
    )
}

internal fun resolveDanmakuCloudSyncStateAfterResult(
    previous: DanmakuCloudSyncUiState,
    result: Result<Unit>,
    completedAtMillis: Long
): DanmakuCloudSyncUiState {
    return if (result.isSuccess) {
        previous.copy(
            status = DanmakuCloudSyncStatus.SUCCESS,
            message = "已同步",
            lastSuccessAtMillis = completedAtMillis
        )
    } else {
        previous.copy(
            status = DanmakuCloudSyncStatus.FAILURE,
            message = result.exceptionOrNull()?.message ?: "同步失败"
        )
    }
}

internal fun shouldRunDanmakuManualCloudSync(
    manualRequestVersion: Long?,
    lastHandledManualRequestVersion: Long?
): Boolean {
    manualRequestVersion ?: return false
    val lastHandled = lastHandledManualRequestVersion ?: Long.MIN_VALUE
    return manualRequestVersion > lastHandled
}

internal fun shouldSyncDanmakuSettingsToCloud(
    isLoggedIn: Boolean,
    cloudSyncEnabled: Boolean
): Boolean = isLoggedIn && cloudSyncEnabled

internal fun resolveDanmakuCloudSyncToggleSubtitle(enabled: Boolean): String {
    return if (enabled) {
        "开启后，透明度、速度、显示区域等设置会同步到账号，并影响网页版等其它端"
    } else {
        "关闭后仅保存在本机，不会覆盖网页版或其它设备的弹幕显示"
    }
}
