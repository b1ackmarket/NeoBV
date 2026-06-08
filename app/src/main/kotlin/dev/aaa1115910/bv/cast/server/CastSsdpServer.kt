package dev.aaa1115910.bv.cast.server

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.MulticastSocket
import java.net.SocketTimeoutException
import java.util.Locale
import kotlin.random.Random

class CastSsdpServer(
    private val uuid: String,
    private val requestLogger: CastRequestLogger,
    private val scope: CoroutineScope
) {
    private val logger = KotlinLogging.logger("CastSsdpServer")
    private var listenJob: Job? = null
    private var notifyJob: Job? = null
    @Volatile
    private var running = false

    fun start() {
        if (running) return
        running = true
        listenJob = scope.launch { listenLoop() }
        notifyJob = scope.launch { notifyLoop() }
    }

    fun stop() {
        running = false
        listenJob?.cancel()
        notifyJob?.cancel()
        listenJob = null
        notifyJob = null
    }

    private suspend fun listenLoop() {
        runCatching {
            MulticastSocket(null).use { socket ->
                socket.reuseAddress = true
                socket.bind(InetSocketAddress(CastReceiverConfig.SSDP_PORT))
                socket.joinGroup(InetAddress.getByName(CastReceiverConfig.SSDP_ADDRESS))
                socket.soTimeout = 1000
                val buffer = ByteArray(8192)
                requestLogger.log("SSDP listener started")
                while (running && scope.coroutineContext.isActive) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    try {
                        socket.receive(packet)
                    } catch (_: SocketTimeoutException) {
                        continue
                    }
                    val message = String(packet.data, packet.offset, packet.length)
                    if (!message.startsWith("M-SEARCH", ignoreCase = true)) continue
                    requestLogger.log("SSDP M-SEARCH from=${packet.address.hostAddress}:${packet.port} body=${message.oneLine()}")
                    val targets = responseTargetsFor(message)
                    if (targets.isNotEmpty()) {
                        sendSearchResponses(packet.address, packet.port, targets)
                    }
                }
            }
        }.onFailure {
            if (running) logger.warn(it) { "SSDP listener failed" }
        }
    }

    private suspend fun notifyLoop() {
        sendByeByeNotifications()
        delay(STARTUP_ALIVE_DELAY_MS)
        repeat(STARTUP_NOTIFY_BURSTS) {
            if (!running || !scope.coroutineContext.isActive) return
            sendAliveNotifications()
            delay(STARTUP_NOTIFY_INTERVAL_MS)
        }
        while (running && scope.coroutineContext.isActive) {
            delay(REGULAR_NOTIFY_INTERVAL_MS)
            sendAliveNotifications()
        }
    }

    private fun responseTargetsFor(message: String): List<String> {
        val upper = message.uppercase(Locale.ROOT)
        val requestedTarget = Regex("""(?im)^\s*ST\s*:\s*(.+?)\s*$""")
            .find(message)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
            ?.lowercase(Locale.ROOT)
            .orEmpty()
        val targets = ssdpTargets()
        return when {
            requestedTarget == "ssdp:all" -> targets
            requestedTarget == "upnp:rootdevice" -> listOf("upnp:rootdevice")
            requestedTarget == "uuid:${uuid.lowercase(Locale.ROOT)}" -> listOf("uuid:$uuid")
            requestedTarget.isBlank() -> emptyList()
            else -> targets.filter { target ->
                val lowerTarget = target.lowercase(Locale.ROOT)
                lowerTarget == requestedTarget ||
                    lowerTarget.contains(requestedTarget) ||
                    requestedTarget.contains(lowerTarget) ||
                    (requestedTarget.contains("mediarenderer") && lowerTarget.contains("mediarenderer")) ||
                    (requestedTarget.contains("avtransport") && lowerTarget.contains("avtransport")) ||
                    (requestedTarget.contains("renderingcontrol") && lowerTarget.contains("renderingcontrol")) ||
                    (requestedTarget.contains("connectionmanager") && lowerTarget.contains("connectionmanager")) ||
                    (requestedTarget.contains("nirvanacontrol") && lowerTarget.contains("nirvanacontrol")) ||
                    (requestedTarget.contains("app-bilibili-com") && lowerTarget.contains("app-bilibili-com"))
            }
        }
    }

    private fun sendSearchResponses(address: InetAddress, port: Int, targets: List<String>) {
        val host = CastNetworkUtil.localIpv4AddressFor(address)
        targets.forEach { target ->
            val payload = searchResponse(target, host)
            sendUdp(payload, address, port)
        }
        scope.launch {
            delay(SEARCH_RESPONSE_REPEAT_DELAY_MS + Random.nextLong(SEARCH_RESPONSE_JITTER_MS))
            targets.forEach { target ->
                sendUdp(searchResponse(target, host), address, port)
            }
        }
    }

    private fun sendAliveNotifications() {
        val group = InetAddress.getByName(CastReceiverConfig.SSDP_ADDRESS)
        val host = CastNetworkUtil.localIpv4Address()
        ssdpTargets().forEach { target ->
            val payload = aliveNotify(target, host)
            sendUdp(payload, group, CastReceiverConfig.SSDP_PORT)
        }
    }

    private fun sendByeByeNotifications() {
        val group = InetAddress.getByName(CastReceiverConfig.SSDP_ADDRESS)
        ssdpTargets().forEach { target ->
            sendUdp(byeByeNotify(target), group, CastReceiverConfig.SSDP_PORT)
        }
    }

    private fun ssdpTargets(): List<String> = listOf(
        "upnp:rootdevice",
        "uuid:$uuid",
        CastReceiverConfig.MEDIA_RENDERER_DEVICE_TYPE,
        CastReceiverConfig.AV_TRANSPORT_SERVICE_TYPE,
        CastReceiverConfig.RENDERING_CONTROL_SERVICE_TYPE,
        CastReceiverConfig.CONNECTION_MANAGER_SERVICE_TYPE,
        CastReceiverConfig.NIRVANA_SERVICE_TYPE
    )

    private fun searchResponse(st: String, host: String): String {
        return """
            HTTP/1.1 200 OK
            CACHE-CONTROL: max-age=1800
            DATE: ${DateHeader.now()}
            EXT:
            LOCATION: http://$host:${CastReceiverConfig.HTTP_PORT}/description.xml
            SERVER: Android/1.0 UPnP/1.0 NeoBV/1.0
            ST: $st
            USN: ${usnFor(st)}
            
        """.trimIndent().replace("\n", "\r\n")
    }

    private fun aliveNotify(nt: String, host: String): String {
        return """
            NOTIFY / HTTP/1.1
            HOST: ${CastReceiverConfig.SSDP_ADDRESS}:${CastReceiverConfig.SSDP_PORT}
            CACHE-CONTROL: max-age=1800
            LOCATION: http://$host:${CastReceiverConfig.HTTP_PORT}/description.xml
            NT: $nt
            NTS: ssdp:alive
            SERVER: Android/1.0 UPnP/1.0 NeoBV/1.0
            USN: ${usnFor(nt)}
            
        """.trimIndent().replace("\n", "\r\n")
    }

    private fun byeByeNotify(nt: String): String {
        return """
            NOTIFY / HTTP/1.1
            HOST: ${CastReceiverConfig.SSDP_ADDRESS}:${CastReceiverConfig.SSDP_PORT}
            NT: $nt
            NTS: ssdp:byebye
            USN: ${usnFor(nt)}
            
        """.trimIndent().replace("\n", "\r\n")
    }

    private fun usnFor(target: String): String =
        if (target == "uuid:$uuid") {
            "uuid:$uuid"
        } else {
            "uuid:$uuid::$target"
        }

    private fun sendUdp(payload: String, address: InetAddress, port: Int) {
        runCatching {
            DatagramSocket().use { socket ->
                val bytes = payload.toByteArray(Charsets.UTF_8)
                socket.send(DatagramPacket(bytes, bytes.size, address, port))
            }
        }.onFailure {
            logger.warn(it) { "Send SSDP packet failed to ${address.hostAddress}:$port" }
        }
    }

    private fun String.oneLine(): String =
        replace('\r', ' ').replace('\n', ' ').take(2048)

    private companion object {
        const val STARTUP_ALIVE_DELAY_MS = 200L
        const val STARTUP_NOTIFY_BURSTS = 3
        const val STARTUP_NOTIFY_INTERVAL_MS = 1_000L
        const val REGULAR_NOTIFY_INTERVAL_MS = 30_000L
        const val SEARCH_RESPONSE_REPEAT_DELAY_MS = 80L
        const val SEARCH_RESPONSE_JITTER_MS = 120L
    }
}
