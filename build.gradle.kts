val kotlinVersion: String by project
val logbackVersion: String by project
val ktorVersion: String by project
val jacksonVersion: String by project

plugins {
    kotlin("jvm") version "2.1.10"
    id("io.ktor.plugin") version "3.1.0"
    kotlin("plugin.serialization") version "2.1.10"
    id("jacoco")
}

jacoco {
    toolVersion = "0.8.12"
    reportsDirectory.set(layout.buildDirectory.dir("reportsJaCoCo"))
}

group = "cz.cvut.fit.atlasest"
version = "0.0.1"

application {
    mainClass = "cz.cvut.fit.atlasest.application.ApplicationKt"

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
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
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:$jacksonVersion")
    implementation("com.jsoizo:kotlin-csv:1.10.0")
    implementation("com.fasterxml.jackson.module:jackson-module-jsonSchema:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("org.json:json:20250107")
    implementation("com.github.wnameless.json:json-flattener:0.17.2")

    // Testing dependencies for Ktor
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")

    // Kotlin test with JUnit5 integration
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:$kotlinVersion")

    // JSON Schema
    implementation("com.github.saasquatch:json-schema-inferrer:0.2.1") // For inferring schema
    implementation("commons-validator:commons-validator:1.9.0") // For inferring type formats in schema
    implementation("io.github.optimumcode:json-schema-validator:0.4.0") // For validation using schema

    // OpenAPI and Swagger UI
    implementation("io.github.smiley4:ktor-openapi:5.0.1") // For generating OpenAPI specification
    implementation("io.github.smiley4:ktor-swagger-ui:5.0.1") // For serving SwaggerUI
    implementation("io.swagger.parser.v3:swagger-parser:2.1.25") // For working with OpenAPI schema

    // Command-line args
    implementation("commons-cli:commons-cli:1.9.0")

    // Mocking in tests
    testImplementation("io.mockk:mockk:1.13.17")
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)

    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}
