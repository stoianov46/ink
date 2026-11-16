package com.idea.ink.colors

/**
 * What colours the terminal gets, decided the way Mosaic decides it: `COLORTERM` first, then `TERM`.
 *
 * Without 24-bit colour Mosaic rounds every channel c to the 256-colour cube level `round(c / 255 * 5)`, shown as
 * 0, 95, 135, 175, 215 or 255, which lifts a dark slate such as `1B2A41` to a much lighter lavender (`5F5F87`);
 * [fitTo256] picks the nearest cube level up front so dark colours stay dark.
 */
object ColorSupport {
    /** `NO_COLOR` is set (to anything non-empty): the user asked for no colour. */
    fun noColor(env: Map<String, String> = System.getenv()): Boolean = !env["NO_COLOR"].isNullOrEmpty()

    /** 24-bit colour is available and wanted. Pass [env] to test; the default reads the real environment. */
    fun isTruecolor(env: Map<String, String> = System.getenv()): Boolean {
        if (noColor(env)) return false
        val term = env["COLORTERM"] ?: env["TERM"] ?: ""
        return term.contains("24bit", ignoreCase = true) || term.contains("truecolor", ignoreCase = true)
    }

    /** [isTruecolor] for this process, read once. */
    val truecolor: Boolean by lazy { isTruecolor() }

    /** The user did not ask for no colour (a 256-colour terminal still shows colour, only approximated). */
    val supportsColor: Boolean get() = !noColor()
}
