/* Copyright 2026 Vsevolod Petrov (lazyhat). Licensed under Apache-2.0. */
import java.util.Properties
import java.util.zip.ZipFile
import org.jmailen.gradle.kotlinter.tasks.ConfigurableKtLintTask

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("dev.architectury.loom")
    id("architectury-plugin")
    id("ru.lazyhat.compukters.addon")
    id("org.jmailen.kotlinter")
}
group = "ru.lazyhat.compukters"
version = providers.gradleProperty("addonVersion").get()
val sdk = Properties().apply { file(gradle.extra["compuktersSdkDirectory"].toString()).resolve("sdk.properties").inputStream().use(::load) }
val sdkVersion = sdk.getProperty("sdkVersion")
val targetLine = sdk.getProperty("modVersion").split('.').take(2).joinToString(".")
base { archivesName.set("compukters-connected-1.21.1-neoforge-$targetLine") }
kotlin { jvmToolchain(21) }
java { toolchain.languageVersion.set(JavaLanguageVersion.of(21)) }
architectury { minecraft = "1.21.1"; platformSetupLoomIde(); neoForge() }
repositories {
    mavenCentral()
    maven("https://maven.neoforged.net/releases/")
    maven("https://api.modrinth.com/maven")
    maven("https://maven.createmod.net")
    maven("https://maven.ithundxr.dev/snapshots")
}
apply(from = "gradle/workspace-sdk.gradle.kts")
compuktersAddon { register("connected", "1.0") }
val gameTest by sourceSets.creating
kotlin.target.compilations.named(gameTest.name) { associateWith(kotlin.target.compilations.getByName("main")) }
configurations[gameTest.implementationConfigurationName].extendsFrom(configurations.testImplementation.get())
configurations[gameTest.runtimeOnlyConfigurationName].extendsFrom(configurations.testRuntimeOnly.get())
gameTest.compileClasspath += sourceSets.main.get().compileClasspath + sourceSets.main.get().output
gameTest.runtimeClasspath += sourceSets.main.get().runtimeClasspath + sourceSets.main.get().output

dependencies {
    minecraft("net.minecraft:minecraft:1.21.1")
    mappings(loom.officialMojangMappings())
    neoForge("net.neoforged:neoforge:21.1.252")
    modImplementation("com.simibubi.create:create-1.21.1:6.0.10-280:slim") { isTransitive = false }
    modImplementation("maven.modrinth:create-connected:Xe7EqzfQ") { isTransitive = false }
    modImplementation("net.createmod.ponder:ponder-neoforge:1.0.82+mc1.21.1")
    modCompileOnly("dev.engine-room.flywheel:flywheel-neoforge-api-1.21.1:1.0.6")
    modRuntimeOnly("dev.engine-room.flywheel:flywheel-neoforge-1.21.1:1.0.6")
    modImplementation("com.tterrag.registrate:Registrate:MC1.21-1.3.0+67")
    compileOnly("ru.lazyhat.compukters:compukters-addon-api:$sdkVersion")
    compileOnly("ru.lazyhat.compukters:compukters-addon-neoforge-1.21.1:$sdkVersion")
    val workspace = file(providers.gradleProperty("compuktersWorkspace").get())
    val development = files(workspace.resolve("modules/minecraft/v1_21_1/v1_21_1-neoforge/build/devlibs/compukters-development-mod.jar"))
        .builtBy(gradle.includedBuild("Compukters").task(":v1_21_1-neoforge:developmentModJar"))
    runtimeOnly(development)
    add(gameTest.implementationConfigurationName, development)
    testImplementation("ru.lazyhat.compukters:compukters-addon-api:$sdkVersion")
    testImplementation("ru.lazyhat.compukters:compukters-addon-neoforge-1.21.1:$sdkVersion")
    testImplementation(kotlin("test"))
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    listOf("org.jetbrains.kotlin:kotlin-stdlib:2.4.10", "io.github.oshai:kotlin-logging-jvm:8.0.4",
        "org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.11.0", "org.tukaani:xz:1.12"
    ).forEach { forgeRuntimeLibrary(it) { isTransitive = false } }
}
loom {
    mods { maybeCreate("compukters_connected").sourceSet("main") }
    runs {
        named("client") { runDir("run/client") }
        named("server") { runDir("run/server") }
        register("gameTestServer") {
            server(); source(gameTest); environment("gametestserver"); forgeTemplate("gameTestServer")
            runDir("run/gameTestServer")
            property("neoforge.enabledGameTestNamespaces", "minecraft")
            mods { maybeCreate("compukters_connected").apply { sourceSet("main"); sourceSet(gameTest.name) } }
        }
    }
}
tasks.test { useJUnitPlatform() }
tasks.withType<ConfigurableKtLintTask>().configureEach { exclude { it.file.path.contains("build/generated") } }
tasks.processResources {
    inputs.property("addonVersion", project.version)
    filesMatching("META-INF/neoforge.mods.toml") {
        expand("addon_version" to project.version, "compukters_version" to sdk.getProperty("modBuildVersion"))
    }
}
tasks.jar { archiveClassifier.set("dev") }
tasks.named("check") { dependsOn(gameTest.classesTaskName, "remapJar") }
tasks.register<Jar>("developmentModJar") {
    manifest.attributes["Fabric-Loom-Remap"] = "false"
    archiveFileName.set("compukters-connected-development-mod.jar")
    destinationDirectory.set(layout.buildDirectory.dir("devlibs"))
    from(sourceSets.main.get().output)
}

val verifyAddonJar = tasks.register("verifyAddonJar") {
    dependsOn("remapJar")
    doLast {
        val archive = tasks.named<net.fabricmc.loom.task.RemapJarTask>("remapJar").get().archiveFile.get().asFile
        val entries = ZipFile(archive).use { zip -> zip.entries().asSequence().map { it.name }.toList() }
        listOf("META-INF/neoforge.mods.toml", "META-INF/compukters/addons/connected.cagb", "compukters_connected.mixins.json").forEach { required ->
            check(entries.count { it == required } == 1) { "Missing or duplicate addon resource: $required" }
        }
        check(entries.none { it.startsWith("com/hlysine/") || it.startsWith("com/simibubi/") || it.startsWith("ru/lazyhat/compukters/api/") }) {
            "Dependency implementation leaked into the standalone addon"
        }
        check(entries.none { it.contains("BatteryGameTest") }) { "GameTests leaked into the production archive" }
    }
}
tasks.named("check") { dependsOn(verifyAddonJar) }
