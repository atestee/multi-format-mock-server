package cz.cvut.fit.atlasest.service

import com.cesarferreira.pluralize.pluralize
import com.cesarferreira.pluralize.singularize
import cz.cvut.fit.atlasest.utils.add
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * A service for processing request parameters such as filtering, pagination, and sorting.
 */
class ParameterService {
    private val filterService = FilterService()
    private val paginationService = PaginationService()
    private val sortingService = SortingService()

    /**
     * Applies filtering to a collection of JSON objects based on request parameters.
     *
     * @param collectionItems The list of JSON objects to be filtered.
     * @param params The request parameters containing filter criteria.
     *
     * @return A filtered list of JSON objects.
     */
    fun applyFilter(
        collectionItems: MutableList<JsonObject>,
        params: Map<String, List<String>>,
    ): MutableList<JsonObject> {
        var items = collectionItems
        val filterParams = getFilterParameters(params)
        filterParams.forEach { (keyOperator, value) ->
            items = filterService.applyFilter(items, keyOperator, value)
        }
        return items
    }

    /**
     * Applies pagination to a collection of JSON objects based on request parameters.
     *
     * @param collectionItems The list of JSON objects to be paginated.
     * @param params The request parameters containing pagination details.
     *
     * @return A paginated list of JSON objects.
     */
    fun applyPagination(
        collectionItems: MutableList<JsonObject>,
        params: Map<String, List<String>>,
    ): MutableList<JsonObject> {
        val page = params[PAGE]?.first()?.toIntOrNull()
        val limit = params[LIMIT]?.first()?.toIntOrNull()
        return paginationService.applyPagination(
            collectionItems,
            page,
            limit,
        )
    }

    /**
     * Applies sorting to a collection of JSON objects based on request parameters.
     *
     * @param collectionItems The list of JSON objects to be sorted.
     * @param params The request parameters containing sorting details.
     *
     * @return A sorted list of JSON objects.
     */
    fun applySorting(
        collectionItems: MutableList<JsonObject>,
        params: Map<String, List<String>>,
    ): MutableList<JsonObject> {
        val sort = params[SORT]?.first()
        val order = params[ORDER]?.first()
        return sortingService.applySorting(
            collectionItems,
            sort,
            order,
        )
    }

    /**
     * Embeds and expands a collection with other collections defined by request parameters to main collection based on foreign keys.
     *
     * @param params The request parameters containing embedding and expanding details.
     * @param collectionName The name of the main collection.
     * @param collectionService The service used to retrieve collections.
     *
     * @return Collection data including embedded items.
     */
    fun applyEmbedAndExpand(
        params: Map<String, List<String>>,
        collectionName: String,
        collectionService: CollectionService,
    ): MutableList<JsonObject> {
        val embedKeys = params[EMBED]
        val expandKeys = params[EXPAND]
        val mainCollection = collectionService.getCollection(collectionName)
        val identifier = collectionService.getCollectionIdentifier(collectionName)
        val embedForeignKey = collectionName.singularize() + "Id"

        return mainCollection
            .map { item ->
                val newItem = embedItem(item, embedKeys, collectionService, identifier, embedForeignKey)
                expandItem(newItem, expandKeys, collectionService)
            }.toMutableList()
    }

    /**
     * Embeds and expands an item with other collections defined by request parameters to main collection based on foreign keys.
     *
     * @param params The request parameters containing embedding and expanding details.
     * @param collectionName The name of the main collection.
     * @param id The identifier value of the item.
     * @param collectionService The service used to retrieve collections.
     *
     * @return Collection data including embedded items.
     */
    fun applyEmbedAndExpand(
        params: Map<String, List<String>>,
        collectionName: String,
        id: String,
        collectionService: CollectionService,
    ): JsonObject {
        val embedKeys = params[EMBED]
        val expandKeys = params[EXPAND]
        var item = collectionService.getItemById(collectionName, id)
        val identifier = collectionService.getCollectionIdentifier(collectionName)
        val embedForeignKey = collectionName.singularize() + "Id"
        item = embedItem(item, embedKeys, collectionService, identifier, embedForeignKey)
        item = expandItem(item, expandKeys, collectionService)
        return item
    }

    /**
     * Embeds related items that reference the main item (using a foreign key in related items) into the main item.
     *
     * @param item The main item to embed data from related collections into.
     * @param embedKeys A list of collection names to embed into the JSON object.
     * @param collectionService The service used to retrieve collections.
     * @param identifier The identifier key for the main collection.
     * @param foreignKey The key in the related collection that links back to the main item.
     *
     * @return The main item with related collection data embedded.
     */
    private fun embedItem(
        item: JsonObject,
        embedKeys: List<String>?,
        collectionService: CollectionService,
        identifier: String,
        foreignKey: String,
    ): JsonObject {
        val idValue = item[identifier]
        var newItem = item
        embedKeys?.forEach { embeddedCollectionName ->
            val embeddedCollection = collectionService.getCollection(embeddedCollectionName)
            val embeddedItems =
                embeddedCollection.filter { embeddedItem ->
                    idValue == embeddedItem[foreignKey]
                }
            newItem = newItem.add(embeddedCollectionName, JsonArray(embeddedItems))
        }
        return newItem
    }

    /**
     * Expands main item with related items by using foreign key references found in the main item.
     *
     * @param item The main item to embed data from related collections into.
     * @param expandKeys A list of collection names (singularized) to expand the JSON object with.
     * @param collectionService The service used to retrieve collections.
     *
     * @return The main item
     */
    private fun expandItem(
        item: JsonObject,
        expandKeys: List<String>?,
        collectionService: CollectionService,
    ): JsonObject {
        var newItem = item
        expandKeys?.forEach { expandKey ->
            val expandId = item[expandKey + "Id"]?.jsonPrimitive?.content
            if (expandId != null) {
                val expandItem = collectionService.getItemById(expandKey.pluralize(), expandId)
                newItem = newItem.add(expandKey, expandItem)
            }
        }
        return newItem
    }

    /**
     * Extracts filter parameters from the request by removing pagination, sorting, nested, full-text query parameters.
     *
     * @param params The request parameters.
     *
     * @return A map containing only filter parameters.
     */
    private fun getFilterParameters(params: Map<String, List<String>>): Map<String, List<String>> =
        params.filterNot {
            it.key in
                listOf(
                    PAGE,
                    LIMIT,
                    SORT,
                    ORDER,
                    EMBED,
                    EXPAND,
                    QUERY_SHORT,
                    QUERY_LONG,
                )
        }
}
