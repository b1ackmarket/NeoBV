package dev.aaa1115910.bv.subtitle

import dev.aaa1115910.biliapi.entity.video.Subtitle
import dev.aaa1115910.biliapi.entity.video.SubtitleAiStatus
import dev.aaa1115910.biliapi.entity.video.SubtitleAiType
import dev.aaa1115910.biliapi.entity.video.SubtitleType
import dev.aaa1115910.bv.subtitle.translation.SubtitleTranslationConfig
import dev.aaa1115910.bv.ui.state.SubtitleMemory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SubtitleSelectionPolicyTest {
    @Test
    fun `secondary options exclude current main subtitle and prefer target language`() {
        val config = SubtitleTranslationConfig(targetLanguage = "en")
        val options = buildSecondarySubtitleOptions(
            tracks = listOf(chinese(), english(), japanese()),
            currentMainSubtitleId = 1L,
            config = config,
            preferCustom = false,
            sourceSubtitleAvailable = true
        )

        assertEquals(listOf(2L, 3L), options.map { it.id })
    }

    @Test
    fun `custom subtitle only appears after verified config and can be first`() {
        val config = SubtitleTranslationConfig(targetLanguage = "en")
            .let { it.copy(verifiedSignature = it.configSignature()) }
        val options = buildSecondarySubtitleOptions(
            tracks = listOf(chinese(), english()),
            currentMainSubtitleId = 1L,
            config = config,
            preferCustom = true,
            sourceSubtitleAvailable = true
        )

        assertIs<SecondarySubtitleOption.CustomTranslation>(options.first())
    }

    @Test
    fun `custom subtitle stays hidden without source subtitle`() {
        val config = SubtitleTranslationConfig(targetLanguage = "en")
            .let { it.copy(verifiedSignature = it.configSignature()) }
        val options = buildSecondarySubtitleOptions(
            tracks = listOf(english()),
            currentMainSubtitleId = -1L,
            config = config,
            preferCustom = true,
            sourceSubtitleAvailable = false
        )

        assertEquals(listOf(2L), options.map { it.id })
    }

    @Test
    fun `default secondary uses memory before ordering fallback`() {
        val config = SubtitleTranslationConfig(targetLanguage = "en")
        val options = buildSecondarySubtitleOptions(
            tracks = listOf(chinese(), english(), japanese()),
            currentMainSubtitleId = 1L,
            config = config,
            preferCustom = false,
            sourceSubtitleAvailable = true
        )

        val selected = resolveDefaultSecondarySubtitleOption(
            options = options,
            memory = SubtitleMemory(id = 3L, lang = "ja", langDoc = "日语")
        )

        assertEquals(3L, selected?.id)
    }

    private fun chinese() = subtitle(1L, "zh", "中文")
    private fun english() = subtitle(2L, "en", "英语")
    private fun japanese() = subtitle(3L, "ja", "日语")

    private fun subtitle(id: Long, lang: String, langDoc: String) = Subtitle(
        id = id,
        lang = lang,
        langDoc = langDoc,
        url = "",
        type = SubtitleType.CC,
        aiType = SubtitleAiType.Normal,
        aiStatus = SubtitleAiStatus.None
    )
}
