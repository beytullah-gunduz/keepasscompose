package org.github.keepasscompose.core.browser

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Callback interface for the browser protocol handler to interact with the database.
 */
interface BrowserDatabaseProvider {
    /** Whether a database is currently open and unlocked. */
    fun isDatabaseOpen(): Boolean

    /** SHA-256 hash of the root group UUID, used to identify the database. */
    fun getDatabaseHash(): String

    /** Look up entries matching the given URL. */
    fun getLoginsForUrl(url: String): List<BrowserEntry>

    /** Save or update a login entry. */
    fun saveLogin(url: String, submitUrl: String, login: String, password: String, uuid: String?, group: String?)

    /** Get TOTP code for an entry by UUID. */
    fun getTotpForEntry(uuid: String): String?

    /** Lock the database. */
    fun lockDatabase()

    /** Get the group tree for the database. */
    fun getDatabaseGroups(): List<BrowserGroup>

    /** Create a new group. Returns the UUID of the created group. */
    fun createNewGroup(name: String, parentUuid: String?): String?

    /** Get the stored associations for the current database. */
    fun getAssociations(): List<BrowserAssociation>

    /** Store a new association. */
    fun saveAssociation(association: BrowserAssociation)
}

/**
 * Implements the KeePassXC-Browser protocol message handling.
 *
 * This handler processes JSON messages from a browser extension, performs the
 * requested operations (key exchange, credential lookup, etc.), and returns
 * JSON responses. All message payloads (except `change-public-keys`) are
 * encrypted using NaCl public-key authenticated encryption.
 *
 * Protocol flow:
 * 1. `change-public-keys` — Exchange X25519 public keys (unencrypted)
 * 2. `associate` — Associate the extension with the database (encrypted)
 * 3. `test-associate` — Verify an existing association (encrypted)
 * 4. `get-logins` — Retrieve credentials matching a URL (encrypted)
 * 5. `set-login` — Save/update a credential (encrypted)
 * 6. `get-databasehash` — Get the database identifier (encrypted)
 * 7. `lock-database` — Lock the database (encrypted)
 * 8. `get-database-groups` — List groups (encrypted)
 * 9. `create-new-group` — Create a group (encrypted)
 * 10. `get-totp` — Get TOTP code for an entry (encrypted)
 */
@OptIn(ExperimentalEncodingApi::class)
class BrowserProtocolHandler(private val crypto: BrowserCrypto, private val databaseProvider: BrowserDatabaseProvider) {
    companion object {
        const val PROTOCOL_VERSION = "2.0.0"
    }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    // Our key pair for the current session
    private var serverPublicKey: ByteArray? = null
    private var serverSecretKey: ByteArray? = null

    // Client's public key from the initial key exchange
    private var clientPublicKey: ByteArray? = null

    /**
     * Process a raw JSON message from the browser extension and return the JSON response.
     */
    fun handleMessage(rawMessage: String): String {
        val request = try {
            json.decodeFromString<BrowserRequest>(rawMessage)
        } catch (e: Exception) {
            return errorResponse(
                null,
                "Invalid message format",
                BrowserErrorCode.EMPTY_MESSAGE_RECEIVED,
            )
        }

        return when (request.action) {
            BrowserAction.CHANGE_PUBLIC_KEYS -> handleChangePublicKeys(request)

            BrowserAction.GET_DATABASE_HASH -> handleEncryptedAction(request) { handleGetDatabaseHash(it) }

            BrowserAction.ASSOCIATE -> handleEncryptedAction(request) { handleAssociate(it) }

            BrowserAction.TEST_ASSOCIATE -> handleEncryptedAction(request) { handleTestAssociate(it) }

            BrowserAction.GET_LOGINS -> handleEncryptedAction(request) { handleGetLogins(it) }

            BrowserAction.SET_LOGIN -> handleEncryptedAction(request) { handleSetLogin(it) }

            BrowserAction.LOCK_DATABASE -> handleEncryptedAction(request) { handleLockDatabase(it) }

            BrowserAction.GET_DATABASE_GROUPS -> handleEncryptedAction(request) { handleGetDatabaseGroups(it) }

            BrowserAction.CREATE_NEW_GROUP -> handleEncryptedAction(request) { handleCreateNewGroup(it) }

            BrowserAction.GET_TOTP -> handleEncryptedAction(request) { handleGetTotp(it) }

            else -> errorResponse(
                request.action,
                "Unknown action",
                BrowserErrorCode.INCORRECT_ACTION,
            )
        }
    }

