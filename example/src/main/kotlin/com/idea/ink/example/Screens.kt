package com.idea.ink.example

import androidx.compose.runtime.Composable
import com.idea.ink.colors.ColorSupport
import com.idea.ink.colors.Rgb
import com.idea.ink.colors.contrast
import com.idea.ink.colors.toHex
import com.idea.ink.terminal.CellSize
import com.idea.ink.text.sanitizeForTerminal
import com.idea.ink.theme.InkColors
import com.idea.ink.theme.RequiredRoles
import com.idea.ink.theme.ThemeSpec
import com.idea.ink.theme.Themes
import com.idea.ink.ui.ActionBar
import com.idea.ink.ui.ActionItem
import com.idea.ink.ui.ActionStyle
import com.idea.ink.ui.Badge
import com.idea.ink.ui.Card
import com.idea.ink.ui.HBar
import com.idea.ink.ui.HGap
import com.idea.ink.ui.Label
import com.idea.ink.ui.LocalContentHeight
import com.idea.ink.ui.LocalContentWidth
import com.idea.ink.ui.Palette
import com.idea.ink.ui.Panel
import com.idea.ink.ui.ScreenHeader
import com.idea.ink.ui.Stack
import com.idea.ink.ui.StatusBar
import com.idea.ink.ui.TabBar
import com.idea.ink.ui.TabLabel
import com.idea.ink.ui.VGap
import com.idea.ink.ui.gaugeSegment
import com.idea.ink.ui.meterSegment
import com.idea.ink.ui.statusSegment
import com.idea.ink.ui.toMosaic
import com.jakewharton.mosaic.layout.background
import com.jakewharton.mosaic.layout.fillMaxWidth
import com.jakewharton.mosaic.modifier.Modifier
import com.jakewharton.mosaic.ui.Color
import com.jakewharton.mosaic.ui.Row
import com.jakewharton.mosaic.ui.TextStyle

// ───────────────────────────── building blocks ─────────────────────────────

/** A one-line section heading inside a panel, like BOTTLENECKS or NEXT UP on Arcana's Brain screen. */
@Composable
internal fun Heading(text: String) = Label(text, color = Palette.Muted, textStyle = TextStyle.Bold)

/** The colour a list row's icon takes; resolved while composing, so a theme change repaints it. */
enum class Tone { Accent, Cta, Orange, Muted, Soft }

@Composable
private fun Tone.color(): Color = when (this) {
    Tone.Accent -> Palette.Accent
    Tone.Cta -> Palette.Cta
    Tone.Orange -> Palette.Orange
    Tone.Muted -> Palette.Muted
    Tone.Soft -> Palette.CyanSoft
}

/** One selectable row: icon, title, a muted detail that takes the rest of the line; the selected row sits on [Palette.Selected]. */
@Composable
internal fun ListRow(selected: Boolean, icon: String, tone: Tone, title: String, detail: String = "") {
    val bg = if (selected) Palette.Selected else Color.Unspecified
    Row(Modifier.fillMaxWidth().background(bg)) {
        Label(" $icon ", color = tone.color(), background = bg, textStyle = TextStyle.Bold)
        Label(title, color = Palette.Text, background = bg, textStyle = if (selected) TextStyle.Bold else TextStyle.Unspecified)
        Label("  $detail", Modifier.weight(1f), color = Palette.Muted, background = bg, fill = true)
    }
}

/** A colour swatch: [width] cells painted [rgb] (drawn as its real colour even in a see-through theme). */
@Composable
private fun Chip(rgb: Rgb, width: Int = 2) = Label(" ".repeat(width), background = rgb.toMosaic())

@Composable
private fun SwatchStrip(spec: ThemeSpec) = Row { Themes.swatchColors(spec).forEach { Chip(it) } }

/** The WCAG grade of a contrast ratio with the colour that says it. */
@Composable
private fun grade(ratio: Double): Pair<String, Color> = when {
    ratio >= 7.0 -> "AAA ✓" to Palette.Green
    ratio >= 4.5 -> "AA ✓" to Palette.Green
    ratio >= 3.0 -> "large only ⚠" to Palette.Orange
    else -> "fails ✕" to Palette.Red
}

