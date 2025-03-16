package cz.cvut.fit.atlasest.service

import BaseTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.assertDoesNotThrow
import org.koin.test.inject

const val REQUIRED = "required"
const val TYPE = "type"
const val PROPERTIES = "properties"
const val INT = "integer"
const val OBJECT = "object"

const val ID = "id"
const val TITLE = "title"
const val AUTHOR = "author"
const val GENRE = "genre"
const val YEAR = "year"
const val MONTH = "month"
const val DAY = "day"
const val RELEASE_DATE = "releaseDate"

class SchemaServiceTest : BaseTest() {
    private val schemaService by inject<SchemaService>()

    @Test
    fun `inferJsonSchema - properties that are in all objects will be in required`() =
        testWithApp {
            val schema =
                schemaService.inferJsonSchema(
                    JsonArray(
                        listOf(
                            generateJsonObject(1, "title1", genre = "genre1"),
                            generateJsonObject(2, "title2", author = "author1"),
                        ),
                    ),
                )
            val schemaObject = Json.parseToJsonElement(schema.toString()).jsonObject
            val required = schemaObject[REQUIRED]?.jsonArray
            val expectedRequired = JsonArray(listOf(JsonPrimitive(ID), JsonPrimitive(TITLE)))
            assertNotNull(required)
            assertEquals(expectedRequired, required)
        }

    @Test
    fun `inferJsonSchema - nested object has correct types and required array`() =
        testWithApp {
            val item1 =
                generateJsonObject(1, "title1", genre = "genre1").add(
                    RELEASE_DATE,
                    generateJsonObject(year = 2020, month = 10, day = 11),
                )
            val item2 =
                generateJsonObject(2, "title2", author = "author2").add(
                    RELEASE_DATE,
                    generateJsonObject(year = 2021, month = 11),
                )

            val schema = schemaService.inferJsonSchema(JsonArray(listOf(item1, item2)))
            val schemaObject = Json.parseToJsonElement(schema.toString()).jsonObject
            val nestedObjectProps = schemaObject[PROPERTIES]?.jsonObject?.get(RELEASE_DATE)?.jsonObject
            assertNotNull(nestedObjectProps)
            val nestedRequired = nestedObjectProps[REQUIRED]?.jsonArray
            val expectedNestedRequired = JsonArray(listOf(JsonPrimitive(YEAR), JsonPrimitive(MONTH)))
            assertNotNull(nestedRequired)
            assertEquals(expectedNestedRequired, nestedRequired)
            assertEquals(JsonPrimitive(OBJECT), nestedObjectProps[TYPE]?.jsonPrimitive)
            assertEquals(
                JsonPrimitive(INT),
                nestedObjectProps[PROPERTIES]
                    ?.jsonObject
                    ?.get(YEAR)
                    ?.jsonObject
                    ?.get(TYPE),
            )
            assertEquals(
                JsonPrimitive(INT),
                nestedObjectProps[PROPERTIES]
                    ?.jsonObject
                    ?.get(MONTH)
                    ?.jsonObject
                    ?.get(TYPE),
            )
            assertEquals(
                JsonPrimitive(INT),
                nestedObjectProps[PROPERTIES]
                    ?.jsonObject
                    ?.get(DAY)
                    ?.jsonObject
                    ?.get(TYPE),
            )
        }

    @Test
    fun `validateDataAgainstSchema - given valid data - should no throw exception`() =
        testWithApp {
            val item1 =
                generateJsonObject(1, "title1", genre = "genre1").add(
                    RELEASE_DATE,
                    generateJsonObject(year = 2020, month = 10, day = 11),
                )
            val item2 =
                generateJsonObject(2, "title2", author = "author2").add(
                    RELEASE_DATE,
                    generateJsonObject(year = 2021, month = 11),
                )
            val schema = schemaService.inferJsonSchema(JsonArray(listOf(item1, item2)))
            val insertedItem =
                generateJsonObject(3, "title3", author = "author3").add(
                    RELEASE_DATE,
                    generateJsonObject(year = 2022, month = 12),
                )

            assertDoesNotThrow { schemaService.validateDataAgainstSchema(insertedItem, schema) }
        }

    @Test
    fun `validateDataAgainstSchema - given data with missing author and year - should throw exception with invalid fields`() =
        testWithApp {
            val item1 =
                generateJsonObject(1, "title1", genre = "genre1").add(
                    RELEASE_DATE,
                    generateJsonObject(year = 2020, month = 10, day = 11),
                )
            val item2 =
                generateJsonObject(2, "title2", author = "author2").add(
                    RELEASE_DATE,
                    generateJsonObject(year = 2021, month = 11),
                )
            val schema = schemaService.inferJsonSchema(JsonArray(listOf(item1, item2)))
            val insertedItem =
                generateJsonObject(3, author = "author3").add(
                    RELEASE_DATE,
                    generateJsonObject(month = 12),
                )

            val exception =
                assertFailsWith<javax.validation.ValidationException> {
                    schemaService.validateDataAgainstSchema(
                        insertedItem,
                        schema,
                    )
                }

            assertEquals(
                listOf(
                    generateValidationError("year", "/$RELEASE_DATE"),
                    generateValidationError("title"),
                ).toString(),
                exception.message,
            )
        }
}

fun generateJsonObject(
    id: Int? = null,
    title: String? = null,
    author: String? = null,
    genre: String? = null,
    year: Int? = null,
    month: Int? = null,
    day: Int? = null,
): JsonObject =
    JsonObject(
        buildMap {
            id?.let { put(ID, JsonPrimitive(it)) }
            title?.let { put(TITLE, JsonPrimitive(it)) }
            author?.let { put(AUTHOR, JsonPrimitive(it)) }
            genre?.let { put(GENRE, JsonPrimitive(it)) }
            year?.let { put(YEAR, JsonPrimitive(it)) }
            month?.let { put(MONTH, JsonPrimitive(it)) }
            day?.let { put(DAY, JsonPrimitive(it)) }
        },
    )

fun generateValidationError(
    property: String,
    path: String? = null,
): String =
    if (path != null) {
        "#$path: missing required properties: [$property]"
    } else {
        "#: missing required properties: [$property]"
    }

fun generateValidationError(
    properties: List<String>,
    path: String? = null,
): String =
    if (path != null) {
        "#$path: missing required properties: $properties"
    } else {
        "#: missing required properties: $properties"
    }