    private fun handleChangePublicKeys(request: BrowserRequest): String {
        val clientKey = request.nonce?.let { BrowserCrypto.decodeBase64(it) }
        // In the change-public-keys action, the client's public key is in the `publicKey` field
        // but our BrowserRequest uses the nonce field; let's re-parse for the publicKey field
        val parsed = try {
            json.decodeFromString<ChangePublicKeysRequest>(
                json.encodeToString(request),
            )
        } catch (e: Exception) {
            null
        }

        val clientPubKeyBase64 = parsed?.publicKey ?: request.nonce
        if (clientPubKeyBase64 == null) {
            return errorResponse(
                BrowserAction.CHANGE_PUBLIC_KEYS,
                "No public key received",
                BrowserErrorCode.CLIENT_PUBLIC_KEY_NOT_RECEIVED,
            )
        }

        clientPublicKey = BrowserCrypto.decodeBase64(clientPubKeyBase64)

        val keyPair = crypto.generateKeyPair()
        serverPublicKey = keyPair.first
        serverSecretKey = keyPair.second

        return json.encodeToString(
            ChangePublicKeysResponse(
                action = BrowserAction.CHANGE_PUBLIC_KEYS,
                version = PROTOCOL_VERSION,
                publicKey = BrowserCrypto.encodeBase64(serverPublicKey!!),
                success = "true",
            ),
        )
    }

    private fun handleEncryptedAction(request: BrowserRequest, handler: (DecryptedRequest) -> DecryptedResponse): String {
        if (serverSecretKey == null || clientPublicKey == null) {
            return errorResponse(
                request.action,
                "Key exchange not completed",
                BrowserErrorCode.ENCRYPTION_KEY_UNRECOGNIZED,
            )
        }

        val messageBase64 = request.message ?: return errorResponse(
            request.action,
            "Empty message",
            BrowserErrorCode.EMPTY_MESSAGE_RECEIVED,
        )
        val nonceBase64 = request.nonce ?: return errorResponse(
            request.action,
            "No nonce",
            BrowserErrorCode.CANNOT_DECRYPT_MESSAGE,
        )

        val nonce = BrowserCrypto.decodeBase64(nonceBase64)
        val ciphertext = BrowserCrypto.decodeBase64(messageBase64)

        val plaintext = crypto.boxDecrypt(ciphertext, nonce, clientPublicKey!!, serverSecretKey!!)
            ?: return errorResponse(
                request.action,
                "Cannot decrypt message",
                BrowserErrorCode.CANNOT_DECRYPT_MESSAGE,
            )

        val decrypted = try {
            json.decodeFromString<DecryptedRequest>(plaintext.decodeToString())
        } catch (e: Exception) {
            return errorResponse(
                request.action,
                "Invalid decrypted message",
                BrowserErrorCode.CANNOT_DECRYPT_MESSAGE,
            )
        }

        val response = handler(decrypted)

        // Encrypt the response
        val responseNonce = BrowserCrypto.incrementNonce(nonce)
        val responseJson = json.encodeToString(response)
        val encrypted = crypto.boxEncrypt(
            responseJson.encodeToByteArray(),
            responseNonce,
            clientPublicKey!!,
            serverSecretKey!!,
        )

        return json.encodeToString(
            BrowserResponse(
                action = request.action,
                message = BrowserCrypto.encodeBase64(encrypted),
                nonce = BrowserCrypto.encodeBase64(responseNonce),
            ),
        )
    }

