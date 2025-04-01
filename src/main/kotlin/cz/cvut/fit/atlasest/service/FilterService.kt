package cz.cvut.fit.atlasest.service

import cz.cvut.fit.atlasest.utils.getFieldValue
import io.ktor.server.plugins.BadRequestException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull

/**
 * A service for filtering JSON objects based on specified parameters.
 */
class FilterService {
    /**
     * Applies filtering to a collection of JSON objects based on a given key with operator, and values.
     *
     * @param collectionItems The list of JSON objects to be filtered.
     * @param keyOperator The string with key and optional operator used for filtering, divided by underscore, for example "name_like".
     * @param values The list of values to filter against.
     *
     * @return A filtered list of JSON objects.
     */
    internal fun applyFilter(
        collectionItems: MutableList<JsonObject>,
        keyOperator: String,
        values: List<String>,
    ): MutableList<JsonObject> {
        val (key, operator) = parseKeyOperator(keyOperator)

        return collectionItems
            .filter { json ->
                val fieldValue = json.getFieldValue(key)
                fieldValue != null && values.any { value -> applyOperator(fieldValue, operator, value) }
            }.toMutableList()
    }

    /**
     * Parses a key-operator string into key and a [FilterOperator].
     *
     * @param keyOperator The key-operator string.
     *
     * @return A [Pair] containing the key and the corresponding [FilterOperator].
     */
    private fun parseKeyOperator(keyOperator: String): Pair<String, FilterOperator> {
        val parts = keyOperator.split("_", limit = 2)
        val key = parts[0]
        val operatorString = if (parts.size > 1) parts[1].removePrefix("_").uppercase() else return key to FilterOperator.EQ
        val operator = FilterOperator.valueOf(operatorString)
        return key to operator
    }

    /**
     * Applies a filter operator to a JSON field and a given value.
     *
     * @param field The JSON field value, supports JSON primitives and arrays.
     * @param operator The filter operator.
     * @param value The value from query to compare against.
     *
     * @return `true` if the condition matches, otherwise `false`.
     * @throws BadRequestException If filtering is attempted on an unsupported JSON type.
     */
    private fun applyOperator(
        field: JsonElement,
        operator: FilterOperator,
        value: String,
    ): Boolean =
        if (field is JsonPrimitive) {
            applyOperator(field, operator, value)
        } else if (field is JsonArray) {
            applyOperator(field, operator, value)
        } else {
            throw BadRequestException("JSON object is not supported for query parameter values.")
        }

    /**
     * Applies a filter operator to a JSON primitive and a given value.
     *
     * @param fieldValue The JSON primitive value.
     * @param operator The filter operator.
     * @param value The value from query to compare against.
     *
     * @return `true` if the condition matches, otherwise `false`.
     */
    private fun applyOperator(
        fieldValue: JsonPrimitive,
        operator: FilterOperator,
        value: String,
    ): Boolean =
        when (operator) {
            FilterOperator.EQ -> fieldValue.content == value
            FilterOperator.NE -> fieldValue.content != value
            FilterOperator.LT -> (fieldValue.doubleOrNull ?: Double.NaN) < value.toDouble()
            FilterOperator.GT -> (fieldValue.doubleOrNull ?: Double.NaN) > value.toDouble()
            FilterOperator.LTE -> (fieldValue.doubleOrNull ?: Double.NaN) <= value.toDouble()
            FilterOperator.GTE -> (fieldValue.doubleOrNull ?: Double.NaN) >= value.toDouble()
            FilterOperator.LIKE -> fieldValue.content.contains(value, ignoreCase = true)
        }

    /**
     * Applies a filter operator to a JSON array and a given value.
     *
     * @param fieldArray The JSON array.
     * @param operator The filter operator.
     * @param value The value from query to compare against.
     *
     * @return `true` if the condition matches, otherwise `false`.
     * @throws BadRequestException If the filter operator is not supported for arrays.
     */
    private fun applyOperator(
        fieldArray: JsonArray,
        operator: FilterOperator,
        value: String,
    ): Boolean =
        when (operator) {
            FilterOperator.EQ -> fieldArray.any { it is JsonPrimitive && applyOperator(it, operator, value) }
            FilterOperator.NE -> fieldArray.all { it is JsonPrimitive && applyOperator(it, operator, value) }
            FilterOperator.LIKE -> fieldArray.any { it is JsonPrimitive && applyOperator(it, operator, value) }
            else -> throw BadRequestException("Operator $operator is not supported for multiple values.")
        }
}
