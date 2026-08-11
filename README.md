# My Launcher

A Nova-inspired Android home screen launcher built with Kotlin and Jetpack Compose.

## Features

- **Home screen** with gradient wallpaper, large clock, and date
- **Swipeable pages** (2 home screens) with page indicators
- **Dock** with Phone, Messages, Browser, and Camera (auto-detected from installed apps)
- **Swipe up** or tap the dock pill to open the **app drawer**
- **Search** all installed apps in the drawer
- Registers as a **default home app** (`HOME` intent) so you can set it as your launcher

## Requirements

- Android 8.0+ (API 26)
- Android SDK for building locally

## Build

```bash
./gradlew assembleDebug
```

The APK is at `app/build/outputs/apk/debug/app-debug.apk`.

## Install on your phone

1. Enable **Developer options** and **USB debugging** on your device, or transfer the APK manually.
2. Install the APK:

   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

3. Press the **Home** button and choose **My Launcher** when prompted, or go to **Settings → Apps → Default apps → Home app**.

## Usage

| Gesture / action | Result |
|------------------|--------|
| Swipe up on home screen | Open app drawer |
| Tap dock pill | Open app drawer |
| Swipe down on drawer | Close drawer |
| Back button (drawer open) | Close drawer |
| Swipe left/right on home | Switch home pages |
| Tap dock or home icons | Launch app |

## Project structure

```
app/src/main/java/com/homelauncher/app/
├── MainActivity.kt       # Launcher entry + state
├── HomeScreen.kt         # Wallpaper, clock, dock, pages
├── AppDrawer.kt          # Searchable app grid
├── AppInfo.kt            # App loading and launching
├── LauncherPreferences.kt# Dock/home favorites persistence
└── LauncherTheme.kt      # Colors and theme
```

## License

MIT
