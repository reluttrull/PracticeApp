package com.example.practiceapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.SharedPreferences
import android.view.View
import android.widget.Button
import android.widget.RemoteViews
import android.widget.TextView
import android.widget.Toast
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt
import java.time.Duration
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

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
            views.setInt(R.id.appwidget_layout, "setBackgroundColor", "#FFD8D8".toColorInt());

            // Instruct the widget manager to update the widget
            appWidgetManager.updateAppWidget(appWidgetId, views)

            scheduleUpdates(context)
            scheduleCheckins(context)
        }
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
        context.alarmManager.cancel(getUpdatePendingIntent(context))
    }

    override fun onReceive(context: Context, intent: Intent) {
        val angryText = "HAVE YOU PRACTICED TODAY?"
        val successText = "You practiced today!"
        if (intent.action == "com.example.CHECKIN_UPDATE") {
            // check in throughout the day
            val sharedPreferences = context.getSharedPreferences("PracticeLog", MODE_PRIVATE)
            val now = ZonedDateTime.now()
            val practicedToday =
                sharedPreferences.getString(now.truncatedTo(ChronoUnit.DAYS).toString(), "")
                    .toBoolean()
            if (!practicedToday) {
                val views = RemoteViews(context.packageName, R.layout.practice_app_widget)
                // gradually darker red throughout the day if user hasn't practiced
                if (now.hour < 8) {
                    views.setInt(
                        R.id.appwidget_layout,
                        "setBackgroundColor",
                        "#FFC6C6".toColorInt()
                    );
                } else if (now.hour < 12) {
                    views.setInt(
                        R.id.appwidget_layout,
                        "setBackgroundColor",
                        "#FFA0A0".toColorInt()
                    );
                } else if (now.hour < 16) {
                    views.setInt(
                        R.id.appwidget_layout,
                        "setBackgroundColor",
                        "#FF7F7F".toColorInt()
                    );
                } else {
                    views.setTextViewText(R.id.appwidget_text, angryText)
                    views.setInt(
                        R.id.appwidget_layout,
                        "setBackgroundColor",
                        "#FF4848".toColorInt()
                    );
                }
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, PracticeAppWidget::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

                appWidgetManager.updateAppWidget(appWidgetIds, views)
                // schedule next checkin if hasn't practiced, otherwise update will reset
                scheduleCheckins(context)
            }
        } else if (intent.action == "com.example.MY_WIDGET_CLICK") {
            // Handle the click event
            val views = RemoteViews(context.packageName, R.layout.practice_app_widget)
            views.setTextViewText(R.id.appwidget_text, successText)
            views.setInt(R.id.appwidget_layout, "setBackgroundColor", "#C1FDAA".toColorInt());

            val sharedPreferences = context.getSharedPreferences("PracticeLog", MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putString(ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS).toString(), "true")
            editor.apply()

            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, PracticeAppWidget::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

            appWidgetManager.updateAppWidget(appWidgetIds, views)
        }

        super.onReceive(context, intent)
    }

    private fun getActiveWidgetIds(context: Context): IntArray {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, this::class.java)

        // return ID of all active widgets within this AppWidgetProvider
        return appWidgetManager.getAppWidgetIds(componentName)
    }

    private fun scheduleCheckins(context: Context) {
        val activeWidgetIds = getActiveWidgetIds(context)

        if (activeWidgetIds.isNotEmpty()) {
            // midnight tomorrow
            val nextUpdate = ZonedDateTime.now().plusHours(1)
            val pendingIntent = getCheckinPendingIntent(context)

            context.alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                nextUpdate.toInstant().toEpochMilli(), // alarm time in millis since 1970-01-01 UTC
                pendingIntent
            )
        }
    }

    private fun scheduleUpdates(context: Context) {
        val activeWidgetIds = getActiveWidgetIds(context)

        if (activeWidgetIds.isNotEmpty()) {
            // midnight tomorrow
            val nextUpdate = ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS).plusDays(1);
            val pendingIntent = getUpdatePendingIntent(context)

            context.alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                nextUpdate.toInstant().toEpochMilli(), // alarm time in millis since 1970-01-01 UTC
                pendingIntent
            )
        }
    }

    private fun getCheckinPendingIntent(context: Context): PendingIntent {
        val widgetClass = this::class.java
        val widgetIds = getActiveWidgetIds(context)
        val updateIntent = Intent(context, widgetClass)
            .setAction("com.example.CHECKIN_UPDATE")
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)
        val requestCode = widgetClass.name.hashCode()
        val flags = PendingIntent.FLAG_CANCEL_CURRENT or
                PendingIntent.FLAG_IMMUTABLE

        return PendingIntent.getBroadcast(context, requestCode, updateIntent, flags)
    }

    private fun getUpdatePendingIntent(context: Context): PendingIntent {
        val widgetClass = this::class.java
        val widgetIds = getActiveWidgetIds(context)
        val updateIntent = Intent(context, widgetClass)
            .setAction("android.appwidget.action.APPWIDGET_UPDATE") // update whole app fresh
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)
        val requestCode = widgetClass.name.hashCode()
        val flags = PendingIntent.FLAG_CANCEL_CURRENT or
                PendingIntent.FLAG_IMMUTABLE

        return PendingIntent.getBroadcast(context, requestCode, updateIntent, flags)
    }

    private val Context.alarmManager: AlarmManager
        get() = getSystemService(Context.ALARM_SERVICE) as AlarmManager
//
//    companion object {
//        private val WIDGET_UPDATE_INTERVAL = Duration.ofMinutes(30)
//    }
}