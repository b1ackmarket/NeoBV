package dev.aaa1115910.bv.cast.server

import dev.aaa1115910.bv.BuildConfig

object CastReceiverConfig {
    val HTTP_PORT = BuildConfig.CAST_RECEIVER_HTTP_PORT
    const val SSDP_PORT = 1900
    const val SSDP_ADDRESS = "239.255.255.250"
    const val DEVICE_NAME = "NeoBV"
    const val MANUFACTURER = "Bilibili Inc."
    const val MODEL_NAME = "NeoBV Cast Receiver"
    const val MODEL_NUMBER = "1"
    const val OFFICIAL_YST_PACKAGE_NAME = "com.xiaodianshi.tv.yst"
    const val LOG_FILE_NAME = "cast_receiver_requests.log"
    const val MAX_LOG_BYTES = 512 * 1024L

    const val MEDIA_RENDERER_DEVICE_TYPE = "urn:schemas-upnp-org:device:MediaRenderer:1"
    const val AV_TRANSPORT_SERVICE_TYPE = "urn:schemas-upnp-org:service:AVTransport:1"
    const val RENDERING_CONTROL_SERVICE_TYPE = "urn:schemas-upnp-org:service:RenderingControl:1"
    const val CONNECTION_MANAGER_SERVICE_TYPE = "urn:schemas-upnp-org:service:ConnectionManager:1"
    const val NIRVANA_SERVICE_TYPE = "urn:app-bilibili-com:service:NirvanaControl:3"
}
