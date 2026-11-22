package com.example

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ads.AdManager
import com.example.auth.AuthScreen
import com.example.auth.signOut
import com.example.data.AdminAndLiveConfigRepository
import com.example.data.NexaEarnRepository
import com.example.model.AppConfig
import com.example.model.UiState
import com.example.model.UserProfile
import com.example.model.buildWhatsAppSupportUrl
import com.example.model.sanitizeWhatsAppDigits
import com.example.notifications.NexaEarnNotificationHelper
import com.example.ui.MainViewModel
import com.example.ui.components.AdBlockerLockDialog
import com.example.ui.components.BannedAccountLockScreen
import com.example.ui.components.MaintenanceLockScreen
import com.example.ui.components.OnboardingTutorialOverlay
import com.example.ui.screens.AdminScreen
import com.example.ui.screens.AdsAndTestScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MathQuizScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ReferralScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TapTapScreen
import com.example.ui.screens.TermsAndConditionsScreen
import com.example.ui.screens.TicTacToeScreen
import com.example.ui.screens.WalletScreen
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MidnightBg
import com.example.ui.theme.MidnightCard
import com.example.ui.theme.NexaEarnTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

const val ROUTE_HOME = "home"
const val ROUTE_PLAY = "play"
const val ROUTE_TAP_TAP = "tap_tap"
const val ROUTE_MATH_QUIZ = "math_quiz"
const val ROUTE_HISTORY = "history"
const val ROUTE_ADS = "ads"
const val ROUTE_WALLET = "wallet"
const val ROUTE_PROFILE = "profile"
const val ROUTE_REFERRALS = "referrals"
const val ROUTE_SETTINGS = "settings"
const val ROUTE_TERMS = "terms_and_conditions"
const val ROUTE_ADMIN = "restricted_admin_console"

private data class NavDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val appDestinations = listOf(
    NavDestination(ROUTE_HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home, "nav_home_tab"),
    NavDestination(ROUTE_REFERRALS, "Referral", Icons.Filled.CardGiftcard, Icons.Outlined.CardGiftcard, "nav_referral_tab"),
    NavDestination(ROUTE_HISTORY, "History", Icons.Filled.History, Icons.Outlined.History, "nav_history_tab"),
    NavDestination(ROUTE_WALLET, "Wallet", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet, "nav_wallet_tab"),
    NavDestination(ROUTE_PROFILE, "Profile", Icons.Filled.Person, Icons.Outlined.Person, "nav_profile_tab")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NexaEarnNotificationHelper.ensureChannels(this)
        enableEdgeToEdge()
        setContent {
            NexaEarnTheme {
                AppContent()
            }
        }
    }
}

@Composable
fun AppContent() {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = { currentUser = Firebase.auth.currentUser })
    } else {
        key(user.uid) {
            AuthenticatedNexaEarnApp(
                currentUserId = user.uid,
                displayName = user.displayName,
                email = user.email
            )
        }
    }
}

