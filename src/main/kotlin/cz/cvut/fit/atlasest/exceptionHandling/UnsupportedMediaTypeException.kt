package cz.cvut.fit.atlasest.exceptionHandling

import io.ktor.http.ContentType

class UnsupportedMediaTypeException(
    message: String,
    val supportedMediaTypes: List<ContentType>,
) : RuntimeException(message)
