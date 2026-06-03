pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

rootProject.name = "WatchCompassPlugin"
include(":plugin")

includeBuild("../../..") {
    dependencySubstitution {
        substitute(module("com.github.jay3-yy.BiliPai:plugin-sdk"))
            .using(project(":plugin-sdk"))
    }
}
