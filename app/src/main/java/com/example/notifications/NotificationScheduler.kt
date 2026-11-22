package com.example.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.data.NotificationPreferences
import java.util.concurrent.TimeUnit
import kotlin.math.max

object NotificationScheduler {
    const val ACTION_STREAK_INACTIVITY = "com.example.notifications.ACTION_STREAK_INACTIVITY"
    const val ACTION_MOTIVATIONAL = "com.example.notifications.ACTION_MOTIVATIONAL"
    const val EXTRA_USER_ID = "extra_user_id"

    private const val REQ_STREAK_INACTIVITY = 4001
    private const val REQ_MOTIVATIONAL = 4002
    private const val MIN_RETRIGGER_MS = 60L * 60L * 1000L

    fun syncSchedules(
        context: Context,
        preferences: NotificationPreferences = NotificationPreferences(context),
        userIdOverride: String? = null,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val userId = userIdOverride?.takeIf { it.isNotBlank() } ?: preferences.getActiveUserId()

        if (userId.isNullOrBlank()) {
            cancelAll(context, alarmManager)
            return
        }

        val settings = preferences.readSettings(userId)
        val snapshot = preferences.readUserSnapshot(userId)

        if (settings.isInactivityStreakActive()) {
            val intervalMs = TimeUnit.HOURS.toMillis(settings.reminderDelayHours.toLong())
            val candidateTrigger = snapshot.lastAppOpenMillis + intervalMs
            val triggerAt = max(candidateTrigger, nowMillis + MIN_RETRIGGER_MS)
            scheduleInexactWakeup(
                context = context,
                alarmManager = alarmManager,
                requestCode = REQ_STREAK_INACTIVITY,
                action = ACTION_STREAK_INACTIVITY,
                userId = userId,
                triggerAtMillis = triggerAt
            )
        } else {
            cancelAction(context, alarmManager, REQ_STREAK_INACTIVITY, ACTION_STREAK_INACTIVITY)
        }

        if (settings.isMotivationalActive()) {
            val intervalMs = TimeUnit.HOURS.toMillis(settings.reminderDelayHours.toLong())
            val triggerAt = nowMillis + max(intervalMs, MIN_RETRIGGER_MS)
            scheduleInexactWakeup(
                context = context,
                alarmManager = alarmManager,
                requestCode = REQ_MOTIVATIONAL,
                action = ACTION_MOTIVATIONAL,
                userId = userId,
                triggerAtMillis = triggerAt
            )
        } else {
            cancelAction(context, alarmManager, REQ_MOTIVATIONAL, ACTION_MOTIVATIONAL)
        }
    }

    fun rescheduleAfterTrigger(
        context: Context,
        action: String,
        userId: String,
        preferences: NotificationPreferences = NotificationPreferences(context),
        nowMillis: Long = System.currentTimeMillis()
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val settings = preferences.readSettings(userId)
        val intervalMs = max(
            TimeUnit.HOURS.toMillis(settings.reminderDelayHours.toLong()),
            MIN_RETRIGGER_MS
        )

        when (action) {
            ACTION_STREAK_INACTIVITY -> {
                if (settings.isInactivityStreakActive()) {
                    scheduleInexactWakeup(
                        context = context,
                        alarmManager = alarmManager,
                        requestCode = REQ_STREAK_INACTIVITY,
                        action = ACTION_STREAK_INACTIVITY,
                        userId = userId,
                        triggerAtMillis = nowMillis + intervalMs
                    )
                }
            }
            ACTION_MOTIVATIONAL -> {
                if (settings.isMotivationalActive()) {
                    scheduleInexactWakeup(
                        context = context,
                        alarmManager = alarmManager,
                        requestCode = REQ_MOTIVATIONAL,
                        action = ACTION_MOTIVATIONAL,
                        userId = userId,
                        triggerAtMillis = nowMillis + intervalMs
                    )
                }
            }
        }
    }

    private fun scheduleInexactWakeup(
        context: Context,
        alarmManager: AlarmManager,
        requestCode: Int,
        action: String,
        userId: String,
        triggerAtMillis: Long
    ) {
        val pendingIntent = buildPendingIntent(
            context = context,
            requestCode = requestCode,
            action = action,
            userId = userId
        )
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
        )
    }

    private fun cancelAll(context: Context, alarmManager: AlarmManager) {
        cancelAction(context, alarmManager, REQ_STREAK_INACTIVITY, ACTION_STREAK_INACTIVITY)
        cancelAction(context, alarmManager, REQ_MOTIVATIONAL, ACTION_MOTIVATIONAL)
    }

    private fun cancelAction(
        context: Context,
        alarmManager: AlarmManager,
        requestCode: Int,
        action: String
    ) {
        val intent = Intent(context, NotificationAlarmReceiver::class.java).apply {
            this.action = action
        }
        val existing = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (existing != null) {
            alarmManager.cancel(existing)
            existing.cancel()
        }
    }

    private fun buildPendingIntent(
        context: Context,
        requestCode: Int,
        action: String,
        userId: String
    ): PendingIntent {
        val intent = Intent(context, NotificationAlarmReceiver::class.java).apply {
            this.action = action
            putExtra(EXTRA_USER_ID, userId)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
