# Fortnite Chest Counter (Android)

**Counts every chest you open in Fortnite Battle Royale (`experience_br`), calculates your Treasure Hunt drop probabilities, and tracks your seasonal collection on Android.**

Rewritten as a native Android application using **Kotlin**, **Jetpack Compose (Material 3)**, **Room**, and **DataStore**, featuring direct launcher support for [`https://play.fn.gg/island/experience_br`](https://play.fn.gg/island/experience_br) (`com.epicgames.fortnite`).

---

## Features

- **Fortnite Battle Royale (`experience_br`) Launcher (`com.epicgames.fortnite`)**:
  - Checks if `com.epicgames.fortnite` is installed on the device and displays its version.
  - Launches `https://play.fn.gg/island/experience_br` directly into Fortnite Mobile (with browser fallback if not installed).
  - Supports `experience_br` and `Playlist_experience_br` alongside standard Battle Royale playlists (`Playlist_Default*`, `Playlist_NoBuildBR*`, etc.).
- **Home**:
  - Dismissible Welcome onboarding card (language, timezone, clock format).
  - Live `content.json` catalog notices and scan progress bar.
  - Hero Total Chests card with quick manual adjustment (`+` / `−`, weekly tagging, and Career Matches Estimator).
  - Session, Last Match, Treasures, and optional Squad Total metric tiles plus an Activity Log.
- **Report**:
  - Interactive 30-day chest bar chart (tap any bar to inspect date and count).
  - Breakdown by chest type and treasure sources.
  - **Your Chances**: Live/upcoming Treasure Hour countdowns, Gold Outfit & Pirate Shop Access featured cards, weekly & all-season odds tables, cumulative `1 - ∏(1 - p * boost)` math, verdict badges, and pace projections.
  - **Your Collection**: Interactive season treasure grid bundled with local `.webp` assets and auto-ticked replay drops.
  - Paginated **Treasures Found** and **Latest Matches** (including vehicle-destroyed loot warning and match chest editor), plus **CSV** and **`data.json`** export via Storage Access Framework.
- **Overlay**:
  - Interactive live **HUD** and **Classic** overlay previews across Idle, Match in Progress, and Live (+12) states.
  - Adjustable Style, Size (`70%`–`175%`), Opacity (`30%`–`100%`), and Squad Total toggle, plus `overlay.html` export/copy for OBS.
- **Settings & Help**:
  - French, English, and Spanish (`fr` / `en` / `es`), local vs UTC time, auto / 12h / 24h clock, multi-profile switching, catalog refresh from GitHub, SAF `.replay` file/folder scanning, desktop `data.json` import, and `errors.log` viewer.

## Android Scope & Replay Notes

- Uses Android's **Storage Access Framework** (`OpenMultipleDocuments`, `OpenDocumentTree`, `OpenDocument`, `CreateDocument`)—no broad storage or system alert window permissions required.
- The built-in Kotlin binary `.replay` parser reads Unreal `ReplayInfo`, `Header` (`LevelNamesAndTimes`), and `Event` (`TeamStats` / `Stats`) chunks for match metadata, playlist/map filtering (including `experience_br`), and completion status.
- Full in-match Oodle Mermaid `ReplayData` actor stream decompression is not bundled in this pure-Kotlin Android build; players can import full match chest/treasure logs via desktop `data.json` or log/edit chests manually in the Edit Chests dialog.

## Tech Stack

- **UI**: Jetpack Compose, Material 3, Adaptive `WindowSizeClass` (`NavigationBar` / `NavigationRail`), Pirata One & Plus Jakarta Sans typography
- **Architecture**: MVVM (`MainViewModel`, `StateFlow`), Clean Domain (`TreasureCatalog`, `Profiles`, `ChestEstimate`, `GameModes`, `ReportExporter`)
- **Persistence**: Room (`MatchEntity`, `ManualEntryEntity`, `ObtainedPrizeEntity`, `ActivityLogEntity`) + Preferences DataStore
- **Networking & Images**: OkHttp (`content.json` catalog sync), Coil + bundled `.webp` drawables

## License

[MIT](LICENSE). Not affiliated with, endorsed, or sponsored by Epic Games. Fortnite is a trademark of Epic Games, Inc.
