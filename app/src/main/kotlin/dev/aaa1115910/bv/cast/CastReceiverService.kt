package dev.aaa1115910.bv.cast

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.IBinder
import dev.aaa1115910.bv.cast.server.CastHttpServer
import dev.aaa1115910.bv.cast.server.CastNetworkUtil
import dev.aaa1115910.bv.cast.server.CastRequestLogger
import dev.aaa1115910.bv.cast.server.CastSsdpServer
import dev.aaa1115910.bv.util.Prefs
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import java.util.UUID

class CastReceiverService : Service() {
    private val logger = KotlinLogging.logger("CastReceiverService")
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var multicastLock: WifiManager.MulticastLock? = null
    private var httpServer: CastHttpServer? = null
    private var ssdpServer: CastSsdpServer? = null

    override fun onCreate() {
        super.onCreate()
        val requestLogger = CastRequestLogger(applicationContext)
        val uuid = Prefs.castReceiverUuid.ifBlank {
            UUID.randomUUID().toString().also { Prefs.castReceiverUuid = it }
        }
        acquireMulticastLock()
        httpServer = CastHttpServer(
            uuid = uuid,
            requestLogger = requestLogger,
            playbackLauncher = CastPlaybackLauncher(applicationContext),
            scope = scope
        )
        ssdpServer = CastSsdpServer(
            uuid = uuid,
            requestLogger = requestLogger,
            scope = scope
        )
        runCatching {
            httpServer?.start()
            ssdpServer?.start()
            requestLogger.log("Cast receiver active addresses=${CastNetworkUtil.localIpv4Addresses()}")
        }.onFailure {
            logger.warn(it) { "Start cast receiver failed" }
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        ssdpServer?.stop()
        httpServer?.stop()
        ssdpServer = null
        httpServer = null
        multicastLock?.release()
        multicastLock = null
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun acquireMulticastLock() {
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return
        multicastLock = wifiManager.createMulticastLock("NeoBV-CastReceiver").apply {
            setReferenceCounted(false)
            acquire()
        }
    }

    companion object {
        fun start(context: Context) {
            context.startService(Intent(context, CastReceiverService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, CastReceiverService::class.java))
        }
    }
}

