package org.github.keepasscompose.core.autotype

import kotlinx.coroutines.delay
import java.awt.Robot
import java.awt.event.KeyEvent

actual class KeyboardSimulator actual constructor() {
    private val robot: Robot? = try {
        Robot().also { it.autoDelay = 0 }
    } catch (_: Exception) {
        null
    }

    actual fun isAvailable(): Boolean = robot != null

    actual suspend fun typeText(text: String, interKeyDelayMs: Long) {
        val r = robot ?: return
        for (char in text) {
            typeChar(r, char)
            if (interKeyDelayMs > 0) delay(interKeyDelayMs)
        }
    }

    actual suspend fun pressKey(keyCode: Int) {
        val r = robot ?: return
        val nativeCode = mapToNativeKeyCode(keyCode)
        r.keyPress(nativeCode)
        r.keyRelease(nativeCode)
    }

    actual suspend fun pressKeyCombo(modifiers: List<Int>, keyCode: Int) {
        val r = robot ?: return
        val nativeMods = modifiers.map { mapToNativeKeyCode(it) }
        val nativeKey = mapToNativeKeyCode(keyCode)

        nativeMods.forEach { r.keyPress(it) }
        r.keyPress(nativeKey)
        r.keyRelease(nativeKey)
        nativeMods.reversed().forEach { r.keyRelease(it) }
    }

    actual suspend fun pressTab() {
        val r = robot ?: return
        r.keyPress(KeyEvent.VK_TAB)
        r.keyRelease(KeyEvent.VK_TAB)
    }

    actual suspend fun pressEnter() {
        val r = robot ?: return
        r.keyPress(KeyEvent.VK_ENTER)
        r.keyRelease(KeyEvent.VK_ENTER)
    }

    private fun typeChar(robot: Robot, char: Char) {
        val keyCode = KeyEvent.getExtendedKeyCodeForChar(char.code)
        if (keyCode == KeyEvent.VK_UNDEFINED) return

        val needsShift = char.isUpperCase() || char in SHIFT_CHARS
        if (needsShift) robot.keyPress(KeyEvent.VK_SHIFT)
        robot.keyPress(keyCode)
        robot.keyRelease(keyCode)
        if (needsShift) robot.keyRelease(KeyEvent.VK_SHIFT)
    }

    private fun mapToNativeKeyCode(autoTypeKey: Int): Int = when (autoTypeKey) {
        AutoTypeKey.TAB -> KeyEvent.VK_TAB
        AutoTypeKey.ENTER -> KeyEvent.VK_ENTER
        AutoTypeKey.UP -> KeyEvent.VK_UP
        AutoTypeKey.DOWN -> KeyEvent.VK_DOWN
        AutoTypeKey.LEFT -> KeyEvent.VK_LEFT
        AutoTypeKey.RIGHT -> KeyEvent.VK_RIGHT
        AutoTypeKey.HOME -> KeyEvent.VK_HOME
        AutoTypeKey.END -> KeyEvent.VK_END
        AutoTypeKey.DELETE -> KeyEvent.VK_DELETE
        AutoTypeKey.BACKSPACE -> KeyEvent.VK_BACK_SPACE
        AutoTypeKey.ESCAPE -> KeyEvent.VK_ESCAPE
        AutoTypeKey.SPACE -> KeyEvent.VK_SPACE
        AutoTypeKey.SHIFT -> KeyEvent.VK_SHIFT
        AutoTypeKey.CTRL -> KeyEvent.VK_CONTROL
        AutoTypeKey.ALT -> KeyEvent.VK_ALT
        AutoTypeKey.META -> KeyEvent.VK_META
        else -> autoTypeKey // Pass through native key codes
    }

    companion object {
        private val SHIFT_CHARS = setOf(
            '!', '@', '#', '$', '%', '^', '&', '*', '(', ')',
            '_', '+', '{', '}', '|', ':', '"', '<', '>', '?', '~',
        )
    }
}
