package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.core.content.FileProvider
import com.example.data.ViralMediaStorage
import com.example.util.QrCodeGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class PlatformActions(context: Context) {
    private val appContext: Context = context.applicationContext

    fun copyToClipboard(label: String, text: String) {
        val clipboard = appContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    }

    fun shareReferralText(chooserTitle: String, shareBody: String) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareBody)
            type = "text/plain"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val chooser = Intent.createChooser(sendIntent, chooserTitle).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        appContext.startActivity(chooser)
    }

    suspend fun shareAppQrImageToGroups(downloadUrl: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val qrBmp = QrCodeGenerator.generateBitmap(
                    text = downloadUrl,
                    pixelSize = 760,
                    darkColor = Color(0xFF05070A),
                    lightColor = Color.White
                )

                val cardWidth = 900
                val cardHeight = 1040
                val cardBitmap = Bitmap.createBitmap(cardWidth, cardHeight, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(cardBitmap)
                canvas.drawColor(android.graphics.Color.parseColor("#0A0F16"))

                val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.WHITE
                }
                val qrCardRect = RectF(70f, 130f, cardWidth - 70f, 890f)
                canvas.drawRoundRect(qrCardRect, 36f, 36f, whitePaint)
                canvas.drawBitmap(qrBmp, 70f, 130f, null)

                val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.parseColor("#F59E0B")
                    textSize = 46f
                    isFakeBoldText = true
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText("REFREELS APP", cardWidth / 2f, 82f, titlePaint)

                val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.parseColor("#E2ECFA")
                    textSize = 28f
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText("Scan QR to download RefReels", cardWidth / 2f, 965f, footerPaint)

                val outDir = File(appContext.cacheDir, "shared_qr").apply { mkdirs() }
                val outFile = File(outDir, "refreels_share_qr.png")
                FileOutputStream(outFile).use { stream ->
                    cardBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                }

                val contentUri = FileProvider.getUriForFile(
                    appContext,
                    "${appContext.packageName}.fileprovider",
                    outFile
                )

                val inviteText = "Install RefReels to discover verified referral links, QR codes & viral promo videos:\n$downloadUrl"
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    putExtra(Intent.EXTRA_TEXT, inviteText)
                    clipData = ClipData.newRawUri("RefReels QR", contentUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                val chooser = Intent.createChooser(shareIntent, "Share RefReels QR to Groups").apply {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                withContext(Dispatchers.Main) {
                    appContext.startActivity(chooser)
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun shareAppApkToGroups(): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val sourceApkPath = appContext.applicationInfo.sourceDir
                val sourceApk = File(sourceApkPath)
                if (!sourceApk.exists()) {
                    return@withContext Result.failure(IllegalStateException("App APK not accessible on device."))
                }

                val outDir = File(appContext.cacheDir, "shared_apk").apply { mkdirs() }
                val destApk = File(outDir, "RefReels.apk")
                sourceApk.inputStream().use { input ->
                    destApk.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                val contentUri = FileProvider.getUriForFile(
                    appContext,
                    "${appContext.packageName}.fileprovider",
                    destApk
                )

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/vnd.android.package-archive"
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    putExtra(Intent.EXTRA_SUBJECT, "RefReels App APK")
                    clipData = ClipData.newRawUri("RefReels APK", contentUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                val chooser = Intent.createChooser(shareIntent, "Send RefReels APK to Groups").apply {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                withContext(Dispatchers.Main) {
                    appContext.startActivity(chooser)
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    fun openEasypaisaApp(): Boolean {
        val pm = appContext.packageManager
        val launchIntent = pm.getLaunchIntentForPackage("pk.com.telenor.phoenix")
            ?: pm.getLaunchIntentForPackage("com.easypaisa")
        return if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            appContext.startActivity(launchIntent)
            true
        } else {
            false
        }
    }

    suspend fun copyPickedVideoToInternal(uri: Uri): String? {
        return ViralMediaStorage.copyPickedVideoToInternal(appContext, uri)
    }

    fun deleteLocalFile(path: String) {
        ViralMediaStorage.deleteStoredFileIfOwned(appContext, path)
    }
}
