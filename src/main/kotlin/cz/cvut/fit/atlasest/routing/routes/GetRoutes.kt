package cz.cvut.fit.atlasest.routing.routes

import com.cesarferreira.pluralize.singularize
import cz.cvut.fit.atlasest.services.CollectionService
import cz.cvut.fit.atlasest.services.EMBED
import cz.cvut.fit.atlasest.services.EXPAND
import cz.cvut.fit.atlasest.services.LIMIT
import cz.cvut.fit.atlasest.services.ORDER
import cz.cvut.fit.atlasest.services.PAGE
import cz.cvut.fit.atlasest.services.ParameterService
import cz.cvut.fit.atlasest.services.QUERY
import cz.cvut.fit.atlasest.services.SORT
import cz.cvut.fit.atlasest.utils.ALL_MIME
import cz.cvut.fit.atlasest.utils.returnResourceInAcceptedFormat
import io.github.smiley4.ktoropenapi.config.descriptors.ref
import io.github.smiley4.ktoropenapi.get
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.util.toMap
import io.swagger.v3.oas.models.parameters.Parameter
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

fun Route.getRoutes(
    collectionService: CollectionService,
    collectionName: String,
    parameterService: ParameterService,
) {
    val queryParams = listOf(PAGE, LIMIT, SORT, ORDER, EMBED, EXPAND, QUERY)

    // GET collection
    get("/$collectionName", {
        tags(collectionName)
        request {
            queryParams.forEach { parameter ->
                queryParameter<String>(parameter)
            }
            queryParameter<JsonObject>("filter") {
                explode = true
                style = Parameter.StyleEnum.FORM
            }
        }
        response {
            code(HttpStatusCode.OK) {
                body(ref(collectionName)) {
                    mediaTypes(ContentType.Application.Json, ContentType.Application.Xml, ContentType.Text.CSV)
                }
            }
        }
    }) {
        val accept = call.request.headers["Accept"] ?: ALL_MIME
        val params = call.request.queryParameters.toMap()
        if (params.isEmpty()) {
            val data = collectionService.getCollectionItems(collectionName)
            val (body, type) = returnResourceInAcceptedFormat(JsonArray(data), accept)
            call.response.headers.append("Content-Type", type)
            call.respond(HttpStatusCode.OK, body)
        } else {
            val (data, links) = parameterService.getCollectionItemWithParams(collectionName, params)
            if (links is String) call.response.headers.append("Link", links)
            val (body, type) = returnResourceInAcceptedFormat(JsonArray(data), accept)
            call.response.headers.append("Content-Type", type)
            call.respond(HttpStatusCode.OK, body)
        }
    }

    // GET collections item
    get("/$collectionName/{id}", {
        tags(collectionName)
        request {
            pathParameter<String>("id")
            queryParams.forEach { parameter ->
                queryParameter<String>(parameter)
            }
            queryParameter<JsonObject>("filter") {
                explode = true
                style = Parameter.StyleEnum.FORM
            }
        }
        response {
            code(HttpStatusCode.OK) {
                body(ref(collectionName.singularize())) {
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
        val params = call.request.queryParameters.toMap()
        val data =
            if (params[EMBED] is List<String> || params[EXPAND] is List<String>) {
                parameterService.applyEmbedAndExpand(params, collectionName, id)
            } else {
                collectionService.getItemById(collectionName, id)
            }
        val (body, type) = returnResourceInAcceptedFormat(data, accept)
        call.response.headers.append("Content-Type", type)
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
