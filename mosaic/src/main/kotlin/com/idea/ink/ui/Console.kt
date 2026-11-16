package com.idea.ink.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.idea.ink.text.sanitizeForTerminal
import com.idea.ink.text.wrapRows
import com.jakewharton.mosaic.layout.MeasurePolicy
import com.jakewharton.mosaic.layout.Measurable
import com.jakewharton.mosaic.layout.MeasureResult
import com.jakewharton.mosaic.layout.MeasureScope
import com.jakewharton.mosaic.layout.background
import com.jakewharton.mosaic.layout.drawBehind
import com.jakewharton.mosaic.layout.fillMaxWidth
import com.jakewharton.mosaic.layout.size
import com.jakewharton.mosaic.modifier.Modifier
import com.jakewharton.mosaic.ui.Box
import com.jakewharton.mosaic.ui.Color
import com.jakewharton.mosaic.ui.Layout
import com.jakewharton.mosaic.ui.Row
import com.jakewharton.mosaic.ui.TextStyle
import com.jakewharton.mosaic.ui.unit.Constraints
import kotlinx.coroutines.delay

// ───────────────────────────── the transcript of a session with a model ─────────────────────────────

/** One thing in a conversation with a model, the way Arcana's Chat shows it. */
sealed interface ConsoleEntry

/** What you wrote: a name on a band of the theme's prompt colour. */
class UserEntry(val text: String) : ConsoleEntry

/** What the model wrote, as Markdown; [streaming] while it is still arriving. */
class AssistantEntry(val text: String, val model: String = "", val streaming: Boolean = false) : ConsoleEntry

enum class ToolStatus { Running, WaitingApproval, Done, Failed }

/** A tool the model called: its name and arguments, how it is going, and what it returned. */
class ToolEntry(val name: String, val args: String, val status: ToolStatus, val result: String = "", val seconds: Double? = null) : ConsoleEntry

/** The model's reasoning: a collapsed line once finished, a live line while it goes on. */
class ThinkingEntry(val text: String, val live: Boolean, val seconds: Double? = null) : ConsoleEntry

/** A one-line outcome: ✓ or ✕. */
class NoticeEntry(val text: String, val isError: Boolean = false) : ConsoleEntry

const val SPINNER_FRAMES = "⠋⠙⠹⠸⠼⠴⠦⠧⠇⠏"

/** At most this many lines of a tool's result are shown under its call; the rest is counted. */
const val TOOL_RESULT_LINES = 6

private fun cut(text: String, width: Int) = if (text.length <= width) text else text.take((width - 1).coerceAtLeast(0)) + "…"

/**
 * The [entries] as console rows no wider than [width]: your messages on a tinted band, the model's replies as rendered Markdown, tool calls as a
 * line with a spinner / ✓ / ✕ and the first lines of their result, thinking as one dim line. [frame] picks the spinner glyph (see [rememberFrame]).
 * Pure apart from the [Palette], so it can be tested.
 */
