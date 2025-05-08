package cz.cvut.fit.atlasest.exceptionHandling

/**
 * Exception thrown when no supported media type can be provided based on the `Accept` header
 */
class NotAcceptableException(
    message: String,
) : RuntimeException(message)
