package com.android.purebilibili.feature.space

import androidx.lifecycle.SavedStateHandle
import com.android.purebilibili.data.model.response.SpaceUserInfo
import com.android.purebilibili.feature.space.SpaceMainTab
import com.android.purebilibili.feature.space.SpaceUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SpaceViewModelMainTabStateTest {

    @Test
    fun `selectMainTab updates selectedMainTab state`() {
        val savedStateHandle = SavedStateHandle()
        val viewModel = SpaceViewModel(savedStateHandle)
        assertEquals(2, viewModel.selectedMainTab.value)

        viewModel.selectMainTab(3)
        assertEquals(3, viewModel.selectedMainTab.value)
        assertEquals(3, savedStateHandle.get<Int>("space_selected_main_tab"))
    }

    @Test
    fun `selectedMainTab restores from saved state`() {
        val savedStateHandle = SavedStateHandle(mapOf("space_selected_main_tab" to 3))
        val viewModel = SpaceViewModel(savedStateHandle)

        assertEquals(3, viewModel.selectedMainTab.value)
    }

    @Test
    fun `success state initializes tab shell with all tabs`() {
        val success = SpaceUiState.Success(userInfo = SpaceUserInfo())

        assertEquals(SpaceMainTab.HOME, success.tabShellState.selectedTab)
        assertEquals(6, success.tabShellState.tabStates.size)
    }

    @Test
    fun `selectMainTab keeps tab shell selected state in sync`() {
        val viewModel = SpaceViewModel(SavedStateHandle())
        val field = SpaceViewModel::class.java.getDeclaredField("_uiState").apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        val flow = field.get(viewModel) as MutableStateFlow<SpaceUiState>
        flow.value = SpaceUiState.Success(
            userInfo = SpaceUserInfo(mid = 1L, name = "UP"),
            headerState = buildHeaderState(
                userInfo = SpaceUserInfo(mid = 1L, name = "UP"),
                relationStat = null,
                upStat = null,
                topVideo = null,
                notice = "",
                createdFavorites = emptyList(),
                collectedFavorites = emptyList()
            ),
            tabShellState = buildInitialTabShellState(selectedTab = SpaceMainTab.CONTRIBUTION)
                .withUpdatedTab(SpaceMainTab.CONTRIBUTION) { it.copy(hasLoaded = true) }
        )

        viewModel.selectMainTab(1)

        val success = viewModel.uiState.value as SpaceUiState.Success
        assertEquals(SpaceMainTab.DYNAMIC, success.tabShellState.selectedTab)
        assertTrue(success.tabShellState.tabStates[SpaceMainTab.CONTRIBUTION]?.hasLoaded == true)
    }
}
