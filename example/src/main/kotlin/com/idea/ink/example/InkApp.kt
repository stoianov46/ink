package com.idea.ink.example

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.idea.ink.layout.paneStep
import com.idea.ink.settings.InkSettings
import com.idea.ink.settings.InkSettingsStore
import com.idea.ink.input.Clipboard
import com.idea.ink.input.ClipboardReader
import com.idea.ink.input.Key
import com.idea.ink.input.KeyDispatcher
import com.idea.ink.input.KeyboardLayout
import com.idea.ink.theme.Themes
import com.idea.ink.ui.NavItem
import com.idea.ink.ui.OutputState
import com.idea.ink.ui.ScrollState
import com.idea.ink.ui.TextFieldState
import com.idea.ink.ui.consoleEntryTexts
import com.idea.ink.ui.toKey
import com.idea.ink.ui.NavPosition
import com.idea.ink.ui.ShellToast
import com.idea.ink.ui.ShortcutKeys
import com.jakewharton.mosaic.layout.KeyEvent

/** The screens of the demo, in sidebar order (the position is the number key), under two headings like Arcana's BRAIN / SYSTEM. */
enum class Screen(val title: String, val icon: String, val group: String) {
    Overview("Overview", "◉", "DESIGN"),
    Console("Console", "»", "DESIGN"),
    Themes("Themes", "◐", "DESIGN"),
    Colours("Colours", "▥", "DESIGN"),
    Components("Components", "❯", "DESIGN"),
    Settings("Settings", "✱", "SYSTEM"),
    Terminal("Terminal", "▣", "SYSTEM"),
    Guide("Guide", "¶", "SYSTEM"),
    ;

    val nav get() = NavItem(title, icon, group)

    companion object {
        val items = entries.map { it.nav }
    }
}

/** The pages of the Overview's right-hand panel. */
enum class OverviewTab(val title: String) { Theme("Theme"), Roles("Roles"), Contrast("Contrast") }

/**
 * Everything the demo remembers: which screen is on show, the settings (saved to [store] at every change), the selections, the mock session
 * that is playing in the Console, and what the footer says. The keys are handled here, in one place.
 */
