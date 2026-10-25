# MedRemind — project state / session handoff

Updated: 2026-10-17

## What this is
Native Android app (Kotlin + Jetpack Compose): offline-first medicine reminders +
cross-device caregiver sync. Repo: `github.com/wahidiqubal9-svg/MedRemind` (public).
Distributed via Obtainium (Direct APK Link) and the in-app updater.

## Current release
- **v0.183.12** (versionCode 1012) — built by GitHub Actions.
- Direct APK: `https://raw.githubusercontent.com/wahidiqubal9-svg/MedRemind/apk/medremind.apk`
- Version feed: `https://raw.githubusercontent.com/wahidiqubal9-svg/MedRemind/apk/version.json`
- In-app updater: Settings → Check for updates; launch popup; downloads + system installer.
- CI: `.github/workflows/android.yml`; runs on push to `main` (ignores `*.md`).
  Gradle 8.9 wrapper, AGP 8.7.3, Kotlin 2.0.21, JDK 17.
  `versionCode = 1000 + run_number`, `versionName = 0.183.<run>`.
  Signs with `ci/debug.keystore`; publishes the `apk` branch + versioned release `v*`.
- GitHub Actions default workflow permissions set to **write**.

## Build environment (this device)
- `source /workspace/toolchain/env.sh`
- Gradle: `/workspace/toolchain/gradle-8.9/bin/gradle`
- Local builds need `-Pandroid.aapt2FromMavenOverride=/workspace/toolchain/bin/aapt2`
  (removed from `gradle.properties` so CI uses the default aapt2; scripts pass it).
- `adb`: `/usr/bin/adb` (wireless debugging; the port changes).
- Prefer CI for APKs; local builds are an optional fallback.

## Backend
- Firebase project **medremind-6973d**; Auth (email + Google), Firestore sync.
- **No FCM service, no Cloud Functions.**
- Cloud Storage used for medicine photos (needs Blaze for new projects).
- Reminders: patient’s local exact alarms; “Remind now” writes a Firestore
  `reminders` doc; patient listener + 15‑min `ReminderPollWorker`; caregiver
  5/30‑min delivery checks.

## Data (Room v18)
`medicines` (incl. `pausedAt`), `schedules` (FK cascade), `dose_events`,
`metrics`, `archived_doses` (kept history), caregiver links/patients/activity.

## Recent work (v0.182 → v0.183.12)
- Intake option card fix (flat opaque card, clipped ripple).
- Solid blue `#4E75F4`, text `#2B2B2B`, softened type scale.
- Pill nav icon.
- Per‑medicine adherence page.
- Progress → separate History page (7/30/90/All), tap a medicine → its history.
- Health → separate History page (All/BP/Glucose/Weight).
- Medicine ⋮ → bottom sheet: Edit/Duplicate/Share/Refill/Pause/Delete
  (delete options inline: keep history vs delete + log).
- Pause/Resume medicine.
- Pharmacy moved from Med tab to Me tab.
- Pro lock overlays; free users can’t open caregiving profiles from Today.
- Refill Now without pharmacy → Add pharmacy dialog → WhatsApp the pharmacy.
- In‑app updater (version.json → download → system installer).

## Open decision (last topic)
Instant reminders / “Remind now” **without Blaze**:
1. **Foreground service** (`remoteMessaging`) keeps the Firestore listener alive →
   instant; needs a persistent notification + battery exemption.
2. **FCM + free Cloudflare Worker** → real push, no persistent notification;
   needs an FCM service/token in the app, a Worker + Firebase service‑account
   key, and the caregiver calling the Worker.
3. **OneSignal** free tier (third‑party) as an alternative.
Also pending: **photos → Firestore base64** (helpers exist) to remove the
Storage/Blaze dependency.

Scheduled doses are already instant locally once synced; the gaps are
“Remind now” while the patient app is closed and the initial sync of a newly
added schedule (up to 15 min).

## Important caveats
- Repo is **public** (for the direct link + updater). `ci/debug.keystore` and
  `google-services.json` are public.
- Keep the debug keystore (cert SHA‑256 `5b519138…dcff524c`) for update continuity.
- A GitHub token was pasted into chat earlier — rotate/revoke it. Obtainium’s
  Direct APK Link needs no token.
- `versionCode` must increase for updates; CI does it.
- Raw GitHub CDN caches ~5 min after a release.
- Pushes that only change `*.md` do not trigger a build.
- In‑app updater uses `filesDir/updates` + FileProvider + `REQUEST_INSTALL_PACKAGES`;
  the user must allow “Install unknown apps” for MedRemind once.

## Key files
- `app/build.gradle.kts` (buildConfig, signing, version overrides)
- `gradle.properties`
- `scripts/release.sh`, `scripts/dev.sh`
- `.github/workflows/android.yml`, `ci/debug.keystore`
- `ui/Theme.kt`, `MainTabs.kt`, `TodayContent.kt`, `MedContent.kt`,
  `HistoryScreen.kt`, `MedicineHistoryScreen.kt`, `DoseHistoryScreen.kt`,
  `HealthHistoryScreen.kt`, `MeScreen.kt`, `SettingsScreen.kt`, `AppRoot.kt`,
  `Ui.kt`, `PaywallScreen.kt`
- `data/AppDatabase.kt`, `Medicine.kt`, `ArchivedDose.kt`, `DoseEventDao.kt`
- `ui/MedicineViewModel.kt`, `ui/EntitlementViewModel.kt`
- `update/UpdateManager.kt`
- `data/caregiver/CloudSyncManager.kt`, `CloudRemoteReminderService.kt`,
  `PatientDataSync.kt`
- `alarm/ReminderScheduler.kt`, `alarm/ReminderPollWorker.kt`

## Continue in a new session
1. Open the workspace `/workspace/MedRemind`.
2. Read this file.
3. Push to `main`; GitHub Actions builds and publishes. No local build required.
4. Verify the latest run/release with the GitHub API (authenticated).
5. Next: choose the instant‑reminder option (foreground service vs FCM + Worker)
   and decide on the Firestore‑base64 photo fallback.
