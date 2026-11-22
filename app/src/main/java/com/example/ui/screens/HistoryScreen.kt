package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.GameMatchRecord
import com.example.model.MatchOutcome
import com.example.model.RewardEvent
import com.example.model.RewardType
import com.example.model.UiState
import com.example.model.UserProfile
import com.example.ui.components.AdMobTestBannerCard
import com.example.ui.components.MatchHistoryCard
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CoralError
import com.example.ui.theme.CyanSecondary
import com.example.ui.theme.EmeraldOnPrimary
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MidnightCard
import com.example.ui.theme.MidnightCardHigh
import com.example.ui.theme.MidnightOutline
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

private enum class HistoryOutcomeFilter(val label: String) {
    ALL("All"),
    WON("Won"),
    LOST("Lost"),
    DRAW("Draw")
}

@Composable
fun HistoryScreen(
    profile: UserProfile,
    matchesState: UiState<List<GameMatchRecord>>,
    rewardsState: UiState<List<RewardEvent>>,
    onClaimMatchAd: (String) -> Unit,
    onPlayTicTacToe: () -> Unit,
    isAdBlockerActive: Boolean = false,
    bannerAdUnitId: String = com.example.ads.AdSdkInfo.TEST_BANNER_AD_UNIT_ID,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var outcomeFilter by remember { mutableStateOf(HistoryOutcomeFilter.ALL) }
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.US) }

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
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "History",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Your recent Tic Tac Toe matches & coin payouts",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = onPlayTicTacToe,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldPrimary,
                            contentColor = EmeraldOnPrimary
                        ),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("history_play_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Play Tic Tac Toe",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                        Text(
                            text = "Play",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Summary Strip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HistorySummaryTile(
                        title = "Matches",
                        value = "${profile.totalGamesPlayed}",
                        icon = Icons.Filled.SportsEsports,
                        tint = CyanSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    HistorySummaryTile(
                        title = "Win Rate",
                        value = profile.formattedWinRate(),
                        icon = Icons.Filled.EmojiEvents,
                        tint = EmeraldPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    HistorySummaryTile(
                        title = "Coins Earned",
                        value = "${profile.totalCoinsEarned}",
                        icon = Icons.Filled.MonetizationOn,
                        tint = AmberGold,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Section Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MidnightCard,
                    contentColor = EmeraldPrimary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Matches") },
                        modifier = Modifier.testTag("history_tab_matches")
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Rewards & Ads") },
                        modifier = Modifier.testTag("history_tab_rewards")
                    )
                }

                if (selectedTab == 0) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(HistoryOutcomeFilter.entries) { filter ->
                            val selected = outcomeFilter == filter
                            FilterChip(
                                selected = selected,
                                onClick = { outcomeFilter = filter },
                                label = { Text(filter.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldPrimary.copy(alpha = 0.2f),
                                    selectedLabelColor = EmeraldPrimary,
                                    containerColor = MidnightCard,
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = selected,
                                    borderColor = MidnightOutline,
                                    selectedBorderColor = EmeraldPrimary.copy(alpha = 0.6f)
                                ),
                                modifier = Modifier.testTag("history_filter_${filter.name.lowercase(Locale.US)}")
                            )
                        }
                    }
                }
            }
        }

        if (selectedTab == 0) {
            when (matchesState) {
                is UiState.Loading -> item {
                    HistoryLoadingCard("Loading match history...")
                }

                is UiState.Error -> item {
                    HistoryErrorCard(matchesState.message)
                }

                is UiState.Success -> {
                    val allMatches = matchesState.data
                    val filteredMatches = when (outcomeFilter) {
                        HistoryOutcomeFilter.ALL -> allMatches
                        HistoryOutcomeFilter.WON -> allMatches.filter {
                            MatchOutcome.fromRaw(it.outcome) == MatchOutcome.WIN
                        }
                        HistoryOutcomeFilter.LOST -> allMatches.filter {
                            MatchOutcome.fromRaw(it.outcome) == MatchOutcome.LOSS
                        }
                        HistoryOutcomeFilter.DRAW -> allMatches.filter {
                            MatchOutcome.fromRaw(it.outcome) == MatchOutcome.DRAW
                        }
                    }

                    if (filteredMatches.isEmpty()) {
                        item {
                            val emptyMsg = if (allMatches.isEmpty()) {
                                "No Tic Tac Toe matches yet. Start Level 1 vs the Medium Bot to record your wins, losses, and dates!"
                            } else {
                                "No ${outcomeFilter.label.lowercase(Locale.US)} matches recorded yet."
                            }
                            HistoryEmptyCard(message = emptyMsg)
                        }
                    } else {
                        items(filteredMatches, key = { it.id }) { match ->
                            MatchHistoryCard(
                                match = match,
                                onClaimPendingMatchAd = onClaimMatchAd,
                                modifier = Modifier.widthIn(max = 720.dp)
                            )
                        }
                    }
                }
            }
        } else {
            when (rewardsState) {
                is UiState.Loading -> item {
                    HistoryLoadingCard("Loading ad & reward history...")
                }

                is UiState.Error -> item {
                    HistoryErrorCard(rewardsState.message)
                }

                is UiState.Success -> {
                    val rewards = rewardsState.data
                    if (rewards.isEmpty()) {
                        item {
                            HistoryEmptyCard(
                                message = "No ad or streak rewards recorded yet. Complete a match or claim your daily streak!"
                            )
                        }
                    } else {
                        items(rewards, key = { it.id }) { event ->
                            val ts = event.createdAt ?: Timestamp.now()
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MidnightCard),
                                border = BorderStroke(1.dp, MidnightOutline),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .widthIn(max = 720.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val parsedRewardType = RewardType.fromRaw(event.rewardType)
                                    val eventAccent = when (parsedRewardType) {
                                        RewardType.EXTRA_EARN -> EmeraldPrimary
                                        RewardType.REFERRAL_BONUS -> AmberGold
                                        else -> CyanSecondary
                                    }
                                    val eventIcon = when (parsedRewardType) {
                                        RewardType.EXTRA_EARN -> Icons.Filled.Savings
                                        RewardType.REFERRAL_BONUS -> Icons.Filled.CardGiftcard
                                        else -> Icons.Filled.PlayCircle
                                    }
                                    Surface(
                                        color = eventAccent.copy(alpha = 0.16f),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.size(42.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = eventIcon,
                                                contentDescription = "Reward event",
                                                tint = eventAccent,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = event.note,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${event.rewardType} • ${dateFormatter.format(ts.toDate())}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Surface(
                                        color = EmeraldPrimary.copy(alpha = 0.16f),
                                        shape = RoundedCornerShape(50)
                                    ) {
                                        Text(
                                            text = "+${event.coins}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = EmeraldPrimary,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
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
private fun HistorySummaryTile(
    title: String,
    value: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MidnightCardHigh,
        shape = RoundedCornerShape(16.dp),
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
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HistoryLoadingCard(text: String) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MidnightCard),
        border = BorderStroke(1.dp, MidnightOutline),
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 720.dp)
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
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HistoryErrorCard(message: String) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 720.dp)
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = CoralError,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun HistoryEmptyCard(message: String) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MidnightCard),
        border = BorderStroke(1.dp, MidnightOutline),
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 720.dp)
            .testTag("history_empty_card")
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.History,
                contentDescription = "No history",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
