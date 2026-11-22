package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

const val COINS_PER_GAME = 5L
const val BONUS_AD_COINS = 3L
const val WATCH_AD_BUNDLE_COUNT = 3L
const val WATCH_AD_BUNDLE_COINS = 15L
const val MIN_WITHDRAWAL_COINS = 5000L
const val COINS_PER_RUPEE = 100L
const val BANK_WITHDRAWAL_FEE_RUPEES = 10.0
const val WITHDRAW_PROCESSING_TIME_LABEL = "10 minutes to 24 hours"
const val REFERRAL_TARGET_GAMES = 5
const val REFERRAL_BONUS_COINS = 50L
const val DAILY_GOAL_TARGET_COINS = 100L
const val DAILY_GOAL_RESET_HOUR = 1
const val DAILY_GOAL_WINDOW_MILLIS = 24L * 60L * 60L * 1000L
const val EXTRA_EARN_BONUS_PERCENT = 5L
const val TAP_TAP_DURATION_SECONDS = 30
const val TAP_TAP_TARGET_TAPS = 100
const val TAP_TAP_COINS = 5L
const val MATH_QUIZ_QUESTIONS_PER_LEVEL = 5
const val MATH_QUIZ_COINS = 10L

enum class MiniGameType(
    val label: String,
    val difficultyTag: String,
    val rewardCoins: Long
) {
    TIC_TAC_TOE("Tic Tac Toe", "MEDIUM", COINS_PER_GAME),
    TAP_TAP("Tap Tap 30s", "ARCADE", TAP_TAP_COINS),
    MATH_QUIZ("Math Quiz", "EASY", MATH_QUIZ_COINS);

    companion object {
        fun fromRaw(raw: String): MiniGameType =
            entries.firstOrNull { it.name == raw } ?: TIC_TAC_TOE
    }
}

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class MatchOutcome {
    WIN,
    LOSS,
    DRAW;

    companion object {
        fun fromRaw(raw: String): MatchOutcome =
            entries.firstOrNull { it.name == raw } ?: DRAW
    }
}

enum class AdSource {
    ADMOB_TEST,
    PENDING,
    REFERRAL,
    EXTRA_EARN,
    ADMIN;

    companion object {
        fun fromRaw(raw: String): AdSource =
            entries.firstOrNull { it.name == raw } ?: ADMOB_TEST
    }
}

enum class RewardType {
    MATCH_REWARD,
    DAILY_STREAK,
    WATCH_AD,
    TEST_AD,
    REFERRAL_BONUS,
    EXTRA_EARN,
    ADMIN_ADJUSTMENT;

    companion object {
        fun fromRaw(raw: String): RewardType =
            entries.firstOrNull { it.name == raw } ?: WATCH_AD
    }
}

enum class ReferralStatus(val label: String) {
    IN_PROGRESS("In Progress"),
    QUALIFIED("Ready to Claim"),
    CLAIMED("Claimed");

    companion object {
        fun fromRaw(raw: String): ReferralStatus =
            entries.firstOrNull { it.name == raw.trim().uppercase(Locale.US) } ?: IN_PROGRESS
    }
}

