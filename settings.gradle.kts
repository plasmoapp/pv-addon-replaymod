import org.gradle.kotlin.dsl.support.listFilesOrdered

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenLocal()
        mavenCentral()
        google()

        maven("https://jitpack.io/")
        maven("https://maven.fabricmc.net")
        maven("https://maven.architectury.dev/")
        maven("https://maven.minecraftforge.net")
        maven("https://repo.plasmoverse.com/snapshots")
    }

    plugins {
        val egtVersion = "0.7.0-SNAPSHOT"
        id("gg.essential.defaults") version egtVersion
        id("gg.essential.multi-version.root") version egtVersion
    }
}

rootProject.name = "pv-addon-replaymod"

include("versions")
project(":versions").apply {
    projectDir = file("versions/")
    buildFileName = "root.gradle.kts"
}

file("versions").listFilesOrdered {
    return@listFilesOrdered it.isDirectory && it.name.contains("-")
}.forEach {
    include("versions:${it.name}")
    project(":versions:${it.name}").apply {
        projectDir = file("versions/${it.name}")
        buildFileName = "../build.gradle.kts"
    }
}