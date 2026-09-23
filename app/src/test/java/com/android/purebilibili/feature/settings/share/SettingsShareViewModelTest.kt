package com.android.purebilibili.feature.settings.share

import android.app.Application
import android.net.Uri
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsShareViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadImportPreview_populatesPendingSessionOnSuccess() {
        runTest {
            val session = SettingsShareImportSession(
                profile = SettingsShareProfile(
                    appVersion = "6.8.2",
                    exportedAtIso = "2026-03-07T08:00:00Z",
                    profileName = "测试配置"
                ),
                preview = SettingsShareImportPreview(
                    profileName = "测试配置",
                    importableSections = listOf(SettingsShareSection.APPEARANCE),
                    skippedKeys = listOf("token")
                ),
                rawJson = "{}"
            )
            val viewModel = SettingsShareViewModel(
                application = createApplication(),
                service = FakeSettingsShareService(readImportSessionResult = Result.success(session))
            )

            viewModel.loadImportPreview(mockk(relaxed = true))
            advanceUntilIdle()

            assertEquals(session, viewModel.uiState.value.pendingImportSession)
            assertNull(viewModel.uiState.value.statusMessage)
        }
    }

    @Test
    fun confirmImport_appliesSessionAndClearsPendingState() {
        runTest {
            val session = SettingsShareImportSession(
                profile = SettingsShareProfile(
                    appVersion = "6.8.2",
                    exportedAtIso = "2026-03-07T08:00:00Z",
                    profileName = "测试配置"
                ),
                preview = SettingsShareImportPreview(
                    profileName = "测试配置",
                    importableSections = listOf(SettingsShareSection.APPEARANCE, SettingsShareSection.PLAYBACK),
                    skippedKeys = listOf("token")
                ),
                rawJson = "{}"
            )
            val viewModel = SettingsShareViewModel(
                application = createApplication(),
                service = FakeSettingsShareService(
                    applyImportResult = Result.success(
                        SettingsShareApplyResult(
                            appliedKeys = listOf("theme_mode_v2", "auto_play"),
                            skippedKeys = listOf("token")
                        )
                    )
                )
            )

            viewModel.setPendingImportSessionForTest(session)
            viewModel.confirmImport()
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.pendingImportSession)
            assertEquals("已导入 2 项设置，已跳过 1 项", viewModel.uiState.value.statusMessage)
        }
    }

    @Test
    fun prepareShare_setsPendingShareUri() {
        runTest {
            val shareUri = mockk<Uri>(relaxed = true)
            val viewModel = SettingsShareViewModel(
                application = createApplication(),
                service = FakeSettingsShareService(createShareUriResult = Result.success(shareUri))
            )

            viewModel.prepareShare()
            advanceUntilIdle()

            assertEquals(shareUri, viewModel.uiState.value.pendingShareUri)
            assertEquals("已生成分享文件（含设备调试信息）", viewModel.uiState.value.statusMessage)
        }
    }

    private fun createApplication(): Application {
        return Application()
    }
}

private class FakeSettingsShareService(
    private val exportToUriResult: Result<SettingsShareExportArtifact> = Result.failure(IllegalStateException("unused")),
    private val createShareUriResult: Result<Uri> = Result.failure(IllegalStateException("unused")),
    private val readImportSessionResult: Result<SettingsShareImportSession> = Result.failure(IllegalStateException("unused")),
    private val applyImportResult: Result<SettingsShareApplyResult> = Result.failure(IllegalStateException("unused"))
) : SettingsShareServiceContract {

    override suspend fun exportToUri(
        uri: Uri,
        profileName: String,
        includeDeviceDebug: Boolean,
    ): Result<SettingsShareExportArtifact> = exportToUriResult

    override suspend fun createShareUri(
        profileName: String,
        includeDeviceDebug: Boolean,
    ): Result<Uri> = createShareUriResult

    override suspend fun readImportSession(uri: Uri): Result<SettingsShareImportSession> = readImportSessionResult

    override suspend fun applyImport(session: SettingsShareImportSession): Result<SettingsShareApplyResult> = applyImportResult
}
