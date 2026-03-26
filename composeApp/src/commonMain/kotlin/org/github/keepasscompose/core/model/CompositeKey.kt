package org.github.keepasscompose.core.model

data class CompositeKey(val password: String? = null, val keyFileData: ByteArray? = null, val hardwareKeyChallenge: ByteArray? = null) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CompositeKey) return false
        return password == other.password &&
            keyFileData.contentEquals(other.keyFileData) &&
            hardwareKeyChallenge.contentEquals(other.hardwareKeyChallenge)
    }

    override fun hashCode(): Int {
        var result = password?.hashCode() ?: 0
        result = 31 * result + (keyFileData?.contentHashCode() ?: 0)
        result = 31 * result + (hardwareKeyChallenge?.contentHashCode() ?: 0)
        return result
    }
}

private fun ByteArray?.contentEquals(other: ByteArray?): Boolean {
    if (this === other) return true
    if (this == null || other == null) return false
    return this.contentEquals(other)
}

private fun ByteArray?.contentHashCode(): Int = this?.contentHashCode() ?: 0
