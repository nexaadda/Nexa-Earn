package com.example

import com.example.game.CellMark
import com.example.game.MathOperation
import com.example.game.MathQuizEngine
import com.example.game.TicTacToeEngine
import com.example.model.ADMIN_UI_VERIFY_HASH
import com.example.model.AdSource
import com.example.model.AppConfig
import com.example.model.DAILY_GOAL_TARGET_COINS
import com.example.model.DAILY_GOAL_WINDOW_MILLIS
import com.example.model.DEFAULT_ADMOB_BANNER_UNIT_ID
import com.example.model.DEFAULT_ADMOB_REWARDED_UNIT_ID
import com.example.model.DEFAULT_WHATSAPP_NUMBER
import com.example.model.EXTRA_EARN_BONUS_PERCENT
import com.example.model.MATH_QUIZ_COINS
import com.example.model.MATH_QUIZ_QUESTIONS_PER_LEVEL
import com.example.model.MIN_WITHDRAWAL_COINS
import com.example.model.MatchOutcome
import com.example.model.MiniGameType
import com.example.model.REFERRAL_BONUS_COINS
import com.example.model.REFERRAL_TARGET_GAMES
import com.example.model.ReferralRecord
import com.example.model.ReferralStatus
import com.example.model.TAP_TAP_COINS
import com.example.model.TAP_TAP_DURATION_SECONDS
import com.example.model.TAP_TAP_TARGET_TAPS
import com.example.model.UserProfile
import com.example.model.WATCH_AD_BUNDLE_COINS
import com.example.model.WATCH_AD_BUNDLE_COUNT
import com.example.model.WithdrawalMethod
import com.example.model.WithdrawalRequest
import com.example.model.WithdrawalStatus
import com.example.model.buildWhatsAppSupportUrl
import com.example.model.coinsToRupees
import com.example.model.formatRupees
import com.example.model.generateReferralCode
import com.example.model.goalCycleStartMillis
import com.example.model.isValidAdMobUnitId
import com.example.model.nextGoalResetMillis
import com.example.model.normalizeAdMobUnitId
import com.example.model.sanitizeWhatsAppDigits
import com.example.model.sha256Hex
import com.example.model.streakRewardForDay
import com.example.model.verifyAdminPasswordAndComputeProof
import com.example.model.withdrawalFeeRupees
import com.example.model.withdrawalNetRupees
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import kotlin.random.Random

class TicTacToeEngineTest {

    @Test
    fun mediumBotTakesImmediateWinningMove() {
        val board = listOf(
            CellMark.BOT_O, CellMark.BOT_O, CellMark.EMPTY,
            CellMark.PLAYER_X, CellMark.PLAYER_X, CellMark.EMPTY,
            CellMark.EMPTY, CellMark.EMPTY, CellMark.EMPTY
        )
        val move = TicTacToeEngine.chooseMediumBotMove(board, Random(42))
        assertEquals(2, move)
    }

    @Test
    fun evaluateDetectsPlayerWinAndWinningLine() {
        val board = listOf(
            CellMark.PLAYER_X, CellMark.BOT_O, CellMark.EMPTY,
            CellMark.EMPTY, CellMark.PLAYER_X, CellMark.BOT_O,
            CellMark.EMPTY, CellMark.EMPTY, CellMark.PLAYER_X
        )
        val eval = TicTacToeEngine.evaluate(board)
        assertEquals(MatchOutcome.WIN, eval.outcome)
        assertEquals(listOf(0, 4, 8), eval.winningLine)
    }

    @Test
    fun evaluateDetectsDraw() {
        val board = listOf(
            CellMark.PLAYER_X, CellMark.BOT_O, CellMark.PLAYER_X,
            CellMark.PLAYER_X, CellMark.BOT_O, CellMark.BOT_O,
            CellMark.BOT_O, CellMark.PLAYER_X, CellMark.PLAYER_X
        )
        val eval = TicTacToeEngine.evaluate(board)
        assertEquals(MatchOutcome.DRAW, eval.outcome)
        assertTrue(eval.winningLine.isEmpty())
    }

    @Test
    fun streakRewardsScaleAcrossSevenDayCycle() {
        assertEquals(10L, streakRewardForDay(1))
        assertEquals(25L, streakRewardForDay(4))
        assertEquals(60L, streakRewardForDay(7))
        assertEquals(10L, streakRewardForDay(8))
    }

