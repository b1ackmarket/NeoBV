package dev.aaa1115910.bv.subtitle.translation

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.URLBuilder
import io.ktor.http.appendPathSegments
import io.ktor.http.contentType
import io.ktor.http.encodeURLParameter
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import java.security.MessageDigest
import kotlin.random.Random

data class SubtitleTranslationItem(
    val id: Int,
    val text: String
)

data class SubtitleTranslationRequest(
    val title: String,
    val targetLanguage: SubtitleLanguage,
    val context: String,
    val items: List<SubtitleTranslationItem>
)

data class SubtitleTranslationResult(
    val translations: Map<Int, String>
)

interface SubtitleTranslationProvider {
    suspend fun translate(request: SubtitleTranslationRequest): SubtitleTranslationResult
}

object SubtitleTranslationProviderFactory {
    fun create(config: SubtitleTranslationConfig): SubtitleTranslationProvider {
        val sanitized = config.sanitized
        return when (sanitized.providerType) {
            SubtitleTranslationProviderType.OpenAiCompatible -> OpenAiCompatibleSubtitleTranslationProvider(sanitized)
            SubtitleTranslationProviderType.Baidu -> BaiduSubtitleTranslationProvider(sanitized)
            SubtitleTranslationProviderType.Microsoft -> MicrosoftSubtitleTranslationProvider(sanitized)
            SubtitleTranslationProviderType.DeepL -> DeepLSubtitleTranslationProvider(sanitized)
            SubtitleTranslationProviderType.DeepLX -> DeepLXSubtitleTranslationProvider(sanitized)
        }
    }
}

class OpenAiCompatibleSubtitleTranslationProvider(
    private val config: SubtitleTranslationConfig,
    private val client: HttpClient = defaultSubtitleHttpClient()
) : SubtitleTranslationProvider {
    override suspend fun translate(request: SubtitleTranslationRequest): SubtitleTranslationResult {
        require(config.openAiBaseUrl.isNotBlank()) { "Base URL 不能为空" }
        require(config.openAiApiKey.isNotBlank()) { "API Key 不能为空" }
        require(config.openAiModel.isNotBlank()) { "模型名不能为空" }

        val itemsJson = request.items.toJsonArray().toString()
        val prompt = injectSubtitlePrompt(
            prompt = config.openAiPrompt.ifBlank { DefaultSubtitleTranslationPrompt },
            targetLanguage = request.targetLanguage,
            title = "",
            context = request.context,
            itemsJson = itemsJson
        )
        val body = buildJsonObject {
            put("model", JsonPrimitive(config.openAiModel))
            putJsonArray("messages") {
                addJsonObject {
                    put("role", JsonPrimitive("system"))
                    put("content", JsonPrimitive("只返回 JSON，不要 Markdown，不要解释。"))
                }
                addJsonObject {
                    put("role", JsonPrimitive("user"))
                    put("content", JsonPrimitive(prompt))
                }
            }
            put("temperature", JsonPrimitive(0.2))
        }
        val text = client.post(config.openAiBaseUrl.joinUrlPath("chat/completions")) {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${config.openAiApiKey}")
            setBody(body.toString())
        }.bodyAsText()
        val content = json.parseToJsonElement(text).jsonObject
            .getValue("choices").jsonArray
            .firstOrNull()?.jsonObject
            ?.get("message")?.jsonObject
            ?.get("content")?.jsonPrimitive?.contentOrNull
            ?: error("接口未返回翻译内容")
        return SubtitleTranslationResult(parseIdTextTranslations(content))
    }
}

class BaiduSubtitleTranslationProvider(
    private val config: SubtitleTranslationConfig,
    private val client: HttpClient = defaultSubtitleHttpClient()
) : SubtitleTranslationProvider {
    override suspend fun translate(request: SubtitleTranslationRequest): SubtitleTranslationResult {
        require(config.baiduAppId.isNotBlank()) { "百度 App ID 不能为空" }
        require(config.baiduAppKey.isNotBlank()) { "百度 App Key 不能为空" }
        val targetCode = request.targetLanguage.providerCode(SubtitleTranslationProviderType.Baidu)
        val query = request.items.joinToString("\n") { it.text }
        val salt = Random.nextInt(100000, 999999).toString()
        val sign = md5(config.baiduAppId + query + salt + config.baiduAppKey)
        val response = client.post("https://fanyi-api.baidu.com/api/trans/vip/translate") {
            contentType(ContentType.Application.FormUrlEncoded)
            setBody(
                listOf(
                    "q" to query,
                    "from" to "auto",
                    "to" to targetCode,
                    "appid" to config.baiduAppId,
                    "salt" to salt,
                    "sign" to sign
                ).formBody()
            )
        }.bodyAsText()
        val lines = json.parseToJsonElement(response).jsonObject["trans_result"]
            ?.jsonArray
            ?.mapNotNull { it.jsonObject["dst"]?.jsonPrimitive?.contentOrNull }
            .orEmpty()
        return SubtitleTranslationResult(parseTabLineTranslations(request.items, lines))
    }
}

