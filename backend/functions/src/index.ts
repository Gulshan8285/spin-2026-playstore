import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

admin.initializeApp();
const db = admin.firestore();

interface SpinSegment {
  index: number;
  points: number;
  weight: number;
}

const SPIN_SEGMENTS: SpinSegment[] = [
  { index: 0, points: 10, weight: 25 },
  { index: 1, points: 20, weight: 20 },
  { index: 2, points: 30, weight: 15 },
  { index: 3, points: 50, weight: 10 },
  { index: 4, points: 100, weight: 5 },
  { index: 5, points: 5, weight: 15 },
  { index: 6, points: 0, weight: 5 }, // Try again
  { index: 7, points: 25, weight: 5 }
];

/**
 * Helper to fetch financial settings (defaults to 1000 pts = ₹1.00 INR)
 */
async function getPointsToCurrencyRate(): Promise<number> {
  try {
    const doc = await db.collection("settings").doc("financial").get();
    if (doc.exists) {
      const data = doc.data();
      if (data && typeof data.pointsToCurrencyRate === "number" && data.pointsToCurrencyRate > 0) {
        return data.pointsToCurrencyRate;
      }
    }
  } catch (_) {}
  return 1000.0; // 1,000 Points = ₹1.00 INR (100 Pts = ₹0.10)
}

/**
 * 1. Server-authoritative Spin Execution
 * Enforces authenticated user, account status, daily spin limits, server-side weighted RNG,
 * atomic wallet point credit, balance update, and immutable transaction record.
 */
export const spinWheel = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "User must be authenticated.");
  }
  const uid = context.auth.uid;
  const userRef = db.collection("users").doc(uid);

  return await db.runTransaction(async (transaction) => {
    const userDoc = await transaction.get(userRef);
    if (!userDoc.exists) {
      throw new functions.https.HttpsError("not-found", "User account not found.");
    }
    const userData = userDoc.data()!;

    if (userData.status === "banned") {
      throw new functions.https.HttpsError("permission-denied", "Account is frozen/banned.");
    }

    const maxSpins = 10;
    const spinsToday = userData.spinsToday || 0;
    if (spinsToday >= maxSpins) {
      throw new functions.https.HttpsError("failed-precondition", "Daily spin limit reached.");
    }

    // Weighted Random Selection
    const totalWeight = SPIN_SEGMENTS.reduce((sum, s) => sum + s.weight, 0);
    let randomNum = Math.floor(Math.random() * totalWeight);
    let chosen = SPIN_SEGMENTS[0];
    for (const seg of SPIN_SEGMENTS) {
      if (randomNum < seg.weight) {
        chosen = seg;
        break;
      }
      randomNum -= seg.weight;
    }

    const pointsRate = 1000.0; // 1000 points = ₹1.00
    const balanceBeforePoints = Number(userData.walletPoints ?? userData.points ?? 0);
    const balanceBeforeRupees = Number(userData.walletBalance ?? userData.balanceRupees ?? 0.0);

    const newPoints = balanceBeforePoints + chosen.points;
    const cashDelta = chosen.points / pointsRate;
    const newBalance = Number((balanceBeforeRupees + cashDelta).toFixed(2));
    const totalEarned = Number(((userData.totalEarned || 0.0) + cashDelta).toFixed(2));
    const totalSpins = (userData.totalSpins || 0) + 1;
    const newTier = newPoints >= 5000 ? "PLATINUM" : newPoints >= 2000 ? "GOLD" : newPoints >= 500 ? "SILVER" : "BRONZE";

    const txRef = db.collection("transactions").doc();
    const spinRef = db.collection("spins").doc();

    transaction.update(userRef, {
      walletPoints: newPoints,
      walletBalance: newBalance,
      points: newPoints,
      balanceRupees: newBalance,
      totalEarned: totalEarned,
      totalSpins: totalSpins,
      tier: newTier,
      spinsToday: spinsToday + 1,
      lastSpinTime: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    });

    // Record Spin Attempt
    transaction.set(spinRef, {
      spinId: spinRef.id,
      uid: uid,
      segmentIndex: chosen.index,
      pointsWon: chosen.points,
      createdAt: admin.firestore.FieldValue.serverTimestamp()
    });

    // Record Immutable Transaction
    transaction.set(txRef, {
      transactionId: txRef.id,
      uid: uid,
      type: "spin_reward",
      points: chosen.points,
      balanceBefore: balanceBeforePoints,
      balanceAfter: newPoints,
      source: "spin_wheel",
      referenceId: spinRef.id,
      amount: cashDelta,
      title: chosen.points > 0 ? `Wheel Spin Won +${chosen.points} Pts` : "Wheel Spin: Try Again",
      status: "completed",
      createdAt: admin.firestore.FieldValue.serverTimestamp()
    });

    return {
      success: true,
      segmentIndex: chosen.index,
      pointsWon: chosen.points,
      newWalletPoints: newPoints,
      newWalletBalance: newBalance
    };
  });
});

