package dev.aaa1115910.biliapi.repositories

import dev.aaa1115910.biliapi.http.entity.search.SearchResultData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SearchTypeResultTest {
    @Test
    fun `web search result parser tolerates empty result list`() {
        val result = SearchTypeResult.fromSearchTypeResult(
            SearchResultData(
                seid = "",
                page = 1,
                pageSize = 20,
                numResults = 0,
                numPages = 0,
                suggestKeyword = "",
                rqtType = "",
                eggHit = 0,
                showColumn = 0,
                inBlackKey = 0,
                inWhiteKey = 0,
                result = emptyList()
            )
        )

        assertTrue(result.videos.isEmpty())
        assertTrue(result.pgcs.isEmpty())
        assertTrue(result.users.isEmpty())
        assertEquals(SearchTypePage(nextPageForWeb = 2), result.page)
    }
}
