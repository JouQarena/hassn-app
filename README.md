<div dir="rtl">

# حصّن 🛡️
![CI](https://github.com/JouQarena/hassn-app/actions/workflows/ci.yml/badge.svg)


**حوِّل نفسك بعيداً عن التشتت — تلقائياً**

تطبيق أندرويد يراقب استخدام تطبيقاتك عبر خدمة الوصول (AccessibilityService). عند فتح تطبيق مُشِتِّت، يرد حصّن تلقائياً حسب إعداداتك: يعرض رسالة تحفيزية، يطلب منك إتمام تحدٍّ، أو يحوّلك مباشرة إلى تطبيق مفيد — أو أيّ تركيبة من ذلك بالترتيب الذي تحدده.

</div>

# Hassn 🛡️

**Redirect yourself away from distraction — automatically**

An Android app that monitors your app usage through an AccessibilityService. When you open a distracting app, Hassn responds automatically based on your setup: it shows a motivational message, asks you to complete a challenge, redirects you to a productive app — or any combination of those in the order you choose.

## ✨ Features / المميزات

- 📢 **نظام استجابة مرن** — اختار أي تركيبة من (رسالة تحفيزية، تحدٍّ، تحويل) وحدد ترتيب التنفيذ.
  **Flexible response system** — combine (motivational message, challenge, redirect) in any custom order.
- 📝 **رسالة تحفيزية مخصصة** — نص، صورة، ألوان، شفافية، حجم خط، مدة عرض.
  **Custom message** — text, image, colors, opacity, font size, duration.
- 🎯 **9 أنواع تحديات** بثلاث مستويات صعوبة (Morse Code معطّل افتراضياً).
  **9 challenge types** across three difficulties (Morse Code disabled by default).
- 🔄 **تحويل تلقائي** لتطبيق مفيد تختاره.
  **Automatic redirect** to a productive app you choose.
- 🔍 **كشف محسّن للوضع الخاص** — Reddit (الوضع المجهول)، Brave (التبويب الخاص)، Chrome (الوضع الخفي) بنظام نقاط متعدد الإشارات، وكلمات مفتاحية مخصصة لأي تطبيق آخر.
  **Enhanced private-mode detection** — Reddit anonymous, Brave private tab, Chrome incognito via a multi-signal scoring engine, plus custom keywords for any other app.
- 📱 **مراقبة تطبيقات مخصصة** — أضف أي تطبيق وضعّي (دائماً / كشف الوضع الخاص فقط).
  **Custom app monitoring** — monitor any app, always or private-mode-only.
- 🔒 **تحديات منع الإيقاف** — إيقاف الحماية يتطلب اجتياز تحدٍّ متصاعد (سهل ← متوسط ← صعب)، وقفل مؤقت بعد 7 محاولات فاشلة.
  **Disable-protection challenges** — turning protection off requires passing an escalating challenge, with a temporary lock after 7 failed attempts.
- 📊 **مراجعة قبل الحذف** — شاشة تُظهر رحلتك (أيام الاستخدام، التحويلات، التحديات، الوقت الموفَّر) قبل التوجه لحذف التطبيق.
  **Uninstall review** — a screen showing your journey (days used, redirections, challenges, time saved) before you head to uninstall.
- 🌐 **عربي أولاً** — واجهة RTL كاملة مع تبديل سريع إلى الإنجليزية.
  **Arabic-first** — full RTL UI with a quick English toggle.
- 📴 **بلا إنترنت** — كل البيانات محلية (DataStore)، بدون أي أذونات شبكة أو تحليلات.
  **Fully offline** — all data local (DataStore), no network permissions, no analytics.

## 📸 Screenshots / لقطات شاشة

| | |
|---|---|
| ![Main](screenshots/main.png) | ![Challenges](screenshots/challenges.png) |

*(أضف لقطات الشاشة هنا / Add screenshots here)*

## 📦 Installation / التثبيت

1. ثبّت التطبيق (v2.0.0، أندرويد 7.0+).
   Install the app (v2.0.0, Android 7.0+).
2. من الشاشة الرئيسية، فعّل **خدمة الوصول**:
   From the main screen, enable the **accessibility service**:
   *إعدادات ← الوصولية ← حصّن* → `Settings → Accessibility → Hassn`
3. امنح إذن **العرض فوق التطبيقات** (لعرض الرسائل والتحديات).
   Grant the **Draw over other apps** permission (to show messages and challenges).
4. أضف التطبيقات المشتتة واختر سلوك الاستجابة، ثم شغّل **الحماية**.
   Add your distracting apps, choose the response behavior, then turn **Protection** on.

## 🔐 Permissions / الأذونات

| الإذن / Permission | السبب / Why |
|---|---|
| الوصولية / Accessibility | مراقبة فتح التطبيقات وكشف الوضع الخاص عبر شجرة العنصرات. Monitor app launches and detect private mode via the view tree. |
| العرض فوق التطبيقات / SYSTEM_ALERT_WINDOW | عرض الرسالة والتحدٍّ، والبقاء النشط لفتح التطبيق المفيد (متطلب أندرويد 10+). Show message/challenge overlays and allow launching the destination app (Android 10+ requirement). |
| استكمال تشغيل الجهاز / RECEIVE_BOOT_COMPLETED | (مهيّأ لاستئناف المراقبة بعد إعادة التشغيل). Reserved for restarting monitoring after reboot. |

**لا توجد أي أذونات شبكة. التطبيق لا يرسل أي بيانات للخارج.**
**No network permissions at all. The app never sends data outside the device.**

## 🔨 Build / البناء

```bash
# يتطلب: JDK 17 + Gradle 8.0+
# Requires: JDK 17 + Gradle 8.0+
gradle wrapper          # إذا لم يكن الـ wrapper موجوداً / if the wrapper is missing
./gradlew assembleDebug
```

- AGP 8.1.0 · Kotlin 1.9.0 · Compose Compiler 1.5.3 · minSdk 24 · targetSdk 34

## 🏗️ Architecture / البنية

- **MVVM** — Jetpack Compose (Material 3) + ViewModels (Kotlin Coroutines & Flow).
- **DataStore Preferences** للتخزين (بدون قاعدة بيانات) مع kotlinx.serialization لنماذج الإعدادات.
  **DataStore Preferences** for persistence (no database) with kotlinx-serialized settings models.
- **Dependency injection يدوي** عبر `HassnApp` (Application) — بدون مكتبات DI.
  **Manual DI** through `HassnApp` — no DI frameworks.
- **الدفع الأساسي / Core flow:**
  `HassnAccessibilityService.onAccessibilityEvent` → debounce (2s) → mode check
  (ALWAYS / PRIVATE_ONLY → `PrivacyDetectionEngine`) → `ResponseExecutor`
  (message overlay → challenge overlay → GLOBAL_ACTION_HOME + destination launch).
- **كشف الوضع الخاص / Privacy detection:** `PrivacyDetector` interface مع أوزان إشارات
  (Keyword 40 / ResourceId 30 / Icon 25 / Button 20) وعتبات لكل متصفح، مع كاش 2 ثانية.
  `PrivacyDetector` interface with weighted signals and per-browser thresholds, 2s result cache.
- كل عمليات `AccessibilityNodeInfo` داخل try-catch مع `recycle()` في finally.
  All node operations are try-catch wrapped with `recycle()` in finally blocks.

```
app/src/main/java/com/hassn/app/
├── HassnApp.kt / MainActivity.kt
├── data/        # Models + DataStore repositories
├── detection/   # Privacy detectors + scoring engine
├── service/     # AccessibilityService + ResponseExecutor
├── ui/          # screens/ components/ challenges/ theme/ navigation
├── viewmodel/   # Main / MessageSettings / Challenge ViewModels
└── util/        # Constants + node traversal extensions
```

## 🧪 Tests / الاختبارات

- `gradle test` — اختبارات وحدة: الكاشفات الأربعة، منطق ResponseExecutor، المستودعات (DataStore حقيقي على ملف مؤقت)، وViewModels.
  Unit tests: detectors, ResponseExecutor logic, repositories (real DataStore on a temp file), ViewModels.
- `gradle connectedAndroidTest` — اختبار إطلاق (smoke test).

> **ملاحظة / Note:** حماية الحذف عبر Device Admin (feature 5 في المواصفات) **لم تُفعّل** عمداً لتجنّب احتكاك سياسة Google Play —
> اعتمدنا النسخة الخفيفة (شاشة مراجعة + إحصائيات) فقط، دون أي صلاحية مسؤول.
> The Device Admin uninstall block was deliberately **not** enabled (Google Play policy friction) — we kept the lightweight version (review screen + stats) only, with no admin privileges.

## 📄 License / الرخصة

[MIT](LICENSE) © JouQarena

## 🔗 Repository / المستودع

**https://github.com/JouQarena/hassn-app**
