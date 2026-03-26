package org.github.keepasscompose.core.database

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.github.keepasscompose.core.model.KdbxEntry
import org.github.keepasscompose.core.model.KdbxEntryField
import org.github.keepasscompose.core.model.KdbxGroup

/**
 * Imports entries from 1Password export files.
 *
 * Supports two formats:
 * - **CSV** (modern): Exported from 1Password 8+ via File > Export
 * - **1PIF** (legacy): JSON-per-line format from older 1Password versions
 *
 * 1Password CSV columns:
 * ```
 * title,website,username,password,notes,type,tags
 * ```
 */
object OnePasswordImporter {

    data class ImportResult(
        val entries: List<KdbxEntry>,
        val groupTree: KdbxGroup,
        val totalItems: Int,
        val logins: Int,
        val secureNotes: Int,
        val other: Int,
    )

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Import from 1Password CSV export.
     */
    fun importCsv(csvContent: String): ImportResult {
        val rows = CsvImporter.parseCsvRows(csvContent, ',')
        if (rows.size < 2) return emptyResult()

        val headers = rows[0].map { it.lowercase().trim() }
        val dataRows = rows.drop(1)

        val titleIdx = headers.indexOfFirst { it in setOf("title", "name") }
        val urlIdx = headers.indexOfFirst { it in setOf("website", "url", "urls") }
        val userIdx = headers.indexOfFirst { it in setOf("username", "user") }
        val passIdx = headers.indexOfFirst { it in setOf("password") }
        val notesIdx = headers.indexOfFirst { it in setOf("notes", "notesplain") }
        val typeIdx = headers.indexOfFirst { it in setOf("type") }
        val tagsIdx = headers.indexOfFirst { it in setOf("tags") }

        val entries = mutableListOf<KdbxEntry>()
        val tagGroups = mutableMapOf<String, MutableList<KdbxEntry>>()
        var logins = 0
        var secureNotes = 0
        var other = 0

        for (row in dataRows) {
            fun col(idx: Int): String = if (idx >= 0 && idx < row.size) row[idx] else ""

            val title = col(titleIdx)
            val url = col(urlIdx)
            val userName = col(userIdx)
            val password = col(passIdx)
            val notes = col(notesIdx)
            val type = col(typeIdx).lowercase()
            val tags = col(tagsIdx)

            if (title.isBlank() && userName.isBlank() && password.isBlank()) continue

            when {
                type.contains("secure note") || type.contains("note") -> secureNotes++
                type.contains("login") || type.isEmpty() -> logins++
                else -> other++
            }

            val fields = mutableListOf(
                KdbxEntryField(KdbxEntry.FIELD_TITLE, title),
                KdbxEntryField(KdbxEntry.FIELD_USER_NAME, userName),
                KdbxEntryField(KdbxEntry.FIELD_PASSWORD, password, isProtected = true),
                KdbxEntryField(KdbxEntry.FIELD_URL, url),
                KdbxEntryField(KdbxEntry.FIELD_NOTES, notes),
            )

            val entryTags = if (tags.isNotBlank()) {
                tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            } else {
                emptyList()
            }

            val entry = KdbxEntry(uuid = "1p-${entries.size}", fields = fields, tags = entryTags)
            entries.add(entry)

            val primaryTag = entryTags.firstOrNull() ?: "Imported"
            tagGroups.getOrPut(primaryTag) { mutableListOf() }.add(entry)
        }

        val groupTree = buildGroupTree(tagGroups, entries)
        return ImportResult(entries, groupTree, dataRows.size, logins, secureNotes, other)
    }

    /**
     * Import from 1Password 1PIF (legacy) format.
     *
     * 1PIF is a text file with alternating JSON objects and separator lines (`***...***`).
     */
    fun importOnePif(content: String): ImportResult {
        val lines = content.lines().filter { it.isNotBlank() && !it.startsWith("***") }
        val entries = mutableListOf<KdbxEntry>()
        var logins = 0
        var secureNotes = 0
        var other = 0

        for (line in lines) {
            val item = try {
                json.decodeFromString<OnePifItem>(line)
            } catch (_: Exception) {
                continue
            }

            val typeName = item.typeName.orEmpty().lowercase()
            when {
                typeName.contains("securenote") -> secureNotes++
                typeName.contains("login") || typeName.contains("webform") -> logins++
                else -> other++
            }

            val title = item.title.orEmpty()
            val url = item.location.orEmpty()
            val notes = item.secureContents?.notesPlain.orEmpty()
            val userName = item.secureContents?.fields?.firstOrNull {
                it.designation == "username" || it.name == "username"
            }?.value.orEmpty()
            val password = item.secureContents?.fields?.firstOrNull {
                it.designation == "password" || it.name == "password"
            }?.value.orEmpty()

            if (title.isBlank() && userName.isBlank() && password.isBlank()) continue

            val fields = mutableListOf(
                KdbxEntryField(KdbxEntry.FIELD_TITLE, title),
                KdbxEntryField(KdbxEntry.FIELD_USER_NAME, userName),
                KdbxEntryField(KdbxEntry.FIELD_PASSWORD, password, isProtected = true),
                KdbxEntryField(KdbxEntry.FIELD_URL, url),
                KdbxEntryField(KdbxEntry.FIELD_NOTES, notes),
            )

            // Add extra fields from secureContents.fields
            item.secureContents?.fields?.filter {
                it.designation != "username" && it.designation != "password" &&
                    it.name != "username" && it.name != "password" &&
                    !it.value.isNullOrBlank()
            }?.forEach { field ->
                fields.add(KdbxEntryField(field.name ?: field.designation.orEmpty(), field.value.orEmpty()))
            }

            entries.add(KdbxEntry(uuid = "1p-${entries.size}", fields = fields))
        }

        val groupTree = KdbxGroup(uuid = "1p-root", name = "1Password Import", entries = entries)
        return ImportResult(entries, groupTree, lines.size, logins, secureNotes, other)
    }

    private fun buildGroupTree(tagGroups: Map<String, List<KdbxEntry>>, allEntries: List<KdbxEntry>): KdbxGroup {
        val root = KdbxGroup(uuid = "1p-root", name = "1Password Import")
        if (tagGroups.isEmpty()) return root.copy(entries = allEntries)
        if (tagGroups.size == 1 && tagGroups.keys.first() == "Imported") {
            return root.copy(entries = allEntries)
        }

        val subgroups = tagGroups.map { (name, groupEntries) ->
            KdbxGroup(uuid = "1p-g-$name", name = name, entries = groupEntries)
        }
        return root.copy(groups = subgroups)
    }

    private fun emptyResult() = ImportResult(
        emptyList(),
        KdbxGroup(uuid = "1p-root", name = "1Password Import"),
        0,
        0,
        0,
        0,
    )

    // --- 1PIF JSON model ---

    @Serializable
    internal data class OnePifItem(
        val title: String? = null,
        val typeName: String? = null,
        val location: String? = null,
        val secureContents: OnePifSecureContents? = null,
    )

    @Serializable
    internal data class OnePifSecureContents(val notesPlain: String? = null, val fields: List<OnePifField>? = null)

    @Serializable
    internal data class OnePifField(val designation: String? = null, val name: String? = null, val value: String? = null)
}
