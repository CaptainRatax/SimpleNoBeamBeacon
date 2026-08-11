plugins {
    java
}

val pluginVersion = project.version.toString()

val paper26Check = configurations.create("paper26Check") {
    isCanBeConsumed = false
    isCanBeResolved = true
    attributes {
        attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 25)
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "paper"
    }
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") {
        name = "spigotSnapshots"
        mavenContent { snapshotsOnly() }
    }
}

dependencies {
    // Tinted glass was introduced in Minecraft 1.17. Compiling against that
    // API prevents accidental use of Bukkit methods introduced later.
    compileOnly("org.spigotmc:spigot-api:1.17.1-R0.1-SNAPSHOT")

    testImplementation("org.spigotmc:spigot-api:1.17.1-R0.1-SNAPSHOT")
    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // Paper 26.2 remains the primary target and gets its own API drift check.
    paper26Check("io.papermc.paper:paper-api:26.2.build.102-stable")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
    withSourcesJar()
}

base {
    archivesName.set("SimpleNoBeamBeacon-Paper")
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(16)
    options.encoding = "UTF-8"
}

tasks.processResources {
    inputs.property("version", pluginVersion)
    filesMatching("plugin.yml") {
        expand("version" to pluginVersion)
    }
}

tasks.test {
    useJUnitPlatform()
}

val compilePaper26CheckJava = tasks.register<JavaCompile>("compilePaper26CheckJava") {
    description = "Compiles the plugin sources against the Paper 26.2 API."
    source = sourceSets.main.get().allJava
    classpath = paper26Check
    destinationDirectory.set(layout.buildDirectory.dir("paper26-check/classes"))
    javaCompiler.set(javaToolchains.compilerFor {
        languageVersion.set(JavaLanguageVersion.of(25))
    })
    options.release.set(25)
    options.encoding = "UTF-8"
}

tasks.check {
    dependsOn(compilePaper26CheckJava)
}
