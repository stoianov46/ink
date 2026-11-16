package com.idea.ink.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import com.idea.ink.terminal.CellSize
import com.idea.ink.terminal.ResizeSignalGuard
import com.idea.ink.terminal.TerminalSize
import com.jakewharton.mosaic.LocalTerminalState
import com.jakewharton.mosaic.layout.background
import com.jakewharton.mosaic.layout.fillMaxHeight
import com.jakewharton.mosaic.layout.fillMaxWidth
import com.jakewharton.mosaic.layout.padding
import com.jakewharton.mosaic.layout.size
import com.jakewharton.mosaic.layout.width
import com.jakewharton.mosaic.modifier.Modifier
import com.jakewharton.mosaic.ui.Box
import com.jakewharton.mosaic.ui.Color
import com.jakewharton.mosaic.ui.Column
import com.jakewharton.mosaic.ui.Row
import com.jakewharton.mosaic.ui.RowScope
import com.jakewharton.mosaic.ui.Text
import com.jakewharton.mosaic.ui.TextStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** The keys that open the screens of a shell by number: the first screen is `1`, the tenth `0`. */
const val ShortcutKeys = "1234567890"

/** One screen of a shell: its [title], a one-cell [icon] (shown on the folded rail), and the [group] heading it sits under in the sidebar ("" for none). */
class NavItem(val title: String, val icon: String, val group: String = "")

/** Where the screens are listed: down the left or the right side (folds to icons), or as one row across the top. */
enum class NavPosition { Left, Top, Right;

    /** The id the settings file uses (`left`, `top`, `right`). */
    val id: String get() = name.lowercase()

    companion object {
        fun of(id: String?): NavPosition = entries.firstOrNull { it.id == id } ?: Left
    }
}

/** A message that replaces the key hints in the footer for a few seconds. */
class ShellToast(val text: String, val isError: Boolean = false)

/**
 * The size of the terminal: the real one from the OS (polled every half second, so window resizes are caught), falling back to what Mosaic
 * reports. Also replaces Mosaic's crashing native resize handler once it is installed (see [ResizeSignalGuard]).
 * @param envPrefix prefix of the `<PREFIX>_COLUMNS` / `<PREFIX>_ROWS` overrides
 */
@Composable
fun rememberTerminalSize(envPrefix: String = "INK"): CellSize {
    val mosaicSize = LocalTerminalState.current.size
    LaunchedEffect(Unit) {
        delay(300)
        ResizeSignalGuard.install()
    }
    val probed by produceState<CellSize?>(null) {
        while (true) {
            value = withContext(Dispatchers.IO) { TerminalSize.probe(System.getenv(), envPrefix) }
            delay(500)
        }
    }
    return probed ?: CellSize(mosaicSize.columns, mosaicSize.rows)
}

/**
 * The frame around every screen, the way Arcana draws it: a sidebar with the app's logo, the screens grouped under headings with their
 * number keys and a fold button (or a [NavPosition.Top] bar), the screen's own [content], and a footer bar with status facts and key hints.
 *
 * The shell draws only; the app owns the state and the keys: pass `Modifier.onKeyEvent { … }` as [modifier] and decide what the number keys,
 * Ctrl+B (fold) and Ctrl+C do. Below `Dimens.MinColumns × MinRows` it shows an "enlarge the window" message instead.
 *
 * @param items the screens, in sidebar order; a screen's position decides its number key
 * @param current index of the screen on show
 * @param size the terminal size, e.g. from [rememberTerminalSize]
 * @param collapsed fold the sidebar to a rail of icons (it folds by itself below 90 columns)
 * @param status the facts of the footer's left side, wrapped onto as many rows as they need
 * @param hints the key hints of the footer's right side, `"key  what   key  what"` (two spaces inside a hint, three between hints)
 * @param badge a count or marker on a screen's entry (`"3" to color`), or null
 */
