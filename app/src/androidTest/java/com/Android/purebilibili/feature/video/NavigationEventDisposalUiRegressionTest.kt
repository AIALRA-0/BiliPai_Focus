package com.android.purebilibili.feature.video

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.NavigationEventHandler
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventDispatcherOwner
import androidx.navigationevent.compose.rememberNavigationEventState
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Exercises the packaged dependency, including its Compose owner's deferred cleanup. */
@RunWith(AndroidJUnit4::class)
class NavigationEventDisposalUiRegressionTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun nestedOwnerRemovalAndReentryDoNotReuseOrDisposeLiveDispatchers() {
        val visible = mutableStateOf(true)
        lateinit var root: NavigationEventDispatcher
        lateinit var child: NavigationEventDispatcher
        var completions = 0
        rule.setContent {
            if (visible.value) {
                val rootOwner = rememberNavigationEventDispatcherOwner(parent = null)
                val parentOwner = rememberNavigationEventDispatcherOwner(parent = rootOwner)
                val childOwner = rememberNavigationEventDispatcherOwner(parent = parentOwner)
                root = rootOwner.navigationEventDispatcher
                child = childOwner.navigationEventDispatcher
                CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides childOwner) {
                    NavigationBackHandler(
                        state = rememberNavigationEventState(NavigationEventInfo.None),
                        onBackCompleted = { completions++ },
                    )
                }
            }
        }
        rule.runOnIdle {
            val input = DirectNavigationEventInput()
            root.addInput(input)
            input.backCompleted()
            assertEquals(1, completions)
        }
        val oldRoot = root
        val oldChild = child
        rule.runOnIdle { visible.value = false }
        rule.waitForIdle()
        rule.runOnIdle {
            // Mimic an enabled-state effect / child cleanup arriving after ancestor disposal.
            oldChild.isEnabled = false
            oldChild.isEnabled = true
            oldChild.dispose()
            oldRoot.dispose()
            assertThrows(IllegalStateException::class.java) {
                oldChild.addInput(DirectNavigationEventInput())
            }
            visible.value = true
        }
        rule.waitForIdle()
        rule.runOnIdle {
            assertNotSame(oldRoot, root)
            assertNotSame(oldChild, child)
            val input = DirectNavigationEventInput()
            root.addInput(input)
            input.backCompleted()
            assertEquals(2, completions)
        }
    }

    @Test
    fun lateComposeBackHandlerOnDisposedPageIsInertAndReentryStillHandlesBack() {
        val visible = mutableStateOf(false)
        val owner = mutableStateOf<NavigationEventDispatcherOwner?>(null)
        lateinit var root: NavigationEventDispatcher
        lateinit var removed: NavigationEventDispatcher
        val input = DirectNavigationEventInput()
        var completions = 0
        rule.runOnIdle {
            root = NavigationEventDispatcher()
            root.addInput(input)
            removed = NavigationEventDispatcher(root)
            owner.value = object : NavigationEventDispatcherOwner {
                override val navigationEventDispatcher = removed
            }
        }
        rule.setContent {
            if (visible.value) {
                CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides checkNotNull(owner.value)) {
                    BackHandler { completions++ }
                }
            }
        }
        try {
            rule.runOnIdle {
                // Parent teardown has already run when the deferred onRemembered arrives.
                removed.dispose()
                visible.value = true
            }
            rule.waitForIdle()
            rule.runOnIdle {
                input.backCompleted()
                assertEquals(0, completions)
                visible.value = false
            }
            rule.waitForIdle()
            rule.runOnIdle {
                owner.value = object : NavigationEventDispatcherOwner {
                    override val navigationEventDispatcher = NavigationEventDispatcher(root)
                }
                visible.value = true
            }
            rule.waitForIdle()
            rule.runOnIdle {
                input.backCompleted()
                assertEquals(1, completions)
            }
        } finally {
            rule.runOnIdle { visible.value = false }
            rule.waitForIdle()
            rule.runOnIdle { root.dispose() }
        }
    }

    @Test
    fun disposingOneBranchKeepsSiblingBackHandlingAndRejectsDeadRegistrations() {
        rule.setContent { }
        rule.runOnIdle {
            val root = NavigationEventDispatcher()
            val removed = NavigationEventDispatcher(root)
            val sibling = NavigationEventDispatcher(root)
            var completions = 0
            val handler = object : NavigationEventHandler<NavigationEventInfo>(
                initialInfo = NavigationEventInfo.None,
                isBackEnabled = true,
                isForwardEnabled = false,
            ) {
                override fun onBackCompleted() { completions++ }
            }
            sibling.addHandler(handler)
            val input = DirectNavigationEventInput()
            root.addInput(input)
            removed.dispose()
            removed.dispose()
            removed.isEnabled = true
            assertThrows(IllegalStateException::class.java) { removed.addHandler(handler) }
            input.backCompleted()
            assertEquals(1, completions)
            root.dispose()
            sibling.dispose()
        }
    }
}
