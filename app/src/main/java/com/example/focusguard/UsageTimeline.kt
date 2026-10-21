package com.example.focusguard

/** Foreground intervals clipped to a reporting window, including an app opened before midnight. */
class UsageTimeline(private val start: Long) {
    private val totals = mutableMapOf<String, Long>()
    private var active: String? = null
    private var since = start
    fun resume(pkg: String, at: Long) {
        stop(at)
        active = pkg
        since = at
    }
    fun pause(pkg: String, at: Long) { if (active == pkg) stop(at) }
    fun stop(at: Long) {
        active?.let { pkg ->
            totals[pkg] = (totals[pkg] ?: 0L) + (at - maxOf(start, since)).coerceAtLeast(0L)
        }
        active = null
    }
    fun finish(end: Long): Map<String, Long> { stop(end); return totals.toMap() }
}
