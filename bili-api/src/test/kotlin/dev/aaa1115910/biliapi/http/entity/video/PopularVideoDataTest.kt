package dev.aaa1115910.biliapi.http.entity.video

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class PopularVideoDataTest {
    private val json = Json {
        coerceInputValues = true
        ignoreUnknownKeys = true
    }

    @Test
    fun `popular precious videos accept object ogv info`() {
        val response = json.decodeFromString<PopularVideoData>(
            """
            {
              "no_more": true,
              "list": [
                {
                  "aid": 1,
                  "bvid": "BV1000000001",
                  "videos": 1,
                  "tid": 21,
                  "tname": "日常",
                  "copyright": 1,
                  "pic": "https://i0.hdslb.com/test.jpg",
                  "title": "入站必刷",
                  "pubdate": 1,
                  "desc": "",
                  "state": 0,
                  "duration": 60,
                  "rights": {
                    "bp": 0,
                    "elec": 0,
                    "download": 0,
                    "movie": 0,
                    "pay": 0,
                    "hd5": 1,
                    "no_reprint": 1,
                    "autoplay": 1,
                    "ugc_pay": 0,
                    "is_cooperation": 0,
                    "ugc_pay_preview": 0,
                    "arc_pay": 0
                  },
                  "owner": {"mid": 2, "name": "up", "face": ""},
                  "stat": {"aid": 1, "view": 3, "danmaku": 4},
                  "dynamic": "",
                  "cid": 5,
                  "dimension": {"width": 1920, "height": 1080, "rotate": 0},
                  "is_ogv": false,
                  "ogv_info": {"title": "ignored"}
                }
              ]
            }
            """.trimIndent()
        )

        assertEquals(1, response.list.single().aid)
        assertEquals("BV1000000001", response.list.single().bvid)
    }

    @Test
    fun `popular precious videos accept missing no more flag`() {
        val response = json.decodeFromString<PopularVideoData>(
            """
            {
              "list": []
            }
            """.trimIndent()
        )

        assertEquals(true, response.noMore)
    }
}
