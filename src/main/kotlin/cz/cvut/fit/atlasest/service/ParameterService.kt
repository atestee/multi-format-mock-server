package cz.cvut.fit.atlasest.service

import kotlinx.serialization.json.JsonObject

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
     * Extracts filter parameters from the request by removing pagination, sorting, nested, full-text query parameters.
     *
     * @param params The request parameters.
     *
     * @return A map containing only filter parameters.
     */
    private fun getFilterParameters(params: Map<String, List<String>>): Map<String, List<String>> =
        params.filter {
            it.key !in
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
