package dev.aaa1115910.bv.entity

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object AuthTransferFile {
    private val fileNameDateFormat = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    fun encode(authData: AuthData): String = authData.toJson()

    fun decode(content: String): AuthData = AuthData.fromJson(content.trim())

    fun defaultFileName(
        authData: AuthData,
        now: Date = Date()
    ): String {
        val timestamp = fileNameDateFormat.format(now)
        return "bv-auth-${authData.uid}-$timestamp.json"
    }
}
