package com.idea.ink.theme

import com.idea.ink.colors.Rgb
import com.idea.ink.colors.hsv
import com.idea.ink.colors.luminance
import com.idea.ink.colors.mix

/**
 * Every colour one theme gives the UI, as `0xRRGGBB`: what the [ThemeSpec] sets, and the rest derived from it by Ink's
 * colour maths (tints mixed into the background, softened status colours, tab and chart colours…). The `ink-mosaic`
 * module turns these into terminal colours; nothing here depends on Mosaic.
 *
 * Roles a theme may set itself are optional keys of [ThemeSpec.colors]; each documents what it falls back to.
 */
class InkColors(val spec: ThemeSpec) {
    private val c = spec.colors

    /** The colour a theme may set itself (by [key]); otherwise [default] is derived from the required ones. */
    private fun opt(key: String, default: () -> Rgb): Rgb = c[key] ?: default()

    private val backgroundRgb = c.getValue("background")
    private val surfaceRgb = c.getValue("surface")
    private val surfaceHighRgb = c.getValue("surfaceHigh")
    private val textRgb = c.getValue("text")
    private val mutedRgb = c.getValue("muted")
    private val greenRgb = c.getValue("green")
    private val redRgb = c.getValue("red")
    private val blueRgb = c.getValue("blue")
    private val orangeRgb = c.getValue("orange")
    private val purpleRgb = c.getValue("purple")
    private val cyanRgb = c.getValue("cyan")

    /** The background is bright: tints and badge colours then work the other way round. */
    val light: Boolean = luminance(backgroundRgb) > 0.5

    /** The theme paints no background, card or entry: the terminal's own shows through. */
    val transparent: Boolean get() = spec.transparent

    private val entryRgb = opt("entry") { mix(surfaceHighRgb, surfaceRgb, 0.5) }
    private val selectedRgb = opt("selected") { mix(textRgb, surfaceHighRgb, 0.06) }
    private val codeBgRgb = opt("codeBg") { mix(blueRgb, surfaceRgb, 0.12) }
    private val codeFgRgb = opt("codeFg") { mix(orangeRgb, textRgb, 0.35) }

    // ---- Text on a selected row: the same as the plain colours unless the theme inverts its selection (a bright row needs dark text) ----
    val onSelected: Rgb = opt("onSelected") { textRgb }
    val onSelectedMuted: Rgb = opt("onSelectedMuted") { mutedRgb }
    val onSelectedDim: Rgb = opt("onSelectedDim") { c.getValue("dim") }

    // ---- Surfaces ----
    val background: Rgb = backgroundRgb
    val surface: Rgb = surfaceRgb

    /** Raised blocks and the input field that is not focused: solid even in a transparent theme, so a field never disappears. */
    val surfaceHigh: Rgb = surfaceHighRgb

    /** A list entry's own block, a step away from the card it sits on. */
    val entryBg: Rgb = entryRgb
    val selected: Rgb = selectedRgb

    /** The background of the input that has the keyboard (`focus`, else [selected]). */
    val focus: Rgb = opt("focus") { selectedRgb }

    // ---- Text and status colours ----
    val text: Rgb = textRgb
    val muted: Rgb = mutedRgb
    val dim: Rgb = c.getValue("dim")
    val green: Rgb = greenRgb
    val red: Rgb = redRgb
    val blue: Rgb = blueRgb
    val orange: Rgb = orangeRgb
    val purple: Rgb = purpleRgb
    val cyan: Rgb = cyanRgb

    /** A mid grey that takes black badge text and white chip text alike (`gray`, else the muted colour faded into the background). */
    val gray: Rgb = opt("gray") { mix(mutedRgb, backgroundRgb, 0.7) }

    /** Calmer versions of the status colours for large areas such as chart bars: the colour mixed into the background. */
    val greenSoft: Rgb = mix(greenRgb, backgroundRgb, 0.55)
    val redSoft: Rgb = mix(redRgb, backgroundRgb, 0.55)
    val orangeSoft: Rgb = mix(orangeRgb, backgroundRgb, 0.55)
    val cyanSoft: Rgb = mix(cyanRgb, backgroundRgb, 0.55)
    val accentSoft: Rgb = mix(purpleRgb, backgroundRgb, 0.55)

    /** The selected tab: a soft cyan tint over the background, with the normal text colour in bold. For the page tabs of a screen. */
    val tabOn: Rgb = mix(cyanRgb, backgroundRgb, 0.45)

    /** The selected tab of the sidebar and of a second row of tabs: a soft violet tint. */
    val tabOnAccent: Rgb = mix(purpleRgb, backgroundRgb, 0.45)

    /** Three chart hues (cyan, violet, amber) mixed into the background by the same amount, so they read as one set. */
    val chartA: Rgb = mix(cyanRgb, backgroundRgb, 0.82)
    val chartB: Rgb = mix(purpleRgb, backgroundRgb, 0.82)
    val chartC: Rgb = mix(orangeRgb, backgroundRgb, 0.82)

    /** The empty part of a bar: quiet, but visible. */
    val chartTrack: Rgb = mix(mutedRgb, backgroundRgb, 0.62)
    val greenBg: Rgb = opt("greenBg") { mix(greenRgb, surfaceRgb, 0.15) }
    val redBg: Rgb = opt("redBg") { mix(redRgb, surfaceRgb, 0.17) }

