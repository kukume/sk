plugins {
    val kotlinVersion = "1.9.21"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.spring") version kotlinVersion
    id("org.springframework.boot") version "3.2.0"
    id("io.spring.dependency-management") version "1.1.0"
    application
}

group = "me.kuku"
version = "1.0-SNAPSHOT"

repositories {
//    mavenLocal()
    maven("https://nexus.kuku.me/repository/maven-public/")
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("me.kuku:ktor-spring-boot-starter:2.3.6.1")
    implementation("me.kuku:utils:2.3.6.0")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    implementation("org.jsoup:jsoup:1.15.3")
    implementation("com.microsoft.playwright:playwright:1.40.0")
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