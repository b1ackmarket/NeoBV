package dev.aaa1115910.bv.viewmodel.home

import dev.aaa1115910.bv.component.HomeTopNavItem
import dev.aaa1115910.biliapi.repositories.HomeRankingPeriod
import dev.aaa1115910.bv.screen.main.home.displayLabel
import dev.aaa1115910.bv.screen.main.home.popupDisplayName
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

    @Test
    fun `ranking period label only keeps issue number`() {
        assertEquals("第123期", HomeRankingPeriod(123, "2026年6月2日第123期").displayLabel())
        assertEquals("最新一期", HomeRankingPeriod(0, "最新一期").displayLabel())
    }

    @Test
    fun `ranking inline period label keeps original issue text`() {
        val period = HomeRankingPeriod(123, "2026第123期 05.22 - 05.28")

        assertEquals("2026第123期 05.22 - 05.28", period.label)
    }

    @Test
    fun `ranking type popup uses short music names`() {
        assertEquals("每周必刷", HomeRankingType.Weekly.popupDisplayName)
        assertEquals("热歌榜", HomeRankingType.MusicHot.popupDisplayName)
        assertEquals("二创榜", HomeRankingType.MusicOriginal.popupDisplayName)
    }
}
