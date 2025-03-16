package cz.cvut.fit.atlasest.service

import cz.cvut.fit.atlasest.application.AppConfig
import cz.cvut.fit.atlasest.data.Collection
import cz.cvut.fit.atlasest.data.Item
import cz.cvut.fit.atlasest.exceptions.CorruptedDataException
import io.ktor.server.plugins.NotFoundException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import io.swagger.v3.oas.models.media.Schema as OASchema

class CollectionService(
    private val appConfig: AppConfig,
    private val schemaFilename: String?,
    private val jsonService: JsonService,
    private val schemaService: SchemaService,
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

        val schemaString = if (schemaFilename != null) jsonService.readJsonFile(schemaFilename).toString() else null

        collectionData.jsonObject.forEach { (collectionName, collection) ->
            if (collection is JsonArray) {
                val identifier =
                    identifiers[collectionName]
                        ?: throw CorruptedDataException("No identifier key found for collection '$collectionName'")
                val lastId =
                    collection.mapNotNull { it.jsonObject[identifier]?.jsonPrimitive?.int }.maxOrNull()
                        ?: throw CorruptedDataException("Wrong identifier key defined for collection '$collectionName'")
                val schema =
                    if (schemaString != null) {
                        schemaService.getCollectionSchema(collectionName, schemaString)
                    } else {
                        schemaService.inferJsonSchema(collection)
                    }

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

    fun getCollectionSchema(collectionName: String) =
        Json.parseToJsonElement(getCollectionData(collectionName).schema.toString()).jsonObject

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
        val id = collection.nextId
        val itemWithId = item.add(collection.identifier, JsonPrimitive(id))
        this.schemaService.validateDataAgainstSchema(itemWithId, collection.schema)
        collection.insertItem(itemWithId)
        saveData()
        return Item(id, itemWithId)
    }

    fun updateItemInCollection(
        collectionName: String,
        id: String,
        newItem: JsonObject,
    ): JsonObject {
        val collection = getCollectionData(collectionName)
        val index = collection.getItemIndex(id)
        this.schemaService.validateDataAgainstSchema(newItem, collection.schema)
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

    fun getOpenApiSchema(collectionName: String): OASchema<Any> {
        val jsonSchema = getCollectionData(collectionName).schema
        return schemaService.convertJsonSchemaToOpenApi(jsonSchema)
    }
}
