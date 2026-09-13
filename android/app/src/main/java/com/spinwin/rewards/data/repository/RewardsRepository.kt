package com.spinwin.rewards.data.repository

import android.content.Context
import com.spinwin.rewards.components.UserTier
import com.spinwin.rewards.data.local.OfflineCacheManager
import com.spinwin.rewards.data.model.AppConfig
import com.spinwin.rewards.data.model.CountryInfo
import com.spinwin.rewards.data.model.CountryRegistry
import com.spinwin.rewards.data.model.LeaderboardUser
import com.spinwin.rewards.data.model.QuizCategory
import com.spinwin.rewards.data.model.QuizQuestion
import com.spinwin.rewards.data.model.TransactionRecord
import com.spinwin.rewards.data.model.UserProfile
import com.spinwin.rewards.data.model.WithdrawalRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * RewardsRepository:
 * Single source of truth for user state, gamification activities, and real-time ledger.
 * Updated Earning Model:
 * - 100 Points = 10 Paise (₹0.10)
 * - Watch Video Ad (HIGH EARNING) = 100 Points + 20 Paise (₹0.20) cash instantly!
 * - Unlimited Quiz = 20 Points per correct answer with endless questions!
 */
class RewardsRepository(context: Context) {
    private val cacheManager = OfflineCacheManager(context)

    private val _userProfile = MutableStateFlow(
        UserProfile(
            uid = "user_new",
            name = "",
            email = "",
            phone = "",
            points = 0,
            balanceRupees = 0.0,
            tier = UserTier.BRONZE,
            referralCode = "SPIN8829",
            spinsToday = 0,
            maxDailySpins = 10,
            streakDays = 1,
            age = "",
            countryCode = "IN"
        )
    )
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private val _transactions = MutableStateFlow<List<TransactionRecord>>(emptyList())
    val transactions: StateFlow<List<TransactionRecord>> = _transactions.asStateFlow()

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _appConfig = MutableStateFlow(AppConfig())
    val appConfig: StateFlow<AppConfig> = _appConfig.asStateFlow()

    private val _activeCountry = MutableStateFlow(CountryRegistry.DEFAULT)
    val activeCountry: StateFlow<CountryInfo> = _activeCountry.asStateFlow()

    init {
        val cached = cacheManager.getUserProfile()
        if (cached != null) {
            _userProfile.value = cached
            _activeCountry.value = CountryRegistry.findByCode(cached.countryCode)
        } else {
            val detected = CountryRegistry.detectDefaultCountry()
            _userProfile.value = _userProfile.value.copy(countryCode = detected.code)
            _activeCountry.value = detected
        }
        val cachedTx = cacheManager.getCachedTransactions()
        _transactions.value = cachedTx
    }

    fun creditPoints(points: Int, title: String, emoji: String = "🪙", type: String = "reward", explicitCashRupees: Double? = null) {
        val current = _userProfile.value
        val newPoints = current.points + points
        // 100 points = 10 paise = 0.10 Rs => 0.001 Rs per point
        val cashDelta = explicitCashRupees ?: (points * 0.001)
        val newBalance = current.balanceRupees + cashDelta
        val newTier = when {
            newPoints >= 5000 -> UserTier.PLATINUM
            newPoints >= 2000 -> UserTier.GOLD
            newPoints >= 500 -> UserTier.SILVER
            else -> UserTier.BRONZE
        }

        val updated = current.copy(
            points = newPoints,
            balanceRupees = newBalance,
            tier = newTier
        )
        _userProfile.value = updated
        cacheManager.saveUserProfile(updated)

        val tx = TransactionRecord(
            txId = "tx_" + UUID.randomUUID().toString().take(8),
            uid = current.uid,
            type = type,
            title = title,
            pointsChange = points,
            amountRupees = cashDelta,
            status = "completed",
            timestamp = System.currentTimeMillis(),
            iconEmoji = emoji
        )
        val updatedList = listOf(tx) + _transactions.value
        _transactions.value = updatedList
        cacheManager.cacheTransactions(updatedList)
    }

