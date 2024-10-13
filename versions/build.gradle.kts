import org.gradle.jvm.tasks.Jar

plugins {
    id("gg.essential.multi-version")
    id("gg.essential.defaults")
    id("com.github.johnrengelman.shadow")
}

base.archivesName.set("${rootProject.name}-${platform.mcVersionStr}")

val minecraftSupportedVersions = mapOf(
    11605 to "\">=1.16.5 <=1.20.4\"",
    12100 to "\">=1.21\""
)

repositories {
    maven {
        name = "Modrinth"
        url = uri("https://api.modrinth.com/maven")

        content {
            includeGroup("maven.modrinth")
        }
    }
    maven("https://repo.plasmoverse.com/snapshots")
    maven("https://repo.plasmoverse.com/releases")
}

dependencies {
    if (platform.mcVersion >= 12100) {
        modImplementation("net.fabricmc.fabric-api:fabric-api:0.100.7+1.21")
    } else {
        modImplementation("net.fabricmc.fabric-api:fabric-api:0.42.0+1.16")
    }

    implementation(libs.plasmovoice)
    implementation(project(":common"))
    shadow(project(":common")) { isTransitive = false }

    annotationProcessor(libs.lombok)

    modImplementation("maven.modrinth:plasmo-voice:fabric-${platform.mcVersionStr}-2.1.0")

    if (platform.mcVersion >= 12100) {
        modImplementation("maven.modrinth:replaymod:${platform.mcVersionStr}-2.6.17")
    } else if (platform.mcVersion == 11605) {
        modImplementation("maven.modrinth:replaymod:1.16.4-2.6.19")
    }
}

tasks {
    processResources {
        inputs.property("version", version)

        from("LICENSE")
        filesMatching("fabric.mod.json") {
            val fabricLoaderVersion: String by project

            expand(mapOf(
                "version" to version,
                "loader_version" to fabricLoaderVersion,
                "minecraft_dependency" to minecraftSupportedVersions[platform.mcVersion],
                "pv_dependency" to libs.versions.plasmovoice.get(),
                "replaymod_dependency" to "1.16.4-2.6.9" // todo: ???
            ))
        }
    }

    java {
        withSourcesJar()
    }

    jar {
        from("LICENSE") {
            rename { "${it}_${rootProject.name}" }
        }
    }

    shadowJar {
        configurations = listOf(project.configurations.shadow.get())
        archiveClassifier = "shadow-dev"
    }

    remapJar {
        inputFile.set(shadowJar.get().archiveFile.get())
        dependsOn(shadowJar)
    }

    build {
        doLast {
            copyJarToRootProject(remapJar.get())
        }
    }
}

fun Project.copyJarToRootProject(task: Jar) {
    val file = task.archiveFile.get().asFile
    val destinationFile = rootProject.layout.buildDirectory
        .file("libs/${file.name.replace("-all", "")}")
        .get()
        .asFile

    file.copyTo(destinationFile, true)
}
