# JAVIS Launcher ProGuard Rules

# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }
-keep @dagger.hilt.InstallIn class * { *; }

# Keep Room entities
-keep class com.javis.launcher.data.model.** { *; }
-keep class com.javis.launcher.data.local.** { *; }

# Keep Retrofit models
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }
-keep class com.google.gson.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Kotlin coroutines
-keep class kotlinx.coroutines.** { *; }

# Keep accessibility service
-keep class com.javis.launcher.accessibility.** { *; }

# Keep services and receivers
-keep class com.javis.launcher.services.** { *; }
-keep class com.javis.launcher.receivers.** { *; }
