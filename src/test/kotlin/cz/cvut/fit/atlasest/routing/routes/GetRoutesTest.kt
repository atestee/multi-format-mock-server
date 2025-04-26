package cz.cvut.fit.atlasest.routing.routes

import BaseTest
import com.fasterxml.jackson.databind.JsonNode
import cz.cvut.fit.atlasest.services.LIMIT
import cz.cvut.fit.atlasest.services.ORDER
import cz.cvut.fit.atlasest.services.PAGE
import cz.cvut.fit.atlasest.services.SORT
import cz.cvut.fit.atlasest.utils.getFieldValue
import cz.cvut.fit.atlasest.utils.toJsonArray
import cz.cvut.fit.atlasest.utils.toJsonObject
import cz.cvut.fit.atlasest.utils.xmlMapper
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class GetRoutesTest : BaseTest() {
    val bookList =
        listOf(
            "The Catcher in the Rye",
            "To Kill a Mockingbird",
            "1984",
            "Moby Dick",
            "Pride and Prejudice",
            "The Great Gatsby",
            "Sense and Sensibility",
            "Les Miserables",
            "Brave New World",
            "Frankenstein",
        )

    @Test
    fun `GET collections - returns list of collection names`() =
        testWithApp {
            val response = client.get("/collections")
            val responseBody = response.bodyAsText()
            assertNotNull(responseBody)
            assertEquals("collections: [books, loans, users, libraries, libraryBooks, libraryRegistrations]", responseBody)
            assertEquals(HttpStatusCode.OK, response.status)
        }

    @Test
    fun `GET collection - when given existing collection name - should return valid JSON collection`() =
        testWithApp {
            val response = client.get("/books")
            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.bodyAsText()
            val responseBodyArray = responseBody.toJsonArray()
            assertNotNull(responseBody)
            assertEquals(10, responseBodyArray.jsonArray.size)
            val responseTitles = responseBodyArray.map { it.jsonObject["title"]?.jsonPrimitive?.content }
            val expectedTitles = bookList
            assertEquals(expectedTitles, responseTitles)
        }

    @Test
    fun `GET collection - when given existing collection name and Accept=XML - should return valid XML collection`() =
        testWithApp {
            val response =
                client.get("/books") {
                    accept(ContentType.Application.Xml)
                }
            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.bodyAsText()
            val actualTitles = getTitleListFromXmlCollection(responseBody)
            val expectedTitles = bookList
            assertEquals(expectedTitles, actualTitles)
        }

    @Test
    fun `GET collection - when given existing collection name and Accept=CSV - should return valid CSV collection`() =
        testWithApp {
            val response =
                client.get("/books") {
                    accept(ContentType.Text.CSV)
                }
            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.bodyAsText()
            val actualTitles = getTitleListFromCsvCollection(responseBody)
            val expectedTitles = bookList
            assertEquals(expectedTitles, actualTitles)
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
            val responseBodyObject = responseBody.toJsonObject()
            assertEquals(responseBodyObject["id"]?.jsonPrimitive?.content, id)
            assertEquals(responseBodyObject["title"]?.jsonPrimitive?.content, "The Catcher in the Rye")
        }

    @Test
    fun `GET collection item - when given valid id and Accept=XML - should return corresponding XML item`() =
        testWithApp {
            val id = "1"
            val response = client.get("/books/$id") { accept(ContentType.Application.Xml) }
            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.bodyAsText()
            assertEquals(getValueFromXmlItem(responseBody, "id"), id)
            assertEquals(getValueFromXmlItem(responseBody, "title"), "The Catcher in the Rye")
        }

    @Test
    fun `GET collection item - when given valid id and Accept=CSV - should return corresponding CSV item`() =
        testWithApp {
            val id = "1"
            val response = client.get("/books/$id") { accept(ContentType.Text.CSV) }
            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.bodyAsText()
            assertEquals(getValueFromCsvItem(responseBody, "id"), id)
            assertEquals(getValueFromCsvItem(responseBody, "title"), "The Catcher in the Rye")
        }

    @Test
    fun `GET collection item - when given invalid id - should return 404`() =
        testWithApp {
            val invalidId = "100"
            val response = client.get("/books/$invalidId")
            assertEquals(HttpStatusCode.NotFound, response.status)
            val responseBody = response.bodyAsText()
            assertEquals("Item with id '$invalidId' not found in collection 'books'", responseBody)
        }

    @Test
    fun `GET collection item - when given valid id for item with custom identifier - should return corresponding item`() =
        testWithApp {
            val id = "1"
            val response = client.get("/users/$id")
            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.bodyAsText()
            assertNotNull(responseBody)
            val responseBodyObject = responseBody.toJsonObject()
            assertEquals(responseBodyObject["identifier"]?.jsonPrimitive?.content, id)
            assertEquals(responseBodyObject["name"]?.jsonPrimitive?.content, "Alice")
            assertEquals(responseBodyObject["email"]?.jsonPrimitive?.content, "alice@example.com")
        }

    @Test
    fun `GET collection with filter - when given filter - should return corresponding collection items`() =
        testWithApp {
            val response =
                client.get("/books") {
                    parameter("genre", "Fiction")
                    parameter("genre", "Dystopian")
                }
            val responseBody = response.bodyAsText().toJsonArray()
            val titles = responseBody.map { it.jsonObject["title"]?.jsonPrimitive?.content }
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(3, responseBody.size)
            assertContains(titles, "Brave New World")
            assertContains(titles, "1984")
            assertContains(titles, "The Great Gatsby")
        }

    @Test
    fun `GET collection with pagination - when given page=2 and limit=4 - should return corresponding collection items`() =
        testWithApp {
            val response =
                client.get("/books") {
                    parameter("_page", "2")
                    parameter("_limit", "4")
                }
            val responseBody = response.bodyAsText().toJsonArray()
            val ids = responseBody.map { it.jsonObject["id"]?.jsonPrimitive?.content }
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(4, responseBody.size)
            assertEquals("5", ids.first())
            assertEquals("8", ids.last())
        }

    @Test
    fun `GET collection with pagination - when given page=null and limit=4 - should throw 400`() =
        testWithApp {
            val response =
                client.get("/books") {
                    parameter("_limit", "4")
                }
            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertEquals("Pagination parameter $LIMIT is without $PAGE", response.bodyAsText())
        }

    @Test
    fun `GET collection with sorting - when given sort=published year and order=desc - should return items sorted desc`() =
        testWithApp {
            val publishedYearKey = "published.year"
            val response =
                client.get("/books") {
                    parameter("_sort", publishedYearKey)
                    parameter("_order", "desc")
                }
            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.bodyAsText().toJsonArray()
            val years =
                responseBody.map {
                    it.jsonObject
                        .getFieldValue(publishedYearKey)
                        ?.jsonPrimitive
                        ?.content
                }
            assertEquals("1960", years.first())
            assertEquals("1811", years.last())
        }

    @Test
    fun `GET collection with sorting - when given sort=published year and order=null - should return items sorted asc`() =
        testWithApp {
            val publishedYearKey = "published.year"
            val response =
                client.get("/books") {
                    parameter("_sort", publishedYearKey)
                }
            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.bodyAsText().toJsonArray()
            val years =
                responseBody.map {
                    it.jsonObject
                        .getFieldValue(publishedYearKey)
                        ?.jsonPrimitive
                        ?.content
                }
            assertEquals("1811", years.first())
            assertEquals("1960", years.last())
        }

    @Test
    fun `GET collection with sorting - when given sort=author and order=desc - should return items sorted desc`() =
        testWithApp {
            val authorKey = "author"
            val response =
                client.get("/books") {
                    parameter("_sort", authorKey)
                    parameter("_order", "desc")
                }
            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.bodyAsText().toJsonArray()
            val authors =
                responseBody.map {
                    it.jsonObject[authorKey]
                        ?.jsonPrimitive
                        ?.content
                }
            assertEquals("Victor Hugo", authors.first())
            assertEquals("Aldous Huxley", authors.last())
        }

    @Test
    fun `GET collection with sorting - when given sort=null and order=null - should return items unsorted`() =
        testWithApp {
            val response =
                client.get("/books")
            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.bodyAsText().toJsonArray()
            val ids =
                responseBody.map {
                    it.jsonObject["id"]
                        ?.jsonPrimitive
                        ?.content
                }
            assertEquals("1", ids.first())
            assertEquals("10", ids.last())
        }

    @Test
    fun `GET collection with sorting - when given sort=null and order=asc - should throw 400`() =
        testWithApp {
            val response =
                client.get("/books") {
                    parameter("_order", "asc")
                }
            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertEquals("Sorting parameter $ORDER is without $SORT", response.bodyAsText())
        }

    @Test
    fun `GET collection with sorting - when given sort=author order is invalid - should throw 400`() =
        testWithApp {
            val response =
                client.get("/books") {
                    parameter("_sort", "author")
                    parameter("_order", "wrong")
                }
            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertEquals("Invalid sorting parameter $ORDER (must be 'asc' or 'desc')", response.bodyAsText())
        }

    @Test
    fun `GET collection with sorting - when given sorting key is invalid - should throw 400`() =
        testWithApp {
            val invalidKey = "wrongKey"
            val response =
                client.get("/books") {
                    parameter("_sort", invalidKey)
                }
            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertEquals("Value of '$invalidKey' is not present", response.bodyAsText())
        }

    @Test
    fun `GET collection with embedding - when given 2 embed values - should return collection with embedded items from both collections`() =
        testWithApp {
            val response =
                client.get("/books") {
                    parameter("_embed", "loans")
                    parameter("_embed", "libraryBooks")
                }
            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.bodyAsText().toJsonArray()
            val loans = responseBody.first().jsonObject["loans"]?.jsonArray
            val libraryBooks = responseBody.first().jsonObject["libraryBooks"]?.jsonArray

            assertNotNull(loans)
            assertNotNull(libraryBooks)
            assertEquals(1, loans.size)
            assertEquals(2, libraryBooks.size)
        }

    @Test
    fun `GET collection with embedding - when given 2 expand values - should return collection with embedded items from both collections`() =
        testWithApp {
            val response =
                client.get("/loans") {
                    parameter("_expand", "book")
                    parameter("_expand", "user")
                }
            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.bodyAsText().toJsonArray()
            val users = responseBody.first().jsonObject["user"]
            val books = responseBody.first().jsonObject["book"]

            assertNotNull(users)
            assertNotNull(books)
        }

    @Test
    fun `GET collection with embedding - when given embed and expand values - should return collection with related items`() =
        testWithApp {
            val response =
                client.get("/users") {
                    parameter("_embed", "loans")
                    parameter("_expand", "libraryRegistration")
                }
            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.bodyAsText().toJsonArray()
            val loans = responseBody.first().jsonObject["loans"]
            val libraryRegistration = responseBody.first().jsonObject["libraryRegistration"]

            assertNotNull(loans)
            assertNotNull(libraryRegistration)
        }

    @Test
    fun `GET collection item with embedding - when given 2 embed values - should return collection item with embedded items`() =
        testWithApp {
            val response =
                client.get("/books/1") {
                    parameter("_embed", "loans")
                    parameter("_embed", "libraryBooks")
                }
            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.bodyAsText().toJsonObject()
            val loans = responseBody["loans"]?.jsonArray
            val libraryBooks = responseBody["libraryBooks"]?.jsonArray

            assertNotNull(loans)
            assertNotNull(libraryBooks)
            assertEquals(1, loans.size)
            assertEquals(2, libraryBooks.size)
        }

    @Test
    fun `GET collection item with embedding - when given 2 expand values - should return collection item with expanded items`() =
        testWithApp {
            val response =
                client.get("/loans/1") {
                    parameter("_expand", "book")
                    parameter("_expand", "user")
                }
            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.bodyAsText().toJsonObject()
            val book = responseBody["book"]
            val user = responseBody["user"]

            assertNotNull(book)
            assertNotNull(user)
        }

    @Test
    fun `GET collection item with embedding - when given embed and expand values - should return item with related items`() =
        testWithApp {
            val response =
                client.get("/users/1") {
                    parameter("_embed", "loans")
                    parameter("_expand", "libraryRegistration")
                }
            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.bodyAsText().toJsonObject()
            val loans = responseBody["loans"]
            val libraryRegistration = responseBody["libraryRegistration"]

            assertNotNull(loans)
            assertNotNull(libraryRegistration)
        }

    private fun getTitleListFromXmlCollection(xml: String): List<String> {
        val node: JsonNode = xmlMapper.readTree(xml)
        return node.get("item").map { it.get("title").textValue() }
    }

    private fun getValueFromXmlItem(
        xml: String,
        tag: String,
    ): String {
        val node: JsonNode = xmlMapper.readTree(xml)
        return node.get(tag).textValue()
    }

    private fun getTitleListFromCsvCollection(csv: String): List<String> {
        val lines = csv.lines()
        val indexOfTitle = lines[0].split(";").indexOfFirst { it == "title" }
        return lines.subList(1, lines.size - 1).map { it.split(";")[indexOfTitle] }
    }

    private fun getValueFromCsvItem(
        csv: String,
        tag: String,
    ): String {
        val lines = csv.lines()
        val indexOfTitle = lines[0].split(";").indexOfFirst { it == tag }
        return lines[1].split(";")[indexOfTitle]
    }
}
