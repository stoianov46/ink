package com.idea.ink.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private fun MdLine.text() = runs.joinToString("") { it.text }

class ConsoleTest {
    private fun lines(md: String, width: Int = 60) = renderMarkdown(md, width).map { it.text() }

    @Test fun headingsBulletsAndInlineMarkersAreNotShownRaw() {
        val out = lines("## Title\n\n- one **bold** and `code`\n- two")
        assertTrue(out.any { it.startsWith("▌ Title") })
        assertTrue(out.any { it.contains("● one bold and  code ") })
        assertTrue(out.none { it.contains("**") || it.contains("## ") })
    }

    @Test fun aTableIsDrawnAsABoxWithinTheWidth() {
        val out = lines("| a | b |\n|---|--:|\n| 1 | 22 |", width = 30)
        assertTrue(out.first().startsWith("╭") && out.last().startsWith("╰"))
        assertTrue(out.all { it.length <= 30 })
        assertTrue(out.any { it.startsWith("├") })
    }

    @Test fun aCodeFenceIsAClosedBoxWithLineNumbersAndAnOpenOneWhileStreaming() {
        val closed = lines("```kotlin\nval x = 1\nval y = 2\n```", 40)
        assertTrue(closed.first().contains("kotlin"))
        assertTrue(closed.any { it.contains(" 1 │ val x = 1") })
        assertTrue(closed.last().startsWith("╰"))
        val open = lines("```kotlin\nval x = 1", 40) // the closing fence has not arrived
        assertTrue(open.size >= 3 && open.last().startsWith("╰"))
    }

    @Test fun everyRowFitsTheWidth() {
        val md = "A very long paragraph with `inline code` and a [link](https://example.com/very/long/path) that must wrap over several rows. ".repeat(4)
        assertTrue(lines(md, 40).all { it.length <= 40 })
    }

    @Test fun emojiAndLatexAreMadeSafe() {
        assertEquals("ok ✓", lines("ok ✅").single())
        assertEquals("a → b", lines("a $\\rightarrow$ b").single())
    }

    @Test fun plainGlyphsUseAscii() {
        val out = renderMarkdown("- item\n\n> quote", 30, plainGlyphs = true).map { it.text() }
        assertTrue(out.any { it.startsWith("- item") })
        assertTrue(out.any { it.startsWith("> quote") })
    }

    @Test fun entriesBecomeConsoleRows() {
        val rows = consoleLines(
            listOf(
                UserEntry("hi"),
                ThinkingEntry("hmm", live = false, seconds = 1.2),
                ToolEntry("find", "x", ToolStatus.Done, "a\nb\nc\nd\ne\nf\ng\nh", 0.4),
                ToolEntry("run", "ls", ToolStatus.WaitingApproval),
                AssistantEntry("**done**", "model"),
            ),
            width = 50,
        ).map { it.text() }
        assertTrue(rows.any { it.contains("❯ you") })
        assertTrue(rows.any { it.contains("thought for 1.2s") })
        assertTrue(rows.any { it.contains("⚙ find x") && it.contains("✓ 0.4s") })
        assertTrue(rows.any { it.contains("… +2 more lines") }) // 8 result lines, 6 shown
        assertTrue(rows.any { it.contains("waiting for approval") })
        assertTrue(rows.any { it.contains("◆ model") })
        assertTrue(rows.all { it.length <= 50 })
    }

    @Test fun aRunningToolShowsTheSpinnerFrame() {
        val a = consoleLines(listOf(ToolEntry("t", "", ToolStatus.Running)), 40, frame = 0).single().text()
        val b = consoleLines(listOf(ToolEntry("t", "", ToolStatus.Running)), 40, frame = 3).single().text()
        assertTrue(a.contains("⠋ running…") && b.contains("⠸ running…"))
    }

    @Test fun slimBarFillsAndPlainModeUsesAscii() {
        assertEquals("▰▰▰" to "▱▱", slimBar(5, 3))
        assertEquals("###" to "--", slimBar(5, 3, plain = true))
        assertEquals("▰▰▰▰▰" to "", slimBar(5, 99))
    }
}

class NavPositionTest {
    @Test fun idsRoundTrip() {
        NavPosition.entries.forEach { assertEquals(it, NavPosition.of(it.id)) }
        assertEquals(NavPosition.Left, NavPosition.of("diagonal"))
        assertEquals(NavPosition.Left, NavPosition.of(null))
    }
}
