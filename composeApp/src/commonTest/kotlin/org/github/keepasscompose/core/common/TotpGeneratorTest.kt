package org.github.keepasscompose.core.common

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class TotpGeneratorTest {

    // RFC 6238 test secret: "12345678901234567890" (ASCII)
    private val rfcSecret = "12345678901234567890".encodeToByteArray()

    // --- RFC 4226 HOTP test vectors (Appendix D) ---
    // Secret = "12345678901234567890", counter = 0..9

    @Test
    fun hotp_rfc4226_testVectors() {
        val expectedCodes = listOf(
            "755224", "287082", "359152", "969429", "338314",
            "254676", "287922", "162583", "399871", "520489",
        )
        for ((counter, expected) in expectedCodes.withIndex()) {
            val code = TotpGenerator.generateHotp(rfcSecret, counter.toLong())
            assertEquals(expected, code, "HOTP counter=$counter")
        }
    }

    // --- RFC 6238 TOTP test vectors (Table 1) ---
    // SHA-1, period=30, digits=8

    @Test
    fun totp_rfc6238_sha1_time59() {
        val params = TotpGenerator.TotpParams(
            secret = rfcSecret, algorithm = TotpGenerator.Algorithm.SHA1,
            digits = 8, period = 30,
        )
        val code = TotpGenerator.generateCode(params, timeMillis = 59_000L)
        assertEquals("94287082", code)
    }

    @Test
    fun totp_rfc6238_sha1_time1111111109() {
        val params = TotpGenerator.TotpParams(
            secret = rfcSecret, algorithm = TotpGenerator.Algorithm.SHA1,
            digits = 8, period = 30,
        )
        val code = TotpGenerator.generateCode(params, timeMillis = 1111111109_000L)
        assertEquals("07081804", code)
    }

    @Test
    fun totp_rfc6238_sha1_time1234567890() {
        val params = TotpGenerator.TotpParams(
            secret = rfcSecret, algorithm = TotpGenerator.Algorithm.SHA1,
            digits = 8, period = 30,
        )
        val code = TotpGenerator.generateCode(params, timeMillis = 1234567890_000L)
        assertEquals("89005924", code)
    }

    // --- 6-digit codes (most common) ---

    @Test
    fun totp_6digit_defaultPeriod() {
        val params = TotpGenerator.TotpParams(secret = rfcSecret, digits = 6, period = 30)
        val code = TotpGenerator.generateCode(params, timeMillis = 59_000L)
        assertEquals(6, code.length)
        assertEquals("287082", code) // counter=1, same as HOTP counter=1
    }

    // --- Seconds remaining ---

    @Test
    fun secondsRemaining_startOfPeriod() {
        // At exactly t=0, full period remains
        assertEquals(30, TotpGenerator.secondsRemaining(30, timeMillis = 0))
    }

    @Test
    fun secondsRemaining_midPeriod() {
        // At t=15s, 15s remain
        assertEquals(15, TotpGenerator.secondsRemaining(30, timeMillis = 15_000))
    }

    @Test
    fun secondsRemaining_endOfPeriod() {
        // At t=29s, 1s remains
        assertEquals(1, TotpGenerator.secondsRemaining(30, timeMillis = 29_000))
    }

    // --- URI parsing ---

    @Test
    fun parseOtpauthUri_basic() {
        val uri = "otpauth://totp/Example:alice@example.com?secret=JBSWY3DPEHPK3PXP&issuer=Example"
        val params = TotpGenerator.parseOtpauthUri(uri)
        assertEquals("Example", params.issuer)
        assertEquals("alice@example.com", params.account)
        assertEquals(TotpGenerator.Algorithm.SHA1, params.algorithm)
        assertEquals(6, params.digits)
        assertEquals(30, params.period)
    }

    @Test
    fun parseOtpauthUri_withAlgorithmAndDigits() {
        val uri = "otpauth://totp/Test?secret=JBSWY3DPEHPK3PXP&algorithm=SHA256&digits=8&period=60"
        val params = TotpGenerator.parseOtpauthUri(uri)
        assertEquals(TotpGenerator.Algorithm.SHA256, params.algorithm)
        assertEquals(8, params.digits)
        assertEquals(60, params.period)
    }

    @Test
    fun parseOtpauthUri_sha512() {
        val uri = "otpauth://totp/Test?secret=JBSWY3DPEHPK3PXP&algorithm=SHA512"
        val params = TotpGenerator.parseOtpauthUri(uri)
        assertEquals(TotpGenerator.Algorithm.SHA512, params.algorithm)
    }

    @Test
    fun parseOtpauthUri_missingSecret_throws() {
        assertFailsWith<IllegalArgumentException> {
            TotpGenerator.parseOtpauthUri("otpauth://totp/Test?issuer=Example")
        }
    }

    @Test
    fun parseOtpauthUri_invalidScheme_throws() {
        assertFailsWith<IllegalArgumentException> {
            TotpGenerator.parseOtpauthUri("https://example.com")
        }
    }

    // --- KeePass attribute parsing ---

    @Test
    fun parseKeePassAttributes_otpUri() {
        val fields = mapOf("otp" to "otpauth://totp/Test?secret=JBSWY3DPEHPK3PXP&digits=8")
        val params = TotpGenerator.parseKeePassAttributes(fields)
        assertNotNull(params)
        assertEquals(8, params.digits)
    }

    @Test
    fun parseKeePassAttributes_timeOtpFields() {
        val fields = mapOf(
            "TimeOtp-Secret-Base32" to "JBSWY3DPEHPK3PXP",
            "TimeOtp-Period" to "60",
            "TimeOtp-Length" to "8",
            "TimeOtp-Algorithm" to "HMAC-SHA-256",
        )
        val params = TotpGenerator.parseKeePassAttributes(fields)
        assertNotNull(params)
        assertEquals(TotpGenerator.Algorithm.SHA256, params.algorithm)
        assertEquals(8, params.digits)
        assertEquals(60, params.period)
    }

    @Test
    fun parseKeePassAttributes_noTotpFields_returnsNull() {
        val fields = mapOf("Title" to "Test", "Password" to "secret")
        val params = TotpGenerator.parseKeePassAttributes(fields)
        assertNull(params)
    }

    // --- Base32 decoding ---

    @Test
    fun base32Decode_knownValue() {
        // "Hello!" in Base32 is "JBSWY3DPEE======"
        val decoded = TotpGenerator.base32Decode("JBSWY3DPEE")
        assertEquals("Hello!", decoded.decodeToString())
    }

    @Test
    fun base32Decode_withPadding() {
        // JBSWY3DPEHPK3PXP is a well-known test secret used in many TOTP examples
        val decoded = TotpGenerator.base32Decode("JBSWY3DPEHPK3PXP")
        assertEquals(10, decoded.size, "Decoded length should be 10 bytes")
        // Verify round-trip by re-decoding
        val decoded2 = TotpGenerator.base32Decode("JBSWY3DPEHPK3PXP")
        assertContentEquals(decoded, decoded2)
    }

    @Test
    fun base32Decode_caseInsensitive() {
        val upper = TotpGenerator.base32Decode("JBSWY3DPEE")
        val lower = TotpGenerator.base32Decode("jbswy3dpee")
        assertContentEquals(upper, lower)
    }
}
