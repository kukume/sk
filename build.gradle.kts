plugins {
    val kotlinVersion = "2.0.20"
    kotlin("jvm") version kotlinVersion
    val ktorVersion = "2.3.12"
    id("io.ktor.plugin") version ktorVersion
}

group = "me.kuku"
version = "1.0-SNAPSHOT"

repositories {
//    mavenLocal()
    maven("https://nexus.kuku.me/repository/maven-public/")
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-core")
    implementation("io.ktor:ktor-server-thymeleaf")
    implementation("io.ktor:ktor-server-status-pages")
    implementation("io.ktor:ktor-server-call-logging")
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-serialization-jackson")
    implementation("io.ktor:ktor-server-cio")
    implementation("io.ktor:ktor-server-config-yaml")
    implementation("io.ktor:ktor-serialization-kotlinx-json")
    implementation("io.ktor:ktor-server-double-receive")

    implementation("ch.qos.logback:logback-classic:1.5.6")

    implementation("me.kuku:utils:2.3.12.1")
    implementation("org.jsoup:jsoup:1.15.3")
    implementation("com.microsoft.playwright:playwright:1.44.0")
}

tasks.compileKotlin {
    kotlinOptions.freeCompilerArgs = listOf("-Xjsr305=strict", "-Xcontext-receivers")
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass = "me.kuku.api.ApiApplicationKt"
}