package dev.aaa1115910.biliapi.entity

import dev.aaa1115910.biliapi.http.entity.video.PlayUrlData
import dev.aaa1115910.biliapi.http.entity.video.PlayUrlLanguage
import dev.aaa1115910.biliapi.http.entity.video.PlayUrlLanguageItem
import kotlin.test.Test
import kotlin.test.assertEquals

class PlayDataAiTranslationTest {
    @Test
    fun `play url language items map to ai audio translations`() {
        val data = PlayData.fromPlayUrlData(
            PlayUrlData(
                from = "local",
                result = "suee",
                message = "",
                quality = 80,
                format = "hdflv2",
                timeLength = 1000,
                acceptFormat = "hdflv2",
                videoCodecId = 7,
                seekParam = "start",
                seekType = "offset",
                language = PlayUrlLanguage(
                    support = true,
                    items = listOf(
                        PlayUrlLanguageItem(lang = "en", title = "English", subtitleLang = "ai-en")
                    )
                ),
                currentLanguage = "en"
            )
        )

        assertEquals(
            listOf(AiAudioTranslation(lang = "en", title = "English", subtitleLang = "ai-en")),
            data.aiAudioTranslations
        )
        assertEquals("en", data.currentAiAudioLanguage)
    }
}
