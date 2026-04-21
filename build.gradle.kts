import net.fabricmc.loom.LoomGradlePlugin
import net.fabricmc.loom.LoomNoRemapGradlePlugin
import net.fabricmc.loom.api.LoomGradleExtensionAPI
import net.fabricmc.loom.task.RemapJarTask

plugins {
    kotlin("jvm") version libs.versions.kotlin
    alias(libs.plugins.loom) apply false
    alias(libs.plugins.buildconfig)
}

stonecutter {
    fun fromFile(direction: Boolean, path: String) {
        file(rootProject.layout.projectDirectory.dir("versions").file(path))
            .readText()
            .lines()
            .filter { it.isNotBlank() }
            .map { it.trim() }
            .filter { !it.startsWith("#") }
            .map { it.substringBefore(" ") to it.substringAfter(" ") }
            .forEach { (replaceFrom, replaceTo) ->
                replacements.string {
                    this.direction = direction
                    replace(replaceFrom, replaceTo)
                }
            }
    }

    fromFile(eval(current.version, ">=1.21"), "1.16.5-1.21.txt")
    fromFile(eval(current.version, ">=26.1"), "1.21-26.1.txt")
}

val minecraftVersion = stonecutter.current.version.substringBefore('-')

base.archivesName.set("${rootProject.name}-${minecraftVersion}")

val noMappings = stonecutter.eval(minecraftVersion, ">=26.1")

if (noMappings) {
    apply<LoomNoRemapGradlePlugin>()

    configurations.api.get().extendsFrom(configurations.create("modApi"))
    configurations.implementation.get().extendsFrom(configurations.create("modImplementation"))
    configurations.compileOnly.get().extendsFrom(configurations.create("modCompileOnly"))
    configurations.runtimeOnly.get().extendsFrom(configurations.create("modRuntimeOnly"))
} else {
    apply<LoomGradlePlugin>()
}

val loom = the<LoomGradleExtensionAPI>()

repositories {
    maven {
        name = "Modrinth"
        url = uri("https://api.modrinth.com/maven")

        content {
            includeGroup("maven.modrinth")
        }
    }
    maven("https://repo.plasmoverse.com/releases")
}

dependencies {
    "minecraft"("com.mojang:minecraft:$minecraftVersion")
    if (!noMappings) {
        "mappings"(loom.officialMojangMappings())
    }

    annotationProcessor(libs.lombok)

    implementation(libs.plasmovoice)

    "modImplementation"(libs.fabricloader)

    "modImplementation"("net.fabricmc.fabric-api:fabric-api:${property("deps.fabric_api")}")
    "modImplementation"("maven.modrinth:plasmo-voice:${property("deps.plasmo_voice")}")
    "modImplementation"("maven.modrinth:replaymod:${property("deps.replaymod")}")
}

buildConfig {
    packageName("su.plo.replayvoice")
    buildConfigField("VERSION", project.version.toString())
}

tasks {
    processResources {
        inputs.property("version", version)

        from("LICENSE")
        filesMatching("fabric.mod.json") {
            expand(mapOf(
                "version" to version,
                "loader_version" to "0.17.0",
                "minecraft_dependency" to project.property("mod.minecraft_dependency"),
                "pv_dependency" to libs.versions.plasmovoice.get(),
                "replaymod_dependency" to "1.16.4-2.6.9" // todo: ???
            ))
        }
    }

    java {
        toolchain.languageVersion.set(JavaLanguageVersion.of(property("deps.java_version").toString().toInt()))
        withSourcesJar()
    }

    jar {
        from("LICENSE") {
            rename { "${it}_${rootProject.name}" }
        }
    }

    val copyToRoot =
        register<Copy>("copyToRoot") {
            val outputJar = if (!noMappings) named<RemapJarTask>("remapJar") else jar

            from(outputJar.get().archiveFile)
            into(rootProject.layout.buildDirectory.dir("libs"))
        }

    build {
        finalizedBy(copyToRoot)
    }
}
