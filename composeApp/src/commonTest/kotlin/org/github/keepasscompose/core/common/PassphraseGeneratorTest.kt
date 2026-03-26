package org.github.keepasscompose.core.common

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PassphraseGeneratorTest {

    @Test
    fun generate_defaultConfig_hasFiveWords() {
        val passphrase = PassphraseGenerator.generate(PassphraseGenerator.Config())
        val words = passphrase.split("-")
        assertEquals(5, words.size)
    }

    @Test
    fun generate_customWordCount() {
        for (count in listOf(1, 3, 5, 7, 10)) {
            val config = PassphraseGenerator.Config(wordCount = count)
            val passphrase = PassphraseGenerator.generate(config)
            val words = passphrase.split("-")
            assertEquals(count, words.size, "Should have $count words")
        }
    }

    @Test
    fun generate_zeroWordCount_throws() {
        assertFailsWith<IllegalArgumentException> {
            PassphraseGenerator.generate(PassphraseGenerator.Config(wordCount = 0))
        }
    }

    // --- Separator ---

    @Test
    fun generate_dashSeparator() {
        val config = PassphraseGenerator.Config(wordCount = 3, separator = PassphraseGenerator.Separator.DASH)
        val passphrase = PassphraseGenerator.generate(config, Random(42))
        assertTrue(passphrase.contains("-"), "Should contain dash separator")
        assertEquals(2, passphrase.count { it == '-' }, "Should have 2 dashes for 3 words")
    }

    @Test
    fun generate_spaceSeparator() {
        val config = PassphraseGenerator.Config(wordCount = 3, separator = PassphraseGenerator.Separator.SPACE)
        val passphrase = PassphraseGenerator.generate(config, Random(42))
        assertTrue(passphrase.contains(" "))
    }

    @Test
    fun generate_dotSeparator() {
        val config = PassphraseGenerator.Config(wordCount = 3, separator = PassphraseGenerator.Separator.DOT)
        val passphrase = PassphraseGenerator.generate(config, Random(42))
        assertTrue(passphrase.contains("."))
    }

    @Test
    fun generate_underscoreSeparator() {
        val config = PassphraseGenerator.Config(wordCount = 3, separator = PassphraseGenerator.Separator.UNDERSCORE)
        val passphrase = PassphraseGenerator.generate(config, Random(42))
        assertTrue(passphrase.contains("_"))
    }

    @Test
    fun generate_noneSeparator() {
        val config = PassphraseGenerator.Config(wordCount = 3, separator = PassphraseGenerator.Separator.NONE)
        val passphrase = PassphraseGenerator.generate(config, Random(42))
        assertFalse(passphrase.contains("-"))
        assertFalse(passphrase.contains(" "))
    }

    // --- Capitalize ---

    @Test
    fun generate_capitalize() {
        val config = PassphraseGenerator.Config(wordCount = 5, capitalize = true)
        val passphrase = PassphraseGenerator.generate(config, Random(42))
        val words = passphrase.split("-")
        for (word in words) {
            // Strip trailing digit if includeNumber was set
            val clean = word.trimEnd { it.isDigit() }
            assertTrue(clean[0].isUpperCase(), "Word '$word' should start with uppercase")
        }
    }

    @Test
    fun generate_noCapitalize() {
        val config = PassphraseGenerator.Config(wordCount = 5, capitalize = false)
        val passphrase = PassphraseGenerator.generate(config, Random(42))
        val words = passphrase.split("-")
        for (word in words) {
            assertTrue(word[0].isLowerCase(), "Word '$word' should start with lowercase")
        }
    }

    // --- Include number ---

    @Test
    fun generate_includeNumber() {
        val config = PassphraseGenerator.Config(wordCount = 5, includeNumber = true)
        val passphrase = PassphraseGenerator.generate(config, Random(42))
        assertTrue(passphrase.any { it.isDigit() }, "Should contain at least one digit: $passphrase")
    }

    @Test
    fun generate_noNumber() {
        val config = PassphraseGenerator.Config(wordCount = 5, includeNumber = false)
        val passphrase = PassphraseGenerator.generate(config, Random(42))
        assertFalse(passphrase.any { it.isDigit() }, "Should not contain digits: $passphrase")
    }

    // --- Determinism ---

    @Test
    fun generate_deterministic_withSameRandom() {
        val config = PassphraseGenerator.Config(wordCount = 5)
        val p1 = PassphraseGenerator.generate(config, Random(42))
        val p2 = PassphraseGenerator.generate(config, Random(42))
        assertEquals(p1, p2, "Same seed must produce same passphrase")
    }

    @Test
    fun generate_different_withDifferentRandom() {
        val config = PassphraseGenerator.Config(wordCount = 5)
        val p1 = PassphraseGenerator.generate(config, Random(1))
        val p2 = PassphraseGenerator.generate(config, Random(2))
        assertFalse(p1 == p2, "Different seeds should produce different passphrases")
    }

    // --- Word list ---

    @Test
    fun wordListSize_isPositive() {
        assertTrue(PassphraseGenerator.wordListSize > 100, "Word list should have 100+ words")
    }

    // --- Edge cases ---

    @Test
    fun generate_singleWord() {
        val config = PassphraseGenerator.Config(wordCount = 1)
        val passphrase = PassphraseGenerator.generate(config)
        assertFalse(passphrase.contains("-"), "Single word should have no separator")
        assertTrue(passphrase.isNotEmpty())
    }

    @Test
    fun generate_capitalizeAndNumber() {
        val config = PassphraseGenerator.Config(
            wordCount = 3, capitalize = true, includeNumber = true,
        )
        val passphrase = PassphraseGenerator.generate(config, Random(42))
        assertTrue(passphrase.any { it.isDigit() }, "Should have a digit")
        assertTrue(passphrase.any { it.isUpperCase() }, "Should have uppercase")
    }
}
