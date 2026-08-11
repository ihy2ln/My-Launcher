# Home Launcher

A Nova-inspired Android home screen launcher built with Kotlin and Jetpack Compose.

## Features (Nova-style)

- **Desktop** — clock, configurable grid (3–6 columns / 4–7 rows), dock, folders
- **App drawer** — swipe-up, search, vertical/horizontal scroll, custom groups/tabs
- **Search micro-results** — calculator and unit conversions (e.g. `12*7`, `10 km to mi`)
- **Icon theming** — circle / squircle / square / teardrop shapes, size & label controls
- **Themes** — light / dark / system, accent colors, Material You toggle, wallpaper styles
- **Gestures (Prime)** — swipe up/down, double-tap, pinch-in → drawer, search, settings, notifications
- **Hide apps** — hide from drawer without uninstalling
- **Folders** — stack apps on the home screen to create folders
- **Backup & restore** — export/import full layout + settings as JSON

## Install

Download the latest APK from the [Releases](https://github.com/ihy2ln/My-Launcher/releases) page.

1. Install **HomeLauncher-v0.3.0.apk**
2. Press **Home** → choose **Home Launcher** → **Always**

### Build from source

```bash
./gradlew assembleRelease
```

APK: `app/build/outputs/apk/release/app-release.apk`

## Usage

| Action | Result |
| --- | --- |
| Swipe up | App drawer (configurable) |
| Swipe down | Search / notifications (configurable) |
| Double-tap / pinch | Configurable gesture |
| Long-press home | Open Nova Settings |
| Tap empty slot | Place app |
| Place app on another app | Create folder |
| Long-press drawer app | Add / hide / group |
| Settings → Backup | Save or restore your setup |

## Requirements

- Android 8.0 (API 26) or newer
