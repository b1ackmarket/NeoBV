package dev.aaa1115910.biliapi.websocket

import dev.aaa1115910.biliapi.http.BiliLiveHttpApi
import dev.aaa1115910.biliapi.http.entity.live.DanmakuEvent
import dev.aaa1115910.biliapi.http.entity.live.HostListItem
import dev.aaa1115910.biliapi.http.entity.live.LiveEvent
import dev.aaa1115910.biliapi.http.entity.live.SuperChatEvent
import dev.aaa1115910.biliapi.http.util.brotliDecompress
import dev.aaa1115910.biliapi.http.util.zlibDecompress
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext
import kotlin.math.min

object LiveDataWebSocket {
    private val logger = KotlinLogging.logger { }
    private val json = Json { ignoreUnknownKeys = true }
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .writeTimeout(12, TimeUnit.SECONDS)
        .pingInterval(0, TimeUnit.SECONDS)
        .build()

    private const val LiveDanmakuPath = "/sub"
    private const val BroadcastHost = "broadcastlv.chat.bilibili.com"
    private const val HeaderLength = 16
    private const val OpHeartbeat = 2
    private const val OpMessage = 5
    private const val OpAuth = 7
    private const val OpAuthReply = 8
    private const val VersionPlain = 0
    private const val VersionHeartbeatAuth = 1
    private const val VersionZlib = 2
    private const val VersionBrotli = 3

    suspend fun connectLiveEvent(
        roomId: Int,
        uid: Long = 0L,
        sessData: String = "",
        biliJct: String = "",
        uidCkMd5: String = "",
        sid: String = "",
        buvid3: String = "",
        onDebug: (event: LiveDataWebSocketDebugEvent) -> Unit = {},
        onEvent: (event: LiveEvent) -> Unit
    ): Job {
        onDebug(LiveDataWebSocketDebugEvent.StateChanged(LiveDataWebSocketState.Connecting))
        val realRoomId =
            BiliLiveHttpApi.getLiveRoomPlayInfo(roomId).data?.roomId
                ?: throw CancellationException("No live room info")
        val authUid = normalizeLiveDanmakuUid(uid = uid, sessData = sessData)
        val danmuInfo =
            BiliLiveHttpApi.getLiveDanmuInfo(
                roomId = realRoomId,
                uid = authUid,
                sessData = sessData.takeIf { authUid > 0L }.orEmpty(),
                biliJct = biliJct.takeIf { authUid > 0L }.orEmpty(),
                uidCkMd5 = uidCkMd5.takeIf { authUid > 0L }.orEmpty(),
                sid = sid.takeIf { authUid > 0L }.orEmpty(),
                buvid3 = buvid3
            ).data ?: throw CancellationException("No live danmaku info")
        val endpoints = buildLiveDanmakuEndpoints(danmuInfo.hostList)
            .ifEmpty { throw CancellationException("No live danmaku endpoint") }
        val session = LiveDanmakuSocketSession(
            roomId = realRoomId,
            uid = authUid,
            cookieHeader = buildLiveCookieHeader(
                uid = authUid,
                sessData = sessData.takeIf { authUid > 0L }.orEmpty(),
                biliJct = biliJct.takeIf { authUid > 0L }.orEmpty(),
                uidCkMd5 = uidCkMd5.takeIf { authUid > 0L }.orEmpty(),
                sid = sid.takeIf { authUid > 0L }.orEmpty(),
                buvid3 = buvid3
            ),
            token = danmuInfo.token,
            endpoints = endpoints,
            coroutineContext = kotlinx.coroutines.currentCoroutineContext(),
            onDebug = onDebug,
            onEvent = onEvent
        )
        session.start()
        return session.job
    }

    internal fun preferLiveDanmakuHosts(hosts: List<HostListItem>): List<HostListItem> {
        return hosts
            .filter { it.host.isNotBlank() && (it.wssPort > 0 || it.wsPort > 0) }
            .distinctBy { "${it.host}:${it.wssPort}:${it.wsPort}" }
            .sortedBy { if (it.host == BroadcastHost) 0 else 1 }
    }

