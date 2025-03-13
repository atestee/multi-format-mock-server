package cz.cvut.fit.atlasest.data

import cz.cvut.fit.atlasest.application.AppConfig
import cz.cvut.fit.atlasest.exceptions.CorruptedDataException
import cz.cvut.fit.atlasest.service.JsonService
import cz.cvut.fit.atlasest.utils.inferJsonSchema
import io.ktor.server.plugins.NotFoundException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class Repository(
    private val appConfig: AppConfig,
    private val jsonService: JsonService,
) {
    val collections: MutableMap<String, Collection> = mutableMapOf()

    init {
        loadData()
    }

    private fun loadData() {
        val collectionData = jsonService.readJsonFile(appConfig.fileName)
        val identifiers =
            jsonService
                .readJsonFile(
                    appConfig.identifiersFileName,
                ).jsonObject
                .mapValues { it.value.jsonPrimitive.content }

        collectionData.jsonObject.forEach { (collectionName, collection) ->
            if (collection is JsonArray) {
                val identifier =
                    identifiers[collectionName]
                        ?: throw CorruptedDataException("No identifier key found for collection '$collectionName'")
                val lastId =
                    collection.mapNotNull { it.jsonObject[identifier]?.jsonPrimitive?.int }.maxOrNull()
                        ?: throw CorruptedDataException("Wrong identifier key defined for collection '$collectionName'")
                val schema = inferJsonSchema(collection.toString(), identifier)
                collections[collectionName] =
                    Collection(
                        collectionName = collectionName,
                        identifier = identifier,
                        items = collection,
                        nextId = lastId + 1,
                        schema = schema,
                    )
            }
        }
    }

    private fun saveData() {
        val collectionsObject =
            JsonObject(
                this.collections.mapValues { (_, collection) ->
                    collection.items
                },
            )
        jsonService.saveJsonFile(appConfig.fileName, JsonObject(collectionsObject))
    }

    private fun getCollectionData(collectionName: String) =
        this.collections[collectionName] ?: throw CorruptedDataException(
            "Problem when trying to get collection '$collectionName'",
        )

    fun getCollection(collectionName: String) = getCollectionData(collectionName).items

    fun getItemById(
        collectionName: String,
        id: String,
    ): JsonObject {
        val collection = getCollectionData(collectionName)
        val idKey = collection.identifier
        return collection.items.find { it.jsonObject[idKey]?.jsonPrimitive?.content == id }?.jsonObject
            ?: throw NotFoundException("Item with $idKey '$id' not found in collection '$collectionName'")
    }

    fun insertItemToCollection(
        collectionName: String,
        item: JsonObject,
    ): Item {
        val collection = getCollectionData(collectionName)
        val insertedItem = collection.insertItem(item)
        saveData()
        return insertedItem
    }

    fun updateItemInCollection(
        collectionName: String,
        id: String,
        newItem: JsonObject,
    ): JsonObject {
        val collection = getCollectionData(collectionName)
        val index = collection.getItemIndex(id)
        val updatedData = collection.updateItem(id, newItem, index)
        saveData()
        return updatedData
    }

    fun deleteItemFromCollection(
        collectionName: String,
        id: String,
    ) {
        val collection = getCollectionData(collectionName)
        collection.deleteItem(id)
        saveData()
    }
}
