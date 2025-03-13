package cz.cvut.fit.atlasest.data

import cz.cvut.fit.atlasest.utils.validateDataAgainstSchema
import io.ktor.server.plugins.BadRequestException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.everit.json.schema.NumberSchema
import org.everit.json.schema.ObjectSchema
import org.everit.json.schema.Schema

data class Collection(
    val collectionName: String,
    val identifier: String,
    var items: JsonArray,
    var nextId: Int,
    var schema: Schema,
)

fun Collection.getItemIndex(id: String): Int {
    val itemsMap = this.items.toMutableList()
    val index = itemsMap.indexOfFirst { it.jsonObject[this.identifier]?.jsonPrimitive?.content == id }
    return index
}

fun Collection.insertItem(item: JsonObject): Item {
    validateDataAgainstSchema(item.toString(), this.schema)
    val newItem = item.toMutableMap()
    val id = nextId++
    newItem[this.identifier] = JsonPrimitive(id)

    val itemsMap = this.items.toMutableList()
    itemsMap.add(JsonObject(newItem))
    this.items = JsonArray(itemsMap)

    return Item(id, JsonObject(newItem))
}

fun Collection.updateItem(
    id: String,
    item: JsonObject,
    index: Int,
): JsonObject {
    validateDataAgainstSchema(item.toString(), this.schema)
    val itemMap = item.toMutableMap()
    if ((this.schema as ObjectSchema).propertySchemas["id"] is NumberSchema) {
        itemMap[this.identifier] = JsonPrimitive(id.toInt())
    } else {
        itemMap[this.identifier] = JsonPrimitive(id)
    }
    val newItem = JsonObject(itemMap)
    val itemsMap = this.items.toMutableList()
    if (newItem.keys != itemsMap[index].jsonObject.keys) {
        throw BadRequestException("Item with ${this.identifier} '$id' is incomplete")
    }
    itemsMap[index] = JsonObject(newItem)
    this.items = JsonArray(itemsMap)
    return JsonObject(newItem)
}

fun Collection.deleteItem(id: String) {
    val itemsMap = this.items.toMutableList()
    val index = getItemIndex(id)
    if (index >= 0) {
        itemsMap.removeAt(index)
        this.items = JsonArray(itemsMap)
    }
}
