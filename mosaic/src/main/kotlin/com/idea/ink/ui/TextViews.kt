package com.idea.ink.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.idea.ink.input.Selection
import com.idea.ink.text.caretRow
import com.idea.ink.text.sanitizeForTerminal
import com.idea.ink.text.wrapRows
import com.jakewharton.mosaic.layout.DrawScope
import com.jakewharton.mosaic.layout.Measurable
import com.jakewharton.mosaic.layout.MeasurePolicy
import com.jakewharton.mosaic.layout.MeasureResult
import com.jakewharton.mosaic.layout.MeasureScope
import com.jakewharton.mosaic.layout.drawBehind
import com.jakewharton.mosaic.modifier.Modifier
import com.jakewharton.mosaic.ui.Color
import com.jakewharton.mosaic.ui.Layout
import com.jakewharton.mosaic.ui.unit.Constraints

private class TextViewState {
    var lines: List<String> = emptyList()
    /** The first visible row of a [WrappedText], kept between frames so the text only scrolls when the cursor leaves the window. */
    var first = 0
    /** Per drawn row, the columns to show as selected (reverse video), or null. */
    var selected: List<IntRange?> = emptyList()
    /** Per drawn row of a [WrappedText] that scrolls, the scrollbar glyph for its last column ("█" thumb, "│" track); empty when everything fits. */
    var bar: List<String> = emptyList()
}

/** Draws the selected columns of [line] on row [row] in reverse video (the theme's background colour on its text colour), over what was already drawn. */
private fun DrawScope.drawSelected(row: Int, line: String, cols: IntRange?) {
    if (cols == null) return
    val a = cols.first.coerceIn(0, line.length)
    val b = (cols.last + 1).coerceIn(a, line.length)
    if (b > a) drawText(row, a, line.substring(a, b), Palette.Ink, Palette.Text)
}

/**
 * One row of text that follows its cursor. With the cursor at the end (the usual case) it shows the END of the text,
 * cut from the start with "…"; with the cursor moved into the text it shows the stretch around the cursor, with "…" on
 * the cut side(s). Line breaks show as "⏎". Only the visible part is sanitized, so a huge paste costs nothing.
 *
 * @param caretAt the cursor position in [text], or -1 for the end; ignored when [caret] is false.
 */
@Composable
fun TailText(text: String, modifier: Modifier = Modifier, color: Color = Color.Unspecified, caret: Boolean = false, caretAt: Int = -1, selection: Selection? = null) {
    val state = remember { TextViewState() }
    Layout(
        content = {},
        modifier = modifier.drawBehind {
            state.lines.firstOrNull()?.let { drawText(0, 0, it, color); drawSelected(0, it, state.selected.firstOrNull()) }
        },
        debugInfo = { "TailText" },
        measurePolicy = object : MeasurePolicy {
            override fun MeasureScope.measure(measurables: List<Measurable>, constraints: Constraints): MeasureResult {
                val w = if (constraints.hasBoundedWidth) constraints.maxWidth else 80
                if (w <= 0 || constraints.maxHeight < 1) {
                    state.lines = emptyList()
                    state.selected = emptyList()
                    return layout(0, 0) {}
                }
                fun flat(part: String) = sanitizeForTerminal(part).replace('\n', '⏎')
                val selected = if (caret) selection?.bounds(text.length) else null
                val pos = if (caret && selected != null) (if (caretAt < 0) text.length else caretAt.coerceAtMost(text.length)) else if (caret && caretAt in 0 until text.length) caretAt else -1
                state.selected = emptyList()
                val line = if (pos < 0) {
                    val cursor = if (caret) "▏" else ""
                    val room = (w - cursor.length).coerceAtLeast(1)
                    // A line break is drawn as one glyph, so the tail we look at can be a little more than the room.
                    val tail = flat(text.takeLast(room * 3))
                    (if (tail.length <= room && text.length <= room * 3) tail else "…" + tail.takeLast(room - 1)) + cursor
                } else {
                    // The cursor is inside the text: show the stretch around it, with "…" where the text goes on.
                    val room = (w - 3).coerceAtLeast(1)
                    val from = (pos - room / 2).coerceIn(0, (text.length - room).coerceAtLeast(0))
                    val to = minOf(text.length, from + room)
                    val lead = if (from > 0) 1 else 0
                    if (selected != null) {
                        // A selection is shown in reverse video instead of a cursor glyph, so columns line up with the text.
                        val a = maxOf(selected.first, from) - from + lead
                        val b = minOf(selected.last + 1, to) - from + lead
                        state.selected = listOf(if (b > a) a until b else null)
                        (if (from > 0) "…" else "") + flat(text.substring(from, to)) + (if (to < text.length) "…" else "")
                    } else {
                        (if (from > 0) "…" else "") + flat(text.substring(from, pos)) + "▏" + flat(text.substring(pos, to)) + (if (to < text.length) "…" else "")
                    }
                }
                state.lines = listOf(line.take(w).padEnd(w))
                return layout(w, 1) {}
            }
        },
    )
}


