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
    // Compiling against the oldest useful API prevents accidental use of newer,
    // incompatible Bukkit methods. Tinted glass was introduced in Minecraft 1.17.
    compileOnly("org.spigotmc:spigot-api:1.17.1-R0.1-SNAPSHOT")

    testImplementation("org.spigotmc:spigot-api:1.17.1-R0.1-SNAPSHOT")
    // JUnit 5 still supports Java 16, matching the plugin's compatibility target.
    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // A second compilation pass guards the primary target against API drift.
    paper26Check("io.papermc.paper:paper-api:26.2.build.102-stable")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    // Minecraft 1.17 runs on Java 16. Newer servers can load this bytecode too.
    options.release.set(16)
    options.encoding = "UTF-8"
}

tasks.processResources {
    inputs.property("version", pluginVersion)
    filesMatching("plugin.yml") {
        expand("version" to pluginVersion)
    }
}

tasks.jar {
    archiveBaseName.set("SimpleNoBeamBeacon-Bukkit")
}

tasks.test {
    useJUnitPlatform()
}

val compilePaper26CheckJava = tasks.register<JavaCompile>("compilePaper26CheckJava") {
    description = "Compiles the Bukkit sources against the Paper 26.2 API."
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
