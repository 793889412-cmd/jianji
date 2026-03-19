pluginManagement {
    repositories {
        // 国内稳定仓库顺序（解决SSL错误）
        maven("https://maven.aliyun.com/repository/gradle-plugin")
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/maven-central")
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // 国内稳定仓库
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/maven-central")
        google()
        mavenCentral()
    }
}

rootProject.name = "智能字幕剪辑工具"
include(":app")