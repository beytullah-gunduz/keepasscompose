package org.github.keepasscompose.core.database

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KdbxBinaryPoolTest {
    @Test
    fun emptyPool() {
        val pool = KdbxBinaryPool()
        assertEquals(0, pool.size)
        assertTrue(pool.isEmpty())
        assertNull(pool[0])
        assertFalse(pool.contains(0))
    }

    @Test
    fun putAndGet() {
        val pool = KdbxBinaryPool()
        val data = "hello".encodeToByteArray()

        pool.put(0, data)

        assertEquals(1, pool.size)
        assertTrue(pool.contains(0))
        assertTrue(data.contentEquals(pool[0]!!))
    }

    @Test
    fun constructFromMap() {
        val data0 = "file1".encodeToByteArray()
        val data1 = "file2".encodeToByteArray()
        val pool = KdbxBinaryPool(mapOf(0 to data0, 1 to data1))

        assertEquals(2, pool.size)
        assertTrue(data0.contentEquals(pool[0]!!))
        assertTrue(data1.contentEquals(pool[1]!!))
    }

    @Test
    fun add_assignsSequentialIds() {
        val pool = KdbxBinaryPool()

        val id0 = pool.add("first".encodeToByteArray())
        val id1 = pool.add("second".encodeToByteArray())

        assertEquals(0, id0)
        assertEquals(1, id1)
        assertEquals(2, pool.size)
    }

    @Test
    fun add_continuesAfterExistingMaxId() {
        val pool = KdbxBinaryPool(mapOf(5 to "data".encodeToByteArray()))

        val newId = pool.add("new".encodeToByteArray())

        assertEquals(6, newId)
    }

    @Test
    fun remove() {
        val data = "removeme".encodeToByteArray()
        val pool = KdbxBinaryPool(mapOf(0 to data))

        val removed = pool.remove(0)

        assertNotNull(removed)
        assertTrue(data.contentEquals(removed))
        assertEquals(0, pool.size)
        assertNull(pool[0])
    }

    @Test
    fun remove_nonExistent() {
        val pool = KdbxBinaryPool()
        assertNull(pool.remove(99))
    }

    @Test
    fun clear() {
        val pool =
            KdbxBinaryPool(
                mapOf(
                    0 to "a".encodeToByteArray(),
                    1 to "b".encodeToByteArray(),
                ),
            )

        pool.clear()

        assertEquals(0, pool.size)
        assertTrue(pool.isEmpty())
    }

    @Test
    fun entries_returnsImmutableCopy() {
        val pool = KdbxBinaryPool(mapOf(0 to "data".encodeToByteArray()))

        val entries = pool.entries()

        assertEquals(1, entries.size)
        assertTrue(entries.containsKey(0))
    }

    @Test
    fun findByContent_found() {
        val data = "findme".encodeToByteArray()
        val pool = KdbxBinaryPool(mapOf(3 to data))

        val id = pool.findByContent("findme".encodeToByteArray())

        assertEquals(3, id)
    }

    @Test
    fun findByContent_notFound() {
        val pool = KdbxBinaryPool(mapOf(0 to "other".encodeToByteArray()))

        assertNull(pool.findByContent("missing".encodeToByteArray()))
    }

    @Test
    fun addOrReuse_reusesExisting() {
        val data = "shared".encodeToByteArray()
        val pool = KdbxBinaryPool(mapOf(2 to data))

        val id = pool.addOrReuse("shared".encodeToByteArray())

        assertEquals(2, id)
        assertEquals(1, pool.size) // No new entry added
    }

    @Test
    fun addOrReuse_addsNew() {
        val pool = KdbxBinaryPool(mapOf(0 to "existing".encodeToByteArray()))

        val id = pool.addOrReuse("new".encodeToByteArray())

        assertEquals(1, id)
        assertEquals(2, pool.size)
    }

    @Test
    fun gzipRoundTrip() {
        val original = "This is some test data for GZip compression testing!".encodeToByteArray()

        val compressed = KdbxBinaryPool.compressGzip(original)
        val decompressed = KdbxBinaryPool.decompressGzip(compressed)

        assertTrue(original.contentEquals(decompressed))
        // Compressed data should differ from original
        assertFalse(original.contentEquals(compressed))
    }

    @Test
    fun gzipRoundTrip_emptyData() {
        val original = ByteArray(0)

        val compressed = KdbxBinaryPool.compressGzip(original)
        val decompressed = KdbxBinaryPool.decompressGzip(compressed)

        assertTrue(original.contentEquals(decompressed))
    }

    @Test
    fun gzipRoundTrip_binaryData() {
        val original = ByteArray(256) { it.toByte() }

        val compressed = KdbxBinaryPool.compressGzip(original)
        val decompressed = KdbxBinaryPool.decompressGzip(compressed)

        assertTrue(original.contentEquals(decompressed))
    }

    @Test
    fun put_overwritesExisting() {
        val pool = KdbxBinaryPool()
        pool.put(0, "old".encodeToByteArray())
        pool.put(0, "new".encodeToByteArray())

        assertEquals(1, pool.size)
        assertEquals("new", pool[0]!!.decodeToString())
    }
}