/**
 * 2. Real User Profile Initializer
 * Creates an authoritative user document with exactly 0 points / ₹0.00 cash if not present.
 */
export const initializeUser = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "User must be authenticated.");
  }
  const uid = context.auth.uid;
  const { name, mobileNumber, email } = data;

  const userRef = db.collection("users").doc(uid);
  const userDoc = await userRef.get();

  if (userDoc.exists) {
    // Update last login
    await userRef.update({
      lastLoginAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      ...(name && { name }),
      ...(mobileNumber && { mobileNumber, phone: mobileNumber })
    });
    return { success: true, isNewUser: false, user: userDoc.data() };
  }

  // Create initial fresh user with strictly 0 points and 0 balance
  const initialUser = {
    uid: uid,
    name: name || "Player",
    mobileNumber: mobileNumber || "",
    phone: mobileNumber || "",
    email: email || context.auth.token.email || "",
    emailVerified: context.auth.token.email_verified || false,
    walletPoints: 0,
    walletBalance: 0.0,
    points: 0,
    balanceRupees: 0.0,
    totalEarned: 0.0,
    totalWithdrawn: 0.0,
    totalSpins: 0,
    status: "active",
    tier: "BRONZE",
    referralCode: "SPIN" + Math.floor(1000 + Math.random() * 9000),
    spinsToday: 0,
    streakDays: 1,
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    lastLoginAt: admin.firestore.FieldValue.serverTimestamp()
  };

  await userRef.set(initialUser);
  return { success: true, isNewUser: true, user: initialUser };
});

/**
 * 3. Server-authoritative Withdrawal Request
 * Validates balance, deducts points atomically, creates withdrawal & transaction records.
 */
export const requestWithdrawal = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "User must be authenticated.");
  }
  const uid = context.auth.uid;
  const { amountRupees, payoutAddress } = data;

  if (typeof amountRupees !== "number" || amountRupees <= 0) {
    throw new functions.https.HttpsError("invalid-argument", "Invalid withdrawal amount.");
  }
  if (!payoutAddress || typeof payoutAddress !== "string") {
    throw new functions.https.HttpsError("invalid-argument", "Valid UPI ID or account required.");
  }

  const pointsRate = await getPointsToCurrencyRate();
  const pointsNeeded = Math.round(amountRupees * pointsRate);

  if (pointsNeeded < 1000) { // Minimum 1000 points = ₹1.00
    throw new functions.https.HttpsError("invalid-argument", `Minimum withdrawal is 1000 points (₹${(1000 / pointsRate).toFixed(2)}).`);
  }

  const userRef = db.collection("users").doc(uid);

  return await db.runTransaction(async (t) => {
    const userDoc = await t.get(userRef);
    if (!userDoc.exists) {
      throw new functions.https.HttpsError("not-found", "User account not found.");
    }
    const userData = userDoc.data()!;

    if (userData.status === "banned") {
      throw new functions.https.HttpsError("permission-denied", "Account is frozen/banned.");
    }

    const currentPoints = Number(userData.walletPoints ?? userData.points ?? 0);
    if (currentPoints < pointsNeeded) {
      throw new functions.https.HttpsError("failed-precondition", "Insufficient points balance.");
    }

    const newPoints = currentPoints - pointsNeeded;
    const newBalance = Number((newPoints / pointsRate).toFixed(2));
    const totalWithdrawn = Number(((userData.totalWithdrawn || 0.0) + amountRupees).toFixed(2));

    const wdRef = db.collection("withdrawals").doc();
    const txRef = db.collection("transactions").doc();

    t.update(userRef, {
      walletPoints: newPoints,
      walletBalance: newBalance,
      points: newPoints,
      balanceRupees: newBalance,
      totalWithdrawn: totalWithdrawn,
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    });

    t.set(wdRef, {
      withdrawalId: wdRef.id,
      id: wdRef.id,
      uid: uid,
      userName: userData.name || "Player",
      amountINR: amountRupees,
      pointsDeducted: pointsNeeded,
      method: payoutAddress.includes("@") ? "UPI" : "Bank Transfer",
      payoutAddress: payoutAddress,
      upiId: payoutAddress,
      status: "pending",
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      requestedAt: admin.firestore.FieldValue.serverTimestamp()
    });

    t.set(txRef, {
      transactionId: txRef.id,
      uid: uid,
      type: "withdrawal",
      points: -pointsNeeded,
      balanceBefore: currentPoints,
      balanceAfter: newPoints,
      source: "upi_withdrawal",
      referenceId: wdRef.id,
      amount: -amountRupees,
      title: `Payout Request to ${payoutAddress}`,
      status: "pending",
      createdAt: admin.firestore.FieldValue.serverTimestamp()
    });

    return {
      success: true,
      withdrawalId: wdRef.id,
      newWalletPoints: newPoints,
      newWalletBalance: newBalance
    };
  });
});

