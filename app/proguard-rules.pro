# Firestore deserializes DTOs via reflection — keep field names and no-arg constructors intact.
-keepclassmembers class com.iseeu.app.data.remote.dto.** {
    <init>(...);
    <fields>;
}

# osmdroid does some reflective config/tile-source lookups.
-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**
