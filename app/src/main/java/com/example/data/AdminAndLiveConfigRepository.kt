package com.example.data

import com.example.model.AdSource
import com.example.model.AdminNotification
import com.example.model.AppConfig
import com.example.model.DEFAULT_ADMOB_BANNER_UNIT_ID
import com.example.model.DEFAULT_ADMOB_REWARDED_UNIT_ID
import com.example.model.DEFAULT_TERMS_AND_CONDITIONS
import com.example.model.DEFAULT_WHATSAPP_NUMBER
import com.example.model.DEFAULT_WHATSAPP_PREFILL
import com.example.model.RewardType
import com.example.model.UserProfile
import com.example.model.WithdrawalRequest
import com.example.model.WithdrawalStatus
import com.example.model.isValidAdMobUnitId
import com.example.model.normalizeAdMobUnitId
import com.example.model.sanitizeWhatsAppDigits
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class AdminAndLiveConfigRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google.")
    }

    fun observeAppConfig(): Flow<AppConfig> = flow {
        val uid = requireUserId()
        val docRef = db.collection("appConfig").document("main")
        emitAll(
            docRef.snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        val parsed = snap.toObject(
                            AppConfig::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        ) ?: AppConfig()
                        val hasRewardedField = snap.contains("rewardedAdUnitId")
                        val hasBannerField = snap.contains("bannerAdUnitId")
                        val hasSavedRewarded = snap.contains("savedRewardedAdUnitIds")
                        val hasSavedBanner = snap.contains("savedBannerAdUnitIds")
                        parsed.copy(
                            whatsappSupportNumber = parsed.whatsappSupportNumber.ifBlank { DEFAULT_WHATSAPP_NUMBER },
                            whatsappPrefillMessage = parsed.whatsappPrefillMessage.ifBlank { DEFAULT_WHATSAPP_PREFILL },
                            termsAndConditions = parsed.termsAndConditions.ifBlank { DEFAULT_TERMS_AND_CONDITIONS },
                            rewardedAdUnitId = if (hasRewardedField) {
                                parsed.rewardedAdUnitId.trim()
                            } else {
                                DEFAULT_ADMOB_REWARDED_UNIT_ID
                            },
                            bannerAdUnitId = if (hasBannerField) {
                                parsed.bannerAdUnitId.trim()
                            } else {
                                DEFAULT_ADMOB_BANNER_UNIT_ID
                            },
                            savedRewardedAdUnitIds = if (hasSavedRewarded) {
                                parsed.savedRewardedAdUnitIds.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
                            } else {
                                listOf(DEFAULT_ADMOB_REWARDED_UNIT_ID)
                            },
                            savedBannerAdUnitIds = if (hasSavedBanner) {
                                parsed.savedBannerAdUnitIds.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
                            } else {
                                listOf(DEFAULT_ADMOB_BANNER_UNIT_ID)
                            }
                        )
                    } else {
                        AppConfig()
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

    fun observeMyNotifications(userId: String): Flow<List<AdminNotification>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch." }
        val path = "notifications"
        emitAll(
            db.collection("notifications")
                .whereIn("targetUid", listOf("ALL", userId))
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        AdminNotification::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
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

    suspend fun claimAdminAccess(passDigest: String): Result<Unit> {
        var path = "admins"
        return try {
            val uid = requireUserId()
            val ref = db.collection("admins").document(uid)
            path = ref.path
            ref.set(
                mapOf(
                    "uid" to uid,
                    "passDigest" to passDigest,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun revokeAdminAccess(): Result<Unit> {
        var path = "admins"
        return try {
            val uid = requireUserId()
            val ref = db.collection("admins").document(uid)
            path = ref.path
            ref.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    fun observeAllUsersForAdmin(): Flow<List<UserProfile>> = flow {
        val uid = requireUserId()
        val path = "users"
        emitAll(
            db.collection("users")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        UserProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.coinsBalance }
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

    fun observeAllWithdrawalsForAdmin(): Flow<List<WithdrawalRequest>> = flow {
        val uid = requireUserId()
        val path = "collectionGroup/withdrawals"
        emitAll(
            db.collectionGroup("withdrawals")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        WithdrawalRequest::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(
                        compareBy<WithdrawalRequest> {
                            when (it.parsedStatus()) {
                                WithdrawalStatus.PENDING -> 0
                                WithdrawalStatus.COMPLETED -> 1
                                WithdrawalStatus.FAILED -> 2
                            }
                        }.thenByDescending { it.createdAt?.toDate()?.time ?: 0L }
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

    fun observeAllNotificationsForAdmin(): Flow<List<AdminNotification>> = flow {
        val uid = requireUserId()
        val path = "notifications"
        emitAll(
            db.collection("notifications")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        AdminNotification::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
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

    suspend fun adjustUserCoins(
        targetUid: String,
        deltaCoins: Long,
        note: String,
        adminEmail: String?
    ): Result<Long> {
        var path = "users/$targetUid"
        return try {
            requireUserId()
            require(targetUid.isNotBlank()) { "Target user ID is required." }
            require(deltaCoins != 0L) { "Enter a non-zero coin amount." }
            val userRef = db.collection("users").document(targetUid)
            val rewardRef = userRef.collection("rewards").document()
            val notifRef = db.collection("notifications").document()
            path = userRef.path

            val newBalance = db.runTransaction { transaction ->
                val snap = transaction.get(userRef)
                val current = snap.toObject(
                    UserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: throw IllegalStateException("Target user not found.")

                val updatedBalance = (current.coinsBalance + deltaCoins).coerceIn(0L, 10_000_000L)
                val updatedTotalEarned = if (deltaCoins > 0L) {
                    (current.totalCoinsEarned + deltaCoins).coerceIn(0L, 10_000_000L)
                } else {
                    current.totalCoinsEarned.coerceIn(0L, 10_000_000L)
                }

                transaction.update(
                    userRef,
                    mapOf(
                        "coinsBalance" to updatedBalance,
                        "totalCoinsEarned" to updatedTotalEarned,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                val cleanNote = note.trim().take(120).ifBlank {
                    if (deltaCoins > 0L) "Admin credited +$deltaCoins coins" else "Admin deducted ${-deltaCoins} coins"
                }

                if (deltaCoins > 0L) {
                    transaction.set(
                        rewardRef,
                        mapOf(
                            "userId" to targetUid,
                            "rewardType" to RewardType.ADMIN_ADJUSTMENT.name,
                            "coins" to deltaCoins.coerceAtMost(10_000_000L),
                            "adSource" to AdSource.ADMIN.name,
                            "note" to cleanNote,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

                val notifTitle = if (deltaCoins > 0L) {
                    "+$deltaCoins Coins Added by Admin"
                } else {
                    "${-deltaCoins} Coins Deducted by Admin"
                }
                transaction.set(
                    notifRef,
                    mapOf(
                        "title" to notifTitle,
                        "body" to "$cleanNote • New balance: $updatedBalance coins.",
                        "targetType" to "USER",
                        "targetUid" to targetUid,
                        "targetEmail" to current.email,
                        "sentByEmail" to adminEmail?.trim()?.take(120),
                        "createdAt" to FieldValue.serverTimestamp()
                    ).filterValues { it != null }
                )

                updatedBalance
            }.await()

            Result.success(newBalance)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun setUserAccessStatus(
        targetUid: String,
        banned: Boolean,
        whitelisted: Boolean,
        reason: String?,
        adminEmail: String?
    ): Result<Unit> {
        var path = "users/$targetUid"
        return try {
            requireUserId()
            require(targetUid.isNotBlank()) { "Target user ID is required." }
            require(!(banned && whitelisted)) { "A user cannot be banned and whitelisted simultaneously." }

            val userRef = db.collection("users").document(targetUid)
            val notifRef = db.collection("notifications").document()
            path = userRef.path

            val safeReason = reason?.trim()?.take(200)?.ifBlank { null }
            val safeAdminEmail = adminEmail?.trim()?.take(120)

            db.runTransaction { transaction ->
                val snap = transaction.get(userRef)
                val current = snap.toObject(
                    UserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: throw IllegalStateException("Target user not found.")

                val updateFields = mutableMapOf<String, Any?>(
                    "banned" to banned,
                    "whitelisted" to whitelisted,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (banned) {
                    updateFields["banReason"] = safeReason ?: "Account suspended by NexaEarn Admin."
                    updateFields["bannedByEmail"] = safeAdminEmail
                    updateFields["bannedAt"] = FieldValue.serverTimestamp()
                } else {
                    updateFields["banReason"] = null
                    updateFields["bannedByEmail"] = null
                    updateFields["bannedAt"] = null
                }

                transaction.update(userRef, updateFields)

                val notifTitle = when {
                    banned -> "Account Suspended / Blacklisted"
                    whitelisted -> "Account Verified & Whitelisted"
                    else -> "Account Suspension Lifted"
                }
                val notifBody = when {
                    banned -> safeReason ?: "Your account has been restricted by an Administrator. Contact WhatsApp Support for review."
                    whitelisted -> "Your NexaEarn account has been marked Trusted / Whitelisted by Admin."
                    else -> "Your NexaEarn account access has been restored by Admin."
                }.take(500)

                transaction.set(
                    notifRef,
                    mapOf(
                        "title" to notifTitle,
                        "body" to notifBody,
                        "targetType" to "USER",
                        "targetUid" to targetUid,
                        "targetEmail" to current.email,
                        "sentByEmail" to safeAdminEmail,
                        "createdAt" to FieldValue.serverTimestamp()
                    ).filterValues { it != null }
                )
            }.await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateWithdrawalStatus(
        userId: String,
        withdrawalId: String,
        newStatus: WithdrawalStatus,
        refundCoinsOnFail: Boolean,
        adminNote: String,
        adminEmail: String?
    ): Result<Unit> {
        var path = "users/$userId/withdrawals/$withdrawalId"
        return try {
            requireUserId()
            require(userId.isNotBlank() && withdrawalId.isNotBlank()) { "Invalid request reference." }
            require(newStatus != WithdrawalStatus.PENDING) { "Select Approve or Failed." }

            val userRef = db.collection("users").document(userId)
            val wdRef = userRef.collection("withdrawals").document(withdrawalId)
            val notifRef = db.collection("notifications").document()
            path = wdRef.path

            val safeNote = adminNote.trim().take(160)
            val safeAdminEmail = adminEmail?.trim()?.take(120)

            db.runTransaction { transaction ->
                val wdSnap = transaction.get(wdRef)
                val wd = wdSnap.toObject(
                    WithdrawalRequest::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: throw IllegalStateException("Request not found.")

                check(wd.parsedStatus() == WithdrawalStatus.PENDING) {
                    "This request has already been marked ${wd.parsedStatus().label}."
                }

                val userSnap = transaction.get(userRef)
                val userProfile = userSnap.toObject(
                    UserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )

                transaction.update(
                    wdRef,
                    mapOf(
                        "status" to newStatus.name,
                        "adminNote" to safeNote.ifBlank { null },
                        "processedByEmail" to safeAdminEmail,
                        "processedAt" to FieldValue.serverTimestamp()
                    )
                )

                val didRefund = newStatus == WithdrawalStatus.FAILED && refundCoinsOnFail && userProfile != null
                if (didRefund && userProfile != null) {
                    val refundedBalance = (userProfile.coinsBalance + wd.coins).coerceAtMost(10_000_000L)
                    transaction.update(
                        userRef,
                        mapOf(
                            "coinsBalance" to refundedBalance,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

                val notifTitle = if (newStatus == WithdrawalStatus.COMPLETED) {
                    "Request Approved (${wd.shortTransactionId()})"
                } else {
                    "Request Failed (${wd.shortTransactionId()})"
                }
                val notifBody = buildString {
                    if (newStatus == WithdrawalStatus.COMPLETED) {
                        append("Your ${wd.coins}-coin payout via ${WithdrawalMethodLabel(wd.method)} was approved.")
                    } else {
                        append("Your ${wd.coins}-coin request was marked Failed.")
                        if (didRefund) append(" ${wd.coins} coins were refunded to your wallet.")
                    }
                    if (safeNote.isNotBlank()) append(" Note: $safeNote")
                }.take(500)

                transaction.set(
                    notifRef,
                    mapOf(
                        "title" to notifTitle,
                        "body" to notifBody,
                        "targetType" to "USER",
                        "targetUid" to userId,
                        "targetEmail" to userProfile?.email,
                        "sentByEmail" to safeAdminEmail,
                        "createdAt" to FieldValue.serverTimestamp()
                    ).filterValues { it != null }
                )
            }.await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun sendCustomNotification(
        title: String,
        body: String,
        targetUid: String,
        targetEmail: String?,
        sentByEmail: String?
    ): Result<Unit> {
        var path = "notifications"
        return try {
            requireUserId()
            val cleanTitle = title.trim().take(120)
            val cleanBody = body.trim().take(500)
            require(cleanTitle.isNotBlank()) { "Notification title cannot be empty." }
            require(cleanBody.isNotBlank()) { "Notification message cannot be empty." }

            val isBroadcast = targetUid == "ALL"
            val notifRef = db.collection("notifications").document()
            path = notifRef.path

            val payload = mapOf(
                "title" to cleanTitle,
                "body" to cleanBody,
                "targetType" to if (isBroadcast) "ALL" else "USER",
                "targetUid" to if (isBroadcast) "ALL" else targetUid.trim(),
                "targetEmail" to if (isBroadcast) null else targetEmail?.trim()?.take(120),
                "sentByEmail" to sentByEmail?.trim()?.take(120),
                "createdAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>

            notifRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteNotification(notificationId: String): Result<Unit> {
        var path = "notifications/$notificationId"
        return try {
            requireUserId()
            require(notificationId.isNotBlank()) { "Invalid notification ID." }
            db.collection("notifications").document(notificationId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun saveAppConfig(config: AppConfig, adminEmail: String?): Result<Unit> {
        val path = "appConfig/main"
        return try {
            requireUserId()
            val digits = sanitizeWhatsAppDigits(config.whatsappSupportNumber)
            require(digits.length in 7..18) {
                "Enter a valid WhatsApp number with country code (7–18 digits)."
            }
            val cleanTerms = config.termsAndConditions.trim().take(20000)
            require(cleanTerms.isNotBlank()) { "Terms & Conditions cannot be empty." }

            val cleanRewardedId = normalizeAdMobUnitId(config.rewardedAdUnitId)
            val cleanBannerId = normalizeAdMobUnitId(config.bannerAdUnitId)
            require(cleanRewardedId.isEmpty() || isValidAdMobUnitId(cleanRewardedId)) {
                "Invalid Rewarded Ad Unit ID. Expected format: ca-app-pub-XXXXXXXXXXXXXXXX/YYYYYYYYYY"
            }
            require(cleanBannerId.isEmpty() || isValidAdMobUnitId(cleanBannerId)) {
                "Invalid Banner Ad Unit ID. Expected format: ca-app-pub-XXXXXXXXXXXXXXXX/YYYYYYYYYY"
            }

            val cleanSavedRewarded = config.savedRewardedAdUnitIds
                .map(::normalizeAdMobUnitId)
                .filter(::isValidAdMobUnitId)
                .distinct()
                .take(20)
            val cleanSavedBanner = config.savedBannerAdUnitIds
                .map(::normalizeAdMobUnitId)
                .filter(::isValidAdMobUnitId)
                .distinct()
                .take(20)

            val payload = mapOf(
                "whatsappSupportNumber" to digits,
                "whatsappPrefillMessage" to config.whatsappPrefillMessage.trim().take(200),
                "termsAndConditions" to cleanTerms,
                "announcementTitle" to config.announcementTitle.trim().take(120),
                "announcementBody" to config.announcementBody.trim().take(400),
                "maintenanceMode" to config.maintenanceMode,
                "maintenanceMessage" to config.maintenanceMessage.trim().take(300),
                "supportHours" to config.supportHours.trim().take(80),
                "rewardedAdUnitId" to cleanRewardedId,
                "bannerAdUnitId" to cleanBannerId,
                "savedRewardedAdUnitIds" to cleanSavedRewarded,
                "savedBannerAdUnitIds" to cleanSavedBanner,
                "updatedByEmail" to adminEmail?.trim()?.take(120),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>

            db.collection("appConfig").document("main").set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    private fun WithdrawalMethodLabel(rawMethod: String): String =
        com.example.model.WithdrawalMethod.fromRaw(rawMethod).label
}
