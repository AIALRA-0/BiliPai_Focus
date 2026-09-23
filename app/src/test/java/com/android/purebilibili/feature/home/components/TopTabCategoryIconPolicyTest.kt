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
import kotlin.test.Test
import kotlin.test.assertEquals

class TopTabCategoryIconPolicyTest {

    @Test
    fun `top category tabs use Material Rounded icons for every supported category`() {
        assertEquals(Icons.Rounded.Home, resolveTopTabMaterialIcon("RECOMMEND"))
        assertEquals(Icons.Rounded.People, resolveTopTabMaterialIcon("FOLLOW"))
        assertEquals(Icons.Rounded.Whatshot, resolveTopTabMaterialIcon("POPULAR"))
        assertEquals(Icons.Rounded.CollectionsBookmark, resolveTopTabMaterialIcon("ANIME"))
        assertEquals(Icons.Rounded.LiveTv, resolveTopTabMaterialIcon("LIVE"))
        assertEquals(Icons.Rounded.SportsEsports, resolveTopTabMaterialIcon("GAME"))
        assertEquals(Icons.Rounded.School, resolveTopTabMaterialIcon("KNOWLEDGE"))
        assertEquals(Icons.Rounded.Memory, resolveTopTabMaterialIcon("TECH"))
    }

    @Test
    fun `partition and unknown categories use the shared grid fallback`() {
        assertEquals(Icons.Rounded.GridView, resolveTopTabMaterialIcon("PARTITION"))
        assertEquals(Icons.Rounded.GridView, resolveTopTabMaterialIcon("未知栏目"))
    }
}
