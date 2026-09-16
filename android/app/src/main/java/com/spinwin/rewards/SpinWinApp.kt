package com.spinwin.rewards

import android.app.Application
import android.provider.Settings
import android.util.Log
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.spinwin.rewards.audio.SoundManager
import com.spinwin.rewards.data.repository.RewardsRepository
import java.security.MessageDigest

class SpinWinApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Preload sound engine & repositories
        SoundManager.getInstance(this)
        RewardsRepository.getInstance(this)

        initAdMob()
    }

    private fun initAdMob() {
        try {
            // ✅ Register this device as a TEST device so ads always load during development.
            // Google's test IDs (ca-app-pub-3940256099942544/...) need the device to be
            // declared as test device, otherwise ads silently fail to show.
            val testDeviceIds = mutableListOf(
                // Google's own emulator ID — always include this
                "EMULATOR",
                // Auto-detect the real device's Advertising ID hash
                getDeviceAdIdHash()
            ).filterNotNull()

            Log.d("AdMob", "Setting up test devices: $testDeviceIds")

            val config = RequestConfiguration.Builder()
                .setTestDeviceIds(testDeviceIds)
                .build()

            MobileAds.setRequestConfiguration(config)

            MobileAds.initialize(this) { initStatus ->
                val statusMap = initStatus.adapterStatusMap
                statusMap.forEach { (adapter, status) ->
                    Log.d("AdMob", "Adapter: $adapter | Status: ${status.initializationState} | Latency: ${status.latency}ms")
                }
                Log.d("AdMob", "✅ MobileAds initialized successfully")
            }
        } catch (e: Exception) {
            Log.e("AdMob", "❌ MobileAds init failed: ${e.message}")
        }
    }

    /**
     * Returns the MD5 hash of the Android ID which AdMob uses as the test device fingerprint.
     * This is the same string you see in logcat when you first run an ad:
     * "Use RequestConfiguration.Builder().setTestDeviceIds(Arrays.asList("XXXXXX")) to get test ads"
     */
    private fun getDeviceAdIdHash(): String? {
        return try {
            val androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
            val md5 = MessageDigest.getInstance("MD5").digest(androidId.toByteArray())
            md5.joinToString("") { "%02x".format(it) }.uppercase()
        } catch (e: Exception) {
            null
        }
    }
}
