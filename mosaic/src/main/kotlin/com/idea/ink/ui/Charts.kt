package com.idea.ink.ui

import androidx.compose.runtime.Composable
import com.idea.ink.chart.Chart
import com.jakewharton.mosaic.layout.DrawScope
import com.jakewharton.mosaic.layout.Measurable
import com.jakewharton.mosaic.layout.MeasurePolicy
import com.jakewharton.mosaic.layout.MeasureResult
import com.jakewharton.mosaic.layout.MeasureScope
import com.jakewharton.mosaic.layout.drawBehind
import com.jakewharton.mosaic.modifier.Modifier
import com.jakewharton.mosaic.ui.Color
import com.jakewharton.mosaic.ui.Layout
import com.jakewharton.mosaic.ui.Row
import com.jakewharton.mosaic.ui.unit.Constraints

/**
 * A bar [width] cells wide with [value] of [max] filled in [color] and the rest in [track], drawn in one of six [Chart.BarStyle]s (each says when to use it).
 * A value above zero always shows at least one cell, so a small amount is never invisible.
 */
@Composable
fun Bar(value: Int, max: Int, width: Int, style: Chart.BarStyle = Chart.BarStyle.Block, color: Color = Palette.Accent, track: Color = Palette.Dim) {
    val (on, off) = Chart.barParts(value, max, width, style)
    Row {
        Label(on, color = color)
        Label(off, color = track)
    }
}

/** One character per value (`▁▂▃▅▇█`), as tall as the value is against the largest; a zero is a dot. */
@Composable
fun Sparkline(values: List<Long>, color: Color = Palette.ChartA) = Label(Chart.spark(values), color = color)

/** A strip of cells, each lit by its level 0..4 (see [Chart.heatLevel]): a dot for nothing, then `░▒▓█` getting brighter. One row of a calendar heat map. */
@Composable
fun HeatStrip(levels: List<Int>) {
    Row {
        levels.forEach { level ->
            val color = when (level) { 0 -> Palette.Dim; 1 -> Palette.ChartTrack; 2 -> Palette.CyanSoft; 3 -> Palette.ChartA; else -> Palette.Cta }
            Label(Chart.heatGlyph(level).toString(), color = color)
        }
    }
}

/** One column of a [BarChart]: stacked segments, bottom-up. */
data class StackedBar(val label: String, val segments: List<Pair<Double, Color>>) {
    val total get() = segments.sumOf { it.first }
}

private val eighths = charArrayOf(' ', '▁', '▂', '▃', '▄', '▅', '▆', '▇', '█')

/**
 * Stacked vertical bar chart drawn with block characters (1/8-row resolution). Bars are as wide as the space allows, so a 7-day chart uses the full width just
 * like a 90-day one; the y axis uses round values with faint gridlines, and labels are spread evenly. Bars are bucketed only when there are more of them than
 * horizontal cells. It is at least 14 columns wide and 4 rows tall, otherwise it draws nothing.
 */
@Composable
fun BarChart(bars: List<StackedBar>, modifier: Modifier = Modifier, preferredHeight: Int = 8, maxLabel: (Double) -> String) {
    Layout(
        content = {},
        modifier = modifier.drawBehind { drawBars(bars, maxLabel) },
        debugInfo = { "BarChart" },
        measurePolicy = object : MeasurePolicy {
            override fun MeasureScope.measure(measurables: List<Measurable>, constraints: Constraints): MeasureResult {
                val w = if (constraints.hasBoundedWidth) constraints.maxWidth else 40
                val h = if (constraints.hasBoundedHeight) minOf(preferredHeight, constraints.maxHeight) else preferredHeight
                return if (w < 14 || h < 4) layout(0, 0) {} else layout(w, h) {}
            }
        },
    )
}

private fun DrawScope.drawBars(bars: List<StackedBar>, maxLabel: (Double) -> String) {
    if (bars.isEmpty()) return
    val plotRows = height - 1
    val divisions = if (plotRows >= 9) 4 else 2
    val axisMax = Chart.niceMax(bars.maxOf { it.total }, divisions)
    val axisLabels = (0..divisions).map { maxLabel(axisMax * it / divisions) }
    val axisWidth = axisLabels.maxOf { it.length } + 2
    val plotWidth = width - axisWidth

    val bucketSize = ((bars.size + plotWidth - 1) / plotWidth).coerceAtLeast(1)
    val buckets = bars.chunked(bucketSize).map { group ->
        val merged = LinkedHashMap<Color, Double>()
        group.forEach { b -> b.segments.forEach { (v, c) -> merged[c] = (merged[c] ?: 0.0) + v } }
        StackedBar(group.last().label, merged.map { (c, v) -> v to c })
    }
    val cell = (plotWidth / buckets.size).coerceAtLeast(1)
    val gap = if (cell >= 3) maxOf(1, cell / 6) else 0
    val barWidth = cell - gap
    val left = axisWidth + (plotWidth - cell * buckets.size) / 2 // centre the group, so the chart is balanced

    // gridlines and y labels at each division (row 0 is the top)
    for (d in 0..divisions) {
        val row = (plotRows - 1) - ((plotRows - 1) * d / divisions)
        drawText(row, axisWidth, "┈".repeat(plotWidth.coerceAtLeast(0)), Palette.SurfaceHigh)
        drawText(row, 0, axisLabels[d].padStart(axisWidth - 1), Palette.Muted)
    }

    for (screenRow in 0 until plotRows) {
        val row = plotRows - 1 - screenRow // 0 = bottom
        buckets.forEachIndexed { i, bar ->
            var from = 0.0
            var dominant: Color? = null
            var dominantOverlap = 0.0
            var filled = 0.0
            val lo = row * 8.0
            val hi = lo + 8
            bar.segments.forEach { (v, c) ->
                val segLo = from / axisMax * plotRows * 8
                val segHi = (from + v) / axisMax * plotRows * 8
                val overlap = (minOf(segHi, hi) - maxOf(segLo, lo)).coerceAtLeast(0.0)
                if (overlap > dominantOverlap) { dominantOverlap = overlap; dominant = c }
                filled += overlap
                from += v
            }
            if (filled > 0.25) {
                val glyph = eighths[filled.toInt().coerceIn(1, 8)]
                drawText(screenRow, left + i * cell + gap / 2, glyph.toString().repeat(barWidth), dominant ?: Palette.Dim)
            }
        }
    }

    // x labels, evenly spaced and never overlapping
    val labelWidth = buckets.maxOf { it.label.length }
    val step = ((labelWidth + 2 + cell - 1) / cell).coerceAtLeast(1)
    var lastEnd = -1
    fun place(i: Int) {
        val label = buckets[i].label
        val start = (left + i * cell + gap / 2 + (barWidth - label.length) / 2).coerceIn(axisWidth, (width - label.length).coerceAtLeast(axisWidth))
        if (start > lastEnd) {
            drawText(height - 1, start, label, Palette.Muted)
            lastEnd = start + label.length + 1
        }
    }
    for (i in buckets.indices step step) place(i)
    place(buckets.lastIndex) // always show the newest label if it still fits
}

/** `■ name  ■ name`: the key of a chart's colours. */
@Composable
fun Legend(vararg items: Pair<String, Color>) {
    Row {
        items.forEach { (label, color) ->
            Label("■ ", color = color)
            Label("$label  ", color = Palette.Muted)
        }
    }
}
