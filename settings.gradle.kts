pluginManagement {
    repositories {
        // 👇 必须加上谷歌官方仓库，KSP 只在这里发布
        maven("https://dl.google.com/dl/android/maven2")
        maven("https://plugins.gradle.org/m2/")

        // 你原来的阿里云仓库保留
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/gradle-plugin")
        maven("https://maven.aliyun.com/repository/maven-central")
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // 这里也加上谷歌官方仓库
        maven("https://dl.google.com/dl/android/maven2")
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/maven-central")
        google()
        mavenCentral()
    }
}

rootProject.name = "智能字幕剪辑工具"
include(":app")