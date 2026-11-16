package com.idea.ink.terminal

/**
 * Mosaic 0.18 installs a native SIGWINCH handler that calls back into the JVM from a thread the JVM doesn't know about.
 * On macOS that kills the process every time the terminal window is resized. Once Mosaic has installed its handler,
 * [install] replaces it with a harmless JVM-managed one; resizes are picked up by [TerminalSize] polling instead.
 *
 * Mosaic installs its handler when the first frame starts, so call [install] a moment after that (Arcana waits 300 ms
 * in a `LaunchedEffect`), not before. It relies on the JDK-internal `sun.misc.Signal`, so it does nothing (and says so
 * through its result) where that is not available, such as on Windows.
 */
object ResizeSignalGuard {
    /** Installs the guard; true if the handler was replaced. Safe to call more than once. */
    @JvmStatic
    fun install(): Boolean = runCatching {
        sun.misc.Signal.handle(sun.misc.Signal("WINCH")) { /* handled by TerminalSize polling */ }
        true
    }.getOrDefault(false)
}
