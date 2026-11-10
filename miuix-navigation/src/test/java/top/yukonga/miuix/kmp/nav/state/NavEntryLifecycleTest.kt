package top.yukonga.miuix.kmp.nav.state

import androidx.lifecycle.Lifecycle
import org.junit.Assert.assertEquals
import org.junit.Test
import top.yukonga.miuix.kmp.nav.core.NavPresentationState
import top.yukonga.miuix.kmp.nav.transition.NavRole

class NavEntryLifecycleTest {
    @Test
    fun depthCanLeaveAndReenterWithoutDestroyingTheComposedEntry() {
        val entry = NavPresentationState(role = NavRole.Top, isRemoving = false)
        assertEquals(Lifecycle.State.CREATED, navMaxLifecycleFor(entry, -1.01f, false))
        assertEquals(Lifecycle.State.STARTED, navMaxLifecycleFor(entry, -0.75f, false))
        assertEquals(Lifecycle.State.RESUMED, navMaxLifecycleFor(entry, 0f, false))
    }
}
