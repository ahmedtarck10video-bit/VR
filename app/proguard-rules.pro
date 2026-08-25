# Proguard rules for app
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.room.* *;
}
