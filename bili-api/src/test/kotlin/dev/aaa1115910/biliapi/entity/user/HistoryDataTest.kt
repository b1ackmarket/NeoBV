package dev.aaa1115910.biliapi.entity.user

import dev.aaa1115910.biliapi.http.entity.history.HistoryData as HttpHistoryData
import dev.aaa1115910.biliapi.http.entity.history.HistoryItem as HttpHistoryItem
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class HistoryDataTest {
    @Test
    fun `web live histories are mapped when live business is selected`() {
        val data = HistoryData.fromHistoryResponse(
            data = historyResponse(
                items = listOf(
                    historyItem(business = "archive", oid = 11L),
                    historyItem(business = "live", oid = 22739471L, title = "直播间", tagName = "单机游戏")
                )
            ),
            business = HistoryBusiness.Live
        )

        assertEquals(1, data.data.size)
        assertEquals(HistoryItemType.Live, data.data.first().type)
        assertEquals(22739471L, data.data.first().oid)
        assertEquals("单机游戏", data.data.first().tagName)
    }

    @Test
    fun `web video histories exclude live records`() {
        val data = HistoryData.fromHistoryResponse(
            data = historyResponse(
                items = listOf(
                    historyItem(business = "archive", oid = 11L),
                    historyItem(business = "pgc", oid = 22L),
                    historyItem(business = "live", oid = 22739471L)
                )
            ),
            business = HistoryBusiness.Video
        )

        assertEquals(listOf(HistoryItemType.Archive, HistoryItemType.Pgc), data.data.map { it.type })
    }

    private fun historyResponse(items: List<HttpHistoryItem>) = HttpHistoryData(
        cursor = HttpHistoryData.Cursor(max = 0L, viewAt = 0L, business = "", ps = 20),
        tab = emptyList(),
        list = items
    )

    private fun historyItem(
        business: String,
        oid: Long,
        title: String = "title",
        tagName: String = "",
        liveStatus: Int = 1
    ) = HttpHistoryItem(
        title = title,
        longTitle = "",
        cover = "cover",
        covers = null,
        uri = "https://live.bilibili.com/$oid",
        history = HttpHistoryItem.HistoryInfo(
            oid = oid,
            epid = 0,
            bvid = "",
            page = 0,
            cid = 0L,
            part = "",
            business = business,
            dt = 2
        ),
        videos = 0,
        authorName = "author",
        authorFace = "",
        authorMid = 1L,
        viewAt = 0,
        progress = 0,
        badge = "",
        showTitle = "",
        duration = 0,
        current = "",
        total = 0,
        newDesc = "",
        isFinish = 0,
        isFav = 0,
        kid = 0L,
        tagName = tagName,
        liveStatus = liveStatus
    )
}
