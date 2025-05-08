package cz.cvut.fit.atlasest.routing

import io.ktor.http.HttpHeaders
import io.swagger.v3.oas.models.media.Schema

/**
 * Defines response header definitions for the generated OpenAPI specification
 */
data class ResponseHeaderDefinition(
    val header: String,
    val schema: Schema<*>,
    val description: String? = null,
)

val linkHeader =
    ResponseHeaderDefinition(
        header = HttpHeaders.Link,
        schema =
            Schema<Any>().apply {
                this.addType("string")
            },
        description = "Pagination links to navigate between pages",
    )

val locationHeader =
    ResponseHeaderDefinition(
        header = HttpHeaders.Location,
        schema =
            Schema<Any>().apply {
                this.addType("string")
                format = "uri"
            },
        description = "The URI of the inserted item",
    )

val varyHeader =
    ResponseHeaderDefinition(
        header = HttpHeaders.Vary,
        schema =
            Schema<Any>().apply {
                this.addType("string")
            },
        description = "Indicates which header is used for content negotiation",
    )

val acceptHeader =
    ResponseHeaderDefinition(
        header = HttpHeaders.Accept,
        schema =
            Schema<Any>().apply {
                this.addType("string")
            },
        description = "Indicates which media types are accepted by the server",
    )