    internal fun buildLiveDanmakuEndpoints(hosts: List<HostListItem>): List<LiveDanmakuEndpoint> {
        return preferLiveDanmakuHosts(hosts)
            .flatMap { host ->
                buildList {
                    if (host.wssPort > 0) {
                        add(LiveDanmakuEndpoint(host = host.host, scheme = "wss", port = host.wssPort))
                    }
                    if (host.wsPort > 0) {
                        add(LiveDanmakuEndpoint(host = host.host, scheme = "ws", port = host.wsPort))
                    }
                }
            }
            .distinctBy { "${it.scheme}:${it.host}:${it.port}" }
    }

    internal fun chooseLiveDanmakuHost(hosts: List<HostListItem>): HostListItem {
        return preferLiveDanmakuHosts(hosts).firstOrNull()
            ?: throw CancellationException("No live danmaku host")
    }

    internal fun normalizeLiveDanmakuUid(uid: Long, sessData: String): Long =
        if (uid > 0L && sessData.isNotBlank()) uid else 0L

    internal fun buildLiveAuthPacket(roomId: Int, uid: Long, token: String): ByteArray {
        val data = buildJsonObject {
            put("uid", uid)
            put("roomid", roomId)
            put("protover", 2)
            put("platform", "web")
            put("type", 2)
            put("key", token)
        }.toString().toByteArray()
        return buildPacket(op = OpAuth, version = VersionHeartbeatAuth, sequence = 1, body = data)
    }

    internal fun buildLiveCookieHeader(
        uid: Long = 0L,
        sessData: String = "",
        biliJct: String = "",
        uidCkMd5: String = "",
        sid: String = "",
        buvid3: String = ""
    ): String {
        return buildList {
            if (uid > 0L) add("DedeUserID=$uid")
            if (uidCkMd5.isNotBlank()) add("DedeUserID__ckMd5=$uidCkMd5")
            if (sessData.isNotBlank()) add("SESSDATA=$sessData")
            if (biliJct.isNotBlank()) add("bili_jct=$biliJct")
            if (sid.isNotBlank()) add("sid=$sid")
            if (buvid3.isNotBlank()) add("buvid3=$buvid3")
        }.joinToString("; ")
    }

    internal fun buildLiveWebSocketFailureReason(
        throwable: Throwable,
        endpoint: LiveDanmakuEndpoint,
        stage: String,
        response: Response? = null,
        endpointIndex: Int = 0,
        endpointCount: Int = 1,
        reconnectAttempt: Int = 0,
        hasCookie: Boolean = false,
        tokenLength: Int = 0
    ): String {
        return buildString {
            append(throwable::class.simpleName ?: "failure")
            throwable.message?.takeIf { it.isNotBlank() }?.let { append(": ").append(it) }
            throwable.cause?.let { cause ->
                append(" cause=")
                    .append(cause::class.simpleName ?: "unknown")
                cause.message?.takeIf { it.isNotBlank() }?.let { append(":").append(it) }
            }
            append(" stage=").append(stage)
            append(" endpoint=").append(endpoint.displayName())
            append(" url=").append(endpoint.websocketUrl())
            append(" idx=").append(endpointIndex + 1).append('/').append(endpointCount.coerceAtLeast(1))
            append(" retry=").append(reconnectAttempt)
            append(" cookie=").append(if (hasCookie) "yes" else "no")
            append(" token=").append(tokenLength)
            response?.let {
                append(" http=").append(it.code).append(' ').append(it.message)
                append(" server=").append(it.header("server").orEmpty().ifBlank { "-" })
            }
        }.take(320)
    }

    internal fun buildLiveHeartbeatPacket(sequence: Int = 1): ByteArray {
        return buildPacket(
            op = OpHeartbeat,
            version = VersionHeartbeatAuth,
            sequence = sequence,
            body = "[object Object]".toByteArray()
        )
    }

    internal fun isLiveAuthReplySuccess(data: ByteArray): Boolean {
        return parsePackets(data).any { packet ->
            if (packet.op != OpAuthReply) return@any false
            val text = packet.body.decodeToString().trim()
            runCatching {
                json.parseToJsonElement(text).jsonObject["code"]?.jsonPrimitive?.intOrNull == 0
            }.getOrDefault(false)
        }
    }

    internal suspend fun handleLiveEventData(data: ByteArray): List<LiveEvent> = withContext(Dispatchers.Default) {
        parseLiveEvents(data)
    }