@Composable
private fun RatioLine(label: String, fg: Rgb, bg: Rgb) {
    val ratio = contrast(fg, bg)
    val (grade, color) = grade(ratio)
    Row {
        Chip(fg); Chip(bg, 1)
        Label(" ${label.padEnd(16)}", color = Palette.Text)
        Label("${"%.1f".format(ratio)}:1".padStart(7), color = color, textStyle = TextStyle.Bold)
        Label("  $grade", color = color)
    }
}

/** A key badge and what it does, like the key map of Arcana. */
@Composable
private fun KeyRow(key: String, text: String, keyWidth: Int, accent: Color = Palette.Accent) {
    Row {
        Label(" ${key.padEnd(keyWidth)} ", color = Palette.Ink, background = accent, textStyle = TextStyle.Bold)
        Label(" $text", color = Palette.Text)
    }
}

/** The first row of the rows [selected] keeps in view in a window of [visible] rows over [total]. */
private fun windowStart(selected: Int, visible: Int, total: Int) = (selected - visible / 2).coerceIn(0, (total - visible).coerceAtLeast(0))

// ───────────────────────────── Overview (Arcana's Brain screen) ─────────────────────────────

class Entry(val heading: String?, val icon: String, val tone: Tone, val title: String, val detail: String, val screen: Screen? = null, val action: ((InkApp) -> Unit)? = null)

val OVERVIEW_ENTRIES = listOf(
    Entry("AN APP WITH A MODEL · watch it work", "»", Tone.Accent, "Console", "thinking, tool calls, approval and a streaming answer, in split panes", Screen.Console),
    Entry(null, "↻", Tone.Orange, "Replay the session", "r in the Console", action = { it.go(Screen.Console); it.restart() }),
    Entry("COMPONENTS · what to build with", "▤", Tone.Accent, "The catalog", "${SPECIMENS.size} pieces: browse them like a Storybook", Screen.Components),
    Entry(null, "❯", Tone.Cta, "Progress, charts, inputs, scrolling", "every piece, live, with its code", Screen.Components),
    Entry(null, "▥", Tone.Soft, "Text, selection and key handling", "the behaviours you try with the keyboard", Screen.Components),
    Entry("THEMES · pick a look", "◐", Tone.Accent, "Built-in themes", "${Themes.all.size} of them, Arcana's and CosmoCLI's", Screen.Themes),
    Entry(null, "▥", Tone.Muted, "Colour roles", "12 required, the rest derived", Screen.Colours),
    Entry("SETTINGS", "✱", Tone.Muted, "Navigation and panes", "sidebar left, top or right; dock the panes", Screen.Settings),
    Entry(null, "▣", Tone.Muted, "Terminal", "size, colour level, sanitizing", Screen.Terminal),
    Entry(null, "¶", Tone.Muted, "Guide", "keys and how the pieces fit", Screen.Guide),
)

@Composable
fun OverviewScreen(app: InkApp) {
    val theme = app.theme
    val wide = LocalContentWidth.current >= 100
    Stack {
        ScreenHeader("Ink", "Terminal design system · extracted from Arcana and CosmoCLI")
        VGap()
        // the vitals line, like Arcana's: quiet facts with coloured values
        StatusBar(
            listOf(
                statusSegment("themes", "${Themes.all.size}", Palette.Cta, bold = true),
                statusSegment("roles", "${RequiredRoles.size} required", Palette.Green),
                statusSegment("colour", if (ColorSupport.truecolor) "truecolor" else "256", if (ColorSupport.truecolor) Palette.Blue else Palette.Warn),
                statusSegment("active", theme.name, Palette.Accent, bold = true),
            ),
        )
        Row {
            TabBar(OverviewTab.entries.map { it.title }, app.tab.ordinal)
        }
        VGap()
        if (wide) {
            Row(Modifier.fillMaxWidth()) {
                Panel("Where to go", Modifier.weight(1f)) { EntryList(app) }
                HGap()
                Panel("${app.tab.title} · ${theme.name}", Modifier.weight(1f)) { OverviewTabContent(app.tab, theme) }
            }
        } else {
            Panel("Where to go") { EntryList(app) }
            Panel("${app.tab.title} · ${theme.name}") { OverviewTabContent(app.tab, theme) }
        }
    }
}

@Composable
private fun EntryList(app: InkApp) {
    Stack {
        OVERVIEW_ENTRIES.forEachIndexed { i, e ->
            if (e.heading != null) {
                if (i > 0) VGap()
                Heading(e.heading)
            }
            ListRow(app.selected == i, e.icon, e.tone, e.title, e.detail)
        }
    }
}

