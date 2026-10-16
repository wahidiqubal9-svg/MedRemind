# Pillo (`xyz.rtrvr.pillo`) — Reverse Engineering Notes

Static analysis of the shipped Android build, kept as a reference for MedRemind.
Facts below come from the APK manifest, decoded resources, DEX class inventory, and
decompiled app code. Interpretations/inferences are marked as such.

---

## 0. Target identity & provenance

| Field | Value |
|---|---|
| Package | `xyz.rtrvr.pillo` |
| App name | `pillo` (manifest label includes a heart glyph) |
| Version | **0.6.24** (`versionCode` 459) |
| min / target SDK | 23 / 36 |
| Source | XAPK obtained from APKPure (`Pillo: Pill Reminder & Alarm`) |
| Base APK SHA-256 | `8b541ba0cbc55ff425bbc3a12a92abd0d9c76b161318b0e2c2110c97abdb17ef` |
| XAPK SHA-256 | `07de5ebec6fa5ac77f1bb45e6bd1d94abcb01edc58a624fcdcc80fe7e3fee41c` |
| Signature scheme | v2/v3 (BNDLTOOL) |
| Languages (locale config) | `en`, `ko-KR`, `de`, `es`, `ja`, `pt-BR`, `tr` |

> Note: this mirror is 0.6.24 (Play may ship a newer build). The base APK in this
> download contains English resources; other languages are delivered as language splits.

**Method:** JADX (resources + code), a custom DEX parser for the full class inventory
(all 18 DEX files), and focused decompilation of the app's own DEX files. No execution,
emulation, or runtime instrumentation was performed.

---

## 1. Tech stack & architecture (observed)

- **Language / UI:** Kotlin + **Jetpack Compose** (~49.5k app classes total, ~42.5k under `ui`; extensive `ComposableSingletons$…`).
- **DI:** Hilt/Dagger (`DaggerPilloApp_HiltComponents_SingletonC`, `*_GeneratedInjector`, `di/` modules).
- **Data:** Room (`data/persistence/database` + `migrations` + `*Dao`), DataStore + Preferences, **Chuck** HTTP inspector (`data/chuck`).
- **Networking:** Retrofit + OkHttp (`data/datasource/remote/...`).
- **Background work:** WorkManager, foreground services (`dataSync`, `specialUse`), exact alarms.
- **Platform SDKs:** Firebase (FCM messaging, Analytics, Google auth), Google Play **Billing** (`iap`), Google **Maps/Places**.
- **Animation / media:** **Lottie** (mascot + effects), bundled GIF/MP3 assets.
- **Ads / mediation:** AdMob, Meta Audience Network, AppLovin, **Moloco**, InMobi, Unity Ads; MRAID + IAB OM SDK.
- **Backend:** `https://api.pillo.care/v1/`
- **AI backend:** `https://api.agent.corca.ai` (Corca — a Korean AI vendor)
- **Legal pages:** hosted on `rtrvr.notion.site`

Feature packages under `xyz/rtrvr/pillo/domain/` enumerate the product surface:
`drug, interaction, fda, water, bp, glucose, spo2, temperature, weight, allergy,
condition, rx, diary, mood, meal, appointment, doctor, pharmacy, medicalfacility,
inventory, caregiver, donation, streak, quiz, vote, geofence, myplace, drive, backup,
export, iap, adherence, profile, misseddose, reminder, alarm, tracker, today, log`.

### App components (from `AndroidManifest.xml`)

