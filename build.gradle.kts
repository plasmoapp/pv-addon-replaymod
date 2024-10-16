plugins {
    kotlin("jvm") version libs.versions.kotlin.get()
    id("com.github.johnrengelman.shadow") version libs.versions.shadow.get() apply false
    id("gg.essential.multi-version.root") apply false
    alias(libs.plugins.idea.ext)
}

tasks.jar {
    enabled = false
}