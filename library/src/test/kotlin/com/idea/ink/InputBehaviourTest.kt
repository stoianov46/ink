package com.idea.ink

import com.idea.ink.input.Caret
import com.idea.ink.input.ClipboardReader
import com.idea.ink.input.Key
import com.idea.ink.input.KeyDispatcher
import com.idea.ink.input.KeyboardLayout
import com.idea.ink.input.MetaSequence
import com.idea.ink.input.Selection
import com.idea.ink.input.TextEdit
import com.idea.ink.scroll.scrollThumb
import com.idea.ink.text.caretRow
import com.idea.ink.text.wrapRows
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** A text field driven by [TextEdit], the way an app uses it: keys in, text and caret out. */
private class Field(var text: String = "", var caret: Caret = Caret()) {
    fun press(vararg keys: Key): Field {
        for (k in keys) TextEdit.apply("f", text, caret, k)?.let { (t, c) -> text = t; caret = c }
        return this
    }
    fun type(s: String): Field { s.forEach { press(Key.Text(it)) }; return this }
    val pos get() = caret.at("f", text)
    val selected: String? get() = caret.selectionFor("f")?.bounds(text.length)?.let { text.substring(it.first, it.last + 1) }
}

class TextEditTest {
    @Test fun typingInsertsAtTheCaretAndTheCaretFollows() {
        val f = Field().type("helo").press(Key.Left).type("l")
        assertEquals("hello", f.text)
        assertEquals(4, f.pos)
        f.press(Key.End).type("!")
        assertEquals("hello!", f.text)
    }

    @Test fun backspaceAndDeleteStopAtTheEnds() {
        val f = Field("ab").press(Key.Home, Key.Backspace)
        assertEquals("ab", f.text)
        f.press(Key.Delete)
        assertEquals("b", f.text)
        f.press(Key.End, Key.Delete)
        assertEquals("b", f.text)
    }

    @Test fun shiftMovesSelectAndTypingReplacesTheSelection() {
        val f = Field("hello world").press(Key.Select(Key.Left), Key.Select(Key.Left), Key.Select(Key.Left))
        assertEquals("rld", f.selected)
        f.type("X")
        assertEquals("hello woX", f.text)
        assertNull(f.selected)
    }

    @Test fun selectionGrowsWordByWordAndBackspaceDeletesIt() {
        val f = Field("one two three").press(Key.Select(Key.WordLeft))
        assertEquals("three", f.selected)
        f.press(Key.Select(Key.WordLeft))
        assertEquals("two three", f.selected)
        f.press(Key.Backspace)
        assertEquals("one ", f.text)
    }

    @Test fun selectAllThenTypeReplacesEverythingAndEmptyTextHasNothingToSelect() {
        val f = Field("abc").press(Key.SelectAll)
        assertEquals("abc", f.selected)
        f.type("z")
        assertEquals("z", f.text)
        assertNull(Field().press(Key.SelectAll).selected)
    }

    @Test fun leftAndRightCollapseASelectionToItsEnds() {
        val a = Field("abcdef").press(Key.Select(Key.Left), Key.Select(Key.Left)).press(Key.Left)
        assertEquals(4, a.pos); assertNull(a.selected) // collapsed to the start of "ef"
        val b = Field("abcdef").press(Key.Home, Key.Select(Key.Right), Key.Select(Key.Right)).press(Key.Right)
        assertEquals(2, b.pos); assertNull(b.selected) // collapsed to the end of "ab"
    }

    @Test fun wordMovesAndDeleteWord() {
        val f = Field("alpha beta  gamma")
        f.press(Key.WordLeft); assertEquals(12, f.pos)
        f.press(Key.WordLeft); assertEquals(6, f.pos)
        f.press(Key.WordRight); assertEquals(10, f.pos)
        f.press(Key.End, Key.DeleteWord)
        assertEquals("alpha beta  ", f.text)
    }

    @Test fun lineOperationsWorkOnTheCaretsLine() {
        val f = Field("first\nsecond line\nthird")
        f.caret = Caret("f", 8) // inside "second line"
        f.press(Key.DeleteToLineEnd)
        assertEquals("first\nse\nthird", f.text)
        f.press(Key.DeleteToLineStart)
        assertEquals("first\n\nthird", f.text)
        assertNull(TextEdit.apply("f", "abc", Caret("f", 0), Key.DeleteToLineStart)) // already at the start: not an edit
    }

    @Test fun homeAndEndStayOnTheLineWhileTextStartAndEndCrossLines() {
        val f = Field("ab\ncd").press(Key.Home)
        assertEquals(3, f.pos)
        f.press(Key.End); assertEquals(5, f.pos)
        f.press(Key.TextStart); assertEquals(0, f.pos)
        f.press(Key.TextEnd); assertEquals(5, f.pos)
    }

