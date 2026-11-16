package com.idea.ink.ui

import androidx.compose.runtime.Composable
import com.idea.ink.layout.DOCK_GAP
import com.idea.ink.layout.Dock
import com.idea.ink.layout.DockPlan
import com.idea.ink.layout.PaneId
import com.jakewharton.mosaic.layout.size
import com.jakewharton.mosaic.modifier.Modifier
import com.jakewharton.mosaic.ui.Box
import com.jakewharton.mosaic.ui.Column
import com.jakewharton.mosaic.ui.Row

/**
 * Split panes, the way Arcana docks them around the conversation: the main part in the middle, columns on the left and right at full [height], strips above and
 * below the main part. The [plan] (from `planDocks`) says what fits; [pane] draws a pane given its id and its width and height (borders included, so a pane is
 * usually a `Panel(title, Modifier.height(h))`), [main] draws the main part given the room it has. Everything is given exact sizes, because Mosaic cannot clip.
 */
@Composable
fun Docked(plan: DockPlan, height: Int, pane: @Composable (id: PaneId, width: Int, height: Int) -> Unit, main: @Composable (width: Int, height: Int) -> Unit) {
    Row {
        plan.at(Dock.Left).forEach { p ->
            Box(Modifier.size(p.size, height)) { pane(p.id, p.size, height) }
            HGap(DOCK_GAP)
        }
        Column(Modifier.size(plan.mainWidth, height)) {
            plan.at(Dock.Top).forEach { p -> Box(Modifier.size(plan.mainWidth, p.size)) { pane(p.id, plan.mainWidth, p.size) } }
            Box(Modifier.size(plan.mainWidth, plan.mainHeight)) { main(plan.mainWidth, plan.mainHeight) }
            plan.at(Dock.Bottom).forEach { p -> Box(Modifier.size(plan.mainWidth, p.size)) { pane(p.id, plan.mainWidth, p.size) } }
        }
        plan.at(Dock.Right).forEach { p ->
            HGap(DOCK_GAP)
            Box(Modifier.size(p.size, height)) { pane(p.id, p.size, height) }
        }
    }
}
