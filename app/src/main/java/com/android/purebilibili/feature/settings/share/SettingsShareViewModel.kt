package com.android.purebilibili.feature.settings.share

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsShareUiState(
    val isBusy: Boolean = false,
    val statusMessage: String? = null,
    val pendingImportSession: SettingsShareImportSession? = null,
    val pendingShareUri: Uri? = null
)

class SettingsShareViewModel(
    application: Application,
    private val service: SettingsShareServiceContract =
        SettingsShareService(application.applicationContext)
) : AndroidViewModel(application) {

    constructor(application: Application) : this(
        application = application,
        service = SettingsShareService(application.applicationContext)
    )

    private val _uiState = MutableStateFlow(SettingsShareUiState())
    val uiState: StateFlow<SettingsShareUiState> = _uiState.asStateFlow()

    fun clearStatus() {
        _uiState.value = _uiState.value.copy(statusMessage = null)
    }

    fun exportToUri(uri: Uri) {
        runAction(
            loadingMessage = "正在导出设置...",
            fallbackError = "导出失败，请稍后重试"
        ) {
            service.exportToUri(uri)
                .fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(
                            statusMessage = "已导出设置文件",
                            pendingShareUri = null
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            statusMessage = error.message ?: "导出失败，请稍后重试"
                        )
                    }
                )
        }
    }

    fun prepareShare() {
        runAction(
            loadingMessage = "正在生成分享文件...",
            fallbackError = "分享文件生成失败，请稍后重试"
        ) {
            service.createShareUri()
                .fold(
                    onSuccess = { uri ->
                        _uiState.value = _uiState.value.copy(
                            pendingShareUri = uri,
                            statusMessage = "已生成分享文件"
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            statusMessage = error.message ?: "分享文件生成失败，请稍后重试"
                        )
                    }
                )
        }
    }

    fun consumeShareUri() {
        _uiState.value = _uiState.value.copy(pendingShareUri = null)
    }

    fun loadImportPreview(uri: Uri) {
        runAction(
            loadingMessage = "正在读取设置文件...",
            fallbackError = "导入文件读取失败，请检查文件内容"
        ) {
            service.readImportSession(uri)
                .fold(
                    onSuccess = { session ->
                        _uiState.value = _uiState.value.copy(
                            pendingImportSession = session,
                            statusMessage = null
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            pendingImportSession = null,
                            statusMessage = error.message ?: "导入文件读取失败，请检查文件内容"
                        )
                    }
                )
        }
    }

    fun dismissImportPreview() {
        _uiState.value = _uiState.value.copy(pendingImportSession = null)
    }

    fun confirmImport() {
        val session = _uiState.value.pendingImportSession ?: return
        runAction(
            loadingMessage = "正在应用设置...",
            fallbackError = "导入失败，请稍后重试"
        ) {
            service.applyImport(session)
                .fold(
                    onSuccess = { result ->
                        val appliedCount = result.appliedKeys.size
                        val skippedCount = result.skippedKeys.size
                        _uiState.value = _uiState.value.copy(
                            pendingImportSession = null,
                            statusMessage = buildImportStatusMessage(appliedCount, skippedCount)
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            statusMessage = error.message ?: "导入失败，请稍后重试"
                        )
                    }
                )
        }
    }

    internal fun setPendingImportSessionForTest(session: SettingsShareImportSession?) {
        _uiState.value = _uiState.value.copy(pendingImportSession = session)
    }

    private fun runAction(
        loadingMessage: String,
        fallbackError: String,
        action: suspend () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isBusy = true,
                statusMessage = loadingMessage
            )
            try {
                action()
            } catch (error: Throwable) {
                _uiState.value = _uiState.value.copy(
                    statusMessage = error.message ?: fallbackError
                )
            } finally {
                _uiState.value = _uiState.value.copy(isBusy = false)
            }
        }
    }
}

internal class SettingsShareViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsShareViewModel::class.java)) {
            return SettingsShareViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

private fun buildImportStatusMessage(appliedCount: Int, skippedCount: Int): String {
    return if (skippedCount > 0) {
        "已导入 $appliedCount 项设置，已跳过 $skippedCount 项"
    } else {
        "已导入 $appliedCount 项设置"
    }
}
