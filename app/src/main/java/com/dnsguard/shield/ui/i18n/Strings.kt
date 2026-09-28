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
    val permissionsStatus: String
    val permSecureSettings: String
    val permUsageStats: String
    val permForegroundService: String
    val permGranted: String
    val permTapToFix: String
    val permNeedsAdb: String
    val dnsSettingsButton: String
    val adbGuideButton: String
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
    val presetAutomaticName: String
    val presetAutomaticDesc: String
    val customHostnameTitle: String
    val customHostnamePlaceholder: String
    val applyButton: String
    val dnsAppliedMessage: String
    val dnsAutomaticAppliedMessage: String
    val errorInvalidHostname: String
    val errorNoSecureSettings: String
    val openAdbGuide: String
    val dnsActiveBadge: String
    val dnsInactiveBadge: String
    val dnsNote: String
    val copyCommand: String
    val copiedCommand: String

    // ── ADB Setup Guide ────────────────────────────────────────────────────
    val adbGuideTitle: String
    val adbIntro: String
    val step1Title: String
    val step1Body: String
    val step2Title: String
    val step2Body: String
    val step3Title: String
    val step3Body: String
    val commandLabel: String
    val verifyTitle: String
    val verifyBody: String
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
    override val shieldIncomplete = "Setup incomplete — grant permissions below"
    override val permissionsStatus = "Permissions Status"
    override val permSecureSettings = "WRITE_SECURE_SETTINGS"
    override val permUsageStats = "PACKAGE_USAGE_STATS"
    override val permForegroundService = "Foreground Service"
    override val permGranted = "Granted"
    override val permTapToFix = "Tap to fix"
    override val permNeedsAdb = "Needs ADB"
    override val dnsSettingsButton = "⚙️ DNS Settings"
    override val adbGuideButton = "💻 ADB Setup Guide"
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
    override val presetAutomaticName = "Automatic (system default)"
    override val presetAutomaticDesc = "Let Android and your network decide."
    override val customHostnameTitle = "Custom hostname"
    override val customHostnamePlaceholder = "e.g. family.adguard-dns.com"
    override val applyButton = "Apply"
    override val dnsAppliedMessage = "Private DNS updated to %1\$s"
    override val dnsAutomaticAppliedMessage = "Private DNS set to Automatic"
    override val errorInvalidHostname = "Enter a valid DNS hostname (letters, digits, hyphens and dots)."
    override val errorNoSecureSettings = "WRITE_SECURE_SETTINGS is not granted. Run the ADB command from the guide."
    override val openAdbGuide = "Open ADB guide"
    override val dnsActiveBadge = "Active"
    override val dnsInactiveBadge = "Inactive"
    override val dnsNote = "Applied system-wide through Android Private DNS (DNS-over-TLS)."
    override val copyCommand = "Copy command"
    override val copiedCommand = "Copied ✓"

    override val adbGuideTitle = "ADB Setup Guide"
    override val adbIntro = "Android only grants these powerful permissions over USB debugging. Follow the three steps once — the app runs fully self-sufficient afterwards."
    override val step1Title = "Step 1 — Enable developer options"
    override val step1Body = "Open Settings → About phone → tap \"Build number\" seven times, then go back."
    override val step2Title = "Step 2 — Enable USB debugging"
    override val step2Body = "Settings → System → Developer options → turn on USB debugging, then connect the phone to a computer."
    override val step3Title = "Step 3 — Grant the permissions"
    override val step3Body = "Run these two commands in a terminal:"
    override val commandLabel = "Command"
    override val verifyTitle = "Verify the safety guarantee"
    override val verifyBody = "The accessibility master switch must read 0 while Reddit is closed. The app writes 1 only during the exact seconds when com.reddit.frontpage is in the foreground, and forces 0 again immediately after."
    override val safetyTitle = "Safety model"
    override val safetyBody = "WRITE_SECURE_SETTINGS only sets Android's Private DNS hostname and forces accessibility_enabled back to 0 when Reddit leaves the foreground. PACKAGE_USAGE_STATS only detects the foreground package. The app declares no INTERNET permission — nothing ever leaves the device."

    override val overlayTitle = "🛡️ NSFW content shielded"
    override val overlayBody = "DNS Guard hid this content while Reddit is in the foreground."
    override val overlayRevealButton = "Reveal for 10 seconds"
    override val overlayFooter = "Accessibility shield runs only inside Reddit."

    override val notifTitle = "DNS Guard is watching"
    override val notifText = "Accessibility is disabled. It activates only while Reddit is open."
    override val notifChannelName = "Shield watchdog"
    override val notifChannelDesc = "Keeps the Reddit NSFW shield armed and guarantees accessibility stays disabled everywhere else."
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
    override val shieldIncomplete = "الإعداد غير مكتمل — امنح الأذونات أدناه"
    override val permissionsStatus = "حالة الأذونات"
    override val permSecureSettings = "WRITE_SECURE_SETTINGS"
    override val permUsageStats = "PACKAGE_USAGE_STATS"
    override val permForegroundService = "خدمة أمامية"
    override val permGranted = "ممنوحة"
    override val permTapToFix = "اضغط للإصلاح"
    override val permNeedsAdb = "تتطلب ADB"
    override val dnsSettingsButton = "⚙️ إعدادات DNS"
    override val adbGuideButton = "💻 دليل إعداد ADB"
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
    override val presetAutomaticName = "تلقائي (إعدادات النظام)"
    override val presetAutomaticDesc = "دع Android والشبكة يقرران."
    override val customHostnameTitle = "اسم مضيف مخصص"
    override val customHostnamePlaceholder = "مثال: family.adguard-dns.com"
    override val applyButton = "تطبيق"
    override val dnsAppliedMessage = "تم تحديث DNS الخاص إلى %1\$s"
    override val dnsAutomaticAppliedMessage = "تم ضبط DNS الخاص على تلقائي"
    override val errorInvalidHostname = "أدخل اسم مضيف DNS صالحًا (أحرف وأرقام وشرطات ونقاط)."
    override val errorNoSecureSettings = "لم يُمنح WRITE_SECURE_SETTINGS. نفّذ أمر ADB من الدليل."
    override val openAdbGuide = "فتح دليل ADB"
    override val dnsActiveBadge = "نشط"
    override val dnsInactiveBadge = "غير نشط"
    override val dnsNote = "يُطبَّق على النظام كله عبر DNS الخاص في Android (DNS-over-TLS)."
    override val copyCommand = "نسخ الأمر"
    override val copiedCommand = "تم النسخ ✓"

    override val adbGuideTitle = "دليل إعداد ADB"
    override val adbIntro = "منح Android هذه الأذونات الحساسة فقط عبر تصحيح USB. اتبع الخطوات الثلاث مرة واحدة — ثم يعمل التطبيق باستقلالية تامة."
    override val step1Title = "الخطوة 1 — تفعيل خيارات المطوّر"
    override val step1Body = "الإعدادات ← عن الهاتف ← اضغط «رقم الإصدار» سبع مرات، ثم عد للخلف."
    override val step2Title = "الخطوة 2 — تفعيل تصحيح USB"
    override val step2Body = "الإعدادات ← النظام ← خيارات المطوّر ← فعّل تصحيح USB، ثم وصّل الهاتف بالحاسوب."
    override val step3Title = "الخطوة 3 — منح الأذونات"
    override val step3Body = "نفّذ هذين الأمرين في الطرفية:"
    override val commandLabel = "الأمر"
    override val verifyTitle = "التحقق من ضمان الأمان"
    override val verifyBody = "يجب أن يقرأ المفتاح الرئيسي للوصولية 0 عندما يكون Reddit مغلقًا. يكتب التطبيق 1 فقط أثناء الثواني الدقيقة التي يكون فيها com.reddit.frontpage في المقدمة، ثم يُجبر القيمة 0 فورًا بعدها."
    override val safetyTitle = "نموذج الأمان"
    override val safetyBody = "يُستخدم WRITE_SECURE_SETTINGS فقط لضبط اسم مضيف DNS الخاص في Android وإجبار accessibility_enabled على 0 عند خروج Reddit من المقدمة. يُستخدم PACKAGE_USAGE_STATS فقط لاكتشاف التطبيق في المقدمة. لا يُصرّح التطبيق بإذن INTERNET — لا يغادر أي شيء الجهاز أبدًا."

    override val overlayTitle = "🛡️ تم حجب محتوى NSFW"
    override val overlayBody = "حجب DNS Guard هذا المحتوى لأن Reddit في المقدمة."
    override val overlayRevealButton = "إظهار لمدة 10 ثوانٍ"
    override val overlayFooter = "درع الوصلية يعمل فقط داخل Reddit."

    override val notifTitle = "DNS Guard في وضع المراقبة"
    override val notifText = "الوصولية معطّلة. لا تتفعل إلا عند فتح Reddit."
    override val notifChannelName = "مراقب الدرع"
    override val notifChannelDesc = "يُبقي درع Reddit من NSFW مُسلَّحًا ويضمن تعطيل الوصولية في كل وقت آخر."
}

/** Resolves the string bundle for a language. */
fun stringsFor(language: AppLanguage): Strings = when (language) {
    AppLanguage.EN -> EnglishStrings
    AppLanguage.AR -> ArabicStrings
}
