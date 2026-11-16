package com.idea.ink.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.idea.ink.text.sanitizeForTerminal
import com.jakewharton.mosaic.layout.Measurable
import com.jakewharton.mosaic.layout.MeasurePolicy
import com.jakewharton.mosaic.layout.MeasureResult
import com.jakewharton.mosaic.layout.MeasureScope
import com.jakewharton.mosaic.layout.drawBehind
import com.jakewharton.mosaic.modifier.Modifier
import com.jakewharton.mosaic.ui.Color
import com.jakewharton.mosaic.ui.Layout
import com.jakewharton.mosaic.ui.TextStyle
import com.jakewharton.mosaic.ui.unit.Constraints

enum class LabelAlign { Start, Center, End }

private class LabelState {
    var lines: List<String> = emptyList()
}

/**
 * Constraint-aware text. Mosaic's own `Text` ignores the space it is given and draws past the edge of the screen
 * (which crashes the renderer), so every piece of UI text goes through this: it sanitizes the text, truncates it with an
 * ellipsis to the available width and drops lines that don't fit the available height.
 *
 * @param fill take all available width (needed for alignment / full-width backgrounds)
 * @param wrap word-wrap instead of truncating
 */
@Composable
fun Label(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    background: Color = Color.Unspecified,
    textStyle: TextStyle = TextStyle.Unspecified,
    align: LabelAlign = LabelAlign.Start,
    fill: Boolean = false,
    wrap: Boolean = false,
) {
    val state = remember { LabelState() }
    val clean = remember(text) { sanitizeForTerminal(text) }
    // Text with no colour of its own uses the theme's, not the terminal's, so it stays readable on any theme.
    val fg = if (color == Color.Unspecified) Palette.Text else color
    Layout(
        content = {},
        modifier = modifier.drawBehind {
            state.lines.forEachIndexed { row, line -> drawText(row, 0, line, fg, background, textStyle) }
        },
        debugInfo = { "Label($text)" },
        measurePolicy = object : MeasurePolicy {
            override fun MeasureScope.measure(measurables: List<Measurable>, constraints: Constraints): MeasureResult {
                val maxW = constraints.maxWidth
                val maxH = constraints.maxHeight
                var lines = clean.split('\n')
                if (wrap && constraints.hasBoundedWidth) lines = lines.flatMap { wrapText(it, maxW) }
                if (constraints.hasBoundedHeight && lines.size > maxH) {
                    // more text than rows: the last row says so instead of the rest vanishing
                    lines = if (maxH <= 0) emptyList() else lines.take(maxH).let { it.dropLast(1) + (it.last().trimEnd() + "…") }
                }
                if (constraints.hasBoundedWidth) {
                    lines = lines.map { if (it.length > maxW) it.take((maxW - 1).coerceAtLeast(0)) + if (maxW > 0) "…" else "" else it }
                }
                var width = lines.maxOfOrNull { it.length } ?: 0
                if (fill && constraints.hasBoundedWidth) width = maxW
                width = width.coerceIn(constraints.minWidth, maxW.coerceAtLeast(0))
                if (width == 0 || lines.isEmpty()) {
                    state.lines = emptyList()
                    return layout(0, 0) {}
                }
                state.lines = lines.map {
                    when (align) {
                        LabelAlign.Start -> it.padEnd(width)
                        LabelAlign.End -> it.padStart(width)
                        LabelAlign.Center -> it.padStart((width + it.length) / 2).padEnd(width)
                    }.let { s -> if (fill || align != LabelAlign.Start) s else it }
                }
                return layout(width, lines.size) {}
            }
        },
    )
}

/** A horizontal bar of block characters, [fraction] filled; fills the available width (capped at [maxCells]). */
@Composable
fun HBar(
    fraction: Float,
    color: Color,
    track: Color,
    modifier: Modifier = Modifier,
    maxCells: Int = 60,
    /** Where to mark a limit on the bar (0..1), drawn as a tick over the bar. */
    marker: Float? = null,
) {
    Layout(
        content = {},
        modifier = modifier.drawBehind {
            val filled = (fraction.coerceIn(0f, 1f) * width).toInt().coerceIn(0, width)
            if (filled > 0) drawText(0, 0, "▬".repeat(filled), color)
            if (width - filled > 0) drawText(0, filled, "▬".repeat(width - filled), track)
            marker?.let { drawText(0, (it * width).toInt().coerceIn(0, (width - 1).coerceAtLeast(0)), "┃", Palette.Text) }
        },
        debugInfo = { "HBar($fraction)" },
        measurePolicy = object : MeasurePolicy {
            override fun MeasureScope.measure(measurables: List<Measurable>, constraints: Constraints): MeasureResult {
                val w = (if (constraints.hasBoundedWidth) constraints.maxWidth else 10).coerceAtMost(maxCells)
                return if (w <= 0 || constraints.maxHeight < 1) layout(0, 0) {} else layout(w, 1) {}
            }
        },
    )
}

/** Word-wraps [text] to [width] columns; blank lines are kept, a word longer than the width is cut. */
fun wrapText(text: String, width: Int): List<String> {
    val max = width.coerceAtLeast(4)
    val out = mutableListOf<String>()
    for (raw in text.split('\n')) {
        var line = raw
        if (line.isEmpty()) {
            out += ""
            continue
        }
        while (line.length > max) {
            var cut = line.lastIndexOf(' ', max)
            if (cut <= 0) cut = max
            out += line.substring(0, cut).trimEnd()
            line = line.substring(cut).trimStart()
        }
        out += line
    }
    return out
}
