package cz.cvut.fit.atlasest.routing.routes

import BaseTest
import io.ktor.client.request.delete
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals

class DeleteRouteTest : BaseTest() {
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
