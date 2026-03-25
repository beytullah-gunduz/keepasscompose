package org.github.keepasscompose.core.model

import kotlin.time.Instant

data class KdbxEntry(
    val uuid: String,
    val icon: KdbxIcon = KdbxIcon(),
    val tags: List<String> = emptyList(),
    val fields: List<KdbxEntryField> = emptyList(),
    val attachments: List<KdbxAttachment> = emptyList(),
    val history: List<KdbxEntry> = emptyList(),
    val creationTime: Instant? = null,
    val lastModificationTime: Instant? = null,
    val lastAccessTime: Instant? = null,
    val expiryTime: Instant? = null,
    val expires: Boolean = false,
) {
    val title: String get() = field(FIELD_TITLE)
    val userName: String get() = field(FIELD_USER_NAME)
    val password: String get() = field(FIELD_PASSWORD)
    val url: String get() = field(FIELD_URL)
    val notes: String get() = field(FIELD_NOTES)

    fun field(key: String): String =
        fields.firstOrNull { it.key == key }?.value.orEmpty()

    companion object {
        const val FIELD_TITLE = "Title"
        const val FIELD_USER_NAME = "UserName"
        const val FIELD_PASSWORD = "Password"
        const val FIELD_URL = "URL"
        const val FIELD_NOTES = "Notes"
    }
}
