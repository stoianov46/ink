package com.idea.ink.theme

import com.idea.ink.colors.luminance

/** Built-in themes for terminal applications: the ones Arcana and CosmoCLI ship, in one place. Plain data only. */
object Themes {
    const val DEFAULT_ID = "dark"

    /** The id of the user's own theme (see [CustomTheme.spec]). */
    const val CUSTOM_ID = "custom"

    val Dark = ThemeSpec(
        "dark", "Dark",
        mapOf(
            "background" to 0x0A0A0A, "surface" to 0x161616, "surfaceHigh" to 0x262626,
            "entry" to 0x212121, "selected" to 0x2A2A2A,
            "text" to 0xEDEDED, "muted" to 0x8C8C8C, "dim" to 0x4A4A4A,
            "green" to 0x3DBE7C, "red" to 0xEF5350, "blue" to 0x4F8BF7, "orange" to 0xF59E0B, "purple" to 0x8B6CF6,
            "gray" to 0x6B7280, "cyan" to 0x5BC8E6,
            "hint" to 0x8097BD, "codeFg" to 0xF2C46D, "valueFg" to 0x9AD9B3,
        ),
    )

    val Light = ThemeSpec(
        "light", "Light",
        mapOf(
            "background" to 0xE9E9E6, "surface" to 0xF6F6F4, "surfaceHigh" to 0xE1E1DD,
            "entry" to 0xECECE9, "selected" to 0xD5D5D0,
            "text" to 0x1F2328, "muted" to 0x5F6770, "dim" to 0x98A0A8,
            "green" to 0x1A7F37, "red" to 0xCF222E, "blue" to 0x0969DA, "orange" to 0xB35900, "purple" to 0x6F42C1,
            "cyan" to 0x0A7EA4,
        ),
    )

    /**
     * IntelliJ-style High contrast, and clearly not a brighter Dark: a pure black app with deep violet cards edged in
     * white, list rows that are always plain grey so they stand off the card, fully saturated colours, no grey text,
     * and an inverted selection (a bright row with black text).
     */
    val HighContrast = ThemeSpec(
        "high-contrast", "High contrast",
        mapOf(
            "background" to 0x000000, "surface" to 0x2C1450, "surfaceHigh" to 0x5B4B7A, "entry" to 0x3A3A3A, "selected" to 0xF2F2F2,
            "text" to 0xFFFFFF, "muted" to 0xF0F0F0, "dim" to 0xD6D6D6,
            "green" to 0x00E676, "red" to 0xFF5252, "blue" to 0x4D9DFF, "orange" to 0xFFAB00, "purple" to 0xE040FB, "gray" to 0x777777, "cyan" to 0x00E5FF,
            "hint" to 0x82B1FF, "codeFg" to 0xFFEA00, "valueFg" to 0x69F0AE,
            "greenBg" to 0x00391C, "redBg" to 0x4D0A0A, "codeBg" to 0x000000, "promptBg" to 0x000000, "finalPromptBg" to 0x004D57,
            "argBg" to 0x5A3AA8, "valueBg" to 0x00471F, "readOnly" to 0x651FFF, "readOnlyText" to 0xD1C4E9,
            "promptBand" to 0x423702, "promptBandActive" to 0xFFE680, "promptText" to 0xFFF7CC, "promptTextActive" to 0x000000,
            "onSelected" to 0x000000, "onSelectedMuted" to 0x2B2B2B, "onSelectedDim" to 0x3A3A3A, "focus" to 0x7A3FF2, "cardEdge" to 0xFFFFFF,
        ),
    )

    /** A calmer dark: dark slate instead of near-black, off-white text and pastel accents, so nothing glares. */
    val SoftDark = ThemeSpec(
        "soft-dark", "Soft Dark",
        mapOf(
            "background" to 0x2B2D33, "surface" to 0x34373E, "surfaceHigh" to 0x3F434B,
            "text" to 0xD6D9DE, "muted" to 0xA3A9B2, "dim" to 0x7A808A,
            "green" to 0x8FD0A5, "red" to 0xEBA0A0, "blue" to 0x8DB4EE, "orange" to 0xE8C082, "purple" to 0xBBA6EA,
            "gray" to 0x8D939D, "cyan" to 0x8CCFD8,
        ),
    )

    /** A calmer light: warm paper instead of near-white, charcoal text and muted accents. */
    val SoftLight = ThemeSpec(
        "soft-light", "Soft Light",
        mapOf(
            "background" to 0xF1ECE4, "surface" to 0xF9F6F0, "surfaceHigh" to 0xE6E0D6,
            "text" to 0x383C43, "muted" to 0x5F646C, "dim" to 0x8C929A,
            "green" to 0x3F7F5A, "red" to 0xA94F54, "blue" to 0x3F6FAE, "orange" to 0x8E6224, "purple" to 0x735AA6,
            "gray" to 0x6B7078, "cyan" to 0x31808C,
        ),
    )

