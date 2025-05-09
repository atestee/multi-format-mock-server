package cz.cvut.fit.atlasest.services

import com.cesarferreira.pluralize.pluralize
import com.cesarferreira.pluralize.singularize
import cz.cvut.fit.atlasest.application.AppConfig
import cz.cvut.fit.atlasest.utils.add
import cz.cvut.fit.atlasest.utils.toCSV
import io.ktor.server.plugins.NotFoundException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * A service for applying data processing operations based on request parameters such as filtering, pagination, sorting, query search,
 * embedding and expanding.
 */
class ParameterService(
    private val collectionService: CollectionService,
    private val schemaService: SchemaService,
    private val appConfig: AppConfig,
    private val filterService: FilterService,
    private val paginationService: PaginationService,
    private val sortingService: SortingService,
) {
    /**
     * Retrieves collection items based on given query parameters
     *
     * @param collectionName The name of the collection
     * @param params The query parameters
     *
     * @return A pair containing collection items and the `Link` header value
     * @throws NotFoundException If there is a problem with processing the data based on the parameters
     */
    fun getCollectionWithParams(
        collectionName: String,
        params: Map<String, List<String>>,
    ): Pair<MutableList<JsonObject>, String?> {
        var (data, schemas) =
            if (params[EMBED] is List<String> || params[EXPAND] is List<String>) {
                val data = applyEmbedAndExpand(params, collectionName)
                val schemas = getEmbedAndExpandCollectionSchemas(params)
                data to schemas
            } else {
                collectionService.getCollection(collectionName) to JsonObject(mapOf())
            }
        val queriedData = applyQuerySearch(data, params)
        val schema = collectionService.getCollectionSchema(collectionName)
        schemas = schemas.add(collectionName, schema)
        val filteredData = applyFilter(collectionName, queriedData, params, schemas)
        val sortedData = applySorting(filteredData, params)
        val (paginatedData, links) = applyPagination(sortedData, params, "http://${appConfig.host}:${appConfig.port}/$collectionName")
        return paginatedData to links
    }

    /**
     * Embeds and expands an item with related collections (based on foreign keys) defined by request parameters
     *
     * @param params The request parameters containing embedding and expanding details
     * @param collectionName The name of the embedded or expanded collection
     * @param id The identifier value of the item
     *
     * @return Collection data including embedded items
     */
    fun getItemByIdWithEmbedAndExpandParams(
        params: Map<String, List<String>>,
        collectionName: String,
        id: String,
    ): JsonObject {
        val embedKeys = params[EMBED]
        val expandKeys = params[EXPAND]
        var item = collectionService.getItemById(collectionName, id)
        val identifier = collectionService.getCollectionIdentifier(collectionName)
        val embedForeignKey = collectionName.singularize() + "Id"
        item = embedItem(item, embedKeys, identifier, embedForeignKey)
        item = expandItem(item, expandKeys)
        return item
    }

    /**
     * Applies filtering to a [MutableList] of [JsonObject] based on request parameters
     *
     * Request parameters are either only the property, like "title" which we want to match with a value,
     * or there can be a field and operator divided with an underscore like, "title_ne"
     *
     * The operators available are: ne, lt, lte, gt, lte, like
     *
     * @param collectionName The name of the collection
     * @param collectionItems The list of [JsonObject] to be filtered
     * @param params The request parameters containing filter criteria
     * @param schemas The JSON Schemas of collection items and related collection items (indexed by collection name)
     *
     * @return A filtered list of [JsonObject]
     */
    internal fun applyFilter(
        collectionName: String,
        collectionItems: MutableList<JsonObject>,
        params: Map<String, List<String>>,
        schemas: JsonObject,
    ): MutableList<JsonObject> {
        var items = collectionItems
        val filterParams = getFilterParameters(params)
        filterParams.forEach { (keyOperator, value) ->
            items =
                filterService.applyFilter(items, keyOperator, value) { key ->
                    schemaService.getTypeAndFormatFromJsonSchema(schemas, key, collectionName)
                }
        }
        return items
    }

    /**
     * Applies pagination to a list of [JsonObject] based on request parameters `_page` and `_limit`
     *
     * @param collectionItems The list of [JsonObject] objects to be paginated
     * @param params The request parameters containing pagination details
     * @param baseUrl The base URL
     *
     * @return A pair containing the paginated list of [JsonObject] and an optional string of pagination links
     */
    internal fun applyPagination(
        collectionItems: MutableList<JsonObject>,
        params: Map<String, List<String>>,
        baseUrl: String,
    ): Pair<MutableList<JsonObject>, String?> {
        val page = params[PAGE]?.first()?.toIntOrNull()
        val limit = params[LIMIT]?.first()?.toIntOrNull()
        val paramsString =
            params
                .filter { it.key !in listOf(PAGE, LIMIT) }
                .flatMap { (key, values) ->
                    values.map { value -> "$key=$value" }
                }.joinToString("&")

        return paginationService.applyPagination(
            collectionItems,
            page,
            limit,
            paramsString,
            baseUrl,
        )
    }

    /**
     * Applies sorting to a list of [JsonObject] based on request parameters `_sort` and `_order`
     *
     * @param collectionItems The list of [JsonObject] to be sorted
     * @param params The request query parameters containing sorting details
     *
     * @return A sorted list of [JsonObject]
     */
    internal fun applySorting(
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
     * Applies query search to a list of [JsonObject] based on query parameter `_query`
     *
     * @param collectionItems The list of queried [JsonObject]
     * @param params The request query parameters containing the search terms
     *
     * @return The resulting list of [JsonObject]
     */
    internal fun applyQuerySearch(
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
     * Embeds and expands a collection with related collections (based on foreign keys) defined by request parameters
     *
     * @param params The request parameters containing embedding and expanding details
     * @param collectionName The name of the embedded or expanded collection
     *
     * @return Collection data including embedded or expanded items
     */
    internal fun applyEmbedAndExpand(
        params: Map<String, List<String>>,
        collectionName: String,
    ): MutableList<JsonObject> {
        val embedValues = params[EMBED]
        val expandValues = params[EXPAND]
        val mainCollection = collectionService.getCollection(collectionName)
        val identifier = collectionService.getCollectionIdentifier(collectionName)
        val embedForeignKey = collectionName.singularize() + "Id"

        return mainCollection
            .map { item ->
                val newItem = embedItem(item, embedValues, identifier, embedForeignKey)
                expandItem(newItem, expandValues)
            }.toMutableList()
    }

    /**
     * Retrieves the JSON schemas of related collections based embed and expand parameters.
     *
     * @param params The request parameters containing embedding and expanding details.
     *
     * @return JSON schemas of related collections.
     */
    private fun getEmbedAndExpandCollectionSchemas(params: Map<String, List<String>>): JsonObject {
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
     * Embeds a collection item with items from related collections (using a foreign key)
     *
     * @param item The embedded item (main item)
     * @param embedKeys A list of collection names to embed into the main item.
     * @param identifier The identifier key for the main collection
     * @param foreignKey The key in the related collection that links back to the main item
     *
     * @return The main item with related collection data embedded.
     */
    private fun embedItem(
        item: JsonObject,
        embedKeys: List<String>?,
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
     * Expands a collection item with items from related collections (using a foreign key)
     *
     * @param item The expanded item (main item)
     * @param expandKeys A list of collection names (singularized) to expand the JSON object with.
     *
     * @return The main item
     */
    private fun expandItem(
        item: JsonObject,
        expandKeys: List<String>?,
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
     * Extracts filter parameters from the request by removing pagination, sorting, embed, expand, and query search parameters.
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
