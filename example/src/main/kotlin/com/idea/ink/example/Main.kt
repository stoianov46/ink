package com.idea.ink.example

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.idea.ink.colors.ColorSupport
import com.idea.ink.settings.InkSettingsStore
import com.idea.ink.theme.Themes
import com.idea.ink.ui.NavPosition
import com.idea.ink.ui.InkShell
import com.idea.ink.ui.Palette
import com.idea.ink.ui.statusSegment
import com.idea.ink.ui.rememberFrame
import com.idea.ink.ui.rememberTerminalSize
import com.jakewharton.mosaic.layout.onKeyEvent
import com.jakewharton.mosaic.modifier.Modifier
import com.jakewharton.mosaic.runMosaic
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Ink's example: the main window of Arcana (sidebar with the logo, screens under headings and a Fold button; a content area with a
 * header, tabs and framed panels; a footer with status facts and key badges), built from Ink's components and themes.
 *
 * Run with `./gradlew :example:installDist` then `example/build/install/ink/bin/ink` (a real terminal is needed), or `make run`.
 * `INK_THEME=nord` picks the theme it starts with; `INK_COLUMNS` / `INK_ROWS` override the terminal size.
 */
fun main() {
    val quit = CompletableDeferred<Unit>()
    // Mosaic renders inline; start from a clean screen so the frame never scrolls and old output does not show through.
    print("\u001b[2J\u001b[H")
    runBlocking {
        val ui = launch { runMosaic { InkExample(onQuit = { quit.complete(Unit) }) } }
        // Quitting cancels Mosaic from outside, which restores the terminal, instead of relying on Mosaic seeing an exact Ctrl+C event.
        launch {
            quit.await()
            ui.cancel()
        }
        ui.join()
        coroutineContext.cancelChildren()
    }
    // Mosaic reads the keyboard on a native thread; halting skips the JVM's orderly shutdown, which could race with it on exit.
    System.out.flush()
    Runtime.getRuntime().halt(0)
}

@Composable
private fun InkExample(onQuit: () -> Unit) {
    val app = remember {
        val store = InkSettingsStore.default()
        // INK_THEME picks the theme for this run without touching the saved settings' other values
        val loaded = store.load().let { s -> System.getenv("INK_THEME")?.let { s.copy(theme = it) } ?: s }
        InkApp(onQuit, loaded, store)
    }
    val size = rememberTerminalSize("INK")
    LaunchedEffect(app.theme) { Palette.use(app.theme) }
    // the clock of the mock session in the Console
    LaunchedEffect(Unit) {
        while (true) {
            delay(TICK_MS)
            app.advance()
        }
    }
    val frame = rememberFrame(active = app.view.phase == "waiting for approval")
    LaunchedEffect(app.quitPending) {
        if (app.quitPending) { delay(2_500); app.quitPending = false }
    }
    LaunchedEffect(app.toast) {
        if (app.toast != null) { delay(3_500); app.toast = null }
    }
    InkShell(
        items = Screen.items,
        current = app.screen.ordinal,
        size = size,
        modifier = Modifier.onKeyEvent(app::onKey),
        logo = "⟐ ink",
        collapsed = app.settings.sidebarCollapsed,
        position = NavPosition.of(app.settings.navPosition),
        busy = app.settings.animateLogo && app.running,
        uiScale = app.settings.uiScale,
        plainGlyphs = app.settings.plainGlyphs,
        badge = { i ->
            // like Arcana's Chat tab: `…` while a reply is generated, a pulsing `!` while a tool waits for your approval
            if (Screen.entries[i] != Screen.Console) null
            else if (app.view.phase == "waiting for approval") "!" to (if (frame % 10 < 5) Palette.Orange else Palette.Warn)
            else if (app.running) "…" to Palette.Accent
            else null
        },
        status = footerStatus(app, size.columns, size.rows),
        hints = if (app.quitPending) "Press Ctrl+C again to quit (any other key cancels)" else app.hints,
        toast = app.toast,
        quitPending = app.quitPending,
    ) {
        if (app.helpOpen) HelpOverlay() else when (app.screen) {
            Screen.Overview -> OverviewScreen(app)
            Screen.Console -> ConsoleScreen(app)
            Screen.Themes -> ThemesScreen(app)
            Screen.Settings -> SettingsScreen(app)
            Screen.Colours -> ColoursScreen(app)
            Screen.Components -> CatalogScreen(app)
            Screen.Terminal -> TerminalScreen(size)
            Screen.Guide -> GuideScreen()
        }
    }
}

/** The footer's facts, like Arcana's model / tokens / cpu line: the theme (green dot like "model online"), the colour level, the size, the screen. */
private fun footerStatus(app: InkApp, columns: Int, rows: Int) = listOf(
    statusSegment("", "● ${app.theme.name}", Palette.Green, bold = true),
    statusSegment("", if (ColorSupport.truecolor) "⚡ truecolor" else "◌ 256 colours", if (ColorSupport.truecolor) Palette.Blue else Palette.Warn),
    statusSegment("themes", "${Themes.all.size}", Palette.Cta),
    statusSegment("size", "${columns}x$rows", Palette.Muted),
) + (if (app.theme.transparent) listOf(statusSegment("", "see-through", Palette.Accent)) else emptyList())
