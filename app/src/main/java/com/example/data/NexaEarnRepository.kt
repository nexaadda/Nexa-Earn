package com.example.data

import android.content.Context
import com.example.R
import com.example.model.AdSource
import com.example.model.COINS_PER_GAME
import com.example.model.BONUS_AD_COINS
import com.example.model.GameMatchRecord
import com.example.model.MIN_WITHDRAWAL_COINS
import com.example.model.MatchOutcome
import com.example.model.MiniGameType
import com.example.model.REFERRAL_BONUS_COINS
import com.example.model.REFERRAL_TARGET_GAMES
import com.example.model.ReferralCodeEntry
import com.example.model.ReferralRecord
import com.example.model.ReferralStatus
import com.example.model.RewardEvent
import com.example.model.RewardType
import com.example.model.UserProfile
import com.example.model.WATCH_AD_BUNDLE_COINS
import com.example.model.WATCH_AD_BUNDLE_COUNT
import com.example.model.WithdrawalMethod
import com.example.model.WithdrawalRequest
import com.example.model.generateReferralCode
import com.example.model.goalCycleStartMillis
import com.example.model.streakRewardForDay
import com.example.model.todayDateString
import com.example.model.yesterdayDateString
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class NexaEarnRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch." }
        val docRef = db.collection("users").document(userId)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            UserProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, docRef.path)
                    }
                    throw error
                }
        )
    }

    fun observeRecentMatches(userId: String): Flow<List<GameMatchRecord>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch." }
        val path = "users/$userId/matches"
        emitAll(
            db.collection("users")
                .document(userId)
                .collection("matches")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(20)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        GameMatchRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeRecentRewards(userId: String): Flow<List<RewardEvent>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch." }
        val path = "users/$userId/rewards"
        emitAll(
            db.collection("users")
                .document(userId)
                .collection("rewards")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(30)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        RewardEvent::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeWithdrawals(userId: String): Flow<List<WithdrawalRequest>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch." }
        val path = "users/$userId/withdrawals"
        emitAll(
            db.collection("users")
                .document(userId)
                .collection("withdrawals")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(20)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        WithdrawalRequest::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeSentReferrals(userId: String): Flow<List<ReferralRecord>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch." }
        val path = "referrals"
        emitAll(
            db.collection("referrals")
                .whereEqualTo("referrerUid", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ReferralRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: Long.MAX_VALUE }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeMyReferralLink(userId: String): Flow<ReferralRecord?> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch." }
        val docRef = db.collection("referrals").document(userId)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            ReferralRecord::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, docRef.path)
                    }
                    throw error
                }
        )
    }

    suspend fun ensureUserProfile(
        displayName: String?,
        email: String?
    ): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            val userRef = db.collection("users").document(uid)
            path = userRef.path
            val safeName = displayName?.trim()?.take(60)?.ifBlank { "NexaEarn Player" } ?: "NexaEarn Player"
            val safeEmail = email?.trim()?.take(120)?.ifBlank { null }
            val defaultCode = generateReferralCode(uid)

            val snapshot = userRef.get().await()
            val resolvedCode: String
            if (!snapshot.exists()) {
                resolvedCode = defaultCode
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "email" to safeEmail,
                    "coinsBalance" to 0L,
                    "totalCoinsEarned" to 0L,
                    "dailyGamesPlayed" to 0L,
                    "lastPlayedDate" to "",
                    "currentStreak" to 0L,
                    "longestStreak" to 0L,
                    "lastStreakClaimDate" to "",
                    "totalAdsWatched" to 0L,
                    "bonusAdCycleProgress" to 0L,
                    "totalGamesWon" to 0L,
                    "totalGamesPlayed" to 0L,
                    "referralCode" to resolvedCode,
                    "referralBonusEarned" to 0L,
                    "referralsCompletedCount" to 0L,
                    "goalWindowStartMillis" to 0L,
                    "goalWindowCoins" to 0L,
                    "extraEarnPotHundredths" to 0L,
                    "totalExtraEarnClaimed" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null } as Map<String, Any>
                userRef.set(payload).await()
            } else {
                val existingCode = snapshot.getString("referralCode")?.trim().orEmpty()
                if (existingCode.isEmpty()) {
                    resolvedCode = defaultCode
                    val initPayload = mapOf(
                        "displayName" to safeName,
                        "email" to safeEmail,
                        "referralCode" to resolvedCode,
                        "referralBonusEarned" to 0L,
                        "referralsCompletedCount" to 0L,
                        "updatedAt" to FieldValue.serverTimestamp()
                    ).filterValues { it != null } as Map<String, Any>
                    userRef.update(initPayload).await()
                } else {
                    resolvedCode = existingCode
                    val existingName = snapshot.getString("displayName") ?: ""
                    val existingEmail = snapshot.getString("email")
                    if (existingName != safeName || (safeEmail != null && existingEmail != safeEmail)) {
                        val updatePayload = mapOf(
                            "displayName" to safeName,
                            "email" to safeEmail,
                            "updatedAt" to FieldValue.serverTimestamp()
                        ).filterValues { it != null } as Map<String, Any>
                        userRef.update(updatePayload).await()
                    }
                }
            }

            // Ensure referral code lookup entry exists
            val codeRef = db.collection("referralCodes").document(resolvedCode)
            path = codeRef.path
            val codeSnap = codeRef.get().await()
            if (!codeSnap.exists()) {
                codeRef.set(
                    mapOf(
                        "code" to resolvedCode,
                        "ownerUid" to uid,
                        "ownerName" to safeName,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun recordCompletedMatch(
        outcome: MatchOutcome,
        adWatched: Boolean,
        adSource: AdSource,
        gameType: MiniGameType = MiniGameType.TIC_TAC_TOE,
        customLevelNumber: Long? = null
    ): Result<String> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            val today = todayDateString()
            val userRef = db.collection("users").document(uid)
            path = userRef.path
            val referralRef = db.collection("referrals").document(uid)
            val (snapshot, referralSnapshot) = coroutineScope {
                val uDeferred = async { userRef.get().await() }
                val rDeferred = async { referralRef.get().await() }
                uDeferred.await() to rDeferred.await()
            }
            val current = snapshot.toObject(
                UserProfile::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: throw IllegalStateException("User profile not initialized.")

            val playedToday = current.effectiveDailyGamesPlayed(today)
            val nextDailyGamesPlayed: Long
            val nextLastPlayedDate: String
            val nextLevel: Long
            if (gameType == MiniGameType.TIC_TAC_TOE) {
                nextLevel = (playedToday + 1L).coerceAtMost(10_000L)
                nextDailyGamesPlayed = playedToday + 1L
                nextLastPlayedDate = today
            } else {
                nextLevel = (customLevelNumber ?: 1L).coerceIn(1L, 10_000L)
                nextDailyGamesPlayed = playedToday
                nextLastPlayedDate = current.lastPlayedDate
            }

            if (adWatched) {
                require(adSource == AdSource.ADMOB_TEST) {
                    "Google AdMob ad must complete before coins are added."
                }
            }

            val gameRewardCoins = gameType.rewardCoins
            val coinsDelta = if (adWatched) gameRewardCoins else 0L
            val wonDelta = if (outcome == MatchOutcome.WIN) 1L else 0L
            val adDelta = if (adWatched) 1L else 0L
            val resolvedAdSource = if (adWatched) adSource else AdSource.PENDING

            val activeReferral: ReferralRecord? = if (!current.referredByUid.isNullOrBlank() && referralSnapshot.exists()) {
                referralSnapshot.toObject(
                    ReferralRecord::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            } else {
                null
            }

            val matchRef = userRef.collection("matches").document()
            val batch = db.batch()

            val userUpdates = mutableMapOf<String, Any>(
                "coinsBalance" to (current.coinsBalance + coinsDelta),
                "totalCoinsEarned" to (current.totalCoinsEarned + coinsDelta),
                "dailyGamesPlayed" to nextDailyGamesPlayed,
                "lastPlayedDate" to nextLastPlayedDate,
                "totalAdsWatched" to (current.totalAdsWatched + adDelta),
                "totalGamesWon" to (current.totalGamesWon + wonDelta),
                "totalGamesPlayed" to (current.totalGamesPlayed + 1L),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (adWatched && coinsDelta > 0L) {
                val goalUpdate = current.computeGoalProgressUpdate(coinsDelta)
                userUpdates["goalWindowStartMillis"] = goalUpdate.nextWindowStartMillis
                userUpdates["goalWindowCoins"] = goalUpdate.nextWindowCoins
                userUpdates["extraEarnPotHundredths"] =
                    current.extraEarnPotHundredths + goalUpdate.potDeltaHundredths
            }

            batch.update(userRef, userUpdates)

            batch.set(
                matchRef,
                mapOf(
                    "userId" to uid,
                    "gameType" to gameType.name,
                    "botDifficulty" to gameType.difficultyTag,
                    "outcome" to outcome.name,
                    "levelNumber" to nextLevel,
                    "playedDate" to today,
                    "coinsAwarded" to coinsDelta,
                    "adWatched" to adWatched,
                    "adSource" to resolvedAdSource.name,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            if (adWatched) {
                val rewardNote = when (gameType) {
                    MiniGameType.TIC_TAC_TOE -> "Tic Tac Toe Level $nextLevel (${outcome.name}) vs Medium Bot"
                    MiniGameType.TAP_TAP -> "Tap Tap 30s Round $nextLevel (100+ Taps)"
                    MiniGameType.MATH_QUIZ -> "Math Quiz Level $nextLevel (5/5 Questions)"
                }
                val rewardRef = userRef.collection("rewards").document()
                batch.set(
                    rewardRef,
                    mapOf(
                        "userId" to uid,
                        "rewardType" to RewardType.MATCH_REWARD.name,
                        "coins" to gameRewardCoins,
                        "adSource" to resolvedAdSource.name,
                        "note" to rewardNote,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            if (activeReferral != null &&
                activeReferral.parsedStatus() == ReferralStatus.IN_PROGRESS &&
                activeReferral.inviteeGamesCompleted < REFERRAL_TARGET_GAMES.toLong()
            ) {
                val nextGames = activeReferral.inviteeGamesCompleted + 1L
                val nextStatus = if (nextGames >= REFERRAL_TARGET_GAMES.toLong()) {
                    ReferralStatus.QUALIFIED.name
                } else {
                    ReferralStatus.IN_PROGRESS.name
                }
                batch.update(
                    referralRef,
                    mapOf(
                        "inviteeGamesCompleted" to nextGames,
                        "status" to nextStatus,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            batch.commit().await()
            Result.success(matchRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun claimPendingMatchAd(
        matchId: String,
        adSource: AdSource
    ): Result<Long> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/matches/$matchId"
        return try {
            val uid = requireUserId()
            require(matchId.isNotBlank()) { "Match ID is required." }
            val userRef = db.collection("users").document(uid)
            val matchRef = userRef.collection("matches").document(matchId)
            path = matchRef.path

            require(adSource == AdSource.ADMOB_TEST) {
                "Google AdMob ad must complete before claiming coins."
            }

            val (userSnap, matchSnap) = coroutineScope {
                val uDeferred = async { userRef.get().await() }
                val mDeferred = async { matchRef.get().await() }
                uDeferred.await() to mDeferred.await()
            }
            val profile = userSnap.toObject(
                UserProfile::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: throw IllegalStateException("Profile not found.")

            val match = matchSnap.toObject(
                GameMatchRecord::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: throw IllegalStateException("Match record not found.")

            check(!match.adWatched) { "Reward already claimed for this match." }

            val rewardCoins = match.rewardCoinsForGame()
            val parsedGame = match.parsedGameType()
            val goalUpdate = profile.computeGoalProgressUpdate(rewardCoins)
            val batch = db.batch()
            batch.update(
                matchRef,
                mapOf(
                    "adWatched" to true,
                    "coinsAwarded" to rewardCoins,
                    "adSource" to adSource.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                userRef,
                mapOf(
                    "coinsBalance" to (profile.coinsBalance + rewardCoins),
                    "totalCoinsEarned" to (profile.totalCoinsEarned + rewardCoins),
                    "totalAdsWatched" to (profile.totalAdsWatched + 1L),
                    "goalWindowStartMillis" to goalUpdate.nextWindowStartMillis,
                    "goalWindowCoins" to goalUpdate.nextWindowCoins,
                    "extraEarnPotHundredths" to (profile.extraEarnPotHundredths + goalUpdate.potDeltaHundredths),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            val rewardRef = userRef.collection("rewards").document()
            batch.set(
                rewardRef,
                mapOf(
                    "userId" to uid,
                    "rewardType" to RewardType.MATCH_REWARD.name,
                    "coins" to rewardCoins,
                    "adSource" to adSource.name,
                    "note" to "Post-match rewarded ad for ${parsedGame.label} Level ${match.levelNumber}",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(rewardCoins)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun claimDailyStreakAd(adSource: AdSource): Result<Long> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            require(adSource == AdSource.ADMOB_TEST) {
                "Google AdMob ad must complete before claiming streak coins."
            }
            val today = todayDateString()
            val yesterday = yesterdayDateString()
            val userRef = db.collection("users").document(uid)
            path = userRef.path

            val snapshot = userRef.get().await()
            val current = snapshot.toObject(
                UserProfile::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: throw IllegalStateException("Profile not found.")

            check(!current.hasClaimedStreakToday(today)) {
                "Today's daily streak reward has already been claimed."
            }

            val nextStreak = current.nextStreakDay(today, yesterday)
            val rewardCoins = streakRewardForDay(nextStreak)
            val newLongest = maxOf(current.longestStreak, nextStreak.toLong())
            val goalUpdate = current.computeGoalProgressUpdate(rewardCoins)

            val batch = db.batch()
            batch.update(
                userRef,
                mapOf(
                    "coinsBalance" to (current.coinsBalance + rewardCoins),
                    "totalCoinsEarned" to (current.totalCoinsEarned + rewardCoins),
                    "currentStreak" to nextStreak.toLong(),
                    "longestStreak" to newLongest,
                    "lastStreakClaimDate" to today,
                    "totalAdsWatched" to (current.totalAdsWatched + 1L),
                    "goalWindowStartMillis" to goalUpdate.nextWindowStartMillis,
                    "goalWindowCoins" to goalUpdate.nextWindowCoins,
                    "extraEarnPotHundredths" to (current.extraEarnPotHundredths + goalUpdate.potDeltaHundredths),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            val rewardRef = userRef.collection("rewards").document()
            batch.set(
                rewardRef,
                mapOf(
                    "userId" to uid,
                    "rewardType" to RewardType.DAILY_STREAK.name,
                    "coins" to rewardCoins,
                    "adSource" to adSource.name,
                    "note" to "Day $nextStreak Daily Streak Rewarded Ad",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(rewardCoins)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun recordBonusOrTestAdReward(
        rewardType: RewardType,
        adSource: AdSource,
        note: String
    ): Result<Long> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            require(adSource == AdSource.ADMOB_TEST) {
                "Google AdMob ad must complete before adding coins."
            }
            val userRef = db.collection("users").document(uid)
            path = userRef.path

            val snapshot = userRef.get().await()
            val current = snapshot.toObject(
                UserProfile::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: throw IllegalStateException("Profile not found.")

            val coins = BONUS_AD_COINS
            val safeNote = note.trim().take(120).ifBlank { "Rewarded ad completed" }
            val userUpdates = mutableMapOf<String, Any>(
                "coinsBalance" to (current.coinsBalance + coins),
                "totalCoinsEarned" to (current.totalCoinsEarned + coins),
                "totalAdsWatched" to (current.totalAdsWatched + 1L),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (rewardType == RewardType.WATCH_AD) {
                val goalUpdate = current.computeGoalProgressUpdate(coins)
                userUpdates["goalWindowStartMillis"] = goalUpdate.nextWindowStartMillis
                userUpdates["goalWindowCoins"] = goalUpdate.nextWindowCoins
                userUpdates["extraEarnPotHundredths"] =
                    current.extraEarnPotHundredths + goalUpdate.potDeltaHundredths
            }

            val batch = db.batch()
            batch.update(userRef, userUpdates)
            val rewardRef = userRef.collection("rewards").document()
            batch.set(
                rewardRef,
                mapOf(
                    "userId" to uid,
                    "rewardType" to rewardType.name,
                    "coins" to coins,
                    "adSource" to adSource.name,
                    "note" to safeNote,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(coins)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    data class WatchAdBundleResult(
        val adsCompletedInCycle: Long,
        val targetAdsInCycle: Long,
        val coinsAwarded: Long
    )

    suspend fun recordWatchAdBundleStep(
        adSource: AdSource
    ): Result<WatchAdBundleResult> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            require(adSource == AdSource.ADMOB_TEST) {
                "Google AdMob ad must be available and completed before counting progress or adding coins."
            }
            val userRef = db.collection("users").document(uid)
            path = userRef.path

            val snapshot = userRef.get().await()
            val current = snapshot.toObject(
                UserProfile::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: throw IllegalStateException("Profile not found.")

            val priorProgress = current.bonusAdCycleProgress.coerceIn(0L, WATCH_AD_BUNDLE_COUNT - 1L)
            val nextCompletedCount = priorProgress + 1L
            val completedBundle = nextCompletedCount >= WATCH_AD_BUNDLE_COUNT

            if (completedBundle) {
                val coins = WATCH_AD_BUNDLE_COINS
                val goalUpdate = current.computeGoalProgressUpdate(coins)
                val userUpdates = mapOf<String, Any>(
                    "coinsBalance" to (current.coinsBalance + coins),
                    "totalCoinsEarned" to (current.totalCoinsEarned + coins),
                    "totalAdsWatched" to (current.totalAdsWatched + 1L),
                    "bonusAdCycleProgress" to 0L,
                    "goalWindowStartMillis" to goalUpdate.nextWindowStartMillis,
                    "goalWindowCoins" to goalUpdate.nextWindowCoins,
                    "extraEarnPotHundredths" to (current.extraEarnPotHundredths + goalUpdate.potDeltaHundredths),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                val batch = db.batch()
                batch.update(userRef, userUpdates)
                val rewardRef = userRef.collection("rewards").document()
                batch.set(
                    rewardRef,
                    mapOf(
                        "userId" to uid,
                        "rewardType" to RewardType.WATCH_AD.name,
                        "coins" to coins,
                        "adSource" to adSource.name,
                        "note" to "Completed $WATCH_AD_BUNDLE_COUNT Google AdMob Ads (+$coins coins)",
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.commit().await()
                Result.success(
                    WatchAdBundleResult(
                        adsCompletedInCycle = WATCH_AD_BUNDLE_COUNT,
                        targetAdsInCycle = WATCH_AD_BUNDLE_COUNT,
                        coinsAwarded = coins
                    )
                )
            } else {
                userRef.update(
                    mapOf(
                        "totalAdsWatched" to (current.totalAdsWatched + 1L),
                        "bonusAdCycleProgress" to nextCompletedCount,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                Result.success(
                    WatchAdBundleResult(
                        adsCompletedInCycle = nextCompletedCount,
                        targetAdsInCycle = WATCH_AD_BUNDLE_COUNT,
                        coinsAwarded = 0L
                    )
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun submitWithdrawalRequest(
        method: WithdrawalMethod,
        accountTitle: String,
        destination: String,
        coins: Long
    ): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/withdrawals"
        return try {
            val uid = requireUserId()
            require(coins >= MIN_WITHDRAWAL_COINS) {
                "Minimum withdrawal is $MIN_WITHDRAWAL_COINS coins."
            }
            val safeTitle = accountTitle.trim().take(80)
            val safeDestination = destination.trim().take(80)
            require(safeTitle.isNotBlank()) { "Please enter account or carrier details." }
            require(safeDestination.length >= 4) { "Please enter a valid destination account or phone number." }

            val userRef = db.collection("users").document(uid)
            val withdrawalRef = userRef.collection("withdrawals").document()
            path = withdrawalRef.path

            val snapshot = userRef.get().await()
            val current = snapshot.toObject(
                UserProfile::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: throw IllegalStateException("Profile not found.")

            check(current.coinsBalance >= coins) {
                "Insufficient coin balance. Available: ${current.coinsBalance} coins."
            }

            val batch = db.batch()
            batch.update(
                userRef,
                mapOf(
                    "coinsBalance" to (current.coinsBalance - coins),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                withdrawalRef,
                mapOf(
                    "userId" to uid,
                    "method" to method.name,
                    "accountTitle" to safeTitle,
                    "destination" to safeDestination,
                    "coins" to coins,
                    "status" to "PENDING",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun applyReferralCode(rawCode: String): Result<String> {
        var path = "referralCodes"
        return try {
            val uid = requireUserId()
            val cleanCode = rawCode.trim().uppercase(Locale.US)
            require(cleanCode.length in 6..16 && cleanCode.matches(Regex("^[A-Z0-9]+$"))) {
                "Enter a valid 6–16 character referral code (letters and numbers)."
            }

            val userRef = db.collection("users").document(uid)
            val codeRef = db.collection("referralCodes").document(cleanCode)
            val referralRef = db.collection("referrals").document(uid)
            path = userRef.path

            val (userSnap, codeSnap, existingRefSnap) = coroutineScope {
                val uDef = async { userRef.get().await() }
                val cDef = async { codeRef.get().await() }
                val rDef = async { referralRef.get().await() }
                Triple(uDef.await(), cDef.await(), rDef.await())
            }
            val current = userSnap.toObject(
                UserProfile::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: throw IllegalStateException("Profile not initialized.")

            check(current.referredByUid.isNullOrBlank()) {
                "You have already applied a friend's referral code."
            }
            check(current.totalGamesPlayed < REFERRAL_TARGET_GAMES.toLong()) {
                "Referral codes can only be applied during your first $REFERRAL_TARGET_GAMES games."
            }
            require(cleanCode != current.resolvedReferralCode()) {
                "You cannot apply your own referral code."
            }

            check(codeSnap.exists()) {
                "Referral code \"$cleanCode\" was not found. Check with your friend and try again."
            }
            val codeEntry = codeSnap.toObject(
                ReferralCodeEntry::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: throw IllegalStateException("Invalid referral code entry.")

            require(codeEntry.ownerUid.isNotBlank() && codeEntry.ownerUid != uid) {
                "You cannot apply your own referral code."
            }

            check(!existingRefSnap.exists()) {
                "A referral link already exists for your account."
            }

            val referrerName = codeEntry.ownerName.trim().take(60).ifBlank { "Friend" }
            val inviteeName = current.displayName.trim().take(60).ifBlank { "NexaEarn Player" }

            val batch = db.batch()
            batch.update(
                userRef,
                mapOf(
                    "referredByUid" to codeEntry.ownerUid,
                    "referredByCode" to cleanCode,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                referralRef,
                mapOf(
                    "referrerUid" to codeEntry.ownerUid,
                    "referrerCode" to cleanCode,
                    "referrerName" to referrerName,
                    "inviteeUid" to uid,
                    "inviteeName" to inviteeName,
                    "inviteeGamesCompleted" to current.totalGamesPlayed,
                    "targetGames" to REFERRAL_TARGET_GAMES.toLong(),
                    "bonusCoins" to REFERRAL_BONUS_COINS,
                    "status" to ReferralStatus.IN_PROGRESS.name,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(referrerName)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun claimReferralBonus(inviteeUid: String): Result<Long> {
        var path = "referrals/$inviteeUid"
        return try {
            val uid = requireUserId()
            require(inviteeUid.isNotBlank()) { "Invalid referral record ID." }
            val userRef = db.collection("users").document(uid)
            val referralRef = db.collection("referrals").document(inviteeUid)
            val rewardRef = userRef.collection("rewards").document()

            val creditedCoins = db.runTransaction { transaction ->
                val userSnap = transaction.get(userRef)
                val profile = userSnap.toObject(
                    UserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: throw IllegalStateException("Profile not found.")

                val refSnap = transaction.get(referralRef)
                val referral = refSnap.toObject(
                    ReferralRecord::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: throw IllegalStateException("Referral record not found.")

                check(referral.referrerUid == uid) { "You do not own this referral record." }
                check(referral.parsedStatus() == ReferralStatus.QUALIFIED) {
                    if (referral.parsedStatus() == ReferralStatus.CLAIMED) {
                        "Referral bonus has already been claimed."
                    } else {
                        "Friend must complete $REFERRAL_TARGET_GAMES games first."
                    }
                }
                check(referral.inviteeGamesCompleted >= REFERRAL_TARGET_GAMES.toLong()) {
                    "Friend has completed ${referral.inviteeGamesCompleted}/$REFERRAL_TARGET_GAMES games."
                }

                transaction.update(
                    referralRef,
                    mapOf(
                        "status" to ReferralStatus.CLAIMED.name,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                transaction.update(
                    userRef,
                    mapOf(
                        "coinsBalance" to (profile.coinsBalance + REFERRAL_BONUS_COINS),
                        "totalCoinsEarned" to (profile.totalCoinsEarned + REFERRAL_BONUS_COINS),
                        "referralBonusEarned" to (profile.referralBonusEarned + REFERRAL_BONUS_COINS),
                        "referralsCompletedCount" to (profile.referralsCompletedCount + 1L),
                        "lastClaimedReferralUid" to inviteeUid,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                val safeInviteeName = referral.inviteeName.trim().take(40).ifBlank { "Friend" }
                transaction.set(
                    rewardRef,
                    mapOf(
                        "userId" to uid,
                        "rewardType" to RewardType.REFERRAL_BONUS.name,
                        "coins" to REFERRAL_BONUS_COINS,
                        "adSource" to AdSource.REFERRAL.name,
                        "note" to "Referral bonus: $safeInviteeName completed $REFERRAL_TARGET_GAMES games",
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                REFERRAL_BONUS_COINS
            }.await()

            Result.success(creditedCoins)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun claimExtraEarnPot(): Result<Long> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            val userRef = db.collection("users").document(uid)
            path = userRef.path
            val rewardRef = userRef.collection("rewards").document()

            val claimedCoins = db.runTransaction { transaction ->
                val snapshot = transaction.get(userRef)
                val current = snapshot.toObject(
                    UserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: throw IllegalStateException("Profile not found.")

                val currentPot = current.extraEarnPotHundredths.coerceAtLeast(0L)
                val claimableWhole = currentPot / 100L
                check(claimableWhole >= 1L) {
                    val needed = (100L - currentPot).coerceAtLeast(1L)
                    val neededFormatted = String.format(Locale.US, "%.2f", needed / 100.0)
                    "Earn $neededFormatted more Extra Earn coins to claim 1 coin."
                }
                val remainingPot = currentPot - (claimableWhole * 100L)

                transaction.update(
                    userRef,
                    mapOf(
                        "coinsBalance" to (current.coinsBalance + claimableWhole),
                        "totalCoinsEarned" to (current.totalCoinsEarned + claimableWhole),
                        "extraEarnPotHundredths" to remainingPot,
                        "totalExtraEarnClaimed" to (current.totalExtraEarnClaimed + claimableWhole),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                transaction.set(
                    rewardRef,
                    mapOf(
                        "userId" to uid,
                        "rewardType" to RewardType.EXTRA_EARN.name,
                        "coins" to claimableWhole,
                        "adSource" to AdSource.EXTRA_EARN.name,
                        "note" to "Claimed $claimableWhole ${if (claimableWhole == 1L) "coin" else "coins"} from Extra Earn pot (5% goal bonus)",
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                claimableWhole
            }.await()

            Result.success(claimedCoins)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun autoClaimExtraEarnOnOneAmReset(): Result<Long> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            val userRef = db.collection("users").document(uid)
            path = userRef.path
            val rewardRef = userRef.collection("rewards").document()

            val claimedCoins = db.runTransaction { transaction ->
                val snapshot = transaction.get(userRef)
                val current = snapshot.toObject(
                    UserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: throw IllegalStateException("Profile not found.")

                val nowMillis = System.currentTimeMillis()
                val cycleStart = goalCycleStartMillis(nowMillis)
                if (!current.needsOneAmAutoClaim(nowMillis)) {
                    return@runTransaction 0L
                }

                // Always claim strictly whole integer coins (e.g. 76 from 76.72)
                val currentPot = current.extraEarnPotHundredths.coerceAtLeast(0L)
                val claimableWhole = currentPot / 100L
                if (claimableWhole < 1L) {
                    return@runTransaction 0L
                }
                val remainingPot = currentPot - (claimableWhole * 100L)

                transaction.update(
                    userRef,
                    mapOf(
                        "coinsBalance" to (current.coinsBalance + claimableWhole),
                        "totalCoinsEarned" to (current.totalCoinsEarned + claimableWhole),
                        "extraEarnPotHundredths" to remainingPot,
                        "totalExtraEarnClaimed" to (current.totalExtraEarnClaimed + claimableWhole),
                        "goalWindowStartMillis" to cycleStart,
                        "goalWindowCoins" to 0L,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                transaction.set(
                    rewardRef,
                    mapOf(
                        "userId" to uid,
                        "rewardType" to RewardType.EXTRA_EARN.name,
                        "coins" to claimableWhole,
                        "adSource" to AdSource.EXTRA_EARN.name,
                        "note" to "1:00 AM auto-claim: +$claimableWhole whole ${if (claimableWhole == 1L) "coin" else "coins"} from Extra Earn",
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                claimableWhole
            }.await()

            Result.success(claimedCoins)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }
}
