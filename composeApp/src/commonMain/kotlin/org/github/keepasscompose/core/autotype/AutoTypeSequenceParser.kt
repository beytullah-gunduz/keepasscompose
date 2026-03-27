package org.github.keepasscompose.core.autotype

import org.github.keepasscompose.core.model.KdbxEntry

/**
 * Parses KeePass auto-type sequences into executable actions.
 *
 * KeePass auto-type uses a special syntax for typing credentials:
 * - `{USERNAME}` — types the entry's username
 * - `{PASSWORD}` — types the entry's password
 * - `{URL}` — types the entry's URL
 * - `{TITLE}` — types the entry's title
 * - `{NOTES}` — types the entry's notes
 * - `{TAB}` — presses Tab
 * - `{ENTER}` — presses Enter
 * - `{DELAY X}` — waits X milliseconds
 * - `{SPACE}` — presses Space
 * - `{BACKSPACE}` / `{BS}` — presses Backspace
 * - `{DELETE}` / `{DEL}` — presses Delete
 * - `{HOME}` — presses Home
 * - `{END}` — presses End
 * - `{ESC}` — presses Escape
 * - `{UP}` / `{DOWN}` / `{LEFT}` / `{RIGHT}` — arrow keys
 * - `{+}` / `{^}` / `{%}` / `{~}` — literal +, ^, %, ~
 * - `{S:FieldName}` — types a custom field value
 * - `+` — Shift modifier (when not in braces)
 * - `^` — Ctrl modifier
 * - `%` — Alt modifier
 * - Literal text — typed as-is
 *
 * The default sequence is `{USERNAME}{TAB}{PASSWORD}{ENTER}`.
 */
object AutoTypeSequenceParser {
    const val DEFAULT_SEQUENCE = "{USERNAME}{TAB}{PASSWORD}{ENTER}"

    /**
     * An executable action in the auto-type sequence.
     */
    sealed class Action {
        /** Type literal text. */
        data class TypeText(val text: String) : Action()

        /** Type a field value from the entry. */
        data class TypeField(val fieldKey: String) : Action()

        /** Press a special key. */
        data class PressKey(val key: Int) : Action()

        /** Press a key with modifiers. */
        data class PressKeyCombo(val modifiers: List<Int>, val key: Int) : Action()

        /** Wait for a specified number of milliseconds. */
        data class Delay(val milliseconds: Long) : Action()

        /** Type the value of a custom string field. */
        data class TypeCustomField(val fieldName: String) : Action()
    }

    /**
     * Parse an auto-type sequence string into a list of actions.
     *
     * @param sequence The auto-type sequence string.
     * @return List of parsed actions.
     */
    fun parse(sequence: String): List<Action> {
        if (sequence.isBlank()) return parse(DEFAULT_SEQUENCE)

        val actions = mutableListOf<Action>()
        var i = 0
        var pendingModifiers = mutableListOf<Int>()

        while (i < sequence.length) {
            val char = sequence[i]

            when {
                // Brace-enclosed placeholder
                char == '{' -> {
                    val closeIndex = sequence.indexOf('}', i + 1)
                    if (closeIndex == -1) {
                        // Unterminated brace — type as literal
                        actions.add(Action.TypeText("{"))
                        i++
                        continue
                    }

                    val placeholder = sequence.substring(i + 1, closeIndex)
                    val action = parsePlaceholder(placeholder)

                    if (pendingModifiers.isNotEmpty() && action is Action.PressKey) {
                        actions.add(Action.PressKeyCombo(pendingModifiers.toList(), action.key))
                        pendingModifiers.clear()
                    } else {
                        flushModifiers(pendingModifiers, actions)
                        actions.add(action)
                    }

                    i = closeIndex + 1
                }

                // Modifier keys outside braces
                char == '+' -> {
                    pendingModifiers.add(AutoTypeKey.SHIFT)
                    i++
                }

                char == '^' -> {
                    pendingModifiers.add(AutoTypeKey.CTRL)
                    i++
                }

                char == '%' -> {
                    pendingModifiers.add(AutoTypeKey.ALT)
                    i++
                }

                char == '~' -> {
                    flushModifiers(pendingModifiers, actions)
                    actions.add(Action.PressKey(AutoTypeKey.ENTER))
                    i++
                }

                // Literal text
                else -> {
                    if (pendingModifiers.isNotEmpty()) {
                        // Single character with modifiers
                        actions.add(Action.TypeText(char.toString()))
                        pendingModifiers.clear()
                        i++
                    } else {
                        // Collect consecutive literal characters
                        val start = i
                        while (i < sequence.length && sequence[i] !in SPECIAL_CHARS) {
                            i++
                        }
                        actions.add(Action.TypeText(sequence.substring(start, i)))
                    }
                }
            }
        }

        flushModifiers(pendingModifiers, actions)
        return actions
    }

