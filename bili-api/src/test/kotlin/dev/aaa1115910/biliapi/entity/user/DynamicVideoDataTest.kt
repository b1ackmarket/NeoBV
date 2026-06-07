package dev.aaa1115910.biliapi.entity.user

import dev.aaa1115910.biliapi.http.entity.dynamic.DynamicData
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class DynamicVideoDataTest {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Test
    fun `web dynamic mapping skips non archive items`() {
        val data = json.decodeFromString<DynamicData>(
            """
            {
              "has_more": true,
              "offset": "next-offset",
              "update_baseline": "baseline",
              "update_num": 0,
              "items": [
                ${dynamicItemJson(archive = true)},
                ${dynamicItemJson(archive = false)}
              ]
            }
            """.trimIndent()
        )

        val mapped = DynamicVideoData.fromDynamicData(data)

        assertEquals(1, mapped.videos.size)
        assertEquals(1001L, mapped.videos.first().aid)
    }

    @Test
    fun `web dynamic mapping tolerates loose boolean metadata`() {
        val data = json.decodeFromString<DynamicData>(
            """
            {
              "has_more": true,
              "offset": "next-offset",
              "update_baseline": "baseline",
              "update_num": 0,
              "items": [
                ${withLooseBooleanMetadata(dynamicItemJson(archive = true))}
              ]
            }
            """.trimIndent()
        )

        val mapped = DynamicVideoData.fromDynamicData(data)

        assertEquals(1, mapped.videos.size)
        assertEquals("UP", mapped.videos.first().author)
    }

    private fun dynamicItemJson(archive: Boolean): String {
        val major = if (archive) {
            """
            "major": {
              "type": "MAJOR_TYPE_ARCHIVE",
              "archive": {
                "aid": "1001",
                "badge": {"bg_color":"","color":"","text":""},
                "bvid": "BV1001",
                "cover": "cover.jpg",
                "desc": "",
                "disable_preview": 0,
                "duration_text": "01:02",
                "jump_url": "",
                "stat": {"danmaku": "3", "play": "2万"},
                "title": "动态视频｜标题",
                "type": 1
              }
            }
            """.trimIndent()
        } else {
            """"major": {"type": "MAJOR_TYPE_COMMON"}"""
        }
        return """
            {
              "basic": {
                "comment_id_str": "",
                "comment_type": 0,
                "like_icon": {"action_url":"","end_url":"","id":0,"start_url":""},
                "rid_str": ""
              },
              "id_str": "dynamic-id",
              "modules": {
                "module_author": {
                  "face": "",
                  "jump_url": "",
                  "label": "",
                  "mid": 42,
                  "name": "UP",
                  "official_verify": {"desc":"","type":-1},
                  "pendant": {"expire":0,"image":"","image_enhance":"","image_enhance_frame":"","name":"","pid":0},
                  "pub_action": "",
                  "pub_location_text": "",
                  "pub_time": "刚刚",
                  "pub_ts": 0,
                  "type": "",
                  "vip": {"avatar_subscript":0,"avatar_subscript_url":"","due_date":0,"label":{"bg_color":"","bg_style":0,"border_color":"","img_label_uri_hans":"","img_label_uri_hans_static":"","img_label_uri_hant":"","img_label_uri_hant_static":"","label_theme":"","path":"","text":"","text_color":"","use_img_label":false},"nickname_color":"","status":0,"theme_type":0,"type":0}
                },
                "module_dynamic": {$major},
                "module_more": {"three_point_items":[]},
                "module_stat": {
                  "comment": {"count":0},
                  "forward": {"count":0},
                  "like": {"count":0}
                }
              },
              "type": "DYNAMIC_TYPE_AV"
            }
        """.trimIndent()
    }

    private fun withLooseBooleanMetadata(json: String): String {
        return json
            .replace(
                """"type": "DYNAMIC_TYPE_AV"""",
                """"visible": "..", "type": "DYNAMIC_TYPE_AV""""
            )
            .replace(
                """"face": "",""",
                """"face": "", "face_nft": "..", "following": "..","""
            )
            .replace(
                """"comment": {"count":0}""",
                """"comment": {"count":0, "forbidden": "..", "statue": ".."}"""
            )
    }
}