    private fun parseLiveEvents(data: ByteArray): List<LiveEvent> {
        val result = mutableListOf<LiveEvent>()
        parsePackets(data).forEach { packet ->
            when (packet.op) {
                OpMessage -> {
                    val payload = when (packet.version) {
                        VersionPlain, VersionHeartbeatAuth -> packet.body
                        VersionZlib -> runCatching { packet.body.zlibDecompress() }.getOrNull()
                        VersionBrotli -> runCatching { packet.body.brotliDecompress() }.getOrNull()
                        else -> null
                    } ?: return@forEach

                    if (packet.version == VersionZlib || packet.version == VersionBrotli) {
                        result += parseLiveEvents(payload)
                    } else {
                        parseLiveCommandEvent(payload.decodeToString())?.let { result += it }
                    }
                }

                OpAuthReply -> {
                    val text = packet.body.decodeToString().trim()
                    val authed = runCatching {
                        json.parseToJsonElement(text).jsonObject["code"]?.jsonPrimitive?.intOrNull == 0
                    }.getOrDefault(false)
                    if (!authed && text.isNotBlank()) {
                        logger.warn { "Live danmaku auth reply failed: $text" }
                    }
                }
            }
        }
        return result
    }

    private fun parseLiveCommandEvent(strData: String): LiveEvent? {
        val dataJson = runCatching { json.parseToJsonElement(strData).jsonObject }.getOrNull()
            ?: return null
        val cmd = dataJson["cmd"]?.jsonPrimitive?.content ?: return null
        return when (cmd.substringBefore(":")) {
            "DANMU_MSG" -> parseDanmakuEvent(dataJson)
            "SUPER_CHAT_MESSAGE",
            "SUPER_CHAT_MESSAGE_JPN" -> parseSuperChatEvent(dataJson)
            else -> null
        }
    }

    private fun parseDanmakuEvent(dataJson: JsonObject): DanmakuEvent? {
        return runCatching {
            val info = dataJson["info"]!!.jsonArray
            val extra = info.getOrNull(0)?.jsonArrayOrNull()
            val contentExtra = extra
                ?.getOrNull(15)
                ?.jsonObjectOrNull()
                ?.get("extra")
                ?.jsonPrimitive
                ?.contentOrNull
                ?.let { raw ->
                    runCatching { json.parseToJsonElement(raw).jsonObject }.getOrNull()
                }
            val contentUser = extra
                ?.getOrNull(15)
                ?.jsonObjectOrNull()
                ?.get("user")
                ?.jsonObjectOrNull()
            val sender = info.getOrNull(2)?.jsonArrayOrNull()
            val medal = info.getOrNull(3)?.jsonArrayOrNull()
            val danmakuContent = info[1].jsonPrimitive.content
            val senderMid = contentUser
                ?.get("uid")
                ?.jsonPrimitive
                ?.longOrNull
                ?: sender?.getOrNull(0)?.jsonPrimitive?.longOrNull
                ?: 0L
            val senderUsername = contentUser
                ?.get("base")
                ?.jsonObjectOrNull()
                ?.get("name")
                ?.jsonPrimitive
                ?.contentOrNull
                ?: sender?.getOrNull(1)?.jsonPrimitive?.content.orEmpty()
            val contentMedal = contentUser
                ?.get("medal")
                ?.jsonObjectOrNull()
            val color = contentExtra
                ?.get("color")
                ?.jsonPrimitive
                ?.intOrNull
                ?: extra?.getOrNull(3)?.jsonPrimitive?.intOrNull
                ?: 0xffffff
            val mode = contentExtra
                ?.get("mode")
                ?.jsonPrimitive
                ?.intOrNull
                ?: extra?.getOrNull(1)?.jsonPrimitive?.intOrNull
                ?: 1
            val rndTimeMs = extra?.getOrNull(4)?.jsonPrimitive?.longOrNull
                ?.takeIf { it > 0L }
                ?.let { normalizeTimestampMs(it) }
            val sendTimeMs = dataJson["send_time"]?.jsonPrimitive?.longOrNull
                ?.takeIf { it > 0L }
                ?.let { normalizeTimestampMs(it) }

            DanmakuEvent(
                content = danmakuContent,
                mid = senderMid,
                username = senderUsername,
                medalName = contentMedal
                    ?.get("name")
                    ?.jsonPrimitive
                    ?.contentOrNull
                    ?: medal?.getOrNull(1)?.jsonPrimitive?.content,
                medalLevel = contentMedal
                    ?.get("level")
                    ?.jsonPrimitive
                    ?.intOrNull
                    ?: medal?.getOrNull(0)?.jsonPrimitive?.intOrNull,
                color = color,
                mode = mode,
                eventTimeMs = rndTimeMs ?: sendTimeMs ?: System.currentTimeMillis(),
                sendTimeMs = sendTimeMs,
                rndTimeMs = rndTimeMs
            )
        }.onFailure {
            logger.warn { "Parse live danmaku failed: ${it.message}" }
        }.getOrNull()
    }

