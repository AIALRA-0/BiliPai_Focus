package com.android.purebilibili.feature.home.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.CollectionsBookmark
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.ui.graphics.vector.ImageVector
import com.android.purebilibili.feature.home.HomeCategory

/**
 * 顶部栏目固定使用 Material Rounded 图标。
 *
 * 顶部 Tab 通过紧凑的圆角选中背景表达状态，因此图标本身不随选中状态切换填充版本。
 * 底部导航仍使用独立的 Miuix 图标策略。
 */
internal fun resolveTopTabMaterialIcon(categoryKey: String): ImageVector {
    val normalizedKey = categoryKey.trim()
    val category = HomeCategory.entries.firstOrNull { entry ->
        entry.name.equals(normalizedKey, ignoreCase = true) || entry.label == normalizedKey
    }

    return when (category) {
        HomeCategory.RECOMMEND -> Icons.Rounded.Home
        HomeCategory.FOLLOW -> Icons.Rounded.People
        HomeCategory.POPULAR -> Icons.Rounded.Whatshot
        HomeCategory.ANIME -> Icons.Rounded.CollectionsBookmark
        HomeCategory.LIVE -> Icons.Rounded.LiveTv
        HomeCategory.GAME -> Icons.Rounded.SportsEsports
        HomeCategory.KNOWLEDGE -> Icons.Rounded.School
        HomeCategory.TECH -> Icons.Rounded.Memory
        null -> Icons.Rounded.GridView
    }
}
