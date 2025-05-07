package cz.cvut.fit.atlasest.data

import cz.cvut.fit.atlasest.exceptionHandling.InvalidDataException
import cz.cvut.fit.atlasest.utils.add
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CollectionTest {
    private val collectionName = "testCollection"
    private val identifier = "testIdentifier"

    @Test
    fun `getNextId - when ids are integers - should return nextId as Int`() {
        val item1 = generateItem(JsonPrimitive(1))
        val item2 = generateItem(JsonPrimitive(2))
        val expectedNextId = 3
        val collection = generateCollection(listOf(item1, item2), "integer", expectedNextId)

        val nextId = collection.getNextId()

        assertEquals(JsonPrimitive(expectedNextId), nextId)
    }

    @Test
    fun `getNextId - when ids are strings - should return nextId as String`() {
        val item1 = generateItem(JsonPrimitive("1"))
        val item2 = generateItem(JsonPrimitive("2"))
        val expectedNextId = 3
        val collection = generateCollection(listOf(item1, item2), "string", expectedNextId)

        val nextId = collection.getNextId()

        assertEquals(JsonPrimitive(expectedNextId.toString()), nextId)
    }

    @Test
    fun `getNextId - when schema is missing properties - should throw InvalidDataException`() {
        val collection = generateCollectionWithMissingProps()

        val exception =
            assertFailsWith<InvalidDataException> {
                collection.getNextId()
            }

        assertEquals("Invalid or missing properties in schema for collection '$collectionName' (must be JSON object)", exception.message)
    }

    @Test
    fun `getNextId - when schema is missing identifier property - should throw InvalidDataException`() {
        val collection = generateCollectionWithIdentifierProps()

        val exception =
            assertFailsWith<InvalidDataException> {
                collection.getNextId()
            }

        assertEquals(
            "Invalid or missing identifier property in schema for collection '$collectionName' (must be JSON object)",
            exception.message,
        )
    }

    @Test
    fun `getNextId - when schema is missing identifier type property - should throw InvalidDataException`() {
        val collection = generateCollectionWithMissingIdentifierType()

        val exception =
            assertFailsWith<InvalidDataException> {
                collection.getNextId()
            }

        assertEquals(
            "Invalid or missing type for identifier property in schema for collection '$collectionName' (must be JSON primitive)",
            exception.message,
        )
    }

    @Test
    fun `getIdInCorrectType - when ids are integers - should return items id as Int`() {
        val item1 = generateItem(JsonPrimitive(1))
        val item2 = generateItem(JsonPrimitive(2))
        val collection = generateCollection(listOf(item1, item2), "integer")

        val id = collection.getIdInCorrectType("1")

        assertEquals(JsonPrimitive(1), id)
    }

    @Test
    fun `getIdInCorrectType - when ids are string - should return items id as String`() {
        val item1 = generateItem(JsonPrimitive(1))
        val item2 = generateItem(JsonPrimitive(2))
        val collection = generateCollection(listOf(item1, item2), "string")

        val id = collection.getIdInCorrectType("1")

        assertEquals(JsonPrimitive("1"), id)
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
    fun `getItemById - when given valid id value - returns the correct item`() {
        val searchedIdValue = "1"
        val item1 = generateItem(JsonPrimitive(searchedIdValue))
        val item2 = generateItem(JsonPrimitive("2"))

        val collection = generateCollection(listOf(item1, item2))

        val result = collection.getItemById(searchedIdValue)

        assertEquals(item1, result)
    }

    @Test
    fun `getItemById - when given invalid id value - returns the correct item`() {
        val searchedIdValue = "3"
        val item1 = generateItem(JsonPrimitive("1"))
        val item2 = generateItem(JsonPrimitive("2"))

        val collection = generateCollection(listOf(item1, item2))

        val exception =
            assertFailsWith<NotFoundException> {
                collection.getItemById(searchedIdValue)
            }

        assertEquals("Item with $identifier '$searchedIdValue' not found in collection '$collectionName'", exception.message)
    }

    @Test
    fun `insertItem - identifier type is string - increases nextId and adds item to items`() {
        val itemId = 2
        val item = generateItem(JsonPrimitive(1))
        val insertedItem = generateItem(JsonPrimitive(itemId.toString()))
        val collection = generateCollection(listOf(item), nextId = itemId)

        collection.insertItem(item)

        assertEquals(2, collection.items.size)
        assertEquals(itemId + 1, collection.getNextId().content.toIntOrNull())
        assertEquals(itemId.toString(), insertedItem[identifier]!!.jsonPrimitive.content)
        assertEquals(insertedItem, collection.items[1])
    }

    @Test
    fun `insertItem - identifier type is number - increases nextId and adds item to items`() {
        val itemId = 2
        val item = generateItem(JsonPrimitive(1))
        val insertedItem = generateItem(JsonPrimitive(itemId))
        val collection = generateCollection(listOf(item), "number", itemId)

        collection.insertItem(item)

        assertEquals(2, collection.items.size)
        assertEquals(itemId + 1, collection.getNextId().content.toIntOrNull())
        assertEquals(itemId.toString(), insertedItem[identifier]!!.jsonPrimitive.content)
        assertEquals(insertedItem, collection.items[1])
    }

    @Test
    fun `updateItem - identifier type is string - updates the item in colleciton`() {
        val itemId = 2
        val item = generateItem(JsonPrimitive(itemId.toString())).add("key", JsonPrimitive("value"))
        val updatedItem = generateItem(JsonPrimitive(itemId.toString())).add("key", JsonPrimitive("newValue"))
        val collection = generateCollection(listOf(item))

        collection.updateItem(itemId.toString(), updatedItem)

        assertEquals(updatedItem, collection.items[0])
    }

    @Test
    fun `updateItem - identifier type is number - updates the item in colleciton`() {
        val itemId = 2
        val item = generateItem(JsonPrimitive(itemId)).add("key", JsonPrimitive("value"))
        val updatedItem = generateItem(JsonPrimitive(itemId)).add("key", JsonPrimitive("newValue"))
        val collection = generateCollection(listOf(item), "number")

        collection.updateItem(itemId.toString(), updatedItem)

        assertEquals(updatedItem, collection.items[0])
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

    private fun generateCollection(
        items: List<JsonObject>,
        idType: String = "string",
        nextId: Int = 1,
    ): Collection =
        Collection(
            collectionName = collectionName,
            identifier = identifier,
            items = items.toMutableList(),
            nextId = nextId,
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

    private fun generateCollectionWithMissingIdentifierType(): Collection =
        Collection(
            collectionName = collectionName,
            identifier = identifier,
            items = mutableListOf(JsonObject(mapOf())),
            nextId = 1,
            schema =
                JsonObject(
                    mapOf(
                        "properties" to
                            JsonObject(
                                mapOf(
                                    identifier to JsonObject(mapOf()),
                                ),
                            ),
                    ),
                ),
        )

    private fun generateCollectionWithIdentifierProps(): Collection =
        Collection(
            collectionName = collectionName,
            identifier = identifier,
            items = mutableListOf(JsonObject(mapOf())),
            nextId = 1,
            schema =
                JsonObject(
                    mapOf(
                        "properties" to
                            JsonObject(
                                mapOf(
                                    identifier to JsonNull,
                                ),
                            ),
                    ),
                ),
        )

    private fun generateCollectionWithMissingProps(): Collection =
        Collection(
            collectionName = collectionName,
            identifier = identifier,
            items = mutableListOf(JsonObject(mapOf())),
            nextId = 1,
            schema =
                JsonObject(
                    mapOf(
                        "properties" to JsonNull,
                    ),
                ),
        )
}
