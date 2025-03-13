package cz.cvut.fit.atlasest.exceptions

import cz.cvut.fit.atlasest.utils.log
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
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to cause.message))
        }
        exception<NotFoundException> { call, cause ->
            call.respond(HttpStatusCode.NotFound, mapOf("error" to cause.message))
        }
        exception<CorruptedDataException> { call, cause ->
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Data corruption detected"))
            log.error("Problem with local data", cause)
        }
        exception<Throwable> { call, cause ->
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Something went wrong"))
            log.error("Unhandled error", cause)
        }
        exception<ValidationException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to cause.message))
            log.error("Validation exception", cause)
        }
    }
}