    @Test
    fun userProfileComputesWinRateAndInitialsAccurately() {
        val freshProfile = UserProfile(displayName = "Alex Mercer", totalGamesPlayed = 0L, totalGamesWon = 0L)
        assertEquals(0f, freshProfile.winRatePercent(), 0.01f)
        assertEquals("0.0%", freshProfile.formattedWinRate())
        assertEquals("AM", freshProfile.initials())

        val activeProfile = UserProfile(displayName = "Sam", totalGamesPlayed = 8L, totalGamesWon = 5L)
        assertEquals(62.5f, activeProfile.winRatePercent(), 0.01f)
        assertEquals("62.5%", activeProfile.formattedWinRate())
        assertEquals("SA", activeProfile.initials())
    }

    @Test
    fun userProfileStreakStatusReflectsClaimState() {
        val today = "2026-11-20"
        val yesterday = "2026-11-19"
        val claimed = UserProfile(currentStreak = 3L, lastStreakClaimDate = today)
        assertEquals("Claimed Today", claimed.streakStatusLabel(today, yesterday))

        val activeUnclaimed = UserProfile(currentStreak = 3L, lastStreakClaimDate = yesterday)
        assertEquals("Active • Claim Day 4", activeUnclaimed.streakStatusLabel(today, yesterday))

        val lapsed = UserProfile(currentStreak = 4L, lastStreakClaimDate = "2026-11-10")
        assertEquals("Streak Reset • Claim Day 1", lapsed.streakStatusLabel(today, yesterday))

        val brandNew = UserProfile(currentStreak = 0L, lastStreakClaimDate = "")
        assertEquals("Ready to Start", brandNew.streakStatusLabel(today, yesterday))
    }

    @Test
    fun userProfileDetectsNewUserForOnboarding() {
        val newUser = UserProfile(totalGamesPlayed = 0L, totalAdsWatched = 0L)
        assertTrue(newUser.isNewUserForOnboarding())

        val playedMatchUser = UserProfile(totalGamesPlayed = 1L, totalAdsWatched = 0L)
        assertEquals(false, playedMatchUser.isNewUserForOnboarding())

        val watchedAdUser = UserProfile(totalGamesPlayed = 0L, totalAdsWatched = 1L)
        assertEquals(false, watchedAdUser.isNewUserForOnboarding())
    }

    @Test
    fun withdrawalExchangeRateAndBankFeeCalculationsAreAccurate() {
        assertEquals(5000L, MIN_WITHDRAWAL_COINS)
        assertEquals(1.0, coinsToRupees(100L), 0.001)
        assertEquals(50.0, coinsToRupees(5000L), 0.001)
        assertEquals("Rs. 50.00", formatRupees(coinsToRupees(5000L)))

        assertEquals(0.0, withdrawalFeeRupees(WithdrawalMethod.ESEWA_WALLET), 0.001)
        assertEquals(0.0, withdrawalFeeRupees(WithdrawalMethod.SIM_RECHARGE), 0.001)
        assertEquals(10.0, withdrawalFeeRupees(WithdrawalMethod.BANK_ACCOUNT), 0.001)

        assertEquals(50.0, withdrawalNetRupees(WithdrawalMethod.ESEWA_WALLET, 5000L), 0.001)
        assertEquals(50.0, withdrawalNetRupees(WithdrawalMethod.SIM_RECHARGE, 5000L), 0.001)
        assertEquals(40.0, withdrawalNetRupees(WithdrawalMethod.BANK_ACCOUNT, 5000L), 0.001)
    }

    @Test
    fun withdrawalStatusAndTransactionIdNormalizeCorrectly() {
        val pendingReq = WithdrawalRequest(id = "abc12345xyz", status = "PENDING")
        assertEquals(WithdrawalStatus.PENDING, pendingReq.parsedStatus())
        assertEquals("Pending", pendingReq.parsedStatus().label)
        assertEquals("TXN-ABC12345", pendingReq.shortTransactionId())

        val completedReq = WithdrawalRequest(id = "paid99", status = "completed")
        assertEquals(WithdrawalStatus.COMPLETED, completedReq.parsedStatus())
        assertEquals("Completed", completedReq.parsedStatus().label)
        assertEquals("TXN-PAID99", completedReq.shortTransactionId())

        val emptyIdReq = WithdrawalRequest(id = "", status = "REJECTED")
        assertEquals(WithdrawalStatus.FAILED, emptyIdReq.parsedStatus())
        assertEquals("TXN-NEW", emptyIdReq.shortTransactionId())
    }

