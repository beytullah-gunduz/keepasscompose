package org.github.keepasscompose.core.database

import org.github.keepasscompose.core.model.KdbxEntry
import org.github.keepasscompose.core.model.KdbxEntryField
import org.github.keepasscompose.core.model.KdbxGroup

/**
 * Imports entries from LastPass CSV export files.
 *
 * LastPass CSV format:
 * ```
 * url,username,password,totp,extra,name,grouping,fav
 * ```
 *
 * Secure notes have `http://sn` as the URL. Folders map to the `grouping` column
 * with `/` as the path separator.
 */
object LastPassImporter {
    data class ImportResult(val entries: List<KdbxEntry>, val groupTree: KdbxGroup, val totalRows: Int, val secureNotes: Int)

    fun import(csvContent: String): ImportResult {
        val rows = CsvImporter.parseCsvRows(csvContent, ',')
        if (rows.size < 2) return ImportResult(emptyList(), emptyGroup(), 0, 0)

        val headers = rows[0].map { it.lowercase().trim() }
        val dataRows = rows.drop(1)

        val urlIdx = headers.indexOf("url")
        val userIdx = headers.indexOf("username")
        val passIdx = headers.indexOf("password")
        val totpIdx = headers.indexOf("totp")
        val extraIdx = headers.indexOf("extra")
        val nameIdx = headers.indexOf("name")
        val groupIdx = headers.indexOf("grouping")

        val entries = mutableListOf<KdbxEntry>()
        val groupMap = mutableMapOf<String, MutableList<KdbxEntry>>()
        var secureNotes = 0

        for (row in dataRows) {
            fun col(idx: Int): String = if (idx >= 0 && idx < row.size) row[idx] else ""

            val url = col(urlIdx)
            val isSecureNote = url == "http://sn"
            if (isSecureNote) secureNotes++

            val title = col(nameIdx)
            val userName = col(userIdx)
            val password = col(passIdx)
            val notes = col(extraIdx)
            val totp = col(totpIdx)
            val groupPath = col(groupIdx).ifBlank { "Imported" }

            if (title.isBlank() && userName.isBlank() && password.isBlank()) continue

            val fields =
                mutableListOf(
                    KdbxEntryField(KdbxEntry.FIELD_TITLE, title),
                    KdbxEntryField(KdbxEntry.FIELD_USER_NAME, userName),
                    KdbxEntryField(KdbxEntry.FIELD_PASSWORD, password, isProtected = true),
                    KdbxEntryField(KdbxEntry.FIELD_URL, if (isSecureNote) "" else url),
                    KdbxEntryField(KdbxEntry.FIELD_NOTES, notes),
                )
            if (totp.isNotBlank()) {
                fields.add(KdbxEntryField("otp", totp))
            }

            val entry = KdbxEntry(uuid = "lp-${entries.size}", fields = fields)
            entries.add(entry)
            groupMap.getOrPut(groupPath) { mutableListOf() }.add(entry)
        }

        val groupTree = buildGroupTree(groupMap)
        return ImportResult(entries, groupTree, dataRows.size, secureNotes)
    }

    private fun buildGroupTree(groupMap: Map<String, List<KdbxEntry>>): KdbxGroup {
        val root = KdbxGroup(uuid = "lp-root", name = "LastPass Import")
        if (groupMap.isEmpty()) return root

        // Build nested groups from slash-separated paths
        val rootEntries = mutableListOf<KdbxEntry>()
        val subgroups = mutableMapOf<String, MutableList<KdbxEntry>>()

        for ((path, entries) in groupMap) {
            if (path == "Imported" || path.isBlank()) {
                rootEntries.addAll(entries)
            } else {
                // Use first path segment as top-level group
                subgroups.getOrPut(path) { mutableListOf() }.addAll(entries)
            }
        }

        val groups =
            subgroups.map { (name, groupEntries) ->
                KdbxGroup(uuid = "lp-g-$name", name = name, entries = groupEntries)
            }

        return root.copy(entries = rootEntries, groups = groups)
    }

    private fun emptyGroup() = KdbxGroup(uuid = "lp-root", name = "LastPass Import")
}
