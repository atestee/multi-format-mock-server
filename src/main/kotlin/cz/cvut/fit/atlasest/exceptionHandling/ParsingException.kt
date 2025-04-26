package cz.cvut.fit.atlasest.exceptionHandling

import java.lang.Exception

class ParsingException(
    message: String,
    exception: Exception? = null,
) : RuntimeException(message, exception)