    @Test
    fun referralCodeGenerationIsDeterministicAndFormattedForFirestoreRules() {
        val codeA1 = generateReferralCode("uid-alice-123")
        val codeA2 = generateReferralCode("uid-alice-123")
        val codeB = generateReferralCode("uid-bob-456")

        assertEquals(codeA1, codeA2)
        assertNotEquals(codeA1, codeB)
        assertTrue(codeA1.startsWith("NX"))
        assertEquals(8, codeA1.length)
        assertTrue(codeA1.matches(Regex("^[A-Z0-9]{6,16}$")))
    }

    @Test
    fun userProfileReferralEligibilityAndCodeResolutionWorkProperly() {
        val eligibleNewUser = UserProfile(
            userId = "uid-bob",
            totalGamesPlayed = 4L,
            referredByUid = null
        )
        assertTrue(eligibleNewUser.canApplyReferralCode())
        assertEquals(generateReferralCode("uid-bob"), eligibleNewUser.resolvedReferralCode())

        val afterFifthGameUser = UserProfile(
            userId = "uid-bob",
            totalGamesPlayed = 5L,
            referredByUid = null,
            referralCode = "NXCUSTOM1"
        )
        assertFalse(afterFifthGameUser.canApplyReferralCode())
        assertEquals("NXCUSTOM1", afterFifthGameUser.resolvedReferralCode())

        val alreadyReferredUser = UserProfile(
            userId = "uid-bob",
            totalGamesPlayed = 1L,
            referredByUid = "uid-alice"
        )
        assertFalse(alreadyReferredUser.canApplyReferralCode())
    }

    @Test
    fun referralRecordProgressAndReadyToClaimStatesAreAccurate() {
        assertEquals(5, REFERRAL_TARGET_GAMES)
        assertEquals(50L, REFERRAL_BONUS_COINS)

        val inProgress = ReferralRecord(
            inviteeGamesCompleted = 3L,
            status = "IN_PROGRESS"
        )
        assertEquals(ReferralStatus.IN_PROGRESS, inProgress.parsedStatus())
        assertEquals(3, inProgress.clampedProgress())
        assertFalse(inProgress.isReadyToClaim())

        val qualified = ReferralRecord(
            inviteeGamesCompleted = 5L,
            status = "qualified"
        )
        assertEquals(ReferralStatus.QUALIFIED, qualified.parsedStatus())
        assertEquals(5, qualified.clampedProgress())
        assertTrue(qualified.isReadyToClaim())

        val claimed = ReferralRecord(
            inviteeGamesCompleted = 6L,
            status = "CLAIMED"
        )
        assertEquals(ReferralStatus.CLAIMED, claimed.parsedStatus())
        assertEquals(5, claimed.clampedProgress())
        assertFalse(claimed.isReadyToClaim())
    }

