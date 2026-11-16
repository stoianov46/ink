package com.idea.ink.example

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import com.idea.ink.chart.Chart
import com.idea.ink.input.Selection
import com.idea.ink.layout.Dock
import com.idea.ink.layout.DockRequest
import com.idea.ink.layout.PaneId
import com.idea.ink.layout.planDocks
import com.idea.ink.layout.rects
import com.idea.ink.layout.sliderCells
import com.idea.ink.ui.*
import com.jakewharton.mosaic.layout.fillMaxWidth
import com.jakewharton.mosaic.layout.height
import com.jakewharton.mosaic.layout.size
import com.jakewharton.mosaic.layout.width
import com.jakewharton.mosaic.modifier.Modifier
import com.jakewharton.mosaic.ui.Box
import com.jakewharton.mosaic.ui.Color
import com.jakewharton.mosaic.ui.Row
import com.jakewharton.mosaic.ui.RowScope
import com.jakewharton.mosaic.ui.TextStyle

/** What an interactive piece does with the keyboard once you press Enter on it (Esc gives it back). */
enum class Interaction { Text, KeyReader, ScrollColumn, ScrollView }

/** What a demo is given: the app (for the state of the interactive ones), the columns it may use, and whether it has the keyboard. */
class DemoContext(val app: InkApp, val width: Int, val focused: Boolean)

/** One piece of the design system in the catalog: what it is for, a live demo, and the code that makes it. */
class Specimen(
    val category: String,
    val name: String,
    val summary: String,
    val code: String,
    val interaction: Interaction? = null,
    val demo: @Composable (DemoContext) -> Unit,
)

/** The finished mock session: one entry of every kind, used by the transcript and scroll demos. */
val DEMO_ENTRIES = playSession(DEFAULT_PROMPT, sessionLength()).entries
val DEMO_ENTRY_TEXTS = consoleEntryTexts(DEMO_ENTRIES)

// ───────────────────────────── helpers for the demos ─────────────────────────────

@Composable
private fun Variant(label: String, labelWidth: Int = 14, content: @Composable RowScope.() -> Unit) = Row {
    Label(label.padEnd(labelWidth), color = Palette.Muted)
    content()
}

@Composable
private fun Note(text: String) = Label(text, color = Palette.Muted, wrap = true)

@Composable
private fun Caption(text: String) = Label(text, color = Palette.Dim)

@Composable
private fun KeyTable(rows: List<Pair<String, String>>, keyWidth: Int) {
    Stack { rows.forEach { (k, v) -> Row { Label(" ${k.padEnd(keyWidth)} ", color = Palette.Ink, background = Palette.Accent, textStyle = TextStyle.Bold); Label(" $v", color = Palette.Text) } } }
}

private fun dockDiagram(requests: List<DockRequest>, dw: Int = 46, dh: Int = 9): List<String> {
    val w = 120
    val h = 40
    val plan = planDocks(w, h, requests)
    val grid = Array(dh) { CharArray(dw) { ' ' } }
    for (r in plan.rects(w, h)) {
        val x0 = r.x * dw / w
        val x1 = ((r.x + r.width) * dw / w - 1).coerceAtLeast(x0 + 3).coerceAtMost(dw - 1)
        val y0 = r.y * dh / h
        val y1 = ((r.y + r.height) * dh / h - 1).coerceAtLeast(y0 + 1).coerceAtMost(dh - 1)
        for (x in x0..x1) { grid[y0][x] = '─'; grid[y1][x] = '─' }
        for (y in y0..y1) { grid[y][x0] = '│'; grid[y][x1] = '│' }
        grid[y0][x0] = '╭'; grid[y0][x1] = '╮'; grid[y1][x0] = '╰'; grid[y1][x1] = '╯'
        val label = r.label.take(x1 - x0 - 1)
        val ly = (y0 + y1) / 2
        val lx = x0 + (x1 - x0 + 1 - label.length) / 2
        label.forEachIndexed { i, c -> grid[ly][lx + i] = c }
    }
    return grid.map { String(it) }
}

private val SAMPLE_MARKDOWN = """
## Heading with `code` and **bold**
A paragraph with *italic*, a [link](https://example.com) and figures: 12,310 visits, +1.6%, -4.2%, ${'$'}1,250.

- a bullet with `inline code`
- [x] a finished task
- [ ] an open task

> a quotation

| Week | Signups |
|------|--------:|
| W45  | 1,094   |
| W46  | 897     |
""".trimIndent()

