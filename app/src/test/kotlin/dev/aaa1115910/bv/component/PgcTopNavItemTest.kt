package dev.aaa1115910.bv.component

import kotlin.test.Test
import kotlin.test.assertEquals

class PgcTopNavItemTest {
    @Test
    fun `cinema is the first pgc tab`() {
        assertEquals(PgcTopNavItem.Cinema, PgcTopNavItem.entries.first())
    }
}