    @Test
    fun dailyGoalTracksAndResetsAtOneAmWithWholeCoinClaimOnly() {
        assertEquals(100L, DAILY_GOAL_TARGET_COINS)
        assertEquals(86_400_000L, DAILY_GOAL_WINDOW_MILLIS)
        assertEquals(5L, EXTRA_EARN_BONUS_PERCENT)

        val day1Afternoon = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 21, 14, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val day1OneAm = goalCycleStartMillis(day1Afternoon)
        val day2ZeroThirtyAm = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 22, 0, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val day2OneAm = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 22, 1, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        // 00:30 AM still belongs to previous day's 1:00 AM cycle
        assertEquals(day1OneAm, goalCycleStartMillis(day2ZeroThirtyAm))
        assertEquals(day2OneAm, nextGoalResetMillis(day1Afternoon))
        // At 01:00 AM local time, cycle rolls over
        assertEquals(day2OneAm, goalCycleStartMillis(day2OneAm))

        val fresh = UserProfile()

        // First qualifying earn starts at Day 1 1:00 AM cycle boundary
        val firstUpdate = fresh.computeGoalProgressUpdate(earnedCoins = 85L, nowMillis = day1Afternoon)
        assertEquals(day1OneAm, firstUpdate.nextWindowStartMillis)
        assertEquals(85L, firstUpdate.nextWindowCoins)
        assertEquals(0L, firstUpdate.potDeltaHundredths)
        assertFalse(firstUpdate.goalCompletedAfterAction)

        // Crossing 100 coins awards 5% only on the overflow portion above 100
        val afterFirst = fresh.copy(
            goalWindowStartMillis = firstUpdate.nextWindowStartMillis,
            goalWindowCoins = firstUpdate.nextWindowCoins
        )
        val crossingUpdate = afterFirst.computeGoalProgressUpdate(earnedCoins = 25L, nowMillis = day1Afternoon + 10_000L)
        assertEquals(day1OneAm, crossingUpdate.nextWindowStartMillis)
        assertEquals(110L, crossingUpdate.nextWindowCoins)
        assertEquals(50L, crossingUpdate.potDeltaHundredths) // 10 overflow * 5 = 0.50 coins
        assertTrue(crossingUpdate.goalCompletedAfterAction)

        // Post-goal earnings stay active at 00:30 AM (before 1:00 AM reset)
        val beforeOneAmProfile = afterFirst.copy(
            goalWindowCoins = 1600L,
            extraEarnPotHundredths = 7672L // 76.72 coins in pot
        )
        assertTrue(beforeOneAmProfile.isGoalWindowActive(day2ZeroThirtyAm))
        assertEquals(1600L, beforeOneAmProfile.activeGoalCoins(day2ZeroThirtyAm))
        assertFalse(beforeOneAmProfile.needsOneAmAutoClaim(day2ZeroThirtyAm))

        // Only 76 whole coins can be claimed from 76.72 (NOT 76.72)
        assertEquals(76L, beforeOneAmProfile.extraEarnPotWholeCoins())
        assertEquals(72L, beforeOneAmProfile.extraEarnPotRemainderHundredths())
        assertEquals("76.72", beforeOneAmProfile.extraEarnPotFormatted())

        // At 01:00 AM: goal resets to 0 and unclaimed whole coins (76) require auto-claim
        assertFalse(beforeOneAmProfile.isGoalWindowActive(day2OneAm))
        assertEquals(0L, beforeOneAmProfile.activeGoalCoins(day2OneAm))
        assertTrue(beforeOneAmProfile.needsOneAmAutoClaim(day2OneAm))

        val resetEarnUpdate = beforeOneAmProfile.computeGoalProgressUpdate(earnedCoins = 10L, nowMillis = day2OneAm)
        assertEquals(day2OneAm, resetEarnUpdate.nextWindowStartMillis)
        assertEquals(10L, resetEarnUpdate.nextWindowCoins)
        assertEquals(0L, resetEarnUpdate.potDeltaHundredths)

        // After 1:00 AM auto-claim completes for day2OneAm, needsOneAmAutoClaim clears
        val afterAutoClaim = beforeOneAmProfile.copy(
            goalWindowStartMillis = day2OneAm,
            goalWindowCoins = 0L,
            extraEarnPotHundredths = 72L // 0.72 left after claiming 76 whole coins
        )
        assertFalse(afterAutoClaim.needsOneAmAutoClaim(day2OneAm))
    }

    @Test
    fun extraEarnPotFormatsAndSplitsWholeAndFractionalCoinsAccurately() {
        val zeroPot = UserProfile(extraEarnPotHundredths = 0L)
        assertEquals(0L, zeroPot.extraEarnPotWholeCoins())
        assertEquals(0L, zeroPot.extraEarnPotRemainderHundredths())
        assertEquals("0.00", zeroPot.extraEarnPotFormatted())

        val partialPot = UserProfile(extraEarnPotHundredths = 90L)
        assertEquals(0L, partialPot.extraEarnPotWholeCoins())
        assertEquals(90L, partialPot.extraEarnPotRemainderHundredths())
        assertEquals("0.90", partialPot.extraEarnPotFormatted())

        val claimablePot = UserProfile(extraEarnPotHundredths = 7672L)
        assertEquals(76L, claimablePot.extraEarnPotWholeCoins())
        assertEquals(72L, claimablePot.extraEarnPotRemainderHundredths())
        assertEquals("76.72", claimablePot.extraEarnPotFormatted())
    }

