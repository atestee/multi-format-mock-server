package cz.cvut.fit.atlasest.utils

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import com.github.wnameless.json.flattener.JsonFlattener
import com.github.wnameless.json.unflattener.JsonUnflattener
import io.ktor.server.plugins.BadRequestException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.json.JSONArray
import org.json.JSONObject
import org.json.XML

val objectMapper = ObjectMapper()
val xmlMapper = XmlMapper()

/**
 * Converts a [JsonElement] to an XML string.
 *
 * @throws BadRequestException if the JSON is not a [JsonObject] or a [JsonArray] of [JsonObject]
 * @return The XML representation of the JSON
 */
fun JsonElement.toXML(): String =
    when (this) {
        is JsonObject -> {
            this.toXML()
        }

        is JsonArray -> {
            if (this.all { it is JsonObject }) {
                this.toXML()
            } else {
                throw BadRequestException("Invalid JSON body, should either be object or array of object")
            }
        }

        else -> {
            throw BadRequestException("Invalid JSON body, should either be object or array of object")
        }
    }

/**
 * Converts an XML string to a [JsonObject]
 *
 * @param xmlString The XML content as a string.
 * @return A [JsonObject] representation of the XML.
 */
fun xmlToJson(xmlString: String): JsonObject {
    val node: JsonNode = xmlMapper.readTree(xmlString)
    val json = objectMapper.writeValueAsString(node)
    return json.toJsonObject()
}

/**
 * Converts a CSV string into a [JsonObject]
 *
 * This function assumes that the CSV contains only one row of data
 *
 * @param csvString The CSV content as a string
 * @return A [JsonObject] representation of the CSV data
 */
fun csvToJson(csvString: String): JsonObject {
    val csvData =
        csvReader {
            this.delimiter = ';'
        }.readAllWithHeader(csvString)
    val json = JsonUnflattener.unflattenAsMap(csvData.first()) as Map<String, Any>
    return objectMapper.writeValueAsString(json).toJsonObject()
}

/**
 * Converts a [JsonObject] or [JsonArray] into a CSV string
 *
 * @throws BadRequestException if the input is not a [JsonObject] or [JsonArray]
 * @return The CSV representation of the JSON
 */
fun JsonElement.toCSV(): String =
    when (this) {
        is JsonObject -> jsonItemToCsv(this.toString())
        is JsonArray -> jsonArrayToCsv(this.map { it.jsonObject.toString() })
        else -> throw BadRequestException("Invalid request body. It should be JSON object or array.")
    }

/**
 * Converts a list of JSON objects (as strings) into a CSV format
 *
 * @param jsonArray A list of JSON objects represented as strings
 * @return The CSV representation of the JSON array
 */
fun jsonArrayToCsv(jsonArray: List<String>): String {
    val csv = mutableListOf<List<Any>>()
    jsonArray.forEachIndexed { index, json ->
        val flattened = JsonFlattener.flattenAsMap(json) as Map<String, Any>
        if (index == 0) csv.add(flattened.keys.toList())
        csv.add(flattened.values.toList())
    }
    return csvWriter {
        this.delimiter = ';'
    }.writeAllAsString(csv)
}

/**
 * Converts a single JSON object (as a string) into CSV format
 *
 * @param jsonString A JSON object as a string
 * @return The CSV representation of the JSON object
 */
fun jsonItemToCsv(jsonString: String): String {
    val flattened = JsonFlattener.flattenAsMap(jsonString) as Map<String, Any>
    return csvWriter {
        this.delimiter = ';'
    }.writeAllAsString(listOf(flattened.keys.toList(), flattened.values.toList()))
}

/**
 * Converts a [JsonObject] to an XML string
 *
 * @return The XML representation of the [JsonObject]
 */
private fun JsonObject.toXML(): String {
    val json = JSONObject(this.toString())
    val item = XML.toString(json)
    val itemWrapped = XML.toString(item, "item")
    return XML.unescape(itemWrapped)
}

/**
 * Converts a [JsonArray] to an XML string
 *
 * @return The XML representation of the [JsonArray]
 */
private fun JsonArray.toXML(): String {
    val json = JSONArray(this.toString())
    val item = XML.toString(json, "item")
    val xml = XML.toString(item, "items")
    return XML.unescape(xml)
}
