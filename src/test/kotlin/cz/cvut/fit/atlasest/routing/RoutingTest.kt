package cz.cvut.fit.atlasest.routing

import BaseTest
import cz.cvut.fit.atlasest.service.CollectionService
import cz.cvut.fit.atlasest.service.JsonService
import cz.cvut.fit.atlasest.service.generateValidationError
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlin.collections.set
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.AfterEach
import org.koin.test.inject

class RoutingTest : BaseTest() {
    private val collectionService by inject<CollectionService>()
    private val jsonService by inject<JsonService>()
    private var items = jsonService.readJsonFile(appConfig.fileName).jsonObject

    @AfterEach
    fun afterEach() {
        jsonService.saveJsonFile(appConfig.fileName, items)
    }

    @Test
    fun `GET collections - returns list of collection names`() =
        testWithApp {
            val response = client.get("/collections")
            val responseBody = response.bodyAsText()
            assertNotNull(responseBody)
            assertEquals("collections: [books, loans, users]", responseBody)
            assertEquals(HttpStatusCode.OK, response.status)
        }

    @Test
    fun `GET collection - when given existing collection name - should return valid collection`() =
        testWithApp {
            val response = client.get("/books")
            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.bodyAsText()
            val responseBodyArray = Json.parseToJsonElement(responseBody).jsonArray
            assertNotNull(responseBody)
            assertEquals(10, responseBodyArray.jsonArray.size)
            val responseTitles = responseBodyArray.map { it.jsonObject["title"]?.jsonPrimitive?.content }
            val expectedTitles =
                listOf(
                    "The Catcher in the Rye",
                    "To Kill a Mockingbird",
                    "1984",
                    "Moby Dick",
                    "Pride and Prejudice",
                    "The Great Gatsby",
                    "War and Peace",
                    "The Odyssey",
                    "Brave New World",
                    "Catch-22",
                )
            assertEquals(expectedTitles, responseTitles)
        }

    @Test
    fun `GET collection - when given non-existing collection name - should return 404`() =
        testWithApp {
            val response = client.get("/invalidName")
            assertEquals(HttpStatusCode.NotFound, response.status)
        }

    @Test
    fun `GET collection item - when given valid id - should return corresponding item`() =
        testWithApp {
            val id = "1"
            val response = client.get("/books/$id")
            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.bodyAsText()
            assertNotNull(responseBody)
            val responseBodyObject = Json.parseToJsonElement(responseBody).jsonObject
            assertEquals(responseBodyObject["id"]?.jsonPrimitive?.content, id)
            assertEquals(responseBodyObject["title"]?.jsonPrimitive?.content, "The Catcher in the Rye")
        }

    @Test
    fun `GET collection item - when given invalid id - should return 404`() =
        testWithApp {
            val invalidId = "100"
            val response = client.get("/books/$invalidId")
            assertEquals(HttpStatusCode.NotFound, response.status)
            val responseBody = response.bodyAsText()
            assertNotNull(responseBody)
            val responseBodyObject = Json.parseToJsonElement(responseBody).jsonObject
            assertEquals(
                "Item with id '$invalidId' not found in collection 'books'",
                responseBodyObject["error"]?.jsonPrimitive?.content,
            )
        }

    @Test
    fun `POST collection item - when given valid item - should insert item`() =
        testWithApp {
            val id = 11
            val collectionName = "books"

            val itemMap =
                mutableMapOf(
                    "author" to JsonPrimitive("author1"),
                    "title" to JsonPrimitive("title1"),
                    "genre" to JsonPrimitive("genre1"),
                    "isbn" to JsonPrimitive("12345"),
                    "publishedYear" to JsonPrimitive(2024),
                    "id" to JsonPrimitive(1),
                )

            val response =
                client.post("/$collectionName") {
                    contentType(ContentType.Application.Json)
                    setBody(JsonObject(itemMap).toString())
                }

            assertEquals(HttpStatusCode.Created, response.status)
            assertEquals("/$collectionName/$id", response.headers[HttpHeaders.Location])
            itemMap["id"] = JsonPrimitive(id)
            assertEquals(JsonObject(itemMap), Json.parseToJsonElement(response.bodyAsText()).jsonObject["data"])
        }

