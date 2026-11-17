# Reels Counter — Android prototype

A native Kotlin Android proof of concept for local, encrypted short-video counts.

## Build an installable APK

Requirements: Android Studio (JDK 17), Android SDK Platform 35 and build tools, Gradle 8.9.

1. Open the `ReelsCounter` folder in Android Studio.
2. Allow Android Studio to sync the Gradle project and download dependencies.
3. Select **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
4. Find the debug APK at `app/build/outputs/apk/debug/app-debug.apk`.
5. Transfer the APK to your Android phone, allow installation from your file manager, then install.
6. Open **Reels Counter > Enable / manage tracking**, find **Reels Counter tracking** under Installed accessibility services, and enable it.
7. Repeat installation and permission setup on each friend's phone.

## Important limitations

- **This ZIP contains source code, NOT a compiled APK.** The current build environment does not have Android SDK/Gradle installed, so compilation and on-device testing have not been performed.
- This is an **experimental counter**: Instagram, Facebook and YouTube do not expose a reliable public cross-app reel-scroll API. Accessibility events are inconsistent across versions and devices; counters may be inaccurate, including false positives.
- This prototype implements daily/7-day counts and erase-history. Time spent, monthly charts, and daily limit notifications are **not implemented**.
- Only encrypted aggregate counts by date and platform are stored. Visible screen text is processed in memory for detection and is never persisted or transmitted.
- AES-256-GCM uses a key managed by Android Keystore. `allowBackup=false` and data-extraction exclusions prevent routine Android backup and transfer. No `INTERNET` permission is requested.
- Encryption is not a promise of absolute secrecy against rooted devices, malware, compromised OS, or attackers who control the unlocked device.
- For distributing to friends, debug APKs are suitable for a small test. For broader release, use a signed release build and review Android Accessibility policy and consent requirements.
