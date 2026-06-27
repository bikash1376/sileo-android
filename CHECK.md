# CHECK.md — Tuning & rebuild guide

A map of every number that controls the **pill** and **gooey** look, with exact
`file:line`, plus the fastest rebuild-and-look loop. Line numbers are accurate as of
this commit; if you add/remove lines they shift, so search the snippet if a number
looks off.

---

## 1. Rebuild & look (the fast loop)

From `D:\noti` (PowerShell or the Bash tool):

```bash
# Build + install to the running emulator/device in one step
./gradlew :app:installDebug

# Launch it
D:/Android/Sdk/platform-tools/adb.exe shell am start -n com.sileo.island/.MainActivity

# Fire a toast WITHOUT touching the screen (taps the "Success" chip):
D:/Android/Sdk/platform-tools/adb.exe shell input tap 221 2167
#   Error=450,2167  Warning=675,2167  Info=895,2167  Action=415,2510  Promise=650,2510

# Grab a screenshot to a file you can open:
D:/Android/Sdk/platform-tools/adb.exe exec-out screencap -p > shot.png
```

Start the emulator if it isn't running:
```bash
D:/Android/Sdk/emulator/emulator.exe -avd revpdf -gpu host
```

Tip: after `installDebug`, **fully relaunch** (or `am force-stop com.sileo.island` first)
so old toasts don't linger. Wait ~3s after firing before screenshotting — the morph
spring is slow on purpose.

> Prefer Android Studio? Open `D:\noti` as a project and hit Run ▶. Same result.

---

## 2. THE PILL (the top capsule: badge + title)

All in `app/src/main/java/com/sileo/island/ui/SileoToast.kt`.

| What | Where | Now | Effect of changing |
|------|-------|-----|--------------------|
| **Pill height** | `SileoToast.kt:68` `pillHeightDp = 48.dp` | 48 | Taller/shorter capsule. Also sets the pill's corner roundness (it's `height/2`, fully round). |
| **Pill min length** | `SileoToast.kt:148` `widthIn(min = 210.dp, …)` | 210 | Minimum pill width even for short titles. Raise → longer pill. |
| **Pill max length** | `SileoToast.kt:148` `widthIn(…, max = 360.dp)` | 360 | Cap before the title ellipsizes. |
| **Pill side padding** | `SileoToast.kt:150` `padding(horizontal = 28.dp)` | 28 | Space left/right of badge+title → makes the pill longer/shorter around the text. |
| **Badge ↔ title gap** | `SileoToast.kt:153` `spacedBy(10.dp)` | 10 | Gap between the colored badge and the title. |
| **Badge size** | `SileoToast.kt:156` `Modifier.size(22.dp)` | 22 | Diameter of the colored circle icon. |
| **Title font size** | `SileoToast.kt:161` `fontSize = 15.sp` | 15 | Title text size. |
| **Pill corner radius** | `SileoToast.kt:129` `CornerRadius(pillHeightPx / 2f)` | height/2 | Change `/2f` to a fixed px for a less-round pill. |

---

## 3. THE GOOEY BODY (the panel that drops down with the description)

`app/src/main/java/com/sileo/island/ui/SileoToast.kt`.

| What | Where | Now | Effect |
|------|-------|-----|--------|
| **Body width** | `SileoToast.kt:185` `width(316.dp)` | 316 | Total body panel width. **Keep it fixed** — this is what makes the measured height match the drawn height. |
| **Body side padding** | `SileoToast.kt:186` `padding(horizontal = 26.dp)` | 26 | Inset of the description text from the body edges. |
| **Body top/bottom padding** | `SileoToast.kt:187` `padding(top = 10.dp, bottom = 22.dp)` | 10 / 22 | Vertical breathing room → controls body **height** around the text. |
| **Body corner radius** | `SileoToast.kt:74` `bodyCornerPx = 26.dp` | 26 | Roundness of the body panel's bottom corners. |
| **Description font** | `SileoToast.kt:199` `fontSize = 13.5.sp` | 13.5 | Description text size. |
| **Description line height** | `SileoToast.kt:200` `lineHeight = 18.sp` | 18 | Line spacing → taller/shorter body for multi-line text. |

> The body height is **derived from its content**, not hard-coded. To make it taller,
> increase the paddings (`:187`), font (`:199`), or line height (`:200`) — or give it
> longer text. It can never clip now: it's measured at full size and revealed via a
> spring (`SileoToast.kt:181-193`, the `.layout { … reveal … }` block).

---

## 4. THE GOO MERGE (how the pill melts into the body — the "liquid neck")

