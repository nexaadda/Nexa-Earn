package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.NotificationPreferences
import com.example.model.todayDateString
import java.util.concurrent.TimeUnit

class NotificationAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val prefs = NotificationPreferences(context)
        val userId = intent.getStringExtra(NotificationScheduler.EXTRA_USER_ID)
            ?.takeIf { it.isNotBlank() }
            ?: prefs.getActiveUserId()
            ?: return

        val settings = prefs.readSettings(userId)
        val snapshot = prefs.readUserSnapshot(userId)
        val nowMillis = System.currentTimeMillis()
        val today = todayDateString()

        when (action) {
            NotificationScheduler.ACTION_STREAK_INACTIVITY -> {
                if (settings.isInactivityStreakActive()) {
                    val thresholdMs = TimeUnit.HOURS.toMillis(settings.reminderDelayHours.toLong())
                    val shouldNotify = NexaEarnNotificationHelper.shouldSendInactivityOrStreakReminder(
                        lastAppOpenMillis = snapshot.lastAppOpenMillis,
                        lastStreakClaimDate = snapshot.lastStreakClaimDate,
                        todayDate = today,
                        nowMillis = nowMillis,
                        inactivityThresholdMillis = thresholdMs
                    )
                    if (shouldNotify) {
                        NexaEarnNotificationHelper.showInactivityOrStreakReminder(
                            context = context,
                            displayName = snapshot.displayName,
                            streakClaimedToday = snapshot.lastStreakClaimDate == today,
                            currentStreak = snapshot.currentStreak
                        )
                    }
                    NotificationScheduler.rescheduleAfterTrigger(
                        context = context,
                        action = action,
                        userId = userId,
                        preferences = prefs,
                        nowMillis = nowMillis
                    )
                }
            }
            NotificationScheduler.ACTION_MOTIVATIONAL -> {
                if (settings.isMotivationalActive()) {
                    val index = prefs.advanceMotivationIndex(userId)
                    NexaEarnNotificationHelper.showMotivationalNotification(
                        context = context,
                        motivationIndex = index
                    )
                    NotificationScheduler.rescheduleAfterTrigger(
                        context = context,
                        action = action,
                        userId = userId,
                        preferences = prefs,
                        nowMillis = nowMillis
                    )
                }
            }
        }
    }
}
