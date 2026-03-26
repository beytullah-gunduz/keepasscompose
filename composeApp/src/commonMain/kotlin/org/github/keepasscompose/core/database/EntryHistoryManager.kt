package org.github.keepasscompose.core.database

import org.github.keepasscompose.core.model.KdbxEntry
import org.github.keepasscompose.core.model.KdbxGroup
import org.github.keepasscompose.core.model.KdbxMeta
import kotlin.time.Instant

/**
 * Manages entry history: capturing previous versions on modification and
 * pruning history to stay within configured limits.
 *
 * KeePass stores previous versions of an entry inside the entry itself as
 * a `history` list. Each history entry is a full snapshot (without its own
 * nested history) taken at the moment before modification.
 */
class EntryHistoryManager(
    private val historyMaxItems: Int = KdbxMeta.DEFAULT_HISTORY_MAX_ITEMS,
    private val historyMaxSize: Long = KdbxMeta.DEFAULT_HISTORY_MAX_SIZE,
) {
    constructor(meta: KdbxMeta) : this(meta.historyMaxItems, meta.historyMaxSize)

    /**
     * Records the previous version of an entry into its history and returns
     * the updated entry with the new history list (already pruned).
     *
     * The [previousVersion] is stored without its own history to avoid
     * recursive nesting. The returned entry contains the merged and pruned
     * history list.
     *
     * @param currentEntry The updated entry (after modification) whose history list will be extended.
     * @param previousVersion The entry state before the modification.
     * @return A copy of [currentEntry] with the previous version appended to history and pruned.
     */
    fun recordModification(currentEntry: KdbxEntry, previousVersion: KdbxEntry): KdbxEntry {
        val snapshot = previousVersion.copy(history = emptyList())
        val updatedHistory = currentEntry.history + snapshot
        return currentEntry.copy(history = pruneHistory(updatedHistory))
    }

    /**
     * Prunes a history list to respect both [historyMaxItems] and [historyMaxSize] limits.
     *
     * Entries are sorted by [KdbxEntry.lastModificationTime] (oldest first) and the oldest
     * entries are removed until both limits are satisfied.
     *
     * A negative limit value means unlimited (no pruning for that dimension).
     */
    fun pruneHistory(history: List<KdbxEntry>): List<KdbxEntry> {
        if (history.isEmpty()) return history

        val sorted = history.sortedBy { it.lastModificationTime ?: Instant.DISTANT_PAST }

        var result = sorted

        // Prune by item count
        if (historyMaxItems >= 0 && result.size > historyMaxItems) {
            result = result.takeLast(historyMaxItems)
        }

        // Prune by total size
        if (historyMaxSize >= 0) {
            while (result.size > 0 && totalSize(result) > historyMaxSize) {
                result = result.drop(1) // drop oldest
            }
        }

        return result
    }

    /**
     * Prunes history for all entries in a group tree. Used during save to enforce limits.
     */
    fun pruneGroupHistory(group: KdbxGroup): KdbxGroup = group.copy(
        entries =
        group.entries.map { entry ->
            if (entry.history.isEmpty()) {
                entry
            } else {
                entry.copy(history = pruneHistory(entry.history))
            }
        },
        groups = group.groups.map { pruneGroupHistory(it) },
    )

    companion object {
        /**
         * Estimates the in-memory size of a single entry (excluding its history).
         * This mirrors KeePass's approach of summing string and binary sizes.
         */
        fun estimateEntrySize(entry: KdbxEntry): Long {
            var size = 128L // base overhead: UUID, icon, timestamps, booleans

            for (field in entry.fields) {
                size += field.key.length.toLong() * 2 // UTF-16 estimate
                size += field.value.length.toLong() * 2
            }

            for (attachment in entry.attachments) {
                size += attachment.name.length.toLong() * 2
                size += attachment.data.size.toLong()
            }

            for (tag in entry.tags) {
                size += tag.length.toLong() * 2
            }

            return size
        }

        /**
         * Computes the total estimated size of all entries in a history list.
         */
        fun totalSize(history: List<KdbxEntry>): Long = history.sumOf { estimateEntrySize(it) }
    }
}
