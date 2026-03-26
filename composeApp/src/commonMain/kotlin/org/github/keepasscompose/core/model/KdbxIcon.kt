package org.github.keepasscompose.core.model

data class KdbxIcon(val standardIndex: Int = 0, val customUuid: String? = null, val customData: ByteArray? = null) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KdbxIcon) return false
        return standardIndex == other.standardIndex &&
            customUuid == other.customUuid &&
            customData.contentEquals(other.customData)
    }

    override fun hashCode(): Int {
        var result = standardIndex
        result = 31 * result + (customUuid?.hashCode() ?: 0)
        result = 31 * result + (customData?.contentHashCode() ?: 0)
        return result
    }
}
