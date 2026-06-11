package dev.aaa1115910.bv.util

import android.media.MediaDrm
import android.os.Build
import java.util.UUID

object WidevineUtil {
    private val widevineUuid = UUID(-0x121074568629b532L, -0x5c37d8232ae2de13L)
    @Volatile
    private var cachedInfo: WidevineInfo? = null

    fun isSupported(): Boolean {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                MediaDrm.isCryptoSchemeSupported(widevineUuid)
            } else {
                false
            }
        }.getOrDefault(false)
    }

    fun readInfo(): WidevineInfo {
        cachedInfo?.let { return it }
        if (!isSupported()) return WidevineInfo(isSupported = false).also { cachedInfo = it }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            return WidevineInfo(isSupported = false).also { cachedInfo = it }
        }

        return runCatching {
            val mediaDrm = MediaDrm(widevineUuid)
            try {
                WidevineInfo(
                    isSupported = true,
                    vendor = mediaDrm.getPropertyStringOrNull("vendor"),
                    version = mediaDrm.getPropertyStringOrNull("version"),
                    description = mediaDrm.getPropertyStringOrNull("description"),
                    securityLevel = mediaDrm.getPropertyStringOrNull("securityLevel"),
                    maxNumberOfSessions = mediaDrm.getPropertyStringOrNull("maxNumberOfSessions"),
                    hdcpLevel = mediaDrm.getPropertyStringOrNull("hdcpLevel"),
                    maxHdcpLevel = mediaDrm.getPropertyStringOrNull("maxHdcpLevel"),
                    algorithms = mediaDrm.getPropertyStringOrNull("algorithms"),
                    systemId = mediaDrm.getPropertyStringOrNull("systemId"),
                    privacyMode = mediaDrm.getPropertyStringOrNull("privacyMode"),
                    sessionSharing = mediaDrm.getPropertyStringOrNull("sessionSharing"),
                    usageReportingSupport = mediaDrm.getPropertyStringOrNull("usageReportingSupport"),
                    deviceUniqueId = mediaDrm.getPropertyByteArraySummary("deviceUniqueId")
                )
            } finally {
                mediaDrm.release()
            }
        }.getOrElse {
            WidevineInfo(isSupported = true, error = it.message)
        }.also { cachedInfo = it }
    }

    private fun MediaDrm.getPropertyStringOrNull(name: String): String? {
        return runCatching { getPropertyString(name).takeIf { it.isNotBlank() } }.getOrNull()
    }

    private fun MediaDrm.getPropertyByteArraySummary(name: String): String? {
        return runCatching {
            val bytes = getPropertyByteArray(name).takeIf { it.isNotEmpty() } ?: return@runCatching null
            "可读取（${bytes.size} bytes）"
        }.getOrNull()
    }
}

data class WidevineInfo(
    val isSupported: Boolean,
    val vendor: String? = null,
    val version: String? = null,
    val description: String? = null,
    val securityLevel: String? = null,
    val maxNumberOfSessions: String? = null,
    val hdcpLevel: String? = null,
    val maxHdcpLevel: String? = null,
    val algorithms: String? = null,
    val systemId: String? = null,
    val privacyMode: String? = null,
    val sessionSharing: String? = null,
    val usageReportingSupport: String? = null,
    val deviceUniqueId: String? = null,
    val error: String? = null
) {
    val canTryProtectedPlayback: Boolean
        get() = isSupported && error.isNullOrBlank()
}
