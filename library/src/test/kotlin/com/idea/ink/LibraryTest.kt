package com.idea.ink

import com.idea.ink.colors.ColorSupport
import com.idea.ink.colors.contrast
import com.idea.ink.colors.fitChannelTo256
import com.idea.ink.colors.fitTo256
import com.idea.ink.colors.hsv
import com.idea.ink.colors.luminance
import com.idea.ink.colors.mix
import com.idea.ink.colors.parseHex
import com.idea.ink.colors.toHex
import com.idea.ink.terminal.CellSize
import com.idea.ink.terminal.TerminalSize
import com.idea.ink.text.sanitizeForTerminal
import com.idea.ink.theme.CustomTheme
import com.idea.ink.theme.InkColors
import com.idea.ink.theme.RequiredRoles
import com.idea.ink.theme.ThemeSpec
import com.idea.ink.theme.Themes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ColorTest {
    @Test fun contrastIsWcag() {
        assertEquals(21.0, contrast(0x000000, 0xFFFFFF), 0.01)
        assertEquals(1.0, contrast(0x123456, 0x123456), 0.0001)
        // the known WCAG figure for #777 on white is 4.48; the old brightness formula gave about 4.4 for black-ish greys
        assertEquals(4.48, contrast(0x777777, 0xFFFFFF), 0.02)
        assertEquals(contrast(0x1F2328, 0xE9E9E6), contrast(0xE9E9E6, 0x1F2328), 1e-9)
    }

    @Test fun mixClampsAndRounds() {
        assertEquals(0x808080, mix(0xFFFFFF, 0x000000, 0.5))
        assertEquals(0xFFFFFF, mix(0xFFFFFF, 0x000000, 7.0))
        assertEquals(0x000000, mix(0xFFFFFF, 0x000000, -3.0))
        assertEquals(0x123456, mix(0xABCDEF, 0x123456, 0.0))
    }

    @Test fun hsvWrapsAndClamps() {
        assertEquals(0xFF0000, hsv(0.0, 1.0, 1.0))
        assertEquals(hsv(0.25, 0.5, 0.5), hsv(1.25, 0.5, 0.5))
        assertEquals(hsv(0.0, 1.0, 1.0), hsv(-1.0, 1.0, 1.0))
        assertEquals(0xFFFFFF, hsv(0.3, -5.0, 9.0))
    }

    @Test fun parseHexRejectsGarbage() {
        assertEquals(0x1B2A41, parseHex("#1B2A41"))
        assertEquals(0x1B2A41, parseHex("1b2a41"))
        assertEquals(0xAABBCC, parseHex("#abc"))
        assertEquals(0x1B2A41, parseHex("0x1B2A41"))
        assertNull(parseHex("-1234F"))
        assertNull(parseHex("+1234F"))
        assertNull(parseHex("12345G"))
        assertNull(parseHex("#12345"))
        assertNull(parseHex(null))
    }

    @Test fun toHexIsSixDigitsAndMasked() {
        assertEquals("#0A0A0A", toHex(0x0A0A0A))
        assertEquals("#FFFFFF", toHex(-1))
    }

    @Test fun fitKeepsDarkColoursDark() {
        assertEquals(0, fitChannelTo256(0x1B))
        assertEquals(0x1B2A41, fitTo256(0x1B2A41, truecolor = true))
        // 1B2A41 -> channels 27, 42, 65 -> cube levels 0, 0, 95 (65 is nearer 95 than 0? no: |65-0|=65 > |65-95|=30)
        assertEquals(0x000033, fitTo256(0x1B2A41, truecolor = false))
    }

    @Test fun truecolorFollowsMosaicRules() {
        assertTrue(ColorSupport.isTruecolor(mapOf("COLORTERM" to "truecolor")))
        assertTrue(ColorSupport.isTruecolor(mapOf("TERM" to "xterm-24bit")))
        assertFalse(ColorSupport.isTruecolor(mapOf("TERM" to "xterm-256color")))
        assertFalse(ColorSupport.isTruecolor(mapOf("COLORTERM" to "truecolor", "NO_COLOR" to "1")))
        // COLORTERM wins over TERM, as in Mosaic
        assertFalse(ColorSupport.isTruecolor(mapOf("COLORTERM" to "yes", "TERM" to "xterm-truecolor")))
    }

    @Test fun luminanceTellsLightFromDark() {
        assertTrue(luminance(0xF1ECE4) > 0.5)
        assertTrue(luminance(0x0A0A0A) < 0.5)
    }
}

class ThemeTest {
    @Test fun everyBuiltInThemeIsCompleteAndUnique() {
        assertTrue(Themes.all.size >= 19)
        assertEquals(Themes.all.size, Themes.ids.toSet().size)
        Themes.all.forEach { t -> RequiredRoles.forEach { assertTrue(it in t.colors, "${t.id} lacks $it") } }
    }

