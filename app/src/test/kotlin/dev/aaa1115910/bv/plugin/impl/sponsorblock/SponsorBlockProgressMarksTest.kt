package dev.aaa1115910.bv.plugin.impl.sponsorblock

import kotlin.test.Test
import kotlin.test.assertEquals

class SponsorBlockProgressMarksTest {
    @Test
    fun `progress marks use configured visible policies and category colors`() {
        val config = SponsorBlockConfig(
            enabled = true,
            categoryPolicy = mapOf(
                "sponsor" to SkipPolicy.Auto,
                "intro" to SkipPolicy.Prompt,
                "outro" to SkipPolicy.Disabled
            )
        )
        val segments = listOf(
            SponsorSegment(id = "sponsor-1", category = "sponsor", startMs = 1_000, endMs = 5_000),
            SponsorSegment(id = "intro-1", category = "intro", startMs = 8_000, endMs = 10_000),
            SponsorSegment(id = "outro-1", category = "outro", startMs = 20_000, endMs = 25_000)
        )

        val marks = buildSponsorBlockProgressMarks(config, segments)

        assertEquals(2, marks.size)
        assertEquals(0xFF43D676, marks[0].colorArgb)
        assertEquals(0xFF22D6FF, marks[1].colorArgb)
        assertEquals(listOf(1_000L to 5_000L, 8_000L to 10_000L), marks.map { it.startMs to it.endMs })
    }

    @Test
    fun `progress marks are empty when sponsorblock is globally disabled`() {
        val config = SponsorBlockConfig(
            enabled = false,
            categoryPolicy = mapOf("sponsor" to SkipPolicy.Auto)
        )

        val marks = buildSponsorBlockProgressMarks(
            config = config,
            segments = listOf(SponsorSegment(id = "sponsor-1", category = "sponsor", startMs = 1_000, endMs = 5_000))
        )

        assertEquals(emptyList(), marks)
    }
}
