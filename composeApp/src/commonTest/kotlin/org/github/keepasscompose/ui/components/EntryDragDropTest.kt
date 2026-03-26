package org.github.keepasscompose.ui.components

import org.github.keepasscompose.TestFixtures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class EntryDragDropTest {

    @Test
    fun moveEntryToAnotherGroup() {
        val tree = TestFixtures.createGroupTree()
        val result = applyEntryMove(tree, EntryMoveRequest(listOf("e2"), "banking"))
        assertNotNull(result)
        val general = result.groups.first { it.name == "General" }
        assertEquals(1, general.entries.size) // e3 remains
        val banking = result.groups.first { it.name == "Banking" }
        assertEquals(2, banking.entries.size) // e4 + e2
    }

    @Test
    fun moveMultipleEntries() {
        val tree = TestFixtures.createGroupTree()
        val result = applyEntryMove(tree, EntryMoveRequest(listOf("e2", "e3"), "root"))
        assertNotNull(result)
        assertEquals(3, result.entries.size) // e1 + e2 + e3
        val general = result.groups.first { it.name == "General" }
        assertEquals(0, general.entries.size)
    }

    @Test
    fun moveToNonExistentGroupReturnsNull() {
        val tree = TestFixtures.createGroupTree()
        val result = applyEntryMove(tree, EntryMoveRequest(listOf("e1"), "nonexistent"))
        assertNull(result)
    }

    @Test
    fun moveNonExistentEntryReturnsNull() {
        val tree = TestFixtures.createGroupTree()
        val result = applyEntryMove(tree, EntryMoveRequest(listOf("nonexistent"), "general"))
        assertNull(result)
    }
}
