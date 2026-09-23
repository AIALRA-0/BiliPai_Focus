// 根目录 build.gradle.kts
// AGP 9.3 ships R8 9.3.x (newer than the previous Kotlin-2.4 pin of 9.1.31).
// Do not force an older com.android.tools:r8 override unless AGP's bundled R8 regresses.

plugins {
    // 1. Android 插件 (版本号要固定)
    // AGP ≥ 9.1 required by Compose BOM 2026.06 / Material3 alpha25 / Lifecycle 2.11 / Nav3 1.2
    id("com.android.application") version "9.3.2" apply false
    id("com.android.library") version "9.3.2" apply false
    id("com.android.test") version "9.3.2" apply false

    id("com.google.devtools.ksp") version "2.3.10" apply false

    // 2. Kotlin 插件（AGP 9+ 内置 Kotlin，无需 org.jetbrains.kotlin.android）
    // Compose 编译器插件
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.0" apply false
    // 序列化插件
    id("org.jetbrains.kotlin.plugin.serialization") version "2.4.0" apply false
    
    // 3. Firebase 相关插件
    id("com.google.gms.google-services") version "4.4.2" apply false
    id("com.google.firebase.crashlytics") version "3.0.2" apply false
}
