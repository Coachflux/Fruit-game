package io.github.coachflux.fruitfusion

import android.app.Activity
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/**
 * Native AdMob manager. Handles banner, interstitial and rewarded ads.
 * Communicates results back to the WebView via injected JavaScript callbacks.
 */
class AdManager(
    private val activity: Activity,
    private val bannerContainer: FrameLayout,
    private val webView: android.webkit.WebView
) {
    companion object {
        private const val TAG = "FruitFusionAds"
    }

    private var bannerAdView: AdView? = null
    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null
    private var isBannerVisible = false

    // ------------------------------------------------------------------
    // Banner
    // ------------------------------------------------------------------
    fun showBanner() {
        activity.runOnUiThread {
            if (bannerAdView != null) {
                bannerContainer.visibility = android.view.View.VISIBLE
                isBannerVisible = true
                return@runOnUiThread
            }
            val adView = AdView(activity)
            adView.setAdSize(AdSize.BANNER)
            adView.adUnitId = BuildConfig.BANNER_AD_UNIT_ID
            bannerContainer.removeAllViews()
            bannerContainer.addView(adView, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ))
            bannerContainer.visibility = android.view.View.VISIBLE
            bannerAdView = adView
            isBannerVisible = true
            adView.loadAd(AdRequest.Builder().build())
            Log.d(TAG, "Banner load requested")
        }
    }

    fun hideBanner() {
        activity.runOnUiThread {
            bannerContainer.visibility = android.view.View.GONE
            isBannerVisible = false
        }
    }

    // ------------------------------------------------------------------
    // Interstitial
    // ------------------------------------------------------------------
    fun loadInterstitial() {
        val request = AdRequest.Builder().build()
        InterstitialAd.load(
            activity,
            BuildConfig.INTERSTITIAL_AD_UNIT_ID,
            request,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    Log.d(TAG, "Interstitial loaded")
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            interstitialAd = null
                            // Pre-load next one
                            loadInterstitial()
                        }
                        override fun onAdFailedToShowFullScreenContent(error: AdError) {
                            Log.w(TAG, "Interstitial failed to show: ${error.message}")
                            interstitialAd = null
                            loadInterstitial()
                        }
                    }
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Interstitial failed to load: ${error.message}")
                    interstitialAd = null
                }
            }
        )
    }

    fun showInterstitial() {
        activity.runOnUiThread {
            val ad = interstitialAd
            if (ad != null) {
                ad.show(activity)
            } else {
                Log.d(TAG, "Interstitial not ready – loading a new one")
                loadInterstitial()
            }
        }
    }

    // ------------------------------------------------------------------
    // Rewarded
    // ------------------------------------------------------------------
    private var rewardedEarned = false

    fun loadRewarded() {
        val request = AdRequest.Builder().build()
        RewardedAd.load(
            activity,
            BuildConfig.REWARDED_AD_UNIT_ID,
            request,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    Log.d(TAG, "Rewarded ad loaded")
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            rewardedAd = null
                            // Only notify "no reward" if the user did not earn it
                            if (!rewardedEarned) {
                                evaluateJs("if(typeof window.onAndroidRewardedClosed==='function')window.onAndroidRewardedClosed();")
                            }
                            rewardedEarned = false
                            loadRewarded()
                        }
                        override fun onAdFailedToShowFullScreenContent(error: AdError) {
                            Log.w(TAG, "Rewarded failed to show: ${error.message}")
                            rewardedAd = null
                            rewardedEarned = false
                            evaluateJs("if(typeof window.onAndroidRewardedClosed==='function')window.onAndroidRewardedClosed();")
                            loadRewarded()
                        }
                    }
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Rewarded failed to load: ${error.message}")
                    rewardedAd = null
                }
            }
        )
    }

    fun showRewarded() {
        activity.runOnUiThread {
            val ad = rewardedAd
            if (ad != null) {
                rewardedEarned = false
                ad.show(activity) { rewardItem ->
                    // User earned the reward – notify the game
                    Log.d(TAG, "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
                    rewardedEarned = true
                    evaluateJs("if(typeof window.onAndroidRewarded==='function')window.onAndroidRewarded();")
                }
            } else {
                Log.d(TAG, "Rewarded not ready – loading a new one and notifying no-reward")
                evaluateJs("if(typeof window.onAndroidRewardedClosed==='function')window.onAndroidRewardedClosed();")
                loadRewarded()
            }
        }
    }

    private fun evaluateJs(script: String) {
        webView.post {
            webView.evaluateJavascript(script, null)
        }
    }

    fun destroy() {
        bannerAdView?.destroy()
        bannerAdView = null
        interstitialAd = null
        rewardedAd = null
    }
}
