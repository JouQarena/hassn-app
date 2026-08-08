# ProGuard / R8 rules for Focus Redirect
# ──────────────────────────────────────────────────────────────────────

# Keep the Accessibility Service — it's referenced from the manifest only.
-keep class com.focusredirect.app.service.FocusAccessibilityService { *; }

# Keep the Boot Receiver
-keep class com.focusredirect.app.receiver.BootReceiver { *; }

# DataStore (Preferences) uses reflection internally
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}

# Keep Kotlin coroutines internals
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**

# General Android rules
-keepattributes Signature
-keepattributes *Annotation*
