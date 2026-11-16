package com.idea.ink.layout

import kotlin.math.roundToInt

/** Where a pane sits around the main part of a screen, like panels docked around an editor. */
enum class Dock(val id: String) {
    Left("left"), Right("right"), Top("top"), Bottom("bottom");

    /** A column (full height, takes columns) rather than a strip (takes rows from the main part). */
    val isColumn: Boolean get() = this == Left || this == Right

    companion object {
        fun of(id: String?, default: Dock): Dock = entries.firstOrNull { it.id == id } ?: default
    }
}

/** The panes Arcana docks around its conversation. */
enum class PaneId { Status, Thinking }

const val PANE_MIN = 10
const val PANE_MAX = 90
const val PANE_STEP = 5

/** What `{ }` / `[ ]` do to a pane: whether it is on, its size in percent, and what to tell the user (empty = nothing). */
class PaneStep(val on: Boolean, val percent: Int, val message: String)

/**
 * One press of a pane's size keys: [larger] switches the pane on when it is off, then grows it by 5 % up to 90 %; smaller shrinks it by 5 % and,
 * at 10 %, switches it off. Smaller does nothing while the pane is off. Pure, so it can be tested.
 */
fun paneStep(name: String, on: Boolean, percent: Int, larger: Boolean): PaneStep {
    val now = percent.coerceIn(PANE_MIN, PANE_MAX)
    if (!on) {
        if (!larger) return PaneStep(false, now, "")
        return PaneStep(true, now, "$name on at $now %.")
    }
    if (larger && now >= PANE_MAX) return PaneStep(true, now, "The $name is already at its largest: $now %.")
    if (!larger && now <= PANE_MIN) return PaneStep(false, PANE_MIN, "$name off.")
    val next = (if (larger) (now / PANE_STEP + 1) * PANE_STEP else ((now + PANE_STEP - 1) / PANE_STEP - 1) * PANE_STEP).coerceIn(PANE_MIN, PANE_MAX)
    return PaneStep(true, next, "$name: $next %.")
}

/** How many of [cells] a slider fills for [value] inside [min]..[max]: none at the minimum, at least one for anything above it, all at the maximum. */
fun sliderCells(value: Double, min: Double, max: Double, cells: Int): Int {
    if (cells <= 0 || max <= min) return 0
    val share = ((value - min) / (max - min)).coerceIn(0.0, 1.0)
    val n = (share * cells).roundToInt().coerceIn(0, cells)
    return if (n == 0 && value > min + 1e-9) 1 else n
}

/** A pane that wants to be shown: where, and how large in percent (of the width for a column, of the main part's height for a strip). */
class DockRequest(val id: PaneId, val dock: Dock, val percent: Int)

/** A pane that fits: [size] is its columns (left, right) or rows (top, bottom), borders included. */
class PaneBox(val id: PaneId, val dock: Dock, val size: Int)

/** The result of [planDocks]: the room left to the main part and the panes that fit, in the order of the request. */
class DockPlan(val mainWidth: Int, val mainHeight: Int, val panes: List<PaneBox>) {
    fun at(dock: Dock) = panes.filter { it.dock == dock }
}

/** Columns between a docked column and the main part. */
const val DOCK_GAP = 1

/** The main part keeps at least this many columns / rows; a column needs a window [COLUMN_MIN_WIDTH] wide, a strip one [STRIP_MIN_HEIGHT] tall. */
const val MAIN_MIN_WIDTH = 40
const val MAIN_MIN_ROWS = 5
const val COLUMN_MIN_WIDTH = 80
const val STRIP_MIN_HEIGHT = 26

/**
 * Lays [requests] out in a [width] × [height] area. Columns take their share of the width (the main part keeps [MAIN_MIN_WIDTH]) and the full height;
 * strips take their share of what height is left for the main part (it keeps [MAIN_MIN_ROWS]). A pane that does not fit (a window narrower than
 * [COLUMN_MIN_WIDTH] for a column, shorter than [STRIP_MIN_HEIGHT] for a strip) is left out, as is a second pane on a side that is already taken.
 */
fun planDocks(width: Int, height: Int, requests: List<DockRequest>): DockPlan {
    val taken = HashSet<Dock>()
    val wanted = requests.filter { r ->
        val fits = if (r.dock.isColumn) width >= COLUMN_MIN_WIDTH else height >= STRIP_MIN_HEIGHT
        fits && taken.add(r.dock)
    }
    var mainWidth = width
    val boxes = ArrayList<PaneBox>()
    for (r in wanted.filter { it.dock.isColumn }) {
        val share = (width * r.percent.coerceIn(PANE_MIN, PANE_MAX) / 100.0).roundToInt()
        val size = share.coerceAtMost(mainWidth - DOCK_GAP - MAIN_MIN_WIDTH).coerceAtLeast(0)
        if (size < 12) continue // too narrow to hold anything
        boxes += PaneBox(r.id, r.dock, size)
        mainWidth -= size + DOCK_GAP
    }
    var mainHeight = height
    for (r in wanted.filter { !it.dock.isColumn }) {
        val share = (height * r.percent.coerceIn(PANE_MIN, PANE_MAX) / 100.0).roundToInt()
        val size = share.coerceAtMost(mainHeight - MAIN_MIN_ROWS).coerceAtLeast(0)
        if (size < 4) continue // a frame and two rows at least
        boxes += PaneBox(r.id, r.dock, size)
        mainHeight -= size
    }
    return DockPlan(mainWidth, mainHeight, wanted.mapNotNull { r -> boxes.firstOrNull { it.id == r.id } })
}

/** Why [pane] cannot be switched on at [dock] given the panes already on, or null if it can. A side holds ONE panel. */
fun dockRefusal(pane: PaneId, dock: Dock, others: List<Pair<PaneId, Dock>>): String? {
    if (pane == PaneId.Status && !dock.isColumn) return "The status pane is a column: dock it on the left or the right."
    val other = others.firstOrNull { it.first != pane && it.second == dock } ?: return null
    return "The ${other.first.name.lowercase()} pane is already on the ${dock.id}: switch it off or move it first (a side holds one panel)."
}

/** A rectangle of a [DockPlan] laid out in cells: [label] is `main` or the pane's name. */
class PaneRect(val label: String, val x: Int, val y: Int, val width: Int, val height: Int)

/**
 * The plan as rectangles in a [width] × [height] area: left columns, then the main part between its strips, then right columns, exactly as `Docked` places them.
 * For diagrams and for tests of the layout.
 */
fun DockPlan.rects(width: Int, height: Int): List<PaneRect> {
    val out = ArrayList<PaneRect>()
    var x = 0
    at(Dock.Left).forEach { out += PaneRect(it.id.name.lowercase(), x, 0, it.size, height); x += it.size + DOCK_GAP }
    val mainX = x
    var y = 0
    at(Dock.Top).forEach { out += PaneRect(it.id.name.lowercase(), mainX, y, mainWidth, it.size); y += it.size }
    out += PaneRect("main", mainX, y, mainWidth, mainHeight)
    y += mainHeight
    at(Dock.Bottom).forEach { out += PaneRect(it.id.name.lowercase(), mainX, y, mainWidth, it.size); y += it.size }
    x = mainX + mainWidth
    at(Dock.Right).forEach { x += DOCK_GAP; out += PaneRect(it.id.name.lowercase(), x, 0, it.size, height); x += it.size }
    return out
}
