package dev.aaa1115910.bv.subtitle.translation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SubtitleTranslationConfigTest {
    @Test
    fun `prompt injects target language placeholders without source language`() {
        val prompt = injectSubtitlePrompt(
            prompt = "from={sourcelanguage},to={targetlanguage},title={title},items={items},ctx={context}",
            targetLanguage = SubtitleLanguages.find("en"),
            title = "测试视频",
            context = "上一句",
            itemsJson = """[{"id":1,"text":"你好"}]"""
        )

        assertEquals(
            """from=,to=英语,title=测试视频,items=[{"id":1,"text":"你好"}],ctx=上一句""",
            prompt
        )
    }

    @Test
    fun `default prompt does not send video title`() {
        assertFalse(DefaultSubtitleTranslationPrompt.contains("{title}"))
        assertFalse(DefaultSubtitleTranslationPrompt.contains("视频标题"))
        assertFalse(DefaultSubtitleTranslationPrompt.contains("{sourcelanguage}"))
        assertTrue(DefaultSubtitleTranslationTestSentence.isNotBlank())
    }

    @Test
    fun `verified signature becomes invalid when config changes`() {
        val config = SubtitleTranslationConfig(
            providerType = SubtitleTranslationProviderType.OpenAiCompatible,
            targetLanguage = "en",
            openAiBaseUrl = "https://example.com/v1",
            openAiApiKey = "key",
            openAiModel = "model"
        )
        val verified = config.copy(verifiedSignature = config.configSignature())

        assertTrue(verified.verified())
        assertFalse(verified.copy(targetLanguage = "ja").verified())
        assertNotEquals(verified.configSignature(), verified.copy(targetLanguage = "ja").configSignature())
    }

    @Test
    fun `provider language mapping uses service specific codes`() {
        val chinese = SubtitleLanguages.find("zh")
        val japanese = SubtitleLanguages.find("ja")

        assertEquals("zh-Hans", chinese.providerCode(SubtitleTranslationProviderType.Microsoft))
        assertEquals("jp", japanese.providerCode(SubtitleTranslationProviderType.Baidu))
        assertEquals("JA", japanese.providerCode(SubtitleTranslationProviderType.DeepL))
    }

    @Test
    fun `translation parsers remove numeric prefixes from provider output`() {
        assertEquals(
            mapOf(7 to "Hello", 8 to "World"),
            parseTabLineTranslations(
                sourceItems = listOf(
                    SubtitleTranslationItem(7, "你好"),
                    SubtitleTranslationItem(8, "世界")
                ),
                translatedLines = listOf("1. Hello", "2、World")
            )
        )
        assertEquals(
            mapOf(3 to "Good morning"),
            parseIdTextTranslations("""[{"id":3,"text":"3. Good morning"}]""")
        )
    }
}