    @Test fun upAndDownKeepTheColumnAndAreNotOursOnTheFirstAndLastLine() {
        val f = Field("abcdef\nxy\nlonger line")
        f.caret = Caret("f", 5)
        f.press(Key.Down); assertEquals(9, f.pos)   // "xy" is shorter: the end of that line
        f.press(Key.Down); assertEquals(9 + 1 + 2, f.pos).also { }
        assertNull(TextEdit.apply("f", "one line", Caret("f", 2), Key.Up))
        assertNull(TextEdit.apply("f", "one line", Caret("f", 2), Key.Down))
    }

    @Test fun pastedNewlineIsALineBreakAndControlCharactersAreRejected() {
        assertEquals("a\nb", Field("ab").press(Key.Left, Key.PastedNewline).text)
        assertNull(TextEdit.apply("f", "x", Caret(), Key.Text('\u0007')))
        assertNull(TextEdit.apply("f", "x", Caret(), Key.Enter)) // Enter is the caller's
        assertNull(TextEdit.apply("f", "x", Caret(), Key.Tab))
    }

    @Test fun aCaretFromAnotherFieldIsTheEndOfThisOne() {
        assertEquals(5, Caret("other", 1).at("f", "hello"))
        assertEquals(5, Caret("f", 99).at("f", "hello")) // out of range: the end
    }

    @Test fun lineFieldsRefuseGrowthPastTheirLimitAndTurnPastedBreaksIntoSpaces() {
        assertNull(TextEdit.applyLine("f", "abcd", Caret(), Key.Text('e'), max = 4))
        assertEquals("a b", TextEdit.applyLine("f", "ab", Caret("f", 1), Key.PastedNewline, max = 10)!!.first)
    }

    @Test fun selectionBoundsAreClampedAndEmptyWhenAnchorEqualsCaret() {
        assertEquals(2 until 5, Selection(5, 2).bounds(10))
        assertNull(Selection(3, 3).bounds(10))
        assertEquals(3 until 10, Selection(3, Caret.END).bounds(10))
    }
}

class MetaSequenceTest {
    private fun feed(vararg keys: Key): MetaSequence.Result {
        val m = MetaSequence()
        var last: MetaSequence.Result = MetaSequence.Result.Pass
        keys.forEachIndexed { i, k -> last = m.feed(k, i * 1_000_000L) }
        return last
    }

    @Test fun optionShiftArrowSentAsEscEscBracketIsPutBackTogether() {
        val r = feed(Key.MetaPrefix, Key.Text('['), Key.Text('1'), Key.Text(';'), Key.Text('4'), Key.Text('D'))
        assertEquals(MetaSequence.Result.Emit(Key.Select(Key.WordLeft)), r)
    }

    @Test fun plainBracketLettersAreSwallowedWhileTheSequenceIsRead() {
        assertEquals(MetaSequence.Result.Swallow, feed(Key.MetaPrefix))
        assertEquals(MetaSequence.Result.Swallow, feed(Key.MetaPrefix, Key.Text('[')))
    }

    @Test fun anOrdinaryKeyPassesAndASequenceThatStallsIsAbandoned() {
        assertEquals(MetaSequence.Result.Pass, feed(Key.Text('a')))
        val m = MetaSequence(windowNanos = 10)
        m.feed(Key.MetaPrefix, 0)
        assertEquals(MetaSequence.Result.Pass, m.feed(Key.Text('['), 1_000_000)) // too late: it is a typed bracket
    }

    @Test fun cmdArrowsGoToTheStartAndEndOfTheText() {
        val m = MetaSequence()
        listOf(Key.MetaPrefix, Key.Text('['), Key.Text('1'), Key.Text(';'), Key.Text('9')).forEachIndexed { i, k -> m.feed(k, i.toLong()) }
        assertEquals(MetaSequence.Result.Emit(Key.TextStart), m.feed(Key.Text('D'), 6))
    }
}

class KeyboardLayoutTest {
    @Test fun cyrillicLettersMapToTheQwertyKeyInTheSamePosition() {
        assertEquals('n', KeyboardLayout.latinFor('т'))
        assertEquals('Q', KeyboardLayout.latinFor('Й'))
        assertEquals('s', KeyboardLayout.latinFor('і'))
        assertNull(KeyboardLayout.latinFor('x'))
    }
}

class KeyDispatcherTest {
    private var now = 0L
    private val seen = ArrayList<Key>()
    private fun dispatcher(clipboard: String? = null, handler: (Key) -> Boolean = { seen += it; true }) =
        KeyDispatcher(globalHandler = { false }, clipboard = ClipboardReader { clipboard }, nanoTime = { now }).also { it.screenHandler = handler }