fun consoleLines(entries: List<ConsoleEntry>, width: Int, plainGlyphs: Boolean = false, frame: Int = 0): List<MdLine> {
    val w = width.coerceAtLeast(12)
    val spin = SPINNER_FRAMES[frame.mod(SPINNER_FRAMES.length)].toString()
    val out = ArrayList<MdLine>()
    entries.forEachIndexed { index, entry ->
        if (index > 0) out += MdLine(emptyList())
        val first = out.size
        when (entry) {
            is UserEntry -> {
                val bg = Palette.UserBg
                out += MdLine(emptyList(), bg)
                out += MdLine(listOf(Run(" ❯ you", Palette.UserName, bg, TextStyle.Bold)), bg)
                wrapText(sanitizeForTerminal(entry.text), w - 2).forEach { out += MdLine(listOf(Run(" $it", Palette.UserText, bg)), bg) }
                out += MdLine(emptyList(), bg)
            }
            is AssistantEntry -> {
                val name = entry.model.ifEmpty { "assistant" }
                out += MdLine(listOf(Run("◆ ", Palette.ConsoleUser), Run(name, Palette.ConsoleUser, style = TextStyle.Bold)) + if (entry.streaming) listOf(Run("  $spin", Palette.Warn)) else emptyList())
                out += renderMarkdown(entry.text, w, plainGlyphs)
            }
            is ToolEntry -> {
                val state = when (entry.status) {
                    ToolStatus.Running -> listOf(Run("  $spin running…", Palette.Warn))
                    ToolStatus.WaitingApproval -> listOf(Run("  ", Palette.Text), Run(" ⚠ waiting for approval ", Palette.Ink, Palette.Warn, TextStyle.Bold))
                    ToolStatus.Done -> listOf(Run("  ✓${entry.seconds?.let { " ${"%.1f".format(it)}s" } ?: ""}", Palette.ConsoleOk))
                    ToolStatus.Failed -> listOf(Run("  ✕ failed", Palette.ConsoleErr, style = TextStyle.Bold))
                }
                val stateWidth = state.sumOf { it.text.length }
                out += MdLine(
                    listOf(Run(" ⚙ ", Palette.ConsoleTool, style = TextStyle.Bold), Run(entry.name, Palette.ConsoleTool, style = TextStyle.Bold),
                        Run(" " + cut(sanitizeForTerminal(entry.args), (w - entry.name.length - stateWidth - 5).coerceAtLeast(4)), Palette.ConsoleDim)) + state,
                )
                val result = sanitizeForTerminal(entry.result).lines().filter { it.isNotBlank() }
                result.take(TOOL_RESULT_LINES).forEach { out += MdLine(listOf(Run("   │ ", Palette.ConsoleDim), Run(cut(it, w - 5), Palette.ConsoleMuted))) }
                if (result.size > TOOL_RESULT_LINES) out += MdLine(listOf(Run("   │ ", Palette.ConsoleDim), Run("… +${result.size - TOOL_RESULT_LINES} more lines", Palette.ConsoleDim, style = TextStyle.Italic)))
            }
            is ThinkingEntry -> out += MdLine(
                if (entry.live) listOf(Run(" $spin ", Palette.Warn), Run("thinking…", Palette.ConsoleDim, style = TextStyle.Italic))
                else listOf(Run(" ∴ ", Palette.ConsoleDim), Run("thought for ${entry.seconds?.let { "%.1f".format(it) } ?: "a moment"}s · ${entry.text.length} characters", Palette.ConsoleDim, style = TextStyle.Italic)),
            )
            is NoticeEntry -> out += MdLine(listOf(Run(if (entry.isError) " ✕ " else " ✓ ", if (entry.isError) Palette.ConsoleErr else Palette.ConsoleOk, style = TextStyle.Bold), Run(entry.text, if (entry.isError) Palette.ConsoleErr else Palette.ConsoleOk)))
        }
        // every row of the entry is tagged with it, so select mode can walk and copy whole entries (the blank row between two entries belongs to none)
        for (i in first until out.size) out[i] = out[i].withEntry(index)
    }
    return if (plainGlyphs) out.map { it.asciiOnly() } else out
}

/** What select mode copies for each of [entries]: what you wrote, what the model wrote (Markdown, as written), a tool's call and result, the thoughts. */
fun consoleEntryTexts(entries: List<ConsoleEntry>): List<String> = entries.map { e ->
    when (e) {
        is UserEntry -> e.text
        is AssistantEntry -> e.text
        is ToolEntry -> "${e.name} ${e.args}" + if (e.result.isNotEmpty()) "\n${e.result}" else ""
        is ThinkingEntry -> e.text
        is NoticeEntry -> e.text
    }
}

/** [text] as dim italic rows for the thinking pane: wrapped to [width], the END of a long stream kept when it does not fit [rows] (the view follows it). */
fun thinkingLines(text: String, width: Int): List<MdLine> =
    wrapText(sanitizeForTerminal(text), width.coerceAtLeast(8)).map { MdLine(listOf(Run(it, Palette.ConsoleDim, style = TextStyle.Italic))) }

// ───────────────────────────── drawing ─────────────────────────────

