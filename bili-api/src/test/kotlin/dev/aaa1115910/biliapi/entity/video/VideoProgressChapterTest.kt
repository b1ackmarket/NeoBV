package dev.aaa1115910.biliapi.entity.video

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlin.test.Test
import kotlin.test.assertEquals

class VideoProgressChapterTest {
    @Test
    fun `missing web view points return no chapters`() {
        assertEquals(emptyList(), VideoProgressChapter.fromViewPoints(null, durationMs = 200_000))
    }

    @Test
    fun `parse web view points into chapters`() {
        val points = Json.parseToJsonElement(
            """
            [
              {"from": 0, "to": 92, "content": "开场", "cover": "https://i0.hdslb.com/a.jpg", "type": 1},
              {"from": 92, "to": 186, "content": "实机演示", "cover": "", "type": 1}
            ]
            """.trimIndent()
        ).jsonArray

        val chapters = VideoProgressChapter.fromViewPoints(points, durationMs = 200_000)

        assertEquals(
            listOf(
                VideoProgressChapter(
                    startMs = 0,
                    endMs = 92_000,
                    title = "开场",
                    cover = "https://i0.hdslb.com/a.jpg",
                    type = 1
                ),
                VideoProgressChapter(
                    startMs = 92_000,
                    endMs = 186_000,
                    title = "实机演示",
                    cover = "",
                    type = 1
                )
            ),
            chapters
        )
    }

    @Test
    fun `chapter end falls back to next chapter or duration`() {
        val points = Json.parseToJsonElement(
            """
            [
              {"from": 0, "content": "第一段"},
              {"from": 30, "content": "第二段"}
            ]
            """.trimIndent()
        ).jsonArray

        val chapters = VideoProgressChapter.fromViewPoints(points, durationMs = 80_000)

        assertEquals(30_000, chapters[0].endMs)
        assertEquals(80_000, chapters[1].endMs)
    }

    @Test
    fun `chapter end keeps last chapter when duration is unknown`() {
        val points = Json.parseToJsonElement(
            """
            [
              {"from": 0, "content": "第一段"},
              {"from": 30, "content": "第二段"}
            ]
            """.trimIndent()
        ).jsonArray

        val chapters = VideoProgressChapter.fromViewPoints(points, durationMs = 0)

        assertEquals(30_000, chapters[0].endMs)
        assertEquals(Long.MAX_VALUE, chapters[1].endMs)
    }

    @Test
    fun `parse decimal second and millisecond time fields`() {
        val points = Json.parseToJsonElement(
            """
            [
              {"from": 1.5, "to": 3.25, "content": "秒字段"},
              {"startMs": 4000, "endMs": 6000, "content": "毫秒字段"}
            ]
            """.trimIndent()
        ).jsonArray

        val chapters = VideoProgressChapter.fromViewPoints(points, durationMs = 8_000)

        assertEquals(1_500, chapters[0].startMs)
        assertEquals(3_250, chapters[0].endMs)
        assertEquals(4_000, chapters[1].startMs)
        assertEquals(6_000, chapters[1].endMs)
    }

    @Test
    fun `find chapter by preview position`() {
        val chapters = listOf(
            VideoProgressChapter(startMs = 0, endMs = 5_000, title = "序章"),
            VideoProgressChapter(startMs = 5_000, endMs = 12_000, title = "正片")
        )

        assertEquals("序章", chapters.currentChapterAt(4_999)?.title)
        assertEquals("正片", chapters.currentChapterAt(5_000)?.title)
        assertEquals("正片", chapters.currentChapterAt(12_000)?.title)
    }
}