fun generateReferralCode(uid: String): String {
    val cleanUid = uid.trim()
    if (cleanUid.isBlank()) return "NX000000"
    val digest = MessageDigest.getInstance("SHA-256")
        .digest(cleanUid.toByteArray(Charsets.UTF_8))
    val hexPrefix = buildString {
        for (i in 0 until 3) {
            append(String.format(Locale.US, "%02X", digest[i]))
        }
    }
    return "NX$hexPrefix"
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Player",
    val email: String? = null,
    val coinsBalance: Long = 0L,
    val totalCoinsEarned: Long = 0L,
    val dailyGamesPlayed: Long = 0L,
    val lastPlayedDate: String = "",
    val currentStreak: Long = 0L,
    val longestStreak: Long = 0L,
    val lastStreakClaimDate: String = "",
    val totalAdsWatched: Long = 0L,
    val bonusAdCycleProgress: Long = 0L,
    val totalGamesWon: Long = 0L,
    val totalGamesPlayed: Long = 0L,
    val referralCode: String = "",
    val referredByUid: String? = null,
    val referredByCode: String? = null,
    val referralBonusEarned: Long = 0L,
    val referralsCompletedCount: Long = 0L,
    val lastClaimedReferralUid: String? = null,
    val goalWindowStartMillis: Long = 0L,
    val goalWindowCoins: Long = 0L,
    val extraEarnPotHundredths: Long = 0L,
    val totalExtraEarnClaimed: Long = 0L,
    val banned: Boolean = false,
    val whitelisted: Boolean = false,
    val banReason: String? = null,
    val bannedByEmail: String? = null,
    @ServerTimestamp val bannedAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun effectiveDailyGamesPlayed(today: String = todayDateString()): Long =
        if (lastPlayedDate == today) dailyGamesPlayed.coerceAtLeast(0L) else 0L

    fun ticTacToeNextLevel(today: String = todayDateString()): Int =
        (effectiveDailyGamesPlayed(today) + 1L).toInt().coerceAtLeast(1)

    fun hasClaimedStreakToday(today: String = todayDateString()): Boolean =
        lastStreakClaimDate == today

    fun nextStreakDay(today: String = todayDateString(), yesterday: String = yesterdayDateString()): Int {
        if (hasClaimedStreakToday(today)) return currentStreak.toInt().coerceAtLeast(1)
        return if (lastStreakClaimDate == yesterday && currentStreak > 0L) {
            (currentStreak.toInt() + 1)
        } else {
            1
        }
    }

    fun winRatePercent(): Float =
        if (totalGamesPlayed > 0L) {
            ((totalGamesWon.toFloat() / totalGamesPlayed.toFloat()) * 100f).coerceIn(0f, 100f)
        } else {
            0f
        }

    fun formattedWinRate(): String =
        if (totalGamesPlayed <= 0L) {
            "0.0%"
        } else {
            String.format(Locale.US, "%.1f%%", winRatePercent())
        }

    fun initials(): String {
        val parts = displayName.trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
        return when {
            parts.isEmpty() -> "P"
            parts.size == 1 -> parts[0].take(2).uppercase(Locale.US)
            else -> "${parts[0].first()}${parts[1].first()}".uppercase(Locale.US)
        }
    }

    fun streakStatusLabel(
        today: String = todayDateString(),
        yesterday: String = yesterdayDateString()
    ): String {
        return when {
            hasClaimedStreakToday(today) -> "Claimed Today"
            lastStreakClaimDate == yesterday && currentStreak > 0L ->
                "Active • Claim Day ${nextStreakDay(today, yesterday)}"
            currentStreak > 0L -> "Streak Reset • Claim Day 1"
            else -> "Ready to Start"
        }
    }

    fun isNewUserForOnboarding(): Boolean =
        totalGamesPlayed == 0L && totalAdsWatched == 0L

    fun clampedWatchAdProgress(): Int =
        bonusAdCycleProgress.toInt().coerceIn(0, (WATCH_AD_BUNDLE_COUNT - 1L).toInt())

    fun resolvedReferralCode(): String =
        referralCode.ifBlank { generateReferralCode(userId.ifBlank { id }) }

    fun canApplyReferralCode(): Boolean =
        referredByUid.isNullOrBlank() && totalGamesPlayed < REFERRAL_TARGET_GAMES.toLong()

    fun isGoalWindowActive(nowMillis: Long = System.currentTimeMillis()): Boolean {
        val cycleStart = goalCycleStartMillis(nowMillis)
        val cycleEnd = nextGoalResetMillis(nowMillis)
        return goalWindowStartMillis >= cycleStart && goalWindowStartMillis < cycleEnd
    }

    fun activeGoalCoins(nowMillis: Long = System.currentTimeMillis()): Long =
        if (isGoalWindowActive(nowMillis)) goalWindowCoins.coerceAtLeast(0L) else 0L

    fun isDailyGoalCompleted(nowMillis: Long = System.currentTimeMillis()): Boolean =
        activeGoalCoins(nowMillis) >= DAILY_GOAL_TARGET_COINS

    fun remainingGoalMillis(nowMillis: Long = System.currentTimeMillis()): Long =
        (nextGoalResetMillis(nowMillis) - nowMillis).coerceAtLeast(0L)

    fun extraEarnPotWholeCoins(): Long =
        extraEarnPotHundredths.coerceAtLeast(0L) / 100L

    fun extraEarnPotRemainderHundredths(): Long =
        extraEarnPotHundredths.coerceAtLeast(0L) % 100L

    fun extraEarnPotFormatted(): String =
        String.format(Locale.US, "%.2f", extraEarnPotHundredths.coerceAtLeast(0L) / 100.0)

    fun needsOneAmAutoClaim(nowMillis: Long = System.currentTimeMillis()): Boolean {
        val cycleStart = goalCycleStartMillis(nowMillis)
        val fromPriorCycle = goalWindowStartMillis == 0L || goalWindowStartMillis < cycleStart
        return fromPriorCycle && extraEarnPotWholeCoins() >= 1L
    }

    fun computeGoalProgressUpdate(
        earnedCoins: Long,
        nowMillis: Long = System.currentTimeMillis()
    ): GoalProgressUpdate {
        val safeEarned = earnedCoins.coerceAtLeast(0L)
        val cycleStart = goalCycleStartMillis(nowMillis)
        val active = isGoalWindowActive(nowMillis)
        val windowStart = if (active) goalWindowStartMillis else cycleStart
        val prevCoins = if (active) goalWindowCoins.coerceAtLeast(0L) else 0L
        val nextCoins = prevCoins + safeEarned
        val coinsAfterGoal = when {
            prevCoins >= DAILY_GOAL_TARGET_COINS -> safeEarned
            nextCoins > DAILY_GOAL_TARGET_COINS -> nextCoins - DAILY_GOAL_TARGET_COINS
            else -> 0L
        }
        val potDeltaHundredths = coinsAfterGoal * EXTRA_EARN_BONUS_PERCENT
        return GoalProgressUpdate(
            nextWindowStartMillis = windowStart,
            nextWindowCoins = nextCoins,
            potDeltaHundredths = potDeltaHundredths,
            goalCompletedAfterAction = nextCoins >= DAILY_GOAL_TARGET_COINS
        )
    }
}

