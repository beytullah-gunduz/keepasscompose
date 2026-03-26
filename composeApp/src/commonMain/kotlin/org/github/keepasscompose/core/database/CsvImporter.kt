package org.github.keepasscompose.core.database

import org.github.keepasscompose.core.model.KdbxEntry
import org.github.keepasscompose.core.model.KdbxEntryField
import org.github.keepasscompose.core.model.KdbxGroup

/**
 * Imports entries from CSV files into KeePass domain models.
 *
 * Supports configurable column mapping, different delimiters, and
 * RFC 4180 quoted field handling.
 */
object CsvImporter {

    /**
     * Column-to-field mapping configuration.
     */
    data class ColumnMapping(
        val titleColumn: Int? = null,
        val userNameColumn: Int? = null,
        val passwordColumn: Int? = null,
        val urlColumn: Int? = null,
        val notesColumn: Int? = null,
        val groupColumn: Int? = null,
    )

    /**
     * Import configuration.
     */
    data class Config(
        val delimiter: Char = ',',
        val hasHeader: Boolean = true,
        val mapping: ColumnMapping? = null,
    )

    /**
     * Result of parsing CSV, before committing to the database.
     */
    data class ImportPreview(
        val headers: List<String>,
        val entries: List<KdbxEntry>,
        val groupTree: KdbxGroup,
        val totalRows: Int,
        val skippedRows: Int,
    )

    /**
     * Parse CSV content and produce an import preview.
     *
     * If no explicit [Config.mapping] is provided, columns are auto-detected
     * from the header row using common field names.
     *
     * @param csvContent The raw CSV text.
     * @param config Import configuration.
     * @return [ImportPreview] with parsed entries organized into a group tree.
     */
    fun parse(csvContent: String, config: Config = Config()): ImportPreview {
        val rows = parseCsvRows(csvContent, config.delimiter)
        if (rows.isEmpty()) {
            return ImportPreview(emptyList(), emptyList(), emptyGroup(), 0, 0)
        }

        val headers: List<String>
        val dataRows: List<List<String>>

        if (config.hasHeader && rows.size > 1) {
            headers = rows[0]
            dataRows = rows.drop(1)
        } else if (config.hasHeader && rows.size == 1) {
            headers = rows[0]
            dataRows = emptyList()
        } else {
            headers = List(rows[0].size) { "Column ${it + 1}" }
            dataRows = rows
        }

        val mapping = config.mapping ?: autoDetectMapping(headers)
        val entries = mutableListOf<KdbxEntry>()
        var skipped = 0

        for (row in dataRows) {
            if (row.all { it.isBlank() }) {
                skipped++
                continue
            }
            val entry = rowToEntry(row, mapping)
            if (entry != null) {
                entries.add(entry)
            } else {
                skipped++
            }
        }

        val groupTree = buildGroupTree(entries, dataRows, mapping)

        return ImportPreview(
            headers = headers,
            entries = entries,
            groupTree = groupTree,
            totalRows = dataRows.size,
            skippedRows = skipped,
        )
    }

    // --- CSV Parsing (RFC 4180) ---

