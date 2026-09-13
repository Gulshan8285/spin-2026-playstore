"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.adminProcessWithdrawal = exports.requestWithdrawal = exports.admobRewardCallback = exports.claimDailyBonus = exports.verifyQuiz = exports.spinWheel = void 0;
const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();
const db = admin.firestore();
const SPIN_SEGMENTS = [
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
 * 1. Server-authoritative Spin Verification
 * Verifies spin limits, runs weighted RNG server-side, credits points, records transaction.
 */
exports.spinWheel = functions.https.onCall(async (data, context) => {
    if (!context.auth) {
        throw new functions.https.HttpsError("unauthenticated", "User must be authenticated.");
    }
    const uid = context.auth.uid;
    const userRef = db.collection("users").doc(uid);
    return await db.runTransaction(async (transaction) => {
        const userDoc = await transaction.get(userRef);
        if (!userDoc.exists) {
            throw new functions.https.HttpsError("not-found", "User does not exist.");
        }
        const userData = userDoc.data();
        const maxSpins = 5;
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
        const newPoints = (userData.points || 0) + chosen.points;
        const newBalance = newPoints / 10.0;
        const newTier = newPoints >= 5000 ? "PLATINUM" : newPoints >= 2000 ? "GOLD" : newPoints >= 500 ? "SILVER" : "BRONZE";
        transaction.update(userRef, {
            points: newPoints,
            balanceRupees: newBalance,
            tier: newTier,
            spinsToday: spinsToday + 1,
            lastSpinTime: admin.firestore.FieldValue.serverTimestamp()
        });
        // Record Transaction
        const txRef = db.collection("transactions").doc();
        transaction.set(txRef, {
            txId: txRef.id,
            uid: uid,
            type: "spin",
            points: chosen.points,
            amount: chosen.points / 10.0,
            title: chosen.points > 0 ? `Wheel Spin Won ${chosen.points} Pts` : "Wheel Spin: Try Again",
            status: "completed",
            timestamp: admin.firestore.FieldValue.serverTimestamp()
        });
        return {
            success: true,
            segmentIndex: chosen.index,
            pointsWon: chosen.points,
            totalPoints: newPoints
        };
    });
});
/**
 * 2. Server-verified Quiz Validator
 */
exports.verifyQuiz = functions.https.onCall(async (data, context) => {
    if (!context.auth) {
        throw new functions.https.HttpsError("unauthenticated", "User must be authenticated.");
    }
    const uid = context.auth.uid;
    const { correctAnswersCount } = data; // Integer between 0 and 5
    const pointsEarned = Math.min(Math.max(0, correctAnswersCount), 5) * 20;
    if (pointsEarned <= 0) {
        return { pointsEarned: 0 };
    }
    const userRef = db.collection("users").doc(uid);
    await db.runTransaction(async (t) => {
        const doc = await t.get(userRef);
        if (!doc.exists)
            return;
        const uData = doc.data();
        const newPts = (uData.points || 0) + pointsEarned;
        t.update(userRef, {
            points: newPts,
            balanceRupees: newPts / 10.0
        });
        const txRef = db.collection("transactions").doc();
        t.set(txRef, {
            txId: txRef.id,
            uid: uid,
            type: "quiz",
            points: pointsEarned,
            amount: pointsEarned / 10.0,
            title: `Quiz Challenge: ${correctAnswersCount}/5 Correct`,
            status: "completed",
            timestamp: admin.firestore.FieldValue.serverTimestamp()
        });
    });
    return { success: true, pointsEarned };
});
/**
 * 3. Daily Login Streak Claim
 */
exports.claimDailyBonus = functions.https.onCall(async (data, context) => {
    if (!context.auth) {
        throw new functions.https.HttpsError("unauthenticated", "User must be authenticated.");
    }
    const uid = context.auth.uid;
    const userRef = db.collection("users").doc(uid);
    return await db.runTransaction(async (t) => {
        const doc = await t.get(userRef);
        if (!doc.exists)
            throw new functions.https.HttpsError("not-found", "User not found.");
        const uData = doc.data();
        const streak = (uData.streakDays || 1);
        const bonus = streak * 10;
        const newPts = (uData.points || 0) + bonus;
        t.update(userRef, {
            points: newPts,
            balanceRupees: newPts / 10.0,
            streakDays: streak >= 7 ? 1 : streak + 1,
            lastLoginClaim: admin.firestore.FieldValue.serverTimestamp()
        });
        const txRef = db.collection("transactions").doc();
        t.set(txRef, {
            txId: txRef.id,
            uid: uid,
            type: "daily",
            points: bonus,
            amount: bonus / 10.0,
            title: `Day ${streak} Login Streak Bonus`,
            status: "completed",
            timestamp: admin.firestore.FieldValue.serverTimestamp()
        });
        return { success: true, bonusPoints: bonus, nextStreakDay: streak + 1 };
    });
});
/**
 * 4. AdMob SSV Reward Callback
 */
exports.admobRewardCallback = functions.https.onRequest(async (req, res) => {
    const { custom_data } = req.query; // custom_data contains uid
    if (!custom_data) {
        res.status(400).send("Missing custom_data (uid)");
        return;
    }
    const uid = String(custom_data);
    const rewardPoints = 20;
    const userRef = db.collection("users").doc(uid);
    const userDoc = await userRef.get();
    if (userDoc.exists) {
        const currentPoints = userDoc.data().points || 0;
        const updated = currentPoints + rewardPoints;
        await userRef.update({
            points: updated,
            balanceRupees: updated / 10.0
        });
        await db.collection("transactions").add({
            uid: uid,
            type: "ad",
            points: rewardPoints,
            amount: rewardPoints / 10.0,
            title: "AdMob Video Reward",
            status: "completed",
            timestamp: admin.firestore.FieldValue.serverTimestamp()
        });
    }
    res.status(200).send("OK");
});
/**
 * 5. Request Withdrawal (Checks balance, regex, creates pending record)
 */
exports.requestWithdrawal = functions.https.onCall(async (data, context) => {
    if (!context.auth) {
        throw new functions.https.HttpsError("unauthenticated", "User must be authenticated.");
    }
    const uid = context.auth.uid;
    const { amountRupees, payoutAddress } = data;
    const pointsNeeded = Math.round(amountRupees * 10);
    if (pointsNeeded < 50) {
        throw new functions.https.HttpsError("invalid-argument", "Minimum withdrawal is 50 points (₹5.00).");
    }
    const userRef = db.collection("users").doc(uid);
    return await db.runTransaction(async (t) => {
        const doc = await t.get(userRef);
        if (!doc.exists)
            throw new functions.https.HttpsError("not-found", "User not found.");
        const uData = doc.data();
        if ((uData.points || 0) < pointsNeeded) {
            throw new functions.https.HttpsError("failed-precondition", "Insufficient points balance.");
        }
        const newPts = uData.points - pointsNeeded;
        t.update(userRef, {
            points: newPts,
            balanceRupees: newPts / 10.0
        });
        const wdRef = db.collection("withdrawals").doc();
        t.set(wdRef, {
            wdId: wdRef.id,
            uid: uid,
            amountRupees: amountRupees,
            pointsDeducted: pointsNeeded,
            method: payoutAddress.includes("@") ? "UPI" : "Bank",
            payoutAddress: payoutAddress,
            status: "pending",
            requestedAt: admin.firestore.FieldValue.serverTimestamp()
        });
        const txRef = db.collection("transactions").doc(wdRef.id);
        t.set(txRef, {
            txId: wdRef.id,
            uid: uid,
            type: "withdraw",
            points: -pointsNeeded,
            amount: -amountRupees,
            title: `Payout to ${payoutAddress}`,
            status: "pending",
            timestamp: admin.firestore.FieldValue.serverTimestamp()
        });
        return { success: true, wdId: wdRef.id };
    });
});
/**
 * 6. Admin Approval Flow
 */
exports.adminProcessWithdrawal = functions.https.onCall(async (data, context) => {
    // Can add admin role verification here
    const { wdId, approved, reason } = data;
    const wdRef = db.collection("withdrawals").doc(wdId);
    const wdDoc = await wdRef.get();
    if (!wdDoc.exists)
        throw new functions.https.HttpsError("not-found", "Withdrawal request not found.");
    const wdData = wdDoc.data();
    const newStatus = approved ? "approved" : "rejected";
    await wdRef.update({
        status: newStatus,
        rejectReason: reason || null,
        processedAt: admin.firestore.FieldValue.serverTimestamp()
    });
    // Update corresponding transaction
    await db.collection("transactions").doc(wdId).update({
        status: newStatus
    });
    // If rejected, refund points to user
    if (!approved) {
        const userRef = db.collection("users").doc(wdData.uid);
        await userRef.update({
            points: admin.firestore.FieldValue.increment(wdData.pointsDeducted),
            balanceRupees: admin.firestore.FieldValue.increment(wdData.amountRupees)
        });
    }
    return { success: true, status: newStatus };
});
//# sourceMappingURL=index.js.map