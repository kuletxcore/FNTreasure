# Instructions for Gemini (aistudio.google.com)

Paste the block below into the project's **custom instructions** in Google AI Studio. AGENTS.md has the details behind each rule.

```
// Custom instructions for Chest Counter (com.kuletx.forttreasure)

You will always:
* Read AGENTS.md before changing anything, and update it when you change the build, CI, package, permissions or architecture
* Use emojis instead of SVG icons for new buttons and labels (the overlay already uses 🧰 ★ ■ 👁 ↻)
* Do not change model strings found in code
* Avoid using gradients: keep the flat Logbook palette (NightSeaBg, ParchmentCard, AccentGold, TealAccent)
* Keep applicationId = "com.kuletx.forttreasure" and namespace = "com.example"
* Keep the app name the same in strings.xml, settings.gradle.kts and metadata.json, and never remove MAJOR_CAPABILITY_SERVER_SIDE_GEMINI_API from metadata.json
* Never modify debug.keystore, debug.keystore.base64 or signingConfigs, and never create local.properties
* Build with the system gradle, never ./gradlew: gradle :app:testDebugUnitTest and gradle :app:assembleDebug must both pass before you finish
* Add or update a JUnit or Robolectric test in app/src/test/java/com/example/ for every behaviour you change; never delete, skip or weaken a test to make the build pass
* Wrap every user-facing string in L.t(lang, fr, en, es) with all three languages filled in
* Keep the @SerialName keys in Models.kt unchanged so data.json stays compatible with the Windows app
* Keep match number, session number and chest counts separate: only chest figures count chests
* Never show 100% for a chance; use Math.log1p and expm1 for odds
* Use the Storage Access Framework for files; never add READ_EXTERNAL_STORAGE or MANAGE_EXTERNAL_STORAGE
* Keep content/content.json and app/src/main/res/raw/content.json identical, and raise revision when you change them
* For a release, raise versionCode and versionName in app/build.gradle.kts and rewrite RELEASE_NOTES.md in English, French and Spanish
* Give each commit a short message saying what changed and why
```
