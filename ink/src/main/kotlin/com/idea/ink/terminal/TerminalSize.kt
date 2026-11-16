package com.idea.ink.terminal

import java.io.File
import java.util.concurrent.TimeUnit

/** A terminal size in cells. */
data class CellSize(val columns: Int, val rows: Int) {
    override fun toString(): String = "$columns x $rows"
}

/**
 * Asks the OS for the real size of the controlling terminal (`stty size`), independent of whether the terminal emulator
 * answers Mosaic's size queries. `<PREFIX>_COLUMNS` / `<PREFIX>_ROWS` override everything (diagnostics, unusual terminals):
 * the prefix is `INK` unless the app passes its own (`ARCANA`, `COSMO`…).
 */
object TerminalSize {
    /**
     * The current terminal size, or null when there is no controlling terminal or `stty` fails. Takes at most about a second.
     * @param env the environment (defaults to the real one; tests pass their own)
     * @param envPrefix the prefix of the override variables
     */
    fun probe(env: Map<String, String> = System.getenv(), envPrefix: String = "INK"): CellSize? {
        override(env, envPrefix)?.let { return it }
        return runCatching {
            val tty = File("/dev/tty")
            if (!tty.exists()) return@runCatching null
            val process = ProcessBuilder("stty", "size")
                .redirectInput(ProcessBuilder.Redirect.from(tty))
                .redirectErrorStream(true)
                .start()
            // Wait first, then read: reading before waiting would block for as long as stty runs, so the timeout would never apply.
            // `stty size` prints a few bytes, so it cannot fill the pipe and stall while we wait.
            if (!process.waitFor(1, TimeUnit.SECONDS)) {
                process.destroyForcibly()
                return@runCatching null
            }
            parseStty(process.inputStream.bufferedReader().readText())
        }.getOrNull()
    }

    /** The size set by `<prefix>_COLUMNS` and `<prefix>_ROWS` (both must be valid and positive), or null. */
    fun override(env: Map<String, String>, envPrefix: String = "INK"): CellSize? {
        val cols = env["${envPrefix}_COLUMNS"]?.toIntOrNull()
        val rows = env["${envPrefix}_ROWS"]?.toIntOrNull()
        return if (cols != null && rows != null && cols > 0 && rows > 0) CellSize(cols, rows) else null
    }

    /** `stty size` prints "<rows> <columns>". Null if the format is unexpected. */
    fun parseStty(output: String): CellSize? {
        val parts = output.trim().split(Regex("\\s+"))
        if (parts.size != 2) return null
        val rows = parts[0].toIntOrNull() ?: return null
        val cols = parts[1].toIntOrNull() ?: return null
        return if (rows > 0 && cols > 0) CellSize(cols, rows) else null
    }
}
