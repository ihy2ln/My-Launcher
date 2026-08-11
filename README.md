# Home Launcher

A Nova-inspired Android home screen launcher built with Kotlin and Jetpack Compose.

## Features

- **Edit Home Screen** — long-press the desktop to enter edit mode (matches the module grid with `+` slots)
- **Background** — color wheel, picture, or looping video wallpaper
- **Modules** — per-module opacity plus optional picture/video backgrounds
- **Add content** — apps, groups/folders, and widgets (clock, weather, app drawer)
- **Desktop + dock** — configurable grid (default 5×6), dock icons, live clock
- **App drawer** — swipe-up search, groups, hide apps
- **Themes & gestures** — light/dark, accents, swipe/double-tap/pinch actions
- **Backup & restore** — export/import JSON setups

## Install

Download the latest APK from the [Releases](https://github.com/ihy2ln/My-Launcher/releases) page.

1. Install **HomeLauncher-v0.4.0.apk**
2. Press **Home** → choose **Home Launcher** → **Always**

### Build from source

```bash
./gradlew assembleRelease
```

## Edit mode

| Action | Result |
| --- | --- |
| Long-press home | Enter Edit Home Screen |
| Tap **Background** | Color wheel / picture / video |
| Module opacity slider | Adjust empty module transparency |
| Tap `+` | Add app, widget, or group |
| Module style | Per-cell opacity, picture, or video |
| **Done** | Leave edit mode |

## Requirements

- Android 8.0 (API 26) or newer