    val Darcula = ThemeSpec(
        "darcula", "Darcula",
        mapOf(
            "background" to 0x232425, "surface" to 0x2B2B2B, "surfaceHigh" to 0x3C3F41,
            "entry" to 0x323436, "selected" to 0x45494A,
            "text" to 0xA9B7C6, "muted" to 0x808080, "dim" to 0x8B919B,
            "green" to 0x6A8759, "red" to 0xFF6B68, "blue" to 0x589DF6, "orange" to 0xCC7832, "purple" to 0x9876AA,
            "gray" to 0x6B6F73, "cyan" to 0x4FB8B8,
            "codeFg" to 0xFFC66D, "valueFg" to 0xA5C261,
            // The prompt line: warm cream text on a semi-transparent yellow (Darcula's own yellow over the row).
            "promptBand" to 0x50493E, "promptBandActive" to 0x686050, "promptText" to 0xFFF1CF,
        ),
    )

    /**
     * Solarized's teal, cream and violet, with greens that suit the teal and more contrast than the original palette.
     * The prompt line sits on a semi-transparent amber strip so it stays amber and does not turn olive on the teal.
     */
    val SolarizedDark = ThemeSpec(
        "solarized-dark", "Solarized Dark",
        mapOf(
            "background" to 0x00212B, "surface" to 0x002B36, "surfaceHigh" to 0x0F4856,
            "entry" to 0x073642, "selected" to 0x17596B,
            "text" to 0xEEE8D5, "muted" to 0xAEBABA, "dim" to 0x8CA1A8,
            "green" to 0x4FC48C, "red" to 0xF25C58, "blue" to 0x4DAAF0, "orange" to 0xDDA82A, "purple" to 0x8F94EA,
            "gray" to 0x8AA0A6, "cyan" to 0x3CC2B8,
            "codeFg" to 0xF0763A,
            "promptBand" to 0x493611, "promptBandActive" to 0x594518, "promptText" to 0xFFF0C4,
        ),
    )

    val SolarizedLight = ThemeSpec(
        "solarized-light", "Solarized Light",
        mapOf(
            "background" to 0xFDF6E3, "surface" to 0xEEE8D5, "surfaceHigh" to 0xDDD6BC,
            "entry" to 0xFFFBEF, "selected" to 0xD3CAAA,
            "text" to 0x073642, "muted" to 0x40585F, "dim" to 0x667B80,
            "green" to 0x1D7F52, "red" to 0xC0272D, "blue" to 0x1B72B8, "orange" to 0x8F6600, "purple" to 0x5A5FB5,
            "gray" to 0x5F7277, "cyan" to 0x157A73,
            "codeFg" to 0xB23E0D,
            "promptBand" to 0xF0E4BF, "promptBandActive" to 0xCDBD88, "promptText" to 0x3B2E00,
        ),
    )

    val Nord = ThemeSpec(
        "nord", "Nord",
        mapOf(
            "background" to 0x242933, "surface" to 0x2E3440, "surfaceHigh" to 0x3B4252,
            "entry" to 0x343B49, "selected" to 0x434C5E,
            "text" to 0xD8DEE9, "muted" to 0x929CB0, "dim" to 0x5B667D,
            "green" to 0xA3BE8C, "red" to 0xBF616A, "blue" to 0x81A1C1, "orange" to 0xD08770, "purple" to 0xB48EAD,
            "gray" to 0x6B7690, "cyan" to 0x88C0D0,
            "codeFg" to 0xEBCB8B,
        ),
    )

    val Cosmo = ThemeSpec(
        "cosmo", "Cosmo",
        mapOf(
            "background" to 0x141F1C, "surface" to 0x2B1539, "surfaceHigh" to 0x3B2650,
            "entry" to 0x1D0E29, "selected" to 0x3A2150,
            "text" to 0xEDEDED, "muted" to 0xA3AFAB, "dim" to 0x7A8782,
            "green" to 0x3DBE7C, "red" to 0xEF5350, "blue" to 0x4F8BF7, "orange" to 0xE0A030, "purple" to 0x8B6CF6,
            "gray" to 0x6F7F7A, "cyan" to 0x5BC8E6,
            "hint" to 0x8EA7D8, "codeFg" to 0xF5C45E, "valueFg" to 0x6BE0A0,
        ),
    )

