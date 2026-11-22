package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.model.TAP_TAP_COINS
import com.example.model.TAP_TAP_DURATION_SECONDS
import com.example.model.TAP_TAP_TARGET_TAPS
import com.example.ui.TapTapUiState
import com.example.ui.components.AdMobTestBannerCard
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
fun TapTapScreen(
    tapTapState: TapTapUiState,
    onStartRound: () -> Unit,
    onTapPressed: () -> Unit,
    onClaimPendingAd: () -> Unit,
    onLeaveScreen: () -> Unit,
    onNavigateBack: () -> Unit,
    isAdBlockerActive: Boolean = false,
    bannerAdUnitId: String = com.example.ads.AdSdkInfo.TEST_BANNER_AD_UNIT_ID,
    modifier: Modifier = Modifier
) {
    DisposableEffect(Unit) {
        onDispose { onLeaveScreen() }
    }

    val goalReached = tapTapState.tapCount >= TAP_TAP_TARGET_TAPS
    val tapProgress = (tapTapState.tapCount.toFloat() / TAP_TAP_TARGET_TAPS.toFloat()).coerceIn(0f, 1f)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val buttonScale by animateFloatAsState(
        targetValue = if (isPressed && tapTapState.isRunning) 0.93f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 700f),
        label = "tap_pad_scale"
    )

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
                // Header Row
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
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("tap_tap_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Column {
                            Text(
                                text = "Tap Tap Challenge",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Reach $TAP_TAP_TARGET_TAPS taps in ${TAP_TAP_DURATION_SECONDS}s • Auto Google AdMob ad",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        color = AmberGold.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MonetizationOn,
                                contentDescription = "Reward",
                                tint = AmberGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "+$TAP_TAP_COINS",
                                style = MaterialTheme.typography.labelLarge,
                                color = AmberGold,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Timer + Tap Goal Progress Card
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MidnightCard),
                    border = BorderStroke(
                        1.dp,
                        if (goalReached) EmeraldPrimary.copy(alpha = 0.55f) else MidnightOutline
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val timerTint = when {
                                    tapTapState.isRunning && tapTapState.secondsRemaining <= 5 && !goalReached -> CoralError
                                    goalReached -> EmeraldPrimary
                                    else -> CyanSecondary
                                }
                                Icon(
                                    imageVector = Icons.Filled.Timer,
                                    contentDescription = "Time remaining",
                                    tint = timerTint,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = "${tapTapState.secondsRemaining}s",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = timerTint,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.testTag("tap_tap_timer_text")
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${tapTapState.tapCount} / $TAP_TAP_TARGET_TAPS",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = if (goalReached) EmeraldPrimary else AmberGold,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.testTag("tap_tap_count_text")
                                )
                                Text(
                                    text = if (goalReached) "100+ Goal Reached!" else "Target: $TAP_TAP_TARGET_TAPS Taps",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (goalReached) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        LinearProgressIndicator(
                            progress = { tapProgress },
                            color = if (goalReached) EmeraldPrimary else AmberGold,
                            trackColor = MidnightCardHigh,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(50))
                                .testTag("tap_tap_progress_bar")
                        )
                    }
                }

                // Giant Interactive Tap Pad
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MidnightCard),
                    border = BorderStroke(1.dp, MidnightOutline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 28.dp, horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        val ringColor = when {
                            goalReached -> EmeraldPrimary
                            tapTapState.isRunning -> CyanSecondary
                            else -> AmberGold
                        }
                        Box(
                            modifier = Modifier
                                .size(210.dp)
                                .scale(buttonScale)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            ringColor.copy(alpha = if (tapTapState.isRunning) 0.35f else 0.16f),
                                            MidnightBg
                                        )
                                    )
                                )
                                .clickable(
                                    enabled = tapTapState.isRunning,
                                    interactionSource = interactionSource,
                                    indication = null,
                                    onClick = onTapPressed
                                )
                                .semantics {
                                    contentDescription = "Tap rapidly to reach $TAP_TAP_TARGET_TAPS taps in $TAP_TAP_DURATION_SECONDS seconds"
                                }
                                .testTag("tap_tap_target_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MidnightCardHigh,
                                border = BorderStroke(3.dp, ringColor),
                                modifier = Modifier.size(182.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = if (goalReached) Icons.Filled.CheckCircle else Icons.Filled.TouchApp,
                                        contentDescription = null,
                                        tint = ringColor,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Text(
                                        text = "${tapTapState.tapCount}",
                                        style = MaterialTheme.typography.displayMedium,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = when {
                                            tapTapState.isRunning && goalReached -> "KEEP GOING!"
                                            tapTapState.isRunning -> "TAP FAST!"
                                            tapTapState.isFinished && tapTapState.passedGoal -> "COMPLETE"
                                            tapTapState.isFinished -> "TIME UP"
                                            else -> "READY"
                                        },
                                        style = MaterialTheme.typography.labelLarge,
                                        color = ringColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Text(
                            text = when {
                                tapTapState.autoLaunchingAd -> "30s complete! Starting rewarded ad automatically..."
                                tapTapState.isRunning && goalReached ->
                                    "100 taps reached! Keep tapping until the ${tapTapState.secondsRemaining}s timer ends."
                                tapTapState.isRunning ->
                                    "Tap the circle as fast as you can before 30 seconds end!"
                                tapTapState.isFinished && tapTapState.passedGoal && tapTapState.lastMatchAdClaimed ->
                                    "Awesome speed! +$TAP_TAP_COINS coins claimed (${tapTapState.tapCount} taps)."
                                tapTapState.isFinished && tapTapState.passedGoal ->
                                    "Challenge complete! Watch the rewarded ad to unlock +$TAP_TAP_COINS coins."
                                tapTapState.isFinished ->
                                    "You scored ${tapTapState.tapCount} / $TAP_TAP_TARGET_TAPS taps. Reach 100 in 30s to unlock the rewarded ad!"
                                else ->
                                    "Tap at least $TAP_TAP_TARGET_TAPS times in $TAP_TAP_DURATION_SECONDS seconds. Rewarded ad starts automatically after 30s."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Auto-Ad Spinner
                AnimatedVisibility(visible = tapTapState.autoLaunchingAd) {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MidnightCardHigh),
                        border = BorderStroke(1.dp, CyanSecondary.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                color = CyanSecondary,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "Starting Rewarded Ad for +$TAP_TAP_COINS coins...",
                                style = MaterialTheme.typography.titleSmall,
                                color = CyanSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Skipped Post-Game Ad Claim Button
                AnimatedVisibility(
                    visible = tapTapState.isFinished &&
                        tapTapState.passedGoal &&
                        !tapTapState.autoLaunchingAd &&
                        !tapTapState.lastMatchAdClaimed &&
                        tapTapState.lastRecordedMatchId != null
                ) {
                    Button(
                        onClick = onClaimPendingAd,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AmberGold,
                            contentColor = Color(0xFF451A03)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("tap_tap_claim_pending_ad_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayCircle,
                            contentDescription = "Watch rewarded ad",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = "Watch Google AdMob Ad to Claim +$TAP_TAP_COINS Coins",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Start / Retry / Play Again Button
                if (!tapTapState.isRunning && !tapTapState.autoLaunchingAd) {
                    Button(
                        onClick = onStartRound,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldPrimary,
                            contentColor = EmeraldOnPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag(if (tapTapState.isFinished) "tap_tap_retry_button" else "tap_tap_start_button")
                    ) {
                        Icon(
                            imageVector = if (tapTapState.isFinished) Icons.Filled.Replay else Icons.Filled.PlayArrow,
                            contentDescription = "Start Tap Tap",
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = when {
                                tapTapState.isFinished && tapTapState.passedGoal -> "Play Tap Tap Again"
                                tapTapState.isFinished -> "Try Again (Need 100 Taps)"
                                else -> "Start 30s Tap Tap"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                AdMobTestBannerCard(
                    isAdBlockerActive = isAdBlockerActive,
                    bannerAdUnitId = bannerAdUnitId
                )
            }
        }
    }
}
