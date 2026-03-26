package org.github.keepasscompose.core.model

import org.github.keepasscompose.TestFixtures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KdbxGroupTest {

    @Test
    fun groupHoldsEntriesAndSubGroups() {
        val tree = TestFixtures.createGroupTree()
        assertEquals("Root", tree.name)
        assertEquals(1, tree.entries.size)
        assertEquals(2, tree.groups.size)
    }

    @Test
    fun nestedGroupStructure() {
        val tree = TestFixtures.createGroupTree()
        val banking = tree.groups.first { it.name == "Banking" }
        assertEquals(1, banking.entries.size)
        assertEquals(1, banking.groups.size)
        assertEquals("Credit Cards", banking.groups[0].name)
    }

    @Test
    fun copyPreservesHierarchy() {
        val tree = TestFixtures.createGroupTree()
        val renamed = tree.copy(name = "New Root")
        assertEquals("New Root", renamed.name)
        assertEquals(2, renamed.groups.size)
        assertEquals("General", renamed.groups[0].name)
    }

    @Test
    fun emptyGroupHasNoEntriesOrSubGroups() {
        val group = TestFixtures.createGroup()
        assertTrue(group.entries.isEmpty())
        assertTrue(group.groups.isEmpty())
    }

    @Test
    fun groupWithCustomIcon() {
        val group = KdbxGroup(
            uuid = "icon-group",
            name = "Custom",
            icon = KdbxIcon(standardIndex = 42),
        )
        assertEquals(42, group.icon.standardIndex)
    }
}
