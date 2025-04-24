package cz.cvut.fit.atlasest.service

import cz.cvut.fit.atlasest.application.AppConfig
import cz.cvut.fit.atlasest.exceptions.ParsingException
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.runs
import io.mockk.verify
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import javax.validation.ValidationException
import kotlin.test.Test
import kotlin.test.assertEquals

@ExtendWith(MockKExtension::class)
class CollectionServiceTest {
    @MockK
    lateinit var documentService: DocumentService

    @MockK
    lateinit var appConfig: AppConfig

    var schemaFilename: String? = null

    @MockK
    lateinit var schemaService: SchemaService

    private val collectionsFilename = "collections.json"
    private val identifiersFilename = "identifiers.json"
    private val collectionName = "collection1"

    @BeforeEach
    fun init() {
        every { appConfig.collectionsFilename } returns collectionsFilename
        every { appConfig.identifiersFileName } returns identifiersFilename
    }

    @Test
    fun `init - identifier is not a JSON primitive`() {
        every { documentService.readJsonFile(collectionsFilename) } returns
            JsonObject(
                mapOf(
                    collectionName to JsonArray(listOf()),
                ),
            )
        every { documentService.readJsonFile(identifiersFilename) } returns
            JsonObject(
                mapOf(
                    collectionName to JsonObject(mapOf()),
                ),
            )

        val exception =
            assertThrows<ParsingException> {
                CollectionService(
                    documentService = documentService,
                    appConfig = appConfig,
                    schemaFilename = schemaFilename,
                    schemaService = schemaService,
                )
            }

        assertEquals(
            "Invalid identifier for collection $collectionName in $identifiersFilename (must be a JSON primitive)",
            exception.message,
        )
    }

    @Test
    fun `init - collection is not a JSON array`() {
        every { documentService.readJsonFile(collectionsFilename) } returns
            JsonObject(
                mapOf(
                    collectionName to JsonPrimitive("not a JSON array"),
                ),
            )
        every { documentService.readJsonFile(identifiersFilename) } returns
            JsonObject(
                mapOf(
                    collectionName to JsonPrimitive("id"),
                ),
            )

        val exception =
            assertThrows<ParsingException> {
                CollectionService(
                    documentService = documentService,
                    appConfig = appConfig,
                    schemaFilename = schemaFilename,
                    schemaService = schemaService,
                )
            }

        assertEquals("Invalid value for $collectionName in ${appConfig.collectionsFilename} (must be a JSON array)", exception.message)
    }

    @Test
    fun `init - no identifier found for collection`() {
        every { documentService.readJsonFile(collectionsFilename) } returns
            JsonObject(
                mapOf(
                    collectionName to JsonArray(listOf(JsonObject(mapOf()))),
                ),
            )
        every { documentService.readJsonFile(identifiersFilename) } returns
            JsonObject(
                mapOf(
                    "differentCollection" to JsonPrimitive("id"),
                ),
            )

        val exception =
            assertThrows<ParsingException> {
                CollectionService(
                    documentService = documentService,
                    appConfig = appConfig,
                    schemaFilename = schemaFilename,
                    schemaService = schemaService,
                )
            }

        assertEquals("No identifier key found for collection '$collectionName'", exception.message)
    }

    @Test
    fun `init - collection item does not have identifier value`() {
        every { documentService.readJsonFile(collectionsFilename) } returns
            JsonObject(
                mapOf(
                    collectionName to
                        JsonArray(
                            listOf(
                                JsonObject(
                                    mapOf(
                                        "not identifier" to JsonPrimitive("not identifier value"),
                                    ),
                                ),
                            ),
                        ),
                ),
            )
        every { documentService.readJsonFile(identifiersFilename) } returns
            JsonObject(
                mapOf(
                    collectionName to JsonPrimitive("id"),
                ),
            )

        val exception =
            assertThrows<ParsingException> {
                CollectionService(
                    documentService = documentService,
                    appConfig = appConfig,
                    schemaFilename = schemaFilename,
                    schemaService = schemaService,
                )
            }

        assertEquals("Missing identifier value for item in collection $collectionName", exception.message)
    }