/**
 * A text field that wraps over up to [maxRows] rows and scrolls, instead of cutting the text: the window follows the
 * cursor, and the last column shows "↑" / "↓" when there are rows above / below it. Without [caret] it shows the start.
 * Rows break by character, so the cursor and the edited text always line up with what is drawn.
 *
 * @param caretAt the cursor position in [text], or -1 for the end; ignored when [caret] is false.
 */
@Composable
fun WrappedText(text: String, maxRows: Int, modifier: Modifier = Modifier, color: Color = Color.Unspecified, caret: Boolean = false, caretAt: Int = -1, selection: Selection? = null) {
    val state = remember { TextViewState() }
    Layout(
        content = {},
        modifier = modifier.drawBehind {
            state.lines.forEachIndexed { row, line -> drawText(row, 0, line, color); drawSelected(row, line, state.selected.getOrNull(row)) }
            state.bar.forEachIndexed { row, glyph -> drawText(row, width - 1, glyph, if (glyph == "█") Palette.Accent else Palette.Dim) }
        },
        debugInfo = { "WrappedText" },
        measurePolicy = object : MeasurePolicy {
            override fun MeasureScope.measure(measurables: List<Measurable>, constraints: Constraints): MeasureResult {
                val w = if (constraints.hasBoundedWidth) constraints.maxWidth else 80
                val rowLimit = minOf(maxRows, if (constraints.hasBoundedHeight) constraints.maxHeight else maxRows)
                if (w < 3 || rowLimit < 1) {
                    state.lines = emptyList()
                    return layout(0, 0) {}
                }
                // One column is kept for the cursor glyph and one for the scroll marks.
                val rows = wrapRows(text, w - 2)
                val shown = minOf(rows.size, rowLimit)
                val pos = if (caret && caretAt in 0..text.length) caretAt else text.length
                val selected = if (caret) selection?.bounds(text.length) else null
                if (caret) {
                    val at = caretRow(rows, pos)
                    if (at < state.first) state.first = at
                    if (at >= state.first + shown) state.first = at - shown + 1
                } else state.first = 0
                state.first = state.first.coerceIn(0, rows.size - shown)
                state.selected = (0 until shown).map { i ->
                    val row = rows[state.first + i]
                    // Columns match the text, because a selection draws no cursor glyph.
                    selected?.let { r -> (maxOf(r.first, row.start) - row.start) until (minOf(r.last + 1, row.end) - row.start) }?.takeIf { !it.isEmpty() }
                }
                state.lines = (0 until shown).map { i ->
                    val index = state.first + i
                    val row = rows[index]
                    val raw = text.substring(row.start, row.end)
                    val body = if (caret && selected == null && caretRow(rows, pos) == index) {
                        val a = (pos - row.start).coerceIn(0, raw.length)
                        sanitizeForTerminal(raw.substring(0, a)) + "▏" + sanitizeForTerminal(raw.substring(a))
                    } else sanitizeForTerminal(raw)
                    body.take(w - 1).padEnd(w - 1) + " "
                }
                // A scrollbar in the last column: the thumb is as tall as the visible share of the text and sits where the window is.
                state.bar = if (rows.size <= shown) emptyList() else {
                    val thumb = (shown * shown / rows.size).coerceIn(1, shown)
                    val top = if (rows.size == shown) 0 else ((state.first.toLong() * (shown - thumb) + (rows.size - shown) / 2) / (rows.size - shown)).toInt()
                    (0 until shown).map { if (it in top until top + thumb) "█" else "│" }
                }
                return layout(w, shown) {}
            }
        },
    )
}

