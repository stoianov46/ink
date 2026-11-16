package com.idea.ink.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.idea.ink.text.sanitizeForTerminal
import com.jakewharton.mosaic.layout.background
import com.jakewharton.mosaic.layout.drawBehind
import com.jakewharton.mosaic.layout.fillMaxWidth
import com.jakewharton.mosaic.layout.padding
import com.jakewharton.mosaic.modifier.Modifier
import com.jakewharton.mosaic.ui.Color
import com.jakewharton.mosaic.ui.Row
import com.jakewharton.mosaic.ui.TextStyle

/** The characters of a panel's frame: thin lines with rounded corners, or plain ASCII for the screen-reader mode. */
internal class FrameGlyphs(val topLeft: String, val topRight: String, val bottomLeft: String, val bottomRight: String, val horizontal: String, val vertical: String) {
    companion object {
        val Thin = FrameGlyphs("╭", "╮", "╰", "╯", "─", "│")
        val Plain = FrameGlyphs("+", "+", "+", "+", "-", "|")
        fun of(plain: Boolean) = if (plain) Plain else Thin
    }
}

/** The top edge of a [width]-wide frame, the [title] set into it ("╭─ title ────╮"); the title is cut to fit and may be empty. */
internal fun frameTop(width: Int, title: String, g: FrameGlyphs = FrameGlyphs.Thin): String {
    if (width < 2) return ""
    val inner = width - 2
    val label = if (title.isBlank() || inner < 5) "" else " " + title.take(inner - 4).trimEnd() + " "
    val lead = if (label.isEmpty()) 0 else 1
    return g.topLeft + g.horizontal.repeat(lead) + label + g.horizontal.repeat((inner - lead - label.length).coerceAtLeast(0)) + g.topRight
}

internal fun frameBottom(width: Int, g: FrameGlyphs = FrameGlyphs.Thin): String =
    if (width < 2) "" else g.bottomLeft + g.horizontal.repeat(width - 2) + g.bottomRight

/**
 * A container with a thin border and a title set into its top edge, on a tinted background: the border and title take
 * [accent], the content sits one row and two columns inside. The frame costs two rows, so on a very short terminal it is
 * dropped and the title becomes a plain line above the content.
 */
@Composable
fun Panel(
    title: String,
    modifier: Modifier = Modifier,
    accent: Color = Palette.Accent,
    background: Color = Palette.Surface,
    content: @Composable () -> Unit,
) {
    // a title can carry an emoji: drawn raw it takes two cells, the row wraps and the whole screen scrolls
    val heading = remember(title) { sanitizeForTerminal(title) }
    val framed = !rememberDensity().tiny
    val glyphs = FrameGlyphs.of(LocalPlainGlyphs.current)
    val base = modifier.fillMaxWidth().background(background).let { m ->
        if (!framed) {
            m
        } else {
            m.drawBehind {
                if (width < 2 || height < 2) return@drawBehind
                drawText(0, 0, frameTop(width, heading, glyphs), accent, background)
                if (heading.isNotBlank() && width >= 7) {
                    drawText(0, 2, " " + heading.take(width - 6).trimEnd() + " ", accent, background, TextStyle.Bold)
                }
                for (row in 1 until height - 1) {
                    drawText(row, 0, glyphs.vertical, accent, background)
                    drawText(row, width - 1, glyphs.vertical, accent, background)
                }
                drawText(height - 1, 0, frameBottom(width, glyphs), accent, background)
            }.padding(horizontal = 2, vertical = 1)
        }
    }
    Stack(base) {
        if (!framed && heading.isNotBlank()) Label(heading, color = accent, textStyle = TextStyle.Bold)
        content()
    }
}

/** A plain tinted block, without a frame; [tight] drops the vertical padding. */
@Composable
fun Card(
    modifier: Modifier = Modifier,
    title: String? = null,
    background: Color = Palette.Surface,
    tight: Boolean = false,
    content: @Composable () -> Unit,
) {
    val density = rememberDensity()
    val base = modifier.fillMaxWidth().background(background)
        .padding(horizontal = if (density.narrow) 1 else 2, vertical = if (density.compact || tight) 0 else 1)
    Stack(base) {
        if (title != null && !density.tiny) {
            Label(title, color = Palette.Text, textStyle = TextStyle.Bold)
            if (!density.compact && !tight) VGap()
        }
        content()
    }
}

/** A screen's title in bold with a muted line under it. */
@Composable
fun ScreenHeader(title: String, subtitle: String? = null) {
    Stack {
        Label(title, color = Palette.Text, textStyle = TextStyle.Bold)
        if (subtitle != null && !rememberDensity().tiny) Label(subtitle, color = Palette.Muted)
    }
}

/** A small coloured pill, for a status or a tag next to a title. */
@Composable
fun Badge(text: String, color: Color = Palette.Accent, background: Color = Palette.AccentBg) {
    Label(" $text ", color = color, background = background, textStyle = TextStyle.Bold)
}

/** One selectable row: [icon] in [iconColor], the [title], and a muted [detail] that takes the rest of the line. The selected row sits on [Palette.Selected] with the theme's "text on selected" colours. */
@Composable
fun ListRow(selected: Boolean, icon: String, iconColor: Color, title: String, detail: String = "", dim: Boolean = false) {
    val bg = if (selected) Palette.Selected else Color.Unspecified
    Row(Modifier.fillMaxWidth().background(bg)) {
        Label(" $icon ", color = iconColor, background = bg, textStyle = TextStyle.Bold)
        Label(title, color = if (selected) Palette.OnSelected else if (dim) Palette.Dim else Palette.Text, background = bg, textStyle = if (selected) TextStyle.Bold else TextStyle.Unspecified)
        Label("  $detail", Modifier.weight(1f), color = if (selected) Palette.OnSelectedMuted else Palette.Muted, background = bg, fill = true)
    }
}

/** Rendered rows (from [renderMarkdown] or [consoleLines]) drawn as they are, one screen row each, clipped to the width they are given. */
@Composable
fun StyledLines(lines: List<MdLine>) {
    com.jakewharton.mosaic.ui.Column {
        lines.forEach { RunsRow(it.runs, it.fill, Modifier.fillMaxWidth()) }
    }
}
