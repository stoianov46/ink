package com.idea.ink.example

import com.idea.ink.ui.AssistantEntry
import com.idea.ink.ui.ConsoleEntry
import com.idea.ink.ui.ThinkingEntry
import com.idea.ink.ui.ToolEntry
import com.idea.ink.ui.ToolStatus
import com.idea.ink.ui.UserEntry

/*
 * Mock data for the Console screen: a scripted conversation with a model, the way Arcana runs one (the model thinks, calls tools, one of
 * them asks for approval, then the answer streams in). There is no model behind it: [playSession] is a pure function of a tick counter, so
 * the same tick always gives the same screen, and it can be tested. One tick is [TICK_MS] milliseconds at 1× speed.
 */

const val TICK_MS = 50L
const val MODEL_NAME = "ink-mock-7b"
const val CONTEXT_WINDOW = 8192
const val AUTO_COMPACT_AT = 0.80
const val DEFAULT_PROMPT = "Why did our weekly signups drop, and what should we change?"

/** One step of the script. */
sealed interface Step
/** The model reasons: [text] appears [rate] characters per tick (shown live in the thinking pane). */
class Think(val text: String, val rate: Int = 9) : Step
/** The model calls a tool: it may wait [approvalTicks] for approval, then runs for [runTicks] and returns [result]. */
class Tool(val name: String, val args: String, val result: String, val runTicks: Int, val approvalTicks: Int = 0) : Step
/** The model's answer (Markdown) streams in [rate] characters per tick. */
class Write(val text: String, val rate: Int = 14) : Step

private val ANSWER = """
## Signups fell 18% in week 46

The drop starts the day **Signup form v2** shipped (Nov 11), not with traffic: visits stayed flat at about 12.4k.

| Week | Visits | Signups | Conv. |
|------|-------:|--------:|------:|
| W43  | 12,310 | 1,085   | 8.8%  |
| W44  | 12,480 | 1,102   | 8.8%  |
| W45  | 12,390 | 1,094   | 8.8%  |
| W46  | 12,410 | 897     | 7.2%  |

### What changed

- The new form asks for a **phone number** before the email step (`SignupForm.kt`)
- Validation rejects a leading `+`, so international numbers fail silently
- 3 notes mention the experiment, but none records a rollback plan

> ship to 100% after 3 days if no regressions

### Suggested fix

```diff
--- a/web/signup/SignupForm.kt
+++ b/web/signup/SignupForm.kt
@@ -41,7 +41,7 @@ fun validate(form: Form): Result {
-    if (!form.phone.matches(Regex("^[0-9]{10}$"))) return Invalid("phone")
+    if (form.phone.isNotBlank() && !form.phone.matches(Regex("^\\+?[0-9]{7,15}$"))) return Invalid("phone")
     return Valid
 }
```

1. Make the phone field **optional** and move it after the email step
2. Accept `+` and 7-15 digits
3. Run the experiment again for 3 days; expect about **+1.6 pts** of conversion back
""".trimIndent()

/** The script of the session. */
fun sessionScript(): List<Step> = listOf(
    Think(
        "The user asks why weekly signups dropped. I should look at the weekly numbers first and see whether visits or conversion moved, " +
            "then check the notes for anything that changed around that time.",
    ),
    Tool(
        "query_metrics", "weekly_signups --weeks 6",
        "week  visits  signups  conv\nW41   12,150  1,071    8.8%\nW42   12,260  1,079    8.8%\nW43   12,310  1,085    8.8%\nW44   12,480  1,102    8.8%\nW45   12,390  1,094    8.8%\nW46   12,410  897      7.2%",
        runTicks = 24,
    ),
    Think("Visits are flat, conversion fell from 8.8% to 7.2% in W46. So something changed in the funnel. Search the vault for an experiment.", rate = 10),
    Tool(
        "search_notes", "\"signup form experiment\"",
        "3 notes\nExperiments/Signup form v2.md  0.91\nRetro/Week 45 growth review.md  0.78\nIdeas/Phone-first onboarding.md  0.64",
        runTicks = 18,
    ),
    Tool(
        "read_note", "Experiments/Signup form v2.md",
        "# Signup form v2\nShipped Nov 11 to 100% of traffic.\nChange: phone number is asked before the email step.\nPlan: ship to 100% after 3 days if no regressions.",
        runTicks = 14,
    ),
    Think("The phone step went live on Nov 11, which matches the drop. Check the code history to see how the field is validated.", rate = 10),
    Tool(
        "run_shell", "git log --since=2.weeks -- web/signup",
        "a41f9c2 Signup form v2: phone step first (Nov 11)\n7be03d1 Tidy signup copy (Nov 4)",
        runTicks = 14, approvalTicks = 24,
    ),
    Write(ANSWER),
)

