package org.github.keepasscompose.core.common

import kotlin.random.Random

/**
 * Generates random passwords based on configurable character set rules.
 *
 * Supports uppercase/lowercase letters, digits, special characters (with
 * separately toggleable groups), ambiguous character exclusion, and custom
 * character exclusion.
 */
object PasswordGenerator {
    // Character set groups
    const val UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    const val LOWERCASE = "abcdefghijklmnopqrstuvwxyz"
    const val DIGITS = "0123456789"
    const val SPECIAL = "!@#\$%^&*"
    const val BRACKETS = "()[]{}<>"
    const val QUOTES = "\"'`"
    const val DASHES = "-_"
    const val MATH = "+=/\\|~"

    // Ambiguous characters that can be confused visually
    private const val AMBIGUOUS = "0O1lI|"

    /**
     * Configuration for password generation.
     *
     * @param length Password length (must be >= 1).
     * @param useUppercase Include A-Z.
     * @param useLowercase Include a-z.
     * @param useDigits Include 0-9.
     * @param useSpecial Include !@#$%^&*.
     * @param useBrackets Include ()[]{}< >.
     * @param useQuotes Include "'`.
     * @param useDashes Include -_.
     * @param useMath Include +=/\|~.
     * @param excludeAmbiguous Remove visually ambiguous characters (0O1lI|).
     * @param excludeCharacters Additional characters to exclude.
     */
    data class Config(
        val length: Int = 20,
        val useUppercase: Boolean = true,
        val useLowercase: Boolean = true,
        val useDigits: Boolean = true,
        val useSpecial: Boolean = true,
        val useBrackets: Boolean = false,
        val useQuotes: Boolean = false,
        val useDashes: Boolean = true,
        val useMath: Boolean = false,
        val excludeAmbiguous: Boolean = false,
        val excludeCharacters: String = "",
    )

    /**
     * Generate a password using the given configuration.
     *
     * @param config The generation rules.
     * @param random The random source (defaults to secure random).
     * @return The generated password string.
     * @throws IllegalArgumentException if length < 1 or no character sets are enabled.
     */
    fun generate(config: Config, random: Random = Random): String {
        require(config.length >= 1) { "Password length must be at least 1" }

        val charPool = buildCharPool(config)
        require(charPool.isNotEmpty()) { "At least one character set must be enabled" }

        // Generate password ensuring at least one character from each enabled group
        val enabledGroups =
            getEnabledGroups(config)
                .map { applyExclusions(it, config) }
                .filter { it.isNotEmpty() }

        if (enabledGroups.isEmpty() || config.length < enabledGroups.size) {
            // Not enough length to guarantee one from each group — just fill randomly
            return buildString(config.length) {
                repeat(config.length) {
                    append(charPool[random.nextInt(charPool.length)])
                }
            }
        }

        // Pick one character from each enabled group
        val mandatory =
            CharArray(enabledGroups.size) { i ->
                val group = enabledGroups[i]
                group[random.nextInt(group.length)]
            }

        // Fill remaining slots from the full pool
        val remaining = config.length - mandatory.size
        val result = CharArray(config.length)
        mandatory.copyInto(result)
        for (i in mandatory.size until config.length) {
            result[i] = charPool[random.nextInt(charPool.length)]
        }

        // Shuffle to avoid predictable positions
        for (i in result.indices.reversed()) {
            val j = random.nextInt(i + 1)
            val tmp = result[i]
            result[i] = result[j]
            result[j] = tmp
        }

        return result.concatToString()
    }

    /**
     * Build the combined character pool from the configuration.
     */
    fun buildCharPool(config: Config): String {
        val groups = getEnabledGroups(config)
        val pool = groups.joinToString("")
        return applyExclusions(pool, config)
    }

    private fun getEnabledGroups(config: Config): List<String> = buildList {
        if (config.useUppercase) add(UPPERCASE)
        if (config.useLowercase) add(LOWERCASE)
        if (config.useDigits) add(DIGITS)
        if (config.useSpecial) add(SPECIAL)
        if (config.useBrackets) add(BRACKETS)
        if (config.useQuotes) add(QUOTES)
        if (config.useDashes) add(DASHES)
        if (config.useMath) add(MATH)
    }

    private fun applyExclusions(pool: String, config: Config): String {
        var result = pool
        if (config.excludeAmbiguous) {
            result = result.filter { it !in AMBIGUOUS }
        }
        if (config.excludeCharacters.isNotEmpty()) {
            result = result.filter { it !in config.excludeCharacters }
        }
        return result
    }
}
