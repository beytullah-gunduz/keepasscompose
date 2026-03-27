package org.github.keepasscompose.core.autotype

/**
 * Platform-abstracted global hotkey listener for triggering auto-type.
 *
 * Registers system-wide hotkeys that work even when the application is
 * not focused. The default hotkey for auto-type is typically Ctrl+Alt+A
 * (configurable).
 *
 * - **Desktop (JVM)**: Uses JNativeHook or AWT global key listener
 * - **Android/iOS**: Not supported (returns [isAvailable] = false)
 */
expect class GlobalHotkeyListener() {
    /**
     * Whether global hotkey listening is available on this platform.
     */
    fun isAvailable(): Boolean

    /**
     * Register a hotkey that triggers the given callback when pressed.
     *
     * @param hotkey The hotkey definition.
     * @param callback The function to invoke when the hotkey is pressed.
     */
    fun register(hotkey: HotkeyDefinition, callback: () -> Unit)

    /**
     * Unregister all hotkeys and stop listening.
     */
    fun unregisterAll()

    /**
     * Start listening for registered hotkeys.
     */
    fun start()

    /**
     * Stop listening for hotkeys.
     */
    fun stop()
}

/**
 * Defines a hotkey combination.
 */
data class HotkeyDefinition(
    val keyCode: Int,
    val ctrl: Boolean = false,
    val alt: Boolean = false,
    val shift: Boolean = false,
    val meta: Boolean = false,
) {
    companion object {
        /**
         * Default auto-type hotkey: Ctrl+Alt+A
         */
        val AUTO_TYPE_DEFAULT = HotkeyDefinition(
            keyCode = 'A'.code,
            ctrl = true,
            alt = true,
        )

        /**
         * Parse a hotkey string like "Ctrl+Alt+A" into a HotkeyDefinition.
         */
        fun parse(hotkeyString: String): HotkeyDefinition? {
            if (hotkeyString.isBlank()) return null

            val parts = hotkeyString.split("+").map { it.trim().lowercase() }
            if (parts.isEmpty()) return null

            val keyPart = parts.last()
            val keyCode = when {
                keyPart.length == 1 -> keyPart[0].uppercaseChar().code

                keyPart.startsWith("f") && keyPart.substring(1).toIntOrNull() != null -> {
                    111 + (keyPart.substring(1).toInt()) // F1=112, F2=113, etc.
                }

                else -> return null
            }

            return HotkeyDefinition(
                keyCode = keyCode,
                ctrl = parts.any { it == "ctrl" || it == "control" },
                alt = parts.any { it == "alt" },
                shift = parts.any { it == "shift" },
                meta = parts.any { it == "meta" || it == "win" || it == "cmd" || it == "command" },
            )
        }
    }

    override fun toString(): String {
        val parts = mutableListOf<String>()
        if (ctrl) parts.add("Ctrl")
        if (alt) parts.add("Alt")
        if (shift) parts.add("Shift")
        if (meta) parts.add("Meta")
        parts.add(keyCode.toChar().toString())
        return parts.joinToString("+")
    }
}
