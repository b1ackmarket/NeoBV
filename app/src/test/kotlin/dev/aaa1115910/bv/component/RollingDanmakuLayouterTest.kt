package dev.aaa1115910.bv.component

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RollingDanmakuLayouterTest {
    @Test
    fun `uses preferred lane only when candidates are equally safe`() {
        val placement = RollingDanmakuLayouter.place(
            laneCount = 4,
            requestedStartAtMs = 1_000L,
            displayWidthPx = 1_000f,
            newWidthPx = 200f,
            newDurationMs = 8_000L,
            minGapPx = 36f,
            preferredLane = 2,
            activeItems = emptyList()
        )

        assertEquals(2, placement.lane)
        assertEquals(1_000L, placement.startAtMs)
    }

    @Test
    fun `chooses an immediately safe lane before the preferred blocked lane`() {
        val placement = RollingDanmakuLayouter.place(
            laneCount = 2,
            requestedStartAtMs = 1_000L,
            displayWidthPx = 1_000f,
            newWidthPx = 200f,
            newDurationMs = 8_000L,
            minGapPx = 36f,
            preferredLane = 0,
            activeItems = listOf(
                RollingDanmakuLayouter.Item(
                    lane = 0,
                    startAtMs = 0L,
                    durationMs = 8_000L,
                    widthPx = 900f
                )
            )
        )

        assertEquals(1, placement.lane)
        assertEquals(1_000L, placement.startAtMs)
    }

    @Test
    fun `delays entry when the only lane would collide`() {
        val placement = RollingDanmakuLayouter.place(
            laneCount = 1,
            requestedStartAtMs = 1_000L,
            displayWidthPx = 1_000f,
            newWidthPx = 200f,
            newDurationMs = 8_000L,
            minGapPx = 36f,
            preferredLane = 0,
            activeItems = listOf(
                RollingDanmakuLayouter.Item(
                    lane = 0,
                    startAtMs = 0L,
                    durationMs = 8_000L,
                    widthPx = 900f
                )
            )
        )

        assertEquals(0, placement.lane)
        assertTrue(placement.startAtMs > 1_000L)
    }

    @Test
    fun `does not delay when the front item is already safely ahead`() {
        val placement = RollingDanmakuLayouter.place(
            laneCount = 1,
            requestedStartAtMs = 3_000L,
            displayWidthPx = 1_000f,
            newWidthPx = 100f,
            newDurationMs = 8_000L,
            minGapPx = 36f,
            preferredLane = 0,
            activeItems = listOf(
                RollingDanmakuLayouter.Item(
                    lane = 0,
                    startAtMs = 0L,
                    durationMs = 4_000L,
                    widthPx = 200f
                )
            )
        )

        assertEquals(0, placement.lane)
        assertEquals(3_000L, placement.startAtMs)
    }
}
