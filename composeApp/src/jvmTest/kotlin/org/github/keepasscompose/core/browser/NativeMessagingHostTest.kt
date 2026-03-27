package org.github.keepasscompose.core.browser

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NativeMessagingHostTest {
    private val fakeCrypto = object : BrowserCrypto {
        override fun generateKeyPair() = Pair(ByteArray(32), ByteArray(32))
        override fun boxEncrypt(message: ByteArray, nonce: ByteArray, theirPublicKey: ByteArray, mySecretKey: ByteArray) = message
        override fun boxDecrypt(ciphertext: ByteArray, nonce: ByteArray, theirPublicKey: ByteArray, mySecretKey: ByteArray) = ciphertext
    }

    private val fakeProvider = object : BrowserDatabaseProvider {
        override fun isDatabaseOpen() = true
        override fun getDatabaseHash() = "hash"
        override fun getLoginsForUrl(url: String) = emptyList<BrowserEntry>()
        override fun saveLogin(url: String, submitUrl: String, login: String, password: String, uuid: String?, group: String?) {}
        override fun getTotpForEntry(uuid: String): String? = null
        override fun lockDatabase() {}
        override fun getDatabaseGroups() = emptyList<BrowserGroup>()
        override fun createNewGroup(name: String, parentUuid: String?): String? = null
        override fun getAssociations() = emptyList<BrowserAssociation>()
        override fun saveAssociation(association: BrowserAssociation) {}
    }

    private fun createHost(): NativeMessagingHost {
        val handler = BrowserProtocolHandler(fakeCrypto, fakeProvider)
        return NativeMessagingHost(handler, CoroutineScope(Dispatchers.IO))
    }

    private fun encodeNativeMessage(json: String): ByteArray {
        val messageBytes = json.toByteArray(Charsets.UTF_8)
        val lengthBytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(messageBytes.size).array()
        return lengthBytes + messageBytes
    }

    private fun decodeNativeMessage(data: ByteArray): String {
        val length = ByteBuffer.wrap(data, 0, 4).order(ByteOrder.LITTLE_ENDIAN).int
        return String(data, 4, length, Charsets.UTF_8)
    }

    @Test
    fun readMessageDecodesLengthPrefixedJson() {
        val host = createHost()
        val message = """{"action":"change-public-keys"}"""
        val input = ByteArrayInputStream(encodeNativeMessage(message))

        val result = host.readMessage(input)

        assertEquals(message, result)
    }

    @Test
    fun readMessageReturnsNullOnEmptyStream() {
        val host = createHost()
        val input = ByteArrayInputStream(ByteArray(0))

        val result = host.readMessage(input)

        assertNull(result)
    }

    @Test
    fun readMessageReturnsNullOnTruncatedHeader() {
        val host = createHost()
        val input = ByteArrayInputStream(byteArrayOf(0x05, 0x00))

        val result = host.readMessage(input)

        assertNull(result)
    }

    @Test
    fun readMessageReturnsNullOnZeroLength() {
        val host = createHost()
        val lengthBytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(0).array()
        val input = ByteArrayInputStream(lengthBytes)

        val result = host.readMessage(input)

        assertNull(result)
    }

    @Test
    fun readMessageReturnsNullOnOversizedMessage() {
        val host = createHost()
        val lengthBytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(2 * 1024 * 1024).array()
        val input = ByteArrayInputStream(lengthBytes)

        val result = host.readMessage(input)

        assertNull(result)
    }

    @Test
    fun writeMessageEncodesLengthPrefixedJson() {
        val host = createHost()
        val output = ByteArrayOutputStream()
        val message = """{"action":"change-public-keys","success":"true"}"""

        host.writeMessage(output, message)

        val data = output.toByteArray()
        val length = ByteBuffer.wrap(data, 0, 4).order(ByteOrder.LITTLE_ENDIAN).int
        assertEquals(message.length, length)
        assertEquals(message, String(data, 4, length, Charsets.UTF_8))
    }

    @Test
    fun readWriteRoundTrip() {
        val host = createHost()
        val original = """{"action":"test","data":"hello world with special chars: äöü"}"""

        // Write
        val output = ByteArrayOutputStream()
        host.writeMessage(output, original)

        // Read back
        val input = ByteArrayInputStream(output.toByteArray())
        val result = host.readMessage(input)

        assertEquals(original, result)
    }

    @Test
    fun readMultipleMessages() {
        val host = createHost()
        val messages = listOf(
            """{"action":"first"}""",
            """{"action":"second"}""",
            """{"action":"third"}""",
        )

        val combined = ByteArrayOutputStream()
        messages.forEach { combined.write(encodeNativeMessage(it)) }

        val input = ByteArrayInputStream(combined.toByteArray())

        assertEquals(messages[0], host.readMessage(input))
        assertEquals(messages[1], host.readMessage(input))
        assertEquals(messages[2], host.readMessage(input))
        assertNull(host.readMessage(input))
    }

    @Test
    fun isRunningIsFalseInitially() {
        val host = createHost()
        assertTrue(!host.isRunning)
    }
}