@Composable
fun InkShell(
    items: List<NavItem>,
    current: Int,
    size: CellSize,
    modifier: Modifier = Modifier,
    logo: String = "⟐ ink",
    collapsed: Boolean = false,
    position: NavPosition = NavPosition.Left,
    busy: Boolean = false,
    status: List<StatusSegment> = emptyList(),
    hints: String = "",
    toast: ShellToast? = null,
    quitPending: Boolean = false,
    uiScale: String = "normal",
    plainGlyphs: Boolean = false,
    badge: (Int) -> Pair<String, Color>? = { null },
    content: @Composable () -> Unit,
) {
    val viewRows = size.rows - Dimens.ReservedRows
    if (size.columns < Dimens.MinColumns || viewRows < Dimens.MinRows) {
        Column(modifier) {
            Text("Terminal too small: ${size.columns}x${size.rows}", color = Palette.Orange, textStyle = TextStyle.Bold)
            Text("Need at least ${Dimens.MinColumns}x${Dimens.MinRows + Dimens.ReservedRows}. Enlarge the window, Ctrl+C to quit.")
        }
        return
    }
    val rail = collapsed || size.columns < 90
    val top = position == NavPosition.Top
    val sidebarWidth = if (top) 0 else if (rail) 7 else (size.columns / 5).coerceIn(18, Dimens.SidebarWidth)
    val vPad = if (viewRows < 24) 0 else 1
    val footerRows = footerRows(status, size.columns)

    CompositionLocalProvider(LocalUiScale provides uiScale, LocalPlainGlyphs provides plainGlyphs) {
        Column(modifier.size(size.columns, viewRows).background(Palette.Background)) {
            if (top) TopBar(items, current, size.columns, logo, busy, badge)
            Row(Modifier.weight(1f).fillMaxWidth()) {
                if (position == NavPosition.Left) Sidebar(items, current, sidebarWidth, rail, viewRows, logo, busy, badge)
                // Breathing room above the content, but none below it: the last row belongs to the content, right above the footer.
                Box(Modifier.weight(1f).fillMaxHeight().padding(left = Dimens.ContentPadding, top = vPad, right = Dimens.ContentPadding, bottom = 0)) {
                    CompositionLocalProvider(
                        LocalContentHeight provides viewRows - footerRows.size - vPad - (if (top) 1 else 0),
                        LocalContentWidth provides size.columns - sidebarWidth - 2 * Dimens.ContentPadding,
                    ) {
                        content()
                    }
                }
                if (position == NavPosition.Right) Sidebar(items, current, sidebarWidth, rail, viewRows, logo, busy, badge)
            }
            Footer(footerRows, hints, toast, quitPending, size.columns)
        }
    }
}

private fun shortcut(i: Int) = ShortcutKeys.getOrElse(i) { ' ' }

/** Navigation as one row across the top: the logo, then every screen as `number title`, the current one lit; when the titles do not fit, the others shrink to `number icon`. */
@Composable
private fun TopBar(items: List<NavItem>, current: Int, columns: Int, logo: String, busy: Boolean, badge: (Int) -> Pair<String, Color>?) {
    fun badgeText(i: Int) = badge(i)?.first?.let { " $it" } ?: ""
    fun full(i: Int, s: NavItem) = " ${shortcut(i)} ${s.title}${badgeText(i)} "
    val logoWidth = logo.length + 2
    val fits = logoWidth + items.withIndex().sumOf { (i, s) -> full(i, s).length + 1 } <= columns
    Row(Modifier.fillMaxWidth().background(Palette.Surface)) {
        Label(" ", background = Palette.Surface)
        RainbowText(logo, busy, Palette.Accent)
        Label("  ", background = Palette.Surface)
        items.forEachIndexed { i, s ->
            val on = i == current
            val b = badge(i)
            val text = if (fits || on) full(i, s) else " ${shortcut(i)} ${s.icon}${if (b != null) "•" else ""} "
            Label(
                text,
                color = if (on) Palette.Text else if (b != null && !fits) b.second else Palette.Muted,
                background = if (on) Palette.TabOnAccent else Palette.Surface,
                textStyle = if (on) TextStyle.Bold else TextStyle.Unspecified,
            )
            Label(" ", background = Palette.Surface)
        }
        Label("", Modifier.weight(1f), background = Palette.Surface, fill = true)
    }
}

