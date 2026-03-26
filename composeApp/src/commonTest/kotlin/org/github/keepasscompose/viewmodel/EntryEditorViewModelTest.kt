package org.github.keepasscompose.viewmodel

import org.github.keepasscompose.TestFixtures
import org.github.keepasscompose.ui.screens.EntryEditorResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EntryEditorViewModelTest {

    private val vm = EntryEditorViewModel()

    private fun createResult(
        title: String = "Test",
        userName: String = "user",
        password: String = "pass",
    ) = EntryEditorResult(
        title = title,
        userName = userName,
        password = password,
        url = "https://example.com",
        notes = "",
        iconIndex = 0,
        tags = listOf("tag1"),
        customFields = emptyList(),
        expires = false,
        expiryDate = "",
    )

    @Test
    fun saveCreatesNewEntryWithGeneratedUuid() {
        vm.startCreating()
        val entry = vm.save(createResult())
        assertNotNull(entry)
        assertTrue(entry.uuid.isNotEmpty())
        assertEquals("Test", entry.title)
        assertEquals("user", entry.userName)
        assertNotNull(entry.creationTime)
    }

    @Test
    fun saveWithBlankTitleReturnsNull() {
        vm.startCreating()
        val entry = vm.save(createResult(title = ""))
        assertNull(entry)
        assertIs<EntryEditorState.ValidationError>(vm.state.value)
    }

    @Test
    fun editPreservesHistoryAndUuid() {
        val original = TestFixtures.createEntry(uuid = "orig-uuid", title = "V1")
        vm.startEditing(original)
        val entry = vm.save(createResult(title = "V2"))
        assertNotNull(entry)
        assertEquals("orig-uuid", entry.uuid)
        assertEquals("V2", entry.title)
        assertEquals(1, entry.history.size)
        assertEquals("V1", entry.history[0].title)
    }

    @Test
    fun saveSetsSavedState() {
        vm.startCreating()
        vm.save(createResult())
        assertIs<EntryEditorState.Saved>(vm.state.value)
    }

    @Test
    fun resetStateClearsToIdle() {
        vm.startCreating()
        vm.save(createResult())
        vm.resetState()
        assertIs<EntryEditorState.Idle>(vm.state.value)
    }
}