    private fun handleGetDatabaseHash(request: DecryptedRequest): DecryptedResponse {
        if (!databaseProvider.isDatabaseOpen()) {
            return DecryptedResponse(
                success = "false",
                error = "Database not opened",
                errorCode = BrowserErrorCode.DATABASE_NOT_OPENED,
            )
        }
        return DecryptedResponse(
            hash = databaseProvider.getDatabaseHash(),
            version = PROTOCOL_VERSION,
            success = "true",
        )
    }

    private fun handleAssociate(request: DecryptedRequest): DecryptedResponse {
        if (!databaseProvider.isDatabaseOpen()) {
            return DecryptedResponse(
                success = "false",
                error = "Database not opened",
                errorCode = BrowserErrorCode.DATABASE_NOT_OPENED,
            )
        }

        val keyPair = request.keys?.firstOrNull()
        val idKey = keyPair?.key ?: return DecryptedResponse(
            success = "false",
            error = "No ID key provided",
            errorCode = BrowserErrorCode.ASSOCIATION_FAILED,
        )

        val associationId = "keepasscompose_${generateAssociationId()}"
        val association = BrowserAssociation(
            id = associationId,
            idKeyBase64 = idKey,
            publicKeyBase64 = BrowserCrypto.encodeBase64(clientPublicKey!!),
        )
        databaseProvider.saveAssociation(association)

        return DecryptedResponse(
            hash = databaseProvider.getDatabaseHash(),
            version = PROTOCOL_VERSION,
            id = associationId,
            success = "true",
        )
    }

    private fun handleTestAssociate(request: DecryptedRequest): DecryptedResponse {
        if (!databaseProvider.isDatabaseOpen()) {
            return DecryptedResponse(
                success = "false",
                error = "Database not opened",
                errorCode = BrowserErrorCode.DATABASE_NOT_OPENED,
            )
        }

        val id = request.id ?: return DecryptedResponse(
            success = "false",
            error = "No association ID",
            errorCode = BrowserErrorCode.ASSOCIATION_FAILED,
        )

        val associations = databaseProvider.getAssociations()
        val found = associations.any { it.id == id }
        return if (found) {
            DecryptedResponse(
                hash = databaseProvider.getDatabaseHash(),
                version = PROTOCOL_VERSION,
                id = id,
                success = "true",
            )
        } else {
            DecryptedResponse(
                success = "false",
                error = "Association not found",
                errorCode = BrowserErrorCode.ASSOCIATION_FAILED,
            )
        }
    }

    private fun handleGetLogins(request: DecryptedRequest): DecryptedResponse {
        if (!databaseProvider.isDatabaseOpen()) {
            return DecryptedResponse(
                success = "false",
                error = "Database not opened",
                errorCode = BrowserErrorCode.DATABASE_NOT_OPENED,
            )
        }

        val url = request.url
        if (url.isNullOrBlank()) {
            return DecryptedResponse(
                success = "false",
                error = "No URL provided",
                errorCode = BrowserErrorCode.NO_URL_PROVIDED,
            )
        }

        val entries = databaseProvider.getLoginsForUrl(url)
        return if (entries.isEmpty()) {
            DecryptedResponse(
                success = "false",
                error = "No logins found",
                errorCode = BrowserErrorCode.NO_LOGINS_FOUND,
                count = 0,
            )
        } else {
            DecryptedResponse(
                hash = databaseProvider.getDatabaseHash(),
                version = PROTOCOL_VERSION,
                entries = entries,
                count = entries.size,
                success = "true",
                id = request.id,
            )
        }
    }