class MicrosoftSubtitleTranslationProvider(
    private val config: SubtitleTranslationConfig,
    private val client: HttpClient = defaultSubtitleHttpClient()
) : SubtitleTranslationProvider {
    override suspend fun translate(request: SubtitleTranslationRequest): SubtitleTranslationResult {
        require(config.microsoftKey.isNotBlank()) { "Microsoft Key 不能为空" }
        val endpoint = config.microsoftEndpoint.trimEnd('/')
        val targetCode = request.targetLanguage.providerCode(SubtitleTranslationProviderType.Microsoft)
        val body = buildJsonArray {
            request.items.forEach { add(buildJsonObject { put("Text", JsonPrimitive(it.text)) }) }
        }
        val response = client.post("$endpoint/translate?api-version=3.0&to=$targetCode") {
            contentType(ContentType.Application.Json)
            header("Ocp-Apim-Subscription-Key", config.microsoftKey)
            if (config.microsoftRegion.isNotBlank()) {
                header("Ocp-Apim-Subscription-Region", config.microsoftRegion)
            }
            setBody(body.toString())
        }.bodyAsText()
        val lines = json.parseToJsonElement(response).jsonArray.mapNotNull { item ->
            item.jsonObject["translations"]?.jsonArray?.firstOrNull()
                ?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull
        }
        return SubtitleTranslationResult(parseTabLineTranslations(request.items, lines))
    }
}

class DeepLSubtitleTranslationProvider(
    private val config: SubtitleTranslationConfig,
    private val client: HttpClient = defaultSubtitleHttpClient()
) : SubtitleTranslationProvider {
    override suspend fun translate(request: SubtitleTranslationRequest): SubtitleTranslationResult {
        require(config.deepLApiKey.isNotBlank()) { "DeepL API Key 不能为空" }
        val endpoint = config.deepLEndpoint.trimEnd('/')
        val targetCode = request.targetLanguage.providerCode(SubtitleTranslationProviderType.DeepL)
        val textFields = request.items.joinToString("&") {
            "text=${it.text.encodeURLParameter()}"
        }
        val response = client.post("$endpoint/v2/translate") {
            contentType(ContentType.Application.FormUrlEncoded)
            header("Authorization", "DeepL-Auth-Key ${config.deepLApiKey}")
            setBody("$textFields&target_lang=$targetCode")
        }.bodyAsText()
        val lines = json.parseToJsonElement(response).jsonObject["translations"]
            ?.jsonArray
            ?.mapNotNull { it.jsonObject["text"]?.jsonPrimitive?.contentOrNull }
            .orEmpty()
        return SubtitleTranslationResult(parseTabLineTranslations(request.items, lines))
    }
}

class DeepLXSubtitleTranslationProvider(
    private val config: SubtitleTranslationConfig,
    private val client: HttpClient = defaultSubtitleHttpClient()
) : SubtitleTranslationProvider {
    override suspend fun translate(request: SubtitleTranslationRequest): SubtitleTranslationResult {
        require(config.deepLXEndpoint.isNotBlank()) { "DeepLX Endpoint 不能为空" }
        val body = buildJsonObject {
            put("source_lang", JsonPrimitive("auto"))
            put("target_lang", JsonPrimitive(request.targetLanguage.providerCode(SubtitleTranslationProviderType.DeepLX)))
            put("text", JsonPrimitive(request.items.joinToString("\n") { it.text }))
        }
        val response = client.post(config.deepLXEndpoint.trimEnd('/').joinUrlPath("translate")) {
            contentType(ContentType.Application.Json)
            if (config.deepLXApiKey.isNotBlank()) header("Authorization", "Bearer ${config.deepLXApiKey}")
            setBody(body.toString())
        }.bodyAsText()
        val resultText = json.parseToJsonElement(response).jsonObject["data"]?.jsonPrimitive?.contentOrNull
            ?: json.parseToJsonElement(response).jsonObject["text"]?.jsonPrimitive?.contentOrNull
            ?: ""
        return SubtitleTranslationResult(parseTabLineTranslations(request.items, resultText.lines()))
    }
}

