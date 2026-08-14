# Home Launcher

A modern Nova/Lawnchair-inspired Android home screen launcher built with Kotlin and Jetpack Compose.

## Features (v0.12.0)

- **Multi-page home** — swipeable pages with scroll effects, configurable grid & dock
- **Floating widgets** — blank/native AppWidgets, clock, live weather, music/video/game, search, calendar, notes
- **PiP app frames** — open apps in an on-home picture-in-picture style window
- **Live media cards** — themed per-app drawer cards for every active media session
- **Notification badges** — unread counts from the notification listener
- **App shortcuts** — long-press sheet with Android launcher shortcuts
- **Suggested apps + A–Z scrubber** in the drawer
- **Material You** dynamic colors (Android 12+)
- **Gestures** — swipe, double-tap, pinch → drawer / search / settings / edit
- **Folders, groups, aliases, hide apps, backup/restore**

## Install

Download the latest APK from the [Releases](https://github.com/ihy2ln/My-Launcher/releases) page.

1. Install **HomeLauncher-v0.12.0.apk**
2. Press **Home** → choose **Home Launcher** → **Always**
3. Grant **Notification access** for live media cards and badges
4. (Optional) Grant **Usage access** for Suggested apps; **Location** for local weather

### Build from source

```bash
./gradlew assembleRelease
```

## Tips

| Action | Result |
| --- | --- |
| Long-press app | Action sheet (shortcuts, dock, folder, info…) |
| Long-press widget | Drag to reposition |
| Pinch in / gear | Edit Home Screen |
| Tap app widget | Open in-home PiP frame |
| Swipe up | App drawer |

## Requirements

- Android 8.0 (API 26) or newer