    /**
     * Resolve field references in actions to their values from the given entry.
     *
     * @param actions The parsed actions.
     * @param entry The entry to resolve field values from.
     * @return Actions with field references replaced by TypeText actions.
     */
    fun resolve(actions: List<Action>, entry: KdbxEntry): List<Action> = actions.map { action ->
        when (action) {
            is Action.TypeField -> {
                val value = entry.field(action.fieldKey)
                Action.TypeText(value)
            }

            is Action.TypeCustomField -> {
                val value = entry.field(action.fieldName)
                Action.TypeText(value)
            }

            else -> action
        }
    }

    /**
     * Get the auto-type sequence for an entry, falling back to default.
     */
    fun getSequenceForEntry(entry: KdbxEntry): String {
        val customSequence = entry.field("AutoType-Default")
        return customSequence.ifBlank { DEFAULT_SEQUENCE }
    }

    private fun parsePlaceholder(placeholder: String): Action {
        val upper = placeholder.uppercase()

        return when {
            // Field references
            upper == "USERNAME" -> Action.TypeField("UserName")

            upper == "PASSWORD" -> Action.TypeField("Password")

            upper == "URL" -> Action.TypeField("URL")

            upper == "TITLE" -> Action.TypeField("Title")

            upper == "NOTES" -> Action.TypeField("Notes")

            // Special keys
            upper == "TAB" -> Action.PressKey(AutoTypeKey.TAB)

            upper == "ENTER" -> Action.PressKey(AutoTypeKey.ENTER)

            upper == "SPACE" -> Action.PressKey(AutoTypeKey.SPACE)

            upper == "BACKSPACE" || upper == "BS" -> Action.PressKey(AutoTypeKey.BACKSPACE)

            upper == "DELETE" || upper == "DEL" -> Action.PressKey(AutoTypeKey.DELETE)

            upper == "HOME" -> Action.PressKey(AutoTypeKey.HOME)

            upper == "END" -> Action.PressKey(AutoTypeKey.END)

            upper == "ESC" || upper == "ESCAPE" -> Action.PressKey(AutoTypeKey.ESCAPE)

            upper == "UP" -> Action.PressKey(AutoTypeKey.UP)

            upper == "DOWN" -> Action.PressKey(AutoTypeKey.DOWN)

            upper == "LEFT" -> Action.PressKey(AutoTypeKey.LEFT)

            upper == "RIGHT" -> Action.PressKey(AutoTypeKey.RIGHT)

            // Literal special characters
            placeholder == "+" -> Action.TypeText("+")

            placeholder == "^" -> Action.TypeText("^")

            placeholder == "%" -> Action.TypeText("%")

            placeholder == "~" -> Action.TypeText("~")

            placeholder == "{" -> Action.TypeText("{")

            placeholder == "}" -> Action.TypeText("}")

            // Delay
            upper.startsWith("DELAY ") || upper.startsWith("DELAY=") -> {
                val ms = upper.removePrefix("DELAY ").removePrefix("DELAY=").trim().toLongOrNull() ?: 0
                Action.Delay(ms)
            }

            // Custom field
            upper.startsWith("S:") -> {
                val fieldName = placeholder.substring(2)
                Action.TypeCustomField(fieldName)
            }

            // Unknown placeholder — type as literal with braces
            else -> Action.TypeText("{$placeholder}")
        }
    }

    private fun flushModifiers(modifiers: MutableList<Int>, actions: MutableList<Action>) {
        modifiers.clear()
    }

    private val SPECIAL_CHARS = setOf('{', '+', '^', '%', '~')
}
