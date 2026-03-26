package org.github.keepasscompose.viewmodel

import org.github.keepasscompose.TestFixtures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GroupNavigationViewModelTest {

    private val vm = GroupNavigationViewModel()

    @Test
    fun setRootGroupSelectsRoot() {
        val tree = TestFixtures.createGroupTree()
        vm.setRootGroup(tree)
        assertEquals("Root", vm.selectedGroup.value?.name)
    }

    @Test
    fun navigateToUpdatesSelectedGroup() {
        val tree = TestFixtures.createGroupTree()
        vm.setRootGroup(tree)
        val general = tree.groups.first { it.name == "General" }
        vm.navigateTo(general)
        assertEquals("General", vm.selectedGroup.value?.name)
    }

    @Test
    fun breadcrumbShowsPathFromRoot() {
        val tree = TestFixtures.createGroupTree()
        vm.setRootGroup(tree)
        val creditCards = tree.groups.first { it.name == "Banking" }.groups[0]
        vm.navigateTo(creditCards)
        val names = vm.breadcrumb.value.map { it.name }
        assertEquals(listOf("Root", "Banking", "Credit Cards"), names)
    }

    @Test
    fun navigateToParentGoesUp() {
        val tree = TestFixtures.createGroupTree()
        vm.setRootGroup(tree)
        val creditCards = tree.groups.first { it.name == "Banking" }.groups[0]
        vm.navigateTo(creditCards)
        assertTrue(vm.hasParent())
        vm.navigateToParent()
        assertEquals("Banking", vm.selectedGroup.value?.name)
    }

    @Test
    fun rootHasNoParent() {
        val tree = TestFixtures.createGroupTree()
        vm.setRootGroup(tree)
        assertFalse(vm.hasParent())
    }
}
