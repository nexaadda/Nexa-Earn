package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.model.AdminNotification
import com.example.model.AppConfig
import com.example.model.UserProfile
import com.example.model.WithdrawalRequest
import com.example.model.WithdrawalStatus
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CoralError
import com.example.ui.theme.CyanSecondary
import com.example.ui.theme.EmeraldOnPrimary
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MidnightCard
import com.example.ui.theme.MidnightCardHigh
import com.example.ui.theme.MidnightOutline

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    isUnlocked: Boolean,
    isUnlocking: Boolean,
    authError: String?,
    isBusy: Boolean,
    users: List<UserProfile>,
    withdrawals: List<WithdrawalRequest>,
    notifications: List<AdminNotification>,
    appConfig: AppConfig,
    onUnlock: (String) -> Unit,
    onLockAndExit: () -> Unit,
    onAdjustCoins: (targetUid: String, deltaCoins: Long, note: String) -> Unit,
    onSetUserAccessStatus: (targetUid: String, banned: Boolean, whitelisted: Boolean, reason: String?) -> Unit,
    onUpdateWithdrawalStatus: (WithdrawalRequest, WithdrawalStatus, Boolean, String) -> Unit,
    onSendNotification: (title: String, body: String, targetUid: String, targetEmail: String?) -> Unit,
    onDeleteNotification: (String) -> Unit,
    onSaveAppConfig: (AppConfig) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onNavigateBack)

    if (!isUnlocked) {
        AdminPasswordLockGate(
            isUnlocking = isUnlocking,
            authError = authError,
            onUnlock = onUnlock,
            onNavigateBack = onNavigateBack,
            modifier = modifier
        )
        return
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    var notifyPreselectedUser by remember { mutableStateOf<UserProfile?>(null) }
    val usersById = remember(users) { users.associateBy { it.userId } }
    val tabs = listOf("Overview", "Users", "Requests", "AdMob IDs", "Notify & Live")

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("admin_dashboard_screen")
    ) {
        // Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("admin_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Icon(
                    imageVector = Icons.Filled.AdminPanelSettings,
                    contentDescription = "Admin Console",
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(26.dp)
                )
                Column {
                    Text(
                        text = "Admin Console",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Live Firestore Sync • ${users.size} Users",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldPrimary
                    )
                }
            }

            OutlinedButton(
                onClick = onLockAndExit,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, CoralError.copy(alpha = 0.6f)),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("admin_lock_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = "Lock Admin Panel",
                    tint = CoralError,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text("Lock", color = CoralError)
            }
        }

        PrimaryScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = MidnightCard,
            contentColor = EmeraldPrimary,
            edgePadding = 12.dp
        ) {
            tabs.forEachIndexed { idx, title ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTab == idx) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 14.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 760.dp)
                ) {
                    when (selectedTab) {
                        0 -> AdminOverviewTab(
                            users = users,
                            withdrawals = withdrawals,
                            appConfig = appConfig,
                            isBusy = isBusy,
                            onJumpToTab = { selectedTab = it },
                            onSaveAppConfig = onSaveAppConfig
                        )
                        1 -> AdminUsersTab(
                            users = users,
                            isBusy = isBusy,
                            onAdjustCoins = onAdjustCoins,
                            onSetUserAccessStatus = onSetUserAccessStatus,
                            onQuickNotifyUser = { targetUser ->
                                notifyPreselectedUser = targetUser
                                selectedTab = 4
                            }
                        )
                        2 -> AdminRequestsTab(
                            withdrawals = withdrawals,
                            usersById = usersById,
                            isBusy = isBusy,
                            onUpdateStatus = onUpdateWithdrawalStatus
                        )
                        3 -> AdminAdMobTab(
                            appConfig = appConfig,
                            isBusy = isBusy,
                            onSaveAppConfig = onSaveAppConfig
                        )
                        else -> AdminNotifyAndConfigTab(
                            users = users,
                            notifications = notifications,
                            appConfig = appConfig,
                            preselectedUser = notifyPreselectedUser,
                            isBusy = isBusy,
                            onSendNotification = onSendNotification,
                            onDeleteNotification = onDeleteNotification,
                            onSaveAppConfig = onSaveAppConfig
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminPasswordLockGate(
    isUnlocking: Boolean,
    authError: String?,
    onUnlock: (String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = MidnightCard),
            border = BorderStroke(1.dp, CyanSecondary.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    color = AmberGold.copy(alpha = 0.16f),
                    shape = CircleShape,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = "Admin Password Required",
                            tint = AmberGold,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                Text(
                    text = "Restricted Admin Access",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Enter the administrator password to manage users, payouts, notifications, and live app settings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Admin Password") },
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (password.isNotEmpty() && !isUnlocking) {
                                onUnlock(password)
                            }
                        }
                    ),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (showPassword) "Hide password" else "Show password"
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_password_input")
                )

                if (!authError.isNullOrBlank()) {
                    Surface(
                        color = CoralError.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, CoralError.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = authError,
                            style = MaterialTheme.typography.bodySmall,
                            color = CoralError,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Button(
                    onClick = { onUnlock(password) },
                    enabled = password.isNotEmpty() && !isUnlocking,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldPrimary,
                        contentColor = EmeraldOnPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("admin_unlock_button")
                ) {
                    if (isUnlocking) {
                        CircularProgressIndicator(
                            color = EmeraldOnPrimary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Verifying...", fontWeight = FontWeight.Bold)
                    } else {
                        Text("Unlock Admin Panel", fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(
                    onClick = onNavigateBack,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MidnightOutline),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = MidnightCardHigh),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}
