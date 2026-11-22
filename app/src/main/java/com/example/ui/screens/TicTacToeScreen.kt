package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.game.CellMark
import com.example.model.COINS_PER_GAME
import com.example.model.GameMatchRecord
import com.example.model.MatchOutcome
import com.example.model.UiState
import com.example.model.UserProfile
import com.example.model.todayDateString
import com.example.ui.TicTacToeUiState
import com.example.ui.components.MatchHistorySection
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CoralError
import com.example.ui.theme.CyanSecondary
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MidnightBg
import com.example.ui.theme.MidnightCard
import com.example.ui.theme.MidnightOutline

@Composable
fun TicTacToeScreen(
    profile: UserProfile,
    gameState: TicTacToeUiState,
    matchesState: UiState<List<GameMatchRecord>>,
    onStartMatch: () -> Unit,
    onCellTap: (Int) -> Unit,
    onClaimPendingMatchAd: () -> Unit,
    onClaimMatchAd: (String) -> Unit,
    onOpenTutorial: () -> Unit = {},
    onNavigateBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val today = todayDateString()
    val playedToday = profile.effectiveDailyGamesPlayed(today).toInt()
    val nextLevel = profile.ticTacToeNextLevel(today)

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
                    .widthIn(max = 560.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header & Level Quota
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (onNavigateBack != null) {
                            IconButton(
                                onClick = onNavigateBack,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("ttt_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Tic Tac Toe",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.SmartToy,
                                    contentDescription = "Medium Bot",
                                    tint = CyanSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Bot Difficulty: MEDIUM • +$COINS_PER_GAME coins/game",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = onOpenTutorial,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("ttt_help_tutorial_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                contentDescription = "How to play and earn coins",
                                tint = CyanSecondary
                            )
                        }
                        Surface(
                            color = EmeraldPrimary.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "$playedToday Today • Unlimited",
                                style = MaterialTheme.typography.labelMedium,
                                color = EmeraldPrimary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                // Turn / Result Banner
                Card(
                    colors = CardDefaults.cardColors(containerColor = MidnightCard),
                    border = BorderStroke(1.dp, MidnightOutline),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val statusTitle = when {
                                gameState.autoLaunchingAd -> "Loading Google AdMob Rewarded Ad..."
                                gameState.outcome == MatchOutcome.WIN -> "Victory! Level ${gameState.currentLevelNumber} Complete"
                                gameState.outcome == MatchOutcome.LOSS -> "Bot Won! Level ${gameState.currentLevelNumber} Complete"
                                gameState.outcome == MatchOutcome.DRAW -> "Draw! Level ${gameState.currentLevelNumber} Complete"
                                gameState.isMatchActive && gameState.isBotThinking -> "Medium Bot (O) is thinking..."
                                gameState.isMatchActive -> "Your Turn (X) • Level ${gameState.currentLevelNumber}"
                                else -> "Ready for Level $nextLevel (Unlimited)"
                            }
                            Text(
                                text = statusTitle,
                                style = MaterialTheme.typography.titleMedium,
                                color = when (gameState.outcome) {
                                    MatchOutcome.WIN -> EmeraldPrimary
                                    MatchOutcome.LOSS -> CoralError
                                    MatchOutcome.DRAW -> AmberGold
                                    null -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                            Text(
                                text = if (gameState.isMatchActive) {
                                    "After this match ends, a Google AdMob ad starts automatically."
                                } else {
                                    "Watch the full Google AdMob ad after every match to add +$COINS_PER_GAME coins."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (gameState.isBotThinking || gameState.autoLaunchingAd) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = CyanSecondary,
                                strokeWidth = 2.5.dp
                            )
                        }
                    }
                }

                // 3x3 Board
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MidnightCard),
                    border = BorderStroke(1.dp, MidnightOutline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        for (row in 0 until 3) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                for (col in 0 until 3) {
                                    val index = row * 3 + col
                                    val mark = gameState.board[index]
                                    val isWinningCell = index in gameState.winningLine
                                    BoardCell(
                                        index = index,
                                        mark = mark,
                                        isWinningCell = isWinningCell,
                                        enabled = gameState.isMatchActive &&
                                            !gameState.isBotThinking &&
                                            mark == CellMark.EMPTY &&
                                            gameState.outcome == null,
                                        onClick = { onCellTap(index) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                // Action Area: Daily limit lock OR Claim skipped post-match ad OR Start Next Match
                AnimatedVisibility(visible = gameState.outcome != null && !gameState.autoLaunchingAd && !gameState.lastMatchAdClaimed && gameState.lastRecordedMatchId != null) {
                    Button(
                        onClick = onClaimPendingMatchAd,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AmberGold,
                            contentColor = Color(0xFF451A03)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("claim_pending_match_ad_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayCircle,
                            contentDescription = "Watch post-match rewarded ad",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = "Watch Google AdMob Ad to Claim +$COINS_PER_GAME Coins",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (!gameState.isMatchActive && !gameState.autoLaunchingAd) {
                    Button(
                        onClick = onStartMatch,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldPrimary,
                            contentColor = Color(0xFF022C22)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("start_tic_tac_toe_match_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Start match",
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = if (gameState.outcome == null) {
                                "Start Level $nextLevel"
                            } else {
                                "Play Next Level (Level $nextLevel)"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Match History Section
                MatchHistorySection(
                    matchesState = matchesState,
                    onClaimPendingMatchAd = onClaimMatchAd
                )
            }
        }
    }
}

@Composable
private fun BoardCell(
    index: Int,
    mark: CellMark,
    isWinningCell: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = when {
        isWinningCell && mark == CellMark.PLAYER_X -> EmeraldPrimary
        isWinningCell && mark == CellMark.BOT_O -> CoralError
        else -> MidnightOutline
    }
    val cellBg = when {
        isWinningCell && mark == CellMark.PLAYER_X -> EmeraldPrimary.copy(alpha = 0.16f)
        isWinningCell && mark == CellMark.BOT_O -> CoralError.copy(alpha = 0.16f)
        else -> MidnightBg
    }
    val cellDesc = when (mark) {
        CellMark.EMPTY -> "Empty board cell ${index + 1}"
        CellMark.PLAYER_X -> "Player X at cell ${index + 1}"
        CellMark.BOT_O -> "Bot O at cell ${index + 1}"
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(cellBg)
            .border(1.5.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = cellDesc }
            .testTag("ttt_cell_$index"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            when (mark) {
                CellMark.PLAYER_X -> {
                    val stroke = 14f
                    drawLine(
                        color = EmeraldPrimary,
                        start = Offset(0f, 0f),
                        end = Offset(size.width, size.height),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = EmeraldPrimary,
                        start = Offset(size.width, 0f),
                        end = Offset(0f, size.height),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                }
                CellMark.BOT_O -> {
                    drawCircle(
                        color = CyanSecondary,
                        radius = size.minDimension / 2f,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 14f)
                    )
                }
                CellMark.EMPTY -> Unit
            }
        }
    }
}
