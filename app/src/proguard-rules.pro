# Keep WebView JavaScript interface methods
-keepclassmembers class io.github.coachflux.fruitfusion.AdsJavascriptInterface {
    @android.webkit.JavascriptInterface <methods>;
}

# Google Mobile Ads
-keep class com.google.android.gms.ads.** { *; }
-dontwarn com.google.android.gms.ads.**

# Keep BuildConfig
-keep class io.github.coachflux.fruitfusion.BuildConfig { *; }
