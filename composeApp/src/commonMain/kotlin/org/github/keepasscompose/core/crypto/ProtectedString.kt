package org.github.keepasscompose.core.crypto

/**
 * A string wrapper that clears its backing character array when [close] is called.
 *
 * Use this for passwords and other sensitive text that should not linger
 * in memory longer than necessary.
 *
 * Note: Due to JVM string interning and Kotlin/Native immutability, this cannot
 * guarantee that all copies are erased. It provides best-effort clearing of the
 * primary backing buffer. Avoid calling [toString] as it creates an unprotected copy.
 *
 * Implements [AutoCloseable] for use with `use {}` blocks:
 * ```
 * ProtectedString.fromString("password").use { protected ->
 *     // work with protected.toCharArray() or protected.toByteArray()
 * } // buffer is zeroed here
 * ```
 */
class ProtectedString private constructor(private val chars: CharArray) : AutoCloseable {

    private var closed = false

    /**
     * Zero the backing character array. Safe to call multiple times.
     */
    override fun close() {
        if (!closed) {
            chars.fill('\u0000')
            closed = true
        }
    }

    /** Whether the buffer has been zeroed. */
    val isClosed: Boolean get() = closed

    /** The length of the string. */
    val length: Int get() = chars.size

    /**
     * Get a copy of the character array. The caller should zero the copy when done.
     */
    fun toCharArray(): CharArray {
        check(!closed) { "ProtectedString has been closed" }
        return chars.copyOf()
    }

    /**
     * Encode as UTF-8 bytes inside a [ProtectedByteArray].
     * The caller should close the returned ProtectedByteArray when done.
     */
    fun toProtectedByteArray(): ProtectedByteArray {
        check(!closed) { "ProtectedString has been closed" }
        val str = String(chars)
        val bytes = str.encodeToByteArray()
        return ProtectedByteArray(bytes)
    }

    /**
     * Convert to a regular String. Use sparingly — the returned String cannot
     * be cleared from memory.
     */
    override fun toString(): String {
        check(!closed) { "ProtectedString has been closed" }
        return String(chars)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ProtectedString) return false
        if (closed || other.closed) return false
        return chars.contentEquals(other.chars)
    }

    override fun hashCode(): Int {
        if (closed) return 0
        return chars.contentHashCode()
    }

    companion object {
        /**
         * Create from a regular String. The string's content is copied into an internal
         * char array that will be zeroed on [close].
         */
        fun fromString(value: String): ProtectedString =
            ProtectedString(value.toCharArray())

        /**
         * Create from a char array. The array is used directly (not copied) —
         * the caller should not modify or zero it afterward.
         */
        fun fromCharArray(chars: CharArray): ProtectedString =
            ProtectedString(chars)
    }
}