@Composable
fun AuthenticatedNexaEarnApp(
    currentUserId: String,
    displayName: String?,
    email: String?
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: MainViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                val repo = NexaEarnRepository(db)
                val adminRepo = AdminAndLiveConfigRepository(db)
                val adManager = AdManager(app)
                MainViewModel(
                    repository = repo,
                    adminRepository = adminRepo,
                    adManager = adManager,
                    appContext = app.applicationContext,
                    currentUserId = currentUserId,
                    initialDisplayName = displayName,
                    initialEmail = email
                )
            }
        }
    )

    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val matchesState by viewModel.matchesState.collectAsStateWithLifecycle()
    val rewardsState by viewModel.rewardsState.collectAsStateWithLifecycle()
    val withdrawalsState by viewModel.withdrawalsState.collectAsStateWithLifecycle()
    val sentReferralsState by viewModel.sentReferralsState.collectAsStateWithLifecycle()
    val myReferralLinkState by viewModel.myReferralLinkState.collectAsStateWithLifecycle()
    val isApplyingReferral by viewModel.isApplyingReferral.collectAsStateWithLifecycle()
    val claimingReferralIds by viewModel.claimingReferralIds.collectAsStateWithLifecycle()
    val isSubmittingWithdrawal by viewModel.isSubmittingWithdrawal.collectAsStateWithLifecycle()
    val isClaimingExtraEarn by viewModel.isClaimingExtraEarn.collectAsStateWithLifecycle()
    val gameState by viewModel.gameState.collectAsStateWithLifecycle()
    val tapTapState by viewModel.tapTapState.collectAsStateWithLifecycle()
    val mathQuizState by viewModel.mathQuizState.collectAsStateWithLifecycle()
    val adBlockState by viewModel.adBlockState.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanningNetwork.collectAsStateWithLifecycle()
    val simulateAdBlocker by viewModel.simulateAdBlocker.collectAsStateWithLifecycle()
    val adSdkInfo by viewModel.adSdkInfo.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val notificationSettings by viewModel.notificationSettings.collectAsStateWithLifecycle()
    val showOnboardingOverlay by viewModel.showOnboardingOverlay.collectAsStateWithLifecycle()
    val appConfig by viewModel.appConfig.collectAsStateWithLifecycle()
    val latestLiveAlert by viewModel.latestLiveAlert.collectAsStateWithLifecycle()
    val isAdminUnlocked by viewModel.isAdminUnlocked.collectAsStateWithLifecycle()
    val isUnlockingAdmin by viewModel.isUnlockingAdmin.collectAsStateWithLifecycle()
    val adminAuthError by viewModel.adminAuthError.collectAsStateWithLifecycle()
    val isAdminBusy by viewModel.isAdminBusy.collectAsStateWithLifecycle()
    val adminUsers by viewModel.adminUsers.collectAsStateWithLifecycle()
    val adminWithdrawals by viewModel.adminWithdrawals.collectAsStateWithLifecycle()
    val adminNotifications by viewModel.adminNotifications.collectAsStateWithLifecycle()
    val activeProfile = (profileState as? UiState.Success)?.data
    val isAccountBanned = activeProfile?.banned == true

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(statusMessage) {
        val msg = statusMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.consumeStatusMessage()
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkOneAmGoalReset()
                viewModel.recordAppOpened()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ROUTE_HOME

    val navigateTo: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    // Show non-dismissible Ad Blocker Lock Dialog when detected
    if (adBlockState.isBlocked) {
        AdBlockerLockDialog(
            adBlockState = adBlockState,
            isScanning = isScanning,
            onRescan = { viewModel.runAdBlockerScan(forceRefresh = true) },
            onDisableSimulation = if (adBlockState.isSimulated) {
                { viewModel.toggleSimulateAdBlocker(false) }
            } else null
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = MidnightBg,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useNavigationRail) {
                    NavigationBar(containerColor = MidnightCard) {
                        appDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTo(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = EmeraldPrimary,
                                    selectedTextColor = EmeraldPrimary,
                                    indicatorColor = EmeraldPrimary.copy(alpha = 0.18f)
                                ),
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavigationRail) {
                    NavigationRail(
                        containerColor = MidnightCard,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        appDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTo(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    when (val pState = profileState) {
                        is UiState.Loading -> {
                            Column(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(color = EmeraldPrimary)
                                Text(
                                    text = "Syncing your NexaEarn wallet...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        is UiState.Error -> {
                            Column(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = pState.message,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Button(onClick = {
                                    signOut(credentialManager, scope)
                                }) {
                                    Text("Sign Out & Retry")
                                }
                            }
                        }
                        is UiState.Success -> {
                            val profile: UserProfile = pState.data
                            NavHost(
                                navController = navController,
                                startDestination = ROUTE_HOME,
                                enterTransition = { fadeIn(animationSpec = tween(160)) },
                                exitTransition = { fadeOut(animationSpec = tween(160)) },
                                popEnterTransition = { fadeIn(animationSpec = tween(160)) },
                                popExitTransition = { fadeOut(animationSpec = tween(160)) },
                                modifier = Modifier.fillMaxSize()
                            ) {
                                composable(ROUTE_HOME) {
                                    HomeScreen(
                                        profile = profile,
                                        adBlockState = adBlockState,
                                        appConfig = appConfig,
                                        latestLiveAlert = latestLiveAlert,
                                        onDismissLiveAlert = { viewModel.dismissLatestLiveAlert() },
                                        onPlayTicTacToe = {
                                            navController.navigate(ROUTE_PLAY) {
                                                launchSingleTop = true
                                            }
                                        },
                                        onClaimStreakAd = {
                                            viewModel.claimDailyStreakWithAd(activity)
                                        },
                                        onWatchBonusAd = {
                                            viewModel.watchBonusAd(activity)
                                        },
                                        onOpenAdsLab = {
                                            navController.navigate(ROUTE_ADS) {
                                                launchSingleTop = true
                                            }
                                        },
                                        onOpenProfile = {
                                            navigateTo(ROUTE_PROFILE)
                                        },
                                        onOpenSettings = {
                                            navController.navigate(ROUTE_SETTINGS) {
                                                launchSingleTop = true
                                            }
                                        },
                                        isClaimingExtraEarn = isClaimingExtraEarn,
                                        onClaimExtraEarn = {
                                            viewModel.claimExtraEarnPot()
                                        },
                                        onPlayTapTap = {
                                            navController.navigate(ROUTE_TAP_TAP) {
                                                launchSingleTop = true
                                            }
                                        },
                                        onPlayMathQuiz = {
                                            navController.navigate(ROUTE_MATH_QUIZ) {
                                                launchSingleTop = true
                                            }
                                        }
                                    )
                                }
                                composable(ROUTE_PLAY) {
                                    TicTacToeScreen(
                                        profile = profile,
                                        gameState = gameState,
                                        matchesState = matchesState,
                                        onStartMatch = { viewModel.startNewMatch(activity) },
                                        onCellTap = { idx -> viewModel.onCellTapped(idx, activity) },
                                        onClaimPendingMatchAd = { viewModel.claimPendingMatchRewardAd(activity) },
                                        onClaimMatchAd = { matchId ->
                                            viewModel.claimMatchRewardAd(matchId, activity)
                                        },
                                        onOpenTutorial = {
                                            viewModel.openOnboardingTutorial()
                                        },
                                        onNavigateBack = {
                                            navController.popBackStack()
                                        }
                                    )
                                }
                                composable(ROUTE_TAP_TAP) {
                                    TapTapScreen(
                                        tapTapState = tapTapState,
                                        isAdBlockerActive = adBlockState.isBlocked,
                                        bannerAdUnitId = appConfig.bannerAdUnitId,
                                        onStartRound = { viewModel.startTapTapRound(activity) },
                                        onTapPressed = { viewModel.onTapTapPressed() },
                                        onClaimPendingAd = { viewModel.claimPendingTapTapAd(activity) },
                                        onLeaveScreen = { viewModel.cancelTapTapIfRunning() },
                                        onNavigateBack = {
                                            viewModel.cancelTapTapIfRunning()
                                            navController.popBackStack()
                                        }
                                    )
                                }
                                composable(ROUTE_MATH_QUIZ) {
                                    MathQuizScreen(
                                        quizState = mathQuizState,
                                        isAdBlockerActive = adBlockState.isBlocked,
                                        bannerAdUnitId = appConfig.bannerAdUnitId,
                                        onStartLevel = { viewModel.startMathQuizLevel(activity) },
                                        onSelectOption = { opt -> viewModel.onMathOptionSelected(opt, activity) },
                                        onWatchContinueAd = { viewModel.retryMathQuizContinueAd(activity) },
                                        onClaimPendingAd = { viewModel.claimPendingMathQuizAd(activity) },
                                        onNavigateBack = {
                                            navController.popBackStack()
                                        }
                                    )
                                }
                                composable(ROUTE_HISTORY) {
                                    HistoryScreen(
                                        profile = profile,
                                        matchesState = matchesState,
                                        rewardsState = rewardsState,
                                        isAdBlockerActive = adBlockState.isBlocked,
                                        bannerAdUnitId = appConfig.bannerAdUnitId,
                                        onClaimMatchAd = { matchId ->
                                            viewModel.claimMatchRewardAd(matchId, activity)
                                        },
                                        onPlayTicTacToe = {
                                            navController.navigate(ROUTE_PLAY) {
                                                launchSingleTop = true
                                            }
                                        }
                                    )
                                }
                                composable(ROUTE_ADS) {
                                    AdsAndTestScreen(
                                        adSdkInfo = adSdkInfo,
                                        adBlockState = adBlockState,
                                        simulateAdBlocker = simulateAdBlocker,
                                        isScanning = isScanning,
                                        bonusAdCycleProgress = profile.clampedWatchAdProgress(),
                                        onWatchBonusAd = { viewModel.watchBonusAd(activity) },
                                        onLaunchGoogleRewardedAd = {
                                            viewModel.launchGoogleRewardedAdTest(activity)
                                        },
                                        onLaunchRewardedInterstitialAd = {
                                            viewModel.launchRewardedInterstitialAdTest(activity)
                                        },
                                        onLaunchInterstitialTestAd = {
                                            viewModel.launchInterstitialTestAd(activity)
                                        },
                                        onOpenAdInspector = {
                                            viewModel.openAdInspector(activity)
                                        },
                                        onRefreshTestAdSdk = { viewModel.refreshTestAdSdk() },
                                        onRescanAdBlocker = { viewModel.runAdBlockerScan(forceRefresh = true) },
                                        onToggleSimulateAdBlocker = { enabled ->
                                            viewModel.toggleSimulateAdBlocker(enabled)
                                        },
                                        onNavigateBack = {
                                            navController.popBackStack()
                                        }
                                    )
                                }
                                composable(ROUTE_WALLET) {
                                    WalletScreen(
                                        profile = profile,
                                        withdrawalsState = withdrawalsState,
                                        isSubmittingWithdrawal = isSubmittingWithdrawal,
                                        isAdBlockerActive = adBlockState.isBlocked,
                                        bannerAdUnitId = appConfig.bannerAdUnitId,
                                        onSubmitWithdrawal = { method, title, dest, coins, onSuccess ->
                                            viewModel.submitWithdrawal(
                                                method = method,
                                                accountTitle = title,
                                                destination = dest,
                                                coins = coins,
                                                onSuccess = onSuccess
                                            )
                                        },
                                        onSignOut = { signOut(credentialManager, scope) },
                                        isClaimingExtraEarn = isClaimingExtraEarn,
                                        onClaimExtraEarn = {
                                            viewModel.claimExtraEarnPot()
                                        }
                                    )
                                }
                                composable(ROUTE_PROFILE) {
                                    ProfileScreen(
                                        profile = profile,
                                        appConfig = appConfig,
                                        onSignOut = { signOut(credentialManager, scope) },
                                        onOpenTutorial = {
                                            viewModel.openOnboardingTutorial()
                                        },
                                        onOpenReferrals = {
                                            navigateTo(ROUTE_REFERRALS)
                                        },
                                        onOpenSettings = {
                                            navController.navigate(ROUTE_SETTINGS) {
                                                launchSingleTop = true
                                            }
                                        },
                                        onOpenTerms = {
                                            navController.navigate(ROUTE_TERMS) {
                                                launchSingleTop = true
                                            }
                                        },
                                        onContactSupportWhatsApp = {
                                            launchWhatsAppSupport(context, appConfig) { fallbackMsg ->
                                                viewModel.showStatusMessage(fallbackMsg)
                                            }
                                        },
                                        onOpenHiddenAdminPanel = {
                                            navController.navigate(ROUTE_ADMIN) {
                                                launchSingleTop = true
                                            }
                                        }
                                    )
                                }
                                composable(ROUTE_TERMS) {
                                    TermsAndConditionsScreen(
                                        appConfig = appConfig,
                                        onContactSupport = {
                                            launchWhatsAppSupport(context, appConfig) { fallbackMsg ->
                                                viewModel.showStatusMessage(fallbackMsg)
                                            }
                                        },
                                        onNavigateBack = {
                                            navController.popBackStack()
                                        }
                                    )
                                }
                                composable(ROUTE_ADMIN) {
                                    AdminScreen(
                                        isUnlocked = isAdminUnlocked,
                                        isUnlocking = isUnlockingAdmin,
                                        authError = adminAuthError,
                                        isBusy = isAdminBusy,
                                        users = adminUsers,
                                        withdrawals = adminWithdrawals,
                                        notifications = adminNotifications,
                                        appConfig = appConfig,
                                        onUnlock = { password -> viewModel.unlockAdminPanel(password) },
                                        onLockAndExit = {
                                            viewModel.lockAdminPanel()
                                            navController.popBackStack()
                                        },
                                        onAdjustCoins = { targetUid, deltaCoins, note ->
                                            viewModel.adminAdjustUserCoins(targetUid, deltaCoins, note)
                                        },
                                        onSetUserAccessStatus = { targetUid, banned, whitelisted, reason ->
                                            viewModel.adminSetUserAccessStatus(targetUid, banned, whitelisted, reason)
                                        },
                                        onUpdateWithdrawalStatus = { wd, status, refund, note ->
                                            viewModel.adminUpdateWithdrawalStatus(wd, status, refund, note)
                                        },
                                        onSendNotification = { title, body, targetUid, targetEmail ->
                                            viewModel.adminSendCustomNotification(title, body, targetUid, targetEmail)
                                        },
                                        onDeleteNotification = { id ->
                                            viewModel.adminDeleteNotification(id)
                                        },
                                        onSaveAppConfig = { config ->
                                            viewModel.adminSaveAppConfig(config)
                                        },
                                        onNavigateBack = {
                                            navController.popBackStack()
                                        }
                                    )
                                }
                                composable(ROUTE_SETTINGS) {
                                    SettingsScreen(
                                        profile = profile,
                                        notificationSettings = notificationSettings,
                                        onToggleMaster = { viewModel.setMasterNotificationsEnabled(it) },
                                        onToggleInactivityStreak = { viewModel.setInactivityStreakNotificationsEnabled(it) },
                                        onToggleMotivational = { viewModel.setMotivationalNotificationsEnabled(it) },
                                        onToggleEarningAlerts = { viewModel.setEarningAlertsEnabled(it) },
                                        onSelectReminderIntervalHours = { viewModel.setReminderIntervalHours(it) },
                                        onSendTestNotification = { viewModel.sendTestNotification() },
                                        onPermissionResultMessage = { viewModel.showStatusMessage(it) },
                                        onNavigateBack = {
                                            navController.popBackStack()
                                        }
                                    )
                                }
                                composable(ROUTE_REFERRALS) {
                                    ReferralScreen(
                                        profile = profile,
                                        sentReferralsState = sentReferralsState,
                                        myReferralLinkState = myReferralLinkState,
                                        isApplyingReferral = isApplyingReferral,
                                        claimingReferralIds = claimingReferralIds,
                                        onApplyReferralCode = { code, onSuccess ->
                                            viewModel.applyReferralCode(code, onSuccess)
                                        },
                                        onClaimReferralBonus = { inviteeUid, friendName ->
                                            viewModel.claimReferralBonus(inviteeUid, friendName)
                                        },
                                        onShowMessage = { msg ->
                                            viewModel.showStatusMessage(msg)
                                        },
                                        onNavigateBack = null
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = isAccountBanned && !isAdminUnlocked && currentRoute != ROUTE_ADMIN,
            enter = fadeIn(animationSpec = tween(160)),
            exit = fadeOut(animationSpec = tween(160))
        ) {
            var banTapCount by remember { mutableIntStateOf(0) }
            var lastBanTap by remember { mutableLongStateOf(0L) }
            BannedAccountLockScreen(
                email = activeProfile?.email,
                banReason = activeProfile?.banReason,
                supportHours = appConfig.supportHours,
                onContactSupport = {
                    launchWhatsAppSupport(context, appConfig) { fallbackMsg ->
                        viewModel.showStatusMessage(fallbackMsg)
                    }
                },
                onSignOut = { signOut(credentialManager, scope) },
                onStealthTap = {
                    val now = System.currentTimeMillis()
                    if (now - lastBanTap > 3000L) banTapCount = 1 else banTapCount += 1
                    lastBanTap = now
                    if (banTapCount >= 7) {
                        banTapCount = 0
                        navController.navigate(ROUTE_ADMIN) {
                            launchSingleTop = true
                        }
                    }
                }
            )
        }

        AnimatedVisibility(
            visible = !isAccountBanned && appConfig.maintenanceMode && !isAdminUnlocked && currentRoute != ROUTE_ADMIN,
            enter = fadeIn(animationSpec = tween(160)),
            exit = fadeOut(animationSpec = tween(160))
        ) {
            var maintTapCount by remember { mutableIntStateOf(0) }
            var lastMaintTap by remember { mutableLongStateOf(0L) }
            MaintenanceLockScreen(
                message = appConfig.maintenanceMessage,
                supportHours = appConfig.supportHours,
                onContactSupport = {
                    launchWhatsAppSupport(context, appConfig) { fallbackMsg ->
                        viewModel.showStatusMessage(fallbackMsg)
                    }
                },
                onStealthTap = {
                    val now = System.currentTimeMillis()
                    if (now - lastMaintTap > 3000L) maintTapCount = 1 else maintTapCount += 1
                    lastMaintTap = now
                    if (maintTapCount >= 7) {
                        maintTapCount = 0
                        navController.navigate(ROUTE_ADMIN) {
                            launchSingleTop = true
                        }
                    }
                }
            )
        }

        AnimatedVisibility(
            visible = showOnboardingOverlay && !adBlockState.isBlocked && !appConfig.maintenanceMode && !isAccountBanned,
            enter = fadeIn(animationSpec = tween(160)),
            exit = fadeOut(animationSpec = tween(160))
        ) {
            OnboardingTutorialOverlay(
                onDismiss = { viewModel.dismissOnboardingTutorial() },
                onStartPlaying = {
                    viewModel.dismissOnboardingTutorial()
                    navController.navigate(ROUTE_PLAY) {
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}

private fun launchWhatsAppSupport(
    context: Context,
    appConfig: AppConfig,
    onFeedback: (String) -> Unit
) {
    val url = buildWhatsAppSupportUrl(
        rawPhone = appConfig.whatsappSupportNumber,
        prefillMessage = appConfig.whatsappPrefillMessage
    )
    if (url == null) {
        onFeedback("Customer Support WhatsApp number is not configured yet.")
        return
    }
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        val digits = sanitizeWhatsAppDigits(appConfig.whatsappSupportNumber)
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText("WhatsApp Support", "+$digits"))
        onFeedback("WhatsApp not installed. Support number +$digits copied to clipboard!")
    }
}