    @Test
    fun mathQuizGeneratesFiveEasyQuestionsCoveringAllFourOperations() {
        assertEquals(5, MATH_QUIZ_QUESTIONS_PER_LEVEL)
        assertEquals(10L, MATH_QUIZ_COINS)

        val questions = MathQuizEngine.generateLevelQuestions(levelNumber = 1, random = Random(123))
        assertEquals(5, questions.size)

        val opsSeen = questions.map { it.operation }.toSet()
        assertTrue(opsSeen.containsAll(MathOperation.entries))

        questions.forEach { q ->
            assertEquals(4, q.options.size)
            assertEquals(4, q.options.toSet().size)
            assertTrue(q.options.contains(q.correctAnswer))
            assertTrue(q.options.all { it >= 0 })

            val computed = when (q.operation) {
                MathOperation.ADD -> q.leftOperand + q.rightOperand
                MathOperation.SUBTRACT -> q.leftOperand - q.rightOperand
                MathOperation.MULTIPLY -> q.leftOperand * q.rightOperand
                MathOperation.DIVIDE -> {
                    assertEquals(0, q.leftOperand % q.rightOperand)
                    q.leftOperand / q.rightOperand
                }
            }
            assertEquals(computed, q.correctAnswer)
        }
    }

    @Test
    fun tapTapTargetAndMiniGameRewardsAreConsistent() {
        assertEquals(30, TAP_TAP_DURATION_SECONDS)
        assertEquals(100, TAP_TAP_TARGET_TAPS)
        assertEquals(5L, TAP_TAP_COINS)

        assertFalse(MathQuizEngine.isTapTapGoalReached(99))
        assertTrue(MathQuizEngine.isTapTapGoalReached(100))
        assertTrue(MathQuizEngine.isTapTapGoalReached(135))

        assertEquals(5L, MiniGameType.fromRaw("TAP_TAP").rewardCoins)
        assertEquals(10L, MiniGameType.fromRaw("MATH_QUIZ").rewardCoins)
        assertEquals(5L, MiniGameType.fromRaw("TIC_TAC_TOE").rewardCoins)

        // 10-coin Math Quiz reward after 24h goal completed stores 0.50 coins (50 hundredths) in Extra Earn pot
        val completedGoalProfile = UserProfile(
            goalWindowStartMillis = 1_700_000_000_000L,
            goalWindowCoins = 100L
        )
        val quizGoalUpdate = completedGoalProfile.computeGoalProgressUpdate(
            earnedCoins = MATH_QUIZ_COINS,
            nowMillis = 1_700_000_010_000L
        )
        assertEquals(110L, quizGoalUpdate.nextWindowCoins)
        assertEquals(50L, quizGoalUpdate.potDeltaHundredths)
    }

    @Test
    fun unlimitedDailyLevelsAndAdMobOnlySourceBehavior() {
        val today = "2026-11-21"
        val heavyPlayer = UserProfile(
            dailyGamesPlayed = 25L,
            lastPlayedDate = today
        )
        assertEquals(25L, heavyPlayer.effectiveDailyGamesPlayed(today))
        assertEquals(26, heavyPlayer.ticTacToeNextLevel(today))

        // Different date resets daily count back to 0 / level 1
        assertEquals(0L, heavyPlayer.effectiveDailyGamesPlayed("2026-11-22"))
        assertEquals(1, heavyPlayer.ticTacToeNextLevel("2026-11-22"))

        // AdSource maps valid enum names and safely defaults legacy/unknown to ADMOB_TEST
        assertEquals(AdSource.ADMOB_TEST, AdSource.fromRaw("ADMOB_TEST"))
        assertEquals(AdSource.PENDING, AdSource.fromRaw("PENDING"))
        assertEquals(AdSource.ADMOB_TEST, AdSource.fromRaw("BUILTIN_TEST"))
        assertEquals(AdSource.ADMOB_TEST, AdSource.fromRaw("UNKNOWN_LEGACY"))
    }

