package cz.cvut.fit.atlasest.routing

import com.cesarferreira.pluralize.singularize
import cz.cvut.fit.atlasest.application.AppConfig
import cz.cvut.fit.atlasest.routing.routes.deleteRoute
import cz.cvut.fit.atlasest.routing.routes.getRoutes
import cz.cvut.fit.atlasest.routing.routes.postRoute
import cz.cvut.fit.atlasest.routing.routes.putRoute
import cz.cvut.fit.atlasest.services.CollectionService
import cz.cvut.fit.atlasest.services.ParameterService
import cz.cvut.fit.atlasest.services.SchemaService
import io.github.smiley4.ktoropenapi.OpenApi
import io.github.smiley4.ktoropenapi.openApi
import io.github.smiley4.ktorswaggerui.swaggerUI
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.XML
import kotlinx.serialization.json.Json
import org.koin.ktor.ext.inject

fun Application.configureRouting() {
    val collectionService by inject<CollectionService>()
    val schemaService by inject<SchemaService>()
    val parameterService by inject<ParameterService>()
    val appConfig by inject<AppConfig>()

    install(ContentNegotiation) {
        json(
            Json {
                prettyPrint = true
                isLenient = true
            },
        )
    }

    install(OpenApi) {
        info {
            title = "Multi Format Mock Server API"
            version = "0.0.1"
        }
        server {
            url = appConfig.rootPath
        }
        rootPath = ""
        schemas {
            collectionService.getCollectionNames().forEach { collectionName ->
                schema(
                    collectionName.singularize(),
                    collectionService.getOpenApiSchema(collectionName).apply {
                        xml =
                            XML().apply {
                                name = "item"
                            }
                    },
                )
                schema(
                    collectionName,
                    ArraySchema().apply {
                        items =
                            Schema<Any>().apply {
                                `$ref` = "#/components/schemas/${collectionName.singularize()}"
                            }
                        xml =
                            XML().apply {
                                name = "items"
                                wrapped = true
                            }
                    },
                )
            }
        }
    }

    routing {
        route("openapi.json") {
            openApi()
        }
        route("swagger-ui") {
            swaggerUI("${appConfig.rootPath}/openapi.json")
        }
        val collectionNames = collectionService.getCollectionNames()
        get("/collections") {
            call.respondText("collections: $collectionNames")
        }
        collectionNames.forEach { collectionName ->
            getRoutes(collectionService, collectionName, parameterService)
            postRoute(collectionService, collectionName, schemaService)
            putRoute(collectionService, collectionName, schemaService)
            deleteRoute(collectionService, collectionName)
        }
    }
}
