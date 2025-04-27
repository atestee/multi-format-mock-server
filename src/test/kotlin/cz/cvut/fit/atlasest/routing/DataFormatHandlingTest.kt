package cz.cvut.fit.atlasest.routing

import cz.cvut.fit.atlasest.utils.ALL_MIME
import cz.cvut.fit.atlasest.utils.CSV_MIME
import cz.cvut.fit.atlasest.utils.JSON_MIME
import cz.cvut.fit.atlasest.utils.XML_MIME
import cz.cvut.fit.atlasest.utils.getResourceInJsonFormat
import cz.cvut.fit.atlasest.utils.processAcceptHeader
import cz.cvut.fit.atlasest.utils.returnResourceInAcceptedFormat
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.response.respond
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DataFormatHandlingTest {
    private val json = JsonObject(mapOf("key" to JsonPrimitive("value")))
    private val xml = "<item><key>value</key></item>"
    private val csv = "key\nvalue"

    private fun convertAndValidateJson(json: JsonObject): JsonObject = json

    val call = mockk<ApplicationCall>()

    @Test
    fun `getResourceInJsonFormat - when req body has invalid JSON - should throw BadRequestException`() {
        val exception =
            assertFailsWith<BadRequestException> {
                getResourceInJsonFormat("[1,2,3]", JSON_MIME, ::convertAndValidateJson)
            }
        assertEquals("Problem when parsing JSON body (must be a JSON object)", exception.message)
    }

    @Test
    fun `getResourceInJsonFormat - when content type is not supported - should throw BadRequestException`() {
        val exception =
            assertFailsWith<BadRequestException> {
                getResourceInJsonFormat(json.toString(), "text/html", ::convertAndValidateJson)
            }
        assertEquals("Unsupported content type text/html. Supported types are: [$JSON_MIME, $XML_MIME, $CSV_MIME]", exception.message)
    }

    @Test
    fun `getResourceInJsonFormat - when given valid JSON - should respond with JSON`() {
        val result = getResourceInJsonFormat(json.toString(), JSON_MIME, ::convertAndValidateJson)
        assertEquals(json, result)
    }

    @Test
    fun `getResourceInJsonFormat - when req body is valid XML - should respond with converted JSON`() {
        val result = getResourceInJsonFormat(xml, XML_MIME, ::convertAndValidateJson)
        assertEquals(json, result)
    }

    @Test
    fun `getResourceInJsonFormat - when req body is valid CSV - should respond with converted JSON`() {
        val result = getResourceInJsonFormat(csv, CSV_MIME, ::convertAndValidateJson)
        assertEquals(json, result)
    }

    @Test
    fun `returnResourceInAcceptedFormat - when accept is json  - should return data in json format`(): Unit =
        runBlocking {
            val code = HttpStatusCode.OK

            coEvery { call.response.headers.append("Content-Type", JSON_MIME) } just Runs
            coEvery { call.respond(code, any<JsonElement>()) } just Runs

            returnResourceInAcceptedFormat(call, code, json, JSON_MIME)

            verify { call.response.headers.append("Content-Type", JSON_MIME) }
            val jsonSlot = slot<JsonElement>()
            coVerify { call.respond(code, capture(jsonSlot)) }

            assertEquals(jsonSlot.captured.toString(), json.toString())
        }

    @Test
    fun `returnResourceInAcceptedFormat - when accept is all media  - should return data in json format`(): Unit =
        runBlocking {
            val code = HttpStatusCode.OK

            coEvery { call.response.headers.append("Content-Type", JSON_MIME) } just Runs
            coEvery { call.respond(code, any<JsonElement>()) } just Runs

            returnResourceInAcceptedFormat(call, code, json, ALL_MIME)

            verify { call.response.headers.append("Content-Type", JSON_MIME) }
            val jsonSlot = slot<JsonElement>()
            coVerify { call.respond(code, capture(jsonSlot)) }

            assertEquals(jsonSlot.captured.toString(), json.toString())
        }

    @Test
    fun `returnResourceInAcceptedFormat - when accept is xml  - should return data in xml format`(): Unit =
        runBlocking {
            val code = HttpStatusCode.OK

            coEvery { call.response.headers.append("Content-Type", XML_MIME) } just Runs
            coEvery { call.respond(code, xml) } just Runs

            returnResourceInAcceptedFormat(call, code, json, XML_MIME)

            verify { call.response.headers.append("Content-Type", XML_MIME) }
            coVerify { call.respond(code, xml) }
        }

    @Test
    fun `returnResourceInAcceptedFormat - when accept is csv  - should return data in csv format`(): Unit =
        runBlocking {
            val code = HttpStatusCode.OK

            coEvery { call.response.headers.append("Content-Type", CSV_MIME) } just Runs
            coEvery { call.respond(code, any<String>()) } just Runs

            returnResourceInAcceptedFormat(call, code, json, CSV_MIME)

            verify { call.response.headers.append("Content-Type", CSV_MIME) }
            val csvDataSlot = slot<String>()
            coVerify { call.respond(code, capture(csvDataSlot)) }

            assertEquals(
                csv.replace(Regex("\\R"), ""),
                csvDataSlot.captured.replace(Regex("\\R"), ""),
            )
        }

    @Test
    fun `returnResourceInAcceptedFormat - when accept is unsupported media type  - should throw BadRequestException`(): Unit =
        runBlocking {
            val exception =
                assertFailsWith<BadRequestException> {
                    returnResourceInAcceptedFormat(call, HttpStatusCode.OK, json, "text/html")
                }
            assertEquals("Unsupported accept type text/html. Supported types are: [$JSON_MIME, $XML_MIME, $CSV_MIME]", exception.message)
        }

    @Test
    fun `negotiateContent - when CSV has higher priority than all application types - returns CSV`() {
        val mediaType = processAcceptHeader("text/csv, application/*; q=0.9")
        assertEquals(ContentType.Text.CSV, mediaType)
    }

    @Test
    fun `negotiateContent - when accepts all application types - returns JSON`() {
        val mediaType = processAcceptHeader("application/*")
        assertEquals(ContentType.Application.Json, mediaType)
    }

    @Test
    fun `negotiateContent - when non supported types are given - returns null`() {
        val mediaType = processAcceptHeader("text/html, text/plain")
        assertEquals(null, mediaType)
    }

    @Test
    fun `negotiateContent - when accepts explicitly XML and all application types - returns XML`() {
        val mediaType = processAcceptHeader("application/*, application/xml") // application/xml will have higher priority
        assertEquals(ContentType.Application.Xml, mediaType)
    }

    @Test
    fun `negotiateContent - when all supported types are accepted - returns JSON`() {
        val mediaType = processAcceptHeader("application/json, application/xml, text/csv")
        assertEquals(ContentType.Application.Json, mediaType)
    }

    @Test
    fun `negotiateContent - when JSON has highest priority - returns JSON`() {
        val mediaType = processAcceptHeader("application/json; q=0.9, application/xml; q=0.8, text/csv; q=0.6")
        assertEquals(ContentType.Application.Json, mediaType)
    }

    @Test
    fun `negotiateContent - when XML has highest priority - returns XML`() {
        val mediaType = processAcceptHeader("application/json; q=0.5, application/xml; q=0.9, text/csv; q=0.7")
        assertEquals(ContentType.Application.Xml, mediaType)
    }

    @Test
    fun `negotiateContent - when CSV has highest priority - returns CSV`() {
        val mediaType = processAcceptHeader("application/json; q=0.4, application/xml; q=0.5, text/csv; q=0.9")
        assertEquals(ContentType.Text.CSV, mediaType)
    }

    @Test
    fun `negotiateContent - when any application type is preferred - returns JSON`() {
        val mediaType = processAcceptHeader("application/*, text/csv; q=0.9")
        assertEquals(ContentType.Application.Json, mediaType)
    }

    @Test
    fun `negotiateContent - when any text type is preferred - returns CSV`() {
        val mediaType = processAcceptHeader("text/*, application/*; q=0.9")
        assertEquals(ContentType.Text.CSV, mediaType)
    }

    @Test
    fun `negotiateContent - when wildcard is used - returns JSON`() {
        val mediaType = processAcceptHeader("*/*")
        assertEquals(ContentType.Application.Json, mediaType)
    }

    @Test
    fun `negotiateContent - when wildcard is used but JSON is excluded - returns XML`() {
        val mediaType = processAcceptHeader("*/*, application/json; q=0")
        assertEquals(ContentType.Application.Xml, mediaType)
    }

    @Test
    fun `negotiateContent - when wildcard is used but JSON and XML is excluded - returns CSV`() {
        val mediaType = processAcceptHeader("*/*, application/json; q=0, application/xml; q=0")
        assertEquals(ContentType.Text.CSV, mediaType)
    }

    @Test
    fun `negotiateContent - when unsupported media type is preferred, but a supported type is included - returns JSON`() {
        val mediaType = processAcceptHeader("image/png, application/json; q=0.9")
        assertEquals(ContentType.Application.Json, mediaType)
    }

    @Test
    fun `negotiateContent - when no Accept header is present - returns JSON`() {
        val mediaType = processAcceptHeader("")
        assertEquals(ContentType.Application.Json, mediaType)
    }

    @Test
    fun `negotiateContent - when all formats are explicitly rejected - returns null`() {
        val mediaType = processAcceptHeader("application/json; q=0, application/xml; q=0, text/csv; q=0")
        assertEquals(null, mediaType)
    }
}
