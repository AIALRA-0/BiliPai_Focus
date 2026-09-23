plugins {
    id("com.android.library")
    // AGP 9+ built-in Kotlin
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.android.purebilibili.designsystem"
    compileSdk {
        version = release(37) {
            minorApiLevel = 0
        }
    }

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests.all {
            it.useJUnitPlatform()
        }
        unitTests.isReturnDefaultValues = true
    }

    lint {
        abortOnError = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

dependencies {
    api(platform(libs.androidx.compose.bom))
    api("androidx.compose.ui:ui")
    api("androidx.compose.foundation:foundation")
    api("androidx.compose.animation:animation")
    api("androidx.compose.material:material-icons-extended")
    api("androidx.compose.material3:material3:1.5.0-alpha25")
    api("dev.chrisbanes.haze:haze:2.0.0-alpha03")
    api("dev.chrisbanes.haze:haze-blur:2.0.0-alpha03")
    implementation("dev.chrisbanes.haze:haze-blur-materials:2.0.0-alpha03")
    implementation("io.github.alexzhirkevich:cupertino:0.1.0-alpha04")
    api("io.github.alexzhirkevich:cupertino-icons-extended:0.1.0-alpha04")
    api(libs.miuix.ui)
    implementation(libs.miuix.preference)

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.10.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:2.4.0")
}
