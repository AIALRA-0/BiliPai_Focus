pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        // 🔥 ByteDance DanmakuRenderEngine 仓库
        maven { url = uri("https://artifact.bytedance.com/repository/releases/") }
        // Cling Repo
        maven { 
            url = uri("http://4thline.org/m2") 
            isAllowInsecureProtocol = true
        }
    }
}

rootProject.name = "BiliPai"
include(":app")
include(":baselineprofile")
include(":settings-core")
include(":network-core")
include(":plugin-sdk")

 
