package cz.cvut.fit.atlasest.service

import cz.cvut.fit.atlasest.exceptions.InvalidDataException
import cz.cvut.fit.atlasest.utils.getFieldValue
import io.ktor.server.plugins.BadRequestException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import java.time.LocalDate

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
     * @param getTypeAndFormatFromJsonSchema A function to retrieve data type and format from JSON schema.
     *
     * @return A filtered list of JSON objects.
     */
    internal fun applyFilter(
        collectionItems: MutableList<JsonObject>,
        keyOperator: String,
        values: List<String>,
        getTypeAndFormatFromJsonSchema: (String) -> Pair<String, String?>?,
    ): MutableList<JsonObject> {
        val (key, operator) = parseKeyOperator(keyOperator)

        var (type, format) =
            getTypeAndFormatFromJsonSchema(key)
                ?: throw InvalidDataException("Type for $keyOperator was not found")

        type =
            if (type == "string" && format != null) {
                format
            } else {
                type
            }

        return collectionItems
            .filter { json ->
                val fieldValue = json.getFieldValue(key)
                fieldValue != null && values.any { value -> applyOperator(fieldValue, operator, value, type) }
            }.toMutableList()
    }

    /**
     * Parses a key-operator string into key and a [FilterOperator].
     *
     * @param keyOperator The key-operator string.
     *
     * @return A [Pair] containing the key and the corresponding [FilterOperator].
     */
    internal fun parseKeyOperator(keyOperator: String): Pair<String, FilterOperator> {
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
     * @param type data type or format of JSON field.
     *
     * @return `true` if the condition matches, otherwise `false`.
     * @throws BadRequestException If filtering is attempted on an unsupported JSON type.
     */
    private fun applyOperator(
        field: JsonElement,
        operator: FilterOperator,
        value: String,
        type: String,
    ): Boolean =
        if (field is JsonPrimitive) {
            applyOperator(field, operator, value, type)
        } else if (field is JsonArray) {
            applyOperator(field, operator, value, type)
        } else {
            throw BadRequestException("JSON object is not supported for query parameter values.")
        }

    /**
     * Applies a filter operator to a JSON primitive and a given value.
     *
     * @param fieldValue The JSON primitive value.
     * @param operator The filter operator.
     * @param value The value from query to compare against.
     * @param type data type or format of the field value.
     *
     * @return `true` if the condition matches, otherwise `false`.
     */
    private fun applyOperator(
        fieldValue: JsonPrimitive,
        operator: FilterOperator,
        value: String,
        type: String,
    ): Boolean =
        when (operator) {
            FilterOperator.EQ -> fieldValue.content == value
            FilterOperator.NE -> fieldValue.content != value
            FilterOperator.LT -> lt(fieldValue, value, type)
            FilterOperator.GT -> gt(fieldValue, value, type)
            FilterOperator.LTE -> lte(fieldValue, value, type)
            FilterOperator.GTE -> gte(fieldValue, value, type)
            FilterOperator.LIKE -> fieldValue.content.contains(value, ignoreCase = true)
        }

    /**
     * Applies a filter operator to a JSON array and a given value.
     *
     * @param fieldArray The JSON array.
     * @param operator The filter operator.
     * @param value The value from query to compare against.
     * @param type data type or format of JSON array items.
     *
     * @return `true` if the condition matches, otherwise `false`.
     * @throws BadRequestException If the filter operator is not supported for arrays.
     */
    private fun applyOperator(
        fieldArray: JsonArray,
        operator: FilterOperator,
        value: String,
        type: String,
    ): Boolean =
        when (operator) {
            FilterOperator.EQ -> fieldArray.any { it is JsonPrimitive && applyOperator(it, operator, value, type) }
            FilterOperator.NE -> fieldArray.all { it is JsonPrimitive && applyOperator(it, operator, value, type) }
            FilterOperator.LIKE -> fieldArray.any { it is JsonPrimitive && applyOperator(it, operator, value, type) }
            else -> throw BadRequestException("Operator $operator is not supported for multiple values.")
        }

    /**
     * Compares two values of a given type and returns whether `a` is less than `b`.
     *
     * This function supports comparison for types: "number", "integer", and "date".
     *
     * @param a The first value as a JSON primitive. Its content will be parsed based on the specified type.
     * @param b The second value as a String. This string will be parsed to the same type as "a" for comparison.
     * @param type The data type to use for comparison. Supported values: "number", "integer", "date".
     *
     * @return true if `a` is less than `b`, otherwise false.
     *
     * @throws BadRequestException If the type is unsupported or if `a` is an invalid date.
     * @throws InvalidDataException If `b` is an invalid date when type is "date".
     */
    private fun lt(
        a: JsonPrimitive,
        b: String,
        type: String,
    ): Boolean =
        when (type) {
            "number" -> (a.doubleOrNull ?: Double.NaN) < b.toDouble()
            "integer" -> (a.doubleOrNull ?: Double.NaN) < b.toDouble()
            "date" -> {
                val aDate =
                    kotlin.runCatching { LocalDate.parse(a.content) }.getOrNull()
                        ?: throw BadRequestException("Date is not valid")
                val bDate =
                    kotlin.runCatching { LocalDate.parse(b) }.getOrNull()
                        ?: throw InvalidDataException("Problem when parsing date string.")
                aDate < bDate
            }
            else -> throw BadRequestException("LT operator is not supported for $type.")
        }

    /**
     * Compares two values of a given type and returns whether `a` is less than or equal to `b`.
     *
     * This function supports comparison for types: "number", "integer", and "date".
     *
     * @param a The first value as a JSON primitive. Its content will be parsed based on the specified type.
     * @param b The second value as a String. This string will be parsed to the same type as "a" for comparison.
     * @param type The data type to use for comparison. Supported values: "number", "integer", "date".
     *
     * @return true if `a` is less than or equal to `b`, otherwise false.
     *
     * @throws BadRequestException If the type is unsupported or if `a` is an invalid date.
     * @throws InvalidDataException If `b` is an invalid date when type is "date".
     */
    private fun lte(
        a: JsonPrimitive,
        b: String,
        type: String,
    ): Boolean =
        when (type) {
            "number" -> (a.doubleOrNull ?: Double.NaN) <= b.toDouble()
            "integer" -> (a.doubleOrNull ?: Double.NaN) <= b.toDouble()
            "date" -> {
                val aDate =
                    kotlin.runCatching { LocalDate.parse(a.content) }.getOrNull()
                        ?: throw BadRequestException("Date is not valid")
                val bDate =
                    kotlin.runCatching { LocalDate.parse(b) }.getOrNull()
                        ?: throw InvalidDataException("Problem when parsing date string.")
                aDate <= bDate
            }
            else -> throw BadRequestException("LT operator is not supported for $type.")
        }

    /**
     * Compares two values of a given type and returns whether `a` is greater than `b`.
     *
     * This function supports comparison for types: "number", "integer", and "date".
     *
     * @param a The first value as a JSON primitive. Its content will be parsed based on the specified type.
     * @param b The second value as a String. This string will be parsed to the same type as "a" for comparison.
     * @param type The data type to use for comparison. Supported values: "number", "integer", "date".
     *
     * @return true if `a` is greater than `b`, otherwise false.
     *
     * @throws BadRequestException If the type is unsupported or if `a` is an invalid date.
     * @throws InvalidDataException If `b` is an invalid date when type is "date".
     */
    private fun gt(
        a: JsonPrimitive,
        b: String,
        type: String,
    ): Boolean =
        when (type) {
            "number" -> (a.doubleOrNull ?: Double.NaN) > b.toDouble()
            "integer" -> (a.doubleOrNull ?: Double.NaN) > b.toDouble()
            "date" -> {
                val aDate =
                    kotlin.runCatching { LocalDate.parse(a.content) }.getOrNull()
                        ?: throw BadRequestException("Date is not valid")
                val bDate =
                    kotlin.runCatching { LocalDate.parse(b) }.getOrNull()
                        ?: throw InvalidDataException("Problem when parsing date string.")
                aDate > bDate
            }
            else -> throw BadRequestException("LT operator is not supported for $type.")
        }

    /**
     * Compares two values of a given type and returns whether `a` is greater than or equal to `b`.
     *
     * This function supports comparison for types: "number", "integer", and "date".
     *
     * @param a The first value as a JSON primitive. Its content will be parsed based on the specified type.
     * @param b The second value as a String. This string will be parsed to the same type as "a" for comparison.
     * @param type The data type to use for comparison. Supported values: "number", "integer", "date".
     *
     * @return true if `a` is greater than or equal to `b`, otherwise false.
     *
     * @throws BadRequestException If the type is unsupported or if `a` is an invalid date.
     * @throws InvalidDataException If `b` is an invalid date when type is "date".
     */
    private fun gte(
        a: JsonPrimitive,
        b: String,
        type: String,
    ): Boolean =
        when (type) {
            "number" -> (a.doubleOrNull ?: Double.NaN) >= b.toDouble()
            "integer" -> (a.doubleOrNull ?: Double.NaN) >= b.toDouble()
            "date" -> {
                val aDate =
                    kotlin.runCatching { LocalDate.parse(a.content) }.getOrNull()
                        ?: throw BadRequestException("Date is not valid")
                val bDate =
                    kotlin.runCatching { LocalDate.parse(b) }.getOrNull()
                        ?: throw InvalidDataException("Problem when parsing date string.")
                aDate >= bDate
            }
            else -> throw BadRequestException("LT operator is not supported for $type.")
        }
}