    fun recordSpin(pointsWon: Int) {
        val current = _userProfile.value
        val updated = current.copy(
            spinsToday = current.spinsToday + 1,
            lastSpinTime = System.currentTimeMillis()
        )
        _userProfile.value = updated

        if (pointsWon > 0) {
            val cashValue = pointsWon * 0.001
            creditPoints(pointsWon, "Wheel Spin Won +$pointsWon Pts", "🎡", "spin", cashValue)
        } else {
            val tx = TransactionRecord(
                txId = "tx_" + UUID.randomUUID().toString().take(8),
                uid = current.uid,
                type = "spin",
                title = "Spin: Better luck next time!",
                pointsChange = 0,
                amountRupees = 0.0,
                status = "completed",
                timestamp = System.currentTimeMillis(),
                iconEmoji = "🎡"
            )
            _transactions.value = listOf(tx) + _transactions.value
        }
    }

    /**
     * HIGH EARNING Video Ad Reward:
     * Grants +100 Points AND +₹0.20 (20 Paise) cash directly!
     */
    fun watchAdReward(): Pair<Int, Double> {
        val pts = 100
        val cash = 0.10 // 10 Paise (100 Pts = ₹0.10)
        creditPoints(pts, "⚡ Sponsor Video Reward (+100 Pts)", "📺", "ad", cash)
        return Pair(pts, cash)
    }

    fun claimDailyCheckIn(): Boolean {
        val current = _userProfile.value
        val bonus = current.streakDays * 10
        val updated = current.copy(
            streakDays = current.streakDays + 1,
            lastLoginClaim = System.currentTimeMillis()
        )
        _userProfile.value = updated
        creditPoints(bonus, "Daily Check-In Day ${current.streakDays} Bonus", "🔥", "daily", bonus * 0.001)
        return true
    }

    fun submitWithdrawal(amountRupees: Double, upiOrBank: String, method: String = "PayPal"): Result<WithdrawalRequest> {
        val current = _userProfile.value
        val sym = _activeCountry.value.currencySymbol

        if (amountRupees < 1.0) {
            return Result.failure(Exception("Minimum withdrawal is $sym" + "1.00"))
        }
        if (current.balanceRupees < amountRupees) {
            return Result.failure(Exception("Insufficient balance in your Virtual ATM wallet"))
        }

        val pointsDeducted = (amountRupees * 1000).toInt()
        val updatedBalance = current.balanceRupees - amountRupees
        val updatedPoints = (current.points - pointsDeducted).coerceAtLeast(0)

        val updatedUser = current.copy(points = updatedPoints, balanceRupees = updatedBalance)
        _userProfile.value = updatedUser
        cacheManager.saveUserProfile(updatedUser)

        val req = WithdrawalRequest(
            wdId = "wd_" + UUID.randomUUID().toString().take(8),
            uid = current.uid,
            amountRupees = amountRupees,
            pointsDeducted = pointsDeducted,
            method = method,
            payoutAddress = upiOrBank,
            status = "pending",
            requestedAt = System.currentTimeMillis()
        )

        val tx = TransactionRecord(
            txId = req.wdId,
            uid = current.uid,
            type = "withdraw",
            title = "Payout to $method ($upiOrBank)",
            pointsChange = -pointsDeducted,
            amountRupees = -amountRupees,
            status = "pending",
            timestamp = System.currentTimeMillis(),
            iconEmoji = "💸"
        )
        _transactions.value = listOf(tx) + _transactions.value

        return Result.success(req)
    }

    fun updateCountry(country: CountryInfo) {
        _activeCountry.value = country
        val current = _userProfile.value
        val updated = current.copy(countryCode = country.code)
        _userProfile.value = updated
        cacheManager.saveUserProfile(updated)
    }

    fun updateGoogleUser(name: String, email: String, photoUrl: String) {
        val current = _userProfile.value
        val updated = current.copy(
            name = if (name.isNotBlank()) name else current.name,
            email = if (email.isNotBlank()) email else current.email,
            photoUrl = photoUrl
        )
        _userProfile.value = updated
        cacheManager.saveUserProfile(updated)
    }

    fun updateUserName(newName: String) {
        if (newName.isBlank()) return
        val current = _userProfile.value
        val updated = current.copy(name = newName.trim())
        _userProfile.value = updated
        cacheManager.saveUserProfile(updated)
    }

