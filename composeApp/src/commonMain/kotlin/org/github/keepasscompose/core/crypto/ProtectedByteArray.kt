package org.github.keepasscompose.core.crypto

/**
 * A byte array wrapper that zeros its backing buffer when [close] is called.
 *
 * Use this for passwords, keys, and other sensitive byte data that should not
 * linger in memory longer than necessary.
 *
 * Implements [AutoCloseable] for use with `use {}` blocks:
 * ```
 * ProtectedByteArray(sensitiveBytes).use { protected ->
 *     // work with protected.bytes
 * } // buffer is zeroed here
 * ```
 */
class ProtectedByteArray(private val data: ByteArray) : AutoCloseable {
    private var closed = false

    /**
     * The underlying byte array. Throws [IllegalStateException] if already closed.
     */
    val bytes: ByteArray
        get() {
            check(!closed) { "ProtectedByteArray has been closed" }
            return data
        }

    /** The size of the underlying array. */
    val size: Int get() = data.size

    /**
     * Zero the backing buffer. Safe to call multiple times.
     */
    override fun close() {
        if (!closed) {
            data.fill(0)
            closed = true
        }
    }

    /** Whether the buffer has been zeroed. */
    val isClosed: Boolean get() = closed

    /**
     * Create a copy of the data. The caller is responsible for clearing the copy.
     */
    fun copyBytes(): ByteArray {
        check(!closed) { "ProtectedByteArray has been closed" }
        return data.copyOf()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ProtectedByteArray) return false
        if (closed || other.closed) return false
        return data.contentEquals(other.data)
    }

    override fun hashCode(): Int {
        if (closed) return 0
        return data.contentHashCode()
    }
}
