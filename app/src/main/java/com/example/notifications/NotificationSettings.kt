package com.example.notifications

val REMINDER_INTERVAL_OPTIONS_HOURS = listOf(6, 12, 24)

data class NotificationSettings(
    val masterEnabled: Boolean = true,
    val inactivityStreakEnabled: Boolean = true,
    val motivationalEnabled: Boolean = true,
    val earningAlertsEnabled: Boolean = true,
    val reminderDelayHours: Int = 24
) {
    fun isInactivityStreakActive(): Boolean = masterEnabled && inactivityStreakEnabled
    fun isMotivationalActive(): Boolean = masterEnabled && motivationalEnabled
    fun isEarningAlertsActive(): Boolean = masterEnabled && earningAlertsEnabled
}