private val EDIT_MOVE = listOf(
    "← →" to "move the caret",
    "Alt+← →" to "by word (Ctrl+← → too)",
    "Home End" to "start / end of the line",
    "Ctrl+A  Ctrl+E" to "start / end of the whole text",
    "↑ ↓" to "between lines, keeping the column",
)
private val EDIT_SELECT = listOf(
    "Shift + a move" to "selects while it moves",
    "Alt+A" to "select all (Cmd+A is turned into it)",
    "type / paste" to "replaces the selection",
    "← →" to "collapse it to its start / end",
)
private val EDIT_DELETE = listOf(
    "Backspace  Delete" to "a character, or the selection",
    "Ctrl+W  Alt+⌫" to "the word before the caret",
    "Ctrl+U" to "to the start of the line",
    "Ctrl+K" to "to the end of the line",
)
private val EDIT_NEWLINE = listOf(
    "Enter" to "sends (the app decides)",
    "Alt+Enter  Shift+Enter" to "a line break in the field",
    "Ctrl+J" to "a line break, in every terminal",
)

// ───────────────────────────── the catalog ─────────────────────────────

val SPECIMENS: List<Specimen> = listOf(
    // ───────── PROGRESS ─────────
    Specimen("PROGRESS", "Bar", "A filled and an empty part in one of six styles; each style says what it is for. A value above zero always shows at least one cell.",
        "Bar(value, max, width, Chart.BarStyle.Slim, color, track)") {
        Stack {
            Chart.BarStyle.entries.forEach { style ->
                Variant(style.name, 11) { Bar(62, 100, 16, style); Label("  ${style.use}", Modifier.weight(1f), color = Palette.Muted) }
            }
            VGap()
            Variant("tiny value", 11) { Bar(1, 1000, 16, Chart.BarStyle.Slim); Label("  1 of 1000 still shows a cell", color = Palette.Muted) }
            Variant("empty", 11) { Bar(0, 100, 16, Chart.BarStyle.Slim); Label("  0 shows none", color = Palette.Muted) }
        }
    },
    Specimen("PROGRESS", "HBar", "A one-row bar that takes the width it is given, with an optional tick for a limit.",
        "HBar(0.62f, Palette.ChartA, Palette.ChartTrack, marker = 0.8f)") {
        Stack {
            Row { HBar(0.62f, Palette.ChartA, Palette.ChartTrack, Modifier.weight(1f), maxCells = 60, marker = 0.8f) }
            Caption("62 %, with a limit marked at 80 %")
            Row { HBar(0.2f, Palette.ChartC, Palette.ChartTrack, Modifier.weight(1f), maxCells = 60) }
            Caption("20 %, no marker")
        }
    },
    Specimen("PROGRESS", "Gauge", "A labelled percentage that gets worse as it grows: green with room, orange from 60 %, red from 85 %. For things that fill up.",
        "gaugeSegment(\"ctx\", fraction, \"13k/131k\")") {
        Stack {
            StatusBar(listOf(gaugeSegment("ctx", 0.12, "13k/131k"), gaugeSegment("disk", 0.68, "")), 60)
            StatusBar(listOf(gaugeSegment("mem", 0.91, "58 of 64 GB"), gaugeSegment("plain", 0.4, "", plain = true)), 60)
            Note("Gauges are status segments: put them in a StatusBar or a footer.")
        }
    },
    Specimen("PROGRESS", "Meter", "A quiet gauge in muted colours for a fact about the machine; only a warning turns it orange.",
        "meterSegment(\"model\", 0.45, \"17 of 64 GB\", warn = false)") {
        StatusBar(listOf(meterSegment("model", 0.45, "17 of 64 GB"), meterSegment("memory", 0.9, "58 of 64 GB", warn = true)), 60)
    },
    Specimen("PROGRESS", "Slider", "A setting's value on its range, slim: the user's value in the accent, the model's own muted. At least one cell above the minimum.",
        "SliderRow(\"temperature\", sliderCells(0.7, 0.0, 2.0, cells), cells, \"0.7\")") {
        Stack {
            SliderRow("temperature", sliderCells(0.7, 0.0, 2.0, 12), 12, "0.7")
            SliderRow("top_p", sliderCells(0.9, 0.0, 1.0, 12), 12, "0.9")
            SliderRow("num_ctx", sliderCells(8192.0, 2048.0, 32768.0, 12), 12, "8.2k", color = Palette.Muted)
            SliderRow("min_p", sliderCells(0.0, 0.0, 1.0, 12), 12, "off", color = Palette.Dim)
        }
    },
    Specimen("PROGRESS", "Context bar", "The context window in three tones: what is used (green, orange, red), the room up to the auto-compact line (blue), the rest quiet.",
        "ContextBar(cells = 24, used = 0.21, compactAt = 0.8)") {
        Stack {
            Variant("21 % used", 11) { ContextBar(24, 0.21, 0.8) }
            Variant("68 % used", 11) { ContextBar(24, 0.68, 0.8) }
            Variant("91 % used", 11) { ContextBar(24, 0.91, 0.8) }
        }
    },
    Specimen("PROGRESS", "Spinner", "Something is happening and there is no progress to show. The text is asked for again every frame, so it can carry elapsed time.",
        "Spinner { \"Reading the vault… ${'$'}seconds s\" }") {
        Stack {
            Spinner("Reading your vault…")
            Loading("notes")
            Caption(SPINNER_FRAMES.toList().joinToString(" "))
        }
    },
    Specimen("PROGRESS", "Steps", "Counting rather than measuring: step 3 of 5, links found, retries used. One dot per step.",
        "Bar(3, 5, 5, Chart.BarStyle.Dots)") {
        Stack { for (n in listOf(1, 3, 5)) Variant("step $n of 5", 11) { Bar(n, 5, 5, Chart.BarStyle.Dots, Palette.Accent) } }
    },

    // ───────── CHARTS ─────────
    Specimen("CHARTS", "Sparkline", "A series in one row of characters, as tall as the value against the largest. A zero is a dot, so a quiet day is still visible.",
        "Sparkline(listOf(3, 5, 9, 4, 0, 7, 12))") {
        Stack {
            Variant("signups", 10) { Sparkline(listOf(1071, 1079, 1085, 1102, 1094, 897)) ; Label("  weekly", color = Palette.Muted) }
            Variant("with gaps", 10) { Sparkline(listOf(3, 5, 9, 4, 0, 0, 7, 12, 2, 0, 1)) }
        }
    },
    Specimen("CHARTS", "Heat strip", "Intensity per day as ░▒▓█, a dot for nothing; one row per weekday makes a calendar.",
        "HeatStrip(levels)  // Chart.heatLevel(count, max)") {
        Stack {
            listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEachIndexed { r, day ->
                Row { Label("$day ", color = Palette.Muted); HeatStrip(List(24) { c -> ((r * 31 + c * 17 + r * c) % 5) }) }
            }
        }
    },
    Specimen("CHARTS", "Bar chart", "Stacked columns with a round y axis and faint gridlines; as wide as the room, bucketed only when there are more bars than cells.",
        "BarChart(bars) { \"${'$'}{it.toInt()}\" }  +  Legend(\"ok\" to Palette.ChartA)") {
        Stack {
            BarChart(
                List(14) { i -> StackedBar("${i + 1}", listOf((6.0 + (i * 7) % 9) to Palette.ChartA, (1.0 + (i * 5) % 4) to Palette.ChartC)) },
                Modifier.fillMaxWidth(), preferredHeight = 8,
            ) { "${it.toInt()}" }
            Legend("passed" to Palette.ChartA, "failed" to Palette.ChartC)
        }
    },
    Specimen("CHARTS", "Stat tile", "One number that matters: a muted label, a bold value in a status colour, an optional dim hint.",
        "StatTile(\"Signups\", \"897\", Palette.Red, hint = \"-18% vs W45\")") {
        Row {
            StatTile("Signups", "897", Palette.Red, hint = "-18% vs W45"); HGap()
            StatTile("Visits", "12,410", Palette.Text, hint = "flat"); HGap()
            StatTile("Conversion", "7.2%", Palette.Orange)
        }
    },

    // ───────── TEXT ─────────
    Specimen("TEXT", "Label", "Every piece of UI text goes through Label: it sanitizes, truncates with … to the width it is given, drops what does not fit the height, and can fill, align and wrap.",
        "Label(text, color = Palette.Muted, textStyle = TextStyle.Bold, align = LabelAlign.End, fill = true, wrap = true)") { ctx ->
        Stack {
            Row { Label("bold ", textStyle = TextStyle.Bold); Label("italic ", textStyle = TextStyle.Italic); Label("strike ", textStyle = TextStyle.Strikethrough); Label("muted ", color = Palette.Muted); Label("dim", color = Palette.Dim) }
            Label("start", Modifier.fillMaxWidth(), background = Palette.SurfaceHigh, fill = true)
            Label("center", Modifier.fillMaxWidth(), background = Palette.SurfaceHigh, fill = true, align = LabelAlign.Center)
            Label("end", Modifier.fillMaxWidth(), background = Palette.SurfaceHigh, fill = true, align = LabelAlign.End)
            Box(Modifier.width(24)) { Label("a long text that is cut with an ellipsis") }
            Label("wrap = true breaks a long sentence over as many rows as the width needs instead of cutting it.", Modifier.width((ctx.width - 4).coerceAtMost(46)), wrap = true, color = Palette.Muted)
        }
    },
    Specimen("TEXT", "Markdown", "What a model writes, rendered: headings, lists, tasks, quotes, tables, inline code, links, figures lit up. Text that is not finished still renders.",
        "renderMarkdown(text, width)  →  StyledLines(lines)") { ctx ->
        StyledLines(renderMarkdown(SAMPLE_MARKDOWN, (ctx.width - 2).coerceAtLeast(20)))
    },
    Specimen("TEXT", "Colour roles", "The roles every theme sets (and Ink derives the rest from). Use the role, never a colour: a theme change then repaints everything.",
        "Palette.Text  Palette.Muted  Palette.Accent  Palette.Cta  …") {
        val roles = listOf(
            "Text" to Palette.Text, "Muted" to Palette.Muted, "Dim" to Palette.Dim, "Accent" to Palette.Accent, "Cta" to Palette.Cta, "Hint" to Palette.Hint,
            "Warn" to Palette.Warn, "Green" to Palette.Green, "Red" to Palette.Red, "Blue" to Palette.Blue, "Orange" to Palette.Orange, "Purple" to Palette.Purple,
        )
        Stack { roles.chunked(3).forEach { row -> Row { row.forEach { (n, c) -> Label("██ ", color = c); Label(n.padEnd(8), color = Palette.Text) } } } }
    },

    // ───────── MESSAGES ─────────
    Specimen("MESSAGES", "Notices", "One line per kind of message, each its own colour and mark, so text of different meaning never looks the same.",
        "NoticeLine(Notice(\"Saved\"))  Cta(\"…\")  Hint(\"…\")  Warning(\"…\")") {
        Stack {
            NoticeLine(Notice("Saved 3 notes"))
            NoticeLine(Notice("Could not reach the model", isError = true))
            Cta("Press 6 to choose your folder.")
            Hint("Ctrl+B folds the sidebar.")
            Warning("Ollama is not running, so AI features are off.")
            Loading("notes")
        }
    },
    Specimen("MESSAGES", "Badge and pill", "A small coloured block for a status or a tag: a Badge (tinted) next to a title, a pill (solid) in a status bar for what asks for attention.",
        "Badge(\"ready\", Palette.Green, Palette.GreenBg)  pillSegment(\"paused\", Palette.Warn)") {
        Stack {
            Row { Badge("ready", Palette.Green, Palette.GreenBg); HGap(); Badge("failed", Palette.Red, Palette.RedBg); HGap(); Badge("brand"); HGap(); Badge("3 new", Palette.Cta, Palette.AccentBg) }
            StatusBar(listOf(statusSegment("", "● model", Palette.Green, bold = true), pillSegment("paused", Palette.Warn), pillSegment("offline", Palette.Red)), 60)
        }
    },

    // ───────── CONTAINERS ─────────
    Specimen("CONTAINERS", "Panel", "A thin frame with a title set into its top edge on a tinted background. The accent says what it is: brand, call to action, danger. Too short a window drops the frame.",
        "Panel(\"Title\", accent = Palette.Cta, background = Palette.Surface) { … }") {
        Stack {
            Panel("Default") { Label("content sits one row and two columns inside", color = Palette.Muted) }
            Panel("Call to action", accent = Palette.Cta) { Label("the next thing to do", color = Palette.Muted) }
            Panel("Danger", accent = Palette.Red, background = Palette.RedBg) { Label("this cannot be undone", color = Palette.Red) }
        }
    },
    Specimen("CONTAINERS", "Card", "A tinted block without a frame, with an optional bold title; [tight] drops the vertical padding.",
        "Card(title = \"Card\", tight = false) { … }") {
        Stack {
            Card(title = "Card with a title") { Label("no frame, just a tint", color = Palette.Muted) }
            Card(tight = true) { Label("tight: no vertical padding", color = Palette.Muted) }
        }
    },
    Specimen("CONTAINERS", "Stack", "Vertical flow that never overflows its height (Mosaic cannot clip): a child taller than the rows left is offered those rows and shortens itself; only one that still cannot fit is hidden.",
        "Stack { child; child; … }  ·  Modifier.flexible()") { ctx ->
        Stack {
            Caption("below: eight rows into a box of five")
            Box(Modifier.size((ctx.width - 2).coerceAtLeast(20), 5)) {
                Stack { repeat(8) { Label("row ${it + 1} of 8", color = Palette.Text) } }
            }
        }
    },

    // ───────── NAVIGATION ─────────
    Specimen("NAVIGATION", "Tabs", "One row of tabs with the current one lit. When they do not fit, the window follows the current tab and ‹ › marks more. Two tints: page tabs and accent tabs.",
        "TabBar(titles, current)  ·  TabLabel(title, on, accent)") {
        Stack {
            CompositionLocalProvider(LocalContentWidth provides 46) { TabBar(listOf("Overview", "Roles", "Contrast", "Charts", "Usage", "Settings", "Guide", "Changelog"), 4) }
            Caption("the same eight tabs in 46 columns")
            Row { TabLabel("Overview", on = true); TabLabel("Roles", on = false); HGap(2); TabLabel("Sidebar", on = true, accent = true); TabLabel("Settings", on = false) }
        }
    },
    Specimen("NAVIGATION", "Actions", "The keys a screen offers: quiet hints inside a screen, filled buttons in a dialog. A disabled one is dim; a long bar continues on the next line.",
        "ActionBar(items, style = ActionStyle.Quiet | Button)") {
        Stack {
            ActionBar(listOf(ActionItem("Enter", "open"), ActionItem("x", "done"), ActionItem("t", "next theme"), ActionItem("d", "delete", enabled = false)), 60)
            ActionBar(listOf(ActionItem("Enter", "Apply", accent = Palette.Cta), ActionItem("Esc", "Cancel")), 60, ActionStyle.Button)
        }
    },
    Specimen("NAVIGATION", "Status bar", "Facts separated by │ with coloured values. It wraps onto as many rows as it needs and never drops one; pills start a row of their own.",
        "StatusBar(listOf(statusSegment(\"\", \"● model\", Palette.Green), gaugeSegment(…)), width)") {
        val facts = listOf(statusSegment("", "● ink-mock-7b", Palette.Green, bold = true), gaugeSegment("ctx", 0.21, "1.7k/8.2k"), statusSegment("", "⚡ 70 tok/s", Palette.Blue), statusSegment("", "⏱ 10s", Palette.Muted), pillSegment("paused", Palette.Warn))
        Stack {
            Caption("70 columns"); StatusBar(facts, 70)
            Caption("36 columns: the same facts, more rows"); StatusBar(facts, 36)
        }
    },
    Specimen("NAVIGATION", "Shell", "The frame around every screen: navigation on the left, the top or the right, content, and a footer with status and key hints. The app owns the keys; n cycles it here.",
        "InkShell(items, current, size, position = NavPosition.Top, status = …, hints = …) { … }") {
        Stack {
            Row { NavPreview("left"); HGap(2); NavPreview("top") }
            NavPreview("right")
        }
    },

    // ───────── LISTS AND TABLES ─────────
    Specimen("LISTS", "List row", "One selectable row: icon, title, a muted detail that takes the rest. The selected row sits on the selection colour with the theme's own text-on-selected colours (High contrast inverts it).",
        "ListRow(selected, \"▤\", Palette.Accent, \"Title\", \"detail\")") {
        Stack {
            ListRow(false, "▤", Palette.Accent, "Panel, Label, Stack", "framed blocks")
            ListRow(true, "❯", Palette.Cta, "Tabs, actions, badges", "the selected row")
            ListRow(false, "·", Palette.Dim, "A dim row", "nothing to do here", dim = true)
        }
    },
    Specimen("LISTS", "Table", "Columns with weights that shrink on narrow terminals, fixed-width columns for badges and dates, right-aligned numbers, and cells that are text, pills or bars.",
        "TableHeader(columns); TableRow(cells, columns, selected)") {
        val columns = listOf(TableColumn("Job", 2f), TableColumn("State", fixed = 10), TableColumn("Progress", 1.5f), TableColumn("Time", fixed = 7, alignEnd = true))
        Stack {
            TableHeader(columns)
            TableRow(listOf(Cell("nightly export"), Cell("done", Palette.Ink, Palette.Green), Cell(bar = 1f, barColor = Palette.Green), Cell("4m 12s", Palette.Muted)), columns)
            TableRow(listOf(Cell("reindex vault"), Cell("running", Palette.Ink, Palette.Warn), Cell(bar = 0.55f, barColor = Palette.Warn), Cell("1m 03s", Palette.Muted)), columns, selected = true)
            TableRow(listOf(Cell("sync notes"), Cell("failed", Palette.Ink, Palette.Red), Cell(bar = 0.2f, barColor = Palette.Red), Cell("12s", Palette.Muted)), columns)
        }
    },
    Specimen("LISTS", "Scroll bar", "A one-column track with a thumb as tall as the visible share of the content, at least one row, placed where the window is. Absent when everything fits.",
        "ScrollBar(scroll, total, visible, Modifier.height(rows))") {
        Stack {
            Row {
                Label("top      ", color = Palette.Muted); Label("middle   ", color = Palette.Muted); Label("end       ", color = Palette.Muted); Label("fits", color = Palette.Muted)
            }
            Row {
                HGap(3); ScrollBar(0, 100, 10, Modifier.height(8)); HGap(9); ScrollBar(45, 100, 10, Modifier.height(8)); HGap(9); ScrollBar(90, 100, 10, Modifier.height(8)); HGap(9); ScrollBar(0, 8, 10, Modifier.height(8))
            }
        }
    },

    // ───────── INPUT AND KEYS (behaviours) ─────────
    Specimen("INPUT & KEYS", "Text input", "A real text field: caret, selection in reverse video, word and line moves, deletes, multi-line, paste repair. Press Enter to try it; every key goes to the field.",
        "val field = TextFieldState(multiline = true);  TextInput(\"Message\", field, focused)  +  field.onKey(key)", Interaction.Text) { ctx ->
        val f = ctx.app.demoField
        Stack {
            TextInput("Message  ·  multi-line", f, focused = ctx.focused, maxRows = 5)
            Row {
                Label("caret ${if (f.caretAt < 0) f.text.length else f.caretAt} of ${f.text.length}", color = Palette.Muted)
                Label("   selected ", color = Palette.Muted)
                Label(f.selectedText?.let { "“${it.replace('\n', '⏎').take(24)}”" } ?: "nothing", color = if (f.selectedText != null) Palette.Cta else Palette.Dim)
            }
            Caption("Shift+arrows select · Alt+arrows jump words · Ctrl+W deletes a word · Alt+Enter adds a line · paste works")
        }
    },
    Specimen("INPUT & KEYS", "Selected text", "Selection is drawn in reverse video (the theme's background on its text colour) so columns line up with the text; with no selection the caret is a ▏ glyph. Long text is cropped around the caret with …",
        "TailText(text, caret = true, caretAt, selection = Selection(anchor, caret))  ·  WrappedText(text, maxRows, …)") { ctx ->
        val long = "The quick brown fox jumps over the lazy dog, then the dog wakes up and the chase goes on across the field until the sun goes down."
        Stack {
            Caption("TailText: one row, selection inside")
            TailText("select this word please", Modifier.fillMaxWidth(), Palette.Text, caret = true, caretAt = 11, selection = Selection(7, 11))
            Caption("TailText: caret only, cropped to the room")
            Box(Modifier.width((ctx.width - 2).coerceAtMost(30))) { TailText(long, Modifier.fillMaxWidth(), Palette.Text, caret = true, caretAt = 40) }
            Caption("WrappedText: three rows, selection across rows, ↑↓ marks when rows are out of view")
            Box(Modifier.width((ctx.width - 2).coerceAtMost(36))) { WrappedText(long, 3, Modifier.fillMaxWidth(), Palette.Text, caret = true, caretAt = 70, selection = Selection(20, 70)) }
        }
    },
    Specimen("INPUT & KEYS", "Key reader", "What the terminal sends and what Ink makes of it: modifiers, the editing keys, and the keys that are the app's own. Press Enter, then try chords (Esc leaves).",
        "event.toKey()  →  Key.Text('a') | Key.WordLeft | Key.Select(Key.Left) | …", Interaction.KeyReader) { ctx ->
        val log = ctx.app.keyLog
        Stack {
            if (log.isEmpty()) Label(if (ctx.focused) "press any key…" else "nothing yet: press Enter on this piece, then keys", color = Palette.Muted)
            log.forEachIndexed { i, line -> Label(line, color = if (i == 0) Palette.Cta else Palette.Muted, textStyle = if (i == 0) TextStyle.Bold else TextStyle.Unspecified) }
        }
    },
    Specimen("INPUT & KEYS", "Editing keys", "Every chord a field understands, as TextEdit does it (pure functions, tested). Terminals send different bytes for the same chord; toKey() and MetaSequence make them one.",
        "TextEdit.apply(field, text, caret, key)  →  (newText, newCaret) | null") { ctx ->
        if (ctx.width >= 84) {
            Row {
                Stack(Modifier.weight(1f)) { Label("MOVE", color = Palette.Muted, textStyle = TextStyle.Bold); KeyTable(EDIT_MOVE, 14); VGap(); Label("SELECT", color = Palette.Muted, textStyle = TextStyle.Bold); KeyTable(EDIT_SELECT, 14) }
                HGap()
                Stack(Modifier.weight(1f)) { Label("DELETE", color = Palette.Muted, textStyle = TextStyle.Bold); KeyTable(EDIT_DELETE, 17); VGap(); Label("NEW LINE", color = Palette.Muted, textStyle = TextStyle.Bold); KeyTable(EDIT_NEWLINE, 21) }
            }
        } else {
            Stack { KeyTable(EDIT_MOVE + EDIT_SELECT + EDIT_DELETE + EDIT_NEWLINE, 21) }
        }
    },
    Specimen("INPUT & KEYS", "Paste and layouts", "A paste arrives as a burst of key presses and Mosaic keeps only the last 64: KeyDispatcher notices the burst and restores the text from the clipboard. It also fixes Enter, Tab and non-Latin keyboards.",
        "KeyDispatcher(globalHandler, overlay, clipboard).also { it.screenHandler = { key -> … } }") {
        Stack {
            Label("•  keys closer than 20 ms are a paste, not a person", color = Palette.Text)
            Label("•  a pasted Enter is a line break in the text, never a click on a button", color = Palette.Text)
            Label("•  a pasted Tab is a space, not a move to the next field", color = Palette.Text)
            Label("•  a paste longer than 64 keys is restored whole from the clipboard", color = Palette.Text)
            Label("•  Option+Shift+arrows sent as ESC ESC [ … are put back together", color = Palette.Text)
            Label("•  Ukrainian ЙЦУКЕН keys work as shortcuts (т is n); fields still get the letter typed", color = Palette.Text)
            Caption("try it: paste a long text into the Text input piece")
        }
    },
    Specimen("INPUT & KEYS", "Choice controls", "Pick one of a few (Chips, RadioRow), yes or no (Checkbox), and labelled fields that light up while focused (LabeledField, InlineField).",
        "Chips(options, selected, focused)  RadioRow(…)  Checkbox(label, checked)  LabeledField(label, focused) { … }") {
        val sample = remember { TextFieldState("hello") }
        Stack {
            Variant("Chips", 11) { Chips(listOf("summary", "full", "hidden"), 0, focused = true) }
            Variant("Chips, quiet", 11) { Chips(listOf("summary", "full", "hidden"), 1, focused = false) }
            Variant("RadioRow", 11) { RadioRow(listOf("left", "top", "right"), 0, focused = true) }
            Variant("Checkbox", 11) { Checkbox("animate the logo", true, focused = true); Label("   ") ; Checkbox("plain glyphs", false) }
            VGap()
            LabeledField("Name  ·  what the field is for", focused = true) { TextValue(sample, focused = true) }
            InlineField("Model", focused = false) { Label("ink-mock-7b", color = Palette.Text) }
        }
    },

    // ───────── SCROLLING ─────────
    Specimen("SCROLLING", "Scroll column", "A list of blocks taller than the room: it shows a window of whole blocks (Mosaic cannot clip), shortens the next one to fill the gap, and draws a scrollbar. Press Enter, then ↑↓ PgUp PgDn Home End.",
        "val state = rememberScrollState();  ScrollColumn(state, follow = cursor) { blocks }  +  state.handle(key)", Interaction.ScrollColumn) { ctx ->
        val state = ctx.app.demoScroll
        @Suppress("UNUSED_VARIABLE") val moved = state.version // so the readout below is drawn again when a key moves the column
        Stack {
            ScrollColumn(state, Modifier.size((ctx.width - 1).coerceAtLeast(20), 10)) {
                repeat(8) { i ->
                    Panel("Block ${i + 1}", accent = if (i % 2 == 0) Palette.Accent else Palette.Cta) {
                        repeat(1 + i % 3) { Label("row ${it + 1} of block ${i + 1}", color = Palette.Muted) }
                    }
                }
            }
            Label("first block ${state.first + 1} · ${state.shown} of ${state.count} on show${if (state.scrollable) " · scrollable" else ""}", color = if (ctx.focused) Palette.Cta else Palette.Muted)
        }
    },
    Specimen("SCROLLING", "Scroll view and select mode", "Output that follows its end, like a log or a chat: ↑↓ a line, PgUp PgDn a page, g G the ends. s starts select mode: a cursor walks whole entries, Space ticks, a ticks all, c copies, b copies only code and tables.",
        "val out = OutputState();  ScrollView(lines, rows, out)  +  out.handle(key, entryTexts, onNotice = …)", Interaction.ScrollView) { ctx ->
        val out = ctx.app.demoOutput
        Stack {
            ScrollView(consoleLines(DEMO_ENTRIES, (ctx.width - 3).coerceAtLeast(20), frame = 0), rows = 12, state = out)
            Label(
                if (out.selecting) "select mode · entry ${out.cursor + 1} of ${DEMO_ENTRIES.size} · ${out.marked.size} ticked · c copies to the clipboard"
                else "following the end · scrolled up ${out.offset} lines" ,
                color = if (out.selecting) Palette.Cta else Palette.Muted,
            )
        }
    },

    // ───────── DIALOGS ─────────
    Specimen("DIALOGS", "Confirm dialog", "A framed, tinted panel with what is about to happen, what it means, what it applies to, and the two answers. Red for a danger. The convention: y confirms, anything else keeps things as they are.",
        "ConfirmDialog(title, confirmLabel, danger = true, message, details)") {
        Stack {
            ConfirmDialog("Move 3 notes to the trash?", "Move", message = "They stay in the vault trash for 30 days.", details = listOf("Ideas/Phone-first onboarding.md", "Retro/Week 45.md", "Inbox/untitled.md"))
            ConfirmDialog("Delete the vault index?", "Delete", danger = true, message = "It is rebuilt on the next start.")
        }
    },

    // ───────── CONSOLE (apps that talk to a model) ─────────
    Specimen("CONSOLE", "Transcript", "A conversation as rows: your message on a band, the model's reply as rendered Markdown, tool calls with spinner / ✓ / ✕ and their first result lines, thinking collapsed to one line.",
        "consoleLines(entries, width, frame)  +  ScrollView") { ctx ->
        StyledLines(
            consoleLines(
                listOf(
                    UserEntry("Why did signups drop?"),
                    ThinkingEntry("…", live = false, seconds = 1.2),
                    ToolEntry("query_metrics", "weekly_signups", ToolStatus.Done, "W45  1,094\nW46  897", 1.2),
                    ToolEntry("run_shell", "git log --since=2.weeks", ToolStatus.WaitingApproval),
                    ToolEntry("read_note", "Retro/Week 45.md", ToolStatus.Failed),
                    AssistantEntry("Conversion fell from **8.8%** to **7.2%**.", "my-model"),
                ),
                (ctx.width - 2).coerceAtLeast(30),
            ),
        )
    },
    Specimen("CONSOLE", "Split panes", "The conversation is the main part; panes dock around it like an IDE. A column takes width and the full height, a strip takes the main part's height; a side holds one panel; what does not fit drops out.",
        "Docked(planDocks(w, h, listOf(DockRequest(PaneId.Thinking, Dock.Left, 20), …)), h, pane = { … }, main = { … })") {
        Stack {
            Caption("default: thinking left, status right")
            dockDiagram(listOf(DockRequest(PaneId.Thinking, Dock.Left, 20), DockRequest(PaneId.Status, Dock.Right, 25))).forEach { Label(it, color = Palette.Accent) }
            Caption("thinking as a strip under the conversation")
            dockDiagram(listOf(DockRequest(PaneId.Thinking, Dock.Bottom, 30), DockRequest(PaneId.Status, Dock.Right, 25))).forEach { Label(it, color = Palette.Cta) }
        }
    },
)

