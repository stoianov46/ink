package com.idea.ink.theme

/** The colour roles every theme must set (`0xRRGGBB`); everything else the UI draws is mixed from them or set optionally. */
val RequiredRoles: List<String> = listOf(
    "background", "surface", "surfaceHigh", "text", "muted", "dim",
    "green", "red", "blue", "orange", "purple", "cyan",
)

/**
 * A colour theme: a name and its colours by role (`0xRRGGBB`). Plain data with no UI types, so themes can be built in,
 * loaded from a file, and edited in a theme studio. Immutable: the colour map is copied on construction.
 *
 * Besides the [RequiredRoles] a theme may set optional roles (`entry`, `selected`, `focus`, `codeFg`, `promptBand`,
 * `consoleBold`, …); see [InkColors] for what each one falls back to.
 *
 * @param id unique identifier (used in settings and config files)
 * @param name display name
 * @param colors colours by role
 * @param transparent the background, the cards and the list entries are not painted, so the terminal's own background
 *   (its colour, opacity or blur) shows through; the colours of those roles are then only used to judge light or dark
 *   and for preview swatches
 */
class ThemeSpec(
    val id: String,
    val name: String,
    colors: Map<String, Int>,
    val transparent: Boolean = false,
) {
    val colors: Map<String, Int> = colors.toMap()

    init {
        val missing = RequiredRoles.filter { it !in this.colors }
        require(missing.isEmpty()) { "Theme '$id' lacks colours: ${missing.joinToString()}" }
    }

    /** The same theme under another id and name, with some colours replaced. */
    fun with(id: String, name: String, overrides: Map<String, Int>): ThemeSpec = ThemeSpec(id, name, colors + overrides, transparent)

    override fun equals(other: Any?): Boolean =
        other is ThemeSpec && id == other.id && name == other.name && colors == other.colors && transparent == other.transparent

    override fun hashCode(): Int = ((id.hashCode() * 31 + name.hashCode()) * 31 + colors.hashCode()) * 31 + transparent.hashCode()

    override fun toString(): String = "ThemeSpec($id, $name, ${colors.size} colours${if (transparent) ", transparent" else ""})"
}

/**
 * The user's own theme as it is edited and saved: a name, a built-in base and the colours that were set on top of it.
 */
data class CustomTheme(
    val name: String,
    val baseId: String,
    val overrides: Map<String, Int>,
) {
    /** Resolves this into a complete theme on top of [allThemes]; a base that no longer exists falls back to [Themes.Dark]. */
    fun spec(allThemes: List<ThemeSpec> = Themes.all): ThemeSpec {
        val base = allThemes.firstOrNull { it.id == baseId } ?: Themes.Dark
        return base.with(Themes.CUSTOM_ID, name, overrides)
    }
}
