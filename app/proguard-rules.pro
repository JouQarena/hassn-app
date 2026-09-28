# DNS Guard & Shield — R8 / ProGuard rules.
#
# The app is fully self-contained Kotlin + Jetpack Compose. The rules below mainly
# protect the Android components that are referenced from the manifest through
# reflection-free but string-based wiring, and keep line numbers for readable
# crash reports.

# Keep all application components (services, receivers, providers, activities).
-keep class com.dnsguard.shield.MainActivity { *; }
-keep class com.dnsguard.shield.service.** { *; }
-keep class com.dnsguard.shield.receiver.** { *; }

# Keep line numbers for deobfuscated stack traces.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Kotlin
-dontwarn kotlinx.coroutines.**

# Compose is handled automatically by the Compose R8 rules bundled with the
# Android Gradle Plugin; nothing extra is required here.
