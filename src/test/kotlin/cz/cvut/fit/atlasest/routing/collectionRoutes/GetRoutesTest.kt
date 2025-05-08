package cz.cvut.fit.atlasest.routing.collectionRoutes

import BaseTest
import com.fasterxml.jackson.databind.JsonNode
import cz.cvut.fit.atlasest.services.LIMIT
import cz.cvut.fit.atlasest.services.ORDER
import cz.cvut.fit.atlasest.services.PAGE
import cz.cvut.fit.atlasest.services.SORT
import cz.cvut.fit.atlasest.utils.getPropertyValue
import cz.cvut.fit.atlasest.utils.toJsonArray
import cz.cvut.fit.atlasest.utils.toJsonObject
import cz.cvut.fit.atlasest.utils.xmlMapper
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.intOrNull
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
            "Les Miserables",
            "Frankenstein",
            "1984",
            "One Hundred Years of Solitude",
            "Pride and Prejudice",
            "The Great Gatsby",
            "Sense and Sensibility",
            "Jane Eyre",
            "Brave New World",
            "To Kill a Mockingbird",
        )

    @Test
    fun `GET collections - when collection endpoint is called - should return list of collection names`() =
        testWithApp {
            val response = client.get("/collections")
            val responseBody = response.bodyAsText()
            assertNotNull(responseBody)
            assertEquals("collections: [books, loans, users, libraries, libraryBooks, libraryRegistrations, reviews]", responseBody)
            assertEquals(HttpStatusCode.OK, response.status)
        }

    @Test
    fun `GET collection names - when given existing collection name - should return valid JSON collection`() =
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
    fun `GET books - when given unsupported Accept header - should return 406 and Vary header`() =
        testWithApp {
            val response =
                client.get("/books") {
                    accept(ContentType.Application.Yaml)
                }
            assertEquals(HttpStatusCode.NotAcceptable, response.status)
            assertNotNull(response.headers[HttpHeaders.Vary])
            assertEquals(HttpHeaders.Accept, response.headers[HttpHeaders.Vary])
        }

    @Test
    fun `GET book item - when given unsupported Accept header - should return 406 and Vary header`() =
        testWithApp {
            val response =
                client.get("/books/1") {
                    accept(ContentType.Application.Yaml)
                }
            assertEquals(HttpStatusCode.NotAcceptable, response.status)
            assertNotNull(response.headers[HttpHeaders.Vary])
            assertEquals(HttpHeaders.Accept, response.headers[HttpHeaders.Vary])
        }

    @Test
    fun `GET books - when given existing collection name and Accept=XML - should return valid XML collection`() =
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
    fun `GET books - when given existing collection name and Accept=CSV - should return valid CSV collection`() =
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
    fun `GET invalidName - when given non-existing collection name - should return 404`() =
        testWithApp {
            val response = client.get("/invalidName")
            assertEquals(HttpStatusCode.NotFound, response.status)
        }

    @Test
    fun `GET book item - when given valid id - should return corresponding item`() =
        testWithApp {
            val id = "1"
            val response = client.get("/books/$id")
            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.bodyAsText()
            assertNotNull(responseBody)
            val responseBodyObject = responseBody.toJsonObject()
            assertEquals(responseBodyObject["id"]?.jsonPrimitive?.content, id)
            assertEquals(responseBodyObject["title"]?.jsonPrimitive?.content, "Les Miserables")
        }

    @Test
    fun `GET book item - when given valid id and Accept=XML - should return corresponding XML item`() =
        testWithApp {
            val id = "1"
            val response = client.get("/books/$id") { accept(ContentType.Application.Xml) }
            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.bodyAsText()
            assertEquals(getValueFromXmlItem(responseBody, "id"), id)
            assertEquals(getValueFromXmlItem(responseBody, "title"), "Les Miserables")
        }

    @Test
    fun `GET book item - when given valid id and Accept=CSV - should return corresponding CSV item`() =
        testWithApp {
            val id = "1"
            val response = client.get("/books/$id") { accept(ContentType.Text.CSV) }
            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.bodyAsText()
            assertEquals(getValueFromCsvItem(responseBody, "id"), id)
            assertEquals(getValueFromCsvItem(responseBody, "title"), "Les Miserables")
        }

    @Test
    fun `GET book item - when given invalid id - should return 404`() =
        testWithApp {
            val invalidId = "100"
            val response = client.get("/books/$invalidId")
            assertEquals(HttpStatusCode.NotFound, response.status)
            val responseBody = response.bodyAsText()
            assertEquals("Item with id '$invalidId' not found in collection 'books'", responseBody)
        }

    @Test
    fun `GET user item - when given valid id for item with custom identifier - should return corresponding item`() =
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
    fun `GET books with filter - when given genre=Fiction and genre=Dystopian - should return corresponding collection items`() =
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
    fun `GET books with filter - when given genre0_like=Fiction and genre1=Dystopian - should return corresponding collection items`() =
        testWithApp {
            val response =
                client.get("/books") {
                    parameter("genre[0]_like", "Fiction")
                    parameter("genre[1]", "Dystopian")
                }
            val responseBody = response.bodyAsText().toJsonArray()
            val titles = responseBody.map { it.jsonObject["title"]?.jsonPrimitive?.content }
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(1, responseBody.size)
            assertContains(titles, "Brave New World")
        }

    @Test
    fun `GET reviews with filter - when given submitted=2025-02-24T13-20-40Z - should return corresponding collection items`() =
        testWithApp {
            val response =
                client.get("/reviews") {
                    parameter("submitted", "2025-02-24T13:20:40Z")
                }
            val responseBody = response.bodyAsText().toJsonArray()
            val ids = responseBody.map { it.jsonObject["id"]?.jsonPrimitive?.content }
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(2, responseBody.size)
            assertContains(ids, "9")
            assertContains(ids, "10")
        }

    @Test
    fun `GET reviews with filter - when given submitted=2025-02-24 - should return corresponding collection items`() =
        testWithApp {
            val response =
                client.get("/reviews") {
                    parameter("submitted_like", "2025-02-24")
                }
            val responseBody = response.bodyAsText().toJsonArray()
            val ids = responseBody.map { it.jsonObject["id"]?.jsonPrimitive?.content }
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(3, responseBody.size)
            assertContains(ids, "6")
            assertContains(ids, "9")
            assertContains(ids, "10")
        }

    @Test
    fun `GET reviews with filter - when given submitted_gt=2025-02-24 - should return corresponding collection items`() =
        testWithApp {
            val response =
                client.get("/reviews") {
                    parameter("submitted_gt", "2025-02-24")
                }
            val body = response.bodyAsText()
            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = body.toJsonArray()
            val ids = responseBody.map { it.jsonObject["id"]?.jsonPrimitive?.content }
            assertEquals(4, responseBody.size)
            assertContains(ids, "2")
            assertContains(ids, "3")
            assertContains(ids, "4")
            assertContains(ids, "5")
        }

    @Test
    fun `GET reviews with filter - when given submitted_gte=2025-02-24 - should return corresponding collection items`() =
        testWithApp {
            val response =
                client.get("/reviews") {
                    parameter("submitted_gte", "2025-02-24")
                }
            val body = response.bodyAsText()
            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = body.toJsonArray()
            val ids = responseBody.map { it.jsonObject["id"]?.jsonPrimitive?.content }

            assertEquals(7, responseBody.size)
            assertContains(ids, "2")
            assertContains(ids, "3")
            assertContains(ids, "4")
            assertContains(ids, "5")
            assertContains(ids, "6")
            assertContains(ids, "9")
            assertContains(ids, "10")
        }

    @Test
    fun `GET reviews with filter - when given submitted_gte=2025-02-24T13-20-40Z - should return corresponding collection items`() =
        testWithApp {
            val response =
                client.get("/reviews") {
                    parameter("submitted_gte", "2025-02-24T13:20:40Z")
                }
            val body = response.bodyAsText()
            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = body.toJsonArray()
            val ids = responseBody.map { it.jsonObject["id"]?.jsonPrimitive?.content }

            assertEquals(6, responseBody.size)
            assertContains(ids, "2")
            assertContains(ids, "3")
            assertContains(ids, "4")
            assertContains(ids, "5")
            assertContains(ids, "9")
            assertContains(ids, "10")
        }

    @Test
    fun `GET reviews with filter - when given submitted_lt=2025-02-24 - should return corresponding collection items`() =
        testWithApp {
            val response =
                client.get("/reviews") {
                    parameter("submitted_lt", "2025-02-24")
                }
            val body = response.bodyAsText()
            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = body.toJsonArray()
            val ids = responseBody.map { it.jsonObject["id"]?.jsonPrimitive?.content }

            assertEquals(3, responseBody.size)
            assertContains(ids, "1")
            assertContains(ids, "7")
            assertContains(ids, "8")
        }

    @Test
    fun `GET reviews with filter - when given submitted_lte=2025-02-24 - should return corresponding collection items`() =
        testWithApp {
            val response =
                client.get("/reviews") {
                    parameter("submitted_lte", "2025-02-24")
                }
            val body = response.bodyAsText()
            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = body.toJsonArray()
            val ids = responseBody.map { it.jsonObject["id"]?.jsonPrimitive?.content }

            assertEquals(6, responseBody.size)
            assertContains(ids, "1")
            assertContains(ids, "6")
            assertContains(ids, "7")
            assertContains(ids, "8")
            assertContains(ids, "9")
            assertContains(ids, "10")
        }

    @Test
    fun `GET reviews with filter - when given submitted_lte=2025-02-24T11-50-00Z - should return corresponding collection items`() =
        testWithApp {
            val response =
                client.get("/reviews") {
                    parameter("submitted_lte", "2025-02-24T11:50:00Z")
                }
            val body = response.bodyAsText()
            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = body.toJsonArray()
            val ids = responseBody.map { it.jsonObject["id"]?.jsonPrimitive?.content }

            assertEquals(4, responseBody.size)
            assertContains(ids, "1")
            assertContains(ids, "6")
            assertContains(ids, "7")
            assertContains(ids, "8")
        }

    @Test
    fun `GET libraries with filter - when given shiftTimetable 1 1 = Jack - should return corresponding collection item`() =
        testWithApp {
            val response =
                client.get("/libraries") {
                    parameter("shiftTimetable[1][1]", "Jack")
                }
            val body = response.bodyAsText()
            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = body.toJsonArray()
            val ids = responseBody.map { it.jsonObject["id"]?.jsonPrimitive?.content }
            assertEquals(1, responseBody.size)
            assertContains(ids, "2")
        }

    @Test
    fun `GET libraries with filter - when given shiftTimetable wildcard 0 = Anna - should return corresponding collection items`() =
        testWithApp {
            val response =
                client.get("/libraries") {
                    parameter("shiftTimetable[*][0]", "Anna")
                }
            val body = response.bodyAsText()
            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = body.toJsonArray()
            val ids = responseBody.map { it.jsonObject["id"]?.jsonPrimitive?.content }
            assertEquals(2, responseBody.size)
            assertContains(ids, "1")
            assertContains(ids, "2")
        }

    @Test
    fun `GET books with pagination - when given page=1 and limit=default 10 - should return corresponding collection items and empty Link`() =
        testWithApp {
            val response =
                client.get("/books") {
                    parameter("_page", "1")
                }
            val responseBody = response.bodyAsText().toJsonArray()
            val ids = responseBody.map { it.jsonObject["id"]?.jsonPrimitive?.content }
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(10, responseBody.size)
            assertEquals("1", ids.first())
            assertEquals("10", ids.last())
            val linkHeader = response.headers[HttpHeaders.Link]
            assertEquals("", linkHeader)
        }

    @Test
    fun `GET books with pagination - when given page=2 and limit=default 10 - should return 0 items and first and prev links`() =
        testWithApp {
            val response =
                client.get("/books") {
                    parameter("_page", "2")
                }
            val responseBody = response.bodyAsText().toJsonArray()
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(0, responseBody.size)
            val baseUrl = "http://${appConfig.host}:${appConfig.port}/books"
            val expectedLinks =
                """
                <$baseUrl?_page=1&_limit=10>; rel="first", 
                <$baseUrl?_page=1&_limit=10>; rel="prev"
                """.trimIndent().replace("\n", "")
            val linkHeader = response.headers[HttpHeaders.Link]
            assertEquals(expectedLinks, linkHeader)
        }

    @Test
    fun `GET books with pagination - when given page=2 and limit=4 - should return corresponding collection items and all links`() =
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

            val baseUrl = "http://${appConfig.host}:${appConfig.port}/books"
            val expectedLinks =
                """
                <$baseUrl?_page=1&_limit=4>; rel="first", 
                <$baseUrl?_page=1&_limit=4>; rel="prev", 
                <$baseUrl?_page=3&_limit=4>; rel="next", 
                <$baseUrl?_page=3&_limit=4>; rel="last"
                """.trimIndent().replace("\n", "")
            val linkHeader = response.headers[HttpHeaders.Link]
            assertEquals(expectedLinks, linkHeader)
        }

    @Test
    fun `GET books with pagination - when given page=null and limit=4 - should throw 400`() =
        testWithApp {
            val response =
                client.get("/books") {
                    parameter("_limit", "4")
                }
            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertEquals("Pagination parameter $LIMIT is without $PAGE", response.bodyAsText())
        }

    @Test
    fun `GET books with sorting - when given nested sort=published year and order=desc - should return items sorted desc`() =
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
                        .getPropertyValue(publishedYearKey)
                        ?.jsonPrimitive
                        ?.content
                }
            assertEquals("1967", years.first())
            assertEquals("1811", years.last())
        }

    @Test
    fun `GET books with sorting - when given nested sort=published year and order=null - should return items sorted asc`() =
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
                        .getPropertyValue(publishedYearKey)
                        ?.jsonPrimitive
                        ?.content
                }
            assertEquals("1811", years.first())
            assertEquals("1967", years.last())
        }

    @Test
    fun `GET books with sorting - when given sort=author and order=desc - should return items sorted desc`() =
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
    fun `GET books with sorting - when given sort=null and order=null - should return items unsorted`() =
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
    fun `GET books with sorting - when given sort=null and order=asc - should throw 400`() =
        testWithApp {
            val response =
                client.get("/books") {
                    parameter("_order", "asc")
                }
            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertEquals("Sorting parameter $ORDER is without $SORT", response.bodyAsText())
        }

    @Test
    fun `GET books with sorting - when given sort=author order is invalid - should throw 400`() =
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
    fun `GET books with sorting - when given sorting key is invalid - should throw 400`() =
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
    fun `GET books with embedding - when given 2 embed values - should return collection with embedded items from both collections`() =
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
    fun `GET books with embedding - when given 2 expand values - should return collection with embedded items from both collections`() =
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
    fun `GET users with embedding and expanding - when given embed and expand values - should return collection with related items`() =
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
    fun `GET book item with embedding - when given 2 embed values - should return collection item with embedded items`() =
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
    fun `GET book item with embedding - when given 2 expand values - should return collection item with expanded items`() =
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
    fun `GET user item with embedding - when given embed and expand values - should return item with related items`() =
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

    @Test
    fun `GET user item with filter on embedded - when given embed value and filter - should apply filter on embedded data`() =
        testWithApp {
            val response =
                client.get("/users") {
                    parameter("_embed", "loans")
                    parameter("loans[0].bookId", 1)
                }
            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.bodyAsText().toJsonArray()
            assertNotNull(responseBody)
            assertEquals(1, responseBody.size)
            assertEquals(
                1,
                responseBody
                    .first()
                    .jsonObject["loans"]
                    ?.jsonArray
                    ?.size,
            )
            assertEquals(
                1,
                responseBody
                    .first()
                    .jsonObject["loans"]
                    ?.jsonArray
                    ?.first()
                    ?.jsonObject
                    ?.get("userId")
                    ?.jsonPrimitive
                    ?.intOrNull,
            )
        }

    @Test
    fun `GET user item with filter on embedded - when given embed=loans and loans wildcard bookId=1 - should apply filter on embedded data`() =
        testWithApp {
            val response =
                client.get("/users") {
                    parameter("_embed", "loans")
                    parameter("loans[*].bookId", 1)
                }
            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.bodyAsText().toJsonArray()
            assertNotNull(responseBody)
            assertEquals(1, responseBody.size)
            assertEquals(
                1,
                responseBody
                    .first()
                    .jsonObject["loans"]
                    ?.jsonArray
                    ?.size,
            )
            assertEquals(
                1,
                responseBody
                    .first()
                    .jsonObject["loans"]
                    ?.jsonArray
                    ?.first()
                    ?.jsonObject
                    ?.get("userId")
                    ?.jsonPrimitive
                    ?.intOrNull,
            )
        }

    @Test
    fun `GET book item with filter on expanded - when given expand value and filter - should apply filter on expanded data`() =
        testWithApp {
            val response =
                client.get("/loans") {
                    parameter("_expand", "user")
                    parameter("user.name", "Alice")
                }
            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.bodyAsText().toJsonArray()
            assertNotNull(responseBody)
            assertEquals(1, responseBody.size)
            assertEquals(
                "2025-02-10",
                responseBody
                    .first()
                    .jsonObject
                    ["loanDate"]
                    ?.jsonPrimitive
                    ?.content,
            )
            assertEquals(
                "alice@example.com",
                responseBody
                    .first()
                    .jsonObject
                    ["user"]
                    ?.jsonObject
                    ?.get("email")
                    ?.jsonPrimitive
                    ?.content,
            )
        }

    @Test
    fun `GET books schema - when given collection name - should respond with collection schema`() =
        testWithApp {
            val response =
                client.get("/books/schema")
            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.bodyAsText().toJsonObject()
            val properties = responseBody["properties"]?.jsonObject
            assertEquals(setOf("id", "title", "author", "genre", "isbn", "published"), properties?.keys)
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