    /** A green-and-yellow theme, after the Brazilian flag: an emerald base with golden-yellow accents. */
    val Brasil = ThemeSpec(
        "brasil", "Brasil",
        mapOf(
            "background" to 0x1D2E24, "surface" to 0x263B2F, "surfaceHigh" to 0x30493B, "entry" to 0x2B4335, "selected" to 0x3B5C49,
            "text" to 0xDDE8D8, "muted" to 0xA3B9A3, "dim" to 0x738B78,
            "green" to 0xE6CF6A, "red" to 0xE59A82, "blue" to 0x84C49B, "orange" to 0xDDBB5A, "purple" to 0xEBDC9C,
            "gray" to 0x8A9C8C, "cyan" to 0xA0D2B0,
            "codeFg" to 0xE8D27A,
            "promptBand" to 0x263B2F, "promptText" to 0xF0E5B5,
        ),
    )

    /** A blue-and-yellow theme, after the Ukrainian flag: a deep cobalt base with golden-yellow accents. */
    val Liberty = ThemeSpec(
        "liberty", "Liberty",
        mapOf(
            "background" to 0x1B2A41, "surface" to 0x23364F, "surfaceHigh" to 0x2D4560, "entry" to 0x28405A, "selected" to 0x35557A,
            "text" to 0xDCE3EE, "muted" to 0x9FB1C9, "dim" to 0x6F84A0,
            "green" to 0xE3C766, "red" to 0xE59A82, "blue" to 0x7FA8E0, "orange" to 0xDDBB5A, "purple" to 0xEBD998,
            "gray" to 0x8797AE, "cyan" to 0x9CBEE6,
            "codeFg" to 0xE8CF7A,
            "promptBand" to 0x23364F, "promptText" to 0xF0E2B0,
        ),
    )

    /**
     * A see-through theme for a terminal with a transparent or blurred background: nothing behind the text is painted,
     * only the selected row is tinted, and borders, titles and key badges glow in bright neon accents. For a dark terminal.
     */
    val Ghost = ThemeSpec(
        "ghost", "Ghost",
        mapOf(
            "background" to 0x0B0F14, "surface" to 0x11161D, "surfaceHigh" to 0x363D55, "entry" to 0x2B3248, "selected" to 0x35386E, "focus" to 0x4A3F8F,
            "text" to 0xE6EAF2, "muted" to 0xA7B0C0, "dim" to 0x6B7587,
            "green" to 0x5CF2B0, "red" to 0xFF6B8A, "blue" to 0x6EA8FF, "orange" to 0xFFC857, "purple" to 0xB48CFF,
            "gray" to 0x8791A3, "cyan" to 0x4FE3F5,
            "codeFg" to 0xFFD27A, "promptBand" to 0x3B4260, "promptText" to 0xF4E7C4,
        ),
        transparent = true,
    )

    /**
     * One hue, a light blue on black. Roles differ by lightness and saturation, not hue (the UI never uses colour as the
     * only signal); `red` is a muted rose so an error still stands out. Needs a truecolor terminal.
     */
    val Monochrome = ThemeSpec(
        "monochrome", "Monochrome",
        mapOf(
            "background" to 0x000000, "surface" to 0x0B131D, "surfaceHigh" to 0x17232F, "entry" to 0x121D29, "selected" to 0x1D2C42,
            "text" to 0xA7B6D6, "muted" to 0x7B8AAA, "dim" to 0x4A566B,
            "green" to 0xB8D4E8, "red" to 0xC48CA0, "blue" to 0x8EAEEB, "orange" to 0xC8D2E6, "purple" to 0x7F9AD0,
            "gray" to 0x7A869A, "cyan" to 0xD3DDF2,
            "codeFg" to 0xC8D2E6,
            "todo" to 0x7FD6A4,
            "consoleUser" to 0xD3DDF2, "consoleTool" to 0x7F9AD0, "consoleCodeFg" to 0x8EAEEB, "consoleCodeBg" to 0x111C2B,
        ),
    )

    /** [Monochrome] plus ONE warm accent, amber (the complement of the blue): warnings, your own messages, list markers, effort and context. */
    val MonochromeAccent = ThemeSpec(
        "monochrome-accent", "Monochrome Accent",
        Monochrome.colors + mapOf(
            "orange" to 0xE8B35E, "consoleTool" to 0xE8B35E,
            "consoleBold" to 0x6FD0CC, "consoleItalic" to 0xB8D4E8, "consoleLink" to 0x8EAEEB, "consoleNumber" to 0xE8B35E,
            "consoleHeadingMark" to 0xE8B35E, "consoleQuote" to 0xB8924A, "syntaxString" to 0xF0C674, "syntaxNumber" to 0xE4A04F,
            "statusAccent" to 0xE8B35E, "thinkAccent" to 0xE8B35E, "contextAccent" to 0xE8B35E,
        ),
    )

