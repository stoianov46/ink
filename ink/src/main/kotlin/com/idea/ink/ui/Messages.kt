package com.idea.ink.ui

import androidx.compose.runtime.Composable
import com.idea.ink.theme.ThemeSpec
import com.jakewharton.mosaic.layout.background
import com.jakewharton.mosaic.layout.padding
import com.jakewharton.mosaic.modifier.Modifier
import com.jakewharton.mosaic.ui.Color
import com.jakewharton.mosaic.ui.TextStyle

/** A one-line outcome of an action: shown as ✓ (success) or ✕ (error). */
data class Notice(val text: String, val isError: Boolean = false)

/** [notice] as ✓ green or ✕ red (`Done:` / `Error:` with plain glyphs); nothing for null. */
@Composable
fun NoticeLine(notice: Notice?) {
    if (notice != null) {
        val plain = LocalPlainGlyphs.current
        Label(
            (if (notice.isError) (if (plain) "Error: " else "✕ ") else (if (plain) "Done: " else "✓ ")) + notice.text,
            color = if (notice.isError) Palette.Red else Palette.Green,
            wrap = true,
        )
    }
}

/** Call to action: the next thing the user should press to go on. Cyan, bold, marked with `→`. */
@Composable
fun Cta(text: String, modifier: Modifier = Modifier) =
    Label((if (LocalPlainGlyphs.current) "Next: " else "→ ") + text, modifier, color = Palette.Cta, textStyle = TextStyle.Bold, wrap = true)

/** Hint: background information or a tip; nothing to do about it. A soft steel blue, marked with `›`. */
@Composable
fun Hint(text: String, modifier: Modifier = Modifier) =
    Label((if (LocalPlainGlyphs.current) "Note: " else "› ") + text, modifier, color = Palette.Hint, wrap = true)

/** Warning: something is blocked, limited or needs care. Orange, marked with `⚠`. */
@Composable
fun Warning(text: String, modifier: Modifier = Modifier) =
    Label((if (LocalPlainGlyphs.current) "Warning: " else "⚠ ") + text, modifier, color = Palette.Warn, wrap = true)

/** A quiet `Loading …` line for something that has no progress to show. */
@Composable
fun Loading(what: String) {
    Label("Loading $what…", color = Palette.Muted)
}

/** A braille spinner with [text] beside it. */
@Composable
fun Spinner(text: String) = Spinner { text }

/** A spinner whose [text] is asked for again on every frame, so it can show elapsed time and a growing count. */
@Composable
fun Spinner(text: () -> String) {
    val frame = rememberFrame()
    Label("${SPINNER_FRAMES[frame % SPINNER_FRAMES.length]} ${text()}", color = Palette.Warn)
}

/** A small card with a muted [label], a bold [value] in [valueColor] and an optional dim [hint]: one number that matters. */
@Composable
fun StatTile(label: String, value: String, valueColor: Color, modifier: Modifier = Modifier, hint: String? = null) {
    Stack(modifier.background(Palette.Surface).padding(horizontal = 2, vertical = if (rememberDensity().compact) 0 else 1)) {
        Label(label, color = Palette.Muted)
        Label(value, color = valueColor, textStyle = TextStyle.Bold)
        if (hint != null) Label(hint, color = Palette.Dim)
    }
}

/** The colours a theme is recognised by, for a preview strip: backgrounds, text and the accent colours. */
fun ThemeSpec.swatchColors(): List<Color> =
    listOf("background", "surface", "surfaceHigh", "text", "muted", "green", "red", "blue", "orange", "purple", "cyan").map { colors.getValue(it).toMosaic() }

/** A row of colour blocks showing what a theme looks like. */
@Composable
fun ThemeSwatches(spec: ThemeSpec) {
    com.jakewharton.mosaic.ui.Row {
        spec.swatchColors().forEach { color ->
            Label("   ", background = color)
            Label(" ")
        }
    }
}
