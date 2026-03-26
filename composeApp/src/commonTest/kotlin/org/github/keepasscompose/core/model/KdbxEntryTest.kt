package org.github.keepasscompose.core.model

import org.github.keepasscompose.TestFixtures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KdbxEntryTest {

    @Test
    fun standardFieldAccessorsReturnCorrectValues() {
        val entry = TestFixtures.createEntry(
            title = "My Title",
            userName = "user1",
            password = "secret",
            url = "https://test.com",
            notes = "Some notes",
        )
        assertEquals("My Title", entry.title)
        assertEquals("user1", entry.userName)
        assertEquals("secret", entry.password)
        assertEquals("https://test.com", entry.url)
        assertEquals("Some notes", entry.notes)
    }

    @Test
    fun missingFieldReturnsEmptyString() {
        val entry = KdbxEntry(uuid = "test")
        assertEquals("", entry.title)
        assertEquals("", entry.userName)
        assertEquals("", entry.password)
        assertEquals("", entry.url)
        assertEquals("", entry.notes)
    }

    @Test
    fun fieldMethodReturnsValueByKey() {
        val entry = TestFixtures.createEntry()
        assertEquals("testuser", entry.field(KdbxEntry.FIELD_USER_NAME))
    }

    @Test
    fun fieldMethodReturnsEmptyForMissingKey() {
        val entry = TestFixtures.createEntry()
        assertEquals("", entry.field("NonExistent"))
    }

    @Test
    fun copyPreservesAllFields() {
        val original = TestFixtures.createEntry(title = "Original")
        val modified = original.copy(uuid = "new-uuid")
        assertEquals("new-uuid", modified.uuid)
        assertEquals("Original", modified.title)
    }

    @Test
    fun entryWithHistoryPreservesVersions() {
        val v1 = TestFixtures.createEntry(uuid = "e1", title = "Version 1")
        val v2 = v1.copy(
            fields = v1.fields.map {
                if (it.key == KdbxEntry.FIELD_TITLE) it.copy(value = "Version 2") else it
            },
            history = listOf(v1),
        )
        assertEquals("Version 2", v2.title)
        assertEquals(1, v2.history.size)
        assertEquals("Version 1", v2.history[0].title)
    }

    @Test
    fun tagsArePreserved() {
        val entry = KdbxEntry(uuid = "t1", tags = listOf("work", "important"))
        assertEquals(2, entry.tags.size)
        assertTrue("work" in entry.tags)
    }
}
