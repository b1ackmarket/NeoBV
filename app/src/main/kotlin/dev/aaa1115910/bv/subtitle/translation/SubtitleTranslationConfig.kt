package dev.aaa1115910.bv.subtitle.translation

import dev.aaa1115910.bv.util.Prefs
import java.security.MessageDigest

enum class SubtitleTranslationProviderType(val displayName: String) {
    OpenAiCompatible("OpenAI 兼容"),
    Baidu("百度翻译"),
    Microsoft("Microsoft Translator"),
    DeepL("DeepL"),
    DeepLX("DeepLX");

    companion object {
        fun fromName(name: String): SubtitleTranslationProviderType {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
                ?: OpenAiCompatible
        }
    }
}

data class SubtitleLanguage(
    val code: String,
    val displayName: String,
    val baiduCode: String,
    val microsoftCode: String,
    val deepLCode: String
) {
    fun providerCode(providerType: SubtitleTranslationProviderType): String {
        return when (providerType) {
            SubtitleTranslationProviderType.Baidu -> baiduCode
            SubtitleTranslationProviderType.Microsoft -> microsoftCode
            SubtitleTranslationProviderType.DeepL,
            SubtitleTranslationProviderType.DeepLX -> deepLCode
            SubtitleTranslationProviderType.OpenAiCompatible -> code
        }
    }
}

object SubtitleLanguages {
    val supported = listOf(
        SubtitleLanguage("zh", "中文", "zh", "zh-Hans", "ZH"),
        SubtitleLanguage("en", "英语", "en", "en", "EN-US"),
        SubtitleLanguage("ja", "日语", "jp", "ja", "JA"),
        SubtitleLanguage("ko", "韩语", "kor", "ko", "KO"),
        SubtitleLanguage("fr", "法语", "fra", "fr", "FR"),
        SubtitleLanguage("de", "德语", "de", "de", "DE"),
        SubtitleLanguage("es", "西班牙语", "spa", "es", "ES"),
        SubtitleLanguage("ru", "俄语", "ru", "ru", "RU"),
        SubtitleLanguage("it", "意大利语", "it", "it", "IT"),
        SubtitleLanguage("pt", "葡萄牙语", "pt", "pt", "PT-PT")
    )

    fun find(code: String): SubtitleLanguage {
        return supported.firstOrNull { it.code.equals(code, ignoreCase = true) }
            ?: supported.first()
    }
}

data class SubtitleTranslationConfig(
    val providerType: SubtitleTranslationProviderType = SubtitleTranslationProviderType.OpenAiCompatible,
    val targetLanguage: String = "en",
    val contextBefore: Int = 2,
    val contextAfter: Int = 1,
    val requestBatchSize: Int = 12,
    val preTranslateSeconds: Int = 90,
    val openAiBaseUrl: String = "",
    val openAiApiKey: String = "",
    val openAiModel: String = "",
    val openAiPrompt: String = DefaultSubtitleTranslationPrompt,
    val baiduAppId: String = "",
    val baiduAppKey: String = "",
    val microsoftKey: String = "",
    val microsoftRegion: String = "",
    val microsoftEndpoint: String = "https://api.cognitive.microsofttranslator.com",
    val deepLApiKey: String = "",
    val deepLEndpoint: String = "https://api-free.deepl.com",
    val deepLXEndpoint: String = "",
    val deepLXApiKey: String = "",
    val verifiedSignature: String = ""
) {
    val sanitized: SubtitleTranslationConfig
        get() = copy(
            contextBefore = contextBefore.coerceIn(0, 20),
            contextAfter = contextAfter.coerceIn(0, 20),
            requestBatchSize = requestBatchSize.coerceIn(1, 80),
            preTranslateSeconds = preTranslateSeconds.coerceIn(15, 600),
            openAiBaseUrl = openAiBaseUrl.trim(),
            openAiApiKey = openAiApiKey.trim(),
            openAiModel = openAiModel.trim(),
            baiduAppId = baiduAppId.trim(),
            baiduAppKey = baiduAppKey.trim(),
            microsoftKey = microsoftKey.trim(),
            microsoftRegion = microsoftRegion.trim(),
            microsoftEndpoint = microsoftEndpoint.trim().ifBlank {
                "https://api.cognitive.microsofttranslator.com"
            },
            deepLApiKey = deepLApiKey.trim(),
            deepLEndpoint = deepLEndpoint.trim().ifBlank { "https://api-free.deepl.com" },
            deepLXEndpoint = deepLXEndpoint.trim(),
            deepLXApiKey = deepLXApiKey.trim()
        )

    fun configSignature(): String {
        val source = listOf(
            providerType.name,
            targetLanguage,
            contextBefore.coerceIn(0, 20).toString(),
            contextAfter.coerceIn(0, 20).toString(),
            requestBatchSize.coerceIn(1, 80).toString(),
            preTranslateSeconds.coerceIn(15, 600).toString(),
            openAiBaseUrl.trim(),
            maskSecret(openAiApiKey),
            openAiModel.trim(),
            openAiPrompt,
            baiduAppId.trim(),
            maskSecret(baiduAppKey),
            maskSecret(microsoftKey),
            microsoftRegion.trim(),
            microsoftEndpoint.trim(),
            maskSecret(deepLApiKey),
            deepLEndpoint.trim(),
            deepLXEndpoint.trim(),
            maskSecret(deepLXApiKey)
        ).joinToString("\u001f")
        return sha256(source)
    }

    fun verified(): Boolean = verifiedSignature.isNotBlank() && verifiedSignature == configSignature()
}

