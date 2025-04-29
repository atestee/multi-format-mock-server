package cz.cvut.fit.atlasest.services

import com.cesarferreira.pluralize.pluralize
import com.cesarferreira.pluralize.singularize
import cz.cvut.fit.atlasest.utils.add
import cz.cvut.fit.atlasest.utils.toCSV
import io.ktor.server.plugins.BadRequestException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * A service for applying data processing operations based request parameters such as filtering, pagination, sorting, query search,
 * embedding and expanding.
 */
class ParameterService(
    private val schemaService: SchemaService,
    private val filterService: FilterService,
    private val paginationService: PaginationService,
    private val sortingService: SortingService,
) {
    /**
     * Applies filtering to a collection of JSON objects based on request parameters.
     * Request parameters are either only the property, like "title" which we want to match with a value.
     * Or there can be a field and operator divided with an underscore like, "title_ne".
     * The operators available are: ne, lt, lte, gt, lte, like.
     *
     * @param collectionName The name of the collection.
     * @param collectionItems The list of JSON objects to be filtered.
     * @param params The request parameters containing filter criteria.
     * @param schemas The JSON schemas of collection items and related collection items (indexed by collection name).
     *
     * @return A filtered list of JSON objects.
     */
    fun applyFilter(
        collectionName: String,
        collectionItems: MutableList<JsonObject>,
        params: Map<String, List<String>>,
        schemas: JsonObject,
    ): MutableList<JsonObject> {
        var items = collectionItems
        val filterParams = getFilterParameters(params)
        filterParams.forEach { (keyOperator, value) ->
            items =
                filterService.applyFilter(items, keyOperator, value, { key ->
                    schemaService.getTypeAndFormatFromJsonSchema(schemas, key, collectionName)
                })
        }
        return items
    }

    /**
     * Applies pagination to a collection of JSON objects based on request parameters _page and _limit.
     *
     * @param collectionItems The list of JSON objects to be paginated.
     * @param params The request parameters containing pagination details.
     * @param baseUrl The base URL to which pagination parameters will be appended.
     * @param totalItems The total number of items across all pages.
     *
     * @return A pair containing the paginated list of JSON objects and an optional string of pagination links (RFC 5988 format).
     */
    fun applyPagination(
        collectionItems: MutableList<JsonObject>,
        params: Map<String, List<String>>,
        baseUrl: String,
        totalItems: Int,
    ): Pair<MutableList<JsonObject>, String?> {
        val page = params[PAGE]?.first()?.toIntOrNull()
        val limit = params[LIMIT]?.first()?.toIntOrNull()
        val paramsString =
            params
                .filter { it.key !in listOf(PAGE, LIMIT) }
                .flatMap { (key, values) ->
                    values.map { value -> "$key=$value" }
                }.joinToString("&")
        if (page == null) {
            if (limit == null) {
                return collectionItems to null
            } else {
                throw BadRequestException("Pagination parameter $LIMIT is without $PAGE")
            }
        }
        val links =
            paginationService.createPaginationLinks(
                baseUrl,
                page,
                limit ?: paginationService.defaultLimit,
                totalItems,
                paramsString,
            )
        val items =
            paginationService.applyPagination(
                collectionItems,
                page,
                limit,
            )
        return items to links
    }

    /**
     * Applies sorting to a collection of JSON objects based on request parameters _sort and _order.
     *
     * @param collectionItems The list of JSON objects to be sorted.
     * @param params The request query parameters containing sorting details.
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
     * Applies query search to a collection of JSON objects based on query parameter _q.
     *
     * @param collectionItems The list of queried JSON objects.
     * @param params The request query parameters containing the query.
     *
     * @return The resulting list of JSON objects.
     */
    fun applyQuerySearch(
        collectionItems: MutableList<JsonObject>,
        params: Map<String, List<String>>,
    ): MutableList<JsonObject> {
        val query = params[QUERY]?.first() ?: return collectionItems
        return collectionItems
            .filter { item ->
                item
                    .toCSV()
                    .lines()[1]
                    .split(";".toRegex())
                    .any { value ->
                        query.lowercase().split("\\s*;\\s*".toRegex()).any { searchTerm ->
                            value.lowercase().contains(searchTerm.lowercase())
                        }
                    }
            }.toMutableList()
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
        val embedValues = params[EMBED]
        val expandValues = params[EXPAND]
        val mainCollection = collectionService.getCollectionItems(collectionName)
        val identifier = collectionService.getCollectionIdentifier(collectionName)
        val embedForeignKey = collectionName.singularize() + "Id"

        return mainCollection
            .map { item ->
                val newItem = embedItem(item, embedValues, collectionService, identifier, embedForeignKey)
                expandItem(newItem, expandValues, collectionService)
            }.toMutableList()
    }

    /**
     * Retrieves the JSON schemas of collections related to the main collection from embed and expand parameters.
     *
     * @param params The request parameters containing embedding and expanding details.
     * @param collectionService The service used to retrieve collections.
     *
     * @return JSON schemas of related collections.
     */
    fun getEmbedAndExpandCollectionSchemas(
        params: Map<String, List<String>>,
        collectionService: CollectionService,
    ): JsonObject {
        val embedValues = params[EMBED]
        val expandValues = params[EXPAND]
        var schemas = JsonObject(mapOf())
        embedValues?.forEach { collectionName ->
            val schema = collectionService.getCollectionSchema(collectionName)
            schemas = schemas.add(collectionName, schema)
        }
        expandValues?.forEach { collectionNameSingular ->
            val collectionName = collectionNameSingular.pluralize()
            val schema = collectionService.getCollectionSchema(collectionName)
            schemas = schemas.add(collectionName, schema)
        }
        return schemas
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
            val embeddedCollection = collectionService.getCollectionItems(embeddedCollectionName)
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
                    QUERY,
                )
        }
}
