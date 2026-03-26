package org.github.keepasscompose.core.database

import org.github.keepasscompose.core.model.KdbxEntry
import org.github.keepasscompose.core.model.KdbxEntryField
import org.github.keepasscompose.core.model.KdbxGroup
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class XmlExporterTest {
    private fun entry(
        title: String,
        userName: String = "",
        password: String = "",
        url: String = "",
        notes: String = "",
        tags: List<String> = emptyList(),
    ) = KdbxEntry(
        uuid = title,
        fields =
        listOf(
            KdbxEntryField("Title", title),
            KdbxEntryField("UserName", userName),
            KdbxEntryField("Password", password, isProtected = true),
            KdbxEntryField("URL", url),
            KdbxEntryField("Notes", notes),
        ),
        tags = tags,
    )

    private val root =
        KdbxGroup(
            uuid = "root",
            name = "Root",
            entries = listOf(entry("GitHub", "john", "secret", "https://github.com", "Dev", listOf("dev", "code"))),
            groups =
            listOf(
                KdbxGroup(
                    uuid = "sub",
                    name = "Email",
                    entries = listOf(entry("Gmail", "john@gmail.com", "mailpass")),
                ),
            ),
        )

    @Test
    fun export_validXmlStructure() {
        val xml = XmlExporter.export(root)
        assertTrue(xml.contains("<?xml version=\"1.0\""))
        assertTrue(xml.contains("<KeePassFile>"))
        assertTrue(xml.contains("</KeePassFile>"))
        assertTrue(xml.contains("<Root>"))
        assertTrue(xml.contains("<Group>"))
        assertTrue(xml.contains("<Entry>"))
    }

    @Test
    fun export_containsEntryFields() {
        val xml = XmlExporter.export(root)
        assertTrue(xml.contains("<Key>Title</Key>"))
        assertTrue(xml.contains("<Value>GitHub</Value>"))
        assertTrue(xml.contains("<Key>UserName</Key>"))
        assertTrue(xml.contains("<Value>john</Value>"))
    }

    @Test
    fun export_passwordsExcludedByDefault() {
        val xml = XmlExporter.export(root)
        assertFalse(xml.contains("secret"))
        assertFalse(xml.contains("mailpass"))
    }

    @Test
    fun export_passwordsIncludedWhenConfigured() {
        val xml = XmlExporter.export(root, XmlExporter.Config(includePasswords = true))
        assertTrue(xml.contains("secret"))
        assertTrue(xml.contains("Protected=\"True\""))
    }

    @Test
    fun export_subgroups() {
        val xml = XmlExporter.export(root)
        assertTrue(xml.contains("<Name>Email</Name>"))
        assertTrue(xml.contains("<Value>Gmail</Value>"))
    }

    @Test
    fun export_tags() {
        val xml = XmlExporter.export(root)
        assertTrue(xml.contains("<Tags>dev;code</Tags>"))
    }

    @Test
    fun export_escapesXml() {
        val xssEntry = entry("Title <script>", "user&admin")
        val group = KdbxGroup(uuid = "r", name = "Root & \"Stuff\"", entries = listOf(xssEntry))
        val xml = XmlExporter.export(group)
        assertTrue(xml.contains("&lt;script&gt;"))
        assertTrue(xml.contains("user&amp;admin"))
        assertTrue(xml.contains("Root &amp; &quot;Stuff&quot;"))
    }

    @Test
    fun export_emptyDatabase() {
        val empty = KdbxGroup(uuid = "r", name = "Root")
        val xml = XmlExporter.export(empty)
        assertTrue(xml.contains("<Name>Root</Name>"))
        assertFalse(xml.contains("<Entry>"))
    }

    @Test
    fun export_noIndent() {
        val xml = XmlExporter.export(root, XmlExporter.Config(indent = false))
        assertFalse(xml.contains("\t"))
    }

    @Test
    fun export_containsUuids() {
        val xml = XmlExporter.export(root)
        assertTrue(xml.contains("<UUID>root</UUID>"))
        assertTrue(xml.contains("<UUID>GitHub</UUID>"))
    }
}