@Composable
private fun Sidebar(items: List<NavItem>, current: Int, width: Int, collapsed: Boolean, rows: Int, logo: String, busy: Boolean, badge: (Int) -> Pair<String, Color>?) {
    val roomy = rows >= 24
    Column(Modifier.width(width).fillMaxHeight().background(Palette.Surface).padding(horizontal = 1, vertical = if (roomy) 1 else 0)) {
        Stack(Modifier.weight(1f)) {
            RainbowText(if (collapsed) " ${logo.take(1)}" else " $logo", busy, Palette.Accent)
            if (roomy) VGap()
            items.forEachIndexed { i, s ->
                if (!collapsed && roomy && s.group.isNotEmpty() && s.group != items.getOrNull(i - 1)?.group) {
                    if (i > 0) VGap()
                    Label(s.group, color = Palette.Muted, textStyle = TextStyle.Bold)
                }
                val on = i == current
                val b = badge(i)
                Row(Modifier.fillMaxWidth().background(if (on) Palette.Selected else Color.Unspecified)) {
                    Label(
                        if (collapsed) "${shortcut(i)} ${s.icon}" else " ${shortcut(i)}  ${s.title}",
                        modifier = Modifier.weight(1f),
                        color = if (on) Palette.OnSelected else Palette.Muted,
                        textStyle = if (on) TextStyle.Bold else TextStyle.Unspecified,
                        fill = true,
                    )
                    if (b != null) {
                        if (collapsed) Label("●", color = b.second) else Label(" ${b.first} ", color = Palette.Ink, background = b.second, textStyle = TextStyle.Bold)
                    }
                }
            }
            if (roomy) VGap()
            Row(Modifier.fillMaxWidth()) {
                Label(if (collapsed) "^B" else " Ctrl+B ", color = Palette.Ink, background = Palette.Cta, textStyle = TextStyle.Bold)
                if (!collapsed) Label(" ‹ Fold", Modifier.weight(1f), color = Palette.Text, background = Palette.SurfaceHigh, fill = true)
            }
        }
    }
}

private fun footerRows(status: List<StatusSegment>, columns: Int): List<List<StatusSegment>> = wrapSegments(status, columns - 2, 3)

private fun rowWidth(row: List<StatusSegment>): Int = row.sumOf { it.width } + 3 * (row.size - 1).coerceAtLeast(0) + 1

@Composable
private fun Footer(rows: List<List<StatusSegment>>, hints: String, toast: ShellToast?, quitPending: Boolean, columns: Int) {
    val separator = if (LocalPlainGlyphs.current) " | " else " │ "
    val bar = Palette.SurfaceHigh
    Column(Modifier.fillMaxWidth()) {
        rows.ifEmpty { listOf(emptyList()) }.forEachIndexed { index, row ->
            Row(Modifier.fillMaxWidth().background(bar)) {
                Label(" ", background = bar)
                row.forEachIndexed { i, segment ->
                    if (i > 0) Label(separator, color = Palette.Dim, background = bar)
                    segment.parts.forEach { part -> Label(part.text, color = part.color, background = bar, textStyle = if (part.bold) TextStyle.Bold else TextStyle.Unspecified) }
                }
                if (index < rows.lastIndex) {
                    Label("", Modifier.weight(1f), background = bar, fill = true)
                } else {
                    val room = columns - rowWidth(row) - 1
                    if (toast != null && !quitPending) {
                        Label("", Modifier.weight(1f), background = bar, fill = true)
                        Label(" ${if (toast.isError) "✕" else "✓"} ${toast.text} ".take(room.coerceAtLeast(10)), color = if (toast.isError) Palette.Red else Palette.Green, background = bar, textStyle = TextStyle.Bold)
                        Label(" ", background = bar)
                    } else {
                        FooterHints(if (quitPending) hints else fitHints(hints, room), badges = !quitPending)
                    }
                }
            }
        }
    }
}

/** Only the hints that fit, whole; the rest are one key map away. */
fun fitHints(hint: String, width: Int): String {
    val parts = hint.split("   ").filter { it.isNotBlank() }
    val kept = mutableListOf<String>()
    var used = 0
    for (part in parts) {
        val key = part.substringBefore("  ")
        val w = key.length + 3 + part.substringAfter("  ", "").trim().length + if (kept.isEmpty()) 0 else 2
        if (used + w > width) break
        kept += part
        used += w
    }
    return kept.joinToString("   ")
}

/** One footer line: keys are coloured, the explanation is muted; the hints sit at the right edge. */
@Composable
private fun RowScope.FooterHints(line: String, badges: Boolean) {
    val bar = Palette.SurfaceHigh
    Label("", Modifier.weight(1f), background = bar, fill = true)
    if (!badges) {
        Label(line, color = Palette.Warn, background = bar, textStyle = TextStyle.Bold)
        return
    }
    line.split("   ").filter { it.isNotBlank() }.forEachIndexed { i, part ->
        if (i > 0) Label("  ", background = bar)
        val key = part.substringBefore("  ", "")
        if (key.isEmpty()) {
            Label(part, color = Palette.Muted, background = bar)
        } else {
            Label(key, color = Palette.Accent, background = bar, textStyle = TextStyle.Bold)
            Label(" ${part.substringAfter("  ").trim()}", color = Palette.Muted, background = bar)
        }
    }
    Label(" ", background = bar)
}
