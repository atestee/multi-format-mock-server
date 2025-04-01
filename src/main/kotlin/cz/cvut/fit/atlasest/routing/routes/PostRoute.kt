package cz.cvut.fit.atlasest.routing.routes

import cz.cvut.fit.atlasest.routing.ALL_MIME
import cz.cvut.fit.atlasest.routing.getResourceInJsonFormat
import cz.cvut.fit.atlasest.routing.returnResourceInAcceptedFormat
import cz.cvut.fit.atlasest.service.CollectionService
import cz.cvut.fit.atlasest.service.SchemaService
import io.github.smiley4.ktoropenapi.config.descriptors.ref
import io.github.smiley4.ktoropenapi.post
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.contentType
import io.ktor.server.request.receiveText
import io.ktor.server.routing.Route

fun Route.postRoute(
    collectionService: CollectionService,
    collectionName: String,
    schemaService: SchemaService,
) {
    post("/$collectionName", {
        tags(collectionName)
        request {
            body(ref(collectionName.removeSuffix("s"))) {
                mediaTypes(ContentType.Application.Json, ContentType.Application.Xml, ContentType.Text.CSV)
            }
        }
        response {
            code(HttpStatusCode.Created) {
                body(ref(collectionName.removeSuffix("s")), {
                    mediaTypes(ContentType.Application.Json, ContentType.Application.Xml, ContentType.Text.CSV)
                })
                header<String>("Content-Type") {
                    description = "The media type of the resource being sent"
                    required = true
                }
                header<String>("Location") {
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
        val accept = call.request.headers["Accept"] ?: ALL_MIME

        val schema = collectionService.getCollectionSchema(collectionName)
        val jsonItem =
            getResourceInJsonFormat(
                call.receiveText(),
                contentType,
            ) { json -> schemaService.convertTypes(jsonSchema = schema, jsonObject = json) }
        val insertedItem = collectionService.insertItemToCollection(collectionName, jsonItem)
        call.response.headers.append("Location", "/$collectionName/${insertedItem.identifier}")
        returnResourceInAcceptedFormat(call, HttpStatusCode.Created, insertedItem.data, accept)
    }
}
