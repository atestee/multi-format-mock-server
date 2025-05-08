package cz.cvut.fit.atlasest.application

import io.ktor.server.application.Application
import io.ktor.server.application.host
import io.ktor.server.application.port

/**
 * Data class holding configuration data
 */
data class AppConfig(
    val port: Int,
    val host: String,
    val rootPath: String,
    val collectionsFilename: String,
    val identifiersFileName: String,
    val schemaFilename: String?,
    val defaultLimit: Int,
    val defaultIdentifier: String,
)

/**
 * Initialized [AppConfig]
 */
fun Application.loadAppConfig(): AppConfig {
    val schemaFilename = System.getProperty("schema")
    val defaultPaginationLimit = System.getProperty("defaultPaginationLimit")
    val defaultIdentifier = System.getProperty("defaultIdentifier")
    val config = environment.config
    return AppConfig(
        port = config.port,
        host = config.host,
        rootPath = config.property("ktor.deployment.rootPath").getString(),
        collectionsFilename = config.property("data.collectionsFilename").getString(),
        identifiersFileName = config.property("data.identifiersFilename").getString(),
        schemaFilename = schemaFilename,
        defaultLimit = defaultPaginationLimit?.toIntOrNull() ?: 10,
        defaultIdentifier = defaultIdentifier ?: "id",
    )
}
