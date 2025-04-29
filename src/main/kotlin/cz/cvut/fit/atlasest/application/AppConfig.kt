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
    val defaultLimit: Int,
    val isTest: Boolean,
)

fun Application.loadAppConfig(): AppConfig {
    val schemaFilename = System.getProperty("schema")
    val paginationLimit = System.getProperty("paginationLimit")
    val config = environment.config
    return AppConfig(
        port = config.port,
        host = config.host,
        rootPath = config.property("ktor.deployment.rootPath").getString(),
        collectionsFilename = config.property("data.collectionsFilename").getString(),
        identifiersFileName = config.property("data.identifiersFilename").getString(),
        schemaFilename = schemaFilename,
        isTest = false,
        defaultLimit = paginationLimit?.toIntOrNull() ?: 10,
    )
}