data class GoalProgressUpdate(
    val nextWindowStartMillis: Long,
    val nextWindowCoins: Long,
    val potDeltaHundredths: Long,
    val goalCompletedAfterAction: Boolean
)

@Keep
@IgnoreExtraProperties
data class ReferralCodeEntry(
    @DocumentId val id: String = "",
    val code: String = "",
    val ownerUid: String = "",
    val ownerName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ReferralRecord(
    @DocumentId val id: String = "",
    val referrerUid: String = "",
    val referrerCode: String = "",
    val referrerName: String = "",
    val inviteeUid: String = "",
    val inviteeName: String = "",
    val inviteeGamesCompleted: Long = 0L,
    val targetGames: Long = REFERRAL_TARGET_GAMES.toLong(),
    val bonusCoins: Long = REFERRAL_BONUS_COINS,
    val status: String = ReferralStatus.IN_PROGRESS.name,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun parsedStatus(): ReferralStatus = ReferralStatus.fromRaw(status)

    fun clampedProgress(): Int =
        inviteeGamesCompleted.toInt().coerceIn(0, targetGames.toInt().coerceAtLeast(1))

    fun isReadyToClaim(): Boolean =
        parsedStatus() == ReferralStatus.QUALIFIED && inviteeGamesCompleted >= targetGames
}

@Keep
@IgnoreExtraProperties
data class GameMatchRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val gameType: String = "TIC_TAC_TOE",
    val botDifficulty: String = "MEDIUM",
    val outcome: String = MatchOutcome.DRAW.name,
    val levelNumber: Long = 1L,
    val playedDate: String = "",
    val coinsAwarded: Long = 0L,
    val adWatched: Boolean = false,
    val adSource: String = AdSource.PENDING.name,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun parsedGameType(): MiniGameType = MiniGameType.fromRaw(gameType)

    fun rewardCoinsForGame(): Long = parsedGameType().rewardCoins
}

