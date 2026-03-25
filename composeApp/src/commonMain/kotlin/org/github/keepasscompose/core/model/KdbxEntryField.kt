package org.github.keepasscompose.core.model

data class KdbxEntryField(
    val key: String,
    val value: String,
    val isProtected: Boolean = false,
)
