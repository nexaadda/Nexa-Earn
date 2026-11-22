package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.model.COINS_PER_GAME
import com.example.model.WATCH_AD_BUNDLE_COINS
import com.example.model.WATCH_AD_BUNDLE_COUNT
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CyanSecondary
import com.example.ui.theme.EmeraldOnPrimary
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MidnightBg
import com.example.ui.theme.MidnightCard
import com.example.ui.theme.MidnightCardHigh
import com.example.ui.theme.MidnightOutline

private data class TutorialStep(
    val badge: String,
    val title: String,
    val subtitle: String,
    val accent: Color
)

private val tutorialSteps = listOf(
    TutorialStep(
        badge = "STEP 1 OF 3 • HOW TO PLAY",
        title = "Unlimited Mini Games",
        subtitle = "Beat the Medium Bot in Tic Tac Toe (unlimited levels/day), hit 100 taps in 30s in Tap Tap, or solve 5 easy Math Quiz questions per level!",
        accent = EmeraldPrimary
    ),
    TutorialStep(
        badge = "STEP 2 OF 3 • GOOGLE ADMOB",
        title = "Earn Coins via Google AdMob",
        subtitle = "Each completed game automatically launches a Google AdMob rewarded ad (+$COINS_PER_GAME to +10 coins). Coins are only added when an AdMob ad is available and completed.",
        accent = AmberGold
    ),
    TutorialStep(
        badge = "STEP 3 OF 3 • STREAKS & GOALS",
        title = "Daily Streaks & 5% Extra Earn",
        subtitle = "Claim daily streaks (up to +60 coins), watch $WATCH_AD_BUNDLE_COUNT Google AdMob ads for +$WATCH_AD_BUNDLE_COINS coins (unlimited!), hit 100 coins for +5% Extra Earn, and keep ad blockers off.",
        accent = CyanSecondary
    )
)

