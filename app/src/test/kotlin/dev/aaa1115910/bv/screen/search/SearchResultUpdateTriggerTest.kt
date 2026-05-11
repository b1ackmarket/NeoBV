package dev.aaa1115910.bv.screen.search

import dev.aaa1115910.biliapi.repositories.SearchFilterDuration
import dev.aaa1115910.biliapi.repositories.SearchFilterOrderType
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SearchResultUpdateTriggerTest {
    @Test
    fun `search update waits until keyword is available`() {
        assertFalse(
            SearchResultUpdateTrigger(
                keyword = "",
                order = SearchFilterOrderType.ComprehensiveSort,
                duration = SearchFilterDuration.All,
                partitionTid = null,
                childPartitionTid = null
            ).isReady
        )
    }

    @Test
    fun `search update trigger changes when keyword arrives from intent`() {
        val beforeIntent = SearchResultUpdateTrigger(
            keyword = "",
            order = SearchFilterOrderType.ComprehensiveSort,
            duration = SearchFilterDuration.All,
            partitionTid = null,
            childPartitionTid = null
        )
        val afterIntent = beforeIntent.copy(keyword = "测试")

        assertTrue(afterIntent.isReady)
        assertNotEquals(beforeIntent, afterIntent)
    }
}
