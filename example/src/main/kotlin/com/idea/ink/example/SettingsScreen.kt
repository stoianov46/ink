package com.idea.ink.example

import androidx.compose.runtime.Composable
import com.idea.ink.layout.Dock
import com.idea.ink.layout.PANE_MAX
import com.idea.ink.layout.PANE_MIN
import com.idea.ink.layout.PANE_STEP
import com.idea.ink.layout.PaneId
import com.idea.ink.layout.dockRefusal
import com.idea.ink.settings.InkSettings
import com.idea.ink.theme.Themes
import com.idea.ink.ui.HGap
import com.idea.ink.ui.Label
import com.idea.ink.ui.LocalContentWidth
import com.idea.ink.ui.Palette
import com.idea.ink.ui.Panel
import com.idea.ink.ui.ScreenHeader
import com.idea.ink.ui.Stack
import com.idea.ink.ui.VGap
import com.jakewharton.mosaic.layout.background
import com.jakewharton.mosaic.layout.fillMaxWidth
import com.jakewharton.mosaic.layout.width
import com.jakewharton.mosaic.modifier.Modifier
import com.jakewharton.mosaic.ui.Color
import com.jakewharton.mosaic.ui.Row
import com.jakewharton.mosaic.ui.TextStyle

/** The outcome of changing a setting: the new settings, and something to tell the user (an explanation, or why it was refused). */
class Change(val settings: InkSettings, val message: String? = null, val error: Boolean = false)

/** One row of the Settings screen: its label, what its value reads, and what ← / → (−1 / +1) do to it. */
class SettingRow(val group: String, val label: String, val about: String, val value: (InkSettings) -> String, val change: (InkSettings, Int) -> Change)

private fun <T> List<T>.step(current: T, dir: Int): T = this[(indexOf(current) + dir).mod(size)]
private fun onOff(b: Boolean) = if (b) "on" else "off"

private fun otherPanes(s: InkSettings, except: PaneId): List<Pair<PaneId, Dock>> = listOfNotNull(
    if (s.statusPane && except != PaneId.Status) PaneId.Status to Dock.of(s.statusPaneDock, Dock.Right) else null,
    if (s.thinkingPane && except != PaneId.Thinking) PaneId.Thinking to Dock.of(s.thinkingPaneDock, Dock.Bottom) else null,
)

private fun percent(p: Int, dir: Int) = (p + dir * PANE_STEP).coerceIn(PANE_MIN, PANE_MAX)

val SETTING_ROWS: List<SettingRow> = listOf(
    SettingRow("Appearance", "Theme", "colours of every screen", { Themes.byId(it.theme).name }, { s, d ->
        val t = Themes.all.step(Themes.byId(s.theme), d); Change(s.copy(theme = t.id), "Theme: ${t.name}")
    }),
    SettingRow("Appearance", "Navigation", "where the screens are listed: left, top or right (n from anywhere)", { it.navPosition }, { s, d ->
        val p = InkSettings.NAV_POSITIONS.step(s.navPosition, d); Change(s.copy(navPosition = p), "Navigation: $p")
    }),
    SettingRow("Appearance", "Sidebar", "folds to icons by itself below 90 columns (Ctrl+B)", { if (it.sidebarCollapsed) "folded" else "expanded" }, { s, _ ->
        Change(s.copy(sidebarCollapsed = !s.sidebarCollapsed))
    }),
    SettingRow("Appearance", "Density", "compact packs sooner, spacious keeps the roomy layouts longer", { it.uiScale }, { s, d ->
        Change(s.copy(uiScale = InkSettings.UI_SCALES.step(s.uiScale, d)))
    }),
    SettingRow("Appearance", "Plain glyphs", "words and ASCII instead of decorative characters", { onOff(it.plainGlyphs) }, { s, _ -> Change(s.copy(plainGlyphs = !s.plainGlyphs)) }),
    SettingRow("Appearance", "Animated logo", "a rainbow flows along the logo while a reply is generated", { onOff(it.animateLogo) }, { s, _ -> Change(s.copy(animateLogo = !s.animateLogo)) }),

    SettingRow("Console panes", "Status pane", "model, sampling and context window in a column ({ wider, } narrower)", { onOff(it.statusPane) }, { s, _ ->
        val refusal = if (!s.statusPane) dockRefusal(PaneId.Status, Dock.of(s.statusPaneDock, Dock.Right), otherPanes(s, PaneId.Status)) else null
        if (refusal != null) Change(s, refusal, error = true) else Change(s.copy(statusPane = !s.statusPane))
    }),
    SettingRow("Console panes", "Status dock", "the column sits on the left or the right", { it.statusPaneDock }, { s, d ->
        val dock = listOf("left", "right").step(s.statusPaneDock, d)
        val refusal = if (s.statusPane) dockRefusal(PaneId.Status, Dock.of(dock, Dock.Right), otherPanes(s, PaneId.Status)) else null
        if (refusal != null) Change(s, refusal, error = true) else Change(s.copy(statusPaneDock = dock))
    }),
    SettingRow("Console panes", "Status width", "percent of the width, 10 to 90", { "${it.statusPanePercent} %" }, { s, d -> Change(s.copy(statusPanePercent = percent(s.statusPanePercent, d))) }),
    SettingRow("Console panes", "Thinking pane", "the model's thoughts beside the conversation ([ smaller, ] larger)", { onOff(it.thinkingPane) }, { s, _ ->
        val refusal = if (!s.thinkingPane) dockRefusal(PaneId.Thinking, Dock.of(s.thinkingPaneDock, Dock.Bottom), otherPanes(s, PaneId.Thinking)) else null
        if (refusal != null) Change(s, refusal, error = true) else Change(s.copy(thinkingPane = !s.thinkingPane))
    }),
    SettingRow("Console panes", "Thinking dock", "bottom, top, left or right; a side holds one panel", { it.thinkingPaneDock }, { s, d ->
        val dock = Dock.entries.map { it.id }.step(s.thinkingPaneDock, d)
        val refusal = if (s.thinkingPane) dockRefusal(PaneId.Thinking, Dock.of(dock, Dock.Bottom), otherPanes(s, PaneId.Thinking)) else null
        if (refusal != null) Change(s, refusal, error = true) else Change(s.copy(thinkingPaneDock = dock))
    }),
    SettingRow("Console panes", "Thinking size", "percent of the conversation's height or of the width, 10 to 90", { "${it.thinkingPanePercent} %" }, { s, d -> Change(s.copy(thinkingPanePercent = percent(s.thinkingPanePercent, d))) }),
    SettingRow("Reset", "Defaults", "Enter restores every setting: left nav, thinking left, status right", { "restore" }, { _, _ -> Change(InkSettings(), "Settings are back to their defaults") }),
)

