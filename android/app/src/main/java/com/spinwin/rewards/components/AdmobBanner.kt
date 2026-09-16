package com.spinwin.rewards.components

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.spinwin.rewards.theme.BorderGlass
import com.spinwin.rewards.theme.InterFamily

/**
 * Google AdMob Ad Units for SpinWin Rewards
 *
 * TEST IDs (used while app is in development / not yet approved by AdMob):
 *   Banner:        ca-app-pub-3940256099942544/6300978111
 *   Interstitial:  ca-app-pub-3940256099942544/1033173712
 *   Rewarded:      ca-app-pub-3940256099942544/5224354917
 *   App Open:      ca-app-pub-3940256099942544/9257395921
 *
 * When your real AdMob unit IDs are ready, replace TEST_ constants with real IDs.
 */
object AdUnitIds {
    // ✅ Official Google Test Ad IDs (safe to use during testing — no policy violations)
    const val BANNER_TEST      = "ca-app-pub-3940256099942544/6300978111"
    const val INTERSTITIAL_TEST = "ca-app-pub-3940256099942544/1033173712"
    const val REWARDED_TEST    = "ca-app-pub-3940256099942544/5224354917"
    const val APP_OPEN_TEST    = "ca-app-pub-3940256099942544/9257395921"

    // 🔄 Replace below with your REAL AdMob unit IDs from admob.google.com when ready:
    // const val BANNER_REAL   = "ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX"
    // const val REWARDED_REAL = "ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX"

    // Currently active (set to TEST while building, switch to REAL for release)
    val BANNER      get() = BANNER_TEST
    val REWARDED    get() = REWARDED_TEST
    val INTERSTITIAL get() = INTERSTITIAL_TEST
}

/**
 * Unrewarded AdMob Banner Ad:
 * Shows a standard 320x50 banner.
 * Developer earns 100% revenue — user gets no points from this.
 */
@Composable
fun AdmobBanner(
    modifier: Modifier = Modifier,
    adUnitId: String = AdUnitIds.BANNER
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, BorderGlass, RoundedCornerShape(12.dp))
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "SPONSORED",
                color = Color.White.copy(alpha = 0.35f),
                fontFamily = InterFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 8.sp,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 2.dp)
            )

            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                factory = { context ->
                    AdView(context).apply {
                        setAdSize(AdSize.BANNER)
                        this.adUnitId = adUnitId
                        adListener = object : AdListener() {
                            override fun onAdLoaded() {
                                Log.d("AdmobBanner", "✅ Banner ad loaded successfully")
                            }
                            override fun onAdFailedToLoad(error: LoadAdError) {
                                Log.w("AdmobBanner", "⚠️ Banner ad failed: ${error.message}")
                            }
                        }
                        loadAd(AdRequest.Builder().build())
                    }
                }
            )
        }
    }
}

/**
 * Load and show a Rewarded Ad.
 * Call this when user taps "Watch Ad for Points".
 * onRewarded() is called only if user watches the full ad.
 */
fun loadAndShowRewardedAd(
    context: Context,
    onRewarded: () -> Unit,
    onFailed: () -> Unit = {}
) {
    val adRequest = AdRequest.Builder().build()
    RewardedAd.load(
        context,
        AdUnitIds.REWARDED,
        adRequest,
        object : RewardedAdLoadCallback() {
            override fun onAdLoaded(rewardedAd: RewardedAd) {
                Log.d("RewardedAd", "✅ Rewarded ad loaded")
                val activity = context as? Activity
                if (activity != null) {
                    rewardedAd.show(activity) { _ ->
                        Log.d("RewardedAd", "✅ User earned reward!")
                        onRewarded()
                    }
                } else {
                    onFailed()
                }
            }
            override fun onAdFailedToLoad(error: LoadAdError) {
                Log.w("RewardedAd", "⚠️ Rewarded ad failed to load: ${error.message}")
                onFailed()
            }
        }
    )
}

/**
 * Load and show an Interstitial Ad.
 * Call this between screens or after completing tasks.
 */
fun loadAndShowInterstitialAd(
    context: Context,
    onDismissed: () -> Unit = {}
) {
    val adRequest = AdRequest.Builder().build()
    InterstitialAd.load(
        context,
        AdUnitIds.INTERSTITIAL,
        adRequest,
        object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                Log.d("InterstitialAd", "✅ Interstitial ad loaded")
                interstitialAd.fullScreenContentCallback = object : com.google.android.gms.ads.FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        Log.d("InterstitialAd", "Ad dismissed")
                        onDismissed()
                    }
                }
                val activity = context as? Activity
                activity?.let { interstitialAd.show(it) } ?: onDismissed()
            }
            override fun onAdFailedToLoad(error: LoadAdError) {
                Log.w("InterstitialAd", "⚠️ Interstitial failed: ${error.message}")
                onDismissed()
            }
        }
    )
}
