package com.idea.ink.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.idea.ink.colors.ColorSupport
import com.idea.ink.colors.Rgb
import com.idea.ink.colors.blue
import com.idea.ink.colors.fitChannelTo256
import com.idea.ink.colors.green
import com.idea.ink.colors.red
import com.idea.ink.theme.InkColors
import com.idea.ink.theme.ThemeSpec
import com.idea.ink.theme.Themes
import com.jakewharton.mosaic.ui.Color

/** `0xRRGGBB` as a Mosaic colour; on a terminal without 24-bit colour each channel is first moved to the nearest 256-colour cube level so dark colours stay dark. */
fun Rgb.toMosaic(truecolor: Boolean = ColorSupport.truecolor): Color =
    if (truecolor) Color(red(this), green(this), blue(this)) else Color(fitChannelTo256(red(this)), fitChannelTo256(green(this)), fitChannelTo256(blue(this)))

/** Every colour of one theme as terminal colours: [InkColors] turned into Mosaic [Color]s. A transparent theme leaves the background, cards and list entries unpainted. */
class MosaicColors(val ink: InkColors, private val truecolor: Boolean = ColorSupport.truecolor) {
    private fun Rgb.c() = toMosaic(truecolor)
    private fun ground(rgb: Rgb): Color = if (ink.transparent) Color.Unspecified else rgb.c()

    val spec: ThemeSpec get() = ink.spec
    val light = ink.light

    val Background = ground(ink.background)
    val Surface = ground(ink.surface)

    /** The theme's real background colour, even when it is transparent: the text colour on a badge or a highlighted tab, where a see-through [Background] would vanish on the accent. */
    val Ink = ink.background.c()
    val SurfaceHigh = ink.surfaceHigh.c()
    val EntryBg = ground(ink.entryBg)
    val Selected = ink.selected.c()
    val OnSelected = ink.onSelected.c()
    val OnSelectedMuted = ink.onSelectedMuted.c()
    val OnSelectedDim = ink.onSelectedDim.c()
    val Focus = ink.focus.c()
    val Text = ink.text.c()
    val Muted = ink.muted.c()
    val Dim = ink.dim.c()
    val Green = ink.green.c()
    val Red = ink.red.c()
    val Blue = ink.blue.c()
    val Orange = ink.orange.c()
    val Purple = ink.purple.c()
    val Cyan = ink.cyan.c()
    val Gray = ink.gray.c()
    val GreenSoft = ink.greenSoft.c()
    val RedSoft = ink.redSoft.c()
    val OrangeSoft = ink.orangeSoft.c()
    val CyanSoft = ink.cyanSoft.c()
    val AccentSoft = ink.accentSoft.c()
    val TabOn = ink.tabOn.c()
    val TabOnAccent = ink.tabOnAccent.c()
    val ChartA = ink.chartA.c()
    val ChartB = ink.chartB.c()
    val ChartC = ink.chartC.c()
    val ChartTrack = ink.chartTrack.c()
    val GreenBg = ink.greenBg.c()
    val RedBg = ink.redBg.c()
    val Accent = ink.accent.c()
    val AccentBg = ink.accentBg.c()
    val Cta = ink.cta.c()
    val FieldLabel = ink.fieldLabel.c()
    val Hint = ink.hint.c()
    val Warn = ink.warn.c()
    val Editing = ink.editing.c()
    val UserName = ink.userName.c()
    val UserBg = ink.userBg.c()
    val UserText = ink.userText.c()
    val ConsoleText = ink.consoleText.c()
    val ConsoleMuted = ink.consoleMuted.c()
    val ConsoleDim = ink.consoleDim.c()
    val ConsoleUser = ink.consoleUser.c()
    val ConsoleTool = ink.consoleTool.c()
    val ConsoleOk = ink.consoleOk.c()
    val ConsoleErr = ink.consoleErr.c()
    val ConsoleCodeFg = ink.consoleCodeFg.c()
    val ConsoleCodeBg = ink.consoleCodeBg.c()
    val ConsoleBold: Color? = ink.consoleBold?.c()
    val ConsoleItalic: Color? = ink.consoleItalic?.c()
    val ConsoleLink = ink.consoleLink.c()
    val ConsoleNumber = ink.consoleNumber.c()
    val ConsoleHeadingMark = ink.consoleHeadingMark.c()
    val ConsoleQuote = ink.consoleQuote.c()
    val SyntaxString = ink.syntaxString.c()
    val SyntaxNumber = ink.syntaxNumber.c()
    val Todo = ink.todo.c()
    val StatusAccent: Color? = ink.statusAccent?.c()
    val ThinkAccent: Color? = ink.thinkAccent?.c()
    val ContextAccent: Color? = ink.contextAccent?.c()
    val ContextHot: Color? = ink.contextHot?.c()
    fun thinkShade(t: Double): Color? = ink.thinkShade(t)?.c()
    fun logoShade(t: Double): Color = ink.logoShade(t).c()
}

