# DEX FOCUS — Android app blocker

This is a self-control focus tool, not a device-management or anti-removal app.

## Current behavior
- First launch selects all visible launcher apps except essential dialer, emergency, Android Settings, System UI, permission controller and package installer packages.
- One shared daily usage budget applies across the selected apps. Default: 180 minutes (3 hours).
- A selected app first shows an intention pause; after the pause, the user can choose a short 5-minute pass.
- A configured quiet schedule blocks selected apps during the chosen hours.
- Calls and essential Android recovery/settings paths are intentionally kept available.

## Required setup
1. Install the debug APK.
2. Open DEX FOCUS and grant Usage Access.
3. Enable its Accessibility service in Android Settings.
4. Review the selected-app list and ensure Phone, emergency and other essential apps remain available.
5. Set the daily budget and quiet hours.

## Important Android limitations
- Usage Access and Accessibility are user-granted special permissions. Android does not let an ordinary Play-installed app silently grant itself these permissions.
- A regular app cannot guarantee that it is impossible to uninstall, force-stop, disable, or bypass. This project intentionally does not hide itself, lock the owner out of Settings, or interfere with emergency calling.
- The three-hour budget is the combined foreground usage of selected apps, not a hard cap on every minute the phone screen is on. Android usage statistics can be delayed and manufacturer battery controls may stop background services.
- Do not select system-critical apps. Keep phone, emergency, Settings and recovery access available.

## Build
The repository workflow `.github/workflows/android-build.yml` builds a debug APK with GitHub Actions and uploads it as an artifact when the workflow runs.
