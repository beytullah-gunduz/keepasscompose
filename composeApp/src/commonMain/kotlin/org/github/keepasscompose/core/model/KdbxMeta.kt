package org.github.keepasscompose.core.model

data class KdbxMeta(
    val databaseName: String = "",
    val description: String = "",
    val defaultUserName: String = "",
    val recycleBinEnabled: Boolean = true,
    val recycleBinUuid: String? = null,
    val protectTitle: Boolean = false,
    val protectUserName: Boolean = false,
    val protectPassword: Boolean = true,
    val protectUrl: Boolean = false,
    val protectNotes: Boolean = false,
)
