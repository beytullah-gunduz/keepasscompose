package org.github.keepasscompose.core.common

import kotlin.math.ln
import kotlin.math.pow

/**
 * Estimates password strength based on entropy calculation, pattern detection,
 * and crack time estimation.
 */
object PasswordStrength {
    /**
     * Strength levels with associated thresholds and display properties.
     */
    enum class Level {
        VERY_WEAK,
        WEAK,
        FAIR,
        STRONG,
        VERY_STRONG,
    }

    /**
     * Result of a password strength estimation.
     *
     * @param entropyBits Estimated entropy in bits.
     * @param level Qualitative strength level.
     * @param crackTimeSeconds Estimated time to crack at 10 billion guesses/second.
     * @param crackTimeDisplay Human-readable crack time string.
     * @param penalties Descriptions of detected weaknesses (repeated chars, patterns, etc.).
     */
    data class Result(
        val entropyBits: Double,
        val level: Level,
        val crackTimeSeconds: Double,
        val crackTimeDisplay: String,
        val penalties: List<String>,
    )

    /**
     * Estimate the strength of a password.
     */
    fun estimate(password: String): Result {
        if (password.isEmpty()) {
            return Result(0.0, Level.VERY_WEAK, 0.0, "instant", emptyList())
        }

        val penalties = mutableListOf<String>()
        var entropy = calculateCharsetEntropy(password)

        // Penalty: repeated characters
        val repeatPenalty = calculateRepeatPenalty(password)
        if (repeatPenalty > 0) {
            entropy -= repeatPenalty
            penalties.add("Repeated characters")
        }

        // Penalty: sequential patterns (abc, 123)
        val sequentialPenalty = calculateSequentialPenalty(password)
        if (sequentialPenalty > 0) {
            entropy -= sequentialPenalty
            penalties.add("Sequential patterns")
        }

        // Penalty: common patterns (keyboard rows, etc.)
        if (isCommonPattern(password)) {
            entropy *= 0.5
            penalties.add("Common pattern detected")
        }

        // Penalty: all same character
        if (password.toSet().size == 1) {
            entropy = ln(password.length.toDouble() + 1.0) / ln(2.0)
            penalties.add("All identical characters")
        }

        entropy = entropy.coerceAtLeast(0.0)

        val crackTimeSeconds = estimateCrackTime(entropy)
        val crackTimeDisplay = formatCrackTime(crackTimeSeconds)
        val level = entropyToLevel(entropy)

        return Result(entropy, level, crackTimeSeconds, crackTimeDisplay, penalties)
    }

    /**
     * Calculate entropy based on the character sets used in the password.
     * Entropy = length * log2(charsetSize)
     */
    internal fun calculateCharsetEntropy(password: String): Double {
        var charsetSize = 0
        if (password.any { it in 'a'..'z' }) charsetSize += 26
        if (password.any { it in 'A'..'Z' }) charsetSize += 26
        if (password.any { it in '0'..'9' }) charsetSize += 10
        if (password.any { it.isSymbol() }) charsetSize += 32
        if (password.any { it == ' ' }) charsetSize += 1
        // Unicode/other characters
        if (password.any { !it.isLetterOrDigit() && !it.isSymbol() && it != ' ' }) charsetSize += 64

        if (charsetSize == 0) charsetSize = 1

        return password.length * (ln(charsetSize.toDouble()) / ln(2.0))
    }

    private fun Char.isSymbol(): Boolean = this in "!@#\$%^&*()-_=+[]{}|;:',.<>?/\\`~\""

    /**
     * Penalize passwords with many repeated characters.
     * Returns entropy bits to subtract.
     */
    internal fun calculateRepeatPenalty(password: String): Double {
        if (password.length < 3) return 0.0
        val charCounts = password.groupBy { it }.mapValues { it.value.size }
        val maxRepeat = charCounts.values.maxOrNull() ?: 0
        val ratio = maxRepeat.toDouble() / password.length
        return if (ratio > 0.3) (ratio * 10.0) else 0.0
    }

    /**
     * Penalize passwords with sequential character patterns (abc, 123, cba, 321).
     * Returns entropy bits to subtract.
     */
    internal fun calculateSequentialPenalty(password: String): Double {
        if (password.length < 3) return 0.0
        var sequentialCount = 0
        for (i in 0 until password.length - 2) {
            val c1 = password[i].code
            val c2 = password[i + 1].code
            val c3 = password[i + 2].code
            // Ascending or descending sequence
            if ((c2 - c1 == 1 && c3 - c2 == 1) || (c1 - c2 == 1 && c2 - c3 == 1)) {
                sequentialCount++
            }
        }
        return sequentialCount * 2.0
    }

    /**
     * Check for common keyboard patterns and well-known passwords.
     */
    internal fun isCommonPattern(password: String): Boolean {
        val lower = password.lowercase()
        val commonPatterns =
            listOf(
                "password",
                "123456",
                "qwerty",
                "abc123",
                "letmein",
                "admin",
                "welcome",
                "monkey",
                "master",
                "dragon",
                "login",
                "princess",
                "football",
                "shadow",
                "sunshine",
                "trustno1",
                "iloveyou",
                "batman",
                "access",
                "hello",
                "charlie",
                "donald",
                "qwerty123",
                "password1",
                "1234567890",
                "12345678",
                "1234567",
                "123456789",
                "qwertyuiop",
                "asdfghjkl",
                "zxcvbnm",
            )
        return commonPatterns.any { lower.contains(it) }
    }

    /**
     * Estimate crack time assuming 10 billion guesses per second (modern GPU cluster).
     */
    private fun estimateCrackTime(entropyBits: Double): Double {
        if (entropyBits <= 0) return 0.0
        val guesses = 2.0.pow(entropyBits)
        // Average case: half the keyspace
        return (guesses / 2.0) / 10_000_000_000.0
    }

    private fun entropyToLevel(entropy: Double): Level = when {
        entropy < 28 -> Level.VERY_WEAK
        entropy < 36 -> Level.WEAK
        entropy < 60 -> Level.FAIR
        entropy < 80 -> Level.STRONG
        else -> Level.VERY_STRONG
    }

    /**
     * Format crack time as a human-readable string.
     */
    internal fun formatCrackTime(seconds: Double): String {
        val minute = 60.0
        val hour = 3600.0
        val day = 86400.0
        val year = day * 365.0
        return when {
            seconds < 1 -> "instant"
            seconds < minute -> "${seconds.toLong()} seconds"
            seconds < hour -> "${(seconds / minute).toLong()} minutes"
            seconds < day -> "${(seconds / hour).toLong()} hours"
            seconds < year -> "${(seconds / day).toLong()} days"
            seconds < year * 100 -> "${(seconds / year).toLong()} years"
            seconds < year * 1_000_000 -> "${(seconds / (year * 1000)).toLong()} thousand years"
            seconds < year * 1e9 -> "${(seconds / (year * 1e6)).toLong()} million years"
            else -> "billions of years"
        }
    }
}
