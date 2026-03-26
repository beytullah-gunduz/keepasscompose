package org.github.keepasscompose

import org.github.keepasscompose.core.model.KdbxAttachment
import org.github.keepasscompose.core.model.KdbxEntry
import org.github.keepasscompose.core.model.KdbxEntryField
import org.github.keepasscompose.core.model.KdbxGroup
import org.github.keepasscompose.core.model.KdbxIcon
import org.github.keepasscompose.core.model.KdbxMeta

/**
 * Shared test fixtures for creating test data consistently across test classes.
 */
object TestFixtures {

    fun createEntry(
        uuid: String = "entry-1",
        title: String = "Test Entry",
        userName: String = "testuser",
        password: String = "testpass123",
        url: String = "https://example.com",
        notes: String = "",
    ): KdbxEntry = KdbxEntry(
        uuid = uuid,
        fields = listOf(
            KdbxEntryField(KdbxEntry.FIELD_TITLE, title),
            KdbxEntryField(KdbxEntry.FIELD_USER_NAME, userName),
            KdbxEntryField(KdbxEntry.FIELD_PASSWORD, password, isProtected = true),
            KdbxEntryField(KdbxEntry.FIELD_URL, url),
            KdbxEntryField(KdbxEntry.FIELD_NOTES, notes),
        ),
    )

    fun createGroup(
        uuid: String = "group-1",
        name: String = "Test Group",
        entries: List<KdbxEntry> = emptyList(),
        groups: List<KdbxGroup> = emptyList(),
    ): KdbxGroup = KdbxGroup(
        uuid = uuid,
        name = name,
        icon = KdbxIcon(),
        entries = entries,
        groups = groups,
    )

    fun createGroupTree(): KdbxGroup = createGroup(
        uuid = "root",
        name = "Root",
        entries = listOf(createEntry(uuid = "e1", title = "Root Entry")),
        groups = listOf(
            createGroup(
                uuid = "general",
                name = "General",
                entries = listOf(
                    createEntry(uuid = "e2", title = "Gmail", userName = "user@gmail.com"),
                    createEntry(uuid = "e3", title = "GitHub", userName = "devuser"),
                ),
            ),
            createGroup(
                uuid = "banking",
                name = "Banking",
                entries = listOf(
                    createEntry(uuid = "e4", title = "Bank", userName = "account123"),
                ),
                groups = listOf(
                    createGroup(uuid = "credit-cards", name = "Credit Cards"),
                ),
            ),
        ),
    )

    fun createMeta(
        databaseName: String = "Test Database",
    ): KdbxMeta = KdbxMeta(
        databaseName = databaseName,
        description = "A test database",
        defaultUserName = "testuser",
    )

    fun createAttachment(
        id: Int = 0,
        name: String = "test.txt",
        data: ByteArray = "Hello, World!".encodeToByteArray(),
    ): KdbxAttachment = KdbxAttachment(id = id, name = name, data = data)
}
