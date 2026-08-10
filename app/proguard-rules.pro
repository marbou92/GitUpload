# ============================================================================
# GitUpload ProGuard / R8 rules
# ============================================================================
# Minification (isMinifyEnabled = true) is enabled for release builds, so R8
# shrinks, obfuscates and optimises the release APK. The rules below keep the
# handful of things R8 cannot prove are reachable by static analysis:
#
#   * Moshi @JsonClass data classes (KSP-generated adapters reference them by
#     fully-qualified name, so R8 would otherwise think they are unused).
#   * Retrofit's GitHubApiService interface (Retrofit builds a dynamic proxy
#     from it at runtime via java.lang.reflect.Proxy).
#   * Room entities and DAOs (Room's generated code looks them up by name).
#   * Kotlin metadata so reflection on Kotlin classes keeps working.
#   * Firebase, OkHttp, Coil consumer rules are already shipped by those
#     libraries, so we only silence a few known-safe warnings.
#
# Obfuscation is intentionally left ON. To debug a release-only stack trace,
# run `./gradlew :app:assembleRelease` then use the generated mapping file at
# app/build/outputs/mapping/release/mapping.txt with (retrace)[https://developer.android.com/studio/build/shrink-code#retrace].
# ============================================================================

# --- Kotlin metadata ---------------------------------------------------------
-keepattributes Kotlin*Runtime*,RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,RuntimeInvisibleAnnotations,RuntimeInvisibleParameterAnnotations,AnnotationDefault,Signature,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable
-keep class kotlin.Metadata { *; }

# --- Moshi -------------------------------------------------------------------
# Keep every @JsonClass-annotated data class so its KSP-generated adapter can
# find it by name. Field names are themselves managed by the adapter so we do
# not need to keep individual members.
-keep @com.squareup.moshi.JsonClass class * { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}
# Keep the model + entity packages wholesale as a belt-and-braces measure.
-keep class com.gitupload.data.models.** { *; }
-keep class com.gitupload.data.db.** { *; }

# --- Retrofit ----------------------------------------------------------------
# The GitHub API service is a Retrofit interface; Retrofit creates a dynamic
# proxy from it at runtime so R8 cannot see the method invocations.
-keep,allowobfuscation interface com.gitupload.data.api.GitHubApiService { *; }
# Retrofit / OkHttp consumer rules already cover the runtime, but keep the
# generic response parameter types referenced by service methods.
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }
-keep class okio.** { *; }
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

# --- Room --------------------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { <methods>; }

# --- Android Keystore / crypto used by TokenCrypto --------------------------
# javax.crypto and java.security.KeyStore are part of the platform; no keeps
# needed, but silence warnings about missing references on older API levels.
-dontwarn javax.crypto.**
-dontwarn java.security.KeyStore$Builder

# --- Compose runtime ---------------------------------------------------------
# Compose ships its own consumer rules; we only suppress the occasional
# internal-API warning.
-dontwarn androidx.compose.**

# --- Firebase ----------------------------------------------------------------
# Firebase BOM consumer rules cover the runtime. Keep the Auth callback types
# used by reflection (OAuthProvider).
-keep class com.google.firebase.auth.** { *; }
-keep class com.google.firebase.auth.OAuthCredential { *; }
-dontwarn com.google.firebase.**