| What | Where | Now | Effect |
|------|-------|-----|--------|
| **Neck overlap** | `SileoToast.kt:73` `overlapPx = 22.dp` | 22 | How far the body tucks UP under the pill. Bigger → taller, more pronounced liquid neck. |
| **Base blur** | `SileoToast.kt:71` `baseBlurPx = 12.dp` | 12 | Core gooeyness. Bigger → softer/blobbier merge (and rounder corners). |
| **Motion-blur peak** | `SileoToast.kt:72` `peakBlurPx = 10.dp` | 10 | Extra blur added *during* the morph for the "stretch" feel, then it settles. 0 = no motion blur. |
| **Goo threshold** | `GooeyEffect.kt:40` `0f,0f,0f, 18f, -2295f` | 18 / -2295 | The alpha ramp that snaps the blur back into a solid shape. Higher `18` = crisper edges; the `-2295` bias is `≈ -9 × 255` and sets where the edge sits. This is the metaball math — tweak carefully. |
| **Blur tile mode** | `GooeyEffect.kt:31` `Shader.TileMode.DECAL` | DECAL | Leave as-is (fades blur to transparent at edges). |

---

## 5. ANIMATION & TIMING (speed of the morph and the on/off lifecycle)

| What | Where | Now | Effect |
|------|-------|-----|--------|
| **Morph spring** | `SileoToast.kt:58` `dampingRatio = 0.72f, stiffness = 150f` | 0.72 / 150 | The open/close feel. **Higher stiffness = faster** (try 220 for snappier). Lower damping = more bounce. |
| **Body fade speed** | `SileoToast.kt:90` `tween(if (expanded) 280 else 140)` | 280 / 140 | How fast the description text fades in/out. |
| **Delay before expand** | `SileoHost.kt:30` `AUTO_EXPAND_DELAY_MS = 480L` | 480 | How long the collapsed pill shows before it morphs open. |
| **Collapse settle time** | `SileoHost.kt:32` `COLLAPSE_SETTLE_MS = 820L` | 820 | Pause for the close morph to finish before it leaves. |
| **Exit time** | `SileoHost.kt:33` `EXIT_MS = 460L` | 460 | Fade/scale-out duration. |
| **Enter pop** | `SileoHost.kt:82-84` `initialScale = 0.85f`, `spring(0.6f, 380f)` | — | The scale-in when a toast appears. |
| **Stack spacing** | `SileoHost.kt:50` `spacedBy(12.dp)` | 12 | Gap between stacked toasts. |

---

## 6. COLORS

| What | Where | Now |
|------|-------|-----|
| **Island (dark) fill** | `SileoToast.kt:53` `ISLAND = 0xFF1B1B1D` | near-black |
| **Title text** | `SileoToast.kt:54` `TITLE = 0xFFF4F4F5` | off-white |
| **Description text** | `SileoToast.kt:55` `SUBTLE = 0xFFAEAEB4` | grey |
| **Variant accents** | `Sileo.kt:17-22` | success `#34C759`, error `#FF453A`, warning `#FFB020`, info `#0A84FF`, action `#5E5CE6`, promise `#8E8E93` |

---

## 7. DURATIONS & THE PROMISE FLOW

`app/src/main/java/com/sileo/island/Sileo.kt`.

| What | Where | Now | Effect |
|------|-------|-----|--------|
| **Default on-screen time** | `Sileo.kt:32` / `:65` `durationMs = 6000L` | 6000 | Total time a toast lives. |
| **Promise: time to resolve** | `Sileo.kt:106` `resolveAfterMs = 1900L` | 1900 | "Saving…" duration before it flips to "Saved". |
| **Promise: total life** | `Sileo.kt:110` `durationMs = resolveAfterMs + 2900L` | +2900 | How long the resolved "Saved" stays before leaving. |
| **Promise resolved text** | `Sileo.kt:104-105` `resolvedTitle`, `resolvedDescription` | "Saved" / … | What it morphs into. |

---

## 8. Where each file lives

```
app/src/main/java/com/sileo/island/
  Sileo.kt          # variants, colors, durations, the promise flow      (§6, §7)
  MainActivity.kt   # the demo screen + which chip fires which toast
  ui/
    SileoToast.kt   # the island: pill, gooey body, morph, neck           (§2, §3, §4, §5)
    GooeyEffect.kt  # the blur + threshold that makes the goo             (§4)
    SileoHost.kt    # stacking + appear/expand/collapse/exit timing       (§5)
    Badge.kt        # the drawn variant icons + loading spinner
```
