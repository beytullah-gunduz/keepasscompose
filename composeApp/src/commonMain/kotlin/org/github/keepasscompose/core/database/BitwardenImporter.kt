package org.github.keepasscompose.core.database

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.github.keepasscompose.core.model.KdbxEntry
import org.github.keepasscompose.core.model.KdbxEntryField
import org.github.keepasscompose.core.model.KdbxGroup

/**
 * Imports entries from Bitwarden JSON export files.
 *
 * Bitwarden JSON structure:
 * ```json
 * {
 *   "folders": [{"id": "...", "name": "..."}],
 *   "items": [{"type": 1, "name": "...", "login": {"username": "...", ...}, ...}]
 * }
 * ```
 *
 * Also supports Bitwarden CSV exports via [CsvImporter].
 */
object BitwardenImporter {

    data class ImportResult(
        val entries: List<KdbxEntry>,
        val groupTree: KdbxGroup,
        val totalItems: Int,
        val secureNotes: Int,
        val cards: Int,
        val identities: Int,
    )

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun import(jsonContent: String): ImportResult {
        val export = json.decodeFromString<BitwardenExport>(jsonContent)

        val folderMap = export.folders?.associate { it.id to it.name } ?: emptyMap()
        val entries = mutableListOf<KdbxEntry>()
        val groupEntries = mutableMapOf<String, MutableList<KdbxEntry>>()
        var secureNotes = 0
        var cards = 0
        var identities = 0

        for (item in export.items.orEmpty()) {
            when (item.type) {
                2 -> { secureNotes++; /* process below */ }
                3 -> { cards++; continue } // Card items — skip for now
                4 -> { identities++; continue } // Identity items — skip for now
            }

            val fields = mutableListOf(
                KdbxEntryField(KdbxEntry.FIELD_TITLE, item.name.orEmpty()),
                KdbxEntryField(KdbxEntry.FIELD_USER_NAME, item.login?.username.orEmpty()),
                KdbxEntryField(KdbxEntry.FIELD_PASSWORD, item.login?.password.orEmpty(), isProtected = true),
                KdbxEntryField(KdbxEntry.FIELD_URL, item.login?.uris?.firstOrNull()?.uri.orEmpty()),
                KdbxEntryField(KdbxEntry.FIELD_NOTES, item.notes.orEmpty()),
            )

            // TOTP
            item.login?.totp?.let { totp ->
                if (totp.isNotBlank()) fields.add(KdbxEntryField("otp", totp))
            }

            // Custom fields
            item.fields?.forEach { cf ->
                val isHidden = cf.type == 1
                fields.add(KdbxEntryField(cf.name.orEmpty(), cf.value.orEmpty(), isProtected = isHidden))
            }

            val entry = KdbxEntry(uuid = "bw-${entries.size}", fields = fields)
            entries.add(entry)

            val folderName = item.folderId?.let { folderMap[it] } ?: "No Folder"
            groupEntries.getOrPut(folderName) { mutableListOf() }.add(entry)
        }

        val groupTree = buildGroupTree(groupEntries)
        return ImportResult(
            entries, groupTree, export.items?.size ?: 0,
            secureNotes, cards, identities,
        )
    }

    /**
     * Import from Bitwarden CSV export format.
     * Bitwarden CSV: folder,favorite,type,name,notes,fields,reprompt,login_uri,login_username,login_password,login_totp
     */
    fun importCsv(csvContent: String): ImportResult {
        val config = CsvImporter.Config(
            mapping = CsvImporter.ColumnMapping(
                groupColumn = 0,
                titleColumn = 3,
                notesColumn = 4,
                urlColumn = 7,
                userNameColumn = 8,
                passwordColumn = 9,
            ),
        )
        val preview = CsvImporter.parse(csvContent, config)
        return ImportResult(
            entries = preview.entries,
            groupTree = preview.groupTree,
            totalItems = preview.totalRows,
            secureNotes = 0, cards = 0, identities = 0,
        )
    }

    private fun buildGroupTree(groupEntries: Map<String, List<KdbxEntry>>): KdbxGroup {
        val root = KdbxGroup(uuid = "bw-root", name = "Bitwarden Import")
        if (groupEntries.isEmpty()) return root

        val rootEntries = groupEntries["No Folder"] ?: emptyList()
        val subgroups = groupEntries.filterKeys { it != "No Folder" }.map { (name, entries) ->
            KdbxGroup(uuid = "bw-g-$name", name = name, entries = entries)
        }

        return root.copy(entries = rootEntries, groups = subgroups)
    }

    // --- Bitwarden JSON model ---

    @Serializable
    internal data class BitwardenExport(
        val folders: List<BitwardenFolder>? = null,
        val items: List<BitwardenItem>? = null,
    )

    @Serializable
    internal data class BitwardenFolder(
        val id: String,
        val name: String,
    )

    @Serializable
    internal data class BitwardenItem(
        val type: Int = 1,
        val name: String? = null,
        val notes: String? = null,
        val login: BitwardenLogin? = null,
        val fields: List<BitwardenCustomField>? = null,
        val folderId: String? = null,
    )

    @Serializable
    internal data class BitwardenLogin(
        val username: String? = null,
        val password: String? = null,
        val totp: String? = null,
        val uris: List<BitwardenUri>? = null,
    )

    @Serializable
    internal data class BitwardenUri(
        val uri: String? = null,
    )

    @Serializable
    internal data class BitwardenCustomField(
        val name: String? = null,
        val value: String? = null,
        val type: Int = 0, // 0=text, 1=hidden, 2=boolean
    )
}