@Composable
fun OnboardingTutorialOverlay(
    onDismiss: () -> Unit,
    onStartPlaying: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentStep by rememberSaveable { mutableIntStateOf(0) }
    val step = tutorialSteps[currentStep]
    val totalSteps = tutorialSteps.size

    BackHandler {
        if (currentStep > 0) {
            currentStep -= 1
        } else {
            onDismiss()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.78f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
            .safeDrawingPadding()
            .padding(horizontal = 18.dp, vertical = 16.dp)
            .testTag("onboarding_overlay"),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MidnightCard),
            border = BorderStroke(1.dp, step.accent.copy(alpha = 0.55f)),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top Bar: Badge + Skip / Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = step.accent.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, step.accent.copy(alpha = 0.45f))
                    ) {
                        Text(
                            text = step.badge,
                            style = MaterialTheme.typography.labelSmall,
                            color = step.accent,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (currentStep < totalSteps - 1) {
                            TextButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("onboarding_skip_button")
                            ) {
                                Text(
                                    text = "Skip",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("onboarding_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Close tutorial",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Animated Illustration + Copy
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        val forward = targetState >= initialState
                        (slideInHorizontally { if (forward) it / 3 else -it / 3 } + fadeIn())
                            .togetherWith(slideOutHorizontally { if (forward) -it / 3 else it / 3 } + fadeOut())
                    },
                    label = "onboarding_step_content"
                ) { pageIndex ->
                    val page = tutorialSteps[pageIndex]
                    Column(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        when (pageIndex) {
                            0 -> TicTacToeTutorialIllustration()
                            1 -> CoinEarningFlowIllustration()
                            else -> StreakAndShieldIllustration()
                        }

                        Text(
                            text = page.title,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = page.subtitle,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Step Indicator Dots
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(totalSteps) { index ->
                        val selected = index == currentStep
                        val dotWidth by animateDpAsState(
                            targetValue = if (selected) 26.dp else 8.dp,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label = "indicator_width"
                        )
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .height(8.dp)
                                .width(dotWidth)
                                .clip(CircleShape)
                                .background(
                                    if (selected) step.accent else MidnightOutline
                                )
                        )
                    }
                }

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStep > 0) {
                        OutlinedButton(
                            onClick = { currentStep -= 1 },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, MidnightOutline),
                            modifier = Modifier
                                .height(50.dp)
                                .testTag("onboarding_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Previous step",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                            Text("Back")
                        }
                    }

                    if (currentStep < totalSteps - 1) {
                        Button(
                            onClick = { currentStep += 1 },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = step.accent,
                                contentColor = EmeraldOnPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("onboarding_next_button")
                        ) {
                            Text(
                                text = "Next",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Next step",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        Button(
                            onClick = onStartPlaying,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldPrimary,
                                contentColor = EmeraldOnPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("onboarding_play_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = "Start playing",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(
                                text = "Play Tic Tac Toe",
                                style = MaterialTheme.typography.titleMedium,
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
private fun TicTacToeTutorialIllustration() {
    val sampleBoard = listOf(
        "X", "O", "",
        "", "X", "O",
        "", "", "X"
    )
    val winningCells = setOf(0, 4, 8)

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MidnightBg,
        border = BorderStroke(1.dp, MidnightOutline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Mini 3x3 Preview
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.size(116.dp)
            ) {
                for (r in 0 until 3) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (c in 0 until 3) {
                            val idx = r * 3 + c
                            val mark = sampleBoard[idx]
                            val isWin = idx in winningCells
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isWin) EmeraldPrimary.copy(alpha = 0.22f) else MidnightCardHigh
                                    )
                                    .border(
                                        1.dp,
                                        if (isWin) EmeraldPrimary else MidnightOutline,
                                        RoundedCornerShape(8.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = mark,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (mark == "X") EmeraldPrimary else CyanSecondary
                                )
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TutorialChip(
                    icon = Icons.Filled.SportsEsports,
                    text = "You play as X",
                    tint = EmeraldPrimary
                )
                TutorialChip(
                    icon = Icons.Filled.SmartToy,
                    text = "Bot plays O (Medium)",
                    tint = CyanSecondary
                )
                TutorialChip(
                    icon = Icons.Filled.MonetizationOn,
                    text = "Unlimited levels / day",
                    tint = AmberGold
                )
            }
        }
    }
}

@Composable
private fun CoinEarningFlowIllustration() {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MidnightBg,
        border = BorderStroke(1.dp, MidnightOutline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            MidnightBg,
                            AmberGold.copy(alpha = 0.08f)
                        )
                    )
                )
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FlowStepBadge(
                icon = Icons.Filled.SportsEsports,
                title = "1. Play",
                subtitle = "Finish level",
                tint = CyanSecondary
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "then",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            FlowStepBadge(
                icon = Icons.Filled.PlayCircle,
                title = "2. Watch Ad",
                subtitle = "Auto-starts",
                tint = AmberGold
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "earns",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            FlowStepBadge(
                icon = Icons.Filled.MonetizationOn,
                title = "3. +$COINS_PER_GAME Coins",
                subtitle = "Credited live",
                tint = EmeraldPrimary
            )
        }
    }
}

@Composable
private fun StreakAndShieldIllustration() {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MidnightBg,
        border = BorderStroke(1.dp, MidnightOutline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TutorialFeatureRow(
                icon = Icons.Filled.LocalFireDepartment,
                title = "Daily Streak Bonus",
                detail = "Day 1 (+10) up to Day 7 (+60) coins via 1 rewarded ad",
                tint = AmberGold
            )
            TutorialFeatureRow(
                icon = Icons.Filled.PlayCircle,
                title = "Watch $WATCH_AD_BUNDLE_COUNT Ads • +$WATCH_AD_BUNDLE_COINS Coins",
                detail = "Watch $WATCH_AD_BUNDLE_COUNT Google AdMob ads unlimited times for +$WATCH_AD_BUNDLE_COINS coins (0 if ads unavailable)",
                tint = CyanSecondary
            )
            TutorialFeatureRow(
                icon = Icons.Filled.Shield,
                title = "Keep Ad-Shield Green",
                detail = "Turn off Private DNS / ad-blocking VPNs so rewards verify",
                tint = EmeraldPrimary
            )
        }
    }
}

@Composable
private fun TutorialChip(
    icon: ImageVector,
    text: String,
    tint: Color
) {
    Surface(
        color = MidnightCardHigh,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, MidnightOutline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = tint,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun FlowStepBadge(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Surface(
            color = tint.copy(alpha = 0.16f),
            shape = CircleShape,
            border = BorderStroke(1.dp, tint.copy(alpha = 0.5f)),
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = tint,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TutorialFeatureRow(
    icon: ImageVector,
    title: String,
    detail: String,
    tint: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            color = tint.copy(alpha = 0.16f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = tint,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
