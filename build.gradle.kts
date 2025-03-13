val kotlinVersion: String by project
val logbackVersion: String by project
val ktorVersion: String by project

plugins {
    kotlin("jvm") version "2.1.10"
    id("io.ktor.plugin") version "3.1.0"
    kotlin("plugin.serialization") version "2.1.10"
}

group = "cz.cvut.fit.atlasest"
version = "0.0.1"

application {
    mainClass = "io.ktor.server.netty.EngineMain"

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
}

dependencies {
    // Ktor server core dependencies
    implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-config-yaml:$ktorVersion")
    testImplementation("io.ktor:ktor-server-config-yaml:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

    // Koin
    implementation("io.insert-koin:koin-ktor:4.0.2")
    implementation("io.insert-koin:koin-test:4.0.2")
    implementation("io.insert-koin:koin-test-junit5:4.0.2")

    // Logging
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("org.slf4j:slf4j-api:2.0.16")

    // Status pages
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.2")
    implementation("com.fasterxml.jackson.module:jackson-module-jsonSchema:2.18.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.2")
    implementation("com.github.erosb:everit-json-schema:1.14.5")

    // Testing dependencies for Ktor
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")

    // Kotlin test with JUnit5 integration
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:$kotlinVersion")

    // JUnit5 dependencies
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.12.0")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.12.0")
}

tasks.test {
    useJUnitPlatform()
}
