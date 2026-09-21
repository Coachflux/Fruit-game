package io.github.coachflux.fruitfusion

import android.webkit.JavascriptInterface

/**
 * Secure, minimal JavaScript interface exposed to the local game WebView.
 *
 * Only the methods annotated with @JavascriptInterface are callable from JS.
 * The interface is only attached when loading the packaged local assets,
 * so arbitrary external web content cannot invoke these methods.
 */
class AdsJavascriptInterface(
    private val adManager: AdManager
) {
    @JavascriptInterface
    fun showBanner() {
        adManager.showBanner()
    }

    @JavascriptInterface
    fun hideBanner() {
        adManager.hideBanner()
    }

    @JavascriptInterface
    fun showInterstitial() {
        adManager.showInterstitial()
    }

    @JavascriptInterface
    fun showRewarded() {
        adManager.showRewarded()
    }
}
