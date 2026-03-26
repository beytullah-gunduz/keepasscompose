package org.github.keepasscompose.core.crypto

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProtectedByteArrayTest {

    @Test
    fun bytes_returnsData() {
        val data = byteArrayOf(1, 2, 3)
        val p = ProtectedByteArray(data)
        assertContentEquals(data, p.bytes)
    }

    @Test
    fun size_returnsLength() {
        val p = ProtectedByteArray(ByteArray(16))
        assertEquals(16, p.size)
    }

    @Test
    fun close_zerosBuffer() {
        val data = byteArrayOf(1, 2, 3, 4, 5)
        val p = ProtectedByteArray(data)
        p.close()
        // The original array should be zeroed
        assertTrue(data.all { it == 0.toByte() })
        assertTrue(p.isClosed)
    }

    @Test
    fun close_idempotent() {
        val p = ProtectedByteArray(byteArrayOf(1, 2, 3))
        p.close()
        p.close() // should not throw
        assertTrue(p.isClosed)
    }

    @Test
    fun bytes_afterClose_throws() {
        val p = ProtectedByteArray(byteArrayOf(1, 2, 3))
        p.close()
        assertFailsWith<IllegalStateException> { p.bytes }
    }

    @Test
    fun copyBytes_returnsIndependentCopy() {
        val data = byteArrayOf(10, 20, 30)
        val p = ProtectedByteArray(data)
        val copy = p.copyBytes()
        assertContentEquals(data, copy)
        // Modifying copy doesn't affect original
        copy[0] = 0
        assertEquals(10, p.bytes[0])
    }

    @Test
    fun copyBytes_afterClose_throws() {
        val p = ProtectedByteArray(byteArrayOf(1))
        p.close()
        assertFailsWith<IllegalStateException> { p.copyBytes() }
    }

    @Test
    fun use_closesAfterBlock() {
        val data = byteArrayOf(1, 2, 3)
        val p = ProtectedByteArray(data)
        p.use { protected ->
            assertContentEquals(byteArrayOf(1, 2, 3), protected.bytes)
        }
        assertTrue(p.isClosed)
        assertTrue(data.all { it == 0.toByte() })
    }

    @Test
    fun equals_sameContent() {
        val a = ProtectedByteArray(byteArrayOf(1, 2, 3))
        val b = ProtectedByteArray(byteArrayOf(1, 2, 3))
        assertTrue(a == b)
    }

    @Test
    fun equals_closedReturnsFalse() {
        val a = ProtectedByteArray(byteArrayOf(1, 2, 3))
        val b = ProtectedByteArray(byteArrayOf(1, 2, 3))
        a.close()
        assertFalse(a == b)
    }
}
