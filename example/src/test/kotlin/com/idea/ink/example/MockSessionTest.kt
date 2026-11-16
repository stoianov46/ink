package com.idea.ink.example

import com.idea.ink.ui.AssistantEntry
import com.idea.ink.ui.ThinkingEntry
import com.idea.ink.ui.ToolEntry
import com.idea.ink.ui.ToolStatus
import com.idea.ink.ui.UserEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MockSessionTest {
    private val end = sessionLength()

    @Test fun tickZeroShowsOnlyYourMessage() {
        val v = playSession("hello", 0)
        assertTrue(v.entries.first() is UserEntry)
        assertEquals("hello", (v.entries.first() as UserEntry).text)
        assertFalse(v.done)
        assertEquals("thinking", v.phase)
    }

    @Test fun theSessionIsAPureFunctionOfTheTick() {
        val a = playSession("p", 120)
        val b = playSession("p", 120)
        assertEquals(a.entries.size, b.entries.size)
        assertEquals(a.contextTokens, b.contextTokens)
        assertEquals(a.phase, b.phase)
    }

    @Test fun contextOnlyGrowsAndStaysInsideTheWindow() {
        var last = 0
        for (t in 0..end + 5) {
            val v = playSession(DEFAULT_PROMPT, t)
            assertTrue(v.contextTokens >= last, "context shrank at tick $t")
            assertTrue(v.contextTokens < CONTEXT_WINDOW, "context overflows at tick $t")
            last = v.contextTokens
        }
    }

    @Test fun entriesOnlyEverAppendAndFinishInOrder() {
        var seen = 0
        for (t in 0..end) {
            val n = playSession(DEFAULT_PROMPT, t).entries.size
            assertTrue(n >= seen, "an entry disappeared at tick $t")
            seen = n
        }
    }

    @Test fun aToolWaitsForApprovalThenRunsThenFinishes() {
        val statuses = (0..end).mapNotNull { t -> playSession(DEFAULT_PROMPT, t).entries.filterIsInstance<ToolEntry>().firstOrNull { it.name == "run_shell" }?.status }.distinct()
        assertEquals(listOf(ToolStatus.WaitingApproval, ToolStatus.Running, ToolStatus.Done), statuses)
    }

    @Test fun phasesFollowTheScript() {
        val phases = (0..end).map { playSession(DEFAULT_PROMPT, it).phase }.distinct()
        assertEquals("thinking", phases.first())
        assertTrue("waiting for approval" in phases)
        assertTrue(phases.any { it.startsWith("calling ") })
        assertTrue("writing" in phases)
        assertEquals("done", phases.last())
    }

    @Test fun theFinishedSessionHasTheWholeAnswerAndAllThoughts() {
        val v = playSession(DEFAULT_PROMPT, end)
        assertTrue(v.done)
        assertEquals(0, v.tokensPerSecond)
        val answer = v.entries.last() as AssistantEntry
        assertFalse(answer.streaming)
        assertTrue(answer.text.contains("```diff") && answer.text.contains("| W46"))
        assertEquals(3, v.entries.count { it is ThinkingEntry && !it.live })
        assertEquals(4, v.entries.count { it is ToolEntry })
        assertEquals(v.entries.size, playSession(DEFAULT_PROMPT, end + 1000).entries.size) // past the end nothing changes
    }

    @Test fun theAnswerStreamsInPieces() {
        val partway = playSession(DEFAULT_PROMPT, end - 20).entries.last() as AssistantEntry
        assertTrue(partway.streaming)
        assertTrue(partway.text.length < (playSession(DEFAULT_PROMPT, end).entries.last() as AssistantEntry).text.length)
        assertTrue(playSession(DEFAULT_PROMPT, end - 20).tokensPerSecond > 0)
    }
}
