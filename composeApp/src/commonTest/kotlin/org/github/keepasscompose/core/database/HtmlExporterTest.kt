package org.github.keepasscompose.core.database

import org.github.keepasscompose.core.model.KdbxEntry
import org.github.keepasscompose.core.model.KdbxEntryField
import org.github.keepasscompose.core.model.KdbxGroup
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HtmlExporterTest {
    private fun entry(title: String, userName: String = "", password: String = "", url: String = "", notes: String = "") = KdbxEntry(
        uuid = title,
        fields =
        listOf(
            KdbxEntryField("Title", title),
            KdbxEntryField("UserName", userName),
            KdbxEntryField("Password", password, isProtected = true),
            KdbxEntryField("URL", url),
            KdbxEntryField("Notes", notes),
        ),
    )

    private val root =
        KdbxGroup(
            uuid = "root",
            name = "Root",
            entries = listOf(entry("GitHub", "john", "secret", "https://github.com", "Dev")),
            groups =
            listOf(
                KdbxGroup(
                    uuid = "sub",
                    name = "Email",
                    entries = listOf(entry("Gmail", "john@gmail.com", "mailpass", "https://gmail.com")),
                ),
            ),
        )

    @Test
    fun export_producesValidHtml() {
        val html = HtmlExporter.export(root)
        assertTrue(html.contains("<!DOCTYPE html>"))
        assertTrue(html.contains("<html"))
        assertTrue(html.contains("</html>"))
    }

    @Test
    fun export_containsTitle() {
        val html = HtmlExporter.export(root, HtmlExporter.Config(title = "My DB"))
        assertTrue(html.contains("<title>My DB</title>"))
        assertTrue(html.contains("<h1>My DB</h1>"))
    }

    @Test
    fun export_containsEntryData() {
        val html = HtmlExporter.export(root)
        assertTrue(html.contains("GitHub"))
        assertTrue(html.contains("john"))
        assertTrue(html.contains("https://github.com"))
    }

    @Test
    fun export_passwordsExcludedByDefault() {
        val html = HtmlExporter.export(root)
        assertFalse(html.contains("secret"))
        assertFalse(html.contains("mailpass"))
    }

    @Test
    fun export_passwordsIncludedWhenConfigured() {
        val html = HtmlExporter.export(root, HtmlExporter.Config(includePasswords = true))
        assertTrue(html.contains("secret"))
        assertTrue(html.contains("mailpass"))
    }

    @Test
    fun export_subgroups() {
        val html = HtmlExporter.export(root)
        assertTrue(html.contains("Email"))
        assertTrue(html.contains("Gmail"))
    }

    @Test
    fun export_urlAsLink() {
        val html = HtmlExporter.export(root)
        assertTrue(html.contains("href=\"https://github.com\""))
    }

    @Test
    fun export_escapesHtml() {
        val xssEntry = entry("Title <script>alert(1)</script>", "user&admin")
        val group = KdbxGroup(uuid = "r", name = "Root", entries = listOf(xssEntry))
        val html = HtmlExporter.export(group)
        assertFalse(html.contains("<script>"))
        assertTrue(html.contains("&lt;script&gt;"))
        assertTrue(html.contains("user&amp;admin"))
    }

    @Test
    fun export_emptyDatabase() {
        val empty = KdbxGroup(uuid = "r", name = "Root")
        val html = HtmlExporter.export(empty)
        assertTrue(html.contains("Root"))
        assertFalse(html.contains("<table>"))
    }

    @Test
    fun export_containsCss() {
        val html = HtmlExporter.export(root)
        assertTrue(html.contains("<style>"))
        assertTrue(html.contains("font-family"))
    }
}
