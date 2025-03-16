package cz.cvut.fit.atlasest.data

import cz.cvut.fit.atlasest.exceptions.InvalidSchemaException
import cz.cvut.fit.atlasest.service.add
import io.ktor.server.plugins.BadRequestException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class Collection(
    val collectionName: String,
    val identifier: String,
    var items: JsonArray,
    var nextId: Int,
    var schema: JsonObject,
) {
    fun getItemIndex(id: String): Int {
        val itemsMap = this.items.toMutableList()
        val index = itemsMap.indexOfFirst { it.jsonObject[this.identifier]?.jsonPrimitive?.content == id }
        return index
    }

    fun insertItem(item: JsonObject) {
        this.nextId++
        this.items.add(item)
    }

    fun updateItem(
        id: String,
        item: JsonObject,
        index: Int,
    ): JsonObject {
        val newItem =
            if (this.getIdentifierType() == "integer") {
                item.add(this.identifier, JsonPrimitive(id.toInt()))
            } else {
                item.add(this.identifier, JsonPrimitive(id))
            }
        if (newItem.keys !=
            this.items
                .toMutableList()[index]
                .jsonObject.keys
        ) {
            throw BadRequestException("Item with ${this.identifier} '$id' is incomplete")
        }
        this.items = items.add(newItem)
        return newItem
    }

    fun deleteItem(id: String) {
        val itemsMap = this.items.toMutableList()
        val index = getItemIndex(id)
        if (index >= 0) {
            itemsMap.removeAt(index)
            this.items = JsonArray(itemsMap)
        }
    }

    private fun getIdentifierType(): String =
        this.schema["properties"]
            ?.jsonObject
            ?.get(this.identifier)
            ?.jsonObject
            ?.get("type")
            ?.jsonPrimitive
            ?.content
            ?: throw InvalidSchemaException("Invalid $collectionName schema")
}
