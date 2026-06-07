package dev.aaa1115910.bv.cast.server

import android.content.Context
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CastRequestLogger(context: Context) {
    private val logger = KotlinLogging.logger("CastReceiver")
    private val logFile = File(context.filesDir, CastReceiverConfig.LOG_FILE_NAME)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

    @Synchronized
    fun log(message: String) {
        logger.info { message }
        rotateIfNeeded()
        logFile.appendText("[${dateFormat.format(Date())}] $message\n")
    }

    @Synchronized
    fun logRequest(
        method: String,
        path: String,
        remoteHost: String?,
        headers: Map<String, List<String>>,
        body: String?
    ) {
        val safeBody = body
            ?.take(4096)
            ?.replace('\r', ' ')
            ?.replace('\n', ' ')
            ?.takeIf { it.isNotBlank() }
        log(
            buildString {
                append("request method=").append(method)
                append(" path=").append(path)
                append(" remote=").append(remoteHost ?: "unknown")
                append(" headers=").append(headers.entries.joinToString(";") { "${it.key}=${it.value.joinToString("|")}" })
                if (safeBody != null) append(" body=").append(safeBody)
            }
        )
    }

    private fun rotateIfNeeded() {
        if (logFile.exists() && logFile.length() > CastReceiverConfig.MAX_LOG_BYTES) {
            val rotated = File(logFile.parentFile, "${CastReceiverConfig.LOG_FILE_NAME}.old")
            if (rotated.exists()) rotated.delete()
            logFile.renameTo(rotated)
        }
    }
}

