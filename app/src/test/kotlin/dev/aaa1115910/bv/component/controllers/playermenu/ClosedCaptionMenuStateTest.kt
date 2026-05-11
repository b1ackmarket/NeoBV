package dev.aaa1115910.bv.component.controllers.playermenu

import dev.aaa1115910.biliapi.entity.video.Subtitle
import dev.aaa1115910.biliapi.entity.video.SubtitleAiStatus
import dev.aaa1115910.biliapi.entity.video.SubtitleAiType
import dev.aaa1115910.biliapi.entity.video.SubtitleType
import kotlin.test.Test
import kotlin.test.assertEquals

class ClosedCaptionMenuStateTest {
    @Test
    fun `missing selected subtitle falls back to close option`() {
        val subtitles = listOf(
            Subtitle(
                id = -1,
                lang = "",
                langDoc = "关闭",
                url = "",
                type = SubtitleType.CC,
                aiType = SubtitleAiType.Normal,
                aiStatus = SubtitleAiStatus.None
            ),
            Subtitle(
                id = 100L,
                lang = "zh-CN",
                langDoc = "中文",
                url = "https://example.com/subtitle.json",
                type = SubtitleType.CC,
                aiType = SubtitleAiType.Normal,
                aiStatus = SubtitleAiStatus.None
            )
        )

        assertEquals(0, resolveSelectedSubtitleTrackIndex(currentSubtitleId = 999L, tracks = subtitles))
        assertEquals(1, resolveSelectedSubtitleTrackIndex(currentSubtitleId = 100L, tracks = subtitles))
    }
}
