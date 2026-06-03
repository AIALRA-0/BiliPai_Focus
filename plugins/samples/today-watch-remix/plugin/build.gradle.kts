plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "dev.example.todaywatchremix"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        jvmToolchain(21)
    }
}

dependencies {
    implementation("com.github.jay3-yy.BiliPai:plugin-sdk:0.1.0-SNAPSHOT")
}

val packageBpPlugin by tasks.registering(Zip::class) {
    dependsOn("assembleRelease")
    archiveBaseName.set("today-watch-remix")
    archiveExtension.set("bpplugin")
    destinationDirectory.set(rootProject.layout.buildDirectory.dir("distributions"))

    from(rootProject.layout.projectDirectory.file("plugin-manifest.json"))
    from(layout.buildDirectory.file("intermediates/aar_main_jar/release/syncReleaseLibJars/classes.jar")) {
        rename { "classes.jar" }
    }
}
