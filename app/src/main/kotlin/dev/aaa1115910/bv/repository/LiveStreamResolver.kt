package dev.aaa1115910.bv.repository

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

data class LiveLineOption(
    val label: String,
    val url: String
)

data class LiveQualityOption(
    val qn: Int,
    val desc: String
)

data class LivePlaybackSource(
    val playUrl: String,
    val lines: List<LiveLineOption>,
    val currentLineIndex: Int,
    val qualities: List<LiveQualityOption>,
    val currentQuality: Int
)

object LiveStreamResolver {
    private val preferredProtocols = listOf("http_hls", "http_stream")

    private data class LiveRouteCandidate(
        val routeKey: String,
        val routeIndex: Int,
        val url: String,
        val score: Int
    )

    fun resolveRoomId(roomInfo: JsonObject?, fallbackRoomId: Int): Int {
        return roomInfo
            ?.get("room_id")
            ?.jsonPrimitive
            ?.intOrNull
            ?.takeIf { it > 0 }
            ?: fallbackRoomId
    }

    fun resolvePlayableUrl(playInfo: JsonObject): String? {
        return resolvePlayableSource(playInfo)?.playUrl
    }

    fun resolvePlayableSource(
        playInfo: JsonObject,
        requestedLineIndex: Int = 0
    ): LivePlaybackSource? {
        val lines = resolveLines(playInfo)
        val qualities = resolveQualities(playInfo)
        val currentQuality = playInfo["current_quality"]?.jsonPrimitive?.intOrNull
            ?: playInfo["playurl_info"]
                .asJsonObjectOrNull()
                ?.get("playurl")
                .asJsonObjectOrNull()
                ?.get("stream")
                .asJsonArrayOrNull()
                ?.firstOrNull()
                .asJsonObjectOrNull()
                ?.get("format")
                .asJsonArrayOrNull()
                ?.firstOrNull()
                .asJsonObjectOrNull()
                ?.get("codec")
                .asJsonArrayOrNull()
                ?.firstOrNull()
                .asJsonObjectOrNull()
                ?.get("current_qn")
                ?.jsonPrimitive
                ?.intOrNull
            ?: qualities.firstOrNull()?.qn
            ?: 0

        if (lines.isNotEmpty()) {
            val lineIndex = requestedLineIndex.coerceIn(0, lines.lastIndex)
            return LivePlaybackSource(
                playUrl = lines[lineIndex].url,
                lines = lines,
                currentLineIndex = lineIndex,
                qualities = qualities,
                currentQuality = currentQuality
            )
        }

        val fallbackUrl = playInfo["durl"]
            .asJsonArrayOrNull()
            ?.firstOrNull()
            .asJsonObjectOrNull()
            ?.get("url")
            ?.jsonPrimitive
            ?.contentOrNull
            ?: return null

        val fallbackLines = listOf(LiveLineOption(label = "默认线路", url = fallbackUrl))
        return LivePlaybackSource(
            playUrl = fallbackUrl,
            lines = fallbackLines,
            currentLineIndex = 0,
            qualities = qualities,
            currentQuality = currentQuality
        )
    }

    private fun resolveLines(playInfo: JsonObject): List<LiveLineOption> {
        val playUrlInfo = playInfo["playurl_info"].asJsonObjectOrNull()
        val playUrl = playUrlInfo?.get("playurl").asJsonObjectOrNull()
        val streams = playUrl?.get("stream").asJsonArrayOrNull() ?: JsonArray(emptyList())

        val lines = preferredProtocols.firstNotNullOfOrNull { protocolName ->
            val stream = streams.firstOrNull {
                it.asJsonObjectOrNull()?.get("protocol_name")?.jsonPrimitive?.contentOrNull == protocolName
            }.asJsonObjectOrNull() ?: return@firstNotNullOfOrNull null

            resolveFromFormats(
                formats = stream["format"].asJsonArrayOrNull() ?: JsonArray(emptyList()),
                preferM3u8 = protocolName == "http_hls"
            ).takeIf { it.isNotEmpty() }
        }
            ?: emptyList()

        return lines.mapIndexed { index, url ->
            LiveLineOption(label = "线路${index + 1}", url = url)
        }
    }

