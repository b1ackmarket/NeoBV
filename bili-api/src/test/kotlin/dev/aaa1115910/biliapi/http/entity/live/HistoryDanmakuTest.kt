package dev.aaa1115910.biliapi.http.entity.live

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class HistoryDanmakuTest {
    @Test
    fun `history danmaku accepts 64 bit rnd string from live api`() {
        val json = Json {
            coerceInputValues = true
            ignoreUnknownKeys = true
        }

        val decoded = json.decodeFromString<HistoryDanmaku>(
            """
            {
              "room": [
                {
                  "text": "hello",
                  "dm_type": 0,
                  "uid": 1,
                  "nickname": "user",
                  "uname_color": "",
                  "timeline": "2026-05-11 00:11:56",
                  "isadmin": 0,
                  "vip": 0,
                  "svip": 0,
                  "medal": [],
                  "title": [],
                  "user_level": [],
                  "rank": 0,
                  "teamid": 0,
                  "rnd": "1778429083018",
                  "user_title": "",
                  "guard_level": 0,
                  "bubble": 0,
                  "bubble_color": "",
                  "lpl": 0,
                  "yeah_space_url": "",
                  "jump_to_url": "",
                  "check_info": { "ts": 0, "ct": "" },
                  "voice_dm_info": {
                    "voice_url": "",
                    "file_format": "",
                    "text": "",
                    "file_duration": 0,
                    "file_id": ""
                  },
                  "emoticon": {
                    "id": 0,
                    "emoticon_unique": "",
                    "text": "",
                    "perm": 0,
                    "url": "",
                    "in_player_area": 0,
                    "bulge_display": 0,
                    "is_dynamic": 0,
                    "height": 0,
                    "width": 0
                  }
                }
              ]
            }
            """.trimIndent()
        )

        assertEquals(1778429083018L, decoded.room.first().rnd.toLong())
    }

    @Test
    fun `history danmaku accepts empty rnd from live api`() {
        val json = Json {
            coerceInputValues = true
            ignoreUnknownKeys = true
        }

        val decoded = json.decodeFromString<HistoryDanmaku>(
            """
            {
              "room": [
                {
                  "text": "hello",
                  "dm_type": 0,
                  "uid": 1,
                  "nickname": "user",
                  "uname_color": "",
                  "timeline": "2026-05-11 00:11:56",
                  "isadmin": 0,
                  "vip": 0,
                  "svip": 0,
                  "medal": [],
                  "title": [],
                  "user_level": [],
                  "rank": 10000,
                  "teamid": 0,
                  "rnd": "",
                  "user_title": "",
                  "guard_level": 0,
                  "bubble": 0,
                  "bubble_color": "",
                  "lpl": 0,
                  "yeah_space_url": "",
                  "jump_to_url": "",
                  "check_info": { "ts": 0, "ct": "" },
                  "voice_dm_info": {
                    "voice_url": "",
                    "file_format": "",
                    "text": "",
                    "file_duration": 0,
                    "file_id": ""
                  },
                  "emoticon": {
                    "id": 0,
                    "emoticon_unique": "",
                    "text": "",
                    "perm": 0,
                    "url": "",
                    "in_player_area": 0,
                    "bulge_display": 0,
                    "is_dynamic": 0,
                    "height": 0,
                    "width": 0
                  }
                }
              ]
            }
            """.trimIndent()
        )

        assertEquals(0L, decoded.room.first().rnd)
    }
}
