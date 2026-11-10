plugins {
    id("com.android.library")
}

android {
    namespace = "com.android.purebilibili.designtokens"
    compileSdk {
        version = release(37) { minorApiLevel = 0 }
    }
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlin { jvmToolchain(21) }
}

dependencies {
    api(platform(libs.androidx.compose.bom))
    api("androidx.compose.ui:ui-graphics")
    api("androidx.compose.ui:ui-text")
    api("androidx.compose.animation:animation-core")
}
