package cz.cvut.fit.atlasest.exceptionHandling

import java.lang.Exception

/**
 * Exception thrown when there is a problem with the user-defined files (i.e. files
 * with collection data, custom identifiers, and collection JSON Schemas)
 */
class InvalidDataException(
    message: String,
    exception: Exception? = null,
) : RuntimeException(message, exception)
