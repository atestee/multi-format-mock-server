package cz.cvut.fit.atlasest.data

import cz.cvut.fit.atlasest.exceptionHandling.InvalidDataException
import cz.cvut.fit.atlasest.exceptionHandling.ParsingException
import cz.cvut.fit.atlasest.utils.add
import io.ktor.server.plugins.BadRequestException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class Collection(
    val collectionName: String,
    val identifier: String,
    var items: MutableList<JsonObject>,
    var nextId: Int,
    var schema: JsonObject,
) {
    fun getItemIdValue(item: JsonObject): String {
        val itemId =
            item[identifier]
                ?: throw InvalidDataException("Item is missing identifier in collection $collectionName")
        val idValue =
            itemId.takeIf { value -> value is JsonPrimitive }?.jsonPrimitive?.contentOrNull
                ?: throw InvalidDataException("Invalid identifier value in collection $collectionName (must be a JSON primitive)")
        return idValue
    }

    fun getItemIndex(id: String): Int =
        this.items.indexOfFirst {
            getItemIdValue(it) == id
        }

    fun insertItem(item: JsonObject) {
        val newItem =
            if (this.getIdentifierType() in listOf("number", "integer")) {
                item.add(this.identifier, JsonPrimitive(nextId))
            } else {
                item.add(this.identifier, JsonPrimitive(nextId.toString()))
            }
        this.items.add(newItem)
        this.nextId++
    }

    fun updateItem(
        id: String,
        item: JsonObject,
    ): JsonObject {
        val index = getItemIndex(id)
        if (index < 0) throw BadRequestException("Collection $collectionName does not have an item with $identifier=$id")
        val newItem =
            if (this.getIdentifierType() in listOf("number", "integer")) {
                item.add(this.identifier, JsonPrimitive(id.toInt()))
            } else {
                item.add(this.identifier, JsonPrimitive(id))
            }

        if (newItem.keys != this.items[index].keys) {
            throw BadRequestException("Item with ${this.identifier} '$id' is incomplete")
        }
        items[index] = newItem
        return newItem
    }

    fun deleteItem(id: String) {
        val index = getItemIndex(id)
        if (index >= 0) {
            this.items.removeAt(index)
        }
    }

    private fun getIdentifierType(): String {
        val properties =
            this.schema["properties"]?.takeIf { it is JsonObject }
                ?: throw ParsingException("Invalid or missing properties in schema for collection '$collectionName' (must be JSON object)")

        val identifierProp =
            properties.jsonObject[this.identifier]?.takeIf { it is JsonObject }
                ?: throw ParsingException(
                    "Invalid or missing identifier property in schema for collection '$collectionName' (must be JSON object)",
                )

        val identifierType =
            identifierProp.jsonObject["type"]?.takeIf { it is JsonPrimitive }
                ?: throw ParsingException(
                    "Invalid or missing type for identifier property in schema for collection '$collectionName' " +
                        "(must be JSON primitive)",
                )

        return identifierType.jsonPrimitive.content
    }
}