    private fun KeyDispatcher.send(key: Key, afterMs: Long = 200) { now += afterMs * 1_000_000; dispatch(key) }

    @Test fun anEnterRightBehindAnotherKeyIsAPastedLineBreakButASlowOneIsEnter() {
        val d = dispatcher()
        d.send(Key.Text('a')); d.send(Key.Enter, afterMs = 1); d.send(Key.Enter, afterMs = 300)
        assertEquals(listOf(Key.Text('a'), Key.PastedNewline, Key.Enter), seen)
    }

    @Test fun aTabInAPasteIsAnIndentSpaceNotAFieldChange() {
        val d = dispatcher()
        d.send(Key.Text('a')); d.send(Key.Tab, afterMs = 1)
        assertEquals(Key.Text(' '), seen.last())
        d.send(Key.Tab, afterMs = 300)
        assertEquals(Key.Tab, seen.last())
    }

    @Test fun cyrillicKeysReachTheScreenAsTypedAndShortcutsGetTheLatinLetter() {
        // a screen with a text field takes what was typed; a screen with a shortcut `n` gets it from `т`
        val typed = ArrayList<Key>()
        val first = dispatcher { k -> typed += k; false }
        first.send(Key.Text('т'))
        assertEquals(listOf<Key>(Key.Text('т'), Key.Text('n')), typed)
        seen.clear()
        val d = dispatcher { k -> seen += k; k == Key.Text('т') } // the field takes the Cyrillic letter itself
        d.send(Key.Text('т'))
        assertEquals(listOf<Key>(Key.Text('т')), seen)
    }

    @Test fun shiftArrowOnAScreenWithoutATextFieldIsThePlainArrow() {
        val got = ArrayList<Key>()
        val d = dispatcher { k -> got += k; k == Key.Up }
        d.send(Key.Select(Key.Up))
        assertEquals(listOf(Key.Select(Key.Up), Key.Up), got)
    }

    @Test fun aLongPasteThatLostItsBeginningIsRestoredFromTheClipboard() {
        // Mosaic keeps the last 64 events of a burst: the terminal sent 130 characters, the app saw only the last 64
        val full = (0 until 130).map { ('a' + it % 26) + "" + (it / 26) }.joinToString("").take(130)
        val lost = full.takeLast(64)
        var field = ""
        var caret = Caret()
        val d = dispatcher(clipboard = full) { k -> TextEdit.apply("f", field, caret, k)?.let { (t, c) -> field = t; caret = c; true } ?: false }
        lost.forEach { d.send(Key.Text(it), afterMs = 1) }
        assertEquals(full, field)
    }

    @Test fun aShortPasteIsLeftAlone() {
        var field = ""
        var caret = Caret()
        val d = dispatcher(clipboard = "short text") { k -> TextEdit.apply("f", field, caret, k)?.let { (t, c) -> field = t; caret = c; true } ?: false }
        "short text".forEach { d.send(Key.Text(it), afterMs = 1) }
        assertEquals("short text", field)
    }
}

class RowsAndScrollMathTest {
    @Test fun rowsBreakByCharacterAndAtLineBreaksAndAlwaysThereIsOneRow() {
        assertEquals(listOf(0 to 4, 4 to 8, 8 to 10), wrapRows("abcdefghij", 4).map { it.start to it.end })
        assertEquals(listOf(0 to 2, 3 to 5), wrapRows("ab\ncd", 10).map { it.start to it.end })
        assertEquals(1, wrapRows("", 5).size)
    }

    @Test fun theCaretOfAFullRowIsDrawnOnTheNextRow() {
        val rows = wrapRows("abcd", 2)
        assertEquals(0, caretRow(rows, 1))
        assertEquals(1, caretRow(rows, 2))
        assertEquals(1, caretRow(rows, 4))
    }

    @Test fun neverSplitsASurrogatePair() {
        val rows = wrapRows("a😀b", 2)
        assertTrue(rows.all { r -> !("a😀b".getOrNull(r.end - 1)?.isHighSurrogate() ?: false) || r.end == 4 })
    }

    @Test fun thumbIsAtLeastOneRowTracksTheScrollAndIsAbsentWhenEverythingFits() {
        assertNull(scrollThumb(10, 0, 5, 10))
        assertEquals(0 until 1, scrollThumb(10, 0, 1000, 10))
        val end = scrollThumb(10, 90, 100, 10)!!
        assertEquals(9, end.last)
        assertTrue(scrollThumb(10, 45, 100, 10)!!.first in 1..8)
    }
}
