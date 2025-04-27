import cz.cvut.fit.atlasest.application.AppConfig
import cz.cvut.fit.atlasest.di.appModule
import cz.cvut.fit.atlasest.exceptionHandling.configureExceptionHandling
import cz.cvut.fit.atlasest.routing.configureRouting
import cz.cvut.fit.atlasest.services.CollectionService
import io.ktor.server.application.host
import io.ktor.server.application.port
import io.ktor.server.config.yaml.YamlConfigLoader
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.AfterEach
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest
import org.koin.test.inject

open class BaseTest : KoinTest {
    private val appConfig: AppConfig = loadConfig()
    val collectionService by inject<CollectionService>()

    init {
        startKoin {
            modules(appModule(appConfig))
        }
    }

    @AfterEach
    fun afterEach() {
        stopKoin()
    }

    private fun loadConfig(): AppConfig {
        val config =
            YamlConfigLoader().load("application-test.yaml")
                ?: throw NullPointerException("Test YAML config loading failed")
        return AppConfig(
            host = config.host,
            port = config.port,
            rootPath = config.property("ktor.deployment.rootPath").getString(),
            collectionsFilename = config.property("data.collectionsFileName").getString(),
            identifiersFileName = config.property("data.identifiersFileName").getString(),
            schemaFilename = null,
            isTest = true,
            defaultLimit = 10,
        )
    }

    protected fun testWithApp(testBlock: suspend ApplicationTestBuilder.() -> Unit) =
        testApplication {
            application {
                configureExceptionHandling()
                configureRouting()
            }
            testBlock()
        }
}
