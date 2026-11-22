package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.model.MATH_QUIZ_COINS
import com.example.model.MATH_QUIZ_QUESTIONS_PER_LEVEL
import com.example.ui.MathQuizContinueReason
import com.example.ui.MathQuizUiState
import com.example.ui.components.AdMobTestBannerCard
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CoralError
import com.example.ui.theme.CyanSecondary
import com.example.ui.theme.EmeraldOnPrimary
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MidnightCard
import com.example.ui.theme.MidnightCardHigh
import com.example.ui.theme.MidnightOutline

@Composable
fun MathQuizScreen(
    quizState: MathQuizUiState,
    onStartLevel: () -> Unit,
    onSelectOption: (Int) -> Unit,
    onWatchContinueAd: () -> Unit = {},
    onClaimPendingAd: () -> Unit,
    onNavigateBack: () -> Unit,
    isAdBlockerActive: Boolean = false,
    bannerAdUnitId: String = com.example.ads.AdSdkInfo.TEST_BANNER_AD_UNIT_ID,
    modifier: Modifier = Modifier
) {
    val activeQuestion = quizState.questions.getOrNull(quizState.currentQuestionIndex)

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
                // Header
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
                                .testTag("math_quiz_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Column {
                            Text(
                                text = "Easy Math Quiz",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$MATH_QUIZ_QUESTIONS_PER_LEVEL Questions/Level • +, -, ×, ÷ • Auto AdMob Ad",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        color = EmeraldPrimary.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MonetizationOn,
                                contentDescription = "Quiz reward",
                                tint = AmberGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "+$MATH_QUIZ_COINS",
                                style = MaterialTheme.typography.labelLarge,
                                color = EmeraldPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Level & 5-Question Step Progress Card
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MidnightCard),
                    border = BorderStroke(1.dp, MidnightOutline),
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
                            Text(
                                text = "Level ${quizState.levelNumber} • Easy",
                                style = MaterialTheme.typography.titleMedium,
                                color = CyanSecondary,
                                fontWeight = FontWeight.Bold
                            )
                            val completedQuestions = when {
                                quizState.isLevelCompleted -> MATH_QUIZ_QUESTIONS_PER_LEVEL
                                quizState.isLevelActive -> quizState.currentQuestionIndex
                                else -> 0
                            }
                            Text(
                                text = "$completedQuestions / $MATH_QUIZ_QUESTIONS_PER_LEVEL Solved",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (quizState.isLevelCompleted) EmeraldPrimary else AmberGold,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.testTag("math_quiz_progress_text")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            repeat(MATH_QUIZ_QUESTIONS_PER_LEVEL) { idx ->
                                val isDone = quizState.isLevelCompleted ||
                                    (quizState.isLevelActive && idx < quizState.currentQuestionIndex) ||
                                    (quizState.isLevelActive && idx == quizState.currentQuestionIndex && quizState.correctOptionSelected != null)
                                val isCurrent = quizState.isLevelActive && idx == quizState.currentQuestionIndex && !isDone
                                Surface(
                                    color = when {
                                        isDone -> EmeraldPrimary
                                        isCurrent -> CyanSecondary.copy(alpha = 0.25f)
                                        else -> MidnightCardHigh
                                    },
                                    shape = RoundedCornerShape(50),
                                    border = if (isCurrent) BorderStroke(1.dp, CyanSecondary) else null,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(10.dp)
                                ) {}
                            }
                        }
                    }
                }

                // Active Question Card + 4 Answer Options
                if (quizState.isLevelActive && activeQuestion != null) {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MidnightCard),
                        border = BorderStroke(1.dp, CyanSecondary.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Surface(
                                color = CyanSecondary.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = "Question ${quizState.currentQuestionIndex + 1} of $MATH_QUIZ_QUESTIONS_PER_LEVEL • ${activeQuestion.operation.label}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = CyanSecondary,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                )
                            }

                            Text(
                                text = activeQuestion.promptText,
                                style = MaterialTheme.typography.displayMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.testTag("math_quiz_question_text")
                            )

                            Text(
                                text = when {
                                    quizState.correctOptionSelected != null -> "Correct! Next question..."
                                    quizState.isLoadingContinueAd && quizState.continueAdReason == MathQuizContinueReason.CHECKPOINT_THREE ->
                                        "3/5 solved! Starting rewarded ad to continue..."
                                    quizState.isContinueAdRequired && quizState.continueAdReason == MathQuizContinueReason.CHECKPOINT_THREE ->
                                        "3/5 solved! Watch a rewarded ad to unlock Question ${quizState.currentQuestionIndex + 1}."
                                    quizState.isLoadingContinueAd -> "Wrong answer! Starting rewarded ad to continue..."
                                    quizState.isContinueAdRequired -> "Wrong answer! Watch a rewarded ad to continue playing."
                                    quizState.wrongOptionSelected != null -> "Not quite — try another answer!"
                                    else -> "Choose the right answer below"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = when {
                                    quizState.correctOptionSelected != null -> EmeraldPrimary
                                    quizState.isContinueAdRequired && quizState.continueAdReason == MathQuizContinueReason.CHECKPOINT_THREE -> AmberGold
                                    quizState.isContinueAdRequired || quizState.wrongOptionSelected != null -> CoralError
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                textAlign = TextAlign.Center,
                                modifier = Modifier.testTag("math_quiz_feedback_text")
                            )
                        }
                    }

                    // 2x2 Options Grid
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        for (rowIdx in 0 until 2) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                for (colIdx in 0 until 2) {
                                    val optIdx = rowIdx * 2 + colIdx
                                    val optionValue = activeQuestion.options[optIdx]
                                    val isCorrectPicked = quizState.correctOptionSelected == optionValue
                                    val isWrongPicked = quizState.wrongOptionSelected == optionValue
                                    val containerColor = when {
                                        isCorrectPicked -> EmeraldPrimary
                                        isWrongPicked -> CoralError.copy(alpha = 0.22f)
                                        else -> MidnightCard
                                    }
                                    val borderColor = when {
                                        isCorrectPicked -> EmeraldPrimary
                                        isWrongPicked -> CoralError
                                        else -> MidnightOutline
                                    }
                                    val textColor = when {
                                        isCorrectPicked -> EmeraldOnPrimary
                                        isWrongPicked -> CoralError
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }

                                    Surface(
                                        onClick = { onSelectOption(optionValue) },
                                        enabled = quizState.correctOptionSelected == null &&
                                            !quizState.isContinueAdRequired &&
                                            !quizState.isLoadingContinueAd,
                                        shape = RoundedCornerShape(18.dp),
                                        color = containerColor,
                                        border = BorderStroke(1.5.dp, borderColor),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(74.dp)
                                            .testTag("math_quiz_option_$optIdx")
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "$optionValue",
                                                style = MaterialTheme.typography.headlineMedium,
                                                color = textColor,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    AnimatedVisibility(visible = quizState.isContinueAdRequired) {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MidnightCardHigh),
                            border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("math_quiz_continue_ad_card")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (quizState.isLoadingContinueAd) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            color = AmberGold,
                                            strokeWidth = 2.5.dp,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Text(
                                            text = if (quizState.continueAdReason == MathQuizContinueReason.CHECKPOINT_THREE) {
                                                "3/5 Questions Solved — Starting Google AdMob Rewarded Ad to continue..."
                                            } else {
                                                "Wrong Answer — Starting Google AdMob Rewarded Ad to continue..."
                                            },
                                            style = MaterialTheme.typography.titleSmall,
                                            color = AmberGold,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                } else {
                                    Text(
                                        text = if (quizState.continueAdReason == MathQuizContinueReason.CHECKPOINT_THREE) {
                                            "3 questions completed! Watch a Google AdMob rewarded ad to continue to Question ${quizState.currentQuestionIndex + 1}."
                                        } else {
                                            "Watch a Google AdMob rewarded ad to continue Question ${quizState.currentQuestionIndex + 1} without losing your progress."
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Button(
                                        onClick = onWatchContinueAd,
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = AmberGold,
                                            contentColor = Color(0xFF451A03)
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp)
                                            .testTag("math_quiz_watch_continue_ad_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.PlayCircle,
                                            contentDescription = "Watch rewarded ad to continue",
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.size(8.dp))
                                        Text(
                                            text = "Watch Rewarded Ad to Continue",
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    OutlinedButton(
                                        onClick = onStartLevel,
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(1.dp, MidnightOutline),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("math_quiz_restart_level_button")
                                    ) {
                                        Text(
                                            text = "Restart Level ${quizState.levelNumber}",
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Ready or Completed Hero Card
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MidnightCard),
                        border = BorderStroke(
                            1.dp,
                            if (quizState.isLevelCompleted) EmeraldPrimary.copy(alpha = 0.6f) else MidnightOutline
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                color = (if (quizState.isLevelCompleted) EmeraldPrimary else CyanSecondary).copy(alpha = 0.16f),
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier.size(64.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (quizState.isLevelCompleted) Icons.Filled.CheckCircle else Icons.Filled.Calculate,
                                        contentDescription = "Math Quiz",
                                        tint = if (quizState.isLevelCompleted) EmeraldPrimary else CyanSecondary,
                                        modifier = Modifier.size(34.dp)
                                    )
                                }
                            }
                            Text(
                                text = if (quizState.isLevelCompleted) {
                                    "Level ${quizState.levelNumber} Complete!"
                                } else {
                                    "Ready for Level ${quizState.levelNumber}"
                                },
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = when {
                                    quizState.autoLaunchingAd ->
                                        "5/5 solved! Starting rewarded ad automatically for +$MATH_QUIZ_COINS coins..."
                                    quizState.isLevelCompleted && quizState.lastMatchAdClaimed ->
                                        "You solved all 5 questions and claimed +$MATH_QUIZ_COINS coins!"
                                    quizState.isLevelCompleted ->
                                        "Watch the rewarded ad to claim +$MATH_QUIZ_COINS coins for Level ${quizState.levelNumber}."
                                    else ->
                                        "Solve $MATH_QUIZ_QUESTIONS_PER_LEVEL easy math questions (+, -, ×, ÷) to unlock +$MATH_QUIZ_COINS coins. Completing 3 questions or answering wrong runs a rewarded ad to continue."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    AnimatedVisibility(visible = quizState.autoLaunchingAd) {
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
                                    text = "Starting Rewarded Ad for +$MATH_QUIZ_COINS coins...",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = CyanSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = quizState.isLevelCompleted &&
                            !quizState.autoLaunchingAd &&
                            !quizState.lastMatchAdClaimed &&
                            quizState.lastRecordedMatchId != null
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
                                .testTag("math_quiz_claim_pending_ad_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayCircle,
                                contentDescription = "Watch rewarded ad",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = "Watch Google AdMob Ad to Claim +$MATH_QUIZ_COINS Coins",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (!quizState.autoLaunchingAd) {
                        Button(
                            onClick = onStartLevel,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldPrimary,
                                contentColor = EmeraldOnPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag(
                                    if (quizState.isLevelCompleted) "math_quiz_next_level_button"
                                    else "math_quiz_start_button"
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = "Start Math Quiz",
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = if (quizState.isLevelCompleted) {
                                    "Play Level ${quizState.levelNumber + 1} (+$MATH_QUIZ_COINS Coins)"
                                } else {
                                    "Start Level ${quizState.levelNumber} (5 Questions)"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
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
