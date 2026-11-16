package com.idea.ink.ui

import androidx.compose.runtime.Composable
import com.jakewharton.mosaic.ui.Color
import com.jakewharton.mosaic.ui.Row
import com.jakewharton.mosaic.ui.TextStyle

/** One entry of an [ActionBar]. */
data class ActionItem(val key: String, val label: String, val enabled: Boolean = true, val accent: Color? = null)

/** How an [ActionBar] looks: [Quiet] (coloured key, muted label, no background) for controls inside a screen, [Button] (filled chips) for dialogs and calls to act. */
enum class ActionStyle { Quiet, Button }

private const val ButtonGap = 2
private const val QuietGap = 3 // " · "

private fun ActionItem.widthIn(style: ActionStyle) = if (style == ActionStyle.Button) key.length + 2 + label.length + 2 else key.length + 1 + label.length

/** The lines an [ActionBar] breaks [items] into at [width]: an item goes on the current line while it still fits. */
fun breakIntoLines(items: List<ActionItem>, width: Int, style: ActionStyle = ActionStyle.Quiet): List<List<ActionItem>> {
    val gap = if (style == ActionStyle.Button) ButtonGap else QuietGap
    val lines = mutableListOf<MutableList<ActionItem>>()
    var used = 0
    for (item in items) {
        val w = item.widthIn(style)
        if (lines.isEmpty() || used + gap + w > width) {
            lines += mutableListOf(item)
            used = w
        } else {
            lines.last() += item
            used += gap + w
        }
    }
    return lines
}

/** How many rows an [ActionBar] of [items] takes (its buttons plus the blank row above), so a screen can give everything else the rest. */
@Composable
fun actionBarRows(items: List<ActionItem>, style: ActionStyle = ActionStyle.Quiet): Int =
    breakIntoLines(items, LocalContentWidth.current, style).size + if (style == ActionStyle.Button && !rememberDensity().tiny) 1 else 0

/** A row of `key label` hints (or `[ key ] Label` chips with [ActionStyle.Button]); when they do not fit on one line they continue on the next. */
@Composable
fun ActionBar(items: List<ActionItem>, width: Int = LocalContentWidth.current, style: ActionStyle = ActionStyle.Quiet) {
    if (style == ActionStyle.Button && !rememberDensity().tiny) VGap()
    breakIntoLines(items, width, style).forEach { line ->
        Row {
            line.forEachIndexed { i, item ->
                if (style == ActionStyle.Button) {
                    if (i > 0) HGap(ButtonGap)
                    Label(" ${item.key} ", color = if (item.enabled) Palette.Ink else Palette.Dim, background = if (item.enabled) item.accent ?: Palette.Accent else Palette.SurfaceHigh, textStyle = TextStyle.Bold)
                    Label(" ${item.label} ", color = if (item.enabled) Palette.Text else Palette.Dim, background = Palette.SurfaceHigh)
                } else {
                    if (i > 0) Label(" · ", color = Palette.Dim)
                    Label(item.key, color = if (item.enabled) item.accent ?: Palette.Accent else Palette.Dim, textStyle = TextStyle.Bold)
                    Label(" ${item.label}", color = if (item.enabled) Palette.Muted else Palette.Dim)
                }
            }
        }
    }
}
