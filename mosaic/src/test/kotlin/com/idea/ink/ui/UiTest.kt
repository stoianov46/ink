package com.idea.ink.ui

import com.idea.ink.theme.InkColors
import com.idea.ink.theme.Themes
import com.jakewharton.mosaic.ui.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UiTest {
    private fun seg(text: String) = StatusSegment(listOf(StatusPart(text, Color.Unspecified)))

    @Test fun frameTopSetsTheTitleIntoTheEdge() {
        assertEquals("╭─ Hi ───╮", frameTop(10, "Hi"))
        assertEquals("╭────────╮", frameTop(10, ""))
        assertEquals(10, frameTop(10, "A very long title indeed").length)
        assertEquals("+- Hi ---+", frameTop(10, "Hi", FrameGlyphs.Plain))
        assertEquals("╰────────╯", frameBottom(10))
    }

    @Test fun statusBarWrapsAndNeverDropsASegment() {
        val segments = listOf(seg("aaaaaaaa"), seg("bbbbbbbb"), seg("cccccccc"), seg("dddddddd"))
        val rows = wrapSegments(segments, width = 30, separatorWidth = 3)
        assertEquals(4, rows.sumOf { it.size })
        assertTrue(rows.size >= 2)
        rows.forEach { row -> assertTrue(row.sumOf { it.width } + 3 * (row.size - 1) <= 30) }
        assertEquals(1, wrapSegments(segments, width = 200).size)
    }

    @Test fun aSegmentWiderThanTheRowIsCutWithAnEllipsis() {
        val rows = wrapSegments(listOf(seg("x".repeat(100))), width = 20)
        assertEquals(1, rows.size)
        assertEquals(20, rows[0][0].width)
        assertTrue(rows[0][0].parts.last().text == "…")
    }

    @Test fun actionBarBreaksIntoLines() {
        val items = List(6) { ActionItem("k$it", "label") }
        val lines = breakIntoLines(items, width = 30)
        assertEquals(6, lines.sumOf { it.size })
        assertTrue(lines.size > 1)
    }

    @Test fun hintsAreKeptWholeOrLeftOut() {
        val hints = "Enter  open   Tab  next page   t  next theme"
        assertEquals(hints, fitHints(hints, 200))
        assertEquals("Enter  open", fitHints(hints, 15))
        assertEquals("", fitHints(hints, 3))
    }

    @Test fun wrapTextKeepsBlankLinesAndCutsLongWords() {
        assertEquals(listOf("aaa bbb", "ccc"), wrapText("aaa bbb ccc", 7))
        assertEquals(listOf("a", "", "b"), wrapText("a\n\nb", 10))
        assertTrue(wrapText("x".repeat(30), 10).all { it.length <= 10 })
    }

    @Test fun spanText() {
        assertEquals("45s", spanText(45_000))
        assertEquals("14m", spanText(14 * 60_000L))
        assertEquals("1h 02m", spanText(62 * 60_000L))
    }

    @Test fun truecolorPassesThroughAndA256TerminalKeepsDarkColoursDark() {
        assertEquals(Color(0x1B, 0x2A, 0x41), 0x1B2A41.toMosaic(truecolor = true))
        assertEquals(Color(0, 0, 51), 0x1B2A41.toMosaic(truecolor = false))
    }

    @Test fun transparentThemesLeaveTheGroundUnpainted() {
        val ghost = MosaicColors(InkColors(Themes.Ghost), truecolor = true)
        assertEquals(Color.Unspecified, ghost.Background)
        assertEquals(Color.Unspecified, ghost.Surface)
        assertEquals(Color.Unspecified, ghost.EntryBg)
        assertEquals(Color(0x0B, 0x0F, 0x14), ghost.Ink) // the real background still serves as text colour on badges
        val dark = MosaicColors(InkColors(Themes.Dark), truecolor = true)
        assertEquals(Color(0x0A, 0x0A, 0x0A), dark.Background)
    }
}
