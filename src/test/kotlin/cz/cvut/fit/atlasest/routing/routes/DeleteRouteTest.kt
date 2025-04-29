package cz.cvut.fit.atlasest.routing.routes

import BaseTest
import cz.cvut.fit.atlasest.utils.getFieldValue
import io.ktor.client.request.delete
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DeleteRouteTest : BaseTest() {
    @Test
    fun `DELETE collection item - when given valid id - should return 200 and item with connected items should be deleted`() =
        testWithApp {
            val response = client.delete("/books/1")
            assertEquals(HttpStatusCode.OK, response.status)

            val books = collectionService.getCollectionItems("books")
            assertTrue(books.none { it.getFieldValue("id")?.jsonPrimitive?.content == "1" })

            val loans = collectionService.getCollectionItems("loans")
            assertTrue(loans.none { it.getFieldValue("bookId")?.jsonPrimitive?.content == "1" })
        }

    @Test
    fun `DELETE collection item - when given invalid id - should return 200 and item should be deleted`() =
        testWithApp {
            val response = client.delete("/books/123")
            assertEquals(HttpStatusCode.OK, response.status)
        }
}
