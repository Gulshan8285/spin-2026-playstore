package com.spinwin.rewards.data.local

import android.content.Context
import android.content.SharedPreferences
import com.spinwin.rewards.components.UserTier
import com.spinwin.rewards.data.model.TransactionRecord
import com.spinwin.rewards.data.model.UserProfile
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * OfflineCacheManager:
 * Enterprise Multi-User Cache Manager.
 * Stores each unique user (by Email) in an isolated sandbox so that:
 * 1. A new Google sign-in starts with STRICTLY 0 points and ₹0.00 cash.
 * 2. An existing user gets their own accumulated points restored.
 * 3. User A never sees User B's points, transactions, or UPI balance.
 */
class OfflineCacheManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("spinwin_cache", Context.MODE_PRIVATE)
    private val authPrefs: SharedPreferences = context.getSharedPreferences("spinwin_auth_session", Context.MODE_PRIVATE)

    init {
        val version = prefs.getInt("cache_version", 0)
        if (version < 4) {
            prefs.edit().putInt("cache_version", 4).apply()
        }
    }

    private fun userKey(email: String): String {
        return "usr_" + email.trim().lowercase().replace(".", "_").replace("@", "_at_")
    }

    private fun txKey(email: String): String {
        return "tx_" + email.trim().lowercase().replace(".", "_").replace("@", "_at_")
    }

    fun hasUser(email: String): Boolean {
        if (email.isBlank()) return false
        return prefs.contains(userKey(email))
    }

    fun getActiveEmail(): String? {
        return prefs.getString("current_active_email", null)?.ifBlank { null }
    }

    fun setActiveEmail(email: String) {
        prefs.edit().putString("current_active_email", email.trim().lowercase()).apply()
    }

    fun saveUserProfile(user: UserProfile) {
        if (user.email.isBlank()) return

        val emailNorm = user.email.trim().lowercase()
        val key = userKey(emailNorm)

        val json = JSONObject().apply {
            put("uid", user.uid)
            put("name", user.name)
            put("email", user.email)
            put("phone", user.phone)
            put("photoUrl", user.photoUrl)
            put("points", user.points)
            put("balance", user.balanceRupees)
            put("tier", user.tier.name)
            put("referralCode", user.referralCode)
            put("spinsToday", user.spinsToday)
            put("streakDays", user.streakDays)
            put("age", user.age)
            put("countryCode", user.countryCode)
            put("upiId", user.upiId)
            put("status", user.status)
        }

        // Add email to registered users list
        val regList = getRegisteredEmails().toMutableSet()
        regList.add(emailNorm)
        val regArray = JSONArray()
        regList.forEach { regArray.put(it) }

        prefs.edit().apply {
            putString(key, json.toString())
            putString("registered_emails_json", regArray.toString())
            putString("current_active_email", emailNorm)
            // Backward compatibility top-level keys
            putString("uid", user.uid)
            putString("name", user.name)
            putString("email", user.email)
            putString("phone", user.phone)
            putInt("points", user.points)
            putFloat("balance", user.balanceRupees.toFloat())
            putString("upiId", user.upiId)
            apply()
        }
    }

    fun createNewUserProfile(
        email: String,
        name: String,
        photoUrl: String = "",
        phone: String = "",
        age: String = ""
    ): UserProfile {
        val uniqueUid = "usr_" + UUID.randomUUID().toString().take(10)
        val freshProfile = UserProfile(
            uid = uniqueUid,
            name = name,
            email = email,
            phone = phone,
            photoUrl = photoUrl,
            points = 0, // STRICT 0 POINTS
            balanceRupees = 0.0, // STRICT ₹0.00 CASH
            tier = UserTier.BRONZE,
            referralCode = "SPIN" + (1000..9999).random(),
            spinsToday = 0,
            maxDailySpins = 10,
            streakDays = 1,
            age = age,
            countryCode = "IN",
            upiId = "",
            status = "ACTIVE"
        )

        saveUserProfile(freshProfile)
        // Clear transactions for this fresh user
        prefs.edit().remove(txKey(email)).apply()
        return freshProfile
    }

    fun getUserProfile(email: String? = null): UserProfile? {
        val targetEmail = email?.trim()?.lowercase() ?: getActiveEmail()
        if (targetEmail != null) {
            val rawJson = prefs.getString(userKey(targetEmail), null)
            if (rawJson != null) {
                try {
                    val obj = JSONObject(rawJson)
                    return UserProfile(
                        uid = obj.optString("uid", "usr_" + targetEmail.hashCode()),
                        name = obj.optString("name", ""),
                        email = obj.optString("email", targetEmail),
                        phone = obj.optString("phone", ""),
                        photoUrl = obj.optString("photoUrl", ""),
                        points = obj.optInt("points", 0),
                        balanceRupees = obj.optDouble("balance", 0.0),
                        tier = try { UserTier.valueOf(obj.optString("tier", "BRONZE")) } catch (_: Exception) { UserTier.BRONZE },
                        referralCode = obj.optString("referralCode", "SPIN8829"),
                        spinsToday = obj.optInt("spinsToday", 0),
                        streakDays = obj.optInt("streakDays", 1),
                        age = obj.optString("age", ""),
                        countryCode = obj.optString("countryCode", "IN"),
                        upiId = obj.optString("upiId", ""),
                        status = obj.optString("status", "ACTIVE")
                    )
                } catch (_: Exception) {}
            }
        }

        // Legacy fallback
        val uid = prefs.getString("uid", null) ?: return null
        val legacyEmail = prefs.getString("email", "") ?: ""
        if (targetEmail != null && legacyEmail.isNotBlank() && legacyEmail != targetEmail) {
            return null // Do not bleed another user's legacy data into this target email!
        }

        return UserProfile(
            uid = uid,
            name = prefs.getString("name", "") ?: "",
            email = legacyEmail,
            phone = prefs.getString("phone", "") ?: "",
            photoUrl = prefs.getString("photoUrl", "") ?: "",
            points = prefs.getInt("points", 0),
            balanceRupees = prefs.getFloat("balance", 0.0f).toDouble(),
            referralCode = prefs.getString("referralCode", "SPIN8829") ?: "SPIN8829",
            spinsToday = prefs.getInt("spinsToday", 0),
            streakDays = prefs.getInt("streakDays", 1),
            age = prefs.getString("age", "") ?: "",
            countryCode = prefs.getString("countryCode", "IN") ?: "IN",
            upiId = prefs.getString("upiId", "") ?: "",
            status = "ACTIVE"
        )
    }

    fun getRegisteredEmails(): List<String> {
        val jsonStr = prefs.getString("registered_emails_json", null) ?: return emptyList()
        val list = mutableListOf<String>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                list.add(array.getString(i))
            }
        } catch (_: Exception) {}
        return list
    }

    fun clearActiveSession() {
        prefs.edit().remove("current_active_email").apply()
        authPrefs.edit().clear().apply()
    }

    fun clearAllData(context: Context) {
        prefs.edit().clear().putInt("cache_version", 4).apply()
        authPrefs.edit().clear().apply()
    }

    fun cacheTransactions(transactions: List<TransactionRecord>, email: String? = null) {
        val targetEmail = email?.trim()?.lowercase() ?: getActiveEmail() ?: return
        val jsonArray = JSONArray()
        transactions.take(50).forEach { tx ->
            val obj = JSONObject().apply {
                put("txId", tx.txId)
                put("title", tx.title)
                put("pointsChange", tx.pointsChange)
                put("amountRupees", tx.amountRupees)
                put("status", tx.status)
                put("timestamp", tx.timestamp)
                put("iconEmoji", tx.iconEmoji)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString(txKey(targetEmail), jsonArray.toString()).apply()
    }

    fun getCachedTransactions(email: String? = null): List<TransactionRecord> {
        val targetEmail = email?.trim()?.lowercase() ?: getActiveEmail() ?: return emptyList()
        val jsonStr = prefs.getString(txKey(targetEmail), null) ?: return emptyList()
        val list = mutableListOf<TransactionRecord>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    TransactionRecord(
                        txId = obj.optString("txId"),
                        title = obj.optString("title"),
                        pointsChange = obj.optInt("pointsChange"),
                        amountRupees = obj.optDouble("amountRupees"),
                        status = obj.optString("status"),
                        timestamp = obj.optLong("timestamp"),
                        iconEmoji = obj.optString("iconEmoji")
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }
}
