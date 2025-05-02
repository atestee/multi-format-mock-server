package cz.cvut.fit.atlasest.services

import cz.cvut.fit.atlasest.exceptionHandling.NotAcceptableException
import cz.cvut.fit.atlasest.exceptionHandling.UnsupportedMediaTypeException
import io.ktor.http.ContentType
import io.ktor.server.plugins.BadRequestException
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@ExtendWith(MockKExtension::class)
class ContentNegotiationServiceTest {
    @MockK
    private lateinit var collectionServiceMock: CollectionService

    @MockK
    private lateinit var schemaServiceMock: SchemaService

    @InjectMockKs
    private lateinit var contentNegotiationService: ContentNegotiationService

    private val json = JsonObject(mapOf("key" to JsonPrimitive("value")))
    private val xml = "<item><key>value</key></item>"
    private val csv = "key\nvalue"
    private val collectionName = "collection"
    val schema = JsonObject(mapOf())

    @Test
    fun `getResourceInJsonFormat - when req body has invalid JSON - should throw BadRequestException`() {
        every { collectionServiceMock.getCollectionSchema(collectionName) } returns schema
        every { schemaServiceMock.convertTypes(schema, any()) } returns json

        val exception =
            assertFailsWith<BadRequestException> {
                contentNegotiationService.getResourceInJsonFormat(collectionName, "[1,2,3]", JSON_MIME)
            }
        assertEquals("Problem when parsing JSON body (must be a JSON object)", exception.message)
    }

    @Test
    fun `getResourceInJsonFormat - when content type is not supported - should throw BadRequestException`() {
        every { collectionServiceMock.getCollectionSchema(collectionName) } returns schema
        every { schemaServiceMock.convertTypes(schema, any()) } returns json

        val exception =
            assertFailsWith<UnsupportedMediaTypeException> {
                contentNegotiationService.getResourceInJsonFormat(collectionName, json.toString(), "text/html")
            }
        assertEquals("Unsupported media type text/html. Supported types are: [$JSON_MIME, $XML_MIME, $CSV_MIME]", exception.message)
    }

    @Test
    fun `getResourceInJsonFormat - when given valid JSON - should respond with JSON`() {
        every { collectionServiceMock.getCollectionSchema(collectionName) } returns schema
        every { schemaServiceMock.convertTypes(schema, any()) } returns json

        val result = contentNegotiationService.getResourceInJsonFormat(collectionName, json.toString(), JSON_MIME)
        assertEquals(json, result)
    }

    @Test
    fun `getResourceInJsonFormat - when req body is valid XML - should respond with converted JSON`() {
        every { collectionServiceMock.getCollectionSchema(collectionName) } returns schema
        every { schemaServiceMock.convertTypes(schema, any()) } returns json

        val result = contentNegotiationService.getResourceInJsonFormat(collectionName, xml, XML_MIME)
        assertEquals(json, result)
    }

    @Test
    fun `getResourceInJsonFormat - when req body is valid CSV - should respond with converted JSON`() {
        every { collectionServiceMock.getCollectionSchema(collectionName) } returns schema
        every { schemaServiceMock.convertTypes(schema, any()) } returns json

        val result = contentNegotiationService.getResourceInJsonFormat(collectionName, csv, CSV_MIME)
        assertEquals(json, result)
    }

    @Test
    fun `getResourceInAcceptedFormat - when accept is json  - should return data in json format`(): Unit =
        runBlocking {
            val (resultData, resultType) = contentNegotiationService.getResourceInAcceptedFormat(json, JSON_MIME)

            assertEquals(resultData, json.toString())
            assertEquals(resultType, JSON_MIME)
        }

    @Test
    fun `getResourceInAcceptedFormat - when accept is all media  - should return data in json format`(): Unit =
        runBlocking {
            val (resultData, resultType) = contentNegotiationService.getResourceInAcceptedFormat(json, ALL_MIME)

            assertEquals(json.toString(), resultData)
            assertEquals(JSON_MIME, resultType)
        }

    @Test
    fun `getResourceInAcceptedFormat - when accept is xml  - should return data in xml format`(): Unit =
        runBlocking {
            val (resultData, resultType) = contentNegotiationService.getResourceInAcceptedFormat(json, XML_MIME)

            assertEquals(xml, resultData)
            assertEquals(resultType, XML_MIME)
        }

    @Test
    fun `getResourceInAcceptedFormat - when accept is csv  - should return data in csv format`(): Unit =
        runBlocking {
            val (resultData, resultType) = contentNegotiationService.getResourceInAcceptedFormat(json, CSV_MIME)

            assertEquals(
                csv.replace(Regex("\\R"), ""),
                resultData.replace(Regex("\\R"), ""),
            )
            assertEquals(resultType, CSV_MIME)
        }

