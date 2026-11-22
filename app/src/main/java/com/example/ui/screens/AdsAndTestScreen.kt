package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ads.AdBlockerCheckResult
import com.example.ads.AdSdkInfo
import com.example.ads.AdSdkLoadStatus
import com.example.model.WATCH_AD_BUNDLE_COINS
import com.example.model.WATCH_AD_BUNDLE_COUNT
import com.example.ui.components.AdMobTestBannerCard
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CoralError
import com.example.ui.theme.CyanSecondary
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MidnightCard
import com.example.ui.theme.MidnightCardHigh
import com.example.ui.theme.MidnightOutline

@Composable
fun AdsAndTestScreen(
    adSdkInfo: AdSdkInfo,
    adBlockState: AdBlockerCheckResult,
    simulateAdBlocker: Boolean,
    isScanning: Boolean,
    bonusAdCycleProgress: Int = 0,
    onWatchBonusAd: () -> Unit,
    onLaunchGoogleRewardedAd: () -> Unit,
    onLaunchRewardedInterstitialAd: () -> Unit,
    onLaunchInterstitialTestAd: () -> Unit,
    onOpenAdInspector: () -> Unit,
    onRefreshTestAdSdk: () -> Unit,
    onRescanAdBlocker: () -> Unit,
    onToggleSimulateAdBlocker: (Boolean) -> Unit,
    onNavigateBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (onNavigateBack != null) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("ads_lab_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                    Text(
                        text = "Google AdMob Ads & Shield",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                // Watch Google AdMob Rewarded Video Card (3 Ads -> +15 Coins Unlimited)
                val clampedProgress = bonusAdCycleProgress.coerceIn(0, (WATCH_AD_BUNDLE_COUNT - 1L).toInt())
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MidnightCard),
                    border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayCircle,
                                contentDescription = "Google AdMob Rewarded Ads",
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                            Column {
                                Text(
                                    text = "Watch $WATCH_AD_BUNDLE_COUNT Google Ads • +$WATCH_AD_BUNDLE_COINS Coins",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Unlimited times! Watch $WATCH_AD_BUNDLE_COUNT Google AdMob ads to earn +$WATCH_AD_BUNDLE_COINS coins. 0 coins if ads aren't available.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ads_watch_progress_bar"),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (stepIdx in 1..WATCH_AD_BUNDLE_COUNT.toInt()) {
                                val done = stepIdx <= clampedProgress
                                Surface(
                                    color = if (done) EmeraldPrimary.copy(alpha = 0.22f) else MidnightCardHigh,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        if (done) EmeraldPrimary else MidnightOutline
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (done) Icons.Filled.CheckCircle else Icons.Filled.PlayCircle,
                                            contentDescription = "Ad $stepIdx of $WATCH_AD_BUNDLE_COUNT",
                                            tint = if (done) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.size(4.dp))
                                        Text(
                                            text = "Ad $stepIdx/$WATCH_AD_BUNDLE_COUNT",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (done) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = if (done) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = onWatchBonusAd,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldPrimary,
                                contentColor = Color(0xFF022C22)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("watch_bonus_ad_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayCircle,
                                contentDescription = "Watch Google AdMob rewarded ad",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = if (adSdkInfo.anyRewardedAvailable) {
                                    "Watch Ad ($clampedProgress/$WATCH_AD_BUNDLE_COUNT) • +$WATCH_AD_BUNDLE_COINS Coins"
                                } else {
                                    "AdMob Ad Unavailable (0 Coins Added)"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Google AdMob Formats Suite (All Google AdMob Ads Only)
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MidnightCard),
                    border = BorderStroke(1.dp, CyanSecondary.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("test_ad_lab_card")
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
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.BugReport,
                                    contentDescription = "Google AdMob Suite",
                                    tint = CyanSecondary,
                                    modifier = Modifier.size(26.dp)
                                )
                                Text(
                                    text = "Google AdMob Ad Suite",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Surface(
                                color = when (adSdkInfo.status) {
                                    AdSdkLoadStatus.ADMOB_LOADED -> EmeraldPrimary.copy(alpha = 0.16f)
                                    AdSdkLoadStatus.LOADING -> CyanSecondary.copy(alpha = 0.16f)
                                    else -> AmberGold.copy(alpha = 0.16f)
                                },
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = when (adSdkInfo.status) {
                                        AdSdkLoadStatus.ADMOB_LOADED -> "ADMOB READY"
                                        AdSdkLoadStatus.LOADING -> "LOADING"
                                        else -> "UNAVAILABLE"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = when (adSdkInfo.status) {
                                        AdSdkLoadStatus.ADMOB_LOADED -> EmeraldPrimary
                                        AdSdkLoadStatus.LOADING -> CyanSecondary
                                        else -> AmberGold
                                    },
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }

                        Surface(
                            color = MidnightCardHigh,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MidnightOutline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Active Google AdMob Units:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Rewarded: ${adSdkInfo.testAdUnitId.ifBlank { "Removed / Disabled" }}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (adSdkInfo.testAdUnitId.isBlank()) CoralError else CyanSecondary
                                )
                                Text(
                                    text = "Rewarded Interstitial: ${adSdkInfo.testRewardedInterstitialUnitId}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CyanSecondary
                                )
                                Text(
                                    text = "Interstitial: ${adSdkInfo.testInterstitialUnitId}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CyanSecondary
                                )
                                Text(
                                    text = "Banner: ${adSdkInfo.testBannerUnitId.ifBlank { "Removed / Disabled" }}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (adSdkInfo.testBannerUnitId.isBlank()) CoralError else CyanSecondary
                                )
                                Text(
                                    text = adSdkInfo.statusDetail,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (adSdkInfo.anyRewardedAvailable) EmeraldPrimary else AmberGold
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onLaunchGoogleRewardedAd,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CyanSecondary,
                                    contentColor = Color(0xFF082F49)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("launch_google_test_ad_button")
                            ) {
                                Text(
                                    text = if (adSdkInfo.rewardedLoaded) "Rewarded Ad" else "Rewarded (Wait)",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            OutlinedButton(
                                onClick = onLaunchRewardedInterstitialAd,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.6f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("launch_rewarded_interstitial_ad_button")
                            ) {
                                Text(
                                    text = if (adSdkInfo.rewardedInterstitialLoaded) "Rewarded Interstitial" else "Load Rew. Int.",
                                    color = EmeraldPrimary,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onLaunchInterstitialTestAd,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("launch_interstitial_test_ad_button")
                            ) {
                                Text(
                                    text = if (adSdkInfo.interstitialLoaded) "Interstitial Ad" else "Load Interstitial",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                            OutlinedButton(
                                onClick = onOpenAdInspector,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("open_ad_inspector_button")
                            ) {
                                Text(
                                    text = "Ad Inspector",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = onRefreshTestAdSdk,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("reload_test_ad_sdk_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "Reload Google AdMob Ads",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                            Text("Reload All Google AdMob Ads")
                        }
                    }
                }

                // Live Google AdMob Banner Ad
                AdMobTestBannerCard(
                    isAdBlockerActive = adBlockState.isBlocked,
                    bannerAdUnitId = adSdkInfo.testBannerUnitId
                )

                // Ad Blocker Detector & Diagnostics Card
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MidnightCard),
                    border = BorderStroke(
                        1.dp,
                        if (adBlockState.isBlocked) CoralError.copy(alpha = 0.6f) else MidnightOutline
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ad_blocker_diagnostics_card")
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
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = if (adBlockState.isBlocked) Icons.Filled.GppBad else Icons.Filled.Shield,
                                    contentDescription = "Ad Blocker Detector",
                                    tint = if (adBlockState.isBlocked) CoralError else EmeraldPrimary,
                                    modifier = Modifier.size(26.dp)
                                )
                                Column {
                                    Text(
                                        text = "Ad Blocker Detector",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (adBlockState.isBlocked) "Blocked" else "Verified Clean",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (adBlockState.isBlocked) CoralError else EmeraldPrimary
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Filled.Verified,
                                contentDescription = "Detector active",
                                tint = if (adBlockState.isBlocked) CoralError else EmeraldPrimary
                            )
                        }

                        Surface(
                            color = MidnightCardHigh,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MidnightOutline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                adBlockState.probeDetails.forEach { line ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (adBlockState.isBlocked) Icons.Filled.GppBad else Icons.Filled.CheckCircle,
                                            contentDescription = null,
                                            tint = if (adBlockState.isBlocked) CoralError else EmeraldPrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = line,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Simulate Ad Blocker (Test)",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Preview how NexaEarn blocks coin earning when an ad blocker is active",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = simulateAdBlocker,
                                onCheckedChange = onToggleSimulateAdBlocker,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = CoralError
                                ),
                                modifier = Modifier.testTag("simulate_ad_blocker_switch")
                            )
                        }

                        OutlinedButton(
                            onClick = onRescanAdBlocker,
                            enabled = !isScanning,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("run_adblock_scan_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "Scan connection",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(if (isScanning) "Scanning DNS & Network..." else "Run Live Ad-Shield Scan")
                        }
                    }
                }
            }
        }
    }
}
