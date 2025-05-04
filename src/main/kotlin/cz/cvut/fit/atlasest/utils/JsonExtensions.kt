package cz.cvut.fit.atlasest.utils

import com.nfeld.jsonpathkt.kotlinx.resolvePathOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

/**
 * Adds a new key-value pair to the [JsonObject], returning a new [JsonObject] with the added pair.
 *
 * @param key The key to add to the JSON object.
 * @param value The value to associate with the key.
 *
 * @return A new [JsonObject] with the added key-value pair.
 */
fun JsonObject.add(
    key: String,
    value: JsonElement,
): JsonObject {
    val map = this.toMutableMap()
    map[key] = value
    return JsonObject(map)
}

/**
 * Parses a JSON string into a [JsonElement].
 *
 * @return The parsed [JsonElement] representation of the string.
 */
fun String.toJsonElement(): JsonElement = Json.parseToJsonElement(this)

/**
 * Parses a JSON string into a [JsonObject].
 *
 * @return The parsed [JsonObject] representation of the string.
 */
fun String.toJsonObject(): JsonObject = Json.parseToJsonElement(this).jsonObject

/**
 * Parses a JSON string into a [JsonArray].
 *
 * @return The parsed [JsonArray] representation of the string.
 */
fun String.toJsonArray(): JsonArray = Json.parseToJsonElement(this).jsonArray

/**
 * Retrieves the value of a specified field from the JSON object with JSON Path.
 *
 * @param key The key representing the field, supports nested keys using dot notation array keys using indexing.
 *
 * @return The corresponding JSON element, or `null` if not found.
 */
fun JsonObject.getFieldValue(key: String): JsonElement? = this.resolvePathOrNull("\$.$key")
