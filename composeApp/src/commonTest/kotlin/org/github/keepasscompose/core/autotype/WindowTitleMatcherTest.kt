package org.github.keepasscompose.core.autotype

import org.github.keepasscompose.core.model.KdbxEntry
import org.github.keepasscompose.core.model.KdbxEntryField
import org.github.keepasscompose.core.model.KdbxGroup
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WindowTitleMatcherTest {
    private fun entry(title: String, windowPattern: String? = null, url: String = ""): KdbxEntry {
        val fields = mutableListOf(
            KdbxEntryField("Title", title),
            KdbxEntryField("UserName", "user"),
            KdbxEntryField("Password", "pass", isProtected = true),
            KdbxEntryField("URL", url),
            KdbxEntryField("Notes", ""),
        )
        if (windowPattern != null) {
            fields.add(KdbxEntryField("AutoType-Window", windowPattern))
        }
        return KdbxEntry(uuid = title, fields = fields)
    }

    private fun group(name: String, entries: List<KdbxEntry> = emptyList(), groups: List<KdbxGroup> = emptyList()): KdbxGroup =
        KdbxGroup(uuid = name, name = name, entries = entries, groups = groups)

    // --- Plain text matching ---

    @Test
    fun plainTextMatchesSubstring() {
        assertTrue(WindowTitleMatcher.matches("GitHub", "GitHub - Dashboard"))
    }

    @Test
    fun plainTextMatchIsCaseInsensitive() {
        assertTrue(WindowTitleMatcher.matches("github", "GitHub - Dashboard"))
    }

    @Test
    fun plainTextDoesNotMatchDifferentText() {
        assertFalse(WindowTitleMatcher.matches("GitLab", "GitHub - Dashboard"))
    }

    // --- Wildcard matching ---

    @Test
    fun wildcardMatchesStar() {
        assertTrue(WindowTitleMatcher.matches("*GitHub*", "My GitHub Dashboard"))
    }

    @Test
    fun wildcardMatchesPrefix() {
        assertTrue(WindowTitleMatcher.matches("GitHub*", "GitHub - Dashboard"))
    }

    @Test
    fun wildcardMatchesSuffix() {
        assertTrue(WindowTitleMatcher.matches("*Dashboard", "GitHub - Dashboard"))
    }

    @Test
    fun wildcardMatchesExact() {
        assertTrue(WindowTitleMatcher.matches("GitHub", "GitHub - Dashboard"))
    }

    @Test
    fun wildcardDoesNotMatchWrongPattern() {
        assertFalse(WindowTitleMatcher.matches("GitLab*", "GitHub - Dashboard"))
    }

    @Test
    fun wildcardMatchesMiddle() {
        assertTrue(WindowTitleMatcher.matches("*-*", "GitHub - Dashboard"))
    }

    @Test
    fun wildcardIsCaseInsensitive() {
        assertTrue(WindowTitleMatcher.matches("*GITHUB*", "GitHub - Dashboard"))
    }

    // --- Regex matching ---

    @Test
    fun regexMatches() {
        assertTrue(WindowTitleMatcher.matches("//Git(Hub|Lab)//", "GitHub - Dashboard"))
    }

    @Test
    fun regexMatchesAlternative() {
        assertTrue(WindowTitleMatcher.matches("//Git(Hub|Lab)//", "GitLab - Dashboard"))
    }

    @Test
    fun regexDoesNotMatchWrongPattern() {
        assertFalse(WindowTitleMatcher.matches("//^Exact$//", "GitHub - Dashboard"))
    }

    @Test
    fun regexIsCaseInsensitive() {
        assertTrue(WindowTitleMatcher.matches("//github//", "GitHub - Dashboard"))
    }

    @Test
    fun invalidRegexReturnsFalse() {
        assertFalse(WindowTitleMatcher.matches("//[invalid//", "text"))
    }

    // --- Blank inputs ---

    @Test
    fun blankPatternReturnsFalse() {
        assertFalse(WindowTitleMatcher.matches("", "Some Window"))
    }

    @Test
    fun blankWindowTitleReturnsFalse() {
        assertFalse(WindowTitleMatcher.matches("pattern", ""))
    }

    // --- getWindowPatterns ---

    @Test
    fun fallsBackToTitleWithWildcards() {
        val e = entry("GitHub Login")
        val patterns = WindowTitleMatcher.getWindowPatterns(e)
        assertEquals(listOf("*GitHub Login*"), patterns)
    }

    @Test
    fun usesCustomWindowPattern() {
        val e = entry("GitHub", windowPattern = "GitHub*")
        val patterns = WindowTitleMatcher.getWindowPatterns(e)
        assertEquals(listOf("GitHub*"), patterns)
    }

    @Test
    fun emptyTitleReturnsEmptyPatterns() {
        val e = entry("")
        val patterns = WindowTitleMatcher.getWindowPatterns(e)
        assertTrue(patterns.isEmpty())
    }

    // --- findMatches ---

    @Test
    fun findMatchesByTitle() {
        val root = group(
            "Root",
            entries = listOf(entry("GitHub"), entry("GitLab")),
        )

        val results = WindowTitleMatcher.findMatches(root, "GitHub - Dashboard")
        assertEquals(1, results.size)
        assertEquals("GitHub", results[0].entry.title)
    }

    @Test
    fun findMatchesByCustomPattern() {
        val root = group(
            "Root",
            entries = listOf(
                entry("My Bank", windowPattern = "*Banking Portal*"),
            ),
        )

        val results = WindowTitleMatcher.findMatches(root, "Welcome to Banking Portal - Chrome")
        assertEquals(1, results.size)
    }

    @Test
    fun findMatchesSearchesRecursively() {
        val root = group(
            "Root",
            groups = listOf(
                group(
                    "Social",
                    entries = listOf(entry("Twitter")),
                ),
            ),
        )

        val results = WindowTitleMatcher.findMatches(root, "Twitter / Home")
        assertEquals(1, results.size)
        assertEquals(listOf("Root", "Social"), results[0].groupPath)
    }

    @Test
    fun findMatchesReturnsEmptyForNoMatch() {
        val root = group(
            "Root",
            entries = listOf(entry("GitHub")),
        )

        val results = WindowTitleMatcher.findMatches(root, "Microsoft Word")
        assertTrue(results.isEmpty())
    }

    @Test
    fun findMatchesReturnsSequence() {
        val e = KdbxEntry(
            uuid = "test",
            fields = listOf(
                KdbxEntryField("Title", "Test"),
                KdbxEntryField("AutoType-Default", "{PASSWORD}{ENTER}"),
            ),
        )
        val root = group("Root", entries = listOf(e))

        val results = WindowTitleMatcher.findMatches(root, "Test Window")
        assertEquals(1, results.size)
        assertEquals("{PASSWORD}{ENTER}", results[0].sequence)
    }

    @Test
    fun findMatchesUsesDefaultSequenceWhenNoCustom() {
        val root = group("Root", entries = listOf(entry("MyApp")))

        val results = WindowTitleMatcher.findMatches(root, "MyApp - Main Window")
        assertEquals(1, results.size)
        assertEquals(AutoTypeSequenceParser.DEFAULT_SEQUENCE, results[0].sequence)
    }

    // --- wildcardMatch ---

    @Test
    fun wildcardMatchEscapesRegexChars() {
        assertTrue(WindowTitleMatcher.wildcardMatch("file.txt*", "file.txt (Notepad)"))
        assertFalse(WindowTitleMatcher.wildcardMatch("file.txt*", "filextxt (Notepad)"))
    }
}
