package cz.cvut.fit.atlasest.service

import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

class JsonService {
    private val json = Json { prettyPrint = true }

    fun readJsonFile(fileName: String) = json.parseToJsonElement(File(fileName).readText())

    fun saveJsonFile(
        fileName: String,
        data: JsonObject,
    ) = File(fileName).writeText(json.encodeToString(data))
}

fun JsonObject.add(
    key: String,
    value: JsonElement,
): JsonObject {
    val map = this.toMutableMap()
    map[key] = value
    return JsonObject(map)
}

fun JsonArray.add(elem: JsonElement): JsonArray = JsonArray(this.toList() + elem)
