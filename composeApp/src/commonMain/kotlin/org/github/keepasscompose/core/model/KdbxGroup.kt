package org.github.keepasscompose.core.model

data class KdbxGroup(
    val uuid: String,
    val name: String,
    val icon: KdbxIcon = KdbxIcon(),
    val notes: String = "",
    val groups: List<KdbxGroup> = emptyList(),
    val entries: List<KdbxEntry> = emptyList(),
)
