package org.github.keepasscompose.core.autotype

/**
 * iOS keyboard simulation stub.
 *
 * iOS does not support programmatic keyboard simulation from
 * regular apps. Auto-type is not available on this platform.
 */
actual class KeyboardSimulator actual constructor() {
    actual fun isAvailable(): Boolean = false

    actual suspend fun typeText(text: String, interKeyDelayMs: Long) {
        // Not available on iOS
    }

    actual suspend fun pressKey(keyCode: Int) {
        // Not available on iOS
    }

    actual suspend fun pressKeyCombo(modifiers: List<Int>, keyCode: Int) {
        // Not available on iOS
    }

    actual suspend fun pressTab() {
        // Not available on iOS
    }

    actual suspend fun pressEnter() {
        // Not available on iOS
    }
}
