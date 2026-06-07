package dev.aaa1115910.bv.component.controllers

import dev.aaa1115910.biliapi.entity.video.VideoProgressChapter
import dev.aaa1115910.bv.entity.ProgressSegmentMark
import androidx.compose.ui.unit.dp
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

    @Test
    fun `subtitle bottom padding is lifted while bottom controller is visible`() {
        assertEquals(12.dp, resolveSubtitleBottomPadding(basePadding = 12.dp, liftForBottomController = false))
        assertEquals(108.dp, resolveSubtitleBottomPadding(basePadding = 12.dp, liftForBottomController = true))
    }

    @Test
    fun `chapter markers skip first chapter and out of range starts`() {
        val markers = calculateChapterMarkerFractions(
            duration = 100_000,
            chapters = listOf(
                VideoProgressChapter(startMs = 0, endMs = 20_000, title = "开场"),
                VideoProgressChapter(startMs = 20_000, endMs = 80_000, title = "正片"),
                VideoProgressChapter(startMs = 120_000, endMs = 130_000, title = "越界")
            )
        )

        assertEquals(listOf(0.2f), markers)
    }

    @Test
    fun `chapter track ranges fall back to a continuous range without chapters`() {
        val ranges = calculateChapterTrackRanges(
            duration = 100_000,
            chapters = emptyList()
        )

        assertEquals(listOf(ProgressTrackRange(0f, 1f)), ranges)
    }

    @Test
    fun `chapter track ranges clip chapters to playable duration`() {
        val ranges = calculateChapterTrackRanges(
            duration = 100_000,
            chapters = listOf(
                VideoProgressChapter(startMs = -2_000, endMs = 30_000, title = "开场"),
                VideoProgressChapter(startMs = 30_000, endMs = 120_000, title = "正片"),
                VideoProgressChapter(startMs = 60_000, endMs = 50_000, title = "无效")
            )
        )

        assertEquals(
            listOf(
                ProgressTrackRange(0f, 0.3f),
                ProgressTrackRange(0.3f, 1f)
            ),
            ranges
        )
    }

    @Test
    fun `segment marks are intersected with chapter ranges`() {
        val intersections = calculateSegmentIntersections(
            trackRanges = listOf(
                ProgressTrackRange(0f, 0.3f),
                ProgressTrackRange(0.3f, 0.7f),
                ProgressTrackRange(0.7f, 1f)
            ),
            segmentRange = ProgressSegmentRange(
                startFraction = 0.2f,
                endFraction = 0.8f,
                colorArgb = 0xFFFF0000
            )
        )

        assertEquals(
            listOf(
                ProgressSegmentRange(0.2f, 0.3f, 0xFFFF0000),
                ProgressSegmentRange(0.3f, 0.7f, 0xFFFF0000),
                ProgressSegmentRange(0.7f, 0.8f, 0xFFFF0000)
            ),
            intersections
        )
    }
}
