# Working with Gemini and Claude on this repo

Two AI assistants work on Chest Counter (`com.kuletx.forttreasure`), with you deciding what gets built:

| | **Gemini** (Google AI Studio, aistudio.google.com) | **Claude** (Claude Code) |
|---|---|---|
| Best at here | Building features and screens fast: Compose UI, new cards, the Treasure Picker, the odds calculator | Checking and finishing: code review, CI, tests, bug fixes, releases, porting code between repos |
| How its work reaches GitHub | You press **Push** in AI Studio; the commit lands on `main` | Its own `claude/*` branch, then a pull request that CI must pass, then merged into `main` |
| Reads | `AGENTS.md` + the custom instructions in [AISTUDIO_INSTRUCTIONS.md](../AISTUDIO_INSTRUCTIONS.md) | `CLAUDE.md`, which loads `AGENTS.md` and this file |
| Can't do | Run CI on GitHub, publish releases | Push tags (its environment refuses them; it publishes releases with the workflow's **release** input instead) |

**`main` is the one source of truth.** Every change from either side ends up there, and both start from it.

---

## 1. The files that hold the rules

- **`AGENTS.md`**: the project rules for both assistants (package name, toolchain, architecture, signing, guardrails). When either one changes the build, CI, permissions or structure, it updates this file in the same commit.
- **`AISTUDIO_INSTRUCTIONS.md`**: the block you paste into AI Studio's custom instructions. When it changes in the repo, paste it again; AI Studio doesn't read it by itself.
- **`CLAUDE.md`**: makes Claude Code load `AGENTS.md` and this file.
- **`docs/COLLABORATION.md`**: this file.

---

## 2. The branches

- **`main`**: what gets built and released. CI runs on every push.
- **`claude/<name>`**: Claude's working branches. Each one becomes a pull request into `main`.
- **`claude-main`**: a copy of `main`, moved forward automatically by `.github/workflows/sync-claude.yml` after every push to `main`. If it ever holds commits `main` lacks, the workflow leaves it alone and warns instead of overwriting it.

---

## 3. One round, step by step

### A. You and Gemini build something
1. Before asking Gemini for anything, make sure AI Studio has the **latest `main`**, including what Claude merged since your last session. If it works from an older copy, its next push undoes Claude's work: in this repo, CI fixes, tests and the sync workflow would disappear.
2. Ask for the feature. Gemini follows the custom instructions: tests for each change, `gradle :app:testDebugUnitTest` and `gradle :app:assembleDebug` passing, and a summary of the files it changed and what it verified.
3. **Press Push.** Nothing reaches GitHub until you do. In this project, one round of changes was described as done but never pushed.

### B. Hand it to Claude
Tell Claude something like:

> Gemini pushed to FNTreasure: *(paste Gemini's summary)*. Check it.

Claude then:
1. **Checks that the commit is on GitHub.** If `main` hasn't moved, the push didn't happen; Claude says so.
2. **Compares the claims with the code.** Gemini's summary is a description, not a proof. "20 tests passing" counts only once CI has run them.
3. **Checks for undone work.** It compares the push with Claude's earlier commits; if Gemini's copy was older and reverted them, Claude restores them.
4. **Watches CI.** If CI fails, Claude finds the cause and fixes it on a `claude/*` branch. It never skips or deletes a test to get green.
5. **Reviews.** Real bugs are fixed the same way, and Claude tells you what it found.
6. **Merges** once CI is green: through a pull request, or straight to `main` when you ask.

### C. Back to Gemini
Before the next round in AI Studio, bring in Claude's changes (step A1). If Claude changed something Gemini needs to know about, such as a new rule in `AGENTS.md` or a changed `AISTUDIO_INSTRUCTIONS.md`, re-paste the instructions.

**Never let both work at the same time** on the same repo: one round at a time, each starting from the other's latest `main`.

---

## 4. Releases (Claude)

1. Raise `versionCode` and `versionName` in `app/build.gradle.kts`.
2. Rewrite `RELEASE_NOTES.md` in English, French and Spanish. It becomes the release text.
3. Push to `main` and wait for CI to pass.
4. Run the **Android CI** workflow on `main` with **release** set to the tag (e.g. `v1.6.0`). It builds the release APK and publishes the GitHub Release with the APK attached. Pushing a `v*` tag does the same, from a machine that is allowed to push tags.

**Signing.** An APK only updates the installed app, with its data kept, if it is signed with the same key as the one before. Until the four secrets `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS` and `ANDROID_KEY_PASSWORD` are set in the repo settings, releases carry a debug-signed APK. Each CI run makes a new debug key, so moving from one such release to the next means uninstalling first. AGENTS.md §3 explains how to create the keystore. Set the secrets once and keep the keystore file somewhere safe: losing it means no more updates for anyone who installed the app.

---

## 5. Messages that work well

**To Gemini**, in AI Studio:
- "Build *feature*. Add tests for it, run the tests and the debug build, and list what you changed and verified."
- "Claude merged *X* to main. Sync with GitHub before you start."

**To Claude**, in Claude Code:
- "Gemini pushed *summary*. Check it, fix CI if needed, and merge."
- "Release v1.x.y" (version bump, notes in three languages, then publish).
- "Port *feature* from Fortnite-ChestCounter."

---

## 6. When something goes wrong

| Problem | What to do |
|---|---|
| Gemini says it's done but GitHub hasn't changed | Press **Push** in AI Studio. |
| A Gemini push removed Claude's files (`.github/`, `CLAUDE.md`, tests) | Ask Claude to restore them from history, then sync AI Studio before the next round. |
| CI is red after a push | Ask Claude to fix it. It fixes the cause and never disables a test. |
| An update won't install over the old app ("App not installed") | The two APKs are signed with different keys: uninstall first, and set the signing secrets (section 4). |
| Gemini and Claude disagree about a rule | `AGENTS.md` decides. Change it there, and re-paste `AISTUDIO_INSTRUCTIONS.md` if needed. |

---

## 7. The other repository

[`kuletxcore/Fortnite-ChestCounter`](https://github.com/kuletxcore/Fortnite-ChestCounter) is the Windows app plus the first Android version (C#/.NET, `io.github.chestcounter.android`), and it's where `content/content.json` is published. **This repo is the authoritative Android app.** Features that exist only there, such as automatic chest detection by screen capture, get ported here rather than developed in both places.
