package cz.cvut.fit.atlasest.application

import io.ktor.server.application.Application
import io.ktor.server.application.host
import io.ktor.server.application.port

data class AppConfig(
    val port: Int,
    val host: String,
    val rootPath: String,
    val collectionsFilename: String,
    val identifiersFileName: String,
    val schemaFilename: String?,
    val isTest: Boolean,
    val defaultLimit: Int,
)

fun Application.loadAppConfig(schemaFilename: String?): AppConfig {
    val config = environment.config
    return AppConfig(
        port = config.port,
        host = config.host,
        rootPath = config.property("ktor.deployment.rootPath").getString(),
        collectionsFilename = config.property("data.collectionsFileName").getString(),
        identifiersFileName = config.property("data.identifiersFileName").getString(),
        schemaFilename = schemaFilename,
        isTest = false,
        defaultLimit = config.propertyOrNull("pagination.defaultLimit")?.getString()?.toIntOrNull() ?: 10,
    )
}
