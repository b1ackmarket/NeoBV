package dev.aaa1115910.bv.screen.live

import kotlin.test.Test
import kotlin.test.assertEquals

class LiveDanmakuTimelineTest {
    @Test
    fun `timeline advances while started and holds while paused`() {
        var now = 1_000L
        val timeline = LiveDanmakuTimeline(nowMs = { now })

        timeline.start()
        now = 1_750L

        assertEquals(750L, timeline.currentPositionMs())

        timeline.pause()
        now = 2_500L

        assertEquals(750L, timeline.currentPositionMs())

        timeline.start()
        now = 3_000L

        assertEquals(1_250L, timeline.currentPositionMs())
    }
}
