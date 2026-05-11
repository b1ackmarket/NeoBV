package dev.aaa1115910.bv.viewmodel.player

import kotlin.test.Test
import kotlin.test.assertEquals

class PlaybackFallbackPlannerTest {
    @Test
    fun `planner retries same quality with another codec before dropping resolution`() {
        val planner = PlaybackFallbackPlanner(
            candidates = listOf(
                StreamCandidate(127, "av01", "video://8k-av1", "audio://main"),
                StreamCandidate(127, "hev1", "video://8k-hevc", "audio://main"),
                StreamCandidate(120, "hev1", "video://4k-hevc", "audio://main")
            )
        )

        val next = planner.nextAfterFailure(
            StreamCandidate(127, "av01", "video://8k-av1", "audio://main")
        )

        assertEquals(
            StreamCandidate(127, "hev1", "video://8k-hevc", "audio://main"),
            next
        )
    }
}
