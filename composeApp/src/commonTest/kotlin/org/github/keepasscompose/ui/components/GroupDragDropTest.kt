package org.github.keepasscompose.ui.components

import org.github.keepasscompose.TestFixtures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GroupDragDropTest {

    @Test
    fun validMoveToSibling() {
        val tree = TestFixtures.createGroupTree()
        assertTrue(isValidGroupMove(tree, "general", "banking"))
    }

    @Test
    fun cannotMoveGroupIntoItself() {
        val tree = TestFixtures.createGroupTree()
        assertFalse(isValidGroupMove(tree, "banking", "banking"))
    }

    @Test
    fun cannotMoveGroupIntoOwnChild() {
        val tree = TestFixtures.createGroupTree()
        assertFalse(isValidGroupMove(tree, "banking", "credit-cards"))
    }

    @Test
    fun applyGroupMoveReturnsNewTree() {
        val tree = TestFixtures.createGroupTree()
        val result = applyGroupMove(tree, GroupMoveRequest("credit-cards", "general", 0))
        assertNotNull(result)
        val general = result.groups.first { it.name == "General" }
        assertEquals(1, general.groups.size)
        assertEquals("Credit Cards", general.groups[0].name)
    }

    @Test
    fun invalidMoveReturnsNull() {
        val tree = TestFixtures.createGroupTree()
        val result = applyGroupMove(tree, GroupMoveRequest("banking", "credit-cards", 0))
        assertNull(result)
    }
}
