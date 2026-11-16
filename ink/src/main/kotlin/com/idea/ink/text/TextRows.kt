package com.idea.ink.text

/** One screen row of a wrapped text field: the characters `[start, end)` of the text (a line break is not part of a row). */
data class TextRow(val start: Int, val end: Int)

/**
 * Breaks [text] into rows of at most [width] characters: at every line break, and by character (not by word) where a row
 * is full, so a long branch name or URL is never cut and a cursor position maps to exactly one place. Always at least one row.
 */
fun wrapRows(text: String, width: Int): List<TextRow> {
    val w = width.coerceAtLeast(1)
    val rows = ArrayList<TextRow>()
    var lineStart = 0
    while (true) {
        val nl = text.indexOf('\n', lineStart).let { if (it < 0) text.length else it }
        var start = lineStart
        do {
            var end = minOf(start + w, nl)
            if (end < nl && end > start + 1 && text[end - 1].isHighSurrogate()) end-- // never split a surrogate pair
            rows += TextRow(start, end)
            start = end
        } while (start < nl)
        if (nl >= text.length) break
        lineStart = nl + 1
    }
    return rows
}

/** The row the cursor at [pos] is drawn on: the last row that starts at or before it (a full row hands over to the next). */
fun caretRow(rows: List<TextRow>, pos: Int): Int = rows.indexOfLast { it.start <= pos }.coerceAtLeast(0)
