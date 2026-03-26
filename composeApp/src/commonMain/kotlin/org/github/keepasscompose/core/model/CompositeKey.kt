package org.github.keepasscompose.core.model

/**
 * Holds user credentials for database unlock/save.
 *
 * Implements [AutoCloseable]: call [close] (or use `use {}`) to zero
 * sensitive byte arrays ([keyFileData], [hardwareKeyChallenge]) from memory.
 *
 * Note: [password] is a JVM String and cannot be deterministically zeroed.
 * Callers should minimize the lifetime of CompositeKey instances.
 */
class CompositeKey(val password: String? = null, val keyFileData: ByteArray? = null, val hardwareKeyChallenge: ByteArray? = null) : AutoCloseable {

    /**
     * Zeros all sensitive byte arrays. Safe to call multiple times.
     * The [password] String reference is cleared but the JVM String content
     * cannot be deterministically wiped.
     */
    override fun close() {
        keyFileData?.fill(0)
        hardwareKeyChallenge?.fill(0)
    }

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
