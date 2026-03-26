package org.github.keepasscompose.core.database

import org.github.keepasscompose.TestFixtures
import org.github.keepasscompose.core.model.KdbxEntry
import org.github.keepasscompose.core.model.KdbxEntryField
import org.github.keepasscompose.core.model.KdbxGroup
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KdbxEdgeCaseTest {

    @Test
    fun emptyDatabaseHasNoEntriesOrGroups() {
        val root = KdbxGroup(uuid = "root", name = "Root")
        assertTrue(root.entries.isEmpty())
        assertTrue(root.groups.isEmpty())
    }

    @Test
    fun entryWithUnicodeTitle() {
        val entry = TestFixtures.createEntry(title = "日本語テスト 🔒")
        assertEquals("日本語テスト 🔒", entry.title)
    }

    @Test
    fun entryWithEmptyFields() {
        val entry = KdbxEntry(
            uuid = "empty",
            fields = listOf(
                KdbxEntryField(KdbxEntry.FIELD_TITLE, ""),
                KdbxEntryField(KdbxEntry.FIELD_USER_NAME, ""),
                KdbxEntryField(KdbxEntry.FIELD_PASSWORD, "", isProtected = true),
                KdbxEntryField(KdbxEntry.FIELD_URL, ""),
                KdbxEntryField(KdbxEntry.FIELD_NOTES, ""),
            ),
        )
        assertEquals("", entry.title)
        assertEquals("", entry.userName)
        assertEquals("", entry.password)
    }

    @Test
    fun entryWithVeryLongField() {
        val longValue = "x".repeat(100_000)
        val entry = TestFixtures.createEntry(notes = longValue)
        assertEquals(100_000, entry.notes.length)
    }

    @Test
    fun deeplyNestedGroupHierarchy() {
        var group = KdbxGroup(uuid = "leaf", name = "Leaf")
        for (i in 10 downTo 1) {
            group = KdbxGroup(uuid = "level-$i", name = "Level $i", groups = listOf(group))
        }
        assertEquals("Level 1", group.name)

        var current = group
        var depth = 0
        while (current.groups.isNotEmpty()) {
            current = current.groups[0]
            depth++
        }
        assertEquals(10, depth)
        assertEquals("Leaf", current.name)
    }

    @Test
    fun entryWithManyCustomFields() {
        val fields = (1..100).map { KdbxEntryField("Custom$it", "Value$it") }
        val entry = KdbxEntry(uuid = "many-fields", fields = fields)
        assertEquals(100, entry.fields.size)
        assertEquals("Value50", entry.field("Custom50"))
    }

    @Test
    fun groupWithManyEntries() {
        val entries = (1..1000).map { TestFixtures.createEntry(uuid = "e-$it", title = "Entry $it") }
        val group = TestFixtures.createGroup(entries = entries)
        assertEquals(1000, group.entries.size)
    }

    @Test
    fun entryWithSpecialCharactersInFields() {
        val entry = TestFixtures.createEntry(
            title = "<script>alert('xss')</script>",
            userName = "user&name\"test",
            url = "https://example.com/path?q=a&b=c",
        )
        assertEquals("<script>alert('xss')</script>", entry.title)
        assertEquals("user&name\"test", entry.userName)
    }

    @Test
    fun entryWithLargeAttachment() {
        val data = ByteArray(1024 * 1024) // 1 MB
        val attachment = TestFixtures.createAttachment(data = data)
        assertEquals(1024 * 1024, attachment.data.size)
    }
}
