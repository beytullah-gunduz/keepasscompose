package org.github.keepasscompose.core.autotype

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HotkeyDefinitionTest {
    @Test
    fun defaultAutoTypeHotkeyIsCtrlAltA() {
        val hotkey = HotkeyDefinition.AUTO_TYPE_DEFAULT
        assertEquals('A'.code, hotkey.keyCode)
        assertTrue(hotkey.ctrl)
        assertTrue(hotkey.alt)
        assertFalse(hotkey.shift)
        assertFalse(hotkey.meta)
    }

    @Test
    fun parseCtrlAltA() {
        val hotkey = HotkeyDefinition.parse("Ctrl+Alt+A")
        assertNotNull(hotkey)
        assertEquals('A'.code, hotkey.keyCode)
        assertTrue(hotkey.ctrl)
        assertTrue(hotkey.alt)
    }

    @Test
    fun parseShiftCtrlB() {
        val hotkey = HotkeyDefinition.parse("Shift+Ctrl+B")
        assertNotNull(hotkey)
        assertEquals('B'.code, hotkey.keyCode)
        assertTrue(hotkey.shift)
        assertTrue(hotkey.ctrl)
        assertFalse(hotkey.alt)
    }

    @Test
    fun parseSingleKey() {
        val hotkey = HotkeyDefinition.parse("A")
        assertNotNull(hotkey)
        assertEquals('A'.code, hotkey.keyCode)
        assertFalse(hotkey.ctrl)
        assertFalse(hotkey.alt)
    }

    @Test
    fun parseFunctionKey() {
        val hotkey = HotkeyDefinition.parse("Ctrl+F12")
        assertNotNull(hotkey)
        assertEquals(123, hotkey.keyCode) // F12 = 111 + 12
        assertTrue(hotkey.ctrl)
    }

    @Test
    fun parseCaseInsensitive() {
        val hotkey = HotkeyDefinition.parse("ctrl+alt+a")
        assertNotNull(hotkey)
        assertTrue(hotkey.ctrl)
        assertTrue(hotkey.alt)
    }

    @Test
    fun parseMetaVariants() {
        val win = HotkeyDefinition.parse("Win+A")
        assertNotNull(win)
        assertTrue(win.meta)

        val cmd = HotkeyDefinition.parse("Cmd+A")
        assertNotNull(cmd)
        assertTrue(cmd.meta)
    }

    @Test
    fun parseBlankReturnsNull() {
        assertNull(HotkeyDefinition.parse(""))
        assertNull(HotkeyDefinition.parse("   "))
    }

    @Test
    fun toStringFormatsCorrectly() {
        val hotkey = HotkeyDefinition(keyCode = 'A'.code, ctrl = true, alt = true)
        assertEquals("Ctrl+Alt+A", hotkey.toString())
    }

    @Test
    fun toStringWithShiftMeta() {
        val hotkey = HotkeyDefinition(keyCode = 'Z'.code, shift = true, meta = true)
        assertEquals("Shift+Meta+Z", hotkey.toString())
    }
}
