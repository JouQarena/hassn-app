package com.dnsguard.shield.ui.i18n

/**
 * Every user-visible string in the application.
 *
 * Both translations (English and Arabic) live in the same interface, so the
 * compiler guarantees a translation can never be forgotten: adding a property
 * here forces an implementation in [EnglishStrings] *and* [ArabicStrings] or
 * the build fails.
 */
interface Strings {
    val language: AppLanguage

    // ── Top bar ────────────────────────────────────────────────────────────
    val appTitle: String
    val switchLanguage: String

    // ── Dashboard ──────────────────────────────────────────────────────────
    val dnsProtection: String
    val activeAndProtected: String
    val dnsInactiveTitle: String
    val dnsAutomaticHostname: String
    val redditShield: String
    val shieldArmed: String
    val shieldActivatesSubtitle: String
    val shieldActiveNow: String
    val shieldIncomplete: String
    val shieldDisabled: String
    val shieldRedditOpenNow: String
    val permissionsStatus: String
    val permShieldService: String
    val permUsageStats: String
    val permForegroundService: String
    val permGranted: String
    val permTapToFix: String
    val dnsSettingsButton: String
    val setupGuideButton: String
    val watchdogStarted: String
    val notificationsOptional: String
    val openUsageAccess: String

    // ── DNS Settings ───────────────────────────────────────────────────────
    val dnsSettingsTitle: String
    val back: String
    val currentLabel: String
    val choosePreset: String
    val presetAdguardFamilyName: String
    val presetAdguardFamilyDesc: String
    val presetAdguardName: String
    val presetAdguardDesc: String
    val presetAdguardUnfilteredName: String
    val presetAdguardUnfilteredDesc: String
    val presetCloudflareName: String
    val presetCloudflareDesc: String
    val presetCloudflareFamilyName: String
    val presetCloudflareFamilyDesc: String
    val presetQuad9Name: String
    val presetQuad9Desc: String
    val customHostnameTitle: String
    val customHostnamePlaceholder: String
    val dnsCopyOpenBtn: String
    val dnsAppliedMessage: String
    val errorInvalidHostname: String
    val dnsActiveBadge: String
    val dnsInactiveBadge: String
    val dnsNote: String

    // ── Setup Guide (no commands — Android's own screens only) ─────────────
    val setupGuideTitle: String
    val setupIntro: String
    val step1Title: String
    val step1Body: String
    val step2Title: String
    val step2Body: String
    val step3Title: String
    val step3Body: String
    val step4Title: String
    val step4Body: String
    val openUsageBtn: String
    val openA11yBtn: String
    val armHint: String
    val allowNotifBtn: String
    val safetyTitle: String
    val safetyBody: String

    // ── Accessibility overlay (inside Reddit) ──────────────────────────────
    val overlayTitle: String
    val overlayBody: String
    val overlayRevealButton: String
    val overlayFooter: String

    // ── Foreground service notification ────────────────────────────────────
    val notifTitle: String
    val notifText: String
    val notifChannelName: String
    val notifChannelDesc: String
}

/** English (default) strings. */
object EnglishStrings : Strings {
    override val language = AppLanguage.EN
    override val appTitle = "DNS Guard & Shield"
    override val switchLanguage = "Switch language"

    override val dnsProtection = "DNS Protection"
    override val activeAndProtected = "Active & Protected"
    override val dnsInactiveTitle = "Inactive — Automatic DNS"
    override val dnsAutomaticHostname = "Automatic — not enforced"
    override val redditShield = "Reddit NSFW Shield"
    override val shieldArmed = "Shield Armed"
    override val shieldActivatesSubtitle = "Activates only when Reddit is opened"
    override val shieldActiveNow = "Active now — Reddit is in the foreground"
    override val shieldIncomplete = "Observer offline — foreground service stopped"
    override val shieldDisabled = "Shield disabled — arm it before opening Reddit"
    override val shieldRedditOpenNow = "Reddit is open right now — arm the shield"
    override val permissionsStatus = "Permissions Status"
    override val permShieldService = "Accessibility Shield"
    override val permUsageStats = "PACKAGE_USAGE_STATS"
    override val permForegroundService = "Foreground Service"
    override val permGranted = "Granted"
    override val permTapToFix = "Tap to fix"
    override val dnsSettingsButton = "⚙️ DNS Settings"
    override val setupGuideButton = "💻 Setup Guide"
    override val watchdogStarted = "Watchdog service started"
    override val notificationsOptional = "Notifications declined — the shield still works"
    override val openUsageAccess = "Opening Usage Access settings…"

