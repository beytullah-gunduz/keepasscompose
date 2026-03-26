package org.github.keepasscompose.core.common

import org.github.keepasscompose.core.model.KdbxEntry
import org.github.keepasscompose.core.model.KdbxGroup

/**
 * Checks passwords against the Have I Been Pwned (HIBP) Pwned Passwords API
 * using the k-anonymity model.
 *
 * Only the first 5 characters of the SHA-1 hash are sent to the API,
 * preserving password privacy while checking for known breaches.
 *
 * @param httpFetcher Platform-specific HTTP client for API requests.
 * @param sha1 SHA-1 hash function (defaults to [TotpGenerator]'s built-in SHA-1).
 */
class HibpChecker(private val httpFetcher: HttpFetcher, private val sha1: (ByteArray) -> ByteArray = { sha1Hash(it) }) {
    /**
     * Abstraction for HTTP GET requests. Implement per-platform.
     */
    fun interface HttpFetcher {
        /**
         * Perform an HTTP GET request and return the response body as a string.
         * Should throw on network errors.
         */
        fun fetch(url: String): String
    }

    /**
     * Result of checking a single password.
     */
    data class BreachResult(val isBreached: Boolean, val breachCount: Int)

    /**
     * Result of checking a single entry.
     */
    data class EntryBreachResult(val entry: KdbxEntry, val groupPath: List<String>, val result: BreachResult)

    /**
     * Check if a single password appears in known breaches.
     *
     * @param password The password to check.
     * @return [BreachResult] with breach status and count.
     */
    fun checkPassword(password: String): BreachResult {
        if (password.isEmpty()) return BreachResult(false, 0)

        val hash = sha1(password.encodeToByteArray()).toHexUpper()
        val prefix = hash.substring(0, 5)
        val suffix = hash.substring(5)

        val response = httpFetcher.fetch("$API_BASE_URL/$prefix")
        return parseResponse(response, suffix)
    }

    /**
     * Batch check all entries in a database.
     * Entries with the same password are only checked once.
     *
     * @param rootGroup The root group to check recursively.
     * @param onProgress Called after each unique password is checked, with (checked, total).
     * @return List of breach results for all entries with passwords.
     */
    fun checkAllEntries(rootGroup: KdbxGroup, onProgress: ((checked: Int, total: Int) -> Unit)? = null): List<EntryBreachResult> {
        val allEntries = collectEntries(rootGroup, emptyList())

        // Group by password to avoid redundant API calls
        val passwordGroups = mutableMapOf<String, MutableList<Pair<KdbxEntry, List<String>>>>()
        for ((entry, path) in allEntries) {
            val password = entry.password
            if (password.isNotEmpty()) {
                passwordGroups.getOrPut(password) { mutableListOf() }.add(entry to path)
            }
        }

        val results = mutableListOf<EntryBreachResult>()
        var checked = 0
        val total = passwordGroups.size

        for ((password, entries) in passwordGroups) {
            val breachResult = checkPassword(password)
            for ((entry, path) in entries) {
                results.add(EntryBreachResult(entry, path, breachResult))
            }
            checked++
            onProgress?.invoke(checked, total)
        }

        return results
    }

    // --- Internal ---

    internal fun parseResponse(response: String, suffix: String): BreachResult {
        // Response format: "SUFFIX:COUNT\r\n" per line
        for (line in response.lineSequence()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue
            val parts = trimmed.split(":", limit = 2)
            if (parts.size == 2 && parts[0].equals(suffix, ignoreCase = true)) {
                val count = parts[1].trim().toIntOrNull() ?: 0
                return BreachResult(true, count)
            }
        }
        return BreachResult(false, 0)
    }

    private fun collectEntries(group: KdbxGroup, path: List<String>): List<Pair<KdbxEntry, List<String>>> {
        val currentPath = path + group.name
        val results = mutableListOf<Pair<KdbxEntry, List<String>>>()
        for (entry in group.entries) results.add(entry to currentPath)
        for (subgroup in group.groups) results.addAll(collectEntries(subgroup, currentPath))
        return results
    }

    private fun ByteArray.toHexUpper(): String = joinToString("") {
        it
            .toUByte()
            .toString(16)
            .padStart(2, '0')
            .uppercase()
    }

    companion object {
        const val API_BASE_URL = "https://api.pwnedpasswords.com/range"

        // Built-in SHA-1 (reuses the implementation from TotpGenerator)
        @OptIn(ExperimentalUnsignedTypes::class)
        private fun sha1Hash(data: ByteArray): ByteArray {
            var h0 = 0x67452301u
            var h1 = 0xEFCDAB89u
            var h2 = 0x98BADCFEu
            var h3 = 0x10325476u
            var h4 = 0xC3D2E1F0u

            val bitLength = data.size.toLong() * 8
            val padded = mutableListOf<Byte>()
            padded.addAll(data.toList())
            padded.add(0x80.toByte())
            while ((padded.size % 64) != 56) padded.add(0)
            for (i in 56 downTo 0 step 8) padded.add((bitLength shr i).toByte())

            val message = padded.toByteArray()

            for (blockStart in message.indices step 64) {
                val w = UIntArray(80)
                for (i in 0 until 16) {
                    w[i] = (
                        (message[blockStart + i * 4].toUByte().toUInt() shl 24) or
                            (message[blockStart + i * 4 + 1].toUByte().toUInt() shl 16) or
                            (message[blockStart + i * 4 + 2].toUByte().toUInt() shl 8) or
                            message[blockStart + i * 4 + 3].toUByte().toUInt()
                        )
                }
                for (i in 16 until 80) {
                    w[i] = (w[i - 3] xor w[i - 8] xor w[i - 14] xor w[i - 16]).rotateLeft(1)
                }

                var a = h0
                var b = h1
                var c = h2
                var d = h3
                var e = h4

                for (i in 0 until 80) {
                    val (f, k) =
                        when (i) {
                            in 0..19 -> Pair((b and c) or (b.inv() and d), 0x5A827999u)
                            in 20..39 -> Pair(b xor c xor d, 0x6ED9EBA1u)
                            in 40..59 -> Pair((b and c) or (b and d) or (c and d), 0x8F1BBCDCu)
                            else -> Pair(b xor c xor d, 0xCA62C1D6u)
                        }
                    val temp = a.rotateLeft(5) + f + e + k + w[i]
                    e = d
                    d = c
                    c = b.rotateLeft(30)
                    b = a
                    a = temp
                }

                h0 += a
                h1 += b
                h2 += c
                h3 += d
                h4 += e
            }

            val result = ByteArray(20)

            fun putUInt(value: UInt, offset: Int) {
                result[offset] = (value shr 24).toByte()
                result[offset + 1] = (value shr 16).toByte()
                result[offset + 2] = (value shr 8).toByte()
                result[offset + 3] = value.toByte()
            }
            putUInt(h0, 0)
            putUInt(h1, 4)
            putUInt(h2, 8)
            putUInt(h3, 12)
            putUInt(h4, 16)
            return result
        }
    }
}
