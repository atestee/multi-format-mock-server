package cz.cvut.fit.atlasest.services

import cz.cvut.fit.atlasest.utils.getPropertyValue
import io.ktor.server.plugins.BadRequestException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

/**
 * A service for filtering collection items based on specified parameters
 */
class FilterService {
    /**
     * Applies filtering to a collection of collection items based on a given key with operator, and values
     *
     * @param collectionItems The list of collection items to be filtered
     * @param keyOperator The [String] with key and optional operator used for filtering, divided by underscore, for example "name_like",
     * for more information see `Route.getRoutes`.
     * @param values The list of values to filter against
     * @param getTypeAndFormatFromJsonSchema A function to retrieve data type and format from JSON schema
     *
     * @return A filtered list of JSON objects.
     */
    internal fun applyFilter(
        collectionItems: MutableList<JsonObject>,
        keyOperator: String,
        values: List<String>,
        getTypeAndFormatFromJsonSchema: (String) -> Pair<String, String?>,
    ): MutableList<JsonObject> {
        val (key, operator) = parseKeyOperator(keyOperator)

        var (type, format) = getTypeAndFormatFromJsonSchema(key)

        type =
            if (type == "string" && format != null) {
                format
            } else {
                type
            }

        return collectionItems
            .filter { json ->
                val propertyValue = json.getPropertyValue(key)
                propertyValue != null && values.any { value -> applyOperator(propertyValue, operator, value, type) }
            }.toMutableList()
    }

    /**
     * Parses a key-operator string into key and a [FilterOperator]
     *
     * @param keyOperator The key-operator string
     *
     * @return A [Pair] containing the key and the corresponding [FilterOperator]
     */
    private fun parseKeyOperator(keyOperator: String): Pair<String, FilterOperator> {
        val parts = keyOperator.split("_", limit = 2)
        val key = parts[0]
        val operatorString =
            if (parts.size > 1) parts[1].removePrefix("_").uppercase() else return key to FilterOperator.EQ
        val operator = FilterOperator.valueOf(operatorString)
        return key to operator
    }

    /**
     * Applies a filter operator to a JSON property and a given value
     *
     * @param field The JSON property value, supports [JsonPrimitive] and [JsonArray]
     * @param operator The filter operator
     * @param value The value from query to compare against
     * @param type data type or format of JSON field
     *
     * @return `true` if the condition matches, otherwise `false`
     * @throws BadRequestException If filtering is attempted on an unsupported [JsonElement]
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
            throw BadRequestException("JSON object is not supported for filter parameter values.")
        }

    /**
     * Applies a filter operator on a [JsonPrimitive] and a given value
     *
     * @param fieldValue The [JsonPrimitive] value on which to apply the operator
     * @param operator The filter operator
     * @param value The query parameter value to compare against
     * @param type data type or format of the field value
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
            FilterOperator.LIKE -> fieldValue.content.contains(value, ignoreCase = true)
            else -> applyOperatorByType(fieldValue, FilterOperatorNumeric.valueOf(operator.name), value, type)
        }

    /**
     * Applies filter operator LT, GT, LTE, GTE based on JSON Schema types `number` and `integer` or formats `date` or `date-time`
     *
     * @param fieldValue The [JsonPrimitive] value on which to apply the operator
     * @param operator The filter operator for numeric types
     * @param value The query parameter value to compare against
     * @param type data type or format of the field value
     *
     * @return `true` if the condition matches, otherwise `false`
     * @throws BadRequestException if the type or format is not one of `number`, `integer`, `date`, or `date-time`
     */
    private fun applyOperatorByType(
        fieldValue: JsonPrimitive,
        operator: FilterOperatorNumeric,
        value: String,
        type: String,
    ): Boolean =
        when (type) {
            "number", "integer" ->
                applyCompareOperator(
                    (fieldValue.doubleOrNull ?: Double.NaN),
                    value.toDouble(),
                    operator,
                )
            "date" -> {
                val aDate =
                    kotlin.runCatching { LocalDate.parse(fieldValue.content) }.getOrNull()
                        ?: throw BadRequestException("Date is not valid")
                val bDate =
                    kotlin.runCatching { LocalDate.parse(value) }.getOrNull()
                        ?: throw BadRequestException("Problem when parsing date string.")
                applyCompareOperator(aDate, bDate, operator)
            }
            "date-time" -> {
                val valueIsDateTime = runCatching { OffsetDateTime.parse(value) }.isSuccess
                if (valueIsDateTime) {
                    val aDate =
                        kotlin.runCatching { OffsetDateTime.parse(fieldValue.content) }.getOrNull()
                            ?: throw BadRequestException("Date is not valid")
                    val bDate =
                        kotlin.runCatching { OffsetDateTime.parse(value) }.getOrNull()
                            ?: throw BadRequestException("Problem when parsing date string.")
                    applyCompareOperator(aDate, bDate, operator)
                } else {
                    val aDateTime =
                        kotlin
                            .runCatching {
                                OffsetDateTime.parse(fieldValue.content).toLocalDate()
                            }.getOrNull()
                            ?: throw BadRequestException("Date is not valid")
                    val bDateTime =
                        kotlin
                            .runCatching {
                                LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE)
                            }.getOrNull()
                            ?: throw BadRequestException("Problem when parsing date string.")
                    val result = applyCompareOperator(aDateTime, bDateTime, operator)
                    println("${fieldValue.content}: $result")
                    result
                }
            }
            else -> throw BadRequestException("$operator operator is not supported for $type.")
        }

    /**
     * Applies filter operator LT, GT, LTE or GTE between two comparable values using the specified filter operator
     *
     * @param T The type of the values being compared. Must implement [Comparable]
     * @param a left operand
     * @param b right operand
     * @param operator the comparison operator to apply
     *
     * @return `true` if the condition matches, otherwise `false`
     */
    private fun <T : Comparable<T>> applyCompareOperator(
        a: T,
        b: T,
        operator: FilterOperatorNumeric,
    ): Boolean =
        when (operator) {
            FilterOperatorNumeric.LT -> a < b
            FilterOperatorNumeric.GT -> a > b
            FilterOperatorNumeric.LTE -> a <= b
            FilterOperatorNumeric.GTE -> a >= b
        }

    /**
     * Applies a filter operator to a [JsonArray] and a given value.
     *
     * @param fieldArray The [JsonArray] on which to apply the operator
     * @param operator The filter operator
     * @param value The query parameter value to compare against
     * @param type data type or format of JSON array items
     *
     * @return `true` if the condition matches, otherwise `false`
     * @throws BadRequestException If the filter operator is not supported for arrays
     */
    private fun applyOperator(
        fieldArray: JsonArray,
        operator: FilterOperator,
        value: String,
        type: String,
    ): Boolean {
        if (fieldArray.first() !is JsonPrimitive) throw BadRequestException("Operator $operator is not supported array of objects.")
        return when (operator) {
            FilterOperator.EQ -> fieldArray.any { it is JsonPrimitive && applyOperator(it, operator, value, type) }
            FilterOperator.NE -> fieldArray.all { it is JsonPrimitive && applyOperator(it, operator, value, type) }
            FilterOperator.LIKE -> fieldArray.any { it is JsonPrimitive && applyOperator(it, operator, value, type) }
            else -> throw BadRequestException("Operator $operator is not supported for multiple values.")
        }
    }
}
