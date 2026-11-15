# My Blocker (Android)

A local Android focus helper. Default daily budget: **180 minutes (3 hours) shared across the apps selected in the app list**.

## Build

GitHub Actions builds a debug APK on pushes affecting `Blocker/**`, this README, or the workflow.

1. Open the **Actions** tab.
2. Select **MyBlocker Android APK**.
3. Open the latest successful run.
4. Download the `MyBlocker-debug-apk` artifact.

## First-time setup on Android

1. Install the APK.
2. Open My Blocker.
3. Enable **Accessibility** for My Blocker in Android Settings.
4. Enable **Usage access** for My Blocker.
5. Keep essential system, phone, emergency and Settings apps available.
6. Select the apps you want included in the shared 3-hour budget.

## What it does

- Adds an intention pause before opening selected apps.
- Offers a short 5-minute pass after the pause.
- Blocks selected apps after their combined foreground usage reaches the configured daily budget.
- Supports a time-of-day schedule.

## Important limitations and safety

- This is **not** a full-device screen-time meter; it counts usage for selected launchable apps only.
- Android UsageStats can update with a delay. Accessibility services may be stopped by device battery management, including some Xiaomi/Redmi settings.
- This app does not and should not prevent emergency calls, lock the owner out of Android Settings, hide itself, or make itself impossible to uninstall.
- A debug APK is for testing. It is not a Play Store release and is not independently security-audited.
- Test on the target phone before relying on it. Verify calls, emergency access, Settings, alarms and other essential functions remain usable.
