package org.github.keepasscompose.core.database

import okio.Buffer
import okio.GzipSink
import okio.GzipSource
import okio.buffer

/**
 * Manages a pool of binary attachments for a KDBX database.
 *
 * In KDBX 3.1, binaries are stored in the XML `<Meta><Binaries>` section.
 * In KDBX 4.x, binaries are stored in the inner header as separate fields.
 *
 * This pool provides a unified interface for both formats, supporting:
 * - ID-based storage and retrieval
 * - GZip compression and decompression
 * - Deduplication (finding existing binaries by content)
 */
class KdbxBinaryPool(entries: Map<Int, ByteArray> = emptyMap()) {
    private val pool = mutableMapOf<Int, ByteArray>()

    init {
        pool.putAll(entries)
    }

    /** Returns the number of binaries in the pool. */
    val size: Int get() = pool.size

    /** Returns true if the pool contains no binaries. */
    fun isEmpty(): Boolean = pool.isEmpty()

    /** Returns the binary data for the given [id], or null if not found. */
    operator fun get(id: Int): ByteArray? = pool[id]

    /** Returns true if the pool contains a binary with the given [id]. */
    fun contains(id: Int): Boolean = pool.containsKey(id)

    /** Returns all entries as an immutable map. */
    fun entries(): Map<Int, ByteArray> = pool.toMap()

    /**
     * Adds binary [data] to the pool with the given [id].
     * Overwrites any existing binary with the same ID.
     */
    fun put(id: Int, data: ByteArray) {
        pool[id] = data
    }

    /**
     * Adds binary [data] to the pool, automatically assigning the next available ID.
     *
     * @return The assigned ID.
     */
    fun add(data: ByteArray): Int {
        val id = nextId()
        pool[id] = data
        return id
    }

    /** Removes the binary with the given [id]. Returns the removed data, or null. */
    fun remove(id: Int): ByteArray? = pool.remove(id)

    /** Removes all binaries from the pool. */
    fun clear() = pool.clear()

    /**
     * Finds an existing binary by content, returning its ID if found.
     * Useful for deduplication when adding attachments.
     */
    fun findByContent(data: ByteArray): Int? = pool.entries.firstOrNull { it.value.contentEquals(data) }?.key

    /**
     * Adds binary [data] to the pool, reusing an existing ID if identical content
     * already exists. Returns the ID (existing or newly assigned).
     */
    fun addOrReuse(data: ByteArray): Int = findByContent(data) ?: add(data)

    private fun nextId(): Int = if (pool.isEmpty()) 0 else pool.keys.max() + 1

    companion object {
        /** Compresses [data] using GZip. */
        fun compressGzip(data: ByteArray): ByteArray {
            val buffer = Buffer()
            GzipSink(buffer).buffer().use { sink ->
                sink.write(data)
            }
            return buffer.readByteArray()
        }

        /** Decompresses GZip-compressed [data]. */
        fun decompressGzip(data: ByteArray): ByteArray {
            val source = Buffer().apply { write(data) }
            return GzipSource(source).buffer().readByteArray()
        }
    }
}
