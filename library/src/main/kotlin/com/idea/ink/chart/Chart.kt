package com.idea.ink.chart

import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

/**
 * Small charts drawn with block characters, as plain text: the maths and the glyphs only, so they can be tested without a terminal.
 * The `ink-mosaic` components (`Bar`, `Sparkline`, `HeatStrip`, `BarChart`) colour the text.
 */
object Chart {
    private const val STEPS = "▁▂▃▄▅▆▇█"
    private const val HEAT = "░▒▓█"

    /** One character per value, as tall as the value is against the largest: a zero is a dot, so a quiet week is still visible. */
    fun spark(values: List<Long>): String {
        val max = values.maxOrNull() ?: 0L
        return values.joinToString("") { v ->
            if (v <= 0L || max <= 0L) "·" else STEPS[(((v * 8 + max - 1) / max).toInt() - 1).coerceIn(0, 7)].toString()
        }
    }

    /**
     * How a bar is drawn: the glyph of a filled cell and of an empty one. Each chart takes the style that suits what it shows.
     * [use] says when to pick it.
     */
    enum class BarStyle(val on: Char, val off: Char, val use: String) {
        /** The solid block: full height, the heaviest look. */
        Block('█', '░', "the default: a bar that should be seen from across the room"),

        /** A thin line, for a gauge of a percentage (how much of something is in order). */
        Line('━', '─', "a gauge of a percentage: how much of something is in order"),

        /** Slim slanted blocks, for progress (how far work has got). */
        Slim('▰', '▱', "progress: how far work has got (the status pane's sliders)"),

        /** Half the height with a thin baseline, for comparing sizes side by side. */
        Half('▄', '▁', "comparing sizes side by side in a table or a list"),

        /** One dot per step, for counting things (how many links, step 3 of 5). */
        Dots('●', '○', "counting things: steps, links, retries"),

        /** A textured track of braille dots, for a series over time. */
        Braille('⣿', '⣀', "a series over time, a textured track"),
    }

    /** How many of [width] cells are filled for [value] of [max]: none for zero, at least one for any value above zero. */
    fun filledCells(value: Int, max: Int, width: Int): Int = if (width <= 0 || max <= 0 || value <= 0) 0 else (value.toLong() * width / max).toInt().coerceIn(1, width)

    /** A bar [width] cells wide: [value] of [max] filled, the rest empty, in [style]. A value above zero always shows at least one cell. */
    fun bar(value: Int, max: Int, width: Int, style: BarStyle = BarStyle.Block): String = barParts(value, max, width, style).let { it.first + it.second }

    /** [bar] as its two parts, the filled cells and the empty ones, so a screen can colour them apart (the empty part quietly). */
    fun barParts(value: Int, max: Int, width: Int, style: BarStyle = BarStyle.Block): Pair<String, String> {
        if (width <= 0) return "" to ""
        val filled = filledCells(value, max, width)
        return style.on.toString().repeat(filled) to style.off.toString().repeat(width - filled)
    }

    /** The intensity of a day, 0 (nothing) to 4 (the busiest), for [count] against [max]. */
    fun heatLevel(count: Int, max: Int): Int = if (count <= 0 || max <= 0) 0 else (((count * 4 + max - 1) / max)).coerceIn(1, 4)

    /**
     * [counts] (a count per day, in date order) as a calendar: seven rows (Monday to Sunday), one column per week, the first column being the week of the first
     * day. A day that is not in [counts] (the rest of this week) is null. Each cell is a [heatLevel].
     */
    fun heatCalendar(counts: List<Pair<java.time.LocalDate, Int>>, weeks: Int): List<List<Int?>> {
        if (counts.isEmpty()) return List(7) { List(weeks) { null } }
        val max = counts.maxOf { it.second }
        val first = counts.first().first
        val byDay = counts.toMap()
        return (0 until 7).map { row ->
            (0 until weeks).map { col -> byDay[first.plusDays((col * 7 + row).toLong())]?.let { heatLevel(it, max) } }
        }
    }

    /** The weekday labels of the heat rows. */
    val WEEKDAYS = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    /** The glyph of one cell of a heat strip: a dot for nothing, then `░▒▓█` by intensity. */
    fun heatGlyph(level: Int): Char = if (level <= 0) '·' else HEAT[(level - 1).coerceIn(0, 3)]

    /** Smallest "round" value (1, 2 or 5 × 10^k) that is ≥ [x]: the step of a chart's axis. */
    fun niceStep(x: Double): Double {
        if (x <= 0.0) return 1.0
        val pow = 10.0.pow(floor(log10(x)))
        val m = x / pow
        val nice = when { m <= 1.0 -> 1.0; m <= 2.0 -> 2.0; m <= 5.0 -> 5.0; else -> 10.0 }
        return nice * pow
    }

    /** Axis maximum: [divisions] equal round steps that cover [max]. */
    fun niceMax(max: Double, divisions: Int): Double = divisions * niceStep(max / divisions)
}
