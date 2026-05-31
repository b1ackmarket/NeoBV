package dev.aaa1115910.biliapi.repositories

import dev.aaa1115910.biliapi.entity.pgc.PgcType
import dev.aaa1115910.biliapi.entity.pgc.index.Area
import dev.aaa1115910.biliapi.entity.pgc.index.Copyright
import dev.aaa1115910.biliapi.entity.pgc.index.IndexOrder
import dev.aaa1115910.biliapi.entity.pgc.index.IndexOrderType
import dev.aaa1115910.biliapi.entity.pgc.index.IsFinish
import dev.aaa1115910.biliapi.entity.pgc.index.PgcIndexData
import dev.aaa1115910.biliapi.entity.pgc.index.Producer
import dev.aaa1115910.biliapi.entity.pgc.index.ReleaseDate
import dev.aaa1115910.biliapi.entity.pgc.index.SeasonMonth
import dev.aaa1115910.biliapi.entity.pgc.index.SeasonStatus
import dev.aaa1115910.biliapi.entity.pgc.index.SeasonVersion
import dev.aaa1115910.biliapi.entity.pgc.index.SpokenLanguage
import dev.aaa1115910.biliapi.entity.pgc.index.Style
import dev.aaa1115910.biliapi.entity.pgc.index.Year
import dev.aaa1115910.biliapi.http.entity.BiliResponse
import dev.aaa1115910.biliapi.http.entity.pgc.PgcCinemaTabData
import dev.aaa1115910.biliapi.http.entity.pgc.PgcFeedData
import dev.aaa1115910.biliapi.http.entity.pgc.PgcFeedV3Data
import dev.aaa1115910.biliapi.http.entity.pgc.PgcRankData
import dev.aaa1115910.biliapi.entity.pgc.PgcCinemaTabData as DomainPgcCinemaTabData
import dev.aaa1115910.biliapi.entity.pgc.PgcFeedData as DomainPgcFeedData
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PgcRepositoryTest {
    private val pgcRepository: PgcRepository = PgcRepository()
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Test
    fun `get pgc carousel data`() {
        runBlocking {
            PgcType.entries.forEach { pgcType ->
                println("pgcType: $pgcType")
                val data = pgcRepository.getCarousel(pgcType)
                println(data)
            }
        }
    }

    @Test
    fun `get pgc feed data`() {
        runBlocking {
            PgcType.entries.forEach { pgcType ->
                println("pgcType: $pgcType")
                val data = pgcRepository.getFeed(
                    pgcType = pgcType,
                    cursor = 0
                )
                println(data)
            }
        }
    }

    @Test
    fun `get pgc index`(){
        runBlocking {
            PgcType.entries.forEach { pgcType ->
                println("pgcType: $pgcType")
                val data=pgcRepository.getPgcIndex(
                    pgcType = pgcType,
                    indexOrder = IndexOrder.PlayCount,
                    indexOrderType = IndexOrderType.Desc,
                    seasonVersion = SeasonVersion.All,
                    spokenLanguage = SpokenLanguage.All,
                    area=Area.All,
                    isFinish = IsFinish.All,
                    copyright = Copyright.All,
                    seasonStatus = SeasonStatus.All,
                    seasonMonth = SeasonMonth.All,
                    producer = Producer.All,
                    year = Year.All,
                    releaseDate = ReleaseDate.All,
                    style = Style.All,
                    page = PgcIndexData.PgcIndexPage()
                )
                println(data)
            }
        }
    }

    @Test
    fun `decode guochuang rank with large view count`() {
        val response = json.decodeFromString<BiliResponse<PgcRankData>>(
            """
            {
              "code": 0,
              "data": {
                "list": [
                  {
                    "cover": "https://example.com/cover.png",
                    "new_ep": {"index_show": "更新至第176话"},
                    "rating": "9.7分",
                    "season_id": 28747,
                    "season_type": 4,
                    "stat": {"follow": 16580629, "view": 6423687065},
                    "title": "凡人修仙传",
                    "url": "https://www.bilibili.com/bangumi/play/ss28747"
                  }
                ],
                "note": "近 3 日热度",
                "season_type": 4
              }
            }
            """.trimIndent()
        )

        val item = response.getResponseData().list.single()
        assertEquals(28747, item.seasonId)
        assertEquals(6423687065L, item.stat?.view)
    }

    @Test
    fun `decode tv feed item with zero stat values`() {
        val response = json.decodeFromString<BiliResponse<PgcFeedData>>(
            """
            {
              "code": 0,
              "data": {
                "coursor": 14,
                "has_next": true,
                "items": [
                  {
                    "cover": "https://example.com/tv.png",
                    "episode_id": 3665037,
                    "hover": {"img": "https://example.com/hover.jpg", "text": ["科幻", "全12集"]},
                    "link": "https://www.bilibili.com/bangumi/play/ep3665037",
                    "rank_id": 0,
                    "season_id": 220269,
                    "season_type": 5,
                    "stat": {"danmaku": 0, "duration": 0, "view": 1835193},
                    "sub_title": "神秘博士的空间冒险",
                    "text": [],
                    "title": "神秘博士 第九季",
                    "user_status": {"follow": 0}
                  }
                ]
              }
            }
            """.trimIndent()
        )

        val data = response.getResponseData()
        assertTrue(data.hasNext)
        assertEquals(220269, data.items.single().seasonId)
    }

    @Test
    fun `convert pgc feed skips malformed item only`() {
        val response = json.decodeFromString<BiliResponse<PgcFeedData>>(
            """
            {
              "code": 0,
              "data": {
                "coursor": 14,
                "has_next": true,
                "items": [
                  {
                    "cover": "https://example.com/tv.png",
                    "episode_id": 3665037,
                    "rank_id": 0,
                    "season_id": 220269,
                    "season_type": 5,
                    "sub_title": "神秘博士的空间冒险",
                    "title": "神秘博士 第九季"
                  },
                  {
                    "cover": "https://example.com/bad.png",
                    "episode_id": 1,
                    "rank_id": 0,
                    "sub_title": "missing season_id",
                    "title": "bad item"
                  }
                ]
              }
            }
            """.trimIndent()
        )

        val data = DomainPgcFeedData.fromPgcFeedData(response.getResponseData())

        assertEquals(1, data.items.size)
        assertEquals(220269, data.items.single().seasonId)
    }

    @Test
    fun `convert pgc feed v3 ignores empty groups`() {
        val response = json.decodeFromString<BiliResponse<PgcFeedV3Data>>(
            """
            {
              "code": 0,
              "data": {
                "coursor": 14,
                "has_next": true,
                "items": [
                  {"rank_id": 0, "sub_items": []},
                  {
                    "rank_id": 1,
                    "sub_items": [
                      {
                        "card_style": "v_card",
                        "cover": "https://example.com/anime.png",
                        "episode_id": 1,
                        "rank_id": 0,
                        "report": {},
                        "season_id": 2,
                        "season_type": 1,
                        "sub_title": "sub",
                        "title": "title"
                      }
                    ]
                  }
                ]
              }
            }
            """.trimIndent()
        )

        val data = DomainPgcFeedData.fromPgcFeedData(response.getResponseData())

        assertEquals(1, data.items.size)
        assertEquals(2, data.items.single().seasonId)
    }

    @Test
    fun `convert cinema tab keeps banner and content rows`() {
        val response = json.decodeFromString<BiliResponse<PgcCinemaTabData>>(
            """
            {
              "code": 0,
              "result": {
                "has_next": 1,
                "next_cursor": "{\"feed_season_index\":26}",
                "modules": [
                  {
                    "title": "新banner16:9",
                    "style": "banner_v3",
                    "items": [
                      {
                        "cover": "//example.com/banner.png",
                        "episode_id": 3044114,
                        "season_id": 145780,
                        "title": "banner"
                      }
                    ]
                  },
                  {
                    "title": "功能入口",
                    "style": "function",
                    "items": [{"title": "纪录片"}]
                  },
                  {
                    "title": "电影热播",
                    "style": "v_card",
                    "items": [
                      {
                        "cover": "https://example.com/movie.png",
                        "desc": "2021-11-11上映",
                        "season_id": 39892,
                        "season_type": 2,
                        "title": "扬名立万"
                      },
                      {
                        "cover": "https://example.com/movie2.png",
                        "desc": "第二部",
                        "season_id": 39893,
                        "season_type": 2,
                        "title": "第二部电影"
                      }
                    ]
                  },
                  {
                    "title": "热门榜单",
                    "style": "rank_v2",
                    "items": [
                      {
                        "title": "热门电影",
                        "items": [
                          {
                            "cover": "https://example.com/rank.png",
                            "season_id": 106518,
                            "season_type": 2,
                            "title": "阿凡达：火与烬",
                            "new_ep": {"index_show": "2025-12-19上映"}
                          },
                          {
                            "cover": "https://example.com/rank2.png",
                            "season_id": 106519,
                            "season_type": 2,
                            "title": "第二名"
                          }
                        ]
                      },
                      {
                        "title": "热门纪录片",
                        "items": [
                          {
                            "cover": "https://example.com/documentary.png",
                            "season_id": 106520,
                            "season_type": 3,
                            "title": "纪录片第一名"
                          }
                        ]
                      }
                    ]
                  }
                ]
              }
            }
            """.trimIndent()
        )

        val data = DomainPgcCinemaTabData.fromHttpData(response.getResponseData())

        assertEquals(true, data.hasNext)
        assertEquals("https://example.com/banner.png", data.carouselItems.single().cover)
        assertEquals(2, data.rows.size)
        assertEquals("电影热播", data.rows.first().title)
        assertEquals("电影热播", data.rows.first().topics.single().title)
        assertEquals(2, data.rows.first().topics.single().items.size)
        assertEquals(39892, data.rows.first().topics.single().items.first().seasonId)
        assertEquals("热门电影", data.rows[1].topics.first().title)
        assertEquals("2025-12-19上映", data.rows[1].topics.first().items.first().subTitle)
        assertEquals("热门纪录片", data.rows[1].topics[1].title)
    }
}