    @Test
    fun `init - collection item identifier value is not a JSON primitive`() {
        every { documentService.readJsonFile(collectionsFilename) } returns
            JsonObject(
                mapOf(
                    collectionName to
                        JsonArray(
                            listOf(
                                JsonObject(
                                    mapOf(
                                        "id" to JsonArray(listOf()),
                                    ),
                                ),
                            ),
                        ),
                ),
            )
        every { documentService.readJsonFile(identifiersFilename) } returns
            JsonObject(
                mapOf(
                    collectionName to JsonPrimitive("id"),
                ),
            )

        val exception =
            assertThrows<ParsingException> {
                CollectionService(
                    documentService = documentService,
                    appConfig = appConfig,
                    schemaFilename = schemaFilename,
                    schemaService = schemaService,
                )
            }

        assertEquals("Invalid identifier value for item in collection $collectionName (must be a JSON primitive)", exception.message)
    }

    @Test
    fun `init - collection item identifier value is not an integer or integer string`() {
        every { documentService.readJsonFile(collectionsFilename) } returns
            JsonObject(
                mapOf(
                    collectionName to
                        JsonArray(
                            listOf(
                                JsonObject(
                                    mapOf(
                                        "id" to
                                            JsonPrimitive(
                                                "not and integer string",
                                            ),
                                    ),
                                ),
                            ),
                        ),
                ),
            )
        every { documentService.readJsonFile(identifiersFilename) } returns
            JsonObject(
                mapOf(
                    collectionName to JsonPrimitive("id"),
                ),
            )

        val exception =
            assertThrows<ParsingException> {
                CollectionService(
                    documentService = documentService,
                    appConfig = appConfig,
                    schemaFilename = schemaFilename,
                    schemaService = schemaService,
                )
            }

        assertEquals("Invalid identifier value in collection $collectionName (must be an integer or integer string)", exception.message)
    }

    @Test
    fun `init - collection is empty`() {
        every { documentService.readJsonFile(collectionsFilename) } returns
            JsonObject(
                mapOf(
                    collectionName to
                        JsonArray(
                            listOf(
                                JsonObject(
                                    mapOf(
                                        "id" to
                                            JsonPrimitive(
                                                "not and integer string",
                                            ),
                                    ),
                                ),
                            ),
                        ),
                ),
            )
        every { documentService.readJsonFile(identifiersFilename) } returns
            JsonObject(
                mapOf(
                    collectionName to JsonPrimitive("id"),
                ),
            )

        val exception =
            assertThrows<ParsingException> {
                CollectionService(
                    documentService = documentService,
                    appConfig = appConfig,
                    schemaFilename = schemaFilename,
                    schemaService = schemaService,
                )
            }

        assertEquals("Invalid identifier value in collection $collectionName (must be an integer or integer string)", exception.message)
    }

    @Test
    fun `init - when schemaFilename is null - a valid schema is inferred`() {
        val collection =
            JsonArray(
                listOf(
                    JsonObject(
                        mapOf(
                            "id" to JsonPrimitive(1),
                        ),
                    ),
                ),
            )
        every { documentService.readJsonFile(collectionsFilename) } returns
            JsonObject(
                mapOf(
                    collectionName to collection,
                ),
            )
        every { documentService.readJsonFile(identifiersFilename) } returns
            JsonObject(
                mapOf(
                    collectionName to JsonPrimitive("id"),
                ),
            )
        every { schemaService.inferJsonSchema(any()) } returns
            JsonObject(
                mapOf(
                    "properties" to JsonPrimitive(1),
                ),
            )

        CollectionService(
            documentService = documentService,
            appConfig = appConfig,
            schemaFilename = schemaFilename,
            schemaService = schemaService,
        )

        verify { schemaService.inferJsonSchema(collection) }
    }