/**
 * 4. Admin Point Adjustment with Mandatory Audit Logging
 */
export const adminAdjustPoints = functions.https.onCall(async (data, context) => {
  const { targetUid, pointsDelta, reason, masterSecret } = data;

  // Verify master secret or admin custom claim
  const isMasterAuthorized = masterSecret === "SpinWin@2026";
  const isAdminClaim = context.auth?.token?.admin === true;

  if (!isMasterAuthorized && !isAdminClaim) {
    throw new functions.https.HttpsError("permission-denied", "Unauthorized admin operation.");
  }

  if (!targetUid || typeof pointsDelta !== "number" || pointsDelta === 0) {
    throw new functions.https.HttpsError("invalid-argument", "Target UID and non-zero pointsDelta are required.");
  }
  if (!reason || typeof reason !== "string" || reason.trim().length < 3) {
    throw new functions.https.HttpsError("invalid-argument", "A valid reason (min 3 chars) is mandatory for audit trail.");
  }

  const adminUid = context.auth?.uid || "admin_master";
  const userRef = db.collection("users").doc(targetUid);
  const pointsRate = await getPointsToCurrencyRate();

  return await db.runTransaction(async (t) => {
    const userDoc = await t.get(userRef);
    if (!userDoc.exists) {
      throw new functions.https.HttpsError("not-found", "Target user does not exist.");
    }
    const userData = userDoc.data()!;
    const oldPoints = Number(userData.walletPoints ?? userData.points ?? 0);
    const oldBalance = Number(userData.walletBalance ?? userData.balanceRupees ?? 0.0);

    const newPoints = Math.max(0, oldPoints + pointsDelta);
    const newBalance = Number((newPoints / pointsRate).toFixed(2));

    const logRef = db.collection("auditLogs").doc();
    const txRef = db.collection("transactions").doc();

    t.update(userRef, {
      walletPoints: newPoints,
      walletBalance: newBalance,
      points: newPoints,
      balanceRupees: newBalance,
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    });

    t.set(logRef, {
      logId: logRef.id,
      adminUid: adminUid,
      targetUid: targetUid,
      targetUserName: userData.name || "Player",
      targetUserEmail: userData.email || "",
      oldPoints: oldPoints,
      newPoints: newPoints,
      pointsChanged: pointsDelta,
      oldBalance: oldBalance,
      newBalance: newBalance,
      reason: reason.trim(),
      transactionId: txRef.id,
      timestamp: admin.firestore.FieldValue.serverTimestamp()
    });

    t.set(txRef, {
      transactionId: txRef.id,
      uid: targetUid,
      type: "admin_adjustment",
      points: pointsDelta,
      balanceBefore: oldPoints,
      balanceAfter: newPoints,
      source: "admin_panel",
      referenceId: logRef.id,
      amount: Number((pointsDelta / pointsRate).toFixed(2)),
      title: `Admin Adjustment: ${reason.trim()}`,
      status: "completed",
      createdAt: admin.firestore.FieldValue.serverTimestamp()
    });

    return {
      success: true,
      newWalletPoints: newPoints,
      newWalletBalance: newBalance,
      logId: logRef.id
    };
  });
});