/** What the screen shows at one moment of the session. */
class SessionView(
    val entries: List<ConsoleEntry>,
    /** The reasoning so far (live while thinking, the last thoughts otherwise): what the thinking pane shows. */
    val thinking: String,
    val thinkingLive: Boolean,
    /** `thinking`, `calling query_metrics`, `waiting for approval`, `writing`, `done`. */
    val phase: String,
    val contextTokens: Int,
    val outputTokens: Int,
    val tokensPerSecond: Int,
    val elapsedMs: Long,
    val done: Boolean,
) {
    val contextShare: Double get() = contextTokens.toDouble() / CONTEXT_WINDOW
}

private fun tokens(chars: Int) = (chars + 3) / 4

/** The session at [tick] (0 = just sent). Steps that are over are shown whole, the one in progress partly, later ones not at all. */
fun playSession(prompt: String, tick: Int, script: List<Step> = sessionScript()): SessionView {
    val entries = ArrayList<ConsoleEntry>()
    entries += UserEntry(prompt)
    var left = tick.coerceAtLeast(0)
    var context = 1180 + tokens(prompt.length) // the system prompt and the question
    var output = 0
    var thinking = ""
    var live = false
    var phase = "done"
    var rate = 0
    var finished = true

    for (step in script) {
        when (step) {
            is Think -> {
                val ticks = (step.text.length + step.rate - 1) / step.rate
                val shown = (left * step.rate).coerceAtMost(step.text.length)
                if (left >= ticks) {
                    entries += ThinkingEntry(step.text, live = false, seconds = ticks * TICK_MS / 1000.0)
                    thinking = step.text
                    context += tokens(step.text.length); output += tokens(step.text.length)
                    left -= ticks
                } else {
                    entries += ThinkingEntry(step.text.take(shown), live = true)
                    thinking = step.text.take(shown)
                    live = true; phase = "thinking"; rate = step.rate * 1000 / TICK_MS.toInt() / 4
                    context += tokens(shown); output += tokens(shown)
                    finished = false
                }
            }
            is Tool -> {
                val total = step.approvalTicks + step.runTicks
                when {
                    left >= total -> {
                        entries += ToolEntry(step.name, step.args, ToolStatus.Done, step.result, step.runTicks * TICK_MS / 1000.0)
                        context += tokens(step.result.length + step.args.length)
                        left -= total
                    }
                    left < step.approvalTicks -> {
                        entries += ToolEntry(step.name, step.args, ToolStatus.WaitingApproval)
                        phase = "waiting for approval"; finished = false
                    }
                    else -> {
                        entries += ToolEntry(step.name, step.args, ToolStatus.Running)
                        phase = "calling ${step.name}"; finished = false
                    }
                }
            }
            is Write -> {
                val ticks = (step.text.length + step.rate - 1) / step.rate
                val shown = (left * step.rate).coerceAtMost(step.text.length)
                val complete = left >= ticks
                entries += AssistantEntry(step.text.take(shown), MODEL_NAME, streaming = !complete)
                context += tokens(shown); output += tokens(shown)
                if (complete) left -= ticks else { phase = "writing"; rate = step.rate * 1000 / TICK_MS.toInt() / 4; finished = false }
            }
        }
        if (!finished) break
    }
    return SessionView(entries, thinking, live, if (finished) "done" else phase, context, output, if (finished) 0 else rate, tick * TICK_MS, finished)
}

/** The tick at which [script] is over: how long the session lasts. */
fun sessionLength(script: List<Step> = sessionScript()): Int = script.sumOf { step ->
    when (step) {
        is Think -> (step.text.length + step.rate - 1) / step.rate
        is Tool -> step.approvalTicks + step.runTicks
        is Write -> (step.text.length + step.rate - 1) / step.rate
    }
}
