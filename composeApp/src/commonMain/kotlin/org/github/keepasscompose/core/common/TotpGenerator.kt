package org.github.keepasscompose.core.common

import org.github.keepasscompose.core.crypto.CryptoProvider
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.math.pow

/**
 * Implements TOTP (RFC 6238) and HOTP (RFC 4226) one-time password generation.
 *
 * Supports SHA-1 (default), SHA-256, and SHA-512 hash algorithms.
 * Parses `otpauth://totp/` URIs and KeePass `TimeOtp-*` entry attributes.
 */
object TotpGenerator {
    enum class Algorithm {
        SHA1,
        SHA256,
        SHA512,
    }

    /**
     * TOTP parameters parsed from a URI or entry attributes.
     */
    data class TotpParams(
        val secret: ByteArray,
        val algorithm: Algorithm = Algorithm.SHA1,
        val digits: Int = 6,
        val period: Int = 30,
        val issuer: String? = null,
        val account: String? = null,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is TotpParams) return false
            return secret.contentEquals(other.secret) && algorithm == other.algorithm &&
                digits == other.digits && period == other.period
        }

        override fun hashCode(): Int = secret.contentHashCode() * 31 + algorithm.hashCode()
    }

    /**
     * Generate a TOTP code for the current time.
     *
     * @param params TOTP parameters (secret, algorithm, digits, period).
     * @param crypto CryptoProvider for SHA-256/SHA-512 HMAC (not needed for SHA-1).
     * @param timeMillis Current time in milliseconds (defaults to system time).
     * @return The TOTP code as a zero-padded string.
     */
    fun generateCode(params: TotpParams, crypto: CryptoProvider? = null, timeMillis: Long = currentTimeMillis()): String {
        val counter = timeMillis / 1000 / params.period
        return generateHotp(params.secret, counter, params.algorithm, params.digits, crypto)
    }

    /**
     * Calculate the seconds remaining until the current TOTP code expires.
     */
    fun secondsRemaining(period: Int, timeMillis: Long = currentTimeMillis()): Int {
        val elapsed = (timeMillis / 1000) % period
        return (period - elapsed).toInt()
    }

    /**
     * Generate an HOTP code (RFC 4226).
     */
    fun generateHotp(
        secret: ByteArray,
        counter: Long,
        algorithm: Algorithm = Algorithm.SHA1,
        digits: Int = 6,
        crypto: CryptoProvider? = null,
    ): String {
        // Counter as 8-byte big-endian
        val counterBytes = ByteArray(8)
        var c = counter
        for (i in 7 downTo 0) {
            counterBytes[i] = (c and 0xFF).toByte()
            c = c shr 8
        }

        // HMAC
        val hash =
            when (algorithm) {
                Algorithm.SHA1 -> {
                    hmacSha1(secret, counterBytes)
                }

                Algorithm.SHA256 -> {
                    requireNotNull(crypto) { "CryptoProvider required for SHA-256" }
                    crypto.hmacSha256(secret, counterBytes)
                }

                Algorithm.SHA512 -> {
                    requireNotNull(crypto) { "CryptoProvider required for SHA-512" }
                    hmacSha512(crypto, secret, counterBytes)
                }
            }

        // Dynamic truncation (RFC 4226 Section 5.4)
        val offset = hash[hash.size - 1].toInt() and 0x0F
        val code =
            ((hash[offset].toInt() and 0x7F) shl 24) or
                ((hash[offset + 1].toInt() and 0xFF) shl 16) or
                ((hash[offset + 2].toInt() and 0xFF) shl 8) or
                (hash[offset + 3].toInt() and 0xFF)

        val otp = code % 10.0.pow(digits).toInt()
        return otp.toString().padStart(digits, '0')
    }

    // --- URI Parsing ---

    /**
     * Parse an `otpauth://totp/` URI into [TotpParams].
     *
     * Format: `otpauth://totp/Label?secret=BASE32&algorithm=SHA1&digits=6&period=30&issuer=Issuer`
     */
    @OptIn(ExperimentalEncodingApi::class)
    fun parseOtpauthUri(uri: String): TotpParams {
        require(uri.startsWith("otpauth://totp/") || uri.startsWith("otpauth://hotp/")) {
            "Invalid otpauth URI: must start with otpauth://totp/ or otpauth://hotp/"
        }

        val afterScheme =
            uri
                .substringAfter("otpauth://totp/", "")
                .ifEmpty { uri.substringAfter("otpauth://hotp/", "") }
        val pathAndQuery = afterScheme.split("?", limit = 2)
        val label = pathAndQuery[0].decodePercent()
        val queryString = pathAndQuery.getOrElse(1) { "" }

        val params = parseQueryString(queryString)

        val secretBase32 = requireNotNull(params["secret"]) { "Missing 'secret' parameter" }
        val secret = base32Decode(secretBase32)

        val algorithm =
            when (params["algorithm"]?.uppercase()) {
                "SHA256", "SHA-256" -> Algorithm.SHA256
                "SHA512", "SHA-512" -> Algorithm.SHA512
                else -> Algorithm.SHA1
            }
        val digits = params["digits"]?.toIntOrNull() ?: 6
        val period = params["period"]?.toIntOrNull() ?: 30
        val issuer = params["issuer"]?.decodePercent()

        val account = if (":" in label) label.substringAfter(":").trim() else label

        return TotpParams(
            secret = secret,
            algorithm = algorithm,
            digits = digits,
            period = period,
            issuer = issuer ?: if (":" in label) label.substringBefore(":").trim() else null,
            account = account.ifEmpty { null },
        )
    }

    // --- KeePass attribute parsing ---

    /**
     * Parse KeePass `TimeOtp-*` entry attributes into [TotpParams].
     *
     * KeePass stores TOTP config as entry string fields:
     * - `otp`: otpauth:// URI (preferred)
     * - `TimeOtp-Secret-Base32`: Base32-encoded secret
     * - `TimeOtp-Period`: Period in seconds
     * - `TimeOtp-Length`: Number of digits
     * - `TimeOtp-Algorithm`: HMAC-SHA-1, HMAC-SHA-256, or HMAC-SHA-512
     */
    fun parseKeePassAttributes(fields: Map<String, String>): TotpParams? {
        // Prefer otpauth:// URI if present
        fields["otp"]?.let { uri ->
            if (uri.startsWith("otpauth://")) {
                return parseOtpauthUri(uri)
            }
        }

        // Fall back to TimeOtp-* attributes
        val secretBase32 = fields["TimeOtp-Secret-Base32"] ?: return null
        val secret = base32Decode(secretBase32)

        val algorithm =
            when (fields["TimeOtp-Algorithm"]?.uppercase()) {
                "HMAC-SHA-256" -> Algorithm.SHA256
                "HMAC-SHA-512" -> Algorithm.SHA512
                else -> Algorithm.SHA1
            }
        val digits = fields["TimeOtp-Length"]?.toIntOrNull() ?: 6
        val period = fields["TimeOtp-Period"]?.toIntOrNull() ?: 30

        return TotpParams(secret = secret, algorithm = algorithm, digits = digits, period = period)
    }

    // --- Base32 ---

    private const val BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"

    internal fun base32Decode(input: String): ByteArray {
        val cleaned = input.uppercase().replace("=", "").replace(" ", "")
        val output = mutableListOf<Byte>()
        var buffer = 0
        var bitsLeft = 0

        for (c in cleaned) {
            val value = BASE32_ALPHABET.indexOf(c)
            if (value < 0) continue // skip invalid chars
            buffer = (buffer shl 5) or value
            bitsLeft += 5
            if (bitsLeft >= 8) {
                bitsLeft -= 8
                output.add((buffer shr bitsLeft).toByte())
                buffer = buffer and ((1 shl bitsLeft) - 1)
            }
        }

        return output.toByteArray()
    }

    // --- Built-in HMAC-SHA1 ---

    private fun hmacSha1(key: ByteArray, data: ByteArray): ByteArray {
        val blockSize = 64
        val adjustedKey =
            when {
                key.size > blockSize -> sha1(key)
                key.size < blockSize -> key + ByteArray(blockSize - key.size)
                else -> key
            }

        val ipad = ByteArray(blockSize) { (adjustedKey[it].toInt() xor 0x36).toByte() }
        val opad = ByteArray(blockSize) { (adjustedKey[it].toInt() xor 0x5C).toByte() }

        return sha1(opad + sha1(ipad + data))
    }

    private fun hmacSha512(crypto: CryptoProvider, key: ByteArray, data: ByteArray): ByteArray {
        // HMAC-SHA512 using CryptoProvider's sha512
        val blockSize = 128
        val adjustedKey =
            when {
                key.size > blockSize -> crypto.sha512(key)
                key.size < blockSize -> key + ByteArray(blockSize - key.size)
                else -> key
            }

        val ipad = ByteArray(blockSize) { (adjustedKey[it].toInt() xor 0x36).toByte() }
        val opad = ByteArray(blockSize) { (adjustedKey[it].toInt() xor 0x5C).toByte() }

        return crypto.sha512(opad + crypto.sha512(ipad + data))
    }

    // --- Built-in SHA-1 (FIPS 180-4) ---

    @OptIn(ExperimentalUnsignedTypes::class)
    private fun sha1(data: ByteArray): ByteArray {
        var h0 = 0x67452301u
        var h1 = 0xEFCDAB89u
        var h2 = 0x98BADCFEu
        var h3 = 0x10325476u
        var h4 = 0xC3D2E1F0u

        // Pre-processing: pad message
        val bitLength = data.size.toLong() * 8
        val padded = mutableListOf<Byte>()
        padded.addAll(data.toList())
        padded.add(0x80.toByte())
        while ((padded.size % 64) != 56) padded.add(0)
        // Append length as 64-bit big-endian
        for (i in 56 downTo 0 step 8) padded.add((bitLength shr i).toByte())

        val message = padded.toByteArray()

        // Process each 512-bit block
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

    // --- Helpers ---

    private fun parseQueryString(query: String): Map<String, String> {
        if (query.isEmpty()) return emptyMap()
        return query.split("&").associate { param ->
            val parts = param.split("=", limit = 2)
            parts[0] to (parts.getOrElse(1) { "" })
        }
    }

    private fun String.decodePercent(): String = replace(Regex("%[0-9A-Fa-f]{2}")) { match ->
        match.value
            .substring(1)
            .toInt(16)
            .toChar()
            .toString()
    }

    private fun currentTimeMillis(): Long = kotlin.time.Clock.System
        .now()
        .toEpochMilliseconds()
}
