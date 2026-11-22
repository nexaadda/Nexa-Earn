package com.example.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.model.streakRewardForDay

data class NotificationCopy(
    val title: String,
    val body: String
)

object NexaEarnNotificationHelper {
    const val CHANNEL_EARNINGS = "nexaearn_earnings"
    const val CHANNEL_STREAK_REMINDERS = "nexaearn_streak_reminders"
    const val CHANNEL_MOTIVATION = "nexaearn_motivation"
    const val CHANNEL_ADMIN_BROADCAST = "nexaearn_announcements"

    private const val NOTIFICATION_ID_STREAK_REMINDER = 2001
    private const val NOTIFICATION_ID_MOTIVATION = 2002
    private const val NOTIFICATION_ID_TEST = 2003
    private const val NOTIFICATION_ID_EARNING_BASE = 3000
    private const val NOTIFICATION_ID_ADMIN_BASE = 4000

    private var earningSequence = 0

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        val earningsChannel = NotificationChannel(
            CHANNEL_EARNINGS,
            "Coin Earnings Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Instant push alerts whenever you earn coins from Google AdMob ads, Daily Streak, or Mini Games."
        }

        val streakChannel = NotificationChannel(
            CHANNEL_STREAK_REMINDERS,
            "Daily Streak & Inactivity Reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Reminds you when you haven't opened NexaEarn or haven't claimed today's Daily Streak."
        }

        val motivationChannel = NotificationChannel(
            CHANNEL_MOTIVATION,
            "Earning Motivation",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Motivating alerts to reach the 100-coin 24h Goal, unlock 5% Extra Earn, and boost your wallet."
        }

        val adminBroadcastChannel = NotificationChannel(
            CHANNEL_ADMIN_BROADCAST,
            "Official Announcements & Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Important live updates, payout status alerts, and official messages from NexaEarn."
        }

