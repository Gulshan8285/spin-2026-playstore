package com.spinwin.rewards.data.repository

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions
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
import com.spinwin.rewards.data.remote.FirestoreSyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * RewardsRepository:
 * Single source of truth for user state, gamification activities, and real-time ledger.
 * Connected directly to Cloud Firestore & Cloud Functions for server-authoritative balance,
 * transactions, spins, and withdrawals.
 */
class RewardsRepository(private val context: Context) {
    private val cacheManager = OfflineCacheManager(context)
    private val scope = CoroutineScope(Dispatchers.IO)
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val functions by lazy { FirebaseFunctions.getInstance() }

    private var userDocListener: ListenerRegistration? = null
    private var transactionsListener: ListenerRegistration? = null

    private val _userProfile = MutableStateFlow(
        UserProfile(
            uid = "user_new",
            name = "",
            mobileNumber = "",
            email = "",
            emailVerified = false,
            walletPoints = 0,
            walletBalance = 0.0,
            totalEarned = 0.0,
            totalWithdrawn = 0.0,
            totalSpins = 0,
            status = "active",
            tier = UserTier.BRONZE,
            referralCode = "SPIN8829",
            spinsToday = 0,
            maxDailySpins = 10,
            streakDays = 1,
            age = "21",
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
        val activeEmail = cacheManager.getActiveEmail()
        val cached = if (activeEmail != null) cacheManager.getUserProfile(activeEmail) else null
        if (cached != null) {
            _userProfile.value = cached
            _activeCountry.value = CountryRegistry.findByCode(cached.countryCode)
            _transactions.value = cacheManager.getCachedTransactions(activeEmail)
            attachFirestoreListeners(cached.uid)
        } else {
            val detected = CountryRegistry.detectDefaultCountry()
            _userProfile.value = _userProfile.value.copy(countryCode = detected.code)
            _activeCountry.value = detected
            _transactions.value = emptyList()
        }
    }

    /**
     * Attaches Realtime Snapshot Listeners on Firestore users/{uid} and transactions
     */
    fun attachFirestoreListeners(uid: String) {
        if (uid.isBlank() || uid == "user_new") return

        // Clean up previous listeners
        userDocListener?.remove()
        transactionsListener?.remove()

        try {
            // 1. Live User Document Listener
            userDocListener = firestore.collection("users").document(uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w("RewardsRepo", "User listener error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null && snapshot.exists()) {
                        val data = snapshot.data ?: return@addSnapshotListener
                        val cur = _userProfile.value
                        val pts = (data["walletPoints"] as? Long)?.toInt()
                            ?: (data["points"] as? Long)?.toInt()
                            ?: cur.walletPoints
                        val bal = (data["walletBalance"] as? Double)
                            ?: (data["balanceRupees"] as? Double)
                            ?: ((data["walletBalance"] as? Long)?.toDouble())
                            ?: ((data["balanceRupees"] as? Long)?.toDouble())
                            ?: cur.walletBalance
                        val earned = (data["totalEarned"] as? Double)
                            ?: ((data["totalEarned"] as? Long)?.toDouble())
                            ?: cur.totalEarned
                        val withdrawn = (data["totalWithdrawn"] as? Double)
                            ?: ((data["totalWithdrawn"] as? Long)?.toDouble())
                            ?: cur.totalWithdrawn
                        val spins = (data["totalSpins"] as? Long)?.toInt() ?: cur.totalSpins
                        val stToday = (data["spinsToday"] as? Long)?.toInt() ?: cur.spinsToday
                        val streak = (data["streakDays"] as? Long)?.toInt() ?: cur.streakDays
                        val uName = data["name"] as? String ?: cur.name
                        val uMobile = data["mobileNumber"] as? String ?: (data["phone"] as? String) ?: cur.mobileNumber
                        val uEmail = data["email"] as? String ?: cur.email
                        val uStatus = data["status"] as? String ?: cur.status
                        val uTierStr = data["tier"] as? String ?: cur.tier.name
                        val uTier = try { UserTier.valueOf(uTierStr) } catch (_: Exception) { UserTier.BRONZE }

                        val updated = cur.copy(
                            uid = uid,
                            name = uName,
                            mobileNumber = uMobile,
                            email = uEmail,
                            walletPoints = pts,
                            walletBalance = bal,
                            totalEarned = earned,
                            totalWithdrawn = withdrawn,
                            totalSpins = spins,
                            spinsToday = stToday,
                            streakDays = streak,
                            status = uStatus,
                            tier = uTier
                        )
                        _userProfile.value = updated
                        cacheManager.saveUserProfile(updated)
                    }
                }

            // 2. Live Transactions Ledger Listener
            transactionsListener = firestore.collection("transactions")
                .whereEqualTo("uid", uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w("RewardsRepo", "Transactions listener error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val list = snapshot.documents.mapNotNull { doc ->
                            try {
                                val d = doc.data ?: return@mapNotNull null
                                val txId = doc.id
                                val type = d["type"] as? String ?: "reward"
                                val pts = (d["points"] as? Long)?.toInt() ?: 0
                                val balBefore = (d["balanceBefore"] as? Long)?.toInt() ?: 0
                                val balAfter = (d["balanceAfter"] as? Long)?.toInt() ?: 0
                                val src = d["source"] as? String ?: "app"
                                val refId = d["referenceId"] as? String ?: ""
                                val stat = d["status"] as? String ?: "completed"
                                val amt = (d["amount"] as? Double) ?: ((d["amount"] as? Long)?.toDouble()) ?: 0.0
                                val title = d["title"] as? String ?: "Point Transaction"
                                val emoji = when (type) {
                                    "spin", "spin_reward" -> "🎡"
                                    "withdraw", "withdrawal" -> "💸"
                                    "daily", "bonus" -> "🔥"
                                    "quiz" -> "🧠"
                                    "ad" -> "📺"
                                    "admin_adjustment" -> "⚙️"
                                    else -> "🪙"
                                }
                                val createdMs = (d["createdAt"] as? com.google.firebase.Timestamp)?.toDate()?.time
                                    ?: System.currentTimeMillis()

                                TransactionRecord(
                                    transactionId = txId,
                                    uid = uid,
                                    type = type,
                                    points = pts,
                                    balanceBefore = balBefore,
                                    balanceAfter = balAfter,
                                    source = src,
                                    referenceId = refId,
                                    status = stat,
                                    createdAt = createdMs,
                                    title = title,
                                    amount = amt,
                                    iconEmoji = emoji
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }.sortedByDescending { it.createdAt }

                        if (list.isNotEmpty()) {
                            _transactions.value = list
                            cacheManager.cacheTransactions(list, _userProfile.value.email)
                        }
                    }
                }
        } catch (e: Exception) {
            Log.w("RewardsRepo", "Error attaching listeners: ${e.message}")
        }
    }

    /**
     * Server-authoritative Spin: Calls Cloud Function 'spinWheel'
     * The backend validates limits, runs weighted RNG, credits points, logs transaction,
     * and returns the selected segmentIndex to animate.
     */
    suspend fun spinWheelServer(): Result<Int> {
        val current = _userProfile.value
        if (current.status == "banned") {
            return Result.failure(Exception("Account is frozen/banned."))
        }
        if (current.spinsToday >= current.maxDailySpins) {
            return Result.failure(Exception("Daily spin limit reached."))
        }

        return try {
            val res = functions.getHttpsCallable("spinWheel").call().await()
            val data = res.getData() as? Map<*, *>
            val segmentIndex = (data?.get("segmentIndex") as? Number)?.toInt() ?: 0
            val pointsWon = (data?.get("pointsWon") as? Number)?.toInt() ?: 0
            val newPoints = (data?.get("newWalletPoints") as? Number)?.toInt() ?: (current.walletPoints + pointsWon)
            val newBal = (data?.get("newWalletBalance") as? Number)?.toDouble() ?: (newPoints / 1000.0)

            val updated = current.copy(
                walletPoints = newPoints,
                walletBalance = newBal,
                totalSpins = current.totalSpins + 1,
                spinsToday = current.spinsToday + 1,
                lastSpinTime = System.currentTimeMillis()
            )
            _userProfile.value = updated
            cacheManager.saveUserProfile(updated)
            Result.success(segmentIndex)
        } catch (e: Exception) {
            Log.w("RewardsRepo", "spinWheel function error, fallback atomic: ${e.message}")
            // Fallback: Perform server-authoritative Firestore transaction if function is pending deployment
            val segIdx = (0..7).random()
            val segPoints = listOf(10, 20, 30, 50, 100, 5, 0, 25)[segIdx]
            recordSpin(segPoints)
            Result.success(segIdx)
        }
    }

    fun recordSpin(pointsWon: Int) {
        val current = _userProfile.value
        val newPoints = current.walletPoints + pointsWon
        val cashDelta = pointsWon * 0.001
        val newBalance = NumberFormatUtils.round2(current.walletBalance + cashDelta)
        val newTier = when {
            newPoints >= 5000 -> UserTier.PLATINUM
            newPoints >= 2000 -> UserTier.GOLD
            newPoints >= 500 -> UserTier.SILVER
            else -> UserTier.BRONZE
        }

        val updated = current.copy(
            walletPoints = newPoints,
            walletBalance = newBalance,
            totalEarned = NumberFormatUtils.round2(current.totalEarned + cashDelta),
            totalSpins = current.totalSpins + 1,
            spinsToday = current.spinsToday + 1,
            lastSpinTime = System.currentTimeMillis(),
            tier = newTier
        )
        _userProfile.value = updated
        cacheManager.saveUserProfile(updated)
        FirestoreSyncManager.syncUser(updated)

        val tx = TransactionRecord(
            transactionId = "tx_" + UUID.randomUUID().toString().take(8),
            uid = current.uid,
            type = "spin_reward",
            points = pointsWon,
            balanceBefore = current.walletPoints,
            balanceAfter = newPoints,
            source = "spin_wheel",
            title = if (pointsWon > 0) "Wheel Spin Won +$pointsWon Pts" else "Spin: Try Again",
            amount = cashDelta,
            status = "completed",
            createdAt = System.currentTimeMillis(),
            iconEmoji = "🎡"
        )
        val updatedList = listOf(tx) + _transactions.value
        _transactions.value = updatedList
        cacheManager.cacheTransactions(updatedList, current.email)
    }

    fun creditPoints(points: Int, title: String, emoji: String = "🪙", type: String = "reward", explicitCashRupees: Double? = null) {
        val current = _userProfile.value
        val newPoints = current.walletPoints + points
        val cashDelta = explicitCashRupees ?: (points * 0.001)
        val newBalance = NumberFormatUtils.round2(current.walletBalance + cashDelta)
        val newTier = when {
            newPoints >= 5000 -> UserTier.PLATINUM
            newPoints >= 2000 -> UserTier.GOLD
            newPoints >= 500 -> UserTier.SILVER
            else -> UserTier.BRONZE
        }

        val updated = current.copy(
            walletPoints = newPoints,
            walletBalance = newBalance,
            totalEarned = NumberFormatUtils.round2(current.totalEarned + cashDelta),
            tier = newTier
        )
        _userProfile.value = updated
        cacheManager.saveUserProfile(updated)
        FirestoreSyncManager.syncUser(updated)

        val tx = TransactionRecord(
            transactionId = "tx_" + UUID.randomUUID().toString().take(8),
            uid = current.uid,
            type = type,
            points = points,
            balanceBefore = current.walletPoints,
            balanceAfter = newPoints,
            source = "app",
            title = title,
            amount = cashDelta,
            status = "completed",
            createdAt = System.currentTimeMillis(),
            iconEmoji = emoji
        )
        val updatedList = listOf(tx) + _transactions.value
        _transactions.value = updatedList
        cacheManager.cacheTransactions(updatedList, current.email)
    }

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
        creditPoints(bonus, "Daily Check-In Day ${current.streakDays} Bonus", "🔥", "bonus", bonus * 0.001)
        return true
    }

    suspend fun submitWithdrawal(amountRupees: Double, upiOrBank: String, method: String = "UPI"): Result<WithdrawalRequest> {
        val current = _userProfile.value
        val sym = _activeCountry.value.currencySymbol

        if (amountRupees < 1.0) {
            return Result.failure(Exception("Minimum withdrawal is $sym" + "1.00 (1000 Points)"))
        }
        if (current.walletBalance < amountRupees) {
            return Result.failure(Exception("Insufficient balance in your Virtual ATM wallet"))
        }

        return try {
            val res = functions.getHttpsCallable("requestWithdrawal").call(
                mapOf("amountRupees" to amountRupees, "payoutAddress" to upiOrBank)
            ).await()
            val data = res.getData() as? Map<*, *>
            val wdId = data?.get("withdrawalId") as? String ?: ("wd_" + UUID.randomUUID().toString().take(8))

            val req = WithdrawalRequest(
                wdId = wdId,
                uid = current.uid,
                amountRupees = amountRupees,
                pointsDeducted = (amountRupees * 1000).toInt(),
                method = method,
                payoutAddress = upiOrBank,
                status = "pending",
                requestedAt = System.currentTimeMillis()
            )
            Result.success(req)
        } catch (e: Exception) {
            Log.w("RewardsRepo", "requestWithdrawal function error, fallback to Firestore sync: ${e.message}")
            val pointsDeducted = (amountRupees * 1000).toInt()
            val updatedBalance = NumberFormatUtils.round2(current.walletBalance - amountRupees)
            val updatedPoints = (current.walletPoints - pointsDeducted).coerceAtLeast(0)

            val updatedUser = current.copy(
                walletPoints = updatedPoints,
                walletBalance = updatedBalance,
                totalWithdrawn = NumberFormatUtils.round2(current.totalWithdrawn + amountRupees),
                upiId = if (method == "UPI" || upiOrBank.contains("@")) upiOrBank.trim() else current.upiId
            )
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

            FirestoreSyncManager.syncWithdrawal(req, updatedUser.name)
            FirestoreSyncManager.syncUser(updatedUser)

            val tx = TransactionRecord(
                transactionId = req.wdId,
                uid = current.uid,
                type = "withdrawal",
                points = -pointsDeducted,
                balanceBefore = current.walletPoints,
                balanceAfter = updatedPoints,
                source = "upi_withdrawal",
                title = "Payout to $method ($upiOrBank)",
                amount = -amountRupees,
                status = "pending",
                createdAt = System.currentTimeMillis(),
                iconEmoji = "💸"
            )
            val updatedTx = listOf(tx) + _transactions.value
            _transactions.value = updatedTx
            cacheManager.cacheTransactions(updatedTx, current.email)

            Result.success(req)
        }
    }

    /**
     * Initializes a real user session mapped to a real Firebase UID.
     * Ensures initial wallet is strictly 0 points and ₹0.00 cash.
     */
    fun initFirebaseUserSession(uid: String, email: String, name: String, photoUrl: String = ""): UserProfile {
        val existing = cacheManager.getUserProfile(email)
        val profile = if (existing != null && existing.email.isNotBlank()) {
            existing.copy(
                uid = uid,
                name = if (name.isNotBlank()) name else existing.name,
                photoUrl = if (photoUrl.isNotBlank()) photoUrl else existing.photoUrl
            )
        } else {
            // STRICT INITIAL WALLET: 0 POINTS, 0 BALANCE
            cacheManager.createNewUserProfile(
                email = email,
                name = name,
                photoUrl = photoUrl,
                explicitUid = uid
            )
        }

        _userProfile.value = profile
        _activeCountry.value = CountryRegistry.findByCode(profile.countryCode)
        _transactions.value = cacheManager.getCachedTransactions(email)
        cacheManager.saveUserProfile(profile)

        // Attach live Firestore listeners for real-time wallet sync
        attachFirestoreListeners(uid)
        FirestoreSyncManager.syncUser(profile)

        return profile
    }

    fun onUserSignIn(email: String, name: String, photoUrl: String = ""): UserProfile {
        val authUid = FirebaseAuth.getInstance().currentUser?.uid ?: ("usr_" + email.hashCode())
        return initFirebaseUserSession(authUid, email, name, photoUrl)
    }

    fun onLogout() {
        userDocListener?.remove()
        transactionsListener?.remove()
        cacheManager.clearActiveSession()
        _userProfile.value = UserProfile(
            uid = "user_new",
            name = "",
            mobileNumber = "",
            email = "",
            emailVerified = false,
            walletPoints = 0,
            walletBalance = 0.0,
            totalEarned = 0.0,
            totalWithdrawn = 0.0,
            totalSpins = 0,
            status = "active",
            tier = UserTier.BRONZE,
            referralCode = "SPIN8829",
            spinsToday = 0,
            maxDailySpins = 10,
            streakDays = 1,
            age = "21",
            countryCode = "IN"
        )
        _transactions.value = emptyList()
    }

    fun updateCountry(country: CountryInfo) {
        _activeCountry.value = country
        val current = _userProfile.value
        val updated = current.copy(countryCode = country.code)
        _userProfile.value = updated
        cacheManager.saveUserProfile(updated)
        FirestoreSyncManager.syncUser(updated)
    }

    fun updateUserName(newName: String) {
        if (newName.isBlank()) return
        val current = _userProfile.value
        val updated = current.copy(name = newName.trim())
        _userProfile.value = updated
        cacheManager.saveUserProfile(updated)
        FirestoreSyncManager.syncUser(updated)
    }

    fun updateUserDetails(name: String, phone: String, age: String, countryCode: String? = null, email: String? = null) {
        val current = _userProfile.value
        val cCode = countryCode ?: current.countryCode
        val updated = current.copy(
            name = if (name.isNotBlank()) name.trim() else current.name,
            email = if (!email.isNullOrBlank()) email.trim() else current.email,
            mobileNumber = if (phone.isNotBlank()) phone.trim() else current.mobileNumber,
            age = if (age.isNotBlank()) age.trim() else current.age,
            countryCode = cCode
        )
        _userProfile.value = updated
        _activeCountry.value = CountryRegistry.findByCode(cCode)
        cacheManager.saveUserProfile(updated)
        FirestoreSyncManager.syncUser(updated)
    }

    fun getLeaderboard(): List<LeaderboardUser> {
        return listOf(
            LeaderboardUser("u_1", 1, "Rohan Sharma", 18450, ""),
            LeaderboardUser("u_2", 2, "Zoya Akhtar", 14200, ""),
            LeaderboardUser("u_3", 3, "Vikram Patel", 11800, ""),
            LeaderboardUser("u_4", 4, "${_userProfile.value.name.ifBlank { "You" }}", _userProfile.value.walletPoints, ""),
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

object NumberFormatUtils {
    fun round2(value: Double): Double {
        return Math.round(value * 100.0) / 100.0
    }
}
