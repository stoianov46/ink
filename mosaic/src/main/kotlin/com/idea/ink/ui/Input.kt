package com.idea.ink.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.idea.ink.input.Caret
import com.idea.ink.input.Key
import com.idea.ink.input.KeyDispatcher
import com.idea.ink.input.Selection
import com.idea.ink.input.TextEdit
import com.jakewharton.mosaic.layout.KeyEvent


/** Maps Mosaic's raw key event to the presentation layer's [Key]; null = not ours (e.g. Ctrl-C). */
fun KeyEvent.toKey(): Key? {
    if (ctrl || alt) {
        // Caret navigation in text fields: word jumps (Alt/Ctrl+arrows, with Shift they select), and Alt+A selects all
        // (Ctrl+A / Ctrl+E go to the start / end of the whole text, which many terminals send for Cmd+← / Cmd+→). Ctrl+C and Ctrl+B are for the app to handle before it calls toKey().
        fun move(m: Key) = if (shift) Key.Select(m) else m
        return when {
            alt && key == "Escape" -> Key.MetaPrefix
            alt && key == "Enter" -> Key.PastedNewline // Alt+Enter: a line break in a field instead of sending
            ctrl && key.equals("j", ignoreCase = true) -> Key.PastedNewline // Ctrl+J: the same, in every terminal
            alt && key == "Backspace" -> Key.DeleteWord
            ctrl && key.equals("w", ignoreCase = true) -> Key.DeleteWord
            alt && key == "b" -> Key.WordLeft // Alt+B / Alt+F: how many terminals send Option+←/→
            alt && key == "f" -> Key.WordRight
            alt && key == "B" -> Key.Select(Key.WordLeft)
            alt && key == "F" -> Key.Select(Key.WordRight)
            key == "ArrowLeft" -> move(Key.WordLeft)
            key == "ArrowRight" -> move(Key.WordRight)
            key == "ArrowUp" && shift -> Key.Select(Key.Up)
            key == "ArrowDown" && shift -> Key.Select(Key.Down)
            key == "Home" && shift && !ctrl -> Key.Select(Key.Home)
            key == "End" && shift && !ctrl -> Key.Select(Key.End)
            alt && key.equals("a", ignoreCase = true) -> Key.SelectAll // Alt+A, and what Cmd+A is turned into
            ctrl && shift && key.equals("a", ignoreCase = true) -> Key.SelectAll
            key == "Home" && ctrl -> move(Key.TextStart) // Ctrl+Home, and what Cmd+← is turned into
            key == "End" && ctrl -> move(Key.TextEnd)
            ctrl && key.equals("a", ignoreCase = true) -> Key.TextStart // many terminals send this for Cmd+←
            ctrl && key.equals("u", ignoreCase = true) -> Key.DeleteToLineStart // iTerm's Cmd+Backspace
            ctrl && key.equals("k", ignoreCase = true) -> Key.DeleteToLineEnd
            ctrl && key.equals("e", ignoreCase = true) -> Key.TextEnd
            ctrl && key.equals("o", ignoreCase = true) -> Key.Browse
            ctrl && key.equals("g", ignoreCase = true) -> Key.Effort
            ctrl && key.equals("r", ignoreCase = true) -> Key.Sampling
            ctrl && key.equals("s", ignoreCase = true) -> Key.Save
            ctrl && key.equals("x", ignoreCase = true) -> Key.Skip
            ctrl && key.equals("p", ignoreCase = true) -> Key.Palette
            ctrl && key.equals("n", ignoreCase = true) -> Key.Capture
            else -> null
        }
    }
    // Shift + a caret key selects while it moves.
    fun move(m: Key) = if (shift) Key.Select(m) else m
    return when (key) {
        "Tab" -> if (shift) Key.BackTab else Key.Tab
        "Enter" -> if (shift) Key.PastedNewline else Key.Enter // Shift+Enter (terminals that report it) is a line break
        "Escape" -> Key.Escape
        "Backspace" -> Key.Backspace
        "Delete" -> Key.Delete
        "Home" -> move(Key.Home)
        "End" -> move(Key.End)
        "ArrowUp" -> move(Key.Up)
        "ArrowDown" -> move(Key.Down)
        "ArrowLeft" -> move(Key.Left)
        "ArrowRight" -> move(Key.Right)
        "PageUp" -> Key.PageUp
        "PageDown" -> Key.PageDown
        else -> key.singleOrNull()?.let(Key::Text)
    }
}


private const val FIELD = "text"

/**
 * The text and caret of one input field, edited with [TextEdit]: typing inserts at the caret, ←/→ move it, Alt/Ctrl+←/→
 * jump by word, Home/End go to the line's ends, Shift+movement selects, Backspace/Delete/Ctrl+W/Ctrl+U/Ctrl+K remove
 * text. A pasted line break becomes a space unless the field is [multiline].
 */
class TextFieldState(initial: String = "", private val multiline: Boolean = false, private val max: Int = 20_000) {
    var text by mutableStateOf(initial)
        private set
    var caret by mutableStateOf(Caret())
        private set

    /** The caret position to draw, or -1 for "at the end". */
    val caretAt: Int get() = caret.forField(FIELD)

    /** The selected stretch, or null. */
    val selection: Selection? get() = caret.selectionFor(FIELD)

    val selectedText: String?
        get() = selection?.bounds(text.length)?.let { text.substring(it.first, it.last + 1) }

    fun set(value: String) {
        text = value
        caret = Caret()
    }

    fun clear() = set("")

    /** Applies [key] as a text field would; false when it is not an editing key (the caller then handles it). */
    fun onKey(key: Key): Boolean {
        val k = if (key == Key.PastedNewline && !multiline) Key.Text(' ') else key
        val (newText, newCaret) = TextEdit.apply(FIELD, text, caret, k) ?: return false
        if (newText.length <= max) {
            text = newText
            caret = newCaret
        }
        return true
    }
}

/** The character of a typed key, or null for a special key. */
val Key.char: Char? get() = (this as? Key.Text)?.value

val LocalKeyDispatcher = compositionLocalOf<KeyDispatcher> { error("No KeyDispatcher provided") }

/**
 * Routes keyboard input to [handler] while this composable is on screen; keys it declines fall through to the global
 * shortcuts (screen switching, the key map). Only one screen shows at a time, so there is one handler.
 */
@Composable
fun KeyHandler(handler: (Key) -> Boolean) {
    val dispatcher = LocalKeyDispatcher.current
    val latest by rememberUpdatedState(handler)
    DisposableEffect(dispatcher) {
        dispatcher.screenHandler = { latest(it) }
        onDispose { dispatcher.screenHandler = null }
    }
}
