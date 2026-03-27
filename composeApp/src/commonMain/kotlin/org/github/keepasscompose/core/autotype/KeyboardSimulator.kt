package org.github.keepasscompose.core.autotype

/**
 * Platform-abstracted keyboard simulation for auto-type.
 *
 * Simulates key presses, releases, and typed text as if the user were
 * physically typing on the keyboard. Each platform provides its own
 * implementation:
 * - **Desktop (JVM)**: Uses `java.awt.Robot` for native key simulation
 * - **Android**: Uses `AccessibilityService` for input injection
 * - **iOS**: Stub (auto-type not supported on iOS)
 */
expect class KeyboardSimulator() {
    /**
     * Whether keyboard simulation is available on this platform.
     */
    fun isAvailable(): Boolean

    /**
     * Type a string of characters, simulating individual key presses.
     * Each character is typed with a configurable inter-key delay.
     *
     * @param text The text to type.
     * @param interKeyDelayMs Delay between key presses in milliseconds.
     */
    suspend fun typeText(text: String, interKeyDelayMs: Long = 10)

    /**
     * Press and release a single key by its virtual key code.
     *
     * @param keyCode Platform-specific virtual key code.
     */
    suspend fun pressKey(keyCode: Int)

    /**
     * Press and release a key combination (e.g., Ctrl+A).
     *
     * @param modifiers List of modifier key codes (e.g., Ctrl, Alt, Shift).
     * @param keyCode The main key code.
     */
    suspend fun pressKeyCombo(modifiers: List<Int>, keyCode: Int)

    /**
     * Simulate pressing the Tab key.
     */
    suspend fun pressTab()

    /**
     * Simulate pressing the Enter/Return key.
     */
    suspend fun pressEnter()
}

/**
 * Platform-independent virtual key codes for common auto-type actions.
 * Platform implementations map these to native key codes.
 */
object AutoTypeKey {
    const val TAB = 1
    const val ENTER = 2
    const val UP = 3
    const val DOWN = 4
    const val LEFT = 5
    const val RIGHT = 6
    const val HOME = 7
    const val END = 8
    const val DELETE = 9
    const val BACKSPACE = 10
    const val ESCAPE = 11
    const val SPACE = 12

    // Modifiers
    const val SHIFT = 100
    const val CTRL = 101
    const val ALT = 102
    const val META = 103 // Windows/Command key
}
