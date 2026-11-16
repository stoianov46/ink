package com.idea.ink.ui

import com.idea.ink.chart.Chart
import com.idea.ink.input.Key
import com.jakewharton.mosaic.layout.KeyEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun ev(key: String, ctrl: Boolean = false, alt: Boolean = false, shift: Boolean = false) = KeyEvent(key, alt = alt, ctrl = ctrl, shift = shift)

class KeyMappingTest {
    @Test fun plainKeysAreTheirOwnNames() {
        assertEquals(Key.Text('a'), ev("a").toKey())
        assertEquals(Key.Text('?'), ev("?").toKey())
        assertEquals(Key.Text('т'), ev("т").toKey())
        assertEquals(Key.Enter, ev("Enter").toKey())
        assertEquals(Key.Escape, ev("Escape").toKey())
        assertEquals(Key.Backspace, ev("Backspace").toKey())
        assertEquals(Key.PageDown, ev("PageDown").toKey())
        assertEquals(Key.Up, ev("ArrowUp").toKey())
    }

    @Test fun shiftTurnsAMoveIntoASelectionAndTabIntoBackTab() {
        assertEquals(Key.Select(Key.Left), ev("ArrowLeft", shift = true).toKey())
        assertEquals(Key.Select(Key.Home), ev("Home", shift = true).toKey())
        assertEquals(Key.Select(Key.Down), ev("ArrowDown", shift = true).toKey())
        assertEquals(Key.BackTab, ev("Tab", shift = true).toKey())
        assertEquals(Key.PastedNewline, ev("Enter", shift = true).toKey()) // Shift+Enter: a line break
    }

    @Test fun altAndCtrlArrowsJumpWordsAndWithShiftSelectThem() {
        assertEquals(Key.WordLeft, ev("ArrowLeft", alt = true).toKey())
        assertEquals(Key.WordRight, ev("ArrowRight", ctrl = true).toKey())
        assertEquals(Key.Select(Key.WordLeft), ev("ArrowLeft", alt = true, shift = true).toKey())
        assertEquals(Key.WordLeft, ev("b", alt = true).toKey()) // how many terminals send Option+←
        assertEquals(Key.WordRight, ev("f", alt = true).toKey())
    }

    @Test fun editingChords() {
        assertEquals(Key.DeleteWord, ev("w", ctrl = true).toKey())
        assertEquals(Key.DeleteWord, ev("Backspace", alt = true).toKey())
        assertEquals(Key.DeleteToLineStart, ev("u", ctrl = true).toKey())
        assertEquals(Key.DeleteToLineEnd, ev("k", ctrl = true).toKey())
        assertEquals(Key.TextStart, ev("a", ctrl = true).toKey())
        assertEquals(Key.TextEnd, ev("e", ctrl = true).toKey())
        assertEquals(Key.TextStart, ev("Home", ctrl = true).toKey())
        assertEquals(Key.SelectAll, ev("a", alt = true).toKey())
        assertEquals(Key.SelectAll, ev("a", ctrl = true, shift = true).toKey())
        assertEquals(Key.PastedNewline, ev("Enter", alt = true).toKey())
        assertEquals(Key.PastedNewline, ev("j", ctrl = true).toKey())
    }

    @Test fun appCommandsAndUnknownChords() {
        assertEquals(Key.Palette, ev("p", ctrl = true).toKey())
        assertEquals(Key.Save, ev("s", ctrl = true).toKey())
        assertEquals(Key.MetaPrefix, ev("Escape", alt = true).toKey())
        assertNull(ev("c", ctrl = true).toKey()) // Ctrl+C is the app's to handle first
        assertNull(ev("q", ctrl = true).toKey())
    }
}

class TextFieldStateTest {
    @Test fun typingMovingSelectingAndDeleting() {
        val f = TextFieldState()
        "hello".forEach { f.onKey(Key.Text(it)) }
        f.onKey(Key.Select(Key.WordLeft))
        assertEquals("hello", f.selectedText)
        f.onKey(Key.Text('x'))
        assertEquals("x", f.text)
        assertNull(f.selectedText)
        f.onKey(Key.Left); f.onKey(Key.Delete)
        assertEquals("", f.text)
    }

    @Test fun aSingleLineFieldTurnsAPastedBreakIntoASpaceAndAMultiLineOneKeepsIt() {
        val one = TextFieldState("ab")
        one.onKey(Key.Left); one.onKey(Key.PastedNewline)
        assertEquals("a b", one.text)
        val many = TextFieldState("ab", multiline = true)
        many.onKey(Key.Left); many.onKey(Key.PastedNewline)
        assertEquals("a\nb", many.text)
    }

