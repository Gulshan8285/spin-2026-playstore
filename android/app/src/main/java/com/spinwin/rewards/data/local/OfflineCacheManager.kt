package com.spinwin.rewards.data.local

import android.content.Context
import android.content.SharedPreferences
import com.spinwin.rewards.data.model.TransactionRecord
import com.spinwin.rewards.data.model.UserProfile
import org.json.JSONArray
import org.json.JSONObject

/**
 * OfflineCacheManager:
 * Caches user profile and up to 50 transactions locally in SharedPreferences,
 * queues actions when offline, and checks network sync status.
 */
class OfflineCacheManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("spinwin_cache", Context.MODE_PRIVATE)

    init {
        val version = prefs.getInt("cache_version", 0)
        if (version < 3) {
            prefs.edit().clear().putInt("cache_version", 3).apply()
            context.getSharedPreferences("spinwin_auth_session", Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply()
        }
    }

    fun clearAllData(context: Context) {
        prefs.edit().clear().putInt("cache_version", 3).apply()
        context.getSharedPreferences("spinwin_auth_session", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }

    fun saveUserProfile(user: UserProfile) {
        prefs.edit().apply {
            putString("uid", user.uid)
            putString("name", user.name)
            putString("email", user.email)
            putString("phone", user.phone)
            putString("photoUrl", user.photoUrl)
            putInt("points", user.points)
            putFloat("balance", user.balanceRupees.toFloat())
            putString("tier", user.tier.name)
            putString("referralCode", user.referralCode)
            putInt("spinsToday", user.spinsToday)
            putInt("streakDays", user.streakDays)
            putString("age", user.age)
            putString("countryCode", user.countryCode)
            apply()
        }
    }

    fun getUserProfile(): UserProfile? {
        val uid = prefs.getString("uid", null) ?: return null
        val name = prefs.getString("name", "") ?: ""
        val email = prefs.getString("email", "") ?: ""
        val phone = prefs.getString("phone", "") ?: ""
        val photoUrl = prefs.getString("photoUrl", "") ?: ""
        val points = prefs.getInt("points", 0)
        val balance = prefs.getFloat("balance", 0.0f).toDouble()
        val referralCode = prefs.getString("referralCode", "SPIN8829") ?: "SPIN8829"
        val spinsToday = prefs.getInt("spinsToday", 0)
        val streak = prefs.getInt("streakDays", 1)
        val age = prefs.getString("age", "") ?: ""
        val countryCode = prefs.getString("countryCode", "IN") ?: "IN"

        return UserProfile(
            uid = uid,
            name = name,
            email = email,
            phone = phone,
            photoUrl = photoUrl,
            points = points,
            balanceRupees = balance,
            referralCode = referralCode,
            spinsToday = spinsToday,
            streakDays = streak,
            age = age,
            countryCode = countryCode
        )
    }

    fun cacheTransactions(transactions: List<TransactionRecord>) {
        val jsonArray = JSONArray()
        // Cache up to 50 items
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
        prefs.edit().putString("cached_transactions", jsonArray.toString()).apply()
    }

    fun getCachedTransactions(): List<TransactionRecord> {
        val jsonStr = prefs.getString("cached_transactions", null) ?: return emptyList()
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
