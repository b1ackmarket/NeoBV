package dev.aaa1115910.bv.viewmodel.search

import kotlin.test.Test
import kotlin.test.assertEquals

class SearchResultAccumulatorTest {
    @Test
    fun `accumulator drops duplicate pgc season ids and repeated page cursors`() {
        val accumulator = SearchResultAccumulator<FakePgc, Int>(
            itemKey = { it.seasonId },
            initialCursor = 1
        )

        val first = listOf(FakePgc(seasonId = 1), FakePgc(seasonId = 2))
        val duplicatePage = listOf(FakePgc(seasonId = 1), FakePgc(seasonId = 2))

        accumulator.append(cursor = 2, items = first)
        accumulator.append(cursor = 2, items = duplicatePage)

        assertEquals(2, accumulator.items.size)
    }

    private data class FakePgc(val seasonId: Long)
}
