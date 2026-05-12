package dev.aaa1115910.bv.screen.search

import dev.aaa1115910.biliapi.repositories.SearchFilterDuration
import dev.aaa1115910.biliapi.repositories.SearchFilterOrderType
import dev.aaa1115910.biliapi.repositories.SearchType
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
                type = SearchType.Video,
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
            type = SearchType.Video,
            order = SearchFilterOrderType.ComprehensiveSort,
            duration = SearchFilterDuration.All,
            partitionTid = null,
            childPartitionTid = null
        )
        val afterIntent = beforeIntent.copy(keyword = "测试")

        assertTrue(afterIntent.isReady)
        assertNotEquals(beforeIntent, afterIntent)
    }

    @Test
    fun `search update trigger changes when selected result type changes`() {
        val videoTrigger = SearchResultUpdateTrigger(
            keyword = "测试",
            type = SearchType.Video,
            order = SearchFilterOrderType.ComprehensiveSort,
            duration = SearchFilterDuration.All,
            partitionTid = null,
            childPartitionTid = null
        )

        assertNotEquals(SearchType.MediaFt, videoTrigger.type)
        assertNotEquals(videoTrigger, videoTrigger.copy(type = SearchType.MediaFt))
    }

    @Test
    fun `search update request skips repeated trigger for same result type`() {
        val videoTrigger = SearchResultUpdateTrigger(
            keyword = "测试",
            type = SearchType.Video,
            order = SearchFilterOrderType.ComprehensiveSort,
            duration = SearchFilterDuration.All,
            partitionTid = null,
            childPartitionTid = null
        )
        val requestedTriggers = mapOf(SearchType.Video to videoTrigger)

        assertFalse(shouldRequestSearchResult(requestedTriggers, videoTrigger))
    }

    @Test
    fun `search update request allows same query for a result type that was not requested yet`() {
        val videoTrigger = SearchResultUpdateTrigger(
            keyword = "测试",
            type = SearchType.Video,
            order = SearchFilterOrderType.ComprehensiveSort,
            duration = SearchFilterDuration.All,
            partitionTid = null,
            childPartitionTid = null
        )
        val mediaFtTrigger = videoTrigger.copy(type = SearchType.MediaFt)
        val requestedTriggers = mapOf(SearchType.Video to videoTrigger)

        assertTrue(shouldRequestSearchResult(requestedTriggers, mediaFtTrigger))
    }
}
