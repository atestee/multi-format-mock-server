package cz.cvut.fit.atlasest.services

import cz.cvut.fit.atlasest.application.AppConfig
import cz.cvut.fit.atlasest.data.Collection
import cz.cvut.fit.atlasest.exceptionHandling.InvalidDataException
import cz.cvut.fit.atlasest.exceptionHandling.ParsingException
import cz.cvut.fit.atlasest.utils.add
import cz.cvut.fit.atlasest.utils.log
import io.ktor.server.plugins.NotFoundException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.validation.ValidationException
import io.swagger.v3.oas.models.media.Schema as Schema

/**
 * A service for managing collections of items.
 */
class CollectionService(
    private val appConfig: AppConfig,
    private val schemaFilename: String?,
    private val documentService: DocumentService,
    private val schemaService: SchemaService,
) {
    val collections: MutableMap<String, Collection> = mutableMapOf()

    init {
        loadData()
    }

    /**
     * Retrieves all items from a specified collection.
     *
     * @param collectionName The name of the collection.
     *
     * @return A JSON array with JSON objects representing the collection of items.
     * @throws NotFoundException If the collection was not found.
     */
    fun getCollection(collectionName: String) = getCollectionData(collectionName).items

    /**
     * Retrieves identifier the specified collection.
     *
     * @param collectionName The name of the collection.
     *
     * @return A string representing the collection identifier.
     * @throws NotFoundException If the collection was not found.
     */
    fun getCollectionIdentifier(collectionName: String) = getCollectionData(collectionName).identifier

    /**
     * Retrieves the schema of a specified collection.
     *
     * @param collectionName The name of the collection.
     *
     * @return A JSON object representing the schema.
     *
     * @throws NotFoundException If the collection was not found.
     * @throws InvalidDataException If there is a problem with the JSON data.
     */
    fun getCollectionSchema(collectionName: String) = getCollectionData(collectionName).schema

    /**
     * Retrieves an item by its identifier from a specified collection.
     *
     * @param collectionName The name of the collection.
     * @param id The identifier value of the item.
     *
     * @return The JSON object representing the item.
     *
     * @throws NotFoundException If the item is not found.
     * @throws InvalidDataException If there is a problem with the JSON data.
     */
    fun getItemById(
        collectionName: String,
        id: String,
    ): JsonObject {
        val collection = getCollectionData(collectionName)
        val item =
            collection.items.find {
                collection.getItemIdValue(it) == id
            } ?: throw NotFoundException("Item with ${collection.identifier} '$id' not found in collection '$collectionName'")
        return item
    }

    /**
     * Inserts a new item into a specified collection.
     *
     * @param collectionName The name of the collection.
     * @param item The JSON object representing the item to be inserted.
     *
     * @return The inserted item and its identifier.
     *
     * @throws NotFoundException If the collection was not found.
     * @throws ValidationException If the item does not match the schema.
     */
    fun insertItemToCollection(
        collectionName: String,
        item: JsonObject,
    ): Pair<Int, JsonObject> {
        val collection = getCollectionData(collectionName)
        val id = collection.nextId
        val itemWithId = item.add(collection.identifier, JsonPrimitive(id))
        this.schemaService.validateDataAgainstSchema(itemWithId, collection.schema)
        collection.insertItem(itemWithId)
        saveData()
        return id to itemWithId
    }

    /**
     * If the item exists, updates it, otherwise inserts a new item.
     *
     * @param collectionName The name of the collection.
     * @param id The identifier of the item to update.
     * @param newItem A JSON object representing the updated or inserted item.
     *
     * @return The updated JSON object.
     *
     * @throws NotFoundException If the item or collection is not found.
     * @throws ValidationException If the new item does not match the schema.
     * @throws InvalidDataException If there is a problem with the JSON data.
     */
    fun updateItemInCollection(
        collectionName: String,
        id: String,
        newItem: JsonObject,
    ): JsonObject {
        val collection = getCollectionData(collectionName)
        this.schemaService.validateDataAgainstSchema(newItem, collection.schema)
        val updatedData = collection.updateItem(id, newItem)
        saveData()
        return updatedData
    }

    /**
     * Deletes an item from a specified collection.
     *
     * @param collectionName The name of the collection.
     * @param id The identifier of the item to delete.
     */
    fun deleteItemFromCollection(
        collectionName: String,
        id: String,
    ) {
        val collection = getCollectionData(collectionName)
        collection.deleteItem(id)
        saveData()
    }

    /**
     * Retrieves the OpenAPI schema for a specified collection.
     *
     * @param collectionName The name of the collection.
     *
     * @return An OpenAPI [Schema] object representing the collection schema.
     */
    fun getOpenApiSchema(collectionName: String): Schema<Any> {
        val jsonSchema = getCollectionData(collectionName).schema
        return schemaService.convertJsonSchemaToOpenApi(jsonSchema.jsonObject)
    }

    /**
     * Loads and parses collection and schema data from local directory.
     *
     * @throws ParsingException If there is an issue parsing the data.
     * @throws ValidationException When validating collection data against JSON schema.
     */
    private fun loadData() {
        val collectionData = documentService.readJsonFile(appConfig.collectionsFilename)
        val identifiersData = documentService.readJsonFile(appConfig.identifiersFileName)

        val identifiers = getIdentifiers(identifiersData)
        val schemaCollection = if (schemaFilename != null) documentService.readJsonFile(schemaFilename) else null

        collectionData.forEach { (collectionName, collection) ->
            if (collection !is JsonArray) {
                throw ParsingException("Invalid value for $collectionName in ${appConfig.collectionsFilename} (must be a JSON array)")
            } else {
                if (collection.isEmpty()) throw ParsingException("Collection $collectionName is empty")
                val collectionList =
                    collection
                        .map {
                            it as? JsonObject
                                ?: throw ParsingException("Invalid item in collection $collectionName (must be a JSON object)")
                        }.toMutableList()
                val identifier =
                    identifiers[collectionName]
                        ?: throw ParsingException("No identifier key found for collection '$collectionName'")
                val lastId = getMaxIdentifier(collectionList, identifier, collectionName)
                val schema =
                    if (schemaCollection != null) {
                        log.info("Trying to retrieve schema from given file $schemaFilename for collection '$collectionName'")
                        val schemaForCollection = schemaService.getCollectionSchema(collectionName, schemaCollection)
                        if (schemaForCollection is JsonObject) {
                            log.info("Schema for collection $collectionName exists")
                            log.info("Validating collection $collectionName against schema")
                            validateCollectionAgainstSchema(collectionList, collectionName, identifier, schemaForCollection)
                            schemaForCollection
                        } else {
                            log.info("Schema does not exist for collection $collectionName, will be inferred")
                            schemaService.inferJsonSchema(collection)
                        }
                    } else {
                        log.info("Schema file was not included, inferring schema for collection '$collectionName'")
                        schemaService.inferJsonSchema(collection)
                    }

                collections[collectionName] =
                    Collection(
                        collectionName = collectionName,
                        identifier = identifier,
                        items = collectionList,
                        nextId = lastId + 1,
                        schema = schema,
                    )
            }
        }
    }

    /**
     * Saves collection data to file in local directory.
     */
    private fun saveData() {
        val collectionsObject =
            JsonObject(
                this.collections.mapValues { (_, collection) ->
                    JsonArray(collection.items)
                },
            )
        documentService.saveJsonFile(appConfig.collectionsFilename, JsonObject(collectionsObject))
    }

    /**
     * Extracts collection identifiers.
     *
     * @param identifiersData The JSON object containing identifiers.
     *
     * @return A map of identifiers indexed by the collection name.
     * @throws ParsingException If there is an issue parsing the data.
     */
    private fun getIdentifiers(identifiersData: JsonObject): Map<String, String> =
        identifiersData.mapValues { entry ->
            (entry.value as? JsonPrimitive)?.jsonPrimitive?.content
                ?: throw ParsingException(
                    "Invalid identifier for collection ${entry.key} in ${appConfig.identifiersFileName}" +
                        " (must be a JSON primitive)",
                )
        }

    /**
     * Finds the maximum identifier value in a collection. Expects collection to not be empty.
     *
     * @param collection The JSON array representing the collection.
     * @param identifier The collection identifier key.
     * @param collectionName The name of the collection.
     *
     * @return The highest identifier value found.
     *
     * @throws ParsingException If there is an issue parsing the data.
     */
    private fun getMaxIdentifier(
        collection: List<JsonObject>,
        identifier: String,
        collectionName: String,
    ): Int =
        collection.maxOf { item ->
            val idValue =
                item[identifier]
                    ?: throw ParsingException("Missing identifier value for item in collection $collectionName")

            val idJsonPrimitive =
                idValue as? JsonPrimitive
                    ?: throw ParsingException(
                        "Invalid identifier value for item in collection $collectionName (must be a JSON primitive)",
                    )

            idJsonPrimitive.intOrNull
                ?: throw ParsingException("Invalid identifier value in collection $collectionName (must be an integer or integer string)")
        }

    /**
     * Validates collection items against a given JSON schema.
     *
     * @param collection The JSON array representing the collection.
     * @param collectionName The name of the collection.
     * @param identifier The collection identifier key.
     * @param schema The JSON schema used for validation.
     */
    private fun validateCollectionAgainstSchema(
        collection: List<JsonObject>,
        collectionName: String,
        identifier: String,
        schema: JsonObject,
    ) {
        val errors: MutableList<String> = mutableListOf()
        collection.forEach { item ->
            try {
                schemaService.validateDataAgainstSchema(item, schema)
            } catch (e: ValidationException) {
                log.error(
                    "Validation failed for collection '$collectionName' at item with $identifier ${item.jsonObject[identifier]}: ${e.message}",
                )
                errors.add(e.message.toString())
            }
        }
        if (errors.isNotEmpty()) {
            throw ValidationException(errors.joinToString("\n"))
        }
    }

    /**
     * Retrieves a collection by name.
     *
     * @param collectionName The name of the collection.
     *
     * @return The [Collection] object.
     * @throws NotFoundException If the collection does not exist.
     */
    private fun getCollectionData(collectionName: String) =
        this.collections[collectionName] ?: throw NotFoundException("Collection $collectionName not found")
}