    @Test
    fun `POST collection item - when given valid item with id - should ignore id and insert item`() =
        testWithApp {
            val insertedId = 11
            val wrongId = 1
            val collectionName = "books"

            val itemMap =
                mutableMapOf(
                    "author" to JsonPrimitive("author1"),
                    "title" to JsonPrimitive("title1"),
                    "genre" to JsonPrimitive("genre1"),
                    "isbn" to JsonPrimitive("12345"),
                    "publishedYear" to JsonPrimitive(2024),
                    "id" to JsonPrimitive(wrongId),
                )

            val response =
                client.post("/$collectionName") {
                    contentType(ContentType.Application.Json)
                    setBody(JsonObject(itemMap).toString())
                }

            assertEquals(HttpStatusCode.Created, response.status)
            assertEquals("/$collectionName/$insertedId", response.headers[HttpHeaders.Location])
            itemMap["id"] = JsonPrimitive(insertedId)
            assertEquals(JsonObject(itemMap), Json.parseToJsonElement(response.bodyAsText()).jsonObject["data"])
        }

    @Test
    fun `POST collection item - when given item with missing property - should return 400`() =
        testWithApp {
            // missing publishedYear property
            val invalidItem =
                JsonObject(
                    mutableMapOf(
                        "author" to JsonPrimitive("author1"),
                        "title" to JsonPrimitive("title1"),
                        "genre" to JsonPrimitive("genre1"),
                        "isbn" to JsonPrimitive("12345"),
                    ),
                )

            val response =
                client.post("/books") {
                    contentType(ContentType.Application.Json)
                    setBody(JsonObject(invalidItem).toString())
                }

            assertEquals(HttpStatusCode.BadRequest, response.status)
            val responseObject = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            assertEquals(generateValidationError("publishedYear"), responseObject["error"]?.jsonPrimitive?.content)
        }

    @Test
    fun `PUT collection item - when given valid id and valid item - should update item`() =
        testWithApp {
            val id = 10
            val collectionName = "books"
            val originalItem = collectionService.getItemById(collectionName, id.toString())
            val updatedItemMap =
                originalItem.toMutableMap().apply {
                    this["author"] = JsonPrimitive("author2")
                }

            val response =
                client.put("/$collectionName/$id") {
                    contentType(ContentType.Application.Json)
                    setBody(JsonObject(updatedItemMap).toString())
                }
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(JsonObject(updatedItemMap), Json.parseToJsonElement(response.bodyAsText()).jsonObject)
        }

    @Test
    fun `PUT collection item - when given valid id and invalid item - should update item`() =
        testWithApp {
            val id = 10
            val collectionName = "books"
            val originalItem = collectionService.getItemById(collectionName, id.toString())
            val updatedItem = originalItem.toMutableMap()
            updatedItem.remove("author")
            val response =
                client.put("/$collectionName/$id") {
                    contentType(ContentType.Application.Json)
                    setBody(JsonObject(updatedItem).toString())
                }
            assertEquals(HttpStatusCode.BadRequest, response.status)
            val responseObject = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            assertEquals(generateValidationError("author"), responseObject["error"]?.jsonPrimitive?.content)
        }

    @Test
    fun `PUT collection item - when given invalid id and valid item - should return 201`() =
        testWithApp {
            val invalidId = "110"
            val realId = 11
            val collectionName = "books"

            val itemMap =
                mutableMapOf(
                    "author" to JsonPrimitive("author1"),
                    "title" to JsonPrimitive("title1"),
                    "genre" to JsonPrimitive("genre1"),
                    "isbn" to JsonPrimitive("12345"),
                    "publishedYear" to JsonPrimitive(2024),
                )
            val response =
                client.put("/$collectionName/$invalidId") {
                    contentType(ContentType.Application.Json)
                    setBody(JsonObject(itemMap).toString())
                }
            assertEquals(HttpStatusCode.Created, response.status)
            itemMap["id"] = JsonPrimitive(realId)
            assertEquals(JsonObject(itemMap), Json.parseToJsonElement(response.bodyAsText()).jsonObject["data"])
        }

    @Test
    fun `PUT item - when given invalid item with missing properties - should return 400 and validation errors`() =
        testWithApp {
            val invalidId = "110"
            val response =
                client.put("/books/$invalidId") {
                    contentType(ContentType.Application.Json)
                    setBody("{}")
                }
            assertEquals(HttpStatusCode.BadRequest, response.status)
            val responseBody = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            val error = responseBody["error"]?.jsonPrimitive?.content
            assertNotNull(error)
            assertEquals(
                generateValidationError(
                    listOf(
                        "genre",
                        "publishedYear",
                        "title",
                        "author",
                        "isbn",
                    ),
                ),
                error,
            )
        }

    @Test
    fun `DELETE collection item - when given valid id - should return 200 and item should be deleted`() =
        testWithApp {
            val response = client.delete("/books/1")
            assertEquals(HttpStatusCode.OK, response.status)
        }

    @Test
    fun `DELETE collection item - when given invalid id - should return 200 and item should be deleted`() =
        testWithApp {
            val response = client.delete("/books/123")
            assertEquals(HttpStatusCode.OK, response.status)
        }
}
