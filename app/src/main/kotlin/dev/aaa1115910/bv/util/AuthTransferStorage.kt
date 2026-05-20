package dev.aaa1115910.bv.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import dev.aaa1115910.bv.entity.AuthData
import dev.aaa1115910.bv.entity.AppDataTransferFile
import dev.aaa1115910.bv.entity.AuthTransferFile
import java.io.File
import java.io.IOException
import java.util.Date

data class AuthExportResult(
    val displayPath: String,
    val uri: Uri? = null
)

object AuthTransferStorage {
    private const val MIME_TYPE_JSON = "application/json"

    fun exportToDocuments(
        context: Context,
        authData: AuthData,
        now: Date = Date()
    ): AuthExportResult {
        val fileName = AuthTransferFile.defaultFileName(authData, now)
        val content = AuthTransferFile.encode(authData)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            exportWithMediaStore(context, fileName, content)
        } else {
            exportWithLegacyDocuments(fileName, content)
        }
    }

    fun importFromUri(
        context: Context,
        uri: Uri
    ): AppDataTransferFile {
        val content = context.contentResolver.openInputStream(uri)
            ?.bufferedReader()
            ?.use { it.readText() }
            ?: throw IOException("无法读取导入文件")
        return AuthTransferFile.decodeTransfer(content)
    }

    fun resolveDisplayName(
        context: Context,
        uri: Uri
    ): String {
        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) {
                return cursor.getString(nameIndex)
            }
        }
        return uri.lastPathSegment ?: uri.toString()
    }

    private fun exportWithMediaStore(
        context: Context,
        fileName: String,
        content: String
    ): AuthExportResult {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, MIME_TYPE_JSON)
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val collection = MediaStore.Files.getContentUri("external")
        val uri = context.contentResolver.insert(collection, values)
            ?: throw IOException("无法创建导出文件")
        runCatching {
            context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                writer.write(content)
            } ?: throw IOException("无法写入导出文件")
        }.onFailure {
            context.contentResolver.delete(uri, null, null)
            throw it
        }
        values.clear()
        values.put(MediaStore.MediaColumns.IS_PENDING, 0)
        context.contentResolver.update(uri, values, null, null)
        return AuthExportResult(
            displayPath = "Documents/$fileName",
            uri = uri
        )
    }

    private fun exportWithLegacyDocuments(
        fileName: String,
        content: String
    ): AuthExportResult {
        val directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        if (!directory.exists() && !directory.mkdirs()) {
            throw IOException("无法创建 Documents 目录")
        }
        val file = File(directory, fileName)
        file.writeText(content)
        return AuthExportResult(
            displayPath = file.absolutePath,
            uri = null
        )
    }
}
