package com.android.purebilibili.core.ui.popup

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class RetainedDialogRegistryTest {
    @Test
    fun removalRetainsTheEntryUntilItsOwnExitFinishes() {
        val registry = RetainedDialogRegistry()
        val owner = Any()
        registry.show(owner, null) {}
        val entry = registry.entries.single()
        registry.finish(entry)
        assertSame(entry, registry.entries.single())
        registry.dismiss(owner)
        assertTrue(entry.closing)
        assertSame(entry, registry.entries.single())
        registry.finish(entry)
        assertTrue(registry.entries.isEmpty())
    }

    @Test
    fun repeatedRequestsDoNotDuplicateOrReopenAClosingWindow() {
        val registry = RetainedDialogRegistry()
        val owner = Any()
        repeat(20) { registry.show(owner, null) {} }
        assertEquals(1, registry.entries.size)
        registry.dismiss(owner)
        repeat(20) { registry.show(owner, null) {}; registry.dismiss(owner) }
        assertEquals(1, registry.entries.size)
        assertTrue(registry.entries.single().closing)
    }

    @Test
    fun parentCloseIncludesAllDescendantsAndRejectsLateChildren() {
        val registry = RetainedDialogRegistry()
        val parent = Any()
        val child = Any()
        val grandchild = Any()
        val unrelated = Any()
        registry.show(parent, null) {}
        registry.show(child, parent) {}
        registry.show(grandchild, child) {}
        registry.show(unrelated, null) {}
        registry.dismiss(parent)
        assertTrue(registry.entries.take(3).all { it.closing })
        assertFalse(registry.entries.last().closing)
        registry.show(Any(), child) {}
        assertEquals(4, registry.entries.size)
    }

    @Test
    fun childExitDoesNotCloseItsParentOrRemoveANewDialog() {
        val registry = RetainedDialogRegistry()
        val parent = Any()
        val child = Any()
        registry.show(parent, null) {}
        registry.show(child, parent) {}
        val childEntry = registry.entries.last()
        registry.dismiss(child)
        val replacement = Any()
        registry.show(replacement, parent) {}
        registry.finish(childEntry)
        assertEquals(listOf(parent, replacement), registry.entries.map { it.owner })
        assertTrue(registry.entries.none { it.closing })
    }
}
