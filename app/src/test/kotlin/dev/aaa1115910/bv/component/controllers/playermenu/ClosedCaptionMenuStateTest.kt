package dev.aaa1115910.bv.component.controllers.playermenu

import dev.aaa1115910.biliapi.entity.video.Subtitle
import dev.aaa1115910.biliapi.entity.video.SubtitleAiStatus
import dev.aaa1115910.biliapi.entity.video.SubtitleAiType
import dev.aaa1115910.biliapi.entity.video.SubtitleType
import dev.aaa1115910.bv.subtitle.SecondarySubtitleOption
import kotlin.test.assertIs
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

    @Test
    fun `secondary subtitle menu restores close option first`() {
        val options = buildSecondarySubtitleMenuOptions(
            options = listOf(
                SecondarySubtitleOption.BiliTrack(subtitle(2L, "英语")),
                SecondarySubtitleOption.CustomTranslation
            )
        )

        assertIs<SecondarySubtitleMenuOption.Off>(options.first())
        assertEquals(0, resolveSelectedSecondarySubtitleMenuIndex(options, -1L, false))
        assertEquals(1, resolveSelectedSecondarySubtitleMenuIndex(options, 2L, false))
        assertEquals(2, resolveSelectedSecondarySubtitleMenuIndex(options, Long.MIN_VALUE, true))
    }

    private fun subtitle(id: Long, langDoc: String) = Subtitle(
        id = id,
        lang = "",
        langDoc = langDoc,
        url = "",
        type = SubtitleType.CC,
        aiType = SubtitleAiType.Normal,
        aiStatus = SubtitleAiStatus.None
    )
}