@Keep
@IgnoreExtraProperties
data class RewardEvent(
    @DocumentId val id: String = "",
    val userId: String = "",
    val rewardType: String = RewardType.WATCH_AD.name,
    val coins: Long = 0L,
    val adSource: String = AdSource.ADMOB_TEST.name,
    val note: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

enum class WithdrawalMethod(val label: String) {
    ESEWA_WALLET("eSewa Wallet"),
    BANK_ACCOUNT("Bank Account"),
    SIM_RECHARGE("SIM Card Recharge");

    companion object {
        fun fromRaw(raw: String): WithdrawalMethod =
            entries.firstOrNull { it.name == raw } ?: ESEWA_WALLET
    }
}

enum class WithdrawalStatus(val label: String) {
    PENDING("Pending"),
    COMPLETED("Completed"),
    FAILED("Failed");

    companion object {
        fun fromRaw(raw: String): WithdrawalStatus =
            when (raw.trim().uppercase(Locale.US)) {
                "COMPLETED", "APPROVED", "PAID", "SUCCESS" -> COMPLETED
                "FAILED", "REJECTED", "CANCELLED" -> FAILED
                else -> PENDING
            }
    }
}

@Keep
@IgnoreExtraProperties
data class WithdrawalRequest(
    @DocumentId val id: String = "",
    val userId: String = "",
    val method: String = WithdrawalMethod.ESEWA_WALLET.name,
    val accountTitle: String = "",
    val destination: String = "",
    val coins: Long = 0L,
    val status: String = "PENDING",
    val adminNote: String? = null,
    val processedByEmail: String? = null,
    val processedAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun parsedStatus(): WithdrawalStatus = WithdrawalStatus.fromRaw(status)

    fun shortTransactionId(): String {
        val clean = id.trim()
        return if (clean.isBlank()) "TXN-NEW" else "TXN-${clean.take(8).uppercase(Locale.US)}"
    }
}

fun coinsToRupees(coins: Long): Double =
    coins.toDouble() / COINS_PER_RUPEE.toDouble()

fun formatRupees(amount: Double): String =
    String.format(Locale.US, "Rs. %.2f", amount)

fun withdrawalFeeRupees(method: WithdrawalMethod): Double =
    if (method == WithdrawalMethod.BANK_ACCOUNT) BANK_WITHDRAWAL_FEE_RUPEES else 0.0

fun withdrawalNetRupees(method: WithdrawalMethod, coins: Long): Double =
    (coinsToRupees(coins) - withdrawalFeeRupees(method)).coerceAtLeast(0.0)

fun todayDateString(): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    return formatter.format(Calendar.getInstance().time)
}

fun yesterdayDateString(): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_YEAR, -1)
    return formatter.format(cal.time)
}

