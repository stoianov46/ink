package com.idea.ink.example

import androidx.compose.runtime.Composable
import com.idea.ink.layout.Dock
import com.idea.ink.layout.DockRequest
import com.idea.ink.layout.PaneId
import com.idea.ink.layout.planDocks
import com.idea.ink.layout.sliderCells
import com.idea.ink.ui.ContextBar
import com.idea.ink.ui.Docked
import com.idea.ink.ui.Label
import com.idea.ink.ui.LocalContentHeight
import com.idea.ink.ui.LocalContentWidth
import com.idea.ink.ui.MessageField
import com.idea.ink.ui.Palette
import com.idea.ink.ui.Panel
import com.idea.ink.ui.ScreenHeader
import com.idea.ink.ui.ScrollView
import com.idea.ink.ui.SliderRow
import com.idea.ink.ui.Stack
import com.idea.ink.ui.StatusBar
import com.idea.ink.ui.StatusSegment
import com.idea.ink.ui.VGap
import com.idea.ink.ui.consoleLines
import com.idea.ink.ui.messageFieldHeight
import com.idea.ink.ui.gaugeSegment
import com.idea.ink.ui.pillSegment
import com.idea.ink.ui.rememberDensity
import com.idea.ink.ui.rememberFrame
import com.idea.ink.ui.spanText
import com.idea.ink.ui.statusRowCount
import com.idea.ink.ui.statusSegment
import com.idea.ink.ui.thinkingLines
import com.jakewharton.mosaic.layout.height
import com.jakewharton.mosaic.modifier.Modifier
import com.jakewharton.mosaic.ui.Row
import com.jakewharton.mosaic.ui.TextStyle

/** `1.4k`, `812`: a token count in the shortest form. */
fun compactTokens(n: Int): String = if (n < 1000) "$n" else "${"%.1f".format(n / 1000.0)}k"

/** The facts under the message field, like the status bar under Arcana's chat input: model, context gauge, speed, time, what it is doing. */
private fun sessionSegments(app: InkApp, view: SessionView): List<StatusSegment> = buildList {
    add(statusSegment("", "● $MODEL_NAME", Palette.Green, bold = true))
    add(gaugeSegment("ctx", view.contextShare, "${compactTokens(view.contextTokens)}/${compactTokens(CONTEXT_WINDOW)}", plain = app.settings.plainGlyphs))
    if (view.tokensPerSecond > 0) add(statusSegment("", "⚡ ${view.tokensPerSecond} tok/s", Palette.Blue))
    add(statusSegment("", "⏱ ${spanText(view.elapsedMs)}", Palette.Muted))
    add(statusSegment("", if (view.done) "✓ done" else view.phase + "…", if (view.done) Palette.Green else Palette.Warn, bold = true))
    if (app.speed > 1) add(statusSegment("speed", "×${app.speed}", Palette.Cta))
    if (!app.playing && !view.done) add(pillSegment("paused", Palette.Warn))
}

@Composable
fun ConsoleScreen(app: InkApp) {
    val s = app.settings
    val view = app.view
    val frame = rememberFrame(active = app.running)
    val tiny = rememberDensity().tiny
    val headerRows = if (tiny) 1 else 3
    val bodyHeight = (LocalContentHeight.current - headerRows).coerceAtLeast(5)
    val plan = planDocks(
        LocalContentWidth.current, bodyHeight,
        listOfNotNull(
            if (s.statusPane) DockRequest(PaneId.Status, Dock.of(s.statusPaneDock, Dock.Right), s.statusPanePercent) else null,
            if (s.thinkingPane) DockRequest(PaneId.Thinking, Dock.of(s.thinkingPaneDock, Dock.Bottom), s.thinkingPanePercent) else null,
        ),
    )
    Stack {
        ScreenHeader("Console", "a scripted session with a model: it thinks, calls tools, asks approval, then answers")
        VGap()
        Docked(
            plan, bodyHeight,
            pane = { id, w, h -> if (id == PaneId.Status) StatusPane(view, app, w, h) else ThinkingPane(view, app, w, h) },
            main = { w, h ->
                val segments = sessionSegments(app, view)
                val statusRows = statusRowCount(segments, w)
                val fieldRows = messageFieldHeight(app.field.text, w)
                val panelHeight = (h - fieldRows - statusRows).coerceAtLeast(4)
                Stack {
                    Panel(
                        if (app.output.selecting) "Conversation · select mode" else "Conversation",
                        Modifier.height(panelHeight),
                        accent = if (app.output.selecting) Palette.Cta else Palette.Accent,
                    ) {
                        // the rows are wrapped to the panel's width minus the select gutter, the gap and the scrollbar
                        ScrollView(consoleLines(view.entries, w - 7, s.plainGlyphs, frame), rows = panelHeight - 2, state = app.output)
                    }
                    MessageField(app.field, "Ask anything…   i to type, Enter to send, r to replay", app.editing, w)
                    StatusBar(segments, w)
                }
            },
        )
    }
}

