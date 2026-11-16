package com.idea.ink.ui

import androidx.compose.runtime.Composable
import com.jakewharton.mosaic.layout.background
import com.jakewharton.mosaic.layout.fillMaxWidth
import com.jakewharton.mosaic.layout.padding
import com.jakewharton.mosaic.layout.width
import com.jakewharton.mosaic.modifier.Modifier
import com.jakewharton.mosaic.ui.Box
import com.jakewharton.mosaic.ui.Color
import com.jakewharton.mosaic.ui.Row
import com.jakewharton.mosaic.ui.TextStyle

/** [weight] is the column's share of the row; columns shrink proportionally (and truncate) on narrow terminals. */
data class TableColumn(
    val title: String,
    /** Share of the flexible space; ignored when [fixed] is set. */
    val weight: Float = 1f,
    val alignEnd: Boolean = false,
    /** Exact width in cells (including the 1-cell gap) for columns that must never be truncated, like badges and dates. */
    val fixed: Int = 0,
)

/** Table cells are single-line: text with line breaks must not make its row taller. */
private fun String.flat() = replace(Regex("\\s*\\n\\s*"), " ")

/** One cell: text in [color], or a pill when [badgeBg] is set, or a horizontal bar of [bar] (0..1) instead of text. */
data class Cell(
    val text: String = "",
    val color: Color = Palette.Text,
    val badgeBg: Color = Color.Unspecified,
    val style: TextStyle = TextStyle.Unspecified,
    val bar: Float? = null,
    val barColor: Color = Palette.Green,
)

@Composable
fun TableHeader(columns: List<TableColumn>) {
    Row(Modifier.fillMaxWidth()) {
        columns.forEach { c ->
            Box((if (c.fixed > 0) Modifier.width(c.fixed) else Modifier.weight(c.weight)).padding(right = 1)) {
                Label(c.title, color = Palette.Text, textStyle = TextStyle.Bold, fill = true, align = if (c.alignEnd) LabelAlign.End else LabelAlign.Start)
            }
        }
    }
}

/** A row of [cells] under [columns]; a [selected] row sits on [Palette.Selected] and plain colours turn into the theme's "text on selected" ones (a bright selection needs dark text). */
@Composable
fun TableRow(cells: List<Cell>, columns: List<TableColumn>, selected: Boolean = false) {
    Row(Modifier.fillMaxWidth().let { if (selected) it.background(Palette.Selected) else it }) {
        cells.forEachIndexed { i, cell ->
            val col = columns[i]
            val color = if (selected && cell.color == Palette.Text) Palette.OnSelected else if (selected && cell.color == Palette.Muted) Palette.OnSelectedMuted else cell.color
            Box((if (col.fixed > 0) Modifier.width(col.fixed) else Modifier.weight(col.weight)).padding(right = 1)) {
                when {
                    cell.bar != null -> HBar(cell.bar, cell.barColor, Palette.SurfaceHigh)
                    cell.badgeBg != Color.Unspecified -> Label(" ${cell.text.flat()} ", color = cell.color, background = cell.badgeBg, textStyle = cell.style)
                    else -> Label(cell.text.flat(), color = color, textStyle = cell.style, fill = true, align = if (col.alignEnd) LabelAlign.End else LabelAlign.Start)
                }
            }
        }
    }
}
