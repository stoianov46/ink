package com.idea.ink.settings

import com.idea.ink.layout.Dock
import com.idea.ink.layout.PANE_MAX
import com.idea.ink.layout.PANE_MIN
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/**
 * The appearance settings every Ink app has, as Arcana keeps them (Settings → Appearance and Console): the theme, where the navigation sits
 * (`left`, `top` or `right`), the sidebar fold, density, plain glyphs, the animated logo and the two docked panes. The default layout is Arcana's:
 * the thinking pane in a column on the left, the conversation in the middle, the status pane in a column on the right. Plain data: unknown or broken
 * values read from a file fall back to the defaults, so a hand-edited file can never break the app.
 */
data class InkSettings(
    val theme: String = "dark",
    /** `left`, `top` or `right`. */
    val navPosition: String = "left",
    val sidebarCollapsed: Boolean = false,
    /** `compact`, `normal` or `spacious`. */
    val uiScale: String = "normal",
    val plainGlyphs: Boolean = false,
    val animateLogo: Boolean = true,
    val statusPane: Boolean = true,
    val statusPanePercent: Int = 25,
    /** `left` or `right`. */
    val statusPaneDock: String = "right",
    val thinkingPane: Boolean = true,
    val thinkingPanePercent: Int = 20,
    /** `left`, `right`, `top` or `bottom`. */
    val thinkingPaneDock: String = "left",
) {
    /** `key=value` lines, in a stable order. */
    fun toText(): String = buildString {
        append("# Ink settings\n")
        for ((k, v) in entries()) append(k).append('=').append(v).append('\n')
    }

    private fun entries(): List<Pair<String, Any>> = listOf(
        "theme" to theme, "navPosition" to navPosition, "sidebarCollapsed" to sidebarCollapsed, "uiScale" to uiScale,
        "plainGlyphs" to plainGlyphs, "animateLogo" to animateLogo,
        "statusPane" to statusPane, "statusPanePercent" to statusPanePercent, "statusPaneDock" to statusPaneDock,
        "thinkingPane" to thinkingPane, "thinkingPanePercent" to thinkingPanePercent, "thinkingPaneDock" to thinkingPaneDock,
    )

    companion object {
        val NAV_POSITIONS = listOf("left", "top", "right")
        val UI_SCALES = listOf("compact", "normal", "spacious")

        /** Reads [text] (`key=value` lines; `#` starts a comment); anything missing, unknown or out of range takes its default. */
        fun parse(text: String): InkSettings {
            val d = InkSettings()
            val m = text.lineSequence().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") && '=' in it }
                .associate { it.substringBefore('=').trim() to it.substringAfter('=').trim() }
            fun bool(k: String, default: Boolean) = when (m[k]?.lowercase()) { "true" -> true; "false" -> false; else -> default }
            fun pct(k: String, default: Int) = (m[k]?.toIntOrNull() ?: default).coerceIn(PANE_MIN, PANE_MAX)
            fun pick(k: String, allowed: List<String>, default: String) = m[k]?.takeIf { it in allowed } ?: default
            return InkSettings(
                theme = m["theme"]?.takeIf { it.isNotBlank() } ?: d.theme,
                navPosition = pick("navPosition", NAV_POSITIONS, d.navPosition),
                sidebarCollapsed = bool("sidebarCollapsed", d.sidebarCollapsed),
                uiScale = pick("uiScale", UI_SCALES, d.uiScale),
                plainGlyphs = bool("plainGlyphs", d.plainGlyphs),
                animateLogo = bool("animateLogo", d.animateLogo),
                statusPane = bool("statusPane", d.statusPane),
                statusPanePercent = pct("statusPanePercent", d.statusPanePercent),
                statusPaneDock = pick("statusPaneDock", listOf("left", "right"), d.statusPaneDock),
                thinkingPane = bool("thinkingPane", d.thinkingPane),
                thinkingPanePercent = pct("thinkingPanePercent", d.thinkingPanePercent),
                thinkingPaneDock = pick("thinkingPaneDock", Dock.entries.map { it.id }, d.thinkingPaneDock),
            )
        }
    }
}

/** Where settings live: [path] (the app passes `~/.config/<app>/settings.properties`). Loading never throws; saving is atomic (a temporary file, then a move). */
class InkSettingsStore(private val path: Path) {
    fun load(): InkSettings = runCatching { InkSettings.parse(Files.readString(path)) }.getOrDefault(InkSettings())

    /** True if the file was written. A read-only folder is not an error for the app, only for the caller to mention. */
    fun save(settings: InkSettings): Boolean = runCatching {
        path.parent?.let { Files.createDirectories(it) }
        val tmp = path.resolveSibling(path.fileName.toString() + ".tmp")
        Files.writeString(tmp, settings.toText())
        Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING)
        true
    }.getOrDefault(false)

    companion object {
        /** `$INK_CONFIG`, else `~/.config/ink/settings.properties`. */
        fun default(env: Map<String, String> = System.getenv(), home: String = System.getProperty("user.home")): InkSettingsStore =
            InkSettingsStore(Path.of(env["INK_CONFIG"] ?: "$home/.config/ink/settings.properties"))
    }
}
