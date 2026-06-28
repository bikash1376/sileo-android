<div align="center">

# Sileo · Android

An Android port of **[Sileo](https://sileo.aaryan.design)** — the opinionated,
physics-based "Dynamic Island"–style toast by
**[Aaryan](https://github.com/hiaaryan/sileo)**.

Sileo is a React component (SVG morphing + spring physics). This project recreates
that look **natively in Kotlin + Jetpack Compose**, and then takes it further:
turning it into a real **notification island** that floats over any app.

<img src="app/public/demo.gif" width="280" alt="Sileo Android demo" />

▶️ **[Watch the full demo (with audio)](https://github.com/bikash1376/sileo-android/raw/main/app/public/demo.mp4)**

</div>

---

## What it is

A black, rounded **pill** that springs open into a wider **body** and back, with the
two shapes fused by a liquid "gooey" join — just like the original Sileo. It comes in
the same variants: **success · error · warning · info · action · promise**.

There are two flavors, on two branches:

| Branch | What it is |
|--------|------------|
| **`main`** | The **island component + a playground**. A standalone app with buttons that fire each variant so you can see/tune the look. No special permissions. |
| **`listener`** | The **real notification island**. Reads your actual notifications and shows them as the island over any app, with a per-app picker. |

---

## How it works

**The island (both branches).** The "goo" is reproduced with Android's
[`RenderEffect`](https://developer.android.com/reference/android/graphics/RenderEffect):
a **blur** chained with an **alpha-threshold color matrix** — the exact blur-then-snap
trick the web version does with its SVG filter. Two rounded rectangles (a narrow pill
+ a wider body) are drawn into that layer, and the blur fuses them into a smooth
concave neck. The open/close morph is a Compose **`spring`** tuned to match
framer-motion's feel. The body is revealed with a custom `layout` so its measured
height always matches what's drawn (no text clipping).

**The notification island (`listener` branch).** On a normal (non-rooted) phone you
**can't replace** Android's notification renderer — so, like DynamicSpot / Dynamic
Island apps, Sileo:

1. Uses a **`NotificationListenerService`** to read incoming notifications.
2. Draws the island in a **`SYSTEM_ALERT_WINDOW`** overlay on top of everything.
3. **Classifies** each notification into the right variant (progress → promise,
   error → error, alarm/call/actions → action, message/email/social → info) and shows
   the **posting app's icon**.
4. **Suppresses the default heads-up** for your chosen apps (so it feels like Sileo
   *instead of* the stock popup). The notification still lives in the shade.

You pick **which apps** use Sileo; everything else stays stock Android.

---

## Tech

Kotlin · Jetpack Compose · `RenderEffect` goo · Compose `spring` physics ·
`NotificationListenerService` + overlay window. minSdk 26, targetSdk 36.
Gooey effect needs Android 12+ (gracefully degrades below).

---

## Build & run

You don't strictly need Android Studio — the Gradle wrapper + Android SDK is enough.

```bash
# Build + install to a running emulator / connected device (Android 12+)
./gradlew :app:installDebug

# Launch
adb shell am start -n com.sileo.island/.MainActivity
```

Or open the folder in **Android Studio** and hit Run ▶.

### Testing `main` (the playground)
Tap the **Success / Error / Warning / Info / Action / Promise** buttons — each fires
the island so you can see the morph.

### Testing `listener` (real notifications)
```bash
git checkout listener
./gradlew :app:installDebug
```

---

## Set it up on your phone

The `listener` branch is meant to run on a real phone (Android 12+). You can build and
push it over USB, or just sideload the APK.

**1. Get the app on the phone**
```bash
# Build the APK
./gradlew :app:assembleDebug
# …then install it over USB (-r reinstalls without the Play Protect dialog)
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
Or copy `app/build/outputs/apk/debug/app-debug.apk` to the phone and tap it.

> **Play Protect warning?** This is an unsigned, sideloaded app that reads notifications
> and draws overlays, so Play Protect may warn. It's a false positive — choose
> *Install anyway* (or use `adb install -r`, which skips the dialog).

**2. Grant permissions, in the app**
1. **Notification access** — lets Sileo read incoming notifications.
2. **Display over other apps** — lets it draw the island on top.
3. **Choose apps** — pick which apps should show as the island.

**3. Try it**
Tap **Send a real test notification**, or trigger a notification from a chosen app — it
pops as the island over whatever's on screen. Tap the **gooey body** to open the source
app; tap the **pill** to collapse it; **swipe up** to dismiss.

### Xiaomi / POCO / Redmi (MIUI / HyperOS)
MIUI has extra gates beyond stock Android. If the island doesn't appear for real
notifications, in **Settings → Apps → Sileo Island**, also enable:
- **Display pop-up windows while running in the background** (the critical one)
- **Autostart**
- Battery saver → **No restrictions**

Then toggle **Notification access** off and back on so the listener re-binds.

> A physical Android 12+ device is best here, so you can see it react to your real
> messages, downloads, alarms, etc. The gooey effect needs Android 12+ (it degrades
> gracefully below).

---

## Status & caveats

- ✅ The island look, all variants, the promise resolve flow.
- ✅ Real notifications → island overlay, per-app picker, app-icon badges, heads-up
  suppression.
- ✅ **Interactive island** — the overlay is a touch-transparent wrap-content window, so
  only the island catches taps (the rest of the screen still passes through). Tap the
  body to open the source app; tap an action chip to fire its action.
- ⚠️ **Inline reply isn't possible** from an overlay (it needs the system's notification
  UI), so a Reply action opens the app instead of sending text in place.
- Heads-up suppression uses the snooze trick; a brief flash is possible on some OEMs.
- Per-device camera-cutout positioning is approximate for now.

---

## Privacy

Sileo reads your notifications and draws over other apps — powerful access, so here's
exactly what it does with it (this is also in-app, under **What about privacy?**):

- **Nothing leaves your phone.** Sileo has **no internet permission** at all — no
  servers, no analytics, no tracking. Your notifications can't be uploaded or shared.
- **Nothing is stored.** No notification history is kept; the only saved data is the
  short list of apps you picked, in private on-device storage.
- **Your content is never logged.** Release builds never write notification titles or
  text to the device log, so OTPs, messages and banking alerts stay private. (Diagnostic
  logging exists only in debug builds, and even then it logs package/category — never
  content.)
- **It can't see your taps.** The overlay only redraws a notification you already
  received; it can't read what you type or what's underneath it.
- **You choose what's included.** Only the apps you pick become islands; everything else
  stays stock Android.
- `allowBackup` is disabled so the app's data isn't pulled into device backups.

> **Note on replies & actions:** Sileo is *not* a full notification replacement. Reply
> and action buttons don't work inline like the native notification — tapping one just
> **opens the respective app** (an overlay can't host the system's inline reply UI).

---

## Open source & feedback

Sileo is fully open source — read every line, fork it, or build it yourself.

- **Source:** <https://github.com/bikash1376/sileo-android>
- **Found a bug or have feedback?** Open an issue:
  <https://github.com/bikash1376/sileo-android/issues/new>

---

## Credits

Design and concept: **[Sileo](https://sileo.aaryan.design)** by
**[Aaryan](https://github.com/hiaaryan/sileo)** (MIT). This is an independent Android
re-creation built for learning and personal use.
