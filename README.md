# Fruit Fusion — Android

Standalone Android application that packages the original **Fruit Fusion** HTML/CSS/JavaScript match-3 game inside a native WebView and integrates **Google AdMob** via the official Mobile Ads SDK.

**Package ID:** `io.github.coachflux.fruitfusion`  
**App name:** Fruit Fusion

---

## What was preserved

- The complete original game logic, levels, scoring, power-ups, animations, sounds, and UI.
- All original game files live under `app/src/main/assets/game/`.
- Only a **minimal** change was made to the game’s existing `AdManager` so it can talk to the native Android AdMob layer when running inside the WebView. Browser / mock behaviour still works for testing outside Android.

---

## Project structure (key parts)

```
Fruit-Fusion/
├── app/
│   ├── src/main/
│   │   ├── assets/game/          ← original Fruit Fusion web game
│   │   ├── java/.../fruitfusion/ ← MainActivity, AdManager, JS bridge
│   │   ├── res/                  ← icons, themes, splash, layout
│   │   └── AndroidManifest.xml
│   ├── build.gradle
│   └── proguard-rules.pro
├── .github/workflows/android-build.yml
├── build.gradle
├── settings.gradle
├── gradle.properties
├── gradlew / gradlew.bat
└── README.md
```

---

## Building with GitHub Actions (from your phone)

1. Push this repository to GitHub.
2. Go to **Actions → Android Build → Run workflow**.
3. After the run finishes, download the artifacts:
   - `Fruit-Fusion-debug-apk`
   - `Fruit-Fusion-release-apk` (signed if secrets are set)
   - `Fruit-Fusion-release-aab`

### Required GitHub Secrets (for signed release builds)

| Secret name                 | Description                                      |
|-----------------------------|--------------------------------------------------|
| `ANDROID_KEYSTORE_BASE64`   | Base64-encoded `.jks` / `.keystore` file         |
| `KEYSTORE_PASSWORD`         | Keystore password                                |
| `KEY_ALIAS`                 | Key alias                                        |
| `KEY_PASSWORD`              | Key password                                     |

**Optional (production AdMob IDs):**

| Secret name                   | Description                |
|-------------------------------|----------------------------|
| `ADMOB_APP_ID`                | Your AdMob App ID          |
| `BANNER_AD_UNIT_ID`           | Banner ad unit             |
| `INTERSTITIAL_AD_UNIT_ID`     | Interstitial ad unit       |
| `REWARDED_AD_UNIT_ID`         | Rewarded ad unit           |

If the AdMob secrets are omitted, the build uses **Google’s official test ad unit IDs** (safe for development).

### Creating a keystore (one-time, on any computer)

```bash
keytool -genkey -v -keystore fruitfusion.jks -keyalg RSA -keysize 2048 -validity 10000 -alias fruitfusion
base64 -w 0 fruitfusion.jks > keystore.b64   # upload the content of keystore.b64 as ANDROID_KEYSTORE_BASE64
```

---

## AdMob configuration

### Default = Test ads

By default the project uses Google’s test IDs so you never accidentally serve real ads during development.

### Switching to production

**Option A – local.properties (local builds)**

```properties
ADMOB_APP_ID=ca-app-pub-xxxxxxxx~yyyyyyyyyy
BANNER_AD_UNIT_ID=ca-app-pub-xxxxxxxx/zzzzzzzz
INTERSTITIAL_AD_UNIT_ID=ca-app-pub-xxxxxxxx/aaaaaaaa
REWARDED_AD_UNIT_ID=ca-app-pub-xxxxxxxx/bbbbbbbb
```

**Option B – environment variables / GitHub Secrets (CI)**

Set the same variable names as secrets or env vars. The Gradle script prefers environment variables over `local.properties`.

---

## JavaScript ↔ Native bridge

The game already had an `AdManager`. It was updated so that when running inside the Android WebView it calls:

| JS call                    | Native action                          |
|----------------------------|----------------------------------------|
| `AndroidAds.showBanner()`  | Shows native AdMob banner at bottom    |
| `AndroidAds.hideBanner()`  | Hides the banner                       |
| `AndroidAds.showInterstitial()` | Shows interstitial (if loaded)    |
| `AndroidAds.showRewarded()`| Shows rewarded ad; reward granted only after Google confirms |

Callbacks back into the game:

- `window.onAndroidRewarded()` – user earned the reward
- `window.onAndroidRewardedClosed()` – ad closed without reward

These are already wired into the existing continue-after-loss flow.

---

## Local development (optional)

```bash
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

Requires Android SDK + JDK 17.

---

## Versioning

Edit in `app/build.gradle`:

```groovy
versionCode 1
versionName "1.0.0"
```

Increment `versionCode` for every Play Store upload.

---

## Notes

- The game runs **offline**. Internet is only needed for AdMob.
- No Median.co or third-party ad plugins are used.
- Portrait orientation is locked (matches the original game design).
- Full-screen immersive experience; no browser URL bar.
- Hardware acceleration and WebView settings are tuned for a game.
- Package ID is consistently `io.github.coachflux.fruitfusion`.

---

## Changing the package ID later

If you ever need a different package name, update:

1. `applicationId` in `app/build.gradle`
2. `namespace` in `app/build.gradle`
3. Package declaration + folder structure under `java/`
4. Any references in documentation / assetlinks
