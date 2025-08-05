package com.example.practiceapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.widget.RemoteViews
import androidx.core.graphics.toColorInt
import com.example.practiceapp.AlarmHelper.Companion.alarmManager
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import androidx.core.content.edit

class PracticeAppWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {

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

            // Instruct the widget manager to update the widget
            appWidgetManager.updateAppWidget(appWidgetId, views)

            makeUpdates(context)

            AlarmHelper.scheduleCheckins(context, true)
        }
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
        context.alarmManager.cancel(AlarmHelper.getCheckinPendingIntent(context))
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.example.CHECKIN_UPDATE") {
            makeUpdates(context)
            AlarmHelper.scheduleCheckins(context, false)
        } else if (intent.action == "com.example.MY_WIDGET_CLICK") {
            // Handle the click event
            val successText = "You practiced today!"
            val views = RemoteViews(context.packageName, R.layout.practice_app_widget)
            views.setTextViewText(R.id.appwidget_text, successText)
            views.setInt(R.id.appwidget_layout, "setBackgroundColor", "#C1FDAA".toColorInt())

            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, PracticeAppWidget::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

            appWidgetManager.updateAppWidget(appWidgetIds, views)

            val sharedPreferences = context.getSharedPreferences("PracticeLog", MODE_PRIVATE)
            val today = ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS)
            val practicedToday =
                sharedPreferences.getString(today.toString(), "")
                    .toBoolean()
            if (!practicedToday) {
                sharedPreferences.edit {
                    putString(today.toString(), "true")
                }
            }
        }

        super.onReceive(context, intent)
    }

    private fun makeUpdates(context: Context) {
        val questionText = "Have you practiced today?"
        val angryText = "HAVE YOU PRACTICED TODAY?"
        val successText = "You practiced today!"
        // check in throughout the day
        val sharedPreferences = context.getSharedPreferences("PracticeLog", MODE_PRIVATE)
        val now = ZonedDateTime.now()
        val practicedToday =
            sharedPreferences.getString(now.truncatedTo(ChronoUnit.DAYS).toString(), "")
                .toBoolean()
        val views = RemoteViews(context.packageName, R.layout.practice_app_widget)
        if (practicedToday) {
            views.setTextViewText(R.id.appwidget_text, successText)
            views.setInt(
                R.id.appwidget_layout,
                "setBackgroundColor",
                "#C1FDAA".toColorInt()
            )
        } else {
            views.setTextViewText(R.id.appwidget_text, questionText)
            // gradually darker red throughout the day if user hasn't practiced
            if (now.hour < 8) {
                views.setInt(
                    R.id.appwidget_layout,
                    "setBackgroundColor",
                    "#FFC6C6".toColorInt()
                )
            } else if (now.hour < 12) {
                views.setInt(
                    R.id.appwidget_layout,
                    "setBackgroundColor",
                    "#FFA0A0".toColorInt()
                )
            } else if (now.hour < 16) {
                views.setInt(
                    R.id.appwidget_layout,
                    "setBackgroundColor",
                    "#FF7F7F".toColorInt()
                )
            } else {
                views.setTextViewText(R.id.appwidget_text, angryText)
                views.setInt(
                    R.id.appwidget_layout,
                    "setBackgroundColor",
                    "#FF4848".toColorInt()
                )
            }
        }
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, PracticeAppWidget::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
        appWidgetManager.updateAppWidget(appWidgetIds, views)
    }

}

class AlarmHelper {
    companion object {
        fun scheduleCheckins(context: Context, isImmediate: Boolean) {
            val activeWidgetIds = getActiveWidgetIds(context)

            if (activeWidgetIds.isNotEmpty()) {
                // midnight tomorrow
                val nextUpdate: ZonedDateTime
                if (isImmediate) {
                    nextUpdate = ZonedDateTime.now().plusMinutes(1)
                } else {
                    nextUpdate = ZonedDateTime.now().plusHours(1)
                }
                val pendingIntent = getCheckinPendingIntent(context)

                context.alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    nextUpdate.toInstant()
                        .toEpochMilli(), // alarm time in millis since 1970-01-01 UTC
                    pendingIntent
                )
            }
        }

        fun getActiveWidgetIds(context: Context): IntArray {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, PracticeAppWidget::class.java)

            // return ID of all active widgets within this AppWidgetProvider
            return appWidgetManager.getAppWidgetIds(componentName)
        }
        fun getCheckinPendingIntent(context: Context): PendingIntent {
            val widgetClass = PracticeAppWidget::class.java
            val widgetIds = getActiveWidgetIds(context)
            val updateIntent = Intent(context, widgetClass)
                .setAction("com.example.CHECKIN_UPDATE")
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)
            val requestCode = widgetClass.name.hashCode()
            val flags = PendingIntent.FLAG_CANCEL_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE

            return PendingIntent.getBroadcast(context, requestCode, updateIntent, flags)
        }

        val Context.alarmManager: AlarmManager
            get() = getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

}

// this should get checkins moving again after user reboot (untested)
class BootBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(pContext: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            AlarmHelper.scheduleCheckins(pContext, true)
        }
    }
}