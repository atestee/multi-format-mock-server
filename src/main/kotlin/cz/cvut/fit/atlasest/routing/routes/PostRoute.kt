package cz.cvut.fit.atlasest.routing.routes

import com.cesarferreira.pluralize.singularize
import cz.cvut.fit.atlasest.services.ALL_MIME
import cz.cvut.fit.atlasest.services.CollectionService
import cz.cvut.fit.atlasest.services.ContentNegotiationService
import io.github.smiley4.ktoropenapi.config.descriptors.ref
import io.github.smiley4.ktoropenapi.post
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.contentType
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.Route

fun Route.postRoute(
    collectionName: String,
    collectionService: CollectionService,
    contentNegotiationService: ContentNegotiationService,
) {
    post("/$collectionName", {
        tags(collectionName)
        request {
            body(ref(collectionName.singularize())) {
                mediaTypes(ContentType.Application.Json, ContentType.Application.Xml, ContentType.Text.CSV)
            }
        }
        response {
            code(HttpStatusCode.Created) {
                body(ref(collectionName.singularize()), {
                    mediaTypes(ContentType.Application.Json, ContentType.Application.Xml, ContentType.Text.CSV)
                })
                header<String>(HttpHeaders.ContentType) {
                    description = "The media type of the resource being sent"
                    required = true
                }
                header<String>(HttpHeaders.Location) {
                    description = "The URI of the inserted item"
                    required = true
                }
            }
            code(HttpStatusCode.BadRequest) {
                description = "Bad Request"
            }
        }
    }) {
        val contentType = "${call.request.contentType().contentType}/${call.request.contentType().contentSubtype}"
        val accept = call.request.headers[HttpHeaders.Accept] ?: ALL_MIME
        val body = call.receiveText()
        val jsonItem =
            contentNegotiationService.getResourceInJsonFormat(
                collectionName,
                body,
                contentType,
            )
        val (insertedItemId, insertedItem) = collectionService.insertItemToCollection(collectionName, jsonItem)
        call.response.headers.append(HttpHeaders.Vary, HttpHeaders.Accept)
        val (bodyInAcceptedFormat, type) = contentNegotiationService.getResourceInAcceptedFormat(insertedItem, accept)
        call.response.headers.append(HttpHeaders.Location, "/$collectionName/$insertedItemId")
        call.response.headers.append(HttpHeaders.ContentType, type)
        call.respond(HttpStatusCode.Created, bodyInAcceptedFormat)
    }
}
