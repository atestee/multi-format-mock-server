package cz.cvut.fit.atlasest.routing.routes

import BaseTest
import com.fasterxml.jackson.databind.JsonNode
import cz.cvut.fit.atlasest.utils.toJsonArray
import cz.cvut.fit.atlasest.utils.toJsonObject
import cz.cvut.fit.atlasest.utils.xmlMapper
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class GetRoutesTest : BaseTest() {
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
    fun `GET collection - when given existing collection name - should return valid JSON collection`() =
        testWithApp {
            val response = client.get("/books")
            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.bodyAsText()
            val responseBodyArray = responseBody.toJsonArray()
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
    fun `GET collection - when given existing collection name and Accept=XML - should return valid XML collection`() =
        testWithApp {
            val response =
                client.get("/books") {
                    accept(ContentType.Application.Xml)
                }
            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.bodyAsText()
            val actualTitles = getTitleListFromXmlCollection(responseBody)
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
