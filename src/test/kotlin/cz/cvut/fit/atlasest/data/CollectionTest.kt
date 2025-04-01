package cz.cvut.fit.atlasest.data

import cz.cvut.fit.atlasest.exceptions.InvalidDataException
import cz.cvut.fit.atlasest.utils.add
import io.ktor.server.plugins.BadRequestException
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CollectionTest {
    private val collectionName = "testCollection"
    private val identifier = "testIdentifier"

    @Test
    fun `getItemIdValue - when item is missing identifier - throws InvalidDataException`() {
        val item =
            JsonObject(
                mapOf(
                    "key1" to JsonPrimitive("value1"),
                    "key2" to JsonPrimitive("value2"),
                ),
            )

        val collection = generateCollection(item)

        val exception =
            assertFailsWith<InvalidDataException> {
                collection.getItemIdValue(item)
            }

        assertEquals("Item is missing identifier in collection $collectionName", exception.message)
    }

    @Test
    fun `getItemIdValue - when item has invalid identifier - throws InvalidDataException`() {
        val item =
            JsonObject(
                mapOf(
                    identifier to JsonObject(mapOf()),
                ),
            )

        val collection = generateCollection(item)

        val exception =
            assertFailsWith<InvalidDataException> {
                collection.getItemIdValue(item)
            }

        assertEquals("Invalid identifier value in collection $collectionName (must be a JSON primitive)", exception.message)
    }

    @Test
    fun `getItemIdValue - when item has valid identifier - returns the id value`() {
        val idValue = "1"
        val item = generateItem(JsonPrimitive(idValue))

        val collection = generateCollection(item)

        val result = collection.getItemIdValue(item)

        assertEquals(idValue, result)
    }

    @Test
    fun `getItemIndex - when given valid id value - returns the corresponding item`() {
        val searchedIdValue = "1"
        val item1 = generateItem(JsonPrimitive(searchedIdValue))
        val item2 = generateItem(JsonPrimitive("2"))

        val collection = generateCollection(listOf(item1, item2))

        val result = collection.getItemIndex(searchedIdValue)

        assertEquals(0, result)
    }

    @Test
    fun `insertItem - identifier type is string - increases nextId and adds item to items`() {
        val itemId = 2
        val item = generateItem(JsonNull)
        val insertedItem = generateItem(JsonPrimitive(itemId.toString()))
        val collection = generateCollection(listOf(item)).apply { this.nextId = itemId }

        collection.insertItem(item)

        assertEquals(2, collection.items.size)
        assertEquals(itemId + 1, collection.nextId)
        assertEquals(itemId.toString(), collection.getItemIdValue(insertedItem))
        assertEquals(insertedItem, collection.items[1])
    }

    @Test
    fun `insertItem - identifier type is number - increases nextId and adds item to items`() {
        val itemId = 2
        val item = generateItem(JsonNull)
        val insertedItem = generateItem(JsonPrimitive(itemId))
        val collection = generateCollection(listOf(item), "number").apply { this.nextId = itemId }

        collection.insertItem(item)

        assertEquals(2, collection.items.size)
        assertEquals(itemId + 1, collection.nextId)
        assertEquals(itemId.toString(), collection.getItemIdValue(insertedItem))
        assertEquals(insertedItem, collection.items[1])
    }

    @Test
    fun `updateItem - identifier type is string - increases nextId and adds item to items`() {
        val itemId = 2
        val item = generateItem(JsonPrimitive(itemId.toString())).add("key", JsonPrimitive("value"))
        val updatedItem = generateItem(JsonPrimitive(itemId.toString())).add("key", JsonPrimitive("newValue"))
        val collection = generateCollection(listOf(item))

        collection.updateItem(itemId.toString(), updatedItem)

        assertEquals(updatedItem, collection.items[1])
    }

    @Test
    fun `updateItem - identifier type is number - updates the item in colleciton`() {
        val itemId = 2
        val item = generateItem(JsonPrimitive(itemId)).add("key", JsonPrimitive("value"))
        val updatedItem = generateItem(JsonPrimitive(itemId)).add("key", JsonPrimitive("newValue"))
        val collection = generateCollection(listOf(item), "number")

        collection.updateItem(itemId.toString(), updatedItem)

        assertEquals(updatedItem, collection.items[1])
    }

    @Test
    fun `updateItem - item is missing a key-value pair - throws BadRequestException`() {
        val itemId = 2
        val item = generateItem(JsonPrimitive(itemId)).add("key", JsonPrimitive("value"))
        val updatedItem = generateItem(JsonPrimitive(itemId))
        val collection = generateCollection(listOf(item), "number")

        val exception =
            assertFailsWith<BadRequestException> {
                collection.updateItem(itemId.toString(), updatedItem)
            }

        assertEquals("Item with $identifier '$itemId' is incomplete", exception.message)
    }

    @Test
    fun `updateItem - no item with given id - throws BadRequestException`() {
        val wrongId = 3
        val itemId = 1
        val item = generateItem(JsonPrimitive(itemId)).add("key", JsonPrimitive("value"))
        val updatedItem = generateItem(JsonPrimitive(itemId))
        val collection = generateCollection(listOf(item), "number")

        val exception =
            assertFailsWith<BadRequestException> {
                collection.updateItem(wrongId.toString(), updatedItem)
            }

        assertEquals("Collection $collectionName does not have an item with $identifier=$wrongId", exception.message)
    }

    @Test
    fun `updateItem - when item is present - deletes it`() {
        val deletedId = 1
        val item1 = generateItem(JsonPrimitive(deletedId))
        val item2 = generateItem(JsonPrimitive(2))

        val collection = generateCollection(listOf(item1, item2), "number")

        collection.deleteItem(deletedId.toString())

        assertEquals(1, collection.items.size)
    }

    @Test
    fun `updateItem - when item is not present - nothing is thrown`() {
        val wrongId = 3
        val item1 = generateItem(JsonPrimitive(1))
        val item2 = generateItem(JsonPrimitive(2))

        val collection = generateCollection(listOf(item1, item2), "number")

        collection.deleteItem(wrongId.toString())

        assertEquals(2, collection.items.size)
    }

    private fun generateItem(id: JsonPrimitive): JsonObject =
        JsonObject(
            mapOf(
                identifier to id,
            ),
        )

    private fun generateCollection(item: JsonObject): Collection =
        Collection(
            collectionName = collectionName,
            identifier = identifier,
            items = mutableListOf(item),
            nextId = 1,
            schema = JsonObject(mapOf()),
        )

    private fun generateCollection(
        items: List<JsonObject>,
        idType: String = "string",
    ): Collection =
        Collection(
            collectionName = collectionName,
            identifier = identifier,
            items = items.toMutableList(),
            nextId = 1,
            schema =
                JsonObject(
                    mapOf(
                        "properties" to
                            JsonObject(
                                mapOf(
                                    identifier to
                                        JsonObject(
                                            mapOf(
                                                "type" to JsonPrimitive(idType),
                                            ),
                                        ),
                                ),
                            ),
                    ),
                ),
        )
}