suspend fun testSubtitleTranslationConnection(
    config: SubtitleTranslationConfig,
    testSentence: String = DefaultSubtitleTranslationTestSentence
): Result<String> {
    return runCatching {
        val sanitized = config.sanitized
        val target = SubtitleLanguages.find(sanitized.targetLanguage)
        val provider = SubtitleTranslationProviderFactory.create(sanitized)
        val sentence = testSentence.trim().ifBlank { DefaultSubtitleTranslationTestSentence }
        val result = provider.translate(
            SubtitleTranslationRequest(
                title = "",
                targetLanguage = target,
                context = "",
                items = listOf(SubtitleTranslationItem(1, sentence))
            )
        )
        result.translations[1].orEmpty().trim()
            .also { require(it.isNotBlank()) { "测试返回为空" } }
    }
}

const val DefaultSubtitleTranslationTestSentence: String = "我今天想练习英语听力。"

fun parseIdTextTranslations(raw: String): Map<Int, String> {
    val cleaned = raw.trim()
        .removePrefix("```json").removePrefix("```")
        .removeSuffix("```")
        .trim()
    val element = json.parseToJsonElement(cleaned)
    val array = when (element) {
        is JsonArray -> element
        is JsonObject -> element["translations"]?.jsonArray
            ?: element["result"]?.jsonArray
            ?: error("翻译结果不是数组")
        else -> error("翻译结果格式错误")
    }
    return array.mapNotNull { item ->
        val obj = item.jsonObject
        val id = obj["id"]?.jsonPrimitive?.contentOrNull?.toIntOrNull()
            ?: obj["index"]?.jsonPrimitive?.contentOrNull?.toIntOrNull()
            ?: return@mapNotNull null
        val text = obj["text"]?.jsonPrimitive?.contentOrNull
            ?: obj["translation"]?.jsonPrimitive?.contentOrNull
            ?: return@mapNotNull null
        id to cleanTranslatedSubtitleText(text, expectedPrefixes = listOf(id, id + 1))
    }.toMap()
}

fun parseTabLineTranslations(
    sourceItems: List<SubtitleTranslationItem>,
    translatedLines: List<String>
): Map<Int, String> {
    return sourceItems.mapIndexedNotNull { index, item ->
        val raw = translatedLines.getOrNull(index).orEmpty()
        val text = cleanTranslatedSubtitleText(
            raw = raw,
            expectedPrefixes = listOf(item.id, item.id + 1, index, index + 1)
        )
        if (text.isBlank()) null else item.id to text
    }.toMap()
}

fun cleanTranslatedSubtitleText(
    raw: String,
    expectedPrefixes: List<Int> = emptyList()
): String {
    val text = raw
        .substringAfter('\t', raw)
        .trim()
    val prefixes = expectedPrefixes
        .filter { it >= 0 }
        .distinct()
        .joinToString("|") { Regex.escape(it.toString()) }
    if (prefixes.isBlank()) return text
    val prefixRegex = Regex(
        """^\s*(?:[\[\(（【]\s*(?:$prefixes)\s*[\]\)）】]|(?:$prefixes))\s*(?:\t+|[、,，:：)）\]】\-–—]\s*|[.。](?!\d)\s*|\s+)"""
    )
    return text.replace(prefixRegex, "").trim()
}

private fun defaultSubtitleHttpClient(): HttpClient {
    return HttpClient(OkHttp) {
        install(HttpTimeout) {
            requestTimeoutMillis = 15000
            connectTimeoutMillis = 10000
            socketTimeoutMillis = 15000
        }
    }
}

private fun List<SubtitleTranslationItem>.toJsonArray(): JsonArray {
    return buildJsonArray {
        this@toJsonArray.forEach { item ->
            addJsonObject {
                put("id", JsonPrimitive(item.id))
                put("text", JsonPrimitive(item.text))
            }
        }
    }
}

private fun List<Pair<String, String>>.formBody(): String {
    return joinToString("&") { (key, value) ->
        "${key.encodeURLParameter()}=${value.encodeURLParameter()}"
    }
}

private fun String.joinUrlPath(path: String): String {
    val base = trimEnd('/')
    return URLBuilder(base).apply {
        appendPathSegments(path.split('/').filter { it.isNotBlank() })
    }.buildString()
}

private fun md5(value: String): String {
    val bytes = MessageDigest.getInstance("MD5").digest(value.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}

private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
}
