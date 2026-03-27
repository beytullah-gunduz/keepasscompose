package org.github.keepasscompose.core.autotype

import org.github.keepasscompose.core.autotype.AutoTypeSequenceParser.Action
import org.github.keepasscompose.core.model.KdbxEntry
import org.github.keepasscompose.core.model.KdbxEntryField
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AutoTypeSequenceParserTest {
    // --- Default sequence ---

    @Test
    fun defaultSequenceConstant() {
        assertEquals("{USERNAME}{TAB}{PASSWORD}{ENTER}", AutoTypeSequenceParser.DEFAULT_SEQUENCE)
    }

    @Test
    fun blankInputUsesDefault() {
        val actions = AutoTypeSequenceParser.parse("")
        assertEquals(4, actions.size)
        assertTrue(actions[0] is Action.TypeField)
        assertTrue(actions[1] is Action.PressKey)
        assertTrue(actions[2] is Action.TypeField)
        assertTrue(actions[3] is Action.PressKey)
    }

    // --- Field references ---

    @Test
    fun parsesUsername() {
        val actions = AutoTypeSequenceParser.parse("{USERNAME}")
        assertEquals(1, actions.size)
        assertEquals(Action.TypeField("UserName"), actions[0])
    }

    @Test
    fun parsesPassword() {
        val actions = AutoTypeSequenceParser.parse("{PASSWORD}")
        assertEquals(1, actions.size)
        assertEquals(Action.TypeField("Password"), actions[0])
    }

    @Test
    fun parsesUrl() {
        val actions = AutoTypeSequenceParser.parse("{URL}")
        assertEquals(1, actions.size)
        assertEquals(Action.TypeField("URL"), actions[0])
    }

    @Test
    fun parsesTitle() {
        val actions = AutoTypeSequenceParser.parse("{TITLE}")
        assertEquals(1, actions.size)
        assertEquals(Action.TypeField("Title"), actions[0])
    }

    @Test
    fun parsesNotes() {
        val actions = AutoTypeSequenceParser.parse("{NOTES}")
        assertEquals(1, actions.size)
        assertEquals(Action.TypeField("Notes"), actions[0])
    }

    // --- Special keys ---

    @Test
    fun parsesTab() {
        val actions = AutoTypeSequenceParser.parse("{TAB}")
        assertEquals(1, actions.size)
        assertEquals(Action.PressKey(AutoTypeKey.TAB), actions[0])
    }

    @Test
    fun parsesEnter() {
        val actions = AutoTypeSequenceParser.parse("{ENTER}")
        assertEquals(1, actions.size)
        assertEquals(Action.PressKey(AutoTypeKey.ENTER), actions[0])
    }

    @Test
    fun parsesSpace() {
        val actions = AutoTypeSequenceParser.parse("{SPACE}")
        assertEquals(Action.PressKey(AutoTypeKey.SPACE), actions[0])
    }

    @Test
    fun parsesBackspace() {
        val actions = AutoTypeSequenceParser.parse("{BACKSPACE}")
        assertEquals(Action.PressKey(AutoTypeKey.BACKSPACE), actions[0])
    }

    @Test
    fun parsesBsAlias() {
        val actions = AutoTypeSequenceParser.parse("{BS}")
        assertEquals(Action.PressKey(AutoTypeKey.BACKSPACE), actions[0])
    }

    @Test
    fun parsesDelete() {
        val actions = AutoTypeSequenceParser.parse("{DELETE}")
        assertEquals(Action.PressKey(AutoTypeKey.DELETE), actions[0])
    }

    @Test
    fun parsesDelAlias() {
        val actions = AutoTypeSequenceParser.parse("{DEL}")
        assertEquals(Action.PressKey(AutoTypeKey.DELETE), actions[0])
    }

    @Test
    fun parsesEsc() {
        val actions = AutoTypeSequenceParser.parse("{ESC}")
        assertEquals(Action.PressKey(AutoTypeKey.ESCAPE), actions[0])
    }

    @Test
    fun parsesArrowKeys() {
        assertEquals(Action.PressKey(AutoTypeKey.UP), AutoTypeSequenceParser.parse("{UP}")[0])
        assertEquals(Action.PressKey(AutoTypeKey.DOWN), AutoTypeSequenceParser.parse("{DOWN}")[0])
        assertEquals(Action.PressKey(AutoTypeKey.LEFT), AutoTypeSequenceParser.parse("{LEFT}")[0])
        assertEquals(Action.PressKey(AutoTypeKey.RIGHT), AutoTypeSequenceParser.parse("{RIGHT}")[0])
    }

    @Test
    fun parsesHomeEnd() {
        assertEquals(Action.PressKey(AutoTypeKey.HOME), AutoTypeSequenceParser.parse("{HOME}")[0])
        assertEquals(Action.PressKey(AutoTypeKey.END), AutoTypeSequenceParser.parse("{END}")[0])
    }

    // --- Tilde as Enter ---

    @Test
    fun tildeActsAsEnter() {
        val actions = AutoTypeSequenceParser.parse("~")
        assertEquals(1, actions.size)
        assertEquals(Action.PressKey(AutoTypeKey.ENTER), actions[0])
    }

    // --- Delay ---

    @Test
    fun parsesDelay() {
        val actions = AutoTypeSequenceParser.parse("{DELAY 500}")
        assertEquals(1, actions.size)
        assertEquals(Action.Delay(500), actions[0])
    }

    @Test
    fun parsesDelayWithEquals() {
        val actions = AutoTypeSequenceParser.parse("{DELAY=1000}")
        assertEquals(1, actions.size)
        assertEquals(Action.Delay(1000), actions[0])
    }

    // --- Literal special characters ---

    @Test
    fun parsesLiteralPlus() {
        val actions = AutoTypeSequenceParser.parse("{+}")
        assertEquals(Action.TypeText("+"), actions[0])
    }

    @Test
    fun parsesLiteralCaret() {
        val actions = AutoTypeSequenceParser.parse("{^}")
        assertEquals(Action.TypeText("^"), actions[0])
    }

    @Test
    fun parsesLiteralPercent() {
        val actions = AutoTypeSequenceParser.parse("{%}")
        assertEquals(Action.TypeText("%"), actions[0])
    }

    @Test
    fun parsesLiteralTilde() {
        val actions = AutoTypeSequenceParser.parse("{~}")
        assertEquals(Action.TypeText("~"), actions[0])
    }

    // --- Custom field ---

    @Test
    fun parsesCustomField() {
        val actions = AutoTypeSequenceParser.parse("{S:MyField}")
        assertEquals(1, actions.size)
        assertEquals(Action.TypeCustomField("MyField"), actions[0])
    }

    // --- Literal text ---

    @Test
    fun parsesLiteralText() {
        val actions = AutoTypeSequenceParser.parse("hello")
        assertEquals(1, actions.size)
        assertEquals(Action.TypeText("hello"), actions[0])
    }

    // --- Complex sequences ---

    @Test
    fun parsesDefaultSequenceCorrectly() {
        val actions = AutoTypeSequenceParser.parse("{USERNAME}{TAB}{PASSWORD}{ENTER}")
        assertEquals(4, actions.size)
        assertEquals(Action.TypeField("UserName"), actions[0])
        assertEquals(Action.PressKey(AutoTypeKey.TAB), actions[1])
        assertEquals(Action.TypeField("Password"), actions[2])
        assertEquals(Action.PressKey(AutoTypeKey.ENTER), actions[3])
    }

    @Test
    fun parsesMixedSequence() {
        val actions = AutoTypeSequenceParser.parse("admin{TAB}p@ss{ENTER}")
        assertEquals(4, actions.size)
        assertEquals(Action.TypeText("admin"), actions[0])
        assertEquals(Action.PressKey(AutoTypeKey.TAB), actions[1])
        assertEquals(Action.TypeText("p@ss"), actions[2])
        assertEquals(Action.PressKey(AutoTypeKey.ENTER), actions[3])
    }

    @Test
    fun parsesSequenceWithDelay() {
        val actions = AutoTypeSequenceParser.parse("{USERNAME}{TAB}{DELAY 200}{PASSWORD}{ENTER}")
        assertEquals(5, actions.size)
        assertEquals(Action.Delay(200), actions[2])
    }

    // --- Case insensitivity ---

    @Test
    fun caseInsensitivePlaceholders() {
        assertEquals(Action.TypeField("UserName"), AutoTypeSequenceParser.parse("{username}")[0])
        assertEquals(Action.TypeField("Password"), AutoTypeSequenceParser.parse("{Password}")[0])
        assertEquals(Action.PressKey(AutoTypeKey.TAB), AutoTypeSequenceParser.parse("{tab}")[0])
    }

    // --- Unknown placeholder ---

    @Test
    fun unknownPlaceholderTypedAsLiteral() {
        val actions = AutoTypeSequenceParser.parse("{UNKNOWN}")
        assertEquals(1, actions.size)
        assertEquals(Action.TypeText("{UNKNOWN}"), actions[0])
    }

    // --- Resolve ---

    @Test
    fun resolveReplacesFieldReferences() {
        val entry = KdbxEntry(
            uuid = "test",
            fields = listOf(
                KdbxEntryField("UserName", "admin"),
                KdbxEntryField("Password", "secret"),
            ),
        )

        val actions = AutoTypeSequenceParser.parse("{USERNAME}{TAB}{PASSWORD}{ENTER}")
        val resolved = AutoTypeSequenceParser.resolve(actions, entry)

        assertEquals(4, resolved.size)
        assertEquals(Action.TypeText("admin"), resolved[0])
        assertEquals(Action.PressKey(AutoTypeKey.TAB), resolved[1])
        assertEquals(Action.TypeText("secret"), resolved[2])
        assertEquals(Action.PressKey(AutoTypeKey.ENTER), resolved[3])
    }

    @Test
    fun resolveCustomField() {
        val entry = KdbxEntry(
            uuid = "test",
            fields = listOf(
                KdbxEntryField("PIN", "1234"),
            ),
        )

        val actions = AutoTypeSequenceParser.parse("{S:PIN}{ENTER}")
        val resolved = AutoTypeSequenceParser.resolve(actions, entry)

        assertEquals(2, resolved.size)
        assertEquals(Action.TypeText("1234"), resolved[0])
    }

    @Test
    fun resolvesMissingFieldToEmptyString() {
        val entry = KdbxEntry(uuid = "test", fields = emptyList())

        val actions = AutoTypeSequenceParser.parse("{USERNAME}")
        val resolved = AutoTypeSequenceParser.resolve(actions, entry)

        assertEquals(Action.TypeText(""), resolved[0])
    }

    // --- getSequenceForEntry ---

    @Test
    fun getSequenceForEntryReturnsDefault() {
        val entry = KdbxEntry(uuid = "test", fields = emptyList())
        assertEquals(AutoTypeSequenceParser.DEFAULT_SEQUENCE, AutoTypeSequenceParser.getSequenceForEntry(entry))
    }

    @Test
    fun getSequenceForEntryReturnsCustom() {
        val entry = KdbxEntry(
            uuid = "test",
            fields = listOf(KdbxEntryField("AutoType-Default", "{PASSWORD}{ENTER}")),
        )
        assertEquals("{PASSWORD}{ENTER}", AutoTypeSequenceParser.getSequenceForEntry(entry))
    }
}
