package cz.cvut.fit.atlasest.exceptionHandling

import BaseTest
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import org.junit.jupiter.api.Assertions.assertEquals
import javax.validation.ValidationException
import kotlin.test.Test
import kotlin.test.assertTrue

class ExceptionHandlingTest : BaseTest() {
    @Test
    fun `configureExceptionHandling - when BadRequestException is thrown - 400`() =
        testAppWithExceptionThrows(exception = BadRequestException("(Test) Bad Request")) {
            val response = client.get("/test")
            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertEquals("(Test) Bad Request", response.bodyAsText())
        }

    @Test
    fun `configureExceptionHandling - when NotFoundException is thrown - 404`() =
        testAppWithExceptionThrows(exception = NotFoundException("(Test) Not Found")) {
            val response = client.get("/test")
            assertEquals(HttpStatusCode.NotFound, response.status)
            assertEquals("(Test) Not Found", response.bodyAsText())
        }

    @Test
    fun `configureExceptionHandling - when InvalidDataException is thrown - 500`() =
        testAppWithExceptionThrows(exception = InvalidDataException("(Test) Missing id")) {
            val response = client.get("/test")
            assertEquals(HttpStatusCode.InternalServerError, response.status)
            assertEquals("Invalid Data: (Test) Missing id", response.bodyAsText())
        }

    @Test
    fun `configureExceptionHandling - when NotAcceptableException is thrown - 406`() =
        testAppWithExceptionThrows(exception = NotAcceptableException("(Test) Not Acceptable")) {
            val response = client.get("/test")
            assertEquals(HttpStatusCode.NotAcceptable, response.status)
            assertEquals("(Test) Not Acceptable", response.bodyAsText())
            assertTrue(response.headers.names().contains(HttpHeaders.Vary))
            assertEquals(HttpHeaders.Accept, response.headers[HttpHeaders.Vary])
        }

    @Test
    fun `configureExceptionHandling - when UnsupportedMediaTypeException is thrown - 415`() =
        testAppWithExceptionThrows(
            exception =
                UnsupportedMediaTypeException(
                    "(Test) Unsupported Media Type",
                    listOf(ContentType.Application.Json, ContentType.Application.Yaml),
                ),
        ) {
            val response = client.get("/test")
            assertEquals(HttpStatusCode.UnsupportedMediaType, response.status)
            assertEquals("(Test) Unsupported Media Type", response.bodyAsText())
            assertTrue(response.headers.names().contains(HttpHeaders.Vary))
            assertTrue(response.headers.names().contains(HttpHeaders.Accept))
            assertEquals(HttpHeaders.Accept, response.headers[HttpHeaders.Vary])
            assertEquals(
                listOf(ContentType.Application.Json, ContentType.Application.Yaml).joinToString(", "),
                response.headers[HttpHeaders.Accept],
            )
        }

    @Test
    fun `configureExceptionHandling - when ValidationException is thrown - 400`() =
        testAppWithExceptionThrows(
            exception = ValidationException("(Test) Validation Error"),
        ) {
            val response = client.get("/test")
            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertEquals("(Test) Validation Error", response.bodyAsText())
        }

    @Test
    fun `configureExceptionHandling - when Throwable is thrown - 500`() =
        testAppWithExceptionThrows(
            exception = Throwable("(Test) Unhandled Error"),
        ) {
            val response = client.get("/test")
            assertEquals(HttpStatusCode.InternalServerError, response.status)
            assertEquals("Something went wrong", response.bodyAsText())
        }

    @Test
    fun `configureExceptionHandling - when unknown endpoint - 404`() =
        testWithApp {
            val response = client.get("/unknown")
            assertEquals(HttpStatusCode.NotFound, response.status)
            assertEquals("404: Not Found", response.bodyAsText())
        }
}
