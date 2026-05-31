package dev.aaa1115910.bv.viewmodel.home

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PopularRankCategoryTest {
    @Test
    fun `all category keeps popular feed endpoint`() {
        assertEquals(PopularFeedSource.Popular, PopularRankCategory.All.feedSource)
        assertNull(PopularRankCategory.All.slug)
        assertNull(PopularRankCategory.All.rid)
    }

    @Test
    fun `douga category maps to rank endpoint identifiers`() {
        assertEquals(PopularFeedSource.Rank, PopularRankCategory.Douga.feedSource)
        assertEquals("douga", PopularRankCategory.Douga.slug)
        assertEquals(1, PopularRankCategory.Douga.rid)
    }
}
