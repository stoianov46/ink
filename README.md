# Ink — Terminal Design System

The design system of **Arcana** and **CosmoCLI**, extracted into one place: colour maths, themes, terminal probing, and the
Mosaic components that make up Arcana's main window (palette, label, panel, stack, tabs, status bar, shell), and the pieces of an app that talks to a
model: a console that renders model output (Markdown, code, tables, diffs, tool calls, thinking), split panes docked around it, and a message field.

```
make run            # the demo: Arcana's main window, built from Ink's parts
make build          # compile and test everything
make publish-local  # publish com.idea:ink to ~/.m2
```

## Modules

| Module | Artifact | Depends on | What it is |
|---|---|---|---|
| `:ink` | `com.idea:ink` | Mosaic 0.18 | **One artifact.** Plain Kotlin parts (packages `colors`, `theme`, `input`, `text`, `chart`, `layout`, `settings`, `terminal`, `scroll`): Colour maths, `ThemeSpec` and the built-in themes, `InkColors` (every role a theme gives the UI), terminal size probing, text sanitising, **text editing (`TextEdit`: caret, selection, word and line operations), the key model and `KeyDispatcher` (paste repair, Cyrillic shortcuts)**, chart maths, dock planning, persisted `InkSettings`. Plain Kotlin, `Int` colours. Mosaic parts (package `ui`): `Palette`, `Label`, `Panel`, `Card`, `Stack`, tabs, `StatusBar`, `ActionBar`, `InkShell` (sidebar left / top / right, footer), **`Bar` (6 styles), `HBar`, `Sparkline`, `HeatStrip`, `BarChart`, `Spinner`, `SliderRow`, `ContextBar`, `StatTile`, notices, `Table`, `Chips`, `Checkbox`, `ConfirmDialog`, `ListRow`**, **`TextFieldState`, `TextInput`, `TailText`, `WrappedText` (selection drawn in reverse video), `ScrollColumn`, `ScrollView` + `OutputState` (select mode, copy)**, `renderMarkdown`, `consoleLines`, `MessageField`, `Docked`. |
| `:example` | – | `ink` | The demo window: Arcana's main window, a **Components catalog** (every piece, live, with its code; the behaviours are interactive), a **Console** that plays a scripted model session in split panes, and a **Settings** screen (navigation position, panes). |

```
ink/src/main/kotlin/com/idea/ink/
  colors/    Rgb.kt  ColorSupport.kt  ColorMath.kt    mix, hsv, hsl, WCAG contrast, 256-colour fitting, hex parsing
  theme/     ThemeSpec.kt  BuiltInThemes.kt  InkColors.kt
  layout/    Docks.kt                                 where panes dock, how large, what fits; the pane size keys; slider maths
  settings/  InkSettings.kt                           theme, navigation position, panes, density…: parsed safely, saved atomically
  terminal/  TerminalSize.kt  ResizeSignalGuard.kt
  text/      Sanitize.kt                              make arbitrary text safe for a Mosaic cell grid
ink/src/main/kotlin/com/idea/ink/ui/
  Palette.kt  Label.kt  Layout.kt  Panel.kt  TabBar.kt  Rainbow.kt  StatusBar.kt  Actions.kt  Shell.kt
  Markdown.kt  Console.kt  Docks.kt                   model output, the transcript model, split panes
example/src/main/kotlin/…/example/
  Main.kt  InkApp.kt  Screens.kt  ConsoleScreen.kt  SettingsScreen.kt  MockSession.kt
  (+ com/jakewharton/mosaic/compat.kt, see below)
```

## Using it

```kotlin
dependencies {
    implementation("com.idea:ink:0.4.0")   // brings Mosaic along
}
repositories { mavenLocal() }
```

```kotlin
LaunchedEffect(config.theme) { Palette.use(Themes.byId(config.theme)) }   // choosing a theme repaints every screen

val size = rememberTerminalSize("ARCANA")        // real size from the OS; ARCANA_COLUMNS / ARCANA_ROWS override it
InkShell(
    items = screens.map { NavItem(it.title, it.icon, it.group) },
    current = index, size = size, logo = "⟐ arcana", collapsed = folded,
    status = listOf(statusSegment("", "● model", Palette.Green, bold = true)),
    hints = "Ctrl+P  commands   Ctrl+N  capture   ?  keys",
    modifier = Modifier.onKeyEvent { handleKey(it) },     // the app owns the keys; the shell only draws
) {
    Panel("Today") { Label("Hello") }
}
```

### An app that talks to a model