const val DefaultSubtitleTranslationPrompt: String =
    "你是视频字幕翻译助手。请把字幕翻译成 {targetlanguage}，用于学习二外。" +
        "保持原意自然流畅，保留专有名词，不添加解释。" +
        "必须按输入字幕 id 返回 JSON 数组，格式为 [{\"id\":1,\"text\":\"translation\"}]。" +
        "上下文：{context}。字幕：{items}"

fun injectSubtitlePrompt(
    prompt: String,
    targetLanguage: SubtitleLanguage,
    title: String,
    context: String,
    itemsJson: String
): String {
    return prompt
        .replace("{sourcelanguage}", "")
        .replace("{targetlanguage}", targetLanguage.displayName)
        .replace("{title}", title)
        .replace("{context}", context)
        .replace("{items}", itemsJson)
}

fun readSubtitleTranslationConfigFromPrefs(): SubtitleTranslationConfig {
    return SubtitleTranslationConfig(
        providerType = Prefs.subtitleTranslationProviderType,
        targetLanguage = Prefs.subtitleTranslationTargetLanguage,
        contextBefore = Prefs.bilingualSubtitleContextBefore,
        contextAfter = Prefs.bilingualSubtitleContextAfter,
        requestBatchSize = Prefs.bilingualSubtitleRequestBatchSize,
        preTranslateSeconds = Prefs.subtitleTranslationPreTranslateSeconds,
        openAiBaseUrl = Prefs.bilingualSubtitleBaseUrl,
        openAiApiKey = Prefs.bilingualSubtitleApiKey,
        openAiModel = Prefs.bilingualSubtitleModel,
        openAiPrompt = Prefs.bilingualSubtitlePrompt,
        baiduAppId = Prefs.subtitleTranslationBaiduAppId,
        baiduAppKey = Prefs.subtitleTranslationBaiduAppKey,
        microsoftKey = Prefs.subtitleTranslationMicrosoftKey,
        microsoftRegion = Prefs.subtitleTranslationMicrosoftRegion,
        microsoftEndpoint = Prefs.subtitleTranslationMicrosoftEndpoint,
        deepLApiKey = Prefs.subtitleTranslationDeepLApiKey,
        deepLEndpoint = Prefs.subtitleTranslationDeepLEndpoint,
        deepLXEndpoint = Prefs.subtitleTranslationDeepLXEndpoint,
        deepLXApiKey = Prefs.subtitleTranslationDeepLXApiKey,
        verifiedSignature = Prefs.subtitleTranslationVerifiedSignature
    ).sanitized
}

fun writeSubtitleTranslationConfigToPrefs(config: SubtitleTranslationConfig) {
    val sanitized = config.sanitized
    Prefs.subtitleTranslationProviderType = sanitized.providerType
    Prefs.subtitleTranslationTargetLanguage = sanitized.targetLanguage
    Prefs.bilingualSubtitleContextBefore = sanitized.contextBefore
    Prefs.bilingualSubtitleContextAfter = sanitized.contextAfter
    Prefs.bilingualSubtitleRequestBatchSize = sanitized.requestBatchSize
    Prefs.subtitleTranslationPreTranslateSeconds = sanitized.preTranslateSeconds
    Prefs.bilingualSubtitleBaseUrl = sanitized.openAiBaseUrl
    Prefs.bilingualSubtitleApiKey = sanitized.openAiApiKey
    Prefs.bilingualSubtitleModel = sanitized.openAiModel
    Prefs.bilingualSubtitlePrompt = sanitized.openAiPrompt
    Prefs.subtitleTranslationBaiduAppId = sanitized.baiduAppId
    Prefs.subtitleTranslationBaiduAppKey = sanitized.baiduAppKey
    Prefs.subtitleTranslationMicrosoftKey = sanitized.microsoftKey
    Prefs.subtitleTranslationMicrosoftRegion = sanitized.microsoftRegion
    Prefs.subtitleTranslationMicrosoftEndpoint = sanitized.microsoftEndpoint
    Prefs.subtitleTranslationDeepLApiKey = sanitized.deepLApiKey
    Prefs.subtitleTranslationDeepLEndpoint = sanitized.deepLEndpoint
    Prefs.subtitleTranslationDeepLXEndpoint = sanitized.deepLXEndpoint
    Prefs.subtitleTranslationDeepLXApiKey = sanitized.deepLXApiKey
    Prefs.subtitleTranslationVerifiedSignature = sanitized.verifiedSignature
}

private fun maskSecret(secret: String): String = secret.trim()

fun sha256(value: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}
