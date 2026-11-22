package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.DAILY_GOAL_TARGET_COINS
import com.example.model.EXTRA_EARN_BONUS_PERCENT
import com.example.model.UserProfile
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CyanSecondary
import com.example.ui.theme.EmeraldOnPrimary
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MidnightBg
import com.example.ui.theme.MidnightCard
import com.example.ui.theme.MidnightCardHigh
import com.example.ui.theme.MidnightOutline
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun GoalAndExtraEarnCard(
    profile: UserProfile,
    isClaimingExtraEarn: Boolean,
    onClaimExtraEarn: () -> Unit,
    modifier: Modifier = Modifier
) {
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        nowMillis = System.currentTimeMillis()
        while (true) {
            delay(1000L)
            nowMillis = System.currentTimeMillis()
        }
    }

    val activeGoalCoins = profile.activeGoalCoins(nowMillis)
    val goalCompleted = profile.isDailyGoalCompleted(nowMillis)
    val goalFraction = (activeGoalCoins.toFloat() / DAILY_GOAL_TARGET_COINS.toFloat()).coerceIn(0f, 1f)
    val remainingMillis = profile.remainingGoalMillis(nowMillis)
    val timerLabel = "${formatGoalCountdown(remainingMillis)} • Reset 1 AM"

    val potWhole = profile.extraEarnPotWholeCoins()
    val potFormatted = profile.extraEarnPotFormatted()
    val hasFractionalRemainder = profile.extraEarnPotRemainderHundredths() > 0L

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MidnightCard),
        border = BorderStroke(
            1.dp,
            if (goalCompleted) EmeraldPrimary.copy(alpha = 0.6f) else CyanSecondary.copy(alpha = 0.45f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("goal_extra_earn_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            if (goalCompleted) EmeraldPrimary.copy(alpha = 0.12f) else CyanSecondary.copy(alpha = 0.08f),
                            MidnightCard
                        )
                    )
                )
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Goal Header + 24h Timer Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = (if (goalCompleted) EmeraldPrimary else CyanSecondary).copy(alpha = 0.18f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (goalCompleted) Icons.Filled.CheckCircle else Icons.Filled.Flag,
                                contentDescription = "24-hour 100 coin goal",
                                tint = if (goalCompleted) EmeraldPrimary else CyanSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "24h Goal • $DAILY_GOAL_TARGET_COINS Coins",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Daily Streak, Mini Games & Ads • Resets 1:00 AM",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = CyanSecondary.copy(alpha = 0.16f),
                    border = BorderStroke(
                        1.dp,
                        CyanSecondary.copy(alpha = 0.48f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Timer,
                            contentDescription = "Resets at 1:00 AM",
                            tint = CyanSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = timerLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = CyanSecondary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.testTag("goal_timer_label")
                        )
                    }
                }
            }

            // Progress Bar toward 100 Coins
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (goalCompleted) {
                            "Goal Completed! +$EXTRA_EARN_BONUS_PERCENT% Extra Earn Active"
                        } else {
                            "${(DAILY_GOAL_TARGET_COINS - activeGoalCoins).coerceAtLeast(0L)} coins to unlock +$EXTRA_EARN_BONUS_PERCENT% Extra Earn"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = if (goalCompleted) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (goalCompleted) FontWeight.Bold else FontWeight.Normal
                    )
                    Text(
                        text = "${activeGoalCoins.coerceAtMost(DAILY_GOAL_TARGET_COINS)} / $DAILY_GOAL_TARGET_COINS",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (goalCompleted) EmeraldPrimary else AmberGold,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag("goal_progress_value")
                    )
                }

                LinearProgressIndicator(
                    progress = { goalFraction },
                    color = if (goalCompleted) EmeraldPrimary else CyanSecondary,
                    trackColor = MidnightCardHigh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(50))
                        .testTag("goal_progress_bar")
                )
            }

            // Extra Earn Pot Surface
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MidnightBg.copy(alpha = 0.85f),
                border = BorderStroke(
                    1.dp,
                    if (potWhole >= 1L) AmberGold.copy(alpha = 0.6f) else MidnightOutline
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("extra_earn_pot_panel")
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                color = AmberGold.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.Savings,
                                        contentDescription = "Extra Earn Pot",
                                        tint = AmberGold,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Extra Earn Pot",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Surface(
                                        color = AmberGold.copy(alpha = 0.16f),
                                        shape = RoundedCornerShape(50)
                                    ) {
                                        Text(
                                            text = "+$EXTRA_EARN_BONUS_PERCENT%",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = AmberGold,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Claimed total: ${profile.totalExtraEarnClaimed} Coins",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (potWhole >= 1L) "$potWhole Claimable" else "$potFormatted Coins",
                                style = MaterialTheme.typography.titleMedium,
                                color = AmberGold,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.testTag("extra_earn_pot_value")
                            )
                            Text(
                                text = if (potWhole >= 1L) "Pot: $potFormatted (whole only)" else "Stored in pot",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (potWhole >= 1L) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (potWhole == 0L && hasFractionalRemainder) {
                        val neededFormatted = String.format(
                            Locale.US,
                            "%.2f",
                            (100L - profile.extraEarnPotRemainderHundredths()) / 100.0
                        )
                        Text(
                            text = "Only whole coins (e.g. 1, not 0.72) can be claimed. Earn $neededFormatted more in Extra Earn to unlock 1 whole coin.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (potWhole >= 1L) {
                        Text(
                            text = "Only whole coins ($potWhole, not decimals like $potFormatted) can be claimed. Auto-claims at 1:00 AM if unclaimed.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (profile.extraEarnPotHundredths == 0L) {
                        Text(
                            text = if (goalCompleted) {
                                "Goal complete! Extra earnings store +$EXTRA_EARN_BONUS_PERCENT% here (whole coins auto-claim at 1:00 AM)."
                            } else {
                                "Reach $DAILY_GOAL_TARGET_COINS coins before 1:00 AM so +$EXTRA_EARN_BONUS_PERCENT% of extra earnings fill this pot."
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = onClaimExtraEarn,
                        enabled = potWhole >= 1L && !isClaimingExtraEarn,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AmberGold,
                            contentColor = Color(0xFF451A03),
                            disabledContainerColor = MidnightCardHigh,
                            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("goal_claim_extra_earn_button")
                    ) {
                        if (isClaimingExtraEarn) {
                            CircularProgressIndicator(
                                color = Color(0xFF451A03),
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Icon(
                                imageVector = if (potWhole >= 1L) Icons.Filled.MonetizationOn else Icons.Filled.AutoAwesome,
                                contentDescription = "Claim Extra Earn pot",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(
                                text = when {
                                    potWhole >= 1L -> "Claim +$potWhole ${if (potWhole == 1L) "Coin" else "Coins"} from Extra Earn"
                                    hasFractionalRemainder -> "Accumulating ($potFormatted / 1.00 Coin)"
                                    else -> "Claim Extra Earn Anytime"
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExtraEarnPotCompactCard(
    profile: UserProfile,
    isClaimingExtraEarn: Boolean,
    onClaimExtraEarn: () -> Unit,
    modifier: Modifier = Modifier
) {
    val potWhole = profile.extraEarnPotWholeCoins()
    val potFormatted = profile.extraEarnPotFormatted()
    val goalCoins = profile.activeGoalCoins()

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MidnightCard),
        border = BorderStroke(
            1.dp,
            if (potWhole >= 1L) AmberGold.copy(alpha = 0.55f) else MidnightOutline
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("wallet_extra_earn_card")
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = AmberGold.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Savings,
                                contentDescription = "Extra Earn Pot",
                                tint = AmberGold,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Extra Earn Pot (+$EXTRA_EARN_BONUS_PERCENT%)",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Goal: ${goalCoins.coerceAtMost(DAILY_GOAL_TARGET_COINS)}/$DAILY_GOAL_TARGET_COINS • Resets & auto-claims 1:00 AM",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (potWhole >= 1L) "$potWhole Claimable" else "$potFormatted Coins",
                        style = MaterialTheme.typography.titleMedium,
                        color = AmberGold,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Pot: $potFormatted (whole only)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Button(
                onClick = onClaimExtraEarn,
                enabled = potWhole >= 1L && !isClaimingExtraEarn,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldPrimary,
                    contentColor = EmeraldOnPrimary,
                    disabledContainerColor = MidnightCardHigh,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("wallet_claim_extra_earn_button")
            ) {
                if (isClaimingExtraEarn) {
                    CircularProgressIndicator(
                        color = EmeraldOnPrimary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Text(
                        text = if (potWhole >= 1L) {
                            "Claim +$potWhole Whole ${if (potWhole == 1L) "Coin" else "Coins"} to Wallet"
                        } else {
                            "Claim Whole Coins (1.00+ in Pot)"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun formatGoalCountdown(remainingMillis: Long): String {
    val totalSeconds = (remainingMillis / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
}
