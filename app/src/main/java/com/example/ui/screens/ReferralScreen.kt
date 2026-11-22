package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.REFERRAL_BONUS_COINS
import com.example.model.REFERRAL_TARGET_GAMES
import com.example.model.ReferralRecord
import com.example.model.ReferralStatus
import com.example.model.UiState
import com.example.model.UserProfile
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CoralError
import com.example.ui.theme.CyanSecondary
import com.example.ui.theme.EmeraldOnPrimary
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MidnightBg
import com.example.ui.theme.MidnightCard
import com.example.ui.theme.MidnightCardHigh
import com.example.ui.theme.MidnightOutline
import java.util.Locale

@Composable
fun ReferralScreen(
    profile: UserProfile,
    sentReferralsState: UiState<List<ReferralRecord>>,
    myReferralLinkState: UiState<ReferralRecord?>,
    isApplyingReferral: Boolean,
    claimingReferralIds: Set<String>,
    onApplyReferralCode: (String, () -> Unit) -> Unit,
    onClaimReferralBonus: (inviteeUid: String, friendName: String) -> Unit,
    onShowMessage: (String) -> Unit,
    onNavigateBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (onNavigateBack != null) {
        BackHandler(onBack = onNavigateBack)
    }

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val myCode = profile.resolvedReferralCode()
    var codeInput by remember { mutableStateOf("") }

    val sentList = (sentReferralsState as? UiState.Success)?.data.orEmpty()
    val pendingFriendsCount = sentList.count { it.parsedStatus() == ReferralStatus.IN_PROGRESS }

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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (onNavigateBack != null) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("referral_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Referral",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Earn +$REFERRAL_BONUS_COINS coins when each friend finishes $REFERRAL_TARGET_GAMES games",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Hero Bonus & How It Works Card
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MidnightCard),
                    border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("referral_hero_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        EmeraldPrimary.copy(alpha = 0.14f),
                                        MidnightCard
                                    )
                                )
                            )
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    color = AmberGold.copy(alpha = 0.18f),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.size(46.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.CardGiftcard,
                                            contentDescription = "Referral reward",
                                            tint = AmberGold,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = "+$REFERRAL_BONUS_COINS Coins Per Friend",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Milestone: First $REFERRAL_TARGET_GAMES Tic Tac Toe games",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = EmeraldPrimary
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ReferralStepChip(
                                step = "1",
                                label = "Share your code",
                                modifier = Modifier.weight(1f)
                            )
                            ReferralStepChip(
                                step = "2",
                                label = "Friend plays $REFERRAL_TARGET_GAMES games",
                                modifier = Modifier.weight(1f)
                            )
                            ReferralStepChip(
                                step = "3",
                                label = "Get +$REFERRAL_BONUS_COINS coins",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Your Referral Code & Share Card
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MidnightCard),
                    border = BorderStroke(1.dp, CyanSecondary.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("referral_code_card")
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "YOUR REFERRAL CODE",
                            style = MaterialTheme.typography.labelMedium,
                            color = CyanSecondary
                        )

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MidnightBg.copy(alpha = 0.85f),
                            border = BorderStroke(1.dp, MidnightOutline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = myCode,
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        letterSpacing = 2.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.testTag("referral_code_value")
                                )
                                Surface(
                                    color = EmeraldPrimary.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        text = "ACTIVE",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = EmeraldPrimary,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(myCode))
                                    onShowMessage("Referral code $myCode copied!")
                                },
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, CyanSecondary.copy(alpha = 0.6f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("referral_copy_code_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ContentCopy,
                                    contentDescription = "Copy referral code",
                                    tint = CyanSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.size(6.dp))
                                Text("Copy Code", color = CyanSecondary)
                            }

                            Button(
                                onClick = {
                                    val shareBody = buildString {
                                        append("Play Tic Tac Toe & earn coins on NexaEarn! ")
                                        append("Use my referral code $myCode before finishing your first $REFERRAL_TARGET_GAMES games. ")
                                        append("Invite link: https://nexaearn.app/invite/$myCode")
                                    }
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, "Join NexaEarn with code $myCode")
                                        putExtra(Intent.EXTRA_TEXT, shareBody)
                                    }
                                    context.startActivity(
                                        Intent.createChooser(sendIntent, "Share NexaEarn Referral")
                                    )
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EmeraldPrimary,
                                    contentColor = EmeraldOnPrimary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("referral_share_invite_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Share,
                                    contentDescription = "Share referral invite",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.size(6.dp))
                                Text("Share Invite", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Referral Stats Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ReferralStatCard(
                        label = "Referral Coins",
                        value = "+${profile.referralBonusEarned}",
                        subtitle = "Bonus earned",
                        accent = AmberGold,
                        modifier = Modifier.weight(1f)
                    )
                    ReferralStatCard(
                        label = "Friends Qualified",
                        value = "${profile.referralsCompletedCount}",
                        subtitle = if (pendingFriendsCount > 0) "$pendingFriendsCount in progress" else "Completed 5 games",
                        accent = EmeraldPrimary,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Apply Friend's Referral Code Card
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MidnightCard),
                    border = BorderStroke(1.dp, MidnightOutline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("referral_apply_card")
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.GroupAdd,
                                contentDescription = "Friend referral code",
                                tint = AmberGold,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "Have a Friend's Code?",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        val linkedRecord = (myReferralLinkState as? UiState.Success)?.data
                        val linkedCode = linkedRecord?.referrerCode ?: profile.referredByCode
                        if (!linkedCode.isNullOrBlank()) {
                            val gamesCount = linkedRecord?.clampedProgress()
                                ?: profile.totalGamesPlayed.toInt().coerceAtMost(REFERRAL_TARGET_GAMES)
                            val qualified = gamesCount >= REFERRAL_TARGET_GAMES ||
                                (linkedRecord != null && linkedRecord.parsedStatus() != ReferralStatus.IN_PROGRESS)

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (qualified) EmeraldPrimary.copy(alpha = 0.14f) else MidnightCardHigh,
                                border = BorderStroke(
                                    1.dp,
                                    if (qualified) EmeraldPrimary.copy(alpha = 0.5f) else MidnightOutline
                                ),
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
                                        Text(
                                            text = "Invited by ${linkedRecord?.referrerName ?: "Friend"} ($linkedCode)",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = "$gamesCount / $REFERRAL_TARGET_GAMES",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (qualified) EmeraldPrimary else AmberGold,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    LinearProgressIndicator(
                                        progress = { gamesCount.toFloat() / REFERRAL_TARGET_GAMES.toFloat() },
                                        color = if (qualified) EmeraldPrimary else AmberGold,
                                        trackColor = MidnightBg,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(50))
                                    )

                                    Text(
                                        text = if (qualified) {
                                            "Milestone reached! Your friend unlocked their +$REFERRAL_BONUS_COINS coin bonus."
                                        } else {
                                            val remaining = (REFERRAL_TARGET_GAMES - gamesCount).coerceAtLeast(1)
                                            "Play $remaining more Tic Tac Toe ${if (remaining == 1) "game" else "games"} to unlock +$REFERRAL_BONUS_COINS bonus coins for your friend."
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else if (!profile.canApplyReferralCode()) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MidnightCardHigh,
                                border = BorderStroke(1.dp, MidnightOutline),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Referral codes can only be entered before completing your first $REFERRAL_TARGET_GAMES games (${profile.totalGamesPlayed} played so far). Share your code above to invite new friends!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        } else {
                            Text(
                                text = "Enter a friend's referral code during your first $REFERRAL_TARGET_GAMES games (${profile.totalGamesPlayed}/$REFERRAL_TARGET_GAMES played). When you finish $REFERRAL_TARGET_GAMES games, they earn +$REFERRAL_BONUS_COINS coins.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = codeInput,
                                    onValueChange = { raw ->
                                        codeInput = raw.uppercase(Locale.US)
                                            .filter { it.isLetterOrDigit() }
                                            .take(16)
                                    },
                                    placeholder = { Text("e.g. NX4A9F12") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        capitalization = KeyboardCapitalization.Characters,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            if (codeInput.length >= 6 && !isApplyingReferral) {
                                                onApplyReferralCode(codeInput) { codeInput = "" }
                                            }
                                        }
                                    ),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = EmeraldPrimary,
                                        unfocusedBorderColor = MidnightOutline
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("referral_code_input")
                                )

                                Button(
                                    onClick = {
                                        onApplyReferralCode(codeInput) {
                                            codeInput = ""
                                        }
                                    },
                                    enabled = codeInput.length >= 6 && !isApplyingReferral,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AmberGold,
                                        contentColor = Color(0xFF451A03)
                                    ),
                                    modifier = Modifier
                                        .height(54.dp)
                                        .testTag("referral_apply_button")
                                ) {
                                    if (isApplyingReferral) {
                                        CircularProgressIndicator(
                                            color = Color(0xFF451A03),
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    } else {
                                        Text("Apply", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // Invited Friends Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Invited Friends",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${sentList.size} Total",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        when (sentReferralsState) {
            is UiState.Loading -> item {
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
                            color = EmeraldPrimary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Loading invited friends...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            is UiState.Error -> item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 720.dp)
                ) {
                    Text(
                        text = sentReferralsState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = CoralError,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            is UiState.Success -> {
                val records = sentReferralsState.data
                if (records.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MidnightCard),
                            border = BorderStroke(1.dp, MidnightOutline),
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 720.dp)
                                .testTag("referral_friends_empty_card")
                        ) {
                            Row(
                                modifier = Modifier.padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.SportsEsports,
                                    contentDescription = "No invited friends yet",
                                    tint = CyanSecondary,
                                    modifier = Modifier.size(26.dp)
                                )
                                Text(
                                    text = "No friends invited yet. Share your code $myCode — when they finish their first $REFERRAL_TARGET_GAMES Tic Tac Toe games, you earn +$REFERRAL_BONUS_COINS bonus coins!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(records, key = { it.inviteeUid }) { record ->
                        InvitedFriendProgressCard(
                            record = record,
                            isClaiming = claimingReferralIds.contains(record.inviteeUid),
                            onClaimClick = {
                                onClaimReferralBonus(record.inviteeUid, record.inviteeName)
                            },
                            modifier = Modifier.widthIn(max = 720.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReferralStepChip(
    step: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MidnightBg.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, MidnightOutline.copy(alpha = 0.8f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = EmeraldPrimary.copy(alpha = 0.2f),
                modifier = Modifier.size(22.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = step,
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ReferralStatCard(
    label: String,
    value: String,
    subtitle: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MidnightCard,
        border = BorderStroke(1.dp, MidnightOutline),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = accent,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InvitedFriendProgressCard(
    record: ReferralRecord,
    isClaiming: Boolean,
    onClaimClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val status = record.parsedStatus()
    val progress = record.clampedProgress()
    val target = record.targetGames.toInt().coerceAtLeast(1)
    val statusColor = when (status) {
        ReferralStatus.CLAIMED -> EmeraldPrimary
        ReferralStatus.QUALIFIED -> AmberGold
        ReferralStatus.IN_PROGRESS -> CyanSecondary
    }
    val friendInitial = record.inviteeName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "F"

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MidnightCard),
        border = BorderStroke(
            1.dp,
            if (status == ReferralStatus.QUALIFIED) AmberGold.copy(alpha = 0.6f) else MidnightOutline
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("referral_friend_card_${record.inviteeUid}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = statusColor.copy(alpha = 0.18f),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = friendInitial,
                            style = MaterialTheme.typography.titleMedium,
                            color = statusColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = record.inviteeName.ifBlank { "Invited Friend" },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$progress / $target games completed",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = statusColor.copy(alpha = 0.16f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = when (status) {
                                ReferralStatus.CLAIMED -> Icons.Filled.CheckCircle
                                ReferralStatus.QUALIFIED -> Icons.Filled.MonetizationOn
                                ReferralStatus.IN_PROGRESS -> Icons.Filled.HourglassTop
                            },
                            contentDescription = status.label,
                            tint = statusColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (status == ReferralStatus.CLAIMED) {
                                "+${record.bonusCoins} Claimed"
                            } else {
                                status.label
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            LinearProgressIndicator(
                progress = { progress.toFloat() / target.toFloat() },
                color = statusColor,
                trackColor = MidnightCardHigh,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50))
            )

            if (record.isReadyToClaim()) {
                Button(
                    onClick = onClaimClick,
                    enabled = !isClaiming,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AmberGold,
                        contentColor = Color(0xFF451A03)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("referral_claim_${record.inviteeUid}")
                ) {
                    if (isClaiming) {
                        CircularProgressIndicator(
                            color = Color(0xFF451A03),
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.MonetizationOn,
                            contentDescription = "Claim bonus coins",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(
                            text = "Claim +${record.bonusCoins} Bonus Coins",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
