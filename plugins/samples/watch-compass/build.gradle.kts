buildscript {
    dependencies {
        // Match the local SDK's Kotlin metadata without applying kotlin-android.
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.0")
    }
}

plugins {
    id("com.android.library") version "9.3.2" apply false
    // AGP 9+ built-in Kotlin — no org.jetbrains.kotlin.android
}