class InkApp(
    private val quit: () -> Unit,
    initial: InkSettings = InkSettings(),
    private val store: InkSettingsStore? = null,
    /** The clock of the key dispatcher (nanoseconds): tests pass their own, because keys closer than 20 ms are read as a paste. */
    clock: () -> Long = System::nanoTime,
    private val clipboard: ClipboardReader = ClipboardReader { Clipboard.readText() },
) {
    var settings by mutableStateOf(initial)
        private set
    var screen by mutableStateOf(Screen.Overview)
    var tab by mutableStateOf(OverviewTab.Theme)
    var selected by mutableIntStateOf(0)
    var toast by mutableStateOf<ShellToast?>(null)
    var quitPending by mutableStateOf(false)
    var helpOpen by mutableStateOf(false)

    // the Console's mock session
    var prompt by mutableStateOf(DEFAULT_PROMPT)
        private set
    var tick by mutableIntStateOf(0)
        private set
    /** The session starts the first time the Console is opened; after that it goes on in the background (the Console tab shows `…`). */
    var playing by mutableStateOf(false)
        private set
    private var started = false
    var speed by mutableIntStateOf(1)
        private set
    /** Scrolling and select mode of the conversation (and of the thinking pane, which only follows its end). */
    val output = OutputState()
    val thinkOutput = OutputState()

    /** What you are typing in the Console's message field: several lines, with caret and selection. */
    val field = TextFieldState(multiline = true)
    var editing by mutableStateOf(false)
        private set

    // the Components catalog: which demo has the keyboard, and the state of the demos that need some
    var catalogFocus by mutableStateOf(false)
        private set
    val demoField = TextFieldState("The quick brown fox\njumps over the lazy dog.\n\nTry Shift+arrows, Alt+arrows, Ctrl+W, Ctrl+U.", multiline = true)
    val demoScroll = ScrollState()
    val demoOutput = OutputState()
    /** The newest key events the Key reader shows, newest first: what the terminal sent and what Ink made of it. */
    var keyLog by mutableStateOf<List<String>>(emptyList())
        private set

    private val dispatcher = KeyDispatcher(globalHandler = { false }, clipboard = clipboard, nanoTime = clock).also { it.screenHandler = ::modalKey }

    private val length = sessionLength()

    val theme get() = Themes.byId(settings.theme)
    val themeIndex get() = Themes.all.indexOfFirst { it.id == theme.id }.coerceAtLeast(0)
    val view: SessionView get() = playSession(prompt, tick)

    /** A reply is being generated (the logo then flows, the Console tab shows `…`). */
    val running: Boolean get() = playing && tick < length

    /** One step of the session clock; called every [TICK_MS] by the window. */
    fun advance() { if (running) tick = (tick + speed).coerceAtMost(length) }

    fun update(new: InkSettings) {
        settings = new
        store?.save(new)
    }

    private fun say(text: String, error: Boolean = false) { toast = ShellToast(text, error) }

    /** How many rows the list of the screen on show has (what ↑↓ move through). */
    val listSize: Int get() = when (screen) {
        Screen.Overview -> OVERVIEW_ENTRIES.size
        Screen.Themes -> Themes.all.size
        Screen.Settings -> SETTING_ROWS.size
        Screen.Components -> SPECIMENS.size
        else -> 0
    }

    fun go(to: Screen) {
        screen = to
        selected = if (to == Screen.Themes) themeIndex else 0
        helpOpen = false
        editing = false
        catalogFocus = false
        if (to == Screen.Console && !started) { started = true; playing = true }
    }

    fun applyTheme(index: Int) {
        update(settings.copy(theme = Themes.all[index.mod(Themes.all.size)].id))
        say("Theme: ${theme.name}")
    }

    fun restart(newPrompt: String = prompt) {
        started = true
        prompt = newPrompt
        tick = 0
        playing = true
        output.reset()
        thinkOutput.reset()
    }

    // ── keys ──

    /** What the keys of the demo do; true if the key was used. */
    fun onKey(e: KeyEvent): Boolean {
        if (e.ctrl && e.key.equals("c", ignoreCase = true)) {
            // the first press asks, the second quits
            if (quitPending) quit() else quitPending = true
            return true
        }
        quitPending = false
        if (e.ctrl && e.key.equals("b", ignoreCase = true)) {
            update(settings.copy(sidebarCollapsed = !settings.sidebarCollapsed))
            return true
        }
        if (helpOpen) { helpOpen = false; return true }
        // while typing, or while a demo has the keyboard, every key is its own: the dispatcher repairs pastes and maps Cyrillic keys for it
        if (editing || catalogFocus) {
            if (catalogFocus && SPECIMENS.getOrNull(selected)?.interaction == Interaction.KeyReader) logKey(e)
            e.toKey()?.let(dispatcher::dispatch)
            return true
        }
        if (e.ctrl || e.alt) return false
        // a Cyrillic letter is the shortcut on the same QWERTY key (т is n), so the shortcuts work on a Ukrainian layout; fields still get what was typed
        val ch = e.key.singleOrNull()?.let { KeyboardLayout.latinFor(it) ?: it }
        if (screen == Screen.Console && consoleKey(e, ch)) return true
        if (screen == Screen.Components && componentsKey(e)) return true
        if (screen == Screen.Settings && settingsKey(e)) return true
        when {
            ch == '?' -> helpOpen = true
            ch == 'q' -> quit()
            ch == 't' -> applyTheme(themeIndex + 1)
            ch == 'T' -> applyTheme(themeIndex - 1)
            ch == 'n' -> cycleNavigation()
            ch != null && ShortcutKeys.indexOf(ch) in Screen.entries.indices -> go(Screen.entries[ShortcutKeys.indexOf(ch)])
            e.key == "Escape" -> if (screen == Screen.Overview) return false else go(Screen.Overview)
            e.key == "ArrowUp" || ch == 'k' -> selected = (selected - 1).coerceAtLeast(0)
            e.key == "ArrowDown" || ch == 'j' -> selected = (selected + 1).coerceAtMost((listSize - 1).coerceAtLeast(0))
            e.key == "Tab" && screen == Screen.Overview -> tab = OverviewTab.entries[(tab.ordinal + 1) % OverviewTab.entries.size]
            e.key == "Enter" -> when (screen) {
                Screen.Themes -> applyTheme(selected)
                Screen.Overview -> OVERVIEW_ENTRIES.getOrNull(selected)?.let { entry -> entry.screen?.let(::go) ?: entry.action?.invoke(this) }
                else -> return false
            }
            else -> return false
        }
        return true
    }

    /** `n` from anywhere: the navigation moves from the left to the top to the right. */
    fun cycleNavigation(by: Int = 1) {
        val all = InkSettings.NAV_POSITIONS
        val next = all[(all.indexOf(settings.navPosition) + by).mod(all.size)]
        update(settings.copy(navPosition = next))
        say("Navigation: $next")
    }

    private fun settingsKey(e: KeyEvent): Boolean {
        val ch = e.key.singleOrNull()
        val dir = when {
            e.key == "ArrowRight" || ch == 'l' || e.key == "Enter" -> 1
            e.key == "ArrowLeft" || ch == 'h' -> -1
            else -> return false
        }
        val row = SETTING_ROWS.getOrNull(selected) ?: return false
        val result = row.change(settings, dir)
        if (result.settings != settings) update(result.settings)
        result.message?.let { say(it, result.error) }
        return true
    }

    private fun notice(n: com.idea.ink.ui.Notice) = say(n.text, n.isError)

    private fun consoleKey(e: KeyEvent, ch: Char?): Boolean {
        val key = e.toKey()
        val texts = consoleEntryTexts(view.entries)
        // select mode: a cursor walks the entries; every key is its own until Esc
        if (output.selecting) {
            key?.let { output.handle(it, texts, letters = true, space = true, onNotice = ::notice) }
            return true
        }
        when {
            ch == 'r' -> { restart(); say("Replaying the session") }
            ch == ' ' -> if (tick >= length) restart() else playing = !playing
            ch == 'f' -> { speed = when (speed) { 1 -> 2; 2 -> 4; else -> 1 }; say("Speed ×$speed") }
            ch == 'i' || e.key == "Enter" -> { editing = true; field.clear() }
            // the status pane is a column; `{` makes it wider and switches it on, `}` narrower and at the smallest off; on the left the keys swap, as in Arcana
            ch == '{' || ch == '}' -> {
                val left = settings.statusPaneDock == "left"
                val step = paneStep("Status pane", settings.statusPane, settings.statusPanePercent, larger = (ch == '{') != left)
                update(settings.copy(statusPane = step.on, statusPanePercent = step.percent))
                if (step.message.isNotEmpty()) say(step.message)
            }
            // the thinking pane: `]` larger (and on), `[` smaller (and at the smallest off)
            ch == '[' || ch == ']' -> {
                val step = paneStep("Thinking pane", settings.thinkingPane, settings.thinkingPanePercent, larger = ch == ']')
                update(settings.copy(thinkingPane = step.on, thinkingPanePercent = step.percent))
                if (step.message.isNotEmpty()) say(step.message)
            }
            // ↑↓ one line, PgUp/PgDn a page, Home/End the ends, `s` starts select mode: the behaviours of OutputState
            key != null && output.handle(key, texts, letters = true, space = false, onNotice = ::notice) -> Unit
            else -> return false
        }
        return true
    }

    /** What the dispatcher hands to the mode that has the keyboard: the message field while typing, the demo while one is focused. */
    private fun modalKey(key: Key): Boolean {
        when {
            editing -> editKey(key)
            catalogFocus -> catalogKey(key)
            else -> return false
        }
        return true
    }

    /**
     * Typing in the message field. Enter sends (the mock session then plays with your words as the prompt); a line break is Alt+Enter, Shift+Enter or Ctrl+J, or
     * an Enter that came with a paste. Esc stops typing. Everything else is the field's: [TextFieldState.onKey].
     */
    private fun editKey(key: Key) {
        when (key) {
            Key.Escape -> editing = false
            Key.Enter -> {
                val text = field.text.trim().ifEmpty { DEFAULT_PROMPT }
                field.clear()
                editing = false
                restart(text)
            }
            else -> field.onKey(key)
        }
    }

    // ── the Components catalog ──

    private fun componentsKey(e: KeyEvent): Boolean {
        val spec = SPECIMENS.getOrNull(selected) ?: return false
        return when {
            e.key == "Enter" && spec.interaction != null -> { catalogFocus = true; true }
            e.key == "Tab" -> { // the first piece of the next group
                val groups = SPECIMENS.map { it.category }.distinct()
                selected = SPECIMENS.indexOfFirst { it.category == groups[(groups.indexOf(spec.category) + 1) % groups.size] }
                true
            }
            else -> false
        }
    }

    private fun catalogKey(key: Key) {
        val spec = SPECIMENS.getOrNull(selected) ?: run { catalogFocus = false; return }
        when (spec.interaction) {
            Interaction.Text -> if (key == Key.Escape) catalogFocus = false else demoField.onKey(if (key == Key.Enter) Key.PastedNewline else key)
            Interaction.KeyReader -> if (key == Key.Escape) catalogFocus = false
            Interaction.ScrollColumn -> if (key == Key.Escape) catalogFocus = false else demoScroll.handle(key, letters = true)
            Interaction.ScrollView -> when {
                key == Key.Escape && !demoOutput.selecting -> catalogFocus = false
                else -> demoOutput.handle(key, DEMO_ENTRY_TEXTS, letters = true, space = true, onNotice = ::notice)
            }
            null -> catalogFocus = false
        }
    }

    private fun logKey(e: KeyEvent) {
        val raw = buildString {
            if (e.ctrl) append("Ctrl+"); if (e.alt) append("Alt+"); if (e.shift) append("Shift+")
            append(if (e.key == " ") "Space" else e.key)
        }
        val made = when (val k = e.toKey()) {
            null -> "nothing: the app's own (Ctrl+C, Ctrl+B)"
            is Key.Text -> "Text '${k.value}'"
            else -> k.toString()
        }
        keyLog = (listOf("$raw  →  $made") + keyLog).take(8)
    }

    /** The key hints of the footer for the screen on show. */
    val hints: String get() = when {
        helpOpen -> "any key  close"
        editing -> "Enter  send   Alt+Enter  new line   Shift+←→  select   Esc  stop typing"
        catalogFocus -> "Esc  leave the demo   every other key goes to it"
        screen == Screen.Console && output.selecting -> "↑↓  move   Space  tick   a  all   c  copy   b  code only   Esc  leave"
        screen == Screen.Components -> "↑↓  choose   Tab  next group   Enter  try it (marked ▶)   ?  keys"
        screen == Screen.Console -> "i  ask   s  select   r  replay   Space  pause   f  speed   { }  status   [ ]  thinking   ↑↓  scroll"
        screen == Screen.Settings -> "↑↓  choose   ←→  change   Enter  next value   n  navigation   ?  keys"
        screen == Screen.Themes -> "↑↓  choose   Enter  apply   t  next theme   ?  keys"
        screen == Screen.Overview -> "↑↓  choose   Enter  open   Tab  next page   t  next theme   ?  keys"
        else -> "1-8  screens   t  next theme   n  navigation   Ctrl+B  fold   ?  keys"
    }
}
