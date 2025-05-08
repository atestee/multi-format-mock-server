package cz.cvut.fit.atlasest.routing.collectionRoutes

import BaseTest
import cz.cvut.fit.atlasest.services.generateValidationError
import cz.cvut.fit.atlasest.utils.toJsonObject
import io.ktor.client.request.accept
import io.ktor.client.request.post
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

class PostRouteTest : BaseTest() {
    private val itemMap =
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

    @Test
    fun `POST collection item - when given unsupported Accept header - should return 406 and Vary header`() =
        testWithApp {
            val collectionName = "books"
            val response =
                client.post("/$collectionName") {
                    accept(ContentType.Application.Yaml)
                    contentType(ContentType.Application.Json)
                    setBody(JsonObject(itemMap).toString())
                }

            assertEquals(HttpStatusCode.NotAcceptable, response.status)
            assertNotNull(response.headers[HttpHeaders.Vary])
            assertEquals(HttpHeaders.Accept, response.headers[HttpHeaders.Vary])
        }

    @Test
    fun `POST collection item - when given unsupported ContentType header - should return 415 and Vary and Accept header`() =
        testWithApp {
            val collectionName = "books"
            val response =
                client.post("/$collectionName") {
                    contentType(ContentType.Application.Yaml)
                }

            assertEquals(HttpStatusCode.UnsupportedMediaType, response.status)
            assertNotNull(response.headers[HttpHeaders.Vary])
            assertNotNull(response.headers[HttpHeaders.Accept])
            assertEquals(HttpHeaders.Accept, response.headers[HttpHeaders.Vary])
            assertEquals(listOf("application/json", "application/xml", "text/csv").joinToString(", "), response.headers[HttpHeaders.Accept])
        }

    @Test
    fun `POST collection item - when given valid JSON item - should insert item`() =
        testWithApp {
            val id = 11
            val collectionName = "books"

            val response =
                client.post("/$collectionName") {
                    contentType(ContentType.Application.Json)
                    setBody(JsonObject(itemMap).toString())
                }

            assertEquals(HttpStatusCode.Created, response.status)
            assertEquals("/$collectionName/$id", response.headers[HttpHeaders.Location])
            itemMap["id"] = JsonPrimitive(id)
            assertEquals(JsonObject(itemMap), response.bodyAsText().toJsonObject())
        }

    @Test
    fun `POST collection item - when content type is JSON and accept is CSV - should insert item and return CSV`() =
        testWithApp {
            val id = 11
            val collectionName = "books"
            val expectedCsv = getCsv(id)

            val response =
                client.post("/$collectionName") {
                    contentType(ContentType.Application.Json)
                    accept(ContentType.Text.CSV)
                    setBody(JsonObject(itemMap).toString())
                }

            assertEquals(HttpStatusCode.Created, response.status)
            assertEquals("/$collectionName/$id", response.headers[HttpHeaders.Location])
            assertEquals(
                expectedCsv.replace("\\s".toRegex(), ""),
                response.bodyAsText().replace("\\s".toRegex(), ""),
            )
        }

    @Test
    fun `POST collection item - when content type is JSON and accept is XML - should insert item and return XML`() =
        testWithApp {
            val id = 11
            val collectionName = "books"
            val expectedXml = getXml(id)

            val response =
                client.post("/$collectionName") {
                    contentType(ContentType.Application.Json)
                    setBody(JsonObject(itemMap).toString())
                    accept(ContentType.Application.Xml)
                }

            assertEquals(HttpStatusCode.Created, response.status)
            assertEquals("/$collectionName/$id", response.headers[HttpHeaders.Location])
            assertEquals(
                expectedXml.replace("\\s".toRegex(), ""),
                response.bodyAsText().replace("\\s".toRegex(), ""),
            )
        }

    @Test
    fun `POST collection item - when content type is CSV and accept is any - should insert item and return JSON`() =
        testWithApp {
            val id = 11
            val collectionName = "books"
            val csvItem = getCsv()

            val response =
                client.post("/$collectionName") {
                    contentType(ContentType.Text.CSV)
                    setBody(csvItem)
                }

            assertEquals(HttpStatusCode.Created, response.status)
            assertEquals("/$collectionName/$id", response.headers[HttpHeaders.Location])
            itemMap["id"] = JsonPrimitive(id)
            assertEquals(JsonObject(itemMap), response.bodyAsText().toJsonObject())
        }

