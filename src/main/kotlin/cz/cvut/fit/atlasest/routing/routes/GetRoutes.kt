package cz.cvut.fit.atlasest.routing.routes

import com.cesarferreira.pluralize.singularize
import cz.cvut.fit.atlasest.routing.ALL_MIME
import cz.cvut.fit.atlasest.routing.returnResourceInAcceptedFormat
import cz.cvut.fit.atlasest.service.CollectionService
import cz.cvut.fit.atlasest.service.EMBED
import cz.cvut.fit.atlasest.service.EXPAND
import cz.cvut.fit.atlasest.service.LIMIT
import cz.cvut.fit.atlasest.service.ORDER
import cz.cvut.fit.atlasest.service.PAGE
import cz.cvut.fit.atlasest.service.ParameterService
import cz.cvut.fit.atlasest.service.QUERY
import cz.cvut.fit.atlasest.service.SORT
import cz.cvut.fit.atlasest.utils.add
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
    host: String,
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
        var (data, schemas) =
            if (params[EMBED] is List<String> || params[EXPAND] is List<String>) {
                val data = parameterService.applyEmbedAndExpand(params, collectionName, collectionService)
                val schemas = parameterService.getEmbedAndExpandCollectionSchemas(params, collectionService)
                data to schemas
            } else {
                collectionService.getCollection(collectionName) to JsonObject(mapOf())
            }
        val queriedData = parameterService.applyQuerySearch(data, params)
        val schema = collectionService.getCollectionSchema(collectionName)
        schemas = schemas.add(collectionName, schema)
        val filteredData = parameterService.applyFilter(collectionName, queriedData, params, schemas)
        val (paginatedData, links) = parameterService.applyPagination(filteredData, params, "$host/$collectionName", filteredData.size)
        val sortedData = parameterService.applySorting(paginatedData, params)
        if (links is String) call.response.headers.append("Link", links)
        returnResourceInAcceptedFormat(call, HttpStatusCode.OK, JsonArray(sortedData), accept)
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
                parameterService.applyEmbedAndExpand(params, collectionName, id, collectionService)
            } else {
                collectionService.getItemById(collectionName, id)
            }
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
