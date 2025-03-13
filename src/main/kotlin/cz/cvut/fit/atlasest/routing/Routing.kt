package cz.cvut.fit.atlasest.routing

import cz.cvut.fit.atlasest.data.Repository
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
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import org.koin.ktor.ext.inject

fun Application.configureRouting() {
    install(ContentNegotiation) {
        json(
            Json {
                prettyPrint = true
                isLenient = true
            },
        )
    }

    val repository by inject<Repository>()

    routing {
        get {
            call.respondText("Hello World!")
        }
        get("/collections") {
            call.respondText("collections: ${repository.collections.keys}")
        }
        repository.collections.keys.forEach { collectionName ->
            get("/$collectionName") {
                val data = repository.getCollection(collectionName)
                call.respond(data)
            }
            get("/$collectionName/{id}") {
                val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing ID")
                val item = repository.getItemById(collectionName, id)
                call.respond(item)
            }
            post("/$collectionName") {
                val data = call.receive<JsonElement>()
                if (data is JsonObject) {
                    val item = repository.insertItemToCollection(collectionName, data)
                    call.response.headers.append("Location", "/$collectionName/${item.identifier}")
                    call.respond(HttpStatusCode.Created, item)
                } else {
                    call.respond(HttpStatusCode.BadRequest, "Invalid JSON")
                }
            }
            put("/$collectionName/{id}") {
                val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest, "Missing ID")
                val body = call.receive<JsonObject>()
                val itemExists = kotlin.runCatching { repository.getItemById(collectionName, id) }.isSuccess
                if (itemExists) {
                    val item = repository.updateItemInCollection(collectionName, id, body)
                    call.respond(HttpStatusCode.OK, item)
                } else {
                    val item = repository.insertItemToCollection(collectionName, body)
                    call.response.headers.append("Location", "/$collectionName/${item.identifier}")
                    call.respond(HttpStatusCode.Created, item)
                }
            }
            delete("/$collectionName/{id}") {
                val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest, "Missing ID")
                repository.deleteItemFromCollection(collectionName, id)
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}
