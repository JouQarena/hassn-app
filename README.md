# 🛡️ DNS Guard & Shield

A production-ready Android application combining **system-wide Private DNS filtering**
with a **Reddit-only NSFW accessibility shield** — built entirely with
**Jetpack Compose (Material 3)**, dark mode only, zero purple, zero placeholders,
**zero ADB**.

| Concern | Guarantee |
| --- | --- |
| DNS protection | Android Private DNS (DNS-over-TLS), with the selected hostname persisted by the app. After the optional ADB grant of `WRITE_SECURE_SETTINGS`, a foreground service restores changes on an I/O coroutine via a debounced `ContentObserver` plus a poll fallback. Without that grant it cannot write Private DNS; no `INTERNET` permission is needed. |
| Reddit NSFW shield | The accessibility service overlays NSFW content **only** while `com.reddit.frontpage` is in the foreground |
| Banking-app safety | The shield **removes itself from Android's enabled-accessibility-services the instant any other app appears** (or the screen turns off, or a 30-second grace window expires without Reddit) — enforced by a unit-tested state machine plus an independent watchdog. It never writes `Settings.Secure` at all |
| Data egress | The app declares **no `INTERNET` permission** — nothing ever leaves the device |

---

## Features

### Screen 1 — Main Dashboard
* `#0D47A1` TopAppBar — "🛡️ DNS Guard & Shield" + `🌐 EN` language toggle (**English ↔ Arabic**, full RTL layout mirroring when Arabic is active)
* **DNS Protection** card: green/red status dot + current hostname in `#64B5F6`
* **Reddit NSFW Shield** card: three states — *incomplete setup*, *standby (service in control)*, *active — Reddit is in the foreground*, with a live "Reddit is open right now" hint from the watchdog
* **Permissions Status** card: `✅ / ⚠️` rows for
  `Accessibility Shield`, `PACKAGE_USAGE_STATS`, `Foreground Service`
* Full-width `#1E88E5` **⚙️ DNS Settings** button and `#2C2C2C`
  **💻 Setup Guide** outlined button

### Screen 2 — DNS Settings
Six hostname presets (AdGuard Family / AdGuard / Cloudflare / Cloudflare Families /
Quad9 / Unfiltered) plus validated custom hostname entry, live "Current" status
with Active/Inactive badge. **📋 Copy & open Private DNS** copies the hostname to
the clipboard and opens Android's Private DNS screen — paste it once and Android
applies it everywhere (a snackbar confirms the copy).

### Screen 3 — Setup Guide
Four core steps plus an optional uninstall-protection step, every one a **normal Android screen or dialog** — no
computer, no commands, no ADB:

1. **DNS protection** — copy hostname + open the Private DNS dialog
2. **Usage access** — Android's Usage Access settings screen
3. **Arm the shield** — Android's Accessibility settings screen, toggle
   *DNS Guard Reddit Shield* ON
4. **Notifications** — the standard runtime permission dialog (Android 13+)

Each step shows its live ✅/⚠️ state, and a safety note explains exactly what the
shield does when other apps are on screen.

---

## 🏦 Accessibility safety model (the core guarantee)

```
  ┌────────┐  Reddit visible    ┌────────┐  other package /   ┌──────┐
  │ STANDBY│ ─────────────────► │ ACTIVE │  screen off /      │ DEAD │
  │        │   (30 s grace for  │        │  grace timeout     │      │
  │        │    Android's own   │        │ ─────────────────► │      │
  │        │    enable steps)   │        │   disableSelf()    │ (terminal,
  └────────┘                    └────────┘                    └──────┘
```

The shield's safety is built from **three independent, unit-tested layers**
(`core/ShieldPolicyTest.kt`, 17 scenarios):

1. **`ShieldAccessibilityService`** (event-driven, reacts in milliseconds)
   * runs a pure finite-state machine: `STANDBY (grace) → ACTIVE → DEAD`
   * **GRACE (30 s):** only `com.reddit.frontpage` may keep the service alive;
     anything else — including a package it cannot resolve — calls `disableSelf()`
   * **ACTIVE:** any non-Reddit, non-self package owning the window,
     `ACTION_SCREEN_OFF`, or losing the active window → hide overlay → `disableSelf()`
   * **DEAD** is terminal; re-arming requires the user to re-enable the service
     in Android's Accessibility settings (a one-tap standard setting)

2. **`ShieldWatchdogService`** (foreground service, 750 ms poll)
   * reconstructs the foreground package from `UsageStatsManager`
   * **observer only**: it never writes `Settings.Secure` and never touches
     accessibility flags — it just feeds the dashboard's live "Reddit is open
     right now" status and surfaces a persistent notification so the shield's
     presence is always visible
   * failure direction is safe: if `PACKAGE_USAGE_STATS` is missing the dashboard
     shows the permission as missing and the shield only relies on layer 1

