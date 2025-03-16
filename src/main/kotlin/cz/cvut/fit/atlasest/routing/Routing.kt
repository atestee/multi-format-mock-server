package cz.cvut.fit.atlasest.routing

import cz.cvut.fit.atlasest.application.AppConfig
import cz.cvut.fit.atlasest.service.CollectionService
import io.github.smiley4.ktoropenapi.OpenApi
import io.github.smiley4.ktoropenapi.config.descriptors.ref
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.openApi
import io.github.smiley4.ktoropenapi.post
import io.github.smiley4.ktorswaggerui.swaggerUI
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.Schema
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import org.koin.ktor.ext.inject

fun Application.configureRouting() {
    val collectionService by inject<CollectionService>()
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
                val schema = collectionService.getOpenApiSchema(collectionName)
                schema(collectionName.removeSuffix("s"), schema)
                val schemaArray =
                    ArraySchema().apply {
                        items =
                            Schema<Any>().apply {
                                `$ref` = "#/components/schemas/${collectionName.removeSuffix("s")}"
                            }
                    }
                schema(collectionName, schemaArray)
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
            get("/$collectionName", {
                response {
                    code(HttpStatusCode.OK) {
                        body(ref(collectionName)) {
                            mediaTypes(ContentType.Application.Json)
                        }
                    }
                }
            }) {
                val data = collectionService.getCollection(collectionName)
                call.respond(data)
            }
            get("/$collectionName/schema") {
                val data = collectionService.getCollectionSchema(collectionName)
                call.respond(data)
            }
            get("/$collectionName/{id}", {
                request {
                    pathParameter<String>("id")
                }
                response {
                    code(HttpStatusCode.OK) {
                        body(ref(collectionName.removeSuffix("s"))) {
                            mediaTypes(ContentType.Application.Json)
                        }
                    }
                    code(HttpStatusCode.BadRequest) {
                        description = "Missing id path parameter"
                    }
                }
            }) {
                val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing ID")
                val item = collectionService.getItemById(collectionName, id)
                call.respond(item)
            }
            post("/$collectionName", {
                request {
                    pathParameter<String>("id")
                    body(ref(collectionName.removeSuffix("s"))) {
                        mediaTypes(ContentType.Application.Json)
                    }
                }
                response {
                    code(HttpStatusCode.Created) {
                        body(ref(collectionName.removeSuffix("s")))
                    }
                }
            }) {
                val data = call.receive<JsonElement>()
                if (data is JsonObject) {
                    val item = collectionService.insertItemToCollection(collectionName, data)
                    call.response.headers.append("Location", "/$collectionName/${item.identifier}")
                    call.respond(HttpStatusCode.Created, item)
                } else {
                    call.respond(HttpStatusCode.BadRequest, "Invalid JSON")
                }
            }
            put("/$collectionName/{id}") {
                val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest, "Missing ID")
                val body = call.receive<JsonObject>()
                val itemExists = kotlin.runCatching { collectionService.getItemById(collectionName, id) }.isSuccess
                if (itemExists) {
                    val item = collectionService.updateItemInCollection(collectionName, id, body)
                    call.respond(HttpStatusCode.OK, item)
                } else {
                    val item = collectionService.insertItemToCollection(collectionName, body)
                    call.response.headers.append("Location", "/$collectionName/${item.identifier}")
                    call.respond(HttpStatusCode.Created, item)
                }
            }
            delete("/$collectionName/{id}") {
                val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest, "Missing ID")
                collectionService.deleteItemFromCollection(collectionName, id)
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}
