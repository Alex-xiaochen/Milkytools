pluginManagement {
    repositories {
        maven {
            name = "Fabric"
            url = uri("https://maven.fabricmc.net/")
        }
        maven {
            // dev.kikugie.loom-back-compat lives here
            name = "KikuGie Releases"
            url = uri("https://maven.kikugie.dev/releases")
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
    id("dev.kikugie.stonecutter") version "0.9.8"
    id("dev.kikugie.loom-back-compat") version "0.4.3"
}

stonecutter {
    kotlinController = true
    centralScript = "build.gradle.kts"

    create(rootProject) {
        // Staged rollout: 26.2 first, then 26.1.2 and 26.3, then 1.21.11.
        versions("1.21.11", "26.1.2", "26.2", "26.3")
        // The committed state of src/ is written for 26.2.
        //
        // IMPORTANT: the active version is compiled straight from src/ **without** being
        // preprocessed, so src/ must itself be a valid 26.2 state -- for every `//? if`
        // in src/, the branch matching 26.2 (as well as `stonecutter active`) has to be
        // the plain, uncommented code. Getting this wrong still compiles (mixin targets
        // are just strings) and only fails at runtime with an InjectionError.
        vcsVersion = "26.2"
    }
}

rootProject.name = "Milkytools"
