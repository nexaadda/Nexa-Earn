package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.BANK_WITHDRAWAL_FEE_RUPEES
import com.example.model.COINS_PER_RUPEE
import com.example.model.MIN_WITHDRAWAL_COINS
import com.example.model.WITHDRAW_PROCESSING_TIME_LABEL
import com.example.model.WithdrawalMethod
import com.example.model.coinsToRupees
import com.example.model.formatRupees
import com.example.model.withdrawalFeeRupees
import com.example.model.withdrawalNetRupees
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CoralError
import com.example.ui.theme.CyanSecondary
import com.example.ui.theme.EmeraldOnPrimary
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MidnightBg
import com.example.ui.theme.MidnightCard
import com.example.ui.theme.MidnightCardHigh
import com.example.ui.theme.MidnightOutline
import java.text.NumberFormat
import java.util.Locale

@Composable
fun WithdrawDialog(
    availableCoins: Long,
    defaultDisplayName: String,
    initialMethod: WithdrawalMethod,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (
        method: WithdrawalMethod,
        accountTitle: String,
        destination: String,
        coins: Long
    ) -> Unit
) {
    var selectedMethod by remember(initialMethod) { mutableStateOf(initialMethod) }
    var accountTitle by remember(initialMethod) {
        mutableStateOf(
            when (initialMethod) {
                WithdrawalMethod.ESEWA_WALLET -> defaultDisplayName
                WithdrawalMethod.BANK_ACCOUNT -> ""
                WithdrawalMethod.SIM_RECHARGE -> "NTC"
            }
        )
    }
    var destination by remember(initialMethod) { mutableStateOf("") }
    var coinsInput by remember(availableCoins) {
        mutableStateOf(MIN_WITHDRAWAL_COINS.toString())
    }
    var localError by remember { mutableStateOf<String?>(null) }

    val carriers = listOf("NTC", "Ncell", "SmartCell")
    val numberFormatter = remember { NumberFormat.getIntegerInstance(Locale.US) }
    val parsedCoins = coinsInput.toLongOrNull() ?: 0L
    val grossRupees = coinsToRupees(parsedCoins)
    val feeRupees = withdrawalFeeRupees(selectedMethod)
    val netRupees = withdrawalNetRupees(selectedMethod, parsedCoins)

    Dialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = MidnightCard),
            border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp)
                .widthIn(max = 520.dp)
                .testTag("withdraw_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Withdraw Coins",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Available: ${numberFormatter.format(availableCoins)} Coins (${formatRupees(coinsToRupees(availableCoins))})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AmberGold
                        )
                        Text(
                            text = "Rate: $COINS_PER_RUPEE Coins = Rs. 1 • Min ${numberFormatter.format(MIN_WITHDRAWAL_COINS)} Coins",
                            style = MaterialTheme.typography.labelMedium,
                            color = EmeraldPrimary
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        enabled = !isSubmitting,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("withdraw_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close withdraw dialog",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = "Select Withdrawal Method",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    WithdrawalMethod.entries.forEach { method ->
                        val selected = selectedMethod == method
                        val subtitle = when (method) {
                            WithdrawalMethod.ESEWA_WALLET -> "Instant transfer to your eSewa ID • No fee"
                            WithdrawalMethod.BANK_ACCOUNT -> "Direct deposit to your bank • Rs. ${BANK_WITHDRAWAL_FEE_RUPEES.toInt()} charge"
                            WithdrawalMethod.SIM_RECHARGE -> "Mobile top-up for NTC, Ncell & SmartCell • No fee"
                        }
                        val accent = when (method) {
                            WithdrawalMethod.ESEWA_WALLET -> Color(0xFF60BB46)
                            WithdrawalMethod.BANK_ACCOUNT -> CyanSecondary
                            WithdrawalMethod.SIM_RECHARGE -> AmberGold
                        }
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (selected) accent.copy(alpha = 0.14f) else MidnightBg,
                            border = BorderStroke(
                                width = if (selected) 1.5.dp else 1.dp,
                                color = if (selected) accent else MidnightOutline
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !isSubmitting) {
                                    selectedMethod = method
                                    localError = null
                                    accountTitle = when (method) {
                                        WithdrawalMethod.ESEWA_WALLET -> defaultDisplayName
                                        WithdrawalMethod.BANK_ACCOUNT -> ""
                                        WithdrawalMethod.SIM_RECHARGE -> "NTC"
                                    }
                                    destination = ""
                                }
                                .testTag("withdraw_method_${method.name.lowercase(Locale.US)}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                WithdrawalMethodLogo(method = method, size = 44.dp)
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
                                if (selected) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = "Selected method",
                                        tint = accent,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Prominent Bank Account Fee Notice
                AnimatedVisibility(visible = selectedMethod == WithdrawalMethod.BANK_ACCOUNT) {
                    Surface(
                        color = AmberGold.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.65f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bank_fee_notice_card")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Info,
                                contentDescription = "Bank withdrawal fee notice",
                                tint = AmberGold,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "Bank Account Charge Notice",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = AmberGold,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "A processing charge of Rs. ${BANK_WITHDRAWAL_FEE_RUPEES.toInt()} (1,000 coins) will be deducted from your payout when choosing Bank Account.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Method-specific details
                when (selectedMethod) {
                    WithdrawalMethod.ESEWA_WALLET -> {
                        OutlinedTextField(
                            value = accountTitle,
                            onValueChange = { accountTitle = it; localError = null },
                            label = { Text("eSewa Account Name") },
                            singleLine = true,
                            colors = withdrawFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("withdraw_account_title_input")
                        )
                        OutlinedTextField(
                            value = destination,
                            onValueChange = { destination = it; localError = null },
                            label = { Text("eSewa ID / Mobile Number") },
                            placeholder = { Text("98XXXXXXXX") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            colors = withdrawFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("withdraw_destination_input")
                        )
                    }

                    WithdrawalMethod.BANK_ACCOUNT -> {
                        OutlinedTextField(
                            value = accountTitle,
                            onValueChange = { accountTitle = it; localError = null },
                            label = { Text("Bank Name & Account Holder") },
                            placeholder = { Text("e.g., Global IME • Alex") },
                            singleLine = true,
                            colors = withdrawFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("withdraw_account_title_input")
                        )
                        OutlinedTextField(
                            value = destination,
                            onValueChange = { destination = it; localError = null },
                            label = { Text("Bank Account Number") },
                            placeholder = { Text("Enter account number") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = withdrawFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("withdraw_destination_input")
                        )
                    }

                    WithdrawalMethod.SIM_RECHARGE -> {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Select SIM Carrier",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                carriers.forEach { carrier ->
                                    val selected = accountTitle == carrier
                                    FilterChip(
                                        selected = selected,
                                        onClick = {
                                            accountTitle = carrier
                                            localError = null
                                        },
                                        label = { Text(carrier) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = AmberGold.copy(alpha = 0.2f),
                                            selectedLabelColor = AmberGold,
                                            containerColor = MidnightCardHigh,
                                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        modifier = Modifier.testTag("withdraw_carrier_${carrier.lowercase(Locale.US)}")
                                    )
                                }
                            }
                        }
                        OutlinedTextField(
                            value = destination,
                            onValueChange = { destination = it; localError = null },
                            label = { Text("Mobile Number to Recharge") },
                            placeholder = { Text("98XXXXXXXX") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            colors = withdrawFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("withdraw_destination_input")
                        )
                    }
                }

                // Coin amount selector
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(5000L, 10000L, 25000L).forEach { preset ->
                            FilterChip(
                                selected = coinsInput == preset.toString(),
                                onClick = {
                                    coinsInput = preset.toString()
                                    localError = null
                                },
                                label = { Text(numberFormatter.format(preset)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldPrimary.copy(alpha = 0.2f),
                                    selectedLabelColor = EmeraldPrimary,
                                    containerColor = MidnightCardHigh
                                )
                            )
                        }
                        if (availableCoins >= MIN_WITHDRAWAL_COINS) {
                            FilterChip(
                                selected = coinsInput == availableCoins.toString(),
                                onClick = {
                                    coinsInput = availableCoins.toString()
                                    localError = null
                                },
                                label = { Text("Max") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldPrimary.copy(alpha = 0.2f),
                                    selectedLabelColor = EmeraldPrimary,
                                    containerColor = MidnightCardHigh
                                )
                            )
                        }
                    }

                    OutlinedTextField(
                        value = coinsInput,
                        onValueChange = { input ->
                            coinsInput = input.filter { it.isDigit() }
                            localError = null
                        },
                        label = { Text("Coins to Withdraw (Min ${numberFormatter.format(MIN_WITHDRAWAL_COINS)})") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = withdrawFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("withdraw_coins_input")
                    )
                }

                // Live Exchange & Payout Summary
                Surface(
                    color = MidnightCardHigh,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MidnightOutline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("withdraw_payout_summary")
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SummaryRow(
                            label = "Exchange Rate",
                            value = "$COINS_PER_RUPEE Coins = Rs. 1",
                            valueColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        SummaryRow(
                            label = "Gross Value (${numberFormatter.format(parsedCoins)} Coins)",
                            value = formatRupees(grossRupees),
                            valueColor = MaterialTheme.colorScheme.onSurface
                        )
                        if (selectedMethod == WithdrawalMethod.BANK_ACCOUNT) {
                            SummaryRow(
                                label = "Bank Account Charge",
                                value = "- ${formatRupees(feeRupees)}",
                                valueColor = CoralError
                            )
                        }
                        SummaryRow(
                            label = "Withdraw Time",
                            value = WITHDRAW_PROCESSING_TIME_LABEL,
                            valueColor = CyanSecondary
                        )
                        HorizontalDivider(color = MidnightOutline)
                        SummaryRow(
                            label = "Net Payout",
                            value = formatRupees(netRupees),
                            valueColor = EmeraldPrimary,
                            isBold = true
                        )
                    }
                }

                if (localError != null) {
                    Text(
                        text = localError!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = CoralError,
                        modifier = Modifier.testTag("withdraw_error_text")
                    )
                }

                Button(
                    onClick = {
                        val trimmedTitle = accountTitle.trim()
                        val trimmedDest = destination.trim()
                        when {
                            availableCoins < MIN_WITHDRAWAL_COINS -> {
                                localError = "You need at least ${numberFormatter.format(MIN_WITHDRAWAL_COINS)} coins to withdraw (current: ${numberFormatter.format(availableCoins)})."
                            }
                            parsedCoins < MIN_WITHDRAWAL_COINS -> {
                                localError = "Minimum withdrawal is ${numberFormatter.format(MIN_WITHDRAWAL_COINS)} coins."
                            }
                            parsedCoins > availableCoins -> {
                                localError = "Amount exceeds available balance (${numberFormatter.format(availableCoins)} coins)."
                            }
                            trimmedTitle.isBlank() -> {
                                localError = "Please enter account or bank details."
                            }
                            trimmedDest.length < 4 -> {
                                localError = "Please enter a valid account or mobile number."
                            }
                            else -> {
                                localError = null
                                onSubmit(selectedMethod, trimmedTitle, trimmedDest, parsedCoins)
                            }
                        }
                    },
                    enabled = !isSubmitting,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldPrimary,
                        contentColor = EmeraldOnPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("withdraw_submit_button")
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = EmeraldOnPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Submitting...")
                    } else {
                        Text(
                            text = "Confirm Withdraw • ${formatRupees(netRupees)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    valueColor: Color,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (isBold) FontWeight.SemiBold else FontWeight.Normal
        )
        Text(
            text = value,
            style = if (isBold) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodySmall,
            color = valueColor,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun withdrawFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = EmeraldPrimary,
    unfocusedBorderColor = MidnightOutline,
    focusedLabelColor = EmeraldPrimary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    focusedTextColor = MaterialTheme.colorScheme.onBackground,
    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
    cursorColor = EmeraldPrimary
)
