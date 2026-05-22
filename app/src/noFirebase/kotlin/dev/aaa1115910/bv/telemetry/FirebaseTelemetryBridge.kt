package dev.aaa1115910.bv.telemetry

import android.app.Application

internal object FirebaseTelemetryBridge {
    fun initialize(application: Application) = Unit
    fun setCrashCollectionEnabled(enabled: Boolean) = Unit
    fun setAnalyticsCollectionEnabled(enabled: Boolean) = Unit
    fun setCustomKey(key: String, value: Any) = Unit
    fun recordException(throwable: Throwable, keys: Map<String, Any>) = Unit
    fun logEvent(name: String, params: Map<String, Any>) = Unit
}