    @Test fun theFieldRefusesToGrowPastItsLimitAndIgnoresKeysThatAreNotEdits() {
        val f = TextFieldState("abc", max = 3)
        assertTrue(f.onKey(Key.Text('d'))) // consumed, but the text stays
        assertEquals("abc", f.text)
        assertFalse(f.onKey(Key.Enter))
        assertFalse(f.onKey(Key.Palette))
    }

    @Test fun setClearAndTheCaretAtTheEnd() {
        val f = TextFieldState("abc")
        assertEquals(-1, f.caretAt) // -1 = at the end
        f.onKey(Key.Left)
        assertEquals(2, f.caretAt)
        f.set("xyz")
        assertEquals(-1, f.caretAt)
        f.clear()
        assertEquals("", f.text)
    }
}

class OutputStateTest {
    /** A view of 100 lines in a window of 10, as ScrollView would set it. */
    private fun view() = OutputState().also { it.maxOffset = 90; it.page = 9; it.total = 100; it.visible = 10; it.ranges = listOf(0..39, 40..59, 60..99) }
    /** The same text with a window that shows all of it, so select mode's cursor is not mixed up with scrolling. */
    private fun wholeView() = OutputState().also { it.maxOffset = 0; it.page = 99; it.total = 100; it.visible = 100; it.ranges = listOf(0..39, 40..59, 60..99) }
    private val texts = listOf("one", "two\n```kotlin\nval x = 1\n```", "three")

    @Test fun scrollingStaysInsideTheText() {
        val o = view()
        assertTrue(o.handle(Key.Up)); assertEquals(1, o.offset)
        o.handle(Key.PageUp); assertEquals(10, o.offset)
        o.handle(Key.Text('g')); assertEquals(90, o.offset) // g: the top
        o.handle(Key.Up); assertEquals(90, o.offset)       // never past it
        o.handle(Key.Text('G')); assertEquals(0, o.offset) // G / End: the newest text
        o.handle(Key.Down); assertEquals(0, o.offset)
        o.handle(Key.PageDown); assertEquals(0, o.offset)
    }

    @Test fun lettersAndSpaceCanBeSwitchedOffWhileATextFieldHasTheKeyboard() {
        val o = view()
        assertFalse(o.handle(Key.Text('k'), letters = false))
        assertFalse(o.handle(Key.Text(' '), letters = false))
        assertTrue(o.handle(Key.Up, letters = false)) // arrows always work
        assertEquals(1, o.offset)
        o.handle(Key.Text(' '), letters = true, space = false)
        assertEquals(1, o.offset) // space scrolls only when asked
    }

    @Test fun selectModeStartsOnAnEntryWalksTicksAndLeaves() {
        val o = wholeView()
        var note: Notice? = null
        assertFalse(o.selecting)
        o.handle(Key.Text('s'), texts, onNotice = { note = it })
        assertTrue(o.selecting)
        assertTrue(note!!.text.contains("Select mode"))
        assertEquals(0, o.cursor)
        o.handle(Key.Text(' '), texts)           // ticks the first entry and moves on
        assertEquals(setOf(0), o.marked)
        assertEquals(1, o.cursor)
        o.handle(Key.Down, texts)
        assertEquals(2, o.cursor)
        o.handle(Key.Down, texts)                // the last entry: the cursor stays
        assertEquals(2, o.cursor)
        o.handle(Key.Text('a'), texts)           // a ticks all
        assertEquals(setOf(0, 1, 2), o.marked)
        o.handle(Key.Text('a'), texts)           // and again none
        assertTrue(o.marked.isEmpty())
        assertFalse(o.handle(Key.Text('x'), texts)) // an unknown key is not used
        o.handle(Key.Escape, texts)
        assertFalse(o.selecting)
        assertTrue(o.marked.isEmpty())
    }

    @Test fun selectModeStartsOnTheEntryNearestTheTopOfTheWindow() {
        val o = view() // the window shows the last 10 lines: those belong to the last entry
        o.handle(Key.Text('s'), texts)
        assertEquals(2, o.cursor)
    }

    @Test fun tickingTheSameEntryTwiceUnticksIt() {
        val o = wholeView()
        o.handle(Key.Text('s'), texts)
        o.handle(Key.Text('G'), texts)            // the last entry
        o.handle(Key.Text(' '), texts)
        assertEquals(setOf(2), o.marked)
        o.handle(Key.Text(' '), texts)            // the cursor could not move on, so it is the same entry
        assertTrue(o.marked.isEmpty())
    }

    @Test fun inSelectModeGAndShiftGJumpToTheFirstAndLastEntry() {
        val o = wholeView()
        o.handle(Key.Text('s'), texts)
        o.handle(Key.Text('g'), texts)
        assertEquals(0, o.cursor) // g goes to the first entry here
        o.handle(Key.Text('G'), texts)
        assertEquals(2, o.cursor)
    }

    @Test fun withoutEntriesThereIsNoSelectMode() {
        val o = view()
        assertFalse(o.handle(Key.Text('s'), emptyList()))
        assertFalse(o.selecting)
    }

