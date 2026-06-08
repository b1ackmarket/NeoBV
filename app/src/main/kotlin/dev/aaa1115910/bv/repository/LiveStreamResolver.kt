package dev.aaa1115910.bv.repository

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

data class LiveLineOption(
    val label: String,
    val url: String,
    val qn: Int = 0,
    val codecName: String = "",
    val sourceProtocol: String = "",
    val sourceFormat: String = "",
    val sourceType: String = "",
    val container: String = ""
)

data class LiveQualityOption(
    val qn: Int,
    val desc: String,
    val tags: List<String> = emptyList(),
    val playbackDesc: String = ""
)

data class LiveMasterVariant(
    val qn: Int,
    val display: String,
    val width: Int,
    val height: Int,
    val frameRate: Float = 0f,
    val bandwidth: Long,
    val codecs: String,
    val stream: String,
    val url: String
) {
    val inferredDisplayName: String
        get() = buildPlaybackQualityLabel(
            baseName = display.ifBlank { qn.takeIf { it > 0 }?.toString().orEmpty() },
            tags = emptyList(),
            width = width,
            height = height
        )
}

data class LivePlaybackSource(
    val playUrl: String,
    val lines: List<LiveLineOption>,
    val currentLineIndex: Int,
    val qualities: List<LiveQualityOption>,
    val currentQuality: Int,
    val masterRequestedQn: Int = 0,
    val masterVariants: List<LiveMasterVariant> = emptyList()
)

object LiveStreamResolver {
    private const val QnOriginal = 10000
    private const val QnHighBitrate = 25000
    private val defaultPreferredProtocols = listOf("http_hls", "http_stream")
    private val liveQualityOrder = listOf(30000, 25000, 20000, 15000, 10000, 400, 250, 150, 80)

