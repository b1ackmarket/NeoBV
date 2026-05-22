package dev.aaa1115910.bv.telemetry

import android.app.Application
import android.os.Build
import dev.aaa1115910.bv.BuildConfig
import dev.aaa1115910.bv.util.Prefs
import java.time.LocalDate
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

object FirebaseTelemetry {
    private val nonFatalLastReportMs = ConcurrentHashMap<String, Long>()
    private const val nonFatalMinIntervalMs = 10 * 60 * 1000L

    private var initialized = false
    private var lastDailyActiveDate = ""

    fun initialize(application: Application) {
        if (initialized) return
        initialized = true
        FirebaseTelemetryBridge.initialize(application)
        applyConsent()
        setBaseKeys()
        setLastScreen(TelemetryScreen.AppStart)
        logAppOpen()
        logDailyActive("app_start")
    }

    fun applyConsent() {
        FirebaseTelemetryBridge.setCrashCollectionEnabled(Prefs.enableCrashReportCollection)
        FirebaseTelemetryBridge.setAnalyticsCollectionEnabled(Prefs.enableAnonymousUsageCollection)
    }

    fun setCrashReportCollectionEnabled(enabled: Boolean) {
        Prefs.enableCrashReportCollection = enabled
        FirebaseTelemetryBridge.setCrashCollectionEnabled(enabled)
    }

    fun setAnonymousUsageCollectionEnabled(enabled: Boolean) {
        Prefs.enableAnonymousUsageCollection = enabled
        FirebaseTelemetryBridge.setAnalyticsCollectionEnabled(enabled)
    }

    fun setLastScreen(screen: TelemetryScreen) {
        FirebaseTelemetryBridge.setCustomKey("last_screen", screen.key)
        if (Prefs.enableAnonymousUsageCollection) {
            FirebaseTelemetryBridge.logEvent("screen_view", mapOf("screen_name" to screen.key))
        }
    }

    fun setLastEvent(event: TelemetryEvent) {
        FirebaseTelemetryBridge.setCustomKey("last_event", event.key)
    }

    fun reportNonFatal(
        domain: TelemetryErrorDomain,
        type: TelemetryErrorType,
        throwable: Throwable,
        extras: Map<String, Any?> = emptyMap()
    ) {
        if (!Prefs.enableCrashReportCollection) return
        val key = "${domain.key}:${type.key}"
        val now = System.currentTimeMillis()
        val lastReportMs = nonFatalLastReportMs[key] ?: 0L
        if (now - lastReportMs < nonFatalMinIntervalMs) return
        nonFatalLastReportMs[key] = now

        setLastEvent(
            when (domain) {
                TelemetryErrorDomain.Video -> TelemetryEvent.VideoError
                TelemetryErrorDomain.Live -> TelemetryEvent.LiveError
                TelemetryErrorDomain.Danmaku -> TelemetryEvent.DanmakuError
                TelemetryErrorDomain.Api -> TelemetryEvent.ApiError
                TelemetryErrorDomain.Database,
                TelemetryErrorDomain.Startup -> TelemetryEvent.ApiError
            }
        )
        val params = buildMap {
            put("error_domain", domain.key)
            put("error_type", type.key)
            putAll(TelemetrySanitizer.sanitizeExtras(extras))
        }
        FirebaseTelemetryBridge.recordException(throwable, params)
    }

    fun logDailyActive(source: String) {
        if (!Prefs.enableAnonymousUsageCollection) return
        val today = LocalDate.now().toString()
        if (lastDailyActiveDate == today) return
        lastDailyActiveDate = today
        setLastEvent(TelemetryEvent.DailyActive)
        FirebaseTelemetryBridge.logEvent(
            TelemetryEvent.DailyActive.key,
            mapOf(
                "app_version" to BuildConfig.VERSION_NAME,
                "build_type" to BuildConfig.BUILD_TYPE,
                "local_date" to today,
                "source" to source.take(32)
            )
        )
    }

    fun logVideoError(
        errorType: TelemetryErrorType,
        playerType: String,
        networkType: TelemetryNetworkType
    ) {
        if (!Prefs.enableAnonymousUsageCollection) return
        setLastEvent(TelemetryEvent.VideoError)
        FirebaseTelemetryBridge.logEvent(
            TelemetryEvent.VideoError.key,
            mapOf(
                "error_type" to errorType.key,
                "player_type" to playerType.take(32),
                "network_type" to networkType.key
            )
        )
    }

    fun logLiveError(errorType: TelemetryErrorType, networkType: TelemetryNetworkType) {
        if (!Prefs.enableAnonymousUsageCollection) return
        setLastEvent(TelemetryEvent.LiveError)
        FirebaseTelemetryBridge.logEvent(
            TelemetryEvent.LiveError.key,
            mapOf(
                "error_type" to errorType.key,
                "network_type" to networkType.key
            )
        )
    }

    private fun logAppOpen() {
        if (!Prefs.enableAnonymousUsageCollection) return
        setLastEvent(TelemetryEvent.AppOpen)
        FirebaseTelemetryBridge.logEvent(TelemetryEvent.AppOpen.key, emptyMap())
    }

    private fun setBaseKeys() {
        FirebaseTelemetryBridge.setCustomKey("app_version", BuildConfig.VERSION_NAME)
        FirebaseTelemetryBridge.setCustomKey("version_code", BuildConfig.VERSION_CODE)
        FirebaseTelemetryBridge.setCustomKey("build_type", BuildConfig.BUILD_TYPE)
        FirebaseTelemetryBridge.setCustomKey("android_sdk", Build.VERSION.SDK_INT)
        FirebaseTelemetryBridge.setCustomKey("device_brand", Build.BRAND.take(40))
        FirebaseTelemetryBridge.setCustomKey("device_model", Build.MODEL.take(60))
        FirebaseTelemetryBridge.setCustomKey(
            "supported_abis",
            Build.SUPPORTED_ABIS.joinToString(",").lowercase(Locale.ROOT).take(100)
        )
        FirebaseTelemetryBridge.setCustomKey("player_type", Prefs.playerType.name)
        FirebaseTelemetryBridge.setCustomKey("video_codec_pref", Prefs.defaultVideoCodec.name)
        FirebaseTelemetryBridge.setCustomKey("default_quality", Prefs.defaultQuality.name)
        FirebaseTelemetryBridge.setCustomKey("danmaku_enabled", Prefs.defaultDanmakuTypes.isNotEmpty())
        FirebaseTelemetryBridge.setCustomKey("proxy_enabled", Prefs.enableProxy)
        FirebaseTelemetryBridge.setCustomKey("incognito_mode", Prefs.incognitoMode)
    }
}
