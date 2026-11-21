# Instructions for Gemini (aistudio.google.com)

Paste the block below into the project's **custom instructions** in Google AI Studio. AGENTS.md has the details behind each rule; [docs/COLLABORATION.md](docs/COLLABORATION.md) explains how Gemini and Claude share this repo.

```
// Custom instructions for Chest Counter (com.kuletx.forttreasure)

You will always:
* Read AGENTS.md before changing anything, and update it when you change the build, CI, package, permissions or architecture
* Start from the latest main on GitHub: Claude also pushes to it (CI, tests, fixes, releases), and pushing an older copy of the project would undo that work
* Never delete or rewrite files under .github/, CLAUDE.md, AGENTS.md, AISTUDIO_INSTRUCTIONS.md or docs/COLLABORATION.md unless asked; if you must change one, say so in the summary
* Keep applicationId = "com.kuletx.forttreasure" and namespace = "com.kuletx.forttreasure"
* Keep the app name the same in strings.xml, settings.gradle.kts and metadata.json, and never remove MAJOR_CAPABILITY_SERVER_SIDE_GEMINI_API from metadata.json
* Never modify debug.keystore, debug.keystore.base64 or signingConfigs, and never create local.properties or commit build outputs (.gradle/, build/, *.apk)
* Build with the system gradle, never ./gradlew: gradle :app:testDebugUnitTest and gradle :app:assembleDebug must both pass before you finish
* Add or update a JUnit or Robolectric test in app/src/test/java/com/example/ for every behaviour you change; never delete, skip or weaken a test to make the build pass
* Wrap every user-facing string in L.t(lang, fr, en, es) with all three languages filled in
* Keep the @SerialName keys in Models.kt unchanged so data.json stays compatible with the Windows app
* Keep match number, session number and chest counts separate: only chest figures count chests
* Never show 100% for a chance; use Math.log1p and expm1 for odds
* Use the Storage Access Framework for files; never add READ_EXTERNAL_STORAGE or MANAGE_EXTERNAL_STORAGE
* Keep content/content.json and app/src/main/res/raw/content.json identical, and raise revision when you change them
* Do not bump the version or publish releases: Claude does that (see docs/COLLABORATION.md)
* End with a summary listing every file you changed, what you verified (which commands, how many tests passed) and what you did not verify
```
