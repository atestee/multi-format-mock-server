package cz.cvut.fit.atlasest.routing.routes

import cz.cvut.fit.atlasest.routing.ALL_MIME
import cz.cvut.fit.atlasest.routing.returnResourceInAcceptedFormat
import cz.cvut.fit.atlasest.service.CollectionService
import io.github.smiley4.ktoropenapi.config.descriptors.ref
import io.github.smiley4.ktoropenapi.get
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import kotlinx.serialization.json.JsonArray

fun Route.getRoutes(
    collectionService: CollectionService,
    collectionName: String,
) {
    // GET collection
    get("/$collectionName", {
        tags(collectionName)
        response {
            code(HttpStatusCode.OK) {
                body(ref(collectionName)) {
                    mediaTypes(ContentType.Application.Json, ContentType.Application.Xml, ContentType.Text.CSV)
                }
            }
        }
    }) {
        val accept = call.request.headers["Accept"] ?: ALL_MIME
        val data = collectionService.getCollection(collectionName)
        returnResourceInAcceptedFormat(call, HttpStatusCode.OK, JsonArray(data), accept)
    }

    // GET collections item
    get("/$collectionName/{id}", {
        tags(collectionName)
        request {
            pathParameter<String>("id")
        }
        response {
            code(HttpStatusCode.OK) {
                body(ref(collectionName.removeSuffix("s"))) {
                    mediaTypes(ContentType.Application.Json, ContentType.Application.Xml, ContentType.Text.CSV)
                }
            }
            code(HttpStatusCode.NotFound) {
                description = "Not Found"
            }
        }
    }) {
        val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing ID")
        val accept = call.request.headers["Accept"] ?: ALL_MIME
        val data = collectionService.getItemById(collectionName, id)
        returnResourceInAcceptedFormat(call, HttpStatusCode.OK, data, accept)
    }

    // GET collection schema
    get("/$collectionName/schema", {
        tags(collectionName)
        response {
            code(HttpStatusCode.OK) {
                description = "Returns the JSON schema for this collection"
            }
        }
    }) {
        val data = collectionService.getCollectionSchema(collectionName)
        call.respond(data)
    }
}
