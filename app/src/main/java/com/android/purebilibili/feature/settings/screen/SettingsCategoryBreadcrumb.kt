package com.android.purebilibili.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.android.purebilibili.core.theme.AppUiStyle
import com.android.purebilibili.core.theme.LocalAppUiStyle
import top.yukonga.miuix.kmp.basic.BreadcrumbBar
import top.yukonga.miuix.kmp.basic.BreadcrumbItem

@Composable
internal fun SettingsCategoryBreadcrumb(
    title: String,
    onSettingsHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (LocalAppUiStyle.current != AppUiStyle.MIUIX) return
    val items = remember(title) {
        listOf(
            BreadcrumbItem(path = "settings", text = "设置"),
            BreadcrumbItem(path = "category", text = title),
        )
    }
    BreadcrumbBar(
        items = items,
        onItemClick = { index -> if (index == 0) onSettingsHome() },
        modifier = modifier,
    )
}
