package com.idea.ink.colors

/**
 * A colour as `0xRRGGBB` in a plain [Int]. The library works entirely with Int colours; the `ink-mosaic` module wraps
 * them into Mosaic's `Color`. These are plain functions (not extensions) so `5.red()` does not become valid for every Int.
 */
typealias Rgb = Int

/** The red channel (0..255) of [rgb]. */
fun red(rgb: Rgb): Int = (rgb shr 16) and 0xFF

/** The green channel (0..255) of [rgb]. */
fun green(rgb: Rgb): Int = (rgb shr 8) and 0xFF

/** The blue channel (0..255) of [rgb]. */
fun blue(rgb: Rgb): Int = rgb and 0xFF

/** Packs three channel values into `0xRRGGBB`; each is clamped to 0..255. */
fun rgb(red: Int, green: Int, blue: Int): Rgb =
    (red.coerceIn(0, 255) shl 16) or (green.coerceIn(0, 255) shl 8) or blue.coerceIn(0, 255)

/** Parses `#RRGGBB`, `RRGGBB`, `0xRRGGBB` or `#RGB`; null for anything else (signs, spaces and non-hex digits included). */
fun parseHex(text: String?): Rgb? {
    val t = text?.trim()?.removePrefix("#")?.removePrefix("0x")?.removePrefix("0X") ?: return null
    val full = if (t.length == 3) t.map { "$it$it" }.joinToString("") else t
    if (full.length != 6 || !full.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) return null
    return full.toInt(16)
}

/** [rgb] as `#RRGGBB` (any alpha bits are dropped). */
fun toHex(rgb: Rgb): String {
    val digits = "0123456789ABCDEF"
    val v = rgb and 0xFFFFFF
    return buildString(7) {
        append('#')
        for (shift in intArrayOf(20, 16, 12, 8, 4, 0)) append(digits[(v shr shift) and 0xF])
    }
}