    @Test
    fun `init - when schemaFilename is not null but schema for collection does not exist - the schema is inferred`() {
        val item =
            JsonObject(
                mapOf(
                    "id" to JsonPrimitive(1),
                ),
            )
        val collection = JsonArray(listOf(item))
        val schemaCollection = JsonObject(mapOf())
        val schemaFilename2 = "schema.json"
        every { documentService.readJsonFile(collectionsFilename) } returns
            JsonObject(
                mapOf(
                    collectionName to collection,
                ),
            )
        every { documentService.readJsonFile(identifiersFilename) } returns
            JsonObject(
                mapOf(
                    collectionName to JsonPrimitive("id"),
                ),
            )
        every { documentService.readJsonFile(schemaFilename2) } returns schemaCollection
        every { schemaService.getCollectionSchema(collectionName, schemaCollection) } returns null
        every { schemaService.inferJsonSchema(any()) } returns
            JsonObject(
                mapOf(
                    "properties" to JsonPrimitive(1),
                ),
            )

        CollectionService(
            documentService = documentService,
            appConfig = appConfig,
            schemaFilename = schemaFilename2,
            schemaService = schemaService,
        )

        verify { schemaService.getCollectionSchema(collectionName, schemaCollection) }
        verify { schemaService.inferJsonSchema(collection) }
    }

    @Test
    fun `init - when schema is provided and collection contains invalid item - a ValidationException is thrown`() {
        val item =
            JsonObject(
                mapOf(
                    "id" to JsonPrimitive(1),
                ),
            )
        val collection = JsonArray(listOf(item))
        val schema = JsonObject(mapOf("properties" to JsonObject(mapOf("id" to JsonPrimitive(1)))))
        val schemaCollection = JsonObject(mapOf(collectionName to schema))
        val schemaFilename2 = "schema.json"
        every { documentService.readJsonFile(collectionsFilename) } returns
            JsonObject(
                mapOf(
                    collectionName to collection,
                ),
            )
        every { documentService.readJsonFile(identifiersFilename) } returns
            JsonObject(
                mapOf(
                    collectionName to JsonPrimitive("id"),
                ),
            )
        every { documentService.readJsonFile(schemaFilename2) } returns schemaCollection
        every { schemaService.getCollectionSchema(collectionName, schemaCollection) } returns schema
        every { schemaService.validateDataAgainstSchema(item, schema) } throws ValidationException("error")

        val exception =
            assertThrows<ValidationException> {
                CollectionService(
                    documentService = documentService,
                    appConfig = appConfig,
                    schemaFilename = schemaFilename2,
                    schemaService = schemaService,
                )
            }

        assertEquals("error", exception.message)

        verify { schemaService.getCollectionSchema(collectionName, schemaCollection) }
        verify { schemaService.validateDataAgainstSchema(item, schema) }
    }

    @Test
    fun `init - when schema is provided and collection does not contain invalid item - no exception is thrown`() {
        val item =
            JsonObject(
                mapOf(
                    "id" to JsonPrimitive(1),
                ),
            )
        val collection = JsonArray(listOf(item))
        val schema = JsonObject(mapOf("properties" to JsonObject(mapOf("id" to JsonPrimitive(1)))))
        val schemaCollection = JsonObject(mapOf(collectionName to schema))
        val schemaFilename2 = "schema.json"
        every { documentService.readJsonFile(collectionsFilename) } returns
            JsonObject(
                mapOf(
                    collectionName to collection,
                ),
            )
        every { documentService.readJsonFile(identifiersFilename) } returns
            JsonObject(
                mapOf(
                    collectionName to JsonPrimitive("id"),
                ),
            )
        every { documentService.readJsonFile(schemaFilename2) } returns schemaCollection
        every { schemaService.getCollectionSchema(collectionName, schemaCollection) } returns schema
        every { schemaService.validateDataAgainstSchema(item, schema) } just runs

        assertDoesNotThrow {
            CollectionService(
                documentService = documentService,
                appConfig = appConfig,
                schemaFilename = schemaFilename2,
                schemaService = schemaService,
            )
        }

        verify { schemaService.getCollectionSchema(collectionName, schemaCollection) }
        verify { schemaService.validateDataAgainstSchema(item, schema) }
    }
}
