package cz.cvut.fit.atlasest.routing.routes

import cz.cvut.fit.atlasest.routing.ALL_MIME
import cz.cvut.fit.atlasest.routing.getResourceInJsonFormat
import cz.cvut.fit.atlasest.routing.returnResourceInAcceptedFormat
import cz.cvut.fit.atlasest.service.CollectionService
import cz.cvut.fit.atlasest.service.SchemaService
import io.github.smiley4.ktoropenapi.config.descriptors.ref
import io.github.smiley4.ktoropenapi.put
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.contentType
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.Route

fun Route.putRoute(
    collectionService: CollectionService,
    collectionName: String,
    schemaService: SchemaService,
) {
    put("/$collectionName/{id}", {
        tags(collectionName)
        request {
            pathParameter<String>("id")
            body(ref(collectionName.removeSuffix("s"))) {
                mediaTypes(ContentType.Application.Json, ContentType.Application.Xml)
            }
        }
        response {
            code(HttpStatusCode.Created) {
                description = "Item was inserted into collection"
                body(ref(collectionName.removeSuffix("s")))
                header<String>("Content-Type") {
                    description = "The media type of the resource being sent"
                    required = true
                }
                header<String>("Location") {
                    description = "The URI of the inserted item"
                    required = true
                }
            }
            code(HttpStatusCode.OK) {
                description = "Item with id was updated"
                body(ref(collectionName.removeSuffix("s")))
            }
            code(HttpStatusCode.BadRequest) {
                description = "Bad Request"
            }
        }
    }) {
        val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest, "Missing ID")
        val contentType = "${call.request.contentType().contentType}/${call.request.contentType().contentSubtype}"
        val accept = call.request.headers["Accept"] ?: ALL_MIME
        val schema = collectionService.getCollectionSchema(collectionName)
        val jsonItem =
            getResourceInJsonFormat(
                call.receiveText(),
                contentType,
            ) { json -> schemaService.convertTypes(jsonSchema = schema, jsonObject = json) }
        val itemExists = kotlin.runCatching { collectionService.getItemById(collectionName, id) }.isSuccess
        if (itemExists) {
            val updatedItem = collectionService.updateItemInCollection(collectionName, id, jsonItem)
            returnResourceInAcceptedFormat(call, HttpStatusCode.OK, updatedItem, accept)
        } else {
            val insertedItem = collectionService.insertItemToCollection(collectionName, jsonItem)
            call.response.headers.append("Location", "/$collectionName/${insertedItem.identifier}")
            returnResourceInAcceptedFormat(call, HttpStatusCode.Created, insertedItem.data, accept)
        }
    }
}
