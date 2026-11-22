package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Outbox
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.BANK_WITHDRAWAL_FEE_RUPEES
import com.example.model.COINS_PER_RUPEE
import com.example.model.MIN_WITHDRAWAL_COINS
import com.example.model.UiState
import com.example.model.UserProfile
import com.example.model.WITHDRAW_PROCESSING_TIME_LABEL
import com.example.model.WithdrawalMethod
import com.example.model.WithdrawalRequest
import com.example.model.WithdrawalStatus
import com.example.model.coinsToRupees
import com.example.model.formatRupees
import com.example.model.withdrawalFeeRupees
import com.example.model.withdrawalNetRupees
import com.example.ui.components.AdMobTestBannerCard
import com.example.ui.components.ExtraEarnPotCompactCard
import com.example.ui.components.WithdrawDialog
import com.example.ui.components.WithdrawalMethodLogo
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CoralError
import com.example.ui.theme.CyanSecondary
import com.example.ui.theme.EmeraldOnPrimary
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MidnightCard
import com.example.ui.theme.MidnightCardHigh
import com.example.ui.theme.MidnightOutline
import com.google.firebase.Timestamp
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

private enum class WithdrawHistoryFilter(val label: String) {
    ALL("All"),
    PENDING("Pending"),
    COMPLETED("Completed")
}

