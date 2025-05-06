package cz.cvut.fit.atlasest.routing.collectionRoutes

import com.cesarferreira.pluralize.singularize
import cz.cvut.fit.atlasest.routing.acceptHeader
import cz.cvut.fit.atlasest.routing.locationHeader
import cz.cvut.fit.atlasest.routing.varyHeader
import cz.cvut.fit.atlasest.services.ALL_MIME
import cz.cvut.fit.atlasest.services.CollectionService
import cz.cvut.fit.atlasest.services.ContentNegotiationService
import io.github.smiley4.ktoropenapi.config.descriptors.ref
import io.github.smiley4.ktoropenapi.put
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.contentType
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.Route

fun Route.putRoute(
    collectionName: String,
    collectionService: CollectionService,
    contentNegotiationService: ContentNegotiationService,
) {
    put("/$collectionName/{id}", {
        tags(collectionName)
        request {
            pathParameter<String>("id")
            body(ref(collectionName.singularize())) {
                mediaTypes(contentNegotiationService.supportedMediaTypes)
            }
        }
        response {
            code(HttpStatusCode.Created) {
                description = "Item was inserted into collection"
                body(ref(collectionName.singularize())) {
                    mediaTypes(contentNegotiationService.supportedMediaTypes)
                }
                header(locationHeader.header, locationHeader.schema) {
                    description = locationHeader.description
                    required = true
                }
                header(varyHeader.header, varyHeader.schema) {
                    description = varyHeader.description
                    required = true
                }
            }
            code(HttpStatusCode.OK) {
                description = "Item with id was updated"
                body(ref(collectionName.singularize())) {
                    mediaTypes(contentNegotiationService.supportedMediaTypes)
                }
                header(varyHeader.header, varyHeader.schema) {
                    description = varyHeader.description
                    required = true
                }
            }
            code(HttpStatusCode.NotAcceptable) {
                description = "Not Acceptable"
                header(varyHeader.header, varyHeader.schema) {
                    description = varyHeader.description
                    required = true
                }
            }
            code(HttpStatusCode.UnsupportedMediaType) {
                description = "Unsupported Media Type"
                header(acceptHeader.header, acceptHeader.schema) {
                    description = acceptHeader.description
                    required = true
                }
                header(varyHeader.header, varyHeader.schema) {
                    description = varyHeader.description
                    required = true
                }
            }
            code(HttpStatusCode.BadRequest) {
                description = "Bad Request"
            }
        }
    }) {
        val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest, "Missing ID")
        val contentType = "${call.request.contentType().contentType}/${call.request.contentType().contentSubtype}"
        val accept = call.request.headers[HttpHeaders.Accept] ?: ALL_MIME
        val body = call.receiveText()
        val jsonItem =
            contentNegotiationService.getResourceInJsonFormat(
                collectionName,
                body,
                contentType,
            )
        val itemExists = kotlin.runCatching { collectionService.getItemById(collectionName, id) }.isSuccess
        if (itemExists) {
            val updatedItem = collectionService.updateItemInCollection(collectionName, id, jsonItem)
            val (bodyInAcceptedFormat, type) = contentNegotiationService.getResourceInAcceptedFormat(updatedItem, accept)
            call.response.headers.append(HttpHeaders.Vary, HttpHeaders.Accept)
            call.response.headers.append(HttpHeaders.ContentType, type)
            call.respond(HttpStatusCode.OK, bodyInAcceptedFormat)
        } else {
            val (insertedItemId, insertedItem) = collectionService.insertItemToCollection(collectionName, jsonItem)
            call.response.headers.append(HttpHeaders.Vary, HttpHeaders.Accept)
            val (bodyInAcceptedFormat, type) = contentNegotiationService.getResourceInAcceptedFormat(insertedItem, accept)
            call.response.headers.append(HttpHeaders.Location, "/$collectionName/$insertedItemId")
            call.response.headers.append(HttpHeaders.ContentType, type)
            call.respond(HttpStatusCode.Created, bodyInAcceptedFormat)
        }
    }
}
