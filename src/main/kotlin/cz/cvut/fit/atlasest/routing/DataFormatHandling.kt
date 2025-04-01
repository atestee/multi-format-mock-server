package cz.cvut.fit.atlasest.routing

import cz.cvut.fit.atlasest.utils.csvToJson
import cz.cvut.fit.atlasest.utils.jsonToCsv
import cz.cvut.fit.atlasest.utils.toXML
import cz.cvut.fit.atlasest.utils.xmlToJson
import io.ktor.http.HttpStatusCode
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

fun getResourceInJsonFormat(
    body: String,
    contentType: String,
    convertJson: (JsonObject) -> JsonObject,
): JsonObject =
    when (contentType) {
        JSON_MIME ->
            try {
                Json.parseToJsonElement(body).jsonObject
            } catch (e: Exception) {
                throw BadRequestException("Problem when parsing JSON body (must be a JSON object)", e)
            }

        XML_MIME -> {
            convertJson(xmlToJson(body))
        }
        CSV_MIME -> {
            convertJson(csvToJson(body))
        }

        else -> throw BadRequestException(
            "Unsupported content type $contentType. Supported types are: [$JSON_MIME, $XML_MIME, $CSV_MIME]",
        )
    }

suspend fun returnResourceInAcceptedFormat(
    call: ApplicationCall,
    code: HttpStatusCode,
    resource: JsonElement,
    accept: String,
) = when (accept) {
    JSON_MIME, ALL_MIME -> {
        call.response.headers.append("Content-Type", JSON_MIME)
        call.respond(code, resource)
    }
    XML_MIME -> {
        call.response.headers.append("Content-Type", XML_MIME)
        call.respond(code, resource.toXML())
    }
    CSV_MIME -> {
        call.response.headers.append("Content-Type", CSV_MIME)
        call.respond(code, jsonToCsv(resource))
    }
    else -> throw BadRequestException(
        "Unsupported accept type $accept. Supported types are: [$JSON_MIME, $XML_MIME, $CSV_MIME]",
    )
}