@Composable
private fun OverviewTabContent(tab: OverviewTab, theme: ThemeSpec) {
    val ink = InkColors(theme)
    when (tab) {
        OverviewTab.Theme -> Stack {
            Label(theme.name, color = Palette.Text, textStyle = TextStyle.Bold)
            Label("${theme.id} · ${if (ink.light) "light" else "dark"}${if (theme.transparent) " · see-through" else ""} · ${theme.colors.size} colours set", color = Palette.Muted)
            VGap()
            SwatchStrip(theme)
            VGap()
            Heading("READABILITY")
            RatioLine("text", ink.text, ink.background)
            RatioLine("muted", ink.muted, ink.background)
            RatioLine("accent", ink.accent, ink.background)
            RatioLine("cta", ink.cta, ink.background)
        }
        OverviewTab.Roles -> Stack {
            RequiredRoles.forEach { role ->
                val rgb = theme.colors.getValue(role)
                Row { Chip(rgb, 3); Label(" ${role.padEnd(12)}", color = Palette.Text); Label(toHex(rgb), color = Palette.Muted) }
            }
        }
        OverviewTab.Contrast -> Stack {
            Heading("ON THE BACKGROUND")
            listOf("green" to ink.green, "red" to ink.red, "blue" to ink.blue, "orange" to ink.orange, "purple" to ink.purple, "cyan" to ink.cyan).forEach { (name, rgb) ->
                RatioLine(name, rgb, ink.background)
            }
        }
    }
}

// ───────────────────────────── Themes ─────────────────────────────

@Composable
fun ThemesScreen(app: InkApp) {
    val all = Themes.all
    // header 2 + gap 1 + the panel's frame 2
    val visible = (LocalContentHeight.current - 5).coerceAtLeast(3).coerceAtMost(all.size)
    val first = windowStart(app.selected, visible, all.size)
    Stack {
        ScreenHeader("Themes", "${all.size} built-in · Enter applies the highlighted one")
        VGap()
        Panel("Built-in themes") {
            Stack {
                for (i in first until (first + visible).coerceAtMost(all.size)) {
                    val t = all[i]
                    val on = app.selected == i
                    val bg = if (on) Palette.Selected else Color.Unspecified
                    Row(Modifier.fillMaxWidth().background(bg)) {
                        Label(if (i == app.themeIndex) " ● " else "   ", color = Palette.Green, background = bg, textStyle = TextStyle.Bold)
                        Label(t.name.padEnd(20), color = Palette.Text, background = bg, textStyle = if (on) TextStyle.Bold else TextStyle.Unspecified)
                        SwatchStrip(t)
                        Label("  ${if (Themes.isLight(t)) "light" else "dark"}${if (t.transparent) " · see-through" else ""}", Modifier.weight(1f), color = Palette.Muted, background = bg, fill = true)
                    }
                }
            }
        }
    }
}

// ───────────────────────────── Colours ─────────────────────────────

@Composable
fun ColoursScreen(app: InkApp) {
    val ink = InkColors(app.theme)
    val derived = listOf(
        "accentBg" to ink.accentBg, "tabOn" to ink.tabOn, "tabOnAccent" to ink.tabOnAccent, "greenSoft" to ink.greenSoft,
        "orangeSoft" to ink.orangeSoft, "chartA" to ink.chartA, "chartB" to ink.chartB, "chartC" to ink.chartC,
        "chartTrack" to ink.chartTrack, "userBg" to ink.userBg, "consoleCodeBg" to ink.consoleCodeBg, "entryBg" to ink.entryBg,
    )
    val wide = LocalContentWidth.current >= 72
    Stack {
        ScreenHeader("Colours", "${app.theme.name} · what the theme sets, and what Ink derives from it")
        VGap()
        val required: @Composable () -> Unit = {
            Stack { RequiredRoles.forEach { r -> val v = app.theme.colors.getValue(r); Row { Chip(v, 3); Label(" ${r.padEnd(12)}", color = Palette.Text); Label(toHex(v), color = Palette.Muted) } } }
        }
        val derivedList: @Composable () -> Unit = {
            Stack { derived.forEach { (n, v) -> Row { Chip(v, 3); Label(" ${n.padEnd(14)}", color = Palette.Text); Label(toHex(v), color = Palette.Muted) } } }
        }
        if (wide) {
            Row(Modifier.fillMaxWidth()) {
                Panel("Set by the theme", Modifier.weight(1f)) { required() }
                HGap()
                Panel("Derived by Ink", Modifier.weight(1f)) { derivedList() }
            }
        } else {
            Panel("Set by the theme") { required() }
        }
    }
}

