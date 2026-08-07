import org.spongepowered.gradle.plugin.config.PluginLoaders
import org.spongepowered.plugin.metadata.model.PluginDependency

plugins {
    `java-library`
    id("org.spongepowered.gradle.vanilla") version "0.3.2"
    id("org.spongepowered.gradle.plugin") version "2.3.0"
}

description = "Makes tinted glass hide beacon beams without disabling beacon effects."

repositories {
    mavenCentral()
    maven("https://repo.spongepowered.org/repository/maven-public/") {
        name = "sponge"
    }
}

minecraft {
    version("26.2")
}

sponge {
    apiVersion("20.0.0-SNAPSHOT")
    license("MIT")
    loader {
        name(PluginLoaders.JAVA_PLAIN)
        version("1.0")
    }
    plugin("simplenobeambeacon") {
        displayName("SimpleNoBeamBeacon")
        entrypoint("com.captainratax.simplenobeambeacon.sponge.SimpleNoBeamBeaconSponge")
        description(project.description)
        dependency("spongeapi") {
            loadOrder(PluginDependency.LoadOrder.AFTER)
            optional(false)
        }
    }
}

dependencies {
    compileOnly("org.spongepowered:mixin:0.8.7")
    compileOnly("io.github.llamalad7:mixinextras-common:0.5.3")
    annotationProcessor("org.spongepowered:mixin:0.8.7:processor")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(25)
}

tasks.jar {
    archiveBaseName.set("SimpleNoBeamBeacon-Sponge-26.2")
    manifest {
        attributes("MixinConfigs" to "simplenobeambeacon.mixins.json")
    }
}
