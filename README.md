# Sileo · Android

An Android port of [**Sileo**](https://github.com/hiaaryan/sileo) by Aaryan — the
opinionated, physics-based "Dynamic Island"–style toast for React. This project
recreates the look (gooey morphing pill + spring physics) natively in **Kotlin +
Jetpack Compose**, with the longer-term goal of using it as a real notification
overlay that replaces the system heads-up banner.

Original web demo: https://sileo.aaryan.design/play · License: MIT (design ported with that in mind).

---

## Status at a glance

| Phase | What | State |
|------|------|-------|
| **1** | Visual demo — the Sileo island, fired by buttons | ✅ **Built & compiling** |
| **2** | Wire to real notifications (read + overlay on top of all apps) | ⬜ Not started |
| **3** | Polish, settings, device-cutout positioning, distribution | ⬜ Not started |

> **Are we done?** Phase 1 (the look) is built and produces a debug APK. Phases 2–3
> (the actual "replace your notifications" behaviour) are still ahead. See below.

---

## ✅ Done (Phase 1)

- **Gradle project** with wrapper (Gradle 8.9, AGP 8.7.3, Kotlin 2.0.21, Compose BOM
  2024.10.01). Builds from the command line — **no Android Studio required**.
- **The gooey morph** (`ui/GooeyEffect.kt`) — reproduces Sileo's SVG filter
  (`feGaussianBlur` → alpha-threshold `feColorMatrix`) using the platform
  `RenderEffect` graph: blur → alpha ramp, so the pill and body **merge like liquid**.
  Gated to Android 12+ (API 31); older devices fall back to a clean spring morph.
- **The island** (`ui/SileoToast.kt`) — a header pill (badge + title) that springs
  open into a body (description + optional action button). Content is measured so any
  title/description length fits; heights are spring-animated.
- **Spring physics** tuned to match framer-motion's `bounce: 0.25 / 0.6s`
  (`spring(dampingRatio = 0.62, stiffness = 320)`).
- **Variants** (`Sileo.kt`): success / error / warning / info / action / promise, with
  the original OKLCH accent colors converted to sRGB, drawn glyphs, and a spinning
  loader for the promise/loading state.
- **Autopilot host** (`ui/SileoHost.kt`) — stacks toasts at top-center and runs each
  one's appear → auto-expand → hold → collapse → exit timeline. Tap to expand/collapse.
- **Demo screen** (`MainActivity.kt`) — the playground: chips that fire each variant.

### File map
```
app/src/main/java/com/sileo/island/
  Sileo.kt            # variants, ToastData, the sileo.success()/error()/… controller
  MainActivity.kt     # demo "playground" screen
  ui/
    GooeyEffect.kt    # the blur + alpha-threshold RenderEffect (the "goo")
    SileoToast.kt     # the morphing island (pill ⇄ body)
    Badge.kt          # drawn variant glyphs + loading spinner
    SileoHost.kt      # stacking + auto expand/collapse/dismiss lifecycle
```

---

## ⬜ Left to do

### Phase 2 — make it a real notification island
- **`NotificationListenerService`** to read incoming notifications (requires the user
  to grant *Notification access* in Settings).
- **Overlay window** via `WindowManager` + `SYSTEM_ALERT_WINDOW` ("Display over other
  apps") to paint the island on top of every app, anchored near the camera cutout.
- **Foreground service** to keep the listener/overlay alive under background limits.
- Map a real `StatusBarNotification` (app icon, title, text, actions) → `ToastData`.
- Optionally **suppress the original heads-up banner** so only the island shows.

### Phase 3 — polish & ship
- Per-device **cutout positioning** (notch/punch-hole varies by phone).
- Tap-through / expand-on-tap / swipe-to-dismiss on the overlay.
- Settings: position, duration, which apps, light/dark.
- Decide distribution (sideload APK vs Play Store — overlay + notification-listener
  apps face stricter Play review).
- Reduced-motion + pre-API-31 fallbacks verified on real hardware.

### Known limitations / honest caveats
- On non-rooted Android you **cannot** truly replace the OS notification renderer.
  The realistic model (same as DynamicSpot / Dynamic Island apps) is: **read** via the
  listener, **draw your own island overlay** on top, optionally **hide the heads-up**.
  Notifications still live in the shade. True system replacement needs root/custom ROM.
- The goo effect needs **Android 12+**; below that it degrades to a plain spring morph.
- Not yet visually verified on a device/emulator (next step).

---

## Build & run

Prereqs (already present on the dev machine): JDK 17, Android SDK at `D:\Android\Sdk`,
build-tools 35.0.0, an emulator or a USB device (Android 12+ recommended).

```bash
# Build the debug APK
./gradlew :app:assembleDebug

# Install to a running emulator / connected device
./gradlew :app:installDebug
# (or)  adb install -r app/build/outputs/apk/debug/app-debug.apk

# Launch
adb shell am start -n com.sileo.island/.MainActivity
```

Then tap **Success / Error / Warning / Info / Action / Promise** to fire the island.