    @Test fun missingRolesAreRejected() {
        assertFailsWith<IllegalArgumentException> { ThemeSpec("x", "X", mapOf("background" to 0)) }
    }

    @Test fun colourMapIsCopied() {
        val m = Themes.Dark.colors.toMutableMap()
        val t = ThemeSpec("copy", "Copy", m)
        m["background"] = 0xFFFFFF
        assertEquals(0x0A0A0A, t.colors["background"])
        assertEquals(Themes.Dark, ThemeSpec(Themes.Dark.id, Themes.Dark.name, Themes.Dark.colors))
    }

    @Test fun lookup() {
        assertEquals(Themes.Liberty, Themes.byId("freedom"))
        assertEquals(Themes.Dark, Themes.byId("nope"))
        assertNull(Themes.find("nope"))
        assertTrue(Themes.isLight(Themes.SoftLight))
        assertFalse(Themes.isLight(Themes.Nord))
    }

    @Test fun customThemeWithMissingBaseFallsBackToDark() {
        val custom = CustomTheme("Mine", "deleted-theme", mapOf("purple" to 0x112233)).spec()
        assertEquals(0x112233, custom.colors["purple"])
        assertEquals(Themes.Dark.colors["text"], custom.colors["text"])
        assertEquals(Themes.CUSTOM_ID, custom.id)
    }

    @Test fun withKeepsTransparency() {
        assertTrue(Themes.Ghost.with("g2", "G2", emptyMap()).transparent)
    }

    @Test fun derivedColoursAreStableAndOverridable() {
        val dark = InkColors(Themes.Dark)
        assertFalse(dark.light)
        assertEquals(0x212121, dark.entryBg)               // set by the theme
        assertEquals(mix(0x161616 /*surface*/, 0, 0.0).let { dark.surface }, 0x161616)
        val soft = InkColors(Themes.SoftDark)
        assertEquals(mix(Themes.SoftDark.colors.getValue("surfaceHigh"), Themes.SoftDark.colors.getValue("surface"), 0.5), soft.entryBg) // derived
        assertNull(soft.statusAccent)
        assertNotNull(InkColors(Themes.MonochromeAccent).statusAccent)
        assertTrue(InkColors(Themes.Ghost).transparent)
        assertTrue(InkColors(Themes.Light).light)
    }

    @Test fun thinkShadeRunsQuietToLight() {
        val c = InkColors(Themes.MonochromeAccent)
        assertNull(InkColors(Themes.Dark).thinkShade(0.5))
        assertEquals(c.thinkAccent, c.thinkShade(0.5))
        assertTrue(luminance(c.thinkShade(0.0)!!) < luminance(c.thinkShade(1.0)!!))
    }

    @Test fun themesReadWellEnough() {
        // body text must be readable on the app background in every theme (WCAG AA for body text is 4.5)
        Themes.all.forEach { t ->
            val ratio = contrast(t.colors.getValue("text"), t.colors.getValue("background"))
            assertTrue(ratio >= 4.5, "${t.id}: text on background is only ${"%.2f".format(ratio)}")
        }
    }
}

class TerminalTest {
    @Test fun parsesStty() {
        assertEquals(CellSize(120, 40), TerminalSize.parseStty("40 120\n"))
        assertNull(TerminalSize.parseStty("0 0"))
        assertNull(TerminalSize.parseStty("stty: not a tty"))
        assertNull(TerminalSize.parseStty("40"))
    }

    @Test fun overrideNeedsBothAndPrefix() {
        assertEquals(CellSize(100, 30), TerminalSize.probe(mapOf("INK_COLUMNS" to "100", "INK_ROWS" to "30")))
        assertEquals(CellSize(90, 20), TerminalSize.probe(mapOf("ARCANA_COLUMNS" to "90", "ARCANA_ROWS" to "20"), "ARCANA"))
        assertNull(TerminalSize.override(mapOf("INK_COLUMNS" to "100"), "INK"))
        assertNull(TerminalSize.override(mapOf("INK_COLUMNS" to "0", "INK_ROWS" to "30"), "INK"))
    }
}

class SanitizeTest {
    @Test fun stripsEscapesAndWideCharacters() {
        assertEquals("red", sanitizeForTerminal("\u001B[31mred\u001B[0m"))
        assertEquals("a    b", sanitizeForTerminal("a\tb"))
        assertEquals("ok ✓", sanitizeForTerminal("ok ✅"))
        assertEquals("▪", sanitizeForTerminal("日"))
        assertEquals("e", sanitizeForTerminal("é"))
        assertEquals("line1\nline2", sanitizeForTerminal("line1\nline2"))
    }
}
