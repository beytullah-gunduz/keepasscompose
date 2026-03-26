package org.github.keepasscompose.core.database

import org.github.keepasscompose.core.model.KdbxEntry
import org.github.keepasscompose.core.model.KdbxGroup

/**
 * Exports a KeePass database to formatted HTML (suitable for printing).
 *
 * Produces a self-contained HTML document with a group hierarchy table.
 * Passwords can optionally be included or masked.
 */
object HtmlExporter {
    data class Config(val includePasswords: Boolean = false, val title: String = "KeePass Database Export")

    fun export(rootGroup: KdbxGroup, config: Config = Config()): String {
        val sb = StringBuilder()
        sb.appendLine("<!DOCTYPE html>")
        sb.appendLine("<html lang=\"en\">")
        sb.appendLine("<head>")
        sb.appendLine("<meta charset=\"UTF-8\">")
        sb.appendLine("<title>${escapeHtml(config.title)}</title>")
        sb.appendLine("<style>")
        sb.appendLine(CSS)
        sb.appendLine("</style>")
        sb.appendLine("</head>")
        sb.appendLine("<body>")
        sb.appendLine("<h1>${escapeHtml(config.title)}</h1>")
        renderGroup(sb, rootGroup, 2, config)
        sb.appendLine("</body>")
        sb.appendLine("</html>")
        return sb.toString()
    }

    private fun renderGroup(sb: StringBuilder, group: KdbxGroup, level: Int, config: Config) {
        sb.appendLine("<h$level>${escapeHtml(group.name)}</h$level>")

        if (group.entries.isNotEmpty()) {
            sb.appendLine("<table>")
            sb.appendLine("<thead><tr><th>Title</th><th>Username</th>")
            if (config.includePasswords) sb.append("<th>Password</th>")
            sb.appendLine("<th>URL</th><th>Notes</th></tr></thead>")
            sb.appendLine("<tbody>")
            for (entry in group.entries) {
                renderEntry(sb, entry, config)
            }
            sb.appendLine("</tbody>")
            sb.appendLine("</table>")
        }

        val nextLevel = minOf(level + 1, 6)
        for (subgroup in group.groups) {
            renderGroup(sb, subgroup, nextLevel, config)
        }
    }

    private fun renderEntry(sb: StringBuilder, entry: KdbxEntry, config: Config) {
        sb.append("<tr>")
        sb.append("<td>${escapeHtml(entry.title)}</td>")
        sb.append("<td>${escapeHtml(entry.userName)}</td>")
        if (config.includePasswords) {
            sb.append("<td>${escapeHtml(entry.password)}</td>")
        }
        val url = entry.url
        if (url.isNotBlank()) {
            sb.append("<td><a href=\"${escapeHtml(url)}\">${escapeHtml(url)}</a></td>")
        } else {
            sb.append("<td></td>")
        }
        sb.append("<td>${escapeHtml(entry.notes)}</td>")
        sb.appendLine("</tr>")
    }

    internal fun escapeHtml(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")

    private val CSS =
        """
        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; margin: 2em; color: #333; }
        h1 { border-bottom: 2px solid #333; padding-bottom: 0.3em; }
        h2, h3, h4, h5, h6 { margin-top: 1.5em; color: #555; }
        table { border-collapse: collapse; width: 100%; margin: 1em 0; }
        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; vertical-align: top; }
        th { background-color: #f5f5f5; font-weight: 600; }
        tr:nth-child(even) { background-color: #fafafa; }
        a { color: #0366d6; text-decoration: none; }
        a:hover { text-decoration: underline; }
        @media print { body { margin: 0; } }
        """.trimIndent()
}
