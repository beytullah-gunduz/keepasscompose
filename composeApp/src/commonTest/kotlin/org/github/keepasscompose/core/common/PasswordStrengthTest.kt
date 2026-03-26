package org.github.keepasscompose.core.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PasswordStrengthTest {
    // --- Strength levels ---

    @Test
    fun estimate_emptyPassword_veryWeak() {
        val result = PasswordStrength.estimate("")
        assertEquals(PasswordStrength.Level.VERY_WEAK, result.level)
        assertEquals(0.0, result.entropyBits)
        assertEquals("instant", result.crackTimeDisplay)
    }

    @Test
    fun estimate_singleChar_veryWeak() {
        val result = PasswordStrength.estimate("a")
        assertEquals(PasswordStrength.Level.VERY_WEAK, result.level)
    }

    @Test
    fun estimate_shortNumeric_veryWeak() {
        val result = PasswordStrength.estimate("1234")
        assertEquals(PasswordStrength.Level.VERY_WEAK, result.level)
    }

    @Test
    fun estimate_commonPassword_penalized() {
        val result = PasswordStrength.estimate("password")
        assertTrue(result.penalties.any { "Common pattern" in it })
    }

    @Test
    fun estimate_mediumPassword_fairOrBetter() {
        // 10 chars mixed case + digits, no common patterns
        val result = PasswordStrength.estimate("Kx7mP2nQ9w")
        assertTrue(result.level >= PasswordStrength.Level.FAIR, "Should be at least FAIR: ${result.level}")
    }

    @Test
    fun estimate_strongPassword() {
        // 16 chars with all character types
        val result = PasswordStrength.estimate("Tr0ub4dor&3!xYzW")
        assertTrue(result.level >= PasswordStrength.Level.FAIR, "Should be at least FAIR: ${result.level}")
        assertTrue(result.entropyBits > 50, "Should have > 50 bits entropy")
    }

    @Test
    fun estimate_longRandomPassword_veryStrong() {
        // 32 random-looking chars
        val result = PasswordStrength.estimate("k8#Lm2!qR5@vN7&pX3\$wZ9*cF4^bY6+")
        assertEquals(PasswordStrength.Level.VERY_STRONG, result.level)
        assertTrue(result.entropyBits > 80)
    }

    // --- Entropy calculation ---

    @Test
    fun entropy_lowercaseOnly() {
        // 8 lowercase chars: 8 * log2(26) ≈ 37.6 bits
        val entropy = PasswordStrength.calculateCharsetEntropy("abcdefgh")
        assertTrue(entropy > 37 && entropy < 39, "Expected ~37.6, got $entropy")
    }

    @Test
    fun entropy_mixedCase() {
        // 8 mixed case: 8 * log2(52) ≈ 45.6 bits
        val entropy = PasswordStrength.calculateCharsetEntropy("AbCdEfGh")
        assertTrue(entropy > 45 && entropy < 47, "Expected ~45.6, got $entropy")
    }

    @Test
    fun entropy_mixedCaseDigits() {
        // 8 chars: 8 * log2(62) ≈ 47.6 bits
        val entropy = PasswordStrength.calculateCharsetEntropy("Abc12345")
        assertTrue(entropy > 47 && entropy < 49, "Expected ~47.6, got $entropy")
    }

    @Test
    fun entropy_allCharTypes() {
        // 8 chars with symbols: 8 * log2(94) ≈ 52.4 bits
        val entropy = PasswordStrength.calculateCharsetEntropy("Ab1!Cd2@")
        assertTrue(entropy > 50 && entropy < 55, "Expected ~52, got $entropy")
    }

    // --- Penalty detection ---

    @Test
    fun repeatPenalty_highRepetition() {
        val penalty = PasswordStrength.calculateRepeatPenalty("aaaaaabb")
        assertTrue(penalty > 0, "Should penalize 75% repetition")
    }

    @Test
    fun repeatPenalty_noRepetition() {
        val penalty = PasswordStrength.calculateRepeatPenalty("abcdefgh")
        assertEquals(0.0, penalty)
    }

    @Test
    fun sequentialPenalty_ascending() {
        val penalty = PasswordStrength.calculateSequentialPenalty("abcdef")
        assertTrue(penalty > 0, "Should detect ascending sequence")
    }

    @Test
    fun sequentialPenalty_descending() {
        val penalty = PasswordStrength.calculateSequentialPenalty("fedcba")
        assertTrue(penalty > 0, "Should detect descending sequence")
    }

    @Test
    fun sequentialPenalty_numeric() {
        val penalty = PasswordStrength.calculateSequentialPenalty("123456")
        assertTrue(penalty > 0, "Should detect numeric sequence")
    }

    @Test
    fun sequentialPenalty_none() {
        val penalty = PasswordStrength.calculateSequentialPenalty("azbycx")
        assertEquals(0.0, penalty)
    }

    // --- Common patterns ---

    @Test
    fun commonPattern_qwerty() {
        assertTrue(PasswordStrength.isCommonPattern("qwerty"))
    }

    @Test
    fun commonPattern_password123() {
        assertTrue(PasswordStrength.isCommonPattern("password123"))
    }

    @Test
    fun commonPattern_randomString_notCommon() {
        assertTrue(!PasswordStrength.isCommonPattern("k8Lm2qR5vN7"))
    }

    // --- Crack time formatting ---

    @Test
    fun formatCrackTime_instant() {
        assertEquals("instant", PasswordStrength.formatCrackTime(0.5))
    }

    @Test
    fun formatCrackTime_seconds() {
        assertEquals("30 seconds", PasswordStrength.formatCrackTime(30.0))
    }

    @Test
    fun formatCrackTime_minutes() {
        assertEquals("5 minutes", PasswordStrength.formatCrackTime(300.0))
    }

    @Test
    fun formatCrackTime_hours() {
        assertEquals("2 hours", PasswordStrength.formatCrackTime(7200.0))
    }

    @Test
    fun formatCrackTime_days() {
        assertEquals("30 days", PasswordStrength.formatCrackTime(30.0 * 86400))
    }

    @Test
    fun formatCrackTime_years() {
        assertEquals("10 years", PasswordStrength.formatCrackTime(10.0 * 86400 * 365))
    }

    // --- All same character ---

    @Test
    fun estimate_allSameChar_penalized() {
        val result = PasswordStrength.estimate("aaaaaaaaaa")
        assertTrue(result.penalties.any { "identical" in it })
        assertEquals(PasswordStrength.Level.VERY_WEAK, result.level)
    }
}