    private fun handleSetLogin(request: DecryptedRequest): DecryptedResponse {
        if (!databaseProvider.isDatabaseOpen()) {
            return DecryptedResponse(
                success = "false",
                error = "Database not opened",
                errorCode = BrowserErrorCode.DATABASE_NOT_OPENED,
            )
        }

        val url = request.url ?: return DecryptedResponse(
            success = "false",
            error = "No URL provided",
            errorCode = BrowserErrorCode.NO_URL_PROVIDED,
        )
        val login = request.login ?: ""
        val password = request.password ?: ""

        databaseProvider.saveLogin(url, request.submitUrl ?: url, login, password, request.uuid, request.group)

        return DecryptedResponse(
            hash = databaseProvider.getDatabaseHash(),
            version = PROTOCOL_VERSION,
            success = "true",
        )
    }

    private fun handleLockDatabase(request: DecryptedRequest): DecryptedResponse {
        databaseProvider.lockDatabase()
        return DecryptedResponse(success = "true")
    }

    private fun handleGetDatabaseGroups(request: DecryptedRequest): DecryptedResponse {
        if (!databaseProvider.isDatabaseOpen()) {
            return DecryptedResponse(
                success = "false",
                error = "Database not opened",
                errorCode = BrowserErrorCode.DATABASE_NOT_OPENED,
            )
        }

        return DecryptedResponse(
            groups = databaseProvider.getDatabaseGroups(),
            success = "true",
        )
    }

    private fun handleCreateNewGroup(request: DecryptedRequest): DecryptedResponse {
        if (!databaseProvider.isDatabaseOpen()) {
            return DecryptedResponse(
                success = "false",
                error = "Database not opened",
                errorCode = BrowserErrorCode.DATABASE_NOT_OPENED,
            )
        }

        val groupName = request.group ?: return DecryptedResponse(
            success = "false",
            error = "No group name provided",
            errorCode = BrowserErrorCode.INCORRECT_ACTION,
        )

        val newUuid = databaseProvider.createNewGroup(groupName, request.groupUuid)
        return if (newUuid != null) {
            DecryptedResponse(success = "true")
        } else {
            DecryptedResponse(
                success = "false",
                error = "Failed to create group",
                errorCode = BrowserErrorCode.INCORRECT_ACTION,
            )
        }
    }

    private fun handleGetTotp(request: DecryptedRequest): DecryptedResponse {
        if (!databaseProvider.isDatabaseOpen()) {
            return DecryptedResponse(
                success = "false",
                error = "Database not opened",
                errorCode = BrowserErrorCode.DATABASE_NOT_OPENED,
            )
        }

        val uuid = request.uuid ?: return DecryptedResponse(
            success = "false",
            error = "No entry UUID provided",
            errorCode = BrowserErrorCode.INCORRECT_ACTION,
        )

        val totp = databaseProvider.getTotpForEntry(uuid)
        return if (totp != null) {
            DecryptedResponse(
                success = "true",
                entries = listOf(BrowserEntry(login = "", password = "", name = "", uuid = uuid, totp = totp)),
            )
        } else {
            DecryptedResponse(
                success = "false",
                error = "No TOTP configured for entry",
                errorCode = BrowserErrorCode.NO_LOGINS_FOUND,
            )
        }
    }

    private fun errorResponse(action: String?, error: String, errorCode: Int): String = json.encodeToString(
        BrowserResponse(
            action = action,
            success = "false",
            error = error,
            errorCode = errorCode,
        ),
    )

    private fun generateAssociationId(): String {
        val chars = "abcdefghijklmnopqrstuvwxyz0123456789"
        return (1..16).map { chars.random() }.joinToString("")
    }

    /**
     * Reset the session state (e.g., when database is locked).
     */
    fun resetSession() {
        serverPublicKey = null
        serverSecretKey = null
        clientPublicKey = null
    }
}

/**
 * Internal model for parsing the change-public-keys request which has a `publicKey` field.
 */
@kotlinx.serialization.Serializable
private data class ChangePublicKeysRequest(
    val action: String,
    val publicKey: String? = null,
    val nonce: String? = null,
    val clientID: String? = null,
)

/**
 * Response model for the change-public-keys action.
 */
@kotlinx.serialization.Serializable
private data class ChangePublicKeysResponse(val action: String, val version: String, val publicKey: String, val success: String)
