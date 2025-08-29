package com.example.practiceapp

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.Handler
import android.os.HandlerThread
import android.widget.RemoteViews
import androidx.core.content.edit
import androidx.core.graphics.toColorInt
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit


class PracticeAppWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {

            makeUpdates(context)

            AlarmHelper.scheduleCheckins(context, true)
        }
    }

    private fun getClickIntent(context: Context, isClick: Boolean): Intent {
        val eventName = if (isClick) "com.example.MY_WIDGET_CLICK" else "com.example.MY_WIDGET_UNCLICK"
        // Create an Intent to handle the click
        return Intent(context, PracticeAppWidget::class.java).apply {
            action = eventName
        }
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
        val handlerThread = HandlerThread("BackgroundThread")
        handlerThread.start()
        val backgroundLooper = handlerThread.looper
        val handler = Handler(backgroundLooper)
        handler.removeCallbacksAndMessages(null)
        handlerThread.quitSafely()
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.example.CHECKIN_UPDATE") {
            makeUpdates(context)
            AlarmHelper.scheduleCheckins(context, false)
        } else if (intent.action == "com.example.MY_WIDGET_UNCLICK") {
            val sharedPreferences = context.getSharedPreferences("PracticeLog", MODE_PRIVATE)
            val today = ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS)
            val practicedToday =
                sharedPreferences.getString(today.toString(), "")
                    .toBoolean()
            if (practicedToday) {
                sharedPreferences.edit { remove(today.toString()) }
            }
            makeUpdates(context)
            // Handle undo button event
        } else if (intent.action == "com.example.MY_WIDGET_CLICK") {
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
            makeUpdates(context)
        }

        super.onReceive(context, intent)
    }

    private fun makeUpdates(context: Context) {
        val handlerThread = HandlerThread("BackgroundThread")
        handlerThread.quitSafely()
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
            val intent = getClickIntent(context, false)

            // Wrap the Intent in a PendingIntent
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            // Set pending intent
            views.apply {
                    setOnClickPendingIntent(R.id.appwidget_button, pendingIntent)
                }
            views.setTextViewText(R.id.appwidget_button, "Undo")
            views.setTextViewText(R.id.appwidget_text, successText)
            views.setInt(
                R.id.appwidget_layout,
                "setBackgroundColor",
                "#C1FDAA".toColorInt()
            )
        } else {
            views.setTextViewText(R.id.appwidget_text, questionText)
            val intent = getClickIntent(context, true)

            // Wrap the Intent in a PendingIntent
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            // Set pending intent
            views.apply {
                    setOnClickPendingIntent(R.id.appwidget_button, pendingIntent)
                }
            views.setTextViewText(R.id.appwidget_button, "I have")
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
                val delay : Long = if (isImmediate) 1000 else 3600000
                val pendingIntent = getCheckinPendingIntent(context)

                val handlerThread = HandlerThread("BackgroundThread")
                handlerThread.start()
                val backgroundLooper = handlerThread.looper
                val handler = Handler(backgroundLooper)
                // Schedule the PendingIntent to be triggered after a delay
                handler.postDelayed({
                    try {
                        pendingIntent.send() // Trigger the PendingIntent
                    } catch (e: PendingIntent.CanceledException) {
                        e.printStackTrace() // Handle the exception if the PendingIntent is canceled
                    }
                }, delay)
            }
        }

        private fun getActiveWidgetIds(context: Context): IntArray {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, PracticeAppWidget::class.java)

            // return ID of all active widgets within this AppWidgetProvider
            return appWidgetManager.getAppWidgetIds(componentName)
        }
        private fun getCheckinPendingIntent(context: Context): PendingIntent {
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