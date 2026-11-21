# AGENTS.md

Guidance for LLM coding agents (including Claude) working on **Fortnite Chest Counter (Android)**.

---

## 1. Project Overview & Provenance

- **Base Repository**: [`https://github.com/kuletxcore/Fortnite-ChestCounter/`](https://github.com/kuletxcore/Fortnite-ChestCounter/) (forked from [`LED-N/Fortnite-ChestCounter`](https://github.com/LED-N/Fortnite-ChestCounter)), rewritten from the Windows/.NET C# desktop app into a native **Android** application using **Kotlin**, **Jetpack Compose (Material 3)**, **Room**, and **Preferences DataStore**.
- **Android Application Package (`applicationId`)**: **`com.kuletx.forttreasure`** (configured in `app/build.gradle.kts`).
  - **Code Namespace (`namespace`)**: **`com.kuletx.forttreasure`**, the same as the package (directory tree `app/src/main/java/com/kuletx/forttreasure/`, `R` class `com.kuletx.forttreasure.R`). Every Kotlin `package` and import uses it; a new file goes under that tree. It was `com.example` up to v1.6.1.
- **Android Additions Beyond Desktop**:
  - **Fortnite Mobile (`com.epicgames.fortnite`) & `experience_br` support**: Package visibility `<queries>`, install/version detection (`FortniteLauncher`), one-tap deep link to `https://play.fn.gg/island/experience_br`, and playlist matching for `experience_br` / `Playlist_experience_br`.
  - **Live Match & Session State (`LiveMatchState`, `LiveMatchSessionCard`, `OverlayModel`)**: Tracks **Match #** (`matchNumber`), **Session #** (`sessionNumber`), **chests in current match** (`liveMatch.chests`), **session chests**, and **picked treasures** (`liveMatch.treasures`) independently so chests opened (e.g. `9`, `15`) never overwrite `Match 1 · Session 1`.
  - **Interactive Floating Overlay (`SYSTEM_ALERT_WINDOW` + Foreground Service)**: Draggable `TYPE_APPLICATION_OVERLAY` window (`FloatingOverlayService`) running as an Android 14+ `specialUse` Foreground Service (`PROPERTY_SPECIAL_USE_FGS_SUBTYPE`, ongoing notification with live Match/Session/Chest status + `+1 chest` / `Stop` actions, `POST_NOTIFICATIONS` prompt on API 33+), synced in real time via `AppContainer` Flows with `+1` chest, `★` inline **Treasure Picker** strip, `■` finish & save match, `👁` compact mode, and `↻` undo / reset match / new session menu (long-press `↻` for quick match reset), plus `Settings.ACTION_MANAGE_OVERLAY_PERMISSION` (`package:com.kuletx.forttreasure`) and `ON_RESUME` status refresh.
  - **Treasure Picker (`TreasurePicker.kt`)**: Allows picking weekly prizes (including Jam Track, Pickaxe/Back Bling, Car Decal, Loading Screen, Gold Outfit), Pirate Shop Access, and Gizmos (Gizmo any, Golden firework, Portable extractor, Sprite locator, Lucky locator, Extraction accelerator) both live and for any recorded match in the Match Log / Report (`MatchDetailDialog`).
  - **Island Loot Odds Calculator** (`IslandLootOdds`, `LootOddsCalculatorCard`): Simulates single-chest and cumulative `N`-chest percentage odds, expected drops, Treasure Hour multipliers, and `25%`/`50%`/`75%`/`90%` chest/match milestones from live `content.json` drop rates or custom `%` rates.
  - **Quick-Switch FAB Menu** (`QuickSwitchFabMenu`): Expandable Material 3 speed-dial FAB menu toggling between **Loot Tracker**, **Chest Odds Calculator**, and **Logbook** views.

---

## 2. Package Configuration (`com.kuletx.forttreasure`) & Build Environment

### Package & Toolchain Setup
- **Android Package (`applicationId`)**: `com.kuletx.forttreasure` in `app/build.gradle.kts` (`defaultConfig.applicationId`). Do **NOT** revert it.
- **Runtime Package Resolution**: All Android APIs (`context.packageName`, `Settings.ACTION_MANAGE_OVERLAY_PERMISSION` Uri `package:${context.packageName}`, Room DB path) automatically resolve to `com.kuletx.forttreasure`. Do **NOT** hardcode old package IDs.
- **Gradle / AGP / Kotlin**:
  - **AGP `9.1.1`** with built-in Kotlin **`2.2.21`**, **KSP `2.3.6`**, **Gradle `9.3.1`**, **JVM 21**, `compileSdk = 36`, `targetSdk = 36`, `minSdk = 26`.
  - `android/gradle.properties` includes `android.useAndroidX=true`.
  - Do **NOT** apply the `org.jetbrains.kotlin.android` plugin; AGP 9 handles Kotlin natively.
  - Do **NOT** downgrade KSP below `2.3.x` unless disabling AGP built-in Kotlin (not recommended).
- **Commands & Files**:
  - Always use system `gradle` (`gradle :app:assembleDebug`, `gradle :app:bundleRelease`, `gradle :app:testDebugUnitTest`). **Never** run `./gradlew` or `gradlew` in environments without a Gradle wrapper.
  - **Never** create, edit, or use `local.properties` (`ANDROID_SDK_ROOT` is provided via environment variable).
  - Keep the app name synchronized across `app/src/main/res/values/strings.xml`, `settings.gradle.kts`, and `metadata.json`. **Never** remove `MAJOR_CAPABILITY_SERVER_SIDE_GEMINI_API` from `metadata.json`.
- **CI & Repo Hygiene**:
  - `.github/workflows/build.yml` runs Android CI on `ubuntu-latest` (JDK 21 + Android SDK + Gradle `9.3.1`), executing `gradle :app:testDebugUnitTest` and `gradle :app:assembleDebug` (and release APK/AAB packaging on `v*` tags).
  - `.gitignore` excludes `.gradle/`, `.kotlin/`, `**/build/`, `local.properties`, `*.jks`, `*.keystore`, and `*.apk`/`*.aab` while preserving `debug.keystore.base64` if used.
- **Gemini + Claude**: see `docs/COLLABORATION.md` (who does what, branches, handoff, releases). `AISTUDIO_INSTRUCTIONS.md` holds the custom instructions pasted into AI Studio; keep it in step with this file.
- **Releases**: run the Android CI workflow with the `release` input (e.g. `v1.6.0`), or push a `v*` tag; CI publishes the APK with `RELEASE_NOTES.md` as the release text.
- **Testing**: Do not write `androidTest/` instrumented tests if no emulator/ADB is available; use JUnit / Robolectric JVM tests under `app/src/test/java/com/kuletx/forttreasure/` (`gradle :app:testDebugUnitTest`).

---

## 3. Packaging & Signing `com.kuletx.forttreasure` (APK & AAB)

### Important Note When Changing `applicationId`
- Changing `applicationId` to **`com.kuletx.forttreasure`** creates a distinct app identity on Android devices. If a previous build with an older `applicationId` (e.g. `com.aistudio.chestcounter.fnkpxq`) is installed on a phone, uninstall it or expect both apps to sit side-by-side until removed.
- To preserve Room/DataStore user data across future updates of `com.kuletx.forttreasure`, **every subsequent release APK/AAB MUST keep `applicationId = "com.kuletx.forttreasure"` and be signed with the exact same keystore certificate**.

### Debug & AI Studio Builds (Protected — Do Not Break)
- **Debug Keystore**: AI Studio automatically decodes `debug.keystore.base64` into `${rootDir}/debug.keystore` prior to builds (alias: `androiddebugkey`, store/key password: `android`).
- **CRITICAL**:
  - **NEVER** modify, overwrite, or delete `debug.keystore` or `debug.keystore.base64`.
  - **NEVER** alter existing `signingConfigs` or add product flavors if working inside AI Studio's managed debug pipeline.
  - Build debug APK:
    ```bash
    gradle :app:assembleDebug
    ```
    Output: `app/build/outputs/apk/debug/app-debug.apk` (package `com.kuletx.forttreasure`, signed with debug key).

### Release Packaging & Signing (GitHub Actions / Local CLI / Claude Builds)
Follow these exact steps when building a signed release APK or Play Store AAB for `com.kuletx.forttreasure`:

1. **Generate an Upload/Release Keystore (one-time, never commit to git)**:
   ```bash
   keytool -genkeypair -v \
     -keystore forttreasure-release.jks \
     -alias forttreasure \
     -keyalg RSA -keysize 2048 -validity 10000
   ```
   (Add `*.jks` and `*.keystore` to `.gitignore` — except `debug.keystore.base64` if managed by AI Studio.)

2. **Pass Signing Credentials via Environment Variables (Never hardcode or use `local.properties`)**:
   ```bash
   export ANDROID_KEYSTORE_PATH="/abs/path/to/forttreasure-release.jks"
   export ANDROID_KEYSTORE_PASSWORD="your-store-password"
   export ANDROID_KEY_ALIAS="forttreasure"
   export ANDROID_KEY_PASSWORD="your-key-password"
   ```

3. **Configure Conditional Env Signing in `app/build.gradle.kts`**:
   Only enable when `ANDROID_KEYSTORE_PATH` is set so unconfigured debug builds never fail:
   ```kotlin
   android {
       // ...
       signingConfigs {
           val ksPath = System.getenv("ANDROID_KEYSTORE_PATH")
           if (!ksPath.isNullOrBlank()) {
               create("releaseEnv") {
                   storeFile = file(ksPath)
                   storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
                   keyAlias = System.getenv("ANDROID_KEY_ALIAS")
                   keyPassword = System.getenv("ANDROID_KEY_PASSWORD")
               }
           }
       }
       buildTypes {
           release {
               isMinifyEnabled = false
               val releaseSigning = signingConfigs.findByName("releaseEnv")
               if (releaseSigning != null) {
                   signingConfig = releaseSigning
               }
               proguardFiles(
                   getDefaultProguardFile("proguard-android-optimize.txt"),
                   "proguard-rules.pro"
               )
           }
       }
   }
   ```

4. **Build Signed Release APK or Play App Bundle (.aab)**:
   ```bash
   # Signed APK for direct GitHub Release / sideloading:
   gradle :app:assembleRelease
   # -> app/build/outputs/apk/release/app-release.apk

   # Signed Android App Bundle (.aab) for Google Play Console:
   gradle :app:bundleRelease
   # -> app/build/outputs/bundle/release/app-release.aab
   ```

5. **Manual Align + Sign + Verify (If building `app-release-unsigned.apk` without Gradle `signingConfigs`)**:
   ```bash
   "$ANDROID_SDK_ROOT/build-tools/35.0.0/zipalign" -v -p 4 \
     app/build/outputs/apk/release/app-release-unsigned.apk \
     app-release-aligned.apk

   "$ANDROID_SDK_ROOT/build-tools/35.0.0/apksigner" sign \
     --ks forttreasure-release.jks \
     --ks-key-alias forttreasure \
     --ks-pass env:ANDROID_KEYSTORE_PASSWORD \
     --key-pass env:ANDROID_KEY_PASSWORD \
     --out com.kuletx.forttreasure-release.apk \
     app-release-aligned.apk

   "$ANDROID_SDK_ROOT/build-tools/35.0.0/apksigner" verify --verbose com.kuletx.forttreasure-release.apk
   ```
   (Adjust `build-tools/<version>` to whichever version is installed under `$ANDROID_SDK_ROOT/build-tools/`.)

6. **Verify Package ID & Signature Before Distributing**:
   ```bash
   "$ANDROID_SDK_ROOT/build-tools/35.0.0/aapt2" dump badging com.kuletx.forttreasure-release.apk | head -n 5
   # Confirm: package: name='com.kuletx.forttreasure' versionCode='...' versionName='...'
   ```

---

## 4. Architecture & File Map (`app/src/main/java/com/kuletx/forttreasure/`)

- **`MainActivity.kt`**: Edge-to-edge `ComponentActivity`, SAF document launchers (`data.json`, CSV/JSON/HTML export), `ON_RESUME` lifecycle refresh for `canDrawOverlays` and `FortniteLauncher`.
- **`domain/`** (Pure Kotlin business logic & DTOs):
  - `Models.kt`: Desktop-compatible `TrackerDataJson`, `MatchRecordDto` (including optional `SessionNumber` / `MatchNumber` / `Notes`), `ChestRecordDto`, `TreasureFindDto`, `ManualEntryDto`, `LiveMatchState`, `AppConfig`, `AppInfo` (`REPO`, `UPSTREAM_REPO`, `CONTENT_URL`, `UPSTREAM_CONTENT_URL`, `FORTNITE_PACKAGE`, `EXPERIENCE_BR_URL`).
  - `TreasureCatalog.kt`: Parses/validates `content.json` (embedded `res/raw/content.json`, cached `filesDir/content.json`, remote GitHub `main` sync), computes `logNever`/`expected`/`percentText`/`verdict`/Treasure Hour countdowns, and maps CDN filenames to bundled `res/drawable/treasure_*.webp`.
  - `IslandLootOdds.kt`: Calculates per-chest and cumulative odds, Treasure Hour boosts, and chest/match milestone targets for seasonal, weekly, and custom loot drop rates.
  - `GameModes.kt`: Season map (`/Delilah/Maps/Delilah_Terrain`) & BR playlist validation (`Playlist_Default*`, `Playlist_NoBuildBR*`, `experience_br`, `Playlist_experience_br`).
  - `OverlayModel.kt`: Builds `OverlayView` cleanly separating `matchNumber`, `sessionNumber`, `liveChests`, `session`, `last`, and `treasures` (`isInSession()`, `nextMatchNumber()`, `buildLive()`).
  - `Profiles.kt`, `ChestEstimate.kt`, `ReportExporter.kt` (`matches.csv`, `overlay.html`), `Lang.kt` (`L` localization for `fr`/`en`/`es`, `ChestKinds`, `TreasureNames`).
- **`data/`**:
  - `FortniteLauncher.kt`: Checks `com.epicgames.fortnite` install state/version via `PackageManager` and opens `https://play.fn.gg/island/experience_br`.
  - `local/`:
    - `AppContainer.kt`: Process-wide singleton providing one shared `AppDatabase`, `SettingsRepository`, and `ChestCounterRepository` for both `MainViewModel` and `FloatingOverlayService`.
    - `AppDatabase.kt`, `Entities.kt`, `Daos.kt`, `ChestCounterRepository.kt` (includes `incrementLiveChest`, `addLiveTreasure`, `removeLiveTreasure`, `finishLiveMatch`, `resetLiveMatch`, `startNewSession`, `updateMatchDetails`), `SettingsRepository.kt` (Preferences DataStore + `liveMatchFlow` + `sessionNumber`).
- **`overlay/`**:
  - `FloatingOverlayService.kt`: `OverlayPermissionHelper` + `specialUse` Foreground Service + `WindowManager` `TYPE_APPLICATION_OVERLAY` draggable interactive pill observing `AppContainer` Flows (`+1`, `★` inline Treasure Picker, `■` finish match, `👁` compact toggle, `↻` undo/reset/new session drawer + `runningFlow` synced to `MainViewModel`).
- **`test/`** (`app/src/test/java/com/kuletx/forttreasure/`):
  - `domain/OverlayModelTest.kt`, `domain/IslandLootOddsAndCatalogTest.kt`, `domain/GameModesAndReportTest.kt`, and Robolectric `data/LiveMatchRepositoryTest.kt`.
- **`ui/`**:
  - `MainViewModel.kt`: Single source of truth (`StateFlow<MainUiState>`) backed by `AppContainer`, ticker for countdowns, catalog sync, import/export, live match & session actions.
  - `AppShell.kt`: Adaptive `NavigationBar` (compact) / `NavigationRail` (medium/expanded) for 5 `AppTab`s (`HOME`, `REPORT`, `OVERLAY`, `SETTINGS`, `HELP`) + `QuickSwitchFabMenu` switching `QuickViewMode` (`LOGBOOK`, `LOOT_TRACKER`, `CHEST_ODDS_CALCULATOR`), with `BackHandler` and dismiss scrim.
  - `theme/`: `Color.kt`, `Type.kt` (Pirata One + Plus Jakarta Sans bundled in `res/font/`), `Theme.kt`.
  - `components/`: `LogbookComponents.kt`, `QuickSwitchFabMenu.kt`, `LiveMatchCard.kt`, `TreasurePicker.kt` (`TreasureChoices`, `TreasurePickerDialog`, `MatchDetailDialog`).
  - `screens/`: `HomeScreen.kt`, `ReportScreen.kt`, `ReportChancesSection.kt`, `LootOddsCalculatorCard.kt`, `QuickViews.kt`, `OverlayScreen.kt`, `SettingsScreen.kt`, `HelpScreen.kt`, `EditChestsDialog.kt`.

---

## 5. Upstream Sync (`content.json`) & Data Compatibility

- **Desktop Compatibility**: Never rename `@SerialName` PascalCase keys in `Models.kt` (`TrackerDataJson`, `MatchRecordDto`, etc.) — Android users can import `data.json` directly from Windows Chest Counter and export back to it. Optional fields (`SessionNumber`, `MatchNumber`, `Notes`) have safe defaults (`null`).
- **Syncing `content.json` Updates**:
  - At runtime, `TreasureCatalog.refreshBlocking()` checks `https://raw.githubusercontent.com/kuletxcore/Fortnite-ChestCounter/main/content/content.json` first and then `https://raw.githubusercontent.com/LED-N/Fortnite-ChestCounter/main/content/content.json`, adopting whichever valid payload has the highest `revision`.
  - When updating the bundled fallback in the repo, keep `/content/content.json` and `/app/src/main/res/raw/content.json` identical, increment `revision`, validate strictly against `TreasureCatalog.parse()` (`schema == 1`, valid ISO UTC instants, allowed `kind` values), and ensure any new `image` filenames have corresponding `res/drawable/treasure_*.webp` entries mapped in `TreasureCatalog.localDrawableFor()`.

---

## 6. Coding, UI & Policy Guardrails

- **Math**: Always use `java.lang.Math.log1p` and `kotlin.math.expm1` for probability calculations (`kotlin.math.log1p` does not exist in Kotlin stdlib). Never display `100%` for probabilistic outcomes (`TreasureCatalog.percentText` caps at `>99.9%`).
- **Design ("Logbook" Theme)**: Preserve the dark-sea (`NightSeaBg`) and warm parchment (`ParchmentCard`, `HeroChestBg`, `AccentGold`, `TealAccent`) palette. Avoid generic gray/material defaults.
- **Localization**: Wrap all user-facing strings in `L.t(lang, fr, en, es)` so French, English, and Spanish remain complete.
- **Accessibility & Testing**: Attach `Modifier.testTag("snake_case")` to new interactive controls, set meaningful `contentDescription` on non-decorative icons/images, keep touch targets `>= 48.dp`, and handle `BackHandler` on secondary views/overlays.
- **Permissions**: File import/export uses Storage Access Framework (`OpenMultipleDocuments`, `OpenDocumentTree`, `OpenDocument`, `CreateDocument`). Do not add `READ_EXTERNAL_STORAGE` or `MANAGE_EXTERNAL_STORAGE`. Declared permissions are `INTERNET`, `SYSTEM_ALERT_WINDOW`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`, and `POST_NOTIFICATIONS` (requested at runtime on API 33+).