**Activities (selection):** `MainActivity`, `OnboardingActivity`, `AlarmActivity`,
`DedicatedReminderActivity`, `CreateMedActivity` / `EditMedActivity` / `MedicationListActivity`
/ `MedDetailActivity`, `MedInfoActivity`, `LogAsNeededMedicationActivity`,
`InventoryActivity` + `StockCalculatorActivity`, `ExportDataActivity`,
`BackupAndRestoreActivity`, `MedisafeCsvImportActivity`, condition screens
(`AddConditionActivity`, `ViewOneConditionActivity`, `ConnectDrugsToOneConditionActivity`),
profile screens (`AddProfileActivity`, `AddHealthProfileActivity`,
`TransferDrugToAnotherProfileActivity`), doctor/pharmacy/facility screens,
`AppointmentListActivity` + add/edit/history, `AddCareGiverActivity` / `EditCareGiverActivity`,
`CreateTrackerActivity` / `EditTrackerActivity` / `LogTrackerEventAsNeededActivity` /
`TrackerListActivity`, `SymptomHistoryActivity`, `AddUserAllergyActivity`,
Rx screens (`AddRxEntryActivity`, `EditRxEntryActivity`, `RxEntryHistoryActivity`),
`DiaryHistoryActivity` + add/edit, `DonationReportActivity`, `StreakInformationActivity`,
`AlarmThemePreviewActivity`, subscription screens (`SubscriptionPaywallActivity`,
`SubscriptionManageActivity`, `ChangeSubscriptionPlanActivity`), `WebViewActivity`,
`DisplayOverPermissionActivity`, `DismissAppLockActivity` / `ChangePinCodeActivity` /
`ForgotPinCodeActivity` (exported), `AdWarnActivity`, `DrugPhotoActivity`,
`SnoozeActionActivity`, `MeetingInstruction` add/edit, `SettingMedSchedule/Dosage/Management`,
`SelectSymptomsActivity`, `AddAsNeededMedLogActivity`, `CrashReportActivity`.

**Receivers (selection):** `ReminderAlarmReceiver`, `KeepAlarmAwakeReceiver`,
`EnsureMedLoggingAlarmReceiver`, `RefillReminderReceiver`, `TakingTimeoutReceiver`,
`ResumeMedAlarmReceiver`, `PilloBackUpReceiver`, `DoubleLoginProtectReceiver`,
`RescheduleDayIntervalMedReminderReceiver`, `PinCodeAuthReceiver`,
`OnboardingInduceNotiReceiver`, `AsNeededIntervalPushNotiReceiver`,
`AppointmentReminderNotiReceiver`, **`CareGiverMissedMedsNotiReceiver`**, `PilloBootReceiver`,
`TimezoneChangeReceiver`, **`SmsSentReceiver`** (`TWILLIO_SMS_SENT`), `MealInstructionNotiReceiver`,
`EarlySnoozeNoti*`, `FallbackFullScreenReminderAlarmReceiver`, `Light*` receivers,
`RxExpiryAlertReminderReceiver`, `GeofencingReceiver`, `SnoozeCountdownActionReceiver`,
`ReminderAlarmRefreshReceiver`, `AlarmVisibleStateCheckReceiver`, `AppUpdateReceiver`,
`DistractionProofBackupAlarmReceiver`, widget receivers (`TodayWidgetReceiver`,
`CabinetWidgetReceiver`, `HeartGardenWidgetReceiver`).

**Services:** `FirebaseMsgHandler`, `PilloBackupUploadService` (dataSync),
`ForegroundPromotionOnDooaAvailableJobService`, `SnoozeCountdownService`,
`SimpleReminderService`, `AlarmAutoSnoozeService` (each `specialUse` with a
declared `PROPERTY_SPECIAL_USE_FGS_SUBTYPE`).

**Providers:** `FileProvider`, `InitializationProvider` (AndroidX Startup) wiring
`AndroidThreeTenInitializer`, `DiagnosticLogInitializer`, `ProcessLifecycleInitializer`,
Unity Ads initializer, `EmojiCompatInitializer`, `ProfileInstallerInitializer`.

