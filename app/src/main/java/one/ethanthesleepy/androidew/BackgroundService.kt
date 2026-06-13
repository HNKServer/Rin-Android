package one.ethanthesleepy.androidew

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat


class BackgroundService : Service() {
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun startServer() {
        MainActivity.Instance?.setStatusText(getString(R.string.starting))
        var folder = Utilities.getExternalDataPath(this)
        if (folder == null) {
            MainActivity.Instance?.setStatusText(getString(R.string.storage_not_allowed))
            return
        }

        folder += "/ew_data/"
        val settings = Utilities.getSettings(this)
        val assetPath = settings.assetPath.ifBlank { folder + "assets" }
        val masterdataPath = settings.masterdataPath.ifBlank { folder + "masterdata" }
        Log.d("rust", "Starting server with data directory: $folder")
        Log.d("rust", "Asset CDN directory: $assetPath")
        Log.d("rust", "Masterdata directory: $masterdataPath")

        val startServer = startServer(folder, assetPath, masterdataPath, settings.easterMode)
        Log.d("rust", startServer)

        Log.d("rust", "It didn't crash!")

        MainActivity.Instance?.setStatusText(getString(R.string.server_started))
        MainActivity.Instance?.setButtonText(getString(R.string.stop_server))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Instance = this
        createNotification()

        startServer()

        return START_STICKY
    }

    private fun createNotificationChannel(): String{
        val channelId = "ew_server"
        val channelName = "Server Running"
        val chan = NotificationChannel(channelId,
            channelName, NotificationManager.IMPORTANCE_LOW)
        chan.lightColor = Color.BLUE
        chan.importance = NotificationManager.IMPORTANCE_LOW
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return channelId
    }
    private fun createNotification() {
        val channelId = createNotificationChannel()

        val notificationBuilder = NotificationCompat.Builder(this, channelId)

        val notificationIntent = Intent(
            this,
            MainActivity::class.java
        )
        notificationIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        val contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        val notification = notificationBuilder.setOngoing(true)
            .setSmallIcon(R.drawable.ic_service_notification)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setPriority(NotificationCompat.PRIORITY_HIGH) // High - so we have a lock on power
            .setCategory(Notification.CATEGORY_SERVICE)
            .setContentIntent(contentIntent)
            .build()
        startForeground(1, notification)
    }

    override fun onDestroy() {
        quitServer()
        super.onDestroy()
    }

    private fun quitServer() {
        Instance = null
        foregroundIntent = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        this.stopSelf()
        this.stopServer()
    }

    companion object {
        @JvmStatic
        var Instance: BackgroundService? = null

        @JvmStatic
        var foregroundIntent: Intent? = null

        @JvmStatic
        fun isRunning(): Boolean {
            return foregroundIntent != null
        }
    }

    init {
        System.loadLibrary("ew")
    }
    private external fun startServer(dataPath: String, assetPath: String, masterdataPath: String, easterMode: Boolean) : String
    private external fun stopServer() : String
    external fun setEasterMode(easterMode: Boolean)
}