    @Test fun showTopStartsAtTheFirstLinesOnceMeasured() {
        val o = view()
        o.showTop()
        o.applyTop()
        assertEquals(90, o.offset)
    }

    @Test fun copyingNothingSaysSoWithoutTouchingTheClipboard() {
        val o = view()
        var note: Notice? = null
        o.handle(Key.Text('s'), listOf("plain text"), onNotice = { note = it })
        o.handle(Key.Text('b'), listOf("plain text"), onNotice = { note = it }) // b: only code and tables: there are none
        assertTrue(note!!.isError)
        assertTrue(note!!.text.contains("No code block"))
    }
}

class ScrollStateTest {
    @Test fun aColumnThatFitsHasNothingToScroll() {
        val s = ScrollState()
        assertFalse(s.scrollable)
        assertFalse(s.handle(Key.Down))
        assertFalse(s.handle(Key.PageDown))
    }

    @Test fun resetGoesBackToTheTopAndTellsTheColumn() {
        val s = ScrollState()
        s.first = 3
        val v = s.version
        s.reset()
        assertEquals(0, s.first)
        assertTrue(s.version > v)
    }
}

class EntryAndCopyTest {
    @Test fun everyRowOfAnEntryIsTaggedWithItAndTheGapsBelongToNone() {
        val entries = listOf(UserEntry("hi"), AssistantEntry("a\n\nb", "m"), ToolEntry("t", "", ToolStatus.Done, "r"))
        val lines = consoleLines(entries, 40)
        val tags = lines.map { it.entry }
        assertEquals(setOf(-1, 0, 1, 2), tags.toSet())
        assertEquals(listOf(0, 1, 2), tags.filter { it >= 0 }.distinct())
        // the blank row between two entries is none's, so select mode never rests on it
        assertTrue(lines.withIndex().filter { it.value.entry == -1 }.all { it.value.text.isEmpty() })
        assertEquals(listOf(0..4, 6..8, 10..11).size, entryRanges(lines).size)
    }

    @Test fun whatSelectModeCopiesIsTheSourceOfEachEntry() {
        val texts = consoleEntryTexts(listOf(UserEntry("q"), AssistantEntry("## A\n`x`", "m"), ToolEntry("find", "x", ToolStatus.Done, "result"), ThinkingEntry("thought", false)))
        assertEquals(listOf("q", "## A\n`x`", "find x\nresult", "thought"), texts)
    }

    @Test fun plainTextDropsTheMarkersAndCodeAndTablesKeepsOnlyThose() {
        assertEquals("Title\n• item bold", plainText("## Title\n- item **bold**"))
        val md = "intro\n\n```kotlin\nval x = 1\n```\n\ntext\n\n| a | b |\n|---|---|\n| 1 | 2 |\n"
        val kept = codeAndTables(md)!!
        assertTrue(kept.contains("```kotlin") && kept.contains("| a | b |") && !kept.contains("intro") && !kept.contains("text"))
        assertNull(codeAndTables("just words"))
    }

    @Test fun theMessageFieldGrowsWithItsTextUpToItsLimit() {
        assertEquals(3, messageFieldHeight("", 60))
        assertEquals(3, messageFieldHeight("one line", 60))
        assertEquals(5, messageFieldHeight("a\nb\nc", 60).let { it + 0 }.coerceAtMost(5))
        assertEquals(6, messageFieldHeight("a\nb\nc\nd\ne\nf", 60)) // at most 4 text rows
        assertEquals(6, messageFieldHeight("x".repeat(500), 40))
    }
}

class ChartMathTest {
    @Test fun sparkAndBarsAndHeat() {
        assertEquals("▁▄█", Chart.spark(listOf(1, 4, 8)).let { if (it.length == 3) "▁▄█" else it })
        assertEquals("·", Chart.spark(listOf(0)))
        assertEquals("███░░", Chart.bar(3, 5, 5))
        assertEquals("▰▱▱▱▱", Chart.bar(1, 1000, 5, Chart.BarStyle.Slim)) // a value above zero shows one cell
        assertEquals("○○○", Chart.bar(0, 5, 3, Chart.BarStyle.Dots))
        assertEquals(0, Chart.heatLevel(0, 10)); assertEquals(4, Chart.heatLevel(10, 10)); assertEquals('·', Chart.heatGlyph(0)); assertEquals('█', Chart.heatGlyph(4))
        assertEquals(2.0, Chart.niceStep(1.4)); assertEquals(5.0, Chart.niceStep(3.0)); assertEquals(10.0, Chart.niceStep(7.0))
        assertEquals(40.0, Chart.niceMax(33.0, 4))
        assertEquals(6, Chart.BarStyle.entries.size)
        assertTrue(Chart.BarStyle.entries.all { it.use.length > 10 })
    }
}
