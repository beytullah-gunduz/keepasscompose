package org.github.keepasscompose.core.model

data class KdbxDatabase(val meta: KdbxMeta = KdbxMeta(), val header: KdbxHeader = KdbxHeader(), val rootGroup: KdbxGroup)
