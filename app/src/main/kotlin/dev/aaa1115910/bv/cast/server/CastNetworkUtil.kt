package dev.aaa1115910.bv.cast.server

import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.DatagramSocket

object CastNetworkUtil {
    fun localIpv4Address(): String =
        NetworkInterface.getNetworkInterfaces()
            .asSequence()
            .filter { it.isUp && !it.isLoopback }
            .flatMap { it.inetAddresses.asSequence() }
            .filterIsInstance<Inet4Address>()
            .firstOrNull { !it.isLoopbackAddress && !it.hostAddress.orEmpty().startsWith("169.254.") }
            ?.hostAddress
            ?: "127.0.0.1"

    fun localIpv4Addresses(): List<String> =
        NetworkInterface.getNetworkInterfaces()
            .asSequence()
            .filter { it.isUp && !it.isLoopback }
            .flatMap { it.inetAddresses.asSequence() }
            .filterIsInstance<Inet4Address>()
            .filter { !it.isLoopbackAddress && !it.hostAddress.orEmpty().startsWith("169.254.") }
            .mapNotNull { it.hostAddress }
            .toList()
            .ifEmpty { listOf("127.0.0.1") }

    fun localIpv4AddressFor(remoteAddress: InetAddress): String =
        runCatching {
            DatagramSocket().use { socket ->
                socket.connect(remoteAddress, CastReceiverConfig.SSDP_PORT)
                (socket.localAddress as? Inet4Address)
                    ?.takeUnless { it.isLoopbackAddress }
                    ?.hostAddress
            }
        }.getOrNull()
            ?: localIpv4Address()
}
