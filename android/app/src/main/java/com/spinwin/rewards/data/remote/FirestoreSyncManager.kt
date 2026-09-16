package com.spinwin.rewards.data.remote

import android.os.Build
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.spinwin.rewards.data.model.UserProfile
import com.spinwin.rewards.data.model.WithdrawalRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * FirestoreSyncManager:
 * Enterprise Dual-Channel Live Synchronizer for SpinWin Rewards.
 * Syncs user profile, live points, earnings, and withdrawal requests to:
 * 1. Official Google Firebase Firestore in the "spin-game-3f38a" project as real-time database.
 * 2. Global Live Cloud Relay as secondary zero-config relay.
 */
object FirestoreSyncManager {
    private const val TAG = "FirestoreSync"

    // 🌐 Channel 1: Live Cloud Relay (Fallback)
    private const val CLOUD_USERS_URL = "https://api.restful-api.dev/objects/ff808181a09d98f701a0a8c1970616f3"
    private const val CLOUD_WITHDRAWALS_URL = "https://api.restful-api.dev/objects/ff808181a09d98f701a0a8c1a52a16f4"

    // 🔥 Channel 2: Google Cloud Firestore REST API & SDK
    private const val PROJECT_ID = "spin-game-3f38a"
    private const val API_KEY = "AIzaSyBt1gPq8dxTh1xSJkkte3WRtSj_w2y_iNw"
    private const val FIRESTORE_BASE_URL = "https://firestore.googleapis.com/v1/projects/$PROJECT_ID/databases/(default)/documents"

    private val scope = CoroutineScope(Dispatchers.IO)

    /**
     * Syncs user profile (Name, Email, Phone, UPI ID, Points, Balance, Device) to Live Cloud & Firestore
     */
    fun syncUser(user: UserProfile) {
        val stableUid = if (user.uid.isNotBlank() && user.uid != "user_new") {
            user.uid
        } else if (user.email.isNotBlank()) {
            "u_" + user.email.replace("@", "_").replace(".", "_")
        } else if (user.mobileNumber.isNotBlank()) {
            "u_" + user.mobileNumber.replace("+", "").replace(" ", "")
        } else {
            "u_dev_" + UUID.randomUUID().toString().take(8)
        }

        // 1. Primary: Direct Google Firestore SDK Sync (Instant, Realtime, Persistent)
        syncUserToFirestoreSDK(user, stableUid)

        scope.launch {
            // 2. Secondary Sync to Firestore REST API (as backup)
            try {
                syncUserToFirestore(user, stableUid)
            } catch (e: Exception) {
                Log.w(TAG, "Firestore secondary REST sync skipped: ${e.message}")
            }

            // 3. Fallback Sync to Cloud Relay
            try {
                syncUserToCloudRelay(user, stableUid)
            } catch (e: Exception) {
                Log.w(TAG, "Cloud relay user sync warning: ${e.message}")
            }
        }
    }

    private fun syncUserToCloudRelay(user: UserProfile, uid: String) {
        val deviceName = "${Build.MANUFACTURER.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }} ${Build.MODEL}"
        val nowFormatted = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())

        // Fetch current users array
        var existingUsers = JSONArray()
        try {
            val getConn = (URL(CLOUD_USERS_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 6000
            }
            if (getConn.responseCode == 200) {
                val resp = getConn.inputStream.bufferedReader().use { it.readText() }
                val rootJson = JSONObject(resp)
                val dataObj = rootJson.optJSONObject("data")
                existingUsers = dataObj?.optJSONArray("users") ?: JSONArray()
            }
            getConn.disconnect()
        } catch (e: Exception) {
            existingUsers = JSONArray()
        }

        // Build current user JSON
        val userJson = JSONObject().apply {
            put("uid", uid)
            put("name", user.name.ifBlank { "Player" })
            put("email", user.email)
            put("phone", user.mobileNumber.ifBlank { user.phone })
            put("upiId", user.upiId)
            put("points", user.walletPoints)
            put("balanceRupees", user.walletBalance)
            put("tier", user.tier.name)
            put("status", user.status)
            put("deviceModel", deviceName)
            put("registeredDate", nowFormatted)
            put("lastActive", "Active Now 🟢")
            put("totalSpins", user.totalSpins)
            put("totalEarned", user.totalEarned)
            put("updatedAt", System.currentTimeMillis())
        }

        // Replace or add user
        val updatedArray = JSONArray()
        var replaced = false
        for (i in 0 until existingUsers.length()) {
            val u = existingUsers.optJSONObject(i) ?: continue
            val existingUid = u.optString("uid")
            val existingEmail = u.optString("email")
            val existingPhone = u.optString("phone")

            if ((existingUid.isNotBlank() && existingUid == uid) ||
                (existingEmail.isNotBlank() && user.email.isNotBlank() && existingEmail == user.email) ||
                (existingPhone.isNotBlank() && user.mobileNumber.isNotBlank() && existingPhone == user.mobileNumber)
            ) {
                updatedArray.put(userJson)
                replaced = true
            } else {
                updatedArray.put(u)
            }
        }
        if (!replaced) {
            updatedArray.put(userJson)
        }

        // PUT back to cloud relay
        val putConn = (URL(CLOUD_USERS_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "PUT"
            connectTimeout = 8000
            readTimeout = 8000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        }
        val putPayload = JSONObject().apply {
            put("name", "spinwin_2026_users")
            put("data", JSONObject().apply {
                put("users", updatedArray)
            })
        }
        OutputStreamWriter(putConn.outputStream, "UTF-8").use { writer ->
            writer.write(putPayload.toString())
            writer.flush()
        }
        val code = putConn.responseCode
        putConn.disconnect()
        Log.d(TAG, "Cloud Relay User Sync result: $code (Users count: ${updatedArray.length()})")
    }

    private fun syncUserToFirestore(user: UserProfile, uid: String) {
        val docId = sanitizeDocId(uid)
        val endpoint = "$FIRESTORE_BASE_URL/users/$docId?key=$API_KEY"
        val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "PATCH"
            connectTimeout = 5000
            readTimeout = 5000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        }

        val deviceName = "${Build.MANUFACTURER.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }} ${Build.MODEL}"
        val nowFormatted = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())