    override val dnsSettingsTitle = "DNS Settings"
    override val back = "Back"
    override val currentLabel = "Current"
    override val choosePreset = "Choose a preset"
    override val presetAdguardFamilyName = "AdGuard Family DNS"
    override val presetAdguardFamilyDesc = "Blocks adult sites, gambling, drugs and other unsafe content."
    override val presetAdguardName = "AdGuard DNS"
    override val presetAdguardDesc = "General ad and tracker blocking."
    override val presetAdguardUnfilteredName = "AdGuard Unfiltered"
    override val presetAdguardUnfilteredDesc = "No filtering — ads and adult content allowed."
    override val presetCloudflareName = "Cloudflare"
    override val presetCloudflareDesc = "Fast, privacy-first DNS with no filtering."
    override val presetCloudflareFamilyName = "Cloudflare for Families"
    override val presetCloudflareFamilyDesc = "Blocks malware and adult content."
    override val presetQuad9Name = "Quad9"
    override val presetQuad9Desc = "Blocks malicious domains using threat intelligence."
    override val customHostnameTitle = "Custom hostname"
    override val customHostnamePlaceholder = "e.g. family.adguard-dns.com"
    override val dnsCopyOpenBtn = "📋 Copy & open Private DNS"
    override val dnsAppliedMessage = "Copied %1\$s — paste it in the Private DNS dialog"
    override val errorInvalidHostname = "Enter a valid DNS hostname (letters, digits, hyphens and dots)."
    override val dnsActiveBadge = "Active"
    override val dnsInactiveBadge = "Inactive"
    override val dnsNote = "Set once in Android's Private DNS screen (DNS-over-TLS); this dashboard reads the live value."

    override val setupGuideTitle = "Setup Guide"
    override val setupIntro = "Four one-time steps, all through Android's own screens — no computer, no commands."
    override val step1Title = "Step 1 — DNS protection"
    override val step1Body = "Copy a hostname and paste it into Android's Private DNS dialog. Android remembers it forever."
    override val step2Title = "Step 2 — Usage access"
    override val step2Body = "Lets the watchdog see which app is in front, so the dashboard can tell you when Reddit is open."
    override val step3Title = "Step 3 — Arm the shield"
    override val step3Body = "Open Accessibility and switch ON «DNS Guard Reddit Shield». The service removes itself the moment any other app appears — it only ever runs while Reddit is in front."
    override val step4Title = "Step 4 — Notifications"
    override val step4Body = "Optional: allow notifications so the watchdog can show the shield's live state."
    override val openUsageBtn = "Open Usage Access settings"
    override val openA11yBtn = "Open Accessibility settings"
    override val armHint = "Find «DNS Guard Reddit Shield» in the list and toggle it ON."
    override val allowNotifBtn = "Allow notifications"
    override val safetyTitle = "Safety model"
    override val safetyBody = "Every permission is granted through Android's own screens — never a computer or a shell command. The shield runs only while Reddit is in the foreground and removes itself from the enabled accessibility services the instant any other app appears. DNS is applied by Android itself, and the app declares no INTERNET permission — nothing leaves the device."

    override val overlayTitle = "🛡️ NSFW content shielded"
    override val overlayBody = "DNS Guard hid this content while Reddit is in the foreground."
    override val overlayRevealButton = "Reveal for 10 seconds"
    override val overlayFooter = "Accessibility shield runs only inside Reddit."

    override val notifTitle = "DNS Guard is watching"
    override val notifText = "Shield runs only inside Reddit and shuts off the moment you leave."
    override val notifChannelName = "Shield watchdog"
    override val notifChannelDesc = "Shows the live shield state; the accessibility service only runs while Reddit is in the foreground."
}

/** Arabic translations (right-to-left). */
object ArabicStrings : Strings {
    override val language = AppLanguage.AR
    override val appTitle = "DNS Guard & Shield"
    override val switchLanguage = "تغيير اللغة"

    override val dnsProtection = "حماية DNS"
    override val activeAndProtected = "نشطة ومحمية"
    override val dnsInactiveTitle = "غير نشطة — DNS تلقائي"
    override val dnsAutomaticHostname = "تلقائي — غير مفروض"
    override val redditShield = "درع Reddit من NSFW"
    override val shieldArmed = "الدرع مُسلَّح"
    override val shieldActivatesSubtitle = "يتفعل فقط عند فتح Reddit"
    override val shieldActiveNow = "نشط الآن — Reddit في المقدمة"
    override val shieldIncomplete = "المراقب متوقف — الخدمة الأمامية مغلقة"
    override val shieldDisabled = "الدرع معطّل — فعّله قبل فتح Reddit"
    override val shieldRedditOpenNow = "Reddit مفتوح الآن — فعّل الدرع"
    override val permissionsStatus = "حالة الأذونات"
    override val permShieldService = "درع الوصولية"
    override val permUsageStats = "PACKAGE_USAGE_STATS"
    override val permForegroundService = "خدمة أمامية"
    override val permGranted = "ممنوحة"
    override val permTapToFix = "اضغط للإصلاح"
    override val dnsSettingsButton = "⚙️ إعدادات DNS"
    override val setupGuideButton = "💻 دليل الإعداد"
    override val watchdogStarted = "تم تشغيل خدمة المراقبة"
    override val notificationsOptional = "تم رفض الإشعارات — الدرع يعمل رغم ذلك"
    override val openUsageAccess = "جارٍ فتح إعدادات الوصول إلى بيانات الاستخدام…"