3. **The user's own toggle** (one per session, standard Android)
   * the service cannot enable itself — Android only connects it when the user
     flips the switch in Settings → Accessibility, and `disableSelf()` is the
     only way it ever turns itself off

While alive, the service scans Reddit's node tree (exact-match `NSFW` chip
detection, 400-node walk cap, 250 ms debounce) and raises a full-screen
`TYPE_ACCESSIBILITY_OVERLAY` shield with a **"Reveal for 10 seconds"** button.
Touches are consumed — nothing falls through to the content underneath.

> **Deviation note (honest wording):** older revisions of this project flipped
> the global `Settings.Secure.ACCESSIBILITY_ENABLED` bit via ADB. This build
> deliberately does **not** — the guarantee it actually provides is *"the shield
> component leaves `enabled_accessibility_services` instantly"*, which is
> enforced by `disableSelf()` and observable in Android's own Accessibility
> screen.

---

## Building

### CI (zero configuration)

Every push and pull request runs
[`.github/workflows/build.yml`](.github/workflows/build.yml):

```bash
./gradlew --no-daemon lintDebug testDebugUnitTest assembleDebug
```

The repository contains the full Gradle wrapper (`gradlew`, `gradlew.bat`,
`gradle-wrapper.jar`, `gradle-wrapper.properties`), so CI needs no extra setup
beyond JDK 17. The debug APK and all reports are uploaded as artifacts.

### Locally

```bash
# JDK 17 required. Point Gradle at your SDK either through the ANDROID_HOME
# environment variable or a git-ignored local.properties file:
#     echo "sdk.dir=/path/to/Android/Sdk" > local.properties
./gradlew assembleDebug          # APK → app/build/outputs/apk/debug/
./gradlew testDebugUnitTest      # JVM unit tests (57 tests)
./gradlew lintDebug              # Android Lint
```

### Release signing

No keystores are ever committed. To produce a signed release:

```bash
keytool -genkey -v -keystore release.jks -keyalg RSA -keysize 2048 \
        -validity 10000 -alias dns_guard
# then configure signingConfigs in app/build.gradle.kts (see android.signingConfigs)
./gradlew assembleRelease
```

---

## 📲 First-run setup (once, entirely on the phone)

| # | Where | What |
| --- | --- | --- |
| 1 | 📋 **DNS Settings** → *Copy & open Private DNS* | Paste the copied hostname into Android's Private DNS dialog |
| 2 | **Setup Guide** step 2 | Toggle **Usage access** ON for DNS Guard in Android's settings |
| 3 | **Setup Guide** step 3 | Toggle **DNS Guard Reddit Shield** ON in Settings → Accessibility |
| 4 | **Setup Guide** step 4 | Allow notifications (Android 13+) so the watchdog can run |

Then open Reddit — the dashboard shows *Active now*. Close Reddit (or press the
home button) — the shield removes itself immediately; the next Reddit session
needs one more toggle in Settings → Accessibility (search "DNS Guard"), which
Android keeps reachable in one tap from the notification shade as well.

**Requirements:** Android 9.0 (API 28)+ — `disableSelf()` is the mechanism
behind the banking-app guarantee.

---

## Architecture

```
app/src/main/java/com/dnsguard/shield/
├── DnsGuardApplication.kt        # notification channel + process-wide prefs
├── MainActivity.kt               # Compose host, language provider
├── core/
│   ├── DnsManager.kt             # Private DNS status + Settings deep-link
│   ├── DnsEnforcer.kt            # IO-only writes, mode + specifier verification
│   ├── Permissions.kt            # side-effect-free permission checks
│   ├── Prefs.kt                  # language persistence
│   ├── ShieldPolicy.kt           # pure FSM: STANDBY(grace) → ACTIVE → DEAD
│   └── ShieldRuntime.kt          # live phase StateFlow for the dashboard
├── receiver/
│   └── BootCompletedReceiver.kt  # watchdog restart after reboot
├── service/
│   ├── ShieldAccessibilityService.kt  # Reddit-only NSFW overlay + settings toggle guard
│   ├── ShieldWatchdogService.kt       # Reddit foreground poll + Private DNS observer
│   └── overlay/NsfwOverlay.kt         # TYPE_ACCESSIBILITY_OVERLAY shield
├── ui/
│   ├── components/               # StatusCard, StatusDot, …
│   ├── i18n/                     # Strings interface + EN/AR implementations
│   ├── navigation/AppNav.kt      # 3-route Navigation-Compose graph
│   ├── screens/                  # Dashboard, DnsSettings, SetupGuide
│   └── theme/                    # exact palette, zero purple by construction
└── util/
    └── DnsHostname.kt            # RFC-952 validation (unit-tested)
```

Unit tests live in `app/src/test/…`: `ShieldPolicyTest` (17 safety scenarios),
`StringsTest` (i18n completeness, compile-enforced by the `Strings` interface),
`DnsHostnameTest` (hostname validation), plus `TamperGuardPolicyTest` and
`PinPolicyTest` (anti-tamper / Master PIN), `DnsEnforcerTest` and
`RedditTogglePolicyTest` — 57 tests total.