    val ColourblindDark = ThemeSpec(
        "colourblind-dark", "Colour-blind Dark",
        mapOf(
            "background" to 0x0A0A0A, "surface" to 0x161616, "surfaceHigh" to 0x262626,
            "entry" to 0x212121, "selected" to 0x2A2A2A,
            "text" to 0xEDEDED, "muted" to 0x9A9A9A, "dim" to 0x5A5A5A,
            "green" to 0x009E73, "red" to 0xD55E00, "blue" to 0x0072B2, "orange" to 0xE69F00,
            "purple" to 0xCC79A7, "gray" to 0x6B7280, "cyan" to 0x56B4E9,
        ),
    )

    val ColourblindLight = ThemeSpec(
        "colourblind-light", "Colour-blind Light",
        mapOf(
            "background" to 0xF2F2EF, "surface" to 0xFBFBF9, "surfaceHigh" to 0xE6E6E2,
            "entry" to 0xEDEDEA, "selected" to 0xD7D7D1,
            "text" to 0x1F2328, "muted" to 0x5F6770, "dim" to 0x8B939B,
            "green" to 0x007A59, "red" to 0xB34700, "blue" to 0x005B8F, "orange" to 0x9A6A00,
            "purple" to 0xA64D79, "gray" to 0x6E7781, "cyan" to 0x0A6E8F,
        ),
    )

    val Amber = ThemeSpec(
        "amber", "Amber terminal",
        mapOf(
            "background" to 0x110D07, "surface" to 0x1A140B, "surfaceHigh" to 0x2B2212,
            "entry" to 0x211A0E, "selected" to 0x372B16,
            "text" to 0xE6B450, "muted" to 0xB08A40, "dim" to 0x7A5E2E,
            "green" to 0x6FD36A, "red" to 0xE86A4A, "blue" to 0x7FB4DC, "orange" to 0xFFE066, "purple" to 0xCFA3D6,
            "gray" to 0x9A8460, "cyan" to 0x6FCDB0,
            "codeFg" to 0xFFEB8A, "valueFg" to 0x9BE08A,
        ),
    )

    val Hacker = ThemeSpec(
        "hacker", "Hacker terminal",
        mapOf(
            "background" to 0x0C0D0C, "surface" to 0x141614, "surfaceHigh" to 0x232623,
            "entry" to 0x1A1C1A, "selected" to 0x2A302A,
            "text" to 0xD0D4D0, "muted" to 0x8E948E, "dim" to 0x5E645E,
            "green" to 0x2EE66B, "red" to 0xFF5C5C, "blue" to 0x5CB8FF, "orange" to 0xFFD21F,
            "purple" to 0xC08CFF, "gray" to 0x7A807A, "cyan" to 0x19F0D8,
            "codeFg" to 0xE6F25C, "valueFg" to 0x9CE8B4,
        ),
    )

    /** All built-in themes in the order they appear in settings UIs. */
    val all: List<ThemeSpec> = listOf(
        Dark, Light, HighContrast, SoftDark, SoftLight, Darcula,
        SolarizedDark, SolarizedLight, Nord, Cosmo, Brasil, Liberty, Ghost,
        Monochrome, MonochromeAccent, ColourblindDark, ColourblindLight, Amber, Hacker,
    )

    /** All theme ids, useful for dropdowns in settings. */
    val ids: List<String> = all.map { it.id }

    /** Display names paired with ids: what a settings screen shows. */
    val choices: List<Pair<String, String>> = all.map { it.id to it.name }

    /** The theme with this id, or null. */
    fun find(id: String): ThemeSpec? = all.firstOrNull { it.id == (if (id == "freedom") "liberty" else id) }

    /** Lookup by id; [Dark] for an unknown id (`freedom`, the old name of Liberty, still resolves). */
    fun byId(id: String): ThemeSpec = find(id) ?: Dark

    /** True if the theme's background is bright (perceived brightness above 0.5). */
    fun isLight(theme: ThemeSpec): Boolean = luminance(theme.colors.getValue("background")) > 0.5

    /** The colours a theme is recognised by, for a preview strip: backgrounds, text and the accent colours. */
    fun swatchColors(theme: ThemeSpec): List<Int> =
        listOf("background", "surface", "surfaceHigh", "text", "muted", "green", "red", "blue", "orange", "purple", "cyan").map { theme.colors.getValue(it) }
}