    override val dnsSettingsTitle = "إعدادات DNS"
    override val back = "رجوع"
    override val currentLabel = "الحالي"
    override val choosePreset = "اختر إعدادًا مسبقًا"
    override val presetAdguardFamilyName = "AdGuard لحماية العائلة"
    override val presetAdguardFamilyDesc = "يحجب مواقع البالغين والقمار والمخدرات وغيرها من المحتوى غير الآمن."
    override val presetAdguardName = "AdGuard DNS"
    override val presetAdguardDesc = "حجب عام للإعلانات وبرامج التتبع."
    override val presetAdguardUnfilteredName = "AdGuard بدون تصفية"
    override val presetAdguardUnfilteredDesc = "بدون تصفية — يُسمح بالإعلانات والمحتوى للبالغين."
    override val presetCloudflareName = "Cloudflare"
    override val presetCloudflareDesc = "DNS سريع يحترم الخصوصية بدون تصفية."
    override val presetCloudflareFamilyName = "Cloudflare للعائلات"
    override val presetCloudflareFamilyDesc = "يحجب البرمجيات الخبيثة والمحتوى للبالغين."
    override val presetQuad9Name = "Quad9"
    override val presetQuad9Desc = "يحجب النطاقات الخبيثة باستخدام استخبارات التهديدات."
    override val customHostnameTitle = "اسم مضيف مخصص"
    override val customHostnamePlaceholder = "مثال: family.adguard-dns.com"
    override val dnsCopyOpenBtn = "📋 نسخ وفتح DNS الخاص"
    override val dnsAppliedMessage = "تم نسخ %1\$s — الصقه في نافذة DNS الخاص"
    override val errorInvalidHostname = "أدخل اسم مضيف DNS صالحًا (أحرف وأرقام وشرطات ونقاط)."
    override val dnsActiveBadge = "نشط"
    override val dnsInactiveBadge = "غير نشط"
    override val dnsNote = "يُضبط مرة واحدة في شاشة DNS الخاص في Android (DNS-over-TLS)؛ تعرض لوحة المعلومات القيمة الحية."

    override val setupGuideTitle = "دليل الإعداد"
    override val setupIntro = "أربع خطوات مرة واحدة، كلها عبر شاشات أندرويد نفسها — بلا حاسوب وبلا أوامر."
    override val step1Title = "الخطوة 1 — حماية DNS"
    override val step1Body = "انسخ اسم مضيف والصقه في نافذة DNS الخاص في Android. يتذكره النظام للأبد."
    override val step2Title = "الخطوة 2 — الوصول لبيانات الاستخدام"
    override val step2Body = "يتيح للمراقب معرفة التطبيق في المقدمة، ليخبرك لوحة المعلومات عندما يُفتح Reddit."
    override val step3Title = "الخطوة 3 — تسليح الدرع"
    override val step3Body = "افتح الوصولية وفعّل «درع Reddit الخاص بـ DNS Guard». تُلغي الخدمة نفسها لحظة ظهور أي تطبيق آخر — لا تعمل إلا أثناء تواجد Reddit في المقدمة."
    override val step4Title = "الخطوة 4 — الإشعارات"
    override val step4Body = "اختياري: اسمح بالإشعارات ليعرض المراقب حالة الدرع الحية."
    override val openUsageBtn = "فتح إعدادات الوصول للاستخدام"
    override val openA11yBtn = "فتح إعدادات الوصولية"
    override val armHint = "ابحث عن «درع Reddit الخاص بـ DNS Guard» في القائمة وفعّله."
    override val allowNotifBtn = "السماح بالإشعارات"
    override val safetyTitle = "نموذج الأمان"
    override val safetyBody = "كل الأذونات تُمنح عبر شاشات أندرويد نفسها — لا حاسوب ولا أمر سطر أوامر أبدًا. يعمل الدرع فقط أثناء تواجد Reddit في المقدمة ويُلغي نفسه من خدمات الوصولية المفعّلة لحظة ظهور أي تطبيق آخر. يطبّق Android الـDNS بنفسه، والتطبيق لا يصرّح بإذن INTERNET — لا يغادر أي شيء الجهاز."

    override val overlayTitle = "🛡️ تم حجب محتوى NSFW"
    override val overlayBody = "حجب DNS Guard هذا المحتوى لأن Reddit في المقدمة."
    override val overlayRevealButton = "إظهار لمدة 10 ثوانٍ"
    override val overlayFooter = "درع الوصلية يعمل فقط داخل Reddit."

    override val notifTitle = "DNS Guard في وضع المراقبة"
    override val notifText = "الدرع يعمل داخل Reddit فقط ويُطفأ لحظة خروجك منه."
    override val notifChannelName = "مراقب الدرع"
    override val notifChannelDesc = "يعرض حالة الدرع الحية؛ خدمة الوصولية تعمل فقط أثناء تواجد Reddit في المقدمة."
}

/** Resolves the string bundle for a language. */
fun stringsFor(language: AppLanguage): Strings = when (language) {
    AppLanguage.EN -> EnglishStrings
    AppLanguage.AR -> ArabicStrings
}
