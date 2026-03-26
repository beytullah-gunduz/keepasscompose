package org.github.keepasscompose.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.github.keepasscompose.core.model.KdbxGroup

class GroupNavigationViewModel : ViewModel() {
    private val _selectedGroup = MutableStateFlow<KdbxGroup?>(null)
    val selectedGroup: StateFlow<KdbxGroup?> = _selectedGroup.asStateFlow()

    private val _breadcrumb = MutableStateFlow<List<KdbxGroup>>(emptyList())
    val breadcrumb: StateFlow<List<KdbxGroup>> = _breadcrumb.asStateFlow()

    private var rootGroup: KdbxGroup? = null

    fun setRootGroup(group: KdbxGroup) {
        rootGroup = group
        navigateTo(group)
    }

    fun navigateTo(group: KdbxGroup) {
        _selectedGroup.value = group
        _breadcrumb.value = buildBreadcrumb(group)
    }

    fun navigateToParent() {
        val crumb = _breadcrumb.value
        if (crumb.size > 1) {
            navigateTo(crumb[crumb.size - 2])
        }
    }

    fun hasParent(): Boolean = _breadcrumb.value.size > 1

    private fun buildBreadcrumb(target: KdbxGroup): List<KdbxGroup> {
        val root = rootGroup ?: return listOf(target)
        val path = mutableListOf<KdbxGroup>()
        if (findPath(root, target.uuid, path)) {
            return path
        }
        return listOf(target)
    }

    private fun findPath(current: KdbxGroup, targetUuid: String, path: MutableList<KdbxGroup>): Boolean {
        path.add(current)
        if (current.uuid == targetUuid) return true
        for (child in current.groups) {
            if (findPath(child, targetUuid, path)) return true
        }
        path.removeAt(path.lastIndex)
        return false
    }
}
