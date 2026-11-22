const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_user_123";
const BOB_UID = "bob_user_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function buildInitialProfile(uid, now) {
  return {
    userId: uid,
    displayName: "Alice Player",
    email: "alice@example.com",
    coinsBalance: 0,
    totalCoinsEarned: 0,
    dailyGamesPlayed: 0,
    lastPlayedDate: "",
    currentStreak: 0,
    longestStreak: 0,
    lastStreakClaimDate: "",
    totalAdsWatched: 0,
    totalGamesWon: 0,
    totalGamesPlayed: 0,
    createdAt: now,
    updatedAt: now,
  };
}

test("Unauthenticated user cannot read or write users, matches, or rewards", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const now = new Date(Date.now() - 2000);

  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .set(buildInitialProfile(ALICE_UID, now))
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("matches").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("rewards").get()
  );
});

test("Owner can create initial profile and perform valid match, streak, and bonus ad updates", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);
  const userRef = aliceDb.collection("users").doc(ALICE_UID);

  // Initial create (zeros required)
  await assertSucceeds(userRef.set(buildInitialProfile(ALICE_UID, created)));

  // Reject profile create with pre-loaded fake coins
  await assertFails(
    aliceDb
      .collection("users")
      .doc("fake_uid_999")
      .set({
        ...buildInitialProfile("fake_uid_999", created),
        coinsBalance: 500,
      })
  );

  // Action: RecordMatchAndReward (+5 coins, 1st daily level)
  await assertSucceeds(
    userRef.update({
      coinsBalance: 5,
      totalCoinsEarned: 5,
      dailyGamesPlayed: 1,
      lastPlayedDate: "2026-11-20",
      totalAdsWatched: 1,
      totalGamesWon: 1,
      totalGamesPlayed: 1,
      updatedAt: new Date(Date.now() - 5000),
    })
  );

  // Action: ClaimStreakAd (+10 coins)
  await assertSucceeds(
    userRef.update({
      coinsBalance: 15,
      totalCoinsEarned: 15,
      currentStreak: 1,
      longestStreak: 1,
      lastStreakClaimDate: "2026-11-20",
      totalAdsWatched: 2,
      updatedAt: new Date(Date.now() - 3000),
    })
  );

  // Action: WatchBonusAd (+3 coins)
  await assertSucceeds(
    userRef.update({
      coinsBalance: 18,
      totalCoinsEarned: 18,
      totalAdsWatched: 3,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Security bounds allow unlimited (>10) daily levels while blocking coin tampering, negative daily counts, immutable changes, and ghost fields", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);
  const userRef = aliceDb.collection("users").doc(ALICE_UID);
  await assertSucceeds(userRef.set(buildInitialProfile(ALICE_UID, created)));

  // Reject inflated bonus coin jump (+500)
  await assertFails(
    userRef.update({
      coinsBalance: 500,
      totalCoinsEarned: 500,
      totalAdsWatched: 1,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Allow > 10 daily levels (unlimited daily Tic Tac Toe levels)
  await assertSucceeds(
    userRef.update({
      coinsBalance: 5,
      totalCoinsEarned: 5,
      dailyGamesPlayed: 25,
      lastPlayedDate: "2026-11-20",
      totalAdsWatched: 1,
      totalGamesWon: 1,
      totalGamesPlayed: 25,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject negative dailyGamesPlayed
  await assertFails(
    userRef.update({
      coinsBalance: 10,
      totalCoinsEarned: 10,
      dailyGamesPlayed: -1,
      lastPlayedDate: "2026-11-20",
      totalAdsWatched: 2,
      totalGamesWon: 2,
      totalGamesPlayed: 26,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject modifying immutable createdAt
  await assertFails(
    userRef.update({
      displayName: "New Name",
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject shadow/ghost field
  await assertFails(
    userRef.update({
      displayName: "New Name",
      isAdmin: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject future timestamp
  await assertFails(
    userRef.update({
      displayName: "Future",
      updatedAt: new Date(Date.now() + 60000),
    })
  );
});

test("Match & Reward subcollections enforce ownership, schema, and terminal adWatched lock", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 5000);

  // Create match where ad was initially skipped (0 coins)
  const matchRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("matches")
    .doc("match_1");

  await assertSucceeds(
    matchRef.set({
      userId: ALICE_UID,
      gameType: "TIC_TAC_TOE",
      botDifficulty: "MEDIUM",
      outcome: "WIN",
      levelNumber: 1,
      playedDate: "2026-11-20",
      coinsAwarded: 0,
      adWatched: false,
      adSource: "PENDING",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Claim post-match ad once
  await assertSucceeds(
    matchRef.update({
      adWatched: true,
      coinsAwarded: 5,
      adSource: "BUILTIN_TEST",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Terminal lock: cannot re-claim an already watched match
  await assertFails(
    matchRef.update({
      adWatched: true,
      coinsAwarded: 5,
      adSource: "ADMOB_TEST",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject match with @DocumentId 'id' inside payload
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("matches")
      .doc("match_bad")
      .set({
        id: "match_bad",
        userId: ALICE_UID,
        gameType: "TIC_TAC_TOE",
        botDifficulty: "MEDIUM",
        outcome: "DRAW",
        levelNumber: 2,
        playedDate: "2026-11-20",
        coinsAwarded: 5,
        adWatched: true,
        adSource: "BUILTIN_TEST",
        createdAt: now,
        updatedAt: now,
      })
  );

  // Create reward event
  const rewardRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("rewards")
    .doc("reward_1");
  await assertSucceeds(
    rewardRef.set({
      userId: ALICE_UID,
      rewardType: "MATCH_REWARD",
      coins: 5,
      adSource: "BUILTIN_TEST",
      note: "Tic Tac Toe Level 1 vs Medium Bot",
      createdAt: now,
    })
  );

  // Reward events are immutable
  await assertFails(rewardRef.update({ coins: 50 }));
  await assertFails(rewardRef.delete());

  // List queries succeed for owner
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("matches").get()
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("rewards").get()
  );

  // Bob cannot read or write Alice's profile, matches, or rewards
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("matches").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("rewards").get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("rewards")
      .doc("steal_1")
      .set({
        userId: ALICE_UID,
        rewardType: "TEST_AD",
        coins: 10,
        adSource: "BUILTIN_TEST",
        note: "Hijack",
        createdAt: now,
      })
  );
});

test("Withdrawals validate ownership, minimum 5000 coins, balance deduction, and immutability", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 10000);
  const userRef = aliceDb.collection("users").doc(ALICE_UID);

  await assertSucceeds(userRef.set(buildInitialProfile(ALICE_UID, created)));
  // Seed enough balance across streak/match updates using test helper sequence
  let bal = 0;
  for (let i = 1; i <= 85; i++) {
    bal += 60;
    await assertSucceeds(
      userRef.update({
        coinsBalance: bal,
        totalCoinsEarned: bal,
        currentStreak: i,
        longestStreak: i,
        lastStreakClaimDate: `2026-11-${String(i).padStart(2, "0")}`,
        totalAdsWatched: i,
        updatedAt: new Date(Date.now() - 5000),
      })
    );
  }

  // Valid balance deduction + withdrawal creation (5000 coins)
  await assertSucceeds(
    userRef.update({
      coinsBalance: bal - 5000,
      updatedAt: new Date(Date.now() - 2000),
    })
  );

  const withdrawalRef = userRef.collection("withdrawals").doc("wd_1");
  await assertSucceeds(
    withdrawalRef.set({
      userId: ALICE_UID,
      method: "BANK_ACCOUNT",
      accountTitle: "Global IME - Alice",
      destination: "0123456789",
      coins: 5000,
      status: "PENDING",
      createdAt: new Date(Date.now() - 2000),
    })
  );

  // Reject withdrawal under 5000 coins
  await assertFails(
    userRef.collection("withdrawals").doc("wd_small").set({
      userId: ALICE_UID,
      method: "SIM_RECHARGE",
      accountTitle: "NTC",
      destination: "9841000000",
      coins: 4999,
      status: "PENDING",
      createdAt: new Date(Date.now() - 1000),
    })
  );

  // Withdrawals are immutable and unreadable by Bob
  await assertFails(withdrawalRef.update({ status: "APPROVED" }));
  await assertFails(withdrawalRef.delete());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("withdrawals").get());
});

test("Referral system enforces code ownership, 5-game progression, and atomic +50 bonus claim", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 20000);
  const aliceCode = "NXALICE1";

  const aliceRef = aliceDb.collection("users").doc(ALICE_UID);
  const bobRef = bobDb.collection("users").doc(BOB_UID);

  await assertSucceeds(
    aliceRef.set({
      ...buildInitialProfile(ALICE_UID, created),
      referralCode: aliceCode,
      referralBonusEarned: 0,
      referralsCompletedCount: 0,
    })
  );
  await assertSucceeds(
    bobRef.set({
      ...buildInitialProfile(BOB_UID, created),
      displayName: "Bob Friend",
      referralCode: "NXBOB999",
      referralBonusEarned: 0,
      referralsCompletedCount: 0,
    })
  );

  // Register Alice's referral code entry
  await assertSucceeds(
    aliceDb.collection("referralCodes").doc(aliceCode).set({
      code: aliceCode,
      ownerUid: ALICE_UID,
      ownerName: "Alice Player",
      createdAt: created,
    })
  );

  // Listing all referral codes is forbidden (no scraping)
  await assertFails(bobDb.collection("referralCodes").get());

  // Bob looks up Alice's code and applies it atomically
  await assertSucceeds(
    bobDb.collection("referralCodes").doc(aliceCode).get()
  );
  const bobBatch = bobDb.batch();
  bobBatch.update(bobRef, {
    referredByUid: ALICE_UID,
    referredByCode: aliceCode,
    updatedAt: new Date(Date.now() - 15000),
  });
  bobBatch.set(bobDb.collection("referrals").doc(BOB_UID), {
    referrerUid: ALICE_UID,
    referrerCode: aliceCode,
    referrerName: "Alice Player",
    inviteeUid: BOB_UID,
    inviteeName: "Bob Friend",
    inviteeGamesCompleted: 0,
    targetGames: 5,
    bonusCoins: 50,
    status: "IN_PROGRESS",
    createdAt: new Date(Date.now() - 15000),
    updatedAt: new Date(Date.now() - 15000),
  });
  await assertSucceeds(bobBatch.commit());

  // Alice cannot pre-claim before Bob finishes 5 games
  await assertFails(
    aliceDb.collection("referrals").doc(BOB_UID).update({
      status: "CLAIMED",
      updatedAt: new Date(Date.now() - 10000),
    })
  );

  // Bob cannot jump 0 -> 5 in a single write
  await assertFails(
    bobDb.collection("referrals").doc(BOB_UID).update({
      inviteeGamesCompleted: 5,
      status: "QUALIFIED",
      updatedAt: new Date(Date.now() - 10000),
    })
  );

  // Bob progresses 1 game at a time up to 5 -> becomes QUALIFIED on game 5
  for (let g = 1; g <= 5; g++) {
    await assertSucceeds(
      bobDb.collection("referrals").doc(BOB_UID).update({
        inviteeGamesCompleted: g,
        status: g === 5 ? "QUALIFIED" : "IN_PROGRESS",
        updatedAt: new Date(Date.now() - (9000 - g * 1000)),
      })
    );
  }

  // Alice cannot forge +50 bonus coins without claiming the referral record
  await assertFails(
    aliceRef.update({
      coinsBalance: 50,
      totalCoinsEarned: 50,
      referralBonusEarned: 50,
      referralsCompletedCount: 1,
      lastClaimedReferralUid: BOB_UID,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Alice atomically claims the +50 referral bonus via batch/transaction
  const claimBatch = aliceDb.batch();
  claimBatch.update(aliceRef, {
    coinsBalance: 50,
    totalCoinsEarned: 50,
    referralBonusEarned: 50,
    referralsCompletedCount: 1,
    lastClaimedReferralUid: BOB_UID,
    updatedAt: new Date(Date.now() - 500),
  });
  claimBatch.update(aliceDb.collection("referrals").doc(BOB_UID), {
    status: "CLAIMED",
    updatedAt: new Date(Date.now() - 500),
  });
  claimBatch.set(aliceRef.collection("rewards").doc("ref_bonus_1"), {
    userId: ALICE_UID,
    rewardType: "REFERRAL_BONUS",
    coins: 50,
    adSource: "REFERRAL",
    note: "Referral bonus: Bob Friend completed 5 games",
    createdAt: new Date(Date.now() - 500),
  });
  await assertSucceeds(claimBatch.commit());

  // Cannot claim the same referral a second time
  const doubleClaimBatch = aliceDb.batch();
  doubleClaimBatch.update(aliceRef, {
    coinsBalance: 100,
    totalCoinsEarned: 100,
    referralBonusEarned: 100,
    referralsCompletedCount: 2,
    lastClaimedReferralUid: BOB_UID,
    updatedAt: new Date(Date.now() - 100),
  });
  doubleClaimBatch.update(aliceDb.collection("referrals").doc(BOB_UID), {
    status: "CLAIMED",
    updatedAt: new Date(Date.now() - 100),
  });
  await assertFails(doubleClaimBatch.commit());
});

test("24h 100-coin goal tracks qualifying earnings and allows exact ClaimExtraEarnPot", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 30000);
  const windowStart = Date.now() - 25000;
  const userRef = aliceDb.collection("users").doc(ALICE_UID);

  await assertSucceeds(
    userRef.set({
      ...buildInitialProfile(ALICE_UID, created),
      goalWindowStartMillis: 0,
      goalWindowCoins: 0,
      extraEarnPotHundredths: 0,
      totalExtraEarnClaimed: 0,
    })
  );

  // Reach 120 goal coins across two Day 7 streak claims (60 + 60 -> 20 coins overflow * 5 = 100 hundredths = 1 coin in Extra Earn)
  await assertSucceeds(
    userRef.update({
      coinsBalance: 60,
      totalCoinsEarned: 60,
      currentStreak: 7,
      longestStreak: 7,
      lastStreakClaimDate: "2026-11-19",
      totalAdsWatched: 1,
      goalWindowStartMillis: windowStart,
      goalWindowCoins: 60,
      extraEarnPotHundredths: 0,
      updatedAt: new Date(Date.now() - 20000),
    })
  );

  await assertSucceeds(
    userRef.update({
      coinsBalance: 120,
      totalCoinsEarned: 120,
      currentStreak: 8,
      longestStreak: 8,
      lastStreakClaimDate: "2026-11-20",
      totalAdsWatched: 2,
      goalWindowStartMillis: windowStart,
      goalWindowCoins: 120,
      extraEarnPotHundredths: 100,
      updatedAt: new Date(Date.now() - 15000),
    })
  );

  // Mini game match after goal (+5 coins -> +0.25 coins = +25 hundredths -> 125 total in pot)
  await assertSucceeds(
    userRef.update({
      coinsBalance: 125,
      totalCoinsEarned: 125,
      dailyGamesPlayed: 1,
      lastPlayedDate: "2026-11-20",
      totalAdsWatched: 3,
      totalGamesWon: 1,
      totalGamesPlayed: 1,
      goalWindowStartMillis: windowStart,
      goalWindowCoins: 125,
      extraEarnPotHundredths: 125,
      updatedAt: new Date(Date.now() - 10000),
    })
  );

  // Watch ad after goal (+3 coins -> +0.15 coins = +15 hundredths -> 140 total in pot)
  await assertSucceeds(
    userRef.update({
      coinsBalance: 128,
      totalCoinsEarned: 128,
      totalAdsWatched: 4,
      goalWindowStartMillis: windowStart,
      goalWindowCoins: 128,
      extraEarnPotHundredths: 140,
      updatedAt: new Date(Date.now() - 8000),
    })
  );

  // Reject inflating pot beyond 5% max delta (+500 on a Watch Ad)
  await assertFails(
    userRef.update({
      coinsBalance: 131,
      totalCoinsEarned: 131,
      totalAdsWatched: 5,
      goalWindowStartMillis: windowStart,
      goalWindowCoins: 131,
      extraEarnPotHundredths: 640,
      updatedAt: new Date(Date.now() - 6000),
    })
  );

  // Reject over-claiming Extra Earn pot (140 hundredths only allows 1 whole coin, not 2)
  await assertFails(
    userRef.update({
      coinsBalance: 130,
      totalCoinsEarned: 130,
      extraEarnPotHundredths: 0,
      totalExtraEarnClaimed: 2,
      updatedAt: new Date(Date.now() - 5000),
    })
  );

  // Valid claim: claim 1 whole coin from 140 hundredths -> leaves 40 hundredths in pot + logs EXTRA_EARN reward
  const claimExtraBatch = aliceDb.batch();
  claimExtraBatch.update(userRef, {
    coinsBalance: 129,
    totalCoinsEarned: 129,
    extraEarnPotHundredths: 40,
    totalExtraEarnClaimed: 1,
    updatedAt: new Date(Date.now() - 2000),
  });
  claimExtraBatch.set(userRef.collection("rewards").doc("extra_earn_1"), {
    userId: ALICE_UID,
    rewardType: "EXTRA_EARN",
    coins: 1,
    adSource: "EXTRA_EARN",
    note: "Claimed 1 coin from Extra Earn pot (5% goal bonus)",
    createdAt: new Date(Date.now() - 2000),
  });
  await assertSucceeds(claimExtraBatch.commit());

  // 1:00 AM goal reset & auto-claim: claim whole coin(s) while resetting goalWindowCoins to 0
  // Earn 100 more hundredths (20 coins after goal via streak) -> pot becomes 140 hundredths -> 1 AM claims 1 whole coin and resets goalWindowCoins = 0
  await assertSucceeds(
    userRef.update({
      coinsBalance: 149,
      totalCoinsEarned: 149,
      currentStreak: 9,
      longestStreak: 9,
      lastStreakClaimDate: "2026-11-21",
      totalAdsWatched: 5,
      goalWindowStartMillis: windowStart,
      goalWindowCoins: 148,
      extraEarnPotHundredths: 140,
      updatedAt: new Date(Date.now() - 1500),
    })
  );

  // Reject 1:00 AM auto-claim if trying to reset goalWindowCoins to non-zero
  await assertFails(
    userRef.update({
      coinsBalance: 150,
      totalCoinsEarned: 150,
      extraEarnPotHundredths: 40,
      totalExtraEarnClaimed: 2,
      goalWindowStartMillis: Date.now() - 1000,
      goalWindowCoins: 10,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  const oneAmResetBatch = aliceDb.batch();
  oneAmResetBatch.update(userRef, {
    coinsBalance: 150,
    totalCoinsEarned: 150,
    extraEarnPotHundredths: 40,
    totalExtraEarnClaimed: 2,
    goalWindowStartMillis: Date.now() - 1000,
    goalWindowCoins: 0,
    updatedAt: new Date(Date.now() - 1000),
  });
  oneAmResetBatch.set(userRef.collection("rewards").doc("extra_earn_one_am"), {
    userId: ALICE_UID,
    rewardType: "EXTRA_EARN",
    coins: 1,
    adSource: "EXTRA_EARN",
    note: "1:00 AM auto-claim: +1 whole coin from Extra Earn",
    createdAt: new Date(Date.now() - 1000),
  });
  await assertSucceeds(oneAmResetBatch.commit());
});

test("Tap Tap and Math Quiz mini games enforce exact 5-coin and 10-coin reward rules", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 20000);
  const userRef = aliceDb.collection("users").doc(ALICE_UID);
  await assertSucceeds(userRef.set(buildInitialProfile(ALICE_UID, created)));

  // Valid MATH_QUIZ (+10 coins) batch
  const quizBatch = aliceDb.batch();
  quizBatch.update(userRef, {
    coinsBalance: 10,
    totalCoinsEarned: 10,
    dailyGamesPlayed: 0,
    lastPlayedDate: "2026-11-21",
    totalAdsWatched: 1,
    totalGamesWon: 1,
    totalGamesPlayed: 1,
    goalWindowStartMillis: Date.now() - 10000,
    goalWindowCoins: 10,
    extraEarnPotHundredths: 0,
    updatedAt: new Date(Date.now() - 5000),
  });
  quizBatch.set(userRef.collection("matches").doc("quiz_1"), {
    userId: ALICE_UID,
    gameType: "MATH_QUIZ",
    botDifficulty: "EASY",
    outcome: "WIN",
    levelNumber: 1,
    playedDate: "2026-11-21",
    coinsAwarded: 10,
    adWatched: true,
    adSource: "BUILTIN_TEST",
    createdAt: created,
    updatedAt: new Date(Date.now() - 5000),
  });
  quizBatch.set(userRef.collection("rewards").doc("quiz_reward_1"), {
    userId: ALICE_UID,
    rewardType: "MATCH_REWARD",
    coins: 10,
    adSource: "BUILTIN_TEST",
    note: "Math Quiz Level 1 (5/5 Questions)",
    createdAt: new Date(Date.now() - 5000),
  });
  await assertSucceeds(quizBatch.commit());

  // Valid TAP_TAP pending -> claimed (+5 coins)
  const tapMatchRef = userRef.collection("matches").doc("tap_1");
  await assertSucceeds(
    tapMatchRef.set({
      userId: ALICE_UID,
      gameType: "TAP_TAP",
      botDifficulty: "ARCADE",
      outcome: "WIN",
      levelNumber: 1,
      playedDate: "2026-11-21",
      coinsAwarded: 0,
      adWatched: false,
      adSource: "PENDING",
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(
    tapMatchRef.update({
      adWatched: true,
      coinsAwarded: 5,
      adSource: "ADMOB_TEST",
      updatedAt: new Date(Date.now() - 2000),
    })
  );

  // Reject inflating TAP_TAP match to 10 coins (only MATH_QUIZ gets 10)
  await assertFails(
    userRef.collection("matches").doc("tap_bad").set({
      userId: ALICE_UID,
      gameType: "TAP_TAP",
      botDifficulty: "ARCADE",
      outcome: "WIN",
      levelNumber: 2,
      playedDate: "2026-11-21",
      coinsAwarded: 10,
      adWatched: true,
      adSource: "BUILTIN_TEST",
      createdAt: created,
      updatedAt: created,
    })
  );
});

test("Watch 3 Ads -> +15 Coins unlimited cycle validates progress steps and rejects premature coin jumps", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 30000);
  const userRef = aliceDb.collection("users").doc(ALICE_UID);
  await assertSucceeds(
    userRef.set({
      ...buildInitialProfile(ALICE_UID, created),
      bonusAdCycleProgress: 0,
    })
  );

  // Reject jumping coins on ad #1 (bonusAdCycleProgress = 1 must not increase coinsBalance)
  await assertFails(
    userRef.update({
      coinsBalance: 15,
      totalCoinsEarned: 15,
      totalAdsWatched: 1,
      bonusAdCycleProgress: 1,
      updatedAt: new Date(Date.now() - 25000),
    })
  );

  // Ad 1 of 3: progress -> 1, 0 coins added
  await assertSucceeds(
    userRef.update({
      totalAdsWatched: 1,
      bonusAdCycleProgress: 1,
      updatedAt: new Date(Date.now() - 20000),
    })
  );

  // Ad 2 of 3: progress -> 2, 0 coins added
  await assertSucceeds(
    userRef.update({
      totalAdsWatched: 2,
      bonusAdCycleProgress: 2,
      updatedAt: new Date(Date.now() - 15000),
    })
  );

  // Ad 3 of 3: resets bonusAdCycleProgress -> 0 and credits +15 coins + goal + reward doc
  const cycleBatch = aliceDb.batch();
  cycleBatch.update(userRef, {
    coinsBalance: 15,
    totalCoinsEarned: 15,
    totalAdsWatched: 3,
    bonusAdCycleProgress: 0,
    goalWindowStartMillis: Date.now() - 10000,
    goalWindowCoins: 15,
    extraEarnPotHundredths: 0,
    updatedAt: new Date(Date.now() - 10000),
  });
  cycleBatch.set(userRef.collection("rewards").doc("watch_bundle_1"), {
    userId: ALICE_UID,
    rewardType: "WATCH_AD",
    coins: 15,
    adSource: "ADMOB_TEST",
    note: "Completed 3 Google AdMob Ads (+15 coins)",
    createdAt: new Date(Date.now() - 10000),
  });
  await assertSucceeds(cycleBatch.commit());

  // Unlimited cycles: second cycle starts cleanly (progress -> 1 again)
  await assertSucceeds(
    userRef.update({
      totalAdsWatched: 4,
      bonusAdCycleProgress: 1,
      updatedAt: new Date(Date.now() - 5000),
    })
  );
});

test("Admin Panel enforces hash-verified access, live AppConfig, custom notifications, user coin adjust, and withdrawal approve/fail", async () => {
  const crypto = require("crypto");
  const adminUid = "admin_user_001";
  const adminDb = testEnv
    .authenticatedContext(adminUid, {
      email: "owner.test@example.com",
      email_verified: true,
    })
    .firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 10000);

  // Create normal user profiles
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(buildInitialProfile(ALICE_UID, now))
  );
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(BOB_UID).set({
      ...buildInitialProfile(BOB_UID, now),
      displayName: "Bob Wallet",
      email: "bob@example.com",
      coinsBalance: 6000,
      totalCoinsEarned: 6000,
    });
  });

  // Bob submits a 5000-coin withdrawal request (balance becomes 1000)
  const bobWdRef = bobDb.collection("users").doc(BOB_UID).collection("withdrawals").doc("wd_bob_1");
  const wdBatch = bobDb.batch();
  wdBatch.update(bobDb.collection("users").doc(BOB_UID), {
    coinsBalance: 1000,
    updatedAt: new Date(Date.now() - 8000),
  });
  wdBatch.set(bobWdRef, {
    userId: BOB_UID,
    method: "ESEWA_WALLET",
    accountTitle: "Bob",
    destination: "9800000001",
    coins: 5000,
    status: "PENDING",
    createdAt: new Date(Date.now() - 8000),
  });
  await assertSucceeds(wdBatch.commit());

  // Non-admin (Alice) CANNOT list all users, CANNOT adjust Bob's coins, CANNOT write appConfig or notifications, CANNOT approve withdrawal
  await assertFails(aliceDb.collection("users").get());
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).update({
      coinsBalance: 9999,
      totalCoinsEarned: 9999,
      updatedAt: new Date(Date.now() - 5000),
    })
  );
  await assertFails(
    aliceDb.collection("appConfig").doc("main").set({
      whatsappSupportNumber: "9779800000000",
      termsAndConditions: "Fake terms",
      maintenanceMode: false,
      updatedAt: new Date(Date.now() - 5000),
    })
  );
  await assertFails(
    aliceDb.collection("notifications").doc("fake_notif").set({
      title: "Hack",
      body: "Fake",
      targetType: "ALL",
      targetUid: "ALL",
      createdAt: new Date(Date.now() - 5000),
    })
  );
  await assertFails(
    aliceDb.collection("admins").doc(ALICE_UID).set({
      uid: ALICE_UID,
      passDigest: "0".repeat(64),
      createdAt: new Date(Date.now() - 5000),
    })
  );

  // Admin unlocks via SHA-256 passDigest matching rules digest
  const validPassDigest = crypto
    .createHash("sha256")
    .update("NexaAdminGate:Kanxa@9822871917")
    .digest("hex");

  await assertSucceeds(
    adminDb.collection("admins").doc(adminUid).set({
      uid: adminUid,
      passDigest: validPassDigest,
      createdAt: new Date(Date.now() - 4000),
    })
  );

  // Re-unlocking Admin Panel (e.g. after app restart without tapping Lock) must also succeed
  await assertSucceeds(
    adminDb.collection("admins").doc(adminUid).set({
      uid: adminUid,
      passDigest: validPassDigest,
      createdAt: new Date(Date.now() - 3500),
    })
  );

  // Updating /admins/{uid} with wrong passDigest still fails
  await assertFails(
    adminDb.collection("admins").doc(adminUid).set({
      uid: adminUid,
      passDigest: "0".repeat(64),
      createdAt: new Date(Date.now() - 3400),
    })
  );

  // Nobody can read /admins/{uid} directly
  await assertFails(adminDb.collection("admins").doc(adminUid).get());
  await assertFails(aliceDb.collection("admins").doc(adminUid).get());

  // Unlocked Admin can list all users and collectionGroup("withdrawals")
  await assertSucceeds(adminDb.collection("users").get());
  await assertSucceeds(adminDb.collectionGroup("withdrawals").get());

  // Admin adjusts Alice coins (+250) + writes ADMIN_ADJUSTMENT reward log
  const adjBatch = adminDb.batch();
  adjBatch.update(adminDb.collection("users").doc(ALICE_UID), {
    coinsBalance: 250,
    totalCoinsEarned: 250,
    updatedAt: new Date(Date.now() - 3000),
  });
  adjBatch.set(adminDb.collection("users").doc(ALICE_UID).collection("rewards").doc("admin_adj_1"), {
    userId: ALICE_UID,
    rewardType: "ADMIN_ADJUSTMENT",
    coins: 250,
    adSource: "ADMIN",
    note: "Admin bonus credit",
    createdAt: new Date(Date.now() - 3000),
  });
  await assertSucceeds(adjBatch.commit());

  // Normal user cannot spoof ADMIN_ADJUSTMENT reward event
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("rewards").doc("spoof_adj").set({
      userId: ALICE_UID,
      rewardType: "ADMIN_ADJUSTMENT",
      coins: 500,
      adSource: "ADMIN",
      note: "Spoof",
      createdAt: new Date(Date.now() - 2500),
    })
  );

  // Admin marks Bob's withdrawal FAILED and refunds 5000 coins
  const failBatch = adminDb.batch();
  failBatch.update(adminDb.collection("users").doc(BOB_UID).collection("withdrawals").doc("wd_bob_1"), {
    status: "FAILED",
    adminNote: "Invalid eSewa ID",
    processedByEmail: "owner.test@example.com",
    processedAt: new Date(Date.now() - 2000),
  });
  failBatch.update(adminDb.collection("users").doc(BOB_UID), {
    coinsBalance: 6000,
    updatedAt: new Date(Date.now() - 2000),
  });
  await assertSucceeds(failBatch.commit());

  // Admin saves live AppConfig (WhatsApp support + Terms & Conditions + Announcement)
  await assertSucceeds(
    adminDb.collection("appConfig").doc("main").set({
      whatsappSupportNumber: "9779822871917",
      whatsappPrefillMessage: "Hi NexaEarn Support!",
      termsAndConditions: "1. Fair play only.\n2. Minimum 5000 coins for withdrawal.",
      announcementTitle: "Weekend Bonus",
      announcementBody: "Earn +15 coins per 3 ads!",
      maintenanceMode: false,
      maintenanceMessage: "Maintenance in progress",
      supportHours: "9 AM - 9 PM",
      updatedByEmail: "owner.test@example.com",
      updatedAt: new Date(Date.now() - 1500),
    })
  );

  // Signed-in normal user CAN read live AppConfig
  await assertSucceeds(aliceDb.collection("appConfig").doc("main").get());

  // Admin sends a broadcast notification (ALL) and a targeted notification (Bob)
  await assertSucceeds(
    adminDb.collection("notifications").doc("notif_all_1").set({
      title: "App Live Update",
      body: "New 3-Ad +15 Coins event is live!",
      targetType: "ALL",
      targetUid: "ALL",
      targetEmail: null,
      sentByEmail: "owner.test@example.com",
      createdAt: new Date(Date.now() - 1000),
    })
  );
  await assertSucceeds(
    adminDb.collection("notifications").doc("notif_bob_1").set({
      title: "Payout Refunded",
      body: "5000 coins returned to your wallet.",
      targetType: "USER",
      targetUid: BOB_UID,
      targetEmail: "bob@example.com",
      sentByEmail: "owner.test@example.com",
      createdAt: new Date(Date.now() - 1000),
    })
  );

  // Alice can query ALL + ALICE_UID, and cannot read Bob's private notification
  await assertSucceeds(
    aliceDb.collection("notifications").where("targetUid", "in", ["ALL", ALICE_UID]).get()
  );
  await assertFails(aliceDb.collection("notifications").doc("notif_bob_1").get());
  await assertSucceeds(
    bobDb.collection("notifications").where("targetUid", "in", ["ALL", BOB_UID]).get()
  );
});

test("Admin Panel enforces user ban/blacklist, unban/whitelist, and live AdMob Unit IDs", async () => {
  const crypto = require("crypto");
  const adminUid = "admin_user_002";
  const adminDb = testEnv
    .authenticatedContext(adminUid, {
      email: "owner.test@example.com",
      email_verified: true,
    })
    .firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 10000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(buildInitialProfile(ALICE_UID, now))
  );
  await assertSucceeds(
    bobDb.collection("users").doc(BOB_UID).set(buildInitialProfile(BOB_UID, now))
  );

  const validPassDigest = crypto
    .createHash("sha256")
    .update("NexaAdminGate:Kanxa@9822871917")
    .digest("hex");

  await assertSucceeds(
    adminDb.collection("admins").doc(adminUid).set({
      uid: adminUid,
      passDigest: validPassDigest,
      createdAt: new Date(Date.now() - 8000),
    })
  );

  // Regular user Bob CANNOT ban or whitelist Alice, nor can Alice modify her own ban/whitelist fields
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      banned: true,
      updatedAt: new Date(Date.now() - 7000),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      whitelisted: true,
      updatedAt: new Date(Date.now() - 7000),
    })
  );

  // Admin CANNOT mark a user both banned=true and whitelisted=true simultaneously
  await assertFails(
    adminDb.collection("users").doc(ALICE_UID).update({
      banned: true,
      whitelisted: true,
      updatedAt: new Date(Date.now() - 6500),
    })
  );

  // Admin bans / blacklists Alice
  await assertSucceeds(
    adminDb.collection("users").doc(ALICE_UID).update({
      banned: true,
      whitelisted: false,
      banReason: "Multiple suspicious accounts",
      bannedByEmail: "owner.test@example.com",
      bannedAt: new Date(Date.now() - 6000),
      updatedAt: new Date(Date.now() - 6000),
    })
  );

  // Banned Alice can still read her profile to view ban reason,
  // but CANNOT update wallet/profile, CANNOT create matches, rewards, or withdrawals, and CANNOT unban herself
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      banned: false,
      updatedAt: new Date(Date.now() - 5000),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      displayName: "Renamed While Banned",
      updatedAt: new Date(Date.now() - 5000),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("matches").doc("banned_match").set({
      userId: ALICE_UID,
      gameType: "TIC_TAC_TOE",
      botDifficulty: "MEDIUM",
      outcome: "WIN",
      levelNumber: 1,
      playedDate: "2026-11-21",
      coinsAwarded: 5,
      adWatched: true,
      adSource: "ADMOB_TEST",
      createdAt: new Date(Date.now() - 5000),
      updatedAt: new Date(Date.now() - 5000),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("rewards").doc("banned_rew").set({
      userId: ALICE_UID,
      rewardType: "WATCH_AD",
      coins: 5,
      adSource: "ADMOB_TEST",
      note: "Should fail when banned",
      createdAt: new Date(Date.now() - 5000),
    })
  );

  // Admin unbans & whitelists Alice
  await assertSucceeds(
    adminDb.collection("users").doc(ALICE_UID).update({
      banned: false,
      whitelisted: true,
      banReason: null,
      bannedByEmail: null,
      bannedAt: null,
      updatedAt: new Date(Date.now() - 4000),
    })
  );

  // Whitelisted Alice can play and record matches again
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("matches").doc("unbanned_match").set({
      userId: ALICE_UID,
      gameType: "TIC_TAC_TOE",
      botDifficulty: "MEDIUM",
      outcome: "WIN",
      levelNumber: 1,
      playedDate: "2026-11-21",
      coinsAwarded: 5,
      adWatched: true,
      adSource: "ADMOB_TEST",
      createdAt: new Date(Date.now() - 3000),
      updatedAt: new Date(Date.now() - 3000),
    })
  );

  // Admin can change/add live AdMob Unit IDs for Rewarded & Banner ads
  await assertSucceeds(
    adminDb.collection("appConfig").doc("main").set({
      whatsappSupportNumber: "9779822871917",
      whatsappPrefillMessage: "Hi!",
      termsAndConditions: "Fair play terms.",
      maintenanceMode: false,
      rewardedAdUnitId: "ca-app-pub-1234567890123456/1111111111",
      bannerAdUnitId: "ca-app-pub-1234567890123456/2222222222",
      savedRewardedAdUnitIds: [
        "ca-app-pub-3940256099942544/5224354917",
        "ca-app-pub-1234567890123456/1111111111",
      ],
      savedBannerAdUnitIds: [
        "ca-app-pub-3940256099942544/6300978111",
        "ca-app-pub-1234567890123456/2222222222",
      ],
      updatedByEmail: "owner.test@example.com",
      updatedAt: new Date(Date.now() - 2000),
    })
  );

  // Admin can remove/disable active Rewarded or Banner Ad Unit IDs (set to empty string)
  await assertSucceeds(
    adminDb.collection("appConfig").doc("main").set({
      whatsappSupportNumber: "9779822871917",
      termsAndConditions: "Fair play terms.",
      maintenanceMode: false,
      rewardedAdUnitId: "",
      bannerAdUnitId: "",
      savedRewardedAdUnitIds: [],
      savedBannerAdUnitIds: [],
      updatedByEmail: "owner.test@example.com",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Malformed AdMob Unit ID is rejected by security rules
  await assertFails(
    adminDb.collection("appConfig").doc("main").set({
      whatsappSupportNumber: "9779822871917",
      termsAndConditions: "Fair play terms.",
      maintenanceMode: false,
      rewardedAdUnitId: "not-a-valid-admob-id",
      bannerAdUnitId: "",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});




