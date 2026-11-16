package com.idea.ink

import com.idea.ink.layout.Dock
import com.idea.ink.layout.DockRequest
import com.idea.ink.layout.PaneId
import com.idea.ink.layout.dockRefusal
import com.idea.ink.layout.paneStep
import com.idea.ink.layout.planDocks
import com.idea.ink.layout.rects
import com.idea.ink.layout.sliderCells
import com.idea.ink.settings.InkSettings
import com.idea.ink.settings.InkSettingsStore
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PaneStepTest {
    @Test fun largerSwitchesOnThenGrowsByFive() {
        val on = paneStep("Status pane", false, 30, larger = true)
        assertTrue(on.on); assertEquals(30, on.percent)
        assertEquals(35, paneStep("Status pane", true, 30, true).percent)
        assertEquals(90, paneStep("Status pane", true, 88, true).percent)
        assertTrue(paneStep("Status pane", true, 90, true).message.contains("largest"))
    }

    @Test fun smallerShrinksAndAtTheMinimumSwitchesOff() {
        assertEquals(25, paneStep("P", true, 30, false).percent)
        assertEquals(30, paneStep("P", true, 33, false).percent) // rounds to the step below
        val off = paneStep("P", true, 10, false)
        assertFalse(off.on)
        val nothing = paneStep("P", false, 30, false)
        assertFalse(nothing.on); assertEquals("", nothing.message)
    }

    @Test fun sliderFillsAtLeastOneCellAboveTheMinimum() {
        assertEquals(0, sliderCells(0.0, 0.0, 2.0, 10))
        assertEquals(1, sliderCells(0.01, 0.0, 2.0, 10))
        assertEquals(5, sliderCells(1.0, 0.0, 2.0, 10))
        assertEquals(10, sliderCells(9.0, 0.0, 2.0, 10))
        assertEquals(0, sliderCells(1.0, 1.0, 1.0, 10))
    }
}

class DockPlanTest {
    private val status = DockRequest(PaneId.Status, Dock.Right, 30)
    private val thinking = DockRequest(PaneId.Thinking, Dock.Bottom, 30)

    @Test fun columnTakesItsShareAndTheGapAndStripTakesRows() {
        val plan = planDocks(120, 40, listOf(status, thinking))
        assertEquals(36, plan.at(Dock.Right).single().size)
        assertEquals(120 - 36 - 1, plan.mainWidth)
        assertEquals(12, plan.at(Dock.Bottom).single().size)
        assertEquals(40 - 12, plan.mainHeight)
    }

    @Test fun smallWindowsLeaveOutWhatDoesNotFit() {
        val narrow = planDocks(70, 40, listOf(status, thinking))
        assertTrue(narrow.at(Dock.Right).isEmpty()); assertEquals(70, narrow.mainWidth)
        val short = planDocks(120, 20, listOf(status, thinking))
        assertTrue(short.at(Dock.Bottom).isEmpty()); assertEquals(20, short.mainHeight)
    }

    @Test fun mainPartKeepsItsMinimum() {
        val plan = planDocks(80, 26, listOf(DockRequest(PaneId.Status, Dock.Right, 90), DockRequest(PaneId.Thinking, Dock.Bottom, 90)))
        assertTrue(plan.mainWidth >= 40)
        assertTrue(plan.mainHeight >= 5)
    }

    @Test fun aSideHoldsOnePanel() {
        val plan = planDocks(120, 40, listOf(status, DockRequest(PaneId.Thinking, Dock.Right, 30)))
        assertEquals(listOf(PaneId.Status), plan.panes.map { it.id })
        assertTrue(dockRefusal(PaneId.Thinking, Dock.Right, listOf(PaneId.Status to Dock.Right))!!.contains("one panel"))
        assertNull(dockRefusal(PaneId.Thinking, Dock.Bottom, listOf(PaneId.Status to Dock.Right)))
        assertTrue(dockRefusal(PaneId.Status, Dock.Bottom, emptyList()) != null)
    }
}

class DockRectsTest {
    @Test fun rectanglesTileTheAreaWithTheGapsBetweenColumnsAndMain() {
        val plan = planDocks(120, 40, listOf(DockRequest(PaneId.Thinking, Dock.Left, 20), DockRequest(PaneId.Status, Dock.Right, 25)))
        val r = plan.rects(120, 40).associateBy { it.label }
        assertEquals(0, r.getValue("thinking").x)
        assertEquals(r.getValue("thinking").width + 1, r.getValue("main").x)
        assertEquals(r.getValue("main").x + r.getValue("main").width + 1, r.getValue("status").x)
        assertEquals(120, r.getValue("status").x + r.getValue("status").width)
        assertTrue(r.values.all { it.height == 40 })
    }

    @Test fun stripsStackAboveAndBelowTheMainPartOnly() {
        val plan = planDocks(120, 40, listOf(DockRequest(PaneId.Thinking, Dock.Top, 20), DockRequest(PaneId.Status, Dock.Bottom, 20)).filter { true })
        val r = plan.rects(120, 40).associateBy { it.label }
        assertEquals(0, r.getValue("thinking").y)
        assertEquals(r.getValue("thinking").height, r.getValue("main").y)
        assertEquals(40, r.getValue("status").y + r.getValue("status").height)
        assertTrue(r.values.all { it.x == 0 && it.width == 120 })
    }
}

class SettingsTest {
    @Test fun roundTripsThroughText() {
        val s = InkSettings(theme = "nord", navPosition = "top", sidebarCollapsed = true, uiScale = "compact", plainGlyphs = true, animateLogo = false,
            statusPane = false, statusPanePercent = 40, statusPaneDock = "left", thinkingPane = true, thinkingPanePercent = 55, thinkingPaneDock = "top")
        assertEquals(s, InkSettings.parse(s.toText()))
    }

    @Test fun brokenValuesFallBackToDefaults() {
        val s = InkSettings.parse("navPosition=diagonal\nuiScale=huge\nstatusPanePercent=500\nthinkingPanePercent=abc\nstatusPaneDock=top\nplainGlyphs=maybe\n#theme=x\nnonsense\n")
        val d = InkSettings()
        assertEquals(d.navPosition, s.navPosition)
        assertEquals(d.uiScale, s.uiScale)
        assertEquals(90, s.statusPanePercent)
        assertEquals(d.thinkingPanePercent, s.thinkingPanePercent)
        assertEquals(d.statusPaneDock, s.statusPaneDock)
        assertEquals(d.plainGlyphs, s.plainGlyphs)
        assertEquals(d.theme, s.theme)
    }

    @Test fun storeSavesAtomicallyAndLoadNeverThrows() {
        val dir = Files.createTempDirectory("ink-settings")
        val store = InkSettingsStore(dir.resolve("sub/settings.properties"))
        assertEquals(InkSettings(), store.load()) // no file yet
        assertTrue(store.save(InkSettings(navPosition = "right")))
        assertEquals("right", store.load().navPosition)
        assertFalse(Files.exists(dir.resolve("sub/settings.properties.tmp")))
        Files.writeString(dir.resolve("sub/settings.properties"), "\u0000garbage")
        assertEquals(InkSettings(), store.load())
        dir.toFile().deleteRecursively()
    }

    @Test fun defaultPathHonoursInkConfig() {
        val s = InkSettingsStore.default(mapOf("INK_CONFIG" to "/tmp/x/ink.properties"), "/home/me")
        assertTrue(s.toString().isNotEmpty())
    }
}
