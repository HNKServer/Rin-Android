package one.ethanthesleepy.androidew

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import one.ethanthesleepy.androidew.MainActivity.Settings
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream


@Serializable
data class UpdateResult(
    val version: String
)

class Utilities {
    companion object {
        @JvmStatic
        fun getExternalDataPath(context: Context): String? {
            val fold = context.getExternalFilesDir(null) ?: return null

            return fold.absolutePath
        }

        @JvmStatic
        fun uriToPath(uri: Uri): String? {
            return try {
                if (DocumentsContract.isTreeUri(uri)) {
                    val treeId = DocumentsContract.getTreeDocumentId(uri)
                    val split = treeId.split(":", limit = 2)
                    if (split.size == 2 && split[0].equals("primary", ignoreCase = true)) {
                        if (split[1].isBlank()) "/storage/emulated/0" else "/storage/emulated/0/${split[1]}"
                    } else null
                } else null
            } catch (_: Exception) { null }
        }

        @JvmStatic
        fun getCurrentVersion(context: Context): String? {
            return try {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName
            } catch (_: PackageManager.NameNotFoundException) {
                null
            }
        }

        @JvmStatic
        fun getLatestVersion(onFetchDone: (String?) -> Unit) {
            Thread {
                try {
                    val version = URL("https://git.ethanthesleepy.one/ethanaobrien/ew-android/releases/download/latest/version.json").readText()
                    onFetchDone(Json.decodeFromString<UpdateResult>(version).version)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.d("ew-update", "Failed to check for updates")
                    onFetchDone(null)
                }
            }.start()
        }

        @JvmStatic
        fun getSettings(context: Context): Settings {
            try {
                val settings = File(context.filesDir.absolutePath + "/settings.json").readText()
                return Json.decodeFromString<Settings>(settings)
            } catch(_: Exception) {
                return Settings(launchOnStartup = true, easterMode = false, assetPath = "", masterdataPath = "")
            }
        }

        @SuppressLint("ObsoleteSdkInt")
        @JvmStatic
        fun sendNotification(
            context: Context,
            channelId: String,
            channelName: String,
            contentTitle: String,
            contentText: String
        ) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            // Create NotificationChannel (required for Android O and above)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    channelName,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notification channel for notifications"
                }

