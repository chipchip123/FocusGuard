package com.example.focusguard

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process

object UsageReader {
    fun permitted(context: Context): Boolean {
        val ops = context.getSystemService(AppOpsManager::class.java)
        return ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName) == AppOpsManager.MODE_ALLOWED
    }

    /** Reconstruct intervals from events: aggregate UsageStats buckets may cross midnight. */
    fun read(context: Context, start: Long = FocusRepository.dayStart(), end: Long = System.currentTimeMillis()): Map<String, Long> {
        if (!permitted(context) || end <= start) return emptyMap()
        val manager = context.getSystemService(UsageStatsManager::class.java)
        val events = manager.queryEvents(start - 86_400_000L, end) ?: return emptyMap()
        val event = UsageEvents.Event()
        val timeline = UsageTimeline(start)
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    // Repeated resume events from the same app should not lose elapsed time.
                    timeline.resume(event.packageName, event.timeStamp)
                }
                UsageEvents.Event.ACTIVITY_PAUSED -> timeline.pause(event.packageName, event.timeStamp)
                UsageEvents.Event.SCREEN_NON_INTERACTIVE, UsageEvents.Event.DEVICE_SHUTDOWN -> timeline.stop(event.timeStamp)
            }
        }
        return timeline.finish(end)
    }
}
