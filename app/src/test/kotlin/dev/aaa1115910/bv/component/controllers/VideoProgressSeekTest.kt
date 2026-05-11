package dev.aaa1115910.bv.component.controllers

import dev.aaa1115910.bv.entity.ProgressSegmentMark
import kotlin.test.Test
import kotlin.test.assertEquals

class VideoProgressSeekTest {
    @Test
    fun `progress segment ranges clip marks to playable duration`() {
        val ranges = calculateProgressSegmentRanges(
            duration = 10_000,
            segments = listOf(
                ProgressSegmentMark(startMs = -1_000, endMs = 2_000, colorArgb = 0xFF43D676),
                ProgressSegmentMark(startMs = 8_000, endMs = 12_000, colorArgb = 0xFF22D6FF),
                ProgressSegmentMark(startMs = 5_000, endMs = 5_000, colorArgb = 0xFFFF0000)
            )
        )

        assertEquals(2, ranges.size)
        assertEquals(0f, ranges[0].startFraction)
        assertEquals(0.2f, ranges[0].endFraction)
        assertEquals(0.8f, ranges[1].startFraction)
        assertEquals(1f, ranges[1].endFraction)
    }
}
