package com.example.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

object ViralMediaStorage {

    private const val VIDEOS_DIR = "viral_videos"
    private const val RECEIPTS_DIR = "viral_receipts"
    const val REQUIRED_FEE_PKR = 100

    fun isValidPakistaniPhone(raw: String): Boolean {
        val cleaned = raw.trim().replace(" ", "").replace("-", "")
        return Regex("^(03\\d{9}|\\+?923\\d{9})$").matches(cleaned)
    }

    fun normalizePakistaniPhone(raw: String): String {
        val cleaned = raw.trim().replace(" ", "").replace("-", "")
        return when {
            cleaned.startsWith("+92") -> "0" + cleaned.removePrefix("+92")
            cleaned.startsWith("92") && cleaned.length == 12 -> "0" + cleaned.removePrefix("92")
            else -> cleaned
        }
    }

    fun isValidEasypaisaTid(raw: String): Boolean {
        val trimmed = raw.trim()
        return Regex("^[A-Za-z0-9\\-]{6,24}$").matches(trimmed)
    }

    fun isSafeHttpUrl(raw: String): Boolean {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return false
        val lower = trimmed.lowercase()
        if (!lower.startsWith("https://") && !lower.startsWith("http://")) return false
        val afterScheme = trimmed.substringAfter("://")
        val host = afterScheme.substringBefore("/").substringBefore("?").substringBefore(":")
        return host.isNotBlank() && host.contains(".") && !trimmed.contains(" ")
    }

    suspend fun copyPickedVideoToInternal(context: Context, sourceUri: Uri): String? =
        withContext(Dispatchers.IO) {
            copyUriToSubdir(context, sourceUri, VIDEOS_DIR, ".mp4")
        }

    suspend fun copyPickedReceiptToInternal(context: Context, sourceUri: Uri): String? =
        withContext(Dispatchers.IO) {
            copyUriToSubdir(context, sourceUri, RECEIPTS_DIR, ".jpg")
        }

    fun deleteStoredFileIfOwned(context: Context, pathOrUri: String) {
        if (pathOrUri.isBlank()) return
        try {
            val file = if (pathOrUri.startsWith("file://")) {
                File(Uri.parse(pathOrUri).path ?: return)
            } else if (pathOrUri.startsWith("/")) {
                File(pathOrUri)
            } else {
                return
            }
            val filesDirCanonical = context.filesDir.canonicalPath
            val targetCanonical = file.canonicalPath
            if (targetCanonical.startsWith("$filesDirCanonical${File.separator}")) {
                file.delete()
            }
        } catch (_: Exception) {
        }
    }

    private fun copyUriToSubdir(
        context: Context,
        sourceUri: Uri,
        subdirName: String,
        defaultExt: String
    ): String? {
        return try {
            val dir = File(context.filesDir, subdirName)
            if (!dir.exists()) dir.mkdirs()
            val fileName = "${UUID.randomUUID()}$defaultExt"
            val destFile = File(dir, fileName)
            val resolvedDest = destFile.canonicalPath
            if (!resolvedDest.startsWith("${dir.canonicalPath}${File.separator}")) {
                return null
            }
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return null
            if (destFile.length() == 0L) {
                destFile.delete()
                null
            } else {
                Uri.fromFile(destFile).toString()
            }
        } catch (_: Exception) {
            null
        }
    }
}