```kotlin
val entries = listOf(
    UserEntry("Why did signups drop?"),
    ThinkingEntry("Look at the weekly numbers first…", live = false, seconds = 1.1),
    ToolEntry("query_metrics", "weekly_signups --weeks 6", ToolStatus.Done, result = "week  visits  signups\n…", seconds = 1.2),
    AssistantEntry("## Signups fell 18%\n\n| Week | Signups |\n|---|--:|\n| W46 | 897 |", model = "my-model", streaming = true),
)
val frame = rememberFrame()                                             // drives the spinners
val output = remember { OutputState() }                                 // scrolling and select mode
ScrollView(consoleLines(entries, width - 7, frame = frame), rows = 20, state = output)   // follows the newest line; width - 7 = gutter, gap, scrollbar
MessageField(field, "Ask anything…", focused = typing, width = width)   // field = TextFieldState(multiline = true)
```

`renderMarkdown` handles headings, lists, task lists, quotes, tables, `**bold**`, `*italic*`, `` `code` ``, links, fenced code in an editor-style box (line numbers,
syntax colours, `diff` lines green and red), figures (money, percentages, losses red), and a fence that is still open while text streams. Emoji, CJK and ANSI codes are made safe first.

### The catalog: every piece, and how it behaves

The **Components** screen (`5`) is a Storybook for the system: 38 pieces in 11 groups on the left, and on the right the selected one with what it is for, live variants and the code that makes it.
`↑↓` browse, `Tab` jumps to the next group, and the pieces marked **▶** are interactive: `Enter` gives them the keyboard, `Esc` takes it back.

Progress and charts: six bar styles (`Chart.BarStyle`: Block, Line, Slim, Half, Dots, Braille, each with a *when to use it*), `HBar` with a limit marker, gauge and meter segments, sliders, the context bar,
spinners, step dots, sparklines, heat strips, stacked bar charts with a legend, stat tiles. Then text, messages, containers, navigation, lists and tables, choice controls, dialogs, the model-console pieces and split panes.

The behaviours are the part of a design system that is hardest to get right, so they are extracted whole and each one has a live piece:

