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
    val isTest: Boolean,
    val defaultLimit: Int,
)

fun Application.loadAppConfig(): AppConfig {
    val config = environment.config
    return AppConfig(
        port = config.port,
        host = config.host,
        rootPath = config.property("ktor.deployment.rootPath").getString(),
        collectionsFilename = config.property("data.fileName").getString(),
        identifiersFileName = config.property("data.identifiersFileName").getString(),
        isTest = false,
        defaultLimit = config.propertyOrNull("pagination.defaultLimit")?.getString()?.toIntOrNull() ?: 10,
    )
}
