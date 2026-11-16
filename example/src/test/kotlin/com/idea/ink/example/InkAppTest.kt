package com.idea.ink.example

import com.idea.ink.input.ClipboardReader
import com.idea.ink.settings.InkSettings
import com.idea.ink.settings.InkSettingsStore
import com.idea.ink.theme.Themes
import com.jakewharton.mosaic.layout.KeyEvent
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InkAppTest {
    private var quit = 0
    private var now = 0L
    private var clip: String? = null
    private fun app(settings: InkSettings = InkSettings(), store: InkSettingsStore? = null) =
        InkApp({ quit++ }, settings, store, clock = { now }, clipboard = ClipboardReader { clip })

    private fun key(k: String, ctrl: Boolean = false, alt: Boolean = false, shift: Boolean = false) = KeyEvent(k, alt = alt, ctrl = ctrl, shift = shift)

    /** A person at the keyboard: 200 ms between keys, so none is read as part of a paste. */
    private fun InkApp.press(vararg keys: String) = keys.forEach { now += 200_000_000; onKey(key(it)) }
    private fun InkApp.chord(k: String, ctrl: Boolean = false, alt: Boolean = false, shift: Boolean = false) { now += 200_000_000; onKey(key(k, ctrl, alt, shift)) }
    private fun InkApp.typeText(text: String) = text.forEach { press(it.toString()) }

    @Test fun numberKeysSwitchScreens() {
        val a = app()
        assertTrue(a.onKey(key("4")))
        assertEquals(Screen.Colours, a.screen)
        assertTrue(a.onKey(key("8")))
        assertEquals(Screen.Guide, a.screen)
        assertTrue(a.onKey(key("Escape")))
        assertEquals(Screen.Overview, a.screen)
        assertFalse(a.onKey(key("Escape"))) // already home: not used
    }

    @Test fun themeKeysWrapAround() {
        val a = app()
        a.onKey(key("T"))
        assertEquals(Themes.all.last(), a.theme)
        a.onKey(key("t"))
        assertEquals(Themes.all.first(), a.theme)
    }

    @Test fun selectionStaysInsideTheList() {
        val a = app()
        repeat(50) { a.onKey(key("ArrowDown")) }
        assertEquals(OVERVIEW_ENTRIES.size - 1, a.selected)
        repeat(50) { a.onKey(key("ArrowUp")) }
        assertEquals(0, a.selected)
    }

    @Test fun enterAppliesTheHighlightedTheme() {
        val a = app()
        a.onKey(key("3"))
        assertEquals(Screen.Themes, a.screen)
        repeat(3) { a.onKey(key("ArrowDown")) }
        a.onKey(key("Enter"))
        assertEquals(Themes.all[3], a.theme)
        assertEquals("Theme: ${Themes.all[3].name}", a.toast?.text)
    }

    @Test fun enterOnAnOverviewEntryOpensItsScreen() {
        val a = app()
        a.selected = OVERVIEW_ENTRIES.indexOfFirst { it.screen == Screen.Terminal }
        a.onKey(key("Enter"))
        assertEquals(Screen.Terminal, a.screen)
    }

    @Test fun ctrlCAsksThenQuits() {
        val a = app()
        a.onKey(key("c", ctrl = true))
        assertTrue(a.quitPending)
        assertEquals(0, quit)
        a.onKey(key("c", ctrl = true))
        assertEquals(1, quit)
    }

    @Test fun anyOtherKeyTakesTheQuitQuestionBack() {
        val a = app()
        a.onKey(key("c", ctrl = true))
        a.onKey(key("ArrowDown"))
        assertFalse(a.quitPending)
    }

    @Test fun ctrlCQuitsEvenWhileTyping() {
        val a = app()
        a.press("2", "i")
        a.chord("c", ctrl = true); a.chord("c", ctrl = true)
        assertEquals(1, quit)
    }

    @Test fun foldAndHelp() {
        val a = app()
        a.onKey(key("b", ctrl = true))
        assertTrue(a.settings.sidebarCollapsed)
        a.onKey(key("?"))
        assertTrue(a.helpOpen)
        a.onKey(key("x")) // any key closes the map and is swallowed
        assertFalse(a.helpOpen)
        assertEquals(Screen.Overview, a.screen)
    }

    @Test fun startThemeFromSettings() {
        assertEquals(Themes.Nord, app(InkSettings(theme = "nord")).theme)
        assertEquals(Themes.Dark, app(InkSettings(theme = "no-such-theme")).theme)
    }

    @Test fun theDefaultLayoutIsArcanasThinkingLeftConversationMiddleStatusRight() {
        val d = InkSettings()
        assertEquals("left", d.thinkingPaneDock)
        assertEquals("right", d.statusPaneDock)
        assertTrue(d.thinkingPane && d.statusPane)
    }

    @Test fun theThinkingTitleNeverCutsAWord() {
        assertTrue(thinkingTitle(120, live = true, chars = 12).startsWith("Thinking · $MODEL_NAME"))
        listOf(20, 28, 40).forEach { room ->
            val t = thinkingTitle(room, live = false, chars = 117)
            assertTrue(t.length <= room || t == "Thinking")
            assertTrue(t.startsWith("Thinking"))
        }
        assertEquals("Thinking", thinkingTitle(5, live = true, chars = 1))
    }

    // ── navigation position ──

    @Test fun nCyclesTheNavigationLeftTopRight() {
        val a = app()
        assertEquals("left", a.settings.navPosition)
        a.onKey(key("n")); assertEquals("top", a.settings.navPosition)
        a.onKey(key("n")); assertEquals("right", a.settings.navPosition)
        a.onKey(key("n")); assertEquals("left", a.settings.navPosition)
    }

    @Test fun settingsScreenChangesAreSavedAtOnce() {
        val dir = Files.createTempDirectory("ink-app")
        val store = InkSettingsStore(dir.resolve("settings.properties"))
        val a = app(store = store)
        a.press("6")                       // Settings
        a.press("ArrowDown")               // Navigation
        a.press("Enter")                   // next value: top
        assertEquals("top", store.load().navPosition)
        a.press("ArrowRight")              // right
        assertEquals("right", store.load().navPosition)
        a.press("ArrowLeft", "ArrowLeft")  // top, left
        assertEquals("left", store.load().navPosition)
        dir.toFile().deleteRecursively()
    }

    @Test fun aSideHoldsOnePanelSoSettingsRefuseTheSecond() {
        val a = app(InkSettings(statusPane = true, statusPaneDock = "right", thinkingPane = false, thinkingPaneDock = "bottom"))
        a.press("6")
        a.selected = SETTING_ROWS.indexOfFirst { it.label == "Thinking dock" }
        a.press("ArrowRight") // the dock order is left, right, top, bottom: bottom -> left (the pane is off, any side is fine)
        assertEquals("left", a.settings.thinkingPaneDock)
        a.selected = SETTING_ROWS.indexOfFirst { it.label == "Thinking pane" }
        a.press("Enter") // on, at the left
        assertTrue(a.settings.thinkingPane)
        a.selected = SETTING_ROWS.indexOfFirst { it.label == "Thinking dock" }
        a.press("ArrowRight") // left -> right: refused, the status pane holds the right
        assertEquals("left", a.settings.thinkingPaneDock)
        assertTrue(a.toast?.isError == true)
        assertTrue(a.toast?.text?.contains("one panel") == true)
        a.press("ArrowLeft") // left -> bottom: fine
        assertEquals("bottom", a.settings.thinkingPaneDock)
    }

    @Test fun resetRestoresTheDefaults() {
        val a = app(InkSettings(navPosition = "top", thinkingPaneDock = "bottom", statusPanePercent = 60))
        a.press("6")
        a.selected = SETTING_ROWS.indexOfFirst { it.label == "Defaults" }
        a.press("Enter")
        assertEquals(InkSettings(), a.settings)
    }

    // ── the Console ──

    @Test fun theSessionStartsWhenTheConsoleIsFirstOpened() {
        val a = app()
        repeat(20) { a.advance() }
        assertEquals(0, a.tick)
        a.press("2")
        assertTrue(a.running)
        a.advance()
        assertEquals(1, a.tick)
        a.press("1") // leaving does not stop it: Arcana keeps streaming in the background
        a.advance()
        assertEquals(2, a.tick)
    }

    @Test fun consoleKeysPauseSpeedAndReplay() {
        val a = app()
        a.press("2")
        repeat(5) { a.advance() }
        a.press(" ")
        assertFalse(a.running)
        a.advance()
        assertEquals(5, a.tick)
        a.press(" ", "f")
        assertEquals(2, a.speed)
        a.advance()
        assertEquals(7, a.tick)
        a.press("r")
        assertEquals(0, a.tick)
    }

    @Test fun statusPaneKeysResizeAndTheyPointTheWayTheEdgeMoves() {
        val a = app()
        a.press("2", "{")
        assertEquals(30, a.settings.statusPanePercent)
        a.press("}", "}")
        assertEquals(20, a.settings.statusPanePercent)
        val left = app(InkSettings(statusPaneDock = "left"))
        left.press("2", "}") // on the left the keys swap
        assertEquals(30, left.settings.statusPanePercent)
        a.press("]", "]")
        assertEquals(30, a.settings.thinkingPanePercent)
        repeat(10) { a.press("[") }
        assertFalse(a.settings.thinkingPane) // smaller than the smallest switches it off
    }

    // ── typing in the message field (the text-editing behaviours, through the whole app) ──

    @Test fun typingIsTextNotShortcutsAndEnterSendsItAsThePrompt() {
        val a = app()
        a.press("2", "i")
        assertTrue(a.editing)
        a.typeText("q2t") // not shortcuts now
        assertEquals("q2t", a.field.text)
        assertEquals(0, quit)
        assertEquals(Screen.Console, a.screen)
        a.press("Backspace")
        assertEquals("q2", a.field.text)
        a.press("Enter")
        assertFalse(a.editing)
        assertEquals("q2", a.prompt)
        assertEquals(0, a.tick)
        assertTrue(a.running)
        assertEquals("", a.field.text)
        a.press("i", "Escape")
        assertFalse(a.editing)
    }

    @Test fun shiftArrowsSelectAndTypingReplacesTheSelection() {
        val a = app()
        a.press("2", "i")
        a.typeText("hello world")
        repeat(5) { a.chord("ArrowLeft", shift = true) }
        assertEquals("world", a.field.selectedText)
        a.typeText("there")
        assertEquals("hello there", a.field.text)
        assertNull(a.field.selectedText)
    }

    @Test fun altArrowsJumpWordsAndCtrlWDeletesAWord() {
        val a = app()
        a.press("2", "i")
        a.typeText("one two three")
        a.chord("ArrowLeft", alt = true)
        a.typeText("X")
        assertEquals("one two Xthree", a.field.text)
        a.chord("ArrowRight", alt = true, shift = true) // select to the end of the word
        assertEquals("three", a.field.selectedText)
        a.chord("w", ctrl = true) // deletes the selection first
        assertEquals("one two X", a.field.text)
        a.chord("u", ctrl = true) // to the start of the line
        assertEquals("", a.field.text)
    }

    @Test fun altEnterIsALineBreakAndEnterSends() {
        val a = app()
        a.press("2", "i")
        a.typeText("line one")
        a.chord("Enter", alt = true)
        a.typeText("line two")
        assertEquals("line one\nline two", a.field.text)
        assertTrue(a.editing)
        a.press("Enter")
        assertEquals("line one\nline two", a.prompt)
    }

    @Test fun aPastedEnterIsALineBreakNotASend() {
        val a = app()
        a.press("2", "i")
        a.typeText("a")
        now += 1_000_000 // 1 ms later: a paste, not a person
        a.onKey(key("Enter"))
        assertTrue(a.editing)
        assertEquals("a\n", a.field.text)
    }

    @Test fun aLongPasteThatLostItsBeginningIsRestoredFromTheClipboard() {
        val full = (0 until 140).map { ('a' + it % 26).toString() + (it / 26) }.joinToString("").take(140)
        clip = full
        val a = app()
        a.press("2", "i")
        full.takeLast(64).forEach { now += 1_000_000; a.onKey(key(it.toString())) } // Mosaic kept only the last 64 key events of the burst
        assertEquals(full, a.field.text)
    }

    @Test fun escapeStopsTypingAndCyrillicLettersAreTypedAsThey() {
        val a = app()
        a.press("2", "i")
        a.typeText("так")
        assertEquals("так", a.field.text)
        a.press("Escape")
        assertFalse(a.editing)
        a.press("т") // a Cyrillic т is the n shortcut when no field has the keyboard: it cycles the navigation
        assertEquals("top", a.settings.navPosition)
    }

    // ── select mode in the conversation ──

    @Test fun sStartsSelectModeAndEveryKeyIsItsOwnUntilEscape() {
        val a = app()
        a.press("2")
        repeat(400) { a.advance() } // the session is over: there is something to walk
        assertFalse(a.output.selecting)
        // select mode needs the view to have measured the lines; the key still starts it and then swallows letters
        a.press("s")
        assertTrue(a.output.selecting)
        a.press("r") // would replay the session, but in select mode the key belongs to select mode
        assertTrue(a.tick > 0)
        a.press("Escape")
        assertFalse(a.output.selecting)
    }

    // ── the catalog ──

    @Test fun tabJumpsToTheNextGroupAndTheLastWrapsToTheFirst() {
        val a = app()
        a.press("5")
        assertEquals(0, a.selected)
        a.press("Tab")
        assertEquals(SPECIMENS.indexOfFirst { it.category == SPECIMEN_GROUPS[1] }, a.selected)
        repeat(SPECIMEN_GROUPS.size - 1) { a.press("Tab") }
        assertEquals(0, a.selected)
    }

    private fun InkApp.openPiece(name: String) {
        press("5")
        selected = SPECIMENS.indexOfFirst { it.name == name }
        press("Enter")
    }

    @Test fun enterGivesAnInteractivePieceTheKeyboardAndEscapeTakesItBack() {
        val a = app()
        a.press("5")
        a.press("Enter") // Bar is not interactive
        assertFalse(a.catalogFocus)
        a.openPiece("Text input")
        assertTrue(a.catalogFocus)
        a.typeText("q") // not a shortcut while a demo has the keyboard
        assertEquals(0, quit)
        a.press("Escape")
        assertFalse(a.catalogFocus)
    }

    @Test fun theTextInputDemoIsARealFieldWithSelectionAndWordMoves() {
        val a = app()
        a.openPiece("Text input")
        a.chord("End", ctrl = true)
        a.chord("ArrowLeft", shift = true)
        a.chord("ArrowLeft", shift = true)
        assertEquals("U.", a.demoField.selectedText)
        a.chord("ArrowLeft", alt = true, shift = true)
        assertTrue(a.demoField.selectedText!!.length > 2)
        a.press("Backspace")
        assertNull(a.demoField.selectedText)
        a.press("Enter") // a line break in this multi-line field
        assertTrue(a.demoField.text.endsWith("\n"))
    }

    @Test fun theKeyReaderShowsWhatTheTerminalSentAndWhatInkMadeOfIt() {
        val a = app()
        a.openPiece("Key reader")
        a.chord("ArrowLeft", alt = true, shift = true)
        a.chord("w", ctrl = true)
        a.typeText("x")
        assertEquals(3, a.keyLog.size)
        assertTrue(a.keyLog[2].contains("Alt+Shift+ArrowLeft") && a.keyLog[2].contains("Select(move=WordLeft)"))
        assertTrue(a.keyLog[1].contains("Ctrl+w") && a.keyLog[1].contains("DeleteWord"))
        assertTrue(a.keyLog[0].endsWith("x") || a.keyLog[0].contains("x"))
        repeat(20) { a.typeText("z") }
        assertEquals(8, a.keyLog.size) // only the newest are kept
    }

    @Test fun theScrollViewDemoHasSelectModeWithItsOwnKeysAndEscapeLeavesSelectModeFirst() {
        val a = app()
        a.openPiece("Scroll view and select mode")
        a.press("s")
        assertTrue(a.demoOutput.selecting)
        a.press("Escape") // leaves select mode, not the demo
        assertFalse(a.demoOutput.selecting)
        assertTrue(a.catalogFocus)
        a.press("Escape")
        assertFalse(a.catalogFocus)
    }
}
