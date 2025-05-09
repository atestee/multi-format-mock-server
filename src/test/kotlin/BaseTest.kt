import cz.cvut.fit.atlasest.application.AppConfig
import cz.cvut.fit.atlasest.data.FileHandler
import cz.cvut.fit.atlasest.di.appModule
import cz.cvut.fit.atlasest.exceptionHandling.configureExceptionHandling
import cz.cvut.fit.atlasest.routing.configureRouting
import cz.cvut.fit.atlasest.services.CollectionService
import io.ktor.server.application.host
import io.ktor.server.application.port
import io.ktor.server.config.yaml.YamlConfigLoader
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.AfterEach
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest
import org.koin.test.inject
import java.io.File

open class BaseTest : KoinTest {
    val appConfig: AppConfig = loadConfig()
    val collectionService by inject<CollectionService>()
    val fileHandler by inject<FileHandler>()

    private val collectionData = readResourceFile(appConfig.collectionsFilename) as String
    private val identifierData = readResourceFile(appConfig.identifiersFileName) as String

    private val collectionsFile = File(appConfig.collectionsFilename)
    private val identifierFile = File(appConfig.identifiersFileName)

    init {
        startKoin {
            modules(appModule(appConfig))
        }
        collectionsFile.createNewFile()
        collectionsFile.deleteOnExit()
        identifierFile.createNewFile()
        identifierFile.deleteOnExit()
        collectionsFile.writeText(collectionData)
        identifierFile.writeText(identifierData)
    }

    @AfterEach
    fun afterEach() {
        stopKoin()
        collectionsFile.writeText(collectionData)
        identifierFile.writeText(identifierData)
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
            defaultLimit = 10,
            defaultIdentifier = "id",
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

    protected fun testAppWithExceptionThrows(
        exception: Throwable,
        testBlock: suspend ApplicationTestBuilder.() -> Unit,
    ) {
        testApplication {
            application {
                configureExceptionHandling()
                routing {
                    get("/test") {
                        throw exception
                    }
                }
            }
            testBlock()
        }
    }

    private fun readResourceFile(fileName: String) = javaClass.classLoader.getResource(fileName)?.readText()
}
