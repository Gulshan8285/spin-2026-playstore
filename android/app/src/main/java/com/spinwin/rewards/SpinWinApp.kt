package com.spinwin.rewards

import android.app.Application
import com.spinwin.rewards.audio.SoundManager
import com.spinwin.rewards.data.repository.RewardsRepository

class SpinWinApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Preload sound engine & repositories
        SoundManager.getInstance(this)
        RewardsRepository.getInstance(this)
        try {
            com.google.android.gms.ads.MobileAds.initialize(this) {}
        } catch (e: Exception) {
            // Graceful fallback
        }
    }
}