**Notable permissions:** `USE_EXACT_ALARM`, `SCHEDULE_EXACT_ALARM`, `USE_FULL_SCREEN_INTENT`,
`SYSTEM_ALERT_WINDOW`, `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, `FOREGROUND_SERVICE_*`,
`POST_NOTIFICATIONS`, `CALL_PHONE`, `READ_PHONE_STATE`, camera, fine/coarse/background
location, `AD_ID` + `ACCESS_ADSERVICES_*`. `usesCleartextTraffic="true"`, `largeHeap="true"`,
`allowBackup="false"`.

**Root detection:** manifest `<queries>` include `com.topjohnwu.magisk`,
`eu.chainfire.supersu`, `com.noshufou.android.su`, `com.koushikdutta.superuser`,
`com.thirdparty.superuser`.

---

## 2. Fonts & design assets

Bundled in `res/font/` and applied in Compose via custom families:

| Font | Weights present | Role |
|---|---|---|
| **Outfit** | Black, SemiBold, Medium, Regular | Latin display/body |
| **Pretendard** | Bold, SemiBold, Medium | **Korean** text |
| **Roboto Medium Numbers** | single face | tabular numerals (doses/timers) |

Compose side (in code, not an XML font-family): `OutfitFamily` / `OutfitStyle`,
`PretendardFamily` / `PretendardStyle`.

Mascot & effects are **Lottie** + GIF/audio, not fonts, e.g.:
`lottie_mascot_*`, `lottie_pillo_mascot_on_timer_{idle_happy,annoyed,angry}`,
`lottie_confetti`, `lottie_water`, `lottie_permission_dooa_{en,kr,samsung}`,
`pillo_ring_{cheerful,worried,grumpy,annoyed}.gif`, `pillo_voice_{cheerful,sad,pouting}{,_ko}.mp3`,
`take_med_timer_bgm.mp3`, `nagging_{med,tracker}.mp3`, `morning.mp3`, `rage.mp3`, `dispensing.mp3`.

---

## 3. Feature catalog

**Medication management** — create/edit meds (segmented schedule/dosage/management screens),
med list + detail, custom sorting, as-needed (PRN) logging, skip reasons, suspend/resume,
med name editor, drug photos, stock/inventory + **stock calculator** + refill reminders,
meal instructions, Rx (prescription) entries.

**Reminders / alarms** — exact alarms + **full-screen alarms** (`showWhenLocked`,
`turnScreenOn`), light vs focus alarm types, snooze + countdown service, auto-snooze,
backup/fallback alarms, alarm themes, ringtone picker (recommend / music / sound / voice),
mascot "mood on timer", post-alarm donation prompt, dose-spacing ("smart snooze"),
battery saver.

**Health trackers** — Water, BP, Glucose, SpO₂, Body temperature, Weight, Mood, Sleep,
Symptoms, check-ins; create/edit/log/history, reading lists, surveys.

**Med safety** — interaction checkers via API: `interaction/ddi` (drug–drug), `dfi`
(drug–food), `dci` (drug–condition), `dai` (drug–allergy), plus `dse`, `dind`,
`normalize`; FDA drug library (`drugs_of_openfda_230907.csv`, `pillo_condition_230907.csv`)
and "Med Library" search.

**AI Pill Identifier** — camera or gallery photo, or manual imprint/shape/color. Endpoints:
`pill-id/identify`, `pill-id/browse`, `pill-id/concepts/{id}/info`, `requestIllustration`,
plus registration `image/parseImage`. Results generated from **U.S. FDA data via AI**
(US-only), with an explicit disclaimer; identification history retained.

**Care network** — profiles & health profiles (multi-profile, dependents), caregivers,
doctors, pharmacies, medical facilities, appointments (+history), allergies, conditions.

**Retention & gamification** — adherence, streaks, quizzes, and the **Donation Garden**:
earn one "Heart" per 100%-adherence day, give it to one of ~24 charities (ADA, AHA,
Doctors Without Borders, UNICEF, Shriners Children's, NAMI-adjacent orgs, etc.),
auto/manual donation mode, quarterly **Donation Report**, "watch ad to donate".

**Platform** — home-screen **widgets** (Today, Cabinet, Heart Garden), **Wear OS** connect
(Samsung Galaxy, Pixel Watch, Wear OS, Xiaomi, Oppo, Fossil, Skagen), WebView screens.

**Data & privacy** — **Excel export** (`ExcelBuilder`), backup/restore with cloud **image
backup** (`user/images`, `user/image/{filename}`), **Medisafe CSV import**, app-lock **PIN**
screen (+forgot/change), "double login protect", network security config.

**Monetization** — Google Play subscriptions: **Ad-free** and **Premium**. Paywall
advertises: ad-free, caregiver ("team up with loved ones"), DDI safety checkers, more
dependents, location-aware snooze, watch connect, more widgets, appointment history.
Consumable "ticket bundles" (`iap_ticket_bundle_{25,50,100,200}`). Cancel-flow survey captured.

### Backend API surface (observed in Retrofit services)

- `POST interaction/{ddi,dfi,dci,dai,dse,dind}` + `interaction/normalize`
- `GET interaction/search?q=`
- `GET pill-id/browse`, `GET pill-id/concepts/{conceptId}/info`, `GET pill-id/products/{productId}/info`, `POST pill-id/identify`, `POST pill-id/suggest-imprint`, `POST pill-id/illustration`
- `POST registration/image` (`parseImage`)
- Image backup: `POST user/image`, `GET user/images`, `GET user/image/{filename}`, `DELETE user/image/{filename}`, `DELETE user/images`

---

## 4. Onboarding process (observed)

`OnboardingActivity` drives an enum `OnboardingPages`:

```
INTRO → ASK_INTENT → (CHURN_USER_EXCLUSIVE_INDUCER) → ADD_MED(_INTRO)
      | PILL_IDENTIFIER(_INTRO|_FLOW) → WATER_INTENT_INTRO → WATER_REMINDER_FLOW → LOADING
