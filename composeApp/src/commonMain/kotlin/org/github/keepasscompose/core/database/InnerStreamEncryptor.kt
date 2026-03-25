package org.github.keepasscompose.core.database

/**
 * Stateful encryptor for protected field values in the KDBX XML payload.
 *
 * The inner random stream (Salsa20 for KDBX 3.1, ChaCha20 for KDBX 4.x) is a stream cipher,
 * so each call to [encrypt] consumes the next chunk of the keystream. Protected values must
 * therefore be encrypted in the order they appear in the XML.
 *
 * The actual cipher implementation is provided by the cryptography layer (Epic 3).
 */
fun interface InnerStreamEncryptor {
    fun encrypt(plaintext: ByteArray): ByteArray
}
