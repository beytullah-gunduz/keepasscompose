package org.github.keepasscompose.core.browser

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Installs and uninstalls native messaging host manifests for browser extensions.
 *
 * Each browser has a specific directory where native messaging host manifests
 * must be placed. The manifest is a JSON file that tells the browser how to
 * launch the native messaging host application.
 *
 * Supported browsers: Chrome, Chromium, Firefox, Edge, Brave, Vivaldi.
 */
object NativeMessagingInstaller {
    const val HOST_NAME = "org.keepasscompose.browser"

    private val json = Json { prettyPrint = true }

    /**
     * Supported browser targets for manifest installation.
     */
    enum class Browser(val displayName: String) {
        CHROME("Google Chrome"),
        CHROMIUM("Chromium"),
        FIREFOX("Firefox"),
        EDGE("Microsoft Edge"),
        BRAVE("Brave"),
        VIVALDI("Vivaldi"),
    }

    /**
     * Install the native messaging host manifest for the given browsers.
     *
     * @param executablePath Path to the KeePass Compose executable.
     * @param extensionId The browser extension ID (for Chromium-based browsers).
     * @param browsers The set of browsers to install for.
     * @return Map of browser to installation result (success/failure message).
     */
    fun install(executablePath: String, extensionId: String, browsers: Set<Browser> = Browser.entries.toSet()): Map<Browser, Result<String>> =
        browsers.associateWith { browser ->
            try {
                val manifestDir = getManifestDirectory(browser)
                manifestDir.mkdirs()

                val manifestFile = File(manifestDir, "$HOST_NAME.json")
                val manifest = createManifest(executablePath, extensionId, browser)
                manifestFile.writeText(json.encodeToString(manifest))

                Result.success("Installed to ${manifestFile.absolutePath}")
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Uninstall the native messaging host manifest for the given browsers.
     */
    fun uninstall(browsers: Set<Browser> = Browser.entries.toSet()): Map<Browser, Result<String>> = browsers.associateWith { browser ->
        try {
            val manifestDir = getManifestDirectory(browser)
            val manifestFile = File(manifestDir, "$HOST_NAME.json")
            if (manifestFile.exists()) {
                manifestFile.delete()
                Result.success("Removed ${manifestFile.absolutePath}")
            } else {
                Result.success("Not installed")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Check which browsers have the manifest installed.
     */
    fun getInstalledBrowsers(): Set<Browser> = Browser.entries.filter { browser ->
        val manifestDir = getManifestDirectory(browser)
        File(manifestDir, "$HOST_NAME.json").exists()
    }.toSet()

    internal fun getManifestDirectory(browser: Browser): File {
        val home = System.getProperty("user.home")
        val os = System.getProperty("os.name").lowercase()

        return when {
            os.contains("linux") -> getLinuxManifestDir(browser, home)
            os.contains("mac") || os.contains("darwin") -> getMacManifestDir(browser, home)
            os.contains("win") -> getWindowsManifestDir(browser)
            else -> throw UnsupportedOperationException("Unsupported OS: $os")
        }
    }

    private fun getLinuxManifestDir(browser: Browser, home: String): File {
        val subdir = when (browser) {
            Browser.CHROME -> ".config/google-chrome/NativeMessagingHosts"
            Browser.CHROMIUM -> ".config/chromium/NativeMessagingHosts"
            Browser.FIREFOX -> ".mozilla/native-messaging-hosts"
            Browser.EDGE -> ".config/microsoft-edge/NativeMessagingHosts"
            Browser.BRAVE -> ".config/BraveSoftware/Brave-Browser/NativeMessagingHosts"
            Browser.VIVALDI -> ".config/vivaldi/NativeMessagingHosts"
        }
        return File(home, subdir)
    }

    private fun getMacManifestDir(browser: Browser, home: String): File {
        val subdir = when (browser) {
            Browser.CHROME -> "Library/Application Support/Google/Chrome/NativeMessagingHosts"
            Browser.CHROMIUM -> "Library/Application Support/Chromium/NativeMessagingHosts"
            Browser.FIREFOX -> "Library/Application Support/Mozilla/NativeMessagingHosts"
            Browser.EDGE -> "Library/Application Support/Microsoft Edge/NativeMessagingHosts"
            Browser.BRAVE -> "Library/Application Support/BraveSoftware/Brave-Browser/NativeMessagingHosts"
            Browser.VIVALDI -> "Library/Application Support/Vivaldi/NativeMessagingHosts"
        }
        return File(home, subdir)
    }

    private fun getWindowsManifestDir(browser: Browser): File {
        val appData = System.getenv("LOCALAPPDATA") ?: System.getProperty("user.home")
        val subdir = when (browser) {
            Browser.CHROME -> "Google/Chrome/User Data/NativeMessagingHosts"
            Browser.CHROMIUM -> "Chromium/User Data/NativeMessagingHosts"
            Browser.FIREFOX -> "Mozilla/NativeMessagingHosts"
            Browser.EDGE -> "Microsoft/Edge/User Data/NativeMessagingHosts"
            Browser.BRAVE -> "BraveSoftware/Brave-Browser/User Data/NativeMessagingHosts"
            Browser.VIVALDI -> "Vivaldi/User Data/NativeMessagingHosts"
        }
        return File(appData, subdir)
    }

    private fun createManifest(executablePath: String, extensionId: String, browser: Browser): NativeMessagingManifest =
        if (browser == Browser.FIREFOX) {
            NativeMessagingManifest(
                name = HOST_NAME,
                description = "KeePass Compose native messaging host",
                path = executablePath,
                type = "stdio",
                allowedExtensions = listOf(extensionId),
                allowedOrigins = null,
            )
        } else {
            NativeMessagingManifest(
                name = HOST_NAME,
                description = "KeePass Compose native messaging host",
                path = executablePath,
                type = "stdio",
                allowedOrigins = listOf("chrome-extension://$extensionId/"),
                allowedExtensions = null,
            )
        }

    @Serializable
    internal data class NativeMessagingManifest(
        val name: String,
        val description: String,
        val path: String,
        val type: String,
        val allowedOrigins: List<String>? = null,
        val allowedExtensions: List<String>? = null,
    )
}
