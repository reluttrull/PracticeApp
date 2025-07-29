package com.example.practiceapp

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.Button
import android.widget.RemoteViews
import android.widget.Toast
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt

/**
 * Implementation of App Widget functionality.
 */
class PracticeAppWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            val widgetText = "Have you practiced today?"
            context.getString(R.string.appwidget_text)

            // Create an Intent to handle the click
            //val intent = Intent(context, AppWidgetProvider::class.java).apply {
            val intent = Intent(context, PracticeAppWidget::class.java).apply {
                action = "com.example.MY_WIDGET_CLICK"
            }

            // Wrap the Intent in a PendingIntent
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            // Construct the RemoteViews object
            val views = RemoteViews(context.packageName, R.layout.practice_app_widget)
                .apply {
                    setOnClickPendingIntent(R.id.appwidget_button, pendingIntent)
                }
            views.setTextViewText(R.id.appwidget_text, widgetText)
            views.setInt(R.id.appwidget_layout, "setBackgroundColor", "#FFC6C6".toColorInt());

            // Instruct the widget manager to update the widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    override fun onReceive(context: Context, intent: Intent) {

        if (intent.action == "com.example.MY_WIDGET_CLICK") {
            // Handle the click event
            val views = RemoteViews(context.packageName, R.layout.practice_app_widget)
            views.setTextViewText(R.id.appwidget_text, "You practiced!")
            views.setInt(R.id.appwidget_layout, "setBackgroundColor", "#C1FDAA".toColorInt());

            Toast.makeText(context, "You practiced today!", Toast.LENGTH_SHORT).show()

            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, PracticeAppWidget::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

            appWidgetManager.updateAppWidget(appWidgetIds, views)
        }

        super.onReceive(context, intent)
    }
}