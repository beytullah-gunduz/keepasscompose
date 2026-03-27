package org.github.keepasscompose.core.autotype

actual class GlobalHotkeyListener actual constructor() {
    actual fun isAvailable(): Boolean = false
    actual fun register(hotkey: HotkeyDefinition, callback: () -> Unit) {}
    actual fun unregisterAll() {}
    actual fun start() {}
    actual fun stop() {}
}
