package org.github.keepasscompose.core.common

import io.github.aakira.napier.Napier
import java.awt.MenuItem
import java.awt.PopupMenu
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.image.BufferedImage

/**
 * Desktop system tray integration.
 *
 * Provides a tray icon with a context menu that allows users to open the app,
 * lock the database, or exit, even when the main window is minimized.
 */
object SystemTrayManager {
    private const val TAG = "SystemTray"
    private var trayIcon: TrayIcon? = null

    /** Returns `true` when the platform supports a system tray. */
    fun isSupported(): Boolean = SystemTray.isSupported()

    /**
     * Shows a tray icon with the given [tooltip] and context-menu actions.
     *
     * @param tooltip  text shown on hover
     * @param onOpen   callback when "Open KeePass Compose" is clicked
     * @param onLock   callback when "Lock Database" is clicked
     * @param onExit   callback when "Exit" is clicked
     */
    fun show(tooltip: String, onOpen: () -> Unit, onLock: () -> Unit, onExit: () -> Unit) {
        if (!isSupported()) {
            Napier.w(tag = TAG) { "System tray is not supported on this platform" }
            return
        }

        // Remove existing icon before adding a new one
        hide()

        val popup = PopupMenu()

        val openItem = MenuItem("Open KeePass Compose")
        openItem.addActionListener { onOpen() }
        popup.add(openItem)

        val lockItem = MenuItem("Lock Database")
        lockItem.addActionListener { onLock() }
        popup.add(lockItem)

        popup.addSeparator()

        val exitItem = MenuItem("Exit")
        exitItem.addActionListener { onExit() }
        popup.add(exitItem)

        // Create a simple default icon (16x16 filled square)
        val image = BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()
        g.color = java.awt.Color(0x42, 0x85, 0xF4) // Material Blue
        g.fillRoundRect(0, 0, 16, 16, 4, 4)
        g.dispose()

        val icon = TrayIcon(image, tooltip, popup)
        icon.isImageAutoSize = true
        icon.addActionListener { onOpen() }

        try {
            SystemTray.getSystemTray().add(icon)
            trayIcon = icon
            Napier.d(tag = TAG) { "System tray icon shown" }
        } catch (e: Exception) {
            Napier.e(tag = TAG, throwable = e) { "Failed to add tray icon" }
        }
    }

    /** Removes the tray icon if it is currently shown. */
    fun hide() {
        trayIcon?.let { icon ->
            try {
                SystemTray.getSystemTray().remove(icon)
                Napier.d(tag = TAG) { "System tray icon hidden" }
            } catch (e: Exception) {
                Napier.e(tag = TAG, throwable = e) { "Failed to remove tray icon" }
            }
            trayIcon = null
        }
    }

    /** Updates the tooltip text of the current tray icon. */
    fun updateTooltip(text: String) {
        trayIcon?.toolTip = text
    }
}
