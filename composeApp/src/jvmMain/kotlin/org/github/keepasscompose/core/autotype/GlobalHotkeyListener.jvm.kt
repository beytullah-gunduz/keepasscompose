package org.github.keepasscompose.core.autotype

import java.awt.event.KeyEvent

/**
 * Desktop global hotkey listener using AWT KeyEventDispatcher.
 *
 * Registers a global key event dispatcher with the default KeyboardFocusManager
 * to capture hotkeys even when the application window doesn't have focus
 * (requires the JVM process to be running).
 *
 * For truly system-wide hotkeys (capturing keys from other applications),
 * a native library like JNativeHook would be needed. This implementation
 * covers the in-app global hotkey case.
 */
actual class GlobalHotkeyListener actual constructor() {
    private data class Registration(val hotkey: HotkeyDefinition, val callback: () -> Unit)

    private val registrations = mutableListOf<Registration>()
    private var dispatcher: java.awt.KeyEventDispatcher? = null
    private var running = false

    actual fun isAvailable(): Boolean = true

    actual fun register(hotkey: HotkeyDefinition, callback: () -> Unit) {
        registrations.add(Registration(hotkey, callback))
    }

    actual fun unregisterAll() {
        registrations.clear()
        stop()
    }

    actual fun start() {
        if (running) return
        running = true

        dispatcher = java.awt.KeyEventDispatcher { event ->
            if (event.id == KeyEvent.KEY_PRESSED) {
                for (reg in registrations) {
                    if (matchesHotkey(event, reg.hotkey)) {
                        reg.callback()
                        return@KeyEventDispatcher true
                    }
                }
            }
            false
        }

        try {
            java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .addKeyEventDispatcher(dispatcher)
        } catch (_: Exception) {
            // May fail in headless environments
            running = false
        }
    }

    actual fun stop() {
        if (!running) return
        running = false

        dispatcher?.let { d ->
            try {
                java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager()
                    .removeKeyEventDispatcher(d)
            } catch (_: Exception) {
                // Ignore cleanup errors
            }
        }
        dispatcher = null
    }

    private fun matchesHotkey(event: KeyEvent, hotkey: HotkeyDefinition): Boolean {
        if (event.keyCode != mapKeyCode(hotkey.keyCode)) return false
        if (hotkey.ctrl != event.isControlDown) return false
        if (hotkey.alt != event.isAltDown) return false
        if (hotkey.shift != event.isShiftDown) return false
        if (hotkey.meta != event.isMetaDown) return false
        return true
    }

    private fun mapKeyCode(code: Int): Int {
        // HotkeyDefinition uses char codes for letters
        return if (code in 'A'.code..'Z'.code) {
            KeyEvent.VK_A + (code - 'A'.code)
        } else {
            code
        }
    }
}
