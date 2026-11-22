package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.model.UserProfile
import com.example.model.todayDateString
import com.example.notifications.NexaEarnNotificationHelper
import com.example.notifications.NotificationSettings
import com.example.notifications.REMINDER_INTERVAL_OPTIONS_HOURS
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CoralError
import com.example.ui.theme.CyanSecondary
import com.example.ui.theme.EmeraldOnPrimary
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MidnightBg
import com.example.ui.theme.MidnightCard
import com.example.ui.theme.MidnightCardHigh
import com.example.ui.theme.MidnightOutline

@Composable
fun SettingsScreen(
    profile: UserProfile,
    notificationSettings: NotificationSettings,
    onToggleMaster: (Boolean) -> Unit,
    onToggleInactivityStreak: (Boolean) -> Unit,
    onToggleMotivational: (Boolean) -> Unit,
    onToggleEarningAlerts: (Boolean) -> Unit,
    onSelectReminderIntervalHours: (Int) -> Unit,
    onSendTestNotification: () -> Unit,
    onPermissionResultMessage: (String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasSystemPermission by remember {
        mutableStateOf(NexaEarnNotificationHelper.hasNotificationPermission(context))
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasSystemPermission = NexaEarnNotificationHelper.hasNotificationPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val openSystemNotificationSettings: () -> Unit = {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.fromParts("package", context.packageName, null)
            }
        }
        runCatching { context.startActivity(intent) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasSystemPermission = NexaEarnNotificationHelper.hasNotificationPermission(context)
        if (granted && hasSystemPermission) {
            onPermissionResultMessage("Push notifications enabled for NexaEarn!")
        } else {
            onPermissionResultMessage("Enable notifications in Android Settings to receive alerts.")
        }
    }

    val requestNotificationAccess: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !NexaEarnNotificationHelper.hasNotificationPermission(context)
        ) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            openSystemNotificationSettings()
        }
    }

    val streakClaimedToday = profile.hasClaimedStreakToday(todayDateString())

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Settings & Alerts",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Control push notifications, streak nudges & earning alerts",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Android System Permission Status Card
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MidnightCard),
                    border = BorderStroke(
                        1.dp,
                        if (hasSystemPermission) EmeraldPrimary.copy(alpha = 0.45f)
                        else AmberGold.copy(alpha = 0.55f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_permission_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        if (hasSystemPermission) EmeraldPrimary.copy(alpha = 0.14f)
                                        else AmberGold.copy(alpha = 0.14f),
                                        MidnightCard
                                    )
                                )
                            )
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (hasSystemPermission) EmeraldPrimary.copy(alpha = 0.18f)
                                        else AmberGold.copy(alpha = 0.2f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (hasSystemPermission) {
                                        Icons.Filled.NotificationsActive
                                    } else {
                                        Icons.Filled.NotificationsOff
                                    },
                                    contentDescription = "Notification permission status",
                                    tint = if (hasSystemPermission) EmeraldPrimary else AmberGold,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (hasSystemPermission) {
                                        "Push Notifications Permitted"
                                    } else {
                                        "Allow Push Notifications"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (hasSystemPermission) {
                                        "Device permission is active. NexaEarn can send streak, motivation, and coin alerts."
                                    } else {
                                        "Grant notification permission so you don't miss Daily Streak bonuses or instant coin updates."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (!hasSystemPermission) {
                            Button(
                                onClick = requestNotificationAccess,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EmeraldPrimary,
                                    contentColor = EmeraldOnPrimary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("settings_grant_permission_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.NotificationsActive,
                                    contentDescription = "Allow notifications",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                                Text("Enable Push Permission", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            OutlinedButton(
                                onClick = openSystemNotificationSettings,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MidnightOutline),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("settings_system_notification_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Tune,
                                    contentDescription = "Open system notification channels",
                                    tint = CyanSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                                Text("Manage Android Channels", color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }

                // Master Toggle Card
                NotificationToggleCard(
                    icon = Icons.Filled.NotificationsActive,
                    accentColor = EmeraldPrimary,
                    title = "All Push Notifications",
                    subtitle = if (notificationSettings.masterEnabled) {
                        "Master switch is ON — customize individual notification types below."
                    } else {
                        "All NexaEarn notifications are paused."
                    },
                    checked = notificationSettings.masterEnabled,
                    enabled = true,
                    switchTestTag = "settings_master_switch",
                    onCheckedChange = { enabled ->
                        onToggleMaster(enabled)
                        if (enabled && !hasSystemPermission) {
                            requestNotificationAccess()
                        }
                    }
                )

                Text(
                    text = "NOTIFICATION PREFERENCES",
                    style = MaterialTheme.typography.labelMedium,
                    color = CyanSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )

                // 1. Inactivity & Daily Streak Reminder Card + Frequency Selector
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MidnightCard),
                    border = BorderStroke(1.dp, MidnightOutline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(AmberGold.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.LocalFireDepartment,
                                    contentDescription = "Daily streak and inactivity alerts",
                                    tint = AmberGold,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "App Inactivity & Daily Streak",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Notify if you haven't opened the app or haven't claimed today's Daily Streak.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = notificationSettings.inactivityStreakEnabled,
                                onCheckedChange = onToggleInactivityStreak,
                                enabled = notificationSettings.masterEnabled,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = EmeraldOnPrimary,
                                    checkedTrackColor = EmeraldPrimary
                                ),
                                modifier = Modifier.testTag("settings_streak_switch")
                            )
                        }

                        // Today's Streak Status Chip
                        Surface(
                            color = if (streakClaimedToday) {
                                EmeraldPrimary.copy(alpha = 0.14f)
                            } else {
                                CoralError.copy(alpha = 0.14f)
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                1.dp,
                                if (streakClaimedToday) {
                                    EmeraldPrimary.copy(alpha = 0.4f)
                                } else {
                                    CoralError.copy(alpha = 0.4f)
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (streakClaimedToday) {
                                        Icons.Filled.CheckCircle
                                    } else {
                                        Icons.Filled.LocalFireDepartment
                                    },
                                    contentDescription = "Today's streak status",
                                    tint = if (streakClaimedToday) EmeraldPrimary else CoralError,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (streakClaimedToday) {
                                        "Today's Daily Streak claimed (${profile.currentStreak}d active)"
                                    } else {
                                        "Today's Daily Streak is still unclaimed — reminders will nudge you"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (streakClaimedToday) EmeraldPrimary else CoralError
                                )
                            }
                        }

                        // Reminder Delay Selector
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Schedule,
                                    contentDescription = "Reminder timing",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Reminder check interval",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                REMINDER_INTERVAL_OPTIONS_HOURS.forEach { hours ->
                                    val selected = notificationSettings.reminderDelayHours == hours
                                    FilterChip(
                                        selected = selected,
                                        onClick = { onSelectReminderIntervalHours(hours) },
                                        enabled = notificationSettings.masterEnabled &&
                                            (notificationSettings.inactivityStreakEnabled ||
                                                notificationSettings.motivationalEnabled),
                                        label = {
                                            Text(
                                                text = "Every ${hours}h",
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = EmeraldPrimary.copy(alpha = 0.22f),
                                            selectedLabelColor = EmeraldPrimary
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("settings_interval_${hours}h")
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Motivational Earning Notifications
                NotificationToggleCard(
                    icon = Icons.Filled.AutoAwesome,
                    accentColor = CyanSecondary,
                    title = "Motivational Earn Boosts",
                    subtitle = "Receive uplifting nudges to complete the 100-coin 24h Goal (+5% Extra Earn), play Math Quiz (+10), or Tap Tap 30s (+5).",
                    checked = notificationSettings.motivationalEnabled,
                    enabled = notificationSettings.masterEnabled,
                    switchTestTag = "settings_motivation_switch",
                    onCheckedChange = onToggleMotivational
                )

                // 3. Instant Coin Earning Push Notifications
                NotificationToggleCard(
                    icon = Icons.Filled.MonetizationOn,
                    accentColor = EmeraldPrimary,
                    title = "Instant Earning Alerts",
                    subtitle = "Send a push notification every time you earn coins from Watching Ads, claiming Daily Streak, or playing Mini Games.",
                    checked = notificationSettings.earningAlertsEnabled,
                    enabled = notificationSettings.masterEnabled,
                    switchTestTag = "settings_earning_switch",
                    onCheckedChange = onToggleEarningAlerts
                )

                // Test Notification Preview Card
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MidnightCardHigh),
                    border = BorderStroke(1.dp, CyanSecondary.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Test Push Notification",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Trigger an instant sample notification right now to confirm your device alerts look and sound right.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(
                            onClick = {
                                if (!hasSystemPermission) {
                                    requestNotificationAccess()
                                } else {
                                    onSendTestNotification()
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, CyanSecondary.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("settings_test_notification_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.NotificationsActive,
                                contentDescription = "Send test notification",
                                tint = CyanSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = "Send Test Notification",
                                color = CyanSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationToggleCard(
    icon: ImageVector,
    accentColor: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    switchTestTag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) MidnightCard else MidnightBg.copy(alpha = 0.7f)
        ),
        border = BorderStroke(
            1.dp,
            if (checked && enabled) accentColor.copy(alpha = 0.4f) else MidnightOutline
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = if (enabled) 0.16f else 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (enabled) accentColor else accentColor.copy(alpha = 0.5f),
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onBackground
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = EmeraldOnPrimary,
                    checkedTrackColor = EmeraldPrimary
                ),
                modifier = Modifier.testTag(switchTestTag)
            )
        }
    }
}