@Composable
fun WalletScreen(
    profile: UserProfile,
    withdrawalsState: UiState<List<WithdrawalRequest>>,
    isSubmittingWithdrawal: Boolean,
    onSubmitWithdrawal: (
        method: WithdrawalMethod,
        accountTitle: String,
        destination: String,
        coins: Long,
        onSuccess: () -> Unit
    ) -> Unit,
    onSignOut: () -> Unit,
    isClaimingExtraEarn: Boolean = false,
    onClaimExtraEarn: () -> Unit = {},
    isAdBlockerActive: Boolean = false,
    bannerAdUnitId: String = com.example.ads.AdSdkInfo.TEST_BANNER_AD_UNIT_ID,
    modifier: Modifier = Modifier
) {
    var activeWithdrawMethod by remember { mutableStateOf<WithdrawalMethod?>(null) }
    var historyFilter by remember { mutableStateOf(WithdrawHistoryFilter.ALL) }
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.US) }
    val numberFormatter = remember { NumberFormat.getIntegerInstance(Locale.US) }
    val formattedCoins = numberFormatter.format(profile.coinsBalance)
    val formattedMinCoins = numberFormatter.format(MIN_WITHDRAWAL_COINS)
    val availableRupees = formatRupees(coinsToRupees(profile.coinsBalance))

    val dialogMethod = activeWithdrawMethod
    if (dialogMethod != null) {
        WithdrawDialog(
            availableCoins = profile.coinsBalance,
            defaultDisplayName = profile.displayName,
            initialMethod = dialogMethod,
            isSubmitting = isSubmittingWithdrawal,
            onDismiss = { activeWithdrawMethod = null },
            onSubmit = { method, title, dest, coins ->
                onSubmitWithdrawal(method, title, dest, coins) {
                    activeWithdrawMethod = null
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp),
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Wallet",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    OutlinedButton(
                        onClick = onSignOut,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Sign out",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Sign Out")
                    }
                }

                // Summary Card with Total Available Coins & Exchange Rate
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MidnightCard),
                    border = BorderStroke(1.dp, MidnightOutline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("wallet_summary_card")
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
                            Icon(
                                imageVector = Icons.Filled.AccountBalanceWallet,
                                contentDescription = "Wallet balance",
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(32.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Total Available Coins",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.testTag("wallet_total_available_coins_label")
                                )
                                Text(
                                    text = "$formattedCoins Coins",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = AmberGold,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.testTag("wallet_total_available_coins_value")
                                )
                                Text(
                                    text = "Approx. $availableRupees • ${profile.email ?: profile.displayName}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.testTag("wallet_total_available_rupees")
                                )
                            }
                        }

                        // Exchange Rate Banner
                        Surface(
                            color = EmeraldPrimary.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("wallet_exchange_rate_banner")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CurrencyExchange,
                                        contentDescription = "Exchange rate",
                                        tint = EmeraldPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Exchange Rate",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = "$COINS_PER_RUPEE Coins = Rs. 1",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = EmeraldPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            WalletStatBox(
                                title = "Games",
                                value = "${profile.totalGamesPlayed}",
                                icon = Icons.Filled.SportsEsports,
                                tint = CyanSecondary,
                                modifier = Modifier.weight(1f)
                            )
                            WalletStatBox(
                                title = "Best Streak",
                                value = "${profile.longestStreak}d",
                                icon = Icons.Filled.LocalFireDepartment,
                                tint = AmberGold,
                                modifier = Modifier.weight(1f)
                            )
                            WalletStatBox(
                                title = "Lifetime",
                                value = numberFormatter.format(profile.totalCoinsEarned),
                                icon = Icons.Filled.MonetizationOn,
                                tint = EmeraldPrimary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Extra Earn Pot Card (Claim 5% Goal Bonus Anytime)
                ExtraEarnPotCompactCard(
                    profile = profile,
                    isClaimingExtraEarn = isClaimingExtraEarn,
                    onClaimExtraEarn = onClaimExtraEarn
                )

                // Withdraw Button & 3 Method Options with Logos
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MidnightCard),
                    border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("wallet_withdraw_card")
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Withdraw Coins",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Minimum $formattedMinCoins coins (${formatRupees(coinsToRupees(MIN_WITHDRAWAL_COINS))}) • $COINS_PER_RUPEE Coins = Rs. 1",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Withdraw Processing Time Notice
                        Surface(
                            color = CyanSecondary.copy(alpha = 0.10f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, CyanSecondary.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("wallet_withdraw_processing_time")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Schedule,
                                    contentDescription = "Withdraw processing time",
                                    tint = CyanSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Withdraw Time: $WITHDRAW_PROCESSING_TIME_LABEL",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = CyanSecondary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // 3 Withdrawal Options with Logos
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            WithdrawOptionTile(
                                method = WithdrawalMethod.ESEWA_WALLET,
                                subtitle = "Instant payout to your eSewa ID • No fee",
                                onClick = { activeWithdrawMethod = WithdrawalMethod.ESEWA_WALLET }
                            )
                            WithdrawOptionTile(
                                method = WithdrawalMethod.BANK_ACCOUNT,
                                subtitle = "Direct bank transfer • Rs. ${BANK_WITHDRAWAL_FEE_RUPEES.toInt()} fee applies",
                                onClick = { activeWithdrawMethod = WithdrawalMethod.BANK_ACCOUNT }
                            )
                            WithdrawOptionTile(
                                method = WithdrawalMethod.SIM_RECHARGE,
                                subtitle = "Instant mobile top-up (NTC / Ncell / SmartCell) • No fee",
                                onClick = { activeWithdrawMethod = WithdrawalMethod.SIM_RECHARGE }
                            )
                        }

                        Button(
                            onClick = { activeWithdrawMethod = WithdrawalMethod.ESEWA_WALLET },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldPrimary,
                                contentColor = EmeraldOnPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("wallet_withdraw_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Outbox,
                                contentDescription = "Withdraw",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = "Withdraw Coins",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Withdraw History Section Header
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp)
                    .testTag("wallet_withdraw_history_header"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Withdraw History",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Estimated payout time: $WITHDRAW_PROCESSING_TIME_LABEL",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (withdrawalsState is UiState.Success && withdrawalsState.data.isNotEmpty()) {
                        Surface(
                            color = MidnightCardHigh,
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, MidnightOutline)
                        ) {
                            Text(
                                text = "${withdrawalsState.data.size} Requests",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                if (withdrawalsState is UiState.Success && withdrawalsState.data.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        WithdrawHistoryFilter.entries.forEach { filter ->
                            val selected = historyFilter == filter
                            FilterChip(
                                selected = selected,
                                onClick = { historyFilter = filter },
                                label = { Text(filter.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldPrimary.copy(alpha = 0.2f),
                                    selectedLabelColor = EmeraldPrimary,
                                    containerColor = MidnightCardHigh,
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag(
                                    "withdraw_history_filter_${filter.name.lowercase(Locale.US)}"
                                )
                            )
                        }
                    }
                }
            }
        }

        // Withdraw History Body (Loading, Error, Empty, or Filtered List)
        when (withdrawalsState) {
            is UiState.Loading -> {
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MidnightCard),
                        border = BorderStroke(1.dp, MidnightOutline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 720.dp)
                            .testTag("withdraw_history_loading")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = EmeraldPrimary,
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = "Loading withdraw history...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            is UiState.Error -> {
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MidnightCard),
                        border = BorderStroke(1.dp, CoralError.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 720.dp)
                            .testTag("withdraw_history_error")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ErrorOutline,
                                contentDescription = "Withdraw history error",
                                tint = CoralError
                            )
                            Text(
                                text = withdrawalsState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = CoralError
                            )
                        }
                    }
                }
            }

            is UiState.Success -> {
                val allWithdrawals = withdrawalsState.data
                val filteredWithdrawals = when (historyFilter) {
                    WithdrawHistoryFilter.ALL -> allWithdrawals
                    WithdrawHistoryFilter.PENDING -> allWithdrawals.filter {
                        it.parsedStatus() == WithdrawalStatus.PENDING
                    }
                    WithdrawHistoryFilter.COMPLETED -> allWithdrawals.filter {
                        it.parsedStatus() == WithdrawalStatus.COMPLETED
                    }
                }

                if (filteredWithdrawals.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MidnightCard),
                            border = BorderStroke(1.dp, MidnightOutline),
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 720.dp)
                                .testTag("withdraw_history_empty_state")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(22.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ReceiptLong,
                                    contentDescription = "No withdraw history",
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(34.dp)
                                )
                                Text(
                                    text = if (allWithdrawals.isEmpty()) {
                                        "No Withdraw History Yet"
                                    } else {
                                        "No ${historyFilter.label} Withdrawals"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (allWithdrawals.isEmpty()) {
                                        "Your submitted eSewa, Bank Account, and SIM Recharge transactions will appear here. Withdraw time is $WITHDRAW_PROCESSING_TIME_LABEL."
                                    } else {
                                        "Switch the filter above to inspect other withdrawal requests."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(filteredWithdrawals, key = { it.id }) { req ->
                        WithdrawHistoryItemCard(
                            request = req,
                            dateFormatter = dateFormatter,
                            numberFormatter = numberFormatter,
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 720.dp)
                        )
                    }
                }
            }
        }

        item {
            AdMobTestBannerCard(
                isAdBlockerActive = isAdBlockerActive,
                bannerAdUnitId = bannerAdUnitId,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp)
            )
        }
    }
}

