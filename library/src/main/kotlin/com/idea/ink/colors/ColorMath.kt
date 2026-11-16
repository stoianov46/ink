package com.idea.ink.colors

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToInt

private val CubeLevels = intArrayOf(0, 95, 135, 175, 215, 255)

private fun channel(rgb: Rgb, shift: Int) = (rgb shr shift) and 0xFF

/** [channel] (0..255) moved to the nearest of the six 256-colour cube levels, as a value (level * 51) that Mosaic rounds back to exactly that level. */
fun fitChannelTo256(channel: Int): Int = CubeLevels.indices.minBy { abs(CubeLevels[it] - channel.coerceIn(0, 255)) } * 51

/** [rgb] with every channel fitted to the 256-colour cube, unless [truecolor] (the default: this terminal's) in which case it is unchanged. */
fun fitTo256(rgb: Rgb, truecolor: Boolean = ColorSupport.truecolor): Rgb =
    if (truecolor) rgb else rgb(fitChannelTo256(red(rgb)), fitChannelTo256(green(rgb)), fitChannelTo256(blue(rgb)))

/** [fg] laid over [bg] at [alpha] (0..1, clamped): what a see-through colour looks like on a terminal that cannot do transparency. */
fun mix(fg: Rgb, bg: Rgb, alpha: Double): Rgb {
    val a = alpha.coerceIn(0.0, 1.0)
    return listOf(16, 8, 0).fold(0) { acc, shift ->
        val b = channel(bg, shift)
        (acc shl 8) or (b + a * (channel(fg, shift) - b)).roundToInt().coerceIn(0, 255)
    }
}

/** Hue [h] (0..1, wraps), saturation and value (0..1, clamped) as `0xRRGGBB`. */
fun hsv(h: Double, s: Double, v: Double): Rgb {
    val hue = h - floor(h)
    val sat = s.coerceIn(0.0, 1.0)
    val value = v.coerceIn(0.0, 1.0)
    val sector = (hue * 6).toInt() % 6
    val f = hue * 6 - (hue * 6).toInt()
    val p = value * (1 - sat)
    val q = value * (1 - f * sat)
    val t = value * (1 - (1 - f) * sat)
    val (r, g, b) = when (sector) {
        0 -> Triple(value, t, p)
        1 -> Triple(q, value, p)
        2 -> Triple(p, value, t)
        3 -> Triple(p, q, value)
        4 -> Triple(t, p, value)
        else -> Triple(value, p, q)
    }
    return rgb((r * 255).roundToInt(), (g * 255).roundToInt(), (b * 255).roundToInt())
}

/**
 * Perceived brightness of [rgb] (0..1, Rec. 601 weights on the encoded values). It is what decides whether a theme is light or dark.
 * For contrast ratios use [relativeLuminance] and [contrast].
 */
fun luminance(rgb: Rgb): Double = (0.299 * red(rgb) + 0.587 * green(rgb) + 0.114 * blue(rgb)) / 255

/** WCAG relative luminance (0 = black, 1 = white): the channels linearised, then weighted. */
fun relativeLuminance(rgb: Rgb): Double {
    fun lin(shift: Int): Double = (channel(rgb, shift) / 255.0).let { if (it <= 0.03928) it / 12.92 else ((it + 0.055) / 1.055).pow(2.4) }
    return 0.2126 * lin(16) + 0.7152 * lin(8) + 0.0722 * lin(0)
}

/** WCAG contrast ratio between two colours (1 = identical, 21 = black on white); 4.5 is the usual minimum for body text. */
fun contrast(a: Rgb, b: Rgb): Double {
    val x = relativeLuminance(a)
    val y = relativeLuminance(b)
    return (maxOf(x, y) + 0.05) / (minOf(x, y) + 0.05)
}

/** Hue, saturation and lightness, each 0..1. */
class Hsl(val h: Double, val s: Double, val l: Double)

fun toHsl(rgb: Rgb): Hsl {
    val r = red(rgb) / 255.0
    val g = green(rgb) / 255.0
    val b = blue(rgb) / 255.0
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val l = (max + min) / 2
    if (max == min) return Hsl(0.0, 0.0, l)
    val d = max - min
    val s = if (l > 0.5) d / (2 - max - min) else d / (max + min)
    val h = when (max) {
        r -> (g - b) / d + (if (g < b) 6 else 0)
        g -> (b - r) / d + 2
        else -> (r - g) / d + 4
    } / 6
    return Hsl(h, s, l)
}

fun fromHsl(h: Double, s: Double, l: Double): Rgb {
    val ll = l.coerceIn(0.0, 1.0)
    val ss = s.coerceIn(0.0, 1.0)
    fun byte(x: Double) = (x * 255 + 0.5).toInt().coerceIn(0, 255)
    if (ss == 0.0) return byte(ll).let { (it shl 16) or (it shl 8) or it }
    val q = if (ll < 0.5) ll * (1 + ss) else ll + ss - ll * ss
    val p = 2 * ll - q
    fun f(t0: Double): Double {
        val t = ((t0 % 1.0) + 1.0) % 1.0
        return when {
            t < 1.0 / 6 -> p + (q - p) * 6 * t
            t < 0.5 -> q
            t < 2.0 / 3 -> p + (q - p) * (2.0 / 3 - t) * 6
            else -> p
        }
    }
    return (byte(f(h + 1.0 / 3)) shl 16) or (byte(f(h)) shl 8) or byte(f(h - 1.0 / 3))
}

/** [rgb] with its hue, saturation and lightness moved by the given amounts (hue wraps, the others stop at their ends). */
fun shifted(rgb: Rgb, dHue: Double = 0.0, dSat: Double = 0.0, dLight: Double = 0.0): Rgb {
    val c = toHsl(rgb)
    // A grey has no hue to turn; give it a little saturation so a hue change shows.
    val s = if (c.s == 0.0 && dHue != 0.0) 0.2 else c.s
    return fromHsl(((c.h + dHue) % 1.0 + 1.0) % 1.0, s + dSat, c.l + dLight)
}
