package com.hassn.app.util

object Constants {

    // ── Built-in package names ─────────────────────────────────────
    const val CHROME_PACKAGE = "com.android.chrome"
    const val BRAVE_PACKAGE = "com.brave.browser"
    const val REDDIT_PACKAGE = "com.reddit.frontpage"

    // ── Redirect modes ─────────────────────────────────────────────
    const val MODE_ALWAYS = "always"
    const val MODE_PRIVATE_ONLY = "private_only"

    // ── Incognito keywords - دقيقة جداً لتجنب الإيجابيات الكاذبة
    // Reddit: فقط عبارات التصفح المجهول الكاملة، ليس كلمة anonymous وحدها (تظهر في البروفايل)
    // Brave: فقط عبارات التبويب الخاص، ليس كلمة private وحدها (تظهر في مواقع كثيرة)
    val CHROME_INCOGNITO_KEYWORDS = listOf(
        "you've gone incognito", "incognito"
    )
    val BRAVE_PRIVATE_KEYWORDS = listOf(
        "private tab", "private browsing", "new private tab"
    )
    val REDDIT_ANONYMOUS_KEYWORDS = listOf(
        "anonymous browsing", "browse anonymously",
        "stop anonymous browsing", "anonymous mode"
    )
    // كلمات عربية - فقط العبارات الكاملة
    val ARABIC_PRIVATE_KEYWORDS = listOf(
        "التصفح الخفي", "وضع التصفح الخفي",
        "التصفح الخاص", "تبويب خاص",
        "التصفح المجهول"
    )
    val INCOGNITO_KEYWORDS = CHROME_INCOGNITO_KEYWORDS +
            BRAVE_PRIVATE_KEYWORDS + REDDIT_ANONYMOUS_KEYWORDS + ARABIC_PRIVATE_KEYWORDS

    // Built-in monitored apps packages (لا تُحذف)
    val BUILT_IN_PACKAGES = setOf(REDDIT_PACKAGE, CHROME_PACKAGE, BRAVE_PACKAGE)

    // Default destination "واعي"
    const val DEFAULT_DEST_PACKAGE = "com.waie.app"
    const val DEFAULT_DEST_LABEL = "واعي"

    // ── Timing ─────────────────────────────────────────────────────
    const val DEBOUNCE_MS = 800L
    const val REDIRECT_DELAY_MS = 350L

    // ── DataStore keys ─────────────────────────────────────────────
    const val PREF_MONITORING_ENABLED = "monitoring_enabled"
    const val PREF_TARGET_APP_PACKAGE = "target_app_package"
    const val PREF_TARGET_APP_NAME = "target_app_name"
    const val PREF_DISABLE_ATTEMPTS = "disable_attempts"
    const val PREF_DISABLE_FAILS = "disable_fails"
    const val PREF_LAST_DISABLE_TS = "last_disable_ts"
    const val PREF_LOCKOUT_UNTIL = "lockout_until"
    const val PREF_MONITORED_APPS = "monitored_apps_json"
    const val PREF_APP_LANGUAGE = "app_language"
    const val PREF_FIRST_LAUNCH = "first_launch_done"

    // ── Suggested destination apps ─────────────────────────────────
    val SUGGESTED_APPS = listOf(
        "com.google.android.apps.nbu.files",
        "com.google.android.calendar",
        "com.google.android.apps.tasks",
        "com.google.android.keep",
        "com.google.android.apps.docs",
        "com.microsoft.office.outlook",
        "com.microsoft.office.word",
        "com.evernote",
        "com.todoist",
        "com.notion.id",
        "com.duolingo",
        "com.amazon.kindle",
        "com.audible.application",
        "com.spotify.music",
        "com.headspace.app",
        "com.calm.android",
        "com.google.android.apps.books",
        "com.google.android.deskclock",
        "com.google.android.apps.maps",
        "com.android.settings",
        "com.android.chrome"
    )

    // ── Popular distracting apps (for quick-add suggestions) ───────
    val POPULAR_DISTRACTING_APPS = listOf(
        "com.reddit.frontpage",
        "com.android.chrome",
        "com.brave.browser",
        "com.instagram.android",
        "com.zhiliaoapp.musically", // TikTok
        "com.ss.android.ugc.trill",
        "com.facebook.katana",
        "com.snapchat.android",
        "com.twitter.android",
        "com.google.android.youtube",
        "com.vivo.browser",
        "com.sec.android.app.sbrowser",
        "org.mozilla.firefox",
        "com.opera.browser"
    )

    // ── Challenge tuning ───────────────────────────────────────────
    const val MIN_ACTIVITIES_PER_ATTEMPT = 3
    const val MAX_ACTIVITIES_PER_ATTEMPT = 5
    const val BASE_FAILS_FOR_BASE_QUEUE = 2
    const val LOCKOUT_STEP_SECONDS = 60L
    const val LOCKOUT_MAX_SECONDS = 60L * 30L
    const val LOCKOUT_RESET_AFTER_SECONDS = 60L * 60L * 24L
}
