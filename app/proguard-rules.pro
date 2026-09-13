# Keep data classes for kotlinx serialization
-keep class com.hassn.app.data.** { *; }

# Keep accessibility service
-keep class com.hassn.app.service.HassnAccessibilityService { *; }

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep AccessibilityNodeInfo methods
-keep class android.view.accessibility.AccessibilityNodeInfo { *; }
