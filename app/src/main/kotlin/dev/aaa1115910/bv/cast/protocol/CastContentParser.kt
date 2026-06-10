package dev.aaa1115910.bv.cast.protocol

import dev.aaa1115910.biliapi.util.AvBvConverter
import io.ktor.http.Parameters
import io.ktor.http.decodeURLQueryComponent
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object CastContentParser {
    private val json = Json { ignoreUnknownKeys = true }
    private val keyValueRegex = Regex(
        pattern = """(?i)(aid|avid|av|bvid|bv|cid|epid|ep_id|seasonid|season_id|roomid|room_id|seekts|seek_ts|progress|qn|quality|userDesireQn|speed|play_speed|userDesireSpeed|danmakuSwitchSave|danmakuState|danmakuStatus|danmaku_switch|dm_switch|dmSwitch|barrageSwitch|title|part_title|partTitle)["'\s:=]+([^\s"'&,<>{}\]]+)"""
    )
    private val projectionExtKeys = listOf("nva_ext", "_nva_ext_")
    private val directMediaUrlKeys = listOf("current_uri", "res_uri", "url", "play_url", "media_url", "video_url")
    private val hlsMimeHints = listOf("application/vnd.apple.mpegurl", "application/x-mpegurl")
    private val dashMimeHints = listOf("application/dash+xml")
    private val audioMimeHints = listOf("audio/")
    private val audioExtensions = listOf(".mp3", ".m4a", ".aac", ".flac", ".wav", ".ogg", ".opus", ".wma")

    fun parse(
        path: String,
        queryParameters: Parameters,
        body: String?,
        headers: Map<String, String> = emptyMap()
    ): CastContent? {
        val fields = linkedMapOf<String, String>()
        headers.forEach { (name, value) ->
            if (value.isNotBlank()) fields["header_${name.lowercase()}"] = value
        }
        queryParameters.names().forEach { name ->
            queryParameters[name]?.takeIf { it.isNotBlank() }?.let { value ->
                fields[name] = value
            }
        }

        parseQueryString(path.substringAfter("?", missingDelimiterValue = "")).forEach { (key, value) ->
            fields.putIfAbsent(key, value)
        }

        val bodyText = body?.takeIf { it.isNotBlank() } ?: ""
        if (bodyText.isNotBlank()) {
            parseBody(bodyText).forEach { (key, value) -> fields.putIfAbsent(key, value) }
        }

        if (fields.isEmpty()) return null

        normalizeAlias(fields, "avid", "aid")
        normalizeAlias(fields, "av", "aid")
        normalizeAlias(fields, "bv", "bvid")
        normalizeAlias(fields, "bd", "bvid")
        normalizeAlias(fields, "ep_id", "epid")
        normalizeAlias(fields, "eid", "epid")
        normalizeAlias(fields, "season_id", "seasonid")
        normalizeAlias(fields, "sid", "seasonid")
        normalizeAlias(fields, "room_id", "roomid")
        normalizeAlias(fields, "bili_room_id", "roomid")
        normalizeAlias(fields, "seek_ts", "seekts")
        normalizeAlias(fields, "sk", "seekts")
        normalizeAlias(fields, "progress", "seekts")
        normalizeAlias(fields, "quality", "qn")
        if (fields.firstInt("roomid") != null) {
            normalizeAlias(fields, "userDesireQn", "qn")
        } else {
            preferAlias(fields, "userDesireQn", "qn")
        }
        normalizeAlias(fields, "speed", "userDesireSpeed")
        normalizeAlias(fields, "play_speed", "userDesireSpeed")
        normalizeAlias(fields, "danmakuSwitchSave", "danmaku_enabled")
        normalizeAlias(fields, "danmakuState", "danmaku_enabled")
        normalizeAlias(fields, "danmakuStatus", "danmaku_enabled")
        normalizeAlias(fields, "danmaku_switch", "danmaku_enabled")
        normalizeAlias(fields, "dm_switch", "danmaku_enabled")
        normalizeAlias(fields, "dmSwitch", "danmaku_enabled")
        normalizeAlias(fields, "barrageSwitch", "danmaku_enabled")
        normalizeAlias(fields, "partTitle", "part_title")

        val directMediaUrl = fields.firstDirectMediaUrl()
        val bvid = fields.firstString("bvid")?.takeIf { it.startsWith("BV", ignoreCase = true) }
        val aid = fields.firstLong("aid") ?: bvid?.let { runCatching { AvBvConverter.bv2av(it) }.getOrNull() }
        val roomId = fields.firstInt("roomid")?.takeIf { it > 0 }
        val content = CastContent(
            aid = aid?.takeIf { it > 0L },
            bvid = bvid,
            cid = fields.firstLong("cid")?.takeIf { it > 0L },
            epid = fields.firstInt("epid")?.takeIf { it > 0 },
            seasonId = fields.firstInt("seasonid")?.takeIf { it > 0 },
            roomId = roomId,
            seekSeconds = fields.firstInt("seekts")?.takeIf { it >= 0 },
            quality = fields.firstInt("qn")?.takeIf { roomId == null && it > 0 },
            playSpeed = fields.firstFloat("userDesireSpeed")?.takeIf { it > 0f },
            danmakuEnabled = fields.firstBoolean("danmaku_enabled"),
            title = fields.firstString("title")?.decodeLoose(),
            partTitle = fields.firstString("part_title")?.decodeLoose(),
            directMediaUrl = directMediaUrl,
            directMediaType = fields.resolveDirectMediaType(directMediaUrl),
            directMediaCover = fields.firstString("album_art_uri")?.decodeLoose()?.toHttpMediaUrlOrNull(),
            creator = (fields.firstString("artist") ?: fields.firstString("creator"))?.decodeLoose(),
            clientHint = fields.resolveClientHint(directMediaUrl),
            rawFields = fields.toMap()
        )
        return content.takeIf { it.hasVideoIdentity || it.hasLiveIdentity || it.hasDirectMedia }
    }

    private fun parseBody(body: String): Map<String, String> {
        val trimmed = body.trim()
        val result = linkedMapOf<String, String>()

        if (trimmed.startsWith("{")) {
            runCatching { json.parseToJsonElement(trimmed) }
                .getOrNull()
                ?.let { collectJsonFields(it, result) }
        }

        parseQueryString(trimmed).forEach { (key, value) -> result.putIfAbsent(key, value) }
        parseProjectionSoapBody(body, result)
        projectionExtKeys.forEach { key ->
            result.entries.firstOrNull { it.key.equals(key, ignoreCase = true) }?.value
                ?.let { collectNvaExtFields(it, result) }
        }

        keyValueRegex.findAll(body).forEach { match ->
            val key = match.groupValues[1]
            val value = match.groupValues[2].trim().trim(',', ';')
            if (value.isNotBlank()) result.putIfAbsent(key, value)
        }

        return result
    }

    private fun parseProjectionSoapBody(body: String, result: MutableMap<String, String>) {
        extractXmlText(body, "CurrentURI")
            ?.decodeXmlEntities()
            ?.let { uri ->
                result.putIfAbsent("current_uri", uri)
                collectProjectionUriFields(uri, result)
            }

        extractXmlText(body, "CurrentURIMetaData")
            ?.decodeXmlEntities()
            ?.let { metadata -> collectDidlFields(metadata, result) }
    }

    private fun collectProjectionUriFields(uri: String, result: MutableMap<String, String>) {
        parseQueryString(uri.substringAfter("?", missingDelimiterValue = ""))
            .forEach { (key, value) -> result.putIfAbsent(key, value) }
    }

    private fun collectDidlFields(metadata: String, result: MutableMap<String, String>) {
        val didl = metadata.decodeXmlEntities()
        extractXmlText(didl, "title")
            ?.decodeXmlEntities()
            ?.takeIf { it.isNotBlank() }
            ?.let { result.putIfAbsent("title", it) }
        extractXmlText(didl, "creator")
            ?.decodeXmlEntities()
            ?.takeIf { it.isNotBlank() }
            ?.let { result.putIfAbsent("creator", it) }
        extractXmlText(didl, "artist")
            ?.decodeXmlEntities()
            ?.takeIf { it.isNotBlank() }
            ?.let { result.putIfAbsent("artist", it) }
        extractXmlText(didl, "album")
            ?.decodeXmlEntities()
            ?.takeIf { it.isNotBlank() }
            ?.let { result.putIfAbsent("album", it) }
        extractXmlText(didl, "albumArtURI")
            ?.decodeXmlEntities()
            ?.takeIf { it.isNotBlank() }
            ?.let { result.putIfAbsent("album_art_uri", it) }
        Regex("""(?is)<(?:[A-Za-z0-9_.-]+:)?albumArtURI\b([^>]*)/>""")
            .find(didl)
            ?.groupValues
            ?.getOrNull(1)
            ?.let { attrs ->
                Regex("""(?i)\b(?:src|url|href)\s*=\s*["']([^"']+)["']""")
                    .find(attrs)
                    ?.groupValues
                    ?.getOrNull(1)
                    ?.decodeXmlEntities()
                    ?.takeIf { it.isNotBlank() }
                    ?.let { result.putIfAbsent("album_art_uri", it) }
            }

        Regex(
            pattern = """(?is)<(?:[A-Za-z0-9_.-]+:)?res\b([^>]*)>(.*?)</(?:[A-Za-z0-9_.-]+:)?res>"""
        ).find(didl)?.let { match ->
            val attrs = match.groupValues.getOrNull(1).orEmpty()
            val uri = match.groupValues.getOrNull(2).orEmpty().decodeXmlEntities()
            result.putIfAbsent("res_uri", uri)
            collectProjectionUriFields(uri, result)
            Regex("""(?i)\bprotocolInfo\s*=\s*["']([^"']+)["']""")
                .find(attrs)
                ?.groupValues
                ?.getOrNull(1)
                ?.decodeXmlEntities()
                ?.let { result.putIfAbsent("protocol_info", it) }
        }

        extractXmlText(didl, "longDescription")
            ?.decodeXmlEntities()
            ?.let { collectNvaExtFields(it, result) }
    }

    private fun collectNvaExtFields(value: String, result: MutableMap<String, String>) {
        val decodedValue = value.decodeLoose().trim()
        val jsonText = if (decodedValue.startsWith("{")) {
            decodedValue
        } else {
            NvaExtDecoder.decode(decodedValue)
        } ?: return

        runCatching { json.parseToJsonElement(jsonText) }
            .getOrNull()
            ?.let { collectJsonFields(it, result) }
    }

    private fun collectJsonFields(element: JsonElement, result: MutableMap<String, String>) {
        when (element) {
            is JsonObject -> {
                element.forEach { (key, value) ->
                    if (value is JsonPrimitive) {
                        value.contentOrNull
                            ?.takeIf { it.isNotBlank() }
                            ?.let { result.putIfAbsent(key, it) }
                    } else {
                        collectJsonFields(value, result)
                    }
                }
            }

            else -> runCatching {
                element.jsonObject.forEach { (key, value) ->
                    value.jsonPrimitive.contentOrNull?.let { result.putIfAbsent(key, it) }
                }
            }
        }
    }

    private fun parseQueryString(value: String): Map<String, String> {
        if (!value.contains("=")) return emptyMap()
        return value
            .split('&')
            .mapNotNull { part ->
                val key = part.substringBefore("=").trim()
                val rawValue = part.substringAfter("=", missingDelimiterValue = "").trim()
                if (key.isBlank() || rawValue.isBlank()) {
                    null
                } else {
                    key to rawValue.decodeLoose()
                }
            }
            .toMap()
    }

    private fun extractXmlText(xml: String, localName: String): String? {
        val tagName = Regex.escape(localName)
        return Regex(
            pattern = """(?is)<(?:[A-Za-z0-9_.-]+:)?$tagName\b[^>]*>(.*?)</(?:[A-Za-z0-9_.-]+:)?$tagName>"""
        ).find(xml)?.groupValues?.getOrNull(1)
    }

    private fun normalizeAlias(fields: MutableMap<String, String>, alias: String, normalized: String) {
        val value = fields.entries.firstOrNull { it.key.equals(alias, ignoreCase = true) }?.value ?: return
        fields.putIfAbsent(normalized, value)
    }

    private fun preferAlias(fields: MutableMap<String, String>, alias: String, normalized: String) {
        val value = fields.entries.firstOrNull { it.key.equals(alias, ignoreCase = true) }?.value ?: return
        fields[normalized] = value
    }

    private fun Map<String, String>.firstString(key: String): String? =
        entries.firstOrNull { it.key.equals(key, ignoreCase = true) }?.value?.takeIf { it.isNotBlank() }

    private fun Map<String, String>.firstLong(key: String): Long? =
        firstString(key)?.filter { it.isDigit() }?.takeIf { it.isNotBlank() }?.toLongOrNull()

    private fun Map<String, String>.firstInt(key: String): Int? =
        firstLong(key)?.takeIf { it <= Int.MAX_VALUE }?.toInt()

    private fun Map<String, String>.firstFloat(key: String): Float? =
        firstString(key)?.toFloatOrNull()

    private fun Map<String, String>.firstBoolean(key: String): Boolean? =
        when (firstString(key)?.trim()?.lowercase()) {
            "1", "true", "yes", "open", "on" -> true
            "0", "false", "no", "close", "closed", "off" -> false
            else -> null
        }

    private fun Map<String, String>.firstDirectMediaUrl(): String? =
        directMediaUrlKeys
            .asSequence()
            .mapNotNull { key -> firstString(key) }
            .map { it.decodeLoose().decodeXmlEntities().trim() }
            .mapNotNull { it.toHttpMediaUrlOrNull() }
            .firstOrNull()

    private fun Map<String, String>.resolveClientHint(directMediaUrl: String?): CastClientHint {
        val values = values.joinToString(separator = "\n").lowercase()
        val currentUri = firstString("current_uri").orEmpty()
        return when {
            currentUri.startsWith("bilibili://", ignoreCase = true) ||
                values.contains("proj_source=bilibili") -> CastClientHint.OfficialBilibili
            directMediaUrl.orEmpty().isBilibiliMediaUrl() &&
                (values.contains("piliplus") || values.contains("dart/")) -> CastClientHint.PiliPlus
            directMediaUrl.orEmpty().isBilibiliMediaUrl() -> CastClientHint.GenericBilibili
            else -> CastClientHint.Generic
        }
    }

    private fun Map<String, String>.resolveDirectMediaType(directMediaUrl: String?): CastDirectMediaType {
        val url = directMediaUrl.orEmpty().substringBefore("?").lowercase()
        val protocol = firstString("protocol_info").orEmpty().lowercase()
        val values = values.joinToString(separator = "\n").lowercase()
        return when {
            dashMimeHints.any { it in protocol || it in values } ||
                url.endsWith(".mpd") -> CastDirectMediaType.Dash
            hlsMimeHints.any { it in protocol || it in values } ||
                url.endsWith(".m3u8") ||
                "/m3u8/" in url -> CastDirectMediaType.Hls
            audioMimeHints.any { it in protocol || it in values } ||
                audioExtensions.any { url.endsWith(it) } -> CastDirectMediaType.Audio
            directMediaUrl != null -> CastDirectMediaType.Progressive
            else -> CastDirectMediaType.Unknown
        }
    }

    private fun String.toHttpMediaUrlOrNull(): String? {
        val normalized = replace(", amp;", "&")
            .replace(" amp;", "&")
            .replace("&amp;", "&")
            .trim()
        return normalized.takeIf {
            it.startsWith("http://", ignoreCase = true) ||
                it.startsWith("https://", ignoreCase = true)
        }
    }

    private fun String.decodeLoose(): String =
        runCatching { decodeURLQueryComponent() }.getOrDefault(this)

    private fun String.decodeXmlEntities(): String =
        replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&amp;", "&")
}