```

Copy/flow reconstructed from strings + class names (`OnboardingAdaptViewModel`,
`ObAdaptQueryViewModel`, segments under `ui/onboarding/adapt/segments/...`):

1. **Intro** — "Hello, I'm Pillo!" → "What can I help you with?"
   (Remind me to take meds / Drink water / Identify a pill).
2. **Intent questions** — health area (Meds / Water / BP / Glucose / Weight),
   then med count ("Only 1", "2 meds", "More than 2 meds", "Not sure now").
3. **Add-med "adapt" flow (segmented):** how many times/day → med name search
   ("Search by Treatment") → dosage proportion → quantity → schedule type
   (**Daily** vs **Non-daily**: specific days-of-week, every X days, X times/month,
   every X hours, interval) → time picking → **reminder sound** (recommend / music /
   sound / voice, preview) → **alarm wallpaper** → **alarm type** (Focus full-screen vs
   Light) → "Preparing your med reminders…".
4. **Pill-identifier branch** — "How would you like to identify your pill?"
   (Scan / Upload photo / Enter manually) → US-only disclaimer.
5. **Water branch** — gender → weight → unit (ml/oz) → wake-up time → pre-sleep time →
   interval / regular-cycle choice → target suggestion → "Preparing your water reminders…".
6. **Data option** — "Import/Restore Data": *Import 'Medisafe' Report Data* or
   *Restore Pillo Back-up Data*.
7. **Connection step** ("Connecting your pill reminders…") → **Today coachmark**
   (highlighting the add med/tracker FAB).
8. **Permission onboarding** — the "Dooa" steps = *Display over other apps* prompts
   (`ObDooaPermView`, `ObDooaPerm2nd…`, plus Samsung-specific Lottie variant).

Every step is instrumented: `AnalyticsEvent$Onboarding$ObMedCountView/Tap`,
`ObMedFreqView/ConfirmTap`, `ObWaterAsk*`, `ObDataOptionTap`, `ObDooaPerm*`, etc.

---

## 5. Caregiver features (decompiled)

**Purpose:** alert a caregiver about the patient's doses by **SMS**.

**Entity `CareGiver`** (`models/entities/CareGiver.kt`): `careGiverId`, `isActive`,
`createdAtMilli`, `avatar`, `name`, `phoneNum`, `drugIds` (per-med subscription),
`sendNotiOnMissedMeds`, `sendNotiOnSkippedMeds`, `sendNotiOnTakenMeds`,
`missedMedsWaitMin` (default **30 min**). `CareGiver.NotiTypes` derives the active set.

**Alert types `CareGiverNotiTypes`:** `MISSED`, `SKIPPED`, `ALL_TAKEN` — each with
icon/label/desc/preview-desc/preview-image resource ids.
Strings: "When logging all meds as taken", "Doesn't respond to reminder in time",
"When logging a skipped dose".

**Add flow** (`ui/caregiver/add`, `AddCareGiverPages`):
Intro → Name ("Enter your name for the %s to recognize you"; edits profile name) →
Phone number → select **Drugs** → **NotiTypes** toggles → **NotiPreview** (message
preview) → **Send consent SMS** ("Request your caregiver's consent to receive messages")
→ **Pending status**.

**Consent / verification:**
- The patient sends a consent SMS; the caregiver must consent; verification is
  confirmed and the patient is notified (`noti_title_caregiver_verified`).
- `CareGiverVerificationUriHelper.fetchVerificationUri(careGiverId, phoneNum, …)`
  obtains an FCM device token (`FirebaseMessaging.getToken()`), requests a verification
  `uri` (JSON field `uri`), and returns `(uriString, smsTextWithUriString)`.
- Deep link: scheme `pillo://install` (`MainActivity` intent filter, `autoVerify`).
- **SMS is sent through Twilio** (`SmsSentReceiver` action `TWILLIO_SMS_SENT`;
  `handleCaregiverAlertSmsSendResult`). UI shows sender number **904-474-3566** and
  "*Available only in US & Canada*".

