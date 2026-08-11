# Home Launcher

A Nova-inspired Android home screen launcher built with Kotlin and Jetpack Compose.

## Features

- **Home screen** with clock, date, and a 4×5 app grid
- **Dock** with five pinned app slots
- **Swipe-up app drawer** with search
- **Customize shortcuts** by tapping empty slots or long-pressing apps in the drawer
- **Persistent layout** saved across restarts

## Install on your phone

1. Open this project in Android Studio (or build from the command line).
2. Connect your Android device with USB debugging enabled, or use an emulator.
3. Build and run the `app` module.
4. Press the Home button and choose **Home Launcher** when prompted.
5. Select **Always** to set it as your default launcher.

### Build from the command line

```bash
./gradlew assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`. Transfer it to your phone and install, or run:

```bash
./gradlew installDebug
```

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
