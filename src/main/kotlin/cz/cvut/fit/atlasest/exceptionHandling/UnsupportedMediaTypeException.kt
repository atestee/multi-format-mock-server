package cz.cvut.fit.atlasest.exceptionHandling

import io.ktor.http.ContentType

/**
 * Exception thrown when the media type given in `Content-Type` header is not supported
 */
class UnsupportedMediaTypeException(
    message: String,
    val supportedMediaTypes: List<ContentType>,
) : RuntimeException(message)
