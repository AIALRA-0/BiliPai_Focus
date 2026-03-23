package com.android.purebilibili.feature.space

import com.android.purebilibili.data.model.response.SpaceDynamicContent
import com.android.purebilibili.data.model.response.SpaceDynamicDraw
import com.android.purebilibili.data.model.response.SpaceDynamicDrawItem
import com.android.purebilibili.data.model.response.SpaceDynamicItem
import com.android.purebilibili.data.model.response.SpaceDynamicMajor
import com.android.purebilibili.data.model.response.SpaceDynamicModules
import com.android.purebilibili.feature.dynamic.components.DynamicCardMediaAction
import com.android.purebilibili.feature.dynamic.components.resolveDynamicCardMediaAction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SpaceDynamicLoadPolicyTest {

    @Test
    fun shouldRequestInitialSpaceDynamicLoad_onlyTriggersBeforeFirstCompletedLoad() {
        assertEquals(
            true,
            shouldRequestInitialSpaceDynamicLoad(
                hasLoadedOnce = false,
                isLoading = false
            )
        )
        assertEquals(
            false,
            shouldRequestInitialSpaceDynamicLoad(
                hasLoadedOnce = false,
                isLoading = true
            )
        )
        assertEquals(
            false,
            shouldRequestInitialSpaceDynamicLoad(
                hasLoadedOnce = true,
                isLoading = false
            )
        )
    }

    @Test
    fun resolveSpaceDynamicPresentationState_treatsUntouchedEmptyStateAsLoading() {
        assertEquals(
            SpaceDynamicPresentationState.LOADING,
            resolveSpaceDynamicPresentationState(
                itemCount = 0,
                isLoading = false,
                hasLoadedOnce = false,
                lastLoadFailed = false
            )
        )
    }

    @Test
    fun resolveSpaceDynamicPresentationState_returnsEmptyAfterSuccessfulEmptyLoad() {
        assertEquals(
            SpaceDynamicPresentationState.EMPTY,
            resolveSpaceDynamicPresentationState(
                itemCount = 0,
                isLoading = false,
                hasLoadedOnce = true,
                lastLoadFailed = false
            )
        )
    }

    @Test
    fun resolveSpaceDynamicPresentationState_returnsErrorAfterFailedEmptyLoad() {
        assertEquals(
            SpaceDynamicPresentationState.ERROR,
            resolveSpaceDynamicPresentationState(
                itemCount = 0,
                isLoading = false,
                hasLoadedOnce = true,
                lastLoadFailed = true
            )
        )
    }

    @Test
    fun resolveSpaceDynamicPresentationState_prefersContentWhenItemsExist() {
        assertEquals(
            SpaceDynamicPresentationState.CONTENT,
            resolveSpaceDynamicPresentationState(
                itemCount = 3,
                isLoading = false,
                hasLoadedOnce = true,
                lastLoadFailed = false
            )
        )
    }

    @Test
    fun resolveSpaceDynamicCardItems_maps_space_draw_to_shared_media_capable_card() {
        val mapped = resolveSpaceDynamicCardItems(
            listOf(
                SpaceDynamicItem(
                    id_str = "123",
                    modules = SpaceDynamicModules(
                        module_dynamic = SpaceDynamicContent(
                            major = SpaceDynamicMajor(
                                draw = SpaceDynamicDraw(
                                    id = 9L,
                                    items = listOf(
                                        SpaceDynamicDrawItem(src = "a", width = 10, height = 10),
                                        SpaceDynamicDrawItem(src = "b", width = 20, height = 20)
                                    )
                                )
                            )
                        )
                    )
                )
            )
        ).first()

        val mediaAction = assertIs<DynamicCardMediaAction.PreviewImages>(
            resolveDynamicCardMediaAction(mapped, clickedIndex = 1)
        )

        assertEquals(listOf("a", "b"), mediaAction.images)
        assertEquals(1, mediaAction.initialIndex)
    }
}
