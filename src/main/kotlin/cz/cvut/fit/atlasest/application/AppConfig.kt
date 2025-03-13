package cz.cvut.fit.atlasest.application

import io.ktor.server.application.Application
import io.ktor.server.application.host
import io.ktor.server.application.port

data class AppConfig(
    val port: Int,
    val host: String,
    val rootPath: String,
    val fileName: String,
    val identifiersFileName: String,
)

fun Application.loadAppConfig(): AppConfig {
    val config = environment.config
    return AppConfig(
        port = config.port,
        host = config.host,
        rootPath = config.property("ktor.deployment.rootPath").getString(),
        fileName = config.property("data.fileName").getString(),
        identifiersFileName = config.property("data.identifiersFileName").getString(),
    )
}
