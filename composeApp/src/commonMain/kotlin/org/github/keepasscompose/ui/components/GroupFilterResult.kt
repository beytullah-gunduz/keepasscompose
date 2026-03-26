package org.github.keepasscompose.ui.components

import org.github.keepasscompose.core.model.KdbxGroup

data class GroupFilterResult(val matchingUuids: Set<String>, val expandedUuids: Set<String>)

/**
 * Filters a group tree by name, returning the UUIDs of matching groups
 * and all ancestor UUIDs that should be auto-expanded to reveal matches.
 */
fun filterGroupTree(root: KdbxGroup, query: String): GroupFilterResult {
    if (query.isBlank()) {
        return GroupFilterResult(emptySet(), setOf(root.uuid))
    }

    val matchingUuids = mutableSetOf<String>()
    val expandedUuids = mutableSetOf<String>()

    fun search(group: KdbxGroup): Boolean {
        val nameMatches = group.name.contains(query, ignoreCase = true)
        if (nameMatches) {
            matchingUuids.add(group.uuid)
        }

        var hasMatchingDescendant = false
        for (child in group.groups) {
            if (search(child)) {
                hasMatchingDescendant = true
            }
        }

        if (nameMatches || hasMatchingDescendant) {
            expandedUuids.add(group.uuid)
            return true
        }
        return false
    }

    search(root)
    return GroupFilterResult(matchingUuids, expandedUuids)
}
