package one.ethanthesleepy.androidew

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import androidx.core.content.ContextCompat

class AppBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("intent", intent.action.toString())
        if ("START_SERVICE" == intent.action) {
            startService(context)
        } else if ("STOP_SERVICE" == intent.action) {
            stopService(context)
        } else if ("TOGGLE_SERVICE" == intent.action) {
            if (BackgroundService.foregroundIntent == null) {
                startService(context)
            } else {
                stopService(context)
            }
        }
        updateWidgets(context)
    }
    private fun startService(context: Context) {
        if (BackgroundService.foregroundIntent != null) {
            return
        }
        val serviceIntent = Intent(
            context,
            BackgroundService::class.java
        )
        BackgroundService.foregroundIntent = serviceIntent
        ContextCompat.startForegroundService(context, serviceIntent)
    }
    private fun stopService(context: Context) {
        if (BackgroundService.foregroundIntent == null) {
            return
        }
        val serviceIntent = Intent(
            context,
            BackgroundService::class.java
        )
        BackgroundService.foregroundIntent = null
        context.stopService(serviceIntent)
    }
    private fun updateWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, WidgetService::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

        for (appWidgetId in appWidgetIds) {
            // Create new RemoteViews for the widget
            val views = RemoteViews(context.packageName, R.layout.widget_service)

            // Update the widget's UI, for example, change the text
            if (BackgroundService.isRunning()) {
                views.setTextViewText(R.id.start_server, context.getString(R.string.stop_server))
            } else {
                views.setTextViewText(R.id.start_server, context.getString(R.string.start_server))
            }

            // Update the widget with the new RemoteViews
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
