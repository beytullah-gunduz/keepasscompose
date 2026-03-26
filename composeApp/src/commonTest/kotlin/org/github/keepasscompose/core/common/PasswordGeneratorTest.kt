package org.github.keepasscompose.core.common

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PasswordGeneratorTest {
    // --- Basic generation ---

    @Test
    fun generate_defaultConfig_correctLength() {
        val password = PasswordGenerator.generate(PasswordGenerator.Config())
        assertEquals(20, password.length)
    }

    @Test
    fun generate_customLength() {
        for (len in listOf(1, 4, 8, 16, 32, 64, 128)) {
            val password = PasswordGenerator.generate(PasswordGenerator.Config(length = len))
            assertEquals(len, password.length, "Password length must be $len")
        }
    }

    @Test
    fun generate_zeroLength_throws() {
        assertFailsWith<IllegalArgumentException> {
            PasswordGenerator.generate(PasswordGenerator.Config(length = 0))
        }
    }

    @Test
    fun generate_noCharSets_throws() {
        assertFailsWith<IllegalArgumentException> {
            PasswordGenerator.generate(
                PasswordGenerator.Config(
                    useUppercase = false,
                    useLowercase = false,
                    useDigits = false,
                    useSpecial = false,
                    useBrackets = false,
                    useQuotes = false,
                    useDashes = false,
                    useMath = false,
                ),
            )
        }
    }

    // --- Character set inclusion ---

    @Test
    fun generate_uppercaseOnly() {
        val config =
            PasswordGenerator.Config(
                length = 50,
                useUppercase = true,
                useLowercase = false,
                useDigits = false,
                useSpecial = false,
                useDashes = false,
            )
        val password = PasswordGenerator.generate(config)
        assertTrue(password.all { it in 'A'..'Z' }, "Should contain only uppercase: $password")
    }

    @Test
    fun generate_lowercaseOnly() {
        val config =
            PasswordGenerator.Config(
                length = 50,
                useUppercase = false,
                useLowercase = true,
                useDigits = false,
                useSpecial = false,
                useDashes = false,
            )
        val password = PasswordGenerator.generate(config)
        assertTrue(password.all { it in 'a'..'z' }, "Should contain only lowercase: $password")
    }

    @Test
    fun generate_digitsOnly() {
        val config =
            PasswordGenerator.Config(
                length = 50,
                useUppercase = false,
                useLowercase = false,
                useDigits = true,
                useSpecial = false,
                useDashes = false,
            )
        val password = PasswordGenerator.generate(config)
        assertTrue(password.all { it in '0'..'9' }, "Should contain only digits: $password")
    }

    @Test
    fun generate_containsAllEnabledGroups() {
        // With a long password, all enabled groups should be represented
        val config =
            PasswordGenerator.Config(
                length = 100,
                useUppercase = true,
                useLowercase = true,
                useDigits = true,
                useSpecial = true,
                useDashes = true,
            )
        val password = PasswordGenerator.generate(config, Random(42))
        assertTrue(password.any { it in 'A'..'Z' }, "Should contain uppercase")
        assertTrue(password.any { it in 'a'..'z' }, "Should contain lowercase")
        assertTrue(password.any { it in '0'..'9' }, "Should contain digits")
        assertTrue(password.any { it in PasswordGenerator.SPECIAL }, "Should contain special")
        assertTrue(password.any { it in PasswordGenerator.DASHES }, "Should contain dashes")
    }

    // --- Exclusions ---

    @Test
    fun generate_excludeAmbiguous() {
        val config =
            PasswordGenerator.Config(
                length = 200,
                excludeAmbiguous = true,
            )
        val password = PasswordGenerator.generate(config)
        val ambiguous = "0O1lI|"
        assertFalse(password.any { it in ambiguous }, "Should not contain ambiguous chars: $password")
    }

    @Test
    fun generate_excludeCustomCharacters() {
        val config =
            PasswordGenerator.Config(
                length = 200,
                excludeCharacters = "abc123",
            )
        val password = PasswordGenerator.generate(config)
        assertFalse(password.any { it in "abc123" }, "Should not contain excluded chars: $password")
    }

    // --- Determinism with seeded random ---

    @Test
    fun generate_deterministic_withSameRandom() {
        val config = PasswordGenerator.Config(length = 20)
        val p1 = PasswordGenerator.generate(config, Random(42))
        val p2 = PasswordGenerator.generate(config, Random(42))
        assertEquals(p1, p2, "Same seed must produce same password")
    }

    @Test
    fun generate_different_withDifferentRandom() {
        val config = PasswordGenerator.Config(length = 20)
        val p1 = PasswordGenerator.generate(config, Random(1))
        val p2 = PasswordGenerator.generate(config, Random(2))
        assertFalse(p1 == p2, "Different seeds should produce different passwords")
    }

    // --- Character pool ---

    @Test
    fun buildCharPool_defaultConfig() {
        val pool = PasswordGenerator.buildCharPool(PasswordGenerator.Config())
        assertTrue(pool.contains('A'))
        assertTrue(pool.contains('a'))
        assertTrue(pool.contains('0'))
        assertTrue(pool.contains('!'))
        assertTrue(pool.contains('-'))
    }

    @Test
    fun buildCharPool_excludeAmbiguous_removesCorrectChars() {
        val pool =
            PasswordGenerator.buildCharPool(
                PasswordGenerator.Config(excludeAmbiguous = true),
            )
        assertFalse('0' in pool, "Pool should not contain '0'")
        assertFalse('O' in pool, "Pool should not contain 'O'")
        assertFalse('1' in pool, "Pool should not contain '1'")
        assertFalse('l' in pool, "Pool should not contain 'l'")
        assertFalse('I' in pool, "Pool should not contain 'I'")
    }

    // --- Edge cases ---

    @Test
    fun generate_length1() {
        val config =
            PasswordGenerator.Config(
                length = 1,
                useDigits = true,
                useUppercase = false,
                useLowercase = false,
                useSpecial = false,
                useDashes = false,
            )
        val password = PasswordGenerator.generate(config)
        assertEquals(1, password.length)
        assertTrue(password[0] in '0'..'9')
    }

    @Test
    fun generate_allGroupsEnabled() {
        val config =
            PasswordGenerator.Config(
                length = 50,
                useUppercase = true,
                useLowercase = true,
                useDigits = true,
                useSpecial = true,
                useBrackets = true,
                useQuotes = true,
                useDashes = true,
                useMath = true,
            )
        val password = PasswordGenerator.generate(config)
        assertEquals(50, password.length)
    }
}
