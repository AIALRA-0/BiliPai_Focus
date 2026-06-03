# === BiliPai ProGuard Rules ===
# Fixes: java.lang.Class cannot be cast to java.lang.reflect.ParameterizedType

# --- General ---
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keepattributes InnerClasses,EnclosingMethod
-dontwarn javax.annotation.**
-dontwarn org.jetbrains.annotations.**

# === 优化选项 ===
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification
-dontpreverify

# === CRITICAL: Retrofit + OkHttp ===
# Keep generic signature for Retrofit API interfaces
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
# Keep Call/Response generic types
-keep,allowobfuscation,allowshrinking class retrofit2.Response
# Keep Retrofit service methods
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# === CRITICAL: Kotlinx Serialization ===
# Keep @Serializable classes and their serializers
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keep class * implements kotlinx.serialization.KSerializer { *; }
-keepclassmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
# Keep generated serializers
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
}
-keepclasseswithmembers class ** {
    kotlinx.serialization.KSerializer serializer(...);
}

# === Data Models (MUST keep for serialization) ===
-keep class com.android.purebilibili.data.model.** { *; }
-keepclassmembers class com.android.purebilibili.data.model.** { *; }

# === API Interfaces ===
-keep interface com.android.purebilibili.core.network.** { *; }
-keep class com.android.purebilibili.core.network.** { *; }

# === ViewModel 保护 ===
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keepclassmembers class * extends androidx.lifecycle.ViewModel { *; }

# === Compose ===
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Android 16 ART can reject the heavily optimized dex for the large Compose
# VideoDetailScreen entrypoint. Keep this class unoptimized while preserving
# R8 for the rest of the release build.
-keep class com.android.purebilibili.feature.video.screen.VideoDetailScreenKt { *; }

# Release-only player overlay regressions are hard to diagnose because gestures
# can keep working while Compose control layers stop rendering. Keep the
# player section and overlay classes out of R8 optimization; this preserves
# minification for the rest of the app while protecting the control UI path.
-keep class com.android.purebilibili.feature.video.ui.section.** { *; }
-keep class com.android.purebilibili.feature.video.ui.overlay.** { *; }

# Release 下底栏搜索入口曾出现点击无响应，只在正式版复现。
# 保留底栏搜索、搜索页入口和导航交接相关的 Compose 函数及合成 lambda，
# 避免 R8 优化破坏点击、展开、焦点和搜索页入场链路。
-keep class com.android.purebilibili.feature.home.components.BottomBarKt { *; }
-keep class com.android.purebilibili.feature.home.components.BottomBarKt$* { *; }
-keep class com.android.purebilibili.feature.search.SearchScreenKt { *; }
-keep class com.android.purebilibili.feature.search.SearchScreenKt$* { *; }
-keep class com.android.purebilibili.feature.search.SearchEntryMotionSource { *; }
-keep class com.android.purebilibili.navigation.AppNavigationKt { *; }
-keep class com.android.purebilibili.navigation.AppNavigationKt$* { *; }

# === Haze (毛玻璃效果) ===
-keep class dev.chrisbanes.haze.** { *; }
-dontwarn dev.chrisbanes.haze.**

# === Cupertino (iOS 风格组件) ===
-keep class io.github.alexzhirkevich.cupertino.** { *; }
-dontwarn io.github.alexzhirkevich.cupertino.**

# === DanmakuFlameMaster ===
-keep class master.flame.danmaku.** { *; }
-dontwarn master.flame.danmaku.**

# === ByteDance DanmakuRenderEngine ===
-keep class com.bytedance.danmaku.** { *; }
-dontwarn com.bytedance.danmaku.**

# === Room Database ===
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *
-dontwarn androidx.room.paging.**

# === Media3 / ExoPlayer ===
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# === Coil Image Loading ===
-keep class coil.** { *; }
-dontwarn coil.**

# === ZXing (二维码) ===
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**

# === Lottie ===
-keep class com.airbnb.lottie.** { *; }
-dontwarn com.airbnb.lottie.**

# === Shimmer ===
-keep class com.valentinilk.shimmer.** { *; }
-dontwarn com.valentinilk.shimmer.**

# === Kotlin Coroutines ===
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }
-dontwarn kotlinx.coroutines.**

# === R8 full mode compatibility ===
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# === Enum 保护 ===
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# === Parcelable ===
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# === Cling (DLNA) ===
-keep class org.fourthline.cling.** { *; }
-dontwarn org.fourthline.cling.**
-dontwarn javax.enterprise.context.**
-dontwarn javax.inject.**
-dontwarn org.seamless.**