## Color palette

| Token | Hex | Use |
| --- | --- | --- |
| TopBarBlue | `#0D47A1` | TopAppBar |
| PrimaryBlue | `#1E88E5` | Buttons, selection |
| AccentLightBlue | `#64B5F6` | Hostnames, accents |
| AppBackground | `#121212` | Window background |
| CardSurface | `#1E1E1E` | Cards |
| BorderAndInputBg | `#2C2C2C` | Borders, inputs, outlined button |
| InputBorder | `#424242` | Input outlines |
| TextPrimary | `#E0E0E0` | Primary text |
| TextSecondary | `#9E9E9E` | Secondary text |
| StatusGreen | `#66BB6A` | Active/granted |
| WarningOrange | `#FFA726` | Attention |
| DangerRed | `#EF5350` | Inactive/error |
| CodeBlockBg | `#0A0A0A` | Mono/code surface token |
| CodeBlockText | `#80CBC4` | Mono/code surface token |

Every one of the 36 Material 3 `ColorScheme` slots is written out explicitly in
`ui/theme/Theme.kt` — no default (purple) value can ever leak in.

## License

Provided as-is for personal use. Use responsibly and in accordance with local
laws and the terms of service of the applications you install it alongside.

---

## Optional uninstall protection (added after the Reddit shield)

The dashboard and Setup Guide now offer an **opt-in** protection flow:

1. Set a Master PIN (4–16 letters/digits). The app stores a salted BCrypt
   cost-10 hash, not the PIN, in Android Keystore–backed
   `EncryptedSharedPreferences`. If an OEM Keystore fails, an in-app warning
   appears and the **hash only** is stored in app-private preferences.
2. Tap **Activate Device Admin** and confirm on Android's own consent screen.
   Android will then block a normal launcher/Settings uninstall until the admin
   is deactivated. The app requests **no device-admin policies** beyond the
   active-admin status itself (no wipe, lock, or password rules).
3. Optionally enable **DNS Guard Settings Guard** in Accessibility settings.
   This is **not** the Reddit shield: it is a separate accessibility service
   that stays connected for Settings; the Reddit shield still turns itself off
   immediately outside Reddit. The guard inspects Settings-family windows for
   our full app label/package name plus destructive controls in English/Arabic.
   It either presses Back or presents an accessibility-overlay PIN challenge.

The PIN gates the in-app **DNS Settings** route, PIN changes, and the in-app
**Deactivate protection** button. The admin can still be deactivated from
Android's own system Settings, and the accessibility guard can be switched
**off at any time** in Accessibility settings. A successful Settings-overlay
PIN challenge pauses the guard for **2 minutes**; the in-app deactivation
challenge pauses it for **10 minutes** so the owner can complete removal.
The guard does **not** lock down the entire Settings app or prevent a person
with access to Android Settings from changing Private DNS directly. Android
can also allow safe mode, factory reset, ADB/device-owner actions or OEM
uninstall flows that a normal app cannot control. This is **tamper friction**,
not guaranteed parental control, malware resistance, or an unremovable app.

See `core/TamperGuardPolicy.kt` and `core/security/PinPolicy.kt` for the pure
security decisions, `service/TamperGuardAccessibilityService.kt` for event
execution, and their JVM tests for the adversarial matrix. The dependency
versions are pinned in `app/build.gradle.kts`.

**Baseline repair note:** the upstream revision `31afb46` did not compile:
`core/AccessibilitySwitch.kt`, `ui/screens/AdbGuideScreen.kt`, and
`ui/components/CodeBlock.kt` referenced APIs/strings removed by an earlier
zero-ADB migration. They had no callers in the running app and were removed
in the local baseline commit before implementing this feature.


### Private DNS restoration and Reddit setting toggles

The Android components are named `DnsEnforcer`, `ShieldWatchdogService`, and
`ShieldAccessibilityService` (rather than `GuardForegroundService` and
`RedditAccessibilityService`). The watchdog registers a `ContentObserver` on
`private_dns_mode` and `private_dns_specifier`. Changes are coalesced for 100 ms;
the 750 ms poll is a fallback. A single I/O mutex serializes checks and writes
the saved hostname followed by strict `hostname` mode, then verifies both
values. Failed writes are logged, not reported as restored. This depends on an
ADB grant of `WRITE_SECURE_SETTINGS`; without it no ordinary app can restore
Private DNS automatically. Android may stop the foreground service or revoke
the grant, so restoration cannot be guaranteed.

The Reddit service watches window/content/click events only for
`com.reddit.frontpage`. When a supported adult-content label appears beside a
CHECKED switch/checkbox in a small parent row, it clicks that control OFF and
backs out; it never clicks an unchecked control. Reddit post labels without
nearby toggles still use the NSFW overlay. UI behavior varies by Reddit version
and OEM, so an on-device check is needed in addition to JVM tests.
