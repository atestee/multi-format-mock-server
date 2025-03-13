package cz.cvut.fit.atlasest.utils

import org.slf4j.Logger
import org.slf4j.LoggerFactory

val Any.log: Logger
    get() = LoggerFactory.getLogger(this::class.java)