    fun updateUserDetails(name: String, phone: String, age: String, countryCode: String? = null) {
        val current = _userProfile.value
        val cCode = countryCode ?: current.countryCode
        val updated = current.copy(
            name = if (name.isNotBlank()) name.trim() else current.name,
            phone = if (phone.isNotBlank()) phone.trim() else current.phone,
            age = if (age.isNotBlank()) age.trim() else current.age,
            countryCode = cCode
        )
        _userProfile.value = updated
        _activeCountry.value = CountryRegistry.findByCode(cCode)
        cacheManager.saveUserProfile(updated)
    }

    fun getLeaderboard(): List<LeaderboardUser> {
        return listOf(
            LeaderboardUser("u_1", 1, "Rohan Sharma", 18450, ""),
            LeaderboardUser("u_2", 2, "Zoya Akhtar", 14200, ""),
            LeaderboardUser("u_3", 3, "Vikram Patel", 11800, ""),
            LeaderboardUser("u_4", 4, "${_userProfile.value.name} (You)", _userProfile.value.points, ""),
            LeaderboardUser("u_5", 5, "Priya Singh", 9400, ""),
            LeaderboardUser("u_6", 6, "Dev Malhotra", 8150, ""),
            LeaderboardUser("u_7", 7, "Ananya Roy", 7300, "")
        )
    }

    fun getQuizCategories(): List<QuizCategory> {
        return listOf(
            QuizCategory("tech", "Tech & Gaming", "💻", "Unlimited", 4, 20),
            QuizCategory("crypto", "Crypto & Money", "⚡", "Unlimited", 5, 20),
            QuizCategory("sports", "Cricket & Sports", "🏏", "Unlimited", 2, 20),
            QuizCategory("cinema", "Bollywood & Movies", "🎬", "Unlimited", 3, 20),
            QuizCategory("gk", "World GK & India", "🌍", "Unlimited", 3, 20),
            QuizCategory("math", "Brain Speed Math", "🔢", "Unlimited", 4, 20)
        )
    }

