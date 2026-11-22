package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.notifications.NotificationSettings
import com.example.notifications.REMINDER_INTERVAL_OPTIONS_HOURS
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserNotificationSnapshot(
    val userId: String,
    val displayName: String,
    val lastStreakClaimDate: String?,
    val currentStreak: Long,
    val lastAppOpenMillis: Long,
    val motivationIndex: Int
)

class NotificationPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun readSettings(userId: String): NotificationSettings {
        if (userId.isBlank()) return NotificationSettings()
        val interval = prefs.getInt(key(userId, KEY_REMINDER_HOURS), 24)
            .let { if (it in REMINDER_INTERVAL_OPTIONS_HOURS) it else 24 }
        return NotificationSettings(
            masterEnabled = prefs.getBoolean(key(userId, KEY_MASTER_ENABLED), true),
            inactivityStreakEnabled = prefs.getBoolean(key(userId, KEY_INACTIVITY_STREAK_ENABLED), true),
            motivationalEnabled = prefs.getBoolean(key(userId, KEY_MOTIVATIONAL_ENABLED), true),
            earningAlertsEnabled = prefs.getBoolean(key(userId, KEY_EARNING_ALERTS_ENABLED), true),
            reminderDelayHours = interval
        )
    }

    fun observeSettings(userId: String): StateFlowHolder {
        return StateFlowHolder(userId)
    }

    inner class StateFlowHolder(private val userId: String) {
        private val _flow = MutableStateFlow(readSettings(userId))
        val flow: Flow<NotificationSettings> = _flow.asStateFlow()

        fun refresh() {
            _flow.value = readSettings(userId)
        }
    }

    fun updateSettings(userId: String, transform: (NotificationSettings) -> NotificationSettings): NotificationSettings {
        val current = readSettings(userId)
        val updated = transform(current).copy(
            reminderDelayHours = transform(current).reminderDelayHours.let {
                if (it in REMINDER_INTERVAL_OPTIONS_HOURS) it else 24
            }
        )
        prefs.edit()
            .putString(KEY_ACTIVE_USER_ID, userId)
            .putBoolean(key(userId, KEY_MASTER_ENABLED), updated.masterEnabled)
            .putBoolean(key(userId, KEY_INACTIVITY_STREAK_ENABLED), updated.inactivityStreakEnabled)
            .putBoolean(key(userId, KEY_MOTIVATIONAL_ENABLED), updated.motivationalEnabled)
            .putBoolean(key(userId, KEY_EARNING_ALERTS_ENABLED), updated.earningAlertsEnabled)
            .putInt(key(userId, KEY_REMINDER_HOURS), updated.reminderDelayHours)
            .apply()
        return updated
    }

    fun setActiveUser(userId: String) {
        if (userId.isBlank()) return
        prefs.edit().putString(KEY_ACTIVE_USER_ID, userId).apply()
    }

    fun getActiveUserId(): String? = prefs.getString(KEY_ACTIVE_USER_ID, null)?.takeIf { it.isNotBlank() }

    fun recordAppOpen(userId: String, nowMillis: Long = System.currentTimeMillis()) {
        if (userId.isBlank()) return
        prefs.edit()
            .putString(KEY_ACTIVE_USER_ID, userId)
            .putLong(key(userId, KEY_LAST_APP_OPEN_MILLIS), nowMillis)
            .apply()
    }

    fun updateUserActivitySnapshot(
        userId: String,
        displayName: String,
        lastStreakClaimDate: String?,
        currentStreak: Long
    ) {
        if (userId.isBlank()) return
        val editor = prefs.edit()
            .putString(KEY_ACTIVE_USER_ID, userId)
            .putString(key(userId, KEY_DISPLAY_NAME), displayName)
            .putLong(key(userId, KEY_CURRENT_STREAK), currentStreak)
        if (lastStreakClaimDate != null) {
            editor.putString(key(userId, KEY_LAST_STREAK_CLAIM_DATE), lastStreakClaimDate)
        } else {
            editor.remove(key(userId, KEY_LAST_STREAK_CLAIM_DATE))
        }
        editor.apply()
    }

    fun readUserSnapshot(userId: String): UserNotificationSnapshot {
        val lastOpen = prefs.getLong(key(userId, KEY_LAST_APP_OPEN_MILLIS), System.currentTimeMillis())
        return UserNotificationSnapshot(
            userId = userId,
            displayName = prefs.getString(key(userId, KEY_DISPLAY_NAME), "Player") ?: "Player",
            lastStreakClaimDate = prefs.getString(key(userId, KEY_LAST_STREAK_CLAIM_DATE), null),
            currentStreak = prefs.getLong(key(userId, KEY_CURRENT_STREAK), 0L),
            lastAppOpenMillis = lastOpen,
            motivationIndex = prefs.getInt(key(userId, KEY_MOTIVATION_INDEX), 0)
        )
    }

    fun advanceMotivationIndex(userId: String): Int {
        val current = prefs.getInt(key(userId, KEY_MOTIVATION_INDEX), 0)
        val next = (current + 1) % 1000
        prefs.edit().putInt(key(userId, KEY_MOTIVATION_INDEX), next).apply()
        return current
    }

    fun getDeliveredAdminNotificationIds(userId: String): Set<String> {
        if (userId.isBlank()) return emptySet()
        return prefs.getStringSet(key(userId, KEY_DELIVERED_ADMIN_NOTIF_IDS), emptySet())
            ?.toSet()
            ?: emptySet()
    }

    fun markAdminNotificationDelivered(userId: String, notificationId: String) {
        if (userId.isBlank() || notificationId.isBlank()) return
        val updated = getDeliveredAdminNotificationIds(userId).toMutableSet()
        updated.add(notificationId)
        val trimmed = if (updated.size > 150) {
            updated.toList().takeLast(150).toSet()
        } else {
            updated
        }
        prefs.edit()
            .putStringSet(key(userId, KEY_DELIVERED_ADMIN_NOTIF_IDS), trimmed)
            .apply()
    }

    private fun key(userId: String, suffix: String) = "${userId}_$suffix"

    private companion object {
        const val PREFS_NAME = "nexaearn_notification_prefs"
        const val KEY_ACTIVE_USER_ID = "active_user_id"
        const val KEY_MASTER_ENABLED = "master_enabled"
        const val KEY_INACTIVITY_STREAK_ENABLED = "inactivity_streak_enabled"
        const val KEY_MOTIVATIONAL_ENABLED = "motivational_enabled"
        const val KEY_EARNING_ALERTS_ENABLED = "earning_alerts_enabled"
        const val KEY_REMINDER_HOURS = "reminder_hours"
        const val KEY_LAST_APP_OPEN_MILLIS = "last_app_open_millis"
        const val KEY_LAST_STREAK_CLAIM_DATE = "last_streak_claim_date"
        const val KEY_CURRENT_STREAK = "current_streak"
        const val KEY_DISPLAY_NAME = "display_name"
        const val KEY_MOTIVATION_INDEX = "motivation_index"
        const val KEY_DELIVERED_ADMIN_NOTIF_IDS = "delivered_admin_notif_ids"
    }
}