    private fun resolveQualities(playInfo: JsonObject): List<LiveQualityOption> {
        val availableQns = resolveAvailableQualityQns(playInfo)
        val qualityElements = playInfo["quality_description"].asJsonArrayOrNull()
            ?: playInfo["playurl_info"]
                .asJsonObjectOrNull()
                ?.get("playurl")
                .asJsonObjectOrNull()
                ?.get("g_qn_desc")
                .asJsonArrayOrNull()
            ?: JsonArray(emptyList())

        return qualityElements.mapNotNull { qualityElement ->
            val obj = qualityElement.asJsonObjectOrNull() ?: return@mapNotNull null
            val qn = obj["qn"]?.jsonPrimitive?.intOrNull ?: return@mapNotNull null
            val desc = obj["desc"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            if (availableQns.isNotEmpty() && qn !in availableQns) return@mapNotNull null
            desc.takeIf { it.isNotBlank() && it != "默认" }?.let {
                LiveQualityOption(qn = qn, desc = normalizeLiveQualityDesc(qn = qn, desc = it))
            }
        }
    }

    internal fun normalizeLiveQualityDesc(qn: Int, desc: String): String {
        return when (qn) {
            25000 -> "1080P高码率"
            10000 -> "1080P原画"
            400 -> "1080P蓝光"
            250 -> "720P超清"
            150 -> "高清"
            80 -> "流畅"
            else -> desc
        }
    }

    private fun resolveAvailableQualityQns(playInfo: JsonObject): Set<Int> {
        val streams = playInfo["playurl_info"]
            .asJsonObjectOrNull()
            ?.get("playurl")
            .asJsonObjectOrNull()
            ?.get("stream")
            .asJsonArrayOrNull()
            ?: JsonArray(emptyList())

        preferredProtocols.forEach { protocolName ->
            val stream = streams.firstOrNull {
                it.asJsonObjectOrNull()?.get("protocol_name")?.jsonPrimitive?.contentOrNull == protocolName
            }.asJsonObjectOrNull() ?: return@forEach

            val result = linkedSetOf<Int>()
            var hasExplicitAcceptedQns = false
            val formats = stream["format"].asJsonArrayOrNull() ?: JsonArray(emptyList())
            formats.forEach { formatElement ->
                val format = formatElement.asJsonObjectOrNull() ?: return@forEach
                val codecs = format["codec"].asJsonArrayOrNull() ?: JsonArray(emptyList())
                codecs.forEach { codecElement ->
                    val codec = codecElement.asJsonObjectOrNull() ?: return@forEach
                    val acceptedQns = codec["accept_qn"].asJsonArrayOrNull()
                    if (acceptedQns != null) {
                        hasExplicitAcceptedQns = true
                    }
                    acceptedQns?.forEach { qnElement ->
                        qnElement.jsonPrimitive.intOrNull?.let { result += it }
                    }
                }
            }
            if (hasExplicitAcceptedQns && result.isNotEmpty()) {
                return result
            }
        }

        return emptySet()
    }

    private fun resolveFromFormats(
        formats: JsonArray,
        preferM3u8: Boolean
    ): List<String> {
        val candidatesByRoute = linkedMapOf<String, LiveRouteCandidate>()
        formats.forEach { formatElement ->
            val format = formatElement.asJsonObjectOrNull() ?: return@forEach
            val formatName = format["format_name"]?.jsonPrimitive?.contentOrNull.orEmpty()
            val codecs = format["codec"].asJsonArrayOrNull() ?: JsonArray(emptyList())
            codecs.forEach { codecElement ->
                val codec = codecElement.asJsonObjectOrNull() ?: return@forEach
                val codecName = codec["codec_name"]?.jsonPrimitive?.contentOrNull.orEmpty()
                val baseUrl = codec["base_url"]?.jsonPrimitive?.contentOrNull ?: return@forEach
                val infos = codec["url_info"].asJsonArrayOrNull() ?: JsonArray(emptyList())
                infos.forEachIndexed { routeIndex, infoElement ->
                    val info = infoElement.asJsonObjectOrNull() ?: return@forEachIndexed
                    val host = info["host"]?.jsonPrimitive?.contentOrNull ?: return@forEachIndexed
                    val extra = info["extra"]?.jsonPrimitive?.contentOrNull.orEmpty()
                    val candidate = LiveRouteCandidate(
                        routeKey = "$routeIndex|$host",
                        routeIndex = routeIndex,
                        url = "$host$baseUrl$extra",
                        score = resolveRouteCandidateScore(
                            formatName = formatName,
                            codecName = codecName,
                            baseUrl = baseUrl,
                            preferM3u8 = preferM3u8
                        )
                    )
                    val existing = candidatesByRoute[candidate.routeKey]
                    if (existing == null || candidate.score < existing.score) {
                        candidatesByRoute[candidate.routeKey] = candidate
                    }
                }
            }
        }

        val urls = candidatesByRoute.values
            .sortedWith(compareBy<LiveRouteCandidate> { it.routeIndex }.thenBy { it.score })
            .map { it.url }
        val stableUrls = urls.filterNot { it.contains("mcdn", ignoreCase = true) }
        return stableUrls.ifEmpty { urls }
    }

    private fun resolveRouteCandidateScore(
        formatName: String,
        codecName: String,
        baseUrl: String,
        preferM3u8: Boolean
    ): Int {
        val normalizedFormat = formatName.lowercase()
        val normalizedCodec = codecName.lowercase()
        var score = 0

        if (preferM3u8) {
            score += when (normalizedFormat) {
                "ts" -> 0
                "fmp4" -> 10
                else -> 20
            }
            score += if (baseUrl.contains("index.m3u8", ignoreCase = true)) 0 else 1
        }

        score += when (normalizedCodec) {
            "avc" -> 0
            "hevc" -> 5
            else -> 10
        }

        return score
    }
}

private fun JsonElement?.asJsonObjectOrNull(): JsonObject? = this as? JsonObject

private fun JsonElement?.asJsonArrayOrNull(): JsonArray? = this as? JsonArray
