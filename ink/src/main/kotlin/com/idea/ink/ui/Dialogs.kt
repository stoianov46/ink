package com.idea.ink.ui

import androidx.compose.runtime.Composable
import com.jakewharton.mosaic.layout.fillMaxWidth
import com.jakewharton.mosaic.modifier.Modifier
import com.jakewharton.mosaic.ui.Row
import com.jakewharton.mosaic.ui.TextStyle

/** How many names a dialog lists before it says `… and N more`. */
private const val MAX_DETAILS = 5

/**
 * The confirmation dialog: a framed, tinted panel (red for a [danger], cyan for the rest) with what is about to happen, what it means, the things it applies
 * to, and the two answers. The app decides the keys: the convention is [key] (y) confirms and Esc or any other key keeps things as they are.
 */
@Composable
fun ConfirmDialog(
    title: String,
    confirmLabel: String,
    modifier: Modifier = Modifier,
    danger: Boolean = false,
    message: String? = null,
    details: List<String> = emptyList(),
    keepLabel: String = "Keep",
    key: String = "y",
) {
    val accent = if (danger) Palette.Red else Palette.Cta
    Panel(if (danger) "⚠ Danger" else "? Confirm", modifier.fillMaxWidth(), accent = accent, background = if (danger) Palette.RedBg else Palette.SurfaceHigh) {
        Row {
            Label(if (danger) "⚠  " else "?  ", color = accent, textStyle = TextStyle.Bold)
            Label(title, color = Palette.Text, textStyle = TextStyle.Bold, wrap = true)
        }
        if (message != null) Label(message, color = Palette.Muted, wrap = true)
        if (details.isNotEmpty()) {
            VGap()
            details.take(MAX_DETAILS).forEach { Label("•  $it", color = Palette.Text) }
            if (details.size > MAX_DETAILS) Label("   … and ${details.size - MAX_DETAILS} more", color = Palette.Muted)
        }
        VGap()
        ActionBar(listOf(ActionItem(key, confirmLabel, accent = accent), ActionItem("Esc", keepLabel)), style = ActionStyle.Button)
    }
}
