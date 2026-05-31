package dev.aaa1115910.bv.viewmodel.home

import dev.aaa1115910.bv.component.HomeTopNavItem
import dev.aaa1115910.bv.screen.main.HomeTabAction
import dev.aaa1115910.bv.screen.main.resolveHomeMenuAction
import dev.aaa1115910.bv.screen.main.resolveHomeTopNavOrder
import dev.aaa1115910.bv.screen.main.toClickAction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HomeRankingPolicyTest {
    @Test
    fun `home nav keeps ranking after popular`() {
        val items = resolveHomeTopNavOrder(HomeTopNavItem.Recommend)

        assertEquals(
            listOf(HomeTopNavItem.Recommend, HomeTopNavItem.Popular, HomeTopNavItem.Ranking),
            items.take(3)
        )
    }

    @Test
    fun `ranking menu key does nothing`() {
        assertNull(resolveHomeMenuAction(HomeTopNavItem.Ranking))
    }

    @Test
    fun `ranking tab click refreshes only ranking`() {
        assertEquals(HomeTabAction.RefreshRanking, HomeTopNavItem.Ranking.toClickAction())
    }
}
