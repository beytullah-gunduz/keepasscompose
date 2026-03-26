package org.github.keepasscompose.core.crypto

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProtectedStringTest {
    @Test
    fun fromString_preservesContent() {
        val p = ProtectedString.fromString("hello")
        assertEquals("hello", p.toString())
        assertEquals(5, p.length)
    }

    @Test
    fun fromCharArray_preservesContent() {
        val chars = charArrayOf('a', 'b', 'c')
        val p = ProtectedString.fromCharArray(chars)
        assertEquals("abc", p.toString())
    }

    @Test
    fun close_zerosCharArray() {
        val chars = charArrayOf('s', 'e', 'c', 'r', 'e', 't')
        val p = ProtectedString.fromCharArray(chars)
        p.close()
        // The original array should be zeroed
        assertTrue(chars.all { it == '\u0000' })
        assertTrue(p.isClosed)
    }

    @Test
    fun close_idempotent() {
        val p = ProtectedString.fromString("test")
        p.close()
        p.close() // should not throw
        assertTrue(p.isClosed)
    }

    @Test
    fun toString_afterClose_throws() {
        val p = ProtectedString.fromString("secret")
        p.close()
        assertFailsWith<IllegalStateException> { p.toString() }
    }

    @Test
    fun toCharArray_returnsIndependentCopy() {
        val p = ProtectedString.fromString("test")
        val copy = p.toCharArray()
        assertContentEquals(charArrayOf('t', 'e', 's', 't'), copy)
        // Modifying copy doesn't affect original
        copy[0] = 'X'
        assertEquals("test", p.toString())
    }

    @Test
    fun toCharArray_afterClose_throws() {
        val p = ProtectedString.fromString("test")
        p.close()
        assertFailsWith<IllegalStateException> { p.toCharArray() }
    }

    @Test
    fun toProtectedByteArray_encodesUtf8() {
        val p = ProtectedString.fromString("hello")
        val pb = p.toProtectedByteArray()
        assertContentEquals("hello".encodeToByteArray(), pb.bytes)
        pb.close()
    }

    @Test
    fun toProtectedByteArray_afterClose_throws() {
        val p = ProtectedString.fromString("test")
        p.close()
        assertFailsWith<IllegalStateException> { p.toProtectedByteArray() }
    }

    @Test
    fun use_closesAfterBlock() {
        val chars = charArrayOf('p', 'a', 's', 's')
        val p = ProtectedString.fromCharArray(chars)
        p.use { protected ->
            assertEquals("pass", protected.toString())
        }
        assertTrue(p.isClosed)
        assertTrue(chars.all { it == '\u0000' })
    }

    @Test
    fun equals_sameContent() {
        val a = ProtectedString.fromString("test")
        val b = ProtectedString.fromString("test")
        assertTrue(a == b)
    }

    @Test
    fun equals_differentContent() {
        val a = ProtectedString.fromString("test1")
        val b = ProtectedString.fromString("test2")
        assertFalse(a == b)
    }

    @Test
    fun equals_closedReturnsFalse() {
        val a = ProtectedString.fromString("test")
        val b = ProtectedString.fromString("test")
        a.close()
        assertFalse(a == b)
    }
}
