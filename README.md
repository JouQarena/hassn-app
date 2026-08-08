# حصّن — Hassn

**حوّل نفسك بعيداً عن التشتت — تلقائياً.**

حصّن هو تطبيق أندرويد يراقب التطبيقات التي تفتحها عبر خدمة إمكانية الوصول، وعند فتح أي تطبيقات اخترتها (تيك توك، إنستغرام، ريديت، كروم الخفي...) يقوم بتحويلك فوراً إلى تطبيق مفيد من اختيارك (ملاحظات، كيندل، دولينجو، قرآن، تسبيح...).

> **جديد في حصّن:** واجهة عربية كاملة مع دعم الإنجليزية، وإمكانية اختيار **أي تطبيقات** تريد الحماية منها مع اختيار وضع لكل تطبيق (دائماً أو فقط في الوضع الخاص/الخفي).

---

## ✨ المميزات

- 🇸🇦 **واجهة عربية 100%** مع دعم RTL + زر تبديل للإنجليزية
- 🎯 **اختيار تطبيقات مخصصة** — اختر أي تطبيقات مثبتة للمراقبة (متعدد الاختيار)
- ⚙️ **وضع لكل تطبيق:**
  - `دائماً` — تحويل فور فتح التطبيق
  - `خاص فقط` — تحويل فقط عند كشف كلمات مثل "خفي / خاص / incognito / private"
- 🔀 **تطبيق وجهة مخصص** — اختر أي تطبيق يتم نقلك إليه
- 🛡️ **حماية بخدمة إمكانية الوصول** — بدون صلاحية إنترنت، كل المعالجة على جهازك
- 🔒 **تحدي منع الإيقاف** — 9 تحديات مترجمة (ضغط، ثبات، حبس نفس، ماء، كتابة، حساب، ألوان، مورس...) مع تصعيد وقفل مؤقت
- 🎨 **Material 3 + Dynamic Color** (أندرويد 12+)

---

## 📸 كيف يعمل

```
تفتح تطبيق مراقب (مثلاً تيك توك / ريديت المجهول / كروم الخفي)
        ↓
خدمة إمكانية الوصول تكشف التطبيق + (إن لزم) كلمات الوضع الخاص
        ↓
الخدمة تضغط زر الرئيسية + تشغّل تطبيق الوجهة بعد 350ms
        ↓
تجد نفسك في تطبيق مفيد بدلاً من التشتت
```

**كشف الوضع الخاص:** فحص شجرة إمكانية الوصول عن كلمات: `incognito, private, anonymous, خفي, خاص, مجهول` — بدون لقطات شاشة.

---

## 📋 المتطلبات

| المتطلب | التفاصيل |
|---|---|
| أندرويد | 7.0 (API 24) فما فوق |
| المعمارية | arm64, armeabi, x86, x86_64 |
| الصلاحيات | إمكانية الوصول + بدء التشغيل |

---

## 📥 التثبيت

1. حمّل APK من تبويب **Actions → Build APKs → Artifacts → Hassn-APKs**
2. ثبّت `app-debug.apk` (قد يظهر تحذير Play Protect → "المزيد" → "التثبيت على أي حال")
3. افتح **الإعدادات → إمكانية الوصول → حصّن** وفعّل الخدمة
4. افتح التطبيق → اختر **تطبيق الوجهة** → اختر **التطبيقات المراقبة** مع تحديد الوضع لكل منها → فعّل **الحماية**

---

## 🔧 البناء من المصدر

```bash
git clone https://github.com/YOUR_USERNAME/hassn.git
cd hassn
./gradlew assembleDebug
# APK في: app/build/outputs/apk/debug/app-debug.apk
```

**Release:**
```bash
keytool -genkey -v -keystore release.keystore -alias hassn -keyalg RSA -keysize 2048 -validity 10000 -storepass hassn123 -keypass hassn123 -dname "CN=Hassn"
./gradlew assembleRelease -Prelease.storeFile=release.keystore -Prelease.storePassword=hassn123 -Prelease.keyAlias=hassn -Prelease.keyPassword=hassn123
```

---

## 🏗️ هيكل المشروع

```
Hassn/
├── app/src/main/java/com/hassn/app/
│   ├── HassnApp.kt
│   ├── MainActivity.kt
│   ├── data/ (AppInfo, MonitoredApp, SettingsDataStore)
│   ├── service/HassnAccessibilityService.kt
│   ├── util/ (Constants, LocaleHelper)
│   ├── viewmodel/MainViewModel.kt
│   └── ui/
│       ├── screens/MainScreen.kt (عربي)
│       └── challenge/ (9 تحديات مترجمة)
└── res/values/ + values-ar/ + values-en/
```

---

## 🔒 الخصوصية

كل شيء **على جهازك**. لا إنترنت، لا جمع بيانات، لا تتبع. خدمة إمكانية الوصول تفحص فقط بنية الواجهة (نصوص/معرفات) لكشف الوضع الخاص.

---

## 📄 الترخيص

MIT

---

## 🌐 English Summary

**Hassn** redirects you from distracting apps to a productive destination. Arabic-first with English toggle, pick any apps to monitor with per-app mode (Always / Private-only), on-device only, with gamified disable challenges.