/** The longest title for the thinking pane that fits [room] columns, so a narrow column never cuts a word in half. */
fun thinkingTitle(room: Int, live: Boolean, chars: Int): String {
    val state = if (live) "live" else "last thoughts"
    return listOf("Thinking · $MODEL_NAME · $state · $chars characters", "Thinking · $state · $chars chars", "Thinking · $state", "Thinking")
        .firstOrNull { it.length <= room } ?: "Thinking"
}

@Composable
private fun ThinkingPane(view: SessionView, app: InkApp, width: Int, height: Int) {
    val title = thinkingTitle(width - 6, view.thinkingLive, view.thinking.length)
    Panel(title, Modifier.height(height)) {
        if (view.thinking.isEmpty()) Label("Nothing yet: the model has not started thinking.", color = Palette.Dim)
        else ScrollView(thinkingLines(view.thinking, width - 7), rows = height - 2, state = app.thinkOutput)
    }
}

/** A section title in a pane. */
@Composable
private fun Section(text: String) = Label(text, color = Palette.Muted, textStyle = TextStyle.Bold)

/**
 * The status pane: what shapes the answers, in sections: the model, the sampling options (value and slider), the context window (a bar in three tones and
 * when auto-compact starts), and the session. It is a Stack, so on a short pane the last sections are the ones left out.
 */
@Composable
private fun StatusPane(view: SessionView, app: InkApp, width: Int, height: Int) {
    val inner = width - 4
    val cells = (inner - 12 - 7).coerceIn(3, 12)
    val left = (CONTEXT_WINDOW * AUTO_COMPACT_AT).toInt() - view.contextTokens
    Panel("Status", Modifier.height(height)) {
        Stack {
            Section("MODEL")
            Row { Label("● ", color = Palette.Green); Label(MODEL_NAME, color = Palette.Text, textStyle = TextStyle.Bold) }
            Label("runs on this machine", color = Palette.Muted)
            VGap()
            Section("SAMPLING")
            SliderRow("temperature", sliderCells(0.7, 0.0, 2.0, cells), cells, "0.7")
            SliderRow("top_p", sliderCells(0.9, 0.0, 1.0, cells), cells, "0.9")
            SliderRow("top_k", sliderCells(40.0, 0.0, 100.0, cells), cells, "40")
            SliderRow("num_ctx", sliderCells(CONTEXT_WINDOW.toDouble(), 2048.0, 32768.0, cells), cells, compactTokens(CONTEXT_WINDOW), color = Palette.Muted)
            VGap()
            Section("CONTEXT")
            ContextBar(inner.coerceIn(10, 24), view.contextShare, AUTO_COMPACT_AT)
            Label("${compactTokens(view.contextTokens)} of ${compactTokens(CONTEXT_WINDOW)} tokens · ${(view.contextShare * 100).toInt()}%", color = Palette.Text)
            Label("auto-compact at ${(AUTO_COMPACT_AT * 100).toInt()} %", color = Palette.Muted)
            if (left > 0) Label("~${compactTokens(left)} left before it", color = Palette.Blue)
            else Label("due: the next message compacts first", color = Palette.Warn, textStyle = TextStyle.Bold)
            VGap()
            Section("SESSION")
            Label(if (view.done) "✓ done" else "${view.phase}…", color = if (view.done) Palette.Green else Palette.Warn)
            Label("${compactTokens(view.outputTokens)} tokens written", color = Palette.Muted)
            Label("elapsed ${spanText(view.elapsedMs)}", color = Palette.Muted)
        }
    }
}
