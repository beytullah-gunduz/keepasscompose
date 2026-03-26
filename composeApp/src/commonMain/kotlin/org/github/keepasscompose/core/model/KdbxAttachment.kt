package org.github.keepasscompose.core.model

data class KdbxAttachment(val id: Int, val name: String, val data: ByteArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KdbxAttachment) return false
        return id == other.id &&
            name == other.name &&
            data.contentEquals(other.data)
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + name.hashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }
}
