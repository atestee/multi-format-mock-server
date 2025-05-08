package cz.cvut.fit.atlasest.services

import BaseTest
import cz.cvut.fit.atlasest.exceptionHandling.InvalidDataException
import cz.cvut.fit.atlasest.testData.TestData
import cz.cvut.fit.atlasest.utils.add
import cz.cvut.fit.atlasest.utils.toJsonObject
import io.swagger.v3.oas.models.SpecVersion
import io.swagger.v3.oas.models.media.Schema
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.koin.test.inject
import javax.validation.ValidationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

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

private val testData = TestData()

class SchemaServiceTest : BaseTest() {
    private val schemaService by inject<SchemaService>()

    @Test
    fun `inferJsonSchema - when given complex JSON object - should return corresponding JSON schema`() =
        testWithApp {
            val json = testData.generateComplexJsonObject()
            val expectedSchema = testData.complexSchema.toJsonObject()

            val actualSchema = schemaService.inferJsonSchema(JsonArray(listOf(json)))

            assertEquals(expectedSchema, actualSchema)
        }

    @Test
    fun `inferJsonSchema - when all collection items contain the same properties - the properties should be in required`() =
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
            val schemaObject = schema.toString().toJsonObject()
            val required = schemaObject[REQUIRED]?.jsonArray
            val expectedRequired = JsonArray(listOf(JsonPrimitive(ID), JsonPrimitive(TITLE)))
            assertNotNull(required)
            assertEquals(expectedRequired, required)
        }

    @Test
    fun `inferJsonSchema - when nested object with incomplete properties - nested schema should have all props but required should have only subset of props`() =
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
            val schemaObject = schema.toString().toJsonObject()
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
    fun `convertTypesAndValidate - when given schema with missing property - should throw ValidationException`() {
        val json = testData.generateSimpleJsonObject()
        val schema = testData.generateSimpleSchema(simpleProp2 = null)

        val exception =
            assertThrows<ValidationException> {
                schemaService.convertTypes(schema, json)
            }

        assertEquals("#: missing property simpleProperty2 in schema", exception.message)
    }

    @Test
    fun `convertTypesAndValidate - when given schema with missing nested properties - should throw ValidationException`() {
        val json = testData.generateSimpleJsonObject()
        val schema =
            testData.generateSimpleSchema(
                objectProp =
                    JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("object"),
                            "required" to
                                JsonArray(
                                    listOf(
                                        JsonPrimitive("prop"),
                                    ),
                                ),
                        ),
                    ),
            )

        val exception =
            assertThrows<ValidationException> {
                schemaService.convertTypes(schema, json)
            }

        assertEquals("#/objectProperty: missing properties", exception.message)
    }

    @Test
    fun `convertTypesAndValidate - when schema is missing items for array property - should throw ValidationException`() {
        val json = testData.generateSimpleJsonObject()
        val schema =
            testData.generateSimpleSchema(
                arrayProp =
                    JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("array"),
                        ),
                    ),
            )

        val exception =
            assertThrows<ValidationException> {
                schemaService.convertTypes(schema, json)
            }

        assertEquals("#/arrayProperty: missing items field", exception.message)
    }

    @Test
    fun `convertTypesAndValidate - when given schema with invalid type property - should throw ValidationException`() {
        val json = testData.generateSimpleJsonObject()
        val schema =
            testData.generateSimpleSchema(
                simpleProp1 =
                    JsonObject(
                        mapOf(
                            "notType" to JsonPrimitive("string"),
                        ),
                    ),
            )

        val exception =
            assertThrows<ValidationException> {
                schemaService.convertTypes(schema, json)
            }

        assertEquals("#: missing type field", exception.message)
    }

    @Test
    fun `validateItemAgainstSchema - when given valid data - should not throw exception`() =
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

            assertDoesNotThrow { schemaService.validateItemAgainstSchema(insertedItem, schema) }
        }

    @Test
    fun `validateItemAgainstSchema - when author and year is missing - should throw exception with invalid fields`() =
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
                assertFailsWith<ValidationException> {
                    schemaService.validateItemAgainstSchema(
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

    @Test
    fun `validateCollectionAgainstSchema - when author and year is missing - should throw exception with invalid fields`() =
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
                assertFailsWith<ValidationException> {
                    schemaService.validateCollectionAgainstSchema(
                        listOf(item1, item2, insertedItem),
                        "collection",
                        ID,
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

    @Test
    fun `convertTypes - when valid schema and json with wrong types - should return json with correct types`() {
        val jsonWithIncorrectTypes = testData.generateComplexJsonObject(strings = true, flattenArrayWithOneItem = true)
        val jsonWithCorrectTypes = testData.generateComplexJsonObject()
        val schema = testData.complexSchema.toJsonObject()

        val result = schemaService.convertTypes(schema, jsonWithIncorrectTypes)

        assertEquals(jsonWithCorrectTypes, result)
    }

    @Test
    fun `convertTypes - when given schema with missing properties - should throw ValidationException`() {
        val json = testData.generateSimpleJsonObject()
        val schema =
            JsonObject(
                mapOf(
                    "type" to JsonPrimitive("object"),
                    "required" to
                        JsonArray(
                            listOf(
                                JsonPrimitive("simpleProperty1"),
                                JsonPrimitive("simpleProperty2"),
                                JsonPrimitive("objectProperty"),
                                JsonPrimitive("arrayProperty"),
                            ),
                        ),
                ),
            )

        val exception =
            assertThrows<ValidationException> {
                schemaService.convertTypes(schema, json)
            }

        assertEquals("#: missing properties field", exception.message)
    }

    @Test
    fun `getCollectionSchema - when given valid collectionName and jsonSchemaCollection - should return right schema`() {
        val schema = testData.generateSimpleSchema()
        val collectionName = "testName"
        val schemaCollection =
            JsonObject(
                mapOf(
                    collectionName to schema,
                ),
            )

        val result = schemaService.getCollectionSchema(collectionName, schemaCollection)

        assertEquals(schema, result)
    }

    @Test
    fun `getCollectionSchema - when schema is not a JsonObject - should throw ParsingException`() {
        val collectionName = "testName"
        val schemaCollection =
            JsonObject(
                mapOf(
                    collectionName to JsonPrimitive("test"),
                ),
            )

        val exception =
            assertThrows<InvalidDataException> {
                schemaService.getCollectionSchema(collectionName, schemaCollection)
            }

        assertEquals("Schema for collection '$collectionName' is not a JSON object", exception.message)
    }

    @Test
    fun `getCollectionSchema - when schema version is missing  - should throw ParsingException`() {
        val schema = testData.generateSimpleSchema(schemaVersion = null)
        val collectionName = "testName"
        val schemaCollection =
            JsonObject(
                mapOf(
                    collectionName to schema,
                ),
            )

        val exception =
            assertThrows<InvalidDataException> {
                schemaService.getCollectionSchema(collectionName, schemaCollection)
            }

        assertEquals("Invalid or missing schema version (must be a JSON primitive)", exception.message)
    }

    @Test
    fun `getCollectionSchema - when schema version is not supported  - should throw ValidationException`() {
        val schema = testData.generateSimpleSchema(schemaVersion = "wrong version")
        val supportedSpecVersion = "https://json-schema.org/draft/2020-12/schema#"
        val collectionName = "testName"
        val schemaCollection =
            JsonObject(
                mapOf(
                    collectionName to schema,
                ),
            )

        val exception =
            assertThrows<ValidationException> {
                schemaService.getCollectionSchema(collectionName, schemaCollection)
            }

        assertEquals(
            "Unsupported schema version. The supported version is $supportedSpecVersion",
            exception.message,
        )
    }

    @Test
    fun `convertJsonSchemaToOpenApi - when given JSON object schema with mixed data types - should throw InvalidDataException`() {
        val schema =
            """
            {
                "${"$"}schema": "https://json-schema.org/draft/2020-12/schema#",
                "type": "object",
                "properties": {
                    "name": {
                        "type": ["string", "integer"]
                    }
                }
            }
            """.trimIndent()

        val exception =
            assertFailsWith<InvalidDataException> {
                schemaService.convertJsonSchemaToOpenApi(schema.toJsonObject(), "collection")
            }

        assertEquals("Data contains mixed data types for collection.name", exception.message)
    }

    @Test
    fun `convertJsonSchemaToOpenApi - when given JSON object schema - should return corresponding OpenAPI schema`() {
        val jsonSchema = testData.complexSchema.toJsonObject()

        val openApiSchema = schemaService.convertJsonSchemaToOpenApi(jsonSchema, "collection")

        assertEquals(setOf("object"), openApiSchema.types)
        assertEquals(SpecVersion.V31, openApiSchema.specVersion)
        assertNotNull(openApiSchema.properties)
        assertEquals(
            listOf(
                "name",
                "age",
                "gpa",
                "student",
                "birthday",
                "classmates",
                "teachers",
                "schools",
                "subjects",
            ).sorted(),
            openApiSchema.required,
        )

        assertAndGetProperty(openApiSchema, "name", "string")
        assertAndGetProperty(openApiSchema, "age", "integer")
        assertAndGetProperty(openApiSchema, "gpa", "number")
        assertAndGetProperty(openApiSchema, "student", "boolean")

        val birthdaySchema = assertAndGetProperty(openApiSchema, "birthday", "object")
        assertAndGetProperty(birthdaySchema, "year", "integer")
        assertAndGetProperty(birthdaySchema, "month", "integer")
        assertAndGetProperty(birthdaySchema, "day", "integer")
        assertAndGetProperty(birthdaySchema, "place", "string")
        assertEquals(
            listOf(
                "year",
                "month",
                "day",
                "place",
            ).sorted(),
            birthdaySchema.required,
        )

        val classmatesSchema = assertAndGetProperty(openApiSchema, "classmates", "array")
        assertEquals(setOf("string"), classmatesSchema.items.types)

        val teachersSchema = assertAndGetProperty(openApiSchema, "teachers", "array")
        assertEquals(setOf("string"), teachersSchema.items.types)

        val schoolsSchema = assertAndGetProperty(openApiSchema, "schools", "array").items
        assertAndGetProperty(schoolsSchema, "name", "string")
        assertEquals(listOf("name"), schoolsSchema.required)

        val subjectsSchema = assertAndGetProperty(openApiSchema, "subjects", "array").items
        assertAndGetProperty(subjectsSchema, "name", "string")
        assertAndGetProperty(subjectsSchema, "code", "integer")
        assertAndGetProperty(subjectsSchema, "grade", "number")
        assertAndGetProperty(subjectsSchema, "passed", "boolean")
        assertEquals(
            listOf(
                "name",
                "code",
                "grade",
                "passed",
            ).sorted(),
            subjectsSchema.required,
        )
    }
}

fun assertAndGetProperty(
    openApiSchema: Schema<*>,
    propertyName: String,
    propertyType: String,
): Schema<Any> {
    val property = openApiSchema.properties[propertyName]
    assertNotNull(property)
    assertEquals(setOf(propertyType), property.types)
    return property
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
