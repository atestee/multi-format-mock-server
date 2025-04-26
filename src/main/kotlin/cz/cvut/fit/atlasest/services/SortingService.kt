package cz.cvut.fit.atlasest.services

import cz.cvut.fit.atlasest.utils.getFieldValue
import io.ktor.server.plugins.BadRequestException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * A service for handling sorting of JSON object collections.
 */
class SortingService {
    /**
     * Applies sorting to a collection of JSON objects based on the provided sort and order parameters.
     *
     * @param collectionItems The list of JSON objects to be sorted.
     * @param sort The key to be sorted by.
     * @param order The sorting order. If `null`, the default sorting order is used.
     *
     * @return The sorted collection.
     */
    internal fun applySorting(
        collectionItems: MutableList<JsonObject>,
        sort: String?,
        order: String?,
    ): MutableList<JsonObject> {
        if (sort == null) {
            if (order == null) {
                return collectionItems
            } else {
                throw BadRequestException("Sorting parameter $ORDER is without $SORT")
            }
        }

        val newItems = collectionItems.toMutableList()

        val sortFunction: (MutableList<JsonObject>, (JsonObject) -> String) -> Unit =
            when (order) {
                null, "asc" -> MutableList<JsonObject>::sortBy
                "desc" -> MutableList<JsonObject>::sortByDescending
                else -> throw BadRequestException("Invalid sorting parameter $ORDER (must be 'asc' or 'desc')")
            }

        sortFunction(newItems) {
            it
                .getFieldValue(sort)
                ?.jsonPrimitive
                ?.content
                ?: throw BadRequestException("Value of '$sort' is not present")
        }

        return newItems
    }
}