    internal fun parseCsvRows(content: String, delimiter: Char): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val fields = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < content.length) {
            val c = content[i]
            when {
                inQuotes -> {
                    if (c == '"') {
                        if (i + 1 < content.length && content[i + 1] == '"') {
                            field.append('"')
                            i++ // skip escaped quote
                        } else {
                            inQuotes = false
                        }
                    } else {
                        field.append(c)
                    }
                }
                c == '"' -> inQuotes = true
                c == delimiter -> {
                    fields.add(field.toString())
                    field.clear()
                }
                c == '\r' -> {
                    // skip, handle \n
                }
                c == '\n' -> {
                    fields.add(field.toString())
                    field.clear()
                    rows.add(fields.toList())
                    fields.clear()
                }
                else -> field.append(c)
            }
            i++
        }

        // Last field/row
        if (field.isNotEmpty() || fields.isNotEmpty()) {
            fields.add(field.toString())
            rows.add(fields.toList())
        }

        return rows
    }

    // --- Auto-detection ---

    private val TITLE_NAMES = setOf("title", "name", "entry", "login name", "login")
    private val USERNAME_NAMES = setOf("username", "user", "user name", "login_username", "email")
    private val PASSWORD_NAMES = setOf("password", "pass", "login_password", "secret")
    private val URL_NAMES = setOf("url", "uri", "website", "login_uri", "web site")
    private val NOTES_NAMES = setOf("notes", "note", "comments", "extra", "description")
    private val GROUP_NAMES = setOf("group", "folder", "path", "category", "grouping")

    internal fun autoDetectMapping(headers: List<String>): ColumnMapping {
        fun findColumn(names: Set<String>): Int? =
            headers.indexOfFirst { it.trim().lowercase() in names }.takeIf { it >= 0 }

        return ColumnMapping(
            titleColumn = findColumn(TITLE_NAMES),
            userNameColumn = findColumn(USERNAME_NAMES),
            passwordColumn = findColumn(PASSWORD_NAMES),
            urlColumn = findColumn(URL_NAMES),
            notesColumn = findColumn(NOTES_NAMES),
            groupColumn = findColumn(GROUP_NAMES),
        )
    }

    // --- Entry conversion ---

    private fun rowToEntry(row: List<String>, mapping: ColumnMapping): KdbxEntry? {
        fun col(index: Int?): String = if (index != null && index < row.size) row[index] else ""

        val title = col(mapping.titleColumn)
        val userName = col(mapping.userNameColumn)
        val password = col(mapping.passwordColumn)
        val url = col(mapping.urlColumn)
        val notes = col(mapping.notesColumn)

        // Skip rows with no meaningful data
        if (title.isBlank() && userName.isBlank() && password.isBlank() && url.isBlank()) {
            return null
        }

        return KdbxEntry(
            uuid = generateUuid(),
            fields = listOf(
                KdbxEntryField(KdbxEntry.FIELD_TITLE, title),
                KdbxEntryField(KdbxEntry.FIELD_USER_NAME, userName),
                KdbxEntryField(KdbxEntry.FIELD_PASSWORD, password, isProtected = true),
                KdbxEntryField(KdbxEntry.FIELD_URL, url),
                KdbxEntryField(KdbxEntry.FIELD_NOTES, notes),
            ),
        )
    }

    // --- Group tree ---

    private fun buildGroupTree(
        entries: List<KdbxEntry>,
        dataRows: List<List<String>>,
        mapping: ColumnMapping,
    ): KdbxGroup {
        if (mapping.groupColumn == null) {
            return KdbxGroup(uuid = generateUuid(), name = "Imported", entries = entries)
        }

        val groupMap = mutableMapOf<String, MutableList<KdbxEntry>>()
        for ((index, entry) in entries.withIndex()) {
            val row = dataRows.getOrNull(index)
            val groupPath = if (row != null && mapping.groupColumn < row.size) {
                row[mapping.groupColumn].ifBlank { "Imported" }
            } else "Imported"
            groupMap.getOrPut(groupPath) { mutableListOf() }.add(entry)
        }

        val root = KdbxGroup(uuid = generateUuid(), name = "Imported")
        if (groupMap.size == 1 && groupMap.keys.first() == "Imported") {
            return root.copy(entries = entries)
        }

        val subgroups = groupMap.map { (name, groupEntries) ->
            KdbxGroup(uuid = generateUuid(), name = name, entries = groupEntries)
        }
        return root.copy(groups = subgroups)
    }

    private fun emptyGroup() = KdbxGroup(uuid = generateUuid(), name = "Imported")

    private var uuidCounter = 0
    private fun generateUuid(): String = "import-${uuidCounter++}"
}
