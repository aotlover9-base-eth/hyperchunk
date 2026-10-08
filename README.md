# hyperchunk

A minimal, 100% native Kotlin Android daily planner adhering to Xiaomi HyperOS 3 design aesthetics. Breaks the 24 hours of your day into intuitive, customizable focus blocks with buttery smooth spring physics, an interactive clock wheel picker, selective task alarms, and a native homescreen widget.

---

## The Problem

Traditional calendar and planner apps (Google Calendar, Notion, Todoist) suffer from two major flaws:
1. **Rigid Event Bloat**: Setting up a simple daily routine requires cluttering your calendar with 15 disconnected events, date pickers, and annoying invites.
2. **Missing 24-Hour Awareness**: Most apps display endless vertical lists without giving you an instant visual sense of how your entire 24-hour day is divided across sleep, routines, deep work, lectures, and rest.

`hyperchunk` replaces calendar friction with a dedicated 24-hour time-chunking system.

---

## Features

- **24-Hour Time-Chunking Engine**: Divide any day into customizable chunks (e.g., `00:00 - 06:00` Sleep, `06:00 - 07:00` Routine, `07:30 - 10:00` College Lecture, `10:00 - 13:00` Study/Free Slot).
- **HyperOS 3 Aesthetics**: Built from the ground up with Xiaomi HyperOS 3 design tokens—32dp ultra-rounded squircles, frosted glass surfaces, velvet dark backgrounds, and zero boxy rectangles.
- **Interactive Clock Wheel Picker**: Smooth vertical drum picker for Hours (`00-23`) and Minutes (`00-59`) with center magnification, snapping inertia, and haptic feedback.
- **Selective Exact Alarms**: Toggle sound and vibration alerts per task. Set an alarm for hydration, gym, or morning wake-up while keeping resting and study slots completely silent.
- **HyperOS 3 Homescreen Widget**: Native Android `AppWidgetProvider` card showing your active task, remaining time countdown, live progress bar, and up-next preview directly on your homescreen.
- **Day-of-Week Presets**: Configure unique schedules for Monday through Sunday with a 1-tap schedule duplication tool.
- **24-Hour Visual Ribbon**: Live proportional color gauge showing the day's time allocation with a real-time moving indicator needle.

---

## Architecture & Tech Stack

- **Language**: 100% Kotlin
- **UI Framework**: Jetpack Compose with Material 3 & custom HyperOS 3 design tokens
- **Architecture**: Single Activity, Repository Pattern, StateFlow reactive state
- **Background & Reminders**: Android `AlarmManager.setExactAndAllowWhileIdle()` + `NotificationManager` channels
- **Homescreen Widget**: Android `AppWidgetProvider` + custom RemoteViews
- **Persistence**: SharedPreferences with GSON JSON serialization

---

## Getting Started

### Prerequisites
- Android SDK 35 (compileSdk: 35, minSdk: 26)
- JDK 17
- Gradle 8.9 (included via wrapper)

### Build APK
```bash
./gradlew assembleDebug
```
The compiled APK will be generated at `app/build/outputs/apk/debug/app-debug.apk` (and root `hyperchunk.apk`).

### Install via ADB
```bash
adb install hyperchunk.apk
```

---

## License

MIT
