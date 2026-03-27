package org.github.keepasscompose.core.autotype

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class KeyboardSimulatorTest {
    @Test
    fun autoTypeKeysHaveDistinctValues() {
        val keys = listOf(
            AutoTypeKey.TAB, AutoTypeKey.ENTER, AutoTypeKey.UP, AutoTypeKey.DOWN,
            AutoTypeKey.LEFT, AutoTypeKey.RIGHT, AutoTypeKey.HOME, AutoTypeKey.END,
            AutoTypeKey.DELETE, AutoTypeKey.BACKSPACE, AutoTypeKey.ESCAPE, AutoTypeKey.SPACE,
        )
        assertEquals(keys.size, keys.toSet().size, "All key codes should be unique")
    }

    @Test
    fun modifierKeysHaveDistinctValues() {
        val mods = listOf(AutoTypeKey.SHIFT, AutoTypeKey.CTRL, AutoTypeKey.ALT, AutoTypeKey.META)
        assertEquals(mods.size, mods.toSet().size, "All modifier codes should be unique")
    }

    @Test
    fun modifierKeysDoNotOverlapWithActionKeys() {
        val actionKeys = setOf(
            AutoTypeKey.TAB, AutoTypeKey.ENTER, AutoTypeKey.UP, AutoTypeKey.DOWN,
            AutoTypeKey.LEFT, AutoTypeKey.RIGHT, AutoTypeKey.HOME, AutoTypeKey.END,
            AutoTypeKey.DELETE, AutoTypeKey.BACKSPACE, AutoTypeKey.ESCAPE, AutoTypeKey.SPACE,
        )
        val modifiers = setOf(AutoTypeKey.SHIFT, AutoTypeKey.CTRL, AutoTypeKey.ALT, AutoTypeKey.META)
        val overlap = actionKeys.intersect(modifiers)
        assertEquals(0, overlap.size, "Modifiers and action keys should not overlap")
    }

    @Test
    fun keyboardSimulatorCanBeInstantiated() {
        val sim = KeyboardSimulator()
        // On CI/headless environments isAvailable may be false, that's OK
        // Just verify it doesn't throw
        val available = sim.isAvailable()
        // Can be true or false depending on environment — just verify no exception
        @Suppress("SENSELESS_COMPARISON")
        assertEquals(true, available == true || available == false)
    }
}
