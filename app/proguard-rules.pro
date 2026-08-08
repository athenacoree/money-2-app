# Room Database keep rules
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>(...);
}
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Keep Moshi models and reflective JSON classes
-keep class com.example.data.model.** { *; }
-keep class com.example.data.qvapay.** { *; }

# ZXing Core keep rules
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**

# Kotlin Coroutines and Reflection rules
-keepclassmembernames class kotlinx.coroutines.android.HandlerDispatcher {
    <init>(...);
}
-dontwarn kotlinx.coroutines.**