    @Test
    fun watchThreeAdsBundleAwardsFifteenCoinsAndClampsCycleProgress() {
        assertEquals(3L, WATCH_AD_BUNDLE_COUNT)
        assertEquals(15L, WATCH_AD_BUNDLE_COINS)

        assertEquals(0, UserProfile(bonusAdCycleProgress = -1L).clampedWatchAdProgress())
        assertEquals(0, UserProfile(bonusAdCycleProgress = 0L).clampedWatchAdProgress())
        assertEquals(1, UserProfile(bonusAdCycleProgress = 1L).clampedWatchAdProgress())
        assertEquals(2, UserProfile(bonusAdCycleProgress = 2L).clampedWatchAdProgress())
        // Safeguards out-of-range values above 2
        assertEquals(2, UserProfile(bonusAdCycleProgress = 5L).clampedWatchAdProgress())

        // After hitting 100-coin daily goal, +15 coins from watching 3 ads adds 0.75 coins (75 hundredths) to Extra Earn pot
        val goalCompleted = UserProfile(
            goalWindowStartMillis = 1_700_000_000_000L,
            goalWindowCoins = 100L
        )
        val update = goalCompleted.computeGoalProgressUpdate(
            earnedCoins = WATCH_AD_BUNDLE_COINS,
            nowMillis = 1_700_000_010_000L
        )
        assertEquals(115L, update.nextWindowCoins)
        assertEquals(75L, update.potDeltaHundredths)
    }

    @Test
    fun whatsAppSupportUrlSanitizesPhoneAndEncodesPrefill() {
        assertEquals("9779822871917", sanitizeWhatsAppDigits("+977 982-287-1917"))
        assertEquals("", sanitizeWhatsAppDigits("abc-xyz"))

        val validUrl = buildWhatsAppSupportUrl("+977 982-287-1917", "Hello NexaEarn Team")
        assertEquals("https://wa.me/9779822871917?text=Hello+NexaEarn+Team", validUrl)

        assertNull(buildWhatsAppSupportUrl("12345", "Too short"))
        assertNull(buildWhatsAppSupportUrl("not-a-number", "Invalid"))

        val defaults = AppConfig()
        assertEquals(DEFAULT_WHATSAPP_NUMBER, defaults.whatsappSupportNumber)
        assertFalse(defaults.maintenanceMode)
        assertTrue(defaults.termsAndConditions.contains("NexaEarn"))
    }

    @Test
    fun adminPasswordVerifierRejectsBlankAndWrongInputs() {
        assertNull(verifyAdminPasswordAndComputeProof(""))
        assertNull(verifyAdminPasswordAndComputeProof("wrong-password-123"))
        assertNull(verifyAdminPasswordAndComputeProof("admin123"))

        // SHA-256 produces deterministic 64-char lowercase hex digests
        val sampleDigest = sha256Hex("hello")
        assertEquals(
            "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
            sampleDigest
        )
        assertEquals(64, ADMIN_UI_VERIFY_HASH.length)
    }

    @Test
    fun adMobUnitIdValidationAndAccessControlDefaultsWorkCorrectly() {
        assertTrue(isValidAdMobUnitId(DEFAULT_ADMOB_REWARDED_UNIT_ID))
        assertTrue(isValidAdMobUnitId(DEFAULT_ADMOB_BANNER_UNIT_ID))
        assertTrue(isValidAdMobUnitId("  ca-app-pub-1234567890123456/1234567890  "))
        assertEquals(
            "ca-app-pub-1234567890123456/1234567890",
            normalizeAdMobUnitId("  ca-app-pub-1234567890123456/1234567890  ")
        )

        assertFalse(isValidAdMobUnitId(""))
        assertFalse(isValidAdMobUnitId("ca-app-pub-123/456"))
        assertFalse(isValidAdMobUnitId("not-an-admob-unit"))

        val config = AppConfig()
        assertEquals(DEFAULT_ADMOB_REWARDED_UNIT_ID, config.rewardedAdUnitId)
        assertEquals(DEFAULT_ADMOB_BANNER_UNIT_ID, config.bannerAdUnitId)
        assertTrue(config.savedRewardedAdUnitIds.contains(DEFAULT_ADMOB_REWARDED_UNIT_ID))
        assertTrue(config.savedBannerAdUnitIds.contains(DEFAULT_ADMOB_BANNER_UNIT_ID))

        val user = UserProfile()
        assertFalse(user.banned)
        assertFalse(user.whitelisted)
        assertNull(user.banReason)
    }
}