/**
 * The colours the UI draws with. Every `Palette.X` reads the active theme, and the active theme is Compose state, so
 * choosing another theme with [use] redraws every screen that has read a colour.
 */
object Palette {
    private var colors by mutableStateOf(MosaicColors(InkColors(Themes.Dark)))

    val spec: ThemeSpec get() = colors.spec
    val themeId: String get() = colors.spec.id
    val themeName: String get() = colors.spec.name

    /** Makes [spec] the theme of the whole app. */
    fun use(spec: ThemeSpec) {
        colors = MosaicColors(InkColors(spec))
    }

    val light: Boolean get() = colors.light
    val Background get() = colors.Background
    val Surface get() = colors.Surface
    val Ink get() = colors.Ink
    val SurfaceHigh get() = colors.SurfaceHigh
    val EntryBg get() = colors.EntryBg
    val Selected get() = colors.Selected
    val OnSelected get() = colors.OnSelected
    val OnSelectedMuted get() = colors.OnSelectedMuted
    val OnSelectedDim get() = colors.OnSelectedDim
    val Focus get() = colors.Focus
    val Text get() = colors.Text
    val Muted get() = colors.Muted
    val Dim get() = colors.Dim
    val Green get() = colors.Green
    val Red get() = colors.Red
    val Blue get() = colors.Blue
    val Orange get() = colors.Orange
    val Purple get() = colors.Purple
    val Cyan get() = colors.Cyan
    val Gray get() = colors.Gray
    val GreenSoft get() = colors.GreenSoft
    val RedSoft get() = colors.RedSoft
    val OrangeSoft get() = colors.OrangeSoft
    val CyanSoft get() = colors.CyanSoft
    val AccentSoft get() = colors.AccentSoft
    val TabOn get() = colors.TabOn
    val TabOnAccent get() = colors.TabOnAccent
    val ChartA get() = colors.ChartA
    val ChartB get() = colors.ChartB
    val ChartC get() = colors.ChartC
    val ChartTrack get() = colors.ChartTrack
    val GreenBg get() = colors.GreenBg
    val RedBg get() = colors.RedBg
    val Accent get() = colors.Accent
    val AccentBg get() = colors.AccentBg
    val Cta get() = colors.Cta
    val FieldLabel get() = colors.FieldLabel
    val Hint get() = colors.Hint
    val Warn get() = colors.Warn
    val Editing get() = colors.Editing
    val UserName get() = colors.UserName
    val UserBg get() = colors.UserBg
    val UserText get() = colors.UserText
    val ConsoleText get() = colors.ConsoleText
    val ConsoleMuted get() = colors.ConsoleMuted
    val ConsoleDim get() = colors.ConsoleDim
    val ConsoleUser get() = colors.ConsoleUser
    val ConsoleTool get() = colors.ConsoleTool
    val ConsoleOk get() = colors.ConsoleOk
    val ConsoleErr get() = colors.ConsoleErr
    val ConsoleCodeFg get() = colors.ConsoleCodeFg
    val ConsoleCodeBg get() = colors.ConsoleCodeBg
    val ConsoleBold get() = colors.ConsoleBold
    val ConsoleItalic get() = colors.ConsoleItalic
    val ConsoleLink get() = colors.ConsoleLink
    val ConsoleNumber get() = colors.ConsoleNumber
    val ConsoleHeadingMark get() = colors.ConsoleHeadingMark
    val ConsoleQuote get() = colors.ConsoleQuote
    val SyntaxString get() = colors.SyntaxString
    val SyntaxNumber get() = colors.SyntaxNumber
    val Todo get() = colors.Todo
    val StatusAccent get() = colors.StatusAccent
    val ThinkAccent get() = colors.ThinkAccent
    val ContextAccent get() = colors.ContextAccent
    val ContextHot get() = colors.ContextHot
    fun thinkShade(t: Double) = colors.thinkShade(t)
    fun logoShade(t: Double) = colors.logoShade(t)

    /** The preview strip of a theme as terminal colours (it does not change the active theme). */
    fun swatches(spec: ThemeSpec): List<Color> = Themes.swatchColors(spec).map { it.toMosaic() }
}

/** Sizes the layout is built around. */
object Dimens {
    /** Below this there is no sensible layout; above it, everything shrinks / hides sections to fit. */
    const val MinColumns = 60
    const val MinRows = 12

    /** Terminal rows left unused at the bottom (the cursor row), so the inline frame never scrolls the screen. */
    const val ReservedRows = 1
    const val SidebarWidth = 24
    const val ContentPadding = 2
}
