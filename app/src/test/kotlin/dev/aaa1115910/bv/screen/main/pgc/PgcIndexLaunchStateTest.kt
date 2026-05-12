package dev.aaa1115910.bv.screen.main.pgc

import dev.aaa1115910.biliapi.entity.pgc.PgcType
import dev.aaa1115910.biliapi.entity.pgc.index.IndexOrder
import kotlin.test.Test
import kotlin.test.assertEquals

class PgcIndexLaunchStateTest {
    @Test
    fun `launch state uses requested pgc type before first effect runs`() {
        val state = resolvePgcIndexLaunchState(PgcType.Movie)

        assertEquals(PgcType.Movie, state.pgcType)
        assertEquals(IndexOrder.PlayCount, state.indexOrder)
    }
}
