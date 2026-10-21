package com.example.focusguard

data class ManagedApp(val packageName: String, val name: String, val limitMinutes: Int = 30)

object FocusPolicy {
    const val REWARD_MS = 5 * 60_000L
    fun blocked(enabled: Boolean, usedMs: Long, limitMinutes: Int, rewardUntil: Long, now: Long, demo: Boolean = false): Boolean =
        enabled && (demo || usedMs >= limitMinutes * 60_000L) && now >= rewardUntil

    fun rewardExpiry(now: Long, nextMidnight: Long): Long = minOf(now + REWARD_MS, nextMidnight)
}

/** A high threshold and a separate reset threshold prevent counting one movement repeatedly. */
class ShakeCounter {
    private var armed = true
    private var last = Long.MIN_VALUE
    fun sample(g: Float, elapsedMs: Long): Boolean {
        if (g < 1.08f) armed = true
        if (g > 1.25f && armed && (last == Long.MIN_VALUE || elapsedMs - last >= 250)) {
            armed = false
            last = elapsedMs
            return true
        }
        return false
    }
}
