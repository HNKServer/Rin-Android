package one.ethanthesleepy.androidew

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class WidgetService : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }
}

internal fun updateAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {
    val views = RemoteViews(context.packageName, R.layout.widget_service)
    if (BackgroundService.isRunning()) {
        views.setTextViewText(R.id.start_server, context.getString(R.string.stop_server))
    } else {
        views.setTextViewText(R.id.start_server, context.getString(R.string.start_server))
    }
    val intent = Intent(context, AppBroadcastReceiver::class.java)
    intent.action = "TOGGLE_SERVICE"
    val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    views.setOnClickPendingIntent(R.id.start_server, pendingIntent)

    appWidgetManager.updateAppWidget(appWidgetId, views)
}