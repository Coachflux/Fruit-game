package io.github.coachflux.fruitfusion

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding

/**
 * Main activity: hosts the Fruit Fusion HTML game inside a full-screen WebView
 * and provides native AdMob integration via a minimal JS bridge.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var bannerContainer: FrameLayout
    private lateinit var adManager: AdManager
    private var isGameLoaded = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Force portrait (game is designed for portrait)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        bannerContainer = findViewById(R.id.bannerContainer)

        setupImmersiveMode()
        setupWebView()
        setupAds()
        setupBackNavigation()

        // Load the local game from assets
        webView.loadUrl("file:///android_asset/game/index.html")
    }

    private fun setupImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        // Keep screen on while playing
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.allowFileAccess = true
        settings.allowContentAccess = true
        // Restrict file access from file URLs for security (game is local)
        settings.allowFileAccessFromFileURLs = true
        settings.allowUniversalAccessFromFileURLs = false
        settings.mediaPlaybackRequiresUserGesture = false
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.setSupportZoom(false)
        settings.builtInZoomControls = false
        settings.displayZoomControls = false
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.javaScriptCanOpenWindowsAutomatically = false
        // Hardware acceleration is enabled at the activity / application level

        webView.setBackgroundColor(Color.parseColor("#eef1f7")) // match game background
        webView.isVerticalScrollBarEnabled = false
        webView.isHorizontalScrollBarEnabled = false
        webView.overScrollMode = View.OVER_SCROLL_NEVER

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                // Prevent any external navigation – keep everything inside the WebView
                val url = request?.url?.toString() ?: return true
                return if (url.startsWith("file://") || url.startsWith("about:")) {
                    false // allow local
                } else {
                    // Block external URLs for security
                    true
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                isGameLoaded = true
                // After page load, the game’s own onload will call AdManager.initBanner()
                // which talks to the JS bridge we injected below.
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                // Game is fully local; network errors should not break core gameplay
            }
        }

        webView.webChromeClient = WebChromeClient()

        // Inject the secure JS bridge. Name must match what the game expects: AndroidAds
        adManager = AdManager(this, bannerContainer, webView)
        webView.addJavascriptInterface(
            AdsJavascriptInterface(adManager),
            "AndroidAds"
        )
    }

    private fun setupAds() {
        // Pre-load interstitial & rewarded so they are ready when the game asks for them
        adManager.loadInterstitial()
        adManager.loadRewarded()
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    // Let the game handle “back” via its own UI if possible,
                    // otherwise finish the activity.
                    finish()
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
        // Resume banner if it was showing
        if (::adManager.isInitialized) {
            // no-op; banner lifecycle is managed by AdView
        }
    }

    override fun onPause() {
        webView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        if (::adManager.isInitialized) {
            adManager.destroy()
        }
        // Properly clean up WebView to avoid leaks
        (webView.parent as? ViewGroup)?.removeView(webView)
        webView.stopLoading()
        webView.destroy()
        super.onDestroy()
    }
}