    @Test
    fun `getResourceInAcceptedFormat - when accept is unsupported media type  - should throw BadRequestException`(): Unit =
        runBlocking {
            val exception =
                assertFailsWith<NotAcceptableException> {
                    contentNegotiationService.getResourceInAcceptedFormat(json, "text/html")
                }
            assertEquals("Supported types are: [$JSON_MIME, $XML_MIME, $CSV_MIME]", exception.message)
        }

    @Test
    fun `processAcceptHeader - when CSV has higher priority than all application types - returns CSV`() {
        val mediaType = contentNegotiationService.processAcceptHeader("text/csv, application/*; q=0.9")
        assertEquals(ContentType.Text.CSV, mediaType)
    }

    @Test
    fun `processAcceptHeader - when accepts all application types - returns JSON`() {
        val mediaType = contentNegotiationService.processAcceptHeader("application/*")
        assertEquals(ContentType.Application.Json, mediaType)
    }

    @Test
    fun `processAcceptHeader - when non supported types are given - returns null`() {
        val mediaType = contentNegotiationService.processAcceptHeader("text/html, text/plain")
        assertEquals(null, mediaType)
    }

    @Test
    fun `processAcceptHeader - when accepts explicitly XML and all application types - returns XML`() {
        // application/xml will have higher priority
        val mediaType = contentNegotiationService.processAcceptHeader("application/*, application/xml")
        assertEquals(ContentType.Application.Xml, mediaType)
    }

    @Test
    fun `processAcceptHeader - when all supported types are accepted - returns JSON`() {
        val mediaType = contentNegotiationService.processAcceptHeader("application/json, application/xml, text/csv")
        assertEquals(ContentType.Application.Json, mediaType)
    }

    @Test
    fun `processAcceptHeader - when JSON has highest priority - returns JSON`() {
        val mediaType = contentNegotiationService.processAcceptHeader("application/json; q=0.9, application/xml; q=0.8, text/csv; q=0.6")
        assertEquals(ContentType.Application.Json, mediaType)
    }

    @Test
    fun `processAcceptHeader - when XML has highest priority - returns XML`() {
        val mediaType = contentNegotiationService.processAcceptHeader("application/json; q=0.5, application/xml; q=0.9, text/csv; q=0.7")
        assertEquals(ContentType.Application.Xml, mediaType)
    }

    @Test
    fun `processAcceptHeader - when CSV has highest priority - returns CSV`() {
        val mediaType = contentNegotiationService.processAcceptHeader("application/json; q=0.4, application/xml; q=0.5, text/csv; q=0.9")
        assertEquals(ContentType.Text.CSV, mediaType)
    }

    @Test
    fun `processAcceptHeader - when any application type is preferred - returns JSON`() {
        val mediaType = contentNegotiationService.processAcceptHeader("application/*, text/csv; q=0.9")
        assertEquals(ContentType.Application.Json, mediaType)
    }

    @Test
    fun `processAcceptHeader - when any text type is preferred - returns CSV`() {
        val mediaType = contentNegotiationService.processAcceptHeader("text/*, application/*; q=0.9")
        assertEquals(ContentType.Text.CSV, mediaType)
    }

    @Test
    fun `processAcceptHeader - when wildcard is used - returns JSON`() {
        val mediaType = contentNegotiationService.processAcceptHeader("*/*")
        assertEquals(ContentType.Application.Json, mediaType)
    }

    @Test
    fun `processAcceptHeader - when wildcard is used but JSON is excluded - returns XML`() {
        val mediaType = contentNegotiationService.processAcceptHeader("*/*, application/json; q=0")
        assertEquals(ContentType.Application.Xml, mediaType)
    }

    @Test
    fun `processAcceptHeader - when wildcard is used but JSON and XML is excluded - returns CSV`() {
        val mediaType = contentNegotiationService.processAcceptHeader("*/*, application/json; q=0, application/xml; q=0")
        assertEquals(ContentType.Text.CSV, mediaType)
    }

    @Test
    fun `processAcceptHeader - when unsupported media type is preferred, but a supported type is included - returns JSON`() {
        val mediaType = contentNegotiationService.processAcceptHeader("image/png, application/json; q=0.9")
        assertEquals(ContentType.Application.Json, mediaType)
    }

    @Test
    fun `processAcceptHeader - when no Accept header is present - returns JSON`() {
        val mediaType = contentNegotiationService.processAcceptHeader("")
        assertEquals(ContentType.Application.Json, mediaType)
    }

    @Test
    fun `processAcceptHeader - when all formats are explicitly rejected - returns null`() {
        val mediaType = contentNegotiationService.processAcceptHeader("application/json; q=0, application/xml; q=0, text/csv; q=0")
        assertEquals(null, mediaType)
    }

    @Test
    fun `processAcceptHeader - when media range has same priority and specific media type - returns the specific media type`() {
        val mediaType = contentNegotiationService.processAcceptHeader("application/yaml, application/*; q=0.8, text/csv; q=0.8")
        assertEquals(ContentType.Text.CSV, mediaType)
    }
}
