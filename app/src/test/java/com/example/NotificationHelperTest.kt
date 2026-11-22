package com.example

import com.example.notifications.NexaEarnNotificationHelper
import com.example.notifications.NotificationSettings
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationHelperTest {

    @Test
    fun masterSwitchDisablesAllNotificationCategories() {
        val disabled = NotificationSettings(
            masterEnabled = false,
            inactivityStreakEnabled = true,
            motivationalEnabled = true,
            earningAlertsEnabled = true
        )
        assertFalse(disabled.isInactivityStreakActive())
        assertFalse(disabled.isMotivationalActive())
        assertFalse(disabled.isEarningAlertsActive())

        val enabled = disabled.copy(masterEnabled = true)
        assertTrue(enabled.isInactivityStreakActive())
        assertTrue(enabled.isMotivationalActive())
        assertTrue(enabled.isEarningAlertsActive())
    }

    @Test
    fun inactivityOrStreakReminderTriggersWhenStreakUnclaimedEvenIfRecentlyOpened() {
        val now = 1_700_000_000_000L
        val threshold = TimeUnit.HOURS.toMillis(24)

        val shouldNotify = NexaEarnNotificationHelper.shouldSendInactivityOrStreakReminder(
            lastAppOpenMillis = now - TimeUnit.HOURS.toMillis(2),
            lastStreakClaimDate = "2026-11-20",
            todayDate = "2026-11-21",
            nowMillis = now,
            inactivityThresholdMillis = threshold
        )
        assertTrue(shouldNotify)
    }

    @Test
    fun inactivityOrStreakReminderTriggersWhenAppUnopenedPastThreshold() {
        val now = 1_700_000_000_000L
        val threshold = TimeUnit.HOURS.toMillis(12)

        val shouldNotify = NexaEarnNotificationHelper.shouldSendInactivityOrStreakReminder(
            lastAppOpenMillis = now - TimeUnit.HOURS.toMillis(13),
            lastStreakClaimDate = "2026-11-21",
            todayDate = "2026-11-21",
            nowMillis = now,
            inactivityThresholdMillis = threshold
        )
        assertTrue(shouldNotify)
    }

    @Test
    fun inactivityOrStreakReminderSkipsWhenOpenedRecentlyAndStreakClaimed() {
        val now = 1_700_000_000_000L
        val threshold = TimeUnit.HOURS.toMillis(24)

        val shouldNotify = NexaEarnNotificationHelper.shouldSendInactivityOrStreakReminder(
            lastAppOpenMillis = now - TimeUnit.HOURS.toMillis(1),
            lastStreakClaimDate = "2026-11-21",
            todayDate = "2026-11-21",
            nowMillis = now,
            inactivityThresholdMillis = threshold
        )
        assertFalse(shouldNotify)
    }

    @Test
    fun streakAndMotivationalCopyIncludesExpectedMessagingAndRotates() {
        val streakUnclaimedCopy = NexaEarnNotificationHelper.buildInactivityStreakCopy(
            displayName = "Ali Raza",
            streakClaimedToday = false,
            currentStreak = 2L
        )
        assertTrue(streakUnclaimedCopy.title.contains("Daily Streak"))
        assertTrue(streakUnclaimedCopy.title.contains("Ali"))
        assertTrue(streakUnclaimedCopy.body.contains("Day 3"))

        val claimedCopy = NexaEarnNotificationHelper.buildInactivityStreakCopy(
            displayName = "Ali Raza",
            streakClaimedToday = true,
            currentStreak = 3L
        )
        assertTrue(claimedCopy.body.contains("Math Quiz"))

        val firstMotivation = NexaEarnNotificationHelper.buildMotivationalCopy(0)
        val secondMotivation = NexaEarnNotificationHelper.buildMotivationalCopy(1)
        val wrappedMotivation = NexaEarnNotificationHelper.buildMotivationalCopy(5)
        assertNotEquals(firstMotivation.title, secondMotivation.title)
        assertEquals(firstMotivation.title, wrappedMotivation.title)
    }
}
