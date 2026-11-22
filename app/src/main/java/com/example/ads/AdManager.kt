package com.example.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.example.model.AdSource
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

enum class AdSdkLoadStatus {
    IDLE,
    LOADING,
    ADMOB_LOADED,
    UNAVAILABLE
}

data class AdSdkInfo(
    val status: AdSdkLoadStatus = AdSdkLoadStatus.IDLE,
    val statusDetail: String = "Initializing Google AdMob SDK...",
    val testAdUnitId: String = TEST_REWARDED_AD_UNIT_ID,
    val testRewardedInterstitialUnitId: String = TEST_REWARDED_INTERSTITIAL_AD_UNIT_ID,
    val testBannerUnitId: String = TEST_BANNER_AD_UNIT_ID,
    val testInterstitialUnitId: String = TEST_INTERSTITIAL_AD_UNIT_ID,
    val rewardedLoaded: Boolean = false,
    val rewardedInterstitialLoaded: Boolean = false,
    val interstitialLoaded: Boolean = false
) {
    val anyRewardedAvailable: Boolean
        get() = rewardedLoaded || rewardedInterstitialLoaded

    companion object {
        // Official Google AdMob Ad Unit IDs (Test units for safe development)
        const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
        const val TEST_REWARDED_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/5354046379"
        const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
        const val TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
    }
}

class AdManager(context: Context) {

    private val appContext = context.applicationContext
    private var rewardedAd: RewardedAd? = null
    private var rewardedInterstitialAd: RewardedInterstitialAd? = null
    private var interstitialAd: InterstitialAd? = null
    private var sdkInitialized = false
    private var isRewardedLoading = false
    private var isRewardedInterstitialLoading = false
    private var isInterstitialLoading = false
    private var activeRewardedUnitId: String = AdSdkInfo.TEST_REWARDED_AD_UNIT_ID
    private var activeBannerUnitId: String = AdSdkInfo.TEST_BANNER_AD_UNIT_ID
    private var rewardedLoadGeneration: Long = 0L

    private val _sdkInfo = MutableStateFlow(AdSdkInfo())
    val sdkInfo: StateFlow<AdSdkInfo> = _sdkInfo.asStateFlow()

    fun updateAdUnitIds(rewardedUnitId: String, bannerUnitId: String) {
        val cleanRewarded = rewardedUnitId.trim()
        val cleanBanner = bannerUnitId.trim()
        val rewardedChanged = cleanRewarded != activeRewardedUnitId
        val bannerChanged = cleanBanner != activeBannerUnitId

        if (!rewardedChanged && !bannerChanged) return

        activeBannerUnitId = cleanBanner
        if (rewardedChanged) {
            rewardedLoadGeneration += 1L
            activeRewardedUnitId = cleanRewarded
            rewardedAd = null
            isRewardedLoading = false
        }

        _sdkInfo.value = _sdkInfo.value.copy(
            testAdUnitId = activeRewardedUnitId,
            testBannerUnitId = activeBannerUnitId
        )

        if (rewardedChanged) {
            if (activeRewardedUnitId.isBlank()) {
                updateStatusSummary("Google AdMob Rewarded Ad removed/disabled by Admin")
            } else if (sdkInitialized) {
                preloadRewardedAd()
            } else {
                updateStatusSummary()
            }
        }
    }

    fun initializeAndPreload() {
        if (!sdkInitialized) {
            sdkInitialized = true
            _sdkInfo.value = _sdkInfo.value.copy(
                status = AdSdkLoadStatus.LOADING,
                statusDetail = "Initializing Google AdMob SDK..."
            )
            try {
                val requestConfig = RequestConfiguration.Builder()
                    .setTestDeviceIds(listOf(AdRequest.DEVICE_ID_EMULATOR))
                    .build()
                MobileAds.setRequestConfiguration(requestConfig)
                MobileAds.initialize(appContext) {
                    preloadAllGoogleAdMobAds()
                }
            } catch (e: Exception) {
                Log.w(TAG, "MobileAds.initialize error: ${e.message}")
                _sdkInfo.value = _sdkInfo.value.copy(
                    status = AdSdkLoadStatus.UNAVAILABLE,
                    statusDetail = "Google AdMob unavailable: ${e.localizedMessage ?: "SDK init failed"}"
                )
            }
        } else {
            preloadMissingAds()
        }
    }

    fun preloadAllGoogleAdMobAds() {
        preloadRewardedAd()
        preloadRewardedInterstitialAd()
        preloadInterstitialAd()
    }

    fun preloadMissingAds() {
        if (activeRewardedUnitId.isNotBlank() && rewardedAd == null && !isRewardedLoading) preloadRewardedAd()
        if (rewardedInterstitialAd == null && !isRewardedInterstitialLoading) preloadRewardedInterstitialAd()
        if (interstitialAd == null && !isInterstitialLoading) preloadInterstitialAd()
    }

    fun hasAnyRewardedAd(): Boolean {
        if (activeRewardedUnitId.isBlank()) return false
        return rewardedAd != null || rewardedInterstitialAd != null
    }

