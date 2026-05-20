package dev.aaa1115910.bv.screen.search

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SearchInputDirectVideoTest {
    @Test
    fun `extracts bv id from plain input and links`() {
        assertNotNull(resolveDirectVideoAid("BV1xx411c7mD"))
        assertNotNull(resolveDirectVideoAid("https://www.bilibili.com/video/BV1xx411c7mD/?spm_id_from=333"))
        assertNotNull(resolveDirectVideoAid("https://www.bilibili.com/video/BV1cxLz6XEKT/"))
        assertNotNull(resolveDirectVideoAid("https://www.bilibili.com/video/BV13e5KzsExq/?spm_id_from=333.788.recommend_more_video.11"))
    }

    @Test
    fun `extracts av id from plain input and links`() {
        assertEquals(123456L, resolveDirectVideoAid("av123456"))
        assertEquals(123456L, resolveDirectVideoAid("https://www.bilibili.com/video/av123456/"))
        assertEquals(
            115705261327589L,
            resolveDirectVideoAid("【今天吃柯基-哔哩哔哩】 https://www.bilibili.com/video/av115705261327589")
        )
    }
}