    fun getQuestionsForCategory(catId: String): List<QuizQuestion> {
        val pool = when (catId) {
            "sports" -> listOf(
                QuizQuestion("s1", catId, "Who has the record for highest individual score in ODI cricket?", listOf("Rohit Sharma (264)", "Virender Sehwag (219)", "Chris Gayle (215)", "Sachin Tendulkar (200)"), 0, "Hitman!"),
                QuizQuestion("s2", catId, "How many players are on the field in a cricket team?", listOf("9", "10", "11", "12"), 2, "Standard 11."),
                QuizQuestion("s3", catId, "Which country won the inaugural ICC T20 World Cup in 2007?", listOf("Pakistan", "India", "Australia", "West Indies"), 1, "MS Dhoni was captain."),
                QuizQuestion("s4", catId, "How many overs are bowled per side in a T20 match?", listOf("15", "20", "25", "50"), 1, "Twenty-twenty!"),
                QuizQuestion("s5", catId, "Who is known as the 'God of Cricket' in India?", listOf("Virat Kohli", "Sachin Tendulkar", "Kapil Dev", "Sunil Gavaskar"), 1, "Master Blaster 100 centuries."),
                QuizQuestion("s6", catId, "What is the maximum number of runs allowed per single normal ball without extras?", listOf("4", "6", "5", "8"), 1, "Over the boundary ropes.")
            )
            "cinema" -> listOf(
                QuizQuestion("c1", catId, "Which movie won the Oscar for Best Original Song with 'Naatu Naatu'?", listOf("RRR", "KGF Chapter 2", "Baahubali", "Pushpa"), 0, "Directed by SS Rajamouli."),
                QuizQuestion("c2", catId, "Who played the character of Kabir Singh in the Bollywood movie?", listOf("Shahid Kapoor", "Ranbir Kapoor", "Ranveer Singh", "Varun Dhawan"), 0, "Preeti!"),
                QuizQuestion("c3", catId, "Which film is highest-grossing Indian film of all time globally?", listOf("Dangal", "Baahubali 2", "RRR", "Jawan"), 0, "Aamir Khan wrestling drama."),
                QuizQuestion("c4", catId, "Who is known as the 'King of Bollywood'?", listOf("Salman Khan", "Shah Rukh Khan", "Aamir Khan", "Akshay Kumar"), 1, "Badshah of Bollywood."),
                QuizQuestion("c5", catId, "What was the name of Amitabh Bachchan's character in Sholay?", listOf("Jai", "Veeru", "Gabbar", "Thakur"), 0, "Jai and Veeru duo.")
            )
            "crypto" -> listOf(
                QuizQuestion("cr1", catId, "What was the very first cryptocurrency created?", listOf("Ethereum", "Bitcoin", "Dogecoin", "Ripple"), 1, "Created by Satoshi Nakamoto in 2009."),
                QuizQuestion("cr2", catId, "What is the total maximum supply of Bitcoin that will ever exist?", listOf("21 Million", "100 Million", "50 Million", "Unlimited"), 0, "Hardcap is 21M."),
                QuizQuestion("cr3", catId, "What technology forms the backbone of cryptocurrencies?", listOf("Cloud Computing", "Blockchain", "Artificial Intelligence", "Quantum Encryption"), 1, "Decentralized ledger."),
                QuizQuestion("cr4", catId, "What is the smallest unit of a Bitcoin called?", listOf("Bit", "Satoshi", "Gwei", "Finney"), 1, "Named after its creator."),
                QuizQuestion("cr5", catId, "Which cryptocurrency network introduced smart contracts?", listOf("Bitcoin", "Ethereum", "Solana", "Cardano"), 1, "Vitalik Buterin's network.")
            )
            "math" -> listOf(
                QuizQuestion("m1", catId, "What is 15 x 6 + 10?", listOf("90", "100", "110", "85"), 1, "15x6=90, +10=100."),
                QuizQuestion("m2", catId, "What is the square root of 144?", listOf("10", "11", "12", "14"), 2, "12 x 12 = 144."),
                QuizQuestion("m3", catId, "If a car travels at 60 km/h, how far does it go in 2.5 hours?", listOf("120 km", "150 km", "180 km", "200 km"), 1, "60 x 2.5 = 150."),
                QuizQuestion("m4", catId, "What is 25% of 200?", listOf("40", "50", "60", "25"), 1, "One quarter of 200 is 50."),
                QuizQuestion("m5", catId, "What is the value of 2 to the power of 5 (2^5)?", listOf("16", "24", "32", "64"), 2, "2, 4, 8, 16, 32.")
            )
            else -> listOf(
                QuizQuestion("t1", catId, "Which Google operating system powers most mobile smartphones?", listOf("Android", "ChromeOS", "Fuchsia", "WearOS"), 0, "World's most popular mobile OS."),
                QuizQuestion("t2", catId, "What does UPI stand for in digital Indian payments?", listOf("Unified Payments Interface", "Universal Payment ID", "United Pay India", "Unique Personal Index"), 0, "Instant mobile payment interface."),
                QuizQuestion("t3", catId, "Which company developed the Flutter & Jetpack Compose toolkits?", listOf("Apple", "Google", "Microsoft", "Meta"), 1, "Mountain View tech giant."),
                QuizQuestion("t4", catId, "What does HTTP stand for in web browsing?", listOf("HyperText Transfer Protocol", "Hyper Transfer Text Path", "High Tech Transport Page", "Hyperlink Text Program"), 0, "Standard web protocol."),
                QuizQuestion("t5", catId, "How many bytes are there in 1 Kilobyte (KB)?", listOf("512", "1000", "1024", "2048"), 2, "Binary 2^10 = 1024 bytes."),
                QuizQuestion("t6", catId, "Which company created the ChatGPT AI assistant?", listOf("Google", "OpenAI", "Microsoft", "Amazon"), 1, "Sam Altman's company.")
            )
        }
        return pool.shuffled()
    }

    companion object {
        @Volatile
        private var instance: RewardsRepository? = null

        fun getInstance(context: Context): RewardsRepository {
            return instance ?: synchronized(this) {
                instance ?: RewardsRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
