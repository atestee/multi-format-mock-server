package cz.cvut.fit.atlasest.routing.collectionRoutes

import com.cesarferreira.pluralize.singularize
import cz.cvut.fit.atlasest.routing.linkHeader
import cz.cvut.fit.atlasest.routing.varyHeader
import cz.cvut.fit.atlasest.services.ALL_MIME
import cz.cvut.fit.atlasest.services.CollectionService
import cz.cvut.fit.atlasest.services.ContentNegotiationService
import cz.cvut.fit.atlasest.services.EMBED
import cz.cvut.fit.atlasest.services.EXPAND
import cz.cvut.fit.atlasest.services.LIMIT
import cz.cvut.fit.atlasest.services.ORDER
import cz.cvut.fit.atlasest.services.PAGE
import cz.cvut.fit.atlasest.services.ParameterService
import cz.cvut.fit.atlasest.services.QUERY
import cz.cvut.fit.atlasest.services.SORT
import io.github.smiley4.ktoropenapi.config.descriptors.ref
import io.github.smiley4.ktoropenapi.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.util.toMap
import io.swagger.v3.oas.models.parameters.Parameter
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

fun Route.getRoutes(
    collectionName: String,
    collectionService: CollectionService,
    contentNegotiationService: ContentNegotiationService,
    parameterService: ParameterService,
) {
    // GET collection
    get("/$collectionName", {
        tags(collectionName)
        request {
            listOf(PAGE, LIMIT, SORT, ORDER, EMBED, EXPAND, QUERY).forEach { parameter ->
                queryParameter(
                    parameter,
                    ref("string"),
                )
            }
            queryParameter<JsonObject>("filter") {
                explode = true
                style = Parameter.StyleEnum.FORM
            }
        }
        response {
            code(HttpStatusCode.OK) {
                description = "The retrieved collection items"
                body(ref(collectionName)) {
                    mediaTypes(contentNegotiationService.supportedMediaTypes)
                }
                header(linkHeader.header, linkHeader.schema) {
                    description = linkHeader.description
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
        }
    }) {
        val accept = call.request.headers[HttpHeaders.Accept] ?: ALL_MIME
        val params = call.request.queryParameters.toMap()
        call.response.headers.append(HttpHeaders.Vary, HttpHeaders.Accept)
        if (params.isEmpty()) {
            val data = collectionService.getCollection(collectionName)
            val (body, type) = contentNegotiationService.getResourceInAcceptedFormat(JsonArray(data), accept)
            call.response.headers.append(HttpHeaders.ContentType, type)
            call.respond(HttpStatusCode.OK, body)
        } else {
            val (data, links) = parameterService.getCollectionWithParams(collectionName, params)
            if (links is String) call.response.headers.append(HttpHeaders.Link, links)
            val (body, type) = contentNegotiationService.getResourceInAcceptedFormat(JsonArray(data), accept)
            call.response.headers.append(HttpHeaders.ContentType, type)
            call.respond(HttpStatusCode.OK, body)
        }
    }

    // GET collections item
    get("/$collectionName/{id}", {
        tags(collectionName)
        request {
            pathParameter<String>("id")
            queryParameter<String>(EMBED)
            queryParameter<String>(EXPAND)
        }
        response {
            code(HttpStatusCode.OK) {
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
            code(HttpStatusCode.NotFound) {
                description = "Not Found"
            }
        }
    }) {
        val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing ID")
        val accept = call.request.headers[HttpHeaders.Accept] ?: ALL_MIME
        val params = call.request.queryParameters.toMap()
        val data =
            if (params[EMBED] is List<String> || params[EXPAND] is List<String>) {
                parameterService.applyEmbedAndExpand(params, collectionName, id)
            } else {
                collectionService.getItemById(collectionName, id)
            }
        val (body, type) = contentNegotiationService.getResourceInAcceptedFormat(data, accept)
        call.response.headers.append(HttpHeaders.Vary, HttpHeaders.Accept)
        call.response.headers.append(HttpHeaders.ContentType, type)
        call.respond(HttpStatusCode.OK, body)
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
