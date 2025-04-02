package cz.cvut.fit.atlasest.routing

import cz.cvut.fit.atlasest.utils.csvToJson
import cz.cvut.fit.atlasest.utils.jsonToCsv
import cz.cvut.fit.atlasest.utils.toXML
import cz.cvut.fit.atlasest.utils.xmlToJson
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.parseAndSortContentTypeHeader
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.response.respond
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

val JSON_MIME = "application/json"
val XML_MIME = "application/xml"
val CSV_MIME = "text/csv"
val ALL_MIME = "*/*"

val SUPPORTED_TYPES = listOf(ContentType.Application.Json, ContentType.Application.Xml, ContentType.Text.CSV)

fun getResourceInJsonFormat(
    body: String,
    contentTypeValue: String,
    convertJson: (JsonObject) -> JsonObject,
): JsonObject {
    val contentType = ContentType.parse(contentTypeValue)
    return when (contentType) {
        ContentType.Application.Json ->
            try {
                Json.parseToJsonElement(body).jsonObject
            } catch (e: Exception) {
                throw BadRequestException("Problem when parsing JSON body (must be a JSON object)", e)
            }

        ContentType.Application.Xml -> {
            convertJson(xmlToJson(body))
        }

        ContentType.Text.CSV -> {
            convertJson(csvToJson(body))
        }

        else -> throw BadRequestException(
            "Unsupported content type $contentType. Supported types are: [$JSON_MIME, $XML_MIME, $CSV_MIME]",
        )
    }
}

suspend fun returnResourceInAcceptedFormat(
    call: ApplicationCall,
    code: HttpStatusCode,
    resource: JsonElement,
    accept: String,
) {
    val acceptNegotiated = negotiateContent(accept)
    when (acceptNegotiated) {
        ContentType.Application.Json -> {
            call.response.headers.append("Content-Type", JSON_MIME)
            call.respond(code, resource)
        }
        ContentType.Application.Xml -> {
            call.response.headers.append("Content-Type", XML_MIME)
            call.respond(code, resource.toXML())
        }
        ContentType.Text.CSV -> {
            call.response.headers.append("Content-Type", CSV_MIME)
            call.respond(code, jsonToCsv(resource))
        }

        else -> throw BadRequestException(
            "Unsupported accept type $accept. Supported types are: [$JSON_MIME, $XML_MIME, $CSV_MIME]",
        )
    }
}

internal fun negotiateContent(accept: String): ContentType? {
    if (accept.isBlank()) {
        return SUPPORTED_TYPES.first()
    }
    val acceptParsed = parseAndSortContentTypeHeader(accept)

    val (disallowedTypes, allowedTypes) =
        acceptParsed
            .partition { it.quality == 0.0 }
            .let { (disallowed, allowed) ->
                disallowed.map { ContentType.parse(it.value) } to allowed.map { ContentType.parse(it.value) }
            }
    val allowedSupportedTypes = SUPPORTED_TYPES.filterNot { it in disallowedTypes }

    val result =
        allowedTypes.firstNotNullOfOrNull { accepted ->
            allowedSupportedTypes.firstOrNull { supported ->
                (accepted.contentType == supported.contentType && accepted.contentSubtype == supported.contentSubtype) ||
                    (accepted.contentType == supported.contentType && accepted.contentSubtype == "*") ||
                    (accepted.contentType == "*" && accepted.contentSubtype == "*")
            }
        }

    return result
}
