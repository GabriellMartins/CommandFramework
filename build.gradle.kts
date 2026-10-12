plugins {
    id("java")
    id("net.minecrell.plugin-yml.bukkit") version "0.6.0"
}

group = "org.github.gabrielmartins.command"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    compileOnly(libs.paper.api)
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    testImplementation(platform(libs.junit.bom))
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

bukkit {
    name = "CommandFramework"
    version = "${project.version}"
    main = "org.github.gabrielmartins.command.BukkitCommand"
    apiVersion = "1.21"
    author = "Gabriel Martins"
    description = "A command framework for Bukkit/Spigot/Paper plugins"
}
