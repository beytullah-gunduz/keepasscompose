package org.github.keepasscompose.core.common

import io.github.aakira.napier.Napier
import java.io.File

/**
 * Desktop file association support for `.kdbx` files.
 *
 * On Linux, creates/removes a `.desktop` entry and registers the MIME type.
 * On Windows, describes the registry operations needed (actual registry writes
 * require elevated privileges and are left as TODOs).
 */
object FileAssociation {
    private const val TAG = "FileAssociation"
    private const val DESKTOP_FILE_NAME = "keepasscompose.desktop"
    private const val MIME_TYPE = "application/x-keepass"

    private val isLinux: Boolean
        get() = System.getProperty("os.name").lowercase().contains("linux")

    private val isWindows: Boolean
        get() = System.getProperty("os.name").lowercase().contains("windows")

    /**
     * Registers `.kdbx` file association for the current platform.
     *
     * @param executablePath absolute path to the application executable / launcher script
     */
    fun registerKdbxAssociation(executablePath: String) {
        when {
            isLinux -> registerLinux(executablePath)
            isWindows -> registerWindows(executablePath)
            else -> Napier.w(tag = TAG) { "File association not supported on this platform" }
        }
    }

    /** Removes the `.kdbx` file association on the current platform. */
    fun unregisterKdbxAssociation() {
        when {
            isLinux -> unregisterLinux()
            isWindows -> unregisterWindows()
            else -> Napier.w(tag = TAG) { "File association not supported on this platform" }
        }
    }

    /** Returns `true` when a `.kdbx` association is currently registered. */
    fun isAssociated(): Boolean = when {
        isLinux -> desktopFile().exists()

        isWindows -> {
            // TODO: Check HKEY_CURRENT_USER\Software\Classes\.kdbx registry key
            false
        }

        else -> false
    }

    /**
     * Extracts a `.kdbx` file path from command-line arguments, if present.
     *
     * @return the first argument that ends with `.kdbx` (case-insensitive), or `null`
     */
    fun parseCommandLineArgs(args: Array<String>): String? = args.firstOrNull { it.endsWith(".kdbx", ignoreCase = true) }

    // ── Linux ───────────────────────────────────────────────────────────

    private fun desktopFile(): File {
        val applicationsDir =
            File(System.getProperty("user.home"), ".local/share/applications")
        return File(applicationsDir, DESKTOP_FILE_NAME)
    }

    private fun registerLinux(executablePath: String) {
        try {
            val file = desktopFile()
            file.parentFile.mkdirs()
            file.writeText(
                buildString {
                    appendLine("[Desktop Entry]")
                    appendLine("Type=Application")
                    appendLine("Name=KeePass Compose")
                    appendLine("Comment=KeePass-compatible password manager")
                    appendLine("Exec=$executablePath %f")
                    appendLine("MimeType=$MIME_TYPE;")
                    appendLine("Categories=Utility;Security;")
                    appendLine("Terminal=false")
                },
            )
            // Update the MIME database so the desktop environment picks up the association
            Runtime.getRuntime().exec(arrayOf("update-desktop-database", file.parent))
            Napier.d(tag = TAG) { "Linux .desktop entry created at ${file.absolutePath}" }
        } catch (e: Exception) {
            Napier.e(tag = TAG, throwable = e) { "Failed to register Linux file association" }
        }
    }

    private fun unregisterLinux() {
        try {
            val file = desktopFile()
            if (file.delete()) {
                Runtime.getRuntime().exec(arrayOf("update-desktop-database", file.parent))
                Napier.d(tag = TAG) { "Linux .desktop entry removed" }
            }
        } catch (e: Exception) {
            Napier.e(tag = TAG, throwable = e) { "Failed to unregister Linux file association" }
        }
    }

    // ── Windows ─────────────────────────────────────────────────────────

    @Suppress("UnusedParameter")
    private fun registerWindows(executablePath: String) {
        // TODO: Write HKEY_CURRENT_USER\Software\Classes\.kdbx with default value "KeePassCompose.kdbx"
        // TODO: Write HKEY_CURRENT_USER\Software\Classes\KeePassCompose.kdbx\shell\open\command
        //       with default value "<executablePath> "%1""
        Napier.w(tag = TAG) { "Windows registry association requires elevated privileges (not yet implemented)" }
    }

    private fun unregisterWindows() {
        // TODO: Delete HKEY_CURRENT_USER\Software\Classes\.kdbx
        // TODO: Delete HKEY_CURRENT_USER\Software\Classes\KeePassCompose.kdbx
        Napier.w(tag = TAG) { "Windows registry removal requires elevated privileges (not yet implemented)" }
    }
}
