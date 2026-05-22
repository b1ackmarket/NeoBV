package dev.aaa1115910.bv.telemetry

import android.app.Application
import android.os.Bundle
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics

internal object FirebaseTelemetryBridge {
    private var analytics: FirebaseAnalytics? = null
    private var crashlytics: FirebaseCrashlytics? = null

    fun initialize(application: Application) {
        runCatching {
            FirebaseApp.initializeApp(application)
            analytics = FirebaseAnalytics.getInstance(application)
            crashlytics = FirebaseCrashlytics.getInstance()
        }
    }

    fun setCrashCollectionEnabled(enabled: Boolean) {
        crashlytics?.setCrashlyticsCollectionEnabled(enabled)
        if (enabled) {
            crashlytics?.sendUnsentReports()
        } else {
            crashlytics?.deleteUnsentReports()
        }
    }

    fun setAnalyticsCollectionEnabled(enabled: Boolean) {
        analytics?.setAnalyticsCollectionEnabled(enabled)
        if (!enabled) analytics?.resetAnalyticsData()
    }

    fun setCustomKey(key: String, value: Any) {
        when (value) {
            is Boolean -> crashlytics?.setCustomKey(key, value)
            is Double -> crashlytics?.setCustomKey(key, value)
            is Float -> crashlytics?.setCustomKey(key, value)
            is Int -> crashlytics?.setCustomKey(key, value)
            is Long -> crashlytics?.setCustomKey(key, value)
            else -> crashlytics?.setCustomKey(key, value.toString())
        }
    }

    fun recordException(throwable: Throwable, keys: Map<String, Any>) {
        keys.forEach { (key, value) -> setCustomKey(key, value) }
        crashlytics?.recordException(throwable)
    }

    fun logEvent(name: String, params: Map<String, Any>) {
        val bundle = Bundle().apply {
            params.forEach { (key, value) ->
                when (value) {
                    is Boolean -> putString(key, value.toString())
                    is Double -> putDouble(key, value)
                    is Float -> putDouble(key, value.toDouble())
                    is Int -> putInt(key, value)
                    is Long -> putLong(key, value)
                    else -> putString(key, value.toString())
                }
            }
        }
        analytics?.logEvent(name, bundle)
    }
}
