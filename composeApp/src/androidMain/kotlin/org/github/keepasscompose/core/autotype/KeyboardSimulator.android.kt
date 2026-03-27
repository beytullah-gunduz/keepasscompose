package org.github.keepasscompose.core.autotype

/**
 * Android keyboard simulation stub.
 *
 * Full auto-type on Android requires an AccessibilityService, which
 * is implemented separately in the Android autofill infrastructure.
 * This stub returns [isAvailable] = false to indicate that direct
 * keyboard simulation is not supported in the shared layer.
 */
actual class KeyboardSimulator actual constructor() {
    actual fun isAvailable(): Boolean = false

    actual suspend fun typeText(text: String, interKeyDelayMs: Long) {
        // Not available on Android — use AccessibilityService instead
    }

    actual suspend fun pressKey(keyCode: Int) {
        // Not available on Android
    }

    actual suspend fun pressKeyCombo(modifiers: List<Int>, keyCode: Int) {
        // Not available on Android
    }

    actual suspend fun pressTab() {
        // Not available on Android
    }

    actual suspend fun pressEnter() {
        // Not available on Android
    }
}
