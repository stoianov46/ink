package com.idea.ink.ui

import androidx.compose.runtime.Composable
import com.jakewharton.mosaic.layout.background
import com.jakewharton.mosaic.layout.fillMaxWidth
import com.jakewharton.mosaic.layout.padding
import com.jakewharton.mosaic.modifier.Modifier
import com.jakewharton.mosaic.ui.Box
import com.jakewharton.mosaic.ui.Color
import com.jakewharton.mosaic.ui.Row
import com.jakewharton.mosaic.ui.TextStyle

/** A labelled input box; highlighted while it has keyboard focus. On a short window it becomes an [InlineField]. */
@Composable
fun LabeledField(label: String, focused: Boolean, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null, content: @Composable () -> Unit) {
    if (rememberDensity().inline) {
        InlineField(label.substringBefore("  ·"), focused, modifier, content = content)
        return
    }
    Stack(modifier) {
        FieldTitle(label, focused)
        Box(Modifier.fillMaxWidth().background(if (focused) Palette.Focus else Palette.SurfaceHigh).padding(horizontal = 1)) {
            // [trailing] sits at the right end of the field's first row
            if (trailing == null) content() else Row { Box(Modifier.weight(1f)) { content() }; trailing() }
        }
    }
}

/** The title line of a field; what follows the first `  ·  ` is an explanation, so it is quiet grey and not bold. */
@Composable
fun FieldTitle(label: String, focused: Boolean) {
    val title = label.substringBefore("  ·")
    val hint = label.substringAfter("  ·", "").trim()
    Row {
        Label(title, color = if (focused) Palette.Cta else Palette.FieldLabel, textStyle = TextStyle.Bold)
        if (hint.isNotEmpty()) {
            Label("  ·  ", color = Palette.Dim)
            Label(hint, color = Palette.Muted)
        }
    }
}

/**
 * The text of [field] with a caret when [focused], the selection in reverse video, and a muted [placeholder] when empty. With [maxRows] above zero it wraps over
 * that many rows and scrolls with the caret; without, it is one row that always shows the part around the caret, cropped with `…` to the room it has.
 */
@Composable
fun TextValue(field: TextFieldState, focused: Boolean, placeholder: String = "", maxRows: Int = 0) {
    if (field.text.isEmpty()) {
        Label(if (focused) "▏$placeholder" else placeholder, color = Palette.Muted, fill = true)
    } else if (maxRows > 0) {
        WrappedText(field.text, maxRows, Modifier.fillMaxWidth(), Palette.Text, caret = focused, caretAt = field.caretAt, selection = field.selection)
    } else {
        TailText(field.text, Modifier.fillMaxWidth(), Palette.Text, caret = focused, caretAt = field.caretAt, selection = field.selection)
    }
}

/**
 * A labelled text input: [LabeledField] holding a [TextValue]; [maxRows] above zero lets it wrap over several rows. [above] is drawn right above the field and goes with it
 * (the attachment line of a message field).
 */
@Composable
fun TextInput(label: String, field: TextFieldState, focused: Boolean = true, placeholder: String = "", maxRows: Int = 0, above: (@Composable () -> Unit)? = null, trailing: (@Composable () -> Unit)? = null) {
    if (above == null) {
        LabeledField(label, focused, trailing = trailing) { TextValue(field, focused, placeholder, maxRows) }
        return
    }
    Stack {
        above()
        LabeledField(label, focused, trailing = trailing) { TextValue(field, focused, placeholder, maxRows) }
    }
}

const val INLINE_LABEL_WIDTH = 13

/** A one-row `Label  value` field for compact layouts; the value sits on a strip that lights up while focused. */
@Composable
fun InlineField(label: String, focused: Boolean, modifier: Modifier = Modifier, labelWidth: Int = INLINE_LABEL_WIDTH, content: @Composable () -> Unit) {
    Row(modifier.fillMaxWidth().background(if (focused) Palette.Focus else Palette.SurfaceHigh).padding(horizontal = 1)) {
        Label(fit(label, labelWidth), color = if (focused) Palette.Cta else Palette.FieldLabel, textStyle = TextStyle.Bold)
        Box(Modifier.weight(1f)) { content() }
    }
}

/** [text] cut with `…` or padded to exactly [width] columns. */
fun fit(text: String, width: Int): String = if (text.length > width) text.take((width - 1).coerceAtLeast(0)) + "…" else text.padEnd(width)

/**
 * A single-line row of options with the selected one highlighted (bright block while [focused], a quiet one otherwise); windowed around the selection when
 * the options do not fit [budget] columns, `‹` `›` marking more. For choosing one of a few values.
 */
@Composable
fun Chips(options: List<String>, selected: Int, focused: Boolean, budget: Int = 44) {
    if (options.isEmpty()) return
    val sel = selected.coerceIn(0, options.lastIndex)
    var start = sel
    var end = sel
    var used = options[sel].length + 2
    while (true) {
        val right = options.getOrNull(end + 1)?.let { it.length + 3 }
        val left = options.getOrNull(start - 1)?.let { it.length + 3 }
        when {
            right != null && used + right <= budget -> { end++; used += right }
            left != null && used + left <= budget -> { start--; used += left }
            else -> break
        }
    }
    Row {
        if (start > 0) Label("‹ ", color = Palette.Muted)
        for (i in start..end) {
            val on = i == sel
            Label(
                " ${options[i]} ",
                color = if (on && focused) Palette.Ink else if (on) Palette.Text else Palette.Muted,
                background = if (on && focused) Palette.Text else if (on) Palette.Gray else Color.Unspecified,
                textStyle = if (on) TextStyle.Bold else TextStyle.Unspecified,
            )
            if (i < end) Label(" ")
        }
        if (end < options.lastIndex) Label(" ›", color = Palette.Muted)
    }
}

/** `☑ label` / `☐ label` (`[x]` / `[ ]` with plain glyphs): a yes or no. The focused one is bold in the call-to-action colour. */
@Composable
fun Checkbox(label: String, checked: Boolean, focused: Boolean = false) {
    val plain = LocalPlainGlyphs.current
    Row {
        Label(if (checked) (if (plain) "[x] " else "☑ ") else (if (plain) "[ ] " else "☐ "), color = if (checked) Palette.Green else Palette.Muted, textStyle = TextStyle.Bold)
        Label(label, color = if (focused) Palette.Cta else Palette.Text, textStyle = if (focused) TextStyle.Bold else TextStyle.Unspecified)
    }
}

/** `◉ chosen  ○ other  ○ other`: exactly one of a few options, shown all at once (use [Chips] when they do not fit). */
@Composable
fun RadioRow(options: List<String>, selected: Int, focused: Boolean = false) {
    val plain = LocalPlainGlyphs.current
    Row {
        options.forEachIndexed { i, option ->
            val on = i == selected
            if (i > 0) Label("  ")
            Label(if (on) (if (plain) "(*) " else "◉ ") else (if (plain) "( ) " else "○ "), color = if (on) Palette.Accent else Palette.Muted)
            Label(option, color = if (on && focused) Palette.Cta else if (on) Palette.Text else Palette.Muted, textStyle = if (on) TextStyle.Bold else TextStyle.Unspecified)
        }
    }
}
