package dev.aaa1115910.bv.telemetry

import android.app.Activity
import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import dev.aaa1115910.biliapi.http.entity.AuthFailureException
import dev.aaa1115910.bv.BuildConfig
import dev.aaa1115910.bv.util.Prefs
import io.ktor.client.plugins.ResponseException
import java.io.IOException
import java.time.LocalDate
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.TimeoutException
import kotlin.coroutines.cancellation.CancellationException

object FirebaseTelemetry {
    private val nonFatalLastReportMs = ConcurrentHashMap<String, Long>()
    private const val nonFatalMinIntervalMs = 10 * 60 * 1000L

    private var initialized = false
    private var lastDailyActiveDate = ""
    private var applicationContext: Context? = null
    private val startedActivityCount = AtomicInteger(0)

    fun initialize(application: Application) {
        if (initialized) return
        initialized = true
        applicationContext = application.applicationContext
        FirebaseTelemetryBridge.initialize(application)
        applyConsent()
        setBaseKeys()
        setAppInForeground(false)
        registerForegroundTracking(application)
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
        if (enabled) {
            logDailyActive("foreground")
        }
    }

    fun setLastScreen(screen: TelemetryScreen) {
        FirebaseTelemetryBridge.setCustomKey("last_screen", screen.key)
        if (Prefs.enableAnonymousUsageCollection) {
            logEvent("screen_view", mapOf("screen_name" to screen.key))
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

    fun reportVideoError(
        type: TelemetryErrorType,
        throwable: Throwable,
        extras: Map<String, Any?> = emptyMap()
    ) {
        if (throwable is CancellationException) return
        reportNonFatal(
            domain = TelemetryErrorDomain.Video,
            type = type,
            throwable = throwable,
            extras = extras
        )
        logVideoError(
            errorType = type,
            playerType = Prefs.playerType.name,
            networkType = detectNetworkType()
        )
    }

    fun reportLiveError(
        type: TelemetryErrorType,
        throwable: Throwable,
        extras: Map<String, Any?> = emptyMap()
    ) {
        if (throwable is CancellationException) return
        reportNonFatal(
            domain = TelemetryErrorDomain.Live,
            type = type,
            throwable = throwable,
            extras = extras
        )
        logLiveError(
            errorType = type,
            networkType = detectNetworkType()
        )
    }

    fun reportDanmakuError(
        type: TelemetryErrorType,
        throwable: Throwable,
        extras: Map<String, Any?> = emptyMap()
    ) {
        if (throwable is CancellationException) return
        reportNonFatal(
            domain = TelemetryErrorDomain.Danmaku,
            type = type,
            throwable = throwable,
            extras = extras
        )
    }

    fun reportApiError(
        throwable: Throwable,
        endpoint: String,
        httpCode: Int? = null,
        extras: Map<String, Any?> = emptyMap()
    ) {
        if (throwable is CancellationException) return
        val type = classifyThrowable(throwable)
        reportNonFatal(
            domain = TelemetryErrorDomain.Api,
            type = type,
            throwable = throwable,
            extras = buildMap {
                put("api_endpoint", endpoint)
                httpCode?.let { put("http_code", it) }
                putAll(extras)
            }
        )
    }

    fun reportDatabaseError(
        throwable: Throwable,
        extras: Map<String, Any?> = emptyMap()
    ) {
        if (throwable is CancellationException) return
        reportNonFatal(
            domain = TelemetryErrorDomain.Database,
            type = classifyThrowable(throwable),
            throwable = throwable,
            extras = extras
        )
    }

    fun classifyThrowable(throwable: Throwable): TelemetryErrorType {
        return when (throwable) {
            is AuthFailureException -> TelemetryErrorType.AuthExpired
            is ResponseException -> {
                when (throwable.response.status.value) {
                    401, 403 -> TelemetryErrorType.AuthExpired
                    else -> TelemetryErrorType.NetworkError
                }
            }

            is IOException -> TelemetryErrorType.NetworkError
            is TimeoutException -> TelemetryErrorType.Timeout
            is IllegalArgumentException -> TelemetryErrorType.ParseError
            else -> when (throwable.cause) {
                null -> TelemetryErrorType.Unknown
                else -> classifyThrowable(throwable.cause!!)
            }
        }
    }

    fun logDailyActive(source: String) {
        if (!Prefs.enableAnonymousUsageCollection) return
        val today = LocalDate.now().toString()
        if (lastDailyActiveDate == today) return
        lastDailyActiveDate = today
        val safeSource = when (source) {
            "app_start", "foreground" -> source
            else -> "foreground"
        }
        setLastEvent(TelemetryEvent.DailyActive)
        logEvent(
            TelemetryEvent.DailyActive.key,
            mapOf(
                "app_version" to BuildConfig.VERSION_NAME,
                "build_type" to BuildConfig.BUILD_TYPE,
                "local_date" to today,
                "source" to safeSource
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
        logEvent(
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
        logEvent(
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
        logEvent(TelemetryEvent.AppOpen.key, emptyMap())
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

    private fun logEvent(name: String, params: Map<String, Any?>) {
        FirebaseTelemetryBridge.logEvent(name, TelemetrySanitizer.sanitizeEventParams(params))
    }

    private fun setAppInForeground(value: Boolean) {
        FirebaseTelemetryBridge.setCustomKey("app_in_foreground", value)
    }

    private fun registerForegroundTracking(application: Application) {
        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: Activity) {
                if (startedActivityCount.getAndIncrement() == 0) {
                    setAppInForeground(true)
                    logDailyActive("foreground")
                }
            }

            override fun onActivityStopped(activity: Activity) {
                if (startedActivityCount.decrementAndGet().coerceAtLeast(0) == 0) {
                    startedActivityCount.set(0)
                    setAppInForeground(false)
                }
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityResumed(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
    }

    private fun detectNetworkType(): TelemetryNetworkType {
        val context = applicationContext ?: return TelemetryNetworkType.Unknown
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return TelemetryNetworkType.Unknown
        val network = connectivityManager.activeNetwork ?: return TelemetryNetworkType.Unknown
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return TelemetryNetworkType.Unknown
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> TelemetryNetworkType.Wifi
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> TelemetryNetworkType.Cellular
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> TelemetryNetworkType.Ethernet
            else -> TelemetryNetworkType.Unknown
        }
    }
}
