package cz.cvut.fit.atlasest.routing.collectionRoutes

import BaseTest
import cz.cvut.fit.atlasest.services.generateValidationError
import cz.cvut.fit.atlasest.utils.toJsonObject
import io.ktor.client.request.accept
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class PutRouteTest : BaseTest() {
    private val insertedItemMap =
        mutableMapOf(
            "author" to JsonPrimitive("author1"),
            "title" to JsonPrimitive("title1"),
            "genre" to
                JsonArray(
                    listOf(JsonPrimitive("genre1")),
                ),
            "isbn" to JsonPrimitive("12345"),
            "published" to
                JsonObject(
                    mapOf(
                        "place" to JsonPrimitive("place1"),
                        "year" to JsonPrimitive(2024),
                    ),
                ),
        )

    private val updatedId = 2
    private val insertedId = 11
    private val invalidId = "110"
    private val collectionName = "books"
    private val originalItem = collectionService.getItemById(collectionName, updatedId.toString())
    private val updatedItemMap =
        originalItem.toMutableMap().apply {
            this["author"] = JsonPrimitive("author2")
        }

    @Test
    fun `PUT collection item - when given unsupported Accept header - should return 406 and Vary header`() =
        testWithApp {
            val collectionName = "books"
            val response =
                client.put("/$collectionName/$updatedId") {
                    accept(ContentType.Application.Yaml)
                    contentType(ContentType.Application.Json)
                    setBody(JsonObject(updatedItemMap).toString())
                }

            assertEquals(HttpStatusCode.NotAcceptable, response.status)
            assertNotNull(response.headers[HttpHeaders.Vary])
            assertEquals(HttpHeaders.Accept, response.headers[HttpHeaders.Vary])
        }

    @Test
    fun `PUT collection item - when given unsupported ContentType header - should return 415 and Vary and Accept header`() =
        testWithApp {
            val collectionName = "books"
            val response =
                client.put("/$collectionName/$updatedId") {
                    contentType(ContentType.Application.Yaml)
                }

            assertEquals(HttpStatusCode.UnsupportedMediaType, response.status)
            assertNotNull(response.headers[HttpHeaders.Vary])
            assertNotNull(response.headers[HttpHeaders.Accept])
            assertEquals(HttpHeaders.Accept, response.headers[HttpHeaders.Vary])
            assertEquals(listOf("application/json", "application/xml", "text/csv").joinToString(", "), response.headers[HttpHeaders.Accept])
        }

    @Test
    fun `PUT collection item - when given valid id and valid item - should update item and return 200`() =
        testWithApp {
            val response =
                client.put("/$collectionName/$updatedId") {
                    contentType(ContentType.Application.Json)
                    setBody(JsonObject(updatedItemMap).toString())
                }
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(JsonObject(updatedItemMap), response.bodyAsText().toJsonObject())
        }

    @Test
    fun `PUT collection item - when given valid id and invalid item - should return 400`() =
        testWithApp {
            val updatedItem = originalItem.toMutableMap().also { it.remove("author") }

            val response =
                client.put("/$collectionName/$updatedId") {
                    contentType(ContentType.Application.Json)
                    setBody(JsonObject(updatedItem).toString())
                }

            assertEquals(HttpStatusCode.BadRequest, response.status)
            val responseBody = response.bodyAsText()
            assertEquals(generateValidationError("author"), responseBody)
        }

    @Test
    fun `PUT collection item - when given invalid id and valid item - should insert item and return 201`() =
        testWithApp {
            val expectedItem = insertedItemMap.toMutableMap().also { it["id"] = JsonPrimitive(insertedId) }

            val response =
                client.put("/$collectionName/$invalidId") {
                    contentType(ContentType.Application.Json)
                    setBody(JsonObject(insertedItemMap).toString())
                }

            assertEquals(HttpStatusCode.Created, response.status)
            assertEquals(JsonObject(expectedItem), response.bodyAsText().toJsonObject())
        }

    @Test
    fun `PUT item - when given invalid item with missing properties - should return 400 and validation errors`() =
        testWithApp {
            val response =
                client.put("/books/$invalidId") {
                    contentType(ContentType.Application.Json)
                    setBody("{}")
                }

            assertEquals(HttpStatusCode.BadRequest, response.status)
            val error = response.bodyAsText()
            assertNotNull(error)
            assertEquals(
                generateValidationError(
                    listOf(
                        "genre",
                        "published",
                        "title",
                        "author",
                        "isbn",
                    ),
                ),
                error,
            )
        }

    @Test
    fun `PUT collection item - when Content-type=JSON, Accept=CSV and invalid id - should insert item and return CSV`() =
        testWithApp {
            val expectedCsv = getInsertedCsv(withId = true)

            val response =
                client.put("/$collectionName/$invalidId") {
                    contentType(ContentType.Application.Json)
                    setBody(JsonObject(insertedItemMap).toString())
                    accept(ContentType.Text.CSV)
                }

            assertEquals(HttpStatusCode.Created, response.status)
            assertEquals("/$collectionName/$insertedId", response.headers[HttpHeaders.Location])
            assertEquals(
                expectedCsv.replace("\\s".toRegex(), ""),
                response.bodyAsText().replace("\\s".toRegex(), ""),
            )
        }

    @Test
    fun `PUT collection item - when Content-type=JSON, Accept=CSV and valid id - should update item and return CSV`() =
        testWithApp {
            val id = 2
            val expectedCsv = getUpdatedCsv()

            val response =
                client.put("/$collectionName/$id") {
                    contentType(ContentType.Application.Json)
                    setBody(JsonObject(updatedItemMap).toString())
                    accept(ContentType.Text.CSV)
                }

            assertEquals(HttpStatusCode.OK, response.status)
            assertNull(response.headers[HttpHeaders.Location])
            assertEquals(
                expectedCsv.replace("\\s".toRegex(), ""),
                response.bodyAsText().replace("\\s".toRegex(), ""),
            )
        }

    @Test
    fun `PUT collection item - when Content-type=JSON, Accept=XML and invalid id - should insert item and return CSV`() =
        testWithApp {
            val invalidId = 110
            val expectedXml = getInsertedXml(withId = true)

            val response =
                client.put("/$collectionName/$invalidId") {
                    contentType(ContentType.Application.Json)
                    setBody(JsonObject(insertedItemMap).toString())
                    accept(ContentType.Application.Xml)
                }

            assertEquals(HttpStatusCode.Created, response.status)
            assertEquals("/$collectionName/$insertedId", response.headers[HttpHeaders.Location])
            assertEquals(
                expectedXml.replace("\\s".toRegex(), ""),
                response.bodyAsText().replace("\\s".toRegex(), ""),
            )
        }

    @Test
    fun `PUT collection item - when Content-type=JSON, Accept=XML and valid id - should update item and return CSV`() =
        testWithApp {
            val expectedXml = getUpdatedXml()

            val response =
                client.put("/$collectionName/$updatedId") {
                    contentType(ContentType.Application.Json)
                    setBody(JsonObject(updatedItemMap).toString())
                    accept(ContentType.Application.Xml)
                }

            assertEquals(HttpStatusCode.OK, response.status)
            assertNull(response.headers[HttpHeaders.Location])
            assertEquals(
                expectedXml.replace("\\s".toRegex(), ""),
                response.bodyAsText().replace("\\s".toRegex(), ""),
            )
        }

    @Test
    fun `PUT collection item - when Content-type=XML, Accept=JSON and invalid id - should insert item and return JSON`() =
        testWithApp {
            val invalidId = 110
            val insertedXml = getInsertedXml(withId = false)
            val expectedItem = insertedItemMap.toMutableMap().also { it["id"] = JsonPrimitive(insertedId) }

            val response =
                client.put("/$collectionName/$invalidId") {
                    contentType(ContentType.Application.Xml)
                    setBody(insertedXml)
                    accept(ContentType.Application.Json)
                }

            assertEquals(HttpStatusCode.Created, response.status)
            assertEquals("/$collectionName/$insertedId", response.headers[HttpHeaders.Location])
            assertEquals(JsonObject(expectedItem), response.bodyAsText().toJsonObject())
        }

    @Test
    fun `PUT collection item - when Content-type=XML, Accept=JSON and valid id - should update item and return JSON`() =
        testWithApp {
            val updatedXml = getUpdatedXml()

            val response =
                client.put("/$collectionName/$updatedId") {
                    contentType(ContentType.Application.Xml)
                    setBody(updatedXml)
                    accept(ContentType.Application.Json)
                }

            assertEquals(HttpStatusCode.OK, response.status)
            assertNull(response.headers[HttpHeaders.Location])
            assertEquals(JsonObject(updatedItemMap), response.bodyAsText().toJsonObject())
        }

    @Test
    fun `PUT collection item - when Content-type=CSV, Accept=JSON and invalid id - should insert item and return JSON`() =
        testWithApp {
            val invalidId = 110
            val insertedCsv = getInsertedCsv(withId = false)
            val expectedItem = insertedItemMap.toMutableMap().also { it["id"] = JsonPrimitive(insertedId) }

            val response =
                client.put("/$collectionName/$invalidId") {
                    contentType(ContentType.Text.CSV)
                    setBody(insertedCsv)
                    accept(ContentType.Application.Json)
                }

            assertEquals(HttpStatusCode.Created, response.status)
            assertEquals("/$collectionName/$insertedId", response.headers[HttpHeaders.Location])
            assertEquals(JsonObject(expectedItem), response.bodyAsText().toJsonObject())
        }

    @Test
    fun `PUT collection item - when Content-type=CSV, Accept=JSON and valid id - should update item and return JSON`() =
        testWithApp {
            val insertedCsv = getUpdatedCsv()

            val response =
                client.put("/$collectionName/$updatedId") {
                    contentType(ContentType.Text.CSV)
                    setBody(insertedCsv)
                    accept(ContentType.Application.Json)
                }

            assertEquals(HttpStatusCode.OK, response.status)
            assertNull(response.headers[HttpHeaders.Location])
            assertEquals(JsonObject(updatedItemMap), response.bodyAsText().toJsonObject())
        }

    @Test
    fun `PUT collection item - when Content-type=XML, Accept=CSV and invalid id - should insert item and return CSV`() =
        testWithApp {
            val invalidId = 110
            val insertedXml = getInsertedXml(withId = false)
            val expectedCsv = getInsertedCsv(withId = true)

            val response =
                client.put("/$collectionName/$invalidId") {
                    contentType(ContentType.Application.Xml)
                    setBody(insertedXml)
                    accept(ContentType.Text.CSV)
                }

            assertEquals(HttpStatusCode.Created, response.status)
            assertEquals("/$collectionName/$insertedId", response.headers[HttpHeaders.Location])

            assertCsvEquals(expectedCsv, response.bodyAsText())
        }

    @Test
    fun `PUT collection item - when Content-type=XML, Accept=CSV and valid id - should update item and return CSV`() =
        testWithApp {
            val updatedXml = getUpdatedXml()
            val expectedCsv = getUpdatedCsv()

            val response =
                client.put("/$collectionName/$updatedId") {
                    contentType(ContentType.Application.Xml)
                    setBody(updatedXml)
                    accept(ContentType.Text.CSV)
                }

            assertEquals(HttpStatusCode.OK, response.status)
            assertNull(response.headers[HttpHeaders.Location])
            assertCsvEquals(expectedCsv, response.bodyAsText())
        }

    @Test
    fun `PUT collection item - when Content-type=CSV, Accept=XML and invalid id - should insert item and return XML`() =
        testWithApp {
            val invalidId = 110
            val insertedCsv = getInsertedCsv(withId = false)
            val expectedXml = getInsertedXml(withId = true)

            val response =
                client.put("/$collectionName/$invalidId") {
                    contentType(ContentType.Text.CSV)
                    setBody(insertedCsv)
                    accept(ContentType.Application.Xml)
                }

            assertEquals(HttpStatusCode.Created, response.status)
            assertEquals("/$collectionName/$insertedId", response.headers[HttpHeaders.Location])

            assertEquals(
                expectedXml.replace("\\s+".toRegex(), ""),
                response.bodyAsText().replace("\\s+".toRegex(), ""),
            )
        }

    @Test
    fun `PUT collection item - when Content-type=CSV, Accept=XML and valid id - should update item and return XML`() =
        testWithApp {
            val updatedCsv = getUpdatedCsv()
            val expectedXml = getUpdatedXml()

            val response =
                client.put("/$collectionName/$updatedId") {
                    contentType(ContentType.Text.CSV)
                    setBody(updatedCsv)
                    accept(ContentType.Application.Xml)
                }

            assertEquals(HttpStatusCode.OK, response.status)
            assertNull(response.headers[HttpHeaders.Location])
            assertEquals(
                expectedXml.replace("\\s+".toRegex(), ""),
                response.bodyAsText().replace("\\s+".toRegex(), ""),
            )
        }

    private fun getInsertedCsv(withId: Boolean): String =
        if (withId) {
            """
            author;title;genre[0];isbn;published.place;published.year;id
            author1;title1;genre1;12345;place1;2024;$insertedId
            
            """.trimIndent()
        } else {
            """
            author;title;genre[0];isbn;published.place;published.year
            author1;title1;genre1;12345;place1;2024
            
            """.trimIndent()
        }

    private fun getUpdatedCsv(): String =
        """
        id;title;author;genre[0];genre[1];genre[2];isbn;published.place;published.year
        $updatedId;Frankenstein;author2;Gothic Novel;Horror Fiction;Science Fiction;9780143131847;UK;1818
        
        """.trimIndent()

    private fun getInsertedXml(withId: Boolean): String =
        if (withId) {
            """
            <item>
                <author>author1</author>
                <isbn>12345</isbn>
                <genre>genre1</genre>
                <published>
                    <year>2024</year>
                    <place>place1</place>
                </published>
                <id>$insertedId</id>
                <title>title1</title>
            </item>
            """.trimIndent()
        } else {
            """
            <item>
                <author>author1</author>
                <isbn>12345</isbn>
                <genre>genre1</genre>
                <published>
                    <year>2024</year>
                    <place>place1</place>
                </published>
                <title>title1</title>
            </item>
            """.trimIndent()
        }

    private fun getUpdatedXml(): String =
        """
        <item>
            <author>author2</author>
            <isbn>9780143131847</isbn>
            <genre>Gothic Novel</genre>
            <genre>Horror Fiction</genre>
            <genre>Science Fiction</genre>
            <id>$updatedId</id>
            <published>
                <year>1818</year>
                <place>UK</place>
            </published>
            <title>Frankenstein</title>
        </item>
        """.trimIndent()

    private fun assertCsvEquals(
        expected: String,
        actual: String,
    ) {
        val expectedLines = expected.lines()
        val expectedHeader = expectedLines.first().split(";").toSet()
        val expectedRows = expectedLines.subList(1, expectedLines.size - 1).map { it.split(";").toSet() }.toSet()

        val actualLines = actual.lines()
        val actualHeader = actualLines.first().split(";").toSet()
        val actualRows = actualLines.subList(1, actualLines.size - 1).map { it.split(";").toSet() }.toSet()

        assertEquals(expectedHeader, actualHeader)
        assertEquals(expectedRows, actualRows)
    }
}
