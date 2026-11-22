package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VerifiedUser
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ads.AdBlockerCheckResult
import com.example.model.AdminNotification
import com.example.model.AppConfig
import com.example.model.COINS_PER_GAME
import com.example.model.MATH_QUIZ_COINS
import com.example.model.MATH_QUIZ_QUESTIONS_PER_LEVEL
import com.example.model.TAP_TAP_COINS
import com.example.model.TAP_TAP_DURATION_SECONDS
import com.example.model.TAP_TAP_TARGET_TAPS
import com.example.model.UserProfile
import com.example.model.WATCH_AD_BUNDLE_COINS
import com.example.model.WATCH_AD_BUNDLE_COUNT
import com.example.model.streakRewardForDay
import com.example.model.todayDateString
import com.example.ui.components.AdMobTestBannerCard
import com.example.ui.components.GoalAndExtraEarnCard
import com.example.ui.components.LiveAnnouncementBannerCard
import com.example.ui.components.LiveCustomAlertCard
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CoralError
import com.example.ui.theme.CyanSecondary
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MidnightBg
import com.example.ui.theme.MidnightCard
import com.example.ui.theme.MidnightCardHigh
import com.example.ui.theme.MidnightOutline

@Composable
fun HomeScreen(
    profile: UserProfile,
    adBlockState: AdBlockerCheckResult,
    appConfig: AppConfig = AppConfig(),
    latestLiveAlert: AdminNotification? = null,
    onDismissLiveAlert: () -> Unit = {},
    onPlayTicTacToe: () -> Unit,
    onClaimStreakAd: () -> Unit,
    onWatchBonusAd: () -> Unit,
    onOpenAdsLab: () -> Unit,
    onOpenProfile: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    isClaimingExtraEarn: Boolean = false,
    onClaimExtraEarn: () -> Unit = {},
    onPlayTapTap: () -> Unit = {},
    onPlayMathQuiz: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val today = todayDateString()
    val playedToday = profile.effectiveDailyGamesPlayed(today).toInt()
    val nextLevel = profile.ticTacToeNextLevel(today)
    val streakClaimedToday = profile.hasClaimedStreakToday(today)
    val nextStreakDay = profile.nextStreakDay(today)
    val nextStreakCoins = streakRewardForDay(nextStreakDay)
    val watchAdProgress = profile.clampedWatchAdProgress()

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
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_launcher_emblem),
                            contentDescription = "NexaEarn app logo",
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Column {
                            Text(
                                text = "Welcome back,",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = profile.displayName,
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            onClick = onOpenAdsLab,
                            shape = RoundedCornerShape(50),
                            color = if (adBlockState.isBlocked) CoralError.copy(alpha = 0.16f) else EmeraldPrimary.copy(alpha = 0.16f),
                            border = BorderStroke(
                                1.dp,
                                if (adBlockState.isBlocked) CoralError.copy(alpha = 0.5f) else EmeraldPrimary.copy(alpha = 0.45f)
                            ),
                            modifier = Modifier.testTag("ad_shield_status_chip")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Shield,
                                    contentDescription = "Ad-Shield status",
                                    tint = if (adBlockState.isBlocked) CoralError else EmeraldPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (adBlockState.isBlocked) "Blocked" else "Ad-Shield OK",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (adBlockState.isBlocked) CoralError else EmeraldPrimary
                                )
                            }
                        }

                        Surface(
                            onClick = onOpenSettings,
                            shape = CircleShape,
                            color = MidnightCardHigh,
                            border = BorderStroke(1.dp, MidnightOutline),
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("home_settings_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Settings,
                                    contentDescription = "Open Settings & Notifications",
                                    tint = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Surface(
                            onClick = onOpenProfile,
                            shape = CircleShape,
                            color = MidnightCardHigh,
                            border = BorderStroke(1.dp, CyanSecondary.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("home_profile_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = profile.initials(),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = CyanSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Live incoming custom alert card (when Admin sends a push notification)
                if (latestLiveAlert != null) {
                    LiveCustomAlertCard(
                        title = latestLiveAlert.title,
                        body = latestLiveAlert.body,
                        onDismiss = onDismissLiveAlert
                    )
                }

                // Live Announcement Banner from AppConfig
                LiveAnnouncementBannerCard(
                    title = appConfig.announcementTitle,
                    body = appConfig.announcementBody
                )

                // Coin Balance Hero Card
                Card(
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, MidnightOutline),
                    colors = CardDefaults.cardColors(containerColor = MidnightCard),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("hero_balance_card")
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Image(
                            painter = painterResource(id = R.drawable.img_hero_banner),
                            contentDescription = "NexaEarn wallet backdrop",
                            modifier = Modifier
                                .matchParentSize()
                                .clip(RoundedCornerShape(24.dp)),
                            contentScale = ContentScale.Crop,
                            alpha = 0.28f
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            MidnightBg.copy(alpha = 0.92f),
                                            MidnightCard.copy(alpha = 0.72f)
                                        )
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "COIN WALLET",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = EmeraldPrimary
                                )
                                Surface(
                                    color = AmberGold.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.LocalFireDepartment,
                                            contentDescription = "Current streak",
                                            tint = AmberGold,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "${profile.currentStreak}d Streak",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = AmberGold
                                        )
                                    }
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.MonetizationOn,
                                    contentDescription = "Coins balance",
                                    tint = AmberGold,
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = "${profile.coinsBalance}",
                                    style = MaterialTheme.typography.displaySmall,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "COINS",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                StatMiniPill(
                                    label = "Total Earned",
                                    value = "${profile.totalCoinsEarned}",
                                    modifier = Modifier.weight(1f)
                                )
                                StatMiniPill(
                                    label = "Ads Watched",
                                    value = "${profile.totalAdsWatched}",
                                    modifier = Modifier.weight(1f)
                                )
                                StatMiniPill(
                                    label = "Wins",
                                    value = "${profile.totalGamesWon}",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // 24h 100-Coin Goal & Extra Earn Pot Card
                GoalAndExtraEarnCard(
                    profile = profile,
                    isClaimingExtraEarn = isClaimingExtraEarn,
                    onClaimExtraEarn = onClaimExtraEarn
                )

                // Daily Streak Rewarded Ad Card
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MidnightCard),
                    border = BorderStroke(
                        1.dp,
                        if (streakClaimedToday) MidnightOutline else AmberGold.copy(alpha = 0.55f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("daily_streak_card")
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Daily Streak Rewards",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (streakClaimedToday) {
                                        "Today's Day ${profile.currentStreak} bonus claimed! Come back tomorrow."
                                    } else {
                                        "Watch 1 rewarded ad to claim Day $nextStreakDay (+$nextStreakCoins coins)"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items((1..7).toList()) { day ->
                                val cycleIndex = ((nextStreakDay - 1) % 7) + 1
                                val completed = if (streakClaimedToday) {
                                    day <= (((profile.currentStreak.toInt() - 1).coerceAtLeast(0) % 7) + 1)
                                } else {
                                    day < cycleIndex
                                }
                                val isTodayTarget = !streakClaimedToday && day == cycleIndex
                                val dayCoins = streakRewardForDay(day)

                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = when {
                                        isTodayTarget -> AmberGold.copy(alpha = 0.2f)
                                        completed -> EmeraldPrimary.copy(alpha = 0.16f)
                                        else -> MidnightCardHigh
                                    },
                                    border = BorderStroke(
                                        1.dp,
                                        when {
                                            isTodayTarget -> AmberGold
                                            completed -> EmeraldPrimary.copy(alpha = 0.5f)
                                            else -> MidnightOutline
                                        }
                                    ),
                                    modifier = Modifier.width(66.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "D$day",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isTodayTarget) AmberGold else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Icon(
                                            imageVector = if (completed) Icons.Filled.CheckCircle else Icons.Filled.MonetizationOn,
                                            contentDescription = "Day $day reward",
                                            tint = if (completed) EmeraldPrimary else AmberGold,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "+$dayCoins",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = onClaimStreakAd,
                            enabled = !streakClaimedToday,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AmberGold,
                                contentColor = Color(0xFF451A03)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("claim_daily_streak_button")
                        ) {
                            Icon(
                                imageVector = if (streakClaimedToday) Icons.Filled.VerifiedUser else Icons.Filled.PlayCircle,
                                contentDescription = "Claim streak",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = if (streakClaimedToday) {
                                    "Streak Claimed Today"
                                } else {
                                    "Watch Ad & Claim +$nextStreakCoins Coins"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Tic Tac Toe 10-Level Daily Arena Card
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MidnightCard),
                    border = BorderStroke(1.dp, MidnightOutline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tic_tac_toe_home_card")
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    color = CyanSecondary.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.size(46.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.SportsEsports,
                                            contentDescription = "Tic Tac Toe Arena",
                                            tint = CyanSecondary,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = "Tic Tac Toe • Medium Bot",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "+$COINS_PER_GAME coins per match • Auto-ad after match",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Unlimited Daily Levels Strip
                        Surface(
                            color = MidnightCardHigh,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, MidnightOutline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Unlimited Daily Levels",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = EmeraldPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "$playedToday Played Today",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = CyanSecondary
                                )
                            }
                        }

                        Button(
                            onClick = onPlayTicTacToe,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldPrimary,
                                contentColor = Color(0xFF022C22)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("play_tic_tac_toe_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = "Play Tic Tac Toe",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(
                                text = "Play Level $nextLevel",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Watch 3 Google AdMob Ads -> +15 Coins (Unlimited) Card
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MidnightCard),
                    border = BorderStroke(1.dp, CyanSecondary.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_watch_ads_bundle_card")
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
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    color = CyanSecondary.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.size(46.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.PlayCircle,
                                            contentDescription = "Watch 3 Google AdMob Ads",
                                            tint = CyanSecondary,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = "Watch $WATCH_AD_BUNDLE_COUNT Ads • +$WATCH_AD_BUNDLE_COINS Coins",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Unlimited times! Watch $WATCH_AD_BUNDLE_COUNT Google AdMob ads to earn +$WATCH_AD_BUNDLE_COINS coins",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Surface(
                                color = CyanSecondary.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = "+$WATCH_AD_BUNDLE_COINS",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = CyanSecondary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }

                        // 3-Step Live Progress Strip
                        Surface(
                            color = MidnightCardHigh,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, MidnightOutline),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("home_watch_ad_progress_strip")
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Cycle Progress: $watchAdProgress/$WATCH_AD_BUNDLE_COUNT ads",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = CyanSecondary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    val adsLeft = WATCH_AD_BUNDLE_COUNT.toInt() - watchAdProgress
                                    Text(
                                        text = "$adsLeft ${if (adsLeft == 1) "ad" else "ads"} left for +$WATCH_AD_BUNDLE_COINS",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    for (stepIdx in 1..WATCH_AD_BUNDLE_COUNT.toInt()) {
                                        val completed = stepIdx <= watchAdProgress
                                        Surface(
                                            color = if (completed) EmeraldPrimary.copy(alpha = 0.22f) else MidnightCard,
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(
                                                1.dp,
                                                if (completed) EmeraldPrimary else MidnightOutline
                                            ),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = if (completed) Icons.Filled.CheckCircle else Icons.Filled.PlayCircle,
                                                    contentDescription = "Ad $stepIdx of $WATCH_AD_BUNDLE_COUNT",
                                                    tint = if (completed) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.size(4.dp))
                                                Text(
                                                    text = "Ad $stepIdx",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (completed) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontWeight = if (completed) FontWeight.Bold else FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }
                                Text(
                                    text = "If Google AdMob ads aren't available, 0 coins & no progress are added.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Button(
                            onClick = onWatchBonusAd,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyanSecondary,
                                contentColor = Color(0xFF082F49)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("home_watch_ad_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayCircle,
                                contentDescription = "Watch Google AdMob Ad",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = "Watch Ad ($watchAdProgress/$WATCH_AD_BUNDLE_COUNT) • +$WATCH_AD_BUNDLE_COINS Coins",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Tap Tap 30s Mini Game Card
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MidnightCard),
                    border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_tap_tap_card")
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
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    color = AmberGold.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.size(46.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.TouchApp,
                                            contentDescription = "Tap Tap Mini Game",
                                            tint = AmberGold,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = "Tap Tap • 30s Challenge",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Reach $TAP_TAP_TARGET_TAPS taps in ${TAP_TAP_DURATION_SECONDS}s • Auto-ad (+$TAP_TAP_COINS coins)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Surface(
                                color = AmberGold.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = "+$TAP_TAP_COINS",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = AmberGold,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }

                        Button(
                            onClick = onPlayTapTap,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AmberGold,
                                contentColor = Color(0xFF451A03)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("home_play_tap_tap_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.TouchApp,
                                contentDescription = "Play Tap Tap",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(
                                text = "Play Tap Tap (100 Taps / 30s)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Easy Math Quiz Mini Game Card
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MidnightCard),
                    border = BorderStroke(1.dp, CyanSecondary.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_math_quiz_card")
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
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    color = CyanSecondary.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.size(46.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.Calculate,
                                            contentDescription = "Easy Math Quiz",
                                            tint = CyanSecondary,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = "Easy Math Quiz",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "$MATH_QUIZ_QUESTIONS_PER_LEVEL questions/level (+, -, ×, ÷) • Auto-ad",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Surface(
                                color = EmeraldPrimary.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = "+$MATH_QUIZ_COINS",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = EmeraldPrimary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }

                        Button(
                            onClick = onPlayMathQuiz,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyanSecondary,
                                contentColor = Color(0xFF042F2E)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("home_play_math_quiz_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Calculate,
                                contentDescription = "Play Math Quiz",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(
                                text = "Play Math Quiz (+$MATH_QUIZ_COINS Coins / Level)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Google AdMob Banner Ad
                AdMobTestBannerCard(
                    isAdBlockerActive = adBlockState.isBlocked,
                    bannerAdUnitId = appConfig.bannerAdUnitId
                )
            }
        }
    }
}

@Composable
private fun StatMiniPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MidnightBg.copy(alpha = 0.72f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MidnightOutline.copy(alpha = 0.7f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
