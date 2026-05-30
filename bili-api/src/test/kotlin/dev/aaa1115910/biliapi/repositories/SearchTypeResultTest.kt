package dev.aaa1115910.biliapi.repositories

import bilibili.pagination.paginationReply
import bilibili.polymer.app.search.v1.item
import bilibili.polymer.app.search.v1.searchByTypeResponse
import bilibili.polymer.app.search.v1.searchLiveInlineCard
import bilibili.polymer.app.search.v1.searchTipsCard
import dev.aaa1115910.biliapi.http.entity.search.SearchResultData
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
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
                result = JsonArray(emptyList())
            )
        )

        assertTrue(result.videos.isEmpty())
        assertTrue(result.pgcs.isEmpty())
        assertTrue(result.users.isEmpty())
        assertEquals(SearchTypePage(nextPageForWeb = 2), result.page)
    }

    @Test
    fun `web live room search results parse from typed result array`() {
        val result = SearchTypeResult.fromSearchTypeResult(
            searchResultData(
                result = Json.parseToJsonElement(
                    """
                    [
                      {
                        "type": "live_room",
                        "roomid": 12345,
                        "title": "直播间",
                        "cover": "//i0.hdslb.com/live.jpg",
                        "user_cover": "",
                        "uname": "主播",
                        "online": 6789,
                        "cate_name": "单机游戏"
                      }
                    ]
                    """.trimIndent()
                )
            )
        )

        assertEquals(1, result.lives.size)
        assertEquals(12345, result.lives.first().roomId)
        assertEquals("https://i0.hdslb.com/live.jpg", result.lives.first().cover)
        assertEquals(SearchTypePage(nextPageForWeb = 2), result.page)
    }

    @Test
    fun `app live search skips non result cards before live inline cards`() {
        val result = SearchTypeResult.fromSearchTypeResult(
            searchByTypeResponse {
                items.add(
                    item {
                        tips = searchTipsCard {
                            title = "tips"
                        }
                    }
                )
                items.add(
                    item {
                        param = "12345"
                        liveInline = searchLiveInlineCard {
                            roomid = 12345
                            title = "直播间"
                            cover = "https://i0.hdslb.com/live.jpg"
                        }
                    }
                )
                pagination = paginationReply {
                    next = "next-cursor"
                }
            }
        )

        assertEquals(1, result.lives.size)
        assertEquals(12345, result.lives.first().roomId)
        assertEquals(SearchTypePage(nextPageForApp = "next-cursor"), result.page)
    }

    @Test
    fun `preferred live parsing combines live result cards even after non-live cards`() {
        val result = SearchTypeResult.fromSearchTypeResult(
            searchResultData(
                result = Json.parseToJsonElement(
                    """
                    [
                      {
                        "type": "bili_user",
                        "mid": 1,
                        "uname": "用户",
                        "usign": "",
                        "fans": 0,
                        "videos": 0,
                        "upic": "",
                        "face_nft": 0,
                        "face_nft_type": 0,
                        "verify_info": "",
                        "level": 0,
                        "gender": 0,
                        "is_upuser": 0,
                        "is_live": 0,
                        "room_id": 0,
                        "res": [],
                        "official_verify": {"type": -1, "desc": ""},
                        "hit_columns": [],
                        "is_senior_member": 0
                      },
                      {
                        "type": "live_room",
                        "roomid": 45678,
                        "title": "直播间",
                        "cover": "//i0.hdslb.com/live2.jpg",
                        "user_cover": "",
                        "uname": "主播",
                        "online": 100,
                        "cate_name": "娱乐"
                      }
                    ]
                    """.trimIndent()
                )
            ),
            preferredType = SearchType.Live
        )

        assertEquals(1, result.lives.size)
        assertEquals(45678, result.lives.first().roomId)
    }

    private fun searchResultData(
        result: kotlinx.serialization.json.JsonElement
    ) = SearchResultData(
        seid = "",
        page = 1,
        pageSize = 20,
        numResults = 1,
        numPages = 1,
        suggestKeyword = "",
        rqtType = "",
        eggHit = 0,
        showColumn = 0,
        inBlackKey = 0,
        inWhiteKey = 0,
        result = result
    )
}
