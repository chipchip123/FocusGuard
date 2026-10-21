package com.example.focusguard

import org.junit.Assert.*
import org.junit.Test

class UsageTimelineTest {
    @Test fun `an app opened yesterday only counts time after midnight`() {
        val timeline = UsageTimeline(1000)
        timeline.resume("social", 500)
        timeline.pause("social", 1800)
        assertEquals(800L, timeline.finish(2000)["social"])
    }
    @Test fun `screen off does not accumulate foreground time`() {
        val timeline = UsageTimeline(0)
        timeline.resume("social", 100)
        timeline.stop(300)
        assertEquals(200L, timeline.finish(10_000)["social"])
    }
    @Test fun `activity transitions preserve time and ignore old app pause`() {
        val timeline = UsageTimeline(0)
        timeline.resume("social", 100)
        timeline.resume("social", 300)
        timeline.resume("reader", 500)
        timeline.pause("social", 600)
        val result = timeline.finish(800)
        assertEquals(400L, result["social"])
        assertEquals(300L, result["reader"])
    }
    @Test fun `events before report window contribute no time`() {
        val timeline = UsageTimeline(1000)
        timeline.resume("social", 100)
        timeline.pause("social", 300)
        assertEquals(0L, timeline.finish(2000)["social"])
    }
}