@Composable
private fun SettingLine(row: SettingRow, on: Boolean, labelWidth: Int) {
    val bg = if (on) Palette.Selected else Color.Unspecified
    Row(Modifier.fillMaxWidth().background(bg)) {
        Label(" ${row.label.padEnd(labelWidth)}", color = Palette.Text, background = bg, textStyle = if (on) TextStyle.Bold else TextStyle.Unspecified)
        Label(" ‹ ", color = Palette.Dim, background = bg)
        Label(row.value(LocalSettings.current), color = Palette.Cta, background = bg, textStyle = TextStyle.Bold)
        Label(" ›", color = Palette.Dim, background = bg)
        Label("  ${row.about}", Modifier.weight(1f), color = Palette.Muted, background = bg, fill = true)
    }
}

/** The settings being drawn, so a row reads its own value. */
val LocalSettings = androidx.compose.runtime.compositionLocalOf { InkSettings() }

/** A small drawing of where the navigation sits: the navigation in the accent colour, the content muted. */
@Composable
internal fun NavPreview(position: String) {
    val nav = Palette.Accent
    val body = Palette.Dim
    Stack {
        when (position) {
            "top" -> {
                Label("┌─────────────────┐", color = body)
                Row { Label("│", color = body); Label("▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒", color = nav); Label("│", color = body) }
                Label("├─────────────────┤", color = body)
                repeat(3) { Label("│                 │", color = body) }
                Label("└─────────────────┘", color = body)
            }
            "right" -> {
                Label("┌──────────┬──────┐", color = body)
                repeat(5) { Row { Label("│          │", color = body); Label("▒▒▒▒▒▒", color = nav); Label("│", color = body) } }
                Label("└──────────┴──────┘", color = body)
            }
            else -> {
                Label("┌──────┬──────────┐", color = body)
                repeat(5) { Row { Label("│", color = body); Label("▒▒▒▒▒▒", color = nav); Label("│          │", color = body) } }
                Label("└──────┴──────────┘", color = body)
            }
        }
        Label("navigation: $position", color = Palette.Muted)
    }
}

@Composable
fun SettingsScreen(app: InkApp) {
    val s = app.settings
    val labelWidth = SETTING_ROWS.maxOf { it.label.length }
    val groups = SETTING_ROWS.withIndex().groupBy { it.value.group }
    val wide = LocalContentWidth.current >= 100
    androidx.compose.runtime.CompositionLocalProvider(LocalSettings provides s) {
        Stack {
            ScreenHeader("Settings", "saved at every change · ←→ or Enter change the highlighted setting")
            VGap()
            Row(Modifier.fillMaxWidth()) {
                Stack(Modifier.weight(1f)) {
                    groups.forEach { (name, rows) ->
                        Panel(name) { Stack { rows.forEach { (i, row) -> SettingLine(row, app.selected == i, labelWidth) } } }
                    }
                }
                if (wide) {
                    HGap()
                    Panel("Preview", Modifier.width(30)) { NavPreview(s.navPosition) }
                }
            }
        }
    }
}
