package com.android.purebilibili.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.structuralEqualityPolicy
import com.android.purebilibili.feature.home.components.cards.isVideoCardSharedReturnTarget

internal data class HomeCardReturnFlags(
    val returning: Boolean = false,
    val quickReturn: Boolean = false,
)

/** 整页返回许可不应成为每张卡片的视觉参数，只有匹配来源参与封面和文字交接。 */
@Composable
internal fun rememberHomeCardReturnFlags(
    bvid: String,
    sourceRoute: String,
    sourceKey: () -> String?,
    returning: State<Boolean>,
    quickReturn: State<Boolean>,
): State<HomeCardReturnFlags> = remember(bvid, sourceRoute, sourceKey, returning, quickReturn) {
    derivedStateOf(structuralEqualityPolicy()) {
        if (returning.value && isVideoCardSharedReturnTarget(bvid, sourceRoute, sourceKey())) {
            HomeCardReturnFlags(returning = true, quickReturn = quickReturn.value)
        } else HomeCardReturnFlags()
    }
}
