package cz.cvut.fit.atlasest.routing

import cz.cvut.fit.atlasest.application.AppConfig
import cz.cvut.fit.atlasest.di.appModule
import cz.cvut.fit.atlasest.exceptions.configureExceptionHandling
import io.ktor.server.application.host
import io.ktor.server.application.port
import io.ktor.server.config.yaml.YamlConfigLoader
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest

open class BaseTest : KoinTest {
    val appConfig: AppConfig = loadConfig()

    init {
        startKoin {
            modules(appModule(appConfig))
        }
    }

    private fun loadConfig(): AppConfig {
        val config =
            YamlConfigLoader().load("application-test.yaml") ?: throw NullPointerException("YAML config loading failed")
        return AppConfig(
            host = config.host,
            port = config.port,
            rootPath = config.property("ktor.deployment.rootPath").getString(),
            fileName = config.property("data.fileName").getString(),
            identifiersFileName = config.property("data.identifiersFileName").getString(),
        )
    }

    protected fun testWithApp(testBlock: suspend ApplicationTestBuilder.() -> Unit) =
        testApplication {
            application {
                configureExceptionHandling()
                configureRouting()
            }
            testBlock()
            stopKoin()
        }
}
