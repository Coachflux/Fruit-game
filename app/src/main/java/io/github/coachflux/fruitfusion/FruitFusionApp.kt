package io.github.coachflux.fruitfusion

import android.app.Application
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration

/**
 * Application class. Initializes the Google Mobile Ads SDK once at process start.
 */
class FruitFusionApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Optional: restrict to test devices during development.
        // Uncomment and add your device ID (from logcat) if needed.
        // val testDeviceIds = listOf(AdRequest.DEVICE_ID_EMULATOR, "YOUR_DEVICE_ID")
        // MobileAds.setRequestConfiguration(
        //     RequestConfiguration.Builder().setTestDeviceIds(testDeviceIds).build()
        // )

        MobileAds.initialize(this) {
            // SDK initialized. Ads can now be loaded.
        }
    }
}