// ───────────────────────────── Terminal ─────────────────────────────

@Composable
fun TerminalScreen(size: CellSize) {
    val env = System.getenv()
    val raw = "Deploy \u2705 done \u65E5\u672C\u8A9E \u001B[31mred\u001B[0m"
    Stack {
        ScreenHeader("Terminal", "what Ink knows about this window")
        VGap()
        Panel("Size and colour") {
            Stack {
                Row { Label("size".padEnd(12), color = Palette.Muted); Label("${size.columns} × ${size.rows}", color = Palette.Text, textStyle = TextStyle.Bold) }
                Row { Label("colour".padEnd(12), color = Palette.Muted); Label(if (ColorSupport.truecolor) "24-bit truecolor" else "256 colours (dark tints fitted to the cube)", color = if (ColorSupport.truecolor) Palette.Green else Palette.Warn) }
                Row { Label("COLORTERM".padEnd(12), color = Palette.Muted); Label(env["COLORTERM"] ?: "—", color = Palette.Text) }
                Row { Label("TERM".padEnd(12), color = Palette.Muted); Label(env["TERM"] ?: "—", color = Palette.Text) }
                Row { Label("NO_COLOR".padEnd(12), color = Palette.Muted); Label(if (ColorSupport.noColor()) "set" else "not set", color = Palette.Text) }
            }
        }
        Panel("Text is made safe before it is drawn") {
            Stack {
                Row { Label("input".padEnd(12), color = Palette.Muted); Label("an emoji, three CJK letters and an ANSI colour code (${raw.length} chars)", color = Palette.Dim) }
                Row { Label("drawn as".padEnd(12), color = Palette.Muted); Label(sanitizeForTerminal(raw), color = Palette.Text) }
                Row { Label("".padEnd(12)); Label("one cell per character, no escape codes reach the terminal", color = Palette.Muted) }
            }
        }
    }
}

// ───────────────────────────── Guide ─────────────────────────────

private val KEYS = listOf(
    "1-8" to "switch screen",
    "↑ ↓  j k" to "move the selection",
    "Enter" to "open or apply",
    "Tab" to "next page of the Overview",
    "t / T" to "next / previous theme",
    "n" to "navigation: left, top, right",
    "Ctrl+B" to "fold the sidebar",
    "?" to "this key map",
    "Esc" to "back to the Overview",
    "q  Ctrl+C ×2" to "quit",
)

private val CONSOLE_KEYS = listOf(
    "i  Enter" to "type a message (Esc stops, Enter sends)",
    "r" to "replay the session",
    "Space" to "pause / resume",
    "f" to "speed ×1 ×2 ×4",
    "{  }" to "status pane wider / narrower",
    "[  ]" to "thinking pane smaller / larger",
    "↑ ↓  PgUp PgDn" to "scroll the conversation",
    "End" to "follow the newest line",
)

@Composable
private fun KeyList() {
    val all = KEYS + CONSOLE_KEYS
    val width = all.maxOf { it.first.length }
    Stack {
        KEYS.forEach { (k, v) -> KeyRow(k, v, width) }
        VGap()
        Label("In the Console", color = Palette.Muted, textStyle = TextStyle.Bold)
        CONSOLE_KEYS.forEach { (k, v) -> KeyRow(k, v, width, Palette.Cta) }
    }
}

@Composable
fun GuideScreen() {
    Stack {
        ScreenHeader("Guide", "keys, and how the pieces fit")
        VGap()
        Panel("Keys", accent = Palette.Cta) { KeyList() }
        Panel("Modules") {
            Stack {
                Label("ink          colours, themes, terminal probing: plain Kotlin, no dependencies", color = Palette.Text)
                Label("ink-mosaic   Palette, Label, Panel, Stack, tabs, status bar and the shell", color = Palette.Text)
                Label("example      this window, with a mock model session in the Console", color = Palette.Muted)
            }
        }
    }
}

@Composable
fun HelpOverlay() {
    Stack {
        Panel("Keys · any key closes this map", accent = Palette.Cta, background = Palette.Background) { KeyList() }
    }
}