    suspend fun ensureRewardedAdLoadedQuickly(timeoutMillis: Long = 900L): Boolean {
        if (activeRewardedUnitId.isBlank()) return false
        if (hasAnyRewardedAd()) return true
        preloadMissingAds()
        if (isRewardedLoading || isRewardedInterstitialLoading) {
            withTimeoutOrNull(timeoutMillis) {
                _sdkInfo.first { it.anyRewardedAvailable || (!isRewardedLoading && !isRewardedInterstitialLoading) }
            }
        }
        return hasAnyRewardedAd()
    }

    fun preloadRewardedAd() {
        val unitId = activeRewardedUnitId
        if (unitId.isBlank()) {
            isRewardedLoading = false
            rewardedAd = null
            updateStatusSummary("Google AdMob Rewarded Ad removed/disabled by Admin")
            return
        }
        if (isRewardedLoading) return
        val generation = rewardedLoadGeneration
        isRewardedLoading = true
        updateStatusSummary()
        try {
            val adRequest = AdRequest.Builder().build()
            RewardedAd.load(
                appContext,
                unitId,
                adRequest,
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        if (generation != rewardedLoadGeneration) return
                        isRewardedLoading = false
                        rewardedAd = ad
                        updateStatusSummary("Google AdMob Rewarded Ad loaded & ready")
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        if (generation != rewardedLoadGeneration) return
                        isRewardedLoading = false
                        rewardedAd = null
                        Log.w(TAG, "AdMob RewardedAd failed (${loadAdError.code}): ${loadAdError.message}")
                        updateStatusSummary("AdMob Rewarded unavailable (code ${loadAdError.code})")
                    }
                }
            )
        } catch (e: Exception) {
            if (generation != rewardedLoadGeneration) return
            isRewardedLoading = false
            rewardedAd = null
            Log.w(TAG, "RewardedAd.load exception: ${e.message}")
            updateStatusSummary("AdMob Rewarded unavailable")
        }
    }

    fun preloadRewardedInterstitialAd() {
        if (isRewardedInterstitialLoading) return
        isRewardedInterstitialLoading = true
        updateStatusSummary()
        try {
            val adRequest = AdRequest.Builder().build()
            RewardedInterstitialAd.load(
                appContext,
                AdSdkInfo.TEST_REWARDED_INTERSTITIAL_AD_UNIT_ID,
                adRequest,
                object : RewardedInterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedInterstitialAd) {
                        isRewardedInterstitialLoading = false
                        rewardedInterstitialAd = ad
                        updateStatusSummary()
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        isRewardedInterstitialLoading = false
                        rewardedInterstitialAd = null
                        Log.w(TAG, "AdMob RewardedInterstitial failed (${loadAdError.code}): ${loadAdError.message}")
                        updateStatusSummary()
                    }
                }
            )
        } catch (e: Exception) {
            isRewardedInterstitialLoading = false
            rewardedInterstitialAd = null
            Log.w(TAG, "RewardedInterstitialAd.load exception: ${e.message}")
            updateStatusSummary()
        }
    }

    fun preloadInterstitialAd() {
        if (isInterstitialLoading) return
        isInterstitialLoading = true
        try {
            val adRequest = AdRequest.Builder().build()
            InterstitialAd.load(
                appContext,
                AdSdkInfo.TEST_INTERSTITIAL_AD_UNIT_ID,
                adRequest,
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        isInterstitialLoading = false
                        interstitialAd = ad
                        _sdkInfo.value = _sdkInfo.value.copy(interstitialLoaded = true)
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        isInterstitialLoading = false
                        interstitialAd = null
                        _sdkInfo.value = _sdkInfo.value.copy(interstitialLoaded = false)
                        Log.w(TAG, "AdMob Interstitial failed (${loadAdError.code}): ${loadAdError.message}")
                    }
                }
            )
        } catch (e: Exception) {
            isInterstitialLoading = false
            interstitialAd = null
            _sdkInfo.value = _sdkInfo.value.copy(interstitialLoaded = false)
            Log.w(TAG, "InterstitialAd.load exception: ${e.message}")
        }
    }

    fun showAdMobRewardedAd(
        activity: Activity?,
        onRewardEarned: (AdSource) -> Unit,
        onDismissedWithoutReward: () -> Unit
    ): Boolean {
        if (activity == null) return false
        if (activeRewardedUnitId.isBlank()) return false

        val primaryRewarded = rewardedAd
        if (primaryRewarded != null) {
            var rewardGranted = false
            primaryRewarded.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    rewardedAd = null
                    updateStatusSummary()
                    preloadRewardedAd()
                    if (!rewardGranted) {
                        onDismissedWithoutReward()
                    }
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.w(TAG, "AdMob Rewarded failed to show: ${adError.message}")
                    rewardedAd = null
                    updateStatusSummary()
                    preloadRewardedAd()
                    onDismissedWithoutReward()
                }
            }
            rewardedAd = null
            updateStatusSummary()
            primaryRewarded.show(activity) {
                rewardGranted = true
                onRewardEarned(AdSource.ADMOB_TEST)
            }
            return true
        }

        val secondaryRewarded = rewardedInterstitialAd
        if (secondaryRewarded != null) {
            var rewardGranted = false
            secondaryRewarded.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    rewardedInterstitialAd = null
                    updateStatusSummary()
                    preloadRewardedInterstitialAd()
                    if (!rewardGranted) {
                        onDismissedWithoutReward()
                    }
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.w(TAG, "AdMob RewardedInterstitial failed to show: ${adError.message}")
                    rewardedInterstitialAd = null
                    updateStatusSummary()
                    preloadRewardedInterstitialAd()
                    onDismissedWithoutReward()
                }
            }
            rewardedInterstitialAd = null
            updateStatusSummary()
            secondaryRewarded.show(activity) {
                rewardGranted = true
                onRewardEarned(AdSource.ADMOB_TEST)
            }
            return true
        }

        return false
    }

    fun showAdMobRewardedInterstitialOnly(
        activity: Activity?,
        onRewardEarned: (AdSource) -> Unit,
        onDismissedWithoutReward: () -> Unit
    ): Boolean {
        if (activity == null) return false
        val ad = rewardedInterstitialAd ?: return false
        var rewardGranted = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedInterstitialAd = null
                updateStatusSummary()
                preloadRewardedInterstitialAd()
                if (!rewardGranted) {
                    onDismissedWithoutReward()
                }
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                rewardedInterstitialAd = null
                updateStatusSummary()
                preloadRewardedInterstitialAd()
                onDismissedWithoutReward()
            }
        }
        rewardedInterstitialAd = null
        updateStatusSummary()
        ad.show(activity) {
            rewardGranted = true
            onRewardEarned(AdSource.ADMOB_TEST)
        }
        return true
    }

    fun showLoadedInterstitialAd(
        activity: Activity?,
        onClosed: () -> Unit
    ): Boolean {
        val ad = interstitialAd ?: return false
        if (activity == null) return false

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                _sdkInfo.value = _sdkInfo.value.copy(interstitialLoaded = false)
                preloadInterstitialAd()
                onClosed()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                interstitialAd = null
                _sdkInfo.value = _sdkInfo.value.copy(interstitialLoaded = false)
                preloadInterstitialAd()
                onClosed()
            }
        }
        interstitialAd = null
        _sdkInfo.value = _sdkInfo.value.copy(interstitialLoaded = false)
        ad.show(activity)
        return true
    }

    fun openAdInspector(activity: Activity?, onResult: (String) -> Unit) {
        if (activity == null) {
            onResult("Activity unavailable for AdMob Inspector.")
            return
        }
        try {
            MobileAds.openAdInspector(activity) { error ->
                if (error != null) {
                    onResult("AdMob Inspector: ${error.message}")
                } else {
                    onResult("Google AdMob Inspector closed.")
                }
            }
        } catch (e: Exception) {
            onResult("AdMob Inspector unavailable: ${e.localizedMessage ?: "SDK error"}")
        }
    }

    private fun updateStatusSummary(overrideDetail: String? = null) {
        val rewardedEnabled = activeRewardedUnitId.isNotBlank()
        val hasRewarded = rewardedEnabled && rewardedAd != null
        val hasRewardedInterstitial = rewardedEnabled && rewardedInterstitialAd != null
        val status = when {
            !rewardedEnabled -> AdSdkLoadStatus.UNAVAILABLE
            hasRewarded || hasRewardedInterstitial -> AdSdkLoadStatus.ADMOB_LOADED
            isRewardedLoading || isRewardedInterstitialLoading -> AdSdkLoadStatus.LOADING
            else -> AdSdkLoadStatus.UNAVAILABLE
        }
        val detail = overrideDetail ?: when {
            !rewardedEnabled ->
                "Rewarded Ad Unit ID removed/disabled by Admin — no coins can be earned"
            status == AdSdkLoadStatus.ADMOB_LOADED -> when {
                hasRewarded && hasRewardedInterstitial ->
                    "Google AdMob Rewarded & Rewarded Interstitial ads ready"
                hasRewarded ->
                    "Google AdMob Rewarded Ad loaded & ready"
                else ->
                    "Google AdMob Rewarded Interstitial Ad loaded & ready"
            }
            status == AdSdkLoadStatus.LOADING ->
                "Loading Google AdMob ads..."
            else ->
                "Google AdMob ads currently unavailable — no coins can be earned until an ad loads"
        }
        _sdkInfo.value = _sdkInfo.value.copy(
            status = status,
            statusDetail = detail,
            testAdUnitId = activeRewardedUnitId,
            testBannerUnitId = activeBannerUnitId,
            rewardedLoaded = hasRewarded,
            rewardedInterstitialLoaded = hasRewardedInterstitial,
            interstitialLoaded = interstitialAd != null
        )
    }

    private companion object {
        const val TAG = "NexaEarnAdManager"
    }
}
