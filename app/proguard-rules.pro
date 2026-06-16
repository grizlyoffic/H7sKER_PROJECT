# ─── H7skER TOOL — ProGuard Rules v3.0 ──────────────────────────────────────

# Keep app entry points
-keep class com.nexbytes.h7skertool.MainActivity { *; }
-keep class com.nexbytes.h7skertool.service.** { *; }

# Shizuku AIDL interfaces (must not be renamed)
-keep interface rikka.shizuku.** { *; }
-keep class rikka.shizuku.** { *; }
-keep class com.nexbytes.h7skertool.shizuku.** { *; }

# OkHttp / TLS pinning — keep all
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okio.** { *; }

# NanoHTTPD
-keep class fi.iki.elonen.** { *; }

# Gson serialisation (ModFile, models)
-keep class com.nexbytes.h7skertool.utils.ModFile { *; }
-keep class com.nexbytes.h7skertool.model.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**

# DataStore / Kotlin Preferences
-keep class androidx.datastore.** { *; }
-keep class kotlin.coroutines.** { *; }
-dontwarn kotlin.**

# Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# SSL pinning — keep cert-checking logic
-keep class javax.net.ssl.** { *; }
-keep class java.security.** { *; }
-dontwarn sun.security.**

# Core utils (proto, hex, conversion, mod manager)
-keep class com.nexbytes.h7skertool.utils.ProtoModifier { *; }
-keep class com.nexbytes.h7skertool.utils.HexUtils { *; }
-keep class com.nexbytes.h7skertool.utils.ConversionUtils { *; }
-keep class com.nexbytes.h7skertool.utils.ModManager { *; }
-keep class com.nexbytes.h7skertool.utils.DecodeUtils { *; }

# Keep enums
-keepclassmembers enum * { *; }

# Strip verbose logs from release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
}

# Optimisation
-optimizationpasses 5
-allowaccessmodification
-repackageclasses 'h'
