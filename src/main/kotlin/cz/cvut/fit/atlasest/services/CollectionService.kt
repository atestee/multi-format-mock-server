package cz.cvut.fit.atlasest.services

import cz.cvut.fit.atlasest.data.Repository
import cz.cvut.fit.atlasest.exceptionHandling.InvalidDataException
import cz.cvut.fit.atlasest.utils.add
import io.ktor.server.plugins.NotFoundException
import io.swagger.v3.oas.models.media.Schema
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import javax.validation.ValidationException

class CollectionService(
    private val schemaService: SchemaService,
    private val repository: Repository,
) {
    /**
     * Retrieves all collection names from repository.
     *
     * @return A list of strings representing the names of all the collections.
     */
    fun getCollectionNames() = repository.getCollectionNames()

    /**
     * Retrieves all items from a specified collection from the repository.
     *
     * @param collectionName The name of the collection.
     *
     * @return A JSON array with JSON objects representing the collection of items.
     * @throws NotFoundException If the collection was not found.
     */
    fun getCollection(collectionName: String) = repository.getCollection(collectionName)

    /**
     * Retrieves identifier of the specified collection from the repository.
     *
     * @param collectionName The name of the collection.
     *
     * @return A string representing the collection identifier.
     * @throws NotFoundException If the collection was not found.
     */
    fun getCollectionIdentifier(collectionName: String) = repository.getCollectionIdentifier(collectionName)

    /**
     * Retrieves the schema of a specified collection from the repository.
     *
     * @param collectionName The name of the collection.
     *
     * @return A JSON object representing the schema.
     *
     * @throws NotFoundException If the collection was not found.
     * @throws InvalidDataException If there is a problem with the JSON data.
     */
    fun getCollectionSchema(collectionName: String) = repository.getCollectionSchema(collectionName)

    /**
     * Retrieves an item by its identifier from a specified collection from the repository.
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
    ) = repository.getItemById(collectionName, id)

    /**
     * Inserts a new item into a specified collection using the repository.
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
        val schema = repository.getCollectionSchema(collectionName)
        val id = repository.getCollectionNextId(collectionName)
        val identifier = repository.getCollectionIdentifier(collectionName)
        val itemWithId = item.add(identifier, id)
        this.schemaService.validateItemAgainstSchema(itemWithId, schema)
        return repository.insertItemToCollection(collectionName, itemWithId)
    }

    /**
     * If the item exists, updates it, otherwise inserts a new item using the repository.
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
        val schema = repository.getCollectionSchema(collectionName)
        val idInCorrectType = repository.getCollectionIdInCorrectType(collectionName, id)
        val identifier = repository.getCollectionIdentifier(collectionName)
        val newItemWithId = newItem.add(identifier, idInCorrectType)
        this.schemaService.validateItemAgainstSchema(newItemWithId, schema)
        return repository.updateItemInCollection(collectionName, id, newItemWithId)
    }

    /**
     * Deletes an item from a specified collection using the repository.
     *
     * @param collectionName The name of the collection.
     * @param id The identifier of the item to delete.
     */
    fun deleteItemFromCollection(
        collectionName: String,
        id: String,
    ) {
        repository.deleteItemFromCollection(collectionName, id)
    }

    /**
     * Retrieves the OpenAPI schema for a specified collection.
     *
     * @param collectionName The name of the collection.
     *
     * @return An OpenAPI [Schema] object representing the collection schema.
     */
    fun getOpenApiSchema(collectionName: String): Schema<Any> {
        val jsonSchema = repository.getCollectionSchema(collectionName)
        return schemaService.convertJsonSchemaToOpenApi(jsonSchema.jsonObject, collectionName)
    }
}
