# ISEEU

Private, sideload-only family location sharing. Up to 10 trusted people, no login — create or join a family with a short code and see everyone on the map. Not published to Play Store; distributed as an APK directly to family members.

Phase 1 (this build): onboarding, real-time map, privacy toggle, profile editing, adaptive location updates. See `family_locator_dev_prompt.md` for the full 3-phase brief.

## Stack

Kotlin, Jetpack Compose, MVVM + Repository, Hilt. Firebase (Firestore + Anonymous Auth — no Cloud Functions, stays on the free Spark plan). Maps via osmdroid/OpenStreetMap (no Google Maps API key needed).

## One-time setup

### 1. Android Studio

1. Install from `developer.android.com/studio` (default options — bundles a matching JDK and the SDK manager).
2. Open this folder (`D:\ISEEU`) as a project and let Gradle sync (the wrapper is already checked in, pinned to Gradle 9.7.1).
3. Connect a physical device over USB with Developer Options → USB debugging on, or create an emulator via Device Manager (any recent Pixel profile is fine).

### 2. Firebase project

1. [console.firebase.google.com](https://console.firebase.google.com) → **Add project** (Google Analytics is optional, fine to skip).
2. **Add an Android app** inside the project — package name `com.iseeu.app`. SHA-1 is not required (this app only uses Anonymous Auth, no Google Sign-In/Dynamic Links).
3. Download `google-services.json` and place it at `app/google-services.json` (already gitignored — it's project-specific and shouldn't be committed).
4. **Build → Firestore Database → Create database** (production mode, pick a region close to your family).
5. **Build → Authentication → Sign-in method → enable Anonymous.**
6. **Firestore → Rules tab** → paste the contents of `firestore.rules` from this repo → **Publish**. (If you install the Firebase CLI later, `firebase deploy --only firestore:rules` does the same thing from the command line.)

### 3. Build and run

Once both of the above are done, use Android Studio's Run button with a connected device/emulator selected.

## Build verification already done

This machine has no Android SDK, so a full compile hasn't been possible here — but the Gradle project itself has been build-tested against a real Gradle 9.7.1 + JDK install: `./gradlew help` and `./gradlew :app:dependencies` both pass cleanly, meaning every plugin (AGP, the Compose compiler, KSP, Hilt, google-services) and every single dependency version in `gradle/libs.versions.toml` is confirmed to actually exist and resolve — not just guessed. Along the way this caught and fixed several real version/config errors (wrong KSP version, a too-old Hilt incompatible with AGP 9's internal APIs, the now-removed `org.jetbrains.kotlin.android` plugin conflicting with AGP 9's built-in Kotlin support, `kotlinOptions{}` needing to become `kotlin { jvmToolchain(17) }`). What's left unverified is the actual Kotlin/Compose source compiling cleanly against real Android SDK APIs — first thing to check once Android Studio is up.

## Notes on this build

- **No Google Maps API key** — osmdroid renders OpenStreetMap tiles directly, no Google Cloud billing account needed. The map screen shows an on-map OSM attribution as required by their usage terms.
- **No FCM** — the original brief's "wake device via push to fetch a live location" is instead a pure Firestore listener: tapping a member's pin writes a `refreshRequestedAt` field their own device listens for. Works whenever their app/service is alive; falls back to their last known location otherwise. See the plan doc for the full reasoning.
- **Auto-resumes tracking after a reboot** via a boot receiver — not in the original brief, added because a silently-stopped-sharing phone after a routine restart is the failure mode that matters most for exactly who this app is likely to track (kids, elderly parents).
- App icon/wordmark assets are in `assets/` (SVG sources + PNG exports); the launcher icon and the Welcome screen wordmark are already wired in from there.

## Known gaps (by design, for Phase 1)

- No "leave family" flow — an old/retired device stays listed. Cheap to add later.
- No enforced 10-member cap — purely a design assumption, not a rule in code.
- `ACTIVITY_RECOGNITION` permission isn't requested yet — deferred to Phase 3, which is what actually needs it.
