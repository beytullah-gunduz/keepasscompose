package org.github.keepasscompose.core.model

data class KdbxMeta(
    val databaseName: String = "",
    val description: String = "",
    val defaultUserName: String = "",
    val historyMaxItems: Int = DEFAULT_HISTORY_MAX_ITEMS,
    val historyMaxSize: Long = DEFAULT_HISTORY_MAX_SIZE,
    val recycleBinEnabled: Boolean = true,
    val recycleBinUuid: String? = null,
    val protectTitle: Boolean = false,
    val protectUserName: Boolean = false,
    val protectPassword: Boolean = true,
    val protectUrl: Boolean = false,
    val protectNotes: Boolean = false,
) {
    companion object {
        const val DEFAULT_HISTORY_MAX_ITEMS = 12
        const val DEFAULT_HISTORY_MAX_SIZE = 6_291_456L // 6 MB
    }
}
