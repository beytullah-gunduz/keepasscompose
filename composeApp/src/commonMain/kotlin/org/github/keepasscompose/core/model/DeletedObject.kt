package org.github.keepasscompose.core.model

import kotlin.time.Instant

data class DeletedObject(val uuid: String, val deletionTime: Instant? = null)