    private data class LiveRouteCandidate(
        val routeKey: String,
        val routeIndex: Int,
        val url: String,
        val score: Int,
        val codecName: String,
        val protocolName: String,
        val formatName: String,
        val acceptQns: Set<Int>,
        val currentQn: Int
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
        requestedLineIndex: Int = 0,
        masterVariants: List<LiveMasterVariant> = emptyList(),
        masterRequestedQn: Int = 0,
        preferHighBitrate: Boolean = false
    ): LivePlaybackSource? {
        val protocolOrder = if (preferHighBitrate) {
            listOf("http_hls", "http_stream")
        } else {
            defaultPreferredProtocols
        }
        val qualities = resolveQualities(playInfo, protocolOrder)
        val reportedCurrentQuality = resolveReportedCurrentQuality(playInfo, qualities)
        val requestedQn = masterRequestedQn.takeIf { it > 0 } ?: reportedCurrentQuality
        val requestedSupportedLines = if (requestedQn > 0) {
            resolveLines(
                playInfo = playInfo,
                protocolOrder = protocolOrder,
                preferHighBitrate = preferHighBitrate,
                requestedQn = requestedQn,
                strictRequestedQn = true
            )
        } else {
            emptyList()
        }
        val requestedUnsupportedLines = if (requestedQn > 0) {
            resolveLines(
                playInfo = playInfo,
                protocolOrder = protocolOrder,
                preferHighBitrate = preferHighBitrate,
                requestedQn = requestedQn,
                includeUnsupportedLiveFlvForRequestedQn = true,
                strictRequestedQn = true
            )
        } else {
            emptyList()
        }
        val targetQn = resolveTargetQn(
            requestedQn = requestedQn,
            availableQns = resolveActualCurrentQns(playInfo, protocolOrder),
            fallbackQn = reportedCurrentQuality
        )
        val lines = requestedSupportedLines
            .ifEmpty {
                if (requestedUnsupportedLines.isNotEmpty()) {
                    resolveMasterVariantLines(masterVariants, requestedQn)
                        .ifEmpty { requestedUnsupportedLines }
                } else {
                    emptyList()
                }
            }
            .ifEmpty {
                resolveLines(
                    playInfo = playInfo,
                    protocolOrder = protocolOrder,
                    preferHighBitrate = preferHighBitrate,
                    requestedQn = targetQn
                )
            }

        if (lines.isNotEmpty()) {
            val lineIndex = requestedLineIndex.coerceIn(0, lines.lastIndex)
            val lineUrl = lines[lineIndex].url
            return LivePlaybackSource(
                playUrl = lineUrl,
                lines = lines,
                currentLineIndex = lineIndex,
                qualities = qualities,
                currentQuality = resolveSelectedCurrentQuality(
                    playUrl = lineUrl,
                    selectedLineQn = lines[lineIndex].qn,
                    reportedCurrentQuality = reportedCurrentQuality
                ),
                masterRequestedQn = masterRequestedQn,
                masterVariants = masterVariants
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

        val fallbackLines = listOf(
            LiveLineOption(
                label = "默认线路（${fallbackUrl.toLiveSourceType()}）",
                url = fallbackUrl,
                sourceType = fallbackUrl.toLiveSourceType(),
                container = fallbackUrl.toLiveContainer()
            )
        )
        return LivePlaybackSource(
            playUrl = fallbackUrl,
            lines = fallbackLines,
            currentLineIndex = 0,
            qualities = qualities,
            currentQuality = resolveSelectedCurrentQuality(
                playUrl = fallbackUrl,
                selectedLineQn = 0,
                reportedCurrentQuality = reportedCurrentQuality
            ),
            masterRequestedQn = masterRequestedQn,
            masterVariants = masterVariants
        )
    }

    fun parseMasterPlaylist(masterPlaylist: String): List<LiveMasterVariant> {
        val lines = masterPlaylist.lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toList()
        val variants = mutableListOf<LiveMasterVariant>()
        lines.forEachIndexed { index, line ->
            if (!line.startsWith("#EXT-X-STREAM-INF:", ignoreCase = true)) return@forEachIndexed
            val attrs = parseM3u8Attributes(line.substringAfter(':'))
            val qn = attrs["BILI-QN"]?.toIntOrNull() ?: 0
            val display = attrs["BILI-DISPLAY"].orEmpty()
            val (width, height) = parseResolution(attrs["RESOLUTION"].orEmpty())
            variants += LiveMasterVariant(
                qn = qn,
                display = display,
                width = width,
                height = height,
                frameRate = attrs["FRAME-RATE"]?.toFloatOrNull() ?: 0f,
                bandwidth = attrs["BANDWIDTH"]?.toLongOrNull() ?: 0L,
                codecs = attrs["CODECS"].orEmpty(),
                stream = attrs["BILI-STREAM"].orEmpty(),
                url = lines.drop(index + 1)
                    .firstOrNull { it.isNotBlank() && !it.startsWith("#") }
                    .orEmpty()
            )
        }
        return variants
            .distinctBy {
                listOf(
                    it.qn,
                    it.width,
                    it.height,
                    it.frameRate,
                    it.bandwidth,
                    it.codecs,
                    it.stream
                )
            }
            .sortedWith(
                compareByDescending<LiveMasterVariant> { it.height * it.width }
                    .thenByDescending { it.bandwidth }
                    .thenByDescending { it.qn }
            )
    }

    private fun resolveLines(
        playInfo: JsonObject,
        protocolOrder: List<String>,
        preferHighBitrate: Boolean,
        requestedQn: Int,
        includeUnsupportedLiveFlvForRequestedQn: Boolean = false,
        strictRequestedQn: Boolean = false
    ): List<LiveLineOption> {
        val playUrlInfo = playInfo["playurl_info"].asJsonObjectOrNull()
        val playUrl = playUrlInfo?.get("playurl").asJsonObjectOrNull()
        val streams = playUrl?.get("stream").asJsonArrayOrNull() ?: JsonArray(emptyList())

        val lines = protocolOrder.firstNotNullOfOrNull { protocolName ->
            val stream = streams.firstOrNull {
                it.asJsonObjectOrNull()?.get("protocol_name")?.jsonPrimitive?.contentOrNull == protocolName
            }.asJsonObjectOrNull() ?: return@firstNotNullOfOrNull null

            resolveFromFormats(
                formats = stream["format"].asJsonArrayOrNull() ?: JsonArray(emptyList()),
                protocolName = protocolName,
                preferM3u8 = protocolName == "http_hls",
                preferHighBitrate = preferHighBitrate,
                requestedQn = requestedQn,
                includeUnsupportedLiveFlvForRequestedQn = includeUnsupportedLiveFlvForRequestedQn,
                strictRequestedQn = strictRequestedQn
            ).takeIf { it.isNotEmpty() }
        }
            ?: emptyList()

        return lines.mapIndexed { index, candidate ->
            val sourceType = buildLiveSourceType(
                protocolName = candidate.protocolName,
                formatName = candidate.formatName,
                url = candidate.url
            )
            val boostSuffix = candidate.url.toLiveBoostSuffix()
            LiveLineOption(
                label = "线路${index + 1}（$sourceType$boostSuffix）",
                url = candidate.url,
                qn = candidate.currentQn,
                codecName = candidate.codecName,
                sourceProtocol = candidate.protocolName,
                sourceFormat = candidate.formatName,
                sourceType = sourceType,
                container = candidate.url.toLiveContainer()
            )
        }
    }

    private fun resolveMasterVariantLines(
        masterVariants: List<LiveMasterVariant>,
        requestedQn: Int
    ): List<LiveLineOption> {
        if (requestedQn <= 0) return emptyList()
        return masterVariants
            .filter { it.qn == requestedQn && it.url.isNotBlank() }
            .sortedWith(
                compareByDescending<LiveMasterVariant> { it.width * it.height }
                    .thenByDescending { it.bandwidth }
            )
            .mapIndexed { index, variant ->
                val sourceType = buildLiveSourceType(
                    protocolName = "http_hls",
                    formatName = variant.url.toLiveContainer().takeIf { it == "m3u8" } ?: "",
                    url = variant.url
                )
                val boostSuffix = variant.url.toLiveBoostSuffix()
                LiveLineOption(
                    label = "线路${index + 1}（$sourceType$boostSuffix）",
                    url = variant.url,
                    qn = variant.qn,
                    codecName = variant.stream.ifBlank { variant.codecs.toLiveCodecNameOrEmpty() },
                    sourceProtocol = "http_hls",
                    sourceFormat = sourceType.substringAfter('/', missingDelimiterValue = ""),
                    sourceType = sourceType,
                    container = variant.url.toLiveContainer()
                )
            }
    }

    private fun resolveQualities(playInfo: JsonObject, protocolOrder: List<String>): List<LiveQualityOption> {
        val availableQns = resolveAvailableQualityQns(playInfo, protocolOrder)
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
            buildQualityOptionLabel(desc)?.let { label ->
                LiveQualityOption(
                    qn = qn,
                    desc = label,
                    tags = resolveLiveQualityTags(quality = obj),
                    playbackDesc = resolveLiveQualityPlaybackDesc(quality = obj)
                )
            }
        }
    }

    internal fun buildQualityOptionLabel(desc: String): String? =
        desc.trim().takeIf { it.isNotBlank() && it != "默认" }

    private fun resolveLiveQualityTags(quality: JsonObject): List<String> =
        buildList {
            quality["hdr_desc"]
                ?.jsonPrimitive
                ?.contentOrNull
                ?.takeIf { it.isNotBlank() }
                ?.let { add(it) }
            quality["attr_desc"]
                ?.jsonPrimitiveOrNull()
                ?.contentOrNull
                ?.takeIf { it.isNotBlank() }
                ?.let { add(it) }
            quality["media_base_desc"]
                .asJsonObjectOrNull()
                ?.get("detail_desc")
                .asJsonObjectOrNull()
                ?.get("tag")
                .asJsonArrayOrNull()
                ?.mapNotNull { it.jsonPrimitive.contentOrNull?.takeIf(String::isNotBlank) }
                ?.let { addAll(it) }
        }.distinct()

    private fun resolveLiveQualityPlaybackDesc(quality: JsonObject): String =
        quality["media_base_desc"]
            .asJsonObjectOrNull()
            ?.get("detail_desc")
            .asJsonObjectOrNull()
            ?.get("desc")
            ?.jsonPrimitiveOrNull()
            ?.contentOrNull
            ?.trim()
            .orEmpty()

    fun buildPlaybackQualityLabel(
        source: LivePlaybackSource,
        videoWidth: Int,
        videoHeight: Int
    ): String {
        val variant = resolveCurrentMasterVariant(
            source = source,
            videoWidth = videoWidth,
            videoHeight = videoHeight
        )
        val quality = source.qualities.firstOrNull { it.qn == source.currentQuality }
        val baseName = resolvePlaybackBaseName(
            source = source,
            qn = source.currentQuality,
            variantDisplay = variant?.display,
            officialName = quality?.desc
        )
        val width = videoWidth.takeIf { it > 0 } ?: variant?.width ?: 0
        val height = videoHeight.takeIf { it > 0 } ?: variant?.height ?: 0
        return buildPlaybackQualityLabel(
            baseName = baseName,
            tags = quality?.tags.orEmpty(),
            width = width,
            height = height
        )
    }

    fun buildPlaybackQualityLabel(
        source: LivePlaybackSource,
        quality: LiveQualityOption
    ): String {
        val variant = resolveMasterVariantForQuality(source, quality.qn)
        val baseName = resolvePlaybackBaseName(
            source = source,
            qn = quality.qn,
            variantDisplay = variant?.display,
            officialName = quality.desc
        )
        val fallbackPrefix = if (variant == null) {
            quality.playbackDesc.toResolutionPrefixOrNull()
        } else {
            null
        }
        return buildPlaybackQualityLabel(
            baseName = baseName,
            tags = quality.tags,
            width = variant?.width ?: 0,
            height = variant?.height ?: 0,
            prefixOverride = fallbackPrefix
        )
    }

    fun resolveCurrentMasterVariant(
        source: LivePlaybackSource,
        videoWidth: Int,
        videoHeight: Int
    ): LiveMasterVariant? {
        if (source.masterVariants.isEmpty()) return null
        val currentQns = listOfNotNull(
            source.playUrl.toQueryParamOrNull("qn")?.toIntOrNull(),
            source.currentQuality.takeIf { it > 0 }
        ).distinct()
        if (currentQns.isNotEmpty()) {
            val qnMatchedVariants = source.masterVariants.filter { it.qn in currentQns }
            if (videoWidth > 0 && videoHeight > 0) {
                qnMatchedVariants.firstOrNull { variant ->
                    variant.width == videoWidth && variant.height == videoHeight
                }?.let { return it }
            }
            qnMatchedVariants.firstOrNull()?.let { return it }
        }
        if (videoWidth > 0 && videoHeight > 0) {
            source.masterVariants.firstOrNull { variant ->
                variant.width == videoWidth && variant.height == videoHeight
            }?.let { return it }
        }
        return null
    }

    private fun resolveMasterVariantForQuality(
        source: LivePlaybackSource,
        qn: Int
    ): LiveMasterVariant? {
        if (source.masterVariants.isEmpty() || qn <= 0) return null
        return source.masterVariants
            .filter { it.qn == qn }
            .maxWithOrNull(
                compareBy<LiveMasterVariant> { it.width * it.height }
                    .thenBy { it.bandwidth }
            )
    }

    private fun resolvePlaybackBaseName(
        source: LivePlaybackSource,
        qn: Int,
        variantDisplay: String?,
        officialName: String?
    ): String {
        if (qn == QnOriginal && source.hasHighBitrateQuality()) return "高码率"
        return variantDisplay
            ?.takeIf { it.isNotBlank() }
            ?: officialName?.takeIf { it.isNotBlank() }
            ?: qn.takeIf { it > 0 }?.toString()
            ?: ""
    }

    private fun LivePlaybackSource.hasHighBitrateQuality(): Boolean =
        qualities.any { it.qn == QnHighBitrate } ||
            masterVariants.any { it.qn == QnHighBitrate }

    private fun resolveReportedCurrentQuality(playInfo: JsonObject, qualities: List<LiveQualityOption>): Int =
        playInfo["current_quality"]?.jsonPrimitive?.intOrNull
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

    private fun resolveSelectedCurrentQuality(
        playUrl: String,
        selectedLineQn: Int,
        reportedCurrentQuality: Int
    ): Int =
        playUrl.toQueryParamOrNull("qn")?.toIntOrNull()?.takeIf { it > 0 }
            ?: selectedLineQn.takeIf { it > 0 }
            ?: reportedCurrentQuality

    private fun resolveTargetQn(
        requestedQn: Int,
        availableQns: Set<Int>,
        fallbackQn: Int
    ): Int {
        if (requestedQn <= 0) return fallbackQn
        if (availableQns.isEmpty() || requestedQn in availableQns) return requestedQn
        val orderedAvailable = liveQualityOrder.filter { it in availableQns }
        if (orderedAvailable.isEmpty()) return fallbackQn
        val requestedRank = liveQualityOrder.indexOf(requestedQn).takeIf { it >= 0 }
            ?: return orderedAvailable.first()
        return orderedAvailable.firstOrNull { qn ->
            val rank = liveQualityOrder.indexOf(qn)
            rank >= requestedRank
        } ?: orderedAvailable.last()
    }

    private fun resolveActualCurrentQns(playInfo: JsonObject, protocolOrder: List<String>): Set<Int> {
        val streams = playInfo["playurl_info"]
            .asJsonObjectOrNull()
            ?.get("playurl")
            .asJsonObjectOrNull()
            ?.get("stream")
            .asJsonArrayOrNull()
            ?: JsonArray(emptyList())

        return protocolOrder.firstNotNullOfOrNull { protocolName ->
            val stream = streams.firstOrNull {
                it.asJsonObjectOrNull()?.get("protocol_name")?.jsonPrimitive?.contentOrNull == protocolName
            }.asJsonObjectOrNull() ?: return@firstNotNullOfOrNull null

            val result = linkedSetOf<Int>()
            val formats = stream["format"].asJsonArrayOrNull() ?: JsonArray(emptyList())
            formats.forEach { formatElement ->
                val format = formatElement.asJsonObjectOrNull() ?: return@forEach
                val codecs = format["codec"].asJsonArrayOrNull() ?: JsonArray(emptyList())
                codecs.forEach { codecElement ->
                    codecElement.asJsonObjectOrNull()
                        ?.get("current_qn")
                        ?.jsonPrimitive
                        ?.intOrNull
                        ?.takeIf { it > 0 }
                        ?.let { result += it }
                }
            }
            result.takeIf { it.isNotEmpty() }
        } ?: emptySet()
    }

    private fun resolveAvailableQualityQns(playInfo: JsonObject, protocolOrder: List<String>): Set<Int> {
        val streams = playInfo["playurl_info"]
            .asJsonObjectOrNull()
            ?.get("playurl")
            .asJsonObjectOrNull()
            ?.get("stream")
            .asJsonArrayOrNull()
            ?: JsonArray(emptyList())

        protocolOrder.forEach { protocolName ->
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
        protocolName: String,
        preferM3u8: Boolean,
        preferHighBitrate: Boolean,
        requestedQn: Int,
        includeUnsupportedLiveFlvForRequestedQn: Boolean = false,
        strictRequestedQn: Boolean = false
    ): List<LiveRouteCandidate> {
        val candidatesByRoute = linkedMapOf<String, LiveRouteCandidate>()
        formats.forEach { formatElement ->
            val format = formatElement.asJsonObjectOrNull() ?: return@forEach
            val formatName = format["format_name"]?.jsonPrimitive?.contentOrNull.orEmpty()
            val codecs = format["codec"].asJsonArrayOrNull() ?: JsonArray(emptyList())
            codecs.forEach { codecElement ->
                val codec = codecElement.asJsonObjectOrNull() ?: return@forEach
                val codecName = codec["codec_name"]?.jsonPrimitive?.contentOrNull.orEmpty()
                val acceptQns = codec["accept_qn"]
                    .asJsonArrayOrNull()
                    ?.mapNotNull { it.jsonPrimitive.intOrNull }
                    ?.toSet()
                    .orEmpty()
                val currentQn = codec["current_qn"]?.jsonPrimitive?.intOrNull ?: 0
                val baseUrl = codec["base_url"]?.jsonPrimitive?.contentOrNull ?: return@forEach
                val isRequestedQnCandidate = requestedQn > 0 &&
                        (currentQn == requestedQn || acceptQns.contains(requestedQn))
                if (
                    isUnsupportedLiveFlvCodec(formatName = formatName, codecName = codecName, baseUrl = baseUrl) &&
                    !(includeUnsupportedLiveFlvForRequestedQn && isRequestedQnCandidate)
                ) {
                    return@forEach
                }
                val infos = codec["url_info"].asJsonArrayOrNull() ?: JsonArray(emptyList())
                infos.forEachIndexed { routeIndex, infoElement ->
                    val info = infoElement.asJsonObjectOrNull() ?: return@forEachIndexed
                    val host = info["host"]?.jsonPrimitive?.contentOrNull ?: return@forEachIndexed
                    val extra = info["extra"]?.jsonPrimitive?.contentOrNull.orEmpty()
                    val url = "$host$baseUrl$extra"
                    val candidate = LiveRouteCandidate(
                        routeKey = "$routeIndex|$host",
                        routeIndex = routeIndex,
                        url = url,
                        score = resolveRouteCandidateScore(
                            formatName = formatName,
                            codecName = codecName,
                            baseUrl = baseUrl,
                            url = url,
                            preferM3u8 = preferM3u8,
                            preferHighBitrate = preferHighBitrate,
                            acceptQns = acceptQns,
                            currentQn = currentQn,
                            requestedQn = requestedQn
                        ),
                        codecName = codecName,
                        protocolName = protocolName,
                        formatName = formatName,
                        acceptQns = acceptQns,
                        currentQn = currentQn
                    )
                    val existing = candidatesByRoute[candidate.routeKey]
                    if (existing == null || candidate.score < existing.score) {
                        candidatesByRoute[candidate.routeKey] = candidate
                    }
                }
            }
        }

        val candidates = candidatesByRoute.values.filter { candidate ->
            !strictRequestedQn ||
                requestedQn <= 0 ||
                candidate.currentQn == requestedQn ||
                candidate.url.toQueryParamOrNull("qn")?.toIntOrNull() == requestedQn
        }
        val sortedCandidates = if (preferHighBitrate) {
            candidates.sortedWith(compareBy<LiveRouteCandidate> { it.score }.thenBy { it.routeIndex })
        } else {
            candidates.sortedWith(compareBy<LiveRouteCandidate> { it.routeIndex }.thenBy { it.score })
        }
        val stableCandidates = sortedCandidates.filterNot { isKnownPcdnUrl(it.url) }
        return stableCandidates.ifEmpty { sortedCandidates }
            .distinctBy { it.url }
    }

    private fun isKnownPcdnUrl(url: String): Boolean {
        val host = runCatching { java.net.URI(url).host.orEmpty() }.getOrDefault("")
        if (url.contains("mcdn", ignoreCase = true)) return true
        if (host.endsWith("szbdyd.com", ignoreCase = true)) return true
        return host.matches(Regex("""\d{1,3}(?:\.\d{1,3}){3}"""))
    }

    private fun resolveRouteCandidateScore(
        formatName: String,
        codecName: String,
        baseUrl: String,
        url: String,
        preferM3u8: Boolean,
        preferHighBitrate: Boolean,
        acceptQns: Set<Int>,
        currentQn: Int,
        requestedQn: Int
    ): Int {
        val normalizedFormat = formatName.lowercase()
        val normalizedCodec = codecName.lowercase()
        var score = 0

        if (requestedQn > 0) {
            score += when {
                currentQn == requestedQn -> 0
                acceptQns.contains(requestedQn) -> 20_000
                else -> 100_000
            }
        }

        if (preferHighBitrate) {
            score += if (url.contains("gotcha204b", ignoreCase = true)) 0 else 500
            score += if (baseUrl.contains("/index.m3u8", ignoreCase = true)) 0 else 200
            score += when (normalizedFormat) {
                "fmp4" -> 0
                "ts" -> 50
                else -> 100
            }
        } else if (preferM3u8) {
            score += when (normalizedFormat) {
                "ts" -> 0
                "fmp4" -> 10
                else -> 20
            }
            score += if (baseUrl.contains("index.m3u8", ignoreCase = true)) 0 else 1
        }

        score += when (normalizedCodec) {
            "avc" -> 0
            "hevc" -> if (requestedQn >= 400 && !acceptQns.contains(requestedQn)) -5 else 5
            else -> 10
        }

        return score
    }

    private fun isUnsupportedLiveFlvCodec(formatName: String, codecName: String, baseUrl: String): Boolean {
        val normalizedFormat = formatName.lowercase()
        val normalizedCodec = codecName.lowercase()
        val isFlv = normalizedFormat == "flv" || baseUrl.substringBefore('?').endsWith(".flv", ignoreCase = true)
        val isExtendedCodec = normalizedCodec == "hevc" || normalizedCodec == "av1"
        return isFlv && isExtendedCodec
    }

    private fun parseResolution(value: String): Pair<Int, Int> {
        val parts = value.lowercase().split('x', limit = 2)
        return (parts.getOrNull(0)?.toIntOrNull() ?: 0) to
                (parts.getOrNull(1)?.toIntOrNull() ?: 0)
    }

    private fun parseM3u8Attributes(value: String): Map<String, String> {
        val attrs = linkedMapOf<String, String>()
        var index = 0
        while (index < value.length) {
            while (index < value.length && (value[index] == ',' || value[index].isWhitespace())) index++
            val keyStart = index
            while (index < value.length && value[index] != '=') index++
            if (index >= value.length) break
            val key = value.substring(keyStart, index).trim()
            index++
            val attrValue = if (index < value.length && value[index] == '"') {
                index++
                buildString {
                    while (index < value.length) {
                        val char = value[index++]
                        if (char == '"') break
                        append(char)
                    }
                }
            } else {
                val valueStart = index
                while (index < value.length && value[index] != ',') index++
                value.substring(valueStart, index).trim()
            }
            if (key.isNotBlank()) {
                attrs[key] = attrValue
            }
            while (index < value.length && value[index] != ',') index++
        }
        return attrs
    }
}

private fun JsonElement?.asJsonObjectOrNull(): JsonObject? = this as? JsonObject

private fun JsonElement?.asJsonArrayOrNull(): JsonArray? = this as? JsonArray

private fun JsonElement?.jsonPrimitiveOrNull(): JsonPrimitive? = this as? JsonPrimitive

private fun buildPlaybackQualityLabel(
    baseName: String,
    tags: List<String>,
    width: Int,
    height: Int,
    prefixOverride: String? = null
): String {
    val normalizedBaseName = baseName.trim().ifBlank { "-" }
    val prefix = (prefixOverride ?: resolutionPrefix(width, height))
        ?.takeUnless { normalizedBaseName.hasResolutionSemantics() }
    val mainName = listOfNotNull(prefix, normalizedBaseName)
        .joinToString(separator = " ")
    val suffixTags = tags
        .map { it.trim() }
        .filter { it.isNotBlank() && !normalizedBaseName.contains(it, ignoreCase = true) }
        .distinct()
    return if (suffixTags.isEmpty()) {
        mainName
    } else {
        "$mainName（${suffixTags.joinToString(separator = "")}）"
    }
}

private fun resolutionPrefix(width: Int, height: Int): String? {
    if (width <= 0 || height <= 0) return null
    val maxSide = maxOf(width, height)
    val minSide = minOf(width, height)
    if (height > width) {
        return when {
            maxSide >= 1920 && minSide >= 1080 -> "1080P"
            maxSide >= 1280 && minSide >= 720 -> "720P"
            maxSide >= 960 && minSide >= 540 -> "720P"
            maxSide >= 854 && minSide >= 480 -> "480P"
            else -> "360P"
        }
    }
    return when {
        maxSide >= 3840 && minSide >= 2160 -> "4K"
        maxSide >= 2560 && minSide >= 1440 -> "2K"
        maxSide >= 1920 && minSide >= 1080 -> "1080P"
        maxSide >= 1280 && minSide >= 720 -> "720P"
        maxSide >= 960 && minSide >= 540 -> "720P"
        maxSide >= 854 && minSide >= 480 -> "480P"
        else -> "360P"
    }
}

private fun String.hasResolutionSemantics(): Boolean =
    Regex("""(^|[^A-Za-z0-9])\d{2,4}P""", RegexOption.IGNORE_CASE).containsMatchIn(this) ||
        contains("2K", ignoreCase = true) ||
        contains("4K", ignoreCase = true) ||
        contains("杜比", ignoreCase = true)

private fun String.toResolutionPrefixOrNull(): String? =
    Regex("""(^|[^A-Za-z0-9])(4K|2K|\d{2,4}P)(?=$|[^A-Za-z0-9])""", RegexOption.IGNORE_CASE)
        .find(this)
        ?.groupValues
        ?.getOrNull(2)
        ?.uppercase()

private fun String.toLiveCodecNameOrEmpty(): String {
    val normalized = lowercase()
    return when {
        normalized.contains("hev1") || normalized.contains("hvc1") || normalized.contains("hevc") -> "hevc"
        normalized.contains("av01") || normalized.contains("av1") -> "av1"
        normalized.contains("avc1") || normalized.contains("avc3") || normalized.contains("avc") -> "avc"
        else -> ""
    }
}

private fun buildLiveSourceType(
    protocolName: String,
    formatName: String,
    url: String
): String {
    val normalizedProtocol = protocolName.lowercase()
    val normalizedFormat = formatName.lowercase().ifBlank { url.toLiveContainer() }
    return when (normalizedProtocol) {
        "http_hls" -> when (normalizedFormat) {
            "fmp4" -> "HLS/fMP4"
            "ts" -> "HLS/TS"
            else -> "HLS/${normalizedFormat.uppercase().ifBlank { "m3u8" }}"
        }

        "http_stream" -> when (normalizedFormat) {
            "flv" -> "Stream/FLV"
            else -> "Stream/${normalizedFormat.uppercase().ifBlank { "URL" }}"
        }

        else -> url.toLiveSourceType()
    }
}

private fun String.toLiveSourceType(): String =
    when (toLiveContainer()) {
        "m3u8" -> "HLS/m3u8"
        "flv" -> "Stream/FLV"
        else -> "URL"
    }

private fun String.toLiveContainer(): String {
    val path = substringBefore('?').substringBefore('#').lowercase()
    return when {
        path.endsWith(".m3u8") -> "m3u8"
        path.endsWith(".flv") -> "flv"
        else -> ""
    }
}

private fun String.toLiveBoostSuffix(): String =
    when {
        contains("gotcha204b", ignoreCase = true) -> " 204b"
        contains("gotcha204", ignoreCase = true) -> " 204"
        else -> ""
    }

private fun String.toQueryParamOrNull(name: String): String? {
    val query = runCatching { java.net.URI(this).rawQuery }.getOrNull()
        ?: substringAfter('?', missingDelimiterValue = "")
            .substringBefore('#')
            .takeIf { it.isNotBlank() }
        ?: return null
    return query
        .split('&')
        .firstNotNullOfOrNull { part ->
            val key = part.substringBefore('=', missingDelimiterValue = part)
            if (key == name) part.substringAfter('=', missingDelimiterValue = "") else null
        }
        ?.takeIf { it.isNotBlank() }
}
