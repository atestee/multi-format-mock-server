package cz.cvut.fit.atlasest.services

import cz.cvut.fit.atlasest.exceptionHandling.NotAcceptableException
import cz.cvut.fit.atlasest.exceptionHandling.UnsupportedMediaTypeException
import cz.cvut.fit.atlasest.utils.csvToJson
import cz.cvut.fit.atlasest.utils.toCSV
import cz.cvut.fit.atlasest.utils.toXML
import cz.cvut.fit.atlasest.utils.xmlToJson
import io.ktor.http.ContentType
import io.ktor.http.parseAndSortContentTypeHeader
import io.ktor.server.plugins.BadRequestException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

class ContentNegotiationService(
    val collectionService: CollectionService,
    val schemaService: SchemaService,
) {
    val supportedTypes = listOf(ContentType.Application.Json, ContentType.Application.Xml, ContentType.Text.CSV)

    fun getResourceInJsonFormat(
        collectionName: String,
        body: String,
        contentTypeValue: String,
    ): JsonObject {
        val contentType = ContentType.parse(contentTypeValue)
        val schema = collectionService.getCollectionSchema(collectionName)
        return when (contentType) {
            ContentType.Application.Json ->
                try {
                    Json.parseToJsonElement(body).jsonObject
                } catch (e: Exception) {
                    throw BadRequestException("Problem when parsing JSON body (must be a JSON object)", e)
                }

            ContentType.Application.Xml -> {
                schemaService.convertTypes(schema, xmlToJson(body))
            }

            ContentType.Text.CSV -> {
                schemaService.convertTypes(schema, csvToJson(body))
            }

            else -> throw UnsupportedMediaTypeException(
                "Unsupported media type $contentType. Supported types are: [$JSON_MIME, $XML_MIME, $CSV_MIME]",
            )
        }
    }

    fun getResourceInAcceptedFormat(
        resource: JsonElement,
        accept: String,
    ): Pair<String, String> {
        val acceptNegotiated = processAcceptHeader(accept)
        return when (acceptNegotiated) {
            ContentType.Application.Json -> {
                resource.toString() to JSON_MIME
            }
            ContentType.Application.Xml -> {
                resource.toXML() to XML_MIME
            }
            ContentType.Text.CSV -> {
                resource.toCSV() to CSV_MIME
            }
            else -> throw NotAcceptableException(
                "Supported types are: [$JSON_MIME, $XML_MIME, $CSV_MIME]",
            )
        }
    }

    internal fun processAcceptHeader(accept: String): ContentType? {
        if (accept.isBlank()) {
            return supportedTypes.first()
        }
        val acceptParsed = parseAndSortContentTypeHeader(accept)

        val (disallowedTypes, allowedTypes) =
            acceptParsed
                .partition { it.quality == 0.0 }
                .let { (disallowed, allowed) ->
                    disallowed.map { ContentType.parse(it.value) } to allowed.map { ContentType.parse(it.value) }
                }
        val allowedSupportedTypes = supportedTypes.filterNot { it in disallowedTypes }

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
}
