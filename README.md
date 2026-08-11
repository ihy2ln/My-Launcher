# Home Launcher

A Nova-inspired Android home screen launcher built with Kotlin and Jetpack Compose.

## Features

- **Home screen** with clock, date, and a 4×5 app grid
- **Dock** with five pinned app slots
- **Swipe-up app drawer** with search
- **Customize shortcuts** by tapping empty slots or long-pressing apps in the drawer
- **Persistent layout** saved across restarts

## Install on your phone

Download the latest APK from the [Releases](https://github.com/ihy2ln/My-Launcher/releases) page, transfer it to your Android device, and open it to install. You may need to allow installs from unknown sources.

Or build from source:

```bash
./gradlew assembleDebug
```

The APK is written to `app/build/outputs/apk/release/app-release.apk`. Transfer it to your phone and install, or run:

```bash
./gradlew installRelease
```

After installing, press the Home button and choose **Home Launcher** → **Always**.

## Usage

| Action | Result |
| --- | --- |
| Swipe up from the home screen | Open the app drawer |
| Swipe down in the app drawer | Close the drawer |
| Tap an app | Launch it |
| Tap an empty home or dock slot | Pick an app from the drawer |
| Long-press an app in the drawer | Add it to home or dock |
| Long-press a home or dock shortcut | Remove it |

## Requirements

- Android 8.0 (API 26) or newer
- Kotlin 2.0+
- Jetpack Compose
