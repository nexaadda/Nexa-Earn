package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AdUnits
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.AdminNotification
import com.example.model.AppConfig
import com.example.model.UserProfile
import com.example.model.WithdrawalMethod
import com.example.model.WithdrawalRequest
import com.example.model.WithdrawalStatus
import com.example.model.coinsToRupees
import com.example.model.formatRupees
import com.example.ui.components.WhatsAppGreen
import com.example.ui.components.WhatsAppLogoIcon
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CoralError
import com.example.ui.theme.CyanSecondary
import com.example.ui.theme.EmeraldOnPrimary
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MidnightCard
import com.example.ui.theme.MidnightCardHigh
import com.example.ui.theme.MidnightOutline
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun AdminOverviewTab(
    users: List<UserProfile>,
    withdrawals: List<WithdrawalRequest>,
    appConfig: AppConfig,
    isBusy: Boolean,
    onJumpToTab: (Int) -> Unit,
    onSaveAppConfig: (AppConfig) -> Unit
) {
    val numFmt = remember { NumberFormat.getIntegerInstance(Locale.US) }
    val pendingCount = withdrawals.count { it.parsedStatus() == WithdrawalStatus.PENDING }
    val completedCount = withdrawals.count { it.parsedStatus() == WithdrawalStatus.COMPLETED }
    val totalCoins = users.sumOf { it.coinsBalance }
    val totalAds = users.sumOf { it.totalAdsWatched }

    var maintenanceMode by remember { mutableStateOf(appConfig.maintenanceMode) }
    var maintenanceMsg by remember { mutableStateOf(appConfig.maintenanceMessage) }
    var annTitle by remember { mutableStateOf(appConfig.announcementTitle) }
    var annBody by remember { mutableStateOf(appConfig.announcementBody) }

    LaunchedEffect(appConfig) {
        maintenanceMode = appConfig.maintenanceMode
        maintenanceMsg = appConfig.maintenanceMessage
        annTitle = appConfig.announcementTitle
        annBody = appConfig.announcementBody
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // KPI Cards Row 1
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AdminKpiCard(
                icon = Icons.Filled.Group,
                label = "Total Users",
                value = numFmt.format(users.size),
                accent = CyanSecondary,
                modifier = Modifier
                    .weight(1f)
                    .testTag("admin_total_users_kpi"),
                onClick = { onJumpToTab(1) }
            )
            AdminKpiCard(
                icon = Icons.Filled.PendingActions,
                label = "Pending Requests",
                value = "$pendingCount",
                accent = if (pendingCount > 0) AmberGold else EmeraldPrimary,
                modifier = Modifier
                    .weight(1f)
                    .testTag("admin_pending_requests_kpi"),
                onClick = { onJumpToTab(2) }
            )
        }

        // KPI Cards Row 2
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AdminKpiCard(
                icon = Icons.Filled.MonetizationOn,
                label = "Total Coins Held",
                value = numFmt.format(totalCoins),
                accent = AmberGold,
                modifier = Modifier.weight(1f),
                onClick = { onJumpToTab(1) }
            )
            AdminKpiCard(
                icon = Icons.Filled.CheckCircle,
                label = "Paid / Ads Watched",
                value = "$completedCount / ${numFmt.format(totalAds)}",
                accent = EmeraldPrimary,
                modifier = Modifier.weight(1f),
                onClick = { onJumpToTab(2) }
            )
        }

        // KPI Cards Row 3: Access Status & Live AdMob Units
        val bannedCount = users.count { it.banned }
        val whitelistedCount = users.count { it.whitelisted && !it.banned }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AdminKpiCard(
                icon = Icons.Filled.GppBad,
                label = "Blacklisted / Whitelisted",
                value = "$bannedCount / $whitelistedCount",
                accent = if (bannedCount > 0) CoralError else EmeraldPrimary,
                modifier = Modifier
                    .weight(1f)
                    .testTag("admin_access_status_kpi"),
                onClick = { onJumpToTab(1) }
            )
            AdminKpiCard(
                icon = Icons.Filled.AdUnits,
                label = "Live AdMob (Rew / Ban)",
                value = "${if (appConfig.rewardedAdUnitId.isNotBlank()) "ON" else "OFF"} / ${if (appConfig.bannerAdUnitId.isNotBlank()) "ON" else "OFF"}",
                accent = CyanSecondary,
                modifier = Modifier
                    .weight(1f)
                    .testTag("admin_admob_status_kpi"),
                onClick = { onJumpToTab(3) }
            )
        }

        // Live Home Announcement Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MidnightCard),
            border = BorderStroke(1.dp, CyanSecondary.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.Campaign, contentDescription = null, tint = CyanSecondary)
                    Text(
                        text = "Live Home Screen Announcement",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }
                OutlinedTextField(
                    value = annTitle,
                    onValueChange = { annTitle = it },
                    label = { Text("Announcement Title (blank = hide banner)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = annBody,
                    onValueChange = { annBody = it },
                    label = { Text("Announcement Message") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            onSaveAppConfig(
                                appConfig.copy(
                                    announcementTitle = annTitle.trim(),
                                    announcementBody = annBody.trim(),
                                    maintenanceMode = maintenanceMode,
                                    maintenanceMessage = maintenanceMsg.trim()
                                )
                            )
                        },
                        enabled = !isBusy,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanSecondary,
                            contentColor = Color(0xFF082F49)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("Update Live", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = {
                            annTitle = ""
                            annBody = ""
                            onSaveAppConfig(
                                appConfig.copy(
                                    announcementTitle = "",
                                    announcementBody = "",
                                    maintenanceMode = maintenanceMode,
                                    maintenanceMessage = maintenanceMsg.trim()
                                )
                            )
                        },
                        enabled = !isBusy,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("Clear")
                    }
                }
            }
        }

        // Live Maintenance Mode Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MidnightCard),
            border = BorderStroke(
                1.dp,
                if (maintenanceMode) CoralError.copy(alpha = 0.6f) else MidnightOutline
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "App Maintenance Mode",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (maintenanceMode) {
                                "ON — Users see maintenance notice & WhatsApp Support"
                            } else {
                                "OFF — App is live for all users"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (maintenanceMode) CoralError else EmeraldPrimary
                        )
                    }
                    Switch(
                        checked = maintenanceMode,
                        onCheckedChange = { checked ->
                            maintenanceMode = checked
                            onSaveAppConfig(
                                appConfig.copy(
                                    announcementTitle = annTitle.trim(),
                                    announcementBody = annBody.trim(),
                                    maintenanceMode = checked,
                                    maintenanceMessage = maintenanceMsg.trim()
                                )
                            )
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = CoralError
                        )
                    )
                }
                OutlinedTextField(
                    value = maintenanceMsg,
                    onValueChange = { maintenanceMsg = it },
                    label = { Text("Maintenance Screen Message") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private enum class AdminUserFilter(val label: String) {
    ALL("All"),
    ACTIVE("Active"),
    WHITELISTED("Whitelisted"),
    BANNED("Banned")
}

@Composable
fun AdminUsersTab(
    users: List<UserProfile>,
    isBusy: Boolean,
    onAdjustCoins: (targetUid: String, deltaCoins: Long, note: String) -> Unit,
    onSetUserAccessStatus: (targetUid: String, banned: Boolean, whitelisted: Boolean, reason: String?) -> Unit,
    onQuickNotifyUser: (UserProfile) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf(AdminUserFilter.ALL) }
    var coinEditUser by remember { mutableStateOf<UserProfile?>(null) }
    var accessEditUser by remember { mutableStateOf<UserProfile?>(null) }
    val numFmt = remember { NumberFormat.getIntegerInstance(Locale.US) }

    val bannedCount = remember(users) { users.count { it.banned } }
    val whitelistedCount = remember(users) { users.count { it.whitelisted && !it.banned } }

    val filteredUsers = remember(users, searchQuery, statusFilter) {
        val q = searchQuery.trim().lowercase(Locale.US)
        users.filter { u ->
            val matchesFilter = when (statusFilter) {
                AdminUserFilter.ALL -> true
                AdminUserFilter.ACTIVE -> !u.banned
                AdminUserFilter.WHITELISTED -> u.whitelisted && !u.banned
                AdminUserFilter.BANNED -> u.banned
            }
            val matchesQuery = q.isEmpty() ||
                u.email.orEmpty().lowercase(Locale.US).contains(q) ||
                u.displayName.lowercase(Locale.US).contains(q) ||
                u.userId.lowercase(Locale.US).contains(q)
            matchesFilter && matchesQuery
        }
    }

    coinEditUser?.let { target ->
        AdjustUserCoinsDialog(
            user = target,
            isBusy = isBusy,
            onDismiss = { coinEditUser = null },
            onConfirm = { delta, note ->
                onAdjustCoins(target.userId, delta, note)
                coinEditUser = null
            }
        )
    }

    accessEditUser?.let { target ->
        ManageUserAccessDialog(
            user = target,
            isBusy = isBusy,
            onDismiss = { accessEditUser = null },
            onUpdateStatus = { banned, whitelisted, reason ->
                onSetUserAccessStatus(target.userId, banned, whitelisted, reason)
                accessEditUser = null
            }
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search users by Email or Username") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search users") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("admin_user_search_input")
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                AdminUserFilter.ALL to "All (${users.size})",
                AdminUserFilter.BANNED to "Banned ($bannedCount)",
                AdminUserFilter.WHITELISTED to "Whitelisted ($whitelistedCount)",
                AdminUserFilter.ACTIVE to "Active"
            ).forEach { (filter, label) ->
                val selected = statusFilter == filter
                FilterChip(
                    selected = selected,
                    onClick = { statusFilter = filter },
                    label = { Text(label, maxLines = 1) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = if (filter == AdminUserFilter.BANNED) {
                            CoralError.copy(alpha = 0.2f)
                        } else {
                            EmeraldPrimary.copy(alpha = 0.2f)
                        },
                        selectedLabelColor = if (filter == AdminUserFilter.BANNED) {
                            CoralError
                        } else {
                            EmeraldPrimary
                        }
                    )
                )
            }
        }

        Text(
            text = "Showing ${filteredUsers.size} of ${users.size} users",
            style = MaterialTheme.typography.labelMedium,
            color = CyanSecondary
        )

        if (filteredUsers.isEmpty()) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MidnightCard),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (searchQuery.isBlank()) {
                        "No users in this filter."
                    } else {
                        "No users match \"$searchQuery\"."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(18.dp)
                )
            }
        } else {
            filteredUsers.forEach { user ->
                val cardBorderColor = when {
                    user.banned -> CoralError.copy(alpha = 0.65f)
                    user.whitelisted -> EmeraldPrimary.copy(alpha = 0.55f)
                    else -> MidnightOutline
                }
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MidnightCard),
                    border = BorderStroke(1.dp, cardBorderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = user.displayName,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (user.banned) {
                                        Surface(
                                            color = CoralError.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(50)
                                        ) {
                                            Text(
                                                text = "BLACKLISTED",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = CoralError,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    } else if (user.whitelisted) {
                                        Surface(
                                            color = EmeraldPrimary.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(50)
                                        ) {
                                            Text(
                                                text = "WHITELISTED",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = EmeraldPrimary,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = user.email ?: "No email linked",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CyanSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Surface(
                                color = AmberGold.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = "${numFmt.format(user.coinsBalance)} Coins",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = AmberGold,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }

                        if (user.banned && !user.banReason.isNullOrBlank()) {
                            Surface(
                                color = CoralError.copy(alpha = 0.10f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, CoralError.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Ban Reason: ${user.banReason}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CoralError,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MiniInfoTag("Earned: ${numFmt.format(user.totalCoinsEarned)}", Modifier.weight(1f))
                            MiniInfoTag("Games: ${user.totalGamesPlayed}", Modifier.weight(1f))
                            MiniInfoTag("Ads: ${user.totalAdsWatched}", Modifier.weight(1f))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { coinEditUser = user },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EmeraldPrimary,
                                    contentColor = EmeraldOnPrimary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Icon(Icons.Filled.CurrencyExchange, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.size(6.dp))
                                Text("Coins", fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                            Button(
                                onClick = { accessEditUser = user },
                                enabled = !isBusy,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (user.banned) EmeraldPrimary else CoralError,
                                    contentColor = if (user.banned) EmeraldOnPrimary else Color.White
                                ),
                                modifier = Modifier
                                    .weight(1.25f)
                                    .height(48.dp)
                                    .testTag("admin_user_access_button_${user.userId}")
                            ) {
                                Icon(
                                    imageVector = if (user.banned) Icons.Filled.VerifiedUser else Icons.Filled.Block,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.size(6.dp))
                                Text(
                                    text = if (user.banned) "Unban / Allow" else "Ban / Whitelist",
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                            OutlinedButton(
                                onClick = { onQuickNotifyUser(user) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Icon(Icons.Filled.NotificationsActive, contentDescription = "Notify user", tint = CyanSecondary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminRequestsTab(
    withdrawals: List<WithdrawalRequest>,
    usersById: Map<String, UserProfile>,
    isBusy: Boolean,
    onUpdateStatus: (WithdrawalRequest, WithdrawalStatus, Boolean, String) -> Unit
) {
    var selectedFilter by remember { mutableStateOf<WithdrawalStatus?>(WithdrawalStatus.PENDING) }
    var actionTarget by remember { mutableStateOf<Pair<WithdrawalRequest, WithdrawalStatus>?>(null) }
    val dateFmt = remember { SimpleDateFormat("MMM dd • hh:mm a", Locale.US) }

    val pendingCount = withdrawals.count { it.parsedStatus() == WithdrawalStatus.PENDING }
    val filtered = remember(withdrawals, selectedFilter) {
        if (selectedFilter == null) withdrawals else withdrawals.filter { it.parsedStatus() == selectedFilter }
    }

    actionTarget?.let { (wd, targetStatus) ->
        ReviewRequestDialog(
            request = wd,
            user = usersById[wd.userId],
            targetStatus = targetStatus,
            isBusy = isBusy,
            onDismiss = { actionTarget = null },
            onConfirm = { refundOnFail, note ->
                onUpdateStatus(wd, targetStatus, refundOnFail, note)
                actionTarget = null
            }
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                WithdrawalStatus.PENDING to "Pending ($pendingCount)",
                WithdrawalStatus.COMPLETED to "Approved",
                WithdrawalStatus.FAILED to "Failed",
                null to "All"
            ).forEach { (status, label) ->
                val selected = selectedFilter == status
                FilterChip(
                    selected = selected,
                    onClick = { selectedFilter = status },
                    label = { Text(label, maxLines = 1) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = EmeraldPrimary.copy(alpha = 0.2f),
                        selectedLabelColor = EmeraldPrimary
                    )
                )
            }
        }

        if (filtered.isEmpty()) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MidnightCard),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "No ${selectedFilter?.label?.lowercase() ?: ""} requests right now.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(18.dp)
                )
            }
        } else {
            filtered.forEach { wd ->
                val owner = usersById[wd.userId]
                val status = wd.parsedStatus()
                val statusColor = when (status) {
                    WithdrawalStatus.PENDING -> AmberGold
                    WithdrawalStatus.COMPLETED -> EmeraldPrimary
                    WithdrawalStatus.FAILED -> CoralError
                }
                val methodLabel = WithdrawalMethod.fromRaw(wd.method).label
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MidnightCard),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${owner?.displayName ?: wd.accountTitle} • ${wd.shortTransactionId()}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = owner?.email ?: wd.userId,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CyanSecondary
                                )
                            }
                            Surface(
                                color = statusColor.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = status.label.uppercase(Locale.US),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = statusColor,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Text(
                            text = "$methodLabel • ${wd.accountTitle} (${wd.destination})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${wd.coins} Coins (${formatRupees(coinsToRupees(wd.coins))})",
                                style = MaterialTheme.typography.labelLarge,
                                color = AmberGold,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = wd.createdAt?.toDate()?.let { dateFmt.format(it) } ?: "Just now",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (!wd.adminNote.isNullOrBlank()) {
                            Text(
                                text = "Note: ${wd.adminNote}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (status == WithdrawalStatus.PENDING) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { actionTarget = wd to WithdrawalStatus.COMPLETED },
                                    enabled = !isBusy,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = EmeraldPrimary,
                                        contentColor = EmeraldOnPrimary
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                ) {
                                    Text("Approve", fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { actionTarget = wd to WithdrawalStatus.FAILED },
                                    enabled = !isBusy,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = CoralError,
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                ) {
                                    Text("Failed", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminNotifyAndConfigTab(
    users: List<UserProfile>,
    notifications: List<AdminNotification>,
    appConfig: AppConfig,
    preselectedUser: UserProfile?,
    isBusy: Boolean,
    onSendNotification: (title: String, body: String, targetUid: String, targetEmail: String?) -> Unit,
    onDeleteNotification: (String) -> Unit,
    onSaveAppConfig: (AppConfig) -> Unit
) {
    var sendToAll by remember { mutableStateOf(preselectedUser == null) }
    var targetSearch by remember { mutableStateOf(preselectedUser?.email ?: "") }
    var selectedUser by remember { mutableStateOf(preselectedUser) }
    var notifTitle by remember { mutableStateOf("") }
    var notifBody by remember { mutableStateOf("") }

    LaunchedEffect(preselectedUser) {
        if (preselectedUser != null) {
            sendToAll = false
            selectedUser = preselectedUser
            targetSearch = preselectedUser.email ?: preselectedUser.displayName
        }
    }

    var whatsappInput by remember { mutableStateOf(appConfig.whatsappSupportNumber) }
    var prefillInput by remember { mutableStateOf(appConfig.whatsappPrefillMessage) }
    var supportHoursInput by remember { mutableStateOf(appConfig.supportHours) }
    var termsInput by remember { mutableStateOf(appConfig.termsAndConditions) }

    LaunchedEffect(appConfig) {
        whatsappInput = appConfig.whatsappSupportNumber
        prefillInput = appConfig.whatsappPrefillMessage
        supportHoursInput = appConfig.supportHours
        termsInput = appConfig.termsAndConditions
    }

    val matchingUsers = remember(users, targetSearch) {
        val q = targetSearch.trim().lowercase(Locale.US)
        if (q.isEmpty()) users.take(5) else users.filter {
            it.email.orEmpty().lowercase(Locale.US).contains(q) ||
                it.displayName.lowercase(Locale.US).contains(q)
        }.take(5)
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // 1. Custom Push Notification Composer
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MidnightCard),
            border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.NotificationsActive, contentDescription = null, tint = EmeraldPrimary)
                    Text(
                        text = "Send Custom Push Notification",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = sendToAll,
                        onClick = { sendToAll = true },
                        label = { Text("All Users") }
                    )
                    FilterChip(
                        selected = !sendToAll,
                        onClick = { sendToAll = false },
                        label = { Text("Specific User") }
                    )
                }

                if (!sendToAll) {
                    OutlinedTextField(
                        value = targetSearch,
                        onValueChange = {
                            targetSearch = it
                            selectedUser = null
                        },
                        label = { Text("Recipient User Email / Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    matchingUsers.forEach { candidate ->
                        val picked = selectedUser?.userId == candidate.userId
                        Surface(
                            onClick = {
                                selectedUser = candidate
                                targetSearch = candidate.email ?: candidate.displayName
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = if (picked) EmeraldPrimary.copy(alpha = 0.18f) else MidnightCardHigh,
                            border = BorderStroke(1.dp, if (picked) EmeraldPrimary else MidnightOutline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(candidate.displayName, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                                    Text(candidate.email ?: candidate.userId, style = MaterialTheme.typography.labelSmall, color = CyanSecondary)
                                }
                                if (picked) {
                                    Icon(Icons.Filled.CheckCircle, contentDescription = "Selected", tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = notifTitle,
                    onValueChange = { notifTitle = it },
                    label = { Text("Notification Title") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_notif_title_input")
                )
                OutlinedTextField(
                    value = notifBody,
                    onValueChange = { notifBody = it },
                    label = { Text("Notification Message") },
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_notif_body_input")
                )

                Button(
                    onClick = {
                        val tUid = if (sendToAll) "ALL" else selectedUser?.userId.orEmpty()
                        val tEmail = if (sendToAll) null else selectedUser?.email
                        onSendNotification(notifTitle, notifBody, tUid, tEmail)
                        if (notifTitle.isNotBlank() && notifBody.isNotBlank() && (sendToAll || tUid.isNotBlank())) {
                            notifTitle = ""
                            notifBody = ""
                        }
                    },
                    enabled = !isBusy && notifTitle.isNotBlank() && notifBody.isNotBlank() && (sendToAll || selectedUser != null),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldPrimary,
                        contentColor = EmeraldOnPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("admin_send_notif_button")
                ) {
                    Icon(Icons.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = if (sendToAll) "Send Push to All Users" else "Send Push to Selected User",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 2. Customer Support WhatsApp Live Editor
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MidnightCard),
            border = BorderStroke(1.dp, WhatsAppGreen.copy(alpha = 0.55f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    WhatsAppLogoIcon(size = 24.dp)
                    Text(
                        text = "Customer Support WhatsApp (Live)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }
                OutlinedTextField(
                    value = whatsappInput,
                    onValueChange = { whatsappInput = it },
                    label = { Text("WhatsApp Number (with country code, e.g. 97798...)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_whatsapp_input")
                )
                OutlinedTextField(
                    value = prefillInput,
                    onValueChange = { prefillInput = it },
                    label = { Text("Default WhatsApp Greeting") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = supportHoursInput,
                    onValueChange = { supportHoursInput = it },
                    label = { Text("Support Hours Label") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        onSaveAppConfig(
                            appConfig.copy(
                                whatsappSupportNumber = whatsappInput,
                                whatsappPrefillMessage = prefillInput,
                                supportHours = supportHoursInput,
                                termsAndConditions = termsInput
                            )
                        )
                    },
                    enabled = !isBusy,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WhatsAppGreen,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Save Support Number Live", fontWeight = FontWeight.Bold)
                }
            }
        }

        // 3. Terms & Conditions Live Editor
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MidnightCard),
            border = BorderStroke(1.dp, CyanSecondary.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.Gavel, contentDescription = null, tint = CyanSecondary)
                    Text(
                        text = "Terms & Conditions (Live Editor)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }
                OutlinedTextField(
                    value = termsInput,
                    onValueChange = { termsInput = it },
                    label = { Text("Terms & Conditions Content") },
                    minLines = 6,
                    maxLines = 14,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_terms_input")
                )
                Button(
                    onClick = {
                        onSaveAppConfig(
                            appConfig.copy(
                                whatsappSupportNumber = whatsappInput,
                                whatsappPrefillMessage = prefillInput,
                                supportHours = supportHoursInput,
                                termsAndConditions = termsInput
                            )
                        )
                    },
                    enabled = !isBusy && termsInput.isNotBlank(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanSecondary,
                        contentColor = Color(0xFF082F49)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("admin_save_terms_button")
                ) {
                    Text("Save Terms & Conditions Live", fontWeight = FontWeight.Bold)
                }
            }
        }

        // 4. Recent Sent Notifications
        if (notifications.isNotEmpty()) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MidnightCard),
                border = BorderStroke(1.dp, MidnightOutline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Recent Live Notifications (${notifications.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    notifications.take(15).forEach { item ->
                        Surface(
                            color = MidnightCardHigh,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MidnightOutline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                                    Text(item.body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = if (item.targetType == "ALL") "Target: All Users" else "Target: ${item.targetEmail ?: item.targetUid}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CyanSecondary
                                    )
                                }
                                IconButton(onClick = { onDeleteNotification(item.id) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete notification", tint = CoralError)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminKpiCard(
    icon: ImageVector,
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = MidnightCard,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = label, tint = accent, modifier = Modifier.size(22.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MiniInfoTag(text: String, modifier: Modifier = Modifier) {
    Surface(
        color = MidnightCardHigh,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun AdjustUserCoinsDialog(
    user: UserProfile,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (deltaCoins: Long, note: String) -> Unit
) {
    var isAdding by remember { mutableStateOf(true) }
    var amountText by remember { mutableStateOf("100") }
    var noteText by remember { mutableStateOf("") }
    val amount = amountText.trim().toLongOrNull() ?: 0L

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adjust Coins • ${user.displayName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "${user.email ?: user.userId}\nCurrent Balance: ${user.coinsBalance} Coins",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = isAdding,
                        onClick = { isAdding = true },
                        leadingIcon = { Icon(Icons.Filled.AddCircle, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text("Add (+)") }
                    )
                    FilterChip(
                        selected = !isAdding,
                        onClick = { isAdding = false },
                        leadingIcon = { Icon(Icons.Filled.RemoveCircle, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text("Subtract (-)") }
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(50L, 100L, 500L, 1000L).forEach { preset ->
                        Surface(
                            onClick = { amountText = preset.toString() },
                            shape = RoundedCornerShape(8.dp),
                            color = MidnightCardHigh,
                            border = BorderStroke(1.dp, MidnightOutline)
                        ) {
                            Text(
                                text = "$preset",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Coin Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Admin Reason / Note (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val delta = if (isAdding) amount else -amount
                    if (delta != 0L) onConfirm(delta, noteText)
                },
                enabled = !isBusy && amount > 0L,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isAdding) EmeraldPrimary else CoralError,
                    contentColor = if (isAdding) EmeraldOnPrimary else Color.White
                )
            ) {
                Text(if (isAdding) "Add +$amount" else "Subtract -$amount")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun ReviewRequestDialog(
    request: WithdrawalRequest,
    user: UserProfile?,
    targetStatus: WithdrawalStatus,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (refundCoinsOnFail: Boolean, note: String) -> Unit
) {
    val isApprove = targetStatus == WithdrawalStatus.COMPLETED
    var refundCoins by remember { mutableStateOf(!isApprove) }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isApprove) "Approve Payout Request?" else "Mark Request Failed?")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "${user?.displayName ?: request.accountTitle} (${user?.email ?: request.userId})\n${request.coins} Coins • ${request.destination}",
                    style = MaterialTheme.typography.bodySmall
                )
                if (!isApprove) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = refundCoins, onCheckedChange = { refundCoins = it })
                        Text(
                            text = "Refund ${request.coins} coins back to user's wallet",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(if (isApprove) "Approval Note (optional)" else "Failure Reason (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(refundCoins, note) },
                enabled = !isBusy,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isApprove) EmeraldPrimary else CoralError,
                    contentColor = if (isApprove) EmeraldOnPrimary else Color.White
                )
            ) {
                Text(if (isApprove) "Confirm Approve" else "Mark Failed")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun ManageUserAccessDialog(
    user: UserProfile,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onUpdateStatus: (banned: Boolean, whitelisted: Boolean, reason: String?) -> Unit
) {
    var banReason by remember { mutableStateOf(user.banReason ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (user.banned) {
                    "Unban / Whitelist User"
                } else {
                    "Access Control • ${user.displayName}"
                }
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "${user.email ?: user.userId}\nCurrent Status: ${
                        when {
                            user.banned -> "Blacklisted / Banned"
                            user.whitelisted -> "Whitelisted (Trusted)"
                            else -> "Active (Standard)"
                        }
                    }",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!user.banned) {
                    OutlinedTextField(
                        value = banReason,
                        onValueChange = { banReason = it },
                        label = { Text("Ban / Blacklist Reason (optional)") },
                        maxLines = 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_ban_reason_input")
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Ad Blocker Abuse", "Fake / Bot Activity", "Policy Violation").forEach { preset ->
                            Surface(
                                onClick = { banReason = preset },
                                shape = RoundedCornerShape(8.dp),
                                color = MidnightCardHigh,
                                border = BorderStroke(1.dp, MidnightOutline)
                            ) {
                                Text(
                                    text = preset,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = MidnightOutline)
                    Button(
                        onClick = { onUpdateStatus(true, false, banReason) },
                        enabled = !isBusy,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CoralError,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("admin_confirm_ban_button")
                    ) {
                        Icon(Icons.Filled.Block, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Ban & Blacklist Account", fontWeight = FontWeight.Bold)
                    }
                    if (!user.whitelisted) {
                        OutlinedButton(
                            onClick = { onUpdateStatus(false, true, null) },
                            enabled = !isBusy,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, EmeraldPrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("admin_confirm_whitelist_button")
                        ) {
                            Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.size(6.dp))
                            Text("Mark Trusted / Whitelist", color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        OutlinedButton(
                            onClick = { onUpdateStatus(false, false, null) },
                            enabled = !isBusy,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text("Remove Whitelist (Set Standard Active)")
                        }
                    }
                } else {
                    Button(
                        onClick = { onUpdateStatus(false, true, null) },
                        enabled = !isBusy,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldPrimary,
                            contentColor = EmeraldOnPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("admin_confirm_unban_whitelist_button")
                    ) {
                        Icon(Icons.Filled.VerifiedUser, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Unban & Whitelist User", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { onUpdateStatus(false, false, null) },
                        enabled = !isBusy,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("admin_confirm_unban_active_button")
                    ) {
                        Text("Unban Only (Standard Active)")
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
