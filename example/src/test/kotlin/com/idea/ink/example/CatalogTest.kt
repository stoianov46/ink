package com.idea.ink.example

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CatalogTest {
    @Test fun everyPieceIsNamedExplainedAndShownWithItsCode() {
        assertTrue(SPECIMENS.size >= 35, "the catalog should cover the whole system")
        assertEquals(SPECIMENS.size, SPECIMENS.map { it.name }.toSet().size, "names are unique")
        SPECIMENS.forEach { s ->
            assertTrue(s.summary.length > 30, "${s.name} says what it is for")
            assertTrue(s.code.isNotBlank(), "${s.name} shows its code")
        }
    }

    @Test fun groupsAreContiguousSoTheListHasOneHeadingPerGroup() {
        val groups = SPECIMENS.map { it.category }
        val runs = groups.zipWithNext().count { (a, b) -> a != b } + 1
        assertEquals(SPECIMEN_GROUPS.size, runs)
    }

    @Test fun theBehavioursAreAllInteractive() {
        val byInteraction = SPECIMENS.mapNotNull { it.interaction }.toSet()
        assertEquals(Interaction.entries.toSet(), byInteraction)
        assertTrue(SPECIMENS.first { it.name == "Text input" }.interaction == Interaction.Text)
    }

    @Test fun everyKindOfProgressAndChartIsThere() {
        val names = SPECIMENS.map { it.name }
        listOf("Bar", "HBar", "Gauge", "Meter", "Slider", "Context bar", "Spinner", "Steps", "Sparkline", "Heat strip", "Bar chart").forEach { assertTrue(it in names, "missing $it") }
    }

    @Test fun theDemoTranscriptHasEveryKindOfEntryForSelectMode() {
        assertEquals(DEMO_ENTRIES.size, DEMO_ENTRY_TEXTS.size)
        assertTrue(DEMO_ENTRY_TEXTS.any { it.contains("```diff") })
    }
}
