package com.android.purebilibili.feature.profile

import androidx.compose.runtime.staticCompositionLocalOf
import com.android.purebilibili.data.model.AccountLedgerKind

/** 个人中心的多个布局共用同一明细入口，由页面宿主提供导航回调。 */
internal val LocalProfileLedgerNavigation = staticCompositionLocalOf<((AccountLedgerKind) -> Unit)?> { null }

internal val LocalProfileCommentsNavigation = staticCompositionLocalOf<(() -> Unit)?> { null }

internal fun ledgerMenuActions(
    open: ((AccountLedgerKind) -> Unit)?,
    openComments: (() -> Unit)? = null,
): List<com.android.purebilibili.core.ui.components.AppWindowAction> = buildList {
    if (open != null) AccountLedgerKind.entries.forEach { kind ->
        add(com.android.purebilibili.core.ui.components.AppWindowAction(label = kind.title, onClick = { open(kind) }))
    }
    if (openComments != null) add(com.android.purebilibili.core.ui.components.AppWindowAction(label = "我的评论", onClick = openComments))
}
