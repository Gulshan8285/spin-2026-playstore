package com.spinwin.rewards.data.model

import com.spinwin.rewards.components.UserTier

data class UserProfile(
    val uid: String = "",
    val name: String = "Player",
    val email: String = "",
    val phone: String = "",
    val photoUrl: String = "",
    val points: Int = 0,
    val balanceRupees: Double = 0.0,
    val tier: UserTier = UserTier.BRONZE,
    val referralCode: String = "SPIN8829",
    val referredBy: String? = null,
    val spinsToday: Int = 0,
    val maxDailySpins: Int = 10,
    val quizzesToday: Int = 0,
    val adsToday: Int = 0,
    val lastSpinTime: Long = 0,
    val lastLoginClaim: Long = 0,
    val streakDays: Int = 1,
    val cardNumber: String = "5412 •••• •••• 9921",
    val validThru: String = "12/28",
    val cvv: String = "689",
    val age: String = "",
    val countryCode: String = "US"
)

data class TransactionRecord(
    val txId: String = "",
    val uid: String = "",
    val type: String = "spin", // spin, quiz, ad, referral, withdraw
    val title: String = "",
    val pointsChange: Int = 0,
    val amountRupees: Double = 0.0,
    val status: String = "completed", // completed, pending, failed
    val timestamp: Long = System.currentTimeMillis(),
    val iconEmoji: String = "🪙"
)

data class WithdrawalRequest(
    val wdId: String = "",
    val uid: String = "",
    val amountRupees: Double = 0.0,
    val pointsDeducted: Int = 0,
    val method: String = "UPI", // UPI or Bank
    val payoutAddress: String = "", // UPI ID or Bank A/C
    val status: String = "pending", // pending, approved, rejected
    val requestedAt: Long = System.currentTimeMillis(),
    val processedAt: Long? = null
)

data class QuizQuestion(
    val id: String = "",
    val category: String = "Tech",
    val question: String = "",
    val options: List<String> = emptyList(),
    val correctIndex: Int = 0,
    val hint: String = ""
)

data class QuizCategory(
    val id: String = "",
    val name: String = "",
    val icon: String = "🧠",
    val questionCount: String = "Unlimited",
    val difficultyStars: Int = 3,
    val rewardPoints: Int = 20
)

data class LeaderboardUser(
    val uid: String = "",
    val rank: Int = 1,
    val name: String = "",
    val points: Int = 0,
    val avatarUrl: String = ""
)

data class AppConfig(
    val pointsPerRupee: Int = 1000, // 100 pts = 10 paise = 0.10 Rs
    val minWithdrawalPoints: Int = 100, // Minimum 100 pts / 10 paise test or ₹5
    val minWithdrawalRupees: Double = 5.0,
    val adRewardPoints: Int = 100, // 100 Points
    val adRewardRupees: Double = 0.10, // 10 Paise cash (100 Pts = ₹0.10)
    val spinCooldownSeconds: Int = 600,
    val dailyLoginReward: Int = 50
)
