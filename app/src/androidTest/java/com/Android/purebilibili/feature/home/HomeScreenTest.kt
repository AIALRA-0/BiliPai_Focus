package com.android.purebilibili.feature.home

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * HomeScreen Compose UI 测试
 * 
 * 测试覆盖:
 * - 分类标签显示
 * - 视频卡片渲染
 * - 交互响应
 */
@RunWith(AndroidJUnit4::class)
class HomeScreenTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun categoryTabs_shouldDisplayAllCategories() {
        // Given: 显示 CategoryTabRow
        composeTestRule.setContent {
            com.android.purebilibili.feature.home.components.CategoryTabRow(
                categories = listOf("推荐", "热门", "直播", "追番", "影视"),
                selectedIndex = 0,
                onCategorySelected = {}
            )
        }
        
        // Then: 所有分类应该可见
        composeTestRule.onNodeWithText("推荐").assertIsDisplayed()
        composeTestRule.onNodeWithText("热门").assertIsDisplayed()
        composeTestRule.onNodeWithText("直播").assertIsDisplayed()
    }
    
    @Test
    fun categoryTab_clickShouldTriggerCallback() {
        // Given: 记录点击的索引
        val clickedIndex = mutableIntStateOf(-1)
        
        composeTestRule.setContent {
            MaterialTheme {
                com.android.purebilibili.feature.home.components.CategoryTabItem(
                    category = "热门",
                    index = 1,
                    selectedIndex = 0,
                    currentPosition = 0f,
                    primaryColor = Color.Red,
                    unselectedColor = Color.Gray,
                    labelMode = 2,
                    onClick = { clickedIndex.intValue = 1 }
                )
            }
        }
        
        // When: 点击 "热门"
        composeTestRule
            .onNodeWithContentDescription("热门")
            .performSemanticsAction(SemanticsActions.OnClick)
        
        // Then: 应回调索引 1
        composeTestRule.runOnIdle {
            assertEquals("Expected index 1", 1, clickedIndex.intValue)
        }
    }
    
    @Test
    fun bottomNavBar_shouldDisplayAllItems() {
        // Given: 显示底部导航栏
        composeTestRule.setContent {
            com.android.purebilibili.feature.home.components.FrostedBottomBar(
                currentItem = com.android.purebilibili.feature.home.components.BottomNavItem.HOME,
                onItemClick = {}
            )
        }
        
        // Then: 所有导航项应该可见
        composeTestRule.onNodeWithContentDescription("首页").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("动态").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("历史记录").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("个人中心").assertIsDisplayed()
    }
}
