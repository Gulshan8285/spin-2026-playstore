package com.spinwin.rewards.data.remote

import android.os.Build
import android.util.Log
import com.spinwin.rewards.data.model.UserProfile
import com.spinwin.rewards.data.model.WithdrawalRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * FirestoreSyncManager:
 * Lightweight, zero-dependency Firestore REST synchronizer.
 * Syncs user profile, live points, earnings, and withdrawal requests
 * directly to Google Cloud Firestore in the "spin-game-3f38a" project,
 * so every user and live balance appears instantly in the Web Admin Panel.
 */
object FirestoreSyncManager {
    private const val TAG = "FirestoreSync"
    private const val PROJECT_ID = "spin-game-3f38a"
    private const val API_KEY = "AIzaSyBt1gPq8dxTh1xSJkkte3WRtSj_w2y_iNw"
    private const val BASE_URL = "https://firestore.googleapis.com/v1/projects/$PROJECT_ID/databases/(default)/documents"

    private val scope = CoroutineScope(Dispatchers.IO)

    /**
     * Syncs user profile (Name, Email, Phone, UPI ID, Points, Balance, Device) to Firestore /users/{uid}
     */
    fun syncUser(user: UserProfile) {
        if (user.uid.isBlank() || user.uid == "user_new" || user.email.isBlank()) {
            return
        }

        scope.launch {
            try {
                val docId = sanitizeDocId(user.uid)
                val endpoint = "$BASE_URL/users/$docId?key=$API_KEY"
                val url = URL(endpoint)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "PATCH"
                    connectTimeout = 8000
                    readTimeout = 8000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                }

                val deviceName = "${Build.MANUFACTURER.capitalize(Locale.ROOT)} ${Build.MODEL}"
                val nowFormatted = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())

                val fields = JSONObject().apply {
                    put("uid", JSONObject().put("stringValue", user.uid))
                    put("name", JSONObject().put("stringValue", user.name.ifBlank { "Player" }))
                    put("email", JSONObject().put("stringValue", user.email))
                    put("phone", JSONObject().put("stringValue", user.phone))
                    put("upiId", JSONObject().put("stringValue", user.upiId))
                    put("points", JSONObject().put("integerValue", user.points.toString()))
                    put("balanceRupees", JSONObject().put("doubleValue", user.balanceRupees))
                    put("tier", JSONObject().put("stringValue", user.tier.name))
                    put("status", JSONObject().put("stringValue", user.status))
                    put("deviceModel", JSONObject().put("stringValue", deviceName))
                    put("referralCode", JSONObject().put("stringValue", user.referralCode))
                    put("streakDays", JSONObject().put("integerValue", user.streakDays.toString()))
                    put("spinsToday", JSONObject().put("integerValue", user.spinsToday.toString()))
                    put("lastActive", JSONObject().put("stringValue", "Active Now 🟢"))
                    put("registeredDate", JSONObject().put("stringValue", nowFormatted))
                    put("updatedAt", JSONObject().put("integerValue", System.currentTimeMillis().toString()))
                }

                val payload = JSONObject().apply {
                    put("fields", fields)
                }

                OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
                    writer.write(payload.toString())
                    writer.flush()
                }

                val responseCode = conn.responseCode
                if (responseCode in 200..299) {
                    Log.d(TAG, "Successfully synced user ${user.email} (Pts: ${user.points}, ₹${user.balanceRupees}) to Admin Panel / Firestore")
                } else {
                    val err = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "Code $responseCode"
                    Log.w(TAG, "Firestore sync returned $responseCode: $err")
                }
                conn.disconnect()
            } catch (e: Exception) {
                // Offline or Firestore waiting to be created - fails silently without crashing app
                Log.w(TAG, "Firestore sync skipped: ${e.message}")
            }
        }
    }

    /**
     * Syncs a withdrawal request to Firestore /withdrawals/{wdId}
     */
    fun syncWithdrawal(request: WithdrawalRequest, userName: String) {
        if (request.wdId.isBlank()) return

        scope.launch {
            try {
                val endpoint = "$BASE_URL/withdrawals/${request.wdId}?key=$API_KEY"
                val url = URL(endpoint)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "PATCH"
                    connectTimeout = 8000
                    readTimeout = 8000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                }

                val nowFormatted = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(request.requestedAt))

                val fields = JSONObject().apply {
                    put("id", JSONObject().put("stringValue", request.wdId))
                    put("uid", JSONObject().put("stringValue", request.uid))
                    put("userName", JSONObject().put("stringValue", userName))
                    put("amountINR", JSONObject().put("doubleValue", request.amountRupees))
                    put("pointsDeducted", JSONObject().put("integerValue", request.pointsDeducted.toString()))
                    put("method", JSONObject().put("stringValue", request.method))
                    put("upiId", JSONObject().put("stringValue", request.payoutAddress))
                    put("status", JSONObject().put("stringValue", request.status))
                    put("time", JSONObject().put("stringValue", nowFormatted))
                    put("isRealUser", JSONObject().put("booleanValue", true))
                }

                val payload = JSONObject().apply {
                    put("fields", fields)
                }

                OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
                    writer.write(payload.toString())
                    writer.flush()
                }

                val responseCode = conn.responseCode
                Log.d(TAG, "Withdrawal sync response code: $responseCode")
                conn.disconnect()
            } catch (e: Exception) {
                Log.w(TAG, "Withdrawal sync skipped: ${e.message}")
            }
        }
    }

    private fun sanitizeDocId(raw: String): String {
        return raw.replace("/", "_").replace(".", "_")
    }
}