/** A counter that goes up every [periodMs] while [active]: it drives spinners, and passing it to [consoleLines] animates them. */
@Composable
fun rememberFrame(active: Boolean = true, periodMs: Long = 90): Int {
    var frame by remember { mutableIntStateOf(0) }
    LaunchedEffect(active) {
        while (active) {
            delay(periodMs)
            frame++
        }
    }
    return frame
}

/** How many rows [MessageField] takes for [text] at [width]: the two padding rows and one row per wrapped line of text, up to [maxRows]. */
fun messageFieldHeight(text: String, width: Int, maxRows: Int = 4): Int =
    2 + minOf(maxRows.coerceAtLeast(1), wrapRows(text, (width - 5).coerceAtLeast(3)).size)

/**
 * The field you type a message in: a block on [Palette.SurfaceHigh] (on [Palette.Focus] while [focused]) with a `❯` prompt, the text of [field] wrapped over up to
 * [maxRows] rows (it scrolls with the caret, ↑ ↓ marks show when rows are out of view), a caret while focused, the selection in reverse video, and a muted
 * [placeholder] while empty and not focused. Feed it keys with `field.onKey(key)`; its height is [messageFieldHeight].
 */
@Composable
fun MessageField(field: TextFieldState, placeholder: String, focused: Boolean, width: Int, maxRows: Int = 4) {
    val bg = if (focused) Palette.Focus else Palette.SurfaceHigh
    Label("", Modifier.fillMaxWidth(), background = bg, fill = true)
    Row(Modifier.fillMaxWidth().background(bg)) {
        Label(" ❯ ", color = if (focused) Palette.Cta else Palette.Accent, background = bg, textStyle = TextStyle.Bold)
        Box(Modifier.weight(1f)) {
            if (field.text.isEmpty() && !focused) Label(placeholder, Modifier.fillMaxWidth(), color = Palette.Muted, fill = true)
            else WrappedText(field.text, maxRows, Modifier.fillMaxWidth(), Palette.Text, caret = focused, caretAt = field.caretAt, selection = field.selection)
        }
    }
    Label("", Modifier.fillMaxWidth(), background = bg, fill = true)
}

// ───────────────────────────── slim bars ─────────────────────────────

/** A bar of [cells] cells with [filled] on: the slim `▰▱` (plain glyphs use `#` and `-`). */
fun slimBar(cells: Int, filled: Int, plain: Boolean = false): Pair<String, String> {
    val on = filled.coerceIn(0, cells)
    return (if (plain) "#" else "▰").repeat(on) to (if (plain) "-" else "▱").repeat(cells - on)
}

/** `label ▰▰▰▱▱▱ value`: a slider row for a setting, the bar in [color] (the user's value) or [Palette.Muted]. */
@Composable
fun SliderRow(label: String, filled: Int, cells: Int, value: String, labelWidth: Int = 12, color: Color = Palette.Accent) {
    val (on, off) = slimBar(cells, filled, LocalPlainGlyphs.current)
    Row {
        Label(label.padEnd(labelWidth), color = Palette.Muted)
        Label(on, color = color)
        Label(off, color = Palette.Dim)
        Label(" $value", color = Palette.Text)
    }
}

/**
 * The context window as a bar in three tones: what is used (green, orange from 60 %, red from 85 %), the room up to the auto-compact line (blue), the rest quiet.
 * All three fractions are shares of the whole window.
 */
@Composable
fun ContextBar(cells: Int, used: Double, compactAt: Double) {
    val plain = LocalPlainGlyphs.current
    val glyph = if (plain) "#" else "▰"
    val empty = if (plain) "-" else "▱"
    val u = (used.coerceIn(0.0, 1.0) * cells).toInt().let { if (it == 0 && used > 0.0) 1 else it }.coerceIn(0, cells)
    val room = ((compactAt.coerceIn(0.0, 1.0) * cells).toInt() - u).coerceIn(0, cells - u)
    val tone = when { used >= 0.85 -> Palette.Red; used >= 0.60 -> Palette.Orange; else -> Palette.Green }
    Row {
        Label(glyph.repeat(u), color = tone)
        Label(glyph.repeat(room), color = Palette.Blue)
        Label(empty.repeat(cells - u - room), color = Palette.Dim)
    }
}
