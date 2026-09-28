# 🛡️ DNS Guard & Shield

A production-ready Android application combining **system-wide Private DNS filtering**
with a **Reddit-only NSFW accessibility shield** — built entirely with
**Jetpack Compose (Material 3)**, dark mode only, zero purple, zero placeholders.

| Concern | Guarantee |
| --- | --- |
| DNS protection | Android Private DNS (DNS-over-TLS) pinned to a vetted hostname (default `family.adguard-dns.com`) |
| Reddit NSFW shield | Accessibility overlay covers NSFW content **only** while `com.reddit.frontpage` is in the foreground |
| Banking-app safety | `Settings.Secure.ACCESSIBILITY_ENABLED` is forced to **0 at all times**, flipping to 1 for the exact seconds Reddit is open, and back to 0 the instant it is not — enforced by *two independent layers* |
| Data egress | The app declares **no `INTERNET` permission** — nothing ever leaves the device |

---

## Features

### Screen 1 — Main Dashboard
* `#0D47A1` TopAppBar — "🛡️ DNS Guard & Shield" + `🌐 EN` language toggle (**English ↔ Arabic**, full RTL layout mirroring when Arabic is active)
* **DNS Protection** card: green/red status dot + current hostname in `#64B5F6`
* **Reddit NSFW Shield** card: `Shield Armed` → `Active now — Reddit is in the foreground`
* **Permissions Status** card: clickable `✅ / ⚠️` rows for
  `WRITE_SECURE_SETTINGS`, `PACKAGE_USAGE_STATS`, `Foreground Service`
* Full-width `#1E88E5` **⚙️ DNS Settings** button and `#2C2C2C`
  **💻 ADB Setup Guide** outlined button

### Screen 2 — DNS Settings
Presets (AdGuard Family / AdGuard / Unfiltered / Cloudflare / Cloudflare Families /
Quad9 / Automatic), validated custom hostname entry, live "Current" status with
Active/Inactive badge. Applied through Android's Private DNS keys:

```
private_dns_mode      = hostname
private_dns_specifier = family.adguard-dns.com
```

### Screen 3 — ADB Setup Guide
Three numbered steps with copyable command blocks (`#0A0A0A` background,
`#80CBC4` text):

```bash
adb shell pm grant com.dnsguard.shield android.permission.WRITE_SECURE_SETTINGS
adb shell appops set com.dnsguard.shield GET_USAGE_STATS allow

# Verify the safety guarantee — expected output: 0
adb shell settings get secure accessibility_enabled
```

---

## 🏦 Accessibility safety model (the core guarantee)

```
                      ┌────────────────────────────────────────────┐
                      │  ACCESSIBILITY_ENABLED (global switch)    │
 time ───────────────►│  default ..................... 0  (OFF)    │
                      │  Reddit moves to foreground .. 1  (ON)     │
                      │  Reddit leaves / screen off .. 0  (OFF)    │
                      └────────────────────────────────────────────┘
```

Two independent enforcement layers make the guarantee resilient:

1. **`ShieldWatchdogService`** (foreground service, 750 ms poll)
   * reconstructs the foreground package from `UsageStatsManager`
   * writes `ENABLED_ACCESSIBILITY_SERVICES += com.dnsguard.shield/.service.ShieldAccessibilityService`
     and `ACCESSIBILITY_ENABLED = 1` **only** when the poll observes
     `com.reddit.frontpage` *and* the display is on
   * forces `ACCESSIBILITY_ENABLED = 0` on every other observation
   * if `PACKAGE_USAGE_STATS` is missing it can never prove Reddit is in front,
     so the switch stays at 0 forever (failure direction is always the safe one)

2. **`ShieldAccessibilityService`** (event-driven, reacts in milliseconds)
   * any accessibility event whose package is **not** `com.reddit.frontpage`
     triggers: hide overlay → `ACCESSIBILITY_ENABLED = 0` → `disableSelf()`
   * `ACTION_SCREEN_OFF` does the same
   * `onDestroy` re-asserts 0 as a last line of defence

While alive, the service scans Reddit's node tree (exact-match `NSFW` chip
detection, 400-node walk cap, 250 ms debounce) and raises a full-screen
`TYPE_ACCESSIBILITY_OVERLAY` shield with a **"Reveal for 10 seconds"** button.
Touches are consumed — nothing falls through to the content underneath.

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
./gradlew testDebugUnitTest      # JVM unit tests
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

## 📲 First-run setup (once, over ADB)

| # | Command | Why |
| --- | --- | --- |
| 1 | `adb shell pm grant com.dnsguard.shield android.permission.WRITE_SECURE_SETTINGS` | Apply Private DNS + flip the accessibility master switch |
| 2 | `adb shell appops set com.dnsguard.shield GET_USAGE_STATS allow` | Detect the foreground package (Reddit vs everything else) |

Then grant **Usage Access** manually if you prefer UI, start the app, press the
**Foreground Service** row, and open Reddit — the dashboard shows
*Active now* and `adb shell settings get secure accessibility_enabled` reads `1`
only while Reddit is open, `0` at every other moment.

---

## Architecture

```
app/src/main/java/com/dnsguard/shield/
├── DnsGuardApplication.kt        # notification channel + process-wide prefs
├── MainActivity.kt               # Compose host, language provider
├── core/
│   ├── AccessibilitySwitch.kt    # THE single authority for the 0/1 flip
│   ├── DnsManager.kt             # private_dns_mode / private_dns_specifier
│   ├── Permissions.kt            # side-effect-free permission checks
│   ├── Prefs.kt                  # language persistence
│   └── ShieldRuntime.kt          # live phase StateFlow for the dashboard
├── receiver/
│   └── BootCompletedReceiver.kt  # watchdog restart after reboot
├── service/
│   ├── ShieldAccessibilityService.kt  # Reddit-only, self-disabling
│   ├── ShieldWatchdogService.kt       # 750 ms foreground poll + FGS
│   └── overlay/NsfwOverlay.kt         # TYPE_ACCESSIBILITY_OVERLAY shield
├── ui/
│   ├── components/               # StatusCard, StatusDot, CodeBlock, …
│   ├── i18n/                     # Strings interface + EN/AR implementations
│   ├── navigation/AppNav.kt      # 3-route Navigation-Compose graph
│   ├── screens/                  # Dashboard, DnsSettings, AdbGuide
│   └── theme/                    # exact palette, zero purple by construction
└── util/
    └── DnsHostname.kt            # RFC-952 validation (unit-tested)
```

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
| CodeBlockBg | `#0A0A0A` | Code blocks |
| CodeBlockText | `#80CBC4` | Code text |

Every one of the 36 Material 3 `ColorScheme` slots is written out explicitly in
`ui/theme/Theme.kt` — no default (purple) value can ever leak in.

## License

Provided as-is for personal use. Use responsibly and in accordance with local
laws and the terms of service of the applications you install it alongside.