        val fields = JSONObject().apply {
            put("uid", JSONObject().put("stringValue", uid))
            put("name", JSONObject().put("stringValue", user.name.ifBlank { "Player" }))
            put("email", JSONObject().put("stringValue", user.email))
            put("mobileNumber", JSONObject().put("stringValue", user.mobileNumber.ifBlank { user.phone }))
            put("phone", JSONObject().put("stringValue", user.phone))
            put("upiId", JSONObject().put("stringValue", user.upiId))
            put("walletPoints", JSONObject().put("integerValue", user.walletPoints.toString()))
            put("walletBalance", JSONObject().put("doubleValue", user.walletBalance))
            put("points", JSONObject().put("integerValue", user.walletPoints.toString()))
            put("balanceRupees", JSONObject().put("doubleValue", user.walletBalance))
            put("tier", JSONObject().put("stringValue", user.tier.name))
            put("status", JSONObject().put("stringValue", user.status))
            put("deviceModel", JSONObject().put("stringValue", deviceName))
            put("registeredDate", JSONObject().put("stringValue", nowFormatted))
            put("lastActive", JSONObject().put("stringValue", "Active Now 🟢"))
            put("updatedAt", JSONObject().put("integerValue", System.currentTimeMillis().toString()))
        }

        val payload = JSONObject().apply {
            put("fields", fields)
        }

        OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
            writer.write(payload.toString())
            writer.flush()
        }
        val code = conn.responseCode
        conn.disconnect()
    }

    /**
     * 🔥 Direct Google Cloud Firestore SDK Synchronization for User Profile.
     * Uses official FirebaseFirestore instance with offline persistence and automatic retries.
     */
    private fun syncUserToFirestoreSDK(user: UserProfile, uid: String) {
        try {
            val firestore = FirebaseFirestore.getInstance()
            val deviceName = "${Build.MANUFACTURER.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }} ${Build.MODEL}"
            val nowFormatted = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())

            val userMap = hashMapOf(
                "uid" to uid,
                "name" to user.name.ifBlank { "Player" },
                "email" to user.email,
                "phone" to user.mobileNumber.ifBlank { user.phone },
                "mobileNumber" to user.mobileNumber.ifBlank { user.phone },
                "upiId" to user.upiId,
                "points" to user.walletPoints,
                "walletPoints" to user.walletPoints,
                "balanceRupees" to user.walletBalance,
                "walletBalance" to user.walletBalance,
                "tier" to user.tier.name,
                "status" to user.status,
                "deviceModel" to deviceName,
                "registeredDate" to nowFormatted,
                "lastActive" to "Active Now 🟢",
                "totalSpins" to user.totalSpins,
                "totalEarned" to user.totalEarned,
                "totalWithdrawn" to user.totalWithdrawn,
                "age" to user.age,
                "countryCode" to user.countryCode,
                "updatedAt" to System.currentTimeMillis()
            )

            val docId = sanitizeDocId(uid)
            firestore.collection("users").document(docId)
                .set(userMap, SetOptions.merge())
                .addOnSuccessListener {
                    Log.d(TAG, "User $docId synced to Firestore successfully")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "User $docId sync to Firestore failed: ${e.message}")
                }
        } catch (e: Exception) {
            Log.w(TAG, "Firestore SDK user sync exception: ${e.message}")
        }
    }

    /**
     * Syncs a withdrawal request to Live Cloud & Firestore
     */
    fun syncWithdrawal(request: WithdrawalRequest, userName: String) {
        if (request.wdId.isBlank()) return

        // 1. Primary: Direct Google Firestore SDK Sync
        syncWithdrawalToFirestoreSDK(request, userName)

        // 2. Secondary: Cloud Relay Sync
        scope.launch {
            try {
                syncWithdrawalToCloudRelay(request, userName)
            } catch (e: Exception) {
                Log.w(TAG, "Cloud relay withdrawal sync warning: ${e.message}")
            }
        }
    }

    /**
     * 🔥 Direct Google Cloud Firestore SDK Synchronization for Withdrawals
     */
    private fun syncWithdrawalToFirestoreSDK(request: WithdrawalRequest, userName: String) {
        try {
            val firestore = FirebaseFirestore.getInstance()
            val nowFormatted = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(request.requestedAt))

            val wdMap = hashMapOf(
                "id" to request.wdId,
                "uid" to request.uid,
                "userName" to userName,
                "amountINR" to request.amountRupees,
                "pointsDeducted" to request.pointsDeducted,
                "method" to request.method,
                "upiId" to request.payoutAddress,
                "status" to request.status,
                "time" to nowFormatted,
                "isRealUser" to true,
                "timestamp" to request.requestedAt
            )

            firestore.collection("withdrawals").document(request.wdId)
                .set(wdMap, SetOptions.merge())
                .addOnSuccessListener {
                    Log.d(TAG, "Withdrawal ${request.wdId} synced to Firestore successfully")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Withdrawal ${request.wdId} sync failed: ${e.message}")
                }
        } catch (e: Exception) {
            Log.w(TAG, "Firestore SDK withdrawal sync exception: ${e.message}")
        }
    }

    private fun syncWithdrawalToCloudRelay(request: WithdrawalRequest, userName: String) {
        val nowFormatted = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(request.requestedAt))

        // Fetch current withdrawals
        var existingWds = JSONArray()
        try {
            val getConn = (URL(CLOUD_WITHDRAWALS_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 6000
            }
            if (getConn.responseCode == 200) {
                val resp = getConn.inputStream.bufferedReader().use { it.readText() }
                val rootJson = JSONObject(resp)
                val dataObj = rootJson.optJSONObject("data")
                existingWds = dataObj?.optJSONArray("withdrawals") ?: JSONArray()
            }
            getConn.disconnect()
        } catch (e: Exception) {
            existingWds = JSONArray()
        }

        val wdJson = JSONObject().apply {
            put("id", request.wdId)
            put("uid", request.uid)
            put("userName", userName)
            put("amountINR", request.amountRupees)
            put("pointsDeducted", request.pointsDeducted)
            put("method", request.method)
            put("upiId", request.payoutAddress)
            put("status", request.status)
            put("time", nowFormatted)
            put("isRealUser", true)
        }

        val updatedArray = JSONArray()
        var replaced = false
        for (i in 0 until existingWds.length()) {
            val w = existingWds.optJSONObject(i) ?: continue
            if (w.optString("id") == request.wdId) {
                updatedArray.put(wdJson)
                replaced = true
            } else {
                updatedArray.put(w)
            }
        }
        if (!replaced) {
            updatedArray.put(wdJson)
        }

        // PUT back
        val putConn = (URL(CLOUD_WITHDRAWALS_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "PUT"
            connectTimeout = 8000
            readTimeout = 8000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        }
        val putPayload = JSONObject().apply {
            put("name", "spinwin_2026_withdrawals")
            put("data", JSONObject().apply {
                put("withdrawals", updatedArray)
            })
        }
        OutputStreamWriter(putConn.outputStream, "UTF-8").use { writer ->
            writer.write(putPayload.toString())
            writer.flush()
        }
        putConn.responseCode
        putConn.disconnect()
    }

    /**
     * Pulls latest user profile from Live Cloud Relay so admin edits are immediately received by the app
     */
    fun fetchUserFromCloudRelay(uid: String, onResult: (JSONObject?) -> Unit) {
        scope.launch {
            try {
                val conn = (URL(CLOUD_USERS_URL).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 5000
                    readTimeout = 5000
                }
                if (conn.responseCode == 200) {
                    val resp = conn.inputStream.bufferedReader().use { it.readText() }
                    val root = JSONObject(resp)
                    val usersArr = root.optJSONObject("data")?.optJSONArray("users") ?: JSONArray()
                    for (i in 0 until usersArr.length()) {
                        val u = usersArr.optJSONObject(i) ?: continue
                        if (u.optString("uid") == uid) {
                            onResult(u)
                            conn.disconnect()
                            return@launch
                        }
                    }
                }
                conn.disconnect()
                onResult(null)
            } catch (e: Exception) {
                onResult(null)
            }
        }
    }

    private fun sanitizeDocId(raw: String): String {
        return raw.replace("/", "_").replace(".", "_")
    }
}