@Composable
private fun WithdrawHistoryItemCard(
    request: WithdrawalRequest,
    dateFormatter: SimpleDateFormat,
    numberFormatter: NumberFormat,
    modifier: Modifier = Modifier
) {
    val method = WithdrawalMethod.fromRaw(request.method)
    val parsedStatus = request.parsedStatus()
    val ts = request.createdAt ?: Timestamp.now()
    val grossRs = formatRupees(coinsToRupees(request.coins))
    val feeRs = withdrawalFeeRupees(method)
    val netRs = formatRupees(withdrawalNetRupees(method, request.coins))

    val statusColor = when (parsedStatus) {
        WithdrawalStatus.PENDING -> AmberGold
        WithdrawalStatus.COMPLETED -> EmeraldPrimary
        WithdrawalStatus.FAILED -> CoralError
    }
    val statusIcon = when (parsedStatus) {
        WithdrawalStatus.PENDING -> Icons.Filled.Schedule
        WithdrawalStatus.COMPLETED -> Icons.Filled.CheckCircle
        WithdrawalStatus.FAILED -> Icons.Filled.ErrorOutline
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MidnightCard),
        border = BorderStroke(1.dp, MidnightOutline),
        modifier = modifier.testTag("withdrawal_item_${request.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                WithdrawalMethodLogo(method = method, size = 42.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = method.label,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${request.accountTitle} • ${request.destination}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Surface(
                    color = statusColor.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.4f)),
                    modifier = Modifier.testTag("withdrawal_status_${request.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = parsedStatus.label,
                            tint = statusColor,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = parsedStatus.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            HorizontalDivider(color = MidnightOutline.copy(alpha = 0.7f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "${request.shortTransactionId()} • ${dateFormatter.format(ts.toDate())}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val detailLine = buildString {
                        append("Gross $grossRs")
                        if (feeRs > 0.0) {
                            append(" • Fee ${formatRupees(feeRs)}")
                        }
                        if (parsedStatus == WithdrawalStatus.PENDING) {
                            append(" • Est. $WITHDRAW_PROCESSING_TIME_LABEL")
                        }
                    }
                    Text(
                        text = detailLine,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (parsedStatus == WithdrawalStatus.PENDING) CyanSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "-${numberFormatter.format(request.coins)} Coins",
                        style = MaterialTheme.typography.labelMedium,
                        color = AmberGold,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Net $netRs",
                        style = MaterialTheme.typography.labelMedium,
                        color = EmeraldPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun WithdrawOptionTile(
    method: WithdrawalMethod,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MidnightCardHigh,
        border = BorderStroke(1.dp, MidnightOutline),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("wallet_option_${method.name.lowercase(Locale.US)}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            WithdrawalMethodLogo(method = method, size = 42.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = method.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                color = EmeraldPrimary.copy(alpha = 0.14f),
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    text = "Select",
                    style = MaterialTheme.typography.labelSmall,
                    color = EmeraldPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
    }
}

@Composable
private fun WalletStatBox(
    title: String,
    value: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MidnightCardHigh,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MidnightOutline),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
