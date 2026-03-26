package org.github.keepasscompose.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class CompositeKeyTest {

    @Test
    fun passwordOnlyKey() {
        val key = CompositeKey(password = "test123")
        assertEquals("test123", key.password)
        assertEquals(null, key.keyFileData)
        assertEquals(null, key.hardwareKeyChallenge)
    }

    @Test
    fun keyWithPasswordAndKeyFile() {
        val keyData = byteArrayOf(1, 2, 3)
        val key = CompositeKey(password = "pass", keyFileData = keyData)
        assertEquals("pass", key.password)
        assertEquals(3, key.keyFileData!!.size)
    }

    @Test
    fun equalityWithSamePasswordOnly() {
        val k1 = CompositeKey(password = "a")
        val k2 = CompositeKey(password = "a")
        assertEquals(k1, k2)
        assertEquals(k1.hashCode(), k2.hashCode())
    }

    @Test
    fun inequalityWithDifferentPassword() {
        val k1 = CompositeKey(password = "a")
        val k2 = CompositeKey(password = "b")
        assertNotEquals(k1, k2)
    }

    @Test
    fun nullKeyFileEquality() {
        val k1 = CompositeKey(password = "a", keyFileData = null)
        val k2 = CompositeKey(password = "a", keyFileData = null)
        assertEquals(k1, k2)
    }

    @Test
    fun emptyKey() {
        val key = CompositeKey()
        assertEquals(null, key.password)
        assertEquals(null, key.keyFileData)
    }
}
