pluginManagement {
    repositories {
        //插件还是从阿里云下载可靠点
        maven("https://maven.aliyun.com/repository/gradle-plugin/")
        //maven("https://oss.sonatype.org/content/repositories/releases/")
        //maven("https://plugins.gradle.org/m2")
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

rootProject.name = "replace-bean-boot-starter"