    /** The brand colour: key badges, the selection bar, the logo. */
    val accent: Rgb = purpleRgb
    val accentBg: Rgb = mix(purpleRgb, surfaceRgb, 0.16)

    // ---- Message roles: one colour per kind of message, so text of different meaning never looks the same ----
    /** Call to action: the next thing to press. Cyan, bold, marked with "→". */
    val cta: Rgb = cyanRgb

    /** The title of an input field: a light blue of the theme's own blue. */
    val fieldLabel: Rgb = mix(blueRgb, textRgb, 0.35)

    /** Hint: a tip or an explanation. A soft steel blue, marked with "›". */
    val hint: Rgb = opt("hint") { mix(blueRgb, mutedRgb, 0.5) }

    /** Warning: something is blocked, limited or needs care. Marked with "⚠". */
    val warn: Rgb = orangeRgb

    // ---- The console: model output (Markdown), styled apart from the other panels ----
    val consoleText: Rgb = opt("consoleText") { textRgb }
    val consoleMuted: Rgb = opt("consoleMuted") { mutedRgb }
    val consoleDim: Rgb = opt("consoleDim") { dim }
    private val consoleUserRgb = opt("consoleUser") { blueRgb }

    /** Headings, links and the speaker of a message. */
    val consoleUser: Rgb = consoleUserRgb

    /** The colour of **bold** text in the body (null: the text colour, bold only). */
    val consoleBold: Rgb? = c["consoleBold"]

    /** The colour of *italic* text in the body (null: the text colour, italic only). */
    val consoleItalic: Rgb? = c["consoleItalic"]

    /** Links: the heading colour unless set. */
    val consoleLink: Rgb = opt("consoleLink") { consoleUserRgb }

    /** Figures in the text: the call-to-action colour unless set. */
    val consoleNumber: Rgb = opt("consoleNumber") { cyanRgb }

    /** The block before a level 1 and 2 heading: the accent unless set. */
    val consoleHeadingMark: Rgb = opt("consoleHeadingMark") { purpleRgb }

    /** The bar of a quotation: the accent unless set. */
    val consoleQuote: Rgb = opt("consoleQuote") { purpleRgb }

    /** In code blocks: strings (the green role unless set) and numbers (the orange role unless set). */
    val syntaxString: Rgb = opt("syntaxString") { greenRgb }
    val syntaxNumber: Rgb = opt("syntaxNumber") { orangeRgb }

    /** List markers (the orange role unless set). */
    val consoleTool: Rgb = opt("consoleTool") { orangeRgb }

    /** The task list: the green role, unless a theme sets `todo` (the monochrome themes do: their `green` is a blue). */
    val todo: Rgb = opt("todo") { greenRgb }
    val consoleOk: Rgb = opt("consoleOk") { greenRgb }
    val consoleErr: Rgb = opt("consoleErr") { redRgb }

    /** Inline code: a warm foreground on a tinted chip; fenced code blocks sit on the same tint. */
    val consoleCodeFg: Rgb = opt("consoleCodeFg") { codeFgRgb }
    val consoleCodeBg: Rgb = opt("consoleCodeBg") { codeBgRgb }

    /** Your own messages: an amber name on a semi-transparent amber band with warm cream text. A theme can set `promptBand`, `promptText`. */
    val userName: Rgb = orangeRgb
    val userBg: Rgb = opt("promptBand") { mix(orangeRgb, surfaceRgb, if (light) 0.22 else 0.15) }
    val userText: Rgb = opt("promptText") { mix(orangeRgb, textRgb, 0.22) }

    // ---- Optional accents: a theme may colour the status pane, the thinking effort and the context bar by ONE accent ----
    /** The status pane's own values (null = the usual green/orange/red scales). */
    val statusAccent: Rgb? = c["statusAccent"]

    /** The thinking effort, low to high: a quiet shade of the accent, the accent, a light shade of it (see [thinkShade]). */
    val thinkAccent: Rgb? = c["thinkAccent"]

    /** The context bar: the accent while there is room, a light shade of it getting full; the red role past the limit stays red. */
    val contextAccent: Rgb? = c["contextAccent"]

    /** [t] 0..1 (low to high effort) as a shade of the think accent; null without one. */
    fun thinkShade(t: Double): Rgb? = thinkAccent?.let { if (t < 0.34) mix(it, backgroundRgb, 0.6) else if (t < 0.8) it else mix(0xFFFFFF, it, 0.4) }

    /** The context accent made lighter, for a bar that is getting full. */
    val contextHot: Rgb? = contextAccent?.let { mix(0xFFFFFF, it, 0.4) }

    /** One colour of the animated logo's rainbow, [t] (0..1, wrapping) round the colour wheel: soft pastels. */
    fun logoShade(t: Double): Rgb = hsv((t % 1.0 + 1.0) % 1.0, if (light) 0.45 else 0.34, if (light) 0.70 else 0.86)

    /** The "you are typing" marker. */
    val editing: Rgb = if (light) 0xB5001F else 0xE0143C
}