fun goalCycleStartMillis(nowMillis: Long = System.currentTimeMillis()): Long {
    val oneAm = Calendar.getInstance().apply {
        timeInMillis = nowMillis
        set(Calendar.HOUR_OF_DAY, DAILY_GOAL_RESET_HOUR)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    if (nowMillis < oneAm.timeInMillis) {
        oneAm.add(Calendar.DAY_OF_YEAR, -1)
    }
    return oneAm.timeInMillis
}

fun nextGoalResetMillis(nowMillis: Long = System.currentTimeMillis()): Long {
    val cycleStart = goalCycleStartMillis(nowMillis)
    return Calendar.getInstance().apply {
        timeInMillis = cycleStart
        add(Calendar.DAY_OF_YEAR, 1)
    }.timeInMillis
}

fun streakRewardForDay(dayNumber: Int): Long {
    val cycleDay = ((dayNumber - 1).coerceAtLeast(0) % 7) + 1
    return when (cycleDay) {
        1 -> 10L
        2 -> 15L
        3 -> 20L
        4 -> 25L
        5 -> 35L
        6 -> 45L
        else -> 60L
    }
}

const val DEFAULT_WHATSAPP_NUMBER = "9779822871917"
const val DEFAULT_WHATSAPP_PREFILL = "Hello NexaEarn Support, I need assistance with my account."
const val DEFAULT_SUPPORT_HOURS = "9:00 AM – 9:00 PM Daily"
const val DEFAULT_ADMOB_REWARDED_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
const val DEFAULT_ADMOB_BANNER_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
const val ADMIN_UI_VERIFY_HASH = "8ff825b3cae0cd82bbae7c6d12e5897e9a13a330bc9849a4fe3532fbfe08cf72"

val DEFAULT_TERMS_AND_CONDITIONS: String = """
1. Acceptance of Terms
By signing in to and using NexaEarn, you agree to comply with these Terms & Conditions and all applicable fair-play policies.

2. Earning Coins & Google AdMob Ads
• Coins can be earned through Daily Streaks, Mini Games (Tic Tac Toe, Tap Tap 30s, Math Quiz), Watch 3 Ads bundles (+15 coins, unlimited), and Friend Referrals.
• Coins are credited strictly after an available Google AdMob rewarded ad finishes completely. If an ad is unavailable, skipped, or blocked, 0 coins are added.
• Use of Private DNS ad blockers, VPN ad filters, modified APKs, automated clickers, or emulators to manipulate rewards is strictly prohibited and may result in withheld payouts.

3. 24-Hour 100-Coin Goal & Extra Earn Pot
• Earning 100 qualifying coins within the daily cycle (1:00 AM – 1:00 AM) unlocks a 5% Extra Earn bonus on subsequent qualifying earnings.
• Only whole coins (integer amounts, e.g., 76 coins from 76.72) may be claimed into your main wallet balance. Any remaining fractional amount carries forward safely and whole coins are auto-claimed at the 1:00 AM reset.

4. Withdrawals & Payout Processing
• The minimum withdrawal threshold is $MIN_WITHDRAWAL_COINS coins (100 coins = Rs. 1.00).
• Payouts support eSewa Wallet, Bank Account (Rs. 10 bank fee), and SIM Card Recharge within $WITHDRAW_PROCESSING_TIME_LABEL.
• Incorrect destination details may cause a request to be marked Failed; verified refunds or adjustments are managed by NexaEarn Administrators.

5. Account Privacy & Support
• NexaEarn uses Google Sign-In to protect your wallet and payout history across devices.
• For help with payouts, coins, or technical issues, tap Customer Support on your Profile tab to chat with our team on WhatsApp.
""".trimIndent()

@Keep
@IgnoreExtraProperties
data class AppConfig(
    @DocumentId val id: String = "main",
    val whatsappSupportNumber: String = DEFAULT_WHATSAPP_NUMBER,
    val whatsappPrefillMessage: String = DEFAULT_WHATSAPP_PREFILL,
    val termsAndConditions: String = DEFAULT_TERMS_AND_CONDITIONS,
    val announcementTitle: String = "",
    val announcementBody: String = "",
    val maintenanceMode: Boolean = false,
    val maintenanceMessage: String = "NexaEarn is currently undergoing scheduled maintenance. Please check back shortly or contact Customer Support.",
    val supportHours: String = DEFAULT_SUPPORT_HOURS,
    val rewardedAdUnitId: String = DEFAULT_ADMOB_REWARDED_UNIT_ID,
    val bannerAdUnitId: String = DEFAULT_ADMOB_BANNER_UNIT_ID,
    val savedRewardedAdUnitIds: List<String> = listOf(DEFAULT_ADMOB_REWARDED_UNIT_ID),
    val savedBannerAdUnitIds: List<String> = listOf(DEFAULT_ADMOB_BANNER_UNIT_ID),
    val updatedByEmail: String? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AdminNotification(
    @DocumentId val id: String = "",
    val title: String = "",
    val body: String = "",
    val targetType: String = "ALL",
    val targetUid: String = "ALL",
    val targetEmail: String? = null,
    val sentByEmail: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
)

fun sha256Hex(input: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
    return digest.joinToString(separator = "") { b ->
        String.format(Locale.US, "%02x", b)
    }
}

fun verifyAdminPasswordAndComputeProof(password: String): String? {
    if (password.isEmpty()) return null
    val step1 = sha256Hex("NexaAdminGate:$password")
    val step2 = sha256Hex("NexaUiVerify:$step1")
    return if (step2 == ADMIN_UI_VERIFY_HASH) step1 else null
}

fun normalizeAdMobUnitId(raw: String): String = raw.trim()

fun isValidAdMobUnitId(raw: String): Boolean =
    normalizeAdMobUnitId(raw).matches(Regex("^ca-app-pub-[0-9]{10,20}/[0-9]{6,15}$"))

fun sanitizeWhatsAppDigits(rawPhone: String): String =
    rawPhone.filter { it.isDigit() }

fun buildWhatsAppSupportUrl(rawPhone: String, prefillMessage: String): String? {
    val digits = sanitizeWhatsAppDigits(rawPhone)
    if (digits.length !in 7..18) return null
    val encodedMessage = java.net.URLEncoder.encode(
        prefillMessage.trim().take(200).ifBlank { DEFAULT_WHATSAPP_PREFILL },
        Charsets.UTF_8.name()
    )
    return "https://wa.me/$digits?text=$encodedMessage"
}
