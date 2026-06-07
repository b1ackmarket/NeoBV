package dev.aaa1115910.biliapi.repositories

import dev.aaa1115910.biliapi.http.entity.video.MusicTopListItem
import dev.aaa1115910.biliapi.http.entity.video.MusicTopListPeriodItem
import kotlin.test.Test
import kotlin.test.assertEquals

class HomeRankingMappingTest {
    @Test
    fun `music ranking fallback item keeps only uploader name`() {
        val item = MusicTopListItem(
            creationNickname = "洲洲巴士音乐榜#1·热度 2723170"
        )

        assertEquals("洲洲巴士", item.creationUpName())
    }

    @Test
    fun `music ranking uploader cleanup handles heat suffix without separator`() {
        assertEquals(
            "洲洲巴士",
            cleanMusicRankingUpName("洲洲巴士 音乐榜#1 热度 1616252")
        )
        assertEquals(
            "普通UP",
            cleanMusicRankingUpName("普通UP 热度 1616252")
        )
    }

    @Test
    fun `music ranking period keeps publish time for cards`() {
        val period = MusicTopListPeriodItem(id = 40101, period = 1, publishTime = 1_765_555_200)

        assertEquals(1_765_555_200, period.toHomeRankingPeriod().publishTime)
    }
}