/** The groups of the catalog, in order. */
val SPECIMEN_GROUPS: List<String> = SPECIMENS.map { it.category }.distinct()

// ───────────────────────────── the screen ─────────────────────────────

/** The rows of the piece list: a heading where the group changes, then the piece. [index] is the specimen's place, or -1 for a heading. */
private class PieceRow(val heading: String?, val index: Int)

private val PIECE_ROWS: List<PieceRow> = buildList {
    SPECIMENS.forEachIndexed { i, s ->
        if (s.category != SPECIMENS.getOrNull(i - 1)?.category) add(PieceRow(s.category, -1))
        add(PieceRow(null, i))
    }
}

/**
 * The design system's catalog, Storybook-style: every piece in groups on the left; on the right the selected one with what it is for, live variants
 * and the code that makes it. The pieces marked ▶ are interactive: Enter gives them the keyboard (Esc takes it back).
 */
@Composable
fun CatalogScreen(app: InkApp) {
    val spec = SPECIMENS.getOrNull(app.selected) ?: return
    val width = LocalContentWidth.current
    val bodyHeight = (LocalContentHeight.current - 3).coerceAtLeast(6)
    val listWidth = if (width >= 90) 28 else 24
    val detailWidth = (width - listWidth - 1 - 4).coerceAtLeast(20)
    Stack {
        ScreenHeader("Components", "${SPECIMENS.size} pieces in ${SPECIMEN_GROUPS.size} groups · ↑↓ browse · Tab next group · Enter tries the ones marked ▶")
        VGap()
        Row {
            Panel("Pieces", Modifier.width(listWidth).height(bodyHeight)) { PieceList(app, bodyHeight - 2) }
            HGap()
            Panel(spec.name, Modifier.weight(1f).height(bodyHeight), accent = if (app.catalogFocus) Palette.Cta else Palette.Accent) {
                Stack {
                    Label(spec.summary, color = Palette.Muted, wrap = true)
                    if (spec.interaction != null) {
                        Label(
                            if (app.catalogFocus) "● this demo has the keyboard: every key goes to it, Esc leaves" else "▶ press Enter to try it: the keys then go to this demo",
                            color = if (app.catalogFocus) Palette.Cta else Palette.Accent, textStyle = TextStyle.Bold,
                        )
                    }
                    VGap()
                    spec.demo(DemoContext(app, detailWidth, app.catalogFocus))
                    VGap()
                    Label(spec.code, color = Palette.ConsoleCodeFg, background = Palette.ConsoleCodeBg, fill = true, wrap = true)
                }
            }
        }
    }
}

@Composable
private fun PieceList(app: InkApp, rows: Int) {
    val at = PIECE_ROWS.indexOfFirst { it.index == app.selected }.coerceAtLeast(0)
    val visible = rows.coerceAtLeast(3).coerceAtMost(PIECE_ROWS.size)
    val first = (at - visible / 2).coerceIn(0, (PIECE_ROWS.size - visible).coerceAtLeast(0))
    Stack {
        for (i in first until (first + visible).coerceAtMost(PIECE_ROWS.size)) {
            val row = PIECE_ROWS[i]
            if (row.heading != null) {
                Label(row.heading, color = Palette.Muted, textStyle = TextStyle.Bold)
            } else {
                val s = SPECIMENS[row.index]
                ListRow(app.selected == row.index, if (s.interaction != null) "▶" else "·", if (s.interaction != null) Palette.Cta else Palette.Dim, s.name)
            }
        }
    }
}
