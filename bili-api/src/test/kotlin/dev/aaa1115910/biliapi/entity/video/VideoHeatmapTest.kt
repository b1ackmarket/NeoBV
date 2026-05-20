package dev.aaa1115910.biliapi.entity.video

import dev.aaa1115910.biliapi.http.entity.video.VideoPbp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class VideoHeatmapTest {
    @Test
    fun `convert pbp data to heatmap points`() {
        val heatmap = VideoHeatmap.fromPbp(
            VideoPbp(
                stepSec = 3,
                events = VideoPbp.Events(default = listOf(0.0, 12.0, 4.0))
            )
        )

        assertEquals(3_000L, heatmap?.stepMs)
        assertEquals(
            listOf(
                VideoHeatmapPoint(startMs = 0L, endMs = 3_000L, value = 0.0),
                VideoHeatmapPoint(startMs = 3_000L, endMs = 6_000L, value = 12.0),
                VideoHeatmapPoint(startMs = 6_000L, endMs = 9_000L, value = 4.0)
            ),
            heatmap?.points
        )
    }

    @Test
    fun `ignore empty or invalid pbp data`() {
        assertNull(VideoHeatmap.fromPbp(VideoPbp(stepSec = 0)))
        assertNull(VideoHeatmap.fromPbp(VideoPbp(stepSec = 3)))
    }
}
