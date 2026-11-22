package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ads.AdSdkInfo
import com.example.ui.theme.CyanSecondary
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MidnightCard
import com.example.ui.theme.MidnightCardHigh
import com.example.ui.theme.MidnightOutline
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

@Composable
fun AdMobTestBannerCard(
    isAdBlockerActive: Boolean,
    bannerAdUnitId: String = AdSdkInfo.TEST_BANNER_AD_UNIT_ID,
    modifier: Modifier = Modifier
) {
    val cleanUnitId = bannerAdUnitId.trim()
    if (isAdBlockerActive || cleanUnitId.isBlank()) return

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var bannerStatus by remember(cleanUnitId) { mutableStateOf("Loading Google AdMob Banner...") }
    var isLoaded by remember(cleanUnitId) { mutableStateOf(false) }

    val adView = remember(context, cleanUnitId) {
        AdView(context).apply {
            setAdSize(AdSize.BANNER)
            adUnitId = cleanUnitId
            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    isLoaded = true
                    bannerStatus = "AdMob Banner Loaded"
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    isLoaded = false
                    bannerStatus = "AdMob Unavailable (${error.code})"
                }
            }
            loadAd(AdRequest.Builder().build())
        }
    }

    DisposableEffect(lifecycleOwner, adView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> adView.resume()
                Lifecycle.Event.ON_PAUSE -> adView.pause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            adView.destroy()
        }
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MidnightCard),
        border = BorderStroke(1.dp, MidnightOutline),
        modifier = modifier
            .fillMaxWidth()
            .testTag("admob_test_banner_card")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = CyanSecondary.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "GOOGLE ADMOB BANNER",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyanSecondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Text(
                    text = bannerStatus,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isLoaded) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                color = MidnightCardHigh,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    AndroidView(
                        modifier = Modifier.fillMaxWidth(),
                        factory = { adView }
                    )
                }
            }
        }
    }
}
