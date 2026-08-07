pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.spongepowered.org/repository/maven-public/")
    }
}

rootProject.name = "SimpleNoBeamBeacon"

include("bukkit")
include("sponge")