        manager.createNotificationChannels(
            listOf(earningsChannel, streakChannel, motivationChannel, adminBroadcastChannel)
        )
    }

    fun hasNotificationPermission(context: Context): Boolean {
        val runtimeGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        return runtimeGranted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun shouldSendInactivityOrStreakReminder(
        lastAppOpenMillis: Long,
        lastStreakClaimDate: String?,
        todayDate: String,
        nowMillis: Long,
        inactivityThresholdMillis: Long
    ): Boolean {
        val streakUnclaimedToday = lastStreakClaimDate != todayDate
        val appUnopenedLongEnough = (nowMillis - lastAppOpenMillis) >= inactivityThresholdMillis
        return streakUnclaimedToday || appUnopenedLongEnough
    }

    fun buildInactivityStreakCopy(
        displayName: String,
        streakClaimedToday: Boolean,
        currentStreak: Long
    ): NotificationCopy {
        val firstName = displayName.trim().substringBefore(" ").ifBlank { "Player" }
        return if (!streakClaimedToday) {
            val nextDay = (currentStreak + 1L).coerceAtLeast(1L)
            val nextBonus = streakRewardForDay(nextDay.toInt())
            NotificationCopy(
                title = "Don't lose your Daily Streak, $firstName!",
                body = "You haven't claimed today's Day $nextDay streak yet. Open NexaEarn and watch an AdMob ad to grab +$nextBonus coins!"
            )
        } else {
            NotificationCopy(
                title = "We miss you in NexaEarn, $firstName!",
                body = "Your mini games are ready: earn +10 coins in Math Quiz, +5 in Tap Tap 30s, or +5 in Tic Tac Toe toward your 100-coin goal."
            )
        }
    }

    fun buildMotivationalCopy(index: Int): NotificationCopy {
        val prompts = listOf(
            NotificationCopy(
                title = "Boost your Extra Earn pot today!",
                body = "Earn 100 coins before the 1:00 AM reset from Mini Games, Daily Streak & Watch Ads to unlock +5% Extra Earn!"
            ),
            NotificationCopy(
                title = "Quick +10 Coins in Math Quiz!",
                body = "Solve 5 fast questions per level (+, -, ×, ÷) and watch a Google AdMob ad to pocket +10 coins right away."
            ),
            NotificationCopy(
                title = "Tap Tap 30s Challenge is waiting!",
                body = "Can you hit 100 taps in 30 seconds? Beat the clock and unlock +5 instant coins!"
            ),
            NotificationCopy(
                title = "Closer to your 5,000-coin withdrawal!",
                body = "Every Tic Tac Toe level, Watch Ad, and Daily Streak brings you closer to Easypaisa or JazzCash payout."
            ),
            NotificationCopy(
                title = "Keep the momentum going!",
                body = "Play unlimited levels of Tic Tac Toe or watch bonus Google AdMob ads anytime to grow your NexaEarn wallet."
            )
        )
        val safeIdx = ((index % prompts.size) + prompts.size) % prompts.size
        return prompts[safeIdx]
    }

    fun showEarningNotification(
        context: Context,
        title: String,
        body: String
    ): Boolean {
        ensureChannels(context)
        if (!hasNotificationPermission(context)) return false
        val id = NOTIFICATION_ID_EARNING_BASE + ((earningSequence++) % 50)
        return postNotification(
            context = context,
            channelId = CHANNEL_EARNINGS,
            notificationId = id,
            title = title,
            body = body,
            priority = NotificationCompat.PRIORITY_HIGH
        )
    }

    fun showInactivityOrStreakReminder(
        context: Context,
        displayName: String,
        streakClaimedToday: Boolean,
        currentStreak: Long
    ): Boolean {
        ensureChannels(context)
        if (!hasNotificationPermission(context)) return false
        val copy = buildInactivityStreakCopy(
            displayName = displayName,
            streakClaimedToday = streakClaimedToday,
            currentStreak = currentStreak
        )
        return postNotification(
            context = context,
            channelId = CHANNEL_STREAK_REMINDERS,
            notificationId = NOTIFICATION_ID_STREAK_REMINDER,
            title = copy.title,
            body = copy.body,
            priority = NotificationCompat.PRIORITY_DEFAULT
        )
    }

    fun showMotivationalNotification(
        context: Context,
        motivationIndex: Int
    ): Boolean {
        ensureChannels(context)
        if (!hasNotificationPermission(context)) return false
        val copy = buildMotivationalCopy(motivationIndex)
        return postNotification(
            context = context,
            channelId = CHANNEL_MOTIVATION,
            notificationId = NOTIFICATION_ID_MOTIVATION,
            title = copy.title,
            body = copy.body,
            priority = NotificationCompat.PRIORITY_DEFAULT
        )
    }

    fun showTestNotification(context: Context): Boolean {
        ensureChannels(context)
        if (!hasNotificationPermission(context)) return false
        return postNotification(
            context = context,
            channelId = CHANNEL_EARNINGS,
            notificationId = NOTIFICATION_ID_TEST,
            title = "NexaEarn Push Alerts Active!",
            body = "You'll receive instant alerts for coin earnings, unclaimed Daily Streaks, and motivational boosts.",
            priority = NotificationCompat.PRIORITY_HIGH
        )
    }

    fun showAdminCustomNotification(
        context: Context,
        notificationKey: String,
        title: String,
        body: String
    ): Boolean {
        ensureChannels(context)
        if (!hasNotificationPermission(context)) return false
        val offset = (notificationKey.hashCode() and 0x7FFFFFFF) % 500
        return postNotification(
            context = context,
            channelId = CHANNEL_ADMIN_BROADCAST,
            notificationId = NOTIFICATION_ID_ADMIN_BASE + offset,
            title = title,
            body = body,
            priority = NotificationCompat.PRIORITY_HIGH
        )
    }

    @SuppressLint("MissingPermission")
    private fun postNotification(
        context: Context,
        channelId: String,
        notificationId: Int,
        title: String,
        body: String,
        priority: Int
    ): Boolean {
        return try {
            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val contentPendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_notification_coin)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(priority)
                .setAutoCancel(true)
                .setContentIntent(contentPendingIntent)
                .build()

            NotificationManagerCompat.from(context).notify(notificationId, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }
}
