# ---- Room -------------------------------------------------------------------
-keep class androidx.room.** { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-dontwarn androidx.room.paging.**

# ---- Gson (model classes are reflected) --------------------------------------
-keep class com.novatube.app.data.model.** { *; }
-keep class com.novatube.app.data.entity.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class com.google.gson.reflect.TypeToken
-keep class * extends com.google.gson.reflect.TypeToken

# ---- OkHttp / Okio -----------------------------------------------------------
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# ---- Media3 / ExoPlayer ------------------------------------------------------
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# ---- Kotlin coroutines -------------------------------------------------------
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# ---- App reflection-friendly keep rules -------------------------------------
-keep class com.novatube.app.NovaTubeApp { *; }
-keep class com.novatube.app.MainActivity { *; }
-keep class com.novatube.app.service.DownloadServiceImpl { *; }
