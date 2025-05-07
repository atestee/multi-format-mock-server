package cz.cvut.fit.atlasest.data

import cz.cvut.fit.atlasest.exceptionHandling.InvalidDataException
import cz.cvut.fit.atlasest.utils.add
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class Collection(
    val collectionName: String,
    val identifier: String,
    var items: MutableList<JsonObject>,
    var schema: JsonObject,
    private var nextId: Int,
) {
    fun getNextId(): JsonPrimitive =
        if (this.getIdentifierType() in listOf("number", "integer")) {
            JsonPrimitive(nextId)
        } else {
            JsonPrimitive(nextId.toString())
        }

    fun getIdInCorrectType(id: String): JsonPrimitive =
        if (this.getIdentifierType() in listOf("number", "integer")) {
            JsonPrimitive(id.toInt())
        } else {
            JsonPrimitive(id)
        }

    fun getItemIndex(id: String): Int =
        this.items.indexOfFirst {
            it[identifier]!!.jsonPrimitive.content == id
        }

    /**
     * Retrieves an item by the given id.
     *
     * @param id The identifier value of the item.
     *
     * @return The JSON object representing the item.
     *
     * @throws NotFoundException If the item is not found.
     * @throws InvalidDataException If there is a problem with the JSON data.
     */
    fun getItemById(id: String): JsonObject {
        val item =
            items.find {
                it[identifier]!!.jsonPrimitive.content == id
            } ?: throw NotFoundException("Item with $identifier '$id' not found in collection '$collectionName'")
        return item
    }

    fun insertItem(item: JsonObject): Int {
        val newItem =
            if (this.getIdentifierType() in listOf("number", "integer")) {
                item.add(this.identifier, JsonPrimitive(nextId))
            } else {
                item.add(this.identifier, JsonPrimitive(nextId.toString()))
            }
        this.items.add(newItem)
        return this.nextId++
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
                ?: throw InvalidDataException(
                    "Invalid or missing properties in schema for collection '$collectionName' (must be JSON object)",
                )

        val identifierProp =
            properties.jsonObject[this.identifier]?.takeIf { it is JsonObject }
                ?: throw InvalidDataException(
                    "Invalid or missing identifier property in schema for collection '$collectionName' (must be JSON object)",
                )

        val identifierType =
            identifierProp.jsonObject["type"]?.takeIf { it is JsonPrimitive }
                ?: throw InvalidDataException(
                    "Invalid or missing type for identifier property in schema for collection '$collectionName' " +
                        "(must be JSON primitive)",
                )

        return identifierType.jsonPrimitive.content
    }
}
