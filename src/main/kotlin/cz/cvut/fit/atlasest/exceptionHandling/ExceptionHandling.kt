package cz.cvut.fit.atlasest.exceptionHandling

import cz.cvut.fit.atlasest.utils.log
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import javax.validation.ValidationException

fun Application.configureExceptionHandling() {
    install(StatusPages) {
        exception<BadRequestException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, cause.message as String)
        }
        exception<NotFoundException> { call, cause ->
            call.respond(HttpStatusCode.NotFound, cause.message as String)
        }
        exception<InvalidDataException> { call, cause ->
            log.error("Invalid data", cause)
            call.respond(HttpStatusCode.InternalServerError, "Invalid data: ${cause.message}")
        }
        exception<ParsingException> { call, cause ->
            log.error("Parsing error", cause)
            call.respond(HttpStatusCode.InternalServerError, "Parsing error: ${cause.message}")
        }
        exception<NotAcceptableException> { call, cause ->
            log.error("Not acceptable", cause)
            call.response.headers.append(HttpHeaders.Vary, HttpHeaders.Accept)
            call.respond(HttpStatusCode.NotAcceptable, cause.message as String)
        }
        exception<UnsupportedMediaTypeException> { call, cause ->
            log.error("Unsupported media type", cause)
            call.response.headers.append(HttpHeaders.Vary, HttpHeaders.Accept)
            call.response.headers.append(HttpHeaders.Accept, cause.supportedMediaTypes.joinToString(", "))
            call.respond(HttpStatusCode.UnsupportedMediaType, cause.message as String)
        }
        exception<Throwable> { call, cause ->
            log.error("Unhandled error", cause)
            call.respond(HttpStatusCode.InternalServerError, "Something went wrong")
        }
        exception<ValidationException> { call, cause ->
            log.error("Validation exception", cause)
            call.respond(HttpStatusCode.BadRequest, cause.message as String)
        }
        status(HttpStatusCode.NotFound) { call, _ ->
            call.respond(HttpStatusCode.NotFound, "404: Not Found")
        }
    }
}
