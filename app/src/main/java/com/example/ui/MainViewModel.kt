package com.example.ui

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ads.AdBlockerCheckResult
import com.example.ads.AdBlockerDetector
import com.example.ads.AdManager
import com.example.ads.AdSdkInfo
import com.example.data.AdminAndLiveConfigRepository
import com.example.data.NexaEarnRepository
import com.example.data.NotificationPreferences
import com.example.data.OnboardingPreferences
import com.example.game.CellMark
import com.example.game.MathQuestion
import com.example.game.MathQuizEngine
import com.example.game.TicTacToeEngine
import com.example.notifications.NexaEarnNotificationHelper
import com.example.notifications.NotificationScheduler
import com.example.notifications.NotificationSettings
import com.example.model.AdSource
import com.example.model.AdminNotification
import com.example.model.AppConfig
import com.example.model.COINS_PER_GAME
import com.example.model.GameMatchRecord
import com.example.model.MATH_QUIZ_COINS
import com.example.model.MatchOutcome
import com.example.model.MiniGameType
import com.example.model.REFERRAL_TARGET_GAMES
import com.example.model.ReferralRecord
import com.example.model.RewardEvent
import com.example.model.RewardType
import com.example.model.TAP_TAP_COINS
import com.example.model.TAP_TAP_DURATION_SECONDS
import com.example.model.TAP_TAP_TARGET_TAPS
import com.example.model.UiState
import com.example.model.UserProfile
import com.example.model.WATCH_AD_BUNDLE_COINS
import com.example.model.WithdrawalMethod
import com.example.model.WithdrawalRequest
import com.example.model.WithdrawalStatus
import com.example.model.formatRupees
import com.example.model.goalCycleStartMillis
import com.example.model.nextGoalResetMillis
import com.example.model.streakRewardForDay
import com.example.model.todayDateString
import com.example.model.verifyAdminPasswordAndComputeProof
import com.example.model.withdrawalNetRupees
import com.google.firebase.firestore.FirebaseFirestoreException
import java.lang.ref.WeakReference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AdPurpose {
    POST_MATCH,
    TAP_TAP_COMPLETE,
    MATH_QUIZ_COMPLETE,
    MATH_QUIZ_CONTINUE,
    PENDING_MATCH_CLAIM,
    DAILY_STREAK,
    BONUS_AD,
    TEST_AD
}

data class TicTacToeUiState(
    val board: List<CellMark> = List(9) { CellMark.EMPTY },
    val isBotThinking: Boolean = false,
    val isMatchActive: Boolean = false,
    val outcome: MatchOutcome? = null,
    val winningLine: List<Int> = emptyList(),
    val currentLevelNumber: Int = 1,
    val botGoesFirst: Boolean = false,
    val autoLaunchingAd: Boolean = false,
    val lastRecordedMatchId: String? = null,
    val lastMatchAdClaimed: Boolean = false
)

data class TapTapUiState(
    val isRunning: Boolean = false,
    val secondsRemaining: Int = TAP_TAP_DURATION_SECONDS,
    val tapCount: Int = 0,
    val isFinished: Boolean = false,
    val passedGoal: Boolean = false,
    val autoLaunchingAd: Boolean = false,
    val roundNumber: Int = 1,
    val lastRecordedMatchId: String? = null,
    val lastMatchAdClaimed: Boolean = false
)

enum class MathQuizContinueReason {
    WRONG_ANSWER,
    CHECKPOINT_THREE
}

