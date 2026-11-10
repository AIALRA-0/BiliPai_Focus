# Retrofit reads service annotations and generic suspend signatures at runtime.
-keepattributes Signature,InnerClasses,EnclosingMethod,RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations
-keep,allowoptimization,allowshrinking interface com.android.purebilibili.core.network.** { *; }
