package cz.cvut.fit.atlasest.service

import io.ktor.server.plugins.BadRequestException
import kotlinx.serialization.json.JsonObject

/**
 * A service for handling pagination of JSON object collections.
 */
class PaginationService {
    private val defaultLimit = 10

    /**
     * Applies pagination to a collection of JSON objects based on the provided page and limit parameters.
     *
     * @param collectionItems The list of JSON objects to be paginated.
     * @param page The page number (indexed from 1).
     * @param limit The number of items per page. If `null`, the default limit is used.
     *
     * @return A paginated subset of the original collection.
     * @throws BadRequestException If the `_limit` parameter is provided without `_page`.
     */
    internal fun applyPagination(
        collectionItems: MutableList<JsonObject>,
        page: Int?,
        limit: Int?,
    ): MutableList<JsonObject> {
        if (page == null) {
            if (limit == null) {
                return collectionItems
            } else {
                throw BadRequestException("Pagination parameter $LIMIT is without $PAGE")
            }
        } else {
            val newLimit = limit ?: defaultLimit
            val start = (page - 1) * newLimit
            val end = start + newLimit

            return if (start < collectionItems.size) {
                collectionItems.subList(start, end).toMutableList()
            } else {
                mutableListOf()
            }
        }
    }
}
