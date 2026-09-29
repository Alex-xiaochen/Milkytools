plugins {
    // The version comes from settings.gradle.kts. This plugin picks
    // net.fabricmc.fabric-loom-remap for obfuscated Minecraft (<26) and
    // net.fabricmc.fabric-loom for the unobfuscated versions (>=26).
    id("dev.kikugie.loom-back-compat")
}

// This is Stonecutter's central script: it runs once per version node in versions/.
val minecraftVersion: String = sc.current.version

// 1.21.11 ships Java 21 class files, 26.1+ ship Java 25. This single value drives both
// options.release and the ${java} placeholder in the json resources, so the bytecode
// level and the declared compatibility level cannot drift apart.
val requiredJava: Int = when {
    sc.current.parsed >= "26.1" -> 25
    sc.current.parsed >= "1.20.5" -> 21
    else -> 17
}

// Resolved up front: inside task configuration blocks `property(...)` would bind to
// Task.property(...) instead of the project's, since Task also declares that method.
val modVersion: String = "${property("mod_version")}+$minecraftVersion"
val loaderVersion: String = property("loader_version") as String
val minecraftDependency: String = property("minecraft_dependency") as String

version = modVersion
group = property("mod_group") as String

base {
    archivesName.set(property("mod_archives_base_name") as String)
}

repositories {
    maven {
        name = "TerraformersMC"
        url = uri("https://maven.terraformersmc.com/releases/")
    }
    maven {
        name = "Modrinth"
        url = uri("https://api.modrinth.com/maven")
    }
}

dependencies {
    // A no-op on unobfuscated versions; on 1.21.11 it is equivalent to
    // mappings(loom.officialMojangMappings()). Sticking to Mojang's official names keeps
    // the shared source identical across versions instead of matching Yarn on 1.21.11.
    loomx.applyMojangMappings()

    minecraft("com.mojang:minecraft:$minecraftVersion")

    // modImplementation stays valid on every node: on unobfuscated Loom
    // loom-back-compat aliases it back to implementation.
    modImplementation("net.fabricmc:fabric-loader:$loaderVersion")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_api_version")}")

    // malilib is a hard dependency (declared in fabric.mod.json "depends"), so it has to
    // take part in both compilation and the development runtime.
    modImplementation("maven.modrinth:malilib:${property("malilib_version")}")

    // Mod Menu is an optional integration (it only provides the config screen factory),
    // so it stays compile-only plus dev runtime and never becomes a hard dependency.
    modCompileOnly("com.terraformersmc:modmenu:${property("modmenu_version")}")
    modLocalRuntime("com.terraformersmc:modmenu:${property("modmenu_version")}")
}

java {
    // One JDK 25 toolchain drives every node; the bytecode level is set per node by
    // options.release below.
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

// Do not split source sets for 26.x no-mappings builds: splitEnvironmentSourceSets()
// can force Loom to configure remap/mappings paths.
tasks.withType<JavaCompile>().configureEach {
    options.release.set(requiredJava)
    options.encoding = "UTF-8"
}

tasks.processResources {
    inputs.property("version", modVersion)
    inputs.property("java", requiredJava)
    inputs.property("minecraft", minecraftVersion)
    inputs.property("minecraft_dependency", minecraftDependency)

    // Plain JSON cannot hold Stonecutter comments, so these two files use ${}
    // placeholders filled in at build time instead.
    filesMatching(listOf("fabric.mod.json", "milkytools.mixins.json")) {
        expand(
            mapOf(
                "version" to modVersion,
                "java" to requiredJava.toString(),
                "minecraft" to minecraftVersion,
                "minecraft_dependency" to minecraftDependency,
                "loader" to loaderVersion,
            )
        )
    }
}

// Stonecutter 0.9 has no built-in buildAndCollect (the chiseled tasks were removed in
// 0.7), so register one. loomx.modJar is remapJar on obfuscated nodes and jar on the rest.
// Sync rather than Copy so each per-version directory mirrors the build output exactly;
// a plain copy would leave stale jars behind when an artifact stops being produced.
tasks.register<Sync>("buildAndCollect") {
    group = "build"
    description = "Builds the mod and collects the jar into build/libs/<minecraft version>/"
    from(loomx.modJar)
    into(rootProject.layout.buildDirectory.dir("libs/$minecraftVersion"))
}