| Behaviour | Where | What you can try |
|---|---|---|
| **Text editing** | `TextEdit` (pure), `TextFieldState`, `TextInput` | caret, Shift+move selection, Alt/Ctrl+←→ by word, Home/End by line, Ctrl+A/E by text, Ctrl+W / Ctrl+U / Ctrl+K deletes, multi-line with Alt+Enter, typing replaces the selection |
| **Selected text** | `TailText`, `WrappedText` | reverse video (the theme's background on its text colour), cropped with `…` around the caret, `↑↓` marks and a scrollbar when rows are out of view |
| **Keys** | `KeyEvent.toKey()`, `MetaSequence`, the *Key reader* piece | what the terminal sent and what Ink made of it: Option+Shift+arrows arrive as `ESC ESC [ 1;4 D` on some terminals and are put back together |
| **Paste** | `KeyDispatcher` | keys closer than 20 ms are a paste: a pasted Enter is a line break, a pasted Tab a space, and a paste Mosaic truncated (it keeps 64 key events) is restored whole from the clipboard |
| **Shortcuts on other layouts** | `KeyboardLayout` | Ukrainian ЙЦУКЕН keys act as the QWERTY key in the same place (`т` is `n`); fields still get the letter typed |
| **Scroll container** | `ScrollColumn`, `ScrollState` | whole blocks (Mosaic cannot clip), the next one shortened to fill the gap, a scrollbar, ↑↓ steps at least 3 rows, PgUp/PgDn a page, Home/End |
| **Scroll view and select mode** | `ScrollView`, `OutputState` | output that follows its end; `s` starts select mode: a cursor walks whole entries, Space ticks, `a` ticks all, `c` copies, `b` copies only code and tables, Esc leaves |

Two things to know when you build on them. A key is given to the app's field or demo **modally** (everything is its own until Esc), which is what makes typing safe from shortcuts.
And `ScrollState.version` / `OutputState.offset` are observable: read them in your own composable if you show the position, so that text is drawn again when a key moves the view.

### Split panes

The conversation is the main part; panes dock around it like an IDE (as in Arcana's Chat). `planDocks` decides what fits: a **column** (left / right) takes a share of the
width and the full height and needs a window of 80 columns; a **strip** (top / bottom) takes a share of the main part's height and needs 26 rows; the main part keeps at least
40 columns / 5 rows; a side holds one panel. `Docked` draws the plan, giving every part an exact size (Mosaic cannot clip).

```kotlin
val plan = planDocks(width, height, listOf(
    DockRequest(PaneId.Status, Dock.Right, percent = 30),       // model, sampling, context
    DockRequest(PaneId.Thinking, Dock.Bottom, percent = 30),    // the model's thoughts, live
))
Docked(plan, height,
    pane = { id, w, h -> if (id == PaneId.Status) StatusPane(w, h) else ThinkingPane(w, h) },
    main = { w, h -> Conversation(w, h) })
```

`paneStep` is the pure rule behind the size keys (`{ }` for the status column, `[ ]` for the thinking strip): larger switches the pane on and grows it 5 % up to 90 %, smaller shrinks it
and at 10 % switches it off.

### Navigation position and settings

`InkShell(position = NavPosition.Left | Top | Right)`. The app keeps its choices in `InkSettings` (`navPosition`, `sidebarCollapsed`, `uiScale`, `plainGlyphs`, `animateLogo`, both panes and their docks and sizes),
`InkSettingsStore` saves them atomically (`~/.config/ink/settings.properties`, or `$INK_CONFIG`) and a broken or hand-edited file falls back to defaults value by value. In the example `n` cycles the navigation
from anywhere and the Settings screen changes everything (←→ or Enter), saved at every change.

### The mock session

The example's Console plays a scripted session with no model behind it: the model thinks (live in the thinking pane), calls `query_metrics`, `search_notes` and `read_note`, asks your approval for `run_shell`
(the Console tab pulses `!`), then streams a Markdown answer with a table and a diff while the context gauge fills. `playSession(prompt, tick)` is a pure function of a counter, so the same tick always draws the
same screen and the script is tested. `i` types your own prompt (the same script plays with it), `r` replays, Space pauses, `f` changes the speed; it keeps running in the background when you leave the screen.

Only the core, with no Mosaic: `Themes.byId("nord")`, `InkColors(theme).tabOn` (an `0xRRGGBB` Int), `mix`, `contrast`, `TerminalSize.probe()`.

## Themes

19 built-ins, the union of Arcana's and CosmoCLI's: Dark, Light, High contrast, Soft Dark/Light, Darcula, Solarized Dark/Light, Nord,
Cosmo, Brasil, Liberty, Ghost (see-through: `transparent = true` leaves the background, cards and entries unpainted), Monochrome,
Monochrome Accent, Colour-blind Dark/Light, Amber, Hacker.

A theme sets 12 required roles (`background surface surfaceHigh text muted dim green red blue orange purple cyan`); `InkColors` derives
the rest (tab tints, soft status colours, chart colours, prompt band…). Optional roles (`entry`, `selected`, `focus`, `hint`, `codeFg`,
`promptBand`, `consoleBold`, `statusAccent`…) override a derived value; each one documents its fallback in `InkColors.kt`. A test checks that
body text is readable (WCAG 4.5:1) on the background in every built-in theme.

## Things that are easy to get wrong in a Mosaic app

- **Never draw dynamic text with Mosaic's `Text`.** It ignores its box and overflows. Use `Label` (sanitises, truncates, wraps), `Stack` (never taller than its box) and `Panel`.
- **`compat.kt` must live in the app** (`com/jakewharton/mosaic/compat.kt`, copied from Arcana/Cosmo into `example`). It shadows Mosaic's own key mapping, which crashes on non-Latin keys; Mosaic calls it by class name, so it cannot move into a library.
- **Start from a clean screen and quit from outside.** See `example/…/Main.kt`: clear the screen, cancel Mosaic's job from another coroutine, then `Runtime.halt(0)`.
- **Without truecolor** Mosaic rounds colours to the 256-colour cube and dark tints turn light. `Palette` fits each channel to the nearest cube level up front.
- **Resizing on macOS** kills the process through Mosaic's native SIGWINCH handler: `rememberTerminalSize` installs `ResizeSignalGuard` 300 ms after the first frame and polls `stty size`.

## Differences from the copies in Arcana and CosmoCLI

Ink merged the two; migrating either app means swapping its own `design/Theme.kt` / `ColorMath` / `Sanitize` for these.

- Env override prefix is a parameter (`TerminalSize.probe(env, "ARCANA")`); the default is `INK`.
- `contrast()` is the real WCAG ratio (CosmoCLI's). Arcana had none; `luminance()` stays the brightness used to decide light or dark.
- `mix`, `hsv` round instead of truncating and clamp their inputs; this can change a derived colour by one step.
- Themes that both apps have are Cosmo's versions (they spell out `entry` / `selected`, Arcana derives them: a difference of about one step). Arcana's Darcula/Solarized prompt-band colours were added to Ink's.
- `Palette` and `MosaicColors` cover every role Arcana reads; CosmoCLI's status-band `Accent` class (badge strengths) is still in Cosmo and is the next thing to extract.

## Requirements

Kotlin 2.3.20, JVM 21, Mosaic 0.18.0 (versions in `gradle/libs.versions.toml`, shared with Arcana). The Gradle wrapper is included (9.2.1).