    private fun parseSuperChatEvent(dataJson: JsonObject): SuperChatEvent? {
        return runCatching {
            val data = dataJson["data"]?.jsonObjectOrNull() ?: return@runCatching null
            val message = data["message"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
                ?: return@runCatching null
            val userInfo = data["user_info"]?.jsonObjectOrNull()
            val ts = data["ts"]?.jsonPrimitive?.longOrNull
                ?.takeIf { it > 0L }
                ?.let { normalizeTimestampMs(it) }
            SuperChatEvent(
                id = data["id"]?.jsonPrimitive?.longOrNull ?: 0L,
                uid = data["uid"]?.jsonPrimitive?.longOrNull ?: 0L,
                username = userInfo?.get("uname")?.jsonPrimitive?.contentOrNull.orEmpty(),
                message = message,
                price = data["price"]?.jsonPrimitive?.longOrNull ?: 0L,
                eventTimeMs = ts ?: System.currentTimeMillis()
            )
        }.onFailure {
            logger.warn { "Parse live super chat failed: ${it.message}" }
        }.getOrNull()
    }

    private fun buildPacket(op: Int, version: Int, sequence: Int, body: ByteArray): ByteArray {
        val total = HeaderLength + body.size
        return ByteBuffer.allocate(total)
            .order(ByteOrder.BIG_ENDIAN)
            .putInt(total)
            .putShort(HeaderLength.toShort())
            .putShort(version.toShort())
            .putInt(op)
            .putInt(sequence)
            .put(body)
            .array()
    }

    private fun parsePackets(bytes: ByteArray): List<LivePacket> {
        val result = mutableListOf<LivePacket>()
        var offset = 0
        while (offset + HeaderLength <= bytes.size) {
            val packetLength = readInt(bytes, offset)
            if (packetLength < HeaderLength || offset + packetLength > bytes.size) {
                logger.warn { "Invalid live danmaku packet length: length=$packetLength offset=$offset size=${bytes.size}" }
                break
            }
            val headerLength = readShort(bytes, offset + 4)
            if (headerLength < HeaderLength || headerLength > packetLength) {
                logger.warn { "Invalid live danmaku header length: header=$headerLength length=$packetLength" }
                break
            }
            val version = readShort(bytes, offset + 6)
            val op = readInt(bytes, offset + 8)
            val sequence = readInt(bytes, offset + 12)
            val bodyStart = offset + headerLength
            val bodyEnd = offset + packetLength
            val body = bytes.copyOfRange(bodyStart, bodyEnd)
            result += LivePacket(version = version, op = op, sequence = sequence, body = body)
            offset += packetLength
        }
        return result
    }

    private fun readInt(bytes: ByteArray, offset: Int): Int =
        ByteBuffer.wrap(bytes, offset, 4).order(ByteOrder.BIG_ENDIAN).int

    private fun readShort(bytes: ByteArray, offset: Int): Int =
        ByteBuffer.wrap(bytes, offset, 2).order(ByteOrder.BIG_ENDIAN).short.toInt() and 0xffff

    private fun normalizeTimestampMs(value: Long): Long {
        return if (value < 10_000_000_000L) value * 1000L else value
    }

    private fun kotlinx.serialization.json.JsonElement.jsonArrayOrNull(): JsonArray? = this as? JsonArray
    private fun kotlinx.serialization.json.JsonElement.jsonObjectOrNull(): JsonObject? = this as? JsonObject

    data class LiveDanmakuEndpoint(
        val host: String,
        val scheme: String,
        val port: Int
    ) {
        fun displayName(): String = "$scheme://$host:$port"

        fun websocketUrl(): String {
            val defaultPort = if (scheme == "wss") 443 else 80
            return if (port == defaultPort) {
                "$scheme://$host$LiveDanmakuPath"
            } else {
                "$scheme://$host:$port$LiveDanmakuPath"
            }
        }
    }

    private data class LivePacket(
        val version: Int,
        val op: Int,
        val sequence: Int,
        val body: ByteArray
    )

    private class LiveDanmakuSocketSession(
        private val roomId: Int,
        private val uid: Long,
        private val cookieHeader: String,
        private val token: String,
        private val endpoints: List<LiveDanmakuEndpoint>,
        coroutineContext: CoroutineContext,
        private val onDebug: (event: LiveDataWebSocketDebugEvent) -> Unit,
        private val onEvent: (event: LiveEvent) -> Unit
    ) {
        private val logger = KotlinLogging.logger { }
        private var webSocket: WebSocket? = null
        private var heartbeatJob: Job? = null
        private var authTimeoutJob: Job? = null
        private var endpointIndex = 0
        private var reconnectAttempt = 0
        private var sequence = 1
        private var connectionStage = "idle"
        @Volatile
        private var closed = false
        @Volatile
        private var authed = false

        val job: CompletableJob = Job(coroutineContext[Job])
        private val scope = kotlinx.coroutines.CoroutineScope(coroutineContext + job)

        fun start() {
            job.invokeOnCompletion { closeSocket() }
            connectCurrentHost()
        }

        private fun connectCurrentHost() {
            if (closed || !job.isActive) return
            val endpoint = endpoints.getOrNull(endpointIndex) ?: run {
                job.completeExceptionally(CancellationException("No live danmaku endpoint"))
                return
            }
            authed = false
            connectionStage = "connecting"
            heartbeatJob?.cancel()
            authTimeoutJob?.cancel()
            runCatching { webSocket?.close(1000, "reconnect") }
            webSocket = null

            val request = Request.Builder()
                .url(endpoint.websocketUrl())
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36"
                )
                .header("Referer", "https://live.bilibili.com/")
                .header("Origin", "https://live.bilibili.com")
                .header("Accept-Language", "zh-CN,zh;q=0.9")
                .header("Pragma", "no-cache")
                .header("Cache-Control", "no-cache")
                .apply {
                    if (cookieHeader.isNotBlank()) header("Cookie", cookieHeader)
                }
                .build()
            logger.info { "Live danmaku connecting: room=$roomId endpoint=${endpoint.displayName()} hasCookie=${cookieHeader.isNotBlank()}" }
            onDebug(LiveDataWebSocketDebugEvent.StateChanged(LiveDataWebSocketState.Connecting))
            onDebug(LiveDataWebSocketDebugEvent.HostChanged(endpoint.displayName()))
            onDebug(
                LiveDataWebSocketDebugEvent.Info(
                    "connect stage=$connectionStage endpoint=${endpoint.displayName()} " +
                        "idx=${endpointIndex + 1}/${endpoints.size} retry=$reconnectAttempt " +
                        "cookie=${if (cookieHeader.isNotBlank()) "yes" else "no"} token=${token.length}"
                )
            )
            webSocket = okHttpClient.newWebSocket(request, Listener(endpoint))
        }

        private fun onAuthed() {
            authed = true
            connectionStage = "authed"
            reconnectAttempt = 0
            authTimeoutJob?.cancel()
            onDebug(LiveDataWebSocketDebugEvent.StateChanged(LiveDataWebSocketState.Authed))
            heartbeatJob?.cancel()
            heartbeatJob = scope.launch {
                while (isActive) {
                    val nextSequence = sequence++
                    webSocket?.send(ByteString.of(*buildLiveHeartbeatPacket(nextSequence)))
                    delay(30_000)
                }
            }
        }

        private fun scheduleReconnect(reason: String) {
            if (closed || !job.isActive || endpoints.isEmpty()) return
            heartbeatJob?.cancel()
            authTimeoutJob?.cancel()
            endpointIndex = (endpointIndex + 1) % endpoints.size
            val delayMs = min(10_000L, (1L shl reconnectAttempt.coerceIn(0, 4)) * 1000L)
            reconnectAttempt++
            logger.warn { "Live danmaku reconnect scheduled: room=$roomId delay=${delayMs}ms reason=$reason" }
            onDebug(LiveDataWebSocketDebugEvent.StateChanged(LiveDataWebSocketState.Reconnecting))
            scope.launch {
                delay(delayMs)
                connectCurrentHost()
            }
        }

        private fun closeSocket() {
            closed = true
            heartbeatJob?.cancel()
            authTimeoutJob?.cancel()
            runCatching { webSocket?.close(1000, "bye") }
            webSocket = null
        }

        private inner class Listener(private val endpoint: LiveDanmakuEndpoint) : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                connectionStage = "opened"
                onDebug(
                    LiveDataWebSocketDebugEvent.Info(
                        "opened http=${response.code} ${endpoint.displayName()} " +
                            "protocol=${response.header("sec-websocket-protocol").orEmpty().ifBlank { "-" }}"
                    )
                )
                val sent = webSocket.send(ByteString.of(*buildLiveAuthPacket(roomId, uid, token)))
                connectionStage = if (sent) "auth_sent" else "auth_send_failed"
                logger.info { "Live danmaku auth sent: room=$roomId endpoint=${endpoint.displayName()} sent=$sent uid=$uid" }
                onDebug(
                    LiveDataWebSocketDebugEvent.Info(
                        "auth sent=$sent stage=$connectionStage ${endpoint.displayName()} uid=$uid token=${token.length}"
                    )
                )
                authTimeoutJob = scope.launch {
                    delay(6_000)
                    if (!authed && isActive) {
                        logger.warn { "Live danmaku auth timeout: room=$roomId endpoint=${endpoint.displayName()}" }
                        onDebug(LiveDataWebSocketDebugEvent.StateChanged(LiveDataWebSocketState.Failed))
                        onDebug(LiveDataWebSocketDebugEvent.Error("auth timeout ${endpoint.displayName()}"))
                        runCatching { webSocket.close(1000, "auth timeout") }
                        scheduleReconnect("auth timeout")
                    }
                }
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                val raw = bytes.toByteArray()
                onDebug(LiveDataWebSocketDebugEvent.RawMessage)
                scope.launch(Dispatchers.Default) {
                    if (!authed && isLiveAuthReplySuccess(raw)) {
                        logger.info { "Live danmaku auth success: room=$roomId endpoint=${endpoint.displayName()}" }
                        onAuthed()
                    }
                    runCatching { parseLiveEvents(raw) }
                        .onSuccess { events ->
                            if (events.isNotEmpty()) {
                                logger.trace { "Live danmaku events: room=$roomId count=${events.size}" }
                            }
                            val danmakuCount = events.count { it is DanmakuEvent }
                            if (danmakuCount > 0) {
                                onDebug(LiveDataWebSocketDebugEvent.DanmakuParsed(danmakuCount))
                            }
                            events.forEach(onEvent)
                        }
                        .onFailure { logger.warn(it) { "Handle live danmaku packet failed: ${it.message}" } }
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                logger.debug { "Live danmaku text frame: room=$roomId text=${text.take(120)}" }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                val reason = buildLiveWebSocketFailureReason(
                    throwable = t,
                    endpoint = endpoint,
                    stage = connectionStage,
                    response = response,
                    endpointIndex = endpointIndex,
                    endpointCount = endpoints.size,
                    reconnectAttempt = reconnectAttempt,
                    hasCookie = cookieHeader.isNotBlank(),
                    tokenLength = token.length
                )
                logger.warn(t) { "Live danmaku websocket failed: room=$roomId endpoint=${endpoint.displayName()} reason=$reason" }
                onDebug(LiveDataWebSocketDebugEvent.StateChanged(LiveDataWebSocketState.Failed))
                onDebug(LiveDataWebSocketDebugEvent.Error(reason))
                scheduleReconnect("failure ${t::class.simpleName}:${t.message}")
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                runCatching { webSocket.close(code, reason) }
                onDebug(LiveDataWebSocketDebugEvent.Error("closing $code $reason"))
                if (!closed) scheduleReconnect("closing $code $reason")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                onDebug(LiveDataWebSocketDebugEvent.Error("closed $code $reason"))
                if (!closed) scheduleReconnect("closed $code $reason")
            }
        }
    }
}

enum class LiveDataWebSocketState {
    Disabled,
    Connecting,
    Authed,
    Failed,
    Reconnecting
}

sealed interface LiveDataWebSocketDebugEvent {
    data class StateChanged(val state: LiveDataWebSocketState) : LiveDataWebSocketDebugEvent
    data class HostChanged(val host: String) : LiveDataWebSocketDebugEvent
    data class Info(val message: String) : LiveDataWebSocketDebugEvent
    data class Error(val reason: String) : LiveDataWebSocketDebugEvent
    data object RawMessage : LiveDataWebSocketDebugEvent
    data class DanmakuParsed(val count: Int) : LiveDataWebSocketDebugEvent
}