data class MathQuizUiState(
    val levelNumber: Int = 1,
    val questions: List<MathQuestion> = emptyList(),
    val currentQuestionIndex: Int = 0,
    val wrongOptionSelected: Int? = null,
    val correctOptionSelected: Int? = null,
    val mistakesInLevel: Int = 0,
    val isLevelActive: Boolean = false,
    val isLevelCompleted: Boolean = false,
    val isContinueAdRequired: Boolean = false,
    val isLoadingContinueAd: Boolean = false,
    val continueAdReason: MathQuizContinueReason? = null,
    val autoLaunchingAd: Boolean = false,
    val lastRecordedMatchId: String? = null,
    val lastMatchAdClaimed: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(
    private val repository: NexaEarnRepository,
    private val adminRepository: AdminAndLiveConfigRepository,
    private val adManager: AdManager,
    private val appContext: Context,
    private val currentUserId: String,
    initialDisplayName: String?,
    private val initialEmail: String?,
    private val onboardingPreferences: OnboardingPreferences = OnboardingPreferences(appContext),
    private val notificationPreferences: NotificationPreferences = NotificationPreferences(appContext)
) : ViewModel() {

    val adSdkInfo: StateFlow<AdSdkInfo> = adManager.sdkInfo

    val appConfig: StateFlow<AppConfig> = adminRepository.observeAppConfig()
        .catch { error ->
            if (error is CancellationException) throw error
            Log.w(TAG, "Error observing live AppConfig", error)
            emit(AppConfig())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = AppConfig()
        )

    private val _latestLiveAlert = MutableStateFlow<AdminNotification?>(null)
    val latestLiveAlert: StateFlow<AdminNotification?> = _latestLiveAlert.asStateFlow()

    private val _isAdminUnlocked = MutableStateFlow(false)
    val isAdminUnlocked: StateFlow<Boolean> = _isAdminUnlocked.asStateFlow()

    private val _isUnlockingAdmin = MutableStateFlow(false)
    val isUnlockingAdmin: StateFlow<Boolean> = _isUnlockingAdmin.asStateFlow()

    private val _adminAuthError = MutableStateFlow<String?>(null)
    val adminAuthError: StateFlow<String?> = _adminAuthError.asStateFlow()

    private val _isAdminBusy = MutableStateFlow(false)
    val isAdminBusy: StateFlow<Boolean> = _isAdminBusy.asStateFlow()

    val adminUsers: StateFlow<List<UserProfile>> = _isAdminUnlocked
        .flatMapLatest { unlocked ->
            if (unlocked) {
                adminRepository.observeAllUsersForAdmin().catch { emit(emptyList()) }
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList()
        )

    val adminWithdrawals: StateFlow<List<WithdrawalRequest>> = _isAdminUnlocked
        .flatMapLatest { unlocked ->
            if (unlocked) {
                adminRepository.observeAllWithdrawalsForAdmin().catch { emit(emptyList()) }
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList()
        )

    val adminNotifications: StateFlow<List<AdminNotification>> = _isAdminUnlocked
        .flatMapLatest { unlocked ->
            if (unlocked) {
                adminRepository.observeAllNotificationsForAdmin().catch { emit(emptyList()) }
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList()
        )

    val profileState: StateFlow<UiState<UserProfile>> = repository.observeUserProfile(currentUserId)
        .map<UserProfile?, UiState<UserProfile>> { profile ->
            if (profile != null) {
                UiState.Success(profile)
            } else {
                UiState.Loading
            }
        }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing profile", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load user profile"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val matchesState: StateFlow<UiState<List<GameMatchRecord>>> =
        repository.observeRecentMatches(currentUserId)
            .map<List<GameMatchRecord>, UiState<List<GameMatchRecord>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing matches", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load match history"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val rewardsState: StateFlow<UiState<List<RewardEvent>>> =
        repository.observeRecentRewards(currentUserId)
            .map<List<RewardEvent>, UiState<List<RewardEvent>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing rewards", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load reward ledger"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val withdrawalsState: StateFlow<UiState<List<WithdrawalRequest>>> =
        repository.observeWithdrawals(currentUserId)
            .map<List<WithdrawalRequest>, UiState<List<WithdrawalRequest>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing withdrawals", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load withdrawal requests"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val sentReferralsState: StateFlow<UiState<List<ReferralRecord>>> =
        repository.observeSentReferrals(currentUserId)
            .map<List<ReferralRecord>, UiState<List<ReferralRecord>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing referrals", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load referral progress"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val myReferralLinkState: StateFlow<UiState<ReferralRecord?>> =
        repository.observeMyReferralLink(currentUserId)
            .map<ReferralRecord?, UiState<ReferralRecord?>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing my referral link", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load referral link"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    private val _gameState = MutableStateFlow(TicTacToeUiState())
    val gameState: StateFlow<TicTacToeUiState> = _gameState.asStateFlow()

    private val _tapTapState = MutableStateFlow(TapTapUiState())
    val tapTapState: StateFlow<TapTapUiState> = _tapTapState.asStateFlow()

    private val _mathQuizState = MutableStateFlow(MathQuizUiState())
    val mathQuizState: StateFlow<MathQuizUiState> = _mathQuizState.asStateFlow()

    private val _adBlockState = MutableStateFlow(AdBlockerCheckResult(isBlocked = false))
    val adBlockState: StateFlow<AdBlockerCheckResult> = _adBlockState.asStateFlow()

    private val _isScanningNetwork = MutableStateFlow(false)
    val isScanningNetwork: StateFlow<Boolean> = _isScanningNetwork.asStateFlow()

    private val _simulateAdBlocker = MutableStateFlow(false)
    val simulateAdBlocker: StateFlow<Boolean> = _simulateAdBlocker.asStateFlow()

    private val _isSubmittingWithdrawal = MutableStateFlow(false)
    val isSubmittingWithdrawal: StateFlow<Boolean> = _isSubmittingWithdrawal.asStateFlow()

    private val _isApplyingReferral = MutableStateFlow(false)
    val isApplyingReferral: StateFlow<Boolean> = _isApplyingReferral.asStateFlow()

    private val _claimingReferralIds = MutableStateFlow<Set<String>>(emptySet())
    val claimingReferralIds: StateFlow<Set<String>> = _claimingReferralIds.asStateFlow()

    private val _isClaimingExtraEarn = MutableStateFlow(false)
    val isClaimingExtraEarn: StateFlow<Boolean> = _isClaimingExtraEarn.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _notificationSettings = MutableStateFlow(notificationPreferences.readSettings(currentUserId))
    val notificationSettings: StateFlow<NotificationSettings> = _notificationSettings.asStateFlow()

    private val _manualTutorialOpen = MutableStateFlow(false)

    private val persistedOnboardingCompleted: StateFlow<Boolean?> =
        onboardingPreferences.observeOnboardingCompleted(currentUserId)
            .map<Boolean, Boolean?> { it }
            .catch { emit(true) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = null
            )

    val showOnboardingOverlay: StateFlow<Boolean> = combine(
        persistedOnboardingCompleted,
        profileState,
        _manualTutorialOpen
    ) { completed, pState, manualOpen ->
        if (manualOpen) {
            true
        } else {
            val isNewUser = (pState as? UiState.Success)?.data?.isNewUserForOnboarding() == true
            completed == false && isNewUser
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = false
    )

    private var pendingMatchOutcome: MatchOutcome? = null
    private var pendingClaimMatchId: String? = null
    private var pendingClaimMatchCoins: Long = COINS_PER_GAME
    private val attemptedAutoClaimIds = mutableSetOf<String>()
    private var tapTapTimerJob: Job? = null
    private var mathQuizAdvanceJob: Job? = null
    private var activeTapTapActivityRef: WeakReference<Activity>? = null

    private var lastAutoClaimAttemptCycleMillis: Long = -1L
    private var isAutoClaimingOneAm: Boolean = false

    init {
        viewModelScope.launch {
            val result = repository.ensureUserProfile(initialDisplayName, initialEmail)
            if (result.isFailure) {
                _statusMessage.value =
                    result.exceptionOrNull()?.localizedMessage ?: "Failed to initialize profile."
            }
        }
        viewModelScope.launch {
            sentReferralsState.collect { state ->
                val records = (state as? UiState.Success)?.data ?: return@collect
                records
                    .filter { it.isReadyToClaim() && attemptedAutoClaimIds.add(it.inviteeUid) }
                    .forEach { readyRecord ->
                        claimReferralBonus(
                            inviteeUid = readyRecord.inviteeUid,
                            friendName = readyRecord.inviteeName
                        )
                    }
            }
        }
        viewModelScope.launch {
            profileState.collect { state ->
                val profile = (state as? UiState.Success)?.data ?: return@collect
                notificationPreferences.updateUserActivitySnapshot(
                    userId = currentUserId,
                    displayName = profile.displayName,
                    lastStreakClaimDate = profile.lastStreakClaimDate,
                    currentStreak = profile.currentStreak
                )
                NotificationScheduler.syncSchedules(
                    context = appContext,
                    preferences = notificationPreferences,
                    userIdOverride = currentUserId
                )
                if (profile.needsOneAmAutoClaim(System.currentTimeMillis())) {
                    checkOneAmGoalReset()
                }
            }
        }
        viewModelScope.launch {
            while (true) {
                val now = System.currentTimeMillis()
                val nextReset = nextGoalResetMillis(now)
                val waitMillis = (nextReset - now + 400L).coerceAtLeast(1000L)
                delay(waitMillis)
                checkOneAmGoalReset()
            }
        }
        viewModelScope.launch {
            adminRepository.observeMyNotifications(currentUserId)
                .catch { error ->
                    if (error is CancellationException) throw error
                    Log.w(TAG, "Error observing custom notifications", error)
                }
                .collect { items ->
                    val deliveredIds = notificationPreferences.getDeliveredAdminNotificationIds(currentUserId)
                    val sevenDaysAgoMillis = System.currentTimeMillis() - (7L * 24L * 60L * 60L * 1000L)
                    val unseen = items.filter { item ->
                        item.id.isNotBlank() && !deliveredIds.contains(item.id)
                    }
                    unseen.sortedBy { it.createdAt?.toDate()?.time ?: Long.MAX_VALUE }.forEach { notif ->
                        notificationPreferences.markAdminNotificationDelivered(currentUserId, notif.id)
                        val createdMillis = notif.createdAt?.toDate()?.time ?: System.currentTimeMillis()
                        if (createdMillis >= sevenDaysAgoMillis) {
                            NexaEarnNotificationHelper.showAdminCustomNotification(
                                context = appContext,
                                notificationKey = notif.id,
                                title = notif.title,
                                body = notif.body
                            )
                            _latestLiveAlert.value = notif
                        }
                    }
                }
        }
        viewModelScope.launch {
            appConfig.collect { cfg ->
                adManager.updateAdUnitIds(
                    rewardedUnitId = cfg.rewardedAdUnitId,
                    bannerUnitId = cfg.bannerAdUnitId
                )
            }
        }
        NexaEarnNotificationHelper.ensureChannels(appContext)
        recordAppOpened()
        adManager.initializeAndPreload()
        runAdBlockerScan(forceRefresh = true)
    }

    fun dismissLatestLiveAlert() {
        _latestLiveAlert.value = null
    }

    fun unlockAdminPanel(password: String) {
        if (_isUnlockingAdmin.value) return
        val proof = verifyAdminPasswordAndComputeProof(password)
        if (proof == null) {
            _adminAuthError.value = "Incorrect administrator password."
            return
        }
        viewModelScope.launch {
            _isUnlockingAdmin.value = true
            _adminAuthError.value = null
            val res = adminRepository.claimAdminAccess(proof)
            _isUnlockingAdmin.value = false
            res.onSuccess {
                _isAdminUnlocked.value = true
                _statusMessage.value = "Admin Console unlocked with live Firestore sync."
            }.onFailure { err ->
                _adminAuthError.value = err.localizedMessage ?: "Failed to authorize admin session."
            }
        }
    }

    fun lockAdminPanel() {
        _isAdminUnlocked.value = false
        _adminAuthError.value = null
        viewModelScope.launch {
            adminRepository.revokeAdminAccess()
        }
        _statusMessage.value = "Admin Console locked."
    }

    fun adminAdjustUserCoins(targetUid: String, deltaCoins: Long, note: String) {
        if (_isAdminBusy.value) return
        viewModelScope.launch {
            _isAdminBusy.value = true
            val res = adminRepository.adjustUserCoins(
                targetUid = targetUid,
                deltaCoins = deltaCoins,
                note = note,
                adminEmail = initialEmail
            )
            _isAdminBusy.value = false
            res.onSuccess { updatedBalance ->
                val action = if (deltaCoins > 0) "Added +$deltaCoins" else "Subtracted ${-deltaCoins}"
                _statusMessage.value = "$action coins! Updated balance: $updatedBalance coins."
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "Failed to update user coins."
            }
        }
    }

    fun adminSetUserAccessStatus(
        targetUid: String,
        banned: Boolean,
        whitelisted: Boolean,
        reason: String? = null
    ) {
        if (_isAdminBusy.value) return
        viewModelScope.launch {
            _isAdminBusy.value = true
            val res = adminRepository.setUserAccessStatus(
                targetUid = targetUid,
                banned = banned,
                whitelisted = whitelisted,
                reason = reason,
                adminEmail = initialEmail
            )
            _isAdminBusy.value = false
            res.onSuccess {
                _statusMessage.value = when {
                    banned -> "User has been banned / blacklisted."
                    whitelisted -> "User has been unbanned & whitelisted."
                    else -> "User status restored to active."
                }
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "Failed to update user status."
            }
        }
    }

    fun adminUpdateWithdrawalStatus(
        withdrawal: WithdrawalRequest,
        newStatus: WithdrawalStatus,
        refundCoinsOnFail: Boolean,
        adminNote: String
    ) {
        if (_isAdminBusy.value) return
        viewModelScope.launch {
            _isAdminBusy.value = true
            val res = adminRepository.updateWithdrawalStatus(
                userId = withdrawal.userId,
                withdrawalId = withdrawal.id,
                newStatus = newStatus,
                refundCoinsOnFail = refundCoinsOnFail,
                adminNote = adminNote,
                adminEmail = initialEmail
            )
            _isAdminBusy.value = false
            res.onSuccess {
                _statusMessage.value = "Request ${withdrawal.shortTransactionId()} marked ${newStatus.label}."
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "Failed to update request status."
            }
        }
    }

    fun adminSendCustomNotification(
        title: String,
        body: String,
        targetUid: String,
        targetEmail: String?
    ) {
        if (_isAdminBusy.value) return
        viewModelScope.launch {
            _isAdminBusy.value = true
            val res = adminRepository.sendCustomNotification(
                title = title,
                body = body,
                targetUid = targetUid,
                targetEmail = targetEmail,
                sentByEmail = initialEmail
            )
            _isAdminBusy.value = false
            res.onSuccess {
                _statusMessage.value = if (targetUid == "ALL") {
                    "Live push notification sent to ALL users!"
                } else {
                    "Live push notification sent to ${targetEmail ?: targetUid}!"
                }
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "Failed to send notification."
            }
        }
    }

    fun adminDeleteNotification(notificationId: String) {
        viewModelScope.launch {
            val res = adminRepository.deleteNotification(notificationId)
            res.onSuccess {
                _statusMessage.value = "Notification removed."
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "Failed to remove notification."
            }
        }
    }

    fun adminSaveAppConfig(config: AppConfig) {
        if (_isAdminBusy.value) return
        viewModelScope.launch {
            _isAdminBusy.value = true
            val res = adminRepository.saveAppConfig(config, adminEmail = initialEmail)
            _isAdminBusy.value = false
            res.onSuccess {
                _statusMessage.value = "Live App Configuration saved & synced to all users!"
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "Failed to save AppConfig."
            }
        }
    }

    fun recordAppOpened() {
        notificationPreferences.recordAppOpen(currentUserId)
        NotificationScheduler.syncSchedules(
            context = appContext,
            preferences = notificationPreferences,
            userIdOverride = currentUserId
        )
    }

    fun setMasterNotificationsEnabled(enabled: Boolean) {
        updateNotificationPrefs { it.copy(masterEnabled = enabled) }
    }

    fun setInactivityStreakNotificationsEnabled(enabled: Boolean) {
        updateNotificationPrefs { it.copy(inactivityStreakEnabled = enabled) }
    }

    fun setMotivationalNotificationsEnabled(enabled: Boolean) {
        updateNotificationPrefs { it.copy(motivationalEnabled = enabled) }
    }

    fun setEarningAlertsEnabled(enabled: Boolean) {
        updateNotificationPrefs { it.copy(earningAlertsEnabled = enabled) }
    }

    fun setReminderIntervalHours(hours: Int) {
        updateNotificationPrefs { it.copy(reminderDelayHours = hours) }
    }

    fun sendTestNotification() {
        val posted = NexaEarnNotificationHelper.showTestNotification(appContext)
        _statusMessage.value = if (posted) {
            "Test push notification sent!"
        } else {
            "Notification permission is disabled in Android Settings."
        }
    }

    private fun updateNotificationPrefs(transform: (NotificationSettings) -> NotificationSettings) {
        val updated = notificationPreferences.updateSettings(currentUserId, transform)
        _notificationSettings.value = updated
        NotificationScheduler.syncSchedules(
            context = appContext,
            preferences = notificationPreferences,
            userIdOverride = currentUserId
        )
    }

    private fun notifyCoinEarning(title: String, body: String) {
        if (!_notificationSettings.value.isEarningAlertsActive()) return
        NexaEarnNotificationHelper.showEarningNotification(
            context = appContext,
            title = title,
            body = body
        )
    }

    fun consumeStatusMessage() {
        _statusMessage.value = null
    }

    fun showStatusMessage(message: String) {
        _statusMessage.value = message
    }

    fun checkOneAmGoalReset() {
        val profile = (profileState.value as? UiState.Success)?.data ?: return
        val nowMillis = System.currentTimeMillis()
        val currentCycle = goalCycleStartMillis(nowMillis)
        if (!profile.needsOneAmAutoClaim(nowMillis)) return
        if (isAutoClaimingOneAm || _isClaimingExtraEarn.value || lastAutoClaimAttemptCycleMillis == currentCycle) {
            return
        }

        lastAutoClaimAttemptCycleMillis = currentCycle
        viewModelScope.launch {
            isAutoClaimingOneAm = true
            _isClaimingExtraEarn.value = true
            val result = repository.autoClaimExtraEarnOnOneAmReset()
            _isClaimingExtraEarn.value = false
            isAutoClaimingOneAm = false
            result.onSuccess { coins ->
                if (coins > 0L) {
                    _statusMessage.value =
                        "1:00 AM Reset: Auto-claimed +$coins whole ${if (coins == 1L) "coin" else "coins"} from Extra Earn!"
                    notifyCoinEarning(
                        title = "+$coins Extra Earn Coins Auto-Claimed!",
                        body = "1:00 AM Goal Reset credited +$coins whole ${if (coins == 1L) "coin" else "coins"} to your wallet."
                    )
                }
            }.onFailure { err ->
                lastAutoClaimAttemptCycleMillis = -1L
                Log.w(TAG, "1:00 AM auto-claim deferred/failed", err)
            }
        }
    }

    fun claimExtraEarnPot() {
        if (_isClaimingExtraEarn.value) return
        viewModelScope.launch {
            _isClaimingExtraEarn.value = true
            val result = repository.claimExtraEarnPot()
            _isClaimingExtraEarn.value = false
            result.onSuccess { coins ->
                _statusMessage.value =
                    "Extra Earn claimed! +$coins whole ${if (coins == 1L) "coin" else "coins"} added to your wallet."
                notifyCoinEarning(
                    title = "+$coins Extra Earn Coins Claimed!",
                    body = "Your +$coins Extra Earn pot bonus has been added to your NexaEarn wallet."
                )
            }.onFailure { err ->
                _statusMessage.value =
                    err.localizedMessage ?: "Could not claim Extra Earn coins."
            }
        }
    }

    fun applyReferralCode(code: String, onSuccess: () -> Unit = {}) {
        if (_isApplyingReferral.value) return
        viewModelScope.launch {
            _isApplyingReferral.value = true
            val result = repository.applyReferralCode(code)
            _isApplyingReferral.value = false
            result.onSuccess { referrerName ->
                _statusMessage.value =
                    "Code applied! Complete $REFERRAL_TARGET_GAMES games to unlock your friend's bonus for $referrerName."
                onSuccess()
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "Could not apply referral code."
            }
        }
    }

    fun claimReferralBonus(inviteeUid: String, friendName: String = "your friend") {
        if (inviteeUid.isBlank() || _claimingReferralIds.value.contains(inviteeUid)) return
        viewModelScope.launch {
            _claimingReferralIds.update { it + inviteeUid }
            val result = repository.claimReferralBonus(inviteeUid)
            _claimingReferralIds.update { it - inviteeUid }
            result.onSuccess { coins ->
                _statusMessage.value =
                    "Referral bonus claimed! +$coins coins added ($friendName completed $REFERRAL_TARGET_GAMES games)."
                notifyCoinEarning(
                    title = "+$coins Referral Bonus Earned!",
                    body = "$friendName completed $REFERRAL_TARGET_GAMES games and unlocked +$coins coins for you."
                )
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "Failed to claim referral bonus."
            }
        }
    }

    fun openOnboardingTutorial() {
        _manualTutorialOpen.value = true
    }

    fun dismissOnboardingTutorial() {
        _manualTutorialOpen.value = false
        viewModelScope.launch {
            onboardingPreferences.setOnboardingCompleted(currentUserId, true)
        }
    }

    fun submitWithdrawal(
        method: WithdrawalMethod,
        accountTitle: String,
        destination: String,
        coins: Long,
        onSuccess: () -> Unit
    ) {
        if (_isSubmittingWithdrawal.value) return
        viewModelScope.launch {
            _isSubmittingWithdrawal.value = true
            val result = repository.submitWithdrawalRequest(
                method = method,
                accountTitle = accountTitle,
                destination = destination,
                coins = coins
            )
            _isSubmittingWithdrawal.value = false
            result.onSuccess {
                val netRs = formatRupees(withdrawalNetRupees(method, coins))
                _statusMessage.value = "Withdrawal submitted for $coins coins ($netRs) via ${method.label}. Withdraw time: 10 minutes to 24 hours."
                onSuccess()
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "Could not submit withdrawal request."
            }
        }
    }

    fun runAdBlockerScan(forceRefresh: Boolean = true) {
        viewModelScope.launch {
            if (forceRefresh) {
                _isScanningNetwork.value = true
            }
            val result = AdBlockerDetector.detect(
                context = appContext,
                simulateAdBlocker = _simulateAdBlocker.value,
                forceRefresh = forceRefresh
            )
            _adBlockState.value = result
            _isScanningNetwork.value = false
        }
    }

    fun toggleSimulateAdBlocker(enabled: Boolean) {
        _simulateAdBlocker.value = enabled
        AdBlockerDetector.invalidateCache()
        runAdBlockerScan(forceRefresh = true)
    }

    fun refreshTestAdSdk() {
        adManager.preloadAllGoogleAdMobAds()
        _statusMessage.value = "Reloading all Google AdMob ads..."
    }

    fun startNewMatch(activity: Activity?) {
        val profile = (profileState.value as? UiState.Success)?.data
        if (profile == null) {
            _statusMessage.value = "Syncing your profile, please wait a moment."
            return
        }
        if (_simulateAdBlocker.value || _adBlockState.value.isBlocked) {
            runAdBlockerScan(forceRefresh = true)
            return
        }

        viewModelScope.launch {
            // Non-blocking background check (uses 30s cache for instant 0ms game start)
            val scan = AdBlockerDetector.detect(appContext, _simulateAdBlocker.value, forceRefresh = false)
            _adBlockState.value = scan
            if (scan.isBlocked) {
                return@launch
            }

            val today = todayDateString()
            val playedToday = profile.effectiveDailyGamesPlayed(today).toInt()
            val nextLevel = playedToday + 1
            val botFirst = (nextLevel % 2 == 0)
            pendingMatchOutcome = null
            _gameState.value = TicTacToeUiState(
                board = List(9) { CellMark.EMPTY },
                isBotThinking = botFirst,
                isMatchActive = true,
                outcome = null,
                winningLine = emptyList(),
                currentLevelNumber = nextLevel,
                botGoesFirst = botFirst,
                autoLaunchingAd = false,
                lastRecordedMatchId = null,
                lastMatchAdClaimed = false
            )
            adManager.preloadMissingAds()

            if (botFirst) {
                delay(220L)
                val initialBoard = _gameState.value.board.toMutableList()
                val move = TicTacToeEngine.chooseMediumBotMove(initialBoard)
                if (move in 0..8) {
                    initialBoard[move] = CellMark.BOT_O
                }
                _gameState.update {
                    it.copy(board = initialBoard, isBotThinking = false)
                }
            }
        }
    }

    fun onCellTapped(index: Int, activity: Activity?) {
        val state = _gameState.value
        if (!state.isMatchActive || state.isBotThinking || state.outcome != null) return
        if (index !in 0..8 || state.board[index] != CellMark.EMPTY) return

        val boardAfterPlayer = state.board.toMutableList().apply {
            this[index] = CellMark.PLAYER_X
        }
        val playerEval = TicTacToeEngine.evaluate(boardAfterPlayer)
        if (playerEval.outcome != null) {
            handleMatchFinished(
                finalBoard = boardAfterPlayer,
                outcome = playerEval.outcome,
                winningLine = playerEval.winningLine,
                activity = activity
            )
            return
        }

        _gameState.update {
            it.copy(board = boardAfterPlayer, isBotThinking = true)
        }

        viewModelScope.launch {
            delay(220L)
            val current = _gameState.value
            if (!current.isMatchActive || current.outcome != null) return@launch
            val botMove = TicTacToeEngine.chooseMediumBotMove(current.board)
            val boardAfterBot = current.board.toMutableList()
            if (botMove in 0..8) {
                boardAfterBot[botMove] = CellMark.BOT_O
            }
            val botEval = TicTacToeEngine.evaluate(boardAfterBot)
            if (botEval.outcome != null) {
                handleMatchFinished(
                    finalBoard = boardAfterBot,
                    outcome = botEval.outcome,
                    winningLine = botEval.winningLine,
                    activity = activity
                )
            } else {
                _gameState.update {
                    it.copy(board = boardAfterBot, isBotThinking = false)
                }
            }
        }
    }

    private fun handleMatchFinished(
        finalBoard: List<CellMark>,
        outcome: MatchOutcome,
        winningLine: List<Int>,
        activity: Activity?
    ) {
        pendingMatchOutcome = outcome
        _gameState.update {
            it.copy(
                board = finalBoard,
                isBotThinking = false,
                isMatchActive = false,
                outcome = outcome,
                winningLine = winningLine,
                autoLaunchingAd = true
            )
        }

        viewModelScope.launch {
            delay(320L)
            triggerRewardedAd(
                purpose = AdPurpose.POST_MATCH,
                activity = activity
            )
        }
    }

    fun startTapTapRound(activity: Activity?) {
        if (_simulateAdBlocker.value || _adBlockState.value.isBlocked) {
            runAdBlockerScan(forceRefresh = true)
            return
        }
        tapTapTimerJob?.cancel()
        activeTapTapActivityRef = WeakReference(activity)
        val currentRound = if (_tapTapState.value.lastMatchAdClaimed || _tapTapState.value.lastRecordedMatchId != null) {
            _tapTapState.value.roundNumber + 1
        } else {
            _tapTapState.value.roundNumber
        }
        _tapTapState.value = TapTapUiState(
            isRunning = true,
            secondsRemaining = TAP_TAP_DURATION_SECONDS,
            tapCount = 0,
            isFinished = false,
            passedGoal = false,
            autoLaunchingAd = false,
            roundNumber = currentRound,
            lastRecordedMatchId = null,
            lastMatchAdClaimed = false
        )
        adManager.preloadMissingAds()

        tapTapTimerJob = viewModelScope.launch {
            for (sec in (TAP_TAP_DURATION_SECONDS - 1) downTo 0) {
                delay(1000L)
                _tapTapState.update { it.copy(secondsRemaining = sec) }
            }
            finishTapTapRound()
        }
    }

    fun onTapTapPressed() {
        _tapTapState.update { state ->
            if (!state.isRunning || state.isFinished) state
            else state.copy(tapCount = state.tapCount + 1)
        }
    }

    fun cancelTapTapIfRunning() {
        if (_tapTapState.value.isRunning) {
            tapTapTimerJob?.cancel()
            tapTapTimerJob = null
            _tapTapState.update {
                it.copy(
                    isRunning = false,
                    secondsRemaining = TAP_TAP_DURATION_SECONDS,
                    tapCount = 0,
                    isFinished = false,
                    passedGoal = false,
                    autoLaunchingAd = false
                )
            }
        }
    }

    private fun finishTapTapRound() {
        val current = _tapTapState.value
        val passed = MathQuizEngine.isTapTapGoalReached(current.tapCount)
        _tapTapState.update {
            it.copy(
                isRunning = false,
                secondsRemaining = 0,
                isFinished = true,
                passedGoal = passed,
                autoLaunchingAd = passed
            )
        }
        if (passed) {
            viewModelScope.launch {
                delay(320L)
                triggerRewardedAd(
                    purpose = AdPurpose.TAP_TAP_COMPLETE,
                    activity = activeTapTapActivityRef?.get()
                )
            }
        } else {
            _statusMessage.value = "You reached ${current.tapCount}/$TAP_TAP_TARGET_TAPS taps. Reach $TAP_TAP_TARGET_TAPS in 30s to unlock the AdMob ad!"
        }
    }

    fun claimPendingTapTapAd(activity: Activity?) {
        val matchId = _tapTapState.value.lastRecordedMatchId ?: return
        if (_tapTapState.value.lastMatchAdClaimed) return
        claimMatchRewardAd(matchId, activity)
    }

    fun startMathQuizLevel(activity: Activity?) {
        if (_simulateAdBlocker.value || _adBlockState.value.isBlocked) {
            runAdBlockerScan(forceRefresh = true)
            return
        }
        mathQuizAdvanceJob?.cancel()
        val prev = _mathQuizState.value
        val nextLevel = if (prev.isLevelCompleted) prev.levelNumber + 1 else prev.levelNumber
        val generated = MathQuizEngine.generateLevelQuestions(levelNumber = nextLevel)
        _mathQuizState.value = MathQuizUiState(
            levelNumber = nextLevel,
            questions = generated,
            currentQuestionIndex = 0,
            wrongOptionSelected = null,
            correctOptionSelected = null,
            mistakesInLevel = 0,
            isLevelActive = true,
            isLevelCompleted = false,
            isContinueAdRequired = false,
            isLoadingContinueAd = false,
            continueAdReason = null,
            autoLaunchingAd = false,
            lastRecordedMatchId = null,
            lastMatchAdClaimed = false
        )
        adManager.preloadMissingAds()
    }

    fun onMathOptionSelected(option: Int, activity: Activity?) {
        val state = _mathQuizState.value
        if (!state.isLevelActive ||
            state.isLevelCompleted ||
            state.correctOptionSelected != null ||
            state.isContinueAdRequired ||
            state.isLoadingContinueAd
        ) return
        val question = state.questions.getOrNull(state.currentQuestionIndex) ?: return

        if (option != question.correctAnswer) {
            _mathQuizState.update {
                it.copy(
                    wrongOptionSelected = option,
                    mistakesInLevel = it.mistakesInLevel + 1,
                    isContinueAdRequired = true,
                    isLoadingContinueAd = true,
                    continueAdReason = MathQuizContinueReason.WRONG_ANSWER
                )
            }
            mathQuizAdvanceJob?.cancel()
            mathQuizAdvanceJob = viewModelScope.launch {
                delay(300L)
                triggerRewardedAd(
                    purpose = AdPurpose.MATH_QUIZ_CONTINUE,
                    activity = activity
                )
            }
            return
        }

        _mathQuizState.update {
            it.copy(
                correctOptionSelected = option,
                wrongOptionSelected = null
            )
        }

        mathQuizAdvanceJob?.cancel()
        mathQuizAdvanceJob = viewModelScope.launch {
            delay(180L)
            val current = _mathQuizState.value
            val nextIdx = current.currentQuestionIndex + 1
            if (nextIdx < current.questions.size) {
                val hitThreeQuestionCheckpoint = nextIdx == MATH_QUIZ_CHECKPOINT_QUESTIONS
                _mathQuizState.update {
                    it.copy(
                        currentQuestionIndex = nextIdx,
                        correctOptionSelected = null,
                        wrongOptionSelected = null,
                        isContinueAdRequired = hitThreeQuestionCheckpoint,
                        isLoadingContinueAd = hitThreeQuestionCheckpoint,
                        continueAdReason = if (hitThreeQuestionCheckpoint) {
                            MathQuizContinueReason.CHECKPOINT_THREE
                        } else {
                            null
                        }
                    )
                }
                if (hitThreeQuestionCheckpoint) {
                    delay(220L)
                    triggerRewardedAd(
                        purpose = AdPurpose.MATH_QUIZ_CONTINUE,
                        activity = activity
                    )
                }
            } else {
                _mathQuizState.update {
                    it.copy(
                        isLevelActive = false,
                        isLevelCompleted = true,
                        correctOptionSelected = null,
                        wrongOptionSelected = null,
                        isContinueAdRequired = false,
                        isLoadingContinueAd = false,
                        continueAdReason = null,
                        autoLaunchingAd = true
                    )
                }
                delay(320L)
                triggerRewardedAd(
                    purpose = AdPurpose.MATH_QUIZ_COMPLETE,
                    activity = activity
                )
            }
        }
    }

    fun retryMathQuizContinueAd(activity: Activity?) {
        val state = _mathQuizState.value
        if (!state.isLevelActive || !state.isContinueAdRequired || state.isLoadingContinueAd) return
        _mathQuizState.update { it.copy(isLoadingContinueAd = true) }
        triggerRewardedAd(
            purpose = AdPurpose.MATH_QUIZ_CONTINUE,
            activity = activity
        )
    }

    fun claimPendingMathQuizAd(activity: Activity?) {
        val matchId = _mathQuizState.value.lastRecordedMatchId ?: return
        if (_mathQuizState.value.lastMatchAdClaimed) return
        claimMatchRewardAd(matchId, activity)
    }

    fun claimPendingMatchRewardAd(activity: Activity?) {
        val matchId = _gameState.value.lastRecordedMatchId ?: return
        if (_gameState.value.lastMatchAdClaimed) return
        claimMatchRewardAd(matchId, activity)
    }

    fun claimMatchRewardAd(matchId: String, activity: Activity?) {
        if (matchId.isBlank()) return
        val existingMatch = (matchesState.value as? UiState.Success)?.data
            ?.firstOrNull { it.id == matchId }
        pendingClaimMatchCoins = existingMatch?.rewardCoinsForGame()
            ?: when {
                _mathQuizState.value.lastRecordedMatchId == matchId -> MATH_QUIZ_COINS
                else -> COINS_PER_GAME
            }
        pendingClaimMatchId = matchId
        triggerRewardedAd(
            purpose = AdPurpose.PENDING_MATCH_CLAIM,
            activity = activity
        )
    }

    fun claimDailyStreakWithAd(activity: Activity?) {
        val profile = (profileState.value as? UiState.Success)?.data ?: return
        if (profile.hasClaimedStreakToday()) {
            _statusMessage.value = "Daily streak reward already claimed today!"
            return
        }
        triggerRewardedAd(
            purpose = AdPurpose.DAILY_STREAK,
            activity = activity
        )
    }

    fun watchBonusAd(activity: Activity?) {
        triggerRewardedAd(
            purpose = AdPurpose.BONUS_AD,
            activity = activity
        )
    }

    fun launchGoogleRewardedAdTest(activity: Activity?) {
        triggerRewardedAd(
            purpose = AdPurpose.TEST_AD,
            activity = activity
        )
    }

    fun launchRewardedInterstitialAdTest(activity: Activity?) {
        viewModelScope.launch {
            val scan = AdBlockerDetector.detect(appContext, _simulateAdBlocker.value, forceRefresh = false)
            _adBlockState.value = scan
            if (scan.isBlocked) return@launch

            val shown = adManager.showAdMobRewardedInterstitialOnly(
                activity = activity,
                onRewardEarned = { source ->
                    onRewardedAdCompleted(AdPurpose.TEST_AD, source)
                },
                onDismissedWithoutReward = {
                    onRewardedAdSkipped(AdPurpose.TEST_AD)
                }
            )
            if (!shown) {
                adManager.preloadRewardedInterstitialAd()
                _statusMessage.value = "Google AdMob Rewarded Interstitial ad is not available right now — no coins added."
            }
        }
    }

    fun launchInterstitialTestAd(activity: Activity?) {
        viewModelScope.launch {
            val scan = AdBlockerDetector.detect(appContext, _simulateAdBlocker.value, forceRefresh = false)
            _adBlockState.value = scan
            if (scan.isBlocked) return@launch

            val shown = adManager.showLoadedInterstitialAd(activity) {
                _statusMessage.value = "Google AdMob Interstitial Ad completed!"
            }
            if (!shown) {
                adManager.preloadInterstitialAd()
                _statusMessage.value = "Google AdMob Interstitial Ad is not available yet — preloading now."
            }
        }
    }

    fun openAdInspector(activity: Activity?) {
        adManager.openAdInspector(activity) { msg ->
            _statusMessage.value = msg
        }
    }

    private fun clearAutoLaunchingFlags() {
        _gameState.update { if (it.autoLaunchingAd) it.copy(autoLaunchingAd = false) else it }
        _tapTapState.update { if (it.autoLaunchingAd) it.copy(autoLaunchingAd = false) else it }
        _mathQuizState.update {
            if (it.autoLaunchingAd || it.isLoadingContinueAd) {
                it.copy(autoLaunchingAd = false, isLoadingContinueAd = false)
            } else {
                it
            }
        }
    }

    private fun isCurrentUserSuspended(): Boolean {
        val profile = (profileState.value as? UiState.Success)?.data ?: return false
        return profile.banned && !_isAdminUnlocked.value
    }

    private fun triggerRewardedAd(
        purpose: AdPurpose,
        activity: Activity?
    ) {
        if (isCurrentUserSuspended()) {
            clearAutoLaunchingFlags()
            _statusMessage.value = "Account suspended. Contact Customer Support via WhatsApp."
            return
        }
        viewModelScope.launch {
            val scan = AdBlockerDetector.detect(appContext, _simulateAdBlocker.value, forceRefresh = false)
            _adBlockState.value = scan

            if (scan.isBlocked) {
                clearAutoLaunchingFlags()
                when (purpose) {
                    AdPurpose.POST_MATCH -> persistSkippedPostMatch(adUnavailable = true)
                    AdPurpose.TAP_TAP_COMPLETE -> persistSkippedTapTapMatch(adUnavailable = true)
                    AdPurpose.MATH_QUIZ_COMPLETE -> persistSkippedMathQuizMatch(adUnavailable = true)
                    AdPurpose.MATH_QUIZ_CONTINUE -> {
                        _statusMessage.value = "Ad blocker active — Google AdMob ad unavailable to continue quiz."
                    }
                    else -> _statusMessage.value = "Ad blocker active — Google AdMob ad unavailable, no coins added."
                }
                return@launch
            }

            if (!adManager.hasAnyRewardedAd()) {
                adManager.ensureRewardedAdLoadedQuickly(timeoutMillis = 900L)
            }
            clearAutoLaunchingFlags()

            val shownGoogleAd = adManager.showAdMobRewardedAd(
                activity = activity,
                onRewardEarned = { source ->
                    onRewardedAdCompleted(purpose, source)
                },
                onDismissedWithoutReward = {
                    onRewardedAdSkipped(purpose)
                }
            )
            if (!shownGoogleAd) {
                adManager.preloadAllGoogleAdMobAds()
                when (purpose) {
                    AdPurpose.POST_MATCH -> persistSkippedPostMatch(adUnavailable = true)
                    AdPurpose.TAP_TAP_COMPLETE -> persistSkippedTapTapMatch(adUnavailable = true)
                    AdPurpose.MATH_QUIZ_COMPLETE -> persistSkippedMathQuizMatch(adUnavailable = true)
                    AdPurpose.MATH_QUIZ_CONTINUE -> {
                        _statusMessage.value = "Google AdMob ad is loading — tap 'Watch Rewarded Ad to Continue' to resume."
                    }
                    else -> {
                        _statusMessage.value = "Google AdMob ad is not available right now — no coins added. Please try again in a moment."
                    }
                }
            }
        }
    }

    private fun onRewardedAdCompleted(purpose: AdPurpose, adSource: AdSource) {
        viewModelScope.launch {
            when (purpose) {
                AdPurpose.POST_MATCH -> {
                    val outcome = pendingMatchOutcome ?: _gameState.value.outcome ?: MatchOutcome.DRAW
                    val res = repository.recordCompletedMatch(
                        outcome = outcome,
                        adWatched = true,
                        adSource = adSource,
                        gameType = MiniGameType.TIC_TAC_TOE
                    )
                    res.onSuccess { matchId ->
                        val level = _gameState.value.currentLevelNumber
                        _gameState.update {
                            it.copy(
                                lastRecordedMatchId = matchId,
                                lastMatchAdClaimed = true
                            )
                        }
                        _statusMessage.value = "+$COINS_PER_GAME coins earned for Level $level!"
                        notifyCoinEarning(
                            title = "+$COINS_PER_GAME Coins Earned (Tic Tac Toe)!",
                            body = "Level $level completed and AdMob ad watched. Keep playing to hit your 100-coin goal!"
                        )
                    }.onFailure { err ->
                        _statusMessage.value = err.localizedMessage ?: "Failed to save match reward."
                    }
                }
                AdPurpose.TAP_TAP_COMPLETE -> {
                    val round = _tapTapState.value.roundNumber.toLong()
                    val taps = _tapTapState.value.tapCount
                    val res = repository.recordCompletedMatch(
                        outcome = MatchOutcome.WIN,
                        adWatched = true,
                        adSource = adSource,
                        gameType = MiniGameType.TAP_TAP,
                        customLevelNumber = round
                    )
                    res.onSuccess { matchId ->
                        _tapTapState.update {
                            it.copy(
                                lastRecordedMatchId = matchId,
                                lastMatchAdClaimed = true
                            )
                        }
                        _statusMessage.value = "+$TAP_TAP_COINS coins earned for Tap Tap 30s ($taps taps)!"
                        notifyCoinEarning(
                            title = "+$TAP_TAP_COINS Coins Earned (Tap Tap 30s)!",
                            body = "Awesome $taps taps in Round $round! +$TAP_TAP_COINS coins added to your NexaEarn wallet."
                        )
                    }.onFailure { err ->
                        _statusMessage.value = err.localizedMessage ?: "Failed to save Tap Tap reward."
                    }
                }
                AdPurpose.MATH_QUIZ_COMPLETE -> {
                    val level = _mathQuizState.value.levelNumber.toLong()
                    val res = repository.recordCompletedMatch(
                        outcome = MatchOutcome.WIN,
                        adWatched = true,
                        adSource = adSource,
                        gameType = MiniGameType.MATH_QUIZ,
                        customLevelNumber = level
                    )
                    res.onSuccess { matchId ->
                        _mathQuizState.update {
                            it.copy(
                                lastRecordedMatchId = matchId,
                                lastMatchAdClaimed = true
                            )
                        }
                        _statusMessage.value = "+$MATH_QUIZ_COINS coins earned for Math Quiz Level $level!"
                        notifyCoinEarning(
                            title = "+$MATH_QUIZ_COINS Coins Earned (Math Quiz)!",
                            body = "Level $level cleared! +$MATH_QUIZ_COINS coins credited after watching your AdMob ad."
                        )
                    }.onFailure { err ->
                        _statusMessage.value = err.localizedMessage ?: "Failed to save Math Quiz reward."
                    }
                }
                AdPurpose.MATH_QUIZ_CONTINUE -> {
                    val reason = _mathQuizState.value.continueAdReason
                    _mathQuizState.update {
                        it.copy(
                            wrongOptionSelected = null,
                            isContinueAdRequired = false,
                            isLoadingContinueAd = false,
                            continueAdReason = null
                        )
                    }
                    val qNum = _mathQuizState.value.currentQuestionIndex + 1
                    val levelNum = _mathQuizState.value.levelNumber
                    _statusMessage.value = if (reason == MathQuizContinueReason.CHECKPOINT_THREE) {
                        "3/5 checkpoint cleared! Complete Questions 4 & 5 to earn +$MATH_QUIZ_COINS coins in Level $levelNum."
                    } else {
                        "Continue unlocked! Solve Question $qNum to complete Level $levelNum."
                    }
                }
                AdPurpose.PENDING_MATCH_CLAIM -> {
                    val matchId = pendingClaimMatchId ?: _gameState.value.lastRecordedMatchId ?: return@launch
                    pendingClaimMatchId = null
                    val res = repository.claimPendingMatchAd(matchId, adSource)
                    res.onSuccess { awardedCoins ->
                        if (_gameState.value.lastRecordedMatchId == matchId) {
                            _gameState.update { it.copy(lastMatchAdClaimed = true) }
                        }
                        if (_tapTapState.value.lastRecordedMatchId == matchId) {
                            _tapTapState.update { it.copy(lastMatchAdClaimed = true) }
                        }
                        if (_mathQuizState.value.lastRecordedMatchId == matchId) {
                            _mathQuizState.update { it.copy(lastMatchAdClaimed = true) }
                        }
                        _statusMessage.value = "+$awardedCoins coins unlocked for your game!"
                        notifyCoinEarning(
                            title = "+$awardedCoins Coins Unlocked (Mini Game)!",
                            body = "Rewarded AdMob ad finished — +$awardedCoins coins have been added to your wallet."
                        )
                    }.onFailure { err ->
                        _statusMessage.value = err.localizedMessage ?: "Failed to claim match reward."
                    }
                }
                AdPurpose.DAILY_STREAK -> {
                    val res = repository.claimDailyStreakAd(adSource)
                    res.onSuccess { coins ->
                        _statusMessage.value = "Daily Streak claimed! +$coins coins added to wallet."
                        notifyCoinEarning(
                            title = "Daily Streak Claimed: +$coins Coins!",
                            body = "Your Daily Streak reward of +$coins coins is now in your wallet. Come back tomorrow to keep building!"
                        )
                    }.onFailure { err ->
                        _statusMessage.value = err.localizedMessage ?: "Failed to claim daily streak."
                    }
                }
                AdPurpose.BONUS_AD -> {
                    val res = repository.recordWatchAdBundleStep(adSource)
                    res.onSuccess { bundle ->
                        if (bundle.coinsAwarded > 0L) {
                            _statusMessage.value =
                                "3/3 Google AdMob ads watched! +${bundle.coinsAwarded} coins added (unlimited cycles!)."
                            notifyCoinEarning(
                                title = "+${bundle.coinsAwarded} Coins Earned (3/3 Ads Watched)!",
                                body = "All 3 Google AdMob ads completed! +${bundle.coinsAwarded} coins added toward your wallet. Watch 3 more anytime!"
                            )
                        } else {
                            val remaining = bundle.targetAdsInCycle - bundle.adsCompletedInCycle
                            val adWord = if (remaining == 1L) "ad" else "ads"
                            _statusMessage.value =
                                "Ad ${bundle.adsCompletedInCycle}/${bundle.targetAdsInCycle} watched! Watch $remaining more $adWord to earn +$WATCH_AD_BUNDLE_COINS coins."
                        }
                    }.onFailure { err ->
                        _statusMessage.value = err.localizedMessage ?: "Failed to record watched ad."
                    }
                }
                AdPurpose.TEST_AD -> {
                    val res = repository.recordBonusOrTestAdReward(
                        rewardType = RewardType.TEST_AD,
                        adSource = adSource,
                        note = "Test Ad verification completed (${adSource.name})"
                    )
                    res.onSuccess { coins ->
                        _statusMessage.value = "Test Ad passed (${adSource.name})! +$coins test coins added."
                        notifyCoinEarning(
                            title = "+$coins Coins Earned (Google AdMob)!",
                            body = "Google AdMob reward verified (+$coins coins credited to your wallet)."
                        )
                    }.onFailure { err ->
                        _statusMessage.value = err.localizedMessage ?: "Failed to record test ad."
                    }
                }
            }
        }
    }

    private fun onRewardedAdSkipped(purpose: AdPurpose) {
        when (purpose) {
            AdPurpose.POST_MATCH -> {
                viewModelScope.launch {
                    persistSkippedPostMatch()
                }
            }
            AdPurpose.TAP_TAP_COMPLETE -> {
                viewModelScope.launch {
                    persistSkippedTapTapMatch()
                }
            }
            AdPurpose.MATH_QUIZ_COMPLETE -> {
                viewModelScope.launch {
                    persistSkippedMathQuizMatch()
                }
            }
            AdPurpose.MATH_QUIZ_CONTINUE -> {
                _mathQuizState.update {
                    it.copy(
                        isContinueAdRequired = true,
                        isLoadingContinueAd = false
                    )
                }
                _statusMessage.value = "Finish watching the Google AdMob rewarded ad to continue your quiz!"
            }
            AdPurpose.PENDING_MATCH_CLAIM -> {
                _statusMessage.value = "Finish watching the full rewarded ad to claim +$pendingClaimMatchCoins coins."
            }
            AdPurpose.DAILY_STREAK -> {
                _statusMessage.value = "Daily streak claim cancelled — watch the full ad to claim!"
            }
            AdPurpose.BONUS_AD -> {
                _statusMessage.value = "Ad skipped before completion — 0 coins and no ad progress added."
            }
            AdPurpose.TEST_AD -> {
                _statusMessage.value = "Ad skipped before completion — no coins awarded."
            }
        }
    }

    private suspend fun persistSkippedPostMatch(adUnavailable: Boolean = false) {
        if (_gameState.value.lastRecordedMatchId != null) return
        val outcome = pendingMatchOutcome ?: _gameState.value.outcome ?: MatchOutcome.DRAW
        val res = repository.recordCompletedMatch(
            outcome = outcome,
            adWatched = false,
            adSource = AdSource.PENDING,
            gameType = MiniGameType.TIC_TAC_TOE
        )
        res.onSuccess { matchId ->
            _gameState.update {
                it.copy(
                    lastRecordedMatchId = matchId,
                    lastMatchAdClaimed = false
                )
            }
            _statusMessage.value = if (adUnavailable) {
                "Google AdMob ad unavailable — no coins added. Tap 'Watch Google AdMob Ad' once loaded."
            } else {
                "Ad closed early — no coins added. Watch the full AdMob ad to claim +$COINS_PER_GAME coins."
            }
        }.onFailure { err ->
            _statusMessage.value = err.localizedMessage ?: "Could not save match record."
        }
    }

    private suspend fun persistSkippedTapTapMatch(adUnavailable: Boolean = false) {
        if (_tapTapState.value.lastRecordedMatchId != null) return
        val res = repository.recordCompletedMatch(
            outcome = MatchOutcome.WIN,
            adWatched = false,
            adSource = AdSource.PENDING,
            gameType = MiniGameType.TAP_TAP,
            customLevelNumber = _tapTapState.value.roundNumber.toLong()
        )
        res.onSuccess { matchId ->
            _tapTapState.update {
                it.copy(
                    lastRecordedMatchId = matchId,
                    lastMatchAdClaimed = false
                )
            }
            _statusMessage.value = if (adUnavailable) {
                "Google AdMob ad unavailable — no coins added. Tap 'Watch Google AdMob Ad' once loaded."
            } else {
                "Ad closed early — no coins added. Watch the full AdMob ad to claim +$TAP_TAP_COINS coins."
            }
        }.onFailure { err ->
            _statusMessage.value = err.localizedMessage ?: "Could not save Tap Tap round."
        }
    }

    private suspend fun persistSkippedMathQuizMatch(adUnavailable: Boolean = false) {
        if (_mathQuizState.value.lastRecordedMatchId != null) return
        val res = repository.recordCompletedMatch(
            outcome = MatchOutcome.WIN,
            adWatched = false,
            adSource = AdSource.PENDING,
            gameType = MiniGameType.MATH_QUIZ,
            customLevelNumber = _mathQuizState.value.levelNumber.toLong()
        )
        res.onSuccess { matchId ->
            _mathQuizState.update {
                it.copy(
                    lastRecordedMatchId = matchId,
                    lastMatchAdClaimed = false
                )
            }
            _statusMessage.value = if (adUnavailable) {
                "Google AdMob ad unavailable — no coins added. Tap 'Watch Google AdMob Ad' once loaded."
            } else {
                "Ad closed early — no coins added. Watch the full AdMob ad to claim +$MATH_QUIZ_COINS coins."
            }
        }.onFailure { err ->
            _statusMessage.value = err.localizedMessage ?: "Could not save Math Quiz record."
        }
    }

    private companion object {
        const val TAG = "NexaEarnVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
        const val MATH_QUIZ_CHECKPOINT_QUESTIONS = 3
    }
}
