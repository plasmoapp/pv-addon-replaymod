pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenLocal()
        mavenCentral()
        google()

        maven("https://jitpack.io/")
        maven("https://maven.fabricmc.net")
        maven("https://maven.minecraftforge.net")
        maven("https://repo.plasmoverse.com/releases")
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.7.10"
}

stonecutter {
    centralScript = "build.gradle.kts"

    create(rootProject) {
        versions("1.16.5", "1.21.1", "26.1.2")
        vcsVersion = "1.16.5"
    }
}

rootProject.name = "pv-addon-replaymod"
