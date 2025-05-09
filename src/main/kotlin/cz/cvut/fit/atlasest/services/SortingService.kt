package cz.cvut.fit.atlasest.services

import cz.cvut.fit.atlasest.utils.getPropertyValue
import io.ktor.server.plugins.BadRequestException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * A service for handling sorting of JSON object collections.
 */
class SortingService {
    /**
     * Applies sorting to a list of [JsonObject] based on the provided sort and order parameters
     *
     * @param collectionItems The list of [JsonObject] to be sorted
     * @param sort The key to be sorted by, dot-notation and array indexing with wildcard can be used to refer to nested objects or arrays
     * @param order The sorting order (`asc` or `desc`). If `null`, the default sorting order is used
     *
     * @return The sorted collection
     * @throws BadRequestException if there is a problem with the parameter values
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
                .getPropertyValue(sort)
                ?.jsonPrimitive
                ?.content
                ?: throw BadRequestException("Value of '$sort' is not present")
        }

        return newItems
    }
}
