import org.gradle.jvm.tasks.Jar

plugins {
    id("gg.essential.multi-version")
    id("gg.essential.defaults")
    id("com.github.johnrengelman.shadow")
}

base.archivesName.set("${rootProject.name}-${platform.mcVersionStr}")

val minecraftSupportedVersions = mapOf(
    11902 to "[\">=1.19.2\", \"<=1.20.4\"]",
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
    maven("https://repo.plo.su")
}

dependencies {
    if (platform.mcVersion >= 12100) {
        modImplementation("net.fabricmc.fabric-api:fabric-api:0.100.7+1.21")
    } else {
        modImplementation("net.fabricmc.fabric-api:fabric-api:0.77.0+1.19.2")
    }

    implementation(libs.plasmovoice)

    annotationProcessor(libs.lombok)

    modImplementation("maven.modrinth:plasmo-voice:fabric-${platform.mcVersionStr}-2.0.10")

    if (platform.mcVersion >= 12100) {
        modImplementation("maven.modrinth:replaymod:${platform.mcVersionStr}-2.6.17")
    } else {
        modImplementation("maven.modrinth:replaymod:${platform.mcVersionStr}-2.6.14")
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
