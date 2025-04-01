package cz.cvut.fit.atlasest.routing

import cz.cvut.fit.atlasest.application.AppConfig
import cz.cvut.fit.atlasest.routing.routes.deleteRoute
import cz.cvut.fit.atlasest.routing.routes.getRoutes
import cz.cvut.fit.atlasest.routing.routes.postRoute
import cz.cvut.fit.atlasest.routing.routes.putRoute
import cz.cvut.fit.atlasest.service.CollectionService
import cz.cvut.fit.atlasest.service.SchemaService
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
            collectionService.collections.keys.forEach { collectionName ->
                schema("error", getErrorSchema())
                schema(collectionName.removeSuffix("s"), collectionService.getOpenApiSchema(collectionName))
                schema(
                    collectionName,
                    ArraySchema().apply {
                        items =
                            Schema<Any>().apply {
                                `$ref` = "#/components/schemas/${collectionName.removeSuffix("s")}"
                            }
                        xml =
                            XML().apply {
                                name = collectionName
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
        get("/collections") {
            call.respondText("collections: ${collectionService.collections.keys}")
        }
        collectionService.collections.keys.forEach { collectionName ->
            getRoutes(collectionService, collectionName)
            postRoute(collectionService, collectionName, schemaService)
            putRoute(collectionService, collectionName, schemaService)
            deleteRoute(collectionService, collectionName)
        }
    }
}

fun getErrorSchema(): Schema<Any> {
    val openApiSchema = Schema<Any>()
    openApiSchema.addType("object")
    val properties = mutableMapOf<String, Schema<Any>>()
    properties["code"] =
        Schema<Any>().apply {
            this.addType("string")
            this.title = "Status code"
            this.example("400")
        }
    properties["message"] =
        Schema<Any>().apply {
            this.addType("string")
            this.title = "Error message"
            this.example("Bad Request")
        }
    openApiSchema.properties = properties
    return openApiSchema
}
