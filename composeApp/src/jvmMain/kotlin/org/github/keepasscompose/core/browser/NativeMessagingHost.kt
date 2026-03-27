package org.github.keepasscompose.core.browser

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Native messaging host for browser extension communication on Desktop (JVM).
 *
 * Implements the Chrome Native Messaging protocol: each message is prefixed
 * with a 4-byte little-endian unsigned integer indicating the message length,
 * followed by the UTF-8 JSON message body.
 *
 * This host listens on a given [InputStream] (typically stdin when launched
 * by the browser) and writes responses to [OutputStream] (stdout). Messages
 * are forwarded to the [BrowserProtocolHandler] for processing.
 *
 * @param protocolHandler The protocol handler to process messages.
 * @param scope The coroutine scope for the message loop.
 */
class NativeMessagingHost(private val protocolHandler: BrowserProtocolHandler, private val scope: CoroutineScope) {
    companion object {
        const val MAX_MESSAGE_SIZE = 1024 * 1024 // 1 MB (Chrome limit)
    }

    private var messageLoopJob: Job? = null

    /**
     * Start listening for messages on the given streams.
     * Typically called with System.`in` and System.out when launched by a browser.
     */
    fun start(input: InputStream = System.`in`, output: OutputStream = System.out) {
        messageLoopJob = scope.launch(Dispatchers.IO) {
            try {
                messageLoop(input, output)
            } catch (_: CancellationException) {
                // Normal shutdown
            }
        }
    }

    /**
     * Stop the message loop.
     */
    fun stop() {
        messageLoopJob?.cancel()
        messageLoopJob = null
    }

    /**
     * Whether the host is currently running.
     */
    val isRunning: Boolean get() = messageLoopJob?.isActive == true

    private suspend fun messageLoop(input: InputStream, output: OutputStream) {
        withContext(Dispatchers.IO) {
            while (isActive) {
                val message = readMessage(input) ?: break
                val response = protocolHandler.handleMessage(message)
                writeMessage(output, response)
            }
        }
    }

    /**
     * Read a single native message from the input stream.
     * Returns null if the stream is closed or an error occurs.
     */
    internal fun readMessage(input: InputStream): String? {
        // Read 4-byte length header (little-endian)
        val lengthBytes = ByteArray(4)
        var totalRead = 0
        while (totalRead < 4) {
            val read = input.read(lengthBytes, totalRead, 4 - totalRead)
            if (read == -1) return null
            totalRead += read
        }

        val length = ByteBuffer.wrap(lengthBytes).order(ByteOrder.LITTLE_ENDIAN).int
        if (length <= 0 || length > MAX_MESSAGE_SIZE) return null

        // Read the message body
        val messageBytes = ByteArray(length)
        totalRead = 0
        while (totalRead < length) {
            val read = input.read(messageBytes, totalRead, length - totalRead)
            if (read == -1) return null
            totalRead += read
        }

        return String(messageBytes, Charsets.UTF_8)
    }

    /**
     * Write a native message to the output stream.
     */
    internal fun writeMessage(output: OutputStream, message: String) {
        val messageBytes = message.toByteArray(Charsets.UTF_8)
        val lengthBytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(messageBytes.size).array()
        output.write(lengthBytes)
        output.write(messageBytes)
        output.flush()
    }
}
