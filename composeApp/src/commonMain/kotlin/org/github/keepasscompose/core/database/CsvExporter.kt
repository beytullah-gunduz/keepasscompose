package org.github.keepasscompose.core.database

import org.github.keepasscompose.core.model.KdbxEntry
import org.github.keepasscompose.core.model.KdbxGroup

/**
 * Exports KeePass entries to CSV format.
 *
 * Supports configurable field selection, custom delimiters, and
 * optional group path column. Produces RFC 4180 compliant output
 * with proper quoting of fields containing delimiters or newlines.
 */
object CsvExporter {
    /**
     * Which fields to include in the export.
     */
    data class Config(
        val includeTitle: Boolean = true,
        val includeUserName: Boolean = true,
        val includePassword: Boolean = false,
        val includeUrl: Boolean = true,
        val includeNotes: Boolean = true,
        val includeGroup: Boolean = false,
        val delimiter: Char = ',',
    )

    /**
     * Export all entries from the group tree to CSV.
     *
     * @param rootGroup The root group to export recursively.
     * @param config Export configuration.
     * @return The CSV content as a string.
     */
    fun export(rootGroup: KdbxGroup, config: Config = Config()): String {
        val sb = StringBuilder()

        // Header row
        val headers = buildHeaders(config)
        sb.appendLine(headers.joinToString(config.delimiter.toString()) { escapeField(it, config.delimiter) })

        // Data rows
        val entries = collectEntries(rootGroup, emptyList())
        for ((entry, path) in entries) {
            val row = buildRow(entry, path, config)
            sb.appendLine(row.joinToString(config.delimiter.toString()) { escapeField(it, config.delimiter) })
        }

        return sb.toString()
    }

    private fun buildHeaders(config: Config): List<String> = buildList {
        if (config.includeGroup) add("Group")
        if (config.includeTitle) add("Title")
        if (config.includeUserName) add("Username")
        if (config.includePassword) add("Password")
        if (config.includeUrl) add("URL")
        if (config.includeNotes) add("Notes")
    }

    private fun buildRow(entry: KdbxEntry, groupPath: List<String>, config: Config): List<String> = buildList {
        if (config.includeGroup) add(groupPath.joinToString("/"))
        if (config.includeTitle) add(entry.title)
        if (config.includeUserName) add(entry.userName)
        if (config.includePassword) add(entry.password)
        if (config.includeUrl) add(entry.url)
        if (config.includeNotes) add(entry.notes)
    }

    internal fun escapeField(field: String, delimiter: Char): String {
        val needsQuoting =
            field.contains(delimiter) || field.contains('"') ||
                field.contains('\n') || field.contains('\r')
        return if (needsQuoting) {
            "\"${field.replace("\"", "\"\"")}\""
        } else {
            field
        }
    }

    private fun collectEntries(group: KdbxGroup, path: List<String>): List<Pair<KdbxEntry, List<String>>> {
        val currentPath = path + group.name
        val results = mutableListOf<Pair<KdbxEntry, List<String>>>()
        for (entry in group.entries) results.add(entry to currentPath)
        for (subgroup in group.groups) results.addAll(collectEntries(subgroup, currentPath))
        return results
    }
}
