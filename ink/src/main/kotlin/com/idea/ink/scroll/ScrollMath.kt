package com.idea.ink.scroll

/**
 * The rows the scrollbar thumb covers in a track [height] rows tall, or null when everything fits. The thumb is at least one row, its size is the
 * shown fraction of the content, and its position tracks [scroll] (the index of the first visible line out of [total], with [visible] on screen).
 */
fun scrollThumb(height: Int, scroll: Int, total: Int, visible: Int): IntRange? {
    if (height <= 0 || total <= visible || total <= 0) return null
    val thumb = (height.toLong() * visible / total).toInt().coerceIn(1, height)
    val track = height - thumb
    val maxScroll = (total - visible).coerceAtLeast(0)
    val pos = scroll.coerceIn(0, maxScroll)
    val top = if (maxScroll == 0) 0 else (track.toLong() * pos / maxScroll).toInt().coerceIn(0, track)
    return top until (top + thumb)
}
