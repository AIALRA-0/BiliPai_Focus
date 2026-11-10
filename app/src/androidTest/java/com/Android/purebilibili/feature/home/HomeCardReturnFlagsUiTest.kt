package com.android.purebilibili.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import com.android.purebilibili.feature.home.components.cards.resolveVideoCardSharedReturnTargetKey
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HomeCardReturnFlagsUiTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun returnAndQuickReturnOnlyInvalidateTheMatchingCard() {
        val route = "home?category=RECOMMEND"
        val source = mutableStateOf(resolveVideoCardSharedReturnTargetKey("BV-first", route))
        val sourceKey = { source.value }
        val returning = mutableStateOf(false)
        val quickReturn = mutableStateOf(false)
        val commits = mutableMapOf<String, Int>()
        val flags = mutableMapOf<String, HomeCardReturnFlags>()
        val onCommit: (String, HomeCardReturnFlags) -> Unit = { id, value ->
            commits[id] = commits.getOrDefault(id, 0) + 1
            flags[id] = value
        }
        rule.setContent {
            ReturnFlagObserver("BV-first", route, sourceKey, returning, quickReturn, onCommit)
            ReturnFlagObserver("BV-second", route, sourceKey, returning, quickReturn, onCommit)
            ReturnFlagObserver("BV-neighbor", route, sourceKey, returning, quickReturn, onCommit)
        }
        rule.waitForIdle()
        val initial = commits.toMap()

        rule.runOnIdle { returning.value = true }
        rule.runOnIdle {
            assertEquals(HomeCardReturnFlags(true, false), flags["BV-first"])
            assertEquals(initial["BV-second"], commits["BV-second"])
            assertEquals(initial["BV-neighbor"], commits["BV-neighbor"])
            quickReturn.value = true
        }
        rule.runOnIdle {
            assertEquals(HomeCardReturnFlags(true, true), flags["BV-first"])
            assertEquals(initial["BV-second"], commits["BV-second"])
            assertEquals(initial["BV-neighbor"], commits["BV-neighbor"])
            source.value = resolveVideoCardSharedReturnTargetKey("BV-second", route)
        }
        rule.runOnIdle {
            assertEquals(HomeCardReturnFlags(), flags["BV-first"])
            assertEquals(HomeCardReturnFlags(true, true), flags["BV-second"])
            assertEquals(initial["BV-neighbor"], commits["BV-neighbor"])
            returning.value = false
        }
        rule.runOnIdle {
            assertEquals(HomeCardReturnFlags(), flags["BV-second"])
            assertEquals(initial["BV-neighbor"], commits["BV-neighbor"])
        }
    }

    @Test
    fun sameVideoInAnotherCategoryDoesNotReceiveReturnFlagsAndMissingSourceClearsThem() {
        val route = "home?category=RECOMMEND"
        val source = mutableStateOf(resolveVideoCardSharedReturnTargetKey("BV-first", route))
        val sourceKey = { source.value }
        val returning = mutableStateOf(true)
        val quickReturn = mutableStateOf(true)
        val flags = mutableMapOf<String, HomeCardReturnFlags>()
        val onCommit: (String, HomeCardReturnFlags) -> Unit = { id, value -> flags[id] = value }
        rule.setContent {
            ReturnFlagObserver("BV-first", "home?category=POPULAR", sourceKey, returning, quickReturn, onCommit)
        }
        rule.runOnIdle {
            assertEquals(HomeCardReturnFlags(), flags["BV-first"])
            source.value = resolveVideoCardSharedReturnTargetKey("BV-first", "home?category=POPULAR")
        }
        rule.runOnIdle {
            assertEquals(HomeCardReturnFlags(true, true), flags["BV-first"])
            source.value = null
        }
        rule.runOnIdle { assertEquals(HomeCardReturnFlags(), flags["BV-first"]) }
    }
}

@Composable
private fun ReturnFlagObserver(
    bvid: String,
    route: String,
    sourceKey: () -> String?,
    returning: State<Boolean>,
    quickReturn: State<Boolean>,
    onCommit: (String, HomeCardReturnFlags) -> Unit,
) {
    val flags by rememberHomeCardReturnFlags(bvid, route, sourceKey, returning, quickReturn)
    SideEffect { onCommit(bvid, flags) }
}