    @Test
    fun `POST collection item - when content type is CSV and accept is XML - should insert item and return XML`() =
        testWithApp {
            val id = 11
            val collectionName = "books"
            val xmlItem = getXml(id).replace("\\s".toRegex(), "")
            val csvItem = getCsv()

            val response =
                client.post("/$collectionName") {
                    contentType(ContentType.Text.CSV)
                    accept(ContentType.Application.Xml)
                    setBody(csvItem)
                }

            assertEquals(HttpStatusCode.Created, response.status)
            assertEquals("/$collectionName/$id", response.headers[HttpHeaders.Location])
            assertEquals(
                xmlItem,
                response.bodyAsText(),
            )
        }

    @Test
    fun `POST collection item - when content type is XML and accept is CSV - should insert item and return CSV`() =
        testWithApp {
            val id = 11
            val collectionName = "books"

            val xmlItem = getXml()
            val csvItem = getCsv(id).replace("\\s".toRegex(), "")

            val response =
                client.post("/$collectionName") {
                    accept(ContentType.Text.CSV)
                    contentType(ContentType.Application.Xml)
                    setBody(xmlItem)
                }

            assertEquals(HttpStatusCode.Created, response.status)
            assertEquals("/$collectionName/$id", response.headers[HttpHeaders.Location])
            assertEquals(
                csvItem,
                response.bodyAsText().replace("\\s".toRegex(), ""),
            )
        }

    @Test
    fun `POST collection item - when content type is XML and accept is JSON - should insert item and return JSON`() =
        testWithApp {
            val id = 11
            val collectionName = "books"

            val xmlItem = getXml()

            val response =
                client.post("/$collectionName") {
                    contentType(ContentType.Application.Xml)
                    setBody(xmlItem)
                    accept(ContentType.Application.Json)
                }

            assertEquals(HttpStatusCode.Created, response.status)
            assertEquals("/$collectionName/$id", response.headers[HttpHeaders.Location])
            itemMap["id"] = JsonPrimitive(id)
            assertEquals(JsonObject(itemMap), response.bodyAsText().toJsonObject())
        }

    @Test
    fun `POST collection item - when given valid item with id - should ignore id and insert item`() =
        testWithApp {
            val insertedId = 11
            val wrongId = 1
            val collectionName = "books"

            val itemMapWithWrongId =
                itemMap.toMutableMap().apply {
                    this["id"] = JsonPrimitive(wrongId)
                }

            val response =
                client.post("/$collectionName") {
                    contentType(ContentType.Application.Json)
                    setBody(JsonObject(itemMapWithWrongId).toString())
                }

            assertEquals(HttpStatusCode.Created, response.status)
            assertEquals("/$collectionName/$insertedId", response.headers[HttpHeaders.Location])
            itemMapWithWrongId["id"] = JsonPrimitive(insertedId)
            assertEquals(JsonObject(itemMapWithWrongId), response.bodyAsText().toJsonObject())
        }

    @Test
    fun `POST collection item - when given item with missing property - should return 400`() =
        testWithApp {
            val itemMapWithMissingPublishedYear =
                itemMap.toMutableMap().apply {
                    this.remove("published")
                }

            val response =
                client.post("/books") {
                    contentType(ContentType.Application.Json)
                    setBody(JsonObject(itemMapWithMissingPublishedYear).toString())
                }

            assertEquals(HttpStatusCode.BadRequest, response.status)
            val responseBody = response.bodyAsText()
            assertEquals(generateValidationError("published"), responseBody)
        }

    private fun getCsv(id: Int? = null): String =
        if (id is Int) {
            """
            author;title;genre[0];isbn;published.place;published.year;id
            author1;title1;genre1;12345;place1;2024;$id
            """.trimIndent()
        } else {
            """
            author;title;genre[0];isbn;published.place;published.year
            author1;title1;genre1;12345;place1;2024
            """.trimIndent()
        }

    private fun getXml(id: Int? = null): String =
        if (id is Int) {
            """
            <item>
                <author>author1</author>
                <isbn>12345</isbn>
                <genre>genre1</genre>
                <published>
                    <year>2024</year>
                    <place>place1</place>
                </published>
                <id>$id</id>
                <title>title1</title>
            </item>
            """.trimIndent()
        } else {
            """
            <item>
                <author>author1</author>
                <title>title1</title>
                <genre>genre1</genre>
                <isbn>12345</isbn>
                <published>
                    <place>place1</place>
                    <year>2024</year>
                </published>
            </item>
            """.trimIndent()
        }
}