                // Register the channel with the system
                notificationManager.createNotificationChannel(channel)
            }

            // Create the notification
            val notification: Notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_service_notification)  // Set the icon for the notification
                .setContentTitle(contentTitle)  // Set the title
                .setContentText(contentText)  // Set the text
                .setPriority(NotificationCompat.PRIORITY_LOW)  // Set priority
                .setAutoCancel(false)  // The notification won't be dismissed when clicked
                .setContentIntent(null)  // Do nothing when clicked
                .build()

            // Show the notification
            notificationManager.notify(0, notification)  // Using 0 as the notification ID
        }

        @Volatile @JvmStatic var isDownloading = false

        @JvmStatic
        @SuppressLint("ObsoleteSdkInt")
        fun downloadFileWithProgress(
            context: Context,
            fileUrl: String,
            destinationPath: String,
            channelId: String,
            channelName: String,
            showNotification: Boolean = true,
            onProgress: ((Int) -> Unit)? = null,
            onDownloadComplete: (String) -> Unit,
            onDownloadError: ((Exception) -> Unit)? = null
        ) {
            isDownloading = true
            val notificationManager = if (showNotification) {
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            } else null

            if (showNotification && notificationManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    channelName,
                    NotificationManager.IMPORTANCE_HIGH
                )
                notificationManager.createNotificationChannel(channel)
            }

            val cancelIntent = Intent(context, DownloadCancelReceiver::class.java)
            val cancelPendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                cancelIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val notificationId = 1
            val notificationBuilder = if (showNotification) {
                NotificationCompat.Builder(context, channelId)
                    .setContentTitle("Downloading update...")
                    .setContentText("Connecting...")
                    .setSmallIcon(android.R.drawable.stat_sys_download)
                    .setProgress(100, 0, true)
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .addAction(
                        android.R.drawable.ic_menu_close_clear_cancel,
                        "Cancel",
                        cancelPendingIntent
                    )
            } else null

            if (showNotification && notificationManager != null && notificationBuilder != null) {
                notificationManager.notify(notificationId, notificationBuilder.build())
            }

            Thread {
                try {
                    val file = File(destinationPath)
                    file.parentFile?.mkdirs()
                    if (file.exists()) file.delete()

                    val url = URL(fileUrl)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.connectTimeout = 10000
                    connection.readTimeout = 15000
                    connection.connect()

                    if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                        throw IOException("Server returned HTTP ${connection.responseCode}")
                    }

                    val totalBytes = connection.contentLength.toLong()
                    
                    connection.inputStream.use { inputStream ->
                        FileOutputStream(file).use { outputStream ->
                            val buffer = ByteArray(8192)
                            var bytesRead: Int
                            var downloadedBytes = 0L
                            var lastProgress = -1

                            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                                if (!isDownloading) break
                                
                                outputStream.write(buffer, 0, bytesRead)
                                downloadedBytes += bytesRead

                                if (totalBytes > 0) {
                                    val progress = (downloadedBytes * 100 / totalBytes).toInt()
                                    if (progress != lastProgress) {
                                        lastProgress = progress
                                        if (showNotification && notificationManager != null && notificationBuilder != null) {
                                            notificationBuilder.setProgress(100, progress, false)
                                                .setContentText("Downloading: $progress%")
                                            notificationManager.notify(notificationId, notificationBuilder.build())
                                        }
                                        onProgress?.invoke(progress)
                                    }
                                } else {
                                    val kbDownloaded = (downloadedBytes / 1024).toInt()
                                    if (kbDownloaded / 100 != lastProgress) {
                                        lastProgress = kbDownloaded / 100
                                        if (showNotification && notificationManager != null && notificationBuilder != null) {
                                            notificationBuilder.setContentText("Downloaded: $kbDownloaded KB")
                                            notificationManager.notify(notificationId, notificationBuilder.build())
                                        }
                                        // For indeterminate progress, maybe send -1 or current KB
                                        onProgress?.invoke(-1)
                                    }
                                }
                            }
                        }
                    }
                    connection.disconnect()

                    if (isDownloading) {
                        if (showNotification && notificationManager != null) {
                            val successNotification = NotificationCompat.Builder(context, channelId)
                                .setContentTitle("Download complete")
                                .setContentText("Update is ready to install")
                                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                                .setOngoing(false)
                                .setAutoCancel(true)
                                .build()
                            
                            notificationManager.notify(notificationId, successNotification)
                        }

                        if (file.extension.lowercase() == "zip") {
                            val apkFile = unzipApk(file)
                            if (apkFile != null) {
                                onDownloadComplete(apkFile.absolutePath)
                                file.delete() // Delete the zip file after extracting
                            } else {
                                throw IOException("No APK found in the zip file")
                            }
                        } else {
                            onDownloadComplete(file.absolutePath)
                        }
                    } else {
                        if (file.exists()) file.delete()
                        if (showNotification && notificationManager != null) {
                            notificationManager.cancel(notificationId)
                        }
                    }

                } catch (e: Exception) {
                    Log.e("ew-update", "Download failed", e)
                    if (showNotification && notificationManager != null) {
                        val errorNotification = NotificationCompat.Builder(context, channelId)
                            .setContentTitle("Download failed")
                            .setContentText(e.localizedMessage ?: "Connection error")
                            .setSmallIcon(android.R.drawable.stat_notify_error)
                            .setOngoing(false)
                            .build()
                        notificationManager.notify(notificationId, errorNotification)
                    }
                    onDownloadError?.invoke(e)
                } finally {
                    isDownloading = false
                }
            }.start()
        }

        private fun unzipApk(zipFile: File): File? {
            val outputDir = zipFile.parentFile ?: return null
            var apkFile: File? = null

            ZipInputStream(zipFile.inputStream()).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory && entry.name.lowercase().endsWith(".apk")) {
                        val outFile = File(outputDir, entry.name)
                        outFile.parentFile?.mkdirs()
                        FileOutputStream(outFile).use { fos ->
                            zis.copyTo(fos)
                        }
                        apkFile = outFile
                        zis.closeEntry()
                        break // Assume only one APK per zip for now
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            return apkFile
        }

        @JvmStatic
        fun clearCache(context: Context) {
            val dir = File(context.filesDir.absolutePath + "/temp/")
            if (dir.isDirectory) {
                val children = dir.list()
                if (children != null) {
                    for (i in children.indices) {
                        File(dir, children[i]).delete()
                    }
                }
            }
        }

        @SuppressLint("ObsoleteSdkInt")
        @JvmStatic
        fun installFile(context: Context, apkPath: String): Boolean {
            val file = File(apkPath)

            if (file.exists()) {
                val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.provider",  // Replace with your own provider name in AndroidManifest
                        file
                    )
                } else {
                    Uri.fromFile(file)
                }

                val installIntent = Intent(Intent.ACTION_VIEW)
                installIntent.setDataAndType(uri, "application/vnd.android.package-archive")
                installIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION

                context.grantUriPermission(
                    context.packageName,
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )

                context.startActivity(installIntent)
                return true
            } else {
                Log.d("ew-installer", "APK file does not exist at $apkPath")
                return false
            }
        }
    }
}

class DownloadCancelReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Utilities.isDownloading = false
    }
}
