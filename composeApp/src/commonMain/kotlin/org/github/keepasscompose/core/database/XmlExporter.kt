package org.github.keepasscompose.core.database

import org.github.keepasscompose.core.model.KdbxEntry
import org.github.keepasscompose.core.model.KdbxGroup

/**
 * Exports a KeePass database to plain XML for interoperability.
 *
 * This is an **unencrypted** export format intended for data exchange,
 * not for secure storage. Passwords are included in plaintext.
 *
 * The output follows a simplified KeePass XML structure:
 * ```xml
 * <KeePassFile>
 *   <Root>
 *     <Group>
 *       <Name>...</Name>
 *       <Entry>
 *         <String><Key>Title</Key><Value>...</Value></String>
 *         ...
 *       </Entry>
 *       <Group>...</Group>
 *     </Group>
 *   </Root>
 * </KeePassFile>
 * ```
 */
object XmlExporter {
    data class Config(val includePasswords: Boolean = true, val indent: Boolean = true)

    fun export(rootGroup: KdbxGroup, config: Config = Config()): String {
        val sb = StringBuilder()
        val i = if (config.indent) "\t" else ""

        sb.appendLine("<?xml version=\"1.0\" encoding=\"utf-8\"?>")
        sb.appendLine("<KeePassFile>")
        sb.appendLine("$i<Root>")
        writeGroup(sb, rootGroup, if (config.indent) 2 else 0, config)
        sb.appendLine("$i</Root>")
        sb.appendLine("</KeePassFile>")

        return sb.toString()
    }

    private fun writeGroup(sb: StringBuilder, group: KdbxGroup, depth: Int, config: Config) {
        val ind = if (config.indent) "\t".repeat(depth) else ""
        val ind1 = if (config.indent) "\t".repeat(depth + 1) else ""
        val ind2 = if (config.indent) "\t".repeat(depth + 2) else ""

        sb.appendLine("$ind<Group>")
        sb.appendLine("$ind1<UUID>${escapeXml(group.uuid)}</UUID>")
        sb.appendLine("$ind1<Name>${escapeXml(group.name)}</Name>")
        if (group.notes.isNotBlank()) {
            sb.appendLine("$ind1<Notes>${escapeXml(group.notes)}</Notes>")
        }

        for (entry in group.entries) {
            writeEntry(sb, entry, depth + 1, config)
        }

        for (subgroup in group.groups) {
            writeGroup(sb, subgroup, depth + 1, config)
        }

        sb.appendLine("$ind</Group>")
    }

    private fun writeEntry(sb: StringBuilder, entry: KdbxEntry, depth: Int, config: Config) {
        val ind = if (config.indent) "\t".repeat(depth) else ""
        val ind1 = if (config.indent) "\t".repeat(depth + 1) else ""
        val ind2 = if (config.indent) "\t".repeat(depth + 2) else ""

        sb.appendLine("$ind<Entry>")
        sb.appendLine("$ind1<UUID>${escapeXml(entry.uuid)}</UUID>")

        for (field in entry.fields) {
            if (!config.includePasswords && field.isProtected) continue
            sb.appendLine("$ind1<String>")
            sb.appendLine("$ind2<Key>${escapeXml(field.key)}</Key>")
            if (field.isProtected) {
                sb.appendLine("$ind2<Value Protected=\"True\">${escapeXml(field.value)}</Value>")
            } else {
                sb.appendLine("$ind2<Value>${escapeXml(field.value)}</Value>")
            }
            sb.appendLine("$ind1</String>")
        }

        if (entry.tags.isNotEmpty()) {
            sb.appendLine("$ind1<Tags>${escapeXml(entry.tags.joinToString(";"))}</Tags>")
        }

        sb.appendLine("$ind</Entry>")
    }

    internal fun escapeXml(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
}
