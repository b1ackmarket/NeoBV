package dev.aaa1115910.bv.repository

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LiveRoomContextTest {
    @Test
    fun `room context detects chat room and extracts voice members`() {
        val json = Json.parseToJsonElement(
            """
            {
              "room_info": {
                "uid": 3706953341601921,
                "room_id": 1962114200,
                "title": "好听得有点想那个了",
                "cover": "https://example.com/cover.jpg",
                "keyframe": "https://example.com/keyframe.jpg",
                "live_status": 1,
                "live_screen_type": 1,
                "area_name": "点唱",
                "parent_area_name": "聊天室",
                "room_type": { "3-71": 0 }
              },
              "multi_voice": {
                "members": [
                  {
                    "nickname": "Hy-蝴蝶效应-招主持",
                    "avatar": "https://example.com/a.jpg",
                    "price_text": "5.8w",
                    "is_mute": 1
                  },
                  {
                    "nickname": "猫本kitty",
                    "avatar": "https://example.com/b.jpg",
                    "price_text": "6075",
                    "is_mute": 0
                  }
                ]
              }
            }
            """.trimIndent()
        ).jsonObject

        val context = json.toLiveRoomContext(fallbackRoomId = 1)

        assertTrue(context.isChatRoom)
        assertEquals(1962114200, context.roomId)
        assertEquals(2, context.voiceMembers.size)
        assertEquals("Hy-蝴蝶效应-招主持", context.voiceMembers.first().nickname)
        assertTrue(context.voiceMembers.first().isMute)
    }
}
