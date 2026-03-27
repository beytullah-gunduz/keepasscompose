package org.github.keepasscompose.core.browser

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Incoming request from a browser extension via the KeePassXC-Browser protocol.
 *
 * The protocol uses JSON messages exchanged over stdin/stdout of a native messaging host.
 * Each message contains an `action` field identifying the operation and optional encrypted
 * payload fields (`message`, `nonce`) for authenticated communication.
 */
@Serializable
data class BrowserRequest(
    val action: String,
    val message: String? = null,
    val nonce: String? = null,
    val clientID: String? = null,
    @SerialName("triggerUnlock") val triggerUnlock: Boolean? = null,
)

/**
 * Outgoing response to the browser extension.
 */
@Serializable
data class BrowserResponse(
    val action: String? = null,
    val message: String? = null,
    val nonce: String? = null,
    val version: String? = null,
    val hash: String? = null,
    val success: String? = null,
    val error: String? = null,
    val errorCode: Int? = null,
    val id: String? = null,
    val entries: List<BrowserEntry>? = null,
    val count: Int? = null,
)

/**
 * A credential entry returned to the browser extension.
 */
@Serializable
data class BrowserEntry(
    val login: String,
    val password: String,
    val name: String,
    val uuid: String,
    val group: String? = null,
    val expired: Boolean? = null,
    val totp: String? = null,
    @SerialName("stringFields") val stringFields: List<BrowserStringField>? = null,
)

/**
 * A custom string field attached to a credential entry.
 */
@Serializable
data class BrowserStringField(val key: String, val value: String)

/**
 * Decrypted message payload from the browser extension.
 * The fields present depend on the action being performed.
 */
@Serializable
data class DecryptedRequest(
    val action: String? = null,
    val url: String? = null,
    val submitUrl: String? = null,
    val id: String? = null,
    val login: String? = null,
    val password: String? = null,
    val group: String? = null,
    val groupUuid: String? = null,
    val uuid: String? = null,
    val keys: List<AssociationKeyPair>? = null,
)

/**
 * A public key pair sent during the associate action.
 */
@Serializable
data class AssociationKeyPair(val id: String? = null, val key: String? = null)

/**
 * Decrypted response payload sent back to the browser extension.
 */
@Serializable
data class DecryptedResponse(
    val hash: String? = null,
    val version: String? = null,
    val success: String? = null,
    val id: String? = null,
    val error: String? = null,
    val errorCode: Int? = null,
    val entries: List<BrowserEntry>? = null,
    val count: Int? = null,
    val groups: List<BrowserGroup>? = null,
)

/**
 * A group entry returned for database-groups action.
 */
@Serializable
data class BrowserGroup(val name: String, val uuid: String, val children: List<BrowserGroup> = emptyList())

/**
 * Standard error codes used in the KeePassXC-Browser protocol.
 */
object BrowserErrorCode {
    const val DATABASE_NOT_OPENED = 1
    const val DATABASE_HASH_NOT_RECEIVED = 2
    const val CLIENT_PUBLIC_KEY_NOT_RECEIVED = 3
    const val CANNOT_DECRYPT_MESSAGE = 4
    const val TIMEOUT_OR_NOT_CONNECTED = 5
    const val ACTION_CANCELLED_OR_DENIED = 6
    const val CANNOT_ENCRYPT_MESSAGE = 7
    const val ASSOCIATION_FAILED = 8
    const val KEY_CHANGE_FAILED = 9
    const val ENCRYPTION_KEY_UNRECOGNIZED = 10
    const val NO_SAVED_DATABASES = 11
    const val INCORRECT_ACTION = 12
    const val EMPTY_MESSAGE_RECEIVED = 13
    const val NO_URL_PROVIDED = 14
    const val NO_LOGINS_FOUND = 15
}

/**
 * Known browser protocol action names.
 */
object BrowserAction {
    const val CHANGE_PUBLIC_KEYS = "change-public-keys"
    const val GET_DATABASE_HASH = "get-databasehash"
    const val ASSOCIATE = "associate"
    const val TEST_ASSOCIATE = "test-associate"
    const val GET_LOGINS = "get-logins"
    const val SET_LOGIN = "set-login"
    const val LOCK_DATABASE = "lock-database"
    const val GET_DATABASE_GROUPS = "get-database-groups"
    const val CREATE_NEW_GROUP = "create-new-group"
    const val GET_TOTP = "get-totp"
    const val REQUEST_AUTOTYPE = "request-autotype"
    const val PASSKEYS_REGISTER = "passkeys-register"
    const val PASSKEYS_GET = "passkeys-get"
}

/**
 * Stored association between the app and a browser extension client.
 */
@Serializable
data class BrowserAssociation(val id: String, val idKeyBase64: String, val publicKeyBase64: String)