**Message templates** (English strings):
- Missed: "Pillo is sending this message on behalf of %1$s. The scheduled dose of %2$s was missed %3$s ago".
- Skipped: "Pillo is sending this message on behalf of %1$s, who has chosen to skip the %2$s dose of %3$s."
- All taken: "…to let you know that I've adhered to my med schedule 100% today…".
- Consent request: "I want to share missed dose alerts with you via the Pillo app, as you are my caregiver… Please click the link to consent…".

**Persistence:** `CareGiverDao`, `CareGiverSmsSendDao`,
`CareGiverRepository(Impl)` (`findAll/findById/insert/update/updateMany/delete`),
`CareGiverSmsSendRepository(Impl)` (dedupe via `existsSentBetween`),
`CareGiverStateLoader`, `AppDatabaseManager` delegates
(`CareGiverRepoDelegate`, `CareGiverSmsSendRepoDelegate`).

**Scheduling/sending:** `CareGiverNotiProvider` (`getActiveCareGiversFor…Noti`,
`getNotifiableEvents`, `reserveNoti`, `trySend…Noti`, `refreshMissedMedsNoti`) and
`CareGiverMissedMedsNotiReceiver`.

**Edit flow** (`ui/caregiver/edit`): name, phone, avatar, drugs, missed-med wait time,
noti types, **send history**, resend consent, delete. Marketing ties caregiver alerts to
the **Premium** tier.

---

## 6. Other notable implementation details

- **Multi-profile model:** user profiles + health profiles + dependents; transfer of
  drugs between profiles; `UserProfilePictures`.
- **Analytics taxonomy** in `analytics/tracking/AnalyticsEvent$…` covering Onboarding,
  Med, MedSafety (DDI/DFI/DCI/DAI report views), Alarm, Today, Progress, Settings —
  useful to reconstruct funnels.
- **Location:** geofencing (`GeofencingReceiver`), "my place" location-based snooze,
  drive detection (`LocationSpeedProcessor`, `domain/drive`).
- **Foreground/overlay strategy:** full-screen intents + `SYSTEM_ALERT_WINDOW`; Play
  reviews complain the "hard" alarm cannot be dismissed and can keep playing ads.
- **Vendor integrations observed in code/assets:** Twilio (caregiver SMS), Corca
  (`api.agent.corca.ai`, AI), Firebase, Google Maps/Places, Facebook SDK + Audience
  Network, AdMob, AppLovin, Moloco, InMobi, Unity Ads, Medisafe.

### Third-party identifiers & secrets found (intentionally redacted)

The APK embeds vendor identifiers and keys. Values that are credentials/keys are
**redacted here on purpose** so this document does not leak them:

- Google Maps SDK key: `<redacted>`
- AppLovin SDK key: `<redacted>`
- AdMob app id: `ca-app-pub-9029044310794961~<redacted>`
- Facebook app id: `497498338902453` (client token not extracted)
- Firebase web client id: `184760355354-<redacted>.apps.googleusercontent.com`

---

## 7. Limitations / unknowns

- Analysis is of APKPure **0.6.24**; the current Play build may differ.
- Base APK here is English; localized strings for `ko/de/es/ja/pt-BR/tr` were not analyzed.
- `classes6` (domain/data) and `classes4` (Hilt glue) were fully decompiled; UI DEX
  (`classes7/8/9`, `13`, `16`) were only partially decompiled. Screen-level Compose
  layout details and exact font-family assignment are **inferred** from strings,
  class names, and resources.
- No runtime execution: actual backend responses (`api.pillo.care`), Twilio delivery,
  and Corca AI prompts were not exercised.
- `rg`/reflection and dynamically loaded code remain unknown.

---

*Generated from static analysis of the shipped APK. Observations vs. inferences are
marked; unfinished coverage is stated explicitly.*
