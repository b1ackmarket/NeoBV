package dev.aaa1115910.bv.cast.server

import dev.aaa1115910.bv.cast.CastPlaybackLauncher
import dev.aaa1115910.bv.cast.CastPlaybackSession
import dev.aaa1115910.bv.cast.CastPlaybackSessionRegistry
import dev.aaa1115910.bv.cast.CastPlaybackSnapshot
import dev.aaa1115910.bv.cast.CastTransportState
import dev.aaa1115910.bv.cast.protocol.CastContent
import dev.aaa1115910.bv.cast.protocol.CastContentParser
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.encodeURLParameter
import io.ktor.http.withCharset
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.cio.CIO
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.head
import io.ktor.server.routing.options
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CastHttpServer(
    private val uuid: String,
    private val requestLogger: CastRequestLogger,
    private val playbackLauncher: CastPlaybackLauncher,
    private val scope: CoroutineScope
) {
    private val logger = KotlinLogging.logger("CastHttpServer")
    private var server: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>? = null
    @Volatile
    private var currentUri: String = ""
    @Volatile
    private var currentMetaData: String = ""

    fun start() {
        if (server != null) return
        val newServer = embeddedServer(CIO, port = CastReceiverConfig.HTTP_PORT) {
            castModule()
        }
        try {
            newServer.start(wait = false)
            server = newServer
            requestLogger.log("HTTP cast receiver started on ${CastReceiverConfig.HTTP_PORT}")
        } catch (t: Throwable) {
            runCatching { newServer.stop(gracePeriodMillis = 0, timeoutMillis = 0) }
            logger.warn(t) { "Start HTTP cast receiver failed" }
            throw t
        }
    }

    fun stop() {
        server?.stop(gracePeriodMillis = 500, timeoutMillis = 1500)
        server = null
    }

    private fun Application.castModule() {
        routing {
            get("/") {
                call.respondText(
                    text = "NeoBV cast receiver",
                    contentType = ContentType.Text.Plain.withCharset(Charsets.UTF_8)
                )
            }
            get("/bilibili/description.xml") {
                call.respondXml(CastXmlDocuments.deviceDescription(call.localDescriptionHost(), uuid))
            }
            head("/bilibili/description.xml") { call.respondText("", contentType = ContentType.Application.Xml) }
            get("/bilibili/AVTransport.xml") { call.respondXml(CastXmlDocuments.avTransportScpd()) }
            get("/bilibili/RenderingControl.xml") { call.respondXml(CastXmlDocuments.renderingControlScpd()) }
            get("/bilibili/ConnectionManager.xml") { call.respondXml(CastXmlDocuments.connectionManagerScpd()) }
            get("/bilibili/NirvanaControl.xml") { call.respondXml(CastXmlDocuments.nirvanaControlScpd()) }
            head("/bilibili/AVTransport.xml") { call.respondText("", contentType = ContentType.Application.Xml) }
            head("/bilibili/RenderingControl.xml") { call.respondText("", contentType = ContentType.Application.Xml) }
            head("/bilibili/ConnectionManager.xml") { call.respondText("", contentType = ContentType.Application.Xml) }
            head("/bilibili/NirvanaControl.xml") { call.respondText("", contentType = ContentType.Application.Xml) }

            post("/bilibili/AVTransport/control") { call.handleControlCall() }
            post("/bilibili/RenderingControl/control") { call.handleControlCall() }
            post("/bilibili/ConnectionManager/control") { call.handleControlCall() }
            post("/bilibili/NirvanaControl/control") { call.handleControlCall() }
            put("/bilibili/AVTransport/event") { call.respondEventSubscription() }
            put("/bilibili/RenderingControl/event") { call.respondEventSubscription() }
            put("/bilibili/ConnectionManager/event") { call.respondEventSubscription() }
            put("/bilibili/NirvanaControl/event") { call.respondEventSubscription() }
            route("/bilibili/AVTransport/event", HttpMethod("SUBSCRIBE")) { handle { call.respondEventSubscription() } }
            route("/bilibili/RenderingControl/event", HttpMethod("SUBSCRIBE")) { handle { call.respondEventSubscription() } }
            route("/bilibili/ConnectionManager/event", HttpMethod("SUBSCRIBE")) { handle { call.respondEventSubscription() } }
            route("/bilibili/NirvanaControl/event", HttpMethod("SUBSCRIBE")) { handle { call.respondEventSubscription() } }
            delete("/bilibili/AVTransport/event") { call.respondText("", status = HttpStatusCode.OK) }
            delete("/bilibili/RenderingControl/event") { call.respondText("", status = HttpStatusCode.OK) }
            delete("/bilibili/ConnectionManager/event") { call.respondText("", status = HttpStatusCode.OK) }
            delete("/bilibili/NirvanaControl/event") { call.respondText("", status = HttpStatusCode.OK) }
            route("/bilibili/AVTransport/event", HttpMethod("UNSUBSCRIBE")) { handle { call.respondText("", status = HttpStatusCode.OK) } }
            route("/bilibili/RenderingControl/event", HttpMethod("UNSUBSCRIBE")) { handle { call.respondText("", status = HttpStatusCode.OK) } }
            route("/bilibili/ConnectionManager/event", HttpMethod("UNSUBSCRIBE")) { handle { call.respondText("", status = HttpStatusCode.OK) } }
            route("/bilibili/NirvanaControl/event", HttpMethod("UNSUBSCRIBE")) { handle { call.respondText("", status = HttpStatusCode.OK) } }

            registerCatchAll()
        }
    }

    private fun Routing.registerCatchAll() {
        get("{path...}") { call.handleGenericCall(body = null) }
        post("{path...}") { call.handleGenericCall(body = call.receiveText()) }
        put("{path...}") { call.handleGenericCall(body = call.receiveText()) }
        delete("{path...}") { call.handleGenericCall(body = call.receiveText()) }
        options("{path...}") {
            call.respondText("", status = HttpStatusCode.OK)
        }
    }

    private suspend fun ApplicationCall.handleControlCall() {
        val body = receiveText()
        logRequest(body)
        handleControlBody(body = body, path = request.path(), soapAction = request.headers["SOAPAction"].orEmpty())
    }

    private suspend fun ApplicationCall.handleControlBody(
        body: String,
        path: String,
        soapAction: String
    ) {
        val action = soapActionName(soapAction, body)
        if (action == "SetAVTransportURI") updateCurrentMedia(body)

        val content = CastContentParser.parse(
            path = path,
            queryParameters = request.queryParameters,
            body = body
        )
        if (content != null) maybeLaunch(content)

        val serviceType = soapServiceType(path, soapAction)
        val response = when (serviceType) {
            CastReceiverConfig.AV_TRANSPORT_SERVICE_TYPE -> handleAvTransportAction(action, body)
            CastReceiverConfig.RENDERING_CONTROL_SERVICE_TYPE -> handleRenderingControlAction(action)
            CastReceiverConfig.CONNECTION_MANAGER_SERVICE_TYPE -> handleConnectionManagerAction(action)
            CastReceiverConfig.NIRVANA_SERVICE_TYPE -> handleNirvanaAction(action, body)
            else -> soapResponse(serviceType, action)
        }
        respondText(
            text = response,
            contentType = ContentType.Application.Xml.withCharset(Charsets.UTF_8)
        )
    }

    private suspend fun ApplicationCall.handleGenericCall(body: String?) {
        logRequest(body)
        val path = request.path()
        val soapAction = request.headers["SOAPAction"].orEmpty()
        val action = soapActionName(soapAction, body.orEmpty())
        if (!body.isNullOrBlank() && isKnownSoapControlPath(path, action)) {
            handleControlBody(body = body, path = path, soapAction = soapAction)
            return
        }

        val content = CastContentParser.parse(
            path = path,
            queryParameters = request.queryParameters,
            body = body
        )
        val launched = content?.let { maybeLaunch(it) } ?: false
        respondText(
            text = """{"code":0,"message":"ok","launched":$launched}""",
            contentType = ContentType.Application.Json.withCharset(Charsets.UTF_8)
        )
    }

    private fun isKnownSoapControlPath(path: String, action: String): Boolean =
        action.isNotBlank() && (
            path.contains("AVTransport/control", ignoreCase = true) ||
                path.contains("RenderingControl/control", ignoreCase = true) ||
                path.contains("ConnectionManager/control", ignoreCase = true) ||
                path.contains("NirvanaControl/control", ignoreCase = true)
            )

    private fun ApplicationCall.logRequest(body: String?) {
        requestLogger.logRequest(
            method = request.httpMethod.value,
            path = buildString {
                append(request.path())
                val query = request.queryParameters.toLogQueryString()
                if (query.isNotBlank()) append("?").append(query)
            },
            remoteHost = request.headers["X-Forwarded-For"] ?: request.headers["Host"],
            headers = request.headers.entries().associate { it.key to it.value },
            body = body
        )
    }

    private fun io.ktor.http.Parameters.toLogQueryString(): String =
        names().flatMap { name ->
            getAll(name).orEmpty().map { value ->
                "${name.encodeURLParameter()}=${value.encodeURLParameter()}"
            }
        }.joinToString("&")

    private fun maybeLaunch(content: CastContent): Boolean {
        requestLogger.log("parsed cast content=$content")
        scope.launch {
            runCatching { playbackLauncher.launch(content) }
                .onFailure { logger.warn(it) { "Launch cast content failed: $content" } }
        }
        return true
    }

    private fun updateCurrentMedia(body: String) {
        currentUri = extractXmlText(body, "CurrentURI")?.decodeXmlEntities().orEmpty()
        currentMetaData = extractXmlText(body, "CurrentURIMetaData")?.decodeXmlEntities().orEmpty()
    }

    private suspend fun handleAvTransportAction(action: String, body: String): String {
        when (action) {
            "Play" -> withPlaybackSession(action) { play() }
            "Pause" -> withPlaybackSession(action) { pause() }
            "Stop" -> withPlaybackSession(action) { stop() }
            "Seek" -> {
                val unit = extractXmlText(body, "Unit")?.decodeXmlEntities().orEmpty()
                val target = extractXmlText(body, "Target")?.decodeXmlEntities().orEmpty()
                val positionMs = parseDlnaTimeMillis(target)
                requestLogger.log("AVTransport Seek unit=$unit target=$target parsedMs=$positionMs")
                if (positionMs != null && (unit.isBlank() || unit == "REL_TIME" || unit == "ABS_TIME")) {
                    withPlaybackSession(action) { seekTo(positionMs) }
                } else {
                    requestLogger.log("ignore unsupported Seek unit=$unit target=$target")
                }
            }
        }

        return when (action) {
            "GetTransportInfo" -> {
                val snapshot = currentSnapshot()
                soapResponse(
                    serviceType = CastReceiverConfig.AV_TRANSPORT_SERVICE_TYPE,
                    action = action,
                    values = mapOf(
                        "CurrentTransportState" to snapshot.state.dlnaName,
                        "CurrentTransportStatus" to "OK",
                        "CurrentSpeed" to snapshot.speed.cleanSpeed()
                    )
                )
            }

            "GetPositionInfo" -> {
                val snapshot = currentSnapshot()
                soapResponse(
                    serviceType = CastReceiverConfig.AV_TRANSPORT_SERVICE_TYPE,
                    action = action,
                    values = mapOf(
                        "Track" to "1",
                        "TrackDuration" to formatDlnaTime(snapshot.durationMs),
                        "TrackMetaData" to currentMetaData,
                        "TrackURI" to currentUri,
                        "RelTime" to formatDlnaTime(snapshot.positionMs),
                        "AbsTime" to formatDlnaTime(snapshot.positionMs),
                        "RelCount" to "0",
                        "AbsCount" to "0"
                    )
                )
            }

            "GetMediaInfo" -> {
                val snapshot = currentSnapshot()
                soapResponse(
                    serviceType = CastReceiverConfig.AV_TRANSPORT_SERVICE_TYPE,
                    action = action,
                    values = mapOf(
                        "NrTracks" to "1",
                        "MediaDuration" to formatDlnaTime(snapshot.durationMs),
                        "CurrentURI" to currentUri,
                        "CurrentURIMetaData" to currentMetaData,
                        "NextURI" to "",
                        "NextURIMetaData" to "",
                        "PlayMedium" to "NETWORK",
                        "RecordMedium" to "NOT_IMPLEMENTED",
                        "WriteStatus" to "NOT_IMPLEMENTED"
                    )
                )
            }

            "GetCurrentTransportActions" -> soapResponse(
                serviceType = CastReceiverConfig.AV_TRANSPORT_SERVICE_TYPE,
                action = action,
                values = mapOf("Actions" to "Play,Pause,Seek,Stop")
            )

            "GetDeviceCapabilities" -> soapResponse(
                serviceType = CastReceiverConfig.AV_TRANSPORT_SERVICE_TYPE,
                action = action,
                values = mapOf(
                    "PlayMedia" to "NETWORK",
                    "RecMedia" to "NOT_IMPLEMENTED",
                    "RecQualityModes" to "NOT_IMPLEMENTED"
                )
            )

            "GetTransportSettings" -> soapResponse(
                serviceType = CastReceiverConfig.AV_TRANSPORT_SERVICE_TYPE,
                action = action,
                values = mapOf(
                    "PlayMode" to "NORMAL",
                    "RecQualityMode" to "NOT_IMPLEMENTED"
                )
            )

            else -> soapResponse(CastReceiverConfig.AV_TRANSPORT_SERVICE_TYPE, action)
        }
    }

    private fun handleRenderingControlAction(action: String): String =
        when (action) {
            "GetVolume" -> soapResponse(
                serviceType = CastReceiverConfig.RENDERING_CONTROL_SERVICE_TYPE,
                action = action,
                values = mapOf("CurrentVolume" to "100")
            )

            "GetMute" -> soapResponse(
                serviceType = CastReceiverConfig.RENDERING_CONTROL_SERVICE_TYPE,
                action = action,
                values = mapOf("CurrentMute" to "0")
            )

            else -> soapResponse(CastReceiverConfig.RENDERING_CONTROL_SERVICE_TYPE, action)
        }

    private fun handleConnectionManagerAction(action: String): String =
        when (action) {
            "GetProtocolInfo" -> soapResponse(
                serviceType = CastReceiverConfig.CONNECTION_MANAGER_SERVICE_TYPE,
                action = action,
                values = mapOf(
                    "Source" to "",
                    "Sink" to CastXmlDocuments.SINK_PROTOCOL_INFO
                )
            )

            "GetCurrentConnectionIDs" -> soapResponse(
                serviceType = CastReceiverConfig.CONNECTION_MANAGER_SERVICE_TYPE,
                action = action,
                values = mapOf("ConnectionIDs" to "0")
            )

            "GetCurrentConnectionInfo" -> soapResponse(
                serviceType = CastReceiverConfig.CONNECTION_MANAGER_SERVICE_TYPE,
                action = action,
                values = mapOf(
                    "RcsID" to "0",
                    "AVTransportID" to "0",
                    "ProtocolInfo" to "",
                    "PeerConnectionManager" to "",
                    "PeerConnectionID" to "-1",
                    "Direction" to "Input",
                    "Status" to "OK"
                )
            )

            else -> soapResponse(CastReceiverConfig.CONNECTION_MANAGER_SERVICE_TYPE, action)
        }

    private suspend fun handleNirvanaAction(action: String, body: String): String {
        when (action) {
            "Play" -> withPlaybackSession("Nirvana.$action") { play() }
            "Pause" -> withPlaybackSession("Nirvana.$action") { pause() }
            "Stop" -> withPlaybackSession("Nirvana.$action") { stop() }
            "Seek" -> {
                val target = extractXmlText(body, "Target")?.decodeXmlEntities()
                    ?: extractXmlText(body, "Position")?.decodeXmlEntities().orEmpty()
                val positionMs = parseDlnaTimeMillis(target)
                requestLogger.log("Nirvana Seek target=$target parsedMs=$positionMs")
                positionMs?.let { withPlaybackSession("Nirvana.$action") { seekTo(it) } }
            }
            "SetSpeed", "SwitchSpeed" -> (
                extractXmlText(body, "Speed")?.decodeXmlEntities()?.toFloatOrNull()
                    ?: extractXmlText(body, "PlaybackSpeed")?.decodeXmlEntities()?.toFloatOrNull()
                    ?: extractXmlText(body, "CurrSpeed")?.decodeXmlEntities()?.toFloatOrNull()
                    ?: extractXmlText(body, "Rate")?.decodeXmlEntities()?.toFloatOrNull()
                )
                ?.let { speed -> withPlaybackSession("Nirvana.$action") { setSpeed(speed) } }
            "SwitchQuality", "SetQuality" -> {
                val qualityId = extractXmlText(body, "Qn")?.decodeXmlEntities()?.toIntOrNull()
                    ?: extractXmlText(body, "Quality")?.decodeXmlEntities()?.toIntOrNull()
                qualityId?.let { withPlaybackSession("Nirvana.$action") { setQuality(it) } }
            }
            "SetDanmakuSwitch", "SwitchDanmaku", "SetDanmaku" -> (
                parseSoapBoolean(extractXmlText(body, "DesiredSwitch")?.decodeXmlEntities())
                    ?: parseSoapBoolean(extractXmlText(body, "Open")?.decodeXmlEntities())
                    ?: parseSoapBoolean(extractXmlText(body, "DanmakuState")?.decodeXmlEntities())
                )
                ?.let { enabled -> withPlaybackSession("Nirvana.$action") { setDanmakuEnabled(enabled) } }
        }

        return when (action) {
            "GetAppInfo" -> soapResponse(
                serviceType = CastReceiverConfig.NIRVANA_SERVICE_TYPE,
                action = action,
                values = mapOf(
                    "PackageName" to CastReceiverConfig.OFFICIAL_YST_PACKAGE_NAME,
                    "AppKey" to "",
                    "Signature" to "",
                    "CurrentSignedIn" to "0"
                )
            )

            "GetAccountInfo" -> soapResponse(
                serviceType = CastReceiverConfig.NIRVANA_SERVICE_TYPE,
                action = action,
                values = mapOf("VipInfo" to "0")
            )

            "PrepareForMirrorProjection" -> soapResponse(
                serviceType = CastReceiverConfig.NIRVANA_SERVICE_TYPE,
                action = action,
                values = mapOf(
                    "ScreenWidth" to "1920",
                    "ScreenHeight" to "1080",
                    "PushUrl" to "http://${CastNetworkUtil.localIpv4Address()}:5223"
                )
            )

            "GetPlayInfo" -> {
                val snapshot = currentSnapshot()
                requestLogger.log(
                    "GetPlayInfo snapshot state=${snapshot.state} " +
                        "positionMs=${snapshot.positionMs} durationMs=${snapshot.durationMs} " +
                        "speed=${snapshot.speed} danmaku=${snapshot.danmakuEnabled} qn=${snapshot.qualityId} " +
                        "playerStatus=${snapshot.state.toNirvanaPlayerState()}"
                )
                soapResponse(
                    serviceType = CastReceiverConfig.NIRVANA_SERVICE_TYPE,
                    action = action,
                    values = mapOf("Content" to CastNirvanaPlayInfoFormatter.format(snapshot))
                )
            }

            else -> soapResponse(CastReceiverConfig.NIRVANA_SERVICE_TYPE, action)
        }
    }

    private suspend fun withPlaybackSession(action: String, block: CastPlaybackSession.() -> Unit) {
        val session = CastPlaybackSessionRegistry.current()
        if (session == null) {
            requestLogger.log("no active cast playback session for action=$action")
            return
        }
        withContext(Dispatchers.Main.immediate) {
            requestLogger.log("execute playback action=$action")
            runCatching { session.block() }
                .onSuccess { requestLogger.log("completed playback action=$action") }
                .onFailure { logger.warn(it) { "Cast playback command failed: $action" } }
        }
    }

    private suspend fun currentSnapshot(): CastPlaybackSnapshot =
        withContext(Dispatchers.Main.immediate) {
            CastPlaybackSessionRegistry.current()?.snapshot()
        } ?: CastPlaybackSnapshot()

    private suspend fun ApplicationCall.respondXml(xml: String) {
        respondText(
            text = xml,
            contentType = ContentType.Application.Xml.withCharset(Charsets.UTF_8)
        )
    }

    private suspend fun ApplicationCall.respondEventSubscription() {
        response.headers.append("SID", "uuid:$uuid")
        response.headers.append("TIMEOUT", "Second-1800")
        respondText("", status = HttpStatusCode.OK)
    }

    private fun ApplicationCall.localDescriptionHost(): String {
        val remote = request.local.remoteHost
            .takeIf { it.isNotBlank() }
            ?.let { runCatching { java.net.InetAddress.getByName(it) }.getOrNull() }
        return remote?.let(CastNetworkUtil::localIpv4AddressFor) ?: CastNetworkUtil.localIpv4Address()
    }

    private fun soapResponse(
        serviceType: String,
        action: String,
        values: Map<String, String> = emptyMap()
    ): String {
        val actionName = action.ifBlank { "Response" }
        val responseValues = values.entries.joinToString(separator = "\n") { (name, value) ->
            "        <$name>${value.escapeXml()}</$name>"
        }
        return buildString {
            append("""<?xml version="1.0" encoding="utf-8"?>""")
            append('\n')
            append("""<s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">""")
            append('\n')
            append("  <s:Body>\n")
            append("""    <u:${actionName}Response xmlns:u="$serviceType">""")
            append('\n')
            if (responseValues.isNotBlank()) {
                append(responseValues)
                append('\n')
            }
            append("""    </u:${actionName}Response>""")
            append('\n')
            append("  </s:Body>\n")
            append("</s:Envelope>")
        }
    }

    private fun soapActionName(soapAction: String, body: String): String {
        val headerAction = soapAction.trim().trim('"')
            .substringAfter("#", missingDelimiterValue = "")
            .takeIf { it.isNotBlank() }
        if (headerAction != null) return headerAction

        return Regex("""<\s*(?:[A-Za-z0-9_.-]+:)?([A-Z][A-Za-z0-9_.-]+)\b""")
            .findAll(body)
            .map { it.groupValues[1] }
            .firstOrNull { it != "Envelope" && it != "Body" }
            .orEmpty()
    }

    private fun soapServiceType(path: String, soapAction: String): String {
        val headerService = soapAction.trim().trim('"')
            .substringBefore("#", missingDelimiterValue = "")
            .takeIf { it.startsWith("urn:") }
        if (headerService != null) return headerService

        return when {
            path.contains("RenderingControl", ignoreCase = true) -> CastReceiverConfig.RENDERING_CONTROL_SERVICE_TYPE
            path.contains("ConnectionManager", ignoreCase = true) -> CastReceiverConfig.CONNECTION_MANAGER_SERVICE_TYPE
            path.contains("NirvanaControl", ignoreCase = true) -> CastReceiverConfig.NIRVANA_SERVICE_TYPE
            else -> CastReceiverConfig.AV_TRANSPORT_SERVICE_TYPE
        }
    }

    private fun extractXmlText(xml: String, localName: String): String? {
        val tagName = Regex.escape(localName)
        return Regex(
            pattern = """(?is)<(?:[A-Za-z0-9_.-]+:)?$tagName\b[^>]*>(.*?)</(?:[A-Za-z0-9_.-]+:)?$tagName>"""
        ).find(xml)?.groupValues?.getOrNull(1)
    }

    private fun parseDlnaTimeMillis(value: String?): Long? {
        val text = value?.trim()?.takeIf { it.isNotBlank() } ?: return null
        if (!text.contains(":")) {
            return text.toDoubleOrNull()?.let { (it * 1000).toLong().coerceAtLeast(0L) }
        }

        val parts = text.split(":")
        if (parts.size !in 2..3) return null
        val hours = if (parts.size == 3) parts[0].toLongOrNull() ?: return null else 0L
        val minutes = parts[parts.size - 2].toLongOrNull() ?: return null
        val seconds = parts.last().toDoubleOrNull() ?: return null
        val totalSeconds = hours * 3600 + minutes * 60 + seconds
        return (totalSeconds * 1000).toLong().coerceAtLeast(0L)
    }

    private fun parseSoapBoolean(value: String?): Boolean? =
        when (value?.trim()?.lowercase()) {
            "1", "true", "yes" -> true
            "0", "false", "no" -> false
            else -> null
        }

    private fun formatDlnaTime(timeMs: Long): String {
        val totalSeconds = (timeMs.coerceAtLeast(0L) / 1000)
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return "%d:%02d:%02d".format(hours, minutes, seconds)
    }

    private fun String.decodeXmlEntities(): String =
        replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&amp;", "&")

    private fun String.escapeXml(): String =
        replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")

    private fun Float.cleanSpeed(): String =
        if (this % 1f == 0f) toInt().toString() else toString()

    private fun CastTransportState.toNirvanaPlayerState(): Int =
        when (this) {
            CastTransportState.PLAYING -> 4
            CastTransportState.PAUSED -> 5
            CastTransportState.TRANSITIONING -> 2
            CastTransportState.STOPPED -> 7
        }
}
