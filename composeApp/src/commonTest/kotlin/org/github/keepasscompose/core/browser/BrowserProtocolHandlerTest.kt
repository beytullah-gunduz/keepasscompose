package org.github.keepasscompose.core.browser

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BrowserProtocolHandlerTest {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    /**
     * Fake BrowserCrypto that uses XOR for encryption/decryption (for testing only).
     * In production, this would use NaCl crypto_box.
     */
    private class FakeBrowserCrypto : BrowserCrypto {
        var generatedPublicKey = ByteArray(32) { (it + 1).toByte() }
        var generatedSecretKey = ByteArray(32) { (it + 33).toByte() }

        override fun generateKeyPair(): Pair<ByteArray, ByteArray> = Pair(generatedPublicKey, generatedSecretKey)

        override fun boxEncrypt(message: ByteArray, nonce: ByteArray, theirPublicKey: ByteArray, mySecretKey: ByteArray): ByteArray {
            // Simple XOR "encryption" for testing
            return message.mapIndexed { i, b -> (b.toInt() xor nonce[i % nonce.size].toInt()).toByte() }.toByteArray()
        }

        override fun boxDecrypt(ciphertext: ByteArray, nonce: ByteArray, theirPublicKey: ByteArray, mySecretKey: ByteArray): ByteArray {
            // Simple XOR "decryption" for testing
            return ciphertext.mapIndexed { i, b -> (b.toInt() xor nonce[i % nonce.size].toInt()).toByte() }.toByteArray()
        }
    }

    private class FakeDatabaseProvider : BrowserDatabaseProvider {
        var isOpen = true
        var hash = "testhash123"
        var logins = listOf<BrowserEntry>()
        var savedLogins = mutableListOf<SavedLogin>()
        val associationList = mutableListOf<BrowserAssociation>()
        var groups = listOf<BrowserGroup>()
        var totpCodes = mutableMapOf<String, String>()
        var locked = false

        data class SavedLogin(val url: String, val submitUrl: String, val login: String, val password: String, val uuid: String?, val group: String?)

        override fun isDatabaseOpen() = isOpen
        override fun getDatabaseHash() = hash
        override fun getLoginsForUrl(url: String) = logins

        override fun saveLogin(url: String, submitUrl: String, login: String, password: String, uuid: String?, group: String?) {
            savedLogins.add(SavedLogin(url, submitUrl, login, password, uuid, group))
        }

        override fun getTotpForEntry(uuid: String) = totpCodes[uuid]

        override fun lockDatabase() {
            locked = true
        }

        override fun getDatabaseGroups() = groups

        override fun createNewGroup(name: String, parentUuid: String?): String? = "new-group-uuid"

        override fun getAssociations(): List<BrowserAssociation> = associationList
        override fun saveAssociation(association: BrowserAssociation) {
            associationList.add(association)
        }
    }

    private fun createHandler(): Triple<BrowserProtocolHandler, FakeBrowserCrypto, FakeDatabaseProvider> {
        val crypto = FakeBrowserCrypto()
        val provider = FakeDatabaseProvider()
        val handler = BrowserProtocolHandler(crypto, provider)
        return Triple(handler, crypto, provider)
    }

    @Test
    fun changePublicKeysReturnsServerPublicKey() {
        val (handler, crypto, _) = createHandler()

        val clientPublicKey = ByteArray(32) { (it + 100).toByte() }
        val request = """{"action":"change-public-keys","publicKey":"${BrowserCrypto.encodeBase64(
            clientPublicKey,
        )}","nonce":"${BrowserCrypto.encodeBase64(ByteArray(24))}","clientID":"test-client"}"""

        val response = handler.handleMessage(request)
        val parsed = json.decodeFromString<ChangePublicKeysTestResponse>(response)

        assertEquals("change-public-keys", parsed.action)
        assertEquals("true", parsed.success)
        assertEquals(BrowserCrypto.encodeBase64(crypto.generatedPublicKey), parsed.publicKey)
        assertNotNull(parsed.version)
    }

    @Test
    fun unknownActionReturnsError() {
        val (handler, _, _) = createHandler()

        val request = """{"action":"unknown-action"}"""
        val response = handler.handleMessage(request)
        val parsed = json.decodeFromString<BrowserResponse>(response)

        assertEquals("false", parsed.success)
        assertEquals(BrowserErrorCode.INCORRECT_ACTION, parsed.errorCode)
    }

    @Test
    fun encryptedActionWithoutKeyExchangeReturnsError() {
        val (handler, _, _) = createHandler()

        val request = """{"action":"get-databasehash","message":"encrypted","nonce":"bm9uY2U="}"""
        val response = handler.handleMessage(request)
        val parsed = json.decodeFromString<BrowserResponse>(response)

        assertEquals("false", parsed.success)
        assertEquals(BrowserErrorCode.ENCRYPTION_KEY_UNRECOGNIZED, parsed.errorCode)
    }

    @Test
    fun getDatabaseHashAfterKeyExchange() {
        val (handler, crypto, _) = createHandler()

        // Key exchange
        performKeyExchange(handler, crypto)

        // Build encrypted get-databasehash request
        val response = sendEncryptedRequest(handler, crypto, """{"action":"get-databasehash"}""")
        val decrypted = decryptResponse(response, crypto)

        assertEquals("true", decrypted.success)
        assertEquals("testhash123", decrypted.hash)
    }

    @Test
    fun getDatabaseHashWhenDatabaseClosedReturnsError() {
        val (handler, crypto, provider) = createHandler()
        provider.isOpen = false

        performKeyExchange(handler, crypto)

        val response = sendEncryptedRequest(handler, crypto, """{"action":"get-databasehash"}""")
        val decrypted = decryptResponse(response, crypto)

        assertEquals("false", decrypted.success)
        assertEquals(BrowserErrorCode.DATABASE_NOT_OPENED, decrypted.errorCode)
    }

    @Test
    fun associateCreatesAssociation() {
        val (handler, crypto, provider) = createHandler()

        performKeyExchange(handler, crypto)

        val response = sendEncryptedRequest(
            handler,
            crypto,
            """{"action":"associate","keys":[{"id":"test-id","key":"dGVzdC1rZXk="}]}""",
        )
        val decrypted = decryptResponse(response, crypto)

        assertEquals("true", decrypted.success)
        assertNotNull(decrypted.id)
        assertTrue(decrypted.id!!.startsWith("keepasscompose_"))
        assertEquals(1, provider.associationList.size)
    }

    @Test
    fun testAssociateWithValidAssociation() {
        val (handler, crypto, provider) = createHandler()
        provider.associationList.add(
            BrowserAssociation(id = "known-id", idKeyBase64 = "a2V5", publicKeyBase64 = "cHVi"),
        )

        performKeyExchange(handler, crypto)

        val response = sendEncryptedRequest(
            handler,
            crypto,
            """{"action":"test-associate","id":"known-id"}""",
        )
        val decrypted = decryptResponse(response, crypto)

        assertEquals("true", decrypted.success)
        assertEquals("known-id", decrypted.id)
    }

    @Test
    fun testAssociateWithUnknownIdFails() {
        val (handler, crypto, _) = createHandler()

        performKeyExchange(handler, crypto)

        val response = sendEncryptedRequest(
            handler,
            crypto,
            """{"action":"test-associate","id":"unknown-id"}""",
        )
        val decrypted = decryptResponse(response, crypto)

        assertEquals("false", decrypted.success)
        assertEquals(BrowserErrorCode.ASSOCIATION_FAILED, decrypted.errorCode)
    }

    @Test
    fun getLoginsReturnsMatchingEntries() {
        val (handler, crypto, provider) = createHandler()
        provider.logins = listOf(
            BrowserEntry(login = "user@test.com", password = "secret123", name = "Test Site", uuid = "uuid-1"),
        )

        performKeyExchange(handler, crypto)

        val response = sendEncryptedRequest(
            handler,
            crypto,
            """{"action":"get-logins","url":"https://test.com","id":"assoc-id"}""",
        )
        val decrypted = decryptResponse(response, crypto)

        assertEquals("true", decrypted.success)
        assertEquals(1, decrypted.count)
        assertEquals("user@test.com", decrypted.entries?.first()?.login)
    }

    @Test
    fun getLoginsWithNoUrlReturnsError() {
        val (handler, crypto, _) = createHandler()

        performKeyExchange(handler, crypto)

        val response = sendEncryptedRequest(handler, crypto, """{"action":"get-logins"}""")
        val decrypted = decryptResponse(response, crypto)

        assertEquals("false", decrypted.success)
        assertEquals(BrowserErrorCode.NO_URL_PROVIDED, decrypted.errorCode)
    }

    @Test
    fun getLoginsWithNoMatchesReturnsError() {
        val (handler, crypto, provider) = createHandler()
        provider.logins = emptyList()

        performKeyExchange(handler, crypto)

        val response = sendEncryptedRequest(
            handler,
            crypto,
            """{"action":"get-logins","url":"https://nomatch.com","id":"assoc-id"}""",
        )
        val decrypted = decryptResponse(response, crypto)

        assertEquals("false", decrypted.success)
        assertEquals(BrowserErrorCode.NO_LOGINS_FOUND, decrypted.errorCode)
    }

    @Test
    fun setLoginSavesCredential() {
        val (handler, crypto, provider) = createHandler()

        performKeyExchange(handler, crypto)

        val response = sendEncryptedRequest(
            handler,
            crypto,
            """{"action":"set-login","url":"https://new.com","submitUrl":"https://new.com/login","login":"admin","password":"p@ss"}""",
        )
        val decrypted = decryptResponse(response, crypto)

        assertEquals("true", decrypted.success)
        assertEquals(1, provider.savedLogins.size)
        assertEquals("admin", provider.savedLogins[0].login)
        assertEquals("p@ss", provider.savedLogins[0].password)
    }

    @Test
    fun lockDatabaseLocksProvider() {
        val (handler, crypto, provider) = createHandler()

        performKeyExchange(handler, crypto)

        val response = sendEncryptedRequest(handler, crypto, """{"action":"lock-database"}""")
        val decrypted = decryptResponse(response, crypto)

        assertEquals("true", decrypted.success)
        assertTrue(provider.locked)
    }

    @Test
    fun getDatabaseGroupsReturnsList() {
        val (handler, crypto, provider) = createHandler()
        provider.groups = listOf(
            BrowserGroup(name = "Root", uuid = "root-uuid", children = listOf(BrowserGroup(name = "Email", uuid = "email-uuid"))),
        )

        performKeyExchange(handler, crypto)

        val response = sendEncryptedRequest(handler, crypto, """{"action":"get-database-groups"}""")
        val decrypted = decryptResponse(response, crypto)

        assertEquals("true", decrypted.success)
        assertEquals(1, decrypted.groups?.size)
        assertEquals("Root", decrypted.groups?.first()?.name)
    }

    @Test
    fun createNewGroupReturnsSuccess() {
        val (handler, crypto, _) = createHandler()

        performKeyExchange(handler, crypto)

        val response = sendEncryptedRequest(
            handler,
            crypto,
            """{"action":"create-new-group","group":"New Group","groupUuid":"parent-uuid"}""",
        )
        val decrypted = decryptResponse(response, crypto)

        assertEquals("true", decrypted.success)
    }

    @Test
    fun getTotpReturnsCode() {
        val (handler, crypto, provider) = createHandler()
        provider.totpCodes["entry-uuid"] = "123456"

        performKeyExchange(handler, crypto)

        val response = sendEncryptedRequest(
            handler,
            crypto,
            """{"action":"get-totp","uuid":"entry-uuid"}""",
        )
        val decrypted = decryptResponse(response, crypto)

        assertEquals("true", decrypted.success)
        assertEquals("123456", decrypted.entries?.first()?.totp)
    }

    @Test
    fun getTotpWithNoConfigReturnsFalse() {
        val (handler, crypto, _) = createHandler()

        performKeyExchange(handler, crypto)

        val response = sendEncryptedRequest(
            handler,
            crypto,
            """{"action":"get-totp","uuid":"no-totp-uuid"}""",
        )
        val decrypted = decryptResponse(response, crypto)

        assertEquals("false", decrypted.success)
    }

    @Test
    fun invalidJsonReturnsError() {
        val (handler, _, _) = createHandler()

        val response = handler.handleMessage("not valid json")
        val parsed = json.decodeFromString<BrowserResponse>(response)

        assertEquals("false", parsed.success)
        assertEquals(BrowserErrorCode.EMPTY_MESSAGE_RECEIVED, parsed.errorCode)
    }

    @Test
    fun resetSessionClearsKeys() {
        val (handler, crypto, _) = createHandler()

        performKeyExchange(handler, crypto)
        handler.resetSession()

        // After reset, encrypted actions should fail
        val request = """{"action":"get-databasehash","message":"encrypted","nonce":"bm9uY2U="}"""
        val response = handler.handleMessage(request)
        val parsed = json.decodeFromString<BrowserResponse>(response)

        assertEquals("false", parsed.success)
        assertEquals(BrowserErrorCode.ENCRYPTION_KEY_UNRECOGNIZED, parsed.errorCode)
    }

    @Test
    fun nonceIncrementWorks() {
        val nonce = byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0x00, 0x00)
        val incremented = BrowserCrypto.incrementNonce(nonce)
        assertEquals(0x00.toByte(), incremented[0])
        assertEquals(0x00.toByte(), incremented[1])
        assertEquals(0x01.toByte(), incremented[2])
        assertEquals(0x00.toByte(), incremented[3])
    }

    @Test
    fun nonceIncrementSimple() {
        val nonce = byteArrayOf(0x01, 0x00, 0x00)
        val incremented = BrowserCrypto.incrementNonce(nonce)
        assertEquals(0x02.toByte(), incremented[0])
        assertEquals(0x00.toByte(), incremented[1])
    }

    // --- Helpers ---

    /** Track the nonce used in the last encrypted request for decrypting the response. */
    private var lastRequestNonce: ByteArray = ByteArray(24)

    private fun performKeyExchange(handler: BrowserProtocolHandler, crypto: FakeBrowserCrypto) {
        val clientPublicKey = ByteArray(32) { (it + 100).toByte() }
        val request = """{"action":"change-public-keys","publicKey":"${BrowserCrypto.encodeBase64(
            clientPublicKey,
        )}","nonce":"${BrowserCrypto.encodeBase64(ByteArray(24))}","clientID":"test-client"}"""
        handler.handleMessage(request)
    }

    private fun sendEncryptedRequest(handler: BrowserProtocolHandler, crypto: FakeBrowserCrypto, decryptedPayload: String): String {
        val nonce = ByteArray(24) { (it + 50).toByte() }
        lastRequestNonce = nonce
        val clientPublicKey = ByteArray(32) { (it + 100).toByte() }
        val encrypted = crypto.boxEncrypt(
            decryptedPayload.encodeToByteArray(),
            nonce,
            crypto.generatedPublicKey,
            clientPublicKey,
        )
        val request = """{"action":${json.encodeToString(
            json.decodeFromString<kotlinx.serialization.json.JsonObject>(decryptedPayload).let {
                it["action"] ?: kotlinx.serialization.json.JsonPrimitive("unknown")
            },
        )},"message":"${BrowserCrypto.encodeBase64(encrypted)}","nonce":"${BrowserCrypto.encodeBase64(nonce)}","clientID":"test-client"}"""
        return handler.handleMessage(request)
    }

    private fun decryptResponse(rawResponse: String, crypto: FakeBrowserCrypto): DecryptedResponse {
        val response = json.decodeFromString<BrowserResponse>(rawResponse)
        val responseNonce = BrowserCrypto.decodeBase64(response.nonce!!)
        val ciphertext = BrowserCrypto.decodeBase64(response.message!!)
        val plaintext = crypto.boxDecrypt(
            ciphertext,
            responseNonce,
            crypto.generatedPublicKey,
            crypto.generatedSecretKey,
        )!!
        return json.decodeFromString<DecryptedResponse>(plaintext.decodeToString())
    }
}

@kotlinx.serialization.Serializable
private data class ChangePublicKeysTestResponse(
    val action: String,
    val version: String? = null,
    val publicKey: String? = null,
    val success: String? = null,
)
