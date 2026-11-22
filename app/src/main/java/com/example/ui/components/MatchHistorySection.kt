package com.example.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.GameMatchRecord
import com.example.model.MatchOutcome
import com.example.model.MiniGameType
import com.example.model.UiState
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CoralError
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MidnightCard
import com.example.ui.theme.MidnightCardHigh
import com.example.ui.theme.MidnightOutline
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun MatchHistorySection(
    matchesState: UiState<List<GameMatchRecord>>,
    onClaimPendingMatchAd: (String) -> Unit,
    modifier: Modifier = Modifier,
    maxItems: Int? = null,
    onViewAllClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("match_history_section"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Match History",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Recent mini-game rounds & rewarded ad payouts",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (onViewAllClick != null) {
                TextButton(
                    onClick = onViewAllClick,
                    modifier = Modifier.testTag("match_history_view_all_button")
                ) {
                    Text(
                        text = "View All",
                        color = EmeraldPrimary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

        when (matchesState) {
            is UiState.Loading -> {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MidnightCard),
                    border = BorderStroke(1.dp, MidnightOutline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = EmeraldPrimary,
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "Loading match history...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            is UiState.Error -> {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = matchesState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            is UiState.Success -> {
                val allMatches = matchesState.data
                if (allMatches.isEmpty()) {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MidnightCard),
                        border = BorderStroke(1.dp, MidnightOutline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("match_history_empty_card")
                    ) {
                        Row(
                            modifier = Modifier.padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                color = MidnightCardHigh,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.History,
                                        contentDescription = "No match history",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "No matches played yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Play a Tic Tac Toe match against the Medium Bot to record your wins, losses, and dates here.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    val wins = allMatches.count { MatchOutcome.fromRaw(it.outcome) == MatchOutcome.WIN }
                    val losses = allMatches.count { MatchOutcome.fromRaw(it.outcome) == MatchOutcome.LOSS }
                    val draws = allMatches.count { MatchOutcome.fromRaw(it.outcome) == MatchOutcome.DRAW }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutcomeSummaryPill(
                            label = "$wins Won",
                            color = EmeraldPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        OutcomeSummaryPill(
                            label = "$losses Lost",
                            color = CoralError,
                            modifier = Modifier.weight(1f)
                        )
                        OutcomeSummaryPill(
                            label = "$draws Draw",
                            color = AmberGold,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    val displayed = if (maxItems != null) allMatches.take(maxItems) else allMatches
                    displayed.forEach { match ->
                        MatchHistoryCard(
                            match = match,
                            onClaimPendingMatchAd = onClaimPendingMatchAd
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MatchHistoryCard(
    match: GameMatchRecord,
    onClaimPendingMatchAd: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val outcome = MatchOutcome.fromRaw(match.outcome)
    val (outcomeLabel, outcomeColor, outcomeIcon) = when (outcome) {
        MatchOutcome.WIN -> Triple("Won", EmeraldPrimary, Icons.Filled.EmojiEvents)
        MatchOutcome.LOSS -> Triple("Lost", CoralError, Icons.Filled.Close)
        MatchOutcome.DRAW -> Triple("Draw", AmberGold, Icons.Filled.Balance)
    }

    val dateTimeFormatter = remember { SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.US) }
    val rawDateParser = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val dateOnlyFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.US) }

    val formattedDate = remember(match.id, match.createdAt, match.playedDate) {
        val ts = match.createdAt
        if (ts != null) {
            dateTimeFormatter.format(ts.toDate())
        } else if (match.playedDate.isNotBlank()) {
            val parsed = runCatching { rawDateParser.parse(match.playedDate) }.getOrNull()
            if (parsed != null) dateOnlyFormatter.format(parsed) else match.playedDate
        } else {
            "Just now"
        }
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MidnightCard),
        border = BorderStroke(1.dp, MidnightOutline),
        modifier = modifier
            .fillMaxWidth()
            .testTag("match_history_item_${match.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = outcomeColor.copy(alpha = 0.16f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, outcomeColor.copy(alpha = 0.45f)),
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = outcomeIcon,
                        contentDescription = outcomeLabel,
                        tint = outcomeColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = outcomeColor.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = outcomeLabel.uppercase(Locale.US),
                            style = MaterialTheme.typography.labelSmall,
                            color = outcomeColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    val gameType = match.parsedGameType()
                    val matchTitle = when (gameType) {
                        MiniGameType.TIC_TAC_TOE -> "Tic Tac Toe • Level ${match.levelNumber}"
                        MiniGameType.TAP_TAP -> "Tap Tap 30s • Round ${match.levelNumber}"
                        MiniGameType.MATH_QUIZ -> "Math Quiz • Level ${match.levelNumber}"
                    }
                    Text(
                        text = matchTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CalendarToday,
                        contentDescription = "Match date",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (match.adWatched) {
                Surface(
                    color = EmeraldPrimary.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "+${match.coinsAwarded}",
                        style = MaterialTheme.typography.labelMedium,
                        color = EmeraldPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    )
                }
            } else {
                FilledTonalButton(
                    onClick = { onClaimPendingMatchAd(match.id) },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = AmberGold.copy(alpha = 0.2f),
                        contentColor = AmberGold
                    ),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("claim_match_ad_${match.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayCircle,
                        contentDescription = "Watch ad to claim",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(
                        text = "+${match.rewardCoinsForGame()}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun OutcomeSummaryPill(
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = color,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