/**
 * 5. Admin User Status Update (Freeze / Activate)
 */
export const adminUpdateUserStatus = functions.https.onCall(async (data, context) => {
  const { targetUid, status, reason, masterSecret } = data;
  const isMasterAuthorized = masterSecret === "SpinWin@2026";
  const isAdminClaim = context.auth?.token?.admin === true;

  if (!isMasterAuthorized && !isAdminClaim) {
    throw new functions.https.HttpsError("permission-denied", "Unauthorized admin operation.");
  }
  if (!targetUid || (status !== "active" && status !== "banned")) {
    throw new functions.https.HttpsError("invalid-argument", "Valid targetUid and status ('active' or 'banned') required.");
  }

  const adminUid = context.auth?.uid || "admin_master";
  const userRef = db.collection("users").doc(targetUid);
  const logRef = db.collection("auditLogs").doc();

  await userRef.update({
    status: status,
    updatedAt: admin.firestore.FieldValue.serverTimestamp()
  });

  await logRef.set({
    logId: logRef.id,
    adminUid: adminUid,
    targetUid: targetUid,
    action: `USER_STATUS_${status.toUpperCase()}`,
    reason: reason || "Admin Status Modification",
    timestamp: admin.firestore.FieldValue.serverTimestamp()
  });

  return { success: true, status };
});

/**
 * 6. Admin Process Withdrawal Approval / Rejection
 */
export const adminProcessWithdrawal = functions.https.onCall(async (data, context) => {
  const { wdId, approved, reason, masterSecret } = data;
  const isMasterAuthorized = masterSecret === "SpinWin@2026";
  const isAdminClaim = context.auth?.token?.admin === true;

  if (!isMasterAuthorized && !isAdminClaim) {
    throw new functions.https.HttpsError("permission-denied", "Unauthorized admin operation.");
  }

  const wdRef = db.collection("withdrawals").doc(wdId);
  const wdDoc = await wdRef.get();
  if (!wdDoc.exists) {
    throw new functions.https.HttpsError("not-found", "Withdrawal request not found.");
  }

  const wdData = wdDoc.data()!;
  const newStatus = approved ? "approved" : "rejected";

  await wdRef.update({
    status: newStatus,
    rejectReason: reason || null,
    processedAt: admin.firestore.FieldValue.serverTimestamp()
  });

  // Update corresponding transaction
  await db.collection("transactions").doc(wdId).update({
    status: newStatus
  }).catch(() => {});

  // If rejected, refund points to user in transaction
  if (!approved) {
    const pointsRate = await getPointsToCurrencyRate();
    const userRef = db.collection("users").doc(wdData.uid);
    const refundPoints = Number(wdData.pointsDeducted || 0);
    const refundRupees = Number(wdData.amountINR || 0.0);

    await db.runTransaction(async (t) => {
      const uDoc = await t.get(userRef);
      if (uDoc.exists) {
        const curPts = Number(uDoc.data()!.walletPoints ?? uDoc.data()!.points ?? 0);
        const newPts = curPts + refundPoints;
        const newBal = Number((newPts / pointsRate).toFixed(2));
        const newWithdrawn = Math.max(0, (uDoc.data()!.totalWithdrawn || 0.0) - refundRupees);

        t.update(userRef, {
          walletPoints: newPts,
          walletBalance: newBal,
          points: newPts,
          balanceRupees: newBal,
          totalWithdrawn: newWithdrawn,
          updatedAt: admin.firestore.FieldValue.serverTimestamp()
        });

        const refundTxRef = db.collection("transactions").doc();
        t.set(refundTxRef, {
          transactionId: refundTxRef.id,
          uid: wdData.uid,
          type: "reversal",
          points: refundPoints,
          balanceBefore: curPts,
          balanceAfter: newPts,
          source: "withdrawal_refund",
          referenceId: wdId,
          amount: refundRupees,
          title: `Withdrawal Refund (${reason || "Rejected by Admin"})`,
          status: "completed",
          createdAt: admin.firestore.FieldValue.serverTimestamp()
        });
      }
    });
  }

  return { success: true, status: newStatus };
});
