package org.github.keepasscompose.core.common

import io.github.aakira.napier.Napier
import java.awt.Dimension
import java.awt.Window
import javax.imageio.ImageIO
import javax.swing.JFrame

/**
 * Utility for configuring native window properties on desktop.
 *
 * Wraps [java.awt.Window] and related APIs to provide a simple interface for
 * setting title, minimum size, icon, and always-on-top behaviour.
 */
object WindowDecorations {
    private const val TAG = "WindowDecorations"

    /** Sets the title of the given [window] (only effective for [JFrame]). */
    fun setTitle(window: Window, title: String) {
        if (window is JFrame) {
            window.title = title
        } else {
            Napier.w(tag = TAG) { "setTitle is only supported on JFrame windows" }
        }
    }

    /** Sets the minimum size of the given [window]. */
    fun setMinSize(window: Window, width: Int, height: Int) {
        window.minimumSize = Dimension(width, height)
    }

    /**
     * Sets the icon of the given [window] from a classpath resource path.
     *
     * @param window   the target window
     * @param iconPath classpath resource path (e.g. "/icon.png")
     */
    fun setIcon(window: Window, iconPath: String) {
        try {
            val stream = WindowDecorations::class.java.getResourceAsStream(iconPath)
            if (stream != null) {
                window.iconImages = listOf(ImageIO.read(stream))
                Napier.d(tag = TAG) { "Window icon set from $iconPath" }
            } else {
                Napier.w(tag = TAG) { "Icon resource not found: $iconPath" }
            }
        } catch (e: Exception) {
            Napier.e(tag = TAG, throwable = e) { "Failed to set window icon" }
        }
    }

    /** Sets whether the given [window] should stay above other windows. */
    fun setAlwaysOnTop(window: Window, onTop: Boolean) {
        window.isAlwaysOnTop = onTop
    }
}
